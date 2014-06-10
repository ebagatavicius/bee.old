package com.butent.bee.client.grid.column;

import com.google.gwt.dom.client.Element;

import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.cell.SelectionCell;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.utils.BeeUtils;

public class SelectionColumn extends AbstractColumn<Boolean> {
  
  private final CellGrid grid;
  
  public SelectionColumn(CellGrid grid) {
    this(grid, new SelectionCell());
  }
  
  private SelectionColumn(CellGrid grid, SelectionCell cell) {
    super(cell);
    this.grid = grid;
    setTextAlign(TextAlign.CENTER);
  }

  @Override
  public XCell export(CellContext context, Integer styleRef, XSheet sheet) {
    if (context != null && BeeUtils.isTrue(getValue(context.getRow()))) {
      return new XCell(context.getColumnIndex(), BeeConst.STRING_CHECK_MARK, styleRef);
    } else {
      return null;
    }
  }

  @Override
  public ColType getColType() {
    return ColType.SELECTION;
  }

  @Override
  public String getString(CellContext context) {
    Boolean value = getValue(context.getRow());
    return (value == null) ? null : BeeUtils.toString(value);
  }

  @Override
  public String getStyleSuffix() {
    return "selection";
  }
  
  @Override
  public Boolean getValue(IsRow row) {
    if (row != null && getGrid() != null) {
      return getGrid().isRowSelected(row.getId());
    } else {
      return null;
    }
  }

  @Override
  public ValueType getValueType() {
    return ValueType.BOOLEAN;
  }
  
  @Override
  public Integer initExport(XSheet sheet) {
    if (sheet == null) {
      return null;
    } else {
      return sheet.registerStyle(XStyle.center());
    }
  }

  public void update(Element cellElement, boolean value) {
    ((SelectionCell) getCell()).update(cellElement, value);
  }

  private CellGrid getGrid() {
    return grid;
  }
}
