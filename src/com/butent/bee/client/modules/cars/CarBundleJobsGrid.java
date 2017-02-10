package com.butent.bee.client.modules.cars;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_CURRENCY;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.COL_MODEL;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CarBundleJobsGrid extends AbstractGridInterceptor implements SelectorEvent.Handler {
  @Override
  public void afterCreateEditor(String source, Editor editor, boolean embedded) {
    if (Objects.equals(source, COL_JOB) && editor instanceof DataSelector) {
      ((DataSelector) editor).addSelectorHandler(this);
    }
    super.afterCreateEditor(source, editor, embedded);
  }

  @Override
  public GridInterceptor getInstance() {
    return new CarBundleJobsGrid();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isOpened()) {
      FormView parentForm = ViewHelper.getForm(getGridView());
      if (Objects.isNull(parentForm)) {
        return;
      }
      Filter filter = Filter.isNull(COL_MODEL);

      if (DataUtils.isId(parentForm.getLongValue(COL_MODEL))) {
        filter = Filter.or(filter, Filter.equals(COL_MODEL, parentForm.getLongValue(COL_MODEL)));
      }
      event.getSelector().setAdditionalFilter(filter);

    } else if (event.isChangePending()) {
      String srcView = event.getRelatedViewName();
      IsRow srcRow = event.getRelatedRow();

      if (srcRow == null) {
        return;
      }
      DataView dataView = ViewHelper.getDataView(event.getSelector());
      if (dataView == null || BeeUtils.isEmpty(dataView.getViewName())) {
        return;
      }
      IsRow target = dataView.getActiveRow();
      if (target == null) {
        return;
      }
      Map<String, String> values = new HashMap<>();

      values.put(COL_DURATION, BeeUtils.nvl(Data.getString(srcView, srcRow,
          COL_MODEL + COL_DURATION), Data.getString(srcView, srcRow, COL_DURATION)));

      values.put(COL_PRICE, BeeUtils.nvl(Data.getString(srcView, srcRow, COL_MODEL + COL_PRICE),
          Data.getString(srcView, srcRow, COL_PRICE)));

      values.put(COL_CURRENCY, BeeUtils.nvl(Data.getString(srcView, srcRow,
          COL_MODEL + COL_CURRENCY), Data.getString(srcView, srcRow, COL_CURRENCY)));

      values.forEach((col, val) -> {
        int colIndex = dataView.getDataIndex(col);

        if (dataView.isFlushable()) {
          target.setValue(colIndex, val);
          dataView.refreshBySource(col);
        } else {
          target.preliminaryUpdate(colIndex, val);
        }
      });
    }
  }
}
