package com.butent.bee.server.utils;

import com.google.common.primitives.Primitives;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.PropertyUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Contains a set of functions, needed to get information of a given class (get type, fields,
 * methods and so on).
 */

public final class ClassUtils {

  private static BeeLogger logger = LogUtils.getLogger(ClassUtils.class);

  @SuppressWarnings("unchecked")
  public static <T extends Annotation> T findAnnotation(Annotation[] arr, Class<T> search) {
    for (Annotation ann : arr) {
      if (isAssignable(ann.getClass(), search)) {
        return (T) ann;
      }
    }
    return null;
  }

  public static Annotation[] getAnnotations(Class<?> cls) {
    Assert.notNull(cls);
    return cls.getAnnotations();
  }

  public static Class<?>[] getClasses(Class<?> cls) {
    Assert.notNull(cls);
    return cls.getClasses();
  }

  public static <T> List<ExtendedProperty> getClassInfo(Class<T> cls) {
    Assert.notNull(cls);

    List<ExtendedProperty> lst = new ArrayList<>();

    PropertyUtils.addProperties(lst, false,
        "Location", getLocation(cls),
        "Package", transformPackage(cls.getPackage()),
        "Name", cls.getName(),
        "Simple Name", cls.getSimpleName(),
        "Canonical Name", cls.getCanonicalName(),
        "Component Type", transformClass(cls.getComponentType()),
        "Declaring Class", transformClass(cls.getDeclaringClass()),
        "Enclosing Class", transformClass(cls.getEnclosingClass()),
        "Enclosing Constructor", transformConstructor(getEnclosingConstructor(cls)),
        "Enclosing Method", transformMethod(getEnclosingMethod(cls)),
        "Superclass", transformClass(cls.getSuperclass()),
        "Generic Superclass", cls.getGenericSuperclass());

    PropertyUtils.addExtended(lst,
        "Modifiers", cls.getModifiers(), Modifier.toString(cls.getModifiers()));

    PropertyUtils.addProperties(lst, false,
        "Is Annotation", transformBoolean(cls.isAnnotation()),
        "Is Anonymous Class", transformBoolean(cls.isAnonymousClass()),
        "Is Array", transformBoolean(cls.isArray()),
        "Is Enum", transformBoolean(cls.isEnum()),
        "Is Interface", transformBoolean(cls.isInterface()),
        "Is Local Class", transformBoolean(cls.isLocalClass()),
        "Is Member Class", transformBoolean(cls.isMemberClass()),
        "Is Primitive", transformBoolean(cls.isPrimitive()),
        "Is Synthetic", transformBoolean(cls.isSynthetic()),
        "Class Loader", cls.getClassLoader(),
        "Desired Assertion Status", transformBoolean(cls.desiredAssertionStatus()));

    Annotation[] annArr = getDeclaredAnnotations(cls);
    if (annArr != null) {
      for (Annotation ann : annArr) {
        PropertyUtils.addExtended(lst, "Declared Annotation", transformAnnotation(ann));
      }
    }

    annArr = getAnnotations(cls);
    if (annArr != null) {
      for (Annotation ann : annArr) {
        PropertyUtils.addExtended(lst, "Annotation", transformAnnotation(ann));
      }
    }

    Class<?>[] clArr = getDeclaredClasses(cls);
    if (clArr != null) {
      for (Class<?> z : clArr) {
        PropertyUtils.addExtended(lst, "Declared Class", transformClass(z));
      }
    }

    clArr = getClasses(cls);
    if (clArr != null) {
      for (Class<?> z : clArr) {
        PropertyUtils.addExtended(lst, "Class", transformClass(z));
      }
    }

    Constructor<?>[] constrArr = getDeclaredConstructors(cls);
    if (constrArr != null) {
      for (Constructor<?> constr : constrArr) {
        PropertyUtils.addExtended(lst, "Declared Constructor", transformConstructor(constr));
      }
    }

    constrArr = getConstructors(cls);
    if (constrArr != null) {
      for (Constructor<?> constr : constrArr) {
        PropertyUtils.addExtended(lst, "Constructor", transformConstructor(constr));
      }
    }

    Method[] methArr = getDeclaredMethods(cls);
    if (methArr != null) {
      for (Method meth : methArr) {
        PropertyUtils.addExtended(lst, "Declared Method", transformMethod(meth));
      }
    }

    methArr = getMethods(cls);
    if (methArr != null) {
      for (Method meth : methArr) {
        PropertyUtils.addExtended(lst, "Method", transformMethod(meth));
      }
    }

    Field[] fldArr = getDeclaredFields(cls);
    if (fldArr != null) {
      for (Field fld : fldArr) {
        PropertyUtils.addExtended(lst, "Declared Field", transformField(fld));
      }
    }

    fldArr = getFields(cls);
    if (fldArr != null) {
      for (Field fld : fldArr) {
        PropertyUtils.addExtended(lst, "Field", transformField(fld));
      }
    }

    T[] enumArr = getEnumConstants(cls);
    if (enumArr != null) {
      for (T en : enumArr) {
        PropertyUtils.addExtended(lst, "Enum Constant", en);
      }
    }

    Type[] tpArr = getGenericInterfaces(cls);
    if (tpArr != null) {
      for (Type tp : tpArr) {
        PropertyUtils.addExtended(lst, "Generic Interface", tp);
      }
    }

    clArr = getInterfaces(cls);
    if (clArr != null) {
      for (Class<?> z : clArr) {
        PropertyUtils.addExtended(lst, "Interface", transformClass(z));
      }
    }

    Object[] objArr = getSigners(cls);
    if (objArr != null) {
      for (Object obj : objArr) {
        PropertyUtils.addExtended(lst, "Signer", obj);
      }
    }

    TypeVariable<Class<T>>[] tpParArr = getTypeParameters(cls);
    if (tpParArr != null) {
      for (TypeVariable<Class<T>> tpPar : tpParArr) {
        PropertyUtils.addExtended(lst, "Type Parameter", tpPar.getGenericDeclaration(),
            tpPar.getName());
      }
    }
    return lst;
  }

  public static Constructor<?>[] getConstructors(Class<?> cls) {
    Assert.notNull(cls);

    Constructor<?>[] constructors;
    try {
      constructors = cls.getConstructors();
    } catch (NoClassDefFoundError err) {
      logger.warning(err);
      constructors = null;
    }

    return constructors;
  }

  public static Annotation[] getDeclaredAnnotations(Class<?> cls) {
    Assert.notNull(cls);
    return cls.getDeclaredAnnotations();
  }

  public static Class<?>[] getDeclaredClasses(Class<?> cls) {
    Assert.notNull(cls);
    return cls.getDeclaredClasses();
  }

  public static Constructor<?>[] getDeclaredConstructors(Class<?> cls) {
    Assert.notNull(cls);

    Constructor<?>[] constructors;
    try {
      constructors = cls.getDeclaredConstructors();
    } catch (NoClassDefFoundError err) {
      logger.warning(err);
      constructors = null;
    }

    return constructors;
  }

  public static Field[] getDeclaredFields(Class<?> cls) {
    Assert.notNull(cls);

    Field[] fields;
    try {
      fields = cls.getDeclaredFields();
    } catch (NoClassDefFoundError err) {
      logger.warning(err);
      fields = null;
    }

    return fields;
  }

  public static Method[] getDeclaredMethods(Class<?> cls) {
    Assert.notNull(cls);

    Method[] methods;
    try {
      methods = cls.getDeclaredMethods();
    } catch (NoClassDefFoundError err) {
      logger.warning(err);
      methods = null;
    }

    return methods;
  }

  public static Constructor<?> getEnclosingConstructor(Class<?> cls) {
    Assert.notNull(cls);

    Constructor<?> constructor;
    try {
      constructor = cls.getEnclosingConstructor();
    } catch (SecurityException err) {
      logger.warning(err);
      constructor = null;
    }

    return constructor;
  }

  public static Method getEnclosingMethod(Class<?> cls) {
    Assert.notNull(cls);

    Method method;
    try {
      method = cls.getEnclosingMethod();
    } catch (SecurityException err) {
      logger.warning(err);
      method = null;
    }

    return method;
  }

  public static <T> T[] getEnumConstants(Class<T> cls) {
    Assert.notNull(cls);
    return cls.getEnumConstants();
  }

  public static Field[] getFields(Class<?> cls) {
    Assert.notNull(cls);
    Field[] fields;

    try {
      fields = cls.getFields();
    } catch (NoClassDefFoundError err) {
      logger.warning(err);
      fields = null;
    }

    return fields;
  }

  public static Type[] getGenericInterfaces(Class<?> cls) {
    Assert.notNull(cls);
    return cls.getGenericInterfaces();
  }

  public static Class<?>[] getInterfaces(Class<?> cls) {
    Assert.notNull(cls);
    return cls.getInterfaces();
  }

  public static String getLocation(Class<?> cls) {
    Assert.notNull(cls);

    ProtectionDomain protectionDomain = cls.getProtectionDomain();
    CodeSource codeSource = (protectionDomain == null) ? null : protectionDomain.getCodeSource();
    URL location = (codeSource == null) ? null : codeSource.getLocation();

    return (location == null) ? null : location.toString();

  }

  public static Method[] getMethods(Class<?> cls) {
    Assert.notNull(cls);

    Method[] methods;
    try {
      methods = cls.getMethods();
    } catch (NoClassDefFoundError err) {
      logger.warning(err);
      methods = null;
    }

    return methods;
  }

  public static Object[] getSigners(Class<?> cls) {
    Assert.notNull(cls);
    return cls.getSigners();
  }

  public static <T> TypeVariable<Class<T>>[] getTypeParameters(Class<T> cls) {
    Assert.notNull(cls);
    return cls.getTypeParameters();
  }

  public static boolean isAssignable(Class<?> from, Class<?> to) {
    if (from == null) {
      return false;
    }
    if (to == null) {
      return false;
    }
    if (from.equals(to)) {
      return true;
    }

    if (from.isPrimitive() && !to.isPrimitive()) {
      return isAssignable(Primitives.wrap(from), to);
    }
    if (Primitives.isWrapperType(from) && to.isPrimitive()) {
      return isAssignable(Primitives.unwrap(from), to);
    }

    if (from.isPrimitive()) {
      if (!to.isPrimitive()) {
        return false;
      }
      if (Integer.TYPE.equals(from)) {
        return Long.TYPE.equals(to) || Float.TYPE.equals(to) || Double.TYPE.equals(to);
      }
      if (Long.TYPE.equals(from)) {
        return Float.TYPE.equals(to) || Double.TYPE.equals(to);
      }
      if (Boolean.TYPE.equals(from)) {
        return false;
      }
      if (Double.TYPE.equals(from)) {
        return false;
      }
      if (Float.TYPE.equals(from)) {
        return Double.TYPE.equals(to);
      }
      if (Character.TYPE.equals(from)) {
        return Integer.TYPE.equals(to) || Long.TYPE.equals(to) || Float.TYPE.equals(to)
            || Double.TYPE.equals(to);
      }
      if (Short.TYPE.equals(from)) {
        return Integer.TYPE.equals(to) || Long.TYPE.equals(to) || Float.TYPE.equals(to)
            || Double.TYPE.equals(to);
      }
      if (Byte.TYPE.equals(from)) {
        return Short.TYPE.equals(to) || Integer.TYPE.equals(to) || Long.TYPE.equals(to)
            || Float.TYPE.equals(to) || Double.TYPE.equals(to);
      }
      if (Void.TYPE.equals(from)) {
        return false;
      }
      Assert.untouchable();
      return false;
    }
    return to.isAssignableFrom(from);
  }

  public static boolean isBoolean(Class<?> clazz) {
    return Boolean.class.equals(clazz) || Boolean.TYPE.equals(clazz);
  }

  public static boolean isByte(Class<?> clazz) {
    return Byte.class.equals(clazz) || Byte.TYPE.equals(clazz);
  }

  public static boolean isCharacter(Class<?> clazz) {
    return Character.class.equals(clazz) || Character.TYPE.equals(clazz);
  }

  public static boolean isDouble(Class<?> clazz) {
    return Double.class.equals(clazz) || Double.TYPE.equals(clazz);
  }

  public static boolean isFloat(Class<?> clazz) {
    return Float.class.equals(clazz) || Float.TYPE.equals(clazz);
  }

  public static boolean isInteger(Class<?> clazz) {
    return Integer.class.equals(clazz) || Integer.TYPE.equals(clazz);
  }

  public static boolean isLong(Class<?> clazz) {
    return Long.class.equals(clazz) || Long.TYPE.equals(clazz);
  }

  public static boolean isMap(Class<?> clazz) {
    return isAssignable(clazz, Map.class);
  }

  public static boolean isShort(Class<?> clazz) {
    return Short.class.equals(clazz) || Short.TYPE.equals(clazz);
  }

  public static boolean isStringArray(Class<?> clazz) {
    if (clazz == null) {
      return false;
    }
    return clazz.isArray() && String.class.equals(clazz.getComponentType());
  }

  public static String transformAnnotation(Annotation ann) {
    if (ann == null) {
      return null;
    } else {
      return ann.toString();
    }
  }

  public static String transformBoolean(boolean b) {
    return b ? BeeConst.STRING_TRUE : null;
  }

  private static String transformClass(Class<?> cls) {
    if (cls == null) {
      return null;
    } else {
      return cls.getName();
    }
  }

  private static String transformConstructor(Constructor<?> constr) {
    if (constr == null) {
      return null;
    } else {
      return constr.toGenericString();
    }
  }

  private static String transformField(Field fld) {
    if (fld == null) {
      return null;
    } else {
      String value = null;

      if (java.lang.reflect.Modifier.isStatic(fld.getModifiers())) {
        fld.setAccessible(true);

        try {
          value = "=" + fld.get(null);
        } catch (IllegalArgumentException | IllegalAccessException e) {
          value = "";
        }
      }
      return fld.toGenericString() + value;
    }
  }

  private static String transformMethod(Method meth) {
    if (meth == null) {
      return null;
    } else {
      return meth.toGenericString();
    }
  }

  private static String transformPackage(Package pck) {
    if (pck == null) {
      return null;
    } else {
      return pck.getName();
    }
  }

  private ClassUtils() {
  }
}
