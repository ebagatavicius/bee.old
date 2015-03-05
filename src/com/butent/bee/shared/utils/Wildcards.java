package com.butent.bee.shared.utils;

import com.butent.bee.shared.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

/**
 * Used for forming search patterns.
 */
public final class Wildcards {
  /**
   * Handles instances of wildcard patterns.
   */

  public static class Pattern {
    private final String expr;
    private final char wildcardAny;
    private final char wildcardOne;
    private final boolean sensitive;

    private final boolean containsWildcards;
    private final String[] tokens;
    private final boolean exact;

    protected Pattern(String expr, char wildcardAny, char wildcardOne, boolean sensitive,
        Character charExact) {
      Assert.notEmpty(expr);
      this.wildcardAny = wildcardAny;
      this.wildcardOne = wildcardOne;
      this.sensitive = sensitive;

      if (expr.indexOf(wildcardAny) >= 0 || expr.indexOf(wildcardOne) >= 0) {
        this.expr = expr.trim();
        this.containsWildcards = true;
        this.tokens = tokenize();
        this.exact = false;
      } else if (charExact != null && BeeUtils.isPrefixOrSuffix(expr, charExact)) {
        this.expr = BeeUtils.removePrefixAndSuffix(expr, charExact);
        this.containsWildcards = false;
        this.tokens = null;
        this.exact = true;
      } else {
        this.expr = expr.trim();
        this.containsWildcards = false;
        this.tokens = null;
        this.exact = false;
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (obj == null) {
        return false;
      } else if (!(obj instanceof Pattern)) {
        return false;
      }

      Pattern other = (Pattern) obj;
      return Objects.equals(expr, other.expr)
          && wildcardAny == other.wildcardAny && wildcardOne == other.wildcardOne
          && sensitive == other.sensitive && exact == other.exact;
    }

    /**
     * @return the current String expression.
     */
    public String getExpr() {
      return expr;
    }

    /**
     * @return current tokens in a String array.
     */
    public String[] getTokens() {
      return tokens;
    }

    /**
     * @return the current ANY wildcard.
     */
    public char getWildcardAny() {
      return wildcardAny;
    }

    /**
     * @return the current ONE wildcard.
     */
    public char getWildcardOne() {
      return wildcardOne;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getExpr(), getWildcardAny(), getWildcardOne(), isSensitive(), isExact());
    }

    /**
     * @return true if an expression has atleast one of the wildcards, otherwise false.
     */
    public boolean hasWildcards() {
      return containsWildcards;
    }

    /**
     * @return true if Exact mode is on, otherwise false.
     */
    public boolean isExact() {
      return exact;
    }

    /**
     * @return true if Sensitive mode is on, otherwise false.
     */
    public boolean isSensitive() {
      return sensitive;
    }

    /**
     * @return a String expression with Sensitive and/or Exact modes included, if on.
     */
    @Override
    public String toString() {
      return BeeUtils.joinWords(expr, sensitive ? "(sensitive)" : "", exact ? "(exact)" : "");
    }

    private String[] tokenize() {
      char[] arr = expr.toCharArray();
      String strAny = String.valueOf(wildcardAny);
      String strOne = String.valueOf(wildcardOne);

      List<String> lst = new ArrayList<>();
      StringBuilder buffer = new StringBuilder();

      int i = 0;
      while (i < arr.length) {
        if (arr[i] == wildcardAny || arr[i] == wildcardOne) {
          if (buffer.length() > 0) {
            lst.add(buffer.toString());
            buffer.setLength(0);
          }

          if (arr[i] == wildcardOne) {
            lst.add(strOne);
          } else {
            for (int j = i + 1; j < arr.length; j++) {
              if (arr[j] == wildcardOne) {
                lst.add(strOne);
                i = j;
              } else if (arr[j] == wildcardAny) {
                i = j;
              } else {
                break;
              }
            }
            lst.add(strAny);
          }
        } else {
          buffer.append(arr[i]);
        }
        i++;
      }
      if (buffer.length() > 0) {
        lst.add(buffer.toString());
      }
      return ArrayUtils.toArray(lst);
    }
  }

  /**
   * Manages default wildcard pattern.
   */

  private static final class DefaultPattern extends Pattern {
    private DefaultPattern(String expr) {
      this(expr, defaultCaseSensitivity);
    }

    private DefaultPattern(String expr, boolean sensitive) {
      this(expr, sensitive, defaultCharExact);
    }

    private DefaultPattern(String expr, boolean sensitive, Character charExact) {
      super(expr, defaultAny, defaultOne, sensitive, charExact);
    }
  }

  /**
   * Enables using file system type wildcard pattern.
   */

  private static final class FsPattern extends Pattern {
    private FsPattern(String expr) {
      this(expr, fsCaseSensitivity);
    }

    private FsPattern(String expr, boolean sensitive) {
      this(expr, sensitive, fsCharExact);
    }

    private FsPattern(String expr, boolean sensitive, Character charExact) {
      super(expr, FS_ANY, FS_ONE, sensitive, charExact);
    }
  }

  /**
   * Enables using sql type wildcard pattern.
   */

  private static final class SqlPattern extends Pattern {
    private SqlPattern(String expr) {
      this(expr, sqlCaseSensitivity);
    }

    private SqlPattern(String expr, boolean sensitive) {
      this(expr, sensitive, sqlCharExact);
    }

    private SqlPattern(String expr, boolean sensitive, Character charExact) {
      super(expr, SQL_ANY, SQL_ONE, sensitive, charExact);
    }
  }

  private static final char SQL_ANY = '%';
  private static final char SQL_ONE = '_';
  private static boolean sqlCaseSensitivity;
  private static Character sqlCharExact;

  private static final char FS_ANY = '*';
  private static final char FS_ONE = '?';
  private static boolean fsCaseSensitivity;
  private static Character fsCharExact;

  private static char defaultAny = FS_ANY;
  private static char defaultOne = FS_ONE;
  private static boolean defaultCaseSensitivity;
  private static Character defaultCharExact;

  public static boolean contains(Collection<? extends Pattern> patterns, String input) {
    if (patterns == null || BeeUtils.isEmpty(input)) {
      return false;
    }

    for (Pattern pattern : patterns) {
      if (isLike(input, pattern)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return the default ANY wildcard.
   */
  public static char getDefaultAny() {
    return defaultAny;
  }

  /**
   * @return teh default ONE wildcard.
   */
  public static char getDefaultOne() {
    return defaultOne;
  }

  /**
   * @param expr the expression for a pattern
   * @return a default Pattern with the specified expression.
   */
  public static Pattern getDefaultPattern(String expr) {
    return new DefaultPattern(expr);
  }

  /**
   * @param expr the expression for a pattern
   * @param sens specifies if Sensitive mode is set
   * @return a default Pattern with the specified expression and Sensitive mode.
   */
  public static Pattern getDefaultPattern(String expr, boolean sens) {
    return new DefaultPattern(expr, sens);
  }

  public static Pattern getDefaultPattern(String expr, boolean sens, Character charExact) {
    return new DefaultPattern(expr, sens, charExact);
  }

  /**
   * @return the current FS ANY wildcard.
   */
  public static char getFsAny() {
    return FS_ANY;
  }

  /**
   * @return the current FS ONE wildcard.
   */
  public static char getFsOne() {
    return FS_ONE;
  }

  /**
   * @param expr an expression used in a FsPattern
   * @return a new FsPattern with the specified expression {@code expr}
   */
  public static Pattern getFsPattern(String expr) {
    return new FsPattern(expr);
  }

  /**
   * @param expr the expression for a pattern
   * @param sens specifies if Sensitive mode is set
   * @return a FsPattern with the specified expression and Sensitive mode.
   */
  public static Pattern getFsPattern(String expr, boolean sens) {
    return new FsPattern(expr, sens);
  }

  public static Pattern getFsPattern(String expr, boolean sens, Character charExact) {
    return new FsPattern(expr, sens, charExact);
  }

  /**
   * @param expr the expression for a pattern
   * @param any the ANY wildcard
   * @param one the ONE wildcard
   * @param sens specifies if Sensitive mode is set
   * @return a Pattern with the specified expression, ONE wilcdard, ANY wildcard and Sensitive mode.
   */
  public static Pattern getPattern(String expr, char any, char one, boolean sens, Character exact) {
    return new Pattern(expr, any, one, sens, exact);
  }

  /**
   * @return the current SQL ANY wildcard.
   */
  public static char getSqlAny() {
    return SQL_ANY;
  }

  /**
   * @return the current SQL ONE wildcard.
   */
  public static char getSqlOne() {
    return SQL_ONE;
  }

  /**
   * @param expr an expression used in a the new SqlPattern
   * @return a new SqlPattern with the specified expression {@code expr}
   */
  public static Pattern getSqlPattern(String expr) {
    return new SqlPattern(expr);
  }

  /**
   * @param expr the expression for a pattern
   * @param sens specifies if Sensitive mode is set
   * @return a SqlPattern with the specified expression and Sensitive mode.
   */
  public static Pattern getSqlPattern(String expr, boolean sens) {
    return new SqlPattern(expr, sens);
  }

  public static Pattern getSqlPattern(String expr, boolean sens, Character charExact) {
    return new SqlPattern(expr, sens, charExact);
  }

  /**
   * Checks if an expression {@code expr} has any of default wildcards.
   * @param expr the expression to check
   * @return true if at least one of the wildcards are found, otherwise false
   */
  public static boolean hasDefaultWildcards(String expr) {
    if (expr == null) {
      return false;
    }
    return expr.indexOf(defaultAny) >= 0 || expr.indexOf(defaultOne) >= 0;
  }

  /**
   * Checks if an expression {@code expr} has any FS wildcards.
   * @param expr the expression to check
   * @return true if at least one of the wildcards are found, otherwise false
   */
  public static boolean hasFsWildcards(String expr) {
    if (expr == null) {
      return false;
    }
    return expr.indexOf(FS_ANY) >= 0 || expr.indexOf(FS_ONE) >= 0;
  }

  /**
   * Checks if an expression {@code expr} has any Sql wildcards.
   * @param expr the expression to check
   * @return true if at least one of the wildcards are found, otherwise false
   */
  public static boolean hasSqlWildcards(String expr) {
    if (expr == null) {
      return false;
    }
    return expr.indexOf(SQL_ANY) >= 0 || expr.indexOf(SQL_ONE) >= 0;
  }

  /**
   * Checks if an expression {@code expr} contains only of the default ANY wildcards.
   * @param expr the expression to check
   * @return true if the expression contain only of default ANY wildcards, otherwise false
   */
  public static boolean isDefaultAny(String expr) {
    return BeeUtils.containsOnly(expr, defaultAny);
  }

  /**
   * @return the defaultCaseSensitivity.
   */
  public static boolean isDefaultCaseSensitive() {
    return defaultCaseSensitivity;
  }

  /**
   * Checks if {@code expr} is a default Pattern.
   * @param expr the expression to check
   * @return true if it's a default Pattern, otherwise false.
   */
  public static boolean isDefaultPattern(String expr) {
    return !BeeUtils.isEmpty(expr) && !isDefaultAny(expr);
  }

  /**
   * Checks if an expression {@code expr} contains only of the FS ANY wildcards.
   * @param expr the expression to check
   * @return true if the expression contain only of FS Any wildcards, otherwise false
   */
  public static boolean isFsAny(String expr) {
    return BeeUtils.containsOnly(expr, FS_ANY);
  }

  /**
   * @return the FsCaseSensitivity.
   */
  public static boolean isFsCaseSensitive() {
    return fsCaseSensitivity;
  }

  /**
   * @param input the input to check
   * @param expr the expression for a FsPattern
   * @return true if the {@code input} matches the Pattern with an expression {@code expr}, false
   *         otherwise.
   */
  public static boolean isFsLike(String input, String expr) {
    return isLike(input, new FsPattern(expr));
  }

  /**
   * @param input the input to check
   * @param expr the expression for a FsPattern
   * @param sensitive the Sensitive mode
   * @return true if the {@code input} matches the Pattern with an expression {@code expr} using the
   *         specified Sensitive mode, false otherwise.
   */
  public static boolean isFsLike(String input, String expr, boolean sensitive) {
    return isLike(input, new FsPattern(expr, sensitive));
  }

  public static boolean isFsLike(String input, String expr, boolean sensitive,
      Character charExact) {
    return isLike(input, new FsPattern(expr, sensitive, charExact));
  }

  /**
   * Checks if {@code expr} is a FS Pattern.
   * @param expr the expression to check
   * @return true if it's a FS Pattern, otherwise false.
   */
  public static boolean isFsPattern(String expr) {
    return !BeeUtils.isEmpty(expr) && !isFsAny(expr);
  }

  /**
   * Checks if an input {@code input} matches the specified Pattern.
   * @param input the input String
   * @param pattern the Pattern to use for checking
   * @return true if {@code input} matches the specified Pattern, otherwise false.
   */
  public static boolean isLike(String input, Pattern pattern) {
    Assert.notNull(pattern);
    if (BeeUtils.isEmpty(input)) {
      return false;
    }

    boolean sensitive = pattern.isSensitive();

    if (pattern.isExact() || !pattern.hasWildcards()) {
      String xStr = pattern.getExpr();
      return sensitive ? input.equals(xStr) : BeeUtils.same(xStr, input);
    }

    String[] tokens = pattern.getTokens();
    String strAny = String.valueOf(pattern.getWildcardAny());
    String strOne = String.valueOf(pattern.getWildcardOne());

    int tokIdx = 0;
    int inpIdx = 0;
    boolean anyChars = false;
    Stack<int[]> backtrack = new Stack<>();

    do {
      if (!backtrack.isEmpty()) {
        int[] positions = backtrack.pop();
        tokIdx = positions[0];
        inpIdx = positions[1];
        anyChars = true;
      }

      while (tokIdx < tokens.length) {
        if (tokens[tokIdx].equals(strOne)) {
          inpIdx++;
          if (inpIdx > input.length()) {
            break;
          }
          anyChars = false;

        } else if (tokens[tokIdx].equals(strAny)) {
          anyChars = true;
          if (tokIdx == tokens.length - 1) {
            inpIdx = input.length();
          }

        } else {
          if (anyChars) {
            inpIdx = indexOfContext(sensitive, input, inpIdx, tokens[tokIdx]);
            if (inpIdx == -1) {
              break;
            }
            int repeat = indexOfContext(sensitive, input, inpIdx + 1, tokens[tokIdx]);
            if (repeat >= 0) {
              backtrack.push(new int[] {tokIdx, repeat});
            }
          } else {
            if (!isContext(sensitive, input, inpIdx, tokens[tokIdx])) {
              break;
            }
          }

          inpIdx += tokens[tokIdx].length();
          anyChars = false;
        }

        tokIdx++;
      }

      if (tokIdx == tokens.length && inpIdx == input.length()) {
        return true;
      }

    } while (!backtrack.isEmpty());

    return false;
  }

  /**
   * @param input the input to check
   * @param expr the expression for a default Pattern
   * @return true if the {@code input} matches the default Pattern with an expression {@code expr},
   *         false otherwise.
   */
  public static boolean isLike(String input, String expr) {
    return isLike(input, new DefaultPattern(expr));
  }

  /**
   * @param input the input to check
   * @param expr the expression for a default Pattern
   * @param sensitive the Sensitive mode
   * @return true if the {@code input} matches the default Pattern with an expression {@code expr}
   *         using the specified Sensitive mode, false otherwise.
   */
  public static boolean isLike(String input, String expr, boolean sensitive) {
    return isLike(input, new DefaultPattern(expr, sensitive));
  }

  public static boolean isLike(String input, String expr, boolean sensitive, Character charExact) {
    return isLike(input, new DefaultPattern(expr, sensitive, charExact));
  }

  /**
   * Checks if an expression {@code expr} contains only of the SQL ANY wildcards.
   * @param expr the expression to check
   * @return true if the expression contain only of SQL ANY wildcards, otherwise false.
   */
  public static boolean isSqlAny(String expr) {
    return BeeUtils.containsOnly(expr, SQL_ANY);
  }

  /**
   * @return the sqlCaseSensitivity
   */
  public static boolean isSqlCaseSensitive() {
    return sqlCaseSensitivity;
  }

  /**
   * @param input the input to check
   * @param expr the expression for an SQLPattern
   * @return true if the {@code input} matches the SQLPattern with an expression {@code expr}, false
   *         otherwise.
   */
  public static boolean isSqlLike(String input, String expr) {
    return isLike(input, new SqlPattern(expr));
  }

  /**
   * @param input the input to check
   * @param expr the expression for an SQL Pattern
   * @param sensitive the Sensitive mode
   * @return true if the {@code input} matches the SQLPattern with an expression {@code expr} using
   *         the specified Sensitive mode, false otherwise.
   */
  public static boolean isSqlLike(String input, String expr, boolean sensitive) {
    return isLike(input, new SqlPattern(expr, sensitive));
  }

  public static boolean isSqlLike(String input, String expr, boolean sensitive,
      Character charExact) {
    return isLike(input, new SqlPattern(expr, sensitive, charExact));
  }

  /**
   * Checks if {@code expr} is an Sql Pattern.
   * @param expr the expression to check
   * @return true if it's an Sql Pattern, otherwise false.
   */
  public static boolean isSqlPattern(String expr) {
    return !BeeUtils.isEmpty(expr) && !isSqlAny(expr);
  }

  /**
   * Sets the default ANY wildcard to the specified {@code defaultAny}.
   * @param defaultAny the value to set ANY wildcard to
   */
  public static void setDefaultAny(char defaultAny) {
    Wildcards.defaultAny = defaultAny;
  }

  /**
   * Sets the default case sensitivity to the specified {@code defaultCastSensitivity}.
   * @param defaultCaseSensitivity the value to set default sensitivity to
   */
  public static void setDefaultCaseSensitivity(boolean defaultCaseSensitivity) {
    Wildcards.defaultCaseSensitivity = defaultCaseSensitivity;
  }

  /**
   * Sets the default ONE wildcard to the specified {@code defaultOne}.
   * @param defaultOne the value to set ONE wildcard to
   */
  public static void setDefaultOne(char defaultOne) {
    Wildcards.defaultOne = defaultOne;
  }

  /**
   * Sets the FS case sensitivity to the specified {@code fsCaseSensitivity}.
   * @param fsCaseSensitivity the value to set fsCaseSensitivity to
   */
  public static void setFsCaseSensitivity(boolean fsCaseSensitivity) {
    Wildcards.fsCaseSensitivity = fsCaseSensitivity;
  }

  /**
   * Sets the SQL case sensitivity to the specified {@code sqlCaseSensitivity}.
   * @param sqlCaseSensitivity the value to set sqlCaseSensitivity to
   */
  public static void setSqlCaseSensitivity(boolean sqlCaseSensitivity) {
    Wildcards.sqlCaseSensitivity = sqlCaseSensitivity;
  }

  private static int indexOfContext(boolean sensitive, String src, int start, String ctxt) {
    int end = src.length() - ctxt.length();
    for (int i = start; i <= end; i++) {
      if (isContext(sensitive, src, i, ctxt)) {
        return i;
      }
    }
    return -1;
  }

  private static boolean isContext(boolean sensitive, String src, int start, String ctxt) {
    return src.regionMatches(!sensitive, start, ctxt, 0, ctxt.length());
  }

  private Wildcards() {
  }
}
