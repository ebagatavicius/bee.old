package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;

public class VisibilityChangeEvent extends Event<VisibilityChangeEvent.Handler> {

  public interface Handler extends EventHandler {
    void onVisibilityChange(VisibilityChangeEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  private static final Map<String, HandlerRegistration> registry = new HashMap<>();

  public static void hideAndFire(Widget widget) {
    Assert.notNull(widget);
    if (widget.isVisible()) {
      widget.setVisible(false);
      String wId = widget.getElement().getId();
      if (!BeeUtils.isEmpty(wId)) {
        BeeKeeper.getBus().fireEvent(new VisibilityChangeEvent(wId, false));
      }
    }
  }

  public static HandlerRegistration register(Handler handler) {
    return BeeKeeper.getBus().addHandler(TYPE, handler, false);
  }

  public static void register(String key, Handler handler) {
    registry.put(key, register(handler));
  }

  public static void showAndFire(Widget widget) {
    Assert.notNull(widget);
    if (!widget.isVisible()) {
      widget.setVisible(true);
      String wId = widget.getElement().getId();
      if (!BeeUtils.isEmpty(wId)) {
        BeeKeeper.getBus().fireEvent(new VisibilityChangeEvent(wId, true));
      }
    }
  }

  public static void unregister(String key) {
    HandlerRegistration handlerRegistration = registry.remove(key);
    if (handlerRegistration != null) {
      handlerRegistration.removeHandler();
    }
  }

  private final String id;
  private final boolean visible;

  public VisibilityChangeEvent(String id, boolean visible) {
    super();
    this.id = id;
    this.visible = visible;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public String getId() {
    return id;
  }

  public boolean isVisible() {
    return visible;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onVisibilityChange(this);
  }
}
