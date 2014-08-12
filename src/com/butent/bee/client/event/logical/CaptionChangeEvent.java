package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import com.butent.bee.shared.ui.HasCaption;

public class CaptionChangeEvent extends GwtEvent<CaptionChangeEvent.Handler> implements HasCaption {

  public interface HasCaptionChangeHandlers extends HasHandlers {
    HandlerRegistration addCaptionChangeHandler(Handler handler);
  }

  public interface Handler extends EventHandler {
    void onCaptionChange(CaptionChangeEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(HasCaptionChangeHandlers source, String caption) {
    source.fireEvent(new CaptionChangeEvent(caption));
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final String caption;

  public CaptionChangeEvent(String caption) {
    super();
    this.caption = caption;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onCaptionChange(this);
  }
}
