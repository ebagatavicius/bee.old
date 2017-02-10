package com.butent.bee.client.modules.trade;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.CustomAction;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashSet;
import java.util.Set;

public class InvoicesGrid extends AbstractGridInterceptor implements ClickHandler {

  private final CustomAction erpAction = new CustomAction(FontAwesome.CLOUD_UPLOAD, this);

  @Override
  public void afterCreatePresenter(final GridPresenter presenter) {
    if (!BeeUtils.isEmpty(Global.getParameterText(AdministrationConstants.PRM_ERP_ADDRESS))) {
      erpAction.setTitle(Localized.dictionary().trSendToERP());
      presenter.getHeader().addCommandItem(erpAction);
    }
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
            Data.onViewChange(view.getViewName(), DataChangeEvent.RESET_REFRESH);
          }
        }
      });
    });
  }

  public void getERPStocks(Set<Long> ids) {
  }
}
