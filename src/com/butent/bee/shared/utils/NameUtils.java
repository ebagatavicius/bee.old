package com.butent.bee.shared.utils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class NameUtils {

  public static final char QUALIFIED_NAME_SEPARATOR = ':';

  public static final String DEFAULT_NAME_SEPARATOR = ",";

  public static final Splitter NAME_SPLITTER =
      Splitter.on(CharMatcher.anyOf(" ,;")).trimResults().omitEmptyStrings();

  public static final Function<Enum<?>, String> GET_NAME = Enum::name;

  private static int nameCounter;

  public static String addName(String nm, int v) {
    return addName(nm, String.valueOf(v));
  }

  public static String addName(String nm, String v) {
    if (BeeUtils.isEmpty(v)) {
      return BeeConst.STRING_EMPTY;
    } else if (BeeUtils.isEmpty(nm)) {
      return v.trim();
    } else {
      return nm.trim() + BeeConst.DEFAULT_VALUE_SEPARATOR + v.trim();
    }
  }

  public static String camelize(String str, char sep) {
    if (str == null || str.indexOf(sep) < 0) {
      return str;
    }

    char[] arr = str.toCharArray();
    int cnt = 0;
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] != sep) {
        if (cnt < i) {
          arr[cnt] = arr[i];
        }
        cnt++;
      } else if (i < arr.length - 1) {
        arr[i + 1] = Character.toUpperCase(arr[i + 1]);
      }
    }
    if (cnt > 0) {
      return new String(arr, 0, cnt);
    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  /**
   * Created a unique name with a specified prefix.
   * @param pfx prefix used for generating a unique name
   * @return String which contains unique value with a specified prefix.
   */
  public static String createUniqueName(String pfx) {
    nameCounter++;

    if (pfx == null) {
      return BeeUtils.toString(nameCounter);
    } else {
      return pfx.trim() + nameCounter;
    }
  }

  public static String decamelize(String str, char sep) {
    if (BeeUtils.isEmpty(str)) {
      return str;
    }
    if (str.equals(str.toLowerCase())) {
      return str;
    }

    StringBuilder sb = new StringBuilder();
    char ch;

    for (int i = 0; i < str.length(); i++) {
      ch = str.charAt(i);
      if (Character.isUpperCase(ch)) {
        sb.append(sep).append(Character.toLowerCase(ch));
      } else {
        sb.append(ch);
      }
    }
    return sb.toString();
  }

  /**
   * @param cls the class to get a name from
   * @return only the String class name with packages excluded.
   */
  public static String getClassName(Class<?> cls) {
    String name = cls.getName();
    int p = Math.max(name.lastIndexOf('.'), name.lastIndexOf('$'));
    return name.substring(p + 1);
  }

  public static String getLocalPart(String qName) {
    if (BeeUtils.contains(qName, QUALIFIED_NAME_SEPARATOR)) {
      return BeeUtils.getSuffix(qName, QUALIFIED_NAME_SEPARATOR);
    } else {
      return qName;
    }
  }

  public static String getName(Object obj) {
    return getClassName(Assert.notNull(obj).getClass());
  }

  public static String getNamespacePrefix(String qName) {
    return BeeUtils.getPrefix(qName, QUALIFIED_NAME_SEPARATOR);
  }

  public static String getWord(String s, int idx) {
    if (BeeUtils.isEmpty(s) || idx < 0) {
      return null;
    }
    int i = 0;
    for (String w : NAME_SPLITTER.split(s)) {
      if (idx == i++) {
        return w;
      }
    }
    return null;
  }

  /**
   * Checks if a string is a correct identifier. Identifier cannot start with a number, and can only
   * contain "_", digits and letters.
   * @param name the name to check
   * @return true if the String is a correct identifier, false otherwise.
   */
  public static boolean isIdentifier(String name) {
    if (BeeUtils.isEmpty(name)) {
      return false;
    }
    if (Character.isDigit(name.charAt(0))) {
      return false;
    }

    boolean ok = true;
    for (int i = 0; i < name.length(); i++) {
      if (!isIdentifierPart(name.charAt(i))) {
        ok = false;
        break;
      }
    }
    return ok;
  }

  public static boolean isIdentifierPart(char c) {
    return c == BeeConst.CHAR_UNDER || Character.isLetterOrDigit(c);
  }

  public static String join(Collection<String> names) {
    return BeeUtils.join(DEFAULT_NAME_SEPARATOR, names);
  }

  public static String normalizeEnumName(String input) {
    return CharMatcher.javaLetterOrDigit().retainFrom(input).toLowerCase();
  }

  public static List<String> rename(List<String> names, String oldName, String newName) {
    List<String> result = new ArrayList<>();
    if (!BeeUtils.isEmpty(names)) {
      return result;
    }

    if (!BeeUtils.isEmpty(oldName) && !BeeUtils.isEmpty(newName)
        && !oldName.trim().equals(newName.trim())) {

      for (int i = 0; i < names.size(); i++) {
        String name = names.get(i);

        if (BeeUtils.same(name, oldName)) {
          result.add(newName.trim());
        } else {
          result.add(name);
        }
      }

    } else {
      result.addAll(names);
    }

    return result;
  }

  public static String rename(String input, String oldName, String newName) {
    if (BeeUtils.containsSame(input, oldName) && !BeeUtils.isEmpty(newName)
        && !oldName.trim().equals(newName.trim())) {
      return join(rename(toList(input), oldName, newName));
    } else {
      return input;
    }
  }

  public static String replaceName(String input, String search, String replacement) {
    if (BeeUtils.isEmpty(input) || BeeUtils.isEmpty(search) || replacement == null) {
      return input;
    }

    int start = 0;
    int pos = input.indexOf(search, start);
    if (pos < 0) {
      return input;
    }

    int len = search.length();
    StringBuilder sb = new StringBuilder();

    while (pos >= 0) {
      if (pos > start) {
        sb.append(input.substring(start, pos));
      }

      boolean ok = pos == 0 || !isIdentifierPart(input.charAt(pos - 1));
      if (ok && pos + len < input.length()) {
        ok = !isIdentifierPart(input.charAt(pos + len));
      }

      if (ok) {
        sb.append(replacement);
      } else {
        sb.append(search);
      }

      start = pos + len;
      pos = input.indexOf(search, start);
    }

    sb.append(input.substring(start));

    return sb.toString();
  }

  public static List<String> toList(String s) {
    if (BeeUtils.isEmpty(s)) {
      return new ArrayList<>();
    } else {
      return NAME_SPLITTER.splitToList(s);
    }
  }

  public static Set<String> toSet(String s) {
    if (BeeUtils.isEmpty(s)) {
      return new HashSet<>();
    } else {
      return new HashSet<>(NAME_SPLITTER.splitToList(s));
    }
  }

  /**
   * Transforms an Object {@code obj} to a String representation. In general, this method returns a
   * string that "textually represents" a class(class name).
   * @param obj value to transform
   * @return Object's class name
   */
  public static String transformClass(Object obj) {
    if (obj == null) {
      return BeeConst.NULL;
    } else {
      return obj.getClass().getName();
    }
  }

  private NameUtils() {
  }
}
