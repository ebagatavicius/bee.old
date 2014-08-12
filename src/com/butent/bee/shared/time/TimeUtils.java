package com.butent.bee.shared.time;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.RangeOptions;

import java.util.Date;
import java.util.List;

/**
 * Contains methods for date/time calculations.
 */
public final class TimeUtils {

  public static final int FIELD_ERA = 0;

  public static final int FIELD_YEAR = 1;
  public static final int FIELD_MONTH = 2;
  public static final int FIELD_WEEK_OF_YEAR = 3;
  public static final int FIELD_WEEK_OF_MONTH = 4;
  public static final int FIELD_DATE = 5;
  public static final int FIELD_DAY_OF_MONTH = 5;
  public static final int FIELD_DAY_OF_YEAR = 6;
  public static final int FIELD_DAY_OF_WEEK = 7;
  public static final int FIELD_DAY_OF_WEEK_IN_MONTH = 8;

  public static final int FIELD_AM_PM = 9;
  public static final int FIELD_HOUR = 10;
  public static final int FIELD_HOUR_OF_DAY = 11;
  public static final int FIELD_MINUTE = 12;
  public static final int FIELD_SECOND = 13;
  public static final int FIELD_MILLISECOND = 14;

  public static final int FIELD_ZONE_OFFSET = 15;
  public static final int FIELD_DST_OFFSET = 16;

  public static final int FIELD_YEAR_WOY = 17;

  public static final int FIELD_DOW_LOCAL = 18;

  public static final int FIELD_EXTENDED_YEAR = 19;
  public static final int FIELD_JULIAN_DAY = 20;

  public static final int FIELD_MILLISECONDS_IN_DAY = 21;

  public static final int DAYS_PER_WEEK = 7;
  public static final int HOURS_PER_DAY = 24;
  public static final int MINUTES_PER_HOUR = 60;
  public static final int SECONDS_PER_MINUTE = 60;
  public static final int MILLIS_PER_SECOND = 1000;

  public static final int MINUTES_PER_DAY = HOURS_PER_DAY * MINUTES_PER_HOUR;

  public static final int MILLIS_PER_MINUTE = SECONDS_PER_MINUTE * MILLIS_PER_SECOND;
  public static final long MILLIS_PER_HOUR = MINUTES_PER_HOUR * MILLIS_PER_MINUTE;
  public static final long MILLIS_PER_DAY = HOURS_PER_DAY * MILLIS_PER_HOUR;
  public static final long MILLIS_PER_WEEK = DAYS_PER_WEEK * MILLIS_PER_DAY;

  public static final RangeOptions OPEN_REQUIRED = new RangeOptions(false, true, true);
  public static final RangeOptions OPEN_NOT_REQUIRED = new RangeOptions(false, true, false);
  public static final RangeOptions CLOSED_REQUIRED = new RangeOptions(false, false, true);
  public static final RangeOptions CLOSED_NOT_REQUIRED = new RangeOptions(false, false, false);

  public static final char DATE_FIELD_SEPARATOR = '-';
  public static final char DATE_TIME_SEPARATOR = ' ';
  public static final char TIME_FIELD_SEPARATOR = ':';
  public static final char MILLIS_SEPARATOR = '.';

  public static final String PERIOD_SEPARATOR = "..";

  private static final String[] FIELD_NAME = {
      "ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH",
      "DAY_OF_MONTH", "DAY_OF_YEAR", "DAY_OF_WEEK",
      "DAY_OF_WEEK_IN_MONTH", "AM_PM", "HOUR", "HOUR_OF_DAY",
      "MINUTE", "SECOND", "MILLISECOND", "ZONE_OFFSET",
      "DST_OFFSET", "YEAR_WOY", "DOW_LOCAL", "EXTENDED_YEAR",
      "JULIAN_DAY", "MILLISECONDS_IN_DAY",
  };

  private static final Splitter FIELD_SPLITTER =
      Splitter.on(CharMatcher.inRange(BeeConst.CHAR_ZERO, BeeConst.CHAR_NINE).negate())
          .omitEmptyStrings().trimResults();

  private static final int MINIMAL_DAYS_IN_FIRST_WEEK = 4;

  private static final String MS = "ms";

  /**
   * Adds an amount of field type data to the date.
   * @param date the initial date to add to
   * @param field the field type to add
   * @param amount the amount to add
   */
  public static void add(DateTime date, int field, int amount) {
    Assert.notNull(date);
    if (amount == 0) {
      return;
    }
    date.setTime(date.getTime() + getDelta(date, field, amount));
  }

  public static void addDay(JustDate date, int amount) {
    Assert.notNull(date);
    if (amount != 0) {
      date.setDays(date.getDays() + amount);
    }
  }

  public static void addHour(DateTime date, int amount) {
    add(date, FIELD_HOUR, amount);
  }

  public static void addMinute(DateTime date, int amount) {
    add(date, FIELD_MINUTE, amount);
  }

  public static <T extends HasDateValue> T clamp(T dt, T min, T max) {
    if (dt == null) {
      return min;
    } else if (min == null) {
      return (max == null) ? dt : min(dt, max);
    } else if (max == null) {
      return max(dt, min);
    } else {
      return min(max(dt, min), max);
    }
  }

  public static DateTime combine(HasDateValue datePart, DateTime timePart) {
    if (datePart == null) {
      return timePart;
    }
    if (timePart == null) {
      return datePart.getDateTime();
    }

    return new DateTime(datePart.getYear(), datePart.getMonth(), datePart.getDom(),
        timePart.getHour(), timePart.getMinute(), timePart.getSecond(), timePart.getMillis());
  }

  public static DateTime combine(HasDateValue datePart, Long timePartMillis) {
    if (datePart == null) {
      return null;
    } else if (timePartMillis == null) {
      return new DateTime(datePart.getYear(), datePart.getMonth(), datePart.getDom());
    } else {
      return new DateTime(datePart.getYear(), datePart.getMonth(), datePart.getDom(),
          0, 0, 0, timePartMillis);
    }
  }

  public static int compare(HasYearMonth d1, HasYearMonth d2) {
    if (d1 == null) {
      if (d2 == null) {
        return BeeConst.COMPARE_EQUAL;
      } else {
        return BeeConst.COMPARE_LESS;
      }

    } else if (d2 == null) {
      return BeeConst.COMPARE_MORE;

    } else if (d1 instanceof YearMonth) {
      if (d2 instanceof YearMonth) {
        return ((YearMonth) d1).compareTo((YearMonth) d2);
      } else if (d2 instanceof JustDate) {
        return ((YearMonth) d1).getDate().compareTo((JustDate) d2);
      } else if (d2 instanceof HasDateValue) {
        return ((YearMonth) d1).getDate().getDateTime()
            .compareTo(((HasDateValue) d2).getDateTime());
      }

    } else if (d1 instanceof JustDate) {
      if (d2 instanceof JustDate) {
        return ((JustDate) d1).compareTo((JustDate) d2);
      } else if (d2 instanceof YearMonth) {
        return ((JustDate) d1).compareTo(((YearMonth) d2).getDate());
      } else if (d2 instanceof HasDateValue) {
        return ((JustDate) d1).getDateTime().compareTo(((HasDateValue) d2).getDateTime());
      }

    } else if (d1 instanceof DateTime) {
      if (d2 instanceof DateTime) {
        return ((DateTime) d1).compareTo((DateTime) d2);
      } else if (d2 instanceof YearMonth) {
        return ((DateTime) d1).compareTo(((YearMonth) d2).getDate().getDateTime());
      } else if (d2 instanceof HasDateValue) {
        return ((DateTime) d1).compareTo(((HasDateValue) d2).getDateTime());
      }
    }

    Assert.untouchable();
    return BeeConst.COMPARE_EQUAL;
  }

  public static int countFields(String s) {
    if (BeeUtils.isEmpty(s)) {
      return 0;
    }
    return Iterables.size(FIELD_SPLITTER.split(s));
  }

  /**
   * Gets the difference between {@code start} and {@code end}.
   * @param start the start time
   * @param end the end time
   * @return the difference between {@code start} and {@code end} in days.
   */
  public static int dateDiff(DateTime start, DateTime end) {
    return fieldDifference(start, end, FIELD_DATE);
  }

  public static String dateToString(HasDateValue date) {
    if (date == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return dateToString(date.getYear(), date.getMonth(), date.getDom());
    }
  }

  public static String dateToString(int year, int month, int dom) {
    return yearToString(year) + DATE_FIELD_SEPARATOR + monthToString(month)
        + DATE_FIELD_SEPARATOR + dayOfMonthToString(dom);
  }

  public static int dayDiff(HasDateValue start, HasDateValue end) {
    Assert.notNull(start);
    Assert.notNull(end);

    return end.getDate().getDays() - start.getDate().getDays();
  }

  public static String dayOfMonthToString(int dom) {
    return padTwo(dom);
  }

  public static int dom() {
    return today().getDom();
  }

  public static String elapsedMillis(long start) {
    return BeeUtils.bracket(System.currentTimeMillis() - start);
  }

  /**
   * Returns the elapsed time in seconds.
   * @param start the start time
   * @return the elapsed time in seconds from the specified start in brackets.
   */
  public static String elapsedSeconds(long start) {
    return BeeUtils.bracket(toSeconds(System.currentTimeMillis() - start));
  }

  public static JustDate endOfMonth() {
    return endOfMonth(today());
  }

  public static JustDate endOfMonth(HasYearMonth ref) {
    return endOfMonth(ref, 0);
  }

  public static JustDate endOfMonth(HasYearMonth ref, int increment) {
    Assert.notNull(ref);
    return YearMonth.get(ref).shiftMonth(increment).getLast();
  }

  public static JustDate endOfPreviousMonth(HasYearMonth ref) {
    return endOfMonth(ref, -1);
  }

  public static JustDate endOfYear() {
    return endOfYear(year());
  }

  public static JustDate endOfYear(int year) {
    return endOfYear(year, 0);
  }

  public static JustDate endOfYear(int year, int increment) {
    return new JustDate(year + increment, 12, 31);
  }

  public static boolean equals(HasDateValue x, HasDateValue y) {
    if (x instanceof JustDate && y instanceof JustDate) {
      return sameDate(x, y);
    }
    if (x instanceof DateTime && y instanceof DateTime) {
      return sameDateTime(x, y);
    }
    return false;
  }

  /**
   * Gets the specified field's difference between {@code start} and {@code end}.
   * @param start the start time
   * @param end the end time
   * @param field the used field. E.g 1(years),2(months),5(days) etc.
   * @return difference between {@code start} and {@code end}.
   */
  public static int fieldDifference(DateTime start, DateTime end, int field) {
    Assert.notNull(start);
    Assert.notNull(end);

    long startMs = start.getTime();
    long endMs = end.getTime();

    if (startMs == endMs) {
      return 0;
    }
    if (startMs > endMs) {
      return -fieldDifference(end, start, field);
    }

    int min = 0;
    int max = 1;

    for (;;) {
      long ms = startMs + getDelta(start, field, max);
      if (ms == endMs) {
        return max;
      } else if (ms > endMs) {
        break;
      } else {
        max <<= 1;
        Assert.isPositive(max, "Field difference too large to fit into int");
      }
    }

    while ((max - min) > 1) {
      int t = (min + max) / 2;
      long ms = startMs + getDelta(start, field, t);
      if (ms == endMs) {
        return t;
      } else if (ms > endMs) {
        max = t;
      } else {
        min = t;
      }
    }

    return min;
  }

  public static JustDate getDate(HasDateValue src, int increment) {
    Assert.notNull(src);
    return new JustDate(src.getDate().getDays() + increment);
  }

  public static int getField(List<Integer> fields, int index) {
    return BeeUtils.isIndex(fields, index) ? fields.get(index) : 0;
  }

  public static long getMillis(int hour, int minute, int second, long millis) {
    long z = 0;
    if (hour != 0) {
      z += hour * MILLIS_PER_HOUR;
    }
    if (minute != 0) {
      z += minute * MILLIS_PER_MINUTE;
    }
    if (second != 0) {
      z += second * MILLIS_PER_SECOND;
    }
    return z + millis;
  }

  public static JustDate goMonth(JustDate ref, int increment) {
    Assert.notNull(ref);
    if (increment == 0) {
      return ref;
    } else {
      YearMonth ym = YearMonth.get(ref).shiftMonth(increment);
      return new JustDate(ym.getYear(), ym.getMonth(), ref.getDom());
    }
  }

  public static DateTime goMonth(DateTime ref, int increment) {
    Assert.notNull(ref);
    if (increment == 0) {
      return ref;
    } else {
      return combine(goMonth(ref.getDate(), increment), ref);
    }
  }

  public static boolean isBetween(HasDateValue dt, HasDateValue min, HasDateValue max,
      RangeOptions options) {
    Assert.notNull(options);

    if (dt == null) {
      return !options.isLowerRequired() && min == null;

    } else if (min == null && max == null) {
      return !options.isLowerRequired() && !options.isUpperRequired();

    } else if (dt instanceof DateTime || min instanceof DateTime || max instanceof DateTime) {
      return options.contains(DateTime.get(min), DateTime.get(max), DateTime.get(dt));

    } else if (dt instanceof JustDate || min instanceof JustDate || max instanceof JustDate) {
      return options.contains(JustDate.get(min), JustDate.get(max), JustDate.get(dt));

    } else {
      return false;
    }
  }

  public static boolean isBetweenExclusiveNotRequired(HasDateValue dt, HasDateValue min,
      HasDateValue max) {
    return isBetween(dt, min, max, OPEN_NOT_REQUIRED);
  }

  public static boolean isBetweenExclusiveRequired(HasDateValue dt, HasDateValue min,
      HasDateValue max) {
    return isBetween(dt, min, max, OPEN_REQUIRED);
  }

  public static boolean isBetweenInclusiveNotRequired(HasDateValue dt, HasDateValue min,
      HasDateValue max) {
    return isBetween(dt, min, max, CLOSED_NOT_REQUIRED);
  }

  public static boolean isBetweenInclusiveRequired(HasDateValue dt, HasDateValue min,
      HasDateValue max) {
    return isBetween(dt, min, max, CLOSED_REQUIRED);
  }

  public static boolean isCurrentYear(HasYearMonth ym) {
    return ym != null && ym.getYear() == year();
  }

  /**
   * Checks if {@code x} is and instance of HasDateValue or Date.
   * @param x the Object to check
   * @return true if {@code x} is an instance of any of these types, false otherwise.
   */
  public static boolean isDateOrDateTime(Object x) {
    return x instanceof HasDateValue || x instanceof Date;
  }

  public static boolean isDow(int dow) {
    return dow >= 1 && dow <= DAYS_PER_WEEK;
  }

  public static boolean isLeq(HasYearMonth d1, HasYearMonth d2) {
    return compare(d1, d2) <= 0;
  }

  public static boolean isLess(HasYearMonth d1, HasYearMonth d2) {
    return compare(d1, d2) < 0;
  }

  public static boolean isMeq(HasYearMonth d1, HasYearMonth d2) {
    return compare(d1, d2) >= 0;
  }

  public static boolean isMonth(Integer month) {
    return month != null && month >= 1 && month <= 12;
  }

  public static boolean isMore(HasYearMonth d1, HasYearMonth d2) {
    return compare(d1, d2) > 0;
  }

  public static boolean isToday(HasDateValue dt) {
    return sameDate(dt, today());
  }

  public static boolean isWeekend(HasDateValue dt) {
    return (dt == null) ? false : dt.getDow() >= 6;
  }

  public static boolean isYear(Integer year) {
    return year != null && year >= 1900 && year < 2100;
  }

  public static <T extends HasDateValue> T max(T d1, T d2) {
    return isMeq(d1, d2) ? d1 : d2;
  }

  public static boolean maybeDom(int dom) {
    return dom >= 1 && dom <= 31;
  }

  /**
   * @param millis the value to convert
   * @return the String representation of milliseconds.
   */
  public static String millisToString(int millis) {
    if (millis >= 0 && millis < MILLIS_PER_SECOND) {
      return Integer.toString(millis + MILLIS_PER_SECOND).substring(1);
    } else {
      return Integer.toString(millis);
    }
  }

  public static <T extends HasDateValue> T min(T d1, T d2) {
    return isLeq(d1, d2) ? d1 : d2;
  }

  public static int minuteDiff(DateTime start, DateTime end) {
    Assert.notNull(start);
    Assert.notNull(end);

    long minutes = end.getTime() / MILLIS_PER_MINUTE - start.getTime() / MILLIS_PER_MINUTE;
    return (int) minutes;
  }

  public static int minutesSinceDayStarted(DateTime dt) {
    Assert.notNull(dt);
    return dt.getHour() * MINUTES_PER_HOUR + dt.getMinute();
  }

  public static int month() {
    return today().getMonth();
  }

  public static int monthDiff(HasYearMonth start, HasYearMonth end) {
    Assert.notNull(start);
    Assert.notNull(end);

    return end.getYear() * 12 + end.getMonth() - start.getYear() * 12 - start.getMonth();
  }

  public static int monthLength(HasYearMonth ym) {
    Assert.notNull(ym);
    return Grego.monthLength(ym.getYear(), ym.getMonth());
  }

  public static String monthToString(int month) {
    return padTwo(month);
  }

  public static void moveOneDayForward(JustDate date) {
    addDay(date, 1);
  }

  public static JustDate nextDay(HasDateValue ref) {
    return nextDay(ref, 1);
  }

  public static JustDate nextDay(HasDateValue ref, int increment) {
    Assert.notNull(ref);
    return new JustDate(ref.getDate().getDays() + increment);
  }

  public static DateTime nextHour(DateTime ref, int increment) {
    Assert.notNull(ref);
    long millis = (ref.getTime() / MILLIS_PER_HOUR + 1) * MILLIS_PER_HOUR;
    return new DateTime(millis + MILLIS_PER_HOUR * increment);
  }

  public static DateTime nextHour(int increment) {
    return nextHour(new DateTime(), increment);
  }

  public static String normalize(AbstractDate x) {
    if (x == null) {
      return null;
    } else {
      return x.serialize();
    }
  }

  public static int normalizeYear(int year) {
    if (year < 0 || year >= 100) {
      return year;
    } else {
      return year + 2000;
    }
  }

  public static DateTime nowHours() {
    return nowHours(0);
  }

  public static DateTime nowHours(int increment) {
    long millis = System.currentTimeMillis() / MILLIS_PER_HOUR * MILLIS_PER_HOUR;
    return new DateTime(millis + increment * MILLIS_PER_HOUR);
  }

  public static DateTime nowMillis() {
    return new DateTime();
  }

  public static DateTime nowMillis(long increment) {
    return new DateTime(System.currentTimeMillis() + increment);
  }

  public static DateTime nowMinutes() {
    return nowMinutes(0);
  }

  public static DateTime nowMinutes(int increment) {
    long millis = System.currentTimeMillis() / MILLIS_PER_MINUTE * MILLIS_PER_MINUTE;
    return new DateTime(millis + increment * MILLIS_PER_MINUTE);
  }

  public static DateTime nowSeconds() {
    return nowSeconds(0);
  }

  public static DateTime nowSeconds(int increment) {
    long millis = System.currentTimeMillis() / MILLIS_PER_SECOND * MILLIS_PER_SECOND;
    return new DateTime(millis + increment * MILLIS_PER_SECOND);
  }

  /**
   * Left pads and integer {@code number} by adding "0" to size of two.
   * @param number the value to pad
   * @return a String representation of the padded value {@code number} if
   *         {@code number >=0 and number < 10}, otherwise a non-padded value String.
   */
  public static String padTwo(int number) {
    if (number >= 0 && number < 10) {
      return BeeConst.STRING_ZERO + number;
    } else {
      return String.valueOf(number);
    }
  }

  public static JustDate parseDate(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }

    if (BeeUtils.isSuffix(input, MS)) {
      return new JustDate(BeeUtils.toLong(BeeUtils.removeSuffix(input, MS)));
    }

    List<Integer> fields = parseFields(input);
    if (BeeUtils.isPositive(BeeUtils.max(fields))) {
      return parseDate(input, fields);
    } else {
      return null;
    }
  }

  public static DateTime parseDateTime(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }

    if (BeeUtils.isSuffix(input, MS)) {
      return new DateTime(BeeUtils.toLong(BeeUtils.removeSuffix(input, MS)));
    }

    List<Integer> fields = parseFields(input);
    if (!BeeUtils.isPositive(BeeUtils.max(fields))) {
      return null;
    }

    int count = fields.size();

    switch (count) {
      case 1:
        String digits = BeeUtils.parseDigits(input);
        int len = digits.length();

        if (len <= 8) {
          return DateTime.get(parseDate(input, fields));
        } else if (len <= 10) {
          return parseDateTime(splitDigits(digits, BeeConst.STRING_SPACE, 4, 2, 2, 2));
        } else if (len <= 12) {
          return parseDateTime(splitDigits(digits, BeeConst.STRING_SPACE, 4, 2, 2, 2, 2));
        } else {
          return parseDateTime(splitDigits(digits, BeeConst.STRING_SPACE, 4, 2, 2, 2, 2, 2));
        }

      case 2:
      case 3:
        return DateTime.get(parseDate(input, fields));

      default:
        return new DateTime(normalizeYear(getField(fields, 0)),
            getField(fields, 1), getField(fields, 2),
            getField(fields, 3), getField(fields, 4),
            getField(fields, 5), getField(fields, 6));
    }
  }

  public static List<Integer> parseFields(String input) {
    List<Integer> result = Lists.newArrayList();
    if (BeeUtils.isEmpty(input)) {
      return result;
    }

    for (String field : FIELD_SPLITTER.split(input)) {
      for (int i = field.length(); i > 0; i--) {
        if (BeeUtils.isInt(field.substring(0, i))) {
          result.add(BeeUtils.toInt(field.substring(0, i)));
          break;
        }
      }
    }
    return result;
  }

  public static Long parseTime(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }

    List<Integer> fields = parseFields(input);
    if (fields.isEmpty()) {
      return null;
    }

    if (fields.size() == 1 && fields.get(0) >= 100) {
      String digits = BeeUtils.toString(fields.get(0));
      int len = digits.length();

      List<Integer> slices;

      switch (len) {
        case 3:
          slices = Lists.newArrayList(1, 2);
          break;

        case 4:
          slices = Lists.newArrayList(2, 2);
          break;

        case 5:
          slices = Lists.newArrayList(1, 2, 2);
          break;

        default:
          slices = Lists.newArrayList(2, 2, 2);
      }

      fields.clear();
      fields.addAll(parseDigits(digits, slices));
    }

    long millis = MILLIS_PER_HOUR * getField(fields, 0) + MILLIS_PER_MINUTE * getField(fields, 1)
        + MILLIS_PER_SECOND * getField(fields, 2) + getField(fields, 3);
    return millis;
  }

  public static JustDate previousDay(HasDateValue ref) {
    return nextDay(ref, -1);
  }

  /**
   * Generates a random JustDate between {@code min} and {@code max}.
   * @param min the minimum JustDate
   * @param max the maximum JustDate
   * @return a JustDate between specified {@code min} and {@code max}.
   */
  public static JustDate randomDate(JustDate min, JustDate max) {
    Assert.notNull(min);
    Assert.notNull(max);
    return new JustDate(BeeUtils.randomInt(min.getDays(), max.getDays()));
  }

  /**
   * Generates a random DateTime between {@code min} and {@code max}.
   * @param min the minimum DateTime
   * @param max the maximum DateTime
   * @return a DateTime between specified {@code min} and {@code max}.
   */
  public static DateTime randomDateTime(DateTime min, DateTime max) {
    Assert.notNull(min);
    Assert.notNull(max);
    return new DateTime(BeeUtils.randomLong(min.getTime(), max.getTime()));
  }

  public static String render(HasYearMonth dt) {
    if (dt == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return dt.toString();
    }
  }

  public static String renderCompact(HasDateValue dt) {
    return renderCompact(dt, false);
  }

  public static String renderCompact(HasDateValue dt, boolean dropCurrentYear) {
    if (dt == null) {
      return BeeConst.STRING_EMPTY;

    } else if (dt instanceof DateTime) {
      if (dropCurrentYear && isCurrentYear(dt)) {
        String ds = renderMonthDay(dt);
        String ts = ((DateTime) dt).toCompactTimeString();
        return BeeUtils.isEmpty(ts) ? ds : (ds + DATE_TIME_SEPARATOR + ts);
      } else {
        return ((DateTime) dt).toCompactString();
      }

    } else if (dropCurrentYear && isCurrentYear(dt)) {
      return renderMonthDay(dt);
    } else {
      return dt.toString();
    }
  }

  public static String renderDateTime(long time) {
    return renderDateTime(time, false);
  }

  public static String renderDateTime(long time, boolean showMillis) {
    return new DateTime(showMillis ? time : (time - time % MILLIS_PER_SECOND)).toString();
  }

  public static String renderMinutes(int minutes, boolean leadingZero) {
    int hours = minutes / MINUTES_PER_HOUR;
    return (leadingZero ? padTwo(hours) : BeeUtils.toString(hours)) + TIME_FIELD_SEPARATOR
        + padTwo(minutes % MINUTES_PER_HOUR);
  }

  public static String renderMonthDay(HasDateValue date) {
    if (date == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return renderMonthDay(date.getMonth(), date.getDom());
    }
  }

  public static String renderMonthDay(int month, int dom) {
    return monthToString(month) + DATE_FIELD_SEPARATOR + dayOfMonthToString(dom);
  }

  public static String renderPeriod(DateTime start, DateTime end) {
    return renderPeriod(start, end, false);
  }

  public static String renderPeriod(DateTime start, DateTime end, boolean dropCurrentYear) {
    if (start == null) {
      if (end == null) {
        return BeeConst.STRING_EMPTY;
      } else {
        return PERIOD_SEPARATOR + renderCompact(end, dropCurrentYear);
      }

    } else if (end == null) {
      return renderCompact(start, dropCurrentYear) + PERIOD_SEPARATOR;

    } else if (sameDate(start, end)) {
      return renderCompact(start, dropCurrentYear) + PERIOD_SEPARATOR + end.toCompactTimeString();

    } else {
      return renderCompact(start, dropCurrentYear) + PERIOD_SEPARATOR
          + renderCompact(end, dropCurrentYear);
    }
  }

  public static String renderTime(int hour, int minute, int second, int millis,
      boolean leadingZero) {
    StringBuilder sb = new StringBuilder();
    sb.append(leadingZero ? padTwo(hour) : BeeUtils.toString(hour));
    sb.append(TIME_FIELD_SEPARATOR).append(padTwo(minute));

    if (second > 0 || millis > 0) {
      sb.append(TIME_FIELD_SEPARATOR).append(padTwo(second));
    }
    if (millis > 0) {
      sb.append(MILLIS_SEPARATOR).append(millisToString(millis));
    }
    return sb.toString();
  }

  public static String renderTime(long millis, boolean leadingZero) {
    if (millis < 0) {
      return BeeConst.STRING_EMPTY;
    }

    int hour = (int) (millis / MILLIS_PER_HOUR);
    int remaining = (int) (millis % MILLIS_PER_HOUR);

    int minute = remaining / MILLIS_PER_MINUTE;
    remaining %= MILLIS_PER_MINUTE;

    int second = remaining / MILLIS_PER_SECOND;
    remaining %= MILLIS_PER_SECOND;

    return renderTime(hour, minute, second, remaining, leadingZero);
  }

  public static boolean sameDate(HasDateValue x, HasDateValue y) {
    if (x == null || y == null) {
      return x == y;
    }
    return x.getYear() == y.getYear() && x.getMonth() == y.getMonth() && x.getDom() == y.getDom();
  }

  public static boolean sameDateTime(HasDateValue x, HasDateValue y) {
    if (x == null || y == null) {
      return x == y;
    }
    return x.getDateTime().getTime() == y.getDateTime().getTime();
  }

  public static boolean sameMonth(HasYearMonth x, HasYearMonth y) {
    if (x == null || y == null) {
      return false;
    }
    return x.getYear() == y.getYear() && x.getMonth() == y.getMonth();
  }

  public static DateTime startOfDay() {
    return new DateTime(today());
  }

  public static DateTime startOfDay(HasDateValue ref) {
    return startOfDay(ref, 0);
  }

  public static DateTime startOfDay(HasDateValue ref, int increment) {
    Assert.notNull(ref);
    return new DateTime(ref.getYear(), ref.getMonth(), ref.getDom() + increment);
  }

  public static DateTime startOfDay(int increment) {
    return startOfDay(today(), increment);
  }

  public static JustDate startOfMonth() {
    JustDate date = new JustDate();
    int dom = date.getDom();
    if (dom > 1) {
      addDay(date, 1 - dom);
    }
    return date;
  }

  public static JustDate startOfMonth(HasYearMonth ref) {
    return startOfMonth(ref, 0);
  }

  public static JustDate startOfMonth(HasYearMonth ref, int increment) {
    Assert.notNull(ref);
    if (increment == 0) {
      return new JustDate(ref.getYear(), ref.getMonth(), 1);
    } else {
      return YearMonth.get(ref).shiftMonth(increment).getDate();
    }
  }

  public static JustDate startOfNextMonth(HasYearMonth ref) {
    return startOfMonth(ref, 1);
  }

  public static JustDate startOfPreviousMonth(HasYearMonth ref) {
    return startOfMonth(ref, -1);
  }

  public static JustDate startOfQuarter(HasYearMonth ref) {
    return startOfQuarter(ref, 0);
  }

  public static JustDate startOfQuarter(HasYearMonth ref, int increment) {
    Assert.notNull(ref);
    return startOfMonth(ref, increment * 3 - (ref.getMonth() - 1) % 3);
  }

  public static JustDate startOfWeek() {
    return startOfWeek(today());
  }

  public static JustDate startOfWeek(HasDateValue ref) {
    return startOfWeek(ref, 0);
  }

  public static JustDate startOfWeek(HasDateValue ref, int increment) {
    Assert.notNull(ref);
    JustDate date = new JustDate(ref.getYear(), ref.getMonth(), ref.getDom());

    int incrDays = 0;
    int dow = ref.getDow();
    if (dow > 1) {
      incrDays -= dow - 1;
    }
    if (increment != 0) {
      incrDays += increment * 7;
    }

    if (incrDays != 0) {
      addDay(date, incrDays);
    }
    return date;
  }

  public static JustDate startOfWeekYear(int year, int minimalDaysInFirstWeek) {
    JustDate date = new JustDate(year, 1, 1);
    int dow = date.getDow();

    if (dow == 1) {
      return date;
    } else if (DAYS_PER_WEEK - dow + 1 < minimalDaysInFirstWeek) {
      return startOfWeek(date, 1);
    } else {
      return startOfWeek(date);
    }
  }

  public static JustDate startOfYear() {
    return startOfYear(year());
  }

  public static JustDate startOfYear(HasYearMonth ref) {
    return startOfYear(ref, 0);
  }

  public static JustDate startOfYear(HasYearMonth ref, int increment) {
    Assert.notNull(ref);
    int year = ref.getYear();
    if (increment != 0) {
      year += increment;
    }
    return new JustDate(year, 1, 1);
  }

  public static JustDate startOfYear(int year) {
    return new JustDate(year, 1, 1);
  }

  /**
   * Converts {@code x} to a JustDate format.
   * @param x the Object to convert
   * @return a JustDate type date.
   */
  public static JustDate toDate(Object x) {
    if (x instanceof JustDate) {
      return (JustDate) x;
    }
    if (x instanceof DateTime) {
      return new JustDate((DateTime) x);
    }
    if (x instanceof Date) {
      return new JustDate((Date) x);
    }

    assertDateOrDateTime(x);
    return null;
  }

  public static JustDate toDateOrNull(Integer day) {
    if (day == null) {
      return null;
    } else {
      return new JustDate(day);
    }
  }

  public static JustDate toDateOrNull(String s) {
    if (BeeUtils.isInt(s)) {
      return new JustDate(BeeUtils.toInt(s));
    } else {
      return null;
    }
  }

  /**
   * Converts {@code x} to a DateTime format.
   * @param x the Object to convert
   * @return a DateTime type date.
   */
  public static DateTime toDateTime(Object x) {
    if (x instanceof DateTime) {
      return (DateTime) x;
    }
    if (x instanceof JustDate) {
      return new DateTime((JustDate) x);
    }
    if (x instanceof Date) {
      return new DateTime((Date) x);
    }

    assertDateOrDateTime(x);
    return null;
  }

  public static DateTime toDateTimeOrNull(HasDateValue dt) {
    if (dt == null) {
      return null;
    } else {
      return dt.getDateTime();
    }
  }

  public static DateTime toDateTimeOrNull(Long time) {
    if (time == null) {
      return null;
    } else {
      return new DateTime(time);
    }
  }

  public static DateTime toDateTimeOrNull(String s) {
    if (BeeUtils.isLong(s)) {
      return new DateTime(BeeUtils.toLong(s));
    } else {
      return null;
    }
  }

  public static JustDate today() {
    return new JustDate();
  }

  public static JustDate today(int increment) {
    JustDate date = new JustDate();
    if (increment != 0) {
      addDay(date, increment);
    }
    return date;
  }

  /**
   * Converts {@code x} to a Date format.
   * @param x the Object to convert
   * @return a Date type date.
   */
  public static Date toJava(Object x) {
    if (x instanceof Date) {
      return (Date) x;
    }
    if (x instanceof HasDateValue) {
      return ((HasDateValue) x).getJava();
    }

    assertDateOrDateTime(x);
    return null;
  }

  /**
   * Converts milliseconds {@code millis} to seconds. E.g 6010 is converted to 6.010.
   * @param millis value to convert
   * @return seconds.
   */
  public static String toSeconds(long millis) {
    return Long.toString(millis / MILLIS_PER_SECOND) + BeeConst.STRING_POINT
        + BeeUtils.toLeadingZeroes((int) (millis % MILLIS_PER_SECOND), 3);
  }

  public static String toTimeString(long millis) {
    return new DateTime(millis).toTimeString();
  }

  public static int weekOfYear(HasDateValue ref) {
    return weekOfYear(ref, MINIMAL_DAYS_IN_FIRST_WEEK);
  }

  public static int weekOfYear(HasDateValue ref, int minimalDaysInFirstWeek) {
    Assert.notNull(ref);

    JustDate start = startOfWeekYear(ref.getYear() + 1, minimalDaysInFirstWeek);
    if (isLess(ref, start)) {
      start = startOfWeekYear(ref.getYear(), minimalDaysInFirstWeek);
    }
    if (isLess(ref, start)) {
      start = startOfWeekYear(ref.getYear() - 1, minimalDaysInFirstWeek);
    }

    return dayDiff(start, ref) / DAYS_PER_WEEK + 1;
  }

  public static int year() {
    return today().getYear();
  }

  /**
   * @param year the number to transform
   * @return a textual representation of {@code year}.
   */
  public static String yearToString(int year) {
    return Integer.toString(year);
  }

  private static void assertDateOrDateTime(Object x) {
    Assert.isTrue(isDateOrDateTime(x), "Argument must be Date or DateTime");
  }

  private static String fieldName(int field) {
    if (ArrayUtils.isIndex(FIELD_NAME, field)) {
      return FIELD_NAME[field];
    } else {
      return "Field " + field;
    }
  }

  private static long getDelta(DateTime date, int field, int amount) {
    long delta = amount;

    switch (field) {
      case FIELD_YEAR:
      case FIELD_MONTH:
        int y1 = date.getYear();
        int m1 = date.getMonth();
        int d1 = date.getDom();
        int y2 = y1;
        int m2 = m1;

        if (field == FIELD_YEAR) {
          y2 += amount;
        } else {
          m2 += amount;
          if (m2 < 1 || m2 > 12) {
            int z = y1 * 12 + m1 - 1 + amount;
            y2 = z / 12;
            m2 = z % 12 + 1;
          }
        }

        int d2 = Math.min(d1, Grego.monthLength(y2, m2));
        delta = new DateTime(y2, m2, d2).getTime() - new DateTime(y1, m1, d1).getTime();
        break;

      case FIELD_WEEK_OF_YEAR:
      case FIELD_WEEK_OF_MONTH:
      case FIELD_DAY_OF_WEEK_IN_MONTH:
        delta *= MILLIS_PER_WEEK;
        break;

      case FIELD_AM_PM:
        delta *= 12 * MILLIS_PER_HOUR;
        break;

      case FIELD_DAY_OF_MONTH:
      case FIELD_DAY_OF_YEAR:
      case FIELD_DAY_OF_WEEK:
      case FIELD_DOW_LOCAL:
      case FIELD_JULIAN_DAY:
        delta *= MILLIS_PER_DAY;
        break;

      case FIELD_HOUR_OF_DAY:
      case FIELD_HOUR:
        delta *= MILLIS_PER_HOUR;
        break;

      case FIELD_MINUTE:
        delta *= MILLIS_PER_MINUTE;
        break;

      case FIELD_SECOND:
        delta *= MILLIS_PER_SECOND;
        break;

      case FIELD_MILLISECOND:
      case FIELD_MILLISECONDS_IN_DAY:
        break;

      default:
        Assert.unsupported(BeeUtils.joinWords("delta", fieldName(field), "not supported"));
    }
    return delta;
  }

  private static JustDate parseDate(String input, List<Integer> fields) {
    int count = fields.size();

    switch (count) {
      case 1:
        int v = fields.get(0);

        String digits = BeeUtils.parseDigits(input);
        int len = digits.length();

        String sep = String.valueOf(DATE_FIELD_SEPARATOR);

        if (isYear(v)) {
          return new JustDate(v, 1, 1);

        } else if (maybeDom(v)) {
          return new JustDate(year(), month(), v);

        } else if (len == 2) {
          return parseDate(splitDigits(digits, sep, 1, 1));

        } else if (len == 3) {
          return parseDate(splitDigits(digits, sep, 1, 2));

        } else if (len == 4) {
          return parseDate(splitDigits(digits, sep, 2, 2));

        } else if (len == 5) {
          return parseDate(splitDigits(digits, sep, 1, 2, 2));

        } else if (len == 6) {
          return parseDate(splitDigits(digits, sep, 2, 2, 2));

        } else if (len == 7) {
          if (digits.substring(4, 5) == BeeConst.STRING_ZERO) {
            return parseDate(splitDigits(digits, sep, 4, 2, 1));
          } else {
            return parseDate(splitDigits(digits, sep, 4, 1, 2));
          }

        } else {
          return parseDate(splitDigits(digits, sep, 4, 2, 2));
        }

      case 2:
        int v0 = fields.get(0);
        int v1 = fields.get(1);

        if (isYear(v0)) {
          return new JustDate(v0, v1, 1);

        } else if (isMonth(v0)) {
          return new JustDate(year(), v0, v1);

        } else if (isMonth(v1)) {
          return new JustDate(normalizeYear(v0), v1, 1);

        } else {
          return null;
        }

      default:
        return new JustDate(normalizeYear(getField(fields, 0)),
            getField(fields, 1), getField(fields, 2));
    }
  }

  private static List<Integer> parseDigits(String s, int first, int second, int... rest) {
    List<Integer> slices = Lists.newArrayList(first, second);

    if (rest != null) {
      for (int len : rest) {
        slices.add(len);
      }
    }

    return parseDigits(s, slices);
  }

  private static List<Integer> parseDigits(String input, List<Integer> slices) {
    List<Integer> result = Lists.newArrayList();
    if (BeeUtils.isEmpty(input) || slices.isEmpty()) {
      return result;
    }

    int pos = 0;
    for (int len : slices) {
      int end = Math.min(pos + len, input.length());
      result.add(BeeUtils.toInt(input.substring(pos, end)));

      pos += len;
    }
    return result;
  }

  private static String splitDigits(String input, String separator, int first, int second,
      int... rest) {
    return BeeUtils.join(separator, parseDigits(input, first, second, rest));
  }

  private TimeUtils() {
  }
}
