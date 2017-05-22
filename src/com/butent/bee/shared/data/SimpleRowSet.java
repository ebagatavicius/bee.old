package com.butent.bee.shared.data;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SimpleRowSet implements Iterable<SimpleRow>, BeeSerializable {

  public final class SimpleRow {
    private final int rowIndex;

    private SimpleRow(int rowIndex) {
      this.rowIndex = rowIndex;
    }

    public Boolean getBoolean(int colIndex) {
      return getRowSet().getBoolean(rowIndex, colIndex);
    }

    public Boolean getBoolean(String colName) {
      return getRowSet().getBoolean(rowIndex, colName);
    }

    public String[] getColumnNames() {
      return getRowSet().getColumnNames();
    }

    public JustDate getDate(int colIndex) {
      return getRowSet().getDate(rowIndex, colIndex);
    }

    public JustDate getDate(String colName) {
      return getRowSet().getDate(rowIndex, colName);
    }

    public DateTime getDateTime(int colIndex) {
      return getRowSet().getDateTime(rowIndex, colIndex);
    }

    public DateTime getDateTime(String colName) {
      return getRowSet().getDateTime(rowIndex, colName);
    }

    public BigDecimal getDecimal(int colIndex) {
      return getRowSet().getDecimal(rowIndex, colIndex);
    }

    public BigDecimal getDecimal(String colName) {
      return getRowSet().getDecimal(rowIndex, colName);
    }

    public Double getDouble(int colIndex) {
      return getRowSet().getDouble(rowIndex, colIndex);
    }

    public Double getDouble(String colName) {
      return getRowSet().getDouble(rowIndex, colName);
    }

    public <E extends Enum<?>> E getEnum(int colIndex, Class<E> clazz) {
      return getRowSet().getEnum(rowIndex, colIndex, clazz);
    }

    public <E extends Enum<?>> E getEnum(String colName, Class<E> clazz) {
      return getRowSet().getEnum(rowIndex, colName, clazz);
    }

    public Integer getInt(int colIndex) {
      return getRowSet().getInt(rowIndex, colIndex);
    }

    public Integer getInt(String colName) {
      return getRowSet().getInt(rowIndex, colName);
    }

    public List<String> getList(List<String> colNames) {
      List<String> result = new ArrayList<>();
      for (String colName : colNames) {
        result.add(getValue(colName));
      }
      return result;
    }

    public Long getLong(int colIndex) {
      return getRowSet().getLong(rowIndex, colIndex);
    }

    public Long getLong(String colName) {
      return getRowSet().getLong(rowIndex, colName);
    }

    public SimpleRowSet getRowSet() {
      return SimpleRowSet.this;
    }

    public String getValue(int colIndex) {
      return getRowSet().getValue(rowIndex, colIndex);
    }

    public String getValue(String colName) {
      return getRowSet().getValue(rowIndex, colName);
    }

    public String[] getValues() {
      return getRowSet().getValues(rowIndex);
    }

    public boolean hasColumn(String colName) {
      return getRowSet().hasColumn(colName);
    }

    public boolean isTrue(String colName) {
      return BeeUtils.isTrue(getBoolean(colName));
    }

    public void setValue(int colIndex, String value) {
      getRowSet().setValue(rowIndex, colIndex, value);
    }

    public void setValue(String colName, String value) {
      getRowSet().setValue(rowIndex, colName, value);
    }
  }

  private class RowSetIterator implements Iterator<SimpleRow> {
    private int index = -1;

    @Override
    public boolean hasNext() {
      return index < (getNumberOfRows() - 1);
    }

    @Override
    public SimpleRow next() {
      if (index >= getNumberOfRows()) {
        Assert.untouchable();
      }
      return getRow(++index);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private enum Serial {
    COLUMN_NAMES, COLUMNS, ROWS
  }

  private String[] columnNames = new String[0];
  private Map<String, Integer> columns = new HashMap<>();
  private List<String[]> rows = new ArrayList<>();
  private Map<Integer, Map<String, Integer>> indexes;

  public SimpleRowSet(String[] cols) {
    Assert.isPositive(ArrayUtils.length(cols));

    columnNames = cols;
    columns = new HashMap<>();

    for (int i = 0; i < cols.length; i++) {
      columns.put(cols[i].toLowerCase(), i);
    }
  }

  private SimpleRowSet() {
  }

  public SimpleRow addEmptyRow() {
    String[] values = new String[getNumberOfColumns()];
    addRow(values);
    return new SimpleRow(getNumberOfRows() - 1);
  }

  public void addRow(String[] row) {
    Assert.lengthEquals(row, getNumberOfColumns());
    rows.add(row);
  }

  public void append(SimpleRowSet other) {
    if (other != null && other.getNumberOfRows() > 0) {
      Assert.isTrue(getNumberOfColumns() == other.getNumberOfColumns());

      for (int i = 0; i < other.getNumberOfRows(); i++) {
        rows.add(ArrayUtils.copyOf(other.rows.get(i)));
      }
    }
  }

  @Override
  public void deserialize(String data) {
    Assert.isTrue(getNumberOfColumns() == 0);

    processMembers(Serial.class, data, (serial, value) -> {
      switch (serial) {
        case COLUMN_NAMES:
          columnNames = Codec.beeDeserializeCollection(value);
          break;

        case COLUMNS:
          String[] colData = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(colData)) {
            columns = HashBiMap.create(colData.length / 2);

            for (int j = 0; j < colData.length; j += 2) {
              columns.put(colData[j], BeeUtils.toInt(colData[j + 1]));
            }
          }
          break;

        case ROWS:
          rows = new ArrayList<>();
          String[] rowData = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(rowData)) {
            for (String r : rowData) {
              rows.add(Codec.beeDeserializeCollection(r));
            }
          }
          break;
      }
    });
  }

  public Boolean getBoolean(int rowIndex, int colIndex) {
    return BeeUtils.toBooleanOrNull(getValue(rowIndex, colIndex));
  }

  public Boolean getBoolean(int rowIndex, String colName) {
    return getBoolean(rowIndex, getColumnIndex(colName));
  }

  public Boolean[] getBooleanColumn(int index) {
    Boolean[] col = new Boolean[getNumberOfRows()];

    for (int i = 0; i < getNumberOfRows(); i++) {
      col[i] = getBoolean(i, index);
    }
    return col;
  }

  public Boolean[] getBooleanColumn(String colName) {
    return getBooleanColumn(getColumnIndex(colName));
  }

  public String[] getColumn(int index) {
    String[] col = new String[getNumberOfRows()];

    for (int i = 0; i < getNumberOfRows(); i++) {
      col[i] = getValue(i, index);
    }
    return col;
  }

  public String[] getColumn(String colName) {
    return getColumn(getColumnIndex(colName));
  }

  public int getColumnIndex(String colName) {
    String col = colName == null ? null : colName.toLowerCase();
    Assert.contains(columns, col);
    return columns.get(col);
  }

  public String getColumnName(int colIndex) {
    Assert.isIndex(colIndex, columnNames.length);
    return columnNames[colIndex];
  }

  public String[] getColumnNames() {
    return ArrayUtils.copyOf(columnNames);
  }

  public JustDate getDate(int rowIndex, int colIndex) {
    Long value = getLong(rowIndex, colIndex);
    return (value == null) ? null : new JustDate(JustDate.readDays(value));
  }

  public JustDate getDate(int rowIndex, String colName) {
    return getDate(rowIndex, getColumnIndex(colName));
  }

  public JustDate[] getDateColumn(int index) {
    JustDate[] col = new JustDate[getNumberOfRows()];

    for (int i = 0; i < getNumberOfRows(); i++) {
      col[i] = getDate(i, index);
    }
    return col;
  }

  public JustDate[] getDateColumn(String colName) {
    return getDateColumn(getColumnIndex(colName));
  }

  public DateTime getDateTime(int rowIndex, int colIndex) {
    return TimeUtils.toDateTimeOrNull(getValue(rowIndex, colIndex));
  }

  public DateTime getDateTime(int rowIndex, String colName) {
    return getDateTime(rowIndex, getColumnIndex(colName));
  }

  public DateTime[] getDateTimeColumn(int index) {
    DateTime[] col = new DateTime[getNumberOfRows()];

    for (int i = 0; i < getNumberOfRows(); i++) {
      col[i] = getDateTime(i, index);
    }
    return col;
  }

  public DateTime[] getDateTimeColumn(String colName) {
    return getDateTimeColumn(getColumnIndex(colName));
  }

  public BigDecimal getDecimal(int rowIndex, int colIndex) {
    return BeeUtils.toDecimalOrNull(getValue(rowIndex, colIndex));
  }

  public BigDecimal getDecimal(int rowIndex, String colName) {
    return getDecimal(rowIndex, getColumnIndex(colName));
  }

  public BigDecimal[] getDecimalColumn(int index) {
    BigDecimal[] col = new BigDecimal[getNumberOfRows()];

    for (int i = 0; i < getNumberOfRows(); i++) {
      col[i] = getDecimal(i, index);
    }
    return col;
  }

  public BigDecimal[] getDecimalColumn(String colName) {
    return getDecimalColumn(getColumnIndex(colName));
  }

  public Double getDouble(int rowIndex, int colIndex) {
    return BeeUtils.toDoubleOrNull(getValue(rowIndex, colIndex));
  }

  public Double getDouble(int rowIndex, String colName) {
    return getDouble(rowIndex, getColumnIndex(colName));
  }

  public Double[] getDoubleColumn(int index) {
    Double[] col = new Double[getNumberOfRows()];

    for (int i = 0; i < getNumberOfRows(); i++) {
      col[i] = getDouble(i, index);
    }
    return col;
  }

  public Double[] getDoubleColumn(String colName) {
    return getDoubleColumn(getColumnIndex(colName));
  }

  public <E extends Enum<?>> E getEnum(int rowIndex, int colIndex, Class<E> clazz) {
    return EnumUtils.getEnumByIndex(clazz, getInt(rowIndex, colIndex));
  }

  public <E extends Enum<?>> E getEnum(int rowIndex, String colName, Class<E> clazz) {
    return getEnum(rowIndex, getColumnIndex(colName), clazz);
  }

  public static SimpleRowSet getIfPresent(Map<String, String> map, String key) {
    if (BeeUtils.containsKey(map, key)) {
      return restore(map.get(key));
    } else {
      return null;
    }
  }

  public Integer getInt(int rowIndex, int colIndex) {
    return BeeUtils.toIntOrNull(getValue(rowIndex, colIndex));
  }

  public Integer getInt(int rowIndex, String colName) {
    return getInt(rowIndex, getColumnIndex(colName));
  }

  public Integer[] getIntColumn(int index) {
    Integer[] col = new Integer[getNumberOfRows()];

    for (int i = 0; i < getNumberOfRows(); i++) {
      col[i] = getInt(i, index);
    }
    return col;
  }

  public Integer[] getIntColumn(String colName) {
    return getIntColumn(getColumnIndex(colName));
  }

  public Long getLong(int rowIndex, int colIndex) {
    return BeeUtils.toLongOrNull(getValue(rowIndex, colIndex));
  }

  public Long getLong(int rowIndex, String colName) {
    return getLong(rowIndex, getColumnIndex(colName));
  }

  public Long[] getLongColumn(int index) {
    Long[] col = new Long[getNumberOfRows()];

    for (int i = 0; i < getNumberOfRows(); i++) {
      col[i] = getLong(i, index);
    }
    return col;
  }

  public Long[] getLongColumn(String colName) {
    return getLongColumn(getColumnIndex(colName));
  }

  public int getNumberOfColumns() {
    return columns.size();
  }

  public int getNumberOfRows() {
    return rows.size();
  }

  public SimpleRow getRow(int index) {
    if (BeeUtils.isIndex(rows, index)) {
      return new SimpleRow(index);
    }
    return null;
  }

  public SimpleRow getRowByKey(String keyName, String keyValue) {
    return getRow(getKeyIndex(keyName, keyValue));
  }

  public List<String[]> getRows() {
    return rows;
  }

  public String getValue(int rowIndex, int colIndex) {
    String[] data = getValues(rowIndex);

    if (data == null) {
      return null;
    }
    Assert.isIndex(colIndex, data.length);
    return data[colIndex];
  }

  public String getValue(int rowIndex, String colName) {
    return getValue(rowIndex, getColumnIndex(colName));
  }

  public String getValueByKey(String keyName, String keyValue, String colName) {
    return getValue(getKeyIndex(keyName, keyValue), getColumnIndex(colName));
  }

  public String[] getValues(int index) {
    if (BeeUtils.isIndex(rows, index)) {
      return rows.get(index);
    }
    return null;
  }

  public boolean hasColumn(String colName) {
    if (BeeUtils.isEmpty(colName)) {
      return false;
    }
    return columns.containsKey(colName.toLowerCase());
  }

  public boolean isEmpty() {
    return rows.isEmpty();
  }

  @Override
  public Iterator<SimpleRow> iterator() {
    return new RowSetIterator();
  }

  public void removeColumn(String colName) {
    if (BeeUtils.isEmpty(colName)) {
      return;
    }
    String key = colName.toLowerCase();
    if (!columns.containsKey(key)) {
      return;
    }
    int index = columns.remove(key);
    columnNames = ArrayUtils.remove(columnNames, index);

    for (int i = 0; i < rows.size(); i++) {
      rows.set(i, ArrayUtils.remove(rows.get(i), index));
    }
    indexes = null;
  }

  public static SimpleRowSet restore(String data) {
    return BeeSerializable.restore(data, SimpleRowSet::new);
  }

  @Override
  public String serialize() {
    return serializeMembers(Serial.class, serial -> {
      Object value = null;

      switch (serial) {
        case COLUMN_NAMES:
          value = columnNames;
          break;

        case COLUMNS:
          value = columns;
          break;

        case ROWS:
          value = rows;
          break;
      }
      return value;
    });
  }

  public void setValue(int rowIndex, int colIndex, String value) {
    Assert.isIndex(rows, rowIndex);
    String[] values = rows.get(rowIndex);
    Assert.isIndex(colIndex, values.length);
    values[colIndex] = value;
  }

  public void setValue(int rowIndex, String colName, String value) {
    setValue(rowIndex, getColumnIndex(colName), value);
  }

  private int getKeyIndex(String keyName, String keyValue) {
    Assert.notNull(keyValue);
    int colIndex = getColumnIndex(keyName);

    if (indexes == null) {
      indexes = new HashMap<>();
    }
    if (!indexes.containsKey(colIndex)) {
      Map<String, Integer> index = Maps.newHashMapWithExpectedSize(getNumberOfRows());

      for (int i = 0; i < getNumberOfRows(); i++) {
        String value = getValue(i, colIndex);

        if (value != null) {
          index.put(value, i);
        }
      }
      indexes.put(colIndex, index);
    }
    Integer idx = indexes.get(colIndex).get(keyValue);

    if (idx == null) {
      idx = BeeConst.UNDEF;
    }
    return idx;
  }
}
