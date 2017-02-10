package com.butent.bee.server.sql;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeConst.SqlEngine;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.SqlConstants.SqlKeyword;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerEvent;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerTiming;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Contains specific requirements for SQL statement building for Microsoft SQL server.
 */

class MsSqlBuilder extends SqlBuilder {

  @Override
  public SqlEngine getEngine() {
    return SqlEngine.MSSQL;
  }

  @Override
  protected String getAuditTrigger(String auditTable, String idName, Collection<String> fields) {
    StringBuilder body = new StringBuilder();
    String insert = "INSERT INTO " + auditTable
        + " (" + BeeUtils.join(",", sqlQuote(AUDIT_FLD_TIME), sqlQuote(AUDIT_FLD_USER),
            sqlQuote(AUDIT_FLD_TX), sqlQuote(AUDIT_FLD_MODE), sqlQuote(AUDIT_FLD_ID),
            sqlQuote(AUDIT_FLD_FIELD), sqlQuote(AUDIT_FLD_VALUE))
        + ") SELECT @_time,@_user,@_transaction,@_mode,";

    body.append("BEGIN ")
        .append("SET NOCOUNT ON;")
        .append("DECLARE @_time BIGINT, @_user BIGINT, @_mode CHAR(1), @_transaction BIGINT;")
        .append("SET @_time=CAST(DATEDIFF(s,'19700101',GETUTCDATE()) AS BIGINT)*1000")
        .append("+DATEPART(ms,GETUTCDATE());")
        .append("SET @_user=CAST(CONTEXT_INFO() AS BIGINT);")
        .append("SELECT @_transaction=transaction_id FROM sys.dm_tran_current_transaction;")
        .append("IF NOT EXISTS(SELECT * FROM inserted)")
        .append("  BEGIN")
        .append("  SET @_mode='D';")
        .append(insert).append("deleted." + idName + ",NULL,NULL FROM deleted;")
        .append("  END;")
        .append("ELSE")
        .append("  BEGIN")
        .append("  IF EXISTS(SELECT * FROM deleted)")
        .append("    SET @_mode='U';")
        .append("  ELSE")
        .append("    SET @_mode='I';");

    insert = insert + "inserted." + idName + ",";

    for (String field : fields) {
      String fld = sqlQuote(field);

      body.append("IF UPDATE(").append(fld).append(") ")
          .append(insert).append("'").append(field).append("',inserted.").append(fld)
          .append(" FROM inserted WHERE @_mode='U' OR inserted.").append(fld)
          .append(" IS NOT NULL;");
    }
    body.append("END;END;");

    return body.toString();
  }

  @Override
  protected String getCreate(SqlCreate sc) {
    Assert.notNull(sc);
    Assert.state(!sc.isEmpty());

    if (sc.getDataSource() == null) {
      return super.getCreate(sc);
    }
    return sc.getDataSource().getSqlString(this).replaceFirst(" FROM ",
        " INTO " + SqlUtils.name(sc.getTarget()).getSqlString(this) + " FROM ");
  }

  @Override
  protected String getRelationTrigger(List<Map<String, String>> fields) {
    StringBuilder body = new StringBuilder("BEGIN SET NOCOUNT ON;");

    for (Map<String, String> entry : fields) {
      String fldName = entry.get("field");
      String relTable = entry.get("relTable");
      String relField = entry.get("relField");

      body.append(new SqlDelete(relTable)
          .setWhere(SqlUtils.in(relTable, relField, "deleted", fldName, null))
          .getQuery());
    }
    return body.append(";END;").toString();
  }

  @Override
  protected String getSelect(SqlSelect ss) {
    String sql = super.getSelect(ss);
    int limit = ss.getLimit();
    int offset = ss.getOffset();

    if (limit <= 0 && offset <= 0) {
      return sql;
    }
    String top = "";
    String numbering = "";
    String idAlias = "";
    boolean hasUnion = !BeeUtils.isEmpty(ss.getUnion());

    if (BeeUtils.isPositive(limit)) {
      top = BeeUtils.joinWords("TOP", offset + limit);
    }
    if (BeeUtils.isPositive(offset)) {
      String order = "ORDER BY (SELECT 0)";

      if (!BeeUtils.isEmpty(ss.getOrderBy())) {
        int idx = sql.lastIndexOf(" ORDER BY ");
        order = sql.substring(idx + 1);
        sql = sql.substring(0, idx);
      }
      idAlias = sqlQuote(SqlUtils.uniqueName());
      numbering = BeeUtils.joinWords(
          "ROW_NUMBER() OVER", BeeUtils.parenthesize(order), "AS", idAlias + ",");
    }
    if (hasUnion) {
      String queryAlias = sqlQuote(SqlUtils.uniqueName());

      sql = BeeUtils.joinWords(
          "SELECT", top, numbering, queryAlias + ".*",
          "FROM", BeeUtils.parenthesize(sql), queryAlias);
    } else {
      String select = "SELECT " + (ss.isDistinctMode() ? "DISTINCT " : "");
      sql = BeeUtils.joinWords(
          select, top, numbering, sql.substring(select.length()));
    }
    if (!BeeUtils.isEmpty(idAlias)) {
      String queryAlias = sqlQuote(SqlUtils.uniqueName());

      sql = BeeUtils.joinWords(
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

    if (whereClause != null) {
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
  protected String getVersionTrigger(String versionName) {
    // TODO implementation required
    return null;
  }

  @Override
  protected String sqlFunction(SqlFunction function, Map<String, Object> params) {
    switch (function) {
      case CONCAT:
        StringBuilder xpr = new StringBuilder(transform(params.get("member" + 0)));

        for (int i = 1; i < params.size(); i++) {
          xpr.append(" + ").append(params.get("member" + i));
        }
        return BeeUtils.parenthesize(xpr.toString());

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

  @SuppressWarnings("unchecked")
  @Override
  protected String sqlKeyword(SqlKeyword option, Map<String, Object> params) {
    switch (option) {
      case SET_PARAMETER:
        String cmd = BeeUtils.join(BeeConst.STRING_EMPTY,
            "DECLARE @tmpVar BINARY(128);SET @tmpVar=",
            sqlTransform(params.get("prmValue")), ";SET CONTEXT_INFO @tmpVar");
        return "EXEC(" + sqlTransform(cmd) + ")";

      case CREATE_INDEX:
        String text = super.sqlKeyword(option, params);

        if (!params.containsKey("expression")) {
          String field = (String) params.get("fields");

          if (!isEmpty(params.get("isUnique")) && !field.contains(",")) {
            text = BeeUtils.joinWords(text, "WHERE", field, "IS NOT NULL");
          }
        }
        return text;

      case CREATE_TRIGGER:
        return BeeUtils.joinWords(
            "CREATE TRIGGER", params.get("name"), "ON", params.get("table"),
            ((SqlTriggerTiming) params.get("timing") == SqlTriggerTiming.BEFORE) ? "FOR" : "AFTER",
            BeeUtils.join(",", (EnumSet<SqlTriggerEvent>) params.get("events")),
            "AS ", getTriggerBody(params));

      case DB_NAME:
        return "SELECT db_name() AS " + sqlQuote("dbName");

      case DB_SCHEMA:
        return "SELECT schema_name() AS " + sqlQuote("dbSchema");

      case DB_TABLES:
        IsCondition wh = SqlUtils.and(SqlUtils.equals("o", "type", "U"),
            SqlUtils.equals("o", "is_ms_shipped", 0),
            SqlUtils.less("p", "index_id", 2));

        Object prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("s", "name", prm));
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("o", "name", prm));
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
        wh = SqlUtils.and(SqlUtils.notNull("i", "name"), SqlUtils.equals("o", "type", "U"),
            SqlUtils.equals("o", "is_ms_shipped", 0));

        prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("s", "name", prm));
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("o", "name", prm));
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
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("s", "name", prm));
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("o", "name", prm));
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
        return BeeUtils.joinWords(
            "sp_rename", params.get("nameFrom"), ",", params.get("nameTo"));

      default:
        return super.sqlKeyword(option, params);
    }
  }

  @Override
  protected String sqlQuote(String value) {
    if (BeeUtils.isEmpty(value)) {
      return null;
    }
    return "[" + value + "]";
  }

  @Override
  protected String sqlType(SqlDataType type, int precision, int scale) {
    switch (type) {
      case DOUBLE:
        return "FLOAT";
      case TEXT:
        return "VARCHAR(MAX)";
      case BLOB:
        return "VARBINARY(MAX)";
      default:
        return super.sqlType(type, precision, scale);
    }
  }
}
