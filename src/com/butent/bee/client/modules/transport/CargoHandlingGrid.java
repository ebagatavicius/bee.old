package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.ParentRowCreator;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.grid.interceptor.ParentRowRefreshGrid;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;
import java.util.Set;

public class CargoHandlingGrid extends ParentRowRefreshGrid {

  @Override
  public GridInterceptor getInstance() {
    return new CargoHandlingGrid();
  }

  @Override
  public boolean previewModify(Set<Long> rowIds) {
    if (super.previewModify(rowIds)) {
      FormView parentForm = ViewHelper.getForm(getGridView());

      if (Objects.nonNull(parentForm)) {
        String table = null;

        switch (parentForm.getViewName()) {
          case VIEW_ORDER_CARGO:
            table = TBL_CARGO_TRIPS;
            break;
          case VIEW_ASSESSMENTS:
            table = TBL_ASSESSMENT_FORWARDERS;
            break;
        }
        if (!BeeUtils.isEmpty(table)) {
          Data.refreshLocal(table);
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
    if (gridView.isEmpty()) {
      IsRow parentRow = ViewHelper.getFormRow(gridView);

      if (DataUtils.isNewRow(parentRow)) {
        FormView parentForm = ViewHelper.getForm(gridView);

        if (parentForm.getViewPresenter() instanceof ParentRowCreator) {
          ((ParentRowCreator) parentForm.getViewPresenter()).createParentRow(parentForm,
              newParentRow -> {
                fillValuesByParameters(newRow, newParentRow);
                FormView gridForm = gridView.getActiveForm();

                if (Objects.nonNull(gridForm)) {
                  gridForm.refresh();
                }
              });
        }
      } else {
        fillValuesByParameters(newRow, parentRow);
      }
    }
    return super.onStartNewRow(gridView, oldRow, newRow, copy);
  }

  private void fillValuesByParameters(IsRow newRow, IsRow parentRow) {
    if (parentRow != null && parentRow.getProperties() != null) {
      String prefix = BeeUtils.removePrefix(getViewName(), COL_CARGO);

      parentRow.getProperties().forEach((name, value) -> {
        if (BeeUtils.isPrefix(name, prefix)) {
          int colIndex = getDataIndex(BeeUtils.removePrefix(name, prefix));

          if (!BeeConst.isUndef(colIndex)) {
            newRow.setValue(colIndex, value);
          }
        }
      });
    }
  }
}
