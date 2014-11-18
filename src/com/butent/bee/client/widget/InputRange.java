package com.butent.bee.client.widget;

import com.google.gwt.event.shared.HandlerRegistration;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.html.builder.elements.Input;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Implements a user interface component which lets to select a value by dragging a slider with a
 * mouse.
 */

public class InputRange extends InputInteger {

  private static Input.Type inputType = Input.Type.RANGE;

  public InputRange() {
    super();
    DomUtils.setInputType(this, inputType);
  }

  public InputRange(int min, int max) {
    super(inputType, min, max);
  }

  public InputRange(int min, int max, int step) {
    super(inputType, min, max, step);
  }

  @Override
  public HandlerRegistration addEditChangeHandler(EditChangeHandler handler) {
    return addValueChangeHandler(handler);
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return null;
  }

  @Override
  public String getIdPrefix() {
    return "slid";
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.INPUT_RANGE;
  }

  @Override
  public void setValue(String value) {
    if (BeeUtils.isEmpty(value) && BeeUtils.isInt(getMinValue())) {
      super.setValue(getMinValue());
    } else {
      super.setValue(value);
    }
  }

  @Override
  protected String getDefaultStyleName() {
    return BeeConst.CSS_CLASS_PREFIX + "InputRange";
  }

  @Override
  protected boolean isTextBox() {
    return false;
  }
}
