package com.butent.bee.client.i18n;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Table;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.DataEvent;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * currency exchange.
 */
public final class Money implements HandlesAllDataEvents {

  private static final BeeLogger logger = LogUtils.getLogger(Money.class);

  private static final Money INSTANCE = new Money();

  public static boolean canExchange(Long from, Long to) {
    return DataUtils.isId(from) && DataUtils.isId(to) && !Objects.equals(from, to)
        && INSTANCE.containsCurrency(from) && INSTANCE.containsCurrency(to);
  }

  public static double exchange(long from, long to, double v, DateTime dt) {
    if (from == to) {
      return v;

    } else {
      long time = (dt == null) ? System.currentTimeMillis() : dt.getTime();
      return v * INSTANCE.getRate(from, time) / INSTANCE.getRate(to, time);
    }
  }

  public static int exchange(long from, long to, DateTime dt,
      String viewName, Collection<? extends IsRow> rows, String colName) {

    Assert.notEmpty(viewName);
    Assert.notNull(rows);
    Assert.notEmpty(colName);

    int index = Data.getColumnIndex(viewName, colName);
    Assert.nonNegative(index);

    Integer scale = Data.getColumnScale(viewName, colName);
    Assert.notNull(scale);

    BeeRowSet rowSet = new BeeRowSet(viewName,
        Collections.singletonList(Data.getColumns(viewName).get(index)));

    for (IsRow row : rows) {
      if (DataUtils.hasId(row)) {
        Double v = row.getDouble(index);

        if (BeeUtils.nonZero(v)) {
          String oldValue = row.getString(index);
          String newValue = BeeUtils.toString(exchange(from, to, v, dt), scale);

          if (!Objects.equals(oldValue, newValue)) {
            BeeRow upd = new BeeRow(row.getId(), row.getVersion(),
                Collections.singletonList(oldValue));
            upd.preliminaryUpdate(0, newValue);

            rowSet.addRow(upd);
          }
        }
      }
    }

    if (!rowSet.isEmpty()) {
      Queries.updateRows(rowSet);
    }

    return rowSet.getNumberOfRows();
  }

  public static List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> info = new ArrayList<>();

    Set<Long> currencies = INSTANCE.rates.keySet();
    info.add(new ExtendedProperty("rated currencies", BeeUtils.bracket(currencies.size())));

    for (Long currency : currencies) {
      List<Pair<Long, Double>> values = INSTANCE.rates.get(currency);

      info.add(new ExtendedProperty("currency", BeeUtils.joinWords("id", currency),
          BeeUtils.bracket(values.size())));

      for (int i = 0; i < values.size(); i++) {
        Pair<Long, Double> rate = values.get(i);

        info.add(new ExtendedProperty(BeeUtils.joinWords("rate", currency, BeeUtils.bracket(i)),
            Format.renderDateTime(rate.getA()), BeeUtils.toString(rate.getB(), 10)));
      }
    }

    return info;
  }

  public static Table<Long, Long, Double> getRates(DateTime dt) {
    Table<Long, Long, Double> result = HashBasedTable.create();
    if (INSTANCE.rates.isEmpty()) {
      return result;
    }

    List<Long> currencies = new ArrayList<>(INSTANCE.rates.keySet());
    if (currencies.size() > 1) {
      Collections.sort(currencies);
    }

    for (Long from : currencies) {
      for (Long to : currencies) {
        result.put(from, to, exchange(from, to, BeeConst.DOUBLE_ONE, dt));
      }
    }

    return result;
  }

  public static void load(String serialized) {
    Assert.notEmpty(serialized);
    INSTANCE.updateRates(BeeRowSet.restore(serialized));
    logger.info("rates", INSTANCE.rates.size());
  }

  private final ListMultimap<Long, Pair<Long, Double>> rates = ArrayListMultimap.create();

  private Money() {
    BeeKeeper.getBus().registerDataHandler(this, false);
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    onDataEvent(event);
  }

  private boolean containsCurrency(Long currency) {
    return rates.containsKey(currency);
  }

  private double getRate(long currency, long time) {
    if (rates.containsKey(currency)) {
      for (Pair<Long, Double> value : rates.get(currency)) {
        if (time >= value.getA()) {
          return value.getB();
        }
      }
    }
    return BeeConst.DOUBLE_ONE;
  }

  private void onDataEvent(DataEvent event) {
    if (event.hasView(VIEW_CURRENCY_RATES)) {
      refresh();
    }
  }

  private void refresh() {
    Queries.getRowSet(VIEW_CURRENCY_RATES, null, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        updateRates(result);
        logger.debug("refreshed rates", result.getNumberOfRows(), rates.size());
      }
    });
  }

  private void updateRates(BeeRowSet rowSet) {
    if (!rates.isEmpty()) {
      rates.clear();
    }

    if (DataUtils.isEmpty(rowSet)) {
      return;
    }

    int currencyIndex = rowSet.getColumnIndex(COL_CURRENCY_RATE_CURRENCY);
    int dateIndex = rowSet.getColumnIndex(COL_CURRENCY_RATE_DATE);
    int quantityIndex = rowSet.getColumnIndex(COL_CURRENCY_RATE_QUANTITY);
    int rateIndex = rowSet.getColumnIndex(COL_CURRENCY_RATE);

    for (BeeRow row : rowSet) {
      Long currency = row.getLong(currencyIndex);
      Long time = row.getLong(dateIndex);
      Integer quantity = row.getInteger(quantityIndex);
      Double rate = row.getDouble(rateIndex);

      if (DataUtils.isId(currency) && time != null && BeeUtils.isPositive(rate)) {
        if (quantity != null && quantity > 1) {
          rate /= quantity;
        }

        if (rates.containsKey(currency)) {
          Pair<Long, Double> last = BeeUtils.getLast(rates.get(currency));
          if (rate.equals(last.getB())) {
            rates.remove(currency, last);
          }
        }

        rates.put(currency, Pair.of(time, rate));
      }
    }
  }
}
