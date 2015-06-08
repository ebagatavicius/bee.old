package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import com.butent.bee.shared.Assert;

public final class MutationEvent extends GwtEvent<MutationEvent.Handler> {

  public interface HasMutationHandlers extends HasHandlers {
    HandlerRegistration addMutationHandler(Handler handler);
  }

  public interface Handler extends EventHandler {
    void onMutation(MutationEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(HasMutationHandlers source) {
    Assert.notNull(source);
    source.fireEvent(new MutationEvent());
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private MutationEvent() {
    super();
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onMutation(this);
  }
}
