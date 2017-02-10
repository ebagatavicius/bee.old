package com.butent.bee.shared.time;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasRange;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public final class MonthRange implements HasRange<YearMonth>, BeeSerializable {

  private static final YearMonth DEFAULT_MIN_YM = new YearMonth(1800, 1);
  private static final YearMonth DEFAULT_MAX_YM = new YearMonth(2999, 12);

  public static MonthRange all() {
    return new MonthRange(Range.closed(DEFAULT_MIN_YM, DEFAULT_MAX_YM));
  }

  public static MonthRange closed(Integer yearFrom, Integer monthFrom,
      Integer yearUntil, Integer monthUntil) {

    YearMonth min = null;
    YearMonth max = null;

    if (yearFrom != null) {
      int y = TimeUtils.normalizeYear(yearFrom);
      int m = BeeUtils.clamp(BeeUtils.unbox(monthFrom), 1, 12);

      min = new YearMonth(y, m);
    }

    if (yearUntil == null) {
      if (min != null && TimeUtils.isMonth(monthUntil)) {
        max = new YearMonth(min.getYear(), monthUntil);
      }

    } else {
      int y = TimeUtils.normalizeYear(yearUntil);
      int m = TimeUtils.isMonth(monthUntil) ? monthUntil : 12;

      max = new YearMonth(y, m);
    }

    return MonthRange.closed(min, max);
  }

  public static MonthRange closed(YearMonth min, YearMonth max) {
    YearMonth lower = (min == null) ? DEFAULT_MIN_YM : min;
    YearMonth upper = (max == null) ? DEFAULT_MAX_YM : max;

    if (isValidClosedRange(lower, upper)) {
      return new MonthRange(Range.closed(lower, upper));
    } else {
      return null;
    }
  }

  public static MonthRange month(YearMonth ym) {
    if (ym == null) {
      return null;
    } else {
      return closed(ym, ym);
    }
  }

  public static MonthRange quarter(YearQuarter yq) {
    if (yq == null) {
      return null;
    } else {
      return closed(yq.getYear(), yq.getFirstMonth(), yq.getYear(), yq.getLastMonth());
    }
  }

  public static MonthRange year(Integer year) {
    if (year == null) {
      return null;
    } else {
      return closed(year, 1, year, 12);
    }
  }

  public static boolean isValidClosedRange(YearMonth min, YearMonth max) {
    return min != null && max != null && BeeUtils.isLeq(min, max);
  }

  public static MonthRange restore(String s) {
    Range<YearMonth> r = deserializeRange(s);
    return (r == null) ? null : new MonthRange(r);
  }

  private static Range<YearMonth> deserializeRange(String s) {
    String lower = BeeUtils.getPrefix(s, BeeConst.DEFAULT_LIST_SEPARATOR);
    String upper = BeeUtils.getSuffix(s, BeeConst.DEFAULT_LIST_SEPARATOR);

    YearMonth min = YearMonth.parse(lower);
    YearMonth max = YearMonth.parse(upper);

    if (isValidClosedRange(min, max)) {
      return Range.closed(min, max);
    } else {
      return null;
    }
  }

  private Range<YearMonth> range;

  private MonthRange(Range<YearMonth> range) {
    this.range = range;
  }

  public boolean contains(YearMonth ym) {
    return ym != null && range.contains(ym);
  }

  @Override
  public void deserialize(String s) {
    Range<YearMonth> r = deserializeRange(s);

    if (r != null) {
      this.range = r;
    }
  }

  public boolean encloses(MonthRange other) {
    return other != null && range.encloses(other.range);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof MonthRange && range.equals(((MonthRange) obj).range);
  }

  public YearMonth getMaxMonth() {
    YearMonth ym = YearMonth.copyOf(range.upperEndpoint());
    if (range.upperBoundType() == BoundType.OPEN) {
      ym.shiftMonth(-1);
    }
    return ym;
  }

  public YearMonth getMinMonth() {
    YearMonth ym = YearMonth.copyOf(range.lowerEndpoint());
    if (range.lowerBoundType() == BoundType.OPEN) {
      ym.shiftMonth(1);
    }
    return ym;
  }

  @Override
  public Range<YearMonth> getRange() {
    return range;
  }

  public List<YearMonth> getValues() {
    List<YearMonth> values = new ArrayList<>();

    YearMonth min = getMinMonth();
    YearMonth max = getMaxMonth();

    for (YearMonth ym = min; BeeUtils.isLeq(ym, max); ym = ym.nextMonth()) {
      values.add(ym);
    }

    return values;
  }

  @Override
  public int hashCode() {
    return range.hashCode();
  }

  public MonthRange intersection(MonthRange other) {
    if (intersects(other)) {
      return new MonthRange(range.intersection(other.range));
    } else {
      return null;
    }
  }

  public boolean intersects(MonthRange other) {
    return other != null && BeeUtils.intersects(range, other.range);
  }

  public boolean isConnected(MonthRange other) {
    return other != null && range.isConnected(other.range);
  }

  @Override
  public String serialize() {
    return BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR,
        getMinMonth().serialize(), getMaxMonth().serialize());
  }

  public int size() {
    return TimeUtils.monthDiff(getMinMonth(), getMaxMonth()) + 1;
  }

  @Override
  public String toString() {
    return range.toString();
  }
}
