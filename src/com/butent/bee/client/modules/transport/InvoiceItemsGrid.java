package com.butent.bee.client.modules.transport;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;

public class InvoiceItemsGrid extends AbstractGridInterceptor {
  private final ScheduledCommand refresher;

  public InvoiceItemsGrid(ScheduledCommand refresher) {
    this.refresher = refresher;
  }

  @Override
  public void afterDeleteRow(long rowId) {
    refresher.execute();
  }

  @Override
  public void afterInsertRow(IsRow result) {
    refresher.execute();
  }

  @Override
  public void afterUpdateCell(IsColumn column, IsRow result, boolean rowMode) {
    if (BeeUtils.inListSame(column.getId(), COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
        COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)) {
      refresher.execute();
    }
  }
  
  @Override
  public GridInterceptor getInstance() {
    return new InvoiceItemsGrid(refresher);
  }
}
