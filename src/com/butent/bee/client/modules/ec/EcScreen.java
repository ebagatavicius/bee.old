package com.butent.bee.client.modules.ec;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.cli.Shell;
import com.butent.bee.client.dialog.Notification;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.logging.ClientLogManager;
import com.butent.bee.client.modules.administration.PasswordService;
import com.butent.bee.client.modules.ec.EcCommandWidget.Type;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.screen.ScreenImpl;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;

import java.util.ArrayList;
import java.util.List;

public class EcScreen extends ScreenImpl {

  private static void createCommands(HasWidgets container) {
    String panelStyle = "commandPanel";

    Horizontal info = new Horizontal();
    EcStyles.add(info, panelStyle);
    EcStyles.add(info, panelStyle, "info");

    info.add(createCommandWidget(EcConstants.SVC_FINANCIAL_INFORMATION,
        Localized.getConstants().ecFinancialInformation(), Type.LINK));

    info.add(createCommandWidget(EcConstants.SVC_SHOW_TERMS_OF_DELIVERY,
        Localized.getConstants().ecTermsOfDelivery(), Type.LINK));
    info.add(createCommandWidget(EcConstants.SVC_SHOW_CONTACTS,
        Localized.getConstants().ecContacts(), Type.LINK));

    container.add(info);

    String styleName = "searchBy";

    Horizontal searchBy = new Horizontal();
    EcStyles.add(searchBy, panelStyle);
    EcStyles.add(searchBy, panelStyle, styleName);

    Label label = new Label(Localized.getConstants().ecSearchBy());
    EcStyles.add(label, styleName, "label");
    searchBy.add(label);

    searchBy.add(createCommandWidget(EcConstants.SVC_SEARCH_BY_ITEM_CODE,
        Localized.getConstants().ecSearchByItemCode(), Type.LABEL));
    searchBy.add(createCommandWidget(EcConstants.SVC_SEARCH_BY_OE_NUMBER,
        Localized.getConstants().ecSearchByOeNumber(), Type.LABEL));
    searchBy.add(createCommandWidget(EcConstants.SVC_SEARCH_BY_CAR,
        Localized.getConstants().ecSearchByCar(), Type.LABEL));
    searchBy.add(createCommandWidget(EcConstants.SVC_SEARCH_BY_BRAND,
        Localized.getConstants().ecSearchByBrand(), Type.LABEL));

    searchBy.add(createCommandWidget(EcConstants.SVC_GENERAL_ITEMS,
        Localized.getConstants().ecSearchByItemGroup(), Type.LABEL));
    searchBy.add(createCommandWidget(EcConstants.SVC_BIKE_ITEMS,
        Localized.getConstants().ecBikeItemsShort(), Type.LABEL));

    container.add(searchBy);

    Horizontal carts = new Horizontal();
    EcStyles.add(carts, "carts");

    Image image = new Image(EcUtils.imageUrl("cart.png"));
    EcStyles.add(image, "cartPicture");

    carts.add(image);
    carts.add(EcKeeper.getCartlist());

    container.add(carts);
  }

  private static Widget createCommandWidget(String service, String html, Type type) {
    EcCommandWidget commandWidget = new EcCommandWidget(service, html, type);
    return commandWidget.getWidget();
  }

  public EcScreen() {
    super();
  }

  @Override
  public boolean activateDomainEntry(Domain domain, Long key) {
    return false;
  }

  @Override
  public void activateWidget(IdentifiableWidget widget) {
  }

  @Override
  public void addDomainEntry(Domain domain, IdentifiableWidget widget, Long key, String caption) {
  }

  @Override
  public void closeWidget(IdentifiableWidget widget) {
    if (widget != null) {
      if (UiHelper.isModal(widget.asWidget())) {
        UiHelper.closeDialog(widget.asWidget());
      } else if (Objects.equal(widget, getActiveWidget())) {
        getScreenPanel().remove(widget);
      }
    }
  }

  @Override
  public boolean containsDomainEntry(Domain domain, Long key) {
    return false;
  }

  @Override
  public int getActivePanelHeight() {
    return getScreenPanel().getCenterHeight();
  }

  @Override
  public int getActivePanelWidth() {
    return getScreenPanel().getCenterWidth();
  }

  @Override
  public IdentifiableWidget getActiveWidget() {
    return getScreenPanel().getCenter();
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> info = new ArrayList<>();

    info.add(new ExtendedProperty("Center Width", BeeUtils.toString(getActivePanelWidth())));
    info.add(new ExtendedProperty("Center Height", BeeUtils.toString(getActivePanelHeight())));
    
    return info;
  }

  @Override
  public List<IdentifiableWidget> getOpenWidgets() {
    List<IdentifiableWidget> result = Lists.newArrayList();
    if (getActiveWidget() != null) {
      result.add(getActiveWidget());
    }
    return result;
  }

  @Override
  public UserInterface getUserInterface() {
    return UserInterface.E_COMMERCE;
  }

  @Override
  public void onLoad() {
    if (EcKeeper.showGlobalSearch()) {
      EcKeeper.getSearchBox().setFocus(true);
    }

    EcKeeper.showPromo(true);
    EcKeeper.restoreShoppingCarts();
  }

  @Override
  public void onWidgetChange(IdentifiableWidget widget) {
  }

  @Override
  public boolean removeDomainEntry(Domain domain, Long key) {
    return false;
  }

  @Override
  public void showInNewPlace(IdentifiableWidget widget) {
    updateActivePanel(widget);
  }
  
  @Override
  public void updateActivePanel(IdentifiableWidget widget) {
    getScreenPanel().updateCenter(widget);
  }

  @Override
  public void updateMenu(IdentifiableWidget widget) {
  }

  @Override
  protected String getScreenStyle() {
    return EcStyles.name("Screen");
  }

  @Override
  protected void hideProgressPanel() {
  }

  @Override
  protected IdentifiableWidget initCenter() {
    return new CustomDiv();
  }

  @Override
  protected Pair<? extends IdentifiableWidget, Integer> initEast() {
    return Pair.of(ClientLogManager.getLogPanel(), 0);
  }

  @Override
  protected Pair<? extends IdentifiableWidget, Integer> initNorth() {
    Flow panel = new Flow();
    EcStyles.add(panel, "NorthContainer");

    Widget logo = createLogo(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        EcKeeper.showPromo(false);
      }
    });

    if (logo != null) {
      EcStyles.add(logo, "Logo");
      panel.add(logo);
    }

    if (EcKeeper.showGlobalSearch()) {
      panel.add(createGlobalSearch());
    }

    createCommands(panel);

    Widget userContainer = createUserContainer();
    EcStyles.add(userContainer, "UserContainer");

    panel.add(userContainer);

    Notification nw = new Notification();
    EcStyles.add(nw, "Notifications");
    setNotification(nw);

    panel.add(nw);

    return Pair.of(panel, getNorthHeight(100));
  }

  @Override
  protected Pair<? extends IdentifiableWidget, Integer> initSouth() {
    Flow panel = new Flow(EcStyles.name("ProgressPanel"));
    panel.add(createCopyright("bee-ec-"));
    setProgressPanel(panel);

    return Pair.of(panel, 18);
  }

  @Override
  protected Pair<? extends IdentifiableWidget, Integer> initWest() {
    Shell shell = new Shell(EcStyles.name("shell"));
    shell.restore();

    Simple wrapper = new Simple(shell);
    return Pair.of(wrapper, 0);
  }

  @Override
  protected void onUserSignatureClick() {
    PasswordService.change();
  }

  @Override
  protected void showProgressPanel() {
  }

  private Widget createGlobalSearch() {
    String styleName = "GlobalSearch";

    final InputText input = EcKeeper.getSearchBox();

    DomUtils.setSearch(input);
    DomUtils.setPlaceholder(input, Localized.getConstants().ecGlobalSearchPlaceholder());
    EcStyles.add(input, styleName, "input");

    AutocompleteProvider.enableAutocomplete(input, EcConstants.NAME_PREFIX + styleName);

    input.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        int keyCode = event.getNativeKeyCode();

        switch (keyCode) {
          case KeyCodes.KEY_ENTER:
            String query = BeeUtils.trim(input.getValue());
            if (!BeeUtils.isEmpty(query)) {
              EcKeeper.doGlobalSearch(query, input);
            }
            break;

          case KeyCodes.KEY_LEFT:
          case KeyCodes.KEY_RIGHT:
            if (input.isEmpty()) {
              Direction dir = (keyCode == KeyCodes.KEY_LEFT) ? Direction.WEST : Direction.EAST;

              int oldSize = getScreenPanel().getDirectionSize(dir);
              int newSize = (oldSize > 0) ? 0 : getActivePanelWidth() / 5;

              getScreenPanel().setDirectionSize(dir, newSize, true);
            }
            break;
        }
      }
    });

    Image submit = new Image(EcUtils.imageUrl("search_button.png"));
    EcStyles.add(submit, styleName, "submit");

    submit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String query = BeeUtils.trim(input.getValue());
        if (!BeeUtils.isEmpty(query)) {
          EcKeeper.doGlobalSearch(query, input);
        }
      }
    });

    Horizontal container = new Horizontal();
    EcStyles.add(container, styleName, "container");

    container.add(input);
    container.add(submit);

    return container;
  }
}
