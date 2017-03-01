package com.butent.bee.client.modules.service;

import com.google.common.collect.Range;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskPriority;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.EnumUtils;

class TaskWrapper implements HasDateRange {

  private static final String idColumn = Data.getIdColumn(VIEW_TASKS);

  private static final String startLabel = Data.getColumnLabel(VIEW_TASKS, COL_START_TIME);
  private static final String endLabel = Data.getColumnLabel(VIEW_TASKS, COL_FINISH_TIME);

  private static final String typeLabel = Data.getColumnLabel(VIEW_TASKS, COL_TASK_TYPE);
  private static final String priorityLabel = Data.getColumnLabel(VIEW_TASKS, COL_PRIORITY);
  private static final String statusLabel = Data.getColumnLabel(VIEW_TASKS, COL_STATUS);

  private static final String summaryLabel = Data.getColumnLabel(VIEW_TASKS, COL_SUMMARY);
  private static final String ownerLabel = Data.getColumnLabel(VIEW_TASKS, COL_OWNER);
  private static final String executorLabel = Data.getColumnLabel(VIEW_TASKS, COL_EXECUTOR);

  private final Long id;

  private final TaskPriority priority;
  private final TaskStatus status;

  private final String typeColor;
  private final Integer star;

  private final Range<JustDate> range;

  private final String title;

  TaskWrapper(SimpleRow row) {
    this.id = row.getLong(idColumn);

    this.priority = EnumUtils.getEnumByIndex(TaskPriority.class, row.getInt(COL_PRIORITY));
    this.status = EnumUtils.getEnumByIndex(TaskStatus.class, row.getInt(COL_STATUS));

    this.typeColor = row.getValue(ALS_TASK_TYPE_BACKGROUND);
    this.star = row.getInt(COL_STAR);

    DateTime start = row.getDateTime(COL_START_TIME);
    DateTime end = row.getDateTime(COL_FINISH_TIME);

    this.range = TimeBoardHelper.getRange(start, end);

    this.title = TimeBoardHelper.buildTitle(
        startLabel, Format.renderDateTime(start),
        endLabel, Format.renderDateTime(end),
        typeLabel, row.getValue(ALS_TASK_TYPE_NAME),
        priorityLabel, (priority == null) ? null : priority.getCaption(),
        statusLabel, (status == null) ? null : status.getCaption(),
        summaryLabel, row.getValue(COL_SUMMARY),
        ownerLabel, Global.getUsers().getSignature(row.getLong(COL_OWNER)),
        executorLabel, Global.getUsers().getSignature(row.getLong(COL_EXECUTOR)));
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

  Integer getStar() {
    return star;
  }

  TaskStatus getStatus() {
    return status;
  }

  String getTypeColor() {
    return typeColor;
  }

  String getTitle() {
    return title;
  }
}
