package com.butent.bee.client.screen;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Settings;
import com.butent.bee.client.cli.Shell;
import com.butent.bee.client.event.logical.ActiveWidgetChangeEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Stack;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;

class CentralScrutinizer extends Stack implements CloseHandler<IdentifiableWidget>,
    ActiveWidgetChangeEvent.Handler {

  private static final class Appliance extends Flow implements
      HasCloseHandlers<IdentifiableWidget>, HasDomain {

    private static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "Appliance";

    private final Domain domain;
    private final Long key;

    private Appliance(Domain domain, Long key, String caption) {
      super();
      this.domain = domain;
      this.key = key;

      addStyleName(STYLE_NAME);
      addStyleName(STYLE_NAME + BeeUtils.proper(domain.name()));

      if (domain.getImageResource() != null) {
        Image icon = new Image(domain.getImageResource());
        icon.addStyleName(STYLE_NAME + "-icon");
        add(icon);
      }

      Label label = new Label(BeeUtils.notEmpty(caption, domain.getCaption()));
      label.addStyleName(STYLE_NAME + "-label");
      add(label);

      if (domain.isRemovable()) {
        CustomDiv close = new CustomDiv(STYLE_NAME + "-close");
        close.setText(String.valueOf(BeeConst.CHAR_TIMES));

        close.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            event.stopPropagation();
            CloseEvent.fire(Appliance.this, Appliance.this);
          }
        });

        add(close);
      }
    }

    @Override
    public HandlerRegistration addCloseHandler(CloseHandler<IdentifiableWidget> handler) {
      return addHandler(handler, CloseEvent.getType());
    }

    @Override
    public Domain getDomain() {
      return domain;
    }

    @Override
    public String getIdPrefix() {
      return "muffin";
    }

    private Long getKey() {
      return key;
    }

    private boolean is(Domain otherDomain, Long otherKey) {
      return getDomain().equals(otherDomain) && Objects.equals(getKey(), otherKey);
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(CentralScrutinizer.class);

  private static final int DEFAULT_HEADER_HEIGHT = 25;

  private static int getHeaderHeight() {
    int height = BeeKeeper.getUser().getApplianceHeaderHeight();
    if (height <= 0) {
      height = Settings.getApplianceHeaderHeight();
    }

    return (height > 0) ? height : DEFAULT_HEADER_HEIGHT;
  }

  CentralScrutinizer() {
    super();
    addStyleName(BeeConst.CSS_CLASS_PREFIX + "CentralScrutinizer");
  }

  public Flow getDomainHeader(Domain domain, Long key) {
    int index = find(domain, key);
    if (index >= 0) {
      return getAppliance(index);
    } else {
      return null;
    }
  }

  @Override
  public String getIdPrefix() {
    return "scrutinizer";
  }

  @Override
  public void onActiveWidgetChange(ActiveWidgetChangeEvent event) {
    if (event.isActive() && event.getDomain() == null && isOpen()) {
      Appliance appliance = getAppliance(getSelectedIndex());

      if (appliance != null && appliance.getDomain().isClosable()) {
        close();
      }
    }
  }

  @Override
  public void onClose(CloseEvent<IdentifiableWidget> event) {
    if (event.getTarget() instanceof Appliance) {
      Appliance appliance = (Appliance) event.getTarget();
      remove(appliance.getDomain(), appliance.getKey());
    }
  }

  @Override
  public boolean remove(Widget child) {
    boolean removed = super.remove(child);
    if (removed && child instanceof HandlesStateChange) {
      ((HandlesStateChange) child).onStateChange(State.REMOVED);
    }
    return removed;
  }

  @Override
  protected State onHeaderClick(Widget child) {
    State state = super.onHeaderClick(child);

    if (State.OPEN.equals(state) && child instanceof HandlesStateChange) {
      ((HandlesStateChange) child).onStateChange(State.ACTIVATED);
    }
    return state;
  }

  boolean activate(Domain domain, Long key) {
    int index = find(domain, key);
    if (index >= 0) {
      showWidget(index);
      return true;
    } else {
      return false;
    }
  }

  void activateShell() {
    if (!activate(Domain.ADMIN, null)) {
      addShell();
      activate(Domain.ADMIN, null);
    }
  }

  void add(Domain domain, IdentifiableWidget widget) {
    add(domain, widget, null, null);
  }

  void add(Domain domain, IdentifiableWidget widget, Long key, String caption) {
    Assert.notNull(domain);
    Assert.notNull(widget);

    if (find(domain, key) >= 0) {
      logger.warning("attempt to add existing appliance failed:", domain, key);
      return;
    }

    int before = getStackSize();
    for (int i = 0; i < getStackSize(); i++) {
      Appliance appliance = getAppliance(i);
      if (appliance != null && appliance.getDomain().ordinal() > domain.ordinal()) {
        before = i;
        break;
      }
    }

    Appliance appliance = new Appliance(domain, key, caption);
    appliance.addCloseHandler(this);

    insert(widget.asWidget(), appliance, getHeaderHeight(), before);
  }

  boolean contains(Domain domain, Long key) {
    return find(domain, key) >= 0;
  }

  boolean maybeUpdateHeaders() {
    return updateHeaderSize(getHeaderHeight());
  }

  boolean remove(Domain domain, Long key) {
    int index = find(domain, key);
    if (index >= 0) {
      return remove(index);
    } else {
      return false;
    }
  }

  void start() {
    if (BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.NEWS)) {
      add(Domain.NEWS, Global.getNewsAggregator().getNewsPanel());
    }

    if (Endpoint.isEnabled()) {
      if (BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.ONLINE)) {
        add(Domain.ONLINE, Global.getUsers().getOnlinePanel());
      }
      if (BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.ROOMS)) {
        add(Domain.ROOMS, Global.getRooms().getRoomsPanel());
      }
    }

    if (BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.ADMIN)) {
      addShell();
    }
  }

  private void addShell() {
    Shell shell = new Shell(BeeConst.CSS_CLASS_PREFIX + "Shell");
    shell.restore();

    Simple wrapper = new Simple(shell);
    add(Domain.ADMIN, wrapper);
  }

  private int find(Domain domain, Long key) {
    for (int i = 0; i < getStackSize(); i++) {
      Appliance appliance = getAppliance(i);
      if (appliance != null && appliance.is(domain, key)) {
        return i;
      }
    }
    return BeeConst.UNDEF;
  }

  private Appliance getAppliance(int index) {
    Widget headerWidget = getHeaderWidget(index);
    if (headerWidget instanceof Appliance) {
      return (Appliance) headerWidget;
    } else {
      return null;
    }
  }
}
