package com.butent.bee.client.i18n;

import com.google.gwt.text.shared.AbstractRenderer;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Handles specific regional formatting of date values.
 */

public class DateRenderer extends AbstractRenderer<JustDate> implements HasDateTimeFormat {

  private DateTimeFormat format;

  public DateRenderer() {
    this(Format.getDefaultDateFormat());
  }

  public DateRenderer(DateTimeFormat format) {
    this.format = format;
  }

  public DateRenderer(String pattern) {
    this.format = BeeUtils.isEmpty(pattern) ? null : Format.getDateTimeFormat(pattern);
  }

  @Override
  public DateTimeFormat getDateTimeFormat() {
    return format;
  }

  @Override
  public String render(JustDate object) {
    if (object == null) {
      return BeeConst.STRING_EMPTY;
    } else if (getDateTimeFormat() == null) {
      return object.toString();
    } else {
      return getDateTimeFormat().format(object);
    }
  }

  @Override
  public void setDateTimeFormat(DateTimeFormat dtFormat) {
    this.format = dtFormat;
  }
}
