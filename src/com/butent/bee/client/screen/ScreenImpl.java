package com.butent.bee.client.screen;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Bee;
import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Screen;
import com.butent.bee.client.Settings;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.Notification;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.layout.Complex;
import com.butent.bee.client.layout.CustomComplex;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.logging.ClientLogManager;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.screen.TilePanel.Tile;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasProgress;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.BrowsingContext;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.css.values.FontSize;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles default (desktop) screen implementation.
 */

public class ScreenImpl implements Screen {

  private static final BeeLogger logger = LogUtils.getLogger(ScreenImpl.class);

  private Split screenPanel;

  private CentralScrutinizer centralScrutinizer;

  private Workspace workspace;
  private HasWidgets commandPanel;

  private HasWidgets menuPanel;

  private HasWidgets userPhotoContainer;
  private HasHtml userSignature;

  private Notification notification;

  private Panel progressPanel;

  private final Map<Direction, Integer> hidden = Maps.newEnumMap(Direction.class);

  private Toggle northToggle;
  private Toggle westToggle;

  private Toggle maximizer;

  public ScreenImpl() {
  }

  @Override
  public boolean activateDomainEntry(Domain domain, Long key) {
    if (getCentralScrutinizer() == null) {
      return false;
    } else {
      return getCentralScrutinizer().activate(domain, key);
    }
  }

  @Override
  public void activateWidget(IdentifiableWidget widget) {
    Assert.notNull(widget, "activateWidget: widget is null");
    getWorkspace().activateWidget(widget);
  }

  @Override
  public void addCommandItem(IdentifiableWidget widget) {
    Assert.notNull(widget);
    if (getCommandPanel() == null) {
      logger.severe(NameUtils.getName(this), "command panel not available");
    } else {
      widget.asWidget().addStyleName(StyleUtils.CLASS_NAME_PREFIX + "MainCommandPanelItem");
      getCommandPanel().add(widget.asWidget());
    }
  }

  @Override
  public void addDomainEntry(Domain domain, IdentifiableWidget widget, Long key, String caption) {
    if (getCentralScrutinizer() == null) {
      logger.severe("cannot add domain", domain);
    } else {
      getCentralScrutinizer().add(domain, widget, key, caption);
    }
  }

  @Override
  public String addProgress(HasProgress widget) {
    if (getProgressPanel() != null && widget != null) {
      if (!getProgressPanel().iterator().hasNext()) {
        showProgressPanel();
      }

      getProgressPanel().add(widget);
      return widget.getId();
    } else {
      return null;
    }
  }

  @Override
  public void clearNotifications() {
    if (getNotification() != null) {
      getNotification().clear();
    }
  }

  @Override
  public void closeWidget(IdentifiableWidget widget) {
    Assert.notNull(widget, "closeWidget: widget is null");

    if (UiHelper.isModal(widget.asWidget())) {
      UiHelper.closeDialog(widget.asWidget());
    } else {
      getWorkspace().closeWidget(widget);
    }
  }

  @Override
  public boolean containsDomainEntry(Domain domain, Long key) {
    if (getCentralScrutinizer() == null) {
      return false;
    } else {
      return getCentralScrutinizer().contains(domain, key);
    }
  }

  @Override
  public int getActivePanelHeight() {
    Tile activeTile = getWorkspace().getActiveTile();
    return (activeTile == null) ? 0 : activeTile.getOffsetHeight();
  }

  @Override
  public int getActivePanelWidth() {
    Tile activeTile = getWorkspace().getActiveTile();
    return (activeTile == null) ? 0 : activeTile.getOffsetWidth();
  }

  @Override
  public IdentifiableWidget getActiveWidget() {
    return getWorkspace().getActiveContent();
  }

  @Override
  public HasWidgets getCommandPanel() {
    return commandPanel;
  }

  @Override
  public Flow getDomainHeader(Domain domain, Long key) {
    if (getCentralScrutinizer() == null) {
      return null;
    } else {
      return getCentralScrutinizer().getDomainHeader(domain, key);
    }
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> info = getWorkspace().getExtendedInfo();

    if (!hidden.isEmpty()) {
      for (Map.Entry<Direction, Integer> entry : hidden.entrySet()) {
        info.add(new ExtendedProperty("Hidden", entry.getKey().name(),
            BeeUtils.toString(entry.getKey())));
      }
    }

    return info;
  }

  @Override
  public int getHeight() {
    return getScreenPanel().getOffsetHeight();
  }

  @Override
  public Set<Direction> getHiddenDirections() {
    return hidden.keySet();
  }

  @Override
  public List<IdentifiableWidget> getOpenWidgets() {
    return getWorkspace().getOpenWidgets();
  }

  @Override
  public Split getScreenPanel() {
    return screenPanel;
  }

  @Override
  public UserInterface getUserInterface() {
    return UserInterface.DESKTOP;
  }

  @Override
  public int getWidth() {
    return getScreenPanel().getOffsetWidth();
  }

  @Override
  public Workspace getWorkspace() {
    return workspace;
  }

  @Override
  public boolean hasNotifications() {
    return getNotification() != null && getNotification().isActive();
  }

  @Override
  public void hideDirections(Set<Direction> directions) {
    boolean changed = false;

    if (!BeeUtils.isEmpty(directions)) {
      for (Direction direction : directions) {
        changed |= expand(direction, false);
      }
    }

    if (changed) {
      getScreenPanel().doLayout();
      refreshExpanders();
    }
  }

  @Override
  public void init() {
    createUi();
  }

  @Override
  public void notifyInfo(String... messages) {
    if (getNotification() != null) {
      getNotification().info(messages);
    }
  }

  @Override
  public void notifySevere(String... messages) {
    if (getNotification() != null) {
      getNotification().severe(messages);
    }
  }

  @Override
  public void notifyWarning(String... messages) {
    if (getNotification() != null) {
      getNotification().warning(messages);
    }
  }

  @Override
  public void onLoad() {
    Global.getSearch().focus();

    if (!Global.getSpaces().isEmpty() && !containsDomainEntry(Domain.WORKSPACES, null)) {
      addDomainEntry(Domain.WORKSPACES, Global.getSpaces().getPanel(), null, null);
    }
    if (!Global.getReportSettings().isEmpty() && !containsDomainEntry(Domain.REPORTS, null)) {
      addDomainEntry(Domain.REPORTS, Global.getReportSettings().getPanel(), null, null);
    }

    if (Global.getNewsAggregator().hasNews()) {
      activateDomainEntry(Domain.NEWS, null);
    }
  }

  @Override
  public void onWidgetChange(IdentifiableWidget widget) {
    Assert.notNull(widget, "onWidgetChange: widget is null");
    getWorkspace().onWidgetChange(widget);
  }

  @Override
  public boolean removeDomainEntry(Domain domain, Long key) {
    if (getCentralScrutinizer() == null) {
      return false;
    } else {
      return getCentralScrutinizer().remove(domain, key);
    }
  }

  @Override
  public void removeProgress(String id) {
    if (getProgressPanel() != null && !BeeUtils.isEmpty(id)) {
      Widget item = DomUtils.getChildById(getProgressPanel(), id);

      if (item != null) {
        getProgressPanel().remove(item);
        if (!getProgressPanel().iterator().hasNext()) {
          hideProgressPanel();
        }
      }
    }
  }

  @Override
  public void restore(String input, boolean append) {
    if (getWorkspace() != null) {
      getWorkspace().restore(input, append);
    }
  }

  @Override
  public void showInNewPlace(IdentifiableWidget widget) {
    getWorkspace().openInNewPlace(widget);
  }

  @Override
  public void start(UserData userData) {
    updateUserData(userData);

    if (getCentralScrutinizer() != null) {
      getCentralScrutinizer().start();
    }

    if (getWorkspace() != null) {
      if (getCentralScrutinizer() != null && getWorkspace() != null) {
        getWorkspace().addActiveWidgetChangeHandler(getCentralScrutinizer());
      }

      Previewer.registerMouseDownPriorHandler(getWorkspace());
    }
  }

  @Override
  public void updateActivePanel(IdentifiableWidget widget) {
    getWorkspace().updateActivePanel(widget);
  }

  @Override
  public void updateCommandPanel(IdentifiableWidget widget) {
    updatePanel(getCommandPanel(), widget);
  }

  @Override
  public void updateMenu(IdentifiableWidget widget) {
    updatePanel(getMenuPanel(), widget);
  }

  @Override
  public void updateProgress(String id, double value) {
    if (getProgressPanel() != null && !BeeUtils.isEmpty(id)) {
      Widget item = DomUtils.getChildById(getProgressPanel(), id);

      if (item instanceof HasProgress) {
        ((HasProgress) item).update(value);
      }
    }
  }

  @Override
  public void updateUserData(UserData userData) {
    if (userData != null) {
      if (getUserPhotoContainer() != null) {
        getUserPhotoContainer().clear();

        String photoFileName = userData.getPhotoFileName();
        if (!BeeUtils.isEmpty(photoFileName)) {
          Image image = new Image(PhotoRenderer.getUrl(photoFileName));
          image.setAlt(userData.getLogin());
          image.addStyleName(StyleUtils.CLASS_NAME_PREFIX + "UserPhoto");

          getUserPhotoContainer().add(image);
        }
      }

      if (getUserSignature() != null) {
        getUserSignature().setText(BeeUtils.trim(userData.getUserSign()));
      }
    }
  }

  protected Panel createCommandPanel() {
    return new Flow(StyleUtils.CLASS_NAME_PREFIX + "MainCommandPanel");
  }

  protected Widget createCopyright(String stylePrefix) {
    Flow copyright = new Flow();
    copyright.addStyleName(stylePrefix + "Copyright");

    Image logo = new Image(UiConstants.wtfplLogo());
    logo.addStyleName(stylePrefix + "Copyright-logo");
    copyright.add(logo);

    Label label = new Label(UiConstants.wtfplLabel());
    label.addStyleName(stylePrefix + "Copyright-label");
    copyright.add(label);

    final String url = UiConstants.wtfplUrl();
    copyright.setTitle(url);

    copyright.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        BrowsingContext.open(url);
      }
    });

    return copyright;
  }

  protected void createExpanders() {
    CustomComplex container = new CustomComplex(DomUtils.createElement(Tags.NAV),
        StyleUtils.CLASS_NAME_PREFIX + "workspace-expander");

    Toggle toggle = new Toggle(FontAwesome.LONG_ARROW_LEFT, FontAwesome.LONG_ARROW_RIGHT,
        StyleUtils.CLASS_NAME_PREFIX + "west-toggle", false);
    setWestToggle(toggle);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getWestToggle().isChecked()) {
          expand(Direction.WEST, true);
        } else {
          compress(Direction.WEST, true);
        }

        refreshExpanders();
      }
    });

    container.add(toggle);

    toggle = new Toggle(FontAwesome.LONG_ARROW_UP, FontAwesome.LONG_ARROW_DOWN,
        StyleUtils.CLASS_NAME_PREFIX + "north-toggle", false);
    setNorthToggle(toggle);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getNorthToggle().isChecked()) {
          expand(Direction.NORTH, true);
        } else {
          compress(Direction.NORTH, true);
        }

        refreshExpanders();
      }
    });

    container.add(toggle);

    toggle = new Toggle(FontAwesome.EXPAND, FontAwesome.COMPRESS,
        StyleUtils.CLASS_NAME_PREFIX + "workspace-maximizer", false);
    setMaximizer(toggle);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getMaximizer().isChecked()) {
          for (Direction direction : Direction.values()) {
            if (!direction.isCenter()) {
              expand(direction, false);
            }
          }

        } else {
          for (Map.Entry<Direction, Integer> entry : hidden.entrySet()) {
            getScreenPanel().setDirectionSize(entry.getKey(), entry.getValue(), false);
          }
          hidden.clear();
        }

        getScreenPanel().doLayout();
        refreshExpanders();
      }
    });

    container.add(toggle);

    getWorkspace().addToTabBar(container);

    refreshExpanders();
  }

  protected Widget createLogo(ScheduledCommand command) {
    String imageUrl = Settings.getLogoImage();
    if (BeeUtils.isEmpty(imageUrl)) {
      return null;
    }

    Image widget = new Image(imageUrl);
    widget.setAlt("logo");

    final String title = Settings.getLogoTitle();
    if (!BeeUtils.isEmpty(title)) {
      widget.setTitle(title);
    }

    final String openUrl = Settings.getLogoOpen();
    if (BeeUtils.isEmpty(openUrl)) {
      if (command == null) {
        widget.getElement().getStyle().setCursor(Cursor.DEFAULT);
      } else {
        widget.setCommand(command);
      }

    } else {
      if (BeeUtils.isEmpty(title)) {
        widget.setTitle(openUrl);
      }

      widget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          BrowsingContext.open(openUrl);
        }
      });
    }

    return widget;
  }

  protected Panel createMenuPanel() {
    return new Flow(StyleUtils.CLASS_NAME_PREFIX + "MainMenu");
  }

  protected Widget createSearch() {
    return Global.getSearchWidget();
  }

  protected void createUi() {
    Split p = new Split(0);
    StyleUtils.occupy(p);
    p.addStyleName(getScreenStyle());

    Pair<? extends IdentifiableWidget, Integer> north = initNorth();
    if (north != null) {
      p.addNorth(north.getA(), north.getB());
    }

    Pair<? extends IdentifiableWidget, Integer> south = initSouth();
    if (south != null) {
      p.addSouth(south.getA(), south.getB());
    }

    Pair<? extends IdentifiableWidget, Integer> west = initWest();
    if (west != null) {
      p.addWest(west.getA(), west.getB());
    }

    Pair<? extends IdentifiableWidget, Integer> east = initEast();
    if (east != null) {
      p.addEast(east.getA(), east.getB());
    }

    IdentifiableWidget center = initCenter();
    if (center != null) {
      p.add(center);
    }

    BodyPanel.get().add(p);
    setScreenPanel(p);

    if (getWorkspace() != null) {
      createExpanders();
    }
  }

  protected Widget createUserContainer() {
    Horizontal userContainer = new Horizontal();

    if (Settings.showUserPhoto()) {
      Flow photoContainer = new Flow(StyleUtils.CLASS_NAME_PREFIX + "UserPhotoContainer");
      userContainer.add(photoContainer);
      setUserPhotoContainer(photoContainer);
    }

    Label signature = new Label();
    signature.addStyleName(StyleUtils.CLASS_NAME_PREFIX + "UserSignature");
    userContainer.add(signature);
    setUserSignature(signature);

    signature.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onUserSignatureClick();
      }
    });

    Simple exitContainer = new Simple();
    exitContainer.addStyleName(StyleUtils.CLASS_NAME_PREFIX + "UserExitContainer");

    FaLabel exit = new FaLabel(FontAwesome.SIGN_OUT);
    exit.addStyleName(StyleUtils.CLASS_NAME_PREFIX + "UserExit");
    exit.setTitle(Localized.getConstants().signOut());

    exit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Global.getMsgBoxen().confirm(Localized.getMessages().endSession(Settings.getAppName()),
            Icon.QUESTION, Lists.newArrayList(Localized.getConstants().questionLogout()),
            Localized.getConstants().yes(), Localized.getConstants().no(),
            new ConfirmationCallback() {
              @Override
              public void onConfirm() {
                Bee.exit();
              }
            }, null, StyleUtils.className(FontSize.MEDIUM), null, null);
      }
    });

    exitContainer.setWidget(exit);
    userContainer.add(exitContainer);

    return userContainer;
  }

  protected int getNorthHeight(int defHeight) {
    return BeeUtils.positive(Settings.getPropertyInt("northHeight"), defHeight);
  }

  protected Notification getNotification() {
    return notification;
  }

  protected String getScreenStyle() {
    return StyleUtils.CLASS_NAME_PREFIX + "Screen";
  }

  protected void hideProgressPanel() {
    getScreenPanel().setWidgetSize(getProgressPanel(), 0);
  }

  protected IdentifiableWidget initCenter() {
    Workspace area = new Workspace();
    setWorkspace(area);
    return area;
  }

  protected Pair<? extends IdentifiableWidget, Integer> initEast() {
    return Pair.of(ClientLogManager.getLogPanel(), ClientLogManager.getInitialPanelSize());
  }

  protected Pair<? extends IdentifiableWidget, Integer> initNorth() {
    Complex panel = new Complex();
    panel.addStyleName(StyleUtils.CLASS_NAME_PREFIX + "NorthContainer");

    Widget logo = createLogo(null);
    if (logo != null) {
      logo.addStyleName(StyleUtils.CLASS_NAME_PREFIX + "Logo");
      panel.add(logo);
    }

    Widget search = createSearch();
    if (search != null) {
      panel.add(search);
    }

    Panel commandContainer = createCommandPanel();
    if (commandContainer != null) {
      panel.add(commandContainer);
      setCommandPanel(commandContainer);
    }

    Panel menuContainer = createMenuPanel();
    if (menuContainer != null) {
      panel.add(menuContainer);
      setMenuPanel(menuContainer);
    }

    Widget userContainer = createUserContainer();
    userContainer.addStyleName(StyleUtils.CLASS_NAME_PREFIX + "UserContainer");
    panel.add(userContainer);

    Notification nw = new Notification();
    nw.addStyleName(StyleUtils.CLASS_NAME_PREFIX + "MainNotificationContainer");
    panel.add(nw);
    setNotification(nw);

    return Pair.of(panel, getNorthHeight(100));
  }

  protected Pair<? extends IdentifiableWidget, Integer> initSouth() {
    Flow panel = new Flow();
    panel.addStyleName(StyleUtils.CLASS_NAME_PREFIX + "ProgressPanel");
    setProgressPanel(panel);

    return Pair.of(panel, 0);
  }

  protected Pair<? extends IdentifiableWidget, Integer> initWest() {
    setCentralScrutinizer(new CentralScrutinizer());

    Flow panel = new Flow();
    panel.add(getCentralScrutinizer());
    panel.add(createCopyright(StyleUtils.CLASS_NAME_PREFIX));

    int width = BeeUtils.resize(Window.getClientWidth(), 1000, 2000, 240, 320);
    return Pair.of(panel, width);
  }

  protected void onUserSignatureClick() {
    BeeRow row = BeeKeeper.getUser().getSettingsRow();
    if (row != null) {
      RowEditor.openRow(AdministrationConstants.VIEW_USER_SETTINGS, row, true,
          new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              BeeKeeper.getUser().updateSettings(result);
            }
          });
    }
  }

  protected void setMenuPanel(HasWidgets menuPanel) {
    this.menuPanel = menuPanel;
  }

  protected void setNotification(Notification notification) {
    this.notification = notification;
  }

  protected void setProgressPanel(Panel progressPanel) {
    this.progressPanel = progressPanel;
  }

  protected void setScreenPanel(Split screenPanel) {
    this.screenPanel = screenPanel;
  }

  protected void setUserPhotoContainer(HasWidgets userPhotoContainer) {
    this.userPhotoContainer = userPhotoContainer;
  }

  protected void setUserSignature(HasHtml userSignature) {
    this.userSignature = userSignature;
  }

  protected void showProgressPanel() {
    getScreenPanel().setWidgetSize(getProgressPanel(), 32);
  }

  private boolean compress(Direction direction, boolean doLayout) {
    Integer size = hidden.remove(direction);

    if (BeeUtils.isPositive(size)) {
      getScreenPanel().setDirectionSize(direction, size, doLayout);
      return true;

    } else {
      return false;
    }
  }

  private boolean expand(Direction direction, boolean doLayout) {
    int size = getScreenPanel().getDirectionSize(direction);

    if (size > 0) {
      hidden.put(direction, size);
      getScreenPanel().setDirectionSize(direction, 0, doLayout);
      return true;

    } else {
      return false;
    }
  }

  private CentralScrutinizer getCentralScrutinizer() {
    return centralScrutinizer;
  }

  private Toggle getMaximizer() {
    return maximizer;
  }

  private HasWidgets getMenuPanel() {
    return menuPanel;
  }

  private Toggle getNorthToggle() {
    return northToggle;
  }

  private Panel getProgressPanel() {
    return progressPanel;
  }

  private HasWidgets getUserPhotoContainer() {
    return userPhotoContainer;
  }

  private HasHtml getUserSignature() {
    return userSignature;
  }

  private Toggle getWestToggle() {
    return westToggle;
  }

  private void refreshExpanders() {
    boolean checked;
    String title;

    if (getWestToggle() != null) {
      checked = hidden.containsKey(Direction.WEST);

      title = checked
          ? Localized.getConstants().actionWorkspaceRestoreSize()
          : Localized.getConstants().actionWorkspaceEnlargeToLeft();
      getWestToggle().setTitle(title);

      if (getWestToggle().isChecked() != checked) {
        getWestToggle().setChecked(checked);
      }
    }

    if (getNorthToggle() != null) {
      checked = hidden.containsKey(Direction.NORTH);

      title = checked
          ? Localized.getConstants().actionWorkspaceRestoreSize()
          : Localized.getConstants().actionWorkspaceEnlargeUp();
      getNorthToggle().setTitle(title);

      if (getNorthToggle().isChecked() != checked) {
        getNorthToggle().setChecked(checked);
      }
    }

    if (getMaximizer() != null) {
      checked = !hidden.isEmpty();

      title = checked
          ? Localized.getConstants().actionWorkspaceRestoreSize()
          : Localized.getConstants().actionWorkspaceMaxSize();
      getMaximizer().setTitle(title);

      if (getMaximizer().isChecked() != checked) {
        getMaximizer().setChecked(checked);
      }
    }
  }

  private void setCentralScrutinizer(CentralScrutinizer centralScrutinizer) {
    this.centralScrutinizer = centralScrutinizer;
  }

  private void setCommandPanel(HasWidgets commandPanel) {
    this.commandPanel = commandPanel;
  }

  private void setMaximizer(Toggle maximizer) {
    this.maximizer = maximizer;
  }

  private void setNorthToggle(Toggle northToggle) {
    this.northToggle = northToggle;
  }

  private void setWestToggle(Toggle westToggle) {
    this.westToggle = westToggle;
  }

  private void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  private void updatePanel(HasWidgets panel, IdentifiableWidget widget) {
    if (panel == null) {
      notifyWarning("updatePanel: panel is null");
      return;
    }
    if (widget == null) {
      notifyWarning("updatePanel: widget is null");
      return;
    }

    panel.clear();
    panel.add(widget.asWidget());
  }
}
