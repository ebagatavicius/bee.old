package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Handles selection count change event.
 */

public class SelectionCountChangeEvent extends GwtEvent<SelectionCountChangeEvent.Handler> {

  /**
   * Requires implementing classes to have a method to handle selection count changes.
   */

  @FunctionalInterface
  public interface Handler extends EventHandler {
    void onSelectionCountChange(SelectionCountChangeEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final int count;

  public SelectionCountChangeEvent(int count) {
    super();
    this.count = count;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public int getCount() {
    return count;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onSelectionCountChange(this);
  }
}
