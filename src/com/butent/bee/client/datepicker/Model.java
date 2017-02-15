package com.butent.bee.client.datepicker;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Model {

  private static final List<String> DAY_OF_WEEK_NAMES = Format.getWeekdaysNarrowStandalone();
  private static final List<String> DAY_OF_MONTH_NAMES =
      IntStream.rangeClosed(1, 31).mapToObj(Integer::toString).collect(Collectors.toList());

  static String formatDayOfMonth(JustDate date) {
    return DAY_OF_MONTH_NAMES.get(date.getDom() - 1);
  }

  static String formatDayOfWeek(int dow) {
    return DAY_OF_WEEK_NAMES.get(dow);
  }

  private final YearMonth currentMonth;

  Model(JustDate date) {
    currentMonth = new YearMonth(date);
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
