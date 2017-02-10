package com.butent.bee.client.grid.column;

import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.grid.cell.DateCell;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.shared.i18n.HasDateTimeFormat;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.time.JustDate;

public class DateColumn extends DataColumn<JustDate> implements HasDateTimeFormat {

  public DateColumn(CellSource cellSource) {
    this(new DateCell(), cellSource);
  }

  public DateColumn(DateTimeFormat format, CellSource cellSource) {
    this(new DateCell(format), cellSource);
  }

  public DateColumn(AbstractCell<JustDate> cell, CellSource cellSource) {
    super(cell, cellSource);
  }

  @Override
  public DateTimeFormat getDateTimeFormat() {
    if (getCell() instanceof HasDateTimeFormat) {
      return ((HasDateTimeFormat) getCell()).getDateTimeFormat();
    } else {
      return null;
    }
  }

  @Override
  public String getStyleSuffix() {
    return "date";
  }

  @Override
  public JustDate getValue(IsRow row) {
    if (row == null) {
      return null;
    }
    return getCellSource().getDate(row);
  }

  @Override
  public void setDateTimeFormat(DateTimeFormat format) {
    if (getCell() instanceof HasDateTimeFormat) {
      ((HasDateTimeFormat) getCell()).setDateTimeFormat(format);
    }
  }
}
