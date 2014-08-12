package com.butent.bee.client.dialog;

import com.butent.bee.shared.Validator;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

public abstract class StringCallback implements Validator<String> {

  private boolean required;

  public StringCallback() {
    this(true);
  }

  public StringCallback(boolean required) {
    this.required = required;
  }

  @Override
  public String getMessage(String value) {
    if (validate(value)) {
      return null;
    } else if (isRequired() && BeeUtils.isEmpty(value)) {
      return Localized.getConstants().valueRequired();
    } else {
      return Localized.getConstants().error();
    }
  }

  @Override
  public boolean isRequired() {
    return required;
  }

  public void onCancel() {
  }

  public abstract void onSuccess(String value);

  public void onTimeout(String value) {
    if (validate(value)) {
      onSuccess(value);
    } else {
      onCancel();
    }
  }

  @Override
  public void setRequired(boolean required) {
    this.required = required;
  }

  @Override
  public boolean validate(String value) {
    if (isRequired()) {
      return !BeeUtils.isEmpty(value);
    } else {
      return true;
    }
  }
}
