package com.butent.bee.client.datepicker;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.shared.DateTimeFormatInfo;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

class Model {

  private static final int WEEKEND_START;
  private static final int WEEKEND_END;

  private static final String[] DAY_OF_WEEK_NAMES;
  private static final String[] DAY_OF_MONTH_NAMES;

  static {
    DateTimeFormatInfo info = LocaleInfo.getCurrentLocale().getDateTimeFormatInfo();
    WEEKEND_START = (info.weekendStart() + 5) % 7 + 1;
    WEEKEND_END = (info.weekendEnd() + 5) % 7 + 1;

    DAY_OF_WEEK_NAMES = new String[7];
    String[] arr = ArrayUtils.copyOf(info.weekdaysNarrow());
    for (int i = 1; i < arr.length; i++) {
      DAY_OF_WEEK_NAMES[i - 1] = arr[i];
    }
    DAY_OF_WEEK_NAMES[6] = arr[0];

    DAY_OF_MONTH_NAMES = new String[31];
    for (int i = 0; i < DAY_OF_MONTH_NAMES.length; ++i) {
      DAY_OF_MONTH_NAMES[i] = Integer.toString(i + 1);
    }
  }

  static String formatDayOfMonth(JustDate date) {
    return DAY_OF_MONTH_NAMES[date.getDom() - 1];
  }

  static String formatDayOfWeek(int dow) {
    return DAY_OF_WEEK_NAMES[dow];
  }

  static boolean isWeekend(int dow) {
    return dow == WEEKEND_START || dow == WEEKEND_END;
  }

  private final YearMonth currentMonth;

  Model(JustDate date) {
    currentMonth = new YearMonth(date);
  }

  String format(YearMonth ym) {
    return BeeUtils.joinWords(ym.getYear(), Format.renderMonthFullStandalone(ym).toLowerCase());
  }

  YearMonth getCurrentMonth() {
    return currentMonth;
  }

  boolean isInCurrentMonth(JustDate date) {
    return TimeUtils.sameMonth(currentMonth, date);
  }

  void setCurrentMonth(YearMonth ym) {
    currentMonth.setYearMonth(ym);
  }

  void shiftCurrentMonth(int deltaMonths) {
    currentMonth.shiftMonth(deltaMonths);
  }
}
