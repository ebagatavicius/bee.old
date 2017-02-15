package com.butent.bee.client.modules.trade.acts;

import com.google.gwt.dom.client.Element;
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
import com.butent.bee.client.data.ClientDefaults;
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
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XRow;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.trade.acts.TradeActTimeUnit;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeActTransferReport extends ReportInterceptor {

  private static BeeLogger logger = LogUtils.getLogger(TradeActTransferReport.class);

  private static final String NAME_START_DATE = "StartDate";
  private static final String NAME_END_DATE = "EndDate";

  private static final String NAME_CURRENCY = COL_TA_CURRENCY;

  private static final List<String> FILTER_NAMES = Arrays.asList(COL_TA_COMPANY, COL_TA_MANAGER,
      COL_CATEGORY, COL_TA_ITEM);

  private static final List<String> GROUP_NAMES =
      Arrays.asList("Group0", "Group1", "Group2", "Group3");
  private static final List<String> GROUP_VALUES = Arrays.asList(COL_TA_COMPANY, COL_TA_MANAGER,
      COL_ITEM_TYPE, COL_ITEM_GROUP, COL_TA_ITEM);

  private static final List<String> TOTAL_COLUMNS = Arrays.asList(COL_TRADE_ITEM_QUANTITY,
      ALS_BASE_AMOUNT);

  private static final List<String> MONEY_COLUMNS = Arrays.asList(COL_TRADE_ITEM_PRICE,
      ALS_BASE_AMOUNT);

  private static final String STYLE_PREFIX = TradeActKeeper.STYLE_PREFIX + "report-trf-";

  private static final String STYLE_TABLE = STYLE_PREFIX + "table";

  private static final String STYLE_HEADER = STYLE_PREFIX + "header";
  private static final String STYLE_BODY = STYLE_PREFIX + "body";
  private static final String STYLE_FOOTER = STYLE_PREFIX + "footer";

  private static final String KEY_ACT = "act";
  private static final String KEY_SERVICE = "svc";

  private static String getColumnStyle(String colName) {
    return STYLE_PREFIX + colName;
  }

  private final XSheet sheet = new XSheet();

  public TradeActTransferReport() {
  }

  @Override
  public FormInterceptor getInstance() {
    return new TradeActTransferReport();
  }

  @Override
  public void onLoad(FormView form) {
    ReportParameters parameters = readParameters();

    if (parameters != null) {
      loadDateTime(parameters, NAME_START_DATE, form);
      loadDateTime(parameters, NAME_END_DATE, form);

      loadId(parameters, NAME_CURRENCY, form);

      loadMulti(parameters, FILTER_NAMES, form);

      loadGroupBy(parameters, GROUP_NAMES, form);
    }

    super.onLoad(form);
  }

  @Override
  public void onUnload(FormView form) {
    storeDateTimeValues(NAME_START_DATE, NAME_END_DATE);
    storeEditorValues(NAME_CURRENCY);

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
    DateTime start = getDateTime(NAME_START_DATE);
    DateTime end = getDateTime(NAME_END_DATE);

    if (start == null) {
      getFormView().notifyWarning(Localized.dictionary()
          .fieldRequired(Localized.dictionary().dateFrom()));
      getFormView().focus(NAME_START_DATE);
      return;
    }
    if (end == null) {
      getFormView().notifyWarning(Localized.dictionary()
          .fieldRequired(Localized.dictionary().dateTo()));
      getFormView().focus(NAME_END_DATE);
      return;
    }

    if (!checkRange(start, end)) {
      return;
    }

    ParameterList params = TradeActKeeper.createArgs(SVC_TRANSFER_REPORT);
    final List<String> headers = StringList.of(getReportCaption());

    params.addDataItem(Service.VAR_FROM, start.getTime());
    params.addDataItem(Service.VAR_TO, end.getTime());

    headers.add(Format.renderPeriod(start, end));

    String currency = getEditorValue(NAME_CURRENCY);
    final String currencyName;

    if (DataUtils.isId(currency)) {
      params.addDataItem(COL_TA_CURRENCY, currency);
      currencyName = getFilterLabel(NAME_CURRENCY);
    } else {
      currencyName = ClientDefaults.getCurrencyName();
    }

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
      }
    }

    List<String> groupBy = getGroupBy(GROUP_NAMES, GROUP_VALUES);
    if (!groupBy.isEmpty()) {
      params.addDataItem(Service.VAR_GROUP_BY, NameUtils.join(groupBy));
    }

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasMessages()) {
          response.notify(getFormView());
        }

        if (response.hasResponse(SimpleRowSet.class)) {
          renderData(SimpleRowSet.restore(response.getResponseAsString()), currencyName);

          sheet.addHeaders(headers);
          sheet.autoSizeAll();

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
    List<String> labels = StringList.of(getReportCaption(),
        Format.renderPeriod(getDateTime(NAME_START_DATE), getDateTime(NAME_END_DATE)),
        getFilterLabel(NAME_CURRENCY));

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
    return Report.TRADE_ACT_TRANSFER;
  }

  @Override
  protected ReportParameters getReportParameters() {
    ReportParameters parameters = new ReportParameters();

    addDateTimeValues(parameters, NAME_START_DATE, NAME_END_DATE);
    addEditorValues(parameters, NAME_CURRENCY);

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

  private void renderData(SimpleRowSet data, String currencyName) {
    HasIndexedWidgets container = getDataContainer();
    if (container == null) {
      return;
    }

    sheet.clear();

    if (!container.isEmpty()) {
      container.clear();
    }

    List<String> viewNames = Arrays.asList(VIEW_TRADE_ACTS, VIEW_TRADE_ACT_SERVICES);
    List<ValueType> types = TradeActHelper.getTypes(viewNames, data);

    Map<Integer, Double> totals = new HashMap<>();

    final boolean hasAct = data.hasColumn(COL_TRADE_ACT);
    final boolean hasService = data.hasColumn(COL_TA_ITEM);

    int boldRef = sheet.registerFont(XFont.bold());
    String text;

    HtmlTable table = new HtmlTable(STYLE_TABLE);
    int r = 0;

    XRow xr = new XRow(r);

    XStyle xs = XStyle.center();
    xs.setColor(Colors.LIGHTGRAY);
    xs.setFontRef(boldRef);

    int headerStyleRef = sheet.registerStyle(xs);

    for (int j = 0; j < data.getNumberOfColumns(); j++) {
      String colName = data.getColumnName(j);

      if (COL_TA_ITEM.equals(colName)) {
        text = Localized.dictionary().service();
      } else if (MONEY_COLUMNS.contains(colName)) {
        text = BeeUtils.joinWords(TradeActHelper.getLabel(colName), currencyName);
      } else {
        text = TradeActHelper.getLabel(colName);
      }

      table.setText(r, j, text, getColumnStyle(colName));
      xr.add(new XCell(j, text, headerStyleRef));

      if (TOTAL_COLUMNS.contains(colName)) {
        totals.put(j, BeeConst.DOUBLE_ZERO);
      }
    }

    table.getRowFormatter().addStyleName(r, STYLE_HEADER);
    sheet.add(xr);

    r++;

    String styleName;
    String styleRightAlign = StyleUtils.className(TextAlign.RIGHT);

    xs = XStyle.right();
    int numberStyleRef = sheet.registerStyle(xs);

    xs = XStyle.right();
    xs.setFormat(AMOUNT_PATTERN);
    int moneyStyleRef = sheet.registerStyle(xs);

    boolean export;

    for (int i = 0; i < data.getNumberOfRows(); i++) {
      xr = new XRow(r);

      for (int j = 0; j < data.getNumberOfColumns(); j++) {
        String colName = data.getColumnName(j);
        ValueType type = types.get(j);

        text = null;
        styleName = null;

        if (COL_TIME_UNIT.equals(colName)) {
          TradeActTimeUnit tu = EnumUtils.getEnumByIndex(TradeActTimeUnit.class, data.getInt(i, j));

          text = (tu == null) ? null : tu.getCaption();
          export = tu != null;

        } else if (ValueType.isNumeric(type)) {
          Double value = data.getDouble(i, j);
          NumberFormat format = TradeActHelper.getNumberFormat(colName);

          if (BeeUtils.isDouble(value)) {
            text = (format == null) ? data.getValue(i, j) : format.format(value);
            styleName = styleRightAlign;

            if (totals.containsKey(j)) {
              totals.put(j, totals.get(j) + value);
            }

            int styleRef = MONEY_COLUMNS.contains(colName) ? moneyStyleRef : numberStyleRef;
            xr.add(new XCell(j, value, styleRef));
          }

          export = false;

        } else {
          if (ValueType.DATE_TIME == type
              || COL_TA_SERVICE_FROM.equals(colName) || COL_TA_SERVICE_TO.equals(colName)) {
            text = TimeUtils.renderCompact(data.getDateTime(i, j));

          } else if (ValueType.DATE == type) {
            text = Format.renderDate(data.getDate(i, j));

          } else {
            text = data.getValue(i, j);
          }

          export = !BeeUtils.isEmpty(text);
        }

        table.setText(r, j, text, getColumnStyle(colName), styleName);
        if (export) {
          xr.add(new XCell(j, text));
        }
      }

      table.getRowFormatter().addStyleName(r, STYLE_BODY);

      if (hasAct) {
        DomUtils.setDataProperty(table.getRow(r), KEY_ACT, data.getLong(i, COL_TRADE_ACT));
      }
      if (hasService) {
        DomUtils.setDataProperty(table.getRow(r), KEY_SERVICE, data.getLong(i, COL_TA_ITEM));
      }

      sheet.add(xr);

      r++;
    }

    if (data.getNumberOfRows() > 1 && !totals.isEmpty()) {
      xr = new XRow(r);

      xs = XStyle.right();
      xs.setFontRef(boldRef);
      xs.setFormat(AMOUNT_PATTERN);

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

        NumberFormat format = TradeActHelper.getNumberFormat(colName);
        text = (format == null) ? BeeUtils.toString(value) : format.format(value);

        table.setText(r, index, text, getColumnStyle(colName), styleRightAlign);
        xr.add(new XCell(index, text, footerStyleRef));
      }

      table.getRowFormatter().addStyleName(r, STYLE_FOOTER);
      sheet.add(xr);
    }

    if (hasAct || hasService) {
      final List<String> actClasses = Arrays.asList(getColumnStyle(COL_TRADE_ACT),
          getColumnStyle(COL_TRADE_ACT_NAME), getColumnStyle(COL_TA_NUMBER));
      final List<String> serviceClasses = Arrays.asList(getColumnStyle(COL_TA_ITEM),
          getColumnStyle(ALS_ITEM_NAME), getColumnStyle(COL_ITEM_ARTICLE));

      table.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Element target = EventUtils.getEventTargetElement(event);

          TableCellElement cell = DomUtils.getParentCell(target, true);
          TableRowElement row = DomUtils.getParentRow(cell, false);

          if (hasAct && StyleUtils.hasAnyClass(cell, actClasses)) {
            long actId = DomUtils.getDataPropertyLong(row, KEY_ACT);
            if (DataUtils.isId(actId)) {
              RowEditor.open(VIEW_TRADE_ACTS, actId, Opener.MODAL);
            }

          } else if (hasService && StyleUtils.hasAnyClass(cell, serviceClasses)) {
            long itemId = DomUtils.getDataPropertyLong(row, KEY_SERVICE);
            if (DataUtils.isId(itemId)) {
              RowEditor.open(VIEW_ITEMS, itemId, Opener.MODAL);
            }
          }
        }
      });
    }

    container.add(table);
  }
}
