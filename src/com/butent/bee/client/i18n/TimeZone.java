package com.butent.bee.client.i18n;

import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.i18n.client.TimeZoneInfo;

import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;

public final class TimeZone {

  private static final int STD_SHORT_NAME = 0;
  private static final int STD_LONG_NAME = 1;
  private static final int DLT_SHORT_NAME = 2;
  private static final int DLT_LONG_NAME = 3;

  public static TimeZone createTimeZone(int timeZoneOffsetInMinutes) {
    TimeZone tz = new TimeZone();
    tz.standardOffset = timeZoneOffsetInMinutes;
    tz.timezoneID = composePOSIXTimeZoneID(timeZoneOffsetInMinutes);
    tz.tzNames = new String[2];
    tz.tzNames[0] = composeUTCString(timeZoneOffsetInMinutes);
    tz.tzNames[1] = composeUTCString(timeZoneOffsetInMinutes);
    tz.transitionPoints = null;
    tz.adjustments = null;
    return tz;
  }

  public static TimeZone createTimeZone(String tzJSON) {
    TimeZoneInfo tzData = TimeZoneInfo.buildTimeZoneData(tzJSON);

    return createTimeZone(tzData);
  }

  public static TimeZone createTimeZone(TimeZoneInfo timezoneData) {
    TimeZone tz = new TimeZone();

    tz.timezoneID = timezoneData.getID();
    tz.standardOffset = -timezoneData.getStandardOffset();

    JsArrayString jsTimezoneNames = timezoneData.getNames();

    tz.tzNames = new String[jsTimezoneNames.length()];

    for (int i = 0; i < jsTimezoneNames.length(); i++) {
      tz.tzNames[i] = jsTimezoneNames.get(i);
    }

    JsArrayInteger transitions = timezoneData.getTransitions();

    if (transitions == null || transitions.length() == 0) {
      tz.transitionPoints = null;
      tz.adjustments = null;
    } else {
      int transitionNum = transitions.length() / 2;
      tz.transitionPoints = new int[transitionNum];
      tz.adjustments = new int[transitionNum];

      for (int i = 0; i < transitionNum; ++i) {
        tz.transitionPoints[i] = transitions.get(i * 2);
        tz.adjustments[i] = transitions.get(i * 2 + 1);
      }
    }
    return tz;
  }

  private static String composeGMTString(int offset) {
    char[] data = {'G', 'M', 'T', '-', '0', '0', ':', '0', '0'};
    int x = offset;

    if (x <= 0) {
      data[3] = '+';
      x = -x;
    }
    data[4] += (x / 60) / 10;
    data[5] += (x / 60) % 10;
    data[7] += (x % 60) / 10;
    data[8] += x % 10;

    return new String(data);
  }

  private static String composePOSIXTimeZoneID(int offset) {
    int x = offset;
    if (x == 0) {
      return "Etc/GMT";
    }

    String str;
    if (x < 0) {
      x = -x;
      str = "Etc/GMT-";
    } else {
      str = "Etc/GMT+";
    }
    return str + offsetDisplay(x);
  }

  private static String composeUTCString(int offset) {
    int x = offset;
    if (x == 0) {
      return "UTC";
    }

    String str;
    if (x < 0) {
      x = -x;
      str = "UTC+";
    } else {
      str = "UTC-";
    }
    return str + offsetDisplay(x);
  }

  private static String offsetDisplay(int offset) {
    int hour = offset / 60;
    int mins = offset % 60;
    if (mins == 0) {
      return Integer.toString(hour);
    }
    return Integer.toString(hour) + ":" + Integer.toString(mins);
  }

  private String timezoneID;
  private int standardOffset;
  private String[] tzNames;
  private int[] transitionPoints;
  private int[] adjustments;

  private TimeZone() {
  }

  public int getDaylightAdjustment(HasDateValue date) {
    if (!(date instanceof DateTime)) {
      return 0;
    }
    if (transitionPoints == null) {
      return 0;
    }
    long timeInHours = ((DateTime) date).getTime() / 1000 / 3600;
    int index = 0;
    while (index < transitionPoints.length && timeInHours >= transitionPoints[index]) {
      ++index;
    }
    return (index == 0) ? 0 : adjustments[index - 1];
  }

  public String getGMTString(HasDateValue date) {
    return composeGMTString(getOffset(date));
  }

  public String getID() {
    return timezoneID;
  }

  public String getISOTimeZoneString(HasDateValue date) {
    int offset = -getOffset(date);
    char[] data = {'+', '0', '0', ':', '0', '0'};
    if (offset < 0) {
      data[0] = '-';
      offset = -offset;
    }
    data[1] += (offset / 60) / 10;
    data[2] += (offset / 60) % 10;
    data[4] += (offset % 60) / 10;
    data[5] += offset % 10;
    return new String(data);
  }

  public String getLongName(HasDateValue date) {
    return tzNames[isDaylightTime(date) ? DLT_LONG_NAME : STD_LONG_NAME];
  }

  public int getOffset(HasDateValue date) {
    return standardOffset - getDaylightAdjustment(date);
  }

  public String getRFCTimeZoneString(HasDateValue date) {
    int offset = -getOffset(date);
    char[] data = {'+', '0', '0', '0', '0'};
    if (offset < 0) {
      data[0] = '-';
      offset = -offset;
    }
    data[1] += (offset / 60) / 10;
    data[2] += (offset / 60) % 10;
    data[3] += (offset % 60) / 10;
    data[4] += offset % 10;
    return new String(data);
  }

  public String getShortName(HasDateValue date) {
    return tzNames[isDaylightTime(date) ? DLT_SHORT_NAME : STD_SHORT_NAME];
  }

  public int getStandardOffset() {
    return standardOffset;
  }

  public boolean isDaylightTime(HasDateValue date) {
    return getDaylightAdjustment(date) > 0;
  }
}
