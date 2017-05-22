package com.butent.bee.shared.utils;

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasRange;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Contains base methods for development.
 */
public final class BeeUtils {

  public static final Joiner NUMBER_JOINER = Joiner.on(BeeConst.CHAR_COMMA).skipNulls();
  public static final Splitter NUMBER_SPLITTER =
      Splitter.on(CharMatcher.anyOf(" ,;")).trimResults().omitEmptyStrings();

  private static final MathContext DEFAULT_MATH_CONTEXT = new MathContext(15);

  public static <T> void addAllNotNull(Collection<T> target, Collection<T> source) {
    Assert.notNull(target);

    if (source != null) {
      for (T item : source) {
        if (item != null) {
          target.add(item);
        }
      }
    }
  }

  /**
   * Adds String {@code item} to collection {@code col} if {@code item} is not empty.
   *
   * @param col Collection where the {@code item} should be added to
   * @param item String that will be added
   * @return true if addition is successful
   */
  public static boolean addNotEmpty(Collection<String> col, String item) {
    if (isEmpty(item)) {
      return false;
    } else {
      Assert.notNull(col).add(item);
      return true;
    }
  }

  /**
   * Adds {@code item} to collection {@code col} if {@code item} is not null.
   *
   * @param col Collection where the {@code item} should be added to
   * @param item T that will be added
   * @return true if addition is successful
   */
  public static <T> boolean addNotNull(Collection<T> col, T item) {
    if (item == null) {
      return false;
    } else {
      Assert.notNull(col).add(item);
      return true;
    }
  }

  /**
   * Adds {@code item} to {@code list} at specified {@code index} or if index not valid appends
   * {@code item} to the end of {@code list}.
   *
   * @param list List to which the {@code item} should be added
   * @param index at which position to add
   * @param item which should be added
   */
  public static <T> void addQuietly(List<T> list, int index, T item) {
    if (isIndex(list, index)) {
      list.add(index, item);
    } else {
      list.add(item);
    }
  }

  /**
   * Checks whether all given Strings are empty or null.
   *
   * @param first String to check.
   * @param second String to check.
   * @param rest Rest of the Strings to check.
   * @return true if all given Strings are empty or null.
   */
  public static boolean allEmpty(String first, String second, String... rest) {
    if (!isEmpty(first) || !isEmpty(second)) {
      return false;
    }
    if (rest == null) {
      return true;
    }

    for (String s : rest) {
      if (!isEmpty(s)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks whether all given Strings are not empty and not null.
   *
   * @param first String to check.
   * @param second String to check.
   * @param rest Rest of the Strings to check.
   * @return true if all given Strings are not empty and not null.
   */
  public static boolean allNotEmpty(String first, String second, String... rest) {
    if (isEmpty(first) || isEmpty(second)) {
      return false;
    }
    if (rest == null) {
      return true;
    }

    for (String s : rest) {
      if (isEmpty(s)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks whether all provided objects are not null.
   *
   * @param first object to check.
   * @param second object to check.
   * @param rest objects to check.
   * @return true if all provided objects are not null.
   */
  public static boolean allNotNull(Object first, Object second, Object... rest) {
    if (first == null || second == null) {
      return false;
    }
    if (rest == null) {
      return true;
    }

    for (Object obj : rest) {
      if (obj == null) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if any of the provided Strings are empty or null.
   *
   * @param first String to check.
   * @param second String to check.
   * @param rest Strings to check.
   * @return true if any of the provided Strings are empty or null.
   */
  public static boolean anyEmpty(String first, String second, String... rest) {
    if (isEmpty(first) || isEmpty(second)) {
      return true;
    }
    if (rest == null) {
      return false;
    }

    for (String s : rest) {
      if (isEmpty(s)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if any of provided Strings are not empty and not null.
   *
   * @param first String to check.
   * @param second String to check.
   * @param rest Strings to check.
   * @return true if any of provided Strings are not empty and not null.
   */
  public static boolean anyNotEmpty(String first, String second, String... rest) {
    if (!isEmpty(first) || !isEmpty(second)) {
      return true;
    }
    if (rest == null) {
      return false;
    }

    for (String s : rest) {
      if (!isEmpty(s)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if any of the provided Objects are not null.
   *
   * @param first object to check.
   * @param second first object to check.
   * @param rest objects to check.
   * @return true if any of the provided objects are not null.
   */
  public static boolean anyNotNull(Object first, Object second, Object... rest) {
    if (first != null || second != null) {
      return true;
    }
    if (rest == null) {
      return false;
    }

    for (Object obj : rest) {
      if (obj != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if any of the provided Objects are not null.
   *
   * @param items collection of objects to check.
   * @return true if any of the provided objects in collection is not null.
   */
  public static boolean anyNotNull(Collection<Object> items) {
    if (!Objects.isNull(items)) {
      for (Object obj : items) {
        if (obj != null) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks whether any of the provided objects are null.
   *
   * @param first Object to check.
   * @param second Object to check.
   * @param rest Objects to check.
   * @return true if any of the provided objects are null.
   */
  public static boolean anyNull(Object first, Object second, Object... rest) {
    if (first == null || second == null) {
      return true;
    }
    if (rest == null) {
      return false;
    }

    for (Object obj : rest) {
      if (obj == null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the specified value {@code x} is between values {@code min} and {@code max}. Note:
   * {@code min} value is inclusive, {@code max} value - exclusive.
   *
   * @param x value to check
   * @param min the minimum value
   * @param max the maximum value
   * @return true if x is between {@code min} (inclusively) and {@code max} (exclusively), else
   * false.
   */
  public static boolean betweenExclusive(int x, int min, int max) {
    return x >= min && x < max;
  }

  /**
   * Checks if the specified value {@code x} is between values {@code min} and {@code max}
   * inclusively.
   *
   * @param x value to check
   * @param min the minimum value
   * @param max the maximum value
   * @return true if {@code x} is between {@code min} and {@code max} inclusively, else false.
   */
  public static boolean betweenInclusive(int x, int min, int max) {
    return x >= min && x <= max;
  }

  /**
   * Surrounds int value {@code x} in brackets.
   *
   * @param x int value to put in brackets.
   * @return a String representation of the int surrounded by brackets.
   */
  public static String bracket(int x) {
    return bracket(toString(x));
  }

  /**
   * Surrounds long value {@code x} in brackets.
   *
   * @param x long value to put in brackets.
   * @return a String representation of the long surrounded by brackets.
   */
  public static String bracket(long x) {
    return bracket(toString(x));
  }

  /**
   * Surrounds the String value {@code s} in brackets.
   *
   * @param s String to put in brackets.
   * @return a String representation of the String surrounded by brackets.
   */
  public static String bracket(String s) {
    if (isEmpty(s)) {
      return BeeConst.STRING_EMPTY;
    } else {
      return BeeConst.STRING_LEFT_BRACKET + s + BeeConst.STRING_RIGHT_BRACKET;
    }
  }

  /**
   * Constructs a String from given lines in a collection. Lines are joined using end of line
   * character. Nulls are skipped.
   *
   * @param lines Collection from which String will be constructed.
   * @return String which will be built from given lines in a collection.
   */
  public static String buildLines(Collection<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return null;
    }

    char sep = BeeConst.CHAR_EOL;
    StringBuilder sb = new StringBuilder();

    for (String line : lines) {
      if (line != null) {
        if (sb.length() > 0) {
          sb.append(sep);
        }
        sb.append(line);
      }
    }
    return sb.toString();
  }

  /**
   * Constructs a String text from given String lines. It takes each of the String line, adds end of
   * line character to the end of each and forms String text.
   *
   * @param lines from which the String will be built.
   * @return String which will be built from given String lines.
   */
  public static String buildLines(String... lines) {
    if (lines == null) {
      return null;
    }

    char sep = BeeConst.CHAR_EOL;
    StringBuilder sb = new StringBuilder();

    for (String line : lines) {
      if (line != null) {
        if (sb.length() > 0) {
          sb.append(sep);
        }
        sb.append(line);
      }
    }
    return sb.toString();
  }

  /**
   * Returns the value rounded to the bigger part (closer to the positive infinity) of the value
   * provided.
   *
   * @param x double which value should be rounded to the bigger part.
   * @return the value rounded to the bigger part (closer to the positive infinity) of the value
   * provided.
   */
  public static int ceil(double x) {
    return toInt(Math.ceil(x));
  }

  /**
   * Returns the result of applying {@code predicate} to {@code input}. If predicate is null,
   * returns true.
   *
   * @param predicate is the condition which should be checked.
   * @param input which should be checked.
   * @return true if {@code predicate} applies to {@code input}.
   */
  public static <T> boolean check(Predicate<T> predicate, T input) {
    return predicate == null || predicate.test(input);
  }

  /**
   * Gets the value between {@code min} and {@code max}.
   *
   * @param x a value to return in the specified limits
   * @param min the minimum possible value
   * @param max the maximum possible value
   * @return {@code x} if the value is between {@code min} and {@code max}, is the value is less
   * than {@code min} it returns {@code min}, if greater than {@code max} it returns
   * {@code max}.
   */
  public static double clamp(double x, double min, double max) {
    if (!isDouble(x)) {
      if (isDouble(min)) {
        return min;
      }
      if (isDouble(max)) {
        return max;
      }
      return x;
    }

    double z;
    if (isDouble(min) && isDouble(max)) {
      z = Math.min(min, max);
      if (x < z) {
        return z;
      }
      z = Math.max(min, max);
      if (x > z) {
        return z;
      }
      return x;
    }

    if (isDouble(min)) {
      return Math.max(x, min);
    }
    if (isDouble(max)) {
      return Math.min(x, max);
    }
    return x;
  }

  /**
   * Gets the value between {@code min} and {@code max}.
   *
   * @param x a value to return in the specified limits
   * @param min the minimum possible value
   * @param max the maximum possible value
   * @return x if the value is between {@code min} and {@code max}, if the value is less than
   * {@code min} it returns {@code min}, if greater than {@code max} it returns {@code max}.
   */
  public static int clamp(int x, int min, int max) {
    int z = Math.min(min, max);
    if (x < z) {
      return z;
    }
    z = Math.max(min, max);
    if (x > z) {
      return z;
    }
    return x;
  }

  /**
   * A string in the specified position is appended with "..." and the left string after the index
   * {@code n} is replaced with a progress indicator.
   * <p>
   * E.g {@code clip("This is a sentence", 6)} result is {@code  "This i...[6/18]"}
   * </p>
   *
   * @param s a String to modify
   * @param n an index in the String where to modify
   * @return returns a String with a progress indicator after the specified {@code n} index
   */
  public static String clip(String s, int n) {
    Assert.isPositive(n);
    if (isEmpty(s)) {
      return BeeConst.STRING_EMPTY;
    }
    if (s.length() <= n) {
      return s;
    }

    int len = s.trim().length();

    if (len <= n) {
      return s.trim();
    } else {
      return s.substring(0, n).trim() + BeeConst.ELLIPSIS + bracket(progress(n, len));
    }
  }

  /**
   * Compares {@code x1} and {@code x2}. Both values must implement the Comparable interface. This
   * method allows to compare values even if one or both of the specified values are {@code null}.
   *
   * @param x1 first value to compare
   * @param x2 second value to compare
   * @return 0 if objects are equal, -1 if {@code x1 < x2}, and 1 if @code {x1 > x2}.
   */
  public static <T extends Comparable<T>> int compare(T x1, T x2, NullOrdering nullOrdering) {
    if (nullOrdering == NullOrdering.NULLS_FIRST
        || nullOrdering == null && NullOrdering.DEFAULT == NullOrdering.NULLS_FIRST) {
      return compareNullsFirst(x1, x2);
    } else {
      return compareNullsLast(x1, x2);
    }
  }

  public static <T extends Comparable<T>> int compareNullsFirst(T x1, T x2) {
    if (x1 == null) {
      if (x2 == null) {
        return BeeConst.COMPARE_EQUAL;
      } else {
        return BeeConst.COMPARE_LESS;
      }
    } else if (x2 == null) {
      return BeeConst.COMPARE_MORE;
    } else {
      return x1.compareTo(x2);
    }
  }

  public static <T extends Comparable<T>> int compareNullsLast(T x1, T x2) {
    if (x1 == null) {
      if (x2 == null) {
        return BeeConst.COMPARE_EQUAL;
      } else {
        return BeeConst.COMPARE_MORE;
      }
    } else if (x2 == null) {
      return BeeConst.COMPARE_LESS;
    } else {
      return x1.compareTo(x2);
    }
  }

  public static <T> boolean contains(Collection<? extends T> col, T item) {
    if (col == null) {
      return false;
    } else {
      return col.contains(item);
    }
  }

  public static boolean contains(String s, char ch) {
    if (s == null) {
      return false;
    } else {
      return s.indexOf(ch) >= 0;
    }
  }

  public static boolean containsAnySame(String x, String first, String second, String... rest) {
    if (isEmpty(x)) {
      return false;
    }
    if (containsSame(x, first)) {
      return true;
    }
    if (containsSame(x, second)) {
      return true;
    }

    if (rest == null) {
      return false;
    }
    for (String y : rest) {
      if (containsSame(x, y)) {
        return true;
      }
    }
    return false;
  }

  public static <K> boolean containsKey(Map<? extends K, ?> map, K key) {
    if (map == null) {
      return false;
    } else {
      return map.containsKey(key);
    }
  }

  /**
   * Checks if the CharSequence {@code src} contains only of the specified characters.
   *
   * @param src source CharSequence to be checked
   * @param ch characters to check for
   * @return true if the src contains only the specified characters, false if src contains any
   * different characters.
   */
  public static boolean containsOnly(CharSequence src, char ch) {
    if (src == null) {
      return false;
    }
    int len = src.length();
    if (len <= 0) {
      return false;
    }

    boolean ok = true;
    for (int i = 0; i < len; i++) {
      if (src.charAt(i) != ch) {
        ok = false;
        break;
      }
    }
    return ok;
  }

  public static boolean containsSame(Collection<String> col, String s) {
    if (isEmpty(col)) {
      return false;
    }

    for (String entry : col) {
      if (same(entry, s)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if and {@code src} string contains the specified sequence of {@code ctxt} string
   * value in case-insensitive.
   *
   * @param src source of search
   * @param ctxt the sequence to search for
   * @return true if {@code src} string contains {@code ctxt}, false otherwise
   */
  public static boolean containsSame(String src, String ctxt) {
    if (isEmpty(src) || isEmpty(ctxt)) {
      return false;
    } else {
      return src.trim().toLowerCase().contains(ctxt.trim().toLowerCase());
    }
  }

  public static boolean containsWhitespace(CharSequence cs) {
    if (cs == null) {
      return false;
    }
    for (int i = 0; i < cs.length(); i++) {
      if (isWhitespace(cs.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Counts the appearances of the specified character {@code ch} in a CharSequence.
   *
   * @param src source to count in
   * @param ch characters to count
   * @return a number of the specified character appearances in the CharSequence.
   */
  public static int count(CharSequence src, char ch) {
    int cnt = 0;
    if (src == null) {
      return cnt;
    }

    for (int i = 0; i < src.length(); i++) {
      if (src.charAt(i) == ch) {
        cnt++;
      }
    }
    return cnt;
  }

  public static int countLines(String src) {
    if (src == null) {
      return 0;
    }

    String s = src.trim();
    if (s.isEmpty()) {
      return 0;
    }

    int cnt = count(s, BeeConst.CHAR_EOL);
    if (cnt <= 0) {
      cnt = count(s, BeeConst.CHAR_CR);
    }

    return cnt + 1;
  }

  /**
   * Deletes a part of a String from specified {@code start} to {@code end}.
   *
   * @param src source String to delete from
   * @param start position to start deleting from
   * @param end position to end deleting
   * @return a String without the deleted part. empty String - if {@code src} is {@code null} or
   * empty, {@code src} - if start and end is wrong to given String
   */
  public static String delete(String src, int start, int end) {
    if (src == null) {
      return BeeConst.STRING_EMPTY;
    }
    int len = src.length();
    if (len <= 0) {
      return BeeConst.STRING_EMPTY;
    }
    if (start >= len || start >= end || end <= 0) {
      return src;
    }

    if (start <= 0 && end >= len) {
      return BeeConst.STRING_EMPTY;
    }
    if (start <= 0) {
      return src.substring(end);
    }
    if (end >= len) {
      return src.substring(0, start);
    }
    return src.substring(0, start) + src.substring(end);
  }

  public static <T> Set<T> difference(Collection<? extends T> col1, Collection<? extends T> col2) {
    Set<T> result;

    if (isEmpty(col1)) {
      result = new HashSet<>();

    } else {
      result = new HashSet<>(col1);

      if (!isEmpty(col2)) {
        result.removeAll(col2);
      }
    }

    return result;
  }

  public static double distance(double x1, double y1, double x2, double y2) {
    double dx = x2 - x1;
    double dy = y2 - y1;

    return Math.sqrt(dx * dx + dy * dy);
  }

  public static double div(int x, int y) {
    return x / (double) y;
  }

  public static String embrace(String s) {
    if (isEmpty(s)) {
      return BeeConst.STRING_EMPTY;
    } else {
      return BeeConst.STRING_LEFT_BRACE + s + BeeConst.STRING_RIGHT_BRACE;
    }
  }

  public static <T> Collection<T> emptyToNull(Collection<T> col) {
    return isEmpty(col) ? null : col;
  }

  public static boolean equals(String s1, String s2) {
    if (s1 == null) {
      return isEmpty(s2);
    } else if (s2 == null) {
      return isEmpty(s1);
    } else {
      return s1.equals(s2);
    }
  }

  /**
   * Trims both Strings and then compares them.
   *
   * @param s1 the String to compare
   * @param s2 the String to compare
   * @return true if trimmed Strings are equal, false if Strings differ or any of them are empty or
   * {@code null}.
   */
  public static boolean equalsTrim(String s1, String s2) {
    if (s1 == null) {
      return isEmpty(s2);
    } else if (s2 == null) {
      return isEmpty(s1);
    } else {
      return s1.trim().equals(s2.trim());
    }
  }

  public static boolean equalsTrimRight(String s1, String s2) {
    if (s1 == null) {
      return isEmpty(s2);
    } else if (s2 == null) {
      return isEmpty(s1);
    } else {
      return trimRight(s1).equals(trimRight(s2));
    }
  }

  /**
   * Raises the value {@code z} to the power of 10.
   *
   * @param z value to raise to the power of 10
   * @return of value of the specified argument raised to the power of 10.
   */
  public static int exp10(int z) {
    Assert.nonNegative(z);
    Double x = Math.pow(10, z);
    Assert.isTrue(x < Integer.MAX_VALUE);
    return x.intValue();
  }

  /**
   * If any {@code src} Collection element contains {@code ctxt} (case is ignored), than that
   * element is added to the new Collection and returned after all elements from {@code src}
   * Collection are covered.
   *
   * @param src collection to search from
   * @param ctxt context to search for
   * @return a new list with elements that contain {@code ctxt} in {@code src} collection.
   */
  public static List<String> filterContext(Collection<String> src, String ctxt) {
    List<String> result = new ArrayList<>();
    if (src == null) {
      return result;
    }

    for (String el : src) {
      if (containsSame(el, ctxt)) {
        result.add(el);
      }
    }
    return result;
  }

  /**
   * Looks for a fitting starting position.
   *
   * @param start the start index
   * @param len the length
   * @param end the end index
   * @return the new starting position
   */
  public static int fitStart(int start, int len, int end) {
    if (start + len <= end) {
      return start;
    } else {
      return end - len;
    }
  }

  public static int floor(double x) {
    return toInt(Math.floor(x));
  }

  /**
   * Converts a String representation of Hex symbol to a corresponding Hex symbol.
   * <p>
   * <b>Note: the specified String must be in double bytes.</b> E.g "005D" is "]".
   * <p>
   *
   * @param s the string to convert
   * @return returns a corresponding Hex symbol for the specified String input, {@code null} if the
   * specified String format is wrong
   */
  public static char[] fromHex(String s) {
    if (!isHexString(s)) {
      return null;
    }

    if (s.length() <= 4) {
      return new char[] {(char) Integer.parseInt(s, 16)};
    } else {
      int z = s.length() % 4;
      int n;

      if (z == 0) {
        n = s.length() / 4;
      } else {
        n = (s.length() - z) / 4 + 1;
      }

      char[] arr = new char[n];
      String u;

      for (int i = 0; i < n; i++) {
        if (z == 0) {
          u = s.substring(i * 4, (i + 1) * 4);
        } else if (i == 0) {
          u = s.substring(0, z);
        } else {
          u = s.substring(z + (i - 1) * 4, z + i * 4);
        }

        arr[i] = (char) Integer.parseInt(u, 16);
      }
      return arr;
    }
  }

  public static int getDecimals(String s) {
    int index = (s == null) ? BeeConst.UNDEF : s.lastIndexOf(BeeConst.CHAR_POINT);
    return (index >= 0) ? s.length() - index - 1 : 0;
  }

  public static <K, V> Collection<V> getIfContains(Multimap<K, V> multimap, K key) {
    if (multimap != null && multimap.containsKey(key)) {
      return multimap.get(key);
    } else {
      return null;
    }
  }

  public static <T> T getLast(List<? extends T> lst) {
    int size = size(lst);
    if (size > 0) {
      return lst.get(size - 1);
    } else {
      return null;
    }
  }

  public static <C extends Comparable<C>> C getLowerEndpoint(Range<C> range) {
    return (range != null && range.hasLowerBound()) ? range.lowerEndpoint() : null;
  }

  /**
   * Gets a prefix from a String, where separator sets that the prefix will end at the first
   * occurrence of the separator.
   *
   * @param src the source to get a prefix from
   * @param sep the separator.
   * @return a String, where the length is determined by the first occurrence of the separator.
   */
  public static String getPrefix(String src, char sep) {
    if (isEmpty(src)) {
      return BeeConst.STRING_EMPTY;
    }

    int p = src.indexOf(sep);

    if (p > 0) {
      return src.substring(0, p).trim();
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  /**
   * Separates a String with separator value. Returns a String that goes before the separator.
   *
   * @param src source to get the prefix from
   * @param sep the separator
   * @return a prefix value that goes before the separator.
   */
  public static String getPrefix(String src, String sep) {
    if (isEmpty(src) || length(sep) <= 0) {
      return BeeConst.STRING_EMPTY;
    }

    int p = src.indexOf(sep);

    if (p > 0) {
      return src.substring(0, p).trim();
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  /**
   * Returns a value from the list in the specified index.
   *
   * @param lst the list to return from
   * @param idx the element which to return by index
   * @return return an element in the specified position.
   */
  public static <T> T getQuietly(List<? extends T> lst, int idx) {
    if (isIndex(lst, idx)) {
      return lst.get(idx);
    } else {
      return null;
    }
  }

  public static <K, V> V getQuietly(Map<? extends K, ? extends V> map, K key) {
    if (map == null) {
      return null;
    } else {
      return map.get(key);
    }
  }

  public static String getSame(Collection<String> col, String s) {
    if (isEmpty(col)) {
      return null;
    }

    for (String entry : col) {
      if (same(entry, s)) {
        return entry;
      }
    }
    return null;
  }

  /**
   * Separates a string with separator value. Returns a string that goes after the separator.
   *
   * @param src source string to get suffix from
   * @param sep separator
   * @return a suffix value that goes after the separator.
   */
  public static String getSuffix(String src, char sep) {
    if (isEmpty(src)) {
      return BeeConst.STRING_EMPTY;
    }

    int p = src.lastIndexOf(sep);

    if (p >= 0 && p < src.length() - 1) {
      return src.substring(p + 1).trim();
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  /**
   * Separates a string with separator value. Returns a string that goes after the separator.
   *
   * @param src source string to get suffix from
   * @param sep separator
   * @return a suffix value that goes after the separator.
   */
  public static String getSuffix(String src, String sep) {
    if (isEmpty(src) || length(sep) <= 0) {
      return BeeConst.STRING_EMPTY;
    }

    int p = src.lastIndexOf(sep);

    if (p >= 0 && p < src.length() - sep.length()) {
      return src.substring(p + sep.length()).trim();
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  public static <C extends Comparable<C>> C getUpperEndpoint(Range<C> range) {
    return (range != null && range.hasUpperBound()) ? range.upperEndpoint() : null;
  }

  public static boolean hasDigit(String s) {
    if (s == null || s.isEmpty()) {
      return false;
    }

    for (int i = 0; i < s.length(); i++) {
      if (isDigit(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasExponent(String s) {
    return contains(s, 'E') || contains(s, 'e');
  }

  public static boolean hasLength(CharSequence cs) {
    return hasLength(cs, 1);
  }

  public static boolean hasLength(CharSequence cs, int min) {
    if (cs == null) {
      return false;
    }
    return cs.length() >= min;
  }

  public static boolean hasDistinctElements(List<?> list) {
    if (size(list) > 1) {
      return list.stream().distinct().count() == list.size();
    } else {
      return true;
    }
  }

  public static int indexOf(String src, String ctxt, int fromIndex, boolean caseSensitive) {
    if (src == null || ctxt == null) {
      return BeeConst.UNDEF;
    } else if (caseSensitive) {
      return src.indexOf(ctxt, fromIndex);
    } else {
      return src.toLowerCase().indexOf(ctxt.toLowerCase(), fromIndex);
    }
  }

  public static <T> int indexOf(List<? extends T> list, T item) {
    return (list == null) ? BeeConst.UNDEF : list.indexOf(item);
  }

  public static int indexOfSame(List<String> list, String s) {
    if (isEmpty(list)) {
      return BeeConst.UNDEF;
    }

    for (int i = 0; i < list.size(); i++) {
      if (same(list.get(i), s)) {
        return i;
      }
    }
    return BeeConst.UNDEF;
  }

  @SafeVarargs
  public static <T> boolean in(T x, T first, T second, T... rest) {
    if (x == null) {
      return false;
    }
    if (x.equals(first)) {
      return true;
    }
    if (x.equals(second)) {
      return true;
    }

    if (rest == null) {
      return false;
    }
    for (T y : rest) {
      if (x.equals(y)) {
        return true;
      }
    }
    return false;
  }

  public static boolean inList(int x, int first, int second, int... rest) {
    if (x == first || x == second) {
      return true;
    }
    if (rest == null) {
      return false;
    }
    for (int y : rest) {
      if (x == y) {
        return true;
      }
    }
    return false;
  }

  public static boolean inList(String x, String first, String second, String... rest) {
    if (x == null) {
      return false;
    }
    if (first != null && equalsTrim(x, first)) {
      return true;
    }
    if (second != null && equalsTrim(x, second)) {
      return true;
    }

    if (rest == null) {
      return false;
    }
    for (String y : rest) {
      if (y != null && equalsTrim(x, y)) {
        return true;
      }
    }
    return false;
  }

  public static boolean inListSame(String x, String first, String second, String... rest) {
    if (x == null) {
      return false;
    }
    if (first != null && same(x, first)) {
      return true;
    }
    if (second != null && same(x, second)) {
      return true;
    }

    if (rest == null) {
      return false;
    }
    for (String y : rest) {
      if (y != null && same(x, y)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Inserts a character to a specified position in the String.
   *
   * @param src the string to be inserted to
   * @param pos position to insert to
   * @param c character to insert
   * @return a String with the character inserted in the specified position
   */
  public static String insert(String src, int pos, char c) {
    Assert.notNull(src);
    Assert.nonNegative(pos);
    Assert.isTrue(pos <= src.length());

    return new StringBuilder(src).insert(pos, c).toString();
  }

  public static <T> Set<T> intersection(Collection<? extends T> col1,
      Collection<? extends T> col2) {

    Set<T> result;

    if (isEmpty(col1) || isEmpty(col2)) {
      result = new HashSet<>();

    } else {
      result = new HashSet<>(col1);
      result.retainAll(col2);
    }

    return result;
  }

  public static <C extends Comparable<C>> boolean intersects(Collection<? extends HasRange<C>> col,
      Range<C> range) {
    if (col == null || range == null) {
      return false;
    }

    for (HasRange<C> item : col) {
      if (item != null && intersects(item.getRange(), range)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks is there are equal elements in the Collections.
   *
   * @param c1 first Collection's elements to be compared
   * @param c2 second Collection's elements to be compared
   * @return true if one of the collections contain at least one equal element from the other
   * collection, otherwise false.
   */
  public static <T> boolean intersects(Collection<T> c1, Collection<T> c2) {
    boolean ok = false;

    int n1 = size(c1);
    if (n1 <= 0) {
      return ok;
    }
    int n2 = size(c2);
    if (n2 <= 0) {
      return ok;
    }

    if (n1 <= n2) {
      for (T el : c1) {
        if (c2.contains(el)) {
          ok = true;
          break;
        }
      }
    } else {
      for (T el : c2) {
        if (c1.contains(el)) {
          ok = true;
          break;
        }
      }
    }
    return ok;
  }

  public static <C extends Comparable<C>> boolean intersects(Range<C> r1, Range<C> r2) {
    if (r1 == null || r2 == null) {
      return false;
    } else if (r1.isConnected(r2)) {
      return !r1.intersection(r2).isEmpty();
    } else {
      return false;
    }
  }

  public static boolean isAsciiLetter(char c) {
    return Ascii.isUpperCase(c) || Ascii.isLowerCase(c);
  }

  /**
   * Checks whether {@code d} is between {@code min} and {@code max}.
   *
   * @param d Double to check
   * @param min minimum value
   * @param minInclusive true if minimum value should be included to check
   * @param max maximum value
   * @param maxInclusive true if maximum value should be included to check
   * @return true if d is between min and max
   */
  public static boolean isBetween(Double d, Double min, boolean minInclusive,
      Double max, boolean maxInclusive) {
    if (!isDouble(d)) {
      return false;
    }

    boolean ok = true;
    if (isDouble(min)) {
      ok = minInclusive ? (d >= min) : (d > min);
    }
    if (ok && isDouble(max)) {
      ok = maxInclusive ? (d <= max) : (d > max);
    }
    return ok;
  }

  /**
   * Checks if {@code s} is a Boolean value.
   *
   * @param s a String to check
   * @return true if s equals "true", "false", "yes", "no", false otherwise.
   */
  public static boolean isBoolean(String s) {
    if (s == null) {
      return false;
    } else {
      return BeeConst.isTrue(s) || BeeConst.isFalse(s);
    }
  }

  public static boolean isDecimal(String s) {
    if (isEmpty(s)) {
      return false;
    }
    BigDecimal d;

    try {
      d = new BigDecimal(s.trim());
    } catch (NumberFormatException ex) {
      d = null;
    }
    return d != null;
  }

  public static boolean isDelimited(CharSequence cs, char delimiter) {
    return isDelimited(cs, delimiter, delimiter);
  }

  public static boolean isDelimited(CharSequence cs, char start, char end) {
    if (cs == null || cs.length() < 2) {
      return false;
    } else {
      return cs.charAt(0) == start && cs.charAt(cs.length() - 1) == end;
    }
  }

  /**
   * Checks if a character {@code c} is a digit.
   *
   * @param c character to check
   * @return true if the character is {@code >= 0 and <= 9};
   */
  public static boolean isDigit(char c) {
    return c >= BeeConst.CHAR_ZERO && c <= BeeConst.CHAR_NINE;
  }

  /**
   * Checks if a CharacterSequence {@code d} is a digit.
   *
   * @param s CharSequence to check
   * @return true if all characters in the sequence are digits, false if sequence is {@code null} or
   * empty or contains at least one non-digit character.
   */
  public static boolean isDigit(CharSequence s) {
    if (s == null) {
      return false;
    }

    int len = s.length();
    if (len < 1) {
      return false;
    }
    boolean ok = true;

    for (int i = 0; i < len; i++) {
      if (!isDigit(s.charAt(i))) {
        ok = false;
        break;
      }
    }
    return ok;
  }

  /**
   * Checks if the value {@code x} is Double.
   *
   * @param x a Double value to check
   * @return true if x is a number and is not infinite, otherwise false
   */
  public static boolean isDouble(Double x) {
    return x != null && !Double.isNaN(x) && !Double.isInfinite(x);
  }

  /**
   * Checks if value {@code s} can be cast to a Double value.
   *
   * @param s a string value to check
   * @return true if {@code s} value can be cast to Double, false otherwise.
   */
  public static boolean isDouble(String s) {
    if (!hasDigit(s)) {
      return false;
    }
    boolean ok;

    try {
      Double.parseDouble(s);
      ok = true;
    } catch (NumberFormatException ex) {
      ok = false;
    }
    return ok;
  }

  public static boolean isDouble(String s, Double min, boolean minInclusive) {
    return isDouble(s, min, minInclusive, null, false);
  }

  /**
   * Checks if Double from String {@code s} is between min and max.
   *
   * @param s String to be converted to Double and checked if it is between min and max values
   * @param min minimum value
   * @param minInclusive true if minimum value should be included
   * @param max maximum value
   * @param maxInclusive true if maximum value should be included
   * @return true if {@code s} is between min and max
   */
  public static boolean isDouble(String s, Double min, boolean minInclusive,
      Double max, boolean maxInclusive) {
    if (!hasDigit(s)) {
      return false;
    }

    boolean ok;
    try {
      double d = Double.parseDouble(s);
      ok = isBetween(d, min, minInclusive, max, maxInclusive);
    } catch (NumberFormatException ex) {
      ok = false;
    }
    return ok;
  }

  /**
   * Checks whether {@code col} is empty or null.
   *
   * @param col Collection to be checked
   * @return true if collection is empty or null.
   */
  public static boolean isEmpty(Collection<?> col) {
    return col == null || col.isEmpty();
  }

  /**
   * Checks whether given {@code map} is empty or null.
   *
   * @param map to be checked
   * @return true if map is empty or null.
   */
  public static boolean isEmpty(Map<?, ?> map) {
    return map == null || map.isEmpty();
  }

  /**
   * Checks whether {@code s} is empty or null.
   *
   * @param s String to be checked.
   * @return true if String is empty or null.
   */
  public static boolean isEmpty(String s) {
    return s == null || s.trim().isEmpty();
  }

  public static boolean isFalse(Boolean b) {
    return Boolean.FALSE.equals(b);
  }

  /**
   * Checks if the specified character contains only Hex digits.
   *
   * @param c the value to check
   * @return true if the value contains only Hex digits, false otherwise.
   */
  public static boolean isHexDigit(char c) {
    return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
  }

  /**
   * Checks if the specified String contains only Hex digits.
   *
   * @param s the value to check
   * @return true if the String contains only Hex digits,false otherwise.
   */
  public static boolean isHexString(String s) {
    if (isEmpty(s)) {
      return false;
    }
    boolean ok = true;

    for (int i = 0; i < s.length(); i++) {
      if (!isHexDigit(s.charAt(i))) {
        ok = false;
        break;
      }
    }
    return ok;
  }

  /**
   * Checks whether collection is not null, index is lower than the size of collection and index is
   * not negative.
   *
   * @param col Collection to be checked
   * @param idx index
   * @return true if index is valid.
   */
  public static boolean isIndex(Collection<?> col, int idx) {
    return col != null && idx >= 0 && idx < col.size();
  }

  public static boolean isInt(long x) {
    return x >= Integer.MIN_VALUE && x <= Integer.MAX_VALUE;
  }

  /**
   * Checks if a String value can be transformed to an Integer value.
   *
   * @param s a string value to transform
   * @return true if the string has a correct number format, otherwise false.
   */
  public static boolean isInt(String s) {
    if (!hasDigit(s)) {
      return false;
    }
    boolean ok;

    try {
      Integer.parseInt(s.trim());
      ok = true;
    } catch (NumberFormatException ex) {
      ok = false;
    }
    return ok;
  }

  public static <T extends Comparable<T>> boolean isLeq(T x1, T x2) {
    return compareNullsFirst(x1, x2) <= 0;
  }

  public static <T extends Comparable<T>> boolean isLess(T x1, T x2) {
    return compareNullsFirst(x1, x2) < 0;
  }

  /**
   * Checks if a Double value can be transformed to a Long value.
   *
   * @param x the value to check
   * @return true if x value range is in long range, false otherwise.
   */
  public static boolean isLong(double x) {
    return isDouble(x) && x > Long.MIN_VALUE && x < Long.MAX_VALUE;
  }

  public static boolean isLong(String s) {
    if (!hasDigit(s)) {
      return false;
    }
    boolean ok;

    try {
      Long.parseLong(s.trim());
      ok = true;
    } catch (NumberFormatException ex) {
      ok = false;
    }
    return ok;
  }

  public static <T extends Comparable<T>> boolean isMeq(T x1, T x2) {
    return compareNullsFirst(x1, x2) >= 0;
  }

  public static <T extends Comparable<T>> boolean isMore(T x1, T x2) {
    return compareNullsFirst(x1, x2) > 0;
  }

  public static boolean isNegative(Double d) {
    if (isDouble(d)) {
      return Double.compare(d, BeeConst.DOUBLE_ZERO) < 0;
    } else {
      return false;
    }
  }

  public static boolean isNegative(Integer x) {
    return x != null && x < 0;
  }

  public static boolean isNegativeInt(String s) {
    return isInt(s) && toInt(s) < 0;
  }

  public static boolean isNonNegative(Double d) {
    if (isDouble(d)) {
      return Double.compare(d, BeeConst.DOUBLE_ZERO) >= 0;
    } else {
      return false;
    }
  }

  public static boolean isNonNegative(Integer x) {
    return x != null && x >= 0;
  }

  public static boolean isNonNegative(Long x) {
    return x != null && x >= 0L;
  }

  public static boolean isNonNegativeDouble(String s) {
    return isDouble(s, BeeConst.DOUBLE_ZERO, true);
  }

  public static boolean isNonNegativeInt(String s) {
    return isInt(s) && toInt(s) >= 0;
  }

  public static boolean isPositive(BigDecimal x) {
    return x != null && x.compareTo(BigDecimal.ZERO) > 0;
  }

  public static boolean isPositive(Double d) {
    if (isDouble(d)) {
      return Double.compare(d, BeeConst.DOUBLE_ZERO) > 0;
    } else {
      return false;
    }
  }

  public static boolean isPositive(Integer x) {
    return x != null && x > 0;
  }

  public static boolean isPositive(Long x) {
    return x != null && x > 0L;
  }

  public static boolean isPositiveDouble(String s) {
    return isDouble(s, BeeConst.DOUBLE_ZERO, false);
  }

  public static boolean isPositiveInt(String s) {
    return isInt(s) && toInt(s) > 0;
  }

  /**
   * Checks if the first character in a CharSequence is a prefix.
   *
   * @param src sequence to check
   * @param pfx a prefix to check for
   * @return true if the first character equals {@code pfx}, otherwise false.
   */
  public static boolean isPrefix(CharSequence src, char pfx) {
    if (src == null || src.length() <= 0) {
      return false;
    }
    return src.charAt(0) == pfx;
  }

  public static boolean isPrefix(String src, String pfx) {
    if (src == null || pfx == null) {
      return false;
    }
    int srcLen = src.length();
    int pfxLen = pfx.length();
    if (pfxLen <= 0 || srcLen <= pfxLen) {
      return false;
    }
    return same(left(src, pfxLen), pfx);
  }

  /**
   * Checks if the specified character is a prefix or a suffix.
   *
   * @param src a sequence to check in
   * @param ch a suffix of prefix to search for
   * @return true if the first or the last character equals {@code pfx}, false if the sequence
   * contains only one symbol and is suffix or prefix, and if the sequence does not contain
   * nor suffix nor prefix
   */
  public static boolean isPrefixOrSuffix(CharSequence src, char ch) {
    return (isPrefix(src, ch) || isSuffix(src, ch)) && !containsOnly(src, ch);
  }

  public static boolean isQuoted(String s) {
    return isDelimited(s, BeeConst.CHAR_QUOT) || isDelimited(s, BeeConst.CHAR_APOS);
  }

  /**
   * Checks if the last character in a CharSequence is a suffix.
   *
   * @param src sequence to check
   * @param sfx a suffix to check for
   * @return true if the last character equals {@code sfx}, otherwise false.
   */
  public static boolean isSuffix(CharSequence src, char sfx) {
    if (src == null) {
      return false;
    }
    int len = src.length();
    if (len <= 0) {
      return false;
    }
    return src.charAt(len - 1) == sfx;
  }

  public static boolean isSuffix(String src, String sfx) {
    if (src == null || sfx == null) {
      return false;
    }
    int srcLen = src.length();
    int sfxLen = sfx.length();
    if (sfxLen <= 0 || srcLen <= sfxLen) {
      return false;
    }
    return same(right(src, sfxLen), sfx);
  }

  /**
   * Checks if the specified value is true.
   *
   * @param b value to check
   * @return true if the Object is a Boolean.TRUE, otherwise false.
   */
  public static boolean isTrue(Boolean b) {
    if (b == null) {
      return false;
    } else {
      return b;
    }
  }

  public static boolean isWhitespace(char ch) {
    return ch <= BeeConst.CHAR_SPACE || ch == BeeConst.CHAR_NBSP;
  }

  /**
   * Checks if a Double value is zero.
   *
   * @param x value to check
   * @return true if x == 0.0, otherwise false.
   */
  public static boolean isZero(Double x) {
    return x != null && x.equals(BeeConst.DOUBLE_ZERO);
  }

  /**
   * Transforms a Collection {@code col} to a String representation using the specified separator
   * {@code separator}.
   *
   * @param separator separator used to transform. Uses a default separator if none are specified.
   * @param col a Collection to transform.
   * @return a String representation of the Collection {@code col}.
   */
  public static String join(String separator, Collection<?> col) {
    if (isEmpty(col)) {
      return BeeConst.STRING_EMPTY;
    }

    String sep = nvl(separator, BeeConst.DEFAULT_LIST_SEPARATOR);
    StringBuilder sb = new StringBuilder();

    for (Object el : col) {
      String s = transform(el);
      if (!s.isEmpty()) {
        if (sb.length() > 0) {
          sb.append(sep);
        }
        sb.append(s);
      }
    }
    return sb.toString();
  }

  /**
   * Joins specified Objects. The first argument is the separator for the rest of the arguments.
   *
   * @param sep separator
   * @return returns a string containing the string representation of each of {@code obj}, using the
   * separator {@code sep} between each.
   */
  public static String join(String sep, Object first, Object second, Object... rest) {
    return doJoin(true, sep, first, second, rest);
  }

  public static String joinInts(Collection<Integer> ints) {
    return isEmpty(ints) ? null : NUMBER_JOINER.join(ints);
  }

  public static String joinItems(Collection<?> col) {
    return join(BeeConst.DEFAULT_LIST_SEPARATOR, col);
  }

  public static String joinItems(Object first, Object second, Object... rest) {
    return join(BeeConst.DEFAULT_LIST_SEPARATOR, first, second, rest);
  }

  public static String joinLongs(Collection<Long> longs) {
    return isEmpty(longs) ? null : NUMBER_JOINER.join(longs);
  }

  public static String joinNoDuplicates(String sep, Object first, Object second, Object... rest) {
    return doJoin(false, sep, first, second, rest);
  }

  public static String joinOptions(Object... options) {
    Assert.notNull(options);
    int c = options.length;
    Assert.parameterCount(c, 2);

    StringBuilder sb = new StringBuilder();
    String el;

    for (int i = 0; i < c - 1; i += 2) {
      el = NameUtils.addName(transform(options[i]), transform(options[i + 1]));
      if (!isEmpty(el)) {
        if (sb.length() > 0) {
          sb.append(BeeConst.DEFAULT_OPTION_SEPARATOR);
        }
        sb.append(el);
      }
    }
    return sb.toString();
  }

  public static String joinWords(Collection<?> col) {
    return join(BeeConst.STRING_SPACE, col);
  }

  public static String joinWords(Object first, Object second, Object... rest) {
    return join(BeeConst.STRING_SPACE, first, second, rest);
  }

  /**
   * Cuts the string from the beginning to a specified index.
   *
   * @param s string to cut
   * @param n index to cut to
   * @return a String with a cut part out of the String.
   */
  public static String left(String s, int n) {
    if (s == null) {
      return null;
    } else if (n <= 0) {
      return BeeConst.STRING_EMPTY;
    } else if (s.length() <= n) {
      return s;
    } else {
      return s.substring(0, n);
    }
  }

  public static int length(String s) {
    return (s == null) ? 0 : s.length();
  }

  public static Integer max(Collection<Integer> col) {
    Integer result = null;
    if (col == null) {
      return result;
    }

    for (Integer item : col) {
      if (item != null) {
        if (result == null) {
          result = item;
        } else {
          result = Math.max(result, item);
        }
      }
    }
    return result;
  }

  public static <T extends Comparable<T>> T max(T x1, T x2) {
    if (x1 == null) {
      return x2;
    } else if (x2 == null) {
      return x1;
    } else if (isMore(x1, x2)) {
      return x1;
    } else {
      return x2;
    }
  }

  public static Integer min(Collection<Integer> col) {
    Integer result = null;
    if (col == null) {
      return result;
    }

    for (Integer item : col) {
      if (item != null) {
        if (result == null) {
          result = item;
        } else {
          result = Math.min(result, item);
        }
      }
    }
    return result;
  }

  public static <T extends Comparable<T>> T min(T x1, T x2) {
    if (x1 == null) {
      return x2;
    } else if (x2 == null) {
      return x1;
    } else if (isLess(x1, x2)) {
      return x1;
    } else {
      return x2;
    }
  }

  public static Double minusPercent(Double d, Double p) {
    if (isDouble(d) && isDouble(p)) {
      return d - d * p / BeeConst.DOUBLE_ONE_HUNDRED;
    } else {
      return d;
    }
  }

  public static String nextString(String value) {
    String expression = trim(value);
    String pattern = "^(.*?)(\\d*?)$";
    String prefix = expression.replaceFirst(pattern, "$1");
    String suffix = expression.replaceFirst(pattern, "$2");

    if (isEmpty(suffix)) {
      suffix = "0";
    }
    int l = suffix.length();
    suffix = transform(Long.parseLong(suffix) + 1);

    return prefix + padLeft(suffix, l, '0');
  }

  public static int nonNegative(int x) {
    return (x >= 0) ? x : 0;
  }

  public static boolean nonZero(Double x) {
    return isDouble(x) && !isZero(x);
  }

  public static double normalize(double x, double min, double max) {
    double z;

    if (min > max) {
      z = 1.0 - normalize(x, max, min);
    } else if (min == max) {
      z = 0;
    } else if (x <= min) {
      z = 0;
    } else if (x >= max) {
      z = 1;
    } else {
      z = (x - min) / (max - min);
    }
    return z;
  }

  /**
   * Normalizes the String to lower case.
   *
   * @param s string to be normalized
   * @return a normalized string
   */
  public static String normalize(String s) {
    if (s == null) {
      return BeeConst.STRING_EMPTY;
    }
    return s.trim().toLowerCase();
  }

  public static String notEmpty(String s, String def) {
    return isEmpty(s) ? def : s;
  }

  public static String notEmpty(String s1, String s2, String def) {
    return isEmpty(s1) ? (isEmpty(s2) ? def : s2) : s1;
  }

  public static String nullOrTrim(String s) {
    return (s == null) ? null : s.trim();
  }

  /**
   * Gets the first object which is not {@code null}.
   *
   * @return the first not null object in specified objects.
   */
  public static <T> T nvl(T o1, T o2) {
    return (o1 == null) ? o2 : o1;
  }

  public static <T> T nvl(T o1, T o2, T o3) {
    return nvl(nvl(o1, o2), o3);
  }

  public static <T> T nvl(T o1, T o2, T o3, T o4) {
    return nvl(nvl(o1, o2), nvl(o3, o4));
  }

  public static <T> void overwrite(Collection<T> target, Collection<T> source) {
    Assert.notNull(target);
    if (!target.isEmpty()) {
      target.clear();
    }

    if (!isEmpty(source)) {
      target.addAll(source);
    }
  }

  public static <K, V> void overwrite(Map<K, V> target, Map<K, V> source) {
    Assert.notNull(target);
    if (!target.isEmpty()) {
      target.clear();
    }

    if (!isEmpty(source)) {
      target.putAll(source);
    }
  }

  /**
   * Pads left with a specified symbol {@code z}, for length of {@code n - s.length}. If the String
   * is longer than {@code n}, it returns the original String.
   *
   * @param s a String to pad left
   * @param n specifies the length
   * @param z character to use for padding
   * @return returns a left padded String with a specified {@code z} character.
   */
  public static String padLeft(String s, int n, char z) {
    if (s == null) {
      return null;
    } else if (n <= 0) {
      return BeeConst.STRING_EMPTY;
    } else if (s.length() >= n) {
      return s;
    } else {
      return replicate(z, n - s.length()) + s;
    }
  }

  /**
   * Transforms an Object to a String and parenthesizes it.
   *
   * @param x object to parenthesize
   * @return a parenthesized String from both sides.
   */
  public static String parenthesize(Object x) {
    String s = transform(x);

    if (s.isEmpty()) {
      return BeeConst.STRING_EMPTY;
    } else {
      return BeeConst.STRING_LEFT_PARENTHESIS + s + BeeConst.STRING_RIGHT_PARENTHESIS;
    }
  }

  public static String parseDigits(String input) {
    if (isEmpty(input)) {
      return BeeConst.STRING_EMPTY;
    } else {
      return CharMatcher.inRange(BeeConst.CHAR_ZERO, BeeConst.CHAR_NINE).retainFrom(input);
    }
  }

  public static <T> T peek(Iterable<T> container) {
    if (container == null) {
      return null;
    }
    for (T element : container) {
      return element;
    }
    return null;
  }

  public static Double percent(Double d, Double p) {
    if (isDouble(d) && isDouble(p)) {
      return d * p / BeeConst.DOUBLE_ONE_HUNDRED;
    } else {
      return null;
    }
  }

  public static int percent(int x, Double p) {
    if (x != 0 && isDouble(p)) {
      return round(x * p / BeeConst.DOUBLE_ONE_HUNDRED);
    } else {
      return 0;
    }
  }

  public static Double percentInclusive(Double d, Double p) {
    if (isDouble(d) && isPositive(p)) {
      return d * p / (p + BeeConst.DOUBLE_ONE_HUNDRED);
    } else {
      return null;
    }
  }

  public static Double plusPercent(Double d, Double p) {
    if (isDouble(d) && isDouble(p)) {
      return d + d * p / BeeConst.DOUBLE_ONE_HUNDRED;
    } else {
      return d;
    }
  }

  public static int plusPercent(int x, Double p) {
    if (x != 0 && isDouble(p)) {
      return x + round(x * p / BeeConst.DOUBLE_ONE_HUNDRED);
    } else {
      return x;
    }
  }

  public static int positive(Integer x, int def) {
    if (isPositive(x)) {
      return x;
    } else {
      return def;
    }
  }

  public static Long positive(Long x, Long def) {
    return isPositive(x) ? x : def;
  }

  public static Double positive(Double x, Double def) {
    return isPositive(x) ? x : def;
  }

  /**
   * Shows how much of the progress is done, separated by the default progress separator.
   * <p>
   * E.g. the current position is 5 of a total 10. Method returns "5/10".
   * </p>
   *
   * @param pos current position
   * @param tot total count of positions
   * @return a String representation of the progress.
   */
  public static String progress(int pos, int tot) {
    return Integer.toString(pos) + BeeConst.DEFAULT_PROGRESS_SEPARATOR + Integer.toString(tot);
  }

  public static String progress(int pos, int tot, String message) {
    return joinWords(progress(pos, tot), message);
  }

  public static String proper(String s) {
    return proper(s, null);
  }

  /**
   * Separates the String {@code s} to an array with a specified separator. Capitalizes each array
   * element's first letter and converts other to lower-case.
   * <p>
   * E.g the separator is {@code '.'}: {@code "this.IS.a.string"} is formed to
   * {@code "This Is A String"}.
   *
   * @param s the String to form
   * @param separator used for separating {@code s}
   * @return a new formed String
   */
  public static String proper(String s, Character separator) {
    if (isEmpty(s)) {
      return BeeConst.STRING_EMPTY;
    }

    int len = s.trim().length();
    if (len <= 0) {
      return BeeConst.STRING_EMPTY;
    }
    if (len == 1) {
      return s.trim().toUpperCase();
    }

    if (separator == null) {
      return s.trim().substring(0, 1).toUpperCase() + s.trim().substring(1).toLowerCase();
    }

    String[] arr = split(s.trim(), separator);
    StringBuilder z = new StringBuilder();

    for (String x : arr) {
      if (z.length() > 0) {
        z.append(BeeConst.CHAR_SPACE);
      }
      z.append(proper(x, null));
    }
    return z.toString();
  }

  public static String quote(String v) {
    if (v == null) {
      return BeeConst.STRING_QUOT + BeeConst.STRING_QUOT;

    } else if (v.contains(BeeConst.STRING_QUOT)) {
      if (v.contains(BeeConst.STRING_APOS)) {
        return bracket(v.trim());
      } else {
        return BeeConst.STRING_APOS + v.trim() + BeeConst.STRING_APOS;
      }

    } else {
      return BeeConst.STRING_QUOT + v.trim() + BeeConst.STRING_QUOT;
    }
  }

  public static char randomChar(char min, char max) {
    return (char) randomInt(min, max);
  }

  public static double randomDouble(double min, double max) {
    Assert.isTrue(max > min);
    return min + Math.random() * (max - min);
  }

  /**
   * Generates a random Integer value in range of the specified {@code min} and {@code max} values.
   *
   * @param min the minimum value
   * @param max the maximum value
   * @return a generated Integer value
   */
  public static int randomInt(int min, int max) {
    Assert.isTrue(max > min);
    Double z = Math.floor(Math.random() * (max - min));
    return min + z.intValue();
  }

  /**
   * Generates a random Long value in range of the specified {@code min} and {@code max} values.
   *
   * @param min the minimum value
   * @param max the maximum value
   * @return a generated value
   */
  public static long randomLong(long min, long max) {
    Assert.isTrue(max > min);
    Double z = Math.floor(Math.random() * (max - min));
    return min + z.longValue();
  }

  public static String randomString(int len) {
    return randomString(len, len, 'a', 'z');
  }

  /**
   * Generates a random String with a specified length from the given characters.
   *
   * @param len length to generate.
   * @param characters characters to use for generating a new string
   * @return a generated random String
   */
  public static String randomString(int len, CharSequence characters) {
    Assert.isPositive(len);
    Assert.notNull(characters);
    int cnt = characters.length();
    Assert.isPositive(cnt);
    if (cnt == 1) {
      return replicate(characters.charAt(0), len);
    }

    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      sb.append(characters.charAt(randomInt(0, cnt)));
    }
    return sb.toString();
  }

  /**
   * Generates a random String with a specified length from the given characters.
   *
   * @param minLen the minimum length of the string
   * @param maxLen the maximum length of the string
   * @param minChar the minimum character to use for generation
   * @param maxChar the maximum character to use for generation
   * @return a generated random String
   */
  public static String randomString(int minLen, int maxLen, char minChar, char maxChar) {
    int len;
    int x = (minLen > 0) ? minLen : 1;
    int y = (maxLen >= x) ? maxLen : x + 20;

    if (x == y) {
      len = minLen;
    } else {
      len = randomInt(x, y + 1);
    }

    x = minChar;
    y = (maxChar >= x) ? maxChar : Math.min(x + 30, Character.MAX_VALUE);

    if (x == y) {
      return replicate(minChar, len);
    }

    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      sb.append((char) randomInt(x, y + 1));
    }
    return sb.toString();
  }

  public static String remove(String str, char ch) {
    if (str == null || str.indexOf(ch) < 0) {
      return str;
    }

    char[] arr = str.toCharArray();
    int cnt = 0;
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] != ch) {
        if (cnt < i) {
          arr[cnt] = arr[i];
        }
        cnt++;
      }
    }

    if (cnt > 0) {
      return new String(arr, 0, cnt);
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  /**
   * Removes the specified prefix from a String.
   *
   * @param str a value to remove the prefix from
   * @param pfx prefix to remove
   * @return a value with the prefix removed
   */
  public static String removePrefix(String str, char pfx) {
    if (isPrefix(str, pfx)) {
      return removePrefix(str.substring(1), pfx);
    }
    return str;
  }

  public static String removePrefix(String str, String pfx) {
    if (isPrefix(str, pfx)) {
      return str.substring(pfx.length());
    }
    return str;
  }

  /**
   * Removes specified prefix and suffix from a String.
   *
   * @param str a value to remove the prefix and suffix from
   * @param ch a prefix and suffix to remove
   * @return a String value with prefix and suffix removed.
   */
  public static String removePrefixAndSuffix(String str, char ch) {
    return removeSuffix(removePrefix(str, ch), ch);
  }

  /**
   * Removes the specified suffix from a String.
   *
   * @param str a value to remove the prefix from
   * @param sfx prefix to remove
   * @return a value with the suffix removed
   */
  public static String removeSuffix(String str, char sfx) {
    if (isSuffix(str, sfx)) {
      return removeSuffix(str.substring(0, str.length() - 1), sfx);
    }
    return str;
  }

  public static String removeSuffix(String str, String sfx) {
    if (isSuffix(str, sfx)) {
      return left(str, str.length() - sfx.length());
    }
    return str;
  }

  /**
   * Removes all trailing zeros.
   *
   * @param str a value to remove trailing zeros from
   * @return a value with trailing zeros removed
   */
  public static String removeTrailingZeros(String str) {
    if (str == null) {
      return null;
    }
    if (str.length() <= 2) {
      return str;
    }
    if (str.charAt(str.length() - 1) != BeeConst.CHAR_ZERO) {
      return str;
    }

    int p = str.indexOf(BeeConst.CHAR_POINT);
    if (p < 1) {
      return str;
    }
    if (!isDigit(str.substring(p + 1)) || hasExponent(str)) {
      return str;
    }

    int idx = str.length() - 1;
    while (str.charAt(idx - 1) == BeeConst.CHAR_ZERO) {
      idx--;
    }
    if (idx == p + 1) {
      idx--;
    }
    return str.substring(0, idx);
  }

  public static String removeWhiteSpace(String str) {
    if (str == null) {
      return null;
    } else {
      return CharMatcher.whitespace().removeFrom(str);
    }
  }

  /**
   * Replaces the specified part of the {@code src} with a {@code c} value.
   *
   * @param src the source String to replace
   * @param start the start position of the replacement
   * @param end the end position of the replacement
   * @param c a String to replace with
   * @return a new String after the replacement.
   */
  public static String replace(String src, int start, int end, char c) {
    return replace(src, start, end, String.valueOf(c));
  }

  /**
   * Replaces the specified part of the {@code src} with a {@code str} value.
   *
   * @param src the source string to replace
   * @param start the start position of the replacement
   * @param end the end position of the replacement
   * @param str a string to replace with
   * @return a new String after the replacement.
   */
  public static String replace(String src, int start, int end, String str) {
    int len = length(src);
    Assert.isIndex(start, len);
    Assert.isIndex(end - 1, len);
    Assert.notNull(str);

    return new StringBuilder(src).replace(start, end, str).toString();
  }

  /**
   * Replaces all occurrences of {@code search} with {@code replacement}.
   *
   * @param text the source text
   * @param search search phrase to replace
   * @param replacement a replacement for the search phrase
   * @return a String with replaced phrases.
   */
  public static String replace(String text, String search, String replacement) {
    return replace(text, search, replacement, -1, true);
  }

  /**
   * Replaces the specified number of occurrences of {@code search} with {@code replacement}.
   *
   * @param text the source text
   * @param search search phrase to replace
   * @param replacement a replacement for the search phrase
   * @param max the number of occurrences to replace
   * @return a String with replaced phrases.
   */
  public static String replace(String text, String search, String replacement, int max,
      boolean caseSensitive) {

    if (!hasLength(text) || !hasLength(search) || max == 0) {
      return text;
    }

    int start = 0;
    int end = indexOf(text, search, start, caseSensitive);
    if (end < 0) {
      return text;
    }

    int len = search.length();
    StringBuilder sb = new StringBuilder();
    int cnt = max;

    while (end >= 0) {
      sb.append(text.substring(start, end));
      if (hasLength(replacement)) {
        sb.append(replacement);
      }

      start = end + len;
      if (--cnt == 0) {
        break;
      }
      end = indexOf(text, search, start, caseSensitive);
    }
    sb.append(text.substring(start));

    return sb.toString();
  }

  public static String replaceSame(String text, String search, String replacement) {
    return replace(text, search, replacement, -1, false);
  }

  /**
   * Fills a String with value {@code z} for the length of {@code n}.
   *
   * @param z the value to fill with
   * @param n length to fill
   * @return a filled String
   */
  public static String replicate(char z, int n) {
    Assert.isPositive(n);

    char[] arr = new char[n];
    Arrays.fill(arr, z);
    return new String(arr);
  }

  public static double rescale(double x, double frMin, double frMax, double toMin, double toMax) {
    return scaleNormalizedToRange(normalize(x, frMin, frMax), toMin, toMax);
  }

  public static int resize(int x, int frMin, int frMax, int toMin, int toMax) {
    return round(rescale(x, frMin, frMax, toMin, toMax));
  }

  public static String right(String s, int n) {
    if (s == null) {
      return null;
    } else if (n <= 0) {
      return BeeConst.STRING_EMPTY;
    } else if (s.length() <= n) {
      return s;
    } else {
      return s.substring(s.length() - n);
    }
  }

  public static int rotateBackwardExclusive(int x, int min, int max) {
    return rotateBackwardInclusive(x, min, max - 1);
  }

  public static int rotateBackwardInclusive(int x, int min, int max) {
    return (x <= min || x > max) ? max : x - 1;
  }

  public static int rotateForwardExclusive(int x, int min, int max) {
    return rotateForwardInclusive(x, min, max - 1);
  }

  public static int rotateForwardInclusive(int x, int min, int max) {
    return (x < min || x >= max) ? min : x + 1;
  }

  /**
   * Rounds a double value to closest int value.
   *
   * @param x double value that should be rounded to an int.
   * @return integer value that will be rounded from double.
   */
  public static int round(double x) {
    return toInt(Math.round(x));
  }

  /**
   * Rounds {@code x} with a specified scale {@code dec}.
   *
   * @param x a value to round
   * @param dec rounding scale
   * @return a rounded value.
   */
  public static double round(double x, int dec) {
    Assert.isScale(dec);
    if (Double.isInfinite(x) || Double.isNaN(x)) {
      return BeeConst.DOUBLE_ZERO;
    }

    if (dec == 0) {
      if (x > BeeConst.DOUBLE_ZERO) {
        return Math.floor(x) + Math.round(x - Math.floor(x));
      }
      if (x < BeeConst.DOUBLE_ZERO) {
        return Math.ceil(x) + Math.round(x - Math.ceil(x));
      }
      return BeeConst.DOUBLE_ZERO;
    }

    double z = Math.pow(10, dec);
    double y = x * z;
    if (isLong(y)) {
      return Math.round(y) / z;
    }

    if (x > BeeConst.DOUBLE_ZERO) {
      return Math.floor(x) + round(x - Math.floor(x), dec);
    }
    if (x < BeeConst.DOUBLE_ZERO) {
      return Math.ceil(x) + round(x - Math.ceil(x), dec);
    }
    return BeeConst.DOUBLE_ZERO;
  }

  public static String round(String s, int dec) {
    if (!isDouble(s)) {
      return null;

    } else if (dec <= 0 || !isLong(Math.pow(10, dec))) {
      double d = round(toDouble(s), dec);
      if (dec == 0 && isLong(d)) {
        return toString(toLong(d));
      } else {
        return toString(d);
      }

    } else {
      long scale = 1;
      for (int i = 0; i < dec; i++) {
        scale *= 10;
      }

      double d = round(toDouble(s) * scale, 0);
      if (isLong(d)) {
        long x = toLong(d);
        StringBuilder sb = new StringBuilder();

        if (x < 0) {
          sb.append(BeeConst.CHAR_MINUS);
        }
        sb.append(Math.abs(x) / scale).append(BeeConst.STRING_POINT);
        sb.append(padLeft(Long.toString(Math.abs(x) % scale), dec, BeeConst.CHAR_ZERO));
        return sb.toString();

      } else {
        return toString(round(toDouble(s), dec));
      }
    }
  }

  public static boolean same(char c1, char c2) {
    if (c1 == c2) {
      return true;
    }
    return Character.toLowerCase(c1) == Character.toLowerCase(c2);
  }

  /**
   * Checks if {@code s1} and {@code s2} are same. Note: values are trimmed, case is ignored.
   *
   * @param s1 value to check
   * @param s2 value to check
   * @return true if values are same, otherwise false.
   */
  public static boolean same(String s1, String s2) {
    if (isEmpty(s1)) {
      return isEmpty(s2);
    }
    if (isEmpty(s2)) {
      return isEmpty(s1);
    }
    return s1.trim().equalsIgnoreCase(s2.trim());
  }

  public static <T> boolean sameElements(Collection<T> c1, Collection<T> c2) {
    if (isEmpty(c1)) {
      return isEmpty(c2);
    } else if (isEmpty(c2)) {
      return isEmpty(c1);
    } else {
      return c1.containsAll(c2) && c2.containsAll(c1);
    }
  }

  public static <K, V> boolean sameEntries(Map<K, V> m1, Map<K, V> m2) {
    if (isEmpty(m1)) {
      return isEmpty(m2);
    } else if (isEmpty(m2)) {
      return isEmpty(m1);
    } else {
      return m1.entrySet().equals(m2.entrySet());
    }
  }

  public static boolean sameSign(int i1, int i2) {
    return Integer.signum(i1) == Integer.signum(i2);
  }

  public static double scaleNormalizedToRange(double x, double min, double max) {
    double z;

    if (min == max) {
      z = min;
    } else if (x <= 0) {
      z = min;
    } else if (x >= 1) {
      z = max;
    } else if (min > max) {
      z = (1 - x) * (min - max) + max;
    } else {
      z = x * (max - min) + min;
    }
    return z;
  }

  public static int size(Collection<?> col) {
    return (col == null) ? 0 : col.size();
  }

  public static int size(Map<?, ?> map) {
    return (map == null) ? 0 : map.size();
  }

  public static int snap(int x, int to) {
    if (x == 0 || to <= 1) {
      return x;
    } else {
      return round((double) x / to) * to;
    }
  }

  public static <T extends Comparable<? super T>> void sort(List<T> list) {
    Assert.notNull(list);
    if (list.size() > 1) {
      Collections.sort(list);
    }
  }

  /**
   * Creates a String with defined length of spaces.
   *
   * @param l spaces count
   * @return a created String.
   */
  public static String space(int l) {
    if (l == 1) {
      return BeeConst.STRING_SPACE;
    } else if (l < 1) {
      return BeeConst.STRING_EMPTY;
    } else {
      return replicate(BeeConst.CHAR_SPACE, l);
    }
  }

  /**
   * Splits {@code str} with a specified separator {@code separator}.
   *
   * @param str a string to split
   * @param separator a separator used for splitting
   * @return a String array splitted using the separator.
   */
  public static String[] split(String str, char separator) {
    if (str == null) {
      return null;
    }
    int len = str.length();
    if (len == 0) {
      return BeeConst.EMPTY_STRING_ARRAY;
    }

    if (str.indexOf(separator) < 0) {
      return new String[] {str.trim()};
    }

    Splitter splitter = Splitter.on(separator).omitEmptyStrings().trimResults();
    List<String> lst = Lists.newArrayList(splitter.split(str));

    return ArrayUtils.toArray(lst);
  }

  /**
   * Checks if {@code s1} and {@code s2} starts the same. The shorter String is compared with the
   * start of the longer String.
   *
   * @param s1 the first String for comparing
   * @param s2 the second String for comparing
   * @return true if {@code s1} and {@code s2} starts the same, otherwise false.
   */
  public static boolean startsSame(String s1, String s2) {
    if (isEmpty(s1) || isEmpty(s2)) {
      return false;
    }

    int len = Math.min(s1.trim().length(), s2.trim().length());
    if (len > 0) {
      return s1.trim().substring(0, len).equalsIgnoreCase(s2.trim().substring(0, len));
    } else {
      return false;
    }
  }

  public static boolean startsSame(String x, String first, String second, String... rest) {
    if (isEmpty(x)) {
      return false;
    }
    if (startsSame(x, first)) {
      return true;
    }
    if (startsSame(x, second)) {
      return true;
    }

    if (rest == null) {
      return false;
    }
    for (String y : rest) {
      if (startsSame(x, y)) {
        return true;
      }
    }
    return false;
  }

  public static boolean startsWith(String s, char c) {
    if (s == null || s.trim().length() <= 0) {
      return false;
    }
    return same(s.trim().charAt(0), c);
  }

  public static boolean startsWith(String str, String pfx) {
    if (isEmpty(str) || isEmpty(pfx)) {
      return false;
    }

    int len = pfx.trim().length();
    if (len > 0 && len <= str.trim().length()) {
      return str.trim().substring(0, len).equalsIgnoreCase(pfx.trim());
    } else {
      return false;
    }
  }

  public static double sum(Collection<Double> col) {
    double result = BeeConst.DOUBLE_ZERO;
    if (col == null) {
      return result;
    }

    for (Double item : col) {
      if (isDouble(item)) {
        result += item;
      }
    }
    return result;
  }

  public static boolean toBoolean(int x) {
    return x == BeeConst.INT_TRUE;
  }

  public static boolean toBoolean(String s) {
    if (isEmpty(s)) {
      return false;
    }
    return BeeConst.isTrue(s);
  }

  public static Boolean toBooleanOrNull(String s) {
    if (isEmpty(s)) {
      return null;
    }
    return toBoolean(s);
  }

  public static char toChar(int x) {
    if (x < Character.MIN_VALUE) {
      return Character.MIN_VALUE;
    }
    if (x >= Character.MAX_VALUE) {
      return Character.MAX_VALUE;
    }
    return (char) x;
  }

  public static BigDecimal toDecimalOrNull(Double x) {
    if (x == null) {
      return null;
    } else {
      return BigDecimal.valueOf(x);
    }
  }

  public static BigDecimal toDecimalOrNull(Integer x) {
    if (x == null) {
      return null;
    } else {
      return BigDecimal.valueOf(x);
    }
  }

  public static BigDecimal toDecimalOrNull(Long x) {
    if (x == null) {
      return null;
    } else {
      return BigDecimal.valueOf(x);
    }
  }

  public static BigDecimal toDecimalOrNull(String s) {
    if (isEmpty(s)) {
      return null;
    }
    BigDecimal d;

    try {
      d = new BigDecimal(s.trim());
    } catch (NumberFormatException ex) {
      d = null;
    }
    return d;
  }

  /**
   * Converts a String value {@code s} to Double.
   *
   * @param s a string to convert
   * @return a corresponding double value
   * @throws NumberFormatException ex
   */
  public static double toDouble(String s) {
    if (!hasDigit(s)) {
      return BeeConst.DOUBLE_ZERO;
    }
    double d;

    try {
      d = Double.parseDouble(s.trim());
    } catch (NumberFormatException ex) {
      d = BeeConst.DOUBLE_ZERO;
    }
    return d;
  }

  public static Double toDoubleOrNull(String s) {
    if (!hasDigit(s)) {
      return null;
    }

    try {
      return Double.parseDouble(s.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  /**
   * Converts {@code b} to Integer.
   *
   * @param b value to convert
   * @return 1 if b is true, otherwise 0.
   */
  public static int toInt(boolean b) {
    return b ? BeeConst.INT_TRUE : BeeConst.INT_FALSE;
  }

  /**
   * Rounds provided Double {@code d} value to an integer.
   *
   * @param d Double that should be rounded to an int.
   * @return int value of the Double or 0 if not Double provided.
   */
  public static int toInt(Double d) {
    return isDouble(d) ? round(d) : 0;
  }

  public static int toInt(long x) {
    if (x <= Integer.MIN_VALUE) {
      return Integer.MIN_VALUE;
    } else if (x >= Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) x;
    }
  }

  /**
   * Converts a String value {@code s} to Integer.
   *
   * @param s value to convert
   * @return a corresponding Integer value
   * @throws NumberFormatException ex
   */
  public static int toInt(String s) {
    if (!hasDigit(s)) {
      return 0;
    }
    int i;

    try {
      i = Integer.parseInt(s.trim());
    } catch (NumberFormatException ex) {
      if (isDouble(s)) {
        i = toInt(toDouble(s));
      } else {
        i = 0;
      }
    }
    return i;
  }

  public static Integer toIntOrNull(String s) {
    if (!hasDigit(s)) {
      return null;
    }

    try {
      return Integer.parseInt(s.trim());
    } catch (NumberFormatException ex) {
      if (isDouble(s)) {
        return toInt(toDouble(s));
      } else {
        return null;
      }
    }
  }

  public static List<Integer> toInts(String input) {
    List<Integer> result = new ArrayList<>();

    if (!isEmpty(input)) {
      for (String s : NUMBER_SPLITTER.split(input)) {
        Integer x = toIntOrNull(s);
        if (x != null) {
          result.add(x);
        }
      }
    }
    return result;
  }

  /**
   * Adds leading zeros to {@code x}. Converts {@code x} to a String and if it's length is less than
   * {@code n} adds them.
   *
   * @param x a value to add leading zeros to
   * @param n length to add zeros
   * @return a String representation of {@code x} with leading zeros.
   */
  public static String toLeadingZeroes(int x, int n) {
    if (x >= 0 && n > 0) {
      return padLeft(toString(x), n, BeeConst.CHAR_ZERO);
    } else {
      return toString(x);
    }
  }

  public static long toLong(Double d) {
    if (!isDouble(d)) {
      return 0L;
    } else if (d <= Long.MIN_VALUE) {
      return Long.MIN_VALUE;
    } else if (d >= Long.MAX_VALUE) {
      return Long.MAX_VALUE;
    } else {
      return Math.round(d);
    }
  }

  /**
   * Converts a String value {@code s} to Long.
   *
   * @param s value to convert
   * @return a corresponding Long value
   * @throws NumberFormatException ex
   */
  public static long toLong(String s) {
    if (!hasDigit(s)) {
      return 0L;
    }
    long x;

    try {
      x = Long.parseLong(s.trim());
    } catch (NumberFormatException ex) {
      if (isDouble(s)) {
        x = toLong(toDouble(s));
      } else {
        x = 0L;
      }
    }
    return x;
  }

  public static Long toLongOrNull(String s) {
    if (!hasDigit(s)) {
      return null;
    }

    try {
      return Long.parseLong(s.trim());
    } catch (NumberFormatException ex) {
      if (isDouble(s)) {
        return toLong(toDouble(s));
      } else {
        return null;
      }
    }
  }

  public static List<Long> toLongs(String input) {
    List<Long> result = new ArrayList<>();

    if (!isEmpty(input)) {
      for (String s : NUMBER_SPLITTER.split(input)) {
        Long x = toLongOrNull(s);
        if (x != null) {
          result.add(x);
        }
      }
    }
    return result;
  }

  public static String toLowerCase(String s) {
    return (s == null) ? BeeConst.STRING_EMPTY : s.toLowerCase();
  }

  public static int toNonNegativeInt(Double d) {
    return toNonNegativeInt(toInt(d));
  }

  public static int toNonNegativeInt(Integer x) {
    if (x == null) {
      return 0;
    }
    return Math.max(x, 0);
  }

  /**
   * Converts a Boolean value {@code b} to a String value.
   *
   * @param b value to convert
   * @return a String representation of {@code b}
   */
  public static String toString(boolean b) {
    return b ? BeeConst.STRING_TRUE : BeeConst.STRING_FALSE;
  }

  public static String toString(char c) {
    return String.valueOf(c);
  }

  /**
   * Converts a Double value {@code x} to a String value. Removes trailing zeroes.
   *
   * @param x value to convert
   * @return a String representation of {@code x}
   */
  public static String toString(double x) {
    if (Double.isNaN(x) || Double.isInfinite(x) || Math.abs(x) > 1e15) {
      return Double.toString(x);

    } else if (Math.abs(x) < 1e-15) {
      return BeeConst.STRING_ZERO;

    } else if (x % 1 == BeeConst.DOUBLE_ZERO) {
      return Long.toString((long) x);

    } else {
      BigDecimal b = new BigDecimal(x, DEFAULT_MATH_CONTEXT);
      return removeTrailingZeros(b.toPlainString());
    }
  }

  public static String toString(double x, int maxDec) {
    String s = toString(x);
    if (maxDec >= 0 && getDecimals(s) > maxDec && !hasExponent(s)) {
      return removeTrailingZeros(round(s, maxDec));
    } else {
      return s;
    }
  }

  /**
   * Converts an Integer value {@code x} to a String value.
   *
   * @param x value to convert
   * @return String representation of {@code x}
   */
  public static String toString(int x) {
    return Integer.toString(x);
  }

  /**
   * Converts an Long value {@code x} to a String value.
   *
   * @param x value to convert
   * @return String representation of {@code x}
   */
  public static String toString(long x) {
    return Long.toString(x);
  }

  public static String toStringOrNull(Double x) {
    return isDouble(x) ? toString(x) : null;
  }

  public static String toStringOrNull(Integer x) {
    return (x == null) ? null : toString(x);
  }

  public static String toStringOrNull(Long x) {
    return (x == null) ? null : toString(x);
  }

  public static String trim(String s) {
    return (s == null) ? BeeConst.STRING_EMPTY : s.trim();
  }

  public static String trimLeft(String s) {
    if (s == null || s.isEmpty()) {
      return BeeConst.STRING_EMPTY;
    }

    int len = s.length();
    int pos = 0;

    while (pos < len && isWhitespace(s.charAt(pos))) {
      pos++;
    }

    if (pos >= len) {
      return BeeConst.STRING_EMPTY;
    } else if (pos > 0) {
      return s.substring(pos);
    } else {
      return s;
    }
  }

  public static String trimRight(String s) {
    if (s == null || s.isEmpty()) {
      return BeeConst.STRING_EMPTY;
    }

    int len = s.length();
    int pos = len;

    while (pos > 0 && isWhitespace(s.charAt(pos - 1))) {
      pos--;
    }

    if (pos <= 0) {
      return BeeConst.STRING_EMPTY;
    } else if (pos < len) {
      return s.substring(0, pos);
    } else {
      return s;
    }
  }

  /**
   * Null-safe Boolean unboxing.
   *
   * @param box an Boolean to unbox
   * @return unboxed boolean value or false if {@code box} is null
   */
  public static boolean unbox(Boolean box) {
    return (box == null) ? false : box;
  }

  /**
   * Null-safe Double unboxing.
   *
   * @param box an Double to unbox
   * @return unboxed double value or 0 if {@code box} is null
   */
  public static double unbox(Double box) {
    return (box == null) ? BeeConst.DOUBLE_ZERO : box;
  }

  public static double unbox(Number box) {
    return (box == null) ? BeeConst.DOUBLE_ZERO : box.doubleValue();
  }

  /**
   * Null-safe Integer unboxing.
   *
   * @param box an Integer to unbox
   * @return unboxed int value or 0 if {@code box} is null
   */
  public static int unbox(Integer box) {
    return (box == null) ? 0 : box;
  }

  /**
   * Null-safe Long unboxing.
   *
   * @param box an Long to unbox
   * @return unboxed long value or 0 if {@code box} is null
   */
  public static long unbox(Long box) {
    return (box == null) ? 0L : box;
  }

  /**
   * Null-safe collection union.
   */
  public static <T> Set<T> union(Collection<? extends T> col1, Collection<? extends T> col2) {
    Set<T> result = new HashSet<>();

    if (col1 != null) {
      result.addAll(col1);
    }
    if (col2 != null) {
      result.addAll(col2);
    }
    return result;
  }

  public static <T> Set<T> union(Collection<? extends T> col1, Collection<? extends T> col2,
      Collection<? extends T> col3) {
    Set<T> result = new HashSet<>();

    if (col1 != null) {
      result.addAll(col1);
    }
    if (col2 != null) {
      result.addAll(col2);
    }
    if (col3 != null) {
      result.addAll(col3);
    }
    return result;
  }

  public static String unquote(String s) {
    if (isQuoted(s)) {
      return s.substring(1, s.length() - 1);
    } else {
      return s;
    }
  }

  /**
   * Searches for an Integer value from a String {@code s}.
   *
   * @param s the String to search value from
   * @return an Integer value if found, otherwise 0;
   */
  public static int val(String s, boolean extract) {
    if (s == null) {
      return 0;
    }
    int len = s.length();
    if (len <= 0) {
      return 0;
    }

    int start = 0;

    if (extract) {
      while (start < len) {
        if (isDigit(s.charAt(start))) {
          break;
        } else if (s.charAt(start) == BeeConst.CHAR_MINUS) {
          if (start < len - 1 && isDigit(s.charAt(start + 1))) {
            break;
          }
        }

        start++;
      }

    } else {
      while (start < len && s.charAt(start) <= BeeConst.CHAR_SPACE) {
        start++;
      }
    }

    if (start >= len) {
      return 0;
    }

    int end = start;
    if (s.charAt(start) == BeeConst.CHAR_MINUS) {
      if (len <= start + 1 || !isDigit(s.charAt(start + 1))) {
        return 0;
      }
      end++;
    }

    while (end < len && isDigit(s.charAt(end))) {
      end++;
    }
    if (end <= start) {
      return 0;
    }

    return toInt(s.substring(start, end));
  }

  /**
   * Transforms an Object {@code x} to a String representation. In general, this method returns a
   * string that "textually represents" this object. String type Objects are trimmed.
   *
   * @param x value to transform.
   * @return a string that "textually represents" this object.
   */
  static String transform(Object x) {
    if (x == null) {
      return BeeConst.STRING_EMPTY;

    } else if (x instanceof String) {
      return ((String) x).trim();

    } else if (ArrayUtils.isArray(x)) {
      return ArrayUtils.toString(x);

    } else if (x instanceof Collection) {
      return isEmpty((Collection<?>) x) ? BeeConst.STRING_EMPTY : x.toString();

    } else if (x instanceof Map) {
      return isEmpty((Map<?, ?>) x) ? BeeConst.STRING_EMPTY : x.toString();

    } else {
      return x.toString();
    }
  }

  private static String doJoin(boolean allowDuplicates, String sep, Object first, Object second,
      Object... rest) {
    Assert.notNull(sep);

    StringBuilder sb = new StringBuilder();
    sb.append(transform(first));

    String s = transform(second);
    if (!s.isEmpty() && (allowDuplicates || !containsSame(sb.toString(), s))) {
      if (sb.length() > 0 && !sep.isEmpty()) {
        sb.append(sep);
      }
      sb.append(s);
    }

    if (rest != null) {
      for (Object x : rest) {
        s = transform(x);
        if (!s.isEmpty() && (allowDuplicates || !containsSame(sb.toString(), s))) {
          if (sb.length() > 0 && !sep.isEmpty()) {
            sb.append(sep);
          }
          sb.append(s);
        }
      }
    }
    return sb.toString();
  }

  private BeeUtils() {
  }
}
