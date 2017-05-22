package com.butent.bee.client.modules.service;

import static com.butent.bee.shared.modules.orders.OrdersConstants.SVC_GET_ERP_STOCKS;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.modules.orders.OrdersKeeper;
import com.butent.bee.client.modules.trade.InvoiceERPForm;
import com.butent.bee.client.modules.trade.TradeDocumentRenderer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.ui.Action;

public class ServiceInvoiceForm extends InvoiceERPForm {

  ServiceInvoiceForm() {
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    if (action == Action.PRINT) {
      IsRow row = getActiveRow();

      if (DataUtils.hasId(row)) {
        DataInfo dataInfo = Data.getDataInfo(getFormView().getViewName());
        TradeDocumentRenderer renderer = new TradeDocumentRenderer(TradeConstants.VIEW_SALE_ITEMS,
            TradeConstants.COL_SALE);

        RowEditor.openForm("PrintServiceInvoice", dataInfo, row, Opener.MODAL, null, renderer);
      }

      return false;

    } else {
      return super.beforeAction(action, presenter);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new ServiceInvoiceForm();
  }

  @Override
  public void getERPStocks(final Long id) {
    ParameterList params = OrdersKeeper.createSvcArgs(SVC_GET_ERP_STOCKS);
    params.addDataItem(Service.VAR_DATA, DataUtils.buildIdList(id));

    BeeKeeper.getRpc().makeRequest(params);
  }
}
