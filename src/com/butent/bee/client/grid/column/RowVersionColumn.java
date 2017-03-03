package com.butent.bee.client.grid.column;

import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.cell.DateTimeCell;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.shared.i18n.HasDateTimeFormat;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Manages a column with a timestamp for the last edit time of particular row.
 */

public class RowVersionColumn extends AbstractColumn<DateTime> implements HasDateTimeFormat {

  public RowVersionColumn() {
    this(null);
  }

  public RowVersionColumn(DateTimeFormat format) {
    super(new DateTimeCell(format));
  }

  @Override
  public ColType getColType() {
    return ColType.VERSION;
  }

  @Override
  public DateTimeFormat getDateTimeFormat() {
    if (getCell() instanceof HasDateTimeFormat) {
      return ((HasDateTimeFormat) getCell()).getDateTimeFormat();
    }
    return null;
  }

  @Override
  public String getString(CellContext context) {
    if (context.getRow() == null) {
      return null;
    }
    return BeeUtils.toString(context.getRow().getVersion());
  }

  @Override
  public String getStyleSuffix() {
    return "version";
  }

  @Override
  public DateTime getValue(IsRow row) {
    if (row == null) {
      return null;
    }
    return new DateTime(row.getVersion());
  }

  @Override
  public ValueType getValueType() {
    return ValueType.DATE_TIME;
  }

  @Override
  public void setDateTimeFormat(DateTimeFormat format) {
    if (getCell() instanceof HasDateTimeFormat) {
      ((HasDateTimeFormat) getCell()).setDateTimeFormat(format);
    }
  }
}
