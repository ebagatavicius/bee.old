package com.butent.bee.client.event;

import com.google.common.collect.Maps;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
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
import com.google.gwt.event.dom.client.DragDropEventBase;
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
import com.google.gwt.event.dom.client.HasAllDragAndDropHandlers;
import com.google.gwt.event.dom.client.HasNativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
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
import com.google.gwt.media.dom.client.MediaError;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.JsFunction;
import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EventUtils {

  private static final BeeLogger logger = LogUtils.getLogger(EventUtils.class);

  public static final String EVENT_TYPE_BLUR = "blur";

  public static final String EVENT_TYPE_CAN_PLAY_THROUGH = "canplaythrough";

  public static final String EVENT_TYPE_CHANGE = "change";

  public static final String EVENT_TYPE_CLICK = "click";

  public static final String EVENT_TYPE_CONTEXT_MENU = "contextmenu";

  public static final String EVENT_TYPE_DBL_CLICK = "dblclick";

  public static final String EVENT_TYPE_DRAG = "drag";
  public static final String EVENT_TYPE_DRAG_END = "dragend";
  public static final String EVENT_TYPE_DRAG_ENTER = "dragenter";
  public static final String EVENT_TYPE_DRAG_LEAVE = "dragleave";
  public static final String EVENT_TYPE_DRAG_OVER = "dragover";
  public static final String EVENT_TYPE_DRAG_START = "dragstart";
  public static final String EVENT_TYPE_DROP = "drop";

  public static final String EVENT_TYPE_ENDED = "ended";

  public static final String EVENT_TYPE_ERROR = "error";

  public static final String EVENT_TYPE_FOCUS = "focus";
  public static final String EVENT_TYPE_FOCUS_IN = "focusin";
  public static final String EVENT_TYPE_FOCUS_OUT = "focusout";

  public static final String EVENT_TYPE_GESTURE_CHANGE = "gesturechange";
  public static final String EVENT_TYPE_GESTURE_END = "gestureend";
  public static final String EVENT_TYPE_GESTURE_START = "gesturestart";

  public static final String EVENT_TYPE_INPUT = "input";

  public static final String EVENT_TYPE_KEY_DOWN = "keydown";
  public static final String EVENT_TYPE_KEY_PRESS = "keypress";
  public static final String EVENT_TYPE_KEY_UP = "keyup";

  public static final String EVENT_TYPE_LOAD = "load";

  public static final String EVENT_TYPE_LOSE_CAPTURE = "losecapture";

  public static final String EVENT_TYPE_MOUSE_DOWN = "mousedown";
  public static final String EVENT_TYPE_MOUSE_MOVE = "mousemove";
  public static final String EVENT_TYPE_MOUSE_OUT = "mouseout";
  public static final String EVENT_TYPE_MOUSE_OVER = "mouseover";
  public static final String EVENT_TYPE_MOUSE_UP = "mouseup";
  public static final String EVENT_TYPE_MOUSE_WHEEL = "mousewheel";

  public static final String EVENT_TYPE_PASTE = "paste";

  public static final String EVENT_TYPE_PROGRESS = "progress";

  public static final String EVENT_TYPE_SCROLL = "scroll";

  public static final String EVENT_TYPE_TOUCH_CANCEL = "touchcancel";
  public static final String EVENT_TYPE_TOUCH_END = "touchend";
  public static final String EVENT_TYPE_TOUCH_MOVE = "touchmove";
  public static final String EVENT_TYPE_TOUCH_START = "touchstart";

  public static final int KEY_INSERT = 45;

  public static final String PROPERTY_DROP_EFFECT = "dropEffect";
  public static final String PROPERTY_EFFECT_ALLOWED = "effectAllowed";

  public static final String EFFECT_NONE = "none";
  public static final String EFFECT_COPY = "copy";
  public static final String EFFECT_MOVE = "move";
  public static final String EFFECT_COPY_MOVE = "copyMove";

  public static final String DEFAULT_DND_DATA_FORMAT = "text/plain";

  private static final Map<String, JsFunction> domHandlers = Maps.newHashMap();

  public static void addClassName(HasNativeEvent ev, String className) {
    Assert.notNull(ev);
    addClassName(ev.getNativeEvent(), className);
  }

  public static void addClassName(NativeEvent ev, String className) {
    Assert.notEmpty(className);
    Element element = getEventTargetElement(ev);
    if (element != null) {
      element.addClassName(className);
    }
  }

  public static boolean addDomHandler(final Widget widget, String type, String body) {
    Assert.notNull(widget);
    Assert.notEmpty(type);
    Assert.notEmpty(body);

    final JsFunction handler = getDomHandler(body);

    if (BeeUtils.same(type, EVENT_TYPE_BLUR)) {
      Binder.addBlurHandler(widget, new BlurHandler() {
        @Override
        public void onBlur(BlurEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_CAN_PLAY_THROUGH)) {
      Binder.addCanPlayThroughHandler(widget, new CanPlayThroughHandler() {
        @Override
        public void onCanPlayThrough(CanPlayThroughEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_CHANGE)) {
      Binder.addChangeHandler(widget, new ChangeHandler() {
        @Override
        public void onChange(ChangeEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_CLICK)) {
      Binder.addClickHandler(widget, new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_CONTEXT_MENU)) {
      Binder.addContextMenuHandler(widget, new ContextMenuHandler() {
        @Override
        public void onContextMenu(ContextMenuEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_DBL_CLICK)) {
      Binder.addDoubleClickHandler(widget, new DoubleClickHandler() {
        @Override
        public void onDoubleClick(DoubleClickEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_DRAG)) {
      Binder.addDragHandler(widget, new DragHandler() {
        @Override
        public void onDrag(DragEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_DRAG_END)) {
      Binder.addDragEndHandler(widget, new DragEndHandler() {
        @Override
        public void onDragEnd(DragEndEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_DRAG_ENTER)) {
      Binder.addDragEnterHandler(widget, new DragEnterHandler() {
        @Override
        public void onDragEnter(DragEnterEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_DRAG_LEAVE)) {
      Binder.addDragLeaveHandler(widget, new DragLeaveHandler() {
        @Override
        public void onDragLeave(DragLeaveEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_DRAG_OVER)) {
      Binder.addDragOverHandler(widget, new DragOverHandler() {
        @Override
        public void onDragOver(DragOverEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_DRAG_START)) {
      Binder.addDragStartHandler(widget, new DragStartHandler() {
        @Override
        public void onDragStart(DragStartEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_DROP)) {
      Binder.addDropHandler(widget, new DropHandler() {
        @Override
        public void onDrop(DropEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_ENDED)) {
      Binder.addEndedHandler(widget, new EndedHandler() {
        @Override
        public void onEnded(EndedEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_ERROR)) {
      Binder.addErrorHandler(widget, new ErrorHandler() {
        @Override
        public void onError(ErrorEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_FOCUS)) {
      Binder.addFocusHandler(widget, new FocusHandler() {
        @Override
        public void onFocus(FocusEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_GESTURE_CHANGE)) {
      Binder.addGestureChangeHandler(widget, new GestureChangeHandler() {
        @Override
        public void onGestureChange(GestureChangeEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_GESTURE_END)) {
      Binder.addGestureEndHandler(widget, new GestureEndHandler() {
        @Override
        public void onGestureEnd(GestureEndEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_GESTURE_START)) {
      Binder.addGestureStartHandler(widget, new GestureStartHandler() {
        @Override
        public void onGestureStart(GestureStartEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_INPUT)) {
      Binder.addInputHandler(widget, new InputHandler() {
        @Override
        public void onInput(InputEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_KEY_DOWN)) {
      Binder.addKeyDownHandler(widget, new KeyDownHandler() {
        @Override
        public void onKeyDown(KeyDownEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_KEY_PRESS)) {
      Binder.addKeyPressHandler(widget, new KeyPressHandler() {
        @Override
        public void onKeyPress(KeyPressEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_KEY_UP)) {
      Binder.addKeyUpHandler(widget, new KeyUpHandler() {
        @Override
        public void onKeyUp(KeyUpEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_LOAD)) {
      Binder.addLoadHandler(widget, new LoadHandler() {
        @Override
        public void onLoad(LoadEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_LOSE_CAPTURE)) {
      Binder.addLoseCaptureHandler(widget, new LoseCaptureHandler() {
        @Override
        public void onLoseCapture(LoseCaptureEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_MOUSE_DOWN)) {
      Binder.addMouseDownHandler(widget, new MouseDownHandler() {
        @Override
        public void onMouseDown(MouseDownEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_MOUSE_MOVE)) {
      Binder.addMouseMoveHandler(widget, new MouseMoveHandler() {
        @Override
        public void onMouseMove(MouseMoveEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_MOUSE_OUT)) {
      Binder.addMouseOutHandler(widget, new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_MOUSE_OVER)) {
      Binder.addMouseOverHandler(widget, new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_MOUSE_UP)) {
      Binder.addMouseUpHandler(widget, new MouseUpHandler() {
        @Override
        public void onMouseUp(MouseUpEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_MOUSE_WHEEL)) {
      Binder.addMouseWheelHandler(widget, new MouseWheelHandler() {
        @Override
        public void onMouseWheel(MouseWheelEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_PROGRESS)) {
      Binder.addProgressHandler(widget, new ProgressHandler() {
        @Override
        public void onProgress(ProgressEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_SCROLL)) {
      Binder.addScrollHandler(widget, new ScrollHandler() {
        @Override
        public void onScroll(ScrollEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_TOUCH_CANCEL)) {
      Binder.addTouchCancelHandler(widget, new TouchCancelHandler() {
        @Override
        public void onTouchCancel(TouchCancelEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_TOUCH_END)) {
      Binder.addTouchEndHandler(widget, new TouchEndHandler() {
        @Override
        public void onTouchEnd(TouchEndEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_TOUCH_MOVE)) {
      Binder.addTouchMoveHandler(widget, new TouchMoveHandler() {
        @Override
        public void onTouchMove(TouchMoveEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    if (BeeUtils.same(type, EVENT_TYPE_TOUCH_START)) {
      Binder.addTouchStartHandler(widget, new TouchStartHandler() {
        @Override
        public void onTouchStart(TouchStartEvent event) {
          dispatchDomEvent(widget, handler, event);
        }
      });
      return true;
    }

    logger.warning("add handler:", NameUtils.getClassName(widget.getClass()),
        DomUtils.getId(widget), type, "event not supported");
    return false;
  }

  public static void allowCopy(DragStartEvent event) {
    setEffectAllowed(event, EFFECT_COPY);
  }

  public static void allowCopyMove(DragStartEvent event) {
    setEffectAllowed(event, EFFECT_COPY_MOVE);
  }

  public static void allowMove(DragStartEvent event) {
    setEffectAllowed(event, EFFECT_MOVE);
  }

  public static void clearRegistry(Collection<? extends HandlerRegistration> registry) {
    if (!BeeUtils.isEmpty(registry)) {
      for (HandlerRegistration hr : registry) {
        hr.removeHandler();
      }
      registry.clear();
    }
  }

  public static NativeEvent createKeyDown(int keyCode) {
    return Document.get().createKeyDownEvent(false, false, false, false, keyCode);
  }

  public static NativeEvent createKeyPress(int charCode) {
    return Document.get().createKeyPressEvent(false, false, false, false, charCode);
  }

  public static NativeEvent createKeyUp(int keyCode) {
    return Document.get().createKeyUpEvent(false, false, false, false, keyCode);
  }

  public static void fireKeyDown(Element target, int keyCode) {
    Assert.notNull(target);
    target.dispatchEvent(createKeyDown(keyCode));
  }

  public static void fireKeyDown(EventTarget target, int keyCode) {
    Assert.notNull(target);
    fireKeyDown(Element.as(target), keyCode);
  }

  public static void fireKeyPress(Element target, int charCode) {
    Assert.notNull(target);
    target.dispatchEvent(createKeyPress(charCode));
  }

  public static void fireKeyPress(EventTarget target, int charCode) {
    Assert.notNull(target);
    fireKeyPress(Element.as(target), charCode);
  }

  public static void fireKeyUp(Element target, int keyCode) {
    Assert.notNull(target);
    target.dispatchEvent(createKeyUp(keyCode));
  }

  public static void fireKeyUp(EventTarget target, int keyCode) {
    Assert.notNull(target);
    fireKeyUp(Element.as(target), keyCode);
  }

  public static String getCurrentTargetId(NativeEvent ev) {
    Assert.notNull(ev);
    EventTarget target = ev.getCurrentEventTarget();

    if (target == null) {
      return null;
    } else {
      return getTargetId(target);
    }
  }

  public static String getDndData(DragDropEventBase<?> event) {
    Assert.notNull(event);
    return event.getData(DEFAULT_DND_DATA_FORMAT);
  }

  public static List<Property> getEventInfo(NativeEvent ev) {
    Assert.notNull(ev);
    return PropertyUtils.createProperties(
        "Client X", ev.getClientX(),
        "Client Y", ev.getClientY(),
        "Screen X", ev.getScreenX(),
        "Screen Y", ev.getScreenY(),
        "Key Code", ev.getKeyCode(),
        "Char Code", ev.getCharCode(),
        "Alt Key", ev.getAltKey(),
        "Shift Key", ev.getShiftKey(),
        "Ctrl Key", ev.getCtrlKey(),
        "Meta Key", ev.getMetaKey(),
        "Button", ev.getButton(),
        "Mouse Wheel Velocity Y", ev.getMouseWheelVelocityY(),
        "Event Target", transformEventTarget(ev.getEventTarget()),
        "Current Event Target", transformEventTarget(ev.getCurrentEventTarget()),
        "Related Event Target", transformEventTarget(ev.getRelatedEventTarget()),
        "String", ev.getString(),
        "Type", ev.getType(),
        "Type Int", getTypeInt(ev));
  }

  public static Element getEventTargetElement(HasNativeEvent ev) {
    Assert.notNull(ev);
    return getEventTargetElement(ev.getNativeEvent());
  }

  public static Element getEventTargetElement(NativeEvent ev) {
    Assert.notNull(ev);
    return getTargetElement(ev.getEventTarget());
  }

  public static String getEventTargetId(NativeEvent ev) {
    Assert.notNull(ev);
    EventTarget target = ev.getEventTarget();

    if (target == null) {
      return null;
    } else {
      return getTargetId(target);
    }
  }

  public static Element getTargetElement(EventTarget et) {
    if (Element.is(et)) {
      return Element.as(et);
    } else {
      return null;
    }
  }

  public static String getTargetId(EventTarget et) {
    if (Element.is(et)) {
      return Element.as(et).getId();
    } else {
      return null;
    }
  }

  public static Node getTargetNode(EventTarget et) {
    if (Node.is(et)) {
      return Node.as(et);
    } else {
      return null;
    }
  }

  public static String getTargetTagName(EventTarget et) {
    if (Element.is(et)) {
      return Element.as(et).getTagName();
    } else {
      return null;
    }
  }

  public static int getTypeInt(NativeEvent ev) {
    if (ev == null) {
      return 0;
    }
    if (ev instanceof Event) {
      return ((Event) ev).getTypeInt();
    }
    return Event.getTypeInt(ev.getType());
  }

  public static boolean hasModifierKey(NativeEvent ev) {
    if (ev == null) {
      return false;
    }
    return ev.getShiftKey() || ev.getCtrlKey() || ev.getAltKey() || ev.getMetaKey();
  }

  public static boolean isArrowKey(int keyCode) {
    switch (keyCode) {
      case KeyCodes.KEY_DOWN:
      case KeyCodes.KEY_LEFT:
      case KeyCodes.KEY_RIGHT:
      case KeyCodes.KEY_UP:
        return true;
      default:
        return false;
    }
  }

  public static boolean isArrowKey(NativeEvent ev) {
    if (ev == null) {
      return false;
    } else {
      return isArrowKey(ev.getKeyCode());
    }
  }

  public static boolean isBitless(String type) {
    if (BeeUtils.isEmpty(type)) {
      return false;
    }
    return isDndEvent(type) || BeeUtils.inListSame(type, EVENT_TYPE_CAN_PLAY_THROUGH,
        EVENT_TYPE_ENDED, EVENT_TYPE_PROGRESS);
  }

  public static boolean isBlur(String type) {
    return isEventType(type, EVENT_TYPE_BLUR);
  }

  public static boolean isChange(String type) {
    return isEventType(type, EVENT_TYPE_CHANGE);
  }

  public static boolean isClick(NativeEvent ev) {
    if (ev == null) {
      return false;
    }
    return isClick(ev.getType());
  }

  public static boolean isClick(String type) {
    return isEventType(type, EVENT_TYPE_CLICK);
  }

  public static boolean isCurrentTarget(Event event, Element element) {
    if (event == null || element == null || event.getCurrentEventTarget() == null) {
      return false;
    } else {
      return event.getCurrentEventTarget().equals(element);
    }
  }

  public static boolean isDblClick(String type) {
    return isEventType(type, EVENT_TYPE_DBL_CLICK);
  }

  public static boolean isDndEvent(String type) {
    if (BeeUtils.isEmpty(type)) {
      return false;
    }
    return BeeUtils.inListSame(type, EVENT_TYPE_DRAG, EVENT_TYPE_DRAG_END, EVENT_TYPE_DRAG_ENTER,
        EVENT_TYPE_DRAG_LEAVE, EVENT_TYPE_DRAG_OVER, EVENT_TYPE_DRAG_START, EVENT_TYPE_DROP);
  }

  public static boolean isEventType(String t1, String t2) {
    if (BeeUtils.isEmpty(t1)) {
      return false;
    }
    return BeeUtils.same(t1, t2);
  }

  public static boolean isFocus(String type) {
    return isEventType(type, EVENT_TYPE_FOCUS);
  }

  public static boolean isInputElement(EventTarget et) {
    return isTargetTagName(et, Tags.INPUT);
  }

  public static boolean isInputEvent(String type) {
    return isEventType(type, EVENT_TYPE_INPUT);
  }

  public static boolean isKeyDown(String type) {
    return isEventType(type, EVENT_TYPE_KEY_DOWN);
  }

  public static boolean isKeyEvent(Event ev) {
    if (ev == null) {
      return false;
    } else {
      return isKeyEvent(ev.getTypeInt());
    }
  }

  public static boolean isKeyEvent(int type) {
    return (type & Event.KEYEVENTS) != 0;
  }

  public static boolean isKeyEvent(String type) {
    return isKeyDown(type) || isKeyPress(type) || isKeyUp(type);
  }

  public static boolean isKeyPress(String type) {
    return isEventType(type, EVENT_TYPE_KEY_PRESS);
  }

  public static boolean isKeyUp(String type) {
    return isEventType(type, EVENT_TYPE_KEY_UP);
  }

  public static boolean isLeftButton(int button) {
    return button == NativeEvent.BUTTON_LEFT;
  }

  public static boolean isLeftButton(NativeEvent event) {
    return (event == null) ? false : isLeftButton(event.getButton());
  }

  public static boolean isMouseButtonEvent(String type) {
    return isMouseDown(type) || isMouseUp(type) || isClick(type) || isDblClick(type);
  }

  public static boolean isMouseDown(String type) {
    return isEventType(type, EVENT_TYPE_MOUSE_DOWN);
  }

  public static boolean isMouseMove(String type) {
    return isEventType(type, EVENT_TYPE_MOUSE_MOVE);
  }

  public static boolean isMouseOut(String type) {
    return isEventType(type, EVENT_TYPE_MOUSE_OUT);
  }

  public static boolean isMouseOver(String type) {
    return isEventType(type, EVENT_TYPE_MOUSE_OVER);
  }

  public static boolean isMouseUp(String type) {
    return isEventType(type, EVENT_TYPE_MOUSE_UP);
  }

  public static boolean isMouseWheel(String type) {
    return isEventType(type, EVENT_TYPE_MOUSE_WHEEL);
  }

  public static boolean isMoveEvent(String type) {
    return isMouseMove(type) || isMouseOut(type) || isMouseOver(type);
  }

  public static boolean isTargetId(HasNativeEvent ev, String id) {
    return (ev == null) ? false : isTargetId(ev.getNativeEvent().getEventTarget(), id);
  }

  public static boolean isTargetId(EventTarget et, String id) {
    if (et == null || BeeUtils.isEmpty(id)) {
      return false;
    } else {
      return BeeUtils.same(getTargetId(et), id);
    }
  }

  public static boolean isTargetTagName(EventTarget et, String tagName) {
    if (et == null || BeeUtils.isEmpty(tagName)) {
      return false;
    }
    return BeeUtils.same(getTargetTagName(et), tagName);
  }

  public static void logEvent(NativeEvent ev) {
    logEvent(ev, false, null);
  }

  public static void logEvent(NativeEvent ev, boolean detailed, String title) {
    Assert.notNull(ev);

    if (detailed) {
      if (!BeeUtils.isEmpty(title)) {
        logger.debug(title);
      }
      List<Property> lst = getEventInfo(ev);
      for (Property el : lst) {
        logger.debug(el.getName(), el.getValue());
      }

      logger.addSeparator();
    } else {
      logger.debug(title, transformEvent(ev));
    }
  }

  public static void logEvent(NativeEvent ev, String title) {
    logEvent(ev, false, title);
  }

  public static boolean makeDndSource(HasAllDragAndDropHandlers widget, DragStartHandler handler) {
    if (!DragDropEventBase.isSupported()) {
      return false;
    }
    Assert.notNull(widget);
    Assert.notNull(handler);

    if (widget instanceof UIObject) {
      DomUtils.setDraggable((UIObject) widget);
    }

    widget.addDragStartHandler(handler);
    if (handler instanceof DragHandler) {
      widget.addDragHandler((DragHandler) handler);
    }
    if (handler instanceof DragEndHandler) {
      widget.addDragEndHandler((DragEndHandler) handler);
    }
    return true;
  }

  public static boolean makeDndTarget(HasAllDragAndDropHandlers widget, DropHandler handler) {
    if (!DragDropEventBase.isSupported()) {
      return false;
    }
    Assert.notNull(widget);
    Assert.notNull(handler);

    widget.addDropHandler(handler);
    if (handler instanceof DragEnterHandler) {
      widget.addDragEnterHandler((DragEnterHandler) handler);
    }
    if (handler instanceof DragOverHandler) {
      widget.addDragOverHandler((DragOverHandler) handler);
    }
    if (handler instanceof DragLeaveHandler) {
      widget.addDragLeaveHandler((DragLeaveHandler) handler);
    }
    return true;
  }

  public static void removeClassName(HasNativeEvent ev, String className) {
    Assert.notNull(ev);
    removeClassName(ev.getNativeEvent(), className);
  }

  public static void removeClassName(NativeEvent ev, String className) {
    Assert.notEmpty(className);
    Element element = getEventTargetElement(ev);
    if (element != null) {
      element.removeClassName(className);
    }
  }

  public static void selectDropCopy(DragOverEvent event) {
    setDropEffect(event, EFFECT_COPY);
  }

  public static void selectDropMove(DragOverEvent event) {
    setDropEffect(event, EFFECT_MOVE);
  }

  public static void selectDropNone(DragOverEvent event) {
    setDropEffect(event, EFFECT_NONE);
  }

  public static void setDndData(DragStartEvent event, Long id) {
    if (id != null) {
      setDndData(event, BeeUtils.toString(id));
    }
  }

  public static void setDndData(DragStartEvent event, String data) {
    setDndData(event, DEFAULT_DND_DATA_FORMAT, data);
  }

  public static void setDndData(DragStartEvent event, String format, String data) {
    Assert.notNull(event);
    Assert.notEmpty(format);
    Assert.notEmpty(data);

    event.setData(format, data);
  }

  public static void setDropEffect(DragOverEvent event, String effect) {
    Assert.notNull(event);
    Assert.notEmpty(effect);

    JsUtils.setProperty(event.getDataTransfer(), PROPERTY_DROP_EFFECT, effect);
  }

  public static void setEffectAllowed(DragStartEvent event, String effect) {
    Assert.notNull(event);
    Assert.notEmpty(effect);

    JsUtils.setProperty(event.getDataTransfer(), PROPERTY_EFFECT_ALLOWED, effect);
  }

  public static void sinkChildEvents(Element parent, String childTag, int eventBits) {
    Assert.notNull(parent);
    Assert.notEmpty(childTag);
    Assert.isTrue(eventBits != 0);

    NodeList<Element> lst = parent.getElementsByTagName(childTag);
    Assert.isTrue(lst.getLength() > 0, childTag + " children not found");

    for (int i = 0; i < lst.getLength(); i++) {
      Event.sinkEvents(lst.getItem(i), eventBits);
    }
  }

  public static void sinkEvents(Widget widget, Set<String> types) {
    Assert.notNull(widget);
    if (BeeUtils.isEmpty(types)) {
      return;
    }

    int eventsToSink = 0;

    for (String type : types) {
      if (isInputEvent(type)) {
        Binder.sinkInput(widget);
      } else if (isBitless(type)) {
        widget.sinkBitlessEvent(type);
      } else {
        int z = Event.getTypeInt(type);
        if (z > 0) {
          eventsToSink |= z;
        }
      }
    }

    if (eventsToSink > 0) {
      widget.sinkEvents(eventsToSink);
    }
  }

  public static String transformEvent(NativeEvent ev) {
    return transformEvent(ev, true);
  }

  public static String transformEvent(NativeEvent ev, boolean targets) {
    if (ev == null) {
      return BeeConst.STRING_EMPTY;
    }

    StringBuilder sb = new StringBuilder();
    sb.append(ev.getType());

    int x = ev.getClientX();
    if (x != 0) {
      sb.append(" x=" + x);
    }
    int y = ev.getClientY();
    if (y != 0) {
      sb.append(" y=" + y);
    }

    int k = ev.getKeyCode();
    if (k != 0) {
      sb.append(" k=" + k);
    }
    int c = ev.getCharCode();
    if (c != 0 && c != k) {
      sb.append(" c=" + c);
    }

    if (ev.getAltKey()) {
      sb.append(" alt");
    }
    if (ev.getShiftKey()) {
      sb.append(" shift");
    }
    if (ev.getCtrlKey()) {
      sb.append(" ctrl");
    }
    if (ev.getMetaKey()) {
      sb.append(" meta");
    }

    int b = ev.getButton();
    if (b != 0) {
      sb.append(" b=" + b);
    }
    int v = ev.getMouseWheelVelocityY();
    if (v != 0) {
      sb.append(" mwv=" + v);
    }

    if (targets) {
      EventTarget et = ev.getEventTarget();
      if (et != null) {
        sb.append(" et=" + transformEventTarget(et));
      }
      EventTarget cet = ev.getCurrentEventTarget();
      if (cet != null && cet != et) {
        sb.append(" cet=" + transformEventTarget(cet));
      }
      EventTarget ret = ev.getRelatedEventTarget();
      if (ret != null) {
        sb.append(" ret=" + transformEventTarget(ret));
      }
    }
    return sb.toString();
  }

  public static String transformEventTarget(EventTarget et) {
    if (et != null && Element.is(et)) {
      return DomUtils.transformElement(Element.as(et));
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  public static String transformMediaError(MediaError error) {
    if (error == null) {
      return BeeConst.STRING_EMPTY;
    }

    switch (error.getCode()) {
      case MediaError.MEDIA_ERR_ABORTED:
        return "playback aborted";
      case MediaError.MEDIA_ERR_DECODE:
        return "decoding error";
      case MediaError.MEDIA_ERR_NETWORK:
        return "network error";
      case MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED:
        return "source not supported";
    }
    return "unknown error";
  }

  private static JsFunction createDomHandler(String body) {
    return JsFunction.create("event, target, row, rowId, rowVersion", body);
  }

  private static void dispatchDomEvent(Widget widget, JsFunction handler, HasNativeEvent event) {
    JavaScriptObject row;
    double rowId;
    double rowVersion;

    FormView form = UiHelper.getForm(widget);
    IsRow data = (form == null) ? null : form.getActiveRow();

    if (data == null) {
      row = null;
      rowId = BeeConst.UNDEF;
      rowVersion = BeeConst.UNDEF;
    } else {
      row = form.getRowJso();
      rowId = data.getId();
      rowVersion = data.getVersion();
    }

    String errorMessage = evalDomHandler(handler, event.getNativeEvent(),
        event.getNativeEvent().getEventTarget(), row, rowId, rowVersion);
    if (!BeeUtils.isEmpty(errorMessage)) {
      logger.severe(errorMessage);
      BeeKeeper.getScreen().notifySevere(errorMessage);
    }
  }

  private static String domHandlerKey(String body) {
    return body;
  }

//@formatter:off
  private static native String evalDomHandler(JsFunction handler, NativeEvent event,
      JavaScriptObject target, JavaScriptObject row, double rowId, double rowVersion) /*-{
    try {
      handler(event, target, row, rowId, rowVersion);
      return null;
    } catch (err) {
      return String(err);
    }
  }-*/;
//@formatter:on

  private static JsFunction getDomHandler(String body) {
    String key = domHandlerKey(body);
    JsFunction handler = domHandlers.get(key);

    if (handler == null) {
      handler = createDomHandler(body);
      domHandlers.put(key, handler);
    }
    return handler;
  }

  private EventUtils() {
  }
}
