package com.butent.bee.server.utils;

import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Enables to invoke methods from a given name reference.
 */

public final class Reflection {

  private static BeeLogger logger = LogUtils.getLogger(Reflection.class);

  public static ResponseObject invoke(Object obj, String methodName, RequestInfo req) {
    Assert.notNull(obj);
    if (BeeUtils.isEmpty(methodName)) {
      return ResponseObject.error("method name not specified");
    }

    Method method = findMethod(obj.getClass(), methodName);
    if (method == null) {
      return ResponseObject.error("method", methodName, "not found");
    }

    if (!method.isAccessible()) {
      method.setAccessible(true);
    }

    return doMethod(obj, method, req);
  }

  private static ResponseObject doMethod(Object obj, Method method, RequestInfo req) {
    Class<?>[] parameterTypes = method.getParameterTypes();
    boolean hasReq = ArrayUtils.contains(parameterTypes, RequestInfo.class);

    ResponseObject response;

    try {
      Object result;
      if (hasReq) {
        result = method.invoke(obj, req);
      } else {
        result = method.invoke(obj);
      }

      if (result instanceof ResponseObject) {
        response = (ResponseObject) result;
      } else {
        response = ResponseObject.error(method, "returned",
            (result == null) ? BeeConst.NULL : NameUtils.getName(result));
      }

    } catch (IllegalArgumentException ex) {
      logger.error(ex, method);
      response = ResponseObject.error(method, ex);

    } catch (IllegalAccessException ex) {
      logger.error(ex, method);
      response = ResponseObject.error(method, ex);

    } catch (InvocationTargetException ex) {
      logger.error(ex, method);
      response = ResponseObject.error(method, ex);
    }

    return response;
  }

  private static Method findMethod(Class<?> cls, String name) {
    Method found = null;
    Method[] arr = cls.getDeclaredMethods();

    for (Method method : arr) {
      if (BeeUtils.same(method.getName(), name)) {
        found = method;
        break;
      }

      if (BeeUtils.containsSame(method.getName(), name)) {
        if (found == null) {
          found = method;
        } else {
          found = null;
        }
      }
    }
    return found;
  }

  private Reflection() {
  }
}
