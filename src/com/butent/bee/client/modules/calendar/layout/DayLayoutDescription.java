package com.butent.bee.client.modules.calendar.layout;

import com.butent.bee.shared.modules.calendar.CalendarItem;

import java.util.ArrayList;
import java.util.List;

public class DayLayoutDescription {

  private final List<CalendarItem> items = new ArrayList<>();

  private final int dayIndex;

  public DayLayoutDescription(int dayIndex) {
    this.dayIndex = dayIndex;
  }

  public void addItem(CalendarItem item) {
    items.add(item);
  }

  public List<CalendarItem> getItems() {
    return items;
  }

  public int getDayIndex() {
    return dayIndex;
  }

  public int getItemCount() {
    return items.size();
  }
}