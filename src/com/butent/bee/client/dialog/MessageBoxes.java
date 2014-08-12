package com.butent.bee.client.dialog;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.IndexedPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.Event;

import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.logical.CloseEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.images.star.Stars;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.WidgetInitializer;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.IsTable;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

/**
 * Implements a message box user interface component, sending text messages to the user.
 */

public class MessageBoxes {

  private static final BeeLogger logger = LogUtils.getLogger(MessageBoxes.class);

  private static final String STYLE_CHOICE_DIALOG = "bee-ChoiceDialog";
  private static final String STYLE_CHOICE_PANEL = "bee-ChoicePanel";
  private static final String STYLE_CHOICE_PROMPT = "bee-ChoicePrompt";
  private static final String STYLE_CHOICE_CONTAINER = "bee-ChoiceContainer";
  private static final String STYLE_CHOICE_GROUP = "bee-ChoiceGroup-";
  private static final String STYLE_CHOICE_DEFAULT = "bee-ChoiceDefault";
  private static final String STYLE_CHOICE_CANCEL = "bee-ChoiceCancel";

  private static final String STYLE_MESSAGE_BOX = "bee-MessageBox";
  private static final String STYLE_MESSAGE_BOX_PANEL = STYLE_MESSAGE_BOX + "-panel";
  private static final String STYLE_MESSAGE_BOX_ICON = STYLE_MESSAGE_BOX + "-icon";
  private static final String STYLE_MESSAGE_BOX_MESSAGE = STYLE_MESSAGE_BOX + "-message";
  private static final String STYLE_MESSAGE_BOX_BUTTON_GROUP = STYLE_MESSAGE_BOX + "-buttonGroup";
  private static final String STYLE_MESSAGE_BOX_BUTTON = STYLE_MESSAGE_BOX + "-button";

  private static final String STYLE_TABLE_CONTAINER = "bee-ModalTableContainer";
  private static final String STYLE_TABLE = "bee-ModalTable";

  private static final String STYLE_STAR_PICKER = "bee-StarPicker";
  private static final String STYLE_STAR_CLUSTER = "bee-StarCluster-";

  private static final int CHOICE_MAX_HORIZONTAL_ITEMS = 10;
  private static final int CHOICE_MAX_HORIZONTAL_CHARS = 100;

  public void choice(String caption, String prompt, List<String> options,
      final ChoiceCallback callback, final int defaultValue, final int timeout,
      String cancelHtml, WidgetInitializer initializer) {
    Assert.notEmpty(options);
    Assert.notNull(callback);

    final Holder<State> state = Holder.of(State.OPEN);

    final DialogBox dialog = DialogBox.create(caption, STYLE_CHOICE_DIALOG);
    UiHelper.initialize(dialog, initializer, DialogConstants.WIDGET_DIALOG);

    final Timer timer = (timeout > 0) ? new DialogTimer(dialog, state) : null;

    Panel panel = new Flow();
    panel.addStyleName(STYLE_CHOICE_PANEL);

    if (!BeeUtils.isEmpty(prompt)) {
      Label label = new Label(prompt.trim());
      label.addStyleName(STYLE_CHOICE_PROMPT);

      UiHelper.add(panel, label, initializer, DialogConstants.WIDGET_PROMPT);
    }

    int size = options.size();

    boolean vertical = size > CHOICE_MAX_HORIZONTAL_ITEMS;
    if (!vertical && size > 1) {
      int len = 0;
      for (String option : options) {
        len += option.trim().length();
      }
      vertical = len > CHOICE_MAX_HORIZONTAL_CHARS;
    }

    TabBar group = new TabBar(STYLE_CHOICE_GROUP,
        vertical ? Orientation.VERTICAL : Orientation.HORIZONTAL);

    for (int i = 0; i < size; i++) {
      Widget widget = UiHelper.initialize(new Button(options.get(i)), initializer,
          DialogConstants.WIDGET_COMMAND_ITEM);
      group.addItem(widget);
    }

    final Holder<Integer> cancelIndex = Holder.absent();

    if (!BeeUtils.isEmpty(cancelHtml)) {
      Widget widget = UiHelper.initialize(new Button(cancelHtml), initializer,
          DialogConstants.WIDGET_CANCEL);
      if (widget != null) {
        widget.addStyleName(STYLE_CHOICE_CANCEL);
        group.addItem(widget);
        cancelIndex.set(group.getItemCount() - 1);
      }
    }

    Flow container = new Flow();
    container.addStyleName(STYLE_CHOICE_CONTAINER);
    container.addStyleName(STYLE_CHOICE_CONTAINER + BeeConst.STRING_MINUS
        + (vertical ? StyleUtils.SUFFIX_VERTICAL : StyleUtils.SUFFIX_HORIZONTAL));

    UiHelper.add(container, group, initializer, DialogConstants.WIDGET_COMMAND_GROUP);
    panel.add(container);

    final Holder<Integer> selectedIndex = Holder.absent();

    group.addSelectionHandler(new SelectionHandler<Integer>() {
      @Override
      public void onSelection(SelectionEvent<Integer> event) {
        selectedIndex.set(event.getSelectedItem());
        dialog.close();
      }
    });

    dialog.setHideOnEscape(true);

    dialog.addCloseHandler(new CloseEvent.Handler() {
      @Override
      public void onClose(CloseEvent event) {
        if (timer != null) {
          timer.cancel();
        }

        if (selectedIndex.isNotNull() && !cancelIndex.contains(selectedIndex.get())) {
          callback.onSuccess(selectedIndex.get());
        } else if (State.EXPIRED.equals(state.get())) {
          callback.onTimeout();
        } else {
          callback.onCancel();
        }
      }
    });

    UiHelper.setWidget(dialog, panel, initializer, DialogConstants.WIDGET_PANEL);

    dialog.setAnimationEnabled(true);
    dialog.center();

    int focusIndex;

    if (group.isIndex(defaultValue)) {
      group.getTabWidget(defaultValue).addStyleName(STYLE_CHOICE_DEFAULT);
      group.selectTab(defaultValue, false);
      focusIndex = defaultValue;
    } else if (cancelIndex.isNotNull()) {
      focusIndex = cancelIndex.get();
    } else {
      focusIndex = 0;
    }

    group.focusTab(focusIndex);

    if (timer != null) {
      timer.schedule(timeout);
    }
  }

  public void confirm(String caption, Icon icon, List<String> messages,
      String optionYes, String optionNo, final ConfirmationCallback callback,
      String dialogStyle, String messageStyle, String buttonStyle, Element target) {

    Assert.notEmpty(messages);
    Assert.notNull(callback);

    List<String> options = Lists.newArrayList(optionYes, optionNo);

    ChoiceCallback choice = new ChoiceCallback() {
      @Override
      public void onCancel() {
        callback.onCancel();
      }

      @Override
      public void onSuccess(int value) {
        if (value == 0) {
          callback.onConfirm();
        } else {
          callback.onCancel();
        }
      }

      @Override
      public void onTimeout() {
        callback.onCancel();
      }
    };

    display(caption, icon, messages, options, 1, choice, BeeConst.UNDEF,
        dialogStyle, messageStyle, buttonStyle, target, null);
  }

  public void decide(String caption, List<String> messages, final DecisionCallback callback,
      int defaultValue, String dialogStyle, String messageStyle, String buttonStyle,
      Element target) {
    Assert.notEmpty(messages);
    Assert.notNull(callback);

    List<String> options = Lists.newArrayList(Localized.getConstants().yes(),
        Localized.getConstants().no(), Localized.getConstants().cancel());

    ChoiceCallback choice = new ChoiceCallback() {
      @Override
      public void onCancel() {
        callback.onCancel();
      }

      @Override
      public void onSuccess(int value) {
        switch (value) {
          case DialogConstants.DECISION_YES:
            callback.onConfirm();
            break;

          case DialogConstants.DECISION_NO:
            callback.onDeny();
            break;

          default:
            callback.onCancel();
        }
      }

      @Override
      public void onTimeout() {
        callback.onCancel();
      }
    };

    display(caption, Icon.QUESTION, messages, options, defaultValue, choice, BeeConst.UNDEF,
        dialogStyle, messageStyle, buttonStyle, target, null);
  }

  public void display(String caption, Icon icon, List<String> messages, List<String> options,
      final int defaultValue, final ChoiceCallback callback, final int timeout,
      String dialogStyle, String messageStyle, String buttonStyle,
      Element target, WidgetInitializer initializer) {

    final Popup popup;
    if (BeeUtils.isEmpty(caption)) {
      popup = new Popup(OutsideClick.IGNORE);
    } else {
      popup = DialogBox.create(caption);
    }

    popup.addStyleName(BeeUtils.notEmpty(dialogStyle, STYLE_MESSAGE_BOX));

    UiHelper.initialize(popup, initializer, DialogConstants.WIDGET_DIALOG);

    final Holder<State> state = Holder.of(State.OPEN);
    final Timer timer = (timeout > 0) ? new DialogTimer(popup, state) : null;

    HtmlTable table = new HtmlTable();
    table.addStyleName(STYLE_MESSAGE_BOX_PANEL);

    int row = 0;
    int col = 0;

    if (icon != null) {
      Widget iconWidget = UiHelper.initialize(new Image(icon.getImageResource()), initializer,
          DialogConstants.WIDGET_ICON);

      if (iconWidget != null) {
        table.setWidgetAndStyle(row, col, iconWidget, STYLE_MESSAGE_BOX_ICON);
        col++;
      }
    }

    if (!BeeUtils.isEmpty(messages)) {
      for (String message : messages) {
        if (message != null) {
          Widget messageWidget = UiHelper.initialize(new Label(message), initializer,
              DialogConstants.WIDGET_PROMPT);

          if (messageWidget != null) {
            if (!BeeUtils.isEmpty(messageStyle)) {
              messageWidget.addStyleName(messageStyle);
            }
            table.setWidgetAndStyle(row++, col, messageWidget, STYLE_MESSAGE_BOX_MESSAGE);
          }
        }
      }
    }

    final Holder<Integer> selectedIndex = Holder.absent();

    final Horizontal group = new Horizontal();

    if (!BeeUtils.isEmpty(options)) {
      for (String option : options) {
        Button button = new Button(option, new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            selectedIndex.set(group.getWidgetIndex((Widget) event.getSource()));
            popup.close();
          }
        });

        button.addStyleName(STYLE_MESSAGE_BOX_BUTTON);
        if (!BeeUtils.isEmpty(buttonStyle)) {
          button.addStyleName(buttonStyle);
        }

        Widget widget = UiHelper.initialize(button, initializer,
            DialogConstants.WIDGET_COMMAND_ITEM);
        if (widget != null) {
          group.add(widget);
        }
      }

      UiHelper.initialize(group, initializer, DialogConstants.WIDGET_COMMAND_GROUP);
      table.setWidgetAndStyle(row, col, group, STYLE_MESSAGE_BOX_BUTTON_GROUP);
    }

    popup.setHideOnEscape(true);

    popup.addCloseHandler(new CloseEvent.Handler() {
      @Override
      public void onClose(CloseEvent event) {
        if (timer != null) {
          timer.cancel();
        }

        if (callback != null) {
          if (selectedIndex.isNotNull()) {
            callback.onSuccess(selectedIndex.get());
          } else if (State.EXPIRED.equals(state.get())) {
            callback.onTimeout();
          } else {
            callback.onCancel();
          }
        }
      }
    });

    UiHelper.setWidget(popup, table, initializer, DialogConstants.WIDGET_PANEL);

    popup.setAnimationEnabled(true);

    if (target == null) {
      popup.center();
    } else {
      popup.showRelativeTo(target);
    }

    if (defaultValue >= 0 && defaultValue < group.getWidgetCount()) {
      UiHelper.focus(group.getWidget(defaultValue));

      if (group.getWidgetCount() > 1) {
        for (Widget widget : group) {
          Binder.addKeyDownHandler(widget, new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
              if (event.isRightArrow() || event.isDownArrow()) {
                rotateFocus(event, group, true);
              } else if (event.isLeftArrow() || event.isUpArrow()) {
                rotateFocus(event, group, false);
              }
            }
          });
        }
      }
    }

    if (timer != null) {
      timer.schedule(timeout);
    }
  }

  public boolean nativeConfirm(String... lines) {
    Assert.notNull(lines);
    Assert.parameterCount(lines.length, 1);
    return Window.confirm(BeeUtils.buildLines(lines));
  }

  public void pickStar(int starCount, Integer defaultValue, Element target,
      final ChoiceCallback callback) {
    Assert.notNull(callback);

    final Popup popup = new Popup(OutsideClick.CLOSE, STYLE_STAR_PICKER);

    TabBar cluster = new TabBar(STYLE_STAR_CLUSTER, Orientation.HORIZONTAL);

    Image delStar = new Image(Stars.getDefaultHeaderResource());

    cluster.addItem(delStar);

    for (int i = 0; i < starCount; i++) {
      Image image = new Image(Stars.get(i));
      cluster.addItem(image);
    }

    final Holder<Integer> selectedIndex = Holder.absent();

    cluster.addSelectionHandler(new SelectionHandler<Integer>() {
      @Override
      public void onSelection(SelectionEvent<Integer> event) {
        selectedIndex.set(event.getSelectedItem());
        popup.close();
      }
    });

    popup.setHideOnEscape(true);

    popup.addCloseHandler(new CloseEvent.Handler() {
      @Override
      public void onClose(CloseEvent event) {
        if (selectedIndex.isNotNull()) {
          callback.onSuccess(selectedIndex.get());
        } else {
          callback.onCancel();
        }
      }
    });

    popup.setWidget(cluster);

    popup.setAnimationEnabled(true);

    if (target == null) {
      popup.center();
    } else {
      popup.showRelativeTo(target);
    }

    int focusIndex;

    if (defaultValue != null && cluster.isIndex(defaultValue + 1)) {
      cluster.selectTab(defaultValue + 1, false);
      focusIndex = defaultValue + 1;
    } else {
      focusIndex = 0;
    }

    cluster.focusTab(focusIndex);
  }

  public void showError(String caption, List<String> messages, String dialogStyle,
      String closeHtml) {

    List<String> options = Lists.newArrayList(BeeUtils.notEmpty(closeHtml,
        Localized.getConstants().ok()));

    display(caption, Icon.ERROR, messages, options, 0, null, BeeConst.UNDEF, dialogStyle, null,
        null, null, null);
  }

  public void showInfo(String caption, List<String> messages, String dialogStyle,
      String closeHtml) {

    List<String> options = Lists.newArrayList(BeeUtils.notEmpty(closeHtml,
        Localized.getConstants().ok()));

    display(caption, Icon.INFORMATION, messages, options, 0, null, BeeConst.UNDEF, dialogStyle,
        null, null, null, null);
  }

  public void showTable(String caption, IsTable<?, ?> table) {
    Assert.notNull(table);

    int c = table.getNumberOfColumns();
    Assert.isPositive(c);

    int r = table.getNumberOfRows();
    if (r <= 0) {
      logger.warning(caption, "data table empty");
      return;
    }

    HtmlTable grid = new HtmlTable();
    grid.addStyleName(STYLE_TABLE);
    int index = 0;

    if (!BeeUtils.isEmpty(caption)) {
      grid.setHtml(index, 0, caption.trim());
      grid.alignCenter(index, 0);
      if (c > 1) {
        grid.getCellFormatter().setColSpan(index, 0, c);
      }
      index++;
    }

    for (int j = 0; j < c; j++) {
      grid.setHtml(index, j, table.getColumnLabel(j));
      grid.alignCenter(index, j);
    }
    index++;

    for (int i = 0; i < r; i++) {
      for (int j = 0; j < c; j++) {
        String value = table.getString(i, j);
        if (!BeeUtils.isEmpty(value)) {
          grid.setHtml(index, j, value);
          if (ValueType.isNumeric(table.getColumnType(j))) {
            grid.alignRight(index, j);
          }
        }
      }
      grid.getRowFormatter().setStyleName(index,
          (index % 2 == 0) ? CellGrid.STYLE_EVEN_ROW : CellGrid.STYLE_ODD_ROW);
      index++;
    }

    CloseButton close = new CloseButton(Localized.getConstants().ok());
    grid.setWidget(index, 0, close);
    grid.alignCenter(index, 0);
    if (c > 1) {
      grid.getCellFormatter().setColSpan(index, 0, c);
    }

    Simple container = new Simple(grid);
    container.addStyleName(STYLE_TABLE_CONTAINER);

    Popup popup = new Popup(OutsideClick.CLOSE);
    popup.setAnimationEnabled(true);
    popup.setHideOnEscape(true);

    popup.setWidget(container);

    popup.center();
    close.setFocus(true);
  }

  public void showWidget(String caption, Widget widget, Element target) {
    Assert.notNull(widget);

    Popup popup;
    if (BeeUtils.isEmpty(caption)) {
      popup = new Popup(OutsideClick.CLOSE);
    } else {
      popup = DialogBox.create(caption);
    }

    popup.setAnimationEnabled(true);
    popup.setHideOnEscape(true);

    popup.setWidget(widget);

    popup.showRelativeTo(target);
  }

  private static void rotateFocus(Event<?> event, IndexedPanel panel, boolean forward) {
    if (!(event.getSource() instanceof Widget)) {
      return;
    }

    int oldIndex = panel.getWidgetIndex((Widget) event.getSource());
    if (oldIndex < 0) {
      return;
    }

    int newIndex;
    if (forward) {
      newIndex = BeeUtils.rotateForwardExclusive(oldIndex, 0, panel.getWidgetCount());
    } else {
      newIndex = BeeUtils.rotateBackwardExclusive(oldIndex, 0, panel.getWidgetCount());
    }

    if (newIndex >= 0 && newIndex != oldIndex) {
      UiHelper.focus(panel.getWidget(newIndex));
    }
  }
}
