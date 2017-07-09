package com.butent.bee.client.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.animation.HasAnimatableActivity;
import com.butent.bee.client.animation.HasHoverAnimation;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Theme;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.client.widget.AnimatableLabel;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Captions;
import com.butent.bee.shared.ui.HandlesActions;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
  private static final String STYLE_ROW_ID = STYLE_PREFIX + "row-id";
  private static final String STYLE_ROW_MESSAGE = STYLE_PREFIX + "row-message";
  private static final String STYLE_READ_ONLY = STYLE_PREFIX + "read-only";

  private static final String STYLE_COMMAND_PANEL = STYLE_PREFIX + "commandPanel";

  private static final String STYLE_CONTROL = STYLE_PREFIX + "control";
  private static final String STYLE_CONTROL_HIDDEN = STYLE_CONTROL + "-hidden";
  private static final String STYLE_CONTROL_DISABLED = STYLE_CONTROL + "-disabled";

  private static final String STYLE_CREATE_NEW = BeeConst.CSS_CLASS_PREFIX + "CreateNew";
  private static final String STYLE_SAVE_LARGE = BeeConst.CSS_CLASS_PREFIX + "SaveLarge";

  private static final int MESSAGE_INDEX_ROW_ID = 0;
  private static final int MESSAGE_INDEX_READ_ONLY = 1;
  private static final int MESSAGE_INDEX_ROW_MESSAGE = 2;

  private static boolean hasAction(Action action, boolean def,
      Set<Action> enabledActions, Set<Action> disabledActions) {
    if (def) {
      return !BeeUtils.contains(disabledActions, action);
    } else {
      return BeeUtils.contains(enabledActions, action);
    }
  }

  private Presenter viewPresenter;
  private HandlesActions actionHandler;

  private final Label captionWidget = new Label();

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
    if (widget instanceof HasAnimatableActivity) {
      int duration = BeeUtils.round(Previewer.getActionSensitivityMillis()
          * ((HasAnimatableActivity) widget).getSensitivityRatio());
      addCommandItem((HasAnimatableActivity) widget, duration);

    } else if (widget != null) {
      addCommand(widget.asWidget());
    }
  }

  @Override
  public void addCommandItem(HasAnimatableActivity widget, int duration) {
    if (widget != null) {
      if (duration > 0) {
        widget.enableAnimation(duration);
      }

      addCommand(widget.asWidget());
    }
  }

  private void addCommand(Widget command) {
    HasHoverAnimation.init(command);
    getCommandPanel().add(command);
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
        AnimatableLabel control = new AnimatableLabel(Localized.dictionary().createNew());
        control.addStyleName(STYLE_CREATE_NEW);

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
      if (Theme.hasActionSaveLarge()) {
        AnimatableLabel control = new AnimatableLabel(Localized.dictionary().actionSave());
        control.addStyleName(STYLE_SAVE_LARGE);

        initControl(control, Action.SAVE, hiddenActions);
        add(control);

      } else {
        add(createFa(Action.SAVE, hiddenActions));
      }
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

    if (hasAction(Action.MINIMIZE, false, enabledActions, disabledActions)) {
      add(createFa(Action.MINIMIZE, hiddenActions));
    }
    if (hasAction(Action.MAXIMIZE, false, enabledActions, disabledActions)) {
      add(createFa(Action.MAXIMIZE, hiddenActions));
    }

    if (hasAction(Action.CLOSE, UiOption.isClosable(options), enabledActions, disabledActions)) {
      add(createFa(Action.CLOSE, hiddenActions));
    }
  }

  @Override
  public boolean enableCommandByStyleName(String styleName, boolean enable) {
    Widget command = getCommandByStyleName(styleName);

    if (command instanceof HasEnabled) {
      ((HasEnabled) command).setEnabled(enable);
      return true;

    } else {
      return false;
    }
  }

  @Override
  public Widget getActionWidget(Action action) {
    if (action == null) {
      return null;
    }

    String id = getActionControls().get(action);

    if (BeeUtils.isEmpty(id)) {
      return null;
    } else {
      return DomUtils.getChildQuietly(this, id);
    }
  }

  @Override
  public String getCaption() {
    return captionWidget.getHtml();
  }

  @Override
  public Widget getCommandByStyleName(String styleName) {
    if (BeeUtils.isEmpty(styleName)) {
      return null;
    } else {
      return UiHelper.getChildByStyleName(getCommandPanel(), styleName);
    }
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
  public String getRowMessage() {
    Element messageElement = Selectors.getElementByClassName(this, STYLE_ROW_MESSAGE);
    return (messageElement == null) ? null : messageElement.getInnerHTML();
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
  public boolean insertControl(Widget w, int beforeIndex) {
    if (w != null && beforeIndex >= 0 && beforeIndex <= getWidgetCount()) {
      w.addStyleName(STYLE_CONTROL);
      insert(w, beforeIndex);

      return true;

    } else {
      return false;
    }
  }

  @Override
  public boolean isActionEnabled(Action action) {
    if (action == null || !isEnabled() && action.isDisablable()) {
      return false;
    }

    Action a = (action == Action.CANCEL) ? Action.CLOSE : action;
    Widget child = getActionWidget(a);

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
      return BeeUtils.isEmpty(id) || !actionControls.containsValue(id);
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
  public boolean removeCommandByStyleName(String styleName) {
    Widget command = getCommandByStyleName(styleName);

    if (command == null) {
      return false;
    } else {
      return getCommandPanel().remove(command);
    }
  }

  @Override
  public void setActionHandler(HandlesActions actionHandler) {
    this.actionHandler = actionHandler;
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

    for (Map.Entry<Action, String> entry : getActionControls().entrySet()) {
      if (entry.getKey().isDisablable()) {
        Widget widget = DomUtils.getChildQuietly(this, entry.getValue());

        if (widget instanceof HasEnabled) {
          ((HasEnabled) widget).setEnabled(enabled);
        }
        if (widget != null) {
          widget.setStyleName(STYLE_CONTROL_DISABLED, !enabled);
        }
      }
    }
  }

  @Override
  public void setHeight(int height) {
    this.height = height;
  }

  @Override
  public void setMessage(int index, String message, String styleName) {
    if (index < 0) {
      logger.warning("invalid message index", index);
      return;
    }

    List<Element> elements = Selectors.getElementsByClassName(getElement(), STYLE_MESSAGE);

    if (BeeUtils.isIndex(elements, index)) {
      Element target = elements.get(index);
      target.setInnerHTML(BeeUtils.trim(message));

      if (!BeeUtils.isEmpty(styleName) && !target.hasClassName(styleName)) {
        target.addClassName(styleName);
      }

    } else if (!BeeUtils.isEmpty(message)) {
      int afterIndex;

      if (BeeUtils.isEmpty(elements)) {
        afterIndex = getWidgetIndex(captionWidget);
      } else {
        afterIndex = DomUtils.getElementIndex(BeeUtils.getLast(elements));
      }

      if (afterIndex >= 0 && afterIndex < getWidgetCount()) {
        int beforeIndex = afterIndex + 1;

        for (int i = BeeUtils.size(elements); i < index; i++) {
          Label emptyWidget = new Label();
          emptyWidget.addStyleName(STYLE_MESSAGE);
          emptyWidget.addStyleName(STYLE_MESSAGE + BeeConst.STRING_MINUS + i);

          insert(emptyWidget, beforeIndex);
          beforeIndex++;
        }

        Label messageWidget = new Label(message.trim());
        messageWidget.addStyleName(STYLE_MESSAGE);
        messageWidget.addStyleName(STYLE_MESSAGE + BeeConst.STRING_MINUS + index);

        if (!BeeUtils.isEmpty(styleName)) {
          messageWidget.addStyleName(styleName);
        }

        insert(messageWidget, beforeIndex);

      } else {
        logger.warning("cannot insert message with index", index);
      }
    }
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
    this.viewPresenter = viewPresenter;
  }

  @Override
  public void showAction(Action action, boolean visible) {
    Widget widget = getActionWidget(action);

    if (widget == null) {
      if (visible) {
        logger.warning("showAction", action, "widget not found");
      }

    } else {
      widget.setStyleName(STYLE_CONTROL_HIDDEN, !visible);
    }
  }

  @Override
  public void showReadOnly(boolean readOnly) {
    String message = readOnly ? Localized.dictionary().readOnly() : null;
    setMessage(MESSAGE_INDEX_READ_ONLY, message, STYLE_READ_ONLY);
  }

  @Override
  public void showRowId(IsRow row) {
    String message = DataUtils.hasId(row) ? BeeUtils.bracket(row.getId()) : null;
    setMessage(MESSAGE_INDEX_ROW_ID, message, STYLE_ROW_ID);
  }

  @Override
  public void showRowMessage(Evaluator evaluator, IsRow row) {
    String message;

    if (evaluator != null && row != null) {
      evaluator.update(row);
      message = evaluator.evaluate();
    } else {
      message = null;
    }

    setMessage(MESSAGE_INDEX_ROW_MESSAGE, message, STYLE_ROW_MESSAGE);
  }

  @Override
  public boolean startCommandByStyleName(String styleName, int duration) {
    if (duration <= 0) {
      return false;
    }

    Widget command = getCommandByStyleName(styleName);
    if (command instanceof HasAnimatableActivity) {
      ((HasAnimatableActivity) command).enableAnimation(duration);
      ((HasAnimatableActivity) command).maybeAnimate();

      return true;

    } else {
      return false;
    }
  }

  @Override
  public boolean stopAction(Action action) {
    Widget child = getActionWidget(action);

    if (child instanceof HasAnimatableActivity) {
      ((HasAnimatableActivity) child).stop();
      return true;

    } else {
      return false;
    }
  }

  @Override
  public boolean stopCommandByStyleName(String styleName, boolean disableAnimation) {
    Widget command = getCommandByStyleName(styleName);

    if (command instanceof HasAnimatableActivity) {
      if (disableAnimation) {
        ((HasAnimatableActivity) command).disableAnimation();
      } else {
        ((HasAnimatableActivity) command).stop();
      }

      return true;

    } else {
      return false;
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

  private HandlesActions getActionHandler() {
    return actionHandler;
  }

  private Horizontal getCommandPanel() {
    return commandPanel;
  }

  private void initControl(Label control, final Action action, Set<Action> hiddenActions) {
    control.addStyleName(STYLE_CONTROL);
    control.addStyleName(action.getStyleName());

    UiHelper.initActionWidget(action, control);

    if (hiddenActions != null && hiddenActions.contains(action)) {
      control.getElement().addClassName(STYLE_CONTROL_HIDDEN);
    }

    control.addClickHandler(event -> {
      if (getViewPresenter() != null) {
        getViewPresenter().handleAction(action);

      } else if (getActionHandler() != null) {
        getActionHandler().handleAction(action);
      }
    });

    getActionControls().put(action, control.getId());
  }
}
