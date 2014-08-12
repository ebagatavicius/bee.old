package com.butent.bee.server.data;

import com.google.common.collect.Lists;

import com.butent.bee.server.jdbc.JdbcException;
import com.butent.bee.server.jdbc.JdbcUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import javax.ejb.Stateless;

/**
 * Is used by rpc_db_meta service requests handler, returns parameters of <code>ResultSets</code>.
 */

@Stateless
public class ResultSetBean {

  private static final BeeLogger logger = LogUtils.getLogger(ResultSetBean.class);
  private static final BeeColumn[] metaCols;

  static {
    String[] arr = new String[] {
        "index", "name", "label",
        "schema", "catalog", "table",
        "class", "sql type", "type name", "value type",
        "precision", "scale", "nullable",
        "display size", "signed", "auto increment", "case sensitive", "currency",
        "searchable", "read only", "writable", "definitely writable"};

    metaCols = new BeeColumn[arr.length];
    for (int i = 0; i < arr.length; i++) {
      metaCols[i] = new BeeColumn(arr[i]);
    }
  }

  public ResponseObject getMetaData(ResultSet rs) {
    Assert.notNull(rs);

    ResponseObject response = new ResponseObject();
    BeeRowSet result = new BeeRowSet(metaCols);

    try {
      ResultSetMetaData rsmd = rs.getMetaData();
      for (int i = 1; i <= rsmd.getColumnCount(); i++) {
        int sqlType = rsmd.getColumnType(i);
        ValueType valueType = JdbcUtils.sqlTypeToValueType(sqlType);

        List<String> values = Lists.newArrayList(String.valueOf(i),
            rsmd.getColumnName(i),
            rsmd.getColumnLabel(i),
            rsmd.getSchemaName(i),
            rsmd.getCatalogName(i),
            rsmd.getTableName(i),
            rsmd.getColumnClassName(i),
            String.valueOf(sqlType),
            rsmd.getColumnTypeName(i),
            (valueType == null) ? null : valueType.name(),
            String.valueOf(rsmd.getPrecision(i)),
            String.valueOf(rsmd.getScale(i)),
            String.valueOf(rsmd.isNullable(i)),
            String.valueOf(rsmd.getColumnDisplaySize(i)),
            String.valueOf(rsmd.isSigned(i)),
            String.valueOf(rsmd.isAutoIncrement(i)),
            String.valueOf(rsmd.isCaseSensitive(i)),
            String.valueOf(rsmd.isCurrency(i)),
            String.valueOf(rsmd.isSearchable(i)),
            String.valueOf(rsmd.isReadOnly(i)),
            String.valueOf(rsmd.isWritable(i)),
            String.valueOf(rsmd.isDefinitelyWritable(i)));

        result.addRow(i, 0, values);
      }

    } catch (SQLException ex) {
      logger.error(ex);
      response.addError(ex);
    }

    return response.setResponse(result);
  }

  public ResponseObject read(ResultSet rs) {
    Assert.notNull(rs);

    ResponseObject response = new ResponseObject();

    List<BeeColumn> columns;
    int c;

    try {
      columns = JdbcUtils.getColumns(rs);
      c = columns.size();
    } catch (JdbcException ex) {
      logger.error(ex);
      response.addError(ex);
      columns = null;
      c = 0;
    }

    if (c <= 0) {
      response.addError("Cannot get result set meta data");
      return response;
    }

    BeeRowSet result = new BeeRowSet(columns);

    try {
      int cnt = 0;

      while (rs.next()) {
        String[] values = new String[c];
        for (int i = 0; i < c; i++) {
          values[i] = rs.getString(i + 1);
        }

        result.addRow(++cnt, 0, values);
      }

    } catch (SQLException ex) {
      logger.error(ex);
      response.addError(ex);
    }

    return response.setResponse(result);
  }
}
