package com.butent.bee.shared.data;

import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.EnumUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Determines method requirements for table structure implementing classes.
 */

public interface IsTable<R extends IsRow, C extends IsColumn> extends Iterable<R> {

  void addColumn(C column);

  void addColumn(ValueType type);

  void addColumn(ValueType type, String label);

  void addColumn(ValueType type, String label, String id);

  void addColumns(Collection<C> columnsToAdd);

  void addRow(R row);

  void addRows(Collection<R> rowsToAdd);

  void addWarning(DataWarning warning);

  void clearCell(int rowIndex, int colIndex);

  void clearRows();

  void clearTableProperty(String key);

  void clearValue(int rowIndex, int colIndex);

  boolean containsColumn(String columnId);

  boolean containsRow(long rowId);

  IsTable<R, C> copy();

  IsTable<R, C> create();

  C createColumn(ValueType type, String label, String id);

  R createRow(long id);

  R findRow(RowFilter filter);

  R findRow(int colIndex, String value);

  R findRow(int colIndex, Long value);

  Boolean getBoolean(int rowIndex, int colIndex);

  IsCell getCell(int rowIndex, int colIndex);

  C getColumn(int colIndex);

  C getColumn(String columnId);

  String getColumnId(int colIndex);

  int getColumnIndex(String columnId);

  String getColumnLabel(int colIndex);

  String getColumnLabel(String columnId);

  String getColumnPattern(int colIndex);

  List<C> getColumns();

  ValueType getColumnType(int colIndex);

  JustDate getDate(int rowIndex, int colIndex);

  DateTime getDateTime(int rowIndex, int colIndex);

  BigDecimal getDecimal(int rowIndex, int colIndex);

  Set<Long> getDistinctLongs(int colIndex);

  Set<String> getDistinctStrings(int colIndex);

  Double getDouble(int rowIndex, int colIndex);

  default <E extends Enum<?>> E getEnum(int rowIndex, int colIndex, Class<E> clazz) {
    return EnumUtils.getEnumByIndex(clazz, getInteger(rowIndex, colIndex));
  }

  default <E extends Enum<?>> E getEnum(int rowIndex, String columnId, Class<E> clazz) {
    return getEnum(rowIndex, getColumnIndex(columnId), clazz);
  }

  String getFormattedValue(int rowIndex, int colIndex);

  Integer getInteger(int rowIndex, int colIndex);

  Long getLong(int rowIndex, int colIndex);

  int getNumberOfColumns();

  int getNumberOfRows();

  R getRow(int rowIndex);

  R getRowById(long rowId);

  List<Long> getRowIds();

  int getRowIndex(long rowId);

  List<R> getRows();

  int[] getSortedRows(List<Pair<Integer, Boolean>> sortInfo, Comparator<String> collator);

  String getString(int rowIndex, int colIndex);

  CustomProperties getTableProperties();

  String getTableProperty(String key);

  Value getValue(int rowIndex, int colIndex);

  List<DataWarning> getWarnings();

  void insertColumn(int colIndex, C column);

  void insertColumn(int colIndex, ValueType type);

  void insertColumn(int colIndex, ValueType type, String label);

  void insertColumn(int colIndex, ValueType type, String label, String id);

  void insertRows(int rowIndex, Collection<R> rowsToAdd);

  void insertRows(int rowIndex, int rowCount);

  void removeColumn(int colIndex);

  void removeColumns(int colIndex, int colCount);

  void removeRow(int rowIndex);

  boolean removeRowById(long rowId);

  void removeRows(int rowIndex, int rowCount);

  void setCell(int rowIndex, int colIndex, IsCell cell);

  void setCell(int rowIndex, int colIndex, Value value);

  void setCell(int rowIndex, int colIndex, Value value, String formattedValue);

  void setColumnLabel(int colIndex, String label);

  void setColumns(List<C> columns);

  void setFormattedValue(int rowIndex, int colIndex, String formattedValue);

  void setRows(Collection<R> rows);

  void setTableProperties(CustomProperties properties);

  void setTableProperty(String propertyKey, String propertyValue);

  void setValue(int rowIndex, int colIndex, BigDecimal value);

  void setValue(int rowIndex, int colIndex, Boolean value);

  void setValue(int rowIndex, int colIndex, DateTime value);

  void setValue(int rowIndex, int colIndex, Double value);

  void setValue(int rowIndex, int colIndex, Integer value);

  void setValue(int rowIndex, int colIndex, JustDate value);

  void setValue(int rowIndex, int colIndex, Long value);

  void setValue(int rowIndex, int colIndex, String value);

  void setValue(int rowIndex, int colIndex, Value value);

  void sort(List<Pair<Integer, Boolean>> sortInfo, Comparator<String> collator);

  void sortByRowId(boolean ascending);

  boolean updateRow(R row);
}