package com.butent.bee.shared.modules.trade.acts;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

public final class TradeActUtils {

  public static Double calculateServicePrice(Double itemTotal, Double tariff, Integer scale) {
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

  public static int countServiceDays(Range<DateTime> range) {
    if (range != null && range.hasLowerBound() && range.hasUpperBound()) {
      return Math.max(TimeUtils.dayDiff(range.lowerEndpoint(), range.upperEndpoint()), 1);
    } else {
      return 0;
    }
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

  public static double dpwToFactor(Integer dpw, int days, Integer minTerm) {
    if (validDpw(dpw) && days > 0) {
      int df = BeeUtils.round(BeeUtils.div(days, TimeUtils.DAYS_PER_WEEK) * dpw);

      if (BeeUtils.isPositive(minTerm)) {
        return Math.max(df, minTerm);
      } else {
        return df;
      }

    } else {
      return BeeConst.DOUBLE_ZERO;
    }
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
