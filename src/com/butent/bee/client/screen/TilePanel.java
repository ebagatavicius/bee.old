package com.butent.bee.client.screen;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.Historian;
import com.butent.bee.client.Place;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.ActiveWidgetChangeEvent;
import com.butent.bee.client.event.logical.CaptionChangeEvent;
import com.butent.bee.client.event.logical.HasActiveWidgetChangeHandlers;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HandlesHistory;
import com.butent.bee.client.ui.HasWidgetSupplier;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.JsonUtils;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.State;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TilePanel extends Split implements HasCaption, SelectionHandler<String> {

  static final class Tile extends Simple implements HasSelectionHandlers<String>, HandlesHistory,
      HasInfo, HasCaption, CaptionChangeEvent.HasCaptionChangeHandlers,
      HasActiveWidgetChangeHandlers {

    private final List<String> contentSuppliers = new ArrayList<>();
    private int contentIndex = BeeConst.UNDEF;

    private Tile() {
      super();
    }

    private Tile(IdentifiableWidget child) {
      super(child.asWidget());
    }

    @Override
    public HandlerRegistration addActiveWidgetChangeHandler(
        ActiveWidgetChangeEvent.Handler handler) {
      return addHandler(handler, ActiveWidgetChangeEvent.getType());
    }

    @Override
    public HandlerRegistration addCaptionChangeHandler(CaptionChangeEvent.Handler handler) {
      return addHandler(handler, CaptionChangeEvent.getType());
    }

    @Override
    public HandlerRegistration addSelectionHandler(SelectionHandler<String> handler) {
      return addHandler(handler, SelectionEvent.getType());
    }

    @Override
    public String getCaption() {
      Widget content = getWidget();

      if (content instanceof HasCaption) {
        return ((HasCaption) content).getCaption();

      } else if (content instanceof View) {
        Presenter presenter = ((View) content).getViewPresenter();
        if (presenter != null) {
          return presenter.getCaption();
        }
      }
      return null;
    }

    @Override
    public String getIdPrefix() {
      return "tile";
    }

    @Override
    public List<Property> getInfo() {
      List<Property> info = new ArrayList<>();

      if (!contentSuppliers.isEmpty()) {
        info.add(new Property("Content Suppliers", BeeUtils.bracket(contentSuppliers.size())));
        for (int i = 0; i < contentSuppliers.size(); i++) {
          info.add(new Property("Supplier " + i, contentSuppliers.get(i)));
        }
        info.add(new Property("Content Index", BeeUtils.toString(getContentIndex())));
      }
      return info;
    }

    @Override
    public boolean onHistory(final Place place, boolean forward) {
      if (contentSuppliers.isEmpty()) {
        return false;
      }

      int oldIndex = getContentIndex();
      int size = contentSuppliers.size();

      if (Historian.getSize() > 1) {
        if (forward) {
          if (oldIndex == size - 1) {
            return false;
          }
        } else {
          if (oldIndex == 0) {
            return false;
          }
        }
      }

      final int newIndex;
      if (forward) {
        newIndex = BeeUtils.rotateForwardExclusive(oldIndex, 0, size);
      } else {
        newIndex = BeeUtils.rotateBackwardExclusive(oldIndex, 0, size);
      }

      if (newIndex == oldIndex) {
        return false;
      }

      ViewFactory.create(contentSuppliers.get(newIndex), new ViewCallback() {
        @Override
        public void onFailure(String... reason) {
          contentSuppliers.remove(newIndex);
          if (getContentIndex() >= newIndex) {
            setContentIndex(getContentIndex() - 1);
          }
        }

        @Override
        public void onSuccess(View result) {
          setContentIndex(newIndex);
          updateContent(result, false);
        }
      });

      return true;
    }

    @Override
    public boolean remove(Widget w) {
      if (getWidget() instanceof HandlesStateChange) {
        ((HandlesStateChange) getWidget()).onStateChange(State.REMOVED);
      }
      return super.remove(w);
    }

    @Override
    protected void init() {
      super.init();
      addStyleName("bee-Tile");
    }

    @Override
    protected void onUnload() {
      Historian.remove(getId());
      super.onUnload();
    }

    void activate(boolean updateHistory) {
      setActiveStyle(true);

      SelectionEvent.fire(this, getId());

      if (updateHistory) {
        Historian.goTo(getId());
      }

      activateContent();
      CaptionChangeEvent.fire(this, getCaption());
    }

    void activateContent() {
      IdentifiableWidget content = getContent();
      if (content instanceof HandlesStateChange) {
        ((HandlesStateChange) content).onStateChange(State.ACTIVATED);
      }

      ActiveWidgetChangeEvent.fireActivate(this, content);
    }

    void blank() {
      setWidget(null);
      setContentIndex(BeeConst.UNDEF);
    }

    IdentifiableWidget getContent() {
      return (getWidget() == null) ? null : (IdentifiableWidget) getWidget();
    }

    TilePanel getPanel() {
      return (TilePanel) getParent();
    }

    boolean isBlank() {
      return getWidget() == null;
    }

    void updateContent(IdentifiableWidget widget, boolean addSupplier) {
      String oldCaption = getCaption();
      setWidget(widget);

      if (addSupplier) {
        if (widget instanceof HasWidgetSupplier) {
          addContentSupplier(((HasWidgetSupplier) widget).getSupplierKey());
        } else {
          setContentIndex(BeeConst.UNDEF);
        }
      }

      activateContent();

      String newCaption = getCaption();
      if (!BeeUtils.equalsTrim(oldCaption, newCaption)) {
        CaptionChangeEvent.fire(this, newCaption);
      }
    }

    private void addContentSupplier(String key) {
      if (!BeeUtils.isEmpty(key)) {
        if (contentSuppliers.contains(key)) {
          contentSuppliers.remove(key);
        }
        contentSuppliers.add(key);

        setContentIndex(contentSuppliers.size() - 1);
      }
    }

    private void deactivate() {
      setActiveStyle(false);
    }

    private int getContentIndex() {
      return contentIndex;
    }

    private String getContentSupplier() {
      return BeeUtils.getQuietly(contentSuppliers, getContentIndex());
    }

    private void setActiveStyle(boolean add) {
      setStyleName("bee-Tile-active", add);
    }

    private void setContentIndex(int contentIndex) {
      this.contentIndex = contentIndex;
    }
  }

  private static final class Boundary extends Position {
    private final int width;
    private final int height;

    private final Position position;

    private Boundary(int width, int height, Position position) {
      super();
      this.width = width;
      this.height = height;
      this.position = position;
    }

    private void apply(Widget widget, Direction direction, int size, int margin) {
      Position p = computePosition(direction, size);
      p.plus(getPosition());

      Style style = widget.getElement().getStyle();

      StyleUtils.setLeft(style, p.getLeft() + margin, UNIT);
      StyleUtils.setRight(style, p.getRight() + margin, UNIT);

      StyleUtils.setTop(style, p.getTop() + margin, UNIT);
      StyleUtils.setBottom(style, p.getBottom() + margin, UNIT);

      increment(direction, size);
    }

    private Position computePosition(Direction direction, int size) {
      Position p = new Position();

      switch (direction) {
        case NORTH:
          p.setLeft(getLeft());
          p.setRight(getRight());

          p.setTop(getTop());
          p.setBottom(getHeight() - getTop() - size);
          break;

        case SOUTH:
          p.setLeft(getLeft());
          p.setRight(getRight());

          p.setTop(getHeight() - getBottom() - size);
          p.setBottom(getBottom());
          break;

        case WEST:
          p.setLeft(getLeft());
          p.setRight(getWidth() - getLeft() - size);

          p.setTop(getTop());
          p.setBottom(getBottom());
          break;

        case EAST:
          p.setLeft(getWidth() - getRight() - size);
          p.setRight(getRight());

          p.setTop(getTop());
          p.setBottom(getBottom());
          break;

        case CENTER:
          p.setLeft(getLeft());
          p.setRight(getRight());

          p.setTop(getTop());
          p.setBottom(getBottom());
          break;
      }

      return p;
    }

    private int getHeight() {
      return height;
    }

    private Position getPosition() {
      return position;
    }

    private int getRemainingHeight() {
      return getHeight() - getTop() - getBottom();
    }

    private int getRemainingWidth() {
      return getWidth() - getLeft() - getRight();
    }

    private int getWidth() {
      return width;
    }

    private void increment(Direction direction, int size) {
      switch (direction) {
        case NORTH:
          setTop(getTop() + size);
          break;

        case SOUTH:
          setBottom(getBottom() + size);
          break;

        case WEST:
          setLeft(getLeft() + size);
          break;

        case EAST:
          setRight(getRight() + size);
          break;

        case CENTER:
          break;
      }
    }
  }

  private final class ContentRestorer implements Consumer<Boolean> {

    private final Map<String, String> contentByTile;
    private final List<String> tileIds;

    private final Consumer<Boolean> callback;

    private final Map<String, HandlerRegistration> readyHandlerRegistry = new HashMap<>();

    private int position;

    private ContentRestorer(Map<String, String> contentByTile, Consumer<Boolean> callback) {
      this.contentByTile = contentByTile;
      this.tileIds = new ArrayList<>(contentByTile.keySet());

      this.callback = callback;
    }

    @Override
    public void accept(Boolean input) {
      if (BeeUtils.isTrue(input) && getPosition() < tileIds.size() - 1) {
        setPosition(getPosition() + 1);
        run();
      } else {
        onComplete(input);
      }
    }

    private int getPosition() {
      return position;
    }

    private void onComplete(Boolean success) {
      EventUtils.clearRegistry(readyHandlerRegistry.values());

      afterRestore();

      if (callback != null) {
        callback.accept(success);
      }
    }

    private void run() {
      final String tileId = tileIds.get(getPosition());
      final String contentSupplier = contentByTile.get(tileId);

      ViewFactory.create(contentSupplier, new ViewCallback() {
        @Override
        public void onSuccess(View result) {
          HandlerRegistration registration = result.addReadyHandler(new ReadyEvent.Handler() {
            @Override
            public void onReady(ReadyEvent event) {
              HandlerRegistration hr = readyHandlerRegistry.remove(tileId);
              if (hr != null) {
                hr.removeHandler();
              }

              logger.info("restored tile", BeeUtils.progress(tileIds.indexOf(tileId) + 1,
                  tileIds.size()), contentSupplier);
              accept(true);
            }
          });

          if (registration != null) {
            readyHandlerRegistry.put(tileId, registration);
          }

          Tile tile = getTileById(tileId);
          if (tile == null) {
            onComplete(false);
          } else {
            tile.addContentSupplier(contentSupplier);
            tile.setWidget(result);
          }
        }
      });
    }

    private void setPosition(int position) {
      this.position = position;
    }

    private void start() {
      setPosition(0);
      run();
    }
  }

  private static class Position {
    private int left;
    private int right;

    private int top;
    private int bottom;

    protected Position() {
      super();
    }

    protected int getBottom() {
      return bottom;
    }

    protected int getLeft() {
      return left;
    }

    protected int getRight() {
      return right;
    }

    protected int getTop() {
      return top;
    }

    protected void plus(Position other) {
      if (other != null) {
        setLeft(getLeft() + other.getLeft());
        setRight(getRight() + other.getRight());

        setTop(getTop() + other.getTop());
        setBottom(getBottom() + other.getBottom());
      }
    }

    protected void setBottom(int bottom) {
      this.bottom = bottom;
    }

    protected void setLeft(int left) {
      this.left = left;
    }

    protected void setRight(int right) {
      this.right = right;
    }

    protected void setTop(int top) {
      this.top = top;
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(TilePanel.class);

  static final int MIN_SIZE = 20;
  static final int TILE_MARGIN = 2;

  private static final String KEY_ACTIVE = "active";
  private static final String KEY_CONTENT = "content";
  private static final String KEY_SPLIT = "split";

  static Tile getTile(Widget widget) {
    for (Widget parent = widget; parent != null; parent = parent.getParent()) {
      if (parent instanceof Tile) {
        return (Tile) parent;
      }
    }
    logger.warning("tile not found");
    return null;
  }

  private String activeTileId;
  private String pendingTileId;

  private final Map<String, List<String>> tree = new HashMap<>();
  private String rootId;

  TilePanel(Workspace workspace) {
    super(5);
    addStyleName("bee-TilePanel");

    init(workspace);
  }

  @Override
  public String getCaption() {
    String caption = getActiveTile().getCaption();
    if (!BeeUtils.isEmpty(caption)) {
      return caption;
    }

    for (Widget child : getChildren()) {
      if (child instanceof Tile && !DomUtils.idEquals(child, getActiveTileId())) {
        caption = ((Tile) child).getCaption();
        if (!BeeUtils.isEmpty(caption)) {
          break;
        }
      }
    }
    return caption;
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> result = super.getExtendedInfo();

    String name = NameUtils.getName(this);

    if (!BeeUtils.isEmpty(getActiveTileId())) {
      result.add(new ExtendedProperty(name, "Active Tile Id", getActiveTileId()));
    }
    if (!BeeUtils.isEmpty(getRootId())) {
      result.add(new ExtendedProperty(name, "Root Id", getRootId()));
    }

    if (!tree.isEmpty()) {
      result.add(new ExtendedProperty(name, "Tree", BeeUtils.bracket(tree.size())));
      for (Map.Entry<String, List<String>> entry : tree.entrySet()) {
        result.add(new ExtendedProperty(name, entry.getKey(), entry.getValue().toString()));
      }
    }

    JSONObject json = toJson();
    if (json != null) {
      result.add(new ExtendedProperty(name, "Json", json.toString()));
    }

    return result;
  }

  @Override
  public String getIdPrefix() {
    return "tile-panel";
  }

  @Override
  public void onResize() {
    layoutChildren();
    super.onResize();
  }

  @Override
  public void onSelection(SelectionEvent<String> event) {
    String id = event.getSelectedItem();
    if (id.equals(getActiveTileId())) {
      return;
    }

    getActiveTile().deactivate();
    setActiveTileId(id);
  }

  @Override
  public boolean remove(Widget w) {
    boolean removed = super.remove(w);

    if (removed && w instanceof Tile && !tree.isEmpty()) {
      String id = ((Tile) w).getId();

      LayoutData data = getLayoutData(w);
      Direction direction = data.getDirection();
      Orientation orientation = direction.getOrientation();
      int size = data.getSize();

      if (tree.containsKey(id)) {
        List<String> children = tree.get(id);

        if (!direction.isCenter()) {
          size += getChildrenSize(children, orientation);
        }

        int childCount = children.size();
        String substId = children.get(childCount - 1);

        if (childCount > 1) {
          List<String> rest = new ArrayList<>(children.subList(0, childCount - 1));
          if (tree.containsKey(substId)) {
            rest.addAll(tree.get(substId));
          }
          tree.put(substId, rest);
        }
        tree.remove(id);

        Tile substitute = getTileById(substId);

        if (id.equals(getRootId())) {
          setRootId(substId);
          convertToCenter(substitute);

        } else {
          String parentId = getParentTileId(id);
          List<String> siblings = tree.get(parentId);
          siblings.set(siblings.indexOf(id), substId);

          if (tree.containsKey(substId)) {
            size -= getChildrenSize(tree.get(substId), orientation);
          }

          LayoutData substData = getLayoutData(substitute);
          substData.setDirection(direction);
          substData.setSize(size);

          Splitter splitter = getAssociatedSplitter(substitute);
          if (splitter != null) {
            int index = getWidgetIndex(splitter);
            super.remove(splitter);
            insertSplitter(direction, getSplitterSize(), index);
          }
        }

      } else {
        String parentId = getParentTileId(id);

        Tile parentTile = getTileById(parentId);
        LayoutData parentData = getLayoutData(parentTile);

        if (orientation != null && orientation.equals(parentData.getDirection().getOrientation())) {
          parentData.setSize(parentData.getSize() + size + getSplitterSize());
        }

        tree.get(parentId).remove(id);
        if (tree.get(parentId).isEmpty()) {
          tree.remove(parentId);
        }
      }

      if (tree.isEmpty()) {
        setRootId(null);
      }
    }

    return removed;
  }

  @Override
  protected List<Property> getChildInfo(Widget w) {
    Style style = w.getElement().getStyle();

    List<Property> info =
        PropertyUtils.createProperties(
            DomUtils.getId(w), "size " + getWidgetSize(w) + " W " + w.getOffsetWidth()
                + " H " + w.getOffsetHeight(),
            "Position", "L " + style.getLeft() + " R " + style.getRight() + " T " + style.getTop()
                + " B " + style.getBottom());

    if (w instanceof Tile) {
      info.addAll(((Tile) w).getInfo());
    }
    return info;
  }

  @Override
  protected void layoutChildren() {
    if (tree.isEmpty()) {
      StyleUtils.occupy(getCenter().asWidget());
    } else {
      int width = getElement().getClientWidth();
      int height = getElement().getClientHeight();

      if (width > MIN_SIZE && height > MIN_SIZE) {
        Boundary boundary = new Boundary(width, height, new Position());
        layoutNode(getRootId(), boundary, TILE_MARGIN);
      }
    }
  }

  @Override
  protected void onSplitterMove(Splitter splitter, int by) {
    if (tree.size() > 1 && splitter.getTarget() instanceof Tile) {
      Tile targetTile = (Tile) splitter.getTarget();

      LayoutData targetData = getLayoutData(targetTile);

      int oldTargetSize = targetData.getSize();
      int newTargetSize = oldTargetSize + by;
      if (newTargetSize < MIN_SIZE) {
        return;
      }

      Orientation orientation = targetData.getDirection().getOrientation();

      String parentTileId = getParentTileId(targetTile.getId());

      LayoutData parentData = null;
      int oldParentSize = 0;

      if (!BeeUtils.isEmpty(parentTileId)) {
        Tile parentTile = getTileById(parentTileId);
        parentData = getLayoutData(parentTile);

        if (orientation.equals(parentData.getDirection().getOrientation())) {
          oldParentSize = parentData.getSize();
          int newParentSize = oldParentSize - by;
          if (oldParentSize <= 0 || newParentSize < MIN_SIZE) {
            return;
          }
          parentData.setSize(newParentSize);

        } else if (Orientation.HORIZONTAL.equals(orientation)) {
          if (parentTile.getOffsetWidth() - by < MIN_SIZE) {
            return;
          }

        } else if (Orientation.VERTICAL.equals(orientation)) {
          if (parentTile.getOffsetHeight() - by < MIN_SIZE) {
            return;
          }
        }
      }

      targetData.setSize(newTargetSize);
      layoutChildren();

      int minSize = MIN_SIZE - TILE_MARGIN * 2;
      boolean ok = true;

      for (Widget child : getChildren()) {
        if (child instanceof Tile) {
          if (Orientation.HORIZONTAL.equals(orientation) && child.getOffsetWidth() < minSize
              || Orientation.VERTICAL.equals(orientation) && child.getOffsetHeight() < minSize) {
            ok = false;
            break;
          }
        }
      }

      if (!ok) {
        targetData.setSize(oldTargetSize);
        if (oldParentSize > 0) {
          parentData.setSize(oldParentSize);
        }
        layoutChildren();
      }

    } else {
      super.onSplitterMove(splitter, by);
    }
  }

  boolean addTile(Workspace workspace, Direction direction) {
    Tile activeTile = getActiveTile();
    if (activeTile == null) {
      return false;
    }

    int size = direction.isHorizontal()
        ? activeTile.getOffsetWidth() : activeTile.getOffsetHeight();
    size = (size + TILE_MARGIN * 2 - getSplitterSize()) / 2;

    if (size < MIN_SIZE) {
      Global.showError("Sub-Planck length is not allowed in this universe");
      return false;

    } else {
      Tile newTile = addTile(workspace, activeTile, direction, size);
      newTile.activate(true);
      return true;
    }
  }

  void afterRestore() {
    Tile tile;

    if (!BeeUtils.isEmpty(getPendingTileId())) {
      tile = getTileById(getPendingTileId());
      setPendingTileId(null);

    } else {
      tile = getActiveTile();
    }

    if (tile != null) {
      tile.activate(false);
    }
  }

  void clear(Workspace workspace) {
    onRemove();

    super.clear();

    tree.clear();
    setRootId(null);

    init(workspace);
  }

  Tile getActiveTile() {
    return getTileById(getActiveTileId());
  }

  String getActiveTileId() {
    return activeTileId;
  }

  String getBookmarkLabel() {
    List<String> labels = new ArrayList<>();

    if (isBookmarkable()) {
      String label;

      for (Widget child : getChildren()) {
        if (child instanceof Tile) {
          Tile tile = (Tile) child;

          if (BeeUtils.isEmpty(tile.getContentSupplier())) {
            label = null;
          } else {
            label = tile.getCaption();
          }

          if (BeeUtils.isEmpty(label)) {
            LayoutData data = getLayoutData(tile);
            if (data != null && data.getDirection() != null) {
              label = BeeUtils.proper(data.getDirection().name());
            }
          }

          if (!BeeUtils.isEmpty(label)) {
            if (DomUtils.idEquals(tile, getActiveTileId()) && !labels.isEmpty()) {
              labels.add(0, label);
            } else {
              labels.add(label);
            }
          }
        }
      }
    }

    if (labels.isEmpty()) {
      return Localized.getConstants().newTab();
    } else {
      return BeeUtils.joinItems(labels);
    }
  }

  List<IdentifiableWidget> getContentWidgets() {
    List<IdentifiableWidget> result = new ArrayList<>();

    for (Widget child : getChildren()) {
      if (child instanceof Tile) {
        IdentifiableWidget content = ((Tile) child).getContent();
        if (content != null) {
          result.add(content);
        }
      }
    }

    return result;
  }

  Tile getEventTile(Node target) {
    if (target == null) {
      return null;
    }

    for (Widget child : getChildren()) {
      if (child.getElement().isOrHasChild(target)) {
        if (child instanceof Tile) {
          return (Tile) child;

        } else if (child instanceof Splitter) {
          Widget widget = getAssociatedWidget((Splitter) child);
          if (widget instanceof Tile) {
            return (Tile) widget;
          }
        }
        break;
      }
    }
    return null;
  }

  Tile getNearestTile(int index) {
    for (int i = index; i < getWidgetCount(); i++) {
      if (getWidget(i) instanceof Tile) {
        return (Tile) getWidget(i);
      }
    }

    for (int i = getWidgetCount() - 1; i >= 0; i--) {
      if (getWidget(i) instanceof Tile) {
        return (Tile) getWidget(i);
      }
    }
    return null;
  }

  int getTileCount() {
    int count = 0;
    for (Widget child : getChildren()) {
      if (child instanceof Tile) {
        count++;
      }
    }
    return count;
  }

  boolean isBookmarkable() {
    if (getTileCount() > 1) {
      return true;
    } else {
      Tile tile = getActiveTile();
      return tile != null && !tile.isBlank() && !BeeUtils.isEmpty(tile.getContentSupplier());
    }
  }

  void onRemove() {
    for (Widget child : getChildren()) {
      if (child instanceof Tile && !((Tile) child).isBlank()) {
        ((Tile) child).blank();
      }
    }
  }

  Map<String, String> restore(Workspace workspace, JSONObject json) {
    return restore(workspace, json, getActiveTile());
  }

  void restoreContent(Map<String, String> contentByTile, final Consumer<Boolean> callback) {
    if (!BeeUtils.isEmpty(contentByTile)) {
      ContentRestorer restorer = new ContentRestorer(contentByTile, callback);
      restorer.start();

    } else if (callback != null) {
      callback.accept(false);
    }
  }

  void setActiveTileId(String activeTileId) {
    this.activeTileId = activeTileId;
  }

  JSONObject toJson() {
    if (BeeUtils.isEmpty(getRootId()) || tree.isEmpty()) {
      return toJson(getActiveTileId());
    } else {
      return toJson(getRootId());
    }
  }

  private Tile addTile(Workspace workspace, Tile activeTile, Direction direction, int size) {
    LayoutData data = getLayoutData(activeTile);
    if (!data.getDirection().isCenter()
        && direction.isHorizontal() == data.getDirection().isHorizontal()) {
      data.setSize(data.getSize() - size - getSplitterSize());
    }

    Tile tile = createTile(workspace);

    String parentId = activeTile.getId();
    String childId = tile.getId();

    if (BeeUtils.isEmpty(getRootId())) {
      setRootId(parentId);
    }
    if (tree.containsKey(parentId)) {
      tree.get(parentId).add(childId);
    } else {
      tree.put(parentId, Lists.newArrayList(childId));
    }

    insert(tile, direction, size, getWidgetIndex(activeTile), getSplitterSize());
    onResize();

    return tile;
  }

  private Tile createTile(Workspace workspace) {
    Tile tile = new Tile();

    tile.addSelectionHandler(this);

    tile.addActiveWidgetChangeHandler(workspace);
    tile.addCaptionChangeHandler(workspace);

    Historian.add(new Workplace(workspace, tile.getId()));

    return tile;
  }

  private int getChildrenSize(List<String> branch, Orientation orientation) {
    int result = 0;
    if (branch == null) {
      return result;
    }

    for (String tileId : branch) {
      LayoutData data = getLayoutData(getTileById(tileId));
      if (orientation.equals(data.getDirection().getOrientation())) {
        result += data.getSize() + getSplitterSize();
        if (tree.containsKey(tileId)) {
          result += getChildrenSize(tree.get(tileId), orientation);
        }
      }
    }
    return result;
  }

  private int getMaxSize(Direction dir) {
    int max = dir.isHorizontal() ? getOffsetWidth() : getOffsetHeight();
    if (max <= 0 && getParent() != null) {
      max = dir.isHorizontal() ? getParent().getOffsetWidth() : getParent().getOffsetHeight();
    }
    return max;
  }

  private String getParentTileId(String childId) {
    for (Map.Entry<String, List<String>> entry : tree.entrySet()) {
      if (entry.getValue().contains(childId)) {
        return entry.getKey();
      }
    }
    return null;
  }

  private String getPendingTileId() {
    return pendingTileId;
  }

  private String getRootId() {
    return rootId;
  }

  private Tile getTileById(String id) {
    for (Widget child : getChildren()) {
      if (DomUtils.idEquals(child, id)) {
        return (Tile) child;
      }
    }

    logger.severe("tile not available by id:", id);
    return null;
  }

  private void init(Workspace workspace) {
    Tile tile = createTile(workspace);
    tile.setActiveStyle(true);
    add(tile);

    setActiveTileId(tile.getId());
  }

  private void layoutNode(String tileId, Boundary boundary, int margin) {
    Tile tile = getTileById(tileId);

    LayoutData data = getLayoutData(tile);
    Direction direction = data.getDirection();
    int size = data.getSize();

    List<String> branch = tree.get(tileId);
    if (BeeUtils.isEmpty(branch)) {
      boundary.apply(tile, direction, size, margin);

    } else {
      if (!direction.isCenter()) {
        size += getChildrenSize(branch, direction.getOrientation());
      }

      int width = direction.isHorizontal() ? size : boundary.getRemainingWidth();
      int height = direction.isVertical() ? size : boundary.getRemainingHeight();

      Position position = boundary.computePosition(direction, size);
      position.plus(boundary.getPosition());

      Boundary nodeBoundary = new Boundary(width, height, position);

      for (String childId : branch) {
        layoutNode(childId, nodeBoundary, margin);
      }

      nodeBoundary.apply(tile, Direction.CENTER, 0, margin);

      boundary.increment(direction, size);
    }

    if (!direction.isCenter()) {
      Splitter splitter = getAssociatedSplitter(tile);
      if (splitter != null) {
        LayoutData ld = getLayoutData(splitter);
        boundary.apply(splitter, ld.getDirection(), ld.getSize(), 0);
      }
    }
  }

  private Map<String, String> restore(Workspace workspace, JSONObject json, final Tile tile) {
    Map<String, String> contentByTile = new HashMap<>();

    if (json.containsKey(KEY_SPLIT)) {
      JSONArray split = json.get(KEY_SPLIT).isArray();

      if (split != null) {
        for (int i = 0; i < split.size(); i++) {
          JSONObject child = split.get(i).isObject();

          if (child != null && child.containsKey(Workspace.KEY_DIRECTION)
              && child.containsKey(Workspace.KEY_SIZE)) {

            Direction direction = Direction.parse(JsonUtils.getString(child,
                Workspace.KEY_DIRECTION));
            Double size = JsonUtils.getNumber(child, Workspace.KEY_SIZE);

            if (direction != null && !direction.isCenter()) {
              int max = getMaxSize(direction);

              if (BeeUtils.isPositive(size) && max > 0) {
                Tile childTile = addTile(workspace, tile, direction,
                    Workspace.restoreSize(size, max));

                contentByTile.putAll(restore(workspace, child, childTile));
              }
            }
          }
        }
      }
    }

    if (json.containsKey(KEY_CONTENT)) {
      String contentSupplier = JsonUtils.getString(json, KEY_CONTENT);
      if (!BeeUtils.isEmpty(contentSupplier)) {
        contentByTile.put(tile.getId(), contentSupplier);
      }
    }

    if (json.containsKey(KEY_ACTIVE) && BeeUtils.isTrue(JsonUtils.getBoolean(json, KEY_ACTIVE))) {
      setPendingTileId(tile.getId());
    }

    return contentByTile;
  }

  private void setPendingTileId(String pendingTileId) {
    this.pendingTileId = pendingTileId;
  }

  private void setRootId(String rootId) {
    this.rootId = rootId;
  }

  private JSONObject toJson(String tileId) {
    JSONObject json = new JSONObject();

    Tile tile = getTileById(tileId);
    if (tile == null) {
      return json;
    }

    LayoutData data = getLayoutData(tile);
    if (data != null) {
      Direction direction = data.getDirection();
      if (direction != null && !direction.isCenter()) {
        json.put(Workspace.KEY_DIRECTION, new JSONString(direction.brief()));

        int size = data.getSize();
        int max = getMaxSize(direction);

        if (size > 0 && max > 0) {
          json.put(Workspace.KEY_SIZE, new JSONNumber(Workspace.scaleSize(size, max)));
        }
      }
    }

    String contentSupplier = tile.getContentSupplier();
    if (!BeeUtils.isEmpty(contentSupplier)) {
      json.put(KEY_CONTENT, new JSONString(contentSupplier));
    }

    if (tileId.equals(getActiveTileId()) && getTileCount() > 1) {
      json.put(KEY_ACTIVE, JSONBoolean.getInstance(true));
    }

    if (tree.containsKey(tileId)) {
      JSONArray split = new JSONArray();

      List<String> children = tree.get(tileId);
      for (int i = 0; i < children.size(); i++) {
        JSONObject child = toJson(children.get(i));
        if (child != null) {
          split.set(i, child);
        }
      }

      json.put(KEY_SPLIT, split);
    }

    return json;
  }
}
