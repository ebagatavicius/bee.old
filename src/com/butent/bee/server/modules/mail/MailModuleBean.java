package com.butent.bee.server.modules.mail;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.BeeView.ConditionProvider;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.server.modules.mail.proxy.MailProxy;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsFrom;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.io.StoredFile;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.mail.MailConstants.AddressType;
import com.butent.bee.shared.modules.mail.MailConstants.MessageFlag;
import com.butent.bee.shared.modules.mail.MailConstants.Protocol;
import com.butent.bee.shared.modules.mail.MailConstants.SystemFolder;
import com.butent.bee.shared.modules.mail.MailFolder;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.websocket.messages.MailMessage;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

@Stateless
@LocalBean
public class MailModuleBean implements BeeModule {

  private static final BeeLogger logger = LogUtils.getLogger(MailModuleBean.class);

  @EJB
  MailProxy proxy;
  @EJB
  MailStorageBean mail;
  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;
  @EJB
  FileStorageBean fs;
  @EJB
  ParamHolderBean prm;

  @Resource
  SessionContext ctx;
  @EJB
  NewsBean news;

  @Asynchronous
  public void checkMail(final MailAccount account, MailFolder localFolder, String progressId) {
    Assert.noNulls(account, localFolder);
    Store store = null;
    int c = 0;
    int f = 0;
    String error = null;

    if (localFolder.isConnected()) {
      try {
        store = account.connectToStore();
        c = checkFolder(account, account.getRemoteFolder(store, localFolder), localFolder,
            progressId);

        if (account.isInbox(localFolder)) {
          f = syncFolders(account, account.getRemoteFolder(store, account.getRootFolder()),
              account.getRootFolder());
        }
      } catch (Exception e) {
        ctx.setRollbackOnly();
        logger.error(e, "LOGIN:", account.getStoreLogin());
        error = BeeUtils.joinWords(account.getStoreLogin(), e.getMessage());
      } finally {
        account.disconnectFromStore(store);
      }
    }
    if (!BeeUtils.isEmpty(progressId)) {
      Endpoint.closeProgress(progressId);
    }
    MailMessage mailMessage = new MailMessage(BeeUtils.isEmpty(progressId) ? null
        : localFolder.getId());
    mailMessage.setMessagesUpdated(c > 0);
    mailMessage.setFoldersUpdated(f > 0);
    mailMessage.setError(error);
    Endpoint.sendToUser(account.getUserId(), mailMessage);
  }

  @Override
  public List<SearchResult> doSearch(String query) {
    return null;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    try {
      if (BeeUtils.same(svc, SVC_RESTART_PROXY)) {
        response = proxy.initServer();
        response.log(logger);

      } else if (BeeUtils.same(svc, SVC_GET_ACCOUNTS)) {
        response = ResponseObject.response(qs.getData(new SqlSelect()
            .addField(TBL_ACCOUNTS, sys.getIdName(TBL_ACCOUNTS), COL_ACCOUNT)
            .addFields(TBL_ACCOUNTS, MailConstants.COL_ADDRESS, COL_USER, COL_ACCOUNT_DESCRIPTION,
                COL_ACCOUNT_DEFAULT, COL_SIGNATURE,
                SystemFolder.Inbox.name() + COL_FOLDER,
                SystemFolder.Drafts.name() + COL_FOLDER,
                SystemFolder.Sent.name() + COL_FOLDER,
                SystemFolder.Trash.name() + COL_FOLDER)
            .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
            .addFrom(TBL_ACCOUNTS)
            .addFromInner(TBL_EMAILS,
                sys.joinTables(TBL_EMAILS, TBL_ACCOUNTS, MailConstants.COL_ADDRESS))
            .setWhere(SqlUtils.equals(TBL_ACCOUNTS, COL_USER,
                BeeUtils.toLongOrNull(reqInfo.getParameter(COL_USER))))
            .addOrder(TBL_ACCOUNTS, COL_ACCOUNT_DESCRIPTION)));

      } else if (BeeUtils.same(svc, SVC_GET_MESSAGE)) {
        response = getMessage(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_PLACE)),
            Codec.unpack(reqInfo.getParameter("showBcc")));

      } else if (BeeUtils.same(svc, SVC_FLAG_MESSAGE)) {
        response = ResponseObject
            .response(setMessageFlag(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_PLACE)),
                EnumUtils.getEnumByName(MessageFlag.class, reqInfo.getParameter(COL_FLAGS)),
                Codec.unpack(reqInfo.getParameter("on"))));

      } else if (BeeUtils.same(svc, SVC_COPY_MESSAGES)) {
        MailAccount account = mail.getAccount(BeeUtils.toLong(reqInfo.getParameter(COL_ACCOUNT)));
        Long targetId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_FOLDER));
        MailFolder target = account.findFolder(targetId);

        if (target == null) {
          response = ResponseObject.error("Folder does not exist: ID =", targetId);
        } else {
          response = processMessages(account,
              account.findFolder(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_FOLDER_PARENT))),
              target, Codec.beeDeserializeCollection(reqInfo.getParameter(COL_PLACE)),
              BeeUtils.toBoolean(reqInfo.getParameter("move")));
        }
      } else if (BeeUtils.same(svc, SVC_REMOVE_MESSAGES)) {
        MailAccount account = mail.getAccount(BeeUtils.toLong(reqInfo.getParameter(COL_ACCOUNT)));

        response = processMessages(account,
            account.findFolder(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_FOLDER))),
            Codec.unpack(reqInfo.getParameter("Purge")) ? null : account.getTrashFolder(),
            Codec.beeDeserializeCollection(reqInfo.getParameter(COL_PLACE)), true);

      } else if (BeeUtils.same(svc, SVC_GET_FOLDERS)) {
        MailAccount account = mail.getAccount(sys.idEquals(TBL_ACCOUNTS,
            BeeUtils.toLong(reqInfo.getParameter(COL_ACCOUNT))), true);

        response = ResponseObject.response(account.getRootFolder());

      } else if (BeeUtils.same(svc, SVC_CREATE_FOLDER)) {
        MailAccount account = mail.getAccount(BeeUtils.toLong(reqInfo.getParameter(COL_ACCOUNT)));
        Long folderId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_FOLDER));
        MailFolder parent;

        if (DataUtils.isId(folderId)) {
          parent = account.findFolder(folderId);
        } else {
          parent = account.getInboxFolder();
        }
        if (parent == null) {
          response = ResponseObject.error("Folder does not exist: ID =", folderId);
        } else {
          String name = reqInfo.getParameter(COL_FOLDER_NAME);
          boolean ok = account.createRemoteFolder(parent, name, false);

          if (ok) {
            mail.createFolder(account, parent, name);
            response = ResponseObject.info("Folder created:", name);
          } else {
            response = ResponseObject.error("Cannot create folder:", name);
          }
        }
      } else if (BeeUtils.same(svc, SVC_DISCONNECT_FOLDER)) {
        MailAccount account = mail.getAccount(BeeUtils.toLong(reqInfo.getParameter(COL_ACCOUNT)));
        Long folderId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_FOLDER));
        MailFolder folder = account.findFolder(folderId);

        if (folder == null) {
          response = ResponseObject.error("Folder does not exist: ID =", folderId);
        } else {
          disconnectFolder(account, folder);
          response = ResponseObject.info("Folder disconnected: " + folder.getName());
        }
      } else if (BeeUtils.same(svc, SVC_RENAME_FOLDER)) {
        MailAccount account = mail.getAccount(BeeUtils.toLong(reqInfo.getParameter(COL_ACCOUNT)));
        Long folderId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_FOLDER));
        MailFolder folder = account.findFolder(folderId);

        if (folder == null) {
          response = ResponseObject.error("Folder does not exist: ID =", folderId);
        } else {
          String name = reqInfo.getParameter(COL_FOLDER_NAME);

          if (account.renameRemoteFolder(folder, name)) {
            mail.renameFolder(folder, name);
            response = ResponseObject.info("Folder renamed: " + folder.getName() + "->" + name);
          } else {
            response = ResponseObject.error("Cannot rename folder", folder.getName(), "to", name);
          }
        }
      } else if (BeeUtils.same(svc, SVC_DROP_FOLDER)) {
        MailAccount account = mail.getAccount(BeeUtils.toLong(reqInfo.getParameter(COL_ACCOUNT)));
        Long folderId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_FOLDER));
        MailFolder folder = account.findFolder(folderId);

        if (folder == null) {
          response = ResponseObject.error("Folder does not exist: ID =", folderId);
        } else {
          if (account.dropRemoteFolder(folder)) {
            mail.dropFolder(folder);
            response = ResponseObject.info("Folder dropped: " + folder.getName());
          } else {
            response = ResponseObject.error("Cannot drop folder", folder.getName());
          }
        }
      } else if (BeeUtils.same(svc, SVC_CHECK_MAIL)) {
        MailAccount account = mail.getAccount(BeeUtils.toLong(reqInfo.getParameter(COL_ACCOUNT)));
        Long folderId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_FOLDER));
        MailFolder folder = account.findFolder(folderId);

        if (folder == null) {
          response = ResponseObject.error("Folder does not exist: ID =", folderId);
        } else {
          ctx.getBusinessObject(this.getClass()).checkMail(account, folder,
              reqInfo.getParameter(Service.VAR_PROGRESS));
          response = ResponseObject.emptyResponse();
        }
      } else if (BeeUtils.same(svc, SVC_SEND_MAIL)) {
        response = new ResponseObject();
        boolean save = BeeUtils.toBoolean(reqInfo.getParameter("Save"));
        Long draftId = BeeUtils.toLongOrNull(reqInfo.getParameter("DraftId"));
        Long accountId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ACCOUNT));
        String[] to = Codec.beeDeserializeCollection(reqInfo.getParameter(AddressType.TO.name()));
        String[] cc = Codec.beeDeserializeCollection(reqInfo.getParameter(AddressType.CC.name()));
        String[] bcc = Codec.beeDeserializeCollection(reqInfo.getParameter(AddressType.BCC.name()));
        String subject = reqInfo.getParameter(COL_SUBJECT);
        String content = reqInfo.getParameter(COL_CONTENT);

        if (DataUtils.isId(draftId)) {
          MailAccount account = mail.getAccount(qs.getLong(new SqlSelect()
              .addFields(TBL_FOLDERS, COL_ACCOUNT)
              .addFrom(TBL_PLACES)
              .addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
              .setWhere(sys.idEquals(TBL_PLACES, draftId))));

          processMessages(account, account.getDraftsFolder(), null,
              new String[] {BeeUtils.toString(draftId)}, true);
        }
        List<StoredFile> attachments = Lists.newArrayList();

        for (Long fileId : DataUtils.parseIdSet(reqInfo.getParameter("Attachments"))) {
          try {
            StoredFile fileInfo = fs.getFile(fileId);
            attachments.add(fileInfo);
          } catch (IOException e) {
            logger.error(e);
            response.addError(e);
          }
        }
        if (!save) {
          try {
            sendMail(accountId, to, cc, bcc, subject, content, attachments, true);
            response.addInfo(usr.getLocalizableConstants().mailMessageSent());

          } catch (MessagingException e) {
            save = true;
            logger.error(e);
            response.addError(e);
          }
        }
        if (save) {
          MailAccount account = mail.getAccount(accountId);
          MailFolder folder = account.getDraftsFolder();
          MimeMessage message = buildMessage(account, to, cc, bcc, subject, content, attachments);
          message.setFlag(Flag.SEEN, true);

          if (!account.addMessageToRemoteFolder(message, folder)) {
            mail.storeMail(account.getUserId(), message, folder.getId(), null);
          }
          response.addInfo(usr.getLocalizableConstants().mailMessageIsSavedInDraft());
        }
        for (StoredFile fileInfo : attachments) {
          if (fileInfo.isTemporary()) {
            logger.debug("File deleted:", fileInfo.getPath(),
                new File(fileInfo.getPath()).delete());
          }
        }
      } else if (BeeUtils.same(svc, SVC_GET_USABLE_CONTENT)) {
        response = getUsableContent(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_MESSAGE)));

      } else {
        String msg = BeeUtils.joinWords("Mail service not recognized:", svc);
        logger.warning(msg);
        response = ResponseObject.error(msg);
      }
    } catch (MessagingException e) {
      ctx.setRollbackOnly();
      logger.error(e);
      response = ResponseObject.error(e);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    List<BeeParameter> params = Lists.newArrayList(
        BeeParameter.createRelation(module, PRM_DEFAULT_ACCOUNT, false,
            TBL_ACCOUNTS, COL_ACCOUNT_DESCRIPTION),
        BeeParameter.createText(module, "POP3Server", false, null),
        BeeParameter.createNumber(module, "POP3ServerPort", false, null),
        BeeParameter.createNumber(module, "POP3BindPort", false, null),
        BeeParameter.createText(module, "SMTPServer", false, null),
        BeeParameter.createNumber(module, "SMTPServerPort", false, null),
        BeeParameter.createNumber(module, "SMTPBindPort", false, null));

    return params;
  }

  @Override
  public Module getModule() {
    return Module.MAIL;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public void init() {
    proxy.initServer();

    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      public void initAccount(ViewInsertEvent event) {
        if (event.isTarget(TBL_ACCOUNTS) && event.isAfter()) {
          mail.initAccount(event.getRow().getId());
        }
      }

      @Subscribe
      public void modifyUserEmailsQuery(ViewQueryEvent event) {
        if (event.isTarget("UserEmails") && event.isBefore()) {
          for (Iterator<IsFrom> iterator = event.getQuery().getFrom().iterator();
          iterator.hasNext();) {

            IsFrom from = iterator.next();

            if (from.getSource() instanceof String
                && BeeUtils.same((String) from.getSource(), TBL_ADDRESSBOOK)) {

              iterator.remove();

              BeeView view = sys.getView(event.getTargetName());
              String src = (String) from.getSource();
              String als = BeeUtils.notEmpty(from.getAlias(), src);

              event.getQuery().addFromLeft(src, als,
                  SqlUtils.and(sys.joinTables(view.getSourceName(), view.getSourceAlias(), als,
                      COL_EMAIL), SqlUtils.equals(als, COL_USER, usr.getCurrentUserId())));
              break;
            }
          }
        }
      }
    });

    BeeView.registerConditionProvider(FILTER_SEARCH, new ConditionProvider() {
      @Override
      public IsCondition getCondition(BeeView view, List<String> args) {
        Map<String, String> keys = Codec.deserializeMap(args.get(0));
        String search = keys.get(FILTER_SEARCH);

        if (BeeUtils.isEmpty(search)) {
          return null;
        }
        SqlSelect query = new SqlSelect().setDistinctMode(true)
            .addFields(TBL_PLACES, COL_MESSAGE)
            .addFrom(TBL_PLACES)
            .addFromInner(TBL_MESSAGES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
            .addFromLeft(TBL_PARTS, sys.joinTables(TBL_MESSAGES, TBL_PARTS, COL_MESSAGE))
            .setWhere(SqlUtils.and(SqlUtils.equals(TBL_PLACES, COL_FOLDER,
                BeeUtils.toLong(keys.get(COL_FOLDER))),
                SqlUtils.or(SqlUtils.contains(TBL_EMAILS, COL_EMAIL_ADDRESS, search),
                    SqlUtils.contains(TBL_ADDRESSBOOK, COL_EMAIL_LABEL, search),
                    SqlUtils.contains(TBL_MESSAGES, COL_SUBJECT, search),
                    SqlUtils.contains(TBL_PARTS, COL_CONTENT, search))));

        if (BeeUtils.toBoolean(keys.get(SystemFolder.Sent.name()))) {
          query.addFromLeft(TBL_RECIPIENTS,
              sys.joinTables(TBL_MESSAGES, TBL_RECIPIENTS, COL_MESSAGE))
              .addFromLeft(TBL_EMAILS,
                  sys.joinTables(TBL_EMAILS, TBL_RECIPIENTS, MailConstants.COL_ADDRESS));
        } else {
          query.addFromLeft(TBL_EMAILS, sys.joinTables(TBL_EMAILS, TBL_MESSAGES, COL_SENDER));
        }
        return SqlUtils.in(TBL_PLACES, COL_MESSAGE,
            query.addFromLeft(TBL_ADDRESSBOOK,
                SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                    SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, usr.getCurrentUserId()))));
      }
    });

    news.registerUsageQueryProvider(Feed.MAIL, new UsageQueryProvider() {
      @Override
      public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
          DateTime startDate) {

        return new SqlSelect()
            .addFields(TBL_PLACES, sys.getIdName(TBL_PLACES))
            .addEmptyDateTime(COL_DATE)
            .addFrom(TBL_PLACES)
            .addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
            .addFromInner(TBL_ACCOUNTS, sys.joinTables(TBL_ACCOUNTS, TBL_FOLDERS, COL_ACCOUNT))
            .setWhere(SqlUtils.and(SqlUtils.equals(TBL_ACCOUNTS, COL_USER, userId),
                SqlUtils.or(SqlUtils.isNull(TBL_PLACES, COL_FLAGS),
                    SqlUtils.equals(SqlUtils.bitAnd(TBL_PLACES, COL_FLAGS,
                        MessageFlag.SEEN.getMask()), 0))));
      }

      @Override
      public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
          DateTime startDate) {

        return new SqlSelect().addConstant(0, "dummy").setWhere(SqlUtils.sqlFalse());
      }
    });
  }

  public Long getSenderAccountId(String logLabel) {
    Long account = prm.getRelation(MailConstants.PRM_DEFAULT_ACCOUNT);

    if (!DataUtils.isId(account)) {
      logger.info(logLabel, "sender account not specified",
          BeeUtils.bracket(MailConstants.PRM_DEFAULT_ACCOUNT));
    }
    return account;
  }

  public ResponseObject sendMail(Long account, String to, String subject, String content) {
    return sendMail(account, new String[] {to}, subject, content);
  }

  public ResponseObject sendMail(Long account, String[] to, String subject, String content) {
    try {
      sendMail(account, to, null, null, subject, content, null, false);
    } catch (MessagingException ex) {
      logger.error(ex);
      return ResponseObject.error(ex);
    }
    return ResponseObject.emptyResponse();
  }

  public void sendMail(Long accountId, String[] to, String[] cc, String[] bcc, String subject,
      String content, List<StoredFile> attachments, boolean store) throws MessagingException {

    Assert.state(DataUtils.isId(accountId));
    MailAccount account = mail.getAccount(accountId);
    Transport transport = null;

    try {
      MimeMessage message = buildMessage(account, to, cc, bcc, subject, content, attachments);
      Address[] recipients = message.getAllRecipients();

      if (recipients == null || recipients.length == 0) {
        throw new MessagingException("No recipients");
      }
      transport = account.connectToTransport();
      transport.sendMessage(message, recipients);

      if (store) {
        MailFolder folder = account.getSentFolder();
        message.setFlag(Flag.SEEN, true);

        if (!account.addMessageToRemoteFolder(message, folder)) {
          mail.storeMail(account.getUserId(), message, folder.getId(), null);
        }
      }
    } finally {
      if (transport != null) {
        try {
          transport.close();
        } catch (MessagingException e) {
          logger.warning(e);
        }
      }
    }
  }

  public void storeProxyMail(String content, String recipient) {
    Assert.notNull(content);
    logger.debug("GOT", BeeUtils.isEmpty(recipient) ? Protocol.SMTP : Protocol.POP3, "mail:");
    logger.debug(content);

    MimeMessage message = null;

    try {
      message = new MimeMessage(null,
          new ByteArrayInputStream(content.getBytes(BeeConst.CHARSET_UTF8)));
    } catch (MessagingException e) {
      throw new BeeRuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      throw new BeeRuntimeException(e);
    }
    MailAccount account = null;

    if (!BeeUtils.isEmpty(recipient)) {
      InternetAddress adr;

      try {
        adr = new InternetAddress(recipient, false);
        adr.validate();
      } catch (AddressException ex) {
        adr = null;
      }
      SqlSelect ss = new SqlSelect()
          .addFields(TBL_ACCOUNTS, sys.getIdName(TBL_ACCOUNTS))
          .addFrom(TBL_ACCOUNTS);
      Long accountId;

      if (adr == null) {
        accountId = qs.getLong(ss
            .setWhere(SqlUtils.equals(TBL_ACCOUNTS, COL_STORE_LOGIN, recipient)));
      } else {
        accountId = qs.getLong(ss
            .addFromInner(TBL_EMAILS,
                sys.joinTables(TBL_EMAILS, TBL_ACCOUNTS, MailConstants.COL_ADDRESS))
            .setWhere(SqlUtils.equals(TBL_EMAILS,
                COL_EMAIL_ADDRESS, recipient)));
      }
      if (DataUtils.isId(accountId)) {
        account = mail.getAccount(accountId);
      }
    }
    if (account != null) {
      try {
        mail.storeMail(account.getUserId(), message, account.getInboxFolder().getId(), null);
      } catch (MessagingException e) {
        throw new BeeRuntimeException(e);
      }
    }
  }

  private static MimeMessage buildMessage(MailAccount account, String[] to, String[] cc,
      String[] bcc, String subject, String content, List<StoredFile> attachments)
      throws MessagingException {

    MimeMessage message = new MimeMessage((Session) null);

    Map<RecipientType, String[]> recs = new HashMap<>();
    recs.put(RecipientType.TO, to);
    recs.put(RecipientType.CC, cc);
    recs.put(RecipientType.BCC, bcc);

    for (RecipientType type : recs.keySet()) {
      String[] emails = recs.get(type);

      if (!ArrayUtils.isEmpty(emails)) {
        List<Address> recipients = new ArrayList<>();

        for (String email : emails) {
          try {
            recipients.add(new InternetAddress(email, true));
          } catch (AddressException e) {
            logger.error(e);
          }
        }
        if (!BeeUtils.isEmpty(recipients)) {
          message.setRecipients(type, recipients.toArray(new Address[0]));
        }
      }
    }
    Address sender = new InternetAddress(account.getAddress(), true);
    message.setSender(sender);
    message.setFrom(sender);
    message.setSentDate(TimeUtils.toJava(new DateTime()));
    message.setSubject(subject, BeeConst.CHARSET_UTF8);

    MimeMultipart multi = null;

    if (!BeeUtils.isEmpty(attachments)) {
      multi = new MimeMultipart();

      for (StoredFile fileInfo : attachments) {
        MimeBodyPart p = new MimeBodyPart();
        File file = new File(fileInfo.getPath());

        try {
          p.attachFile(file, fileInfo.getType(), null);
          p.setFileName(fileInfo.getName());

        } catch (IOException ex) {
          logger.error(ex);
          p = null;
        }
        if (p != null) {
          multi.addBodyPart(p);
        }
      }
    }
    if (HtmlUtils.hasHtml(content)) {
      MimeMultipart mp = new MimeMultipart("alternative");

      MimeBodyPart p = new MimeBodyPart();
      p.setText(HtmlUtils.stripHtml(content), BeeConst.CHARSET_UTF8);
      mp.addBodyPart(p);

      p = new MimeBodyPart();
      p.setText(content, BeeConst.CHARSET_UTF8, "html");
      mp.addBodyPart(p);

      if (multi != null) {
        p = new MimeBodyPart();
        p.setContent(mp);
        multi.addBodyPart(p, 0);
      } else {
        multi = mp;
      }
    } else if (multi != null) {
      MimeBodyPart p = new MimeBodyPart();
      p.setText(content, BeeConst.CHARSET_UTF8);
      multi.addBodyPart(p, 0);
    }
    if (multi != null) {
      message.setContent(multi);
    } else {
      message.setText(content, BeeConst.CHARSET_UTF8);
    }
    message.saveChanges();

    return message;
  }

  private int checkFolder(MailAccount account, Folder remoteFolder, MailFolder localFolder,
      String progressId) throws MessagingException {
    Assert.noNulls(remoteFolder, localFolder);

    int c = 0;

    if (localFolder.isConnected() && account.holdsMessages(remoteFolder)) {
      boolean uidMode = remoteFolder instanceof UIDFolder;
      Long uidValidity = uidMode ? ((UIDFolder) remoteFolder).getUIDValidity() : null;

      mail.validateFolder(localFolder, uidValidity);

      try {
        remoteFolder.open(Folder.READ_ONLY);
        Message[] newMessages;

        if (uidMode) {
          long lastUid = mail.syncFolder(account.getUserId(), localFolder, remoteFolder);
          newMessages = ((UIDFolder) remoteFolder).getMessagesByUID(lastUid + 1, UIDFolder.LASTUID);
        } else {
          newMessages = remoteFolder.getMessages();
        }
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        fp.add(FetchProfile.Item.FLAGS);
        remoteFolder.fetch(newMessages, fp);
        boolean isInbox = account.isInbox(localFolder);

        for (Message message : newMessages) {
          boolean ok = mail.storeMail(account.getUserId(), message, localFolder.getId(),
              uidMode ? ((UIDFolder) remoteFolder).getUID(message) : null);

          if (ok) {
            if (isInbox) {
              // TODO applyRules(message);
              logger.debug("Message rules not implemented yet");
            }
            c++;
          }
          if (!BeeUtils.isEmpty(progressId)
              && !Endpoint.updateProgress(progressId, c / (double) newMessages.length)) {
            break;
          }
        }
      } finally {
        if (remoteFolder.isOpen()) {
          try {
            remoteFolder.close(false);
          } catch (MessagingException e) {
            logger.warning(e);
          }
        }
      }
    }
    return c;
  }

  private void disconnectFolder(MailAccount account, MailFolder folder) throws MessagingException {
    for (MailFolder subFolder : folder.getSubFolders()) {
      disconnectFolder(account, subFolder);
    }
    account.dropRemoteFolder(folder);
    mail.disconnectFolder(folder);
  }

  private ResponseObject getMessage(Long placeId, boolean showBcc) {
    Assert.notNull(placeId);

    Map<String, SimpleRowSet> packet = Maps.newHashMap();
    String drafts = SystemFolder.Drafts.name();

    SimpleRow msg = qs.getRow(new SqlSelect()
        .addFields(TBL_PLACES, COL_MESSAGE, COL_FLAGS)
        .addFields(TBL_MESSAGES, COL_DATE, COL_SUBJECT)
        .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
        .addFields(TBL_ADDRESSBOOK, COL_EMAIL_LABEL)
        .addExpr(SqlUtils.sqlIf(SqlUtils.equals(TBL_PLACES, COL_FOLDER,
            SqlUtils.field(TBL_ACCOUNTS, drafts + COL_FOLDER)),
            SqlUtils.constant(placeId), null), drafts)
        .addFrom(TBL_PLACES)
        .addFromInner(TBL_MESSAGES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
        .addFromLeft(TBL_EMAILS, sys.joinTables(TBL_EMAILS, TBL_MESSAGES, COL_SENDER))
        .addFromLeft(TBL_ADDRESSBOOK,
            SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, usr.getCurrentUserId())))
        .addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
        .addFromInner(TBL_ACCOUNTS, sys.joinTables(TBL_ACCOUNTS, TBL_FOLDERS, COL_ACCOUNT))
        .setWhere(sys.idEquals(TBL_PLACES, placeId)));

    packet.put(TBL_MESSAGES, msg.getRowSet());

    Long messageId = msg.getLong(COL_MESSAGE);
    IsCondition wh = SqlUtils.equals(TBL_RECIPIENTS, COL_MESSAGE, messageId);

    if (!showBcc) {
      wh = SqlUtils.and(wh,
          SqlUtils.notEqual(TBL_RECIPIENTS, COL_ADDRESS_TYPE, AddressType.BCC.name()));
    }
    packet.put(TBL_RECIPIENTS, qs.getData(new SqlSelect()
        .addFields(TBL_RECIPIENTS, COL_ADDRESS_TYPE)
        .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
        .addFields(TBL_ADDRESSBOOK, COL_EMAIL_LABEL)
        .addFrom(TBL_RECIPIENTS)
        .addFromInner(TBL_EMAILS,
            sys.joinTables(TBL_EMAILS, TBL_RECIPIENTS, MailConstants.COL_ADDRESS))
        .addFromLeft(TBL_ADDRESSBOOK,
            SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, usr.getCurrentUserId())))
        .setWhere(wh)
        .addOrderDesc(TBL_RECIPIENTS, COL_ADDRESS_TYPE)));

    String[] cols = new String[] {COL_CONTENT, COL_HTML_CONTENT};

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(TBL_PARTS, cols)
        .addFrom(TBL_PARTS)
        .setWhere(SqlUtils.equals(TBL_PARTS, COL_MESSAGE, messageId)));

    SimpleRowSet newRs = new SimpleRowSet(cols);

    for (SimpleRow row : rs) {
      newRs.addRow(new String[] {row.getValue(COL_CONTENT),
          HtmlUtils.cleanHtml(row.getValue(COL_HTML_CONTENT))});
    }
    packet.put(TBL_PARTS, newRs);

    packet.put(TBL_ATTACHMENTS, qs.getData(new SqlSelect()
        .addFields(TBL_ATTACHMENTS, AdministrationConstants.COL_FILE, COL_ATTACHMENT_NAME)
        .addFields(AdministrationConstants.TBL_FILES, AdministrationConstants.COL_FILE_NAME,
            AdministrationConstants.COL_FILE_SIZE)
        .addFrom(TBL_ATTACHMENTS)
        .addFromInner(AdministrationConstants.TBL_FILES,
            sys.joinTables(AdministrationConstants.TBL_FILES, TBL_ATTACHMENTS,
                AdministrationConstants.COL_FILE))
        .setWhere(SqlUtils.equals(TBL_ATTACHMENTS, COL_MESSAGE, messageId))));

    if (!MessageFlag.SEEN.isSet(msg.getInt(COL_FLAGS))) {
      try {
        setMessageFlag(placeId, MessageFlag.SEEN, true);
      } catch (MessagingException e) {
        logger.error(e);
      }
    }
    return ResponseObject.response(packet);
  }

  private ResponseObject getUsableContent(Long messageId) {
    Assert.notNull(messageId);

    Map<String, Object> packet = Maps.newHashMap();

    SimpleRow data = qs.getRow(new SqlSelect()
        .addFields(TBL_COMPANY_PERSONS, COL_COMPANY)
        .addField(TBL_COMPANY_PERSONS, sys.getIdName(TBL_COMPANY_PERSONS), COL_PERSON)
        .addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME)
        .addFields(TBL_COMPANIES, COL_COMPANY_NAME)
        .addFrom(TBL_MESSAGES)
        .addFromInner(TBL_CONTACTS,
            SqlUtils.join(TBL_MESSAGES, COL_SENDER, TBL_CONTACTS, COL_EMAIL))
        .addFromInner(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_CONTACTS, TBL_COMPANY_PERSONS, COL_CONTACT))
        .addFromInner(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
        .addFromInner(TBL_COMPANIES,
            sys.joinTables(TBL_COMPANIES, TBL_COMPANY_PERSONS, COL_COMPANY))
        .setWhere(sys.idEquals(TBL_MESSAGES, messageId)));

    if (data != null) {
      packet.put(COL_COMPANY, data.getLong(COL_COMPANY));
      packet.put(COL_COMPANY + COL_COMPANY_NAME, data.getValue(COL_COMPANY_NAME));
      packet.put(COL_PERSON, data.getLong(COL_PERSON));
      packet.put(COL_FIRST_NAME, data.getValue(COL_FIRST_NAME));
      packet.put(COL_LAST_NAME, data.getValue(COL_LAST_NAME));
    } else {
      data = qs.getRow(new SqlSelect()
          .addField(TBL_COMPANIES, sys.getIdName(TBL_COMPANIES), COL_COMPANY)
          .addFields(TBL_COMPANIES, COL_COMPANY_NAME)
          .addFrom(TBL_MESSAGES)
          .addFromInner(TBL_CONTACTS,
              SqlUtils.join(TBL_MESSAGES, COL_SENDER, TBL_CONTACTS, COL_EMAIL))
          .addFromInner(TBL_COMPANIES, sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT))
          .setWhere(sys.idEquals(TBL_MESSAGES, messageId)));

      if (data != null) {
        packet.put(COL_COMPANY, data.getValue(COL_COMPANY));
        packet.put(COL_COMPANY + COL_COMPANY_NAME, data.getValue(COL_COMPANY_NAME));
      }
    }
    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(TBL_PARTS, COL_CONTENT, COL_HTML_CONTENT)
        .addFrom(TBL_PARTS)
        .setWhere(SqlUtils.equals(TBL_PARTS, COL_MESSAGE, messageId)));

    StringBuilder content = new StringBuilder();

    for (SimpleRow row : rs) {
      if (content.length() > 0) {
        content.append("\n\n");
      }
      content.append(BeeUtils.notEmpty(HtmlUtils.stripHtml(row.getValue(COL_HTML_CONTENT)),
          row.getValue(COL_CONTENT)));
    }
    packet.put(COL_CONTENT, content.toString());

    packet.put(TBL_ATTACHMENTS, qs.getData(new SqlSelect()
        .addFields(TBL_ATTACHMENTS, AdministrationConstants.COL_FILE, COL_ATTACHMENT_NAME)
        .addFields(AdministrationConstants.TBL_FILES, AdministrationConstants.COL_FILE_NAME,
            AdministrationConstants.COL_FILE_SIZE)
        .addFrom(TBL_ATTACHMENTS)
        .addFromInner(AdministrationConstants.TBL_FILES,
            sys.joinTables(AdministrationConstants.TBL_FILES, TBL_ATTACHMENTS,
                AdministrationConstants.COL_FILE))
        .setWhere(SqlUtils.equals(TBL_ATTACHMENTS, COL_MESSAGE, messageId))));

    return ResponseObject.response(packet);
  }

  @Schedule(minute = "*/5", hour = "*", persistent = false)
  private void mailChecker() {
    for (String accountId : qs.getColumn(new SqlSelect()
        .addFields(TBL_ACCOUNTS, sys.getIdName(TBL_ACCOUNTS))
        .addFrom(TBL_ACCOUNTS)
        .setWhere(SqlUtils.notNull(TBL_ACCOUNTS, COL_STORE_SERVER)))) {

      MailAccount account = mail.getAccount(BeeUtils.toLongOrNull(accountId));
      ctx.getBusinessObject(this.getClass()).checkMail(account, account.getInboxFolder(), null);
    }
  }

  private ResponseObject processMessages(MailAccount account, MailFolder source, MailFolder target,
      String[] places, boolean move) throws MessagingException {
    Assert.state(!ArrayUtils.isEmpty(places), "Empty message list");

    List<Long> lst = Lists.newArrayList();

    for (String id : places) {
      lst.add(BeeUtils.toLong(id));
    }
    IsCondition wh = sys.idInList(TBL_PLACES, lst);

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_PLACES, COL_MESSAGE, COL_FLAGS, COL_MESSAGE_UID)
        .addFrom(TBL_PLACES)
        .setWhere(wh));

    lst.clear();

    for (SimpleRow row : data) {
      Long id = row.getLong(COL_MESSAGE_UID);

      if (id != null) {
        lst.add(id);
      }
    }
    long[] uids = new long[lst.size()];

    for (int i = 0; i < lst.size(); i++) {
      uids[i] = lst.get(i);
    }
    ResponseObject response = null;
    boolean delete = move;

    account.processMessages(uids, source, target, move);

    if (target != null) {
      if (account.isStoredRemotedly(target)) {
        if (!source.isConnected()) {
          Long[] contents = qs.getLongColumn(new SqlSelect()
              .addFields(TBL_MESSAGES, COL_RAW_CONTENT)
              .addFrom(TBL_MESSAGES)
              .addFromInner(TBL_PLACES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
              .setWhere(wh));

          for (Long fileId : contents) {
            StoredFile fileInfo = null;
            File file = null;
            InputStream is = null;

            try {
              fileInfo = fs.getFile(fileId);
              file = new File(fileInfo.getPath());
              is = new BufferedInputStream(new FileInputStream(file));
              account.addMessageToRemoteFolder(new MimeMessage(null, is), target);

            } catch (IOException e) {
              throw new MessagingException(e.getMessage());
            } finally {
              if (is != null) {
                try {
                  is.close();
                } catch (IOException e) {
                  logger.error(e);
                }
              }
              if (fileInfo != null && fileInfo.isTemporary()) {
                logger.debug("File deleted:", file.getAbsolutePath(), file.delete());
              }
            }
          }
        }
        if (!delete) {
          response = ResponseObject.response(data.getNumberOfRows());
        }
      } else {
        if (move) {
          response = qs.updateDataWithResponse(new SqlUpdate(TBL_PLACES)
              .addConstant(COL_FOLDER, target.getId())
              .addConstant(COL_MESSAGE_UID, null)
              .setWhere(wh));
        } else {
          for (SimpleRow row : data) {
            qs.insertData(new SqlInsert(TBL_PLACES)
                .addConstant(COL_FOLDER, target.getId())
                .addConstant(COL_MESSAGE, row.getLong(COL_MESSAGE))
                .addConstant(COL_FLAGS, row.getInt(COL_FLAGS)));
          }
          response = ResponseObject.response(data.getNumberOfRows());
        }
        delete = false;
      }
    }
    if (delete) {
      response = qs.updateDataWithResponse(new SqlDelete(TBL_PLACES).setWhere(wh));
    }
    return response;
  }

  private int setMessageFlag(Long placeId, MessageFlag flag, boolean on)
      throws MessagingException {

    SimpleRow row = qs.getRow(new SqlSelect()
        .addFields(TBL_PLACES, COL_FOLDER, COL_FLAGS, COL_MESSAGE_UID)
        .addFields(TBL_FOLDERS, COL_ACCOUNT)
        .addFrom(TBL_PLACES)
        .addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
        .setWhere(sys.idEquals(TBL_PLACES, placeId)));

    Assert.notNull(row);
    int value = BeeUtils.unbox(row.getInt(COL_FLAGS));
    MailAccount account = mail.getAccount(row.getLong(COL_ACCOUNT));
    MailFolder folder = account.findFolder(row.getLong(COL_FOLDER));

    account.setFlag(folder, new long[] {BeeUtils.unbox(row.getLong(COL_MESSAGE_UID))},
        MailEnvelope.getFlag(flag), on);

    if (on) {
      value = value | flag.getMask();
    } else {
      value = value & ~flag.getMask();
    }
    qs.updateData(new SqlUpdate(TBL_PLACES)
        .addConstant(COL_FLAGS, value)
        .setWhere(sys.idEquals(TBL_PLACES, placeId)));

    MailMessage mailMessage = new MailMessage(null);
    mailMessage.setFlag(flag);
    Endpoint.sendToUser(account.getUserId(), mailMessage);

    return value;
  }

  private int syncFolders(MailAccount account, Folder remoteFolder, MailFolder localFolder)
      throws MessagingException {
    int c = 0;
    Set<String> visitedFolders = Sets.newHashSet();

    if (account.holdsFolders(remoteFolder)) {
      for (Folder subFolder : remoteFolder.list()) {
        visitedFolders.add(subFolder.getName());
        MailFolder localSubFolder = null;

        for (MailFolder sub : localFolder.getSubFolders()) {
          if (BeeUtils.same(sub.getName(), subFolder.getName())) {
            localSubFolder = sub;
            break;
          }
        }
        if (localSubFolder == null) {
          localSubFolder = mail.createFolder(account, localFolder, subFolder.getName());
          c++;
        }
        if (localSubFolder.isConnected() && !subFolder.isSubscribed()) {
          subFolder.setSubscribed(true);
        }
        c += syncFolders(account, subFolder, localSubFolder);
      }
    }
    for (Iterator<MailFolder> iter = localFolder.getSubFolders().iterator(); iter.hasNext();) {
      MailFolder subFolder = iter.next();

      if (!visitedFolders.contains(subFolder.getName()) && subFolder.isConnected()
          && !account.isSystemFolder(subFolder)) {
        c++;
        mail.dropFolder(subFolder);
        iter.remove();
      }
    }
    return c;
  }
}
