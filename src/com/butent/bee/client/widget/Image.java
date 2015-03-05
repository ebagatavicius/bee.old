package com.butent.bee.client.widget;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.HasLoadHandlers;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.utils.HasCommand;
import com.butent.bee.shared.BeeConst;

import elemental.html.ImageElement;
import elemental.js.html.JsImageElement;

/**
 * Implements an image holding user interface component, that displays the image at a given URL.
 */
public class Image extends CustomWidget implements EnablableWidget, HasCommand,
    HasAllMouseHandlers, HasLoadHandlers {

  private ScheduledCommand command;

  private boolean enabled = true;
  private String styleDisabled;

  public Image() {
    super(Document.get().createImageElement());
  }

  public Image(ImageResource resource) {
    this();
    setResource(resource);
  }

  public Image(ImageResource resource, ScheduledCommand cmnd) {
    this(resource);
    setCommand(cmnd);
  }

  public Image(ImageResource resource, ScheduledCommand cmnd, String styleDisabled) {
    this(resource, cmnd);
    this.styleDisabled = styleDisabled;
  }

  public Image(String url) {
    this();
    setUrl(url);
  }

  @Override
  public HandlerRegistration addLoadHandler(LoadHandler handler) {
    return addDomHandler(handler, LoadEvent.getType());
  }

  @Override
  public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
    return addDomHandler(handler, MouseDownEvent.getType());
  }

  @Override
  public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
    return addDomHandler(handler, MouseMoveEvent.getType());
  }

  @Override
  public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
    return addDomHandler(handler, MouseOutEvent.getType());
  }

  @Override
  public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
    return addDomHandler(handler, MouseOverEvent.getType());
  }

  @Override
  public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
    return addDomHandler(handler, MouseUpEvent.getType());
  }

  @Override
  public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
    return addDomHandler(handler, MouseWheelEvent.getType());
  }

  @Override
  public ScheduledCommand getCommand() {
    return command;
  }

  public int getHeight() {
    return getImageElement().getHeight();
  }

  @Override
  public String getIdPrefix() {
    return "img";
  }

  public int getNaturalHeight() {
    return getImageElement().getNaturalHeight();
  }

  public int getNaturalWidth() {
    return getImageElement().getNaturalWidth();
  }

  public String getUrl() {
    return getImageElement().getSrc();
  }

  public int getWidth() {
    return getImageElement().getWidth();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (isEnabled()) {
      if (EventUtils.isClick(event) && getCommand() != null) {
        getCommand().execute();
      }
      super.onBrowserEvent(event);
    }
  }

  public void setAlt(String alt) {
    getImageElement().setAlt(alt);
  }

  @Override
  public void setCommand(ScheduledCommand command) {
    this.command = command;
    if (command != null) {
      sinkEvents(Event.ONCLICK);
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (this.enabled == enabled) {
      return;
    }
    this.enabled = enabled;

    if (styleDisabled != null) {
      if (enabled) {
        getElement().removeClassName(styleDisabled);
      } else {
        getElement().addClassName(styleDisabled);
      }
    }
  }

  public void setResource(ImageResource resource) {
    getImageElement().setSrc(resource.getSafeUri().asString());
  }

  public void setUrl(String url) {
    getImageElement().setSrc(url);
  }

  @Override
  protected void init() {
    super.init();
    addStyleName(BeeConst.CSS_CLASS_PREFIX + "Image");
  }

  private ImageElement getImageElement() {
    return (JsImageElement) getElement().cast();
  }
}
