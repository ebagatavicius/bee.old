package com.butent.bee.client.modules.service;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Map;

public class ObjectInvoicesGrid extends AbstractGridInterceptor {

  private final String idColumnName;

  private boolean filterByMaintenance;
  private Long pendingId;

  ObjectInvoicesGrid() {
    this.idColumnName = Data.getIdColumn(VIEW_SERVICE_INVOICES);
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    maybeRefresh(presenter, getPendingId());
    setPendingId(null);
  }

  @Override
  public boolean beforeAction(Action action, GridPresenter presenter) {
    if (action == Action.ADD) {
      InvoiceBuilder.start(getGridView());
      return false;
    } else {
      return super.beforeAction(action, presenter);
    }
  }

  @Override
  public Map<String, Filter> getInitialParentFilters(Collection<UiOption> uiOptions) {
    return Provider.createDefaultParentFilters(getFilter(getPendingId()));
  }

  @Override
  public GridInterceptor getInstance() {
    return new ObjectInvoicesGrid();
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    setPendingId(event.getRowId());
    if (BeeUtils.same(event.getViewName(), TBL_SERVICE_MAINTENANCE)) {
      setFilterByMaintenance(true);
    }

    if (getGridPresenter() != null) {
      maybeRefresh(getGridPresenter(), event.getRowId());
    }
  }

  private Filter getFilter(Long parentId) {
    Filter filter;

    if (DataUtils.isId(parentId)) {
      filter = Filter.or(Filter.in(idColumnName, VIEW_MAINTENANCE, COL_MAINTENANCE_INVOICE,
          Filter.equals(
              isFilterByMaintenance() ? COL_SERVICE_MAINTENANCE : COL_SERVICE_OBJECT, parentId)),
          Filter.equals(COL_SERVICE_OBJECT, parentId));
    } else {
      filter = Filter.isFalse();
    }

    return filter;
  }

  private Long getPendingId() {
    return pendingId;
  }

  private void maybeRefresh(GridPresenter presenter, Long parentId) {
    if (presenter != null) {
      Filter filter = getFilter(parentId);
      boolean changed = presenter.getDataProvider().setDefaultParentFilter(filter);

      if (changed) {
        presenter.handleAction(Action.REFRESH);
      }
    }
  }

  private void setPendingId(Long pendingId) {
    this.pendingId = pendingId;
  }

  private boolean isFilterByMaintenance() {
    return filterByMaintenance;
  }

  private void setFilterByMaintenance(boolean filterByMaintenance) {
    this.filterByMaintenance = filterByMaintenance;
  }
}
