package com.butent.bee.server.sql;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.SqlConstants.SqlKeyword;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerEvent;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerScope;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerTiming;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerType;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Contains various utility SQL statement related functions like joining by comparisons, creating
 * and dropping keys etc.
 */

public final class SqlUtils {

  public static IsExpression aggregate(SqlFunction fnc, IsExpression expr) {
    Map<String, Object> params = new HashMap<>();
    params.put("expression", expr);
    return new FunctionExpression(fnc, params);
  }

  public static HasConditions and(IsCondition... conditions) {
    return CompoundCondition.and(conditions);
  }

  public static IsCondition anyIntersects(String source, Collection<String> fields,
      Range<?> range) {

    Assert.notEmpty(source);
    Assert.notEmpty(fields);

    HasConditions lowerCondition;
    HasConditions upperCondition;

    IsCondition condition;

    if (range != null && range.hasLowerBound()) {
      lowerCondition = or();

      for (String field : fields) {
        if (!BeeUtils.isEmpty(field)) {
          if (range.lowerBoundType() == BoundType.OPEN) {
            condition = more(source, field, range.lowerEndpoint());
          } else {
            condition = moreEqual(source, field, range.lowerEndpoint());
          }

          lowerCondition.add(condition);
        }
      }
    } else {
      lowerCondition = null;
    }

    if (range != null && range.hasUpperBound()) {
      upperCondition = or();

      for (String field : fields) {
        if (!BeeUtils.isEmpty(field)) {
          if (range.upperBoundType() == BoundType.OPEN) {
            condition = less(source, field, range.upperEndpoint());
          } else {
            condition = lessEqual(source, field, range.upperEndpoint());
          }

          upperCondition.add(condition);
        }
      }
    } else {
      upperCondition = null;
    }

    if (lowerCondition == null && upperCondition == null) {
      return null;
    } else {
      return and(lowerCondition, upperCondition);
    }
  }

  public static IsCondition anyNotNull(String src, String fld1, String fld2) {
    return or(notNull(src, fld1), notNull(src, fld2));
  }

  public static IsExpression bitAnd(IsExpression expr, Object value) {
    return new FunctionExpression(SqlFunction.BITAND,
        ImmutableMap.of("expression", expr, "value", value));
  }

  public static IsExpression bitAnd(String source, String field, Object value) {
    return bitAnd(field(source, field), value);
  }

  public static IsExpression bitOr(IsExpression expr, Object value) {
    return new FunctionExpression(SqlFunction.BITOR,
        ImmutableMap.of("expression", expr, "value", value));
  }

  public static IsExpression bitOr(String source, String field, Object value) {
    return bitOr(field(source, field), value);
  }

  public static IsExpression cast(IsExpression expr, SqlDataType type, int precision, int scale) {
    return new FunctionExpression(SqlFunction.CAST,
        ImmutableMap.of("expression", expr, "type", type, "precision", precision, "scale", scale));
  }

  public static IsCondition compare(IsExpression expr, Operator op, IsSql value) {
    return new ComparisonCondition(op, expr, value);
  }

  public static IsCondition compare(String source, String field, Operator op, Object value) {
    return compare(field(source, field), op, constant(value));
  }

  public static IsExpression concat(Object... members) {
    Assert.minLength(ArrayUtils.length(members), 2);
    Assert.noNulls(members);
    return new FunctionExpression(SqlFunction.CONCAT, getMemberMap(members));
  }

  public static IsExpression constant(Object constant) {
    return new ConstantExpression(Value.getValue(constant));
  }

  public static IsCondition contains(IsExpression expr, String value) {
    Assert.notEmpty(value);
    return compare(expr, Operator.CONTAINS, constant(value));
  }

  public static IsCondition contains(String source, String field, String value) {
    return contains(field(source, field), value);
  }

  public static IsCondition containsAny(String value, IsExpression... expressions) {
    if (ArrayUtils.isEmpty(expressions)) {
      return null;
    }
    HasConditions clause = or();

    for (IsExpression expr : expressions) {
      clause.add(contains(expr, value));
    }
    return clause;
  }

  public static IsQuery createCheck(String table, String name, String expression) {
    return new SqlCommand(SqlKeyword.ADD_CONSTRAINT,
        ImmutableMap.of("table", name(table), "name", name(name), "type", SqlKeyword.CHECK,
            "expression", expression));
  }

  public static IsQuery createForeignKey(String table, String name, List<String> fields,
      String refTable, List<String> refFields, SqlKeyword cascade) {

    Map<String, Object> params = getConstraintMap(SqlKeyword.FOREIGN_KEY, table, name, fields);
    params.put("refTable", name(refTable));

    List<Object> refFlds = new ArrayList<>();
    for (String fld : refFields) {
      if (!BeeUtils.isEmpty(refFlds)) {
        refFlds.add(", ");
      }
      refFlds.add(name(fld));
    }
    params.put("refFields", expression(refFlds.toArray()));
    params.put("cascade", cascade);

    return new SqlCommand(SqlKeyword.ADD_CONSTRAINT, params);
  }

  public static IsQuery createIndex(String table, String name, List<String> fields,
      boolean isUnique) {
    Map<String, Object> params = getConstraintMap(null, table, name, fields);
    params.put("isUnique", isUnique);

    return new SqlCommand(SqlKeyword.CREATE_INDEX, params);
  }

  public static IsQuery createIndex(String table, String name, String expression,
      boolean isUnique) {

    return new SqlCommand(SqlKeyword.CREATE_INDEX, ImmutableMap.of("table", name(table),
        "name", name(name), "expression", expression, "isUnique", isUnique));
  }

  public static IsQuery createPrimaryKey(String table, String name, List<String> fields) {
    return new SqlCommand(SqlKeyword.ADD_CONSTRAINT,
        getConstraintMap(SqlKeyword.PRIMARY_KEY, table, name, fields));
  }

  public static IsQuery createSchema(String schema) {
    return new SqlCommand(SqlKeyword.CREATE_SCHEMA,
        ImmutableMap.of("schema", (Object) name(schema)));
  }

  public static IsQuery createTrigger(String name, String table,
      SqlTriggerType type, Map<String, ?> parameters,
      SqlTriggerTiming timing, EnumSet<SqlTriggerEvent> events, SqlTriggerScope scope) {

    Map<String, Object> params = new HashMap<>();
    params.put("name", name(name));
    params.put("table", name(table));
    params.put("type", type);
    params.put("parameters", parameters);
    params.put("timing", timing);
    params.put("events", events);
    params.put("scope", scope);

    return new SqlCommand(SqlKeyword.CREATE_TRIGGER, params);
  }

  public static IsQuery createUniqueKey(String table, String name, List<String> fields) {
    return new SqlCommand(SqlKeyword.ADD_CONSTRAINT,
        getConstraintMap(SqlKeyword.UNIQUE, table, name, fields));
  }

  public static IsQuery dbConstraints(String dbName, String dbSchema, String table,
      SqlKeyword... types) {
    Map<String, Object> params = new HashMap<>();
    params.put("dbName", dbName);
    params.put("dbSchema", dbSchema);
    params.put("table", table);
    params.put("keyTypes", types);

    return new SqlCommand(SqlKeyword.DB_CONSTRAINTS, params);
  }

  public static IsQuery dbFields(String dbName, String dbSchema, String table) {
    Map<String, Object> params = new HashMap<>();
    params.put("dbName", dbName);
    params.put("dbSchema", dbSchema);
    params.put("table", table);

    return new SqlCommand(SqlKeyword.DB_FIELDS, params);
  }

  public static IsQuery dbForeignKeys(String dbName, String dbSchema, String table,
      String refTable) {
    Map<String, Object> params = new HashMap<>();
    params.put("dbName", dbName);
    params.put("dbSchema", dbSchema);
    params.put("table", table);
    params.put("refTable", refTable);

    return new SqlCommand(SqlKeyword.DB_FOREIGNKEYS, params);
  }

  public static IsQuery dbIndexes(String dbName, String dbSchema, String table) {
    Map<String, Object> params = new HashMap<>();
    params.put("dbName", dbName);
    params.put("dbSchema", dbSchema);
    params.put("table", table);

    return new SqlCommand(SqlKeyword.DB_INDEXES, params);
  }

  public static IsQuery dbName() {
    return new SqlCommand(SqlKeyword.DB_NAME, null);
  }

  public static IsQuery dbSchema() {
    return new SqlCommand(SqlKeyword.DB_SCHEMA, null);
  }

  public static IsQuery dbSchemas(String dbName, String schema) {
    Map<String, Object> params = new HashMap<>();
    params.put("dbName", dbName);
    params.put("schema", schema);

    return new SqlCommand(SqlKeyword.DB_SCHEMAS, params);
  }

  public static IsQuery dbTables(String dbName, String dbSchema, String table) {
    Map<String, Object> params = new HashMap<>();
    params.put("dbName", dbName);
    params.put("dbSchema", dbSchema);
    params.put("table", table);

    return new SqlCommand(SqlKeyword.DB_TABLES, params);
  }

  public static IsQuery dbTriggers(String dbName, String dbSchema, String table) {
    Map<String, Object> params = new HashMap<>();
    params.put("dbName", dbName);
    params.put("dbSchema", dbSchema);
    params.put("table", table);

    return new SqlCommand(SqlKeyword.DB_TRIGGERS, params);
  }

  public static IsExpression divide(Object... members) {
    Assert.minLength(ArrayUtils.length(members), 2);
    Assert.noNulls(members);
    return new FunctionExpression(SqlFunction.DIVIDE, getMemberMap(members));
  }

  public static IsQuery dropForeignKey(String table, String name) {
    return new SqlCommand(SqlKeyword.DROP_FOREIGNKEY,
        ImmutableMap.of("table", (Object) name(table), "name", name(name)));
  }

  public static IsQuery dropTable(String table) {
    return new SqlCommand(SqlKeyword.DROP_TABLE, ImmutableMap.of("table", (Object) name(table)));
  }

  public static IsCondition endsWith(IsExpression expr, String value) {
    Assert.notEmpty(value);
    return compare(expr, Operator.ENDS, constant(value));
  }

  public static IsCondition endsWith(String source, String field, String value) {
    return endsWith(field(source, field), value);
  }

  public static IsCondition equals(IsExpression expr, Object value) {
    if (value == null) {
      return isNull(expr);
    }
    return compare(expr, Operator.EQ, getSqlExpression(value));
  }

  public static IsCondition equals(String source, String field, Object value) {
    return equals(field(source, field), value);
  }

  public static HasConditions equals(String source, String f1, Object v1, String f2, Object v2) {
    return and(equals(source, f1, v1), equals(source, f2, v2));
  }

  public static HasConditions equals(String source, String f1, Object v1, String f2, Object v2,
      String f3, Object v3) {
    return equals(source, f1, v1, f2, v2).add(equals(source, f3, v3));
  }

  public static HasConditions equals(String source, String f1, Object v1, String f2, Object v2,
      String f3, Object v3, String f4, Object v4) {
    return equals(source, f1, v1, f2, v2, f3, v3).add(equals(source, f4, v4));
  }

  public static HasConditions equals(String source, Map<String, Object> pairs) {
    if (BeeUtils.isEmpty(pairs)) {
      return null;
    }
    HasConditions andCondition = and();

    for (Entry<String, Object> pair : pairs.entrySet()) {
      andCondition.add(equals(source, pair.getKey(), pair.getValue()));
    }
    return andCondition;
  }

  public static HasConditions equalsAny(String source, String f1, Object v1, String f2, Object v2) {
    return or(equals(source, f1, v1), equals(source, f2, v2));
  }

  public static HasConditions equalsAny(String source, Map<String, Object> pairs) {
    if (BeeUtils.isEmpty(pairs)) {
      return null;
    }
    HasConditions orCondition = or();

    for (Entry<String, Object> pair : pairs.entrySet()) {
      orCondition.add(equals(source, pair.getKey(), pair.getValue()));
    }
    return orCondition;
  }

  public static IsExpression expression(Object... members) {
    Assert.minLength(ArrayUtils.length(members), 1);
    Assert.noNulls(members);
    return new FunctionExpression(SqlFunction.BULK, getMemberMap(members));
  }

  public static IsExpression field(String source, String field) {
    Assert.notEmpty(source);
    Assert.notEmpty(field);
    return name(BeeUtils.join(".", source, field));
  }

  public static IsExpression[] fields(String source, String... fields) {
    Assert.minLength(ArrayUtils.length(fields), 1);

    int len = ArrayUtils.length(fields);
    IsExpression[] list = new IsExpression[len];

    for (int i = 0; i < len; i++) {
      list[i] = field(source, fields[i]);
    }
    return list;
  }

  public static IsCondition fullText(IsExpression expr, String value) {
    return new FullTextCondition(expr, value);
  }

  public static IsCondition fullText(String source, String field, String value) {
    return fullText(field(source, field), value);
  }

  public static IsCondition in(IsExpression xpr, SqlSelect query) {
    return new ComparisonCondition(Operator.IN, xpr, Assert.notNull(query));
  }

  public static IsCondition in(String src, String fld, SqlSelect query) {
    return new ComparisonCondition(Operator.IN, field(src, fld), query);
  }

  public static IsCondition in(String src, String fld, String dst, String dFld) {
    return in(src, fld, dst, dFld, null);
  }

  public static IsCondition in(String src, String fld, String dst, String dFld,
      IsCondition clause) {
    SqlSelect query = new SqlSelect()
        .setDistinctMode(true)
        .addFields(dst, dFld)
        .addFrom(dst)
        .setWhere(clause);

    return in(src, fld, query);
  }

  public static IsCondition inList(IsExpression expr, Object... values) {
    int len = ArrayUtils.length(values);
    Assert.minLength(len, 1);
    IsCondition cond;

    if (len == 1) {
      cond = equals(expr, values[0]);
    } else {
      IsSql[] vals = new IsSql[len];

      for (int i = 0; i < len; i++) {
        vals[i] = getSqlExpression(values[i]);
      }
      cond = new ComparisonCondition(Operator.IN, expr, vals);
    }
    return cond;
  }

  public static IsCondition inList(String source, String field, Object... values) {
    return inList(field(source, field), values);
  }

  public static IsCondition inList(IsExpression expr, Collection<?> values) {
    if (BeeUtils.isEmpty(values)) {
      return null;
    }
    return inList(expr, values.toArray());
  }

  public static IsCondition inList(String source, String field, Collection<?> values) {
    return inList(field(source, field), values);
  }

  public static IsCondition isDifferent(String src, String fld1, String fld2) {
    return or(
        and(notNull(src, fld1), isNull(src, fld2)),
        and(isNull(src, fld1), notNull(src, fld2)),
        compare(field(src, fld1), Operator.NE, field(src, fld2)));
  }

  public static IsCondition isNull(IsExpression expr) {
    return new ComparisonCondition(Operator.IS_NULL, expr);
  }

  public static IsCondition isNull(String src, String fld) {
    return isNull(field(src, fld));
  }

  public static IsCondition join(String src1, String fld1, String src2, String fld2) {
    return compare(field(src1, fld1), Operator.EQ, field(src2, fld2));
  }

  public static IsCondition joinLess(String src1, String fld1, String src2, String fld2) {
    return compare(field(src1, fld1), Operator.LT, field(src2, fld2));
  }

  public static IsCondition joinLessEqual(String src1, String fld1, String src2, String fld2) {
    return compare(field(src1, fld1), Operator.LE, field(src2, fld2));
  }

  public static IsCondition joinMore(String src1, String fld1, String src2, String fld2) {
    return compare(field(src1, fld1), Operator.GT, field(src2, fld2));
  }

  public static IsCondition joinMoreEqual(String src1, String fld1, String src2, String fld2) {
    return compare(field(src1, fld1), Operator.GE, field(src2, fld2));
  }

  public static IsCondition joinNotEqual(String src1, String fld1, String src2, String fld2) {
    return compare(field(src1, fld1), Operator.NE, field(src2, fld2));
  }

  public static IsCondition joinUsing(String src1, String src2, String... flds) {
    Assert.minLength(ArrayUtils.length(flds), 1);

    IsCondition cond;

    if (flds.length > 1) {
      HasConditions cb = and();

      for (String fld : flds) {
        cb.add(join(src1, fld, src2, fld));
      }
      cond = cb;

    } else {
      String fld = flds[0];
      cond = join(src1, fld, src2, fld);
    }
    return cond;
  }

  public static IsExpression left(IsExpression expr, int len) {
    return new FunctionExpression(SqlFunction.LEFT,
        ImmutableMap.of("expression", expr, "len", len));
  }

  public static IsExpression left(String source, String field, int len) {
    return left(field(source, field), len);
  }

  public static IsExpression length(IsExpression expr) {
    return new FunctionExpression(SqlFunction.LENGTH,
        ImmutableMap.of("expression", (Object) expr));
  }

  public static IsExpression length(String source, String field) {
    return length(field(source, field));
  }

  public static IsCondition less(IsExpression expr, Object value) {
    return compare(expr, Operator.LT, getSqlExpression(value));
  }

  public static IsCondition less(String source, String field, Object value) {
    return less(field(source, field), value);
  }

  public static IsCondition lessEqual(IsExpression expr, Object value) {
    return compare(expr, Operator.LE, getSqlExpression(value));
  }

  public static IsCondition lessEqual(String source, String field, Object value) {
    return lessEqual(field(source, field), value);
  }

  public static IsCondition matches(IsExpression expr, String value) {
    Assert.notEmpty(value);
    return compare(expr, Operator.MATCHES, constant(value));
  }

  public static IsCondition matches(String source, String field, String value) {
    return matches(field(source, field), value);
  }

  public static IsExpression minus(Object... members) {
    Assert.minLength(ArrayUtils.length(members), 2);
    Assert.noNulls(members);
    return new FunctionExpression(SqlFunction.MINUS, getMemberMap(members));
  }

  public static IsCondition more(IsExpression expr, Object value) {
    return compare(expr, Operator.GT, getSqlExpression(value));
  }

  public static IsCondition more(String source, String field, Object value) {
    return more(field(source, field), value);
  }

  public static IsCondition moreEqual(IsExpression expr, Object value) {
    return compare(expr, Operator.GE, getSqlExpression(value));
  }

  public static IsCondition moreEqual(String source, String field, Object value) {
    return moreEqual(field(source, field), value);
  }

  public static IsExpression multiply(Object... members) {
    Assert.minLength(ArrayUtils.length(members), 2);
    Assert.noNulls(members);
    return new FunctionExpression(SqlFunction.MULTIPLY, getMemberMap(members));
  }

  public static IsExpression name(String name) {
    return new NameExpression(name);
  }

  public static IsCondition negative(IsExpression expr) {
    return less(expr, 0);
  }

  public static IsCondition negative(String source, String field) {
    return less(source, field, 0);
  }

  public static IsCondition nonNegative(IsExpression expr) {
    return moreEqual(expr, 0);
  }

  public static IsCondition nonNegative(String source, String field) {
    return moreEqual(source, field, 0);
  }

  public static IsCondition nonPositive(IsExpression expr) {
    return lessEqual(expr, 0);
  }

  public static IsCondition nonPositive(String source, String field) {
    return lessEqual(source, field, 0);
  }

  public static IsCondition nonZero(IsExpression expr) {
    return notEqual(expr, 0);
  }

  public static IsCondition nonZero(String source, String field) {
    return notEqual(source, field, 0);
  }

  public static IsCondition not(IsCondition condition) {
    return new NegationCondition(condition);
  }

  public static IsCondition notEqual(IsExpression expr, Object value) {
    if (value == null) {
      return notNull(expr);
    }
    return compare(expr, Operator.NE, getSqlExpression(value));
  }

  public static IsCondition notEqual(String source, String field, Object value) {
    return notEqual(field(source, field), value);
  }

  public static IsCondition notNull(IsExpression expr) {
    return new ComparisonCondition(Operator.NOT_NULL, expr);
  }

  public static IsCondition notNull(String src, String fld) {
    return notNull(field(src, fld));
  }

  public static IsCondition notNull(String src, String fld1, String fld2) {
    return and(notNull(src, fld1), notNull(src, fld2));
  }

  public static IsExpression nvl(Object... members) {
    Assert.minLength(ArrayUtils.length(members), 2);
    Assert.noNulls(members);
    return new FunctionExpression(SqlFunction.NVL, getMemberMap(members));
  }

  public static HasConditions or(IsCondition... conditions) {
    return CompoundCondition.or(conditions);
  }

  public static IsExpression plus(Object... members) {
    Assert.minLength(ArrayUtils.length(members), 2);
    Assert.noNulls(members);
    return new FunctionExpression(SqlFunction.PLUS, getMemberMap(members));
  }

  public static IsCondition positive(IsExpression expr) {
    return more(expr, 0);
  }

  public static IsCondition positive(String source, String field) {
    return more(source, field, 0);
  }

  public static IsCondition positive(String src, String fld1, String fld2) {
    return and(positive(src, fld1), positive(src, fld2));
  }

  public static IsQuery renameTable(String from, String to) {
    return new SqlCommand(SqlKeyword.RENAME_TABLE,
        ImmutableMap.of("nameFrom", (Object) name(from), "nameTo", name(to)));
  }

  public static IsExpression right(IsExpression expr, int len) {
    return new FunctionExpression(SqlFunction.RIGHT,
        ImmutableMap.of("expression", expr, "len", len));
  }

  public static IsExpression right(String source, String field, int len) {
    return right(field(source, field), len);
  }

  public static IsExpression round(IsExpression expr, int precision) {
    return cast(expr, SqlDataType.DECIMAL, 15, precision);
  }

  public static IsExpression round(String source, String field, int precision) {
    return round(field(source, field), precision);
  }

  public static IsCondition same(IsExpression expr, String value) {
    return and(startsWith(expr, value), endsWith(expr, value));
  }

  public static IsCondition same(String source, String field, String value) {
    return same(field(source, field), value);
  }

  public static IsQuery setSqlParameter(String prmName, Object value) {
    Map<String, Object> params = new HashMap<>();
    params.put("prmName", prmName);
    params.put("prmValue", value);

    return new SqlCommand(SqlKeyword.SET_PARAMETER, params);
  }

  public static IsExpression sqlCase(IsExpression expr, Object... pairs) {
    Assert.notNull(pairs);
    Assert.parameterCount(pairs.length, 2);

    Map<String, Object> params = new HashMap<>();
    if (expr != null) {
      params.put("expression", expr);
    }
    int x = pairs.length % 2;

    for (int i = 0; i < (pairs.length - x) / 2; i++) {
      params.put("case" + i, getSqlExpression(pairs[i * 2]));
      params.put("value" + i, getSqlExpression(pairs[i * 2 + 1]));
    }
    if (x == 1) {
      params.put("caseElse", getSqlExpression(pairs[pairs.length - x]));
    }
    return new FunctionExpression(SqlFunction.CASE, params);
  }

  public static IsCondition sqlFalse() {
    return equals(constant(1), 0);
  }

  public static IsExpression sqlIf(IsCondition cond, Object ifTrue, Object ifFalse) {
    Map<String, Object> params = new HashMap<>();
    params.put("condition", cond);
    params.put("ifTrue", getSqlExpression(ifTrue));
    params.put("ifFalse", getSqlExpression(ifFalse));

    return new FunctionExpression(SqlFunction.IF, params);
  }

  public static IsCondition sqlTrue() {
    return equals(constant(1), 1);
  }

  public static IsCondition startsWith(IsExpression expr, String value) {
    Assert.notEmpty(value);
    return compare(expr, Operator.STARTS, constant(value));
  }

  public static IsCondition startsWith(String source, String field, String value) {
    return startsWith(field(source, field), value);
  }

  public static IsExpression substring(IsExpression expr, int pos) {
    return new FunctionExpression(SqlFunction.SUBSTRING,
        ImmutableMap.of("expression", expr, "pos", pos));
  }

  public static IsExpression substring(IsExpression expr, int pos, int len) {
    return new FunctionExpression(SqlFunction.SUBSTRING,
        ImmutableMap.of("expression", expr, "pos", pos, "len", len));
  }

  public static IsExpression substring(String source, String field, int pos) {
    return substring(field(source, field), pos);
  }

  public static IsExpression substring(String source, String field, int pos, int len) {
    return substring(field(source, field), pos, len);
  }

  public static String table(String schema, String table) {
    Assert.notEmpty(schema);
    Assert.notEmpty(table);
    return BeeUtils.join(".", schema, table);
  }

  public static String temporaryName() {
    String tmp = "tmp_" + uniqueName();
    return temporaryName(tmp);
  }

  public static String temporaryName(String tmp) {
    if (BeeUtils.isEmpty(tmp)) {
      return temporaryName();
    }
    return new SqlCommand(SqlKeyword.TEMPORARY_NAME, ImmutableMap.of("name", (Object) tmp))
        .getQuery();
  }

  public static IsQuery truncateTable(String table) {
    return new SqlCommand(SqlKeyword.TRUNCATE_TABLE,
        ImmutableMap.of("table", (Object) name(table)));
  }

  public static String uniqueName() {
    return BeeUtils.randomString(8);
  }

  public static String uniqueName(String prefix) {
    return BeeUtils.trim(prefix) + uniqueName();
  }

  static <T> Collection<T> addCollection(Collection<T> destination, Collection<T> source) {
    Collection<T> dest = destination;

    if (!BeeUtils.isEmpty(source)) {
      if (BeeUtils.isEmpty(dest)) {
        dest = source;
      } else {
        dest.addAll(source);
      }
    }
    return dest;
  }

  private static Map<String, Object> getConstraintMap(SqlKeyword type, String table, String name,
      List<String> fields) {

    List<Object> flds = new ArrayList<>();
    for (String fld : fields) {
      if (!BeeUtils.isEmpty(flds)) {
        flds.add(", ");
      }
      flds.add(name(fld));
    }
    Map<String, Object> params = new HashMap<>();
    params.put("table", name(table));
    params.put("name", name(name));
    params.put("type", type);
    params.put("fields", expression(flds.toArray()));
    return params;
  }

  private static Map<String, Object> getMemberMap(Object... members) {
    Map<String, Object> params = new HashMap<>();

    if (members != null) {
      for (int i = 0; i < members.length; i++) {
        params.put("member" + i, members[i]);
      }
    }
    return params;
  }

  private static IsSql getSqlExpression(Object value) {
    IsSql v;

    if (value instanceof IsSql) {
      v = (IsSql) value;
    } else {
      v = constant(value);
    }
    return v;
  }

  private SqlUtils() {
  }
}
