package com.butent.bee.client.logging;

import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.utils.ArrayUtils;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientLogger implements BeeLogger {

  private final Logger logger;

  public ClientLogger(String name) {
    this.logger = Logger.getLogger(name);
  }

  public void addHandler(Handler handler) {
    logger.addHandler(handler);
  }

  @Override
  public void addSeparator() {
    logger.log(LogFormatter.LOG_SEPARATOR_LEVEL, LogFormatter.LOG_SEPARATOR_TAG);
  }

  @Override
  public void debug(Object... messages) {
    if (isDebugEnabled()) {
      logger.config(ArrayUtils.joinWords(messages));
    }
  }

  @Override
  public void error(Throwable ex, Object... messages) {
    if (isErrorEnabled()) {
      logger.log(Level.SEVERE, ArrayUtils.joinWords(messages), ex);
    }
  }

  @Override
  public LogLevel getLevel() {
    LogLevel level = LogLevel.of(logger.getLevel());

    if (level != null) {
      return level;
    } else if (isDebugEnabled()) {
      return LogLevel.DEBUG;
    } else if (isInfoEnabled()) {
      return LogLevel.INFO;
    } else if (isWarningEnabled()) {
      return LogLevel.WARNING;
    } else if (isErrorEnabled()) {
      return LogLevel.ERROR;
    } else {
      return null;
    }
  }

  public PanelHandler getPanelHandler() {
    for (Handler handler : logger.getHandlers()) {
      if (handler instanceof PanelHandler) {
        return (PanelHandler) handler;
      }
    }
    return null;
  }

  @Override
  public void info(Object... messages) {
    if (isInfoEnabled()) {
      logger.info(ArrayUtils.joinWords(messages));
    }
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isLoggable(Level.CONFIG);
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isLoggable(Level.SEVERE);
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isLoggable(Level.INFO);
  }

  @Override
  public boolean isWarningEnabled() {
    return logger.isLoggable(Level.WARNING);
  }

  @Override
  public void log(LogLevel level, Object... messages) {
    switch (level) {
      case DEBUG:
        debug(messages);
        break;
      case ERROR:
        severe(messages);
        break;
      case INFO:
        info(messages);
        break;
      case WARNING:
        warning(messages);
        break;
    }
  }

  @Override
  public void setLevel(LogLevel level) {
    logger.setLevel(level.getLevel());
  }

  @Override
  public void severe(Object... messages) {
    if (isErrorEnabled()) {
      logger.severe(ArrayUtils.joinWords(messages));
    }
  }

  @Override
  public void warning(Object... messages) {
    if (isWarningEnabled()) {
      logger.warning(ArrayUtils.joinWords(messages));
    }
  }
}
