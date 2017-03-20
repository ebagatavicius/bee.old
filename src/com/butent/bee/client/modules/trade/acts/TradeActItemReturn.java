package com.butent.bee.client.modules.trade.acts;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.Span;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActKind;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class TradeActItemReturn {

  private static final String STYLE_PREFIX = TradeActKeeper.STYLE_PREFIX + "return-items-";

  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog";
  private static final String STYLE_ALL = STYLE_PREFIX + "all";
  private static final String STYLE_CLEAR = STYLE_PREFIX + "clear";
  private static final String STYLE_SAVE = STYLE_PREFIX + "save";
  private static final String STYLE_CLOSE = STYLE_PREFIX + "close";

  private static final String STYLE_WRAPPER = STYLE_PREFIX + "wrapper";
  private static final String STYLE_TABLE = STYLE_PREFIX + "table";

  private static final String STYLE_HEADER_ROW = STYLE_PREFIX + "header";
  private static final String STYLE_ITEM_ROW = STYLE_PREFIX + "item";
  private static final String STYLE_SELECTED_ROW = STYLE_PREFIX + "selected";

  private static final String STYLE_ID_PREFIX = STYLE_PREFIX + "id-";
  private static final String STYLE_NAME_PREFIX = STYLE_PREFIX + "name-";
  private static final String STYLE_ARTICLE_PREFIX = STYLE_PREFIX + "article-";

  private static final String STYLE_PRICE_PREFIX = STYLE_PREFIX + "price-";
  private static final String STYLE_PRICE_WRAPPER = STYLE_PRICE_PREFIX + "wrapper";
  private static final String STYLE_PRICE_VALUE = STYLE_PRICE_PREFIX + "value";
  private static final String STYLE_PRICE_CURRENCY = STYLE_PRICE_PREFIX + "currency";

  private static final String STYLE_QTY_PREFIX = STYLE_PREFIX + "qty-";

  private static final String STYLE_INPUT_PREFIX = STYLE_PREFIX + "input-";
  private static final String STYLE_INPUT_WIDGET = STYLE_INPUT_PREFIX + "widget";

  private static final String STYLE_ACT_NUMBER_PREFIX = STYLE_PREFIX + "act-number-";
  private static final String STYLE_COMPANY_PREFIX = STYLE_PREFIX + "company-";
  private static final String STYLE_OBJECT_PREFIX = STYLE_PREFIX + "object-";

  private static final String STYLE_HEADER_CELL_SUFFIX = "label";
  private static final String STYLE_CELL_SUFFIX = "cell";

  private static NumberFormat priceFormat;

  static void show(String caption, BeeRowSet parentActs, final BeeRowSet parentItems,
      boolean showActInfo, final Consumer<BeeRowSet> consumer) {

    final HtmlTable table = new HtmlTable(STYLE_TABLE);

    table.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Element target = EventUtils.getEventTargetElement(event);
        TableCellElement cell = DomUtils.getParentCell(target, true);

        if (isQuantityCell(cell)) {
          selectQuantity(cell);
        }
      }
    });

    int r = 0;
    int c = 0;

    table.setText(r, c++, Localized.dictionary().captionId(),
        STYLE_ID_PREFIX + STYLE_HEADER_CELL_SUFFIX);

    table.setText(r, c++, Localized.dictionary().name(),
        STYLE_NAME_PREFIX + STYLE_HEADER_CELL_SUFFIX);
    table.setText(r, c++, Localized.dictionary().article(),
        STYLE_ARTICLE_PREFIX + STYLE_HEADER_CELL_SUFFIX);

    table.setText(r, c++, Localized.dictionary().price(),
        STYLE_PRICE_PREFIX + STYLE_HEADER_CELL_SUFFIX);
    table.setText(r, c++, Localized.dictionary().quantity(),
        STYLE_QTY_PREFIX + STYLE_HEADER_CELL_SUFFIX);

    table.setText(r, c++, Localized.dictionary().taQuantityReturn(),
        STYLE_INPUT_PREFIX + STYLE_HEADER_CELL_SUFFIX);

    if (showActInfo) {
      table.setText(r, c++, Data.getColumnLabel(VIEW_TRADE_ACTS, COL_TA_NUMBER),
          STYLE_ACT_NUMBER_PREFIX + STYLE_HEADER_CELL_SUFFIX);

      table.setText(r, c++, Data.getColumnLabel(VIEW_TRADE_ACTS, COL_TA_COMPANY),
          STYLE_COMPANY_PREFIX + STYLE_HEADER_CELL_SUFFIX);
      table.setText(r, c++, Data.getColumnLabel(VIEW_TRADE_ACTS, COL_TA_OBJECT),
          STYLE_OBJECT_PREFIX + STYLE_HEADER_CELL_SUFFIX);
    }

    table.getRowFormatter().addStyleName(r, STYLE_HEADER_ROW);

    int actIndex = parentItems.getColumnIndex(COL_TRADE_ACT);
    int itemIndex = parentItems.getColumnIndex(COL_TA_ITEM);
    int nameIndex = parentItems.getColumnIndex(ALS_ITEM_NAME);
    int articleIndex = parentItems.getColumnIndex(COL_ITEM_ARTICLE);
    int priceIndex = parentItems.getColumnIndex(COL_TRADE_ITEM_PRICE);
    int qtyIndex = parentItems.getColumnIndex(COL_TRADE_ITEM_QUANTITY);

    int currencyNameIndex = parentActs.getColumnIndex(AdministrationConstants.ALS_CURRENCY_NAME);

    int seriesNameIndex = parentActs.getColumnIndex(COL_SERIES_NAME);
    int numberIndex = parentActs.getColumnIndex(COL_TA_NUMBER);
    int companyNameIndex = parentActs.getColumnIndex(ALS_COMPANY_NAME);
    int objectNameIndex = parentActs.getColumnIndex(COL_COMPANY_OBJECT_NAME);

    BeeColumn qtyColumn = parentItems.getColumn(qtyIndex);

    r++;
    for (BeeRow p : parentItems) {
      BeeRow act = parentActs.getRowById(p.getLong(actIndex));
      Assert.notNull(act, "act not found " + p.getId());

      c = 0;

      table.setText(r, c++, p.getString(itemIndex), STYLE_ID_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, p.getString(nameIndex), STYLE_NAME_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, p.getString(articleIndex), STYLE_ARTICLE_PREFIX + STYLE_CELL_SUFFIX);

      String priceHtml = renderPrice(p.getDouble(priceIndex), act.getString(currencyNameIndex));
      if (!BeeUtils.isEmpty(priceHtml)) {
        table.setHtml(r, c, priceHtml, STYLE_PRICE_PREFIX + STYLE_CELL_SUFFIX);
      }
      c++;

      table.setText(r, c++, p.getString(qtyIndex), STYLE_QTY_PREFIX + STYLE_CELL_SUFFIX);
      table.setWidget(r, c++, renderInput(qtyColumn), STYLE_INPUT_PREFIX + STYLE_CELL_SUFFIX);

      if (showActInfo) {
        table.setText(r, c++,
            BeeUtils.joinWords(act.getString(seriesNameIndex), act.getString(numberIndex)),
            STYLE_ACT_NUMBER_PREFIX + STYLE_CELL_SUFFIX);

        table.setText(r, c++, act.getString(companyNameIndex),
            STYLE_COMPANY_PREFIX + STYLE_CELL_SUFFIX);
        table.setText(r, c++, act.getString(objectNameIndex),
            STYLE_OBJECT_PREFIX + STYLE_CELL_SUFFIX);
      }

      table.getRowFormatter().addStyleName(r, STYLE_ITEM_ROW);
      DomUtils.setDataIndex(table.getRow(r), p.getId());

      r++;
    }

    final DialogBox dialog = DialogBox.withoutCloseBox(caption, STYLE_DIALOG);

    Button all = new Button(Localized.dictionary().selectAll());
    all.addStyleName(STYLE_ALL);

    all.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectAllQuantities(table);
      }
    });

    dialog.addCommand(all);

    Button clear = new Button(Localized.dictionary().clear());
    clear.addStyleName(STYLE_CLEAR);

    clear.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        clearQuantities(table);
      }
    });

    dialog.addCommand(clear);

    FaLabel save = new FaLabel(FontAwesome.SAVE);
    save.addStyleName(STYLE_SAVE);

    save.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Map<Long, Double> quantities = getQuantities(table);
        if (!quantities.isEmpty()) {
          selectItems(parentItems, quantities, consumer);
        }

        dialog.close();
      }
    });

    dialog.addAction(Action.SAVE, save);

    FaLabel close = new FaLabel(FontAwesome.CLOSE);
    close.addStyleName(STYLE_CLOSE);

    close.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        final Map<Long, Double> quantities = getQuantities(table);

        if (quantities.isEmpty()) {
          dialog.close();

        } else {
          Global.decide(TradeActKind.RETURN.getCaption(),
              Collections.singletonList(Localized.dictionary().saveSelectedItems()),
              new DecisionCallback() {
                @Override
                public void onConfirm() {
                  selectItems(parentItems, quantities, consumer);
                  dialog.close();
                }

                @Override
                public void onDeny() {
                  dialog.close();
                }
              }, DialogConstants.DECISION_YES);
        }
      }
    });

    dialog.addAction(Action.CLOSE, close);

    Simple wrapper = new Simple(table);
    wrapper.addStyleName(STYLE_WRAPPER);

    dialog.setWidget(wrapper);
    dialog.center();
  }

  private static void clearQuantities(Widget parent) {
    Collection<InputNumber> inputs = UiHelper.getChildren(parent, InputNumber.class);

    for (InputNumber input : inputs) {
      if (BeeUtils.isPositive(input.getNumber())) {
        input.clearValue();
        onQuantityChange(input.getElement(), null);
      }
    }
  }

  private static NumberFormat ensurePriceFormat() {
    if (priceFormat == null) {
      Integer scale = Data.getColumnScale(VIEW_TRADE_ACT_ITEMS, COL_TRADE_ITEM_PRICE);
      if (scale == null || scale <= 2) {
        priceFormat = Format.getDefaultMoneyFormat();
      } else {
        priceFormat = Format.getDecimalFormat(2, scale);
      }
    }
    return priceFormat;
  }

  private static Map<Long, Double> getQuantities(Widget parent) {
    Map<Long, Double> result = new HashMap<>();

    Collection<InputNumber> inputs = UiHelper.getChildren(parent, InputNumber.class);

    for (InputNumber input : inputs) {
      Double qty = input.getNumber();

      if (BeeUtils.isPositive(qty)) {
        long id = DomUtils.getDataIndexLong(DomUtils.getParentRow(input.getElement(), false));
        if (DataUtils.isId(id)) {
          result.put(id, qty);
        }
      }
    }

    return result;
  }

  private static boolean isQuantityCell(Element cell) {
    return cell != null && cell.hasClassName(STYLE_QTY_PREFIX + STYLE_CELL_SUFFIX);
  }

  private static void onQuantityChange(Element source, Double qty) {
    TableRowElement row = DomUtils.getParentRow(source, true);

    if (row != null) {
      if (BeeUtils.isPositive(qty)) {
        row.addClassName(STYLE_SELECTED_ROW);
      } else {
        row.removeClassName(STYLE_SELECTED_ROW);
      }
    }
  }

  private static Widget renderInput(BeeColumn column) {
    InputNumber input = new InputNumber();
    input.addStyleName(STYLE_INPUT_WIDGET);

    input.setMinValue(BeeConst.STRING_ZERO);

    if (column != null) {
      input.setMaxValue(DataUtils.getMaxValue(column));
      input.setScale(column.getScale());
      input.setMaxLength(UiHelper.getMaxLength(column));
    }

    input.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        if (event.getSource() instanceof InputNumber) {
          InputNumber w = (InputNumber) event.getSource();
          onQuantityChange(w.getElement(), w.getNumber());
        }
      }
    });

    return input;
  }

  private static String renderPrice(Double price, String currency) {
    if (BeeUtils.isDouble(price)) {
      Div div = new Div().addClass(STYLE_PRICE_WRAPPER);
      div.append(new Span().addClass(STYLE_PRICE_VALUE).text(ensurePriceFormat().format(price)));

      if (!BeeUtils.isEmpty(currency)) {
        div.append(new Span().addClass(STYLE_PRICE_CURRENCY).text(currency));
      }

      return div.build();

    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  private static void selectAllQuantities(HtmlTable table) {
    for (int r = 0; r < table.getRowCount(); r++) {
      Element rowEl = table.getRow(r);

      if (rowEl.hasClassName(STYLE_ITEM_ROW) && !rowEl.hasClassName(STYLE_SELECTED_ROW)) {
        List<TableCellElement> cells = table.getRowCells(r);

        for (TableCellElement cell : cells) {
          if (isQuantityCell(cell)) {
            selectQuantity(cell);
            break;
          }
        }
      }
    }
  }

  private static void selectItems(BeeRowSet input, Map<Long, Double> quantities,
      Consumer<BeeRowSet> consumer) {

    BeeRowSet selection = new BeeRowSet(input.getViewName(), input.getColumns());
    int index = input.getColumnIndex(COL_TRADE_ITEM_QUANTITY);

    for (BeeRow item : input) {
      if (quantities.containsKey(item.getId())) {
        BeeRow row = DataUtils.cloneRow(item);
        row.setValue(index, quantities.get(item.getId()));

        selection.addRow(row);
      }
    }

    if (!selection.isEmpty()) {
      consumer.accept(selection);
    }
  }

  private static void selectQuantity(Element qtyCell) {
    String text = qtyCell.getInnerText();
    Double qty = BeeUtils.toDoubleOrNull(text);

    if (BeeUtils.isPositive(qty)) {
      Element target = Selectors.getElement(DomUtils.getParentRow(qtyCell, false),
          Selectors.classSelector(STYLE_INPUT_WIDGET));

      if (InputElement.is(target)) {
        InputElement input = InputElement.as(target);
        if (!BeeUtils.isPositiveDouble(input.getValue())) {
          input.setValue(text);
          onQuantityChange(input, qty);
        }
      }
    }
  }

  private TradeActItemReturn() {
  }
}
