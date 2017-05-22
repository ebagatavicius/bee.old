package com.butent.bee.client.websocket;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.user.client.Timer;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Settings;
import com.butent.bee.client.composite.Thermometer;
import com.butent.bee.client.dom.Features;
import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;
import com.butent.bee.shared.websocket.WsUtils;
import com.butent.bee.shared.websocket.messages.Message;
import com.butent.bee.shared.websocket.messages.ProgressMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import elemental.client.Browser;
import elemental.events.CloseEvent;
import elemental.events.MessageEvent;
import elemental.html.WebSocket;
import elemental.js.events.JsEvent;

public final class Endpoint {

  private enum ReadyState {
    CONNECTING(0),
    OPEN(1),
    CLOSING(2),
    CLOSED(3);

    private static ReadyState getByValue(int value) {
      for (ReadyState state : values()) {
        if (state.value == value) {
          return state;
        }
      }
      return null;
    }

    private final int value;

    ReadyState(int value) {
      this.value = value;
    }
  }

  private static final int PROGRESS_ACTIVATION_TIMEOUT = 2000;

  private static BeeLogger logger = LogUtils.getLogger(Endpoint.class);

  private static Boolean enabled;

  private static WebSocket socket;
  private static String sessionId;

  private static Map<String, Consumer<String>> progressQueue = new HashMap<>();
  private static Map<String, Function<ProgressMessage, Boolean>> progressHandlers = new HashMap<>();

  private static Consumer<Boolean> onlineCallback;

  public static void cancelProgress(String progressId) {
    Assert.notEmpty(progressId);

    ProgressMessage pm = ProgressMessage.cancel(progressId);

    handleProgress(pm);
    removeProgress(progressId);
    send(pm);
  }

  public static void close() {
    if (socket != null && !isClosed()) {
      socket.close();
    }
  }

  public static void dequeueProgress(String progressId) {
    if (progressQueue.containsKey(progressId)) {
      progressQueue.remove(progressId);
    }
  }

  public static void enqueueProgress(final String progressId, Consumer<String> consumer) {
    Assert.notEmpty(progressId);
    Assert.notNull(consumer);

    if (!isEnabled()) {
      consumer.accept(null);
      return;
    }

    progressQueue.put(progressId, consumer);
    send(ProgressMessage.open(progressId));

    Timer timer = new Timer() {
      @Override
      public void run() {
        if (!progressQueue.isEmpty()) {
          Consumer<String> starter = progressQueue.remove(progressId);
          if (starter != null) {
            starter.accept(null);
          }
        }
      }
    };

    timer.schedule(PROGRESS_ACTIVATION_TIMEOUT);
  }

  public static List<Property> getInfo() {
    List<Property> info = new ArrayList<>();

    if (socket == null) {
      PropertyUtils.addProperties(info,
          "Enabled", (enabled == null) ? BeeConst.NULL : Boolean.toString(enabled),
          WebSocket.class.getSimpleName(), BeeConst.NULL);

    } else {
      PropertyUtils.addProperties(info,
          "Session Id", sessionId,
          "Binary Type", socket.getBinaryType(),
          "Buffered Amount", socket.getBufferedAmount(),
          "Extensions", socket.getExtensions(),
          "Protocol", socket.getProtocol(),
          "Ready State", getReadyState(),
          "Url", socket.getUrl());
    }

    info.add(new Property("Progress Queue", BeeUtils.bracket(progressQueue.size())));
    if (!progressQueue.isEmpty()) {
      int i = 0;
      for (String key : progressQueue.keySet()) {
        info.add(new Property(BeeUtils.joinWords("Progress", i++), key));
      }
    }

    return info;
  }

  public static String getSessionId() {
    return sessionId;
  }

  public static boolean handleProgress(ProgressMessage pm) {
    Assert.notNull(pm);

    if (progressHandlers.containsKey(pm.getProgressId())) {
      return BeeUtils.unbox(progressHandlers.get(pm.getProgressId()).apply(pm));
    }
    return false;
  }

  public static String createProgress(String caption, String id) {
    InlineLabel close = new InlineLabel(String.valueOf(BeeConst.CHAR_TIMES));
    Thermometer th = new Thermometer(caption, BeeConst.DOUBLE_ONE, close);

    if (!BeeUtils.isEmpty(id)) {
      th.setId(id);
    }
    String progressId = BeeKeeper.getScreen().addProgress(th);

    if (!BeeUtils.isEmpty(progressId)) {
      close.addClickHandler(ev -> cancelProgress(progressId));
    }
    return progressId;
  }

  public static void initProgress(String caption, final Consumer<String> consumer) {
    String progressId;

    if (Endpoint.isOpen()) {
      progressId = createProgress(caption, null);
    } else {
      progressId = null;
    }
    if (progressId == null) {
      consumer.accept(null);
    } else {
      enqueueProgress(progressId, input -> {
        String progress = progressId;

        if (BeeUtils.isEmpty(input)) {
          cancelProgress(progress);
          progress = null;
        }
        consumer.accept(progress);
      });
    }
  }

  public static boolean isClosed() {
    return socket != null && socket.getReadyState() == ReadyState.CLOSED.value;
  }

  public static boolean isEnabled() {
    if (enabled == null) {
      enabled = Features.supportsWebSockets() && !BeeConst.isOff(Settings.getWebSocketUrl());
    }
    return enabled;
  }

  public static boolean isOpen() {
    return socket != null && socket.getReadyState() == ReadyState.OPEN.value;
  }

  public static void open(Long userId, final Consumer<Boolean> callback) {
    if (!isEnabled() || userId == null) {
      if (callback != null) {
        callback.accept(false);
      }
    } else if (socket == null || isClosed()) {
      try {
        socket = Browser.getWindow().newWebSocket(url(userId));
      } catch (JavaScriptException ex) {
        socket = null;
        logger.severe(ex, "cannot open websocket");

        if (callback != null) {
          callback.accept(false);
        }
      }
      if (socket != null) {
        socket.setOnopen(evt -> {
          onlineCallback = callback;
          onOpen();
        });
        socket.setOnclose(evt -> onClose((CloseEvent) evt));
        socket.setOnerror(evt -> onError((JsEvent) evt));
        socket.setOnmessage(evt -> onMessage((MessageEvent) evt));
      }
    } else if (callback != null) {
      callback.accept(isOpen());
    }
    checkConnection();
  }

  public static void registerProgressHandler(String progressId,
      Function<ProgressMessage, Boolean> function) {
    Assert.notEmpty(progressId);
    Assert.notNull(function);

    if (isEnabled()) {
      progressHandlers.put(progressId, function);
    }
  }

  public static void removeProgress(String progressId) {
    BeeKeeper.getScreen().removeProgress(progressId);
    dequeueProgress(progressId);
    unregisterProgressHandler(progressId);
  }

  public static void send(Message message) {
    if (!isOpen()) {
      if (isEnabled()) {
        logger.warning("ws is not open");
      }

    } else if (message == null) {
      WsUtils.onEmptyMessage(message);

    } else {
      String data = message.encode();
      socket.send(data);

      if (message.isLoggable()) {
        logger.info(BeeConst.STRING_RIGHT_ARROW, data.length(),
            message.getType().name().toLowerCase(), message.brief());
      }
    }
  }

  public static void setSessionId(String sessionId) {
    Endpoint.sessionId = sessionId;
  }

  public static void unregisterProgressHandler(String progressId) {
    if (progressHandlers.containsKey(progressId)) {
      progressHandlers.remove(progressId);
    }
  }

  static void online() {
    if (onlineCallback != null) {
      onlineCallback.accept(true);
      onlineCallback = null;
    }
  }

  static boolean startProgress(String progressId) {
    if (!progressQueue.isEmpty()) {
      Consumer<String> starter = progressQueue.remove(progressId);
      if (starter != null) {
        starter.accept(progressId);
        return true;
      }
    }

    return false;
  }

  private static void checkConnection() {
    BeeKeeper.getScreen().showConnectionStatus(isOpen());
  }

  private static String getReadyState() {
    if (socket == null) {
      return null;

    } else {
      int value = socket.getReadyState();
      ReadyState readyState = ReadyState.getByValue(value);

      return (readyState == null) ? Integer.toString(value) : readyState.name().toLowerCase();
    }
  }

  private static void onClose(CloseEvent event) {
    if (socket != null) {
      setSessionId(null);

      String eventInfo = (event == null) ? null
          : BeeUtils.joinOptions("code", Integer.toString(event.getCode()),
          "reason", event.getReason());
      logger.info("close", socket.getUrl(), getReadyState(), eventInfo);
    }
    checkConnection();
  }

  private static void onError(JsEvent event) {
    logger.severe("ws error event", JsUtils.toString(event));
    checkConnection();
  }

  private static void onMessage(MessageEvent event) {
    Object data = event.getData();

    if (data instanceof String) {
      Message message = Message.decode((String) data);
      if (message != null) {
        if (message.isLoggable()) {
          logger.info(BeeConst.STRING_LEFT_ARROW, ((String) data).length(),
              message.getType().name().toLowerCase(), message.brief());
        }
        MessageDispatcher.dispatch(message);
      }

    } else if (data == null) {
      logger.warning("ws received data is null");
    } else {
      logger.warning("ws unknown received data type", NameUtils.getName(data));
    }
  }

  private static void onOpen() {
    if (socket != null) {
      logger.info(socket.getUrl(), getReadyState());
    }
    checkConnection();
  }

  private static String url(long userId) {
    String url = Settings.getWebSocketUrl();
    if (BeeUtils.isEmpty(url)) {
      String href = GWT.getHostPageBaseURL();
      url = "ws" + Paths.ensureEndSeparator(href.substring(4)) + "ws/";
    }
    return Paths.ensureEndSeparator(url) + Long.toString(userId);
  }

  private Endpoint() {
  }
}
