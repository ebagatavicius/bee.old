package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RowCountChangeEvent extends GwtEvent<RowCountChangeEvent.Handler> {

  public interface Handler extends EventHandler {
    void onRowCountChange(RowCountChangeEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final int count;

  public RowCountChangeEvent(int count) {
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
    handler.onRowCountChange(this);
  }
}
