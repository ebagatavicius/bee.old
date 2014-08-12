package com.butent.bee.shared.data;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;
import java.util.Map;

/**
 * Extends {@code RowList} class and enables using row set objects which are chunks of data put in
 * columns and rows.
 */

public class BeeRowSet extends RowList<BeeRow, BeeColumn> implements BeeSerializable, HasViewName {

  /**
   * Contains a list of items for serialization.
   */

  private enum Serial {
    VIEW, COLUMNS, ROWS, PROPERTIES
  }

  public static BeeRowSet getIfPresent(Map<String, String> map, String key) {
    if (BeeUtils.containsKey(map, key)) {
      String serialized = map.get(key);
      if (!BeeUtils.isEmpty(serialized)) {
        return restore(serialized);
      }
    }
    return null;
  }

  public static BeeRowSet maybeRestore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    } else {
      return restore(s);
    }
  }

  public static BeeRowSet restore(String s) {
    BeeRowSet rs = new BeeRowSet();
    rs.deserialize(s);
    return rs;
  }

  private String viewName;

  public BeeRowSet(List<BeeColumn> columns) {
    super();
    setColumns(columns);
  }

  public BeeRowSet(BeeColumn... columns) {
    super();
    if (columns != null) {
      for (BeeColumn column : columns) {
        addColumn(column);
      }
    }
  }

  public BeeRowSet(String viewName, List<BeeColumn> columns) {
    this(columns);
    this.viewName = viewName;
  }

  public BeeRowSet(String viewName, List<BeeColumn> columns, List<BeeRow> rows) {
    this(viewName, columns);
    setRows(rows);
  }

  public int addEmptyRow() {
    return addRow(new BeeRow(DataUtils.NEW_ROW_ID, new String[getNumberOfColumns()]));
  }

  public int addRow(long id, String[] data) {
    int idx = addRow(id, 0, data);
    return idx;
  }

  public int addRow(long id, long version, String[] data) {
    return addRow(new BeeRow(id, version, data));
  }

  public int addRow(long id, long version, List<String> data) {
    return addRow(new BeeRow(id, version, data));
  }

  @Override
  public BeeRowSet copy() {
    return DataUtils.cloneRowSet(this);
  }

  @Override
  public BeeRowSet create() {
    return new BeeRowSet();
  }

  @Override
  public BeeColumn createColumn(ValueType type, String label, String id) {
    return new BeeColumn(type, label, id);
  }

  @Override
  public BeeRow createRow(long id) {
    return new BeeRow(id, 0);
  }

  @Override
  public void deserialize(String s) {
    Assert.isTrue(getNumberOfColumns() == 0);

    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case VIEW:
          setViewName(value);
          break;

        case COLUMNS:
          String[] cArr = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(cArr)) {
            List<BeeColumn> columns = Lists.newArrayList();

            for (String col : cArr) {
              columns.add(BeeColumn.restore(col));
            }
            setColumns(columns);
          }
          break;

        case ROWS:
          String[] data = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(data)) {
            for (String r : data) {
              addRow(BeeRow.restore(r));
            }
          }
          break;

        case PROPERTIES:
          if (!BeeUtils.isEmpty(value)) {
            setTableProperties(CustomProperties.restore(value));
          }
          break;
      }
    }
  }

  public String getShadowString(int rowIdx, int columnIdx) {
    return getRow(rowIdx).getShadowString(columnIdx);
  }

  public String getShadowString(int rowIdx, String columnId) {
    return getShadowString(rowIdx, getColumnIndex(columnId));
  }

  public String getStringByRowId(long rowId, String columnId) {
    return getString(getRowIndex(rowId), getColumnIndex(columnId));
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  public boolean isEmpty() {
    return getNumberOfRows() <= 0;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case VIEW:
          arr[i++] = getViewName();
          break;

        case COLUMNS:
          arr[i++] = getColumns();
          break;

        case ROWS:
          arr[i++] = getRows();
          break;

        case PROPERTIES:
          arr[i++] = getTableProperties();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setViewName(String viewName) {
    this.viewName = viewName;
  }

  public boolean updateCell(long rowId, String columnId, String value) {
    int columnIdx = getColumnIndex(columnId);
    if (BeeConst.isUndef(columnIdx)) {
      return false;
    } else {
      return updateCell(rowId, columnIdx, value);
    }
  }

  public boolean updateCell(long rowId, int columnIdx, String value) {
    BeeRow row = getRowById(rowId);
    if (row == null) {
      return false;
    } else {
      row.setValue(columnIdx, value);
      return true;
    }
  }
}
