package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Consumable;
import com.butent.bee.shared.State;

public final class RenderingEvent extends GwtEvent<RenderingEvent.Handler> implements Consumable {

  public interface Handler extends EventHandler {
    void onRender(RenderingEvent event);
  }

  public interface HasRenderingHandlers extends HasHandlers {
    HandlerRegistration addRenderingHandler(Handler handler);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static RenderingEvent after() {
    return new RenderingEvent(true);
  }

  public static RenderingEvent before() {
    return new RenderingEvent(false);
  }

  public static void fireAfter(HasRenderingHandlers source) {
    Assert.notNull(source);
    source.fireEvent(after());
  }

  public static void fireBefore(HasRenderingHandlers source) {
    Assert.notNull(source);
    source.fireEvent(before());
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final boolean after;

  private State state;
  private boolean consumed;

  private RenderingEvent(boolean after) {
    super();
    this.after = after;
  }

  public void cancel() {
    setState(State.CANCELED);
  }

  public boolean canceled() {
    return getState() == State.CANCELED;
  }

  @Override
  public void consume() {
    setConsumed(true);
  }

  public boolean dataChanged() {
    return getState() == State.CHANGED;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public State getState() {
    return state;
  }

  public boolean isAfter() {
    return after;
  }

  public boolean isBefore() {
    return !after;
  }

  @Override
  public boolean isConsumed() {
    return consumed;
  }

  @Override
  public void setConsumed(boolean consumed) {
    this.consumed = consumed;
  }

  public void setDataChanged() {
    setState(State.CHANGED);
  }

  public void setState(State state) {
    this.state = state;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRender(this);
  }
}
