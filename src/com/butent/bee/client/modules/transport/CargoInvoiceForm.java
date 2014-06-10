package com.butent.bee.client.modules.transport;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.IdFilter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

public class CargoInvoiceForm extends CargoPurchaseInvoiceForm implements ClickHandler {

  private final Button confirmAction = new Button(Localized.getConstants().trdInvoice(), this);

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    int idx = form.getDataIndex(COL_SALE_PROFORMA);
    boolean proforma = idx != BeeConst.UNDEF && BeeUtils.unbox(row.getBoolean(idx));

    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    header.setCaption(proforma ? Localized.getConstants().trProformaInvoice()
        : Localized.getConstants().trdInvoice());

    if (proforma && form.isEnabled()) {
      header.addCommandItem(confirmAction);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new CargoInvoiceForm();
  }

  @Override
  public void onClick(ClickEvent event) {
    final FormView form = getFormView();

    Global.confirm(Localized.getConstants().trConfirmProforma(), new ConfirmationCallback() {
      @Override
      public void onConfirm() {
        Queries.update(form.getViewName(),
            IdFilter.compareId(form.getActiveRow().getId()),
            COL_SALE_PROFORMA, BooleanValue.getNullValue(), new IntCallback() {
              @Override
              public void onSuccess(Integer result) {
                if (BeeUtils.isPositive(result)) {
                  Data.onViewChange(form.getViewName(), DataChangeEvent.CANCEL_RESET_REFRESH);
                }
              }
            });
      }
    });
  }

  @Override
  protected String getTradeItemsName() {
    return TBL_SALE_ITEMS;
  }
}
