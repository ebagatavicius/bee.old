package com.butent.bee.shared.time;

/**
 * Contains methods for date/time calculations.
 */
public final class Grego {

  public static final int IDX_YEAR = 0;
  public static final int IDX_MONTH = 1;
  public static final int IDX_DOM = 2;
  public static final int IDX_DOW = 3;
  public static final int IDX_DOY = 4;

  public static final int IDX_HOUR = 5;
  public static final int IDX_MINUTE = 6;
  public static final int IDX_SECOND = 7;
  public static final int IDX_MILLIS = 8;

  public static final int FIELD_COUNT = 9;

  private static final int JULIAN_1_CE = 1721426;
  private static final int JULIAN_1970_CE = 2440588;

  private static final int[] MONTH_LENGTH = new int[] {
      31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31,
      31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

  private static final int[] DAYS_BEFORE = new int[] {
      0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334,
      0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335};

  private static final int THURSDAY = 5;

  /**
   * @param day
   *          the day in year
   * @return the day of week
   */
  public static int dayOfWeek(int day) {
    long[] remainder = new long[1];
    floorDivide(day + THURSDAY, 7, remainder);

    int dayOfWeek = (int) remainder[0];
    dayOfWeek = (dayOfWeek == 0) ? 7 : dayOfWeek;

    return dayOfWeek;
  }

  /**
   * Converts the day {@code day} to an integer array representation of the day(
   * year,month,dayOfMonth, dayOfWeek,dayOfYear).
   * 
   * @param day
   *          the day to convert
   * @return an integer array representation of the day.
   */
  public static int[] dayToFields(long day) {
    int[] fields = new int[FIELD_COUNT];

    long d = day + JULIAN_1970_CE - JULIAN_1_CE;

    long[] rem = new long[1];
    long n400 = floorDivide(d, 146097, rem);
    long n100 = floorDivide(rem[0], 36524, rem);
    long n4 = floorDivide(rem[0], 1461, rem);
    long n1 = floorDivide(rem[0], 365, rem);

    int year = (int) (400 * n400 + 100 * n100 + 4 * n4 + n1);
    int dayOfYear = (int) rem[0];
    if (n100 == 4 || n1 == 4) {
      dayOfYear = 365;
    } else {
      year++;
    }

    boolean isLeap = isLeapYear(year);
    int correction = 0;
    int march1 = isLeap ? 60 : 59;
    if (dayOfYear >= march1) {
      correction = isLeap ? 1 : 2;
    }
    int month = (12 * (dayOfYear + correction) + 6) / 367;
    int dayOfMonth = dayOfYear - DAYS_BEFORE[isLeap ? month + 12 : month] + 1;
    int dayOfWeek = (int) ((d + 1) % 7);
    if (dayOfWeek < 1) {
      dayOfWeek += 7;
    }
    dayOfYear++;

    fields[IDX_YEAR] = year;
    fields[IDX_MONTH] = month + 1;
    fields[IDX_DOM] = dayOfMonth;
    fields[IDX_DOW] = dayOfWeek;
    fields[IDX_DOY] = dayOfYear;

    return fields;
  }

  /**
   * Calculates how many days passed since 1970 January 1.
   * 
   * @param year
   *          the year to calculate to
   * @param month
   *          the month to calculate to
   * @param dom
   *          the dom to calculate to
   * @return days passed since 1970 January 1.
   */
  public static int fieldsToDay(int year, int month, int dom) {
    int y = year - 1;
    int julian = 365 * y + floorDivide(y, 4) + (JULIAN_1_CE - 3)
        + floorDivide(y, 400) - floorDivide(y, 100) + 2;
    if (month > 1 && month <= 12) {
      julian += DAYS_BEFORE[month + (isLeapYear(year) ? 11 : -1)];
    }

    if (dom > 0) {
      julian += dom;
    } else if (dom < 0) {
      julian += dom + 1;
    } else {
      julian++;
    }

    return julian - JULIAN_1970_CE;
  }

  /**
   * Checks if {@code year} is a leap year.
   * 
   * @param year
   *          the value to check
   * @return true if {@code year} is a leap year, otherwise false.
   */
  public static boolean isLeapYear(int year) {
    return ((year & 0x3) == 0) && ((year % 100 != 0) || (year % 400 == 0));
  }

  /**
   * Gets the length of the specified month.
   * 
   * @param year
   *          the year to check
   * @param month
   *          the month to check
   * @return the length of the specified year and month.
   */
  public static int monthLength(int year, int month) {
    if (isLeapYear(year)) {
      return MONTH_LENGTH[month + 11];
    } else {
      return MONTH_LENGTH[month - 1];
    }
  }

  /**
   * Converts the time {@code time} to an integer array representation of the time
   * (hour,minute,second,millis).
   * 
   * @param time
   *          the time to convert
   * @return an integer array representation of the time.
   */
  public static int[] timeToFields(long time) {
    long[] remainder = new long[1];

    long day = floorDivide(time, TimeUtils.MILLIS_PER_DAY, remainder);
    int[] fields = dayToFields(day);

    fields[IDX_HOUR] = (int) floorDivide(remainder[0], TimeUtils.MILLIS_PER_HOUR, remainder);
    fields[IDX_MINUTE] = (int) floorDivide(remainder[0], TimeUtils.MILLIS_PER_MINUTE, remainder);
    fields[IDX_SECOND] = (int) floorDivide(remainder[0], TimeUtils.MILLIS_PER_SECOND, remainder);
    fields[IDX_MILLIS] = (int) remainder[0];

    return fields;
  }

  private static int floorDivide(int numerator, int denominator) {
    return (numerator >= 0) ? numerator / denominator : ((numerator + 1) / denominator) - 1;
  }

  private static long floorDivide(long numerator, long denominator, long[] remainder) {
    if (numerator >= 0) {
      remainder[0] = numerator % denominator;
      return numerator / denominator;
    }
    long quotient = ((numerator + 1) / denominator) - 1;
    remainder[0] = numerator - (quotient * denominator);

    return quotient;
  }

  private Grego() {
  }
}
