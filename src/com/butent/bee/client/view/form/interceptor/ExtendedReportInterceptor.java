package com.butent.bee.client.view.form.interceptor;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeMultimap;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.output.Exporter;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.output.ReportItem;
import com.butent.bee.client.output.ReportParameters;
import com.butent.bee.client.output.ReportValue;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.CustomSpan;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XRow;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.report.ReportFunction;
import com.butent.bee.shared.report.ReportInfo;
import com.butent.bee.shared.report.ReportInfoItem;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class ExtendedReportInterceptor extends ReportInterceptor {

  private static final class ResultHolder {
    private static final String CELL = "cell";
    private static final String COL = "col";
    private static final String GROUP = "group";
    private static final String GROUP_COL = "group_col";
    private static final String ROW = "row";
    private static final String TOTAL = "total";

    private final Multimap<ReportValue, ReportValue> rowGroups = TreeMultimap.create();
    private final Set<ReportValue> colGroups = new TreeSet<>();
    private final Table<ReportValue, ReportItem, Object> values = HashBasedTable.create();

    public void addValues(ReportValue rowGroup, ReportValue[] rowValues, ReportValue colGroup,
        SimpleRow row, ReportInfoItem infoItem) {

      ReportValue rowValue = ReportValue.of(rowValues);

      ReportValue key = getKey(CELL, rowGroup, rowValue, colGroup);
      ReportItem item = infoItem.getItem();
      ReportValue value = item.evaluate(row);
      Object calc = item.calculate(getValue(key, item), value, infoItem.getFunction());

      if (calc == null) {
        return;
      }
      if (!rowGroups.containsEntry(rowGroup, rowValue)) {
        rowGroups.put(rowGroup, rowValue);
      }
      if (!colGroups.contains(colGroup)) {
        colGroups.add(colGroup);
      }
      putValue(key, item, calc);

      if (infoItem.isRowSummary() || infoItem.isSorted()) {
        key = getKey(ROW, rowGroup, rowValue);
        putValue(key, item, item.calculate(getValue(key, item), value, infoItem.getFunction()));
      }
      if (infoItem.isColSummary()) {
        if (infoItem.isRowSummary()) {
          key = getKey(TOTAL);
          putValue(key, item, item.calculate(getValue(key, item), value, infoItem.getFunction()));
        }
        key = getKey(COL, colGroup);
        putValue(key, item, item.calculate(getValue(key, item), value, infoItem.getFunction()));
      }
      if (infoItem.isGroupSummary()) {
        if (infoItem.isRowSummary() || infoItem.isSorted()) {
          key = getKey(GROUP, rowGroup);
          putValue(key, item, item.calculate(getValue(key, item), value, infoItem.getFunction()));
        }
        key = getKey(GROUP_COL, rowGroup, colGroup);
        putValue(key, item, item.calculate(getValue(key, item), value, infoItem.getFunction()));
      }
    }

    private static ReportValue getKey(String id, ReportValue... prm) {
      int l = ArrayUtils.length(prm);
      ReportValue[] params = new ReportValue[l + 1];
      params[0] = ReportValue.of(id);

      for (int i = 0; i < l; i++) {
        params[i + 1] = prm[i];
      }
      return ReportValue.of(params);
    }

    public Object getCellValue(ReportValue rowGroup, ReportValue[] row, ReportValue colGroup,
        ReportItem item) {
      return getValue(getKey(CELL, rowGroup, ReportValue.of(row), colGroup), item);
    }

    public Collection<ReportValue> getColGroups() {
      return colGroups;
    }

    public Object getColTotal(ReportValue colGroup, ReportItem item) {
      return getValue(getKey(COL, colGroup), item);
    }

    public Object getGroupTotal(ReportValue rowGroup, ReportItem item) {
      return getValue(getKey(GROUP, rowGroup), item);
    }

    public Object getGroupValue(ReportValue rowGroup, ReportValue colGroup, ReportItem item) {
      return getValue(getKey(GROUP_COL, rowGroup, colGroup), item);
    }

    public Collection<ReportValue> getRowGroups(ReportInfoItem sortedItem) {
      List<ReportValue> result = new ArrayList<>(rowGroups.keySet());

      if (sortedItem != null) {
        Map<ReportValue, Object> items = new HashMap<>();

        for (ReportValue rowGroup : result) {
          items.put(rowGroup, getGroupTotal(rowGroup, sortedItem.getItem()));
        }
        sort(result, items, sortedItem.getDescending());
      }
      return result;
    }

    public Collection<ReportValue[]> getRows(ReportValue rowGroup, ReportInfoItem sortedItem) {
      List<ReportValue> rows = new ArrayList<>(rowGroups.get(rowGroup));

      if (sortedItem != null) {
        Map<ReportValue, Object> items = new HashMap<>();

        for (ReportValue row : rows) {
          items.put(row, getRowTotal(rowGroup, row.getValues(), sortedItem.getItem()));
        }
        sort(rows, items, sortedItem.getDescending());
      }
      List<ReportValue[]> result = new ArrayList<>();

      for (ReportValue row : rows) {
        result.add(row.getValues());
      }
      return result;
    }

    public Object getRowTotal(ReportValue rowGroup, ReportValue[] row, ReportItem item) {
      return getValue(getKey(ROW, rowGroup, ReportValue.of(row)), item);
    }

    public Object getTotal(ReportItem item) {
      return getValue(getKey(TOTAL), item);
    }

    private Object getValue(ReportValue row, ReportItem col) {
      return values.get(row, col);
    }

    private void putValue(ReportValue row, ReportItem col, Object value) {
      if (value != null) {
        values.put(row, col, value);
      }
    }

    private static void sort(List<ReportValue> result, final Map<ReportValue, Object> items,
        final boolean descending) {

      Collections.sort(result, new Comparator<ReportValue>() {
        @SuppressWarnings("unchecked")
        @Override
        public int compare(ReportValue value1, ReportValue value2) {
          Object item1;
          Object item2;

          if (descending) {
            item1 = items.get(value2);
            item2 = items.get(value1);
          } else {
            item1 = items.get(value1);
            item2 = items.get(value2);
          }

          if (item1 == null) {
            if (item2 == null) {
              return BeeConst.COMPARE_EQUAL;
            } else {
              return BeeConst.COMPARE_LESS;
            }
          } else if (item2 == null) {
            return BeeConst.COMPARE_MORE;
          } else if (item1 instanceof Comparable) {
            return ((Comparable<Object>) item1).compareTo(item2);
          } else {
            return BeeConst.COMPARE_EQUAL;
          }
        }
      });
    }
  }

  private final class DrillHandler implements ClickHandler {

    private ReportInfo current;
    private String target;
    private ReportValue rowGroup;
    private ReportValue[] rowValues;
    private ReportValue colGroup;

    private DrillHandler(ReportInfo current, String target,
        ReportValue rowGroup, ReportValue[] rowValues, ReportValue colGroup) {

      this.current = Assert.notNull(current);
      this.target = target;
      this.rowGroup = rowGroup;
      this.rowValues = rowValues;
      this.colGroup = colGroup;
    }

    @Override
    public void onClick(ClickEvent event) {
      ReportInfo rep = null;

      if (!BeeUtils.isEmpty(target)) {
        for (ReportInfo reportInfo : reports) {
          if (Objects.equals(reportInfo.getCaption(), target)) {
            rep = ReportInfo.restore(reportInfo.serialize());
            break;
          }
        }
        if (rep == null) {
          getFormView().notifyWarning(Localized.getMessages().keyNotFound(target));
          return;
        }
        rep.getFilterItems().clear();
        rep.getFilterItems().addAll(current.getFilterItems());
      } else {
        rep = current;
      }
      List<ReportItem> filters = rep.getFilterItems();
      List<ReportItem> reportFilters = new ArrayList<>(filters);
      filters.clear();

      if (current.getColGrouping() != null) {
        setFilter(filters, colGroup, current.getColGrouping().getItem());
      }
      if (!BeeUtils.isEmpty(target) && current.getRowGrouping() != null) {
        setFilter(filters, rowGroup, current.getRowGrouping().getItem());
      }
      if (!ArrayUtils.isEmpty(rowValues)) {
        List<ReportInfoItem> infoItems = current.getRowItems();

        for (int i = 0; i < rowValues.length; i++) {
          setFilter(filters, rowValues[i], infoItems.get(i).getItem());
        }
      }
      for (ReportItem reportFilter : reportFilters) {
        if (!filters.contains(reportFilter)) {
          filters.add(reportFilter);
        }
      }
      getReport()
          .open(new ReportParameters(Collections.singletonMap(COL_RS_REPORT, rep.serialize())));
    }

    private List<String> getValues(ReportValue value) {
      List<String> values = new ArrayList<>();

      if (value.getValues() != null) {
        for (ReportValue val : value.getValues()) {
          values.addAll(getValues(val));
        }
      } else {
        values.add(value.getValue());
      }
      return values;
    }

    private void setFilter(List<ReportItem> filters, ReportValue value, ReportItem item) {
      if (value == null) {
        return;
      }
      List<String> values = getValues(value);
      List<ReportItem> members = item.getMembers();

      for (int i = 0; i < values.size(); i++) {
        String val = values.get(i);
        ReportItem itm = members.get(i);

        if (!BeeUtils.isEmpty(val) && itm.getFilterWidget() != null && !filters.contains(itm)) {
          filters.add(itm.setFilter(val));
        }
      }
    }
  }

  private static final String NAME_REPORT_CONTAINER = "ReportContainer";
  private static final String NAME_LAYOUT_CONTAINER = "LayoutContainer";
  private static final String NAME_FILTER_CONTAINER = "FilterContainer";

  private static final String NAME_CURRENCY = COL_CURRENCY;
  private static final String NAME_VAT = TradeConstants.COL_TRADE_VAT;

  private static final String STYLE_PREFIX = "bee-rep";

  private static final String STYLE_SUMMARY_ON = STYLE_PREFIX + "-summary-on";
  private static final String STYLE_SUMMARY_OFF = STYLE_PREFIX + "-summary-off";

  private static final String STYLE_SORT = STYLE_PREFIX + "-sort";
  private static final String STYLE_SORT_ASC = STYLE_SORT + "-asc";
  private static final String STYLE_SORT_DESC = STYLE_SORT + "-desc";

  private static final String STYLE_FUNCTION = STYLE_PREFIX + "-function";

  private static final String STYLE_ITEM = STYLE_PREFIX + "-item";
  private static final String STYLE_ADD = STYLE_ITEM + "-add";
  private static final String STYLE_REMOVE = STYLE_ITEM + "-remove";

  private static final String STYLE_REPORT = STYLE_PREFIX + "-report";
  private static final String STYLE_REPORT_NORMAL = STYLE_PREFIX + "-report-normal";
  private static final String STYLE_REPORT_ACTIVE = STYLE_PREFIX + "-report-active";

  private static final String STYLE_FILTER_CAPTION = STYLE_PREFIX + "-filter-cap";

  private static final String STYLE_COLGROUP = STYLE_PREFIX + "-cgroup";
  private static final String STYLE_COLGROUP_HEADER = STYLE_COLGROUP + "-hdr";
  private static final String STYLE_COLGROUP_SUMMARY_HEADER = STYLE_COLGROUP + "-tot-hdr";

  private static final String STYLE_ROWGROUP = STYLE_PREFIX + "-rgroup";
  private static final String STYLE_ROWGROUP_COL_SUMMARY = STYLE_ROWGROUP + "-col-tot";
  private static final String STYLE_ROWGROUP_SUMMARY = STYLE_ROWGROUP + "-tot";

  private static final String STYLE_ROW = STYLE_PREFIX + "-row";
  private static final String STYLE_ROW_HEADER = STYLE_ROW + "-hdr";
  private static final String STYLE_ROW_SUMMARY = STYLE_ROW + "-tot";
  private static final String STYLE_ROW_SUMMARY_HEADER = STYLE_ROW_SUMMARY + "-hdr";

  private static final String STYLE_COL = STYLE_PREFIX + "-col";
  private static final String STYLE_COL_HEADER = STYLE_COL + "-hdr";
  private static final String STYLE_COL_SUMMARY = STYLE_COL + "-tot";

  private static final String STYLE_SUMMARY = STYLE_PREFIX + "-tot";
  private static final String STYLE_SUMMARY_HEADER = STYLE_SUMMARY + "-hdr";
  private static final String STYLE_DRILLDOWN = STYLE_PREFIX + "-drill";
  private static final String STYLE_DRILLDOWN_ON = STYLE_DRILLDOWN + "-on";
  private static final String STYLE_DRILLDOWN_OFF = STYLE_DRILLDOWN + "-off";

  private final Report report;

  private final Set<ReportInfo> reports = new LinkedHashSet<>();
  private ReportInfo activeReport;

  public ExtendedReportInterceptor(Report report) {
    this.report = Assert.notNull(report);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof HasClickHandlers) {
      switch (name) {
        case NAME_FILTER_CONTAINER + "Add":
          ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              addFilterItem();
            }
          });
          break;
      }
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    switch (action) {
      case SAVE:
        if (activeReport != null && !activeReport.isEmpty()) {
          Global.inputString(Localized.getConstants().name(), null, new StringCallback() {
            @Override
            public void onSuccess(String value) {
              final ReportInfo rep = new ReportInfo(value);
              rep.deserialize(activeReport.serialize());

              for (ReportInfo reportInfo : reports) {
                if (Objects.equals(rep, reportInfo)) {
                  rep.setId(reportInfo.getId());
                  break;
                }
              }
              if (reports.contains(rep)) {
                Global.confirm(Localized.getConstants().reports(), Icon.QUESTION,
                    Arrays.asList(Localized.getMessages().valueExists(value),
                        Localized.getConstants().actionChange()), new ConfirmationCallback() {
                      @Override
                      public void onConfirm() {
                        saveReport(rep);
                      }
                    });
              } else {
                saveReport(rep);
              }
            }
          }, null, activeReport.getCaption());
        }
        return false;

      default:
        return super.beforeAction(action, presenter);
    }
  }

  @Override
  public String getCaption() {
    return getReport().getReportCaption();
  }

  @Override
  public Set<Action> getEnabledActions(Set<Action> defaultActions) {
    Set<Action> actions = super.getEnabledActions(defaultActions);
    actions.add(Action.SAVE);
    return actions;
  }

  @Override
  public FormInterceptor getInstance() {
    return new ExtendedReportInterceptor(getReport());
  }

  @Override
  public void onLoad(FormView form) {
    ReportParameters parameters = readParameters();
    ReportInfo rep = null;

    if (parameters != null) {
      String data = parameters.getText(COL_RS_REPORT);

      if (!BeeUtils.isEmpty(data)) {
        rep = ReportInfo.restore(data);
      }
      loadId(parameters, NAME_CURRENCY, form);
      loadBoolean(parameters, NAME_VAT, form);
    }
    getReports(rep);
    super.onLoad(form);
  }

  @Override
  public void onUnload(FormView form) {
    storeEditorValues(NAME_CURRENCY);
    storeBooleanValues(NAME_VAT);
  }

  @Override
  protected void clearFilter() {
    clearFilters(activeReport);
  }

  @Override
  protected void doReport() {
    if (getDataContainer() == null) {
      return;
    }
    getDataContainer().clear();

    if (activeReport == null || activeReport.isEmpty()) {
      return;
    }
    ParameterList params = BeeKeeper.getRpc()
        .createParameters(getReport().getModuleAndSub().getModule(), getReport().getReportName());

    String currency = getEditorValue(NAME_CURRENCY);

    if (DataUtils.isId(currency)) {
      params.addDataItem(COL_CURRENCY, currency);
    }
    if (getBoolean(NAME_VAT)) {
      params.addDataItem(TradeConstants.COL_TRADE_VAT, "1");
    }
    params.addDataItem(Service.VAR_DATA, Codec.beeSerialize(activeReport));

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getFormView());

        if (response.hasErrors()) {
          return;
        }
        renderData(SimpleRowSet.restore(response.getResponseAsString()));
      }
    });
  }

  @Override
  protected String getBookmarkLabel() {
    return null;
  }

  @Override
  protected Report getReport() {
    return report;
  }

  @Override
  protected ReportParameters getReportParameters() {
    if (activeReport == null) {
      return null;
    }
    ReportParameters parameters = new ReportParameters();

    parameters.add(COL_RS_REPORT, activeReport.serialize());

    addEditorValues(parameters, NAME_CURRENCY);
    addBooleanValues(parameters, NAME_VAT);

    return parameters;
  }

  @Override
  protected void export() {
    if (getDataContainer() != null && !getDataContainer().isEmpty()) {
      XSheet sheet = getSheet((HtmlTable) getDataContainer().getWidget(0));

      if (!sheet.isEmpty()) {
        Exporter.maybeExport(sheet, getReportCaption());
      }
    }
  }

  @Override
  protected boolean validateParameters(ReportParameters parameters) {
    return true;
  }

  private void activateReport(ReportInfo activeRep) {
    activeReport = activeRep;
    HasWidgets container = (HasWidgets) getFormView().getWidgetByName(NAME_REPORT_CONTAINER);

    if (container != null) {
      container.clear();

      HtmlTable ft = new HtmlTable(STYLE_REPORT);
      int r = 0;

      for (final ReportInfo rep : reports) {
        if (Objects.equals(rep, activeReport)) {
          ft.getRowFormatter().addStyleName(r, STYLE_REPORT_ACTIVE);
        } else {
          ft.getRowFormatter().addStyleName(r, STYLE_REPORT_NORMAL);
        }
        Label name = new Label(rep.getCaption());
        name.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (!Objects.equals(rep, activeReport)) {
              activateReport(rep);
            }
          }
        });
        ft.setWidget(r, 0, name);
        CustomDiv remove = new CustomDiv(STYLE_REMOVE);

        if (DataUtils.isId(rep.getId())) {
          remove.setText(String.valueOf(BeeConst.CHAR_TIMES));
          remove.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              Global.confirmRemove(Localized.getConstants().reports(), rep.getCaption(),
                  new ConfirmationCallback() {
                    @Override
                    public void onConfirm() {
                      Queries.deleteRow(VIEW_REPORT_SETTINGS, rep.getId());
                      reports.remove(rep);
                      activateReport(Objects.equals(rep, activeReport)
                          ? BeeUtils.peek(reports) : activeReport);
                    }
                  });
            }
          });
        }
        ft.setWidget(r, 1, remove);
        r++;
      }
      container.add(ft);
      renderLayout();
      renderFilters();
      getDataContainer().clear();
    }
  }

  private void addFilterItem() {
    if (activeReport != null) {
      final List<ReportItem> items = new ArrayList<>();
      List<String> options = new ArrayList<>();

      for (ReportItem item : getReport().getItems()) {
        if (item.getFilterWidget() != null) {
          items.add(item);
          options.add(item.getCaption());
        }
      }
      Global.choice(null, null, options, new ChoiceCallback() {
        @Override
        public void onSuccess(int value) {
          activeReport.getFilterItems().add(items.get(value));
          renderFilters();
        }
      });
    }
  }

  private Widget buildCaption(final ReportInfoItem infoItem, final Runnable onUpdate) {
    final boolean on = !BeeUtils.isEmpty(infoItem.getRelation());
    final InlineLabel cap = new InlineLabel(infoItem.getItem().getFormatedCaption());
    cap.addStyleName(on ? STYLE_DRILLDOWN_ON : STYLE_DRILLDOWN_OFF);
    cap.setTitle(BeeUtils.join(": ", Localized.getConstants().relation(), infoItem.getRelation()));

    cap.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        final List<String> options = new ArrayList<>();

        for (ReportInfo rep : reports) {
          if (!on || !Objects.equals(rep.getCaption(), infoItem.getRelation())) {
            options.add(rep.getCaption());
          }
        }
        if (on) {
          options.add(Localized.getConstants().clear() + "...");
        }
        Global.choice(cap.getTitle(), null, options, new ChoiceCallback() {
          @Override
          public void onSuccess(int value) {
            infoItem.setRelation(on && (value == options.size() - 1) ? null : options.get(value));
            onUpdate.run();
          }
        });
      }
    });
    return cap;
  }

  private static void clearFilters(ReportInfo rep) {
    if (rep != null) {
      for (ReportItem item : rep.getFilterItems()) {
        item.clearFilter();
      }
    }
  }

  private void getReports(final ReportInfo initialReport) {
    reports.clear();

    Queries.getRowSet(VIEW_REPORT_SETTINGS, Arrays.asList(COL_RS_PARAMETERS),
        Filter.and(Filter.equals(COL_RS_USER, BeeKeeper.getUser().getUserId()),
            Filter.equals(COL_RS_REPORT, getReport().getReportName()),
            Filter.isNull(COL_RS_CAPTION)), new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            for (ReportInfo rep : getReport().getReports()) {
              reports.add(ReportInfo.restore(rep.serialize()));
            }
            for (int i = 0; i < result.getNumberOfRows(); i++) {
              ReportInfo rep = ReportInfo.restore(result.getString(i, COL_RS_PARAMETERS));
              rep.setId(result.getRow(i).getId());
              reports.remove(rep);
              reports.add(rep);
            }
            if (initialReport != null) {
              ReportInfo rep = initialReport;

              if (reports.contains(rep)) {
                for (ReportInfo reportInfo : reports) {
                  if (Objects.equals(reportInfo, rep)) {
                    reportInfo.deserialize(rep.serialize());
                    rep = reportInfo;
                    break;
                  }
                }
              } else {
                reports.add(rep);
              }
              activateReport(rep);
              doReport();
            } else {
              activateReport(BeeUtils.peek(reports));
            }
          }
        });
  }

  private static XSheet getSheet(HtmlTable table) {
    XSheet sheet = new XSheet();
    int bold = sheet.registerFont(XFont.bold());
    Map<String, Integer> styleMap = new HashMap<>();

    Integer idx = sheet.registerStyle(XStyle.right());
    styleMap.put(ReportItem.STYLE_NUM, idx);
    styleMap.put(STYLE_COLGROUP_HEADER, idx);

    styleMap.put(STYLE_COLGROUP, sheet.registerStyle(XStyle.center()));

    XStyle xs = XStyle.center();
    xs.setFontRef(bold);
    idx = sheet.registerStyle(xs);
    styleMap.put(STYLE_COL_HEADER, idx);
    styleMap.put(STYLE_COLGROUP_SUMMARY_HEADER, idx);
    styleMap.put(STYLE_ROW_SUMMARY_HEADER, idx);

    xs = new XStyle();
    xs.setFontRef(bold);
    idx = sheet.registerStyle(xs);
    styleMap.put(STYLE_ROW_HEADER, idx);
    styleMap.put(STYLE_COL_SUMMARY, idx);
    styleMap.put(STYLE_ROW_SUMMARY, idx);
    styleMap.put(STYLE_SUMMARY, idx);

    xs = XStyle.right();
    xs.setFontRef(bold);
    styleMap.put(STYLE_SUMMARY_HEADER, sheet.registerStyle(xs));

    xs = XStyle.background("whitesmoke");
    idx = sheet.registerStyle(xs);
    styleMap.put(STYLE_ROWGROUP, idx);
    styleMap.put(STYLE_ROWGROUP_COL_SUMMARY, idx);

    xs = XStyle.background("whitesmoke");
    xs.setFontRef(bold);
    styleMap.put(STYLE_ROWGROUP_SUMMARY, sheet.registerStyle(xs));

    for (int r = 0; r < table.getRowCount(); r++) {
      int colSpan = 0;
      XRow xr = new XRow(r);
      sheet.add(xr);

      for (int c = 0; c < table.getCellCount(r); c++) {
        XCell xc = new XCell(c + colSpan);
        xr.add(xc);
        TableCellElement cell = table.getRowCells(r).get(c);
        xc.setColSpan(Math.max(cell.getColSpan(), 1));
        String text = cell.getInnerText();
        String[] styles = BeeUtils.split(table.getCellFormatter().getStyleName(r, c), ' ');
        Value value = null;
        xs = null;

        if (styles != null) {
          for (String style : styles) {
            if (styleMap.containsKey(style)) {
              if (xs == null) {
                xs = new XStyle();
              }
              xs = xs.merge(sheet.getStyle(styleMap.get(style)));
            }
            if (BeeUtils.same(style, ReportItem.STYLE_NUM)) {
              value = new NumberValue(BeeUtils.toDoubleOrNull(text));
            }
          }
        }
        if (xs != null) {
          xc.setStyleRef(sheet.registerStyle(xs));
        }
        xc.setValue(value != null ? value : new TextValue(text));
        colSpan += xc.getColSpan() - 1;
      }
    }
    sheet.autoSizeAll();
    return sheet;
  }

  private void renderData(SimpleRowSet rowSet) {
    getDataContainer().clear();

    if (activeReport == null || activeReport.isEmpty()) {
      return;
    }
    if (DataUtils.isEmpty(rowSet)) {
      getFormView().notifyWarning(Localized.getConstants().nothingFound());
      return;
    }
    ResultHolder result = new ResultHolder();

    List<ReportInfoItem> rowItems = activeReport.getRowItems();
    List<ReportInfoItem> colItems = activeReport.getColItems();

    for (final SimpleRow row : rowSet) {
      boolean ok = true;

      for (ReportItem filterItem : activeReport.getFilterItems()) {
        if (!filterItem.validate(row)) {
          ok = false;
          break;
        }
      }
      if (!ok) {
        continue;
      }
      ReportValue rowGroup;
      ReportValue colGroup;

      if (activeReport.getRowGrouping() != null) {
        rowGroup = activeReport.getRowGrouping().getItem().evaluate(row);
      } else {
        rowGroup = ReportValue.empty();
      }
      if (activeReport.getColGrouping() != null) {
        colGroup = activeReport.getColGrouping().getItem().evaluate(row);
      } else {
        colGroup = ReportValue.empty();
      }
      ReportValue[] details = new ReportValue[rowItems.size()];

      for (int i = 0; i < rowItems.size(); i++) {
        details[i] = rowItems.get(i).getItem().evaluate(row);
      }
      for (ReportInfoItem colItem : colItems) {
        result.addValues(rowGroup, details, colGroup, row, colItem);
      }
    }
    ReportInfoItem sortedItem = null;

    for (ReportInfoItem colItem : colItems) {
      if (colItem.isSorted()) {
        sortedItem = colItem;
        break;
      }
    }
    Collection<ReportValue> rowGroups = result.getRowGroups(sortedItem);
    Collection<ReportValue> colGroups = result.getColGroups();

    if (BeeUtils.isEmpty(rowGroups)) {
      getFormView().notifyWarning(Localized.getConstants().nothingFound());
      return;
    }
    ReportInfo activeCopy = ReportInfo.restore(activeReport.serialize());
    HtmlTable table = new HtmlTable(STYLE_PREFIX);
    // HEADER
    int r = 0;
    int c = 0;

    if (activeReport.getColGrouping() != null) {
      if (!BeeUtils.isEmpty(rowItems)) {
        table.getCellFormatter().setColSpan(r, c, rowItems.size());
      }
      table.setText(r, c++, activeReport.getColGrouping().getItem().getFormatedCaption(),
          STYLE_COLGROUP_HEADER);

      for (ReportValue colGroup : colGroups) {
        table.getCellFormatter().setColSpan(r, c, colItems.size());
        table.getCellFormatter().addStyleName(r, c, STYLE_COLGROUP);
        String text = colGroup.toString();

        if (!BeeUtils.isEmpty(text)) {
          if (!BeeUtils.isEmpty(activeReport.getColGrouping().getRelation())) {
            // DRILL DOWN
            Label label = new Label(text);
            label.addClickHandler(new DrillHandler(activeCopy,
                activeReport.getColGrouping().getRelation(), null, null, colGroup));
            table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
            table.setWidget(r, c, label);
          } else {
            table.setText(r, c, text);
          }
        }
        c++;
      }
      r++;
    }
    c = 0;

    for (ReportInfoItem infoItem : rowItems) {
      table.setText(r, c++, infoItem.getItem().getFormatedCaption(), STYLE_ROW_HEADER);
    }
    c = Math.max(c, 1);

    for (int i = 0; i < colGroups.size(); i++) {
      for (ReportInfoItem item : colItems) {
        table.setText(r, c++, item.getItem().getFormatedCaption(), STYLE_COL_HEADER);
      }
    }
    if (activeReport.getColGrouping() != null) {
      int x = c;

      for (ReportInfoItem infoItem : colItems) {
        if (infoItem.isRowSummary()) {
          table.setText(r, x++, infoItem.getItem().getFormatedCaption(), STYLE_ROW_SUMMARY_HEADER);
        }
      }
      if (x > c) {
        table.getCellFormatter().setColSpan(0, colGroups.size() + 1, x - c);
        table.setText(0, colGroups.size() + 1, Localized.getConstants().total(),
            STYLE_COLGROUP_SUMMARY_HEADER);
      }
    }
    r++;
    // DETAILS
    for (ReportValue rowGroup : rowGroups) {
      c = 0;

      if (activeReport.getRowGrouping() != null && !BeeUtils.isEmpty(rowItems)) {
        table.getCellFormatter().setColSpan(r, c, rowItems.size());
        table.getCellFormatter().addStyleName(r, c, STYLE_ROWGROUP);
        String text = activeReport.getRowGrouping().getItem().getFormatedCaption() + ": "
            + rowGroup;

        if (BeeUtils.allNotEmpty(rowGroup.toString(),
            activeReport.getRowGrouping().getRelation())) {
          // DRILL DOWN
          Label label = new Label(text);
          label.addClickHandler(new DrillHandler(activeCopy,
              activeReport.getRowGrouping().getRelation(), rowGroup, null, null));
          table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
          table.setWidget(r, c, label);
        } else {
          table.setText(r, c, text);
        }
        c++;

        for (ReportValue colGroup : colGroups) {
          for (ReportInfoItem infoItem : colItems) {
            table.getCellFormatter().addStyleName(r, c, STYLE_ROWGROUP_COL_SUMMARY);
            table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());

            if (infoItem.isGroupSummary()) {
              Object value = result.getGroupValue(rowGroup, colGroup, infoItem.getItem());
              text = value != null ? value.toString() : null;

              if (!BeeUtils.isEmpty(infoItem.getRelation())) {
                // DRILL DOWN
                Label label = new Label(BeeUtils.notEmpty(text, BeeConst.HTML_NBSP));
                label.addClickHandler(new DrillHandler(activeCopy, infoItem.getRelation(),
                    rowGroup, null, colGroup));
                table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
                table.setWidget(r, c, label);

              } else if (!BeeUtils.isEmpty(text)) {
                table.setText(r, c, text);
              }
            }
            c++;
          }
        }
        if (activeReport.getColGrouping() != null) {
          for (ReportInfoItem infoItem : colItems) {
            if (infoItem.isRowSummary()) {
              table.getCellFormatter().addStyleName(r, c, STYLE_ROWGROUP_SUMMARY);
              table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());

              if (infoItem.isGroupSummary()) {
                Object value = result.getGroupTotal(rowGroup, infoItem.getItem());
                text = value != null ? value.toString() : null;

                if (!BeeUtils.isEmpty(infoItem.getRelation())) {
                  // DRILL DOWN
                  Label label = new Label(BeeUtils.notEmpty(text, BeeConst.HTML_NBSP));
                  label.addClickHandler(new DrillHandler(activeCopy, infoItem.getRelation(),
                      rowGroup, null, null));
                  table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
                  table.setWidget(r, c, label);

                } else if (!BeeUtils.isEmpty(text)) {
                  table.setText(r, c, text);
                }
              }
              c++;
            }
          }
        }
        r++;
      }
      for (ReportValue[] row : result.getRows(rowGroup, sortedItem)) {
        c = 0;

        for (ReportValue detail : row) {
          ReportInfoItem infoItem = rowItems.get(c);
          table.getCellFormatter().addStyleName(r, c, STYLE_ROW);
          table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());
          String text = detail.toString();
          // DRILL DOWN
          if (!BeeUtils.isEmpty(text)) {
            if (!BeeUtils.isEmpty(infoItem.getRelation())
                || activeReport.getRowGrouping() != null) {

              ReportValue[] rowValues = new ReportValue[rowItems.size()];
              rowValues[c] = detail;
              Label label = new Label(text);
              label.addClickHandler(new DrillHandler(activeCopy, infoItem.getRelation(),
                  rowGroup, rowValues, null));
              table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
              table.setWidget(r, c, label);
            } else {
              table.setText(r, c, text);
            }
          }
          c++;
        }
        c = Math.max(c, 1);

        for (ReportValue colGroup : colGroups) {
          for (ReportInfoItem infoItem : colItems) {
            table.getCellFormatter().addStyleName(r, c, STYLE_COL);
            table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());

            Object value = result.getCellValue(rowGroup, row, colGroup, infoItem.getItem());
            String text = value != null ? value.toString() : null;

            if (!BeeUtils.isEmpty(infoItem.getRelation())) {
              // DRILL DOWN
              Label label = new Label(BeeUtils.notEmpty(text, BeeConst.HTML_NBSP));
              label.addClickHandler(new DrillHandler(activeCopy, infoItem.getRelation(),
                  rowGroup, row, colGroup));
              table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
              table.setWidget(r, c, label);

            } else if (!BeeUtils.isEmpty(text)) {
              table.setText(r, c, text);
            }
            c++;
          }
        }
        if (activeReport.getColGrouping() != null) {
          for (ReportInfoItem infoItem : colItems) {
            if (infoItem.isRowSummary()) {
              table.getCellFormatter().addStyleName(r, c, STYLE_ROW_SUMMARY);
              table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());

              Object value = result.getRowTotal(rowGroup, row, infoItem.getItem());
              String text = value != null ? value.toString() : null;

              if (!BeeUtils.isEmpty(infoItem.getRelation())) {
                // DRILL DOWN
                Label label = new Label(BeeUtils.notEmpty(text, BeeConst.HTML_NBSP));
                label.addClickHandler(new DrillHandler(activeCopy, infoItem.getRelation(),
                    rowGroup, row, null));
                table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
                table.setWidget(r, c, label);

              } else if (!BeeUtils.isEmpty(text)) {
                table.setText(r, c, text);
              }
              c++;
            }
          }
        }
        r++;
      }
    }
    c = 0;

    for (ReportInfoItem infoItem : colItems) {
      if (infoItem.isColSummary()) {
        if (!BeeUtils.isEmpty(rowItems)) {
          table.getCellFormatter().setColSpan(r, c, rowItems.size());
        }
        table.setText(r, c++, Localized.getConstants().totalOf(), STYLE_SUMMARY_HEADER);
        break;
      }
    }
    if (c > 0) {
      for (ReportValue colGroup : colGroups) {
        for (ReportInfoItem infoItem : colItems) {
          table.getCellFormatter().addStyleName(r, c, STYLE_COL_SUMMARY);
          table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());

          if (infoItem.isColSummary()) {
            Object value = result.getColTotal(colGroup, infoItem.getItem());
            String text = value != null ? value.toString() : null;

            if (!BeeUtils.isEmpty(infoItem.getRelation())) {
              // DRILL DOWN
              Label label = new Label(BeeUtils.notEmpty(text, BeeConst.HTML_NBSP));
              label.addClickHandler(new DrillHandler(activeCopy, infoItem.getRelation(),
                  null, null, colGroup));
              table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
              table.setWidget(r, c, label);

            } else if (!BeeUtils.isEmpty(text)) {
              table.setText(r, c, text);
            }
          }
          c++;
        }
      }
      if (activeReport.getColGrouping() != null) {
        for (ReportInfoItem infoItem : colItems) {
          if (infoItem.isRowSummary()) {
            table.getCellFormatter().addStyleName(r, c, STYLE_SUMMARY);
            table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());

            if (infoItem.isColSummary()) {
              Object value = result.getTotal(infoItem.getItem());
              table.setText(r, c, value != null ? value.toString() : null);
            }
            c++;
          }
        }
      }
    }
    getDataContainer().add(table);
  }

  private void renderLayout() {
    HasWidgets container = (HasWidgets) getFormView().getWidgetByName(NAME_LAYOUT_CONTAINER);

    if (container == null) {
      return;
    }
    container.clear();

    if (activeReport != null) {
      final Runnable refresh = new Runnable() {
        @Override
        public void run() {
          renderLayout();
        }
      };
      HtmlTable table = new HtmlTable(STYLE_PREFIX);

      // ROWS
      int rCnt = 0;
      int rIdx = 1;

      for (final ReportInfoItem infoItem : activeReport.getRowItems()) {
        table.setWidget(rIdx - 1, rCnt, buildCaption(infoItem, refresh), STYLE_ROW_HEADER);
        table.setWidget(rIdx, rCnt, ReportItem.renderDnd(infoItem.getItem(),
            activeReport.getRowItems(), rCnt, getReport(), refresh), STYLE_ROW);
        table.getCellFormatter().addStyleName(rIdx, rCnt, infoItem.getStyle());
        rCnt++;
      }
      Widget rowAdd = new CustomSpan(STYLE_ADD);
      ((CustomSpan) rowAdd).addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          ReportItem.chooseItem(getReport(), false, new Consumer<ReportItem>() {
            @Override
            public void accept(ReportItem item) {
              activeReport.addRowItem(item);
              refresh.run();
            }
          });
        }
      });
      if (rCnt > 0) {
        Horizontal cont = new Horizontal();
        cont.add(table.getWidget(rIdx, rCnt - 1));
        cont.add(rowAdd);
        rowAdd = cont;
      } else {
        table.setText(rIdx - 1, 0, Localized.getConstants().rows(), STYLE_ROW_HEADER);
        rCnt++;
      }
      table.setWidget(rIdx, rCnt - 1, rowAdd, STYLE_ROW);

      // COLUMNS
      int cCnt = 0;

      for (final ReportInfoItem infoItem : activeReport.getColItems()) {
        final ReportItem item = infoItem.getItem();
        final int idx = cCnt;

        Flow caption = new Flow();
        Flow flow = new Flow();

        InlineLabel agg = new InlineLabel(infoItem.getFunction().getCaption());
        agg.addStyleName(STYLE_FUNCTION);
        agg.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            List<String> options = new ArrayList<>();
            final List<ReportFunction> values = new ArrayList<>();

            for (ReportFunction fnc : item.getAvailableFunctions()) {
              options.add(fnc.getCaption());
              values.add(fnc);
            }
            Global.choice(null, null, options, new ChoiceCallback() {
              @Override
              public void onSuccess(int value) {
                activeReport.setFunction(idx, values.get(value));
                refresh.run();
              }
            });
          }
        });
        flow.add(agg);

        if (infoItem.getFunction() == ReportFunction.LIST) {
          activeReport.setDescending(idx, null);
        } else {
          CustomSpan sort = new CustomSpan(infoItem.isSorted()
              ? (infoItem.getDescending() ? STYLE_SORT_DESC : STYLE_SORT_ASC)
              : STYLE_SORT);
          sort.setTitle(Localized.getConstants().sort());

          sort.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              Boolean descending;

              if (!infoItem.isSorted()) {
                descending = false;
              } else if (infoItem.getDescending()) {
                descending = null;
              } else {
                descending = true;
              }
              for (int i = 0; i < activeReport.getColItems().size(); i++) {
                activeReport.setDescending(i, null);
              }
              activeReport.setDescending(idx, descending);
              refresh.run();
            }
          });
          flow.add(sort);
        }
        caption.add(flow);
        caption.add(buildCaption(infoItem, refresh));
        table.setWidget(0, rCnt + cCnt, caption, STYLE_COL_HEADER);
        table.setWidget(rIdx, rCnt + cCnt, ReportItem.renderDnd(item, activeReport.getColItems(),
            cCnt, getReport(), refresh), STYLE_COL);
        table.getCellFormatter().addStyleName(rIdx, rCnt + cCnt, infoItem.getStyle());

        CustomSpan colResult = new CustomSpan(infoItem.isColSummary()
            ? STYLE_SUMMARY_ON : STYLE_SUMMARY_OFF);
        colResult.setTitle(Localized.getConstants().columnResults());

        colResult.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            activeReport.setColSummary(idx, !infoItem.isColSummary());
            refresh.run();
          }
        });
        table.setWidget(rIdx + 1, 1 + cCnt, colResult, STYLE_COL_SUMMARY);
        table.getCellFormatter().addStyleName(rIdx + 1, 1 + cCnt, infoItem.getStyle());
        cCnt++;
      }
      Widget colAdd = new CustomSpan(STYLE_ADD);
      ((CustomSpan) colAdd).addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          ReportItem.chooseItem(getReport(), null, new Consumer<ReportItem>() {
            @Override
            public void accept(ReportItem item) {
              activeReport.addColItem(item);
              refresh.run();
            }
          });
        }
      });
      if (cCnt > 0) {
        table.getCellFormatter().setColSpan(rIdx + 1, 0, rCnt);
        table.setText(rIdx + 1, 0, Localized.getConstants().totalOf(), STYLE_SUMMARY_HEADER);

        // COL GROUPING
        table.insertRow(0);
        rIdx++;
        table.getCellFormatter().setColSpan(0, 0, rCnt);
        table.getCellFormatter().setColSpan(0, 1, cCnt);
        Widget cGroup;

        if (activeReport.getColGrouping() != null) {
          table.setWidget(0, 0, buildCaption(activeReport.getColGrouping(), refresh),
              STYLE_COLGROUP_HEADER);
          cGroup = activeReport.getColGrouping().getItem().render(getReport(), new Runnable() {
            @Override
            public void run() {
              activeReport.setColGrouping(null);
              refresh.run();
            }
          }, refresh).asWidget();
          table.getCellFormatter().setColSpan(0, 2, cCnt);
          table.setText(0, 2, Localized.getConstants().total(), STYLE_COLGROUP_SUMMARY_HEADER);
          int idx = rCnt + cCnt;

          for (final ReportInfoItem infoItem : activeReport.getColItems()) {
            table.setText(1, idx, infoItem.getItem().getFormatedCaption(),
                STYLE_ROW_SUMMARY_HEADER);
            final int i = idx - (rCnt + cCnt);
            CustomSpan rowResult = new CustomSpan(infoItem.isRowSummary()
                ? STYLE_SUMMARY_ON : STYLE_SUMMARY_OFF);
            rowResult.setTitle(Localized.getConstants().rowResults());

            rowResult.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                activeReport.setRowSummary(i, !infoItem.isRowSummary());
                refresh.run();
              }
            });
            table.setWidget(rIdx, idx, rowResult, STYLE_ROW_SUMMARY);
            table.getCellFormatter().addStyleName(rIdx, idx, infoItem.getStyle());
            idx++;
          }
          table.getCellFormatter().setColSpan(rIdx + 1, 1 + cCnt, cCnt);
          table.getCellFormatter().addStyleName(rIdx + 1, 1 + cCnt, STYLE_SUMMARY);
        } else {
          table.setText(0, 0, Localized.getConstants().groupBy(), STYLE_COLGROUP_HEADER);
          cGroup = new CustomSpan(STYLE_ADD);
          ((CustomSpan) cGroup).addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              ReportItem.chooseItem(getReport(), false, new Consumer<ReportItem>() {
                @Override
                public void accept(ReportItem item) {
                  activeReport.setColGrouping(item);
                  refresh.run();
                }
              });
            }
          });
        }
        table.setWidget(0, 1, cGroup, STYLE_COLGROUP);

        Horizontal cont = new Horizontal();
        cont.add(table.getWidget(rIdx, rCnt + cCnt - 1));
        cont.add(colAdd);
        colAdd = cont;
      } else {
        table.setText(0, rCnt, Localized.getConstants().columns(), STYLE_COL_HEADER);
        cCnt++;
      }
      table.setWidget(rIdx, rCnt + cCnt - 1, colAdd, STYLE_COL);

      // ROW GROUPING
      if (activeReport.getRowItems().size() > 0) {
        table.insertRow(rIdx);
        int gIdx = rIdx++;
        Horizontal rGroup = new Horizontal();

        if (activeReport.getRowGrouping() != null) {
          rGroup.add(buildCaption(activeReport.getRowGrouping(), refresh));
          rGroup.add(activeReport.getRowGrouping().getItem().render(getReport(), new Runnable() {
            @Override
            public void run() {
              activeReport.setRowGrouping(null);
              refresh.run();
            }
          }, refresh));
        } else {
          rGroup.add(new Label(Localized.getConstants().groupBy()));
          CustomSpan rGroupAdd = new CustomSpan(STYLE_ADD);
          rGroupAdd.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              ReportItem.chooseItem(getReport(), false, new Consumer<ReportItem>() {
                @Override
                public void accept(ReportItem item) {
                  activeReport.setRowGrouping(item);
                  refresh.run();
                }
              });
            }
          });
          rGroup.add(rGroupAdd);
        }
        table.setWidget(gIdx, 0, rGroup, STYLE_ROWGROUP);

        if (activeReport.getColItems().size() > 0) {
          if (activeReport.getRowGrouping() != null) {
            table.getCellFormatter().setColSpan(gIdx, 0, rCnt);
            int idx = 1;

            for (final ReportInfoItem infoItem : activeReport.getColItems()) {
              final int i = idx - 1;
              CustomSpan groupResult = new CustomSpan(infoItem.isGroupSummary()
                  ? STYLE_SUMMARY_ON : STYLE_SUMMARY_OFF);
              groupResult.setTitle(Localized.getConstants().groupResults());

              groupResult.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                  activeReport.setGroupSummary(i, !infoItem.isGroupSummary());
                  refresh.run();
                }
              });
              table.setWidget(gIdx, idx, groupResult, STYLE_ROWGROUP_COL_SUMMARY);
              table.getCellFormatter().addStyleName(gIdx, idx, infoItem.getStyle());
              idx++;
            }
            if (activeReport.getColGrouping() != null) {
              table.getCellFormatter().setColSpan(gIdx, 1 + cCnt, cCnt);
              table.getCellFormatter().addStyleName(gIdx, 1 + cCnt, STYLE_ROWGROUP_SUMMARY);
            }
          } else {
            table.getCellFormatter().setColSpan(gIdx, 0,
                rCnt + cCnt + (activeReport.getColGrouping() != null ? cCnt : 0));
          }
        } else {
          table.getCellFormatter().setColSpan(gIdx, 0, rCnt + cCnt);
        }
      }
      container.add(table);
    }
  }

  private void renderFilters() {
    HasWidgets container = (HasWidgets) getFormView().getWidgetByName(NAME_FILTER_CONTAINER);

    if (container == null) {
      return;
    }
    container.clear();

    if (activeReport != null) {
      final List<ReportItem> filterItems = activeReport.getFilterItems();
      HtmlTable table = new HtmlTable();
      table.setColumnCellClasses(0, STYLE_FILTER_CAPTION);
      table.setColumnCellStyles(1, "width:100%;");
      int c = 0;

      for (ReportItem item : filterItems) {
        final int idx = c;
        table.setText(idx, 0, item.getCaption());
        table.setWidget(idx, 1, item.getFilterWidget());

        CustomDiv remove = new CustomDiv(STYLE_REMOVE);
        remove.setText(String.valueOf(BeeConst.CHAR_TIMES));
        remove.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            filterItems.remove(idx);
            renderFilters();
          }
        });
        table.setWidget(idx, 2, remove);
        c++;
      }
      container.add(table);
    }
  }

  private void saveReport(final ReportInfo rep) {
    if (DataUtils.isId(rep.getId())) {
      Queries.update(VIEW_REPORT_SETTINGS, rep.getId(), COL_RS_PARAMETERS,
          TextValue.of(rep.serialize()));

      activateReport(rep);
    } else {
      Queries.insert(VIEW_REPORT_SETTINGS, Data.getColumns(VIEW_REPORT_SETTINGS,
          Arrays.asList(COL_RS_USER, COL_RS_REPORT, COL_RS_PARAMETERS)),
          Arrays.asList(BeeUtils.toString(BeeKeeper.getUser().getUserId()),
              getReport().getReportName(), rep.serialize()), null,
          new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              if (reports.contains(rep)) {
                reports.remove(rep);
              }
              rep.setId(result.getId());
              reports.add(rep);
              activateReport(rep);
            }
          });
    }
  }
}
