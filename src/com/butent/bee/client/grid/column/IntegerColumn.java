package com.butent.bee.client.grid.column;

import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;

/**
 * Enables using columns for integer type data.
 */

public class IntegerColumn extends NumberColumn<Integer> {

  public IntegerColumn(CellSource cellSource) {
    this(Format.getDefaultIntegerFormat(), cellSource);
  }

  public IntegerColumn(NumberFormat format, CellSource cellSource) {
    super(format, cellSource);
  }

  @Override
  protected Integer getNumber(IsRow row) {
    return getCellSource().getInteger(row);
  }

  @Override
  public String getStyleSuffix() {
    return "integer";
  }
}
