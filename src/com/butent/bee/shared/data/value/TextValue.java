package com.butent.bee.shared.data.value;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.math.BigDecimal;

/**
 * The {@code TextValue} class represents character strings.
 */
public class TextValue extends Value {

  private static final TextValue NULL_VALUE = new TextValue(null);

  public static TextValue getNullValue() {
    return NULL_VALUE;
  }

  public static TextValue of(String value) {
    return (value == null) ? NULL_VALUE : new TextValue(value);
  }

  private final String value;

  public TextValue(String value) {
    this.value = value;
  }

  @Override
  public int compareTo(Value o) {
    int diff = precompareTo(o);
    if (diff == BeeConst.COMPARE_UNKNOWN) {
      diff = getString().compareTo(o.getString());
    }
    return diff;
  }

  @Override
  public Boolean getBoolean() {
    if (isNull()) {
      return null;
    }
    return BeeUtils.toBooleanOrNull(value);
  }

  @Override
  public JustDate getDate() {
    if (isNull() || !BeeUtils.isInt(value)) {
      return null;
    }
    return new JustDate(BeeUtils.toInt(value));
  }

  @Override
  public DateTime getDateTime() {
    if (isNull() || !BeeUtils.isLong(value)) {
      return null;
    }
    return new DateTime(BeeUtils.toLong(value));
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
    if (isNull() || !BeeUtils.isDouble(value)) {
      return null;
    }
    return BeeUtils.toDouble(value);
  }

  @Override
  public Integer getInteger() {
    Double d = getDouble();
    if (d == null) {
      return null;
    }
    return d.intValue();
  }

  @Override
  public Long getLong() {
    Double d = getDouble();
    if (d == null) {
      return null;
    }
    return d.longValue();
  }

  @Override
  public String getObjectValue() {
    return value;
  }

  @Override
  public String getString() {
    return value;
  }

  @Override
  public ValueType getType() {
    return ValueType.TEXT;
  }

  @Override
  public int hashCode() {
    if (isNull()) {
      return -1;
    }
    return getString().hashCode();
  }

  @Override
  public boolean isEmpty() {
    return BeeUtils.isEmpty(value);
  }

  @Override
  public boolean isNull() {
    return this == NULL_VALUE || getString() == null;
  }

  @Override
  public String toString() {
    if (isNull()) {
      return BeeConst.NULL;
    }
    return getString();
  }
}
