package com.butent.bee.client.grid.column;

import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.data.CellSource;

/**
 * Enables using columns for currency expressions.
 */

public class CurrencyColumn extends DecimalColumn {

  public CurrencyColumn(CellSource cellSource) {
    this(Format.getDefaultMoneyFormat(), cellSource);
  }

  public CurrencyColumn(NumberFormat format, CellSource cellSource) {
    super(format, cellSource);
  }
}
