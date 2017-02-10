package com.butent.bee.server.websocket;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import com.butent.bee.server.Config;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.Chat;
import com.butent.bee.shared.communication.Presence;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.FiresModificationEvents;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;
import com.butent.bee.shared.websocket.SessionUser;
import com.butent.bee.shared.websocket.WsUtils;
import com.butent.bee.shared.websocket.messages.ChatStateMessage;
import com.butent.bee.shared.websocket.messages.ConfigMessage;
import com.butent.bee.shared.websocket.messages.EchoMessage;
import com.butent.bee.shared.websocket.messages.HasRecipient;
import com.butent.bee.shared.websocket.messages.InfoMessage;
import com.butent.bee.shared.websocket.messages.LogMessage;
import com.butent.bee.shared.websocket.messages.Message;
import com.butent.bee.shared.websocket.messages.ModificationMessage;
import com.butent.bee.shared.websocket.messages.OnlineMessage;
import com.butent.bee.shared.websocket.messages.PresenceMessage;
import com.butent.bee.shared.websocket.messages.ProgressMessage;
import com.butent.bee.shared.websocket.messages.ShowMessage;
import com.butent.bee.shared.websocket.messages.ShowMessage.Subject;
import com.butent.bee.shared.websocket.messages.UsersMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Extension;
import javax.websocket.Extension.Parameter;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/ws/{user-id}")
public class Endpoint {

  private static final String PROPERTY_USER_ID = "UserId";
  private static final String PROPERTY_USER_PRESENCE = "UserPresence";

  private static final Class<? extends RemoteEndpoint> DEFAULT_REMOTE_ENDPOINT_TYPE =
      RemoteEndpoint.Async.class;

  private static final FiresModificationEvents MODIFICATION_SHOOTER =
      (event, locality) -> sendToAll(new ModificationMessage(event));

  private static BeeLogger logger = LogUtils.getLogger(Endpoint.class);

  private static Queue<Session> openSessions = new ConcurrentLinkedQueue<>();
  private static Table<String, String, Long> progressToSession = HashBasedTable.create();

  private static Class<? extends RemoteEndpoint> remoteEndpointType;

  public static void closeProgress(String progressId) {
    Map<String, Long> sessions = progressToSession.row(progressId);

    if (sessions.isEmpty()) {
      logger.info("ws session not found for progress:", progressId);
    } else {
      sessions.keySet().forEach(sessionId -> {
        Session session = findOpenSession(sessionId);

        if (session != null) {
          send(session, ProgressMessage.close(progressId));
        }
      });
      sessions.clear();
    }
  }

  public static String createProgress(long userId, String... label) {
    ProgressMessage message = ProgressMessage.open(BeeUtils.randomString(10), label);

    getUserSessions(userId).forEach(session -> {
      progressToSession.put(message.getProgressId(), session.getId(), 0L);
      send(session, message);
    });
    return message.getProgressId();
  }

  public static FiresModificationEvents getModificationShooter() {
    return MODIFICATION_SHOOTER;
  }

  public static int refreshRows(BeeRowSet rowSet) {
    int count = 0;

    if (!DataUtils.isEmpty(rowSet) && !BeeUtils.isEmpty(rowSet.getViewName())) {
      for (BeeRow row : rowSet) {
        RowUpdateEvent.fire(MODIFICATION_SHOOTER, rowSet.getViewName(), row);
        count++;
      }
    }

    return count;
  }

  public static int refreshViews(String viewName, String... rest) {
    Set<String> viewNames = new HashSet<>();
    if (!BeeUtils.isEmpty(viewName)) {
      viewNames.add(viewName);
    }

    if (rest != null) {
      for (String s : rest) {
        if (!BeeUtils.isEmpty(s)) {
          viewNames.add(s);
        }
      }
    }

    if (viewNames.isEmpty()) {
      logger.warning("refreshViews: view names not specified");
    } else {
      DataChangeEvent.fireRefresh(MODIFICATION_SHOOTER, viewNames);
    }

    return viewNames.size();
  }

  public static void sendToAll(Message message) {
    for (Session session : openSessions) {
      if (session.isOpen()) {
        send(session, message);
      }
    }
  }

  public static void sendToUser(long userId, Message message) {
    getUserSessions(userId).forEach(session -> send(session, message));
  }

  public static void sendToUsers(Collection<Long> users, Message message, String mySessionId) {
    if (BeeUtils.isEmpty(users)) {
      logger.warning("sendToUsers: users not specified");

    } else {
      for (Session session : openSessions) {
        if (session.isOpen() && !BeeUtils.equalsTrim(mySessionId, session.getId())
            && users.contains(getUserId(session))) {
          send(session, message);
        }
      }
    }
  }

  public static boolean updateProgress(String progressId, double value) {
    return updateProgress(progressId, null, value);
  }

  public static boolean updateProgress(String progressId, String label, double value) {
    Map<String, Long> sessions = progressToSession.row(progressId);

    if (sessions.isEmpty()) {
      logger.info("ws session not found for progress:", progressId, "value", value);
      return false;
    } else {
      sessions.entrySet().forEach(entry -> {
        if (!BeeUtils.isEmpty(label)) {
          entry.setValue(0L);
        }
        if ((System.currentTimeMillis() - entry.getValue()) > 10) {
          Session session = findOpenSession(entry.getKey());

          if (session != null) {
            send(session, ProgressMessage.update(progressId, label, value));
          }
          entry.setValue(System.currentTimeMillis());
        }
      });
      return true;
    }
  }

  public static void updateUserData(List<UserData> data) {
    if (BeeUtils.isEmpty(data)) {
      logger.severe("user data is empty");

    } else if (!openSessions.isEmpty()) {
      UsersMessage message = new UsersMessage(data);

      for (Session session : openSessions) {
        if (session.isOpen()) {
          send(session, message);
        }
      }
    }
  }

  private static void dispatch(Session session, Message message) {
    switch (message.getType()) {
      case ADMIN:
      case LOCATION:
      case NOTIFICATION:
      case SIGNALING:
        Session toSession = findOpenSession(((HasRecipient) message).getTo());
        if (toSession != null) {
          send(toSession, message);
        }
        break;

      case CHAT_STATE:
        ChatStateMessage csm = (ChatStateMessage) message;

        if (csm.isValid()) {
          sendToNeighbors(csm.getChat(), csm, session.getId());
        } else {
          WsUtils.onInvalidState(message, toLog(session));
        }
        break;

      case CONFIG:
        ConfigMessage configMessage = (ConfigMessage) message;

        if (configMessage.isValid()) {
          switch (configMessage.getKey()) {
            case ConfigMessage.KEY_REMOTE_ENDPOINT:
              Class<? extends RemoteEndpoint> type =
                  parseRemoteEndpointType(configMessage.getValue());

              if (type != null) {
                setRemoteEndpointType(type);

                String text = BeeUtils.joinWords(configMessage.getKey(), "set to",
                    NameUtils.getClassName(type));
                logger.info(text);

                send(session, new EchoMessage(text));
              }
              break;

            default:
              logger.warning("key not recognized", message);
          }

        } else {
          WsUtils.onEmptyMessage(message, toLog(session));
        }
        break;

      case ECHO:
        send(session, message);
        break;

      case LOG:
        LogMessage logMessage = (LogMessage) message;
        LogLevel level = logMessage.getLevel();

        if (level == null) {
          WsUtils.onInvalidState(message, toLog(session));

        } else if (BeeUtils.isEmpty(logMessage.getText())) {
          logger.setLevel(level);
          logger.log(level, "ws log level set to:", level, toLog(session));

        } else {
          logger.log(level, logMessage.getText());
        }
        break;

      case MODIFICATION:
        ModificationMessage modificationMessage = (ModificationMessage) message;

        if (modificationMessage.isValid()) {
          sendToOtherSessions(modificationMessage, session.getId());
        } else {
          WsUtils.onEmptyMessage(message, toLog(session));
        }
        break;

      case PRESENCE:
        PresenceMessage presenceMessage = (PresenceMessage) message;

        if (presenceMessage.isValid()) {
          setUserPresence(session, presenceMessage.getSessionUser().getPresence());
          sendToOtherSessions(presenceMessage, session.getId());
        } else {
          WsUtils.onInvalidState(message, toLog(session));
        }
        break;

      case PROGRESS:
        ProgressMessage pm = (ProgressMessage) message;
        String progressId = pm.getProgressId();

        if (BeeUtils.isEmpty(progressId)) {
          WsUtils.onEmptyMessage(message, toLog(session));

        } else if (pm.isOpen()) {
          progressToSession.put(progressId, session.getId(), 0L);
          logger.debug("ws activated progress:", progressId, "session:", session.getId());
          send(session, ProgressMessage.activate(progressId));

        } else if (pm.isClosed() || pm.isCanceled()) {
          progressToSession.row(progressId).clear();
          logger.debug("ws removed progress:", progressId);
        } else {
          WsUtils.onInvalidState(message, toLog(session));
        }
        break;

      case SHOW:
        Subject subject = ((ShowMessage) message).getSubject();
        if (subject == null) {
          WsUtils.onEmptyMessage(message, toLog(session));

        } else {
          String caption = subject.getCaption();

          switch (subject) {
            case ENDPOINT:
              send(session, new InfoMessage(caption, getInfo()));
              break;

            case OPEN_SESSIONS:
              send(session, new InfoMessage(caption, getOpenSessionsInfo(openSessions)));
              break;

            case SESSION:
              send(session, new InfoMessage(caption, getSessionInfo(session)));
              break;
          }
        }
        break;

      case CHAT_MESSAGE:
      case INFO:
      case MAIL:
      case ONLINE:
      case PARAMETER:
      case USERS:
        logger.severe("ws message not supported", message, toLog(session));
        break;
    }
  }

  private static Session findOpenSession(String sessionId) {
    for (Session session : openSessions) {
      if (session.getId().endsWith(sessionId) && session.isOpen()) {
        return session;
      }
    }
    logger.warning("ws open session not found", sessionId);
    return null;
  }

  private static List<Property> getExtensionInfo(Extension extension) {
    List<Property> info = new ArrayList<>();

    if (extension == null) {
      info.add(new Property(Extension.class.getSimpleName(), BeeConst.NULL));
    } else {
      info.add(new Property("Name", extension.getName()));

      List<Parameter> parameters = extension.getParameters();
      if (!BeeUtils.isEmpty(parameters)) {
        info.add(new Property("Parameters", BeeUtils.bracket(parameters.size())));

        for (Parameter parameter : parameters) {
          info.add(new Property(parameter.getName(), parameter.getValue()));
        }
      }
    }

    return info;
  }

  private static List<Property> getInfo() {
    List<Property> info = getOpenSessionsInfo(openSessions);

    LogLevel level = logger.getLevel();
    if (level != null) {
      info.add(new Property("Log Level", level.name()));
    }

    if (remoteEndpointType != null) {
      info.add(new Property("Remote Endpoint", NameUtils.getClassName(remoteEndpointType)));
    }

    return info;
  }

  private static List<Property> getOpenSessionsInfo(Collection<Session> sessions) {
    int size = (sessions == null) ? 0 : sessions.size();

    List<Property> info = new ArrayList<>();
    info.add(new Property("Open Sessions", BeeUtils.bracket(size)));

    if (size > 0) {
      int index = 0;

      for (Session session : sessions) {
        String prefix = "Session " + Integer.toString(index++) + BeeConst.STRING_SPACE;

        PropertyUtils.addProperties(info,
            prefix + "Id", session.getId(),
            prefix + "User Principal", getUserName(session),
            prefix + "User Id", getUserId(session),
            prefix + "Open", session.isOpen());
      }
    }

    return info;
  }

  private static synchronized Class<? extends RemoteEndpoint> getRemoteEndpointType() {
    if (remoteEndpointType == null) {
      String value = Config.getProperty(NameUtils.getClassName(RemoteEndpoint.class));
      if (!BeeUtils.isEmpty(value)) {
        remoteEndpointType = parseRemoteEndpointType(value.trim());
      }

      if (remoteEndpointType == null) {
        remoteEndpointType = DEFAULT_REMOTE_ENDPOINT_TYPE;
      }
    }

    return remoteEndpointType;
  }

  private static List<Property> getSessionInfo(Session session) {
    List<Property> info = new ArrayList<>();

    if (session == null) {
      info.add(new Property(Session.class.getSimpleName(), BeeConst.NULL));

    } else {
      Async async = session.getAsyncRemote();
      if (async != null) {
        PropertyUtils.addProperties(info,
            "Async Remote: Batching Allowed", async.getBatchingAllowed(),
            "Async Remote: Send Timeout", async.getSendTimeout());
      }

      Basic basic = session.getBasicRemote();
      if (basic != null) {
        PropertyUtils.addProperties(info,
            "Basic Remote: Batching Allowed", basic.getBatchingAllowed());
      }

      WebSocketContainer wsc = session.getContainer();
      if (wsc != null) {
        info.add(new Property("Container", wsc.getClass().getSimpleName()));
        info.addAll(getWebSocketContainerInfo(wsc));
      }

      PropertyUtils.addProperties(info,
          "Id", session.getId(),
          "Max Binary Message Buffer Size", session.getMaxBinaryMessageBufferSize(),
          "Max Idle Timeout", session.getMaxIdleTimeout(),
          "Max Text Message Buffer Size", session.getMaxTextMessageBufferSize());

      List<Extension> extensions = session.getNegotiatedExtensions();
      if (!BeeUtils.isEmpty(extensions)) {
        info.add(new Property("Negotiated Extensions", BeeUtils.bracket(extensions.size())));

        for (Extension extension : extensions) {
          info.addAll(getExtensionInfo(extension));
        }
      }

      PropertyUtils.addProperties(info,
          "Negotiated Subprotocol", session.getNegotiatedSubprotocol(),
          "Open Sessions", session.getOpenSessions().size(),
          "Path Parameters", session.getPathParameters(),
          "Protocol Version", session.getProtocolVersion(),
          "Query String", session.getQueryString(),
          "Request Parameter Map", session.getRequestParameterMap(),
          "Request URI", session.getRequestURI(),
          "User Principal", getUserName(session),
          "User Properties", session.getUserProperties(),
          "Open", session.isOpen(),
          "Secure", session.isSecure());
    }

    return info;
  }

  private static SessionUser getSessionUser(Session session) {
    return new SessionUser(session.getId(), getUserId(session), getUserPresence(session));
  }

  private static Long getUserId(Session session) {
    if (session != null && session.getUserProperties() != null) {
      Object value = session.getUserProperties().get(PROPERTY_USER_ID);
      if (value instanceof Long) {
        return (Long) value;
      }
    }
    return null;
  }

  private static Presence getUserPresence(Session session) {
    if (session != null && session.getUserProperties() != null) {
      Object value = session.getUserProperties().get(PROPERTY_USER_PRESENCE);
      if (value instanceof Presence) {
        return (Presence) value;
      }
    }
    return null;
  }

  private static String getUserName(Session session) {
    return (session == null || session.getUserPrincipal() == null) ? null
        : session.getUserPrincipal().getName();
  }

  private static Collection<Session> getUserSessions(long userId) {
    List<Session> userSessions = new ArrayList<>();

    openSessions.stream()
        .filter(session -> session.isOpen() && Objects.equals(getUserId(session), userId))
        .forEach(userSessions::add);

    return userSessions;
  }

  private static List<Property> getWebSocketContainerInfo(WebSocketContainer wsc) {
    List<Property> info = new ArrayList<>();

    if (wsc == null) {
      info.add(new Property(WebSocketContainer.class.getSimpleName(), BeeConst.NULL));
    } else {
      PropertyUtils.addProperties(info,
          "Default Async Send Timeout", wsc.getDefaultAsyncSendTimeout(),
          "Default Max Binary Message Buffer Size", wsc.getDefaultMaxBinaryMessageBufferSize(),
          "Default Max Session Idle Timeout", wsc.getDefaultMaxSessionIdleTimeout(),
          "Default Max Text Message Buffer Size", wsc.getDefaultMaxTextMessageBufferSize());

      Set<Extension> extensions = wsc.getInstalledExtensions();
      if (!BeeUtils.isEmpty(extensions)) {
        info.add(new Property("Installed Extensions", BeeUtils.bracket(extensions.size())));

        for (Extension extension : extensions) {
          info.addAll(getExtensionInfo(extension));
        }
      }
    }

    return info;
  }

  private static Class<? extends RemoteEndpoint> parseRemoteEndpointType(String input) {
    char ch = BeeUtils.isEmpty(input) ? BeeConst.CHAR_SPACE : input.trim().toLowerCase().charAt(0);

    switch (ch) {
      case 'a':
        return RemoteEndpoint.Async.class;
      case 'b':
        return RemoteEndpoint.Basic.class;
      default:
        logger.warning("cannot parse remote endpoint type", input);
        return null;
    }
  }

  private static void send(Session session, Message message) {
    if (message == null) {
      WsUtils.onEmptyMessage(message, toLog(session));

    } else {
      Class<? extends RemoteEndpoint> type = getRemoteEndpointType();
      String text = message.encode();

      final String info;

      if (message.isLoggable()) {
        logger.debug("->", message);

        info = BeeUtils.joinWords(message.getType(), "length", text.length(), toLog(session));
        logger.debug("->", info);
      } else {
        info = null;
      }

      if (RemoteEndpoint.Async.class.equals(type)) {
        session.getAsyncRemote().sendText(text, result -> {
          if (result.isOK()) {
            if (info != null) {
              logger.debug("transmitted", info);
            }
          } else {
            logger.error(result.getException());
          }
        });

      } else if (RemoteEndpoint.Basic.class.equals(type)) {
        sendBasic(session, text);

      } else {
        logger.severe(type, "not supported");
      }
    }
  }

  private static synchronized void sendBasic(Session session, String text) {
    try {
      session.getBasicRemote().sendText(text);
    } catch (IOException ex) {
      logger.error(ex);
    }
  }

  private static void sendToNeighbors(Chat chat, Message message, String mySessionId) {
    for (Session session : openSessions) {
      if (session.isOpen() && !mySessionId.equals(session.getId())
          && chat.hasUser(getUserId(session))) {
        send(session, message);
      }
    }
  }

  private static void sendToOtherSessions(Message message, String mySessionId) {
    for (Session session : openSessions) {
      if (session.isOpen() && !mySessionId.equals(session.getId())) {
        send(session, message);
      }
    }
  }

  private static synchronized void setRemoteEndpointType(Class<? extends RemoteEndpoint> type) {
    Endpoint.remoteEndpointType = type;
  }

  private static void setUserId(Session session, Long userId) {
    session.getUserProperties().put(PROPERTY_USER_ID, userId);
  }

  private static void setUserPresence(Session session, Presence presence) {
    session.getUserProperties().put(PROPERTY_USER_PRESENCE, presence);
  }

  private static String toLog(Session session) {
    if (session == null) {
      return null;
    } else {
      Long userId = getUserId(session);
      return BeeUtils.joinOptions("session", session.getId(),
          "principal", getUserName(session),
          "user", (userId == null) ? null : userId.toString());
    }
  }

  @OnClose
  public void onClose(Session session, CloseReason closeReason) {
    openSessions.remove(session);
    progressToSession.column(session.getId()).clear();

    String reasonInfo;
    if (closeReason == null) {
      reasonInfo = null;

    } else {
      Integer code = (closeReason.getCloseCode() == null) ? null
          : closeReason.getCloseCode().getCode();
      String codeInfo = null;

      if (code != null) {
        for (CloseCodes cc : CloseCodes.values()) {
          if (code.equals(cc.getCode())) {
            codeInfo = BeeUtils.joinWords(code, cc.name());
            break;
          }
        }

        if (codeInfo == null) {
          codeInfo = code.toString();
        }
      }

      reasonInfo = BeeUtils.joinOptions("code", codeInfo, "phrase", closeReason.getReasonPhrase());
    }

    logger.info("ws close", reasonInfo, toLog(session));

    if (!openSessions.isEmpty()) {
      setUserPresence(session, Presence.OFFLINE);

      SessionUser sessionUser = getSessionUser(session);
      PresenceMessage message = new PresenceMessage(sessionUser);

      for (Session openSession : openSessions) {
        if (openSession.isOpen()) {
          send(openSession, message);
        }
      }
    }
  }

  @OnError
  public void onError(Session session, Throwable thr) {
    openSessions.remove(session);
    logger.error(thr, "ws error", toLog(session));
  }

  @OnMessage
  public void onMessage(Session session, String data) {
    if (BeeUtils.isEmpty(data)) {
      logger.warning("ws received empty message", toLog(session));

    } else {
      Message message = Message.decode(data);
      if (message != null) {
        if (message.isLoggable()) {
          logger.debug("<-", message);
          logger.debug("<-", message.getType(), "length", data.length(), toLog(session));
        }
        dispatch(session, message);
      }
    }
  }

  @OnOpen
  public void onOpen(@PathParam("user-id") Long userId, Session session) {
    setUserId(session, userId);
    setUserPresence(session, Presence.ONLINE);
    session.setMaxIdleTimeout(Config.getDefaultSessionTimeout() * TimeUtils.MILLIS_PER_MINUTE);

    SessionUser sessionUser = getSessionUser(session);
    PresenceMessage message = new PresenceMessage(sessionUser);

    List<SessionUser> sessionUsers = new ArrayList<>();

    if (!openSessions.isEmpty()) {
      for (Session openSession : openSessions) {
        if (openSession.isOpen()) {
          send(openSession, message);
          sessionUsers.add(getSessionUser(openSession));
        }
      }
    }

    openSessions.add(session);

    logger.info("ws open", toLog(session));

    sessionUsers.add(sessionUser);

    send(session, new OnlineMessage(sessionUsers));
  }
}
