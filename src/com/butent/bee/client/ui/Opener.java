package com.butent.bee.client.ui;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.shared.Assert;

public final class Opener {

  public static final Opener MODAL = new Opener(true, null);

  public static final Opener NEW_TAB = new Opener(PresenterCallback.SHOW_IN_NEW_TAB);
  private static final Opener UPDATE = new Opener(PresenterCallback.SHOW_IN_ACTIVE_PANEL);

  public static Opener modeless() {
    if (BeeKeeper.getUser().openInNewTab()) {
      return NEW_TAB;
    } else {
      return UPDATE;
    }
  }

  public static Opener relativeTo(Element element) {
    return new Opener(true, element);
  }

  public static Opener relativeTo(UIObject obj) {
    if (obj == null) {
      return MODAL;
    } else {
      return relativeTo(obj.getElement());
    }
  }

  public static Opener with(PresenterCallback callback) {
    Assert.notNull(callback);
    return new Opener(callback);
  }

  private final boolean modal;
  private final Element target;

  private final PresenterCallback presenterCallback;

  private Opener(boolean modal, Element target) {
    this(modal, target, null);
  }

  private Opener(boolean modal, Element target, PresenterCallback presenterCallback) {
    this.modal = modal;
    this.target = target;
    this.presenterCallback = presenterCallback;
  }

  private Opener(PresenterCallback presenterCallback) {
    this(false, null, presenterCallback);
  }

  public PresenterCallback getPresenterCallback() {
    return presenterCallback;
  }

  public Element getTarget() {
    return target;
  }

  public boolean isModal() {
    return modal;
  }
}
