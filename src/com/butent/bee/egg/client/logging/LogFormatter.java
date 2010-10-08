package com.butent.bee.egg.client.logging;

import com.butent.bee.egg.shared.BeeDate;
import com.butent.bee.egg.shared.utils.BeeUtils;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
  public static Level LOG_SEPARATOR_LEVEL = Level.FINE;
  public static String LOG_SEPARATOR_TAG = "-";

  @Override
  public String format(LogRecord record) {
    if (record == null) {
      return null;
    } else if (isSeparator(record)) {
      return LOG_SEPARATOR_TAG;
    } else {
      return BeeUtils.concat(1, new BeeDate(record.getMillis()).toLog(),
          BeeUtils.clip(record.getMessage(), 256));
    }
  }

  public boolean isSeparator(LogRecord record) {
    if (record == null) {
      return false;
    } else {
      return record.getLevel() == LOG_SEPARATOR_LEVEL
          && LOG_SEPARATOR_TAG.equals(record.getMessage());
    }
  }

}
