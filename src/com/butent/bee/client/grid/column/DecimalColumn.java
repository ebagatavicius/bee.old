package com.butent.bee.client.grid.column;

import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;

import java.math.BigDecimal;

/**
 * Enables using columns for decimal numbers type of data.
 */

public class DecimalColumn extends NumberColumn<BigDecimal> {

  public DecimalColumn(CellSource cellSource) {
    this(Format.getDecimalFormat(cellSource.getScale()), cellSource);
  }

  public DecimalColumn(NumberFormat format, CellSource cellSource) {
    super(format, cellSource);
  }

  @Override
  protected BigDecimal getNumber(IsRow row) {
    return getCellSource().getDecimal(row);
  }

  @Override
  public String getStyleSuffix() {
    return "decimal";
  }
}
