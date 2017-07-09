package com.butent.bee.client.data;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.event.logical.SortEvent;
import com.butent.bee.client.i18n.Collator;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.ModificationPreviewer;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Extends {@code Provider} class, enables to manage data ranges from sources stored directly in
 * memory.
 */

public class CachedProvider extends Provider {

  private static final BeeLogger logger = LogUtils.getLogger(CachedProvider.class);

  private BeeRowSet table;
  private boolean complete;

  private final Set<Long> filteredRowIds = new HashSet<>();
  private final List<BeeRow> viewRows = new ArrayList<>();

  public CachedProvider(HasDataTable display, HasDataProvider presenter,
      ModificationPreviewer modificationPreviewer, NotificationListener notificationListener,
      String viewName, List<BeeColumn> columns, BeeRowSet table) {

    this(display, presenter, modificationPreviewer, notificationListener,
        viewName, columns, null, null,
        null, table, null, null);
  }

  public CachedProvider(HasDataTable display, HasDataProvider presenter,
      ModificationPreviewer modificationPreviewer, NotificationListener notificationListener,
      String viewName, List<BeeColumn> columns, Filter immutableFilter, BeeRowSet table,
      Map<String, Filter> parentFilters, Filter userFilter) {

    this(display, presenter, modificationPreviewer, notificationListener,
        viewName, columns, null, null,
        immutableFilter, table, parentFilters, userFilter);
  }

  public CachedProvider(HasDataTable display, HasDataProvider presenter,
      ModificationPreviewer modificationPreviewer, NotificationListener notificationListener,
      String viewName, List<BeeColumn> columns, String idColumnName, String versionColumnName,
      Filter immutableFilter, BeeRowSet table, Map<String, Filter> parentFilters,
      Filter userFilter) {

    super(display, presenter, modificationPreviewer, notificationListener,
        viewName, columns, idColumnName, versionColumnName,
        immutableFilter, parentFilters, userFilter);

    Assert.notNull(table);
    this.table = table;
    this.complete = userFilter == null;
  }

  public void addRow(BeeRow row) {
    if (row != null) {
      table.addRow(row);

      if (isComplete() && getUserFilter() != null
          && getUserFilter().isMatch(getTable().getColumns(), row)) {
        filteredRowIds.add(row.getId());
        viewRows.add(row);
      }
    }
  }

  @Override
  public void clear() {
    if (getTable().getNumberOfRows() > 0) {
      getTable().clearRows();
    }

    filteredRowIds.clear();
    viewRows.clear();

    super.clear();
  }

  public BeeRowSet getTable() {
    return table;
  }

  @Override
  public void hasAnyRows(Filter filter, Consumer<Boolean> callback) {
    if (DataUtils.isEmpty(getTable())) {
      callback.accept(false);

    } else if (filter == null) {
      callback.accept(true);

    } else {
      boolean ok = false;
      List<BeeColumn> columns = getTable().getColumns();

      for (BeeRow row : getTable()) {
        if (filter.isMatch(columns, row)) {
          ok = true;
          break;
        }
      }

      callback.accept(ok);
    }
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (BeeUtils.same(event.getViewName(), getViewName())) {
      long id = event.getRowId();
      for (BeeRow row : getTable().getRows()) {
        if (row.getId() == id) {
          event.applyTo(row);
          break;
        }
      }

      super.onCellUpdate(event);
    }
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    if (event.hasView(getViewName())) {
      for (RowInfo rowInfo : event.getRows()) {
        deleteRow(rowInfo.getId());
      }

      if (previewMultiDelete(event)) {
        getDisplay().onMultiDelete(event);
        getDisplay().setRowCount(getRowCount(), true);

        onRequest(false);
      }
    }
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (event.hasView(getViewName())) {
      deleteRow(event.getRowId());

      if (previewRowDelete(event)) {
        getDisplay().onRowDelete(event);
        getDisplay().setRowCount(getRowCount(), true);

        onRequest(false);
      }
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (BeeUtils.same(event.getViewName(), getViewName())) {
      addRow(event.getRow());

      super.onRowInsert(event);
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (BeeUtils.same(event.getViewName(), getViewName())) {
      BeeRow newRow = event.getRow();
      long id = newRow.getId();

      for (BeeRow oldRow : getTable().getRows()) {
        if (oldRow.getId() == id) {
          oldRow.setVersion(newRow.getVersion());
          for (int i = 0; i < getTable().getNumberOfColumns(); i++) {
            oldRow.setValue(i, newRow.getString(i));
          }
          break;
        }
      }

      super.onRowUpdate(event);
    }
  }

  @Override
  public void onSort(SortEvent event) {
    Order order = event.getOrder();
    setOrder(order);
    if (getTable().getNumberOfRows() <= 1) {
      return;
    }

    if (order == null || order.isEmpty()) {
      getTable().sortByRowId(true);
    } else {
      List<Pair<Integer, Boolean>> sortList = new ArrayList<>();
      int index;

      for (Order.Column sortInfo : order.getColumns()) {
        for (String source : sortInfo.getSources()) {
          if (BeeUtils.same(source, getIdColumnName())) {
            index = DataUtils.ID_INDEX;
          } else if (BeeUtils.same(source, getVersionColumnName())) {
            index = DataUtils.VERSION_INDEX;
          } else {
            index = getTable().getColumnIndex(source);
            if (index < 0) {
              logger.warning("onSort: source", source, "not found");
              continue;
            }
          }
          sortList.add(Pair.of(index, sortInfo.isAscending()));
        }
      }
      if (!sortList.isEmpty()) {
        getTable().sort(sortList, Collator.DEFAULT);
      }
    }

    if (viewRows.size() > 1) {
      updateViewRows();
    }
    goTop();
  }

  @Override
  public void refresh(boolean preserveActiveRow) {
    refresh(preserveActiveRow, null);
  }

  @Override
  public void tryFilter(final Filter newFilter, final Consumer<Boolean> callback,
      final boolean notify) {

    if (newFilter == null) {
      if (isComplete()) {
        acceptFilter(newFilter);
      } else {
        setUserFilter(null);
        refresh(true);
      }

      if (callback != null) {
        callback.accept(true);
      }

    } else if (isComplete()) {
      tryNotEmptyFilter(newFilter, callback, notify);

    } else {
      refresh(true, () -> tryNotEmptyFilter(newFilter, callback, notify));
    }
  }

  @Override
  protected void onRequest(boolean preserveActiveRow) {
    updateDisplay(preserveActiveRow);
  }

  protected void updateDisplay(boolean preserveActiveRow) {
    int start = getPageStart();
    int length = getPageSize();
    int rowCount = getRowCount();

    List<BeeRow> rowValues;
    if (rowCount <= 0) {
      rowValues = new ArrayList<>();
    } else if (length <= 0 || length >= rowCount) {
      rowValues = getRowList();
    } else {
      rowValues = getRowList().subList(start, Math.min(start + length, rowCount));
    }

    if (preserveActiveRow) {
      getDisplay().preserveActiveRow(rowValues);
    }
    getDisplay().setRowData(rowValues, true);
  }

  private void acceptFilter(Filter newFilter) {
    applyFilter(newFilter);
    setUserFilter(newFilter);

    getDisplay().setRowCount(getRowCount(), true);
    updateDisplay(true);
  }

  private void applyFilter(Filter newFilter) {
    filteredRowIds.clear();
    viewRows.clear();

    if (newFilter != null) {
      List<BeeColumn> columns = getTable().getColumns();
      for (BeeRow row : getTable().getRows()) {
        if (newFilter.isMatch(columns, row)) {
          filteredRowIds.add(row.getId());
          viewRows.add(row);
        }
      }
    }
  }

  private void deleteRow(long rowId) {
    getTable().removeRowById(rowId);

    if (filteredRowIds.contains(rowId)) {
      filteredRowIds.remove(rowId);

      int index = BeeConst.UNDEF;
      for (int i = 0; i < viewRows.size(); i++) {
        if (viewRows.get(i).getId() == rowId) {
          index = i;
          break;
        }
      }

      if (index >= 0) {
        viewRows.remove(index);
      }
    }
  }

  private int getRowCount() {
    if (getUserFilter() != null && isComplete()) {
      return viewRows.size();
    } else {
      return table.getNumberOfRows();
    }
  }

  private List<BeeRow> getRowList() {
    if (getUserFilter() != null && isComplete()) {
      return viewRows;
    } else {
      return getTable().getRows();
    }
  }

  private boolean isComplete() {
    return complete;
  }

  private void refresh(final boolean preserveActiveRow, final ScheduledCommand callback) {
    final int oldPageSize = getPageSize();
    final int oldTableSize = getTable().getNumberOfRows();

    Queries.getRowSet(getViewName(), null, getQueryFilter(null), getOrder(),
        BeeConst.UNDEF, BeeConst.UNDEF, CachingPolicy.NONE, getQueryOptions(),
        (RowSetCallback) rowSet -> {
          Assert.notNull(rowSet);
          setTable(rowSet);
          setComplete(true);

          applyFilter(getUserFilter());

          int newTableSize = rowSet.getNumberOfRows();

          int oldRc = getDisplay().getRowCount();
          int newRc = getRowCount();

          if (newTableSize != oldTableSize && oldPageSize >= oldTableSize) {
            getDisplay().setPageSize(newTableSize, oldRc == newRc);
          }
          getDisplay().setRowCount(newRc, true);

          updateDisplay(preserveActiveRow);

          if (callback != null) {
            callback.execute();
          }
        });
  }

  private void setComplete(boolean complete) {
    this.complete = complete;
  }

  private void setTable(BeeRowSet table) {
    this.table = table;
  }

  private void tryNotEmptyFilter(Filter newFilter, Consumer<Boolean> callback, boolean notify) {
    boolean ok = false;
    List<BeeColumn> columns = getTable().getColumns();
    for (BeeRow row : getTable().getRows()) {
      if (newFilter.isMatch(columns, row)) {
        ok = true;
        break;
      }
    }

    if (ok) {
      acceptFilter(newFilter);
      if (callback != null) {
        callback.accept(true);
      }

    } else {
      rejectFilter(newFilter, notify);
      if (callback != null) {
        callback.accept(false);
      }
    }
  }

  private void updateViewRows() {
    viewRows.clear();
    if (filteredRowIds.isEmpty()) {
      return;
    }

    for (BeeRow row : getTable().getRows()) {
      if (filteredRowIds.contains(row.getId())) {
        viewRows.add(row);
      }
    }
  }
}
