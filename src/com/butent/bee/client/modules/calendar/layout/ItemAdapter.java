package com.butent.bee.client.modules.calendar.layout;

import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter {

  private final CalendarItem item;
  private final int dayMinutesStart;
  private final int dayMinutesEnd;

  private final List<TimeBlock> intersectingBlocks = new ArrayList<>();

  private int cellStart;
  private int cellSpan;
  private int columnStart = -1;
  private int columnSpan;

  private double cellPercentFill;
  private double cellPercentStart;
  private double top;
  private double left;
  private double width;
  private double height;

  public ItemAdapter(CalendarItem item) {
    this.item = item;

    DateTime start = item.getStartTime();
    DateTime end = item.getEndTime();

    this.dayMinutesStart = TimeUtils.minutesSinceDayStarted(start);
    this.dayMinutesEnd = TimeUtils.sameDate(start, end)
        ? TimeUtils.minutesSinceDayStarted(end) : TimeUtils.MINUTES_PER_DAY;
  }

  public double getCellPercentFill() {
    return cellPercentFill;
  }

  public double getCellPercentStart() {
    return cellPercentStart;
  }

  public int getCellSpan() {
    return cellSpan;
  }

  public int getCellStart() {
    return cellStart;
  }

  public int getColumnSpan() {
    return columnSpan;
  }

  public int getColumnStart() {
    return columnStart;
  }

  public int getDayMinutesEnd() {
    return dayMinutesEnd;
  }

  public int getDayMinutesStart() {
    return dayMinutesStart;
  }

  public double getHeight() {
    return height;
  }

  public List<TimeBlock> getIntersectingBlocks() {
    return intersectingBlocks;
  }

  public CalendarItem getItem() {
    return item;
  }

  public double getLeft() {
    return left;
  }

  public double getTop() {
    return top;
  }

  public double getWidth() {
    return width;
  }

  public void setCellPercentFill(double cellPercentFill) {
    this.cellPercentFill = cellPercentFill;
  }

  public void setCellPercentStart(double cellPercentStart) {
    this.cellPercentStart = cellPercentStart;
  }

  public void setCellSpan(int cellSpan) {
    this.cellSpan = cellSpan;
  }

  public void setCellStart(int cellStart) {
    this.cellStart = cellStart;
  }

  public void setColumnSpan(int columnSpan) {
    this.columnSpan = columnSpan;
  }

  public void setColumnStart(int columnStart) {
    this.columnStart = columnStart;
  }

  public void setHeight(double height) {
    this.height = height;
  }

  public void setLeft(double left) {
    this.left = left;
  }

  public void setTop(double top) {
    this.top = top;
  }

  public void setWidth(double width) {
    this.width = width;
  }
}
