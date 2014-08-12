package com.butent.bee.client.modules.service;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.modules.trade.TradeDocumentRenderer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.ui.Action;

public class ServiceInvoiceForm extends AbstractFormInterceptor {

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
}
