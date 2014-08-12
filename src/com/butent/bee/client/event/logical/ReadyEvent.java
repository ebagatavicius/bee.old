package com.butent.bee.client.event.logical;

import com.google.common.collect.Sets;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.IsUnique;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public final class ReadyEvent extends GwtEvent<ReadyEvent.Handler> {

  public interface Handler extends EventHandler {
    void onReady(ReadyEvent event);
  }

  public interface HasReadyHandlers extends HasHandlers, IsUnique {
    HandlerRegistration addReadyHandler(Handler handler);
  }

  private static final BeeLogger logger = LogUtils.getLogger(ReadyEvent.class);

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(HasReadyHandlers source) {
    Assert.notNull(source);
    source.fireEvent(new ReadyEvent());
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  public static boolean maybeDelegate(HasReadyHandlers delegator, HasReadyHandlers delegate) {
    if (delegator != null && delegate != null) {
      delegate(delegator, Sets.newHashSet(delegate));
      return true;

    } else {
      logger.warning("cannot delegate",
          (delegator == null) ? BeeConst.NULL : delegator.getId(),
          (delegate == null) ? BeeConst.NULL : delegate.getId());
      return false;
    }
  }

  public static boolean maybeDelegate(Widget parent) {
    if (parent instanceof HasReadyHandlers) {
      Collection<HasReadyHandlers> delegates = getDelegates(parent, false);

      if (!delegates.isEmpty()) {
        delegate((HasReadyHandlers) parent, delegates);
        return true;
      }
    }
    return false;
  }

  private static void delegate(final HasReadyHandlers delegator,
      Collection<HasReadyHandlers> delegates) {

    final Map<String, HandlerRegistration> registry = new HashMap<>();

    for (HasReadyHandlers delegate : delegates) {
      HandlerRegistration registration = delegate.addReadyHandler(new ReadyEvent.Handler() {
        @Override
        public void onReady(ReadyEvent event) {
          if (event.getSource() instanceof IsUnique) {
            HandlerRegistration hr = registry.remove(((IsUnique) event.getSource()).getId());
            if (hr != null) {
              hr.removeHandler();

              if (registry.isEmpty()) {
                fire(delegator);
              }
            }
          }
        }
      });

      if (registration != null) {
        registry.put(delegate.getId(), registration);
      }
    }
  }

  private static Collection<HasReadyHandlers> getDelegates(Widget parent, boolean include) {
    Collection<HasReadyHandlers> delegates = new HashSet<>();

    if (parent instanceof HasReadyHandlers && include) {
      delegates.add((HasReadyHandlers) parent);

    } else if (parent instanceof HasWidgets) {
      for (Widget child : (HasWidgets) parent) {
        delegates.addAll(getDelegates(child, true));
      }

    } else if (parent instanceof HasOneWidget) {
      delegates.addAll(getDelegates(((HasOneWidget) parent).getWidget(), true));
    }

    return delegates;
  }

  private ReadyEvent() {
    super();
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onReady(this);
  }
}
