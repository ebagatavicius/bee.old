package com.butent.bee.server.i18n;

import com.google.gwt.i18n.client.Constants.DefaultBooleanValue;
import com.google.gwt.i18n.client.Constants.DefaultDoubleValue;
import com.google.gwt.i18n.client.Constants.DefaultFloatValue;
import com.google.gwt.i18n.client.Constants.DefaultIntValue;
import com.google.gwt.i18n.client.Constants.DefaultStringArrayValue;
import com.google.gwt.i18n.client.Constants.DefaultStringMapValue;
import com.google.gwt.i18n.client.Constants.DefaultStringValue;
import com.google.gwt.i18n.client.LocalizableResource.Key;

import com.butent.bee.server.utils.ClassUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Manages localization related values of system parameters.
 */

public class GwtConstants extends GwtLocalizable {

  public GwtConstants(Properties properties) {
    super(properties);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Key key = method.getAnnotation(Key.class);
    Object value = null;
    if (key != null) {
      value = getConstant(key.value(), method);
    }
    if (value == null) {
      value = getConstant(method.getName(), method);
    }
    return value;
  }

  private Object getConstant(String propertyName, Method method) {
    String str = getProperty(propertyName);
    if (str == null) {
      return getDefaultValue(method);
    } else {
      return getValue(str, method.getReturnType());
    }
  }

  private static Object getDefaultValue(Method method) {
    Class<?> type = method.getReturnType();
    if (type.equals(String.class)) {
      DefaultStringValue ann = method.getAnnotation(DefaultStringValue.class);
      if (ann != null) {
        return ann.value();
      }

    } else if (ClassUtils.isInteger(type)) {
      DefaultIntValue ann = method.getAnnotation(DefaultIntValue.class);
      if (ann != null) {
        return ann.value();
      }

    } else if (ClassUtils.isFloat(type)) {
      DefaultFloatValue ann = method.getAnnotation(DefaultFloatValue.class);
      if (ann != null) {
        return ann.value();
      }

    } else if (ClassUtils.isDouble(type)) {
      DefaultDoubleValue ann = method.getAnnotation(DefaultDoubleValue.class);
      if (ann != null) {
        return ann.value();
      }

    } else if (ClassUtils.isBoolean(type)) {
      DefaultBooleanValue ann = method.getAnnotation(DefaultBooleanValue.class);
      if (ann != null) {
        return ann.value();
      }

    } else if (ClassUtils.isMap(type)) {
      DefaultStringMapValue ann = method.getAnnotation(DefaultStringMapValue.class);
      if (ann != null) {
        Map<String, String> result = new HashMap<>();
        String[] arr = ann.value();
        for (int i = 0; i < arr.length - 1; i += 2) {
          result.put(arr[i], arr[i + 1]);
        }
        return result;
      }

    } else if (ClassUtils.isStringArray(type)) {
      DefaultStringArrayValue ann = method.getAnnotation(DefaultStringArrayValue.class);
      if (ann != null) {
        return ann.value();
      }
    }
    return null;
  }

  private Object getValue(String str, Class<?> type) {
    if (type.equals(String.class)) {
      return str;
    }

    if (ClassUtils.isInteger(type)) {
      return Integer.valueOf(str);
    }
    if (ClassUtils.isFloat(type)) {
      return Float.valueOf(str);
    }
    if (ClassUtils.isDouble(type)) {
      return Double.valueOf(str);
    }
    if (ClassUtils.isBoolean(type)) {
      return Boolean.valueOf(str);
    }

    if (ClassUtils.isMap(type)) {
      Map<String, String> result = new HashMap<>();
      String[] arr = BeeUtils.split(str, BeeConst.CHAR_COMMA);
      for (String key : arr) {
        String value = getProperty(key);
        if (value != null) {
          result.put(key, value);
        }
      }
      return result;
    }
    if (ClassUtils.isStringArray(type)) {
      return BeeUtils.split(str, BeeConst.CHAR_COMMA);
    }

    Assert.untouchable(BeeUtils.joinWords("type", type, "not supported"));
    return null;
  }
}
