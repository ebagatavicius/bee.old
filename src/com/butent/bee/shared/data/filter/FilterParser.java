package com.butent.bee.shared.data.filter;

import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.data.value.DateValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.DateOrdering;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public final class FilterParser {

  private static BeeLogger logger = LogUtils.getLogger(FilterParser.class);

  public static Filter parse(String input, List<? extends IsColumn> columns,
      String idColumnName, String versionColumnName, Long userId) {

    Filter flt = null;

    if (!BeeUtils.isEmpty(input)) {
      List<String> parts = getParts(input, "\\s+[oO][rR]\\s+");

      if (parts.size() > 1) {
        flt = Filter.or();
      } else {
        parts = getParts(input, "\\s+[aA][nN][dD]\\s+");

        if (parts.size() > 1) {
          flt = Filter.and();
        }
      }

      if (flt != null) {
        for (String part : parts) {
          Filter ff = parse(part, columns, idColumnName, versionColumnName, userId);

          if (ff == null) {
            flt = null;
            break;
          }
          ((CompoundFilter) flt).add(ff);
        }

      } else {
        String s = parts.get(0);
        String ptrn = "^\\s*" + CompoundType.NOT.toTextString() + "\\s*\\(\\s*(.*)\\s*\\)\\s*$";

        if (s.matches(ptrn)) {
          flt = parse(s.replaceFirst(ptrn, "$1"), columns, idColumnName, versionColumnName, userId);
          if (flt != null) {
            flt = Filter.isNot(flt);
          }

        } else if (CustomFilter.is(s)) {
          flt = CustomFilter.tryParse(s);

        } else {
          flt = parseExpression(s, columns, idColumnName, versionColumnName, userId);
        }
      }
    }
    return flt;
  }

  private static IsColumn detectColumn(String expr, List<? extends IsColumn> columns,
      String idColumnName, String versionColumnName) {

    if (BeeUtils.isEmpty(expr)) {
      return null;
    }

    IsColumn column = null;
    int len = 0;

    if (!BeeUtils.isEmpty(columns)) {
      for (IsColumn col : columns) {
        String s = col.getId();
        if (BeeUtils.startsWith(expr, s) && BeeUtils.hasLength(s, len + 1)) {
          column = col;
          len = s.length();
        }
      }
    }

    if (BeeUtils.hasLength(idColumnName, len + 1) && BeeUtils.startsWith(expr, idColumnName)) {
      column = new BeeColumn(ValueType.LONG, idColumnName);
      len = idColumnName.length();
    }
    if (BeeUtils.hasLength(versionColumnName, len + 1)
        && BeeUtils.startsWith(expr, versionColumnName)) {
      column = new BeeColumn(ValueType.DATE_TIME, versionColumnName);
    }
    return column;
  }

  private static List<String> getParts(String expr, String pattern) {
    String s = expr;
    String ptrn = "^\\s*\\(\\s*(.+)\\s*\\)\\s*$"; // Unparenthesize
    String x = s.replaceFirst(ptrn, "$1");

    while (validPart(x)) {
      s = x;
      if (!x.matches(ptrn)) {
        break;
      }
      x = x.replaceFirst(ptrn, "$1");
    }

    List<String> parts = new ArrayList<>();
    int cnt = s.split(pattern).length;
    boolean ok = false;

    for (int i = 2; i <= cnt; i++) {
      String[] pair = s.split(pattern, i);
      String right = pair[pair.length - 1];
      String left = s.substring(0, s.lastIndexOf(right)).replaceFirst(pattern + "$", "");

      if (validPart(left)) {
        parts.add(left);
        parts.addAll(getParts(right, pattern));
        ok = true;

      } else if (validPart(right)) {
        parts.addAll(getParts(left, pattern));
        parts.add(right);
        ok = true;
      }
      if (ok) {
        break;
      }
    }

    if (!ok) {
      parts.add(s);
    }
    return parts;
  }

  private static IsColumn isColumn(String expr, List<? extends IsColumn> columns) {
    if (!BeeUtils.isEmpty(expr) && !BeeUtils.isEmpty(columns)) {
      for (IsColumn col : columns) {
        if (BeeUtils.same(col.getId(), expr)) {
          return col;
        }
      }
    }
    return null;
  }

  private static Filter parseExpression(String expr, List<? extends IsColumn> columns,
      String idColumnName, String versionColumnName, Long userId) {

    Filter flt = null;

    if (!BeeUtils.isEmpty(expr)) {
      String s = expr.trim();
      IsColumn column = detectColumn(s, columns, idColumnName, versionColumnName);

      if (column != null) {
        String colName = column.getId();
        String value = s.substring(colName.length()).trim();

        String pattern = "^" + CompoundType.NOT.toTextString() + "\\s*\\((.*)\\)$";
        boolean notMode = value.matches(pattern);

        if (notMode) {
          value = value.replaceFirst(pattern, "$1").trim();
        }
        Operator operator = Operator.detectOperator(value);
        boolean isOperator = operator != null;

        if (isOperator) {
          value = value.replaceFirst("^\\" + operator.toTextString() + "\\s*", "");
        } else {
          if (ValueType.isString(column.getType())) {
            operator = Operator.CONTAINS;
          } else {
            operator = Operator.EQ;
          }
        }

        IsColumn column2 = isColumn(value, columns);

        if (BeeUtils.same(colName, idColumnName)) {
          flt = Filter.compareId(operator, value);

        } else if (BeeUtils.same(colName, versionColumnName)) {
          flt = Filter.compareVersion(operator, value, DateOrdering.DEFAULT);

        } else if (column2 != null) {
          flt = Filter.compareWithColumn(column, operator, column2);

        } else if (BeeUtils.isEmpty(value) && !isOperator) {
          flt = Filter.notNull(colName);

        } else {
          value = value.replaceFirst("^\"(.*)\"$", "$1") // Unquote
              .replaceAll("\"\"", "\"");

          if (BeeUtils.isEmpty(value)) {
            flt = Filter.isNull(colName);

          } else if ("{u}".equals(value) && column.getType() == ValueType.LONG && userId != null) {
            flt = Filter.compareWithValue(column.getId(), operator, new LongValue(userId));

          } else if ("{d}".equals(value) && ValueType.isDateOrDateTime(column.getType())) {
            flt = Filter.compareWithValue(column.getId(), operator,
                new DateValue(TimeUtils.today()));

          } else if ("{t}".equals(value) && ValueType.isDateOrDateTime(column.getType())) {
            flt = Filter.compareWithValue(column.getId(), operator,
                new DateTimeValue(TimeUtils.nowMinutes()));

          } else {
            flt = Filter.compareWithValue(column, operator, value, DateOrdering.DEFAULT);
          }
        }

        if (notMode && flt != null) {
          flt = Filter.isNot(flt);
        }

      } else {
        logger.warning("Unknown column in expression: " + expr);
      }
    }
    return flt;
  }

  private static boolean validPart(String expr) {
    String wh = expr;
    String regex = "^(.*)\"(.*)\"(.*)$";

    while (wh.matches(regex)) {
      String s = wh.replaceFirst(regex, "$2");
      wh = wh.replaceFirst(regex, "$1" + s.replaceAll("[\\(\\)]", "") + "$3");
    }
    if (wh.contains("\"")) {
      return false;
    }
    regex = "^(.*)\\((.*)\\)(.*)$";

    while (wh.matches(regex)) {
      wh = wh.replaceFirst(regex, "$1" + "$2" + "$3");
    }
    return !wh.matches(".*[\\(\\)].*");
  }

  private FilterParser() {
  }
}
