package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class RecurringTasksGrid extends AbstractGridInterceptor {

  RecurringTasksGrid() {
  }

  @Override
  public boolean beforeAction(Action action, final GridPresenter presenter) {
    if (action == Action.COPY) {
      if (presenter.getMainView().isEnabled() && presenter.getActiveRow() != null) {
        String caption = presenter.getActiveRow().getString(getDataIndex(COL_SUMMARY));
        List<String> messages = Lists.newArrayList(Localized.getConstants().crmRTCopyQuestion());

        Global.confirm(caption, Icon.QUESTION, messages, Localized.getConstants().actionCopy(),
            Localized.getConstants().actionCancel(), new ConfirmationCallback() {
              @Override
              public void onConfirm() {
                if (presenter.getActiveRow() == null) {
                  return;
                }
                long rtId = presenter.getActiveRow().getId();

                ParameterList params = TasksKeeper.createArgs(SVC_RT_COPY);
                params.addQueryItem(VAR_RT_ID, rtId);

                BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
                  @Override
                  public void onResponse(ResponseObject response) {
                    if (Queries.checkRowResponse(SVC_RT_COPY, VIEW_RECURRING_TASKS, response)) {
                      BeeRow row = BeeRow.restore(response.getResponseAsString());
                      GridView gridView = presenter.getGridView();

                      if (gridView != null && gridView.asWidget().isAttached()) {
                        gridView.getGrid().insertRow(row, false);

                        if (DomUtils.isVisible(gridView.getGrid())) {
                          gridView.onEditStart(new EditStartEvent(row, null, null,
                              EditStartEvent.CLICK, gridView.isReadOnly()));
                        }
                      }

                      RowInsertEvent.fire(BeeKeeper.getBus(), VIEW_RECURRING_TASKS, row,
                          (gridView == null) ? null : gridView.getId());
                    }
                  }
                });
              }
            });
      }

      return false;

    } else {
      return super.beforeAction(action, presenter);
    }
  }

  @Override
  public List<String> getDeleteRowMessage(IsRow row) {
    String m1 = BeeUtils.joinWords(Localized.getConstants().crmRecurringTask(), row.getId());
    String m2 = Localized.getConstants().crmTaskDeleteQuestion();

    return Lists.newArrayList(m1, m2);
  }

  @Override
  public GridInterceptor getInstance() {
    return new RecurringTasksGrid();
  }

  @Override
  public boolean onRowInsert(RowInsertEvent event) {
    if (BeeKeeper.getUser().is(event.getRow().getLong(getDataIndex(COL_OWNER)))) {
      getGridPresenter().refresh(false);
      return false;

    } else {
      return super.onRowInsert(event);
    }
  }
}
