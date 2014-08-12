package com.butent.bee.client.layout;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
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
import com.google.gwt.event.dom.client.HasAllDragAndDropHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.WidgetCollection;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;

public class Split extends ComplexPanel implements RequiresResize, ProvidesResize,
    IdentifiableWidget, HasExtendedInfo, HasAllDragAndDropHandlers {

  protected static final class LayoutData {

    private Direction direction;
    private int size;

    private LayoutData(Direction direction, int size) {
      super();
      this.direction = direction;
      this.size = Math.max(size, 0);
    }

    public Direction getDirection() {
      return direction;
    }

    public int getSize() {
      return size;
    }

    public void setDirection(Direction direction) {
      this.direction = direction;
    }

    public void setSize(int size) {
      this.size = Math.max(size, 0);
    }
  }

  protected abstract class Splitter extends CustomDiv implements HasInfo {

    private final int size;
    private final boolean reverse;

    private int minSize;
    private int maxSize;

    private boolean mouseDown;
    private int position;
    private Widget target;

    private Splitter(int size, boolean reverse) {
      super();

      this.size = size;
      this.reverse = reverse;

      this.minSize = size * 2;

      addStyleName("bee-Splitter");

      sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONMOUSEMOVE);
    }

    @Override
    public List<Property> getInfo() {
      return PropertyUtils.createProperties(
          "Splitter Size", getSize(),
          "Reverse", isReverse(),
          "Min Size", getMinSize());
    }

    public int getMaxSize() {
      return maxSize;
    }

    public int getMinSize() {
      return minSize;
    }

    public Widget getTarget() {
      return target;
    }

    @Override
    public void onBrowserEvent(Event event) {
      switch (event.getTypeInt()) {
        case Event.ONMOUSEDOWN:
          if (startResizing()) {
            setMouseDown(true);
            setPosition(getEventPosition(event));

            Event.setCapture(getElement());

            event.preventDefault();
            event.stopPropagation();
          }
          break;

        case Event.ONMOUSEUP:
          if (isMouseDown()) {
            setMouseDown(false);
            Event.releaseCapture(getElement());

            event.preventDefault();
            event.stopPropagation();

            endResizing();
          }
          break;

        case Event.ONMOUSEMOVE:
          if (isMouseDown()) {
            event.preventDefault();
            event.stopPropagation();

            int p = getEventPosition(event);
            int by = p - getPosition();

            if (by != 0) {
              setPosition(p);
              Split.this.onSplitterMove(this, isReverse() ? -by : by);
            }
          }
          break;
      }
    }

    protected abstract int getEventPosition(Event event);

    protected abstract int getSize(Widget widget);

    private void endResizing() {
      setTarget(null);
      Split.this.resizeChildren();
    }

    private int getPosition() {
      return position;
    }

    private int getSize() {
      return size;
    }

    private boolean isMouseDown() {
      return mouseDown;
    }

    private boolean isReverse() {
      return reverse;
    }

    private void setMaxSize(int maxSize) {
      this.maxSize = maxSize;
    }

    private void setMinSize(int minSize) {
      this.minSize = minSize;
    }

    private void setMouseDown(boolean mouseDown) {
      this.mouseDown = mouseDown;
    }

    private void setPosition(int position) {
      this.position = position;
    }

    private void setTarget(Widget target) {
      this.target = target;
    }

    private boolean startResizing() {
      if (Split.this.getCenter() == null) {
        return false;
      }

      int index = Split.this.getWidgetIndex(this);
      if (index <= 0) {
        return false;
      }

      setTarget(Split.this.getWidget(index - 1));

      int targetSize = getSize(getTarget());
      int centerSize = getSize(Split.this.getCenter().asWidget());
      setMaxSize(targetSize + centerSize - size * 2);

      return true;
    }
  }

  private final class HorizontalSplitter extends Splitter {

    private HorizontalSplitter(int size, boolean reverse) {
      super(size, reverse);

      StyleUtils.setWidth(getElement(), size);
      addStyleName("bee-HSplitter");
    }

    @Override
    public int getEventPosition(Event event) {
      return event.getClientX();
    }

    @Override
    public String getIdPrefix() {
      return "h-splitter";
    }

    @Override
    public int getSize(Widget widget) {
      return widget.getOffsetWidth();
    }
  }

  private final class VerticalSplitter extends Splitter {

    private VerticalSplitter(int size, boolean reverse) {
      super(size, reverse);

      StyleUtils.setHeight(getElement(), size);
      addStyleName("bee-VSplitter");
    }

    @Override
    public int getEventPosition(Event event) {
      return event.getClientY();
    }

    @Override
    public String getIdPrefix() {
      return "v-splitter";
    }

    @Override
    public int getSize(Widget widget) {
      return widget.getOffsetHeight();
    }
  }

  protected static final CssUnit UNIT = CssUnit.PX;

  private static final int DEFAULT_SPLITTER_SIZE = 8;

  private static final String STYLE_NAME = "bee-Split";
  private static final String STYLE_CHILD = STYLE_NAME + "Child";
  private static final String STYLE_HIDDEN = STYLE_NAME + "-hidden";

  public static boolean validDirection(Direction direction, boolean allowCenter) {
    if (direction == null) {
      return false;
    } else if (direction == Direction.CENTER) {
      return allowCenter;
    } else {
      return true;
    }
  }

  private final int splitterSize;

  private IdentifiableWidget center;

  public Split() {
    this(DEFAULT_SPLITTER_SIZE);
  }

  public Split(int splitterSize) {
    setElement(Document.get().createDivElement());

    this.splitterSize = splitterSize;

    DomUtils.createId(this, getIdPrefix());
    addStyleName(STYLE_NAME);
  }

  public void add(IdentifiableWidget widget, Direction direction, Integer size, Integer splSize) {
    Assert.notNull(widget);
    Assert.isTrue(validDirection(direction, false));
    Assert.notNull(size);

    int z = (splSize == null) ? getSplitterSize() : BeeUtils.unbox(splSize);
    insert(widget, direction, size, BeeConst.UNDEF, z);
  }

  @Override
  public void add(Widget widget) {
    if (widget instanceof IdentifiableWidget) {
      insert((IdentifiableWidget) widget, Direction.CENTER, 0, BeeConst.UNDEF, BeeConst.UNDEF);
    } else {
      Assert.unsupported("only IdentifiableWidget can be added to Split panel");
    }
  }

  @Override
  public HandlerRegistration addDragEndHandler(DragEndHandler handler) {
    return addBitlessDomHandler(handler, DragEndEvent.getType());
  }

  @Override
  public HandlerRegistration addDragEnterHandler(DragEnterHandler handler) {
    return addBitlessDomHandler(handler, DragEnterEvent.getType());
  }

  @Override
  public HandlerRegistration addDragHandler(DragHandler handler) {
    return addBitlessDomHandler(handler, DragEvent.getType());
  }

  @Override
  public HandlerRegistration addDragLeaveHandler(DragLeaveHandler handler) {
    return addBitlessDomHandler(handler, DragLeaveEvent.getType());
  }

  @Override
  public HandlerRegistration addDragOverHandler(DragOverHandler handler) {
    return addBitlessDomHandler(handler, DragOverEvent.getType());
  }

  @Override
  public HandlerRegistration addDragStartHandler(DragStartHandler handler) {
    return addBitlessDomHandler(handler, DragStartEvent.getType());
  }

  @Override
  public HandlerRegistration addDropHandler(DropHandler handler) {
    return addBitlessDomHandler(handler, DropEvent.getType());
  }

  public void addEast(IdentifiableWidget widget, int size) {
    addEast(widget, size, getSplitterSize());
  }

  public void addEast(IdentifiableWidget widget, int size, int splSize) {
    insert(widget, Direction.EAST, size, BeeConst.UNDEF, splSize);
  }

  public void addNorth(IdentifiableWidget widget, int size) {
    addNorth(widget, size, getSplitterSize());
  }

  public void addNorth(IdentifiableWidget widget, int size, int splSize) {
    insert(widget, Direction.NORTH, size, BeeConst.UNDEF, splSize);
  }

  public void addSouth(IdentifiableWidget widget, int size) {
    addSouth(widget, size, getSplitterSize());
  }

  public void addSouth(IdentifiableWidget widget, int size, int splSize) {
    insert(widget, Direction.SOUTH, size, BeeConst.UNDEF, splSize);
  }

  public void addWest(IdentifiableWidget widget, int size) {
    addWest(widget, size, getSplitterSize());
  }

  public void addWest(IdentifiableWidget widget, int size, int splSize) {
    insert(widget, Direction.WEST, size, BeeConst.UNDEF, splSize);
  }

  public void doLayout() {
    layoutChildren();
    onResize();
  }

  public IdentifiableWidget getCenter() {
    return center;
  }

  public int getCenterHeight() {
    IdentifiableWidget c = getCenter();
    if (c != null) {
      return c.asWidget().getOffsetHeight();
    }

    int y = getOffsetHeight();
    for (Widget w : getChildren()) {
      if (isVertical(w)) {
        y -= w.getOffsetHeight();
      }
    }
    return y;
  }

  public int getCenterWidth() {
    IdentifiableWidget c = getCenter();
    if (c != null) {
      return c.asWidget().getOffsetWidth();
    }

    int x = getOffsetWidth();
    for (Widget w : getChildren()) {
      if (isHorizontal(w)) {
        x -= w.getOffsetWidth();
      }
    }
    return x;
  }

  public List<ExtendedProperty> getDirectionInfo(Direction dir) {
    Assert.notNull(dir);
    List<ExtendedProperty> lst = Lists.newArrayList();

    List<Widget> children = getDirectionChildren(dir);
    int c = children.size();

    PropertyUtils.addExtended(lst, dir.toString(), "Widget Count", c);

    if (c > 0) {
      int i = 0;
      for (Widget w : children) {
        PropertyUtils.appendChildrenToExtended(lst, BeeUtils.progress(++i, c), getChildInfo(w));
      }
    }
    return lst;
  }

  public int getDirectionSize(Direction direction) {
    Assert.isTrue(validDirection(direction, false));
    int result = 0;

    for (Widget w : getChildren()) {
      if (!isSplitter(w) && direction.equals(getWidgetDirection(w))) {
        int size = getWidgetSize(w);
        if (size > 0) {
          result += size;
        }
      }
    }
    return result;
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> lst = Lists.newArrayList();

    PropertyUtils.addChildren(lst, NameUtils.getName(this),
        "Id", getId(),
        "Absolute Left", getAbsoluteLeft(),
        "Absolute Top", getAbsoluteTop(),
        "Offset Height", getOffsetHeight(),
        "Offset Width", getOffsetWidth(),
        "Style Name", getStyleName(),
        "Widget Count", getWidgetCount());

    int i = 0;
    int c = getWidgetCount();
    for (Widget w : getChildren()) {
      String name = BeeUtils.joinWords(BeeUtils.progress(++i, c), getWidgetDirection(w).brief());
      PropertyUtils.appendChildrenToExtended(lst, name, getChildInfo(w));
    }
    return lst;
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "split";
  }

  public int getSplitterSize() {
    return splitterSize;
  }

  public Direction getWidgetDirection(Widget child) {
    LayoutData data = getLayoutData(child);
    return (data == null) ? null : data.getDirection();
  }

  public int getWidgetSize(Widget child) {
    LayoutData data = getLayoutData(child);
    return (data == null) ? 0 : data.getSize();
  }

  public int getWidgetSplitterSize(Widget child) {
    Splitter splitter = getAssociatedSplitter(child);
    if (splitter == null) {
      return 0;
    } else {
      return splitter.getSize();
    }
  }

  @Override
  public void onResize() {
    resizeChildren();
  }

  @Override
  public boolean remove(Widget w) {
    Assert.notNull(w);

    if (isSplitter(w)) {
      return super.remove(w);
    }

    Splitter splitter = getAssociatedSplitter(w);

    boolean removed = super.remove(w);
    if (removed) {
      w.removeStyleName(STYLE_CHILD);
      Direction direction = getWidgetDirection(w);
      if (direction != null) {
        w.removeStyleName(STYLE_NAME + direction.getStyleSuffix());
      }

      if (w == center) {
        setCenter(null);
      }

      if (splitter != null) {
        remove(splitter);
      }
    }
    return removed;
  }

  public void setDirectionSize(Direction direction, int size, boolean doLayout) {
    Assert.isTrue(validDirection(direction, false));

    List<LayoutData> data = Lists.newArrayList();
    for (Widget child : getChildren()) {
      if (!isSplitter(child) && direction.equals(getWidgetDirection(child))) {
        LayoutData ld = getLayoutData(child);

        if (ld != null) {
          data.add(ld);

          if (size > 0) {
            child.removeStyleName(STYLE_HIDDEN);
          } else {
            child.addStyleName(STYLE_HIDDEN);
          }
        }
      }
    }

    if (data.isEmpty()) {
      return;
    }

    if (data.size() == 1 || size <= 0) {
      for (LayoutData ld : data) {
        ld.setSize(size);
      }
    } else {
      for (LayoutData ld : data) {
        ld.setSize(size / data.size());
      }
    }

    if (doLayout) {
      doLayout();
    }
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setWidgetMinSize(Widget child, int minSize) {
    Splitter splitter = getAssociatedSplitter(child);
    if (splitter != null) {
      splitter.setMinSize(minSize);
    }
  }

  public void setWidgetSize(Widget widget, int size) {
    LayoutData data = getLayoutData(widget);

    if (data != null && size != data.getSize()) {
      data.setSize(size);

      if (size > 0) {
        widget.removeStyleName(STYLE_HIDDEN);
      } else {
        widget.addStyleName(STYLE_HIDDEN);
      }

      doLayout();
    }
  }

  public void updateCenter(IdentifiableWidget widget) {
    Assert.notNull(widget);

    IdentifiableWidget w = getCenter();
    if (w != null) {
      remove(w.asWidget());
    }
    add(widget);
  }

  protected void convertToCenter(IdentifiableWidget child) {
    removeAssociatedSplitter(child.asWidget());

    WidgetCollection widgets = getChildren();

    int index = widgets.indexOf(child.asWidget());
    if (index < widgets.size() - 1) {
      widgets.remove(index);
      widgets.add(child.asWidget());
    }

    LayoutData data = getLayoutData(child.asWidget());

    Direction direction = data.getDirection();
    if (direction != null) {
      child.asWidget().removeStyleName(STYLE_NAME + direction.getStyleSuffix());
    }

    data.setDirection(Direction.CENTER);
    data.setSize(0);

    child.asWidget().addStyleName(STYLE_NAME + Direction.CENTER.getStyleSuffix());

    StyleUtils.clearWidth(child.asWidget());
    StyleUtils.clearHeight(child.asWidget());

    setCenter(child);
  }

  protected Splitter getAssociatedSplitter(Widget child) {
    int index = getWidgetIndex(child);
    if (index >= 0 && index < getWidgetCount() - 1) {
      Widget splitter = getWidget(index + 1);
      if (isSplitter(splitter)) {
        return (Splitter) splitter;
      }
    }
    return null;
  }

  protected Widget getAssociatedWidget(Splitter splitter) {
    int index = getWidgetIndex(splitter);
    return (index > 0) ? getWidget(index - 1) : null;
  }

  protected List<Property> getChildInfo(Widget w) {
    List<Property> info = Lists.newArrayList();

    Style style = w.getElement().getStyle();

    PropertyUtils.addProperties(info,
        "Size", getWidgetSize(w),
        "Class", NameUtils.getName(w),
        "Id", DomUtils.getId(w),
        "Style Name", w.getStyleName(),
        "Left", style.getLeft(),
        "Right", style.getRight(),
        "Top", style.getTop(),
        "Bottom", style.getBottom(),
        "Width", style.getWidth(),
        "Height", style.getHeight(),
        "Offset Width", w.getOffsetWidth(),
        "Offset Height", w.getOffsetHeight());

    if (isSplitter(w)) {
      info.addAll(((Splitter) w).getInfo());
    }
    return info;
  }

  protected LayoutData getLayoutData(Widget child) {
    if (child == null) {
      return null;
    }

    Object data = child.getLayoutData();
    if (data instanceof LayoutData) {
      return (LayoutData) data;
    } else {
      return null;
    }
  }

  protected void insert(IdentifiableWidget child, Direction direction, int size,
      int before, int splSize) {

    if (Direction.CENTER.equals(direction)) {
      Assert.isTrue(getCenter() == null, "Only one CENTER widget can bee added to Split panel");
    }

    if (BeeConst.isUndef(before)) {
      super.add(child.asWidget(), Element.as(getElement()));
    } else {
      super.insert(child.asWidget(), Element.as(getElement()), before, true);
    }

    LayoutData data = new LayoutData(direction, size);
    child.asWidget().setLayoutData(data);

    StyleUtils.makeAbsolute(child.asWidget());

    if (!isSplitter(child.asWidget())) {
      child.asWidget().addStyleName(STYLE_CHILD);
      child.asWidget().addStyleName(STYLE_NAME + direction.getStyleSuffix());

      if (Direction.CENTER.equals(direction)) {
        setCenter(child);
        layoutChildren();

      } else {
        if (size <= 0) {
          child.asWidget().addStyleName(STYLE_HIDDEN);
        }
        if (splSize > 0) {
          insertSplitter(direction, splSize, BeeConst.isUndef(before) ? before : before + 1);
        }
      }
    }
  }

  protected void insertSplitter(Direction direction, int size, int before) {
    final Splitter splitter;

    switch (direction) {
      case WEST:
        splitter = new HorizontalSplitter(size, false);
        break;
      case EAST:
        splitter = new HorizontalSplitter(size, true);
        break;
      case NORTH:
        splitter = new VerticalSplitter(size, false);
        break;
      case SOUTH:
        splitter = new VerticalSplitter(size, true);
        break;
      default:
        Assert.untouchable();
        splitter = null;
    }

    insert(splitter, direction, size, before, BeeConst.UNDEF);
  }

  protected boolean isSplitter(Widget w) {
    return w instanceof Splitter;
  }

  protected void layoutChildren() {
    int left = 0;
    int right = 0;

    int top = 0;
    int bottom = 0;

    for (Widget child : getChildren()) {
      LayoutData data = getLayoutData(child);
      Direction direction = data.getDirection();
      int size = data.getSize();

      Style style = child.getElement().getStyle();

      switch (direction) {
        case NORTH:
          StyleUtils.setLeft(style, left, UNIT);
          StyleUtils.setRight(style, right, UNIT);

          StyleUtils.setTop(style, top, UNIT);
          StyleUtils.setHeight(style, size, UNIT);

          top += size;
          break;

        case SOUTH:
          StyleUtils.setLeft(style, left, UNIT);
          StyleUtils.setRight(style, right, UNIT);

          StyleUtils.setBottom(style, bottom, UNIT);
          StyleUtils.setHeight(style, size, UNIT);

          bottom += size;
          break;

        case WEST:
          StyleUtils.setLeft(style, left, UNIT);
          StyleUtils.setWidth(style, size, UNIT);

          StyleUtils.setTop(style, top, UNIT);
          StyleUtils.setBottom(style, bottom, UNIT);

          left += size;
          break;

        case EAST:
          StyleUtils.setRight(style, right, UNIT);
          StyleUtils.setWidth(style, size, UNIT);

          StyleUtils.setTop(style, top, UNIT);
          StyleUtils.setBottom(style, bottom, UNIT);

          right += size;
          break;

        case CENTER:
          break;
      }
    }

    if (getCenter() != null) {
      Style style = getCenter().asWidget().getElement().getStyle();

      StyleUtils.setLeft(style, left, UNIT);
      StyleUtils.setRight(style, right, UNIT);

      StyleUtils.setTop(style, top, UNIT);
      StyleUtils.setBottom(style, bottom, UNIT);
    }
  }

  protected void onSplitterMove(Splitter splitter, int by) {
    LayoutData data = getLayoutData(splitter.getTarget());
    if (data == null) {
      return;
    }

    int z = BeeUtils.clamp(data.getSize() + by, splitter.getMinSize(), splitter.getMaxSize());
    if (z != data.getSize()) {
      data.setSize(z);
      layoutChildren();
    }
  }

  private List<Widget> getDirectionChildren(Direction dir) {
    List<Widget> lst = Lists.newArrayList();

    for (Widget w : getChildren()) {
      if (getWidgetDirection(w) == dir) {
        lst.add(w);
      }
    }
    return lst;
  }

  private boolean isHorizontal(Widget w) {
    Direction dir = getWidgetDirection(w);
    return dir != null && dir.isHorizontal();
  }

  private boolean isVertical(Widget w) {
    Direction dir = getWidgetDirection(w);
    return dir != null && dir.isVertical();
  }

  private boolean removeAssociatedSplitter(Widget child) {
    Splitter splitter = getAssociatedSplitter(child);
    if (splitter == null) {
      return false;
    } else {
      return remove(splitter);
    }
  }

  private void resizeChildren() {
    for (Widget child : getChildren()) {
      if (child instanceof RequiresResize && (child == getCenter() || getWidgetSize(child) > 0)) {
        ((RequiresResize) child).onResize();
      }
    }
  }

  private void setCenter(IdentifiableWidget center) {
    this.center = center;
  }
}
