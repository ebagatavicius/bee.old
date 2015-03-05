package com.butent.bee.client.modules.tasks;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskType;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

class ChildTasksGrid extends TasksGrid {

  ChildTasksGrid() {
    super(TaskType.RELATED, null);
  }

  @Override
  public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
    if (copy) {
      return true;
    }

    presenter.getGridView().ensureRelId(new IdCallback() {
      @Override
      public void onSuccess(Long relId) {
        DataInfo childTaskDataInfo = Data.getDataInfo(presenter.getViewName());

        BeeRow childTaskRow = RowFactory.createEmptyRow(childTaskDataInfo, true);
        String relColumn = presenter.getGridView().getRelColumn();

        FormView parentForm = ViewHelper.getForm(presenter.getMainView());
        if (parentForm != null) {
          DataInfo parentFormDataInfo = Data.getDataInfo(parentForm.getViewName());
          IsRow parentFormRow = parentForm.getActiveRow();

          RelationUtils.updateRow(childTaskDataInfo, relColumn, childTaskRow,
              parentFormDataInfo, parentFormRow, true);

          if (BeeUtils.same(parentForm.getViewName(), ProjectConstants.VIEW_PROJECTS)) {
            fillProjectData(childTaskDataInfo, childTaskRow, parentFormDataInfo, parentFormRow);
          }

          if (BeeUtils.same(parentForm.getViewName(), ProjectConstants.VIEW_PROJECT_STAGES)) {
            fillProjectStageData(childTaskDataInfo, childTaskRow, parentFormDataInfo,
                parentFormRow);
            fillProjectData(childTaskDataInfo, childTaskRow, parentFormDataInfo, parentFormRow);
          }
        }

        RowFactory.createRow(childTaskDataInfo, childTaskRow, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            presenter.handleAction(Action.REFRESH);
          }
        });
      }
    });

    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return new ChildTasksGrid();
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    return true;
  }

  private static void fillProjectData(DataInfo taskData, IsRow taskRow, DataInfo parentFormData,
      IsRow parentRowData) {
    if (taskData == null && taskRow == null && parentFormData == null && parentRowData == null) {
      return;
    }

    /* Fill company info */
    int idxTaskCompany = taskData.getColumnIndex(ClassifierConstants.COL_COMPANY);
    int idxProjectCompany = parentFormData.getColumnIndex(ClassifierConstants.COL_COMPANY);

    if (BeeUtils.isNegative(idxTaskCompany) && BeeUtils.isNegative(idxProjectCompany)) {
      return;
    }

    taskRow.setValue(idxTaskCompany, parentRowData.getValue(idxProjectCompany));

    int idxTaskCompanyName = taskData.getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME);
    int idxProjectCompanyName = parentFormData.getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME);

    if (!BeeUtils.isNegative(idxTaskCompanyName) && !BeeUtils.isNegative(idxProjectCompanyName)) {
      taskRow.setValue(idxTaskCompanyName, parentRowData.getValue(idxProjectCompanyName));
    }
  }

  private static void fillProjectStageData(DataInfo taskData, IsRow taskRow,
      DataInfo parentFormData, IsRow parentRowData) {
    if (taskData == null && taskRow == null && parentFormData == null && parentRowData == null) {
      return;
    }

    /* Fill project info */
    int idxTaskProject = taskData.getColumnIndex(ProjectConstants.COL_PROJECT);
    int idxStageProject = parentFormData.getColumnIndex(ProjectConstants.COL_PROJECT);

    if (BeeUtils.isNegative(idxTaskProject) && BeeUtils.isNegative(idxStageProject)) {
      return;
    }

    taskRow.setValue(idxTaskProject, parentRowData.getValue(idxStageProject));
  }
}
