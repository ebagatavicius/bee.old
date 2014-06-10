package com.butent.bee.shared.data.value;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;

import java.math.BigDecimal;

/**
 * Represents a Decimal number value and enables conversions from and to this type.
 */

public class DecimalValue extends Value {

  private static final DecimalValue NULL_VALUE = new DecimalValue(null);

  public static DecimalValue getNullValue() {
    return NULL_VALUE;
  }
  
  public static DecimalValue of(Double value) {
    if (value == null) {
      return NULL_VALUE;
    } else {
      return new DecimalValue(BigDecimal.valueOf(value));
    }
  }

  private final BigDecimal value;

  public DecimalValue(BigDecimal value) {
    this.value = value;
  }

  @Override
  public int compareTo(Value o) {
    int diff = precompareTo(o);
    if (diff == BeeConst.COMPARE_UNKNOWN) {
      diff = getDecimal().compareTo(o.getDecimal());
    }
    return diff;
  }

  @Override
  public Boolean getBoolean() {
    if (isNull()) {
      return null;
    }
    return !BigDecimal.ZERO.equals(value);
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
    return value;
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
    if (isNull()) {
      return null;
    }
    return value.longValue();
  }

  @Override
  public BigDecimal getObjectValue() {
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
    return value.toString();
  }

  @Override
  public ValueType getType() {
    return ValueType.DECIMAL;
  }

  @Override
  public int hashCode() {
    if (isNull()) {
      return 0;
    }
    return getDecimal().hashCode();
  }

  @Override
  public boolean isEmpty() {
    return isNull() || BigDecimal.ZERO.equals(value); 
  }
  
  @Override
  public boolean isNull() {
    return this == NULL_VALUE || getDecimal() == null;
  }

  @Override
  public String toString() {
    if (isNull()) {
      return BeeConst.NULL;
    }
    return value.toString();
  }
}
