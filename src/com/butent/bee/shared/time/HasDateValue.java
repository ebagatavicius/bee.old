package com.butent.bee.shared.time;

import com.butent.bee.shared.BeeSerializable;

import java.util.Date;

/**
 * Requires any implementing classes to be able to get date formats in {@code JustDate},
 * {@code DateTime} and standard Java date type.
 */

public interface HasDateValue extends BeeSerializable, HasYearMonth {

  HasDateValue fromDate(JustDate justDate);

  HasDateValue fromDateTime(DateTime dateTime);

  HasDateValue fromJava(Date date);

  DateTime getDateTime();

  int getDom();

  int getDow();

  int getDoy();

  int getHour();

  Date getJava();

  int getMillis();

  int getMinute();

  int getSecond();

  long getTime();

  int getTimezoneOffset();

  boolean hasTimePart();

  void setDom(int dom);

  boolean supportsTimezoneOffset();

  String toTimeStamp();
}
