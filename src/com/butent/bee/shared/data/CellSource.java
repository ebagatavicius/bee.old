package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasPrecision;
import com.butent.bee.shared.HasScale;
import com.butent.bee.shared.data.value.HasValueType;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.math.BigDecimal;
import java.util.function.Function;

public final class CellSource implements HasPrecision, HasScale, HasValueType, BeeSerializable {

  private enum Serial {
    SOURCE_TYPE, USER_ID, NAME, INDEX, VALUE_TYPE, PRECISION, SCALE, IS_TEXT
  }

  private enum SourceType {
    COLUMN, PROPERTY, ID, VERSION
  }

  public static CellSource forColumn(IsColumn column, int index) {
    Assert.notNull(column);
    Assert.nonNegative(index);

    CellSource source = new CellSource(SourceType.COLUMN, column.getId(), index, column.getType());
    source.setPrecision(column.getPrecision());
    source.setScale(column.getScale());

    if (column.isText()) {
      source.setIsText(true);
    }

    return source;
  }

  public static CellSource forProperty(String name, Long userId, ValueType valueType) {
    Assert.notEmpty(name);
    Assert.notNull(valueType);

    CellSource source = new CellSource(SourceType.PROPERTY, name, null, valueType);
    source.setUserId(userId);

    return source;
  }

  public static CellSource forRowId(String name) {
    return new CellSource(SourceType.ID, name, DataUtils.ID_INDEX, DataUtils.ID_TYPE);
  }

  public static CellSource forRowVersion(String name) {
    return new CellSource(SourceType.VERSION, name, DataUtils.VERSION_INDEX,
        DataUtils.VERSION_TYPE);
  }

  public static CellSource restore(String s) {
    Assert.notEmpty(s);

    CellSource cellSource = new CellSource();
    cellSource.deserialize(s);
    return cellSource;
  }

  private SourceType sourceType;

  private String name;
  private Integer index;
  private Long userId;

  private ValueType valueType;

  private int precision = BeeConst.UNDEF;
  private int scale = BeeConst.UNDEF;

  private boolean isText;

  private CellSource() {
  }

  private CellSource(SourceType sourceType, String name, Integer index, ValueType valueType) {
    this.sourceType = sourceType;
    this.name = name;
    this.index = index;
    this.valueType = valueType;
  }

  public void clear(IsRow row) {
    Assert.notNull(row);

    switch (sourceType) {
      case COLUMN:
        row.clearCell(index);
        break;

      case PROPERTY:
        row.setProperty(name, userId, (String) null);
        break;

      case ID:
        row.setId(DataUtils.NEW_ROW_ID);
        break;

      case VERSION:
        row.setVersion(DataUtils.NEW_ROW_VERSION);
        break;
    }
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case INDEX:
          this.index = BeeUtils.toIntOrNull(value);
          break;

        case IS_TEXT:
          setIsText(Codec.unpack(value));
          break;

        case NAME:
          this.name = value;
          break;

        case PRECISION:
          setPrecision(BeeUtils.toInt(value));
          break;

        case SCALE:
          setScale(BeeUtils.toInt(value));
          break;

        case SOURCE_TYPE:
          this.sourceType = Codec.unpack(SourceType.class, value);
          break;

        case USER_ID:
          this.userId = BeeUtils.toLongOrNull(value);
          break;

        case VALUE_TYPE:
          this.valueType = Codec.unpack(ValueType.class, value);
          break;
      }
    }
  }

  public Boolean getBoolean(IsRow row) {
    if (row != null) {
      switch (sourceType) {
        case COLUMN:
          return row.getBoolean(index);
        case PROPERTY:
          return BeeUtils.toBooleanOrNull(row.getProperty(name, userId));
        case ID:
          return row.getId() != DataUtils.NEW_ROW_ID;
        case VERSION:
          return row.getVersion() != DataUtils.NEW_ROW_VERSION;
      }
    }
    return null;
  }

  public JustDate getDate(IsRow row) {
    if (row != null) {
      switch (sourceType) {
        case COLUMN:
          return row.getDate(index);
        case PROPERTY:
          return TimeUtils.toDateOrNull(row.getProperty(name, userId));
        case ID:
          return null;
        case VERSION:
          return new DateTime(row.getVersion()).getDate();
      }
    }
    return null;
  }

  public DateTime getDateTime(IsRow row) {
    if (row != null) {
      switch (sourceType) {
        case COLUMN:
          return row.getDateTime(index);
        case PROPERTY:
          return TimeUtils.toDateTimeOrNull(row.getProperty(name, userId));
        case ID:
          return null;
        case VERSION:
          return new DateTime(row.getVersion());
      }
    }
    return null;
  }

  public BigDecimal getDecimal(IsRow row) {
    if (row != null) {
      switch (sourceType) {
        case COLUMN:
          return row.getDecimal(index);
        case PROPERTY:
          return BeeUtils.toDecimalOrNull(row.getProperty(name, userId));
        case ID:
          return BigDecimal.valueOf(row.getId());
        case VERSION:
          return BigDecimal.valueOf(row.getVersion());
      }
    }
    return null;
  }

  public Double getDouble(IsRow row) {
    if (row != null) {
      switch (sourceType) {
        case COLUMN:
          return row.getDouble(index);
        case PROPERTY:
          return row.getPropertyDouble(name, userId);
        case ID:
          return (double) row.getId();
        case VERSION:
          return (double) row.getVersion();
      }
    }
    return null;
  }

  public Integer getIndex() {
    return index;
  }

  public Integer getInteger(IsRow row) {
    if (row != null) {
      switch (sourceType) {
        case COLUMN:
          return row.getInteger(index);
        case PROPERTY:
          return row.getPropertyInteger(name, userId);
        case ID:
          return BeeUtils.isInt(row.getId()) ? (int) row.getId() : null;
        case VERSION:
          return BeeUtils.isInt(row.getVersion()) ? (int) row.getVersion() : null;
      }
    }
    return null;
  }

  public Long getLong(IsRow row) {
    if (row != null) {
      switch (sourceType) {
        case COLUMN:
          return row.getLong(index);
        case PROPERTY:
          return row.getPropertyLong(name, userId);
        case ID:
          return row.getId();
        case VERSION:
          return row.getVersion();
      }
    }
    return null;
  }

  public String getName() {
    return name;
  }

  @Override
  public int getPrecision() {
    return precision;
  }

  @Override
  public int getScale() {
    return scale;
  }

  public String getString(IsRow row) {
    if (row != null) {
      switch (sourceType) {
        case COLUMN:
          return row.getString(index);
        case PROPERTY:
          return row.getProperty(name, userId);
        case ID:
          return BeeUtils.toString(row.getId());
        case VERSION:
          return BeeUtils.toString(row.getVersion());
      }
    }
    return null;
  }

  public Value getValue(IsRow row) {
    if (row != null) {
      switch (sourceType) {
        case COLUMN:
          return row.getValue(index);
        case PROPERTY:
          String s = row.getProperty(name, userId);
          return BeeUtils.isEmpty(s) ? null : Value.parseValue(valueType, s, false, null);
        case ID:
          return new LongValue(row.getId());
        case VERSION:
          return new LongValue(row.getVersion());
      }
    }
    return null;
  }

  @Override
  public ValueType getValueType() {
    return valueType;
  }

  public Long getUserId() {
    return userId;
  }

  public boolean hasColumn() {
    return SourceType.COLUMN.equals(sourceType);
  }

  public boolean isEmpty(IsRow row) {
    if (row != null) {
      switch (sourceType) {
        case COLUMN:
          return row.isNull(index);
        case PROPERTY:
          return BeeUtils.isEmpty(row.getProperty(name, userId));
        case ID:
          return row.getId() == DataUtils.NEW_ROW_ID;
        case VERSION:
          return row.getVersion() == DataUtils.NEW_ROW_VERSION;
      }
    }
    return true;
  }

  public boolean isText() {
    return isText;
  }

  public String render(IsRow row, Function<HasDateValue, String> dateRenderer,
      Function<DateTime, String> dateTimeRenderer) {

    if (row == null) {
      return null;

    } else if (sourceType == SourceType.ID) {
      return BeeUtils.toString(row.getId());

    } else if (sourceType == SourceType.VERSION) {
      return new DateTime(row.getVersion()).toString();

    } else if (isEmpty(row)) {
      return null;

    } else {

      switch (valueType) {
        case BOOLEAN:
          Boolean bool = getBoolean(row);
          return (bool == null) ? null : BeeUtils.toString(bool);

        case DATE:
          JustDate date = getDate(row);
          return (date == null || dateRenderer == null) ? null : dateRenderer.apply(date);

        case DATE_TIME:
          DateTime dateTime = getDateTime(row);
          return (dateTime == null || dateTimeRenderer == null)
              ? null : dateTimeRenderer.apply(dateTime);

        case DECIMAL:
          BigDecimal decimal = getDecimal(row);
          return (decimal == null) ? null : decimal.toString();

        case INTEGER:
          Integer integer = getInteger(row);
          return (integer == null) ? null : integer.toString();

        case LONG:
          Long longValue = getLong(row);
          return (longValue == null) ? null : longValue.toString();

        case NUMBER:
          Double doubleValue = getDouble(row);
          return (doubleValue == null) ? null : BeeUtils.toString(doubleValue);

        case TEXT:
        case BLOB:
        case TIME_OF_DAY:
          return getString(row);
      }

      return null;
    }
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case INDEX:
          arr[i++] = getIndex();
          break;

        case IS_TEXT:
          arr[i++] = Codec.pack(isText());
          break;

        case NAME:
          arr[i++] = getName();
          break;

        case PRECISION:
          arr[i++] = getPrecision();
          break;

        case SCALE:
          arr[i++] = getScale();
          break;

        case SOURCE_TYPE:
          arr[i++] = Codec.pack(sourceType);
          break;

        case USER_ID:
          arr[i++] = getUserId();
          break;

        case VALUE_TYPE:
          arr[i++] = Codec.pack(getValueType());
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void set(IsRow row, Integer value) {
    Assert.notNull(row);

    switch (sourceType) {
      case COLUMN:
        row.setValue(index, value);
        break;

      case PROPERTY:
        row.setProperty(name, userId, (value == null) ? null : value.toString());
        break;

      case ID:
        row.setId((value == null) ? DataUtils.NEW_ROW_ID : value.longValue());
        break;

      case VERSION:
        row.setVersion((value == null) ? DataUtils.NEW_ROW_VERSION : value.longValue());
        break;
    }
  }

  public void set(IsRow row, String value) {
    Assert.notNull(row);

    switch (sourceType) {
      case COLUMN:
        row.setValue(index, value);
        break;

      case PROPERTY:
        row.setProperty(name, userId, value);
        break;

      case ID:
        row.setId(BeeUtils.toLong(value));
        break;

      case VERSION:
        row.setVersion(BeeUtils.toLong(value));
        break;
    }
  }

  public void setIsText(boolean isText) {
    this.isText = isText;
  }

  @Override
  public void setPrecision(int precision) {
    this.precision = precision;
  }

  @Override
  public void setScale(int scale) {
    this.scale = scale;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  @Override
  public String toString() {
    return "CellSource [sourceType=" + sourceType + ", name=" + name + ", index=" + index
        + ", userId=" + userId + ", valueType=" + valueType + ", precision=" + precision
        + ", scale=" + scale + ", isText=" + isText + "]";
  }
}
