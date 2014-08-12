package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import com.butent.bee.shared.ui.NavigationOrigin;

public class ScopeChangeEvent extends GwtEvent<ScopeChangeEvent.Handler> {

  public interface Handler extends EventHandler {
    void onScopeChange(ScopeChangeEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final int start;
  private final int length;
  private final int total;
  private final NavigationOrigin origin;

  public ScopeChangeEvent(int start, int length, int total, NavigationOrigin origin) {
    super();

    this.start = start;
    this.length = length;
    this.total = total;
    this.origin = origin;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public int getLength() {
    return length;
  }

  public NavigationOrigin getOrigin() {
    return origin;
  }

  public int getStart() {
    return start;
  }

  public int getTotal() {
    return total;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onScopeChange(this);
  }
}
