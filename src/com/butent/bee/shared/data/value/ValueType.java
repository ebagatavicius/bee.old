package com.butent.bee.shared.data.value;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.time.TimeUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code ValueType} enumeration represents the types of Value. Creates a value of
 * {@code Object} type using {@link #createValue(Object)}.
 */
public enum ValueType {
  BOOLEAN("boolean", 'b'), NUMBER("double", 'n'), TEXT("string", 's'), BLOB("blob", 's'),
  DATE("date", 'd'), TIME_OF_DAY("timeofday", 's'), DATE_TIME("datetime", 'd'),
  INTEGER("integer", 'n'), LONG("long", 'n'), DECIMAL("decimal", 'n');

  private static final Map<String, ValueType> typeCodeToValueType;

  private static final char GROUP_BOOL = 'b';
  private static final char GROUP_DT = 'd';
  private static final char GROUP_NUM = 'n';
  private static final char GROUP_STR = 's';

  static {
    typeCodeToValueType = new HashMap<>();
    for (ValueType type : ValueType.values()) {
      typeCodeToValueType.put(type.typeCode.trim().toLowerCase(), type);
    }
  }

  public static ValueType getByTypeCode(String code) {
    if (code == null || code.isEmpty()) {
      return null;
    }
    return typeCodeToValueType.get(code.trim().toLowerCase());
  }

  public static boolean isBoolean(ValueType type) {
    if (type == null) {
      return false;
    }
    return type.getGroupCode() == GROUP_BOOL;
  }

  public static boolean isDateOrDateTime(ValueType type) {
    if (type == null) {
      return false;
    }
    return type.getGroupCode() == GROUP_DT;
  }

  public static boolean isNumeric(ValueType type) {
    if (type == null) {
      return false;
    }
    return type.getGroupCode() == GROUP_NUM;
  }

  public static boolean isString(ValueType type) {
    if (type == null) {
      return false;
    }
    return type.getGroupCode() == GROUP_STR;
  }

  private final String typeCode;
  private final char groupCode;

  ValueType(String typeCode, char groupCode) {
    this.typeCode = typeCode;
    this.groupCode = groupCode;
  }

  public Value createValue(Object value) {
    Value ret = null;

    if (value == null) {
      ret = Value.getNullValueFromValueType(this);
    } else if ((this == TEXT) && (value instanceof String)) {
      ret = new TextValue((String) value);
    } else if ((this == INTEGER) && (value instanceof Number)) {
      ret = new IntegerValue(((Number) value).intValue());
    } else if ((this == LONG) && (value instanceof Number)) {
      ret = new LongValue(((Number) value).longValue());
    } else if ((this == DECIMAL) && (value instanceof BigDecimal)) {
      ret = new DecimalValue((BigDecimal) value);
    } else if ((this == NUMBER) && (value instanceof Number)) {
      ret = new NumberValue(((Number) value).doubleValue());
    } else if ((this == BOOLEAN) && (value instanceof Boolean)) {
      ret = ((Boolean) value).booleanValue() ? BooleanValue.TRUE : BooleanValue.FALSE;
    } else if ((this == DATE) && TimeUtils.isDateOrDateTime(value)) {
      ret = new DateValue(TimeUtils.toDate(value));
    } else if ((this == DATE_TIME) && TimeUtils.isDateOrDateTime(value)) {
      ret = new DateTimeValue(TimeUtils.toDateTime(value));
    } else if ((this == TIME_OF_DAY) && (value instanceof String)) {
      ret = new TimeOfDayValue((String) value);
    }

    Assert.notNull(ret, "Value type mismatch.");
    return ret;
  }

  public char getGroupCode() {
    return groupCode;
  }

  public String getTypeCode() {
    return typeCode;
  }
}
