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
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.modules.administration.AdministrationKeeper;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.communication.ResponseObject;
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
import com.butent.bee.shared.modules.trade.TradeVatMode;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class TradeUtils {

  private static final String STYLE_ITEMS = TradeKeeper.STYLE_PREFIX + "itemsInfo-";
  private static final String STYLE_ITEMS_TABLE = STYLE_ITEMS + "table";
  private static final String STYLE_ITEMS_HEADER = STYLE_ITEMS + "header";
  private static final String STYLE_ITEMS_DATA = STYLE_ITEMS + "data";
  private static final String STYLE_ITEMS_FOOTER = STYLE_ITEMS + "footer";

  private static final String STYLE_COST_CALCULATION_COMMAND =
      TradeKeeper.STYLE_PREFIX + "cost-calculation";

  private static final String COL_NAME = "Name";
  private static final String COL_ORDINAL = "Ordinal";
  private static final String COL_TOTAL = "Total";
  private static final String COL_RATE_AMOUNT = COL_CURRENCY_RATE + COL_TRADE_AMOUNT;
  private static final String COL_RATE_VAT = COL_CURRENCY_RATE + COL_TRADE_VAT;
  private static final String COL_RATE_TOTAL = COL_CURRENCY_RATE + COL_TOTAL;
  private static final String COL_RATE_CURRENCY = COL_CURRENCY_RATE + COL_CURRENCY;

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

  static double roundPrice(Double price) {
    return Localized.normalizeMoney(price);
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

  private TradeUtils() {
  }
}
