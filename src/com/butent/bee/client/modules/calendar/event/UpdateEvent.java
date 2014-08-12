package com.butent.bee.client.modules.calendar.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import com.butent.bee.client.modules.calendar.Appointment;
import com.butent.bee.shared.time.DateTime;

public final class UpdateEvent extends GwtEvent<UpdateEvent.Handler> {

  public interface Handler extends EventHandler {
    void onUpdate(UpdateEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static boolean fire(HasUpdateHandlers source, Appointment appointment,
      DateTime newStart, DateTime newEnd, int oldColumnIndex, int newColumnIndex) {
    UpdateEvent event = new UpdateEvent(appointment, newStart, newEnd, oldColumnIndex,
        newColumnIndex);
    source.fireEvent(event);
    return !event.isCanceled();
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final Appointment appointment;

  private final DateTime newStart;
  private final DateTime newEnd;

  private final int oldColumnIndex;
  private final int newColumnIndex;

  private boolean canceled;

  private UpdateEvent(Appointment appointment, DateTime newStart, DateTime newEnd,
      int oldColumnIndex, int newColumnIndex) {
    super();
    this.appointment = appointment;
    this.newStart = newStart;
    this.newEnd = newEnd;
    this.oldColumnIndex = oldColumnIndex;
    this.newColumnIndex = newColumnIndex;
  }

  public Appointment getAppointment() {
    return appointment;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public int getNewColumnIndex() {
    return newColumnIndex;
  }

  public DateTime getNewEnd() {
    return newEnd;
  }

  public DateTime getNewStart() {
    return newStart;
  }

  public int getOldColumnIndex() {
    return oldColumnIndex;
  }

  public boolean isCanceled() {
    return canceled;
  }

  public void setCanceled(boolean canceled) {
    this.canceled = canceled;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onUpdate(this);
  }
}
