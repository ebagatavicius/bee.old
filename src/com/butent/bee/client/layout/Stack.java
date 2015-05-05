package com.butent.bee.client.layout;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.animation.RafCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Contains a panel that stacks its children vertically, displaying only one at a time, with a
 * header for each child which the user can click to display.
 */

public class Stack extends ComplexPanel implements ProvidesResize, RequiresResize,
    HasBeforeSelectionHandlers<Integer>, HasSelectionHandlers<Integer>, IdentifiableWidget {

  private static final class Header extends Composite implements HasClickHandlers, ProvidesResize,
      RequiresResize, IdentifiableWidget {

    private int size;

    private Header(Widget child, int size) {
      super();
      this.size = size;

      Style style = child.getElement().getStyle();
      style.setPosition(Position.ABSOLUTE);
      StyleUtils.setLeft(style, 0);
      StyleUtils.setRight(style, 0);

      StyleUtils.setHeight(style, size);

      initWidget(child);
    }

    @Override
    public HandlerRegistration addClickHandler(ClickHandler handler) {
      return addDomHandler(handler, ClickEvent.getType());
    }

    @Override
    public String getId() {
      return DomUtils.getId(this);
    }

    @Override
    public String getIdPrefix() {
      if (getWidget() instanceof IdentifiableWidget) {
        return ((IdentifiableWidget) getWidget()).getIdPrefix();
      } else {
        return "header";
      }
    }

    @Override
    public void onResize() {
      if (getWidget() instanceof RequiresResize) {
        ((RequiresResize) getWidget()).onResize();
      }
    }

    @Override
    public void setId(String id) {
      DomUtils.setId(this, id);
    }

    @Override
    protected Widget getWidget() {
      return super.getWidget();
    }

    private int getSize() {
      return size;
    }

    private boolean updateSize(int newSize) {
      if (newSize > 0 && getSize() != newSize) {
        size = newSize;
        StyleUtils.setHeight(this, newSize);

        return true;

      } else {
        return false;
      }
    }
  }

  private static final class Revelation extends RafCallback {

    private static final double FROM = 0.2;
    private static final double TO = 1.0;

    private Style style;

    private Revelation(double duration) {
      super(duration);
    }

    @Override
    protected void onComplete() {
      StyleUtils.clearTransform(style);
    }

    @Override
    protected boolean run(double elapsed) {
      double factor = BeeUtils.rescale(elapsed, 0, getDuration(), FROM, TO);
      StyleUtils.setTransformScale(style, factor, factor);
      return true;
    }

    private void start(Style st) {
      this.style = st;
      super.start();
    }
  }

  private static final String CONTAINER_STYLE = BeeConst.CSS_CLASS_PREFIX + "StackContainer";
  private static final String HEADER_STYLE = BeeConst.CSS_CLASS_PREFIX + "StackHeader";
  private static final String CONTENT_STYLE = BeeConst.CSS_CLASS_PREFIX + "StackContent";

  private static final String SELECTED_STYLE = HEADER_STYLE + "-selected";

  private static final String KEY_MIN_HEIGHT = "min-height";

  public static void setMinHeight(Widget widget, int height) {
    Assert.notNull(widget);
    DomUtils.setDataProperty(widget.getElement(), KEY_MIN_HEIGHT, height);
  }

  private final Revelation revelation = new Revelation(200);

  private int selectedIndex = BeeConst.UNDEF;

  private int minContentHeight;

  public Stack() {
    setElement(Document.get().createDivElement());

    setStyleName(CONTAINER_STYLE);
    DomUtils.createId(this, getIdPrefix());
  }

  @Override
  public void add(Widget w) {
    Assert.unsupported("Single-argument add() is not supported for Stack");
  }

  public IdentifiableWidget add(Widget widget, String header, int headerSize) {
    return insert(widget, header, headerSize, getStackSize());
  }

  public IdentifiableWidget add(Widget widget, Widget header, int headerSize) {
    return insert(widget, header, headerSize, getStackSize());
  }

  @Override
  public HandlerRegistration addBeforeSelectionHandler(BeforeSelectionHandler<Integer> handler) {
    return addHandler(handler, BeforeSelectionEvent.getType());
  }

  @Override
  public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  @Override
  public void clear() {
    setSelectedIndex(BeeConst.UNDEF);
    while (!isEmpty()) {
      remove(0);
    }
  }

  public void doLayout(boolean animate) {
    if (isAttached()) {
      layoutChildren(animate);
      resizeVisibleChildren();
    }
  }

  public int getContentIndex(Widget content) {
    int index = super.getWidgetIndex(content);
    return (index > 0) ? (index - 1) / 2 : BeeConst.UNDEF;
  }

  public Widget getContentWidget(int index) {
    checkIndex(index);
    return super.getWidget(index * 2 + 1);
  }

  public Widget getHeaderWidget(int index) {
    checkIndex(index);
    return getHeader(index).getWidget();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "stack";
  }

  public int getMinContentHeight() {
    return minContentHeight;
  }

  public int getSelectedIndex() {
    return selectedIndex;
  }

  public int getStackSize() {
    return super.getWidgetCount() / 2;
  }

  public Widget getVisibleWidget() {
    if (isOpen()) {
      return getContentWidget(getSelectedIndex());
    } else {
      return null;
    }
  }

  public IdentifiableWidget insert(Widget child, String text, int headerSize, int before) {
    Label contents = new Label(text);
    return insert(child, contents, headerSize, before);
  }

  public IdentifiableWidget insert(Widget child, Widget headerWidget, int headerSize, int before) {
    Header header = new Header(headerWidget, headerSize);
    insert(child, header, before);
    return header;
  }

  public boolean isEmpty() {
    return getStackSize() == 0;
  }

  public boolean isOpen() {
    return isIndex(getSelectedIndex());
  }

  @Override
  public void onResize() {
    if (isOpen()) {
      layoutChildren(false);
      resizeVisibleChildren();
    }
  }

  private void resizeVisibleChildren() {
    for (Widget child : getChildren()) {
      if (child instanceof RequiresResize && DomUtils.isVisible(child)) {
        ((RequiresResize) child).onResize();
      }
    }
  }

  @Override
  public boolean remove(int index) {
    return remove(getContentWidget(index));
  }

  @Override
  public boolean remove(Widget child) {
    if (child instanceof Header) {
      return super.remove(child);
    }

    int index = getContentIndex(child);
    if (BeeConst.isUndef(index)) {
      return false;
    }

    boolean removed = super.remove(getHeader(index));
    if (removed) {
      removed = super.remove(child);
    }

    if (removed) {
      if (getSelectedIndex() == index) {
        setSelectedIndex(BeeConst.UNDEF);
      } else {
        if (index < getSelectedIndex()) {
          setSelectedIndex(getSelectedIndex() - 1);
        }
      }
      doLayout(false);
    }
    return removed;
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setMinContentHeight(int minContentHeight) {
    this.minContentHeight = minContentHeight;
  }

  public void showWidget(int index) {
    showWidget(index, true);
  }

  public void showWidget(int index, boolean fireEvents) {
    checkIndex(index);
    if (index == getSelectedIndex()) {
      return;
    }

    if (fireEvents) {
      BeforeSelectionEvent<Integer> event = BeforeSelectionEvent.fire(this, index);
      if ((event != null) && event.isCanceled()) {
        return;
      }
    }

    if (isOpen()) {
      hideContent(getSelectedIndex());
    }

    setSelectedIndex(index);
    doLayout(true);

    if (fireEvents) {
      SelectionEvent.fire(this, index);
    }
  }

  public void showWidget(Widget child) {
    showWidget(getContentIndex(child));
  }

  public boolean updateHeaderSize(int size) {
    boolean updated = false;

    for (int i = 0; i < getStackSize(); i++) {
      updated |= getHeader(i).updateSize(size);
    }

    if (updated) {
      doLayout(false);
    }

    return updated;
  }

  protected boolean close() {
    if (isOpen()) {
      hideContent(getSelectedIndex());
      setSelectedIndex(BeeConst.UNDEF);

      layoutChildren(false);
      return true;
    } else {
      return false;
    }
  }

  protected State onHeaderClick(Widget child) {
    int index = getContentIndex(child);
    if (!isIndex(index)) {
      return null;
    }

    if (index == getSelectedIndex()) {
      close();
      return State.CLOSED;

    } else {
      showWidget(index);
      return State.OPEN;
    }
  }

  @Override
  protected void onLoad() {
    layoutChildren(false);
    super.onLoad();
  }

  private void checkIndex(int index) {
    Assert.betweenExclusive(index, 0, getStackSize(), "Index out of bounds");
  }

  private Header getHeader(int index) {
    return (Header) super.getWidget(index * 2);
  }

  private void hideContent(int index) {
    getContentWidget(index).setVisible(false);
  }

  private void insert(final Widget child, Header header, int before) {
    Assert.betweenInclusive(before, 0, getStackSize(), "before index out of bounds");

    child.setVisible(false);

    header.addStyleName(HEADER_STYLE);
    child.addStyleName(CONTENT_STYLE);

    Style style = child.getElement().getStyle();
    style.setPosition(Position.ABSOLUTE);
    StyleUtils.setLeft(style, 0);
    StyleUtils.setRight(style, 0);

    header.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Stack.this.onHeaderClick(child);
      }
    });

    super.insert(header, Element.as(getElement()), before * 2, true);
    super.insert(child, Element.as(getElement()), before * 2 + 1, true);

    if (before >= 0 && before <= getSelectedIndex()) {
      setSelectedIndex(getSelectedIndex() + 1);
    }
    doLayout(false);
  }

  private boolean isIndex(int index) {
    return index >= 0 && index < getStackSize();
  }

  private void layoutChildren(boolean animate) {
    if (isEmpty()) {
      return;
    }

    int top = 0;
    int bottom = 0;

    if (isOpen()) {
      for (int i = 0; i <= getSelectedIndex(); i++) {
        Header header = getHeader(i);
        Style style = header.getElement().getStyle();

        StyleUtils.setTop(style, top);
        style.clearBottom();

        top += header.getSize();
      }

      Widget widget = getVisibleWidget();

      int offsetHeight = getOffsetHeight();

      int minHeight = BeeUtils.unbox(DomUtils.getDataPropertyInt(widget.getElement(),
          KEY_MIN_HEIGHT));
      if (minHeight <= 0) {
        minHeight = getMinContentHeight();
      }

      if (offsetHeight > 0 && minHeight > 0) {
        int h = 0;
        for (int i = getStackSize() - 1; i > getSelectedIndex(); i--) {
          h += getHeader(i).getSize();
        }

        int diff = offsetHeight - top - h - minHeight;
        if (diff < 0) {
          bottom = diff;
        }
      }

      for (int i = getStackSize() - 1; i > getSelectedIndex(); i--) {
        Header header = getHeader(i);
        Style style = header.getElement().getStyle();

        style.clearTop();
        StyleUtils.setBottom(style, bottom);

        bottom += header.getSize();
      }

      Style style = widget.getElement().getStyle();

      StyleUtils.setTop(style, top);
      StyleUtils.setBottom(style, bottom);

      if (animate) {
        StyleUtils.setTransformScale(style, Revelation.FROM, Revelation.FROM);
        widget.setVisible(true);
        revelation.start(style);
      } else {
        widget.setVisible(true);
      }

    } else {
      for (int i = 0; i < getStackSize(); i++) {
        Header header = getHeader(i);
        Style style = header.getElement().getStyle();

        StyleUtils.setTop(style, top);
        style.clearBottom();

        top += header.getSize();
      }
    }
  }

  private void setSelectedIndex(int selectedIndex) {
    if (this.selectedIndex == selectedIndex) {
      return;
    }

    if (isIndex(this.selectedIndex)) {
      getHeaderWidget(this.selectedIndex).removeStyleName(SELECTED_STYLE);
    }

    this.selectedIndex = selectedIndex;

    if (isIndex(selectedIndex)) {
      getHeaderWidget(selectedIndex).addStyleName(SELECTED_STYLE);
    }
  }
}
