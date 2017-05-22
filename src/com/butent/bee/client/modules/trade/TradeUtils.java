package com.butent.bee.client.modules.trade;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.XMLParser;
import com.google.gwt.xml.client.impl.DOMParseException;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.ClientDefaults;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.administration.AdministrationKeeper;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.DoubleLabel;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.Triplet;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.FontStyle;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.Totalizer;
import com.butent.bee.shared.modules.trade.TradeDiscountMode;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeReportGroup;
import com.butent.bee.shared.modules.trade.TradeVatMode;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class TradeUtils {

  private static final String STYLE_ITEMS = TradeKeeper.STYLE_PREFIX + "itemsInfo-";
  private static final String STYLE_ITEMS_TABLE = STYLE_ITEMS + "table";
  private static final String STYLE_ITEMS_HEADER = STYLE_ITEMS + "header";
  private static final String STYLE_ITEMS_DATA = STYLE_ITEMS + "data";
  private static final String STYLE_ITEMS_FOOTER = STYLE_ITEMS + "footer";

  private static final String STYLE_COST_CALCULATION_COMMAND =
      TradeKeeper.STYLE_PREFIX + "cost-calculation";

  private static final String STYLE_ITEM_STOCK_PREFIX = TradeKeeper.STYLE_PREFIX + "item-stock-";
  private static final String STYLE_ITEM_STOCK_TABLE = STYLE_ITEM_STOCK_PREFIX + "table";
  private static final String STYLE_ITEM_STOCK_HEADER = STYLE_ITEM_STOCK_PREFIX + "header";
  private static final String STYLE_ITEM_STOCK_BODY = STYLE_ITEM_STOCK_PREFIX + "body";
  private static final String STYLE_ITEM_STOCK_FOOTER = STYLE_ITEM_STOCK_PREFIX + "footer";
  private static final String STYLE_ITEM_STOCK_WAREHOUSE = STYLE_ITEM_STOCK_PREFIX + "warehouse";
  private static final String STYLE_ITEM_STOCK_QUANTITY = STYLE_ITEM_STOCK_PREFIX + "quantity";
  private static final String STYLE_ITEM_STOCK_RESERVED = STYLE_ITEM_STOCK_PREFIX + "reserved";
  private static final String STYLE_ITEM_STOCK_AVAILABLE = STYLE_ITEM_STOCK_PREFIX + "available";

  private static final String KEY_WAREHOUSE = "warehouse";

  private static final String COL_NAME = "Name";
  private static final String COL_ORDINAL = "Ordinal";
  private static final String COL_TOTAL = "Total";
  private static final String COL_RATE_AMOUNT = COL_CURRENCY_RATE + COL_TRADE_AMOUNT;
  private static final String COL_RATE_VAT = COL_CURRENCY_RATE + COL_TRADE_VAT;
  private static final String COL_RATE_TOTAL = COL_CURRENCY_RATE + COL_TOTAL;
  private static final String COL_RATE_CURRENCY = COL_CURRENCY_RATE + COL_CURRENCY;

  private static NumberFormat quantityFormat;
  private static NumberFormat costFormat;
  private static NumberFormat amountFormat;

  public static void amountEntry(IsRow row, String viewName) {
    Totalizer totalizer = new Totalizer(Data.getColumns(viewName));

    InputNumber input = new InputNumber();
    Double total = totalizer.getTotal(row);

    if (BeeUtils.isDouble(total)) {
      input.setValue(BeeUtils.round(total, 2));
    }
    Global.inputWidget(Localized.dictionary().amount(), input, () -> {
      Double amount = input.getNumber();
      String price = null;

      if (BeeUtils.isDouble(amount)) {
        if (!totalizer.isVatInclusive(row)) {
          Data.clearCell(viewName, row, COL_TRADE_VAT_PLUS);
          amount -= BeeUtils.unbox(totalizer.getVat(row, amount));
          Data.setValue(viewName, row, COL_TRADE_VAT_PLUS, 1);
        }
        Double qty = Data.getDouble(viewName, row, COL_TRADE_ITEM_QUANTITY);
        price = BeeUtils.toString(amount / (BeeUtils.isZero(qty) ? 1 : qty), 5);
      }
      List<BeeColumn> columns = new ArrayList<>();
      List<String> oldValues = new ArrayList<>();
      List<String> newValues = new ArrayList<>();

      columns.add(Data.getColumn(viewName, COL_TRADE_ITEM_PRICE));
      oldValues.add(Data.getString(viewName, row, COL_TRADE_ITEM_PRICE));
      newValues.add(price);

      String oldCurrency = Data.getString(viewName, row, COL_TRADE_CURRENCY);
      String newCurrency = null;

      if (!BeeUtils.isEmpty(price)) {
        newCurrency = BeeUtils.notEmpty(oldCurrency,
            DataUtils.isId(ClientDefaults.getCurrency())
                ? BeeUtils.toString(ClientDefaults.getCurrency()) : null);
      }
      columns.add(Data.getColumn(viewName, COL_TRADE_CURRENCY));
      oldValues.add(oldCurrency);
      newValues.add(newCurrency);

      Queries.update(viewName, row.getId(), row.getVersion(), columns, oldValues, newValues,
          null, new RowUpdateCallback(viewName));
    });
  }

  public static String formatAmount(Double amount) {
    return BeeUtils.nonZero(amount) ? getAmountFormat().format(amount) : BeeConst.STRING_EMPTY;
  }

  public static String formatCost(Double cost) {
    return BeeUtils.nonZero(cost) ? getCostFormat().format(cost) : BeeConst.STRING_EMPTY;
  }

  public static String formatQuantity(Double qty) {
    return BeeUtils.nonZero(qty) ? getQuantityFormat().format(qty) : BeeConst.STRING_EMPTY;
  }

  public static String formatGroupLabel(TradeReportGroup group, String label) {
    if (BeeUtils.isEmpty(label)) {
      return BeeConst.STRING_EMPTY;

    } else if (group == TradeReportGroup.MONTH_RECEIVED && BeeUtils.isDigit(label)) {
      int value = BeeUtils.toInt(label);

      if (TimeUtils.isMonth(value)) {
        return Format.renderMonthFullStandalone(value);

      } else {
        int year = value / 100;
        int month = value % 100;

        if (TimeUtils.isYear(year) && TimeUtils.isMonth(month)) {
          return Format.renderYearMonth(YearMonth.of(year, month));
        } else {
          return label;
        }
      }

    } else {
      return label;
    }
  }

  public static void getDocumentItems(String viewName, long tradeId, String currencyName,
      final HtmlTable table) {
    Assert.notNull(table);

    if (BeeUtils.isEmpty(viewName) || !DataUtils.isId(tradeId)) {
      return;
    }
    ParameterList args = TradeKeeper.createArgs(SVC_ITEMS_INFO);
    args.addDataItem("view_name", viewName);
    args.addDataItem("id", tradeId);

    String currencyTo = DomUtils.getDataProperty(table.getElement(), COL_RATE_CURRENCY);

    if (!BeeUtils.isEmpty(currencyTo)
        && !BeeUtils.same(currencyName, ClientDefaults.getCurrencyName())) {
      currencyTo = ClientDefaults.getCurrencyName();
    }
    final boolean rateExists = !BeeUtils.isEmpty(currencyTo);

    if (rateExists) {
      args.addDataItem(COL_CURRENCY, currencyTo);
    }
    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(BeeKeeper.getScreen());

        if (response.hasErrors()) {
          return;
        }
        if (table.getRowCount() == 0) {
          return;
        }
        int headerRowCount = Math.max(table.getRowCount() - 1, 1);
        boolean footer = table.getRowCount() > 1;

        table.addStyleName(STYLE_ITEMS_TABLE);

        NumberFormat formatter = NumberFormat.getFormat("0.00");
        String currency = null;
        double qtyTotal = 0;
        double vatTotal = 0;
        double sumTotal = 0;
        String rateCurrency = null;
        double rate = 0;
        double currVatTotal = 0;
        double currSumTotal = 0;

        SimpleRowSet rs = SimpleRowSet.restore((String) response.getResponse());
        int ordinal = 0;

        for (SimpleRow row : rs) {
          ordinal++;
          if (BeeUtils.isEmpty(currency)) {
            currency = row.getValue(COL_CURRENCY);
          }
          double qty = BeeUtils.unbox(row.getDouble(COL_TRADE_ITEM_QUANTITY));
          qtyTotal += qty;
          double sum = qty * BeeUtils.unbox(row.getDouble(COL_TRADE_ITEM_PRICE));
          double currSum = 0;
          double vat = BeeUtils.unbox(row.getDouble(COL_TRADE_VAT));
          boolean vatInPercents = BeeUtils.unbox(row.getBoolean(COL_TRADE_VAT_PERC));

          if (BeeUtils.unbox(row.getBoolean(COL_TRADE_VAT_PLUS))) {
            if (vatInPercents) {
              vat = sum / 100 * vat;
            }
          } else {
            if (vatInPercents) {
              vat = sum - sum / (1 + vat / 100);
            }
            sum -= vat;
          }
          sum = BeeUtils.round(sum, 2);
          sumTotal += sum;
          vatTotal += vat;

          if (rateExists) {
            if (BeeUtils.isEmpty(rateCurrency)) {
              rateCurrency = row.getValue(COL_RATE_CURRENCY);
              rate = BeeUtils.unbox(row.getDouble(COL_CURRENCY_RATE));
            }
            currSum = BeeUtils.round(sum * rate, 2);
            currSumTotal += currSum;
            currVatTotal += vat * rate;
          }
          Document xml = null;

          for (int i = 0; i < headerRowCount; i++) {
            int x = table.insertRow(table.getRowCount() - (footer ? 1 : 0));
            table.getRow(x).setInnerHTML(table.getRow(i).getInnerHTML());
            table.getRowFormatter().addStyleName(x, STYLE_ITEMS_DATA);

            for (int j = 0; j < table.getCellCount(x); j++) {
              Multimap<String, Element> elements = getNamedElements(table.getCellFormatter()
                  .getElement(x, j).getFirstChildElement());

              for (String fld : elements.keySet()) {
                String value;

                if (BeeUtils.same(fld, COL_TRADE_ITEM_QUANTITY)) {
                  value = BeeUtils.toString(qty);

                } else if (BeeUtils.same(fld, COL_TRADE_ITEM_PRICE)) {
                  value = formatter.format(sum / qty);

                } else if (BeeUtils.same(fld, COL_TRADE_VAT)) {
                  value = row.getValue(fld);

                  if (value != null && vatInPercents) {
                    value = BeeUtils.removeTrailingZeros(value) + "%";
                  }
                } else if (BeeUtils.same(fld, COL_TRADE_AMOUNT)) {
                  value = formatter.format(sum);

                } else if (BeeUtils.same(fld, COL_RATE_AMOUNT)) {
                  value = formatter.format(currSum);

                } else if (BeeUtils.same(fld, COL_ORDINAL)) {
                  value = BeeUtils.toString(ordinal);

                } else if (BeeUtils.same(fld, COL_CURRENCY)) {
                  value = currency;

                } else if (BeeUtils.same(fld, COL_RATE_CURRENCY)) {
                  value = rateCurrency;

                } else if (BeeUtils.same(fld, COL_CURRENCY_RATE)) {
                  value = BeeUtils.toString(rate, 7);

                } else if (!rs.hasColumn(fld)) {
                  if (xml == null) {
                    try {
                      xml = XMLParser.parse(row.getValue(COL_TRADE_ITEM_NOTE));
                    } catch (DOMParseException ex) {
                      xml = null;
                    }
                  }
                  if (xml != null) {
                    value = BeeUtils.joinItems(XmlUtils.getChildrenText(xml.getDocumentElement(),
                        fld));
                  } else {
                    value = null;
                  }
                } else {
                  value = row.getValue(fld);
                }
                for (Element element : elements.get(fld)) {
                  element.addClassName(STYLE_ITEMS + fld);
                  element.setInnerText(value);
                }
              }
            }
          }
        }
        for (int i = 0; i < headerRowCount; i++) {
          for (int j = 0; j < table.getCellCount(i); j++) {
            Multimap<String, Element> elements = getNamedElements(table.getCellFormatter()
                .getElement(i, j).getFirstChildElement());

            for (String fld : elements.keySet()) {
              for (Element element : elements.get(fld)) {
                element.addClassName(STYLE_ITEMS + fld);
              }
            }
          }
          table.getRowFormatter().addStyleName(i, STYLE_ITEMS_HEADER);
        }
        if (footer) {
          vatTotal = BeeUtils.round(vatTotal, 2);
          currVatTotal = BeeUtils.round(currVatTotal, 2);

          int x = table.getRowCount() - 1;

          for (int j = 0; j < table.getCellCount(x); j++) {
            Multimap<String, Element> elements = getNamedElements(table.getCellFormatter()
                .getElement(x, j).getFirstChildElement());

            for (String fld : elements.keySet()) {
              String value;

              if (BeeUtils.same(fld, COL_TRADE_ITEM_QUANTITY + COL_TOTAL)) {
                value = BeeUtils.removeTrailingZeros(BeeUtils.toString(qtyTotal));

              } else if (BeeUtils.same(fld, COL_TRADE_AMOUNT + COL_TOTAL)) {
                value = formatter.format(sumTotal);

              } else if (BeeUtils.same(fld, COL_TRADE_VAT + COL_TOTAL)) {
                value = formatter.format(vatTotal);

              } else if (BeeUtils.same(fld, COL_TOTAL)) {
                value = formatter.format(sumTotal + vatTotal);

              } else if (BeeUtils.same(fld, COL_CURRENCY)) {
                value = currency;

              } else if (BeeUtils.same(fld, COL_RATE_AMOUNT + COL_TOTAL)) {
                value = formatter.format(currSumTotal);

              } else if (BeeUtils.same(fld, COL_RATE_VAT + COL_TOTAL)) {
                value = formatter.format(currVatTotal);

              } else if (BeeUtils.same(fld, COL_RATE_TOTAL)) {
                value = formatter.format(currSumTotal + currVatTotal);

              } else if (BeeUtils.same(fld, COL_RATE_CURRENCY)) {
                value = rateCurrency;

              } else if (BeeUtils.same(fld, COL_CURRENCY_RATE)) {
                value = BeeUtils.toString(rate, 7);

              } else {
                value = null;
              }
              for (Element element : elements.get(fld)) {
                element.addClassName(STYLE_ITEMS + fld);
                element.setInnerText(value);
              }
            }
          }
          table.getRowFormatter().addStyleName(x, STYLE_ITEMS_FOOTER);
        }
      }
    });
  }

  public static void getTotalInWords(Double amount, final Long currency, final Widget total) {
    Assert.notNull(total);

    if (amount == null || amount <= 0) {
      return;
    }
    ParameterList args = AdministrationKeeper.createArgs(SVC_TOTAL_TO_WORDS);
    args.addDataItem(VAR_AMOUNT, amount);
    args.addDataItem(COL_CURRENCY, currency);

    String locale = DomUtils.getDataProperty(total.getElement(), VAR_LOCALE);

    if (!BeeUtils.isEmpty(locale)) {
      args.addDataItem(VAR_LOCALE, locale);
    }
    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(BeeKeeper.getScreen());

        if (response.hasErrors()) {
          return;
        }
        total.getElement().setInnerText(response.getResponseAsString());
      }
    });
  }

  public static Pair<Double, Boolean> normalizeDiscountOrVatInfo(Pair<Double, Boolean> info) {
    if (info != null && BeeUtils.nonZero(info.getA())) {
      return info;
    } else {
      return Pair.empty();
    }
  }

  public static Widget renderItemStockByWarehouse(long item,
      List<Triplet<String, Double, Double>> data) {

    if (BeeUtils.isEmpty(data)) {
      return null;
    }

    boolean hasReservations = data.stream().anyMatch(e -> BeeUtils.nonZero(e.getC()));

    HtmlTable table = new HtmlTable(STYLE_ITEM_STOCK_TABLE);

    int r = 0;
    int c = 0;

    table.setText(r, c, Localized.dictionary().warehouse());
    c++;

    table.setText(r, c, Localized.dictionary().trdQuantityStock());
    table.setColumnCellClasses(c, StyleUtils.className(TextAlign.RIGHT));
    c++;

    if (hasReservations) {
      table.setText(r, c, Localized.dictionary().trdQuantityReserved());
      table.setColumnCellClasses(c, StyleUtils.className(TextAlign.RIGHT));
      c++;

      table.setText(r, c, Localized.dictionary().trdQuantityAvailable());
      table.setColumnCellClasses(c, StyleUtils.className(TextAlign.RIGHT));
    }

    table.getRowFormatter().addStyleName(r, STYLE_ITEM_STOCK_HEADER);
    r++;

    double totalStock = BeeConst.DOUBLE_ZERO;
    double totalReserved = BeeConst.DOUBLE_ZERO;
    double totalAvailable = BeeConst.DOUBLE_ZERO;

    for (Triplet<String, Double, Double> triplet : data) {
      c = 0;
      table.setText(r, c, triplet.getA(), STYLE_ITEM_STOCK_WAREHOUSE);
      c++;

      Double stock = triplet.getB();
      if (BeeUtils.isDouble(stock)) {
        table.setText(r, c, BeeUtils.toString(stock), STYLE_ITEM_STOCK_QUANTITY);
        totalStock += stock;
      }
      c++;

      if (hasReservations) {
        Double reserved = triplet.getC();

        if (BeeUtils.isDouble(reserved)) {
          DoubleLabel label = new DoubleLabel(false);
          label.setValue(reserved);

          label.addClickHandler(event -> {
            Element target = EventUtils.getEventTargetElement(event);
            String code = DomUtils.getDataProperty(DomUtils.getParentRow(target, true),
                KEY_WAREHOUSE);

            TradeKeeper.getWarehouseId(code, id -> showReservations(id, item, null, target));
          });

          table.setWidgetAndStyle(r, c, label, STYLE_ITEM_STOCK_RESERVED);
          totalReserved += reserved;
        }
        c++;

        double available = Math.max(BeeUtils.unbox(stock) - BeeUtils.unbox(reserved),
            BeeConst.DOUBLE_ZERO);
        table.setText(r, c, BeeUtils.toString(available), STYLE_ITEM_STOCK_AVAILABLE);
        totalAvailable += available;
      }

      table.getRowFormatter().addStyleName(r, STYLE_ITEM_STOCK_BODY);
      DomUtils.setDataProperty(table.getRow(r), KEY_WAREHOUSE, triplet.getA());

      r++;
    }

    if (data.size() > 1) {
      c = 0;
      table.setText(r, c, Localized.dictionary().total(), STYLE_ITEM_STOCK_WAREHOUSE);
      c++;

      table.setText(r, c, BeeUtils.toString(totalStock), STYLE_ITEM_STOCK_QUANTITY);
      c++;

      if (hasReservations) {
        table.setText(r, c, BeeUtils.toString(totalReserved), STYLE_ITEM_STOCK_RESERVED);
        c++;

        table.setText(r, c, BeeUtils.toString(totalAvailable), STYLE_ITEM_STOCK_AVAILABLE);
      }

      table.getRowFormatter().addStyleName(r, STYLE_ITEM_STOCK_FOOTER);
    }

    return table;
  }

  public static Widget renderReservations(Map<ModuleAndSub, Map<String, Double>> info) {
    if (BeeUtils.isEmpty(info)) {
      return null;
    }

    HtmlTable table = new HtmlTable(StyleUtils.NAME_INFO_TABLE);

    table.getRowFormatter().addStyleName(0, StyleUtils.className(FontWeight.BOLD));
    table.getRowFormatter().addStyleName(0, StyleUtils.className(TextAlign.CENTER));
    table.setText(0, 0, Localized.dictionary().reservation());
    table.setText(0, 1, Localized.dictionary().quantity());
    table.setColumnCellClasses(1, StyleUtils.className(TextAlign.RIGHT));

    info.keySet().forEach(mod -> {
      int r = table.getRowCount();
      table.getCellFormatter().setColSpan(r, 0, 2);
      table.getRowFormatter().addStyleName(r, StyleUtils.className(FontStyle.ITALIC));
      table.setText(r, 0, BeeUtils.joinWords(mod.getModule().getCaption(), mod.hasSubModule()
          ? BeeUtils.parenthesize(mod.getSubModule().getCaption()) : null));

      info.get(mod).forEach((text, qty) -> {
        int r2 = table.getRowCount();
        table.setText(r2, 0, text);
        table.setText(r2, 1, BeeUtils.toString(qty));
      });
    });

    return table;
  }

  static void configureCostCalculation(final DataView dataView) {
    HeaderView header = ViewHelper.getHeader(dataView);

    boolean enabled = isCostCalculationEnabled(dataView);
    boolean has = hasCostCalculationCommand(header);

    if (has && !enabled) {
      header.removeCommandByStyleName(STYLE_COST_CALCULATION_COMMAND);

    } else if (enabled && !has && header != null) {
      String caption = Localized.dictionary().recalculateTradeItemCostsCaption();

      Button command = new Button(caption);
      command.addStyleName(STYLE_COST_CALCULATION_COMMAND);

      command.addClickHandler(event -> Global.confirm(caption, Icon.QUESTION,
          Collections.singletonList(Localized.dictionary().recalculateTradeItemCostsQuestion()),
          () -> {
            IsRow row = ViewHelper.getParentRow(dataView.asWidget(), VIEW_TRADE_DOCUMENTS);

            if (isCostCalculationEnabled(row)) {
              ParameterList params = TradeKeeper.createArgs(SVC_CALCULATE_COST);

              params.addQueryItem(COL_TRADE_DOCUMENT, row.getId());
              params.addNotEmptyQuery(COL_TRADE_DATE,
                  Data.getString(VIEW_TRADE_DOCUMENTS, row, COL_TRADE_DATE));
              params.addNotEmptyQuery(COL_TRADE_CURRENCY,
                  Data.getString(VIEW_TRADE_DOCUMENTS, row, COL_TRADE_CURRENCY));

              params.addNotEmptyQuery(COL_TRADE_DOCUMENT_VAT_MODE,
                  Data.getString(VIEW_TRADE_DOCUMENTS, row, COL_TRADE_DOCUMENT_VAT_MODE));
              params.addNotEmptyQuery(COL_TRADE_DOCUMENT_DISCOUNT_MODE,
                  Data.getString(VIEW_TRADE_DOCUMENTS, row, COL_TRADE_DOCUMENT_DISCOUNT_MODE));
              params.addNotEmptyQuery(COL_TRADE_DOCUMENT_DISCOUNT,
                  Data.getString(VIEW_TRADE_DOCUMENTS, row, COL_TRADE_DOCUMENT_DISCOUNT));

              params.setSummary(COL_TRADE_DOCUMENT, row.getId());

              BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
                @Override
                public void onResponse(ResponseObject response) {
                  if (response.hasMessages()) {
                    response.notify(dataView);

                  } else if (response.hasResponse()) {
                    dataView.notifyInfo(
                        Localized.dictionary().recalculateTradeItemCostsNotification(
                            response.getResponseAsString()));

                  } else {
                    dataView.notifyWarning(Localized.dictionary().noData());
                  }
                }
              });
            }
          }));

      header.addCommandItem(command);
    }
  }

  static DateTime getDocumentDate(IsRow row) {
    if (row == null) {
      return null;
    } else {
      return Data.getDateTime(VIEW_TRADE_DOCUMENTS, row, COL_TRADE_DATE);
    }
  }

  static Double getDocumentDiscount(IsRow row) {
    if (row == null) {
      return null;
    } else {
      return Data.getDouble(VIEW_TRADE_DOCUMENTS, row, COL_TRADE_DOCUMENT_DISCOUNT);
    }
  }

  static TradeDiscountMode getDocumentDiscountMode(IsRow row) {
    if (row == null) {
      return null;
    } else {
      return Data.getEnum(VIEW_TRADE_DOCUMENTS, row, COL_TRADE_DOCUMENT_DISCOUNT_MODE,
          TradeDiscountMode.class);
    }
  }

  static ItemPrice getDocumentItemPrice(IsRow row) {
    if (row == null) {
      return null;
    } else {
      return Data.getEnum(VIEW_TRADE_DOCUMENTS, row, COL_TRADE_DOCUMENT_PRICE_NAME,
          ItemPrice.class);
    }
  }

  static OperationType getDocumentOperationType(IsRow row) {
    if (row == null) {
      return null;
    } else {
      return Data.getEnum(VIEW_TRADE_DOCUMENTS, row, COL_OPERATION_TYPE, OperationType.class);
    }
  }

  static TradeDocumentPhase getDocumentPhase(IsRow row) {
    if (row == null) {
      return null;
    } else {
      return Data.getEnum(VIEW_TRADE_DOCUMENTS, row, COL_TRADE_DOCUMENT_PHASE,
          TradeDocumentPhase.class);
    }
  }

  static Long getDocumentRelation(IsRow row, String colName) {
    if (row == null) {
      return null;
    } else {
      return Data.getLong(VIEW_TRADE_DOCUMENTS, row, colName);
    }
  }

  static String getDocumentString(IsRow row, String colName) {
    if (row == null) {
      return null;
    } else {
      return Data.getString(VIEW_TRADE_DOCUMENTS, row, colName);
    }
  }

  static TradeVatMode getDocumentVatMode(IsRow row) {
    if (row == null) {
      return null;
    } else {
      return Data.getEnum(VIEW_TRADE_DOCUMENTS, row, COL_TRADE_DOCUMENT_VAT_MODE,
          TradeVatMode.class);
    }
  }

  static void getDocumentVatPercent(IsRow row, final Consumer<Double> consumer) {
    if (getDocumentVatMode(row) == null) {
      consumer.accept(null);

    } else {
      Long operation = getDocumentRelation(row, COL_TRADE_OPERATION);
      if (operation == null) {
        consumer.accept(null);

      } else {
        Queries.getValue(VIEW_TRADE_OPERATIONS, operation, COL_OPERATION_VAT_PERCENT,
            new RpcCallback<String>() {
              @Override
              public void onSuccess(String result) {
                Double vatPercent = BeeUtils.toDoubleOrNull(result);

                if (vatPercent == null) {
                  Number p = Global.getParameterNumber(PRM_VAT_PERCENT);
                  if (p != null) {
                    vatPercent = p.doubleValue();
                  }
                }

                consumer.accept(vatPercent);
              }
            });
      }
    }
  }

  static boolean isDocumentEditable(IsRow row) {
    if (row == null) {
      return false;

    } else if (!DataUtils.isNewRow(row)) {
      TradeDocumentPhase phase = getDocumentPhase(row);
      if (phase != null && !phase.isEditable(BeeKeeper.getUser().isAdministrator())) {
        return false;
      }

      if (isDocumentProtected(row)) {
        return false;
      }
    }

    return true;
  }

  static boolean isDocumentProtected(IsRow row) {
    JustDate minDate = Global.getParameterDate(PRM_PROTECT_TRADE_DOCUMENTS_BEFORE);

    if (minDate != null) {
      DateTime date = getDocumentDate(row);
      if (date != null && TimeUtils.isLess(date, minDate)) {
        return true;
      }
    }

    return false;
  }

  static boolean documentPriceIsParentCost(IsRow row) {
    return documentPriceIsParentCost(getDocumentOperationType(row), getDocumentItemPrice(row));
  }

  static boolean documentPriceIsParentCost(OperationType operationType, ItemPrice itemPrice) {
    return operationType != null && operationType.consumesStock()
        && (itemPrice == ItemPrice.COST
        || itemPrice == null && operationType.getDefaultPrice() == ItemPrice.COST);
  }

  static Long getCompanyForPriceCalculation(IsRow row, OperationType operationType) {
    Long company = getDocumentRelation(row, COL_TRADE_PAYER);

    if (company == null && operationType != null) {
      String colName = operationType.consumesStock() ? COL_TRADE_CUSTOMER : COL_TRADE_SUPPLIER;
      company = getDocumentRelation(row, colName);

      if (company == null) {
        colName = operationType.consumesStock() ? COL_TRADE_SUPPLIER : COL_TRADE_CUSTOMER;
        company = getDocumentRelation(row, colName);

        if (company == null) {
          company = Global.getParameterRelation(PRM_COMPANY);
        }
      }
    }

    return company;
  }

  static Long getWarehouseForPriceCalculation(IsRow row, OperationType operationType) {
    if (row == null || operationType == null) {
      return null;
    } else {
      return getDocumentRelation(row,
          operationType.consumesStock() ? COL_TRADE_WAREHOUSE_FROM : COL_TRADE_WAREHOUSE_TO);
    }
  }

  static Map<String, String> getDocumentPriceCalculationOptions(IsRow row,
      DateTime date, Long currency, OperationType operationType, Long company, Long warehouse) {

    Map<String, String> options = new HashMap<>();
    if (date == null || currency == null || operationType == null || company == null) {
      return options;
    }

    Long operation = getDocumentRelation(row, COL_TRADE_OPERATION);
    if (operation == null) {
      return options;
    }

    options.put(COL_DISCOUNT_COMPANY, BeeUtils.toStringOrNull(company));

    options.put(COL_DISCOUNT_OPERATION, BeeUtils.toStringOrNull(operation));
    if (operationType.requireOperationForPriceCalculation()) {
      options.put(Service.VAR_REQUIRED, COL_DISCOUNT_OPERATION);
    }

    if (warehouse != null) {
      options.put(COL_DISCOUNT_WAREHOUSE, BeeUtils.toStringOrNull(warehouse));
    }

    options.put(Service.VAR_TIME, BeeUtils.toString(date.getTime()));
    options.put(COL_DISCOUNT_CURRENCY, BeeUtils.toStringOrNull(currency));

    ItemPrice itemPrice = getDocumentItemPrice(row);
    if (itemPrice != null) {
      options.put(COL_DISCOUNT_PRICE_NAME, BeeUtils.toString(itemPrice.ordinal()));
    }

    return options;
  }

  static double roundPrice(Double price) {
    return Localized.normalizeMoney(price);
  }

  static void showReservations(Long warehouse, Long item, String caption, Element target) {
    if (DataUtils.isId(warehouse) && DataUtils.isId(item)) {
      TradeKeeper.getReservationsInfo(warehouse, item, null, info -> {
        Widget widget = renderReservations(info);
        if (widget == null) {
          widget = new Label(Localized.dictionary().noData());
        }

        String cap = BeeUtils.notEmpty(caption, Localized.dictionary().orders());
        Global.showModalWidget(cap, widget, target);
      });
    }
  }

  static Boolean vatIsPercent(Double vat) {
    if (BeeUtils.isDouble(vat)) {
      return true;
    } else {
      return null;
    }
  }

  private static Multimap<String, Element> getNamedElements(Element element) {
    Multimap<String, Element> elements = HashMultimap.create();
    String name = DomUtils.getDataProperty(element, COL_NAME);

    if (!BeeUtils.isEmpty(name)) {
      elements.put(name, element);
    } else {
      NodeList<Element> children = DomUtils.getChildren(element);

      if (children != null) {
        for (int i = 0; i < children.getLength(); i++) {
          elements.putAll(getNamedElements(children.getItem(i)));
        }
      }
    }
    return elements;
  }

  private static boolean hasCostCalculationCommand(HeaderView header) {
    return header != null && Selectors.contains(header.getElement(),
        Selectors.classSelector(STYLE_COST_CALCULATION_COMMAND));
  }

  private static boolean isCostCalculationEnabled(DataView dataView) {
    if (dataView != null && BeeKeeper.getUser().canEditData(VIEW_TRADE_DOCUMENT_ITEMS)
        && BeeKeeper.getUser().canEditData(VIEW_TRADE_ITEM_COST)) {

      IsRow row = ViewHelper.getParentRow(dataView.asWidget(), VIEW_TRADE_DOCUMENTS);
      return isCostCalculationEnabled(row);

    } else {
      return false;
    }
  }

  private static boolean isCostCalculationEnabled(IsRow row) {
    if (DataUtils.hasId(row)) {
      OperationType operationType =
          Data.getEnum(VIEW_TRADE_DOCUMENTS, row, COL_OPERATION_TYPE, OperationType.class);

      return operationType != null && operationType.providesCost();

    } else {
      return false;
    }
  }

  private static NumberFormat getQuantityFormat() {
    if (quantityFormat == null) {
      Integer scale = Data.getColumnScale(VIEW_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY);
      quantityFormat = Format.getDecimalFormat(0, BeeUtils.unbox(scale));
    }
    return quantityFormat;
  }

  private static NumberFormat getCostFormat() {
    if (costFormat == null) {
      Integer scale = Data.getColumnScale(VIEW_TRADE_ITEM_COST, COL_TRADE_ITEM_COST);
      costFormat = Format.getDecimalFormat(2, BeeUtils.unbox(scale));
    }
    return costFormat;
  }

  private static NumberFormat getAmountFormat() {
    if (amountFormat == null) {
      amountFormat = Format.getDefaultMoneyFormat();
    }
    return amountFormat;
  }


  private TradeUtils() {
  }
}
