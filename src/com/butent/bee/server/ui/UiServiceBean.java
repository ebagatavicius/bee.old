package com.butent.bee.server.ui;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.server.Config;
import com.butent.bee.server.DataSourceBean;
import com.butent.bee.server.InitializationBean;
import com.butent.bee.server.data.BeeTable;
import com.butent.bee.server.data.BeeTable.BeeField;
import com.butent.bee.server.data.BeeTable.BeeRelation;
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
import com.butent.bee.server.modules.ModuleHolderBean;
import com.butent.bee.server.modules.ec.TecDocBean;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.ui.XmlSqlDesigner.DataType;
import com.butent.bee.server.ui.XmlSqlDesigner.DataTypeGroup;
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Resource;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.XmlTable;
import com.butent.bee.shared.data.XmlTable.XmlField;
import com.butent.bee.shared.data.XmlTable.XmlRelation;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.rights.RightsObjectType;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.rights.RightsUtils;
import com.butent.bee.shared.time.DateTime;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
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
  GridLoaderBean grd;
  @EJB
  DataSourceBean dsb;
  @EJB
  ModuleHolderBean mod;
  @EJB
  MailModuleBean mail;
  @EJB
  SearchBean search;
  @EJB
  InitializationBean ib;
  @EJB
  TecDocBean tcd;
  @EJB
  NewsBean news;

  public ResponseObject doService(RequestInfo reqInfo) {
    ResponseObject response = null;

    String svc = reqInfo.getService();

    if (BeeUtils.same(svc, Service.GET_GRID)) {
      response = getGrid(reqInfo);
    } else if (BeeUtils.same(svc, Service.GET_FORM)) {
      response = getForm(reqInfo);

    } else if (BeeUtils.same(svc, Service.REBUILD)) {
      response = rebuildData(reqInfo);
    } else if (BeeUtils.same(svc, Service.DO_SQL)) {
      response = doSql(reqInfo);
    } else if (BeeUtils.same(svc, Service.QUERY)) {
      response = getViewData(reqInfo);
    } else if (BeeUtils.same(svc, Service.GET_VALUE)) {
      response = getValue(reqInfo);
    } else if (BeeUtils.same(svc, Service.GET_DATA)) {
      response = getData(reqInfo);

    } else if (BeeUtils.same(svc, Service.GET_DATA_INFO)) {
      response = getDataInfo(reqInfo);
    } else if (BeeUtils.same(svc, Service.GENERATE)) {
      response = generateData(reqInfo);
    } else if (BeeUtils.same(svc, Service.COUNT_ROWS)) {
      response = getViewSize(reqInfo);

    } else if (BeeUtils.same(svc, Service.DELETE_ROWS)) {
      response = deleteRows(reqInfo);
    } else if (BeeUtils.same(svc, Service.DELETE)) {
      response = delete(reqInfo);
    } else if (BeeUtils.same(svc, Service.UPDATE_CELL)) {
      response = updateCell(reqInfo);
    } else if (BeeUtils.same(svc, Service.UPDATE_ROW)) {
      response = updateRow(reqInfo);
    } else if (BeeUtils.same(svc, Service.UPDATE)) {
      response = update(reqInfo);
    } else if (BeeUtils.same(svc, Service.INSERT_ROW)) {
      response = insertRow(reqInfo);
    } else if (BeeUtils.same(svc, Service.INSERT_ROWS)) {
      response = insertRows(reqInfo);
    } else if (BeeUtils.same(svc, Service.INSERT_ROW_SILENTLY)) {
      response = insertRowSilently(reqInfo);

    } else if (BeeUtils.same(svc, Service.GET_VIEW_INFO)) {
      response = getViewInfo(reqInfo);
    } else if (BeeUtils.same(svc, Service.GET_TABLE_INFO)) {
      response = getTableInfo(reqInfo);

    } else if (BeeUtils.same(svc, Service.GET_DSNS)) {
      response = getDsns();
    } else if (BeeUtils.same(svc, Service.SWITCH_DSN)) {
      response = switchDsn(reqInfo.getParameter(Service.VAR_DSN));

    } else if (BeeUtils.same(svc, Service.SEARCH)) {
      response = search.processQuery(reqInfo.getParameter(0));
    } else if (BeeUtils.same(svc, Service.HISTOGRAM)) {
      response = getHistogram(reqInfo);

    } else if (BeeUtils.same(svc, Service.GET_RELATED_VALUES)) {
      response = getRelatedValues(reqInfo);
    } else if (BeeUtils.same(svc, Service.UPDATE_RELATED_VALUES)) {
      response = updateRelatedValues(reqInfo);

    } else if (BeeUtils.same(svc, Service.GET_DECORATORS)) {
      response = getDecorators();

    } else if (BeeUtils.same(svc, Service.GET_AUTOCOMPLETE)) {
      response = getAutocomplete();
    } else if (BeeUtils.same(svc, Service.UPDATE_AUTOCOMPLETE)) {
      response = updateAutocomplete(reqInfo);

    } else if (BeeUtils.same(svc, Service.GET_NEWS)) {
      response = news.getNews();
    } else if (BeeUtils.same(svc, Service.SUBSCRIBE_TO_FEEDS)) {
      response = news.subscribe(reqInfo);
    } else if (BeeUtils.same(svc, Service.ACCESS)) {
      response = news.onAccess(reqInfo);

    } else if (BeeUtils.same(svc, Service.GET_STATE_RIGHTS)) {
      response = usr.getStateRights(
          EnumUtils.getEnumByIndex(RightsObjectType.class, reqInfo.getParameter(COL_OBJECT_TYPE)),
          EnumUtils.getEnumByIndex(RightsState.class, reqInfo.getParameter(COL_STATE)));

    } else if (BeeUtils.same(svc, Service.GET_ROLE_RIGHTS)) {
      response = usr.getRoleRights(
          EnumUtils.getEnumByIndex(RightsObjectType.class, reqInfo.getParameter(COL_OBJECT_TYPE)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ROLE)));

    } else if (BeeUtils.same(svc, Service.SET_STATE_RIGHTS)) {
      response = usr.setStateRights(
          EnumUtils.getEnumByIndex(RightsObjectType.class, reqInfo.getParameter(COL_OBJECT_TYPE)),
          EnumUtils.getEnumByIndex(RightsState.class, reqInfo.getParameter(COL_STATE)),
          Codec.deserializeMap(reqInfo.getParameter(COL_OBJECT)));

    } else if (BeeUtils.same(svc, Service.SET_ROLE_RIGHTS)) {
      response = usr.setRoleRights(
          EnumUtils.getEnumByIndex(RightsObjectType.class, reqInfo.getParameter(COL_OBJECT_TYPE)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ROLE)),
          Codec.deserializeMap(reqInfo.getParameter(COL_OBJECT)));

    } else if (BeeUtils.same(svc, Service.SET_ROW_RIGHTS)) {
      response = setRowRights(reqInfo);

    } else if (BeeUtils.same(svc, Service.IMPORT_CSV_COMPANIES)) {
      response = importCSVCompanies(reqInfo);
    } else {
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

  public Pair<BeeRowSet, BeeRowSet> getGridAndColumnSettings() {
    Filter userFilter = usr.getCurrentUserFilter(GridDescription.COL_GRID_SETTING_USER);

    BeeRowSet gridSettings =
        DataUtils.emptyToNull(qs.getViewData(GridDescription.VIEW_GRID_SETTINGS, userFilter));
    BeeRowSet columnSettings =
        DataUtils.emptyToNull(qs.getViewData(ColumnDescription.VIEW_COLUMN_SETTINGS, userFilter));

    return Pair.of(gridSettings, columnSettings);
  }

  public BeeRowSet getReportSettings() {
    return qs.getViewData(VIEW_REPORT_SETTINGS, usr.getCurrentUserFilter(COL_RS_USER));
  }

  public BeeRowSet getWorkspaces() {
    return qs.getViewData(VIEW_WORKSPACES, usr.getCurrentUserFilter(COL_USER));
  }

  private void buildDbList(String rootTable, Set<String> tables, boolean initial) {
    boolean recurse = BeeUtils.isSuffix(rootTable, '*');
    String root = BeeUtils.normalize(BeeUtils.removeSuffix(rootTable, '*'));

    if (!initial && tables.contains(root) || !sys.isTable(root)) {
      return;
    }
    tables.add(root);

    for (String tbl : sys.getTableNames()) {
      if (!tables.contains(BeeUtils.normalize(tbl))) {
        for (BeeField field : sys.getTableFields(tbl)) {
          if (field instanceof BeeRelation
              && BeeUtils.same(((BeeRelation) field).getRelation(), root)) {
            if (recurse) {
              buildDbList(tbl + '*', tables, false);
            } else {
              tables.add(BeeUtils.normalize(tbl));
            }
          }
        }
      }
    }
  }

  private ResponseObject buildDbSchema(Iterable<String> roots) {
    XmlSqlDesigner designer = new XmlSqlDesigner();
    designer.types = new ArrayList<>();
    designer.tables = new ArrayList<>();

    for (int i = 0; i < 2; i++) {
      boolean extMode = i > 0;
      DataTypeGroup typeGroup = new DataTypeGroup();
      typeGroup.label = BeeUtils.joinWords("SQL", extMode ? "extended" : "", "types");
      typeGroup.color = extMode ? "rgb(0,255,0)" : "rgb(255,255,255)";
      typeGroup.types = new ArrayList<>();

      for (SqlDataType type : SqlDataType.values()) {
        String typeName = type.name();
        DataType dataType = new DataType();
        dataType.label = (extMode ? "Extended " : "") + typeName;
        dataType.sql = typeName + (extMode ? XmlSqlDesigner.EXT : "");
        typeGroup.types.add(dataType);
      }
      designer.types.add(typeGroup);
    }
    DataTypeGroup typeGroup = new DataTypeGroup();
    typeGroup.label = "Table states";
    typeGroup.color = "rgb(255,0,0)";

    DataType dataType = new DataType();
    dataType.label = "STATE";
    dataType.sql = XmlSqlDesigner.STATE;
    typeGroup.types = Lists.newArrayList(dataType);

    designer.types.add(typeGroup);

    Set<String> tables = new HashSet<>();
    Iterable<String> r;

    if (roots == null || !roots.iterator().hasNext()) {
      r = sys.getTableNames();
    } else {
      r = roots;
    }
    for (String root : r) {
      buildDbList(root, tables, true);
    }
    for (String tableName : tables) {
      XmlTable xmlTable = sys.getXmlTable(sys.getTable(tableName).getModule(), tableName);

      if (xmlTable != null) {
        Collection<XmlField> fields = new ArrayList<>();

        if (!BeeUtils.isEmpty(xmlTable.fields)) {
          fields.addAll(xmlTable.fields);
        }
        for (XmlField xmlField : fields) {
          if (xmlField instanceof XmlRelation) {
            XmlRelation xmlRelation = (XmlRelation) xmlField;
            xmlRelation.relationField = sys.getIdName(xmlRelation.relation);
          }
        }
        designer.tables.add(xmlTable);
      }
    }
    return ResponseObject.response(new Resource(null, XmlUtils.marshal(designer, null)));
  }

  private ResponseObject delete(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(Service.VAR_VIEW_NAME);
    Assert.notEmpty(viewName);
    String where = reqInfo.getParameter(Service.VAR_VIEW_WHERE);
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
    String viewName = reqInfo.getParameter(Service.VAR_VIEW_NAME);
    Assert.notEmpty(viewName);
    String[] entries = Codec.beeDeserializeCollection(reqInfo.getParameter(Service.VAR_VIEW_ROWS));
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
      resp.addWarning(usr.getLocalizableMesssages().rowsRetrieved(rc));
      return resp;
    } else if (res instanceof Number) {
      return ResponseObject.warning("Affected rows:", res);
    } else {
      return ResponseObject.error(res);
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
    String viewList = reqInfo.getParameter(Service.VAR_VIEW_LIST);
    if (BeeUtils.isEmpty(viewList)) {
      return ResponseObject.parameterNotFound(Service.VAR_VIEW_LIST);
    }

    List<String> viewNames = NameUtils.toList(viewList);
    List<BeeRowSet> result = new ArrayList<>();

    for (String viewName : viewNames) {
      BeeRowSet rs = qs.getViewData(viewName);
      result.add(rs);
    }
    return ResponseObject.response(result);
  }

  private ResponseObject getDataInfo(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(Service.VAR_VIEW_NAME);
    if (BeeUtils.isEmpty(viewName)) {
      return ResponseObject.response(sys.getDataInfo());
    } else {
      DataInfo dataInfo = sys.getDataInfo(viewName);
      return ResponseObject.response(dataInfo);
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
    String viewName = reqInfo.getParameter(Service.VAR_VIEW_NAME);
    String columns = reqInfo.getParameter(Service.VAR_VIEW_COLUMNS);

    String where = reqInfo.getParameter(Service.VAR_VIEW_WHERE);
    String order = reqInfo.getParameter(Service.VAR_VIEW_ORDER);

    Filter filter = BeeUtils.isEmpty(where) ? null : Filter.restore(where);

    SimpleRowSet res = qs.getHistogram(viewName, filter, NameUtils.toList(columns),
        NameUtils.toList(order));
    return ResponseObject.response(res);
  }

  private ResponseObject getRelatedValues(RequestInfo reqInfo) {
    String tableName = reqInfo.getParameter(Service.VAR_TABLE);
    if (BeeUtils.isEmpty(tableName)) {
      return ResponseObject.parameterNotFound(Service.VAR_TABLE);
    }

    String filterColumn = reqInfo.getParameter(Service.VAR_FILTER_COLUMN);
    if (BeeUtils.isEmpty(filterColumn)) {
      return ResponseObject.parameterNotFound(Service.VAR_FILTER_COLUMN);
    }

    Long filterValue = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_VALUE));
    if (!DataUtils.isId(filterValue)) {
      return ResponseObject.parameterNotFound(Service.VAR_VALUE);
    }

    String resultColumn = reqInfo.getParameter(Service.VAR_VALUE_COLUMN);
    if (BeeUtils.isEmpty(resultColumn)) {
      return ResponseObject.parameterNotFound(Service.VAR_VALUE_COLUMN);
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
    String viewName = reqInfo.getParameter(Service.VAR_VIEW_NAME);
    String rowId = reqInfo.getParameter(Service.VAR_VIEW_ROW_ID);
    String column = reqInfo.getParameter(Service.VAR_COLUMN);

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
    String viewName = reqInfo.getParameter(Service.VAR_VIEW_NAME);
    String columns = reqInfo.getParameter(Service.VAR_VIEW_COLUMNS);

    int limit = BeeUtils.toInt(reqInfo.getParameter(Service.VAR_VIEW_LIMIT));
    int offset = BeeUtils.toInt(reqInfo.getParameter(Service.VAR_VIEW_OFFSET));

    String where = reqInfo.getParameter(Service.VAR_VIEW_WHERE);
    String sort = reqInfo.getParameter(Service.VAR_VIEW_ORDER);

    String getSize = reqInfo.getParameter(Service.VAR_VIEW_SIZE);
    String rowId = reqInfo.getParameter(Service.VAR_VIEW_ROW_ID);

    String rights = reqInfo.getParameter(Service.VAR_RIGHTS);

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
      res.setTableProperty(Service.VAR_VIEW_SIZE,
          BeeUtils.toString(Math.max(cnt, res.getNumberOfRows())));
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
      for (String name : sys.getViewNames()) {
        PropertyUtils.appendWithPrefix(info, name, sys.getView(name).getExtendedInfo());
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

          row.setProperty(alias, Codec.pack(value));
        }
      }
    }
  }

  private ResponseObject getViewSize(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(Service.VAR_VIEW_NAME);
    String where = reqInfo.getParameter(Service.VAR_VIEW_WHERE);

    Filter filter = null;
    if (!BeeUtils.isEmpty(where)) {
      filter = Filter.restore(where);
    }
    return ResponseObject.response(qs.getViewSize(viewName, filter));
  }

  @Deprecated
  private ResponseObject importCSVCompanies(RequestInfo reqInfo) {
    String fileId = reqInfo.getContent();

    logger.info("File ID", fileId);

    String path = fileId;

    try {
      SqlInsert insert;

      FileInputStream fstream = new FileInputStream(path);

      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      boolean firstLine = true;
      while ((strLine = br.readLine()) != null) {
        if (firstLine) {
          firstLine = false;
          continue;
        }

        Splitter splitter = Splitter.on(';');
        List<String> lst = Lists.newArrayList(splitter.split(strLine));

        String[] data = ArrayUtils.toArray(lst);

        logger.info("Current data row", data);

        String companyName = BeeUtils.isEmpty(data[0]) ? null : data[0];
        String companyCode = BeeUtils.isEmpty(data[1]) ? null : data[1];
        String companyVatCode = BeeUtils.isEmpty(data[2]) ? null : data[2];
        String companyPostCode = BeeUtils.isEmpty(data[3]) ? null : data[3];
        String companyAddress = BeeUtils.isEmpty(data[4]) ? null : data[4];
        String companyCity = BeeUtils.isEmpty(data[5]) ? null : data[5];
        String companyCountry = BeeUtils.isEmpty(data[6]) ? "#" : data[6];
        String companyPhone = BeeUtils.isEmpty(data[7]) ? null : data[7];
        String companyEMail = BeeUtils.isEmpty(data[8]) ? null : data[8];

        if (BeeUtils.isEmpty(companyName)) {
          logger.warning("Record was skipped");
        }

        Filter filterCompany = Filter.contains(COL_COMPANY_NAME, companyName);

        if (!BeeUtils.isEmpty(companyCode)) {
          filterCompany = Filter.or(Filter.contains(COL_COMPANY_CODE, companyCode),
              Filter.contains(COL_COMPANY_NAME, companyName));
        }

        BeeRowSet listFilteredCompanies =
            qs.getViewData(ClassifierConstants.VIEW_COMPANIES, filterCompany);

        if (!listFilteredCompanies.isEmpty()) {
          logger.warning("Company ", companyName, companyCode, "allready exists !");
          continue;
        }

        insert = new SqlInsert(TBL_COMPANIES)
            .addFields(COL_COMPANY_CODE, COL_COMPANY_NAME, COL_COMPANY_VAT_CODE, "Notes")
            .addValues(companyCode, companyName, companyVatCode, "Imported "
                + (new DateTime().toTimeStamp()));

        logger.info("do sql", insert.getQuery());

        long companyId = qs.insertData(insert);

        insert = new SqlInsert(TBL_CONTACTS)
            .addFields(COL_ADDRESS, COL_POST_INDEX, COL_PHONE)
            .addValues(companyAddress, companyPostCode, companyPhone);

        logger.info("do sql", insert.getQuery());

        long contactId = qs.insertData(insert);

        SqlUpdate update = new SqlUpdate(TBL_COMPANIES)
            .addConstant(COL_CONTACT, contactId)
            .setWhere(SqlUtils.equals(TBL_COMPANIES,
                sys.getIdName(TBL_COMPANIES), companyId));

        logger.info("do sql", update.getQuery());

        qs.updateData(update);

        BeeRowSet filteredCities = new BeeRowSet();
        if (!BeeUtils.isEmpty(companyCity)) {
          Filter filterCity = Filter.contains("Name", companyCity);
          filteredCities = qs.getViewData(VIEW_CITIES, filterCity);
        }

        if (!filteredCities.isEmpty()) {

          long countryId =
              filteredCities.getRow(0).getLong(filteredCities.getColumnIndex(COL_COUNTRY));

          update = new SqlUpdate(TBL_CONTACTS)
              .addConstant(COL_CITY, filteredCities.getRowIds().get(0))
              .addConstant(COL_COUNTRY, countryId)
              .setWhere(SqlUtils.equals(TBL_CONTACTS, sys.getIdName(TBL_CONTACTS), contactId));

          logger.info("do sql", update.getQuery());
          qs.updateData(update);
        } else {

          Filter filterCountry = Filter.contains("Name", companyCountry);
          BeeRowSet filteredCounties = qs.getViewData(VIEW_COUNTRIES, filterCountry);

          if (!filteredCounties.isEmpty() && !BeeUtils.isEmpty(companyCity)) {
            insert = new SqlInsert(TBL_CITIES)
                .addFields("Name", COL_COUNTRY)
                .addValues(companyCity, filteredCounties.getRowIds().get(0));

            logger.info("do sql", insert.getQuery());
            long cityId = qs.insertData(insert);

            update = new SqlUpdate(TBL_CONTACTS)
                .addConstant(COL_CITY, cityId)
                .addConstant(
                    COL_COUNTRY, filteredCounties.getRowIds().get(0))
                .setWhere(SqlUtils.equals(TBL_CONTACTS, sys.getIdName(TBL_CONTACTS), contactId));

            logger.info("do sql", update.getQuery());
            qs.updateData(update);
          } else if (filteredCounties.isEmpty()) {

            insert = new SqlInsert(TBL_COUNTRIES).addConstant("Name", companyCountry);

            logger.info("do sql", insert.getQuery());
            long countryId = qs.insertData(insert);

            Long cityId = null;
            if (!BeeUtils.isEmpty(companyCity)) {
              insert = new SqlInsert(TBL_CITIES).addConstant("Name", companyCity)
                  .addConstant(COL_COUNTRY, countryId);

              logger.info("do sql", insert.getQuery());
              cityId = qs.insertData(insert);
            }

            update = new SqlUpdate(TBL_CONTACTS)
                .addConstant(COL_CITY, cityId)
                .addConstant(COL_COUNTRY, countryId)
                .setWhere(SqlUtils.equals(TBL_CONTACTS, sys.getIdName(TBL_CONTACTS), contactId));

            logger.info("do sql", update.getQuery());
            qs.updateData(update);
          }
        }

        if (!BeeUtils.isEmpty(companyEMail)) {
          Filter filterEMail = Filter.contains(COL_EMAIL, companyEMail);
          BeeRowSet filteredEMails = qs.getViewData("Emails", filterEMail);

          if (filteredEMails.isEmpty()) {
            insert = new SqlInsert(TBL_EMAILS)
                .addConstant(COL_EMAIL, companyEMail)
                .addConstant("Label", companyName);

            logger.info("do sql", insert.getQuery());
            long emailId = qs.insertData(insert);

            update = new SqlUpdate(TBL_CONTACTS)
                .addConstant(COL_EMAIL, emailId)
                .setWhere(SqlUtils.equals(TBL_CONTACTS, sys.getIdName(TBL_CONTACTS), contactId));

            logger.info("do sql", update.getQuery());
            qs.updateData(update);

          }

        }

      }
      in.close();
    } catch (Exception e) {
      logger.severe(e);
      return ResponseObject.error(e);
    }

    return ResponseObject.info("Data imported");
  }

  private ResponseObject insertRow(RequestInfo reqInfo) {
    return deb.commitRow(BeeRowSet.restore(reqInfo.getContent()));
  }

  private ResponseObject insertRows(RequestInfo reqInfo) {
    BeeRowSet rowSet = BeeRowSet.restore(reqInfo.getContent());
    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.error(reqInfo.getService(), "row set is empty");
    }

    int count = 0;

    for (int i = 0; i < rowSet.getNumberOfRows(); i++) {
      ResponseObject response = deb.commitRow(rowSet, i, RowInfo.class);
      if (response.hasErrors()) {
        return response;
      }

      count++;
    }

    return ResponseObject.response(count);
  }

  private ResponseObject insertRowSilently(RequestInfo reqInfo) {
    return deb.commitRow(BeeRowSet.restore(reqInfo.getContent()), RowInfo.class);
  }

  private ResponseObject rebuildData(RequestInfo reqInfo) {
    ResponseObject response = new ResponseObject();

    String cmd = reqInfo.getContent();

    if (BeeUtils.same(cmd, "all")) {
      sys.rebuildActiveTables();
      response.addInfo("Recreate structure OK");

    } else if (BeeUtils.same(cmd, "tables")) {
      sys.initTables();
      response.addInfo("Tables OK");

    } else if (BeeUtils.same(cmd, "views")) {
      sys.initViews();
      response.addInfo("Views OK");

    } else if (BeeUtils.same(cmd, "grids")) {
      ui.initGrids();
      response.addInfo("Grids OK");

    } else if (BeeUtils.same(cmd, "menu")) {
      ui.initMenu();
      response.addInfo("Menu OK");

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
        String progressId = tbls.isEmpty() ? reqInfo.getParameter(Service.VAR_PROGRESS) : null;
        List<Property> resp = sys.checkTables(tbls, progressId);

        if (BeeUtils.isEmpty(resp)) {
          response.addWarning("No changes in table structure");
        } else {
          response.setResponse(resp);
        }
      } else {
        response.addError(err);
      }
    } else if (BeeUtils.startsSame(cmd, "schema")) {
      String schema = cmd.substring("schema".length()).trim();

      response = buildDbSchema(Splitter.onPattern("[ ,]").trimResults().omitEmptyStrings()
          .split(schema));

    } else if (BeeUtils.same(cmd, "tecdoc")) {
      tcd.suckTecdoc();
      response = ResponseObject.info("TecDoc SUCKS NOW...");

    } else if (BeeUtils.same(cmd, "motonet")) {
      tcd.suckMotonet();
      response = ResponseObject.info("Motonet...");

    } else if (BeeUtils.same(cmd, "butent")) {
      tcd.suckButent();
      response = ResponseObject.info("Butent...");

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

  private ResponseObject setRowRights(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(Service.VAR_VIEW_NAME);
    if (BeeUtils.isEmpty(viewName)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_VIEW_NAME);
    }

    if (!sys.isView(viewName)) {
      return ResponseObject.error(reqInfo.getService(), viewName, "not a view");
    }

    Long id = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_ID));
    if (!DataUtils.isId(id)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_ID);
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

    boolean value = Codec.unpack(reqInfo.getParameter(Service.VAR_VALUE));

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
    String viewName = reqInfo.getParameter(Service.VAR_VIEW_NAME);
    if (BeeUtils.isEmpty(viewName)) {
      return ResponseObject.parameterNotFound(Service.VAR_VIEW_NAME);
    }

    String where = reqInfo.getParameter(Service.VAR_VIEW_WHERE);
    if (BeeUtils.isEmpty(where)) {
      return ResponseObject.parameterNotFound(Service.VAR_VIEW_WHERE);
    }

    String[] cols = Codec.beeDeserializeCollection(reqInfo.getParameter(Service.VAR_COLUMN));
    if (ArrayUtils.isEmpty(cols)) {
      return ResponseObject.parameterNotFound(Service.VAR_COLUMN);
    }

    String[] values = Codec.beeDeserializeCollection(reqInfo.getParameter(Service.VAR_VALUE));
    if (ArrayUtils.isEmpty(values)) {
      return ResponseObject.parameterNotFound(Service.VAR_VALUE);
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

  private ResponseObject updateRelatedValues(RequestInfo reqInfo) {
    String viewName = reqInfo.getParameter(Service.VAR_VIEW_NAME);
    if (BeeUtils.isEmpty(viewName)) {
      return ResponseObject.parameterNotFound(Service.VAR_VIEW_NAME);
    }

    Long parentId = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_VIEW_ROW_ID));
    if (!DataUtils.isId(parentId)) {
      return ResponseObject.parameterNotFound(Service.VAR_VIEW_ROW_ID);
    }

    String serialized = reqInfo.getParameter(Service.VAR_CHILDREN);
    if (BeeUtils.isEmpty(serialized)) {
      return ResponseObject.parameterNotFound(Service.VAR_CHILDREN);
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
}
