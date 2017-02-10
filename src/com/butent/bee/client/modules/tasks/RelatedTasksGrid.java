package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;

import static com.butent.bee.client.modules.mail.Relations.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
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
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.service.ServiceConstants;
import com.butent.bee.shared.modules.tasks.TaskType;
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
    presenter.getGridView().ensureRelId(relId -> {
      DataInfo dataInfo = Data.getDataInfo(VIEW_TASKS);

      BeeRow row = RowFactory.createEmptyRow(dataInfo, true);
      RowActionEvent.fireCreateRow(VIEW_TASKS, row, presenter.getMainView().getId());

      String relViewName = presenter.getGridView().getViewName();

      if (!BeeUtils.isEmpty(relViewName) && BeeUtils.isEmpty(row.getProperty(PFX_RELATED
        + relViewName))) {
        row.setProperty(PFX_RELATED + relViewName, DataUtils.buildIdList(relId));
      }

      RowFactory.createRow(dataInfo, row, Modality.ENABLED, new RowCallback() {
        @Override
        public void onSuccess(BeeRow result) {
          if (BeeUtils.same(relViewName, ServiceConstants.COL_SERVICE_MAINTENANCE)
              && presenter.getMainView() != null) {
            FormView parentForm = ViewHelper.getForm(presenter.getMainView());

            if (parentForm != null && parentForm.getActiveRow() != null) {
              int objectColumnIndex = Data.getColumnIndex(parentForm.getViewName(),
                  ServiceConstants.COL_SERVICE_OBJECT);
              Long objectId = parentForm.getActiveRow().getLong(objectColumnIndex);

              if (DataUtils.isId(objectId)) {
                List<BeeColumn> columns =
                    Data.getColumns(AdministrationConstants.VIEW_RELATIONS,
                        Lists.newArrayList(ServiceConstants.COL_SERVICE_OBJECT,
                            COL_TASK));
                List<String> value = Lists.newArrayList(BeeUtils.toString(objectId),
                    BeeUtils.toString(result.getId()));

                Queries.insert(AdministrationConstants.VIEW_RELATIONS, columns, value);
              }
            }
          }
          presenter.handleAction(Action.REFRESH);
        }
      });
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
}
