package com.butent.bee.client.modules.transport;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.trade.TradeConstants.COL_SALE;

import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.modules.trade.InvoiceForm;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.utils.NameUtils;

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
        caption = Localized.getConstants().trCreditInvoice();
        child = form.getWidgetByName(TransportConstants.VIEW_CARGO_PURCHASES);
      } else {
        caption = Localized.getConstants().trPurchaseInvoice();
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
}
