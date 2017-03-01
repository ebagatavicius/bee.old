package com.butent.bee.client.modules.finance;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.i18n.Localized;

class TradeDocumentFinancialRecordsGrid extends FinanceGrid {

  TradeDocumentFinancialRecordsGrid() {
    super();
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeDocumentFinancialRecordsGrid();
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    if (presenter != null && presenter.getHeader() != null
        && BeeKeeper.getUser().canCreateData(VIEW_FINANCIAL_RECORDS)) {

      Button post = new Button(Localized.dictionary().finPostAction(), event -> post());
      post.addStyleName(TradeKeeper.STYLE_PREFIX + "fin-post");

      presenter.getHeader().addCommandItem(post);
    }

    super.afterCreatePresenter(presenter);
  }

  private void post() {
    final FormView form = ViewHelper.getForm(getGridView());

    if (form != null && DataUtils.hasId(form.getActiveRow())) {
      form.saveChanges(new RowCallback() {
        @Override
        public void onSuccess(BeeRow result) {
          if (DataUtils.hasId(result)) {
            ParameterList params = FinanceKeeper.createArgs(SVC_POST_TRADE_DOCUMENT);
            params.addQueryItem(Service.VAR_ID, result.getId());

            BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject response) {
                GridView gridView = getGridView();

                if (gridView != null && response != null) {
                  response.notify(gridView);

                  if (response.hasResponse() && getGridPresenter() != null) {
                    getGridPresenter().refresh(false, true);
                  }
                }
              }
            });
          }
        }
      });
    }
  }
}
