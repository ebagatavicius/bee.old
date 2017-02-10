package com.butent.bee.client.grid.column;

import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.cell.NumberCell;
import com.butent.bee.client.i18n.HasNumberFormat;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.WhiteSpace;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Implements ID column renderer of data collections or structures(RowList, BeeRowSet) in
 * table-based widgets (CellGrid, ChildGrid). This column renderer display specified row ID
 * (usually unique) number of data structure or source.
 */

public class RowIdColumn extends AbstractColumn<Long> implements HasNumberFormat {

  /**
   * Creates ID column renderer with pre-defined styles without number format.
   */
  public RowIdColumn() {
    this(null);
  }

  /**
   * Creates ID column renderer with pre-defined styles with specified number format.
   * @param format Format of ID number for rendering.
   */
  public RowIdColumn(NumberFormat format) {
    super(new NumberCell<Long>(format));

    setTextAlign(TextAlign.RIGHT);
    setWhiteSpace(WhiteSpace.NOWRAP);
  }

  @Override
  public ColType getColType() {
    return ColType.ID;
  }

  @Override
  public NumberFormat getNumberFormat() {
    if (getCell() instanceof HasNumberFormat) {
      return ((HasNumberFormat) getCell()).getNumberFormat();
    }
    return null;
  }

  @Override
  public String getString(CellContext context) {
    if (context.getRow() == null) {
      return null;
    }
    return BeeUtils.toString(context.getRow().getId());
  }

  @Override
  public String getStyleSuffix() {
    return "id";
  }

  @Override
  public Long getValue(IsRow row) {
    if (row == null) {
      return null;
    }
    return row.getId();
  }

  @Override
  public ValueType getValueType() {
    return ValueType.LONG;
  }

  @Override
  public void setNumberFormat(NumberFormat format) {
    if (getCell() instanceof HasNumberFormat) {
      ((HasNumberFormat) getCell()).setNumberFormat(format);
    }
  }
}
