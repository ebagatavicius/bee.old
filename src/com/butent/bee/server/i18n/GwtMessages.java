package com.butent.bee.server.i18n;

import com.google.gwt.i18n.client.LocalizableResource.Key;
import com.google.gwt.i18n.client.Messages.AlternateMessage;
import com.google.gwt.i18n.client.Messages.DefaultMessage;
import com.google.gwt.i18n.client.Messages.PluralCount;

import com.butent.bee.server.utils.ClassUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Dynamically generates user interface messages depending on language set in the system.
 */

public class GwtMessages extends GwtLocalizable {

  public GwtMessages(Properties properties) {
    super(properties);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Assert.isTrue(String.class.equals(method.getReturnType()));

    Key key = method.getAnnotation(Key.class);
    String result = null;
    if (key != null) {
      result = buildMessage(key.value(), method, args);
    }
    if (result == null) {
      result = buildMessage(method.getName(), method, args);
    }
    return result;
  }

  private String buildMessage(String propertyName, Method method, Object[] args) {
    AlternateMessage altAnnotation = method.getAnnotation(AlternateMessage.class);
    Annotation[][] paramsAnnotations = method.getParameterAnnotations();
    Class<?>[] paramTypes = method.getParameterTypes();

    Map<String, String> alternates = new HashMap<>();
    if (altAnnotation != null) {
      String[] pairs = altAnnotation.value();
      for (int i = 0; (i + 1) < pairs.length; i += 2) {
        alternates.put(pairs[i], pairs[i + 1]);
      }
    }

    String pluralKey = BeeConst.STRING_EMPTY;
    for (int i = 0; i < paramTypes.length; i++) {
      PluralCount pc = ClassUtils.findAnnotation(paramsAnnotations[i], PluralCount.class);
      if (pc == null) {
        continue;
      }

      if (!ClassUtils.isAssignable(paramTypes[i], long.class)) {
        continue;
      }
      long n = (Long) args[i];

      if (n == 0) {
        pluralKey += "[none]";
      } else if (n == 1) {
        pluralKey += "[one]";
      } else if (n > 1) {
        pluralKey += "[many]";
      }
    }

    String template = getProperty(propertyName + pluralKey);
    if (template == null) {
      DefaultMessage dm = method.getAnnotation(DefaultMessage.class);
      if (dm != null) {
        template = dm.value();
      }
      if (template == null) {
        return null;
      }
    }

    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        String search = "{" + i + "}";
        String value = (args[i] == null) ? BeeConst.NULL : args[i].toString();
        template = BeeUtils.replace(template, search, value);
      }
    }
    return template;
  }
}
