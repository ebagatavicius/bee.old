package com.butent.bee.client.view;

import com.google.common.collect.Maps;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Settings;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Captions;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Implements styling and user command capture for data headers.
 */

public class HeaderImpl extends Flow implements HeaderView {

  private final class ActionListener implements ClickHandler {
    private final Action action;
    private long lastTime;

    private ActionListener(Action action) {
      super();
      this.action = action;
    }

    @Override
    public void onClick(ClickEvent event) {
      if (getViewPresenter() != null) {
        long now = System.currentTimeMillis();
        long last = getLastTime();
        setLastTime(now);

        if (now - last >= HeaderImpl.ACTION_SENSITIVITY_MILLIS) {
          getViewPresenter().handleAction(action);
        }
      }
    }

    private long getLastTime() {
      return lastTime;
    }

    private void setLastTime(long lastTime) {
      this.lastTime = lastTime;
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(HeaderImpl.class);

  private static final int HEIGHT = 30;

  private static final String STYLE_PREFIX = StyleUtils.CLASS_NAME_PREFIX + "Header-";

  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";

  private static final String STYLE_CAPTION = STYLE_PREFIX + "caption";
  private static final String STYLE_MESSAGE = STYLE_PREFIX + "message";

  private static final String STYLE_COMMAND_PANEL = STYLE_PREFIX + "commandPanel";

  private static final String STYLE_CONTROL = STYLE_PREFIX + "control";
  private static final String STYLE_CONTROL_HIDDEN = STYLE_CONTROL + "-hidden";

  private static final String STYLE_DISABLED = STYLE_PREFIX + "disabled";

  private static final int ACTION_SENSITIVITY_MILLIS =
      BeeUtils.positive(Settings.getActionSensitivityMillis(), 300);

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

  private final Map<Action, String> actionControls = Maps.newHashMap();

  private final Horizontal commandPanel = new Horizontal();

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
  public void clearCommandPanel() {
    getCommandPanel().clear();
  }

  @Override
  public void create(String caption, boolean hasData, boolean readOnly, String viewName,
      Collection<UiOption> options, Set<Action> enabledActions, Set<Action> disabledActions,
      Set<Action> hiddenActions) {

    addStyleName(STYLE_CONTAINER);

    captionWidget.addStyleName(STYLE_CAPTION);
    if (Captions.isCaption(caption)) {
      setCaption(caption);
    }
    add(captionWidget);

    messageWidget.addStyleName(STYLE_MESSAGE);
    add(messageWidget);

    commandPanel.addStyleName(STYLE_COMMAND_PANEL);
    add(commandPanel);

    if (hasAction(Action.REFRESH, hasData, enabledActions, disabledActions)) {
      add(createImage(Global.getImages().silverReload(), Action.REFRESH, hiddenActions));
    }

    if (hasAction(Action.FILTER, false, enabledActions, disabledActions)) {
      add(createImage(Global.getImages().silverFilter(), Action.FILTER, hiddenActions));
    }
    if (hasAction(Action.REMOVE_FILTER, false, enabledActions, disabledActions)) {
      add(createImage(Global.getImages().closeSmallRed(), Action.REMOVE_FILTER, hiddenActions));
    }

    boolean canAdd = hasData && !readOnly && BeeKeeper.getUser().canCreateData(viewName);
    if (hasAction(Action.ADD, canAdd, enabledActions, disabledActions)) {
      add(createImage(Global.getImages().silverAdd(), Action.ADD, hiddenActions));
    }

    if (hasAction(Action.COPY, false, enabledActions, disabledActions)) {
      add(createFa(FontAwesome.COPY, Action.COPY, hiddenActions));
    }

    boolean canDelete = hasData && !readOnly && BeeKeeper.getUser().canDeleteData(viewName);
    if (hasAction(Action.DELETE, canDelete, enabledActions, disabledActions)) {
      add(createImage(Global.getImages().silverDelete(), Action.DELETE, hiddenActions));
    }

    if (hasAction(Action.BOOKMARK, false, enabledActions, disabledActions)) {
      add(createImage(Global.getImages().silverBookmarkAdd(), Action.BOOKMARK, hiddenActions));
    }

    if (hasAction(Action.EDIT, false, enabledActions, disabledActions)) {
      add(createImage(Global.getImages().silverEdit(), Action.EDIT, hiddenActions));
    }
    if (hasAction(Action.SAVE, false, enabledActions, disabledActions)) {
      add(createImage(Global.getImages().silverSave(), Action.SAVE, hiddenActions));
    }

    if (hasAction(Action.EXPORT, false, enabledActions, disabledActions)) {
      add(createImage(Global.getImages().excel(), Action.EXPORT, hiddenActions));
    }

    if (hasAction(Action.CONFIGURE, false, enabledActions, disabledActions)) {
      add(createImage(Global.getImages().silverConfigure(), Action.CONFIGURE, hiddenActions));
    }

    if (hasAction(Action.AUDIT, false, enabledActions, disabledActions)) {
      add(createFa(FontAwesome.HISTORY, Action.AUDIT, hiddenActions));
    }

    if (hasAction(Action.PRINT, false, enabledActions, disabledActions)) {
      add(createImage(Global.getImages().silverPrint(), Action.PRINT, hiddenActions));
    }

    if (hasAction(Action.MENU, false, enabledActions, disabledActions)) {
      add(createFa(FontAwesome.NAVICON, Action.MENU, hiddenActions));
    }
    
    if (hasAction(Action.CLOSE, UiOption.isWindow(options), enabledActions, disabledActions)) {
      add(createImage(Global.getImages().silverClose(), Action.CLOSE, hiddenActions));
    }
  }

  @Override
  public String getCaption() {
    return captionWidget.getHtml();
  }

  @Override
  public int getHeight() {
    return HEIGHT;
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
    String id = getActionControls().get(action);
    if (BeeUtils.isEmpty(id)) {
      return false;
    }

    Widget child = DomUtils.getChildQuietly(this, id);
    if (child instanceof HasEnabled) {
      return child.isVisible() && ((HasEnabled) child).isEnabled();
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

  private Widget createFa(FontAwesome fa, Action action, Set<Action> hiddenActions) {
    FaLabel control = new FaLabel(fa);
    initControl(control, action, hiddenActions);
    return control;
  }

  private Widget createImage(ImageResource image, Action action, Set<Action> hiddenActions) {
    Image control = new Image(image);
    initControl(control, action, hiddenActions);
    return control;
  }

  private Map<Action, String> getActionControls() {
    return actionControls;
  }

  private Horizontal getCommandPanel() {
    return commandPanel;
  }

  private void initControl(IdentifiableWidget control, Action action, Set<Action> hiddenActions) {
    Binder.addClickHandler(control.asWidget(), new ActionListener(action));

    control.addStyleName(STYLE_CONTROL);
    control.addStyleName(action.getStyleName());

    control.getElement().setTitle(action.getCaption());

    if (hiddenActions != null && hiddenActions.contains(action)) {
      control.getElement().addClassName(STYLE_CONTROL_HIDDEN);
    }

    getActionControls().put(action, control.getId());
  }
}
