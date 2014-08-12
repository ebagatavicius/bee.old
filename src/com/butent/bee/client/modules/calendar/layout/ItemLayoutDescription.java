package com.butent.bee.client.modules.calendar.layout;

import com.google.common.collect.Range;

import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.utils.BeeUtils;

public class ItemLayoutDescription {

  private final CalendarItem item;

  private int fromWeekDay;
  private int toWeekDay;

  public ItemLayoutDescription(int weekDay, CalendarItem item) {
    this(weekDay, weekDay, item);
  }

  public ItemLayoutDescription(int fromWeekDay, int toWeekDay, CalendarItem item) {
    this.toWeekDay = toWeekDay;
    this.fromWeekDay = fromWeekDay;
    this.item = item;
  }

  public CalendarItem getItem() {
    return item;
  }

  public int getWeekEndDay() {
    return toWeekDay;
  }

  public int getWeekStartDay() {
    return fromWeekDay;
  }

  public boolean overlaps(int from, int to) {
    return BeeUtils.intersects(getRange(), Range.closed(from, to));
  }

  public boolean spansMoreThanADay() {
    return fromWeekDay < toWeekDay;
  }

  public ItemLayoutDescription split() {
    ItemLayoutDescription secondPart = null;
    if (spansMoreThanADay()) {
      secondPart = new ItemLayoutDescription(fromWeekDay + 1, toWeekDay, item);
      this.toWeekDay = this.fromWeekDay;
    } else {
      secondPart = this;
    }
    return secondPart;
  }

  private Range<Integer> getRange() {
    return Range.closed(getWeekStartDay(), getWeekEndDay());
  }
}
