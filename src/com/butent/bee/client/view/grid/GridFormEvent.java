package com.butent.bee.client.view.grid;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import com.butent.bee.shared.State;

public class GridFormEvent extends GwtEvent<GridFormEvent.Handler> {

  @FunctionalInterface
  public interface Handler extends EventHandler {
    void onGridForm(GridFormEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Type<Handler> getType() {
    return TYPE;
  }

  private GridFormKind kind;
  private final State state;
  private final boolean popup;

  public GridFormEvent(GridFormKind kind, State state, boolean popup) {
    super();

    this.kind = kind;
    this.state = state;
    this.popup = popup;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public boolean isEdit() {
    return kind == GridFormKind.EDIT;
  }

  public boolean isNewRow() {
    return kind == GridFormKind.NEW_ROW;
  }

  public boolean isClosing() {
    return state == State.CLOSING || state == State.CLOSED || state == State.UNLOADING;
  }

  public boolean isOpening() {
    return state == State.PENDING || state == State.OPEN || state == State.LOADING
        || state == State.LOADED;
  }

  public boolean isPopup() {
    return popup;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onGridForm(this);
  }
}
