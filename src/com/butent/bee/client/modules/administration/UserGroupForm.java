package com.butent.bee.client.modules.administration;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.RadioGroup;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.administration.AdministrationConstants.UserGroupVisibility;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

final class UserGroupForm extends AbstractFormInterceptor {

  private RadioGroup visibility;
  private DataSelector executor;

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    if (BeeUtils.same(editableWidget.getColumnId(),
        AdministrationConstants.COL_USER_GROUP_SETTINGS_VISIBILITY)
        && widget instanceof RadioGroup) {
      visibility = (RadioGroup) widget;
      visibility.addEditChangeHandler(new EditChangeHandler() {

        @Override
        public void onValueChange(ValueChangeEvent<String> value) {
          if (executor == null) {
            return;
          }

          if (UserGroupVisibility.PRIVATE.compareTo(EnumUtils.getEnumByIndex(
              UserGroupVisibility.class, value.getValue())) == 0) {
            executor.setEnabled(false);
          } else {
            executor.setEnabled(true);
          }
        }

        @Override
        public void onKeyDown(KeyDownEvent arg0) {
        }
      });
    } else if (BeeUtils.same(editableWidget.getColumnId(), TaskConstants.COL_TASK_EXECUTOR)
        && widget instanceof DataSelector) {
      executor = (DataSelector) widget;
    }

    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    if (executor == null) {
      return;
    }

    if (UserGroupVisibility.PRIVATE.ordinal() == BeeUtils.unbox(row.getInteger(form
        .getDataIndex(AdministrationConstants.COL_USER_GROUP_SETTINGS_VISIBILITY)))) {
      executor.setEnabled(false);
    } else {
      executor.setEnabled(true);
    }
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action.equals(Action.SAVE) && getActiveRow() != null && getFormView() != null) {
      IsRow row = getActiveRow();
      FormView form = getFormView();

      if (UserGroupVisibility.PRIVATE.ordinal() == BeeUtils.unbox(row.getInteger(form
          .getDataIndex(AdministrationConstants.COL_USER_GROUP_SETTINGS_VISIBILITY)))) {
        row.setValue(form.getDataIndex(TaskConstants.COL_TASK_EXECUTOR), (Long) null);
      }
    }
    return super.beforeAction(action, presenter);
  }

  @Override
  public FormInterceptor getInstance() {
    return new UserGroupForm();
  }

}
