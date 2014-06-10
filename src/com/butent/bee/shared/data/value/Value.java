package com.butent.bee.shared.data.value;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.math.BigDecimal;

/**
 * Class {@code Value} is the root of the class hierarchy. {@code BooleanValue},
 * {@code DateTimeValue}, {@code DateValue}, {@code NumberValue}, {@code TextValue},
 * {@code TimeOfDayValue} has {@code Value} as a superclass.
 */
public abstract class Value implements Comparable<Value>, BeeSerializable {

  public static Value getNullValueFromValueType(ValueType type) {
    switch (type) {
      case BOOLEAN:
        return BooleanValue.getNullValue();
      case TEXT:
      case BLOB:
        return TextValue.getNullValue();
      case NUMBER:
        return NumberValue.getNullValue();
      case TIME_OF_DAY:
        return TimeOfDayValue.getNullValue();
      case DATE:
        return DateValue.getNullValue();
      case DATE_TIME:
        return DateTimeValue.getNullValue();
      case INTEGER:
        return IntegerValue.getNullValue();
      case LONG:
        return LongValue.getNullValue();
      case DECIMAL:
        return DecimalValue.getNullValue();
    }
    return null;
  }

  public static Value getValue(Object value) {
    Value val = null;

    if (value != null) {
      if (value instanceof Value) {
        val = (Value) value;

      } else if (value instanceof Boolean) {
        val = BooleanValue.getInstance((Boolean) value);

      } else if (value instanceof Integer) {
        val = new IntegerValue((Integer) value);

      } else if (value instanceof Long) {
        val = new LongValue((Long) value);

      } else if (value instanceof BigDecimal) {
        val = new DecimalValue((BigDecimal) value);

      } else if (value instanceof Number) {
        val = new NumberValue(((Number) value).doubleValue());

      } else if (value instanceof CharSequence) {
        val = new TextValue(value.toString());

      } else if (value instanceof JustDate) {
        val = new DateValue((JustDate) value);

      } else if (value instanceof DateTime) {
        val = new DateTimeValue((DateTime) value);

      } else {
        Assert.unsupported("Unsupported value type: " + NameUtils.getClassName(value.getClass()));
      }
    }
    return val;
  }

  public static Value parseValue(ValueType type, String value, boolean parseDates) {
    Assert.notNull(type, "value type not specified");
    if (value == null) {
      return getNullValueFromValueType(type);
    }

    switch (type) {
      case BOOLEAN:
        return BooleanValue.getInstance(BeeUtils.toBooleanOrNull(value));
      case TEXT:
      case BLOB:
        return new TextValue(value);
      case NUMBER:
        return new NumberValue(BeeUtils.toDoubleOrNull(value));
      case TIME_OF_DAY:
        return new TimeOfDayValue(value);
      case DATE:
        if (parseDates) {
          return new DateValue(TimeUtils.parseDate(value));
        } else {
          return new DateValue(TimeUtils.toDateOrNull(value));
        }
      case DATE_TIME:
        if (parseDates) {
          return new DateTimeValue(TimeUtils.parseDateTime(value));
        } else {
          return new DateTimeValue(TimeUtils.toDateTimeOrNull(value));
        }
      case INTEGER:
        return new IntegerValue(BeeUtils.toIntOrNull(value));
      case LONG:
        return new LongValue(BeeUtils.toLongOrNull(value));
      case DECIMAL:
        return new DecimalValue(BeeUtils.toDecimalOrNull(value));
    }
    return null;
  }

  public static Value restore(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);
    String clazz = arr[0];
    String data = arr[1];

    ValueType type = ValueType.getByTypeCode(clazz);
    Assert.notNull(type, "Unsupported value type: " + clazz);

    return parseValue(type, data, false);
  }

  @Override
  public void deserialize(String s) {
    Assert.unsupported();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    return this.compareTo((Value) o) == 0;
  }

  public abstract Boolean getBoolean();

  public abstract JustDate getDate();

  public abstract DateTime getDateTime();

  public abstract BigDecimal getDecimal();

  public abstract Double getDouble();

  public abstract Integer getInteger();

  public abstract Long getLong();

  public abstract Object getObjectValue();

  public abstract String getString();

  public abstract ValueType getType();

  @Override
  public abstract int hashCode();

  public abstract boolean isEmpty();

  public abstract boolean isNull();

  @Override
  public String serialize() {
    Object value = getObjectValue();

    if (value != null) {
      if (value instanceof Boolean) {
        value = BooleanValue.pack((Boolean) value);

      } else if (value instanceof JustDate) {
        value = ((JustDate) value).getDays();

      } else if (value instanceof DateTime) {
        value = ((DateTime) value).getTime();
      }
    }
    return Codec.beeSerialize(new Object[] {getType().getTypeCode(), value});
  }

  @Override
  public abstract String toString();

  protected int precompareTo(Value o) {
    if (this == o) {
      return BeeConst.COMPARE_EQUAL;
    } else if (o == null) {
      return BeeConst.COMPARE_MORE;
    } else if (isNull()) {
      return o.isNull() ? BeeConst.COMPARE_EQUAL : BeeConst.COMPARE_LESS;
    } else if (o.isNull()) {
      return BeeConst.COMPARE_MORE;
    } else {
      return BeeConst.COMPARE_UNKNOWN;
    }
  }
}
