package com.butent.bee.server.communication;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.communication.ChatConstants.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_PHOTO;

import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.administration.ExtensionIcons;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.Chat;
import com.butent.bee.shared.communication.ChatItem;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.websocket.messages.ChatMessage;
import com.butent.bee.shared.websocket.messages.ChatStateMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class ChatBean {

  private static BeeLogger logger = LogUtils.getLogger(ChatBean.class);

  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;

  public ResponseObject doService(RequestInfo reqInfo) {
    ResponseObject response;

    String svc = BeeUtils.trim(reqInfo.getService());

    switch (svc) {
      case Service.GET_CHATS:
        response = getChats();
        break;

      case Service.CREATE_CHAT:
        response = createChat(reqInfo);
        break;

      case Service.GET_CHAT_MESSAGES:
        response = getMessages(reqInfo);
        break;

      case Service.SEND_CHAT_MESSAGE:
        response = putMessage(reqInfo);
        break;

      case Service.ACCESS_CHAT:
        response = accessChat(reqInfo);
        break;

      case Service.UPDATE_CHAT:
        response = updateChat(reqInfo);
        break;

      case Service.DELETE_CHAT:
        response = deleteChat(reqInfo);
        break;

      default:
        String msg = BeeUtils.joinWords("chat service not recognized:", svc);
        logger.warning(msg);
        response = ResponseObject.error(msg);
    }

    return response;
  }

  public ResponseObject getChats() {
    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      String message = BeeUtils.joinWords(NameUtils.getName(this), "current user not available");
      logger.warning(message);

      return ResponseObject.warning(message);
    }

    SqlSelect chatQuery = new SqlSelect()
        .addFields(TBL_CHATS, COL_CHAT_NAME, COL_CHAT_CREATED, COL_CHAT_CREATOR)
        .addFields(TBL_CHAT_USERS, COL_CHAT, COL_CHAT_USER_REGISTERED, COL_CHAT_USER_LAST_ACCESS)
        .addFrom(TBL_CHATS)
        .addFromInner(TBL_CHAT_USERS, sys.joinTables(TBL_CHATS, TBL_CHAT_USERS, COL_CHAT))
        .setWhere(SqlUtils.equals(TBL_CHAT_USERS, COL_CHAT_USER, userId));

    SimpleRowSet chatData = qs.getData(chatQuery);
    if (DataUtils.isEmpty(chatData)) {
      logger.info("no chats found for user", userId);
      return ResponseObject.emptyResponse();
    }

    List<Chat> chats = new ArrayList<>();

    for (SimpleRow row : chatData) {
      Chat chat = createChat(row, userId);
      chats.add(chat);
    }

    if (chats.size() > 1) {
      chats.sort(null);
    }

    logger.info(chats.size(), "chats found for user", userId);
    return ResponseObject.response(chats);
  }

  public void init() {
    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void setRowProperties(ViewQueryEvent event) {
        if (event.isAfter(VIEW_CHATS) && event.hasData()) {
          Long currentUser = usr.getCurrentUserId();

          for (BeeRow row : event.getRowset()) {
            List<Long> users = getChatUsers(row.getId());
            if (BeeUtils.contains(users, currentUser)) {
              users.remove(currentUser);
            }

            if (!BeeUtils.isEmpty(users)) {
              if (users.size() == 1) {
                row.setProperty(COL_PHOTO, usr.getUserPhotoFile(users.get(0)));
              }

              List<String> userNames = new ArrayList<>();
              for (Long u : users) {
                userNames.add(usr.getUserSign(u));
              }

              row.setProperty(PROP_OTHER_USERS, DataUtils.buildIdList(users));
              row.setProperty(PROP_USER_NAMES, BeeUtils.joinItems(userNames));
            }
          }

        } else if (event.isAfter(VIEW_CHAT_MESSAGES) && event.hasData()) {
          SqlSelect query = new SqlSelect()
              .addFields(TBL_CHAT_FILES, COL_CHAT_MESSAGE, COL_CHAT_FILE_CAPTION)
              .addFields(TBL_FILES, COL_FILE_NAME)
              .addFrom(TBL_CHAT_FILES)
              .addFromInner(TBL_FILES,
                  sys.joinTables(TBL_FILES, TBL_CHAT_FILES, COL_CHAT_FILE))
              .setWhere(SqlUtils.inList(TBL_CHAT_FILES, COL_CHAT_MESSAGE,
                  event.getRowset().getRowIds()))
              .addOrder(TBL_CHAT_FILES, sys.getIdName(TBL_CHAT_FILES));

          SimpleRowSet data = qs.getData(query);

          if (!DataUtils.isEmpty(data)) {
            Multimap<Long, String> fileNames = ArrayListMultimap.create();

            for (SimpleRow row : data) {
              String caption = BeeUtils.notEmpty(row.getValue(COL_CHAT_FILE_CAPTION),
                  row.getValue(COL_FILE_NAME));

              if (!BeeUtils.isEmpty(caption)) {
                fileNames.put(row.getLong(COL_CHAT_MESSAGE), caption);
              }
            }

            for (BeeRow row : event.getRowset()) {
              if (fileNames.containsKey(row.getId())) {
                row.setProperty(PROP_FILE_NAMES, BeeUtils.joinItems(fileNames.get(row.getId())));
              }
            }
          }
        }
      }
    });
  }

  private ResponseObject accessChat(RequestInfo reqInfo) {
    Long chatId = reqInfo.getParameterLong(COL_CHAT);
    if (!DataUtils.isId(chatId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_CHAT);
    }

    Long userId = usr.getCurrentUserId();
    if (DataUtils.isId(userId)) {
      return onAccess(chatId, userId);

    } else {
      String message = BeeUtils.joinWords(reqInfo.getService(), "current user not available");
      logger.severe(message);

      return ResponseObject.error(message);
    }
  }

  private ResponseObject createChat(RequestInfo reqInfo) {
    String chatName = reqInfo.getParameter(COL_CHAT_NAME);

    List<Long> users = DataUtils.parseIdList(reqInfo.getParameter(TBL_CHAT_USERS));
    if (BeeUtils.isEmpty(users)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), TBL_CHAT_USERS);
    } else if (users.size() < 2) {
      return ResponseObject.error(reqInfo.getService(), "insufficient", TBL_CHAT_USERS, users);
    }

    Long userId = usr.getCurrentUserId();

    if (!DataUtils.isId(userId)) {
      String message = BeeUtils.joinWords(reqInfo.getService(), "current user not available");
      logger.severe(message);

      return ResponseObject.error(message);
    }

    long created = System.currentTimeMillis();

    SqlInsert insChat = new SqlInsert(TBL_CHATS)
        .addConstant(COL_CHAT_CREATED, created)
        .addConstant(COL_CHAT_CREATOR, userId);

    if (!BeeUtils.isEmpty(chatName)) {
      insChat.addConstant(COL_CHAT_NAME, chatName);
    }

    ResponseObject chatResponse = qs.insertDataWithResponse(insChat);
    if (chatResponse.hasErrors()) {
      return chatResponse;
    }

    Long chatId = chatResponse.getResponseAsLong();

    SqlInsert insUser = new SqlInsert(TBL_CHAT_USERS)
        .addConstant(COL_CHAT, chatId)
        .addConstant(COL_CHAT_USER_REGISTERED, created)
        .addConstant(COL_CHAT_USER, userId);

    ResponseObject userResponse = qs.insertDataWithResponse(insUser);
    if (userResponse.hasErrors()) {
      return userResponse;
    }

    Chat chat = new Chat(chatId, chatName);
    chat.setCreated(created);
    chat.setCreator(userId);

    chat.setUsers(users);

    chat.setRegistered(created);

    for (Long u : users) {
      if (!userId.equals(u)) {
        insUser = new SqlInsert(TBL_CHAT_USERS)
            .addConstant(COL_CHAT, chatId)
            .addConstant(COL_CHAT_USER_REGISTERED, created)
            .addConstant(COL_CHAT_USER, u);

        userResponse = qs.insertDataWithResponse(insUser);
        if (userResponse.hasErrors()) {
          return userResponse;
        }
      }
    }

    return ResponseObject.response(chat);
  }

  private Chat createChat(SimpleRow row, long userId) {
    Chat chat = new Chat(row.getLong(COL_CHAT), row.getValue(COL_CHAT_NAME));

    chat.setValues(row);

    setUsers(chat);

    setMessageStatistics(chat);
    setUnreadCount(chat, userId);

    return chat;
  }

  private int countMessages(long chatId) {
    return qs.sqlCount(TBL_CHAT_MESSAGES, SqlUtils.equals(TBL_CHAT_MESSAGES, COL_CHAT, chatId));
  }

  private int countUnread(long chatId, long userId, Long lastAccess) {
    HasConditions where = SqlUtils.and(SqlUtils.equals(TBL_CHAT_MESSAGES, COL_CHAT, chatId),
        SqlUtils.notEqual(TBL_CHAT_MESSAGES, COL_CHAT_USER, userId));

    if (BeeUtils.isPositive(lastAccess)) {
      where.add(SqlUtils.more(TBL_CHAT_MESSAGES, COL_CHAT_MESSAGE_TIME, lastAccess));
    }

    return qs.sqlCount(TBL_CHAT_MESSAGES, where);
  }

  private ResponseObject deleteChat(RequestInfo reqInfo) {
    Long chatId = reqInfo.getParameterLong(COL_CHAT);
    if (!DataUtils.isId(chatId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_CHAT);
    }

    Long currentUser = usr.getCurrentUserId();
    if (!DataUtils.isId(currentUser)) {
      String message = BeeUtils.joinWords(reqInfo.getService(), "current user not available");
      logger.severe(message);

      return ResponseObject.error(message);
    }

    List<Long> users = getChatUsers(chatId);

    SqlDelete delete = new SqlDelete(TBL_CHATS)
        .setWhere(sys.idEquals(TBL_CHATS, chatId));

    ResponseObject deleteResponse = qs.updateDataWithResponse(delete);
    if (deleteResponse.hasErrors()) {
      return deleteResponse;
    }

    if (!BeeUtils.isEmpty(users)) {
      Chat chat = new Chat(chatId, null);
      chat.setUsers(users);

      ChatStateMessage removeMessage = ChatStateMessage.remove(chat);

      for (Long u : users) {
        if (!currentUser.equals(u)) {
          Endpoint.sendToUser(u, removeMessage);
        }
      }
    }

    return ResponseObject.emptyResponse();
  }

  private Chat getChat(long chatId, long userId) {
    SqlSelect chatQuery = new SqlSelect()
        .addFields(TBL_CHATS, COL_CHAT_NAME, COL_CHAT_CREATED, COL_CHAT_CREATOR)
        .addFields(TBL_CHAT_USERS, COL_CHAT, COL_CHAT_USER_REGISTERED, COL_CHAT_USER_LAST_ACCESS)
        .addFrom(TBL_CHATS)
        .addFromInner(TBL_CHAT_USERS, sys.joinTables(TBL_CHATS, TBL_CHAT_USERS, COL_CHAT))
        .setWhere(SqlUtils.and(sys.idEquals(TBL_CHATS, chatId),
            SqlUtils.equals(TBL_CHAT_USERS, COL_CHAT_USER, userId)));

    SimpleRowSet chatData = qs.getData(chatQuery);
    if (DataUtils.isEmpty(chatData)) {
      logger.warning("chat", chatId, "not found for user", userId);
      return null;
    }
    return createChat(chatData.getRow(0), userId);
  }

  private Map<Long, List<FileInfo>> getChatFiles(long chatId) {
    String idName = sys.getIdName(TBL_CHAT_MESSAGES);

    Map<Long, List<FileInfo>> result = new HashMap<>();

    SqlSelect query = new SqlSelect().setDistinctMode(true)
        .addFields(TBL_CHAT_MESSAGES, idName)
        .addFrom(TBL_CHAT_MESSAGES)
        .addFromInner(TBL_CHAT_FILES,
            sys.joinTables(TBL_CHAT_MESSAGES, TBL_CHAT_FILES, COL_CHAT_MESSAGE))
        .setWhere(SqlUtils.equals(TBL_CHAT_MESSAGES, COL_CHAT, chatId));

    Set<Long> messageIds = qs.getLongSet(query);
    for (Long messageId : messageIds) {
      result.put(messageId, getMessageFiles(messageId));
    }

    return result;
  }

  private List<Long> getChatUsers(long chatId) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_CHAT_USERS, COL_CHAT_USER)
        .addFrom(TBL_CHAT_USERS)
        .setWhere(SqlUtils.equals(TBL_CHAT_USERS, COL_CHAT, chatId))
        .addOrder(TBL_CHAT_USERS, COL_CHAT_USER_REGISTERED, sys.getIdName(TBL_CHAT_USERS));

    return qs.getLongList(query);
  }

  private ChatItem getLastMessage(long chatId) {
    String idName = sys.getIdName(TBL_CHAT_MESSAGES);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_CHAT_MESSAGES, COL_CHAT_USER, COL_CHAT_MESSAGE_TIME, COL_CHAT_MESSAGE_TEXT,
            idName)
        .addFrom(TBL_CHAT_MESSAGES)
        .setWhere(SqlUtils.equals(TBL_CHAT_MESSAGES, COL_CHAT, chatId))
        .addOrderDesc(TBL_CHAT_MESSAGES, COL_CHAT_MESSAGE_TIME)
        .setLimit(1);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return null;
    }

    SimpleRow row = data.getRow(0);
    List<FileInfo> files = getMessageFiles(row.getLong(idName));

    return new ChatItem(row.getLong(COL_CHAT_USER), row.getLong(COL_CHAT_MESSAGE_TIME),
        row.getValue(COL_CHAT_MESSAGE_TEXT), files);
  }

  private List<FileInfo> getMessageFiles(long messageId) {
    List<FileInfo> files = new ArrayList<>();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_CHAT_FILES, COL_CHAT_FILE, COL_CHAT_FILE_CAPTION)
        .addFields(TBL_FILES, COL_FILE_HASH, COL_FILE_NAME, COL_FILE_SIZE, COL_FILE_TYPE)
        .addFrom(TBL_CHAT_FILES)
        .addFromInner(TBL_FILES,
            sys.joinTables(TBL_FILES, TBL_CHAT_FILES, COL_CHAT_FILE))
        .setWhere(SqlUtils.equals(TBL_CHAT_FILES, COL_CHAT_MESSAGE, messageId))
        .addOrder(TBL_CHAT_FILES, sys.getIdName(TBL_CHAT_FILES));

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        FileInfo file = new FileInfo(row.getLong(COL_CHAT_FILE), row.getValue(COL_FILE_HASH),
            row.getValue(COL_FILE_NAME), row.getLong(COL_FILE_SIZE), row.getValue(COL_FILE_TYPE));

        file.setCaption(row.getValue(COL_CHAT_FILE_CAPTION));
        file.setIcon(ExtensionIcons.getIcon(file.getName()));
        files.add(file);
      }
    }

    return files;
  }

  private ResponseObject getMessages(RequestInfo reqInfo) {
    Long chatId = reqInfo.getParameterLong(COL_CHAT);
    if (!DataUtils.isId(chatId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_CHAT);
    }

    String idName = sys.getIdName(TBL_CHAT_MESSAGES);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_CHAT_MESSAGES, COL_CHAT_USER, COL_CHAT_MESSAGE_TIME, COL_CHAT_MESSAGE_TEXT,
            idName)
        .addFrom(TBL_CHAT_MESSAGES)
        .setWhere(SqlUtils.equals(TBL_CHAT_MESSAGES, COL_CHAT, chatId))
        .addOrder(TBL_CHAT_MESSAGES, COL_CHAT_MESSAGE_TIME);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.emptyResponse();
    }

    if (reqInfo.hasParameter(COL_CHAT_USER_LAST_ACCESS)) {
      Long userId = usr.getCurrentUserId();
      if (DataUtils.isId(userId)) {
        onAccess(chatId, userId);
      } else {
        logger.warning(reqInfo.getService(), "current user not available");
      }
    }

    Map<Long, List<FileInfo>> files = getChatFiles(chatId);

    List<ChatItem> messages = new ArrayList<>();

    for (SimpleRow row : data) {
      messages.add(new ChatItem(row.getLong(COL_CHAT_USER), row.getLong(COL_CHAT_MESSAGE_TIME),
          row.getValue(COL_CHAT_MESSAGE_TEXT), files.get(row.getLong(idName))));
    }

    logger.info(messages.size(), "messages found in chat", chatId);
    return ResponseObject.response(messages).setSize(messages.size());
  }

  private ResponseObject onAccess(long chatId, long userId) {
    SqlUpdate update = new SqlUpdate(TBL_CHAT_USERS)
        .addConstant(COL_CHAT_USER_LAST_ACCESS, System.currentTimeMillis())
        .setWhere(SqlUtils.equals(TBL_CHAT_USERS, COL_CHAT, chatId, COL_CHAT_USER, userId));

    return qs.updateDataWithResponse(update);
  }

  public void putMessage(String input, Long userId, Map<String, String> linkData) {
    Boolean activeAssistant = qs.getBoolean(new SqlSelect()
        .addFields(TBL_USER_SETTINGS, COL_ASSISTANT)
        .addFrom(TBL_USER_SETTINGS)
        .setWhere(SqlUtils.equals(TBL_USER_SETTINGS, COL_USER, userId)));

    if (BeeUtils.isTrue(activeAssistant)) {
      String messageText = BeeUtils.joinWords(
          usr.getDictionary(userId).chatReminderTitle(),
          input);

      ChatItem chatItem = new ChatItem(0, messageText, linkData);
      ChatMessage message = new ChatMessage(0, chatItem);

      message.getChatItem().setTime(System.currentTimeMillis());

      Endpoint.sendToUser(userId, message);
    }
  }

  private ResponseObject putMessage(RequestInfo reqInfo) {
    String input = reqInfo.getParameter(COL_CHAT_MESSAGE);
    if (BeeUtils.isEmpty(input)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_CHAT_MESSAGE);
    }

    ChatMessage message = ChatMessage.restore(input);
    if (message == null) {
      return ResponseObject.error(reqInfo.getService(), "invalid message:", input);
    }

    String from = reqInfo.getParameter(Service.VAR_FROM);
    if (BeeUtils.isEmpty(from)) {
      logger.warning(reqInfo.getService(), "parameter not found:", Service.VAR_FROM);
    }

    message.getChatItem().setTime(System.currentTimeMillis());

    SqlInsert insert = new SqlInsert(TBL_CHAT_MESSAGES)
        .addConstant(COL_CHAT, message.getChatId())
        .addConstant(COL_CHAT_USER, message.getChatItem().getUserId())
        .addConstant(COL_CHAT_MESSAGE_TIME, message.getChatItem().getTime());

    String text = message.getChatItem().getText();
    if (!BeeUtils.isEmpty(text)) {
      insert.addConstant(COL_CHAT_MESSAGE_TEXT, text);
    }

    ResponseObject response = qs.insertDataWithResponse(insert);
    if (response.hasErrors()) {
      return response;
    }

    if (message.getChatItem().hasFiles()) {
      if (response.hasResponse(Long.class)) {
        Long messageId = response.getResponseAsLong();

        for (FileInfo fileInfo : message.getChatItem().getFiles()) {
          if (DataUtils.isId(fileInfo.getId())) {
            SqlInsert fileInsert = new SqlInsert(TBL_CHAT_FILES)
                .addConstant(COL_CHAT_MESSAGE, messageId)
                .addConstant(COL_CHAT_FILE, fileInfo.getId());

            if (!BeeUtils.isEmpty(fileInfo.getCaption())) {
              fileInsert.addConstant(COL_CHAT_FILE_CAPTION, fileInfo.getCaption());
            }

            qs.insertData(fileInsert);
          }
        }

      } else {
        logger.warning(reqInfo.getService(), "message id not available, cannot save",
            TBL_CHAT_FILES);
      }
    }

    onAccess(message.getChatId(), message.getChatItem().getUserId());

    List<Long> users = getChatUsers(message.getChatId());
    if (BeeUtils.isEmpty(from)) {
      users.remove(message.getChatItem().getUserId());
    }

    if (BeeUtils.isEmpty(users)) {
      logger.warning(reqInfo.getService(), "chat", message.getChatId(), "users not available");
    } else {
      Endpoint.sendToUsers(users, message, from);
    }

    return response;
  }

  private void setMessageStatistics(Chat chat) {
    int count = countMessages(chat.getId());
    chat.setMessageCount(count);

    if (count > 0) {
      chat.setLastMessage(getLastMessage(chat.getId()));
    } else {
      chat.setLastMessage(null);
    }
  }

  private void setUnreadCount(Chat chat, long userId) {
    int count;

    if (chat.getLastMessage() == null) {
      count = 0;

    } else if (BeeUtils.isMore(chat.getLastAccess(), chat.getLastMessage().getTime())) {
      count = 0;

    } else if (!BeeUtils.isPositive(chat.getLastAccess())) {
      count = chat.getMessageCount();

    } else {
      count = countUnread(chat.getId(), userId, chat.getLastAccess());
    }

    chat.setUnreadCount(count);
  }

  private void setUsers(Chat chat) {
    chat.setUsers(getChatUsers(chat.getId()));
  }

  private void setUserValues(Chat chat, long userId) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_CHAT_USERS, COL_CHAT_USER_REGISTERED, COL_CHAT_USER_LAST_ACCESS)
        .addFrom(TBL_CHAT_USERS)
        .setWhere(SqlUtils.equals(TBL_CHAT_USERS, COL_CHAT, chat.getId(), COL_CHAT_USER, userId));

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      logger.warning("chat", chat.getId(), "user", userId, "not found");
      return;
    }

    chat.setUserValues(data.getRow(0));
    setUnreadCount(chat, userId);
  }

  private ResponseObject updateChat(RequestInfo reqInfo) {
    Long chatId = reqInfo.getParameterLong(COL_CHAT);
    if (!DataUtils.isId(chatId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_CHAT);
    }

    String chatName;
    boolean nameChanged;

    List<Long> newUsers;
    boolean usersChanged;

    if (reqInfo.hasParameter(COL_CHAT_NAME)) {
      chatName = reqInfo.getParameter(COL_CHAT_NAME);
      nameChanged = true;

    } else if (COL_CHAT_NAME.equals(reqInfo.getParameter(Service.VAR_CLEAR))) {
      nameChanged = true;
      chatName = null;

    } else {
      nameChanged = false;
      chatName = null;
    }

    if (reqInfo.hasParameter(TBL_CHAT_USERS)) {
      newUsers = DataUtils.parseIdList(reqInfo.getParameter(TBL_CHAT_USERS));
      usersChanged = true;

      if (newUsers.size() < 2) {
        return ResponseObject.error(reqInfo.getService(), "insufficient", TBL_CHAT_USERS, newUsers);
      }

    } else {
      newUsers = new ArrayList<>();
      usersChanged = false;
    }

    if (!nameChanged && !usersChanged) {
      return ResponseObject.warning(reqInfo.getService(), chatId, "nothing changed");
    }

    Long currentUser = usr.getCurrentUserId();
    if (!DataUtils.isId(currentUser)) {
      String message = BeeUtils.joinWords(reqInfo.getService(), "current user not available");
      logger.severe(message);

      return ResponseObject.error(message);
    }

    List<Long> oldUsers = getChatUsers(chatId);

    Long defUser;
    if (!BeeUtils.isEmpty(newUsers)) {
      defUser = newUsers.get(0);
    } else if (!BeeUtils.isEmpty(oldUsers)) {
      defUser = oldUsers.get(0);
    } else {
      defUser = null;
    }

    if (!DataUtils.isId(defUser)) {
      String message = BeeUtils.joinWords(reqInfo.getService(), "chat", chatId, "no users found");
      logger.severe(message);

      return ResponseObject.error(message);
    }

    if (nameChanged) {
      SqlUpdate update = new SqlUpdate(TBL_CHATS)
          .addConstant(COL_CHAT_NAME, chatName)
          .setWhere(sys.idEquals(TBL_CHATS, chatId));

      ResponseObject updateResponse = qs.updateDataWithResponse(update);
      if (updateResponse.hasErrors()) {
        return updateResponse;
      }
    }

    Set<Long> addUsers = new HashSet<>();
    Set<Long> removeUsers = new HashSet<>();

    if (usersChanged) {
      addUsers.addAll(newUsers);
      addUsers.removeAll(oldUsers);

      removeUsers.addAll(oldUsers);
      removeUsers.removeAll(newUsers);
    }

    if (!removeUsers.isEmpty()) {
      SqlDelete delete = new SqlDelete(TBL_CHAT_USERS)
          .setWhere(SqlUtils.and(SqlUtils.equals(TBL_CHAT_USERS, COL_CHAT, chatId),
              SqlUtils.inList(TBL_CHAT_USERS, COL_CHAT_USER, removeUsers)));

      ResponseObject deleteResponse = qs.updateDataWithResponse(delete);
      if (deleteResponse.hasErrors()) {
        return deleteResponse;
      }
    }

    if (!addUsers.isEmpty()) {
      for (Long u : addUsers) {
        SqlInsert insert = new SqlInsert(TBL_CHAT_USERS)
            .addConstant(COL_CHAT, chatId)
            .addConstant(COL_CHAT_USER_REGISTERED, System.currentTimeMillis())
            .addConstant(COL_CHAT_USER, u);

        ResponseObject insertResponse = qs.insertDataWithResponse(insert);
        if (insertResponse.hasErrors()) {
          return insertResponse;
        }
      }
    }

    Chat chat = getChat(chatId, defUser);
    if (chat == null) {
      String message = BeeUtils.joinWords(reqInfo.getService(), "cannot create chat", chatId,
          "for user", defUser);
      logger.warning(message);

      return ResponseObject.warning(message);
    }

    if (!removeUsers.isEmpty()) {
      ChatStateMessage removeMessage = ChatStateMessage.remove(chat);
      for (Long u : removeUsers) {
        Endpoint.sendToUser(u, removeMessage);
      }
    }

    ChatStateMessage message;

    List<Long> recipients = new ArrayList<>(chat.getUsers());
    recipients.remove(currentUser);

    for (Long u : recipients) {
      setUserValues(chat, u);

      if (oldUsers.contains(u)) {
        message = ChatStateMessage.update(chat);
      } else {
        message = ChatStateMessage.add(chat);
      }

      Endpoint.sendToUser(u, message);
    }

    return ResponseObject.emptyResponse();
  }
}
