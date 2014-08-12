package com.butent.bee.client.dialog;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.event.Previewer.PreviewConsumer;
import com.butent.bee.client.event.logical.CloseEvent;
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
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.State;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Implements a user interface component, which enables to produce a input box for information input
 * from the user.
 */

public class InputBoxes {

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

      confirm.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          String message = errorSupplier.get();
          if (BeeUtils.isEmpty(message)) {
            state.set(State.CONFIRMED);
            dialog.close();
          } else {
            showError(errorDisplay.get(), message);
          }
        }
      });

      UiHelper.add(commandGroup, confirm, initializer, DialogConstants.WIDGET_CONFIRM);
    }

    if (!BeeUtils.isEmpty(cancelHtml)) {
      Button cancel = new Button(cancelHtml);
      cancel.addStyleName(STYLE_INPUT_COMMAND);
      cancel.addStyleName(STYLE_INPUT_CANCEL);

      cancel.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          state.set(State.CANCELED);
          dialog.close();
        }
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
  private static final String STYLE_INPUT_PANEL = "bee-InputPanel";
  private static final String STYLE_INPUT_PROMPT = "bee-InputPrompt";
  private static final String STYLE_INPUT_STRING = "bee-InputString";
  private static final String STYLE_INPUT_WIDGET = "bee-InputWidget";
  private static final String STYLE_INPUT_ERROR = "bee-InputError";
  private static final String STYLE_INPUT_COMMAND_GROUP = "bee-InputCommandGroup";
  private static final String STYLE_INPUT_COMMAND = "bee-InputCommand";

  private static final String STYLE_INPUT_CONFIRM = "bee-InputConfirm";
  private static final String STYLE_INPUT_CANCEL = "bee-InputCancel";
  private static final String STYLE_INPUT_ADD = "bee-InputAdd";
  private static final String STYLE_INPUT_DELETE = "bee-InputDelete";
  private static final String STYLE_INPUT_PRINT = "bee-InputPrint";

  private static final String STYLE_INPUT_SAVE = "bee-InputSave";

  private static final String STYLE_INPUT_CLOSE = "bee-InputClose";

  public void inputCollection(String caption, String valueCaption, final boolean unique,
      Collection<String> defaultCollection, final Consumer<Collection<String>> consumer,
      final Function<String, Editor> editorSupplier) {

    Assert.notNull(consumer);

    final HtmlTable table = new HtmlTable();
    final Consumer<String> rowCreator = new Consumer<String>() {
      @Override
      public void accept(String value) {
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
        int row = table.getRowCount();
        table.setWidget(row, 0, input.asWidget());

        final FaLabel delete = new FaLabel(FontAwesome.TRASH_O);
        delete.setTitle(Localized.getConstants().delete());
        delete.getElement().getStyle().setCursor(Cursor.POINTER);

        delete.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            for (int i = 0; i < table.getRowCount(); i++) {
              if (Objects.equal(delete, table.getWidget(i, 1))) {
                table.removeRow(i);
                break;
              }
            }
          }
        });
        table.setWidget(row, 1, delete);
      }
    };
    if (!BeeUtils.isEmpty(defaultCollection)) {
      for (String value : defaultCollection) {
        rowCreator.accept(value);
      }
    }
    FlowPanel widget = new FlowPanel();
    Label cap = new Label(valueCaption);
    StyleUtils.setTextAlign(cap.getElement(), TextAlign.CENTER);
    widget.add(cap);

    widget.add(table);

    FaLabel add = new FaLabel(FontAwesome.PLUS);
    add.setTitle(Localized.getConstants().actionAdd());
    add.getElement().getStyle().setCursor(Cursor.POINTER);
    StyleUtils.setTextAlign(add.getElement(), TextAlign.CENTER);

    add.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        rowCreator.accept(null);
        UiHelper.focus(table.getWidget(table.getRowCount() - 1, 0));
      }
    });
    widget.add(add);

    inputWidget(caption, widget, new InputCallback() {
      @Override
      public String getErrorMessage() {
        String error = super.getErrorMessage();

        if (BeeUtils.isEmpty(error)) {
          Set<String> values = Sets.newHashSet();

          for (int i = 0; i < table.getRowCount(); i++) {
            Editor input = (Editor) table.getWidget(i, 0);
            String value = input.getNormalizedValue();

            if (BeeUtils.isEmpty(value)) {
              error = Localized.getConstants().valueRequired();
            } else if (unique && values.contains(BeeUtils.normalize(value))) {
              error = Localized.getMessages().valueExists(value);
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
      public void onSuccess() {
        Collection<String> result;

        if (unique) {
          result = Sets.newLinkedHashSet();
        } else {
          result = Lists.newArrayList();
        }
        for (int i = 0; i < table.getRowCount(); i++) {
          result.add(((Editor) table.getWidget(i, 0)).getNormalizedValue());
        }
        consumer.accept(result);
      }
    });
  }

  public void inputMap(String caption, final String keyCaption, final String valueCaption,
      Map<String, String> map, final Consumer<Map<String, String>> consumer) {

    final HtmlTable table = new HtmlTable();
    final BiConsumer<String, String> rowCreator = new BiConsumer<String, String>() {
      @Override
      public void accept(String key, String value) {
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
        delete.setTitle(Localized.getConstants().delete());
        delete.getElement().getStyle().setCursor(Cursor.POINTER);

        delete.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            for (int i = 1; i < table.getRowCount(); i++) {
              if (Objects.equal(delete, table.getWidget(i, 2))) {
                table.removeRow(i);
                break;
              }
            }
          }
        });
        table.setWidget(row, 2, delete);
      }
    };
    Label cap = new Label(keyCaption);
    StyleUtils.setMinWidth(cap, 100);
    StyleUtils.setTextAlign(cap.getElement(), TextAlign.CENTER);
    table.setWidget(0, 0, cap);

    cap = new Label(valueCaption);
    StyleUtils.setMinWidth(cap, 100);
    StyleUtils.setTextAlign(cap.getElement(), TextAlign.CENTER);
    table.setWidget(0, 1, cap);

    for (String key : map.keySet()) {
      rowCreator.accept(key, map.get(key));
    }
    FlowPanel widget = new FlowPanel();
    widget.add(table);

    FaLabel add = new FaLabel(FontAwesome.PLUS);
    add.setTitle(Localized.getConstants().actionAdd());
    add.getElement().getStyle().setCursor(Cursor.POINTER);
    StyleUtils.setTextAlign(add.getElement(), TextAlign.CENTER);

    add.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        rowCreator.accept(null, null);
        UiHelper.focus(table.getWidget(table.getRowCount() - 1, 0));
      }
    });
    widget.add(add);

    inputWidget(caption, widget, new InputCallback() {
      @Override
      public String getErrorMessage() {
        String error = super.getErrorMessage();

        if (BeeUtils.isEmpty(error)) {
          Set<String> values = Sets.newHashSet();

          for (int i = 1; i < table.getRowCount(); i++) {
            InputText input = (InputText) table.getWidget(i, 0);

            if (BeeUtils.isEmpty(input.getValue())) {
              error = Localized.getConstants().valueRequired();
            } else if (values.contains(BeeUtils.normalize(input.getValue()))) {
              error = Localized.getMessages().valueExists(input.getValue());
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
      public void onSuccess() {
        Map<String, String> result = Maps.newLinkedHashMap();

        for (int i = 1; i < table.getRowCount(); i++) {
          result.put(((InputText) table.getWidget(i, 0)).getValue(),
              ((InputText) table.getWidget(i, 1)).getValue());
        }
        consumer.accept(result);
      }
    });
  }

  public void inputString(String caption, String prompt, final StringCallback callback,
      String defaultValue, int maxLength, Element target, double width, CssUnit widthUnit,
      final int timeout, String confirmHtml, String cancelHtml, WidgetInitializer initializer) {

    Assert.notNull(callback);

    final Holder<State> state = Holder.of(State.OPEN);

    final DialogBox dialog = DialogBox.create(caption);
    UiHelper.initialize(dialog, initializer, DialogConstants.WIDGET_DIALOG);

    final Timer timer = (timeout > 0) ? new DialogTimer(dialog, state) : null;

    Panel panel = new Flow();
    panel.addStyleName(STYLE_INPUT_PANEL);

    if (!BeeUtils.isEmpty(prompt)) {
      Label label = new Label(prompt.trim());
      label.addStyleName(STYLE_INPUT_PROMPT);

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

    final Supplier<String> errorSupplier = new Supplier<String>() {
      @Override
      public String get() {
        return callback.getMessage(EditorAssistant.getValue(input.get()));
      }
    };

    box.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
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
      }
    });

    UiHelper.add(panel, input, initializer, DialogConstants.WIDGET_INPUT);

    UiHelper.add(panel, errorDisplay, initializer, DialogConstants.WIDGET_ERROR);

    addCommandGroup(dialog, panel, confirmHtml, cancelHtml, initializer, state, errorDisplay,
        errorSupplier);

    dialog.addCloseHandler(new CloseEvent.Handler() {
      @Override
      public void onClose(CloseEvent event) {
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
      }
    });

    UiHelper.setWidget(dialog, panel, initializer, DialogConstants.WIDGET_PANEL);

    dialog.setAnimationEnabled(true);
    dialog.showRelativeTo(target);

    UiHelper.focus(input.get());

    if (timer != null) {
      timer.schedule(timeout);
    }
  }

  public DialogBox inputWidget(String caption, IsWidget widget, final InputCallback callback,
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

    boolean enabled = (widget instanceof HasEnabled) ? ((HasEnabled) widget).isEnabled() : true;

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

      final ScheduledCommand onSave = new ScheduledCommand() {
        @Override
        public void execute() {
          String message = callback.getErrorMessage();
          if (BeeUtils.isEmpty(message)) {
            dialog.close();
            callback.onSuccess();
          } else {
            showError(errorDisplay.get(), message);
          }
        }
      };

      Image save = new Image(Global.getImages().silverSave(), onSave);
      save.addStyleName(STYLE_INPUT_SAVE);
      UiHelper.initialize(save, initializer, DialogConstants.WIDGET_SAVE);
      dialog.addAction(Action.SAVE, save);

      dialog.setOnSave(new PreviewConsumer() {
        @Override
        public void accept(NativePreviewEvent input) {
          if (input != null) {
            input.cancel();
          }
          onSave.execute();
        }
      });

      onClose = new ScheduledCommand() {
        @Override
        public void execute() {
          callback.onClose(new CloseCallback() {
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
        }
      };

    } else {
      onClose = new ScheduledCommand() {
        @Override
        public void execute() {
          callback.onClose(new CloseCallback() {
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
      };
    }

    if (enabledActions != null) {
      if (enabled && enabledActions.contains(Action.ADD)) {
        Image add = new Image(Global.getImages().silverAdd(), new ScheduledCommand() {
          @Override
          public void execute() {
            callback.onAdd();
          }
        });

        add.addStyleName(STYLE_INPUT_ADD);
        UiHelper.initialize(add, initializer, DialogConstants.WIDGET_ADD);
        dialog.addAction(Action.ADD, add);
      }

      if (enabled && enabledActions.contains(Action.DELETE)) {
        Image delete = new Image(Global.getImages().silverDelete(), new ScheduledCommand() {
          @Override
          public void execute() {
            callback.onDelete(dialog);
          }
        });

        delete.addStyleName(STYLE_INPUT_DELETE);
        UiHelper.initialize(delete, initializer, DialogConstants.WIDGET_DELETE);
        dialog.addAction(Action.DELETE, delete);
      }

      if (enabledActions.contains(Action.PRINT)) {
        Image print = new Image(Global.getImages().silverPrint(), new ScheduledCommand() {
          @Override
          public void execute() {
            Printer.print(dialog);
          }
        });

        print.addStyleName(STYLE_INPUT_PRINT);
        UiHelper.initialize(print, initializer, DialogConstants.WIDGET_PRINT);
        dialog.addAction(Action.PRINT, print);
      }
    }

    Image close = new Image(Global.getImages().silverClose(), onClose);
    close.addStyleName(STYLE_INPUT_CLOSE);
    UiHelper.initialize(close, initializer, DialogConstants.WIDGET_CLOSE);
    dialog.addAction(Action.CLOSE, close);

    dialog.setOnEscape(new PreviewConsumer() {
      @Override
      public void accept(NativePreviewEvent input) {
        if (input != null) {
          input.cancel();
        }
        onClose.execute();
      }
    });

    UiHelper.setWidget(dialog, panel, initializer, DialogConstants.WIDGET_PANEL);

    dialog.setAnimationEnabled(true);
    dialog.showRelativeTo(target);

    UiHelper.focus(widget.asWidget());
    return dialog;
  }

  private void inputWidget(String caption, IsWidget input, InputCallback callback) {
    inputWidget(caption, input, callback, null, null, Action.NO_ACTIONS, null);
  }
}
