package com.butent.bee.client.modules.payroll;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.Storage;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.HasSummaryChangeHandlers;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent.Handler;
import com.butent.bee.client.event.logical.VisibilityChangeEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.payroll.Earnings;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

abstract class EarningsWidget extends Flow implements HasSummaryChangeHandlers, Printable,
    VisibilityChangeEvent.Handler {

  private static final String STYLE_PREFIX = PayrollKeeper.STYLE_PREFIX + "earn-";

  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";
  private static final String STYLE_TABLE = STYLE_PREFIX + "table";

  private static final String STYLE_HEADER_PANEL = STYLE_PREFIX + "header-panel";
  private static final String STYLE_CAPTION = STYLE_PREFIX + "caption";
  private static final String STYLE_ACTION = STYLE_PREFIX + "action";

  private static final String STYLE_MONTH_SELECTOR = STYLE_PREFIX + "month-selector";
  private static final String STYLE_MONTH_PANEL = STYLE_PREFIX + "month-panel";
  private static final String STYLE_MONTH_LABEL = STYLE_PREFIX + "month-label";
  private static final String STYLE_MONTH_ACTIVE = STYLE_PREFIX + "month-active";

  private static final String STYLE_PARTITION_TITLE = STYLE_PREFIX + "partition-title";
  private static final String STYLE_COL_GROUP_LABEL = STYLE_PREFIX + "col-group-label";
  private static final String STYLE_COL_LABEL = STYLE_PREFIX + "col-label";
  private static final String STYLE_COL_TOTAL_LABEL = STYLE_PREFIX + "col-total-label";

  private static final String STYLE_PARTITION_PANEL = STYLE_PREFIX + "partition-panel";
  private static final String STYLE_PARTITION_NAME = STYLE_PREFIX + "partition-name";
  private static final String STYLE_PARTITION_INFO = STYLE_PREFIX + "partition-info";
  private static final String STYLE_PARTITION_PERIOD = STYLE_PREFIX + "partition-period";
  private static final String STYLE_PARTITION_SUBST = STYLE_PREFIX + "partition-subst";

  private static final String STYLE_FUND = STYLE_PREFIX + "fund";
  private static final String STYLE_WAGE = STYLE_PREFIX + "wage";
  private static final String STYLE_DAYS = STYLE_PREFIX + "days";
  private static final String STYLE_HOURS = STYLE_PREFIX + "hours";
  private static final String STYLE_AMOUNT = STYLE_PREFIX + "amount";
  private static final String STYLE_TOTAL = STYLE_PREFIX + "total";

  private static final String STYLE_PLANNED = STYLE_PREFIX + "planned";
  private static final String STYLE_ACTUAL = STYLE_PREFIX + "actual";
  private static final String STYLE_HOLY = STYLE_PREFIX + "holy";

  private static final String STYLE_TOTAL_LABEL = STYLE_PREFIX + "total-label";
  private static final String STYLE_TOTAL_ROW = STYLE_PREFIX + "total-row";

  private static final String KEY_YM = "ym";

  private static final String KEY_PART = "part";
  private static final String KEY_SUBST = "subst";

  private static final String NAME_ACTIVE_MONTH = "ActiveMonth";

  private static final String STORAGE_KEY_PREFIX = "Earnings";

  private static final int HEADER_ROW = 0;
  private static final int MONTH_ROW = HEADER_ROW + 1;
  private static final int COL_GROUP_LABEL_ROW = MONTH_ROW + 1;
  private static final int COL_LABEL_ROW = COL_GROUP_LABEL_ROW + 1;
  private static final int EARNINGS_START_ROW = COL_LABEL_ROW + 1;

  private static final int MONTH_SELECTOR_COL = 0;
  private static final int MONTH_PANEL_START_COL = 1;

  private static final int ROW_LABEL_COL = 0;

  private static final int GROUP_PLANNED_START_COL = ROW_LABEL_COL + 1;
  private static final int FUND_COL = GROUP_PLANNED_START_COL;
  private static final int PLANNED_DAYS_COL = FUND_COL + 1;
  private static final int PLANNED_HOURS_COL = PLANNED_DAYS_COL + 1;
  private static final int WAGE_COL = PLANNED_HOURS_COL + 1;
  private static final int GROUP_PLANNED_END_COL = WAGE_COL;

  private static final int GROUP_ACTUAL_START_COL = GROUP_PLANNED_END_COL + 1;
  private static final int ACTUAL_DAYS_COL = GROUP_ACTUAL_START_COL;
  private static final int ACTUAL_HOURS_COL = ACTUAL_DAYS_COL + 1;
  private static final int HOLY_DAYS_COL = ACTUAL_HOURS_COL + 1;
  private static final int HOLY_HOURS_COL = HOLY_DAYS_COL + 1;
  private static final int EARNINGS_WITHOUT_HOLIDAYS_COL = HOLY_HOURS_COL + 1;
  private static final int EARNINGS_FOR_HOLIDAYS_COL = EARNINGS_WITHOUT_HOLIDAYS_COL + 1;
  private static final int TOTAL_EARNINGS_COL = EARNINGS_FOR_HOLIDAYS_COL + 1;
  private static final int GROUP_ACTUAL_END_COL = TOTAL_EARNINGS_COL;

  private static final int LAST_COL = TOTAL_EARNINGS_COL;

  private static final NumberFormat FUND_FORMAT = Format.getDecimalFormat(Earnings.FUND_PRECISION);
  private static final NumberFormat WAGE_FORMAT = Format.getDecimalFormat(Earnings.WAGE_PRECISION);
  private static final NumberFormat AMOUNT_FORMAT =
      Format.getDecimalFormat(Earnings.AMOUNT_PRECISION);

  private static final Set<String> NON_PRINTABLE = Sets.newHashSet(STYLE_ACTION,
      STYLE_MONTH_SELECTOR);

  private static String formatMillis(Long millis) {
    return TimeUtils.renderTime(millis, false);
  }

  private static String storageKey(String name) {
    return Storage.getUserKey(STORAGE_KEY_PREFIX, name);
  }

  private final ScheduleParent scheduleParent;

  private BeeRowSet emData;
  private BeeRowSet obData;

  private final HtmlTable table;

  private YearMonth activeMonth;

  private boolean summarize = true;
  private Value summary;

  private final List<com.google.web.bindery.event.shared.HandlerRegistration> registry =
      new ArrayList<>();

  EarningsWidget(ScheduleParent scheduleParent) {
    super(STYLE_CONTAINER);

    this.scheduleParent = scheduleParent;
    addStyleName(STYLE_PREFIX + scheduleParent.getStyleSuffix());

    this.table = new HtmlTable(STYLE_TABLE);
    add(table);
  }

  @Override
  public HandlerRegistration addSummaryChangeHandler(Handler handler) {
    return addHandler(handler, SummaryChangeEvent.getType());
  }

  @Override
  public Element getPrintElement() {
    return table.getElement();
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    if (StyleUtils.hasAnyClass(source, NON_PRINTABLE)) {
      return false;
    } else if (source.hasClassName(STYLE_MONTH_LABEL)) {
      return source.hasClassName(STYLE_MONTH_ACTIVE);
    } else {
      return true;
    }
  }

  @Override
  public void onVisibilityChange(VisibilityChangeEvent event) {
    if (event.isVisible() && DomUtils.isOrHasAncestor(getElement(), event.getId())) {
      refresh();
    }
  }

  @Override
  public Value getSummary() {
    return summary;
  }

  @Override
  public void setSummarize(boolean summarize) {
    this.summarize = summarize;
  }

  @Override
  public boolean summarize() {
    return summarize;
  }

  protected BeeRow findEmployee(long id) {
    if (DataUtils.isEmpty(getEmData())) {
      return null;
    } else {
      return getEmData().getRowById(id);
    }
  }

  protected BeeRow findObject(long id) {
    if (DataUtils.isEmpty(getObData())) {
      return null;
    } else {
      return getObData().getRowById(id);
    }
  }

  protected BeeRowSet getEmData() {
    return emData;
  }

  protected BeeRowSet getObData() {
    return obData;
  }

  protected abstract List<Integer> getPartitionContactIndexes();

  protected abstract List<BeeColumn> getPartitionDataColumns();

  protected abstract List<Integer> getPartitionInfoIndexes();

  protected abstract List<Integer> getPartitionNameIndexes();

  protected abstract Long getPartitionId(Earnings item);

  protected abstract BeeRow getPartitionRow(Earnings item);

  protected abstract long getRelationId();

  @Override
  protected void onLoad() {
    super.onLoad();

    EventUtils.clearRegistry(registry);
    registry.add(VisibilityChangeEvent.register(this));
  }

  @Override
  protected void onUnload() {
    EventUtils.clearRegistry(registry);

    super.onUnload();
  }

  void refresh() {
    getMonths(months -> {
      setSummary(new IntegerValue(BeeUtils.size(months)));
      SummaryChangeEvent.maybeFire(this);

      if (BeeUtils.isEmpty(months)) {
        clearUi();

      } else {
        if (activeMonth == null || !months.contains(activeMonth)) {
          String s = BeeKeeper.getStorage().get(storageKey(NAME_ACTIVE_MONTH));
          YearMonth ym = BeeUtils.isEmpty(s) ? null : YearMonth.parse(s);

          if (ym == null || !months.contains(ym)) {
            ym = BeeUtils.getLast(months);
          }
          setActiveMonth(ym);
        }

        getEarnings(activeMonth, earnings -> {
          if (BeeUtils.isEmpty(earnings)) {
            render(months, earnings);

          } else {
            loadRelatedData(earnings, () -> {
              if (earnings.size() > 1) {
                sortEarnings(earnings);
              }

              render(months, earnings);
            });
          }
        });
      }
    });
  }

  private boolean activateMonth(YearMonth ym) {
    if (ym == null || Objects.equals(activeMonth, ym)) {
      return false;

    } else {
      setActiveMonth(ym);
      return true;
    }
  }

  private void clearData() {
    if (getEmData() != null) {
      getEmData().clearRows();
    }
    if (getObData() != null) {
      getObData().clearRows();
    }
  }

  private void clearUi() {
    if (!table.isEmpty()) {
      table.clear();
    }
  }

  private void getEarnings(YearMonth ym, final Callback<List<Earnings>> callback) {
    ParameterList params = PayrollKeeper.createArgs(SVC_GET_EARNINGS);

    params.addQueryItem(Service.VAR_YEAR, ym.getYear());
    params.addQueryItem(Service.VAR_MONTH, ym.getMonth());
    params.addQueryItem(scheduleParent.getWorkScheduleRelationColumn(), getRelationId());

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        List<Earnings> earnings = new ArrayList<>();

        if (response.hasResponse()) {
          String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

          if (!ArrayUtils.isEmpty(arr)) {
            for (String s : arr) {
              earnings.add(Earnings.restore(s));
            }
          }
        }

        callback.onSuccess(earnings);
      }
    });
  }

  private String getEmployeeFullName(long id) {
    BeeRow row = findEmployee(id);

    if (row == null) {
      return null;
    } else {
      return BeeUtils.joinWords(DataUtils.getString(emData, row, COL_FIRST_NAME),
          DataUtils.getString(emData, row, COL_LAST_NAME));
    }
  }

  private void getMonths(final Callback<List<YearMonth>> callback) {
    ParameterList params = PayrollKeeper.createArgs(SVC_GET_SCHEDULED_MONTHS);
    params.addQueryItem(scheduleParent.getWorkScheduleRelationColumn(), getRelationId());

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        List<YearMonth> months = new ArrayList<>();

        if (response.hasResponse()) {
          for (String s : Splitter.on(BeeConst.CHAR_COMMA).split(response.getResponseAsString())) {
            YearMonth ym = YearMonth.parse(s);
            if (ym != null) {
              months.add(ym);
            }
          }
        }

        callback.onSuccess(months);
      }
    });
  }

  private String getSubstituteForLabel(Long id) {
    if (DataUtils.isId(id)) {
      return BeeUtils.bracket(getEmployeeFullName(id));
    } else {
      return null;
    }
  }

  private void loadRelatedData(Collection<Earnings> earnings, final Runnable callback) {
    Set<Long> employees = new HashSet<>();
    Set<Long> objects = new HashSet<>();

    for (Earnings item : earnings) {
      BeeUtils.addNotNull(employees, item.getEmployeeId());
      BeeUtils.addNotNull(employees, item.getSubstituteFor());

      BeeUtils.addNotNull(objects, item.getObjectId());
    }

    if (employees.isEmpty() && objects.isEmpty()) {
      callback.run();

    } else {
      Set<String> viewNames = new HashSet<>();
      Map<String, Filter> filters = new HashMap<>();

      if (!employees.isEmpty()) {
        viewNames.add(VIEW_EMPLOYEES);
        filters.put(VIEW_EMPLOYEES, Filter.idIn(employees));
      }
      if (!objects.isEmpty()) {
        viewNames.add(VIEW_LOCATIONS);
        filters.put(VIEW_LOCATIONS, Filter.idIn(objects));
      }

      Queries.getData(viewNames, filters, CachingPolicy.NONE, new Queries.DataCallback() {
        @Override
        public void onSuccess(Collection<BeeRowSet> result) {
          clearData();

          for (BeeRowSet rowSet : result) {
            switch (rowSet.getViewName()) {
              case VIEW_EMPLOYEES:
                setEmData(rowSet);
                break;

              case VIEW_LOCATIONS:
                setObData(rowSet);
                break;
            }
          }

          callback.run();
        }
      });
    }
  }

  private void render(List<YearMonth> months, List<Earnings> earnings) {
    clearUi();

    renderHeaders(months);

    int r = EARNINGS_START_ROW;

    if (!BeeUtils.isEmpty(earnings)) {
      List<Integer> nameIndexes = getPartitionNameIndexes();
      List<Integer> contactIndexes = getPartitionContactIndexes();
      List<Integer> infoIndexes = getPartitionInfoIndexes();

      for (Earnings item : earnings) {
        Widget rowLabel = renderRowLabel(item, nameIndexes, contactIndexes, infoIndexes);
        table.setWidgetAndStyle(r, ROW_LABEL_COL, rowLabel, STYLE_PARTITION_PANEL);

        renderEarnings(item, r);

        Element rowElement = table.getRowFormatter().getElement(r);
        DomUtils.setDataProperty(rowElement, KEY_PART, getPartitionId(item));
        if (item.isSubstitution()) {
          DomUtils.setDataProperty(rowElement, KEY_SUBST, item.getSubstituteFor());
        }

        r++;
      }

      if (earnings.size() > 1) {
        renderFooters(earnings, r);
      }
    }
  }

  private void renderEarnings(Earnings item, int r) {
    if (BeeUtils.isPositive(item.getSalaryFund())) {
      table.setText(r, FUND_COL, FUND_FORMAT.format(item.getSalaryFund()), STYLE_FUND);
    }

    if (BeeUtils.isPositive(item.getPlannedDays())) {
      table.setValue(r, PLANNED_DAYS_COL, item.getPlannedDays(), STYLE_DAYS, STYLE_PLANNED);
    }
    if (BeeUtils.isPositive(item.getPlannedMillis())) {
      table.setText(r, PLANNED_HOURS_COL, formatMillis(item.getPlannedMillis()),
          STYLE_HOURS, STYLE_PLANNED);
    }

    if (BeeUtils.isPositive(item.computeWage())) {
      table.setText(r, WAGE_COL, WAGE_FORMAT.format(item.computeWage()), STYLE_WAGE);
    }

    if (BeeUtils.isPositive(item.getActualDays())) {
      table.setValue(r, ACTUAL_DAYS_COL, item.getActualDays(), STYLE_DAYS, STYLE_ACTUAL);
    }
    if (BeeUtils.isPositive(item.getActualMillis())) {
      table.setText(r, ACTUAL_HOURS_COL, formatMillis(item.getActualMillis()),
          STYLE_HOURS, STYLE_ACTUAL);
    }

    if (BeeUtils.isPositive(item.getHolyDays())) {
      table.setValue(r, HOLY_DAYS_COL, item.getHolyDays(), STYLE_DAYS, STYLE_HOLY);
    }
    if (BeeUtils.isPositive(item.getHolyMillis())) {
      table.setText(r, HOLY_HOURS_COL, formatMillis(item.getHolyMillis()),
          STYLE_HOURS, STYLE_HOLY);
    }

    Double withoutHolidays = item.amountWithoutHolidays();
    if (BeeUtils.isPositive(withoutHolidays)) {
      table.setText(r, EARNINGS_WITHOUT_HOLIDAYS_COL, AMOUNT_FORMAT.format(withoutHolidays),
          STYLE_AMOUNT);
    }
    Double forHolidays = item.amountForHolidays();
    if (BeeUtils.isPositive(forHolidays)) {
      table.setText(r, EARNINGS_FOR_HOLIDAYS_COL, AMOUNT_FORMAT.format(forHolidays),
          STYLE_AMOUNT, STYLE_HOLY);
    }

    Double total = item.total();
    String text = BeeUtils.isPositive(total) ? AMOUNT_FORMAT.format(total) : BeeConst.STRING_EMPTY;
    table.setText(r, TOTAL_EARNINGS_COL, text, STYLE_AMOUNT, STYLE_TOTAL);
  }

  private void renderFooters(List<Earnings> earnings, int r) {
    table.setWidgetAndStyle(r, ROW_LABEL_COL,
        new Label(Localized.dictionary().totalOf()), STYLE_TOTAL_LABEL);

    double fund = BeeConst.DOUBLE_ZERO;

    long plannedMillis = 0L;
    long actualMillis = 0L;
    long holyMillis = 0L;

    double withoutHolidays = BeeConst.DOUBLE_ZERO;
    double forHolidays = BeeConst.DOUBLE_ZERO;
    double total = BeeConst.DOUBLE_ZERO;

    for (Earnings item : earnings) {
      if (BeeUtils.isPositive(item.getSalaryFund())) {
        fund += item.getSalaryFund();
      }

      if (BeeUtils.isPositive(item.getPlannedMillis())) {
        plannedMillis += item.getPlannedMillis();
      }
      if (BeeUtils.isPositive(item.getActualMillis())) {
        actualMillis += item.getActualMillis();
      }
      if (BeeUtils.isPositive(item.getHolyMillis())) {
        holyMillis += item.getHolyMillis();
      }

      Double amount = item.amountWithoutHolidays();
      if (BeeUtils.isPositive(amount)) {
        withoutHolidays += amount;
      }
      amount = item.amountForHolidays();
      if (BeeUtils.isPositive(amount)) {
        forHolidays += amount;
      }
      amount = item.total();
      if (BeeUtils.isPositive(amount)) {
        total += amount;
      }
    }

    if (BeeUtils.isPositive(fund)) {
      table.setText(r, FUND_COL, FUND_FORMAT.format(fund), STYLE_FUND);
    }

    if (BeeUtils.isPositive(plannedMillis)) {
      table.setText(r, PLANNED_HOURS_COL, formatMillis(plannedMillis), STYLE_HOURS, STYLE_PLANNED);
    }
    if (BeeUtils.isPositive(actualMillis)) {
      table.setText(r, ACTUAL_HOURS_COL, formatMillis(actualMillis), STYLE_HOURS, STYLE_ACTUAL);
    }
    if (BeeUtils.isPositive(holyMillis)) {
      table.setText(r, HOLY_HOURS_COL, formatMillis(holyMillis), STYLE_HOURS, STYLE_HOLY);
    }

    if (BeeUtils.isPositive(withoutHolidays)) {
      table.setText(r, EARNINGS_WITHOUT_HOLIDAYS_COL, AMOUNT_FORMAT.format(withoutHolidays),
          STYLE_AMOUNT);
    }
    if (BeeUtils.isPositive(forHolidays)) {
      table.setText(r, EARNINGS_FOR_HOLIDAYS_COL, AMOUNT_FORMAT.format(forHolidays),
          STYLE_AMOUNT, STYLE_HOLY);
    }

    String text = BeeUtils.isPositive(total) ? AMOUNT_FORMAT.format(total) : BeeConst.STRING_EMPTY;
    table.setText(r, TOTAL_EARNINGS_COL, text, STYLE_AMOUNT, STYLE_TOTAL);

    table.getRowFormatter().addStyleName(r, STYLE_TOTAL_ROW);
  }

  private void renderHeaders(List<YearMonth> months) {
    Flow headerPanel = new Flow();

    Label caption = new Label(getCaption());
    caption.addStyleName(STYLE_CAPTION);
    headerPanel.add(caption);

    FaLabel refresh = new FaLabel(Action.REFRESH.getIcon(), STYLE_ACTION);
    refresh.addStyleName(STYLE_PREFIX + Action.REFRESH.getStyleSuffix());
    refresh.setTitle(Action.REFRESH.getCaption());
    StyleUtils.enableAnimation(Action.REFRESH, refresh);

    refresh.addClickHandler(event -> refresh());
    headerPanel.add(refresh);

    FaLabel print = new FaLabel(Action.PRINT.getIcon(), STYLE_ACTION);
    print.addStyleName(STYLE_PREFIX + Action.PRINT.getStyleSuffix());
    print.setTitle(Action.PRINT.getCaption());
    StyleUtils.enableAnimation(Action.PRINT, print);

    print.addClickHandler(event -> Printer.print(this));
    headerPanel.add(print);

    table.setWidgetAndStyle(HEADER_ROW, 0, headerPanel, STYLE_HEADER_PANEL);
    table.getCellFormatter().setColSpan(HEADER_ROW, 0, LAST_COL + 1);

    Widget monthSelector = renderMonthSelector(months);
    table.setWidgetAndStyle(MONTH_ROW, MONTH_SELECTOR_COL, monthSelector, STYLE_MONTH_SELECTOR);

    Widget monthPanel = renderMonths(months);
    table.setWidgetAndStyle(MONTH_ROW, MONTH_PANEL_START_COL, monthPanel, STYLE_MONTH_PANEL);
    table.getCellFormatter().setColSpan(MONTH_ROW, MONTH_PANEL_START_COL,
        LAST_COL - MONTH_PANEL_START_COL + 1);

    int c = GROUP_PLANNED_START_COL;
    Label plannedGroup = new Label(Localized.dictionary().workSchedulePlanned());
    table.setWidgetAndStyle(COL_GROUP_LABEL_ROW, c, plannedGroup, STYLE_COL_GROUP_LABEL);
    table.getCellFormatter().setColSpan(COL_GROUP_LABEL_ROW, c,
        GROUP_PLANNED_END_COL - GROUP_PLANNED_START_COL + 1);

    c++;
    Label actualGroup = new Label(Localized.dictionary().workScheduleActual());
    table.setWidgetAndStyle(COL_GROUP_LABEL_ROW, c, actualGroup, STYLE_COL_GROUP_LABEL);
    table.getCellFormatter().setColSpan(COL_GROUP_LABEL_ROW, c,
        GROUP_ACTUAL_END_COL - GROUP_ACTUAL_START_COL + 1);

    table.setWidgetAndStyle(COL_LABEL_ROW, ROW_LABEL_COL,
        new Label(scheduleParent.getPartitionTitle()), STYLE_PARTITION_TITLE);

    table.setWidgetAndStyle(COL_LABEL_ROW, FUND_COL,
        new Label(Localized.dictionary().salaryFund()), STYLE_COL_LABEL);
    table.setWidgetAndStyle(COL_LABEL_ROW, PLANNED_DAYS_COL,
        new Label(Localized.dictionary().days()), STYLE_COL_LABEL);
    table.setWidgetAndStyle(COL_LABEL_ROW, PLANNED_HOURS_COL,
        new Label(Localized.dictionary().hours()), STYLE_COL_LABEL);
    table.setWidgetAndStyle(COL_LABEL_ROW, WAGE_COL,
        new Label(Localized.dictionary().hourlyWage()), STYLE_COL_LABEL);

    table.setWidgetAndStyle(COL_LABEL_ROW, ACTUAL_DAYS_COL,
        new Label(Localized.dictionary().days()), STYLE_COL_LABEL);
    table.setWidgetAndStyle(COL_LABEL_ROW, ACTUAL_HOURS_COL,
        new Label(Localized.dictionary().hours()), STYLE_COL_LABEL);
    table.setWidgetAndStyle(COL_LABEL_ROW, HOLY_DAYS_COL,
        new Label(Localized.dictionary().workScheduleHolidaysInclusiveShort()), STYLE_COL_LABEL);
    table.setWidgetAndStyle(COL_LABEL_ROW, HOLY_HOURS_COL,
        new Label(Localized.dictionary().workScheduleHolihoursInclusiveShort()), STYLE_COL_LABEL);

    table.setWidgetAndStyle(COL_LABEL_ROW, EARNINGS_WITHOUT_HOLIDAYS_COL,
        new Label(Localized.dictionary().payrollEarningsWithoutHolidays()), STYLE_COL_LABEL);
    table.setWidgetAndStyle(COL_LABEL_ROW, EARNINGS_FOR_HOLIDAYS_COL,
        new Label(Localized.dictionary().payrollEarningsForHolidays()), STYLE_COL_LABEL);
    table.setWidgetAndStyle(COL_LABEL_ROW, TOTAL_EARNINGS_COL,
        new Label(Localized.dictionary().payrollEarningsTotal()), STYLE_COL_TOTAL_LABEL);
  }

  private Widget renderMonths(List<YearMonth> months) {
    Flow panel = new Flow();

    int size = months.size();
    int from = Math.max(size - 6, 0);

    for (YearMonth ym : months.subList(from, size)) {
      Label widget = new Label(Format.renderYearMonth(ym));

      widget.addStyleName(STYLE_MONTH_LABEL);
      if (ym.equals(activeMonth)) {
        widget.addStyleName(STYLE_MONTH_ACTIVE);
      }

      DomUtils.setDataProperty(widget.getElement(), KEY_YM, ym.serialize());

      widget.addClickHandler(event -> {
        String s = DomUtils.getDataProperty(EventUtils.getEventTargetElement(event), KEY_YM);

        if (!BeeUtils.isEmpty(s) && activateMonth(YearMonth.parse(s))) {
          BeeKeeper.getStorage().set(storageKey(NAME_ACTIVE_MONTH), s);
          refresh();
        }
      });

      panel.add(widget);
    }

    return panel;
  }

  private Widget renderMonthSelector(final List<YearMonth> months) {
    Button selector = new Button();
    if (activeMonth != null) {
      selector.setText(Format.renderYearMonth(activeMonth));
    }

    selector.addClickHandler(event -> {
      List<String> labels = new ArrayList<>();
      for (YearMonth ym : months) {
        labels.add(Format.renderYearMonth(ym));
      }

      Global.choiceWithCancel(Localized.dictionary().yearMonth(), null, labels, value -> {
        YearMonth ym = BeeUtils.getQuietly(months, value);

        if (activateMonth(ym)) {
          BeeKeeper.getStorage().set(storageKey(NAME_ACTIVE_MONTH), ym.serialize());
          refresh();
        }
      });
    });

    return selector;
  }

  private Widget renderRowLabel(Earnings item, List<Integer> nameIndexes,
      List<Integer> contactIndexes, List<Integer> infoIndexes) {

    Flow panel = new Flow();

    BeeRow row = getPartitionRow(item);

    String text = (row == null)
        ? DataUtils.buildIdList(getPartitionId(item))
        : DataUtils.join(getPartitionDataColumns(), row, nameIndexes, BeeConst.STRING_SPACE,
        Format.getDateRenderer(), Format.getDateTimeRenderer());

    Label nameWidget = new Label(text);
    nameWidget.addStyleName(STYLE_PARTITION_NAME);

    if (!BeeUtils.isEmpty(contactIndexes) && row != null) {
      String title = DataUtils.join(getPartitionDataColumns(), row, contactIndexes,
          BeeConst.STRING_EOL, Format.getDateRenderer(), Format.getDateTimeRenderer());
      if (!BeeUtils.isEmpty(title)) {
        nameWidget.setTitle(title);
      }
    }

    nameWidget.addClickHandler(event -> {
      Element target = EventUtils.getEventTargetElement(event);
      Long id = DomUtils.getDataPropertyLong(DomUtils.getParentRow(target, false), KEY_PART);

      if (DataUtils.isId(id)) {
        RowEditor.open(scheduleParent.getPartitionViewName(), id, Opener.MODAL);
      }
    });

    panel.add(nameWidget);

    if (!BeeUtils.isEmpty(infoIndexes) && row != null) {
      Label infoWidget = new Label(DataUtils.join(getPartitionDataColumns(), row, infoIndexes,
          BeeConst.DEFAULT_LIST_SEPARATOR, Format.getDateRenderer(), Format.getDateTimeRenderer()));
      infoWidget.addStyleName(STYLE_PARTITION_INFO);

      panel.add(infoWidget);
    }

    JustDate from = item.getDateFrom();
    JustDate until = item.getDateUntil();

    if (activeMonth != null
        && (from != null && !Objects.equals(from, activeMonth.getDate())
        || until != null && !Objects.equals(until, activeMonth.getLast()))) {

      Label periodWidget = new Label(TimeUtils.renderPeriod(Format.renderDate(from),
          Format.renderDate(until)));
      periodWidget.addStyleName(STYLE_PARTITION_PERIOD);

      panel.add(periodWidget);
    }

    if (item.isSubstitution()) {
      Label substWidget = new Label(getSubstituteForLabel(item.getSubstituteFor()));
      substWidget.addStyleName(STYLE_PARTITION_SUBST);

      substWidget.addClickHandler(event -> {
        Element target = EventUtils.getEventTargetElement(event);
        Long id = DomUtils.getDataPropertyLong(DomUtils.getParentRow(target, false), KEY_SUBST);

        if (DataUtils.isId(id)) {
          RowEditor.open(VIEW_EMPLOYEES, id, Opener.MODAL);
        }
      });

      panel.add(substWidget);
    }

    return panel;
  }

  private void setActiveMonth(YearMonth activeMonth) {
    this.activeMonth = activeMonth;
  }

  private void setEmData(BeeRowSet emData) {
    this.emData = emData;
  }

  private void setObData(BeeRowSet obData) {
    this.obData = obData;
  }

  private void setSummary(Value summary) {
    this.summary = summary;
  }

  private void sortEarnings(List<Earnings> earnings) {
    final Map<Long, Integer> partitionIndexes = new HashMap<>();
    final Map<Long, Integer> substitutionIndexes = new HashMap<>();

    for (Earnings item : earnings) {
      Long id = getPartitionId(item);

      if (id != null) {
        int index = BeeConst.UNDEF;

        switch (scheduleParent) {
          case LOCATION:
            if (getEmData() != null) {
              index = getEmData().getRowIndex(id);
            }
            break;

          case EMPLOYEE:
            if (getObData() != null) {
              index = getObData().getRowIndex(id);
            }
            break;
        }

        if (!BeeConst.isUndef(index)) {
          partitionIndexes.put(id, index);
        }
      }

      if (item.isSubstitution() && getEmData() != null) {
        int index = getEmData().getRowIndex(item.getSubstituteFor());

        if (!BeeConst.isUndef(index)) {
          substitutionIndexes.put(item.getSubstituteFor(), index);
        }
      }
    }

    earnings.sort((Earnings e1, Earnings e2) -> {
      int result = BeeUtils.compareNullsLast(partitionIndexes.get(getPartitionId(e1)),
          partitionIndexes.get(getPartitionId(e2)));

      if (result == BeeConst.COMPARE_EQUAL) {
        result = Boolean.compare(e1.isSubstitution(), e2.isSubstitution());
      }
      if (result == BeeConst.COMPARE_EQUAL && e1.isSubstitution()) {
        result = BeeUtils.compareNullsLast(substitutionIndexes.get(e1.getSubstituteFor()),
            substitutionIndexes.get(e2.getSubstituteFor()));
      }

      if (result == BeeConst.COMPARE_EQUAL) {
        result = BeeUtils.compareNullsFirst(e1.getDateFrom(), e2.getDateFrom());
      }

      return result;
    });
  }
}
