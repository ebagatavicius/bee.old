package com.butent.bee.shared.data.value;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.math.BigDecimal;

/**
 * Represents a Integer number value and enables conversions from and to this type.
 */

public class IntegerValue extends Value {

  private static final IntegerValue NULL_VALUE = new IntegerValue(null);

  public static IntegerValue getNullValue() {
    return NULL_VALUE;
  }

  public static IntegerValue of(Enum<?> e) {
    return (e == null) ? NULL_VALUE : new IntegerValue(e.ordinal());
  }

  private final Integer value;

  public IntegerValue(Integer value) {
    this.value = value;
  }

  @Override
  public int compareTo(Value o) {
    int diff = precompareTo(o);
    if (diff == BeeConst.COMPARE_UNKNOWN) {
      diff = getInteger().compareTo(o.getInteger());
    }
    return diff;
  }

  @Override
  public Boolean getBoolean() {
    if (isNull()) {
      return null;
    }
    return BeeUtils.toBoolean(value);
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
    if (isNull()) {
      return null;
    }
    Assert.unsupported("get datetime from integer");
    return null;
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
    if (isNull()) {
      return null;
    }
    return value.doubleValue();
  }

  @Override
  public Integer getInteger() {
    return value;
  }

  @Override
  public Long getLong() {
    if (isNull()) {
      return null;
    }
    return value.longValue();
  }

  @Override
  public Integer getObjectValue() {
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
    return ValueType.INTEGER;
  }

  @Override
  public int hashCode() {
    if (isNull()) {
      return 0;
    }
    return getInteger().hashCode();
  }

  @Override
  public boolean isEmpty() {
    return isNull() || value == 0;
  }

  @Override
  public boolean isNull() {
    return this == NULL_VALUE || getInteger() == null;
  }

  @Override
  public String toString() {
    if (isNull()) {
      return BeeConst.NULL;
    }
    return BeeUtils.toString(value);
  }
}
