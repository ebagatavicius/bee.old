package com.butent.bee.shared.data;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.data.value.DateValue;
import com.butent.bee.shared.data.value.DecimalValue;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Is an abstract class for table classes, contains full range of table operating methods like
 * {@code addRow} or {@code sort}.
 */

public abstract class AbstractTable<R extends IsRow, C extends IsColumn> implements IsTable<R, C> {

  protected final class IndexOrdering implements Comparator<Integer> {
    private RowOrdering<R> rowOrdering;

    private IndexOrdering(RowOrdering<R> rowOrdering) {
      this.rowOrdering = rowOrdering;
    }

    @Override
    public int compare(Integer idx1, Integer idx2) {
      return rowOrdering.compare(getRow(idx1), getRow(idx2));
    }
  }

  protected class RowIdOrdering implements Comparator<R> {
    private final boolean ascending;

    RowIdOrdering() {
      this(true);
    }

    RowIdOrdering(boolean ascending) {
      this.ascending = ascending;
    }

    @Override
    public int compare(R row1, R row2) {
      if (row1 == row2) {
        return BeeConst.COMPARE_EQUAL;
      }
      if (row1 == null) {
        return ascending ? BeeConst.COMPARE_LESS : BeeConst.COMPARE_MORE;
      }
      if (row2 == null) {
        return ascending ? BeeConst.COMPARE_MORE : BeeConst.COMPARE_LESS;
      }

      if (ascending) {
        return Longs.compare(row1.getId(), row2.getId());
      } else {
        return Longs.compare(row2.getId(), row1.getId());
      }
    }
  }

  private List<C> columns = new ArrayList<>();

  private CustomProperties properties;
  private List<DataWarning> warnings = new ArrayList<>();

  public AbstractTable() {
  }

  protected AbstractTable(List<C> columns) {
    Assert.notNull(columns);
    for (C column : columns) {
      addColumn(column);
    }
  }

  protected AbstractTable(String... columnLabels) {
    Assert.notNull(columnLabels);
    for (String label : columnLabels) {
      addColumn(ValueType.TEXT, label);
    }
  }

  @Override
  public void addColumn(C column) {
    assertNewColumnId(column.getId());
    columns.add(column);

    if (getNumberOfRows() > 0) {
      Value nullValue = Value.getNullValueFromValueType(column.getType());
      for (R row : getRows()) {
        row.addCell(new TableCell(nullValue));
      }
    }
  }

  @Override
  public void addColumn(ValueType type) {
    addColumn(type, DataUtils.defaultColumnLabel(getNumberOfColumns()));
  }

  @Override
  public void addColumn(ValueType type, String label) {
    addColumn(type, label, DataUtils.defaultColumnId(getNumberOfColumns()));
  }

  @Override
  public void addColumn(ValueType type, String label, String id) {
    addColumn(createColumn(type, label, id));
  }

  @Override
  public void addColumns(Collection<C> columnsToAdd) {
    Assert.notEmpty(columnsToAdd);
    for (C column : columnsToAdd) {
      addColumn(column);
    }
  }

  @Override
  public void addRow(R row) {
    getRows().add(row);
  }

  @Override
  public void addRows(Collection<R> rowsToAdd) {
    Assert.notNull(rowsToAdd);
    getRows().addAll(rowsToAdd);
  }

  @Override
  public void addWarning(DataWarning warning) {
    warnings.add(warning);
  }

  @Override
  public void clearCell(int rowIndex, int colIndex) {
    getRow(rowIndex).clearCell(colIndex);
  }

  @Override
  public void clearTableProperty(String key) {
    Assert.notEmpty(key);
    if (properties != null) {
      properties.remove(key);
    }
  }

  @Override
  public void clearValue(int rowIndex, int colIndex) {
    IsCell cell = getCell(rowIndex, colIndex);
    cell.clearValue();
    cell.clearFormattedValue();
    cell.clearProperties();
  }

  public boolean containsAllColumnIds(Collection<String> colIds) {
    for (String id : colIds) {
      if (!containsColumn(id)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean containsColumn(String columnId) {
    return DataUtils.contains(getColumns(), columnId);
  }

  @Override
  public boolean containsRow(long rowId) {
    return getRowById(rowId) != null;
  }

  @Override
  public Boolean getBoolean(int rowIndex, int colIndex) {
    return getRow(rowIndex).getBoolean(colIndex);
  }

  public Boolean getBoolean(int rowIndex, String columnId) {
    return getBoolean(rowIndex, getColumnIndex(columnId));
  }

  @Override
  public IsCell getCell(int rowIndex, int colIndex) {
    return getRow(rowIndex).getCell(colIndex);
  }

  @Override
  public C getColumn(int colIndex) {
    assertColumnIndex(colIndex);
    return columns.get(colIndex);
  }

  @Override
  public C getColumn(String columnId) {
    return getColumn(getColumnIndex(columnId));
  }

  @Override
  public String getColumnId(int colIndex) {
    return getColumn(colIndex).getId();
  }

  @Override
  public int getColumnIndex(String columnId) {
    return DataUtils.getColumnIndex(columnId, getColumns(), true);
  }

  @Override
  public String getColumnLabel(int colIndex) {
    return getColumn(colIndex).getLabel();
  }

  @Override
  public String getColumnLabel(String columnId) {
    return getColumnLabel(getColumnIndex(columnId));
  }

  @Override
  public String getColumnPattern(int colIndex) {
    return getColumn(colIndex).getPattern();
  }

  @Override
  public CustomProperties getColumnProperties(int colIndex) {
    return getColumn(colIndex).getProperties();
  }

  @Override
  public String getColumnProperty(int colIndex, String name) {
    return getColumn(colIndex).getProperty(name);
  }

  @Override
  public List<C> getColumns() {
    return columns;
  }

  @Override
  public ValueType getColumnType(int colIndex) {
    return getColumn(colIndex).getType();
  }

  @Override
  public JustDate getDate(int rowIndex, int colIndex) {
    return getRow(rowIndex).getDate(colIndex);
  }

  public JustDate getDate(int rowIndex, String columnId) {
    return getDate(rowIndex, getColumnIndex(columnId));
  }

  @Override
  public DateTime getDateTime(int rowIndex, int colIndex) {
    return getRow(rowIndex).getDateTime(colIndex);
  }

  public DateTime getDateTime(int rowIndex, String columnId) {
    return getDateTime(rowIndex, getColumnIndex(columnId));
  }

  @Override
  public BigDecimal getDecimal(int rowIndex, int colIndex) {
    return getRow(rowIndex).getDecimal(colIndex);
  }

  @Override
  public Set<Long> getDistinctLongs(int colIndex) {
    assertColumnIndex(colIndex);
    Set<Long> result = new HashSet<>();
    for (R row : getRows()) {
      result.add(row.getLong(colIndex));
    }
    return result;
  }

  @Override
  public Set<String> getDistinctStrings(int colIndex) {
    assertColumnIndex(colIndex);
    Set<String> result = new HashSet<>();
    for (R row : getRows()) {
      result.add(row.getString(colIndex));
    }
    return result;
  }

  @Override
  public Double getDouble(int rowIndex, int colIndex) {
    return getRow(rowIndex).getDouble(colIndex);
  }

  public Double getDouble(int rowIndex, String columnId) {
    return getDouble(rowIndex, getColumnIndex(columnId));
  }

  @Override
  public R findRow(RowFilter filter) {
    Assert.notNull(filter);

    for (R row : this) {
      if (filter.isMatch(getColumns(), row)) {
        return row;
      }
    }
    return null;
  }

  @Override
  public R findRow(int colIndex, String value) {
    assertColumnIndex(colIndex);
    for (R row : this) {
      if (Objects.equals(row.getString(colIndex), value)) {
        return row;
      }
    }
    return null;
  }

  @Override
  public R findRow(int colIndex, Long value) {
    assertColumnIndex(colIndex);
    for (R row : this) {
      if (Objects.equals(row.getLong(colIndex), value)) {
        return row;
      }
    }
    return null;
  }

  @Override
  public String getFormattedValue(int rowIndex, int colIndex) {
    return getCell(rowIndex, colIndex).getFormattedValue();
  }

  @Override
  public Integer getInteger(int rowIndex, int colIndex) {
    return getRow(rowIndex).getInteger(colIndex);
  }

  public Integer getInteger(int rowIndex, String columnId) {
    return getInteger(rowIndex, getColumnIndex(columnId));
  }

  @Override
  public Long getLong(int rowIndex, int colIndex) {
    return getRow(rowIndex).getLong(colIndex);
  }

  public Long getLong(int rowIndex, String columnId) {
    return getLong(rowIndex, getColumnIndex(columnId));
  }

  @Override
  public int getNumberOfColumns() {
    return columns.size();
  }

  @Override
  public CustomProperties getProperties(int rowIndex, int colIndex) {
    return getCell(rowIndex, colIndex).getProperties();
  }

  @Override
  public String getProperty(int rowIndex, int colIndex, String name) {
    return getCell(rowIndex, colIndex).getProperty(name);
  }

  @Override
  public R getRowById(long rowId) {
    for (R row : this) {
      if (row.getId() == rowId) {
        return row;
      }
    }
    return null;
  }

  @Override
  public List<Long> getRowIds() {
    List<Long> result = new ArrayList<>();
    for (R row : this) {
      result.add(row.getId());
    }
    return result;
  }

  @Override
  public int getRowIndex(long rowId) {
    for (int i = 0; i < getNumberOfRows(); i++) {
      if (getRow(i).getId() == rowId) {
        return i;
      }
    }
    return BeeConst.UNDEF;
  }

  @Override
  public CustomProperties getRowProperties(int rowIndex) {
    return getRow(rowIndex).getProperties();
  }

  @Override
  public String getRowProperty(int rowIndex, String name) {
    return getRow(rowIndex).getProperty(name);
  }

  @Override
  public int[] getSortedRows(List<Pair<Integer, Boolean>> sortInfo, Comparator<String> collator) {
    Assert.notNull(sortInfo);
    Assert.isTrue(sortInfo.size() >= 1);
    int rowCount = getNumberOfRows();
    if (rowCount <= 0) {
      return new int[0];
    }
    if (rowCount == 1) {
      return new int[] {0};
    }

    List<Integer> rowIndexes = new ArrayList<>(rowCount);
    for (int i = 0; i < rowCount; i++) {
      rowIndexes.add(i);
    }

    Collections.sort(rowIndexes, new IndexOrdering(new RowOrdering<R>(getColumns(),
        sortInfo, collator)));
    return Ints.toArray(rowIndexes);
  }

  @Override
  public String getString(int rowIndex, int colIndex) {
    return getRow(rowIndex).getString(colIndex);
  }

  public String getString(int rowIndex, String columnId) {
    return getString(rowIndex, getColumnIndex(columnId));
  }

  @Override
  public CustomProperties getTableProperties() {
    return properties;
  }

  @Override
  public String getTableProperty(String key) {
    Assert.notEmpty(key);
    if (properties == null) {
      return null;
    }
    return properties.get(key);
  }

  @Override
  public Value getValue(int rowIndex, int colIndex) {
    return getCell(rowIndex, colIndex).getValue();
  }

  @Override
  public List<DataWarning> getWarnings() {
    return warnings;
  }

  @Override
  public void insertColumn(int colIndex, C column) {
    assertColumnIndex(colIndex);
    assertNewColumnId(column.getId());

    columns.add(colIndex, column);
    if (getNumberOfRows() > 0) {
      Value nullValue = Value.getNullValueFromValueType(column.getType());
      for (R row : getRows()) {
        row.insertCell(colIndex, new TableCell(nullValue));
      }
    }
  }

  @Override
  public void insertColumn(int colIndex, ValueType type) {
    insertColumn(colIndex, type, DataUtils.defaultColumnLabel(colIndex));
  }

  @Override
  public void insertColumn(int colIndex, ValueType type, String label) {
    insertColumn(colIndex, type, label, DataUtils.defaultColumnId(colIndex));
  }

  @Override
  public void insertColumn(int colIndex, ValueType type, String label, String id) {
    insertColumn(colIndex, createColumn(type, label, id));
  }

  @Override
  public void insertRows(int rowIndex, Collection<R> rowsToAdd) {
    assertRowIndex(rowIndex);
    int idx = rowIndex;
    for (R row : rowsToAdd) {
      insertRow(idx++, row);
    }
  }

  @Override
  public void insertRows(int rowIndex, int rowCount) {
    assertRowIndex(rowIndex);
    Assert.isPositive(rowCount);

    for (int i = 0; i < rowCount; i++) {
      insertRow(rowIndex + i, fillRow(createRow()));
    }
  }

  @Override
  public void removeColumn(int colIndex) {
    assertColumnIndex(colIndex);
    columns.remove(colIndex);

    for (R row : getRows()) {
      row.removeCell(colIndex);
    }
  }

  @Override
  public void removeColumns(int colIndex, int colCount) {
    assertColumnIndex(colIndex);
    Assert.betweenInclusive(colCount, 1, getNumberOfColumns() - colIndex);

    for (int i = 0; i < colCount; i++) {
      removeColumn(colIndex);
    }
  }

  @Override
  public boolean removeRowById(long rowId) {
    int rowIndex = getRowIndex(rowId);
    if (BeeConst.isUndef(rowIndex)) {
      return false;
    } else {
      removeRow(rowIndex);
      return true;
    }
  }

  @Override
  public void removeRows(int rowIndex, int rowCount) {
    assertRowIndex(rowIndex);
    Assert.betweenInclusive(rowCount, 1, getNumberOfRows() - rowIndex);

    for (int i = 0; i < rowCount; i++) {
      removeRow(rowIndex);
    }
  }

  @Override
  public void setCell(int rowIndex, int colIndex, IsCell cell) {
    getRow(rowIndex).setCell(colIndex, cell);
  }

  @Override
  public void setCell(int rowIndex, int colIndex, Value value) {
    setCell(rowIndex, colIndex, new TableCell(value));
  }

  @Override
  public void setCell(int rowIndex, int colIndex, Value value, String formattedValue) {
    setCell(rowIndex, colIndex, new TableCell(value, formattedValue));
  }

  @Override
  public void setCell(int rowIndex, int colIndex, Value value, String formattedValue,
      CustomProperties prp) {
    setCell(rowIndex, colIndex, new TableCell(value, formattedValue, prp));
  }

  @Override
  public void setColumnLabel(int colIndex, String label) {
    getColumn(colIndex).setLabel(label);
  }

  @Override
  public void setColumnProperties(int colIndex, CustomProperties prp) {
    getColumn(colIndex).setProperties(prp);
  }

  @Override
  public void setColumnProperty(int colIndex, String name, String value) {
    getColumn(colIndex).setProperty(name, value);
  }

  @Override
  public void setColumns(List<C> columns) {
    this.columns = columns;
  }

  @Override
  public void setFormattedValue(int rowIndex, int colIndex, String formattedValue) {
    getCell(rowIndex, colIndex).setFormattedValue(formattedValue);
  }

  @Override
  public void setProperties(int rowIndex, int colIndex, CustomProperties prp) {
    getCell(rowIndex, colIndex).setProperties(prp);
  }

  @Override
  public void setProperty(int rowIndex, int colIndex, String name, String value) {
    getCell(rowIndex, colIndex).setProperty(name, value);
  }

  @Override
  public void setRowProperties(int rowIndex, CustomProperties prp) {
    getRow(rowIndex).setProperties(prp);
  }

  @Override
  public void setRowProperty(int rowIndex, String name, String value) {
    getRow(rowIndex).setProperty(name, value);
  }

  @Override
  public void setRows(Collection<R> rows) {
    clearRows();
    addRows(rows);
  }

  @Override
  public void setTableProperties(CustomProperties prp) {
    this.properties = prp;
  }

  @Override
  public void setTableProperty(String propertyKey, String propertyValue) {
    Assert.notEmpty(propertyKey);

    if (BeeUtils.isEmpty(propertyValue)) {
      clearTableProperty(propertyKey);
    } else {
      if (properties == null) {
        properties = CustomProperties.create();
      }
      properties.put(propertyKey, propertyValue);
    }
  }

  @Override
  public void setValue(int rowIndex, int colIndex, BigDecimal value) {
    setValue(rowIndex, colIndex, new DecimalValue(value));
  }

  @Override
  public void setValue(int rowIndex, int colIndex, Boolean value) {
    setValue(rowIndex, colIndex, BooleanValue.of(value));
  }

  @Override
  public void setValue(int rowIndex, int colIndex, DateTime value) {
    setValue(rowIndex, colIndex, new DateTimeValue(value));
  }

  @Override
  public void setValue(int rowIndex, int colIndex, Double value) {
    setValue(rowIndex, colIndex, new NumberValue(value));
  }

  @Override
  public void setValue(int rowIndex, int colIndex, Integer value) {
    setValue(rowIndex, colIndex, new IntegerValue(value));
  }

  @Override
  public void setValue(int rowIndex, int colIndex, JustDate value) {
    setValue(rowIndex, colIndex, new DateValue(value));
  }

  @Override
  public void setValue(int rowIndex, int colIndex, Long value) {
    setValue(rowIndex, colIndex, new LongValue(value));
  }

  @Override
  public void setValue(int rowIndex, int colIndex, String value) {
    setValue(rowIndex, colIndex, new TextValue(value));
  }

  @Override
  public void setValue(int rowIndex, int colIndex, Value value) {
    getRow(rowIndex).setValue(colIndex, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (int rowIndex = 0; rowIndex < getNumberOfRows(); rowIndex++) {
      R row = getRow(rowIndex);
      for (int cellIndex = 0; cellIndex < row.getCells().size(); cellIndex++) {
        IsCell tableCell = row.getCells().get(cellIndex);
        sb.append(tableCell.toString());
        if (cellIndex < row.getCells().size() - 1) {
          sb.append(",");
        }
      }
      if (rowIndex < getNumberOfRows() - 1) {
        sb.append("\n");
      }
    }

    return sb.toString();
  }

  @Override
  public boolean updateRow(R row) {
    if (row == null) {
      return false;
    }

    int rowIndex = getRowIndex(row.getId());
    if (BeeConst.isUndef(rowIndex)) {
      return false;
    } else {
      getRows().set(rowIndex, row);
      return true;
    }
  }

  protected void assertColumnIndex(int colIndex) {
    Assert.isIndex(columns, colIndex);
  }

  protected void assertRowIndex(int rowIndex) {
    Assert.isIndex(rowIndex, getNumberOfRows());
  }

  protected void copyColumns(IsTable<R, C> target) {
    target.setColumns(getColumns());
  }

  protected void copyProperties(IsTable<R, C> target) {
    if (getTableProperties() != null) {
      target.setTableProperties(getTableProperties().copy());
    }
  }

  protected void copyTableDescription(IsTable<R, C> target) {
    copyColumns(target);
    copyProperties(target);
    copyWarnings(target);
  }

  protected abstract void insertRow(int rowIndex, R row);

  private void assertNewColumnId(String columnId) {
    Assert.notEmpty(columnId);
    Assert.isFalse(containsColumn(columnId));
  }

  private void copyWarnings(IsTable<R, C> target) {
    for (DataWarning warning : getWarnings()) {
      target.getWarnings().add(warning);
    }
  }

  private IsCell createCell(int colIndex) {
    return new TableCell(Value.getNullValueFromValueType(columns.get(colIndex).getType()));
  }

  private R createRow() {
    return createRow(getNumberOfRows() + 1);
  }

  private R fillRow(R row) {
    for (int i = row.getNumberOfCells(); i < columns.size(); i++) {
      row.addCell(createCell(i));
    }
    return row;
  }
}
