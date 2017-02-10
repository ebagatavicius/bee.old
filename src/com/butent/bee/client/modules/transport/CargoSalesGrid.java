package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.modules.trade.InvoiceBuilder;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

public class CargoSalesGrid extends InvoiceBuilder {

  @Override
  protected void createInvoice(final BeeRowSet data, final BiConsumer<BeeRowSet, BeeRow> consumer) {
    final DataInfo targetInfo = Data.getDataInfo(getTargetView());
    final BeeRow newRow = RowFactory.createEmptyRow(targetInfo, true);

    Set<String> orders = new TreeSet<>();
    Set<String> vehicles = new TreeSet<>();
    Set<String> drivers = new TreeSet<>();
    Map<Long, String> customers = new HashMap<>();
    Map<Long, String> payers = new HashMap<>();
    Map<Long, String> currencies = new HashMap<>();

    DataInfo info = Data.getDataInfo(getViewName());
    int order = info.getColumnIndex(COL_ORDER_NO);
    int vehicle = info.getColumnIndex(COL_VEHICLE);
    int trailer = info.getColumnIndex(COL_TRAILER);
    int forwarderVehicle = info.getColumnIndex(COL_FORWARDER_VEHICLE);
    int driver = info.getColumnIndex(COL_DRIVER);
    int forwarderDriver = info.getColumnIndex(COL_FORWARDER_DRIVER);
    int custId = info.getColumnIndex(COL_CUSTOMER);
    int custName = info.getColumnIndex(COL_CUSTOMER_NAME);
    int compId = info.getColumnIndex(COL_COMPANY);
    int compName = info.getColumnIndex(ALS_COMPANY_NAME);
    int payerId = info.getColumnIndex(COL_PAYER);
    int payerName = info.getColumnIndex(COL_PAYER_NAME);
    int currId = info.getColumnIndex(COL_CURRENCY);
    int currName = info.getColumnIndex(ALS_CURRENCY_NAME);

    for (BeeRow row : data.getRows()) {
      Long id = BeeUtils.nvl(row.getLong(compId), row.getLong(custId));
      if (DataUtils.isId(id)) {
        customers.put(id, BeeUtils.nvl(row.getString(compName), row.getString(custName)));
      }
      id = row.getLong(payerId);
      if (DataUtils.isId(id)) {
        payers.put(id, row.getString(payerName));
      }
      id = row.getLong(currId);
      if (DataUtils.isId(id)) {
        currencies.put(id, row.getString(currName));
      }
      orders.add(row.getString(order));
      vehicles.add(BeeUtils.join("/", row.getString(vehicle), row.getString(trailer),
          row.getString(forwarderVehicle)));
      drivers.add(BeeUtils.joinItems(row.getString(driver), row.getString(forwarderDriver)));
    }
    newRow.setValue(targetInfo.getColumnIndex(COL_VEHICLE), BeeUtils.joinItems(vehicles));
    newRow.setValue(targetInfo.getColumnIndex(COL_DRIVER), BeeUtils.joinItems(drivers));
    newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_NOTES), BeeUtils.joinItems(orders));

    newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_MANAGER), BeeKeeper.getUser().getUserId());
    newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_MANAGER + COL_PERSON),
        BeeKeeper.getUser().getUserData().getCompanyPerson());
    newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_MANAGER + COL_FIRST_NAME),
        BeeKeeper.getUser().getFirstName());
    newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_MANAGER + COL_LAST_NAME),
        BeeKeeper.getUser().getLastName());

    if (customers.size() == 1) {
      Map.Entry<Long, String> entry = BeeUtils.peek(customers.entrySet());
      newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_CUSTOMER), entry.getKey());
      newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_CUSTOMER + "Name"), entry.getValue());
    }
    if (payers.size() == 1) {
      Long payer = BeeUtils.peek(payers.keySet());

      if (!Objects.equals(payer, newRow.getLong(targetInfo.getColumnIndex(COL_CUSTOMER)))) {
        newRow.setValue(targetInfo.getColumnIndex(COL_SALE_PAYER), payer);
        newRow.setValue(targetInfo.getColumnIndex(COL_SALE_PAYER + "Name"), payers.get(payer));
      }
    }
    if (currencies.size() == 1) {
      Map.Entry<Long, String> entry = BeeUtils.peek(currencies.entrySet());
      newRow.setValue(targetInfo.getColumnIndex(COL_CURRENCY), entry.getKey());
      newRow.setValue(targetInfo.getColumnIndex(ALS_CURRENCY_NAME), entry.getValue());
    }
    Global.getParameterRelation(PRM_INVOICE_PREFIX, (prefixId, prefix) -> {
      newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_SALE_SERIES), prefixId);
      newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_INVOICE_PREFIX), prefix);
      consumer.accept(data, newRow);
    });
  }

  @Override
  public GridInterceptor getInstance() {
    return new CargoSalesGrid();
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    if (Objects.equals(event.getColumnId(), COL_ORDER_NO)) {
      Long assessment = Data.getLong(getViewName(), event.getRowValue(), COL_ASSESSMENT);

      if (DataUtils.isId(assessment)) {
        RowEditor.open(VIEW_ASSESSMENTS, assessment, Opener.NEW_TAB);
        event.consume();
        return;
      }
    }
    super.onEditStart(event);
  }

  @Override
  protected String getRelationColumn() {
    return COL_SALE;
  }

  @Override
  protected ParameterList getRequestArgs() {
    return TransportHandler.createArgs(SVC_CREATE_INVOICE_ITEMS);
  }

  @Override
  protected String getTargetView() {
    return VIEW_CARGO_INVOICES;
  }
}
