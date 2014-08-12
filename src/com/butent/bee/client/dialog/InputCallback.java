package com.butent.bee.client.dialog;

import com.butent.bee.client.view.form.CloseCallback;

public abstract class InputCallback {

  public String getErrorMessage() {
    return null;
  }

  public void onAdd() {
  }

  public void onCancel() {
  }

  public void onClose(CloseCallback closeCallback) {
    closeCallback.onClose();
  }

  public void onDelete(DialogBox dialog) {
    dialog.close();
  }

  public abstract void onSuccess();
}
