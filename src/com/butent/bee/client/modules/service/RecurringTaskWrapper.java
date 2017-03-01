package com.butent.bee.client.modules.service;

import com.google.common.collect.Range;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskPriority;
import com.butent.bee.shared.time.CronExpression;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.ScheduleDateRange;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.WorkdayTransition;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class RecurringTaskWrapper implements HasDateRange {

  static final String ID_COLUMN = Data.getIdColumn(VIEW_RECURRING_TASKS);

  private static final String fromLabel = Data.getColumnLabel(VIEW_RECURRING_TASKS,
      COL_RT_SCHEDULE_FROM);
  private static final String untilLabel = Data.getColumnLabel(VIEW_RECURRING_TASKS,
      COL_RT_SCHEDULE_UNTIL);

  private static final String startLabel = Data.getColumnLabel(VIEW_TASKS, COL_START_TIME);
  private static final String endLabel = Data.getColumnLabel(VIEW_TASKS, COL_FINISH_TIME);

  private static final String typeLabel = Data.getColumnLabel(VIEW_RECURRING_TASKS, COL_TASK_TYPE);
  private static final String priorityLabel = Data.getColumnLabel(VIEW_RECURRING_TASKS,
      COL_PRIORITY);

  private static final String summaryLabel = Data.getColumnLabel(VIEW_RECURRING_TASKS, COL_SUMMARY);
  private static final String ownerLabel = Data.getColumnLabel(VIEW_RECURRING_TASKS, COL_OWNER);

  static List<RecurringTaskWrapper> spawn(SimpleRow row, Collection<ScheduleDateRange> sdrs,
      JustDate minDate, JustDate maxDate) {

    List<RecurringTaskWrapper> result = new ArrayList<>();

    JustDate from = row.getDate(COL_RT_SCHEDULE_FROM);
    if (from == null) {
      return result;
    }

    JustDate until = BeeUtils.min(row.getDate(COL_RT_SCHEDULE_UNTIL), maxDate);
    if (until == null) {
      until = TimeUtils.endOfYear(TimeUtils.year(), 1);
    }

    JustDate min = BeeUtils.max(BeeUtils.max(from, minDate), TimeUtils.today());

    DateTime lastSpawn = row.getDateTime(ALS_LAST_SPAWN);
    if (lastSpawn != null) {
      min = BeeUtils.max(min, TimeUtils.nextDay(lastSpawn));
    }

    if (BeeUtils.isLess(until, min)) {
      return result;
    }

    CronExpression.Builder builder = new CronExpression.Builder(from, until)
        .id(row.getValue(ID_COLUMN))
        .dayOfMonth(row.getValue(COL_RT_DAY_OF_MONTH))
        .month(row.getValue(COL_RT_MONTH))
        .dayOfWeek(row.getValue(COL_RT_DAY_OF_WEEK))
        .year(row.getValue(COL_RT_YEAR))
        .workdayTransition(EnumUtils.getEnumByIndex(WorkdayTransition.class,
            row.getInt(COL_RT_WORKDAY_TRANSITION)));

    if (!BeeUtils.isEmpty(sdrs)) {
      for (ScheduleDateRange sdr : sdrs) {
        builder.rangeMode(sdr.getRange(), sdr.getMode());
      }
    }

    CronExpression cron = builder.build();

    List<JustDate> dates = cron.getDates(min, until);
    if (dates.isEmpty()) {
      return result;
    }

    Long startAt = TimeUtils.parseTime(row.getValue(COL_RT_START_AT));

    Integer durationDays = row.getInt(COL_RT_DURATION_DAYS);
    Long durationMillis = TimeUtils.parseTime(row.getValue(COL_RT_DURATION_TIME));

    if (!BeeUtils.isPositive(durationDays) && !BeeUtils.isPositive(durationMillis)) {
      durationDays = 1;
    }

    JustDate last = null;

    for (JustDate date : dates) {
      DateTime start = TimeUtils.combine(date, startAt);
      DateTime end;

      if (BeeUtils.isPositive(durationDays) && !BeeUtils.isPositive(durationMillis)
          && last != null && TimeUtils.dayDiff(last, date) == durationDays) {

        if (durationDays > 1) {
          end = TimeUtils.startOfDay(date, durationDays);
        } else {
          end = null;
        }

      } else if (BeeUtils.isPositive(durationDays) || BeeUtils.isPositive(durationMillis)) {
        long endMillis = start.getTime();
        if (BeeUtils.isPositive(durationDays)) {
          endMillis += durationDays * TimeUtils.MILLIS_PER_DAY;
        }
        if (BeeUtils.isPositive(durationMillis)) {
          endMillis += durationMillis;
        }

        end = new DateTime(endMillis);

      } else {
        end = null;
      }

      result.add(new RecurringTaskWrapper(row, start, end));
      last = date;
    }

    return result;
  }

  private final Long id;

  private final TaskPriority priority;

  private final String typeColor;

  private final Range<JustDate> range;

  private final String title;

  private RecurringTaskWrapper(SimpleRow row, DateTime start, DateTime end) {
    this.id = row.getLong(ID_COLUMN);

    this.priority = EnumUtils.getEnumByIndex(TaskPriority.class, row.getInt(COL_PRIORITY));

    this.typeColor = row.getValue(ALS_TASK_TYPE_BACKGROUND);

    this.range = TimeBoardHelper.getRange(start, end);

    this.title = TimeBoardHelper.buildTitle(
        fromLabel, Format.renderDateTime(row.getDate(COL_RT_SCHEDULE_FROM)),
        untilLabel, Format.renderDateTime(row.getDate(COL_RT_SCHEDULE_UNTIL)),
        startLabel, Format.renderDateTime(start),
        endLabel, Format.renderDateTime(end),
        typeLabel, row.getValue(ALS_TASK_TYPE_NAME),
        priorityLabel, (priority == null) ? null : priority.getCaption(),
        summaryLabel, row.getValue(COL_SUMMARY),
        ownerLabel, Global.getUsers().getSignature(row.getLong(COL_OWNER)));
  }

  @Override
  public Range<JustDate> getRange() {
    return range;
  }

  Long getId() {
    return id;
  }

  TaskPriority getPriority() {
    return priority;
  }

  String getTypeColor() {
    return typeColor;
  }

  String getTitle() {
    return title;
  }
}
