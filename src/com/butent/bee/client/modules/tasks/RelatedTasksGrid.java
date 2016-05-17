package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.event.logical.RowActionEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.service.ServiceConstants;
import com.butent.bee.shared.modules.tasks.TaskType;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class RelatedTasksGrid extends TasksGrid {

  private static void openTask(Long id) {
    if (DataUtils.isId(id)) {
      RowEditor.open(VIEW_TASKS, id, Opener.MODAL);
    }
  }

  RelatedTasksGrid() {
    super(TaskType.RELATED, null);
  }

  @Override
  public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
    presenter.getGridView().ensureRelId(new IdCallback() {
      @Override
      public void onSuccess(Long relId) {
        DataInfo dataInfo = Data.getDataInfo(VIEW_TASKS);

        BeeRow row = RowFactory.createEmptyRow(dataInfo, true);
        RowActionEvent.fireCreateRow(VIEW_TASKS, row, presenter.getMainView().getId());

        String relColumn = presenter.getGridView().getRelColumn();
        String property = TaskUtils.translateRelationToTaskProperty(relColumn);

        if (!BeeUtils.isEmpty(property) && BeeUtils.isEmpty(row.getProperty(property))) {
          row.setProperty(property, relId.toString());
        }

        FormView parentForm = ViewHelper.getForm(presenter.getMainView());

        if (parentForm != null) {
          fillFormData(parentForm, dataInfo, row);
        }

        RowFactory.createRow(dataInfo, row, Modality.ENABLED, new RowCallback() {
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
  public DeleteMode beforeDeleteRow(final GridPresenter presenter, final IsRow row) {
    final Long taskId = getTaskId(row);

    if (DataUtils.isId(taskId)) {
      Queries.deleteRow(VIEW_TASKS, taskId, new Queries.IntCallback() {
        @Override
        public void onSuccess(Integer result) {
          RowDeleteEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, taskId);
          presenter.handleAction(Action.REFRESH);
        }
      });
    }

    return DeleteMode.CANCEL;
  }

  @Override
  public GridInterceptor getInstance() {
    return new RelatedTasksGrid();
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    return true;
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    if (!maybeEditStar(event)) {
      event.consume();

      int index = getDataIndex(COL_TASK);
      if (!BeeConst.isUndef(index) && event.getRowValue() != null) {
        openTask(event.getRowValue().getLong(index));
      }
    }
  }

  @Override
  protected void afterCopyAsRecurringTask() {
    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_RELATED_RECURRING_TASKS);
  }

  @Override
  protected void afterCopyTask() {
    if (getGridPresenter() != null) {
      getGridPresenter().handleAction(Action.REFRESH);
    }
  }

  @Override
  protected Long getTaskId(IsRow row) {
    return (row == null) ? null : row.getLong(getDataIndex(COL_TASK));
  }

  private static void fillFormData(FormView parentForm, DataInfo gridData, BeeRow gridRow) {
    IsRow formRow = parentForm.getActiveRow();

    if (formRow == null) {
      return;
    }

    if (!BeeUtils.same(parentForm.getViewName(), ServiceConstants.VIEW_SERVICE_OBJECTS)) {
      return;
    }

    @SuppressWarnings("unchecked")
    List<Pair<String, String>> copyCols = Lists.newArrayList(
        Pair.of(ClassifierConstants.COL_COMPANY, ServiceConstants.COL_SERVICE_CUSTOMER),
        Pair.of(ClassifierConstants.ALS_COMPANY_NAME, ServiceConstants.ALS_SERVICE_CUSTOMER_NAME),
        Pair.of(ProjectConstants.ALS_COMPANY_TYPE_NAME,
            ServiceConstants.ALS_SERVICE_CUSTOMER_TYPE_NAME),
        Pair.of(ProjectConstants.COL_PROJECT, ProjectConstants.COL_PROJECT),
        Pair.of(ProjectConstants.ALS_PROJECT_NAME, ProjectConstants.ALS_PROJECT_NAME)

        );

    for (Pair<String, String> col : copyCols) {
      if (BeeConst.isUndef(parentForm.getDataIndex(col.getB()))) {
        continue;
      }

      gridRow.setValue(gridData.getColumnIndex(col.getA()),
          formRow.getValue(parentForm.getDataIndex(col.getB())));
    }
  }

}
