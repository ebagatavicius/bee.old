package com.butent.bee.client.tree;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.DragEndEvent;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragEvent;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.animation.Animation;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasIdentity;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TreeItem extends UIObject implements HasTreeItems, HasIdentity {

  private static final class TreeItemAnimation extends Animation {

    private static final int DURATION = 200;
    private static final int DURATION_PER_ITEM = 75;

    private Element element;
    private boolean opening = true;

    private int height;

    private TreeItemAnimation() {
      super();
    }

    @Override
    protected void onComplete() {
      if (element != null) {
        setVisible(element, opening);
        if (opening) {
          onUpdate(1.0);
          StyleUtils.autoHeight(element);
        }

        element.getStyle().clearOverflow();
        StyleUtils.autoWidth(element);

        element = null;
      }
    }

    @Override
    protected void onStart() {
      if (element == null) {
        return;
      }

      height = opening ? 0 : element.getScrollHeight();
      StyleUtils.hideScroll(element);

      super.onStart();

      if (opening) {
        setVisible(element, true);
        height = element.getScrollHeight();
      }
    }

    @Override
    protected void onUpdate(double progress) {
      if (element == null) {
        return;
      }

      int h = (int) (progress * height);
      if (!opening) {
        h = height - h;
      }

      StyleUtils.setHeight(element, Math.max(h, 1));
      StyleUtils.setWidth(element, element.getScrollWidth());
    }

    private void setItemState(TreeItem item, boolean animate) {
      cancel();

      if (animate) {
        element = item.getChildSpanElem();
        opening = item.isOpen();

        run(Math.min(DURATION, DURATION_PER_ITEM * item.getChildCount()));
      } else {
        setVisible(item.getChildSpanElem(), item.isOpen());
      }
    }
  }

  private static final TreeItemAnimation itemAnimation = new TreeItemAnimation();

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "TreeItem-";
  private static final String STYLE_BRANCH_CONTAINER = STYLE_PREFIX + "branchContainer";
  private static final String STYLE_LEAF_CONTAINER = STYLE_PREFIX + "leafContainer";

  private static final Element BRANCH_ELEM;
  private static final Element CONTAINER_ELEM;

  static {
    BRANCH_ELEM = DOM.createTable();
    Element tbody = DOM.createTBody();
    Element tr = DOM.createTR();
    Element tdImg = DOM.createTD();
    Element tdContent = DOM.createTD();

    setStyleName(BRANCH_ELEM, STYLE_PREFIX + "branch");
    setStyleName(tdImg, STYLE_PREFIX + "imageCell");
    setStyleName(tdContent, STYLE_PREFIX + "contentCell");

    BRANCH_ELEM.appendChild(tbody);
    tbody.appendChild(tr);
    tr.appendChild(tdImg);
    tr.appendChild(tdContent);

    CONTAINER_ELEM = DOM.createDiv();
    Element content = DOM.createDiv();

    setStyleName(CONTAINER_ELEM, STYLE_LEAF_CONTAINER);
    setStyleName(content, STYLE_PREFIX + "content");

    CONTAINER_ELEM.appendChild(content);
  }

  private List<TreeItem> children;

  private final Element contentElem;
  private Element childSpanElem;
  private Element imageHolder;

  private final boolean isRoot;

  private boolean open;
  private boolean selected;
  private boolean draggable;

  private TreeItem parentItem;
  private Tree tree;

  private Widget widget;
  private Object userObject;

  public TreeItem() {
    this(false);
  }

  public TreeItem(String html) {
    this();
    setHtml(html);
  }

  public TreeItem(String html, Object obj) {
    this(html);
    setUserObject(obj);
  }

  public TreeItem(Widget widget) {
    this();
    setWidget(widget);
  }

  TreeItem(boolean isRoot) {
    this.isRoot = isRoot;

    Element elem = DOM.clone(CONTAINER_ELEM, true);
    setElement(elem);
    DomUtils.createId(elem, getIdPrefix() + "-container");

    this.contentElem = DOM.getFirstChild(elem);
    DomUtils.createId(this.contentElem, getIdPrefix() + "-content");

    if (isRoot) {
      initChildren();
    }
  }

  @Override
  public TreeItem addItem(String itemHtml) {
    TreeItem ret = new TreeItem(itemHtml);
    addItem(ret);
    return ret;
  }

  @Override
  public void addItem(TreeItem item) {
    maybeRemoveItemFromParent(item);
    insertItem(getChildCount(), item);
  }

  @Override
  public TreeItem addItem(Widget w) {
    TreeItem ret = new TreeItem(w);
    addItem(ret);
    return ret;
  }

  public TreeItem getChild(int index) {
    if ((index < 0) || (index >= getChildCount())) {
      return null;
    }
    return getChildren().get(index);
  }

  public int getChildCount() {
    if (getChildren() == null) {
      return 0;
    }
    return getChildren().size();
  }

  public int getChildIndex(TreeItem child) {
    if (getChildren() == null) {
      return BeeConst.UNDEF;
    }
    return getChildren().indexOf(child);
  }

  public Element getContentElem() {
    return contentElem;
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "t-i";
  }

  @Override
  public int getItemCount() {
    return getChildCount();
  }

  public TreeItem getParentItem() {
    return parentItem;
  }

  @Override
  public Collection<TreeItem> getTreeItems() {
    return getChildren();
  }

  public Object getUserObject() {
    return userObject;
  }

  public Widget getWidget() {
    return widget;
  }

  public TreeItem insertItem(int beforeIndex, String itemText) {
    TreeItem ret = new TreeItem(itemText);
    insertItem(beforeIndex, ret);
    return ret;
  }

  public void insertItem(int beforeIndex, TreeItem item) {
    maybeRemoveItemFromParent(item);

    int childCount = getChildCount();
    Assert.betweenInclusive(beforeIndex, 0, childCount);

    if (getChildren() == null) {
      initChildren();
    }

    setStyleName(item.getElement(), STYLE_PREFIX + "root", isRoot);
    setStyleName(item.getElement(), STYLE_PREFIX + "child", !isRoot);

    Element childContainer = isRoot ? getTree().getElement() : getChildSpanElem();
    if (beforeIndex == childCount) {
      childContainer.appendChild(item.getElement());
    } else {
      Element beforeElem = getChild(beforeIndex).getElement();
      childContainer.insertBefore(item.getElement(), beforeElem);
    }

    item.setParentItem(isRoot ? null : this);
    getChildren().add(beforeIndex, item);

    item.setTree(getTree());

    if (!isRoot && getChildren().size() == 1) {
      updateState(false, false);
    }
  }

  public TreeItem insertItem(int beforeIndex, Widget w) {
    TreeItem ret = new TreeItem(w);
    insertItem(beforeIndex, ret);
    return ret;
  }

  public boolean isOpen() {
    return open;
  }

  public boolean isSelected() {
    return selected;
  }

  public void makeDraggable() {
    if (draggable) {
      return;
    }
    Element elem = getContentElem();
    DOM.sinkBitlessEvent(elem, DragStartEvent.getType().getName());
    DOM.sinkBitlessEvent(elem, DragEvent.getType().getName());
    DOM.sinkBitlessEvent(elem, DragEndEvent.getType().getName());
    DOM.sinkBitlessEvent(elem, DragEnterEvent.getType().getName());
    DOM.sinkBitlessEvent(elem, DragOverEvent.getType().getName());
    DOM.sinkBitlessEvent(elem, DragLeaveEvent.getType().getName());
    DOM.sinkBitlessEvent(elem, DropEvent.getType().getName());
    DomUtils.setDraggable(elem);
    this.draggable = true;
  }

  public void remove() {
    if (getParentItem() != null) {
      getParentItem().removeItem(this);
    } else if (getTree() != null) {
      getTree().removeItem(this);
    }
  }

  @Override
  public void removeItem(TreeItem item) {
    if (getChildren() == null || !getChildren().contains(item)) {
      return;
    }

    Tree oldTree = getTree();
    item.setTree(null);

    if (isRoot) {
      oldTree.getElement().removeChild(item.getElement());
    } else {
      getChildSpanElem().removeChild(item.getElement());
    }

    item.setParentItem(null);
    getChildren().remove(item);

    if (!isRoot && getChildren().isEmpty()) {
      updateState(false, false);
    }
  }

  @Override
  public void removeItems() {
    while (getChildCount() > 0) {
      removeItem(getChild(0));
    }
  }

  public void setHtml(String html) {
    setWidget(null);
    contentElem.setInnerHTML(html);
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setOpen(boolean open) {
    setOpen(open, true);
  }

  public void setOpen(boolean op, boolean fireEvents) {
    if (op && getChildCount() == 0) {
      return;
    }

    if (this.open != op) {
      this.open = op;
      updateState(true, true);

      if (fireEvents && getTree() != null) {
        getTree().fireStateChanged(this, op);
      }
    }
  }

  public void setOpenRecursive(boolean op, boolean fireEvents) {
    int n = getChildCount();
    if (n <= 0) {
      return;
    }

    setOpen(op, fireEvents);
    for (int i = 0; i < n; i++) {
      getChildren().get(i).setOpenRecursive(op, fireEvents);
    }
  }

  public void setSelected(boolean selected) {
    if (this.selected == selected) {
      return;
    }
    this.selected = selected;
    setStyleName(getContentElem(), STYLE_PREFIX + "selected", selected);
  }

  public void setUserObject(Object userObj) {
    userObject = userObj;
  }

  public void setWidget(Widget newWidget) {
    if (newWidget != null) {
      newWidget.removeFromParent();
    }

    if (getWidget() != null) {
      try {
        if (getTree() != null) {
          getTree().orphanWidget(getWidget());
        }
      } finally {
        contentElem.removeChild(getWidget().getElement());
        this.widget = null;
      }
    }

    contentElem.setInnerHTML(BeeConst.STRING_EMPTY);

    this.widget = newWidget;

    if (newWidget != null) {
      contentElem.appendChild(newWidget.getElement());

      if (getTree() != null) {
        getTree().adopt(newWidget, this);
      }

      if (Tree.shouldTreeDelegateFocusToElement(newWidget.getElement())) {
        newWidget.getElement().setTabIndex(-1);
      }
    }
  }

  protected Focusable getFocusable() {
    Widget w = getWidget();

    if (w instanceof Focusable) {
      return (Focusable) w;
    } else {
      return null;
    }
  }

  void addTreeItems(List<TreeItem> accum) {
    int size = getChildCount();
    for (int i = 0; i < size; i++) {
      TreeItem item = getChildren().get(i);
      accum.add(item);
      item.addTreeItems(accum);
    }
  }

  Element getImageElement() {
    return DOM.getFirstChild(getImageHolderElement());
  }

  Element getImageHolderElement() {
    if (!isBranch()) {
      convertToBranch();
    }
    return getImageHolder();
  }

  TreeItem getItemByContentId(String contentId) {
    Assert.notEmpty(contentId);
    TreeItem treeItem = null;

    if (BeeUtils.same(contentId, getContentElem().getId())) {
      treeItem = this;

    } else if (getChildCount() > 0) {
      for (TreeItem item : getChildren()) {
        treeItem = item.getItemByContentId(contentId);

        if (treeItem != null) {
          break;
        }
      }
    }
    return treeItem;
  }

  void setTree(Tree newTree) {
    if (getTree() == newTree) {
      return;
    }

    if (getTree() != null) {
      if (getTree().getSelectedItem() == this) {
        getTree().setSelectedItem(null);
      }

      if (getWidget() != null) {
        getTree().orphanWidget(getWidget());
      }
    }

    this.tree = newTree;
    for (int i = 0, n = getChildCount(); i < n; ++i) {
      getChildren().get(i).setTree(newTree);
    }
    updateState(false, true);

    if (newTree != null) {
      if (getWidget() != null) {
        newTree.adopt(getWidget(), this);
      }
    }
  }

  void updateStateRecursive() {
    updateStateRecursiveHelper();
    getTree().maybeUpdateSelection(this, isOpen());
  }

  private void convertToBranch() {
    if (!isBranch()) {
      Element itemTable = DOM.clone(BRANCH_ELEM, true);
      getElement().appendChild(itemTable);
      Element tr = DOM.getFirstChild(DOM.getFirstChild(itemTable));
      Element tdImg = DOM.getFirstChild(tr);
      Element tdContent = DOM.getNextSibling(tdImg);

      tdContent.appendChild(contentElem);

      removeStyleName(STYLE_LEAF_CONTAINER);
      addStyleName(STYLE_BRANCH_CONTAINER);

      setImageHolder(tdImg);
    }
  }

  private void convertToLeaf() {
    if (isBranch()) {
      contentElem.removeFromParent();
      DomUtils.clear(getElement());
      getElement().appendChild(contentElem);

      removeStyleName(STYLE_BRANCH_CONTAINER);
      addStyleName(STYLE_LEAF_CONTAINER);

      setImageHolder(null);
      setChildSpanElem(null);
      setChildren(null);
    }
  }

  private List<TreeItem> getChildren() {
    return children;
  }

  private Element getChildSpanElem() {
    return childSpanElem;
  }

  private Element getImageHolder() {
    return imageHolder;
  }

  private Tree getTree() {
    return tree;
  }

  private void initChildren() {
    convertToBranch();

    setChildSpanElem(DOM.createDiv());
    getElement().appendChild(getChildSpanElem());
    setStyleName(getChildSpanElem(), STYLE_PREFIX + "children");

    setChildren(new ArrayList<TreeItem>());
  }

  private boolean isBranch() {
    return getImageHolder() != null;
  }

  private static void maybeRemoveItemFromParent(TreeItem item) {
    if ((item.getParentItem() != null) || (item.getTree() != null)) {
      item.remove();
    }
  }

  private void setChildren(List<TreeItem> children) {
    this.children = children;
  }

  private void setChildSpanElem(Element childSpanElem) {
    this.childSpanElem = childSpanElem;
  }

  private void setImageHolder(Element imageHolder) {
    this.imageHolder = imageHolder;
  }

  private void setParentItem(TreeItem parentItem) {
    this.parentItem = parentItem;
  }

  private void updateState(boolean animate, boolean updateTreeSelection) {
    if (getTree() == null || !getTree().isAttached()) {
      return;
    }

    if (getChildCount() == 0) {
      if (isBranch() && !isRoot) {
        convertToLeaf();
      }
      return;
    }

    if (animate && (getTree() != null) && (getTree().isAttached())) {
      itemAnimation.setItemState(this, getTree().isAnimationEnabled());
    } else {
      itemAnimation.setItemState(this, false);
    }

    if (isOpen()) {
      getTree().showOpenImage(this);
    } else {
      getTree().showClosedImage(this);
    }

    if (updateTreeSelection) {
      getTree().maybeUpdateSelection(this, isOpen());
    }
  }

  private void updateStateRecursiveHelper() {
    updateState(false, false);
    for (int i = 0, n = getChildCount(); i < n; ++i) {
      getChildren().get(i).updateStateRecursiveHelper();
    }
  }
}
