package com.butent.bee.shared.data.value;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.math.BigDecimal;

/**
 * The {@code BooleanValue} class represents a boolean value. These values
 * are set by using {@link com.butent.bee.shared.data.value.BooleanValue#getInstance(Boolean)} and
 * are comparable.
 */
public final class BooleanValue extends Value {

  public static final String S_TRUE = "t";
  public static final String S_FALSE = "f";

  public static final BooleanValue TRUE = new BooleanValue(true);
  public static final BooleanValue FALSE = new BooleanValue(false);
  public static final BooleanValue NULL = new BooleanValue(null);

  public static BooleanValue of(Boolean value) {
    if (value == null) {
      return NULL;
    }
    return value ? TRUE : FALSE;
  }

  public static BooleanValue getNullValue() {
    return NULL;
  }

  public static String pack(Boolean value) {
    if (value == null) {
      return null;
    }
    return value ? S_TRUE : S_FALSE;
  }

  public static Boolean unpack(String s) {
    if (BeeUtils.startsSame(s, S_TRUE)) {
      return true;
    }
    if (BeeUtils.startsSame(s, S_FALSE)) {
      return false;
    }
    return null;
  }

  private final Boolean value;

  private BooleanValue(Boolean value) {
    this.value = value;
  }

  @Override
  public int compareTo(Value o) {
    int diff = precompareTo(o);
    if (diff == BeeConst.COMPARE_UNKNOWN) {
      diff = getBoolean().compareTo(o.getBoolean());
    }
    return diff;
  }

  @Override
  public Boolean getBoolean() {
    return value;
  }

  @Override
  public JustDate getDate() {
    if (isNull()) {
      return null;
    }
    Assert.unsupported("get date from boolean");
    return null;
  }

  @Override
  public DateTime getDateTime() {
    if (isNull()) {
      return null;
    }
    Assert.unsupported("get datetime from boolean");
    return null;
  }

  @Override
  public BigDecimal getDecimal() {
    if (isNull()) {
      return null;
    }
    return BeeUtils.toDecimalOrNull(BeeUtils.toInt(value));
  }

  @Override
  public Double getDouble() {
    if (isNull()) {
      return null;
    }
    return (double) BeeUtils.toInt(value);
  }

  @Override
  public Integer getInteger() {
    if (isNull()) {
      return null;
    }
    return BeeUtils.toInt(value);
  }

  @Override
  public Long getLong() {
    if (isNull()) {
      return null;
    }
    return (long) BeeUtils.toInt(value);
  }

  @Override
  public Boolean getObjectValue() {
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
    return value ? S_TRUE : S_FALSE;
  }

  @Override
  public ValueType getType() {
    return ValueType.BOOLEAN;
  }

  @Override
  public int hashCode() {
    return isNull() ? -1 : (value ? 1 : 0);
  }

  @Override
  public boolean isEmpty() {
    return !BeeUtils.isTrue(value);
  }

  @Override
  public boolean isNull() {
    return this == NULL || getBoolean() == null;
  }

  @Override
  public String toString() {
    if (isNull()) {
      return BeeConst.NULL;
    }
    return Boolean.toString(value);
  }
}
