package com.butent.bee.client.modules.cars;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_CURRENCY;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.modules.classifiers.ClassifierKeeper;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.ParentRowRefreshGrid;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CarServiceItemsGrid extends ParentRowRefreshGrid implements SelectorEvent.Handler {

  @Override
  public void afterCreateEditor(String source, Editor editor, boolean embedded) {
    if (Objects.equals(source, COL_ITEM) && editor instanceof DataSelector) {
      ((DataSelector) editor).addSelectorHandler(this);
    }
    super.afterCreateEditor(source, editor, embedded);
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    FormView parentForm = ViewHelper.getForm(getGridView());
    if (Objects.isNull(parentForm)) {
      return;
    }
    if (event.isChangePending()) {
      DataView dataView = ViewHelper.getDataView(event.getSelector());
      if (dataView == null || BeeUtils.isEmpty(dataView.getViewName()) || !dataView.isFlushable()) {
        return;
      }
      IsRow target = dataView.getActiveRow();
      if (target == null) {
        return;
      }
      Map<String, String> values = new HashMap<>();

      values.put(COL_PRICE, null);
      values.put(COL_TRADE_DOCUMENT_ITEM_DISCOUNT, null);
      values.put(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT, null);

      Map<String, String> options = new HashMap<>();
      options.put(COL_DISCOUNT_COMPANY, parentForm.getStringValue(COL_CUSTOMER));
      options.put(Service.VAR_TIME, parentForm.getStringValue(COL_DATE));
      options.put(COL_DISCOUNT_CURRENCY, parentForm.getStringValue(COL_CURRENCY));
      options.put(COL_MODEL, parentForm.getStringValue(COL_MODEL));
      options.put(COL_PRODUCTION_DATE, parentForm.getStringValue(COL_PRODUCTION_DATE));

      ClassifierKeeper.getPriceAndDiscount(event.getValue(), options, (price, percent) -> {
        if (BeeUtils.isPositive(price)) {
          values.put(COL_PRICE, BeeUtils.toString(price, 2));
        }
        values.put(COL_TRADE_DOCUMENT_ITEM_DISCOUNT, BeeUtils.toStringOrNull(percent));
        values.put(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT,
            BeeUtils.toString(BeeUtils.isPositive(percent)));

        values.forEach((col, val) -> {
          target.setValue(dataView.getDataIndex(col), val);
          dataView.refreshBySource(col);
        });
      });
    }
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    if (Objects.equals(event.getColumnId(), COL_RESERVE)) {
      event.consume();
      IsRow row = event.getRowValue();
      String value = Data.getString(getViewName(), row, COL_RESERVE);

      Queries.updateCellAndFire(getViewName(), row.getId(), row.getVersion(), COL_RESERVE,
          value, BeeUtils.toString(!BeeUtils.toBoolean(value)));
    } else {
      super.onEditStart(event);
    }
  }
}
