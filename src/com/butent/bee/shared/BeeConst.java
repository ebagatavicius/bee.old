package com.butent.bee.shared;

import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.IntSupplier;

/**
 * Stores all default values (databases, boolean, separators etc.).
 */
public final class BeeConst {

  public enum SqlEngine {
    POSTGRESQL("PostgreSQL"), MSSQL("Microsoft SQL Server"), ORACLE("Oracle"), GENERIC(null);

    public static SqlEngine detectEngine(String expr) {
      if (!BeeUtils.isEmpty(expr)) {
        for (SqlEngine engine : SqlEngine.values()) {
          if (BeeUtils.same(expr, engine.alias)) {
            return engine;
          }
        }
      }
      return GENERIC;
    }

    private String alias;

    SqlEngine(String alias) {
      if (BeeUtils.isEmpty(alias)) {
        this.alias = name();
      } else {
        this.alias = alias;
      }
    }
  }

  public static final String NO = "no";
  public static final String YES = "yes";

  public static final String OFF = "off";
  public static final String ON = "on";

  public static final String UNKNOWN = "unknown";
  public static final String EMPTY = "empty";
  public static final String DEFAULT = "default";
  public static final String ERROR = "error";
  public static final String NULL = "null";
  public static final String ALL = "all";
  public static final String NONE = "none";

  public static final String DEFAULT_LIST_SEPARATOR = ", ";
  public static final String DEFAULT_VALUE_SEPARATOR = "=";
  public static final String DEFAULT_OPTION_SEPARATOR = ", ";
  public static final String DEFAULT_ROW_SEPARATOR = ";";
  public static final String DEFAULT_PROPERTY_SEPARATOR = ".";
  public static final String DEFAULT_PROGRESS_SEPARATOR = "/";

  public static final String ELLIPSIS = "...";
  public static final String DIGITS = "0123456789";

  public static final String STRING_EMPTY = "";
  public static final String STRING_SPACE = " ";
  public static final String STRING_ZERO = "0";
  public static final String STRING_ONE = "1";
  public static final String STRING_COMMA = ",";
  public static final String STRING_POINT = ".";
  public static final String STRING_LEFT_BRACKET = "[";
  public static final String STRING_RIGHT_BRACKET = "]";
  public static final String STRING_LEFT_PARENTHESIS = "(";
  public static final String STRING_RIGHT_PARENTHESIS = ")";
  public static final String STRING_LEFT_BRACE = "{";
  public static final String STRING_RIGHT_BRACE = "}";
  public static final String STRING_EQ = "=";
  public static final String STRING_LT = "<";
  public static final String STRING_GT = ">";
  public static final String STRING_CR = "\r";
  public static final String STRING_EOL = "\n";
  public static final String STRING_MINUS = "-";
  public static final String STRING_PLUS = "+";
  public static final String STRING_ASTERISK = "*";
  public static final String STRING_APOS = "'";
  public static final String STRING_QUOT = "\"";
  public static final String STRING_UNDER = "_";
  public static final String STRING_COLON = ":";
  public static final String STRING_PERCENT = "%";
  public static final String STRING_NBSP = "\u00a0";
  public static final String STRING_SLASH = "/";
  public static final String STRING_QUESTION = "?";
  public static final String STRING_EXCLAMATION = "!";
  public static final String STRING_NUMBER_SIGN = "#";
  public static final String STRING_CHECK_MARK = "\u2713";
  public static final String STRING_LEFT_ARROW = "\u2190";
  public static final String STRING_RIGHT_ARROW = "\u2192";

  public static final String STRING_FALSE = Boolean.toString(false);
  public static final String STRING_TRUE = Boolean.toString(true);

  public static final int UNDEF = -1;

  public static final int INT_ERROR = -1;
  public static final int INT_FALSE = 0;
  public static final int INT_TRUE = 1;

  public static final char CHAR_SPACE = ' ';
  public static final char CHAR_UNDER = '_';
  public static final char CHAR_ZERO = '0';
  public static final char CHAR_ONE = '1';
  public static final char CHAR_TWO = '2';
  public static final char CHAR_THREE = '3';
  public static final char CHAR_FOUR = '4';
  public static final char CHAR_FIVE = '5';
  public static final char CHAR_SIX = '6';
  public static final char CHAR_SEVEN = '7';
  public static final char CHAR_EIGHT = '8';
  public static final char CHAR_NINE = '9';
  public static final char CHAR_POINT = '.';
  public static final char CHAR_COMMA = ',';
  public static final char CHAR_EOL = '\n';
  public static final char CHAR_CR = '\r';
  public static final char CHAR_EQ = '=';
  public static final char CHAR_AMP = '&';
  public static final char CHAR_LT = '<';
  public static final char CHAR_GT = '>';
  public static final char CHAR_QUOT = '"';
  public static final char CHAR_APOS = '\'';
  public static final char CHAR_MINUS = '-';
  public static final char CHAR_PLUS = '+';
  public static final char CHAR_NBSP = '\u00a0';
  public static final char CHAR_COLON = ':';
  public static final char CHAR_SEMICOLON = ';';
  public static final char CHAR_ASTERISK = '*';
  public static final char CHAR_SLASH = '/';
  public static final char CHAR_BACKSLASH = '\\';
  public static final char CHAR_TIMES = '\u00d7';
  public static final char CHAR_QUESTION = '?';
  public static final char CHAR_AT = '@';
  public static final char CHAR_PLUS_MINUS = '\u00b1';

  public static final char DRILL_DOWN = '\u25ba';
  public static final char DROP_DOWN = '\u25bc';

  public static final String CHAR_FALSE = "fFnN0-";
  public static final String CHAR_TRUE = "tTyY1+";

  public static final int COMPARE_UNKNOWN = -2;
  public static final int COMPARE_LESS = -1;
  public static final int COMPARE_EQUAL = 0;
  public static final int COMPARE_MORE = 1;

  public static final String CHARSET_UTF8 = "UTF-8";

  public static final String SERVER = "server";
  public static final String CLIENT = "client";

  public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
  public static final String[] EMPTY_STRING_ARRAY = new String[0];
  public static final int[] EMPTY_INT_ARRAY = new int[0];

  public static final String HTML_NBSP = "&nbsp;";

  public static final int MAX_SCALE = 20;

  public static final long LONG_UNDEF = -1L;

  public static final double DOUBLE_UNDEF = -1.0d;
  public static final double DOUBLE_ZERO = 0.0d;
  public static final double DOUBLE_ONE = 1.0d;
  public static final double DOUBLE_ONE_HUNDRED = 100.0d;

  public static final String YEAR = "Year";
  public static final String MONTH = "Month";

  public static final String CSS_CLASS_PREFIX = "bee-";

  public static final Set<String> EMPTY_IMMUTABLE_STRING_SET = Collections.emptySet();
  public static final List<String> EMPTY_IMMUTABLE_STRING_LIST = Collections.emptyList();

  public static final Set<Long> EMPTY_IMMUTABLE_LONG_SET = Collections.emptySet();
  public static final List<Long> EMPTY_IMMUTABLE_LONG_LIST = Collections.emptyList();

  public static final Set<Integer> EMPTY_IMMUTABLE_INT_SET = Collections.emptySet();

  public static final IntSupplier INT_ZERO_SUPPLIER = () -> 0;

  private static String home = SERVER;

  /**
   * Returns the state of client.
   *
   * @return {@code true} if state is client, {@code false} otherwise
   */
  public static boolean isClient() {
    return home.equals(CLIENT);
  }

  /**
   * Returns if parameter {@code s} value equals {@code DEFAULT} constant.
   *
   * @return {@code true} if {@code s} is equals {@code DEFAULT} constant
   */
  public static boolean isDefault(String s) {
    if (s == null) {
      return false;
    } else {
      return s.trim().equalsIgnoreCase(DEFAULT);
    }
  }

  /**
   * Returns is {@code x} the value of error code.
   *
   * @param x error code
   * @return {@code true} if {@code x} is value of error code.
   */
  public static boolean isError(int x) {
    return x == INT_ERROR;
  }

  /**
   * Returns is {@code c} the boolean value of {@code false}. There are {@code 'f', 'F', 'n',
   * 'N', '0'} values of character witch returns {@code true}
   *
   * @param c character of boolean value
   * @return {@code true} if character {@code c} is the boolean value of {@code false}
   */
  public static boolean isFalse(char c) {
    return CHAR_FALSE.indexOf(c) >= 0;
  }

  public static boolean isFalse(String s) {
    if (s == null) {
      return false;
    } else if (s.trim().length() == 1) {
      return isFalse(s.trim().charAt(0));
    } else {
      return s.trim().toLowerCase().equals(STRING_FALSE.toLowerCase())
          || s.trim().toLowerCase().equals(NO.toLowerCase());
    }
  }

  public static boolean isOff(String s) {
    if (s == null) {
      return false;
    } else {
      return STRING_MINUS.equals(s.trim())
          || s.trim().toLowerCase().equals(STRING_FALSE.toLowerCase());
    }
  }

  /**
   * Returns the state of server.
   *
   * @return {@code} if state is server
   */
  public static boolean isServer() {
    return home.equals(SERVER);
  }

  /**
   * Returns is (@code c} the boolean value od {@code true}. There are
   * {@code 't', 'T', 'y', 'Y', '1'} values of character witch returns {@code true}
   *
   * @param c character of boolean value
   * @return {@code true} if character {@code c} is the bolean value of {@code true}
   */
  public static boolean isTrue(char c) {
    return CHAR_TRUE.indexOf(c) >= 0;
  }

  public static boolean isTrue(String s) {
    if (s == null) {
      return false;
    } else if (s.trim().length() == 1) {
      return isTrue(s.trim().charAt(0));
    } else {
      return s.trim().toLowerCase().equals(STRING_TRUE.toLowerCase())
          || s.trim().toLowerCase().equals(YES.toLowerCase())
          || s.trim().toLowerCase().equals(ON.toLowerCase());
    }
  }

  public static boolean isUndef(int x) {
    return x == UNDEF;
  }

  public static boolean isUndef(long x) {
    return x == LONG_UNDEF;
  }

  /**
   * Sets the state to client.
   *
   * @see #isClient
   */
  public static void setClient() {
    home = CLIENT;
  }

  /**
   * Sets the state to server.
   *
   * @see #isServer
   */
  public static void setServer() {
    home = SERVER;
  }

  /**
   * Returns value of state.
   *
   * @return value of state
   * @see #isClient()
   * @see #isServer()
   */
  public static String whereAmI() {
    if (isClient()) {
      return CLIENT;
    } else if (isServer()) {
      return SERVER;
    } else {
      return UNKNOWN;
    }
  }

  private BeeConst() {
  }
}
