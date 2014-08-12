package com.butent.bee.client.event.logical;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public final class CatchEvent<T> extends GwtEvent<CatchEvent.CatchHandler<T>> {

  public interface HasCatchHandlers<T> extends HasHandlers {
    HandlerRegistration addCatchHandler(CatchHandler<T> handler);
  }

  public interface CatchHandler<T> extends EventHandler {
    void onCatch(CatchEvent<T> event);
  }

  private static final Type<CatchHandler<?>> TYPE = new Type<>();

  public static <T> CatchEvent<T> fire(HasCatchHandlers<T> source, T packet, T destination,
      ScheduledCommand scheduled) {

    CatchEvent<T> event = null;

    if (TYPE != null) {
      event = new CatchEvent<>(packet, destination, scheduled);
      source.fireEvent(event);
    }
    return event;
  }

  public static Type<CatchHandler<?>> getType() {
    return TYPE;
  }

  private final T packet;
  private final T destination;
  private final ScheduledCommand scheduled;
  private boolean consumed;

  private CatchEvent(T packet, T destination, ScheduledCommand scheduled) {
    this.packet = packet;
    this.destination = destination;
    this.scheduled = scheduled;
  }

  public void consume() {
    this.consumed = true;
  }

  public void executeScheduled() {
    if (scheduled != null) {
      scheduled.execute();
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  public Type<CatchHandler<T>> getAssociatedType() {
    return (Type) TYPE;
  }

  public T getDestination() {
    return destination;
  }

  public T getPacket() {
    return packet;
  }

  public boolean isConsumed() {
    return consumed;
  }

  @Override
  protected void dispatch(CatchHandler<T> handler) {
    handler.onCatch(this);
  }
}
