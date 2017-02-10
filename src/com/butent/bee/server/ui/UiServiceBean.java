package com.butent.bee.server.ui;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import static com.butent.bee.shared.Service.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.Config;
import com.butent.bee.server.DataSourceBean;
import com.butent.bee.server.InitializationBean;
import com.butent.bee.server.data.BeeTable;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.IdGeneratorBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SearchBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.io.ExtensionFilter;
import com.butent.bee.server.io.FileUtils;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.server.modules.ec.TecDocBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.utils.InstallCert;
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.RightsObjectType;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.rights.RightsUtils;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.DecoratorConstants;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;
import com.butent.bee.shared.utils.Wildcards;
import com.butent.bee.shared.utils.XmlHelper;
import com.butent.bee.shared.websocket.messages.LogMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Manages <code>rpc_data</code> type service requests from client side, including such services as
 * <code>GET_MENU, GET_FORM, DO_SQL</code>.
 */

@Stateless
@LocalBean
public class UiServiceBean {

  private static BeeLogger logger = LogUtils.getLogger(UiServiceBean.class);

  @EJB
  UiHolderBean ui;
  @EJB
  QueryServiceBean qs;
  @EJB
  IdGeneratorBean ig;
  @EJB
  SystemBean sys;
  @EJB
  DataEditorBean deb;
  @EJB
  UserServiceBean usr;
  @EJB
  DataSourceBean dsb;
  @EJB
  SearchBean search;
  @EJB
  InitializationBean ib;
  @EJB
  TecDocBean tcd;
  @EJB
  NewsBean news;
  @EJB
  FileStorageBean fs;

  public ResponseObject doService(RequestInfo reqInfo) {
    ResponseObject response;

    String svc = BeeUtils.trim(reqInfo.getService());

    switch (svc) {
      case GET_GRID:
        response = getGrid(reqInfo);
        break;
      case GET_FORM:
        response = getForm(reqInfo);
        break;

      case REBUILD:
        response = rebuildData(reqInfo);
        break;
      case DO_SQL:
        response = doSql(reqInfo);
        break;
      case QUERY:
        response = getViewData(reqInfo);
        break;
      case GET_VALUE:
        response = getValue(reqInfo);
        break;
      case GET_DATA:
        response = getData(reqInfo);
        break;

      case GET_DATA_INFO:
        response = getDataInfo(reqInfo);
        break;
      case GENERATE:
        response = generateData(reqInfo);
        break;
      case COUNT_ROWS:
        response = getViewSize(reqInfo);
        break;

      case DELETE_ROWS:
        response = deleteRows(reqInfo);
        break;
      case DELETE:
        response = delete(reqInfo);
        break;

      case UPDATE_CELL:
        response = updateCell(reqInfo);
        break;
      case UPDATE_ROW:
        response = updateRow(reqInfo);
        break;
      case UPDATE_ROWS:
        response = updateRows(reqInfo);
        break;
      case UPDATE:
        response = update(reqInfo);
        break;

      case INSERT_ROW:
        response = insertRow(reqInfo);
        break;
      case INSERT_ROWS:
        response = insertRows(reqInfo);
        break;
      case INSERT_ROW_SILENTLY:
        response = insertRowSilently(reqInfo);
        break;

      case MERGE_ROWS:
        response = mergeRows(reqInfo);
        break;

      case GET_VIEW_INFO:
        response = getViewInfo(reqInfo);
        break;
      case GET_TABLE_INFO:
        response = getTableInfo(reqInfo);
        break;

      case GET_DSNS:
        response = getDsns();
        break;
      case SWITCH_DSN:
        response = switchDsn(reqInfo.getParameter(VAR_DSN));
        break;

      case SEARCH:
        response = search.processQuery(reqInfo.getParameter(0));
        break;
      case HISTOGRAM:
        response = getHistogram(reqInfo);
        break;
      case GET_DISTINCT_LONGS:
        response = getDistinctLongs(reqInfo);
        break;

      case GET_RELATED_VALUES:
        response = getRelatedValues(reqInfo);
        break;
      case UPDATE_RELATED_VALUES:
        String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
        Long parentId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_VIEW_ROW_ID));
        String serialized = reqInfo.getParameter(VAR_CHILDREN);
        response = updateRelatedValues(viewName, parentId, serialized);
        break;

      case GET_DECORATORS:
        response = getDecorators();
        break;

      case GET_AUTOCOMPLETE:
        response = getAutocomplete();
        break;
      case UPDATE_AUTOCOMPLETE:
        response = updateAutocomplete(reqInfo);
        break;

      case GET_NEWS:
        response = news.getNews(Feed.split(reqInfo.getParameter(VAR_FEED)));
        break;
      case SUBSCRIBE_TO_FEEDS:
        response = news.subscribe(reqInfo);
        break;
      case ACCESS:
        response = news.onAccess(reqInfo);
        break;

      case GET_STATE_RIGHTS:
        response =
            usr.getStateRights(
                EnumUtils.getEnumByIndex(RightsObjectType.class, reqInfo
                    .getParameter(COL_OBJECT_TYPE)),
                EnumUtils.getEnumByIndex(RightsState.class, reqInfo.getParameter(COL_STATE)));
        break;

      case GET_ROLE_RIGHTS:
        response =
            usr.getRoleRights(
                EnumUtils.getEnumByIndex(RightsObjectType.class, reqInfo
                    .getParameter(COL_OBJECT_TYPE)),
                BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ROLE)));
        break;

      case SET_STATE_RIGHTS:
        response =
            usr.setStateRights(
                EnumUtils.getEnumByIndex(RightsObjectType.class, reqInfo
                    .getParameter(COL_OBJECT_TYPE)),
                EnumUtils.getEnumByIndex(RightsState.class, reqInfo.getParameter(COL_STATE)),
                Codec.deserializeLinkedHashMap(reqInfo.getParameter(COL_OBJECT)));
        break;

      case SET_ROLE_RIGHTS:
        response =
            usr.setRoleRights(
                EnumUtils.getEnumByIndex(RightsObjectType.class, reqInfo
                    .getParameter(COL_OBJECT_TYPE)),
                BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ROLE)),
                Codec.deserializeLinkedHashMap(reqInfo.getParameter(COL_OBJECT)));
        break;

      case SET_ROW_RIGHTS:
        response = setRowRights(reqInfo);
        break;

      case GET_GRID_SETTINGS:
        response = getGridAndColumnSettings();
        break;
      case ENSURE_GRID_SETTINGS:
        response = ensureGridSettings(reqInfo);
        break;
      case COPY_GRID_SETTINGS:
        response = copyGridSettings(reqInfo);
        break;

      case GET_LAST_UPDATED:
        response = getLastUpdated(reqInfo);
        break;

      default:
        String msg = BeeUtils.joinWords("data service not recognized:", svc);
        logger.warning(msg);
        response = ResponseObject.error(msg);
    }

    return response;
  }

  public ResponseObject getAutocomplete() {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_AUTOCOMPLETE, COL_AUTOCOMPLETE_KEY, COL_AUTOCOMPLETE_VALUE)
        .addFrom(TBL_AUTOCOMPLETE)
        .setWhere(SqlUtils.equals(TBL_AUTOCOMPLETE, COL_AUTOCOMPLETE_USER, usr.getCurrentUserId()))
        .addOrder(TBL_AUTOCOMPLETE, COL_AUTOCOMPLETE_KEY, sys.getVersionName(TBL_AUTOCOMPLETE));

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      List<String> result = new ArrayList<>();

      ListMultimap<String, String> map = ArrayListMultimap.create();
      for (SimpleRow row : data) {
        map.put(row.getValue(COL_AUTOCOMPLETE_KEY), row.getValue(COL_AUTOCOMPLETE_VALUE));
      }

      for (String key : map.keySet()) {
        result.add(key);

        List<String> values = map.get(key);
        result.add(Integer.toString(values.size()));
        result.addAll(values);
      }

      return ResponseObject.response(result);

    } else {
      return ResponseObject.emptyResponse();
    }
  }

  public ResponseObject getDecorators() {
    File dir = new File(Config.WEB_INF_DIR, DecoratorConstants.DIRECTORY);
    List<File> files = FileUtils.findFiles(dir, Lists.newArrayList(FileUtils.INPUT_FILTER,
        new ExtensionFilter(XmlUtils.DEFAULT_XML_EXTENSION)));
    if (files.isEmpty()) {
      return ResponseObject.error("getDecorators: no xml found in", dir.getPath());
    }

    Document dstDoc = XmlUtils.createDocument();
    Element dstRoot = dstDoc.createElement(DecoratorConstants.TAG_DECORATORS);
    dstRoot.setAttribute(XmlHelper.ATTR_XMLNS, DecoratorConstants.NAMESPACE);
    dstDoc.appendChild(dstRoot);

    for (File file : files) {
      String path = file.getPath();

      Document srcDoc =
          XmlUtils.getXmlResource(path, Config.getSchemaPath(DecoratorConstants.SCHEMA));
      if (srcDoc == null) {
        return ResponseObject.error("getDecorators: cannot load xml:", path);
      }

      List<Element> elements = XmlUtils.getChildrenElements(srcDoc.getDocumentElement(),
          Sets.newHashSet(DecoratorConstants.TAG_ABSTRACT, DecoratorConstants.TAG_DECORATOR));
      if (elements.isEmpty()) {
        logger.warning("no decorators found in", path);
      }

      for (Element decorator : elements) {
        dstRoot.appendChild(dstDoc.importNode(decorator, true));
      }
      logger.debug(elements.size(), "decorators loaded from", path);
    }
    return ResponseObject.response(XmlUtils.toString(dstDoc, false));
  }

  public BeeRowSet getFavorites() {
    return qs.getViewData(VIEW_FAVORITES, usr.getCurrentUserFilter(COL_FAVORITE_USER));
  }

  public BeeRowSet getFilters() {
    Order order = new Order(COL_FILTER_KEY, true);
    order.add(COL_FILTER_ORDINAL, true);

    return qs.getViewData(VIEW_FILTERS, usr.getCurrentUserFilter(COL_FILTER_USER), order);
  }

  public ResponseObject getGridAndColumnSettings() {
    BeeRowSet gridSettings = qs.getViewData(GridDescription.VIEW_GRID_SETTINGS,
        usr.getCurrentUserFilter(GridDescription.COL_GRID_SETTING_USER));

    if (DataUtils.isEmpty(gridSettings)) {
      return ResponseObject.emptyResponse();

    } else {
      BeeRowSet columnSettings = qs.getViewData(ColumnDescription.VIEW_COLUMN_SETTINGS,
          Filter.any(ColumnDescription.COL_GRID_SETTING, gridSettings.getRowIds()));

      return ResponseObject.response(Pair.of(gridSettings, DataUtils.emptyToNull(columnSettings)));
    }
  }

  public BeeRowSet getReportSettings() {
    return qs.getViewData(VIEW_REPORT_SETTINGS,
        Filter.and(usr.getCurrentUserFilter(COL_RS_USER), Filter.notNull(COL_RS_CAPTION)));
  }

  public BeeRowSet getWorkspaces() {
    return qs.getViewData(VIEW_WORKSPACES, usr.getCurrentUserFilter(COL_USER));
  }

  private ResponseObject copyGridSettings(RequestInfo reqInfo) {
    Long id = reqInfo.getParameterLong(VAR_ID);
    if (!DataUtils.isId(id)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_ID);
    }

    List<Long> users =
        DataUtils.parseIdList(reqInfo.getParameter(GridDescription.COL_GRID_SETTING_USER));
    if (BeeUtils.isEmpty(users)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(),
          GridDescription.COL_GRID_SETTING_USER);
    }

    BeeRowSet source = qs.getViewData(GridDescription.VIEW_GRID_SETTINGS, Filter.compareId(id));
    if (DataUtils.isEmpty(source)) {
      return ResponseObject.error(reqInfo.getService(), GridDescription.VIEW_GRID_SETTINGS, id,
          "not found");
    }

    BeeRow row = source.getRow(0);
    int index = source.getColumnIndex(GridDescription.COL_GRID_SETTING_USER);

    boolean hasChildren = qs.sqlExists(ColumnDescription.TBL_COLUMN_SETTINGS,
        SqlUtils.equals(ColumnDescription.TBL_COLUMN_SETTINGS, ColumnDescription.COL_GRID_SETTING,
            id));

    for (Long user : users) {
      row.setValue(index, user);

      BeeRowSet insert = DataUtils.createRowSetForInsert(source.getViewName(), source.getColumns(),
          row);
      ResponseObject response = deb.commitRow(insert);

      if (response.hasErrors()) {
        return response;
      }

      if (hasChildren && response.hasResponse(BeeRow.class)) {
        long newId = ((BeeRow) response.getResponse()).getId();
        qs.copyData(ColumnDescription.TBL_COLUMN_SETTINGS, ColumnDescription.COL_GRID_SETTING,
            id, newId);
      }
    }

    return ResponseObject.response(users.size());
  }

  private ResponseObject delete(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    Assert.notEmpty(viewName);
    String where = reqInfo.getParameter(VAR_VIEW_WHERE);
    Assert.notEmpty(where);

    BeeView view = sys.getView(viewName);
    if (view.isReadOnly()) {
      return ResponseObject.error("View", BeeUtils.bracket(view.getName()), "is read only.");
    }

    String tblName = view.getSourceName();
    Filter filter = Filter.restore(where);

    SqlDelete delete = new SqlDelete(tblName)
        .setWhere(view.getCondition(filter));
    return qs.updateDataWithResponse(delete);
  }

  private ResponseObject deleteRows(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    Assert.notEmpty(viewName);
    String[] entries = Codec.beeDeserializeCollection(reqInfo.getParameter(VAR_VIEW_ROWS));
    Assert.isPositive(ArrayUtils.length(entries));
    RowInfo[] rows = new RowInfo[entries.length];

    for (int i = 0; i < entries.length; i++) {
      rows[i] = RowInfo.restore(entries[i]);
    }
    return deb.deleteRows(viewName, rows);
  }

  private ResponseObject doSql(RequestInfo reqInfo) {
    String sql = reqInfo.getContent();

    if (BeeUtils.isEmpty(sql)) {
      return ResponseObject.error("SQL command not found");
    }
    Object res = qs.doSql(sql);

    if (res instanceof BeeRowSet) {
      ResponseObject resp = ResponseObject.response(res);
      int rc = ((BeeRowSet) res).getNumberOfRows();
      resp.addWarning(usr.getDictionary().rowsRetrieved(rc));
      return resp;
    } else if (res instanceof Number) {
      return ResponseObject.warning("Affected rows:", res);
    } else {
      return ResponseObject.error(res);
    }
  }

  private ResponseObject ensureGridSettings(RequestInfo reqInfo) {
    String key = reqInfo.getParameter(GridDescription.COL_GRID_SETTING_KEY);
    if (BeeUtils.isEmpty(key)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(),
          GridDescription.COL_GRID_SETTING_KEY);
    }

    Long user = reqInfo.getParameterLong(GridDescription.COL_GRID_SETTING_USER);
    if (!DataUtils.isId(user)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(),
          GridDescription.COL_GRID_SETTING_USER);
    }

    Filter filter = Filter.and(
        Filter.equals(GridDescription.COL_GRID_SETTING_KEY, key),
        Filter.equals(GridDescription.COL_GRID_SETTING_USER, user));

    BeeRowSet rowSet = qs.getViewData(GridDescription.VIEW_GRID_SETTINGS, filter);

    if (DataUtils.isEmpty(rowSet)) {
      SqlInsert insert = new SqlInsert(GridDescription.TBL_GRID_SETTINGS)
          .addConstant(GridDescription.COL_GRID_SETTING_KEY, key)
          .addConstant(GridDescription.COL_GRID_SETTING_USER, user);

      ResponseObject response = qs.insertDataWithResponse(insert);
      if (response.hasErrors()) {
        return response;
      }

      rowSet = qs.getViewData(GridDescription.VIEW_GRID_SETTINGS, filter);
    }

    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.error(reqInfo.getService(), GridDescription.VIEW_GRID_SETTINGS, filter,
          "not found");
    } else {
      return ResponseObject.response(rowSet.getRow(0));
    }
  }

  private ResponseObject generateData(RequestInfo reqInfo) {
    ResponseObject response;

    String[] arr = BeeUtils.split(reqInfo.getContent(), BeeConst.CHAR_SPACE);
    String tableName = ArrayUtils.getQuietly(arr, 0);
    int rowCount = BeeUtils.toInt(ArrayUtils.getQuietly(arr, 1));
    int refCount = BeeUtils.toInt(ArrayUtils.getQuietly(arr, 2));
    int childCount = BeeUtils.toInt(ArrayUtils.getQuietly(arr, 3));

    if (BeeUtils.isEmpty(tableName)) {
      return ResponseObject.error("Syntax: gen <table> <rowCount> [refCount] [childCount]");
    } else if (!sys.isTable(tableName)) {
      response = ResponseObject.error("Unknown table:", tableName);
    } else if (rowCount <= 0 || rowCount > 100000) {
      response = ResponseObject.error("Invalid row count:", rowCount);
    } else {
      Set<String> cache = new HashSet<>();
      response = deb.generateData(tableName, rowCount, refCount, childCount, cache);
    }
    return response;
  }

  private ResponseObject getData(RequestInfo reqInfo) {
    String viewList = reqInfo.getParameter(VAR_VIEW_LIST);
    if (BeeUtils.isEmpty(viewList)) {
      return ResponseObject.parameterNotFound(VAR_VIEW_LIST);
    }

    List<String> viewNames = NameUtils.toList(viewList);
    List<BeeRowSet> result = new ArrayList<>();

    for (String viewName : viewNames) {
      String where = reqInfo.getParameter(VAR_VIEW_WHERE + viewName);
      Filter filter = BeeUtils.isEmpty(where) ? null : Filter.restore(where);

      BeeRowSet rs = qs.getViewData(viewName, filter);
      result.add(rs);
    }
    return ResponseObject.response(result);
  }

  private ResponseObject getDataInfo(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    if (BeeUtils.isEmpty(viewName)) {
      return ResponseObject.response(sys.getDataInfo());
    } else {
      DataInfo dataInfo = sys.getDataInfo(viewName);
      return ResponseObject.response(dataInfo);
    }
  }

  private ResponseObject getDistinctLongs(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    if (BeeUtils.isEmpty(viewName)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_VIEW_NAME);
    }

    String column = reqInfo.getParameter(VAR_COLUMN);
    if (BeeUtils.isEmpty(column)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_COLUMN);
    }

    String where = reqInfo.getParameter(VAR_VIEW_WHERE);
    Filter filter = BeeUtils.isEmpty(where) ? null : Filter.restore(where);

    Set<Long> values = qs.getDistinctLongs(viewName, column, filter);
    String s = BeeUtils.joinLongs(values);

    if (BeeUtils.isEmpty(s)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(s);
    }
  }

  private ResponseObject getDsns() {
    return ResponseObject.response(dsb.getDsns());
  }

  private ResponseObject getForm(RequestInfo reqInfo) {
    String formName = reqInfo.getContent();
    return ui.getForm(formName);
  }

  private ResponseObject getGrid(RequestInfo reqInfo) {
    String gridName = reqInfo.getContent();
    return ui.getGrid(gridName);
  }

  private ResponseObject getHistogram(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    String columns = reqInfo.getParameter(VAR_VIEW_COLUMNS);

    String where = reqInfo.getParameter(VAR_VIEW_WHERE);
    String order = reqInfo.getParameter(VAR_VIEW_ORDER);

    Filter filter = BeeUtils.isEmpty(where) ? null : Filter.restore(where);

    SimpleRowSet res = qs.getHistogram(viewName, filter, NameUtils.toList(columns),
        NameUtils.toList(order));
    return ResponseObject.response(res);
  }

  private ResponseObject getLastUpdated(RequestInfo reqInfo) {
    String tableName = reqInfo.getParameter(VAR_TABLE);
    if (BeeUtils.isEmpty(tableName)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_TABLE);
    }

    Long id = reqInfo.getParameterLong(VAR_ID);
    if (!DataUtils.isId(id)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_ID);
    }

    String fieldName = reqInfo.getParameter(VAR_COLUMN);
    if (BeeUtils.isEmpty(fieldName)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_COLUMN);
    }

    if (!sys.isAuditable(tableName)) {
      String message = BeeUtils.joinWords("table", tableName, "is not auditable");
      logger.warning(reqInfo.getService(), message);

      return ResponseObject.warning(message);
    }

    String source = sys.getAuditSource(tableName);

    SqlSelect query = new SqlSelect()
        .addMax(source, AUDIT_FLD_TIME)
        .addFrom(source)
        .setWhere(SqlUtils.equals(source, AUDIT_FLD_ID, id, AUDIT_FLD_FIELD, fieldName));

    Long time = qs.getLong(query);

    if (time == null) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(BeeUtils.toString(time));
    }
  }

  private ResponseObject getRelatedValues(RequestInfo reqInfo) {
    String tableName = reqInfo.getParameter(VAR_TABLE);
    if (BeeUtils.isEmpty(tableName)) {
      return ResponseObject.parameterNotFound(VAR_TABLE);
    }

    String filterColumn = reqInfo.getParameter(VAR_FILTER_COLUMN);
    if (BeeUtils.isEmpty(filterColumn)) {
      return ResponseObject.parameterNotFound(VAR_FILTER_COLUMN);
    }

    Long filterValue = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_VALUE));
    if (!DataUtils.isId(filterValue)) {
      return ResponseObject.parameterNotFound(VAR_VALUE);
    }

    String resultColumn = reqInfo.getParameter(VAR_VALUE_COLUMN);
    if (BeeUtils.isEmpty(resultColumn)) {
      return ResponseObject.parameterNotFound(VAR_VALUE_COLUMN);
    }

    Long[] values = qs.getRelatedValues(tableName, filterColumn, filterValue, resultColumn);
    String response = DataUtils.buildIdList(values);

    return ResponseObject.response(Strings.nullToEmpty(response));
  }

  private ResponseObject getTableInfo(RequestInfo reqInfo) {
    String tableName = reqInfo.getParameter(0);
    List<ExtendedProperty> info = new ArrayList<>();

    if (sys.isTable(tableName)) {
      info.addAll(sys.getTableInfo(tableName));

    } else {
      List<String> names = sys.getTableNames();
      Collections.sort(names);

      if (BeeUtils.isEmpty(tableName) || BeeUtils.same(tableName, BeeConst.STRING_MINUS)) {
        Collection<BeeView> views = sys.getViews();

        Multimap<String, String> sources = HashMultimap.create();
        for (BeeView view : views) {
          sources.put(view.getSourceName(), view.getName());
        }

        boolean noViews = BeeUtils.same(tableName, BeeConst.STRING_MINUS);

        for (String name : names) {
          int fieldCount = sys.getTable(name).getFieldCount();

          String viewNames;
          if (sources.containsKey(name)) {
            if (noViews) {
              continue;
            }
            viewNames = sources.get(name).toString();

          } else {
            viewNames = BeeConst.STRING_MINUS;
          }

          info.add(new ExtendedProperty(name, String.valueOf(fieldCount), viewNames));
        }

      } else if (BeeUtils.same(tableName, BeeConst.STRING_ASTERISK)) {
        for (String name : names) {
          PropertyUtils.appendWithPrefix(info, name, sys.getTableInfo(name));
        }

      } else {
        for (String name : names) {
          if (BeeUtils.containsSame(name, tableName)) {
            PropertyUtils.appendWithPrefix(info, name, sys.getTableInfo(name));
          }
        }

        if (info.isEmpty()) {
          return ResponseObject.warning("table not found", tableName);
        }
      }
    }
    return ResponseObject.collection(info, ExtendedProperty.class);
  }

  private ResponseObject getValue(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    String rowId = reqInfo.getParameter(VAR_VIEW_ROW_ID);
    String column = reqInfo.getParameter(VAR_COLUMN);

    Filter filter = Filter.compareId(BeeUtils.toLong(rowId));

    BeeRowSet rowSet = qs.getViewData(viewName, filter, null, BeeConst.UNDEF, BeeConst.UNDEF,
        Lists.newArrayList(column));

    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.response(null, String.class).addWarning("row not found", viewName,
          rowId);
    } else {
      String value = rowSet.getString(0, column);
      return ResponseObject.response(value);
    }
  }

  private ResponseObject getViewData(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    String columns = reqInfo.getParameter(VAR_VIEW_COLUMNS);

    int limit = BeeUtils.toInt(reqInfo.getParameter(VAR_VIEW_LIMIT));
    int offset = BeeUtils.toInt(reqInfo.getParameter(VAR_VIEW_OFFSET));

    String where = reqInfo.getParameter(VAR_VIEW_WHERE);
    String sort = reqInfo.getParameter(VAR_VIEW_ORDER);

    String getSize = reqInfo.getParameter(VAR_VIEW_SIZE);
    String rowId = reqInfo.getParameter(VAR_VIEW_ROW_ID);

    String rights = reqInfo.getParameter(VAR_RIGHTS);

    Filter filter = null;
    if (!BeeUtils.isEmpty(rowId)) {
      filter = Filter.compareId(BeeUtils.toLong(rowId));
    } else if (!BeeUtils.isEmpty(where)) {
      filter = Filter.restore(where);
    }
    Order order = null;
    if (!BeeUtils.isEmpty(sort)) {
      order = Order.restore(sort);
    }

    List<String> colNames = NameUtils.toList(columns);

    int cnt = BeeConst.UNDEF;
    if (!BeeUtils.isEmpty(getSize)) {
      cnt = qs.getViewSize(viewName, filter);
    }
    BeeRowSet res = qs.getViewData(viewName, filter, order, limit, offset, colNames);

    if (cnt >= 0 && res != null) {
      res.setTableProperty(VAR_VIEW_SIZE, BeeUtils.toString(Math.max(cnt, res.getNumberOfRows())));
    }

    if (!BeeUtils.isEmpty(rights) && !DataUtils.isEmpty(res)) {
      getViewRights(res, rights);
    }

    return ResponseObject.response(res);
  }

  private ResponseObject getViewInfo(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(0);
    List<ExtendedProperty> info = new ArrayList<>();

    if (!BeeUtils.isEmpty(viewName)) {
      if (sys.isView(viewName)) {
        info.addAll(sys.getView(viewName).getExtendedInfo());
      } else {
        return ResponseObject.warning("Unknown view name:", viewName);
      }

    } else {
      List<String> names = new ArrayList<>(sys.getViewNames());
      Collections.sort(names);

      for (String name : names) {
        int cc = sys.getView(name).getColumnCount();
        int rc = qs.getViewSize(name, null);

        info.add(new ExtendedProperty(name, BeeUtils.toString(cc), BeeUtils.toString(rc)));
      }
    }

    return ResponseObject.collection(info, ExtendedProperty.class);
  }

  private void getViewRights(BeeRowSet rowSet, String queryStates) {
    BeeView view = sys.getView(rowSet.getViewName());
    String tableName = view.getSourceName();
    String idName = view.getSourceIdName();

    BeeTable table = sys.getTable(tableName);

    Set<RightsState> states = table.getStates();
    if (!BeeUtils.isEmpty(queryStates) && !Wildcards.isDefaultAny(queryStates)) {
      states.retainAll(EnumUtils.parseIndexSet(RightsState.class, queryStates));
    }

    if (states.isEmpty()) {
      logger.warning(tableName, queryStates, "states not defined");
      return;
    }

    Set<RightsState> existingStates = new HashSet<>();

    List<Long> roles = new ArrayList<>();
    roles.add(0L);
    roles.addAll(usr.getRoles());

    SqlSelect query = new SqlSelect()
        .addFields(tableName, idName)
        .addFrom(tableName)
        .setWhere(SqlUtils.inList(tableName, idName, rowSet.getRowIds()));

    for (RightsState state : states) {
      String stateAlias = table.joinState(query, tableName, state);

      if (!BeeUtils.isEmpty(stateAlias)) {
        for (Long role : roles) {
          IsExpression xpr = SqlUtils.sqlIf(table.checkState(stateAlias, state, role),
              true, false);
          query.addExpr(xpr, RightsUtils.getAlias(state, role));
        }

        existingStates.add(state);
      }
    }

    SimpleRowSet rs = existingStates.isEmpty() ? null : qs.getData(query);

    boolean value;
    Long userId = usr.getCurrentUserId();

    for (BeeRow row : rowSet) {
      String rowKey = BeeUtils.toString(row.getId());

      for (RightsState state : states) {
        for (Long role : roles) {
          String alias = RightsUtils.getAlias(state, role);

          if (existingStates.contains(state)) {
            value = BeeUtils.toBoolean(rs.getValueByKey(idName, rowKey, alias));
          } else {
            value = state.isChecked();
          }

          row.setProperty(alias, userId, Codec.pack(value));
        }
      }
    }
  }

  private ResponseObject getViewSize(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    String where = reqInfo.getParameter(VAR_VIEW_WHERE);

    Filter filter = null;
    if (!BeeUtils.isEmpty(where)) {
      filter = Filter.restore(where);
    }
    return ResponseObject.response(qs.getViewSize(viewName, filter));
  }

  private ResponseObject insertRow(RequestInfo reqInfo) {
    return deb.commitRow(BeeRowSet.restore(reqInfo.getContent()));
  }

  private ResponseObject insertRows(RequestInfo reqInfo) {
    BeeRowSet rowSet = BeeRowSet.restore(reqInfo.getContent());
    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.error(reqInfo.getService(), "row set is empty");
    }

    RowInfoList result = new RowInfoList(rowSet.getNumberOfRows());

    for (int i = 0; i < rowSet.getNumberOfRows(); i++) {
      ResponseObject response = deb.commitRow(rowSet, i, RowInfo.class);

      if (response.hasErrors()) {
        logger.severe("error inserting into", rowSet.getViewName());
        logger.severe(rowSet.getRow(i));

        return response;

      } else if (response.hasResponse(RowInfo.class)) {
        result.add((RowInfo) response.getResponse());
      }
    }

    return ResponseObject.response(result);
  }

  private ResponseObject insertRowSilently(RequestInfo reqInfo) {
    return deb.commitRow(BeeRowSet.restore(reqInfo.getContent()), RowInfo.class);
  }

  private ResponseObject mergeRows(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    if (BeeUtils.isEmpty(viewName)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_VIEW_NAME);
    }

    Long from = reqInfo.getParameterLong(VAR_FROM);
    if (!DataUtils.isId(from)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_FROM);
    }
    Long into = reqInfo.getParameterLong(VAR_TO);
    if (!DataUtils.isId(into)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_TO);
    }

    if (Objects.equals(from, into)) {
      return ResponseObject.error(reqInfo.getService(), viewName, VAR_FROM, from, VAR_TO, into);
    }

    if (!sys.isView(viewName)) {
      return ResponseObject.error(reqInfo.getService(), viewName, "view not found");
    }

    String tableName = sys.getViewSource(viewName);
    if (BeeUtils.isEmpty(tableName)) {
      return ResponseObject.error(reqInfo.getService(), viewName, "source not available");
    }

    ResponseObject response = qs.mergeData(tableName, from, into, true);
    if (response.hasErrors() || response.isEmpty()) {
      return response;
    }

    return qs.mergeData(tableName, from, into, false);
  }

  private ResponseObject rebuildData(RequestInfo reqInfo) {
    ResponseObject response = new ResponseObject();

    String cmd = reqInfo.getContent();

    if (BeeUtils.same(cmd, "all")) {
      sys.rebuildActiveTables();
      response.addInfo("Recreate structure OK");

    } else if (BeeUtils.same(cmd, "handling")) {
      // TODO: remove
      qs.updateData(new SqlUpdate(TBL_CARGO_HANDLING)
          .addExpression(COL_CARGO,
              SqlUtils.field(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO)))
          .setFrom(TBL_ORDER_CARGO,
              sys.joinTables(TBL_CARGO_HANDLING, TBL_ORDER_CARGO, "CargoHandling"))
          .setWhere(SqlUtils.and(SqlUtils.isNull(TBL_CARGO_HANDLING, COL_CARGO),
              SqlUtils.isNull(TBL_CARGO_HANDLING, COL_CARGO_TRIP))));

      ImmutableMap.of(COL_LOADING_PLACE, TBL_CARGO_LOADING,
          COL_UNLOADING_PLACE, TBL_CARGO_UNLOADING).forEach((col, tbl) -> {

        SimpleRowSet rs = qs.getData(new SqlSelect()
            .addAllFields(TBL_CARGO_HANDLING)
            .addFrom(TBL_CARGO_HANDLING)
            .setWhere(SqlUtils.notNull(TBL_CARGO_HANDLING, col)));

        for (SimpleRow row : rs) {
          Long place = row.getLong(col);

          SqlUpdate update = new SqlUpdate(TBL_CARGO_PLACES)
              .setWhere(sys.idEquals(TBL_CARGO_PLACES, place));

          for (String s : new String[] {COL_EMPTY_KILOMETERS, COL_LOADED_KILOMETERS}) {
            if (!BeeUtils.isEmpty(row.getValue(s))) {
              update.addConstant(s, row.getValue(s));
            }
          }
          if (!BeeUtils.isEmpty(row.getValue(COL_CARGO_WEIGHT))) {
            update.addConstant(COL_ROUTE_WEIGHT, row.getValue(COL_CARGO_WEIGHT));
          }
          if (!update.isEmpty()) {
            qs.updateData(update);
          }
          SqlInsert insert = new SqlInsert(tbl)
              .addConstant(col, place);

          for (String s : new String[] {COL_CARGO, COL_CARGO_TRIP}) {
            if (DataUtils.isId(row.getLong(s))) {
              qs.insertData(insert.addConstant(s, row.getLong(s)));
              break;
            }
          }
        }
      });
      response.addInfo("OK");

    } else if (BeeUtils.same(cmd, "tables")) {
      sys.initTables();
      response.addInfo("Tables OK");

    } else if (BeeUtils.same(cmd, "views")) {
      sys.initViews();
      response.addInfo("Views OK");

    } else if (BeeUtils.same(cmd, "grids")) {
      ui.initGrids();
      response.addInfo("Grids OK");

    } else if (BeeUtils.same(cmd, "forms")) {
      ui.initForms();
      response.addInfo("Forms OK");

    } else if (BeeUtils.same(cmd, "menu")) {
      ui.initMenu();
      response.addInfo("Menu OK");

    } else if (BeeUtils.same(cmd, "reports")) {
      ui.initReports();
      response.addInfo("Reports OK");

    } else if (BeeUtils.same(cmd, "cacheinfo")) {
      response.addInfo(fs.getCacheStats());

    } else if (BeeUtils.startsSame(cmd, "logger")) {
      List<String> args = NameUtils.NAME_SPLITTER.splitToList(cmd);
      String name = BeeUtils.getQuietly(args, 1);
      BeeLogger currentLogger = BeeUtils.isEmpty(name)
          ? LogUtils.getLogger(QueryServiceBean.class) : LogUtils.getLogger(name);
      LogLevel level = EnumUtils.getEnumByName(LogLevel.class, BeeUtils.getQuietly(args, 2));

      if (Objects.nonNull(level)) {
        currentLogger.setLevel(level);
      }
      response.addInfo(currentLogger.getName(), currentLogger.getLevel());

    } else if (BeeUtils.same(cmd, "system")) {
      ib.init();
      response.addInfo("System OK");

    } else if (BeeUtils.startsSame(cmd, "check")) {
      String err = null;
      List<String> tbls = new ArrayList<>();
      int idx = -1;

      for (String w : NameUtils.NAME_SPLITTER.split(cmd)) {
        idx++;

        if (idx == 0) {
          continue;
        } else if (idx == 1 && BeeUtils.same(w, "all")) {
          break;
        } else if (!sys.isTable(w)) {
          err = BeeUtils.joinWords("Unknown table:", w);
          break;
        } else {
          tbls.add(w);
        }
      }
      if (BeeUtils.isEmpty(err)) {
        String progressId = tbls.isEmpty() ? reqInfo.getParameter(VAR_PROGRESS) : null;
        List<Property> resp = sys.checkTables(tbls, progressId);

        if (BeeUtils.isEmpty(resp)) {
          response.addWarning("No changes in table structure");
        } else {
          response.setResponse(resp);
        }
      } else {
        response.addError(err);
      }

    } else if (BeeUtils.same(cmd, "tecdoc")) {
      tcd.suckTecdoc();
      response = ResponseObject.info("TecDoc SUCKS NOW...");

    } else if (BeeUtils.same(cmd, "motonet")) {
      tcd.suckMotonet(true);
      response = ResponseObject.info("Motonet...");

    } else if (BeeUtils.same(cmd, "butent")) {
      tcd.suckButent(true);
      response = ResponseObject.info("Butent...");

    } else if (BeeUtils.startsSame(cmd, "cert")) {
      try {
        String[] args = BeeUtils.split(BeeUtils.removePrefix(cmd, "cert"), ' ');

        for (String msg : InstallCert.installCert(args)) {
          response.addInfo(msg);
        }
      } catch (Throwable e) {
        response.addError(e);
        logger.error(e);
      }

    } else if (BeeUtils.same(cmd, "date")) {
      response = rebuildDate();

    } else if (BeeUtils.same(cmd, "datetime")) {
      response = rebuildDateTime();

    } else if (!BeeUtils.isEmpty(cmd)) {
      String tbl = NameUtils.getWord(cmd, 0);
      if (sys.isTable(tbl)) {
        sys.rebuildTable(tbl);
        response.addInfo("Rebuild", tbl, "OK");
      } else {
        response.addError("Unknown table:", tbl);
      }

    } else {
      response.addError("Rebuild what?");
    }
    return response;
  }

  private ResponseObject rebuildDate() {
    ResponseObject response = new ResponseObject();
    List<Property> properties = new ArrayList<>();

    Multimap<String, String> fields = HashMultimap.create();

    sys.getTables().forEach(table -> table.getFields().stream()
        .filter(field -> field.getType() == SqlConstants.SqlDataType.DATE)
        .forEach(field -> fields.put(table.getName(), field.getName())));

    Long userId = usr.getCurrentUserId();

    List<String> tables = new ArrayList<>(fields.keySet());
    tables.sort(null);

    for (String table : tables) {
      for (String field : fields.get(table)) {
        Long[] values = qs.getLongColumn(new SqlSelect().setDistinctMode(true)
            .addFields(table, field)
            .addFrom(table)
            .setWhere(SqlUtils.notNull(table, field)));

        if (!ArrayUtils.isEmpty(values)) {
          int count = 0;

          for (long value : values) {
            if (value % TimeUtils.MILLIS_PER_DAY == 0
                && BeeUtils.isInt(value / TimeUtils.MILLIS_PER_DAY)) {

              int days = BeeUtils.toInt(value / TimeUtils.MILLIS_PER_DAY);
              long time = new JustDate(days).getTime();

              if (!Objects.equals(value, time)) {
                SqlUpdate update = new SqlUpdate(table)
                    .addConstant(field, time)
                    .setWhere(SqlUtils.equals(table, field, value));

                ResponseObject updateResponse = qs.updateDataWithResponse(update);

                if (updateResponse.hasErrors()) {
                  response.addMessagesFrom(updateResponse);
                  break;
                } else {
                  Integer result = updateResponse.getResponseAsInt();
                  if (BeeUtils.isPositive(result)) {
                    count += result;
                  }
                }
              }
            }
          }

          if (count > 0) {
            Endpoint.sendToUser(userId, LogMessage.debug(BeeUtils.joinWords(table, field, count)));
            properties.add(new Property(table + BeeConst.STRING_POINT + field, count));
          }
        }

        if (response.hasErrors()) {
          break;
        }
      }
      if (response.hasErrors()) {
        break;
      }
    }

    if (!properties.isEmpty()) {
      response.setCollection(properties, Property.class);
    } else if (!response.hasErrors()) {
      response.addInfo("dates ok:", fields.size(), "fields in", tables.size(), "tables");
    }

    return response;
  }

  private ResponseObject rebuildDateTime() {
    ResponseObject response = new ResponseObject();
    List<Property> properties = new ArrayList<>();

    Multimap<String, String> fields = HashMultimap.create();

    sys.getTables().forEach(table -> table.getFields().stream()
        .filter(field -> field.getType() == SqlConstants.SqlDataType.DATETIME)
        .forEach(field -> fields.put(table.getName(), field.getName())));

    Long userId = usr.getCurrentUserId();

    List<String> tables = new ArrayList<>(fields.keySet());
    tables.sort(null);

    for (String table : tables) {
      String idName = sys.getIdName(table);

      for (String field : fields.get(table)) {
        SqlSelect query = new SqlSelect()
            .addFields(table, idName, field)
            .addFrom(table)
            .setWhere(SqlUtils.negative(table, field));

        SimpleRowSet data = qs.getData(query);

        if (!DataUtils.isEmpty(data)) {
          for (SimpleRow row : data) {
            Long id = row.getLong(idName);
            Long time = row.getLong(field);

            Endpoint.sendToUser(userId,
                LogMessage.warning(BeeUtils.joinWords(table, id, field, time)));
            properties.add(new Property(BeeUtils.joinWords(table, id, field),
                BeeUtils.toStringOrNull(time)));
          }
        }
      }
    }

    if (properties.isEmpty()) {
      response.addInfo("datetimes ok:", fields.size(), "fields in", tables.size(), "tables");
    } else {
      response.setCollection(properties, Property.class);
    }

    return response;
  }

  private ResponseObject setRowRights(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    if (BeeUtils.isEmpty(viewName)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_VIEW_NAME);
    }

    if (!sys.isView(viewName)) {
      return ResponseObject.error(reqInfo.getService(), viewName, "not a view");
    }

    Long id = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_ID));
    if (!DataUtils.isId(id)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_ID);
    }

    Long role = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ROLE));
    if (!DataUtils.isId(role) && !Objects.equals(role, 0L)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_ROLE);
    }

    RightsState state = EnumUtils.getEnumByIndex(RightsState.class,
        reqInfo.getParameter(COL_STATE));
    if (state == null) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_STATE);
    }

    boolean value = Codec.unpack(reqInfo.getParameter(VAR_VALUE));

    BeeView view = sys.getView(viewName);
    String tblName = view.getSourceName();

    deb.setState(tblName, state, id, role, value);

    return ResponseObject.emptyResponse();
  }

  private ResponseObject switchDsn(String dsn) {
    if (!BeeUtils.isEmpty(dsn)) {
      ig.destroy();
      sys.initTables(dsn);
      ib.init();
      return ResponseObject.response(dsn);
    }
    return ResponseObject.error("DSN not specified");
  }

  private ResponseObject update(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    if (BeeUtils.isEmpty(viewName)) {
      return ResponseObject.parameterNotFound(VAR_VIEW_NAME);
    }

    String where = reqInfo.getParameter(VAR_VIEW_WHERE);
    if (BeeUtils.isEmpty(where)) {
      return ResponseObject.parameterNotFound(VAR_VIEW_WHERE);
    }

    String[] cols = Codec.beeDeserializeCollection(reqInfo.getParameter(VAR_COLUMN));
    if (ArrayUtils.isEmpty(cols)) {
      return ResponseObject.parameterNotFound(VAR_COLUMN);
    }

    String[] values = Codec.beeDeserializeCollection(reqInfo.getParameter(VAR_VALUE));
    if (ArrayUtils.isEmpty(values)) {
      return ResponseObject.parameterNotFound(VAR_VALUE);
    }
    List<String> newValues = Lists.newArrayList(values);

    if (cols.length != values.length) {
      return ResponseObject.error("Columns does not match values");
    }
    BeeRowSet rs = qs.getViewData(viewName, Filter.restore(where), null, Lists.newArrayList(cols));
    List<BeeColumn> columns = new ArrayList<>();

    for (String col : cols) {
      columns.add(rs.getColumn(col));
    }
    for (int i = 0; i < rs.getNumberOfRows(); i++) {
      List<String> oldValues = new ArrayList<>();

      for (String col : cols) {
        oldValues.add(rs.getString(i, col));
      }
      BeeRowSet childRs = DataUtils.getUpdated(viewName, rs.getRow(i).getId(),
          rs.getRow(i).getVersion(), columns, oldValues, newValues, null);

      if (childRs != null) {
        ResponseObject response = deb.commitRow(childRs, RowInfo.class);

        if (response.hasErrors()) {
          return response;
        }
      }
    }
    return ResponseObject.response(rs.getNumberOfRows());
  }

  private ResponseObject updateAutocomplete(RequestInfo reqInfo) {
    String key = BeeUtils.trim(reqInfo.getParameter(COL_AUTOCOMPLETE_KEY));
    if (BeeUtils.isEmpty(key)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_AUTOCOMPLETE_KEY);
    }

    String value = BeeUtils.trim(reqInfo.getParameter(COL_AUTOCOMPLETE_VALUE));
    if (BeeUtils.isEmpty(value)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_AUTOCOMPLETE_VALUE);
    }

    Long userId = usr.getCurrentUserId();
    if (userId == null) {
      return ResponseObject.warning(reqInfo.getService(), key, value, "user not available");
    }

    Long id = qs.getId(TBL_AUTOCOMPLETE, COL_AUTOCOMPLETE_USER, userId, COL_AUTOCOMPLETE_KEY, key,
        COL_AUTOCOMPLETE_VALUE, value);

    if (DataUtils.isId(id)) {
      SqlUpdate update = new SqlUpdate(TBL_AUTOCOMPLETE)
          .addConstant(COL_AUTOCOMPLETE_VALUE, value)
          .setWhere(sys.idEquals(TBL_AUTOCOMPLETE, id));

      return qs.updateDataWithResponse(update);

    } else {
      SqlInsert insert = new SqlInsert(TBL_AUTOCOMPLETE)
          .addConstant(COL_AUTOCOMPLETE_USER, userId)
          .addConstant(COL_AUTOCOMPLETE_KEY, key)
          .addConstant(COL_AUTOCOMPLETE_VALUE, value);

      return qs.insertDataWithResponse(insert);
    }
  }

  private ResponseObject updateCell(RequestInfo reqInfo) {
    BeeRowSet rs = BeeRowSet.restore(reqInfo.getContent());
    ResponseObject response = deb.commitRow(rs, RowInfo.class);

    if (!response.hasErrors()) {
      long rowId = rs.getRow(0).getId();
      BeeRowSet updated = qs.getViewData(rs.getViewName(), Filter.compareId(rowId), null,
          BeeConst.UNDEF, BeeConst.UNDEF, DataUtils.getColumnNames(rs.getColumns()));

      if (DataUtils.isEmpty(updated)) {
        response = ResponseObject.error("could not read updated row");
      } else {
        response = ResponseObject.response(updated.getRow(0));
      }
    }
    return response;
  }

  public ResponseObject updateRelatedValues(String viewName, Long parentId, String serialized) {
    if (BeeUtils.isEmpty(viewName)) {
      return ResponseObject.parameterNotFound(VAR_VIEW_NAME);
    }

    if (!DataUtils.isId(parentId)) {
      return ResponseObject.parameterNotFound(VAR_VIEW_ROW_ID);
    }

    if (BeeUtils.isEmpty(serialized)) {
      return ResponseObject.parameterNotFound(VAR_CHILDREN);
    }

    Collection<RowChildren> children = new ArrayList<>();

    String[] arr = Codec.beeDeserializeCollection(serialized);
    if (!ArrayUtils.isEmpty(arr)) {
      for (String s : arr) {
        children.add(RowChildren.restore(s));
      }
    }

    if (children.isEmpty()) {
      return ResponseObject.error("cannot restore children");
    }

    ResponseObject response = new ResponseObject();
    deb.commitChildren(parentId, children, response);

    if (!response.hasErrors()) {
      BeeRowSet rowSet = qs.getViewData(viewName, Filter.compareId(parentId));

      if (DataUtils.isEmpty(rowSet)) {
        response.addError("could not get parent row:", viewName, parentId);
      } else {
        response.setResponse(rowSet.getRow(0));
      }
    }

    return response;
  }

  private ResponseObject updateRow(RequestInfo reqInfo) {
    return deb.commitRow(BeeRowSet.restore(reqInfo.getContent()));
  }

  private ResponseObject updateRows(RequestInfo reqInfo) {
    BeeRowSet rowSet = BeeRowSet.restore(reqInfo.getContent());
    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.error(reqInfo.getService(), "row set is empty");
    }

    RowInfoList result = new RowInfoList(rowSet.getNumberOfRows());

    for (int i = 0; i < rowSet.getNumberOfRows(); i++) {
      ResponseObject response = deb.commitRow(rowSet, i, RowInfo.class);

      if (response.hasErrors()) {
        return response;

      } else if (response.hasResponse(RowInfo.class)) {
        result.add((RowInfo) response.getResponse());
      }
    }

    return ResponseObject.response(result);
  }
}
