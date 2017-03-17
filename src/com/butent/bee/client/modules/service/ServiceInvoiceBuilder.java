package com.butent.bee.client.modules.service;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_ITEM_IS_SERVICE;
import static com.butent.bee.shared.modules.service.ServiceConstants.SVC_CREATE_RESERVATION_INVOICE_ITEMS;

import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.modules.orders.OrderInvoiceBuilder;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;

public class ServiceInvoiceBuilder extends OrderInvoiceBuilder {

  @Override
  public GridInterceptor getInstance() {
    return new ServiceInvoiceBuilder();
  }

  @Override
  public ParameterList getRequestArgs() {
    return ServiceKeeper.createArgs(SVC_CREATE_RESERVATION_INVOICE_ITEMS);
  }

  @Override
  public boolean validItemForInvoice(IsRow row) {
    int isServiceIdx = Data.getColumnIndex(getViewName(), COL_ITEM_IS_SERVICE);
    return !row.isNull(isServiceIdx);
  }
}