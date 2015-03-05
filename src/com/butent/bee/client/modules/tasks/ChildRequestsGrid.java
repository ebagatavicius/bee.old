package com.butent.bee.client.modules.tasks;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.ui.Action;

public class ChildRequestsGrid extends AbstractGridInterceptor {

  @Override
  public GridInterceptor getInstance() {
    return new ChildRequestsGrid();
  }

  @Override
  public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
    if (copy) {
      return true;
    }

    presenter.getGridView().ensureRelId(new IdCallback() {

      @Override
      public void onSuccess(Long relId) {
        DataInfo childRequestDataInfo = Data.getDataInfo(presenter.getViewName());
        BeeRow childRequestRow = RowFactory.createEmptyRow(childRequestDataInfo, true);
        String relColumn = presenter.getGridView().getRelColumn();
        FormView parentForm = ViewHelper.getForm(presenter.getMainView());

        if (parentForm != null) {
          DataInfo parentFormDataInfo = Data.getDataInfo(parentForm.getViewName());
          IsRow parentFormRow = parentForm.getActiveRow();
          RelationUtils.updateRow(childRequestDataInfo, relColumn, childRequestRow,
              parentFormDataInfo, parentFormRow, true);
        }
        RowFactory.createRow(childRequestDataInfo, childRequestRow, new RowCallback() {

          @Override
          public void onSuccess(BeeRow result) {
            presenter.handleAction(Action.REFRESH);
          }
        });
      }
    });

    return false;
  }
}
