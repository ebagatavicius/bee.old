package com.butent.bee.shared.data.value;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.math.BigDecimal;

/**
 * Represents a Long number value and enables conversions from and to this type.
 */

public class LongValue extends Value {

  private static final LongValue NULL_VALUE = new LongValue(null);

  public static LongValue getNullValue() {
    return NULL_VALUE;
  }

  private final Long value;

  public LongValue(Long value) {
    this.value = value;
  }

  @Override
  public int compareTo(Value o) {
    int diff = precompareTo(o);
    if (diff == BeeConst.COMPARE_UNKNOWN) {
      diff = getLong().compareTo(o.getLong());
    }
    return diff;
  }

  @Override
  public Boolean getBoolean() {
    if (isNull()) {
      return null;
    }
    return value != 0L;
  }

  @Override
  public JustDate getDate() {
    if (isNull()) {
      return null;
    }
    Assert.unsupported("get date from long");
    return null;
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
    if (isNull()) {
      return null;
    }
    return value.intValue();
  }

  @Override
  public Long getLong() {
    return value;
  }

  @Override
  public Long getObjectValue() {
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
    return ValueType.LONG;
  }

  @Override
  public int hashCode() {
    if (isNull()) {
      return 0;
    }
    return getLong().hashCode();
  }

  @Override
  public boolean isEmpty() {
    return isNull() || value == 0L;
  }

  @Override
  public boolean isNull() {
    return this == NULL_VALUE || getLong() == null;
  }

  @Override
  public String toString() {
    if (isNull()) {
      return BeeConst.NULL;
    }
    return BeeUtils.toString(value);
  }
}
