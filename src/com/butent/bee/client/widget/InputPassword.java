package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;

import com.butent.bee.shared.BeeConst;

/**
 * Implements a user interface component for inserting passwords.
 */

public class InputPassword extends InputText {

  public InputPassword(int maxLength) {
    super(Document.get().createPasswordInputElement());
    setMaxLength(maxLength);
  }

  @Override
  public String getIdPrefix() {
    return "pswd";
  }

  @Override
  protected String getDefaultStyleName() {
    return BeeConst.CSS_CLASS_PREFIX + "InputPassword";
  }
}
