package com.butent.bee.client.grid.column;

import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;

/**
 * Enables using columns for {@code Long} type data.
 */

public class LongColumn extends NumberColumn<Long> {

  public LongColumn(CellSource cellSource) {
    this(Format.getDefaultLongFormat(), cellSource);
  }

  public LongColumn(NumberFormat format, CellSource cellSource) {
    super(format, cellSource);
  }

  @Override
  protected Long getNumber(IsRow row) {
    return getCellSource().getLong(row);
  }

  @Override
  public String getStyleSuffix() {
    return "long";
  }
}
