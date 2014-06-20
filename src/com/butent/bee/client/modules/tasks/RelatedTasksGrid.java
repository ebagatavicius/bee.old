package com.butent.bee.client.modules.tasks;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.event.logical.RowActionEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.tasks.TaskType;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

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
        RowActionEvent.fireCreateRow(VIEW_TASKS, row, presenter.getWidget().getId());

        String relColumn = presenter.getGridView().getRelColumn();
        String property = TaskUtils.translateRelationToTaskProperty(relColumn);

        if (!BeeUtils.isEmpty(property) && BeeUtils.isEmpty(row.getProperty(property))) {
          row.setProperty(property, relId.toString());
        }

        RowFactory.createRow(dataInfo, row, new RowCallback() {
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
}
