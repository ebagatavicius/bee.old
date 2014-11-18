package com.butent.bee.client.screen;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

import com.butent.bee.client.Bee;
import com.butent.bee.client.Global;
import com.butent.bee.client.cli.CliWidget;
import com.butent.bee.client.cli.CliWorker;
import com.butent.bee.client.dialog.Notification;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.Complex;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.LayoutPanel;
import com.butent.bee.client.logging.ClientLogManager;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.Command;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handles mobile phone size screen implementation.
 */

public class Mobile extends ScreenImpl {

  public Mobile() {
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
  public void closeAll() {
    IdentifiableWidget widget = getActiveWidget();
    if (widget != null) {
      getScreenPanel().remove(widget);
    }
  }

  @Override
  public void closeWidget(IdentifiableWidget widget) {
    Assert.notNull(widget, "closeWidget: widget is null");

    if (UiHelper.isModal(widget.asWidget())) {
      UiHelper.closeDialog(widget.asWidget());
    } else if (Objects.equals(widget, getActiveWidget())) {
      getScreenPanel().remove(widget);
    } else {
      notifyWarning("closeWidget: widget not found");
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
    List<IdentifiableWidget> result = new ArrayList<>();
    if (getActiveWidget() != null) {
      result.add(getActiveWidget());
    }
    return result;
  }

  @Override
  public UserInterface getUserInterface() {
    return UserInterface.MOBILE;
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

  protected int addLogToggle(LayoutPanel panel) {
    final CheckBox toggle = new CheckBox("Log");

    toggle.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        ClientLogManager.setPanelVisible(event.getValue());
      }
    });

    panel.addRightWidthTop(toggle, 3, 48, 2);
    return 50;
  }

  @Override
  protected void createUi() {
    super.createUi();
    ClientLogManager.setPanelVisible(false);
  }

  @Override
  protected IdentifiableWidget initCenter() {
    return new CustomDiv();
  }

  @Override
  protected Pair<? extends IdentifiableWidget, Integer> initNorth() {
    Complex panel = new Complex();
    panel.addStyleName(BeeConst.CSS_CLASS_PREFIX + "NorthContainer");

    panel.addLeftTop(Global.getSearchWidget(), 40, 2);

    Flow menuContainer = new Flow();
    menuContainer.addStyleName(BeeConst.CSS_CLASS_PREFIX + "MainMenu");
    panel.addLeftTop(menuContainer, 10, 30);
    setMenuPanel(menuContainer);

    setNotification(new Notification());
    panel.addRightTop(getNotification(), 1, 1);

    return Pair.of(panel, 60);
  }

  @Override
  protected Pair<? extends IdentifiableWidget, Integer> initSouth() {
    LayoutPanel p = new LayoutPanel();

    int width = DomUtils.getClientWidth();
    int pct = BeeUtils.toInt(BeeUtils.rescale(width, 320, 800, 28, 50));

    final CliWidget cli = new CliWidget();
    p.addLeftWidthTop(cli, 3, CssUnit.PX, pct, CssUnit.PCT, 3, CssUnit.PX);

    Image play = new Image(Global.getImages().play(), new Command() {
      @Override
      public void execute() {
        CliWorker.execute(cli.getValue(), false);
      }
    });
    p.addLeftTop(play, pct + 4, CssUnit.PCT, 2, CssUnit.PX);

    Horizontal hor = new Horizontal();

    Button auth = new Button("Exit", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Bee.exit();
      }
    });

    auth.setId("auth-button");
    hor.add(auth);

    Label user = new Label();
    StyleUtils.setHorizontalPadding(user, 5);
    hor.add(user);
    setUserSignature(user);

    int right = addLogToggle(p);
    p.addLeftRightTop(hor, pct + 12, CssUnit.PCT, right, CssUnit.PX, 1, CssUnit.PX);

    return Pair.of(p, 30);
  }

  @Override
  protected Pair<? extends IdentifiableWidget, Integer> initWest() {
    return null;
  }
}
