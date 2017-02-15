package com.butent.bee.client.modules.orders;

import static com.butent.bee.shared.modules.orders.OrdersConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.modules.classifiers.ItemsPicker;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.orders.OrdersConstants.OrdersStatus;

import java.util.Objects;

class OrderItemsPicker extends ItemsPicker {

  @Override
  public void getItems(Filter filter, final RowSetCallback callback) {
    ParameterList params = OrdersKeeper.createSvcArgs(SVC_GET_ITEMS_FOR_SELECTION);

    if (DataUtils.isId(getWarehouseFrom())) {
      params.addDataItem(ClassifierConstants.COL_WAREHOUSE, getWarehouseFrom());
    }

    if (getRemainderValue()) {
      params.addDataItem(ClassifierConstants.COL_WAREHOUSE_REMAINDER, String
          .valueOf(getRemainderValue()));
    }

    if (filter != null) {
      params.addDataItem(Service.VAR_VIEW_WHERE, filter.serialize());
    }

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(BeeRowSet.class)) {
          callback.onSuccess(BeeRowSet.restore(response.getResponseAsString()));
        } else {
          getSpinner().setStyleName(STYLE_SEARCH_SPINNER_LOADING, false);
          BeeKeeper.getScreen().notifyWarning(Localized.dictionary().nothingFound());
        }
      }
    });
  }

  @Override
  public Long getWarehouseFrom(IsRow row) {
    int warehouseIdx = Data.getColumnIndex(VIEW_ORDERS, ClassifierConstants.COL_WAREHOUSE);
    if (row == null || BeeConst.isUndef(warehouseIdx)) {
      return null;
    }

    return row.getLong(warehouseIdx);
  }

  @Override
  public boolean setIsOrder(IsRow row) {
    int statusIdx = Data.getColumnIndex(VIEW_ORDERS, COL_ORDERS_STATUS);
    if (row == null || BeeConst.isUndef(statusIdx)) {
      return false;
    }

    return Objects.equals(row.getInteger(statusIdx), OrdersStatus.APPROVED.ordinal());
  }
}
