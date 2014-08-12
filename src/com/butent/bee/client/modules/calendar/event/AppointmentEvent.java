package com.butent.bee.client.modules.calendar.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.modules.calendar.Appointment;
import com.butent.bee.client.modules.calendar.CalendarWidget;
import com.butent.bee.shared.State;

public class AppointmentEvent extends Event<AppointmentEvent.Handler> {

  public interface Handler extends EventHandler {
    void onAppointment(AppointmentEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(Appointment appointment, State state) {
    fire(appointment, state, null);
  }

  public static void fire(Appointment appointment, State state, CalendarWidget ignore) {
    BeeKeeper.getBus().fireEvent(new AppointmentEvent(appointment, state, ignore));
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  public static HandlerRegistration register(Handler handler) {
    return BeeKeeper.getBus().addHandler(getType(), handler, false);
  }

  private final Appointment appointment;
  private final State state;
  private final CalendarWidget ignore;

  public AppointmentEvent(Appointment appointment, State state, CalendarWidget ignore) {
    super();
    this.appointment = appointment;
    this.state = state;
    this.ignore = ignore;
  }

  public Appointment getAppointment() {
    return appointment;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public State getState() {
    return state;
  }

  public boolean isNew() {
    return State.CREATED.equals(state);
  }

  public boolean isRelevant(CalendarWidget calendarWidget) {
    return (ignore == null) ? true : !ignore.equals(calendarWidget);
  }

  public boolean isUpdated() {
    return State.CHANGED.equals(state);
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onAppointment(this);
  }
}
