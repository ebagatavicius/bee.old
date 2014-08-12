package com.butent.bee.server.utils;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.Wildcards;
import com.butent.bee.shared.utils.Wildcards.Pattern;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * Finds out whether a particular class is loaded in Java Virtual Machine.
 */

public final class JvmUtils {

  private static Throwable cvfFailure;
  private static final Field CLASSES_VECTOR_FIELD;

  private static final Map<String, Class<?>> PRIMITIVES;

  static {
    PRIMITIVES = new HashMap<>(9);

    PRIMITIVES.put("boolean", boolean.class);
    PRIMITIVES.put("char", char.class);
    PRIMITIVES.put("byte", byte.class);
    PRIMITIVES.put("short", short.class);
    PRIMITIVES.put("int", int.class);
    PRIMITIVES.put("long", long.class);
    PRIMITIVES.put("float", float.class);
    PRIMITIVES.put("double", double.class);
    PRIMITIVES.put("void", void.class);

    Throwable err = null;
    Field fld = null;

    try {
      fld = ClassLoader.class.getDeclaredField("classes");
      if (fld.getType() == Vector.class) {
        fld.setAccessible(true);
      } else {
        err = new BeeException("Classloader.classes not of type java.util.Vector: "
            + fld.getType().getName());
      }
    } catch (Throwable t) {
      err = t;
      fld = null;
    }

    cvfFailure = err;
    CLASSES_VECTOR_FIELD = fld;
  }

  public static Set<Class<?>> findClass(String name) {
    Assert.notEmpty(name);
    String nm = name.trim();

    Set<Class<?>> found = new HashSet<>();
    if (PRIMITIVES.containsKey(nm)) {
      found.add(PRIMITIVES.get(nm));

    } else if (Wildcards.hasDefaultWildcards(nm)) {
      Pattern pattern = Wildcards.getDefaultPattern(nm);

      Set<Class<?>> loaded = getAllLoadedClasses();
      for (Class<?> cls : loaded) {
        if (Wildcards.isLike(cls.getName(), pattern)) {
          found.add(cls);
        }
      }

    } else {
      Class<?> cls = forName(nm);
      if (cls != null) {
        found.add(cls);
      }
    }

    return found;
  }

  public static Throwable getCvfFailure() {
    return cvfFailure;
  }

  public static List<Property> getLoadedClasses() {
    List<Property> lst = new ArrayList<>();

    Set<ClassLoader> loaders = getClassLoaders();
    if (BeeUtils.isEmpty(loaders)) {
      return lst;
    }

    Class<?>[] classes;
    String[] names;

    int lc = loaders.size();
    int li = 0;
    int cc;

    for (ClassLoader loader : loaders) {
      li++;
      if (loader == null) {
        continue;
      }

      classes = getClasses(loader);
      if (classes == null) {
        cc = 0;
      } else {
        cc = classes.length;
      }

      lst.add(new Property(loader.toString(), BeeUtils.joinWords(BeeUtils.progress(li, lc),
          BeeUtils.bracket(cc))));

      if (cc <= 0) {
        continue;
      }

      names = new String[cc];
      for (int i = 0; i < cc; i++) {
        names[i] = classes[i].getName();
      }

      Arrays.sort(names);

      for (int i = 0; i < cc; i++) {
        lst.add(new Property(BeeUtils.progress(i + 1, cc), names[i]));
      }
    }

    return lst;
  }

  private static void addClassLoaders(Set<ClassLoader> classLoaders, ClassLoader loader) {
    if (classLoaders == null || loader == null) {
      return;
    }

    for (ClassLoader cl = loader; cl != null; cl = cl.getParent()) {
      if (classLoaders.contains(cl)) {
        break;
      }
      classLoaders.add(cl);
    }
  }

  private static Class<?> forName(String name) {
    Class<?> cls;

    try {
      cls = Class.forName(name);
    } catch (ClassNotFoundException ex) {
      cls = null;
    }
    return cls;
  }

  private static Set<Class<?>> getAllLoadedClasses() {
    Set<Class<?>> result = new HashSet<>();

    Set<ClassLoader> loaders = getClassLoaders();
    if (BeeUtils.isEmpty(loaders)) {
      return result;
    }

    Class<?>[] classes;

    for (ClassLoader loader : loaders) {
      if (loader == null) {
        continue;
      }

      classes = getClasses(loader);
      if (classes == null) {
        continue;
      }

      for (Class<?> cls : classes) {
        result.add(cls);
      }
    }
    return result;
  }

  private static Class<?>[] getClasses(ClassLoader loader) {
    if (loader == null) {
      return null;
    }

    try {
      @SuppressWarnings("unchecked")
      Vector<Class<?>> classes = (Vector<Class<?>>) CLASSES_VECTOR_FIELD.get(loader);
      if (classes == null) {
        return null;
      }

      Class<?>[] arr;

      synchronized (classes) {
        arr = new Class<?>[classes.size()];
        classes.toArray(arr);
      }

      return arr;
    } catch (IllegalAccessException ex) {
      cvfFailure = ex;
      return null;
    }
  }

  private static Set<ClassLoader> getClassLoaders() {
    Set<ClassLoader> classLoaders = new HashSet<>();

    addClassLoaders(classLoaders, int.class.getClassLoader());
    addClassLoaders(classLoaders, ClassLoader.getSystemClassLoader());
    addClassLoaders(classLoaders, Thread.currentThread().getContextClassLoader());

    return classLoaders;
  }

  private JvmUtils() {
  }
}
