package com.butent.bee.client.modules.trade;

import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;

public class TradeDocumentsGrid extends AbstractGridInterceptor {

  TradeDocumentsGrid() {
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeDocumentsGrid();
  }

  @Override
  public boolean isRowEditable(IsRow row) {
    return super.isRowEditable(row) && TradeUtils.isDocumentEditable(row);
  }
}
