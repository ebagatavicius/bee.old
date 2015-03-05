package com.butent.bee.shared.modules.trade.acts;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class TradeActUtils {

  public static Double calculateServicePrice(Double defPrice, JustDate dateTo, Double itemTotal,
      Double tariff, Integer scale) {

    if (BeeUtils.isPositive(defPrice) && dateTo != null) {
      return defPrice;
    }

    Double price = BeeUtils.percent(itemTotal, tariff);

    if (BeeUtils.nonZero(price) && BeeUtils.isNonNegative(scale)) {
      return BeeUtils.round(price, scale);
    } else {
      return price;
    }
  }

  public static Range<DateTime> convertRange(Range<JustDate> range) {
    if (range == null) {
      return null;
    }

    DateTime start;

    if (range.hasLowerBound()) {
      if (range.lowerBoundType() == BoundType.OPEN) {
        start = TimeUtils.startOfNextDay(range.lowerEndpoint());
      } else {
        start = TimeUtils.startOfDay(range.lowerEndpoint());
      }
    } else {
      start = null;
    }

    DateTime end;

    if (range.hasUpperBound()) {
      if (range.upperBoundType() == BoundType.OPEN) {
        end = TimeUtils.startOfDay(range.upperEndpoint());
      } else {
        end = TimeUtils.startOfNextDay(range.upperEndpoint());
      }
    } else {
      end = null;
    }

    return createRange(start, end);
  }

  public static int countServiceDays(Range<DateTime> range, Collection<Integer> holidays) {
    return countServiceDays(range, holidays, BeeConst.UNDEF);
  }

  public static int countServiceDays(Range<DateTime> range, Collection<Integer> holidays, int dpw) {
    int days = 0;

    if (range != null && range.hasLowerBound() && range.hasUpperBound()) {
      int lower = range.lowerEndpoint().getDate().getDays();
      int upper = Math.max(range.upperEndpoint().getDate().getDays(), lower + 1);

      if (dpw >= TimeUtils.DAYS_PER_WEEK) {
        return upper - lower;
      }

      int dow = range.lowerEndpoint().getDow();

      for (int d = lower; d < upper; d++) {
        if ((dpw <= 0 || dow <= dpw) && (holidays == null || !holidays.contains(d))) {
          days++;
        }

        dow++;
        if (dow > TimeUtils.DAYS_PER_WEEK) {
          dow = 1;
        }
      }
    }

    return days;
  }

  public static Range<DateTime> createRange(HasDateValue start, HasDateValue end) {
    if (start == null) {
      return (end == null) ? null : Range.lessThan(end.getDateTime());

    } else if (end == null) {
      return Range.atLeast(start.getDateTime());

    } else {
      DateTime lower = start.getDateTime();
      DateTime upper = end.getDateTime();

      if (lower.getTime() < upper.getTime()) {
        return Range.closedOpen(lower, upper);
      } else {
        return Range.singleton(lower);
      }
    }
  }

  public static Range<DateTime> createServiceRange(JustDate serviceFrom, JustDate serviceTo,
      TradeActTimeUnit timeUnit, Range<DateTime> builderRange, Range<DateTime> actRange) {

    if (timeUnit == null) {
      DateTime date;

      if (serviceFrom != null) {
        date = serviceFrom.getDateTime();
      } else if (serviceTo != null) {
        date = serviceTo.getDateTime();
      } else {
        date = actRange.lowerEndpoint();
      }

      if (builderRange.contains(date)) {
        return Range.singleton(date);
      } else {
        return null;
      }

    } else {
      DateTime start;
      if (serviceFrom != null) {
        start = TimeUtils.startOfDay(serviceFrom);
      } else {
        start = actRange.lowerEndpoint();
      }

      DateTime end;
      if (serviceTo != null) {
        end = TimeUtils.startOfDay(serviceTo);
      } else if (actRange.hasUpperBound()) {
        end = actRange.upperEndpoint();
      } else {
        end = null;
      }

      Range<DateTime> serviceRange = createRange(start, end);

      if (BeeUtils.intersects(serviceRange, builderRange)) {
        return serviceRange.intersection(builderRange);
      } else {
        return null;
      }
    }
  }

  public static Map<Long, Double> getItemQuantities(BeeRowSet rowSet) {
    Map<Long, Double> result = new HashMap<>();

    if (!DataUtils.isEmpty(rowSet)) {
      int itemIndex = rowSet.getColumnIndex(COL_TA_ITEM);
      int qtyIndex = rowSet.getColumnIndex(COL_TRADE_ITEM_QUANTITY);

      for (BeeRow row : rowSet) {
        Long item = row.getLong(itemIndex);
        Double qty = row.getDouble(qtyIndex);

        if (DataUtils.isId(item) && BeeUtils.isPositive(qty)) {
          result.put(item, qty);
        }
      }
    }

    return result;
  }

  public static DateTime getLower(Range<DateTime> range) {
    return (range == null) ? null : range.lowerEndpoint();
  }

  public static double getMonthFactor(Range<DateTime> range, Collection<Integer> holidays) {
    double factor = BeeConst.DOUBLE_ZERO;

    if (range != null && range.hasLowerBound() && range.hasUpperBound()) {
      JustDate min = range.lowerEndpoint().getDate();

      JustDate max = range.upperEndpoint().getDate();
      if (BeeUtils.isMore(max, min)) {
        max = TimeUtils.previousDay(max);
      }

      int minDay = min.getDays();
      int maxDay = max.getDays();

      for (YearMonth ym = YearMonth.of(min); BeeUtils.isLeq(ym, YearMonth.of(max)); ym =
          ym.nextMonth()) {

        int days = 0;
        int size = 0;

        for (int d = ym.getDate().getDays(); d <= ym.getLast().getDays(); d++) {
          if (holidays == null || !holidays.contains(d)) {
            if (d >= minDay && d <= maxDay) {
              days++;
            }

            size++;
          }
        }

        if (days > 0) {
          factor += BeeUtils.div(days, size);
        }
      }
    }

    return factor;
  }

  public static DateTime getUpper(Range<DateTime> range) {
    return (range == null) ? null : range.upperEndpoint();
  }

  public static double roundAmount(Double amount) {
    if (BeeUtils.nonZero(amount)) {
      return BeeUtils.round(amount, 2);
    } else {
      return BeeConst.DOUBLE_ZERO;
    }
  }

  public static double roundPrice(Double price) {
    return roundAmount(price);
  }

  public static Double serviceAmount(Double quantity, Double price, Double discount,
      TradeActTimeUnit timeUnit, Double factor) {

    if (BeeUtils.isPositive(quantity) && BeeUtils.isPositive(price)) {
      double p = price;
      if (BeeUtils.nonZero(discount)) {
        p = BeeUtils.minusPercent(p, discount);
      }

      double amount = roundAmount(roundPrice(p) * quantity);
      if (timeUnit != null && BeeUtils.isPositive(factor)) {
        amount = roundAmount(amount * factor);
      }

      return amount;

    } else {
      return null;
    }
  }

  public static boolean validDpw(Integer dpw) {
    return dpw != null && dpw >= DPW_MIN && dpw <= DPW_MAX;
  }

  private TradeActUtils() {
  }
}
