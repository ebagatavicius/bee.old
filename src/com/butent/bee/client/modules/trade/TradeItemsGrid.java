package com.butent.bee.client.modules.trade;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.utils.BeeUtils;

public class TradeItemsGrid extends AbstractGridInterceptor {

  private final ScheduledCommand refresher;

  public TradeItemsGrid() {
    this.refresher = createRefresher();
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
  public void afterUpdateCell(IsColumn column, String oldValue, String newValue, IsRow result,
      boolean rowMode) {
    if (BeeUtils.inListSame(column.getId(), COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
        COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)) {
      refresher.execute();
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeItemsGrid();
  }

  private ScheduledCommand createRefresher() {
    return new ScheduledCommand() {
      @Override
      public void execute() {
        FormView form = ViewHelper.getForm(getGridView());

        final String viewName = (form == null) ? null : form.getViewName();
        final Long rowId = (form == null) ? null : form.getActiveRowId();

        if (DataUtils.isId(rowId)) {
          Queries.getRow(viewName, rowId, new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              if (result != null) {
                RowUpdateEvent.fire(BeeKeeper.getBus(), viewName, result);
              }
            }
          });
        }
      }
    };
  }
}
