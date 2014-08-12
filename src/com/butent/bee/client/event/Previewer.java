package com.butent.bee.client.event;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;

import java.util.Collections;
import java.util.List;

public final class Previewer implements NativePreviewHandler, HasInfo {

  public interface PreviewConsumer extends Consumer<NativePreviewEvent> {
  }

  private static final class ComparableHandler implements Comparable<ComparableHandler> {
    private final int index;
    private final PreviewHandler handler;

    private ComparableHandler(int index, PreviewHandler handler) {
      super();
      this.index = index;
      this.handler = handler;
    }

    @Override
    public int compareTo(ComparableHandler o) {
      if (index == o.index) {
        return BeeConst.COMPARE_EQUAL;
      }

      if (handler.getElement() != null && o.handler.getElement() != null) {
        boolean isParent = handler.getElement().isOrHasChild(o.handler.getElement());
        boolean isChild = o.handler.getElement().isOrHasChild(handler.getElement());

        if (isParent && !isChild) {
          return BeeConst.COMPARE_LESS;
        } else if (isChild && !isParent) {
          return BeeConst.COMPARE_MORE;
        }
      }

      return Ints.compare(index, o.index);
    }
  }

  private static final Previewer INSTANCE = new Previewer();

  public static void ensureRegistered(PreviewHandler handler) {
    Assert.notNull(handler);
    if (!INSTANCE.contains(handler)) {
      INSTANCE.add(handler);
    }
    INSTANCE.maybeSortHandlers();
  }

  public static void ensureUnregistered(PreviewHandler handler) {
    Assert.notNull(handler);
    if (INSTANCE.contains(handler)) {
      INSTANCE.remove(handler);
    }
  }

  public static Previewer getInstance() {
    return INSTANCE;
  }

  public static boolean preview(Event event, Node target) {
    Assert.notNull(event);

    INSTANCE.setTargetNode(target);
    boolean result = DOM.previewEvent(event);
    INSTANCE.setTargetNode(null);

    return result;
  }

  public static void register(PreviewHandler handler) {
    Assert.notNull(handler);
    Assert.state(!INSTANCE.contains(handler));

    INSTANCE.add(handler);
    INSTANCE.maybeSortHandlers();
  }

  public static void registerMouseDownPriorHandler(PreviewHandler handler) {
    Assert.notNull(handler);
    INSTANCE.mouseDownPriorHandlers.add(handler);
  }

  public static void unregister(PreviewHandler handler) {
    Assert.notNull(handler);
    Assert.state(INSTANCE.contains(handler));

    INSTANCE.remove(handler);
  }

  public static void unregisterMouseDownPriorHandler(PreviewHandler handler) {
    Assert.notNull(handler);
    INSTANCE.mouseDownPriorHandlers.remove(handler);
  }

  private static boolean isExternalElement(Element element) {
    if (element == null) {
      return false;
    }
    if (element.getId() != null && element.getId().startsWith("mce_")) {
      return true;
    }
    String className = DomUtils.getClassName(element);
    if (className != null && className.startsWith("mce-")) {
      return true;
    }
    return false;
  }

  private final List<PreviewHandler> handlers = Lists.newArrayList();

  private final List<PreviewHandler> mouseDownPriorHandlers = Lists.newArrayList();

  private int modalCount;

  private Node targetNode;

  private Previewer() {
    Event.addNativePreviewHandler(this);
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = Lists.newArrayList();
    info.add(new Property("Modal Count", BeeUtils.toString(modalCount)));

    info.add(new Property("Handlers", BeeUtils.toString(handlers.size())));
    for (PreviewHandler handler : handlers) {
      info.add(new Property(handler.getId(), NameUtils.getName(handler)));
    }

    info.add(new Property("Mouse Down Prior", BeeUtils.toString(mouseDownPriorHandlers.size())));
    for (PreviewHandler handler : mouseDownPriorHandlers) {
      info.add(new Property(handler.getId(), NameUtils.getName(handler)));
    }
    return info;
  }

  @Override
  public void onPreviewNativeEvent(NativePreviewEvent event) {
    String type = event.getNativeEvent().getType();

    if (modalCount == 0 && EventUtils.EVENT_TYPE_MOUSE_DOWN.equals(type)) {
      for (int i = 0; i < mouseDownPriorHandlers.size(); i++) {
        mouseDownPriorHandlers.get(i).onEventPreview(event, getTargetNode(event));
        if (event.isCanceled() || event.isConsumed()) {
          return;
        }
      }

    } else if (EventUtils.EVENT_TYPE_KEY_DOWN.equals(type)) {
      int keyCode = event.getNativeEvent().getKeyCode();

      switch (keyCode) {
        case KeyCodes.KEY_F8:
          if (EventUtils.hasModifierKey(event.getNativeEvent())) {
            tryAction(event, Action.DELETE);
          }
          break;

        case KeyCodes.KEY_F9:
          Action action = EventUtils.hasModifierKey(event.getNativeEvent())
              ? Action.COPY : Action.ADD;
          tryAction(event, action);
          break;
      }

      if (event.isCanceled() || event.isConsumed()) {
        return;
      }
    }

    if (!handlers.isEmpty() && !isExternalEvent(event)) {
      int size = handlers.size();

      if (size == 1) {
        handlers.get(0).onEventPreview(event, getTargetNode(event));

      } else {
        for (int i = size - 1; i >= 0; i--) {
          if (i < handlers.size()) {
            handlers.get(i).onEventPreview(event, getTargetNode(event));
            if (event.isCanceled() || event.isConsumed()) {
              break;
            }
          }
        }
      }
    }
  }

  private void add(PreviewHandler handler) {
    handlers.add(handler);
    if (handler.isModal()) {
      modalCount++;
    }
  }

  private boolean contains(PreviewHandler handler) {
    return !BeeConst.isUndef(indexOf(handler));
  }

  private Node getTargetNode() {
    return targetNode;
  }

  private Node getTargetNode(NativePreviewEvent event) {
    if (getTargetNode() != null || event == null) {
      return getTargetNode();
    } else {
      EventTarget eventTarget = event.getNativeEvent().getEventTarget();
      return (eventTarget == null) ? null : EventUtils.getTargetNode(eventTarget);
    }
  }

  private int indexOf(PreviewHandler handler) {
    for (int i = 0; i < handlers.size(); i++) {
      if (BeeUtils.same(handler.getId(), handlers.get(i).getId())) {
        return i;
      }
    }
    return BeeConst.UNDEF;
  }

  private boolean isExternalEvent(NativePreviewEvent event) {
    Node node = getTargetNode(event);
    if (node == null) {
      return false;
    }

    if (Element.is(node) && isExternalElement(Element.as(node))) {
      return true;
    }
    return isExternalElement(node.getParentElement());
  }

  private void maybeSortHandlers() {
    if (handlers.size() < 2) {
      return;
    }

    List<ComparableHandler> comparableHandlers = Lists.newArrayList();
    for (int i = 0; i < handlers.size(); i++) {
      comparableHandlers.add(new ComparableHandler(i, handlers.get(i)));
    }

    Collections.sort(comparableHandlers);
    handlers.clear();
    for (ComparableHandler ch : comparableHandlers) {
      handlers.add(ch.handler);
    }
  }

  private void remove(PreviewHandler handler) {
    handlers.remove(indexOf(handler));
    if (handler.isModal()) {
      modalCount--;
    }
  }

  private void setTargetNode(Node targetNode) {
    this.targetNode = targetNode;
  }

  private void tryAction(NativePreviewEvent event, Action action) {
    Node node = getTargetNode(event);
    Element element = Element.is(node) ? Element.as(node) : null;

    View view = ViewHelper.getActiveView(element, action);

    if (view != null) {
      event.consume();
      view.getViewPresenter().handleAction(action);
    }
  }
}
