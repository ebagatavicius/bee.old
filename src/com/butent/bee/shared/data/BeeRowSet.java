package com.butent.bee.shared.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.ListSequence;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class BeeRowSet extends RowList<BeeRow, BeeColumn> implements BeeSerializable {

  private enum SerializationMembers {
    VIEW, COLUMNS, ROWS
  }

  private static Logger logger = Logger.getLogger(BeeRowSet.class.getName());

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
    for (BeeColumn column : columns) {
      addColumn(column);
    }
  }

  private BeeRowSet(ListSequence<BeeRow> rows) {
    super(rows);
  }

  public int addEmptyRow() {
    String[] arr = new String[getNumberOfColumns()];
    Arrays.fill(arr, BeeConst.STRING_EMPTY);
    return addRow(-System.currentTimeMillis(), arr);
  }

  public int addRow(long id, String[] data) {
    return addRow(new BeeRow(id, data));
  }

  public int addRow(long id, long version, String[] data) {
    int idx = addRow(id, data);
    getRow(idx).setVersion(version);
    return idx;
  }

  @Override
  public BeeRowSet clone() {
    BeeRowSet result = new BeeRowSet(getRows());
    cloneTableDescription(result);
    return result;
  }

  public void commit(BeeRowSet update) {
    if (!BeeUtils.isEmpty(update)) {
      for (BeeRow upd : update.getRows()) {
        BeeRow dst = findRow(upd.getId());

        if (!BeeUtils.isEmpty(dst)) {
          if (dst.isMarkedForDelete()) {
            removeRow(dst);
          } else {
            if (dst.isMarkedForInsert()) {
              dst.setId(upd.getNewId());
            }
            dst.setValues(upd.getValues());
            dst.setVersion(upd.getVersion());
            dst.reset();
          }
        }
      }
    }
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
    return new BeeRow(id, new String[0]);
  }

  @Override
  public void deserialize(String s) {
    Assert.isTrue(getNumberOfColumns() == 0);

    SerializationMembers[] members = SerializationMembers.values();
    String[] arr = Codec.beeDeserialize(s);
    Assert.arrayLength(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      SerializationMembers member = members[i];
      String value = arr[i];

      switch (member) {
        case VIEW:
          setViewName(value);
          break;

        case COLUMNS:
          String[] cArr = Codec.beeDeserialize(value);
          List<BeeColumn> columns = Lists.newArrayList();
          for (int j = 0; j < cArr.length; j++) {
            columns.add(BeeColumn.restore(cArr[j]));
          }
          setColumns(columns);
          break;

        case ROWS:
          if (!BeeUtils.isEmpty(value)) {
            String[] data = Codec.beeDeserialize(value);
            for (String r : data) {
              addRow(BeeRow.restore(r));
            }
          }
          break;
        default:
          logger.severe("Unhandled serialization member: " + member);
          break;
      }
    }
  }

  public BeeRowSet getChanges() {
    if (isEmpty()) {
      return null;
    }

    BeeRowSet update = create();
    cloneTableDescription(update);
    update.setViewName(getViewName());

    for (BeeRow row : getRows()) {
      if (!BeeUtils.isEmpty(row.getShadow())
          || row.isMarkedForDelete() || row.isMarkedForInsert()) {
        update.addRow(row);
      }
    }

    if (update.isEmpty()) {
      return null;
    }
    return update;
  }

  public BeeColumn[] getColumnArray() {
    BeeColumn[] arr = new BeeColumn[getNumberOfColumns()];
    for (int i = 0; i < getNumberOfColumns(); i++) {
      arr[i] = getColumn(i);
    }
    return arr;
  }

  public String[] getColumnLabels() {
    String[] arr = new String[getNumberOfColumns()];
    for (int i = 0; i < getNumberOfColumns(); i++) {
      arr[i] = getColumnLabel(i);
    }
    return arr;
  }

  public int getInt(BeeRow row, String columnId) {
    Assert.notNull(row);
    return row.getInt(getColumnIndex(columnId));
  }

  public long getLong(BeeRow row, String columnId) {
    Assert.notNull(row);
    return row.getLong(getColumnIndex(columnId));
  }

  public Object getOriginal(BeeRow row, int colIndex) {
    Assert.notNull(row);
    assertColumnIndex(colIndex);
    return row.getOriginal(colIndex, getColumn(colIndex).getSqlType());
  }

  public Object getOriginal(BeeRow row, String columnId) {
    return getOriginal(row, getColumnIndex(columnId));
  }

  public String getString(BeeRow row, String columnId) {
    Assert.notNull(row);
    return row.getString(getColumnIndex(columnId));
  }

  public String getViewName() {
    return viewName;
  }

  public boolean isEmpty() {
    return getNumberOfRows() <= 0;
  }

  public void removeRow(BeeRow row) {
    getRows().getList().remove(row);
  }

  public void rollback() {
    if (!isEmpty()) {
      for (BeeRow row : ImmutableList.copyOf(getRows().getList())) {
        if (row.isMarkedForInsert()) {
          removeRow(row);
        } else {
          if (!BeeUtils.isEmpty(row.getShadow())) {
            for (Entry<Integer, String> shadow : row.getShadow().entrySet()) {
              row.setValue(shadow.getKey(), shadow.getValue());
            }
          }
          row.reset();
        }
      }
    }
  }

  @Override
  public String serialize() {
    StringBuilder sb = new StringBuilder();

    for (SerializationMembers member : SerializationMembers.values()) {
      switch (member) {
        case VIEW:
          sb.append(Codec.beeSerialize(getViewName()));
          break;

        case COLUMNS:
          sb.append(Codec.beeSerialize(getColumns()));
          break;

        case ROWS:
          sb.append(Codec.beeSerialize(getRows().getList()));
          break;

        default:
          logger.severe("Unhandled serialization member: " + member);
          break;
      }
    }
    return sb.toString();
  }

  public void setValue(BeeRow row, String columnId, String value) {
    row.setValue(getColumnIndex(columnId), value);
  }

  public void setViewName(String viewName) {
    this.viewName = viewName;
  }

  private BeeRow findRow(long id) {
    for (BeeRow row : getRows()) {
      if (row.getId() == id) {
        return row;
      }
    }
    return null;
  }
}
