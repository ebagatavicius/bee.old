package com.butent.bee.client.modules.finance.analysis;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.StringList;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AnalysisHeadersGrid extends AbstractGridInterceptor {

  public AnalysisHeadersGrid() {
  }

  @Override
  public GridInterceptor getInstance() {
    return new AnalysisHeadersGrid();
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    if (copy && presenter != null && presenter.getActiveRow() != null) {
      final IsRow oldRow = presenter.getActiveRow();

      String oldName = getStringValue(COL_ANALYSIS_NAME);
      int maxLength = Data.getColumnPrecision(getViewName(), COL_ANALYSIS_NAME);

      Global.inputString(Localized.dictionary().finAnalysisCopy(oldName),
          Localized.dictionary().finAnalysisName(), new StringCallback() {
            @Override
            public void onSuccess(String value) {
              if (!BeeUtils.isEmpty(value)) {
                final String newName = BeeUtils.trim(value);

                Queries.getRowCount(getViewName(), Filter.equals(COL_ANALYSIS_NAME, newName),
                    new Queries.IntCallback() {
                      @Override
                      public void onSuccess(Integer result) {
                        if (BeeUtils.isPositive(result)) {
                          getGridView().notifyWarning(Localized.dictionary().valueExists(newName));
                        } else {
                          copyAnalysisForm(oldRow, newName);
                        }
                      }
                    });
              }
            }
          },
          null, null, maxLength, null, 50, CssUnit.EM);

      return false;

    } else {
      return super.beforeAddRow(presenter, copy);
    }
  }

  private void copyAnalysisForm(final IsRow oldRow, String name) {
    IsRow headerRow = DataUtils.cloneRow(oldRow);
    Data.setValue(getViewName(), headerRow, COL_ANALYSIS_NAME, name);

    int ordinalIndex = getDataIndex(COL_ANALYSIS_HEADER_ORDINAL);
    Integer ordinal = oldRow.getInteger(ordinalIndex);
    if (BeeUtils.isPositive(ordinal)) {
      headerRow.setValue(ordinalIndex, ordinal + 1);
    }

    BeeRowSet rowSet = DataUtils.createRowSetForInsert(getViewName(),
        getGridView().getDataColumns(), headerRow);

    Queries.insertRow(rowSet, new RowCallback() {
      @Override
      public void onSuccess(BeeRow newRow) {
        if (DataUtils.hasId(newRow)) {
          copyChildren(oldRow.getId(), newRow.getId(), ok -> {
            getGridView().getGrid().insertRow(newRow, true);
            RowInsertEvent.fire(BeeKeeper.getBus(), getViewName(), newRow, getGridView().getId());
          });
        }
      }
    });
  }

  private static void copyChildren(final long oldHeaderId, final long newHeaderId,
      final Consumer<Boolean> callback) {

    List<String> viewNames = StringList.of(VIEW_ANALYSIS_COLUMNS, VIEW_ANALYSIS_ROWS);

    Map<String, Filter> filters = new HashMap<>();
    filters.put(VIEW_ANALYSIS_COLUMNS, Filter.equals(COL_ANALYSIS_HEADER, oldHeaderId));
    filters.put(VIEW_ANALYSIS_ROWS, Filter.equals(COL_ANALYSIS_HEADER, oldHeaderId));

    Queries.getData(viewNames, filters, new Queries.DataCallback() {
      @Override
      public void onFailure(String... reason) {
        super.onFailure(reason);
        callback.accept(false);
      }

      @Override
      public void onSuccess(Collection<BeeRowSet> result) {
        BeeRowSet analysisColumns = null;
        BeeRowSet analysisRows = null;

        int count = 0;

        if (!BeeUtils.isEmpty(result)) {
          for (BeeRowSet rowSet : result) {
            if (!DataUtils.isEmpty(rowSet)) {
              switch (rowSet.getViewName()) {
                case VIEW_ANALYSIS_COLUMNS:
                  analysisColumns = rowSet;
                  count++;
                  break;

                case VIEW_ANALYSIS_ROWS:
                  analysisRows = rowSet;
                  count++;
                  break;
              }
            }
          }
        }

        Map<Long, Long> analysisColumnIds = new HashMap<>();
        Map<Long, Long> analysisRowIds = new HashMap<>();

        if (count > 0) {
          Latch latch = new Latch(count);

          Holder<Boolean> columnsOk = Holder.of(analysisColumns == null);
          Holder<Boolean> rowsOk = Holder.of(analysisRows == null);

          if (analysisColumns != null) {
            int index = analysisColumns.getColumnIndex(COL_ANALYSIS_HEADER);
            analysisColumns.forEach(row -> row.setValue(index, newHeaderId));

            insertRows(analysisColumns, ids -> {
              if (!BeeUtils.isEmpty(ids)) {
                analysisColumnIds.putAll(ids);
                columnsOk.set(true);
              }

              latch.decrement();
              if (latch.isOpen()) {
                if (columnsOk.get() && rowsOk.get()) {
                  copyFilters(oldHeaderId, newHeaderId, analysisColumnIds, analysisRowIds,
                      callback);
                } else {
                  callback.accept(false);
                }
              }
            });
          }

          if (analysisRows != null) {
            int index = analysisRows.getColumnIndex(COL_ANALYSIS_HEADER);
            analysisRows.forEach(row -> row.setValue(index, newHeaderId));

            insertRows(analysisRows, ids -> {
              if (!BeeUtils.isEmpty(ids)) {
                analysisRowIds.putAll(ids);
                rowsOk.set(true);
              }

              latch.decrement();
              if (latch.isOpen()) {
                if (columnsOk.get() && rowsOk.get()) {
                  copyFilters(oldHeaderId, newHeaderId, analysisColumnIds, analysisRowIds,
                      callback);
                } else {
                  callback.accept(false);
                }
              }
            });
          }

        } else {
          copyFilters(oldHeaderId, newHeaderId, analysisColumnIds, analysisRowIds, callback);
        }
      }
    });
  }

  private static void copyFilters(final long oldHeaderId, final long newHeaderId,
      final Map<Long, Long> analysisColumnIds, final Map<Long, Long> analysisRowIds,
      final Consumer<Boolean> callback) {

    CompoundFilter filter = Filter.or();
    filter.add(Filter.equals(COL_ANALYSIS_HEADER, oldHeaderId));

    if (!BeeUtils.isEmpty(analysisColumnIds)) {
      filter.add(Filter.any(COL_ANALYSIS_COLUMN, analysisColumnIds.keySet()));
    }
    if (!BeeUtils.isEmpty(analysisRowIds)) {
      filter.add(Filter.any(COL_ANALYSIS_ROW, analysisRowIds.keySet()));
    }

    Queries.getRowSet(VIEW_ANALYSIS_FILTERS, null, filter, new Queries.RowSetCallback() {
      @Override
      public void onFailure(String... reason) {
        super.onFailure(reason);
        callback.accept(false);
      }

      @Override
      public void onSuccess(BeeRowSet oldFilters) {
        if (DataUtils.isEmpty(oldFilters)) {
          callback.accept(true);

        } else {
          int headerIndex = oldFilters.getColumnIndex(COL_ANALYSIS_HEADER);
          int columnIndex = oldFilters.getColumnIndex(COL_ANALYSIS_COLUMN);
          int rowIndex = oldFilters.getColumnIndex(COL_ANALYSIS_ROW);

          for (BeeRow row : oldFilters) {
            if (row.isEqual(headerIndex, oldHeaderId)) {
              row.setValue(headerIndex, newHeaderId);
            }

            Long oldColumnId = row.getLong(columnIndex);
            if (DataUtils.isId(oldColumnId)) {
              row.setValue(columnIndex, analysisColumnIds.get(oldColumnId));
            }

            Long oldRowId = row.getLong(rowIndex);
            if (DataUtils.isId(oldRowId)) {
              row.setValue(rowIndex, analysisRowIds.get(oldRowId));
            }
          }

          insertRows(oldFilters, ids -> callback.accept(!BeeUtils.isEmpty(ids)));
        }
      }
    });
  }

  private static void insertRows(final BeeRowSet input, final Consumer<Map<Long, Long>> callback) {
    Queries.insertRows(DataUtils.createRowSetForInsert(input),
        new RpcCallback<RowInfoList>() {
          @Override
          public void onFailure(String... reason) {
            super.onFailure(reason);
            callback.accept(new HashMap<>());
          }

          @Override
          public void onSuccess(RowInfoList result) {
            Map<Long, Long> ids = new HashMap<>();

            if (result != null && result.size() == input.getNumberOfRows()) {
              for (int i = 0; i < result.size(); i++) {
                ids.put(input.getRow(i).getId(), result.get(i).getId());
              }
            }

            callback.accept(ids);
          }
        });
  }
}
