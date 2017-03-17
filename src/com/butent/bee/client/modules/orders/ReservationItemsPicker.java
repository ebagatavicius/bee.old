package com.butent.bee.client.modules.orders;

import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.SVC_GET_ITEMS_FOR_SELECTION;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.modules.classifiers.ItemsPicker;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;

public abstract class ReservationItemsPicker extends ItemsPicker {

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

    addAdditionalFilter(params);

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

  protected abstract void addAdditionalFilter(ParameterList params);
}
