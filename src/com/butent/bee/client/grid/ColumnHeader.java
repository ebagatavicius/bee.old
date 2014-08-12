package com.butent.bee.client.grid;

import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.grid.cell.HeaderCell;
import com.butent.bee.shared.ui.HasCaption;

/**
 * Creates new header cells for grids.
 */

public class ColumnHeader extends Header<String> implements HasCaption {

  private final String columnId;
  private final String exportLabel;

  public ColumnHeader(String columnId, String caption, String exportLabel) {
    this(columnId, new HeaderCell(caption), exportLabel);
  }

  public ColumnHeader(String columnId, AbstractCell<String> cell, String exportLabel) {
    super(cell);
    this.columnId = columnId;
    this.exportLabel = exportLabel;
  }

  @Override
  public String getCaption() {
    if (getCell() instanceof HasCaption) {
      return ((HasCaption) getCell()).getCaption();
    } else {
      return null;
    }
  }

  public String getExportLabel() {
    return exportLabel;
  }

  @Override
  public String getValue() {
    return columnId;
  }
}
