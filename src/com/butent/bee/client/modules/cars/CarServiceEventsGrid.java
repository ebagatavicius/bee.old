package com.butent.bee.client.modules.cars;

import com.google.common.collect.ImmutableMap;

import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.COL_TRADE_CUSTOMER;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.view.DataInfo;

import java.util.Objects;

public class CarServiceEventsGrid extends AbstractGridInterceptor {
  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
    FormView parentForm = ViewHelper.getForm(getGridView());

    if (Objects.nonNull(parentForm)) {
      DataInfo sourceInfo = Data.getDataInfo(parentForm.getViewName());
      IsRow row = parentForm.getActiveRow();
      DataInfo targetInfo = Data.getDataInfo(getViewName());

      RelationUtils.updateRow(targetInfo, COL_SERVICE_ORDER, newRow, sourceInfo, row, true);

      ImmutableMap.of(COL_TRADE_CUSTOMER, COL_COMPANY, COL_TRADE_CUSTOMER + COL_PERSON,
          COL_COMPANY_PERSON, COL_CAR, COL_CAR).forEach((s, t) ->
          RelationUtils.copyWithDescendants(sourceInfo, s, row, targetInfo, t, newRow));
    }
    return super.onStartNewRow(gridView, oldRow, newRow);
  }

  @Override
  public GridInterceptor getInstance() {
    return new CarServiceEventsGrid();
  }
}
