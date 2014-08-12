package com.butent.bee.client.modules.calendar.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import com.butent.bee.shared.time.DateTime;

public final class TimeBlockClickEvent extends GwtEvent<TimeBlockClickEvent.Handler> {

  public interface Handler extends EventHandler {
    void onTimeBlockClick(TimeBlockClickEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(HasTimeBlockClickHandlers source, DateTime start, Long attendeeId) {
    source.fireEvent(new TimeBlockClickEvent(start, attendeeId));
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final DateTime start;
  private final Long attendeeId;

  private TimeBlockClickEvent(DateTime start, Long attendeeId) {
    super();
    this.start = start;
    this.attendeeId = attendeeId;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public Long getAttendeeId() {
    return attendeeId;
  }

  public DateTime getStart() {
    return start;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onTimeBlockClick(this);
  }
}
