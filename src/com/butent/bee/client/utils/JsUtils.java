// CHECKSTYLE:OFF
package com.butent.bee.client.utils;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.JsDate;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains javascript related utility functions, like array operations or type conversions.
 */

public final class JsUtils {

//@formatter:off
  public static native void clearProperty(JavaScriptObject obj, String p) /*-{
    if (typeof (obj) != "object") {
      return;
    }
    if (p == undefined || p == null || p == "") {
      return;
    }

    if (typeof (obj[p]) != "undefined") {
      obj[p] = undefined;
    }
  }-*/;

  public static native JsArrayString createArray(String src) /*-{
    var arr = new Array(src);
    return arr;
  }-*/;

  public static native JsArrayString createArray(int length) /*-{
    var arr = new Array(length);
    return arr;
  }-*/;

  public static native double currentTimeMillis() /*-{
    return (new Date()).getTime();
  }-*/;

  public static native String doMethod(JavaScriptObject obj, String p) /*-{
    var s;

    try {
      var x = obj[p]();
      if (x == undefined || x == null) {
        s == null;
      } else {
        s = String(x);
      }
    } catch (err) {
      s = null;
    }

    return s;
  }-*/;

  public static native JavaScriptObject eval(String xpr) /*-{
    try {
      return eval(xpr);
    } catch (err) {
      return err;
    }
  }-*/;

  public static native int evalToInt(String xpr) /*-{
    try {
      var v = eval(xpr);
      return parseInt(v) || 0;
    } catch (err) {
      return 0;
    }
  }-*/;

  public static native String evalToString(String xpr) /*-{
    try {
      var z = eval(xpr);
      if (typeof z == "object") {
        return JSON.stringify(z);
      } else {
        return String(z);
      }
    } catch (err) {
      return err.toString();
    }
  }-*/;

  public static native JsArrayString getFunctions(JavaScriptObject obj, String pattern) /*-{
    var arr = new Array();
    var i = 0;
    var v = null;
    var tp = null;
    var ok = true;

    for ( var p in obj) {
      try {
        v = obj[p];
        tp = typeof (v);
        ok = true;
      } catch (err) {
        ok = false;
      }

      if (!ok) {
        continue;
      }
      if (tp != "function") {
        continue;
      }
      if (pattern != null && pattern.length > 0 && p.search(new RegExp(pattern, "i")) < 0) {
        continue;
      }

      arr[i] = p;
      i++;
    }
    return arr;
  }-*/;
//@formatter:on

  public static List<Property> getInfo(JavaScriptObject obj) {
    List<Property> info = new ArrayList<>();
    JsArrayString arr = getProperties(obj);
    if (arr == null || arr.length() < 3) {
      return info;
    }

    for (int i = 0; i < arr.length(); i += 3) {
      String value = arr.get(i + 2);
      if (!BeeUtils.isEmpty(value)) {
        info.add(new Property(arr.get(i), value));
      }
    }
    return info;
  }

  public static JsArrayString getProperties(JavaScriptObject obj) {
    Assert.notNull(obj);
    return getProperties(obj, null);
  }

//@formatter:off
  public static native JsArrayString getProperties(JavaScriptObject obj, String pattern) /*-{
    var arr = new Array();
    var i = 0;
    var v = null;
    var tp = null;
    var ok = true;

    for ( var p in obj) {
      try {
        v = obj[p];
        tp = typeof (v);
        ok = true;
      } catch (err) {
        arr[i] = p;
        arr[i + 1] = "error";
        arr[i + 2] = String(err);
        ok = false;
      }

      if (!ok) {
        continue;
      }
      if (tp == "function") {
        continue;
      }

      if (pattern != null && pattern.length > 0 && p.search(new RegExp(pattern, "i")) < 0) {
        continue;
      }

      arr[i] = p;
      try {
        arr[i + 1] = typeof (obj[p]);
        arr[i + 2] = String(obj[p]);
      } catch (err) {
        arr[i + 1] = "error";
        arr[i + 2] = String(err);
      }
      i += 3;
    }
    return arr;
  }-*/;

  public static native String getProperty(JavaScriptObject obj, String p) /*-{
    return String(obj[p]);
  }-*/;

  public static native int getPropertyInt(JavaScriptObject obj, String p) /*-{
    return obj[p] || 0;
  }-*/;

  public static native boolean hasProperty(JavaScriptObject obj, String p) /*-{
    var ok;

    try {
      if (p in obj) {
        var tp = typeof (obj[p]);
        ok = (tp != "function" && tp != "undefined");
      } else {
        ok = false;
      }
    } catch (err) {
      ok = false;
    }
    return ok;
  }-*/;

  public static native void insert(JsArrayString arr, int index, String value) /*-{
    arr.splice(index, 0, value);
  }-*/;

  public static native boolean isEmpty(JavaScriptObject obj) /*-{
    if (obj == undefined || obj == null) {
      return true;
    } else if (obj instanceof Array) {
      return obj.length == 0;
    } else {
      return String(obj).length == 0;
    }
  }-*/;

  public static native boolean isEmpty(String s) /*-{
    if (s == null || s == "") {
      return true;
    }
    return s.match(/\S/) == null;
  }-*/;

  public static native boolean isFunction(JavaScriptObject obj, String p) /*-{
    var ok;

    try {
      ok = (typeof (obj[p]) == "function");
    } catch (err) {
      ok = false;
    }
    return ok;
  }-*/;

  public static native boolean isIn(String p, JavaScriptObject obj) /*-{
    return p in obj;
  }-*/;

  public static native String randomName(String pfx) /*-{
    if (pfx == null) {
      return String(Math.random()).substr(2);
    }
    return pfx + String(Math.random()).substr(2);
  }-*/;

  public static native void remove(JsArrayString arr, int index) /*-{
    arr.splice(index, 0);
  }-*/;

  public static native void setProperty(JavaScriptObject obj, String p, boolean value) /*-{
    obj[p] = value;
  }-*/;

  public static native void setProperty(JavaScriptObject obj, String p, double value) /*-{
    obj[p] = value;
  }-*/;

  public static native void setProperty(JavaScriptObject obj, String p, JsDate value) /*-{
    obj[p] = value;
  }-*/;

  public static native void setProperty(JavaScriptObject obj, String p, String value) /*-{
    obj[p] = value;
  }-*/;

  public static native void setProperty(JavaScriptObject obj, String p, JavaScriptObject value) /*-{
    obj[p] = value;
  }-*/;

  public static native void setPropertyToNull(JavaScriptObject obj, String p) /*-{
    obj[p] = null;
  }-*/;
  
  public static native void showBrowserNotification(String title, String msg) /*-{
    if (Notification.permission !== "granted") {
      Notification.requestPermission();
      return;
    }
    
    if (typeof $wnd.BeeNotification !== "undefined") {
      $wnd.BeeNotification.close();
    }

    $wnd.BeeNotification = new Notification(title, {
      icon : 'images/sclogo.png',
      body : msg});
  }-*/;

  public static native JsArrayString slice(JsArrayString src, int start, int end) /*-{
    if (src == null) {
      return null;
    }
    return src.slice(start, end);
  }-*/;

  public static native JsArrayString split(String src, String sep) /*-{
    if (src == null) {
      return null;
    }
    return src.split(sep);
  }-*/;

  public static native double toDouble(int from) /*-{
    return from;
  }-*/;

  public static native int toInt(double from) /*-{
    return from;
  }-*/;

  public static native String toSeconds(int millis) /*-{
    if (millis >= 0) {
      var z = millis / 1000;
      return z.toFixed(3);
    }
    return "";
  }-*/;

  public static native String toString(JavaScriptObject obj) /*-{
    if (obj) {
      return JSON.stringify(obj);
    } else {
      return null;
    }
  }-*/;

  public static native String toTime(double millis) /*-{
    if (millis > 0) {
      var d = new Date(millis);
      return d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds() + "." + (d.getMilliseconds() + 1000).toString().substr(1);
    }
    return "";
  }-*/;
//@formatter:on

  private JsUtils() {
  }
}
