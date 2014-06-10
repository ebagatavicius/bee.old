package com.butent.bee.server.sql;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeConst.SqlEngine;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.SqlConstants.SqlKeyword;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerEvent;
import com.butent.bee.shared.data.SqlConstants.SqlTriggerScope;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Contains specific requirements for SQL statement building for Oracle SQL server.
 */

class OracleSqlBuilder extends SqlBuilder {

  @Override
  public SqlEngine getEngine() {
    return SqlEngine.ORACLE;
  }

  @Override
  protected String getAuditTrigger(String auditTable, String idName, Collection<String> fields) {
    // TODO implementation required
    return null;
  }

  @Override
  protected String getRelationTrigger(List<Map<String, String>> fields) {
    StringBuilder body = new StringBuilder();

    for (Map<String, String> entry : fields) {
      String fldName = entry.get("field");
      String relTable = entry.get("relTable");
      String relField = entry.get("relField");
      String var = ":OLD." + sqlQuote(fldName);

      body.append(BeeUtils.joinWords("IF", var, "IS NOT NULL THEN",
          new SqlDelete(relTable).setWhere(SqlUtils.equals(relTable, relField, 69))
              .getQuery().replace("69", var),
          ";END IF;"));
    }
    return body.toString();
  }

  @Override
  protected String getSelect(SqlSelect ss) {
    int limit = ss.getLimit();
    int offset = ss.getOffset();
    String sql = super.getSelect(ss);

    if (BeeUtils.isEmpty(ss.getFrom())) {
      sql = BeeUtils.joinWords(sql, "FROM DUAL");
    }
    if (limit <= 0 && offset <= 0) {
      return sql;
    }
    String idAlias = sqlQuote(SqlUtils.uniqueName());
    String queryAlias = sqlQuote(SqlUtils.uniqueName());

    sql = BeeUtils.joinWords("ROWNUM AS", idAlias + ",", queryAlias + ".*",
        "FROM", BeeUtils.parenthesize(sql), queryAlias);

    if (BeeUtils.isPositive(limit)) {
      sql = BeeUtils.joinWords("/*+ FIRST_ROWS" + BeeUtils.parenthesize(offset + limit), "*/", sql,
          "WHERE ROWNUM <=", offset + limit);
    }
    sql = "SELECT " + sql;

    if (BeeUtils.isPositive(offset)) {
      queryAlias = sqlQuote(SqlUtils.uniqueName());

      sql = BeeUtils.joinWords("SELECT", queryAlias + ".*",
          "FROM", BeeUtils.parenthesize(sql), queryAlias,
          "WHERE", queryAlias + "." + idAlias, ">", offset);
    }
    return sql;
  }

  @Override
  protected String sqlFunction(SqlFunction function, Map<String, Object> params) {
    switch (function) {
      case CAST:
        String sql = "CAST(" + params.get("expression");
        String dataType;
        SqlDataType type = (SqlDataType) params.get("type");
        int precision = (Integer) params.get("precision");
        int scale = (Integer) params.get("scale");

        switch (type) {
          case TEXT:
            dataType = "NVARCHAR2(2000)";
            break;

          default:
            dataType = sqlType(type, precision, scale);
        }
        return BeeUtils.joinWords(sql, "AS", dataType + ")");

      case BITAND:
        return "BITAND(" + params.get("expression") + ", " + params.get("value") + ")";

      case BITOR:
        return params.get("expression") + " + " + params.get("value") + " - "
            + sqlFunction(SqlFunction.BITAND, params);

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
            "BEGIN DBMS_SESSION.SET_CONTEXT('CLIENTCONTEXT','", params.get("prmName"), "','",
            params.get("prmValue"), "');END;");

      case CREATE_TRIGGER:
        return BeeUtils.joinWords(
            "CREATE TRIGGER", params.get("name"), params.get("timing"),
            BeeUtils.join(" OR ", (EnumSet<SqlTriggerEvent>) params.get("events")),
            "ON", params.get("table"),
            ((SqlTriggerScope) params.get("scope") == SqlTriggerScope.ROW) ? "FOR EACH ROW" : "",
            "BEGIN", getTriggerBody(params), "END;");

      case DB_SCHEMA:
        return "SELECT sys_context('USERENV', 'CURRENT_SCHEMA') AS " + sqlQuote("dbSchema")
            + " FROM dual";

      case DB_TABLES:
        IsCondition wh = null;

        Object prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.equals("t", "OWNER", prm);
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("t", "TABLE_NAME", prm));
        }
        return new SqlSelect()
            .addField("t", "TABLE_NAME", SqlConstants.TBL_NAME)
            .addField("t", "NUM_ROWS", SqlConstants.ROW_COUNT)
            .addFrom("ALL_TABLES", "t")
            .setWhere(wh)
            .getSqlString(this);

      case DB_FIELDS:
        wh = null;

        prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.equals("c", "OWNER", prm);
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("c", "TABLE_NAME", prm));
        }
        return new SqlSelect()
            .addField("c", "TABLE_NAME", SqlConstants.TBL_NAME)
            .addField("c", "COLUMN_NAME", SqlConstants.FLD_NAME)
            .addField("c", "NULLABLE", SqlConstants.FLD_NULL)
            .addField("c", "DATA_TYPE", SqlConstants.FLD_TYPE)
            .addField("c", "DATA_LENGTH", SqlConstants.FLD_LENGTH)
            .addField("c", "DATA_PRECISION", SqlConstants.FLD_PRECISION)
            .addField("c", "DATA_SCALE", SqlConstants.FLD_SCALE)
            .addFrom("ALL_TAB_COLUMNS", "c")
            .setWhere(wh)
            .addOrder("c", "COLUMN_ID")
            .getSqlString(this);

      case DB_CONSTRAINTS:
        wh = null;

        prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.equals("k", "OWNER", prm);
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("k", "TABLE_NAME", prm));
        }
        prm = params.get("keyTypes");
        if (!isEmpty(prm)) {
          IsCondition typeWh = null;

          for (SqlKeyword type : (SqlKeyword[]) prm) {
            String tp;

            switch (type) {
              case PRIMARY_KEY:
                tp = "P";
                break;

              case FOREIGN_KEY:
                tp = "R";
                break;

              case UNIQUE:
                tp = "U";
                break;

              case CHECK:
                tp = "C";
                break;

              default:
                tp = null;
            }
            if (!BeeUtils.isEmpty(tp)) {
              typeWh = SqlUtils.or(typeWh, SqlUtils.equals("k", "CONSTRAINT_TYPE", tp));
            }
          }
          if (!isEmpty(typeWh)) {
            wh = SqlUtils.and(wh, typeWh);
          }
        }
        return new SqlSelect()
            .addField("k", "TABLE_NAME", SqlConstants.TBL_NAME)
            .addField("k", "CONSTRAINT_NAME", SqlConstants.KEY_NAME)
            .addField("k", "CONSTRAINT_TYPE", SqlConstants.KEY_TYPE)
            .addFrom("ALL_CONSTRAINTS", "k")
            .setWhere(wh)
            .getSqlString(this);

      case DB_FOREIGNKEYS:
        wh = SqlUtils.equals("c", "CONSTRAINT_TYPE", "R");

        prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh,
              SqlUtils.equals("c", "OWNER", prm),
              SqlUtils.equals("r", "OWNER", prm));
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("c", "TABLE_NAME", prm));
        }
        prm = params.get("refTable");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("r", "TABLE_NAME", prm));
        }
        return new SqlSelect()
            .addField("c", "TABLE_NAME", SqlConstants.TBL_NAME)
            .addField("c", "CONSTRAINT_NAME", SqlConstants.KEY_NAME)
            .addField("r", "TABLE_NAME", SqlConstants.FK_REF_TABLE)
            .addFrom("ALL_CONSTRAINTS", "c")
            .addFromInner("ALL_CONSTRAINTS", "r",
                SqlUtils.join("c", "R_CONSTRAINT_NAME", "r", "CONSTRAINT_NAME"))
            .setWhere(wh)
            .getSqlString(this);

      case DB_INDEXES:
        wh = null;

        prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.equals("i", "OWNER", prm);
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("i", "TABLE_NAME", prm));
        }
        return new SqlSelect()
            .addField("i", "TABLE_NAME", SqlConstants.TBL_NAME)
            .addField("i", "INDEX_NAME", SqlConstants.KEY_NAME)
            .addFrom("ALL_INDEXES", "i")
            .setWhere(wh)
            .getSqlString(this);

      case DB_TRIGGERS:
        wh = null;

        prm = params.get("dbSchema");
        if (!isEmpty(prm)) {
          wh = SqlUtils.equals("t", "OWNER", prm);
        }
        prm = params.get("table");
        if (!isEmpty(prm)) {
          wh = SqlUtils.and(wh, SqlUtils.equals("t", "TABLE_NAME", prm));
        }
        return new SqlSelect()
            .addField("t", "TABLE_NAME", SqlConstants.TBL_NAME)
            .addField("t", "TRIGGER_NAME", SqlConstants.TRIGGER_NAME)
            .addFrom("ALL_TRIGGERS", "t")
            .setWhere(wh)
            .getSqlString(this);

      case TEMPORARY:
        return "";

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
      case INTEGER:
        return "NUMERIC(10)";
      case LONG:
      case DATE:
      case DATETIME:
        return "NUMERIC(19)";
      case DOUBLE:
        return "BINARY_DOUBLE";
      case CHAR:
        return "NCHAR(" + precision + ")";
      case STRING:
        return "NVARCHAR2(" + precision + ")";
      case TEXT:
        return "NCLOB";
      default:
        return super.sqlType(type, precision, scale);
    }
  }
}