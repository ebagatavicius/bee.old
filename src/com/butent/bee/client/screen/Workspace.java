package com.butent.bee.client.screen;

import com.google.common.collect.Maps;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.Historian;
import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.PreviewHandler;
import com.butent.bee.client.event.logical.ActiveWidgetChangeEvent;
import com.butent.bee.client.event.logical.CaptionChangeEvent;
import com.butent.bee.client.event.logical.HasActiveWidgetChangeHandlers;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.screen.TilePanel.Tile;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.JsonUtils;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.CustomHasHtml;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.ExtendedPropertiesData;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Workspace extends TabbedPages implements CaptionChangeEvent.Handler,
    HasActiveWidgetChangeHandlers, ActiveWidgetChangeEvent.Handler, PreviewHandler {

  private enum TabAction {
    CREATE(Localized.getConstants().actionWorkspaceNewTab()) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.insertEmptyPanel(index + 1);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return true;
      }
    },

    NORTH(Localized.getConstants().actionWorkspaceNewTop()) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.splitActiveTile(Direction.NORTH);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex();
      }
    },

    SOUTH(Localized.getConstants().actionWorkspaceNewBottom()) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.splitActiveTile(Direction.SOUTH);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex();
      }
    },

    WEST(Localized.getConstants().actionWorkspaceNewLeft()) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.splitActiveTile(Direction.WEST);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex();
      }
    },

    EAST(Localized.getConstants().actionWorkspaceNewRight()) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.splitActiveTile(Direction.EAST);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex();
      }
    },

    MAXIMIZE(Localized.getConstants().actionWorkspaceMaxSize()) {
      @Override
      void execute(Workspace workspace, int index) {
        for (Direction direction : Direction.values()) {
          if (!Direction.CENTER.equals(direction)) {
            workspace.stretch(direction, false);
          }
        }

        workspace.getResizeContainer().doLayout();
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex()
            && (workspace.canGrow(Direction.NORTH) || workspace.canGrow(Direction.SOUTH)
                || workspace.canGrow(Direction.WEST) || workspace.canGrow(Direction.EAST));
      }
    },

    RESTORE(Localized.getConstants().actionWorkspaceRestoreSize()) {
      @Override
      void execute(Workspace workspace, int index) {
        for (Map.Entry<Direction, Integer> entry : workspace.resized.entrySet()) {
          workspace.getResizeContainer().setDirectionSize(entry.getKey(), entry.getValue(), false);
        }

        workspace.resized.clear();
        workspace.getResizeContainer().doLayout();
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex() && !workspace.resized.isEmpty();
      }
    },

    UP(Localized.getConstants().actionWorkspaceEnlargeUp()) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.stretch(Direction.NORTH, true);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex()
            && !workspace.resized.containsKey(Direction.NORTH)
            && workspace.canGrow(Direction.NORTH);
      }
    },

    DOWN(Localized.getConstants().actionWorkspaceEnlargeDown()) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.stretch(Direction.SOUTH, true);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex()
            && !workspace.resized.containsKey(Direction.SOUTH)
            && workspace.canGrow(Direction.SOUTH);
      }
    },

    LEFT(Localized.getConstants().actionWorkspaceEnlargeToLeft()) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.stretch(Direction.WEST, true);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex()
            && !workspace.resized.containsKey(Direction.WEST)
            && workspace.canGrow(Direction.WEST);
      }
    },

    RIGHT(Localized.getConstants().actionWorkspaceEnlargeToRight()) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.stretch(Direction.EAST, true);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex()
            && !workspace.resized.containsKey(Direction.EAST)
            && workspace.canGrow(Direction.EAST);
      }
    },

    CLOSE_TILE(Localized.getConstants().actionWorkspaceCloseTile()) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.close(workspace.getActiveTile());
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        if (index == workspace.getSelectedIndex()) {
          TilePanel panel = workspace.getActivePanel();
          return panel != null && panel.getTileCount() > 1;
        } else {
          return false;
        }
      }
    },

    CLOSE_TAB(Localized.getConstants().actionWorkspaceCloseTab()) {
      @Override
      void execute(Workspace workspace, int index) {
        if (workspace.getPageCount() > 1) {
          workspace.removePage(index);
        } else {
          workspace.clearPage(index);
        }
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        if (workspace.getPageCount() > 1 || index != workspace.getSelectedIndex()) {
          return true;
        } else {
          TilePanel panel = workspace.getActivePanel();
          return panel != null && (panel.getTileCount() > 1 || !panel.getActiveTile().isBlank());
        }
      }
    },

    CLOSE_OTHER(Localized.getConstants().actionWorkspaceCloseOther()) {
      @Override
      void execute(Workspace workspace, int index) {
        while (workspace.getPageCount() > index + 1) {
          workspace.removePage(workspace.getPageCount() - 1);
        }
        while (workspace.getPageCount() > 1) {
          workspace.removePage(0);
        }
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return workspace.getPageCount() > 1;
      }
    },

    CLOSE_RIGHT(Localized.getConstants().actionWorkspaceCloseRight()) {
      @Override
      void execute(Workspace workspace, int index) {
        while (workspace.getPageCount() > index + 1) {
          workspace.removePage(workspace.getPageCount() - 1);
        }
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index < workspace.getPageCount() - 1;
      }
    },

    CLOSE_ALL(Localized.getConstants().actionWorkspaceCloseAll()) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.clear();
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return workspace.getPageCount() > 1;
      }
    },

    BOOKMARK_TAB(Localized.getConstants().actionWorkspaceBookmarkTab()) {
      @Override
      void execute(Workspace workspace, int index) {
        TilePanel panel = workspace.getActivePanel();
        if (panel != null) {
          JSONObject json = panel.toJson();
          workspace.maybeAddResized(json);
          Global.getSpaces().bookmark(panel.getBookmarkLabel(), json.toString());
        }
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        if (index == workspace.getSelectedIndex()) {
          TilePanel panel = workspace.getActivePanel();
          return panel != null && panel.isBookmarkable();
        } else {
          return false;
        }
      }
    },

    BOOKMARK_ALL(Localized.getConstants().actionWorkspaceBookmarkAll()) {
      @Override
      void execute(Workspace workspace, int index) {
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < workspace.getPageCount(); i++) {
          Widget contentPanel = workspace.getContentWidget(i);
          if (contentPanel instanceof TilePanel) {
            labels.add(((TilePanel) contentPanel).getBookmarkLabel());
          }
        }

        Global.getSpaces().bookmark(BeeUtils.joinItems(labels), workspace.toJson().toString());
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return workspace.getPageCount() > 1;
      }
    };

    private static final String STYLE_NAME_PREFIX = Workspace.STYLE_PREFIX + "action-";

    private final IdentifiableWidget widget;

    private TabAction(String label) {
      this.widget = new Label(label);

      this.widget.asWidget().addStyleName(STYLE_NAME_PREFIX
          + this.name().toLowerCase().replace(BeeConst.CHAR_UNDER, BeeConst.CHAR_MINUS));
    }

    abstract void execute(Workspace workspace, int index);

    abstract boolean isEnabled(Workspace workspace, int index);

    private IdentifiableWidget getWidget() {
      return widget;
    }
  }

  private final class TabWidget extends Flow implements HasCaption {

    private TabWidget(String caption) {
      super(getStylePrefix() + "tabWrapper");

      CustomDiv dropDown = new CustomDiv(getStylePrefix() + "dropDown");
      dropDown.setHtml(String.valueOf(BeeConst.DROP_DOWN));
      dropDown.setTitle(Localized.getConstants().tabControl());
      add(dropDown);

      dropDown.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          event.stopPropagation();
          Workspace.this.showActions(TabWidget.this.getId());
        }
      });

      CustomDiv label = new CustomDiv(getStylePrefix() + "caption");
      label.setHtml(caption);
      add(label);

      CustomDiv closeTab = new CustomDiv(getStylePrefix() + "closeTab");
      closeTab.setText(String.valueOf(BeeConst.CHAR_TIMES));
      closeTab.setTitle(Localized.getConstants().actionWorkspaceCloseTab());
      add(closeTab);

      closeTab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          TabAction.CLOSE_TAB.execute(Workspace.this, getTabIndex(TabWidget.this.getId()));
        }
      });
    }

    @Override
    public String getCaption() {
      return getCaptionWidget().getHtml();
    }

    private CustomDiv getCaptionWidget() {
      return (CustomDiv) getWidget(1);
    }

    private void setCaption(String caption) {
      getCaptionWidget().setHtml(caption);
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(Workspace.class);

  private static final String STYLE_PREFIX = StyleUtils.CLASS_NAME_PREFIX + "Workspace-";

  static final String KEY_DIRECTION = "direction";
  static final String KEY_SIZE = "size";

  private static final String KEY_SELECTED = "selected";
  private static final String KEY_RESIZED = "resized";
  private static final String KEY_TABS = "tabs";

  private static final int SIZE_FACTOR = 1_000_000;

  static int restoreSize(double size, int max) {
    return BeeUtils.round(size * max / SIZE_FACTOR);
  }

  static int scaleSize(int size, int max) {
    return BeeUtils.round((double) size * SIZE_FACTOR / max);
  }

  private final Map<Direction, Integer> resized = Maps.newEnumMap(Direction.class);

  Workspace() {
    super(STYLE_PREFIX);

    insertEmptyPanel(0);

    CustomHasHtml newTab = new CustomHasHtml(DomUtils.createElement(Tags.ASIDE),
        getStylePrefix() + "newTab");
    newTab.setHtml(BeeConst.STRING_PLUS);
    newTab.setTitle(Localized.getConstants().newTab());

    newTab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        insertEmptyPanel(getPageCount());
      }
    });

    getTabBar().add(newTab);
  }

  @Override
  public HandlerRegistration addActiveWidgetChangeHandler(ActiveWidgetChangeEvent.Handler handler) {
    return addHandler(handler, ActiveWidgetChangeEvent.getType());
  }

  @Override
  public void clear() {
    while (getPageCount() > 1) {
      removePage(getPageCount() - 1);
    }
    clearPage(0);
  }

  @Override
  public boolean isModal() {
    return false;
  }

  @Override
  public void onActiveWidgetChange(ActiveWidgetChangeEvent event) {
    fireEvent(event);
  }

  @Override
  public void onCaptionChange(CaptionChangeEvent event) {
    if (event.getSource() instanceof Tile) {
      updateCaption((Tile) event.getSource(), event.getCaption());
    }
  }

  @Override
  public void onEventPreview(NativePreviewEvent event, Node targetNode) {
    if (targetNode == null) {
      return;
    }

    TilePanel activePanel = getActivePanel();
    if (activePanel == null || activePanel.getWidgetCount() <= 1
        || !activePanel.getElement().isOrHasChild(targetNode)) {
      return;
    }

    Tile activeTile = activePanel.getActiveTile();
    if (activeTile == null || activeTile.getElement().isOrHasChild(targetNode)) {
      return;
    }

    Tile eventTile = activePanel.getEventTile(targetNode);
    if (eventTile == null || eventTile.getId().equals(activeTile.getId())) {
      return;
    }

    eventTile.activate(true);
  }

  public boolean randomClose(boolean debug) {
    TilePanel panel = getActivePanel();
    int tileCount = panel.getTileCount();
    if (tileCount <= 1) {
      return false;
    }

    List<ExtendedProperty> info = debug ? panel.getExtendedInfo() : null;

    int tileIndex = BeeUtils.randomInt(0, tileCount);

    int index = 0;
    for (Widget child : panel) {
      if (child instanceof Tile) {
        if (index == tileIndex) {
          if (debug) {
            info.add(new ExtendedProperty("close", child.getElement().getId(),
                BeeUtils.joinWords(panel.getWidgetDirection(child), panel.getWidgetSize(child))));
          }

          close((Tile) child);
          break;
        }
        index++;
      }
    }

    if (!debug) {
      return true;
    }

    boolean ok = true;
    int minSize = TilePanel.MIN_SIZE - TilePanel.TILE_MARGIN * 2;

    for (Widget child : panel) {
      if (child instanceof Tile) {
        if (child.getOffsetWidth() < minSize || child.getOffsetHeight() < minSize) {
          info.add(new ExtendedProperty("small", child.getElement().getId(),
              "W " + child.getOffsetWidth() + " H " + child.getOffsetHeight()));
          ok = false;
          break;
        }
      }
    }

    if (!ok) {
      Global.showModalGrid("close error", new ExtendedPropertiesData(info, false));
    }
    return ok;
  }

  public void randomSplit() {
    TilePanel panel = getActivePanel();
    int tileIndex = BeeUtils.randomInt(0, panel.getTileCount());

    int count = 0;
    for (Widget child : panel) {
      if (child instanceof Tile) {
        if (count >= tileIndex) {
          Tile tile = (Tile) child;

          Direction direction;
          int d = BeeUtils.randomInt(0, 4);
          switch (d) {
            case 0:
              direction = Direction.NORTH;
              break;
            case 1:
              direction = Direction.SOUTH;
              break;
            case 2:
              direction = Direction.WEST;
              break;
            case 3:
              direction = Direction.EAST;
              break;
            default:
              Assert.untouchable();
              direction = null;
          }

          int size = direction.isHorizontal() ? tile.getOffsetWidth() : tile.getOffsetHeight();
          size = (size + TilePanel.TILE_MARGIN * 2 - panel.getSplitterSize()) / 2;
          if (size < TilePanel.MIN_SIZE) {
            continue;
          }

          if (!tile.getId().equals(panel.getActiveTileId())) {
            tile.activate(false);
          }
          panel.addTile(this, direction);
          break;
        }

        count++;
      }
    }
  }

  @Override
  public void removePage(int index) {
    Widget widget = getContentWidget(index);
    if (widget instanceof TilePanel) {
      ((TilePanel) widget).onRemove();
    }

    super.removePage(index);
  }

  @Override
  public void selectPage(int index, SelectionOrigin origin) {
    super.selectPage(index, origin);

    if (SelectionOrigin.CLICK.equals(origin) || SelectionOrigin.INSERT.equals(origin)
        || SelectionOrigin.REMOVE.equals(origin)) {
      Tile tile = getActiveTile();

      Historian.goTo(tile.getId());
      tile.activateContent();
    }
  }

  void activateWidget(IdentifiableWidget widget) {
    if (widget == null) {
      return;
    }

    IdentifiableWidget activeContent = getActiveContent();
    if (activeContent != null && widget.getId().equals(activeContent.getId())) {
      return;
    }

    Tile tile = TilePanel.getTile(widget.asWidget());

    int index = getPageIndex(tile);
    if (index != getSelectedIndex()) {
      selectPage(index, SelectionOrigin.SCRIPT);
    }

    tile.activate(false);
  }

  void closeWidget(IdentifiableWidget widget) {
    Tile tile = TilePanel.getTile(widget.asWidget());
    if (tile != null) {
      close(tile);
    }
  }

  IdentifiableWidget getActiveContent() {
    return getActiveTile().getContent();
  }

  Tile getActiveTile() {
    TilePanel panel = getActivePanel();
    if (panel == null) {
      return null;
    }

    return panel.getActiveTile();
  }

  List<IdentifiableWidget> getOpenWidgets() {
    List<IdentifiableWidget> result = new ArrayList<>();

    for (int i = 0; i < getPageCount(); i++) {
      Widget contentPanel = getContentWidget(i);
      if (contentPanel instanceof TilePanel) {
        result.addAll(((TilePanel) contentPanel).getContentWidgets());
      }
    }

    return result;
  }

  int getPageIndex(Tile tile) {
    for (int i = 0; i < getPageCount(); i++) {
      if (getContentWidget(i).getElement().isOrHasChild(tile.getElement())) {
        return i;
      }
    }
    showError("page not found for tile" + tile.getId());
    return BeeConst.UNDEF;
  }

  void onWidgetChange(IdentifiableWidget widget) {
    if (widget != null) {
      Tile tile = TilePanel.getTile(widget.asWidget());
      if (tile != null) {
        updateCaption(tile, null);
      }
    }
  }

  void openInNewPlace(IdentifiableWidget widget) {
    Tile tile = getActiveTile();
    if (tile == null || !tile.isBlank()) {
      int index = getSelectedIndex();
      if (index < 0) {
        index = getPageCount() - 1;
      }
      insertEmptyPanel(index + 1);
    }

    updateActivePanel(widget);
  }

  void restore(String input, boolean append) {
    JSONObject json = JsonUtils.parse(input);
    if (json == null) {
      logger.warning("cannot parse", input);
      return;
    }

    if (!append) {
      clear();
    }

    if (json.containsKey(KEY_RESIZED) && resized.isEmpty()) {
      JSONArray arr = json.get(KEY_RESIZED).isArray();

      if (arr != null) {
        for (int i = 0; i < arr.size(); i++) {
          JSONString string = arr.get(i).isString();
          
          if (string != null) {
            Direction direction = Direction.parse(string.stringValue());

            if (direction != null && !direction.isCenter()) {
              stretch(direction, false);
            }
          }
        }
        
        if (!resized.isEmpty()) {
          getResizeContainer().doLayout();
        }
      }
    }

    TilePanel panel;

    if (json.containsKey(KEY_TABS)) {
      int oldPageCount = append ? getPageCount() : 0;
      JSONArray tabs = json.get(KEY_TABS).isArray();

      if (tabs != null) {
        for (int i = 0; i < tabs.size(); i++) {
          JSONObject child = tabs.get(i).isObject();

          if (child != null) {
            if (i > 0 || append) {
              panel = insertEmptyPanel(getPageCount());
            } else {
              panel = getActivePanel();
            }

            panel.restore(this, child);
          }
        }

        if (json.containsKey(KEY_SELECTED) && getPageCount() == oldPageCount + tabs.size()) {
          Double value = JsonUtils.getNumber(json, KEY_SELECTED);
          int index = (value == null) ? BeeConst.UNDEF : BeeUtils.round(value);

          if (BeeUtils.betweenExclusive(index, 0, tabs.size())) {
            selectPage(oldPageCount + index, SelectionOrigin.SCRIPT);
          }
        }
      }

    } else {
      if (append) {
        panel = insertEmptyPanel(getPageCount());
      } else {
        panel = getActivePanel();
      }

      panel.restore(this, json);
    }
  }

  void showInfo() {
    List<ExtendedProperty> info = new ArrayList<>();

    info.add(new ExtendedProperty("Selected Index", BeeUtils.toString(getSelectedIndex())));

    if (!resized.isEmpty()) {
      for (Map.Entry<Direction, Integer> entry : resized.entrySet()) {
        info.add(new ExtendedProperty("Resized", entry.getKey().name(),
            BeeUtils.toString(entry.getKey())));
      }
    }

    int tabCount = getPageCount();
    info.add(new ExtendedProperty("Page Count", BeeUtils.toString(tabCount)));

    info.add(new ExtendedProperty("Json", toJson().toString()));

    for (int i = 0; i < tabCount; i++) {
      Widget contentPanel = getContentWidget(i);
      if (contentPanel instanceof TilePanel) {
        PropertyUtils.appendWithPrefix(info, BeeUtils.progress(i + 1, tabCount),
            ((TilePanel) contentPanel).getExtendedInfo());
      }
    }

    Global.showModalGrid("Tiles", new ExtendedPropertiesData(info, false));
  }

  void updateActivePanel(IdentifiableWidget widget) {
    if (widget == null) {
      showError("widget is null");
      return;
    }

    Tile tile = getActiveTile();
    if (tile != null) {
      tile.updateContent(widget, true);
    }
  }

  private boolean canGrow(Direction direction) {
    return BeeUtils.isPositive(getSiblingSize(direction));
  }

  private void clearPage(int index) {
    Widget widget = getContentWidget(index);
    if (widget instanceof TilePanel) {
      ((TilePanel) widget).clear(this);
    }
  }

  private void close(Tile tile) {
    TilePanel panel = tile.getPanel();
    int pageIndex = getPageIndex(tile);

    if (panel.getTileCount() > 1) {
      boolean wasActive = tile.getId().equals(panel.getActiveTileId());
      int tileIndex = panel.getWidgetIndex(tile);

      if (!tile.isBlank()) {
        tile.blank();
      }

      panel.remove(tile);

      if (wasActive) {
        Tile nearestTile = panel.getNearestTile(tileIndex);
        panel.setActiveTileId(nearestTile.getId());

        resizePage(pageIndex);

        nearestTile.activate(pageIndex == getSelectedIndex());
      } else {
        resizePage(pageIndex);
      }

    } else if (getPageCount() > 1) {
      removePage(pageIndex);

    } else if (!tile.isBlank()) {
      tile.blank();
      updateCaption(tile, null);
    }
  }

  private TilePanel getActivePanel() {
    Widget widget = getSelectedWidget();
    if (widget instanceof TilePanel) {
      return (TilePanel) widget;
    } else {
      showError("selected panel not available");
      return null;
    }
  }

  private Split getResizeContainer() {
    for (Widget parent = this.getParent(); parent != null; parent = parent.getParent()) {
      if (parent instanceof Split) {
        return (Split) parent;
      }
    }
    return null;
  }

  private Integer getSiblingSize(Direction direction) {
    Split container = getResizeContainer();
    if (container == null) {
      return null;
    }
    return container.getDirectionSize(direction);
  }

  private int getTabIndex(String tabId) {
    for (int i = 0; i < getPageCount(); i++) {
      if (tabId.equals(getTabWidget(i).getElement().getId())) {
        return i;
      }
    }
    return BeeConst.UNDEF;
  }

  private TilePanel insertEmptyPanel(int before) {
    TilePanel panel = new TilePanel(this);
    TabWidget tab = new TabWidget(Localized.getConstants().newTab());

    insert(panel, tab, before);

    selectPage(before, SelectionOrigin.INSERT);
    return panel;
  }

  private void maybeAddResized(JSONObject json) {
    if (!resized.isEmpty()) {
      JSONArray arr = new JSONArray();
      int index = 0;
      
      for (Direction direction : resized.keySet()) {
        if (direction != null) {
          arr.set(index++, new JSONString(direction.brief()));
        }
      }
      
      if (index > 0) {
        json.put(KEY_RESIZED, arr);
      }
    }
  }

  private void showActions(String tabId) {
    final int index = getTabIndex(tabId);
    if (BeeConst.isUndef(index)) {
      showError("tab widget not found, id: " + tabId);
      return;
    }

    final List<TabAction> actions = new ArrayList<>();
    for (TabAction action : TabAction.values()) {
      if (action.isEnabled(this, index)) {
        actions.add(action);
      }
    }

    TabBar bar = new TabBar(STYLE_PREFIX + "actionMenu-", Orientation.VERTICAL);
    for (TabAction action : actions) {
      bar.addItem(action.getWidget().asWidget());
    }

    final Popup popup = new Popup(OutsideClick.CLOSE, STYLE_PREFIX + "actionPopup");

    bar.addSelectionHandler(new SelectionHandler<Integer>() {
      @Override
      public void onSelection(SelectionEvent<Integer> event) {
        popup.close();
        actions.get(event.getSelectedItem()).execute(Workspace.this, index);
      }
    });

    popup.setWidget(bar);
    popup.showRelativeTo(getTabWidget(index).getElement());
  }

  private void showError(String message) {
    logger.severe(getClass().getName(), message);
  }

  private void splitActiveTile(Direction direction) {
    TilePanel panel = getActivePanel();
    if (panel != null) {
      panel.addTile(this, direction);
    }
  }
  
  private void stretch(Direction direction, boolean doLayout) {
    Integer size = getSiblingSize(direction);
    if (BeeUtils.isPositive(size)) {
      getResizeContainer().setDirectionSize(direction, 0, doLayout);
      resized.put(direction, size);
    }
  }

  private JSONObject toJson() {
    JSONObject json = new JSONObject();

    if (getPageCount() > 1) {
      json.put(KEY_SELECTED, new JSONNumber(getSelectedIndex()));
    }
    
    maybeAddResized(json);

    JSONArray tabs = new JSONArray();

    for (int i = 0; i < getPageCount(); i++) {
      Widget contentPanel = getContentWidget(i);
      if (contentPanel instanceof TilePanel) {
        tabs.set(i, ((TilePanel) contentPanel).toJson());
      }
    }

    json.put(KEY_TABS, tabs);

    return json;
  }

  private void updateCaption(Tile tile, String tileCaption) {
    String caption = tileCaption;
    if (BeeUtils.isEmpty(caption)) {
      caption = tile.getPanel().getCaption();
    }

    if (BeeUtils.isEmpty(caption)) {
      caption = Localized.getConstants().newTab();
    }

    int index = getPageIndex(tile);

    TabWidget tab = (TabWidget) getTabWidget(index);
    if (caption.equals(tab.getCaption())) {
      return;
    }

    boolean checkSize = isAttached() && getPageCount() > 1;
    if (checkSize) {
      saveLayout();
    }

    tab.setCaption(caption);

    if (checkSize) {
      checkLayout();
    }
  }
}
