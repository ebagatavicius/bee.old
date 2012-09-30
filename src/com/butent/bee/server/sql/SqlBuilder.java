package com.butent.bee.server.sql;

import com.google.common.collect.Maps;

import com.butent.bee.server.sql.SqlCreate.SqlField;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.HasLength;
import com.butent.bee.shared.BeeConst.SqlEngine;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.SqlConstants.SqlKeyword;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerType;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * Is an abstract class for all SQL server specific SQL builders, contains core requirements for SQL
 * statements.
 */

public abstract class SqlBuilder {

  public abstract SqlEngine getEngine();

  protected abstract String getAuditTrigger(String auditTable, String idName,
      Collection<String> fields);

  /**
   * Generates an SQL CREATE query from the specified argument {@code sc}. There are two ways to
   * generate the query. First: by defining a {@code  dataSource}. Second: describing the fields
   * manually. Only one at an instance of the SqlCreate object is possible.
   * 
   * @param sc the SqlCreate object
   * @return a generated SQL CREATE query
   */
  protected String getCreate(SqlCreate sc) {
    Assert.notNull(sc);
    Assert.state(!sc.isEmpty());

    StringBuilder query = new StringBuilder("CREATE ");

    if (sc.isTemporary()) {
      query.append(sqlKeyword(SqlKeyword.TEMPORARY, null));
    }
    query.append("TABLE ");

    query.append(SqlUtils.name(sc.getTarget()).getSqlString(this));

    List<SqlField> fieldList = sc.getFields();

    if (sc.getDataSource() != null) {
      query.append(" AS ").append(sc.getDataSource().getSqlString(this));
    } else {
      query.append(" (");

      for (int i = 0; i < fieldList.size(); i++) {
        if (i > 0) {
          query.append(", ");
        }
        SqlField field = fieldList.get(i);
        query.append(field.getName().getSqlString(this))
            .append(" ").append(sqlType(field.getType(), field.getPrecision(), field.getScale()));

        if (field.isNotNull()) {
          query.append(" NOT NULL");
        }
      }
      query.append(")");
    }
    return query.toString();
  }

  /**
   * Generates an SQL DELETE query from the specified argument {@code sd}. {@code sd} must have From
   * and Where conditions set.
   * 
   * @param sd the SqlDelete object to use for generating
   * @return a generated SQL DELETE query
   */
  protected String getDelete(SqlDelete sd) {
    Assert.notNull(sd);
    Assert.state(!sd.isEmpty());

    StringBuilder query = new StringBuilder("DELETE FROM ");

    query.append(SqlUtils.name(sd.getTarget()).getSqlString(this));

    String wh = sd.getWhere().getSqlString(this);

    if (!BeeUtils.isEmpty(wh)) {
      query.append(" WHERE ").append(wh);
    }
    return query.toString();
  }

  /**
   * Generates an SQL INSERT query from the specified argument {@code si}. There are two ways to
   * generate the query. First: by defining a {@code  dataSource}. Second: describing the fields
   * manually. Only one at an instance of the SqlInsert object is possible.
   * 
   * @param si the SqlInsert object
   * @return a generated SQL INSERT query
   */
  protected String getInsert(SqlInsert si) {
    Assert.notNull(si);
    Assert.state(!si.isEmpty());

    StringBuilder query = new StringBuilder("INSERT INTO ");

    query.append(SqlUtils.name(si.getTarget()).getSqlString(this));

    List<IsExpression> fieldList = si.getFields();

    query.append(" (");

    for (int i = 0; i < fieldList.size(); i++) {
      if (i > 0) {
        query.append(", ");
      }
      IsExpression field = fieldList.get(i);
      query.append(field.getSqlString(this));
    }
    query.append(") ");

    if (si.getDataSource() != null) {
      query.append(si.getDataSource().getSqlString(this));
    } else {
      List<IsExpression> valueList = si.getValues();

      if (!BeeUtils.isEmpty(valueList)) {
        query.append("VALUES (");

        for (int i = 0; i < valueList.size(); i++) {
          if (i > 0) {
            query.append(", ");
          }
          IsExpression value = valueList.get(i);
          query.append(value.getSqlString(this));
        }
        query.append(")");
      }
    }
    return query.toString();
  }

  protected final String getQuery(IsQuery query) {
    if (query instanceof SqlCreate) {
      return getCreate((SqlCreate) query);

    } else if (query instanceof SqlDelete) {
      return getDelete((SqlDelete) query);

    } else if (query instanceof SqlInsert) {
      return getInsert((SqlInsert) query);

    } else if (query instanceof SqlSelect) {
      return getSelect((SqlSelect) query);

    } else if (query instanceof SqlUpdate) {
      return getUpdate((SqlUpdate) query);

    } else {
      Assert.unsupported("Unsupported class name: " + NameUtils.getClassName(query.getClass()));
    }
    return null;
  }

  protected abstract String getRelationTrigger(List<Map<String, String>> fields);

  /**
   * Generates an SQL SELECT query from the specified argument {@code ss}. From value must be
   * defined in order to generate the query.
   * 
   * @param ss the SqlSelect object
   * @return a generated SQL SELECT query
   */
  protected String getSelect(SqlSelect ss) {
    Assert.notNull(ss);
    Assert.state(!ss.isEmpty());

    StringBuilder query = new StringBuilder("SELECT ");

    if (ss.isDistinctMode()) {
      query.append("DISTINCT ");
    }
    List<IsExpression[]> fieldList = ss.getFields();

    for (int i = 0; i < fieldList.size(); i++) {
      if (i > 0) {
        query.append(", ");
      }
      IsExpression[] fldEntry = fieldList.get(i);
      IsExpression field = fldEntry[SqlSelect.FIELD_EXPR];
      query.append(field.getSqlString(this));

      IsExpression alias = fldEntry[SqlSelect.FIELD_ALIAS];

      if (alias != null) {
        query.append(" AS ").append(alias.getSqlString(this));
      }
    }
    List<IsFrom> fromList = ss.getFrom();

    if (!BeeUtils.isEmpty(fromList)) {
      query.append(" FROM ");

      for (IsFrom from : fromList) {
        query.append(from.getSqlString(this));
      }
    }
    IsCondition whereClause = ss.getWhere();

    if (whereClause != null) {
      String wh = whereClause.getSqlString(this);

      if (!BeeUtils.isEmpty(wh)) {
        query.append(" WHERE ").append(wh);
      }
    }
    List<IsExpression> groupList = ss.getGroupBy();

    if (!BeeUtils.isEmpty(groupList)) {
      query.append(" GROUP BY ");

      for (int i = 0; i < groupList.size(); i++) {
        if (i > 0) {
          query.append(", ");
        }
        String group = groupList.get(i).getSqlString(this);
        query.append(group);
      }
    }
    IsCondition havingClause = ss.getHaving();

    if (havingClause != null) {
      query.append(" HAVING ")
          .append(havingClause.getSqlString(this));
    }
    List<SqlSelect> unionList = ss.getUnion();

    if (!BeeUtils.isEmpty(unionList)) {
      for (SqlSelect union : unionList) {
        query.append(ss.isUnionAllMode() ? " UNION ALL " : " UNION ")
            .append("(").append(union.getSqlString(this)).append(")");
      }
    }
    List<String[]> orderList = ss.getOrderBy();

    if (!BeeUtils.isEmpty(orderList)) {
      query.append(" ORDER BY ");

      for (int i = 0; i < orderList.size(); i++) {
        if (i > 0) {
          query.append(", ");
        }
        String[] orderEntry = orderList.get(i);
        String src = orderEntry[SqlSelect.ORDER_SRC];
        IsExpression order = BeeUtils.isEmpty(src) || !BeeUtils.isEmpty(ss.getUnion())
            ? SqlUtils.name(orderEntry[SqlSelect.ORDER_FLD])
            : SqlUtils.field(src, orderEntry[SqlSelect.ORDER_FLD]);

        query.append(order.getSqlString(this)).append(orderEntry[SqlSelect.ORDER_DESC]);
      }
    }
    return query.toString();
  }

  @SuppressWarnings("unchecked")
  protected String getTriggerBody(Map<String, Object> params) {
    String body = null;
    Map<String, ?> triggerParams = (Map<String, ?>) params.get("parameters");

    switch ((SqlTriggerType) params.get("type")) {
      case AUDIT:
        body = getAuditTrigger(BeeUtils.join(".",
            sqlQuote((String) triggerParams.get("auditSchema")),
            sqlQuote((String) triggerParams.get("auditTable"))),
            sqlQuote((String) triggerParams.get("idName")),
            (Collection<String>) triggerParams.get("fields"));
        break;

      case RELATION:
        body = getRelationTrigger((List<Map<String, String>>) triggerParams.get("fields"));
        break;

      case CUSTOM:
        body = (String) triggerParams.get("body");
        break;
    }
    return body;
  }

  /**
   * Generates an SQL UPDATE query from the specified argument {@code su}. A target table and at
   * least one expression must be defined.
   * 
   * @param su the SqlUpdate object.
   * @return a generated SQL UPDATE query
   */
  protected String getUpdate(SqlUpdate su) {
    Assert.notNull(su);
    Assert.state(!su.isEmpty());

    StringBuilder query = new StringBuilder();
    String dst = SqlUtils.name(su.getTarget()).getSqlString(this);
    IsFrom fromSource = su.getFromSource();

    if (fromSource == null) {
      query.append("UPDATE ").append(dst);
    } else {
      query = new StringBuilder("MERGE INTO ").append(dst)
          .append(" USING ")
          .append(fromSource.getSqlString(this))
          .append(" ON ")
          .append(BeeUtils.parenthesize(su.getFromJoin().getSqlString(this)))
          .append(" WHEN MATCHED THEN UPDATE");
    }
    query.append(" SET ");

    Map<String, IsSql> updates = su.getUpdates();
    boolean first = true;

    for (String field : updates.keySet()) {
      if (first) {
        first = false;
      } else {
        query.append(", ");
      }
      query.append(SqlUtils.name(field).getSqlString(this));

      IsSql value = updates.get(field);
      query.append("=")
          .append(value instanceof SqlSelect
              ? BeeUtils.parenthesize(value.getSqlString(this))
              : value.getSqlString(this));
    }
    IsCondition whereClause = su.getWhere();

    if (whereClause != null) {
      String wh = whereClause.getSqlString(this);

      if (!BeeUtils.isEmpty(wh)) {
        query.append(" WHERE ").append(wh);
      }
    }
    return query.toString();
  }

  protected boolean isEmpty(Object x) {
    boolean ok;

    if (x == null) {
      ok = true;
    } else if (x instanceof String) {
      ok = ((String) x).isEmpty() || ((String) x).trim().isEmpty();
    } else if (x instanceof CharSequence) {
      ok = ((CharSequence) x).length() == 0
          || ((CharSequence) x).toString().trim().isEmpty();
    } else if (x instanceof Number) {
      ok = BeeUtils.isZero(((Number) x).doubleValue());
    } else if (x instanceof Boolean) {
      ok = !(Boolean) x;
    } else if (x instanceof Collection) {
      ok = ((Collection<?>) x).isEmpty();
    } else if (x instanceof Map) {
      ok = ((Map<?, ?>) x).isEmpty();
    } else if (ArrayUtils.isArray(x)) {
      ok = ArrayUtils.length(x) <= 0;
    } else if (x instanceof Enumeration) {
      ok = !((Enumeration<?>) x).hasMoreElements();
    } else if (x instanceof HasLength) {
      ok = ((HasLength) x).getLength() <= 0;
    } else {
      ok = false;
    }
    return ok;
  }
  
  protected String sqlCondition(Operator operator, Map<String, String> params) {
    String expression = params.get("expression");
    String value = params.get("value" + 0);

    switch (operator) {
      case IS_NULL:
      case NOT_NULL:
        return BeeUtils.joinWords(
            expression, "IS", (operator == Operator.NOT_NULL ? "NOT" : ""), "NULL");

      case IN:
        return BeeUtils.joinWords(expression, "IN", value);

      case EQ:
      case NE:
      case LT:
      case GT:
      case LE:
      case GE:
        return BeeUtils.joinWords(expression, operator.toTextString(), value);

      case STARTS:
      case ENDS:
      case CONTAINS:
      case MATCHES:
        value = value.replace("|", "||").replace("%", "|%").replace("_", "|_");

        if (operator == Operator.MATCHES) {
          value = value.replace(Operator.CHAR_ANY, "%").replace(Operator.CHAR_ONE, "_");
        } else {
          value = value.replaceFirst("^(" + sqlTransform(")(.*)(") + ")$",
              "$1" + (operator != Operator.STARTS ? "%" : "")
                  + "$2" + (operator != Operator.ENDS ? "%" : "") + "$3");
        }
        return BeeUtils.joinWords(
            expression, sqlKeyword(SqlKeyword.LIKE, null), value, "ESCAPE '|'");
    }
    Assert.untouchable();
    return null;
  }

  protected String sqlFunction(SqlFunction function, Map<String, Object> params) {
    switch (function) {
      case BITAND:
        return "(" + params.get("expression") + " & " + params.get("value") + ")";

      case IF:
        return BeeUtils.joinWords(
            "CASE WHEN", params.get("condition"),
            "THEN", params.get("ifTrue"),
            "ELSE", params.get("ifFalse"),
            "END");

      case CASE:
        StringBuilder xpr = new StringBuilder("CASE ")
            .append(params.get("expression"));

        int cnt = (params.size() - 2) / 2;

        for (int i = 0; i < cnt; i++) {
          xpr.append(" WHEN ")
              .append(params.get("case" + i))
              .append(" THEN ")
              .append(params.get("value" + i));
        }
        xpr.append(" ELSE ")
            .append(params.get("caseElse"))
            .append(" END");

        return xpr.toString();

      case CAST:
        return BeeUtils.joinWords(
            "CAST(" + params.get("expression"),
            "AS",
            sqlType((SqlDataType) params.get("type"),
                (Integer) params.get("precision"),
                (Integer) params.get("scale")) + ")");

      case MIN:
      case MAX:
      case SUM:
      case AVG:
      case COUNT:
        String expression = (String) params.get("expression");
        if (BeeUtils.isEmpty(expression)) {
          expression = "*";
        }
        return function + "(" + expression + ")";

      case SUM_DISTINCT:
        return "SUM(DISTINCT " + params.get("expression") + ")";

      case AVG_DISTINCT:
        return "AVG(DISTINCT " + params.get("expression") + ")";

      case COUNT_DISTINCT:
        return "COUNT(DISTINCT " + params.get("expression") + ")";

      case PLUS:
      case MINUS:
      case MULTIPLY:
      case DIVIDE:
      case BULK:
        xpr = new StringBuilder(BeeUtils.transform(params.get("member" + 0)));
        String op;

        switch (function) {
          case PLUS:
            op = " + ";
            break;
          case MINUS:
            op = " - ";
            break;
          case MULTIPLY:
            op = " * ";
            break;
          case DIVIDE:
            op = " / ";
            break;
          default:
            op = "";
            break;
        }
        for (int i = 1; i < params.size(); i++) {
          xpr.append(op).append(params.get("member" + i));
        }
        return function != SqlFunction.BULK
            ? BeeUtils.parenthesize(xpr.toString()) : xpr.toString();

      case NVL:
        xpr = new StringBuilder(BeeUtils.transform(params.get("member" + 0)));

        for (int i = 1; i < params.size(); i++) {
          xpr.append(", ").append(params.get("member" + i));
        }
        return "COALESCE(" + xpr.toString() + ")";

      case CONCAT:
        xpr = new StringBuilder(BeeUtils.transform(params.get("member" + 0)));

        for (int i = 1; i < params.size(); i++) {
          xpr.append(" || ").append(params.get("member" + i));
        }
        return xpr.toString();

      case LENGTH:
        return "LENGTH(" + params.get("expression") + ")";

      case SUBSTRING:
        xpr = new StringBuilder("SUBSTR(")
            .append(params.get("expression"))
            .append(",")
            .append(params.get("pos"));

        if (params.containsKey("len")) {
          xpr.append(",").append(params.get("len"));
        }
        return xpr.append(")").toString();

      case LEFT:
        Map<String, Object> newParams = Maps.newHashMap(params);
        newParams.put("pos", 1);
        return sqlFunction(SqlFunction.SUBSTRING, newParams);

      case RIGHT:
        newParams = Maps.newHashMap(params);
        newParams.put("pos", BeeUtils.joinWords(
            sqlFunction(SqlFunction.LENGTH, params), "-", params.get("len"), "+", "1"));

        return sqlFunction(SqlFunction.SUBSTRING, newParams);
    }
    Assert.untouchable();
    return null;
  }

  protected String sqlKeyword(SqlKeyword option, Map<String, Object> params) {
    switch (option) {
      case CREATE_SCHEMA:
        return BeeUtils.joinWords("CREATE SCHEMA", params.get("schema"));

      case CREATE_INDEX:
        return BeeUtils.joinWords(
            "CREATE", isEmpty(params.get("isUnique")) ? "" : "UNIQUE",
            "INDEX", params.get("name"),
            "ON", params.get("table"),
            BeeUtils.parenthesize(params.get("fields")));

      case ADD_CONSTRAINT:
        return BeeUtils.joinWords(
            "ALTER TABLE", params.get("table"),
            "ADD CONSTRAINT", params.get("name"),
            sqlKeyword((SqlKeyword) params.get("type"), params));

      case PRIMARY_KEY:
        return BeeUtils.joinWords("PRIMARY KEY", BeeUtils.parenthesize(params.get("fields")));

      case FOREIGN_KEY:
        String foreign = BeeUtils.joinWords(
            "FOREIGN KEY", BeeUtils.parenthesize(params.get("fields")),
            "REFERENCES", params.get("refTable"), BeeUtils.parenthesize(params.get("refFields")));

        if (params.get("cascade") != null) {
          foreign = BeeUtils.joinWords(foreign, "ON DELETE",
              sqlKeyword((SqlKeyword) params.get("cascade"), params));
        }
        return foreign;

      case CREATE_TRIGGER:
        Assert.notImplemented();
        return null;

      case DB_NAME:
        return "";

      case DB_SCHEMA:
        return "";

      case DB_SCHEMAS:
        IsCondition wh = null;

        Object prm = params.get("dbName");
        if (!isEmpty(prm)) {
          wh = SqlUtils.equal("t", "catalog_name", prm);
        }
        prm = params.get("schema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("t", "schema_name", prm));
        }
        return new SqlSelect()
            .addFields("t", "schema_name")
            .addFrom("information_schema.schemata", "t")
            .setWhere(wh)
            .getSqlString(this);

      case DB_TABLES:
        wh = null;

        prm = params.get("dbName");
        if (!isEmpty(prm)) {
          wh = SqlUtils.equal("t", "table_catalog", prm);
        }
        prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("t", "table_schema", prm));
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("t", "table_name", prm));
        }
        return new SqlSelect()
            .addField("t", "table_name", SqlConstants.TBL_NAME)
            .addField("t", "table_rows", SqlConstants.ROW_COUNT)
            .addFrom("information_schema.tables", "t")
            .setWhere(wh)
            .getSqlString(this);

      case DB_FIELDS:
        wh = null;

        prm = params.get("dbName");
        if (!isEmpty(prm)) {
          wh = SqlUtils.equal("c", "table_catalog", prm);
        }
        prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("c", "table_schema", prm));
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("c", "table_name", prm));
        }
        return new SqlSelect()
            .addField("c", "table_name", SqlConstants.TBL_NAME)
            .addField("c", "column_name", SqlConstants.FLD_NAME)
            .addField("c", "is_nullable", SqlConstants.FLD_NULL)
            .addField("c", "data_type", SqlConstants.FLD_TYPE)
            .addField("c", "character_maximum_length", SqlConstants.FLD_LENGTH)
            .addField("c", "numeric_precision", SqlConstants.FLD_PRECISION)
            .addField("c", "numeric_scale", SqlConstants.FLD_SCALE)
            .addFrom("information_schema.columns", "c")
            .setWhere(wh)
            .addOrder("c", "ordinal_position")
            .getSqlString(this);

      case DB_KEYS:
        wh = null;

        prm = params.get("dbName");
        if (!isEmpty(prm)) {
          wh = SqlUtils.equal("k", "constraint_catalog", prm);
        }
        prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("k", "constraint_schema", prm));
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("k", "table_name", prm));
        }
        prm = params.get("keyTypes");
        if (!isEmpty(prm)) {
          IsCondition typeWh = null;

          for (SqlKeyword type : (SqlKeyword[]) prm) {
            String tp;

            switch (type) {
              case PRIMARY_KEY:
                tp = "PRIMARY KEY";
                break;

              case FOREIGN_KEY:
                tp = "FOREIGN KEY";
                break;

              default:
                tp = null;
            }
            if (!BeeUtils.isEmpty(tp)) {
              typeWh = SqlUtils.or(typeWh, SqlUtils.equal("k", "constraint_type", tp));
            }
          }
          if (!isEmpty(typeWh)) {
            wh = SqlUtils.and(wh, typeWh);
          }
        }
        return new SqlSelect()
            .addField("k", "table_name", SqlConstants.TBL_NAME)
            .addField("k", "constraint_name", SqlConstants.KEY_NAME)
            .addField("k", "constraint_type", SqlConstants.KEY_TYPE)
            .addFrom("information_schema.table_constraints", "k")
            .setWhere(wh)
            .getSqlString(this);

      case DB_FOREIGNKEYS:
        wh = null;

        prm = params.get("dbName");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh,
              SqlUtils.equal("c", "constraint_catalog", prm),
              SqlUtils.equal("t", "table_catalog", prm));
        }
        prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh,
              SqlUtils.equal("c", "constraint_schema", prm),
              SqlUtils.equal("t", "table_schema", prm));
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("t", "table_name", prm));
        }
        prm = params.get("refTable");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("r", "table_name", prm));
        }
        return new SqlSelect()
            .addField("t", "table_name", SqlConstants.TBL_NAME)
            .addField("c", "constraint_name", SqlConstants.KEY_NAME)
            .addField("r", "table_name", SqlConstants.FK_REF_TABLE)
            .addFrom("information_schema.referential_constraints", "c")
            .addFromInner("information_schema.table_constraints", "t",
                SqlUtils.joinUsing("c", "t", "constraint_name"))
            .addFromInner("information_schema.table_constraints", "r",
                SqlUtils.join("c", "unique_constraint_name", "r", "constraint_name"))
            .setWhere(wh)
            .getSqlString(this);

      case DB_INDEXES:
        Assert.notImplemented();
        return null;

      case DB_TRIGGERS:
        wh = null;

        prm = params.get("dbName");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh,
              SqlUtils.equal("t", "trigger_catalog", prm),
              SqlUtils.equal("t", "event_object_catalog", prm));
        }
        prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh,
              SqlUtils.equal("t", "trigger_schema", prm),
              SqlUtils.equal("t", "event_object_schema", prm));
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("t", "event_object_table", prm));
        }
        return new SqlSelect()
            .addField("t", "event_object_table", SqlConstants.TBL_NAME)
            .addField("t", "trigger_name", SqlConstants.TRIGGER_NAME)
            .addFrom("information_schema.triggers", "t")
            .setWhere(wh)
            .getSqlString(this);

      case DROP_TABLE:
        return "DROP TABLE " + params.get("table");

      case DROP_FOREIGNKEY:
        return BeeUtils.joinWords(
            "ALTER TABLE", params.get("table"),
            "DROP CONSTRAINT", params.get("name"));

      case RENAME_TABLE:
        return BeeUtils.joinWords(
            "ALTER TABLE", params.get("nameFrom"), "RENAME TO", params.get("nameTo"));

      case SET_PARAMETER:
        Assert.notImplemented();
        return null;

      case TEMPORARY:
        return "TEMPORARY ";

      case TEMPORARY_NAME:
        return (String) params.get("name");

      case DELETE:
        return "CASCADE";

      case SET_NULL:
        return "SET NULL";

      case LIKE:
        return "LIKE";
    }
    Assert.untouchable();
    return null;
  }

  protected abstract String sqlQuote(String value);

  protected String sqlTransform(Object x) {
    String s = null;

    if (x == null) {
      s = "null";

    } else {
      Object val;

      if (x instanceof Value) {
        val = ((Value) x).getObjectValue();
      } else {
        val = x;
      }
      if (val instanceof Boolean) {
        s = (Boolean) val ? "1" : "0";

      } else if (val instanceof JustDate) {
        s = BeeUtils.transform(((JustDate) val).getTime());

      } else if (val instanceof Date) {
        s = BeeUtils.transform(((Date) val).getTime());

      } else if (val instanceof DateTime) {
        s = BeeUtils.transform(((DateTime) val).getTime());

      } else if (val instanceof Number) {
        s = BeeUtils.removeTrailingZeros(BeeUtils.transformNoTrim(val));

      } else {
        s = BeeUtils.transformNoTrim(val);

        if (val instanceof CharSequence) {
          s = "'" + s.replace("'", "''") + "'";
        }
      }
    }
    return s;
  }

  protected String sqlType(SqlDataType type, int precision, int scale) {
    switch (type) {
      case BOOLEAN:
        return "BIT";
      case INTEGER:
        return "INTEGER";
      case LONG:
      case DATE:
      case DATETIME:
        return "BIGINT";
      case DOUBLE:
        return "DOUBLE";
      case DECIMAL:
        return "NUMERIC(" + precision + ", " + scale + ")";
      case CHAR:
        return "CHAR(" + precision + ")";
      case STRING:
        return "VARCHAR(" + precision + ")";
      case TEXT:
        return "TEXT";
    }
    Assert.untouchable();
    return null;
  }
}
