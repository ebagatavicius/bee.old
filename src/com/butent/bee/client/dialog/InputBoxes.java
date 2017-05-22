package com.butent.bee.client.dialog;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.WidgetInitializer;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.EditorAssistant;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.State;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.Overflow;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implements a user interface component, which enables to produce a input box for information input
 * from the user.
 */

public final class InputBoxes {

  private static boolean addCommandGroup(final Popup dialog, HasWidgets panel, String confirmHtml,
      String cancelHtml, WidgetInitializer initializer, final Holder<State> state,
      final Holder<Widget> errorDisplay, final Supplier<String> errorSupplier) {

    if (BeeUtils.allEmpty(confirmHtml, cancelHtml)) {
      return false;
    }

    Panel commandGroup = new Flow();
    commandGroup.addStyleName(STYLE_INPUT_COMMAND_GROUP);

    if (!BeeUtils.isEmpty(confirmHtml)) {
      Button confirm = new Button(confirmHtml);
      confirm.addStyleName(STYLE_INPUT_COMMAND);
      confirm.addStyleName(STYLE_INPUT_CONFIRM);

      confirm.addClickHandler(event -> {
        String message = errorSupplier.get();
        if (BeeUtils.isEmpty(message)) {
          state.set(State.CONFIRMED);
          dialog.close();
        } else {
          showError(errorDisplay.get(), message);
        }
      });

      UiHelper.add(commandGroup, confirm, initializer, DialogConstants.WIDGET_CONFIRM);
    }

    if (!BeeUtils.isEmpty(cancelHtml)) {
      Button cancel = new Button(cancelHtml);
      cancel.addStyleName(STYLE_INPUT_COMMAND);
      cancel.addStyleName(STYLE_INPUT_CANCEL);

      cancel.addClickHandler(event -> {
        state.set(State.CANCELED);
        dialog.close();
      });

      UiHelper.add(commandGroup, cancel, initializer, DialogConstants.WIDGET_CANCEL);
    }

    UiHelper.add(panel, commandGroup, initializer, DialogConstants.WIDGET_COMMAND_GROUP);
    return true;
  }

  private static Holder<Widget> hold(Widget widget) {
    return Holder.of(widget);
  }

  private static void showError(Widget widget, String message) {
    if (!BeeUtils.isEmpty(message) && !SILENT_ERROR.equals(message)) {
      if (widget instanceof HasHtml) {
        ((HasHtml) widget).setHtml(message);
      } else if (widget instanceof NotificationListener) {
        ((NotificationListener) widget).notifySevere(message);
      } else {
        BeeKeeper.getScreen().notifySevere(message);
      }
    }
  }

  public static final String SILENT_ERROR = "-";

  private static final String STYLE_INPUT_PANEL = BeeConst.CSS_CLASS_PREFIX + "InputPanel";
  private static final String STYLE_INPUT_PROMPT = BeeConst.CSS_CLASS_PREFIX + "InputPrompt";
  private static final String STYLE_INPUT_STRING = BeeConst.CSS_CLASS_PREFIX + "InputString";
  private static final String STYLE_INPUT_WIDGET = BeeConst.CSS_CLASS_PREFIX + "InputWidget";
  private static final String STYLE_INPUT_ERROR = BeeConst.CSS_CLASS_PREFIX + "InputError";
  private static final String STYLE_INPUT_COMMAND_GROUP = BeeConst.CSS_CLASS_PREFIX
      + "InputCommandGroup";
  private static final String STYLE_INPUT_COMMAND = BeeConst.CSS_CLASS_PREFIX + "InputCommand";

  private static final String STYLE_INPUT_CONFIRM = BeeConst.CSS_CLASS_PREFIX + "InputConfirm";
  private static final String STYLE_INPUT_CANCEL = BeeConst.CSS_CLASS_PREFIX + "InputCancel";
  private static final String STYLE_INPUT_ADD = BeeConst.CSS_CLASS_PREFIX + "InputAdd";
  private static final String STYLE_INPUT_DELETE = BeeConst.CSS_CLASS_PREFIX + "InputDelete";
  private static final String STYLE_INPUT_PRINT = BeeConst.CSS_CLASS_PREFIX + "InputPrint";

  private static final String STYLE_INPUT_SAVE = BeeConst.CSS_CLASS_PREFIX + "InputSave";

  private static final String STYLE_INPUT_CLOSE = BeeConst.CSS_CLASS_PREFIX + "InputClose";

  public static void inputCollection(String caption, String valueCaption, boolean unique,
      Collection<String> defaultCollection, Consumer<Collection<String>> consumer,
      Function<String, Editor> editorSupplier) {

    Assert.notNull(consumer);

    HtmlTable table = new HtmlTable();
    StyleUtils.setWidth(table, 100, CssUnit.PCT);

    table.setText(0, 0, valueCaption);
    table.getCellFormatter().setHorizontalAlignment(0, 0, TextAlign.CENTER);
    StyleUtils.setMinWidth(table.getCellFormatter().getElement(0, 0), 200);

    Consumer<String> rowCreator = value -> {
      Editor input = null;

      if (editorSupplier != null) {
        input = editorSupplier.apply(value);
      }
      if (input == null) {
        input = new InputText();

        if (!BeeUtils.isEmpty(value)) {
          input.setValue(value);
        }
      }
      StyleUtils.setWidth(input.getElement(), 100, CssUnit.PCT);

      int row = table.getRowCount();
      table.setWidget(row, 0, input.asWidget());

      FaLabel delete = new FaLabel(FontAwesome.TRASH_O);
      delete.setTitle(Localized.dictionary().delete());
      delete.getElement().getStyle().setCursor(Cursor.POINTER);

      delete.addClickHandler(event -> {
        for (int i = 1; i < table.getRowCount(); i++) {
          if (Objects.equals(delete, table.getWidget(i, 1))) {
            table.removeRow(i);
            break;
          }
        }
      });
      table.setWidget(row, 1, delete);
    };
    if (!BeeUtils.isEmpty(defaultCollection)) {
      for (String value : defaultCollection) {
        rowCreator.accept(value);
      }
    }
    Flow flow = new Flow();
    StyleUtils.setMaxHeight(flow, 80, CssUnit.VH);
    StyleUtils.setOverflow(flow, StyleUtils.ScrollBars.BOTH, Overflow.AUTO);
    flow.add(table);

    inputWidget(caption, flow, new InputCallback() {
      @Override
      public String getErrorMessage() {
        String error = InputCallback.super.getErrorMessage();

        if (BeeUtils.isEmpty(error)) {
          Set<String> values = new HashSet<>();

          for (int i = 1; i < table.getRowCount(); i++) {
            Editor input = (Editor) table.getWidget(i, 0);
            String value = input.getNormalizedValue();

            if (BeeUtils.isEmpty(value)) {
              error = Localized.dictionary().valueRequired();
            } else if (unique && values.contains(BeeUtils.normalize(value))) {
              error = Localized.dictionary().valueExists(value);
            } else {
              values.add(BeeUtils.normalize(value));
              continue;
            }
            UiHelper.focus(input.asWidget());
            break;
          }
        }
        return error;
      }

      @Override
      public void onAdd() {
        rowCreator.accept(null);
        UiHelper.focus(table.getWidget(table.getRowCount() - 1, 0));
      }

      @Override
      public void onSuccess() {
        Collection<String> result;

        if (unique) {
          result = new LinkedHashSet<>();
        } else {
          result = new ArrayList<>();
        }
        for (int i = 1; i < table.getRowCount(); i++) {
          result.add(((Editor) table.getWidget(i, 0)).getNormalizedValue());
        }
        consumer.accept(result);
      }
    }, null, null, EnumSet.of(Action.ADD), null);
  }

  public static void inputMap(String caption, String keyCaption, String valueCaption,
      Map<String, String> map, Consumer<Map<String, String>> consumer) {

    final HtmlTable table = new HtmlTable();
    table.setText(0, 0, keyCaption);
    table.getCellFormatter().setHorizontalAlignment(0, 0, TextAlign.CENTER);
    StyleUtils.setMinWidth(table.getCellFormatter().getElement(0, 0), 100);

    table.setText(0, 1, valueCaption);
    table.getCellFormatter().setHorizontalAlignment(0, 1, TextAlign.CENTER);
    StyleUtils.setMinWidth(table.getCellFormatter().getElement(0, 1), 100);

    final BiConsumer<String, String> rowCreator = (key, value) -> {
      int row = table.getRowCount();
      InputText input = new InputText();
      table.setWidget(row, 0, input);

      if (!BeeUtils.isEmpty(key)) {
        input.setValue(key);
      }
      input = new InputText();
      table.setWidget(row, 1, input);

      if (!BeeUtils.isEmpty(value)) {
        input.setValue(value);
      }
      final FaLabel delete = new FaLabel(FontAwesome.TRASH_O);
      delete.setTitle(Localized.dictionary().delete());
      delete.getElement().getStyle().setCursor(Cursor.POINTER);

      delete.addClickHandler(event -> {
        for (int i = 1; i < table.getRowCount(); i++) {
          if (Objects.equals(delete, table.getWidget(i, 2))) {
            table.removeRow(i);
            break;
          }
        }
      });
      table.setWidget(row, 2, delete);
    };
    for (String key : map.keySet()) {
      rowCreator.accept(key, map.get(key));
    }
    inputWidget(caption, table, new InputCallback() {
      @Override
      public String getErrorMessage() {
        String error = InputCallback.super.getErrorMessage();

        if (BeeUtils.isEmpty(error)) {
          Set<String> values = new HashSet<>();

          for (int i = 1; i < table.getRowCount(); i++) {
            InputText input = (InputText) table.getWidget(i, 0);

            if (BeeUtils.isEmpty(input.getValue())) {
              error = Localized.dictionary().valueRequired();
            } else if (values.contains(BeeUtils.normalize(input.getValue()))) {
              error = Localized.dictionary().valueExists(input.getValue());
            } else {
              values.add(BeeUtils.normalize(input.getValue()));
              continue;
            }
            UiHelper.focus(input);
            break;
          }
        }
        return error;
      }

      @Override
      public void onAdd() {
        rowCreator.accept(null, null);
        UiHelper.focus(table.getWidget(table.getRowCount() - 1, 0));
      }

      @Override
      public void onSuccess() {
        Map<String, String> result = new LinkedHashMap<>();

        for (int i = 1; i < table.getRowCount(); i++) {
          result.put(((InputText) table.getWidget(i, 0)).getValue(),
              ((InputText) table.getWidget(i, 1)).getValue());
        }
        consumer.accept(result);
      }
    }, null, null, EnumSet.of(Action.ADD), null);
  }

  public static void inputString(String caption, String prompt, StringCallback callback,
      String styleName, String defaultValue, int maxLength, Element target, double width,
      CssUnit widthUnit, int timeout, String confirmHtml, String cancelHtml,
      WidgetInitializer initializer) {

    Assert.notNull(callback);

    final Holder<State> state = Holder.of(State.OPEN);

    final DialogBox dialog = DialogBox.create(caption);

    if (!BeeUtils.isEmpty(styleName)) {
      dialog.addStyleName(styleName);
    }

    UiHelper.initialize(dialog, initializer, DialogConstants.WIDGET_DIALOG);

    final Timer timer = (timeout > 0) ? new DialogTimer(dialog, state) : null;

    Panel panel = new Flow();
    panel.addStyleName(STYLE_INPUT_PANEL);

    if (!BeeUtils.isEmpty(prompt)) {
      Label label = new Label(prompt.trim());
      label.addStyleName(STYLE_INPUT_PROMPT);

      if (callback.isRequired()) {
        label.addStyleName(StyleUtils.NAME_REQUIRED);
      }

      UiHelper.add(panel, label, initializer, DialogConstants.WIDGET_PROMPT);
    }

    InputText box = new InputText();
    box.addStyleName(STYLE_INPUT_STRING);

    if (!BeeUtils.isEmpty(defaultValue)) {
      String value = (maxLength > 0)
          ? BeeUtils.left(defaultValue.trim(), maxLength) : defaultValue.trim();
      box.setValue(value);
    }
    if (maxLength > 0) {
      box.setMaxLength(maxLength);
    }
    if (width > 0) {
      StyleUtils.setWidth(box, width, widthUnit);
    }

    final Holder<Widget> input = hold(box);

    Label errorLabel = new Label();
    errorLabel.addStyleName(STYLE_INPUT_ERROR);
    errorLabel.addStyleName(StyleUtils.NAME_ERROR);

    final Holder<Widget> errorDisplay = hold(errorLabel);

    final Supplier<String> errorSupplier =
        () -> callback.getMessage(EditorAssistant.getValue(input.get()));

    box.addKeyDownHandler(event -> {
      if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
        event.preventDefault();
        state.set(State.CANCELED);
        dialog.close();
        return;
      }

      if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
        String message = errorSupplier.get();
        if (BeeUtils.isEmpty(message)) {
          event.preventDefault();
          state.set(State.CONFIRMED);
          dialog.close();
          return;
        } else {
          showError(errorDisplay.get(), message);
        }
      }

      if (timer != null) {
        timer.schedule(timeout);
      }
    });

    UiHelper.add(panel, input, initializer, DialogConstants.WIDGET_INPUT);

    UiHelper.add(panel, errorDisplay, initializer, DialogConstants.WIDGET_ERROR);

    addCommandGroup(dialog, panel, confirmHtml, cancelHtml, initializer, state, errorDisplay,
        errorSupplier);

    dialog.addCloseHandler(event -> {
      if (timer != null) {
        timer.cancel();
      }

      switch (state.get()) {
        case CONFIRMED:
          callback.onSuccess(BeeUtils.trim(EditorAssistant.getValue(input.get())));
          break;
        case EXPIRED:
          callback.onTimeout(BeeUtils.trim(EditorAssistant.getValue(input.get())));
          break;
        default:
          callback.onCancel();
      }
    });

    UiHelper.setWidget(dialog, panel, initializer, DialogConstants.WIDGET_PANEL);

    dialog.setAnimationEnabled(true);

    dialog.focusOnOpen(input.get());
    dialog.showRelativeTo(target);

    if (timer != null) {
      timer.schedule(timeout);
    }
  }

  public static DialogBox inputWidget(String caption, IsWidget widget, InputCallback callback,
      String dialogStyle, Element target, Set<Action> enabledActions,
      WidgetInitializer initializer) {

    Assert.notNull(widget);
    Assert.notNull(callback);

    final DialogBox dialog = DialogBox.withoutCloseBox(caption, dialogStyle);
    UiHelper.initialize(dialog, initializer, DialogConstants.WIDGET_DIALOG);

    Flow panel = new Flow();
    panel.addStyleName(STYLE_INPUT_PANEL);

    widget.asWidget().addStyleName(STYLE_INPUT_WIDGET);
    UiHelper.add(panel, widget.asWidget(), initializer, DialogConstants.WIDGET_INPUT);

    boolean enabled = !(widget instanceof HasEnabled) || ((HasEnabled) widget).isEnabled();

    final ScheduledCommand onClose;

    if (enabled) {
      final Holder<Widget> errorDisplay = new Holder<>(null);

      if (widget instanceof NotificationListener) {
        errorDisplay.set(widget.asWidget());
      } else {
        Label errorLabel = new Label();
        errorLabel.addStyleName(STYLE_INPUT_ERROR);
        errorLabel.addStyleName(StyleUtils.NAME_ERROR);
        errorDisplay.set(errorLabel);
        UiHelper.add(panel, errorDisplay, initializer, DialogConstants.WIDGET_ERROR);
      }

      final ScheduledCommand onSave = () -> {
        String message = callback.getErrorMessage();
        if (BeeUtils.isEmpty(message)) {
          dialog.close();
          callback.onSuccess();
        } else {
          showError(errorDisplay.get(), message);
        }
      };

      Widget save = UiHelper.initialize(new FaLabel(FontAwesome.SAVE), initializer,
          DialogConstants.WIDGET_SAVE);
      save.addStyleName(STYLE_INPUT_SAVE);

      if (save instanceof HasClickHandlers) {
        ((HasClickHandlers) save).addClickHandler(event -> onSave.execute());
      }
      dialog.addAction(Action.SAVE, save);

      dialog.setOnSave(input -> {
        if (input != null) {
          input.cancel();
        }
        onSave.execute();
      });

      onClose = () -> callback.onClose(new CloseCallback() {
        @Override
        public void onClose() {
          dialog.close();
          callback.onCancel();
        }

        @Override
        public void onSave() {
          onSave.execute();
        }
      });

    } else {
      onClose = () -> callback.onClose(new CloseCallback() {
        @Override
        public void onClose() {
          dialog.close();
          callback.onCancel();
        }

        @Override
        public void onSave() {
          onClose();
        }
      });
    }

    if (enabledActions != null) {
      if (enabled && enabledActions.contains(Action.ADD)) {
        FaLabel add = new FaLabel(FontAwesome.PLUS);
        add.addClickHandler(event -> callback.onAdd());
        add.addStyleName(STYLE_INPUT_ADD);

        UiHelper.initialize(add, initializer, DialogConstants.WIDGET_ADD);
        dialog.addAction(Action.ADD, add);
      }

      if (enabled && enabledActions.contains(Action.DELETE)) {
        FaLabel delete = new FaLabel(FontAwesome.TRASH_O);
        delete.addClickHandler(event -> callback.onDelete(dialog));
        delete.addStyleName(STYLE_INPUT_DELETE);

        UiHelper.initialize(delete, initializer, DialogConstants.WIDGET_DELETE);
        dialog.addAction(Action.DELETE, delete);
      }

      if (enabledActions.contains(Action.PRINT)) {
        FaLabel print = new FaLabel(FontAwesome.PRINT);
        print.addClickHandler(event -> Printer.print(dialog));
        print.addStyleName(STYLE_INPUT_PRINT);

        UiHelper.initialize(print, initializer, DialogConstants.WIDGET_PRINT);
        dialog.addAction(Action.PRINT, print);
      }
    }

    FaLabel close = new FaLabel(FontAwesome.CLOSE);
    close.addClickHandler(event -> onClose.execute());
    close.addStyleName(STYLE_INPUT_CLOSE);

    UiHelper.initialize(close, initializer, DialogConstants.WIDGET_CLOSE);
    dialog.addAction(Action.CLOSE, close);

    dialog.setOnEscape(input -> {
      if (input != null) {
        input.cancel();
      }
      onClose.execute();
    });

    UiHelper.setWidget(dialog, panel, initializer, DialogConstants.WIDGET_PANEL);

    dialog.setAnimationEnabled(true);

    dialog.focusOnOpen(widget.asWidget());
    dialog.showRelativeTo(target);

    return dialog;
  }

  private InputBoxes() {
  }
}
