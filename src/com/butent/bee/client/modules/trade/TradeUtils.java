package com.butent.bee.client.modules.trade;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.XMLParser;
import com.google.gwt.xml.client.impl.DOMParseException;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.modules.administration.AdministrationKeeper;
import com.butent.bee.client.render.ProvidesGridColumnRenderer;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Map;
import java.util.Map.Entry;

public final class TradeUtils {

  private static final String STYLE_ITEMS = TradeKeeper.STYLE_PREFIX + "itemsInfo-";
  private static final String STYLE_ITEMS_TABLE = STYLE_ITEMS + "table";
  private static final String STYLE_ITEMS_HEADER = STYLE_ITEMS + "header";
  private static final String STYLE_ITEMS_DATA = STYLE_ITEMS + "data";
  private static final String STYLE_ITEMS_FOOTER = STYLE_ITEMS + "footer";

  private static final String COL_NAME = "Name";
  private static final String COL_ORDINAL = "Ordinal";
  private static final String COL_TOTAL = "Total";
  private static final String COL_RATE_AMOUNT = COL_CURRENCY_RATE + COL_TRADE_AMOUNT;
  private static final String COL_RATE_VAT = COL_CURRENCY_RATE + COL_TRADE_VAT;
  private static final String COL_RATE_TOTAL = COL_CURRENCY_RATE + COL_TOTAL;

  private static ProvidesGridColumnRenderer totalRenderer;

  public static void getDocumentItems(String viewName, long tradeId, final HtmlTable table) {
    Assert.notNull(table);

    if (BeeUtils.isEmpty(viewName) || !DataUtils.isId(tradeId)) {
      return;
    }
    ParameterList args = TradeKeeper.createArgs(SVC_ITEMS_INFO);
    args.addDataItem("view_name", viewName);
    args.addDataItem("id", tradeId);

    final String currencyTo = DomUtils.getDataProperty(table.getElement(),
        COL_CURRENCY_RATE + COL_CURRENCY);
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
          Map<String, String> cols = Maps.newLinkedHashMap();
          // cols.put(COL_ORDINAL, Localized.getConstants().ordinal());
          cols.put(COL_NAME, Localized.getConstants().item());
          cols.put(ClassifierConstants.COL_ITEM_ARTICLE, Localized.getConstants().article());
          cols.put(COL_TRADE_ITEM_QUANTITY, Localized.getConstants().trdQuantity());
          cols.put(ClassifierConstants.COL_UNIT, Localized.getConstants().unit());
          cols.put(COL_TRADE_ITEM_PRICE, Localized.getConstants().trdPrice());
          cols.put(COL_TRADE_AMOUNT, Localized.getConstants().trdAmountWoVat());
          cols.put(COL_TRADE_VAT, Localized.getConstants().vat());

          if (rateExists) {
            cols.put(COL_RATE_AMOUNT,
                BeeUtils.joinWords(Localized.getConstants().trdAmount(), currencyTo));
          }
          int j = 0;

          for (String col : cols.keySet()) {
            Element cell = DomUtils.createDiv(cols.get(col));
            DomUtils.setDataProperty(cell, COL_NAME, col);
            table.setHtml(0, j++, cell.getString());
          }
          Widget cell = new CustomDiv();
          DomUtils.setDataProperty(cell.getElement(),
              COL_NAME, COL_TRADE_ITEM_QUANTITY + COL_TOTAL);

          table.getCellFormatter().setColSpan(1, 0, 3);
          table.getCellFormatter().setVerticalAlignment(1, 0, VerticalAlign.TOP);
          table.setWidget(1, 0, cell);

          FlowPanel cap = new FlowPanel();
          FlowPanel val = new FlowPanel();
          FlowPanel curr = new FlowPanel();

          for (Entry<String, String> row : ImmutableMap.of(
              COL_TRADE_AMOUNT + COL_TOTAL, Localized.getConstants().trdAmount(),
              COL_TRADE_VAT + COL_TOTAL, Localized.getConstants().vat(),
              COL_TOTAL, Localized.getConstants().trdTotal()).entrySet()) {

            cell = new CustomDiv(STYLE_ITEMS + row.getKey() + "-caption");
            cell.getElement().setInnerText(row.getValue());
            cap.add(cell);

            cell = new CustomDiv();
            DomUtils.setDataProperty(cell.getElement(), COL_NAME, row.getKey());
            val.add(cell);

            cell = new CustomDiv();
            DomUtils.setDataProperty(cell.getElement(), COL_NAME, COL_CURRENCY);
            curr.add(cell);
          }
          table.getCellFormatter().setColSpan(1, 1, 2);
          table.setWidget(1, 1, cap);
          table.setWidget(1, 2, val);
          table.setWidget(1, 3, curr);

          if (rateExists) {
            FlowPanel flow = new FlowPanel();

            for (String name : new String[] {COL_RATE_AMOUNT + COL_TOTAL,
                COL_RATE_VAT + COL_TOTAL, COL_RATE_TOTAL}) {

              cell = new CustomDiv();
              DomUtils.setDataProperty(cell.getElement(), COL_NAME, name);
              flow.add(cell);
            }
            table.setWidget(1, 4, flow);
          }
        }
        int headerRowCount = Math.max(table.getRowCount() - 1, 1);
        boolean footer = table.getRowCount() > 1;

        table.addStyleName(STYLE_ITEMS_TABLE);

        NumberFormat formater = NumberFormat.getFormat("0.00");
        String currency = null;
        double qtyTotal = 0;
        double vatTotal = 0;
        double sumTotal = 0;
        String rateCurrency = null;
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
              rateCurrency = row.getValue(COL_CURRENCY_RATE + COL_CURRENCY);
            }
            double rate = BeeUtils.unbox(row.getDouble(COL_CURRENCY_RATE));
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
                  value = formater.format(sum / qty);

                } else if (BeeUtils.same(fld, COL_TRADE_VAT)) {
                  value = row.getValue(fld);

                  if (value != null && vatInPercents) {
                    value = BeeUtils.removeTrailingZeros(value) + "%";
                  }
                } else if (BeeUtils.same(fld, COL_TRADE_AMOUNT)) {
                  value = formater.format(sum);

                } else if (BeeUtils.same(fld, COL_RATE_AMOUNT)) {
                  value = formater.format(currSum);

                } else if (BeeUtils.same(fld, COL_ORDINAL)) {
                  value = BeeUtils.toString(ordinal);

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
                value = formater.format(sumTotal);

              } else if (BeeUtils.same(fld, COL_TRADE_VAT + COL_TOTAL)) {
                value = formater.format(vatTotal);

              } else if (BeeUtils.same(fld, COL_TOTAL)) {
                value = formater.format(sumTotal + vatTotal);

              } else if (BeeUtils.same(fld, COL_CURRENCY)) {
                value = currency;

              } else if (BeeUtils.same(fld, COL_RATE_AMOUNT + COL_TOTAL)) {
                value = formater.format(currSumTotal);

              } else if (BeeUtils.same(fld, COL_RATE_VAT + COL_TOTAL)) {
                value = formater.format(currVatTotal);

              } else if (BeeUtils.same(fld, COL_RATE_TOTAL)) {
                value = formater.format(currSumTotal + currVatTotal);

              } else if (BeeUtils.same(fld, COL_CURRENCY_RATE + COL_CURRENCY)) {
                value = rateCurrency;

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

  public static void getTotalInWords(Double amount, final String currencyName,
      final String minorName, final Widget total) {
    Assert.notNull(total);
    String locale = DomUtils.getDataProperty(total.getElement(), VAR_LOCALE);

    if (amount == null || amount <= 0) {
      return;
    }
    long number = BeeUtils.toLong(Math.floor(amount));
    final int fraction = BeeUtils.toInt((amount - number) * 100);

    ParameterList args = AdministrationKeeper.createArgs(SVC_NUMBER_TO_WORDS);
    args.addDataItem(VAR_AMOUNT, number);

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
        total.getElement().setInnerText(BeeUtils.joinWords(response.getResponse(), currencyName,
            fraction, minorName));
      }
    });
  }

  public static void registerTotalRenderer(String gridName, String columnName) {
    if (totalRenderer == null) {
      totalRenderer = new TotalRenderer.Provider();
    }
    RendererFactory.registerGcrProvider(gridName, columnName, totalRenderer);
  }

  private TradeUtils() {
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
}
