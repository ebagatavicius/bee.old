package com.butent.bee.client.view.edit;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import com.butent.bee.shared.State;

/**
 * Handles edit stop events for such reasons as error or cancellation.
 */

public class EditStopEvent extends GwtEvent<EditStopEvent.Handler> {

  /**
   * Requires implementing methods to have a method to handle edit stop.
   */

  public interface Handler extends EventHandler {
    void onEditStop(EditStopEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final State state;
  private final String message;

  private final Integer keyCode;
  private final boolean hasModifiers;

  public EditStopEvent(State state) {
    this(state, null);
  }

  public EditStopEvent(State state, String message) {
    this(state, message, null, false);
  }

  public EditStopEvent(State state, Integer keyCode, boolean hasModifiers) {
    this(state, null, keyCode, hasModifiers);
  }

  public EditStopEvent(State state, String message, Integer keyCode, boolean hasModifiers) {
    super();
    this.state = state;
    this.message = message;
    this.keyCode = keyCode;
    this.hasModifiers = hasModifiers;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public Integer getKeyCode() {
    return keyCode;
  }

  public String getMessage() {
    return message;
  }

  public State getState() {
    return state;
  }

  public boolean hasKeyCode() {
    return keyCode != null;
  }

  public boolean hasModifiers() {
    return hasModifiers;
  }

  public boolean isCanceled() {
    return State.CANCELED.equals(getState());
  }

  public boolean isChanged() {
    return State.CHANGED.equals(getState());
  }

  public boolean isClosed() {
    return State.CLOSED.equals(getState());
  }

  public boolean isEdited() {
    return State.EDITED.equals(getState());
  }

  public boolean isError() {
    return State.ERROR.equals(getState());
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onEditStop(this);
  }
}
