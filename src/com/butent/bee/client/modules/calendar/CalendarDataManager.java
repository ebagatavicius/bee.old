package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Range;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.modules.calendar.CalendarTask;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CalendarDataManager {

  private static BeeLogger logger = LogUtils.getLogger(CalendarDataManager.class);

  private static DateTime max(DateTime x, DateTime y) {
    return (x.getTime() >= y.getTime()) ? x : y;
  }

  private static DateTime min(DateTime x, DateTime y) {
    return (x.getTime() <= y.getTime()) ? x : y;
  }

  private static DateTime hour(DateTime ref, int hour) {
    return new DateTime(ref.getYear(), ref.getMonth(), ref.getDom(), hour, 0, 0);
  }

  private static List<CalendarItem> split(CalendarItem item, MultidayLayout mdl,
      int whStart, int whEnd) {

    List<CalendarItem> result = new ArrayList<>();

    DateTime start = item.getStartTime();
    DateTime end = item.getEndTime();

    DateTime tmp = DateTime.copyOf(start);

    DateTime from;
    DateTime until;

    switch (mdl) {
      case VERTICAL:
        while (tmp.getTime() < end.getTime()) {
          until = min(end, TimeUtils.startOfDay(tmp, 1));
          result.add(item.split(tmp, until));

          tmp.setTime(until.getTime());
        }
        break;

      case WORKING_HOURS:
        while (tmp.getTime() < end.getTime()) {
          from = (TimeUtils.sameDate(tmp, start) && TimeUtils.minutesSinceDayStarted(start) > 0)
              ? start : hour(tmp, whStart);
          until = TimeUtils.sameDate(tmp, end) ? end : hour(tmp, whEnd);

          if (until.getTime() <= from.getTime()) {
            if (TimeUtils.sameDate(from, start)) {
              until = TimeUtils.startOfDay(from, 1);
            } else {
              from = TimeUtils.startOfDay(from);
            }
          }

          result.add(item.split(from, until));

          tmp = TimeUtils.startOfDay(tmp, 1);
        }
        break;

      case LAST_DAY:
        if (TimeUtils.minutesSinceDayStarted(end) > 0) {
          from = max(start, TimeUtils.startOfDay(end));
          until = end;

          if (validateWorkingHours(whStart, whEnd)) {
            tmp = hour(end, whStart);
            if (tmp.getTime() > from.getTime() && tmp.getTime() < end.getTime()) {
              from = tmp;
            }
          }

        } else if (validateWorkingHours(whStart, whEnd)) {
          from = max(start, hour(TimeUtils.startOfDay(end, -1), whStart));
          until = hour(TimeUtils.startOfDay(end, -1), whEnd);

          if (until.getTime() <= from.getTime()) {
            until = end;
          }

        } else {
          from = max(start, TimeUtils.startOfDay(end, -1));
          until = end;
        }

        result.add(item.split(from, until));
        break;

      default:
        result.add(item);
    }

    return result;
  }

  private static boolean validateWorkingHours(int whStart, int whEnd) {
    return whStart >= 0 && whEnd > whStart && whEnd <= TimeUtils.HOURS_PER_DAY;
  }

  private final List<CalendarItem> items = new ArrayList<>();

  private Range<DateTime> range;

  public CalendarDataManager() {
    super();
  }

  public void addItem(CalendarItem item, CalendarSettings settings) {
    if (item == null || !item.isValid()) {
      logger.warning(NameUtils.getName(this), "attempt to add invalid item");
      return;
    }

    if (item.isMultiDay() && settings != null) {
      MultidayLayout mdl = null;

      switch (item.getItemType()) {
        case APPOINTMENT:
          mdl = settings.getMultidayLayout();
          break;
        case TASK:
          mdl = settings.getMultidayTaskLayout();
          break;
      }

      if (mdl == null || mdl == MultidayLayout.HORIZONTAL) {
        items.add(item);

      } else {
        int whStart = settings.getWorkingHourStart();
        int whEnd = settings.getWorkingHourEnd();

        if (mdl == MultidayLayout.WORKING_HOURS && !validateWorkingHours(whStart, whEnd)) {
          mdl = MultidayLayout.VERTICAL;
        }

        items.addAll(split(item, mdl, whStart, whEnd));
      }

    } else {
      items.add(item);
    }
  }

  public List<CalendarItem> getItems() {
    return items;
  }

  public void loadItems(long calendarId, final Range<DateTime> visibleRange,
      final CalendarSettings settings, boolean force, final IntCallback callback) {

    if (!force && getRange() != null && visibleRange != null
        && getRange().encloses(visibleRange)) {
      if (callback != null) {
        callback.onSuccess(getSize());
      }
      return;
    }

    ParameterList params = CalendarKeeper.createArgs(SVC_GET_CALENDAR_ITEMS);
    params.addQueryItem(PARAM_CALENDAR_ID, calendarId);

    if (visibleRange != null) {
      params.addQueryItem(PARAM_START_TIME, visibleRange.lowerEndpoint().getTime());
      params.addQueryItem(PARAM_END_TIME, visibleRange.upperEndpoint().getTime());
    }

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        items.clear();

        if (!response.isEmpty()) {
          Map<String, String> data = Codec.deserializeLinkedHashMap(response.getResponseAsString());

          for (Map.Entry<String, String> entry : data.entrySet()) {
            ItemType type = EnumUtils.getEnumByName(ItemType.class, entry.getKey());
            if (type == null) {
              logger.severe("item type not recognized", entry.getKey());
              continue;
            }

            String[] arr = Codec.beeDeserializeCollection(entry.getValue());
            if (ArrayUtils.isEmpty(arr)) {
              logger.warning("item type", entry.getKey(), "has no values");
              continue;
            }

            for (String item : arr) {
              switch (type) {
                case APPOINTMENT:
                  addItem(Appointment.create(BeeRow.restore(item)), settings);
                  break;
                case TASK:
                  addItem(CalendarTask.restore(item), settings);
                  break;
              }
            }
          }
        }

        setRange(visibleRange);
        if (callback != null) {
          callback.onSuccess(getSize());
        }
      }
    });
  }

  public boolean removeItem(ItemType type, long id) {
    int index = getItemIndex(type, id);
    if (BeeConst.isUndef(index)) {
      return false;
    }

    do {
      items.remove(index);
      index = getItemIndex(type, id);
    } while (!BeeConst.isUndef(index));

    return true;
  }

  public void sort() {
    if (items.size() > 1) {
      Collections.sort(items);
    }
  }

  private int getItemIndex(ItemType type, long id) {
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getItemType() == type && items.get(i).getId() == id) {
        return i;
      }
    }
    return BeeConst.UNDEF;
  }

  private Range<DateTime> getRange() {
    return range;
  }

  private int getSize() {
    return items.size();
  }

  private void setRange(Range<DateTime> range) {
    this.range = range;
  }
}