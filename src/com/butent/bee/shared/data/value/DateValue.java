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
 * The {@code DateTimeValue} class represents date values. It allows
 * the interpretation of dates as year, month and day values.
 */
public class DateValue extends Value {

  private static final DateValue NULL_VALUE = new DateValue(null);

  public static DateValue getNullValue() {
    return NULL_VALUE;
  }

  private final JustDate value;

  public DateValue(int year, int month, int dayOfMonth) {
    this(new JustDate(year, month, dayOfMonth));
  }

  public DateValue(JustDate date) {
    this.value = date;
  }

  @Override
  public int compareTo(Value o) {
    int diff = precompareTo(o);
    if (diff == BeeConst.COMPARE_UNKNOWN) {
      diff = getDate().compareTo(o.getDate());
    }
    return diff;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof DateValue && compareTo((DateValue) o) == BeeConst.COMPARE_EQUAL;
  }

  @Override
  public Boolean getBoolean() {
    if (isNull()) {
      return null;
    }
    Assert.unsupported("get boolean from date");
    return null;
  }

  @Override
  public JustDate getDate() {
    return value;
  }

  @Override
  public DateTime getDateTime() {
    if (isNull()) {
      return null;
    }
    return new DateTime(value);
  }

  @Override
  public BigDecimal getDecimal() {
    if (isNull()) {
      return null;
    }
    return BeeUtils.toDecimalOrNull(value.getDays());
  }

  @Override
  public Double getDouble() {
    if (isNull()) {
      return null;
    }
    return (double) value.getDays();
  }

  @Override
  public Integer getInteger() {
    if (isNull()) {
      return null;
    }
    return value.getDays();
  }

  @Override
  public Long getLong() {
    if (isNull()) {
      return null;
    }
    return (long) value.getDays();
  }

  @Override
  public JustDate getObjectValue() {
    if (isNull()) {
      return null;
    }
    return value;
  }

  @Override
  public String getString() {
    if (isNull()) {
      return null;
    }
    return BeeUtils.toString(value.getDays());
  }

  @Override
  public ValueType getType() {
    return ValueType.DATE;
  }

  @Override
  public int hashCode() {
    if (isNull()) {
      return -1;
    }
    return value.hashCode();
  }

  @Override
  public boolean isEmpty() {
    return isNull();
  }

  @Override
  public boolean isNull() {
    return this == NULL_VALUE || getDate() == null;
  }

  @Override
  public String render(Function<HasDateValue, String> dateRenderer,
      Function<HasDateValue, String> dateTimeRenderer) {

    return isNull() ? super.render(dateRenderer, dateTimeRenderer) : dateRenderer.apply(getDate());
  }

  @Override
  public String toString() {
    if (isNull()) {
      return BeeConst.NULL;
    }
    return value.toString();
  }
}
