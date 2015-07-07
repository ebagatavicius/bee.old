package com.butent.bee.client.modules.transport;

import com.google.common.collect.ImmutableMap;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CargoCreditSalesGrid extends AbstractGridInterceptor implements ClickHandler {

  private UnboundSelector mainItem;
  private InputNumber creditAmount;

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    presenter.getHeader().clearCommandPanel();
    presenter.getHeader()
        .addCommandItem(new Button(Localized.getConstants().createCreditInvoice(), this));
  }

  @Override
  public Map<String, Filter> getInitialParentFilters(Collection<UiOption> uiOptions) {
    return ImmutableMap.of("pyp",
        Filter.and(Filter.isNull(COL_SALE_PROFORMA), Filter.isNull(COL_PURCHASE)));
  }

  @Override
  public GridInterceptor getInstance() {
    return new CargoCreditSalesGrid();
  }

  @Override
  public void onClick(ClickEvent event) {
    final GridPresenter presenter = getGridPresenter();
    final Set<Long> ids = new HashSet<>();

    for (RowInfo row : presenter.getGridView().getSelectedRows(SelectedRows.ALL)) {
      ids.add(row.getId());
    }
    if (ids.isEmpty()) {
      presenter.getGridView().notifyWarning(Localized.getConstants().selectAtLeastOneRow());
      return;
    }
    Queries.getRowSet(VIEW_CARGO_CREDIT_SALES, null, Filter.idIn(ids), new RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        Set<String> orders = new HashSet<>();
        Map<Long, String> customers = new HashMap<>();
        Map<Long, String> currencies = new HashMap<>();

        boolean itemEmpty = false;
        DataInfo info = Data.getDataInfo(VIEW_CARGO_CREDIT_SALES);

        int item = info.getColumnIndex(ClassifierConstants.COL_ITEM);
        int order = info.getColumnIndex(COL_ORDER_NO);
        int custId = info.getColumnIndex(COL_CUSTOMER);
        int custName = info.getColumnIndex(COL_CUSTOMER_NAME);
        int currId = info.getColumnIndex(COL_CURRENCY);
        int currName = info.getColumnIndex(COL_CURRENCY + COL_CURRENCY_NAME);

        for (BeeRow row : result.getRows()) {
          if (!itemEmpty) {
            itemEmpty = row.getLong(item) == null;
          }
          orders.add(row.getString(order));

          Long id = row.getLong(custId);

          if (DataUtils.isId(id)) {
            customers.put(id, row.getString(custName));
          }
          id = row.getLong(currId);

          if (DataUtils.isId(id)) {
            currencies.put(id, row.getString(currName));
          }
        }
        final boolean mainRequired = itemEmpty;
        final DataInfo purchaseInfo = Data.getDataInfo(VIEW_CARGO_PURCHASE_INVOICES);

        BeeRow newRow = RowFactory.createEmptyRow(purchaseInfo, true);

        newRow.setValue(purchaseInfo.getColumnIndex(COL_TRADE_NUMBER), BeeUtils.joinItems(orders));

        if (customers.size() == 1) {
          for (Entry<Long, String> entry : customers.entrySet()) {
            newRow.setValue(purchaseInfo.getColumnIndex(COL_TRADE_SUPPLIER), entry.getKey());
            newRow.setValue(purchaseInfo.getColumnIndex("SupplierName"), entry.getValue());
          }
        }
        if (currencies.size() == 1) {
          for (Entry<Long, String> entry : currencies.entrySet()) {
            newRow.setValue(purchaseInfo.getColumnIndex(COL_CURRENCY), entry.getKey());
            newRow.setValue(purchaseInfo.getColumnIndex(COL_CURRENCY + COL_CURRENCY_NAME),
                entry.getValue());
          }
        }
        RowFactory.createRow(FORM_NEW_CARGO_CREDIT_INVOICE, null, purchaseInfo, newRow, null,
            new AbstractFormInterceptor() {
              @Override
              public FormInterceptor getInstance() {
                return this;
              }

              @Override
              public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent ev) {
                FormView form = getFormView();
                Widget w = form.getWidgetByName("Cause");

                if (w != null && w instanceof ListBox) {
                  String cause = ((ListBox) w).getValue();
                  int idx = -1;

                  for (int i = 0; i < ev.getColumns().size(); i++) {
                    if (BeeUtils.same(ev.getColumns().get(i).getId(), COL_TRADE_NOTES)) {
                      idx = i;
                      break;
                    }
                  }
                  if (idx >= 0) {
                    ev.getValues().set(idx, cause + "\n" + ev.getValues().get(idx));
                  } else {
                    ev.getColumns().add(Data.getColumn(form.getViewName(), COL_TRADE_NOTES));
                    ev.getValues().add(cause);
                  }
                }
              }

              @Override
              public void onStart(FormView form) {
                Widget w = form.getWidgetByName("MainItem");

                if (w != null && w instanceof UnboundSelector) {
                  mainItem = (UnboundSelector) w;

                  if (mainRequired) {
                    mainItem.setNullable(false);
                    w = form.getWidgetByName("MainItemCaption");

                    if (w != null) {
                      w.addStyleName(StyleUtils.NAME_REQUIRED);
                    }
                  }
                }
                w = form.getWidgetByName(COL_TRADE_AMOUNT);

                if (w != null && w instanceof InputNumber) {
                  creditAmount = (InputNumber) w;
                }
              }
            },
            new RowCallback() {
              @Override
              public void onCancel() {
                mainItem = null;
                creditAmount = null;
              }

              @Override
              public void onSuccess(final BeeRow row) {
                ParameterList args = TransportHandler.createArgs(SVC_CREATE_INVOICE_ITEMS);
                args.addDataItem(TradeConstants.COL_PURCHASE, row.getId());
                args.addDataItem(COL_CURRENCY,
                    row.getLong(purchaseInfo.getColumnIndex(COL_CURRENCY)));
                args.addDataItem(Service.VAR_ID, DataUtils.buildIdList(ids));

                if (mainItem != null && DataUtils.isId(mainItem.getRelatedId())) {
                  args.addDataItem(ClassifierConstants.COL_ITEM, mainItem.getRelatedId());
                }
                if (creditAmount != null && BeeUtils.isPositive(creditAmount.getNumber())) {
                  args.addDataItem(COL_TRADE_AMOUNT, creditAmount.getNumber());
                }
                BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
                  @Override
                  public void onResponse(ResponseObject response) {
                    response.notify(presenter.getGridView());

                    if (!response.hasErrors()) {
                      Data.onViewChange(presenter.getViewName(),
                          DataChangeEvent.CANCEL_RESET_REFRESH);
                      RowEditor.openForm(FORM_CARGO_PURCHASE_INVOICE, purchaseInfo, row.getId(),
                          Opener.MODAL);
                    }
                  }
                });
                onCancel();
              }
            });
      }
    });
  }
}
