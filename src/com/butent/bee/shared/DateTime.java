package com.butent.bee.shared;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Grego;
import com.butent.bee.shared.utils.TimeUtils;

import java.util.Date;

/**
 * The class DateTime represents a specific instant in time, with millisecond precision.
 */
public class DateTime extends AbstractDate implements BeeSerializable, Comparable<DateTime> {

  /**
   * Separator for fields of date separate to YYYY.MM.DD format.
   */
  public static final char DATE_FIELD_SEPARATOR = '.';
  /**
   * Separator for date and time.
   */
  public static final char DATE_TIME_SEPARATOR = ' ';

  /**
   * Separator for fields of time separate to format hh.mm.ss.
   */
  public static final char TIME_FIELD_SEPARATOR = ':';

  /**
   * Separator for field of milliseconds separate.
   */
  public static final char MILLIS_SEPARATOR = '.';

  /**
   * Parsing {@code String s} to date format. If the {@code s} a number expression is converted to a
   * milliseconds. Otherwise parses of string an year, month, day, hours, minutes and seconds
   * between separators. The time fields are not necessary.
   * 
   * @param s String of number or date and time expression.
   * @return new {@code DateTime} object parses of {@code String}
   */
  public static DateTime parse(String s) {
    Assert.notEmpty(s);
    if (BeeUtils.isDigit(s)) {
      return new DateTime(BeeUtils.toLong(s));
    }

    int[] arr = TimeUtils.parseFields(s);
    Assert.minLength(arr, 7);
    Assert.isTrue(Ints.max(arr) > 0);

    return new DateTime(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5], arr[6]);
  }

  private long time;
  private int[] fields = null;
  private int[] utcFields = null;

  /**
   * Creates new {@code DateTime} object to current time.
   */
  public DateTime() {
    this(System.currentTimeMillis());
  }

  /**
   * Creates a new {@code DateTime} object with {@code date} date.
   * 
   * @param date a date of object {@code Date}
   */
  public DateTime(Date date) {
    this(date == null ? 0L : date.getTime());

    if (date != null) {
      @SuppressWarnings("deprecation")
      int z = getTimezoneOffset() - date.getTimezoneOffset();
      if (z != 0) {
        setTime(getTime() + z * TimeUtils.MILLIS_PER_MINUTE);
      }
    }
  }

  /**
   * Creates a new object of {@code DateTime} with date parameters.
   * 
   * @param year the year
   * @param month the month between 1-12
   * @param dom the day of month between 1-31
   */
  public DateTime(int year, int month, int dom) {
    this(year, month, dom, 0, 0, 0, 0);
  }

  /**
   * Creates a new object of {@code DateTime} with date and time parameters.
   * 
   * @param year the year
   * @param month the month between 1-12
   * @param dom the day of month between 1-31
   * @param hour the hours between 0-23
   * @param minute the minutes between 0-59
   * @param second the seconds between 0-59
   */
  public DateTime(int year, int month, int dom, int hour, int minute, int second) {
    this(year, month, dom, hour, minute, second, 0);
  }

  /**
   * Creates a new object of {@code DateTime} with date and times parameters including milliseconds.
   * 
   * @param year the year
   * @param month the month between 1-12
   * @param dom the day of month between 1-31
   * @param hour the hours between 0-23
   * @param minute the minutes between 0-59
   * @param second the seconds between 0-59
   * @param millis milliseconds
   */
  public DateTime(int year, int month, int dom, int hour, int minute, int second, int millis) {
    long z = Grego.fieldsToDay(year, month, dom);
    z *= TimeUtils.MILLIS_PER_DAY;

    if (hour != 0) {
      z += hour * TimeUtils.MILLIS_PER_HOUR;
    }
    if (minute != 0) {
      z += minute * TimeUtils.MILLIS_PER_MINUTE;
    }
    if (second != 0) {
      z += second * TimeUtils.MILLIS_PER_SECOND;
    }

    setLocalTime(z + millis);
  }

  /**
   * Creates new object of {@code DateTime} with {@code JustTime} parameter.
   * 
   * @param date time of {@code JustTime} object
   */
  public DateTime(JustDate date) {
    if (date == null) {
      setTime(0L);
    } else {
      setLocalDate(date.getYear(), date.getMonth(), date.getDom());
    }
  }

  /**
   * Creates new object of {@code DateTime} with milliseconds parameter.
   * 
   * @param time time of milliseconds since 00:00:00 GMT on January 1, 1970
   */
  public DateTime(long time) {
    this.time = time;
  }

  /**
   * Compares two {@code DateTime} objects by date and time values.
   * 
   * @param other the {@code DateTime} to be compared
   * @return 0 the {@code DateTime} values of object are equals, more than 0 the {@code other}
   *         values are greater, less than 0 the (@code other} values are lower.
   */
  public int compareTo(DateTime other) {
    if (other == null) {
      return BeeConst.COMPARE_MORE;
    }
    return Longs.compare(getTime(), other.getTime());
  }

  /**
   * Deserializes {@code String} to {@code DateTime}.
   * 
   * @param s the {@code String} to deserialize
   */
  public void deserialize(String s) {
    setTime(Long.parseLong(s));
  }

  /**
   * Compare two {@code DateTime} objects by date and time for eqaulity.
   * 
   * @return true if the {@code obj} are the same.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DateTime) {
      return getTime() == ((DateTime) obj).getTime();
    }
    return false;
  }

  @Override
  public JustDate getDate() {
    return new JustDate(this);
  }

  @Override
  public DateTime getDateTime() {
    return this;
  }

  /**
   * Returns the number day of month representing this object. The value returned between 1 and 31.
   * 
   * @return the number day of month representing this object.
   */
  public int getDom() {
    ensureFields();
    return fields[Grego.IDX_DOM];
  }

  /**
   * Returns the number day of week representing this object. The value returned 1 and 7. First day
   * of week Sunday (value 1)
   * 
   * @return the number day of week.
   */
  public int getDow() {
    ensureFields();
    return fields[Grego.IDX_DOW];
  }

  /**
   * Returns the number day of year representing this object. The value returned between 1 and 366
   * 
   * @return the number day of year representing this object.
   */
  public int getDoy() {
    ensureFields();
    return fields[Grego.IDX_DOY];
  }

  /**
   * Returns the number of hour represented this object. The value returned between 0 and 23;
   * 
   * @return the number of hour represented this object.
   */
  public int getHour() {
    ensureFields();
    return fields[Grego.IDX_HOUR];
  }

  @Override
  public Date getJava() {
    return new Date(getTime());
  }

  /**
   * Returns the number of miliseconds the past of second. The value returned between 0 and 999.
   * 
   * @return the number miliseconds the past of second.
   */
  public int getMillis() {
    ensureFields();
    return fields[Grego.IDX_MILLIS];
  }

  /**
   * Returns the number of minutes the past of hour. The value returned between 0 and 59.
   * 
   * @return the number of minutes the past of hour.
   */
  public int getMinute() {
    ensureFields();
    return fields[Grego.IDX_MINUTE];
  }

  /**
   * Returns the number of mounthh the year. The value returned between 1 and 12
   * 
   * @return the number of nounth the year.
   */
  public int getMonth() {
    ensureFields();
    return fields[Grego.IDX_MONTH];
  }

  /**
   * Returns the number of minutes past the hour represented by this date, as interpreted in the
   * local time zone.
   * 
   * @return the number of minutes past the hour represented by this date.
   */
  public int getSecond() {
    ensureFields();
    return fields[Grego.IDX_SECOND];
  }

  /**
   * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this
   * DateTime object.
   * 
   * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this
   *         date.
   */
  public long getTime() {
    return time;
  }

  /**
   * Returns the offset, measured in minutes, for the local time zone relative to UTC that is
   * appropriate for the time represented by this DateTime object.
   * 
   * @return the time-zone offset, in minutes, for the current time zone.
   */
  @SuppressWarnings("deprecation")
  public int getTimezoneOffset() {
    return new Date(getTime()).getTimezoneOffset();
  }

  @Override
  public ValueType getType() {
    return ValueType.DATETIME;
  }

  /**
   * Returns the number the day of mounth by UTC time. The value returned between 1 and 31.
   * 
   * @return the number the day of mounth by UTC time.
   */
  public int getUtcDom() {
    ensureUtcFields();
    return utcFields[Grego.IDX_DOM];
  }

  /**
   * Returns the number the day of week by UTC time. The value returned between 1 and 7. First day
   * of week Sunday (value equals 1).
   * 
   * @return the number the day of week by UTC time.
   */
  public int getUtcDow() {
    ensureUtcFields();
    return utcFields[Grego.IDX_DOW];
  }

  /**
   * Returns the number the day of year by UTC time. The value returned between 1 and 366
   * 
   * @return Returns the number the day of year by UTC time.
   */
  public int getUtcDoy() {
    ensureUtcFields();
    return utcFields[Grego.IDX_DOY];
  }

  /**
   * Returns the number of hour of this object by UTC time. The value returned between 0 and 23.
   * 
   * @return the number of hour of this object by UTC time.
   */
  public int getUtcHour() {
    ensureUtcFields();
    return utcFields[Grego.IDX_HOUR];
  }

  /**
   * Returns the number of milliseconds the past of second by UTC time.
   * 
   * @return the number of milliseconds the past of second by UTC time
   */
  public int getUtcMillis() {
    ensureUtcFields();
    return utcFields[Grego.IDX_MILLIS];
  }

  /**
   * Returns the number of minutes by UTC time. The value returned between 0 and 59
   * 
   * @return the number of minutes by UTC time
   */
  public int getUtcMinute() {
    ensureUtcFields();
    return utcFields[Grego.IDX_MINUTE];
  }

  /**
   * Returns a number representing the mounth by UTC time. The value returned between 1 and 12.
   * 
   * @return a number representing the mounth by UTC time
   */
  public int getUtcMonth() {
    ensureUtcFields();
    return utcFields[Grego.IDX_MONTH];
  }

  /**
   * Returns a number of seconds the past minute by UTC time. The value returned between 0 and 59.
   * 
   * @return a number of seconds the past minute by UTC time
   */
  public int getUtcSecond() {
    ensureUtcFields();
    return utcFields[Grego.IDX_SECOND];
  }

  /**
   * Returns a value of UTC year.
   * 
   * @return the UTC year representing this date
   */
  public int getUtcYear() {
    ensureUtcFields();
    return utcFields[Grego.IDX_YEAR];
  }

  /**
   * Returns a value of year.
   * 
   * @return the year representing this date
   */
  public int getYear() {
    ensureFields();
    return fields[Grego.IDX_YEAR];
  }

  /**
   * Returns the {@code DateTime} object hash value.
   * 
   * @return the hash value of {@code DateTime}
   */
  @Override
  public int hashCode() {
    return Long.valueOf(getTime()).hashCode();
  }

  /**
   * Serializes the {@code DateTime} object to {@code String}.
   */
  public String serialize() {
    return Long.toString(time);
  }

  public void setLocalDate(int year, int month, int dom) {
    setLocalDate(year, month, dom, 0, 0, 0, 0);
  }

  public void setLocalDate(int year, int month, int dom,
      int hour, int minute, int second, int millis) {
    long z = Grego.fieldsToDay(year, month, dom);
    z *= TimeUtils.MILLIS_PER_DAY;

    if (hour != 0) {
      z += hour * TimeUtils.MILLIS_PER_HOUR;
    }
    if (minute != 0) {
      z += minute * TimeUtils.MILLIS_PER_MINUTE;
    }
    if (second != 0) {
      z += second * TimeUtils.MILLIS_PER_SECOND;
    }

    setLocalTime(z + millis);
  }

  public void setLocalTime(long localTime) {
    setTime(localTime);
    int tzo = getTimezoneOffsetInMillis();
    if (tzo == 0) {
      return;
    }

    int diff = new DateTime(localTime + tzo).getTimezoneOffsetInMillis() - tzo;
    setTime(localTime + tzo + diff);
  }

  /**
   * Setting the time of milliseconds.
   * 
   * @param time the time milliseconds since January 1, 1970, 00:00:00 GMT
   */
  public void setTime(long time) {
    this.time = time;
    resetComputedFields();
  }

  /**
   * Converts the {@code DateTime} in date to {@code String}.
   * 
   * @return String of date
   */
  public String toDateString() {
    StringBuilder sb = new StringBuilder(10);
    sb.append(TimeUtils.yearToString(getYear())).append(DATE_FIELD_SEPARATOR);
    sb.append(TimeUtils.padTwo(getMonth())).append(DATE_FIELD_SEPARATOR);
    sb.append(TimeUtils.padTwo(getDom()));
    return sb.toString();
  }

  /**
   * Converts the {@code DateTime} to {@code String} with date and time.
   * 
   * @return String of date and time
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(23);
    sb.append(toDateString()).append(DATE_TIME_SEPARATOR).append(toTimeString());
    return sb.toString();
  }

  /**
   * Converts the {@code DateTime} in time to {@code String}.
   * 
   * @return String of the time
   */
  public String toTimeString() {
    StringBuilder sb = new StringBuilder(12);
    sb.append(TimeUtils.padTwo(getHour())).append(TIME_FIELD_SEPARATOR);
    sb.append(TimeUtils.padTwo(getMinute())).append(TIME_FIELD_SEPARATOR);
    sb.append(TimeUtils.padTwo(getSecond()));
    int z = getMillis();
    if (z != 0) {
      sb.append(MILLIS_SEPARATOR).append(TimeUtils.millisToString(z));
    }
    return sb.toString();
  }

  public String toUtcDateString() {
    StringBuilder sb = new StringBuilder(10);
    sb.append(TimeUtils.yearToString(getUtcYear())).append(DATE_FIELD_SEPARATOR);
    sb.append(TimeUtils.padTwo(getUtcMonth())).append(DATE_FIELD_SEPARATOR);
    sb.append(TimeUtils.padTwo(getUtcDom()));
    return sb.toString();
  }

  /**
   * Converting {@code DateTime} object to UTC date and time to the {@code String}.
   * 
   * @return String of UTC date and time
   */
  public String toUtcString() {
    StringBuilder sb = new StringBuilder(23);
    sb.append(toUtcDateString()).append(DATE_TIME_SEPARATOR).append(toUtcTimeString());
    return sb.toString();
  }

  /**
   * Converting {@code DateTime} object to UTC time to the {@code String}.
   * 
   * @return String of UTC time
   */
  public String toUtcTimeString() {
    StringBuilder sb = new StringBuilder(12);
    sb.append(TimeUtils.padTwo(getUtcHour())).append(TIME_FIELD_SEPARATOR);
    sb.append(TimeUtils.padTwo(getUtcMinute())).append(TIME_FIELD_SEPARATOR);
    sb.append(TimeUtils.padTwo(getUtcSecond()));
    int z = getUtcMillis();
    if (z != 0) {
      sb.append(MILLIS_SEPARATOR).append(TimeUtils.millisToString(z));
    }
    return sb.toString();
  }

  private void computeFields() {
    fields = Grego.timeToFields(time - getTimezoneOffsetInMillis());
  }

  private void computeUtcFields() {
    utcFields = Grego.timeToFields(time);
  }

  private void ensureFields() {
    if (fields == null) {
      computeFields();
    }
  }

  private void ensureUtcFields() {
    if (utcFields == null) {
      computeUtcFields();
    }
  }

  private int getTimezoneOffsetInMillis() {
    return getTimezoneOffset() * TimeUtils.MILLIS_PER_MINUTE;
  }

  private void resetComputedFields() {
    fields = null;
    utcFields = null;
  }
}
