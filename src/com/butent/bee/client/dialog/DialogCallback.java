package com.butent.bee.client.dialog;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public abstract class DialogCallback {

  public static ScheduledCommand getCancelCommand(final Popup popup,
      final DialogCallback callback) {

    return new ScheduledCommand() {
      @Override
      public void execute() {
        if (callback == null) {
          popup.close();
        } else {
          callback.onCancel(popup);
        }
      }
    };
  }

  public static ScheduledCommand getConfirmCommand(final Popup popup,
      final DialogCallback callback) {

    return new ScheduledCommand() {
      @Override
      public void execute() {
        if (callback == null || callback.onConfirm(popup)) {
          popup.close();
        }
      }
    };
  }

  public void onCancel(Popup popup) {
    popup.close();
  }

  public abstract boolean onConfirm(Popup popup);
}
