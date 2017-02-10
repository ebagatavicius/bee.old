package com.butent.bee.client.grid.cell;

import com.butent.bee.client.grid.CellContext;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.i18n.HasDateTimeFormat;
import com.butent.bee.shared.time.DateTime;

public class DateTimeCell extends AbstractCell<DateTime> implements HasDateTimeFormat {

  private DateTimeFormat format;

  public DateTimeCell() {
    this(null);
  }

  public DateTimeCell(DateTimeFormat format) {
    super();
    this.format = format;
  }

  @Override
  public DateTimeFormat getDateTimeFormat() {
    return format;
  }

  @Override
  public String render(CellContext context, DateTime value) {
    if (value != null) {
      return (format == null) ? Format.renderDateTime(value) : format.format(value);
    } else {
      return null;
    }
  }

  @Override
  public void setDateTimeFormat(DateTimeFormat dtFormat) {
    this.format = dtFormat;
  }
}
