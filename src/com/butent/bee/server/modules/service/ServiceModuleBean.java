package com.butent.bee.server.modules.service;

import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.documents.DocumentConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.administration.ExchangeUtils;
import com.butent.bee.server.modules.administration.ExtensionIcons;
import com.butent.bee.server.modules.trade.TradeModuleBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class ServiceModuleBean implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(ServiceModuleBean.class);

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;

  @Override
  public List<SearchResult> doSearch(String query) {
    return null;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response;

    if (BeeUtils.same(svc, SVC_CREATE_INVOICE_ITEMS)) {
      response = createInvoiceItems(reqInfo);
    } else if (BeeUtils.same(svc, SVC_CREATE_DEFECT_ITEMS)) {
      response = createDefectItems(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_CALENDAR_DATA)) {
      response = getCalendarData(reqInfo);

    } else if (BeeUtils.same(svc, SVC_COPY_DOCUMENT_CRITERIA)) {
      response = copyDocumentCriteria(reqInfo);

    } else {
      String msg = BeeUtils.joinWords("service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }

    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    return null;
  }

  @Override
  public Module getModule() {
    return Module.SERVICE;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public void init() {
    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      public void setRowProperties(ViewQueryEvent event) {
        if (event.isBefore()) {
          return;
        }

        if (BeeUtils.same(event.getTargetName(), VIEW_SERVICE_FILES)) {
          ExtensionIcons.setIcons(event.getRowset(), ALS_FILE_NAME, PROP_ICON);

        } else if (BeeUtils.same(event.getTargetName(), VIEW_SERVICE_OBJECTS)
            && !DataUtils.isEmpty(event.getRowset())) {

          BeeRowSet rowSet = event.getRowset();
          List<Long> rowIds = rowSet.getRowIds();

          BeeView view = sys.getView(VIEW_SERVICE_OBJECT_CRITERIA);
          SqlSelect query = view.getQuery();

          query.setWhere(SqlUtils.and(query.getWhere(),
              SqlUtils.isNull(view.getSourceAlias(), COL_SERVICE_CRITERIA_GROUP_NAME),
              SqlUtils.inList(view.getSourceAlias(), COL_SERVICE_OBJECT, rowIds)));

          SimpleRowSet criteria = qs.getData(query);

          if (!DataUtils.isEmpty(criteria)) {
            for (SimpleRow row : criteria) {
              BeeRow r = rowSet.getRowById(row.getLong(COL_SERVICE_OBJECT));

              if (r != null) {
                r.setProperty(COL_SERVICE_CRITERION_NAME
                    + row.getValue(COL_SERVICE_CRITERION_NAME),
                    row.getValue(COL_SERVICE_CRITERION_VALUE));
              }
            }
          }
        }
      }
    });
  }

  private ResponseObject copyDocumentCriteria(RequestInfo reqInfo) {
    Long dataId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_DOCUMENT_DATA));
    if (!DataUtils.isId(dataId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_DOCUMENT_DATA);
    }

    Long objId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_SERVICE_OBJECT));
    if (!DataUtils.isId(objId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_SERVICE_OBJECT);
    }

    if (qs.sqlExists(TBL_SERVICE_CRITERIA_GROUPS, COL_SERVICE_OBJECT, objId)) {
      return ResponseObject.emptyResponse();
    }

    String aliasGroupOrdinal = COL_CRITERIA_GROUP + COL_CRITERIA_ORDINAL;

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addField(TBL_CRITERIA_GROUPS, COL_CRITERIA_ORDINAL, aliasGroupOrdinal)
        .addFields(TBL_CRITERIA_GROUPS, COL_CRITERIA_GROUP_NAME)
        .addFields(TBL_CRITERIA, COL_CRITERIA_GROUP, COL_CRITERIA_ORDINAL, COL_CRITERION_NAME,
            COL_CRITERION_VALUE)
        .addFrom(TBL_CRITERIA_GROUPS)
        .addFromLeft(TBL_CRITERIA,
            sys.joinTables(TBL_CRITERIA_GROUPS, TBL_CRITERIA, COL_CRITERIA_GROUP))
        .setWhere(SqlUtils.equals(TBL_CRITERIA_GROUPS, COL_DOCUMENT_DATA, dataId)));

    if (DataUtils.isEmpty(rs)) {
      return ResponseObject.emptyResponse();
    }

    Map<Long, Long> groups = new HashMap<>();
    Long svcGroupId;

    for (SimpleRow row : rs) {
      Long docGroupId = row.getLong(COL_CRITERIA_GROUP);

      if (groups.containsKey(docGroupId)) {
        svcGroupId = groups.get(docGroupId);
        
      } else {
        SqlInsert insGroup = new SqlInsert(TBL_SERVICE_CRITERIA_GROUPS)
            .addConstant(COL_SERVICE_OBJECT, objId);
        
        Integer groupOrdinal = row.getInt(aliasGroupOrdinal);
        if (groupOrdinal != null) {
          insGroup.addConstant(COL_SERVICE_CRITERIA_ORDINAL, groupOrdinal);
        }
        
        String groupName = row.getValue(COL_CRITERIA_GROUP_NAME);
        if (!BeeUtils.isEmpty(groupName)) {
          insGroup.addConstant(COL_SERVICE_CRITERIA_GROUP_NAME, groupName);
        }
        
        svcGroupId = qs.insertData(insGroup);
        groups.put(docGroupId, svcGroupId);
      }

      String criterion = row.getValue(COL_CRITERION_NAME);

      if (DataUtils.isId(svcGroupId) && !BeeUtils.isEmpty(criterion)) {
        SqlInsert insCrit = new SqlInsert(TBL_SERVICE_CRITERIA)
        .addConstant(COL_SERVICE_CRITERIA_GROUP, svcGroupId)
        .addConstant(COL_SERVICE_CRITERION_NAME, criterion);

        Integer ordinal = row.getInt(COL_CRITERIA_ORDINAL);
        if (ordinal != null) {
          insCrit.addConstant(COL_SERVICE_CRITERIA_ORDINAL, ordinal);
        }
        
        String value = row.getValue(COL_CRITERION_VALUE);
        if (!BeeUtils.isEmpty(value)) {
          insCrit.addConstant(COL_SERVICE_CRITERION_VALUE, value);
        }
        
        qs.insertData(insCrit);
      }
    }

    return ResponseObject.response(rs.getNumberOfRows());
  }

  private ResponseObject createDefectItems(RequestInfo reqInfo) {
    Long dfId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_DEFECT));
    if (!DataUtils.isId(dfId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_DEFECT);
    }

    Long currency = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_CURRENCY));
    if (!DataUtils.isId(currency)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_CURRENCY);
    }

    Set<Long> ids = DataUtils.parseIdSet(reqInfo.getParameter(VIEW_MAINTENANCE));
    if (ids.isEmpty()) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VIEW_MAINTENANCE);
    }

    IsCondition where = sys.idInList(TBL_MAINTENANCE, ids);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_MAINTENANCE, COL_MAINTENANCE_ITEM, COL_TRADE_ITEM_QUANTITY,
            COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC, COL_MAINTENANCE_NOTES)
        .addFrom(TBL_MAINTENANCE)
        .setWhere(where);

    IsExpression priceExch = ExchangeUtils.exchangeFieldTo(query,
        TBL_MAINTENANCE, COL_TRADE_ITEM_PRICE, COL_CURRENCY, COL_MAINTENANCE_DATE, currency);
    String priceAlias = "Price_" + SqlUtils.uniqueName();

    IsExpression vatExch = ExchangeUtils.exchangeFieldTo(query,
        TBL_MAINTENANCE, COL_TRADE_VAT, COL_CURRENCY, COL_MAINTENANCE_DATE, currency);
    String vatAlias = "Vat_" + SqlUtils.uniqueName();

    query.addExpr(priceExch, priceAlias)
        .addExpr(vatExch, vatAlias)
        .addOrder(TBL_MAINTENANCE, sys.getIdName(TBL_MAINTENANCE));

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.error(TBL_MAINTENANCE, ids, "not found");
    }

    ResponseObject response = new ResponseObject();

    for (SimpleRow row : data) {
      Long item = row.getLong(COL_MAINTENANCE_ITEM);

      SqlInsert insert = new SqlInsert(TBL_SERVICE_DEFECT_ITEMS)
          .addConstant(COL_DEFECT, dfId)
          .addConstant(COL_DEFECT_ITEM, item);

      Boolean vatPerc = row.getBoolean(COL_TRADE_VAT_PERC);
      Double vat;

      if (BeeUtils.isTrue(vatPerc)) {
        insert.addConstant(COL_TRADE_VAT_PERC, vatPerc);
        vat = row.getDouble(COL_TRADE_VAT);
      } else {
        vat = row.getDouble(vatAlias);
      }

      if (BeeUtils.nonZero(vat)) {
        insert.addConstant(COL_TRADE_VAT, vat);
      }

      Boolean vatPlus = row.getBoolean(COL_TRADE_VAT_PLUS);
      if (BeeUtils.isTrue(vatPlus)) {
        insert.addConstant(COL_TRADE_VAT_PLUS, vatPlus);
      }

      Double quantity = row.getDouble(COL_TRADE_ITEM_QUANTITY);
      Double price = row.getDouble(priceAlias);

      insert.addConstant(COL_TRADE_ITEM_QUANTITY, BeeUtils.unbox(quantity));
      if (price != null) {
        insert.addConstant(COL_TRADE_ITEM_PRICE, price);
      }

      if (data.hasColumn(COL_MAINTENANCE_NOTES)) {
        String notes = row.getValue(COL_MAINTENANCE_NOTES);
        if (!BeeUtils.isEmpty(notes)) {
          insert.addConstant(COL_DEFECT_NOTE, notes);
        }
      }

      ResponseObject insResponse = qs.insertDataWithResponse(insert);
      if (insResponse.hasErrors()) {
        response.addMessagesFrom(insResponse);
        break;
      }
    }

    if (!response.hasErrors()) {
      SqlUpdate update = new SqlUpdate(TBL_MAINTENANCE)
          .addConstant(COL_MAINTENANCE_DEFECT, dfId)
          .setWhere(where);

      ResponseObject updResponse = qs.updateDataWithResponse(update);
      if (updResponse.hasErrors()) {
        response.addMessagesFrom(updResponse);
      }
    }

    return response;
  }

  private ResponseObject createInvoiceItems(RequestInfo reqInfo) {
    Long invId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_MAINTENANCE_INVOICE));
    if (!DataUtils.isId(invId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_MAINTENANCE_INVOICE);
    }

    Long currency =
        BeeUtils.toLongOrNull(reqInfo.getParameter(COL_CURRENCY));
    if (!DataUtils.isId(currency)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_CURRENCY);
    }

    Set<Long> ids = DataUtils.parseIdSet(reqInfo.getParameter(VIEW_MAINTENANCE));
    if (ids.isEmpty()) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VIEW_MAINTENANCE);
    }

    Long mainItem = BeeUtils.toLongOrNull(reqInfo.getParameter(PROP_MAIN_ITEM));

    IsCondition where = sys.idInList(TBL_MAINTENANCE, ids);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_MAINTENANCE, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)
        .addFrom(TBL_MAINTENANCE)
        .setWhere(where);

    IsExpression vatExch = ExchangeUtils.exchangeFieldTo(query,
        TBL_MAINTENANCE, COL_TRADE_VAT, COL_CURRENCY, COL_MAINTENANCE_DATE, currency);

    String vatAlias = "Vat_" + SqlUtils.uniqueName();

    String priceAlias;
    String amountAlias;

    if (DataUtils.isId(mainItem) && ids.size() > 1) {
      IsExpression amountExch = ExchangeUtils.exchangeFieldTo(query,
          TradeModuleBean.getTotalExpression(TBL_MAINTENANCE),
          SqlUtils.field(TBL_MAINTENANCE, COL_CURRENCY),
          SqlUtils.field(TBL_MAINTENANCE, COL_MAINTENANCE_DATE),
          SqlUtils.constant(currency));

      priceAlias = null;
      amountAlias = "Amount_" + SqlUtils.uniqueName();

      query.addSum(TBL_MAINTENANCE, COL_TRADE_ITEM_QUANTITY)
          .addSum(amountExch, amountAlias)
          .addSum(vatExch, vatAlias)
          .addGroup(TBL_MAINTENANCE, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC);

    } else {
      IsExpression priceExch = ExchangeUtils.exchangeFieldTo(query,
          TBL_MAINTENANCE, COL_TRADE_ITEM_PRICE, COL_CURRENCY, COL_MAINTENANCE_DATE, currency);

      priceAlias = "Price_" + SqlUtils.uniqueName();
      amountAlias = null;

      query.addFields(TBL_MAINTENANCE, COL_MAINTENANCE_ITEM, COL_TRADE_ITEM_QUANTITY,
          COL_MAINTENANCE_NOTES)
          .addExpr(priceExch, priceAlias)
          .addExpr(vatExch, vatAlias)
          .addOrder(TBL_MAINTENANCE, sys.getIdName(TBL_MAINTENANCE));
    }

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.error(TBL_MAINTENANCE, ids, "not found");
    }

    ResponseObject response = new ResponseObject();

    for (SimpleRow row : data) {
      Long item = DataUtils.isId(mainItem) ? mainItem : row.getLong(COL_MAINTENANCE_ITEM);

      SqlInsert insert = new SqlInsert(TBL_SALE_ITEMS)
          .addConstant(COL_SALE, invId)
          .addConstant(COL_ITEM, item);

      Boolean vatPerc = row.getBoolean(COL_TRADE_VAT_PERC);
      Double vat;

      if (BeeUtils.isTrue(vatPerc)) {
        insert.addConstant(COL_TRADE_VAT_PERC, vatPerc);
        vat = row.getDouble(COL_TRADE_VAT);
      } else {
        vat = row.getDouble(vatAlias);
      }

      if (BeeUtils.nonZero(vat)) {
        insert.addConstant(COL_TRADE_VAT, vat);
      }

      Boolean vatPlus = row.getBoolean(COL_TRADE_VAT_PLUS);
      if (BeeUtils.isTrue(vatPlus)) {
        insert.addConstant(COL_TRADE_VAT_PLUS, vatPlus);
      }

      Double quantity = row.getDouble(COL_TRADE_ITEM_QUANTITY);
      Double price;

      if (BeeUtils.isEmpty(amountAlias)) {
        price = row.getDouble(priceAlias);

      } else {
        Double amount = row.getDouble(amountAlias);
        if (BeeUtils.isTrue(vatPlus) && BeeUtils.nonZero(vat)) {
          if (BeeUtils.isTrue(vatPerc)) {
            if (BeeUtils.nonZero(amount)) {
              amount = amount * 100d / (100d + vat);
            }
          } else {
            amount = BeeUtils.unbox(amount) - vat;
          }
        }

        if (BeeUtils.isPositive(amount) && BeeUtils.isPositive(quantity)) {
          price = amount / quantity;
        } else {
          quantity = BeeConst.DOUBLE_ONE;
          price = amount;
        }
      }

      insert.addConstant(COL_TRADE_ITEM_QUANTITY, BeeUtils.unbox(quantity));
      if (price != null) {
        insert.addConstant(COL_TRADE_ITEM_PRICE, price);
      }

      if (data.hasColumn(COL_MAINTENANCE_NOTES)) {
        String notes = row.getValue(COL_MAINTENANCE_NOTES);
        if (!BeeUtils.isEmpty(notes)) {
          insert.addConstant(COL_TRADE_ITEM_NOTE, notes);
        }
      }

      ResponseObject insResponse = qs.insertDataWithResponse(insert);
      if (insResponse.hasErrors()) {
        response.addMessagesFrom(insResponse);
        break;
      }
    }

    if (!response.hasErrors()) {
      SqlUpdate update = new SqlUpdate(TBL_MAINTENANCE)
          .addConstant(COL_MAINTENANCE_INVOICE, invId)
          .setWhere(where);

      ResponseObject updResponse = qs.updateDataWithResponse(update);
      if (updResponse.hasErrors()) {
        response.addMessagesFrom(updResponse);
      }
    }

    return response;
  }

  private ResponseObject getCalendarData(RequestInfo reqInfo) {
    BeeRowSet settings = getSettings();
    if (DataUtils.isEmpty(settings)) {
      return ResponseObject.error(reqInfo.getService(), "user settings not available");
    }

    SimpleRowSet objectData = getCalendarObjects();
    if (DataUtils.isEmpty(objectData)) {
      return ResponseObject.response(settings);
    }

    settings.setTableProperty(TBL_SERVICE_OBJECTS, objectData.serialize());

    SimpleRowSet datesData = getCalendarDates();
    if (!DataUtils.isEmpty(datesData)) {
      settings.setTableProperty(TBL_SERVICE_DATES, datesData.serialize());
    }

    SimpleRowSet relationData = getCalendarRelations();
    if (DataUtils.isEmpty(relationData)) {
      return ResponseObject.response(settings);
    }

    settings.setTableProperty(TBL_RELATIONS, relationData.serialize());

    Set<Long> taskTypes = DataUtils.parseIdSet(
        settings.getString(0, COL_SERVICE_CALENDAR_TASK_TYPES));

    SimpleRowSet taskData = getCalendarTasks(taskTypes);
    if (!DataUtils.isEmpty(taskData)) {
      settings.setTableProperty(TBL_TASKS, taskData.serialize());
    }

    SimpleRowSet rtData = getCalendarRecurringTasks(taskTypes);
    if (!DataUtils.isEmpty(rtData)) {
      settings.setTableProperty(TBL_RECURRING_TASKS, rtData.serialize());

      BeeRowSet rtDates = qs.getViewData(VIEW_RT_DATES);
      if (!DataUtils.isEmpty(rtDates)) {
        settings.setTableProperty(VIEW_RT_DATES, rtDates.serialize());
      }
    }

    return ResponseObject.response(settings);
  }

  private SimpleRowSet getCalendarDates() {
    SqlSelect query = new SqlSelect()
        .addAllFields(TBL_SERVICE_DATES)
        .addFrom(TBL_SERVICE_DATES)
        .addOrder(TBL_SERVICE_DATES, COL_SERVICE_OBJECT, COL_SERVICE_DATE_FROM);

    return qs.getData(query);
  }

  private SimpleRowSet getCalendarObjects() {
    String idName = sys.getIdName(TBL_SERVICE_OBJECTS);

    HasConditions where = SqlUtils.or(
        SqlUtils.in(TBL_SERVICE_OBJECTS, idName, TBL_RELATIONS, COL_SERVICE_OBJECT,
            SqlUtils.or(SqlUtils.notNull(TBL_RELATIONS, COL_TASK),
                SqlUtils.notNull(TBL_RELATIONS, COL_RECURRING_TASK))),
        SqlUtils.in(TBL_SERVICE_OBJECTS, idName, TBL_SERVICE_DATES, COL_SERVICE_OBJECT));

    String aliasCustomers = "Cust_" + SqlUtils.uniqueName();
    String aliasContractors = "Contr_" + SqlUtils.uniqueName();

    String companyIdName = sys.getIdName(TBL_COMPANIES);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_SERVICE_OBJECTS, idName, COL_SERVICE_CATEGORY,
            COL_SERVICE_CUSTOMER, COL_SERVICE_CONTRACTOR, COL_SERVICE_ADDRESS)
        .addField(TBL_SERVICE_TREE, COL_SERVICE_CATEGORY_NAME, ALS_SERVICE_CATEGORY_NAME)
        .addField(aliasCustomers, COL_COMPANY_NAME, ALS_SERVICE_CUSTOMER_NAME)
        .addField(aliasContractors, COL_COMPANY_NAME, ALS_SERVICE_CONTRACTOR_NAME)
        .addFrom(TBL_SERVICE_OBJECTS)
        .addFromLeft(TBL_SERVICE_TREE, sys.joinTables(TBL_SERVICE_TREE,
            TBL_SERVICE_OBJECTS, COL_SERVICE_CATEGORY))
        .addFromLeft(TBL_COMPANIES, aliasCustomers,
            SqlUtils.join(aliasCustomers, companyIdName,
                TBL_SERVICE_OBJECTS, COL_SERVICE_CUSTOMER))
        .addFromLeft(TBL_COMPANIES, aliasContractors,
            SqlUtils.join(aliasContractors, companyIdName,
                TBL_SERVICE_OBJECTS, COL_SERVICE_CONTRACTOR))
        .setWhere(where)
        .addOrder(TBL_SERVICE_OBJECTS, COL_SERVICE_ADDRESS, idName);

    return qs.getData(query);
  }

  private SimpleRowSet getCalendarRecurringTasks(Set<Long> taskTypes) {
    String idName = sys.getIdName(TBL_RECURRING_TASKS);

    SqlSelect spawnQuery = new SqlSelect()
        .addFields(TBL_TASKS, COL_RECURRING_TASK)
        .addMax(TBL_TASKS, COL_START_TIME, ALS_LAST_SPAWN)
        .addFrom(TBL_TASKS)
        .setWhere(SqlUtils.notNull(TBL_TASKS, COL_RECURRING_TASK))
        .addGroup(TBL_TASKS, COL_RECURRING_TASK);

    String spawnAlias = "Spawn_" + SqlUtils.uniqueName();

    HasConditions where = SqlUtils.and(SqlUtils.in(TBL_RECURRING_TASKS, idName,
        TBL_RELATIONS, COL_RECURRING_TASK,
        SqlUtils.notNull(TBL_RELATIONS, COL_SERVICE_OBJECT)));
    if (!taskTypes.isEmpty()) {
      where.add(SqlUtils.inList(TBL_RECURRING_TASKS, COL_TASK_TYPE, taskTypes));
    }

    SqlSelect query = new SqlSelect()
        .addAllFields(TBL_RECURRING_TASKS)
        .addField(TBL_TASK_TYPES, COL_TASK_TYPE_NAME, ALS_TASK_TYPE_NAME)
        .addField(TBL_TASK_TYPES, COL_BACKGROUND, ALS_TASK_TYPE_BACKGROUND)
        .addField(TBL_TASK_TYPES, COL_FOREGROUND, ALS_TASK_TYPE_FOREGROUND)
        .addFields(spawnAlias, ALS_LAST_SPAWN)
        .addFrom(TBL_RECURRING_TASKS)
        .addFromLeft(TBL_TASK_TYPES,
            sys.joinTables(TBL_TASK_TYPES, TBL_RECURRING_TASKS, COL_TASK_TYPE))
        .addFromLeft(spawnQuery, spawnAlias,
            SqlUtils.join(spawnAlias, COL_RECURRING_TASK, TBL_RECURRING_TASKS, idName))
        .setWhere(where)
        .addOrder(TBL_RECURRING_TASKS, COL_RT_SCHEDULE_FROM, idName);

    return qs.getData(query);
  }

  private SimpleRowSet getCalendarRelations() {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_RELATIONS, COL_SERVICE_OBJECT, COL_TASK, COL_RECURRING_TASK)
        .addFrom(TBL_RELATIONS)
        .setWhere(SqlUtils.and(
            SqlUtils.notNull(TBL_RELATIONS, COL_SERVICE_OBJECT),
            SqlUtils.or(
                SqlUtils.notNull(TBL_RELATIONS, COL_TASK),
                SqlUtils.notNull(TBL_RELATIONS, COL_RECURRING_TASK))));

    return qs.getData(query);
  }

  private SimpleRowSet getCalendarTasks(Set<Long> taskTypes) {
    String idName = sys.getIdName(TBL_TASKS);

    HasConditions where = SqlUtils.and(SqlUtils.in(TBL_TASKS, idName, TBL_RELATIONS, COL_TASK,
        SqlUtils.notNull(TBL_RELATIONS, COL_SERVICE_OBJECT)));
    if (!taskTypes.isEmpty()) {
      where.add(SqlUtils.inList(TBL_TASKS, COL_TASK_TYPE, taskTypes));
    }

    SqlSelect query = new SqlSelect()
        .addAllFields(TBL_TASKS)
        .addField(TBL_TASK_TYPES, COL_TASK_TYPE_NAME, ALS_TASK_TYPE_NAME)
        .addField(TBL_TASK_TYPES, COL_BACKGROUND, ALS_TASK_TYPE_BACKGROUND)
        .addField(TBL_TASK_TYPES, COL_FOREGROUND, ALS_TASK_TYPE_FOREGROUND)
        .addFields(TBL_TASK_USERS, COL_STAR)
        .addFrom(TBL_TASKS)
        .addFromLeft(TBL_TASK_TYPES,
            sys.joinTables(TBL_TASK_TYPES, TBL_TASKS, COL_TASK_TYPE))
        .addFromLeft(TBL_TASK_USERS,
            SqlUtils.and(
                SqlUtils.join(TBL_TASKS, idName, TBL_TASK_USERS, COL_TASK),
                SqlUtils.equals(TBL_TASK_USERS, COL_USER, usr.getCurrentUserId())))
        .setWhere(where)
        .addOrder(TBL_TASKS, COL_FINISH_TIME, idName);

    return qs.getData(query);
  }

  private BeeRowSet getSettings() {
    long userId = usr.getCurrentUserId();
    Filter filter = Filter.equals(COL_USER, userId);

    BeeRowSet rowSet = qs.getViewData(VIEW_SERVICE_SETTINGS, filter);
    if (!DataUtils.isEmpty(rowSet)) {
      return rowSet;
    }

    SqlInsert sqlInsert = new SqlInsert(TBL_SERVICE_SETTINGS).addConstant(COL_USER, userId);

    ResponseObject response = qs.insertDataWithResponse(sqlInsert);
    if (response.hasErrors()) {
      return null;
    } else {
      return qs.getViewData(VIEW_SERVICE_SETTINGS, filter);
    }
  }
}
