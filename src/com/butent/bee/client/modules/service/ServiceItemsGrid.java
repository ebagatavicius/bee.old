package com.butent.bee.client.modules.service;

import com.google.common.collect.ImmutableMap;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.modules.classifiers.ItemsPicker;
import com.butent.bee.client.modules.orders.OrderItemsGrid;
import com.butent.bee.client.modules.transport.InvoiceCreator;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Map;

public class ServiceItemsGrid extends OrderItemsGrid {

  private ServiceItemsPicker picker;

  @Override
  public ItemsPicker ensurePicker() {
    if (picker == null) {
      picker = new ServiceItemsPicker(Module.SERVICE);
      picker.addSelectionHandler(this);
    }
    return picker;
  }

  @Override
  public Map<String, String> getAdditionalColumns() {
    return ImmutableMap.of(COL_SERVICE_OBJECT, BeeConst.STRING_EMPTY,
        COL_REPAIRER, BeeUtils.toString(BeeKeeper.getUser().getUserData().getCompanyPerson()));
  }

  @Override
  public GridInterceptor getInstance() {
    return new ServiceItemsGrid();
  }

  @Override
  public String getParentDateColumnName() {
    return COL_MAINTENANCE_DATE;
  }

  @Override
  public String getParentRelationColumnName() {
    return COL_SERVICE_MAINTENANCE;
  }

  @Override
  public String getParentViewName() {
    return TBL_SERVICE_MAINTENANCE;
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    setOrderForm(event.getRowId());
    getInvoice().clear();

    if (DataUtils.isId(getOrderForm())) {
      getInvoice().add(new InvoiceCreator(VIEW_SERVICE_SALES,
          Filter.equals(COL_SERVICE_MAINTENANCE, getOrderForm())));
    }
  }

  @Override
  public boolean validParentState(IsRow parentRow) {
    String endingDate = parentRow.getString(Data.getColumnIndex(getParentViewName(),
        COL_ENDING_DATE));
    return BeeUtils.isEmpty(endingDate);
  }

  @Override
  protected double calculateItemPrice(Pair<Double, Double> pair, BeeRow row, int unpackingIdx,
      int qtyIndex) {
    double price = super.calculateItemPrice(pair, row, unpackingIdx, qtyIndex);

    if (picker.isCheckedFilterService()) {
      IsRow parentRow = ViewHelper.getFormRow(getGridView());

      if (parentRow != null) {
        price = ServiceUtils.calculateServicePrice(price, parentRow);
      }
    }
    return price;
  }
}