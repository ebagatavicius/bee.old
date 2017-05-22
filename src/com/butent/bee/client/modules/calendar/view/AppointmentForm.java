package com.butent.bee.client.modules.calendar.view;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_USER;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.projects.ProjectConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.composite.Relations;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;

public class AppointmentForm extends AbstractFormInterceptor {

  private DataSelector prjSelector;

  private class RelationsHandler implements SelectorEvent.Handler {

    @Override
    public void onDataSelector(SelectorEvent event) {
      final String viewName = event.getRelatedViewName();

      if (event.isNewRow()) {
        if (Objects.equals(viewName, VIEW_TASKS)) {
          event.consume();

          final String formName = event.getNewRowFormName();
          final BeeRow row = event.getNewRow();
          final DataSelector selector = event.getSelector();

          Long projectId = getLongValue(COL_PROJECT);
          if (DataUtils.isId(projectId)) {
            String project = getStringValue(ALS_PROJECT_NAME);

            Data.setValue(TaskConstants.VIEW_TASKS, row, COL_PROJECT, projectId);
            Data.setValue(TaskConstants.VIEW_TASKS, row, ALS_PROJECT_NAME, project);
          }

          Long companyId = getLongValue(COL_COMPANY);
          if (DataUtils.isId(companyId)) {
            String company = getStringValue(ALS_COMPANY_NAME);

            Data.setValue(VIEW_TASKS, row, COL_COMPANY, companyId);
            Data.setValue(VIEW_TASKS, row, ALS_COMPANY_NAME, company);
          }
          RowFactory.createRelatedRow(formName, row, selector, null);
        }
      } else {
        Filter filter = null;
        Long user = BeeKeeper.getUser().getUserId();

        if (Objects.equals(viewName, VIEW_TASKS)) {
          filter = Filter.or(Filter.equals(COL_EXECUTOR, user), Filter.equals(COL_OWNER, user),
              Filter.in(COL_TASK_ID, VIEW_TASK_USERS, COL_TASK, Filter.equals(COL_USER, user)));
        } else if (Objects.equals(viewName, VIEW_PROJECTS)) {
          filter = Filter.in(Data.getIdColumn(VIEW_PROJECTS), VIEW_PROJECT_USERS, COL_PROJECT,
              Filter.equals(COL_USER, user));
        }

        event.getSelector().setAdditionalFilter(filter);
      }
    }
  }

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    if (BeeUtils.same(editableWidget.getColumnId(), COL_PROJECT)) {
      prjSelector = (DataSelector) widget;
      prjSelector.addSelectorHandler(event -> {
        Filter filter = Filter.in(Data.getIdColumn(VIEW_PROJECTS), VIEW_PROJECT_USERS, COL_PROJECT,
            Filter.equals(COL_USER, BeeKeeper.getUser().getUserId()));

        event.getSelector().setAdditionalFilter(filter);
      });
    }

    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (widget instanceof Relations) {
      ((Relations) widget).setSelectorHandler(new RelationsHandler());
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public FormInterceptor getInstance() {
    return new AppointmentForm();
  }

  public DataSelector getProjectSelector() {
    return prjSelector;
  }
}