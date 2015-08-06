package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.modules.trade.InvoiceBuilder;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class TripPurchasesGrid extends InvoiceBuilder {

  @Override
  public GridInterceptor getInstance() {
    return new TripPurchasesGrid();
  }

  @Override
  protected void createInvoice(BeeRowSet data, BiConsumer<BeeRowSet, BeeRow> consumer) {
    DataInfo targetInfo = Data.getDataInfo(getTargetView());
    BeeRow newRow = RowFactory.createEmptyRow(targetInfo, true);

    Set<String> trips = new TreeSet<>();
    Map<Long, String> suppliers = new HashMap<>();
    Map<Long, String> currencies = new HashMap<>();

    DataInfo info = Data.getDataInfo(getViewName());
    int trip = info.getColumnIndex(COL_TRIP_NO);
    int suplId = info.getColumnIndex(COL_COSTS_SUPPLIER);
    int suplName = info.getColumnIndex(COL_COSTS_SUPPLIER + "Name");
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
    newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_NOTES), BeeUtils.joinItems(trips));
    newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_MANAGER), BeeKeeper.getUser().getUserId());
    newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_MANAGER + COL_PERSON),
        BeeKeeper.getUser().getUserData().getCompanyPerson());
    newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_MANAGER + COL_FIRST_NAME),
        BeeKeeper.getUser().getFirstName());
    newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_MANAGER + COL_LAST_NAME),
        BeeKeeper.getUser().getLastName());

    if (suppliers.size() == 1) {
      Map.Entry<Long, String> entry = BeeUtils.peek(suppliers.entrySet());
      newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_SUPPLIER), entry.getKey());
      newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_SUPPLIER + "Name"), entry.getValue());
    }
    if (currencies.size() == 1) {
      Map.Entry<Long, String> entry = BeeUtils.peek(currencies.entrySet());
      newRow.setValue(targetInfo.getColumnIndex(COL_CURRENCY), entry.getKey());
      newRow.setValue(targetInfo.getColumnIndex(ALS_CURRENCY_NAME), entry.getValue());
    }
    consumer.accept(data, newRow);
  }

  @Override
  public String getRelationColumn() {
    return COL_PURCHASE;
  }

  @Override
  public ParameterList getRequestArgs() {
    return TransportHandler.createArgs(TransportConstants.SVC_CREATE_INVOICE_ITEMS);
  }

  @Override
  public String getTargetView() {
    return VIEW_TRIP_PURCHASE_INVOICES;
  }
}
