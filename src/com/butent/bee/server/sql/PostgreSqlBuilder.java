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
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Contains specific requirements for SQL statement building for PostgreSQL server.
 */

class PostgreSqlBuilder extends SqlBuilder {

  @Override
  public SqlEngine getEngine() {
    return SqlEngine.POSTGRESQL;
  }

  @Override
  protected String getAuditTrigger(String auditTable, String idName, Collection<String> fields) {
    StringBuilder body = new StringBuilder();
    String insert = "INSERT INTO " + auditTable
        + " (" + BeeUtils.join(",", sqlQuote(AUDIT_FLD_TIME), sqlQuote(AUDIT_FLD_USER),
            sqlQuote(AUDIT_FLD_TX), sqlQuote(AUDIT_FLD_MODE), sqlQuote(AUDIT_FLD_ID),
            sqlQuote(AUDIT_FLD_FIELD), sqlQuote(AUDIT_FLD_VALUE))
        + ") VALUES (_time,_user,TXID_CURRENT(),SUBSTRING(TG_OP,1,1),";

    body.append("DECLARE _time BIGINT=floor(extract(epoch from current_timestamp)*1000);")
        .append("_user BIGINT;")
        .append("BEGIN ")
        .append("BEGIN _user:=CAST(CURRENT_SETTING('").append(AUDIT_USER).append("') AS BIGINT);")
        .append("EXCEPTION WHEN OTHERS THEN _user:=NULL;END;")
        .append("IF (TG_OP='DELETE') THEN ")
        .append(insert).append("OLD." + idName + ",NULL,NULL);")
        .append("ELSE ");

    insert = insert + "NEW." + idName + ",";

    for (String field : fields) {
      String fld = sqlQuote(field);

      body.append("IF ((TG_OP='INSERT' AND NEW.").append(fld)
          .append(" IS NOT NULL) OR (TG_OP='UPDATE' AND OLD.").append(fld)
          .append(" IS DISTINCT FROM NEW.").append(fld).append(")) THEN ")
          .append(insert).append("'").append(field).append("',NEW.").append(fld).append(");")
          .append("END IF;");
    }
    body.append("END IF;")
        .append("RETURN NULL; END;");

    return body.toString();
  }

  @Override
  protected String getRelationTrigger(List<Map<String, String>> fields) {
    StringBuilder body = new StringBuilder("BEGIN ");

    for (Map<String, String> entry : fields) {
      String fldName = entry.get("field");
      String relTable = entry.get("relTable");
      String relField = entry.get("relField");
      String var = "OLD." + sqlQuote(fldName);

      body.append(BeeUtils.joinWords("IF", var, "IS NOT NULL THEN BEGIN",
          new SqlDelete(relTable)
              .setWhere(SqlUtils.equals(relTable, relField, SqlUtils.expression(var)))
              .getQuery(),
          ";EXCEPTION WHEN foreign_key_violation THEN END;END IF;"));
    }
    return body.append("RETURN NULL;END;").toString();
  }

  @Override
  protected String getSelect(SqlSelect ss) {
    int limit = ss.getLimit();
    int offset = ss.getOffset();
    String sql = super.getSelect(ss);

    if (BeeUtils.isPositive(limit)) {
      sql += " LIMIT " + limit;
    }
    if (BeeUtils.isPositive(offset)) {
      sql += " OFFSET " + offset;
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
    StringBuilder query = new StringBuilder("UPDATE ")
        .append(SqlUtils.name(su.getTarget()).getSqlString(this))
        .append(" SET ");

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
    query.append(" FROM ")
        .append(fromSource.getSqlString(this))
        .append(" WHERE ")
        .append(su.getFromJoin().getSqlString(this));

    IsCondition whereClause = su.getWhere();

    if (!isEmpty(whereClause)) {
      String wh = whereClause.getSqlString(this);

      if (!BeeUtils.isEmpty(wh)) {
        query.append(" AND ").append(wh);
      }
    }
    return query.toString();
  }

  @Override
  protected String sqlCondition(Operator operator, Map<String, String> params) {
    switch (operator) {
      case FULL_TEXT:
        String expression = params.get("expression");
        List<String> values = new ArrayList<>(NameUtils.toList(params.get("value" + 0)));

        for (int i = 0; i < values.size(); i++) {
          values.set(i, values.get(i).replace("'", "''")
              .replace("\\", "\\\\").replace(":", "\\:") + ":*");
        }
        return expression + " @@ to_tsquery('simple', '" + BeeUtils.join("&", values) + "')";

      default:
        return super.sqlCondition(operator, params);
    }
  }

  @Override
  protected String sqlFunction(SqlFunction function, Map<String, Object> params) {
    switch (function) {
      case LENGTH:
        return "CHAR_LENGTH(" + params.get("expression") + ")";

      default:
        return super.sqlFunction(function, params);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected String sqlKeyword(SqlKeyword option, Map<String, Object> params) {
    switch (option) {
      case SET_PARAMETER:
        return BeeUtils.join(BeeConst.STRING_EMPTY,
            "SELECT SET_CONFIG('", params.get("prmName"), "','", params.get("prmValue"), "',true)");

      case CREATE_TRIGGER:
        String procName = "trigger_" + Codec.crc32((String) params.get("name"));

        return BeeUtils.joinWords(
            "CREATE OR REPLACE FUNCTION", procName, "() RETURNS TRIGGER AS",
            "$" + procName + "$", getTriggerBody(params),
            "$" + procName + "$", "LANGUAGE plpgsql;",
            "CREATE TRIGGER", params.get("name"), params.get("timing"),
            BeeUtils.join(" OR ", (EnumSet<SqlTriggerEvent>) params.get("events")),
            "ON", params.get("table"), "FOR EACH", params.get("scope"),
            "EXECUTE PROCEDURE", procName, "();");

      case DB_NAME:
        return "SELECT current_database() as " + sqlQuote("dbName");

      case DB_SCHEMA:
        return "SELECT current_schema() as " + sqlQuote("dbSchema");

      case DB_TABLES:
        IsCondition wh = null;

        Object prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("s", "nspname", prm));
        } else {
          wh = SqlUtils.sqlFalse();
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("t", "relname", prm));
        }
        return new SqlSelect()
            .addField("t", "relname", SqlConstants.TBL_NAME)
            .addField("t", "reltuples", SqlConstants.ROW_COUNT)
            .addFrom("pg_class", "t")
            .addFromInner("pg_namespace", "s", SqlUtils.join("t", "relnamespace", "s", "oid"))
            .setWhere(wh)
            .getSqlString(this);

      case DB_INDEXES:
        wh = null;

        prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("s", "nspname", prm));
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("t", "relname", prm));
        }
        return new SqlSelect()
            .addField("t", "relname", SqlConstants.TBL_NAME)
            .addField("i", "relname", SqlConstants.KEY_NAME)
            .addFrom("pg_class", "t")
            .addFromInner("pg_index", "j", SqlUtils.join("t", "oid", "j", "indrelid"))
            .addFromInner("pg_class", "i", SqlUtils.join("j", "indexrelid", "i", "oid"))
            .addFromInner("pg_namespace", "s", SqlUtils.join("i", "relnamespace", "s", "oid"))
            .setWhere(wh)
            .getSqlString(this);

      case LIKE:
        if (BeeUtils.unbox((Boolean) params.get("CaseSensitive"))) {
          return "LIKE";
        } else {
          return "ILIKE";
        }
      default:
        return super.sqlKeyword(option, params);
    }
  }

  @Override
  protected String sqlQuote(String value) {
    if (BeeUtils.isEmpty(value)) {
      return null;
    }
    return "\"" + value + "\"";
  }

  @Override
  protected String sqlType(SqlDataType type, int precision, int scale) {
    switch (type) {
      case BOOLEAN:
        return "NUMERIC(1)";
      case DOUBLE:
        return "DOUBLE PRECISION";
      case BLOB:
        return "BYTEA";
      default:
        return super.sqlType(type, precision, scale);
    }
  }
}
