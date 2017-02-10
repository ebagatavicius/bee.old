package com.butent.bee.client.modules.orders;

import com.google.gwt.event.shared.HasHandlers;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.orders.OrdersConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Arrays;
import java.util.Map;

public class NewOrderInvoiceForm extends AbstractFormInterceptor {

  private static final String NAME_SERIES_LABEL = "SeriesLabel";

  private int companyIdx = Data.getColumnIndex(VIEW_ORDER_CHILD_INVOICES, COL_TRADE_CUSTOMER);

  private int debtIdx = Data.getColumnIndex(VIEW_SALES, VAR_DEBT);
  private int termIdx = Data.getColumnIndex(VIEW_SALES, COL_TRADE_TERM);

  private double creditLimit;
  private double debt;
  private boolean notValid;
  private int toleratedDays;
  private Label seriesLabel;

  @Override
  public FormInterceptor getInstance() {
    return new NewOrderInvoiceForm();
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, COL_TRADE_OPERATION)) {
      ((DataSelector) widget).addSelectorHandler(event -> {
        if (event.isOpened() && BeeUtils.unbox(Global.getParameterBoolean(PRM_CHECK_DEBT))) {
          getInfoAboutCompany(event, Holder.of(4), null, null);
        }
      });
    } else if (BeeUtils.same(name, NAME_SERIES_LABEL)) {
      seriesLabel = (Label) widget;
      seriesLabel.setStyleName(StyleUtils.NAME_REQUIRED, true);
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    createCellValidationHandler(form, row);
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
    if (!isProforma()) {
      if (BeeUtils.unbox(Global.getParameterBoolean(PRM_CHECK_DEBT))) {
        event.consume();
        final Holder<Integer> holder = Holder.of(4);
        getInfoAboutCompany(null, holder, listener, event);

      } else if (Data.isNull(VIEW_ORDER_CHILD_INVOICES, getActiveRow(), COL_TRADE_SALE_SERIES)) {
        getFormView().notifySevere(Localized.dictionary()
            .fieldRequired(Localized.dictionary().trdInvoicePrefix()));
        event.consume();
      }
    }
  }

  private void createCellValidationHandler(FormView form, IsRow row) {
    if (form == null || row == null) {
      return;
    }

    form.addCellValidationHandler(COL_SALE_PROFORMA, event -> {
      getSeriesRequired(event.getNewValue());
      return true;
    });
  }

  private Filter getFilter(BeeRowSet wrhResult) {
    Filter filter = null;
    Long warehouse = getLongValue(COL_TRADE_WAREHOUSE_FROM);
    boolean hasCashRegisterNo = false;

    for (BeeRow row : wrhResult) {
      String cashRegisterNo = row.getString(1);
      if (!BeeUtils.isEmpty(cashRegisterNo)) {
        hasCashRegisterNo = true;
        break;
      }
    }

    if (!isProforma()) {
      if (creditLimit == 0 || debt > creditLimit || notValid) {
        if (wrhResult.getNumberOfRows() > 0 && hasCashRegisterNo) {
          filter =
              Filter.and(Filter.equals(COL_TRADE_WAREHOUSE_FROM, warehouse), Filter
                  .notNull(COL_OPERATION_CASH_REGISTER_NO));
        } else {
          filter = Filter.notNull(COL_OPERATION_CASH_REGISTER_NO);
        }
      } else {
        if (wrhResult.getNumberOfRows() > 0) {
          filter = Filter.equals(COL_TRADE_WAREHOUSE_FROM, warehouse);
        }
      }
    } else {
      if (wrhResult.getNumberOfRows() > 0) {
        filter = Filter.equals(COL_TRADE_WAREHOUSE_FROM, warehouse);
      }
    }
    return filter;
  }

  private boolean isProforma() {
    int proformaIdx = Data.getColumnIndex(VIEW_ORDER_CHILD_INVOICES, COL_SALE_PROFORMA);

    return BeeUtils.unbox(getActiveRow().getBoolean(proformaIdx));
  }

  private void getInfoAboutCompany(SelectorEvent event, Holder<Integer> holder,
      HasHandlers listener, ReadyForInsertEvent event2) {

    IsRow activeRow = getActiveRow();
    if (activeRow == null) {
      return;
    }

    Long companyId = activeRow.getLong(companyIdx);
    Long warehouse = getLongValue(COL_OPERATION_WAREHOUSE_FROM);

    ParameterList args = TradeKeeper.createArgs(SVC_CREDIT_INFO);
    args.addDataItem(COL_COMPANY, companyId);

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {

      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasErrors()) {
          return;
        }

        holder.set(holder.get() - 1);
        Map<String, String> result = Codec.deserializeLinkedHashMap(response.getResponseAsString());

        debt = BeeUtils.toDouble(result.get(VAR_DEBT));
        creditLimit = BeeUtils.toDouble(result.get(COL_COMPANY_CREDIT_LIMIT));

        Queries.getValue(VIEW_COMPANIES, companyId, COL_COMPANY_TOLERATED_DAYS,
            new RpcCallback<String>() {

              @Override
              public void onSuccess(String intValue) {

                holder.set(holder.get() - 1);
                toleratedDays = BeeUtils.toInt(intValue);

                Queries.getRowSet(VIEW_TRADE_OPERATIONS, Arrays
                    .asList(COL_OPERATION_WAREHOUSE_FROM, COL_OPERATION_CASH_REGISTER_NO), Filter
                    .equals(COL_OPERATION_WAREHOUSE_FROM, warehouse), new RowSetCallback() {

                  @Override
                  public void onSuccess(BeeRowSet wrhResult) {
                    holder.set(holder.get() - 1);

                    Queries.getRowSet(VIEW_SALES, null, Filter.equals(COL_TRADE_CUSTOMER,
                        activeRow.getLong(companyIdx)), new RowSetCallback() {

                      @Override
                      public void onSuccess(BeeRowSet rowSet) {

                        holder.set(holder.get() - 1);
                        notValid = false;

                        for (BeeRow row : rowSet) {
                          int days =
                              row.getDate(termIdx) == null ? 0 : row.getDate(termIdx).getDays();
                          if (BeeUtils.unbox(row.getDouble(debtIdx)) > 0
                              && days + toleratedDays < TimeUtils.nowMillis().getDate()
                              .getDays()) {

                            notValid = true;
                            break;
                          }
                        }

                        if (event != null) {
                          event.getSelector().setAdditionalFilter(
                              getFilter(wrhResult));
                        }

                        if (listener != null && holder.get() == 0) {
                          boolean emptyCashRegNo =
                              BeeUtils.isEmpty(activeRow.getString(
                                  Data.getColumnIndex(VIEW_ORDER_CHILD_INVOICES,
                                      COL_OPERATION_CASH_REGISTER_NO)));

                          if (BeeUtils.unbox(creditLimit) == 0 && emptyCashRegNo) {
                            getFormView().notifySevere(
                                Localized.dictionary().ordCreditLimitEmpty());
                            return;
                          }

                          if (debt > BeeUtils.unbox(creditLimit) && emptyCashRegNo) {
                            getFormView().notifySevere(
                                Localized.dictionary().ordDebtExceedsCreditLimit());
                            return;
                          }

                          if (notValid && emptyCashRegNo) {
                            getFormView().notifySevere(
                                Localized.dictionary().ordOverdueInvoices());
                            return;
                          }

                          if (Data.isNull(VIEW_ORDER_CHILD_INVOICES, activeRow,
                              COL_TRADE_SALE_SERIES)) {
                            getFormView().notifySevere(Localized.dictionary()
                                .fieldRequired(Localized.dictionary().trdInvoicePrefix()));
                            return;
                          }

                          listener.fireEvent(event2);
                        }
                      }
                    });
                  }
                });
              }
            });
      }
    });
  }

  private void getSeriesRequired(String newValue) {
    boolean valueRequired = !BeeUtils.isEmpty(newValue);
    seriesLabel.setStyleName(StyleUtils.NAME_REQUIRED, !valueRequired);
  }
}