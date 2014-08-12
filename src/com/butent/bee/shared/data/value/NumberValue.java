package com.butent.bee.shared.data.value;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.math.BigDecimal;

/**
 * The {@code NumberValue} class represents number values. The value is set
 * using {@link com.butent.bee.shared.data.value.NumberValue#NumberValue(Double)} constructor.
 */
public class NumberValue extends Value {

  public static final int MAX_SCALE = 7;

  private static final NumberValue NULL_VALUE = new NumberValue(null);

  public static NumberValue getNullValue() {
    return NULL_VALUE;
  }

  private final Double value;

  public NumberValue(Double value) {
    this.value = value;
  }

  @Override
  public int compareTo(Value o) {
    int diff = precompareTo(o);
    if (diff == BeeConst.COMPARE_UNKNOWN) {
      diff = getDouble().compareTo(o.getDouble());
    }
    return diff;
  }

  @Override
  public Boolean getBoolean() {
    if (isNull()) {
      return null;
    }
    return !BeeUtils.isZero(value);
  }

  @Override
  public JustDate getDate() {
    if (isNull()) {
      return null;
    }
    return new JustDate(value.intValue());
  }

  @Override
  public DateTime getDateTime() {
    if (isNull()) {
      return null;
    }
    return new DateTime(value.longValue());
  }

  @Override
  public BigDecimal getDecimal() {
    if (isNull()) {
      return null;
    }
    return BeeUtils.toDecimalOrNull(value);
  }

  @Override
  public Double getDouble() {
    return value;
  }

  @Override
  public Integer getInteger() {
    if (isNull()) {
      return null;
    }
    return value.intValue();
  }

  @Override
  public Long getLong() {
    if (isNull()) {
      return null;
    }
    return value.longValue();
  }

  @Override
  public Number getObjectValue() {
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
    return BeeUtils.toString(value);
  }

  @Override
  public ValueType getType() {
    return ValueType.NUMBER;
  }

  @Override
  public int hashCode() {
    if (isNull()) {
      return 0;
    }
    return getDouble().hashCode();
  }

  @Override
  public boolean isEmpty() {
    return isNull() || BeeUtils.isZero(value);
  }

  @Override
  public boolean isNull() {
    return this == NULL_VALUE || getDouble() == null;
  }

  @Override
  public String toString() {
    if (isNull()) {
      return BeeConst.NULL;
    }
    return BeeUtils.toString(value);
  }
}
