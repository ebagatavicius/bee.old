package com.butent.bee.shared.sql;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.sql.BeeConstants.DataTypes;
import com.butent.bee.shared.sql.BeeConstants.Keywords;
import com.butent.bee.shared.sql.SqlCreate.SqlField;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SqlBuilder {

  protected String sqlKeyword(Keywords option, Map<String, Object> params) {
    switch (option) {
      case NOT_NULL:
        return "NOT NULL";

      case CREATE_INDEX:
        return BeeUtils.concat(1,
            "CREATE", (Boolean) params.get("unique") ? "UNIQUE" : "",
            "INDEX", params.get("name"),
            "ON", params.get("table"),
            BeeUtils.parenthesize(params.get("fields")));

      case ADD_CONSTRAINT:
        return BeeUtils.concat(1,
            "ALTER TABLE", params.get("table"),
            "ADD CONSTRAINT", params.get("name"),
            sqlKeyword((Keywords) params.get("type"), params));

      case PRIMARYKEY:
        return BeeUtils.concat(1,
            "PRIMARY KEY", BeeUtils.parenthesize(params.get("fields")));

      case FOREIGNKEY:
        String foreign = BeeUtils.concat(1,
            "FOREIGN KEY", BeeUtils.parenthesize(params.get("field")),
            "REFERENCES", params.get("refTable"), BeeUtils.parenthesize(params.get("refField")));

        Keywords action = (Keywords) params.get("action");
        if (!BeeUtils.isEmpty(action)) {
          foreign = BeeUtils.concat(1,
              foreign, "ON DELETE", sqlKeyword(action, null));
        }
        return foreign;

      case CASCADE:
        return "CASCADE";

      case SET_NULL:
        return "SET NULL";

      case DB_NAME:
        return "";

      case DB_SCHEMA:
        return "";

      case DB_TABLES:
        IsCondition wh = null;

        Object prm = params.get("dbName");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.equal("t", "table_catalog", prm);
        }
        prm = params.get("dbSchema");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("t", "table_schema", prm));
        }
        prm = params.get("table");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("t", "table_name", prm));
        }
        return new SqlSelect()
          .addFields("t", "table_name")
          .addFrom("information_schema.tables", "t")
          .setWhere(wh)
          .getQuery(this);

      case DB_FOREIGNKEYS:
        wh = null;

        prm = params.get("dbName");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.and(wh,
              SqlUtils.equal("c", "constraint_catalog", prm),
              SqlUtils.equal("t", "table_catalog", prm));
        }
        prm = params.get("dbSchema");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.and(wh,
              SqlUtils.equal("c", "constraint_schema", prm),
              SqlUtils.equal("t", "table_schema", prm));
        }
        prm = params.get("table");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("t", "table_name", prm));
        }
        prm = params.get("refTable");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("r", "table_name", prm));
        }
        return new SqlSelect()
          .addField("c", "constraint_name", "Name")
          .addField("t", "table_name", "TblName")
          .addField("r", "table_name", "RefTblName")
          .addFrom("information_schema.referential_constraints", "c")
          .addFromInner("information_schema.table_constraints", "t",
              SqlUtils.joinUsing("c", "t", "constraint_name"))
          .addFromInner("information_schema.table_constraints", "r",
              SqlUtils.join("c", "unique_constraint_name", "r", "constraint_name"))
          .setWhere(wh)
          .getQuery(this);

      case DROP_TABLE:
        return "DROP TABLE " + params.get("table");

      case DROP_FOREIGNKEY:
        return BeeUtils.concat(1,
            "ALTER TABLE", params.get("table"),
            "DROP CONSTRAINT", params.get("name"));

      case TEMPORARY:
        return "TEMPORARY ";

      case TEMPORARY_NAME:
        return (String) params.get("name");

      case BITAND:
        return "(" + params.get("expression") + "&" + params.get("value") + ")";

      case IF:
        return BeeUtils.concat(1,
            "CASE WHEN", params.get("condition"),
            "THEN", params.get("ifTrue"),
            "ELSE", params.get("ifFalse"),
            "END");

      default:
        Assert.unsupported("Unsupported keyword: " + option);
        return null;
    }
  }

  protected abstract String sqlQuote(String value);

  protected String sqlTransform(Object x) {
    String s = BeeUtils.transformNoTrim(x);

    if (x instanceof CharSequence) {
      s = "'" + s.replaceAll("'", "''") + "'";
    }
    return s;
  }

  protected Object sqlType(DataTypes type, int precision, int scale) {
    switch (type) {
      case BOOLEAN:
        return "BIT";
      case INTEGER:
        return "INTEGER";
      case LONG:
        return "BIGINT";
      case FLOAT:
        return "FLOAT";
      case DOUBLE:
        return "DOUBLE";
      case NUMERIC:
        return "NUMERIC(" + precision + ", " + scale + ")";
      case CHAR:
        return "CHAR(" + precision + ")";
      case STRING:
        return "VARCHAR(" + precision + ")";
      default:
        Assert.unsupported("Unsupported data type: " + type.name());
        return null;
    }
  }

  String getCommand(SqlCommand sc, boolean paramMode) {
    Assert.notNull(sc);
    Assert.state(!sc.isEmpty());

    Map<String, Object> params = new HashMap<String, Object>();
    Map<String, Object> paramMap = sc.getParameters();

    if (!BeeUtils.isEmpty(paramMap)) {
      for (String prm : paramMap.keySet()) {
        Object value = paramMap.get(prm);

        if (value instanceof IsSql) {
          value = ((IsSql) value).getSqlString(this, paramMode);
        }
        params.put(prm, value);
      }
    }
    return sqlKeyword(sc.getCommand(), params);
  }

  String getCreate(SqlCreate sc, boolean paramMode) {
    Assert.notNull(sc);
    Assert.state(!sc.isEmpty());

    StringBuilder query = new StringBuilder("CREATE ");

    if (sc.isTemporary()) {
      query.append(sqlKeyword(Keywords.TEMPORARY, null));
    }
    query.append("TABLE ");

    query.append(sc.getTarget().getSqlString(this, paramMode));

    List<SqlField> fieldList = sc.getFields();

    if (!BeeUtils.isEmpty(sc.getDataSource())) {
      query.append(" AS ").append(sc.getDataSource().getSqlString(this, paramMode));
    } else {
      query.append(" (");

      for (int i = 0; i < fieldList.size(); i++) {
        if (i > 0) {
          query.append(", ");
        }
        SqlField field = fieldList.get(i);
        query.append(field.getName().getSqlString(this, paramMode))
          .append(" ").append(sqlType(field.getType(), field.getPrecision(), field.getScale()));

        for (Keywords opt : field.getOptions()) {
          query.append(" ").append(sqlKeyword(opt, null));
        }
      }
      query.append(")");
    }
    return query.toString();
  }

  String getDelete(SqlDelete sd, boolean paramMode) {
    Assert.notNull(sd);
    Assert.state(!sd.isEmpty());

    StringBuilder query = new StringBuilder("DELETE ");

    query.append(" FROM ").append(sd.getTarget().getSqlString(this, paramMode));

    List<IsFrom> fromList = sd.getFrom();

    if (!BeeUtils.isEmpty(fromList)) {
      query.append(" FROM ");

      for (IsFrom from : fromList) {
        query.append(from.getJoinMode()).append(from.getSqlString(this, paramMode));
      }
    }
    String wh = sd.getWhere().getSqlString(this, paramMode);

    if (!BeeUtils.isEmpty(wh)) {
      query.append(" WHERE ").append(wh);
    }
    return query.toString();
  }

  String getInsert(SqlInsert si, boolean paramMode) {
    Assert.notNull(si);
    Assert.state(!si.isEmpty());

    StringBuilder query = new StringBuilder("INSERT INTO ");

    query.append(si.getTarget().getSqlString(this, paramMode));

    List<IsExpression> fieldList = si.getFields();

    query.append(" (");

    for (int i = 0; i < fieldList.size(); i++) {
      if (i > 0) {
        query.append(", ");
      }
      IsExpression field = fieldList.get(i);
      query.append(field.getSqlString(this, paramMode));
    }
    query.append(") ");

    if (!BeeUtils.isEmpty(si.getDataSource())) {
      query.append(si.getDataSource().getSqlString(this, paramMode));
    } else {
      List<IsExpression> valueList = si.getValues();

      if (!BeeUtils.isEmpty(valueList)) {
        query.append("VALUES (");

        for (int i = 0; i < valueList.size(); i++) {
          if (i > 0) {
            query.append(", ");
          }
          IsExpression value = valueList.get(i);
          query.append(value.getSqlString(this, paramMode));
        }
        query.append(")");
      }
    }
    return query.toString();
  }

  String getQuery(SqlSelect ss, boolean paramMode) {
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
      query.append(field.getSqlString(this, paramMode));

      IsExpression alias = fldEntry[SqlSelect.FIELD_ALIAS];

      if (!BeeUtils.isEmpty(alias)) {
        query.append(" AS ").append(alias.getSqlString(this, paramMode));
      }
    }
    List<IsFrom> fromList = ss.getFrom();

    query.append(" FROM ");

    for (IsFrom from : fromList) {
      query.append(from.getJoinMode()).append(from.getSqlString(this, paramMode));
    }
    IsCondition whereClause = ss.getWhere();

    if (!BeeUtils.isEmpty(whereClause)) {
      String wh = whereClause.getSqlString(this, paramMode);

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
        String group = groupList.get(i).getSqlString(this, paramMode);
        query.append(group);
      }
    }
    IsCondition havingClause = ss.getHaving();

    if (!BeeUtils.isEmpty(havingClause)) {
      query.append(" HAVING ")
        .append(havingClause.getSqlString(this, paramMode));
    }
    List<SqlSelect> unionList = ss.getUnion();

    if (!BeeUtils.isEmpty(unionList)) {
      for (SqlSelect union : unionList) {
        query.append(ss.isUnionAllMode() ? " UNION ALL " : " UNION ")
          .append("(").append(union.getSqlString(this, paramMode)).append(")");
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
        IsExpression order = BeeUtils.isEmpty(ss.getUnion())
            ? SqlUtils.field(orderEntry[SqlSelect.ORDER_SRC], orderEntry[SqlSelect.ORDER_FLD])
            : SqlUtils.name(orderEntry[SqlSelect.ORDER_FLD]);

        query.append(order.getSqlString(this, paramMode))
          .append(orderEntry[SqlSelect.ORDER_DESC]);
      }
    }
    return query.toString();
  }

  String getUpdate(SqlUpdate su, boolean paramMode) {
    Assert.notNull(su);
    Assert.state(!su.isEmpty());

    StringBuilder query = new StringBuilder("UPDATE ");

    query.append(su.getTarget().getSqlString(this, paramMode));

    List<IsExpression[]> updates = su.getUpdates();

    query.append(" SET ");

    for (int i = 0; i < updates.size(); i++) {
      if (i > 0) {
        query.append(", ");
      }
      IsExpression[] updateEntry = updates.get(i);
      IsExpression field = updateEntry[SqlUpdate.FIELD_INDEX];
      query.append(field.getSqlString(this, paramMode));

      IsExpression value = updateEntry[SqlUpdate.VALUE_INDEX];
      query.append("=").append(value.getSqlString(this, paramMode));
    }
    List<IsFrom> fromList = su.getFrom();

    if (!BeeUtils.isEmpty(fromList)) {
      query.append(" FROM ");

      for (IsFrom from : fromList) {
        query.append(from.getJoinMode()).append(from.getSqlString(this, paramMode));
      }
    }
    IsCondition whereClause = su.getWhere();

    if (!BeeUtils.isEmpty(whereClause)) {
      String wh = whereClause.getSqlString(this, paramMode);

      if (!BeeUtils.isEmpty(wh)) {
        query.append(" WHERE ").append(wh);
      }
    }
    return query.toString();
  }
}
