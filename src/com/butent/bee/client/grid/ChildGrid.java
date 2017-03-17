package com.butent.bee.client.grid;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.grid.GridSettings;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Launchable;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.ProviderType;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

/**
 * Enables using data grids with data related to another source.
 */

public class ChildGrid extends EmbeddedGrid implements Launchable {

  private static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "ChildGrid";

  private static final Collection<UiOption> uiOptions = EnumSet.of(UiOption.CHILD);

  private final int parentIndex;
  private final String relSource;

  private final boolean disablable;

  private GridDescription gridDescription;

  private IsRow pendingRow;
  private Boolean pendingEnabled;

  public ChildGrid(String gridName, GridFactory.GridOptions gridOptions,
      int parentIndex, String relSource, boolean disablable) {

    super(gridName, gridOptions);

    this.parentIndex = parentIndex;
    this.relSource = relSource;
    this.disablable = disablable;

    addStyleName(STYLE_NAME);
  }

  @Override
  public String getIdPrefix() {
    return "child-grid";
  }

  @Override
  public void launch() {
    GridFactory.getGridDescription(getGridName(), result -> {
      if (getGridInterceptor() != null && !getGridInterceptor().initDescription(result)) {
        return;
      }

      setGridDescription(GridSettings.apply(getGridKey(relSource), result));
      resolveState(false);
    });
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    super.onParentRow(event);

    setPendingRow(event.getRow());
    if (isDisablable()) {
      setPendingEnabled(event.isEnabled());
    } else {
      setPendingEnabled(event.getRow() != null);
    }

    resolveState(false);
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (getPresenter() != null) {
      getPresenter().getMainView().setEnabled(enabled);
    } else {
      setPendingEnabled(enabled);
    }
  }

  private void createPresenter() {
    Filter immutableFilter = GridFactory.getImmutableFilter(getGridDescription(), getGridOptions());
    Map<String, Filter> initialFilters = (getGridInterceptor() == null)
        ? null : getGridInterceptor().getInitialParentFilters(uiOptions);

    Order order = getGridDescription().getOrder();

    DataInfo dataInfo = Data.getDataInfo(getGridDescription().getViewName());
    if (dataInfo == null) {
      return;
    }

    GridView gridView = GridFactory.createGridView(getGridDescription(), getGridKey(relSource),
        dataInfo.getColumns(), getRelSource(), uiOptions, getGridInterceptor(), order,
        getGridOptions());

    gridView.getGrid().setPageSize(BeeConst.UNDEF, false);
    afterCreateGrid(gridView);

    BeeRowSet rowSet = new BeeRowSet(dataInfo.getViewName(), dataInfo.getColumns());
    gridView.initData(rowSet.getNumberOfRows(), rowSet);

    GridPresenter gp = new GridPresenter(getGridDescription(), gridView,
        rowSet.getNumberOfRows(), rowSet, ProviderType.ASYNC, getCachingPolicy(),
        uiOptions, getGridInterceptor(), immutableFilter, initialFilters, null, null,
        order, getGridOptions());

    gp.setEventSource(getId());
    setPresenter(gp);

    gp.getMainView().setEnabled(false);

    gridView.addReadyHandler(event -> resolveState(true));

    setWidget(gp.getMainView());

    if (getGridInterceptor() != null) {
      getGridInterceptor().afterCreatePresenter(gp);
    }
  }

  private CachingPolicy getCachingPolicy() {
    return BeeUtils.isTrue(getGridDescription().getCacheData())
        ? CachingPolicy.FULL : CachingPolicy.NONE;
  }

  private Filter getFilter(IsRow row) {
    return Filter.equals(getRelSource(), getParentValue(row));
  }

  private GridDescription getGridDescription() {
    return gridDescription;
  }

  private int getParentIndex() {
    return parentIndex;
  }

  private Long getParentValue(IsRow row) {
    if (row == null) {
      return null;
    } else if (getParentIndex() >= 0) {
      return row.getLong(getParentIndex());
    } else {
      return row.getId();
    }
  }

  private Boolean getPendingEnabled() {
    return pendingEnabled;
  }

  private IsRow getPendingRow() {
    return pendingRow;
  }

  private String getRelSource() {
    return relSource;
  }

  private boolean hasParentValue(IsRow row) {
    return DataUtils.isId(getParentValue(row));
  }

  private boolean isDisablable() {
    return disablable;
  }

  private void resetState() {
    if (getPendingEnabled() != null) {
      setEnabled(getPendingEnabled());
      setPendingEnabled(null);
    }
    setPendingRow(null);
  }

  private void resolveState(boolean ready) {
    if (getGridDescription() == null) {
      return;
    }

    if (getPresenter() == null) {
      createPresenter();

    } else if (ready || getPresenter().isReady()) {
      getPresenter().getGridView().getGrid().deactivate();

      if (updateFilter(getPendingRow())) {
        getPresenter().getGridView().getGrid().clearSelection();
      }

      if (!getPresenter().getGridView().isAdding()) {
        if (hasParentValue(getPendingRow())) {
          getPresenter().refresh(false, true);
        } else {
          getPresenter().getDataProvider().clear();
        }
      }

      resetState();
    }
  }

  private void setGridDescription(GridDescription gridDescription) {
    this.gridDescription = gridDescription;
  }

  public void setPendingEnabled(Boolean pendingEnabled) {
    this.pendingEnabled = pendingEnabled;
  }

  private void setPendingRow(IsRow pendingRow) {
    this.pendingRow = pendingRow;
  }

  private boolean updateFilter(IsRow row) {
    if (getPresenter() != null) {
      getPresenter().getGridView().setRelId(getParentValue(row));

      Provider provider = getPresenter().getDataProvider();

      boolean changed = provider.setParentFilter(getId(), getFilter(row));

      if (changed && provider.getUserFilter() != null) {
        provider.setUserFilter(null);

        if (getPresenter().getFilterManager() != null) {
          getPresenter().getFilterManager().clearFilter();
        }

        HeaderView header = getPresenter().getHeader();
        if (header != null && header.hasAction(Action.REMOVE_FILTER)) {
          header.showAction(Action.REMOVE_FILTER, false);
        }
      }

      return changed;

    } else {
      return false;
    }
  }
}
