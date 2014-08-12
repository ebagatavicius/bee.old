package com.butent.bee.client.view.edit;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import com.butent.bee.shared.State;

public class EditFormEvent extends GwtEvent<EditFormEvent.Handler> {

  public interface Handler extends EventHandler {
    void onEditForm(EditFormEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final State state;
  private final boolean popup;

  public EditFormEvent(State state, boolean popup) {
    super();
    this.state = state;
    this.popup = popup;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public State getState() {
    return state;
  }

  public boolean isCanceled() {
    return State.CANCELED.equals(getState());
  }

  public boolean isChanged() {
    return State.CHANGED.equals(getState());
  }

  public boolean isClosing() {
    return State.CLOSED.equals(getState());
  }

  public boolean isOpening() {
    return State.OPEN.equals(getState());
  }

  public boolean isPending() {
    return State.PENDING.equals(getState());
  }

  public boolean isPopup() {
    return popup;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onEditForm(this);
  }
}
