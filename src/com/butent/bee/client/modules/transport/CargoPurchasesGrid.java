package com.butent.bee.client.modules.transport;

import com.google.common.collect.ImmutableMap;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Pair;
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
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CargoPurchasesGrid extends AbstractGridInterceptor implements ClickHandler {

  private UnboundSelector mainItem;

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    presenter.getHeader().clearCommandPanel();
    presenter.getHeader()
        .addCommandItem(new Button(Localized.getConstants().createPurchaseInvoice(), this));
  }

  @Override
  public Map<String, Filter> getInitialParentFilters() {
    return ImmutableMap.of("pyp", Filter.isNull(COL_PURCHASE));
  }

  @Override
  public GridInterceptor getInstance() {
    return new CargoPurchasesGrid();
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
    Queries.getRowSet(getViewName(), null, Filter.idIn(ids), new RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        Set<String> orders = new HashSet<>();
        Map<Long, Pair<String, Integer>> suppliers = new HashMap<>();
        Map<Long, String> currencies = new HashMap<>();

        boolean itemEmpty = false;
        DataInfo info = Data.getDataInfo(getViewName());

        int item = info.getColumnIndex(ClassifierConstants.COL_ITEM);
        int order = info.getColumnIndex(COL_ORDER_NO);
        int suplId = info.getColumnIndex(COL_TRADE_SUPPLIER);
        int suplName = info.getColumnIndex("SupplierName");
        int currId = info.getColumnIndex(COL_CURRENCY);
        int currName = info.getColumnIndex(ALS_CURRENCY_NAME);

        for (BeeRow row : result.getRows()) {
          if (!itemEmpty) {
            itemEmpty = row.getLong(item) == null;
          }
          orders.add(row.getString(order));

          Long id = row.getLong(suplId);
          if (DataUtils.isId(id)) {
            suppliers.put(id, Pair.of(row.getString(suplName),
                row.getInteger(info.getColumnIndex("CreditDays"))));
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

        if (suppliers.size() == 1) {
          for (Entry<Long, Pair<String, Integer>> entry : suppliers.entrySet()) {
            newRow.setValue(purchaseInfo.getColumnIndex(COL_TRADE_SUPPLIER), entry.getKey());
            newRow.setValue(purchaseInfo.getColumnIndex("SupplierName"), entry.getValue().getA());

            Integer days = entry.getValue().getB();

            if (BeeUtils.isPositive(days)) {
              newRow.setValue(purchaseInfo.getColumnIndex(COL_TRADE_TERM),
                  TimeUtils.nextDay(newRow.getDateTime(purchaseInfo.getColumnIndex(COL_DATE)),
                      days));
            }
          }
        }
        if (currencies.size() == 1) {
          for (Entry<Long, String> entry : currencies.entrySet()) {
            newRow.setValue(purchaseInfo.getColumnIndex(COL_CURRENCY), entry.getKey());
            newRow.setValue(purchaseInfo.getColumnIndex(ALS_CURRENCY_NAME), entry.getValue());
          }
        }
        RowFactory.createRow(FORM_NEW_CARGO_PURCHASE_INVOICE, null, purchaseInfo, newRow, null,
            new AbstractFormInterceptor() {
              @Override
              public FormInterceptor getInstance() {
                return this;
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
              }
            },
            new RowCallback() {
              @Override
              public void onCancel() {
                mainItem = null;
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
