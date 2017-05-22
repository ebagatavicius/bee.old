package com.butent.bee.client.data;

import static com.butent.bee.shared.Service.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.communication.RpcParameter;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Contains methods for getting {@code RowSets} and making POST requests.
 */

public final class Queries {

  public abstract static class DataCallback extends RpcCallback<Collection<BeeRowSet>> {
  }

  public abstract static class IdListCallback extends RpcCallback<String> {
  }

  public abstract static class IntCallback extends RpcCallback<Integer> {
  }

  public abstract static class RowSetCallback extends RpcCallback<BeeRowSet> {
  }

  private static final BeeLogger logger = LogUtils.getLogger(Queries.class);

  private static final int RESPONSE_FROM_CACHE = 0;

  public static List<String> asList(Object... values) {
    List<String> result = new ArrayList<>();
    if (values == null) {
      return result;
    }

    String s;

    for (Object value : values) {
      if (value == null) {
        s = null;

      } else if (value instanceof JustDate) {
        s = BeeUtils.toString(((JustDate) value).getDays());

      } else if (value instanceof DateTime) {
        s = BeeUtils.toString(((DateTime) value).getTime());

      } else {
        s = value.toString();
      }

      result.add(s);
    }

    return result;
  }

  public static boolean checkResponse(String service, String viewName, ResponseObject response,
      Class<?> clazz) {

    return checkResponse(service, BeeConst.UNDEF, viewName, response, clazz,
        new RpcCallback<Object>() {
          @Override
          public void onSuccess(Object result) {
          }
        });
  }

  public static boolean checkResponse(String service, int rpcId, String viewName,
      ResponseObject response, Class<?> clazz, RpcCallback<?> callback) {

    if (callback != null) {
      callback.setRpcId(rpcId);
    }

    if (response == null) {
      error(callback, service, rpcId, viewName, "response is null");
      return false;

    } else if (response.hasErrors()) {
      if (callback != null) {
        callback.onFailure(response.getErrors());
      }
      return false;

    } else if (clazz != null && response.hasResponse() && !response.hasResponse(clazz)) {
      error(callback, service, rpcId, viewName, "response type:", response.getType(),
          "expected:", NameUtils.getClassName(clazz));
      return false;

    } else {
      return true;
    }
  }

  public static boolean checkRowResponse(String service, String viewName, ResponseObject response) {
    return checkResponse(service, viewName, response, BeeRow.class);
  }

  public static void delete(final String viewName, Filter filter, final IntCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notNull(filter, "Delete: filter required");

    List<Property> lst = PropertyUtils.createProperties(VAR_VIEW_NAME, viewName,
        VAR_VIEW_WHERE, filter.serialize());

    ParameterList parameters = new ParameterList(DELETE, RpcParameter.Section.DATA, lst);
    parameters.setSummary(viewName, filter);

    BeeKeeper.getRpc().makePostRequest(parameters, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(DELETE, getRpcId(), viewName, response, Integer.class, callback)) {
          int responseCount = BeeUtils.toInt((String) response.getResponse());
          logger.info(viewName, "deleted", responseCount, "rows");

          if (callback != null) {
            callback.onSuccess(responseCount);
          }
        }
      }
    });
  }

  public static void deleteRow(String viewName, long rowId) {
    delete(viewName, Filter.compareId(rowId), null);
  }

  public static void deleteRow(String viewName, long rowId, long version) {
    deleteRow(viewName, rowId, version, null);
  }

  public static void deleteRow(String viewName, long rowId, IntCallback callback) {
    deleteRow(viewName, rowId, BeeConst.LONG_UNDEF, callback);
  }

  public static void deleteRow(String viewName, long rowId, long version, IntCallback callback) {
    deleteRows(viewName, Collections.singleton(new RowInfo(rowId, version)), callback);
  }

  public static void deleteRowAndFire(final String viewName, final long rowId) {
    delete(viewName, Filter.compareId(rowId), new IntCallback() {
      @Override
      public void onSuccess(Integer result) {
        if (BeeUtils.isPositive(result)) {
          RowDeleteEvent.fire(BeeKeeper.getBus(), viewName, rowId);
        }
      }
    });
  }

  public static void deleteRows(String viewName, Collection<RowInfo> rows) {
    deleteRows(viewName, rows, null);
  }

  public static void deleteRows(final String viewName, Collection<RowInfo> rows,
      final IntCallback callback) {

    Assert.notEmpty(viewName);
    Assert.notNull(rows);

    final int requestCount = rows.size();
    Assert.isPositive(requestCount);

    List<Property> lst = PropertyUtils.createProperties(VAR_VIEW_NAME, viewName,
        VAR_VIEW_ROWS, Codec.beeSerialize(rows));

    ParameterList params = new ParameterList(DELETE_ROWS, RpcParameter.Section.DATA, lst);

    List<Long> rowIds = new ArrayList<>();
    for (RowInfo ri : rows) {
      rowIds.add(ri.getId());
    }
    params.setSummary(viewName, rowIds);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(DELETE_ROWS, getRpcId(), viewName, response, Integer.class, callback)) {
          int responseCount = BeeUtils.toInt((String) response.getResponse());
          String message;

          if (responseCount == requestCount) {
            message = BeeUtils.joinWords(viewName, "deleted", responseCount, "rows");
            logger.info(message);
          } else {
            message = BeeUtils.joinWords(viewName, "deleted", responseCount, "rows of",
                requestCount, "requested");
            logger.warning(message);
          }

          if (callback != null) {
            if (responseCount > 0) {
              callback.onSuccess(responseCount);
            } else {
              callback.onFailure(message);
            }
          }
        }
      }
    });
  }

  public static int getData(Collection<String> viewNames, DataCallback callback) {
    return getData(viewNames, null, callback);
  }

  public static int getData(Collection<String> viewNames, Map<String, Filter> filters,
      DataCallback callback) {
    return getData(viewNames, filters, CachingPolicy.NONE, callback);
  }

  public static int getData(Collection<String> viewNames, Map<String, Filter> filters,
      final CachingPolicy cachingPolicy, final DataCallback callback) {

    Assert.notEmpty(viewNames);
    Assert.notNull(callback);

    final List<BeeRowSet> result = new ArrayList<>();
    final List<String> viewList = new ArrayList<>();

    if (cachingPolicy != null && cachingPolicy.doRead()) {
      for (String viewName : viewNames) {
        BeeRowSet rowSet = BeeUtils.containsKey(filters, viewName)
            ? null : Global.getCache().getRowSet(viewName);

        if (rowSet != null) {
          result.add(rowSet);
        } else {
          viewList.add(viewName);
        }
      }

      if (viewList.isEmpty()) {
        callback.onSuccess(result);
        return RESPONSE_FROM_CACHE;
      }

    } else {
      viewList.addAll(viewNames);
    }

    Assert.notEmpty(viewList);

    final String service = GET_DATA;
    ParameterList parameters = new ParameterList(service);

    parameters.addDataItem(VAR_VIEW_LIST, NameUtils.join(viewList));

    if (!BeeUtils.isEmpty(filters)) {
      for (String viewName : viewList) {
        Filter filter = filters.get(viewName);

        if (filter != null) {
          parameters.addDataItem(VAR_VIEW_WHERE + viewName, filter.serialize());
        }
      }
    }

    parameters.setSummary(viewNames.toString());

    return BeeKeeper.getRpc().makePostRequest(parameters, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(service, getRpcId(), viewList.toString(), response, null, callback)) {
          String[] arr = Codec.beeDeserializeCollection((String) response.getResponse());
          if (ArrayUtils.isEmpty(arr)) {
            error(callback, service, getRpcId(), viewList.toString(),
                "response type:", response.getType());
            return;
          }

          for (String s : arr) {
            BeeRowSet rs = BeeRowSet.restore(s);
            result.add(rs);

            if (cachingPolicy != null && cachingPolicy.doWrite()) {
              Global.getCache().add(Data.getDataInfo(rs.getViewName()), rs);
            }
          }
          callback.onSuccess(result);
        }
      }
    });
  }

  public static void getDistinctLongs(final String viewName, String column, Filter filter,
      final RpcCallback<Set<Long>> callback) {

    Assert.notEmpty(viewName);
    Assert.notEmpty(column);
    Assert.notNull(callback);

    List<Property> lst = PropertyUtils.createProperties(VAR_VIEW_NAME, viewName,
        VAR_COLUMN, column);
    if (filter != null) {
      PropertyUtils.addProperties(lst, VAR_VIEW_WHERE, filter.serialize());
    }

    ParameterList params = new ParameterList(GET_DISTINCT_LONGS, RpcParameter.Section.DATA, lst);
    params.setSummary(viewName, column, filter);

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(GET_DISTINCT_LONGS, getRpcId(), viewName, response, String.class,
            callback)) {

          Set<Long> result = new HashSet<>();
          if (response.hasResponse()) {
            result.addAll(BeeUtils.toLongs(response.getResponseAsString()));
          }

          callback.onSuccess(result);
        }
      }
    });
  }

  public static void getLastUpdated(final String tableName, long rowId, String column,
      final RpcCallback<DateTime> callback) {

    Assert.notEmpty(tableName);
    Assert.notEmpty(column);
    Assert.notNull(callback);

    List<Property> lst = PropertyUtils.createProperties(VAR_TABLE, tableName,
        VAR_ID, BeeUtils.toString(rowId), VAR_COLUMN, column);

    ParameterList params = new ParameterList(GET_LAST_UPDATED, RpcParameter.Section.QUERY, lst);
    params.setSummary(tableName, rowId, column);

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(GET_LAST_UPDATED, getRpcId(), tableName, response, null, callback)) {
          String s = response.hasResponse() ? response.getResponseAsString() : null;
          DateTime dt = BeeUtils.isLong(s) ? DateTime.restore(s) : null;

          callback.onSuccess(dt);
        }
      }
    });
  }

  public static void getRelatedValues(final String tableName, String filterColumn,
      long filterValue, String resultColumn, final IdListCallback callback) {

    Assert.notEmpty(tableName);
    Assert.notEmpty(filterColumn);
    Assert.isTrue(DataUtils.isId(filterValue));
    Assert.notEmpty(resultColumn);
    Assert.notNull(callback);

    List<Property> lst = PropertyUtils.createProperties(VAR_TABLE, tableName,
        VAR_FILTER_COLUMN, filterColumn, VAR_VALUE, filterValue,
        VAR_VALUE_COLUMN, resultColumn);

    ParameterList params = new ParameterList(GET_RELATED_VALUES,
        RpcParameter.Section.QUERY, lst);
    params.setSummary(tableName, filterColumn, filterValue, resultColumn);

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(GET_RELATED_VALUES, getRpcId(), tableName, response,
            String.class, callback)) {
          callback.onSuccess((String) response.getResponse());
        }
      }
    });
  }

  public static void getRow(final String viewName, final Filter filter, List<String> columns,
      final RowCallback callback) {

    Assert.notEmpty(viewName);
    Assert.notNull(filter);
    Assert.notNull(callback);

    final String columnNames;
    if (BeeUtils.isEmpty(columns)) {
      columnNames = null;
    } else {
      columnNames = BeeUtils.join(VIEW_COLUMN_SEPARATOR, columns);
    }

    List<Property> lst = PropertyUtils.createProperties(VAR_VIEW_NAME, viewName,
        VAR_VIEW_WHERE, filter.serialize());
    if (!BeeUtils.isEmpty(columnNames)) {
      PropertyUtils.addProperties(lst, VAR_VIEW_COLUMNS, columnNames);
    }

    ParameterList params = new ParameterList(QUERY, RpcParameter.Section.DATA, lst);
    params.setSummary(viewName, filter);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(QUERY, getRpcId(), viewName, response, BeeRowSet.class, callback)) {
          BeeRowSet rs = BeeRowSet.restore((String) response.getResponse());

          if (rs.getNumberOfRows() == 1) {
            callback.onSuccess(rs.getRow(0));
          } else {
            error(callback, "Get Row:", getRpcId(), viewName, filter.toString(),
                "response number of rows: " + rs.getNumberOfRows());
          }
        }
      }
    });
  }

  public static void getRow(String viewName, long rowId, RowCallback callback) {
    getRow(viewName, Filter.compareId(rowId), null, callback);
  }

  public static void getRowCount(final String viewName, final Filter filter,
      final IntCallback callback) {

    Assert.notEmpty(viewName);
    Assert.notNull(callback);

    List<Property> lst = PropertyUtils.createProperties(VAR_VIEW_NAME, viewName);
    if (filter != null) {
      PropertyUtils.addProperties(lst, VAR_VIEW_WHERE, filter.serialize());
    }

    ParameterList params = new ParameterList(COUNT_ROWS, RpcParameter.Section.DATA, lst);
    params.setSummary(viewName, filter);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(COUNT_ROWS, getRpcId(), viewName, response, Integer.class, callback)) {
          int rowCount = BeeUtils.toInt((String) response.getResponse());
          logger.info(viewName, filter, "row count:", rowCount);

          callback.onSuccess(rowCount);
        }
      }
    });
  }

  public static void getRowSequence(String viewName, final List<Long> rowIds,
      final Callback<List<BeeRow>> callback) {

    Assert.notEmpty(rowIds);
    Assert.notNull(callback);

    getRowSet(viewName, null, Filter.idIn(rowIds), null, new RowSetCallback() {
      @Override
      public void onFailure(String... reason) {
        callback.onFailure(reason);
      }

      @Override
      public void onSuccess(BeeRowSet rowSet) {
        List<BeeRow> result = new ArrayList<>();

        if (!DataUtils.isEmpty(rowSet)) {
          for (long id : rowIds) {
            BeeRow row = rowSet.getRowById(id);
            if (row != null) {
              result.add(row);
            }
          }
        }

        callback.onSuccess(result);
      }
    });
  }

  public static int getRowSet(String viewName, List<String> columns, Filter filter, Order order,
      CachingPolicy cachingPolicy, RowSetCallback callback) {

    return getRowSet(viewName, columns, filter, order, BeeConst.UNDEF, BeeConst.UNDEF,
        cachingPolicy, callback);
  }

  public static int getRowSet(final String viewName, List<String> columns, final Filter filter,
      final Order order, final int offset, final int limit, final CachingPolicy cachingPolicy,
      final Collection<Property> options, final RowSetCallback callback) {

    Assert.notEmpty(viewName);
    Assert.notNull(callback);

    final String columnNames;
    if (BeeUtils.isEmpty(columns)) {
      columnNames = null;
    } else {
      columnNames = BeeUtils.join(VIEW_COLUMN_SEPARATOR, columns);
    }

    if (cachingPolicy != null && cachingPolicy.doRead() && BeeUtils.isEmpty(columnNames)
        && BeeUtils.isEmpty(options)) {
      BeeRowSet rowSet = Global.getCache().getRowSet(viewName, filter, order, offset, limit);
      if (rowSet != null) {
        callback.onSuccess(rowSet);
        return RESPONSE_FROM_CACHE;
      }
    }

    List<Property> lst = PropertyUtils.createProperties(VAR_VIEW_NAME, viewName);
    if (!BeeUtils.isEmpty(columnNames)) {
      PropertyUtils.addProperties(lst, VAR_VIEW_COLUMNS, columnNames);
    }

    if (filter != null) {
      PropertyUtils.addProperties(lst, VAR_VIEW_WHERE, filter.serialize());
    }
    if (order != null) {
      PropertyUtils.addProperties(lst, VAR_VIEW_ORDER, order.serialize());
    }

    String portion;
    if (offset >= 0 && limit > 0) {
      PropertyUtils.addProperties(lst, VAR_VIEW_OFFSET, offset, VAR_VIEW_LIMIT, limit);
      portion = BeeUtils.joinWords(offset, limit);
    } else {
      portion = null;
    }

    if (!BeeUtils.isEmpty(options)) {
      lst.addAll(options);
    }

    ParameterList params = new ParameterList(QUERY, RpcParameter.Section.DATA, lst);
    params.setSummary(viewName, filter, portion, BeeUtils.emptyToNull(options));

    return BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(QUERY, getRpcId(), viewName, response, BeeRowSet.class, callback)) {
          BeeRowSet rs = BeeRowSet.restore((String) response.getResponse());
          if (offset >= 0 && limit > 0) {
            rs.setTableProperty(VAR_VIEW_OFFSET, BeeUtils.toString(offset));
          }

          callback.onSuccess(rs);

          if (cachingPolicy != null && cachingPolicy.doWrite()
              && BeeUtils.isEmpty(columnNames) && isCacheable(options)) {
            Global.getCache().add(Data.getDataInfo(viewName), rs, filter, order, offset, limit);
          }
        }
      }
    });
  }

  public static int getRowSet(String viewName, List<String> columns, Filter filter, Order order,
      int offset, int limit, CachingPolicy cachingPolicy, RowSetCallback callback) {

    return getRowSet(viewName, columns, filter, order, offset, limit, cachingPolicy, null,
        callback);
  }

  public static int getRowSet(String viewName, List<String> columns, Filter filter, Order order,
      int offset, int limit, RowSetCallback callback) {

    return getRowSet(viewName, columns, filter, order, offset, limit, CachingPolicy.NONE, callback);
  }

  public static int getRowSet(String viewName, List<String> columns, Filter filter, Order order,
      RowSetCallback callback) {

    return getRowSet(viewName, columns, filter, order, CachingPolicy.NONE, callback);
  }

  public static int getRowSet(String viewName, List<String> columns, Filter filter,
      RowSetCallback callback) {

    return getRowSet(viewName, columns, filter, null, callback);
  }

  public static int getRowSet(String viewName, List<String> columns, RowSetCallback callback) {
    return getRowSet(viewName, columns, null, null, callback);
  }

  public static void getValue(final String viewName, long rowId, String column,
      final RpcCallback<String> callback) {

    Assert.notEmpty(viewName);
    Assert.notEmpty(column);
    Assert.notNull(callback);

    List<Property> lst = PropertyUtils.createProperties(VAR_VIEW_NAME, viewName,
        VAR_VIEW_ROW_ID, BeeUtils.toString(rowId), VAR_COLUMN, column);

    ParameterList params = new ParameterList(GET_VALUE, RpcParameter.Section.QUERY, lst);
    params.setSummary(viewName, rowId, column);

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(GET_VALUE, getRpcId(), viewName, response, String.class, callback)) {
          callback.onSuccess(response.getResponseAsString());
        }
      }
    });
  }

  public static int insert(String viewName, List<BeeColumn> columns, IsRow row,
      RowCallback callback) {

    Assert.notEmpty(viewName);
    Assert.notEmpty(columns);
    Assert.notNull(row);

    BeeRowSet rs = DataUtils.createRowSetForInsert(viewName, columns, row);
    if (rs == null) {
      if (callback != null) {
        callback.onFailure(viewName, "nothing to insert");
      }
      return 0;
    }

    insertRow(rs, callback);
    return rs.getNumberOfColumns();
  }

  public static void insert(String viewName, List<BeeColumn> columns, List<String> values) {
    insert(viewName, columns, values, null, null);
  }

  public static void insert(String viewName, List<BeeColumn> columns, List<String> values,
      Collection<RowChildren> children, final RowCallback callback) {

    Assert.notEmpty(viewName);
    Assert.notEmpty(columns);
    Assert.notEmpty(values);
    Assert.isTrue(columns.size() == values.size());

    BeeRow row = new BeeRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION, values);
    if (!BeeUtils.isEmpty(children)) {
      row.setChildren(children);
    }

    BeeRowSet rs = new BeeRowSet(columns);
    rs.setViewName(viewName);
    rs.addRow(row);

    insertRow(rs, callback);
  }

  public static void insertAndFire(String viewName, List<BeeColumn> columns, List<String> values) {
    insert(viewName, columns, values, null, new RowInsertCallback(viewName));
  }

  public static void insertRow(BeeRowSet rowSet, final RpcCallback<RowInfo> callback) {
    final String service = INSERT_ROW_SILENTLY;

    if (!checkRowSet(service, rowSet, callback)) {
      return;
    }
    final String viewName = rowSet.getViewName();

    ParameterList params = new ParameterList(service);
    params.setSummary(viewName);

    BeeKeeper.getRpc().sendText(params, Codec.beeSerialize(rowSet), new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(service, getRpcId(), viewName, response, RowInfo.class, callback)) {
          RowInfo rowInfo = RowInfo.restore((String) response.getResponse());
          if (rowInfo == null) {
            error(callback, service, getRpcId(), viewName, "cannot restore rowInfo");
          } else if (callback != null) {
            callback.onSuccess(rowInfo);
          }
        }
      }
    });
  }

  public static void insertRow(BeeRowSet rowSet, RowCallback callback) {
    doRow(INSERT_ROW, rowSet, callback);
  }

  public static void insertRows(BeeRowSet rowSet) {
    if (rowSet == null) {
      error(null, INSERT_ROWS, BeeConst.UNDEF, "rowSet is null");
    } else {
      insertRows(rowSet, new DataChangeCallback(rowSet.getViewName()));
    }
  }

  public static void insertRows(BeeRowSet rowSet, final RpcCallback<RowInfoList> callback) {
    final String svc = INSERT_ROWS;
    if (!checkRowSet(svc, rowSet, callback)) {
      return;
    }

    final String viewName = rowSet.getViewName();

    ParameterList params = new ParameterList(svc);
    params.setSummary(viewName, rowSet.getNumberOfRows());

    BeeKeeper.getRpc().sendText(params, Codec.beeSerialize(rowSet), new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(svc, getRpcId(), viewName, response, RowInfoList.class, callback)) {
          RowInfoList result = RowInfoList.restore(response.getResponseAsString());
          logger.info(viewName, "inserted", result.size(), "rows");

          if (callback != null) {
            callback.onSuccess(result);
          }
        }
      }
    });
  }

  public static boolean isResponseFromCache(int id) {
    return id == RESPONSE_FROM_CACHE;
  }

  public static void mergeRows(final String viewName, long from, long into,
      final IntCallback callback) {

    Assert.notEmpty(viewName);
    Assert.isTrue(DataUtils.isId(from));
    Assert.isTrue(DataUtils.isId(into));
    Assert.isTrue(!Objects.equals(from, into));

    ParameterList params = new ParameterList(MERGE_ROWS);
    params.addQueryItem(VAR_VIEW_NAME, viewName);
    params.addQueryItem(VAR_FROM, from);
    params.addQueryItem(VAR_TO, into);

    params.setSummary(viewName, from, into);

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(MERGE_ROWS, getRpcId(), viewName, response, null, callback)) {
          int size = response.getSize();

          if (size > 0) {
            String[] tables = Codec.beeDeserializeCollection(response.getResponseAsString());

            if (!ArrayUtils.isEmpty(tables)) {
              String mainTable = Data.getViewTable(viewName);

              Set<String> otherTables = new TreeSet<>();
              for (String table : tables) {
                if (!Objects.equals(table, mainTable)) {
                  otherTables.add(table);
                }
              }

              Set<String> viewNames = new TreeSet<>();

              for (DataInfo dataInfo : Data.getDataInfoProvider().getViews()) {
                if (Objects.equals(mainTable, dataInfo.getTableName())) {
                  viewNames.add(dataInfo.getViewName());
                }
              }

              if (!otherTables.isEmpty()) {
                for (DataInfo dataInfo : Data.getDataInfoProvider().getViews()) {
                  boolean ok = dataInfo.getTableName() != null
                      && otherTables.contains(dataInfo.getTableName());

                  if (!ok) {
                    for (ViewColumn vc : dataInfo.getViewColumns()) {
                      if (vc.getTable() != null && otherTables.contains(vc.getTable())) {
                        ok = true;
                        break;
                      }
                    }
                  }

                  if (ok) {
                    viewNames.add(dataInfo.getViewName());
                  }
                }
              }

              logger.info(MERGE_ROWS, "tables", mainTable, otherTables);
              logger.info(MERGE_ROWS, "views", viewNames);

              if (!viewNames.isEmpty()) {
                DataChangeEvent.fireRefresh(BeeKeeper.getBus(), viewNames);
              }
            }
          }

          if (callback != null) {
            callback.onSuccess(size);
          }
        }
      }
    });
  }

  public static void update(String viewName, long rowId, String column, Value value) {
    update(viewName, rowId, column, value, null);
  }

  public static void update(String viewName, long rowId, String column, Value value,
      IntCallback callback) {

    update(viewName, Filter.compareId(rowId), column, value, callback);
  }

  public static void update(String viewName, Filter filter, String column, Value value,
      IntCallback callback) {

    Assert.notNull(value);
    update(viewName, filter, column, value.getString(), callback);
  }

  public static void update(String viewName, Filter filter, String column, String value,
      IntCallback callback) {

    Assert.notEmpty(column);
    update(viewName, filter, Collections.singletonList(column), Collections.singletonList(value),
        callback);
  }

  public static void update(final String viewName, Filter filter, List<String> columns,
      List<String> values, final IntCallback callback) {

    Assert.notEmpty(viewName);
    Assert.notNull(filter);
    Assert.notEmpty(columns);
    Assert.notEmpty(values);

    List<Property> lst = PropertyUtils.createProperties(VAR_VIEW_NAME, viewName,
        VAR_VIEW_WHERE, filter.serialize(),
        VAR_COLUMN, Codec.beeSerialize(columns),
        VAR_VALUE, Codec.beeSerialize(values));

    ParameterList parameters = new ParameterList(UPDATE, RpcParameter.Section.DATA, lst);
    parameters.setSummary(viewName, filter, columns, values);

    BeeKeeper.getRpc().makePostRequest(parameters, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(UPDATE, getRpcId(), viewName, response, Integer.class, callback)
            && callback != null) {
          int responseCount = BeeUtils.toInt((String) response.getResponse());
          callback.onSuccess(responseCount);
        }
      }
    });
  }

  public static int update(String viewName, List<BeeColumn> columns, IsRow oldRow, IsRow newRow,
      Collection<RowChildren> children, RowCallback callback) {

    BeeRowSet rs = DataUtils.getUpdated(viewName, columns, oldRow, newRow, children);

    if (!DataUtils.isEmpty(rs)) {
      updateRow(rs, callback);
      return rs.getNumberOfColumns();

    } else if (!BeeUtils.isEmpty(children)) {
      updateChildren(viewName, oldRow.getId(), children, callback);
      return children.size();

    } else {
      return 0;
    }
  }

  public static void update(String viewName, long rowId, long version, List<BeeColumn> columns,
      List<String> oldValues, List<String> newValues, Collection<RowChildren> children,
      RowCallback callback) {

    if (BeeUtils.isEmpty(columns) && !BeeUtils.isEmpty(children)) {
      updateChildren(viewName, rowId, children, callback);
      return;
    }
    BeeRowSet rs = DataUtils.getUpdated(viewName, rowId, version, columns, oldValues, newValues,
        children);

    if (!DataUtils.isEmpty(rs)) {
      updateRow(rs, callback);
    }
  }

  public static void updateAndFire(final String viewName, long rowId, long version,
      final String colName, String oldValue, String newValue,
      final ModificationEvent.Kind eventKind) {

    Assert.notEmpty(viewName);
    Assert.isTrue(DataUtils.isId(rowId));
    Assert.notEmpty(colName);

    final int colIndex = Data.getColumnIndex(viewName, colName);
    Assert.nonNegative(colIndex);

    if (!BeeUtils.equalsTrimRight(oldValue, newValue)) {
      final BeeColumn column = Data.getColumns(viewName).get(colIndex);
      BeeRowSet rowSet = new BeeRowSet(viewName, Collections.singletonList(column));

      BeeRow row = new BeeRow(rowId, version, Collections.singletonList(oldValue));
      row.preliminaryUpdate(0, newValue);

      rowSet.addRow(row);

      if (eventKind == ModificationEvent.Kind.UPDATE_ROW) {
        updateRow(rowSet, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            RowUpdateEvent.fire(BeeKeeper.getBus(), viewName, result);
          }
        });

      } else if (eventKind == ModificationEvent.Kind.DATA_CHANGE) {
        updateCell(rowSet, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), viewName);
          }
        });

      } else {
        updateCell(rowSet, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            CellUpdateEvent.fire(BeeKeeper.getBus(), viewName, result.getId(), result.getVersion(),
                CellSource.forColumn(column, colIndex), result.getString(0));
          }
        });
      }
    }
  }

  public static void updateCell(BeeRowSet rowSet, RowCallback callback) {
    doRow(UPDATE_CELL, rowSet, callback);
  }

  public static void updateCellAndFire(String viewName, long rowId, long version,
      String colName, String oldValue, String newValue) {

    updateAndFire(viewName, rowId, version, colName, oldValue, newValue,
        ModificationEvent.Kind.UPDATE_CELL);
  }

  public static void updateChildren(final String viewName, long rowId,
      Collection<RowChildren> children, final RowCallback callback) {

    Assert.notEmpty(viewName);
    Assert.isTrue(DataUtils.isId(rowId));
    Assert.notEmpty(children);

    List<Property> lst = PropertyUtils.createProperties(VAR_VIEW_NAME, viewName,
        VAR_VIEW_ROW_ID, rowId, VAR_CHILDREN, Codec.beeSerialize(children));

    ParameterList parameters = new ParameterList(UPDATE_RELATED_VALUES,
        RpcParameter.Section.DATA, lst);
    parameters.setSummary(viewName, rowId);

    BeeKeeper.getRpc().makePostRequest(parameters, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(UPDATE_RELATED_VALUES, getRpcId(), viewName, response,
            BeeRow.class, callback)) {

          if (callback != null) {
            callback.onSuccess(BeeRow.restore((String) response.getResponse()));
          }
        }
      }
    });
  }

  public static void updateRow(BeeRowSet rowSet, RowCallback callback) {
    doRow(UPDATE_ROW, rowSet, callback);
  }

  public static void updateRows(BeeRowSet rowSet) {
    if (rowSet == null) {
      error(null, UPDATE_ROWS, BeeConst.UNDEF, "rowSet is null");
    } else {
      updateRows(rowSet, new DataChangeCallback(rowSet.getViewName()));
    }
  }

  public static void updateRows(BeeRowSet rowSet, final RpcCallback<RowInfoList> callback) {
    final String svc = UPDATE_ROWS;
    if (!checkRowSet(svc, rowSet, callback)) {
      return;
    }

    final String viewName = rowSet.getViewName();

    ParameterList params = new ParameterList(svc);
    params.setSummary(viewName, rowSet.getRowIds());

    BeeKeeper.getRpc().sendText(params, Codec.beeSerialize(rowSet), new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(svc, getRpcId(), viewName, response, RowInfoList.class, callback)) {
          RowInfoList result = RowInfoList.restore(response.getResponseAsString());
          logger.info(viewName, "updated", result.size(), "rows");

          if (callback != null) {
            callback.onSuccess(result);
          }
        }
      }
    });
  }

  private static boolean checkRowSet(String service, BeeRowSet rowSet, RpcCallback<?> callback) {
    if (rowSet == null) {
      error(callback, service, BeeConst.UNDEF, "rowSet is null");
      return false;

    } else if (BeeUtils.isEmpty(rowSet.getViewName())) {
      error(callback, service, BeeConst.UNDEF, "rowSet view name not specified");
      return false;

    } else if (rowSet.getNumberOfColumns() <= 0 || rowSet.getNumberOfRows() <= 0) {
      error(callback, service, BeeConst.UNDEF, rowSet.getViewName(), "rowSet is empty");
      return false;

    } else {
      return true;
    }
  }

  private static void doRow(final String service, BeeRowSet rowSet, final RowCallback callback) {
    if (!checkRowSet(service, rowSet, callback)) {
      return;
    }
    final String viewName = rowSet.getViewName();

    ParameterList params = new ParameterList(service);

    long id = rowSet.getRow(0).getId();
    if (DataUtils.isId(id)) {
      params.setSummary(viewName, id);
    } else {
      params.setSummary(viewName);
    }

    BeeKeeper.getRpc().sendText(params, Codec.beeSerialize(rowSet), new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (checkResponse(service, getRpcId(), viewName, response, BeeRow.class, callback)) {
          BeeRow row = BeeRow.restore((String) response.getResponse());
          if (row == null) {
            error(callback, service, getRpcId(), viewName, "cannot restore row");
          } else if (callback != null) {
            callback.onSuccess(row);
          }
        }
      }
    });
  }

  private static void error(RpcCallback<?> callback, String service, int rpcId,
      String... messages) {

    String rpcMsg = (rpcId > 0) ? BeeUtils.toString(rpcId) : null;
    logger.severe(service, rpcMsg, ArrayUtils.joinWords(messages));

    if (callback != null) {
      callback.onFailure(messages);
    }
  }

  private static boolean isCacheable(Collection<Property> options) {
    if (BeeUtils.isEmpty(options)) {
      return true;

    } else {
      for (Property property : options) {
        if (property != null && VAR_RIGHTS.equals(property.getName())) {
          return false;
        }
      }

      return true;
    }
  }

  private Queries() {
  }
}
