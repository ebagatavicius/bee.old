package com.butent.bee.client.modules.orders;

import com.google.gwt.event.dom.client.ClickEvent;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.orders.OrdersConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.modules.trade.InvoicesGrid;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class OrdersInvoicesGrid extends InvoicesGrid {

  private Button joinAction = new Button(Localized.dictionary().ecOrdersJoin(), this);

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    presenter.getHeader().addCommandItem(joinAction);

    super.afterCreatePresenter(presenter);
  }

  @Override
  public GridInterceptor getInstance() {
    return this;
  }

  @Override
  public void getERPStocks(final Set<Long> ids) {
    ParameterList params = OrdersKeeper.createSvcArgs(SVC_GET_ERP_STOCKS);
    params.addDataItem(Service.VAR_DATA, DataUtils.buildIdList(ids));

    BeeKeeper.getRpc().makeRequest(params);
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {

    int exportedIdx = Data.getColumnIndex(getViewName(), TradeConstants.COL_TRADE_EXPORTED);

    if (exportedIdx < 0) {
      return null;
    }

    DateTime exported = activeRow.getDateTime(exportedIdx);

    if (BeeKeeper.getUser().isAdministrator()) {
      return DeleteMode.SINGLE;
    } else if (exported != null) {
      getGridView().notifySevere(Localized.dictionary().rowIsNotRemovable());
      return DeleteMode.CANCEL;
    } else {
      return DeleteMode.SINGLE;
    }
  }

  @Override
  public void onClick(ClickEvent event) {
    if (Objects.equals(event.getSource(), joinAction)) {
      GridView view = getGridView();
      Set<Long> ids = new HashSet<>();

      for (RowInfo row : view.getSelectedRows(GridView.SelectedRows.ALL)) {
        ids.add(row.getId());
      }
      if (ids.isEmpty()) {
        view.notifyWarning(Localized.dictionary().selectAtLeastOneRow());
        return;
      }
      joinAction.setVisible(false);
      List<String> cols = Arrays.asList(COL_TRADE_OPERATION, COL_TRADE_OPERATION + "Name",
          COL_TRADE_CUSTOMER, COL_TRADE_CUSTOMER + "Name", COL_TRADE_WAREHOUSE_FROM,
          COL_TRADE_WAREHOUSE_FROM + "Code", COL_COMPANY_CREDIT_DAYS);

      Queries.getRowSet(getViewName(), null, Filter.idIn(ids), new Queries.RowSetCallback() {
        @Override
        public void onSuccess(BeeRowSet result) {
          joinAction.setVisible(true);

          for (String col : new String[] {COL_TRADE_EXPORTED, COL_TRADE_CUSTOMER,
              COL_TRADE_OPERATION, COL_TRADE_WAREHOUSE_FROM}) {
            int idx = result.getColumnIndex(col);

            if (result.getDistinctStrings(idx).size() > 1) {
              view.notifyWarning(BeeUtils.joinWords(Localized.dictionary().moreThenOneValue(),
                  Localized.getLabel(result.getColumn(idx))));
              return;
            }
          }

          int idx = result.getColumnIndex(COL_TRADE_JOIN);
          if (result.getDistinctStrings(idx).contains(null)) {
            view.notifyWarning(BeeUtils.joinWords(Localized.dictionary().trInvoiceHasNotAttribute(),
                Localized.getLabel(result.getColumn(idx))));
            return;
          }

          DataInfo dataInfo = Data.getDataInfo(getViewName());
          BeeRow newRow = RowFactory.createEmptyRow(dataInfo, true);

          newRow.setValue(dataInfo.getColumnIndex(COL_TRADE_MANAGER),
              BeeKeeper.getUser().getUserId());
          newRow.setValue(dataInfo.getColumnIndex(COL_TRADE_MANAGER + COL_PERSON),
              BeeKeeper.getUser().getUserData().getCompanyPerson());
          newRow.setValue(dataInfo.getColumnIndex(COL_TRADE_MANAGER + COL_FIRST_NAME),
              BeeKeeper.getUser().getFirstName());
          newRow.setValue(dataInfo.getColumnIndex(COL_TRADE_MANAGER + COL_LAST_NAME),
              BeeKeeper.getUser().getLastName());
          newRow.setValue(dataInfo.getColumnIndex(TradeConstants.COL_TRADE_SUPPLIER),
              BeeKeeper.getUser().getCompany());
          newRow.setValue(dataInfo.getColumnIndex(ALS_SUPPLIER_NAME),
              BeeKeeper.getUser().getCompanyName());

          Integer days = result.getInteger(0, result.getColumnIndex(COL_COMPANY_CREDIT_DAYS));

          if (BeeUtils.isPositive(days)) {
            newRow.setValue(dataInfo.getColumnIndex(TradeConstants.COL_TRADE_TERM),
              TimeUtils.nextDay(TimeUtils.today(), days));
          }

          for (String col : cols) {
              newRow.setValue(dataInfo.getColumnIndex(col),
                  BeeUtils.joinItems(result.getDistinctStrings(result.getColumnIndex(col))));
          }

          RowFactory.createRow(dataInfo.getNewRowForm(), dataInfo.getNewRowCaption(), dataInfo,
              newRow, Modality.ENABLED, null, new NewOrderInvoiceForm(), null, new RowCallback() {
                @Override
                public void onSuccess(BeeRow newInvoice) {
                  Queries.update(VIEW_SALE_ITEMS, Filter.any(COL_SALE, ids), COL_SALE,
                      BeeUtils.toString(newInvoice.getId()), new Queries.IntCallback() {
                        @Override
                        public void onSuccess(Integer res1) {
                          Queries.delete(VIEW_SALES, Filter.idIn(ids), new Queries.IntCallback() {
                            @Override
                            public void onSuccess(Integer result) {
                              Data.onViewChange(VIEW_ORDER_CHILD_INVOICES,
                                  DataChangeEvent.RESET_REFRESH);
                            }
                          });
                        }
                      });
                }
              });
        }
      });

    } else {
      super.onClick(event);
    }
  }
}