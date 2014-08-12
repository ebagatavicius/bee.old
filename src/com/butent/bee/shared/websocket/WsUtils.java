package com.butent.bee.shared.websocket;

import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.websocket.messages.Message;

public final class WsUtils {

  private static BeeLogger logger = LogUtils.getLogger(WsUtils.class);

  public static void onEmptyMessage(Message message) {
    onEmptyMessage(message, null);
  }

  public static void onEmptyMessage(Message message, String sessionInfo) {
    logger.warning("message is empty:", message);
    if (!BeeUtils.isEmpty(sessionInfo)) {
      logger.warning(sessionInfo);
    }
  }

  public static void onInvalidState(Message message) {
    onInvalidState(message, null);
  }

  public static void onInvalidState(Message message, String sessionInfo) {
    logger.warning("unsupported message state:", message);
    if (!BeeUtils.isEmpty(sessionInfo)) {
      logger.warning(sessionInfo);
    }
  }

  private WsUtils() {
  }
}
