package com.butent.bee.client.i18n;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.LocalizedNames;
import com.google.gwt.i18n.client.constants.NumberConstants;
import com.google.gwt.i18n.shared.DateTimeFormatInfo;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Enables user interface translation engine to extract local internationalization and localization
 * parameters.
 */

public final class LocaleUtils {

  private static final String LOCALE_SEPARATOR = "_";

  private static final String LOCALE_NAME_LT = "lt";
  private static final String LOCALE_NAME_EN = "en";

  private static final String[] LT_MONTHS_FULL = {
      "sausio", "vasario", "kovo", "balandžio", "gegužės", "birželio",
      "liepos", "rugpjūčio", "rugsėjo", "spalio", "lapkričio", "gruodžio"};

  private static final String LT_FORMAT_MONTH_ABBREV_DAY = "MMM d";
  private static final String LT_FORMAT_MONTH_FULL_DAY = "MMMM d";
  private static final String EN_FORMAT_MONTH_FULL_DAY = "d MMMM";
  private static final String EN_FORMAT_MOTH_ABBREV_DAY = "d MMM";

  public static boolean copyDateTimeFormat(Object src, Object dst) {
    if (src instanceof HasDateTimeFormat && dst instanceof HasDateTimeFormat && src != dst) {
      ((HasDateTimeFormat) dst).setDateTimeFormat(((HasDateTimeFormat) src).getDateTimeFormat());
      return true;
    } else {
      return false;
    }
  }

  public static boolean copyNumberFormat(Object src, Object dst) {
    if (src instanceof HasNumberFormat && dst instanceof HasNumberFormat && src != dst) {
      ((HasNumberFormat) dst).setNumberFormat(((HasNumberFormat) src).getNumberFormat());
      return true;
    } else {
      return false;
    }
  }

  public static List<ExtendedProperty> getInfo() {
    List<ExtendedProperty> lst = new ArrayList<>();

    String[] names = LocaleInfo.getAvailableLocaleNames();
    PropertyUtils.addExtended(lst, "Available Locale Names", ArrayUtils.length(names));
    int i = 0;
    for (String name : names) {
      PropertyUtils.addExtended(lst, "Name", i++, name);
    }

    PropertyUtils.addProperties(lst, false,
        "Locale Cookie Name", LocaleInfo.getLocaleCookieName(),
        "Locale Query Param", LocaleInfo.getLocaleQueryParam(),
        "Has Any RTL", LocaleInfo.hasAnyRTL());

    LocaleInfo cl = LocaleInfo.getCurrentLocale();
    Assert.notNull(cl);

    PropertyUtils.addProperties(lst, false, "Current Locale", cl.getLocaleName(),
        "Locale Native Display Name", LocaleInfo.getLocaleNativeDisplayName(cl.getLocaleName()),
        "Is RTL", cl.isRTL());

    LocalizedNames localizedNames = cl.getLocalizedNames();
    if (localizedNames != null) {
      String[] codes = localizedNames.getLikelyRegionCodes();
      PropertyUtils.addExtended(lst, "Likely Region Codes", ArrayUtils.length(codes));
      i = 0;
      for (String code : codes) {
        PropertyUtils.addExtended(lst, BeeUtils.joinWords("Region", i++),
            localizedNames.getRegionName(code), code);
      }

      codes = localizedNames.getSortedRegionCodes();
      PropertyUtils.addExtended(lst, "Sorted Region Codes", ArrayUtils.length(codes));
      i = 0;
      for (String code : codes) {
        PropertyUtils.addExtended(lst, BeeUtils.joinWords("Region", i++),
            localizedNames.getRegionName(code), code);
      }
    }

    DateTimeFormatInfo dtf = cl.getDateTimeFormatInfo();
    if (dtf != null) {
      PropertyUtils.addChildren(lst, "Date Time Format",
          "ampms", dtf.ampms(),
          "Date Format", dtf.dateFormat(),
          "Date Format Full", dtf.dateFormatFull(),
          "Date Format Long", dtf.dateFormatLong(),
          "Date Format Medium", dtf.dateFormatMedium(),
          "Date Format Short", dtf.dateFormatShort(),
          "Eras Full", dtf.erasFull(),
          "Eras Short", dtf.erasShort(),
          "First Day Of The Week", dtf.firstDayOfTheWeek(),
          "Format Day", dtf.formatDay(),
          "Format Hour 12 Minute", dtf.formatHour12Minute(),
          "Format Hour 12 Minute Second", dtf.formatHour12MinuteSecond(),
          "Format Hour 24 Minute", dtf.formatHour24Minute(),
          "Format Hour 24 Minute Second", dtf.formatHour24MinuteSecond(),
          "Format Minute Second", dtf.formatMinuteSecond(),
          "Format Month Abbrev", dtf.formatMonthAbbrev(),
          "Format Month Abbrev Day", dtf.formatMonthAbbrevDay(),
          "Format Month Full", dtf.formatMonthFull(),
          "Format Month Full Day", dtf.formatMonthFullDay(),
          "Format Month Full Weekday Day", dtf.formatMonthFullWeekdayDay(),
          "Format Month Num Day", dtf.formatMonthNumDay(),
          "Format Year", dtf.formatYear(),
          "Format Year Month Abbrev", dtf.formatYearMonthAbbrev(),
          "Format Year Month Abbrev Day", dtf.formatYearMonthAbbrevDay(),
          "Format Year Month Full", dtf.formatYearMonthFull(),
          "Format Year Month Full Day", dtf.formatYearMonthFullDay(),
          "Format Year Month Num", dtf.formatYearMonthNum(),
          "Format Year Month Num Day", dtf.formatYearMonthNumDay(),
          "Format Year Month Weekday Day", dtf.formatYearMonthWeekdayDay(),
          "Format Year Quarter Full", dtf.formatYearQuarterFull(),
          "Format Year Quarter Short", dtf.formatYearQuarterShort(),
          "Months Full", dtf.monthsFull(),
          "Months Full Standalone", dtf.monthsFullStandalone(),
          "Months Narrow", dtf.monthsNarrow(),
          "Months Narrow Standalone", dtf.monthsNarrowStandalone(),
          "Months Short", dtf.monthsShort(),
          "Months Short Standalone", dtf.monthsShortStandalone(),
          "Quarters Full", dtf.quartersFull(),
          "Quarters Short", dtf.quartersShort(),
          "Time Format", dtf.timeFormat(),
          "Time Format Full", dtf.timeFormatFull(),
          "Time Format Long", dtf.timeFormatLong(),
          "Time Format Medium", dtf.timeFormatMedium(),
          "Time Format Short", dtf.timeFormatShort(),
          "Weekdays Full", dtf.weekdaysFull(),
          "Weekdays Full Standalone", dtf.weekdaysFullStandalone(),
          "Weekdays Narrow", dtf.weekdaysNarrow(),
          "Weekdays Narrow Standalone", dtf.weekdaysNarrowStandalone(),
          "Weekdays Short", dtf.weekdaysShort(),
          "Weekdays Short Standalone", dtf.weekdaysShortStandalone(),
          "Weekend End", dtf.weekendEnd(),
          "Weekend Start", dtf.weekendStart());
    }

    NumberConstants nc = cl.getNumberConstants();
    if (nc != null) {
      PropertyUtils.addChildren(lst, "Number Constants",
          "Currency Pattern", nc.currencyPattern(),
          "Decimal Pattern", nc.decimalPattern(),
          "Decimal Separator", nc.decimalSeparator(),
          "Def Currency Code", nc.defCurrencyCode(),
          "Exponential Symbol", nc.exponentialSymbol(),
          "Grouping Separator", nc.groupingSeparator(),
          "Infinity", nc.infinity(),
          "Minus Sign", nc.minusSign(),
          "Monetary Grouping Separator", nc.monetaryGroupingSeparator(),
          "Monetary Separator", nc.monetarySeparator(),
          "Not A Number", nc.notANumber(),
          "Percent", nc.percent(),
          "Percent Pattern", nc.percentPattern(),
          "Per Mill", nc.perMill(),
          "Plus Sign", nc.plusSign(),
          "Scientific Pattern", nc.scientificPattern(),
          "Zero Digit", nc.zeroDigit());
    }
    return lst;
  }

  public static String getLanguageCode(LocaleInfo locale) {
    if (locale == null) {
      return BeeConst.STRING_EMPTY;
    }
    String name = locale.getLocaleName();
    int p = name.indexOf(LOCALE_SEPARATOR);
    if (p > 0) {
      return name.substring(0, p);
    } else {
      return name;
    }
  }

  /**
   * cldr patches.
   */
  static String formatMonthAbbrevDay(LocaleInfo localeInfo) {
    if (isLt(localeInfo)) {
      return LT_FORMAT_MONTH_ABBREV_DAY;
    } else if (isEn(localeInfo)) {
      return EN_FORMAT_MOTH_ABBREV_DAY;
    } else {
      Assert.notNull(localeInfo);
      return localeInfo.getDateTimeFormatInfo().formatMonthAbbrevDay();
    }
  }

  static String formatMonthFullDay(LocaleInfo localeInfo) {
    if (isLt(localeInfo)) {
      return LT_FORMAT_MONTH_FULL_DAY;
    } else if (isEn(localeInfo)) {
      return EN_FORMAT_MONTH_FULL_DAY;
    } else {
      Assert.notNull(localeInfo);
      return localeInfo.getDateTimeFormatInfo().formatMonthFullDay();
    }
  }

  static String[] monthsFull(LocaleInfo localeInfo) {
    if (isLt(localeInfo)) {
      return LT_MONTHS_FULL;
    } else {
      Assert.notNull(localeInfo);
      return localeInfo.getDateTimeFormatInfo().monthsFull();
    }
  }

  private static boolean isLt(LocaleInfo localeInfo) {
    return localeInfo != null && BeeUtils.same(localeInfo.getLocaleName(), LOCALE_NAME_LT);
  }

  private static boolean isEn(LocaleInfo localeInfo) {
    return localeInfo != null && BeeUtils.same(localeInfo.getLocaleName(), LOCALE_NAME_EN);
  }

  private LocaleUtils() {
  }
}
