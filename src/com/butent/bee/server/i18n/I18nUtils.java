package com.butent.bee.server.i18n;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.Messages;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.text.BreakIterator;
import java.text.Collator;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;

/**
 * Contains internationalization and localization related utility functions like
 * <code>getAvailableLocales</code> and <code>getIso3Language</code>.
 */

public final class I18nUtils {
  public static final char LOCALE_SEPARATOR = '_';

  /**
   * Compares two locales and returns the result of the comparison.
   */

  public static class LocaleComparator implements Comparator<Locale> {
    @Override
    public int compare(Locale o1, Locale o2) {
      if (o1 == o2) {
        return BeeConst.COMPARE_EQUAL;
      }
      if (o1 == null) {
        return BeeConst.COMPARE_LESS;
      }
      if (o2 == null) {
        return BeeConst.COMPARE_MORE;
      }
      return BeeUtils.compareNullsFirst(o1.toString(), o2.toString());
    }
  }

  public static <T extends Constants> T createConstants(Class<T> itf, Properties properties) {
    Assert.notNull(itf);
    Assert.notNull(properties);

    InvocationHandler ih = new GwtConstants(properties);
    return createProxy(itf, ih);
  }

  public static <T extends Messages> T createMessages(Class<T> itf, Properties properties) {
    Assert.notNull(itf);
    Assert.notNull(properties);

    InvocationHandler ih = new GwtMessages(properties);
    return createProxy(itf, ih);
  }

  public static List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> lst = Lists.newArrayList();

    Map<Locale, String> locales = Maps.newTreeMap(new LocaleComparator());
    for (Locale lc : Locale.getAvailableLocales()) {
      locales.put(lc, "Loc");
    }

    String sep = BeeConst.STRING_COMMA;
    for (Locale lc : BreakIterator.getAvailableLocales()) {
      locales.put(lc, BeeUtils.join(sep, locales.get(lc), "BrIt"));
    }
    for (Locale lc : Collator.getAvailableLocales()) {
      locales.put(lc, BeeUtils.join(sep, locales.get(lc), "Coll"));
    }
    for (Locale lc : DateFormat.getAvailableLocales()) {
      locales.put(lc, BeeUtils.join(sep, locales.get(lc), "DtF"));
    }
    for (Locale lc : DateFormatSymbols.getAvailableLocales()) {
      locales.put(lc, BeeUtils.join(sep, locales.get(lc), "DtFSymb"));
    }
    for (Locale lc : NumberFormat.getAvailableLocales()) {
      locales.put(lc, BeeUtils.join(sep, locales.get(lc), "NumF"));
    }
    for (Locale lc : Calendar.getAvailableLocales()) {
      locales.put(lc, BeeUtils.join(sep, locales.get(lc), "Cal"));
    }

    int i = 0;
    for (Map.Entry<Locale, String> entry : locales.entrySet()) {
      Locale lc = entry.getKey();
      String av = entry.getValue();
      lst.add(new ExtendedProperty(
          BeeUtils.join(BeeUtils.space(3), BeeUtils.progress(++i, locales.size()), lc.toString()),
          BeeUtils.join(" | ", lc.getDisplayName(), lc.getDisplayName(lc),
              (BeeUtils.count(av, BeeConst.CHAR_COMMA) == 7) ? null : av),
          BeeUtils.join(" ; ",
              BeeUtils.join(" | ", lc.getDisplayLanguage(), lc.getDisplayLanguage(lc)),
              BeeUtils.join(" | ", lc.getDisplayCountry(), lc.getDisplayCountry(lc)),
              BeeUtils.join(" | ", lc.getDisplayVariant(), lc.getDisplayVariant(lc)),
              getIso3Language(lc), getIso3Country(lc))));
    }
    return lst;
  }

  public static List<Property> getInfo() {
    List<Property> lst = PropertyUtils.createProperties("Default Locale", Locale.getDefault());

    Locale[] locales = Locale.getAvailableLocales();
    int len = ArrayUtils.length(locales);
    PropertyUtils.addProperty(lst, "Available Locales", len);
    if (len > 0) {
      Arrays.sort(locales, new LocaleComparator());
      Locale lc;
      for (int i = 0; i < len; i++) {
        lc = locales[i];
        lst.add(new Property(BeeUtils.join(BeeUtils.space(3), lc.toString(),
            BeeUtils.progress(i + 1, len)),
            BeeUtils.join(" | ", lc.getDisplayName(), lc.getDisplayName(lc))));
      }
    }

    String[] languages = Locale.getISOLanguages();
    len = ArrayUtils.length(languages);
    PropertyUtils.addProperty(lst, "Languages", len);
    if (len > 0) {
      Arrays.sort(languages);
      lst.addAll(PropertyUtils.createProperties("language", languages));
    }

    String[] countries = Locale.getISOCountries();
    len = ArrayUtils.length(countries);
    PropertyUtils.addProperty(lst, "Countries", len);
    if (len > 0) {
      Arrays.sort(countries);
      lst.addAll(PropertyUtils.createProperties("country", countries));
    }
    return lst;
  }

  public static String getIso3Country(Locale locale) {
    if (locale == null) {
      return BeeConst.STRING_EMPTY;
    }
    String country;
    try {
      country = locale.getISO3Country();
    } catch (MissingResourceException ex) {
      country = BeeConst.STRING_EMPTY;
    }
    return country;
  }

  public static String getIso3Language(Locale locale) {
    if (locale == null) {
      return BeeConst.STRING_EMPTY;
    }
    String lang;
    try {
      lang = locale.getISO3Language();
    } catch (MissingResourceException ex) {
      lang = BeeConst.STRING_EMPTY;
    }
    return lang;
  }

  public static Locale toLocale(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    for (Locale lc : Locale.getAvailableLocales()) {
      if (BeeUtils.same(lc.toString(), s)) {
        return lc;
      }
    }
    return null;
  }

  public static String toString(Locale locale) {
    if (locale == null) {
      return BeeConst.NULL;
    } else if (locale.equals(Locale.ROOT)) {
      return "ROOT";
    } else {
      return locale.toString();
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T createProxy(Class<T> itf, InvocationHandler ih) {
    return (T) Proxy.newProxyInstance(itf.getClassLoader(), new Class[] {itf}, ih);
  }

  private I18nUtils() {
  }
}
