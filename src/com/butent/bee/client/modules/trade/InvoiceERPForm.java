package com.butent.bee.client.modules.trade;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.trade.TradeConstants.COL_SALE_PROFORMA;
import static com.butent.bee.shared.modules.trade.TradeConstants.COL_TRADE_EXPORTED;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.client.widget.CustomAction;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.utils.BeeUtils;

public abstract class InvoiceERPForm extends PrintFormInterceptor implements ClickHandler {

  private CustomAction erpAction;

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    if (erpAction == null) {
      erpAction = new CustomAction(FontAwesome.CLOUD_UPLOAD, this);
      HeaderView header = form.getViewPresenter().getHeader();
      erpAction.setTitle(Localized.dictionary().trSendToERP());
      header.addCommandItem(erpAction);
    }

    int proformaIndex = getDataIndex(COL_SALE_PROFORMA);
    int exportedIndex = getDataIndex(COL_TRADE_EXPORTED);
    erpAction.setVisible(!BeeUtils.isEmpty(Global
        .getParameterText(AdministrationConstants.PRM_ERP_ADDRESS))
        && (BeeConst.isUndef(proformaIndex) || BeeUtils.isEmpty(row.getString(proformaIndex)))
        && (BeeConst.isUndef(exportedIndex) || row.getDateTime(exportedIndex) == null));
    super.afterRefresh(form, row);
  }

  public abstract void getERPStocks(Long id);

  @Override
  public FormInterceptor getPrintFormInterceptor() {
    return new PrintInvoiceInterceptor();
  }

  @Override
  public void onClick(ClickEvent event) {
    final Long invoiceId = getActiveRowId();

    if (!DataUtils.isId(invoiceId)) {
      return;
    }
    Global.confirm(Localized.dictionary().trSendToERPConfirm(), () -> {
      erpAction.running();
      ParameterList args = TradeKeeper.createArgs(TradeConstants.SVC_SEND_TO_ERP);
      args.addDataItem(TradeConstants.VAR_VIEW_NAME, getViewName());
      args.addDataItem(TradeConstants.VAR_ID_LIST, DataUtils.buildIdList(invoiceId));

      BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          erpAction.idle();

          if (!response.hasErrors()) {
            getERPStocks(invoiceId);
            Data.onViewChange(getViewName(), DataChangeEvent.RESET_REFRESH);
          }
        }
      });
    });
  }
}
