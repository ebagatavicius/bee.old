package com.butent.bee.client.modules.mail;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.modules.mail.MailConstants.RuleAction;
import com.butent.bee.shared.modules.mail.MailConstants.RuleCondition;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.EnumSet;

public class RuleForm extends AbstractFormInterceptor implements SelectorEvent.Handler {

  private ListBox conditionWidget;
  private InputText conditionOptions;

  private ListBox actionWidget;
  private InputText actionOptions;
  private UnboundSelector folder;
  private InputArea reply;

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    if (BeeUtils.same(editableWidget.getColumnId(), COL_RULE_CONDITION)) {
      conditionWidget = (ListBox) widget;
      conditionWidget.addChangeHandler(new ChangeHandler() {
        @Override
        public void onChange(ChangeEvent arg0) {
          adjustConditionOptions(null);
        }
      });
    } else if (BeeUtils.same(editableWidget.getColumnId(), COL_RULE_ACTION)) {
      actionWidget = (ListBox) widget;
      actionWidget.addChangeHandler(new ChangeHandler() {
        @Override
        public void onChange(ChangeEvent arg0) {
          adjustActionOptions(null);
        }
      });
    }
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, COL_RULE_CONDITION_OPTIONS)) {
      conditionOptions = (InputText) widget;

    } else if (BeeUtils.same(name, COL_RULE_ACTION_OPTIONS)) {
      actionOptions = (InputText) widget;

    } else if (BeeUtils.same(name, COL_FOLDER) && widget instanceof UnboundSelector) {
      folder = (UnboundSelector) widget;
      folder.addSelectorHandler(this);

    } else if (widget instanceof InputArea) {
      reply = (InputArea) widget;
    }
  }

  @Override
  public boolean beforeAction(Action formAction, Presenter presenter) {
    if (EnumSet.of(Action.SAVE, Action.CLOSE).contains(formAction)) {
      FormView form = getFormView();
      String value;

      RuleCondition condition = EnumUtils.getEnumByIndex(RuleCondition.class,
          conditionWidget == null ? null : conditionWidget.getSelectedIndex());

      if (condition != null) {
        if (condition == RuleCondition.ALL) {
          value = null;
        } else {
          value = conditionOptions.getValue();

          if (formAction == Action.SAVE && BeeUtils.isEmpty(value)) {
            UiHelper.focus(conditionOptions);
            notifyRequired(null);
            return false;
          }
        }
        form.getActiveRow().setValue(form.getDataIndex(COL_RULE_CONDITION_OPTIONS), value);
      }
      RuleAction action = EnumUtils.getEnumByIndex(RuleAction.class,
          actionWidget == null ? null : actionWidget.getSelectedIndex());

      if (action != null) {
        Editor editor = null;

        switch (action) {
          case COPY:
          case MOVE:
            editor = folder;
            break;

          case DELETE:
          case FLAG:
          case READ:
            break;

          case FORWARD:
            editor = actionOptions;
            break;

          case REPLY:
            editor = reply;
            break;
        }
        if (editor == null) {
          value = null;
        } else {
          value = editor.getValue();

          if (formAction == Action.SAVE && BeeUtils.isEmpty(value)) {
            UiHelper.focus(editor.asWidget());
            notifyRequired(null);
            return false;
          }
        }
        form.getActiveRow().setValue(form.getDataIndex(COL_RULE_ACTION_OPTIONS), value);
      }
    }
    return super.beforeAction(formAction, presenter);
  }

  @Override
  public FormInterceptor getInstance() {
    return new RuleForm();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isOpened()) {
      event.getSelector().setAdditionalFilter(Filter.equals(COL_ACCOUNT,
          UiHelper.getFormRowId(getGridView().asWidget())));
    }
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    init(row);
    return super.onStartEdit(form, row, focusCommand);
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    init(newRow);
    super.onStartNewRow(form, oldRow, newRow);
  }

  private void adjustActionOptions(IsRow row) {
    for (Widget widget : new Widget[] {actionOptions, folder, reply}) {
      StyleUtils.setVisible(widget, false);
    }
    boolean init = row != null;
    String value = null;
    Integer idx;

    if (init) {
      value = row.getString(getFormView().getDataIndex(COL_RULE_ACTION_OPTIONS));
      idx = row.getInteger(getFormView().getDataIndex(COL_RULE_ACTION));
    } else {
      idx = actionWidget.getSelectedIndex();
    }
    RuleAction action = EnumUtils.getEnumByIndex(RuleAction.class, idx);

    if (action != null) {
      switch (action) {
        case COPY:
        case MOVE:
          if (init) {
            folder.setValue(BeeUtils.toLongOrNull(value), false);
          }
          StyleUtils.setVisible(folder, true);
          break;

        case DELETE:
        case FLAG:
        case READ:
          break;

        case FORWARD:
          if (init) {
            actionOptions.setValue(value);
          }
          StyleUtils.setVisible(actionOptions, true);
          break;

        case REPLY:
          if (init) {
            reply.setValue(value);
          }
          StyleUtils.setVisible(reply, true);
          break;
      }
    }
  }

  private void adjustConditionOptions(IsRow row) {
    FormView form = getFormView();
    boolean init = row != null;
    Integer idx;

    if (init) {
      idx = row.getInteger(form.getDataIndex(COL_RULE_CONDITION));
    } else {
      idx = conditionWidget.getSelectedIndex();
    }
    RuleCondition condition = EnumUtils.getEnumByIndex(RuleCondition.class, idx);

    boolean visible = condition != null && condition != RuleCondition.ALL;

    if (visible && init) {
      conditionOptions.setValue(row.getString(form.getDataIndex(COL_RULE_CONDITION_OPTIONS)));
    }
    StyleUtils.setVisible(conditionOptions, visible);
  }

  private void init(IsRow row) {
    folder.setValue(null, false);

    for (Editor editor : new Editor[] {conditionOptions, actionOptions, reply}) {
      editor.setValue(null);
    }
    adjustConditionOptions(row);
    adjustActionOptions(row);
  }
}
