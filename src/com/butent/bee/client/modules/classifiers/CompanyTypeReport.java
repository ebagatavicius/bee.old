package com.butent.bee.client.modules.classifiers;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Collator;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.ReportInterceptor;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CompanyTypeReport extends ReportInterceptor {

  private static final class Column implements Comparable<Column> {

    private final Long typeId;
    private final String typeName;

    private final boolean total;

    private Column(Long typeId, String typeName, boolean total) {
      this.typeId = typeId;
      this.typeName = typeName;
      this.total = total;
    }

    @Override
    public int compareTo(Column o) {
      if (this.equals(o)) {
        return BeeConst.COMPARE_EQUAL;

      } else if (total != o.total) {
        return total ? BeeConst.COMPARE_MORE : BeeConst.COMPARE_LESS;

      } else if (typeId == null) {
        return BeeConst.COMPARE_MORE;

      } else {
        return Collator.DEFAULT.compare(typeName, o.typeName);
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;

      } else if (obj instanceof Column) {
        Column c = (Column) obj;
        return Objects.equals(typeId, c.typeId) && total == c.total;

      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return (typeId == null ? 0 : typeId.hashCode()) + Boolean.valueOf(total).hashCode();
    }

    private String getLabel() {
      if (total) {
        return BeeConst.STRING_NUMBER_SIGN;
      } else if (typeId == null) {
        return BeeConst.STRING_MINUS;
      } else {
        return typeName;
      }
    }
  }

  private static final String NAME_START_DATE = "StartDate";
  private static final String NAME_END_DATE = "EndDate";

  private static final String NAME_TYPES = "Types";

  private static final String STYLE_PREFIX = StyleUtils.CLASS_NAME_PREFIX + "co-ctr-";

  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_HEADER = STYLE_PREFIX + "header";

  private static final String STYLE_YEAR = STYLE_PREFIX + "year";
  private static final String STYLE_MONTH = STYLE_PREFIX + "month";

  private static final String STYLE_VALUE = STYLE_PREFIX + "value";
  private static final String STYLE_EMPTY = STYLE_PREFIX + "empty";

  private static final String STYLE_ROW_TOTAL = STYLE_PREFIX + "row-total";
  private static final String STYLE_COL_TOTAL = STYLE_PREFIX + "col-total";
  private static final String STYLE_TOTAL = STYLE_PREFIX + "total";

  private static final String STYLE_DETAILS = STYLE_PREFIX + "details";
  private static final String STYLE_SUMMARY = STYLE_PREFIX + "summary";

  private static final int YEAR_COL = 0;
  private static final int MONTH_COL = 1;
  private static final int VALUE_START_COL = 2;

  private static final String DATA_KEY_YEAR = "year";
  private static final String DATA_KEY_MONTH = "month";
  
  private static void showDetails(YearMonth ym, Column column, DateTime start, DateTime end,
      String types, String typesLabel, boolean modal) {
    
    List<String> labels = Lists.newArrayList(Localized.getConstants().clients());
    List<String> filterArgs = Lists.newArrayList();
    
    DateTime lower = (ym == null) ? start : BeeUtils.max(ym.getDate().getDateTime(), start);
    DateTime upper = (ym == null) ? end : BeeUtils.min(ym.nextMonth().getDate().getDateTime(), end);
    
    if (lower != null || upper != null) {
      labels.add(TimeUtils.renderPeriod(lower, upper));
    }
    
    filterArgs.add((lower == null) ? null : lower.serialize());
    filterArgs.add((upper == null) ? null : upper.serialize());
    
    if (column.total) {
      if (!BeeUtils.isEmpty(typesLabel)) {
        labels.add(typesLabel);
      }
      filterArgs.add(types);

    } else {
      labels.add(column.getLabel());
      filterArgs.add((column.typeId == null) ? BeeConst.STRING_ZERO : column.typeId.toString());
    }
    
    drillDown(GRID_COMPANIES, BeeUtils.join(BeeConst.STRING_SPACE, labels),
        Filter.custom(FILTER_COMPANY_CREATION_AND_TYPE, filterArgs), modal);
  }

  private static Table<YearMonth, Column, Integer> transformData(SimpleRowSet data) {
    Table<YearMonth, Column, Integer> table = HashBasedTable.create();

    int valueIndex = data.getNumberOfColumns() - 1;

    for (SimpleRow row : data) {
      YearMonth ym = new YearMonth(row.getInt(BeeConst.YEAR), row.getInt(BeeConst.MONTH));

      String type = row.getValue(COL_RELATION_TYPE);
      Long id;
      boolean total;

      if (BeeUtils.isDigit(type)) {
        id = BeeUtils.toLong(type);
        total = false;
      } else if (BeeUtils.isEmpty(type)) {
        id = null;
        total = false;
      } else {
        id = null;
        total = true;
      }

      Column column = new Column(id, row.getValue(COL_RELATION_TYPE_NAME), total);

      Integer value = row.getInt(valueIndex);
      if (BeeUtils.isPositive(value)) {
        table.put(ym, column, value);
      }
    }

    return table;
  }

  CompanyTypeReport() {
  }

  @Override
  public FormInterceptor getInstance() {
    return new CompanyTypeReport();
  }

  @Override
  public void onLoad(FormView form) {
    Long user = BeeKeeper.getUser().getUserId();
    if (!DataUtils.isId(user)) {
      return;
    }

    Widget widget = form.getWidgetByName(NAME_START_DATE);
    DateTime dateTime = BeeKeeper.getStorage().getDateTime(storageKey(NAME_START_DATE, user));
    if (widget instanceof InputDateTime && dateTime != null) {
      ((InputDateTime) widget).setDateTime(dateTime);
    }

    widget = form.getWidgetByName(NAME_END_DATE);
    dateTime = BeeKeeper.getStorage().getDateTime(storageKey(NAME_END_DATE, user));
    if (widget instanceof InputDateTime && dateTime != null) {
      ((InputDateTime) widget).setDateTime(dateTime);
    }

    widget = form.getWidgetByName(NAME_TYPES);
    String idList = BeeKeeper.getStorage().get(storageKey(NAME_TYPES, user));
    if (widget instanceof MultiSelector && !BeeUtils.isEmpty(idList)) {
      ((MultiSelector) widget).render(idList);
    }
  }

  @Override
  public void onUnload(FormView form) {
    Long user = BeeKeeper.getUser().getUserId();
    if (!DataUtils.isId(user)) {
      return;
    }

    BeeKeeper.getStorage().set(storageKey(NAME_START_DATE, user), getDateTime(NAME_START_DATE));
    BeeKeeper.getStorage().set(storageKey(NAME_END_DATE, user), getDateTime(NAME_END_DATE));

    BeeKeeper.getStorage().set(storageKey(NAME_TYPES, user), getEditorValue(NAME_TYPES));
  }

  @Override
  protected void clearFilter() {
    clearEditor(NAME_START_DATE);
    clearEditor(NAME_END_DATE);

    clearEditor(NAME_TYPES);
  }

  @Override
  protected void doReport() {
    final DateTime start = getDateTime(NAME_START_DATE);
    final DateTime end = getDateTime(NAME_END_DATE);

    if (!checkRange(start, end)) {
      return;
    }

    ParameterList params = ClassifierKeeper.createArgs(SVC_GET_COMPANY_TYPE_REPORT);

    if (start != null) {
      params.addDataItem(Service.VAR_FROM, start.getTime());
    }
    if (end != null) {
      params.addDataItem(Service.VAR_TO, end.getTime());
    }

    final String types = getEditorValue(NAME_TYPES);
    final String typesLabel;

    if (BeeUtils.isEmpty(types)) {
      typesLabel = null;
    } else {
      params.addDataItem(COL_RELATION_TYPE, types);
      typesLabel = getFilterLabel(NAME_TYPES);
    }
    
    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasMessages()) {
          response.notify(getFormView());
        }

        if (response.hasResponse(SimpleRowSet.class)) {
          SimpleRowSet data = SimpleRowSet.restore(response.getResponseAsString());
          renderData(transformData(data), start, end, types, typesLabel);
        } else {
          getFormView().notifyWarning(Localized.getConstants().nothingFound());
        }
      }
    });
  }

  @Override
  protected String getStorageKeyPrefix() {
    return "CompanyTypeReport_";
  }
  
  private void renderData(Table<YearMonth, Column, Integer> data, 
      final DateTime start, final DateTime end, final String types, final String typesLabel) {

    HasIndexedWidgets container = getDataContainer();
    if (container == null) {
      return;
    }

    if (!container.isEmpty()) {
      container.clear();
    }

    List<YearMonth> yms = Lists.newArrayList(data.rowKeySet());
    if (yms.size() > 1) {
      Collections.sort(yms);
    }

    final List<Column> columns = Lists.newArrayList(data.columnKeySet());
    if (columns.size() > 1) {
      Collections.sort(columns);
    }

    int[] colTotals = new int[columns.size()];

    final HtmlTable table = new HtmlTable(STYLE_TABLE);

    int row = 0;

    table.setText(row, YEAR_COL, Localized.getConstants().year(), STYLE_HEADER);
    table.setText(row, MONTH_COL, Localized.getConstants().month(), STYLE_HEADER);

    for (int j = 0; j < columns.size(); j++) {
      Column column = columns.get(j);
      int col = VALUE_START_COL + j;

      table.setText(row, col, column.getLabel(), STYLE_HEADER);
      if (column.total) {
        table.getCellFormatter().addStyleName(row, col, STYLE_ROW_TOTAL);
      }
    }

    row++;

    for (YearMonth ym : yms) {
      table.setValue(row, YEAR_COL, ym.getYear(), STYLE_YEAR);
      table.setText(row, MONTH_COL, Format.renderMonthFullStandalone(ym.getMonth()), STYLE_MONTH);

      for (int j = 0; j < columns.size(); j++) {
        Column column = columns.get(j);
        int col = VALUE_START_COL + j;

        if (data.contains(ym, column)) {
          Integer value = data.get(ym, column);

          table.setText(row, col, renderQuantity(value), STYLE_VALUE);
          if (column.total) {
            table.getCellFormatter().addStyleName(row, col, STYLE_ROW_TOTAL);
          }
          
          DomUtils.setDataColumn(table.getCellFormatter().getElement(row, col), j);

          colTotals[j] += value;

        } else {
          table.setText(row, col, BeeConst.STRING_EMPTY, STYLE_EMPTY);
        }
      }

      table.getRowFormatter().addStyleName(row, STYLE_DETAILS);
      
      DomUtils.setDataProperty(table.getRow(row), DATA_KEY_YEAR, ym.getYear());
      DomUtils.setDataProperty(table.getRow(row), DATA_KEY_MONTH, ym.getMonth());
      
      row++;
    }

    if (yms.size() > 1) {
      for (int j = 0; j < columns.size(); j++) {
        Column column = columns.get(j);
        int col = VALUE_START_COL + j;

        table.setText(row, col, renderQuantity(colTotals[j]),
            column.total ? STYLE_TOTAL : STYLE_COL_TOTAL);

        if (!column.total) {
          DomUtils.setDataColumn(table.getCellFormatter().getElement(row, col), j);
        }
      }

      table.getRowFormatter().addStyleName(row, STYLE_SUMMARY);
    }

    table.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        TableCellElement cell =
            DomUtils.getParentCell(EventUtils.getEventTargetElement(event), true);

        if (cell != null && !BeeUtils.isEmpty(cell.getInnerText())
            && (cell.hasClassName(STYLE_VALUE) || cell.hasClassName(STYLE_COL_TOTAL))) {

          int index = DomUtils.getDataColumnInt(cell);
          YearMonth ym = null;

          if (cell.hasClassName(STYLE_VALUE)) {
            TableRowElement rowElement = DomUtils.getParentRow(cell, false);

            Integer year = DomUtils.getDataPropertyInt(rowElement, DATA_KEY_YEAR);
            Integer month = DomUtils.getDataPropertyInt(rowElement, DATA_KEY_MONTH);

            if (BeeUtils.isPositive(year) && BeeUtils.isPositive(month)) {
              ym = new YearMonth(year, month);
            }
          }
          
          if (BeeUtils.isIndex(columns, index)) {
            boolean modal = drillModal(event.getNativeEvent());
            showDetails(ym, columns.get(index), start, end, types, typesLabel, modal);
          }
        }
      }
    });

    container.add(table);
  }
}
