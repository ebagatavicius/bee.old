package com.butent.bee.client.modules.calendar.layout;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class TimeBlock {

  private final List<ItemAdapter> adapters = Lists.newArrayList();
  private final Map<Integer, Integer> occupiedColumns = Maps.newHashMap();

  private int totalColumns = 1;
  private int order;

  private int start;
  private int end;

  private int top;
  private int bottom;

  public List<ItemAdapter> getAdapters() {
    return adapters;
  }

  public int getBottom() {
    return bottom;
  }

  public int getEnd() {
    return end;
  }

  public int getFirstAvailableColumn() {
    int col = 0;
    while (true) {
      if (occupiedColumns.containsKey(col)) {
        col++;
      } else {
        return col;
      }
    }
  }

  public Map<Integer, Integer> getOccupiedColumns() {
    return occupiedColumns;
  }

  public int getOrder() {
    return order;
  }

  public int getStart() {
    return start;
  }

  public int getTop() {
    return top;
  }

  public int getTotalColumns() {
    return totalColumns;
  }

  public boolean intersectsWith(ItemAdapter appt) {
    return intersectsWith(appt.getDayMinutesStart(), appt.getDayMinutesEnd());
  }

  public boolean intersectsWith(int apptStart, int apptEnd) {
    if (apptStart >= this.getStart() && apptStart < this.getEnd()) {
      return true;
    }
    return apptEnd > this.getStart() && apptStart < this.getStart();
  }

  public void setBottom(int bottom) {
    this.bottom = bottom;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public void setTop(int top) {
    this.top = top;
  }

  public void setTotalColumns(int totalColumns) {
    this.totalColumns = totalColumns;
  }
}
