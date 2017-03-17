package com.butent.bee.shared.time;

import com.google.common.primitives.Ints;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Date;

/**
 * The class {@code JustDate} represents a specific instant in date, with days precision.
 */
public class JustDate extends AbstractDate implements Comparable<JustDate> {

  public static JustDate copyOf(JustDate original) {
    if (original == null) {
      return null;
    } else {
      return new JustDate(original.getDays());
    }
  }

  public static JustDate get(HasDateValue dt) {
    if (dt == null) {
      return null;
    } else if (dt instanceof JustDate) {
      return (JustDate) dt;
    } else {
      return dt.getDate();
    }
  }

  public static int readDays(long time) {
    return BeeUtils.round((double) time / TimeUtils.MILLIS_PER_DAY);
  }

  private int days;
  private int[] fields;

  /**
   * Creates new {@code JustDate} object instance with current date.
   */
  public JustDate() {
    this(System.currentTimeMillis());
  }

  /**
   * Creates new {@code JustDate} object instance with {@code date} date settings.
   *
   * @param date {@code java.util.Date} object with date settings
   */
  public JustDate(Date date) {
    this(date == null ? 0L : date.getTime());
  }

  /**
   * Creates new {@code JustDate} object instance with {@code dateTime} date settings.
   *
   * @param dateTime {@code DateTime} object with date settings.
   */
  public JustDate(DateTime dateTime) {
    if (dateTime == null) {
      setDays(0);
    } else {
      setDate(dateTime.getYear(), dateTime.getMonth(), dateTime.getDom());
    }
  }

  /**
   * Creates new {@code JustDate} object instance with day value since January 1, 1970.
   *
   * @param days the value of days since January 1, 1970
   */
  public JustDate(int days) {
    this.days = days;
  }

  /**
   * Creates new {@code JustDate} object instance with year, month and day values.
   *
   * @param year year
   * @param month month of year 1-12
   * @param dom day of month 1-31
   */
  public JustDate(int year, int month, int dom) {
    setDate(year, month, dom);
  }

  /**
   * Creates new {@code JustDate} object instance with milliseconds parameter since January 1, 1970.
   * 00:00:00,000 GMT
   *
   * @param time the time of milliseconds since January 1, 1970 00:00:00,000 GMT
   */
  public JustDate(long time) {
    this(new DateTime(time));
  }

  /**
   * Compares two dates object by values of date fields.
   *
   * @param other another date object to compare
   * @return 0 if dates are equals, less than 0 the {@code other} is for later date and otherwise
   */
  @Override
  public int compareTo(JustDate other) {
    if (other == null) {
      return BeeConst.COMPARE_MORE;
    }
    return Ints.compare(getDays(), other.getDays());
  }

  public void decrement() {
    setDays(getDays() - 1);
  }

  /**
   * Deserializes {@code s} to {@code JustDate} object.
   *
   * @param s the String of serialized {@code JustDate} object
   */
  @Override
  public void deserialize(String s) {
    days = Integer.parseInt(s);
    fields = null;
  }

  /**
   * Compare two objects of {@code JustDate} for equality.
   *
   * @param obj Object to compare the {@code JustDate} object
   * @return true the object are equals object of {@code JustDate}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof JustDate) {
      return getDays() == ((JustDate) obj).getDays();
    }
    return false;
  }

  @Override
  public JustDate getDate() {
    return this;
  }

  @Override
  public DateTime getDateTime() {
    return new DateTime(this);
  }

  /**
   * Returns the value of days since January 1, 1970.
   *
   * @return the value of days since January 1, 1970.
   */
  public int getDays() {
    return days;
  }

  /**
   * Returns the value the day of month. The value returned between 1 and 31
   *
   * @return the value the day of month.
   */
  @Override
  public int getDom() {
    ensureFields();
    return fields[Grego.IDX_DOM];
  }

  /**
   * Returns the value the day of week. The value returned between 1 and 7. The first day of week
   * is Monday (value is 1).
   *
   * @return the value the day of week.
   */
  @Override
  public int getDow() {
    ensureFields();
    return fields[Grego.IDX_DOW];
  }

  /**
   * Returns the value the day of year. The value returned between 1 and 366
   *
   * @return the value the day of year.
   */
  @Override
  public int getDoy() {
    ensureFields();
    return fields[Grego.IDX_DOY];
  }

  @Override
  public Date getJava() {
    return new Date(new DateTime(this).getTime());
  }

  /**
   * Returns the value of month. The value returned between 1 and 12
   *
   * @return the value of month
   */
  @Override
  public int getMonth() {
    ensureFields();
    return fields[Grego.IDX_MONTH];
  }

  @Override
  public long getTime() {
    long time = TimeUtils.MILLIS_PER_DAY * days;
    return time + TimeUtils.getTimezoneOffsetInMillis(time);
  }

  @Override
  public ValueType getType() {
    return ValueType.DATE;
  }

  /**
   * Return the value of year.
   *
   * @return the value of year.
   */
  @Override
  public int getYear() {
    ensureFields();
    return fields[Grego.IDX_YEAR];
  }

  @Override
  public boolean hasTimePart() {
    return false;
  }

  /**
   * Returns hash code of {@code JustDate} object.
   */
  @Override
  public int hashCode() {
    return getDays();
  }

  public void increment() {
    setDays(getDays() + 1);
  }

  /**
   * Serializes the {@code JustDate} object to {@code String} format.
   */
  @Override
  public String serialize() {
    return Integer.toString(days);
  }

  public void setDate(int year, int month, int dom) {
    setDays(Grego.fieldsToDay(year, month, dom));
  }

  public void setDate(JustDate date) {
    Assert.notNull(date);
    setDays(date.getDays());
  }

  /**
   * Sets this {@code JustDate} object to represent a point in date that is {@code days} days after
   * January 1, 1970.
   *
   * @param days the number of days.
   */
  public void setDays(int days) {
    this.days = days;
    this.fields = null;
  }

  @Override
  public void setDom(int dom) {
    if (getDom() != dom) {
      fields[Grego.IDX_DOM] = dom;
      updateDays();
    }
  }

  @Override
  public void setMonth(int month) {
    if (getMonth() != month) {
      fields[Grego.IDX_MONTH] = month;
      updateDays();
    }
  }

  @Override
  public void setYear(int year) {
    if (getYear() != year) {
      fields[Grego.IDX_YEAR] = year;
      updateDays();
    }
  }

  @Override
  public boolean supportsTimezoneOffset() {
    return false;
  }

  /**
   * Converts the {@code JustDate} fields of dates to {@code String}.
   *
   * @return string of date
   */
  @Override
  public String toString() {
    return dateToString(getYear(), getMonth(), getDom());
  }

  @Override
  public String toTimeStamp() {
    return TimeUtils.yearToString(getYear()) + TimeUtils.monthToString(getMonth())
        + TimeUtils.dayOfMonthToString(getDom());
  }

  private void computeFields() {
    fields = Grego.dayToFields(days);
  }

  private void ensureFields() {
    if (fields == null) {
      computeFields();
    }
  }

  private void updateDays() {
    setDate(getYear(), getMonth(), getDom());
  }
}
