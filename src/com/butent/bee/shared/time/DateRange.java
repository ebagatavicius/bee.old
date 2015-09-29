package com.butent.bee.shared.time;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public final class DateRange implements HasDateRange, BeeSerializable {

  public static DateRange closed(JustDate min, JustDate max) {
    return new DateRange(Range.closed(min, max));
  }

  public static DateRange day(JustDate date) {
    return closed(date, date);
  }

  public static boolean isValidClosedRange(JustDate min, JustDate max) {
    return min != null && max != null && min.getDays() <= max.getDays();
  }

  public static DateRange restore(String s) {
    Range<JustDate> r = deserializeRange(s);
    return (r == null) ? null : new DateRange(r);
  }

  private static Range<JustDate> deserializeRange(String s) {
    String lower = BeeUtils.getPrefix(s, BeeConst.DEFAULT_LIST_SEPARATOR);
    String upper = BeeUtils.getSuffix(s, BeeConst.DEFAULT_LIST_SEPARATOR);

    if (BeeUtils.isInt(lower) && BeeUtils.isInt(upper)) {
      return Range.closed(new JustDate(BeeUtils.toInt(lower)), new JustDate(BeeUtils.toInt(upper)));
    } else {
      return null;
    }
  }

  private Range<JustDate> range;

  private DateRange(Range<JustDate> range) {
    this.range = range;
  }

  public boolean contains(JustDate date) {
    return date != null && range.contains(date);
  }

  @Override
  public void deserialize(String s) {
    Range<JustDate> r = deserializeRange(s);

    if (r != null) {
      this.range = r;
    }
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof DateRange && range.equals(((DateRange) obj).range);
  }

  public JustDate getMaxDate() {
    return new JustDate(getMaxDays());
  }

  public int getMaxDays() {
    int max = range.upperEndpoint().getDays();
    if (range.upperBoundType() == BoundType.OPEN) {
      max--;
    }

    return max;
  }

  public JustDate getMinDate() {
    return new JustDate(getMinDays());
  }

  public int getMinDays() {
    int min = range.lowerEndpoint().getDays();
    if (range.lowerBoundType() == BoundType.OPEN) {
      min++;
    }

    return min;
  }

  @Override
  public Range<JustDate> getRange() {
    return range;
  }

  public List<JustDate> getValues() {
    List<JustDate> values = new ArrayList<>();

    int min = getMinDays();
    int max = getMaxDays();

    for (int days = min; days <= max; days++) {
      values.add(new JustDate(days));
    }

    return values;
  }

  @Override
  public int hashCode() {
    return range.hashCode();
  }

  public DateRange intersection(DateRange other) {
    if (intersects(other)) {
      return new DateRange(range.intersection(other.range));
    } else {
      return null;
    }
  }

  public boolean intersects(DateRange other) {
    return other != null && BeeUtils.intersects(range, other.range);
  }

  public boolean isConnected(DateRange other) {
    return other != null && range.isConnected(other.range);
  }

  @Override
  public String serialize() {
    return BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR, getMinDays(), getMaxDays());
  }

  public int size() {
    return getMaxDays() - getMinDays() + 1;
  }

  @Override
  public String toString() {
    return range.toString();
  }
}
