package com.butent.bee.client.event;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.CanPlayThroughEvent;
import com.google.gwt.event.dom.client.CanPlayThroughHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.DragEndEvent;
import com.google.gwt.event.dom.client.DragEndHandler;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragEnterHandler;
import com.google.gwt.event.dom.client.DragEvent;
import com.google.gwt.event.dom.client.DragHandler;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragLeaveHandler;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DragOverHandler;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DragStartHandler;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.DropHandler;
import com.google.gwt.event.dom.client.EndedEvent;
import com.google.gwt.event.dom.client.EndedHandler;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.GestureChangeEvent;
import com.google.gwt.event.dom.client.GestureChangeHandler;
import com.google.gwt.event.dom.client.GestureEndEvent;
import com.google.gwt.event.dom.client.GestureEndHandler;
import com.google.gwt.event.dom.client.GestureStartEvent;
import com.google.gwt.event.dom.client.GestureStartHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.LoseCaptureEvent;
import com.google.gwt.event.dom.client.LoseCaptureHandler;
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
import com.google.gwt.event.dom.client.ProgressEvent;
import com.google.gwt.event.dom.client.ProgressHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.dom.client.TouchCancelEvent;
import com.google.gwt.event.dom.client.TouchCancelHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.shared.Assert;

public final class Binder {

  private static boolean initialized;

  private static JavaScriptObject dispatcher;

  public static HandlerRegistration addBlurHandler(Widget widget, BlurHandler handler) {
    Assert.notNull(widget, "addBlurHandler: widget is null");
    Assert.notNull(handler, "addBlurHandler: handler is null");

    return widget.addDomHandler(handler, BlurEvent.getType());
  }

  public static HandlerRegistration addCanPlayThroughHandler(Widget widget,
      CanPlayThroughHandler handler) {
    Assert.notNull(widget, "addCanPlayThroughCanPlayThroughHandler: widget is null");
    Assert.notNull(handler, "addCanPlayThroughHandler: handler is null");

    return widget.addDomHandler(handler, CanPlayThroughEvent.getType());
  }

  public static HandlerRegistration addChangeHandler(Widget widget, ChangeHandler handler) {
    Assert.notNull(widget, "addChangeHandler: widget is null");
    Assert.notNull(handler, "addChangeHandler: handler is null");

    return widget.addDomHandler(handler, ChangeEvent.getType());
  }

  public static HandlerRegistration addClickHandler(Widget widget, ClickHandler handler) {
    Assert.notNull(widget, "addClickHandler: widget is null");
    Assert.notNull(handler, "addClickHandler: handler is null");

    return widget.addDomHandler(handler, ClickEvent.getType());
  }

  public static HandlerRegistration addContextMenuHandler(Widget widget,
      ContextMenuHandler handler) {
    Assert.notNull(widget, "addContextMenuHandler: widget is null");
    Assert.notNull(handler, "addContextMenuHandler: handler is null");

    return widget.addDomHandler(handler, ContextMenuEvent.getType());
  }

  public static HandlerRegistration addDoubleClickHandler(Widget widget,
      DoubleClickHandler handler) {
    Assert.notNull(widget, "addDoubleClickHandler: widget is null");
    Assert.notNull(handler, "addDoubleClickHandler: handler is null");

    return widget.addDomHandler(handler, DoubleClickEvent.getType());
  }

  public static HandlerRegistration addDragEndHandler(Widget widget, DragEndHandler handler) {
    Assert.notNull(widget, "addDragEndHandler: widget is null");
    Assert.notNull(handler, "addDragEndHandler: handler is null");

    return widget.addDomHandler(handler, DragEndEvent.getType());
  }

  public static HandlerRegistration addDragEnterHandler(Widget widget, DragEnterHandler handler) {
    Assert.notNull(widget, "addDragEnterHandler: widget is null");
    Assert.notNull(handler, "addDragEnterHandler: handler is null");

    return widget.addDomHandler(handler, DragEnterEvent.getType());
  }

  public static HandlerRegistration addDragHandler(Widget widget, DragHandler handler) {
    Assert.notNull(widget, "addDragHandler: widget is null");
    Assert.notNull(handler, "addDragHandler: handler is null");

    return widget.addDomHandler(handler, DragEvent.getType());
  }

  public static HandlerRegistration addDragLeaveHandler(Widget widget, DragLeaveHandler handler) {
    Assert.notNull(widget, "addDragLeaveHandler: widget is null");
    Assert.notNull(handler, "addDragLeaveHandler: handler is null");

    return widget.addDomHandler(handler, DragLeaveEvent.getType());
  }

  public static HandlerRegistration addDragOverHandler(Widget widget, DragOverHandler handler) {
    Assert.notNull(widget, "addDragOverHandler: widget is null");
    Assert.notNull(handler, "addDragOverHandler: handler is null");

    return widget.addDomHandler(handler, DragOverEvent.getType());
  }

  public static HandlerRegistration addDragStartHandler(Widget widget, DragStartHandler handler) {
    Assert.notNull(widget, "addDragStartHandler: widget is null");
    Assert.notNull(handler, "addDragStartHandler: handler is null");

    return widget.addDomHandler(handler, DragStartEvent.getType());
  }

  public static HandlerRegistration addDropHandler(Widget widget, DropHandler handler) {
    Assert.notNull(widget, "addDropHandler: widget is null");
    Assert.notNull(handler, "addDropHandler: handler is null");

    return widget.addDomHandler(handler, DropEvent.getType());
  }

  public static HandlerRegistration addEndedHandler(Widget widget, EndedHandler handler) {
    Assert.notNull(widget, "addEndedHandler: widget is null");
    Assert.notNull(handler, "addEndedHandler: handler is null");

    return widget.addDomHandler(handler, EndedEvent.getType());
  }

  public static HandlerRegistration addErrorHandler(Widget widget, ErrorHandler handler) {
    Assert.notNull(widget, "addErrorHandler: widget is null");
    Assert.notNull(handler, "addErrorHandler: handler is null");

    return widget.addDomHandler(handler, ErrorEvent.getType());
  }

  public static HandlerRegistration addFocusHandler(Widget widget, FocusHandler handler) {
    Assert.notNull(widget, "addFocusHandler: widget is null");
    Assert.notNull(handler, "addFocusHandler: handler is null");

    return widget.addDomHandler(handler, FocusEvent.getType());
  }

  public static HandlerRegistration addGestureChangeHandler(Widget widget,
      GestureChangeHandler handler) {
    Assert.notNull(widget, "addGestureChangeHandler: widget is null");
    Assert.notNull(handler, "addGestureChangeHandler: handler is null");

    return widget.addDomHandler(handler, GestureChangeEvent.getType());
  }

  public static HandlerRegistration addGestureEndHandler(Widget widget, GestureEndHandler handler) {
    Assert.notNull(widget, "addGestureEndHandler: widget is null");
    Assert.notNull(handler, "addGestureEndHandler: handler is null");

    return widget.addDomHandler(handler, GestureEndEvent.getType());
  }

  public static HandlerRegistration addGestureStartHandler(Widget widget,
      GestureStartHandler handler) {
    Assert.notNull(widget, "addGestureStartHandler: widget is null");
    Assert.notNull(handler, "addGestureStartHandler: handler is null");

    return widget.addDomHandler(handler, GestureStartEvent.getType());
  }

  public static HandlerRegistration addInputHandler(Widget widget, InputHandler handler) {
    Assert.notNull(handler, "addInputHandler: handler is null");

    sinkInput(widget);
    return widget.addHandler(handler, InputEvent.getType());
  }

  public static HandlerRegistration addKeyDownHandler(Widget widget, KeyDownHandler handler) {
    Assert.notNull(widget, "addKeyDownHandler: widget is null");
    Assert.notNull(handler, "addKeyDownHandler: handler is null");

    return widget.addDomHandler(handler, KeyDownEvent.getType());
  }

  public static HandlerRegistration addKeyPressHandler(Widget widget, KeyPressHandler handler) {
    Assert.notNull(widget, "addKeyPressHandler: widget is null");
    Assert.notNull(handler, "addKeyPressHandler: handler is null");

    return widget.addDomHandler(handler, KeyPressEvent.getType());
  }

  public static HandlerRegistration addKeyUpHandler(Widget widget, KeyUpHandler handler) {
    Assert.notNull(widget, "addKeyUpHandler: widget is null");
    Assert.notNull(handler, "addKeyUpHandler: handler is null");

    return widget.addDomHandler(handler, KeyUpEvent.getType());
  }

  public static HandlerRegistration addLoadHandler(Widget widget, LoadHandler handler) {
    Assert.notNull(widget, "addLoadHandler: widget is null");
    Assert.notNull(handler, "addLoadHandler: handler is null");

    return widget.addDomHandler(handler, LoadEvent.getType());
  }

  public static HandlerRegistration addLoseCaptureHandler(Widget widget,
      LoseCaptureHandler handler) {
    Assert.notNull(widget, "addLoseCaptureHandler: widget is null");
    Assert.notNull(handler, "addLoseCaptureHandler: handler is null");

    return widget.addDomHandler(handler, LoseCaptureEvent.getType());
  }

  public static HandlerRegistration addMouseDownHandler(Widget widget, MouseDownHandler handler) {
    Assert.notNull(widget, "addMouseDownHandler: widget is null");
    Assert.notNull(handler, "addMouseDownHandler: handler is null");

    return widget.addDomHandler(handler, MouseDownEvent.getType());
  }

  public static HandlerRegistration addMouseMoveHandler(Widget widget, MouseMoveHandler handler) {
    Assert.notNull(widget, "addMouseMoveHandler: widget is null");
    Assert.notNull(handler, "addMouseMoveHandler: handler is null");

    return widget.addDomHandler(handler, MouseMoveEvent.getType());
  }

  public static HandlerRegistration addMouseOutHandler(Widget widget, MouseOutHandler handler) {
    Assert.notNull(widget, "addMouseOutHandler: widget is null");
    Assert.notNull(handler, "addMouseOutHandler: handler is null");

    return widget.addDomHandler(handler, MouseOutEvent.getType());
  }

  public static HandlerRegistration addMouseOverHandler(Widget widget, MouseOverHandler handler) {
    Assert.notNull(widget, "addMouseOverHandler: widget is null");
    Assert.notNull(handler, "addMouseOverHandler: handler is null");

    return widget.addDomHandler(handler, MouseOverEvent.getType());
  }

  public static HandlerRegistration addMouseUpHandler(Widget widget, MouseUpHandler handler) {
    Assert.notNull(widget, "addMouseUpHandler: widget is null");
    Assert.notNull(handler, "addMouseUpHandler: handler is null");

    return widget.addDomHandler(handler, MouseUpEvent.getType());
  }

  public static HandlerRegistration addMouseWheelHandler(Widget widget, MouseWheelHandler handler) {
    Assert.notNull(widget, "addMouseWheelHandler: widget is null");
    Assert.notNull(handler, "addMouseWheelHandler: handler is null");

    return widget.addDomHandler(handler, MouseWheelEvent.getType());
  }

  public static HandlerRegistration addProgressHandler(Widget widget, ProgressHandler handler) {
    Assert.notNull(widget, "addProgressHandler: widget is null");
    Assert.notNull(handler, "addProgressHandler: handler is null");

    return widget.addDomHandler(handler, ProgressEvent.getType());
  }

  public static HandlerRegistration addScrollHandler(Widget widget, ScrollHandler handler) {
    Assert.notNull(widget, "addScrollHandler: widget is null");
    Assert.notNull(handler, "addScrollHandler: handler is null");

    return widget.addDomHandler(handler, ScrollEvent.getType());
  }

  public static HandlerRegistration addTouchCancelHandler(Widget widget,
      TouchCancelHandler handler) {
    Assert.notNull(widget, "addTouchCancelHandler: widget is null");
    Assert.notNull(handler, "addTouchCancelHandler: handler is null");

    return widget.addDomHandler(handler, TouchCancelEvent.getType());
  }

  public static HandlerRegistration addTouchEndHandler(Widget widget, TouchEndHandler handler) {
    Assert.notNull(widget, "addTouchEndHandler: widget is null");
    Assert.notNull(handler, "addTouchEndHandler: handler is null");

    return widget.addDomHandler(handler, TouchEndEvent.getType());
  }

  public static HandlerRegistration addTouchMoveHandler(Widget widget, TouchMoveHandler handler) {
    Assert.notNull(widget, "addTouchMoveHandler: widget is null");
    Assert.notNull(handler, "addTouchMoveHandler: handler is null");

    return widget.addDomHandler(handler, TouchMoveEvent.getType());
  }

  public static HandlerRegistration addTouchStartHandler(Widget widget, TouchStartHandler handler) {
    Assert.notNull(widget, "addTouchStartHandler: widget is null");
    Assert.notNull(handler, "addTouchStartHandler: handler is null");

    return widget.addDomHandler(handler, TouchStartEvent.getType());
  }

  public static void sinkInput(Element elem) {
    Assert.notNull(elem, "sinkInput: element is null");
    maybeInitialize();
    sinkInputImpl(elem);
  }

  public static void sinkInput(UIObject obj) {
    Assert.notNull(obj, "sinkInput: object is null");
    sinkInput(obj.getElement());
  }

//@formatter:off
  // CHECKSTYLE:OFF
  private static native void initDispatcher() /*-{
    @com.butent.bee.client.event.Binder::dispatcher = $entry(function(evt) {
      var listener, curElem = this;
      while (curElem && !(listener = curElem.__listener)) {
        curElem = curElem.parentNode;
      }
      if (curElem && curElem.nodeType != 1) {
        curElem = null;
      }
      if (listener) {
        @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/user/client/EventListener;)(evt, curElem, listener);
      }
    });
  }-*/;
  // CHECKSTYLE:ON
//@formatter:on

  private static void maybeInitialize() {
    if (!initialized) {
      initDispatcher();
      initialized = true;
    }
  }

//@formatter:off
  private static native void sinkInputImpl(Element elem) /*-{
    elem.oninput = @com.butent.bee.client.event.Binder::dispatcher;
  }-*/;
//@formatter:on

  private Binder() {
  }
}
