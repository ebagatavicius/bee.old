package com.butent.bee.client.modules.cars;

import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_ITEM;
import static com.butent.bee.shared.modules.trade.TradeConstants.VIEW_TRADE_DOCUMENTS;

import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.ui.Action;

import java.util.Objects;

public class CarServiceInvoicesGrid extends AbstractGridInterceptor {
  private Long parent;

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    refresh(presenter, parent);
    super.afterCreatePresenter(presenter);
  }

  @Override
  public GridInterceptor getInstance() {
    return new CarServiceInvoicesGrid();
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    parent = event.getRowId();
    refresh(getGridPresenter(), parent);
    super.onParentRow(event);
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    RowEditor.open(VIEW_TRADE_DOCUMENTS, getActiveRowId(), Opener.NEW_TAB);
    event.consume();
  }

  private static void refresh(GridPresenter presenter, Long id) {
    if (Objects.nonNull(presenter)) {
      Filter filter = DataUtils.isId(id) ? Filter.or(Filter.equals(COL_ITEM + COL_ORDER, id),
          Filter.equals(COL_JOB + COL_ORDER, id)) : Filter.isFalse();

      presenter.getDataProvider().setDefaultParentFilter(filter);
      presenter.handleAction(Action.REFRESH);
    }
  }
}
