package com.butent.bee.shared.modules.calendar;

import com.google.common.collect.Range;
import com.google.common.primitives.Longs;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.modules.calendar.CalendarConstants.ItemType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.TimeUtils;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class CalendarItem implements Comparable<CalendarItem> {

  private DateTime partStart;
  private DateTime partEnd;

  @Override
  public int compareTo(CalendarItem other) {
    int result = Longs.compare(getStartMillis(), other.getStartMillis());
    if (result == BeeConst.COMPARE_EQUAL) {
      result = Longs.compare(other.getEndMillis(), getEndMillis());
    }
    return result;
  }

  public abstract CalendarItem copy();

  public abstract String getBackground();

  public abstract String getCompactTemplate();

  public abstract String getCompanyName();

  public abstract String getDescription();

  public long getDuration() {
    return getEndMillis() - getStartMillis();
  }

  public long getEndMillis() {
    return getEndTime().getTime();
  }

  public DateTime getEndTime() {
    return (getPartEnd() == null) ? getEnd() : getPartEnd();
  }

  public abstract String getForeground();

  public abstract long getId();

  public abstract ItemType getItemType();

  public abstract String getMultiBodyTemplate();

  public abstract String getMultiHeaderTemplate();

  public abstract String getPartialBodyTemplate();

  public abstract String getPartialHeaderTemplate();

  public Range<DateTime> getRange() {
    return Range.closedOpen(getStartTime(), getEndTime());
  }

  public abstract Long getSeparatedAttendee();

  public abstract String getSimpleBodyTemplate();

  public abstract String getSimpleHeaderTemplate();

  public long getStartMillis() {
    return getStartTime().getTime();
  }

  public DateTime getStartTime() {
    return (getPartStart() == null) ? getStart() : getPartStart();
  }

  public abstract Enum<?> getStatus();

  public abstract String getStringTemplate();

  public abstract Long getStyle();

  public abstract Map<String, String> getSubstitutes(long calendarId, Map<Long, UserData> users,
      boolean addLabels, Function<HasDateValue, String> dateTimeRenderer,
      BiFunction<HasDateValue, HasDateValue, String> periodRenderer);

  public abstract String getSummary();

  public abstract String getTitleTemplate();

  public abstract boolean isEditable(Long userId);

  public abstract boolean isMovable(Long userId);

  public boolean isMultiDay() {
    return TimeUtils.isMore(getEndTime(), TimeUtils.startOfDay(getStartTime(), 1));
  }

  public boolean isPartial() {
    return getPartStart() != null;
  }

  public abstract boolean isRemovable(Long userId);

  public abstract boolean isResizable(Long userId);

  public boolean isValid() {
    return getStartTime() != null && getEndTime() != null && getStartMillis() < getEndMillis();
  }

  public abstract boolean isVisible(Long userId);

  public boolean isWhole() {
    return getPartStart() == null;
  }

  public CalendarItem split(DateTime from, DateTime until) {
    CalendarItem result = copy();

    result.setPartStart(DateTime.copyOf(from));
    result.setPartEnd(DateTime.copyOf(until));

    return result;
  }

  protected abstract DateTime getEnd();

  protected abstract DateTime getStart();

  private DateTime getPartEnd() {
    return partEnd;
  }

  private DateTime getPartStart() {
    return partStart;
  }

  private void setPartEnd(DateTime partEnd) {
    this.partEnd = partEnd;
  }

  private void setPartStart(DateTime partStart) {
    this.partStart = partStart;
  }
}
