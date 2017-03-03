package com.butent.bee.shared.modules.tasks;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.ScheduleDateMode;
import com.butent.bee.shared.time.ScheduleDateRange;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class TaskUtils {

  private static final String NOTE_LABEL_SEPARATOR = ": ";

  public static boolean canConfirmTasks(final DataInfo info, final List<BeeRow> rows,
      long userId, ResponseObject resp) {
    Assert.notNull(info);
    Assert.notNull(rows);

    for (IsRow row : rows) {
      if (BeeUtils.unbox(row.getLong(info.getColumnIndex(COL_OWNER))) != userId) {
        if (resp != null) {
          resp.addWarning(
              BeeUtils.joinWords(Localized.dictionary().crmTask(), row.getId()),
              Localized.dictionary().crmTaskConfirmCanManager());
        }
        return false;
      }

      if (BeeUtils.unbox(row.getInteger(info.getColumnIndex(COL_STATUS)))
        != TaskStatus.COMPLETED.ordinal()) {
        if (resp != null) {
          resp.addWarning(
              BeeUtils.joinWords(Localized.dictionary().crmTask(), row.getId()),
              Localized.dictionary().crmTaskMustBePerformed());
        }
        return false;
      }
    }

    if (resp != null) {
      resp.clearMessages();
      resp.setResponse(null);
    }
    return true;
  }

  public static String getDeleteNote(String label, String value) {
    return BeeUtils.join(NOTE_LABEL_SEPARATOR, label,
        BeeUtils.joinWords(Localized.dictionary().crmDeleted().toLowerCase(), value));
  }

  public static List<FileInfo> getFiles(IsRow row) {
    if (BeeUtils.isEmpty(row.getProperty(PROP_FILES))) {
      return Lists.newArrayList();
    }

    return FileInfo.restoreCollection(row.getProperty(PROP_FILES));
  }

  public static String getInsertNote(String label, String value) {
    return BeeUtils.join(NOTE_LABEL_SEPARATOR, label, BeeUtils
        .joinWords(Localized.dictionary().crmAdded().toLowerCase(), value));
  }

  public static List<ScheduleDateRange> getScheduleDateRanges(BeeRowSet rowSet) {
    List<ScheduleDateRange> result = new ArrayList<>();
    if (DataUtils.isEmpty(rowSet)) {
      return result;
    }

    int fromIndex = rowSet.getColumnIndex(COL_RTD_FROM);
    int untilIndex = rowSet.getColumnIndex(COL_RTD_UNTIL);
    int modeIndex = rowSet.getColumnIndex(COL_RTD_MODE);

    for (BeeRow row : rowSet.getRows()) {
      JustDate from = row.getDate(fromIndex);
      JustDate until = row.getDate(untilIndex);

      ScheduleDateMode mode =
          EnumUtils.getEnumByIndex(ScheduleDateMode.class, row.getInteger(modeIndex));

      ScheduleDateRange sdr = ScheduleDateRange.maybeCreate(from, until, mode);
      if (sdr != null) {
        result.add(sdr);
      }
    }

    return result;
  }

  public static Multimap<Long, ScheduleDateRange> getScheduleDateRangesByTask(BeeRowSet rowSet) {
    Multimap<Long, ScheduleDateRange> result = ArrayListMultimap.create();
    if (DataUtils.isEmpty(rowSet)) {
      return result;
    }

    int rtIndex = rowSet.getColumnIndex(COL_RECURRING_TASK);

    int fromIndex = rowSet.getColumnIndex(COL_RTD_FROM);
    int untilIndex = rowSet.getColumnIndex(COL_RTD_UNTIL);
    int modeIndex = rowSet.getColumnIndex(COL_RTD_MODE);

    for (BeeRow row : rowSet.getRows()) {
      Long rt = row.getLong(rtIndex);

      JustDate from = row.getDate(fromIndex);
      JustDate until = row.getDate(untilIndex);

      ScheduleDateMode mode =
          EnumUtils.getEnumByIndex(ScheduleDateMode.class, row.getInteger(modeIndex));

      ScheduleDateRange sdr = ScheduleDateRange.maybeCreate(from, until, mode);
      if (DataUtils.isId(rt) && sdr != null) {
        result.put(rt, sdr);
      }
    }

    return result;
  }

  public static List<Long> getTaskUsers(IsRow row, List<BeeColumn> columns) {
    List<Long> users = new ArrayList<>();

    Long owner = row.getLong(DataUtils.getColumnIndex(COL_OWNER, columns));
    if (owner != null) {
      users.add(owner);
    }

    Long executor = row.getLong(DataUtils.getColumnIndex(COL_EXECUTOR, columns));
    if (executor != null && !users.contains(executor)) {
      users.add(executor);
    }

    List<Long> observers = DataUtils.parseIdList(row.getProperty(PROP_OBSERVERS));
    for (Long observer : observers) {
      if (!users.contains(observer)) {
        users.add(observer);
      }
    }
    return users;
  }

  public static String getUpdateNote(String label, String oldValue, String newValue) {
    return BeeUtils.join(NOTE_LABEL_SEPARATOR, label, BeeUtils.join(" -> ", oldValue, newValue));
  }

  public static List<String> getUpdateNotes(DataInfo dataInfo, IsRow oldRow, IsRow newRow,
      Function<HasDateValue, String> dateRenderer,
      Function<HasDateValue, String> dateTimeRenderer) {

    List<String> notes = new ArrayList<>();

    if (dataInfo == null || oldRow == null || newRow == null) {
      return notes;
    }
    List<BeeColumn> columns = dataInfo.getColumns();
    for (int i = 0; i < columns.size(); i++) {
      BeeColumn column = columns.get(i);

      String oldValue = oldRow.getString(i);
      String newValue = newRow.getString(i);

      if (!BeeUtils.equalsTrimRight(oldValue, newValue) && column.isEditable()) {
        String label = Localized.getLabel(column);
        String note;

        if (BeeUtils.isEmpty(oldValue)) {
          if (Objects.equals(oldRow.getBoolean(i), Boolean.TRUE)) {
            note = getInsertNote(label, "");
          } else {
            note = getInsertNote(label,
                renderColumn(dataInfo, oldRow, column, i, dateRenderer, dateTimeRenderer));
          }

        } else if (BeeUtils.isEmpty(newValue)) {
          if (Objects.equals(oldRow.getBoolean(i), Boolean.TRUE)) {
            note = getDeleteNote(label, "");
          } else {
            note = getDeleteNote(label,
                renderColumn(dataInfo, oldRow, column, i, dateRenderer, dateTimeRenderer));
          }

        } else {
          note = getUpdateNote(label,
              renderColumn(dataInfo, oldRow, column, i, dateRenderer, dateTimeRenderer),
              renderColumn(dataInfo, newRow, column, i, dateRenderer, dateTimeRenderer));
        }
        notes.add(note);
      }
    }
    return notes;
  }

  @Deprecated
  public static boolean isScheduled(DateTime start) {
    return start != null && TimeUtils.dayDiff(TimeUtils.today(), start) > 0;
  }

  public static String renderColumn(DataInfo dataInfo, IsRow row, BeeColumn column, int index,
      Function<HasDateValue, String> dateRenderer,
      Function<HasDateValue, String> dateTimeRenderer) {

    if (COL_TASK_TYPE.equals(column.getId())) {
      int nameIndex = dataInfo.getColumnIndex(ALS_TASK_TYPE_NAME);

      if (!BeeConst.isUndef(nameIndex)) {
        return row.getString(nameIndex);
      }
    }

    return DataUtils.render(dataInfo, row, column, index, dateRenderer, dateTimeRenderer);
  }

  public static boolean sameObservers(IsRow oldRow, IsRow newRow) {
    if (oldRow == null || newRow == null) {
      return false;
    } else {
      return DataUtils.sameIdSet(oldRow.getProperty(PROP_OBSERVERS),
          newRow.getProperty(PROP_OBSERVERS));
    }
  }

  private TaskUtils() {
  }
}
