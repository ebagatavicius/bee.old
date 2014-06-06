package com.butent.bee.client.screen;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
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
import com.butent.bee.client.ui.IdentifiableWidget;
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

import java.util.List;
import java.util.Map;

public class Workspace extends TabbedPages implements CaptionChangeEvent.Handler,
    HasActiveWidgetChangeHandlers, ActiveWidgetChangeEvent.Handler, PreviewHandler {

  private enum TabAction {
    CREATE(Localized.getConstants().actionWorkspaceNewTab()),

    NORTH(Localized.getConstants().actionWorkspaceNewTop(), Direction.NORTH),
    SOUTH(Localized.getConstants().actionWorkspaceNewBottom(), Direction.SOUTH),
    WEST(Localized.getConstants().actionWorkspaceNewLeft(), Direction.WEST),
    EAST(Localized.getConstants().actionWorkspaceNewRight(), Direction.EAST),

    MAXIMIZE(Localized.getConstants().actionWorkspaceMaxSize()),
    RESTORE(Localized.getConstants().actionWorkspaceRestoreSize()),

    UP(Localized.getConstants().actionWorkspaceEnlargeUp(), Direction.NORTH),
    DOWN(Localized.getConstants().actionWorkspaceEnlargeDown(), Direction.SOUTH),
    LEFT(Localized.getConstants().actionWorkspaceEnlargeToLeft(), Direction.WEST),
    RIGHT(Localized.getConstants().actionWorkspaceEnlargeToRight(), Direction.EAST),

    CLOSE(Localized.getConstants().actionClose()),
    CLOSE_TAB(Localized.getConstants().actionWorkspaceCloseTab()),
    CLOSE_OTHER(Localized.getConstants().actionWorkspaceCloseOther()),
    CLOSE_RIGHT(Localized.getConstants().actionWorkspaceCloseRight()),
    CLOSE_ALL(Localized.getConstants().actionWorkspaceCloseAll()),

    BOOKMARK_TAB(Localized.getConstants().actionWorkspaceBookmarkTab()),
    BOOKMARK_ALL(Localized.getConstants().actionWorkspaceBookmarkAll());

    private static final String STYLE_NAME_PREFIX = Workspace.STYLE_PREFIX + "action-";

    private final IdentifiableWidget widget;
    private final Direction direction;

    private TabAction(String label) {
      this(label, null);
    }

    private TabAction(String label, Direction direction) {
      this.widget = new Label(label);
      this.direction = direction;

      this.widget.asWidget().addStyleName(STYLE_NAME_PREFIX 
          + this.name().toLowerCase().replace(BeeConst.CHAR_UNDER, BeeConst.CHAR_MINUS));
    }

    private Direction getDirection() {
      return direction;
    }

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
      closeTab.setTitle(Localized.getConstants().closeTab());
      add(closeTab);

      closeTab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          doAction(TabAction.CLOSE, getTabIndex(TabWidget.this.getId()));
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

  private static final String STYLE_PREFIX = "bee-Workspace-";

  private final Map<Direction, Integer> resized = Maps.newHashMap();

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
    List<IdentifiableWidget> result = Lists.newArrayList();

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

  void showInfo() {
    TilePanel panel = getActivePanel();
    if (panel == null) {
      return;
    }

    List<ExtendedProperty> info = panel.getExtendedInfo();
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
    }
  }

  private void doAction(TabAction action, int index) {
    switch (action) {
      case CLOSE:
        if (index == getSelectedIndex()) {
          close(getActiveTile());
        } else {
          removePage(index);
        }
        break;

      case CREATE:
        insertEmptyPanel(index + 1);
        break;

      case EAST:
      case NORTH:
      case SOUTH:
      case WEST:
        splitActiveTile(action.getDirection());
        break;

      case MAXIMIZE:
        for (Direction direction : Direction.values()) {
          if (!Direction.CENTER.equals(direction)) {
            stretch(direction, false);
          }
        }
        getResizeContainer().doLayout();
        break;

      case RESTORE:
        for (Map.Entry<Direction, Integer> entry : resized.entrySet()) {
          getResizeContainer().setDirectionSize(entry.getKey(), entry.getValue(), false);
        }
        resized.clear();
        getResizeContainer().doLayout();
        break;

      case UP:
      case DOWN:
      case LEFT:
      case RIGHT:
        stretch(action.getDirection(), true);
        break;

      case BOOKMARK_ALL:
        break;
      case BOOKMARK_TAB:
        break;

      case CLOSE_ALL:
        while (getPageCount() > 1) {
          removePage(getPageCount() - 1);
        }
        clearPage(0);
        break;

      case CLOSE_OTHER:
        while (getPageCount() > index + 1) {
          removePage(getPageCount() - 1);
        }
        while (getPageCount() > 1) {
          removePage(0);
        }
        break;

      case CLOSE_RIGHT:
        while (getPageCount() > index + 1) {
          removePage(getPageCount() - 1);
        }
        break;

      case CLOSE_TAB:
        if (getPageCount() > 1) {
          removePage(index);
        } else {
          clearPage(index);
        }
        break;
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

  private void insertEmptyPanel(int before) {
    TilePanel panel = new TilePanel(this);
    TabWidget tab = new TabWidget(Localized.getConstants().newTab());

    insert(panel, tab, before);

    selectPage(before, SelectionOrigin.INSERT);
  }

  private boolean isActionEnabled(TabAction action, int index) {
    boolean enabled = false;

    switch (action) {
      case CREATE:
        enabled = true;
        break;

      case CLOSE:
        if (getPageCount() > 1) {
          enabled = true;
        } else {
          TilePanel panel = getActivePanel();
          enabled = panel.getTileCount() > 1 || !panel.getActiveTile().isBlank();
        }
        break;

      case EAST:
      case NORTH:
      case SOUTH:
      case WEST:
        enabled = index == getSelectedIndex();
        break;

      case MAXIMIZE:
        enabled = index == getSelectedIndex() && (canGrow(Direction.NORTH)
            || canGrow(Direction.SOUTH) || canGrow(Direction.WEST) || canGrow(Direction.EAST));
        break;

      case RESTORE:
        enabled = index == getSelectedIndex() && !resized.isEmpty();
        break;

      case UP:
      case DOWN:
      case LEFT:
      case RIGHT:
        enabled = index == getSelectedIndex() && !resized.containsKey(action.getDirection())
            && canGrow(action.getDirection());
        break;

      case BOOKMARK_ALL:
        enabled = getPageCount() > 1;
        break;

      case BOOKMARK_TAB:
        if (index == getSelectedIndex()) {
          TilePanel panel = getActivePanel();
          enabled = panel.getTileCount() > 1 || !panel.getActiveTile().isBlank();
        }
        break;
      
      case CLOSE_ALL:
        enabled = getPageCount() > 1;
        break;

      case CLOSE_OTHER:
        enabled = getPageCount() > 1;
        break;
      
      case CLOSE_RIGHT:
        enabled = index < getPageCount() - 1;
        break;

      case CLOSE_TAB:
        if (getPageCount() > 1 || index != getSelectedIndex()) {
          enabled = true;
        } else {
          TilePanel panel = getActivePanel();
          enabled = panel.getTileCount() > 1 || !panel.getActiveTile().isBlank();
        }
        break;
    }

    return enabled;
  }

  private void showActions(String tabId) {
    final int index = getTabIndex(tabId);
    if (BeeConst.isUndef(index)) {
      showError("tab widget not found, id: " + tabId);
      return;
    }

    final List<TabAction> actions = Lists.newArrayList();
    for (TabAction action : TabAction.values()) {
      if (isActionEnabled(action, index)) {
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
        doAction(actions.get(event.getSelectedItem()), index);
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
