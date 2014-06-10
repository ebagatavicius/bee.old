package com.butent.bee.shared.data;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Extends {@code AbstractRow} class, contains it's information in string sequences.
 */

public class StringRow extends AbstractRow {

  private final List<String> values = new ArrayList<>();

  public StringRow(long id, List<String> values) {
    super(id);

    if (values != null) {
      this.values.addAll(values);
    }
  }

  public StringRow(long id, String[] arr) {
    super(id);

    if (arr != null) {
      for (String value : arr) {
        this.values.add(value);
      }
    }
  }

  @Override
  public void addCell(IsCell cell) {
    Assert.notNull(cell);
    values.add(cell.getValue().getString());
  }

  @Override
  public void clearCell(int index) {
    assertIndex(index);
    values.set(index, null);
  }

  @Override
  public Boolean getBoolean(int col) {
    return BeeUtils.toBooleanOrNull(getString(col));
  }

  @Override
  public IsCell getCell(int index) {
    assertIndex(index);
    return new TableCell(new TextValue(values.get(index)));
  }

  @Override
  public List<IsCell> getCells() {
    List<IsCell> lst = Lists.newArrayList();
    for (int i = 0; i < values.size(); i++) {
      lst.add(getCell(i));
    }
    return lst;
  }

  @Override
  public JustDate getDate(int col) {
    if (isNull(col)) {
      return null;
    }
    return new JustDate(getInteger(col));
  }

  @Override
  public DateTime getDateTime(int col) {
    if (isNull(col)) {
      return null;
    }
    return new DateTime(getLong(col));
  }

  @Override
  public BigDecimal getDecimal(int col) {
    return BeeUtils.toDecimalOrNull(getString(col));
  }

  @Override
  public Double getDouble(int col) {
    return BeeUtils.toDoubleOrNull(getString(col));
  }

  @Override
  public Integer getInteger(int col) {
    return BeeUtils.toIntOrNull(getString(col));
  }

  @Override
  public Long getLong(int col) {
    return BeeUtils.toLongOrNull(getString(col));
  }

  @Override
  public int getNumberOfCells() {
    return values.size();
  }

  @Override
  public String getString(int index) {
    assertIndex(index);
    return values.get(index);
  }

  public List<String> getValues() {
    return values;
  }

  @Override
  public void insertCell(int index, IsCell cell) {
    Assert.betweenInclusive(index, 0, getNumberOfCells());
    values.add(index, cell.getValue().getString());
  }

  @Override
  public boolean isNull(int index) {
    assertIndex(index);
    return values.get(index) == null;
  }

  @Override
  public void removeCell(int index) {
    assertIndex(index);
    values.remove(index);
  }

  @Override
  public void setCell(int index, IsCell cell) {
    assertIndex(index);
    values.set(index, cell.getValue().getString());
  }

  @Override
  public void setCells(List<IsCell> cells) {
    for (int i = 0; i < cells.size(); i++) {
      setCell(i, cells.get(i));
    }
  }

  @Override
  public void setValue(int index, BigDecimal value) {
    if (value == null) {
      clearCell(index);
    } else {
      setValue(index, value.toString());
    }
  }

  @Override
  public void setValue(int index, Boolean value) {
    setValue(index, BooleanValue.pack(value));
  }

  @Override
  public void setValue(int index, DateTime value) {
    if (value == null) {
      clearCell(index);
    } else {
      setValue(index, BeeUtils.toString(value.getTime()));
    }
  }

  @Override
  public void setValue(int index, Double value) {
    if (value == null) {
      clearCell(index);
    } else {
      setValue(index, BeeUtils.toString(value));
    }
  }

  @Override
  public void setValue(int index, Integer value) {
    if (value == null) {
      clearCell(index);
    } else {
      setValue(index, BeeUtils.toString(value));
    }
  }

  @Override
  public void setValue(int index, JustDate value) {
    if (value == null) {
      clearCell(index);
    } else {
      setValue(index, BeeUtils.toString(value.getDays()));
    }
  }

  @Override
  public void setValue(int index, Long value) {
    if (value == null) {
      clearCell(index);
    } else {
      setValue(index, BeeUtils.toString(value));
    }
  }

  @Override
  public void setValue(int index, String value) {
    assertIndex(index);
    values.set(index, value);
  }

  @Override
  public void setValue(int index, Value value) {
    if (value == null) {
      clearCell(index);
    } else {
      setValue(index, value.getString());
    }
  }

  public void setValues(List<String> values) {
    BeeUtils.overwrite(this.values, values);
  }

  public void setValues(String[] arr) {
    if (!values.isEmpty()) {
      values.clear();
    }

    if (arr != null) {
      for (String value : arr) {
        values.add(value);
      }
    }
  }

  protected void assertIndex(int index) {
    Assert.isIndex(index, getNumberOfCells());
  }
}
