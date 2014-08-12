package com.butent.bee.shared.data.filter;

import com.butent.bee.shared.utils.BeeUtils;

/**
 * Contains a list of possible comparison operators.
 */

public enum Operator {
  EQ("="),
  NE("!="),
  LT("<"),
  GT(">"),
  LE("<="),
  GE(">="),
  STARTS("^"),
  ENDS("*"),
  CONTAINS("$"),
  MATCHES("~"),
  IN,
  IS_NULL,
  NOT_NULL,
  FULL_TEXT;

  public static final String CHAR_ANY = "*";
  public static final String CHAR_ONE = "?";

  public static Operator detectOperator(String expr) {
    Operator op = null;

    if (!BeeUtils.isEmpty(expr)) {
      for (Operator operator : Operator.values()) {
        String s = operator.toTextString();

        if (!BeeUtils.isEmpty(s)) {
          if (expr.startsWith(s) && (op == null || s.length() > op.toTextString().length())) {
            op = operator;
          }
        }
      }
    }
    return op;
  }

  public static Operator getOperator(String op) {
    if (!BeeUtils.isEmpty(op)) {
      for (Operator operator : Operator.values()) {
        if (BeeUtils.same(operator.toTextString(), op)) {
          return operator;
        }
      }
    }
    return null;
  }

  private String textString;

  Operator() {
    this.textString = null;
  }

  Operator(String textString) {
    this.textString = textString;
  }

  public boolean isStringOperator() {
    return this == Operator.STARTS
        || this == Operator.ENDS
        || this == Operator.CONTAINS
        || this == Operator.MATCHES;
  }

  public String toTextString() {
    return textString;
  }
}
