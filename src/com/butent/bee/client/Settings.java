package com.butent.bee.client;

import com.google.common.base.Splitter;
import com.google.gwt.i18n.client.Dictionary;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;

/**
 * Manages a settings array.
 */

public final class Settings {
  
  private static final Splitter VALUE_SPLITTER = 
      Splitter.on(BeeConst.CHAR_COMMA).omitEmptyStrings().trimResults(); 

  private static Dictionary settings;
  private static boolean initialized;

  public static int getActionSensitivityMillis() {
    return getPropertyInt("actionSensitivityMillis");
  }

  public static String getAppName() {
    return getProperty("appName");
  }
  
  public static List<Property> getInfo() {
    List<Property> info = new ArrayList<>();
    if (checkSettings()) {
      for (String key : settings.keySet()) {
        info.add(new Property(key, settings.get(key)));
      }
    }
    return info;
  }

  public static int getLogCapacity() {
    return getPropertyInt("logCapacity");
  }

  public static LogLevel getLogLevel() {
    String value = getProperty("logLevel");
    return BeeUtils.isEmpty(value) ? null : LogLevel.parse(value);
  }

  public static String getLogoImage() {
    return getProperty("logoImage");
  }

  public static String getLogoOpen() {
    return getProperty("logoOpen");
  }

  public static String getLogoTitle() {
    return getProperty("logoTitle");
  }

  public static List<String> getOnStartup() {
    return getList("onStartup");
  }
  
  public static String getProperty(String name) {
    Assert.notEmpty(name);
    if (checkSettings()) {
      return getQuietly(name);
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  public static boolean getPropertyBoolean(String name) {
    return BeeConst.isTrue(getProperty(name));
  }
  
  public static int getPropertyInt(String name) {
    String value = getProperty(name);
    if (BeeUtils.isInt(value)) {
      return BeeUtils.toInt(value);
    } else {
      return BeeConst.UNDEF;
    }
  }

  public static long getPropertyLong(String name) {
    String value = getProperty(name);
    if (BeeUtils.isLong(value)) {
      return BeeUtils.toLong(value);
    } else {
      return BeeConst.UNDEF;
    }
  }

  public static int getProviderMaxPrefetchSteps() {
    return getPropertyInt("providerMaxPrefetchSteps");
  }
  
  public static int getProviderMinPrefetchSteps() {
    return getPropertyInt("providerMinPrefetchSteps");
  }

  public static int getProviderRepeatMillis() {
    return getPropertyInt("providerRepeatMillis");
  }

  public static int getProviderSensitivityMillis() {
    return getPropertyInt("providerSensitivityMillis");
  }

  public static List<String> getScripts() {
    return getList("scripts");
  }
  
  public static long getStartMillis() {
    return getPropertyLong("startMillis");
  }
  
  public static List<String> getStyleSheets() {
    return getList("styleSheets");
  }

  public static String getVersion() {
    return getProperty("version");
  }
  
  public static String getWebSocketUrl() {
    return getProperty("webSocketUrl");
  }

  public static boolean minimizeNumberOfConcurrentRequests() {
    return getPropertyBoolean("minimizeNumberOfConcurrentRequests");
  }

  public static boolean showUserPhoto() {
    return getPropertyBoolean("showUserPhoto");
  }

  private static boolean checkSettings() {
    if (!initialized) {
      readSettings();
      initialized = true;
    }
    return settings != null;
  }

  private static List<String> getList(String name) {
    String value = getProperty(name);
    if (BeeUtils.isEmpty(value)) {
      return Collections.emptyList();
    } else {
      return VALUE_SPLITTER.splitToList(value);
    }
  }
  
  private static String getQuietly(String name) {
    String value;
    try {
      value = settings.get(name);
    } catch (MissingResourceException ex) {
      value = BeeConst.STRING_EMPTY;
    }
    return value;
  }

  private static void readSettings() {
    try {
      settings = Dictionary.getDictionary("BeeSettings");
    } catch (MissingResourceException ex) {
      settings = null;
    }
  }

  private Settings() {
  }
}
