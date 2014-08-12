package com.butent.bee.client.modules.calendar.layout;

import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

public class WeekLayoutDescription {

  public enum WidgetPart {
    FIRST_WEEK, IN_BETWEEN, LAST_WEEK
  }

  public static final int FIRST_DAY = 0;
  public static final int LAST_DAY = 6;

  private final ItemStackingManager stackingManager;

  private final DayLayoutDescription[] days;

  private final JustDate firstDate;

  public WeekLayoutDescription(JustDate firstDate, int maxLayer) {
    this.firstDate = firstDate;

    this.days = new DayLayoutDescription[7];

    this.stackingManager = new ItemStackingManager(maxLayer);
  }

  public void addItem(CalendarItem item) {
    int dayOfWeek = dayInWeek(item.getStartTime(), false);
    initDay(dayOfWeek).addItem(item);
  }

  public void addMultiDayItem(CalendarItem item) {
    int weekStartDay = dayInWeek(item.getStartTime(), false);
    int weekEndDay = dayInWeek(item.getEndTime(), true);

    stackingManager.assignLayer(weekStartDay, weekEndDay, item);
  }

  public void addMultiWeekItem(CalendarItem item, WidgetPart part) {
    switch (part) {
      case FIRST_WEEK:
        int weekStartDay = dayInWeek(item.getStartTime(), false);
        stackingManager.assignLayer(weekStartDay, LAST_DAY, item);
        break;
      case IN_BETWEEN:
        stackingManager.assignLayer(FIRST_DAY, LAST_DAY, item);
        break;
      case LAST_WEEK:
        int weekEndDay = dayInWeek(item.getEndTime(), true);
        stackingManager.assignLayer(FIRST_DAY, weekEndDay, item);
        break;
    }
  }

  public DayLayoutDescription getDayLayoutDescription(int day) {
    return days[day];
  }

  public ItemStackingManager getTopItemsManager() {
    return stackingManager;
  }

  private int dayInWeek(DateTime dateTime, boolean end) {
    int diff = TimeUtils.dayDiff(firstDate, dateTime);
    if (end && TimeUtils.minutesSinceDayStarted(dateTime) == 0) {
      diff--;
    }
    return BeeUtils.clamp(diff, FIRST_DAY, LAST_DAY);
  }

  private DayLayoutDescription initDay(int day) {
    if (days[day] == null) {
      days[day] = new DayLayoutDescription(day);
    }
    return days[day];
  }
}