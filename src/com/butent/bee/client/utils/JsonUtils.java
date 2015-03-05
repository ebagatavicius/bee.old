package com.butent.bee.client.utils;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains methods for transforming data into and from {@code JSON} type data structure.
 */

public final class JsonUtils {

  public static Boolean getBoolean(JSONObject obj, String key) {
    if (obj == null || BeeUtils.isEmpty(key) || !obj.containsKey(key)) {
      return null;
    } else {
      JSONBoolean value = obj.get(key).isBoolean();
      return (value == null) ? null : value.booleanValue();
    }
  }

  public static Integer getInteger(JSONObject obj, String key) {
    Double value = getNumber(obj, key);
    if (value == null) {
      return BeeConst.UNDEF;
    } else {
      return BeeUtils.round(value);
    }
  }

  public static Double getNumber(JSONObject obj, String key) {
    if (obj == null || BeeUtils.isEmpty(key) || !obj.containsKey(key)) {
      return null;
    } else {
      JSONNumber value = obj.get(key).isNumber();
      return (value == null) ? null : value.doubleValue();
    }
  }

  public static String getString(JSONObject obj, String key) {
    if (obj == null || BeeUtils.isEmpty(key) || !obj.containsKey(key)) {
      return null;
    } else {
      JSONString value = obj.get(key).isString();
      return (value == null) ? null : value.stringValue();
    }
  }

  public static boolean isEmpty(JSONValue value) {
    if (value == null) {
      return true;
    }

    if (value.isBoolean() != null) {
      return !value.isBoolean().booleanValue();
    } else if (value.isNumber() != null) {
      return BeeUtils.isZero(value.isNumber().doubleValue());
    } else if (value.isString() != null) {
      return BeeUtils.isEmpty(value.isString().stringValue());
    } else if (value.isArray() != null) {
      return value.isArray().size() <= 0;
    } else if (value.isObject() != null) {
      return value.isObject().size() <= 0;
    } else {
      return true;
    }
  }

  public static boolean isJson(String s) {
    return BeeUtils.isPrefix(s, BeeConst.STRING_LEFT_BRACE)
        && BeeUtils.isSuffix(s, BeeConst.STRING_RIGHT_BRACE);
  }

  public static JSONObject parse(String s) {
    if (isJson(s)) {
      JSONValue value = JSONParser.parseStrict(s);
      return (value == null) ? null : value.isObject();

    } else {
      return null;
    }
  }

  public static List<String> toList(JSONValue json) {
    List<String> result = new ArrayList<>();
    if (json == null) {
      return result;
    }

    if (json.isArray() == null) {
      result.add(toString(json));
    } else {
      for (int i = 0; i < json.isArray().size(); i++) {
        result.add(toString(json.isArray().get(i)));
      }
    }

    return result;
  }

  public static String toString(JSONValue value) {
    if (value == null) {
      return null;
    }

    if (value.isString() != null) {
      return value.isString().stringValue();
    } else if (value.isBoolean() != null) {
      return BeeUtils.toString(value.isBoolean().booleanValue());
    } else if (value.isNumber() != null) {
      return BeeUtils.toString(value.isNumber().doubleValue());
    } else {
      return null;
    }
  }

  private JsonUtils() {
  }
}
