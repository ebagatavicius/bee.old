package com.butent.bee.client.dialog;

import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.HasDimensions;
import com.butent.bee.shared.BeeConst;

public class ModalForm extends ModalView {

  private final boolean requiresUnload;

  private boolean wasAttached;
  private boolean pendingUnload;

  public ModalForm(Presenter presenter, HasDimensions dimensions, boolean requiresUnload) {
    super(presenter, BeeConst.CSS_CLASS_PREFIX + "ModalForm", dimensions, true);
    this.requiresUnload = requiresUnload;
  }

  @Override
  public String getIdPrefix() {
    return "modal-form";
  }

  public void open() {
    center();
  }

  public void unload() {
    if (wasAttached) {
      pendingUnload = true;
      if (isShowing()) {
        close();
      } else {
        doDetachChildren();
      }
    }
  }

  @Override
  protected void doAttachChildren() {
    if (!requiresUnload || !wasAttached) {
      super.doAttachChildren();
      wasAttached = true;
    }
  }

  @Override
  protected void doDetachChildren() {
    if (!requiresUnload || pendingUnload) {
      super.doDetachChildren();
      wasAttached = false;
      pendingUnload = false;
    }
  }
}
