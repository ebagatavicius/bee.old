package com.butent.bee.shared.i18n;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.time.HasDateValue;

import java.util.function.Function;

public final class Formatter {

  private Formatter() {
  }

  public static Function<HasDateValue, String> getDateRenderer(DateTimeFormatInfo dtfInfo) {
    return date -> renderDate(dtfInfo, date);
  }

  public static Function<HasDateValue, String> getDateTimeRenderer(DateTimeFormatInfo dtfInfo) {
    return dateTime -> renderDateTime(dtfInfo, dateTime);
  }

  public static String renderDate(DateTimeFormatInfo dtfInfo, HasDateValue date) {
    return render(PredefinedFormat.DATE_SHORT, dtfInfo, date);
  }

  public static String renderDateTime(DateTimeFormatInfo dtfInfo, HasDateValue dateTime) {
    if (dtfInfo == null || dateTime == null) {
      return BeeConst.STRING_EMPTY;

    } else if (dateTime.hasTimePart()) {
      PredefinedFormat timeFormat = getTimeFormat(dateTime);

      String pattern = dtfInfo.dateTime(PredefinedFormat.DATE_SHORT.getPattern(dtfInfo),
          timeFormat.getPattern(dtfInfo));

      return DateTimeFormat.of(pattern, dtfInfo).format(dateTime);

    } else {
      return render(PredefinedFormat.DATE_SHORT, dtfInfo, dateTime);
    }
  }

  public static String renderTime(DateTimeFormatInfo dtfInfo, HasDateValue dateTime) {
    if (dtfInfo == null || dateTime == null) {
      return BeeConst.STRING_EMPTY;

    } else if (dateTime.hasTimePart()) {
      return render(getTimeFormat(dateTime), dtfInfo, dateTime);

    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  public static String render(PredefinedFormat predefinedFormat, DateTimeFormatInfo dtfInfo,
      HasDateValue value) {

    if (predefinedFormat == null || value == null || dtfInfo == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return getPredefinedFormat(predefinedFormat, dtfInfo).format(value);
    }
  }

  private static DateTimeFormat getPredefinedFormat(PredefinedFormat predefinedFormat,
      DateTimeFormatInfo dtfInfo) {

    return DateTimeFormat.of(predefinedFormat, dtfInfo);
  }

  private static PredefinedFormat getTimeFormat(HasDateValue dateTime) {
    if (dateTime.getMillis() != 0) {
      return PredefinedFormat.HOUR24_MINUTE_SECOND_MILLISECOND;
    } else if (dateTime.getSecond() != 0) {
      return PredefinedFormat.HOUR24_MINUTE_SECOND;
    } else {
      return PredefinedFormat.HOUR24_MINUTE;
    }
  }
}
