package com.butent.bee.client.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Theme;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Captions;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implements styling and user command capture for data headers.
 */

public class HeaderImpl extends Flow implements HeaderView {

  private static final BeeLogger logger = LogUtils.getLogger(HeaderImpl.class);

  private static final int DEFAULT_HEIGHT = 30;

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "Header-";

  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";

  private static final String STYLE_CAPTION = STYLE_PREFIX + "caption";
  private static final String STYLE_MESSAGE = STYLE_PREFIX + "message";

  private static final String STYLE_COMMAND_PANEL = STYLE_PREFIX + "commandPanel";

  private static final String STYLE_CONTROL = STYLE_PREFIX + "control";
  private static final String STYLE_CONTROL_HIDDEN = STYLE_CONTROL + "-hidden";

  private static final String STYLE_DISABLED = STYLE_PREFIX + "disabled";

  private static boolean hasAction(Action action, boolean def,
      Set<Action> enabledActions, Set<Action> disabledActions) {
    if (def) {
      return !BeeUtils.contains(disabledActions, action);
    } else {
      return BeeUtils.contains(enabledActions, action);
    }
  }

  private Presenter viewPresenter;

  private final Label captionWidget = new Label();
  private final Label messageWidget = new Label();

  private boolean enabled = true;

  private final Map<Action, String> actionControls = new HashMap<>();

  private final Horizontal commandPanel = new Horizontal();

  private int height = DEFAULT_HEIGHT;

  public HeaderImpl() {
    super();
  }

  public HeaderImpl(Widget widget) {
    this();
    if (widget != null) {
      add(widget);
    }
  }

  @Override
  public void addCaptionStyle(String style) {
    Assert.notEmpty(style);
    captionWidget.addStyleName(style);
  }

  @Override
  public void addCommandItem(IdentifiableWidget widget) {
    getCommandPanel().add(widget);
  }

  @Override
  public HandlerRegistration addReadyHandler(ReadyEvent.Handler handler) {
    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public void clearCommandPanel() {
    getCommandPanel().clear();
  }

  @Override
  public void create(String caption, boolean hasData, boolean readOnly, String viewName,
      Collection<UiOption> options, Set<Action> enabledActions, Set<Action> disabledActions,
      Set<Action> hiddenActions) {

    addStyleName(STYLE_CONTAINER);

    int h = UiOption.isChildOrEmbedded(options)
        ? Theme.getChildViewHeaderHeight() : Theme.getViewHeaderHeight();
    if (h > 0) {
      setHeight(h);
    }
    StyleUtils.setHeight(this, getHeight());

    captionWidget.addStyleName(STYLE_CAPTION);
    StyleUtils.setFontSize(captionWidget, BeeUtils.resize(getHeight(), 30, 45, 16, 24));
    if (Captions.isCaption(caption)) {
      setCaption(caption);
    }
    add(captionWidget);

    messageWidget.addStyleName(STYLE_MESSAGE);
    add(messageWidget);

    commandPanel.addStyleName(STYLE_COMMAND_PANEL);
    add(commandPanel);

    boolean canAdd = hasData && !readOnly && BeeKeeper.getUser().canCreateData(viewName);
    if (hasAction(Action.ADD, canAdd, enabledActions, disabledActions)) {
      boolean createNew;

      if (BeeUtils.isEmpty(options)) {
        createNew = false;
      } else if (UiOption.isChildOrEmbedded(options)) {
        createNew = Theme.hasChildActionCreateNew();
      } else if (options.contains(UiOption.GRID)) {
        createNew = Theme.hasGridActionCreateNew();
      } else {
        createNew = Theme.hasViewActionCreateNew();
      }

      if (createNew) {
        Label control = new Label("+ " + Localized.getConstants().createNew());
        control.addStyleName(BeeConst.CSS_CLASS_PREFIX + "CreateNew");

        initControl(control, Action.ADD, hiddenActions);
        add(control);

      } else {
        add(createFa(Action.ADD, hiddenActions));
      }
    }

    if (hasAction(Action.REFRESH, hasData, enabledActions, disabledActions)) {
      add(createFa(Action.REFRESH, hiddenActions));
    }

    if (hasAction(Action.FILTER, false, enabledActions, disabledActions)) {
      add(createFa(Action.FILTER, hiddenActions));
    }
    if (hasAction(Action.REMOVE_FILTER, false, enabledActions, disabledActions)) {
      add(createFa(Action.REMOVE_FILTER, hiddenActions));
    }

    if (hasAction(Action.COPY, false, enabledActions, disabledActions)) {
      add(createFa(Action.COPY, hiddenActions));
    }

    boolean canDelete = hasData && !readOnly && BeeKeeper.getUser().canDeleteData(viewName);
    if (hasAction(Action.DELETE, canDelete, enabledActions, disabledActions)) {
      add(createFa(Action.DELETE, hiddenActions));
    }

    if (hasAction(Action.BOOKMARK, false, enabledActions, disabledActions)) {
      add(createFa(Action.BOOKMARK, hiddenActions));
    }

    if (hasAction(Action.EDIT, false, enabledActions, disabledActions)) {
      add(createFa(Action.EDIT, hiddenActions));
    }
    if (hasAction(Action.SAVE, false, enabledActions, disabledActions)) {
      add(createFa(Action.SAVE, hiddenActions));
    }

    if (hasAction(Action.EXPORT, false, enabledActions, disabledActions)) {
      add(createFa(Action.EXPORT, hiddenActions));
    }

    if (hasAction(Action.CONFIGURE, false, enabledActions, disabledActions)) {
      add(createFa(Action.CONFIGURE, hiddenActions));
    }
    if (hasAction(Action.RESET_SETTINGS, false, enabledActions, disabledActions)) {
      add(createFa(Action.RESET_SETTINGS, hiddenActions));
    }

    if (hasAction(Action.AUDIT, false, enabledActions, disabledActions)) {
      add(createFa(Action.AUDIT, hiddenActions));
    }

    if (hasAction(Action.AUTO_FIT, false, enabledActions, disabledActions)) {
      add(createFa(Action.AUTO_FIT, hiddenActions));
    }

    if (hasAction(Action.PRINT, false, enabledActions, disabledActions)) {
      add(createFa(Action.PRINT, hiddenActions));
    }

    if (hasAction(Action.MENU, false, enabledActions, disabledActions)) {
      add(createFa(Action.MENU, hiddenActions));
    }

    if (hasAction(Action.CLOSE, UiOption.isClosable(options), enabledActions, disabledActions)) {
      add(createFa(Action.CLOSE, hiddenActions));
    }
  }

  @Override
  public String getCaption() {
    return captionWidget.getHtml();
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public Element getPrintElement() {
    return getElement();
  }

  @Override
  public Presenter getViewPresenter() {
    return viewPresenter;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public boolean hasAction(Action action) {
    if (action == null) {
      return false;
    } else {
      return getActionControls().containsKey(action);
    }
  }

  @Override
  public boolean hasCommands() {
    return !commandPanel.isEmpty();
  }

  @Override
  public boolean isActionEnabled(Action action) {
    if (action == null || !isEnabled()) {
      return false;
    }

    Action a = (action == Action.CANCEL) ? Action.CLOSE : action;

    String id = getActionControls().get(a);
    if (BeeUtils.isEmpty(id)) {
      return false;
    }

    Widget child = DomUtils.getChildQuietly(this, id);
    if (DomUtils.isVisible(child)) {
      if (child instanceof HasEnabled) {
        return ((HasEnabled) child).isEnabled();
      } else {
        return true;
      }

    } else {
      return false;
    }
  }

  @Override
  public boolean isActionOrCommand(Element target) {
    if (target == null) {
      return false;
    } else if (commandPanel.getElement().isOrHasChild(target)) {
      return true;
    } else {
      return !BeeUtils.isEmpty(target.getId())
          && getActionControls().values().contains(target.getId());
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    if (DomUtils.isButtonElement(source)) {
      return false;
    } else {
      String id = source.getId();
      return BeeUtils.isEmpty(id) ? true : !actionControls.containsValue(id);
    }
  }

  @Override
  public boolean reactsTo(Action action) {
    return false;
  }

  @Override
  public void removeCaptionStyle(String style) {
    Assert.notEmpty(style);
    captionWidget.removeStyleName(style);
  }

  @Override
  public void setCaption(String caption) {
    captionWidget.setHtml(BeeUtils.trim(Localized.maybeTranslate(caption)));
  }

  @Override
  public void setCaptionTitle(String title) {
    captionWidget.setTitle(title);
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled == isEnabled()) {
      return;
    }
    this.enabled = enabled;

    setStyleName(STYLE_DISABLED, !enabled);

    for (int i = 0; i < getWidgetCount(); i++) {
      Widget child = getWidget(i);
      String id = DomUtils.getId(child);

      if (BeeUtils.containsSame(getActionControls().values(), id) && child instanceof HasEnabled) {
        ((HasEnabled) child).setEnabled(enabled);
      }
    }
  }

  @Override
  public void setHeight(int height) {
    this.height = height;
  }

  @Override
  public void setMessage(String message) {
    messageWidget.setHtml(BeeUtils.trim(message));
  }

  @Override
  public void setMessageTitle(String title) {
    messageWidget.setTitle(title);
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
    this.viewPresenter = viewPresenter;
  }

  @Override
  public void showAction(Action action, boolean visible) {
    Assert.notNull(action);
    String widgetId = getActionControls().get(action);
    if (BeeUtils.isEmpty(widgetId)) {
      if (visible) {
        logger.warning("showAction:", action.name(), "widget not found");
      }
      return;
    }

    Element controlElement = DomUtils.getElement(widgetId);
    if (visible) {
      controlElement.removeClassName(STYLE_CONTROL_HIDDEN);
    } else {
      controlElement.addClassName(STYLE_CONTROL_HIDDEN);
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    ReadyEvent.fire(this);
  }

  private Widget createFa(Action action, Set<Action> hiddenActions) {
    FaLabel control = new FaLabel(action.getIcon());
    initControl(control, action, hiddenActions);
    return control;
  }

  private Map<Action, String> getActionControls() {
    return actionControls;
  }

  private Horizontal getCommandPanel() {
    return commandPanel;
  }

  private void initControl(Label control, final Action action, Set<Action> hiddenActions) {
    control.addStyleName(STYLE_CONTROL);
    control.addStyleName(action.getStyleName());

    control.setTitle(action.getCaption());

    if (hiddenActions != null && hiddenActions.contains(action)) {
      control.getElement().addClassName(STYLE_CONTROL_HIDDEN);
    }

    control.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getViewPresenter() != null) {
          getViewPresenter().handleAction(action);
        }
      }
    });

    getActionControls().put(action, control.getId());
  }
}
