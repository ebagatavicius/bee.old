package com.butent.bee.shared.logging;

import com.butent.bee.shared.utils.BeeUtils;

import java.util.logging.Level;

public enum LogLevel {
  DEBUG(Level.CONFIG), INFO(Level.INFO), WARNING(Level.WARNING), ERROR(Level.SEVERE);

  public static LogLevel of(Level level) {
    if (level == null) {
      return null;
    }

    for (LogLevel lvl : LogLevel.values()) {
      if (lvl.level.equals(level)) {
        return lvl;
      }
    }
    return null;
  }

  public static LogLevel parse(String input) {
    for (LogLevel lvl : LogLevel.values()) {
      if (BeeUtils.inListSame(input, lvl.name(), lvl.getLevel().getName())) {
        return lvl;
      }
    }
    return null;
  }

  private final Level level;

  private LogLevel(Level level) {
    this.level = level;
  }

  public Level getLevel() {
    return level;
  }
}