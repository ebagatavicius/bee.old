package com.butent.bee.client.modules.trade;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashSet;
import java.util.Set;

public class InvoicesGrid extends AbstractGridInterceptor implements ClickHandler {

  private final Button action = new Button(Localized.getConstants().trSendToERP(), this);
  private final Image loading = new Image(Global.getImages().loading());
  private final Simple panel = new Simple();

  @Override
  public void afterCreatePresenter(final GridPresenter presenter) {
    Global.getParameter(AdministrationConstants.PRM_ERP_ADDRESS, new Consumer<String>() {
      @Override
      public void accept(String address) {
        if (!BeeUtils.isEmpty(address)) {
          presenter.getHeader().addCommandItem(panel);
          setWaiting(false);
        }
      }
    });
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
      view.notifyWarning(Localized.getConstants().selectAtLeastOneRow());
      return;
    }
    Global.confirm(Localized.getConstants().trSendToERPConfirm(), new ConfirmationCallback() {
      @Override
      public void onConfirm() {
        setWaiting(true);
        ParameterList args = TradeKeeper.createArgs(TradeConstants.SVC_SEND_TO_ERP);
        args.addDataItem(TradeConstants.VAR_VIEW_NAME, view.getViewName());
        args.addDataItem(TradeConstants.VAR_ID_LIST, DataUtils.buildIdList(ids));

        BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            setWaiting(false);
            response.notify(view);

            if (!response.hasErrors()) {
              Data.onViewChange(view.getViewName(), DataChangeEvent.RESET_REFRESH);
            }
          }
        });
      }
    });
  }

  private void setWaiting(boolean waiting) {
    panel.setWidget(waiting ? loading : action);
  }
}
