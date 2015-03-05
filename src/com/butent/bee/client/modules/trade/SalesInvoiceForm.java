package com.butent.bee.client.modules.trade;

import com.butent.bee.client.output.PrintFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.modules.trade.TradeConstants;

public class SalesInvoiceForm extends PrintFormInterceptor {

  SalesInvoiceForm() {
  }

  @Override
  public FormInterceptor getInstance() {
    return new SalesInvoiceForm();
  }

  @Override
  public FormInterceptor getPrintFormInterceptor() {

    return new TradeDocumentRenderer(TradeConstants.VIEW_SALE_ITEMS,
        TradeConstants.COL_SALE);
  }
}
