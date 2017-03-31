package com.butent.bee.client.modules.trade;

import com.butent.bee.client.data.Provider;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class TradeDocumentsGrid extends AbstractGridInterceptor {

  private Supplier<Filter> filterSupplier;

  public TradeDocumentsGrid() {
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    maybeRefresh(presenter, filterSupplier);
    super.afterCreatePresenter(presenter);
  }

  @Override
  public Set<Action> getDisabledActions(Set<Action> defaultActions) {
    if (Objects.nonNull(filterSupplier)) {
      Set<Action> actionSet = new HashSet<>();
      actionSet.add(Action.ADD);

      if (!BeeUtils.isEmpty(defaultActions)) {
        actionSet.addAll(defaultActions);
      }
      return actionSet;
    }
    return super.getDisabledActions(defaultActions);
  }

  @Override
  public Map<String, Filter> getInitialParentFilters(Collection<UiOption> uiOptions) {
    if (Objects.nonNull(filterSupplier)) {
      return Provider.createDefaultParentFilters(filterSupplier.get());
    }
    return super.getInitialParentFilters(uiOptions);
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeDocumentsGrid();
  }

  @Override
  public boolean isRowEditable(IsRow row) {
    return super.isRowEditable(row) && TradeUtils.isDocumentEditable(row);
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    maybeRefresh(getGridPresenter(), filterSupplier);
    super.onParentRow(event);
  }

  public TradeDocumentsGrid setFilterSupplier(Supplier<Filter> filterSupplier) {
    this.filterSupplier = filterSupplier;
    return this;
  }

  private static void maybeRefresh(GridPresenter presenter, Supplier<Filter> supplier) {
    if (BeeUtils.allNotNull(presenter, supplier)) {
      presenter.getDataProvider().setDefaultParentFilter(supplier.get());
      presenter.handleAction(Action.REFRESH);
    }
  }
}
