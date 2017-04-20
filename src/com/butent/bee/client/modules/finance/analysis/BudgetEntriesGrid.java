package com.butent.bee.client.modules.finance.analysis;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

public class BudgetEntriesGrid extends AbstractGridInterceptor {

  public BudgetEntriesGrid() {
  }

  @Override
  public GridInterceptor getInstance() {
    return new BudgetEntriesGrid();
  }

  @Override
  public ColumnDescription beforeCreateColumn(GridView gridView,
      ColumnDescription columnDescription) {

    if (BeeUtils.isEmpty(columnDescription.getCaption())) {
      Integer month = getBudgetEntryMonth(columnDescription.getId());

      if (TimeUtils.isMonth(month)) {
        columnDescription.setLabel(Format.properMonthFull(month));
        columnDescription.setCaption(Format.properMonthShort(month));

      } else if (columnDescription.getId().startsWith("Quarter")) {
        Integer quarter = BeeUtils.toIntOrNull(BeeUtils.right(columnDescription.getId(), 1));

        if (TimeUtils.isQuarter(quarter)) {
          columnDescription.setLabel(Format.quarterFull(quarter));
          columnDescription.setCaption(Format.quarterShort(quarter));
        }
      }
    }

    return super.beforeCreateColumn(gridView, columnDescription);
  }

  @Override
  public void beforeRender(GridView gridView, RenderingEvent event) {
    IsRow parentRow = ViewHelper.getFormRow(gridView);

    if (parentRow != null) {
      boolean changed = false;

      Long indicator = Data.getLong(VIEW_BUDGET_HEADERS, parentRow, COL_BUDGET_HEADER_INDICATOR);
      changed |= getGridView().getGrid().setColumnVisible(COL_BUDGET_ENTRY_INDICATOR,
          !DataUtils.isId(indicator));

      Long type = Data.getLong(VIEW_BUDGET_HEADERS, parentRow, COL_BUDGET_HEADER_TYPE);
      changed |= getGridView().getGrid().setColumnVisible(COL_BUDGET_ENTRY_TYPE,
          !DataUtils.isId(type));

      Integer year = Data.getInteger(VIEW_BUDGET_HEADERS, parentRow, COL_BUDGET_HEADER_YEAR);
      changed |= getGridView().getGrid().setColumnVisible(COL_BUDGET_ENTRY_YEAR,
          !TimeUtils.isYear(year));

      for (int dimension = 1; dimension <= Dimensions.getObserved(); dimension++) {
        Boolean visible = Data.getBoolean(VIEW_BUDGET_HEADERS, parentRow,
            colBudgetShowEntryDimension(dimension));

        changed |= getGridView().getGrid().setColumnVisible(
            Dimensions.getRelationColumn(dimension), BeeUtils.isTrue(visible));
      }

      Boolean showEmployee = Data.getBoolean(VIEW_BUDGET_HEADERS, parentRow,
          COL_BUDGET_SHOW_ENTRY_EMPLOYEE);
      changed |= getGridView().getGrid().setColumnVisible(COL_BUDGET_ENTRY_EMPLOYEE,
          BeeUtils.isTrue(showEmployee));

      if (changed) {
        event.setDataChanged();
      }
    }

    super.beforeRender(gridView, event);
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
    if (gridView != null && oldRow != null && newRow != null) {
      if (copy && gridView.getGrid().isColumnVisible(COL_BUDGET_ENTRY_ORDINAL)) {
        int index = gridView.getDataIndex(COL_BUDGET_ENTRY_ORDINAL);
        Integer ordinal = oldRow.getInteger(index);

        if (BeeUtils.isPositive(ordinal)) {
          newRow.setValue(index, ordinal + 10);
        }
      }

      if (!copy) {
        copyRelation(gridView, oldRow, newRow, COL_BUDGET_ENTRY_INDICATOR);
        copyRelation(gridView, oldRow, newRow, COL_BUDGET_ENTRY_TYPE);

        for (int dimension = 1; dimension <= Dimensions.getObserved(); dimension++) {
          copyRelation(gridView, oldRow, newRow, Dimensions.getRelationColumn(dimension));
        }

        copyRelation(gridView, oldRow, newRow, COL_BUDGET_ENTRY_EMPLOYEE);

        copyValue(gridView, oldRow, newRow, COL_BUDGET_ENTRY_TURNOVER_OR_BALANCE);
        copyValue(gridView, oldRow, newRow, COL_BUDGET_ENTRY_YEAR);
      }
    }

    return super.onStartNewRow(gridView, oldRow, newRow, copy);
  }

  private static void copyRelation(GridView gridView, IsRow oldRow, IsRow newRow, String columnId) {
    if (gridView.getGrid().isColumnVisible(columnId)) {
      int index = gridView.getDataIndex(columnId);
      Long value = oldRow.getLong(index);

      if (DataUtils.isId(value)) {
        newRow.setValue(index, value);
        RelationUtils.setRelatedValues(gridView.getDataInfo(), columnId, newRow, oldRow);
      }
    }
  }

  private static void copyValue(GridView gridView, IsRow oldRow, IsRow newRow, String columnId) {
    if (gridView.getGrid().isColumnVisible(columnId)) {
      int index = gridView.getDataIndex(columnId);
      String value = oldRow.getString(index);

      if (!BeeUtils.isEmpty(value)) {
        newRow.setValue(index, value);
      }
    }
  }
}
