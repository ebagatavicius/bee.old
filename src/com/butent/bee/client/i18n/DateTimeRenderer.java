package com.butent.bee.client.i18n;

import com.google.gwt.text.shared.AbstractRenderer;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.shared.i18n.HasDateTimeFormat;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Handles specific regional formatting of datetime values.
 */

public class DateTimeRenderer extends AbstractRenderer<DateTime> implements HasDateTimeFormat {

  private DateTimeFormat format;

  public DateTimeRenderer() {
  }

  public DateTimeRenderer(DateTimeFormat format) {
    this.format = format;
  }

  public DateTimeRenderer(String pattern) {
    this.format = BeeUtils.isEmpty(pattern) ? null : Format.parseDateTimeFormat(pattern);
  }

  @Override
  public DateTimeFormat getDateTimeFormat() {
    return format;
  }

  @Override
  public String render(DateTime object) {
    if (object == null) {
      return BeeConst.STRING_EMPTY;
    } else if (getDateTimeFormat() == null) {
      return Format.renderDateTime(object);
    } else {
      return getDateTimeFormat().format(object);
    }
  }

  @Override
  public void setDateTimeFormat(DateTimeFormat dtFormat) {
    this.format = dtFormat;
  }
}
