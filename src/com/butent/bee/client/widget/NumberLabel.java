package com.butent.bee.client.widget;

import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.i18n.HasNumberFormat;
import com.butent.bee.client.i18n.NumberRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.css.values.TextAlign;

/**
 * Enables using text label user interface component.
 */

public class NumberLabel<T extends Number> extends ValueLabel<T> implements HasNumberFormat {

  public NumberLabel(NumberFormat format, boolean inline) {
    super(new NumberRenderer(format), inline);
  }

  public NumberLabel(String pattern, boolean inline) {
    super(new NumberRenderer(pattern), inline);
  }

  @Override
  public NumberFormat getNumberFormat() {
    return ((NumberRenderer) getRenderer()).getNumberFormat();
  }

  @Override
  public void setNumberFormat(NumberFormat format) {
    ((NumberRenderer) getRenderer()).setNumberFormat(format);
  }

  @Override
  protected void init() {
    super.init();
    StyleUtils.setTextAlign(getElement(), TextAlign.RIGHT);
  }
}
