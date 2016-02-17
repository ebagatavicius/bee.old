package com.butent.bee.client.modules.trade.acts;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.values.Display;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActConstants;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;

class TradeActServicePicker extends TradeActItemPicker {

  private static final String STYLE_DATE_INPUT = STYLE_PREFIX + "date-input";
  private static final String STYLE_NUMBER_INPUT = STYLE_PREFIX + "number-input";

  private final Map<Long, InputDate> datesFrom = new HashMap<>();
  private final Map<Long, InputDate> datesTo = new HashMap<>();
  private final Map<Long, Double> tariffs = new HashMap<>();
  private final Map<Long, Double> discounts = new HashMap<>();

  @Override
  protected Filter getDefaultItemFilter() {
    return Filter.notNull(ClassifierConstants.COL_ITEM_IS_SERVICE);
  }

  @Override
  protected void renderItems(Map<Long, Double> quantities, Map<Long, String> warehouses,
      Flow panel, BeeRowSet itemList) {
    super.renderItems(quantities, warehouses, panel, itemList);

    HtmlTable table = null;

    for (int i = 0; i < panel.getWidgetCount(); i++) {
      Widget w = panel.getWidget(i);
      if (w instanceof HtmlTable && StyleUtils.hasClassName(w.getElement(), STYLE_ITEM_TABLE)) {
        table = (HtmlTable) w;
        break;
      }
    }

    if (table == null) {
      return;
    }
    datesFrom.clear();
    datesTo.clear();
    tariffs.clear();
    discounts.clear();

    int r = 0;
    int c = table.getCellCount(r);

    getVisibleTableCols().add(TradeActConstants.COL_TA_SERVICE_FROM);
    table.setText(r, c++, Localized.getConstants().dateFrom());

    getVisibleTableCols().add(TradeActConstants.COL_TA_SERVICE_TO);
    table.setText(r, c++, Localized.getConstants().dateTo());
    getVisibleTableCols().add(TradeActConstants.COL_TA_SERVICE_TARIFF);
    table.setText(r, c++, Localized.getConstants().taTariff());

    getVisibleTableCols().add(TradeConstants.COL_TRADE_DISCOUNT);
    table.setText(r, c++, Localized.getConstants().discount());

    StyleUtils.setDisplay(table.getCellFormatter().getElement(r, getVisibleTableCols().indexOf(
        ClassifierConstants.COL_EXTERNAL_STOCK)), Display.NONE);

    r++;
    for (int i = r; i < table.getRowCount(); i++) {
      c = table.getCellCount(i);

      StyleUtils.setDisplay(table.getCellFormatter().getElement(i, getVisibleTableCols().indexOf(
          ClassifierConstants.COL_EXTERNAL_STOCK)), Display.NONE);

      Long itemId = BeeUtils.toLongOrNull(table.getCellFormatter().getElement(i,
          getVisibleTableCols().indexOf(DataUtils.ID_TAG)).getInnerText());

      if (!DataUtils.isId(itemId)) {
        continue;
      }

      table.setWidget(i, c++, renderDate(datesFrom, itemId));
      table.setWidget(i, c++, renderDate(datesTo, itemId));
      table.setWidget(i, c++, renderNumber(tariffs, itemId));
      table.setWidget(i, c++, renderNumber(discounts, itemId));
    }
  }

  @Override
  protected String getCaption() {
    return Localized.getConstants().services();
  }

  @Override
  public Long getWarehouseFrom(IsRow row) {
    return null;
  }

  @Override
  public boolean setIsOrder() {
    return false;
  }

  public Map<Long, JustDate> getDatesFrom() {
    Map<Long, JustDate> result = new HashMap<>();

    for (Long id : datesFrom.keySet()) {
      result.put(id, TimeUtils.parseDate(datesFrom.get(id).getValue()));
    }

    return result;
  }

  public Map<Long, JustDate> getDatesTo() {
    Map<Long, JustDate> result = new HashMap<>();

    for (Long id : datesTo.keySet()) {
      result.put(id, TimeUtils.parseDate(datesTo.get(id).getValue()));
    }

    return result;
  }

  public Map<Long, Double> getTariffs() {
    return tariffs;
  }

  public Map<Long, Double> getDiscounts() {
    return discounts;
  }

  private static Widget renderDate(final Map<Long, InputDate> store, final long id) {
    final InputDate input = new InputDate();
    input.addStyleName(STYLE_DATE_INPUT);

    if (store == null) {
      return input;
    }

    store.put(id, input);

    return input;
  }

  private static Widget renderNumber(final Map<Long, Double> store, final long id) {
    final InputNumber input = new InputNumber();

    input.setMinValue(BeeConst.STRING_ZERO);
    input.addStyleName(STYLE_NUMBER_INPUT);

    if (store == null) {
      return input;
    }

    input.addChangeHandler(new ChangeHandler() {

      @Override
      public void onChange(ChangeEvent event) {
        store.put(id, input.getNumber());
      }
    });

    return input;
  }
}