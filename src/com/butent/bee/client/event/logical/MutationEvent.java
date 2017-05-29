package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.HasOptions;

public final class MutationEvent extends GwtEvent<MutationEvent.Handler> implements HasOptions {

  public interface HasMutationHandlers extends HasHandlers {
    HandlerRegistration addMutationHandler(Handler handler);
  }

  @FunctionalInterface
  public interface Handler extends EventHandler {
    void onMutation(MutationEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(HasMutationHandlers source) {
    Assert.notNull(source);
    source.fireEvent(new MutationEvent());
  }

  public static void fire(HasMutationHandlers source, String options) {
    Assert.notNull(source);

    MutationEvent event = new MutationEvent();
    event.setOptions(options);

    source.fireEvent(event);
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private String options;

  private MutationEvent() {
    super();
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public String getOptions() {
    return options;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onMutation(this);
  }
}
