package com.butent.bee.client.modules.transport;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.output.Exporter;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.ReportInterceptor;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.css.values.BorderStyle;
import com.butent.bee.shared.css.values.FontStyle;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XRow;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.transport.TransportConstants.OrderStatus;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AssessmentTurnoverReport extends ReportInterceptor {

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {
    if (BeeUtils.same(name, NAME_MANAGERS) && widget instanceof MultiSelector) {
      ((MultiSelector) widget).setAdditionalFilter(Filter.equals(COL_COMPANY,
          BeeKeeper.getUser().getCompany()));
    }
    super.afterCreateWidget(name, widget, callback);
  }

  private static final class RowValue {
    private int quantity;
    private double income1;
    private double expense1;

    private int secondary;
    private double income2;
    private double expense2;

    private RowValue(SimpleRow row) {
      this.quantity = BeeUtils.unbox(row.getInt(AR_RECEIVED));
      this.income1 = BeeUtils.unbox(row.getDouble(AR_INCOME));
      this.expense1 = BeeUtils.unbox(row.getDouble(AR_EXPENSE));

      this.secondary = BeeUtils.unbox(row.getInt(AR_SECONDARY));
      this.income2 = BeeUtils.unbox(row.getDouble(AR_SECONDARY_INCOME));
      this.expense2 = BeeUtils.unbox(row.getDouble(AR_SECONDARY_EXPENSE));
    }

    private double getMargin1() {
      return margin(getProfit1(), income1);
    }

    private double getMargin2() {
      return margin(getProfit2(), income2);
    }

    private double getProfit1() {
      return profit(income1, expense1);
    }

    private double getProfit2() {
      return profit(income2, expense2);
    }

    private void increment(RowValue other) {
      this.quantity += other.quantity;
      this.income1 += other.income1;
      this.expense1 += other.expense1;

      this.secondary += other.secondary;
      this.income2 += other.income2;
      this.expense2 += other.expense2;
    }
  }

  private static BeeLogger logger = LogUtils.getLogger(AssessmentTurnoverReport.class);

  private static final String NAME_START_DATE = "StartDate";
  private static final String NAME_END_DATE = "EndDate";

  private static final String NAME_CURRENCY = "Currency";

  private static final String NAME_DEPARTMENTS = "Departments";
  private static final String NAME_MANAGERS = "Managers";
  private static final String NAME_CUSTOMERS = "Customers";

  private static final List<String> NAME_GROUP_BY =
      Lists.newArrayList("Group0", "Group1", "Group2", "Group3");

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "tr-atr-";

  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_HEADER = STYLE_PREFIX + "header";
  private static final String STYLE_HEADER_1 = STYLE_HEADER + "-1";
  private static final String STYLE_HEADER_2 = STYLE_HEADER + "-2";

  private static final String STYLE_YEAR = STYLE_PREFIX + "year";
  private static final String STYLE_MONTH = STYLE_PREFIX + "month";
  private static final String STYLE_DEPARTMENT = STYLE_PREFIX + "department";
  private static final String STYLE_MANAGER = STYLE_PREFIX + "manager";
  private static final String STYLE_CUSTOMER = STYLE_PREFIX + "customer";

  private static final String STYLE_QUANTITY = STYLE_PREFIX + "quantity";
  private static final String STYLE_INCOME = STYLE_PREFIX + "income";
  private static final String STYLE_EXPENSE = STYLE_PREFIX + "expense";
  private static final String STYLE_PROFIT = STYLE_PREFIX + "profit";
  private static final String STYLE_MARGIN = STYLE_PREFIX + "margin";

  private static final String STYLE_AMOUNT = STYLE_PREFIX + "amount";
  private static final String STYLE_GROWTH = STYLE_PREFIX + "growth";
  private static final String STYLE_PERCENT = STYLE_PREFIX + "percent";
  private static final String STYLE_VALUE = STYLE_PREFIX + "value";

  private static final String STYLE_MAIN = STYLE_PREFIX + "main";
  private static final String STYLE_SECONDARY = STYLE_PREFIX + "secondary";

  private static final String STYLE_DETAILS = STYLE_PREFIX + "details";
  private static final String STYLE_SUMMARY = STYLE_PREFIX + "summary";

  private static final String STYLE_POSITIVE = STYLE_PREFIX + "positive";
  private static final String STYLE_NEGATIVE = STYLE_PREFIX + "negative";
  private static final String STYLE_ZERO = STYLE_PREFIX + "zero";

  private static final String DRILL_DOWN_GRID_NAME = "AssessmentReportDrillDown";

  private static final String COLOR_GROWTH_PLUS = Colors.GREEN;
  private static final String COLOR_MINUS = Colors.RED;

  private static final double SUMMARY_FONT_FACTOR = 1.2;

  private static XCell createCell(int index, Double value, Integer stylePlus, Integer styleMinus) {
    if (BeeUtils.nonZero(value)) {
      return new XCell(index, value, BeeUtils.isPositive(value) ? stylePlus : styleMinus);
    } else {
      return new XCell(index, (Double) null, stylePlus);
    }
  }

  private static Map<Integer, RowValue> getPreviuosValues(SimpleRowSet data, SimpleRowSet prev) {
    Map<Integer, RowValue> result = new HashMap<>();
    if (DataUtils.isEmpty(data)
        || !data.hasColumn(BeeConst.YEAR) || !data.hasColumn(BeeConst.MONTH)) {
      return result;
    }

    boolean prevOk = !DataUtils.isEmpty(prev)
        && Arrays.equals(data.getColumnNames(), prev.getColumnNames());
    if (data.getNumberOfRows() <= 1 && !prevOk) {
      return result;
    }

    boolean hasDepartment = data.hasColumn(COL_DEPARTMENT);
    boolean hasManager = data.hasColumn(COL_COMPANY_PERSON);
    boolean hasCustomer = data.hasColumn(COL_CUSTOMER);

    for (int i = 0; i < data.getNumberOfRows(); i++) {
      Integer year = data.getInt(i, BeeConst.YEAR);
      Integer month = data.getInt(i, BeeConst.MONTH);
      if (!TimeUtils.isYear(year) || !TimeUtils.isMonth(month)) {
        continue;
      }

      YearMonth ym = new YearMonth(year, month).previousMonth();
      year = ym.getYear();
      month = ym.getMonth();

      Long department = hasDepartment ? data.getLong(i, COL_DEPARTMENT) : null;
      Long manager = hasManager ? data.getLong(i, COL_COMPANY_PERSON) : null;
      Long customer = hasCustomer ? data.getLong(i, COL_CUSTOMER) : null;

      if (i > 0) {
        for (int j = 0; j < i; j++) {
          if (matches(data.getRow(j), year, month, department, manager, customer)) {
            result.put(i, new RowValue(data.getRow(j)));
            break;
          }
        }
      }

      if (prevOk && !result.containsKey(i)) {
        for (SimpleRow row : prev) {
          if (matches(row, year, month, department, manager, customer)) {
            result.put(i, new RowValue(row));
            break;
          }
        }
      }
    }

    return result;
  }

  private static double growth(double prev, double value) {
    if (BeeUtils.isPositive(prev)) {
      return (value - prev) * 100d / prev;

    } else if (BeeUtils.isNegative(prev)) {
      return (prev - value) * 100d / prev;

    } else if (BeeUtils.isPositive(value)) {
      return 100d;

    } else if (BeeUtils.isNegative(value)) {
      return -100d;

    } else {
      return BeeConst.DOUBLE_ZERO;
    }
  }

  private static boolean hasSecondaryGrowth(RowValue prev, int secondary) {
    return prev != null && prev.secondary > 0 && secondary > 0;
  }

  private static double margin(Double profit, Double income) {
    if (BeeUtils.isDouble(profit) && BeeUtils.isDouble(income) && !BeeUtils.isZero(income)) {
      return profit * 100d / income;
    } else {
      return BeeConst.DOUBLE_ZERO;
    }
  }

  private static boolean matches(SimpleRow row, int year, int month,
      Long department, Long manager, Long customer) {

    return Objects.equals(year, row.getInt(BeeConst.YEAR))
        && Objects.equals(month, row.getInt(BeeConst.MONTH))
        && (department == null || department.equals(row.getLong(COL_DEPARTMENT)))
        && (manager == null || manager.equals(row.getLong(COL_COMPANY_PERSON)))
        && (customer == null || customer.equals(row.getLong(COL_CUSTOMER)));
  }

  private static double profit(Double income, Double expense) {
    return BeeUtils.unbox(income) - BeeUtils.unbox(expense);
  }

  private static String style(Double value) {
    if (BeeUtils.isPositive(value)) {
      return STYLE_POSITIVE;
    } else if (BeeUtils.isNegative(value)) {
      return STYLE_NEGATIVE;
    } else {
      return STYLE_ZERO;
    }
  }

  private static String style(int value) {
    return (value > 0) ? STYLE_POSITIVE : (value < 0) ? STYLE_NEGATIVE : STYLE_ZERO;
  }

  private final XSheet sheet = new XSheet();

  public AssessmentTurnoverReport() {
  }

  @Override
  public FormInterceptor getInstance() {
    return new AssessmentTurnoverReport();
  }

  @Override
  public void onLoad(FormView form) {
    ReportParameters parameters = readParameters();

    if (parameters != null) {
      Widget widget = form.getWidgetByName(NAME_START_DATE);
      DateTime dateTime = parameters.getDateTime(NAME_START_DATE);
      if (widget instanceof InputDateTime && dateTime != null) {
        ((InputDateTime) widget).setDateTime(dateTime);
      }

      widget = form.getWidgetByName(NAME_END_DATE);
      dateTime = parameters.getDateTime(NAME_END_DATE);
      if (widget instanceof InputDateTime && dateTime != null) {
        ((InputDateTime) widget).setDateTime(dateTime);
      }

      widget = form.getWidgetByName(NAME_CURRENCY);
      Long currency = parameters.getLong(NAME_CURRENCY);
      if (widget instanceof UnboundSelector && DataUtils.isId(currency)) {
        ((UnboundSelector) widget).setValue(currency, false);
      }

      List<String> selectorNames = Lists.newArrayList(NAME_DEPARTMENTS, NAME_MANAGERS,
          NAME_CUSTOMERS);

      for (String selectorName : selectorNames) {
        widget = form.getWidgetByName(selectorName);
        String idList = parameters.get(selectorName);
        if (widget instanceof MultiSelector && !BeeUtils.isEmpty(idList)) {
          ((MultiSelector) widget).setIds(idList);
        }
      }

      for (String groupName : NAME_GROUP_BY) {
        widget = form.getWidgetByName(groupName);
        Integer index = parameters.getInteger(groupName);
        if (widget instanceof ListBox && BeeUtils.isPositive(index)) {
          ((ListBox) widget).setSelectedIndex(index);
        }
      }
    }

    super.onLoad(form);
  }

  @Override
  public void onUnload(FormView form) {
    storeDateTimeValues(NAME_START_DATE, NAME_END_DATE);
    storeEditorValues(NAME_CURRENCY, NAME_DEPARTMENTS, NAME_MANAGERS, NAME_CUSTOMERS);

    for (String groupName : NAME_GROUP_BY) {
      Integer index = getSelectedIndex(groupName);
      if (!BeeUtils.isPositive(index)) {
        index = null;
      }

      storeValue(groupName, index);
    }
  }

  @Override
  protected void clearFilter() {
    clearEditor(NAME_START_DATE);
    clearEditor(NAME_END_DATE);

    clearEditor(NAME_DEPARTMENTS);
    clearEditor(NAME_MANAGERS);
    clearEditor(NAME_CUSTOMERS);
  }

  @Override
  protected void doReport() {
    DateTime start = getDateTime(NAME_START_DATE);
    DateTime end = getDateTime(NAME_END_DATE);

    if (!checkRange(start, end)) {
      return;
    }

    List<String> groupBy = getGroupBy();

    final ParameterList prevParams;

    if (groupBy.contains(BeeConst.MONTH) && start != null) {
      DateTime prevStart;
      if (end == null) {
        prevStart = null;
      } else if (!TimeUtils.sameMonth(start, end) && start.getDom() == end.getDom()) {
        prevStart = TimeUtils.goMonth(start, TimeUtils.monthDiff(end, start));
      } else {
        prevStart = new DateTime(start.getTime() - (end.getTime() - start.getTime()));
      }

      prevParams = TransportHandler.createArgs(SVC_GET_ASSESSMENT_TURNOVER_REPORT);
      if (prevStart != null) {
        prevParams.addDataItem(Service.VAR_FROM, prevStart.getTime());
      }
      prevParams.addDataItem(Service.VAR_TO, start.getTime());

    } else {
      prevParams = null;
    }

    ParameterList params = TransportHandler.createArgs(SVC_GET_ASSESSMENT_TURNOVER_REPORT);
    final List<String> headers = Lists.newArrayList(BeeUtils.joinWords(getReportCaption(),
        getSelectorLabel(NAME_CURRENCY)));

    if (start != null) {
      params.addDataItem(Service.VAR_FROM, start.getTime());
    }
    if (end != null) {
      params.addDataItem(Service.VAR_TO, end.getTime());
    }
    if (start != null || end != null) {
      headers.add(Format.renderPeriod(start, end));
    }

    Long currency = BeeUtils.toLongOrNull(getEditorValue(NAME_CURRENCY));
    if (DataUtils.isId(currency)) {
      params.addDataItem(COL_CURRENCY, currency);
      if (prevParams != null) {
        prevParams.addDataItem(COL_CURRENCY, currency);
      }
    }

    String label;

    String departments = getEditorValue(NAME_DEPARTMENTS);
    if (!BeeUtils.isEmpty(departments)) {
      params.addDataItem(AR_DEPARTMENT, departments);
      if (prevParams != null) {
        prevParams.addDataItem(AR_DEPARTMENT, departments);
      }

      if (DataUtils.parseIdSet(departments).size() > 1) {
        label = Localized.dictionary().departments();
      } else {
        label = Localized.dictionary().department();
      }
      headers.add(BeeUtils.joinWords(label, getSelectorLabel(NAME_DEPARTMENTS)));
    }

    String managers = getEditorValue(NAME_MANAGERS);
    if (!BeeUtils.isEmpty(managers)) {
      params.addDataItem(AR_MANAGER, managers);
      if (prevParams != null) {
        prevParams.addDataItem(AR_MANAGER, managers);
      }

      if (DataUtils.parseIdSet(managers).size() > 1) {
        label = Localized.dictionary().managers();
      } else {
        label = Localized.dictionary().manager();
      }
      headers.add(BeeUtils.joinWords(label, getSelectorLabel(NAME_MANAGERS)));
    }

    String customers = getEditorValue(NAME_CUSTOMERS);
    if (!BeeUtils.isEmpty(customers)) {
      params.addDataItem(AR_CUSTOMER, customers);
      if (prevParams != null) {
        prevParams.addDataItem(AR_CUSTOMER, customers);
      }

      if (DataUtils.parseIdSet(customers).size() > 1) {
        label = Localized.dictionary().clients();
      } else {
        label = Localized.dictionary().client();
      }
      headers.add(BeeUtils.joinWords(label, getSelectorLabel(NAME_CUSTOMERS)));
    }

    if (!groupBy.isEmpty()) {
      params.addDataItem(Service.VAR_GROUP_BY, NameUtils.join(groupBy));
      if (prevParams != null) {
        prevParams.addDataItem(Service.VAR_GROUP_BY, NameUtils.join(groupBy));
      }
    }

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasMessages()) {
          response.notify(getFormView());
        }

        if (response.hasResponse(SimpleRowSet.class)) {
          final SimpleRowSet data = SimpleRowSet.restore(response.getResponseAsString());

          if (prevParams == null) {
            renderData(data, getPreviuosValues(data, null), null);

            sheet.addHeaders(headers);
            sheet.autoSizeAll();

          } else {
            BeeKeeper.getRpc().makeRequest(prevParams, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject prevResponse) {
                if (prevResponse.hasMessages()) {
                  prevResponse.notify(getFormView());
                }

                SimpleRowSet prev;
                RowValue totals = null;

                if (prevResponse.hasResponse(SimpleRowSet.class)) {
                  prev = SimpleRowSet.restore(prevResponse.getResponseAsString());

                  for (SimpleRow row : prev) {
                    RowValue rv = new RowValue(row);
                    if (totals == null) {
                      totals = rv;
                    } else {
                      totals.increment(rv);
                    }
                  }

                } else {
                  prev = null;
                }

                renderData(data, getPreviuosValues(data, prev), totals);

                sheet.addHeaders(headers);
                sheet.autoSizeAll();
              }
            });
          }

        } else {
          getFormView().notifyWarning(Localized.dictionary().nothingFound());
        }
      }
    });
  }

  @Override
  protected void export() {
    if (!sheet.isEmpty()) {
      Exporter.maybeExport(sheet, getReportCaption());
    }
  }

  @Override
  protected String getBookmarkLabel() {
    List<String> labels = Lists.newArrayList(getReportCaption(),
        Format.renderPeriod(getDateTime(NAME_START_DATE), getDateTime(NAME_END_DATE)),
        getSelectorLabel(NAME_CURRENCY),
        getSelectorLabel(NAME_DEPARTMENTS),
        getSelectorLabel(NAME_MANAGERS),
        getSelectorLabel(NAME_CUSTOMERS));

    for (String groupName : NAME_GROUP_BY) {
      if (BeeUtils.isPositive(getSelectedIndex(groupName))) {
        String value = getEditorValue(groupName);
        if (!labels.contains(value)) {
          labels.add(value);
        }
      }
    }

    return BeeUtils.joinWords(labels);
  }

  @Override
  protected Report getReport() {
    return Report.ASSESSMENT_TURNOVER;
  }

  @Override
  protected ReportParameters getReportParameters() {
    ReportParameters parameters = new ReportParameters();

    addDateTimeValues(parameters, NAME_START_DATE, NAME_END_DATE);
    addEditorValues(parameters, NAME_CURRENCY, NAME_DEPARTMENTS, NAME_MANAGERS, NAME_CUSTOMERS);

    for (String groupName : NAME_GROUP_BY) {
      Integer index = getSelectedIndex(groupName);
      if (BeeUtils.isPositive(index)) {
        parameters.add(groupName, index);
      }
    }

    return parameters;
  }

  @Override
  protected boolean validateParameters(ReportParameters parameters) {
    DateTime start = parameters.getDateTime(NAME_START_DATE);
    DateTime end = parameters.getDateTime(NAME_END_DATE);

    return checkRange(start, end);
  }

  private Integer createDetailsStyle(boolean italic, String color, String pattern) {
    return createStyle(false, italic, color, null, pattern, false);
  }

  private Integer createDetailsStyle(String pattern) {
    return createDetailsStyle(null, pattern);
  }

  private Integer createDetailsStyle(String color, String pattern) {
    return createDetailsStyle(false, color, pattern);
  }

  private Integer createStyle(boolean bold, boolean italic, String color,
      Double fontFactor, String pattern, boolean borderTop) {

    XStyle style = XStyle.right();

    if (bold || italic || !BeeUtils.isEmpty(color) || BeeUtils.isPositive(fontFactor)) {
      XFont font = new XFont();

      if (bold) {
        font.setWeight(FontWeight.BOLD);
      }
      if (italic) {
        font.setStyle(FontStyle.ITALIC);
      }

      if (!BeeUtils.isEmpty(color)) {
        font.setColor(color);
      }
      if (BeeUtils.isPositive(fontFactor)) {
        font.setFactor(fontFactor);
      }

      style.setFontRef(sheet.registerFont(font));
    }

    if (!BeeUtils.isEmpty(pattern)) {
      style.setFormat(pattern);
    }
    if (borderTop) {
      style.setBorderTop(BorderStyle.SOLID);
    }

    return sheet.registerStyle(style);
  }

  private Integer createSummaryStyle(boolean italic, String color, String pattern) {
    return createStyle(true, italic, color, SUMMARY_FONT_FACTOR, pattern, true);
  }

  private Integer createSummaryStyle(String pattern) {
    return createSummaryStyle(null, pattern);
  }

  private Integer createSummaryStyle(String color, String pattern) {
    return createSummaryStyle(false, color, pattern);
  }

  private List<String> getGroupBy() {
    List<String> groupBy = new ArrayList<>();

    for (String groupName : NAME_GROUP_BY) {
      Integer index = getSelectedIndex(groupName);

      if (BeeUtils.isPositive(index)) {
        String group;

        switch (index) {
          case 1:
            group = BeeConst.MONTH;
            break;
          case 2:
            group = AR_DEPARTMENT;
            break;
          case 3:
            group = AR_MANAGER;
            break;
          case 4:
            group = AR_CUSTOMER;
            break;
          default:
            group = null;
        }

        if (group != null && !groupBy.contains(group)) {
          groupBy.add(group);
        }
      }
    }

    return groupBy;
  }

  private void renderData(final SimpleRowSet data, Map<Integer, RowValue> prevValues,
      RowValue prevTotal) {

    HasIndexedWidgets container = getDataContainer();
    if (container == null) {
      return;
    }

    sheet.clear();

    if (!container.isEmpty()) {
      container.clear();
    }

    boolean hasGrowth = !BeeUtils.isEmpty(prevValues);

    final HtmlTable table = new HtmlTable(STYLE_TABLE);

    int row = 0;
    int c1 = 0;
    int c2 = 0;
    int col = 0;

    int colYear = BeeConst.UNDEF;
    int colMonth = BeeConst.UNDEF;
    int colDepartment = BeeConst.UNDEF;
    int colManager = BeeConst.UNDEF;
    int colCustomer = BeeConst.UNDEF;

    int colQuantity = BeeConst.UNDEF;
    int colIncome1 = BeeConst.UNDEF;
    int colExpense1 = BeeConst.UNDEF;
    int colProfit1 = BeeConst.UNDEF;
    int colMargin1 = BeeConst.UNDEF;

    int colSecondary = BeeConst.UNDEF;
    int colIncome2 = BeeConst.UNDEF;
    int colExpense2 = BeeConst.UNDEF;
    int colProfit2 = BeeConst.UNDEF;
    int colMargin2 = BeeConst.UNDEF;

    String partStyle;

    XRow xr1 = new XRow(row);
    XRow xr2 = new XRow(row + 1);

    Integer boldRef = sheet.registerFont(XFont.bold());

    XStyle xs = XStyle.center();
    xs.setVerticalAlign(VerticalAlign.MIDDLE);
    xs.setColor(Colors.LIGHTGRAY);
    xs.setFontRef(boldRef);

    int styleRef = sheet.registerStyle(xs);

    XCell xc;
    String text;

    for (int j = 0; j < data.getNumberOfColumns(); j++) {
      String colName = data.getColumnName(j);

      switch (colName) {
        case BeeConst.YEAR:
          colYear = col;

          text = Localized.dictionary().year();
          table.setText(row, c1, text, STYLE_HEADER, STYLE_YEAR);
          table.getCellFormatter().setRowSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setRowSpan(2);
          xr1.add(xc);

          c1++;
          col++;
          break;

        case BeeConst.MONTH:
          colMonth = col;

          text = Localized.dictionary().month();
          table.setText(row, c1, text, STYLE_HEADER, STYLE_MONTH);
          table.getCellFormatter().setRowSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setRowSpan(2);
          xr1.add(xc);

          c1++;
          col++;
          break;

        case COL_DEPARTMENT:
          colDepartment = col;

          text = Localized.dictionary().department();
          table.setText(row, c1, text, STYLE_HEADER, STYLE_DEPARTMENT);
          table.getCellFormatter().setRowSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setRowSpan(2);
          xr1.add(xc);

          c1++;
          col++;
          break;

        case COL_DEPARTMENT_NAME:
          break;

        case COL_COMPANY_PERSON:
          colManager = col;

          text = Localized.dictionary().manager();
          table.setText(row, c1, text, STYLE_HEADER, STYLE_MANAGER);
          table.getCellFormatter().setRowSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setRowSpan(2);
          xr1.add(xc);

          c1++;
          col++;
          break;

        case COL_FIRST_NAME:
        case COL_LAST_NAME:
          break;

        case COL_CUSTOMER:
          colCustomer = col;

          text = Localized.dictionary().customer();
          table.setText(row, c1, text, STYLE_HEADER, STYLE_CUSTOMER);
          table.getCellFormatter().setRowSpan(row, c1, 2);

          xc = new XCell(col, text, styleRef);
          xc.setRowSpan(2);
          xr1.add(xc);

          c1++;
          col++;
          break;

        case ALS_COMPANY_NAME:
          break;

        case AR_RECEIVED:
        case AR_SECONDARY:
          String partLabel;
          if (colName.equals(AR_RECEIVED)) {
            colQuantity = col;
            partLabel = Localized.dictionary().trAssessmentReportAllOrders();
            partStyle = STYLE_MAIN;
          } else {
            colSecondary = col;
            partLabel = Localized.dictionary().trAssessmentReportSecondary();
            partStyle = STYLE_SECONDARY;
          }

          table.setText(row, c1, partLabel, STYLE_HEADER_1, partStyle);
          int span = 5 + (hasGrowth ? 4 : 0);
          table.getCellFormatter().setColSpan(row, c1, span);

          xc = new XCell(col, partLabel, styleRef);
          xc.setColSpan(span);
          xr1.add(xc);

          c1++;

          text = Localized.dictionary().quantity();
          table.setText(row + 1, c2, text, STYLE_HEADER_2, STYLE_QUANTITY, partStyle);
          xr2.add(new XCell(col, text, styleRef));

          c2++;
          col++;

          if (hasGrowth) {
            text = Localized.dictionary().trAssessmentReportGrowth();
            table.setText(row + 1, c2, text, STYLE_HEADER_2, STYLE_GROWTH, partStyle);
            xr2.add(new XCell(col, text, styleRef));

            c2++;
            col++;
          }
          break;

        case AR_INCOME:
        case AR_SECONDARY_INCOME:
          if (colName.equals(AR_INCOME)) {
            colIncome1 = col;
            partStyle = STYLE_MAIN;
          } else {
            colIncome2 = col;
            partStyle = STYLE_SECONDARY;
          }

          text = Localized.dictionary().income();
          table.setText(row + 1, c2, text, STYLE_HEADER_2, STYLE_INCOME, partStyle);
          xr2.add(new XCell(col, text, styleRef));

          c2++;
          col++;

          if (hasGrowth) {
            text = Localized.dictionary().trAssessmentReportGrowth();
            table.setText(row + 1, c2, text, STYLE_HEADER_2, STYLE_GROWTH, partStyle);
            xr2.add(new XCell(col, text, styleRef));

            c2++;
            col++;
          }
          break;

        case AR_EXPENSE:
        case AR_SECONDARY_EXPENSE:
          if (colName.equals(AR_EXPENSE)) {
            colExpense1 = col;
            colProfit1 = col + (hasGrowth ? 2 : 1);
            colMargin1 = col + (hasGrowth ? 4 : 2);
            partStyle = STYLE_MAIN;
          } else {
            colExpense2 = col;
            colProfit2 = col + (hasGrowth ? 2 : 1);
            colMargin2 = col + (hasGrowth ? 4 : 2);
            partStyle = STYLE_SECONDARY;
          }

          text = Localized.dictionary().trExpenses();
          table.setText(row + 1, c2, text, STYLE_HEADER_2, STYLE_EXPENSE, partStyle);
          xr2.add(new XCell(col, text, styleRef));

          c2++;
          col++;

          if (hasGrowth) {
            text = Localized.dictionary().trAssessmentReportGrowth();
            table.setText(row + 1, c2, text, STYLE_HEADER_2, STYLE_GROWTH, partStyle);
            xr2.add(new XCell(col, text, styleRef));

            c2++;
            col++;
          }

          text = Localized.dictionary().profit();
          table.setText(row + 1, c2, text, STYLE_HEADER_2, STYLE_PROFIT, partStyle);
          xr2.add(new XCell(col, text, styleRef));

          c2++;
          col++;

          if (hasGrowth) {
            text = Localized.dictionary().trAssessmentReportGrowth();
            table.setText(row + 1, c2, text, STYLE_HEADER_2, STYLE_GROWTH, partStyle);
            xr2.add(new XCell(col, text, styleRef));

            c2++;
            col++;
          }

          text = Localized.dictionary().marginPercent();
          table.setText(row + 1, c2, text, STYLE_HEADER_2, STYLE_MARGIN, partStyle);
          xr2.add(new XCell(col, text, styleRef));

          c2++;
          col++;
          break;

        default:
          logger.warning("column not recognized", colName);
      }
    }

    sheet.add(xr1);
    sheet.add(xr2);

    int totQuantity = 0;
    double totIncome1 = BeeConst.DOUBLE_ZERO;
    double totExpense1 = BeeConst.DOUBLE_ZERO;

    int totSecondary = 0;
    double totIncome2 = BeeConst.DOUBLE_ZERO;
    double totExpense2 = BeeConst.DOUBLE_ZERO;

    double value;
    Double growth;

    row = 2;
    XRow xr;

    int csQty = createDetailsStyle(QUANTITY_PATTERN);

    int csAmtPlus = createDetailsStyle(AMOUNT_PATTERN);
    int csAmtMinus = createDetailsStyle(COLOR_MINUS, AMOUNT_PATTERN);

    int csGrtPlus = createDetailsStyle(true, COLOR_GROWTH_PLUS, PERCENT_PATTERN);
    int csGrtMinus = createDetailsStyle(true, COLOR_MINUS, PERCENT_PATTERN);

    int csPctPlus = createDetailsStyle(PERCENT_PATTERN);
    int csPctMinus = createDetailsStyle(COLOR_MINUS, PERCENT_PATTERN);

    for (int i = 0; i < data.getNumberOfRows(); i++) {
      xr = new XRow(row);

      RowValue rv = new RowValue(data.getRow(i));
      RowValue pv = hasGrowth ? prevValues.get(i) : null;

      boolean hasSecondaryGrowth = hasSecondaryGrowth(pv, rv.secondary);

      for (int j = 0; j < data.getNumberOfColumns(); j++) {
        String colName = data.getColumnName(j);

        switch (colName) {
          case BeeConst.YEAR:
            text = data.getValue(i, colName);
            table.setText(row, colYear, text, STYLE_YEAR, STYLE_VALUE);
            xr.add(new XCell(colYear, text));
            break;

          case BeeConst.MONTH:
            text = Format.renderMonthFullStandalone(data.getInt(i, colName));
            table.setText(row, colMonth, text, STYLE_MONTH, STYLE_VALUE);
            xr.add(new XCell(colMonth, text));
            break;

          case COL_DEPARTMENT_NAME:
            text = data.getValue(i, colName);
            table.setText(row, colDepartment, text, STYLE_DEPARTMENT, STYLE_VALUE);
            xr.add(new XCell(colDepartment, text));
            break;

          case COL_FIRST_NAME:
            text = BeeUtils.joinWords(data.getValue(i, colName),
                data.getValue(i, COL_LAST_NAME));
            table.setText(row, colManager, text, STYLE_MANAGER, STYLE_VALUE);
            xr.add(new XCell(colManager, text));
            break;

          case ALS_COMPANY_NAME:
            text = data.getValue(i, colName);
            table.setText(row, colCustomer, text, STYLE_CUSTOMER, STYLE_VALUE);
            xr.add(new XCell(colCustomer, text));
            break;

          case AR_RECEIVED:
            table.setText(row, colQuantity, renderQuantity(rv.quantity),
                STYLE_QUANTITY, STYLE_VALUE, STYLE_MAIN, style(rv.quantity));
            if (rv.quantity > 0) {
              xr.add(new XCell(colQuantity, rv.quantity, csQty));
            }

            if (hasGrowth) {
              growth = (pv == null) ? null : growth(pv.quantity, rv.quantity);
              table.setText(row, colQuantity + 1, renderPercent(growth),
                  STYLE_QUANTITY, STYLE_GROWTH, STYLE_MAIN, style(growth));
              if (BeeUtils.nonZero(growth)) {
                xr.add(createCell(colQuantity + 1, growth, csGrtPlus, csGrtMinus));
              }
            }
            break;

          case AR_INCOME:
            table.setText(row, colIncome1, renderAmount(rv.income1),
                STYLE_INCOME, STYLE_AMOUNT, STYLE_MAIN, style(rv.income1));
            if (BeeUtils.nonZero(rv.income1)) {
              xr.add(createCell(colIncome1, rv.income1, csAmtPlus, csAmtMinus));
            }

            if (hasGrowth) {
              growth = (pv == null) ? null : growth(pv.income1, rv.income1);
              table.setText(row, colIncome1 + 1, renderPercent(growth),
                  STYLE_INCOME, STYLE_GROWTH, STYLE_MAIN, style(growth));
              if (BeeUtils.nonZero(growth)) {
                xr.add(createCell(colIncome1 + 1, growth, csGrtPlus, csGrtMinus));
              }
            }
            break;

          case AR_EXPENSE:
            table.setText(row, colExpense1, renderAmount(rv.expense1),
                STYLE_EXPENSE, STYLE_AMOUNT, STYLE_MAIN, style(rv.expense1));
            if (BeeUtils.nonZero(rv.expense1)) {
              xr.add(createCell(colExpense1, rv.expense1, csAmtPlus, csAmtMinus));
            }

            if (hasGrowth) {
              growth = (pv == null) ? null : growth(pv.expense1, rv.expense1);
              table.setText(row, colExpense1 + 1, renderPercent(growth),
                  STYLE_EXPENSE, STYLE_GROWTH, STYLE_MAIN, style(growth));
              if (BeeUtils.nonZero(growth)) {
                xr.add(createCell(colExpense1 + 1, growth, csGrtPlus, csGrtMinus));
              }
            }

            value = rv.getProfit1();
            table.setText(row, colProfit1, renderAmount(value),
                STYLE_PROFIT, STYLE_AMOUNT, STYLE_MAIN, style(value));
            if (BeeUtils.nonZero(value)) {
              xr.add(createCell(colProfit1, value, csAmtPlus, csAmtMinus));
            }

            if (hasGrowth) {
              growth = (pv == null) ? null : growth(pv.getProfit1(), value);
              table.setText(row, colProfit1 + 1, renderPercent(growth),
                  STYLE_PROFIT, STYLE_GROWTH, STYLE_MAIN, style(growth));
              if (BeeUtils.nonZero(growth)) {
                xr.add(createCell(colProfit1 + 1, growth, csGrtPlus, csGrtMinus));
              }
            }

            value = rv.getMargin1();
            table.setText(row, colMargin1, renderPercent(value),
                STYLE_MARGIN, STYLE_PERCENT, STYLE_MAIN, style(value));
            if (BeeUtils.nonZero(value)) {
              xr.add(createCell(colMargin1, value, csPctPlus, csPctMinus));
            }
            break;

          case AR_SECONDARY:
            table.setText(row, colSecondary, renderQuantity(rv.secondary),
                STYLE_QUANTITY, STYLE_VALUE, STYLE_SECONDARY, style(rv.secondary));
            if (rv.secondary > 0) {
              xr.add(new XCell(colSecondary, rv.secondary, csQty));
            }

            if (hasGrowth) {
              growth = hasSecondaryGrowth ? growth(pv.secondary, rv.secondary) : null;
              table.setText(row, colSecondary + 1, renderPercent(growth),
                  STYLE_QUANTITY, STYLE_GROWTH, STYLE_SECONDARY, style(growth));
              if (BeeUtils.nonZero(growth)) {
                xr.add(createCell(colSecondary + 1, growth, csGrtPlus, csGrtMinus));
              }
            }
            break;

          case AR_SECONDARY_INCOME:
            table.setText(row, colIncome2, renderAmount(rv.income2),
                STYLE_INCOME, STYLE_AMOUNT, STYLE_SECONDARY, style(rv.income2));
            if (BeeUtils.nonZero(rv.income2)) {
              xr.add(createCell(colIncome2, rv.income2, csAmtPlus, csAmtMinus));
            }

            if (hasGrowth) {
              growth = hasSecondaryGrowth ? growth(pv.income2, rv.income2) : null;
              table.setText(row, colIncome2 + 1, renderPercent(growth),
                  STYLE_INCOME, STYLE_GROWTH, STYLE_SECONDARY, style(growth));
              if (BeeUtils.nonZero(growth)) {
                xr.add(createCell(colIncome2 + 1, growth, csGrtPlus, csGrtMinus));
              }
            }
            break;

          case AR_SECONDARY_EXPENSE:
            table.setText(row, colExpense2, renderAmount(rv.expense2),
                STYLE_EXPENSE, STYLE_AMOUNT, STYLE_SECONDARY, style(rv.expense2));
            if (BeeUtils.nonZero(rv.expense2)) {
              xr.add(createCell(colExpense2, rv.expense2, csAmtPlus, csAmtMinus));
            }

            if (hasGrowth) {
              growth = hasSecondaryGrowth ? growth(pv.expense2, rv.expense2) : null;
              table.setText(row, colExpense2 + 1, renderPercent(growth),
                  STYLE_EXPENSE, STYLE_GROWTH, STYLE_SECONDARY, style(growth));
              if (BeeUtils.nonZero(growth)) {
                xr.add(createCell(colExpense2 + 1, growth, csGrtPlus, csGrtMinus));
              }
            }

            value = rv.getProfit2();
            table.setText(row, colProfit2, renderAmount(value),
                STYLE_PROFIT, STYLE_AMOUNT, STYLE_SECONDARY, style(value));
            if (BeeUtils.nonZero(value)) {
              xr.add(createCell(colProfit2, value, csAmtPlus, csAmtMinus));
            }

            if (hasGrowth) {
              growth = hasSecondaryGrowth ? growth(pv.getProfit2(), value) : null;
              table.setText(row, colProfit2 + 1, renderPercent(growth),
                  STYLE_PROFIT, STYLE_GROWTH, STYLE_SECONDARY, style(growth));
              if (BeeUtils.nonZero(growth)) {
                xr.add(createCell(colProfit2 + 1, growth, csGrtPlus, csGrtMinus));
              }
            }

            value = rv.getMargin2();
            table.setText(row, colMargin2, renderPercent(value),
                STYLE_MARGIN, STYLE_PERCENT, STYLE_SECONDARY, style(value));
            if (BeeUtils.nonZero(value)) {
              xr.add(createCell(colMargin2, value, csPctPlus, csPctMinus));
            }
            break;
        }
      }

      totQuantity += rv.quantity;
      totIncome1 += rv.income1;
      totExpense1 += rv.expense1;

      totSecondary += rv.secondary;
      totIncome2 += rv.income2;
      totExpense2 += rv.expense2;

      table.getRowFormatter().addStyleName(row, STYLE_DETAILS);
      DomUtils.setDataIndex(table.getRow(row), i);

      sheet.add(xr);
      row++;
    }

    if (data.getNumberOfRows() > 1) {
      xr = new XRow(row);

      int csTotQty = createSummaryStyle(QUANTITY_PATTERN);

      int csTotAmtPlus = createSummaryStyle(AMOUNT_PATTERN);
      int csTotAmtMinus = createSummaryStyle(COLOR_MINUS, AMOUNT_PATTERN);

      int csTotGrtPlus = createSummaryStyle(true, COLOR_GROWTH_PLUS, PERCENT_PATTERN);
      int csTotGrtMinus = createSummaryStyle(true, COLOR_MINUS, PERCENT_PATTERN);

      int csTotPctPlus = createSummaryStyle(PERCENT_PATTERN);
      int csTotPctMinus = createSummaryStyle(COLOR_MINUS, PERCENT_PATTERN);

      table.setText(row, colQuantity, renderQuantity(totQuantity),
          STYLE_QUANTITY, STYLE_VALUE, STYLE_MAIN, style(totQuantity));
      xr.add(new XCell(colQuantity, totQuantity, csTotQty));

      if (hasGrowth) {
        growth = (prevTotal == null) ? null : growth(prevTotal.quantity, totQuantity);
        table.setText(row, colQuantity + 1, renderPercent(growth),
            STYLE_QUANTITY, STYLE_GROWTH, STYLE_MAIN, style(growth));
        xr.add(createCell(colQuantity + 1, growth, csTotGrtPlus, csTotGrtMinus));
      }

      table.setText(row, colIncome1, renderAmount(totIncome1),
          STYLE_INCOME, STYLE_AMOUNT, STYLE_MAIN, style(totIncome1));
      xr.add(createCell(colIncome1, totIncome1, csTotAmtPlus, csTotAmtMinus));

      if (hasGrowth) {
        growth = (prevTotal == null) ? null : growth(prevTotal.income1, totIncome1);
        table.setText(row, colIncome1 + 1, renderPercent(growth),
            STYLE_INCOME, STYLE_GROWTH, STYLE_MAIN, style(growth));
        xr.add(createCell(colIncome1 + 1, growth, csTotGrtPlus, csTotGrtMinus));
      }

      table.setText(row, colExpense1, renderAmount(totExpense1),
          STYLE_EXPENSE, STYLE_AMOUNT, STYLE_MAIN, style(totExpense1));
      xr.add(createCell(colExpense1, totExpense1, csTotAmtPlus, csTotAmtMinus));

      if (hasGrowth) {
        growth = (prevTotal == null) ? null : growth(prevTotal.expense1, totExpense1);
        table.setText(row, colExpense1 + 1, renderPercent(growth),
            STYLE_EXPENSE, STYLE_GROWTH, STYLE_MAIN, style(growth));
        xr.add(createCell(colExpense1 + 1, growth, csTotGrtPlus, csTotGrtMinus));
      }

      double profit = profit(totIncome1, totExpense1);
      double margin = margin(profit, totIncome1);

      table.setText(row, colProfit1, renderAmount(profit),
          STYLE_PROFIT, STYLE_AMOUNT, STYLE_MAIN, style(profit));
      xr.add(createCell(colProfit1, profit, csTotAmtPlus, csTotAmtMinus));

      if (hasGrowth) {
        growth = (prevTotal == null) ? null : growth(prevTotal.getProfit1(), profit);
        table.setText(row, colProfit1 + 1, renderPercent(growth),
            STYLE_PROFIT, STYLE_GROWTH, STYLE_MAIN, style(growth));
        xr.add(createCell(colProfit1 + 1, growth, csTotGrtPlus, csTotGrtMinus));
      }

      table.setText(row, colMargin1, renderPercent(margin),
          STYLE_MARGIN, STYLE_PERCENT, STYLE_MAIN, style(margin));
      xr.add(createCell(colMargin1, margin, csTotPctPlus, csTotPctMinus));

      boolean hasSecondaryGrowth = hasSecondaryGrowth(prevTotal, totSecondary);

      table.setText(row, colSecondary, renderQuantity(totSecondary),
          STYLE_QUANTITY, STYLE_VALUE, STYLE_SECONDARY, style(totSecondary));
      xr.add(new XCell(colSecondary, totSecondary, csTotQty));

      if (hasGrowth) {
        growth = hasSecondaryGrowth ? growth(prevTotal.secondary, totSecondary) : null;
        table.setText(row, colSecondary + 1, renderPercent(growth),
            STYLE_QUANTITY, STYLE_GROWTH, STYLE_SECONDARY, style(growth));
        xr.add(createCell(colSecondary + 1, growth, csTotGrtPlus, csTotGrtMinus));
      }

      table.setText(row, colIncome2, renderAmount(totIncome2),
          STYLE_INCOME, STYLE_AMOUNT, STYLE_SECONDARY, style(totIncome2));
      xr.add(createCell(colIncome2, totIncome2, csTotAmtPlus, csTotAmtMinus));

      if (hasGrowth) {
        growth = hasSecondaryGrowth ? growth(prevTotal.income2, totIncome2) : null;
        table.setText(row, colIncome2 + 1, renderPercent(growth),
            STYLE_INCOME, STYLE_GROWTH, STYLE_SECONDARY, style(growth));
        xr.add(createCell(colIncome2 + 1, growth, csTotGrtPlus, csTotGrtMinus));
      }

      table.setText(row, colExpense2, renderAmount(totExpense2),
          STYLE_EXPENSE, STYLE_AMOUNT, STYLE_SECONDARY, style(totExpense2));
      xr.add(createCell(colExpense2, totExpense2, csTotAmtPlus, csTotAmtMinus));

      if (hasGrowth) {
        growth = hasSecondaryGrowth ? growth(prevTotal.expense2, totExpense2) : null;
        table.setText(row, colExpense2 + 1, renderPercent(growth),
            STYLE_EXPENSE, STYLE_GROWTH, STYLE_SECONDARY, style(growth));
        xr.add(createCell(colExpense2 + 1, growth, csTotGrtPlus, csTotGrtMinus));
      }

      profit = profit(totIncome2, totExpense2);
      margin = margin(profit, totIncome2);

      table.setText(row, colProfit2, renderAmount(profit),
          STYLE_PROFIT, STYLE_AMOUNT, STYLE_SECONDARY, style(profit));
      xr.add(createCell(colProfit2, profit, csTotAmtPlus, csTotAmtMinus));

      if (hasGrowth) {
        growth = hasSecondaryGrowth ? growth(prevTotal.getProfit2(), profit) : null;
        table.setText(row, colProfit2 + 1, renderPercent(growth),
            STYLE_PROFIT, STYLE_GROWTH, STYLE_SECONDARY, style(growth));
        xr.add(createCell(colProfit2 + 1, growth, csTotGrtPlus, csTotGrtMinus));
      }

      table.setText(row, colMargin2, renderPercent(margin),
          STYLE_MARGIN, STYLE_PERCENT, STYLE_SECONDARY, style(margin));
      xr.add(createCell(colMargin2, margin, csTotPctPlus, csTotPctMinus));

      table.getRowFormatter().addStyleName(row, STYLE_SUMMARY);
      sheet.add(xr);
    }

    table.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        TableCellElement cellElement =
            DomUtils.getParentCell(EventUtils.getEventTargetElement(event), true);
        TableRowElement rowElement = DomUtils.getParentRow(cellElement, false);

        if (rowElement != null && rowElement.hasClassName(STYLE_DETAILS)) {
          int dataIndex = DomUtils.getDataIndexInt(rowElement);

          if (!BeeConst.isUndef(dataIndex)) {
            showDetails(data.getRow(dataIndex), cellElement);
          }
        }
      }
    });

    container.add(table);
  }

  private void showDetails(SimpleRow dataRow, TableCellElement cellElement) {
    CompoundFilter filter = Filter.and();

    filter.add(Filter.isEqual(ALS_ORDER_STATUS, new IntegerValue(OrderStatus.COMPLETED.ordinal())));
    filter.add(Filter.in(COL_CARGO, VIEW_CARGO_INCOMES, COL_CARGO,
        Filter.notNull(TradeConstants.COL_SALE)));
    filter.add(Filter.isNot(Filter.in(COL_CARGO, VIEW_CARGO_INCOMES, COL_CARGO,
        Filter.isNull(TradeConstants.COL_SALE))));

    List<String> captions = new ArrayList<>();

    String[] colNames = dataRow.getColumnNames();

    DateTime start = getDateTime(NAME_START_DATE);
    DateTime end = getDateTime(NAME_END_DATE);

    if (ArrayUtils.contains(colNames, BeeConst.YEAR)
        && ArrayUtils.contains(colNames, BeeConst.MONTH)) {

      Integer year = dataRow.getInt(BeeConst.YEAR);
      Integer month = dataRow.getInt(BeeConst.MONTH);

      if (TimeUtils.isYear(year) && TimeUtils.isMonth(month)) {
        if (start == null && end == null) {
          captions.add(BeeUtils.joinWords(year, Format.renderMonthFullStandalone(month)));
        }

        YearMonth ym = new YearMonth(year, month);

        start = BeeUtils.max(start, ym.getDate().getDateTime());
        end = BeeUtils.min(end, TimeUtils.startOfNextMonth(ym).getDateTime());
      }
    }

    if (start != null) {
      filter.add(Filter.isMoreEqual(COL_DATE, new DateTimeValue(start)));
    }
    if (end != null) {
      filter.add(Filter.isLess(COL_DATE, new DateTimeValue(end)));
    }

    if (captions.isEmpty() && (start != null || end != null)) {
      captions.add(Format.renderPeriod(start, end));
    }

    if (ArrayUtils.contains(colNames, COL_COMPANY_PERSON)) {
      Long companyPerson = dataRow.getLong(COL_COMPANY_PERSON);
      if (DataUtils.isId(companyPerson)) {
        filter.add(Filter.equals(COL_COMPANY_PERSON, companyPerson));
        captions.add(BeeUtils.joinWords(dataRow.getValue(COL_FIRST_NAME),
            dataRow.getValue(COL_LAST_NAME)));
      }

    } else {
      String managers = getEditorValue(NAME_MANAGERS);
      if (!BeeUtils.isEmpty(managers)) {
        filter.add(Filter.any(COL_COMPANY_PERSON, DataUtils.parseIdSet(managers)));
        captions.add(getSelectorLabel(NAME_MANAGERS));
      }
    }

    if (ArrayUtils.contains(colNames, COL_DEPARTMENT)) {
      Long department = dataRow.getLong(COL_DEPARTMENT);
      if (DataUtils.isId(department)) {
        filter.add(Filter.equals(COL_DEPARTMENT, department));
        captions.add(dataRow.getValue(COL_DEPARTMENT_NAME));
      }

    } else {
      String departments = getEditorValue(NAME_DEPARTMENTS);
      if (!BeeUtils.isEmpty(departments)) {
        filter.add(Filter.any(COL_DEPARTMENT, DataUtils.parseIdSet(departments)));
        captions.add(getSelectorLabel(NAME_DEPARTMENTS));
      }
    }

    if (ArrayUtils.contains(colNames, COL_CUSTOMER)) {
      Long customer = dataRow.getLong(COL_CUSTOMER);
      if (DataUtils.isId(customer)) {
        filter.add(Filter.equals(COL_CUSTOMER, customer));
        captions.add(dataRow.getValue(ALS_COMPANY_NAME));
      }

    } else {
      String customers = getEditorValue(NAME_CUSTOMERS);
      if (!BeeUtils.isEmpty(customers)) {
        filter.add(Filter.any(COL_CUSTOMER, DataUtils.parseIdSet(customers)));
        captions.add(getSelectorLabel(NAME_CUSTOMERS));
      }
    }

    if (cellElement.hasClassName(STYLE_SECONDARY)
        && BeeUtils.isPositive(dataRow.getInt(AR_SECONDARY))) {
      filter.add(Filter.notNull(COL_ASSESSMENT));
      captions.add(Localized.dictionary().trAssessmentReportSecondary());
    }

    String caption = BeeUtils.notEmpty(BeeUtils.joinItems(captions),
        Localized.dictionary().trAssessmentRequests());

    drillDown(DRILL_DOWN_GRID_NAME, caption, filter);
  }
}
