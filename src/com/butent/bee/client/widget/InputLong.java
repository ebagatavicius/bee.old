package com.butent.bee.client.widget;

import com.google.common.base.CharMatcher;
import com.google.gwt.dom.client.Element;

import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Enables to use a component for input of {@code Long} type values.
 */

public class InputLong extends InputNumber {

  public InputLong() {
    super();
  }

  public InputLong(Element element) {
    super(element);
  }

  @Override
  public String getIdPrefix() {
    return "long";
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.INPUT_LONG;
  }

  @Override
  protected boolean checkType(String v) {
    return BeeUtils.isLong(v);
  }

  @Override
  protected CharMatcher getDefaultCharMatcher() {
    return InputNumber.INT_CHAR_MATCHER;
  }

  @Override
  protected String getDefaultStyleName() {
    return BeeConst.CSS_CLASS_PREFIX + "InputLong";
  }
}
