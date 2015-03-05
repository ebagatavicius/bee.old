package com.butent.bee.client.modules.trade.acts;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.trade.Totalizer;
import com.butent.bee.shared.modules.trade.acts.TradeActUtils;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;

public class TradeActServicesGrid extends AbstractGridInterceptor {

  private static final String STYLE_COMMAND_RECALCULATE_PRICES = TradeActKeeper.STYLE_PREFIX
      + "command-recalculate-prices";

  private static double getItemTotal(GridView gridView) {
    double total = BeeConst.DOUBLE_ZERO;
    GridView items = ViewHelper.getSiblingGrid(gridView.asWidget(), GRID_TRADE_ACT_ITEMS);

    if (items != null && !items.isEmpty()) {
      Totalizer totalizer = new Totalizer(items.getDataColumns());

      int qtyIndex = items.getDataIndex(COL_TRADE_ITEM_QUANTITY);
      totalizer.setQuantityFunction(new QuantityReader(qtyIndex));

      for (IsRow row : items.getRowData()) {
        Double amount = totalizer.getTotal(row);
        if (BeeUtils.isDouble(amount)) {
          total += amount;
        }
      }
    }

    return total;
  }

  TradeActServicesGrid() {
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    GridView gridView = presenter.getGridView();

    if (gridView != null && !gridView.isReadOnly()) {
      Button command = new Button(Localized.getConstants().taRecalculatePrices());
      command.addStyleName(STYLE_COMMAND_RECALCULATE_PRICES);

      command.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          maybeRecalculatePrices();
        }
      });

      presenter.getHeader().addCommandItem(command);
    }
  }

  @Override
  public void afterUpdateCell(IsColumn column, String oldValue, String newValue, IsRow result,
      boolean rowMode) {

    if (COL_TA_SERVICE_TARIFF.equalsIgnoreCase(column.getId())
        && BeeUtils.isPositiveDouble(newValue)) {

      GridView gridView = getGridView();
      double total = getItemTotal(gridView);

      if (BeeUtils.isPositive(total)) {
        IsRow row = gridView.getGrid().getRowById(result.getId());

        String oldPrice = (row == null)
            ? null : row.getString(getDataIndex(COL_TRADE_ITEM_PRICE));
        JustDate dateTo = (row == null) ? null : row.getDate(getDataIndex(COL_TA_SERVICE_TO));

        Double price = calculatePrice(BeeUtils.toDoubleOrNull(oldPrice), dateTo, total,
            BeeUtils.toDouble(newValue));

        if (BeeUtils.isPositive(price)) {
          updatePrice(result.getId(), result.getVersion(), oldPrice, price);
        }
      }
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeActServicesGrid();
  }

  private Double calculatePrice(Double defPrice, JustDate dateTo, Double itemTotal, Double tariff) {
    Integer scale = Data.getColumnScale(getViewName(), COL_TRADE_ITEM_PRICE);
    return TradeActUtils.calculateServicePrice(defPrice, dateTo, itemTotal, tariff, scale);
  }

  private void maybeRecalculatePrices() {
    GridView gridView = getGridView();
    if (gridView == null || gridView.isEmpty()) {
      return;
    }

    double total = getItemTotal(gridView);

    if (!BeeUtils.isPositive(total)) {
      gridView.notifyWarning(Data.getViewCaption(VIEW_TRADE_ACT_ITEMS),
          Localized.getConstants().noData());
      return;
    }

    int toIndex = getDataIndex(COL_TA_SERVICE_TO);
    int tariffIndex = getDataIndex(COL_TA_SERVICE_TARIFF);
    int priceIndex = getDataIndex(COL_TRADE_ITEM_PRICE);

    int count = 0;

    for (IsRow row : gridView.getRowData()) {
      Double tariff = row.getDouble(tariffIndex);

      if (BeeUtils.isPositive(tariff)) {
        Double price =
            calculatePrice(row.getDouble(priceIndex), row.getDate(toIndex), total, tariff);
        updatePrice(row.getId(), row.getVersion(), row.getString(priceIndex), price);

        count++;
      }
    }

    if (count > 0) {
      gridView.notifyInfo(Localized.getMessages().taRecalculatedPrices(count));
    } else {
      gridView.notifyWarning(Data.getColumnLabel(getViewName(), COL_TA_SERVICE_TARIFF),
          Localized.getConstants().noData());
    }
  }

  private boolean updatePrice(long rowId, long version, String oldValue, Double newPrice) {
    String newValue;
    if (BeeUtils.isDouble(newPrice)) {
      Integer scale = Data.getColumnScale(getViewName(), COL_TRADE_ITEM_PRICE);
      newValue = BeeUtils.toString(newPrice, scale);
    } else {
      newValue = null;
    }

    if (Objects.equals(oldValue, newValue)) {
      return false;

    } else {
      Queries.updateCellAndFire(getViewName(), rowId, version, COL_TRADE_ITEM_PRICE,
          oldValue, newValue);

      return true;
    }
  }
}
