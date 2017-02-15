package com.butent.bee.shared.data.value;

import com.google.common.collect.ComparisonChain;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * The {@code DateTimeValue} class represents time values. It allows the interpretation of time as
 * hour, minute, second, millisecond values.
 */
public class TimeOfDayValue extends Value {

  private static final TimeOfDayValue NULL_VALUE = new TimeOfDayValue();

  public static TimeOfDayValue getNullValue() {
    return NULL_VALUE;
  }

  private final int hours;
  private final int minutes;
  private final int seconds;
  private final int milliseconds;

  public TimeOfDayValue(DateTime date) {
    this.hours = date.getHour();
    this.minutes = date.getMinute();
    this.seconds = date.getSecond();
    this.milliseconds = date.getMillis();
  }

  public TimeOfDayValue(int hours) {
    this(hours, 0, 0, 0);
  }

  public TimeOfDayValue(int hours, int minutes) {
    this(hours, minutes, 0, 0);
  }

  public TimeOfDayValue(int hours, int minutes, int seconds) {
    this(hours, minutes, seconds, 0);
  }

  public TimeOfDayValue(int hours, int minutes, int seconds, int milliseconds) {
    Assert.betweenExclusive(hours, 0, TimeUtils.HOURS_PER_DAY);
    Assert.betweenExclusive(minutes, 0, TimeUtils.MINUTES_PER_HOUR);
    Assert.betweenExclusive(seconds, 0, TimeUtils.SECONDS_PER_MINUTE);
    Assert.betweenExclusive(milliseconds, 0, TimeUtils.MILLIS_PER_SECOND);

    this.hours = hours;
    this.minutes = minutes;
    this.seconds = seconds;
    this.milliseconds = milliseconds;
  }

  public TimeOfDayValue(String s) {
    if (BeeUtils.isEmpty(s)) {
      this.hours = BeeConst.UNDEF;
      this.minutes = BeeConst.UNDEF;
      this.seconds = BeeConst.UNDEF;
      this.milliseconds = BeeConst.UNDEF;

    } else {
      List<Integer> fields = TimeUtils.splitFields(s);

      this.hours = TimeUtils.getField(fields, 0);
      this.minutes = TimeUtils.getField(fields, 1);
      this.seconds = TimeUtils.getField(fields, 2);
      this.milliseconds = TimeUtils.getField(fields, 3);
    }
  }

  private TimeOfDayValue() {
    this.hours = BeeConst.UNDEF;
    this.minutes = BeeConst.UNDEF;
    this.seconds = BeeConst.UNDEF;
    this.milliseconds = BeeConst.UNDEF;
  }

  @Override
  public int compareTo(Value o) {
    int diff = precompareTo(o);
    if (diff == BeeConst.COMPARE_UNKNOWN) {
      TimeOfDayValue other = (TimeOfDayValue) o;
      diff = ComparisonChain.start().compare(getHours(), other.getHours()).compare(getMinutes(),
          other.getMinutes()).compare(getSeconds(), other.getSeconds()).compare(getMilliseconds(),
          other.getMilliseconds()).result();
    }
    return diff;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof TimeOfDayValue && compareTo((TimeOfDayValue) o) == BeeConst.COMPARE_EQUAL;
  }

  @Override
  public Boolean getBoolean() {
    if (isNull()) {
      return null;
    }
    Assert.unsupported("get boolean from time of day");
    return null;
  }

  @Override
  public JustDate getDate() {
    if (isNull()) {
      return null;
    }
    Assert.unsupported("get date from time of day");
    return null;
  }

  @Override
  public DateTime getDateTime() {
    if (isNull()) {
      return null;
    }
    Assert.unsupported("get datetime from time of day");
    return null;
  }

  @Override
  public BigDecimal getDecimal() {
    if (isNull()) {
      return null;
    }
    return BeeUtils.toDecimalOrNull(getTime());
  }

  @Override
  public Double getDouble() {
    if (isNull()) {
      return null;
    }
    return (double) getTime();
  }

  public int getHours() {
    Assert.isTrue(!isNull());
    return hours;
  }

  @Override
  public Integer getInteger() {
    if (isNull()) {
      return null;
    }
    return BeeUtils.toInt(getTime());
  }

  @Override
  public Long getLong() {
    if (isNull()) {
      return null;
    }
    return getTime();
  }

  public int getMilliseconds() {
    Assert.isTrue(!isNull());
    return milliseconds;
  }

  public int getMinutes() {
    Assert.isTrue(!isNull());
    return minutes;
  }

  @Override
  public String getObjectValue() {
    if (isNull()) {
      return null;
    }
    return toString();
  }

  public int getSeconds() {
    Assert.isTrue(!isNull());
    return seconds;
  }

  @Override
  public String getString() {
    if (isNull()) {
      return null;
    }
    return BeeUtils.toString(getTime());
  }

  @Override
  public ValueType getType() {
    return ValueType.TIME_OF_DAY;
  }

  @Override
  public int hashCode() {
    if (isNull()) {
      return -1;
    }
    return Objects.hash(getHours(), getMinutes(), getSeconds(), getMilliseconds());
  }

  @Override
  public boolean isEmpty() {
    return isNull() || BeeConst.isUndef(getHours());
  }

  @Override
  public boolean isNull() {
    return this == NULL_VALUE;
  }

  @Override
  public String toString() {
    if (isNull()) {
      return BeeConst.NULL;
    } else {
      return TimeUtils.renderTime(0, null, getHours(), getMinutes(), getSeconds(),
          getMilliseconds(), true);
    }
  }

  private long getTime() {
    return TimeUtils.getMillis(getHours(), getMinutes(), getSeconds(), getMilliseconds());
  }
}
