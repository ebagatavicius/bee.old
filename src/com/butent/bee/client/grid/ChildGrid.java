package com.butent.bee.client.grid;

import com.google.common.base.Objects;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.ReadyEvent.HasReadyHandlers;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.HasFosterParent;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HasGridView;
import com.butent.bee.client.view.grid.GridSettings;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
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
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

/**
 * Enables using data grids with data related to another source.
 */

public class ChildGrid extends Simple implements EnablableWidget, Launchable, HasFosterParent,
    ParentRowEvent.Handler, HasGridView, ReadyEvent.HasReadyHandlers {

  private static final Collection<UiOption> uiOptions = EnumSet.of(UiOption.CHILD);

  private final String gridName;

  private final int parentIndex;
  private final String relSource;

  private final GridFactory.GridOptions gridOptions;

  private final boolean disablable;

  private GridInterceptor gridInterceptor;
  private GridDescription gridDescription;
  private GridPresenter presenter;

  private IsRow pendingRow;
  private Boolean pendingEnabled;

  private String parentId;
  private HandlerRegistration parentRowReg;

  public ChildGrid(String gridName, int parentIndex, String relSource,
      GridFactory.GridOptions gridOptions, boolean disablable) {
    super();

    this.gridName = gridName;
    this.parentIndex = parentIndex;
    this.relSource = relSource;
    this.gridOptions = gridOptions;
    this.disablable = disablable;

    this.gridInterceptor = GridFactory.getGridInterceptor(gridName);

    addStyleName("bee-ChildGrid");
  }

  @Override
  public com.google.gwt.event.shared.HandlerRegistration addReadyHandler(
      ReadyEvent.Handler handler) {

    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public GridView getGridView() {
    return getPresenter() == null ? null : getPresenter().getGridView();
  }

  @Override
  public String getIdPrefix() {
    return "child-grid";
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  public GridPresenter getPresenter() {
    return presenter;
  }

  @Override
  public boolean isEnabled() {
    if (getPresenter() == null) {
      return false;
    }
    return getPresenter().getMainView().isEnabled();
  }

  @Override
  public void launch() {
    GridFactory.getGridDescription(gridName, new Callback<GridDescription>() {
      @Override
      public void onSuccess(GridDescription result) {
        if (getGridInterceptor() != null && !getGridInterceptor().initDescription(result)) {
          return;
        }

        setGridDescription(GridSettings.apply(getGridKey(), result));
        resolveState();
      }
    });
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    if (getGridInterceptor() != null) {
      getGridInterceptor().onParentRow(event);
    }

    setPendingRow(event.getRow());
    if (isDisablable()) {
      setPendingEnabled(event.isEnabled());
    } else {
      setPendingEnabled(event.getRow() != null);
    }

    resolveState();
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (getPresenter() != null) {
      getPresenter().getMainView().setEnabled(enabled);
    } else {
      setPendingEnabled(enabled);
    }
  }

  public void setGridInterceptor(GridInterceptor gridInterceptor) {
    this.gridInterceptor = gridInterceptor;
  }

  @Override
  public void setParentId(String parentId) {
    this.parentId = parentId;
    if (isAttached()) {
      register();
    }
  }

  @Override
  public void setWidget(Widget w) {
    if (w != null) {
      StyleUtils.makeAbsolute(w);

      if (w instanceof HasReadyHandlers) {
        ReadyEvent.maybeDelegate(this, (HasReadyHandlers) w);
      }
    }

    super.setWidget(w);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    register();
  }

  @Override
  protected void onUnload() {
    unregister();
    super.onUnload();
  }

  private void createPresenter(GridView gridView, IsRow row, BeeRowSet rowSet,
      Filter immutableFilter, Map<String, Filter> initialFilters, Order order) {

    GridPresenter gp = new GridPresenter(getGridDescription(), gridView,
        rowSet.getNumberOfRows(), rowSet, ProviderType.ASYNC, getCachingPolicy(),
        uiOptions, getGridInterceptor(), immutableFilter, initialFilters, null, null,
        order, getGridOptions());

    gp.getGridView().getGrid().setPageSize(BeeConst.UNDEF, false);
    gp.setEventSource(getId());

    setWidget(gp.getMainView());
    setPresenter(gp);

    if (getGridInterceptor() != null) {
      getGridInterceptor().afterCreatePresenter(gp);
    }

    if (Objects.equal(row, getPendingRow())) {
      updateFilter(row);
      resetState();
      if (row == null) {
        setEnabled(false);
      }
    } else {
      resolveState();
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

  private GridInterceptor getGridInterceptor() {
    return gridInterceptor;
  }

  private String getGridKey() {
    return GridFactory.getSupplierKey(gridName, getGridInterceptor());
  }

  private GridFactory.GridOptions getGridOptions() {
    return gridOptions;
  }

  private void getInitialRowSet(final IsRow row) {
    final Filter immutableFilter =
        GridFactory.getImmutableFilter(getGridDescription(), getGridOptions());
    final Map<String, Filter> initialFilters =
        (getGridInterceptor() == null) ? null : getGridInterceptor().getInitialParentFilters();

    final Order order = getGridDescription().getOrder();

    DataInfo dataInfo = Data.getDataInfo(getGridDescription().getViewName());
    if (dataInfo == null) {
      return;
    }

    final GridView gridView = GridFactory.createGridView(getGridDescription(), getGridKey(),
        dataInfo.getColumns(), getRelSource(), getGridInterceptor(), order);

    if (!hasParentValue(row)) {
      BeeRowSet rowSet = new BeeRowSet(dataInfo.getViewName(), dataInfo.getColumns());
      gridView.initData(rowSet.getNumberOfRows(), rowSet);
      createPresenter(gridView, row, rowSet, immutableFilter, initialFilters, order);
      return;
    }

    Filter queryFilter = Filter.and(getFilter(row),
        GridFactory.getInitialQueryFilter(immutableFilter, initialFilters, null));

    Queries.getRowSet(getGridDescription().getViewName(), null, queryFilter, order,
        getCachingPolicy(), new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet rowSet) {
            gridView.initData(rowSet.getNumberOfRows(), rowSet);
            createPresenter(gridView, row, rowSet, immutableFilter, initialFilters, order);
          }
        });
  }

  private int getParentIndex() {
    return parentIndex;
  }

  private HandlerRegistration getParentRowReg() {
    return parentRowReg;
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

  private void register() {
    unregister();
    if (!BeeUtils.isEmpty(getParentId())) {
      setParentRowReg(BeeKeeper.getBus().registerParentRowHandler(getParentId(), this, false));
    }
  }

  private void resetState() {
    if (getPendingEnabled() != null) {
      setEnabled(getPendingEnabled());
      setPendingEnabled(null);
    }
    setPendingRow(null);
  }

  private void resolveState() {
    if (getGridDescription() == null) {
      return;
    }

    if (getPresenter() == null) {
      getInitialRowSet(getPendingRow());

    } else {
      getPresenter().getGridView().getGrid().deactivate();

      updateFilter(getPendingRow());

      if (!getPresenter().getGridView().isAdding()) {
        if (hasParentValue(getPendingRow())) {
          getPresenter().refresh(false);
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

  private void setParentRowReg(HandlerRegistration parentRowReg) {
    this.parentRowReg = parentRowReg;
  }

  private void setPendingEnabled(Boolean pendingEnabled) {
    this.pendingEnabled = pendingEnabled;
  }

  private void setPendingRow(IsRow pendingRow) {
    this.pendingRow = pendingRow;
  }

  private void setPresenter(GridPresenter presenter) {
    this.presenter = presenter;
  }

  private void unregister() {
    if (getParentRowReg() != null) {
      getParentRowReg().removeHandler();
      setParentRowReg(null);
    }
  }

  private void updateFilter(IsRow row) {
    if (getPresenter() != null) {
      getPresenter().getDataProvider().setParentFilter(getId(), getFilter(row));
      getPresenter().getGridView().setRelId(getParentValue(row));
    }
  }
}
