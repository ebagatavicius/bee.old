package com.butent.bee.shared;

import com.butent.bee.shared.exceptions.ArgumentCountException;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Map;

/**
 * Implements various assertions for given objects, for example {@code isNull, isEven, isTrue} etc.
 */

public final class Assert {

  public static final String ASSERTION_FAILED = "[Assertion failed] - ";
  public static final String IS_EMPTY = ASSERTION_FAILED + "argument must not be null or empty";

  public static int betweenExclusive(int x, int min, int max) {
    if (!BeeUtils.betweenExclusive(x, min, max)) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "argument " + x
          + " must be >= " + min + " and < " + max);
    }
    return x;
  }

  public static int betweenExclusive(int x, int min, int max, String msg) {
    if (!BeeUtils.betweenExclusive(x, min, max)) {
      throw new BeeRuntimeException(msg);
    }
    return x;
  }

  public static int betweenInclusive(int x, int min, int max) {
    if (!BeeUtils.betweenInclusive(x, min, max)) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "argument " + x
          + " must be >= " + min + " and <= " + max);
    }
    return x;
  }

  public static int betweenInclusive(int x, int min, int max, String msg) {
    if (!BeeUtils.betweenInclusive(x, min, max)) {
      throw new BeeRuntimeException(msg);
    }
    return x;
  }

  public static <T> T contains(Map<T, ?> map, T key) {
    notNull(map);
    notNull(key);
    if (!map.containsKey(key)) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "key (" + key + ") not found");
    }
    return key;
  }

  public static int isEven(int x) {
    return isEven(x, ASSERTION_FAILED + "(" + x + ") argument must even");
  }

  public static int isEven(int x, String msg) {
    if (x % 2 == 1) {
      throw new BeeRuntimeException(msg);
    }
    return x;
  }

  public static void isFalse(boolean expression) {
    isFalse(expression, ASSERTION_FAILED + "this expression must be false");
  }

  public static void isFalse(boolean expression, String message) {
    if (expression) {
      throw new BeeRuntimeException(message);
    }
  }

  public static int isIndex(Collection<?> col, int idx) {
    notNull(col);
    nonNegative(idx);

    int n = col.size();

    if (n <= 0) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "index " + idx + ", collection empty");
    } else if (idx >= n) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "index " + idx + " must be < " + n);
    }
    return idx;
  }

  public static int isIndex(Collection<?> col, int idx, String msg) {
    notNull(col);
    if (idx < 0 || idx >= col.size()) {
      throw new BeeRuntimeException(msg);
    }
    return idx;
  }

  public static int isIndex(int idx, int size) {
    nonNegative(idx);

    if (size <= 0) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "index " + idx
          + " references empty object");
    } else if (idx >= size) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "index " + idx + " must be < " + size);
    }
    return idx;
  }

  public static int isIndex(int idx, int size, String msg) {
    if (idx < 0 || idx >= size) {
      throw new BeeRuntimeException(msg);
    }
    return idx;
  }

  public static <T> T isNull(T object) {
    return isNull(object, ASSERTION_FAILED + "the object argument must be null");
  }

  public static <T> T isNull(T object, String message) {
    if (object != null) {
      throw new BeeRuntimeException(message);
    }
    return object;
  }

  public static int isOdd(int x) {
    return isOdd(x, ASSERTION_FAILED + "(" + x + ") argument must odd");
  }

  public static int isOdd(int x, String msg) {
    if (x % 2 == 0) {
      throw new BeeRuntimeException(msg);
    }
    return x;
  }

  public static double isPositive(double x) {
    return isPositive(x, ASSERTION_FAILED + "(" + x + ") argument must be positive");
  }

  public static double isPositive(double x, String msg) {
    if (x <= 0) {
      throw new BeeRuntimeException(msg);
    }
    return x;
  }

  public static int isPositive(int x) {
    return isPositive(x, ASSERTION_FAILED + "(" + x + ") argument must be positive");
  }

  public static int isPositive(int x, String msg) {
    if (x <= 0) {
      throw new BeeRuntimeException(msg);
    }
    return x;
  }

  public static int isScale(int x) {
    return nonNegative(x, ASSERTION_FAILED + "(" + x + ") scale must be >= 0 and <= "
        + BeeConst.MAX_SCALE);
  }

  public static int isScale(int x, String msg) {
    if (x < 0 || x > BeeConst.MAX_SCALE) {
      throw new BeeRuntimeException(msg);
    }
    return x;
  }

  public static void isTrue(boolean expression) {
    isTrue(expression, ASSERTION_FAILED + "this expression must be true");
  }

  public static void isTrue(boolean expression, String message) {
    if (!expression) {
      throw new BeeRuntimeException(message);
    }
  }

  public static String[] lengthEquals(String[] arr, int size) {
    notNull(arr);
    int len = arr.length;

    if (size >= 0 && len != size) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "length " + len
          + " must be equal to " + size);
    }
    return arr;
  }

  public static void lengthInclusive(int len, int min, int max) {
    if (min > 0 && len < min) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "length " + len + " must be >= " + min);
    }
    if (max > 0 && len > max) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "length " + len + " must be <= " + max);
    }
  }

  public static void maxLength(int len, int max) {
    lengthInclusive(len, -1, max);
  }

  public static void minLength(int len, int min) {
    lengthInclusive(len, min, -1);
  }

  public static int nonNegative(int x) {
    return nonNegative(x, ASSERTION_FAILED + "(" + x + ") argument must be non-negative");
  }

  public static int nonNegative(int x, String msg) {
    if (x < 0) {
      throw new BeeRuntimeException(msg);
    }
    return x;
  }

  public static void noNullElements(String message, Object... obj) {
    if (obj == null) {
      throw new BeeRuntimeException(message);
    }
    for (int i = 0; i < obj.length; i++) {
      if (obj[i] == null) {
        throw new BeeRuntimeException(BeeUtils.joinWords(message, BeeUtils.bracket(i)));
      }
    }
  }

  public static void noNulls(Object... obj) {
    noNullElements(ASSERTION_FAILED + "arguments must not be null", obj);
  }

  public static <T> T notContain(Map<T, ?> map, T key) {
    notNull(map);
    notNull(key);
    if (map.containsKey(key)) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "key (" + key + ") already exists");
    }
    return key;
  }

  public static <T extends Collection<?>> T notEmpty(T col) {
    return notEmpty(col, IS_EMPTY);
  }

  public static <T extends Map<?, ?>> T notEmpty(T map) {
    return notEmpty(map, IS_EMPTY);
  }

  public static String notEmpty(String s) {
    return notEmpty(s, IS_EMPTY);
  }

  public static <T extends Collection<?>> T notEmpty(T col, String message) {
    if (BeeUtils.isEmpty(col)) {
      throw new BeeRuntimeException(message);
    }
    return col;
  }

  public static <T extends Map<?, ?>> T notEmpty(T map, String message) {
    if (BeeUtils.isEmpty(map)) {
      throw new BeeRuntimeException(message);
    }
    return map;
  }

  public static String notEmpty(String s, String message) {
    if (BeeUtils.isEmpty(s)) {
      throw new BeeRuntimeException(message);
    }
    return s;
  }

  public static void notImplemented() {
    notImplemented("Not implemented");
  }

  public static void notImplemented(String message) {
    throw new BeeRuntimeException(message);
  }

  public static <T> T notNull(T object) {
    return notNull(object, ASSERTION_FAILED + "this argument is required; it must not be null");
  }

  public static <T> T notNull(T object, String message) {
    if (object == null) {
      throw new BeeRuntimeException(message);
    }
    return object;
  }

  public static int parameterCount(int c, int min) {
    return parameterCount(c, min, -1);
  }

  public static int parameterCount(int c, int min, int max) {
    if (min > 0 && c < min || max > 0 && c > max) {
      throw new ArgumentCountException(c, min, max);
    }
    return c;
  }

  public static void state(boolean expression) {
    state(expression, ASSERTION_FAILED + "this state invariant must be true");
  }

  public static void state(boolean expression, String message) {
    if (!expression) {
      throw new BeeRuntimeException(message);
    }
  }

  public static void unsupported() {
    unsupported("unsupported operation");
  }

  public static void unsupported(String message) {
    throw new BeeRuntimeException(message);
  }

  public static void untouchable() {
    untouchable("can't touch this");
  }

  public static void untouchable(String message) {
    throw new BeeRuntimeException(message);
  }

  private Assert() {
  }
}
