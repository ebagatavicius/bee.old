package com.butent.bee.client.grid.column;

import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;

/**
 * Enables using columns for {@code Double} type data.
 */

public class DoubleColumn extends NumberColumn<Double> {

  public DoubleColumn(CellSource cellSource) {
    this(Format.getDefaultDoubleFormat(), cellSource);
  }

  public DoubleColumn(NumberFormat format, CellSource cellSource) {
    super(format, cellSource);
  }

  @Override
  protected Double getNumber(IsRow row) {
    return getCellSource().getDouble(row);
  }

  @Override
  public String getStyleSuffix() {
    return "double";
  }
}
