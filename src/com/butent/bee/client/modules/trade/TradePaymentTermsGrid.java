package com.butent.bee.client.modules.trade;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.utils.BeeUtils;

class TradePaymentTermsGrid extends AbstractGridInterceptor {

  TradePaymentTermsGrid() {
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradePaymentTermsGrid();
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
    if (gridView != null && !gridView.isEmpty() && newRow != null && !copy) {
      FormView formView = ViewHelper.getForm(gridView);

      if (formView != null && formView.getFormInterceptor() instanceof TradeDocumentForm) {
        double total = ((TradeDocumentForm) formView.getFormInterceptor()).getTotal();

        String colName = TradeConstants.COL_TRADE_PAYMENT_TERM_AMOUNT;
        int index = getDataIndex(colName);

        for (IsRow row : gridView.getRowData()) {
          total -= BeeUtils.unbox(row.getDouble(index));
        }

        total = BeeUtils.round(total, Data.getColumnScale(getViewName(), colName));

        if (BeeUtils.isPositive(total)) {
          newRow.setValue(index, total);
        }
      }
    }

    return super.onStartNewRow(gridView, oldRow, newRow, copy);
  }
}
