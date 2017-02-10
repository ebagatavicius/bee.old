package com.butent.bee.shared.i18n.DateTimeFormatInfo;

/**
 * Information required for formatting and parsing localized date/time values.
 */
public interface DateTimeFormatInfo {

  /**
   * Returns array of strings containing abbreviations for Ante Meridiem and
   * Post Meridiem.
   */
  String[] ampms();

  /**
   * Returns a safe default date format.
   */
  default String dateFormat() {
    return dateFormatMedium();
  }

  /**
   * Returns a "full" date format.
   */
  String dateFormatFull();

  /**
   * Returns a "long" date format.
   */
  String dateFormatLong();

  /**
   * Returns a "medium" date format.
   */
  String dateFormatMedium();

  /**
   * Returns a "short" date format.
   */
  String dateFormatShort();

  /**
   * Returns a date/time format from a date format pattern and a time format
   * pattern, using the locale default joining.
   *
   * @param datePattern the data pattern String
   * @param timePattern the time pattern String
   */
  default String dateTime(String datePattern, String timePattern) {
    return datePattern + " " + timePattern;
  }

  default String dateTimeFull() {
    return dateTime(dateFormatFull(), timeFormatFull());
  }

  default String dateTimeLong() {
    return dateTime(dateFormatLong(), timeFormatLong());
  }

  default String dateTimeMedium() {
    return dateTime(dateFormatMedium(), timeFormatMedium());
  }

  default String dateTimeShort() {
    return dateTime(dateFormatShort(), timeFormatShort());
  }

  /**
   * Returns an array of the full era names.
   */
  String[] erasFull();

  /**
   * Returns abbreviations of the era names.
   */
  String[] erasShort();

  /**
   * Returns the day which generally comes first in a weekly calendar view, as
   * an index into the return value of {@link #weekdaysFull()}.
   */
  default int firstDayOfTheWeek() {
    return 1;
  }

  /**
   * Returns localized format equivalent to the "d" skeleton pattern.
   */
  default String formatDay() {
    return "d";
  }

  /**
   * Returns localized format equivalent to the "hm" skeleton pattern.
   */
  default String formatHour12Minute() {
    return "h:mm a";
  }

  /**
   * Returns localized format equivalent to the "hms" skeleton pattern.
   */
  default String formatHour12MinuteSecond() {
    return "h:mm:ss a";
  }

  /**
   * Returns localized format equivalent to the "Hm" skeleton pattern.
   */
  default String formatHour24Minute() {
    return "HH:mm";
  }

  /**
   * Returns localized format equivalent to the "Hms" skeleton pattern.
   */
  default String formatHour24MinuteSecond() {
    return "HH:mm:ss";
  }

  default String formatHour24MinuteSecondMillisecond() {
    return "HH:mm:ss.SSS";
  }

  /**
   * Returns localized format equivalent to the "ms" skeleton pattern.
   */
  default String formatMinuteSecond() {
    return "mm:ss";
  }

  /**
   * Returns localized format equivalent to the "MMM" skeleton pattern.
   */
  default String formatMonthAbbrev() {
    return "LLL";
  }

  /**
   * Returns localized format equivalent to the "MMMd" skeleton pattern.
   */
  String formatMonthAbbrevDay();

  /**
   * Returns localized format equivalent to the "MMMM" skeleton pattern.
   */
  default String formatMonthFull() {
    return "LLLL";
  }

  /**
   * Returns localized format equivalent to the "MMMMd" skeleton pattern.
   */
  String formatMonthFullDay();

  /**
   * Returns localized format equivalent to the "MMMMEEEEd" skeleton pattern.
   */
  String formatMonthFullWeekdayDay();

  /**
   * Returns localized format equivalent to the "Md" skeleton pattern.
   */
  String formatMonthNumDay();

  /**
   * Returns localized format equivalent to the "y" skeleton pattern.
   */
  default String formatYear() {
    return "y";
  }

  /**
   * Returns localized format equivalent to the "yMMM" skeleton pattern.
   */
  String formatYearMonthAbbrev();

  /**
   * Returns localized format equivalent to the "yMMMd" skeleton pattern.
   */
  String formatYearMonthAbbrevDay();

  /**
   * Returns localized format equivalent to the "yMMMM" skeleton pattern.
   */
  String formatYearMonthFull();

  /**
   * Returns localized format equivalent to the "yMMMMd" skeleton pattern.
   */
  String formatYearMonthFullDay();

  /**
   * Returns localized format equivalent to the "yM" skeleton pattern.
   */
  String formatYearMonthNum();

  /**
   * Returns localized format equivalent to the "yMd" skeleton pattern.
   */
  String formatYearMonthNumDay();

  /**
   * Returns localized format equivalent to the "yMMMEEEd" skeleton pattern.
   */
  String formatYearMonthWeekdayDay();

  /**
   * Returns localized format equivalent to the "yQQQQ" skeleton pattern.
   */
  String formatYearQuarterFull();

  /**
   * Returns localized format equivalent to the "yQ" skeleton pattern.
   */
  String formatYearQuarterShort();

  /**
   * Returns an array of full month names.
   */
  String[] monthsFull();

  /**
   * Returns an array of month names for use in a stand-alone context.
   */
  default String[] monthsFullStandalone() {
    return monthsFull();
  }

  /**
   * Returns an array of the shortest abbreviations for months, typically a
   * single character and not guaranteed to be unique.
   */
  String[] monthsNarrow();

  /**
   * Returns an array of the shortest abbreviations for months suitable for use
   * in a stand-alone context, typically a single character and not guaranteed
   * to be unique.
   */
  default String[] monthsNarrowStandalone() {
    return monthsNarrow();
  }

  /**
   * Returns an array of month abbreviations.
   */
  String[] monthsShort();

  /**
   * Returns an array of month abbreviations, suitable for use in a stand-alone
   * context.
   */
  String[] monthsShortStandalone();

  /**
   * Returns an array of full quarter names.
   */
  String[] quartersFull();

  /**
   * Returns an array of abbreviations for quarters.
   */
  String[] quartersShort();

  /**
   * Returns a safe default time format.
   */
  default String timeFormat() {
    return timeFormatMedium();
  }

  /**
   * Returns a "full" time format.
   */
  default String timeFormatFull() {
    return "HH:mm:ss zzzz";
  }

  /**
   * Returns a "long" time format.
   */
  default String timeFormatLong() {
    return "HH:mm:ss z";
  }

  /**
   * Returns a "medium" time format.
   */
  default String timeFormatMedium() {
    return "HH:mm:ss";
  }

  /**
   * Returns a "short" time format.
   */
  default String timeFormatShort() {
    return "HH:mm";
  }

  /**
   * Returns an array of the full names of weekdays.
   */
  String[] weekdaysFull();

  /**
   * Returns an array of the full names of weekdays, suitable for use in a
   * stand-alone context.
   */
  default String[] weekdaysFullStandalone() {
    return weekdaysFull();
  }

  /**
   * Returns an array of the shortest abbreviations for weekdays, typically a
   * single character and not guaranteed to be unique.
   */
  String[] weekdaysNarrow();

  /**
   * Returns an array of the shortest abbreviations for weekdays suitable for
   * use in a stand-alone context, typically a single character and not
   * guaranteed to be unique.
   */
  default String[] weekdaysNarrowStandalone() {
    return weekdaysNarrow();
  }

  /**
   * Returns an array of abbreviations for weekdays.
   */
  String[] weekdaysShort();

  /**
   * Returns an array of abbreviations for weekdays, suitable for use in a
   * stand-alone context.
   */
  String[] weekdaysShortStandalone();

  /**
   * Returns the day which ends the weekend, as an index into the return value
   * of {@link #weekdaysFull()}.
   *
   * <p>Note that this value may be numerically less than
   * {@link #weekendStart()} - for example, {@link #weekendStart()} of 6 and
   * {@link #weekendEnd()} of 0 means Saturday and Sunday are the weekend.
   */
  default int weekendEnd() {
    return 0;
  }

  /**
   * Returns the day which starts the weekend, as an index into the return value
   * of {@link #weekdaysFull()}.
   */
  default int weekendStart() {
    return 6;
  }
}