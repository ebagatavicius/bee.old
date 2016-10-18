package com.butent.bee.server.modules.mail;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.io.CharStreams;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.server.Invocation;
import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.utils.HtmlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.mail.MailFolder;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.UIDFolder;
import javax.mail.internet.AddressException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;
import javax.mail.util.SharedByteArrayInputStream;

@Singleton
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
@Lock(LockType.READ)
public class MailStorageBean {

  private static final BeeLogger logger = LogUtils.getLogger(MailStorageBean.class);

  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;
  @EJB
  FileStorageBean fs;
  @EJB
  ConcurrencyBean cb;
  @Resource
  SessionContext ctx;

  private final Table<Long, String, Long> repliedStack = HashBasedTable.create();
  private final ReentrantLock repliedStackLock = new ReentrantLock();

  public void attachMessages(Long folderId, Map<Long, Integer> messages) {
    for (Entry<Long, Integer> message : messages.entrySet()) {
      storePlace(message.getKey(), folderId, message.getValue(), null);
    }
  }

  public void connectFolder(MailFolder folder) {
    validateFolder(folder, null);
  }

  public MailFolder createFolder(MailAccount account, MailFolder parent, String name) {
    Assert.notNull(parent);

    for (MailFolder subFolder : parent.getSubFolders()) {
      if (BeeUtils.same(subFolder.getName(), name)) {
        return subFolder;
      }
    }
    MailFolder folder = createFolder(account.getAccountId(), parent, name, null);

    if (!account.isStoredRemotedly(parent)) {
      disconnectFolder(folder);
    }
    return folder;
  }

  public void detachMessages(IsCondition clause) {
    qs.updateData(new SqlDelete(TBL_PLACES).setWhere(clause));
  }

  public void disconnectFolder(MailFolder folder) {
    Assert.notNull(folder);

    if (folder.isConnected()) {
      folder.disconnect();

      qs.updateData(new SqlUpdate(TBL_PLACES)
          .addConstant(COL_MESSAGE_UID, null)
          .setWhere(SqlUtils.equals(TBL_PLACES, COL_FOLDER, folder.getId())));

      qs.updateData(new SqlUpdate(TBL_FOLDERS)
          .addConstant(COL_FOLDER_UID, folder.getUidValidity())
          .setWhere(sys.idEquals(TBL_FOLDERS, folder.getId())));
    }
  }

  public void dropFolder(MailFolder folder) {
    Assert.notNull(folder);

    qs.updateData(new SqlDelete(TBL_FOLDERS)
        .setWhere(sys.idEquals(TBL_FOLDERS, folder.getId())));
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public MailAccount getAccount(Long accountId) {
    Assert.state(DataUtils.isId(accountId));
    return getAccount(sys.idEquals(TBL_ACCOUNTS, accountId), false);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public MailAccount getAccount(IsCondition condition, boolean checkUnread) {
    MailAccount account = new MailAccount(qs.getRow(new SqlSelect()
        .addAllFields(TBL_ACCOUNTS)
        .addField(TBL_ACCOUNTS, sys.getIdName(TBL_ACCOUNTS), COL_ACCOUNT)
        .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
        .addFrom(TBL_ACCOUNTS)
        .addFromInner(TBL_EMAILS,
            sys.joinTables(TBL_EMAILS, TBL_ACCOUNTS, MailConstants.COL_ADDRESS))
        .setWhere(condition)));

    SqlSelect query = new SqlSelect()
        .addFields(TBL_FOLDERS, COL_FOLDER_PARENT, COL_FOLDER_NAME, COL_FOLDER_UID)
        .addField(TBL_FOLDERS, sys.getIdName(TBL_FOLDERS), COL_FOLDER)
        .addFrom(TBL_FOLDERS)
        .setWhere(SqlUtils.equals(TBL_FOLDERS, COL_ACCOUNT, account.getAccountId()))
        .addOrder(TBL_FOLDERS, COL_FOLDER_PARENT, COL_FOLDER_NAME);

    if (checkUnread) {
      query.addCount(TBL_PLACES, COL_MESSAGE)
          .addFromLeft(TBL_PLACES,
              SqlUtils.and(sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER),
                  SqlUtils.equals(SqlUtils.bitAnd(SqlUtils.nvl(
                      SqlUtils.field(TBL_PLACES, COL_FLAGS), 0), MessageFlag.SEEN.getMask()), 0)))
          .addGroup(TBL_FOLDERS,
              COL_FOLDER_PARENT, COL_FOLDER_NAME, COL_FOLDER_UID, sys.getIdName(TBL_FOLDERS));
    } else {
      query.addEmptyInt(COL_MESSAGE);
    }
    Multimap<Long, SimpleRow> folders = LinkedListMultimap.create();

    for (SimpleRow row : qs.getData(query)) {
      folders.put(row.getLong(COL_FOLDER_PARENT), row);
    }
    account.setFolders(folders);

    return account;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void initAccount(Long accountId) {
    MailAccount account = getAccount(accountId);

    MailFolder inbox = createFolder(account, account.getRootFolder(),
        SystemFolder.Inbox.getFolderName());

    if (!inbox.isConnected()) {
      connectFolder(inbox);
    }
    for (SystemFolder sysFolder : SystemFolder.values()) {
      MailFolder folder = inbox;

      if (sysFolder != SystemFolder.Inbox) {
        folder = createFolder(account, inbox, sysFolder.getFolderName());
      }
      qs.updateData(new SqlUpdate(TBL_ACCOUNTS)
          .addConstant(sysFolder.name() + COL_FOLDER, folder.getId())
          .setWhere(sys.idEquals(TBL_ACCOUNTS, accountId)));
    }
  }

  public void renameFolder(MailFolder folder, String name) {
    qs.updateData(new SqlUpdate(TBL_FOLDERS)
        .addConstant(COL_FOLDER_NAME, name)
        .setWhere(sys.idEquals(TBL_FOLDERS, folder.getId())));
  }

  public void setFlags(Long placeId, int flags) {
    qs.updateData(new SqlUpdate(TBL_PLACES)
        .addConstant(COL_FLAGS, flags)
        .setWhere(sys.idEquals(TBL_PLACES, placeId)));
  }

  public Long storeMail(MailAccount account, Message message, Long folderId, Long messageUID)
      throws MessagingException {

    MailEnvelope envelope = new MailEnvelope(message);
    Holder<Long> messageId = Holder.absent();
    Holder<Boolean> finished = Holder.of(false);

    cb.synchronizedCall(new Runnable() {
      @Override
      public void run() {
        SimpleRow row = qs.getRow(new SqlSelect()
            .addField(TBL_MESSAGES, sys.getIdName(TBL_MESSAGES), COL_MESSAGE)
            .addFields(TBL_MESSAGES, COL_RAW_CONTENT)
            .addFrom(TBL_MESSAGES)
            .setWhere(SqlUtils.equals(TBL_MESSAGES, COL_UNIQUE_ID, envelope.getUniqueId())));

        if (row != null) {
          messageId.set(row.getLong(COL_MESSAGE));
          finished.set(DataUtils.isId(row.getLong(COL_RAW_CONTENT)));
        } else {
          String subj;

          try {
            subj = getStringContent(envelope.getSubject());
          } catch (IOException e) {
            subj = null;
          }
          messageId.set(qs.insertData(new SqlInsert(TBL_MESSAGES)
              .addConstant(COL_UNIQUE_ID, envelope.getUniqueId())
              .addConstant(COL_DATE, envelope.getDate())
              .addNotEmpty(COL_SUBJECT,
                  sys.clampValue(TBL_MESSAGES, COL_SUBJECT, subj))));
        }
      }
    });
    boolean hasPlace = false;

    if (finished.get()) {
      hasPlace = qs.sqlExists(TBL_PLACES, SqlUtils.and(messageUID == null
              ? SqlUtils.isNull(TBL_PLACES, COL_MESSAGE_UID)
              : SqlUtils.equals(TBL_PLACES, COL_MESSAGE_UID, messageUID),
          SqlUtils.equals(TBL_PLACES, COL_MESSAGE, messageId.get(), COL_FOLDER, folderId)));
    } else {
      Holder<Long> senderId = Holder.absent();
      InternetAddress sender = envelope.getSender();

      if (sender != null) {
        try {
          senderId.set(storeAddress(account.getUserId(), sender));
        } catch (AddressException e) {
          logger.warning("(MessageID=", messageId.get(), ") Error storing address:", e);
        }
      }
      Long fileId;
      InputStream is;

      try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.writeTo(bos);
        bos.close();
        is = new SharedByteArrayInputStream(bos.toByteArray());
        fileId = fs.storeFile(is, "mail@" + envelope.getUniqueId(), "text/plain");
      } catch (IOException | MessagingException e) {
        qs.updateData(new SqlDelete(TBL_MESSAGES)
            .setWhere(sys.idEquals(TBL_MESSAGES, messageId.get())));
        throw new MessagingException("(MessageID=" + messageId.get() + ") Error getting content",
            e);
      }
      cb.synchronizedCall(new Runnable() {
        @Override
        public void run() {
          finished.set(!BeeUtils.isPositive(qs.updateData(new SqlUpdate(TBL_MESSAGES)
              .addConstant(COL_SENDER, senderId.get())
              .addConstant(COL_RAW_CONTENT, fileId)
              .setWhere(SqlUtils.and(sys.idEquals(TBL_MESSAGES, messageId.get()),
                  SqlUtils.isNull(TBL_MESSAGES, COL_RAW_CONTENT))))));
        }
      });
      if (!finished.get()) {
        Set<Long> allAddresses = new HashSet<>();

        for (Entry<AddressType, InternetAddress> entry : envelope.getRecipients().entries()) {
          try {
            Long adr = storeAddress(account.getUserId(), entry.getValue());

            if (DataUtils.isId(adr) && allAddresses.add(adr)) {
              qs.insertData(new SqlInsert(TBL_RECIPIENTS)
                  .addConstant(COL_MESSAGE, messageId.get())
                  .addConstant(MailConstants.COL_ADDRESS, adr)
                  .addConstant(COL_ADDRESS_TYPE, entry.getKey().name()));
            }
          } catch (AddressException e) {
            logger.warning("(MessageID=", messageId.get(), ") Error storing address:", e);
          }
        }
        try {
          is.reset();
          Multimap<String, String> parsed = parsePart(messageId.get(), new MimeMessage(null, is));

          for (Entry<String, String> entry : parsed.entries()) {
            String content = entry.getValue();

            switch (entry.getKey()) {
              case COL_CONTENT:
              case COL_HTML_CONTENT:
                boolean isHtml = Objects.equals(entry.getKey(), COL_HTML_CONTENT);

                if (!BeeUtils.isEmpty(content)) {
                  qs.insertData(new SqlInsert(TBL_PARTS)
                      .addConstant(COL_MESSAGE, messageId.get())
                      .addConstant(COL_CONTENT, isHtml ? HtmlUtils.stripHtml(content) : content)
                      .addConstant(COL_HTML_CONTENT, isHtml ? content : null));
                }
                break;

              case COL_FILE:
                String[] arr = Codec.beeDeserializeCollection(content);

                qs.insertData(new SqlInsert(TBL_ATTACHMENTS)
                    .addConstant(COL_MESSAGE, messageId.get())
                    .addConstant(COL_FILE, BeeUtils.toLongOrNull(arr[0]))
                    .addConstant(COL_ATTACHMENT_NAME,
                        sys.clampValue(TBL_ATTACHMENTS, COL_ATTACHMENT_NAME, arr[1])));
                break;
            }
          }
        } catch (MessagingException | IOException e) {
          logger.error(e, "(MessageID=", messageId.get(), ") Error parsing content");
        }
        if (!ArrayUtils.contains(new Long[] {
            account.getDraftsFolder().getId(), account.getTrashFolder().getId()}, folderId)) {

          if (senderId.isNotNull()) {
            allAddresses.add(senderId.get());
          }
          setRelations(messageId.get(), allAddresses);
        }
      }
    }
    if (hasPlace) {
      return null;
    }
    return storePlace(messageId.get(), folderId, envelope.getFlagMask(), messageUID);
  }

  public Pair<Long, Integer> syncFolder(MailAccount account, MailFolder localFolder,
      Folder remoteFolder) throws MessagingException {
    Assert.noNulls(localFolder, remoteFolder);

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_PLACES, COL_FLAGS, COL_MESSAGE_UID)
        .addField(TBL_PLACES, sys.getIdName(TBL_PLACES), COL_PLACE)
        .addFrom(TBL_PLACES)
        .setWhere(SqlUtils.equals(TBL_PLACES, COL_FOLDER, localFolder.getId()))
        .addOrderDesc(TBL_PLACES, COL_MESSAGE_UID)
        .setLimit(100));

    long lastUid = BeeUtils.unbox(data.getLong(0, COL_MESSAGE_UID));
    int c = 0;

    if (data.getNumberOfRows() > 0) {
      Map<Long, Holder<Integer>> syncedMsgs = new HashMap<>();

      Message[] msgs = ((UIDFolder) remoteFolder).getMessagesByUID(BeeUtils
          .unbox(data.getLong(data.getNumberOfRows() - 1, COL_MESSAGE_UID)), lastUid);

      FetchProfile fp = new FetchProfile();
      fp.add(FetchProfile.Item.FLAGS);
      remoteFolder.fetch(msgs, fp);

      for (Message message : msgs) {
        long uid = ((UIDFolder) remoteFolder).getUID(message);
        SimpleRow row = data.getRowByKey(COL_MESSAGE_UID, BeeUtils.toString(uid));

        if (row != null) {
          Integer flags = MailEnvelope.getFlagMask(message);
          Holder<Integer> hasFlags = null;

          if (BeeUtils.unbox(row.getInt(COL_FLAGS)) != BeeUtils.unbox(flags)) {
            hasFlags = Holder.of(flags);
          }
          syncedMsgs.put(row.getLong(COL_PLACE), hasFlags);
        } else {
          try {
            ctx.getBusinessObject(MailStorageBean.class)
                .storeMail(account, message, localFolder.getId(), uid);
            c++;
          } catch (MessagingException e) {
            logger.error(e);
          }
        }
      }
      for (Entry<Long, Holder<Integer>> entry : syncedMsgs.entrySet()) {
        if (entry.getValue() != null) {
          c += qs.updateData(new SqlUpdate(TBL_PLACES)
              .addConstant(COL_FLAGS, entry.getValue().get())
              .setWhere(sys.idEquals(TBL_PLACES, entry.getKey())));
        }
      }
      List<Long> deletedMsgs = new ArrayList<>();

      for (int i = 0; i < data.getNumberOfRows(); i++) {
        Long id = data.getLong(i, COL_PLACE);

        if (!syncedMsgs.containsKey(id)) {
          deletedMsgs.add(id);
        }
      }
      if (!deletedMsgs.isEmpty()) {
        c += qs.updateData(new SqlDelete(TBL_PLACES)
            .setWhere(sys.idInList(TBL_PLACES, deletedMsgs)));
      }
    }
    return Pair.of(lastUid, c);
  }

  public void validateFolder(MailFolder folder, Long uidValidity) {
    Assert.notNull(folder);

    if (!Objects.equals(uidValidity, folder.getUidValidity())) {
      qs.updateData(new SqlDelete(TBL_PLACES)
          .setWhere(SqlUtils.equals(TBL_PLACES, COL_FOLDER, folder.getId())));

      qs.updateData(new SqlUpdate(TBL_FOLDERS)
          .addConstant(COL_FOLDER_UID, uidValidity)
          .setWhere(sys.idEquals(TBL_FOLDERS, folder.getId())));

      folder.setUidValidity(uidValidity);
    }
  }

  public void waitForReplied(Long folderId, String uniqueId, Long repliedFrom) {
    repliedStackLock.lock();
    repliedStack.put(folderId, uniqueId, repliedFrom);
    repliedStackLock.unlock();
  }

  private MailFolder createFolder(Long accountId, MailFolder parent, String name, Long folderUID) {
    Assert.state(DataUtils.isId(accountId));
    Assert.notEmpty(name);

    long id = qs.insertData(new SqlInsert(TBL_FOLDERS)
        .addConstant(COL_ACCOUNT, accountId)
        .addConstant(COL_FOLDER_PARENT, parent == null ? null : parent.getId())
        .addConstant(COL_FOLDER_NAME, name)
        .addConstant(COL_FOLDER_UID, folderUID));

    MailFolder folder = new MailFolder(parent, id, name, folderUID);

    if (parent != null) {
      parent.addSubFolder(folder);
    }
    return folder;
  }

  private static String getStringContent(Object enigma) throws IOException {
    InputStream stream;

    if (Objects.isNull(enigma)) {
      return null;
    } else if (enigma instanceof InputStream) {
      stream = (InputStream) enigma;
    } else {
      stream = new ByteArrayInputStream(enigma.toString().getBytes(BeeConst.CHARSET_UTF8));
    }
    return CharStreams.toString(new InputStreamReader(new FilterInputStream(stream) {
      @Override
      public int read() throws IOException {
        int chr = super.read();

        if (chr == 0) {
          chr = BeeConst.CHAR_SPACE;
        }
        return chr;
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        int cnt = super.read(b, off, len);

        for (int i = 0; i < cnt; i++) {
          if (b[i] == 0) {
            b[i] = BeeConst.CHAR_SPACE;
          }
        }
        return cnt;
      }
    }, BeeConst.CHARSET_UTF8));
  }

  private Multimap<String, String> parsePart(Long messageId, Part part)
      throws MessagingException, IOException {

    Multimap<String, String> parsedPart = LinkedListMultimap.create();

    if (part.isMimeType("multipart/*")) {
      Multipart multiPart = (Multipart) part.getContent();
      Multimap<String, String> related = LinkedListMultimap.create();

      for (int i = 0; i < multiPart.getCount(); i++) {
        Multimap<String, String> parsed;

        try {
          parsed = parsePart(messageId, multiPart.getBodyPart(i));
        } catch (MessagingException e) {
          logger.warning("(MessageID=", messageId, ") Error parsing multipart/* part:", e);
          continue;
        }
        if (part.isMimeType("multipart/alternative")) {
          if (parsed.containsKey(COL_HTML_CONTENT)) {
            parsedPart.clear();
            parsedPart.putAll(parsed);
            break;

          } else if (parsed.containsKey(COL_CONTENT) && !parsedPart.containsKey(COL_CONTENT)) {
            parsedPart.put(COL_CONTENT, BeeUtils.peek(parsed.get(COL_CONTENT)));
          }
        } else if (part.isMimeType("multipart/related")) {
          related.putAll(parsed);
        } else {
          parsedPart.putAll(parsed);
        }
      }
      if (!related.isEmpty()) {
        List<String> orphans = new ArrayList<>();

        if (related.containsKey(COL_FILE)) {
          orphans.addAll(related.get(COL_FILE));
        }
        if (related.containsKey(COL_HTML_CONTENT)) {
          for (String html : related.get(COL_HTML_CONTENT)) {
            String mergedHtml = html;

            if (related.containsKey(COL_FILE)) {
              for (String entry : related.get(COL_FILE)) {
                String before = mergedHtml;
                String[] arr = Codec.beeDeserializeCollection(entry);

                for (int j = 2; j < arr.length; j++) {
                  mergedHtml = mergedHtml.replace("cid:" + arr[j], "file/" + arr[0]);
                }
                if (mergedHtml != before) {
                  orphans.remove(entry);
                }
              }
            }
            parsedPart.put(COL_HTML_CONTENT, mergedHtml);
          }
        } else if (related.containsKey(COL_CONTENT)) {
          parsedPart.putAll(COL_CONTENT, related.get(COL_CONTENT));
        }
        parsedPart.putAll(COL_FILE, orphans);
      }
    } else if (part.isMimeType("message/rfc822")) {
      try {
        parsedPart.putAll(parsePart(messageId, (Message) part.getContent()));
      } catch (MessagingException e) {
        logger.warning("(MessageID=", messageId, ") Error parsing message/rfc822 part:", e);
      }
    } else {
      String contentType = null;

      try {
        contentType = new ContentType(part.getContentType()).getBaseType();
      } catch (ParseException e) {
        logger.warning("(MessageID=", messageId, ") Error getting part content type:", e);
      }
      String disposition = null;

      try {
        disposition = part.getDisposition();
      } catch (ParseException e) {
        logger.warning("(MessageID=", messageId, ") Error getting part disposition:", e);
      }
      String fileName = null;

      try {
        fileName = part.getFileName();

        if (!BeeUtils.isEmpty(fileName)) {
          fileName = MimeUtility.decodeText(fileName);
        }
      } catch (ParseException e) {
        logger.warning("(MessageID=", messageId, ") Error getting part file name:", e);
      }
      if (BeeUtils.same(disposition, Part.ATTACHMENT)
          || !BeeUtils.isEmpty(fileName)
          || !part.isMimeType("text/*")) {

        List<String> fileInfo = new ArrayList<>();
        fileInfo.add(BeeUtils.toString(fs.storeFile(part.getInputStream(), fileName, contentType)));
        fileInfo.add(fileName);

        String[] ids = part.getHeader("Content-ID");

        if (!ArrayUtils.isEmpty(ids)) {
          for (String id : ids) {
            fileInfo.add(BeeUtils.removeSuffix(BeeUtils.removePrefix(id, '<'), '>'));
          }
        }
        parsedPart.put(COL_FILE, Codec.beeSerialize(fileInfo));

      } else {
        String content = getStringContent(part.getContent());

        if (!BeeUtils.isEmpty(content)) {
          parsedPart.put(part.isMimeType("text/html") ? COL_HTML_CONTENT : COL_CONTENT, content);
        }
      }
    }
    return parsedPart;
  }

  private void setRelations(Long messageId, Set<Long> addresses) {
    Long[] adr = null;

    if (!BeeUtils.isEmpty(addresses)) {
      adr = qs.getLongColumn(new SqlSelect()
          .addField(TBL_EMAILS, sys.getIdName(TBL_EMAILS), COL_EMAIL)
          .addFrom(TBL_EMAILS)
          .addFromLeft(TBL_ACCOUNTS,
              sys.joinTables(TBL_EMAILS, TBL_ACCOUNTS, MailConstants.COL_ADDRESS))
          .setWhere(SqlUtils.and(sys.idInList(TBL_EMAILS, addresses),
              SqlUtils.isNull(TBL_ACCOUNTS, COL_ACCOUNT_PRIVATE))));
    }
    if (ArrayUtils.isEmpty(adr)) {
      return;
    }
    IsCondition clause = SqlUtils.inList(TBL_CONTACTS, COL_EMAIL, (Object[]) adr);

    Long[] companies = qs.getLongColumn(new SqlSelect().setUnionAllMode(false)
        .addField(TBL_COMPANIES, sys.getIdName(TBL_COMPANIES), COL_COMPANY)
        .addFrom(TBL_COMPANIES)
        .addFromInner(TBL_CONTACTS, SqlUtils.and(clause,
            sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT)))
        .addUnion(new SqlSelect()
            .addFields(TBL_COMPANY_CONTACTS, COL_COMPANY)
            .addFrom(TBL_COMPANY_CONTACTS)
            .addFromInner(TBL_CONTACTS, SqlUtils.and(clause,
                sys.joinTables(TBL_CONTACTS, TBL_COMPANY_CONTACTS, COL_CONTACT))))
        .addUnion(new SqlSelect()
            .addFields(TBL_COMPANY_PERSONS, COL_COMPANY)
            .addFrom(TBL_COMPANY_PERSONS)
            .addFromInner(TBL_CONTACTS, SqlUtils.and(clause,
                sys.joinTables(TBL_CONTACTS, TBL_COMPANY_PERSONS, COL_CONTACT)))));

    if (!ArrayUtils.isEmpty(companies)) {
      Long[] sysCompanies = qs.getLongColumn(new SqlSelect().setDistinctMode(true)
          .addFields(TBL_COMPANY_PERSONS, COL_COMPANY)
          .addFrom(TBL_USERS)
          .addFromInner(TBL_COMPANY_PERSONS,
              sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS, COL_COMPANY_PERSON)));

      List<Long> otherCompanies = new ArrayList<>();
      boolean ok = false;

      for (Long company : companies) {
        if (ArrayUtils.contains(sysCompanies, company)) {
          ok = true;
        } else {
          otherCompanies.add(company);
        }
      }
      if (ok) {
        for (Long company : otherCompanies) {
          qs.insertData(new SqlInsert(TBL_RELATIONS)
              .addConstant(COL_MESSAGE, messageId)
              .addConstant(COL_COMPANY, company));
        }
      }
    }
  }

  private Long storeAddress(Long userId, InternetAddress address) throws AddressException {
    Assert.notNull(address);

    address.validate();

    String label = address.getPersonal();
    String email = BeeUtils.normalize(address.getAddress());

    Assert.notEmpty(email);

    Holder<Long> emailId = Holder.absent();

    cb.synchronizedCall(new Runnable() {
      @Override
      public void run() {
        QueryServiceBean queryBean = Invocation.locateRemoteBean(QueryServiceBean.class);

        emailId.set(queryBean.getLong(new SqlSelect()
            .addFields(TBL_EMAILS, sys.getIdName(TBL_EMAILS))
            .addFrom(TBL_EMAILS)
            .setWhere(SqlUtils.equals(TBL_EMAILS, COL_EMAIL_ADDRESS, email))));

        if (emailId.isNull()) {
          emailId.set(queryBean.insertData(new SqlInsert(TBL_EMAILS)
              .addConstant(COL_EMAIL_ADDRESS, email)));

          queryBean.insertData(new SqlInsert(TBL_ADDRESSBOOK)
              .addConstant(MailConstants.COL_USER, userId)
              .addConstant(COL_EMAIL, emailId.get())
              .addNotEmpty(COL_EMAIL_LABEL, label));
        } else {
          String bookIdName = sys.getIdName(TBL_ADDRESSBOOK);

          SimpleRow row = qs.getRow(new SqlSelect()
              .addFields(TBL_ADDRESSBOOK, bookIdName, COL_EMAIL_LABEL)
              .addFrom(TBL_ADDRESSBOOK)
              .setWhere(SqlUtils.equals(TBL_ADDRESSBOOK, MailConstants.COL_USER, userId,
                  COL_EMAIL, emailId.get())));

          if (row == null) {
            qs.insertData(new SqlInsert(TBL_ADDRESSBOOK)
                .addConstant(MailConstants.COL_USER, userId)
                .addConstant(COL_EMAIL, emailId.get())
                .addNotEmpty(COL_EMAIL_LABEL, label));

          } else if (BeeUtils.isEmpty(row.getValue(COL_EMAIL_LABEL)) && !BeeUtils.isEmpty(label)) {
            qs.updateData(new SqlUpdate(TBL_ADDRESSBOOK)
                .addConstant(COL_EMAIL_LABEL, label)
                .setWhere(sys.idEquals(TBL_ADDRESSBOOK, row.getLong(bookIdName))));
          }
        }
      }
    });
    return emailId.get();
  }

  private long storePlace(long messageId, Long folderId, Integer flags, Long messageUID) {
    long placeId = qs.insertData(new SqlInsert(TBL_PLACES)
        .addConstant(COL_MESSAGE, messageId)
        .addConstant(COL_FOLDER, folderId)
        .addNotNull(COL_FLAGS, flags)
        .addNotNull(COL_MESSAGE_UID, messageUID));

    if (repliedStack.containsRow(folderId)) {
      String uniqueId = qs.getValue(new SqlSelect()
          .addFields(TBL_MESSAGES, COL_UNIQUE_ID)
          .addFrom(TBL_MESSAGES)
          .setWhere(sys.idEquals(TBL_MESSAGES, messageId)));

      repliedStackLock.lock();
      Long repliedFrom = repliedStack.remove(folderId, uniqueId);
      repliedStackLock.unlock();

      if (DataUtils.isId(repliedFrom)) {
        qs.updateData(new SqlUpdate(TBL_PLACES)
            .addConstant(COL_REPLIED, placeId)
            .setWhere(sys.idEquals(TBL_PLACES, repliedFrom)));
      }
    }
    return placeId;
  }
}
