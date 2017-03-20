package com.butent.bee.client.modules.transport;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.COL_CUSTOMER;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.modules.trade.InvoiceForm;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CargoPurchaseInvoiceForm extends InvoiceForm {

  private boolean done;

  public CargoPurchaseInvoiceForm() {
    super(null);
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    if (!done) {
      String caption;
      Widget child;

      if (DataUtils.isId(form.getLongValue(COL_SALE))) {
        caption = Localized.dictionary().trCreditInvoice();
        child = form.getWidgetByName(TransportConstants.VIEW_CARGO_PURCHASES);
      } else {
        caption = Localized.dictionary().trPurchaseInvoice();
        child = form.getWidgetByName(TransportConstants.VIEW_CARGO_SALES);
      }
      form.getViewPresenter().getHeader().setCaption(caption);

      if (child != null) {
        Widget tabs = form.getWidgetByName(NameUtils.getClassName(TabbedPages.class));

        if (tabs != null && tabs instanceof TabbedPages) {
          int idx = ((TabbedPages) tabs).getContentIndex(child);

          if (!BeeConst.isUndef(idx)) {
            ((TabbedPages) tabs).removePage(idx);
          }
        }
      }
      done = true;
    }
    super.beforeRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new CargoPurchaseInvoiceForm();
  }

  @Override
  protected void getReportParameters(Consumer<Map<String, String>> parametersConsumer) {
    Map<String, Long> companies = new HashMap<>();
    companies.put(COL_CUSTOMER, BeeUtils.nvl(getLongValue(COL_CUSTOMER),
        BeeKeeper.getUser().getCompany()));
    companies.put(COL_TRADE_SUPPLIER, getLongValue(COL_TRADE_SUPPLIER));

    super.getReportParameters(defaultParameters ->
        ClassifierUtils.getCompaniesInfo(companies, companiesInfo -> {
          defaultParameters.putAll(companiesInfo);
          parametersConsumer.accept(defaultParameters);
        }));
  }

  @Override
  protected void getReportData(Consumer<BeeRowSet[]> dataConsumer) {
    Queries.getRowSet(VIEW_PURCHASE_ITEMS, null, Filter.equals(COL_PURCHASE, getActiveRowId()),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            dataConsumer.accept(new BeeRowSet[] {result});
          }
        });
  }
}
