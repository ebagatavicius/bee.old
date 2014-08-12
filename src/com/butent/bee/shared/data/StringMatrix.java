package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.StringRowArray;
import com.butent.bee.shared.data.value.ValueType;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Is an implementation of {@code AbstractTable} abstract class, realizes table structure through
 * string matrix principle.
 */

public class StringMatrix<C extends IsColumn> extends AbstractTable<StringRow, C> {

  private StringRowArray rows;

  public StringMatrix(List<String[]> data, String... columnLabels) {
    super(columnLabels);
    rows = new StringRowArray(new StringRow[data.size()]);
    for (int i = 0; i < data.size(); i++) {
      rows.set(i, new StringRow(i + 1, data.get(i)));
    }
  }

  public StringMatrix(String[][] data, String... columnLabels) {
    super(columnLabels);
    rows = new StringRowArray(new StringRow[data.length]);
    for (int i = 0; i < data.length; i++) {
      rows.set(i, new StringRow(i + 1, data[i]));
    }
  }

  protected StringMatrix() {
    super();
    this.rows = new StringRowArray(new StringRow[0]);
  }

  protected StringMatrix(List<C> columns) {
    super(columns);
  }

  protected StringMatrix(String... columnLabels) {
    super(columnLabels);
  }

  private StringMatrix(StringRowArray rows) {
    super();
    this.rows = rows;
  }

  @Override
  public void clearRows() {
    rows.clear();
  }

  @Override
  public StringMatrix<C> copy() {
    StringMatrix<C> result = new StringMatrix<>(rows);
    copyTableDescription(result);
    return result;
  }

  @Override
  public IsTable<StringRow, C> create() {
    return new StringMatrix<>();
  }

  @SuppressWarnings("unchecked")
  @Override
  public C createColumn(ValueType type, String label, String id) {
    return (C) new BeeColumn(type, label, id);
  }

  @Override
  public StringRow createRow(long id) {
    return new StringRow(id, BeeConst.EMPTY_STRING_ARRAY);
  }

  @Override
  public int getNumberOfRows() {
    return (rows == null) ? 0 : rows.getLength();
  }

  @Override
  public StringRow getRow(int rowIndex) {
    assertRowIndex(rowIndex);
    return rows.get(rowIndex);
  }

  @Override
  public List<StringRow> getRows() {
    return (rows == null) ? null : rows.getList();
  }

  @Override
  public String getString(int rowIndex, int colIndex) {
    assertRowIndex(rowIndex);
    assertColumnIndex(colIndex);
    return rows.get(rowIndex).getString(colIndex);
  }

  @Override
  public void removeRow(int rowIndex) {
    assertRowIndex(rowIndex);
    rows.remove(rowIndex);
  }

  @Override
  public void setValue(int rowIndex, int colIndex, String value) {
    assertRowIndex(rowIndex);
    assertColumnIndex(colIndex);
    rows.get(rowIndex).setValue(colIndex, value);
  }

  @Override
  public void sort(List<Pair<Integer, Boolean>> sortInfo, Comparator<String> collator) {
    Assert.notNull(sortInfo);
    Assert.isTrue(sortInfo.size() >= 1);

    if (getNumberOfRows() > 1) {
      sortRows(new RowOrdering<StringRow>(getColumns(), sortInfo, collator));
    }
  }

  @Override
  public void sortByRowId(boolean ascending) {
    if (getNumberOfRows() > 1) {
      sortRows(new RowIdOrdering(ascending));
    }
  }

  @Override
  protected void insertRow(int rowIndex, StringRow row) {
    Assert.betweenInclusive(rowIndex, 0, getNumberOfRows());
    rows.insert(rowIndex, row);
  }

  protected void setRows(StringRowArray rows) {
    this.rows = rows;
  }

  private void sortRows(Comparator<StringRow> comparator) {
    Pair<StringRow[], Integer> pair = rows.getArray(new StringRow[0]);
    int len = pair.getB();

    StringRow[] arr;
    if (len == pair.getA().length) {
      arr = pair.getA();
    } else {
      arr = new StringRow[len];
      System.arraycopy(pair.getA(), 0, arr, 0, len);
    }

    Arrays.sort(arr, comparator);
    rows.setValues(arr);
  }

  @Override
  public Iterator<StringRow> iterator() {
    return rows.iterator();
  }
}
