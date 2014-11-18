package com.butent.bee.shared.data;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.value.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends {@code AbstractRow} class, contains its information in object tree structure.
 */

public class TableRow extends AbstractRow {

  private List<IsCell> cells = new ArrayList<>();

  public TableRow(long id) {
    super(id);
  }

  @Override
  public void addCell(IsCell cell) {
    cells.add(cell);
  }

  @Override
  public void addValue(Value value) {
    addCell(new TableCell(value));
  }

  @Override
  public void clearCell(int index) {
    assertIndex(index);
    cells.set(index, null);
  }

  @Override
  public IsCell getCell(int index) {
    return cells.get(index);
  }

  @Override
  public List<IsCell> getCells() {
    return cells;
  }

  @Override
  public int getNumberOfCells() {
    return cells.size();
  }

  @Override
  public void insertCell(int index, IsCell cell) {
    assertIndex(index);
    cells.add(index, cell);
  }

  @Override
  public void removeCell(int index) {
    assertIndex(index);
    cells.remove(index);
  }

  @Override
  public void setCell(int index, IsCell cell) {
    assertIndex(index);
    cells.set(index, cell);
  }

  @Override
  public void setCells(List<IsCell> cells) {
    this.cells = cells;
  }

  protected void assertIndex(int index) {
    Assert.isIndex(cells, index);
  }
}
