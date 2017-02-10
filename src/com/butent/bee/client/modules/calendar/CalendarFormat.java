package com.butent.bee.client.modules.calendar;

import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

public final class CalendarFormat {

  private static final DateTimeFormat DAY_OF_WEEK_FORMAT = Format.parseDateTimePattern("EEEE");

  private static final String[] weekDayNames = new String[7];
  private static final String[] hours = new String[24];

  static {
    JustDate date = TimeUtils.startOfWeek();
    for (int i = 0; i < 7; i++) {
      weekDayNames[i] = DAY_OF_WEEK_FORMAT.format(date);
      TimeUtils.moveOneDayForward(date);
    }

    for (int i = 0; i < hours.length; i++) {
      hours[i] = TimeUtils.padTwo(i) + ":00";
    }
  }

  public static String formatWeekOfYear(JustDate date) {
    if (date == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return BeeUtils.joinWords(TimeUtils.weekOfYear(date), Localized.dictionary().unitWeekShort());
    }
  }

  public static String[] getDayOfWeekNames() {
    return weekDayNames;
  }

  public static String[] getHourLabels() {
    return hours;
  }

  private CalendarFormat() {
  }
}