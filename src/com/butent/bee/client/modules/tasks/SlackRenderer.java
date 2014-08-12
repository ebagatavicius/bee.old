package com.butent.bee.client.modules.tasks;

import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;

class SlackRenderer extends AbstractCellRenderer {

  private enum Kind {
    LATE, OPENING, ENDGAME, SCHEDULED;

    private String getStyleName() {
      return STYLE_PREFIX + name().toLowerCase();
    }
  }

  private static final String STYLE_PREFIX = TaskConstants.CRM_STYLE_PREFIX + "Slack-";

  private static final String STYLE_BAR = STYLE_PREFIX + "bar";
  private static final String STYLE_LABEl = STYLE_PREFIX + "label";

  private static String format(Kind kind, String label) {
    Div bar = new Div().addClass(STYLE_BAR).addClass(kind.getStyleName());
    if (!BeeUtils.isEmpty(label)) {
      bar.appendChild(new Div().addClass(STYLE_LABEl).text(label));
    }

    return bar.toString();
  }

  private static Kind getKind(TaskStatus status, DateTime start, DateTime finish) {
    if (status == null || status == TaskStatus.COMPLETED || status == TaskStatus.CANCELED
        || status == TaskStatus.APPROVED) {
      return null;
    }

    DateTime now = TimeUtils.nowMinutes();

    if (finish != null && TimeUtils.isLess(finish, now)) {
      return Kind.LATE;

    } else if (TaskUtils.isScheduled(start)) {
      return Kind.SCHEDULED;

    } else if (start != null && TimeUtils.isMore(finish, start) && TimeUtils.isMeq(now, start)
        && TimeUtils.isLess(now, finish)) {
      if (now.getTime() - start.getTime() < (finish.getTime() - start.getTime()) / 2) {
        return Kind.OPENING;
      } else {
        return Kind.ENDGAME;
      }

    } else {
      return null;
    }
  }

  private static String getLabel(long minutes) {
    if (minutes <= 0L) {
      return BeeConst.STRING_ZERO;

    } else if (minutes < TimeUtils.MINUTES_PER_DAY) {
      return TimeUtils.renderMinutes(BeeUtils.toInt(minutes), false);

    } else {
      return BeeUtils.toString(minutes / TimeUtils.MINUTES_PER_DAY);
    }
  }

  private static long getMinutes(Kind kind, DateTime start, DateTime finish) {
    DateTime now = TimeUtils.nowMinutes();

    switch (kind) {
      case LATE:
        return (now.getTime() - finish.getTime()) / TimeUtils.MILLIS_PER_MINUTE;

      case OPENING:
      case ENDGAME:
        return (finish.getTime() - now.getTime()) / TimeUtils.MILLIS_PER_MINUTE;

      case SCHEDULED:
        return TimeUtils.dayDiff(now, start) * TimeUtils.MINUTES_PER_DAY;

      default:
        Assert.untouchable();
        return 0L;
    }
  }

  private final int statusIndex;

  private final int startIndex;
  private final int finishIndex;

  SlackRenderer(List<? extends IsColumn> columns) {
    super(null);

    this.statusIndex = DataUtils.getColumnIndex(TaskConstants.COL_STATUS, columns);
    this.startIndex = DataUtils.getColumnIndex(TaskConstants.COL_START_TIME, columns);
    this.finishIndex = DataUtils.getColumnIndex(TaskConstants.COL_FINISH_TIME, columns);
  }

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    if (row == null || sheet == null) {
      return null;
    }

    TaskStatus status = EnumUtils.getEnumByIndex(TaskStatus.class, row.getInteger(statusIndex));

    DateTime start = row.getDateTime(startIndex);
    DateTime finish = row.getDateTime(finishIndex);

    Kind kind = getKind(status, start, finish);
    if (kind == null) {
      return null;
    }

    long minutes = getMinutes(kind, start, finish);
    String text = (minutes == 0L) ? BeeConst.STRING_EMPTY : getLabel(minutes);

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
    if (row == null) {
      return null;
    }

    TaskStatus status = EnumUtils.getEnumByIndex(TaskStatus.class, row.getInteger(statusIndex));

    DateTime start = row.getDateTime(startIndex);
    DateTime finish = row.getDateTime(finishIndex);

    Kind kind = getKind(status, start, finish);
    if (kind == null) {
      return BeeConst.STRING_EMPTY;
    }

    long minutes = getMinutes(kind, start, finish);
    if (minutes == 0L) {
      return BeeConst.STRING_EMPTY;
    }

    String label = getLabel(minutes);
    return format(kind, label);
  }
}
