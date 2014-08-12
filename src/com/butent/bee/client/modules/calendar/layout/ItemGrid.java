package com.butent.bee.client.modules.calendar.layout;

import com.butent.bee.client.layout.Absolute;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.modules.calendar.CalendarUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.time.TimeUtils;

public class ItemGrid extends Absolute {

  private int nowIndex = BeeConst.UNDEF;

  public ItemGrid() {
    super();
  }

  public void build(int columnCount, CalendarSettings settings,
      int todayStartColumn, int todayEndColumn) {
    clear();

    int intervalsPerHour = settings.getIntervalsPerHour();
    int intervalSize = settings.getPixelsPerInterval();

    StyleUtils.setHeight(this, intervalsPerHour * intervalSize * TimeUtils.HOURS_PER_DAY);

    int todayWidth = CalendarLayoutManager.getTodayWidth(columnCount, todayStartColumn,
        todayEndColumn);
    int todayLeft = (todayWidth > 0)
        ? CalendarLayoutManager.getTodayLeft(columnCount, todayStartColumn) : BeeConst.UNDEF;

    if (todayWidth > 0) {
      CustomDiv today = new CustomDiv();
      today.addStyleName(CalendarStyleManager.TODAY_MARKER);
      today.addStyleName(CalendarStyleManager.TODAY);

      StyleUtils.setLeft(today, todayLeft, CssUnit.PCT);
      StyleUtils.setWidth(today, todayWidth, CssUnit.PCT);

      add(today);
    }

    int top = 0;
    for (int i = 0; i < TimeUtils.HOURS_PER_DAY; i++) {
      boolean isWork = i >= settings.getWorkingHourStart() && i < settings.getWorkingHourEnd();

      CustomDiv major = new CustomDiv();
      major.addStyleName(CalendarStyleManager.MAJOR_TIME_INTERVAL);
      major.addStyleName(isWork
          ? CalendarStyleManager.WORKING_HOURS : CalendarStyleManager.NON_WORKING);

      StyleUtils.setTop(major, top);
      StyleUtils.setHeight(major, intervalSize - 1);
      add(major);

      top += intervalSize;

      for (int x = 0; x < intervalsPerHour - 1; x++) {
        CustomDiv minor = new CustomDiv();
        minor.addStyleName(CalendarStyleManager.MINOR_TIME_INTERVAL);
        minor.addStyleName(isWork
            ? CalendarStyleManager.WORKING_HOURS : CalendarStyleManager.NON_WORKING);

        StyleUtils.setTop(minor, top);
        StyleUtils.setHeight(minor, intervalSize - 1);
        add(minor);

        top += intervalSize;
      }
    }

    CalendarLayoutManager.addColumnSeparators(this, columnCount);

    if (todayWidth > 0) {
      CustomDiv now = new CustomDiv();
      now.addStyleName(CalendarStyleManager.NOW_MARKER);

      StyleUtils.setLeft(now, todayLeft, CssUnit.PCT);
      StyleUtils.setWidth(now, todayWidth, CssUnit.PCT);

      add(now);
      setNowIndex(getWidgetCount() - 1);

      onClock(settings);
    } else {
      setNowIndex(BeeConst.UNDEF);
    }
  }

  public void onClock(CalendarSettings settings) {
    if (getNowIndex() >= 0) {
      int y = CalendarUtils.getNowY(settings);
      StyleUtils.setTop(getWidget(getNowIndex()), y);
    }
  }

  private int getNowIndex() {
    return nowIndex;
  }

  private void setNowIndex(int nowIndex) {
    this.nowIndex = nowIndex;
  }
}
