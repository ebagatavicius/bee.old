package com.butent.bee.shared.data;

import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Contains necessary methods for row classes, for example {@code addCell} or {@code setValue}.
 */

public interface IsRow extends HasCustomProperties {

  void addCell(IsCell cell);

  void addValue(Value value);

  void clearCell(int index);

  boolean deepEquals(IsRow other);

  Boolean getBoolean(int index);

  IsCell getCell(int index);

  List<IsCell> getCells();

  JustDate getDate(int index);

  DateTime getDateTime(int index);

  BigDecimal getDecimal(int index);

  Double getDouble(int index);

  default <E extends Enum<?>> E getEnum(int index, Class<E> clazz) {
    return EnumUtils.getEnumByIndex(clazz, getInteger(index));
  }

  long getId();

  Integer getInteger(int index);

  Long getLong(int index);

  int getNumberOfCells();

  Map<Integer, String> getShadow();

  String getString(int index);

  Value getValue(int index);

  Value getValue(int index, ValueType type);

  long getVersion();

  void insertCell(int index, IsCell cell);

  boolean isEditable();

  default boolean isEmpty(int index) {
    return BeeUtils.isEmpty(getString(index));
  }

  default boolean isEqual(int index, Long value) {
    return Objects.equals(getLong(index), value);
  }

  default boolean isIndex(int index) {
    return index >= 0 && index < getNumberOfCells();
  }

  boolean isNull(int index);

  boolean isRemovable();

  default boolean isTrue(int index) {
    return BeeUtils.isTrue(getBoolean(index));
  }

  void preliminaryUpdate(int col, String value);

  void removeCell(int index);

  void reset();

  boolean sameValues(IsRow other);

  void setCell(int index, IsCell cell);

  void setCells(List<IsCell> cells);

  void setEditable(boolean editable);

  void setId(long id);

  void setRemovable(boolean removable);

  void setValue(int index, BigDecimal value);

  void setValue(int index, Boolean value);

  void setValue(int index, DateTime value);

  void setValue(int index, Double value);

  default void setValue(int index, Enum<?> value) {
    setValue(index, EnumUtils.ordinal(value));
  }

  void setValue(int index, Integer value);

  void setValue(int index, JustDate value);

  void setValue(int index, Long value);

  void setValue(int index, String value);

  void setValue(int index, Value value);

  void setVersion(long version);
}