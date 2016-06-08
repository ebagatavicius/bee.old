package com.butent.bee.client.modules.orders;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Span;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.modules.trade.PrintInvoiceInterceptor;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.output.ReportUtils;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.Display;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.orders.OrdersConstants;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;

public class PrintOrdersInterceptor extends PrintInvoiceInterceptor {

  boolean sendByEmail;
  OrderForm orderForm;

  public PrintOrdersInterceptor(boolean value, OrderForm orderForm) {
    this.sendByEmail = value;
    this.orderForm = orderForm;
  }

  public PrintOrdersInterceptor() {
  }

  @Override
  public void afterRefresh(final FormView form, IsRow row) {
    if (sendByEmail) {
      HeaderView header = form.getViewPresenter().getHeader();
      header.clearCommandPanel();

      header.addCommandItem(new Button(Localized.dictionary().send(), new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {

          ParameterList params = getParams(form);

          BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {

            @Override
            public void onResponse(ResponseObject response) {
              if (!response.hasErrors()) {
                FileInfo fileInfo = FileInfo.restore(response.getResponseAsString());
                fileInfo.setCaption(getFileName());
                ReportUtils.preview(fileInfo, new Consumer<FileInfo>() {

                  @Override
                  public void accept(FileInfo input) {
                    if (orderForm != null) {
                      orderForm.sendMail(fileInfo);
                    }
                  }
                });
                getFormView().getViewPresenter().handleAction(Action.CLOSE);
              }
            }
          });
        }
      }));
    }
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action.equals(Action.PRINT)) {
      BeeKeeper.getRpc().makePostRequest(getParams(getFormView()),
          new ResponseCallback() {

            @Override
            public void onResponse(ResponseObject response) {
              FileInfo fileInfo = FileInfo.restore(response.getResponseAsString());
              fileInfo.setCaption(getFileName());
              ReportUtils.preview(fileInfo);
              getFormView().getViewPresenter().handleAction(Action.CLOSE);
            }
          });
      return false;
    }
    return true;
  }

  @Override
  public void beforeRefresh(final FormView form, IsRow row) {
    for (String name : companies.keySet()) {
      Long id = form.getLongValue(name);

      if (!DataUtils.isId(id) && !BeeUtils.same(name, COL_SALE_PAYER)) {
        id = BeeKeeper.getUser().getUserData().getCompany();
      }
      ClassifierUtils.getCompanyInfo(id, companies.get(name), name);
    }

    final Widget bankAccounts = form.getWidgetByName(COL_BANK_ACCOUNT);

    if (bankAccounts != null) {
      bankAccounts.getElement().setInnerHTML(null);
      renderBankAccounts(bankAccounts, BeeKeeper.getUser().getUserData().getCompany());
    }

    if (invoiceDetails != null) {

      final String typeTable = DomUtils.getDataProperty(invoiceDetails.getElement(), "content");

      OrdersKeeper.getDocumentItems(getViewName(), row.getId(),
          form.getStringValue(AdministrationConstants.ALS_CURRENCY_NAME), invoiceDetails,
          new Consumer<SimpleRowSet>() {

            @Override
            public void accept(SimpleRowSet data) {
              switch (typeTable) {

                case "OrderItems":
                  double discountTotal = 0.0D;
                  double quantity;
                  double price;
                  double discount;

                  for (SimpleRow r : data) {
                    quantity = BeeUtils.unbox(r.getDouble(COL_TRADE_ITEM_QUANTITY));
                    price = BeeUtils.unbox(r.getDouble(COL_TRADE_ITEM_PRICE));
                    discount = BeeUtils.unbox(r.getDouble(COL_TRADE_DISCOUNT));

                    discountTotal += discount * quantity * price / 100.0D;
                  }

                  NumberFormat formater = NumberFormat.getFormat("0.00");
                  Widget wDiscount = form.getWidgetByName("DiscountTotal");

                  if (wDiscount != null && discountTotal > 0.0D) {
                    wDiscount.getElement().setInnerText(formater.format(discountTotal));
                  } else {
                    Widget wDiscountLbl1 = form.getWidgetByName("DiscountLabel");
                    Widget wDiscountCrc = form.getWidgetByName("DiscountCurrency");
                    if (wDiscountLbl1 != null && wDiscountCrc != null) {
                      StyleUtils.setDisplay(wDiscountLbl1, Display.NONE);
                      StyleUtils.setDisplay(wDiscountCrc, Display.NONE);
                    }
                  }

                  break;
              }
            }
          });
    }
    for (Widget total : totals) {
      TradeUtils.getTotalInWords(form.getDoubleValue(COL_TRADE_AMOUNT),
          form.getLongValue(COL_TRADE_CURRENCY), total);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return this;
  }

  private String getFileName() {
    String series;
    String number;
    IsRow row = getActiveRow();

    if (orderForm != null) {
      series = row.getString(Data.getColumnIndex(OrdersConstants.VIEW_ORDERS, COL_SERIES_NAME));
      number = row.getString(Data.getColumnIndex(OrdersConstants.VIEW_ORDERS, COL_TRADE_NUMBER));
    } else {
      series =
          row.getString(Data.getColumnIndex(OrdersConstants.VIEW_ORDERS_INVOICES,
              COL_TRADE_INVOICE_PREFIX));
      number =
          row.getString(Data.getColumnIndex(OrdersConstants.VIEW_ORDERS_INVOICES,
              COL_TRADE_INVOICE_NO));
    }

    String name;
    if (!BeeUtils.isEmpty(series)) {
      name = BeeUtils.join("_", series, number);
    } else {
      name = "bee_order";
    }

    return name + ".pdf";
  }

  private static ParameterList getParams(FormView form) {
    ParameterList params = OrdersKeeper.createSvcArgs(OrdersConstants.SVC_CREATE_PDF_FILE);

    String printLandscape = null;

    if ("PrintOrder".equals(form.getFormName())) {
      printLandscape = "A4 landscape";
    }

    params.addDataItem(MailConstants.COL_CONTENT, form.getPrintElement().getString());

    if (!BeeUtils.isEmpty(printLandscape)) {
      params.addDataItem(DocumentConstants.PRM_PRINT_SIZE, printLandscape);
    }

    return params;
  }

  private static void renderBankAccounts(final Widget widget, Long supplier) {
    Queries.getRowSet(TBL_COMPANY_BANK_ACCOUNTS, Arrays.asList(COL_BANK_ACCOUNT,
        ALS_BANK_NAME, COL_BANK_CODE, COL_SWIFT_CODE), Filter.equals(COL_COMPANY,
        supplier),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            Flow flow = new Flow();

            for (int i = 0; i < result.getNumberOfRows(); i++) {
              Flow item = new Flow();

              for (int j = 0; j < result.getNumberOfColumns(); j++) {
                String text = result.getString(i, j);

                if (!BeeUtils.isEmpty(text)) {
                  Span span = new Span();
                  span.getElement().setClassName(result.getColumnId(j));
                  span.getElement().setInnerText(text);
                  item.add(span);
                }
              }
              if (!item.isEmpty()) {
                flow.add(item);
              }
            }
            widget.getElement().setInnerHTML(flow.getElement().getString());
          }
        });
  }
}
