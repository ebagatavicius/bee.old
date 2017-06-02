package com.butent.bee.client.modules.trade;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.CustomAction;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InvoicesGrid extends AbstractGridInterceptor implements ClickHandler {

  private final CustomAction erpAction = new CustomAction(FontAwesome.CLOUD_UPLOAD, this);
  private final CustomAction payAction = new CustomAction(FontAwesome.CREDIT_CARD,
      ev -> setPayment());

  @Override
  public void afterCreatePresenter(final GridPresenter presenter) {
    if (!BeeUtils.isEmpty(Global.getParameterText(AdministrationConstants.PRM_ERP_ADDRESS))) {
      erpAction.setTitle(Localized.dictionary().trSendToERP());
      presenter.getHeader().addCommandItem(erpAction);
    }
    payAction.setTitle(Localized.dictionary().pay());
    presenter.getHeader().addCommandItem(payAction);
  }

  @Override
  public GridInterceptor getInstance() {
    return new InvoicesGrid();
  }

  @Override
  public void onClick(ClickEvent event) {
    final GridView view = getGridView();
    final Set<Long> ids = new HashSet<>();

    for (RowInfo row : view.getSelectedRows(SelectedRows.ALL)) {
      ids.add(row.getId());
    }
    if (ids.isEmpty()) {
      view.notifyWarning(Localized.dictionary().selectAtLeastOneRow());
      return;
    }
    Global.confirm(Localized.dictionary().trSendToERPConfirm(), () -> {
      erpAction.running();
      ParameterList args = TradeKeeper.createArgs(TradeConstants.SVC_SEND_TO_ERP);
      args.addDataItem(TradeConstants.VAR_VIEW_NAME, view.getViewName());
      args.addDataItem(TradeConstants.VAR_ID_LIST, DataUtils.buildIdList(ids));

      BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          erpAction.idle();
          response.notify(view);

          if (!response.hasErrors()) {
            getERPStocks(ids);
            Data.resetLocal(view.getViewName());
          }
        }
      });
    });
  }

  private void setPayment() {
    final GridView view = getGridView();
    final Set<Long> ids = new HashSet<>();

    for (RowInfo row : view.getSelectedRows(SelectedRows.ALL)) {
      ids.add(row.getId());
    }
    if (ids.isEmpty()) {
      view.notifyWarning(Localized.dictionary().selectAtLeastOneRow());
      return;
    }
    InputDateTime paymentDate = new InputDateTime();

    Global.inputWidget(Localized.dictionary().trdPaymentTime(), paymentDate, new InputCallback() {
      @Override
      public String getErrorMessage() {
        if (BeeUtils.isEmpty(paymentDate.getValue())) {
          return Localized.dictionary().valueRequired();
        }
        return InputCallback.super.getErrorMessage();
      }

      @Override
      public void onSuccess() {
        payAction.running();

        Filter filter = Filter.and(Filter.idIn(ids), Filter.notNull(TransportConstants.COL_AMOUNT),
            Filter.or(Filter.isNull(TradeConstants.COL_TRADE_PAID),
                Filter.compareWithColumn(TransportConstants.COL_AMOUNT, Operator.NE,
                    TradeConstants.COL_TRADE_PAID)));

        Queries.getRowSet(getViewName(), Collections.singletonList(TransportConstants.COL_AMOUNT),
            filter, new Queries.RowSetCallback() {
              @Override
              public void onSuccess(BeeRowSet result) {
                if (!DataUtils.isEmpty(result)) {
                  BeeRowSet rowSet = new BeeRowSet(getViewName(), Data.getColumns(getViewName(),
                      TradeConstants.COL_TRADE_PAYMENT_TIME, TradeConstants.COL_TRADE_PAID));

                  for (BeeRow row : result) {
                    rowSet.addRow(new BeeRow(row.getId(), row.getVersion(),
                        Arrays.asList(paymentDate.getNormalizedValue(), row.getString(0))));
                  }
                  Queries.updateRows(rowSet, new RpcCallback<RowInfoList>() {
                    @Override
                    public void onSuccess(RowInfoList info) {
                      payAction.idle();
                      Data.resetLocal(view.getViewName());
                    }
                  });
                } else {
                  payAction.idle();
                }
              }
            });
      }
    });
  }

  public void getERPStocks(Set<Long> ids) {
  }
}
