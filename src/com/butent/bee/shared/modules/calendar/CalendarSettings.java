package com.butent.bee.shared.modules.calendar;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.calendar.CalendarConstants.MultidayLayout;
import com.butent.bee.shared.modules.calendar.CalendarConstants.TimeBlockClick;
import com.butent.bee.shared.modules.calendar.CalendarConstants.ViewType;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.EnumMap;
import java.util.List;

public final class CalendarSettings {

  public static CalendarSettings create(IsRow row, List<? extends IsColumn> columns) {
    CalendarSettings settings = new CalendarSettings();
    if (row != null && !BeeUtils.isEmpty(columns)) {
      settings.loadFrom(row, columns);
    }
    return settings;
  }

  private static boolean getBool(IsRow row, List<? extends IsColumn> columns, String columnId) {
    Boolean value = row.getBoolean(DataUtils.getColumnIndex(columnId, columns));
    return (value == null) ? false : value;
  }

  private static int getInt(IsRow row, List<? extends IsColumn> columns, String columnId) {
    Integer value = row.getInteger(DataUtils.getColumnIndex(columnId, columns));
    return (value == null) ? BeeConst.UNDEF : value;
  }

  private long id;

  private int pixelsPerInterval;
  private int intervalsPerHour;
  private int workingHourStart;

  private int workingHourEnd;

  private int scrollToHour;

  private int defaultDisplayedDays;

  private TimeBlockClick timeBlockClickNumber;
  private boolean separateAttendees;

  private MultidayLayout multidayLayout;

  private MultidayLayout multidayTaskLayout;

  private final EnumMap<ViewType, Boolean> views;

  private ViewType activeView;

  private CalendarSettings() {
    this.views = new EnumMap<>(ViewType.class);

    for (ViewType view : ViewType.values()) {
      this.views.put(view, true);
    }
  }

  public ViewType getActiveView() {
    return activeView;
  }

  public int getDefaultDisplayedDays() {
    return defaultDisplayedDays;
  }

  public int getHourHeight() {
    return getIntervalsPerHour() * getPixelsPerInterval();
  }

  public long getId() {
    return id;
  }

  public int getIntervalsPerHour() {
    return intervalsPerHour;
  }

  public MultidayLayout getMultidayLayout() {
    return multidayLayout;
  }

  public MultidayLayout getMultidayTaskLayout() {
    return multidayTaskLayout;
  }

  public int getPixelsPerInterval() {
    return pixelsPerInterval;
  }

  public int getScrollToHour() {
    return scrollToHour;
  }

  public TimeBlockClick getTimeBlockClickNumber() {
    return timeBlockClickNumber;
  }

  public int getWorkingHourEnd() {
    return workingHourEnd;
  }

  public int getWorkingHourStart() {
    return workingHourStart;
  }

  public boolean isAnyVisible() {
    for (ViewType view : ViewType.values()) {
      if (isVisible(view)) {
        return true;
      }
    }
    return false;
  }

  public boolean isDoubleClick() {
    return TimeBlockClick.DOUBLE.equals(getTimeBlockClickNumber());
  }

  public boolean isSingleClick() {
    return TimeBlockClick.SINGLE.equals(getTimeBlockClickNumber());
  }

  public boolean isVisible(ViewType view) {
    return BeeUtils.isTrue(views.get(view));
  }

  public void loadFrom(IsRow row, List<? extends IsColumn> columns) {
    setId(row.getId());

    setPixelsPerInterval(getInt(row, columns, COL_PIXELS_PER_INTERVAL));
    setIntervalsPerHour(getInt(row, columns, COL_INTERVALS_PER_HOUR));

    setWorkingHourStart(getInt(row, columns, COL_WORKING_HOUR_START));
    setWorkingHourEnd(getInt(row, columns, COL_WORKING_HOUR_END));
    setScrollToHour(getInt(row, columns, COL_SCROLL_TO_HOUR));

    setDefaultDisplayedDays(getInt(row, columns, COL_DEFAULT_DISPLAYED_DAYS));

    Integer tbcn = row.getInteger(DataUtils.getColumnIndex(COL_TIME_BLOCK_CLICK_NUMBER, columns));
    setTimeBlockClickNumber(EnumUtils.getEnumByIndex(TimeBlockClick.class, tbcn));

    setSeparateAttendees(getBool(row, columns, COL_SEPARATE_ATTENDEES));

    Integer mdl = row.getInteger(DataUtils.getColumnIndex(COL_MULTIDAY_LAYOUT, columns));
    setMultidayLayout(EnumUtils.getEnumByIndex(MultidayLayout.class, mdl));

    mdl = row.getInteger(DataUtils.getColumnIndex(COL_MULTIDAY_TASK_LAYOUT, columns));
    setMultidayTaskLayout(EnumUtils.getEnumByIndex(MultidayLayout.class, mdl));

    for (ViewType view : ViewType.values()) {
      views.put(view, getBool(row, columns, view.getColumnId()));
    }

    Integer av;
    if (DataUtils.contains(columns, COL_ACTIVE_VIEW)) {
      av = row.getInteger(DataUtils.getColumnIndex(COL_ACTIVE_VIEW, columns));
    } else {
      av = null;
    }
    setActiveView(EnumUtils.getEnumByIndex(ViewType.class, av));
  }

  public boolean separateAttendees() {
    return separateAttendees;
  }

  public void setActiveView(ViewType activeView) {
    this.activeView = activeView;
  }

  public void setDefaultDisplayedDays(int defaultDisplayedDays) {
    this.defaultDisplayedDays = BeeUtils.clamp(defaultDisplayedDays, 2, 100);
  }

  private void setId(long id) {
    this.id = id;
  }

  private void setIntervalsPerHour(int intervals) {
    int value = BeeUtils.clamp(intervals, 1, TimeUtils.MINUTES_PER_HOUR);
    while (TimeUtils.MINUTES_PER_HOUR % value != 0) {
      value--;
    }

    this.intervalsPerHour = value;
  }

  private void setMultidayLayout(MultidayLayout multidayLayout) {
    this.multidayLayout = multidayLayout;
  }

  private void setMultidayTaskLayout(MultidayLayout multidayTaskLayout) {
    this.multidayTaskLayout = multidayTaskLayout;
  }

  private void setPixelsPerInterval(int px) {
    this.pixelsPerInterval = BeeUtils.clamp(px, 1, 100);
  }

  private void setScrollToHour(int hour) {
    this.scrollToHour = hour;
  }

  private void setSeparateAttendees(boolean separateAttendees) {
    this.separateAttendees = separateAttendees;
  }

  private void setTimeBlockClickNumber(TimeBlockClick timeBlockClickNumber) {
    this.timeBlockClickNumber = timeBlockClickNumber;
  }

  private void setWorkingHourEnd(int end) {
    this.workingHourEnd = end;
  }

  private void setWorkingHourStart(int start) {
    this.workingHourStart = start;
  }
}
