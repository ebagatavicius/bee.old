package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.shared.Assert;

public final class OpenEvent extends GwtEvent<OpenEvent.Handler> {

  public interface HasOpenHandlers extends HasHandlers {
    HandlerRegistration addOpenHandler(Handler handler);
  }

  public interface Handler extends EventHandler {
    void onOpen(OpenEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(HasOpenHandlers source) {
    Assert.notNull(source);
    source.fireEvent(new OpenEvent());
  }

  public static Handler focus(final Widget target) {
    return new Handler() {
      @Override
      public void onOpen(OpenEvent event) {
        UiHelper.focus(target);
      }
    };
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private OpenEvent() {
    super();
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onOpen(this);
  }
}
