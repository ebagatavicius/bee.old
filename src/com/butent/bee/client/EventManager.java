package com.butent.bee.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.Event.Type;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.web.bindery.event.shared.SimpleEventBus;

import com.butent.bee.client.event.logical.BookmarkEvent;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.RowActionEvent;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Locality;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.FiresModificationEvents;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.HandlesDeleteEvents;
import com.butent.bee.shared.data.event.HandlesUpdateEvents;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.websocket.messages.ModificationMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * creates handlers for events such as mouse and keyboard actions or server responses.
 */

public class EventManager implements FiresModificationEvents {

  private final EventBus priorBus;
  private final EventBus eventBus;

  private HandlerRegistration exitRegistry;

  public EventManager() {
    this.priorBus = new SimpleEventBus();
    this.eventBus = new SimpleEventBus();
  }

  public <H> HandlerRegistration addHandler(Type<H> type, H handler, boolean prior) {
    Assert.notNull(type);
    Assert.notNull(handler);

    return getBus(prior).addHandler(type, handler);
  }

  public <H> HandlerRegistration addHandlerToSource(Type<H> type, Object source, H handler,
      boolean prior) {
    Assert.notNull(type);
    Assert.notNull(source);
    Assert.notNull(handler);

    return getBus(prior).addHandlerToSource(type, source, handler);
  }

  public void fireEvent(Event<?> event) {
    Assert.notNull(event);

    priorBus.fireEvent(event);
    eventBus.fireEvent(event);
  }

  public void fireEventFromSource(Event<?> event, Object source) {
    Assert.notNull(event);
    Assert.notNull(source);

    priorBus.fireEventFromSource(event, source);
    eventBus.fireEventFromSource(event, source);
  }

  @Override
  public void fireModificationEvent(ModificationEvent<?> event, Locality locality) {
    fireEvent(event);

    if (locality == Locality.ENTANGLED && Endpoint.isOpen()) {
      Endpoint.send(new ModificationMessage(event));
    }
  }

  public HandlerRegistration registerBookmarkHandler(BookmarkEvent.Handler handler, boolean prior) {
    return BookmarkEvent.register(getBus(prior), handler);
  }

  public HandlerRegistration registerCellUpdateHandler(CellUpdateEvent.Handler handler,
      boolean prior) {
    return CellUpdateEvent.register(getBus(prior), handler);
  }

  public HandlerRegistration registerDataChangeHandler(DataChangeEvent.Handler handler,
      boolean prior) {
    return DataChangeEvent.register(getBus(prior), handler);
  }

  public Collection<HandlerRegistration> registerDataHandler(HandlesAllDataEvents handler,
      boolean prior) {
    Assert.notNull(handler);

    List<HandlerRegistration> registry = new ArrayList<>();

    registry.add(registerCellUpdateHandler(handler, prior));
    registry.add(registerMultiDeleteHandler(handler, prior));
    registry.add(registerRowDeleteHandler(handler, prior));
    registry.add(registerRowInsertHandler(handler, prior));
    registry.add(registerRowUpdateHandler(handler, prior));

    registry.add(registerDataChangeHandler(handler, prior));

    return registry;
  }

  public Collection<HandlerRegistration> registerDeleteHandler(HandlesDeleteEvents handler,
      boolean prior) {
    Assert.notNull(handler);

    List<HandlerRegistration> registry = new ArrayList<>();
    registry.add(registerRowDeleteHandler(handler, prior));
    registry.add(registerMultiDeleteHandler(handler, prior));

    return registry;
  }

  public void registerExitHandler(ClosingHandler handler) {
    Assert.notNull(handler);

    removeExitHandler();
    this.exitRegistry = Window.addWindowClosingHandler(handler);
  }

  public HandlerRegistration registerMultiDeleteHandler(MultiDeleteEvent.Handler handler,
      boolean prior) {
    return MultiDeleteEvent.register(getBus(prior), handler);
  }

  public HandlerRegistration registerParentRowHandler(Object source,
      ParentRowEvent.Handler handler, boolean prior) {
    return ParentRowEvent.register(getBus(prior), source, handler);
  }

  public HandlerRegistration registerRowActionHandler(RowActionEvent.Handler handler,
      boolean prior) {
    return RowActionEvent.register(getBus(prior), handler);
  }

  public HandlerRegistration registerRowDeleteHandler(RowDeleteEvent.Handler handler,
      boolean prior) {
    return RowDeleteEvent.register(getBus(prior), handler);
  }

  public HandlerRegistration registerRowInsertHandler(RowInsertEvent.Handler handler,
      boolean prior) {
    return RowInsertEvent.register(getBus(prior), handler);
  }

  public HandlerRegistration registerRowTransformHandler(RowTransformEvent.Handler handler,
      boolean prior) {
    return RowTransformEvent.register(getBus(prior), handler);
  }

  public HandlerRegistration registerRowUpdateHandler(RowUpdateEvent.Handler handler,
      boolean prior) {
    return RowUpdateEvent.register(getBus(prior), handler);
  }

  public Collection<HandlerRegistration> registerUpdateHandler(HandlesUpdateEvents handler,
      boolean prior) {
    Assert.notNull(handler);

    List<HandlerRegistration> registry = new ArrayList<>();
    registry.add(registerCellUpdateHandler(handler, prior));
    registry.add(registerRowUpdateHandler(handler, prior));

    return registry;
  }

  public void removeExitHandler() {
    if (this.exitRegistry != null) {
      this.exitRegistry.removeHandler();
      this.exitRegistry = null;
    }
  }

  private EventBus getBus(boolean prior) {
    return prior ? priorBus : eventBus;
  }
}
