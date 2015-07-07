package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.modules.trade.InvoiceBuilder;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TripInvoiceBuilder extends InvoiceBuilder {

  @Override
  public GridInterceptor getInstance() {
    return new TripInvoiceBuilder();
  }

  @Override
  public Map<String, String> getInitialValues(BeeRowSet data) {
    Set<String> trips = new HashSet<>();
    Map<Long, String> suppliers = new HashMap<>();
    Map<Long, String> currencies = new HashMap<>();

    DataInfo info = Data.getDataInfo(getViewName());

    int trip = info.getColumnIndex(COL_TRIP_NO);
    int suplId = info.getColumnIndex(COL_COSTS_SUPPLIER);
    int suplName = info.getColumnIndex("SupplierName");
    int currId = info.getColumnIndex(COL_CURRENCY);
    int currName = info.getColumnIndex(ALS_CURRENCY_NAME);

    for (BeeRow row : data.getRows()) {
      trips.add(row.getString(trip));

      Long id = row.getLong(suplId);
      if (DataUtils.isId(id)) {
        suppliers.put(id, row.getString(suplName));
      }
      id = row.getLong(currId);
      if (DataUtils.isId(id)) {
        currencies.put(id, row.getString(currName));
      }
    }
    Map<String, String> values = new HashMap<>();
    values.put(COL_TRADE_NOTES, BeeUtils.joinItems(trips));

    values.put(COL_TRADE_MANAGER, BeeUtils.toString(BeeKeeper.getUser().getUserId()));
    values.put(COL_TRADE_MANAGER + COL_PERSON,
        BeeUtils.toString(BeeKeeper.getUser().getUserData().getCompanyPerson()));
    values.put(COL_TRADE_MANAGER + COL_FIRST_NAME, BeeKeeper.getUser().getFirstName());
    values.put(COL_TRADE_MANAGER + COL_LAST_NAME, BeeKeeper.getUser().getLastName());

    if (suppliers.size() == 1) {
      Map.Entry<Long, String> entry = BeeUtils.peek(suppliers.entrySet());
      values.put(COL_TRADE_SUPPLIER, BeeUtils.toString(entry.getKey()));
      values.put("SupplierName", entry.getValue());
    }
    if (currencies.size() == 1) {
      Map.Entry<Long, String> entry = BeeUtils.peek(currencies.entrySet());
      values.put(COL_CURRENCY, BeeUtils.toString(entry.getKey()));
      values.put(ALS_CURRENCY_NAME, entry.getValue());
    }
    return values;
  }

  @Override
  public String getRelationColumn() {
    return COL_PURCHASE;
  }

  @Override
  public ParameterList getRequestArgs() {
    ParameterList args = TransportHandler.createArgs(TransportConstants.SVC_CREATE_INVOICE_ITEMS);
    args.addDataItem(Service.VAR_TABLE, TBL_TRIP_COSTS);
    return args;
  }

  @Override
  public String getTargetView() {
    return VIEW_TRIP_PURCHASE_INVOICES;
  }
}
