package com.butent.bee.client.i18n;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.constants.NumberConstants;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.i18n.HasDateTimeFormat;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;

public final class LocaleUtils {

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

  private LocaleUtils() {
  }
}
