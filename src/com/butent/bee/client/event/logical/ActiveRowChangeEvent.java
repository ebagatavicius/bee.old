package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import com.butent.bee.shared.data.IsRow;

/**
 * Handles an event when active row changes in table based user interface components.
 */

public class ActiveRowChangeEvent extends GwtEvent<ActiveRowChangeEvent.Handler> {

  /**
   * Requires implementing classes to have a method to handle active row change event.
   */

  public interface Handler extends EventHandler {
    void onActiveRowChange(ActiveRowChangeEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final IsRow rowValue;

  public ActiveRowChangeEvent(IsRow rowValue) {
    super();
    this.rowValue = rowValue;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public IsRow getRowValue() {
    return rowValue;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onActiveRowChange(this);
  }
}
