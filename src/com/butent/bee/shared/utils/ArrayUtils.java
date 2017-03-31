package com.butent.bee.shared.utils;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Contains methods for processing arrays.
 */
public final class ArrayUtils {

  public static String[] addFirst(String s, String[] arr) {
    int len = length(arr);
    String[] result = new String[len + 1];

    result[0] = s;
    for (int i = 0; i < len; i++) {
      result[i + 1] = arr[i];
    }
    return result;
  }

  /**
   * Searches the specified collection for a value.
   * 
   * @param arr the array to search
   * @param value the value to be searched for
   * @return true if the value is found, false elsewise
   */
  public static <T> boolean contains(T[] arr, T value) {
    return indexOf(arr, value) >= 0;
  }

  public static boolean containsSame(String[] arr, String value) {
    if (arr == null) {
      return false;
    }

    for (String s : arr) {
      if (BeeUtils.same(s, value)) {
        return true;
      }
    }
    return false;
  }

  public static String[] copyOf(String[] original) {
    if (original == null) {
      return null;
    }
    String[] copy = new String[original.length];
    for (int i = 0; i < original.length; i++) {
      copy[i] = original[i];
    }
    return copy;
  }

  /**
   * Returns an object from specified index from an object array.
   * 
   * @param arr the array returned from
   * @param idx index of the array
   * @return the object from a specified index. If the array is not one of these types: Object,
   *         Boolean, Char, Byte, Short, Integer, Long, Float or Double, it returns null.
   */
  public static Object get(Object arr, int idx) {
    if (arr instanceof Object[]) {
      return ((Object[]) arr)[idx];
    } else if (arr instanceof boolean[]) {
      return ((boolean[]) arr)[idx];
    } else if (arr instanceof char[]) {
      return ((char[]) arr)[idx];
    } else if (arr instanceof byte[]) {
      return ((byte[]) arr)[idx];
    } else if (arr instanceof short[]) {
      return ((short[]) arr)[idx];
    } else if (arr instanceof int[]) {
      return ((int[]) arr)[idx];
    } else if (arr instanceof long[]) {
      return ((long[]) arr)[idx];
    } else if (arr instanceof float[]) {
      return ((float[]) arr)[idx];
    } else if (arr instanceof double[]) {
      return ((double[]) arr)[idx];
    } else {
      return null;
    }
  }

  /**
   * Returns an array element from the specified index.
   * 
   * @param arr the array returned from
   * @param idx index of the element to be returned
   * @return an array element, or null if index is out of bounds.
   */
  public static <T> T getQuietly(T[] arr, int idx) {
    if (isIndex(arr, idx)) {
      return arr[idx];
    } else {
      return null;
    }
  }

  /**
   * Checks if the value is found within the array.
   * 
   * @param arr array to be searched from
   * @param value value to search for
   * @return -1 if the value is not found, or the index of the found value.
   */
  public static <T> int indexOf(T[] arr, T value) {
    int idx = -1;
    int len = length(arr);
    if (len <= 0) {
      return idx;
    }

    for (int i = 0; i < len; i++) {
      if (Objects.equals(value, arr[i])) {
        idx = i;
        break;
      }
    }
    return idx;
  }

  /**
   * Checks if the specified object is an array.
   * 
   * @param obj object to be checked
   * @return true if object is an array, elsewise false.
   */
  public static boolean isArray(Object obj) {
    return obj instanceof Object[] || isPrimitiveArray(obj);
  }

  public static boolean isEmpty(Object[] arr) {
    return arr == null || arr.length == 0;
  }

  /**
   * Checks if a specified index is found in the object.
   * 
   * @param obj the object to check
   * @param idx index to check
   * @return true if the index in the object exists, if not false.
   */
  public static boolean isIndex(Object obj, int idx) {
    if (obj == null || idx < 0) {
      return false;
    } else {
      int n = length(obj);
      return n > 0 && idx < n;
    }
  }

  /**
   * Checks if the specified object is an instance of any primitive type.
   * 
   * @param obj object to check
   * @return true if the object is a primitive array, otherwise false.
   */
  public static boolean isPrimitiveArray(Object obj) {
    return obj instanceof boolean[] || obj instanceof char[]
        || obj instanceof byte[] || obj instanceof short[]
        || obj instanceof int[] || obj instanceof long[]
        || obj instanceof float[] || obj instanceof double[];
  }

  /**
   * Joins an array with the specified separator. Each array element is joined by the separator.
   * 
   * @param separator separator to join with
   * @param arr array to join
   * @return a new string which contains all array elements joined by the specified separator
   */
  public static String join(String separator, Object[] arr) {
    return join(separator, arr, -1, -1);
  }

  /**
   * Joins an array with the specified separator from the specified index. Each array element is
   * joined by the separator.
   * 
   * @param separator separator to join with
   * @param arr array to join
   * @param fromIndex the array index to start from
   * @return a new string which contains all array elements joined by the specified separator
   */
  public static String join(String separator, Object[] arr, int fromIndex) {
    return join(separator, arr, fromIndex, -1);
  }

  /**
   * Joins an array with the specified separator from the specified index to a specified to index.
   * Each array element is joined by the separator.
   * 
   * @param separator separator to join with
   * @param arr array to join
   * @param fromIndex the array index to start from
   * @param toIndex an array index to stop joining
   * @return a new string which contains all array elements joined by the specified separator
   */
  public static String join(String separator, Object[] arr, int fromIndex, int toIndex) {
    Assert.notNull(separator);
    int len = length(arr);
    int fr = (fromIndex > 0) ? fromIndex : 0;
    int to = (toIndex >= 0 && toIndex <= len) ? toIndex : len;

    if (fr >= to) {
      return BeeConst.STRING_EMPTY;
    }

    StringBuilder sb = new StringBuilder();
    for (int i = fr; i < to; i++) {
      String s = BeeUtils.transform(arr[i]);
      if (!s.isEmpty()) {
        if (sb.length() > 0) {
          sb.append(separator);
        }
        sb.append(s);
      }
    }
    return sb.toString();
  }

  public static String joinWords(Object[] arr) {
    return join(BeeConst.STRING_SPACE, arr);
  }

  public static String joinWords(String[] arr) {
    return join(BeeConst.STRING_SPACE, arr);
  }

  /**
   * Gets the length of the specified Object {@code arr}.
   * 
   * @param arr an array to check
   * @return the length of the object if it is one of these types: Object, Boolean, Char, Byte,
   *         Short, Integer, Long, Float, Double. 0 if its none of these types.
   */
  public static int length(Object arr) {
    int len;

    if (arr instanceof Object[]) {
      len = ((Object[]) arr).length;
    } else if (arr instanceof boolean[]) {
      len = ((boolean[]) arr).length;
    } else if (arr instanceof char[]) {
      len = ((char[]) arr).length;
    } else if (arr instanceof byte[]) {
      len = ((byte[]) arr).length;
    } else if (arr instanceof short[]) {
      len = ((short[]) arr).length;
    } else if (arr instanceof int[]) {
      len = ((int[]) arr).length;
    } else if (arr instanceof long[]) {
      len = ((long[]) arr).length;
    } else if (arr instanceof float[]) {
      len = ((float[]) arr).length;
    } else if (arr instanceof double[]) {
      len = ((double[]) arr).length;
    } else {
      len = 0;
    }
    return len;
  }

  public static String[] remove(String[] arr, int index) {
    if (arr == null || index < 0 || arr.length <= index) {
      return arr;
    }

    String[] result = new String[arr.length - 1];

    if (index > 0) {
      for (int i = 0; i < index; i++) {
        result[i] = arr[i];
      }
    }

    if (index < arr.length - 1) {
      for (int i = index + 1; i < arr.length; i++) {
        result[i - 1] = arr[i];
      }
    }

    return result;
  }

  /**
   * Copies the specified range of the {@code source} array into a new array. Implements JavaScript
   * array.slice method. Null-safe.
   * 
   * @param source array to slice
   * @param start specifies where to start the selection (The first element has an index of 0).
   *          Negative value selects from the end of an array.
   * @return a new array containing all elements from the start position to the end of the original
   *         array.
   */
  public static String[] slice(String[] source, int start) {
    if (source == null) {
      return null;
    }
    return slice(source, start, source.length);
  }

  /**
   * Copies the specified range of the {@code source} array into a new array. Implements JavaScript
   * array.slice method. Null-safe.
   * 
   * @param source array to slice
   * @param start specifies where to start the selection (The first element has an index of 0).
   *          Negative value selects from the end of an array.
   * @param end specifies where to end the selection. Negative value selects from the end of an
   *          array.
   * @return a new array containing the specified range from the original array.
   */
  public static String[] slice(String[] source, int start, int end) {
    if (source == null) {
      return null;
    }
    int srcLen = source.length;
    if (srcLen <= 0 || start >= srcLen || end <= -srcLen) {
      return BeeConst.EMPTY_STRING_ARRAY;
    }

    int p1 = (start >= 0) ? start : Math.max(srcLen + start, 0);
    int p2 = (end >= 0) ? Math.min(srcLen, end) : Math.max(srcLen + end, 0);
    if (p1 >= p2) {
      return BeeConst.EMPTY_STRING_ARRAY;
    }

    int len = p2 - p1;
    String[] arr = new String[len];
    for (int i = 0; i < len; i++) {
      arr[i] = source[p1 + i];
    }
    return arr;
  }

  public static int sum(int[] arr) {
    int result = 0;
    for (int v : arr) {
      result += v;
    }
    return result;
  }

  public static String[] toArray(List<String> list) {
    if (list == null) {
      return null;
    } else {
      return list.toArray(new String[list.size()]);
    }
  }

  public static String toString(Object arr) {
    if (arr instanceof Object[]) {
      return Arrays.deepToString((Object[]) arr);
    } else if (arr instanceof boolean[]) {
      return Arrays.toString((boolean[]) arr);
    } else if (arr instanceof char[]) {
      return Arrays.toString((char[]) arr);
    } else if (arr instanceof byte[]) {
      return Arrays.toString((byte[]) arr);
    } else if (arr instanceof short[]) {
      return Arrays.toString((short[]) arr);
    } else if (arr instanceof int[]) {
      return Arrays.toString((int[]) arr);
    } else if (arr instanceof long[]) {
      return Arrays.toString((long[]) arr);
    } else if (arr instanceof float[]) {
      return Arrays.toString((float[]) arr);
    } else if (arr instanceof double[]) {
      return Arrays.toString((double[]) arr);
    } else if (arr == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return arr.toString();
    }
  }

  private ArrayUtils() {
  }
}
