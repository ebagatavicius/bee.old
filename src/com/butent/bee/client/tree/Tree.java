package com.butent.bee.client.tree;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.DataTransfer;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.DragEndEvent;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragEvent;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
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
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.HasOpenHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.AbstractImagePrototype.ImagePrototypeElement;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasAnimation;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.WidgetCollection;
import com.google.gwt.user.client.ui.impl.FocusImpl;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.CatchEvent;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Tree extends Panel implements HasTreeItems, Focusable, HasAnimation,
    HasAllKeyHandlers, HasAllFocusHandlers, HasSelectionHandlers<TreeItem>,
    CatchEvent.HasCatchHandlers<TreeItem>, HasOpenHandlers<TreeItem>, HasCloseHandlers<TreeItem>,
    HasAllMouseHandlers, EnablableWidget, IdentifiableWidget {

  public interface Resources extends ClientBundle {
    @Source("silver/treeClosed_17_18.png")
    ImageResource treeClosed();

    @Source("silver/treeOpen_17_18.png")
    ImageResource treeOpen();
  }

  private final class DndHandler {

    private TreeItem source;
    private boolean captionReady;

    private DndHandler() {
    }

    public boolean handleEvent(Event event) {
      DataTransfer dto = event.getDataTransfer();

      if (dto == null) {
        return false;
      }
      Element elem = EventUtils.getEventTargetElement(event).cast();
      String ev = event.getType();

      if (ev == DragStartEvent.getType().getName()) {
        if (isEnabled()) {
          source = getItemByContentId(elem.getId());
        }
        if (source == null) {
          event.preventDefault();
        } else {
          prepareCaption();
          elem.addClassName(StyleUtils.DND_SOURCE);
          dto.setData(EventUtils.DEFAULT_DND_DATA_FORMAT, elem.getId());
          JsUtils.setProperty(dto, EventUtils.PROPERTY_EFFECT_ALLOWED, EventUtils.EFFECT_MOVE);
        }
      } else if (ev == DragEvent.getType().getName()) {
        int border = 10;
        int width = getOffsetWidth();
        int left = getAbsoluteLeft();
        int right = left + width;
        int x = event.getClientX();
        int height = getOffsetHeight();
        int top = getAbsoluteTop();
        int bottom = top + height;
        int y = event.getClientY();

        if (BeeUtils.betweenInclusive(x, left, right)
            && BeeUtils.betweenInclusive(y, top, bottom)) {

          int sLeft = getElement().getScrollLeft();

          if (x < left + border) {
            if (sLeft > 0) {
              getElement().setScrollLeft(Math.max(sLeft - (border - x + left), 0));
            }
          } else if (x > right - border) {
            int boundary = getElement().getScrollWidth() - width;

            if (sLeft < boundary) {
              getElement().setScrollLeft(Math.min(sLeft + (border - right + x), boundary));
            }
          }
          int sTop = getElement().getScrollTop();

          if (y < top + border) {
            if (sTop > 0) {
              getElement().setScrollTop(Math.max(sTop - (border - y + top), 0));
            }
          } else if (y > bottom - border) {
            int boundary = getElement().getScrollHeight() - height;

            if (sTop < boundary) {
              getElement().setScrollTop(Math.min(sTop + (border - bottom + y), boundary));
            }
          }
        }
      } else if (ev == DragEndEvent.getType().getName()) {
        elem.removeClassName(StyleUtils.DND_SOURCE);
        source = null;

      } else if (ev == DragEnterEvent.getType().getName()) {
        if (isTarget(elem)) {
          elem.addClassName(StyleUtils.DND_OVER);
          TreeItem item = getItemByContentId(elem.getId());

          if (item != null && !item.isOpen()) {
            item.setOpen(true);
          }
        }
      } else if (ev == DragOverEvent.getType().getName()) {
        JsUtils.setProperty(dto, EventUtils.PROPERTY_DROP_EFFECT, EventUtils.EFFECT_MOVE);

      } else if (ev == DragLeaveEvent.getType().getName()) {
        if (isTarget(elem)) {
          elem.removeClassName(StyleUtils.DND_OVER);
        }
      } else if (ev == DropEvent.getType().getName()) {
        event.stopPropagation();

        if (isTarget(elem)) {
          elem.removeClassName(StyleUtils.DND_OVER);

          final HasTreeItems target;
          final TreeItem src = source;
          TreeItem dst = null;

          if (isCaption(elem)) {
            target = Tree.this;
          } else {
            dst = getItemByContentId(elem.getId());
            target = dst;
          }

          CatchEvent<TreeItem> catchEvent = CatchEvent.fire(Tree.this, src, dst,
              new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                  target.addItem(src);
                  setSelectedItem(src);
                  ensureSelectedItemVisible();
                }
              });

          if (!catchEvent.isConsumed()) {
            catchEvent.consume();
            catchEvent.executeScheduled();
          }
        }
      }
      return true;
    }

    private boolean isTarget(Element elem) {
      if (elem == null || source == null) {
        return false;
      }
      if (isCaption(elem)) {
        return source.getParentItem() != null;
      }
      TreeItem item = getItemByContentId(elem.getId());

      if (item == null || item == source.getParentItem()) {
        return false;
      }
      while (item != null) {
        if (item == source) {
          return false;
        }
        item = item.getParentItem();
      }
      return true;
    }

    private void prepareCaption() {
      if (!captionReady) {
        if (caption != null) {
          Element elem = caption.getElement();
          DOM.sinkBitlessEvent(elem, DragEnterEvent.getType().getName());
          DOM.sinkBitlessEvent(elem, DragOverEvent.getType().getName());
          DOM.sinkBitlessEvent(elem, DragLeaveEvent.getType().getName());
          DOM.sinkBitlessEvent(elem, DropEvent.getType().getName());
        }
        captionReady = true;
      }
    }
  }

  private static AbstractImagePrototype treeClosed;
  private static AbstractImagePrototype treeOpen;

  static {
    Resources resources = GWT.create(Resources.class);
    treeClosed = AbstractImagePrototype.create(resources.treeClosed());
    treeOpen = AbstractImagePrototype.create(resources.treeOpen());
  }

  static boolean shouldTreeDelegateFocusToElement(Element elem) {
    if (elem == null) {
      return false;
    }
    return BeeUtils.inList(elem.getTagName(), Tags.SELECT, Tags.INPUT, Tags.TEXT_AREA, Tags.OPTION,
        Tags.BUTTON, Tags.LABEL);
  }

  private final Map<Widget, TreeItem> childWidgets = new HashMap<>();

  private TreeItem selectedItem;

  private final Element focusable;

  private boolean isAnimationEnabled;

  private final Widget caption;
  private final TreeItem root;

  private DndHandler dndHandler;

  private boolean enabled = true;

  public Tree() {
    this(null);
  }

  public Tree(String caption) {
    setElement(Document.get().createDivElement());

    getElement().getStyle().setPosition(Position.RELATIVE);

    focusable = FocusImpl.getFocusImplForPanel().createFocusable();

    StyleUtils.makeAbsolute(focusable);
    StyleUtils.hideOutline(focusable);
    StyleUtils.setZIndex(focusable, -1);

    getElement().appendChild(focusable);

    sinkEvents(Event.ONMOUSEDOWN | Event.ONCLICK | Event.KEYEVENTS);
    DOM.sinkEvents(focusable, Event.FOCUSEVENTS);

    root = new TreeItem(true);
    root.setTree(this);

    if (!BeeUtils.isEmpty(caption)) {
      this.caption = new InlineLabel(Localized.maybeTranslate(caption));
      this.caption.setStyleName(BeeConst.CSS_CLASS_PREFIX + "Tree-caption");
      getElement().appendChild(this.caption.getElement());
    } else {
      this.caption = null;
    }

    setStyleName(BeeConst.CSS_CLASS_PREFIX + "Tree");
    DomUtils.createId(this, getIdPrefix());
  }

  @Override
  public void add(Widget widget) {
    addItem(widget);
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return addDomHandler(handler, BlurEvent.getType());
  }

  @Override
  public HandlerRegistration addCatchHandler(CatchEvent.CatchHandler<TreeItem> handler) {
    return addHandler(handler, CatchEvent.getType());
  }

  @Override
  public HandlerRegistration addCloseHandler(CloseHandler<TreeItem> handler) {
    return addHandler(handler, CloseEvent.getType());
  }

  @Override
  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    return addDomHandler(handler, FocusEvent.getType());
  }

  @Override
  public TreeItem addItem(String itemHtml) {
    return root.addItem(itemHtml);
  }

  @Override
  public void addItem(TreeItem item) {
    root.addItem(item);
  }

  @Override
  public TreeItem addItem(Widget widget) {
    return root.addItem(widget);
  }

  @Override
  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return addDomHandler(handler, KeyDownEvent.getType());
  }

  @Override
  public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
    return addDomHandler(handler, KeyPressEvent.getType());
  }

  @Override
  public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
    return addDomHandler(handler, KeyUpEvent.getType());
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
  public final HandlerRegistration addOpenHandler(OpenHandler<TreeItem> handler) {
    return addHandler(handler, OpenEvent.getType());
  }

  @Override
  public HandlerRegistration addSelectionHandler(SelectionHandler<TreeItem> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  @Override
  public void clear() {
    int size = root.getChildCount();
    for (int i = size - 1; i >= 0; i--) {
      root.getChild(i).remove();
    }
  }

  public void ensureSelectedItemVisible() {
    if (getSelectedItem() == null) {
      return;
    }

    TreeItem parent = getSelectedItem().getParentItem();
    while (parent != null) {
      parent.setOpen(true);
      parent = parent.getParentItem();
    }
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "tree";
  }

  public TreeItem getItem(int index) {
    return root.getChild(index);
  }

  @Override
  public int getItemCount() {
    return root.getChildCount();
  }

  public TreeItem getSelectedItem() {
    return selectedItem;
  }

  @Override
  public int getTabIndex() {
    return FocusImpl.getFocusImplForPanel().getTabIndex(focusable);
  }

  @Override
  public Collection<TreeItem> getTreeItems() {
    return root.getTreeItems();
  }

  public TreeItem insertItem(int beforeIndex, String itemText) {
    return root.insertItem(beforeIndex, itemText);
  }

  public void insertItem(int beforeIndex, TreeItem item) {
    root.insertItem(beforeIndex, item);
  }

  public TreeItem insertItem(int beforeIndex, Widget widget) {
    return root.insertItem(beforeIndex, widget);
  }

  @Override
  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public Iterator<Widget> iterator() {
    WidgetCollection widgetCollection = new WidgetCollection(this);
    for (Widget widget : childWidgets.keySet()) {
      widgetCollection.add(widget);
    }
    return widgetCollection.iterator();
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (handleDndEvent(event)) {
      return;
    }
    int eventType = event.getTypeInt();

    switch (eventType) {
      case Event.ONCLICK:
        Element el = DOM.eventGetTarget(event);
        if (!shouldTreeDelegateFocusToElement(el) && (getSelectedItem() != null)
            && getSelectedItem().getContentElem().isOrHasChild(el)) {
          setFocus(true);
        }
        break;

      case Event.ONMOUSEDOWN:
        if ((DOM.eventGetCurrentTarget(event) == getElement())
            && (event.getButton() == NativeEvent.BUTTON_LEFT)) {
          elementClicked(DOM.eventGetTarget(event));
        }
        break;

      case Event.ONKEYDOWN:
        if (isKeyboardNavigationEnabled() && EventUtils.isArrowKey(event)
            && !event.getAltKey() && !event.getMetaKey()) {
          navigate(event);
          event.preventDefault();
        }
        break;

      case Event.ONKEYUP:
        if (event.getKeyCode() == KeyCodes.KEY_TAB) {
          List<Element> chain = new ArrayList<>();
          collectElementChain(chain, getElement(), DOM.eventGetTarget(event));
          TreeItem item = findItemByChain(chain, 0, root);
          if (item != getSelectedItem()) {
            setSelectedItem(item, true);
          }
        }
        break;
    }

    super.onBrowserEvent(event);
  }

  @Override
  public boolean remove(Widget w) {
    TreeItem item = childWidgets.get(w);
    if (item == null) {
      return false;
    }

    item.setWidget(null);
    return true;
  }

  @Override
  public void removeItem(TreeItem item) {
    root.removeItem(item);
  }

  @Override
  public void removeItems() {
    while (getItemCount() > 0) {
      removeItem(getItem(0));
    }
  }

  @Override
  public void setAccessKey(char key) {
    FocusImpl.getFocusImplForPanel().setAccessKey(focusable, key);
  }

  @Override
  public void setAnimationEnabled(boolean enable) {
    this.isAnimationEnabled = enable;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled == isEnabled()) {
      return;
    }
    this.enabled = enabled;
  }

  @Override
  public void setFocus(boolean focus) {
    if (focus) {
      FocusImpl.getFocusImplForPanel().focus(focusable);
    } else {
      FocusImpl.getFocusImplForPanel().blur(focusable);
    }
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setSelectedItem(TreeItem item) {
    setSelectedItem(item, true);
  }

  public void setSelectedItem(TreeItem item, boolean fireEvents) {
    if (item == null) {
      if (getSelectedItem() == null) {
        return;
      }
      getSelectedItem().setSelected(false);
      this.selectedItem = null;
      return;
    }

    onSelection(item, fireEvents, true);
  }

  @Override
  public void setTabIndex(int index) {
    FocusImpl.getFocusImplForPanel().setTabIndex(focusable, index);
  }

  public Iterator<TreeItem> treeItemIterator() {
    List<TreeItem> accum = new ArrayList<>();
    root.addTreeItems(accum);
    return accum.iterator();
  }

  @Override
  protected void doAttachChildren() {
    try {
      super.doAttachChildren();
    } finally {
      DOM.setEventListener(focusable, this);
    }
  }

  @Override
  protected void doDetachChildren() {
    try {
      super.doDetachChildren();
    } finally {
      DOM.setEventListener(focusable, null);
    }
  }

  protected boolean isKeyboardNavigationEnabled() {
    return true;
  }

  @Override
  protected void onLoad() {
    root.updateStateRecursive();
  }

  void adopt(Widget widget, TreeItem treeItem) {
    Assert.isFalse(childWidgets.containsKey(widget));
    childWidgets.put(widget, treeItem);
    adopt(widget);
  }

  void fireStateChanged(TreeItem item, boolean open) {
    if (open) {
      OpenEvent.fire(this, item);
    } else {
      CloseEvent.fire(this, item);
    }
  }

  void maybeUpdateSelection(TreeItem itemThatChangedState, boolean isItemOpening) {
    if (!isItemOpening) {
      TreeItem tempItem = getSelectedItem();
      while (tempItem != null) {
        if (tempItem == itemThatChangedState) {
          setSelectedItem(itemThatChangedState);
          return;
        }
        tempItem = tempItem.getParentItem();
      }
    }
  }

  void orphanWidget(Widget widget) {
    try {
      orphan(widget);
    } finally {
      childWidgets.remove(widget);
    }
  }

  void showClosedImage(TreeItem treeItem) {
    showImage(treeItem, treeClosed);
  }

  void showOpenImage(TreeItem treeItem) {
    showImage(treeItem, treeOpen);
  }

  private void collectElementChain(List<Element> chain, Element hRoot, Element hElem) {
    if ((hElem == null) || (hElem == hRoot)) {
      return;
    }

    collectElementChain(chain, hRoot, DOM.getParent(hElem));
    chain.add(hElem);
  }

  private boolean elementClicked(Element hElem) {
    if (isCaption(hElem)) {
      selectCaption(true);
      return true;
    }
    List<Element> chain = new ArrayList<>();
    collectElementChain(chain, getElement(), hElem);

    TreeItem item = findItemByChain(chain, 0, root);
    if (item != null && item != root) {
      if (item.getChildCount() > 0 && item.getImageElement().isOrHasChild(hElem)) {
        item.setOpen(!item.isOpen(), true);
        return true;
      } else if (item.getElement().isOrHasChild(hElem)) {
        onSelection(item, true, !shouldTreeDelegateFocusToElement(hElem));
        return true;
      }
    }

    return false;
  }

  private TreeItem findDeepestOpenChild(TreeItem item) {
    if (!item.isOpen()) {
      return item;
    }
    return findDeepestOpenChild(item.getChild(item.getChildCount() - 1));
  }

  private TreeItem findItemByChain(List<Element> chain, int idx, TreeItem rootItem) {
    if (idx == chain.size()) {
      return rootItem;
    }

    Element hCurElem = chain.get(idx);
    for (int i = 0, n = rootItem.getChildCount(); i < n; ++i) {
      TreeItem child = rootItem.getChild(i);
      if (child.getElement() == hCurElem) {
        TreeItem retItem = findItemByChain(chain, idx + 1, rootItem.getChild(i));
        if (retItem == null) {
          return child;
        }
        return retItem;
      }
    }

    return findItemByChain(chain, idx + 1, rootItem);
  }

  private void focus(Element focusElem) {
    int containerLeft = getAbsoluteLeft();
    int containerTop = getAbsoluteTop();

    int left = focusElem.getAbsoluteLeft() - containerLeft + getElement().getScrollLeft();
    int top = focusElem.getAbsoluteTop() - containerTop + getElement().getScrollTop();
    int width = focusElem.getOffsetWidth();
    int height = focusElem.getOffsetHeight();

    if (width <= 0 || height <= 0) {
      StyleUtils.setLeft(focusable, 0);
      StyleUtils.setTop(focusable, 0);
      return;
    }
    StyleUtils.setRectangle(focusable, left, top, width, height);
    focusable.scrollIntoView();

    setFocus(true);
  }

  private TreeItem getItemByContentId(String contentId) {
    return root.getItemByContentId(contentId);
  }

  private TreeItem getTopClosedParent(TreeItem item) {
    if (item == null) {
      return null;
    }

    TreeItem topClosedParent = null;
    TreeItem parent = item.getParentItem();
    while (parent != null && parent != root) {
      if (!parent.isOpen()) {
        topClosedParent = parent;
      }
      parent = parent.getParentItem();
    }
    return topClosedParent;
  }

  private boolean handleDndEvent(Event event) {
    if (dndHandler == null) {
      dndHandler = new DndHandler();
    }
    return dndHandler.handleEvent(event);
  }

  private boolean isCaption(Element elem) {
    return caption != null && elem == caption.getElement();
  }

  private void maybeCollapseTreeItem() {
    if (getSelectedItem() == null) {
      root.setOpenRecursive(false, false);
      if (caption != null) {
        selectCaption(false);
      }
      return;
    }

    TreeItem topClosedParent = getTopClosedParent(getSelectedItem());
    if (topClosedParent != null) {
      setSelectedItem(topClosedParent);
    } else if (getSelectedItem().isOpen()) {
      getSelectedItem().setOpen(false);
    } else {
      TreeItem parent = getSelectedItem().getParentItem();
      if (parent != null) {
        setSelectedItem(parent);
      }
    }
  }

  private void maybeExpandTreeItem() {
    if (getSelectedItem() == null) {
      root.setOpenRecursive(true, false);
      if (caption != null) {
        selectCaption(false);
      }
      return;
    }

    TreeItem topClosedParent = getTopClosedParent(getSelectedItem());
    if (topClosedParent != null) {
      setSelectedItem(topClosedParent);
    } else if (!getSelectedItem().isOpen()) {
      getSelectedItem().setOpen(true);
    } else if (getSelectedItem().getChildCount() > 0) {
      setSelectedItem(getSelectedItem().getChild(0));
    }
  }

  private void moveFocus() {
    Focusable focusableWidget = getSelectedItem().getFocusable();
    if (focusableWidget != null) {
      focusableWidget.setFocus(true);
      ((Widget) focusableWidget).getElement().scrollIntoView();
    } else {
      focus(getSelectedItem().getContentElem());
    }
  }

  private void moveSelectionDown(TreeItem sel, boolean dig) {
    if (sel == null) {
      if (root.getChildCount() > 0) {
        onSelection(root.getChild(0), true, true);
      }
      return;
    }
    if (sel == root) {
      return;
    }

    TreeItem topClosedParent = getTopClosedParent(sel);
    if (topClosedParent != null) {
      moveSelectionDown(topClosedParent, false);
      return;
    }

    TreeItem parent = sel.getParentItem();
    if (parent == null) {
      parent = root;
    }
    int idx = parent.getChildIndex(sel);

    if (!dig || !sel.isOpen()) {
      if (idx < parent.getChildCount() - 1) {
        onSelection(parent.getChild(idx + 1), true, true);
      } else {
        moveSelectionDown(parent, false);
      }
    } else if (sel.getChildCount() > 0) {
      onSelection(sel.getChild(0), true, true);
    }
  }

  private void moveSelectionUp(TreeItem sel) {
    if (sel == null) {
      return;
    }
    TreeItem topClosedParent = getTopClosedParent(sel);
    if (topClosedParent != null) {
      onSelection(topClosedParent, true, true);
      return;
    }

    TreeItem parent = sel.getParentItem();

    if (parent != null) {
      int idx = parent.getChildIndex(sel);
      if (idx > 0) {
        TreeItem sibling = parent.getChild(idx - 1);
        onSelection(findDeepestOpenChild(sibling), true, true);
      } else {
        onSelection(parent, true, true);
      }
    } else if (caption != null) {
      selectCaption(true);
    }
  }

  private void navigate(Event event) {
    switch (event.getKeyCode()) {
      case KeyCodes.KEY_UP:
        moveSelectionUp(getSelectedItem());
        break;

      case KeyCodes.KEY_DOWN:
        moveSelectionDown(getSelectedItem(), true);
        break;

      case KeyCodes.KEY_LEFT:
        maybeCollapseTreeItem();
        break;

      case KeyCodes.KEY_RIGHT:
        maybeExpandTreeItem();
        break;
    }
  }

  private void onSelection(TreeItem item, boolean fireEvents, boolean moveFocus) {
    if (item == root || item == getSelectedItem()) {
      return;
    }

    if (getSelectedItem() != null) {
      getSelectedItem().setSelected(false);
    }
    this.selectedItem = item;

    if (item != null) {
      if (moveFocus) {
        moveFocus();
      }

      item.setSelected(true);
      if (fireEvents) {
        SelectionEvent.fire(this, item);
      }
    }
  }

  private void selectCaption(boolean fireEvents) {
    if (getSelectedItem() != null) {
      setSelectedItem(null);
    }
    focus(caption.getElement());
    if (fireEvents) {
      SelectionEvent.fire(this, null);
    }
  }

  private static void showImage(TreeItem treeItem, AbstractImagePrototype proto) {
    Element holder = treeItem.getImageHolderElement();
    Element child = DOM.getFirstChild(holder);
    if (child == null) {
      DOM.appendChild(holder, proto.createElement());
    } else {
      proto.applyTo((ImagePrototypeElement) child);
    }
  }
}
