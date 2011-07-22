package com.butent.bee.client.i18n;

import com.google.gwt.i18n.client.CurrencyData;
import com.google.gwt.i18n.client.CurrencyList;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.client.constants.NumberConstants;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Manages localized number and date formats.
 */

public class Format {

  /**
   * Handles default formatting values.
   */

  private static class NumberConstantsImpl implements NumberConstants {

    public String currencyPattern() {
      return defaultNumberConstants.currencyPattern();
    }

    public String decimalPattern() {
      return defaultNumberConstants.decimalPattern();
    }

    public String decimalSeparator() {
      if (defaultDecimalSeparator == null) {
        return defaultNumberConstants.decimalSeparator();
      } else {
        return defaultDecimalSeparator;
      }
    }

    public String defCurrencyCode() {
      return defaultNumberConstants.defCurrencyCode();
    }

    public String exponentialSymbol() {
      return defaultNumberConstants.exponentialSymbol();
    }

    public String groupingSeparator() {
      if (defaultGroupingSeparator == null) {
        return defaultNumberConstants.groupingSeparator();
      } else {
        return defaultGroupingSeparator;
      }
    }

    public String infinity() {
      return defaultNumberConstants.infinity();
    }

    public String minusSign() {
      return defaultNumberConstants.minusSign();
    }

    public String monetaryGroupingSeparator() {
      return defaultNumberConstants.monetaryGroupingSeparator();
    }

    public String monetarySeparator() {
      return defaultNumberConstants.monetarySeparator();
    }

    public String notANumber() {
      return defaultNumberConstants.notANumber();
    }

    public String percent() {
      return defaultNumberConstants.percent();
    }

    public String percentPattern() {
      return defaultNumberConstants.percentPattern();
    }

    public String perMill() {
      return defaultNumberConstants.perMill();
    }

    public String plusSign() {
      return defaultNumberConstants.plusSign();
    }

    public String scientificPattern() {
      return defaultNumberConstants.scientificPattern();
    }

    public String zeroDigit() {
      return defaultNumberConstants.zeroDigit();
    }
  }

  /**
   * Creates custom number formats with supplied parameters.
   */

  private static class NumberFormatter extends NumberFormat {

    private NumberFormatter(String pattern) {
      this(pattern, CurrencyList.get().getDefault(), true);
    }

    private NumberFormatter(String pattern, CurrencyData cdata, boolean userSuppliedPattern) {
      super(numberConstants, pattern, cdata, userSuppliedPattern);
    }
  }

  private static final NumberConstants defaultNumberConstants =
      LocaleInfo.getCurrentLocale().getNumberConstants();

  private static final NumberConstants numberConstants = new Format.NumberConstantsImpl();

  private static String defaultDecimalSeparator = BeeConst.STRING_POINT;

  private static String defaultGroupingSeparator = BeeConst.STRING_SPACE;

  private static NumberFormat defaultDoubleFormat = getNumberFormat("#.#######");

  private static NumberFormat defaultIntegerFormat = getNumberFormat("#");

  private static NumberFormat defaultLongFormat = getNumberFormat("#,###");

  private static NumberFormat defaultCurrencyFormat = getNumberFormat("#,##0.00;(#)");

  private static String defaultDecimalPatternInteger = "#,##0";

  private static DateTimeFormat defaultDateFormat =
      DateTimeFormat.getFormat(PredefinedFormat.DATE_MEDIUM);

  private static DateTimeFormat defaultDateTimeFormat =
      DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM);
  
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
    if (scale <= 0) {
      return getNumberFormat(defaultDecimalPatternInteger);
    }
    return getNumberFormat(defaultDecimalPatternInteger + BeeConst.STRING_POINT
        + BeeUtils.replicate(BeeConst.CHAR_ZERO, scale));
  }

  public static NumberFormat getDefaultCurrencyFormat() {
    return defaultCurrencyFormat;
  }

  public static DateTimeFormat getDefaultDateFormat() {
    return defaultDateFormat;
  }

  public static DateTimeFormat getDefaultDateTimeFormat() {
    return defaultDateTimeFormat;
  }

  public static String getDefaultDecimalPatternInteger() {
    return defaultDecimalPatternInteger;
  }

  public static String getDefaultDecimalSeparator() {
    return defaultDecimalSeparator;
  }

  public static NumberFormat getDefaultDoubleFormat() {
    return defaultDoubleFormat;
  }

  public static String getDefaultGroupingSeparator() {
    return defaultGroupingSeparator;
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
  
  public static void setDefaultCurrencyFormat(NumberFormat defaultCurrencyFormat) {
    Format.defaultCurrencyFormat = defaultCurrencyFormat;
  }

  public static void setDefaultDateFormat(DateTimeFormat defaultDateFormat) {
    Format.defaultDateFormat = defaultDateFormat;
  }

  public static void setDefaultDateTimeFormat(DateTimeFormat defaultDateTimeFormat) {
    Format.defaultDateTimeFormat = defaultDateTimeFormat;
  }

  public static void setDefaultDecimalPatternInteger(String defaultDecimalPatternInteger) {
    Format.defaultDecimalPatternInteger = defaultDecimalPatternInteger;
  }

  public static void setDefaultDecimalSeparator(String defaultDecimalSeparator) {
    Format.defaultDecimalSeparator = defaultDecimalSeparator;
  }

  public static void setDefaultDoubleFormat(NumberFormat defaultDoubleFormat) {
    Format.defaultDoubleFormat = defaultDoubleFormat;
  }

  public static void setDefaultGroupingSeparator(String defaultGroupingSeparator) {
    Format.defaultGroupingSeparator = defaultGroupingSeparator;
  }

  public static void setDefaultIntegerFormat(NumberFormat defaultIntegerFormat) {
    Format.defaultIntegerFormat = defaultIntegerFormat;
  }

  public static void setDefaultLongFormat(NumberFormat defaultLongFormat) {
    Format.defaultLongFormat = defaultLongFormat;
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
      isDt = (target instanceof HasDateTimeFormat);
      isNum = (target instanceof HasNumberFormat);
    }

    if (isDt) {
      ((HasDateTimeFormat) target).setDateTimeFormat(DateTimeFormat.getFormat(pattern));
    } else if (isNum) {
      ((HasNumberFormat) target).setNumberFormat(new NumberFormatter(pattern));
    }
  }

  private Format() {
  }
}
