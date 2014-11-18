package com.butent.bee.shared.i18n;

import com.google.common.base.Splitter;

import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Localized {

  private static final BeeLogger logger = LogUtils.getLogger(Localized.class);

  private static final char L10N_PREFIX = '=';
  private static final char L10N_SEPARATOR = '+';

  public static final Splitter L10N_SPLITTER = Splitter.on(L10N_SEPARATOR);

  private static LocalizableConstants constants;
  private static LocalizableMessages messages;

  private static Map<String, String> dictionary;

  public static LocalizableConstants getConstants() {
    return constants;
  }

  public static Map<String, String> getDictionary() {
    return dictionary;
  }

  public static String getLabel(IsColumn column) {
    return maybeTranslate(column.getLabel());
  }

  public static List<String> getLabels(List<? extends IsColumn> columns) {
    List<String> labels = new ArrayList<>();
    for (IsColumn column : columns) {
      labels.add(getLabel(column));
    }
    return labels;
  }

  public static LocalizableMessages getMessages() {
    return messages;
  }

  public static String maybeTranslate(String text) {
    return maybeTranslate(text, dictionary);
  }

  public static String maybeTranslate(String text, Map<String, String> dict) {
    if (text == null || text.length() < 3 || text.charAt(0) != L10N_PREFIX) {
      return text;
    }

    String localized;

    if (text.indexOf(L10N_SEPARATOR) > 0) {
      StringBuilder sb = new StringBuilder();

      for (String s : L10N_SPLITTER.split(text)) {
        sb.append(maybeTranslate(s, dict));
      }

      localized = sb.toString();

    } else {
      localized = translate(text.substring(1), dict);
    }

    if (localized == null) {
      logger.warning("cannot localize:", text);
      return text;
    } else {
      return localized;
    }
  }

  public static void setConstants(LocalizableConstants constants) {
    Localized.constants = constants;
  }

  public static void setDictionary(Map<String, String> dictionary) {
    Localized.dictionary = dictionary;
  }

  public static void setMessages(LocalizableMessages messages) {
    Localized.messages = messages;
  }

  public static String translate(String key) {
    return translate(key, dictionary);
  }

  private static String translate(String key, Map<String, String> dict) {
    return (dict == null) ? null : dict.get(key);
  }

  private Localized() {
  }
}
