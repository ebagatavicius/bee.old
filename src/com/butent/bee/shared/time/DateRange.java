package com.butent.bee.shared.time;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import java.util.ArrayList;
import java.util.List;

public final class DateRange implements HasDateRange {

  public static DateRange closed(JustDate min, JustDate max) {
    return new DateRange(Range.closed(min, max));
  }

  public static DateRange day(JustDate date) {
    return closed(date, date);
  }

  private final Range<JustDate> range;

  private DateRange(Range<JustDate> range) {
    this.range = range;
  }

  public boolean contains(JustDate date) {
    return date != null && range.contains(date);
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

  public int size() {
    return getMaxDays() - getMinDays() + 1;
  }

  @Override
  public String toString() {
    return range.toString();
  }
}
