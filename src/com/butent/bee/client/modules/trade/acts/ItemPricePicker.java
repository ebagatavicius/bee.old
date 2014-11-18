package com.butent.bee.client.modules.trade.acts;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowFunction;
import com.butent.bee.shared.html.builder.elements.Option;
import com.butent.bee.shared.html.builder.elements.Select;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

class ItemPricePicker extends AbstractCellRenderer {

  private static final BeeLogger logger = LogUtils.getLogger(ItemPricePicker.class);

  private static final String ID_PREFIX = "ipp";
  private static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "ItemPricePicker";

  private static final String KEY_ROW_ID = "ipprow";
  private static final String KEY_COL_INDEX = "ippcol";

  private static boolean exported;

  private static void ensureExported() {
    if (!exported) {
      export();
      exported = true;
    }
  }

//@formatter:off
  // CHECKSTYLE:OFF
  private static native void export() /*-{
    $wnd.Bee_selectItemPrice = $entry(@ItemPricePicker::selectItemPrice(Lcom/google/gwt/core/client/JavaScriptObject;));
  }-*/;
  // CHECKSTYLE:ON
//@formatter:on

  private static void selectItemPrice(JavaScriptObject jso) {
    if (!SelectElement.is(jso)) {
      logger.warning("jso not recognized");
      return;
    }

    SelectElement selectElement = (SelectElement) jso.cast();

    String value = selectElement.getValue();
    if (!BeeUtils.isPositiveDouble(value)) {
      return;
    }

    GridView gridView = ViewHelper.getGrid(DomUtils.getWidget(selectElement.getId()));
    if (gridView == null) {
      logger.warning("grid not found for id", selectElement.getId());
      return;
    }

    Long rowId = DomUtils.getDataPropertyLong(selectElement, KEY_ROW_ID);
    Integer colIndex = DomUtils.getDataPropertyInt(selectElement, KEY_COL_INDEX);

    IsRow row = gridView.getGrid().getRowById(rowId);
    if (row == null) {
      logger.warning("row not found", rowId);
      return;
    }

    Queries.updateCellAndFire(gridView.getViewName(), rowId, row.getVersion(),
        gridView.getDataColumns().get(colIndex).getId(), row.getString(colIndex), value);
  }

  private final EnumMap<ItemPrice, Integer> priceIndexes = new EnumMap<>(ItemPrice.class);
  private final EnumMap<ItemPrice, Integer> currencyIndexes = new EnumMap<>(ItemPrice.class);

  private final RowFunction<Long> currencyFunction;

  ItemPricePicker(CellSource cellSource, List<? extends IsColumn> columns,
      RowFunction<Long> currencyFunction) {

    super(cellSource);

    int index;
    for (ItemPrice ip : ItemPrice.values()) {
      index = DataUtils.getColumnIndex(ip.getPriceAlias(), columns);
      if (!BeeConst.isUndef(index)) {
        priceIndexes.put(ip, index);
      }

      index = DataUtils.getColumnIndex(ip.getCurrencyAlias(), columns);
      if (!BeeConst.isUndef(index)) {
        currencyIndexes.put(ip, index);
      }
    }

    this.currencyFunction = currencyFunction;

    ensureExported();
  }

  @Override
  public String render(IsRow row) {
    if (row == null) {
      return null;
    }

    Long currency = (currencyFunction == null) ? null : currencyFunction.apply(row);

    EnumMap<ItemPrice, Double> prices = new EnumMap<>(ItemPrice.class);
    for (Map.Entry<ItemPrice, Integer> entry : priceIndexes.entrySet()) {
      Double p = row.getDouble(entry.getValue());

      if (BeeUtils.isPositive(p)) {
        ItemPrice ip = entry.getKey();
        if (currency != null && currencyIndexes.containsKey(ip)) {
          Long c = row.getLong(currencyIndexes.get(ip));
          if (Money.canExchange(c, currency)) {
            p = BeeUtils.round(Money.exchange(c, currency, p, null), getScale());
          }
        }
        prices.put(ip, p);
      }
    }

    if (prices.isEmpty()) {
      return null;
    }

    NumberFormat format = TradeActHelper.getPriceFormat();

    Double price = getDouble(row);

    Select select = new Select();
    select.setId(DomUtils.createUniqueId(ID_PREFIX));

    select.setData(KEY_ROW_ID, BeeUtils.toString(row.getId()));
    select.setData(KEY_COL_INDEX, BeeUtils.toString(getCellSource().getIndex()));

    select.addClassName(STYLE_NAME);

    select.appendChild(new Option().value(BeeConst.UNDEF));

    for (Map.Entry<ItemPrice, Double> entry : prices.entrySet()) {
      ItemPrice ip = entry.getKey();
      Double x = entry.getValue();

      String value = BeeUtils.toString(x);
      String text = BeeUtils.joinWords(BeeUtils.parenthesize(ip.getLabel()), format.format(x));

      Option option = new Option().value(value).text(text);
      if (price != null && x.equals(price)) {
        option.selected();
        price = null;
      }

      select.appendChild(option);
    }

    select.setOnChange("Bee_selectItemPrice(this)");
    return select.build();
  }
}
