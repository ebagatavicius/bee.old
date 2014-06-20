package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.event.logical.RowActionEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;

class RelatedRecurringTasksGrid extends AbstractGridInterceptor {

  RelatedRecurringTasksGrid() {
  }

  @Override
  public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
    if (copy) {
      String caption = presenter.getActiveRow().getString(getDataIndex(COL_SUMMARY));
      List<String> messages = Lists.newArrayList(Localized.getConstants().crmRTCopyQuestion());

      Global.confirm(caption, Icon.QUESTION, messages, Localized.getConstants().actionCopy(),
          Localized.getConstants().actionCancel(), new ConfirmationCallback() {
            @Override
            public void onConfirm() {
              if (presenter.getActiveRow() == null) {
                return;
              }
              Long rtId = getRecurringTaskId(presenter.getActiveRow());

              ParameterList params = TasksKeeper.createArgs(SVC_RT_COPY);
              params.addQueryItem(VAR_RT_ID, rtId);

              BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
                @Override
                public void onResponse(ResponseObject response) {
                  if (Queries.checkRowResponse(SVC_RT_COPY, VIEW_RECURRING_TASKS, response)) {
                    BeeRow row = BeeRow.restore(response.getResponseAsString());
                    RowInsertEvent.fire(BeeKeeper.getBus(), VIEW_RECURRING_TASKS, row, null);

                    presenter.handleAction(Action.REFRESH);
                    openRecurringTask(row.getId());
                  }
                }
              });
            }
          });

    } else {
      presenter.getGridView().ensureRelId(new IdCallback() {
        @Override
        public void onSuccess(Long relId) {
          DataInfo dataInfo = Data.getDataInfo(VIEW_RECURRING_TASKS);

          BeeRow row = RowFactory.createEmptyRow(dataInfo, true);
          RowActionEvent.fireCreateRow(VIEW_RECURRING_TASKS, row, presenter.getWidget().getId());

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
    }

    return false;
  }

  @Override
  public DeleteMode beforeDeleteRow(final GridPresenter presenter, final IsRow row) {
    final Long rtId = getRecurringTaskId(row);

    if (DataUtils.isId(rtId)) {
      Queries.deleteRow(VIEW_RECURRING_TASKS, rtId, new Queries.IntCallback() {
        @Override
        public void onSuccess(Integer result) {
          RowDeleteEvent.fire(BeeKeeper.getBus(), VIEW_RECURRING_TASKS, rtId);
          presenter.handleAction(Action.REFRESH);
        }
      });
    }

    return DeleteMode.CANCEL;
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {
    return (defMode == DeleteMode.MULTI) ? DeleteMode.SINGLE : defMode;
  }

  @Override
  public List<String> getDeleteRowMessage(IsRow row) {
    String m1 = BeeUtils.joinWords(Localized.getConstants().crmRecurringTask(),
        getRecurringTaskId(row));
    String m2 = Localized.getConstants().crmTaskDeleteQuestion();

    return Lists.newArrayList(m1, m2);
  }

  @Override
  public GridInterceptor getInstance() {
    return new RelatedRecurringTasksGrid();
  }

  @Override
  public boolean isRowEditable(IsRow row) {
    return row != null && BeeKeeper.getUser().is(row.getLong(getDataIndex(COL_OWNER)));
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    event.consume();

    if (event.getRowValue() != null) {
      openRecurringTask(getRecurringTaskId(event.getRowValue()));
    }
  }

  private void openRecurringTask(Long rtId) {
    if (DataUtils.isId(rtId)) {
      RowEditor.open(VIEW_RECURRING_TASKS, rtId, Opener.MODAL, new RowCallback() {
        @Override
        public void onSuccess(BeeRow result) {
          getGridPresenter().handleAction(Action.REFRESH);
        }
      });
    }
  }

  private Long getRecurringTaskId(IsRow row) {
    return (row == null) ? null : row.getLong(getDataIndex(COL_RECURRING_TASK));
  }
}
