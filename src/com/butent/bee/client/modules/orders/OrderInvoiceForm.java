package com.butent.bee.client.modules.orders;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.orders.OrdersConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.COL_CUSTOMER;
import static com.butent.bee.shared.modules.transport.TransportConstants.COL_PAYER;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.modules.mail.NewMailMessage;
import com.butent.bee.client.modules.trade.InvoiceERPForm;
import com.butent.bee.client.output.ReportUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class OrderInvoiceForm extends InvoiceERPForm {

  private Button confirmAction;
  private DataSelector series;

  @Override
  public FormInterceptor getInstance() {
    return new OrderInvoiceForm();
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, COL_SERIES)) {
      series = (DataSelector) widget;
    }
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    HeaderView header = form.getViewPresenter().getHeader();
    int idx = form.getDataIndex(COL_SALE_PROFORMA);
    boolean proforma = !BeeConst.isUndef(idx) && row != null && BeeUtils.unbox(row.getBoolean(idx));

    form.getViewPresenter().getHeader().setCaption(proforma
        ? Localized.dictionary().trProformaInvoice()
        : Localized.dictionary().trdInvoice());

    series.setEnabled(proforma);

    if (confirmAction == null) {
      confirmAction = new Button(Localized.dictionary().trdInvoice(),
          arg0 -> Global.confirm(Localized.dictionary().trConfirmProforma(), () -> {

            if (Data.isNull(VIEW_ORDER_CHILD_INVOICES, row, COL_TRADE_SALE_SERIES)) {
              getFormView().notifySevere(Localized.dictionary()
                  .fieldRequired(Localized.dictionary().trdInvoicePrefix()));
              return;
            }
            Data.setValue(getViewName(), getActiveRow(), COL_SALE_PROFORMA, (Boolean) null);
            BeeRowSet updated = DataUtils.getUpdated(getViewName(), getFormView().getDataColumns(),
              form.getOldRow(), getActiveRow(), null);

            Queries.updateRow(updated, new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
                Data.onViewChange(getViewName(), DataChangeEvent.CANCEL_RESET_REFRESH);
              }
            });
          }));
      header.addCommandItem(confirmAction);
    }
    confirmAction.setVisible(proforma && form.isEnabled());
  }

  @Override
  public void getERPStocks(final Long id) {
    ParameterList params = OrdersKeeper.createSvcArgs(SVC_GET_ERP_STOCKS);
    params.addDataItem(Service.VAR_DATA, DataUtils.buildIdList(id));

    BeeKeeper.getRpc().makeRequest(params);
  }

  @Override
  protected void getReportData(Consumer<BeeRowSet[]> dataConsumer) {
    Queries.getRowSet(VIEW_SALE_ITEMS, null, Filter.equals(COL_SALE, getActiveRowId()),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            dataConsumer.accept(new BeeRowSet[] {result});
          }
        });
  }

  @Override
  protected ReportUtils.ReportCallback getReportCallback() {
    return new ReportUtils.ReportCallback() {
      @Override
      public void accept(FileInfo fileInfo) {
        sendInvoice(fileInfo);
      }

      @Override
      public Widget getActionWidget() {
        FaLabel action = new FaLabel(FontAwesome.ENVELOPE_O);
        action.setTitle(Localized.dictionary().trWriteEmail());
        return action;
      }
    };
  }

  @Override
  protected void getReportParameters(Consumer<Map<String, String>> parametersConsumer) {
    Map<String, Long> companies = new HashMap<>();
    companies.put(COL_CUSTOMER, getLongValue(COL_CUSTOMER));
    companies.put(COL_PAYER, getLongValue(COL_PAYER));
    companies.put(TradeConstants.COL_TRADE_SUPPLIER, BeeKeeper.getUser().getCompany());

    super.getReportParameters(defaultParameters ->
        ClassifierUtils.getCompaniesInfo(companies, companiesInfo -> {
          defaultParameters.putAll(companiesInfo);
          parametersConsumer.accept(defaultParameters);
        }));
  }

  private String getFileName() {
    IsRow row = getActiveRow();

    String invoicePrefix =
        row.getString(Data.getColumnIndex(VIEW_ORDER_CHILD_INVOICES, COL_TRADE_INVOICE_PREFIX));
    String number =
        row.getString(Data.getColumnIndex(VIEW_ORDER_CHILD_INVOICES, COL_TRADE_INVOICE_NO));

    String name;
    if (!BeeUtils.isEmpty(invoicePrefix)) {
      name = BeeUtils.join("_", invoicePrefix, number);
    } else {
      name = "bee_invoice";
    }

    return name + ".pdf";
  }

  private void sendInvoice(FileInfo fileInfo) {
    FormView form = getFormView();
    fileInfo.setCaption(getFileName());

    List<FileInfo> attach = new ArrayList<>();
    attach.add(fileInfo);

    NewMailMessage.create(BeeUtils.notEmpty(form.getStringValue(ALS_PAYER_EMAIL),
        form.getStringValue(ALS_CUSTOMER_EMAIL)), null, null, attach, null);
  }
}