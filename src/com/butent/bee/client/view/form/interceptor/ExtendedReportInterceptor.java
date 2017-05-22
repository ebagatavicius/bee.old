package com.butent.bee.client.view.form.interceptor;

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
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Vertical;
import com.butent.bee.client.output.Exporter;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.output.ReportItem;
import com.butent.bee.client.output.ReportNumericItem;
import com.butent.bee.client.output.ReportResultItem;
import com.butent.bee.client.output.ReportTimeDurationItem;
import com.butent.bee.client.output.ReportUtils;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.CustomSpan;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
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
import com.butent.bee.shared.report.ReportFunction;
import com.butent.bee.shared.report.ReportInfo;
import com.butent.bee.shared.report.ReportInfoItem;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.shared.report.ResultHolder;
import com.butent.bee.shared.report.ResultValue;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ExtendedReportInterceptor extends ReportInterceptor {

  private static final class DrillHandler implements ClickHandler {
    private ReportInfo current;
    private Supplier<ReportInfo> repSupplier;
    private Consumer<ReportInfo> repOpener;
    private ResultValue rowGroup;
    private ResultValue[] rowValues;
    private ResultValue colGroup;

    private DrillHandler(ReportInfo current, Supplier<ReportInfo> repSupplier,
        Consumer<ReportInfo> repOpener, ResultValue rowGroup, ResultValue[] rowValues,
        ResultValue colGroup) {

      this.current = current;
      this.repSupplier = repSupplier;
      this.repOpener = repOpener;
      this.rowGroup = rowGroup;
      this.rowValues = rowValues;
      this.colGroup = colGroup;
    }

    @Override
    public void onClick(ClickEvent event) {
      ReportInfo rep = repSupplier.get();

      if (Objects.isNull(rep)) {
        return;
      }
      List<ReportItem> filters = rep.getFilterItems();
      List<ReportItem> reportFilters = new ArrayList<>(filters);
      filters.clear();

      if (current.getColGrouping() != null) {
        setFilter(filters, colGroup, current.getColGrouping().getItem());
      }
      if (!Objects.equals(rep, current) && current.getRowGrouping() != null) {
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
      repOpener.accept(rep);
    }

    private static List<String> getValues(ResultValue value) {
      List<String> values = new ArrayList<>();

      if (value.getValues() != null) {
        for (ResultValue val : value.getValues()) {
          values.addAll(getValues(val));
        }
      } else {
        values.add(value.getValue());
      }
      return values;
    }

    private static void setFilter(List<ReportItem> filters, ResultValue value, ReportItem item) {
      if (value == null) {
        return;
      }
      List<String> values = getValues(value);
      List<ReportItem> members = item.getMembers();

      for (int i = 0; i < values.size(); i++) {
        ReportItem itm = members.get(i);

        if (itm.getFilterWidget() != null && !filters.contains(itm)) {
          filters.add(itm.setFilter(values.get(i)));
        }
      }
    }
  }

  private static final String NAME_REPORT_CONTAINER = "ReportContainer";
  private static final String NAME_LAYOUT_CONTAINER = "LayoutContainer";
  private static final String NAME_PARAMS_CONTAINER = "ParamsContainer";
  private static final String NAME_FILTER_CONTAINER = "FilterContainer";

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
  private static final String STYLE_REPORT_GLOBAL = STYLE_PREFIX + "-report-global";

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
          ((HasClickHandlers) widget).addClickHandler(event -> addFilterItem());
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
          InputText cap = new InputText();
          cap.setText(getBookmarkLabel());
          InputBoolean global = new InputBoolean(Localized.dictionary().mailPublic());
          global.setChecked(activeReport.isGlobal());

          Vertical flow = new Vertical();
          flow.add(cap);
          flow.add(global);

          Global.inputWidget(Localized.dictionary().name(), flow, new InputCallback() {
            @Override
            public String getErrorMessage() {
              if (cap.isEmpty()) {
                cap.setFocus(true);
                return Localized.dictionary().valueRequired();
              }
              return null;
            }

            @Override
            public void onSuccess() {
              ReportInfo rep = new ReportInfo(cap.getValue());
              rep.deserialize(activeReport.serialize());
              rep.setGlobal(global.isChecked());

              for (ReportInfo reportInfo : reports) {
                if (Objects.equals(rep, reportInfo)) {
                  rep.setId(reportInfo.getId());
                  break;
                }
              }
              if (reports.contains(rep)) {
                Global.confirm(Localized.dictionary().reports(), Icon.QUESTION,
                    Arrays.asList(Localized.dictionary().valueExists(cap.getValue()),
                        Localized.dictionary().actionChange()), () -> saveReport(rep));
              } else {
                saveReport(rep);
              }
            }
          });
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
    renderParams();
    ReportParameters parameters = readParameters();
    ReportInfo rep = null;

    if (parameters != null) {
      String data = parameters.getText(COL_RS_REPORT);

      if (!BeeUtils.isEmpty(data)) {
        rep = ReportInfo.restore(data);
      }
      getParams().forEach((name, editor) -> {
        if (editor instanceof UnboundSelector) {
          ((UnboundSelector) editor).setValue(parameters.getLong(name), false);
        } else {
          editor.setValue(parameters.getText(name));
        }
      });
    }
    getReports(rep);
    super.onLoad(form);
  }

  @Override
  public void onUnload(FormView form) {
    getParams().forEach((name, editor) -> storeValue(name, editor.getValue()));
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

    getParams().forEach((name, editor) -> params.addNotEmptyData(name, editor.getValue()));
    params.addDataItem(Service.VAR_DATA, Codec.beeSerialize(activeReport));
    params.addDataItem(VAR_LOCALE, Localized.dictionary().languageTag());

    ReportInfo reportInfo = ReportInfo.restore(params.getParameter(Service.VAR_DATA));
    Holder<Integer> rpcId = Holder.absent();

    rpcId.set(BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (!rpcId.contains(getRpcId())) {
          return;
        }
        response.notify(getFormView());

        if (!response.hasErrors() && response.hasResponse()) {
          ResultHolder result;
          String content = response.getResponseAsString();

          if (response.hasResponse(ResultHolder.class)) {
            result = BeeSerializable.restore(content, ResultHolder::new);

          } else if (response.hasResponse(SimpleRowSet.class)) {
            result = reportInfo.getResult(SimpleRowSet.restore(content), Localized.dictionary());

          } else {
            result = new ResultHolder();
          }
          renderData(reportInfo, result);
        }
      }
    }));
  }

  @Override
  protected String getBookmarkLabel() {
    if (activeReport == null) {
      return null;
    }
    return activeReport.getCaption();
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
    getParams().forEach((name, editor) -> parameters.add(name, editor.getValue()));
    return parameters;
  }

  @Override
  protected void export() {
    if (getDataContainer() != null && !getDataContainer().isEmpty()) {
      XSheet sheet = getSheet((HtmlTable) getDataContainer().getWidget(0));

      if (!sheet.isEmpty()) {
        Exporter.maybeExport(sheet, getBookmarkLabel());
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

      for (ReportInfo rep : reports) {
        if (Objects.equals(rep, activeReport)) {
          rep.setGlobal(activeReport.isGlobal());
          ft.getRowFormatter().addStyleName(r, STYLE_REPORT_ACTIVE);
        } else {
          ft.getRowFormatter().addStyleName(r, STYLE_REPORT_NORMAL);
        }
        if (rep.isGlobal()) {
          ft.getRowFormatter().addStyleName(r, STYLE_REPORT_GLOBAL);
        }
        Label name = new Label(rep.getCaption());
        name.addClickHandler(event -> {
          if (!Objects.equals(rep, activeReport)) {
            activateReport(rep);
          }
        });
        ft.setWidget(r, 0, name);
        CustomDiv remove = new CustomDiv(STYLE_REMOVE);

        if (DataUtils.isId(rep.getId())) {
          remove.setText(String.valueOf(BeeConst.CHAR_TIMES));
          remove.addClickHandler(
              event -> Global.confirmRemove(Localized.dictionary().reports(), rep.getCaption(),
                  () -> {
                    Queries.deleteRow(VIEW_REPORT_SETTINGS, rep.getId());
                    reports.remove(rep);
                    activateReport(Objects.equals(rep, activeReport)
                        ? BeeUtils.peek(reports) : activeReport);
                  }));
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
      Global.choice(null, null, options, value -> {
        activeReport.getFilterItems().add(items.get(value));
        renderFilters();
      });
    }
  }

  private Widget buildCaption(final ReportInfoItem infoItem, final Runnable onUpdate) {
    final boolean on = !BeeUtils.isEmpty(infoItem.getRelation());
    final InlineLabel cap = new InlineLabel(infoItem.getItem().getFormatedCaption());
    cap.addStyleName(on ? STYLE_DRILLDOWN_ON : STYLE_DRILLDOWN_OFF);
    cap.setTitle(BeeUtils.join(": ", Localized.dictionary().relation(), infoItem.getRelation()));

    cap.addClickHandler(event -> {
      final List<String> options = new ArrayList<>();

      for (ReportInfo rep : reports) {
        if (!on || !Objects.equals(rep.getCaption(), infoItem.getRelation())) {
          options.add(rep.getCaption());
        }
      }
      if (on) {
        options.add(Localized.dictionary().clear() + "...");
      }
      Global.choice(cap.getTitle(), null, options, value -> {
        infoItem.setRelation(on && (value == options.size() - 1) ? null : options.get(value));
        onUpdate.run();
      });
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

  private ClickHandler drill(ReportInfo current, String target, ResultValue rowGroup,
      ResultValue[] rowValues, ResultValue colGroup) {

    Supplier<ReportInfo> repSupplier = () -> {
      ReportInfo rep = null;

      if (!BeeUtils.isEmpty(target)) {
        for (ReportInfo reportInfo : reports) {
          if (Objects.equals(reportInfo.getCaption(), target)) {
            rep = ReportInfo.restore(reportInfo.serialize());
            break;
          }
        }
        if (Objects.isNull(rep)) {
          getFormView().notifyWarning(Localized.dictionary().keyNotFound(target));
        } else {
          rep.getFilterItems().clear();
          rep.getFilterItems().addAll(current.getFilterItems());
        }
      } else {
        rep = ReportInfo.restore(current.serialize());
      }
      return rep;
    };
    Consumer<ReportInfo> repOpener = rep -> {
      ReportParameters parameters = new ReportParameters(Collections.singletonMap(COL_RS_REPORT,
          rep.serialize()));

      if (UiHelper.isModal(getFormView().asWidget())) {
        getReport().showModal(parameters);
      } else {
        getReport().open(parameters);
      }
    };
    return new DrillHandler(current, repSupplier, repOpener, rowGroup, rowValues, colGroup);
  }

  private Map<String, Editor> getParams() {
    Map<String, Editor> params = new HashMap<>();
    HasWidgets container = (HasWidgets) getFormView().getWidgetByName(NAME_PARAMS_CONTAINER);

    if (container != null) {
      container.forEach(widget ->
          params.put(DomUtils.getDataKey(widget.getElement()), (Editor) widget));
    }
    return params;
  }

  private void getReports(ReportInfo initialReport) {
    reports.clear();

    ReportUtils.getReports(getReport(), reps -> {
      reports.addAll(reps);

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
            if (BeeUtils.same(style, ReportItem.STYLE_NUM) && BeeUtils.isDouble(text)) {
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

  private void renderData(ReportInfo reportInfo, ResultHolder result) {
    getDataContainer().clear();

    if (result.isEmpty()) {
      getFormView().notifyWarning(Localized.dictionary().nothingFound());
      return;
    }
    List<ReportInfoItem> rowItems = reportInfo.getRowItems();
    List<ReportInfoItem> colItems = reportInfo.getColItems();

    ReportInfoItem sortedItem = null;

    for (ReportInfoItem colItem : colItems) {
      if (colItem.isSorted()) {
        sortedItem = colItem;
        break;
      }
    }
    Collection<ResultValue> rowGroups = result.getRowGroups(sortedItem);
    Collection<ResultValue> colGroups = result.getColGroups();

    if (BeeUtils.isEmpty(rowGroups)) {
      getFormView().notifyWarning(Localized.dictionary().nothingFound());
      return;
    }
    HtmlTable table = new HtmlTable(STYLE_PREFIX);
    // HEADER
    int r = 0;
    int c = 0;

    if (reportInfo.getColGrouping() != null) {
      if (!BeeUtils.isEmpty(rowItems)) {
        table.getCellFormatter().setColSpan(r, c, rowItems.size());
      }
      table.setText(r, c++, reportInfo.getColGrouping().getItem().getFormatedCaption(),
          STYLE_COLGROUP_HEADER);

      for (ResultValue colGroup : colGroups) {
        table.getCellFormatter().setColSpan(r, c, colItems.size());
        table.getCellFormatter().addStyleName(r, c, STYLE_COLGROUP);
        String text = colGroup.toString();
        // DRILL DOWN
        if (!BeeUtils.isEmpty(reportInfo.getColGrouping().getRelation())) {
          Label label = new Label(BeeUtils.notEmpty(text, BeeConst.HTML_NBSP));
          label.addClickHandler(drill(reportInfo, reportInfo.getColGrouping().getRelation(), null,
              null, colGroup));
          table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
          table.setWidget(r, c, label);
        } else {
          table.setText(r, c, text);
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
    if (reportInfo.getColGrouping() != null) {
      int x = c;

      for (ReportInfoItem infoItem : colItems) {
        if (infoItem.isRowSummary()) {
          table.setText(r, x++, infoItem.getItem().getFormatedCaption(), STYLE_ROW_SUMMARY_HEADER);
        }
      }
      if (x > c) {
        table.getCellFormatter().setColSpan(0, colGroups.size() + 1, x - c);
        table.setText(0, colGroups.size() + 1, Localized.dictionary().total(),
            STYLE_COLGROUP_SUMMARY_HEADER);
      }
    }
    r++;
    // DETAILS
    for (ResultValue rowGroup : rowGroups) {
      c = 0;

      if (reportInfo.getRowGrouping() != null && !BeeUtils.isEmpty(rowItems)) {
        table.getCellFormatter().setColSpan(r, c, rowItems.size());
        table.getCellFormatter().addStyleName(r, c, STYLE_ROWGROUP);
        String text = reportInfo.getRowGrouping().getItem().getFormatedCaption() + ": " + rowGroup;
        // DRILL DOWN
        if (BeeUtils.allNotEmpty(rowGroup.toString(), reportInfo.getRowGrouping().getRelation())) {
          Label label = new Label(text);
          label.addClickHandler(drill(reportInfo, reportInfo.getRowGrouping().getRelation(),
              rowGroup, null, null));
          table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
          table.setWidget(r, c, label);
        } else {
          table.setText(r, c, text);
        }
        c++;

        for (ResultValue colGroup : colGroups) {
          for (ReportInfoItem infoItem : colItems) {
            table.getCellFormatter().addStyleName(r, c, STYLE_ROWGROUP_COL_SUMMARY);
            table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());

            if (infoItem.isGroupSummary()) {
              Object value = result.getGroupValue(rowGroup, colGroup, infoItem.getItem().getName());
              text = value != null ? value.toString() : null;
              // DRILL DOWN
              if (!BeeUtils.isEmpty(infoItem.getRelation())) {
                Label label = new Label(BeeUtils.notEmpty(text, BeeConst.HTML_NBSP));
                label.addClickHandler(drill(reportInfo, infoItem.getRelation(), rowGroup, null,
                    colGroup));
                table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
                table.setWidget(r, c, label);

              } else if (!BeeUtils.isEmpty(text)) {
                table.setText(r, c, text);
              }
            }
            c++;
          }
        }
        if (reportInfo.getColGrouping() != null) {
          for (ReportInfoItem infoItem : colItems) {
            if (infoItem.isRowSummary()) {
              table.getCellFormatter().addStyleName(r, c, STYLE_ROWGROUP_SUMMARY);
              table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());

              if (infoItem.isGroupSummary()) {
                Object value = result.getGroupTotal(rowGroup, infoItem.getItem().getName());
                text = value != null ? value.toString() : null;
                // DRILL DOWN
                if (!BeeUtils.isEmpty(infoItem.getRelation())) {
                  Label label = new Label(BeeUtils.notEmpty(text, BeeConst.HTML_NBSP));
                  label.addClickHandler(drill(reportInfo, infoItem.getRelation(), rowGroup, null,
                      null));
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
      for (ResultValue[] row : result.getRows(rowGroup, sortedItem)) {
        c = 0;

        for (ResultValue detail : row) {
          ReportInfoItem infoItem = rowItems.get(c);
          table.getCellFormatter().addStyleName(r, c, STYLE_ROW);
          table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());
          String text = detail.toString();
          // DRILL DOWN
          if (!BeeUtils.isEmpty(infoItem.getRelation()) || reportInfo.getRowGrouping() != null) {
            ResultValue[] rowValues = new ResultValue[rowItems.size()];
            rowValues[c] = detail;
            Label label = new Label(BeeUtils.notEmpty(text, BeeConst.HTML_NBSP));
            label.addClickHandler(drill(reportInfo, infoItem.getRelation(), rowGroup, rowValues,
                null));
            table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
            table.setWidget(r, c, label);
          } else {
            table.setText(r, c, text);
          }
          c++;
        }
        c = Math.max(c, 1);

        for (ResultValue colGroup : colGroups) {
          for (ReportInfoItem infoItem : colItems) {
            table.getCellFormatter().addStyleName(r, c, STYLE_COL);
            table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());

            Object value = result.getCellValue(rowGroup, row, colGroup,
                infoItem.getItem().getName());
            String text = value != null ? value.toString() : null;
            // DRILL DOWN
            if (!BeeUtils.isEmpty(infoItem.getRelation())) {
              Label label = new Label(BeeUtils.notEmpty(text, BeeConst.HTML_NBSP));
              label.addClickHandler(drill(reportInfo, infoItem.getRelation(), rowGroup, row,
                  colGroup));
              table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
              table.setWidget(r, c, label);

            } else if (!BeeUtils.isEmpty(text)) {
              table.setText(r, c, text);
            }
            c++;
          }
        }
        if (reportInfo.getColGrouping() != null) {
          for (ReportInfoItem infoItem : colItems) {
            if (infoItem.isRowSummary()) {
              table.getCellFormatter().addStyleName(r, c, STYLE_ROW_SUMMARY);
              table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());

              Object value = result.getRowTotal(rowGroup, row, infoItem.getItem().getName());
              String text = value != null ? value.toString() : null;
              // DRILL DOWN
              if (!BeeUtils.isEmpty(infoItem.getRelation())) {
                Label label = new Label(BeeUtils.notEmpty(text, BeeConst.HTML_NBSP));
                label.addClickHandler(drill(reportInfo, infoItem.getRelation(), rowGroup, row,
                    null));
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
        table.setText(r, c++, Localized.dictionary().totalOf(), STYLE_SUMMARY_HEADER);
        break;
      }
    }
    if (c > 0) {
      for (ResultValue colGroup : colGroups) {
        for (ReportInfoItem infoItem : colItems) {
          table.getCellFormatter().addStyleName(r, c, STYLE_COL_SUMMARY);
          table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());

          if (infoItem.isColSummary()) {
            Object value = result.getColTotal(colGroup, infoItem.getItem().getName());
            String text = value != null ? value.toString() : null;
            // DRILL DOWN
            if (!BeeUtils.isEmpty(infoItem.getRelation())) {
              Label label = new Label(BeeUtils.notEmpty(text, BeeConst.HTML_NBSP));
              label.addClickHandler(drill(reportInfo, infoItem.getRelation(), null, null,
                  colGroup));
              table.getCellFormatter().addStyleName(r, c, STYLE_DRILLDOWN);
              table.setWidget(r, c, label);

            } else if (!BeeUtils.isEmpty(text)) {
              table.setText(r, c, text);
            }
          }
          c++;
        }
      }
      if (reportInfo.getColGrouping() != null) {
        for (ReportInfoItem infoItem : colItems) {
          if (infoItem.isRowSummary()) {
            table.getCellFormatter().addStyleName(r, c, STYLE_SUMMARY);
            table.getCellFormatter().addStyleName(r, c, infoItem.getStyle());

            if (infoItem.isColSummary()) {
              Object value = result.getTotal(infoItem.getItem().getName());
              table.setText(r, c, value != null ? value.toString() : null);
            }
            c++;
          }
        }
      }
    }
    getDataContainer().add(table);
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
        remove.addClickHandler(event -> {
          filterItems.remove(idx);
          renderFilters();
        });
        table.setWidget(idx, 2, remove);
        c++;
      }
      container.add(table);
    }
  }

  private void renderLayout() {
    HasWidgets container = (HasWidgets) getFormView().getWidgetByName(NAME_LAYOUT_CONTAINER);

    if (container == null) {
      return;
    }
    container.clear();

    if (activeReport != null) {
      Runnable refresh = this::renderLayout;
      HtmlTable table = new HtmlTable(STYLE_PREFIX);

      List<ReportItem> infoItems = getReport().getItems();

      // ROWS
      int rCnt = 0;
      int rIdx = 1;

      for (final ReportInfoItem infoItem : activeReport.getRowItems()) {
        table.setWidget(rIdx - 1, rCnt, buildCaption(infoItem, refresh), STYLE_ROW_HEADER);
        table.setWidget(rIdx, rCnt, ReportItem.renderDnd(infoItem.getItem(),
            activeReport.getRowItems(), rCnt, infoItems, refresh), STYLE_ROW);
        table.getCellFormatter().addStyleName(rIdx, rCnt, infoItem.getStyle());
        rCnt++;
      }
      Widget rowAdd = new CustomSpan(STYLE_ADD);
      ((CustomSpan) rowAdd)
          .addClickHandler(event -> ReportItem.chooseItem(infoItems, false, item -> {
            activeReport.addRowItem(item);
            refresh.run();
          }));
      if (rCnt > 0) {
        Horizontal cont = new Horizontal();
        cont.add(table.getWidget(rIdx, rCnt - 1));
        cont.add(rowAdd);
        rowAdd = cont;
      } else {
        table.setText(rIdx - 1, 0, Localized.dictionary().rows(), STYLE_ROW_HEADER);
        rCnt++;
      }
      table.setWidget(rIdx, rCnt - 1, rowAdd, STYLE_ROW);

      // COLUMNS
      int cCnt = 0;
      List<ReportItem> calcItems = new ArrayList<>(getReport().getItems());

      calcItems.addAll(activeReport.getColItems().stream()
          .filter(infoItem -> !Objects.equals(infoItem.getFunction(), ReportFunction.LIST))
          .map(ReportInfoItem::getItem)
          .filter(item -> !item.isResultItem())
          .filter(item -> item instanceof ReportNumericItem)
          .filter(item -> !(item instanceof ReportTimeDurationItem))
          .map(ReportResultItem::new)
          .collect(Collectors.toList()));

      for (ReportInfoItem infoItem : activeReport.getColItems()) {
        ReportItem item = infoItem.getItem();
        int idx = cCnt;

        Flow caption = new Flow();
        Flow flow = new Flow();

        InlineLabel agg = new InlineLabel(infoItem.getFunction().getCaption());
        agg.addStyleName(STYLE_FUNCTION);
        agg.addClickHandler(event -> {
          List<String> options = new ArrayList<>();
          List<ReportFunction> values = new ArrayList<>();

          for (ReportFunction fnc : item.getAvailableFunctions()) {
            options.add(fnc.getCaption());
            values.add(fnc);
          }
          Global.choice(null, null, options, value -> {
            activeReport.setFunction(idx, values.get(value));
            refresh.run();
          });
        });
        flow.add(agg);

        if (infoItem.getFunction() == ReportFunction.LIST) {
          activeReport.setDescending(idx, null);
        } else {
          CustomSpan sort = new CustomSpan(infoItem.isSorted()
              ? (infoItem.getDescending() ? STYLE_SORT_DESC : STYLE_SORT_ASC)
              : STYLE_SORT);
          sort.setTitle(Localized.dictionary().sort());

          sort.addClickHandler(event -> {
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
          });
          flow.add(sort);
        }
        caption.add(flow);
        caption.add(buildCaption(infoItem, refresh));
        table.setWidget(0, rCnt + cCnt, caption, STYLE_COL_HEADER);
        table.setWidget(rIdx, rCnt + cCnt, ReportItem.renderDnd(item, activeReport.getColItems(),
            cCnt, calcItems, refresh), STYLE_COL);
        table.getCellFormatter().addStyleName(rIdx, rCnt + cCnt, infoItem.getStyle());

        CustomSpan colResult = new CustomSpan(infoItem.isColSummary()
            ? STYLE_SUMMARY_ON : STYLE_SUMMARY_OFF);
        colResult.setTitle(Localized.dictionary().columnResults());

        colResult.addClickHandler(event -> {
          activeReport.setColSummary(idx, !infoItem.isColSummary());
          refresh.run();
        });
        table.setWidget(rIdx + 1, 1 + cCnt, colResult, STYLE_COL_SUMMARY);
        table.getCellFormatter().addStyleName(rIdx + 1, 1 + cCnt, infoItem.getStyle());
        cCnt++;
      }
      Widget colAdd = new CustomSpan(STYLE_ADD);
      ((CustomSpan) colAdd)
          .addClickHandler(event -> ReportItem.chooseItem(calcItems, null, item -> {
            activeReport.addColItem(item);
            refresh.run();
          }));
      if (cCnt > 0) {
        table.getCellFormatter().setColSpan(rIdx + 1, 0, rCnt);
        table.setText(rIdx + 1, 0, Localized.dictionary().totalOf(), STYLE_SUMMARY_HEADER);

        // COL GROUPING
        table.insertRow(0);
        rIdx++;
        table.getCellFormatter().setColSpan(0, 0, rCnt);
        table.getCellFormatter().setColSpan(0, 1, cCnt);
        Widget cGroup;

        if (activeReport.getColGrouping() != null) {
          table.setWidget(0, 0, buildCaption(activeReport.getColGrouping(), refresh),
              STYLE_COLGROUP_HEADER);
          cGroup = activeReport.getColGrouping().getItem().render(infoItems, () -> {
            activeReport.setColGrouping(null);
            refresh.run();
          }, refresh).asWidget();
          table.getCellFormatter().setColSpan(0, 2, cCnt);
          table.setText(0, 2, Localized.dictionary().total(), STYLE_COLGROUP_SUMMARY_HEADER);
          int idx = rCnt + cCnt;

          for (ReportInfoItem infoItem : activeReport.getColItems()) {
            table.setText(1, idx, infoItem.getItem().getFormatedCaption(),
                STYLE_ROW_SUMMARY_HEADER);
            int i = idx - (rCnt + cCnt);
            CustomSpan rowResult = new CustomSpan(infoItem.isRowSummary()
                ? STYLE_SUMMARY_ON : STYLE_SUMMARY_OFF);
            rowResult.setTitle(Localized.dictionary().rowResults());

            rowResult.addClickHandler(event -> {
              activeReport.setRowSummary(i, !infoItem.isRowSummary());
              refresh.run();
            });
            table.setWidget(rIdx, idx, rowResult, STYLE_ROW_SUMMARY);
            table.getCellFormatter().addStyleName(rIdx, idx, infoItem.getStyle());
            idx++;
          }
          table.getCellFormatter().setColSpan(rIdx + 1, 1 + cCnt, cCnt);
          table.getCellFormatter().addStyleName(rIdx + 1, 1 + cCnt, STYLE_SUMMARY);
        } else {
          table.setText(0, 0, Localized.dictionary().groupBy(), STYLE_COLGROUP_HEADER);
          cGroup = new CustomSpan(STYLE_ADD);
          ((CustomSpan) cGroup).addClickHandler(
              event -> ReportItem.chooseItem(infoItems, false, item -> {
                activeReport.setColGrouping(item);
                refresh.run();
              }));
        }
        table.setWidget(0, 1, cGroup, STYLE_COLGROUP);

        Horizontal cont = new Horizontal();
        cont.add(table.getWidget(rIdx, rCnt + cCnt - 1));
        cont.add(colAdd);
        colAdd = cont;
      } else {
        table.setText(0, rCnt, Localized.dictionary().columns(), STYLE_COL_HEADER);
        cCnt++;
      }
      table.setWidget(rIdx, rCnt + cCnt - 1, colAdd, STYLE_COL);

      // ROW GROUPING
      if (activeReport.getRowItems().size() > 0) {
        table.insertRow(rIdx);
        Horizontal rGroup = new Horizontal();

        if (activeReport.getRowGrouping() != null) {
          rGroup.add(buildCaption(activeReport.getRowGrouping(), refresh));
          rGroup.add(activeReport.getRowGrouping().getItem().render(infoItems, () -> {
            activeReport.setRowGrouping(null);
            refresh.run();
          }, refresh));
        } else {
          rGroup.add(new Label(Localized.dictionary().groupBy()));
          CustomSpan rGroupAdd = new CustomSpan(STYLE_ADD);
          rGroupAdd.addClickHandler(event -> ReportItem.chooseItem(infoItems, false, item -> {
            activeReport.setRowGrouping(item);
            refresh.run();
          }));
          rGroup.add(rGroupAdd);
        }
        table.setWidget(rIdx, 0, rGroup, STYLE_ROWGROUP);

        if (activeReport.getColItems().size() > 0) {
          if (activeReport.getRowGrouping() != null) {
            table.getCellFormatter().setColSpan(rIdx, 0, rCnt);
            int idx = 1;

            for (ReportInfoItem infoItem : activeReport.getColItems()) {
              int i = idx - 1;
              CustomSpan groupResult = new CustomSpan(infoItem.isGroupSummary()
                  ? STYLE_SUMMARY_ON : STYLE_SUMMARY_OFF);
              groupResult.setTitle(Localized.dictionary().groupResults());

              groupResult.addClickHandler(event -> {
                activeReport.setGroupSummary(i, !infoItem.isGroupSummary());
                refresh.run();
              });
              table.setWidget(rIdx, idx, groupResult, STYLE_ROWGROUP_COL_SUMMARY);
              table.getCellFormatter().addStyleName(rIdx, idx, infoItem.getStyle());
              idx++;
            }
            if (activeReport.getColGrouping() != null) {
              table.getCellFormatter().setColSpan(rIdx, 1 + cCnt, cCnt);
              table.getCellFormatter().addStyleName(rIdx, 1 + cCnt, STYLE_ROWGROUP_SUMMARY);
            }
          } else {
            table.getCellFormatter().setColSpan(rIdx, 0,
                rCnt + cCnt + (activeReport.getColGrouping() != null ? cCnt : 0));
          }
        } else {
          table.getCellFormatter().setColSpan(rIdx, 0, rCnt + cCnt);
        }
      }
      container.add(table);
    }
  }

  private void renderParams() {
    HasWidgets container = (HasWidgets) getFormView().getWidgetByName(NAME_PARAMS_CONTAINER);

    if (container == null) {
      return;
    }
    container.clear();
    getReport().getReportParams().forEach((name, editor) -> {
      DomUtils.setDataKey(editor.getElement(), name);
      container.add(editor.asWidget());
    });
  }

  private void saveReport(ReportInfo rep) {
    if (DataUtils.isId(rep.getId())) {
      Queries.update(VIEW_REPORT_SETTINGS, Filter.compareId(rep.getId()),
          Arrays.asList(COL_RS_PARAMETERS, COL_RS_USER),
          Queries.asList(rep.serialize(), rep.isGlobal() ? null : BeeKeeper.getUser().getUserId()),
          null);

      activateReport(rep);
    } else {
      Queries.insert(VIEW_REPORT_SETTINGS, Data.getColumns(VIEW_REPORT_SETTINGS,
          Arrays.asList(COL_RS_USER, COL_RS_REPORT, COL_RS_PARAMETERS)),
          Queries.asList(rep.isGlobal() ? null : BeeKeeper.getUser().getUserId(),
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
