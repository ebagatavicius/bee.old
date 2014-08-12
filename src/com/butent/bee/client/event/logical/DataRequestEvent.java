package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.ui.NavigationOrigin;

public class DataRequestEvent extends GwtEvent<DataRequestEvent.Handler> {

  public interface Handler extends EventHandler {
    void onDataRequest(DataRequestEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(HasHandlers source, NavigationOrigin origin) {
    Assert.notNull(source);
    source.fireEvent(new DataRequestEvent(origin));
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final NavigationOrigin origin;

  public DataRequestEvent(NavigationOrigin origin) {
    super();
    this.origin = origin;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public NavigationOrigin getOrigin() {
    return origin;
  }

  public boolean isKeyboard() {
    return NavigationOrigin.KEYBOARD.equals(origin);
  }

  public boolean isScrolling() {
    return NavigationOrigin.SCROLLER.equals(origin);
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onDataRequest(this);
  }
}
