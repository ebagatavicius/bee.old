package com.butent.bee.client.widget;

import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.DateTimeRenderer;
import com.butent.bee.shared.i18n.HasDateTimeFormat;
import com.butent.bee.shared.time.DateTime;

/**
 * Enables using datetime type label user interface component.
 */

public class DateTimeLabel extends ValueLabel<DateTime> implements HasDateTimeFormat {

  public DateTimeLabel(boolean inline) {
    super(new DateTimeRenderer(), inline);
  }

  public DateTimeLabel(DateTimeFormat format, boolean inline) {
    super(new DateTimeRenderer(format), inline);
  }

  public DateTimeLabel(String pattern, boolean inline) {
    super(new DateTimeRenderer(pattern), inline);
  }

  @Override
  public DateTimeFormat getDateTimeFormat() {
    return ((DateTimeRenderer) getRenderer()).getDateTimeFormat();
  }

  @Override
  public void setDateTimeFormat(DateTimeFormat format) {
    ((DateTimeRenderer) getRenderer()).setDateTimeFormat(format);
  }
}
