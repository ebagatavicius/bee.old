package com.butent.bee.client.grid.column;

import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.ui.ColumnDescription.ColType;

public abstract class DataColumn<C> extends AbstractColumn<C> {

  private final CellSource cellSource;

  public DataColumn(AbstractCell<C> cell, CellSource cellSource) {
    super(cell);
    this.cellSource = cellSource;
  }

  @Override
  public ColType getColType() {
    return ColType.DATA;
  }
  
  @Override
  public String getString(CellContext context) {
    if (context.getRow() == null) {
      return null;
    }
    return cellSource.getString(context.getRow());
  }

  @Override
  public ValueType getValueType() {
    return cellSource.getValueType();
  }

  protected CellSource getCellSource() {
    return cellSource;
  }
}
