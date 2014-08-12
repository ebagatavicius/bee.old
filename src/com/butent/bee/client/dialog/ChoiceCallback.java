package com.butent.bee.client.dialog;

public abstract class ChoiceCallback {

  public ChoiceCallback() {
    super();
  }

  public void onCancel() {
  }

  public abstract void onSuccess(int value);

  public void onTimeout() {
  }
}
