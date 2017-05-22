package com.butent.bee.client.modules.trade.reports;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.HasClickHandlers;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.output.Exporter;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.ReportInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XRow;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeReportGroup;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TradeStockReport extends ReportInterceptor {

  private static BeeLogger logger = LogUtils.getLogger(TradeStockReport.class);

  private static final List<String> SELECTOR_NAMES = Arrays.asList(
      RP_WAREHOUSES, RP_SUPPLIERS, RP_MANUFACTURERS, RP_DOCUMENTS,
      RP_ITEM_TYPES, RP_ITEM_GROUPS, RP_ITEM_CATEGORIES, RP_ITEMS);

  private static final List<String> GROUP_NAMES = reportGroupNames(5);

  private static final String KEY_GROUP = "group";
  private static final String KEY_ID = "id";

  private final String styleTable = stylePrefix() + "table";

  private final String styleHeader = stylePrefix() + "header";
  private final String styleBody = stylePrefix() + "body";
  private final String styleFooter = stylePrefix() + "footer";

  private final String styleColumnEmptyLabel = stylePrefix() + "column-empty-label";
  private final String styleColumnLabel = stylePrefix() + "column-label";

  private final String stylePrice = stylePrefix() + "price";
  private final String styleQuantity = stylePrefix() + "qty";
  private final String styleAmount = stylePrefix() + "amount";

  private final String styleQuantityRow = stylePrefix() + "qty-row";
  private final String styleAmountRow = stylePrefix() + "amount-row";

  private final String styleRowTotal = stylePrefix() + "row-total";
  private final String styleTotal = stylePrefix() + "total";

  private final String styleEditable = stylePrefix() + "editable";

  private final XSheet sheet = new XSheet();

  public TradeStockReport() {
  }

  @Override
  public void onLoad(FormView form) {
    ReportParameters parameters = readParameters();

    if (parameters != null) {
      loadDateTime(parameters, RP_DATE, form);
      commonLoad(parameters, form);
    }

    super.onLoad(form);
  }

  protected static void commonLoad(ReportParameters parameters, FormView form) {
    loadBoolean(parameters, RP_SHOW_QUANTITY, form);
    loadBoolean(parameters, RP_SHOW_AMOUNT, form);

    loadListByIndex(parameters, RP_ITEM_PRICE, form);
    loadId(parameters, RP_CURRENCY, form);

    loadMulti(parameters, SELECTOR_NAMES, form);

    loadDateTime(parameters, RP_RECEIVED_FROM, form);
    loadDateTime(parameters, RP_RECEIVED_TO, form);

    loadText(parameters, RP_ITEM_FILTER, form);

    loadGroupByValue(parameters, GROUP_NAMES, form);
    loadBoolean(parameters, RP_SUMMARY, form);

    loadListByValue(parameters, RP_STOCK_COLUMNS, form);
  }

  @Override
  public void onUnload(FormView form) {
    storeDateTimeValues(RP_DATE);
    commonStore();
  }

  protected void commonStore() {
    storeDateTimeValues(RP_RECEIVED_FROM, RP_RECEIVED_TO);
    storeBooleanValues(RP_SHOW_QUANTITY, RP_SHOW_AMOUNT, RP_SUMMARY);

    storeSelectedIndex(RP_ITEM_PRICE, 0);
    storeSelectedValue(RP_STOCK_COLUMNS, 1);

    storeEditorValues(RP_CURRENCY, RP_ITEM_FILTER);
    storeEditorValues(SELECTOR_NAMES);

    storeGroupByValue(GROUP_NAMES);
  }

  @Override
  public FormInterceptor getInstance() {
    return new TradeStockReport();
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (RP_DOCUMENTS.equals(name) && widget instanceof DataSelector) {
      ((DataSelector) widget).setAdditionalFilter(getDocumentSelectorFilter());

    } else if (NAME_DATA_CONTAINER.equals(name) && widget instanceof HasClickHandlers) {
      ((HasClickHandlers) widget).addClickHandler(event -> {
        Element target = EventUtils.getEventTargetElement(event);
        TableCellElement cell = DomUtils.getParentCell(target, true);

        if (cell != null) {
          onCellClick(cell);
        }
      });
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  protected void clearFilter() {
    clearEditors(RP_DATE);
    commonClearFilter();
  }

  protected void commonClearFilter() {
    clearEditors(RP_RECEIVED_FROM, RP_RECEIVED_TO, RP_ITEM_FILTER);
    clearEditors(SELECTOR_NAMES);
  }

  protected String getService() {
    return SVC_TRADE_STOCK_REPORT;
  }

  @Override
  protected void doReport() {
    ReportParameters reportParameters = getReportParameters();

    if (validateParameters(reportParameters)) {
      ParameterList parameters = TradeKeeper.createArgs(getService());
      parameters.addDataItem(Service.VAR_REPORT_PARAMETERS, reportParameters.serialize());

      BeeKeeper.getRpc().makeRequest(parameters, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          if (response.hasMessages()) {
            response.notify(getFormView());
          }

          if (response.hasResponse()) {
            Map<String, String> data = Codec.deserializeHashMap(response.getResponseAsString());
            render(data);

            sheet.addHeaders(getLabels(false));
            sheet.autoSizeAll();

          } else {
            getFormView().notifyWarning(Localized.dictionary().nothingFound());
          }
        }
      });
    }
  }

  @Override
  protected void export() {
    if (!sheet.isEmpty()) {
      Exporter.maybeExport(sheet, getExportFileName());
    }
  }

  protected String getExportFileName() {
    return Localized.dictionary().trdStock();
  }

  @Override
  protected String getBookmarkLabel() {
    List<String> labels = getLabels(true);
    return BeeUtils.joinWords(labels);
  }

  @Override
  protected Report getReport() {
    return Report.TRADE_STOCK;
  }

  @Override
  protected ReportParameters getReportParameters() {
    ReportParameters parameters = new ReportParameters();

    addDateTimeValues(parameters, RP_DATE);
    addCommonParameters(parameters);

    return parameters;
  }

  protected void addCommonParameters(ReportParameters parameters) {
    addDateTimeValues(parameters, RP_RECEIVED_FROM, RP_RECEIVED_TO);
    addBooleanValues(parameters, RP_SHOW_QUANTITY, RP_SHOW_AMOUNT, RP_SUMMARY);

    addSelectedIndex(parameters, RP_ITEM_PRICE, 0);
    addSelectedValue(parameters, RP_STOCK_COLUMNS, 1);

    addEditorValues(parameters, RP_CURRENCY, RP_ITEM_FILTER);
    addEditorValues(parameters, SELECTOR_NAMES);

    addGroupByValue(parameters, GROUP_NAMES);
  }

  protected String stylePrefix() {
    return TradeKeeper.STYLE_PREFIX + "report-stock-";
  }

  protected String styleTable() {
    return styleTable;
  }

  protected String styleHeader() {
    return styleHeader;
  }

  protected String styleBody() {
    return styleBody;
  }

  protected String styleFooter() {
    return styleFooter;
  }

  protected String styleColumnEmptyLabel() {
    return styleColumnEmptyLabel;
  }

  protected String styleColumnLabel() {
    return styleColumnLabel;
  }

  protected String stylePrice() {
    return stylePrice;
  }

  protected String styleQuantity() {
    return styleQuantity;
  }

  protected String styleAmount() {
    return styleAmount;
  }

  protected String styleQuantityRow() {
    return styleQuantityRow;
  }

  protected String styleAmountRow() {
    return styleAmountRow;
  }

  protected String styleRowTotal() {
    return styleRowTotal;
  }

  protected String styleTotal() {
    return styleTotal;
  }

  protected String styleEditable() {
    return styleEditable;
  }

  @Override
  protected boolean validateParameters(ReportParameters parameters) {
    DateTime receivedFrom = parameters.getDateTime(RP_RECEIVED_FROM);
    DateTime receivedTo = parameters.getDateTime(RP_RECEIVED_TO);

    return checkRange(receivedFrom, receivedTo)
        && checkFilter(ClassifierConstants.VIEW_ITEMS, parameters.getText(RP_ITEM_FILTER));
  }

  protected String getDateCaption() {
    return Format.renderDateLong(getDateTime(RP_DATE));
  }

  protected XSheet sheet() {
    return sheet;
  }

  private List<String> getCaptions(String dateCaption, boolean qty, boolean amount,
      ItemPrice itemPrice, String currencyName) {

    List<String> captions = new ArrayList<>();

    captions.add(getReportCaption());
    if (!BeeUtils.isEmpty(dateCaption)) {
      captions.add(dateCaption);
    }

    if (qty && !amount) {
      captions.add(Localized.dictionary().quantity());

    } else if (itemPrice != null || !BeeUtils.isEmpty(currencyName)) {
      String priceName = (itemPrice == null) ? null : itemPrice.getCaption();
      captions.add(BeeUtils.joinItems(priceName, currencyName));
    }

    return captions;
  }

  protected List<String> getLabels(boolean addGrouping) {
    List<String> labels = StringList.uniqueCaseSensitive();

    labels.addAll(getCaptions(getDateCaption(),
        getBoolean(RP_SHOW_QUANTITY), getBoolean(RP_SHOW_AMOUNT),
        getItemPrice(), getSelectorLabel(RP_CURRENCY)));

    SELECTOR_NAMES.forEach(name -> labels.add(getSelectorLabel(name)));

    DateTime rFrom = getDateTime(RP_RECEIVED_FROM);
    DateTime rTo = getDateTime(RP_RECEIVED_TO);

    if (rFrom != null || rTo != null) {
      labels.add(BeeUtils.joinWords(Localized.dictionary().received(),
          Format.renderPeriod(rFrom, rTo)));
    }

    labels.add(getEditorValue(RP_ITEM_FILTER));

    if (addGrouping) {
      GROUP_NAMES.stream()
          .filter(name -> BeeUtils.isPositive(getSelectedIndex(name)))
          .forEach(name -> labels.add(getSelectedItemText(name)));

      if (getBoolean(RP_SUMMARY)) {
        labels.add(Localized.dictionary().summary());
      }

      if (BeeUtils.isPositive(getSelectedIndex(RP_STOCK_COLUMNS))) {
        labels.add(BeeUtils.joinWords(Localized.dictionary().trdReportColumnsStock(),
            getSelectedItemText(RP_STOCK_COLUMNS)));
      }
    }

    return labels;
  }

  private static Filter getDocumentSelectorFilter() {
    EnumSet<TradeDocumentPhase> phases = EnumSet.noneOf(TradeDocumentPhase.class);
    phases.addAll(Arrays.stream(TradeDocumentPhase.values())
        .filter(TradeDocumentPhase::modifyStock)
        .collect(Collectors.toSet()));

    EnumSet<OperationType> operationTypes = EnumSet.noneOf(OperationType.class);
    operationTypes.addAll(Arrays.stream(OperationType.values())
        .filter(OperationType::producesStock)
        .collect(Collectors.toSet()));

    return Filter.and(Filter.any(COL_TRADE_DOCUMENT_PHASE, phases),
        Filter.any(COL_OPERATION_TYPE, operationTypes));
  }

  private ItemPrice getItemPrice() {
    return EnumUtils.getEnumByIndex(ItemPrice.class, getSelectedIndex(RP_ITEM_PRICE));
  }

  protected void render(Map<String, String> data) {
    SimpleRowSet rowSet = SimpleRowSet.restore(data.get(Service.VAR_DATA));
    if (DataUtils.isEmpty(rowSet)) {
      String message = Localized.dictionary().keyNotFound(Service.VAR_DATA);

      logger.severe(message);
      getFormView().notifySevere(message);
      return;
    }

    ReportParameters parameters = ReportParameters.restore(data.get(Service.VAR_REPORT_PARAMETERS));

    List<TradeReportGroup> rowGroups = EnumUtils.parseIndexList(TradeReportGroup.class,
        data.get(RP_ROW_GROUPS));

    List<String> rowGroupLabelColumns = new ArrayList<>();
    List<String> rowGroupValueColumns = new ArrayList<>();

    if (!rowGroups.isEmpty()) {
      rowGroupLabelColumns.addAll(NameUtils.toList(data.get(RP_ROW_GROUP_LABEL_COLUMNS)));
      rowGroupValueColumns.addAll(NameUtils.toList(data.get(RP_ROW_GROUP_VALUE_COLUMNS)));
    }

    TradeReportGroup columnGroup = EnumUtils.getEnumByIndex(TradeReportGroup.class,
        data.get(RP_STOCK_COLUMN_GROUPS));

    List<String> columnGroupLabels = new ArrayList<>();
    List<String> columnGroupValues = new ArrayList<>();

    if (columnGroup != null) {
      columnGroupLabels.addAll(Codec.deserializeList(data.get(RP_STOCK_END_COLUMN_LABELS)));
      columnGroupValues.addAll(Codec.deserializeList(data.get(RP_STOCK_END_COLUMN_VALUES)));
    }

    List<String> quantityColumns = NameUtils.toList(data.get(RP_QUANTITY_COLUMNS));
    List<String> amountColumns = NameUtils.toList(data.get(RP_AMOUNT_COLUMNS));

    boolean hasQuantity = !quantityColumns.isEmpty();
    boolean hasAmount = !amountColumns.isEmpty();

    String priceColumn = data.get(RP_PRICE_COLUMN);
    boolean hasPrice = !BeeUtils.isEmpty(priceColumn);

    boolean hasEmptyColumnGroupValue = columnGroup != null
        && (quantityColumns.stream().anyMatch(s -> s.endsWith(EMPTY_VALUE_SUFFIX))
        || amountColumns.stream().anyMatch(s -> s.endsWith(EMPTY_VALUE_SUFFIX)));

    boolean needsRowTotals = columnGroup != null
        && (quantityColumns.size() > 1 || amountColumns.size() > 1);

    HasIndexedWidgets container = getDataContainer();
    if (container == null) {
      return;
    }

    if (!container.isEmpty()) {
      container.clear();
    }

    sheet.clear();

    Map<String, Double> totals = new HashMap<>();
    quantityColumns.forEach(column -> totals.put(column, BeeConst.DOUBLE_ZERO));
    amountColumns.forEach(column -> totals.put(column, BeeConst.DOUBLE_ZERO));

    double totalQuantity = BeeConst.DOUBLE_ZERO;
    double totalAmount = BeeConst.DOUBLE_ZERO;

    double rowQuantity;
    double rowAmount;

    Map<String, Integer> columnIndexes = new HashMap<>();
    int rowTotalColumnIndex = BeeConst.UNDEF;

    int boldRef = sheet.registerFont(XFont.bold());

    HtmlTable table = new HtmlTable(styleTable);
    int r = 0;
    int c = 0;

    XRow xr = new XRow(r);

    XStyle xs = XStyle.center();
    xs.setColor(Colors.LIGHTGRAY);
    xs.setFontRef(boldRef);

    int headerStyleRef = sheet.registerStyle(xs);

    String text;

    if (!rowGroups.isEmpty()) {
      for (TradeReportGroup group : rowGroups) {
        text = group.getCaption();

        table.setText(r, c, text, stylePrefix() + group.getStyleSuffix());
        xr.add(new XCell(c, text, headerStyleRef));

        c++;
      }
    }

    if (hasPrice) {
      ItemPrice itemPrice = parameters.getEnum(RP_ITEM_PRICE, ItemPrice.class);
      text = (itemPrice == null) ? Localized.dictionary().cost() : itemPrice.getCaption();

      table.setText(r, c, text, stylePrice);
      xr.add(new XCell(c, text, headerStyleRef));

      c++;
    }

    if (columnGroup == null) {
      if (hasQuantity) {
        columnIndexes.put(quantityColumns.get(0), c);

        text = Localized.dictionary().quantity();

        table.setText(r, c, text, styleQuantity);
        xr.add(new XCell(c, text, headerStyleRef));

        c++;
      }

      if (hasAmount) {
        columnIndexes.put(amountColumns.get(0), c);

        text = Localized.dictionary().amount();

        table.setText(r, c, text, styleAmount);
        xr.add(new XCell(c, text, headerStyleRef));
      }

    } else {
      if (hasQuantity) {
        for (int i = 0; i < quantityColumns.size(); i++) {
          columnIndexes.put(quantityColumns.get(i), c + i);
        }
      }

      if (hasAmount) {
        for (int i = 0; i < amountColumns.size(); i++) {
          columnIndexes.put(amountColumns.get(i), c + i);
        }
      }

      if (hasEmptyColumnGroupValue) {
        text = BeeUtils.bracket(columnGroup.getCaption());

        table.setText(r, c, text, styleColumnEmptyLabel);
        xr.add(new XCell(c, text, headerStyleRef));

        c++;
      }

      for (int i = 0; i < columnGroupLabels.size(); i++) {
        text = TradeUtils.formatGroupLabel(columnGroup, columnGroupLabels.get(i));

        table.setText(r, c, text, styleColumnLabel);
        xr.add(new XCell(c, text, headerStyleRef));

        if (columnGroup.isEditable() && BeeUtils.isIndex(columnGroupValues, i)) {
          TableCellElement cell = table.getCellFormatter().getElement(r, c);
          String value = columnGroupValues.get(i);

          maybeMakeEditable(cell, columnGroup, value);
        }

        c++;
      }

      if (needsRowTotals) {
        rowTotalColumnIndex = c;

        text = Localized.dictionary().total();

        table.setText(r, c, text, styleRowTotal);
        xr.add(new XCell(c, text, headerStyleRef));
      }
    }

    table.getRowFormatter().addStyleName(r, styleHeader);
    sheet.add(xr);

    r++;

    xs = XStyle.right();
    int numberStyleRef = sheet.registerStyle(xs);

    for (SimpleRowSet.SimpleRow row : rowSet) {
      xr = new XRow(r);
      c = 0;

      if (!rowGroups.isEmpty()) {
        for (int i = 0; i < rowGroups.size(); i++) {
          TradeReportGroup group = rowGroups.get(i);

          String column = BeeUtils.getQuietly(rowGroupLabelColumns, i);
          String label = (column == null) ? null : row.getValue(column);

          text = TradeUtils.formatGroupLabel(group, label);

          table.setText(r, c, text, stylePrefix() + group.getStyleSuffix());
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(c, text));
          }

          if (!BeeUtils.isEmpty(label) && group.isEditable()
              && BeeUtils.isIndex(rowGroupValueColumns, i)) {

            TableCellElement cell = table.getCellFormatter().getElement(r, c);
            String value = row.getValue(rowGroupValueColumns.get(i));

            maybeMakeEditable(cell, group, value);
          }

          c++;
        }
      }

      if (hasPrice) {
        Double value = row.getDouble(priceColumn);
        text = TradeUtils.formatCost(value);

        table.setText(r, c, text, stylePrice);
        if (!BeeUtils.isEmpty(text)) {
          xr.add(new XCell(c, value, numberStyleRef));
        }

        c++;
      }

      rowQuantity = BeeConst.DOUBLE_ZERO;
      rowAmount = BeeConst.DOUBLE_ZERO;

      if (columnGroup == null) {
        if (hasQuantity) {
          Double qty = row.getDouble(quantityColumns.get(0));
          text = TradeUtils.formatQuantity(qty);

          table.setText(r, c, text, styleQuantity);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(c, qty, numberStyleRef));
          }

          c++;
        }

        if (hasAmount) {
          Double amount = row.getDouble(amountColumns.get(0));
          text = TradeUtils.formatAmount(amount);

          table.setText(r, c, text, styleAmount);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(c, Localized.normalizeMoney(amount), numberStyleRef));
          }
        }

      } else if (hasQuantity && hasAmount) {
        for (int i = 0; i < quantityColumns.size(); i++) {
          Double qty = row.getDouble(quantityColumns.get(i));
          text = TradeUtils.formatQuantity(qty);

          table.setText(r, c + i, text, styleQuantity);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(c + i, qty, numberStyleRef));
          }

          rowQuantity += BeeUtils.unbox(qty);
        }

        if (needsRowTotals) {
          text = TradeUtils.formatQuantity(rowQuantity);
          int j = c + quantityColumns.size();

          table.setText(r, j, text, styleQuantity, styleRowTotal);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(j, rowQuantity, numberStyleRef));
          }

          totalQuantity += rowQuantity;
        }

        table.getRowFormatter().addStyleName(r, styleQuantityRow);
        table.getRowFormatter().addStyleName(r, styleBody);
        sheet.add(xr);

        r++;

        xr = new XRow(r);

        for (int i = 0; i < amountColumns.size(); i++) {
          Double amount = row.getDouble(amountColumns.get(i));
          text = TradeUtils.formatAmount(amount);

          table.setText(r, c + i, text, styleAmount);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(c + i, Localized.normalizeMoney(amount), numberStyleRef));
          }

          rowAmount += BeeUtils.unbox(amount);
        }

        if (needsRowTotals) {
          text = TradeUtils.formatAmount(rowAmount);
          int j = c + amountColumns.size();

          table.setText(r, j, text, styleAmount, styleRowTotal);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(j, Localized.normalizeMoney(rowAmount), numberStyleRef));
          }

          totalAmount += rowAmount;
        }

        table.getRowFormatter().addStyleName(r, styleAmountRow);

      } else if (hasQuantity) {
        for (String column : quantityColumns) {
          Double qty = row.getDouble(column);
          text = TradeUtils.formatQuantity(qty);

          table.setText(r, c, text, styleQuantity);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(c, qty, numberStyleRef));
          }

          rowQuantity += BeeUtils.unbox(qty);
          c++;
        }

        if (needsRowTotals) {
          text = TradeUtils.formatQuantity(rowQuantity);

          table.setText(r, c, text, styleQuantity, styleRowTotal);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(c, rowQuantity, numberStyleRef));
          }

          totalQuantity += rowQuantity;
        }

      } else if (hasAmount) {
        for (String column : amountColumns) {
          Double amount = row.getDouble(column);
          text = TradeUtils.formatAmount(amount);

          table.setText(r, c, text, styleAmount);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(c, Localized.normalizeMoney(amount), numberStyleRef));
          }

          rowAmount += BeeUtils.unbox(amount);
          c++;
        }

        if (needsRowTotals) {
          text = TradeUtils.formatAmount(rowAmount);

          table.setText(r, c, text, styleAmount, styleRowTotal);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(c, Localized.normalizeMoney(rowAmount), numberStyleRef));
          }

          totalAmount += rowAmount;
        }
      }

      table.getRowFormatter().addStyleName(r, styleBody);
      sheet.add(xr);

      r++;

      if (hasQuantity) {
        quantityColumns.forEach(column -> {
          Double value = row.getDouble(column);
          if (BeeUtils.nonZero(value)) {
            totals.merge(column, value, Double::sum);
          }
        });
      }

      if (hasAmount) {
        amountColumns.forEach(column -> {
          Double value = row.getDouble(column);
          if (BeeUtils.nonZero(value)) {
            totals.merge(column, value, Double::sum);
          }
        });
      }
    }

    if (rowSet.getNumberOfRows() > 1) {
      xr = new XRow(r);

      xs = XStyle.right();
      xs.setColor(Colors.LIGHTGRAY);
      xs.setFontRef(boldRef);

      int footerStyleRef = sheet.registerStyle(xs);

      int minIndex = columnIndexes.values().stream().mapToInt(i -> i).min().getAsInt();
      if (minIndex > 0) {
        text = Localized.dictionary().totalOf();

        table.setText(r, minIndex - 1, text, styleTotal);
        xr.add(new XCell(minIndex - 1, text, footerStyleRef));
      }

      if (hasQuantity) {
        for (String column : quantityColumns) {
          Double qty = totals.get(column);
          text = TradeUtils.formatQuantity(qty);
          int j = columnIndexes.get(column);

          table.setText(r, j, text, styleQuantity);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(j, qty, footerStyleRef));
          }
        }

        if (needsRowTotals) {
          text = TradeUtils.formatQuantity(totalQuantity);

          table.setText(r, rowTotalColumnIndex, text, styleQuantity, styleRowTotal);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(rowTotalColumnIndex, totalQuantity, footerStyleRef));
          }
        }
      }

      if (columnGroup != null && hasQuantity && hasAmount) {
        table.getRowFormatter().addStyleName(r, styleQuantityRow);
        table.getRowFormatter().addStyleName(r, styleFooter);
        sheet.add(xr);

        r++;

        xr = new XRow(r);
      }

      if (hasAmount) {
        for (String column : amountColumns) {
          Double amount = totals.get(column);
          text = TradeUtils.formatAmount(amount);
          int j = columnIndexes.get(column);

          table.setText(r, j, text, styleAmount);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(j, Localized.normalizeMoney(amount), footerStyleRef));
          }
        }

        if (needsRowTotals) {
          text = TradeUtils.formatAmount(totalAmount);

          table.setText(r, rowTotalColumnIndex, text, styleAmount, styleRowTotal);
          if (!BeeUtils.isEmpty(text)) {
            xr.add(new XCell(rowTotalColumnIndex, Localized.normalizeMoney(totalAmount),
                footerStyleRef));
          }
        }

        if (columnGroup != null && hasQuantity) {
          table.getRowFormatter().addStyleName(r, styleAmountRow);
        }
      }

      table.getRowFormatter().addStyleName(r, styleFooter);
      sheet.add(xr);
    }

    container.add(table);
  }

  protected void maybeMakeEditable(Element cell, TradeReportGroup group, String value) {
    if (DataUtils.isId(value)) {
      DomUtils.setDataProperty(cell, KEY_GROUP, group.ordinal());
      DomUtils.setDataProperty(cell, KEY_ID, value);

      cell.addClassName(styleEditable);

    } else if (!BeeUtils.isEmpty(value)) {
      cell.setTitle(value);
    }
  }

  private void onCellClick(Element cell) {
    if (cell.hasClassName(styleEditable)) {
      TradeReportGroup group = EnumUtils.getEnumByIndex(TradeReportGroup.class,
          DomUtils.getDataProperty(cell, KEY_GROUP));

      Long id = DomUtils.getDataPropertyLong(cell, KEY_ID);

      if (group != null && DataUtils.isId(id)) {
        RowEditor.open(group.editViewName(), id, Opener.MODAL);
      }
    }
  }
}
