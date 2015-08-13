package com.butent.bee.server.modules.mail;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.server.Invocation;
import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.concurrency.ConcurrencyBean.AsynchronousRunnable;
import com.butent.bee.server.concurrency.ConcurrencyBean.HasTimerService;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.BeeView.ConditionProvider;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.QueryServiceBean.ViewDataProvider;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.utils.HtmlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Flags;
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
import javax.mail.internet.MimeUtility;

@Stateless
@LocalBean
public class MailModuleBean implements BeeModule, HasTimerService {

  private static final BeeLogger logger = LogUtils.getLogger(MailModuleBean.class);

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
  @EJB
  NewsBean news;
  @EJB
  ConcurrencyBean cb;

  @Resource
  SessionContext ctx;
  @Resource
  TimerService timerService;

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void checkMail(boolean async, MailAccount account, MailFolder localFolder,
      String progressId) {

    Assert.noNulls(account, localFolder);

    if (async) {
      cb.asynchronousCall(new AsynchronousRunnable() {
        @Override
        public String getId() {
          return BeeUtils.join("-", MailModuleBean.class.getName(), localFolder.getId());
        }

        @Override
        public void onError() {
          if (!BeeUtils.isEmpty(progressId)) {
            Endpoint.closeProgress(progressId);
          }
        }

        @Override
        public void run() {
          MailModuleBean bean = Assert.notNull(Invocation.locateRemoteBean(MailModuleBean.class));
          bean.checkMail(false, account, localFolder, progressId);
        }
      });
      return;
    }
    if (localFolder.isConnected()) {
      Store store = null;
      int c = 0;
      int f = 0;
      String error = null;

      try {
        store = account.connectToStore();

        if (account.isInbox(localFolder)) {
          f = syncFolders(account, account.getRemoteFolder(store, account.getRootFolder()),
              account.getRootFolder());
        }
        c = checkFolder(account, account.getRemoteFolder(store, localFolder), localFolder,
            progressId);

      } catch (Exception e) {
        logger.error(e, "LOGIN:", account.getStoreLogin());
        error = BeeUtils.joinWords(account.getStoreLogin(), e.getMessage());
      } finally {
        account.disconnectFromStore(store);
      }
      if (!BeeUtils.isEmpty(error) || c > 0 || f > 0) {
        MailMessage mailMessage = new MailMessage(localFolder.getId());
        mailMessage.setMessagesUpdated(c > 0);
        mailMessage.setFoldersUpdated(f > 0);
        mailMessage.setError(error);
        Endpoint.sendToUser(account.getUserId(), mailMessage);
      }
    }
    if (!BeeUtils.isEmpty(progressId)) {
      Endpoint.closeProgress(progressId);
    }
  }

  @Override
  public List<SearchResult> doSearch(String query) {
    return null;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    try {
      if (BeeUtils.same(svc, SVC_GET_ACCOUNTS)) {
        response = ResponseObject.response(qs.getData(new SqlSelect()
            .addField(TBL_ACCOUNTS, sys.getIdName(TBL_ACCOUNTS), COL_ACCOUNT)
            .addFields(TBL_ACCOUNTS, MailConstants.COL_ADDRESS, COL_USER, COL_ACCOUNT_DESCRIPTION,
                COL_ACCOUNT_DEFAULT, COL_SIGNATURE, COL_ACCOUNT_PRIVATE,
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
        response = getMessage(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_MESSAGE)),
            BeeUtils.toLongOrNull(reqInfo.getParameter(COL_PLACE)));

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
          response = ResponseObject.response(copyMessages(account,
              DataUtils.parseIdList(reqInfo.getParameter(COL_PLACE)), target,
              BeeUtils.toBoolean(reqInfo.getParameter("move"))));
        }
      } else if (BeeUtils.same(svc, SVC_REMOVE_MESSAGES)) {
        response = ResponseObject.response(removeMessages(
            mail.getAccount(BeeUtils.toLong(reqInfo.getParameter(COL_ACCOUNT))),
            DataUtils.parseIdList(reqInfo.getParameter(COL_PLACE))));

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
          checkMail(true, account, folder, reqInfo.getParameter(Service.VAR_PROGRESS));
          response = ResponseObject.emptyResponse();
        }
      } else if (BeeUtils.same(svc, SVC_SEND_MAIL)) {
        response = new ResponseObject();
        boolean save = BeeUtils.toBoolean(reqInfo.getParameter("Save"));
        Long relatedId = BeeUtils.toLongOrNull(reqInfo
            .getParameter(AdministrationConstants.COL_RELATION));
        Long accountId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ACCOUNT));
        String[] to = Codec.beeDeserializeCollection(reqInfo.getParameter(AddressType.TO.name()));
        String[] cc = Codec.beeDeserializeCollection(reqInfo.getParameter(AddressType.CC.name()));
        String[] bcc = Codec.beeDeserializeCollection(reqInfo.getParameter(AddressType.BCC.name()));
        String subject = reqInfo.getParameter(COL_SUBJECT);
        String content = reqInfo.getParameter(COL_CONTENT);

        if (DataUtils.isId(relatedId)) {
          SimpleRow row = qs.getRow(new SqlSelect()
              .addField(TBL_FOLDERS, sys.getIdName(TBL_FOLDERS), COL_FOLDER)
              .addFields(TBL_FOLDERS, COL_ACCOUNT)
              .addFrom(TBL_PLACES)
              .addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
              .setWhere(sys.idEquals(TBL_PLACES, relatedId)));

          MailAccount account = mail.getAccount(row.getLong(COL_ACCOUNT));

          if (Objects.equals(row.getLong(COL_FOLDER), account.getDraftsFolder().getId())) {
            Long repliedFrom = qs.getLong(new SqlSelect()
                .addFields(TBL_PLACES, sys.getIdName(TBL_PLACES))
                .addFrom(TBL_PLACES)
                .setWhere(SqlUtils.equals(TBL_PLACES, COL_REPLIED, relatedId)));

            removeMessages(account, Arrays.asList(relatedId));

            relatedId = repliedFrom;
          } else {
            setMessageFlag(relatedId, MessageFlag.ANSWERED, true);
          }
        }
        Map<Long, String> attachments = new LinkedHashMap<>();

        for (Entry<String, String> entry : Codec
            .deserializeMap(reqInfo.getParameter(TBL_ATTACHMENTS)).entrySet()) {
          attachments.put(BeeUtils.toLong(entry.getKey()), entry.getValue());
        }
        MailAccount account = mail.getAccount(accountId);

        if (!save) {
          try {
            MimeMessage message = sendMail(account, to, cc, bcc, subject, content, attachments);
            storeMessage(account, message, account.getSentFolder(), relatedId);
            response.addInfo(usr.getLocalizableConstants().mailMessageSent());

          } catch (MessagingException e) {
            save = true;
            logger.error(e);
            response.addError(e);
          }
        }
        if (save) {
          MimeMessage message = buildMessage(account, to, cc, bcc, subject, content, attachments);
          storeMessage(account, message, account.getDraftsFolder(), relatedId);
          response.addInfo(usr.getLocalizableConstants().mailMessageIsSavedInDraft());
        }
      } else if (BeeUtils.same(svc, SVC_STRIP_HTML)) {
        response = ResponseObject
            .response(HtmlUtils.stripHtml(reqInfo.getParameter(COL_HTML_CONTENT)));

      } else if (BeeUtils.same(svc, SVC_GET_UNREAD_COUNT)) {

        response = ResponseObject.response(qs.getData(new SqlSelect()
            .addCount("UnreadEmailCount")
            .addFrom(TBL_PLACES)
            .addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
            .addFromInner(TBL_ACCOUNTS, sys.joinTables(TBL_ACCOUNTS, TBL_FOLDERS, COL_ACCOUNT))
            .setWhere(SqlUtils.and(SqlUtils.equals(TBL_ACCOUNTS, COL_USER, usr.getCurrentUserId()),
                SqlUtils.or(SqlUtils.isNull(TBL_PLACES, COL_FLAGS),
                    SqlUtils.equals(SqlUtils.bitAnd(TBL_PLACES, COL_FLAGS,
                        MessageFlag.SEEN.getMask()), 0))))).getIntColumn("UnreadEmailCount"));

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
        BeeParameter.createNumber(module, PRM_MAIL_CHECK_INTERVAL, false, null));

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
  public TimerService getTimerService() {
    return timerService;
  }

  @Override
  public void init() {
    System.setProperty("mail.mime.decodetext.strict", "false");
    System.setProperty("mail.mime.parameters.strict", "false");
    System.setProperty("mail.mime.ignoreunknownencoding", "true");
    System.setProperty("mail.mime.uudecode.ignoreerrors", "true");
    System.setProperty("mail.mime.uudecode.ignoremissingbeginend", "true");
    System.setProperty("mail.mime.ignoremultipartencoding", "false");
    System.setProperty("mail.mime.allowencodedmessages", "true");

    cb.createIntervalTimer(this.getClass(), PRM_MAIL_CHECK_INTERVAL);

    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      public void initAccount(ViewInsertEvent event) {
        if (event.isTarget(TBL_ACCOUNTS) && event.isAfter()) {
          mail.initAccount(event.getRow().getId());
        }
      }

      @Subscribe
      public void getRecipients(ViewQueryEvent event) {
        if (event.isTarget(TBL_PLACES) && event.isAfter()) {
          Set<Long> messages = new HashSet<>();

          BeeRowSet rowSet = event.getRowset();
          int idx = DataUtils.getColumnIndex(COL_MESSAGE, rowSet.getColumns(), false);

          if (!DataUtils.isEmpty(rowSet) && !BeeConst.isUndef(idx)) {
            for (BeeRow row : rowSet) {
              messages.add(row.getLong(idx));
            }
          }
          if (!BeeUtils.isEmpty(messages)) {
            SimpleRowSet result = qs.getData(new SqlSelect()
                .addFields(TBL_RECIPIENTS, COL_MESSAGE, COL_ADDRESS_TYPE)
                .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
                .addFields(TBL_ADDRESSBOOK, COL_EMAIL_LABEL)
                .addFrom(TBL_RECIPIENTS)
                .addFromInner(TBL_EMAILS,
                    sys.joinTables(TBL_EMAILS, TBL_RECIPIENTS, MailConstants.COL_ADDRESS))
                .addFromLeft(TBL_ADDRESSBOOK,
                    SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                        SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, usr.getCurrentUserId())))
                .setWhere(SqlUtils.inList(TBL_RECIPIENTS, COL_MESSAGE, messages))
                .addOrderDesc(TBL_RECIPIENTS, COL_ADDRESS_TYPE)
                .addOrder(TBL_RECIPIENTS, sys.getIdName(TBL_RECIPIENTS)));

            Map<String, String[]> recipients = new HashMap<>();

            for (SimpleRow row : result) {
              String msg = row.getValue(COL_MESSAGE);
              String email = row.getValue(COL_EMAIL_ADDRESS);
              String label = row.getValue(COL_EMAIL_LABEL);

              if (recipients.containsKey(msg)) {
                String[] info = recipients.get(msg);
                info[2] = BeeUtils.toString(BeeUtils.toLong(info[2]) + 1);
              } else {
                recipients.put(msg, new String[] {email, label, "1"});
              }
            }
            for (BeeRow row : rowSet) {
              String[] info = recipients.get(row.getString(idx));

              if (info != null) {
                row.setProperty(COL_EMAIL_ADDRESS, info[0]);
                row.setProperty(COL_EMAIL_LABEL, info[1]);
                row.setProperty(MailConstants.COL_ADDRESS, info[2]);
              }
            }
            String relations = AdministrationConstants.TBL_RELATIONS;
            String relation = AdministrationConstants.COL_RELATION;

            result = qs.getData(new SqlSelect()
                .addFields(relations, COL_MESSAGE)
                .addCount(relation)
                .addFrom(relations)
                .setWhere(SqlUtils.and(SqlUtils.inList(relations, COL_MESSAGE, messages),
                    SqlUtils.isNull(relations, COL_COMPANY)))
                .addGroup(relations, COL_MESSAGE));

            for (BeeRow row : rowSet) {
              row.setProperty(relation,
                  result.getValueByKey(COL_MESSAGE, row.getString(idx), relation));
            }
          }
        }
      }
    });

    BeeView.registerConditionProvider(TBL_PLACES, new ConditionProvider() {
      @Override
      public IsCondition getCondition(BeeView view, List<String> args) {
        Map<String, String> params = Codec.deserializeMap(BeeUtils.peek(args));
        Long folderId = BeeUtils.toLong(params.get(COL_FOLDER));
        Long accountId = BeeUtils.toLong(params.get(COL_ACCOUNT));
        String sender = params.get(COL_SENDER);
        String recipient = params.get(MailConstants.COL_ADDRESS);
        String subject = params.get(COL_SUBJECT);
        String content = params.get(COL_CONTENT);

        SqlSelect query = new SqlSelect().setDistinctMode(true)
            .addFields(TBL_PLACES, sys.getIdName(TBL_PLACES))
            .addFrom(TBL_PLACES)
            .addFromInner(TBL_MESSAGES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE));

        HasConditions clause = SqlUtils.and();

        if (DataUtils.isId(folderId)) {
          clause.add(SqlUtils.equals(TBL_PLACES, COL_FOLDER, folderId));
        } else {
          query.addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER));
          clause.add(SqlUtils.equals(TBL_FOLDERS, COL_ACCOUNT, accountId));
        }
        if (BeeUtils.toBoolean(params.get(MessageFlag.SEEN.name()))) {
          clause.add(SqlUtils.or(SqlUtils.isNull(TBL_PLACES, COL_FLAGS),
              SqlUtils.equals(SqlUtils.bitAnd(TBL_PLACES, COL_FLAGS,
                  MessageFlag.SEEN.getMask()), 0)));
        }
        if (BeeUtils.toBoolean(params.get(MessageFlag.FLAGGED.name()))) {
          clause.add(SqlUtils.more(SqlUtils.bitAnd(TBL_PLACES, COL_FLAGS,
              MessageFlag.FLAGGED.getMask()), 0));
        }
        if (BeeUtils.toBoolean(params.get(TBL_ATTACHMENTS))) {
          clause.add(SqlUtils.more(TBL_MESSAGES, COL_ATTACHMENT_COUNT, 0));
        }
        if (BeeUtils.anyNotEmpty(sender, content)) {
          query.addFromLeft(TBL_EMAILS, sys.joinTables(TBL_EMAILS, TBL_MESSAGES, COL_SENDER))
              .addFromLeft(TBL_ADDRESSBOOK,
                  SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                      SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, usr.getCurrentUserId())));

          if (!BeeUtils.isEmpty(sender)) {
            clause.add(SqlUtils.or(SqlUtils.contains(TBL_EMAILS, COL_EMAIL_ADDRESS, sender),
                SqlUtils.contains(TBL_ADDRESSBOOK, COL_EMAIL_LABEL, sender)));
          }
        }
        String alsMails = SqlUtils.uniqueName();
        String alsBook = SqlUtils.uniqueName();

        if (BeeUtils.anyNotEmpty(recipient, content)) {
          query.addFromLeft(TBL_RECIPIENTS,
              sys.joinTables(TBL_MESSAGES, TBL_RECIPIENTS, COL_MESSAGE))
              .addFromLeft(TBL_EMAILS, alsMails,
                  sys.joinTables(TBL_EMAILS, alsMails, TBL_RECIPIENTS, MailConstants.COL_ADDRESS))
              .addFromLeft(TBL_ADDRESSBOOK, alsBook,
                  SqlUtils.and(sys.joinTables(TBL_EMAILS, alsMails, alsBook, COL_EMAIL),
                      SqlUtils.equals(alsBook, COL_USER, usr.getCurrentUserId())));

          if (!BeeUtils.isEmpty(recipient)) {
            clause.add(SqlUtils.or(SqlUtils.contains(alsMails, COL_EMAIL_ADDRESS, recipient),
                SqlUtils.contains(alsBook, COL_EMAIL_LABEL, recipient)));
          }
        }
        if (!BeeUtils.isEmpty(subject)) {
          clause.add(SqlUtils.contains(TBL_MESSAGES, COL_SUBJECT, subject));
        }
        if (!BeeUtils.isEmpty(content)) {
          query.addFromLeft(TBL_PARTS, sys.joinTables(TBL_MESSAGES, TBL_PARTS, COL_MESSAGE));
          HasConditions cl = SqlUtils.or();

          if (BeeUtils.isEmpty(sender)) {
            cl.add(SqlUtils.contains(TBL_EMAILS, COL_EMAIL_ADDRESS, content))
                .add(SqlUtils.contains(TBL_ADDRESSBOOK, COL_EMAIL_LABEL, content));
          }
          if (BeeUtils.isEmpty(recipient)) {
            cl.add(SqlUtils.contains(alsMails, COL_EMAIL_ADDRESS, content))
                .add(SqlUtils.contains(alsBook, COL_EMAIL_LABEL, content));
          }
          if (BeeUtils.isEmpty(subject)) {
            cl.add(SqlUtils.contains(TBL_MESSAGES, COL_SUBJECT, content));
          }
          clause.add(cl.add(SqlUtils.fullText(TBL_PARTS, COL_CONTENT + "FTS", content)));
        }
        return SqlUtils.in(TBL_PLACES, sys.getIdName(TBL_PLACES), query.setWhere(clause));
      }
    });

    QueryServiceBean.registerViewDataProvider(VIEW_USER_EMAILS, new ViewDataProvider() {
      @Override
      public BeeRowSet getViewData(BeeView view, SqlSelect query, Filter filter) {
        return qs.getViewData(new SqlSelect().setUnionAllMode(true)
            .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
            .addFields(TBL_ADDRESSBOOK, COL_ADDRESSBOOK_LABEL)
            .addFrom(TBL_EMAILS)
            .addFromInner(TBL_ADDRESSBOOK,
                SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                    SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, usr.getCurrentUserId())))
            .addUnion(new SqlSelect()
                .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
                .addField(TBL_COMPANIES, COL_COMPANY_NAME, COL_ADDRESSBOOK_LABEL)
                .addFrom(TBL_EMAILS)
                .addFromInner(TBL_CONTACTS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
                .addFromInner(TBL_COMPANIES,
                    sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT)))
            .addUnion(new SqlSelect()
                .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
                .addExpr(SqlUtils.concat(SqlUtils.field(TBL_COMPANIES, COL_COMPANY_NAME), "' '",
                    SqlUtils.nvl(SqlUtils.field(TBL_CONTACTS, COL_NOTES), "''")),
                    COL_ADDRESSBOOK_LABEL)
                .addFrom(TBL_EMAILS)
                .addFromInner(TBL_CONTACTS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
                .addFromInner(TBL_COMPANY_CONTACTS,
                    sys.joinTables(TBL_CONTACTS, TBL_COMPANY_CONTACTS, COL_CONTACT))
                .addFromInner(TBL_COMPANIES,
                    sys.joinTables(TBL_COMPANIES, TBL_COMPANY_CONTACTS, COL_COMPANY)))
            .addUnion(new SqlSelect()
                .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
                .addExpr(SqlUtils.concat(SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME), "' '",
                    SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_LAST_NAME), "''")),
                    COL_ADDRESSBOOK_LABEL)
                .addFrom(TBL_EMAILS)
                .addFromInner(TBL_CONTACTS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
                .addFromInner(TBL_PERSONS,
                    sys.joinTables(TBL_CONTACTS, TBL_PERSONS, COL_CONTACT)))
            .addUnion(new SqlSelect()
                .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
                .addExpr(SqlUtils.concat(SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME), "' '",
                    SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_LAST_NAME), "''"), "' '",
                    SqlUtils.nvl(SqlUtils.field(TBL_POSITIONS, COL_POSITION_NAME), "''")),
                    COL_ADDRESSBOOK_LABEL)
                .addFrom(TBL_EMAILS)
                .addFromInner(TBL_CONTACTS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
                .addFromInner(TBL_COMPANY_PERSONS,
                    sys.joinTables(TBL_CONTACTS, TBL_COMPANY_PERSONS, COL_CONTACT))
                .addFromInner(TBL_PERSONS,
                    sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
                .addFromLeft(TBL_POSITIONS,
                    sys.joinTables(TBL_POSITIONS, TBL_COMPANY_PERSONS, COL_POSITION)))
            .addOrder(null, COL_EMAIL_ADDRESS),
            sys.getView(VIEW_USER_EMAILS));
      }

      @Override
      public int getViewSize(BeeView view, SqlSelect query, Filter filter) {
        return getViewData(view, query, filter).getNumberOfRows();
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
    Long account = prm.getRelation(PRM_DEFAULT_ACCOUNT);

    if (!DataUtils.isId(account)) {
      logger.info(logLabel, "sender account not specified", BeeUtils.bracket(PRM_DEFAULT_ACCOUNT));
    }
    return account;
  }

  public ResponseObject sendMail(Long accountId, String to, String subject, String content) {
    return sendMail(accountId, new String[] {to}, subject, content);
  }

  public ResponseObject sendMail(Long accountId, String[] to, String subject, String content) {
    try {
      sendMail(mail.getAccount(accountId), to, null, null, subject, content, null);
    } catch (MessagingException ex) {
      logger.error(ex);
      return ResponseObject.error(ex);
    }
    return ResponseObject.emptyResponse();
  }

  public MimeMessage sendMail(MailAccount account, String[] to, String[] cc, String[] bcc,
      String subject, String content, Map<Long, String> attachments)
      throws MessagingException {

    Transport transport = null;
    MimeMessage message = null;

    try {
      message = buildMessage(account, to, cc, bcc, subject, content, attachments);
      Address[] recipients = message.getAllRecipients();

      if (recipients == null || recipients.length == 0) {
        throw new MessagingException("No recipients");
      }
      transport = account.connectToTransport();
      transport.sendMessage(message, recipients);
    } finally {
      if (transport != null) {
        try {
          transport.close();
        } catch (MessagingException e) {
          logger.warning(e);
        }
      }
    }
    return message;
  }

  private Set<MailFolder> applyRules(Message message, long placeId, MailAccount account,
      MailFolder folder, SimpleRowSet rules) throws MessagingException {

    MailEnvelope envelope = new MailEnvelope(message);
    String sender = envelope.getSender() != null ? envelope.getSender().getAddress() : null;
    Set<MailFolder> changedFolders = new HashSet<>();

    for (SimpleRow row : rules) {
      RuleCondition condition = EnumUtils.getEnumByIndex(RuleCondition.class,
          row.getInt(COL_RULE_CONDITION));

      boolean ok = false;
      Set<String> expr = new HashSet<>();

      switch (condition) {
        case ALL:
          ok = true;
          break;

        case RECIPIENTS:
          for (InternetAddress address : envelope.getRecipients().values()) {
            expr.add(address.getAddress());
          }
          break;

        case SENDER:
          expr.add(sender);
          break;

        case SUBJECT:
          expr.add(envelope.getSubject());
          break;
      }
      if (!ok) {
        String value = row.getValue(COL_RULE_CONDITION_OPTIONS);

        for (String x : expr) {
          ok = BeeUtils.containsSame(x, value);

          if (ok) {
            break;
          }
        }
      }
      if (!ok) {
        continue;
      }
      RuleAction action = EnumUtils.getEnumByIndex(RuleAction.class, row.getInt(COL_RULE_ACTION));

      String log = BeeUtils.joinWords(Localized.getConstants().mailRule() + ":",
          condition.getCaption(), row.getValue(COL_RULE_CONDITION_OPTIONS), action.getCaption());

      switch (action) {
        case COPY:
        case MOVE:
        case DELETE:
          boolean move = EnumSet.of(RuleAction.MOVE, RuleAction.DELETE).contains(action);
          MailFolder folderTo = account.findFolder(row.getLong(COL_RULE_ACTION_OPTIONS));

          if (folderTo != null) {
            changedFolders.add(folderTo);
            log += " " + BeeUtils.join("/", folderTo.getParent().getName(), folderTo.getName());
          }
          logger.debug(log);

          processMessages(account, folder, folderTo, Arrays.asList(placeId), move);

          if (move) {
            return changedFolders;
          }
          break;

        case FLAG:
        case READ:
          logger.debug(log);

          setMessageFlag(placeId, action == RuleAction.FLAG
              ? MessageFlag.FLAGGED : MessageFlag.SEEN, true);
          break;

        case FORWARD:
          logger.debug(log, row.getValue(COL_RULE_ACTION_OPTIONS));

          Map<Long, String> attachments = new LinkedHashMap<>();

          SimpleRowSet rs = qs.getData(new SqlSelect()
              .addFields(TBL_ATTACHMENTS, AdministrationConstants.COL_FILE, COL_ATTACHMENT_NAME)
              .addFrom(TBL_ATTACHMENTS)
              .addFromInner(TBL_MESSAGES,
                  sys.joinTables(TBL_MESSAGES, TBL_ATTACHMENTS, COL_MESSAGE))
              .addFromInner(TBL_PLACES,
                  sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
              .setWhere(sys.idEquals(TBL_PLACES, placeId)));

          for (SimpleRow attach : rs) {
            attachments.put(attach.getLong(AdministrationConstants.COL_FILE),
                attach.getValue(COL_ATTACHMENT_NAME));
          }
          rs = qs.getData(new SqlSelect()
              .addFields(TBL_PARTS, COL_CONTENT, COL_HTML_CONTENT)
              .addFrom(TBL_PARTS)
              .addFromInner(TBL_MESSAGES, sys.joinTables(TBL_MESSAGES, TBL_PARTS, COL_MESSAGE))
              .addFromInner(TBL_PLACES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
              .setWhere(sys.idEquals(TBL_PLACES, placeId)));

          LocalizableConstants loc = Localized.getConstants();

          String content = BeeUtils.join("<br>", "---------- "
              + loc.mailForwardedMessage() + " ----------",
              loc.mailFrom() + ": " + sender,
              loc.date() + ": " + envelope.getDate(),
              loc.mailSubject() + ": " + envelope.getSubject(),
              loc.mailTo() + ": " + account.getAddress());

          for (SimpleRow part : rs) {
            String text = BeeUtils.notEmpty(part.getValue(COL_HTML_CONTENT),
                part.getValue(COL_CONTENT));

            if (!BeeUtils.isEmpty(text)) {
              content += "<br><br>" + text;
            }
          }
          sendMail(account, new String[] {row.getValue(COL_RULE_ACTION_OPTIONS)}, null, null,
              envelope.getSubject(), content, attachments);
          break;

        case REPLY:
          if (!BeeUtils.isEmpty(sender)) {
            logger.debug(log);

            content = row.getValue(COL_RULE_ACTION_OPTIONS).replace("\n", "<br>");
            Long signatureId = account.getSignatureId();

            if (DataUtils.isId(signatureId)) {
              content = BeeUtils.join(SIGNATURE_SEPARATOR, content,
                  qs.getValue(new SqlSelect()
                      .addFields(TBL_SIGNATURES, COL_SIGNATURE_CONTENT)
                      .addFrom(TBL_SIGNATURES)
                      .setWhere(sys.idEquals(TBL_SIGNATURES, signatureId))));
            }
            sendMail(account, new String[] {sender}, null, null,
                BeeUtils.joinWords(Localized.getConstants().mailReplayPrefix(),
                    envelope.getSubject()), content, null);
          }
          break;
      }
    }
    return changedFolders;
  }

  private MimeMessage buildMessage(MailAccount account, String[] to, String[] cc, String[] bcc,
      String subject, String content, Map<Long, String> attachments) throws MessagingException {

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
    List<FileInfo> files = new ArrayList<>();

    if (!BeeUtils.isEmpty(attachments)) {
      multi = new MimeMultipart();

      for (Entry<Long, String> entry : attachments.entrySet()) {
        MimeBodyPart p = null;
        FileInfo fileInfo = null;

        try {
          fileInfo = fs.getFile(entry.getKey());
          File file = new File(fileInfo.getPath());

          p = new MimeBodyPart();
          p.attachFile(file, fileInfo.getType(), null);
          p.setFileName(MimeUtility.encodeText(BeeUtils.notEmpty(entry.getValue(),
              fileInfo.getName()), BeeConst.CHARSET_UTF8, null));

        } catch (IOException ex) {
          logger.error(ex);
          p = null;
        }
        if (p != null) {
          multi.addBodyPart(p);
        }
        if (fileInfo != null) {
          files.add(fileInfo);
        }
      }
    }
    if (HtmlUtils.hasHtml(content)) {
      MimeMultipart alternative = new MimeMultipart("alternative");

      MimeBodyPart p = new MimeBodyPart();
      p.setText(HtmlUtils.stripHtml(content), BeeConst.CHARSET_UTF8);
      alternative.addBodyPart(p);

      MimeMultipart related = new MimeMultipart("related");

      Map<Long, String> relatedFiles = HtmlUtils.getFileReferences(content);
      String parsedContent = content;

      for (Long fileId : relatedFiles.keySet()) {
        try {
          FileInfo fileInfo = fs.getFile(fileId);

          if (fileInfo != null) {
            p = new MimeBodyPart();
            File file = new File(fileInfo.getPath());
            String cid = BeeUtils.randomString(10);

            try {
              p.attachFile(file, fileInfo.getType(), null);
              p.addHeader("Content-ID", "<" + cid + ">");
              p.setFileName(MimeUtility.encodeText(fileInfo.getName(), BeeConst.CHARSET_UTF8,
                  null));
            } catch (IOException ex) {
              logger.error(ex);
              p = null;
            }
            if (p != null) {
              parsedContent = parsedContent.replace(relatedFiles.get(fileId), "cid:" + cid);
              related.addBodyPart(p);
            }
            files.add(fileInfo);
          }
        } catch (IOException e) {
          logger.error(e);
        }
      }
      p = new MimeBodyPart();
      p.setText(parsedContent, BeeConst.CHARSET_UTF8, "html");

      if (related.getCount() > 0) {
        related.addBodyPart(p, 0);
        p = new MimeBodyPart();
        p.setContent(related);
      }
      alternative.addBodyPart(p);

      if (multi != null && multi.getCount() > 0) {
        p = new MimeBodyPart();
        p.setContent(alternative);
        multi.addBodyPart(p, 0);
      } else {
        multi = alternative;
      }
    } else if (multi != null && multi.getCount() > 0) {
      MimeBodyPart p = new MimeBodyPart();
      p.setText(content, BeeConst.CHARSET_UTF8);
      multi.addBodyPart(p, 0);
    }
    if (multi != null && multi.getCount() > 0) {
      message.setContent(multi);
    } else {
      message.setText(content, BeeConst.CHARSET_UTF8);
    }
    message.saveChanges();
    MimeMessage msg = new MimeMessage(message);

    for (FileInfo fileInfo : files) {
      if (fileInfo.isTemporary()) {
        logger.debug("File deleted:", fileInfo.getPath(), new File(fileInfo.getPath()).delete());
      }
    }
    return msg;
  }

  private int checkFolder(MailAccount account, Folder remoteFolder, MailFolder localFolder,
      String progressId) throws MessagingException {
    Assert.noNulls(remoteFolder, localFolder);

    int c = 0;
    SimpleRowSet rules = null;

    if (localFolder.isConnected() && account.holdsMessages(remoteFolder)) {
      Set<MailFolder> changedFolders = new HashSet<>();
      boolean uidMode = remoteFolder instanceof UIDFolder;
      Long uidValidity = uidMode ? ((UIDFolder) remoteFolder).getUIDValidity() : null;

      mail.validateFolder(localFolder, uidValidity);

      try {
        remoteFolder.open(Folder.READ_ONLY);
        Message[] newMessages;
        Long lastUid = null;

        if (uidMode) {
          Pair<Long, Integer> pair = mail.syncFolder(account, localFolder, remoteFolder);
          lastUid = pair.getA();
          c += pair.getB();
          newMessages = ((UIDFolder) remoteFolder).getMessagesByUID(lastUid + 1, UIDFolder.LASTUID);
        } else {
          newMessages = remoteFolder.getMessages();
        }
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        fp.add(FetchProfile.Item.FLAGS);
        remoteFolder.fetch(newMessages, fp);
        boolean isInbox = account.isInbox(localFolder);
        int l = 0;

        for (Message message : newMessages) {
          Long currentUid = uidMode ? ((UIDFolder) remoteFolder).getUID(message) : null;

          if (currentUid == null || currentUid > lastUid) {
            Long placeId = mail.storeMail(account, message, localFolder.getId(), currentUid);

            if (DataUtils.isId(placeId)) {
              if (isInbox) {
                if (rules == null) {
                  rules = qs.getData(new SqlSelect()
                      .addFields(TBL_RULES, COL_RULE_CONDITION, COL_RULE_CONDITION_OPTIONS,
                          COL_RULE_ACTION, COL_RULE_ACTION_OPTIONS)
                      .addFrom(TBL_RULES)
                      .setWhere(SqlUtils.and(SqlUtils.equals(TBL_RULES, COL_ACCOUNT,
                          account.getAccountId()), SqlUtils.notNull(TBL_RULES, COL_RULE_ACTIVE)))
                      .addOrder(TBL_RULES, COL_RULE_ORDINAL, sys.getIdName(TBL_RULES)));
                }
                if (!rules.isEmpty()) {
                  changedFolders.addAll(applyRules(message, placeId, account, localFolder, rules));
                }
              }
              c++;
            }
          }
          if (!BeeUtils.isEmpty(progressId)
              && !Endpoint.updateProgress(progressId, ++l / (double) newMessages.length)) {
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
      for (MailFolder mailFolder : changedFolders) {
        if (!account.isInbox(mailFolder)) {
          checkMail(true, account, mailFolder, null);
        }
      }
    }
    return c;
  }

  private int copyMessages(MailAccount account, List<Long> places, MailFolder target,
      boolean move) throws MessagingException {

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_PLACES, COL_FOLDER)
        .addField(TBL_PLACES, sys.getIdName(TBL_PLACES), COL_PLACE)
        .addFrom(TBL_PLACES)
        .setWhere(sys.idInList(TBL_PLACES, places)));

    int c = 0;
    Multimap<Long, Long> folders = HashMultimap.create();

    for (SimpleRow row : data) {
      folders.put(row.getLong(COL_FOLDER), row.getLong(COL_PLACE));
    }
    for (Long folderId : folders.keySet()) {
      MailFolder source = account.findFolder(folderId);

      if (!move || source != target) {
        c += processMessages(account, source, target, folders.get(folderId), move);
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

  private ResponseObject getMessage(Long messageId, Long placeId) {
    Assert.isTrue(BeeUtils.anyNotNull(messageId, placeId));

    Map<String, SimpleRowSet> packet = new HashMap<>();
    String sent = SystemFolder.Sent.name();
    String drafts = SystemFolder.Drafts.name();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_MESSAGES, COL_DATE, COL_SUBJECT, COL_RAW_CONTENT)
        .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
        .addFields(TBL_ADDRESSBOOK, COL_EMAIL_LABEL)
        .addFrom(TBL_MESSAGES)
        .addFromLeft(TBL_EMAILS, sys.joinTables(TBL_EMAILS, TBL_MESSAGES, COL_SENDER))
        .addFromLeft(TBL_ADDRESSBOOK,
            SqlUtils.and(sys.joinTables(TBL_EMAILS, TBL_ADDRESSBOOK, COL_EMAIL),
                SqlUtils.equals(TBL_ADDRESSBOOK, COL_USER, usr.getCurrentUserId())));

    if (DataUtils.isId(placeId)) {
      query.addFields(TBL_PLACES, COL_FLAGS, COL_MESSAGE, COL_REPLIED, COL_FOLDER)
          .addField(TBL_PLACES, sys.getIdName(TBL_PLACES), COL_PLACE)
          .addExpr(SqlUtils.expression(SqlUtils.equals(TBL_PLACES, COL_FOLDER,
              SqlUtils.field(TBL_ACCOUNTS, sent + COL_FOLDER))), sent)
          .addExpr(SqlUtils.expression(SqlUtils.equals(TBL_PLACES, COL_FOLDER,
              SqlUtils.field(TBL_ACCOUNTS, drafts + COL_FOLDER))), drafts)
          .addFields(TBL_ACCOUNTS, COL_USER)
          .addFromInner(TBL_PLACES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
          .addFromInner(TBL_FOLDERS, sys.joinTables(TBL_FOLDERS, TBL_PLACES, COL_FOLDER))
          .addFromInner(TBL_ACCOUNTS, sys.joinTables(TBL_ACCOUNTS, TBL_FOLDERS, COL_ACCOUNT))
          .setWhere(sys.idEquals(TBL_PLACES, placeId));
    } else {
      query.addConstant(messageId, COL_MESSAGE)
          .addEmptyLong(COL_REPLIED)
          .addEmptyLong(COL_PLACE)
          .addEmptyLong(COL_FOLDER)
          .addEmptyBoolean(sent)
          .addEmptyBoolean(drafts)
          .setWhere(sys.idEquals(TBL_MESSAGES, messageId));
    }
    SimpleRow msg = qs.getRow(query);
    Assert.notNull(msg);

    packet.put(TBL_MESSAGES, msg.getRowSet());

    Long message = msg.getLong(COL_MESSAGE);
    IsCondition wh = SqlUtils.equals(TBL_RECIPIENTS, COL_MESSAGE, message);

    if (!DataUtils.isId(placeId)
        || !Objects.equals(msg.getLong(COL_USER), usr.getCurrentUserId())) {
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
        .addOrderDesc(TBL_RECIPIENTS, COL_ADDRESS_TYPE)
        .addOrder(TBL_RECIPIENTS, sys.getIdName(TBL_RECIPIENTS))));

    String[] cols = new String[] {COL_CONTENT, COL_HTML_CONTENT};

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(TBL_PARTS, cols)
        .addFrom(TBL_PARTS)
        .setWhere(SqlUtils.equals(TBL_PARTS, COL_MESSAGE, message))
        .addOrder(TBL_PARTS, sys.getIdName(TBL_PARTS)));

    for (SimpleRow row : rs) {
      row.setValue(COL_HTML_CONTENT, HtmlUtils.cleanHtml(row.getValue(COL_HTML_CONTENT)));
    }
    packet.put(TBL_PARTS, rs);

    packet.put(TBL_ATTACHMENTS, qs.getData(new SqlSelect()
        .addFields(TBL_ATTACHMENTS, AdministrationConstants.COL_FILE, COL_ATTACHMENT_NAME)
        .addFields(AdministrationConstants.TBL_FILES, AdministrationConstants.COL_FILE_NAME,
            AdministrationConstants.COL_FILE_SIZE, AdministrationConstants.COL_FILE_TYPE)
        .addFrom(TBL_ATTACHMENTS)
        .addFromInner(AdministrationConstants.TBL_FILES,
            sys.joinTables(AdministrationConstants.TBL_FILES, TBL_ATTACHMENTS,
                AdministrationConstants.COL_FILE))
        .setWhere(SqlUtils.equals(TBL_ATTACHMENTS, COL_MESSAGE, message))
        .addOrder(TBL_ATTACHMENTS, sys.getIdName(TBL_ATTACHMENTS))));

    if (DataUtils.isId(placeId) && !MessageFlag.SEEN.isSet(msg.getInt(COL_FLAGS))) {
      cb.asynchronousCall(new AsynchronousRunnable() {
        @Override
        public void run() {
          try {
            setMessageFlag(placeId, MessageFlag.SEEN, true);
          } catch (MessagingException e) {
            logger.error(e);
          }
        }
      });
    }
    return ResponseObject.response(packet);
  }

  @Timeout
  private void mailChecker(Timer timer) {
    if (!cb.isParameterTimer(timer, PRM_MAIL_CHECK_INTERVAL)) {
      return;
    }
    for (String accountId : qs.getColumn(new SqlSelect()
        .addFields(TBL_ACCOUNTS, sys.getIdName(TBL_ACCOUNTS))
        .addFrom(TBL_ACCOUNTS)
        .setWhere(SqlUtils.notNull(TBL_ACCOUNTS, COL_STORE_SERVER)))) {

      MailAccount account = mail.getAccount(BeeUtils.toLongOrNull(accountId));
      checkMail(true, account, account.getInboxFolder(), null);

      try {
        Thread.sleep(TimeUtils.MILLIS_PER_SECOND);
      } catch (InterruptedException e) {
        logger.warning(e.getMessage());
      }
    }
  }

  private int processMessages(MailAccount account, MailFolder source, MailFolder target,
      Collection<Long> places, boolean move) throws MessagingException {
    Assert.state(!BeeUtils.isEmpty(places), "Empty message list");

    IsCondition wh = sys.idInList(TBL_PLACES, places);

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_PLACES, COL_MESSAGE, COL_FLAGS, COL_MESSAGE_UID)
        .addFrom(TBL_PLACES)
        .setWhere(wh));

    List<Long> lst = new ArrayList<>();

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
    if (target != null) {
      if (account.isStoredRemotedly(target)) {
        if (account.processMessages(uids, source, target, move)) {
          uids = new long[0];
          checkMail(true, account, target, null);
        } else {
          SimpleRowSet contents = qs.getData(new SqlSelect()
              .addFields(TBL_MESSAGES, COL_RAW_CONTENT)
              .addFields(TBL_PLACES, COL_FLAGS)
              .addFrom(TBL_MESSAGES)
              .addFromInner(TBL_PLACES, sys.joinTables(TBL_MESSAGES, TBL_PLACES, COL_MESSAGE))
              .setWhere(wh));

          for (SimpleRow content : contents) {
            Long fileId = content.getLong(COL_RAW_CONTENT);
            Integer mask = content.getInt(COL_FLAGS);
            FileInfo fileInfo = null;
            File file = null;
            InputStream is = null;

            try {
              fileInfo = fs.getFile(fileId);
              file = new File(fileInfo.getPath());
              is = new BufferedInputStream(new FileInputStream(file));
              MimeMessage message = new MimeMessage(null, is);
              Flags on = new Flags();
              Flags off = new Flags();

              for (MessageFlag messageFlag : MessageFlag.values()) {
                Flags flags = messageFlag.isSet(mask) ? on : off;
                Flag flag = MailEnvelope.getFlag(messageFlag);

                if (flag != null) {
                  flags.add(flag);
                } else {
                  flags.add(messageFlag.name());
                }
              }
              message.setFlags(on, true);
              message.setFlags(off, false);
              storeMail(account, message, target);

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
      } else {
        Map<Long, Integer> messages = new HashMap<>();

        for (SimpleRow row : data) {
          messages.put(row.getLong(COL_MESSAGE), row.getInt(COL_FLAGS));
        }
        mail.attachMessages(target.getId(), messages);

        MailMessage mailMessage = new MailMessage(target.getId());
        mailMessage.setMessagesUpdated(true);
        Endpoint.sendToUser(account.getUserId(), mailMessage);
      }
    }
    if (move) {
      account.processMessages(uids, source, null, true);
      mail.detachMessages(wh);

      MailMessage mailMessage = new MailMessage(source.getId());
      mailMessage.setMessagesUpdated(true);
      Endpoint.sendToUser(account.getUserId(), mailMessage);
    }
    return data.getNumberOfRows();
  }

  private int removeMessages(MailAccount account, List<Long> places)
      throws MessagingException {

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_PLACES, COL_FOLDER)
        .addField(TBL_PLACES, sys.getIdName(TBL_PLACES), COL_PLACE)
        .addFrom(TBL_PLACES)
        .setWhere(sys.idInList(TBL_PLACES, places)));

    int c = 0;
    Multimap<Long, Long> folders = HashMultimap.create();

    for (SimpleRow row : data) {
      folders.put(row.getLong(COL_FOLDER), row.getLong(COL_PLACE));
    }
    for (Long folderId : folders.keySet()) {
      MailFolder source = account.findFolder(folderId);
      MailFolder target = account.getTrashFolder();

      if (target == source) {
        target = null;
      }
      c += processMessages(account, source, target, folders.get(folderId), true);
    }
    return c;
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
    int oldValue = BeeUtils.unbox(row.getInt(COL_FLAGS));
    int value;

    if (on) {
      value = flag.set(oldValue);
    } else {
      value = flag.clear(oldValue);
    }
    if (value == oldValue) {
      return value;
    }
    MailAccount account = mail.getAccount(row.getLong(COL_ACCOUNT));
    MailFolder folder = account.findFolder(row.getLong(COL_FOLDER));

    account.setFlag(folder, new long[] {BeeUtils.unbox(row.getLong(COL_MESSAGE_UID))}, flag, on);

    mail.setFlags(placeId, value);

    MailMessage mailMessage = new MailMessage(folder.getId());
    mailMessage.setFlag(flag);
    Endpoint.sendToUser(account.getUserId(), mailMessage);

    return value;
  }

  private void storeMail(MailAccount account, MimeMessage message, MailFolder folder)
      throws MessagingException {

    if (account.addMessageToRemoteFolder(message, folder)) {
      checkMail(true, account, folder, null);
    } else {
      mail.storeMail(account, message, folder.getId(), null);

      MailMessage mailMessage = new MailMessage(folder.getId());
      mailMessage.setMessagesUpdated(true);
      Endpoint.sendToUser(account.getUserId(), mailMessage);
    }
  }

  private void storeMessage(MailAccount account, MimeMessage message, MailFolder folder,
      Long repliedFrom) throws MessagingException {

    if (DataUtils.isId(repliedFrom)) {
      mail.waitForReplied(folder.getId(), new MailEnvelope(message).getUniqueId(), repliedFrom);
    }
    message.setFlag(Flag.SEEN, true);
    storeMail(account, message, folder);
  }

  private int syncFolders(MailAccount account, Folder remoteFolder, MailFolder localFolder)
      throws MessagingException {
    int c = 0;
    Set<String> visitedFolders = new HashSet<>();

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
