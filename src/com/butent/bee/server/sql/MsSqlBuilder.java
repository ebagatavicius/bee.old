package com.butent.bee.server.sql;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.SqlConstants.SqlKeyword;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Map;

/**
 * Contains specific requirements for SQL statement building for Microsoft SQL server.
 */

class MsSqlBuilder extends SqlBuilder {

  @Override
  protected String getCreate(SqlCreate sc) {
    Assert.notNull(sc);
    Assert.state(!sc.isEmpty());

    if (BeeUtils.isEmpty(sc.getDataSource())) {
      return super.getCreate(sc);
    }
    return sc.getDataSource().getSqlString(this).replaceFirst(" FROM ",
        " INTO " + SqlUtils.name(sc.getTarget()).getSqlString(this) + " FROM ");
  }

  @Override
  protected String getSelect(SqlSelect ss) {
    String sql = super.getSelect(ss);
    int limit = ss.getLimit();
    int offset = ss.getOffset();

    if (BeeUtils.allEmpty(limit, offset)) {
      return sql;
    }
    String top = "";
    String numbering = "";
    String idAlias = "";
    boolean hasUnion = !BeeUtils.isEmpty(ss.getUnion());

    if (BeeUtils.isPositive(limit)) {
      top = BeeUtils.concat(1, "TOP", offset + limit);
    }
    if (BeeUtils.isPositive(offset)) {
      String order = "ORDER BY (SELECT 0)";

      if (!BeeUtils.isEmpty(ss.getOrderBy())) {
        int idx = sql.lastIndexOf(" ORDER BY ");
        order = sql.substring(idx + 1);
        sql = sql.substring(0, idx);
      }
      idAlias = sqlQuote(SqlUtils.uniqueName());
      numbering = BeeUtils.concat(1,
          "ROW_NUMBER() OVER", BeeUtils.parenthesize(order), "AS", idAlias + ",");
    }
    if (hasUnion) {
      String queryAlias = sqlQuote(SqlUtils.uniqueName());

      sql = BeeUtils.concat(1,
          "SELECT", top, numbering, queryAlias + ".*",
          "FROM", BeeUtils.parenthesize(sql), queryAlias);
    } else {
      String select = "SELECT " + (ss.isDistinctMode() ? "DISTINCT " : "");
      sql = BeeUtils.concat(1,
          select, top, numbering, sql.substring(select.length()));
    }
    if (!BeeUtils.isEmpty(idAlias)) {
      String queryAlias = sqlQuote(SqlUtils.uniqueName());

      sql = BeeUtils.concat(1,
          "SELECT", queryAlias + ".*",
          "FROM", BeeUtils.parenthesize(sql), queryAlias,
          "WHERE", queryAlias + "." + idAlias, ">", offset);
    }
    return sql;
  }

  @Override
  protected String getUpdate(SqlUpdate su) {
    Assert.notNull(su);
    Assert.state(!su.isEmpty());

    IsFrom fromSource = su.getFromSource();

    if (fromSource == null) {
      return super.getUpdate(su);
    }
    StringBuilder query = new StringBuilder("MERGE INTO ")
        .append(SqlUtils.name(su.getTarget()).getSqlString(this))
        .append(" USING ")
        .append(fromSource.getSqlString(this))
        .append(" ON ")
        .append(su.getFromJoin().getSqlString(this))
        .append(" WHEN MATCHED");

    IsCondition whereClause = su.getWhere();

    if (!BeeUtils.isEmpty(whereClause)) {
      String wh = whereClause.getSqlString(this);

      if (!BeeUtils.isEmpty(wh)) {
        query.append(" AND ").append(wh);
      }
    }
    query.append(" THEN UPDATE SET ");

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
    return query.append(";").toString();
  }

  @Override
  protected String sqlFunction(SqlFunction function, Map<String, Object> params) {
    switch (function) {
      case CONCAT:
        StringBuilder xpr = new StringBuilder(BeeUtils.transform(params.get("member" + 0)));

        for (int i = 1; i < params.size(); i++) {
          xpr.append(" + ").append(params.get("member" + i));
        }
        return xpr.toString();

      case LENGTH:
        return "LEN(" + params.get("expression") + ")";

      case SUBSTRING:
        xpr = new StringBuilder("SUBSTRING(")
            .append(params.get("expression"))
            .append(",")
            .append(params.get("pos"))
            .append(",");

        if (params.containsKey("len")) {
          xpr.append(params.get("len"));
        } else {
          xpr.append(1e6);
        }
        return xpr.append(")").toString();

      case LEFT:
        return "LEFT(" + params.get("expression") + "," + params.get("len") + ")";

      case RIGHT:
        return "RIGHT(" + params.get("expression") + "," + params.get("len") + ")";

      default:
        return super.sqlFunction(function, params);
    }
  }

  @Override
  protected String sqlKeyword(SqlKeyword option, Map<String, Object> params) {
    switch (option) {
      case CREATE_INDEX:
        String text = super.sqlKeyword(option, params);
        String field = (String) params.get("fields");

        if (!BeeUtils.isEmpty(params.get("isUnique")) && !field.contains(",")) {
          text = BeeUtils.concat(1, text, "WHERE", field, "IS NOT NULL");
        }
        return text;

      case CREATE_TRIGGER:
        @SuppressWarnings("unchecked")
        List<String[]> content = (List<String[]>) params.get("content");
        text = "SET NOCOUNT ON;";

        for (String[] entry : content) {
          String fldName = entry[0];
          String relTable = entry[1];
          String relField = entry[2];

          text = BeeUtils.concat(1, text,
              new SqlDelete(relTable)
                  .setWhere(SqlUtils.in(relTable, relField, "deleted", fldName, null))
                  .getQuery(),
              ";");
        }
        return BeeUtils.concat(1,
            "CREATE TRIGGER", params.get("name"),
            "ON", params.get("table"), params.get("timing"), params.get("event"),
            "AS BEGIN", text, "END;");

      case DB_NAME:
        return "SELECT db_name() AS " + sqlQuote("dbName");

      case DB_SCHEMA:
        return "SELECT schema_name() AS " + sqlQuote("dbSchema");

      case DB_TABLES:
        IsCondition wh = SqlUtils.and(SqlUtils.equal("o", "type", "U"),
            SqlUtils.equal("o", "is_ms_shipped", 0),
            SqlUtils.less("p", "index_id", 2));

        Object prm = params.get("dbSchema");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("s", "name", prm));
        }
        prm = params.get("table");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("o", "name", prm));
        }
        return new SqlSelect()
            .addField("o", "name", SqlConstants.TBL_NAME)
            .addSum("p", "rows", SqlConstants.ROW_COUNT)
            .addFrom("sys.objects", "o")
            .addFromInner("sys.partitions", "p", SqlUtils.joinUsing("o", "p", "object_id"))
            .addFromInner("sys.schemas", "s", SqlUtils.joinUsing("o", "s", "schema_id"))
            .setWhere(wh)
            .addGroup("o", "name")
            .getSqlString(this);

      case DB_INDEXES:
        wh = SqlUtils.and(SqlUtils.notNull("i", "name"), SqlUtils.equal("o", "type", "U"),
            SqlUtils.equal("o", "is_ms_shipped", 0));

        prm = params.get("dbSchema");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("s", "name", prm));
        }
        prm = params.get("table");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("o", "name", prm));
        }
        return new SqlSelect()
            .addField("o", "name", SqlConstants.TBL_NAME)
            .addField("i", "name", SqlConstants.KEY_NAME)
            .addFrom("sys.indexes", "i")
            .addFromInner("sys.objects", "o", SqlUtils.joinUsing("i", "o", "object_id"))
            .addFromInner("sys.schemas", "s", SqlUtils.joinUsing("o", "s", "schema_id"))
            .setWhere(wh)
            .getSqlString(this);

      case DB_TRIGGERS:
        wh = null;

        prm = params.get("dbSchema");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("s", "name", prm));
        }
        prm = params.get("table");
        if (!BeeUtils.isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equal("o", "name", prm));
        }
        return new SqlSelect()
            .addField("o", "name", SqlConstants.TBL_NAME)
            .addField("t", "name", SqlConstants.TRIGGER_NAME)
            .addFrom("sys.triggers", "t")
            .addFromInner("sys.objects", "o", SqlUtils.join("t", "parent_id", "o", "object_id"))
            .addFromInner("sys.schemas", "s", SqlUtils.joinUsing("o", "s", "schema_id"))
            .setWhere(wh)
            .getSqlString(this);

      case TEMPORARY:
        return "";

      case TEMPORARY_NAME:
        return "#" + params.get("name");

      case RENAME_TABLE:
        return BeeUtils.concat(1,
            "sp_rename", params.get("nameFrom"), ",", params.get("nameTo"));

      default:
        return super.sqlKeyword(option, params);
    }
  }

  @Override
  protected String sqlQuote(String value) {
    return "[" + value + "]";
  }

  @Override
  protected String sqlType(SqlDataType type, int precision, int scale) {
    switch (type) {
      case DOUBLE:
        return "FLOAT";
      case TEXT:
        return "VARCHAR(MAX)";
      default:
        return super.sqlType(type, precision, scale);
    }
  }
}
