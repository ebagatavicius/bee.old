package com.butent.bee.client.view.form.interceptor;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.output.ReportItem;
import com.butent.bee.client.output.ReportItem.Function;
import com.butent.bee.client.output.ReportNumericItem;
import com.butent.bee.client.output.ReportParameters;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ExtendedReportInterceptor extends ReportInterceptor {

  private static final String NAME_DETALIZATION_CONTAINER = "DetalizationContainer";
  private static final String NAME_DETALIZATION_ADD = "DetalizationAdd";
  private static final String NAME_DETALIZATION_GROUP = "DetalizationGroup";

  private static final String NAME_CALCULATION_CONTAINER = "CalculationContainer";
  private static final String NAME_CALCULATION_ADD = "CalculationAdd";
  private static final String NAME_CALCULATION_GROUP = "CalculationGroup";

  private static final String NAME_START_DATE = "StartDate";
  private static final String NAME_END_DATE = "EndDate";
  private static final String NAME_CURRENCY = AdministrationConstants.COL_CURRENCY;
  private static final String NAME_VAT = TradeConstants.COL_TRADE_VAT;

  private static final String STYLE_ITEM = "bee-rep-item";
  private static final String STYLE_CAPTION = STYLE_ITEM + "-cap";
  private static final String STYLE_CALCULATION = STYLE_ITEM + "-calc";
  private static final String STYLE_REMOVE = STYLE_ITEM + "-remove";

  private static final String STYLE_R_DATA = "bee-rep";

  private static final String STYLE_R_COLGROUP = STYLE_R_DATA + "-cgroup";
  private static final String STYLE_R_COLGROUP_HEADER = STYLE_R_COLGROUP + "-hdr";
  private static final String STYLE_R_COLGROUP_SUMMARY_HEADER = STYLE_R_COLGROUP + "-tot-hdr";

  private static final String STYLE_R_ROWGROUP = STYLE_R_DATA + "-rgroup";
  private static final String STYLE_R_ROWGROUP_COL_SUMMARY = STYLE_R_ROWGROUP + "-col-tot";
  private static final String STYLE_R_ROWGROUP_SUMMARY = STYLE_R_ROWGROUP + "-tot";

  private static final String STYLE_R_ROW = STYLE_R_DATA + "-row";
  private static final String STYLE_R_ROW_HEADER = STYLE_R_ROW + "-hdr";
  private static final String STYLE_R_ROW_SUMMARY = STYLE_R_ROW + "-tot";
  private static final String STYLE_R_ROW_SUMMARY_HEADER = STYLE_R_ROW_SUMMARY + "-hdr";

  private static final String STYLE_R_COL = STYLE_R_DATA + "-col";
  private static final String STYLE_R_COL_HEADER = STYLE_R_COL + "-hdr";
  private static final String STYLE_R_COL_SUMMARY = STYLE_R_COL + "-tot";

  private static final String STYLE_R_SUMMARY = STYLE_R_DATA + "-tot";
  private static final String STYLE_R_SUMMARY_HEADER = STYLE_R_SUMMARY + "-hdr";

  private final Report report;

  private final List<ReportItem> rowItems = new ArrayList<>();
  private ReportItem rowGrouping;

  private final List<ReportItem> colItems = new ArrayList<>();
  private ReportItem colGrouping;

  public ExtendedReportInterceptor(Report report) {
    this.report = Assert.notNull(report);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof HasClickHandlers) {
      switch (name) {
        case NAME_DETALIZATION_ADD:
          ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              addDetalizationItems();
            }
          });
          break;

        case NAME_CALCULATION_ADD:
          ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              addCalculationItems();
            }
          });
          break;
      }
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public String getCaption() {
    return getReport().getReportName();
  }

  @Override
  public FormInterceptor getInstance() {
    return new ExtendedReportInterceptor(getReport());
  }

  @Override
  public void onLoad(FormView form) {
    ReportParameters parameters = readParameters();

    if (parameters != null) {
      loadDateTime(parameters, NAME_START_DATE, form);
      loadDateTime(parameters, NAME_END_DATE, form);
      loadId(parameters, NAME_CURRENCY, form);
      loadBoolean(parameters, NAME_VAT, form);
    }
    Multimap<String, ReportItem> defaults = getReport().getDefaults();

    if (defaults != null) {
      rowItems.addAll(defaults.get(Report.PROP_ROWS));
      rowGrouping = BeeUtils.peek(defaults.get(Report.PROP_ROW_GROUP));
      colItems.addAll(defaults.get(Report.PROP_COLUMNS));
      colGrouping = BeeUtils.peek(defaults.get(Report.PROP_COLUMN_GROUP));
    }
    render(NAME_DETALIZATION_CONTAINER);
    render(NAME_CALCULATION_CONTAINER);
    super.onLoad(form);
  }

  @Override
  public void onUnload(FormView form) {
    storeDateTimeValues(NAME_START_DATE, NAME_END_DATE);
    storeEditorValues(NAME_CURRENCY);
    storeBooleanValues(NAME_VAT);
  }

  @Override
  protected void clearFilter() {
    clearEditor(NAME_START_DATE);
    clearEditor(NAME_END_DATE);
  }

  @Override
  protected void doReport() {
    if (getDataContainer() != null) {
      getDataContainer().clear();
    }
    if (BeeUtils.isEmpty(rowItems) && BeeUtils.isEmpty(colItems)) {
      getFormView().notifyWarning(Localized.getConstants().noData());
      return;
    }
    DateTime start = getDateTime(NAME_START_DATE);
    DateTime end = getDateTime(NAME_END_DATE);

    if (!checkRange(start, end)) {
      return;
    }
    ParameterList params = BeeKeeper.getRpc()
        .createParameters(getReport().getModuleAndSub().getModule(), getReport().getReportName());

    if (start != null) {
      params.addDataItem(Service.VAR_FROM, start.getTime());
    }
    if (end != null) {
      params.addDataItem(Service.VAR_TO, end.getTime());
    }
    String currency = getEditorValue(NAME_CURRENCY);

    if (DataUtils.isId(currency)) {
      params.addDataItem(AdministrationConstants.COL_CURRENCY, currency);
    }
    if (getBoolean(NAME_VAT)) {
      params.addDataItem(TradeConstants.COL_TRADE_VAT, "1");
    }

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
    List<String> labels = StringList.of(getReportCaption(),
        Format.renderPeriod(getDateTime(NAME_START_DATE), getDateTime(NAME_END_DATE)),
        getFilterLabel(NAME_CURRENCY));

    return BeeUtils.joinWords(labels);
  }

  @Override
  protected Report getReport() {
    return report;
  }

  @Override
  protected ReportParameters getReportParameters() {
    ReportParameters parameters = new ReportParameters();

    addDateTimeValues(parameters, NAME_START_DATE, NAME_END_DATE);
    addEditorValues(parameters, NAME_CURRENCY);
    addBooleanValues(parameters, NAME_VAT);

    return parameters;
  }

  @Override
  protected boolean validateParameters(ReportParameters parameters) {
    DateTime start = parameters.getDateTime(NAME_START_DATE);
    DateTime end = parameters.getDateTime(NAME_END_DATE);

    return checkRange(start, end);
  }

  private void addCalculationItems() {
    Set<String> skip = new HashSet<>();

    for (ReportItem item : colItems) {
      skip.add(item.getName());
    }
    chooseItems(skip, true, new Consumer<List<ReportItem>>() {
      @Override
      public void accept(List<ReportItem> input) {
        for (ReportItem item : input) {
          colItems.add(item.create().enableCalculation());
        }
        render(NAME_CALCULATION_CONTAINER);
      }
    });
  }

  private void addDetalizationItems() {
    Set<String> skip = new HashSet<>();

    for (ReportItem entry : getReport().getItems()) {
      if (entry instanceof ReportNumericItem) {
        skip.add(entry.getName());
      }
    }
    for (ReportItem item : rowItems) {
      skip.add(item.getName());
    }
    chooseItems(skip, true, new Consumer<List<ReportItem>>() {
      @Override
      public void accept(List<ReportItem> input) {
        for (ReportItem item : input) {
          rowItems.add(item.create());
        }
        render(NAME_DETALIZATION_CONTAINER);
      }
    });
  }

  private void chooseItem(final Consumer<ReportItem> consumer) {
    Set<String> skip = new HashSet<>();

    for (ReportItem entry : getReport().getItems()) {
      if (entry instanceof ReportNumericItem) {
        skip.add(entry.getName());
      }
    }
    chooseItems(skip, false, new Consumer<List<ReportItem>>() {
      @Override
      public void accept(List<ReportItem> input) {
        consumer.accept(BeeUtils.peek(input));
      }
    });
  }

  private void chooseItems(Set<String> skip, boolean multi,
      final Consumer<List<ReportItem>> consumer) {

    final ListBox list = new ListBox(multi);

    for (ReportItem item : getReport().getItems()) {
      if (!BeeUtils.contains(skip, item.getName())) {
        list.addItem(item.getCaption(), item.getName());
      }
    }
    if (list.isEmpty()) {
      getFormView().notifyWarning(Localized.getConstants().noData());
      return;
    }
    list.setVisibleItemCount(BeeUtils.min(list.getItemCount(), 20));

    Global.inputWidget(Localized.getConstants().value(), list, new InputCallback() {
      @Override
      public void onSuccess() {
        List<ReportItem> selected = new ArrayList<>();

        for (int i = 0; i < list.getItemCount(); i++) {
          OptionElement optionElement = list.getOptionElement(i);

          if (optionElement.isSelected()) {
            for (ReportItem item : getReport().getItems()) {
              if (BeeUtils.same(item.getName(), optionElement.getValue())) {
                selected.add(item);
                break;
              }
            }
          }
        }
        if (!BeeUtils.isEmpty(selected)) {
          consumer.accept(selected);
        }
      }
    });
  }

  private static String getItemStyle(ReportItem item) {
    if (item.getFunction() != Function.LIST
        && (item instanceof ReportNumericItem || item.getFunction() == Function.COUNT)) {
      return STYLE_R_DATA + "-num";
    }
    return null;
  }

  private void render(final String containerName) {
    HasWidgets container = (HasWidgets) getFormView().getWidgetByName(containerName);

    if (container != null) {
      final List<? extends ReportItem> items;
      ReportItem groupItem;
      HasWidgets groupContainer;
      final Consumer<ReportItem> groupSetter;

      if (BeeUtils.equalsTrim(containerName, NAME_DETALIZATION_CONTAINER)) {
        items = rowItems;
        groupItem = rowGrouping;
        groupContainer = (HasWidgets) getFormView().getWidgetByName(NAME_DETALIZATION_GROUP);
        groupSetter = new Consumer<ReportItem>() {
          @Override
          public void accept(ReportItem input) {
            rowGrouping = input;
          }
        };
      } else {
        items = colItems;
        groupItem = colGrouping;
        groupContainer = (HasWidgets) getFormView().getWidgetByName(NAME_CALCULATION_GROUP);
        groupSetter = new Consumer<ReportItem>() {
          @Override
          public void accept(ReportItem input) {
            colGrouping = input;
          }
        };
      }
      container.clear();
      int idx = 0;

      for (ReportItem item : items) {
        final int index = idx++;

        container.add(renderItem(containerName, item, new Runnable() {
          @Override
          public void run() {
            items.remove(index);
          }
        }));
      }
      if (groupContainer != null) {
        groupContainer.clear();

        if (BeeUtils.isPositive(idx)) {
          groupContainer.add(new InlineLabel(Localized.getConstants().groupBy() + ": "));

          if (groupItem != null) {
            groupContainer.add(renderItem(containerName, groupItem, new Runnable() {
              @Override
              public void run() {
                groupSetter.accept(null);
              }
            }));
          } else {
            FaLabel add = new FaLabel(FontAwesome.PLUS, true);
            add.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                chooseItem(new Consumer<ReportItem>() {
                  @Override
                  public void accept(ReportItem item) {
                    groupSetter.accept(item.create());
                    render(containerName);
                  }
                });
              }
            });
            groupContainer.add(add);
          }
        } else {
          groupSetter.accept(null);
        }
      }
    }
  }

  private void renderData(SimpleRowSet rowSet) {
    if (DataUtils.isEmpty(rowSet)) {
      getFormView().notifyWarning(Localized.getConstants().nothingFound());
      return;
    }
    Map<String, Map<String, String[]>> rowGroups = new TreeMap<>();
    Set<String> colGroups = new TreeSet<>();
    Table<String, String, Object> values = HashBasedTable.create();

    for (final SimpleRow row : rowSet) {
      String rowGroup = null;
      String colGroup = null;

      if (rowGrouping != null) {
        rowGroup = rowGrouping.evaluate(row);
      }
      if (colGrouping != null) {
        colGroup = colGrouping.evaluate(row);
      }
      rowGroup = BeeUtils.nvl(rowGroup, "");
      colGroup = BeeUtils.nvl(colGroup, "");
      String[] details = new String[rowItems.size()];
      StringBuilder sb = new StringBuilder();
      int i = 0;

      for (ReportItem item : rowItems) {
        String value = item.evaluate(row);
        sb.append(i).append(BeeUtils.notEmpty(value, " "));
        details[i++] = value;
      }
      String key = "r" + rowGroup + "v" + sb.toString() + "c" + colGroup;
      boolean ok = BeeUtils.isEmpty(colItems);

      for (ReportItem item : colItems) {
        String col = item.getName();
        Object value = item.calculate(values.get(key, col), item.evaluate(row));

        if (value != null) {
          ok = true;
          values.put(key, col, value);
        }
      }
      if (ok) {
        if (!rowGroups.containsKey(rowGroup)) {
          rowGroups.put(rowGroup, new TreeMap<String, String[]>());
        }
        if (!colGroups.contains(colGroup)) {
          colGroups.add(colGroup);
        }
        Map<String, String[]> rowMap = rowGroups.get(rowGroup);
        key = sb.toString();

        if (!rowMap.containsKey(key)) {
          rowMap.put(key, details);
        }
      }
    }
    if (BeeUtils.isEmpty(rowGroups)) {
      getFormView().notifyWarning(Localized.getConstants().nothingFound());
      return;
    }
    HtmlTable table = new HtmlTable(STYLE_R_DATA);
    // HEADER
    int r = 0;
    int c = 0;

    if (colGrouping != null) {
      if (!BeeUtils.isEmpty(rowItems)) {
        table.getCellFormatter().setColSpan(r, c, rowItems.size());
      }
      table.setText(r, c++, colGrouping.getCaption(), STYLE_R_COLGROUP_HEADER);

      for (String colGroup : colGroups) {
        table.getCellFormatter().setColSpan(r, c, colItems.size());
        table.setText(r, c++, colGroup, STYLE_R_COLGROUP);
      }
      r++;
    }
    c = 0;

    for (ReportItem item : rowItems) {
      table.setText(r, c++, item.getCaption(), STYLE_R_ROW_HEADER, getItemStyle(item));
    }
    c = Math.max(c, 1);

    for (int i = 0; i < colGroups.size(); i++) {
      for (ReportItem item : colItems) {
        table.setText(r, c++, item.getCaption(), STYLE_R_COL_HEADER, getItemStyle(item));
      }
    }
    if (colGrouping != null) {
      int x = c;

      for (ReportItem item : colItems) {
        if (item.isRowSummary()) {
          table.setText(r, x++, item.getCaption(), STYLE_R_ROW_SUMMARY_HEADER, getItemStyle(item));
        }
      }
      if (x > c) {
        table.getCellFormatter().setColSpan(0, colGroups.size() + 1, x - c);
        table.setText(0, colGroups.size() + 1, Localized.getConstants().total(),
            STYLE_R_COLGROUP_SUMMARY_HEADER);
      }
    }
    Table<String, String, Object> colTotals = null;

    for (ReportItem item : colItems) {
      if (item.isColSummary()) {
        colTotals = HashBasedTable.create();
        break;
      }
    }
    r++;
    // DETAILS
    for (String rowGroup : rowGroups.keySet()) {
      int groupIdx = BeeConst.UNDEF;
      Table<String, String, Object> groupTotals = null;

      if (rowGrouping != null) {
        if (colTotals != null) {
          groupIdx = r;
          groupTotals = HashBasedTable.create();
        }
        if (!BeeUtils.isEmpty(rowItems)) {
          table.getCellFormatter().setColSpan(r, 0, rowItems.size());
        }
        table.setText(r++, 0, rowGrouping.getCaption() + ": " + rowGroup, STYLE_R_ROWGROUP);
      }
      for (Entry<String, String[]> entry : rowGroups.get(rowGroup).entrySet()) {
        c = 0;

        for (String detail : entry.getValue()) {
          table.setText(r, c++, detail, STYLE_R_ROW);
        }
        c = Math.max(c, 1);
        Map<String, Object> rowTotals = new HashMap<>();

        for (String colGroup : colGroups) {
          String key = "r" + rowGroup + "v" + entry.getKey() + "c" + colGroup;

          for (ReportItem item : colItems) {
            String col = item.getName();
            Object value = values.get(key, col);
            table.setText(r, c++, value != null ? value.toString() : null,
                STYLE_R_COL, getItemStyle(item));

            if (value != null) {
              if (item.isRowSummary() && colGrouping != null) {
                rowTotals.put(col, item.summarize(rowTotals.get(col), value));
              }
              if (item.isColSummary()) {
                if (groupTotals != null) {
                  groupTotals.put(colGroup, col, item.summarize(groupTotals.get(colGroup, col),
                      value));
                }
                colTotals.put(colGroup, col, item.summarize(colTotals.get(colGroup, col), value));
              }
            }
          }
        }
        for (ReportItem item : colItems) {
          if (item.isRowSummary() && colGrouping != null) {
            Object value = rowTotals.get(item.getName());
            table.setText(r, c++, value != null ? value.toString() : null,
                STYLE_R_ROW_SUMMARY, getItemStyle(item));
          }
        }
        r++;
      }
      if (groupTotals != null) {
        c = 1;
        Map<String, Object> rowTotals = new HashMap<>();

        for (String colGroup : colGroups) {
          for (ReportItem item : colItems) {
            if (item.isColSummary()) {
              String col = item.getName();
              Object value = groupTotals.get(colGroup, col);

              if (item.isRowSummary() && colGrouping != null) {
                rowTotals.put(col, item.summarize(rowTotals.get(col), value));
              }
              table.setText(groupIdx, c, value != null ? value.toString() : null,
                  STYLE_R_ROWGROUP_COL_SUMMARY, getItemStyle(item));
            }
            c++;
          }
        }
        for (ReportItem item : colItems) {
          if (item.isRowSummary() && colGrouping != null) {
            Object value = rowTotals.get(item.getName());
            table.setText(groupIdx, c++, value != null ? value.toString() : null,
                STYLE_R_ROWGROUP_SUMMARY, getItemStyle(item));
          }
        }
      }
    }
    if (colTotals != null) {
      c = 0;

      if (!BeeUtils.isEmpty(rowItems)) {
        table.getCellFormatter().setColSpan(r, c, rowItems.size());
      }
      table.setText(r, c++, Localized.getConstants().totalOf(), STYLE_R_SUMMARY_HEADER);
      Map<String, Object> rowTotals = new HashMap<>();

      for (String colGroup : colGroups) {
        for (ReportItem item : colItems) {
          if (item.isColSummary()) {
            String col = item.getName();
            Object value = colTotals.get(colGroup, col);

            if (item.isRowSummary() && colGrouping != null) {
              rowTotals.put(col, item.summarize(rowTotals.get(col), value));
            }
            table.setText(r, c, value != null ? value.toString() : null,
                STYLE_R_COL_SUMMARY, getItemStyle(item));
          }
          c++;
        }
      }
      for (ReportItem item : colItems) {
        if (item.isRowSummary() && colGrouping != null) {
          Object value = rowTotals.get(item.getName());
          table.setText(r, c++, value != null ? value.toString() : null,
              STYLE_R_SUMMARY, getItemStyle(item));
        }
      }
    }
    getDataContainer().add(table);
  }

  private Widget renderItem(final String containerName, final ReportItem item,
      final Runnable removeCallback) {

    Flow box = new Flow(STYLE_ITEM);
    InlineLabel label = new InlineLabel(item.getCaption());
    label.addStyleName(STYLE_CAPTION);
    box.add(label);

    if (item.getFunction() != null) {
      box.insert(new InlineLabel("("), 0);
      label = new InlineLabel(item.getFunction().getCaption());
      label.addStyleName(STYLE_CALCULATION);
      box.insert(label, 0);
      box.add(new InlineLabel(")"));
    }
    if (removeCallback != null) {
      CustomDiv remove = new CustomDiv(STYLE_REMOVE);
      remove.setText(String.valueOf(BeeConst.CHAR_TIMES));

      remove.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          removeCallback.run();
          render(containerName);
        }
      });
      box.add(remove);
    }
    box.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        HtmlTable table = new HtmlTable();
        table.setColumnCellStyles(0, "text-align:right;");
        int c = 0;

        final InputText expression = new InputText();
        expression.setValue(item.getExpression());
        expression.setEnabled(false);

        table.setText(c, 0, "Išraiška");
        table.setWidget(c++, 1, expression);

        final Editor options = item.getOptionsEditor();

        if (options != null) {
          table.setText(c, 0, item.getOptionsCaption());
          table.setWidget(c++, 1, options.asWidget());
        }
        final ListBox function;
        final InputBoolean colSummary;
        final InputBoolean rowSummary;

        if (item.getFunction() != null) {
          function = new ListBox();

          for (Function fnc : item.getAvailableFunctions()) {
            function.addItem(fnc.getCaption(), fnc.name());
          }
          function.setValue(item.getFunction().name());

          table.setText(c, 0, Localized.getConstants().value());
          table.setWidget(c++, 1, function);

          colSummary = new InputBoolean("Stulpelio rezultatai");
          colSummary.setChecked(item.isColSummary());
          table.setWidget(c++, 1, colSummary);

          if (colGrouping != null) {
            rowSummary = new InputBoolean("Eilutės rezultatai");
            rowSummary.setChecked(item.isRowSummary());
            table.setWidget(c++, 1, rowSummary);
          } else {
            rowSummary = null;
          }
        } else {
          function = null;
          colSummary = null;
          rowSummary = null;
        }
        Global.inputWidget(item.getCaption(), table, new InputCallback() {
          @Override
          public void onSuccess() {
            item.setExpression(expression.getValue());

            if (options != null) {
              item.setOptions(options.getValue());
            }
            if (function != null) {
              item.setFunction(EnumUtils.getEnumByName(Function.class, function.getValue()));

              if (colSummary != null) {
                item.setColSummary(colSummary.isChecked());
              }
              if (rowSummary != null) {
                item.setRowSummary(rowSummary.isChecked());
              }
            }
            render(containerName);
          }
        });
      }
    });
    return box;
  }
}
