package com.butent.bee.client;

import com.google.common.base.Splitter;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Settings {

  private static final Splitter VALUE_SPLITTER =
      Splitter.on(BeeConst.CHAR_COMMA).omitEmptyStrings().trimResults();

  private static JSONObject settings;
  private static boolean initialized;

  public static int getActionSensitivityMillis() {
    return getInt("actionSensitivityMillis");
  }

  public static String getAppName() {
    return getString("appName");
  }

  public static boolean getBoolean(String key) {
    JSONValue value = getValue(key);
    if (value != null) {
      JSONBoolean b = value.isBoolean();
      return (b == null) ? false : b.booleanValue();
    } else {
      return false;
    }
  }

  public static Double getDouble(String key) {
    JSONValue value = getValue(key);
    if (value != null) {
      JSONNumber number = value.isNumber();
      return (number == null) ? null : number.doubleValue();
    } else {
      return null;
    }
  }

  public static List<Property> getInfo() {
    List<Property> info = new ArrayList<>();
    if (checkSettings()) {
      for (String key : settings.keySet()) {
        info.add(new Property(key, settings.get(key).toString()));
      }
    }
    return info;
  }

  public static int getInt(String key) {
    Double d = getDouble(key);
    return (d == null) ? BeeConst.UNDEF : BeeUtils.toInt(d);
  }

  public static int getLogCapacity() {
    return getInt("logCapacity");
  }

  public static LogLevel getLogLevel() {
    String value = getString("logLevel");
    return BeeUtils.isEmpty(value) ? null : LogLevel.parse(value);
  }

  public static String getLogoImage() {
    return getString("logoImage");
  }

  public static String getLogoOpen() {
    return getString("logoOpen");
  }

  public static String getLogoTitle() {
    return getString("logoTitle");
  }

  public static long getLong(String key) {
    Double d = getDouble(key);
    return (d == null) ? BeeConst.UNDEF : BeeUtils.toLong(d);
  }

  public static JSONObject getOnEmptyWorkspace() {
    return getObject("onEmptyWorkspace");
  }

  public static JSONObject getOnStartup() {
    return getObject("onStartup");
  }

  public static int getProviderMaxPrefetchSteps() {
    return getInt("providerMaxPrefetchSteps");
  }

  public static int getProviderMinPrefetchSteps() {
    return getInt("providerMinPrefetchSteps");
  }

  public static int getProviderRepeatMillis() {
    return getInt("providerRepeatMillis");
  }

  public static int getProviderSensitivityMillis() {
    return getInt("providerSensitivityMillis");
  }

  public static List<String> getScripts() {
    return getList("scripts");
  }

  public static List<String> getStyleSheets() {
    return getList("styleSheets");
  }

  public static String getVersion() {
    return getString("version");
  }

  public static String getWebSocketUrl() {
    return getString("webSocketUrl");
  }

  public static boolean minimizeNumberOfConcurrentRequests() {
    return getBoolean("minimizeNumberOfConcurrentRequests");
  }

  public static boolean showUserPhoto() {
    return getBoolean("showUserPhoto");
  }

  private static boolean checkSettings() {
    if (!initialized) {
      JavaScriptObject jso = read();
      if (jso != null) {
        settings = new JSONObject(jso);
      }
      initialized = true;
    }
    return settings != null;
  }

  private static List<String> getList(String key) {
    String value = getString(key);
    if (BeeUtils.isEmpty(value)) {
      return Collections.emptyList();
    } else {
      return VALUE_SPLITTER.splitToList(value);
    }
  }

  private static JSONObject getObject(String key) {
    JSONValue value = getValue(key);
    if (value != null) {
      return value.isObject();
    } else {
      return null;
    }
  }

  private static String getString(String key) {
    JSONValue value = getValue(key);
    if (value != null) {
      JSONString string = value.isString();
      return (string == null) ? null : string.stringValue();
    } else {
      return null;
    }
  }

  private static JSONValue getValue(String key) {
    if (checkSettings() && settings.containsKey(key)) {
      return settings.get(key);
    } else {
      return null;
    }
  }

//@formatter:off
  private static native JavaScriptObject read() /*-{
    return $wnd['BeeSettings'];
  }-*/;
//@formatter:on

  private Settings() {
  }
}
