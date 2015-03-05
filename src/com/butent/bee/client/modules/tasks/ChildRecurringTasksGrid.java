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
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

class ChildRecurringTasksGrid extends RecurringTasksGrid {

  ChildRecurringTasksGrid() {
  }

  @Override
  public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
    if (copy) {
      return true;
    }

    presenter.getGridView().ensureRelId(new IdCallback() {

      @Override
      public void onSuccess(Long relId) {
        fillRelations(presenter);
      }

    });
    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return new ChildRecurringTasksGrid();
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    return true;
  }

  private static void createRow(final GridPresenter presenter, DataInfo rowData, BeeRow row) {
    RowFactory.createRow(rowData, row, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        presenter.handleAction(Action.REFRESH);
      }
    });
  }

  private static void fillProjectData(DataInfo childDataInfo, IsRow childRow,
      DataInfo parentDataInfo, IsRow parentRowData) {
    if (childDataInfo == null && childRow == null && parentDataInfo == null
        && parentRowData == null) {
      return;
    }

    /* Fill company info */
    int idxChildCompany = childDataInfo.getColumnIndex(ClassifierConstants.COL_COMPANY);
    int idxProjectCompany = parentDataInfo.getColumnIndex(ClassifierConstants.COL_COMPANY);

    if (BeeUtils.isNegative(idxChildCompany) && BeeUtils.isNegative(idxProjectCompany)) {
      return;
    }

    childRow.setValue(idxChildCompany, parentRowData.getValue(idxProjectCompany));

    int idxChildCompanyName = childDataInfo.getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME);
    int idxProjectCompanyName = parentDataInfo.getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME);

    if (!BeeUtils.isNegative(idxChildCompanyName) && !BeeUtils.isNegative(idxProjectCompanyName)) {
      childRow.setValue(idxChildCompanyName, parentRowData.getValue(idxProjectCompanyName));
    }

    int idxChildProjectStartDate =
        childDataInfo.getColumnIndex(ProjectConstants.ALS_PROJECT_START_DATE);
    int idxProjectStartDate =
        parentDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_START_DATE);

    if (!(BeeUtils.isNegative(idxChildProjectStartDate)
    && BeeUtils.isNegative(idxProjectStartDate))) {
      childRow.setValue(idxChildProjectStartDate, parentRowData.getValue(idxProjectStartDate));
    }

    int idxChildProjectEnd = childDataInfo.getColumnIndex(ProjectConstants.ALS_PROJECT_END_DATE);
    int idxProjectEndDate = parentDataInfo.getColumnIndex(ProjectConstants.COL_PROJECT_END_DATE);

    if (!(BeeUtils.isNegative(idxChildProjectEnd) && BeeUtils.isNegative(idxProjectEndDate))) {
      childRow.setValue(idxChildProjectEnd, parentRowData.getValue(idxProjectEndDate));
    }

  }

  private static void fillRelations(GridPresenter presenter) {
    DataInfo childDataInfo = Data.getDataInfo(presenter.getViewName());
    BeeRow childRow = RowFactory.createEmptyRow(childDataInfo, true);
    FormView parentForm = ViewHelper.getForm(presenter.getMainView());

    String relColumn = presenter.getGridView().getRelColumn();

    if (parentForm == null) {
      createRow(presenter, childDataInfo, childRow);
      return;
    }

    DataInfo parentDataInfo = Data.getDataInfo(parentForm.getViewName());
    IsRow parentRow = parentForm.getActiveRow();

    String parentViewName = parentDataInfo.getViewName();

    RelationUtils.updateRow(childDataInfo, relColumn, childRow, parentDataInfo, parentRow, true);

    if (BeeUtils.same(parentViewName, ProjectConstants.VIEW_PROJECTS)) {
      fillProjectData(childDataInfo, childRow, parentDataInfo, parentRow);
    }

    createRow(presenter, childDataInfo, childRow);
  }
}
