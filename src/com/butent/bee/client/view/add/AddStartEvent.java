package com.butent.bee.client.view.add;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Gets handler type for add start event and registers the handler.
 */

public class AddStartEvent extends GwtEvent<AddStartEvent.Handler> {

  /**
   * Requires implementing classes to have a method for add start event.
   */

  public interface Handler extends EventHandler {
    void onAddStart(AddStartEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final String caption;
  private final boolean popup;

  public AddStartEvent(String caption, boolean popup) {
    super();
    this.caption = caption;
    this.popup = popup;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public String getCaption() {
    return caption;
  }

  public boolean isPopup() {
    return popup;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onAddStart(this);
  }
}
