package com.butent.bee.client.modules.trade.reports;

import com.google.gwt.dom.client.TableCellElement;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XRow;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.trade.TradeMovementColumn;
import com.butent.bee.shared.modules.trade.TradeReportGroup;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TradeMovementOfGoodsReport extends TradeStockReport {

  private final String styleStart = stylePrefix() + "start";
  private final String styleEnd = stylePrefix() + "end";

  private final String styleIn = stylePrefix() + "in";
  private final String styleOut = stylePrefix() + "out";

  public TradeMovementOfGoodsReport() {
  }

  @Override
  public FormInterceptor getInstance() {
    return new TradeMovementOfGoodsReport();
  }

  @Override
  public void onLoad(FormView form) {
    ReportParameters parameters = readParameters();

    if (parameters != null) {
      loadDateTime(parameters, RP_START_DATE, form);
      loadDateTime(parameters, RP_END_DATE, form);

      commonLoad(parameters, form);

      loadListByValue(parameters, RP_MOVEMENT_COLUMNS, form);
    }

    super.onLoad(form);
  }

  @Override
  public void onUnload(FormView form) {
    storeDateTimeValues(RP_START_DATE, RP_END_DATE);
    commonStore();
    storeSelectedValue(RP_MOVEMENT_COLUMNS, 1);
  }

  @Override
  protected void clearFilter() {
    commonClearFilter();
  }

  @Override
  protected String getService() {
    return SVC_TRADE_MOVEMENT_OF_GOODS_REPORT;
  }

  @Override
  protected Report getReport() {
    return Report.TRADE_MOVEMENT_OF_GOODS;
  }

  @Override
  protected String stylePrefix() {
    return TradeKeeper.STYLE_PREFIX + "report-movement-of-goods-";
  }

  @Override
  protected ReportParameters getReportParameters() {
    ReportParameters parameters = new ReportParameters();

    addDateTimeValues(parameters, RP_START_DATE, RP_END_DATE);
    addCommonParameters(parameters);
    addSelectedValue(parameters, RP_MOVEMENT_COLUMNS, 1);

    return parameters;
  }

  @Override
  protected boolean validateParameters(ReportParameters parameters) {
    DateTime start = parameters.getDateTime(RP_START_DATE);
    DateTime end = parameters.getDateTime(RP_END_DATE);

    return checkRequired(Localized.dictionary().dateFrom(), start)
        && checkRequired(Localized.dictionary().dateTo(), end)
        && checkRange(start, end)
        && super.validateParameters(parameters);
  }

  @Override
  protected String getDateCaption() {
    return Format.renderPeriod(getDateTime(RP_START_DATE), getDateTime(RP_END_DATE));
  }

  @Override
  protected List<String> getLabels(boolean addGrouping) {
    List<String> labels = super.getLabels(addGrouping);

    if (addGrouping && BeeUtils.isPositive(getSelectedIndex(RP_MOVEMENT_COLUMNS))) {
      labels.add(BeeUtils.joinWords(Localized.dictionary().trdReportColumnsMovement(),
          getSelectedItemText(RP_MOVEMENT_COLUMNS)));
    }

    return labels;
  }

  @Override
  protected String getExportFileName() {
    return Localized.dictionary().trdMovement();
  }

  @Override
  protected void render(Map<String, String> data) {
    SimpleRowSet rowSet = SimpleRowSet.restore(data.get(Service.VAR_DATA));
    if (DataUtils.isEmpty(rowSet)) {
      getFormView().notifySevere(Localized.dictionary().keyNotFound(Service.VAR_DATA));
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

    TradeReportGroup stockGroup = EnumUtils.getEnumByIndex(TradeReportGroup.class,
        data.get(RP_STOCK_COLUMN_GROUPS));

    List<String> stockStartLabels = new ArrayList<>();
    List<String> stockStartValues = new ArrayList<>();

    List<String> stockEndLabels = new ArrayList<>();
    List<String> stockEndValues = new ArrayList<>();

    if (stockGroup != null) {
      stockStartLabels.addAll(Codec.deserializeList(data.get(RP_STOCK_START_COLUMN_LABELS)));
      stockStartValues.addAll(Codec.deserializeList(data.get(RP_STOCK_START_COLUMN_VALUES)));

      stockEndLabels.addAll(Codec.deserializeList(data.get(RP_STOCK_END_COLUMN_LABELS)));
      stockEndValues.addAll(Codec.deserializeList(data.get(RP_STOCK_END_COLUMN_VALUES)));
    }

    List<TradeMovementColumn> movementInColumns =
        TradeMovementColumn.restoreList(data.get(RP_MOVEMENT_IN_COLUMNS));
    List<TradeMovementColumn> movementOutColumns =
        TradeMovementColumn.restoreList(data.get(RP_MOVEMENT_OUT_COLUMNS));

    List<String> quantityColumns = NameUtils.toList(data.get(RP_QUANTITY_COLUMNS));
    List<String> amountColumns = NameUtils.toList(data.get(RP_AMOUNT_COLUMNS));

    boolean hasQuantity = !quantityColumns.isEmpty();
    boolean hasAmount = !amountColumns.isEmpty();

    String priceColumn = data.get(RP_PRICE_COLUMN);
    boolean hasPrice = !BeeUtils.isEmpty(priceColumn);

    List<String> startQuantityColumns = new ArrayList<>();
    List<String> startAmountColumns = new ArrayList<>();

    List<String> endQuantityColumns = new ArrayList<>();
    List<String> endAmountColumns = new ArrayList<>();

    if (hasQuantity) {
      startQuantityColumns.addAll(quantityColumns.stream()
          .filter(column -> column.startsWith(PREFIX_START_STOCK))
          .collect(Collectors.toList()));

      endQuantityColumns.addAll(quantityColumns.stream()
          .filter(column -> column.startsWith(PREFIX_END_STOCK))
          .collect(Collectors.toList()));
    }

    if (hasAmount) {
      startAmountColumns.addAll(amountColumns.stream()
          .filter(column -> column.startsWith(PREFIX_START_STOCK))
          .collect(Collectors.toList()));

      endAmountColumns.addAll(amountColumns.stream()
          .filter(column -> column.startsWith(PREFIX_END_STOCK))
          .collect(Collectors.toList()));
    }

    boolean hasEmptyStartGroupValue = stockGroup != null
        && (startQuantityColumns.stream().anyMatch(s -> s.endsWith(EMPTY_VALUE_SUFFIX))
        || startAmountColumns.stream().anyMatch(s -> s.endsWith(EMPTY_VALUE_SUFFIX)));

    boolean hasEmptyEndGroupValue = stockGroup != null
        && (endQuantityColumns.stream().anyMatch(s -> s.endsWith(EMPTY_VALUE_SUFFIX))
        || endAmountColumns.stream().anyMatch(s -> s.endsWith(EMPTY_VALUE_SUFFIX)));

    boolean needsStartTotals = stockGroup != null
        && (startQuantityColumns.size() > 1 || startAmountColumns.size() > 1);
    boolean needsEndTotals = stockGroup != null
        && (endQuantityColumns.size() > 1 || endAmountColumns.size() > 1);

    boolean needsInTotals = movementInColumns.size() > 1;
    boolean needsOutTotals = movementOutColumns.size() > 1;

    boolean hasStart = !startQuantityColumns.isEmpty() || !startAmountColumns.isEmpty();
    boolean hasEnd = !endQuantityColumns.isEmpty() || !endAmountColumns.isEmpty();

    HasIndexedWidgets container = getDataContainer();
    if (container == null) {
      return;
    }

    if (!container.isEmpty()) {
      container.clear();
    }

    sheet().clear();

    Map<String, Double> totals = new HashMap<>();
    quantityColumns.forEach(column -> totals.put(column, BeeConst.DOUBLE_ZERO));
    amountColumns.forEach(column -> totals.put(column, BeeConst.DOUBLE_ZERO));

    double totalStartQuantity = BeeConst.DOUBLE_ZERO;
    double totalStartAmount = BeeConst.DOUBLE_ZERO;

    double totalEndQuantity = BeeConst.DOUBLE_ZERO;
    double totalEndAmount = BeeConst.DOUBLE_ZERO;

    double totalInQuantity = BeeConst.DOUBLE_ZERO;
    double totalInAmount = BeeConst.DOUBLE_ZERO;

    double totalOutQuantity = BeeConst.DOUBLE_ZERO;
    double totalOutAmount = BeeConst.DOUBLE_ZERO;

    double rowStartQuantity;
    double rowStartAmount;

    double rowEndQuantity;
    double rowEndAmount;

    double rowInQuantity;
    double rowInAmount;

    double rowOutQuantity;
    double rowOutAmount;

    Map<String, Integer> columnIndexes = new HashMap<>();

    int rowStartTotalColumnIndex = BeeConst.UNDEF;
    int rowEndTotalColumnIndex = BeeConst.UNDEF;

    int rowInTotalColumnIndex = BeeConst.UNDEF;
    int rowOutTotalColumnIndex = BeeConst.UNDEF;

    int boldRef = sheet().registerFont(XFont.bold());

    HtmlTable table = new HtmlTable(styleTable());
    int r = 0;
    int c = 0;

    XRow xr = new XRow(r);

    XStyle xs = XStyle.center();
    xs.setColor(Colors.LIGHTGRAY);
    xs.setFontRef(boldRef);

    int headerStyleRef = sheet().registerStyle(xs);

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

      table.setText(r, c, text, stylePrice());
      xr.add(new XCell(c, text, headerStyleRef));

      c++;
    }

    if (hasStart) {
      for (int i = 0; i < startQuantityColumns.size(); i++) {
        columnIndexes.put(startQuantityColumns.get(i), c + i);
      }
      for (int i = 0; i < startAmountColumns.size(); i++) {
        columnIndexes.put(startAmountColumns.get(i), c + i);
      }

      if (stockGroup == null) {
        text = Format.renderDate(parameters.getDateTime(RP_START_DATE));

        table.setText(r, c, text, styleStart);
        xr.add(new XCell(c, text, headerStyleRef));

        c++;

      } else {
        if (hasEmptyStartGroupValue) {
          text = BeeUtils.bracket(stockGroup.getCaption());

          table.setText(r, c, text, styleStart, styleColumnEmptyLabel());
          xr.add(new XCell(c, text, headerStyleRef));

          c++;
        }

        for (int i = 0; i < stockStartLabels.size(); i++) {
          text = TradeUtils.formatGroupLabel(stockGroup, stockStartLabels.get(i));

          table.setText(r, c, text, styleStart, styleColumnLabel());
          xr.add(new XCell(c, text, headerStyleRef));

          if (stockGroup.isEditable() && BeeUtils.isIndex(stockStartValues, i)) {
            TableCellElement cell = table.getCellFormatter().getElement(r, c);
            String value = stockStartValues.get(i);

            maybeMakeEditable(cell, stockGroup, value);
          }

          c++;
        }

        if (needsStartTotals) {
          rowStartTotalColumnIndex = c;

          text = BeeUtils.notEmpty(Format.renderDate(parameters.getDateTime(RP_START_DATE)),
              Localized.dictionary().total());

          table.setText(r, c, text, styleStart, styleRowTotal());
          xr.add(new XCell(c, text, headerStyleRef));

          c++;
        }
      }
    }

    if (!movementInColumns.isEmpty()) {
      for (TradeMovementColumn column : movementInColumns) {
        if (hasQuantity) {
          columnIndexes.put(column.getQuantityColumn(), c);
        }
        if (hasAmount) {
          columnIndexes.put(column.getAmountColumn(), c);
        }

        List<String> captions = column.getCaptions();
        if (captions.isEmpty()) {
          captions.add(Localized.dictionary().trdReportMovementIn());
        }

        table.setText(r, c, BeeUtils.buildLines(captions), styleIn, styleColumnLabel());
        xr.add(new XCell(c, BeeUtils.joinItems(captions), headerStyleRef));

        c++;
      }

      if (needsInTotals) {
        rowInTotalColumnIndex = c;

        text = Localized.dictionary().trdReportMovementIn();

        table.setText(r, c, text, styleIn, styleRowTotal());
        xr.add(new XCell(c, text, headerStyleRef));

        c++;
      }
    }

    if (!movementOutColumns.isEmpty()) {
      for (TradeMovementColumn column : movementOutColumns) {
        if (hasQuantity) {
          columnIndexes.put(column.getQuantityColumn(), c);
        }
        if (hasAmount) {
          columnIndexes.put(column.getAmountColumn(), c);
        }

        List<String> captions = column.getCaptions();
        if (captions.isEmpty()) {
          captions.add(Localized.dictionary().trdReportMovementOut());
        }

        table.setText(r, c, BeeUtils.buildLines(captions), styleOut, styleColumnLabel());
        xr.add(new XCell(c, BeeUtils.joinItems(captions), headerStyleRef));

        c++;
      }

      if (needsOutTotals) {
        rowOutTotalColumnIndex = c;

        text = Localized.dictionary().trdReportMovementOut();

        table.setText(r, c, text, styleOut, styleRowTotal());
        xr.add(new XCell(c, text, headerStyleRef));

        c++;
      }
    }

    if (hasEnd) {
      for (int i = 0; i < endQuantityColumns.size(); i++) {
        columnIndexes.put(endQuantityColumns.get(i), c + i);
      }
      for (int i = 0; i < endAmountColumns.size(); i++) {
        columnIndexes.put(endAmountColumns.get(i), c + i);
      }

      if (stockGroup == null) {
        text = Format.renderDate(parameters.getDateTime(RP_END_DATE));

        table.setText(r, c, text, styleEnd);
        xr.add(new XCell(c, text, headerStyleRef));

      } else {
        if (hasEmptyEndGroupValue) {
          text = BeeUtils.bracket(stockGroup.getCaption());

          table.setText(r, c, text, styleEnd, styleColumnEmptyLabel());
          xr.add(new XCell(c, text, headerStyleRef));

          c++;
        }

        for (int i = 0; i < stockEndLabels.size(); i++) {
          text = TradeUtils.formatGroupLabel(stockGroup, stockEndLabels.get(i));

          table.setText(r, c, text, styleEnd, styleColumnLabel());
          xr.add(new XCell(c, text, headerStyleRef));

          if (stockGroup.isEditable() && BeeUtils.isIndex(stockEndValues, i)) {
            TableCellElement cell = table.getCellFormatter().getElement(r, c);
            String value = stockEndValues.get(i);

            maybeMakeEditable(cell, stockGroup, value);
          }

          c++;
        }

        if (needsEndTotals) {
          rowEndTotalColumnIndex = c;

          text = BeeUtils.notEmpty(Format.renderDate(parameters.getDateTime(RP_END_DATE)),
              Localized.dictionary().total());

          table.setText(r, c, text, styleEnd, styleRowTotal());
          xr.add(new XCell(c, text, headerStyleRef));
        }
      }
    }

    table.getRowFormatter().addStyleName(r, styleHeader());
    sheet().add(xr);

    r++;

    xs = XStyle.right();
    int numberStyleRef = sheet().registerStyle(xs);

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

        table.setText(r, c, text, stylePrice());
        if (!BeeUtils.isEmpty(text)) {
          xr.add(new XCell(c, value, numberStyleRef));
        }
      }

      rowStartQuantity = BeeConst.DOUBLE_ZERO;
      rowStartAmount = BeeConst.DOUBLE_ZERO;

      rowEndQuantity = BeeConst.DOUBLE_ZERO;
      rowEndAmount = BeeConst.DOUBLE_ZERO;

      rowInQuantity = BeeConst.DOUBLE_ZERO;
      rowInAmount = BeeConst.DOUBLE_ZERO;

      rowOutQuantity = BeeConst.DOUBLE_ZERO;
      rowOutAmount = BeeConst.DOUBLE_ZERO;

      int rq = r;
      int ra = r;

      XRow xq = xr;
      XRow xa = xr;

      if (hasQuantity && hasAmount) {
        ra = r + 1;
        xa = new XRow(ra);
      }

      if (hasStart) {
        if (hasQuantity) {
          for (String column : startQuantityColumns) {
            Double qty = row.getDouble(column);
            text = TradeUtils.formatQuantity(qty);
            int j = columnIndexes.get(column);

            table.setText(rq, j, text, styleStart, styleQuantity());
            if (!BeeUtils.isEmpty(text)) {
              xq.add(new XCell(j, qty, numberStyleRef));
            }

            rowStartQuantity += BeeUtils.unbox(qty);
          }

          if (needsStartTotals) {
            text = TradeUtils.formatQuantity(rowStartQuantity);

            table.setText(rq, rowStartTotalColumnIndex, text, styleStart, styleQuantity(),
                styleRowTotal());
            if (!BeeUtils.isEmpty(text)) {
              xq.add(new XCell(rowStartTotalColumnIndex, rowStartQuantity, numberStyleRef));
            }

            totalStartQuantity += rowStartQuantity;
          }
        }

        if (hasAmount) {
          for (String column : startAmountColumns) {
            Double amount = row.getDouble(column);
            text = TradeUtils.formatAmount(amount);
            int j = columnIndexes.get(column);

            table.setText(ra, j, text, styleStart, styleAmount());
            if (!BeeUtils.isEmpty(text)) {
              xa.add(new XCell(j, Localized.normalizeMoney(amount), numberStyleRef));
            }

            rowStartAmount += BeeUtils.unbox(amount);
          }

          if (needsStartTotals) {
            text = TradeUtils.formatAmount(rowStartAmount);

            table.setText(ra, rowStartTotalColumnIndex, text, styleStart, styleAmount(),
                styleRowTotal());
            if (!BeeUtils.isEmpty(text)) {
              xa.add(new XCell(rowStartTotalColumnIndex, Localized.normalizeMoney(rowStartAmount),
                  numberStyleRef));
            }

            totalStartAmount += rowStartAmount;
          }
        }
      }

      if (!movementInColumns.isEmpty()) {
        for (TradeMovementColumn column : movementInColumns) {
          if (hasQuantity) {
            Double qty = row.getDouble(column.getQuantityColumn());
            text = TradeUtils.formatQuantity(qty);
            int j = columnIndexes.get(column.getQuantityColumn());

            table.setText(rq, j, text, styleIn, styleQuantity());
            if (!BeeUtils.isEmpty(text)) {
              xq.add(new XCell(j, qty, numberStyleRef));
            }

            rowInQuantity += BeeUtils.unbox(qty);
          }

          if (hasAmount) {
            Double amount = row.getDouble(column.getAmountColumn());
            text = TradeUtils.formatAmount(amount);
            int j = columnIndexes.get(column.getAmountColumn());

            table.setText(ra, j, text, styleIn, styleAmount());
            if (!BeeUtils.isEmpty(text)) {
              xa.add(new XCell(j, Localized.normalizeMoney(amount), numberStyleRef));
            }

            rowInAmount += BeeUtils.unbox(amount);
          }
        }

        if (needsInTotals) {
          if (hasQuantity) {
            text = TradeUtils.formatQuantity(rowInQuantity);

            table.setText(rq, rowInTotalColumnIndex, text, styleIn, styleQuantity(),
                styleRowTotal());
            if (!BeeUtils.isEmpty(text)) {
              xq.add(new XCell(rowInTotalColumnIndex, rowInQuantity, numberStyleRef));
            }

            totalInQuantity += rowInQuantity;
          }

          if (hasAmount) {
            text = TradeUtils.formatAmount(rowInAmount);

            table.setText(ra, rowInTotalColumnIndex, text, styleIn, styleAmount(),
                styleRowTotal());
            if (!BeeUtils.isEmpty(text)) {
              xa.add(new XCell(rowInTotalColumnIndex, Localized.normalizeMoney(rowInAmount),
                  numberStyleRef));
            }

            totalInAmount += rowInAmount;
          }
        }
      }

      if (!movementOutColumns.isEmpty()) {
        for (TradeMovementColumn column : movementOutColumns) {
          if (hasQuantity) {
            Double qty = row.getDouble(column.getQuantityColumn());
            text = TradeUtils.formatQuantity(qty);
            int j = columnIndexes.get(column.getQuantityColumn());

            table.setText(rq, j, text, styleOut, styleQuantity());
            if (!BeeUtils.isEmpty(text)) {
              xq.add(new XCell(j, qty, numberStyleRef));
            }

            rowOutQuantity += BeeUtils.unbox(qty);
          }

          if (hasAmount) {
            Double amount = row.getDouble(column.getAmountColumn());
            text = TradeUtils.formatAmount(amount);
            int j = columnIndexes.get(column.getAmountColumn());

            table.setText(ra, j, text, styleOut, styleAmount());
            if (!BeeUtils.isEmpty(text)) {
              xa.add(new XCell(j, Localized.normalizeMoney(amount), numberStyleRef));
            }

            rowOutAmount += BeeUtils.unbox(amount);
          }
        }

        if (needsOutTotals) {
          if (hasQuantity) {
            text = TradeUtils.formatQuantity(rowOutQuantity);

            table.setText(rq, rowOutTotalColumnIndex, text, styleOut, styleQuantity(),
                styleRowTotal());
            if (!BeeUtils.isEmpty(text)) {
              xq.add(new XCell(rowOutTotalColumnIndex, rowOutQuantity, numberStyleRef));
            }

            totalOutQuantity += rowOutQuantity;
          }

          if (hasAmount) {
            text = TradeUtils.formatAmount(rowOutAmount);

            table.setText(ra, rowOutTotalColumnIndex, text, styleOut, styleAmount(),
                styleRowTotal());
            if (!BeeUtils.isEmpty(text)) {
              xa.add(new XCell(rowOutTotalColumnIndex, Localized.normalizeMoney(rowOutAmount),
                  numberStyleRef));
            }

            totalOutAmount += rowOutAmount;
          }
        }
      }

      if (hasEnd) {
        if (hasQuantity) {
          for (String column : endQuantityColumns) {
            Double qty = row.getDouble(column);
            text = TradeUtils.formatQuantity(qty);
            int j = columnIndexes.get(column);

            table.setText(rq, j, text, styleEnd, styleQuantity());
            if (!BeeUtils.isEmpty(text)) {
              xq.add(new XCell(j, qty, numberStyleRef));
            }

            rowEndQuantity += BeeUtils.unbox(qty);
          }

          if (needsEndTotals) {
            text = TradeUtils.formatQuantity(rowEndQuantity);

            table.setText(rq, rowEndTotalColumnIndex, text, styleEnd, styleQuantity(),
                styleRowTotal());
            if (!BeeUtils.isEmpty(text)) {
              xq.add(new XCell(rowEndTotalColumnIndex, rowEndQuantity, numberStyleRef));
            }

            totalEndQuantity += rowEndQuantity;
          }
        }

        if (hasAmount) {
          for (String column : endAmountColumns) {
            Double amount = row.getDouble(column);
            text = TradeUtils.formatAmount(amount);
            int j = columnIndexes.get(column);

            table.setText(ra, j, text, styleEnd, styleAmount());
            if (!BeeUtils.isEmpty(text)) {
              xa.add(new XCell(j, Localized.normalizeMoney(amount), numberStyleRef));
            }

            rowEndAmount += BeeUtils.unbox(amount);
          }

          if (needsEndTotals) {
            text = TradeUtils.formatAmount(rowEndAmount);

            table.setText(ra, rowEndTotalColumnIndex, text, styleEnd, styleAmount(),
                styleRowTotal());
            if (!BeeUtils.isEmpty(text)) {
              xa.add(new XCell(rowEndTotalColumnIndex, Localized.normalizeMoney(rowEndAmount),
                  numberStyleRef));
            }

            totalEndAmount += rowEndAmount;
          }
        }
      }

      if (hasQuantity && hasAmount) {
        table.getRowFormatter().addStyleName(rq, styleBody());
        table.getRowFormatter().addStyleName(rq, styleQuantityRow());
        sheet().add(xq);

        table.getRowFormatter().addStyleName(ra, styleBody());
        table.getRowFormatter().addStyleName(ra, styleAmountRow());
        sheet().add(xa);

        r += 2;

      } else {
        table.getRowFormatter().addStyleName(r, styleBody());
        sheet().add(xr);

        r++;
      }

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
      int ra = r;

      XRow xq = new XRow(r);
      XRow xa = xq;

      if (hasQuantity && hasAmount) {
        ra = r + 1;
        xa = new XRow(ra);
      }

      xs = XStyle.right();
      xs.setColor(Colors.LIGHTGRAY);
      xs.setFontRef(boldRef);

      int footerStyleRef = sheet().registerStyle(xs);

      int minIndex = columnIndexes.values().stream().mapToInt(i -> i).min().getAsInt();
      if (minIndex > 0) {
        text = Localized.dictionary().totalOf();

        table.setText(r, minIndex - 1, text, styleTotal());
        xq.add(new XCell(minIndex - 1, text, footerStyleRef));
      }

      if (hasQuantity) {
        for (String column : quantityColumns) {
          Double qty = totals.get(column);
          text = TradeUtils.formatQuantity(qty);
          int j = columnIndexes.get(column);

          table.setText(r, j, text, styleQuantity(), getColumnStyle(column));
          if (!BeeUtils.isEmpty(text)) {
            xq.add(new XCell(j, qty, footerStyleRef));
          }
        }

        if (needsStartTotals) {
          text = TradeUtils.formatQuantity(totalStartQuantity);

          table.setText(r, rowStartTotalColumnIndex, text, styleStart, styleQuantity(),
              styleRowTotal());
          if (!BeeUtils.isEmpty(text)) {
            xq.add(new XCell(rowStartTotalColumnIndex, totalStartQuantity, footerStyleRef));
          }
        }

        if (needsInTotals) {
          text = TradeUtils.formatQuantity(totalInQuantity);

          table.setText(r, rowInTotalColumnIndex, text, styleIn, styleQuantity(),
              styleRowTotal());
          if (!BeeUtils.isEmpty(text)) {
            xq.add(new XCell(rowInTotalColumnIndex, totalInQuantity, footerStyleRef));
          }
        }

        if (needsOutTotals) {
          text = TradeUtils.formatQuantity(totalOutQuantity);

          table.setText(r, rowOutTotalColumnIndex, text, styleOut, styleQuantity(),
              styleRowTotal());
          if (!BeeUtils.isEmpty(text)) {
            xq.add(new XCell(rowOutTotalColumnIndex, totalOutQuantity, footerStyleRef));
          }
        }

        if (needsEndTotals) {
          text = TradeUtils.formatQuantity(totalEndQuantity);

          table.setText(r, rowEndTotalColumnIndex, text, styleEnd, styleQuantity(),
              styleRowTotal());
          if (!BeeUtils.isEmpty(text)) {
            xq.add(new XCell(rowEndTotalColumnIndex, totalEndQuantity, footerStyleRef));
          }
        }
      }

      if (hasAmount) {
        for (String column : amountColumns) {
          Double amount = totals.get(column);
          text = TradeUtils.formatAmount(amount);
          int j = columnIndexes.get(column);

          table.setText(ra, j, text, styleAmount(), getColumnStyle(column));
          if (!BeeUtils.isEmpty(text)) {
            xa.add(new XCell(j, Localized.normalizeMoney(amount), footerStyleRef));
          }
        }

        if (needsStartTotals) {
          text = TradeUtils.formatAmount(totalStartAmount);

          table.setText(ra, rowStartTotalColumnIndex, text, styleStart, styleAmount(),
              styleRowTotal());
          if (!BeeUtils.isEmpty(text)) {
            xa.add(new XCell(rowStartTotalColumnIndex, Localized.normalizeMoney(totalStartAmount),
                footerStyleRef));
          }
        }

        if (needsInTotals) {
          text = TradeUtils.formatAmount(totalInAmount);

          table.setText(ra, rowInTotalColumnIndex, text, styleIn, styleAmount(),
              styleRowTotal());
          if (!BeeUtils.isEmpty(text)) {
            xa.add(new XCell(rowInTotalColumnIndex, Localized.normalizeMoney(totalInAmount),
                footerStyleRef));
          }
        }

        if (needsOutTotals) {
          text = TradeUtils.formatAmount(totalOutAmount);

          table.setText(ra, rowOutTotalColumnIndex, text, styleOut, styleAmount(),
              styleRowTotal());
          if (!BeeUtils.isEmpty(text)) {
            xa.add(new XCell(rowOutTotalColumnIndex, Localized.normalizeMoney(totalOutAmount),
                footerStyleRef));
          }
        }

        if (needsEndTotals) {
          text = TradeUtils.formatAmount(totalEndAmount);

          table.setText(ra, rowEndTotalColumnIndex, text, styleEnd, styleAmount(),
              styleRowTotal());
          if (!BeeUtils.isEmpty(text)) {
            xa.add(new XCell(rowEndTotalColumnIndex, Localized.normalizeMoney(totalEndAmount),
                footerStyleRef));
          }
        }
      }

      if (hasQuantity && hasAmount) {
        table.getRowFormatter().addStyleName(r, styleFooter());
        table.getRowFormatter().addStyleName(r, styleQuantityRow());
        sheet().add(xq);

        table.getRowFormatter().addStyleName(ra, styleFooter());
        table.getRowFormatter().addStyleName(ra, styleAmountRow());
        sheet().add(xa);

      } else {
        table.getRowFormatter().addStyleName(r, styleFooter());
        sheet().add(xq);
      }
    }

    container.add(table);
  }

  private String getColumnStyle(String name) {
    if (name.startsWith(PREFIX_START_STOCK)) {
      return styleStart;

    } else if (name.startsWith(PREFIX_END_STOCK)) {
      return styleEnd;

    } else if (name.startsWith(PREFIX_MOVEMENT_IN)) {
      return styleIn;

    } else if (name.startsWith(PREFIX_MOVEMENT_OUT)) {
      return styleOut;

    } else {
      return null;
    }
  }
}
