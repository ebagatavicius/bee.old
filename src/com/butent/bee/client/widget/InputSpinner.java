package com.butent.bee.client.widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.shared.html.builder.elements.Input;
import com.butent.bee.shared.ui.EditorAction;

/**
 * Implements standard spinner user interface component, letting user to increase and decrease input
 * value as well as input it straight from the keyboard.
 */

public class InputSpinner extends InputInteger {

  private static Input.Type inputType = Input.Type.NUMBER;

  public InputSpinner() {
    super();
    DomUtils.setInputType(this, inputType);
  }

  public InputSpinner(int min, int max) {
    super(inputType, min, max);
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return null;
  }

  @Override
  public String getIdPrefix() {
    return "spin";
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.INPUT_SPINNER;
  }

  @Override
  protected String getDefaultStyleName() {
    return "bee-InputSpinner";
  }
}
