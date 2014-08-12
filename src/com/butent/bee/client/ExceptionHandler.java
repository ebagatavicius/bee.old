package com.butent.bee.client;

import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;

import com.butent.bee.client.logging.LogFormatter;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.logging.LogUtils;

/**
 * handles uncaught exceptions and logs them.
 */

public class ExceptionHandler implements UncaughtExceptionHandler {

  private static final BeeLogger logger = LogUtils.getLogger(ExceptionHandler.class);

  @Override
  public void onUncaughtException(Throwable err) {
    logger.error(err, "Uncaught Exception");

    Throwable cause = err.getCause();
    int i = 0;
    while (cause != null && i++ < 100) {
      logger.error(cause);
      cause = cause.getCause();
    }

    LogUtils.logStack(logger, LogLevel.ERROR, err);

    logger.info(LogFormatter.LOG_SEPARATOR_TAG);
  }
}
