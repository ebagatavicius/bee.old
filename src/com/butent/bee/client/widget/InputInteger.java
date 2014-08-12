package com.butent.bee.client.widget;

import com.google.common.base.CharMatcher;
import com.google.gwt.dom.client.Element;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.shared.html.builder.elements.Input;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Enables to use a component for input of integer type values with minimum and maximum values and a
 * increment step.
 */

public class InputInteger extends InputNumber {

  public InputInteger() {
    super();
  }

  public InputInteger(Element element) {
    super(element);
  }

  public InputInteger(Input.Type type, int min, int max) {
    this(type, min, max, 1);
  }

  public InputInteger(Input.Type type, int min, int max, int step) {
    initAttributes(type, min, max, step);
  }

  @Override
  protected String getDefaultStyleName() {
    return "bee-InputInteger";
  }

  @Override
  public String getIdPrefix() {
    return "int";
  }

  public int getIntValue() {
    return BeeUtils.toInt(getValue());
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.INPUT_INTEGER;
  }

  @Override
  public void setMaxValue(String maxValue) {
    super.setMaxValue(maxValue);
    if (BeeUtils.isInt(maxValue)) {
      DomUtils.setMax(this, BeeUtils.toInt(maxValue));
    }
  }

  @Override
  public void setMinValue(String minValue) {
    super.setMinValue(minValue);
    if (BeeUtils.isInt(minValue)) {
      DomUtils.setMin(this, BeeUtils.toInt(minValue));
    }
  }

  @Override
  public void setStepValue(int stepValue) {
    super.setStepValue(stepValue);

    if (stepValue > 0) {
      DomUtils.setStep(this, stepValue);
    } else {
      DomUtils.removeStep(this);
    }
  }

  public void setValue(int value) {
    setValue(Integer.toString(value));
  }

  @Override
  protected boolean checkType(String v) {
    return BeeUtils.isInt(v);
  }

  @Override
  protected CharMatcher getDefaultCharMatcher() {
    return InputNumber.INT_CHAR_MATCHER;
  }

  protected void initAttributes(Input.Type type, int min, int max, int step) {
    if (type != null) {
      DomUtils.setInputType(this, type);
    }
    if (min < max) {
      setMinValue(BeeUtils.toString(min));
      setMaxValue(BeeUtils.toString(max));
    }
    if (step != 0) {
      setStepValue(step);
    }
  }
}
