package com.butent.bee.server.jdbc;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

/**
 * Contains utility functions for management of JDBC related objects (
 * <code>BeeConnection, BeeStatement, BeeResultSet</code>).
 */

public class JdbcUtils {
  private static final BeeLogger logger = LogUtils.getLogger(JdbcUtils.class);

  public static void applyFetchSize(Statement stmt, int rows) throws JdbcException {
    Assert.notNull(stmt);
    Assert.isPositive(rows);

    try {
      stmt.setFetchSize(rows);
    } catch (SQLException ex) {
      throw new JdbcException(ex);
    }
  }

  public static void applyMaxRows(Statement stmt, int rows) throws JdbcException {
    Assert.notNull(stmt);
    Assert.isPositive(rows);

    try {
      stmt.setMaxRows(rows);
    } catch (SQLException ex) {
      throw new JdbcException(ex);
    }
  }

  public static void applyTimeout(Statement stmt, int timeout) throws JdbcException {
    Assert.notNull(stmt);
    Assert.isPositive(timeout);

    try {
      stmt.setQueryTimeout(timeout);
    } catch (SQLException ex) {
      throw new JdbcException(ex);
    }
  }

  public static void closeConnection(Connection con) {
    if (con != null) {
      try {
        con.close();
      } catch (SQLException ex) {
        logger.warning(ex, "Could not close JDBC Connection");
      } catch (Exception ex) {
        logger.warning(ex, "Unexpected exception on closing JDBC Connection");
      }
    }
  }

  public static void closeResultSet(ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException ex) {
        logger.warning(ex, "Could not close JDBC ResultSet");
      } catch (Exception ex) {
        logger.warning(ex, "Unexpected exception on closing JDBC ResultSet");
      }
    }
  }

  public static void closeStatement(Statement stmt) {
    if (stmt != null) {
      try {
        stmt.close();
      } catch (SQLException ex) {
        logger.warning(ex, "Could not close JDBC Statement");
      } catch (Exception ex) {
        logger.warning(ex, "Unexpected exception on closing JDBC Statement");
      }
    }
  }

  public static String concurrencyAsString(int z) {
    switch (z) {
      case (ResultSet.CONCUR_READ_ONLY):
        return BeeConst.CONCUR_READ_ONLY;
      case (ResultSet.CONCUR_UPDATABLE):
        return BeeConst.CONCUR_UPDATABLE;
      default:
        return BeeConst.UNKNOWN;
    }
  }

  public static int concurrencyFromString(String s) {
    Assert.notEmpty(s);
    int z;

    if (BeeUtils.same(s, BeeConst.CONCUR_READ_ONLY)) {
      z = ResultSet.CONCUR_READ_ONLY;
    } else if (BeeUtils.same(s, BeeConst.CONCUR_UPDATABLE)) {
      z = ResultSet.CONCUR_UPDATABLE;
    } else {
      z = JdbcConst.UNKNOWN_CONCURRENCY;
    }
    return z;
  }

  public static String fetchDirectionAsString(int z) {
    switch (z) {
      case (ResultSet.FETCH_FORWARD):
        return BeeConst.FETCH_FORWARD;
      case (ResultSet.FETCH_REVERSE):
        return BeeConst.FETCH_REVERSE;
      case (ResultSet.FETCH_UNKNOWN):
        return BeeConst.FETCH_UNKNOWN;
      default:
        return BeeConst.UNKNOWN;
    }
  }

  public static int fetchDirectionFromString(String s) {
    Assert.notEmpty(s);
    int z;

    if (BeeUtils.same(s, BeeConst.FETCH_FORWARD)) {
      z = ResultSet.FETCH_FORWARD;
    } else if (BeeUtils.same(s, BeeConst.FETCH_REVERSE)) {
      z = ResultSet.FETCH_REVERSE;
    } else if (BeeUtils.same(s, BeeConst.FETCH_UNKNOWN)) {
      z = ResultSet.FETCH_UNKNOWN;
    } else {
      z = JdbcConst.UNKNOWN_FETCH_DIRECTION;
    }
    return z;
  }

  public static String[] getColNames(ResultSet rs) {
    Assert.notNull(rs);

    int c;
    String[] arr = null;

    try {
      ResultSetMetaData md = rs.getMetaData();
      c = md.getColumnCount();

      if (c > 0) {
        arr = new String[c];
        for (int i = 0; i < c; i++) {
          arr[i] = md.getColumnName(i + 1);
        }
      }
    } catch (SQLException ex) {
      c = 0;
    }

    if (c > 0) {
      return arr;
    } else {
      return null;
    }
  }

  public static int getColumnCount(Object obj) {
    Assert.notNull(obj);
    int c = BeeConst.UNDEF;

    try {
      if (obj instanceof ResultSetMetaData) {
        c = ((ResultSetMetaData) obj).getColumnCount();
      } else if (obj instanceof ResultSet) {
        c = ((ResultSet) obj).getMetaData().getColumnCount();
      } else if (obj instanceof BeeResultSet) {
        c = ((BeeResultSet) obj).getColumnCount();
      }
    } catch (SQLException ex) {
      logger.error(ex);
    }
    return c;
  }

  public static List<BeeColumn> getColumns(ResultSet rs) throws JdbcException {
    Assert.notNull(rs);

    int c = getColumnCount(rs);
    Assert.isPositive(c);

    List<BeeColumn> columns = Lists.newArrayListWithCapacity(c);

    try {
      ResultSetMetaData md = rs.getMetaData();
      for (int i = 0; i < c; i++) {
        BeeColumn column = new BeeColumn();
        setColumnInfo(md, i + 1, column);
        columns.add(column);
      }
    } catch (SQLException ex) {
      throw new JdbcException(ex);
    }
    return columns;
  }

  public static Connection getConnection(DataSource src) throws JdbcException {
    if (src == null) {
      return null;
    }
    Connection con = null;

    try {
      con = src.getConnection();
    } catch (SQLException ex) {
      throw new JdbcException(ex);
    }
    return con;
  }

  public static Connection getConnection(ResultSet rs) throws JdbcException {
    Assert.notNull(rs);
    Connection conn;

    try {
      conn = rs.getStatement().getConnection();
    } catch (SQLException ex) {
      throw new JdbcException(ex);
    }
    return conn;
  }

  public static String getCursorName(ResultSet rs) {
    Assert.notNull(rs);
    String crs;

    try {
      if (supportsCursorName(getConnection(rs))) {
        crs = rs.getCursorName();
      } else {
        crs = BeeConst.STRING_EMPTY;
      }
    } catch (SQLFeatureNotSupportedException ex) {
      crs = JdbcConst.FEATURE_NOT_SUPPORTED;
    } catch (SQLException ex) {
      logger.warning(ex);
      crs = BeeConst.ERROR;
    }
    return crs;
  }

  public static int getHoldability(ResultSet rs) {
    Assert.notNull(rs);
    int z;

    try {
      z = rs.getHoldability();
    } catch (SQLFeatureNotSupportedException ex) {
      logger.warning(ex);
      z = BeeConst.INT_FALSE;
    } catch (SQLException ex) {
      logger.warning(ex);
      z = BeeConst.INT_ERROR;
    }
    return z;
  }

  public static String getHoldabilityInfo(ResultSet rs) {
    Assert.notNull(rs);
    String info;

    try {
      int z = rs.getHoldability();
      info = BeeUtils.joinWords(z, holdabilityAsString(z));
    } catch (SQLFeatureNotSupportedException ex) {
      info = JdbcConst.FEATURE_NOT_SUPPORTED;
    } catch (SQLException ex) {
      logger.warning(ex);
      info = ex.toString();
    }
    return info;
  }

  public static String getJdbcTypeName(int jdbcType) {
    String name = null;
    Field[] fields = java.sql.Types.class.getFields();

    for (int i = 0; i < fields.length; i++) {
      try {
        if (fields[i].getInt(null) == jdbcType) {
          name = fields[i].getName();
          break;
        }
      } catch (IllegalArgumentException ex) {
        name = ex.getMessage();
      } catch (IllegalAccessException ex) {
        name = ex.getMessage();
      }
    }
    return name;
  }

  public static boolean getMoreResults(Statement stmt) throws JdbcException {
    Assert.notNull(stmt);
    boolean mr = false;

    try {
      mr = stmt.getMoreResults();
    } catch (SQLException ex) {
      throw new JdbcException(ex);
    }
    return mr;
  }

  public static ResultSet getResultSet(Statement stmt) throws JdbcException {
    Assert.notNull(stmt);
    ResultSet rs = null;

    try {
      rs = stmt.getResultSet();
    } catch (SQLException ex) {
      throw new JdbcException(ex);
    }
    return rs;
  }

  public static String getResultSetValue(ResultSet rs, int idx) throws JdbcException {
    Assert.notNull(rs);
    Assert.isPositive(idx);

    String v;
    try {
      v = rs.getString(idx);
    } catch (SQLException ex) {
      throw new JdbcException(ex);
    }
    return v;
  }

  public static List<Property> getRs(ResultSet rs) {
    if (rs == null) {
      return null;
    }

    String[] cols = getColNames(rs);
    if (ArrayUtils.isEmpty(cols)) {
      return null;
    }

    List<Property> lst = new ArrayList<Property>();
    int r = 0;

    try {
      while (rs.next()) {
        r++;
        PropertyUtils.addProperty(lst, JdbcConst.ROW_ID, r);

        for (String nm : cols) {
          PropertyUtils.addProperty(lst, nm, rs.getString(nm));
        }
      }
    } catch (SQLException ex) {
      PropertyUtils.addProperty(lst, "Error", ex.getMessage());
    }
    return lst;
  }

  public static int getSize(ResultSet rs) {
    int c = 0;
    if (rs == null) {
      return c;
    }

    try {
      if (rs.getType() == ResultSet.TYPE_FORWARD_ONLY) {
        while (rs.next()) {
          c++;
        }
      } else if (rs.last()) {
        c = rs.getRow();
      }
    } catch (SQLException ex) {
      c = BeeConst.UNDEF;
    }
    return c;
  }

  public static String getTypeInfo(ResultSet rs) {
    Assert.notNull(rs);
    String info;

    try {
      int z = rs.getType();
      info = BeeUtils.joinWords(z, rsTypeAsString(z));
    } catch (SQLFeatureNotSupportedException ex) {
      info = JdbcConst.FEATURE_NOT_SUPPORTED;
    } catch (SQLException ex) {
      logger.warning(ex);
      info = ex.toString();
    }
    return info;
  }

  public static int getUpdateCount(Statement stmt) throws JdbcException {
    Assert.notNull(stmt);
    int cnt = BeeConst.UNDEF;

    try {
      cnt = stmt.getUpdateCount();
    } catch (SQLException ex) {
      throw new JdbcException(ex);
    }
    return cnt;
  }

  public static List<String> getWarnings(Connection conn) {
    Assert.notNull(conn);
    SQLWarning warn;

    try {
      warn = conn.getWarnings();
    } catch (SQLException ex) {
      logger.warning(ex);
      warn = null;
    }

    List<String> lst;
    if (warn == null) {
      lst = Collections.emptyList();
    } else {
      lst = unchain(warn);
    }
    return lst;
  }

  public static List<String> getWarnings(ResultSet rs) {
    Assert.notNull(rs);
    SQLWarning warn;

    try {
      warn = rs.getWarnings();
    } catch (SQLException ex) {
      logger.warning(ex);
      warn = null;
    }

    List<String> lst;
    if (warn == null) {
      lst = Collections.emptyList();
    } else {
      lst = unchain(warn);
    }
    return lst;
  }

  public static List<String> getWarnings(Statement stmt) {
    Assert.notNull(stmt);
    SQLWarning warn;

    try {
      warn = stmt.getWarnings();
    } catch (SQLException ex) {
      logger.warning(ex);
      warn = null;
    }

    List<String> lst;
    if (warn == null) {
      lst = Collections.emptyList();
    } else {
      lst = unchain(warn);
    }
    return lst;
  }

  public static String holdabilityAsString(int z) {
    switch (z) {
      case (ResultSet.CLOSE_CURSORS_AT_COMMIT):
        return BeeConst.CLOSE_CURSORS_AT_COMMIT;
      case (ResultSet.HOLD_CURSORS_OVER_COMMIT):
        return BeeConst.HOLD_CURSORS_OVER_COMMIT;
      case (BeeConst.INT_FALSE):
        return JdbcConst.FEATURE_NOT_SUPPORTED;
      case (BeeConst.INT_ERROR):
        return BeeConst.ERROR;
      default:
        return BeeConst.UNKNOWN;
    }
  }

  public static int holdabilityFromString(String s) {
    Assert.notEmpty(s);
    int z;

    if (BeeUtils.same(s, BeeConst.CLOSE_CURSORS_AT_COMMIT)) {
      z = ResultSet.CLOSE_CURSORS_AT_COMMIT;
    } else if (BeeUtils.same(s, BeeConst.HOLD_CURSORS_OVER_COMMIT)) {
      z = ResultSet.HOLD_CURSORS_OVER_COMMIT;
    } else if (BeeUtils.same(s, JdbcConst.FEATURE_NOT_SUPPORTED)) {
      z = BeeConst.INT_FALSE;
    } else if (BeeUtils.same(s, BeeConst.ERROR)) {
      z = BeeConst.INT_ERROR;
    } else {
      z = JdbcConst.UNKNOWN_HOLDABILITY;
    }
    return z;
  }

  public static String requiredSingleResult(ResultSet rs) throws JdbcException {
    Assert.notNull(rs);
    String v = null;

    int size = 0;

    try {
      while (rs.next()) {
        size++;
        if (size == 1) {
          v = rs.getString(1);
        } else {
          break;
        }
      }
    } catch (SQLException ex) {
      throw new JdbcException(ex);
    }

    if (size == 0) {
      throw new JdbcException(JdbcConst.RESULT_SET_EMPTY);
    } else if (size > 1) {
      throw new JdbcException(JdbcConst.rsRows(size));
    }
    return v;
  }

  public static String rsTypeAsString(int z) {
    switch (z) {
      case (ResultSet.TYPE_FORWARD_ONLY):
        return BeeConst.TYPE_FORWARD_ONLY;
      case (ResultSet.TYPE_SCROLL_INSENSITIVE):
        return BeeConst.TYPE_SCROLL_INSENSITIVE;
      case (ResultSet.TYPE_SCROLL_SENSITIVE):
        return BeeConst.TYPE_SCROLL_SENSITIVE;
      default:
        return BeeConst.UNKNOWN;
    }
  }

  public static int rsTypeFromString(String s) {
    Assert.notEmpty(s);
    int z;

    if (BeeUtils.same(s, BeeConst.TYPE_FORWARD_ONLY)) {
      z = ResultSet.TYPE_FORWARD_ONLY;
    } else if (BeeUtils.same(s, BeeConst.TYPE_SCROLL_INSENSITIVE)) {
      z = ResultSet.TYPE_SCROLL_INSENSITIVE;
    } else if (BeeUtils.same(s, BeeConst.TYPE_SCROLL_SENSITIVE)) {
      z = ResultSet.TYPE_SCROLL_SENSITIVE;
    } else {
      z = JdbcConst.UNKNOWN_RESULT_SET_TYPE;
    }
    return z;
  }

  public static boolean setColumnInfo(ResultSetMetaData rsmd, int idx, BeeColumn col) {
    Assert.notNull(rsmd);
    Assert.isPositive(idx);
    Assert.notNull(col);

    if (!validColumnIdx(rsmd, idx)) {
      return false;
    }

    boolean ok = false;

    try {
      String label = rsmd.getColumnLabel(idx);
      col.setId(BeeUtils.notEmpty(label, rsmd.getColumnName(idx)));
      col.setLabel(label);

      col.setType(sqlTypeToValueType(rsmd.getColumnType(idx)));

      col.setPrecision(rsmd.getPrecision(idx));
      col.setScale(rsmd.getScale(idx));

      col.setNullable(rsmd.isNullable(idx) == 1);
      col.setReadOnly(rsmd.isReadOnly(idx));

      ok = true;
    } catch (SQLException ex) {
      logger.error(ex);
      col.setProperty(ex.getClass().getSimpleName(), ex.getMessage());
    }
    return ok;
  }

  public static ValueType sqlTypeToValueType(int sqlType) {
    ValueType valueType;

    switch (sqlType) {
      case Types.BOOLEAN:
      case Types.BIT: {
        valueType = ValueType.BOOLEAN;
        break;
      }
      case Types.CHAR:
      case Types.VARCHAR:
        valueType = ValueType.TEXT;
        break;
      case Types.INTEGER:
      case Types.SMALLINT:
      case Types.TINYINT:
        valueType = ValueType.INTEGER;
        break;
      case Types.BIGINT:
        valueType = ValueType.LONG;
        break;
      case Types.DECIMAL:
      case Types.NUMERIC:
        valueType = ValueType.DECIMAL;
        break;
      case Types.REAL:
      case Types.DOUBLE:
      case Types.FLOAT:
        valueType = ValueType.NUMBER;
        break;
      case Types.DATE:
        valueType = ValueType.DATE;
        break;
      case Types.TIME:
        valueType = ValueType.TIME_OF_DAY;
        break;
      case Types.TIMESTAMP:
        valueType = ValueType.DATE_TIME;
        break;
      default:
        valueType = ValueType.TEXT;
    }
    return valueType;
  }

  public static boolean supportsBatchUpdates(Connection con) throws JdbcException {
    Assert.notNull(con);

    boolean ok;
    try {
      DatabaseMetaData dbmd = con.getMetaData();
      ok = dbmd.supportsBatchUpdates();
    } catch (SQLException ex) {
      throw new JdbcException(ex);
    } catch (AbstractMethodError err) {
      throw new JdbcException("JDBC driver does not support 'supportsBatchUpdates' method", err);
    }
    return ok;
  }

  public static boolean supportsCursorName(Connection conn) {
    Assert.notNull(conn);
    boolean ok;

    try {
      ok = conn.getMetaData().supportsPositionedUpdate();
    } catch (SQLException ex) {
      logger.warning(ex);
      ok = false;
    }
    return ok;
  }

  public static String transactionIsolationAsString(int z) {
    switch (z) {
      case (Connection.TRANSACTION_NONE):
        return BeeConst.TRANSACTION_NONE;
      case (Connection.TRANSACTION_READ_COMMITTED):
        return BeeConst.TRANSACTION_READ_COMMITTED;
      case (Connection.TRANSACTION_READ_UNCOMMITTED):
        return BeeConst.TRANSACTION_READ_UNCOMMITTED;
      case (Connection.TRANSACTION_REPEATABLE_READ):
        return BeeConst.TRANSACTION_REPEATABLE_READ;
      case (Connection.TRANSACTION_SERIALIZABLE):
        return BeeConst.TRANSACTION_SERIALIZABLE;
      default:
        return BeeConst.UNKNOWN;
    }
  }

  public static int transactionIsolationFromString(String s) {
    Assert.notEmpty(s);
    int z;

    if (BeeUtils.same(s, BeeConst.TRANSACTION_NONE)) {
      z = Connection.TRANSACTION_NONE;
    } else if (BeeUtils.same(s, BeeConst.TRANSACTION_READ_COMMITTED)) {
      z = Connection.TRANSACTION_READ_COMMITTED;
    } else if (BeeUtils.same(s, BeeConst.TRANSACTION_READ_UNCOMMITTED)) {
      z = Connection.TRANSACTION_READ_UNCOMMITTED;
    } else if (BeeUtils.same(s, BeeConst.TRANSACTION_REPEATABLE_READ)) {
      z = Connection.TRANSACTION_REPEATABLE_READ;
    } else if (BeeUtils.same(s, BeeConst.TRANSACTION_SERIALIZABLE)) {
      z = Connection.TRANSACTION_SERIALIZABLE;
    } else {
      z = JdbcConst.UNKNOWN_TRANSACTION_ISOLATION;
    }
    return z;
  }

  public static String transform(SQLException ex) {
    Assert.notNull(ex);
    return BeeUtils.joinWords(ex.getSQLState(), ex.getErrorCode(), ex.toString());
  }

  public static List<String> unchain(SQLException x) {
    Assert.notNull(x);
    List<String> lst = new ArrayList<String>();

    SQLException ex = x;
    while (ex != null) {
      lst.add(transform(ex));
      ex = ex.getNextException();
    }
    return lst;
  }

  public static List<String> unchain(SQLWarning w) {
    Assert.notNull(w);
    List<String> lst = new ArrayList<String>();

    SQLWarning ex = w;
    while (ex != null) {
      lst.add(transform(ex));
      ex = ex.getNextWarning();
    }
    return lst;
  }

  public static boolean validColumnIdx(ResultSetMetaData rsmd, int idx) {
    if (rsmd == null) {
      return false;
    } else {
      return idx >= 1 && idx <= getColumnCount(rsmd);
    }
  }

  public static boolean validConcurrency(int z) {
    return z == ResultSet.CONCUR_READ_ONLY || z == ResultSet.CONCUR_UPDATABLE;
  }

  public static boolean validConcurrency(String s) {
    return BeeUtils.inListSame(s, BeeConst.CONCUR_READ_ONLY, BeeConst.CONCUR_UPDATABLE);
  }

  public static boolean validFetchDirection(int z) {
    return z == ResultSet.FETCH_FORWARD || z == ResultSet.FETCH_REVERSE
        || z == ResultSet.FETCH_UNKNOWN;
  }

  public static boolean validFetchDirection(String s) {
    return BeeUtils.inListSame(s, BeeConst.FETCH_FORWARD,
        BeeConst.FETCH_REVERSE, BeeConst.FETCH_UNKNOWN);
  }

  public static boolean validHoldability(int z) {
    return z == ResultSet.CLOSE_CURSORS_AT_COMMIT || z == ResultSet.HOLD_CURSORS_OVER_COMMIT;
  }

  public static boolean validHoldability(String s) {
    return BeeUtils.inListSame(s, BeeConst.CLOSE_CURSORS_AT_COMMIT,
        BeeConst.HOLD_CURSORS_OVER_COMMIT);
  }

  public static boolean validRsType(int z) {
    return z == ResultSet.TYPE_FORWARD_ONLY
        || z == ResultSet.TYPE_SCROLL_INSENSITIVE
        || z == ResultSet.TYPE_SCROLL_SENSITIVE;
  }

  public static boolean validRsType(String s) {
    return BeeUtils.inListSame(s, BeeConst.TYPE_FORWARD_ONLY,
        BeeConst.TYPE_SCROLL_INSENSITIVE, BeeConst.TYPE_SCROLL_SENSITIVE);
  }

  public static boolean validTransactionIsolation(int z) {
    return z == Connection.TRANSACTION_NONE
        || z == Connection.TRANSACTION_READ_COMMITTED
        || z == Connection.TRANSACTION_READ_UNCOMMITTED
        || z == Connection.TRANSACTION_REPEATABLE_READ
        || z == Connection.TRANSACTION_SERIALIZABLE;
  }

  public static boolean validTransactionIsolation(String s) {
    return BeeUtils.inListSame(s, BeeConst.TRANSACTION_NONE,
        BeeConst.TRANSACTION_READ_COMMITTED, BeeConst.TRANSACTION_READ_UNCOMMITTED,
        BeeConst.TRANSACTION_REPEATABLE_READ, BeeConst.TRANSACTION_SERIALIZABLE);
  }

  private JdbcUtils() {
  }
}
