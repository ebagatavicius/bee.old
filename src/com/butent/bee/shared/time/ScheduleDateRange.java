package com.butent.bee.shared.time;

public class ScheduleDateRange {

  public static ScheduleDateRange maybeCreate(JustDate from, JustDate until,
      ScheduleDateMode mode) {

    if (from != null && mode != null) {
      DateRange range;
      if (until == null || from.equals(until)) {
        range = DateRange.day(from);
      } else if (TimeUtils.isLess(from, until)) {
        range = DateRange.closed(from, until);
      } else {
        range = null;
      }

      if (range != null) {
        return new ScheduleDateRange(range, mode);
      }
    }

    return null;
  }

  private final DateRange range;
  private final ScheduleDateMode mode;

  public ScheduleDateRange(DateRange range, ScheduleDateMode mode) {
    this.range = range;
    this.mode = mode;
  }

  public DateRange getRange() {
    return range;
  }

  public ScheduleDateMode getMode() {
    return mode;
  }
}
