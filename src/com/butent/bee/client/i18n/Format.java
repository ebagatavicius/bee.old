package com.butent.bee.client.i18n;

import com.google.gwt.i18n.client.CurrencyData;
import com.google.gwt.i18n.client.CurrencyList;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.client.constants.NumberConstants;

import com.butent.bee.client.i18n.DateTimeFormat.PredefinedFormat;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.HasYearMonth;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

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

  public static DateTimeFormat getDateTimeFormat(String pattern) {
    Assert.notEmpty(pattern);
    DateTimeFormat format = getPredefinedFormat(pattern);
    if (format == null) {
      format = DateTimeFormat.getFormat(pattern);
    }
    return format;
  }

  public static DateTimeFormat getDateTimeFormat(String pattern, DateTimeFormat defaultFormat) {
    if (BeeUtils.isEmpty(pattern)) {
      return defaultFormat;
    } else {
      return getDateTimeFormat(pattern);
    }
  }

  public static NumberFormat getDecimalFormat(int scale) {
    return getNumberFormat(getDecimalPattern(scale));
  }

  public static String getDecimalPattern(int scale) {
    if (scale <= 0) {
      return DEFAULT_DECIMAL_PATTERN_INTEGER;
    } else {
      return DEFAULT_DECIMAL_PATTERN_INTEGER + BeeConst.STRING_POINT
          + BeeUtils.replicate(BeeConst.CHAR_ZERO, scale);
    }
  }

  public static NumberFormat getDefaultCurrencyFormat() {
    return defaultCurrencyFormat;
  }

  public static int getDefaultCurrencyScale() {
    return DEFAULT_CURRENCY_SCALE;
  }

  public static DateTimeFormat getDefaultDateFormat() {
    return defaultDateFormat;
  }

  public static DateTimeFormat getDefaultDateTimeFormat() {
    return defaultDateTimeFormat;
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

  public static DateTimeFormat getDefaultTimeFormat() {
    return defaultTimeFormat;
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

  public static DateTimeFormat getPredefinedFormat(String name) {
    Assert.notNull(name);
    for (DateTimeFormat.PredefinedFormat predef : DateTimeFormat.PredefinedFormat.values()) {
      if (BeeUtils.same(name, predef.name())) {
        return DateTimeFormat.getFormat(predef);
      }
    }
    return null;
  }

  public static JustDate parseDateQuietly(DateTimeFormat format, String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    } else if (format == null) {
      return TimeUtils.parseDate(s);
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

    return TimeUtils.parseDateTime(s);
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
        } else if (getDefaultDateFormat() != null) {
          result = getDefaultDateFormat().format(jd);
        } else {
          result = jd.toString();
        }
        break;

      case DATE_TIME:
        DateTime dt = TimeUtils.toDateTimeOrNull(value);
        if (dt == null) {
          result = null;
        } else if (dateTimeFormat != null) {
          result = dateTimeFormat.format(dt);
        } else if (getDefaultDateTimeFormat() != null) {
          result = getDefaultDateTimeFormat().format(dt);
        } else {
          result = dt.toString();
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

  public static String renderDateFull(HasDateValue date) {
    if (date == null) {
      return null;
    } else {
      return DateTimeFormat.getFormat(PredefinedFormat.DATE_FULL).format(date);
    }
  }

  public static String renderDayOfWeek(HasDateValue date) {
    return (date == null) ? null : renderDayOfWeek(date.getDow());
  }

  public static String renderDayOfWeek(int dow) {
    if (TimeUtils.isDow(dow)) {
      int index = (dow == 7) ? 0 : dow;
      return LocaleInfo.getCurrentLocale().getDateTimeFormatInfo().weekdaysFull()[index];
    } else {
      return null;
    }
  }

  public static String renderMonthFull(HasYearMonth date) {
    return (date == null)
        ? null : LocaleUtils.monthsFull(LocaleInfo.getCurrentLocale())[date.getMonth() - 1];
  }

  public static String renderMonthFullStandalone(HasYearMonth date) {
    return (date == null) ? null : renderMonthFullStandalone(date.getMonth());
  }

  public static String renderMonthFullStandalone(int month) {
    if (TimeUtils.isMonth(month)) {
      return LocaleInfo.getCurrentLocale().getDateTimeFormatInfo()
          .monthsFullStandalone()[month - 1];
    } else {
      return null;
    }
  }

  public static String renderPeriod(DateTime start, DateTime end) {
    if (start == null || end == null || start.hasTimePart() || end.hasTimePart()) {
      return TimeUtils.renderPeriod(start, end);

    } else if (TimeUtils.dayDiff(start, end) == 1) {
      return DateTimeFormat.getFormat(PredefinedFormat.DATE_LONG).format(start);

    } else if (start.getDom() == 1 && end.getDom() == 1 && TimeUtils.monthDiff(start, end) == 1) {
      return BeeUtils.joinWords(start.getYear(), renderMonthFullStandalone(start.getMonth()));

    } else if (start.getMonth() % 3 == 1 && start.getDom() == 1 && end.getDom() == 1
        && TimeUtils.monthDiff(start, end) == 3) {
      return DateTimeFormat.getFormat(PredefinedFormat.YEAR_QUARTER).format(start);

    } else if (start.getMonth() == 1 && start.getDom() == 1
        && end.getYear() == start.getYear() + 1 && end.getMonth() == 1 && end.getDom() == 1) {
      return BeeUtils.toString(start.getYear());

    } else {
      return TimeUtils.renderPeriod(start, end);
    }
  }

  public static void setFormat(Object target, ValueType type, String pattern) {
    Assert.notNull(target);
    Assert.notEmpty(pattern);

    if (target instanceof HasDateTimeFormat) {
      DateTimeFormat predefinedFormat = getPredefinedFormat(pattern);
      if (predefinedFormat != null) {
        ((HasDateTimeFormat) target).setDateTimeFormat(predefinedFormat);
        return;
      }
    }

    boolean isDt = false;
    boolean isNum = false;

    if (target instanceof HasDateTimeFormat && target instanceof HasNumberFormat) {
      isDt = ValueType.isDateOrDateTime(type);
      isNum = ValueType.isNumeric(type);
    } else {
      isDt = target instanceof HasDateTimeFormat;
      isNum = target instanceof HasNumberFormat;
    }

    if (isDt) {
      ((HasDateTimeFormat) target).setDateTimeFormat(DateTimeFormat.getFormat(pattern));
    } else if (isNum) {
      ((HasNumberFormat) target).setNumberFormat(new NumberFormatter(pattern));
    }
  }

  private static final int DEFAULT_CURRENCY_SCALE = 2;

  private static final String DEFAULT_CURRENCY_PATTERN = "#,##0.00;(#)";

  private static final String DEFAULT_DECIMAL_PATTERN_INTEGER = "#,##0";

  private static final NumberConstants DEFAULT_NUMBER_CONSTANTS =
      LocaleInfo.getCurrentLocale().getNumberConstants();

  private static final NumberConstants NUMBER_CONSTANTS = new Format.NumberConstantsImpl();

  private static final String DEFAULT_DECIMAL_SEPARATOR = BeeConst.STRING_POINT;

  private static final String DEFAULT_GROUPING_SEPARATOR = BeeConst.STRING_SPACE;

  private static NumberFormat defaultDoubleFormat = getNumberFormat("#.#######");

  private static NumberFormat defaultIntegerFormat = getNumberFormat("#");

  private static NumberFormat defaultLongFormat = getNumberFormat("#,###");

  private static NumberFormat defaultCurrencyFormat = getNumberFormat(DEFAULT_CURRENCY_PATTERN);

  private static NumberFormat defaultPercentFormat = getNumberFormat("0.0%");

  private static DateTimeFormat defaultDateFormat =
      DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT);

  private static DateTimeFormat defaultDateTimeFormat =
      DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_SHORT);

  private static DateTimeFormat defaultTimeFormat =
      DateTimeFormat.getFormat(PredefinedFormat.TIME_SHORT);

  private Format() {
  }
}
