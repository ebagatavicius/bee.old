package com.butent.bee.client.modules.cars;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_CURRENCY;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.DataChangeCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.client.modules.classifiers.ClassifierKeeper;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.ParentRowRefreshGrid;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.TriConsumer;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class PriceRecalculator extends Button {

  public enum RecalculatorData {
    ITEM, QUANTITY, PRICE, CURRENCY
  }

  private final ParentRowRefreshGrid grid;

  public PriceRecalculator(ParentRowRefreshGrid grid) {
    super(Localized.dictionary().recalculateTradeItemPriceCaption());
    setCommand(this::recalculatePrice);
    this.grid = Assert.notNull(grid);
  }

  public static void getPriceAndDiscount(GridView gridView, int cnt,
      BiFunction<Integer, RecalculatorData, String> dataSupplier,
      TriConsumer<Integer, Double, Double> consumer, Runnable finalizer) {
    FormView parentForm = ViewHelper.getForm(gridView);
    Map<String, String> options = new HashMap<>();

    options.put(COL_DISCOUNT_COMPANY, parentForm.getStringValue(COL_CUSTOMER));
    options.put(Service.VAR_TIME, parentForm.getStringValue(COL_DATE));
    options.put(COL_DISCOUNT_CURRENCY, parentForm.getStringValue(COL_CURRENCY));
    options.put(COL_DISCOUNT_WAREHOUSE, parentForm.getStringValue(COL_WAREHOUSE));
    options.put(COL_MODEL, parentForm.getStringValue(COL_MODEL));
    options.put(COL_PRODUCTION_DATE, parentForm.getStringValue(COL_PRODUCTION_DATE));

    Latch latch = new Latch(cnt);

    for (int i = 0; i < cnt; i++) {
      int idx = i;
      Long item = BeeUtils.toLongOrNull(dataSupplier.apply(idx, RecalculatorData.ITEM));
      options.put(Service.VAR_QTY, dataSupplier.apply(idx, RecalculatorData.QUANTITY));

      ClassifierKeeper.getPriceAndDiscount(item, options, (price, discount) -> {
        Double newPrice = BeeUtils.toDoubleOrNull(dataSupplier.apply(idx, RecalculatorData.PRICE));

        if (BeeUtils.isPositive(newPrice)) {
          Long currency = BeeUtils.toLongOrNull(dataSupplier.apply(idx, RecalculatorData.CURRENCY));

          if (DataUtils.isId(currency)) {
            newPrice = Money.exchange(currency, parentForm.getLongValue(COL_CURRENCY), newPrice,
                parentForm.getDateTimeValue(COL_DATE));
          } else {
            newPrice = null;
          }
        }
        if (BeeUtils.isPositive(price) && (!BeeUtils.isPositive(newPrice)
            || BeeUtils.isLess(price, newPrice))) {
          newPrice = price;
        }
        consumer.accept(idx, Localized.normalizeMoney(newPrice), discount);

        latch.decrement();

        if (latch.isOpen()) {
          finalizer.run();
        }
      });
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    setStyleName(StyleUtils.NAME_DISABLED, !enabled);
    super.setEnabled(enabled);
  }

  private void recalculatePrice() {
    GridView gridView = grid.getGridView();

    if (gridView.isReadOnly() || !gridView.isEnabled()) {
      return;
    }
    List<? extends IsRow> rows = gridView.getRowData().stream()
        .filter(row -> gridView.isRowEditable(row, null))
        .collect(Collectors.toList());

    if (BeeUtils.isEmpty(rows)) {
      gridView.notifyWarning(Localized.dictionary().noData());
      return;
    }
    String caption = Localized.dictionary().recalculateTradeItemPriceCaption();

    List<String> options = new ArrayList<>();
    options.add(BeeUtils.joinWords(grid.getGridPresenter().getCaption(),
        BeeUtils.parenthesize(rows.size())));
    IsRow activeRow = gridView.getActiveRow();

    if (activeRow == null || !gridView.isRowEditable(activeRow, null)) {
      Global.confirm(caption, Icon.QUESTION, options, () -> recalculatePrice(rows));
    } else {
      options.add(BeeUtils.joinWords(activeRow.getString(gridView.getDataIndex(ALS_ITEM_NAME)),
          activeRow.getString(gridView.getDataIndex(COL_TRADE_ITEM_ARTICLE))));

      Global.choiceWithCancel(caption, null, options, choice -> {
        switch (choice) {
          case 0:
            recalculatePrice(rows);
            break;
          case 1:
            recalculatePrice(Collections.singletonList(activeRow));
            break;
        }
      });
    }
  }

  private void recalculatePrice(List<? extends IsRow> rows) {
    setEnabled(false);
    GridView gridView = grid.getGridView();
    Map<Long, Pair<String, String>> priceInfo = new HashMap<>();

    BeeRowSet rowSet = new BeeRowSet(gridView.getViewName(), Data.getColumns(gridView.getViewName(),
        Arrays.asList(COL_PRICE, COL_TRADE_DOCUMENT_ITEM_DISCOUNT,
            COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT)));

    Runnable executor = () ->
        getPriceAndDiscount(gridView, rows.size(), (idx, info) -> {
              switch (info) {
                case ITEM:
                  return rows.get(idx).getString(gridView.getDataIndex(COL_ITEM));
                case QUANTITY:
                  return rows.get(idx).getString(gridView.getDataIndex(COL_TRADE_ITEM_QUANTITY));
                case PRICE:
                case CURRENCY:
                  Pair<String, String> pair = priceInfo.get(rows.get(idx)
                      .getLong(gridView.getDataIndex(COL_ITEM)));

                  if (Objects.nonNull(pair)) {
                    return Objects.equals(info, RecalculatorData.PRICE) ? pair.getA() : pair.getB();
                  }
                  break;
              }
              return null;
            },
            (idx, price, discount) -> rowSet
                .addRow(rows.get(idx).getId(), rows.get(idx).getVersion(),
                    Queries.asList(price, discount, BeeUtils.isPositive(discount))),
            () -> Queries.updateRows(rowSet, new DataChangeCallback(rowSet.getViewName()) {
              @Override
              public void onSuccess(RowInfoList result) {
                setEnabled(true);
                grid.previewModify(null);
                super.onSuccess(result);
              }
            }));
    if (Objects.equals(gridView.getGridName(), GRID_SERVICE_ORDER_JOBS)) {
      Filter filter = Filter.isNull(COL_MODEL);
      FormView parentForm = ViewHelper.getForm(gridView);

      if (DataUtils.isId(parentForm.getLongValue(COL_MODEL))) {
        filter = Filter.or(filter, Filter.equals(COL_MODEL, parentForm.getLongValue(COL_MODEL)));
      }
      filter = Filter.and(Filter.any(COL_ITEM, rows.stream()
          .map(row -> row.getLong(gridView.getDataIndex(COL_ITEM)))
          .collect(Collectors.toList())), filter);

      Queries.getRowSet(TBL_CAR_JOBS, null, filter, new Queries.RowSetCallback() {
        @Override
        public void onSuccess(BeeRowSet result) {
          result.forEach(beeRow -> priceInfo.put(DataUtils.getLong(result, beeRow, COL_ITEM),
              Pair.of(BeeUtils.nvl(DataUtils.getString(result, beeRow, COL_MODEL + COL_PRICE),
                  DataUtils.getString(result, beeRow, COL_PRICE)),
                  BeeUtils.nvl(DataUtils.getString(result, beeRow, COL_MODEL + COL_CURRENCY),
                      DataUtils.getString(result, beeRow, COL_CURRENCY)))));
          executor.run();
        }
      });
    } else {
      executor.run();
    }
  }
}
