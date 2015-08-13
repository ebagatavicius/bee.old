package com.butent.bee.client.modules.trade;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrintInvoiceInterceptor extends AbstractFormInterceptor {

  Map<String, Widget> companies = new HashMap<>();
  HtmlTable invoiceDetails;
  List<Widget> totals = new ArrayList<>();

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.inListSame(name, COL_TRADE_SUPPLIER, COL_TRADE_CUSTOMER, COL_SALE_PAYER)) {
      companies.put(name, widget.asWidget());

    } else if (BeeUtils.same(name, "InvoiceDetails") && widget instanceof HtmlTable) {
      invoiceDetails = (HtmlTable) widget;

    } else if (BeeUtils.startsSame(name, "TotalInWords")) {
      totals.add(widget.asWidget());
    }
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    for (String name : companies.keySet()) {
      Long id = form.getLongValue(name);

      if (!DataUtils.isId(id) && !BeeUtils.same(name, COL_SALE_PAYER)) {
        id = BeeKeeper.getUser().getUserData().getCompany();
      }
      ClassifierUtils.getCompanyInfo(id, companies.get(name));
    }
    if (invoiceDetails != null) {
      TradeUtils.getDocumentItems(getViewName(), row.getId(),
          form.getStringValue(AdministrationConstants.ALS_CURRENCY_NAME), invoiceDetails);
    }
    for (Widget total : totals) {
      TradeUtils.getTotalInWords(form.getDoubleValue(COL_TRADE_AMOUNT),
          form.getLongValue(COL_TRADE_CURRENCY), total);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new PrintInvoiceInterceptor();
  }
}
