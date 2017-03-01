package com.butent.bee.client.modules.trade.acts;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.output.Exporter;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.output.ReportParameters;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.ReportInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XRow;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TradeActStockReport extends ReportInterceptor {

  private static BeeLogger logger = LogUtils.getLogger(TradeActStockReport.class);

  private static final String NAME_START_DATE = "StartDate";
  private static final String NAME_END_DATE = "EndDate";

  private static final List<String> FILTER_NAMES = Arrays.asList(COL_TA_COMPANY, COL_TA_OBJECT,
      COL_WAREHOUSE, COL_CATEGORY, COL_TA_ITEM);

  private static final List<String> GROUP_NAMES = Arrays.asList("Group0", "Group1");
  private static final List<String> GROUP_VALUES = Arrays.asList(COL_ITEM_TYPE, COL_ITEM_GROUP);

  private static final String STYLE_PREFIX = TradeActKeeper.STYLE_PREFIX + "report-stock-";

  private static final String STYLE_TABLE = STYLE_PREFIX + "table";

  private static final String STYLE_HEADER = STYLE_PREFIX + "header";
  private static final String STYLE_BODY = STYLE_PREFIX + "body";
  private static final String STYLE_FOOTER = STYLE_PREFIX + "footer";

  private static final String STYLE_QUANTITY = STYLE_PREFIX + "qty";
  private static final String STYLE_WEIGHT = STYLE_PREFIX + "wgt";

  private static final String STYLE_DATE = STYLE_PREFIX + "date";
  private static final String STYLE_START = STYLE_PREFIX + "start";
  private static final String STYLE_MOVEMENT = STYLE_PREFIX + "movement";
  private static final String STYLE_END = STYLE_PREFIX + "end";

  private static final String KEY_COL_NAME = "cn";

  private static String dropSuffix(String colName) {
    if (isQuantityColumn(colName)) {
      return BeeUtils.removeSuffix(colName, SFX_QUANTITY);
    } else if (isWeightColumn(colName)) {
      return BeeUtils.removeSuffix(colName, SFX_WEIGHT);
    } else {
      return colName;
    }
  }

  private static List<String> getCaptions(DateTime start, DateTime end,
      boolean qty, boolean weight) {

    List<String> captions = new ArrayList<>();

    String h1;
    String h2;

    if (start == null || end == null) {
      h1 = Localized.dictionary().trdRemainders();
      h2 = Format.renderDateLong(BeeUtils.nvl(start, end));

    } else {
      h1 = Localized.dictionary().trdMovementOfGoods();
      h2 = Format.renderPeriod(start, end);
    }

    if (weight) {
      String units;
      if (qty) {
        units = BeeUtils.joinItems(Localized.dictionary().quantity(),
            Localized.dictionary().weight());

      } else {
        units = Localized.dictionary().weight();
      }

      captions.add(BeeUtils.joinWords(h1, BeeUtils.parenthesize(units)));

    } else {
      captions.add(h1);
    }

    if (!BeeUtils.isEmpty(h2)) {
      captions.add(h2);
    }

    return captions;
  }

  private static String getColumnStyle(String colName) {
    if (isQuantityColumn(colName)) {
      return STYLE_QUANTITY;
    } else if (isWeightColumn(colName)) {
      return STYLE_WEIGHT;
    } else {
      return STYLE_PREFIX + colName;
    }
  }

  private static String getOperationLabel(String colName) {
    String s;

    if (isMovementColumn(colName)) {
      s = BeeUtils.removePrefix(colName, PFX_MOVEMENT);
    } else {
      s = colName;
    }

    String id = dropSuffix(s);
    if (DataUtils.isId(id)) {
      return TradeActKeeper.getOperationName(BeeUtils.toLong(id));
    } else {
      logger.warning("cannot parse operation", colName);
      return id;
    }
  }

  private static String getWarehouseLabel(String colName) {
    String s;

    if (isStartColumn(colName)) {
      s = BeeUtils.removePrefix(colName, PFX_START_STOCK);
    } else if (isEndColumn(colName)) {
      s = BeeUtils.removePrefix(colName, PFX_END_STOCK);
    } else {
      s = colName;
    }

    String id = dropSuffix(s);
    if (DataUtils.isId(id)) {
      return TradeActKeeper.getWarehouseCode(BeeUtils.toLong(id));
    } else {
      logger.warning("cannot parse warehouse", colName);
      return id;
    }
  }

  private static boolean isEndColumn(String name) {
    return name.startsWith(PFX_END_STOCK);
  }

  private static boolean isMovementColumn(String name) {
    return name.startsWith(PFX_MOVEMENT);
  }

  private static boolean isQuantityColumn(String name) {
    return name.endsWith(SFX_QUANTITY);
  }

  private static boolean isStartColumn(String name) {
    return name.startsWith(PFX_START_STOCK);
  }

  private static boolean isWeightColumn(String name) {
    return name.endsWith(SFX_WEIGHT);
  }

  private final Map<String, Filter> filters = new HashMap<>();

  private final XSheet sheet = new XSheet();

  public TradeActStockReport() {
  }

  @Override
  public FormInterceptor getInstance() {
    return new TradeActStockReport();
  }

  @Override
  public void onLoad(FormView form) {
    ReportParameters parameters = readParameters();

    if (parameters != null) {
      loadDateTime(parameters, NAME_START_DATE, form);
      loadDateTime(parameters, NAME_END_DATE, form);

      loadBoolean(parameters, COL_TRADE_ITEM_QUANTITY, form);
      loadBoolean(parameters, COL_ITEM_WEIGHT, form);

      loadMulti(parameters, FILTER_NAMES, form);

      loadGroupBy(parameters, GROUP_NAMES, form);
    }

    super.onLoad(form);
  }

  @Override
  public void onUnload(FormView form) {
    storeDateTimeValues(NAME_START_DATE, NAME_END_DATE);
    storeBooleanValues(COL_TRADE_ITEM_QUANTITY, COL_ITEM_WEIGHT);

    storeEditorValues(FILTER_NAMES);

    storeGroupBy(GROUP_NAMES);
  }

  @Override
  protected void clearFilter() {
    clearEditor(NAME_START_DATE);
    clearEditor(NAME_END_DATE);

    for (String name : FILTER_NAMES) {
      clearEditor(name);
    }
  }

  @Override
  protected void doReport() {
    final DateTime start = getDateTime(NAME_START_DATE);
    final DateTime end = getDateTime(NAME_END_DATE);

    if (!checkRange(start, end)) {
      return;
    }

    boolean qty = getBoolean(COL_TRADE_ITEM_QUANTITY);
    boolean weight = getBoolean(COL_ITEM_WEIGHT);

    final List<String> headers = StringList.uniqueCaseSensitive();
    headers.addAll(getCaptions(start, end, qty, weight));

    ParameterList params = TradeActKeeper.createArgs(SVC_STOCK_REPORT);
    filters.clear();

    if (start != null) {
      params.addDataItem(Service.VAR_FROM, start.getTime());
      filters.put(NAME_START_DATE, Filter.isMoreEqual(COL_TA_DATE, new DateTimeValue(start)));
    }
    if (end != null) {
      params.addDataItem(Service.VAR_TO, end.getTime());
      filters.put(NAME_END_DATE, Filter.isLess(COL_TA_DATE, new DateTimeValue(end)));
    }

    if (qty) {
      params.addDataItem(COL_TRADE_ITEM_QUANTITY, Codec.pack(qty));
    }
    if (weight) {
      params.addDataItem(COL_ITEM_WEIGHT, Codec.pack(weight));
    }

    Filter filter;

    for (String name : FILTER_NAMES) {
      String ids = getEditorValue(name);

      if (!BeeUtils.isEmpty(ids)) {
        params.addDataItem(name, ids);

        boolean plural = DataUtils.parseIdSet(ids).size() > 1;
        String label = TradeActHelper.getLabel(name, plural);
        if (BeeUtils.isEmpty(label)) {
          logger.warning(name, "has no label");
        }

        headers.add(BeeUtils.joinWords(label, getFilterLabel(name)));

        Set<Long> values = DataUtils.parseIdSet(ids);

        switch (name) {
          case COL_WAREHOUSE:
            filter = Filter.or(Filter.any(COL_OPERATION_WAREHOUSE_FROM, values),
                Filter.any(COL_OPERATION_WAREHOUSE_TO, values));
            break;

          case COL_CATEGORY:
            filter = Filter.in(COL_TA_ITEM, VIEW_ITEM_CATEGORIES, COL_ITEM,
                Filter.any(COL_CATEGORY, values));
            break;

          default:
            filter = Filter.any(name, values);
        }

        filters.put(name, filter);
      }
    }

    List<String> groupBy = getGroupBy(GROUP_NAMES, GROUP_VALUES);
    if (!groupBy.isEmpty()) {
      params.addDataItem(Service.VAR_GROUP_BY, NameUtils.join(groupBy));
    }

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(final ResponseObject response) {
        if (response.hasMessages()) {
          response.notify(getFormView());
        }

        if (response.hasResponse(SimpleRowSet.class)) {
          TradeActKeeper.ensureChache(() -> {
            renderData(SimpleRowSet.restore(response.getResponseAsString()), start, end);

            sheet.addHeaders(headers);
            sheet.autoSizeAll();
          });

        } else {
          getFormView().notifyWarning(Localized.dictionary().nothingFound());
        }
      }
    });
  }

  @Override
  protected void export() {
    if (!sheet.isEmpty()) {
      DateTime start = getDateTime(NAME_START_DATE);
      DateTime end = getDateTime(NAME_END_DATE);

      String fileName;
      if (start == null || end == null) {
        fileName = Localized.dictionary().trdRemainders();
      } else {
        fileName = Localized.dictionary().trdMovementOfGoods();
      }

      Exporter.maybeExport(sheet, fileName);
    }
  }

  @Override
  protected String getBookmarkLabel() {
    DateTime start = getDateTime(NAME_START_DATE);
    DateTime end = getDateTime(NAME_END_DATE);

    List<String> captions = getCaptions(start, end, getBoolean(COL_TRADE_ITEM_QUANTITY),
        getBoolean(COL_ITEM_WEIGHT));

    List<String> labels = StringList.uniqueCaseSensitive();
    labels.addAll(captions);

    for (String name : FILTER_NAMES) {
      labels.add(getFilterLabel(name));
    }

    for (String groupName : GROUP_NAMES) {
      labels.add(getGroupByLabel(groupName));
    }

    return BeeUtils.joinWords(labels);
  }

  @Override
  protected Report getReport() {
    return Report.TRADE_ACT_STOCK;
  }

  @Override
  protected ReportParameters getReportParameters() {
    ReportParameters parameters = new ReportParameters();

    addDateTimeValues(parameters, NAME_START_DATE, NAME_END_DATE);
    addBooleanValues(parameters, COL_TRADE_ITEM_QUANTITY, COL_ITEM_WEIGHT);

    addEditorValues(parameters, FILTER_NAMES);
    addGroupBy(parameters, GROUP_NAMES);

    return parameters;
  }

  @Override
  protected boolean validateParameters(ReportParameters parameters) {
    DateTime start = parameters.getDateTime(NAME_START_DATE);
    DateTime end = parameters.getDateTime(NAME_END_DATE);

    return checkRange(start, end);
  }

  private void renderData(SimpleRowSet data, DateTime start, DateTime end) {
    HasIndexedWidgets container = getDataContainer();
    if (container == null) {
      return;
    }

    sheet.clear();

    if (!container.isEmpty()) {
      container.clear();
    }

    List<ValueType> types =
        TradeActHelper.getTypes(Arrays.asList(VIEW_TRADE_ACT_ITEMS, VIEW_ITEMS), data);

    Multimap<Integer, String> columnStyles = ArrayListMultimap.create();

    Map<Integer, Double> totals = new HashMap<>();

    boolean hasItem = data.hasColumn(COL_TA_ITEM);

    List<Integer> startColumns = new ArrayList<>();
    List<Integer> movementColumns = new ArrayList<>();
    List<Integer> endColumns = new ArrayList<>();

    List<Integer> quantityColumns = new ArrayList<>();
    List<Integer> weightColumns = new ArrayList<>();

    for (int j = 0; j < data.getNumberOfColumns(); j++) {
      String colName = data.getColumnName(j);
      columnStyles.put(j, getColumnStyle(colName));

      if (isStartColumn(colName)) {
        startColumns.add(j);
        columnStyles.put(j, STYLE_START);

      } else if (isMovementColumn(colName)) {
        movementColumns.add(j);
        columnStyles.put(j, STYLE_MOVEMENT);

      } else if (isEndColumn(colName)) {
        endColumns.add(j);
        columnStyles.put(j, STYLE_END);
      }

      if (isQuantityColumn(colName)) {
        quantityColumns.add(j);
        totals.put(j, BeeConst.DOUBLE_ZERO);

      } else if (isWeightColumn(colName)) {
        weightColumns.add(j);
        totals.put(j, BeeConst.DOUBLE_ZERO);
      }
    }

    int headerSpan = 1;
    if (start != null && !startColumns.isEmpty() || end != null && !endColumns.isEmpty()) {
      headerSpan++;
    }

    int boldRef = sheet.registerFont(XFont.bold());
    String text;

    HtmlTable table = new HtmlTable(STYLE_TABLE);
    int r = 0;

    XRow xr = new XRow(r);
    XRow h2 = (headerSpan > 1) ? new XRow(r + 1) : null;

    XStyle xs = XStyle.center();
    xs.setColor(Colors.LIGHTGRAY);
    xs.setFontRef(boldRef);

    int headerStyleRef = sheet.registerStyle(xs);

    XCell xc;
    int c1 = 0;
    int c2 = 0;

    for (int j = 0; j < data.getNumberOfColumns(); j++) {
      String colName = data.getColumnName(j);

      if (weightColumns.contains(j) && !quantityColumns.isEmpty()) {
        text = Localized.dictionary().kilogramShort();

      } else if (quantityColumns.contains(j) || weightColumns.contains(j)) {
        if (movementColumns.contains(j)) {
          text = getOperationLabel(colName);
        } else {
          text = getWarehouseLabel(colName);
        }

      } else {
        text = TradeActHelper.getLabel(colName);
      }

      xc = new XCell(j, text, headerStyleRef);

      if (h2 == null) {
        table.setText(r, j, text, columnStyles.get(j));
        xr.add(xc);

      } else if (startColumns.contains(j) && start != null) {
        if (startColumns.indexOf(j) == 0) {
          String dateText = Format.renderDateLong(start);
          int dateSpan = startColumns.size();

          table.setText(r, c1, dateText, STYLE_START, STYLE_DATE);
          XCell dateCell = new XCell(j, dateText, headerStyleRef);

          if (dateSpan > 1) {
            table.getCellFormatter().setColSpan(r, c1, dateSpan);
            dateCell.setColSpan(dateSpan);
          }

          xr.add(dateCell);
          c1++;
        }

        table.setText(r + 1, c2++, text, columnStyles.get(j));
        h2.add(xc);

      } else if (endColumns.contains(j) && end != null) {
        if (endColumns.indexOf(j) == 0) {
          String dateText = Format.renderDateLong(end);
          int dateSpan = endColumns.size();

          table.setText(r, c1, dateText, STYLE_END, STYLE_DATE);
          XCell dateCell = new XCell(j, dateText, headerStyleRef);

          if (dateSpan > 1) {
            table.getCellFormatter().setColSpan(r, c1, dateSpan);
            dateCell.setColSpan(dateSpan);
          }

          xr.add(dateCell);
          c1++;
        }

        table.setText(r + 1, c2++, text, columnStyles.get(j));
        h2.add(xc);

      } else {
        table.setText(r, c1, text, columnStyles.get(j));
        table.getCellFormatter().setRowSpan(r, c1, headerSpan);

        xc.setRowSpan(headerSpan);
        xr.add(xc);

        c1++;
      }
    }

    table.getRowFormatter().addStyleName(r, STYLE_HEADER);
    sheet.add(xr);

    if (h2 != null) {
      table.getRowFormatter().addStyleName(r + 1, STYLE_HEADER);
      sheet.add(h2);
    }

    r += headerSpan;

    xs = XStyle.right();
    int numberStyleRef = sheet.registerStyle(xs);

    String rightAlign = StyleUtils.className(TextAlign.RIGHT);
    for (int j = 0; j < data.getNumberOfColumns(); j++) {
      if (ValueType.isNumeric(types.get(j))) {
        columnStyles.put(j, rightAlign);
      }
    }

    boolean export;

    for (int i = 0; i < data.getNumberOfRows(); i++) {
      xr = new XRow(r);

      for (int j = 0; j < data.getNumberOfColumns(); j++) {
        String colName = data.getColumnName(j);
        ValueType type = types.get(j);

        text = null;

        if (ValueType.isNumeric(type)) {
          Double value = data.getDouble(i, j);

          if (BeeUtils.nonZero(value)) {
            NumberFormat format;

            if (quantityColumns.contains(j)) {
              format = TradeActHelper.getQuantityFormat();
            } else if (weightColumns.contains(j)) {
              format = TradeActHelper.getWeightFormat();
            } else {
              format = TradeActHelper.getNumberFormat(colName);
            }

            text = (format == null) ? data.getValue(i, j) : format.format(value);

            if (totals.containsKey(j)) {
              totals.put(j, totals.get(j) + value);
            }

            xr.add(new XCell(j, value, numberStyleRef));
          }

          export = false;

        } else {
          if (ValueType.DATE_TIME == type) {
            text = Format.renderDateTime(data.getDateTime(i, j));

          } else if (ValueType.DATE == type) {
            text = Format.renderDate(data.getDate(i, j));

          } else {
            text = data.getValue(i, j);
          }

          export = !BeeUtils.isEmpty(text);
        }

        table.setText(r, j, text, columnStyles.get(j));
        DomUtils.setDataProperty(table.getCellFormatter().getElement(r, j), KEY_COL_NAME, colName);

        if (export) {
          xr.add(new XCell(j, text));
        }
      }

      table.getRowFormatter().addStyleName(r, STYLE_BODY);
      if (hasItem) {
        DomUtils.setDataIndex(table.getRow(r), data.getLong(i, COL_TA_ITEM));
      }

      sheet.add(xr);

      r++;
    }

    if (data.getNumberOfRows() > 1 && !totals.isEmpty()) {
      xr = new XRow(r);

      xs = XStyle.right();
      xs.setFontRef(boldRef);

      int footerStyleRef = sheet.registerStyle(xs);

      List<Integer> indexes = new ArrayList<>(totals.keySet());
      Collections.sort(indexes);

      Integer first = indexes.get(0);
      if (BeeUtils.isPositive(first)) {
        text = Localized.dictionary().totalOf();

        table.setText(r, first - 1, text);

        xs = new XStyle();
        xs.setFontRef(boldRef);
        xr.add(new XCell(first - 1, text, sheet.registerStyle(xs)));
      }

      for (int index : indexes) {
        String colName = data.getColumnName(index);
        Double value = totals.get(index);

        NumberFormat format;
        if (quantityColumns.contains(index)) {
          format = TradeActHelper.getQuantityFormat();
        } else if (weightColumns.contains(index)) {
          format = TradeActHelper.getWeightFormat();
        } else {
          format = TradeActHelper.getNumberFormat(colName);
        }

        text = (format == null) ? BeeUtils.toString(value) : format.format(value);

        table.setText(r, index, text, columnStyles.get(index));
        DomUtils.setDataProperty(table.getCellFormatter().getElement(r, index),
            KEY_COL_NAME, colName);

        xr.add(new XCell(index, text, footerStyleRef));
      }

      table.getRowFormatter().addStyleName(r, STYLE_FOOTER);
      sheet.add(xr);
    }

    if (hasItem || !movementColumns.isEmpty()) {
      final List<String> itemClasses = Arrays.asList(getColumnStyle(COL_TA_ITEM),
          getColumnStyle(ALS_ITEM_NAME), getColumnStyle(COL_ITEM_ARTICLE));
      final List<String> movementRowClasses = Arrays.asList(STYLE_BODY, STYLE_FOOTER);

      final String period = Format.renderPeriod(start, end);

      table.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Element target = EventUtils.getEventTargetElement(event);

          TableCellElement cell = DomUtils.getParentCell(target, true);
          TableRowElement row = DomUtils.getParentRow(cell, false);

          if (StyleUtils.hasClassName(cell, STYLE_MOVEMENT)
              && StyleUtils.hasAnyClass(row, movementRowClasses)) {

            String colName = DomUtils.getDataProperty(cell, KEY_COL_NAME);
            if (BeeUtils.allNotEmpty(colName, cell.getInnerText())) {
              showMovement(period, row, colName);
            }

          } else if (StyleUtils.hasAnyClass(cell, itemClasses)) {
            long id = DomUtils.getDataIndexLong(row);
            if (DataUtils.isId(id)) {
              RowEditor.open(VIEW_ITEMS, id, Opener.MODAL);
            }
          }
        }
      });
    }

    container.add(table);
  }

  private void showMovement(String period, TableRowElement row, String colName) {
    Long operation = BeeUtils.toLongOrNull(dropSuffix(BeeUtils.removePrefix(colName,
        PFX_MOVEMENT)));
    if (!DataUtils.isId(operation)) {
      logger.warning("cannot parse operation", colName);
      return;
    }

    List<String> captions = new ArrayList<>();
    CompoundFilter filter = Filter.and();

    filter.add(Filter.equals(COL_TA_OPERATION, operation));

    captions.add(TradeActKeeper.getOperationName(operation));
    captions.add(period);

    Long item = null;
    String type = null;
    String group = null;

    if (StyleUtils.hasClassName(row, STYLE_BODY)) {
      NodeList<TableCellElement> cells = row.getCells();
      for (int i = 0; i < cells.getLength(); i++) {
        TableCellElement cell = cells.getItem(i);
        String name = DomUtils.getDataProperty(cell, KEY_COL_NAME);

        if (!BeeUtils.isEmpty(name)) {
          switch (name) {
            case COL_TA_ITEM:
              item = BeeUtils.toLongOrNull(cell.getInnerText());
              if (DataUtils.isId(item)) {
                filter.add(Filter.equals(COL_TA_ITEM, item));
              }
              break;

            case ALS_ITEM_TYPE_NAME:
              type = Strings.nullToEmpty(cell.getInnerText());
              break;

            case ALS_ITEM_GROUP_NAME:
              group = Strings.nullToEmpty(cell.getInnerText());
              break;

            case ALS_ITEM_NAME:
            case COL_ITEM_ARTICLE:
              String text = cell.getInnerText();
              if (!BeeUtils.isEmpty(text)) {
                captions.add(text);
              }
              break;
          }
        }
      }
    }

    for (Map.Entry<String, Filter> entry : filters.entrySet()) {
      if (!COL_WAREHOUSE.equals(entry.getKey())
          && (item == null || !BeeUtils.inList(entry.getKey(), COL_CATEGORY, COL_TA_ITEM))) {
        filter.add(entry.getValue());
      }
    }

    if (item == null) {
      if (type != null) {
        if (type.isEmpty()) {
          filter.add(Filter.isNull(ALS_ITEM_TYPE_NAME));
        } else {
          filter.add(Filter.equals(ALS_ITEM_TYPE_NAME, type));
          captions.add(type);
        }
      }

      if (group != null) {
        if (group.isEmpty()) {
          filter.add(Filter.isNull(ALS_ITEM_GROUP_NAME));
        } else {
          filter.add(Filter.equals(ALS_ITEM_GROUP_NAME, group));
          captions.add(group);
        }
      }

      if (isWeightColumn(colName)) {
        filter.add(Filter.isPositive(COL_ITEM_WEIGHT));
      }
    }

    drillDown(GRID_TRADE_ACTS_AND_ITEMS, BeeUtils.joinWords(captions), filter);
  }
}
