package com.butent.bee.server.data;

import com.google.common.collect.Lists;

import com.butent.bee.server.DataSourceBean;
import com.butent.bee.server.data.DataEvent.TableModifyEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.jdbc.JdbcUtils;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.sql.HasTarget;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.IsQuery;
import com.butent.bee.server.sql.SqlBuilderFactory;
import com.butent.bee.server.sql.SqlCreate;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.utils.BeeDataSource;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeConst.SqlEngine;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants.SqlKeyword;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.exceptions.BeeRuntimeException;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

/**
 * Manages SQL related requests from client side.
 */

@Stateless
@LocalBean
public class QueryServiceBean {

  /**
   * Is a private interface for SQL processing.
   */

  public interface ResultSetProcessor<T> {
    T processResultSet(ResultSet rs) throws SQLException;
  }

  private abstract class SqlHandler<T> implements ResultSetProcessor<T> {

    public T processError(SQLException ex) {
      String error = null;

      Map<String, String> params = prm.getMap(BeeUtils.join(BeeConst.STRING_EMPTY,
          AdministrationConstants.PRM_SQL_MESSAGES, SqlBuilderFactory.getBuilder().getEngine()));

      if (!BeeUtils.isEmpty(params)) {
        String msg = ex.getMessage();

        for (String key : params.keySet()) {
          if (msg.matches("(?s)" + key)) {
            error = msg.replaceAll("(?s)" + key, params.get(key));
            break;
          }
        }
      }
      if (error != null) {
        throw new BeeRuntimeException(error);
      } else {
        throw new BeeRuntimeException(ex);
      }
    }

    public abstract T processUpdateCount(int updateCount);
  }

  private static BeeLogger logger = LogUtils.getLogger(QueryServiceBean.class);

  private static BeeRowSet rsToBeeRowSet(ResultSet rs, BeeView view) throws SQLException {
    List<BeeColumn> rsCols = JdbcUtils.getColumns(rs);

    int idIndex = BeeConst.UNDEF;
    int versionIndex = BeeConst.UNDEF;
    int editableIndex = BeeConst.UNDEF;
    int removableIndex = BeeConst.UNDEF;

    BeeRowSet result;
    List<Integer> indexes = Lists.newArrayListWithCapacity(rsCols.size());

    if (view == null) {
      result = new BeeRowSet(rsCols);
      for (int i = 0; i < rsCols.size(); i++) {
        indexes.add(i + 1);
      }

    } else {
      result = new BeeRowSet();

      for (int i = 0; i < rsCols.size(); i++) {
        BeeColumn column = rsCols.get(i);
        String colName = column.getId();
        int colIndex = i + 1;

        if (view.hasColumn(colName)) {
          view.initColumn(colName, column);
          result.addColumn(column);
          indexes.add(colIndex);

        } else if (BeeUtils.same(colName, view.getSourceIdName())) {
          idIndex = colIndex;

        } else if (BeeUtils.same(colName, view.getSourceVersionName())) {
          versionIndex = colIndex;

        } else if (BeeUtils.same(colName, RightsState.EDIT.name())) {
          editableIndex = colIndex;

        } else if (BeeUtils.same(colName, RightsState.DELETE.name())) {
          removableIndex = colIndex;
        }
      }
    }

    long rowId = 0;
    boolean editable = RightsState.EDIT.isChecked();
    boolean removable = RightsState.DELETE.isChecked();

    int cc = result.getNumberOfColumns();

    while (rs.next()) {
      String[] values = new String[cc];

      for (int i = 0; i < cc; i++) {
        BeeColumn column = result.getColumn(i);
        int colIndex = indexes.get(i);

        switch (column.getType()) {
          case BOOLEAN:
            values[i] = BooleanValue.pack(rs.getBoolean(colIndex));
            if (rs.wasNull()) {
              values[i] = null;
            }
            break;

          case DATE:
            Long time = BeeUtils.toLongOrNull(rs.getString(colIndex));
            values[i] = (time == null) ? null : BeeUtils.toString(time / TimeUtils.MILLIS_PER_DAY);
            break;

          case NUMBER:
          case DECIMAL:
            Double d = rs.getDouble(colIndex);
            if (rs.wasNull() || !BeeUtils.isDouble(d)) {
              values[i] = null;
            } else if (column.getScale() >= 0) {
              values[i] = BeeUtils.toString(d, column.getScale());
            } else {
              values[i] = BeeUtils.toString(d, NumberValue.MAX_SCALE);
            }
            break;

          case BLOB:
            byte[] bytes = rs.getBytes(i + 1);
            values[i] = bytes != null ? Codec.toBase64(bytes) : null;
            break;

          default:
            values[i] = rs.getString(colIndex);
        }
      }

      if (idIndex > 0) {
        rowId = rs.getLong(idIndex);
      } else {
        rowId++;
      }

      BeeRow row;
      if (versionIndex > 0) {
        row = new BeeRow(rowId, rs.getLong(versionIndex), values);
      } else {
        row = new BeeRow(rowId, values);
      }

      if (editableIndex > 0) {
        editable = BeeUtils.toBoolean(rs.getString(editableIndex));
      }
      if (removableIndex > 0) {
        removable = BeeUtils.toBoolean(rs.getString(removableIndex));
      }
      row.setEditable(editable);
      row.setRemovable(removable);

      result.addRow(row);
    }

    if (idIndex > 0) {
      result.setViewName(view.getName());
    }
    logger.debug("cols:", cc, "rows:", result.getNumberOfRows());
    return result;
  }

  private static SimpleRowSet rsToSimpleRowSet(ResultSet rs) throws SQLException {
    List<BeeColumn> rsCols = JdbcUtils.getColumns(rs);
    int cc = rsCols.size();
    String[] columns = new String[cc];

    for (int i = 0; i < cc; i++) {
      columns[i] = rsCols.get(i).getId();
    }
    SimpleRowSet res = new SimpleRowSet(columns);

    while (rs.next()) {
      String[] row = new String[cc];

      for (int i = 0; i < cc; i++) {
        if (rsCols.get(i).getType() == ValueType.BLOB) {
          byte[] bytes = rs.getBytes(i + 1);
          row[i] = bytes != null ? Codec.toBase64(bytes) : null;
        } else {
          row[i] = rs.getString(i + 1);
        }
      }
      res.addRow(row);
    }
    logger.debug("cols:", res.getNumberOfColumns(), "rows:", res.getNumberOfRows());
    return res;
  }

  @EJB
  DataSourceBean dsb;
  @EJB
  IdGeneratorBean ig;
  @EJB
  SystemBean sys;
  @EJB
  UserServiceBean usr;
  @EJB
  ParamHolderBean prm;

  public ResponseObject copyData(String tableName, String filterColumn, Object filterValue,
      Object newValue) {

    if (BeeUtils.anyEmpty(tableName, filterColumn) || BeeUtils.anyNull(filterValue, newValue)) {
      return ResponseObject.error("copy data invalid parameters:", tableName, filterColumn,
          filterValue, newValue);
    }
    if (filterValue.equals(newValue)) {
      return ResponseObject.error("copy data", tableName, filterColumn, filterValue, newValue,
          "values must be different");
    }

    Collection<String> fields = sys.getTableFieldNames(tableName);
    if (!fields.contains(filterColumn)) {
      return ResponseObject.error("copy data", tableName, filterColumn, "column not found");
    }

    SqlSelect query = new SqlSelect();
    for (String field : fields) {
      query.addFields(tableName, field);
    }

    query.addFrom(tableName)
        .setWhere(SqlUtils.equals(tableName, filterColumn, filterValue))
        .addOrder(tableName, sys.getIdName(tableName));

    SimpleRowSet data = getData(query);
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.response(0);
    }

    for (SimpleRow row : data) {
      SqlInsert si = new SqlInsert(tableName)
          .addConstant(filterColumn, newValue);

      for (String field : fields) {
        if (!field.equals(filterColumn)) {
          String value = row.getValue(field);
          if (value != null) {
            si.addConstant(field, value);
          }
        }
      }

      ResponseObject response = insertDataWithResponse(si);
      if (response.hasErrors()) {
        return response;
      }
    }

    return ResponseObject.response(data.getNumberOfRows());
  }

  public SimpleRowSet dbConstraints(String dbName, String dbSchema, String table,
      SqlKeyword... types) {
    return getData(SqlUtils.dbConstraints(dbName, dbSchema, table, types));
  }

  public SqlEngine dbEngine(String dsn) {
    SqlEngine sqlEngine = null;

    if (!BeeUtils.isEmpty(dsn)) {
      BeeDataSource bds = dsb.locateDs(dsn);

      if (bds != null) {
        sqlEngine = dbEngine(bds.getDs());
      }
    }
    return sqlEngine;
  }

  public SqlEngine dbEngine(DataSource ds) {
    SqlEngine sqlEngine = null;

    if (ds != null) {
      String engine;
      Connection con = null;
      try {
        con = ds.getConnection();
        engine = con.getMetaData().getDatabaseProductName();

      } catch (SQLException e) {
        logger.error(e);
        engine = null;
      } finally {
        JdbcUtils.closeConnection(con);
      }
      sqlEngine = SqlEngine.detectEngine(engine);
    }
    return sqlEngine;
  }

  public SimpleRowSet dbFields(String dbName, String dbSchema, String table) {
    return getData(SqlUtils.dbFields(dbName, dbSchema, table));
  }

  public SimpleRowSet dbForeignKeys(String dbName, String dbSchema, String table,
      String refTable) {
    return getData(SqlUtils.dbForeignKeys(dbName, dbSchema, table, refTable));
  }

  public SimpleRowSet dbIndexes(String dbName, String dbSchema, String table) {
    return getData(SqlUtils.dbIndexes(dbName, dbSchema, table));
  }

  public String dbName() {
    IsQuery query = SqlUtils.dbName();

    if (!BeeUtils.isEmpty(query.getQuery())) {
      return getValue(query);
    }
    return "";
  }

  public String dbSchema() {
    IsQuery query = SqlUtils.dbSchema();

    if (!BeeUtils.isEmpty(query.getQuery())) {
      return getValue(query);
    }
    return "";
  }

  public boolean dbSchemaExists(String dbName, String schema) {
    Assert.notEmpty(schema);
    return !ArrayUtils.isEmpty(dbSchemas(dbName, schema));
  }

  public String[] dbSchemas(String dbName, String schema) {
    return getColumn(SqlUtils.dbSchemas(dbName, schema));
  }

  public boolean dbTableExists(String dbName, String dbSchema, String table) {
    Assert.notEmpty(table);
    return dbTables(dbName, dbSchema, table).getNumberOfRows() > 0;
  }

  public SimpleRowSet dbTables(String dbName, String dbSchema, String table) {
    return getData(SqlUtils.dbTables(dbName, dbSchema, table));
  }

  public SimpleRowSet dbTriggers(String dbName, String dbSchema, String table) {
    return getData(SqlUtils.dbTriggers(dbName, dbSchema, table));
  }

  @TransactionAttribute(TransactionAttributeType.MANDATORY)
  public Object doSql(String sql) {
    BeeDataSource bds = dsb.locateDs(SqlBuilderFactory.getDsn());
    Assert.notNull(bds);

    return doSql(bds.getDs(), sql);
  }

  public Object doSql(DataSource ds, String sql) {
    Assert.notNull(ds);
    Assert.notEmpty(sql);

    return processSql(ds, sql, new SqlHandler<Object>() {
      @Override
      public Object processError(SQLException ex) {
        logger.error(ex);
        return ex.getMessage();
      }

      @Override
      public Object processResultSet(ResultSet rs) throws SQLException {
        return rsToBeeRowSet(rs, null);
      }

      @Override
      public Object processUpdateCount(int updateCount) {
        logger.debug("affected rows:", updateCount);
        return updateCount;
      }
    });
  }

  public Boolean getBoolean(IsQuery query) {
    return getSingleValue(query).getBoolean(0, 0);
  }

  public Boolean[] getBooleanColumn(IsQuery query) {
    return getSingleColumn(query).getBooleanColumn(0);
  }

  public List<byte[]> getBytesColumn(SqlSelect query) {
    Assert.state(query.getFields().size() == 1, "Only one column allowed");

    return getData(null, query, new ResultSetProcessor<List<byte[]>>() {
      @Override
      public List<byte[]> processResultSet(ResultSet rs) throws SQLException {
        List<byte[]> data = Lists.newArrayList();

        while (rs.next()) {
          data.add(rs.getBytes(1));
        }
        logger.debug("cols:", 1, "rows:", data.size());
        return data;
      }
    });
  }

  public String[] getColumn(IsQuery query) {
    return getSingleColumn(query).getColumn(0);
  }

  public SimpleRowSet getData(IsQuery query) {
    return getData(null, query, new ResultSetProcessor<SimpleRowSet>() {
      @Override
      public SimpleRowSet processResultSet(ResultSet rs) throws SQLException {
        return rsToSimpleRowSet(rs);
      }
    });
  }

  public <T> T getData(DataSource ds, IsQuery query, final ResultSetProcessor<T> callback) {
    Assert.notNull(query);
    Assert.state(!query.isEmpty());

    String sql;

    if (ds == null) {
      activateTables(query);
      sql = query.getQuery();
    } else {
      sql = query.getSqlString(SqlBuilderFactory.getBuilder(dbEngine(ds)));
    }
    return processSql(ds, sql, new SqlHandler<T>() {
      @Override
      public T processResultSet(ResultSet rs) throws SQLException {
        return callback.processResultSet(rs);
      }

      @Override
      public T processUpdateCount(int updateCount) {
        throw new BeeRuntimeException("Query must return a ResultSet");
      }
    });
  }

  public JustDate getDate(IsQuery query) {
    return getSingleValue(query).getDate(0, 0);
  }

  public JustDate[] getDateColumn(IsQuery query) {
    return getSingleColumn(query).getDateColumn(0);
  }

  public DateTime getDateTime(IsQuery query) {
    return getSingleValue(query).getDateTime(0, 0);
  }

  public DateTime[] getDateTimeColumn(IsQuery query) {
    return getSingleColumn(query).getDateTimeColumn(0);
  }

  public BigDecimal getDecimal(IsQuery query) {
    return getSingleValue(query).getDecimal(0, 0);
  }

  public BigDecimal[] getDecimalColumn(IsQuery query) {
    return getSingleColumn(query).getDecimalColumn(0);
  }

  public Double getDouble(IsQuery query) {
    return getSingleValue(query).getDouble(0, 0);
  }

  public Double[] getDoubleColumn(IsQuery query) {
    return getSingleColumn(query).getDoubleColumn(0);
  }

  public SimpleRowSet getHistogram(String viewName, Filter filter, List<String> columns,
      List<String> order) {

    BeeView view = sys.getView(viewName);
    SqlSelect viewQuery = view.getQuery(filter, null, columns, sys.getViewFinder());

    String queryAlias = "Hist_" + SqlUtils.uniqueName();
    String countAlias = "Count_" + SqlUtils.uniqueName();

    SqlSelect ss = new SqlSelect();
    for (String colName : columns) {
      ss.addFields(queryAlias, colName);
      ss.addGroup(queryAlias, colName);
    }

    ss.addCount(countAlias).addFrom(viewQuery, queryAlias);

    if (!BeeUtils.isEmpty(order)) {
      for (String colName : order) {
        ss.addOrder(queryAlias, colName);
      }
    }

    return getData(ss);
  }

  public Long getId(String tableName, String filterColumn, Object filterValue) {
    SqlSelect query = new SqlSelect()
        .addFields(tableName, sys.getIdName(tableName))
        .addFrom(tableName)
        .setWhere(SqlUtils.equals(tableName, filterColumn, filterValue));

    return getLong(query);
  }

  public Long getId(String tableName, String f1, Object v1, String f2, Object v2) {
    SqlSelect query = new SqlSelect()
        .addFields(tableName, sys.getIdName(tableName))
        .addFrom(tableName)
        .setWhere(SqlUtils.equals(tableName, f1, v1, f2, v2));

    return getLong(query);
  }

  public Long getId(String tableName, String f1, Object v1, String f2, Object v2,
      String f3, Object v3) {
    SqlSelect query = new SqlSelect()
        .addFields(tableName, sys.getIdName(tableName))
        .addFrom(tableName)
        .setWhere(SqlUtils.equals(tableName, f1, v1, f2, v2, f3, v3));

    return getLong(query);
  }

  public Integer getInt(IsQuery query) {
    return getSingleValue(query).getInt(0, 0);
  }

  public Integer[] getIntColumn(IsQuery query) {
    return getSingleColumn(query).getIntColumn(0);
  }

  public Long getLong(IsQuery query) {
    return getSingleValue(query).getLong(0, 0);
  }

  public Long[] getLongColumn(IsQuery query) {
    return getSingleColumn(query).getLongColumn(0);
  }

  public List<Long> getLongList(IsQuery query) {
    List<Long> result = Lists.newArrayList();

    Long[] arr = getLongColumn(query);
    if (arr != null && arr.length > 0) {
      for (Long value : arr) {
        result.add(value);
      }
    }

    return result;
  }

  public String getNextNumber(String tblName, String fldName, String prefix, String prefixFld) {
    Object value = null;

    if (!BeeUtils.allEmpty(tblName, fldName)) {
      IsCondition clause = null;
      IsExpression xpr = null;

      if (BeeUtils.isEmpty(prefix)) {
        xpr = SqlUtils.field(tblName, fldName);
      } else {
        if (!BeeUtils.isEmpty(prefixFld)) {
          xpr = SqlUtils.field(tblName, fldName);
          clause = SqlUtils.equals(tblName, prefixFld, prefix);
        } else {
          xpr = SqlUtils.substring(tblName, fldName, prefix.length() + 1);
          clause = SqlUtils.startsWith(tblName, fldName, prefix);
        }
      }
      clause = SqlUtils.and(clause,
          SqlUtils.compare(SqlUtils.length(xpr), Operator.EQ,
              new SqlSelect()
                  .addMax(SqlUtils.length(xpr), "length")
                  .addFrom(tblName)
                  .setWhere(clause)));

      String maxValue = getValue(new SqlSelect()
          .addMax(xpr, "value")
          .addFrom(tblName)
          .setWhere(clause));

      value = BeeUtils.nextString(maxValue);
    }
    return BeeUtils.join(BeeConst.STRING_EMPTY, BeeUtils.isEmpty(prefixFld) ? prefix : null, value);
  }

  public Long[] getRelatedValues(String tableName, String filterColumn, long filterValue,
      String resultColumn) {

    SqlSelect query = new SqlSelect()
        .addFields(tableName, resultColumn)
        .addFrom(tableName)
        .addOrder(tableName, sys.getIdName(tableName));

    boolean selfRelationsMode = BeeUtils.same(filterColumn, resultColumn)
        && BeeUtils.same(tableName, AdministrationConstants.TBL_RELATIONS);

    if (selfRelationsMode) {
      String als = SqlUtils.uniqueName();

      query.addFromInner(tableName, als,
          SqlUtils.and(sys.joinTables(tableName, als, AdministrationConstants.COL_RELATION),
              SqlUtils.notNull(tableName, resultColumn),
              SqlUtils.equals(als, filterColumn, filterValue)));
    } else {
      query.setWhere(SqlUtils.and(SqlUtils.equals(tableName, filterColumn, filterValue),
          SqlUtils.notNull(tableName, resultColumn)));
    }
    return getLongColumn(query);
  }

  public SimpleRow getRow(IsQuery query) {
    SimpleRowSet res = getData(query);
    Assert.notNull(res);
    Assert.isTrue(res.getNumberOfRows() <= 1, "Result must contain zero or one row");
    return res.getRow(0);
  }

  public List<SearchResult> getSearchResults(String viewName, Filter filter) {
    List<SearchResult> results = Lists.newArrayList();

    BeeRowSet rowSet = getViewData(viewName, filter);
    if (rowSet != null) {
      for (BeeRow row : rowSet.getRows()) {
        results.add(new SearchResult(viewName, row));
      }
    }
    return results;
  }

  public String getValue(IsQuery query) {
    return getSingleValue(query).getValue(0, 0);
  }

  public BeeRowSet getViewData(String viewName) {
    return getViewData(viewName, null);
  }

  public BeeRowSet getViewData(String viewName, Filter filter) {
    return getViewData(viewName, filter, null);
  }

  public BeeRowSet getViewData(String viewName, Filter filter, Order order) {
    return getViewData(viewName, filter, order, BeeConst.UNDEF, BeeConst.UNDEF, null);
  }

  public BeeRowSet getViewData(String viewName, Filter filter, Order order, List<String> columns) {
    return getViewData(viewName, filter, order, BeeConst.UNDEF, BeeConst.UNDEF, columns);
  }

  public BeeRowSet getViewData(String viewName, Filter filter, Order order, int limit, int offset,
      List<String> columns) {

    BeeView view = sys.getView(viewName);
    SqlSelect ss = view.getQuery(filter, order, columns, sys.getViewFinder());

    if (limit > 0) {
      ss.setLimit(limit);
    }
    if (offset > 0) {
      ss.setOffset(offset);
    }
    return getViewData(ss, view);
  }

  public BeeRowSet getViewData(final SqlSelect query, final BeeView view) {
    Assert.notNull(query);
    Assert.state(!query.isEmpty());

    String tableName = view.getSourceName();
    String tableAlias = view.getSourceAlias();

    if (!usr.isAdministrator()) {
      sys.filterVisibleState(query, tableName, tableAlias);

      BeeTable table = sys.getTable(tableName);

      for (RightsState state : EnumSet.of(RightsState.EDIT, RightsState.DELETE)) {
        String stateAlias = table.joinState(query, tableAlias, state);

        if (!BeeUtils.isEmpty(stateAlias)) {
          IsExpression xpr = SqlUtils.sqlIf(table.checkState(stateAlias, state,
              usr.getUserRoles()), true, false);

          if (!BeeUtils.isEmpty(query.getGroupBy())) {
            query.addMax(xpr, state.name());
          } else {
            query.addExpr(xpr, state.name());
          }
        }
      }
    }
    activateTables(query);

    final ViewQueryEvent event = new ViewQueryEvent(view.getName(), query);
    sys.postDataEvent(event);

    return processSql(null, query.getQuery(), new SqlHandler<BeeRowSet>() {
      @Override
      public BeeRowSet processResultSet(ResultSet rs) throws SQLException {
        event.setRowset(rsToBeeRowSet(rs, view));
        sys.postDataEvent(event);
        return event.getRowset();
      }

      @Override
      public BeeRowSet processUpdateCount(int updateCount) {
        throw new BeeRuntimeException("Query must return a ResultSet");
      }
    });
  }

  public int getViewSize(String viewName, Filter filter) {
    BeeView view = sys.getView(viewName);
    SqlSelect query = view.getQuery(filter, sys.getViewFinder());

    if (!usr.isAdministrator()) {
      sys.filterVisibleState(query, view.getSourceName(), view.getSourceAlias());
    }
    
    return sqlCount(query);
  }

  @TransactionAttribute(TransactionAttributeType.MANDATORY)
  public long insertData(SqlInsert si) {
    return insertDataWithResponse(si).getResponse(-1L, logger);
  }

  @TransactionAttribute(TransactionAttributeType.MANDATORY)
  public ResponseObject insertDataWithResponse(SqlInsert si) {
    Assert.notNull(si);

    String target = si.getTarget();
    boolean requiresId = !si.isMultipleInsert() && sys.isTable(target);
    long id = 0;

    Assert.state(requiresId || !si.isEmpty());

    if (requiresId) {
      String versionFld = sys.getVersionName(target);

      if (!si.hasField(versionFld)) {
        si.addConstant(versionFld, System.currentTimeMillis());
      }
      String idFld = sys.getIdName(target);

      if (si.hasField(idFld)) {
        Object value = si.getValue(idFld).getValue();

        if (value instanceof Value) {
          id = ((Value) value).getLong();
        }
      } else {
        id = ig.getId(target);
        si.addConstant(idFld, id);
      }
    }
    ResponseObject response = updateDataWithResponse(si);

    if (!response.hasErrors()) {
      response.setResponse(id);
    }
    return response;
  }

  @TransactionAttribute(TransactionAttributeType.MANDATORY)
  public int loadData(String target, SqlSelect sourceQuery) {
    Assert.state(sys.isTable(target));
    boolean isDebugEnabled = logger.isDebugEnabled();

    int chunk = BeeUtils.toNonNegativeInt(sourceQuery.getLimit());
    int offset = 0;
    int tot = 0;

    SimpleRowSet data = null;
    SqlInsert insert = null;

    do {
      if (chunk > 0) {
        sourceQuery.setOffset(offset);
      }
      data = getData(sourceQuery);

      if (insert == null) {
        insert = new SqlInsert(target)
            .addFields(sys.getIdName(target), sys.getVersionName(target))
            .addFields(data.getColumnNames());
      }
      if (isDebugEnabled) {
        logger.setLevel(LogLevel.INFO);
      }
      for (String[] row : data.getRows()) {
        Object[] values = new Object[row.length + 2];
        values[0] = ig.getId(target);
        values[1] = System.currentTimeMillis();
        System.arraycopy(row, 0, values, 2, row.length);
        insert.addValues(values);

        if (++tot % 1e4 == 0) {
          insertData(insert);
          insert.resetValues();
          logger.info("Inserted", tot, "records into table", target);
        }
      }
      if (tot % 1e4 > 0) {
        insertData(insert);
        logger.info("Inserted", tot, "records into table", target);
      }
      if (isDebugEnabled) {
        logger.setLevel(LogLevel.DEBUG);
      }
      offset += chunk;
    } while (chunk > 0 && data.getNumberOfRows() == chunk);

    return tot;
  }

  public long setYearMonth(String target, String dtCol, String yearCol, String monthCol) {
    long result = 0;

    SqlSelect rangeQuery = new SqlSelect()
        .addMin(target, dtCol, SqlUtils.uniqueName())
        .addMax(target, dtCol, SqlUtils.uniqueName())
        .addFrom(target);

    SimpleRowSet rangeData = getData(rangeQuery);
    if (DataUtils.isEmpty(rangeData)) {
      return result;
    }

    DateTime minDate = rangeData.getDateTime(0, 0);
    DateTime maxDate = rangeData.getDateTime(0, 1);

    if (minDate == null || maxDate == null) {
      return result;
    }

    DateTime lower = DateTime.copyOf(minDate);
    while (TimeUtils.isLeq(lower, maxDate)) {
      DateTime upper = TimeUtils.startOfNextMonth(lower).getDateTime();

      SqlUpdate update = new SqlUpdate(target)
          .addConstant(yearCol, lower.getYear())
          .addConstant(monthCol, lower.getMonth())
          .setWhere(SqlUtils.and(SqlUtils.moreEqual(target, dtCol, lower.getTime()),
              SqlUtils.less(target, dtCol, upper.getTime())));

      int count = updateData(update);
      if (count > 0) {
        result += count;
      }

      lower = DateTime.copyOf(upper);
    }
    return result;
  }

  public int sqlCount(SqlSelect query) {
    SimpleRowSet res;
    SqlSelect ss = query.copyOf().resetOrder();

    if (BeeUtils.isEmpty(ss.getGroupBy()) && BeeUtils.isEmpty(ss.getUnion())) {
      res = getData(ss.resetFields().addCount("cnt"));
    } else {
      res = getData(new SqlSelect().addCount("cnt").addFrom(ss, "als"));
    }
    if (res == null) {
      return BeeConst.UNDEF;
    }
    return BeeUtils.unbox(res.getInt(0, 0));
  }

  public int sqlCount(String source, IsCondition where) {
    return sqlCount(new SqlSelect().addConstant(null, "dummy").addFrom(source).setWhere(where));
  }

  public String sqlCreateTemp(SqlSelect query) {
    String tmp = SqlUtils.temporaryName();
    updateData(new SqlCreate(tmp).setDataSource(query));
    return tmp;
  }

  public void sqlDropTemp(String tmp) {
    Assert.state(!sys.isTable(tmp), "Can't drop a base table: " + tmp);
    updateData(SqlUtils.dropTable(tmp));
  }

  public boolean sqlExists(String source, IsCondition where) {
    return sqlCount(new SqlSelect()
        .addConstant(null, "dummy").addFrom(source).setWhere(where)) > 0;
  }

  public boolean sqlExists(String source, String field, Object value) {
    return sqlExists(source, SqlUtils.equals(source, field, value));
  }

  public void sqlIndex(String tmp, String... fields) {
    Assert.state(!sys.isTable(tmp), "Can't index a base table: " + tmp);
    updateData(SqlUtils.createIndex(tmp, SqlUtils.uniqueName(), Lists.newArrayList(fields), false));
  }

  public String sqlValue(String source, String field, long id) {
    return getValue(new SqlSelect()
        .addFields(source, field)
        .addFrom(source)
        .setWhere(sys.idEquals(source, id)));
  }

  @TransactionAttribute(TransactionAttributeType.MANDATORY)
  public int updateBlob(String table, long id, String field, ByteArrayInputStream data)
      throws SQLException {

    String dsn = SqlBuilderFactory.getDsn();
    BeeDataSource bds = dsb.locateDs(dsn);

    if (bds == null) {
      throw new SQLException("Data source [" + dsn + "] not found");
    }
    DataSource dataSource = bds.getDs();
    int cnt = 0;
    Connection con = null;
    PreparedStatement stmt = null;

    try {
      String sql = new SqlUpdate(table)
          .addConstant(field, SqlUtils.expression("?"))
          .setWhere(sys.idEquals(table, id)).getQuery();

      logger.debug("SQL:", sql);

      con = dataSource.getConnection();
      stmt = con.prepareStatement(sql);
      stmt.setBinaryStream(1, data, data.available());

      long start = System.nanoTime();
      stmt.execute();
      logger.debug(String.format("[%.6f]", (System.nanoTime() - start) / 1e9));

      cnt = stmt.getUpdateCount();
      logger.debug("affected rows:", cnt);

    } finally {
      JdbcUtils.closeStatement(stmt);
      JdbcUtils.closeConnection(con);
    }
    return cnt;
  }

  @TransactionAttribute(TransactionAttributeType.MANDATORY)
  public int updateData(IsQuery query) {
    return updateDataWithResponse(query).getResponse(-1, logger);
  }

  @TransactionAttribute(TransactionAttributeType.MANDATORY)
  public ResponseObject updateDataWithResponse(IsQuery query) {
    Assert.notNull(query);
    Assert.state(!query.isEmpty());

    boolean isDebugEnabled = logger.isDebugEnabled();

    if (isDebugEnabled) {
      logger.setLevel(LogLevel.INFO);
    }
    doSql(SqlUtils.setSqlParameter(AdministrationConstants.AUDIT_USER,
        usr.getCurrentUserId()).getQuery());

    if (isDebugEnabled) {
      logger.setLevel(LogLevel.DEBUG);
    }
    activateTables(query);

    final TableModifyEvent event;

    if (query instanceof HasTarget) {
      event = new TableModifyEvent(((HasTarget) query).getTarget(), query);
      sys.postDataEvent(event);

      if (event.hasErrors()) {
        ResponseObject response = new ResponseObject();

        for (String error : event.getErrorMessages()) {
          response.addError(error);
        }
        return response;
      }
    } else {
      event = null;
    }
    ResponseObject res = processSql(null, query.getQuery(), new SqlHandler<ResponseObject>() {
      @Override
      public ResponseObject processResultSet(ResultSet rs) throws SQLException {
        throw new BeeRuntimeException("Data modification query must not return a ResultSet");
      }

      @Override
      public ResponseObject processUpdateCount(int updateCount) {
        if (event != null) {
          event.setUpdateCount(updateCount);
          sys.postDataEvent(event);
        }
        logger.debug("affected rows:", updateCount);
        return ResponseObject.response(updateCount);
      }
    });

    if (res == null) {
      res = ResponseObject.error("System error. Check server log for more details").setResponse(-1);
    }
    return res;
  }

  private void activateTables(IsQuery query) {
    Collection<String> sources = query.getSources();

    if (!BeeUtils.isEmpty(sources)) {
      for (String source : sources) {
        if (sys.isTable(source) && !sys.getTable(source).isActive()) {
          sys.activateTable(source);
        }
      }
    }
  }

  private SimpleRowSet getSingleColumn(IsQuery query) {
    SimpleRowSet res = getData(query);
    Assert.notNull(res);
    Assert.isTrue(res.getNumberOfColumns() == 1, "Result must contain exactly one column");
    return res;
  }

  private SimpleRowSet getSingleValue(IsQuery query) {
    SimpleRowSet res = getData(query);
    Assert.notNull(res);
    Assert.isTrue(res.getNumberOfColumns() == 1, "Result must contain exactly one column");
    Assert.isTrue(res.getNumberOfRows() <= 1, "Result must contain zero or one row");
    return res;
  }

  private <T> T processSql(DataSource ds, String sql, SqlHandler<T> callback) {
    Assert.notEmpty(sql);
    Assert.notNull(callback);

    Connection con = null;
    Statement stmt = null;
    ResultSet rs = null;
    T result = null;
    DataSource dataSource = ds;

    if (dataSource == null) {
      String dsn = SqlBuilderFactory.getDsn();
      BeeDataSource bds = dsb.locateDs(dsn);

      if (bds == null) {
        result = callback.processError(new SQLException("Data source [" + dsn + "] not found"));
        return result;
      }
      dataSource = bds.getDs();
    }
    logger.debug("SQL:", sql);

    try {
      con = dataSource.getConnection();
      stmt = con.createStatement();

      long start = System.nanoTime();
      boolean isResultSet = stmt.execute(sql);
      logger.debug(String.format("[%.6f]", (System.nanoTime() - start) / 1e9));

      if (isResultSet) {
        rs = stmt.getResultSet();
        result = callback.processResultSet(rs);
      } else {
        int cnt = stmt.getUpdateCount();
        result = callback.processUpdateCount(cnt < 0 ? 0 : cnt);
      }

    } catch (SQLException ex) {
      result = callback.processError(ex);
    } finally {
      JdbcUtils.closeResultSet(rs);
      JdbcUtils.closeStatement(stmt);
      JdbcUtils.closeConnection(con);
    }
    return result;
  }
}
