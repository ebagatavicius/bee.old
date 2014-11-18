package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Range;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class CalendarUtils {

  public static List<CalendarItem> filterByAttendee(Collection<CalendarItem> input, long id) {
    List<CalendarItem> result = new ArrayList<>();

    for (CalendarItem item : input) {
      switch (item.getItemType()) {
        case APPOINTMENT:
          if (((Appointment) item).getAttendees().contains(id)) {
            result.add(item);
          }
          break;

        case TASK:
          break;
      }
    }
    return result;
  }

  public static List<CalendarItem> filterByAttendees(Collection<CalendarItem> input,
      Collection<Long> attIds, boolean separate) {
    List<CalendarItem> result = new ArrayList<>();

    for (CalendarItem item : input) {
      switch (item.getItemType()) {
        case APPOINTMENT:
          Appointment appointment = (Appointment) item;

          for (Long id : appointment.getAttendees()) {
            if (attIds.contains(id)) {
              if (separate) {
                Appointment copy = new Appointment(appointment.getRow(), id);
                result.add(copy);
              } else {
                result.add(appointment);
                break;
              }
            }
          }
          break;

        case TASK:
          result.add(item);
          break;
      }
    }
    return result;
  }

  public static List<CalendarItem> filterByRange(Collection<CalendarItem> input, JustDate date,
      int days) {
    List<CalendarItem> result = new ArrayList<>();

    long min = TimeUtils.startOfDay(date).getTime();
    long max = TimeUtils.startOfDay(date, days).getTime();

    for (CalendarItem item : input) {
      if (intersects(item, min, max)) {
        result.add(item);
      }
    }
    return result;
  }

  public static List<CalendarItem> filterMulti(Collection<CalendarItem> input, JustDate date,
      int days) {
    List<CalendarItem> result = new ArrayList<>();

    long min = TimeUtils.startOfDay(date).getTime();
    long max = TimeUtils.startOfDay(date, days).getTime();

    for (CalendarItem item : input) {
      if (intersects(item, min, max) && item.isMultiDay()) {
        result.add(item);
      }
    }
    return result;
  }

  public static List<CalendarItem> filterMulti(Collection<CalendarItem> input, JustDate date,
      int days, Collection<Long> attIds, boolean separate) {
    List<CalendarItem> lst = filterMulti(input, date, days);
    if (lst.isEmpty()) {
      return lst;
    }
    return filterByAttendees(lst, attIds, separate);
  }

  public static List<CalendarItem> filterMulti(Collection<CalendarItem> input, JustDate date,
      int days, long id) {
    List<CalendarItem> lst = filterMulti(input, date, days);
    if (lst.isEmpty()) {
      return lst;
    }
    return filterByAttendee(lst, id);
  }

  public static List<CalendarItem> filterSimple(Collection<CalendarItem> input, JustDate date) {
    List<CalendarItem> result = new ArrayList<>();

    long min = TimeUtils.startOfDay(date).getTime();
    long max = TimeUtils.startOfDay(date, 1).getTime();

    for (CalendarItem item : input) {
      if (item.getStartMillis() >= min && item.getEndMillis() <= max) {
        result.add(item);
      }
    }
    return result;
  }

  public static List<CalendarItem> filterSimple(Collection<CalendarItem> input, JustDate date,
      Collection<Long> attIds, boolean separate) {
    List<CalendarItem> lst = filterSimple(input, date);
    if (lst.isEmpty()) {
      return lst;
    }
    return filterByAttendees(lst, attIds, separate);
  }

  public static List<CalendarItem> filterSimple(Collection<CalendarItem> input, JustDate date,
      long id) {
    List<CalendarItem> lst = filterSimple(input, date);
    if (lst.isEmpty()) {
      return lst;
    }
    return filterByAttendee(lst, id);
  }

  public static ItemWidget findWidget(Collection<ItemWidget> widgets,
      Element element) {
    if (widgets.isEmpty() || element == null) {
      return null;
    }

    for (ItemWidget widget : widgets) {
      if (widget.getElement().isOrHasChild(element)) {
        return widget;
      }
    }
    return null;
  }

  public static int getColumnWidth(Widget widget, int columnCount) {
    int totalWidth = widget.getElement().getClientWidth();
    if (columnCount <= 1) {
      return totalWidth;
    } else {
      return totalWidth * (100 / columnCount) / 100;
    }
  }

  public static int getEndHour(Collection<ItemWidget> widgets) {
    int result = BeeConst.UNDEF;
    if (widgets == null) {
      return result;
    }

    for (ItemWidget widget : widgets) {
      if (!widget.isMulti()) {
        DateTime end = widget.getItem().getEndTime();
        int hour = end.getHour();
        if (end.getMinute() > 0) {
          hour++;
        } else if (hour == 0) {
          hour = TimeUtils.HOURS_PER_DAY;
        }

        if (BeeConst.isUndef(result)) {
          result = hour;
        } else {
          result = Math.max(result, hour);
        }
      }
    }
    return result;
  }

  public static int getEndPixels(CalendarSettings settings,
      Collection<ItemWidget> widgets) {
    Assert.notNull(settings);

    int hour = settings.getWorkingHourEnd();
    int maxHour = getEndHour(widgets);

    if (hour > 0 || maxHour > 0) {
      hour = Math.max(hour, maxHour);
    } else {
      hour = TimeUtils.HOURS_PER_DAY;
    }

    return hour * settings.getHourHeight();
  }

  public static int getIntervalStartPixels(DateTime dt, CalendarSettings settings) {
    Assert.notNull(dt);
    Assert.notNull(settings);

    int mpi = TimeUtils.MINUTES_PER_HOUR / settings.getIntervalsPerHour();

    return dt.getHour() * settings.getHourHeight()
        + dt.getMinute() / mpi * settings.getPixelsPerInterval();
  }

  public static ItemWidget getItemWidget(Widget child) {
    for (Widget widget = child; widget != null; widget = widget.getParent()) {
      if (widget instanceof ItemWidget) {
        return (ItemWidget) widget;
      }
    }
    return null;
  }

  public static int getMinutes(int y, CalendarSettings settings) {
    int hour = y / settings.getHourHeight();

    int interval = (y - hour * settings.getHourHeight()) / settings.getPixelsPerInterval();
    int minute = interval * TimeUtils.MINUTES_PER_HOUR / settings.getIntervalsPerHour();

    return hour * TimeUtils.MINUTES_PER_HOUR + minute;
  }

  public static int getNowY(CalendarSettings settings) {
    DateTime now = new DateTime();
    int hourHeight = settings.getHourHeight();

    return now.getHour() * hourHeight + now.getMinute() * hourHeight / TimeUtils.MINUTES_PER_HOUR;
  }

  public static int getStartHour(Collection<ItemWidget> widgets) {
    int result = BeeConst.UNDEF;
    if (widgets == null) {
      return result;
    }

    for (ItemWidget widget : widgets) {
      if (!widget.isMulti()) {
        int hour = widget.getItem().getStartTime().getHour();
        if (BeeConst.isUndef(result)) {
          result = hour;
        } else {
          result = Math.min(result, hour);
        }
      }
    }
    return result;
  }

  public static int getStartPixels(CalendarSettings settings,
      Collection<ItemWidget> widgets) {
    Assert.notNull(settings);

    int hour = settings.getScrollToHour();
    if (hour <= 0) {
      hour = Math.max(settings.getWorkingHourStart(), 0);
    }

    int minHour = getStartHour(widgets);
    if (minHour >= 0) {
      hour = Math.min(hour, minHour);
    }

    return hour * settings.getHourHeight();
  }

  public static int getTodayColumn(JustDate date, int days) {
    int diff = TimeUtils.dayDiff(date, TimeUtils.today());
    return BeeUtils.betweenExclusive(diff, 0, days) ? diff : BeeConst.UNDEF;
  }


  public static String renderRange(Range<DateTime> range) {
    return (range == null) ? BeeConst.STRING_EMPTY
        : TimeUtils.renderPeriod(range.lowerEndpoint(), range.upperEndpoint(), true);
  }

  private static boolean intersects(CalendarItem item, long min, long max) {
    return item.getStartMillis() < max && item.getEndMillis() > min;
  }

  private CalendarUtils() {
  }
}
