package com.butent.bee.shared.time;

import com.google.common.primitives.Longs;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Date;

/**
 * The class DateTime represents a specific instant in time, with millisecond precision.
 */
public class DateTime extends AbstractDate implements Comparable<DateTime> {

  public static DateTime copyOf(DateTime original) {
    if (original == null) {
      return null;
    } else {
      return new DateTime(original.getTime());
    }
  }

  public static long deserializeTime(String s) {
    return BeeUtils.toLong(s);
  }

  public static DateTime get(HasDateValue dt) {
    if (dt == null) {
      return null;
    } else if (dt instanceof DateTime) {
      return (DateTime) dt;
    } else {
      return dt.getDateTime();
    }
  }

  public static DateTime restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    return new DateTime(deserializeTime(s));
  }

  private long time;
  private int[] fields;
  private int[] utcFields;

  /**
   * Creates new {@code DateTime} object to current time.
   */
  public DateTime() {
    this(System.currentTimeMillis());
  }

  /**
   * Creates a new {@code DateTime} object with {@code date} date.
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
   * @param year the year
   * @param month the month between 1-12
   * @param dom the day of month between 1-31
   */
  public DateTime(int year, int month, int dom) {
    this(year, month, dom, 0, 0, 0, 0);
  }

  /**
   * Creates a new object of {@code DateTime} with date and time parameters.
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
   * @param year the year
   * @param month the month between 1-12
   * @param dom the day of month between 1-31
   * @param hour the hours between 0-23
   * @param minute the minutes between 0-59
   * @param second the seconds between 0-59
   * @param millis milliseconds
   */
  public DateTime(int year, int month, int dom, int hour, int minute, int second, long millis) {
    setLocalDate(year, month, dom, hour, minute, second, millis);
  }

  /**
   * Creates new object of {@code DateTime} with {@code JustTime} parameter.
   * @param date time of {@code JustTime} object
   */
  public DateTime(JustDate date) {
    if (date == null) {
      setTime(0L);
    } else {
      setLocalDate(date.getYear(), date.getMonth(), date.getDom());
    }
  }

  public DateTime(JustDate date, int hour, int minute, int second, long millis) {
    if (date == null) {
      setTime(0L);
    } else {
      setLocalDate(date.getYear(), date.getMonth(), date.getDom(), hour, minute, second, millis);
    }
  }

  /**
   * Creates new object of {@code DateTime} with milliseconds parameter.
   * @param time time of milliseconds since 00:00:00 GMT on January 1, 1970
   */
  public DateTime(long time) {
    this.time = time;
  }

  public void clearTimePart() {
    if (hasTimePart()) {
      setLocalDate(getYear(), getMonth(), getDom());
    }
  }

  /**
   * Compares two {@code DateTime} objects by date and time values.
   * @param other the {@code DateTime} to be compared
   * @return 0 the {@code DateTime} values of object are equals, more than 0 the {@code other}
   *         values are greater, less than 0 the {@code other} values are lower.
   */
  @Override
  public int compareTo(DateTime other) {
    if (other == null) {
      return BeeConst.COMPARE_MORE;
    }
    return Longs.compare(getTime(), other.getTime());
  }

  /**
   * Deserializes {@code String} to {@code DateTime}.
   * @param s the {@code String} to deserialize
   */
  @Override
  public void deserialize(String s) {
    setTime(deserializeTime(s));
  }

  /**
   * Compare two {@code DateTime} objects by date and time for equality.
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
   * @return the number day of month representing this object.
   */
  @Override
  public int getDom() {
    ensureFields();
    return fields[Grego.IDX_DOM];
  }

  /**
   * Returns the number day of week representing this object. The value returned 1 and 7. First day
   * of week is Monday (value 1)
   * @return the number day of week.
   */
  @Override
  public int getDow() {
    ensureFields();
    return fields[Grego.IDX_DOW];
  }

  /**
   * Returns the number day of year representing this object. The value returned between 1 and 366
   * @return the number day of year representing this object.
   */
  @Override
  public int getDoy() {
    ensureFields();
    return fields[Grego.IDX_DOY];
  }

  /**
   * Returns the number of hour represented this object. The value returned between 0 and 23;
   * @return the number of hour represented this object.
   */
  @Override
  public int getHour() {
    ensureFields();
    return fields[Grego.IDX_HOUR];
  }

  @Override
  public Date getJava() {
    return new Date(getTime());
  }

  /**
   * Returns the number of milliseconds the past of second. The value returned between 0 and 999.
   * @return the number milliseconds the past of second.
   */
  @Override
  public int getMillis() {
    ensureFields();
    return fields[Grego.IDX_MILLIS];
  }

  /**
   * Returns the number of minutes the past of hour. The value returned between 0 and 59.
   * @return the number of minutes the past of hour.
   */
  @Override
  public int getMinute() {
    ensureFields();
    return fields[Grego.IDX_MINUTE];
  }

  /**
   * Returns the number of month. The value returned between 1 and 12
   * @return the number of month.
   */
  @Override
  public int getMonth() {
    ensureFields();
    return fields[Grego.IDX_MONTH];
  }

  /**
   * Returns the number of minutes past the hour represented by this date, as interpreted in the
   * local time zone.
   * @return the number of minutes past the hour represented by this date.
   */
  @Override
  public int getSecond() {
    ensureFields();
    return fields[Grego.IDX_SECOND];
  }

  /**
   * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this
   * DateTime object.
   * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this
   *         date.
   */
  @Override
  public long getTime() {
    return time;
  }

  /**
   * Returns the offset, measured in minutes, for the local time zone relative to UTC that is
   * appropriate for the time represented by this DateTime object.
   * @return the time-zone offset, in minutes, for the current time zone.
   */
  @SuppressWarnings("deprecation")
  @Override
  public int getTimezoneOffset() {
    return new Date(Math.max(getTime(), 0L)).getTimezoneOffset();
  }

  public int getTimezoneOffsetInMillis() {
    return getTimezoneOffset() * TimeUtils.MILLIS_PER_MINUTE;
  }

  @Override
  public ValueType getType() {
    return ValueType.DATE_TIME;
  }

  /**
   * Returns the number the day of month by UTC time. The value returned between 1 and 31.
   * @return the number the day of month by UTC time.
   */
  public int getUtcDom() {
    ensureUtcFields();
    return utcFields[Grego.IDX_DOM];
  }

  /**
   * Returns the number the day of week by UTC time. The value returned between 1 and 7. First day
   * of week is Monday (value equals 1).
   * @return the number the day of week by UTC time.
   */
  public int getUtcDow() {
    ensureUtcFields();
    return utcFields[Grego.IDX_DOW];
  }

  /**
   * Returns the number the day of year by UTC time. The value returned between 1 and 366
   * @return Returns the number the day of year by UTC time.
   */
  public int getUtcDoy() {
    ensureUtcFields();
    return utcFields[Grego.IDX_DOY];
  }

  /**
   * Returns the number of hour of this object by UTC time. The value returned between 0 and 23.
   * @return the number of hour of this object by UTC time.
   */
  public int getUtcHour() {
    ensureUtcFields();
    return utcFields[Grego.IDX_HOUR];
  }

  /**
   * Returns the number of milliseconds the past of second by UTC time.
   * @return the number of milliseconds the past of second by UTC time
   */
  public int getUtcMillis() {
    ensureUtcFields();
    return utcFields[Grego.IDX_MILLIS];
  }

  /**
   * Returns the number of minutes by UTC time. The value returned between 0 and 59
   * @return the number of minutes by UTC time
   */
  public int getUtcMinute() {
    ensureUtcFields();
    return utcFields[Grego.IDX_MINUTE];
  }

  /**
   * Returns a number representing the month by UTC time. The value returned between 1 and 12.
   * @return a number representing the month by UTC time
   */
  public int getUtcMonth() {
    ensureUtcFields();
    return utcFields[Grego.IDX_MONTH];
  }

  /**
   * Returns a number of seconds the past minute by UTC time. The value returned between 0 and 59.
   * @return a number of seconds the past minute by UTC time
   */
  public int getUtcSecond() {
    ensureUtcFields();
    return utcFields[Grego.IDX_SECOND];
  }

  /**
   * Returns a value of UTC year.
   * @return the UTC year representing this date
   */
  public int getUtcYear() {
    ensureUtcFields();
    return utcFields[Grego.IDX_YEAR];
  }

  /**
   * Returns a value of year.
   * @return the year representing this date
   */
  @Override
  public int getYear() {
    ensureFields();
    return fields[Grego.IDX_YEAR];
  }

  /**
   * Returns the {@code DateTime} object hash value.
   * @return the hash value of {@code DateTime}
   */
  @Override
  public int hashCode() {
    return Long.valueOf(getTime()).hashCode();
  }

  @Override
  public boolean hasTimePart() {
    return getHour() != 0 || getMinute() != 0 || getSecond() != 0 || getMillis() != 0;
  }

  /**
   * Serializes the {@code DateTime} object to {@code String}.
   */
  @Override
  public String serialize() {
    return Long.toString(time);
  }

  @Override
  public void setDom(int dom) {
    if (getDom() != dom) {
      fields[Grego.IDX_DOM] = dom;
      updateTime();
    }
  }

  public void setHour(int hour) {
    if (getHour() != hour) {
      fields[Grego.IDX_HOUR] = hour;
      updateTime();
    }
  }

  public void setLocalDate(int year, int month, int dom) {
    setLocalDate(year, month, dom, 0, 0, 0, 0);
  }

  public void setLocalDate(int year, int month, int dom,
      int hour, int minute, int second, long millis) {
    long z = computeLocalTime(year, month, dom, hour, minute, second, millis);
    setLocalTime(z);
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

  public void setMillis(int millis) {
    if (getMillis() != millis) {
      fields[Grego.IDX_MILLIS] = millis;
      updateTime();
    }
  }

  public void setMinute(int minute) {
    if (getMinute() != minute) {
      fields[Grego.IDX_MINUTE] = minute;
      updateTime();
    }
  }

  @Override
  public void setMonth(int month) {
    if (getMonth() != month) {
      fields[Grego.IDX_MONTH] = month;
      updateTime();
    }
  }

  public void setSecond(int second) {
    if (getSecond() != second) {
      fields[Grego.IDX_SECOND] = second;
      updateTime();
    }
  }

  /**
   * Setting the time of milliseconds.
   * @param time the time milliseconds since January 1, 1970, 00:00:00 GMT
   */
  public void setTime(long time) {
    this.time = time;
    resetComputedFields();
  }

  @Override
  public void setYear(int year) {
    if (getYear() != year) {
      fields[Grego.IDX_YEAR] = year;
      updateTime();
    }
  }

  @Override
  public boolean supportsTimezoneOffset() {
    return true;
  }

  public String toCompactString() {
    String timeString = toCompactTimeString();
    if (timeString.isEmpty()) {
      return toDateString();
    } else {
      return toDateString() + TimeUtils.DATE_TIME_SEPARATOR + timeString;
    }
  }

  public String toCompactTimeString() {
    if (getMillis() != 0) {
      return toTimeString();
    }
    if (getHour() == 0 && getMinute() == 0 && getSecond() == 0) {
      return BeeConst.STRING_EMPTY;
    }

    StringBuilder sb = new StringBuilder();
    sb.append(TimeUtils.padTwo(getHour()));
    sb.append(TimeUtils.TIME_FIELD_SEPARATOR).append(TimeUtils.padTwo(getMinute()));

    if (getSecond() != 0) {
      sb.append(TimeUtils.TIME_FIELD_SEPARATOR).append(TimeUtils.padTwo(getSecond()));
    }
    return sb.toString();
  }

  /**
   * Converts the {@code DateTime} to {@code String} with date and time.
   * @return String of date and time
   */
  @Override
  public String toString() {
    return toDateString() + TimeUtils.DATE_TIME_SEPARATOR + toTimeString();
  }

  @Override
  public String toTimeStamp() {
    return TimeUtils.yearToString(getUtcYear()) + TimeUtils.monthToString(getUtcMonth())
        + TimeUtils.dayOfMonthToString(getUtcDom()) + TimeUtils.padTwo(getUtcHour())
        + TimeUtils.padTwo(getUtcMinute()) + TimeUtils.padTwo(getUtcSecond())
        + TimeUtils.millisToString(getUtcMillis());
  }

  /**
   * Converts the {@code DateTime} in time to {@code String}.
   * @return String of the time
   */
  public String toTimeString() {
    StringBuilder sb = new StringBuilder(12);
    sb.append(TimeUtils.padTwo(getHour())).append(TimeUtils.TIME_FIELD_SEPARATOR);
    sb.append(TimeUtils.padTwo(getMinute())).append(TimeUtils.TIME_FIELD_SEPARATOR);
    sb.append(TimeUtils.padTwo(getSecond()));
    int z = getMillis();
    if (z != 0) {
      sb.append(TimeUtils.MILLIS_SEPARATOR).append(TimeUtils.millisToString(z));
    }
    return sb.toString();
  }

  public String toUtcDateString() {
    return TimeUtils.dateToString(getUtcYear(), getUtcMonth(), getUtcDom());
  }

  /**
   * Converting {@code DateTime} object to UTC date and time to the {@code String}.
   * @return String of UTC date and time
   */
  public String toUtcString() {
    return toUtcDateString() + TimeUtils.DATE_TIME_SEPARATOR + toUtcTimeString();
  }

  /**
   * Converting {@code DateTime} object to UTC time to the {@code String}.
   * @return String of UTC time
   */
  public String toUtcTimeString() {
    StringBuilder sb = new StringBuilder(12);
    sb.append(TimeUtils.padTwo(getUtcHour())).append(TimeUtils.TIME_FIELD_SEPARATOR);
    sb.append(TimeUtils.padTwo(getUtcMinute())).append(TimeUtils.TIME_FIELD_SEPARATOR);
    sb.append(TimeUtils.padTwo(getUtcSecond()));
    int z = getUtcMillis();
    if (z != 0) {
      sb.append(TimeUtils.MILLIS_SEPARATOR).append(TimeUtils.millisToString(z));
    }
    return sb.toString();
  }

  private void computeFields() {
    fields = Grego.timeToFields(time - getTimezoneOffsetInMillis());
  }

  private static long computeLocalTime(int year, int month, int dom,
      int hour, int minute, int second, long millis) {
    long z = Grego.fieldsToDay(year, month, dom);
    z *= TimeUtils.MILLIS_PER_DAY;

    z += TimeUtils.getMillis(hour, minute, second, millis);

    return z;
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

  private void resetComputedFields() {
    fields = null;
    utcFields = null;
  }

  private void updateTime() {
    setLocalDate(getYear(), getMonth(), getDom(), getHour(), getMinute(), getSecond(), getMillis());
  }
}
