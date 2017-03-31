package com.butent.bee.shared.data.event;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.utils.BeeUtils;

public class RowTransformEvent extends Event<RowTransformEvent.Handler> implements DataEvent,
    HasViewName {

  @FunctionalInterface
  public interface Handler {
    void onRowTransform(RowTransformEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static HandlerRegistration register(EventBus eventBus, Handler handler) {
    Assert.notNull(eventBus);
    Assert.notNull(handler);
    return eventBus.addHandler(TYPE, handler);
  }

  private final String viewName;
  private final BeeRow row;

  private String result;

  public RowTransformEvent(String viewName, BeeRow row) {
    this.viewName = viewName;
    this.row = row;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public String getResult() {
    return result;
  }

  public BeeRow getRow() {
    return row;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public boolean hasView(String view) {
    return BeeUtils.same(view, getViewName());
  }

  public void setResult(String result) {
    this.result = result;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRowTransform(this);
  }
}
