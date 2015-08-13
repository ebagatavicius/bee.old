package com.butent.bee.client.screen;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Bee;
import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Screen;
import com.butent.bee.client.Settings;
import com.butent.bee.client.Users;
import com.butent.bee.client.cli.Shell;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Notification;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.Animation;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Complex;
import com.butent.bee.client.layout.CustomComplex;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.logging.ClientLogManager;
import com.butent.bee.client.menu.MenuCommand;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.screen.TilePanel.Tile;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasProgress;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.Theme;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.BrowsingContext;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles default (desktop) screen implementation.
 */

public class ScreenImpl implements Screen {

  private static final BeeLogger logger = LogUtils.getLogger(ScreenImpl.class);

  private static final Set<Direction> hidableDirections = EnumSet.of(Direction.WEST,
      Direction.NORTH, Direction.EAST);

  private static final String CONTAINER = "Container";
  private static final String PHOTO_URL = "images/photo/";
  private static FaLabel userLabel = new FaLabel(FontAwesome.USER);
  private static FaLabel emailLabel = new FaLabel(FontAwesome.ENVELOPE_O);
  private static Flow flowUserContainer = new Flow();
  private static Flow flowEmailContainer = new Flow();
  private static Flow flowOnlineUserSize = new Flow();
  private static Flow flowOnlineEmailSize = new Flow();

  private static final String DEFAULT_PHOTO_IMAGE = "images/defaultUser.png";
  public static final HtmlTable NOTIFICATION_CONTENT = new HtmlTable(BeeConst.CSS_CLASS_PREFIX
      + "NotificationBar-Content");

  private static final int NORTH_HEIGHT = 112;
  private static final int MENU_HEIGHT = 50;

  private Split screenPanel;
  private CentralScrutinizer centralScrutinizer;

  private Workspace workspace;
  private HasWidgets commandPanel;

  private HasWidgets menuPanel;

  private HasWidgets userPhotoContainer;
  private HasHtml userSignature;

  private Notification notification;

  private Panel progressPanel;

  private final Map<String, Integer> hidden = new HashMap<>();

  private Toggle northToggle;
  private Toggle eastWestToggle;

  private Toggle maximizer;

  private final CustomDiv newsBadge = new CustomDiv();

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
      widget.asWidget().addStyleName(BeeConst.CSS_CLASS_PREFIX + "MainCommandPanelItem");
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
  public void closeAll() {
    if (getWorkspace() != null) {
      getWorkspace().clear();
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
      for (Map.Entry<String, Integer> entry : hidden.entrySet()) {
        info.add(new ExtendedProperty("Hidden", entry.getKey(),
            BeeUtils.toString(entry.getValue())));
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
    Set<Direction> directions = EnumSet.noneOf(Direction.class);

    for (String id : hidden.keySet()) {
      Direction direction = getScreenPanel().getWidgetDirection(id);
      if (direction != null) {
        directions.add(direction);
      }
    }
    return directions;
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
        changed |= expand(direction);
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
    int eastMargin = getEastMargin();
    if (eastMargin > 0) {
      getScreenPanel().addEast(new CustomDiv(BeeConst.CSS_CLASS_PREFIX + "WorkspaceEastMargin"),
          eastMargin);
      getScreenPanel().doLayout();
    }

    if (getWorkspace() != null) {
      getWorkspace().onStart();
    }

    Global.getSearch().focus();

    if (!Global.getSpaces().isEmpty() && !containsDomainEntry(Domain.WORKSPACES, null)) {
      addDomainEntry(Domain.WORKSPACES, Global.getSpaces().getPanel(), null, null);
    }
    if (!Global.getReportSettings().isEmpty() && !containsDomainEntry(Domain.REPORTS, null)) {
      addDomainEntry(Domain.REPORTS, Global.getReportSettings().getPanel(), null, null);
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
  public void restore(List<String> spaces, boolean append) {
    if (getWorkspace() != null) {
      getWorkspace().restore(spaces, append);
    }
  }

  @Override
  public String serialize() {
    if (getWorkspace() == null) {
      return null;
    } else {
      return getWorkspace().serialize();
    }
  }

  @Override
  public void showInNewPlace(IdentifiableWidget widget) {
    getWorkspace().openInNewPlace(widget);
  }

  @Override
  public void show(IdentifiableWidget widget) {
    if (BeeKeeper.getUser().openInNewTab()) {
      showInNewPlace(widget);
    } else {
      updateActivePanel(widget);
    }
  }

  @Override
  public void start(UserData userData) {
    updateUserData(userData);

    if (!BeeKeeper.getUser().isMenuVisible()) {
      getScreenPanel().setDirectionSize(Direction.NORTH, getNorthHeight(NORTH_HEIGHT), true);
    }

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
  public boolean updateProgress(String id, String label, double value) {
    if (getProgressPanel() != null && !BeeUtils.isEmpty(id)) {
      Widget item = DomUtils.getChildById(getProgressPanel(), id);

      if (item instanceof HasProgress) {
        ((HasProgress) item).update(label, value);
        return true;
      }
    }
    return false;
  }

  @Override
  public void updateUserData(UserData userData) {
    if (userData != null && getUserPhotoContainer() != null) {
      getUserPhotoContainer().clear();
      final Image image;

      String photoFileName = userData.getPhotoFileName();
      if (!BeeUtils.isEmpty(photoFileName)) {
        image = new Image(PhotoRenderer.getUrl(photoFileName));
      } else {
        image = new Image(DEFAULT_PHOTO_IMAGE);
      }

      image.setAlt(userData.getLogin());
      image.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserPhoto");

      image.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          NOTIFICATION_CONTENT.setWidget(0, 0, createUserPanel());
          NOTIFICATION_CONTENT.setWidget(1, 0, createCalendarPanel());

          if (BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.NEWS)) {
            NOTIFICATION_CONTENT.setWidget(2, 0,
                Global.getNewsAggregator().getNewsPanel().asWidget());
          }

          final Popup popup = new Popup(OutsideClick.CLOSE,
              BeeConst.CSS_CLASS_PREFIX + "NotificationBar");
          popup.setWidget(NOTIFICATION_CONTENT);
          popup.setHideOnEscape(true);

          popup.setAnimationEnabled(true);
          popup.setAnimation(new Animation(300) {
            int left;
            int width;

            @Override
            public void start() {
              this.width = getPopup().getOffsetWidth();
              this.left = DomUtils.getClientWidth() - this.width;

              if (getPopup().isShowing()) {
                StyleUtils.setOpacity(getPopup(), BeeConst.DOUBLE_ZERO);
                StyleUtils.setLeft(getPopup(), this.width);
              }
              super.start();
            }

            @Override
            protected void onComplete() {
              if (getPopup().isShowing()) {
                StyleUtils.setLeft(getPopup(), this.left);
              }
              getPopup().getElement().getStyle().clearOpacity();
              super.onComplete();
            }

            @Override
            protected boolean run(double elapsed) {
              if (isCanceled()) {
                return false;
              } else {
                StyleUtils.setOpacity(getPopup(), getFactor(elapsed));
                double x = this.left + (1 - getFactor(elapsed)) * this.width;
                StyleUtils.setLeft(getPopup(), x, CssUnit.PX);
                return true;
              }
            }
          });

          popup.setPopupPositionAndShow(new Popup.PositionCallback() {
            @Override
            public void setPosition(int offsetWidth, int offsetHeight) {
              popup.setPopupPosition(DomUtils.getClientWidth() - offsetWidth, 0);
            }
          });
        }
      });

      newsBadge.setStyleName(BeeConst.CSS_CLASS_PREFIX + "NewsSize-None");

      getUserPhotoContainer().add(newsBadge);
      getUserPhotoContainer().add(image);
    }

    if (getUserSignature() != null) {
      getUserSignature().setText(BeeUtils.trim(userData.getUserSign()));
    }
  }

  protected void activateShell() {
    if (getCentralScrutinizer() != null) {
      getCentralScrutinizer().activateShell();

    } else if (getScreenPanel() != null && getScreenPanel().getDirectionSize(Direction.WEST) <= 0) {
      List<Widget> children = getScreenPanel().getDirectionChildren(Direction.WEST, false);
      for (Widget widget : children) {
        if (UiHelper.isOrHasChild(widget, Shell.class)) {
          getScreenPanel().setDirectionSize(Direction.WEST, getWidth() / 5, true);
          break;
        }
      }
    }
  }

  protected void bindShellActivation(IdentifiableWidget widget) {
    final String id = widget.getId();

    Binder.addClickHandler(widget.asWidget(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (EventUtils.hasModifierKey(event.getNativeEvent())
            && EventUtils.isTargetId(event, id)) {
          activateShell();
        }
      }
    });
  }

  protected Panel createCommandPanel() {
    return new Flow(BeeConst.CSS_CLASS_PREFIX + "MainCommandPanel");
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
        BeeConst.CSS_CLASS_PREFIX + "Workspace-expander");

    Toggle toggle = new Toggle(FontAwesome.LONG_ARROW_LEFT, FontAwesome.LONG_ARROW_RIGHT,
        BeeConst.CSS_CLASS_PREFIX + "east-west-toggle", false);
    setEastWestToggle(toggle);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getEastWestToggle().isChecked()) {
          expand(Direction.WEST);
          expand(Direction.EAST);
        } else {
          compress(Direction.WEST);
          compress(Direction.EAST);
        }

        getScreenPanel().doLayout();
        refreshExpanders();
      }
    });

    container.add(toggle);

    toggle = new Toggle(FontAwesome.LONG_ARROW_UP, FontAwesome.LONG_ARROW_DOWN,
        BeeConst.CSS_CLASS_PREFIX + "north-toggle", false);
    setNorthToggle(toggle);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getNorthToggle().isChecked()) {
          expand(Direction.NORTH);
        } else {
          compress(Direction.NORTH);
        }

        getScreenPanel().doLayout();
        refreshExpanders();
      }
    });

    container.add(toggle);

    toggle = new Toggle(FontAwesome.EXPAND, FontAwesome.COMPRESS,
        BeeConst.CSS_CLASS_PREFIX + "workspace-maximizer", false);
    setMaximizer(toggle);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getMaximizer().isChecked()) {
          for (Direction direction : hidableDirections) {
            expand(direction);
          }

        } else {
          for (Map.Entry<String, Integer> entry : hidden.entrySet()) {
            getScreenPanel().setWidgetSize(entry.getKey(), entry.getValue(), false);
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
    return new Flow(BeeConst.CSS_CLASS_PREFIX + "MainMenu");
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
      bindShellActivation(north.getA());
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
      Flow photoContainer = new Flow(BeeConst.CSS_CLASS_PREFIX + "UserPhotoContainer");
      userContainer.add(photoContainer);
      setUserPhotoContainer(photoContainer);
    }
    return userContainer;
  }

  protected int getEastMargin() {
    return Theme.getWorkspaceMarginRight();
  }

  protected int getNorthHeight(int defHeight) {
    int height = BeeUtils.positive(Settings.getInt("northHeight"), defHeight);
    if (!BeeKeeper.getUser().isMenuVisible() && height > MENU_HEIGHT) {
      height -= MENU_HEIGHT;
    }
    return height;
  }

  protected Notification getNotification() {
    return notification;
  }

  protected String getScreenStyle() {
    return BeeConst.CSS_CLASS_PREFIX + "Screen";
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
    panel.addStyleName(BeeConst.CSS_CLASS_PREFIX + "NorthContainer");

    Widget logo = createLogo(null);
    if (logo != null) {
      logo.addStyleName(BeeConst.CSS_CLASS_PREFIX + "Logo");
      panel.add(logo);
    }

    Widget search = createSearch();
    if (search != null) {
      panel.add(search);
    }

    Panel commandContainer = createCommandPanel();
    if (commandContainer != null) {
      panel.add(commandContainer);

      commandContainer.add(onlineEmail());
      commandContainer.add(onlineUsers());

      setCommandPanel(commandContainer);
    }

    Panel menuContainer = createMenuPanel();
    if (menuContainer != null) {
      panel.add(menuContainer);
      setMenuPanel(menuContainer);
    }

    Widget userContainer = createUserContainer();
    userContainer.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserContainer");
    panel.add(userContainer);

    Notification nw = new Notification();
    nw.addStyleName(BeeConst.CSS_CLASS_PREFIX + "MainNotificationContainer");
    panel.add(nw);
    setNotification(nw);

    return Pair.of(panel, getNorthHeight(NORTH_HEIGHT));
  }

  protected Pair<? extends IdentifiableWidget, Integer> initSouth() {
    Flow panel = new Flow();
    panel.addStyleName(BeeConst.CSS_CLASS_PREFIX + "ProgressPanel");
    setProgressPanel(panel);

    return Pair.of(panel, 0);
  }

  protected Pair<? extends IdentifiableWidget, Integer> initWest() {
    setCentralScrutinizer(new CentralScrutinizer());

    final Label createButton = new Label("+ " + Localized.getConstants().create());
    createButton.addStyleName(BeeConst.CSS_CLASS_PREFIX + "CreateButton");

    createButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        RowFactory.showMenu(createButton);
      }
    });

    Flow myEnv = new Flow(BeeConst.CSS_CLASS_PREFIX + "MyEnvironment");
    myEnv.add(new FaLabel(FontAwesome.BOOKMARK));
    myEnv.add(new Label(Localized.getConstants().myEnvironment()));

    Flow panel = new Flow(BeeConst.CSS_CLASS_PREFIX + "WestContainer");

    panel.add(createButton);
    panel.add(myEnv);
    panel.add(getCentralScrutinizer());
    panel.add(createCopyright(BeeConst.CSS_CLASS_PREFIX));

    int width = BeeUtils.resize(DomUtils.getClientWidth(), 1000, 2000, 240, 320);
    return Pair.of(panel, width);
  }

  protected void onUserSignatureClick() {
    BeeRow row = BeeKeeper.getUser().getSettingsRow();
    if (row != null) {
      RowEditor.open(AdministrationConstants.VIEW_USER_SETTINGS, row, Opener.MODAL,
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

  private boolean compress(Direction direction) {
    Map<String, Integer> children = new HashMap<>();

    for (Map.Entry<String, Integer> entry : hidden.entrySet()) {
      if (getScreenPanel().getWidgetDirection(entry.getKey()) == direction) {
        children.put(entry.getKey(), entry.getValue());
      }
    }

    if (children.isEmpty()) {
      return false;

    } else {
      for (Map.Entry<String, Integer> entry : children.entrySet()) {
        String id = entry.getKey();
        Integer size = entry.getValue();

        if (BeeUtils.isPositive(size)) {
          getScreenPanel().setWidgetSize(id, size, false);
        }

        hidden.remove(id);
      }
      return true;
    }
  }

  private boolean expand(Direction direction) {
    List<Widget> children = getScreenPanel().getDirectionChildren(direction, false);
    int count = 0;

    for (Widget child : children) {
      String id = DomUtils.getId(child);
      int size = getScreenPanel().getWidgetSize(child);

      if (!BeeUtils.isEmpty(id) && size > 0) {
        getScreenPanel().setWidgetSize(child, 0);
        hidden.put(id, size);

        count++;
      }
    }

    return count > 0;
  }

  private CentralScrutinizer getCentralScrutinizer() {
    return centralScrutinizer;
  }

  private Toggle getEastWestToggle() {
    return eastWestToggle;
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

  private void refreshExpanders() {
    boolean checked;
    String title;

    Set<Direction> hiddenDirections = getHiddenDirections();

    if (getEastWestToggle() != null) {
      checked = hiddenDirections.contains(Direction.EAST)
          || hiddenDirections.contains(Direction.WEST);

      title = checked
          ? Localized.getConstants().actionWorkspaceRestoreSize()
          : Localized.getConstants().actionWorkspaceEnlargeToLeft();
      getEastWestToggle().setTitle(title);

      if (getEastWestToggle().isChecked() != checked) {
        getEastWestToggle().setChecked(checked);
      }
    }

    if (getNorthToggle() != null) {
      checked = hiddenDirections.contains(Direction.NORTH);

      title = checked
          ? Localized.getConstants().actionWorkspaceRestoreSize()
          : Localized.getConstants().actionWorkspaceEnlargeUp();
      getNorthToggle().setTitle(title);

      if (getNorthToggle().isChecked() != checked) {
        getNorthToggle().setChecked(checked);
      }
    }

    if (getMaximizer() != null) {
      checked = true;
      for (Direction direction : hidableDirections) {
        if (getScreenPanel().getDirectionSize(direction) > 0) {
          checked = false;
          break;
        }
      }

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

  private void setEastWestToggle(Toggle eastWestToggle) {
    this.eastWestToggle = eastWestToggle;
  }

  private void setMaximizer(Toggle maximizer) {
    this.maximizer = maximizer;
  }

  private void setNorthToggle(Toggle northToggle) {
    this.northToggle = northToggle;
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

  private static Flow onlineUsers() {

    final Set<String> sessions = Global.getUsers().getAllSessions();

    flowUserContainer.addStyleName(BeeConst.CSS_CLASS_PREFIX + "HeaderIcon-Container");
    flowUserContainer.add(flowOnlineUserSize);
    userLabel.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUsers");
    flowOnlineUserSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUserSize-None");

    userLabel.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent ev) {
        final HtmlTable table = new HtmlTable(BeeConst.CSS_CLASS_PREFIX + "PopupUsersContent");
        int r = 0;

        Users users = Global.getUsers();

        if (sessions.size() > 0) {
          for (String session : sessions) {
            UserData user = users.getUserData(users.getUserIdBySession(session));
            Image img;
            if (user.getPhotoFileName() != null) {
              img = new Image(PHOTO_URL + user.getPhotoFileName());
            } else {
              img = new Image(DEFAULT_PHOTO_IMAGE);
            }
            img.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUsersPhoto");
            table.setWidget(r, 0, img);
            table.setText(r, 1, user.getFirstName() + " " + user.getLastName());
            DomUtils.setDataProperty(table.getRow(r++), CONTAINER, user.getPerson());
          }

          if (r > 0) {
            table.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent arg) {
                Element targetElement = EventUtils.getEventTargetElement(arg);
                TableRowElement rowElement = DomUtils.getParentRow(targetElement, true);
                String index = DomUtils.getDataProperty(rowElement, CONTAINER);
                UiHelper.closeDialog(table);

                RowEditor.open(ClassifierConstants.VIEW_PERSONS, Long.valueOf(index),
                    Opener.NEW_TAB);
              }
            });
            Popup popup = new Popup(OutsideClick.CLOSE);
            popup.setStyleName(BeeConst.CSS_CLASS_PREFIX + "PopupUsers");
            popup.setWidget(table);
            popup.setHideOnEscape(true);
            popup.showRelativeTo(userLabel.getElement());
          }
        }
      }
    });

    flowUserContainer.add(userLabel);
    return flowUserContainer;
  }

  public static void updateOnlineUsers() {

    FaLabel label = BeeKeeper.getScreen().getOnlineUserLabel();
    Flow userSize = BeeKeeper.getScreen().getOnlineUserSize();

    if (label == null || userSize == null) {
      return;
    }

    int size = Global.getUsers().getAllSessions().size();

    if (size > 0) {
      userSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUserSize");
      label.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUsers" + "-Selected");
      userSize.getElement().setInnerText(String.valueOf(size));

    } else {
      label.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUsers");
      userSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineUserSize-None");
    }
  }

  public static Widget onlineEmail() {

    final MenuCommand command = new MenuCommand(MenuService.OPEN_MAIL, null);

    flowEmailContainer.addStyleName(BeeConst.CSS_CLASS_PREFIX + "EmailIcon-Container");
    flowEmailContainer.add(flowOnlineEmailSize);

    flowOnlineEmailSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineEmailSize-None");
    emailLabel.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineEmail");

    emailLabel.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg0) {

        if (command.getService().equals(MenuService.OPEN_MAIL)) {
          command.execute();
        }
      }
    });

    flowEmailContainer.add(emailLabel);
    return flowEmailContainer;
  }

  public static void updateOnlineEmails(int size) {

    FaLabel label = BeeKeeper.getScreen().getOnlineEmailLabel();
    Flow emailSize = BeeKeeper.getScreen().getOnlineEmailSize();

    if (label == null || emailSize == null) {
      return;
    }

    if (size > 0) {
      emailSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineEmailSize");
      label.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineEmail" + "-Selected");
      emailSize.getElement().setInnerText(String.valueOf(size));

    } else {
      label.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineEmail");
      emailSize.setStyleName(BeeConst.CSS_CLASS_PREFIX + "OnlineEmailSize-None");
    }
  }

  @Override
  public void updateNewsSize(int size) {
    if (size > 0) {
      newsBadge.setStyleName(BeeConst.CSS_CLASS_PREFIX + "NewsSize");
      newsBadge.getElement().setInnerText(String.valueOf(size));
    } else {
      newsBadge.setStyleName(BeeConst.CSS_CLASS_PREFIX + "NewsSize-None");
      newsBadge.getElement().setInnerText(BeeConst.STRING_EMPTY);
    }
  }

  @Override
  public FaLabel getOnlineUserLabel() {
    return userLabel;
  }

  @Override
  public Flow getOnlineUserSize() {
    return flowOnlineUserSize;
  }

  @Override
  public FaLabel getOnlineEmailLabel() {
    return emailLabel;
  }

  @Override
  public Flow getOnlineEmailSize() {
    return flowOnlineEmailSize;
  }

  private Widget createUserPanel() {
    Horizontal userContainer = new Horizontal();
    Flow userPanel = new Flow();
    Label signature = new Label();
    Flow settingsCnt = new Flow();

    FaLabel sett = new FaLabel(FontAwesome.GEAR);
    FaLabel help = new FaLabel(FontAwesome.QUESTION);
    FaLabel menuHide = new FaLabel(FontAwesome.THUMB_TACK);

    userContainer.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserContainer");
    userPanel.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserPanel");
    settingsCnt.addStyleName(BeeConst.CSS_CLASS_PREFIX + "SettingsContainer");
    signature.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserSignature");
    sett.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserControlIcon");
    help.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserControlIcon");

    sett.setTitle(Localized.getConstants().settings());
    help.setTitle(Localized.getConstants().help());
    menuHide.setTitle(Localized.getConstants().hideOrShowMenu());

    styleMenuToggle(menuHide, BeeKeeper.getUser().isMenuVisible());

    sett.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent arg0) {
        onUserSignatureClick();
        UiHelper.closeDialog((Widget) arg0.getSource());
      }
    });

    help.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent arg1) {
        BrowsingContext.open(UiConstants.helpURL());
      }
    });

    menuHide.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent arg2) {
        boolean visible = !BeeKeeper.getUser().isMenuVisible();
        BeeKeeper.getUser().setMenuVisible(visible);

        styleMenuToggle((Widget) arg2.getSource(), visible);

        getScreenPanel().setDirectionSize(Direction.NORTH, getNorthHeight(NORTH_HEIGHT), true);
      }
    });

    settingsCnt.add(sett);
    settingsCnt.add(help);
    settingsCnt.add(menuHide);

    signature.setText(BeeUtils.trim(BeeKeeper.getUser().getUserSign()));

    Flow exitContainer = new Flow();
    exitContainer.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserExitContainer");

    Label exit = new Label(Localized.getConstants().signOut());
    exit.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserExit");
    exit.setTitle(Localized.getConstants().signOut());

    exit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Bee.exit();
      }
    });

    exitContainer.add(signature);
    exitContainer.add(exit);

    Image image;
    String photoFileName = BeeKeeper.getUser().getUserData().getPhotoFileName();
    if (!BeeUtils.isEmpty(photoFileName)) {
      image = new Image(PhotoRenderer.getUrl(photoFileName));
    } else {
      image = new Image(DEFAULT_PHOTO_IMAGE);
    }

    image.setAlt(BeeKeeper.getUser().getLogin());
    image.addStyleName(BeeConst.CSS_CLASS_PREFIX + "UserPhoto");

    image.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg3) {
        UiHelper.closeDialog((Widget) arg3.getSource());
      }
    });

    userPanel.add(settingsCnt);
    userContainer.add(exitContainer);
    userContainer.add(image);
    userPanel.add(userContainer);

    return userPanel;
  }

  private static void styleMenuToggle(Widget toggle, boolean visible) {
    toggle.setStyleName(BeeConst.CSS_CLASS_PREFIX + "UserMenuIcon", visible);
    toggle.setStyleName(BeeConst.CSS_CLASS_PREFIX + "UserMenuIcon-Selected", !visible);
  }

  private static Widget createCalendarPanel() {
    Flow userCal = new Flow();
    userCal.setStyleName(BeeConst.CSS_CLASS_PREFIX + "CalendarContainer");

    JustDate firstDay = TimeUtils.today();
    JustDate day;

    for (int i = 0; i < 5; i++) {

      Label lblD = new Label();
      Label lblWd = new Label();
      Flow cal = new Flow();

      lblD.setStyleName(BeeConst.CSS_CLASS_PREFIX + "MonthDay");
      lblWd.setStyleName(BeeConst.CSS_CLASS_PREFIX + "WeekDay");

      if (i == 0) {

        lblD.addStyleName(BeeConst.CSS_CLASS_PREFIX + "MonthDayToday");
        lblD.setText(String.valueOf(firstDay.getDom()));
        lblWd.setText(String.valueOf(Format.renderDayOfWeekShort(firstDay.getDow())));
      } else {
        day = TimeUtils.toDateOrNull(firstDay.getDays() + i);
        lblD.setText(String.valueOf(day.getDom()));
        lblWd.setText(String.valueOf(Format.renderDayOfWeekShort(day.getDow())));
      }
      cal.add(lblWd);
      cal.add(lblD);
      userCal.add(cal);
    }
    return userCal;
  }
}
