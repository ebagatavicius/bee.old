package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

import com.butent.bee.shared.data.view.Order;

/**
 * Gets the type of sorting needed to be done and triggers it.
 */

public class SortEvent extends GwtEvent<SortEvent.Handler> {

  /**
   * Requires to have onSort event.
   */
  public interface Handler extends EventHandler {
    void onSort(SortEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static SortEvent fire(HasHandlers source, Order sortList) {
    SortEvent event = new SortEvent(sortList);
    if (TYPE != null) {
      source.fireEvent(event);
    }
    return event;
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final Order order;

  protected SortEvent(Order order) {
    this.order = order;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public Order getOrder() {
    return order;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onSort(this);
  }
}
