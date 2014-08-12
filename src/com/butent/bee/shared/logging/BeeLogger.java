package com.butent.bee.shared.logging;

public interface BeeLogger {

  void addSeparator();

  void debug(Object... messages);

  void error(Throwable ex, Object... messages);

  LogLevel getLevel();

  void info(Object... messages);

  boolean isDebugEnabled();

  boolean isErrorEnabled();

  boolean isInfoEnabled();

  boolean isWarningEnabled();

  void log(LogLevel level, Object... messages);

  void setLevel(LogLevel level);

  void severe(Object... messages);

  void warning(Object... messages);
}
