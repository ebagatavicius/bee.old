package com.butent.bee.client.event.logical;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import com.butent.bee.shared.Assert;

public class CloseEvent extends GwtEvent<CloseEvent.Handler> {

  public enum Cause {
    KEYBOARD_ESCAPE, KEYBOARD_SAVE, MOUSE_CLOSE_BOX, MOUSE_OUTSIDE, SCRIPT
  }

  public interface Handler extends EventHandler {
    void onClose(CloseEvent event);
  }

  public interface HasCloseHandlers extends HasHandlers {
    HandlerRegistration addCloseHandler(Handler handler);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(HasCloseHandlers source, Cause cause, Node target) {
    Assert.notNull(source);
    source.fireEvent(new CloseEvent(cause, target));
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final Cause cause;
  private final Node target;

  public CloseEvent(Cause cause, Node target) {
    super();
    this.cause = cause;
    this.target = target;
  }

  public boolean actionCancel() {
    return keyboardEscape() || mouseEvent();
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public Cause getCause() {
    return cause;
  }

  public Node getTarget() {
    return target;
  }

  public boolean isTarget(Element element) {
    if (element == null || target == null) {
      return false;
    } else {
      return element.isOrHasChild(target);
    }
  }

  public boolean keyboardEscape() {
    return Cause.KEYBOARD_ESCAPE.equals(cause);
  }

  public boolean keyboardEvent() {
    return keyboardEscape() || keyboardSave();
  }

  public boolean keyboardSave() {
    return Cause.KEYBOARD_SAVE.equals(cause);
  }

  public boolean mouseCloseBox() {
    return Cause.MOUSE_CLOSE_BOX.equals(cause);
  }

  public boolean mouseEvent() {
    return mouseCloseBox() || mouseOutside();
  }

  public boolean mouseOutside() {
    return Cause.MOUSE_OUTSIDE.equals(cause);
  }

  public boolean script() {
    return Cause.SCRIPT.equals(cause);
  }

  public boolean userCaused() {
    return keyboardEvent() || mouseEvent();
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onClose(this);
  }
}
