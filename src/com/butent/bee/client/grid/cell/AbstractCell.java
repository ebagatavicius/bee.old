package com.butent.bee.client.grid.cell;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.grid.CellContext;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.EventState;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.ui.CellType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractCell<C> implements HasClickHandlers, HasAllKeyHandlers {

  public static IsRow getEventRow(GwtEvent<?> event) {
    if (event != null && event.getSource() instanceof AbstractCell) {
      return ((AbstractCell<?>) event.getSource()).getEventRow();
    } else {
      return null;
    }
  }

  private Set<String> consumedEvents = new HashSet<>();
  private HandlerManager handlerManager;

  private CellContext eventContext;
  private C eventValue;
  private boolean eventCanceled;

  public AbstractCell() {
    super();
  }

  public AbstractCell(Set<String> eventTypes) {
    this();
    if (eventTypes != null) {
      for (String eventType : eventTypes) {
        addConsumedEvent(eventType);
      }
    }
  }

  public AbstractCell(String eventType) {
    this();
    addConsumedEvent(eventType);
  }

  @Override
  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return addDomHandler(ClickEvent.getType(), handler);
  }

  public <H extends EventHandler> HandlerRegistration addDomHandler(DomEvent.Type<H> type,
      H handler) {
    Assert.notNull(type);
    Assert.notNull(handler);

    addConsumedEvent(type.getName());
    return ensureHandlerManager().addHandler(type, handler);
  }

  @Override
  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return addDomHandler(KeyDownEvent.getType(), handler);
  }

  @Override
  public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
    return addDomHandler(KeyPressEvent.getType(), handler);
  }

  @Override
  public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
    return addDomHandler(KeyUpEvent.getType(), handler);
  }

  public boolean consumesEvent(String eventType) {
    return consumedEvents.contains(eventType);
  }

  @Override
  public void fireEvent(GwtEvent<?> event) {
    if (handlerManager != null) {
      handlerManager.fireEvent(event);
    }
  }

  public CellType getCellType() {
    return CellType.TEXT;
  }

  public Set<String> getConsumedEvents() {
    return consumedEvents;
  }

  public CellContext getEventContext() {
    return eventContext;
  }

  public IsRow getEventRow() {
    return (getEventContext() == null) ? null : getEventContext().getRow();
  }

  public C getEventValue() {
    return eventValue;
  }

  public boolean isEventCanceled() {
    return eventCanceled;
  }

  public EventState onBrowserEvent(CellContext context, Element parent, C value, Event event) {
    if (consumesEvent(event.getType()) && handlerManager != null) {
      setEventContext(context);
      setEventValue(value);

      setEventCanceled(false);

      DomEvent.fireNativeEvent(event, handlerManager, parent);

      setEventContext(null);
      setEventValue(null);

      boolean canceled = isEventCanceled();
      if (canceled) {
        setEventCanceled(false);
      }

      return canceled ? EventState.CANCELED : EventState.PROCESSING;

    } else {
      return EventState.PROCESSING;
    }
  }

  public abstract String render(CellContext context, C value);

  public void setEventCanceled(boolean eventCanceled) {
    this.eventCanceled = eventCanceled;
  }

  private void addConsumedEvent(String eventType) {
    if (!BeeUtils.isEmpty(eventType)) {
      consumedEvents.add(eventType);
    }
  }

  private HandlerManager ensureHandlerManager() {
    if (handlerManager == null) {
      handlerManager = new HandlerManager(this);
    }
    return handlerManager;
  }

  private void setEventContext(CellContext eventContext) {
    this.eventContext = eventContext;
  }

  private void setEventValue(C eventValue) {
    this.eventValue = eventValue;
  }
}
