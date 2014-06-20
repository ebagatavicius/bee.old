package com.butent.bee.server.modules.mail;

import com.google.common.base.Objects;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.mail.MailConstants.AddressType;
import com.butent.bee.shared.modules.mail.MailConstants.MessageFlag;
import com.butent.bee.shared.modules.mail.MailConstants.SystemFolder;
import com.butent.bee.shared.modules.mail.MailFolder;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
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
import javax.mail.internet.ParseException;

@Stateless
@LocalBean
public class MailStorageBean {

  private static final BeeLogger logger = LogUtils.getLogger(MailStorageBean.class);

  private static String getStringContent(Object enigma) throws IOException {
    String content;

    if (enigma instanceof String) {
      content = (String) enigma;

    } else if (enigma instanceof InputStream) {
      content = CharStreams.toString(new InputStreamReader((InputStream) enigma,
          BeeConst.CHARSET_UTF8));
    } else {
      content = enigma.toString();
    }
    return content;
  }

  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;
  @EJB
  FileStorageBean fs;
  @Resource
  SessionContext ctx;

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

  public MailAccount getAccount(Long accountId) {
    Assert.state(DataUtils.isId(accountId));
    return getAccount(sys.idEquals(TBL_ACCOUNTS, accountId), false);
  }

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

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Long storeMail(Long userId, Message message, Long folderId, Long messageUID)
      throws MessagingException {

    MailEnvelope envelope = new MailEnvelope(message);
    Long messageId = null;
    Long placeId = null;

    SimpleRow data = qs.getRow(new SqlSelect()
        .addField(TBL_MESSAGES, sys.getIdName(TBL_MESSAGES), COL_MESSAGE)
        .addMax(TBL_PLACES, sys.getIdName(TBL_PLACES), COL_UNIQUE_ID)
        .addFrom(TBL_MESSAGES)
        .addFromLeft(TBL_PLACES,
            SqlUtils.and(sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE),
                SqlUtils.equals(TBL_PLACES, COL_FOLDER, folderId),
                messageUID == null ? SqlUtils.isNull(TBL_PLACES, COL_MESSAGE_UID)
                    : SqlUtils.equals(TBL_PLACES, COL_MESSAGE_UID, messageUID)))
        .setWhere(SqlUtils.equals(TBL_MESSAGES, COL_UNIQUE_ID, envelope.getUniqueId()))
        .addGroup(TBL_MESSAGES, sys.getIdName(TBL_MESSAGES)));

    if (data != null) {
      messageId = data.getLong(COL_MESSAGE);
      placeId = data.getLong(COL_UNIQUE_ID);
    }
    if (!DataUtils.isId(messageId)) {
      Long fileId;
      InputStream is = null;
      Long senderId = null;
      InternetAddress sender = envelope.getSender();

      if (sender != null) {
        try {
          senderId = storeAddress(userId, sender);
        } catch (AddressException e) {
          logger.error(e);
        }
      }
      try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.writeTo(bos);
        is = new ByteArrayInputStream(bos.toByteArray());
        String contentType = message.getContentType();

        fileId = fs.storeFile(is, envelope.getMessageId(), !BeeUtils.isEmpty(contentType)
            ? new ContentType(message.getContentType()).getBaseType() : null);
      } catch (IOException e) {
        throw new MessagingException(e.toString());
      }
      messageId = qs.insertData(new SqlInsert(TBL_MESSAGES)
          .addConstant(COL_UNIQUE_ID, envelope.getUniqueId())
          .addConstant(COL_DATE, envelope.getDate())
          .addNotNull(COL_SENDER, senderId)
          .addConstant(COL_SUBJECT, envelope.getSubject())
          .addConstant(COL_RAW_CONTENT, fileId));

      Set<Long> allAddresses = Sets.newHashSet();

      for (Entry<AddressType, InternetAddress> entry : envelope.getRecipients().entries()) {
        try {
          Long adr = storeAddress(userId, entry.getValue());

          if (allAddresses.add(adr)) {
            qs.insertData(new SqlInsert(TBL_RECIPIENTS)
                .addConstant(COL_MESSAGE, messageId)
                .addConstant(MailConstants.COL_ADDRESS, adr)
                .addConstant(COL_ADDRESS_TYPE, entry.getKey().name()));
          }
        } catch (AddressException e) {
          logger.error(e);
        }
      }
      try {
        is.reset();
        storePart(messageId, new MimeMessage(null, is), null, null);
      } catch (IOException e) {
        throw new MessagingException(e.toString());
      }
    }
    if (!DataUtils.isId(placeId)) {
      placeId = qs.insertData(new SqlInsert(TBL_PLACES)
          .addConstant(COL_MESSAGE, messageId)
          .addConstant(COL_FOLDER, folderId)
          .addConstant(COL_FLAGS, envelope.getFlagMask())
          .addConstant(COL_MESSAGE_UID, messageUID));
    } else {
      placeId = null;
    }
    return placeId;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public long syncFolder(Long userId, MailFolder localFolder, Folder remoteFolder)
      throws MessagingException {
    Assert.noNulls(localFolder, remoteFolder);

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_PLACES, COL_FLAGS, COL_MESSAGE_UID)
        .addField(TBL_PLACES, sys.getIdName(TBL_PLACES), COL_UNIQUE_ID)
        .addFrom(TBL_PLACES)
        .setWhere(SqlUtils.equals(TBL_PLACES, COL_FOLDER, localFolder.getId()))
        .addOrderDesc(TBL_PLACES, COL_MESSAGE_UID)
        .setLimit(100));

    long lastUid = BeeUtils.unbox(data.getLong(0, COL_MESSAGE_UID));

    if (data.getNumberOfRows() > 0) {
      Set<Long> syncedMsgs = Sets.newHashSet();

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
          Long id = row.getLong(COL_UNIQUE_ID);

          if (BeeUtils.unbox(row.getInt(COL_FLAGS)) != BeeUtils.unbox(flags)) {
            qs.updateData(new SqlUpdate(TBL_PLACES)
                .addConstant(COL_FLAGS, flags)
                .setWhere(sys.idEquals(TBL_PLACES, id)));
          }
          syncedMsgs.add(id);
        } else {
          try {
            storeMail(userId, message, localFolder.getId(), uid);
          } catch (MessagingException e) {
            logger.error(e);
          }
        }
      }
      List<Long> deletedMsgs = Lists.newArrayList();

      for (int i = 0; i < data.getNumberOfRows(); i++) {
        Long id = data.getLong(i, COL_UNIQUE_ID);

        if (!syncedMsgs.contains(id)) {
          deletedMsgs.add(id);
        }
      }
      if (!deletedMsgs.isEmpty()) {
        qs.updateData(new SqlDelete(TBL_PLACES)
            .setWhere(SqlUtils.inList(TBL_PLACES, sys.getIdName(TBL_PLACES), deletedMsgs)));
      }
    }
    return lastUid;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void validateFolder(MailFolder folder, Long uidValidity) {
    Assert.notNull(folder);

    if (!Objects.equal(uidValidity, folder.getUidValidity())) {
      qs.updateData(new SqlDelete(TBL_PLACES)
          .setWhere(SqlUtils.equals(TBL_PLACES, COL_FOLDER, folder.getId())));

      qs.updateData(new SqlUpdate(TBL_FOLDERS)
          .addConstant(COL_FOLDER_UID, uidValidity)
          .setWhere(sys.idEquals(TBL_FOLDERS, folder.getId())));

      folder.setUidValidity(uidValidity);
    }
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

  private Long storeAddress(Long userId, InternetAddress address) throws AddressException {
    Assert.notNull(address);
    String email;
    String label = null;

    address.validate();

    label = address.getPersonal();
    email = BeeUtils.normalize(address.getAddress());

    Assert.notEmpty(email);

    Long id = null;
    Long bookId = null;
    String bookLabel = null;
    String bookIdName = sys.getIdName(TBL_ADDRESSBOOK);

    SimpleRow row = qs.getRow(new SqlSelect()
        .addField(TBL_EMAILS, sys.getIdName(TBL_EMAILS), COL_EMAIL)
        .addFields(TBL_ADDRESSBOOK, bookIdName, COL_EMAIL_LABEL)
        .addFrom(TBL_EMAILS)
        .addFromLeft(TBL_ADDRESSBOOK,
            SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, userId)))
        .setWhere(SqlUtils.equals(TBL_EMAILS, COL_EMAIL_ADDRESS, email)));

    if (row != null) {
      id = row.getLong(COL_EMAIL);
      bookId = row.getLong(bookIdName);
      bookLabel = row.getValue(COL_EMAIL_LABEL);
    }
    if (!DataUtils.isId(id)) {
      id = qs.insertData(new SqlInsert(TBL_EMAILS)
          .addConstant(COL_EMAIL_ADDRESS, email));
    }
    if (BeeUtils.isEmpty(bookLabel) && !BeeUtils.isEmpty(label)) {
      if (DataUtils.isId(bookId)) {
        qs.updateData(new SqlUpdate(TBL_ADDRESSBOOK)
            .addConstant(COL_EMAIL_LABEL, label)
            .setWhere(sys.idEquals(TBL_ADDRESSBOOK, bookId)));
      } else {
        qs.insertData(new SqlInsert(TBL_ADDRESSBOOK)
            .addConstant(COL_USER, userId)
            .addConstant(COL_EMAIL, id)
            .addConstant(COL_EMAIL_LABEL, label));
      }
    }
    return id;
  }

  private void storePart(Long messageId, Part part, List<Pair<String, String>> contents,
      Map<String, Long> attachments) throws MessagingException, IOException {

    if (part.isMimeType("multipart/alternative")) {
      String text = null;
      String html = null;
      Multipart multiPart = (Multipart) part.getContent();

      for (int i = 0; i < multiPart.getCount(); i++) {
        Part bodyPart = multiPart.getBodyPart(i);

        if (bodyPart.isMimeType("text/plain")) {
          text = getStringContent(bodyPart.getContent());

        } else if (bodyPart.isMimeType("text/html")) {
          html = getStringContent(bodyPart.getContent());

        } else {
          storePart(messageId, bodyPart, contents, attachments);
        }
      }
      if (contents != null) {
        contents.add(Pair.of(text, html));
      } else {
        savePart(messageId, text, html);
      }
    } else if (part.isMimeType("multipart/*")) {
      Multipart multiPart = (Multipart) part.getContent();
      List<Pair<String, String>> cont = null;
      Map<String, Long> attach = null;

      boolean isRelated = part.isMimeType("multipart/related");

      if (isRelated) {
        cont = new ArrayList<>();
        attach = new LinkedHashMap<>();
      } else {
        cont = contents;
        attach = attachments;
      }
      for (int i = 0; i < multiPart.getCount(); i++) {
        storePart(messageId, multiPart.getBodyPart(i), cont, attach);
      }
      if (isRelated) {
        for (Pair<String, String> pair : cont) {
          if (!BeeUtils.isEmpty(pair.getB())) {
            for (String id : attach.keySet()) {
              pair.setB(pair.getB().replace("cid:" + id, "file/" + attach.get(id)));
            }
          }
          if (contents != null) {
            contents.add(pair);
          } else {
            savePart(messageId, pair.getA(), pair.getB());
          }
        }
      }
    } else if (part.isMimeType("message/rfc822")) {
      storePart(messageId, (Message) part.getContent(), contents, attachments);
    } else {
      String contentType = null;

      try {
        contentType = new ContentType(part.getContentType()).getBaseType();
      } catch (ParseException e) {
        logger.warning("( MessageID =", messageId, ") Error getting part content type:", e);
      }
      String disposition = null;

      try {
        disposition = part.getDisposition();
      } catch (ParseException e) {
        logger.warning("( MessageID =", messageId, ") Error getting part disposition:", e);
      }
      String fileName = null;

      try {
        fileName = part.getFileName();
      } catch (ParseException e) {
        logger.warning("( MessageID =", messageId, ") Error getting part file name:", e);
      }
      boolean isBase64 = BeeUtils.same("base64",
          ArrayUtils.getQuietly(part.getHeader("Content-Transfer-Encoding"), 0));

      if (!part.isMimeType("text/*")
          || BeeUtils.same(disposition, Part.ATTACHMENT)
          || !BeeUtils.isEmpty(fileName)
          || isBase64) {

        Long fileId = fs.storeFile(part.getInputStream(), fileName, contentType);

        if (attachments != null) {
          String[] ids = part.getHeader("Content-ID");

          if (ids != null) {
            for (String id : ids) {
              attachments.put(BeeUtils.removeSuffix(BeeUtils.removePrefix(id, '<'), '>'), fileId);
            }
          }
        }
        qs.insertData(new SqlInsert(TBL_ATTACHMENTS)
            .addConstant(COL_MESSAGE, messageId)
            .addConstant(AdministrationConstants.COL_FILE, fileId)
            .addConstant(COL_ATTACHMENT_NAME, fileName));
      } else {
        String text = getStringContent(part.getContent());
        String html = null;

        if (part.isMimeType("text/html")) {
          html = text;
          text = HtmlUtils.stripHtml(html);
        }
        if (contents != null) {
          contents.add(Pair.of(text, html));
        } else {
          savePart(messageId, text, html);
        }
      }
    }
  }

  private void savePart(Long messageId, String text, String html) {
    if (BeeUtils.anyNotEmpty(text, html)) {
      qs.insertData(new SqlInsert(TBL_PARTS)
          .addConstant(COL_MESSAGE, messageId)
          .addConstant(COL_CONTENT, BeeUtils.isEmpty(text) ? HtmlUtils.stripHtml(html) : text)
          .addConstant(COL_HTML_CONTENT, html));
    }
  }
}
