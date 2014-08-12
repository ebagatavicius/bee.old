package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.screen.HasDomain;
import com.butent.bee.client.ui.IdentifiableWidget;

public class ActiveWidgetChangeEvent extends GwtEvent<ActiveWidgetChangeEvent.Handler> implements
    HasDomain {

  public interface Handler extends EventHandler {
    void onActiveWidgetChange(ActiveWidgetChangeEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fireActivate(HasActiveWidgetChangeHandlers source, IdentifiableWidget widget) {
    String widgetId = (widget == null) ? null : widget.getId();
    Domain domain = (widget instanceof HasDomain) ? ((HasDomain) widget).getDomain() : null;

    source.fireEvent(new ActiveWidgetChangeEvent(widgetId, true, domain));
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final String widgetId;
  private final boolean active;

  private final Domain domain;

  public ActiveWidgetChangeEvent(String widgetId, boolean active) {
    this(widgetId, active, null);
  }

  public ActiveWidgetChangeEvent(String widgetId, boolean active, Domain domain) {
    super();
    this.widgetId = widgetId;
    this.active = active;
    this.domain = domain;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public Domain getDomain() {
    return domain;
  }

  public String getWidgetId() {
    return widgetId;
  }

  public boolean isActive() {
    return active;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onActiveWidgetChange(this);
  }
}
