package com.butent.bee.client.modules.tasks;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.render.AbstractSlackRenderer;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.*;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;

public class TaskSlackRenderer extends AbstractSlackRenderer {

  private final List<? extends IsColumn> dataColumns;

  TaskSlackRenderer(List<? extends IsColumn> columns) {
    super();
    this.dataColumns = columns;
  }

  @Override
  public DateTime getStartDateTime(List<? extends IsColumn> columns, IsRow row) {
    return row.getDateTime(DataUtils.getColumnIndex(TaskConstants.COL_START_TIME, columns));
  }

  @Override
  public DateTime getFinishDateTime(List<? extends IsColumn> columns, IsRow row) {
    return row.getDateTime(DataUtils.getColumnIndex(TaskConstants.COL_FINISH_TIME, columns));
  }

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    if (row == null || sheet == null) {
      return null;
    }
    DateTime now = TimeUtils.nowMinutes();

    DateTime start = getStartDateTime(dataColumns, row);
    DateTime finish = getFinishDateTime(dataColumns, row);

    SlackKind kind = getKind(start, finish, now);
    if (kind == null) {
      return null;
    }

    long minutes = getMinutes(kind, start, finish, now);
    String text = (minutes == 0L) ? BeeConst.STRING_EMPTY : getFormatedTimeLabel(minutes);

    XStyle style = new XStyle();
    XFont font;

    switch (kind) {
      case LATE:
        style.setColor(Colors.RED);

        font = XFont.bold();
        font.setColor(Colors.WHITE);
        style.setFontRef(sheet.registerFont(font));
        break;

      case OPENING:
        style.setColor(Colors.GREEN);
        style.setTextAlign(TextAlign.CENTER);

        font = XFont.bold();
        font.setColor(Colors.WHITE);
        style.setFontRef(sheet.registerFont(font));
        break;

      case ENDGAME:
        style.setColor(Colors.ORANGE);
        style.setTextAlign(TextAlign.CENTER);

        font = XFont.bold();
        font.setColor(Colors.WHITE);
        style.setFontRef(sheet.registerFont(font));
        break;

      case SCHEDULED:
        style.setColor(Colors.YELLOW);
        style.setTextAlign(TextAlign.RIGHT);
        break;
    }

    return new XCell(cellIndex, text, sheet.registerStyle(style));
  }

  @Override
  public String render(IsRow row) {
    Pair<SlackKind, Long> minutes = getMinutes(row);
    if (minutes == null) {
      return null;
    } else if (minutes.bEquals(0L)) {
      return BeeConst.STRING_EMPTY;
    }

    String label = getFormatedTimeLabel(minutes.getB());
    return createSlackBar(minutes.getA(), label);
  }

  public Pair<SlackKind, Long> getMinutes(IsRow row) {
    if (row == null) {
      return null;
    }

    TaskStatus status =
            EnumUtils.getEnumByIndex(TaskStatus.class,
                    Data.getInteger(VIEW_TASKS, row, COL_STATUS));
    if (status == null) {
      return null;
    } else {
      DateTime now = TimeUtils.nowMinutes();

      if (TaskStatus.in(status.ordinal(), TaskStatus.COMPLETED, TaskStatus.CANCELED,
              TaskStatus.APPROVED)) {
        now = getLastStatusTime(row);

        if (now == null) {
          return null;
        }
      }

      DateTime start = getStartDateTime(dataColumns, row);
      DateTime finish = getFinishDateTime(dataColumns, row);

      SlackKind kind = getKind(start, finish, now);
      if (kind == null) {
        return Pair.of(null, 0L);
      }

      Long minutes = getMinutes(kind, start, finish, now);
      return Pair.of(kind, minutes);
    }
  }

  private DateTime getLastStatusTime(IsRow row) {
    return row.getDateTime(DataUtils.getColumnIndex(ALS_LAST_BREAK_EVENT, dataColumns));
  }
}
