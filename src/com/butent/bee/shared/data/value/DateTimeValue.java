package com.butent.bee.shared.data.value;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * The {@code DateTimeValue} class represents date and time values. It allows
 * the interpretation of dates as year, month, day, hour, minute,
 * second, millisecond values.
 */
public class DateTimeValue extends Value {

  private static final DateTimeValue NULL_VALUE = new DateTimeValue(null);

  public static DateTimeValue getNullValue() {
    return NULL_VALUE;
  }

  private final DateTime value;

  public DateTimeValue(DateTime date) {
    this.value = date;
  }

  public DateTimeValue(int year, int month, int dayOfMonth, int hours, int minutes, int seconds) {
    this(year, month, dayOfMonth, hours, minutes, seconds, 0);
  }

  public DateTimeValue(int year, int month, int dayOfMonth, int hours,
      int minutes, int seconds, int milliseconds) {
    this.value = new DateTime(year, month, dayOfMonth, hours, minutes, seconds, milliseconds);
  }

  @Override
  public int compareTo(Value o) {
    int diff = precompareTo(o);
    if (diff == BeeConst.COMPARE_UNKNOWN) {
      diff = getDateTime().compareTo(o.getDateTime());
    }
    return diff;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof DateTimeValue && compareTo((DateTimeValue) o) == BeeConst.COMPARE_EQUAL;
  }

  @Override
  public Boolean getBoolean() {
    if (isNull()) {
      return null;
    }
    Assert.unsupported("get boolean from datetime");
    return null;
  }

  @Override
  public JustDate getDate() {
    if (isNull()) {
      return null;
    }
    return new JustDate(value);
  }

  @Override
  public DateTime getDateTime() {
    return value;
  }

  public int getDayOfMonth() {
    Assert.notNull(getDateTime());
    return getDateTime().getDom();
  }

  @Override
  public BigDecimal getDecimal() {
    if (isNull()) {
      return null;
    }
    return BeeUtils.toDecimalOrNull(value.getTime());
  }

  @Override
  public Double getDouble() {
    if (isNull()) {
      return null;
    }
    return (double) value.getTime();
  }

  public int getHourOfDay() {
    Assert.notNull(getDateTime());
    return getDateTime().getHour();
  }

  @Override
  public Integer getInteger() {
    if (isNull()) {
      return null;
    }
    Assert.unsupported("get integer from datetime");
    return null;
  }

  @Override
  public Long getLong() {
    if (isNull()) {
      return null;
    }
    return value.getTime();
  }

  public int getMillisecond() {
    Assert.notNull(getDateTime());
    return getDateTime().getMillis();
  }

  public int getMinute() {
    Assert.notNull(getDateTime());
    return getDateTime().getMinute();
  }

  public int getMonth() {
    Assert.notNull(getDateTime());
    return getDateTime().getMonth();
  }

  @Override
  public DateTime getObjectValue() {
    if (isNull()) {
      return null;
    }
    return value;
  }

  public int getSecond() {
    Assert.notNull(getDateTime());
    return getDateTime().getSecond();
  }

  @Override
  public String getString() {
    if (isNull()) {
      return null;
    }
    return BeeUtils.toString(value.getTime());
  }

  @Override
  public ValueType getType() {
    return ValueType.DATE_TIME;
  }

  public int getYear() {
    Assert.notNull(getDateTime());
    return getDateTime().getYear();
  }

  @Override
  public int hashCode() {
    if (isNull()) {
      return -1;
    }
    return getDateTime().hashCode();
  }

  @Override
  public boolean isEmpty() {
    return isNull();
  }

  @Override
  public boolean isNull() {
    return this == NULL_VALUE || getDateTime() == null;
  }

  @Override
  public String render(Function<HasDateValue, String> dateRenderer,
      Function<HasDateValue, String> dateTimeRenderer) {

    return isNull()
        ? super.render(dateRenderer, dateTimeRenderer)
        : dateTimeRenderer.apply(getDateTime());
  }

  @Override
  public String toString() {
    if (isNull()) {
      return BeeConst.NULL;
    }
    return getDateTime().toString();
  }
}
