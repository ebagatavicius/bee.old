package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsRow;

public class ParentRowEvent extends Event<ParentRowEvent.Handler> implements HasViewName {

  public interface Handler extends EventHandler {
    void onParentRow(ParentRowEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static HandlerRegistration register(EventBus eventBus, Object source, Handler handler) {
    Assert.notNull(eventBus);
    Assert.notNull(handler);
    return eventBus.addHandlerToSource(TYPE, source, handler);
  }

  private final String viewName;
  private final IsRow row;
  private final boolean enabled;

  public ParentRowEvent(String viewName, IsRow row, boolean enabled) {
    super();
    this.viewName = viewName;
    this.row = row;
    this.enabled = enabled;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public IsRow getRow() {
    return row;
  }

  public Long getRowId() {
    return (row == null) ? null : row.getId();
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  public boolean isEnabled() {
    return enabled;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onParentRow(this);
  }
}
