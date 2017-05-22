package com.butent.bee.client.modules.finance.analysis;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.finance.FinanceKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasRowValue;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.modules.finance.analysis.AnalysisUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BudgetEntriesGrid extends AbstractGridInterceptor {

  private final class AggregateRenderer extends AbstractCellRenderer implements HasRowValue {

    private final List<String> sources = new ArrayList<>();
    private final List<Integer> indexes = new ArrayList<>();

    private AggregateRenderer(List<? extends IsColumn> dataColumns, int startMonth, int endMonth) {
      super(null);

      for (int i = startMonth; i <= endMonth; i++) {
        String source = colBudgetEntryValue(i);

        sources.add(source);
        indexes.add(DataUtils.getColumnIndex(source, dataColumns));
      }
    }

    @Override
    public boolean dependsOnSource(String source) {
      return sources.contains(source);
    }

    @Override
    public Value getRowValue(IsRow row) {
      return new NumberValue(evaluate(row));
    }

    @Override
    public String render(IsRow row) {
      return BeeUtils.toStringOrNull(evaluate(row));
    }

    private Double evaluate(IsRow row) {
      if (row == null) {
        return null;

      } else {
        double total = BeeConst.DOUBLE_ZERO;
        int count = 0;

        int scale = 0;

        for (int index : indexes) {
          Double value = row.getDouble(index);

          if (BeeUtils.isDouble(value)) {
            total += value;
            count++;

            scale = Math.max(scale, BeeUtils.getDecimals(row.getString(index)));
          }
        }

        if (count > 1 && BeeUtils.nonZero(total)) {
          Pair<Boolean, Integer> pair = getRatioAndScale(row);

          if (pair != null && BeeUtils.isTrue(pair.getA())) {
            total /= count;

            if (AnalysisUtils.isValidScale(pair.getB())) {
              scale = pair.getB();
            } else {
              scale = AnalysisUtils.getRatioScale(total, scale);
            }

            total = BeeUtils.round(total, scale);
          }
        }

        return total;
      }
    }
  }

  private static final List<String> QUARTER_COLUMNS =
      Arrays.asList("Quarter1", "Quarter2", "Quarter3", "Quarter4");

  private static final String TOTAL_COLUMN = "Total";

  private static final String STYLE_REPEAT = FinanceKeeper.STYLE_PREFIX + "budget-repeat-right";

  private final int headerIndicatorIndex;
  private final int headerRatioIndex;
  private final int headerScaleIndex;

  private final int entryIndicatorIndex;
  private final int entryRatioIndex;
  private final int entryScaleIndex;

  public BudgetEntriesGrid() {
    this.headerIndicatorIndex = Data.getColumnIndex(VIEW_BUDGET_HEADERS,
        COL_BUDGET_HEADER_INDICATOR);
    this.headerRatioIndex = Data.getColumnIndex(VIEW_BUDGET_HEADERS, COL_FIN_INDICATOR_RATIO);
    this.headerScaleIndex = Data.getColumnIndex(VIEW_BUDGET_HEADERS, COL_FIN_INDICATOR_SCALE);

    this.entryIndicatorIndex = Data.getColumnIndex(VIEW_BUDGET_ENTRIES,
        COL_BUDGET_ENTRY_INDICATOR);
    this.entryRatioIndex = Data.getColumnIndex(VIEW_BUDGET_ENTRIES, COL_FIN_INDICATOR_RATIO);
    this.entryScaleIndex = Data.getColumnIndex(VIEW_BUDGET_ENTRIES, COL_FIN_INDICATOR_SCALE);
  }

  @Override
  public GridInterceptor getInstance() {
    return new BudgetEntriesGrid();
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    if (presenter != null && presenter.getHeader() != null
        && BeeKeeper.getUser().canEditData(VIEW_BUDGET_ENTRIES)) {

      Button repeatCommand = new Button(Localized.dictionary().finBudgetRepeatRight());
      repeatCommand.addStyleName(STYLE_REPEAT);
      repeatCommand.addClickHandler(event -> repeat());

      presenter.getHeader().addCommandItem(repeatCommand);
    }

    super.afterCreatePresenter(presenter);
  }

  @Override
  public ColumnDescription beforeCreateColumn(GridView gridView,
      ColumnDescription columnDescription) {

    if (BeeUtils.isEmpty(columnDescription.getCaption())) {
      Integer month = getBudgetEntryMonth(columnDescription.getId());

      if (TimeUtils.isMonth(month)) {
        columnDescription.setLabel(Format.properMonthFull(month));
        columnDescription.setCaption(Format.properMonthShort(month));

      } else if (QUARTER_COLUMNS.contains(columnDescription.getId())) {
        int quarter = QUARTER_COLUMNS.indexOf(columnDescription.getId()) + 1;

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
  public AbstractCellRenderer getRenderer(String columnName, List<? extends IsColumn> dataColumns,
      ColumnDescription columnDescription, CellSource cellSource) {

    if (QUARTER_COLUMNS.contains(columnName)) {
      int quarter = QUARTER_COLUMNS.indexOf(columnDescription.getId()) + 1;
      return new AggregateRenderer(dataColumns, quarter * 3 - 2, quarter * 3);

    } else if (TOTAL_COLUMN.equals(columnName)) {
      return new AggregateRenderer(dataColumns, 1, 12);

    } else {
      return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
    }
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

  private Pair<Boolean, Integer> getRatioAndScale(IsRow row) {
    if (row == null) {
      return null;

    } else if (DataUtils.isId(row.getLong(entryIndicatorIndex))) {
      return Pair.of(row.isTrue(entryRatioIndex), row.getInteger(entryScaleIndex));

    } else {
      IsRow parentRow = ViewHelper.getParentRow(getGridView().asWidget(), VIEW_BUDGET_HEADERS);

      if (parentRow != null && DataUtils.isId(parentRow.getLong(headerIndicatorIndex))) {
        return Pair.of(parentRow.isTrue(headerRatioIndex), parentRow.getInteger(headerScaleIndex));
      } else {
        return null;
      }
    }
  }

  private void repeat() {
    final IsRow row = getActiveRow();
    String columnId = getGridView().getGrid().getActiveColumnId();

    if (row != null && row.isEditable() && columnId != null) {
      final Integer month = getBudgetEntryMonth(columnId);

      if (month != null && month < 12) {
        final String newValue = row.getString(getDataIndex(columnId));

        final List<BeeColumn> columns = new ArrayList<>();
        final List<String> oldValues = new ArrayList<>();
        final List<String> newValues = new ArrayList<>();

        for (int i = month + 1; i <= 12; i++) {
          int index = getDataIndex(colBudgetEntryValue(i));
          String oldValue = row.getString(index);

          if (!Objects.equals(oldValue, newValue)) {
            columns.add(getDataColumns().get(index));

            oldValues.add(oldValue);
            newValues.add(newValue);
          }
        }

        if (!columns.isEmpty()) {
          List<String> messages = new ArrayList<>();

          if (month < 11) {
            messages.add(TimeUtils.renderPeriod(Format.renderMonthFullStandalone(month + 1),
                Format.renderMonthFullStandalone(12)));
          } else {
            messages.add(Format.renderMonthFullStandalone(month + 1));
          }

          if (!BeeUtils.isEmpty(newValue)) {
            messages.add(newValue);
          }

          Global.confirm(Localized.dictionary().finBudgetRepeatRight(), Icon.QUESTION, messages,
              () -> Queries.update(getViewName(), row.getId(), row.getVersion(),
                  columns, oldValues, newValues, null, RowCallback.refreshRow(getViewName())));
        }
      }
    }
  }
}
