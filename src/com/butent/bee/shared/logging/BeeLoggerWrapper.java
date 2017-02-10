package com.butent.bee.shared.logging;

import com.butent.bee.shared.exceptions.BeeRuntimeException;

public class BeeLoggerWrapper implements BeeLogger {

  private final String loggerName;
  private BeeLogger logger;

  public BeeLoggerWrapper(String name) {
    this.loggerName = name;
  }

  @Override
  public void addSeparator() {
    if (initLogger()) {
      logger.addSeparator();
    }
  }

  @Override
  public void debug(Object... messages) {
    if (initLogger()) {
      logger.debug(messages);
    }
  }

  @Override
  public void error(Throwable ex, Object... messages) {
    if (initLogger()) {
      Throwable cause = ex;

      while (cause.getCause() != null) {
        if (cause instanceof BeeRuntimeException) {
          return;
        }
        cause = cause.getCause();
      }
      logger.error(cause, messages);
    }
  }

  @Override
  public LogLevel getLevel() {
    if (initLogger()) {
      return logger.getLevel();
    } else {
      return null;
    }
  }

  @Override
  public String getName() {
    if (initLogger()) {
      return logger.getName();
    } else {
      return null;
    }
  }

  @Override
  public void info(Object... messages) {
    if (initLogger()) {
      logger.info(messages);
    }
  }

  @Override
  public boolean isDebugEnabled() {
    if (initLogger()) {
      return logger.isDebugEnabled();
    }
    return false;
  }

  @Override
  public boolean isErrorEnabled() {
    if (initLogger()) {
      return logger.isErrorEnabled();
    }
    return false;
  }

  @Override
  public boolean isInfoEnabled() {
    if (initLogger()) {
      return logger.isInfoEnabled();
    }
    return false;
  }

  @Override
  public boolean isWarningEnabled() {
    if (initLogger()) {
      return logger.isWarningEnabled();
    }
    return false;
  }

  @Override
  public void log(LogLevel level, Object... messages) {
    if (initLogger()) {
      logger.log(level, messages);
    }
  }

  @Override
  public void setLevel(LogLevel level) {
    if (initLogger()) {
      logger.setLevel(level);
    }
  }

  @Override
  public void severe(Object... messages) {
    if (initLogger()) {
      logger.severe(messages);
    }
  }

  @Override
  public void warning(Object... messages) {
    if (initLogger()) {
      logger.warning(messages);
    }
  }

  private boolean initLogger() {
    if (logger == null) {
      logger = LogUtils.createLogger(loggerName);
    }
    return logger != null;
  }
}
