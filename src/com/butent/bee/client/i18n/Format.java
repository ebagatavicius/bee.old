package com.butent.bee.client.i18n;

import com.google.gwt.i18n.client.CurrencyData;
import com.google.gwt.i18n.client.CurrencyList;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.client.constants.NumberConstants;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.DateOrdering;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.HasDateTimeFormat;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.PredefinedFormat;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.HasYearMonth;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages localized number and date formats.
 */

public final class Format {

  /**
   * Handles default formatting values.
   */

  private static class NumberConstantsImpl implements NumberConstants {

    @Override
    public String currencyPattern() {
      return DEFAULT_NUMBER_CONSTANTS.currencyPattern();
    }

    @Override
    public String decimalPattern() {
      return DEFAULT_NUMBER_CONSTANTS.decimalPattern();
    }

    @Override
    public String decimalSeparator() {
      return DEFAULT_DECIMAL_SEPARATOR;
    }

    @Override
    public String defCurrencyCode() {
      return DEFAULT_NUMBER_CONSTANTS.defCurrencyCode();
    }

    @Override
    public String exponentialSymbol() {
      return DEFAULT_NUMBER_CONSTANTS.exponentialSymbol();
    }

    @Override
    public String globalCurrencyPattern() {
      return DEFAULT_NUMBER_CONSTANTS.globalCurrencyPattern();
    }

    @Override
    public String groupingSeparator() {
      return DEFAULT_GROUPING_SEPARATOR;
    }

    @Override
    public String infinity() {
      return DEFAULT_NUMBER_CONSTANTS.infinity();
    }

    @Override
    public String minusSign() {
      return DEFAULT_NUMBER_CONSTANTS.minusSign();
    }

    @Override
    public String monetaryGroupingSeparator() {
      return DEFAULT_NUMBER_CONSTANTS.monetaryGroupingSeparator();
    }

    @Override
    public String monetarySeparator() {
      return DEFAULT_NUMBER_CONSTANTS.monetarySeparator();
    }

    @Override
    public String notANumber() {
      return DEFAULT_NUMBER_CONSTANTS.notANumber();
    }

    @Override
    public String percent() {
      return DEFAULT_NUMBER_CONSTANTS.percent();
    }

    @Override
    public String percentPattern() {
      return DEFAULT_NUMBER_CONSTANTS.percentPattern();
    }

    @Override
    public String perMill() {
      return DEFAULT_NUMBER_CONSTANTS.perMill();
    }

    @Override
    public String plusSign() {
      return DEFAULT_NUMBER_CONSTANTS.plusSign();
    }

    @Override
    public String scientificPattern() {
      return DEFAULT_NUMBER_CONSTANTS.scientificPattern();
    }

    @Override
    public String simpleCurrencyPattern() {
      return DEFAULT_NUMBER_CONSTANTS.simpleCurrencyPattern();
    }

    @Override
    public String zeroDigit() {
      return DEFAULT_NUMBER_CONSTANTS.zeroDigit();
    }
  }

  /**
   * Creates custom number formats with supplied parameters.
   */

  private static final class NumberFormatter extends NumberFormat {

    private NumberFormatter(String pattern) {
      this(pattern, CurrencyList.get().getDefault(), true);
    }

    private NumberFormatter(String pattern, CurrencyData cdata, boolean userSuppliedPattern) {
      super(NUMBER_CONSTANTS, pattern, cdata, userSuppliedPattern);
    }
  }

  private static final String DEFAULT_MONEY_PATTERN = "#,##0.00;(#)";

  private static final String DEFAULT_DECIMAL_PATTERN_INTEGER = "#,##0";

  private static final NumberConstants DEFAULT_NUMBER_CONSTANTS =
      LocaleInfo.getCurrentLocale().getNumberConstants();

  private static final NumberConstants NUMBER_CONSTANTS = new Format.NumberConstantsImpl();

  private static final String DEFAULT_DECIMAL_SEPARATOR = BeeConst.STRING_POINT;

  private static final String DEFAULT_GROUPING_SEPARATOR = BeeConst.STRING_SPACE;

  private static final NumberFormat defaultDoubleFormat = getNumberFormat("#.#######");

  private static final NumberFormat defaultIntegerFormat = getNumberFormat("#");

  private static final NumberFormat defaultLongFormat = getNumberFormat("#,###");

  private static final NumberFormat defaultMoneyFormat = getNumberFormat(DEFAULT_MONEY_PATTERN);

  private static final NumberFormat defaultPercentFormat = getNumberFormat("0.0%");

  private static final Map<PredefinedFormat, DateTimeFormat> pfCache = new HashMap<>();

  public static NumberFormat getDecimalFormat(int scale) {
    return getNumberFormat(getDecimalPattern(scale));
  }

  public static NumberFormat getDecimalFormat(int minScale, int maxScale) {
    return getNumberFormat(getDecimalPattern(minScale, maxScale));
  }

  public static String getDecimalPattern(int scale) {
    if (scale <= 0) {
      return DEFAULT_DECIMAL_PATTERN_INTEGER;
    } else {
      return DEFAULT_DECIMAL_PATTERN_INTEGER + BeeConst.STRING_POINT
          + BeeUtils.replicate(BeeConst.CHAR_ZERO, scale);
    }
  }

  public static String getDecimalPattern(int minScale, int maxScale) {
    if (minScale <= 0 && maxScale <= 0) {
      return DEFAULT_DECIMAL_PATTERN_INTEGER;

    } else {
      StringBuilder sb = new StringBuilder();
      sb.append(DEFAULT_DECIMAL_PATTERN_INTEGER).append(BeeConst.STRING_POINT);

      if (minScale > 0) {
        sb.append(BeeUtils.replicate(BeeConst.CHAR_ZERO, minScale));
      }
      if (maxScale > minScale) {
        for (int i = Math.max(minScale, 0); i < maxScale; i++) {
          sb.append(BeeConst.STRING_NUMBER_SIGN);
        }
      }

      return sb.toString();
    }
  }

  public static NumberFormat getDefaultMoneyFormat() {
    return defaultMoneyFormat;
  }

  public static int getDefaultMoneyScale() {
    return Localized.MONEY_SCALE;
  }

  public static DateOrdering getDefaultDateOrdering() {
    return getDefaultDateTimeFormatInfo().dateOrdering();
  }

  public static NumberFormat getDefaultDoubleFormat() {
    return defaultDoubleFormat;
  }

  public static NumberFormat getDefaultIntegerFormat() {
    return defaultIntegerFormat;
  }

  public static NumberFormat getDefaultLongFormat() {
    return defaultLongFormat;
  }

  public static NumberFormat getDefaultNumberFormat(ValueType type, int scale) {
    Assert.notNull(type);
    NumberFormat format;

    switch (type) {
      case DECIMAL:
        format = getDecimalFormat(scale);
        break;
      case INTEGER:
        format = getDefaultIntegerFormat();
        break;
      case LONG:
        format = getDefaultLongFormat();
        break;
      case NUMBER:
        format = getDefaultDoubleFormat();
        break;
      default:
        format = null;
    }
    return format;
  }

  public static NumberFormat getDefaultPercentFormat() {
    return defaultPercentFormat;
  }

  public static NumberFormat getNumberFormat(String pattern) {
    Assert.notEmpty(pattern);
    return new NumberFormatter(pattern);
  }

  public static NumberFormat getNumberFormat(String pattern, NumberFormat defaultFormat) {
    if (BeeUtils.isEmpty(pattern)) {
      return defaultFormat;
    } else {
      return getNumberFormat(pattern);
    }
  }

  public static DateTimeFormat getPredefinedFormat(PredefinedFormat predefinedFormat) {
    DateTimeFormat dateTimeFormat = pfCache.get(predefinedFormat);
    if (dateTimeFormat == null) {
      dateTimeFormat = DateTimeFormat.of(predefinedFormat, getDefaultDateTimeFormatInfo());
      pfCache.put(predefinedFormat, dateTimeFormat);
    }
    return dateTimeFormat;
  }

  public static List<String> getWeekdaysNarrowStandalone() {
    List<String> names = new ArrayList<>(TimeUtils.DAYS_PER_WEEK);

    String[] arr = getDefaultDateTimeFormatInfo().weekdaysNarrowStandalone();
    for (int i = 1; i < arr.length; i++) {
      names.add(arr[i]);
    }
    names.add(arr[0]);

    return names;
  }

  public static DateTimeFormat parseDateTimeFormat(String pattern) {
    DateTimeFormat format = parsePredefinedFormat(pattern);
    if (format == null) {
      format = parseDateTimePattern(pattern);
    }
    return format;
  }

  public static DateTimeFormat parseDateTimePattern(String pattern) {
    return DateTimeFormat.of(pattern, getDefaultDateTimeFormatInfo());
  }

  public static JustDate parseDateQuietly(DateTimeFormat format, String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    } else if (format == null) {
      return TimeUtils.parseDate(s, getDefaultDateOrdering());
    } else {
      return JustDate.get(parseDateTimeQuietly(format, s));
    }
  }

  public static DateTime parseDateTimeQuietly(DateTimeFormat format, String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    if (format != null) {
      DateTime result = format.parseQuietly(s);
      if (result != null) {
        return result;
      }
    }

    return TimeUtils.parseDateTime(s, getDefaultDateOrdering());
  }

  public static Double parseQuietly(NumberFormat format, String s) {
    if (format == null || BeeUtils.isEmpty(s)) {
      return null;
    }

    Double d;
    try {
      d = format.parse(s.trim());
    } catch (NumberFormatException ex) {
      d = null;
    }
    return d;
  }

  public static String properMonthFull(int month) {
    return BeeUtils.proper(renderMonthFullStandalone(month));
  }

  public static String properMonthShort(int month) {
    return BeeUtils.proper(renderMonthShortStandalone(month));
  }

  public static String quarterFull(int quarter) {
    if (TimeUtils.isQuarter(quarter)) {
      return getDefaultDateTimeFormatInfo().quartersFull()[quarter - 1];
    } else {
      return null;
    }
  }

  public static String quarterShort(int quarter) {
    if (TimeUtils.isQuarter(quarter)) {
      return getDefaultDateTimeFormatInfo().quartersShort()[quarter - 1];
    } else {
      return null;
    }
  }

  public static String render(Boolean value) {
    return BeeUtils.isTrue(value) ? BeeConst.STRING_CHECK_MARK : BeeConst.STRING_EMPTY;
  }

  public static String render(Number value, ValueType type, NumberFormat format, int scale) {
    if (value == null) {
      return null;
    }

    NumberFormat nf;
    if (format != null) {
      nf = format;
    } else if (ValueType.isNumeric(type)) {
      nf = getDefaultNumberFormat(type, scale);
    } else {
      nf = null;
    }

    if (nf == null) {
      return value.toString();
    } else {
      return nf.format(value);
    }
  }

  public static String render(String value, ValueType type, DateTimeFormat dateTimeFormat,
      NumberFormat numberFormat, int scale) {
    if (type == null) {
      return value;
    }

    final String result;

    switch (type) {
      case BOOLEAN:
        result = render(BeeUtils.toBooleanOrNull(value));
        break;

      case DATE:
        JustDate jd = TimeUtils.toDateOrNull(value);
        if (jd == null) {
          result = null;
        } else if (dateTimeFormat != null) {
          result = dateTimeFormat.format(jd);
        } else {
          result = renderDate(jd);
        }
        break;

      case DATE_TIME:
        DateTime dt = TimeUtils.toDateTimeOrNull(value);
        if (dt == null) {
          result = null;
        } else if (dateTimeFormat != null) {
          result = dateTimeFormat.format(dt);
        } else {
          result = renderDateTime(dt);
        }
        break;

      case DECIMAL:
        result = render(BeeUtils.toDecimalOrNull(value), type, numberFormat, scale);
        break;

      case INTEGER:
        result = render(BeeUtils.toIntOrNull(value), type, numberFormat, scale);
        break;

      case LONG:
        result = render(BeeUtils.toLongOrNull(value), type, numberFormat, scale);
        break;

      case NUMBER:
        result = render(BeeUtils.toDoubleOrNull(value), type, numberFormat, scale);
        break;

      case TEXT:
      case TIME_OF_DAY:
        result = BeeUtils.trimRight(value);
        break;

      default:
        Assert.untouchable();
        result = null;
    }
    return result;
  }

  public static String render(PredefinedFormat predefinedFormat, HasDateValue value) {
    if (predefinedFormat == null || value == null) {
      return null;
    } else {
      return getPredefinedFormat(predefinedFormat).format(value);
    }
  }

  public static String renderDate(HasDateValue date) {
    return render(PredefinedFormat.DATE_SHORT, date);
  }

  public static String renderDateFull(HasDateValue date) {
    return render(PredefinedFormat.DATE_FULL, date);
  }

  public static String renderDateLong(HasDateValue date) {
    if (date == null) {
      return null;

    } else {
      PredefinedFormat predefinedFormat = TimeUtils.hasTimePart(date)
          ? PredefinedFormat.DATE_TIME_LONG : PredefinedFormat.DATE_LONG;

      return render(predefinedFormat, date);
    }
  }

  public static String renderDateTime(DateTime dateTime) {
    if (dateTime == null) {
      return null;

    } else if (dateTime.hasTimePart()) {
      PredefinedFormat timeFormat;

      if (dateTime.getMillis() != 0) {
        timeFormat = PredefinedFormat.HOUR24_MINUTE_SECOND_MILLISECOND;
      } else if (dateTime.getSecond() != 0) {
        timeFormat = PredefinedFormat.HOUR24_MINUTE_SECOND;
      } else {
        timeFormat = PredefinedFormat.HOUR24_MINUTE;
      }

      DateTimeFormatInfo dtfInfo = getDefaultDateTimeFormatInfo();
      String pattern = dtfInfo.dateTime(PredefinedFormat.DATE_SHORT.getPattern(dtfInfo),
          timeFormat.getPattern(dtfInfo));

      return DateTimeFormat.of(pattern, dtfInfo).format(dateTime);

    } else {
      return render(PredefinedFormat.DATE_SHORT, dateTime);
    }
  }

  public static String renderDateTimeFull(DateTime dateTime) {
    return render(PredefinedFormat.DATE_TIME_FULL, dateTime);
  }

  public static String renderDayOfWeek(HasDateValue date) {
    return (date == null) ? null : renderDayOfWeek(date.getDow());
  }

  public static String renderDayOfWeek(int dow) {
    if (TimeUtils.isDow(dow)) {
      int index = (dow == 7) ? 0 : dow;
      return getDefaultDateTimeFormatInfo().weekdaysFull()[index];
    } else {
      return null;
    }
  }

  public static String renderDayOfWeekShort(int dow) {
    if (TimeUtils.isDow(dow)) {
      int index = (dow == 7) ? 0 : dow;
      return getDefaultDateTimeFormatInfo().weekdaysShort()[index];
    } else {
      return null;
    }
  }

  public static String renderMonthFullStandalone(HasYearMonth date) {
    return (date == null) ? null : renderMonthFullStandalone(date.getMonth());
  }

  public static String renderMonthFullStandalone(int month) {
    if (TimeUtils.isMonth(month)) {
      return getDefaultDateTimeFormatInfo().monthsFullStandalone()[month - 1];
    } else {
      return null;
    }
  }

  public static String renderMonthShortStandalone(int month) {
    if (TimeUtils.isMonth(month)) {
      return getDefaultDateTimeFormatInfo().monthsShortStandalone()[month - 1];
    } else {
      return null;
    }
  }

  public static String renderPeriod(DateTime start, DateTime end) {
    if (start == null || end == null || start.hasTimePart() || end.hasTimePart()) {
      return TimeUtils.renderPeriod(start, end);

    } else if (TimeUtils.dayDiff(start, end) == 1) {
      return render(PredefinedFormat.DATE_LONG, start);

    } else if (start.getDom() == 1 && end.getDom() == 1 && TimeUtils.monthDiff(start, end) == 1) {
      return render(PredefinedFormat.YEAR_MONTH_STANDALONE, start);

    } else if (start.getMonth() % 3 == 1 && start.getDom() == 1 && end.getDom() == 1
        && TimeUtils.monthDiff(start, end) == 3) {
      return render(PredefinedFormat.YEAR_QUARTER, start);

    } else if (start.getMonth() == 1 && start.getDom() == 1
        && end.getYear() == start.getYear() + 1 && end.getMonth() == 1 && end.getDom() == 1) {
      return render(PredefinedFormat.YEAR, start);

    } else {
      return TimeUtils.renderPeriod(start, end);
    }
  }

  public static String renderYearMonth(HasYearMonth ym) {
    if (ym == null) {
      return null;
    } else {
      return render(PredefinedFormat.YEAR_MONTH_STANDALONE, ym.getDate());
    }
  }

  public static void setFormat(Object target, ValueType type, String pattern) {
    Assert.notNull(target);
    Assert.notEmpty(pattern);

    if (target instanceof HasDateTimeFormat) {
      DateTimeFormat predefinedFormat = parsePredefinedFormat(pattern);
      if (predefinedFormat != null) {
        ((HasDateTimeFormat) target).setDateTimeFormat(predefinedFormat);
        return;
      }
    }

    boolean isDt;
    boolean isNum;

    if (target instanceof HasDateTimeFormat && target instanceof HasNumberFormat) {
      isDt = ValueType.isDateOrDateTime(type);
      isNum = ValueType.isNumeric(type);
    } else {
      isDt = target instanceof HasDateTimeFormat;
      isNum = target instanceof HasNumberFormat;
    }

    if (isDt) {
      ((HasDateTimeFormat) target).setDateTimeFormat(DateTimeFormat.of(pattern,
          getDefaultDateTimeFormatInfo()));
    } else if (isNum) {
      ((HasNumberFormat) target).setNumberFormat(new NumberFormatter(pattern));
    }
  }

  private static DateTimeFormatInfo getDefaultDateTimeFormatInfo() {
    return BeeKeeper.getUser().getDateTimeFormatInfo();
  }

  private static DateTimeFormat parsePredefinedFormat(String name) {
    for (PredefinedFormat pf : PredefinedFormat.values()) {
      if (BeeUtils.same(name, pf.name())) {
        return DateTimeFormat.of(pf, getDefaultDateTimeFormatInfo());
      }
    }
    return null;
  }

  private Format() {
  }
}
