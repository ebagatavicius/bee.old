package com.butent.bee.client.widget;

import com.butent.bee.client.i18n.DateRenderer;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.shared.i18n.HasDateTimeFormat;
import com.butent.bee.shared.time.JustDate;

/**
 * Enables using date label user interface component.
 */

public class DateLabel extends ValueLabel<JustDate> implements HasDateTimeFormat {

  public DateLabel(boolean inline) {
    super(new DateRenderer(), inline);
  }

  public DateLabel(DateTimeFormat format, boolean inline) {
    super(new DateRenderer(format), inline);
  }

  public DateLabel(String pattern, boolean inline) {
    super(new DateRenderer(pattern), inline);
  }

  @Override
  public DateTimeFormat getDateTimeFormat() {
    return ((DateRenderer) getRenderer()).getDateTimeFormat();
  }

  @Override
  public void setDateTimeFormat(DateTimeFormat format) {
    ((DateRenderer) getRenderer()).setDateTimeFormat(format);
  }
}
