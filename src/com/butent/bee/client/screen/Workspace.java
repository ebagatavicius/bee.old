package com.butent.bee.client.screen;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Historian;
import com.butent.bee.client.Settings;
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
import com.butent.bee.client.layout.Vertical;
import com.butent.bee.client.screen.TilePanel.Tile;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Theme;
import com.butent.bee.client.utils.JsonUtils;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.CustomHasHtml;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.ExtendedPropertiesData;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.UserInterface.Component;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Workspace extends TabbedPages implements CaptionChangeEvent.Handler,
    HasActiveWidgetChangeHandlers, ActiveWidgetChangeEvent.Handler, PreviewHandler,
    HasExtendedInfo {

  private final class ContentRestorer implements Consumer<Boolean> {

    private final Map<String, Map<String, String>> contentSuppliers;
    private final List<String> panelIds;

    private final Consumer<Boolean> callback;

    private int position;

    private ContentRestorer(Map<String, Map<String, String>> contentSuppliers,
        Consumer<Boolean> callback) {

      this.contentSuppliers = contentSuppliers;
      this.panelIds = new ArrayList<>(contentSuppliers.keySet());

      this.callback = callback;
    }

    @Override
    public void accept(Boolean input) {
      if (BeeUtils.isTrue(input)) {
        if (getPosition() < panelIds.size() - 1 && isLoading()) {
          setPosition(getPosition() + 1);
          run();

        } else if (callback != null) {
          callback.accept(input);
        }

      } else if (callback != null) {
        callback.accept(input);
      }
    }

    private int getPosition() {
      return position;
    }

    private void run() {
      String panelId = panelIds.get(getPosition());
      int index = getContentIndex(panelId);

      Map<String, String> contentByTile = contentSuppliers.get(panelId);
      logger.info("restoring panel", BeeUtils.progress(getPosition() + 1, panelIds.size()),
          contentByTile.values());

      if (isIndex(index)) {
        selectPage(index, SelectionOrigin.SCRIPT);

        TilePanel panel = getActivePanel();
        if (panel != null) {
          panel.restoreContent(contentByTile, this);
        }
      }
    }

    private void setPosition(int position) {
      this.position = position;
    }

    private void start() {
      setPosition(0);
      run();
    }
  }

  private final class Restorer implements BiConsumer<Boolean, Integer> {

    private final List<JSONObject> spaces;
    private final boolean append;

    private final Timer timer;

    private int position;

    private int pendingPage = BeeConst.UNDEF;

    private Restorer(List<JSONObject> spaces, boolean append) {
      this.spaces = spaces;
      this.append = append;

      this.timer = new Timer() {
        @Override
        public void run() {
          if (isLoading()) {
            setState(State.EXPIRED);
          }
        }
      };
    }

    @Override
    public void accept(Boolean t, Integer u) {
      if (BeeUtils.isNonNegative(u)) {
        setPendingPage(u);
      }

      if (BeeUtils.isTrue(t) && getPosition() < spaces.size() - 1 && isLoading()) {
        setPosition(getPosition() + 1);
        run();

      } else {
        onComplete();
      }
    }

    private int getPendingPage() {
      return pendingPage;
    }

    private int getPosition() {
      return position;
    }

    private void onComplete() {
      timer.cancel();
      setState(State.LOADED);

      if (getPageCount() <= 0) {
        insertEmptyPanel(0);
      } else if (isIndex(getPendingPage()) && getPendingPage() != getSelectedIndex()) {
        selectPage(getPendingPage(), SelectionOrigin.CLICK);
      }
    }

    private void run() {
      JSONObject space = spaces.get(getPosition());
      logger.info("restoring space", BeeUtils.progress(getPosition() + 1, spaces.size()));

      restore(space, this);
    }

    private void setPendingPage(int pendingPage) {
      this.pendingPage = pendingPage;
    }

    private void setPosition(int position) {
      this.position = position;
    }

    private void start() {
      if (!append) {
        while (getPageCount() > 0) {
          removePage(0);
        }
      }

      setState(State.LOADING);

      setPosition(0);
      run();

      timer.schedule(RESTORATION_TIMEOUT);
    }
  }

  private enum TabAction {
    CREATE(Localized.dictionary().actionWorkspaceNewTab(), GROUP_NEW) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.insertEmptyPanel(index + 1);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return true;
      }
    },

    NORTH(Localized.dictionary().actionWorkspaceNewTop(), GROUP_SPLIT) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.splitActiveTile(Direction.NORTH);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex();
      }
    },

    SOUTH(Localized.dictionary().actionWorkspaceNewBottom(), GROUP_SPLIT) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.splitActiveTile(Direction.SOUTH);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex();
      }
    },

    WEST(Localized.dictionary().actionWorkspaceNewLeft(), GROUP_SPLIT) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.splitActiveTile(Direction.WEST);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex();
      }
    },

    EAST(Localized.dictionary().actionWorkspaceNewRight(), GROUP_SPLIT) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.splitActiveTile(Direction.EAST);
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return index == workspace.getSelectedIndex();
      }
    },

    CLOSE_TILE(Localized.dictionary().actionWorkspaceCloseTile(), GROUP_CLOSE) {
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

    CLOSE_TAB(Localized.dictionary().actionWorkspaceCloseTab(), GROUP_CLOSE) {
      @Override
      void execute(Workspace workspace, int index) {
        if (workspace.getPageCount() > 1) {
          workspace.removePage(index);
        } else {
          workspace.clearPage(index);
        }
        workspace.checkEmptiness();
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

    CLOSE_OTHER(Localized.dictionary().actionWorkspaceCloseOther(), GROUP_CLOSE) {
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

    CLOSE_RIGHT(Localized.dictionary().actionWorkspaceCloseRight(), GROUP_CLOSE) {
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

    CLOSE_ALL(Localized.dictionary().actionWorkspaceCloseAll(), GROUP_CLOSE) {
      @Override
      void execute(Workspace workspace, int index) {
        workspace.clear();
        workspace.checkEmptiness();
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        return workspace.getPageCount() > 1;
      }
    },

    BOOKMARK_TAB(Localized.dictionary().actionWorkspaceBookmarkTab(), GROUP_BOOKMARK) {
      @Override
      void execute(Workspace workspace, int index) {
        TilePanel panel = workspace.getActivePanel();
        if (panel != null) {
          JSONObject json = panel.toJson();
          maybeAddHidden(json);
          Global.getSpaces().bookmark(panel.getBookmarkLabel(), json.toString());
        }
      }

      @Override
      boolean isEnabled(Workspace workspace, int index) {
        if (BeeKeeper.getScreen().getUserInterface().hasComponent(Component.WORKSPACES)
            && index == workspace.getSelectedIndex()) {
          TilePanel panel = workspace.getActivePanel();
          return panel != null && panel.isBookmarkable();
        } else {
          return false;
        }
      }
    },

    BOOKMARK_ALL(Localized.dictionary().actionWorkspaceBookmarkAll(), GROUP_BOOKMARK) {
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
        return BeeKeeper.getScreen().getUserInterface().hasComponent(Component.WORKSPACES)
            && workspace.getPageCount() > 1;
      }
    };

    private final String label;
    private final char group;

    private final String styleSuffix;

    TabAction(String label, char group) {
      this.label = label;
      this.group = group;

      this.styleSuffix = name().toLowerCase().replace(BeeConst.CHAR_UNDER, BeeConst.CHAR_MINUS);
    }

    abstract void execute(Workspace workspace, int index);

    abstract boolean isEnabled(Workspace workspace, int index);
  }

  private final class TabWidget extends Flow implements HasCaption {

    private static final String STYLE_WRAPPER = STYLE_PREFIX + "tab-wrapper";
    private static final String STYLE_DROP_DOWN = STYLE_PREFIX + "tab-drop-down";
    private static final String STYLE_CAPTION = STYLE_PREFIX + "tab-caption";
    private static final String STYLE_CLOSE = STYLE_PREFIX + "tab-close";

    private TabWidget(String caption) {
      super(STYLE_WRAPPER);

      CustomDiv dropDown = new CustomDiv(STYLE_DROP_DOWN);
      dropDown.setText(String.valueOf(BeeConst.DROP_DOWN));
      dropDown.setTitle(Localized.dictionary().tabControl());
      add(dropDown);

      dropDown.addClickHandler(event -> {
        event.stopPropagation();
        Workspace.this.showActions(TabWidget.this.getId());
      });

      CustomDiv label = new CustomDiv(STYLE_CAPTION);
      label.setHtml(caption);
      add(label);

      CustomDiv closeTab = new CustomDiv(STYLE_CLOSE);
      closeTab.setText(String.valueOf(BeeConst.CHAR_TIMES));
      closeTab.setTitle(Localized.dictionary().actionWorkspaceCloseTab());
      add(closeTab);

      closeTab.addClickHandler(event ->
          TabAction.CLOSE_TAB.execute(Workspace.this, getTabIndex(TabWidget.this.getId())));
    }

    @Override
    public String getCaption() {
      return getCaptionWidget().getHtml();
    }

    private CustomDiv getCaptionWidget() {
      return (CustomDiv) getWidget(1);
    }

    private void setCaption(String caption) {
      CustomDiv widget = getCaptionWidget();

      widget.setHtml(caption);
      widget.setTitle(caption);
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(Workspace.class);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "Workspace-";

  private static final String STYLE_NEW_TAB = STYLE_PREFIX + "new-tab";

  private static final String STYLE_ACTION_PREFIX = STYLE_PREFIX + "action-";

  private static final String STYLE_ACTION_POPUP = STYLE_ACTION_PREFIX + "popup";
  private static final String STYLE_ACTION_TABLE = STYLE_ACTION_PREFIX + "table";
  private static final String STYLE_ACTION_ENABLED = STYLE_ACTION_PREFIX + "enabled";
  private static final String STYLE_ACTION_DISABLED = STYLE_ACTION_PREFIX + "disabled";
  private static final String STYLE_ACTION_SEPARATOR = STYLE_ACTION_PREFIX + "separator";

  static final String KEY_CONTENT = "content";
  static final String KEY_DIRECTION = "direction";
  static final String KEY_SIZE = "size";

  private static final String KEY_SELECTED = "selected";
  private static final String KEY_HIDDEN = "hidden";
  private static final String KEY_TABS = "tabs";
  private static final String KEY_FORCE = "force";

  private static final int SIZE_FACTOR = 1_000_000;

  private static final char GROUP_NEW = 'n';
  private static final char GROUP_SPLIT = 's';
  private static final char GROUP_CLOSE = 'c';
  private static final char GROUP_BOOKMARK = 'b';

  private static final int RESTORATION_TIMEOUT = TimeUtils.MILLIS_PER_MINUTE * 5;

  public static boolean isForced(JSONObject json) {
    return json != null && json.containsKey(KEY_FORCE);
  }

  public static List<String> maybeForceSpace(List<String> input, JSONObject json) {
    List<String> result = new ArrayList<>();

    if (json == null) {
      if (!BeeUtils.isEmpty(input)) {
        result.addAll(input);
      }

    } else if (BeeUtils.isEmpty(input)) {
      result.add(json.toString());

    } else if (!json.containsKey(KEY_FORCE)) {
      result.addAll(input);

    } else {
      List<JSONObject> spaces = new ArrayList<>();
      Set<String> contents = new HashSet<>();

      for (String s : input) {
        JSONObject space = JsonUtils.parseObject(s);

        if (space != null) {
          spaces.add(space);
          contents.addAll(getContentValues(space));
        }
      }

      if (contents.containsAll(getContentValues(json))) {
        result.addAll(input);

      } else {
        String force = JsonUtils.getString(json, KEY_FORCE);
        boolean last = BeeUtils.inListSame(force, "end", "+", ">");

        if (!last) {
          result.add(json.toString());
        }

        for (JSONObject space : spaces) {
          space.put(KEY_SELECTED, new JSONNumber(BeeConst.UNDEF));
          result.add(space.toString());
        }

        if (last) {
          result.add(json.toString());
        }
      }
    }

    return result;
  }

  static int restoreSize(double size, int max) {
    return BeeUtils.round(size * max / SIZE_FACTOR);
  }

  static int scaleSize(int size, int max) {
    return BeeUtils.round((double) size * SIZE_FACTOR / max);
  }

  private static Set<String> getContentValues(JSONObject json) {
    Set<String> values = new HashSet<>();

    if (json.containsKey(KEY_CONTENT)) {
      String value = JsonUtils.getString(json, KEY_CONTENT);
      if (!BeeUtils.isEmpty(value)) {
        values.add(value);
      }
    }

    if (json.containsKey(KEY_TABS)) {
      JSONArray tabs = json.get(KEY_TABS).isArray();

      if (tabs != null) {
        for (int i = 0; i < tabs.size(); i++) {
          JSONObject tab = tabs.get(i).isObject();
          String value = JsonUtils.getString(tab, KEY_CONTENT);
          if (!BeeUtils.isEmpty(value)) {
            values.add(value);
          }
        }
      }
    }

    return values;
  }

  private static Set<Direction> getHiddenDirections(JSONObject json) {
    Set<Direction> directions = EnumSet.noneOf(Direction.class);

    if (json != null && json.containsKey(KEY_HIDDEN)) {
      JSONArray arr = json.get(KEY_HIDDEN).isArray();

      if (arr != null) {
        for (int i = 0; i < arr.size(); i++) {
          JSONString string = arr.get(i).isString();

          if (string != null) {
            Direction direction = Direction.parse(string.stringValue());
            if (direction != null) {
              directions.add(direction);
            }
          }
        }
      }
    }

    return directions;
  }

  private static void maybeAddHidden(JSONObject json) {
    Set<Direction> directions = BeeKeeper.getScreen().getHiddenDirections();

    if (!BeeUtils.isEmpty(directions)) {
      JSONArray arr = new JSONArray();
      int index = 0;

      for (Direction direction : directions) {
        if (direction != null) {
          arr.set(index++, new JSONString(direction.brief()));
        }
      }

      if (index > 0) {
        json.put(KEY_HIDDEN, arr);
      }
    }
  }

  private static void maybeSetHeight(Widget widget) {
    int height = Theme.getWorkspaceTabHeight();
    if (height > 0) {
      StyleUtils.setLineHeight(widget, height);
    }
  }

  private State state;

  Workspace() {
    super(STYLE_PREFIX);

    insertEmptyPanel(0);

    CustomHasHtml newTab = new CustomHasHtml(DomUtils.createElement(Tags.ASIDE), STYLE_NEW_TAB);
    newTab.setText(BeeConst.STRING_PLUS);
    newTab.setTitle(Localized.dictionary().newTab());

    newTab.addClickHandler(event -> insertEmptyPanel(getPageCount()));

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
  public List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> info = new ArrayList<>();

    info.add(new ExtendedProperty("Selected Index", BeeUtils.toString(getSelectedIndex())));

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

    return info;
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
    if (isLoading()) {
      setState(State.CANCELED);
      return;
    }
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
                BeeUtils.joinWords(Split.getWidgetDirection(child), Split.getWidgetSize(child))));
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
  public boolean selectPage(int index, SelectionOrigin origin) {
    boolean result = super.selectPage(index, origin);

    if (EnumUtils.in(origin, SelectionOrigin.CLICK, SelectionOrigin.INSERT,
        SelectionOrigin.REMOVE)) {

      Tile tile = getActiveTile();

      Historian.goTo(tile.getId());
      tile.activateContent();
    }
    return result;
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

  void addToTabBar(IdentifiableWidget widget) {
    getTabBar().add(widget);
  }

  boolean closeWidget(IdentifiableWidget widget) {
    Tile tile = TilePanel.getTile(widget.asWidget());

    if (tile != null) {
      close(tile);
      checkEmptiness();
      return true;

    } else {
      return false;
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

  void onStart() {
    for (Widget tab : getTabBar()) {
      if (tab.getElement().hasClassName(STYLE_NEW_TAB) || tab instanceof TabWidget) {
        maybeSetHeight(tab);
      }
    }
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

  void restore(List<String> input, boolean append) {
    if (!BeeUtils.isEmpty(input)) {
      List<JSONObject> spaces = new ArrayList<>();
      Set<Direction> hiddenDirections = EnumSet.noneOf(Direction.class);

      for (String s : input) {
        JSONObject space = JsonUtils.parseObject(s);

        if (space == null) {
          showError("cannot parse space " + BeeUtils.trim(s));

        } else {
          spaces.add(space);
          hiddenDirections.addAll(getHiddenDirections(space));
        }
      }

      if (!hiddenDirections.isEmpty()) {
        BeeKeeper.getScreen().hideDirections(hiddenDirections);
      }

      if (!spaces.isEmpty()) {
        Restorer restorer = new Restorer(spaces, append);
        restorer.start();
      }
    }
  }

  String serialize() {
    JSONObject json = toJson();

    if (json == null || json.size() <= 0) {
      return null;

    } else if (json.size() == 1 && json.containsKey(KEY_TABS)) {
      JSONArray arr = json.get(KEY_TABS).isArray();

      if (arr == null) {
        return null;

      } else if (arr.size() == 1) {
        JSONObject tab = arr.get(0).isObject();
        if (tab == null || tab.size() <= 0) {
          return null;
        }
      }
    }

    return json.toString();
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

  private void checkEmptiness() {
    if (isBlank()) {
      JSONObject json = Settings.getOnEmptyWorkspace();
      if (json != null) {
        restore(Lists.newArrayList(json.toString()), false);
      }
    }
  }

  private void clearCaption(int index) {
    TabWidget tab = (TabWidget) getTabWidget(index);
    tab.setCaption(Localized.dictionary().newTab());
  }

  private void clearPage(int index) {
    Widget widget = getContentWidget(index);

    if (widget instanceof TilePanel) {
      ((TilePanel) widget).clear(this);
      clearCaption(index);
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
      clearCaption(pageIndex);
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

  private State getState() {
    return state;
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

    TabWidget tab = new TabWidget(Localized.dictionary().newTab());
    maybeSetHeight(tab);

    insert(panel, tab, null, null, null, before);

    selectPage(before, SelectionOrigin.INSERT);
    return panel;
  }

  private boolean isBlank() {
    if (getPageCount() == 1) {
      TilePanel panel = getActivePanel();

      if (panel != null && panel.getTileCount() == 1) {
        Tile tile = panel.getActiveTile();
        return tile != null && tile.isBlank();
      }
    }
    return false;
  }

  private boolean isLoading() {
    return getState() == State.LOADING;
  }

  private void restore(JSONObject json, final BiConsumer<Boolean, Integer> callback) {
    final Holder<Integer> activePage = Holder.of(BeeConst.UNDEF);

    Map<String, Map<String, String>> contentSuppliers = new LinkedHashMap<>();

    TilePanel panel;

    if (json.containsKey(KEY_TABS)) {
      int oldPageCount = getPageCount();
      JSONArray tabs = json.get(KEY_TABS).isArray();

      if (tabs != null) {
        for (int i = 0; i < tabs.size(); i++) {
          JSONObject child = tabs.get(i).isObject();

          if (child != null) {
            panel = insertEmptyPanel(getPageCount());

            Map<String, String> contentByTile = panel.restore(this, child);
            if (!BeeUtils.isEmpty(contentByTile)) {
              contentSuppliers.put(panel.getId(), contentByTile);
            } else {
              panel.afterRestore();
            }
          }
        }

        if (json.containsKey(KEY_SELECTED) && getPageCount() == oldPageCount + tabs.size()) {
          Integer index = JsonUtils.getInteger(json, KEY_SELECTED);
          if (index != null && BeeUtils.betweenExclusive(index, 0, tabs.size())) {
            activePage.set(oldPageCount + index);
          }
        }
      }

    } else {
      panel = insertEmptyPanel(getPageCount());

      boolean select;
      if (json.containsKey(KEY_SELECTED)) {
        select = BeeUtils.isZero(JsonUtils.getNumber(json, KEY_SELECTED));
      } else {
        select = true;
      }

      if (select) {
        activePage.set(getSelectedIndex());
      }

      Map<String, String> contentByTile = panel.restore(this, json);
      if (!BeeUtils.isEmpty(contentByTile)) {
        contentSuppliers.put(panel.getId(), contentByTile);
      } else {
        panel.afterRestore();
      }
    }

    if (!contentSuppliers.isEmpty()) {
      ContentRestorer restorer = new ContentRestorer(contentSuppliers,
          b -> callback.accept(b, activePage.get()));

      restorer.start();

    } else {
      callback.accept(true, activePage.get());
    }
  }

  private void setState(State state) {
    this.state = state;

    if (state != null) {
      logger.info(NameUtils.getName(this), state.name().toLowerCase());
    }
  }

  private void showActions(String tabId) {
    final int index = getTabIndex(tabId);
    if (BeeConst.isUndef(index)) {
      showError("tab widget not found, id: " + tabId);
      return;
    }

    final Popup popup = new Popup(OutsideClick.CLOSE, STYLE_ACTION_POPUP);

    Vertical table = new Vertical();
    table.addStyleName(STYLE_ACTION_TABLE);

    char currentGroup = BeeConst.CHAR_SPACE;

    for (final TabAction action : TabAction.values()) {
      if (action.group != currentGroup) {
        currentGroup = action.group;

        if (!table.isEmpty()) {
          CustomDiv separator = new CustomDiv(STYLE_ACTION_SEPARATOR);
          table.add(separator);
        }
      }

      Label actionWidget = new Label(action.label);
      actionWidget.addStyleName(STYLE_ACTION_PREFIX + action.styleSuffix);

      if (action.isEnabled(this, index)) {
        actionWidget.addStyleName(STYLE_ACTION_ENABLED);

        actionWidget.addClickHandler(event -> {
          popup.close();
          action.execute(Workspace.this, index);
        });

      } else {
        actionWidget.addStyleName(STYLE_ACTION_DISABLED);
      }

      table.add(actionWidget);
    }

    popup.setWidget(table);
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

  private JSONObject toJson() {
    JSONObject json = new JSONObject();

    if (getPageCount() > 1) {
      json.put(KEY_SELECTED, new JSONNumber(getSelectedIndex()));
    }

    maybeAddHidden(json);

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
      caption = Localized.dictionary().newTab();
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
