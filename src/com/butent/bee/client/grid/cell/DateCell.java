package com.butent.bee.client.grid.cell;

import com.butent.bee.client.grid.CellContext;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.i18n.HasDateTimeFormat;
import com.butent.bee.shared.time.JustDate;

/**
 * Enables using columns which contain date type data.
 */

public class DateCell extends AbstractCell<JustDate> implements HasDateTimeFormat {

  private DateTimeFormat format;

  public DateCell() {
    this(null);
  }

  public DateCell(DateTimeFormat format) {
    super();
    this.format = format;
  }

  @Override
  public DateTimeFormat getDateTimeFormat() {
    return format;
  }

  @Override
  public String render(CellContext context, JustDate value) {
    if (value != null) {
      return (format == null) ? Format.renderDate(value) : format.format(value);
    } else {
      return null;
    }
  }

  @Override
  public void setDateTimeFormat(DateTimeFormat dtFormat) {
    this.format = dtFormat;
  }
}
