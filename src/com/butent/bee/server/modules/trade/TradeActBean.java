package com.butent.bee.server.modules.trade;

import com.butent.bee.shared.data.value.TimeOfDayValue;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.projects.ProjectConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;
import static com.butent.bee.shared.time.TimeUtils.*;

import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.concurrency.ConcurrencyBean.HasTimerService;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.AdministrationModuleBean;
import com.butent.bee.server.modules.administration.ExchangeUtils;
import com.butent.bee.server.modules.administration.ExtensionIcons;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.trade.Totalizer;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActKind;
import com.butent.bee.shared.modules.trade.acts.TradeActTimeUnit;
import com.butent.bee.shared.modules.trade.acts.TradeActUtils;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.webservice.ButentWS;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;

@Stateless
@LocalBean
public class TradeActBean implements HasTimerService {

  private static BeeLogger logger = LogUtils.getLogger(TradeActBean.class);

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;
  @EJB
  DataEditorBean deb;
  @EJB
  ParamHolderBean prm;
  @EJB
  AdministrationModuleBean adm;

  @EJB TradeActDataEventHandler dataHandler;

  @EJB
  ConcurrencyBean cb;
  @Resource
  TimerService timerService;

  public List<SearchResult> doSearch(String query) {
    Set<String> columns = Sets.newHashSet(COL_TRADE_ACT_NAME, COL_TA_NUMBER, COL_OPERATION_NAME,
        COL_STATUS_NAME, ALS_COMPANY_NAME, COL_COMPANY_OBJECT_NAME);
    return qs.getSearchResults(VIEW_TRADE_ACTS, Filter.anyContains(columns, query));
  }

  public ResponseObject doService(String service, RequestInfo reqInfo) {
    ResponseObject response = null;

    String svc = BeeUtils.trim(service);
    switch (svc) {
      case SVC_GET_ITEMS_FOR_SELECTION:
        response = getItemsForSelection(reqInfo);
        break;

      case SVC_COPY_ACT:
        response = copyAct(reqInfo);
        break;

      case SVC_SAVE_ACT_AS_TEMPLATE:
        response = saveActAsTemplate(reqInfo);
        break;

      case SVC_GET_TEMPLATE_ITEMS_AND_SERVICES:
        response = getTemplateItemsAndServices(reqInfo);
        break;

      case SVC_GET_ITEMS_FOR_RETURN:
        response = getItemsForReturn(reqInfo);
        break;

      case SVC_GET_ITEMS_FOR_MULTI_RETURN:
        response = getItemsForMultiReturn(reqInfo);
        break;

      case SVC_MULTI_RETURN_ACT_ITEMS:
        response = doMultiReturnActItems(reqInfo);
        break;

      case SVC_SPLIT_ACT_SERVICES:
        response = splitActServices(reqInfo);
        break;

      case SVC_GET_ACTS_FOR_INVOICE:
        response = getActsForInvoice(reqInfo);
        break;

      case SVC_GET_NEXT_ACT_NUMBER:
        response = getNextActNumber(reqInfo);
        break;

      case SVC_GET_NEXT_RETURN_ACT_NUMBER:
        response = getNextReturnActNumber(reqInfo);
        break;

      case SVC_GET_NEXT_CHILD_ACT_NUMBER:
        response = getNextChildActNumber(reqInfo);
        break;

      case SVC_GET_SERVICES_FOR_INVOICE:
        response = getServicesForInvoice(reqInfo);
        break;

      case SVC_CREATE_ACT_INVOICE:
        response = createInvoice(reqInfo);
        break;

      case SVC_SYNCHRONIZE_ERP_DATA:
        syncERPData();
        response = ResponseObject.info(usr.getDictionary().imported());
        break;

      case SVC_ALTER_ACT_KIND:
        response = alterKind(reqInfo);
        break;

      case SVC_ITEMS_BY_COMPANY_REPORT:
        response = getItemsByCompanyReport(reqInfo);
        break;

      case SVC_STOCK_REPORT:
        response = getStockReport(reqInfo);
        break;

      case SVC_SERVICES_REPORT:
        response = getServicesReport(reqInfo);
        break;

      case SVC_TRANSFER_REPORT:
        response = getTransferReport(reqInfo);
        break;

      case SVC_HAS_INVOICES_OR_SECONDARY_ACTS:
        response = hasInvoicesOrSecondaryActs(reqInfo);
        break;
      case TradeActConstants.SVC_CREATE_INVOICE_ITEMS:
        response = createInvoiceItems(reqInfo);
        break;
      case SVC_REVERT_ACTS_STATUS_BEFORE_DELETE:
        response = restoreActStates(reqInfo);
        break;
      case SVC_ASSIGN_RENT_PROJECT:
        response = assignRentProject(reqInfo);
        break;
    }
    return response;
  }

  public Collection<BeeParameter> getDefaultParameters() {
    String module = ModuleAndSub.of(Module.TRADE, SubModule.ACTS).getName();

    return Lists.newArrayList(
        BeeParameter.createText(module, PRM_IMPORT_TA_ITEM_RX, false, RX_IMPORT_ACT_ITEM),
        BeeParameter.createNumber(module, PRM_TA_NUMBER_LENGTH, false, 6),
        BeeParameter.createRelation(module, PRM_RETURNED_ACT_STATUS,
            TBL_TRADE_STATUSES, COL_STATUS_NAME),
        BeeParameter.createRelation(module, PRM_APPROVED_ACT_STATUS, TBL_TRADE_STATUSES,
            COL_STATUS_NAME),
        BeeParameter.createRelation(module, PRM_COMBINED_ACT_STATUS, TBL_TRADE_STATUSES,
            COL_STATUS_NAME),
        BeeParameter.createRelation(module, PRM_CONTINUOUS_ACT_STATUS, TBL_TRADE_STATUSES,
            COL_STATUS_NAME),
        BeeParameter.createRelation(module, PRM_DEFAULT_TRADE_OPERATION, TBL_TRADE_OPERATIONS,
            COL_OPERATION_NAME),
        BeeParameter.createText(module, PRM_SYNC_ERP_DATA),
        BeeParameter.createNumber(module, PRM_SYNC_ERP_STOCK),
        BeeParameter.createText(module, PRM_INVOICE_MAIL_SIGNATURE),
        BeeParameter.createTimeOfDay(module, PRM_TRADE_SERVICE_TRANSITION_TIME, false, new TimeOfDayValue(14).getLong())
        );
  }

  @Override
  public void ejbTimeout(Timer timer) {
    syncERP(timer);
  }

  @Override
  public TimerService getTimerService() {
    return timerService;
  }

  public void init() {
    cb.createCalendarTimer(this.getClass(), PRM_SYNC_ERP_DATA);
    cb.createIntervalTimer(this.getClass(), PRM_SYNC_ERP_STOCK);

    sys.registerDataEventHandler(dataHandler);

    dataHandler.initConditions();

    sys.registerDataEventHandler(new DataEventHandler() {

      @Subscribe
      @AllowConcurrentEvents
      public void setRowProperties(ViewQueryEvent event) {
        if (event.isBefore()) {
          return;
        }

        if (BeeUtils.same(event.getTargetName(), VIEW_TRADE_ACT_FILES)) {
          ExtensionIcons.setIcons(event.getRowset(), ALS_FILE_NAME, PROP_ICON);
        }

        if (BeeUtils.same(event.getTargetName(), VIEW_TRADE_ACTS)) {
          BeeRowSet rs = event.getRowset();

          if (DataUtils.isEmpty(rs)) {
            return;
          }

          String count = SqlUtils.uniqueName();

          SqlSelect continuousCountsQuery = new SqlSelect()
              .addFields(TBL_TRADE_ACT_ITEMS, COL_TA_PARENT)
              .addCountDistinct(TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT, count)
              .addFrom(TBL_TRADE_ACT_ITEMS)
              .addFromInner(TBL_TRADE_ACTS, sys.joinTables(TBL_TRADE_ACTS, TBL_TRADE_ACT_ITEMS,
                  COL_TRADE_ACT))
              .setWhere(SqlUtils.and(
                  SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_KIND, TradeActKind.CONTINUOUS.ordinal()),
                  SqlUtils.inList(TBL_TRADE_ACT_ITEMS, COL_TA_PARENT, rs.getRowIds())
              ))
              .addGroup(TBL_TRADE_ACT_ITEMS, COL_TA_PARENT);

          SimpleRowSet continuousCounts = qs.getData(continuousCountsQuery);

          for (BeeRow row  : rs) {
            String prop = continuousCounts.getValueByKey(COL_TA_PARENT,
                BeeUtils.toString(row.getId()), count);

            if (!BeeUtils.isEmpty(prop)) {
              row.setProperty(PRP_CONTINUOUS_COUNT, prop);
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillActNumber(ViewInsertEvent event) {
        if (event.isBefore()
            && event.isTarget(VIEW_TRADE_ACTS)
            && (!DataUtils.contains(event.getColumns(), COL_TA_NUMBER))) {

          TradeActKind kind = null;
          Long series = null;

          for (int i = 0; i < event.getColumns().size(); i++) {
            switch (event.getColumns().get(i).getId()) {
              case COL_TA_KIND:
                kind = EnumUtils.getEnumByIndex(TradeActKind.class, event.getRow().getInteger(i));
                break;
              case COL_TA_SERIES:
                series = event.getRow().getLong(i);
                break;
            }
          }

          if (kind != null && kind.autoNumber() && DataUtils.isId(series)
              && kind != TradeActKind.RETURN) {
            BeeColumn column = sys.getView(VIEW_TRADE_ACTS).getBeeColumn(COL_TA_NUMBER);
            String number = getNextActNumber(series, column.getPrecision(), COL_TA_NUMBER);

            if (!BeeUtils.isEmpty(number)) {
              event.addValue(column, new TextValue(number));
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void maybeSetReturnedQty(ViewQueryEvent event) {
        if (event.isAfter(VIEW_TRADE_ACT_ITEMS) && event.hasData()
            && event.getColumnCount() >= sys.getView(event.getTargetName()).getColumnCount()) {

          BeeRowSet rowSet = event.getRowset();
          List<Long> actIds = DataUtils.getDistinct(rowSet, COL_TRADE_ACT);

          int actIndex = rowSet.getColumnIndex(COL_TRADE_ACT);
          int itemIndex = rowSet.getColumnIndex(COL_TA_ITEM);

          int qtyScale = rowSet.getColumn(COL_TRADE_ITEM_QUANTITY).getScale();

          for (Long actId : actIds) {
            TradeActKind kind = getActKind(actId);

            if (kind != null && kind.enableReturn()) {
              Map<Pair<Long, Long>, Double> returnedItems = getReturnedItems(actId);

              if (!returnedItems.isEmpty()) {
                for (BeeRow row : rowSet) {
                  if (actId.equals(row.getLong(actIndex))) {
                    Double qty = returnedItems.get(Pair.of(actId, row.getLong(itemIndex)));

                    if (BeeUtils.isPositive(qty)) {
                      row.setProperty(PRP_RETURNED_QTY, BeeUtils.toString(qty, qtyScale));
                    }
                  }
                }
              }
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      private void maybeSetRemainingQty(ViewQueryEvent event) {
        if (event.isAfter(VIEW_TRADE_ACT_ITEMS) && event.hasData()
            && event.getColumnCount() >= sys.getView(event.getTargetName()).getColumnCount()) {

          BeeRowSet rowSet = event.getRowset();
          List<Long> parentActIds = DataUtils.getDistinct(rowSet, COL_TA_PARENT);
          List<Long> actIds = DataUtils.getDistinct(rowSet, COL_TRADE_ACT);

          if (BeeUtils.isEmpty(parentActIds)) {
            return;
          }

          int actIndex = rowSet.getColumnIndex(COL_TRADE_ACT);
          int parentActIndex = rowSet.getColumnIndex(COL_TA_PARENT);
          int itemIndex = rowSet.getColumnIndex(COL_TA_ITEM);

          int qtyScale = rowSet.getColumn(COL_TRADE_ITEM_QUANTITY).getScale();
          Map<Pair<Long, Long>, Double> remainData = null;

          for (Long actId : actIds) {
            TradeActKind kind = getActKind(actId);

            if (kind != null && kind == TradeActKind.RETURN) {
              if (remainData == null) {
                remainData = new HashMap<>();
                BeeRowSet remainItems = getRemainingItems(parentActIds.toArray(new Long[parentActIds
                    .size()]));

                if (!DataUtils.isEmpty(remainItems)) {
                  for (int i = 0; i < remainItems.getNumberOfRows(); i++) {
                    Long item = remainItems.getLong(i, COL_ITEM);
                    Long act = remainItems.getLong(i, COL_TRADE_ACT);
                    Double qty = remainItems.getDouble(i, COL_TRADE_ITEM_QUANTITY);

                    remainData.put(Pair.of(item, act), qty);
                  }
                }
              }

              if (!remainData.isEmpty()) {
                for (BeeRow row : rowSet) {
                  if (actId.equals(row.getLong(actIndex))) {
                    Long parentAct = row.getLong(parentActIndex);
                    Long item = row.getLong(itemIndex);

                    Double qty = remainData.get(Pair.of(item, parentAct));

                    if (BeeUtils.isDouble(qty)) {
                      row.setProperty(PRP_REMAINING_QTY, BeeUtils.toString(qty, qtyScale));
                    }
                  }
                }
              }
            }
          }
        }
      }

    });
  }

  private static SqlInsert appendInsertConstants(SqlInsert query, BeeRowSet values, Long valueId,
                                                 Collection<BeeColumn> columns) {
    int valueIndex = values.getRowIndex(valueId);
    if (BeeConst.isUndef(valueIndex)) {
      return query;
    }

    for (BeeColumn col : columns) {
      if (values.getColumn(col.getId()).isEditable()) {

        switch (values.getColumn(col.getId()).getType()) {
          case BOOLEAN:
            Boolean boolValue = values.getBoolean(valueIndex, col.getId());
            query.addConstant(col.getId(), boolValue);
            break;
          default:
            String strValue = values.getString(valueIndex, col.getId());
            if (!BeeUtils.isEmpty(strValue)) {
              query.addConstant(col.getId(), strValue);
            }
        }
      }
    }

    return query;
  }

  private static SqlUpdate appendUpdateConstants(SqlUpdate query, BeeRowSet values, Long valueId,
                                                 Collection<BeeColumn> columns) {
    int valueIndex = values.getRowIndex(valueId);
    if (BeeConst.isUndef(valueIndex)) {
      return query;
    }

    for (BeeColumn col : columns) {
      if (values.getColumn(col.getId()).isEditable()) {

        switch (values.getColumn(col.getId()).getType()) {
          case BOOLEAN:
            Boolean boolValue = values.getBoolean(valueIndex, col.getId());
            query.addConstant(col.getId(), boolValue);
            break;
          default:
            String strValue = values.getString(valueIndex, col.getId());
            if (!BeeUtils.isEmpty(strValue)) {
              query.addConstant(col.getId(), strValue);
            }
        }
      }
    }

    return query;
  }

  private Map<String, Long> getReferences(String tableName, String keyName) {
    return getReferences(tableName, keyName, null);
  }

  private Map<String, Long> getReferences(String tableName, String keyName,
      IsCondition clause) {
    Map<String, Long> ref = new HashMap<>();

    for (SimpleRow row : qs.getData(new SqlSelect()
        .addFields(tableName, keyName)
        .addField(tableName, sys.getIdName(tableName), tableName)
        .addFrom(tableName)
        .setWhere(SqlUtils.and(SqlUtils.notNull(tableName, keyName), clause)))) {

      ref.put(row.getValue(keyName), row.getLong(tableName));
    }
    return ref;
  }

  private ResponseObject alterKind(RequestInfo reqInfo) {
    Long actId = reqInfo.getParameterLong(COL_TRADE_ACT);
    if (!DataUtils.isId(actId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TRADE_ACT);
    }

    TradeActKind kind = EnumUtils.getEnumByIndex(TradeActKind.class,
        reqInfo.getParameter(COL_TA_KIND));
    if (kind == null) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TA_KIND);
    }

    BeeRowSet rowSet = qs.getViewData(VIEW_TRADE_ACTS, Filter.compareId(actId));
    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.error(reqInfo.getService(), VIEW_TRADE_ACTS, actId, "not available");
    }

    BeeRow oldRow = rowSet.getRow(0);
    BeeRow newRow = DataUtils.cloneRow(oldRow);

    newRow.setValue(rowSet.getColumnIndex(COL_TA_KIND), kind.ordinal());
    Long series = oldRow.getLong(rowSet.getColumnIndex(COL_TA_SERIES));

    if (kind.autoNumber()) {
      int numberIndex = rowSet.getColumnIndex(COL_TA_NUMBER);

      if (DataUtils.isId(series) && oldRow.isNull(numberIndex)) {
        String number =
            getNextActNumber(series, rowSet.getColumn(numberIndex).getPrecision(), COL_TA_NUMBER);
        if (!BeeUtils.isEmpty(number)) {
          newRow.setValue(numberIndex, number);
        }
      }
    }

    Long operation = getDefaultOperation(kind, series);
    newRow.setValue(rowSet.getColumnIndex(COL_TA_OPERATION), operation);
    newRow.setValue(rowSet.getColumnIndex(COL_TA_DATE), TimeUtils.nowMinutes());

    BeeRowSet updated = DataUtils.getUpdated(rowSet.getViewName(), rowSet.getColumns(),
        oldRow, newRow, null);

    ResponseObject response = deb.commitRow(updated);
    if (!response.hasErrors() && !kind.enableServices()) {
      SqlDelete delete = new SqlDelete(TBL_TRADE_ACT_SERVICES)
          .setWhere(SqlUtils.equals(TBL_TRADE_ACT_SERVICES, COL_TRADE_ACT, actId));
      qs.updateData(delete);
    }

    return response;
  }

  private ResponseObject assignRentProject(RequestInfo reqInfo) {
    Long rentProject = reqInfo.getParameterLong(COL_TRADE_ACT);
    Set<Long> acts = DataUtils.parseIdSet(reqInfo.getParameter(VAR_ID_LIST));
    Integer kind = qs.getInt(new SqlSelect().addFields(TBL_TRADE_ACTS, COL_TA_KIND)
            .addFrom(TBL_TRADE_ACTS).setWhere(sys.idEquals(TBL_TRADE_ACTS, rentProject)));

    if (!Objects.equals(kind, TradeActKind.RENT_PROJECT.ordinal())) {
      return ResponseObject.error("Aktas " + rentProject + "nėra nuomos proj.");
    }

    SimpleRow rentProjectAct = qs.getRow(new SqlSelect()
            .addFields(TBL_TRADE_ACTS, COL_TA_CONTRACT)
            .addFrom(TBL_TRADE_ACTS)
            .setWhere(sys.idEquals(TBL_TRADE_ACTS, rentProject)));

    SqlUpdate upd = new SqlUpdate(TBL_TRADE_ACTS)
            .addConstant(COL_TA_RENT_PROJECT, rentProject)
            .addExpression(COL_TA_CONTRACT, SqlUtils.sqlIf(SqlUtils.isNull(TBL_TRADE_ACTS, COL_TA_CONTRACT),
                    rentProjectAct.getLong(COL_TA_CONTRACT), SqlUtils.field(TBL_TRADE_ACTS, COL_TA_CONTRACT)))
            .setWhere(sys.idInList(TBL_TRADE_ACTS, acts));

    ResponseObject resp = qs.updateDataWithResponse(upd);

    if (resp.hasErrors()) {
      return resp;
    }

    resp = ResponseObject.response(qs.getViewData(VIEW_TRADE_ACTS, Filter.idIn(acts)), BeeRowSet.class);

    return resp;
  }

  private ResponseObject copyAct(RequestInfo reqInfo) {
    Long actId = reqInfo.getParameterLong(COL_TRADE_ACT);
    if (!DataUtils.isId(actId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TRADE_ACT);
    }

    BeeRowSet rowSet = qs.getViewData(VIEW_TRADE_ACTS, Filter.compareId(actId));
    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.error(reqInfo.getService(), VIEW_TRADE_ACTS, actId, "not available");
    }

    BeeRow row = rowSet.getRow(0);

    row.setValue(rowSet.getColumnIndex(COL_TA_DATE), nowMinutes());

    int index = rowSet.getColumnIndex(COL_TA_UNTIL);
    if (!row.isNull(index) && BeeUtils.isLess(row.getDateTime(index),
        startOfNextMonth(today()).getDateTime())) {
      row.clearCell(index);
    }

    row.clearCell(rowSet.getColumnIndex(COL_TA_NUMBER));
    row.clearCell(rowSet.getColumnIndex(COL_TA_CONTINUOUS));
    row.clearCell(rowSet.getColumnIndex(COL_TA_RETURN));
    row.clearCell(rowSet.getColumnIndex(COL_STATUS_NAME));
    row.clearCell(rowSet.getColumnIndex(COL_TRADE_STATUS));

    BeeRowSet insert = DataUtils.createRowSetForInsert(rowSet.getViewName(), rowSet.getColumns(),
        row);
    ResponseObject response = deb.commitRow(insert);

    if (!response.hasErrors() && response.hasResponse(BeeRow.class)) {
      long newId = ((BeeRow) response.getResponse()).getId();

      qs.copyData(TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT, actId, newId);
      qs.copyData(TBL_TRADE_ACT_SERVICES, COL_TRADE_ACT, actId, newId);
    }

    return response;
  }

  private ResponseObject createContinuousAct(BeeRowSet parentActs, long fifoId, Collection<Long> fifoActs, DateTime actDate) {
    if (parentActs.isEmpty()) {
      return ResponseObject.emptyResponse();
    }

    List<Long> parentIds = parentActs.getRowIds();
    BeeRowSet remainItems = getRemainingItems(parentIds.toArray(new Long[parentIds.size()]));
    BeeRow fifoAct = parentActs.getRowById(fifoId);

    if (DataUtils.isEmpty(remainItems)) {
      return ResponseObject.response(fifoAct == null ? fifoId : fifoAct.getId());
    }

    String number = fifoAct.getString(parentActs.getColumnIndex(COL_TA_NUMBER));
    Long series = fifoAct.getLong(parentActs.getColumnIndex(COL_TA_SERIES));
//    DateTime now = nowMinutes();
    Long combStatus = prm.getRelation(PRM_COMBINED_ACT_STATUS);
    Long returnedActStatus = prm.getRelation(PRM_RETURNED_ACT_STATUS);
    Long continuousStatus = prm.getRelation(PRM_CONTINUOUS_ACT_STATUS);

    SqlSelect actNumbersQuery = new SqlSelect()
//        .addFields(TBL_TRADE_ACTS, COL_TA_NUMBER)
        .addExpr(SqlUtils.concat("'" + usr.getDictionary().taContinuousFrom() + "'", "' '",
            SqlUtils.field(TBL_TRADE_ACTS, COL_TA_NUMBER)), COL_TA_NOTES)
        .addFrom(TBL_TRADE_ACTS)
        .setWhere(SqlUtils.and(
            SqlUtils.inList(TBL_TRADE_ACTS, sys.getIdName(TBL_TRADE_ACTS), parentIds),
            SqlUtils.notNull(TBL_TRADE_ACTS, COL_TA_NUMBER)
            )
        );

    actNumbersQuery
        .addGroup(SqlUtils.field(TBL_TRADE_ACTS, sys.getIdName(TBL_TRADE_ACTS)));

    SqlInsert actInsert = new SqlInsert(TBL_TRADE_ACTS)
        .addConstant(COL_TA_KIND, TradeActKind.CONTINUOUS.ordinal())
        .addConstant(COL_TA_NUMBER, number + " T-" + getNextChildActNumber(TradeActKind
            .CONTINUOUS, series, parentIds.get(0), COL_TA_NUMBER))
        .addConstant(COL_TA_DATE, actDate)
        .addConstant(COL_TA_MANAGER, usr.getCurrentUserId())
        .addConstant(COL_TA_NOTES, BeeUtils.joinItems(Lists.newArrayList(qs.getColumn(
            actNumbersQuery))));

    for (String colName : VAR_COPY_TA_COLUMN_NAMES) {
      if (BeeUtils.same(colName, COL_TA_OPERATION)) {
        continue;
      }

      if (!parentActs.containsColumn(colName)) {
        continue;
      }

      if (BeeUtils.isEmpty(parentActs.getString(0, colName))) {
        continue;
      }

      actInsert.addConstant(colName, parentActs.getString(0, colName));
    }

    if (DataUtils.isId(continuousStatus)) {
      actInsert.addConstant(COL_TA_STATUS, continuousStatus);
    }

    ResponseObject response = qs.insertDataWithResponse(actInsert);
    if (response.hasErrors()) {
      return response;
    }

    Long continuousActId = response.getResponseAsLong();

    response = insertChildItems(continuousActId, remainItems);

    if (response.hasErrors()) {
      return response;
    }

    Set<Long> lockActData = getRelatedLockActs(fifoActs, continuousActId);

    BeeRowSet services = qs.getViewData(VIEW_TRADE_ACT_SERVICES,
        Filter.and(
            Filter.or(
                Filter.and(
                    Filter.isNull(COL_TA_SERVICE_FROM),
                    Filter.isNull(COL_TA_SERVICE_TO),
                    Filter.isNull(COL_TIME_UNIT)
                ),
                Filter.and(
                    Filter.isNull(COL_TA_SERVICE_TO),
                    Filter.notNull(COL_TIME_UNIT)
                )
            ),
            Filter.and(
                Filter.any(COL_TRADE_ACT, lockActData),
                Filter.isNull(COL_TA_CONTINUOUS)
            )
        )
    );

    if (!DataUtils.isEmpty(services)) {
      response = createServices(continuousActId, services, actDate);
    }

    if (response.hasErrors()) {
      return response;
    }

    response = createLockRelations(lockActData, continuousActId, COL_TA_CONTINUOUS);

    if (response.hasErrors()) {
      return response;
    }

    if (DataUtils.isId(combStatus)) {
      IsCondition cond = SqlUtils.and(
          sys.idInList(TBL_TRADE_ACTS, lockActData),
          SqlUtils.notEqual(TBL_TRADE_ACTS, COL_TA_KIND, TradeActKind.RETURN.ordinal()));

      if (DataUtils.isId(returnedActStatus)) {
        cond = SqlUtils.and(cond, SqlUtils.notEqual(TBL_TRADE_ACTS, COL_TA_STATUS,
            returnedActStatus));
      }

      SqlUpdate query = new SqlUpdate(TBL_TRADE_ACTS)
          .addConstant(COL_TA_STATUS, combStatus)
          .setWhere(cond);

      response = qs.updateDataWithResponse(query);
    }

    if (response.hasErrors()) {
      return response;
    }

    return ResponseObject.response(continuousActId);
  }

  private ResponseObject createInvoice(RequestInfo reqInfo) {
    String serialized = reqInfo.getParameter(VIEW_SALES);
    if (BeeUtils.isEmpty(serialized)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VIEW_SALES);
    }

    BeeRowSet sales = BeeRowSet.restore(serialized);
    if (DataUtils.isEmpty(sales)) {
      return ResponseObject.error(reqInfo.getService(), sales.getViewName(), "is empty");
    }

    serialized = reqInfo.getParameter(VIEW_SALE_ITEMS);
    if (BeeUtils.isEmpty(serialized)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VIEW_SALE_ITEMS);
    }

    BeeRowSet saleItems = BeeRowSet.restore(serialized);
    if (DataUtils.isEmpty(saleItems)) {
      return ResponseObject.error(reqInfo.getService(), saleItems.getViewName(), "is empty");
    }

    if (sales.containsColumn(COL_TA_OPERATION)) {
      Long tradeOperation = sales.getLong(0, COL_TA_OPERATION);

      SqlSelect selectWrh = new SqlSelect()
          .addFields(TBL_TRADE_OPERATIONS, COL_TRADE_WAREHOUSE_FROM)
          .addFrom(TBL_TRADE_OPERATIONS)
          .setWhere(sys.idEquals(TBL_TRADE_OPERATIONS, tradeOperation));

      Long warehouse = qs.getLong(selectWrh);
      if (DataUtils.isId(warehouse)) {
        sales.setValue(0, DataUtils.getColumnIndex(COL_TRADE_WAREHOUSE_FROM, sales.getColumns()),
            warehouse);
      } else {
        sales.removeColumn(DataUtils.getColumnIndex(COL_TRADE_WAREHOUSE_FROM, sales.getColumns()));
      }
    }

    if (sales.containsColumn(COL_TRADE_NOTES)) {
      Set<Long> objects = DataUtils.parseIdSet(sales.getString(0, COL_TRADE_NOTES));

      SqlSelect selectObjUsers = new SqlSelect()
        .addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME)
        .addFrom(TBL_COMPANY_OBJECTS)
        .addFromLeft(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_OBJECTS, "ObjectPerson"))
        .setWhere(SqlUtils.and(sys.idInList(TBL_COMPANY_OBJECTS, objects), SqlUtils.or(SqlUtils.notNull(TBL_PERSONS,
          COL_FIRST_NAME), SqlUtils.notNull(TBL_PERSONS, COL_LAST_NAME))));

      SimpleRowSet rowSet = qs.getData(selectObjUsers);
      int count = rowSet.getNumberOfRows();
      if (count > 0) {
        String persons = "Atsakingas asmuo";
        for (int i = 0; i < count; i++) {
          persons = BeeUtils.joinWords(persons, rowSet.getRow(i).getValue(0), rowSet.getRow(i).getValue(1));
          if (i < count - 1) {
             persons += ", ";
          }
        }
        sales.setValue(0, DataUtils.getColumnIndex(COL_TRADE_NOTES, sales.getColumns()), persons);
      } else {
        sales.clearCell(0, DataUtils.getColumnIndex(COL_TRADE_NOTES, sales.getColumns()));
      }
    }

    ResponseObject response = deb.commitRow(sales);
    if (response.hasErrors() || !response.hasResponse(BeeRow.class)) {
      return response;
    }

    long invoiceId = ((BeeRow) response.getResponse()).getId();

    int colIndex = saleItems.getColumnIndex(COL_SALE);

    for (int i = 0; i < saleItems.getNumberOfRows(); i++) {
      BeeRow saleItem = saleItems.getRow(i);
      saleItem.setValue(colIndex, invoiceId);

      ResponseObject insResponse = deb.commitRow(saleItems, i, RowInfo.class);
      if (insResponse.hasErrors()) {
        response.addMessagesFrom(insResponse);
        break;
      }

      String svcId = saleItem.getProperty(COL_TA_INVOICE_SERVICE);
      String from = saleItem.getProperty(PRP_TA_SERVICE_FROM);
      String to = saleItem.getProperty(PRP_TA_SERVICE_TO);

      if (DataUtils.isId(svcId) && BeeUtils.isPositiveInt(from) && BeeUtils.isPositiveInt(to)) {
        SqlInsert insert = new SqlInsert(TBL_TRADE_ACT_INVOICES)
            .addConstant(COL_TA_INVOICE_SERVICE, svcId)
            .addConstant(COL_TA_INVOICE_ITEM, ((RowInfo) insResponse.getResponse()).getId())
            .addConstant(COL_TA_INVOICE_FROM, new JustDate(BeeUtils.toInt(from)))
            .addConstant(COL_TA_INVOICE_TO, new JustDate(BeeUtils.toInt(to)));

        /* Transrifus TID31400 { */
        if (saleItem.hasPropertyValue(COL_TA_SERVICE_FACTOR)) {
          insert.addConstant(COL_TA_SERVICE_FACTOR,
            saleItem.getPropertyDouble(COL_TA_SERVICE_FACTOR));
        }
        /* } Transrifus TID31400 */

        qs.insertData(insert);
      }
    }

    Long approvedStatusId = prm.getRelation(PRM_APPROVED_ACT_STATUS);
    Long returnedStatusId = prm.getRelation(PRM_RETURNED_ACT_STATUS);

    if (DataUtils.isId(returnedStatusId) && DataUtils
        .isId(approvedStatusId) && !BeeUtils
        .isEmpty(reqInfo.getParameter(VAR_ID_LIST))) {

      List<Long> acts = DataUtils.parseIdList(reqInfo.getParameter(VAR_ID_LIST));

      maybeApproveTradeActs(returnedStatusId, approvedStatusId, acts);
    }

    return response;
  }

  private ResponseObject createInvoiceItems(RequestInfo reqInfo) {
    Long saleId = BeeUtils.toLongOrNull(reqInfo.getParameter(TradeConstants.COL_SALE));
    Long currency = BeeUtils.toLongOrNull(reqInfo.getParameter(TradeConstants.COL_TRADE_CURRENCY));
    Set<Long> ids = DataUtils.parseIdSet(reqInfo.getParameter(Service.VAR_ID));

    if (!DataUtils.isId(saleId)) {
      return ResponseObject.error("Wrong sale ID");
    }
    if (!DataUtils.isId(currency)) {
      return ResponseObject.error("Wrong currency ID");
    }
    if (BeeUtils.isEmpty(ids)) {
      return ResponseObject.error("Empty ID list");
    }

    IsCondition where = sys.idInList(TBL_TRADE_ACT_ITEMS, ids);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_ACT_ITEMS, TradeConstants.COL_TRADE_VAT_PLUS,
            TradeConstants.COL_TRADE_VAT, TradeConstants.COL_TRADE_VAT_PERC, COL_INCOME_ITEM,
            TradeConstants.COL_TRADE_ITEM_PRICE, TradeConstants.COL_TRADE_DISCOUNT,
            TradeConstants.COL_TRADE_ITEM_QUANTITY, COL_INCOME_NOTE)
        .addFields(TBL_ITEMS, COL_ITEM_ARTICLE)
        .addFrom(TBL_TRADE_ACT_ITEMS)
        .addFromLeft(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_TRADE_ACT_ITEMS, COL_ITEM))
        .setWhere(where);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.error(TBL_TRADE_ACT_ITEMS, ids, "not found");
    }

    ResponseObject response = new ResponseObject();

    for (SimpleRow row : data) {
      Long item = row.getLong(COL_INCOME_ITEM);
      SqlInsert insert = new SqlInsert(TradeConstants.TBL_SALE_ITEMS)
          .addConstant(TradeConstants.COL_SALE, saleId)
          .addConstant(ClassifierConstants.COL_ITEM, item);

      Boolean vatPerc = row.getBoolean(TradeConstants.COL_TRADE_VAT_PERC);
      Double vat;
      if (BeeUtils.isTrue(vatPerc)) {
        insert.addConstant(TradeConstants.COL_TRADE_VAT_PERC, vatPerc);
        vat = row.getDouble(TradeConstants.COL_TRADE_VAT);
      } else {
        vat = row.getDouble(TradeConstants.COL_TRADE_VAT);
      }

      if (!BeeUtils.isEmpty(row.getValue(COL_ITEM_ARTICLE))) {
        insert.addConstant(COL_ITEM_ARTICLE, row.getValue(COL_ITEM_ARTICLE));
      }

      if (BeeUtils.nonZero(vat)) {
        insert.addConstant(TradeConstants.COL_TRADE_VAT, vat);
      }

      Boolean vatPlus = row.getBoolean(TradeConstants.COL_TRADE_VAT_PLUS);

      if (BeeUtils.isTrue(vatPlus)) {
        insert.addConstant(TradeConstants.COL_TRADE_VAT_PLUS, vatPlus);
      }

      Double quantity = row.getDouble(TradeConstants.COL_TRADE_ITEM_QUANTITY);
      Double price = row.getDouble(TradeConstants.COL_TRADE_ITEM_PRICE);
      Double discount = row.getDouble(TradeConstants.COL_TRADE_DISCOUNT);

      insert.addConstant(TradeConstants.COL_TRADE_ITEM_QUANTITY, BeeUtils.unbox(quantity));

      if (price != null) {
        insert.addConstant(TradeConstants.COL_TRADE_ITEM_PRICE, price);
      }

      if (discount != null) {
        insert.addConstant(TradeConstants.COL_TRADE_DISCOUNT, discount);
      }

      if (data.hasColumn(COL_INCOME_NOTE)) {
        String notes = row.getValue(COL_INCOME_NOTE);

        if (!BeeUtils.isEmpty(notes)) {
          insert.addConstant(TradeConstants.COL_TRADE_ITEM_NOTE, notes);
        }
      }

      ResponseObject insResponse = qs.insertDataWithResponse(insert);
      if (insResponse.hasErrors()) {
        response.addMessagesFrom(insResponse);
        break;
      }
    }

    if (!response.hasErrors()) {
      SqlUpdate update = new SqlUpdate(TBL_TRADE_ACT_ITEMS)
          .addConstant(COL_INCOME_SALE, saleId)
          .setWhere(where);

      ResponseObject updResponse = qs.updateDataWithResponse(update);
      if (updResponse.hasErrors()) {
        response.addMessagesFrom(updResponse);
      }
    }

    return response;

  }

  private ResponseObject createLockRelations(Set<Long> lockActData, Long relId, String relCol) {
    if (!lockActData.isEmpty()) {
      TradeActKind k;
      switch (relCol) {
        case COL_TA_CONTINUOUS:
          k = TradeActKind.CONTINUOUS;
          break;
        default:
          k = TradeActKind.RETURN;
      }

      String number = qs.getValue(new SqlSelect()
          .addFields(TBL_TRADE_ACTS, COL_TA_NUMBER)
          .addFrom(TBL_TRADE_ACTS)
          .setWhere(sys.idEquals(TBL_TRADE_ACTS, relId)));

      ResponseObject ro = qs.updateDataWithResponse(new SqlUpdate(TBL_TRADE_ACTS)
          .addConstant(relCol, relId)
          .addConstant(COL_TA_NOTES,
              SqlUtils.concat(
                  SqlUtils.nvl(SqlUtils.field(TBL_TRADE_ACTS, COL_TA_NOTES), "''"),
                  "'\n'",
                  BeeUtils.joinWords("'", usr.getDictionary().createdOn(),
                      k.getCaption(usr.getDictionary()), number, "'")
              )
          )
          .setWhere(SqlUtils.and(sys.idInList(TBL_TRADE_ACTS, lockActData),
              SqlUtils.isNull(TBL_TRADE_ACTS, relCol))));

      if (ro.hasErrors() || k != TradeActKind.CONTINUOUS) {
        return ro;
      }

      Long combActStatus = prm.getRelation(PRM_COMBINED_ACT_STATUS);

      if (!DataUtils.isId(combActStatus)) {
        return ro;
      }

      ro = qs.updateDataWithResponse(new SqlUpdate(TBL_TRADE_ACTS)
          .addConstant(COL_TA_STATUS, combActStatus)
          .setWhere(
              SqlUtils.and(
                  SqlUtils.equals(TBL_TRADE_ACTS, relCol, relId),
                  SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_KIND, k.ordinal())))
      );

      return ro;

    }

    return ResponseObject.emptyResponse();
  }

  private ResponseObject createServices(Long continuousId, BeeRowSet services, DateTime svcTimes) {
    List<BeeColumn> colData = new ArrayList<>();
    int idxSvcActs = services.getColumnIndex(COL_TRADE_ACT);

    Set<Long> actIds = services.getDistinctLongs(idxSvcActs);

    BeeRowSet items = getRemainingItems(actIds.toArray(new Long[actIds.size()]));
    List<Long> contServices = new ArrayList<>();
    double itemValue = 0.0;

   for (BeeRow row : items) {
      double price = BeeUtils.unbox(row.getDouble(items.getColumnIndex(COL_ITEM_PRICE)));
      double qty = BeeUtils.unbox(row.getDouble(items.getColumnIndex("Quantity"))) /*-
              (row.hasPropertyValue("returned_qty") ? BeeUtils.unbox(row.getPropertyDouble("returned_qty")) : 0D)*/;

      double sum = price * qty;
      double dscSum = sum / 100 * BeeUtils.unbox(row.getDouble(items.getColumnIndex(COL_TRADE_DISCOUNT)));
      double vat = BeeUtils.unbox(row.getDouble(items.getColumnIndex(COL_TRADE_VAT)));

      sum -= dscSum;

      if (BeeUtils.unbox(row.getBoolean(items.getColumnIndex(COL_TRADE_VAT_PLUS)))) {
        if (BeeUtils.unbox(row.getBoolean(items.getColumnIndex(COL_TRADE_VAT_PERC)))) {
            vat = sum /100 * vat;
        }
      } else {
        if (BeeUtils.unbox(row.getBoolean(items.getColumnIndex(COL_TRADE_VAT_PERC)))) {
          vat = sum - (sum / (1 + vat / 100));
        }
        sum -= vat;
      }

      itemValue += sum;

    }

    if (!DataUtils.isEmpty(items)) {
      Set<Long> notReturnedActs = items.getDistinctLongs(items.getColumnIndex(COL_TRADE_ACT));

      for (BeeColumn col : services.getColumns()) {
        if (BeeUtils.inListSame(col.getId(), COL_TRADE_ACT, COL_TA_SERVICE_FROM)) {
          continue;
        }
        colData.add(col);
      }

      for (BeeRow service : services) {

        if (!notReturnedActs.contains(service.getLong(services.getColumnIndex(COL_TRADE_ACT)))) {
          continue;
        }

        SqlInsert svcInsert = new SqlInsert(TBL_TRADE_ACT_SERVICES)
            .addConstant(COL_TRADE_ACT, continuousId);

        if (!BeeUtils.isEmpty(service.getString(services.getColumnIndex(COL_TIME_UNIT)))) {
          svcInsert.addConstant(COL_TA_SERVICE_FROM, svcTimes);
        }

        appendInsertConstants(svcInsert, services, service.getId(), colData);

        if (!BeeUtils.isEmpty(service.getString(services.getColumnIndex(COL_TIME_UNIT)))) {
          contServices.add(service.getLong(services.getColumnIndex(COL_ITEM)));
        }

        ResponseObject serviceResponse = qs.insertDataWithResponse(svcInsert);
        if (serviceResponse.hasErrors()) {
          return serviceResponse;
        }
      }
    }

    int timeUnitIdx = DataUtils.getColumnIndex(COL_TIME_UNIT, services.getColumns());

    if (!BeeConst.isUndef(timeUnitIdx)) {
      List<Long> oneTimeServices = new ArrayList<>();

      for (BeeRow service : services) {
        TradeActTimeUnit timeUnit = service.getEnum(timeUnitIdx, TradeActTimeUnit.class);
        if (timeUnit == null) {
          oneTimeServices.add(service.getId());
        }
      }

      if (!oneTimeServices.isEmpty()) {
        SqlDelete dltServicesFromActs = new SqlDelete(TBL_TRADE_ACT_SERVICES)
            .setWhere(sys.idInList(TBL_TRADE_ACT_SERVICES, oneTimeServices));
        qs.updateData(dltServicesFromActs);
      }
    }

    if (!BeeUtils.isEmpty(contServices)) {

      SqlUpdate updateService = new SqlUpdate(TBL_TRADE_ACT_SERVICES)
          .addConstant(COL_TA_SERVICE_TO, svcTimes);

      if (BeeUtils.isPositive(itemValue)) {
        updateService.addConstant("ItemValue", itemValue);
      }
          updateService.setWhere(SqlUtils.and(
              SqlUtils.inList(TBL_TRADE_ACT_SERVICES, COL_TRADE_ACT, actIds),
              SqlUtils.isNull(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_TO),
              SqlUtils.inList(TBL_TRADE_ACT_SERVICES, COL_ITEM, contServices)
          ));

      return qs.updateDataWithResponse(updateService);
    }

    return ResponseObject.emptyResponse();
  }

  private long exchange(String target, String fromCol, long to, long time, String... columns) {
    long result = 0;

    SqlSelect fromQuery = new SqlSelect()
        .setDistinctMode(true)
        .addFields(target, fromCol)
        .addFrom(target)
        .setWhere(SqlUtils.and(SqlUtils.notNull(target, fromCol),
            SqlUtils.notEqual(target, fromCol, to)));

    List<Long> currencies = qs.getLongList(fromQuery);
    if (currencies.isEmpty()) {
      return result;
    }

    double toRate = adm.getRate(to, time);

    for (long from : currencies) {
      double fromRate = adm.getRate(from, time);
      if (Double.compare(toRate, fromRate) == BeeConst.COMPARE_EQUAL) {
        continue;
      }

      double rate = fromRate / toRate;

      for (String column : columns) {
        SqlUpdate update = new SqlUpdate(target)
            .addExpression(column, SqlUtils.multiply(SqlUtils.field(target, column), rate))
            .setWhere(SqlUtils.and(SqlUtils.equals(target, fromCol, from),
                SqlUtils.notNull(target, column)));

        int count = qs.updateData(update);
        if (count > 0) {
          result += count;
        }
      }
    }

    return result;
  }

  private Set<Long> getActItems(Long actId) {
    if (DataUtils.isId(actId)) {
      return qs.getLongSet(new SqlSelect()
          .addFields(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM)
          .addFrom(TBL_TRADE_ACT_ITEMS)
          .setWhere(SqlUtils.equals(TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT, actId)));

    } else {
      return BeeConst.EMPTY_IMMUTABLE_LONG_SET;
    }
  }

  public TradeActKind getActKind(Long actId) {
    if (DataUtils.isId(actId)) {
      Integer value = qs.getInt(new SqlSelect()
          .addFields(TBL_TRADE_ACTS, COL_TA_KIND)
          .addFrom(TBL_TRADE_ACTS)
          .setWhere(sys.idEquals(TBL_TRADE_ACTS, actId)));

      return EnumUtils.getEnumByIndex(TradeActKind.class, value);

    } else {
      return null;
    }
  }

  private Set<Long> getActServices(Long actId) {
    if (DataUtils.isId(actId)) {
      return qs.getLongSet(new SqlSelect()
          .addFields(TBL_TRADE_ACT_SERVICES, COL_TA_ITEM)
          .addFrom(TBL_TRADE_ACT_SERVICES)
          .setWhere(SqlUtils.equals(TBL_TRADE_ACT_SERVICES, COL_TRADE_ACT, actId)));

    } else {
      return BeeConst.EMPTY_IMMUTABLE_LONG_SET;
    }
  }

  private Collection<Integer> getHolidays() {
    Long countryId = prm.getRelation(PRM_COUNTRY);

    if (DataUtils.isId(countryId)) {
      SqlSelect holidaysQuery = new SqlSelect()
          .addFields(TBL_HOLIDAYS, COL_HOLY_DAY)
          .addFrom(TBL_HOLIDAYS)
          .setWhere(SqlUtils.equals(TBL_HOLIDAYS, COL_HOLY_COUNTRY, countryId));

      return Lists.newArrayList(qs.getIntColumn(holidaysQuery));

    } else {
      return BeeConst.EMPTY_IMMUTABLE_INT_SET;
    }

  }

  private Long getDefaultOperation(TradeActKind kind, Long seriesId) {
    IsCondition whKind = SqlUtils.equals(TBL_TRADE_OPERATIONS, COL_OPERATION_KIND, kind.ordinal());

    IsCondition whSer = DataUtils.isId(seriesId)
        ? SqlUtils.equals(TBL_TRADE_OPERATIONS, COL_TA_SERIES, seriesId)
        : SqlUtils.isNull(TBL_TRADE_OPERATIONS, COL_TA_SERIES);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_OPERATIONS, sys.getIdName(TBL_TRADE_OPERATIONS))
        .addFrom(TBL_TRADE_OPERATIONS)
        .setWhere(SqlUtils.and(whKind, whSer,
            SqlUtils.notNull(TBL_TRADE_OPERATIONS, COL_OPERATION_DEFAULT)));

    List<Long> operations = qs.getLongList(query);

    if (operations.size() == 1) {
      return operations.get(0);
    }
    query.setWhere(SqlUtils.and(whKind, whSer));
    operations = qs.getLongList(query);

    if (operations.size() == 1) {
      return operations.get(0);
    }
    query.setWhere(SqlUtils.and(whKind, SqlUtils.isNull(TBL_TRADE_OPERATIONS, COL_TA_SERIES),
        SqlUtils.notNull(TBL_TRADE_OPERATIONS, COL_OPERATION_DEFAULT)));
    operations = qs.getLongList(query);

    return (operations.size() == 1) ? operations.get(0) : null;
  }

  private ResponseObject getNextActNumber(RequestInfo reqInfo) {
    Long seriesId = reqInfo.getParameterLong(COL_TA_SERIES);
    String columnName = reqInfo.getParameter(Service.VAR_COLUMN);
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);

    if (!DataUtils.isId(seriesId) || BeeUtils.isEmpty(columnName) || BeeUtils.isEmpty(viewName)) {
      logger.warning("Missing one of parameter (seriesId, columnName, viewname)", seriesId,
          columnName, viewName);
      return ResponseObject.emptyResponse();
    }

    DataInfo viewData = sys.getDataInfo(viewName);

    if (viewData == null) {
      return ResponseObject.emptyResponse();
    }

    BeeColumn col = viewData.getColumn(columnName);

    if (col == null) {
      return ResponseObject.emptyResponse();
    }

    return ResponseObject.response(getNextActNumber(seriesId, col.getPrecision(), columnName));
  }

  private String getNextActNumber(long series, int maxLength, String column) {
    IsCondition where = SqlUtils.and(SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_SERIES, series),
        SqlUtils.notNull(TBL_TRADE_ACTS, column));

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_ACTS, column)
        .addFrom(TBL_TRADE_ACTS)
        .setWhere(where);

    String[] values = qs.getColumn(query);

    long max = 0;
    BigInteger bigMax = null;

    if (!ArrayUtils.isEmpty(values)) {
      for (String value : values) {
        if (BeeUtils.isDigit(value)) {
          if (BeeUtils.isLong(value)) {
            max = Math.max(max, BeeUtils.toLong(value));

          } else {
            BigInteger big = new BigInteger(value);

            if (bigMax == null || BeeUtils.isLess(bigMax, big)) {
              bigMax = big;
            }
          }
        }
      }
    }

    BigInteger big = new BigInteger(BeeUtils.toString(max));
    if (bigMax != null) {
      big = big.max(bigMax);
    }

    String number = big.add(BigInteger.ONE).toString();

    Integer length = prm.getInteger(PRM_TA_NUMBER_LENGTH);
    if (BeeUtils.isPositive(length) && length > number.length()) {
      number = BeeUtils.padLeft(number, length, BeeConst.CHAR_ZERO);
    }

    if (maxLength > 0 && number.length() > maxLength) {
      number = number.substring(number.length() - maxLength);
    }

    return number;
  }

  private ResponseObject getNextReturnActNumber(RequestInfo reqInfo) {
    Long seriesId = reqInfo.getParameterLong(COL_TA_SERIES);
    String columnName = reqInfo.getParameter(Service.VAR_COLUMN);
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    Long parentId = reqInfo.getParameterLong(COL_TA_PARENT);

    if (!DataUtils.isId(seriesId) || BeeUtils.isEmpty(columnName) || BeeUtils.isEmpty(viewName)
        || !DataUtils.isId(parentId)) {
      logger.warning("Missing one of parameter (seriesId, columnName, viewname, parentId)",
          seriesId,
          columnName, viewName, parentId);
      return ResponseObject.emptyResponse();
    }

    DataInfo viewData = sys.getDataInfo(viewName);

    if (viewData == null) {
      return ResponseObject.emptyResponse();
    }

    BeeColumn col = viewData.getColumn(columnName);

    if (col == null) {
      return ResponseObject.emptyResponse();
    }

    return ResponseObject.response(getNextChildActNumber(TradeActKind.RETURN,
        seriesId, parentId, columnName));
  }

  private ResponseObject getNextChildActNumber(RequestInfo reqInfo) {
    Long seriesId = reqInfo.getParameterLong(COL_TA_SERIES);
    String columnName = reqInfo.getParameter(Service.VAR_COLUMN);
    String viewName = reqInfo.getParameter(VAR_VIEW_NAME);
    Long parentId = reqInfo.getParameterLong(COL_TA_PARENT);
    TradeActKind kind = EnumUtils.getEnumByIndex(TradeActKind.class,
        reqInfo.getParameterInt(COL_TA_KIND));

    if (!DataUtils.isId(seriesId) || BeeUtils.isEmpty(columnName) || BeeUtils.isEmpty(viewName)
        || !DataUtils.isId(parentId) || kind == null) {
      logger.warning("Missing one of parameter (seriesId, columnName, viewname, parentId)",
          seriesId,
          columnName, viewName, parentId);
      return ResponseObject.emptyResponse();
    }

    DataInfo viewData = sys.getDataInfo(viewName);

    if (viewData == null) {
      return ResponseObject.emptyResponse();
    }

    BeeColumn col = viewData.getColumn(columnName);

    if (col == null) {
      return ResponseObject.emptyResponse();
    }

    return ResponseObject.response(getNextChildActNumber(kind,
        seriesId, parentId, columnName));
  }

  private String getNextChildActNumber(TradeActKind childKind, Long seriesId, Long parentId, String
      columnName) {
    Assert.notNull(childKind);

    TradeActKind parentKind = qs.getEnum(new SqlSelect().addFields(TBL_TRADE_ACTS, COL_TA_KIND)
      .addFrom(TBL_TRADE_ACTS).setWhere(sys.idEquals(TBL_TRADE_ACTS, parentId)), TradeActKind.class);

    String parentTbl = TradeActKind.CONTINUOUS.equals(childKind)
        || (TradeActKind.RETURN.equals(childKind) && !TradeActKind.RENT_PROJECT.equals(parentKind))
            ? TBL_TRADE_ACT_ITEMS : TBL_TRADE_ACTS;

    IsCondition where =
        SqlUtils.and(SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_SERIES, seriesId),
            SqlUtils.notNull(TBL_TRADE_ACTS, columnName),
            SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_KIND, childKind.ordinal()),
            SqlUtils.equals(parentTbl, COL_TA_PARENT, parentId));

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_ACTS, columnName)
        .addFrom(TBL_TRADE_ACTS)
        .setWhere(where);

    if (!BeeUtils.same(parentTbl, TBL_TRADE_ACTS)) {
      query.addFromLeft(parentTbl, sys.joinTables(TBL_TRADE_ACTS, parentTbl, COL_TRADE_ACT));
      query.setDistinctMode(true);
    }

    String[] values = qs.getColumn(query);

    long max = 0;
    BigInteger bigMax = null;

    if (!ArrayUtils.isEmpty(values)) {
      for (String value : values) {
        String newValue = value.substring(value.lastIndexOf("-") + 1);
        if (BeeUtils.isDigit(newValue)) {
          if (BeeUtils.isLong(newValue)) {
            max = Math.max(max, BeeUtils.toLong(newValue));

          } else {
            BigInteger big = new BigInteger(newValue);

            if (bigMax == null || BeeUtils.isLess(bigMax, big)) {
              bigMax = big;
            }
          }
        }
      }
    }

    BigInteger big = new BigInteger(BeeUtils.toString(max));
    if (bigMax != null) {
      big = big.max(bigMax);
    }

     return big.add(BigInteger.ONE).toString();
  }

  private ResponseObject getActsForInvoice(RequestInfo reqInfo) {
    String dFrom = reqInfo.getParameter(COL_TA_SERVICE_FROM);
    String dTo = reqInfo.getParameter(COL_TA_SERVICE_TO);

    Long company = reqInfo.getParameterLong(COL_TA_COMPANY);
    List<Long> actId = DataUtils.parseIdList(reqInfo.getParameter(COL_TA_ACT));

    Long object = reqInfo.getParameterLong(COL_OBJECT);
    Long serviceObject = reqInfo.getParameterLong(COL_SERVICE_OBJECT);
    Long manager = reqInfo.getParameterLong(COL_TA_MANAGER);
    Long trSeries = reqInfo.getParameterLong(COL_TA_SERIES);

    LongValue timeFrom =
        BeeUtils.isDigit(dFrom) ? new LongValue(daysToTime(BeeUtils.toInt(dFrom))) : null;
    LongValue timeTo =
        BeeUtils.isDigit(dTo) ? new LongValue(daysToTime(BeeUtils.toInt(dTo))) : null;

    CompoundFilter filter = Filter.and();
    Filter svcObjectFilter = Filter.and();
    filter.add(TradeActKind.getFilterForInvoiceBuilder());

    filter.add(Filter.or(Filter.isNull(COL_TA_STATUS), Filter.notNull(COL_STATUS_ACTIVE)));

    if (timeFrom != null) {
      filter.add(Filter.or(Filter.isNull(COL_TA_UNTIL), Filter.isMore(COL_TA_UNTIL, timeFrom)));
    }
    if (timeTo != null) {
      filter.add(Filter.isLess(COL_TA_DATE, timeTo));
    }

    if (DataUtils.isId(company)) {
      filter.add(Filter.equals(COL_TA_COMPANY, company));
    }

    if (DataUtils.isId(object)) {
      filter.add(Filter.equals(COL_OBJECT, object));
    }

    if (DataUtils.isId(manager)) {
      filter.add(Filter.equals(COL_TA_MANAGER, manager));
    }

    if (DataUtils.isId(trSeries)) {
      filter.add(Filter.equals(COL_TA_SERIES, trSeries));
    }

    if (!BeeUtils.isEmpty(actId)) {
      Filter flt = Filter.idIn(actId);
      Set<Long> objects = qs.getDistinctLongs(TBL_TRADE_ACTS, COL_OBJECT,
          SqlUtils.and(sys.idInList(TBL_TRADE_ACTS, actId),
              SqlUtils.notNull(TBL_TRADE_ACTS, COL_OBJECT)));

      if (!BeeUtils.isEmpty(objects)) {
        flt = Filter.or(flt, Filter.any(COL_OBJECT, objects));
      }
      filter.add(flt);
    }

    if (DataUtils.isId(serviceObject)) {
      svcObjectFilter = Filter.equals(COL_SERVICE_OBJECT, serviceObject);
    }

    filter.add(Filter.in(sys.getIdName(VIEW_TRADE_ACTS), VIEW_TRADE_ACT_SERVICES, COL_TRADE_ACT, svcObjectFilter));

    BeeRowSet acts = qs.getViewData(VIEW_TRADE_ACTS, filter);
    if (DataUtils.isEmpty(acts)) {
      return ResponseObject.emptyResponse();
    }

    for (BeeRow act : acts) {
      double total = totalActItems(act.getId());

      if (BeeUtils.isPositive(total)) {
        act.setProperty(PRP_ITEM_TOTAL, BeeUtils.toString(total, 2));
      }
    }

    Double vatPercent = prm.getDouble(AdministrationConstants.PRM_VAT_PERCENT);
    if (BeeUtils.isPositive(vatPercent)) {
      acts.setTableProperty(AdministrationConstants.PRM_VAT_PERCENT, vatPercent.toString());
    }

    return ResponseObject.response(acts);
  }

  private ResponseObject getServicesForInvoice(RequestInfo reqInfo) {
    Set<Long> actIds = DataUtils.parseIdSet(reqInfo.getParameter(COL_TRADE_ACT));

    if (actIds.isEmpty()) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TRADE_ACT);
    }
    BeeRowSet services = getServicesForInvoice(actIds);

    if (DataUtils.isEmpty(services)) {
      return ResponseObject.emptyResponse();
    }

    return ResponseObject.response(services);
  }

  private BeeRowSet getServicesForInvoice(Set<Long> actIds) {

    Filter filter = Filter.any(COL_TRADE_ACT, actIds);

    BeeRowSet services = qs.getViewData(VIEW_TRADE_ACT_SERVICES, filter);

    if (services.isEmpty()) {
      return services;
    }

    SqlSelect invoiceQuery = new SqlSelect()
        .addFields(TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_FROM, COL_TA_INVOICE_TO)
        .addFrom(TBL_TRADE_ACT_INVOICES)
        .addOrder(TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_FROM, COL_TA_INVOICE_TO);

    for (BeeRow service : services) {
      invoiceQuery.setWhere(SqlUtils.equals(TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_SERVICE,
          service.getId()));

      SimpleRowSet invoiceData = qs.getData(invoiceQuery);

      if (!DataUtils.isEmpty(invoiceData)) {
        List<Integer> invoiceRanges = new ArrayList<>();

        for (SimpleRow row : invoiceData) {
          JustDate from = row.getDate(COL_TA_INVOICE_FROM);
          JustDate to = row.getDate(COL_TA_INVOICE_TO);

          if (from != null && to != null && BeeUtils.isMeq(to, from)) {
            invoiceRanges.add(from.getDays());
            invoiceRanges.add(to.getDays());
          }
        }

        if (!invoiceRanges.isEmpty()) {
          service.setProperty(PRP_INVOICE_PERIODS, BeeUtils.joinInts(invoiceRanges));
        }
      }
    }

    return services;
  }

  private ResponseObject getItemsByCompanyReport(RequestInfo reqInfo) {
    Long startDate = reqInfo.getParameterLong(Service.VAR_FROM);
    Long endDate = reqInfo.getParameterLong(Service.VAR_TO);

    Long currency = reqInfo.getParameterLong(COL_TA_CURRENCY);

    Set<Long> companies = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_COMPANY));
    Set<Long> objects = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_OBJECT));

    Set<Long> operations = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_OPERATION));
    Set<Long> statuses = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_STATUS));

    Set<Long> series = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_SERIES));
    Set<Long> managers = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_MANAGER));

    Set<Long> warehouses = DataUtils.parseIdSet(reqInfo.getParameter(COL_WAREHOUSE));

    Set<Long> categories = DataUtils.parseIdSet(reqInfo.getParameter(COL_CATEGORY));
    Set<Long> items = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_ITEM));

    List<String> groupBy = NameUtils.toList(reqInfo.getParameter(Service.VAR_GROUP_BY));

    HasConditions where = SqlUtils.and();
    where.add(SqlUtils.or(
        SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_KIND, TradeActKind.SALE.ordinal()),
        SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_KIND, TradeActKind.SUPPLEMENT.ordinal())));

    if (startDate != null) {
      where.add(SqlUtils.moreEqual(TBL_TRADE_ACTS, COL_TA_DATE, startDate));
    }
    if (endDate != null) {
      where.add(SqlUtils.less(TBL_TRADE_ACTS, COL_TA_DATE, endDate));
    }

    if (!companies.isEmpty()) {
      where.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_COMPANY, companies));
    }
    if (!objects.isEmpty()) {
      where.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_OBJECT, objects));
    }

    if (!operations.isEmpty()) {
      where.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_OPERATION, operations));
    }
    if (!statuses.isEmpty()) {
      where.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_STATUS, statuses));
    }

    if (!series.isEmpty()) {
      where.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_SERIES, series));
    }
    if (!managers.isEmpty()) {
      where.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_MANAGER, managers));
    }

    if (!warehouses.isEmpty()) {
      where.add(SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_TO, warehouses));
    }

    if (!categories.isEmpty()) {
      where.add(SqlUtils.in(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, TBL_ITEM_CATEGORIES, COL_ITEM,
          SqlUtils.inList(TBL_ITEM_CATEGORIES, COL_CATEGORY, categories)));
    }
    if (!items.isEmpty()) {
      where.add(SqlUtils.inList(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, items));
    }

    SqlSelect returnQuery = new SqlSelect()
        .addFields(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, COL_TA_PARENT)
        .addSum(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY, TradeConstants.ALS_RETURNED_QTY)
        .addFrom(TBL_TRADE_ACTS)
        .addFromInner(TBL_TRADE_ACT_ITEMS,
            sys.joinTables(TBL_TRADE_ACTS, TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT))
        .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_TRADE_ACT_ITEMS, COL_TA_PARENT),
            SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_KIND, TradeActKind.RETURN.ordinal())))
        .addGroup(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, COL_TA_PARENT);

    String returnAlias = "Ret_" + SqlUtils.uniqueName();

    SqlSelect query = new SqlSelect();

    query.addFrom(TBL_TRADE_ACTS);
    query.addFromLeft(TBL_TRADE_OPERATIONS,
        sys.joinTables(TBL_TRADE_OPERATIONS, TBL_TRADE_ACTS, COL_TA_OPERATION));
    query.addFromLeft(TBL_TRADE_ACT_ITEMS,
        sys.joinTables(TBL_TRADE_ACTS, TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT));

    query.addFromLeft(returnQuery, returnAlias, SqlUtils.and(
        SqlUtils.join(TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT, returnAlias, COL_TA_PARENT),
        SqlUtils.join(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, returnAlias, COL_TA_ITEM)));

    if (groupBy.isEmpty()) {
      query.addFromLeft(TBL_TRADE_ACT_NAMES,
          sys.joinTables(TBL_TRADE_ACT_NAMES, TBL_TRADE_ACTS, COL_TA_NAME));
      query.addFromLeft(TBL_TRADE_SERIES,
          sys.joinTables(TBL_TRADE_SERIES, TBL_TRADE_ACTS, COL_TA_SERIES));
      query.addFromLeft(TBL_TRADE_STATUSES,
          sys.joinTables(TBL_TRADE_STATUSES, TBL_TRADE_ACTS, COL_TA_STATUS));

      query.addFields(TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT);
      query.addFields(TBL_TRADE_ACT_NAMES, COL_TRADE_ACT_NAME);
      query.addFields(TBL_TRADE_ACTS, COL_TA_DATE);
      query.addFields(TBL_TRADE_SERIES, COL_SERIES_NAME);
      query.addFields(TBL_TRADE_ACTS, COL_TA_NUMBER);
      query.addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_NAME);
      query.addFields(TBL_TRADE_STATUSES, COL_STATUS_NAME);
    }

    if (groupBy.isEmpty() || groupBy.contains(COL_TA_COMPANY)) {
      query.addFromLeft(TBL_COMPANIES,
          sys.joinTables(TBL_COMPANIES, TBL_TRADE_ACTS, COL_TA_COMPANY));

      query.addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME);
    }

    if (groupBy.isEmpty() || groupBy.contains(COL_TA_OBJECT)) {
      query.addFromLeft(TBL_COMPANY_OBJECTS,
          sys.joinTables(TBL_COMPANY_OBJECTS, TBL_TRADE_ACTS, COL_TA_OBJECT));

      query.addFields(TBL_COMPANY_OBJECTS, COL_COMPANY_OBJECT_NAME);
    }

    if (groupBy.isEmpty() || groupBy.contains(COL_TA_MANAGER)) {
      query.addFromLeft(TBL_USERS,
          sys.joinTables(TBL_USERS, TBL_TRADE_ACTS, COL_TA_MANAGER));
      query.addFromLeft(TBL_COMPANY_PERSONS,
          sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS, COL_COMPANY_PERSON));
      query.addFromLeft(TBL_PERSONS,
          sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON));

      query.addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);
    }

    if (groupBy.isEmpty() || groupBy.contains(COL_WAREHOUSE)) {
      query.addFromLeft(TBL_WAREHOUSES,
          sys.joinTables(TBL_WAREHOUSES, TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_TO));

      query.addField(TBL_WAREHOUSES, COL_WAREHOUSE_CODE, ALS_WAREHOUSE_CODE);
    }

    if (groupBy.isEmpty() || groupBy.contains(COL_TA_ITEM)
        || groupBy.contains(COL_ITEM_TYPE) || groupBy.contains(COL_ITEM_GROUP)) {

      query.addFromLeft(TBL_ITEMS,
          sys.joinTables(TBL_ITEMS, TBL_TRADE_ACT_ITEMS, COL_TA_ITEM));
    }

    if (groupBy.contains(COL_ITEM_TYPE)) {
      query.addFromLeft(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_TYPES,
          sys.joinTables(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_TYPES, TBL_ITEMS, COL_ITEM_TYPE));

      query.addField(ALS_ITEM_TYPES, COL_CATEGORY_NAME, ALS_ITEM_TYPE_NAME);
    }

    if (groupBy.contains(COL_ITEM_GROUP)) {
      query.addFromLeft(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_GROUPS,
          sys.joinTables(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_GROUPS, TBL_ITEMS, COL_ITEM_GROUP));

      query.addField(ALS_ITEM_GROUPS, COL_CATEGORY_NAME, ALS_ITEM_GROUP_NAME);
    }

    if (groupBy.isEmpty() || groupBy.contains(COL_TA_ITEM)) {
      query.addFromLeft(TBL_UNITS,
          sys.joinTables(TBL_UNITS, TBL_ITEMS, COL_UNIT));

      query.addField(TBL_ITEMS, COL_ITEM_NAME, ALS_ITEM_NAME);
      query.addFields(TBL_ITEMS, COL_ITEM_ARTICLE);
      query.addField(TBL_UNITS, COL_UNIT_NAME, ALS_UNIT_NAME);
    }

    query.addFields(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY);
    query.addFields(returnAlias, TradeConstants.ALS_RETURNED_QTY);
    query.addField(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY, ALS_REMAINING_QTY);

    query.addFields(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_PRICE);
    query.addFields(TBL_TRADE_ACTS, COL_TA_CURRENCY);

    query.addFields(TBL_TRADE_ACT_ITEMS, COL_TRADE_DISCOUNT);

    int amountPrecision = 15;
    int amountScale = 2;

    query.addEmptyNumeric(ALS_BASE_AMOUNT, amountPrecision, amountScale);
    query.addEmptyNumeric(ALS_DISCOUNT_AMOUNT, amountPrecision, amountScale);
    query.addEmptyNumeric(ALS_TOTAL_AMOUNT, amountPrecision, amountScale);

    String itemIdName = sys.getIdName(TBL_TRADE_ACT_ITEMS);
    if (groupBy.isEmpty()) {
      query.addFields(TBL_TRADE_ACT_ITEMS, itemIdName);
    }

    query.setWhere(where);

    String tmp = qs.sqlCreateTemp(query);

    SqlUpdate update = new SqlUpdate(tmp)
        .addExpression(ALS_REMAINING_QTY,
            SqlUtils.minus(SqlUtils.field(tmp, COL_TRADE_ITEM_QUANTITY),
                SqlUtils.field(tmp, TradeConstants.ALS_RETURNED_QTY)))
        .setWhere(SqlUtils.positive(tmp, TradeConstants.ALS_RETURNED_QTY));

    qs.updateData(update);

    IsCondition condition = SqlUtils.positive(tmp, ALS_REMAINING_QTY);
    if (!qs.sqlExists(tmp, condition)) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.emptyResponse();
    }

    if (!DataUtils.isId(currency)) {
      currency = prm.getRelation(PRM_CURRENCY);
    }
    if (DataUtils.isId(currency)) {
      exchange(tmp, COL_TA_CURRENCY, currency, System.currentTimeMillis(), COL_TRADE_ITEM_PRICE);
    }

    update = new SqlUpdate(tmp)
        .addExpression(ALS_BASE_AMOUNT,
            SqlUtils.multiply(SqlUtils.field(tmp, ALS_REMAINING_QTY),
                SqlUtils.field(tmp, COL_TRADE_ITEM_PRICE)))
        .setWhere(SqlUtils.and(SqlUtils.positive(tmp, ALS_REMAINING_QTY),
            SqlUtils.positive(tmp, COL_TRADE_ITEM_PRICE)));

    qs.updateData(update);

    update = new SqlUpdate(tmp)
        .addExpression(ALS_DISCOUNT_AMOUNT,
            SqlUtils.divide(SqlUtils.multiply(SqlUtils.field(tmp, ALS_BASE_AMOUNT),
                SqlUtils.field(tmp, COL_TRADE_DISCOUNT)), 100))
        .setWhere(SqlUtils.and(SqlUtils.positive(tmp, ALS_BASE_AMOUNT),
            SqlUtils.notNull(tmp, COL_TRADE_DISCOUNT)));

    qs.updateData(update);

    update = new SqlUpdate(tmp)
        .addExpression(ALS_TOTAL_AMOUNT,
            SqlUtils.minus(SqlUtils.field(tmp, ALS_BASE_AMOUNT),
                SqlUtils.field(tmp, ALS_DISCOUNT_AMOUNT)))
        .setWhere(SqlUtils.notNull(tmp, ALS_DISCOUNT_AMOUNT));

    qs.updateData(update);

    update = new SqlUpdate(tmp)
        .addExpression(ALS_TOTAL_AMOUNT, SqlUtils.field(tmp, ALS_BASE_AMOUNT))
        .setWhere(SqlUtils.isNull(tmp, ALS_DISCOUNT_AMOUNT));

    qs.updateData(update);

    query = new SqlSelect();

    if (groupBy.isEmpty()) {
      query.addFields(tmp, COL_TRADE_ACT, COL_TRADE_ACT_NAME, COL_TA_DATE,
          COL_SERIES_NAME, COL_TA_NUMBER, COL_OPERATION_NAME,
          ALS_COMPANY_NAME, COL_COMPANY_OBJECT_NAME,
          itemIdName, ALS_ITEM_NAME, COL_ITEM_ARTICLE,
          COL_TRADE_ITEM_QUANTITY, ALS_UNIT_NAME, TradeConstants.ALS_RETURNED_QTY, ALS_REMAINING_QTY,
          COL_TRADE_ITEM_PRICE, ALS_BASE_AMOUNT, COL_TRADE_DISCOUNT, ALS_DISCOUNT_AMOUNT,
          ALS_TOTAL_AMOUNT);

      query.addOrder(tmp, COL_TA_DATE, COL_TRADE_ACT, itemIdName);

    } else {
      for (String group : groupBy) {
        List<String> fields = new ArrayList<>();
        List<String> order = new ArrayList<>();

        switch (group) {
          case COL_ITEM_TYPE:
            fields.add(ALS_ITEM_TYPE_NAME);
            break;

          case COL_ITEM_GROUP:
            fields.add(ALS_ITEM_GROUP_NAME);
            break;

          case COL_TA_ITEM:
            fields.add(ALS_ITEM_NAME);
            fields.add(COL_ITEM_ARTICLE);
            fields.add(ALS_UNIT_NAME);
            break;

          case COL_TA_COMPANY:
            fields.add(ALS_COMPANY_NAME);
            break;

          case COL_TA_OBJECT:
            fields.add(COL_COMPANY_OBJECT_NAME);
            break;

          case COL_TA_MANAGER:
            fields.add(COL_FIRST_NAME);
            fields.add(COL_LAST_NAME);

            order.add(COL_LAST_NAME);
            order.add(COL_FIRST_NAME);
            break;

          case COL_WAREHOUSE:
            fields.add(ALS_WAREHOUSE_CODE);
            break;
        }

        for (String field : fields) {
          query.addFields(tmp, field);
        }

        if (order.isEmpty()) {
          order.addAll(fields);
        }
        for (String field : order) {
          query.addGroup(tmp, field);
          query.addOrder(tmp, field);
        }
      }

      query.addSum(tmp, COL_TRADE_ITEM_QUANTITY);
      query.addSum(tmp, TradeConstants.ALS_RETURNED_QTY);
      query.addSum(tmp, ALS_REMAINING_QTY);
      query.addSum(tmp, ALS_BASE_AMOUNT);
      query.addSum(tmp, ALS_DISCOUNT_AMOUNT);
      query.addSum(tmp, ALS_TOTAL_AMOUNT);
    }

    query.addFrom(tmp);
    query.setWhere(condition);

    SimpleRowSet data = qs.getData(query);

    qs.sqlDropTemp(tmp);

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.emptyResponse();

    } else {
      if (groupBy.isEmpty()) {
        data.removeColumn(itemIdName);
      }

      return ResponseObject.response(data);
    }
  }

  private ResponseObject getItemsForReturn(RequestInfo reqInfo) {
    Long actId = reqInfo.getParameterLong(COL_TRADE_ACT);
    if (!DataUtils.isId(actId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TRADE_ACT);
    }

    BeeRowSet items = getRemainingItems(actId);
    if (DataUtils.isEmpty(items)) {
      return ResponseObject.emptyResponse();
    } else {
      BeeRowSet parentAct = qs.getViewData(VIEW_TRADE_ACTS, Filter.compareId(actId));
      if (!DataUtils.isEmpty(parentAct)) {
        items.setTableProperty(PRP_PARENT_ACT, parentAct.getRow(0).serialize());
      }
      return ResponseObject.response(items);
    }
  }

  private ResponseObject getItemsForMultiReturn(RequestInfo reqInfo) {
    List<Long> acts = DataUtils.parseIdList(reqInfo.getParameter(Service.VAR_LIST));

    if (BeeUtils.isEmpty(acts)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_LIST);
    }

    if (reqInfo.hasParameter("DEBUG")) {
      logger.debug(reqInfo.getParameter("DEBUG"), "START" ,acts.size());
    }

    BeeRowSet items = getRemainingItems(acts.toArray(new Long[acts.size()]));

    if (reqInfo.hasParameter("DEBUG")) {
      logger.debug(reqInfo.getParameter("DEBUG"), "END",acts.size());
    }

    if (DataUtils.isEmpty(items)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(items);
    }
  }

  private ResponseObject doMultiReturnActItems(RequestInfo reqInfo) {
    String input = reqInfo.getParameter(VIEW_TRADE_ACT_ITEMS); // Selected returned act items
    String idParentActs = reqInfo.getParameter(VAR_ID_LIST); // Selected act list
    Long actId = reqInfo.getParameterLong(COL_TRADE_ACT);  // Return act id
    List<String> fillCols = null;

    if (!BeeUtils.isEmpty(reqInfo.getParameter(PRP_INSERT_COLS))) {
        fillCols = Lists.newArrayList(Codec.beeDeserializeCollection(
              reqInfo.getParameter(PRP_INSERT_COLS)));
    }

    if (BeeUtils.isEmpty(input)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VIEW_TRADE_ACT_ITEMS);
    }

    if (!DataUtils.isId(actId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TRADE_ACT);
    }

    BeeRowSet parentItems = BeeRowSet.restore(input); // Selected returned act items
    List<Long> parentIds = DataUtils.parseIdList(idParentActs);  // Selected acts ID
    List<Long> fifoActs = DataUtils.getDistinct(parentItems, COL_TRADE_ACT); // Act list by ret. i.

    if (BeeUtils.isEmpty(parentIds) || BeeUtils.isEmpty(fifoActs)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VIEW_TRADE_ACT_ITEMS);
    }

    BeeRowSet parentActs = qs.getViewData(VIEW_TRADE_ACTS, Filter.idIn(parentIds));
    if (DataUtils.isEmpty(parentActs)) {
      return ResponseObject.error(reqInfo.getService(), VIEW_TRADE_ACTS, parentIds, "not found");
    }

    Long retStatus = prm.getRelation(PRM_RETURNED_ACT_STATUS);
    int statusIndex = parentActs.getColumnIndex(COL_TA_STATUS);

    BeeRow fifoAct = null;

    for (BeeRow f : parentActs) {
      if (fifoActs.contains(f.getId())) {
        fifoAct = f;
        break;
      }
    }

    if (fifoAct == null) {
      return ResponseObject.error(reqInfo.getService(), VIEW_TRADE_ACTS, "fifo act not found");
    }

    if (fillCols != null) {
      ResponseObject actUpdateResponse = updateMultiReturnAct(actId, parentActs, fifoAct, fillCols);
      if (actUpdateResponse.hasErrors()) {
        return actUpdateResponse;
      }
    }

    DateTime date = qs.getDateTime(new SqlSelect()
        .addFields(TBL_TRADE_ACTS, COL_TA_DATE)
        .addFrom(TBL_TRADE_ACTS)
        .setWhere(sys.idEquals(TBL_TRADE_ACTS, actId)));

    if (prm.hasParameter(PRM_TRADE_SERVICE_TRANSITION_TIME)) {
        TimeOfDayValue transitionTime = new TimeOfDayValue(
                TimeUtils.renderTime(prm.getTime(PRM_TRADE_SERVICE_TRANSITION_TIME), true));
        TimeOfDayValue actTime = new TimeOfDayValue(date);

        if (actTime.compareTo(transitionTime) > 0) {
            date = TimeUtils.nextDay(date).getDateTime();
        }
    }

    for (BeeRow parentAct : parentActs) {
      long parentId = parentAct.getId();

      if (!fifoActs.contains(parentId)) {
        continue;
      }

      BeeRowSet returnedItems = new BeeRowSet(parentItems.getColumns());
      for (int i = 0; i < parentItems.getNumberOfRows(); i++) {
        if (Objects.equals(parentItems.getLong(i, COL_TRADE_ACT), parentId)) {
          returnedItems.addRow(DataUtils.cloneRow(parentItems.getRow(i)));
        }
      }

      ResponseObject childResponse = insertChildItems(actId, returnedItems);

      if (childResponse.hasErrors()) {
        return childResponse;
      }

      boolean hasItems = !DataUtils.isEmpty(getRemainingItems(parentId));

      /* Closing all services, then all items are returned and never add to CTA */
      if (!hasItems) {
        String svcAls = SqlUtils.uniqueName();
        ResponseObject updSvc = qs.updateDataWithResponse(
            new SqlUpdate(TBL_TRADE_ACT_SERVICES)
                .addConstant(COL_TA_SERVICE_TO, date)
                .setWhere(SqlUtils.and(
                    SqlUtils.equals(TBL_TRADE_ACT_SERVICES, COL_TRADE_ACT, parentId),
                    SqlUtils.isNull(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_TO),
                    SqlUtils.in(TBL_TRADE_ACT_SERVICES, COL_ITEM,
                        new SqlSelect()
                            .addField(svcAls, COL_ITEM, svcAls + COL_ITEM)
                            .addFrom(TBL_TRADE_ACT_SERVICES, svcAls)
                            .addFromLeft(TBL_ITEMS, sys.joinTables(TBL_ITEMS, svcAls, COL_ITEM))
                            .setWhere(
                                SqlUtils.and(
                                    SqlUtils.notNull(TBL_ITEMS, COL_TIME_UNIT),
                                    SqlUtils.equals(svcAls, COL_TRADE_ACT, parentId)
                                    )
                            ))
                )));

        if (updSvc.hasErrors()) {
          return updSvc;
        }

        SqlUpdate updAct = new SqlUpdate(TBL_TRADE_ACTS)
            .addConstant(COL_TA_UNTIL, date)
            .setWhere(sys.idEquals(TBL_TRADE_ACTS, parentId));

        if (DataUtils.isId(retStatus) && !retStatus.equals(parentAct.getLong(statusIndex))) {
           updAct.addConstant(COL_TA_STATUS, retStatus);
        }

        updSvc = qs.updateDataWithResponse(updAct);

        if (updSvc.hasErrors()) {
          return updSvc;
        }
      }
    }

    Set<Long> lockActData = getRelatedLockActs(parentIds, actId);
    ResponseObject relResp = createLockRelations(lockActData, actId, COL_TA_RETURN);

    if (relResp.hasErrors()) {
      return relResp;
    }

    if (DataUtils.isId(fifoAct.getLong(parentActs.getColumnIndex(COL_TA_RENT_PROJECT)))) {
      return updateRentProjectAct(parentActs, fifoAct.getLong(parentActs.getColumnIndex(COL_TA_RENT_PROJECT)), date);
    }


    return createContinuousAct(parentActs, fifoAct.getId(), fifoActs ,date);
  }

  private ResponseObject getItemsForSelection(RequestInfo reqInfo) {
    TradeActKind kind = EnumUtils.getEnumByIndex(TradeActKind.class,
        reqInfo.getParameter(COL_TA_KIND));

    if (kind == null) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TA_KIND);
    }

    Long actId = reqInfo.getParameterLong(COL_TRADE_ACT);
    Long warehouse = reqInfo.getParameterLong(COL_WAREHOUSE);
    String source = reqInfo.getParameter(Service.VAR_TABLE);

    Assert.notEmpty(source);

    String where = reqInfo.getParameter(Service.VAR_VIEW_WHERE);

    CompoundFilter filter = Filter.and();

    Set<Long> actItems =
        BeeUtils.same(source, TBL_TRADE_ACT_SERVICES) ? new HashSet<>()  : getActItems(actId);
    if (!actItems.isEmpty()) {
      filter.add(Filter.idNotIn(actItems));
    }

    if (!BeeUtils.isEmpty(where)) {
      filter.add(Filter.restore(where));
    }

    BeeRowSet items = qs.getViewData(VIEW_ITEMS, filter, Order.ascending(COL_ITEM_ORDINAL));
    if (DataUtils.isEmpty(items)) {
      logger.debug(reqInfo.getService(), "no items found", filter);
      return ResponseObject.emptyResponse();
    }

    if (kind.showStock()) {
      List<Long> itemIds = (items.getNumberOfRows() < 200)
          ? items.getRowIds() : BeeConst.EMPTY_IMMUTABLE_LONG_LIST;
      Table<Long, Long, Double> stock = getStock(itemIds);

      if (stock != null) {
        int scale = sys.getFieldScale(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY);

        List<BeeRow> hasOrdering = new ArrayList<>();
        List<BeeRow> noOrdering = new ArrayList<>();
        int idxOrdinal = items.getColumnIndex(COL_ITEM_ORDINAL);

        for (BeeRow row : items) {
          if (stock.containsRow(row.getId())) {
            for (Map.Entry<Long, Double> entry : stock.row(row.getId()).entrySet()) {
              row.setProperty(PRP_WAREHOUSE_PREFIX + BeeUtils.toString(entry.getKey()),
                  BeeUtils.toString(entry.getValue(), scale));
            }
          }
          if (row.isNull(idxOrdinal)) {
            noOrdering.add(row);
          } else {
            hasOrdering.add(row);
          }
        }
        String prop = Objects.isNull(warehouse) ? null
            : PRP_WAREHOUSE_PREFIX + BeeUtils.toString(warehouse);

        if (!noOrdering.isEmpty() && Objects.nonNull(prop)) {
          noOrdering.sort((o1, o2) -> {
            boolean s1 = BeeUtils.isPositive(o1.getPropertyDouble(prop));
            boolean s2 = BeeUtils.isPositive(o2.getPropertyDouble(prop));

            int res = Objects.compare(s1, s2, Comparator.reverseOrder());

            if (res == 0) {
              res = Objects.compare(o1.getId(), o2.getId(), Comparator.reverseOrder());
            }
            return res;
          });
          items.clearRows();
          items.addRows(hasOrdering);
          items.addRows(noOrdering);
        }

        items.setTableProperty(TBL_WAREHOUSES, DataUtils.buildIdList(stock.columnKeySet()));
      }
    }

    return ResponseObject.response(items);
  }

  private Set<Long> getRelatedLockActs(Collection parentIds, Long relId) {
    Set<Long> relLockActs = new HashSet<>();

    SimpleRowSet relData = qs.getData(new SqlSelect()
        .addFields(TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT)
        .addFields(TBL_TRADE_ACTS, COL_TA_RETURN, COL_TA_CONTINUOUS)
        .addFrom(TBL_TRADE_ACT_ITEMS)
        .addFromInner(TBL_TRADE_ACTS, sys.joinTables(TBL_TRADE_ACTS, TBL_TRADE_ACT_ITEMS,
            COL_TA_PARENT))
        .setWhere(SqlUtils.and(
            sys.idInList(TBL_TRADE_ACTS, parentIds),
            SqlUtils.or(
                SqlUtils.notNull(TBL_TRADE_ACTS, COL_TA_RETURN),
                SqlUtils.notNull(TBL_TRADE_ACTS, COL_TA_CONTINUOUS)
            )
        )));

    relLockActs.addAll(parentIds);
    relLockActs.addAll(Lists.newArrayList(relData.getLongColumn(COL_TRADE_ACT)));
    relLockActs.addAll(Lists.newArrayList(relData.getLongColumn(COL_TA_RETURN)));
    relLockActs.addAll(Lists.newArrayList(relData.getLongColumn(COL_TA_CONTINUOUS)));
    relLockActs.remove(null);
    relLockActs.remove(relId);

    return relLockActs;
  }

  private BeeRowSet getRemainingItems(Long... actId) {
    Filter filter = Filter.and(Filter.any(COL_TRADE_ACT, Lists.newArrayList(actId)),
        Filter.isPositive(COL_TRADE_ITEM_QUANTITY));

    BeeRowSet parentItems =
        qs.getViewData(VIEW_TRADE_ACT_ITEMS, filter, Order.ascending(ALS_ITEM_ORDINAL, COL_TA_ITEM,
            COL_TA_DATE));
    if (DataUtils.isEmpty(parentItems)) {
      return null;
    }

    Map<Pair<Long, Long>, Double> returnedItems = getReturnedItems(actId);
    Map<Long, Double> overallTotal = Maps.newLinkedHashMap();

    BeeRowSet result = new BeeRowSet(parentItems.getViewName(), parentItems.getColumns());

    int actIndex = parentItems.getColumnIndex(COL_TRADE_ACT);
    int itemIndex = parentItems.getColumnIndex(COL_TA_ITEM);
    int qtyIndex = parentItems.getColumnIndex(COL_TRADE_ITEM_QUANTITY);

    for (BeeRow parentRow : parentItems) {
      Long parentRowAct = parentRow.getLong(actIndex);
      Long parentRowItem = parentRow.getLong(itemIndex);
      double qty = BeeUtils.unbox(parentRow.getDouble(qtyIndex));
      Double returnedQty = BeeConst.DOUBLE_ZERO;

      if (!BeeUtils.isEmpty(returnedItems)) {
        returnedQty = returnedItems.get(Pair.of(parentRowAct,
            parentRowItem));
      }

      boolean found = BeeUtils.isPositive(returnedQty);
      if (found) {
        qty -= returnedQty;
      }

      if (BeeUtils.isPositive(qty)) {
        overallTotal.put(parentRowItem, BeeUtils.unbox(overallTotal.get(parentRowItem)) + qty);
        BeeRow row = DataUtils.cloneRow(parentRow);
        if (found) {
          row.setValue(qtyIndex, qty);
        }

        result.addRow(row);
      }

      if (BeeUtils.isDouble(returnedQty)) {
        returnedItems.put(Pair.of(parentRowAct, parentRowItem),
            returnedQty - BeeUtils.unbox(parentRow.getDouble(qtyIndex)));
      }
    }

    for (BeeRow res : result) {
      Long resItem = res.getLong(itemIndex);
      if (overallTotal.containsKey(resItem)) {
        res.setProperty(PROP_OVERALL_TOTAL,
            BeeUtils.toString(overallTotal.get(resItem)));
        overallTotal.remove(resItem);
      }
    }

    if (DataUtils.isEmpty(result)) {
      return null;
    } else {
      return result;
    }
  }

  private Map<Pair<Long, Long>, Double> getReturnedItems(Long... actId) {
    Map<Pair<Long, Long>, Double> result = new HashMap<>();

    SqlSelect query =
        new SqlSelect()
            .addFields(TBL_TRADE_ACT_ITEMS, COL_TA_PARENT, COL_TA_ITEM)
            .addSum(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY)
            .addFrom(TBL_TRADE_ACTS)
            .addFromInner(TBL_TRADE_ACT_ITEMS,
                sys.joinTables(TBL_TRADE_ACTS, TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT))
            .setWhere(
                SqlUtils.and(SqlUtils.inList(TBL_TRADE_ACT_ITEMS, COL_TA_PARENT, Lists
                    .newArrayList(actId)),
                    SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_KIND, TradeActKind.RETURN.ordinal())))
            .addGroup(TBL_TRADE_ACT_ITEMS, COL_TA_PARENT, COL_TA_ITEM);

    SimpleRowSet data = qs.getData(query);
    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        Long act = row.getLong(COL_TA_PARENT);
        Long item = row.getLong(COL_TA_ITEM);
        Double qty = row.getDouble(COL_TRADE_ITEM_QUANTITY);

        if (DataUtils.isId(act) && DataUtils.isId(item) && BeeUtils.isPositive(qty)) {
          result.put(Pair.of(act, item), qty);
        }
      }
    }

    return result;
  }

  private ResponseObject getServicesReport(RequestInfo reqInfo) {
    Long startDate = reqInfo.getParameterLong(Service.VAR_FROM);
    Long endDate = reqInfo.getParameterLong(Service.VAR_TO);

    Long currency = reqInfo.getParameterLong(COL_TA_CURRENCY);

    Set<Long> companies = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_COMPANY));
    Set<Long> managers = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_MANAGER));

    Set<Long> categories = DataUtils.parseIdSet(reqInfo.getParameter(COL_CATEGORY));
    Set<Long> items = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_ITEM));

    Set<Long> suppliers = DataUtils.parseIdSet(reqInfo.getParameter(COL_TRADE_SUPPLIER));

    List<String> groupBy = NameUtils.toList(reqInfo.getParameter(Service.VAR_GROUP_BY));

    SqlSelect rangeQuery = new SqlSelect()
        .addFields(TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_ITEM)
        .addMin(TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_FROM)
        .addMax(TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_TO)
        .addMax(TBL_TRADE_ACT_SERVICES, COL_TRADE_SUPPLIER)
        .addMax(TBL_COMPANIES, COL_COMPANY_NAME, ALS_SUPPLIER_NAME)
        .addMax(TBL_TRADE_ACT_SERVICES, COL_COST_AMOUNT)
        .addFrom(TBL_TRADE_ACT_INVOICES)
        .addFromLeft(TBL_TRADE_ACT_SERVICES,
            sys.joinTables(TBL_TRADE_ACT_SERVICES, TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_SERVICE))
        .addFromLeft(TBL_COMPANIES,
            sys.joinTables(TBL_COMPANIES, TBL_TRADE_ACT_SERVICES, COL_TRADE_SUPPLIER))
        .addGroup(TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_ITEM);

    String rangeAlias = "rng_" + SqlUtils.uniqueName();

    HasConditions where = SqlUtils.and(SqlUtils.notNull(rangeAlias, COL_TA_INVOICE_FROM),
        SqlUtils.notNull(rangeAlias, COL_TA_INVOICE_TO));

    if (!suppliers.isEmpty()) {
      where.add(SqlUtils.inList(rangeAlias, COL_TRADE_SUPPLIER, suppliers));
    }
    if (startDate != null) {
      where.add(SqlUtils.moreEqual(TBL_SALES, COL_TRADE_DATE, startDate));
    }
    if (endDate != null) {
      where.add(SqlUtils.less(TBL_SALES, COL_TRADE_DATE, endDate));
    }

    if (!companies.isEmpty()) {
      where.add(SqlUtils.inList(TBL_SALES, COL_TRADE_CUSTOMER, companies));
    }
    if (!managers.isEmpty()) {
      where.add(SqlUtils.inList(TBL_SALES, COL_TRADE_MANAGER, managers));
    }

    if (!categories.isEmpty()) {
      where.add(SqlUtils.in(TBL_SALE_ITEMS, COL_ITEM, TBL_ITEM_CATEGORIES, COL_ITEM,
          SqlUtils.inList(TBL_ITEM_CATEGORIES, COL_CATEGORY, categories)));
    }
    if (!items.isEmpty()) {
      where.add(SqlUtils.inList(TBL_SALE_ITEMS, COL_ITEM, items));
    }

    SqlSelect query = new SqlSelect();

    query.addFrom(TBL_SALES);
    query.addFromLeft(TBL_SALE_ITEMS,
        sys.joinTables(TBL_SALES, TBL_SALE_ITEMS, COL_SALE));
    query.addFromLeft(rangeQuery, rangeAlias,
        SqlUtils.join(TBL_SALE_ITEMS, sys.getIdName(TBL_SALE_ITEMS),
            rangeAlias, COL_TA_INVOICE_ITEM));

    if (groupBy.isEmpty()) {
      query.addFromLeft(TBL_SALES_SERIES,
          sys.joinTables(TBL_SALES_SERIES, TBL_SALES, COL_TRADE_SALE_SERIES));

      query.addFields(TBL_SALE_ITEMS, COL_SALE);
      query.addFields(TBL_SALES, COL_TRADE_DATE);
      query.addField(TBL_SALES_SERIES, COL_SERIES_NAME, COL_TRADE_INVOICE_PREFIX);
      query.addFields(TBL_SALES, COL_TRADE_INVOICE_NO);
      query.addFields(rangeAlias, COL_TA_INVOICE_FROM, COL_TA_INVOICE_TO);
    }

    if (groupBy.isEmpty() || groupBy.contains(COL_TA_COMPANY)) {
      query.addFromLeft(TBL_COMPANIES,
          sys.joinTables(TBL_COMPANIES, TBL_SALES, COL_TRADE_CUSTOMER));

      query.addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME);
    }

    if (groupBy.isEmpty() || groupBy.contains(COL_TA_MANAGER)) {
      query.addFromLeft(TBL_USERS,
          sys.joinTables(TBL_USERS, TBL_SALES, COL_TRADE_MANAGER));
      query.addFromLeft(TBL_COMPANY_PERSONS,
          sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS, COL_COMPANY_PERSON));
      query.addFromLeft(TBL_PERSONS,
          sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON));

      query.addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);
    }

    if (groupBy.isEmpty() || groupBy.contains(COL_TA_ITEM)
        || groupBy.contains(COL_ITEM_TYPE) || groupBy.contains(COL_ITEM_GROUP)) {

      query.addFromLeft(TBL_ITEMS,
          sys.joinTables(TBL_ITEMS, TBL_SALE_ITEMS, COL_ITEM));
    }

    if (groupBy.contains(COL_ITEM_TYPE)) {
      query.addFromLeft(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_TYPES,
          sys.joinTables(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_TYPES, TBL_ITEMS, COL_ITEM_TYPE));

      query.addField(ALS_ITEM_TYPES, COL_CATEGORY_NAME, ALS_ITEM_TYPE_NAME);
    }

    if (groupBy.contains(COL_ITEM_GROUP)) {
      query.addFromLeft(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_GROUPS,
          sys.joinTables(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_GROUPS, TBL_ITEMS, COL_ITEM_GROUP));

      query.addField(ALS_ITEM_GROUPS, COL_CATEGORY_NAME, ALS_ITEM_GROUP_NAME);
    }

    if (groupBy.isEmpty() || groupBy.contains(COL_TA_ITEM)) {
      query.addFromLeft(TBL_UNITS,
          sys.joinTables(TBL_UNITS, TBL_ITEMS, COL_UNIT));

      query.addField(TBL_ITEMS, COL_ITEM_NAME, ALS_ITEM_NAME);
      query.addFields(TBL_SALE_ITEMS, COL_TRADE_ITEM_ARTICLE);
      query.addField(TBL_UNITS, COL_UNIT_NAME, ALS_UNIT_NAME);
      query.addFields(rangeAlias, ALS_SUPPLIER_NAME, COL_COST_AMOUNT);
    }

    query.addFields(TBL_SALE_ITEMS, COL_TRADE_ITEM_QUANTITY, COL_TA_SERVICE_FACTOR);
    query.addExpr(SqlUtils.multiply(SqlUtils.field(TBL_SALE_ITEMS, COL_TRADE_ITEM_PRICE),
      SqlUtils.field(TBL_SALE_ITEMS, COL_TA_SERVICE_FACTOR)), COL_TRADE_ITEM_PRICE);

    query.addFields(TBL_SALES, COL_TRADE_CURRENCY);

    query.addFields(TBL_SALE_ITEMS, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC);

    int amountPrecision = 15;
    int amountScale = 2;

    query.addEmptyNumeric(ALS_WITHOUT_VAT, amountPrecision, amountScale);
    query.addEmptyNumeric(ALS_VAT_AMOUNT, amountPrecision, amountScale);
    query.addEmptyNumeric(ALS_TOTAL_AMOUNT, amountPrecision, amountScale);

    String itemIdName = sys.getIdName(TBL_SALE_ITEMS);
    if (groupBy.isEmpty()) {
      query.addFields(TBL_SALE_ITEMS, itemIdName);
    }

    query.setWhere(where);

    String tmp = qs.sqlCreateTemp(query);
    if (qs.isEmpty(tmp)) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.emptyResponse();
    }

    SqlUpdate update = new SqlUpdate(tmp)
        .addExpression(ALS_WITHOUT_VAT,
            SqlUtils.multiply(SqlUtils.field(tmp, COL_TRADE_ITEM_QUANTITY),
                SqlUtils.field(tmp, COL_TRADE_ITEM_PRICE)))
        .setWhere(SqlUtils.and(SqlUtils.positive(tmp, COL_TRADE_ITEM_QUANTITY),
            SqlUtils.positive(tmp, COL_TRADE_ITEM_PRICE)));
    qs.updateData(update);

    update = new SqlUpdate(tmp)
        .addExpression(ALS_VAT_AMOUNT,
            SqlUtils.divide(SqlUtils.multiply(SqlUtils.field(tmp, ALS_WITHOUT_VAT),
                SqlUtils.field(tmp, COL_TRADE_VAT)), 100))
        .setWhere(SqlUtils.and(SqlUtils.positive(tmp, ALS_WITHOUT_VAT),
            SqlUtils.notNull(tmp, COL_TRADE_VAT), SqlUtils.notNull(tmp, COL_TRADE_VAT_PERC),
            SqlUtils.notNull(tmp, COL_TRADE_VAT_PLUS)));
    qs.updateData(update);

    update = new SqlUpdate(tmp)
        .addExpression(ALS_VAT_AMOUNT,
            SqlUtils.divide(
                SqlUtils.multiply(SqlUtils.field(tmp, ALS_WITHOUT_VAT),
                    SqlUtils.field(tmp, COL_TRADE_VAT)),
                SqlUtils.plus(SqlUtils.field(tmp, COL_TRADE_VAT), 100)))
        .setWhere(SqlUtils.and(SqlUtils.positive(tmp, ALS_WITHOUT_VAT),
            SqlUtils.notNull(tmp, COL_TRADE_VAT), SqlUtils.notNull(tmp, COL_TRADE_VAT_PERC),
            SqlUtils.isNull(tmp, COL_TRADE_VAT_PLUS)));
    qs.updateData(update);

    update = new SqlUpdate(tmp)
        .addExpression(ALS_VAT_AMOUNT, SqlUtils.field(tmp, COL_TRADE_VAT))
        .setWhere(SqlUtils.and(SqlUtils.notNull(tmp, COL_TRADE_VAT),
            SqlUtils.isNull(tmp, COL_TRADE_VAT_PERC)));
    qs.updateData(update);

    if (!DataUtils.isId(currency)) {
      currency = prm.getRelation(PRM_CURRENCY);
    }
    if (DataUtils.isId(currency)) {
      exchange(tmp, COL_TRADE_CURRENCY, currency, System.currentTimeMillis(),
          COL_TRADE_ITEM_PRICE, ALS_WITHOUT_VAT, ALS_VAT_AMOUNT);
    }

    update = new SqlUpdate(tmp)
        .addExpression(ALS_TOTAL_AMOUNT,
            SqlUtils.plus(SqlUtils.field(tmp, ALS_WITHOUT_VAT),
                SqlUtils.field(tmp, ALS_VAT_AMOUNT)))
        .setWhere(SqlUtils.and(SqlUtils.notNull(tmp, ALS_VAT_AMOUNT),
            SqlUtils.notNull(tmp, COL_TRADE_VAT_PLUS)));
    qs.updateData(update);

    update = new SqlUpdate(tmp)
        .addExpression(ALS_TOTAL_AMOUNT, SqlUtils.field(tmp, ALS_WITHOUT_VAT))
        .setWhere(SqlUtils.or(SqlUtils.isNull(tmp, ALS_VAT_AMOUNT),
            SqlUtils.isNull(tmp, COL_TRADE_VAT_PLUS)));
    qs.updateData(update);

    update = new SqlUpdate(tmp)
        .addExpression(ALS_WITHOUT_VAT,
            SqlUtils.minus(SqlUtils.field(tmp, ALS_TOTAL_AMOUNT),
                SqlUtils.field(tmp, ALS_VAT_AMOUNT)))
        .setWhere(SqlUtils.and(SqlUtils.notNull(tmp, ALS_VAT_AMOUNT),
            SqlUtils.isNull(tmp, COL_TRADE_VAT_PLUS)));
    qs.updateData(update);

    query = new SqlSelect();

    if (groupBy.isEmpty()) {
      query.addFields(tmp, COL_SALE, COL_TRADE_DATE,
          COL_TRADE_INVOICE_PREFIX, COL_TRADE_INVOICE_NO, ALS_COMPANY_NAME,
          COL_TA_INVOICE_FROM, COL_TA_INVOICE_TO,
          itemIdName, ALS_ITEM_NAME, COL_TRADE_ITEM_ARTICLE,
          ALS_SUPPLIER_NAME, COL_COST_AMOUNT,
          COL_TRADE_ITEM_QUANTITY, ALS_UNIT_NAME, COL_TRADE_ITEM_PRICE,
          ALS_WITHOUT_VAT, ALS_VAT_AMOUNT, ALS_TOTAL_AMOUNT);

      query.addOrder(tmp, COL_TRADE_DATE, COL_SALE, itemIdName);

    } else {
      for (String group : groupBy) {
        List<String> fields = new ArrayList<>();
        List<String> order = new ArrayList<>();

        switch (group) {
          case COL_ITEM_TYPE:
            fields.add(ALS_ITEM_TYPE_NAME);
            break;

          case COL_ITEM_GROUP:
            fields.add(ALS_ITEM_GROUP_NAME);
            break;

          case COL_TA_ITEM:
            fields.add(ALS_ITEM_NAME);
            fields.add(COL_TRADE_ITEM_ARTICLE);
            fields.add(ALS_UNIT_NAME);
            fields.add(ALS_SUPPLIER_NAME);
            break;

          case COL_TA_COMPANY:
            fields.add(ALS_COMPANY_NAME);
            break;

          case COL_TA_MANAGER:
            fields.add(COL_FIRST_NAME);
            fields.add(COL_LAST_NAME);

            order.add(COL_LAST_NAME);
            order.add(COL_FIRST_NAME);
            break;
        }

        for (String field : fields) {
          query.addFields(tmp, field);
        }

        if (order.isEmpty()) {
          order.addAll(fields);
        }
        for (String field : order) {
          query.addGroup(tmp, field);
          query.addOrder(tmp, field);
        }
      }
      if (groupBy.contains(COL_TA_ITEM)) {
        query.addSum(tmp, COL_COST_AMOUNT);
      }
      query.addSum(tmp, COL_TRADE_ITEM_QUANTITY);
      query.addSum(tmp, ALS_WITHOUT_VAT);
      query.addSum(tmp, ALS_VAT_AMOUNT);
      query.addSum(tmp, ALS_TOTAL_AMOUNT);
    }

    query.addFrom(tmp);

    SimpleRowSet data = qs.getData(query);

    qs.sqlDropTemp(tmp);

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.emptyResponse();

    } else {
      if (groupBy.isEmpty()) {
        data.removeColumn(itemIdName);
      }

      return ResponseObject.response(data);
    }
  }

  private Table<Long, Long, Double> getStock(Collection<Long> items) {
    Table<Long, Long, Double> result = HashBasedTable.create();

    IsCondition condition = BeeUtils.isEmpty(items)
        ? null : SqlUtils.inList(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, items);

    SqlSelect plusQuery = getStockQuery(TBL_TRADE_ACT_ITEMS, condition, true);
    SimpleRowSet plusData = qs.getData(plusQuery);

    if (!DataUtils.isEmpty(plusData)) {
      for (SimpleRow row : plusData) {
        Double qty = row.getDouble(COL_TRADE_ITEM_QUANTITY);
        if (BeeUtils.nonZero(qty)) {
          result.put(row.getLong(COL_TA_ITEM), row.getLong(COL_OPERATION_WAREHOUSE_TO), qty);
        }
      }
    }

    SqlSelect minusQuery = getStockQuery(TBL_TRADE_ACT_ITEMS, condition, false);
    SimpleRowSet minusData = qs.getData(minusQuery);

    pushStock(result, minusData, COL_OPERATION_WAREHOUSE_FROM);

    condition = BeeUtils.isEmpty(items)
        ? null : SqlUtils.inList(TBL_TRADE_ACT_SERVICES, COL_TA_ITEM, items);

    condition = SqlUtils.and(condition, SqlUtils.isNull(TBL_ITEMS, COL_ITEM_IS_SERVICE),
        SqlUtils.notNull(TBL_TRADE_ACT_SERVICES, COL_SERVICE_DATE_FROM));

    SqlSelect minusServicesQuery = getStockQuery(TBL_TRADE_ACT_SERVICES, condition, false);
    SimpleRowSet minusServicesData = qs.getData(minusServicesQuery);

    pushStock(result, minusServicesData, COL_OPERATION_WAREHOUSE_FROM);

    return result;
  }

  private String getStock(IsCondition actCondition, IsCondition itemCondition, Long time,
      Collection<Long> warehouses, Set<Long> categories, Set<Long> items, String colPrefix) {

    /* Selects all items items from */
    SqlSelect itemsQuery = getStockQuery(TBL_TRADE_ACT_ITEMS, true);
    SqlSelect servicesQuery = getStockQuery(TBL_TRADE_ACT_SERVICES, false);

    servicesQuery.addFromInner(TBL_ITEMS,
        sys.joinTables(TBL_ITEMS, TBL_TRADE_ACT_SERVICES, COL_TA_ITEM));

    HasConditions where = SqlUtils.and();
    HasConditions servicesWhere = SqlUtils.and();
    servicesWhere.add(SqlUtils.and(SqlUtils.isNull(TBL_ITEMS, COL_ITEM_IS_SERVICE), SqlUtils
        .notNull(TBL_TRADE_ACT_SERVICES, COL_SERVICE_DATE_FROM)));

    if (actCondition != null) {
      where.add(actCondition);
      servicesWhere.add(actCondition);
    }

    if (!BeeUtils.isEmpty(categories)) {
      where.add(SqlUtils.in(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, TBL_ITEM_CATEGORIES, COL_ITEM,
          SqlUtils.inList(TBL_ITEM_CATEGORIES, COL_CATEGORY, categories)));

      servicesWhere.add(SqlUtils.in(TBL_TRADE_ACT_SERVICES, COL_TA_ITEM, TBL_ITEM_CATEGORIES,
          COL_ITEM,
          SqlUtils.inList(TBL_ITEM_CATEGORIES, COL_CATEGORY, categories)));
    }

    if (!BeeUtils.isEmpty(items)) {
      where.add(SqlUtils.inList(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, items));
      servicesWhere.add(SqlUtils.inList(TBL_TRADE_ACT_SERVICES, COL_TA_ITEM, items));
    }

    if (itemCondition != null) {
      itemsQuery.addFromInner(TBL_ITEMS,
          sys.joinTables(TBL_ITEMS, TBL_TRADE_ACT_ITEMS, COL_TA_ITEM));
      where.add(itemCondition);

    }

    if (time != null) {
      where.add(SqlUtils.less(TBL_TRADE_ACTS, COL_TA_DATE, time));
      servicesWhere.add(SqlUtils.less(TBL_TRADE_ACTS, COL_TA_DATE, time));
    }

    if (BeeUtils.isEmpty(warehouses)) {
      where.add(SqlUtils.or(
          SqlUtils.notNull(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_FROM),
          SqlUtils.notNull(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_TO)));
      servicesWhere.add(SqlUtils.notNull(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_FROM));
    } else {
      where.add(SqlUtils.or(
          SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_FROM, warehouses),
          SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_TO, warehouses)));
      servicesWhere.add(SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_FROM,
          warehouses));
    }

    itemsQuery.setWhere(where);
    servicesQuery.setWhere(servicesWhere);

    itemsQuery.addGroup(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM);
    itemsQuery.addGroup(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_FROM,
        COL_OPERATION_WAREHOUSE_TO);

    servicesQuery.addGroup(TBL_TRADE_ACT_SERVICES, COL_TA_ITEM);
    servicesQuery.addGroup(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_FROM);

    String tmp = qs.sqlCreateTemp(itemsQuery);

    SqlInsert insertServices = new SqlInsert(tmp)
        .addFields(COL_TA_ITEM, COL_OPERATION_WAREHOUSE_FROM, COL_TRADE_ITEM_QUANTITY)
        .setDataSource(servicesQuery);

    qs.insertData(insertServices);

    Set<Long> ids = qs.getNotNullLongSet(tmp, COL_OPERATION_WAREHOUSE_FROM);
    ids.addAll(qs.getNotNullLongSet(tmp, COL_OPERATION_WAREHOUSE_TO));

    if (!BeeUtils.isEmpty(warehouses)) {
      ids.retainAll(warehouses);
    }

    if (ids.isEmpty()) {
      qs.sqlDropTemp(tmp);
      return null;
    }

    SqlSelect query = new SqlSelect()
        .addFields(tmp, COL_TA_ITEM);

    int precision = sys.getFieldPrecision(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY);
    int scale = sys.getFieldScale(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY);

    IsExpression zero = SqlUtils.cast(SqlUtils.constant(SqlDataType.DECIMAL.getEmptyValue()),
        SqlDataType.DECIMAL, precision, scale);

    List<String> aliases = new ArrayList<>();

    for (Long id : ids) {
      String alias = colPrefix + id;
      aliases.add(alias);

      IsExpression plus = SqlUtils.sqlIf(SqlUtils.equals(tmp, COL_OPERATION_WAREHOUSE_TO, id),
          SqlUtils.field(tmp, COL_TRADE_ITEM_QUANTITY), zero);
      IsExpression minus = SqlUtils.sqlIf(SqlUtils.equals(tmp, COL_OPERATION_WAREHOUSE_FROM, id),
          SqlUtils.field(tmp, COL_TRADE_ITEM_QUANTITY), zero);

      query.addSum(SqlUtils.minus(plus, minus), alias);
    }

    query.addFrom(tmp);
    query.addGroup(tmp, COL_TA_ITEM);

    String stock = qs.sqlCreateTemp(query);
    qs.sqlDropTemp(tmp);

    where = SqlUtils.and();
    for (String alias : aliases) {
      where.add(SqlUtils.or(SqlUtils.isNull(stock, alias), SqlUtils.equals(stock, alias, 0)));
    }

    SqlDelete delete = new SqlDelete(stock).setWhere(where);
    qs.updateData(delete);

    List<String> colNames = new ArrayList<>();
    for (String alias : aliases) {
      HasConditions hasValue = SqlUtils.and(
          SqlUtils.notNull(stock, alias),
          SqlUtils.notEqual(stock, alias, 0));

      if (qs.sqlExists(stock, hasValue)) {
        colNames.add(alias);
      }
    }

    if (colNames.isEmpty()) {
      qs.sqlDropTemp(stock);
      return null;

    } else if (colNames.size() == aliases.size()) {
      return stock;

    } else {
      query = new SqlSelect()
          .addFields(stock, COL_TA_ITEM)
          .addFields(stock, colNames)
          .addFrom(stock);

      String section = qs.sqlCreateTemp(query);
      qs.sqlDropTemp(stock);

      return section;
    }
  }

  private Range<JustDate> getMainRange(List<Long> acts) {
    String colActId = sys.getIdName(TBL_TRADE_ACTS);

    SqlSelect rangesSel = new SqlSelect()
        .addMin(TBL_TRADE_ACTS, COL_TA_DATE, "ActLow")
        .addMax(TBL_TRADE_ACTS, COL_TA_UNTIL, "ActHi")
        .addMin(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_FROM, "SvcLow")
        .addMax(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_TO, "SvcHi")
        .addFrom(TBL_TRADE_ACTS)
        .addFromLeft(TBL_TRADE_ACT_SERVICES, sys.joinTables(TBL_TRADE_ACTS, TBL_TRADE_ACT_SERVICES,
            COL_TRADE_ACT))
        .setWhere(SqlUtils.inList(TBL_TRADE_ACTS, colActId, acts));

    SimpleRowSet minMaxRanges = qs.getData(rangesSel);

    DateTime start = BeeUtils.min(minMaxRanges.getDateTime(0, "ActLow"),
        minMaxRanges.getDateTime(0, "SvcLow"));

    DateTime end = BeeUtils.max(minMaxRanges.getDateTime(0, "ActHi"), minMaxRanges.getDateTime(0,
        "SvcHi"));

    if (start == null || end == null || isMeq(start, end)) {
      logger.warning("Invalid  main range for approve act", start, end);
      return null;
    }

    return Range.closedOpen(new JustDate(start), new JustDate(end));

  }

  private String getMovement(IsCondition actCondition, IsCondition itemCondition,
      Long startTime, Long endTime, Collection<Long> warehouses, Set<Long> categories,
      Set<Long> items) {

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM)
        .addFields(TBL_TRADE_ACTS, COL_TA_OPERATION)
        .addSum(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY)
        .addFrom(TBL_TRADE_ACT_ITEMS)
        .addFromInner(TBL_TRADE_ACTS,
            sys.joinTables(TBL_TRADE_ACTS, TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT))
        .addFromInner(TBL_TRADE_OPERATIONS,
            sys.joinTables(TBL_TRADE_OPERATIONS, TBL_TRADE_ACTS, COL_TA_OPERATION));

    HasConditions where = SqlUtils.and();

    if (actCondition != null) {
      where.add(actCondition);
    }

    if (!BeeUtils.isEmpty(categories)) {
      where.add(SqlUtils.in(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, TBL_ITEM_CATEGORIES, COL_ITEM,
          SqlUtils.inList(TBL_ITEM_CATEGORIES, COL_CATEGORY, categories)));
    }

    if (!BeeUtils.isEmpty(items)) {
      where.add(SqlUtils.inList(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, items));
    }

    if (itemCondition != null) {
      query.addFromInner(TBL_ITEMS,
          sys.joinTables(TBL_ITEMS, TBL_TRADE_ACT_ITEMS, COL_TA_ITEM));
      where.add(itemCondition);
    }

    if (startTime != null) {
      where.add(SqlUtils.moreEqual(TBL_TRADE_ACTS, COL_TA_DATE, startTime));
    }
    if (endTime != null) {
      where.add(SqlUtils.less(TBL_TRADE_ACTS, COL_TA_DATE, endTime));
    }

    if (BeeUtils.isEmpty(warehouses)) {
      where.add(SqlUtils.or(
          SqlUtils.notNull(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_FROM),
          SqlUtils.notNull(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_TO)));
    } else {
      where.add(SqlUtils.or(
          SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_FROM, warehouses),
          SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_TO, warehouses)));
    }

    query.setWhere(where);

    query.addGroup(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM);
    query.addGroup(TBL_TRADE_ACTS, COL_TA_OPERATION);

    String tmp = qs.sqlCreateTemp(query);

    Set<Long> ids = qs.getNotNullLongSet(tmp, COL_TA_OPERATION);
    if (ids.isEmpty()) {
      qs.sqlDropTemp(tmp);
      return null;
    }

    query = new SqlSelect()
        .addFields(tmp, COL_TA_ITEM);

    int precision = sys.getFieldPrecision(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY);
    int scale = sys.getFieldScale(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY);

    IsExpression zero = SqlUtils.cast(SqlUtils.constant(SqlDataType.DECIMAL.getEmptyValue()),
        SqlDataType.DECIMAL, precision, scale);

    for (Long id : ids) {
      IsExpression expr = SqlUtils.sqlIf(SqlUtils.equals(tmp, COL_TA_OPERATION, id),
          SqlUtils.field(tmp, COL_TRADE_ITEM_QUANTITY), zero);

      query.addSum(expr, PFX_MOVEMENT + id);
    }

    query.addFrom(tmp);
    query.addGroup(tmp, COL_TA_ITEM);

    String movement = qs.sqlCreateTemp(query);
    qs.sqlDropTemp(tmp);

    return movement;
  }

  private SqlSelect getStockQuery(String source, IsCondition condition, boolean plus) {
    String colWarehouse = plus ? COL_OPERATION_WAREHOUSE_TO : COL_OPERATION_WAREHOUSE_FROM;

    return new SqlSelect()
        .addFields(source, COL_TA_ITEM)
        .addFields(TBL_TRADE_OPERATIONS, colWarehouse)
        .addSum(source, COL_TRADE_ITEM_QUANTITY)
        .addFrom(source)
        .addFromInner(TBL_TRADE_ACTS,
            sys.joinTables(TBL_TRADE_ACTS, source, COL_TRADE_ACT))
        .addFromInner(TBL_TRADE_OPERATIONS,
            sys.joinTables(TBL_TRADE_OPERATIONS, TBL_TRADE_ACTS, COL_TA_OPERATION))
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, source, COL_TA_ITEM))
        .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_TRADE_OPERATIONS, colWarehouse), condition))
        .addGroup(source, COL_TA_ITEM)
        .addGroup(TBL_TRADE_OPERATIONS, colWarehouse);
  }

  private ResponseObject getStockReport(RequestInfo reqInfo) {
    Long startDate = reqInfo.getParameterLong(Service.VAR_FROM);
    Long endDate = reqInfo.getParameterLong(Service.VAR_TO);

    boolean showQuantity = reqInfo.hasParameter(COL_TRADE_ITEM_QUANTITY);
    boolean showWeight = reqInfo.hasParameter(COL_ITEM_WEIGHT);

    if (!showQuantity && !showWeight) {
      showQuantity = true;
    }

    Set<Long> companies = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_COMPANY));
    Set<Long> objects = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_OBJECT));

    Set<Long> warehouses = DataUtils.parseIdSet(reqInfo.getParameter(COL_WAREHOUSE));

    Set<Long> categories = DataUtils.parseIdSet(reqInfo.getParameter(COL_CATEGORY));
    Set<Long> items = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_ITEM));

    List<String> groupBy = NameUtils.toList(reqInfo.getParameter(Service.VAR_GROUP_BY));

    HasConditions actCondition = SqlUtils.and();

    if (!companies.isEmpty()) {
      actCondition.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_COMPANY, companies));
    }
    if (!objects.isEmpty()) {
      actCondition.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_OBJECT, objects));
    }

    IsCondition itemCondition;
    if (!showQuantity && showWeight) {
      itemCondition = SqlUtils.positive(TBL_ITEMS, COL_ITEM_WEIGHT);
    } else {
      itemCondition = null;
    }

    String startStock = null;
    String movement = null;
    String endStock = null;

    if (startDate == null && endDate == null) {
      endStock =
          getStock(actCondition, itemCondition, null, warehouses, categories, items, PFX_END_STOCK);

    } else if (startDate == null) {
      endStock =
          getStock(actCondition, itemCondition, endDate, warehouses, categories, items,
              PFX_END_STOCK);

    } else if (endDate == null) {
      startStock =
          getStock(actCondition, itemCondition, startDate, warehouses, categories, items,
              PFX_START_STOCK);

    } else {
      startStock =
          getStock(actCondition, itemCondition, startDate, warehouses, categories, items,
              PFX_START_STOCK);
      movement =
          getMovement(actCondition, itemCondition, startDate, endDate, warehouses, categories,
              items);
      endStock =
          getStock(actCondition, itemCondition, endDate, warehouses, categories, items,
              PFX_END_STOCK);
    }

    if (BeeUtils.allEmpty(startStock, movement, endStock)) {
      return ResponseObject.emptyResponse();
    }

    boolean sum = groupBy.contains(COL_ITEM_TYPE) || groupBy.contains(COL_ITEM_GROUP);
    String itemIdName = sys.getIdName(TBL_ITEMS);

    SqlSelect query = new SqlSelect();
    query.addFrom(TBL_ITEMS);

    if (sum) {
      for (String group : groupBy) {
        switch (group) {
          case COL_ITEM_TYPE:
            query.addFromLeft(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_TYPES,
                sys.joinTables(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_TYPES, TBL_ITEMS, COL_ITEM_TYPE));

            query.addField(ALS_ITEM_TYPES, COL_CATEGORY_NAME, ALS_ITEM_TYPE_NAME);

            query.addGroup(ALS_ITEM_TYPES, COL_CATEGORY_NAME);
            query.addOrder(null, ALS_ITEM_TYPE_NAME);
            break;

          case COL_ITEM_GROUP:
            query.addFromLeft(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_GROUPS,
                sys.joinTables(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_GROUPS, TBL_ITEMS, COL_ITEM_GROUP));

            query.addField(ALS_ITEM_GROUPS, COL_CATEGORY_NAME, ALS_ITEM_GROUP_NAME);

            query.addGroup(ALS_ITEM_GROUPS, COL_CATEGORY_NAME);
            query.addOrder(null, ALS_ITEM_GROUP_NAME);
            break;
        }
      }

    } else {
      query.addFromLeft(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_TYPES,
          sys.joinTables(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_TYPES, TBL_ITEMS, COL_ITEM_TYPE));
      query.addFromLeft(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_GROUPS,
          sys.joinTables(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_GROUPS, TBL_ITEMS, COL_ITEM_GROUP));

      query.addFromLeft(TBL_UNITS,
          sys.joinTables(TBL_UNITS, TBL_ITEMS, COL_UNIT));

      query.addField(ALS_ITEM_TYPES, COL_CATEGORY_NAME, ALS_ITEM_TYPE_NAME);
      query.addField(ALS_ITEM_GROUPS, COL_CATEGORY_NAME, ALS_ITEM_GROUP_NAME);

      query.addField(TBL_ITEMS, itemIdName, COL_TA_ITEM);
      query.addField(TBL_ITEMS, COL_ITEM_NAME, ALS_ITEM_NAME);
      query.addFields(TBL_ITEMS, COL_ITEM_ARTICLE, COL_ITEM_WEIGHT);

      query.addField(TBL_UNITS, COL_UNIT_NAME, ALS_UNIT_NAME);

      query.addFields(TBL_ITEMS, COL_EXTERNAL_STOCK);

      query.addOrder(TBL_ITEMS, COL_ITEM_ORDINAL);
      query.addOrder(null, ALS_ITEM_TYPE_NAME);
      query.addOrder(null, ALS_ITEM_GROUP_NAME);
      query.addOrder(null, ALS_ITEM_NAME);
      query.addOrder(TBL_ITEMS, COL_ITEM_ARTICLE);
    }

    HasConditions where = SqlUtils.or();

    if (!BeeUtils.isEmpty(startStock)) {
      addStockColumns(query, startStock, PFX_START_STOCK, showQuantity, showWeight, sum);

      qs.sqlIndex(startStock, COL_TA_ITEM);
      query.addFromLeft(startStock,
          SqlUtils.join(TBL_ITEMS, itemIdName, startStock, COL_TA_ITEM));

      where.add(SqlUtils.notNull(startStock, COL_TA_ITEM));
    }

    if (!BeeUtils.isEmpty(movement)) {
      addMovementColumns(query, movement, showQuantity, showWeight, sum);

      qs.sqlIndex(movement, COL_TA_ITEM);
      query.addFromLeft(movement,
          SqlUtils.join(TBL_ITEMS, itemIdName, movement, COL_TA_ITEM));

      where.add(SqlUtils.notNull(movement, COL_TA_ITEM));
    }

    if (!BeeUtils.isEmpty(endStock)) {
      addStockColumns(query, endStock, PFX_END_STOCK, showQuantity, showWeight, sum);

      qs.sqlIndex(endStock, COL_TA_ITEM);
      query.addFromLeft(endStock,
          SqlUtils.join(TBL_ITEMS, itemIdName, endStock, COL_TA_ITEM));

      where.add(SqlUtils.notNull(endStock, COL_TA_ITEM));
    }

    query.setWhere(where);
    SimpleRowSet data = qs.getData(query);

    if (!BeeUtils.isEmpty(startStock)) {
      qs.sqlDropTemp(startStock);
    }
    if (!BeeUtils.isEmpty(movement)) {
      qs.sqlDropTemp(movement);
    }
    if (!BeeUtils.isEmpty(endStock)) {
      qs.sqlDropTemp(endStock);
    }

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(data);
    }
  }

  private ResponseObject getTransferReport(RequestInfo reqInfo) {
    Long startDate = reqInfo.getParameterLong(Service.VAR_FROM);
    if (startDate == null) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_FROM);
    }

    Long endDate = reqInfo.getParameterLong(Service.VAR_TO);
    if (endDate == null) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_TO);
    }

    Long currency = reqInfo.getParameterLong(COL_TA_CURRENCY);

    Set<Long> companies = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_COMPANY));
    Set<Long> managers = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_MANAGER));

    Set<Long> categories = DataUtils.parseIdSet(reqInfo.getParameter(COL_CATEGORY));
    Set<Long> services = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_ITEM));

    Set<Long> suppliers = DataUtils.parseIdSet(reqInfo.getParameter(COL_TRADE_SUPPLIER));

    Set<Long> operations = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_OPERATION));
    Set<Long> objects = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_OBJECT));

    Set<Long> status = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_STATUS));
    Set<Long> series = DataUtils.parseIdSet(reqInfo.getParameter(COL_TA_SERIES));

    List<String> groupBy = NameUtils.toList(reqInfo.getParameter(Service.VAR_GROUP_BY));

    HasConditions actConditions = SqlUtils.and();
    HasConditions serviceConditions = SqlUtils.and();

    HasConditions kindConditions = SqlUtils.or();
    for (TradeActKind kind : TradeActKind.values()) {
      if (kind.enableInvoices()) {
        kindConditions.add(SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_KIND, kind.ordinal()));
      }
    }
    actConditions.add(kindConditions);
    actConditions.add(
        SqlUtils.or(
            SqlUtils.isNull(TBL_TRADE_ACTS, COL_TA_UNTIL),
            SqlUtils.more(TBL_TRADE_ACTS, COL_TA_UNTIL, startDate)));
    serviceConditions.add(
        SqlUtils.or(
            SqlUtils.isNull(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_TO),
            SqlUtils.more(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_TO, startDate)));

    actConditions.add(SqlUtils.less(TBL_TRADE_ACTS, COL_TA_DATE, endDate));
    serviceConditions.add(
        SqlUtils.or(
            SqlUtils.isNull(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_FROM),
            SqlUtils.less(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_FROM, endDate)
        )
    );

    if (!companies.isEmpty()) {
      actConditions.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_COMPANY, companies));
    }
    if (!managers.isEmpty()) {
      actConditions.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_MANAGER, managers));
    }

    if (!categories.isEmpty()) {
      serviceConditions.add(SqlUtils.in(TBL_TRADE_ACT_SERVICES, COL_TA_ITEM,
          TBL_ITEM_CATEGORIES, COL_ITEM,
          SqlUtils.inList(TBL_ITEM_CATEGORIES, COL_CATEGORY, categories)));
    }
    if (!services.isEmpty()) {
      serviceConditions.add(SqlUtils.inList(TBL_TRADE_ACT_SERVICES, COL_TA_ITEM, services));
    }
    if (!suppliers.isEmpty()) {
      serviceConditions.add(SqlUtils.inList(TBL_TRADE_ACT_SERVICES, COL_TRADE_SUPPLIER, suppliers));
    }
    if (!operations.isEmpty()) {
      actConditions.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_OPERATION, operations));
    }
    if (!series.isEmpty()) {
      actConditions.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_SERIES, series));
    }
    if (!objects.isEmpty()) {
      actConditions.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_OBJECT, objects));
    }
    if (!status.isEmpty()) {
      actConditions.add(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_STATUS, status));
    }

    String actIdName = sys.getIdName(TBL_TRADE_ACTS);

    int amountPrecision = 15;
    int amountScale = 2;

    SqlSelect actQuery = new SqlSelect()
        .addFields(TBL_TRADE_ACTS, actIdName, COL_TA_DATE, COL_TA_UNTIL, COL_TA_CURRENCY);

    actQuery.addEmptyNumeric(ALS_ITEM_TOTAL, amountPrecision, amountScale);

    actQuery.addFrom(TBL_TRADE_ACTS)
        .addFromLeft(TBL_TRADE_STATUSES,
            sys.joinTables(TBL_TRADE_STATUSES, TBL_TRADE_ACTS, COL_TA_STATUS));

    if (groupBy.isEmpty()) {
      actQuery.addFields(TBL_TRADE_ACT_NAMES, COL_TRADE_ACT_NAME)
          .addFields(TBL_TRADE_SERIES, COL_SERIES_NAME)
          .addFields(TBL_TRADE_ACTS, COL_TA_NUMBER)
          .addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_NAME)
          .addFields(TBL_TRADE_STATUSES, COL_STATUS_NAME)
          .addFields(TBL_COMPANY_OBJECTS, COL_COMPANY_OBJECT_NAME);

      actQuery
          .addFromLeft(TBL_TRADE_ACT_NAMES,
              sys.joinTables(TBL_TRADE_ACT_NAMES, TBL_TRADE_ACTS, COL_TA_NAME))
          .addFromLeft(TBL_TRADE_OPERATIONS,
              sys.joinTables(TBL_TRADE_OPERATIONS, TBL_TRADE_ACTS, COL_TA_OPERATION))
          .addFromLeft(TBL_TRADE_SERIES,
              sys.joinTables(TBL_TRADE_SERIES, TBL_TRADE_ACTS, COL_TA_SERIES))
          .addFromLeft(TBL_COMPANY_OBJECTS,
              sys.joinTables(TBL_COMPANY_OBJECTS, TBL_TRADE_ACTS, COL_TA_OBJECT));
    }

    boolean hasCompany = groupBy.isEmpty() || groupBy.contains(COL_TA_COMPANY);
    if (hasCompany) {
      actQuery.addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME);

      actQuery.addFromLeft(TBL_COMPANIES,
          sys.joinTables(TBL_COMPANIES, TBL_TRADE_ACTS, COL_TA_COMPANY));
    }

    boolean hasManager = groupBy.contains(COL_TA_MANAGER);
    if (hasManager) {
      actQuery.addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);

      actQuery.addFromLeft(TBL_USERS,
          sys.joinTables(TBL_USERS, TBL_TRADE_ACTS, COL_TA_MANAGER))
          .addFromLeft(TBL_COMPANY_PERSONS,
              sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS, COL_COMPANY_PERSON))
          .addFromLeft(TBL_PERSONS,
              sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON));
    }

    actQuery.setWhere(SqlUtils.and(actConditions,
        SqlUtils.in(TBL_TRADE_ACTS, actIdName,
                new SqlSelect().addFields(TBL_TRADE_ACT_SERVICES, COL_TRADE_ACT)
                        .addFrom(TBL_TRADE_ACT_SERVICES)
                        .addFromLeft(TBL_ITEMS,
                                sys.joinTables(TBL_ITEMS, TBL_TRADE_ACT_SERVICES, COL_TA_ITEM))
                        .setWhere(serviceConditions)
        )));

    String acts = qs.sqlCreateTemp(actQuery);

    Set<Long> actIds = qs.getLongSet(new SqlSelect().addFields(acts, actIdName).addFrom(acts));
    if (actIds.isEmpty()) {
      qs.sqlDropTemp(acts);
      return ResponseObject.emptyResponse();
    }

    for (Long actId : actIds) {
      double total = totalActItems(actId);

      if (BeeUtils.isPositive(total)) {
        SqlUpdate update = new SqlUpdate(acts)
            .addConstant(ALS_ITEM_TOTAL, BeeUtils.round(total, amountScale))
            .setWhere(SqlUtils.equals(acts, actIdName, actId));
        qs.updateData(update);
      }
    }

    SqlSelect serviceQuery = new SqlSelect()
        .addFields(acts, COL_TA_DATE, COL_TA_UNTIL, COL_TA_CURRENCY, ALS_ITEM_TOTAL);

    if (groupBy.isEmpty()) {
      serviceQuery.addFields(acts, COL_TRADE_ACT_NAME, COL_SERIES_NAME, COL_TA_NUMBER,
          COL_OPERATION_NAME, COL_STATUS_NAME, COL_COMPANY_OBJECT_NAME);
    }

    if (hasCompany) {
      serviceQuery.addFields(acts, ALS_COMPANY_NAME);
    }
    if (hasManager) {
      serviceQuery.addFields(acts, COL_FIRST_NAME, COL_LAST_NAME);
    }

    String serviceIdName = sys.getIdName(TBL_TRADE_ACT_SERVICES);

    serviceQuery.addFields(TBL_TRADE_ACT_SERVICES, serviceIdName,
        COL_TRADE_ACT, COL_TA_SERVICE_FROM, COL_TA_SERVICE_TO,
        COL_TRADE_ITEM_QUANTITY, COL_TA_SERVICE_TARIFF, COL_TRADE_ITEM_PRICE,
        COL_TA_SERVICE_FACTOR, COL_TA_SERVICE_DAYS, COL_TA_SERVICE_MIN, COL_TRADE_DISCOUNT);

    serviceQuery.addFields(TBL_ITEMS, COL_TIME_UNIT);
    serviceQuery.addEmptyText("Arr" + COL_TRADE_AMOUNT);

    serviceQuery.addEmptyText("ArrInvoiceDate");
    serviceQuery.addEmptyText("ArrSaleItemDiscount");
    serviceQuery.addEmptyText("Arr" + COL_TRADE_INVOICE_PREFIX);
    serviceQuery.addEmptyText("Arr" + COL_TRADE_INVOICE_NO);
    serviceQuery.addEmptyText("Arr" + COL_SALE);
    serviceQuery.addEmptyDouble("ArrTotalAmount");
    serviceQuery.addEmptyDouble("SaleFactor");

    serviceQuery.addFrom(acts)
        .addFromLeft(TBL_TRADE_ACT_SERVICES,
            SqlUtils.join(TBL_TRADE_ACT_SERVICES, COL_TRADE_ACT, acts, actIdName))
        .addFromLeft(TBL_ITEMS,
            sys.joinTables(TBL_ITEMS, TBL_TRADE_ACT_SERVICES, COL_TA_ITEM));

    if (groupBy.contains(COL_ITEM_TYPE)) {
      serviceQuery.addFromLeft(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_TYPES,
          sys.joinTables(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_TYPES, TBL_ITEMS, COL_ITEM_TYPE));

      serviceQuery.addField(ALS_ITEM_TYPES, COL_CATEGORY_NAME, ALS_ITEM_TYPE_NAME);
    }

    if (groupBy.contains(COL_ITEM_GROUP)) {
      serviceQuery.addFromLeft(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_GROUPS,
          sys.joinTables(TBL_ITEM_CATEGORY_TREE, ALS_ITEM_GROUPS, TBL_ITEMS, COL_ITEM_GROUP));

      serviceQuery.addField(ALS_ITEM_GROUPS, COL_CATEGORY_NAME, ALS_ITEM_GROUP_NAME);
    }

    if (groupBy.isEmpty() || groupBy.contains(COL_TA_ITEM)) {
      serviceQuery.addFromLeft(TBL_UNITS,
          sys.joinTables(TBL_UNITS, TBL_ITEMS, COL_UNIT));

      serviceQuery.addFields(TBL_TRADE_ACT_SERVICES, COL_TA_ITEM);
      serviceQuery.addField(TBL_ITEMS, COL_ITEM_NAME, ALS_ITEM_NAME);
      serviceQuery.addFields(TBL_ITEMS, COL_ITEM_ARTICLE);
      serviceQuery.addField(TBL_UNITS, COL_UNIT_NAME, ALS_UNIT_NAME);

      serviceQuery.addFromLeft(TBL_COMPANIES,
          sys.joinTables(TBL_COMPANIES, TBL_TRADE_ACT_SERVICES, COL_TRADE_SUPPLIER));

      serviceQuery.addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_SUPPLIER_NAME);
      serviceQuery.addFields(TBL_TRADE_ACT_SERVICES, COL_COST_AMOUNT);
    }

    serviceQuery.addEmptyNumeric(ALS_BASE_AMOUNT, amountPrecision, amountScale);

    if (!serviceConditions.isEmpty()) {
      serviceQuery.setWhere(serviceConditions);
    }

    String tmp = qs.sqlCreateTemp(serviceQuery);
    qs.sqlDropTemp(acts);

    if (qs.isEmpty(tmp)) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.emptyResponse();
    }

    prepareTransferReport(tmp, startDate, endDate, serviceIdName);

    if (!DataUtils.isId(currency)) {
      currency = prm.getRelation(PRM_CURRENCY);
    }
    if (DataUtils.isId(currency)) {
      exchange(tmp, COL_TA_CURRENCY, currency, System.currentTimeMillis(),
          COL_TRADE_ITEM_PRICE, ALS_BASE_AMOUNT);
    }

    SqlSelect query = new SqlSelect();

    if (groupBy.isEmpty()) {
      query.addFields(tmp, COL_TRADE_ACT, COL_TRADE_ACT_NAME, COL_OPERATION_NAME, COL_STATUS_NAME ,COL_TA_DATE,
              COL_TA_UNTIL,  COL_SERIES_NAME, COL_TA_NUMBER,
          ALS_COMPANY_NAME, COL_COMPANY_OBJECT_NAME);

      if (hasManager) {
        query.addFields(tmp, COL_FIRST_NAME, COL_LAST_NAME);
      }

      query.addFields(tmp, COL_TA_SERVICE_FROM, COL_TA_SERVICE_TO, COL_TA_ITEM, ALS_ITEM_NAME,
              COL_ITEM_ARTICLE, COL_TIME_UNIT, ALS_SUPPLIER_NAME, COL_COST_AMOUNT, COL_TRADE_ITEM_QUANTITY,
              ALS_UNIT_NAME, COL_TRADE_ITEM_PRICE, COL_TRADE_DISCOUNT, COL_TA_SERVICE_FACTOR,
              ALS_BASE_AMOUNT, "Arr" + COL_TRADE_INVOICE_PREFIX, "Arr" + COL_TRADE_INVOICE_NO, "ArrInvoiceDate", "ArrSaleItemDiscount",
              "Arr" + COL_TRADE_AMOUNT, ALS_ITEM_TOTAL, "SaleFactor", COL_TA_SERVICE_TARIFF, COL_TA_SERVICE_DAYS,
              COL_TA_SERVICE_MIN,
              "Arr" + COL_SALE, "ArrTotalAmount" // hidden column
      );

     // query.addOrder(tmp, COL_TA_DATE, COL_TRADE_ACT, ALS_ITEM_NAME, COL_ITEM_ARTICLE, COL_TA_ITEM);
      query.addOrder(tmp, COL_TA_SERVICE_FROM, ALS_COMPANY_NAME);

    } else {
      for (String group : groupBy) {
        List<String> fields = new ArrayList<>();
        List<String> order = new ArrayList<>();

        switch (group) {
          case COL_ITEM_TYPE:
            fields.add(ALS_ITEM_TYPE_NAME);
            break;

          case COL_ITEM_GROUP:
            fields.add(ALS_ITEM_GROUP_NAME);
            break;

          case COL_TA_ITEM:
            fields.add(COL_TA_ITEM);
            fields.add(ALS_ITEM_NAME);
            fields.add(COL_ITEM_ARTICLE);
            fields.add(ALS_SUPPLIER_NAME);

            order.add(ALS_ITEM_NAME);
            order.add(COL_ITEM_ARTICLE);
            order.add(COL_TA_ITEM);
            order.add(ALS_SUPPLIER_NAME);
            break;

          case COL_TA_COMPANY:
            fields.add(ALS_COMPANY_NAME);
            break;

          case COL_TA_MANAGER:
            fields.add(COL_FIRST_NAME);
            fields.add(COL_LAST_NAME);

            order.add(COL_LAST_NAME);
            order.add(COL_FIRST_NAME);
            break;
        }

        for (String field : fields) {
          query.addFields(tmp, field);
        }

        if (order.isEmpty()) {
          order.addAll(fields);
        }
        for (String field : order) {
          query.addGroup(tmp, field);
          query.addOrder(tmp, field);
        }
      }
      if (groupBy.contains(COL_TA_ITEM)) {
        query.addSum(tmp, COL_COST_AMOUNT);
      }
      query.addSum(tmp, COL_TRADE_ITEM_QUANTITY);
      query.addSum(tmp, ALS_BASE_AMOUNT);
    }

    query.addFrom(tmp);

    // ID 2 Don't add empty single time service
    query.setWhere(
            SqlUtils.and(
              SqlUtils.and(
              SqlUtils.notNull(
                      SqlUtils.sqlIf(
                        SqlUtils.isNull(tmp, COL_TA_SERVICE_FROM),
                        SqlUtils.sqlIf(SqlUtils.notNull(tmp, COL_TIME_UNIT), SqlUtils.sqlTrue(), null),
                        SqlUtils.sqlTrue()
                      )
              ),
              SqlUtils.or(SqlUtils.positive(tmp, COL_TRADE_ITEM_QUANTITY), SqlUtils.notNull(tmp, COL_TIME_UNIT))
            ),
                    SqlUtils.or(SqlUtils.isNull(tmp, COL_TA_SERVICE_FROM),
                            SqlUtils.moreEqual(tmp, COL_TA_SERVICE_FROM, startDate)),
                    SqlUtils.or(SqlUtils.isNull(tmp, COL_TA_SERVICE_TO), SqlUtils.lessEqual(tmp, COL_TA_SERVICE_TO, endDate))
          )
    );

    SimpleRowSet data = qs.getData(query);
    qs.sqlDropTemp(tmp);

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(data);
    }
  }

  private void maybeApproveTradeActs(Long retId, Long apprId, List<Long> acts) {
    if (!DataUtils.isId(retId) && !DataUtils.isId(apprId) && BeeUtils.isEmpty(acts)) {
      return;
    }

    String colActId = sys.getIdName(TBL_TRADE_ACTS);

    SqlSelect actDataSelect = new SqlSelect()
        .addFields(TBL_TRADE_ACTS, colActId, COL_TA_DATE, COL_TA_UNTIL)
        .addFrom(TBL_TRADE_ACTS)
        .setWhere(SqlUtils.inList(TBL_TRADE_ACTS, colActId, acts));

    SimpleRowSet actData = qs.getData(actDataSelect);

    Range<JustDate> mainRange = getMainRange(acts);

    if (mainRange == null) {
      return;
    }

    Range<DateTime> builderRange = TradeActUtils.convertRange(mainRange);

    Set<Long> actsForApprove = new HashSet<>();
    actsForApprove.addAll(acts);

    BeeRowSet services = getServicesForInvoice(actsForApprove);

    if (!services.isEmpty()) {
      for (int i = 0; i < services.getNumberOfRows(); i++) {
        if (!ArrayUtils.contains(actData.getLongColumn(colActId), services.getLong(i,
            COL_TRADE_ACT))) {
          continue;
        }

        SimpleRow actRow = actData.getRowByKey(colActId, services.getString(i, COL_TRADE_ACT));

        TradeActTimeUnit tu = EnumUtils.getEnumByIndex(TradeActTimeUnit.class, services.getInteger(
            i, COL_TIME_UNIT));
        JustDate dateFrom = services.getDate(i, COL_TA_SERVICE_FROM);
        JustDate dateTo = services.getDate(i, COL_TA_SERVICE_TO);

        Range<DateTime> actRange = TradeActUtils.createRange(actRow.getDateTime(COL_TA_DATE),
            actRow.getDateTime(COL_TA_UNTIL));

        Range<DateTime> serviceRange = TradeActUtils.createServiceRange(dateFrom, dateTo, tu,
            builderRange, actRange);

        if (serviceRange == null) {
          continue;
        }

        List<Range<DateTime>> invoicesRanges = new ArrayList<>();

        List<Integer> periods =
            BeeUtils.toInts(services.getRow(i).getProperty(PRP_INVOICE_PERIODS));
        if (periods.size() >= 2) {
          for (int j = 0; j < periods.size() - 1; j += 2) {
            JustDate from = new JustDate(periods.get(j));
            JustDate to = new JustDate(periods.get(j + 1));

            invoicesRanges.add(TradeActUtils.createRange(from, to));
          }
        }

        List<Range<DateTime>> ranges = TradeActUtils.buildRanges(serviceRange, invoicesRanges, tu);

        if (ranges.isEmpty()) {
          continue;
        }

        boolean hasValidRange = false;

        for (Range<DateTime> r : ranges) {

          if (tu == TradeActTimeUnit.MONTH) {
            double mf = TradeActUtils.getMonthFactor(r, getHolidays());

            if (BeeUtils.isPositive(mf)) {
              hasValidRange = true;
              break;
            }

          } else {
            hasValidRange = true;
            break;
          }
        }

        if (hasValidRange) {
          actsForApprove.remove(actRow.getLong(colActId));
        }
      }
    }

    if (actsForApprove.isEmpty()) {
      return;
    }

    SqlUpdate upd = new SqlUpdate(TBL_TRADE_ACTS)
        .addConstant(COL_TA_STATUS, apprId)
        .setWhere(
          SqlUtils.and(
            SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_STATUS, retId),
            SqlUtils.inList(TBL_TRADE_ACTS, sys.getIdName(TBL_TRADE_ACTS), actsForApprove)
          ));

    qs.updateData(upd);
  }

  private void prepareTransferReport(String tmp, Long startDate, Long endDate, String idName) {
    logger.debug("prepareTransferReport >>>");
    SqlSelect query = new SqlSelect()
        .addFields(tmp, idName, COL_TA_DATE, COL_TA_UNTIL, COL_TA_SERVICE_FROM, COL_TA_SERVICE_TO,
            COL_TIME_UNIT, COL_TRADE_ITEM_QUANTITY, ALS_ITEM_TOTAL, COL_TA_SERVICE_TARIFF,
            COL_TRADE_ITEM_PRICE, COL_TA_SERVICE_FACTOR, COL_TA_SERVICE_DAYS, COL_TA_SERVICE_MIN,
            COL_TRADE_DISCOUNT)
        .addFrom(tmp);

    String saleId = sys.getIdName(TBL_SALES);

    IsExpression amountExpression = SqlUtils.multiply(SqlUtils.field(TBL_SALE_ITEMS, COL_TRADE_ITEM_QUANTITY), SqlUtils.field(TBL_SALE_ITEMS, COL_TRADE_ITEM_PRICE),
      SqlUtils.field(TBL_SALE_ITEMS, COL_TA_SERVICE_FACTOR));

    SqlSelect invoicesQuery = new SqlSelect()
            .addFields(TBL_TRADE_ACT_SERVICES, idName)
            .addFields(TBL_SALES, COL_TRADE_INVOICE_NO, saleId)
            .addField(TBL_SALES, COL_TRADE_DATE, "InvoiceDate")
            .addExpr(SqlUtils.minus(amountExpression, SqlUtils.multiply(amountExpression, SqlUtils.divide(SqlUtils.nvl(
              SqlUtils.field(TBL_SALE_ITEMS, COL_TRADE_DISCOUNT), 0), 100))),COL_TRADE_AMOUNT)
            .addField(TBL_SALE_ITEMS, COL_TRADE_DISCOUNT, "SaleItemDiscount")
            .addField(TBL_SALES_SERIES, COL_SERIES_NAME, COL_TRADE_INVOICE_PREFIX)
            .addFrom(TBL_TRADE_ACT_SERVICES)
            .addFromInner(TBL_TRADE_ACT_INVOICES,
                    sys.joinTables(TBL_TRADE_ACT_SERVICES, TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_SERVICE))
            .addFromLeft(TBL_SALE_ITEMS,
                    sys.joinTables(TBL_SALE_ITEMS, TBL_TRADE_ACT_INVOICES, "SaleItem"))
            .addFromLeft(TBL_SALES,
                    sys.joinTables(TBL_SALES, TBL_SALE_ITEMS, COL_SALE))
            .addFromLeft(TBL_SALES_SERIES,
                    sys.joinTables(TBL_SALES_SERIES, TBL_SALES, COL_TRADE_SALE_SERIES));


    SimpleRowSet data = qs.getData(query);
    SimpleRowSet invoicesData = qs.getData(invoicesQuery
              .setWhere(SqlUtils.and(
                      SqlUtils.inList(TBL_TRADE_ACT_SERVICES, idName, (Object[]) data.getLongColumn(idName)),
                      SqlUtils.more(TBL_SALES, COL_TRADE_DATE, startDate),
                      SqlUtils.less(TBL_SALES, COL_TRADE_DATE, endDate)
                      )
              )
    );

    Map<String, Map<String, String>> collectedInvoicesData = new HashMap<>();

    for (SimpleRow row : invoicesData) {
      Map<String, String> collData = collectedInvoicesData.get(row.getValue(idName));


      if (collData == null){
        collData = new HashMap<>();
        collectedInvoicesData.put(row.getValue(idName), collData);
        collData.put("Arr" + COL_TRADE_AMOUNT, "");
        collData.put("ArrTotalAmount", "");
        collData.put("ArrInvoiceDate", "");
        collData.put("ArrSaleItemDiscount", "");
        collData.put("Arr" + COL_TRADE_INVOICE_PREFIX, "");
        collData.put("Arr" + COL_TRADE_INVOICE_NO, "");
        collData.put("Arr" + COL_SALE, "");
      }

      boolean hasData = collData.containsKey("HasData");
      String sale = row.getValue(saleId);

      collData.put("Arr" + COL_TRADE_AMOUNT, (hasData ? collData.get("Arr" + COL_TRADE_AMOUNT) + "\n" : "")
              + BeeUtils.toString(BeeUtils.unbox(row.getDouble(COL_TRADE_AMOUNT)), 2));

      collData.put("ArrInvoiceDate", (hasData ? collData.get("ArrInvoiceDate") + "\n" : "")
              + row.getDate("InvoiceDate").toString());

      if (row.getDouble("SaleItemDiscount") != null) {
        collData.put("ArrSaleItemDiscount", (hasData ? collData.get("ArrSaleItemDiscount") + "\n" : "")
                + BeeUtils.toString(row.getDouble("SaleItemDiscount"), 2));
      } else {
        collData.put("ArrSaleItemDiscount", (hasData ? collData.get("ArrSaleItemDiscount") + "\n" : "")
                + "");
      }

      collData.put("Arr" + COL_TRADE_INVOICE_PREFIX, (hasData ? collData.get("Arr" + COL_TRADE_INVOICE_PREFIX) + "\n" : "")
              + (BeeUtils.isEmpty(row.getValue(COL_TRADE_INVOICE_PREFIX)) ? "" : row.getValue(COL_TRADE_INVOICE_PREFIX))
      );

      collData.put("Arr" + COL_TRADE_INVOICE_NO, (hasData ? collData.get("Arr" + COL_TRADE_INVOICE_NO) + "\n" : "")
              + (BeeUtils.isEmpty(row.getValue(COL_TRADE_INVOICE_NO)) ? "" : row.getValue(COL_TRADE_INVOICE_NO)));

      collData.put("Arr" + COL_SALE, (hasData ? collData.get("Arr" + COL_SALE) + "\n" : "")
        + (BeeUtils.isEmpty(sale) ? "" : sale));

      collData.put("ArrTotalAmount", BeeUtils.toString(BeeUtils.toDouble(collData.get("ArrTotalAmount"))
              + BeeUtils.unbox(row.getDouble(COL_TRADE_AMOUNT))));

      collData.put("HasData", BeeConst.STRING_TRUE);
    }

    Set<Integer> holidays = new HashSet<>();

    Long country = prm.getRelation(PRM_COUNTRY);

    if (DataUtils.isId(country)) {
      SqlSelect holidayQuery = new SqlSelect()
          .addFields(TBL_HOLIDAYS, COL_HOLY_DAY)
          .addFrom(TBL_HOLIDAYS)
          .setWhere(SqlUtils.equals(TBL_HOLIDAYS, COL_HOLY_COUNTRY, country));

      Integer[] days = qs.getIntColumn(holidayQuery);
      if (days != null) {
        for (Integer day : days) {
          if (BeeUtils.isPositive(day)) {
            holidays.add(day);
          }
        }
      }
    }

    Range<DateTime> reportRange = TradeActUtils.createRange(new DateTime(startDate),
        new DateTime(endDate));

    int reportDays = TradeActUtils.countServiceDays(reportRange, holidays);
    if (reportDays <= 0) {
      return;
    }

    int priceScale = sys.getFieldScale(TBL_TRADE_ACT_SERVICES, COL_TRADE_ITEM_PRICE);
    int factorScale = sys.getFieldScale(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_FACTOR);

    for (SimpleRow row : data) {
      TradeActTimeUnit tu = EnumUtils.getEnumByIndex(TradeActTimeUnit.class,
          row.getInt(COL_TIME_UNIT));

      Range<DateTime> actRange = TradeActUtils.createRange(row.getDateTime(COL_TA_DATE),
          row.getDateTime(COL_TA_UNTIL));

      JustDate dateFrom = row.getDate(COL_TA_SERVICE_FROM);
      JustDate dateTo = row.getDate(COL_TA_SERVICE_TO);
      Range<DateTime> serviceRange = TradeActUtils.createServiceRange(
        dateFrom, dateTo, tu, reportRange, actRange);

        SqlUpdate update = new SqlUpdate(tmp);

        if (serviceRange != null) {
          update.addConstant(COL_TA_SERVICE_FROM, serviceRange.lowerEndpoint().getTime())
            .addConstant(COL_TA_SERVICE_TO, serviceRange.upperEndpoint().getTime());
        }

        Double quantity = row.getDouble(COL_TRADE_ITEM_QUANTITY);
        Double price = row.getDouble(COL_TRADE_ITEM_PRICE);
        Double discount = row.getDouble(COL_TRADE_DISCOUNT);
        Double itemTotal = row.getDouble(ALS_ITEM_TOTAL);
        Double factor = row.getDouble(COL_TA_SERVICE_FACTOR);
        Double continuousFactor = 1D;  // jei mėn, tai 1 jei diena 30


        if (tu != null && serviceRange != null) {
          boolean ok = true;

          switch (tu) {
            case DAY:
              if (!BeeUtils.isPositive(factor)) {
                Integer dpw = row.getInt(COL_TA_SERVICE_DAYS);
                if (TradeActUtils.validDpw(dpw)) {
                  int days = TradeActUtils.countServiceDays(serviceRange, holidays, dpw);

                  if (days > 0) {
                    factor = (double) days;
                    update.addConstant(COL_TA_SERVICE_FACTOR, days);
                  } else {
                    ok = false;
                  }
                }
              }
              continuousFactor = 30D;
              break;

            case MONTH:
              double mf = TradeActUtils.getMonthFactor(serviceRange, holidays);

              if (BeeUtils.isPositive(mf)) {
                if (BeeUtils.isPositive(factor)) {
                  mf *= factor;
                }
                factor = BeeUtils.round(mf, factorScale);
                update.addConstant(COL_TA_SERVICE_FACTOR, factor);
              } else {
                ok = false;
              }
              break;
          }

          if (!ok) {
            continue;
          }
        }

        Double amount = TradeActUtils.serviceAmount(quantity, price, discount, tu, factor);
        if (BeeUtils.isPositive(amount)) {
          update.addConstant(ALS_BASE_AMOUNT, amount);
        }

        Double saleTotals = null;
        Map<String, String> collInvoices = collectedInvoicesData.get(row.getValue(idName));
        if (collInvoices != null) {
          saleTotals = BeeUtils.toDoubleOrNull(collInvoices.get("ArrTotalAmount"));
          for (String colName : collInvoices.keySet()) {
            if (colName == idName || colName == "HasData") {
              continue;
            }

            update.addConstant(colName, collInvoices.get(colName));
          }
        }

        Double saleFactor = null;

        if (tu != null && BeeUtils.unbox(saleTotals) > 0 && BeeUtils.unbox(factor) > 0 && BeeUtils.unbox(row.getDouble(ALS_ITEM_TOTAL)) > 0) {
          saleFactor = BeeUtils.unbox(saleTotals) / factor / row.getDouble(ALS_ITEM_TOTAL) * 100 * continuousFactor;
          update.addConstant("SaleFactor", saleFactor);
        }

        Double tariff = null;
        if (tu != null && BeeUtils.isPositive(factor) && BeeUtils.isPositive(itemTotal)) {
            tariff = BeeUtils.unbox(amount) / factor / itemTotal * 100;
        }

        update.addConstant(COL_TA_SERVICE_TARIFF, tariff);

        update.setWhere(SqlUtils.equals(tmp, idName, row.getLong(idName)));

        if (!update.isEmpty()) {
          qs.updateData(update);
        }

    }
    logger.debug("<<< prepareTransferReport");
  }

  private void addStockColumns(SqlSelect query, String tmp, String prefix,
      boolean quantity, boolean weight, boolean sum) {

    List<String> input = qs.sqlColumns(tmp);
    List<String> columns = new ArrayList<>();

    BeeRowSet warehouses = qs.getViewData(VIEW_WAREHOUSES);
    for (long id : warehouses.getRowIds()) {
      String name = prefix + id;
      if (input.contains(name)) {
        columns.add(name);
      }
    }

    for (String column : columns) {
      if (quantity) {
        if (sum) {
          query.addSum(tmp, column, column + SFX_QUANTITY);
        } else {
          query.addField(tmp, column, column + SFX_QUANTITY);
        }
      }

      if (weight) {
        IsExpression expr = SqlUtils.multiply(SqlUtils.field(tmp, column),
            SqlUtils.field(TBL_ITEMS, COL_ITEM_WEIGHT));

        if (sum) {
          query.addSum(expr, column + SFX_WEIGHT);
        } else {
          query.addExpr(expr, column + SFX_WEIGHT);
        }
      }
    }
    if (quantity && columns.size() > 1) {
      String als = prefix + "viso" + SFX_QUANTITY;
      IsExpression xxx = SqlUtils.plus((Object[]) SqlUtils.fields(tmp,
          columns.toArray(new String[0])));

      if (sum) {
        query.addSum(xxx, als);
      } else {
        query.addExpr(xxx, als);
      }
    }
  }

  private void addMovementColumns(SqlSelect query, String tmp, boolean quantity, boolean weight,
      boolean sum) {

    List<String> input = qs.sqlColumns(tmp);
    List<String> columns = new ArrayList<>();

    BeeRowSet operations = qs.getViewData(VIEW_TRADE_OPERATIONS);
    for (long id : operations.getRowIds()) {
      String name = PFX_MOVEMENT + id;
      if (input.contains(name)) {
        columns.add(name);
      }
    }

    for (String column : columns) {
      if (quantity) {
        if (sum) {
          query.addSum(tmp, column, column + SFX_QUANTITY);
        } else {
          query.addField(tmp, column, column + SFX_QUANTITY);
        }
      }

      if (weight) {
        IsExpression expr = SqlUtils.multiply(SqlUtils.field(tmp, column),
            SqlUtils.field(TBL_ITEMS, COL_ITEM_WEIGHT));

        if (sum) {
          query.addSum(expr, column + SFX_WEIGHT);
        } else {
          query.addExpr(expr, column + SFX_WEIGHT);
        }
      }
    }
  }

  private static Filter getTemplateChildrenFilter(Long templateId, Collection<Long> excludeItems) {
    if (BeeUtils.isEmpty(excludeItems)) {
      return Filter.equals(COL_TRADE_ACT_TEMPLATE, templateId);
    } else {
      return Filter.and(Filter.equals(COL_TRADE_ACT_TEMPLATE, templateId),
          Filter.exclude(COL_TA_ITEM, excludeItems));
    }
  }

  private SqlSelect getStockQuery(String source, boolean requireWarehouseTo) {
    SqlSelect query = new SqlSelect()
        .addFields(source, COL_TA_ITEM)
        .addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_FROM);

    if (requireWarehouseTo) {
      query.addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_TO);
    }

    query.addSum(source, COL_TRADE_ITEM_QUANTITY)
        .addFrom(source)
        .addFromInner(TBL_TRADE_ACTS,
            sys.joinTables(TBL_TRADE_ACTS, source, COL_TRADE_ACT))
        .addFromInner(TBL_TRADE_OPERATIONS,
            sys.joinTables(TBL_TRADE_OPERATIONS, TBL_TRADE_ACTS, COL_TA_OPERATION));

    return query;
  }

  private static void pushStock(Table<Long, Long, Double> result, SimpleRowSet minusData,
      String warehouseFrom) {

    if (!DataUtils.isEmpty(minusData)) {
      for (SimpleRow row : minusData) {
        Double qty = row.getDouble(COL_TRADE_ITEM_QUANTITY);

        if (BeeUtils.nonZero(qty)) {
          Long item = row.getLong(COL_TA_ITEM);
          Long warehouse = row.getLong(warehouseFrom);

          Double stock = result.get(item, warehouse);

          if (stock == null) {
            result.put(item, warehouse, -qty);
          } else if (stock.equals(qty)) {
            result.remove(item, warehouse);
          } else {
            result.put(item, warehouse, stock - qty);
          }
        }
      }
    }

  }

  private ResponseObject getTemplateItemsAndServices(RequestInfo reqInfo) {
    Long templateId = reqInfo.getParameterLong(COL_TRADE_ACT_TEMPLATE);
    if (!DataUtils.isId(templateId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TRADE_ACT_TEMPLATE);
    }

    Long actId = reqInfo.getParameterLong(COL_TRADE_ACT);
    TradeActKind kind = EnumUtils.getEnumByIndex(TradeActKind.class,
        reqInfo.getParameter(COL_TA_KIND));

    List<BeeRowSet> result = new ArrayList<>();

    Set<Long> itemIds = new HashSet<>();

    Set<Long> actItems = getActItems(actId);
    Filter filter = getTemplateChildrenFilter(templateId, actItems);

    BeeRowSet templateItems = qs.getViewData(VIEW_TRADE_ACT_TMPL_ITEMS, filter);
    if (!DataUtils.isEmpty(templateItems)) {
      result.add(templateItems);

      int index = templateItems.getColumnIndex(COL_TA_ITEM);
      itemIds.addAll(templateItems.getDistinctLongs(index));
    }

    if (kind != null && kind.enableServices()) {
      Set<Long> actServices = getActServices(actId);
      filter = getTemplateChildrenFilter(templateId, actServices);

      BeeRowSet templateServices = qs.getViewData(VIEW_TRADE_ACT_TMPL_SERVICES, filter);
      if (!DataUtils.isEmpty(templateServices)) {
        result.add(templateServices);

        int index = templateServices.getColumnIndex(COL_TA_ITEM);
        itemIds.addAll(templateServices.getDistinctLongs(index));
      }
    }

    if (!itemIds.isEmpty()) {
      BeeRowSet items = qs.getViewData(VIEW_ITEMS, Filter.idIn(itemIds));
      if (!DataUtils.isEmpty(items)) {
        result.add(items);
      }
    }

    if (result.isEmpty()) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result).setSize(result.size());
    }
  }

  private ResponseObject hasInvoicesOrSecondaryActs(RequestInfo reqInfo) {
    Long actId = reqInfo.getParameterLong(COL_TRADE_ACT);
    if (!DataUtils.isId(actId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TRADE_ACT);
    }

    boolean has = qs.sqlExists(TBL_TRADE_ACTS, COL_TA_PARENT, actId);

    if (!has) {
      SqlSelect query = new SqlSelect()
          .addFrom(TBL_TRADE_ACT_INVOICES)
          .addFromInner(TBL_TRADE_ACT_SERVICES, sys.joinTables(TBL_TRADE_ACT_SERVICES,
              TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_SERVICE))
          .setWhere(SqlUtils.equals(TBL_TRADE_ACT_SERVICES, COL_TRADE_ACT, actId));

      has = qs.sqlCount(query) > 0;
    }

    return ResponseObject.response(has);
  }

  private ResponseObject insertChildItems(Long actId, BeeRowSet parentItems) {
    int itemActIndex = parentItems.getColumnIndex(COL_TRADE_ACT);
    int itemIndex = parentItems.getColumnIndex(COL_TA_ITEM);
    List<BeeColumn> colData = new ArrayList<>();

    for (BeeColumn col : parentItems.getColumns()) {
      if (BeeUtils.inListSame(col.getId(), COL_TRADE_ACT, COL_TA_PARENT)) {
        continue;
      }
      colData.add(col);
    }

    for (BeeRow parentItem : parentItems) {
      ResponseObject itemsResp = ResponseObject.emptyResponse();
      SimpleRowSet itemRow = qs.getData(new SqlSelect().addFields(TBL_TRADE_ACT_ITEMS, sys.getIdName(TBL_TRADE_ACT_ITEMS),
              COL_TRADE_ITEM_QUANTITY)
        .addFrom(TBL_TRADE_ACT_ITEMS).setWhere(
                SqlUtils.and(SqlUtils.equals(TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT, actId),
                        SqlUtils.equals(TBL_TRADE_ACT_ITEMS, COL_TA_PARENT, parentItem.getLong(itemActIndex)),
                        SqlUtils.equals(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, parentItem.getLong(itemIndex)))));

      if (!DataUtils.isEmpty(itemRow) && DataUtils.isId(itemRow.getLong(0, 0))) {
        SqlUpdate itemUpdate = new SqlUpdate(TBL_TRADE_ACT_ITEMS)
                .setWhere(sys.idEquals(TBL_TRADE_ACT_ITEMS, itemRow.getLong(0, 0)));
        appendUpdateConstants(itemUpdate, parentItems, parentItem.getId(), colData);
        //itemUpdate.
        itemUpdate.updExpression(COL_TRADE_ITEM_QUANTITY, SqlUtils.constant(
                BeeUtils.unbox(parentItem.getLong(parentItems.getColumnIndex(COL_TRADE_ITEM_QUANTITY)))
                        + BeeUtils.unbox(itemRow.getLong(0, 1))));

        itemsResp = qs.updateDataWithResponse(itemUpdate);
      } else {

        SqlInsert itemInsert = new SqlInsert(TBL_TRADE_ACT_ITEMS)
            .addConstant(COL_TRADE_ACT, actId)
            .addConstant(COL_TA_PARENT, parentItem.getLong(itemActIndex));
        appendInsertConstants(itemInsert, parentItems, parentItem.getId(), colData);

       itemsResp = qs.insertDataWithResponse(itemInsert);
      }
        if (itemsResp.hasErrors()) {
          return itemsResp;
        }
    }

    return ResponseObject.response(parentItems.getNumberOfRows());
  }

  private ResponseObject restoreActStates(RequestInfo req) {

    Set<Long> delIds = DataUtils.parseIdSet(req.getParameter(VAR_ID_LIST));

    if (BeeUtils.isEmpty(delIds)) {
      return ResponseObject.error(req.getSubService(), "Parameter", VAR_ID_LIST, "isEmpty");
    }

    Long retActStatus = prm.getRelation(PRM_RETURNED_ACT_STATUS);
    Long combActStatus = prm.getRelation(PRM_COMBINED_ACT_STATUS);
    Set<Long> predefinedStatuses = new HashSet<>();

    predefinedStatuses.add(retActStatus);
    predefinedStatuses.add(combActStatus);
    predefinedStatuses.remove(null);

    if (BeeUtils.isEmpty(predefinedStatuses)) {
      logger.warning("Restoring act states not predefined act statuses in parameters",
          PRM_RETURNED_ACT_STATUS, PRM_COMBINED_ACT_STATUS);
      return ResponseObject.emptyResponse();
    }

    Set<Long> acts = qs.getDistinctLongs(TBL_TRADE_ACTS, COL_TA_PARENT,
        SqlUtils.and(
            SqlUtils.inList(TBL_TRADE_ACTS, sys.getIdName(TBL_TRADE_ACTS), delIds),
            SqlUtils.equals(TBL_TRADE_ACTS, COL_TRADE_KIND, TradeActKind.RETURN.ordinal())));

    acts.remove(null);

    IsCondition idFilter = BeeUtils.isEmpty(acts)
        ? SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_RETURN, delIds)
        : sys.idInList(TBL_TRADE_ACTS, acts);

          /* Restoring primary act statuses */
    SqlUpdate query = new SqlUpdate(TBL_TRADE_ACTS)
        .addConstant(COL_TA_STATUS,
            new SqlSelect()
                .addFields(TBL_TRADE_OPERATIONS, COL_TRADE_OPERATION_STATUS)
                .addFrom(TBL_TRADE_OPERATIONS)
                .setWhere(
                    SqlUtils.equals(TBL_TRADE_OPERATIONS, sys.getIdName(TBL_TRADE_OPERATIONS),
                        SqlUtils.field(TBL_TRADE_ACTS, COL_TA_OPERATION)
                    )
                )
        )
        .setWhere(
            SqlUtils.and(idFilter,
                SqlUtils.not(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_KIND,
                    Lists.newArrayList(TradeActKind.CONTINUOUS.ordinal(),
                        TradeActKind.RETURN.ordinal()))),
                SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_STATUS, predefinedStatuses)
            )
        );

    ResponseObject resp = qs.updateDataWithResponse(query);

    if (resp.hasErrors()) {
      return resp;
    }

          /* if updated any row of primary acts then states of act is restored */
    if (resp.getResponseAsInt() > 0) {
      return resp;
    }

          /* if multi-return act derived from continuous */
    acts = qs.getDistinctLongs(TBL_TRADE_ACT_ITEMS, COL_TA_PARENT,
        SqlUtils.inList(TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT, delIds));

    acts.remove(null);

    if (BeeUtils.isEmpty(acts) || !DataUtils.isId(retActStatus)) {
      return resp;
    }

    query = new SqlUpdate(TBL_TRADE_ACTS)
        .addConstant(COL_TA_STATUS, combActStatus)
        .setWhere(
            SqlUtils.and(sys.idInList(TBL_TRADE_ACTS, acts),
                SqlUtils.not(SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_KIND,
                    Lists.newArrayList(TradeActKind.CONTINUOUS.ordinal(),
                        TradeActKind.RETURN.ordinal()))),
                SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_STATUS, retActStatus)));

    resp = qs.updateDataWithResponse(query);

    /* Reverting Continuous act states */
    Long contActStatus = prm.getRelation(PRM_CONTINUOUS_ACT_STATUS);

    if (!DataUtils.isId(contActStatus) || resp.hasErrors()) {
      return resp;
    }

    query = new SqlUpdate(TBL_TRADE_ACTS)
        .addConstant(COL_TA_STATUS, contActStatus)
        /* Transrifus. Force lock CTA due instead full return edition */
        .addConstant(COL_TA_CONTINUOUS, SqlUtils.field(TBL_TRADE_ACTS, COL_TA_RETURN))
        .setWhere(
            SqlUtils.and(
                SqlUtils.inList(TBL_TRADE_ACTS, COL_TA_CONTINUOUS, delIds),
                SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_KIND, TradeActKind.CONTINUOUS.ordinal())
            ));

    resp = qs.updateDataWithResponse(query);
    return resp;
  }

  private ResponseObject saveActAsTemplate(RequestInfo reqInfo) {
    Long actId = reqInfo.getParameterLong(COL_TRADE_ACT);
    if (!DataUtils.isId(actId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TRADE_ACT);
    }

    String name = reqInfo.getParameter(COL_TA_TEMPLATE_NAME);
    if (BeeUtils.isEmpty(name)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TA_TEMPLATE_NAME);
    }

    if (qs.sqlExists(TBL_TRADE_ACT_TEMPLATES, COL_TA_TEMPLATE_NAME, name)) {
      return ResponseObject.error(usr.getDictionary().valueExists(name));
    }

    SimpleRow actRow = qs.getRow(TBL_TRADE_ACTS, actId);
    if (actRow == null) {
      String msg = BeeUtils.joinWords(reqInfo.getService(), COL_TRADE_ACT, actId, "not found");
      logger.severe(msg);
      return ResponseObject.error(msg);
    }

    SqlInsert insert = new SqlInsert(TBL_TRADE_ACT_TEMPLATES)
        .addConstant(COL_TA_TEMPLATE_NAME, name);

    for (String colName : actRow.getColumnNames()) {
      String value = actRow.getValue(colName);
      if (!BeeUtils.isEmpty(value) && sys.hasField(TBL_TRADE_ACT_TEMPLATES, colName)) {
        insert.addConstant(colName, value);
      }
    }

    long templateId = qs.insertData(insert);
    if (!DataUtils.isId(templateId)) {
      return ResponseObject.error(reqInfo.getService(), "cannot create template");
    }

    saveActChildrenToTemplate(actId, templateId, TBL_TRADE_ACT_ITEMS, TBL_TRADE_ACT_TMPL_ITEMS);
    saveActChildrenToTemplate(actId, templateId, TBL_TRADE_ACT_SERVICES,
        TBL_TRADE_ACT_TMPL_SERVICES);

    BeeRowSet data = qs.getViewData(VIEW_TRADE_ACT_TEMPLATES, Filter.compareId(templateId));
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.error(reqInfo.getService(), "template is dead");
    } else {
      return ResponseObject.response(data.getRow(0));
    }
  }

  private void saveActChildrenToTemplate(long actId, long templateId,
      String sourceTable, String targetTable) {

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addAllFields(sourceTable)
        .addFrom(sourceTable)
        .setWhere(SqlUtils.equals(sourceTable, COL_TRADE_ACT, actId)));

    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        SqlInsert insert = new SqlInsert(targetTable)
            .addConstant(COL_TRADE_ACT_TEMPLATE, templateId);

        for (String colName : row.getColumnNames()) {
          String value = row.getValue(colName);
          if (!BeeUtils.isEmpty(value) && sys.hasField(targetTable, colName)) {
            insert.addConstant(colName, value);
          }
        }

        long id = qs.insertData(insert);
        if (!DataUtils.isId(id)) {
          break;
        }
      }
    }
  }

  private ResponseObject splitActServices(RequestInfo reqInfo) {
    Long actId = reqInfo.getParameterLong(COL_TRADE_ACT);
    if (!DataUtils.isId(actId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TRADE_ACT);
    }

    Long time = reqInfo.getParameterLong(COL_TA_DATE);
    if (!BeeUtils.isPositive(time)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TA_DATE);
    }

    return splitActServices(actId, time);
  }

  private ResponseObject splitActServices(long actId, long time) {
    JustDate date = new DateTime(time).getDate();

    String idName = sys.getIdName(TBL_TRADE_ACT_SERVICES);

    int result = 0;

    List<String> fields = Arrays.asList(COL_TRADE_ACT, COL_TA_ITEM, COL_TA_SERVICE_TO,
        COL_TA_SERVICE_TARIFF, COL_TA_SERVICE_FACTOR, COL_TA_SERVICE_DAYS,
        COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
        COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC, COL_TRADE_DISCOUNT,
        COL_TRADE_SUPPLIER, COL_COST_AMOUNT);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_ACT_SERVICES, idName)
        .addFields(TBL_TRADE_ACT_SERVICES, fields)
        .addFrom(TBL_TRADE_ACT_SERVICES)
        .addFromLeft(TBL_ITEMS,
            sys.joinTables(TBL_ITEMS, TBL_TRADE_ACT_SERVICES, COL_TA_ITEM))
        .setWhere(
            SqlUtils.and(
                SqlUtils.equals(TBL_TRADE_ACT_SERVICES, COL_TRADE_ACT, actId),
                SqlUtils.notNull(TBL_ITEMS, COL_TIME_UNIT),
                SqlUtils.positive(TBL_TRADE_ACT_SERVICES, COL_TRADE_ITEM_QUANTITY),
                SqlUtils.or(
                    SqlUtils.isNull(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_FROM),
                    SqlUtils.less(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_FROM, date)),
                SqlUtils.or(
                    SqlUtils.isNull(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_TO),
                    SqlUtils.more(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_TO, date))))
        .addOrder(TBL_TRADE_ACT_SERVICES, idName);

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      double itemTotal = totalActItems(actId);
      int priceScale = sys.getFieldScale(TBL_TRADE_ACT_SERVICES, COL_TRADE_ITEM_PRICE);

      for (SimpleRow row : data) {
        SqlInsert insert = new SqlInsert(TBL_TRADE_ACT_SERVICES)
            .addConstant(COL_TA_SERVICE_FROM, date);

        Double tariff = row.getDouble(COL_TA_SERVICE_TARIFF);
        Double quantity = row.getDouble(COL_TRADE_ITEM_QUANTITY);
        Double price;
        if (BeeUtils.isPositive(itemTotal) && BeeUtils.isPositive(tariff)) {
          price = TradeActUtils.calculateServicePrice(null, null, itemTotal, tariff, quantity,
            priceScale);
        } else {
          price = null;
        }

        for (String field : fields) {
          if (COL_TRADE_ITEM_PRICE.equals(field) && BeeUtils.isPositive(price)) {
            insert.addConstant(field, price);
          } else {
            String value = row.getValue(field);
            if (value != null) {
              insert.addConstant(field, value);
            }
          }
        }

        ResponseObject response = qs.insertDataWithResponse(insert);
        if (response.hasErrors()) {
          return response;
        }

        SqlUpdate update = new SqlUpdate(TBL_TRADE_ACT_SERVICES)
            .addConstant(COL_TA_SERVICE_TO, date)
            .setWhere(SqlUtils.equals(TBL_TRADE_ACT_SERVICES, idName, row.getLong(idName)));

        qs.updateData(update);

        result++;
      }
    }

    return ResponseObject.response(result);
  }

  private void syncERP(Timer timer) {
    if (cb.isParameterTimer(timer, PRM_SYNC_ERP_DATA)) {
      logger.info("Starting: syncERPData");
      syncERPData();
    }

    if (cb.isParameterTimer(timer, PRM_SYNC_ERP_STOCK)) {
      logger.info("Starting: syncERPStock");
      syncERPStock();
    }
  }

  private void syncERPData() {
    String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
    String remoteLogin = prm.getText(PRM_ERP_LOGIN);
    String remotePassword = prm.getText(PRM_ERP_PASSWORD);

    SimpleRowSet rs;
    // Company Advances
    try {
      rs = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
          .getSQLData("SELECT gav_avans FROM adm_par", new String[] {"gav_avans"});

      String avansSask = rs.getValue(0, "gav_avans");

      if (!BeeUtils.isEmpty(avansSask)) {
        rs = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
            .getSQLData("SELECT klientai.klientas AS kl, klientai.kodas AS kd,"
                + " SUM(CASE WHEN debetas LIKE '" + avansSask
                + "%' THEN (-1) ELSE 1 END * suma)"
                + " AS av"
                + " FROM finans INNER JOIN klientai ON klientai.klientas = finans.klientas"
                + " WHERE debetas LIKE '" + avansSask + "%' OR kreditas LIKE '" + avansSask
                + "%'"
                + " GROUP BY klientai.klientas, klientai.kodas HAVING av > 0",
                new String[] {"kl", "kd", "av"});
      } else {
        rs = null;
      }
    } catch (BeeException e) {
      logger.error(e);
      return;
    }
    if (rs != null) {
      qs.updateData(new SqlUpdate(TBL_COMPANIES)
          .addConstant("ExternalAdvance", null));

      for (SimpleRow row : rs) {
        IsCondition wh = SqlUtils.equals(TBL_COMPANIES, COL_COMPANY_NAME, row.getValue("kl"));

        if (!BeeUtils.isEmpty(row.getValue("kd"))) {
          wh = SqlUtils.or(wh,
              SqlUtils.equals(TBL_COMPANIES, COL_COMPANY_CODE, row.getValue("kd")));
        }
        qs.updateData(new SqlUpdate(TBL_COMPANIES)
            .addConstant("ExternalAdvance", row.getValue("av"))
            .setWhere(wh));

      }
    }
    // Service Objects
    Map<Long, Long> objects = new HashMap<>();

    for (SimpleRow row : qs.getData(new SqlSelect()
        .addFields(TBL_SERVICE_OBJECTS, COL_ITEM_EXTERNAL_CODE)
        .addField(TBL_SERVICE_OBJECTS, sys.getIdName(TBL_SERVICE_OBJECTS), COL_SERVICE_OBJECT)
        .addFrom(TBL_SERVICE_OBJECTS)
        .setWhere(SqlUtils.notNull(TBL_SERVICE_OBJECTS, COL_ITEM_EXTERNAL_CODE)))) {

      objects.put(row.getLong(COL_ITEM_EXTERNAL_CODE), row.getLong(COL_SERVICE_OBJECT));
    }
    try {
      String sql =
          "SELECT rusis AS tp, valst_nr AS nr, car_id AS id, modelis AS md, invent_nr AS inr, "
              + "kebul_nr AS bnr, pag_metai AS yom, pardavejas AS sl FROM masinos";

      if (!BeeUtils.isEmpty(objects)) {
        sql += " WHERE car_id NOT IN(" + BeeUtils.joinItems(Lists.newArrayList(objects.keySet()))
            + ")";
      }
      rs = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
          .getSQLData(sql, new String[] {"tp", "nr", "id", "md", "inr", "bnr", "yom", "sl"});
    } catch (BeeException e) {
      logger.error(e);
      return;
    }
    if (rs != null) {
      Map<String, Long> categories = new HashMap<>();

      for (SimpleRow row : qs.getData(new SqlSelect()
          .addFields(TBL_SERVICE_TREE, COL_SERVICE_CATEGORY_NAME)
          .addField(TBL_SERVICE_TREE, sys.getIdName(TBL_SERVICE_TREE), COL_SERVICE_CATEGORY)
          .addFrom(TBL_SERVICE_TREE))) {

        categories.put(row.getValue(COL_SERVICE_CATEGORY_NAME), row.getLong(COL_SERVICE_CATEGORY));
      }
      for (SimpleRow row : rs) {
        String category = row.getValue("tp");

        if (!categories.containsKey(category)) {
          categories.put(category, qs.insertData(new SqlInsert(TBL_SERVICE_TREE)
              .addConstant(COL_SERVICE_CATEGORY_NAME, category)));
        }
        Long id = row.getLong("id");

        objects.put(id, qs.insertData(new SqlInsert(TBL_SERVICE_OBJECTS)
            .addConstant(COL_SERVICE_CATEGORY, categories.get(category))
            .addConstant(COL_SERVICE_ADDRESS, row.getValue("nr"))
            .addConstant(COL_ITEM_EXTERNAL_CODE, id)
            .addConstant(COL_SERVICE_MODEL, row.getValue("md"))
            .addConstant(COL_SERVICE_INVENT_NO, row.getValue("inr"))
            .addConstant(COL_SERVICE_BODY_NO, row.getValue("bnr"))
            .addConstant(COL_SERVICE_YEAR_OF_MANUFACTURE, row.getValue("yom"))
            .addConstant(COL_SERVICE_SELLER, row.getValue("sl"))));
      }
    }
    // Service Object Repair History
    SimpleRow lastIds = qs.getRow(new SqlSelect()
        .addMin(TBL_MAINTENANCE, COL_ITEM_EXTERNAL_CODE, "remCode")
        .addMax(TBL_MAINTENANCE, COL_ITEM_EXTERNAL_CODE, "apyvCode")
        .addFrom(TBL_MAINTENANCE));

    Map<String, String> cols = new LinkedHashMap<>();
    cols.put("dt", "data");
    cols.put("pr", "preke");
    cols.put("ar", "artikulas");
    cols.put("kk", "kiekis");
    cols.put("kn", "kaina");
    cols.put("vl", "valiuta");
    cols.put("ps", "pvm_stat");
    cols.put("pv", "pvm");
    cols.put("pm", "pvm_p_md");
    cols.put("pp", "pastaba");
    cols.put("ob", "car_id");
    cols.put("id", "remont_id * (-1)");

    StringBuilder sql = new StringBuilder("SELECT ");
    int c = 0;

    for (Entry<String, String> entry : cols.entrySet()) {
      if (c++ > 0) {
        sql.append(", ");
      }
      sql.append(entry.getValue()).append(" AS ").append(entry.getKey());
    }
    sql.append(" FROM tr_remon");

    long lastId = BeeUtils.unbox(lastIds.getLong("remCode"));

    if (lastId < 0) {
      sql.append(" WHERE remont_id > ").append(Math.abs(lastId));
    }
    lastId = BeeUtils.unbox(lastIds.getLong("apyvCode"));
    cols.put("id", "a_gr_id");

    for (int i = 0; i < 2; i++) {
      String col;
      String wh;

      if (i > 0) {
        col = "apyv_gr.car_id";
        wh = " WHERE apyv_gr.car_id IS NOT NULL";
      } else {
        col = "apyvarta.car_id";
        wh = " WHERE apyvarta.car_id IS NOT NULL AND apyv_gr.car_id IS NULL";
      }
      cols.put("ob", col);
      sql.append(" UNION ALL SELECT ");
      c = 0;

      for (Entry<String, String> entry : cols.entrySet()) {
        if (c++ > 0) {
          sql.append(", ");
        }
        sql.append(entry.getValue()).append(" AS ").append(entry.getKey());
      }
      sql.append(" FROM apyvarta INNER JOIN apyv_gr ON apyvarta.apyv_id = apyv_gr.apyv_id")
          .append(wh);

      if (lastId > 0) {
        sql.append(" AND a_gr_id > ").append(lastId);
      }
    }
    try {
      rs = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
          .getSQLData(sql.toString(), cols.keySet().toArray(new String[0]));
    } catch (BeeException e) {
      logger.error(e);
      return;
    }
    Map<String, Long> items = new HashMap<>();

    for (SimpleRow row : qs.getData(new SqlSelect()
        .addFields(TBL_ITEMS, COL_ITEM_EXTERNAL_CODE)
        .addField(TBL_ITEMS, sys.getIdName(TBL_ITEMS), COL_ITEM)
        .addFrom(TBL_ITEMS)
        .setWhere(SqlUtils.notNull(TBL_ITEMS, COL_ITEM_EXTERNAL_CODE)))) {

      items.put(row.getValue(COL_ITEM_EXTERNAL_CODE), row.getLong(COL_ITEM));
    }
    if (rs != null) {
      Map<String, Long> currencies = new HashMap<>();

      for (SimpleRow row : qs.getData(new SqlSelect()
          .addFields(TBL_CURRENCIES, COL_CURRENCY_NAME)
          .addField(TBL_CURRENCIES, sys.getIdName(TBL_CURRENCIES), COL_CURRENCY)
          .addFrom(TBL_CURRENCIES))) {

        currencies.put(row.getValue(COL_CURRENCY_NAME), row.getLong(COL_CURRENCY));
      }
      List<SimpleRow> missing = new ArrayList<>();
      Set<String> missingItems = new HashSet<>();

      for (SimpleRow row : rs) {
        String item = row.getValue("pr");

        if (!items.containsKey(item)) {
          missing.add(row);
          missingItems.add(item);
          continue;
        }
        SqlInsert insert = new SqlInsert(TBL_MAINTENANCE)
            .addConstant(COL_SERVICE_OBJECT, objects.get(row.getLong("ob")))
            .addConstant(COL_ITEM_EXTERNAL_CODE, row.getLong("id"))
            .addConstant(COL_MAINTENANCE_DATE, parseDateTime(row.getValue("dt"),
              usr.getDateOrdering()))
            .addConstant(COL_MAINTENANCE_ITEM, items.get(item))
            .addConstant(COL_TRADE_ITEM_QUANTITY, row.getDouble("kk"))
            .addConstant("Description", row.getValue("ar"))
            .addConstant(COL_MAINTENANCE_NOTES, row.getValue("pp"));

        String curr = row.getValue("vl");

        if (currencies.containsKey(curr)) {
          insert.addConstant(COL_TRADE_ITEM_PRICE, row.getDouble("kn"))
              .addConstant(COL_CURRENCY, currencies.get(curr))
              .addConstant(COL_TRADE_VAT_PLUS,
                  BeeUtils.same(row.getValue("ps"), "S") ? true : null)
              .addConstant(COL_TRADE_VAT, row.getDouble("pv"))
              .addConstant(COL_TRADE_VAT_PERC, BeeUtils.isEmpty(row.getValue("pm")) ? null : true);
        }
        qs.insertData(insert);
      }
      if (!BeeUtils.isEmpty(missingItems)) {
        try {
          rs = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
              .getSQLData("SELECT grupe AS gr, preke AS pr, pavad AS pv, mato_vien AS mv,"
                  + " likutis AS lk FROM prekes WHERE preke IN(" + BeeUtils.joinItems(missingItems)
                  + ")", new String[] {"gr", "pr", "pv", "mv", "lk"});

        } catch (BeeException e) {
          logger.error(e);
          rs = null;
        }
        if (rs != null) {
          Map<String, Long> units = new HashMap<>();
          Map<String, Long> categories = new HashMap<>();

          for (SimpleRow row : qs.getData(new SqlSelect()
              .addFields(TBL_UNITS, COL_UNIT_NAME)
              .addField(TBL_UNITS, sys.getIdName(TBL_UNITS), COL_UNIT)
              .addFrom(TBL_UNITS))) {

            units.put(row.getValue(COL_UNIT_NAME), row.getLong(COL_UNIT));
          }
          for (SimpleRow row : qs.getData(new SqlSelect()
              .addFields(TBL_ITEM_CATEGORY_TREE, COL_CATEGORY_NAME)
              .addField(TBL_ITEM_CATEGORY_TREE, sys.getIdName(TBL_ITEM_CATEGORY_TREE),
                  COL_CATEGORY)
              .addFrom(TBL_ITEM_CATEGORY_TREE))) {

            categories.put(row.getValue(COL_CATEGORY_NAME), row.getLong(COL_CATEGORY));
          }
          for (SimpleRow row : rs) {
            String item = row.getValue("pr");
            String unit = row.getValue("mv");
            String category = row.getValue("gr");

            if (!units.containsKey(unit)) {
              units.put(unit, qs.insertData(new SqlInsert(TBL_UNITS)
                  .addConstant(COL_UNIT_NAME, unit)));
            }
            if (!categories.containsKey(category)) {
              categories.put(category, qs.insertData(new SqlInsert(TBL_ITEM_CATEGORY_TREE)
                  .addConstant(COL_CATEGORY_NAME, category)));
            }
            items.put(item, qs.insertData(new SqlInsert(TBL_ITEMS)
                .addConstant(COL_ITEM_NAME, row.getValue("pv"))
                .addConstant(COL_ITEM_EXTERNAL_CODE, item)
                .addConstant(COL_UNIT, units.get(unit))
                .addConstant(COL_ITEM_IS_SERVICE,
                    BeeUtils.isEmpty(row.getValue("lk")) ? true : null)));

            qs.insertData(new SqlInsert(TBL_ITEM_CATEGORIES)
                .addConstant(COL_ITEM, items.get(item))
                .addConstant(COL_CATEGORY, categories.get(category)));
          }
          for (SimpleRow row : missing) {
            SqlInsert insert = new SqlInsert(TBL_MAINTENANCE)
                .addConstant(COL_SERVICE_OBJECT, objects.get(row.getLong("ob")))
                .addConstant(COL_ITEM_EXTERNAL_CODE, row.getLong("id"))
                .addConstant(COL_MAINTENANCE_DATE, parseDateTime(row.getValue("dt"),
                  usr.getDateOrdering()))
                .addConstant(COL_MAINTENANCE_ITEM, items.get(row.getValue("pr")))
                .addConstant(COL_TRADE_ITEM_QUANTITY, row.getDouble("kk"))
                .addConstant("Description", row.getValue("ar"))
                .addConstant(COL_MAINTENANCE_NOTES, row.getValue("pp"));

            String curr = row.getValue("vl");

            if (currencies.containsKey(curr)) {
              insert.addConstant(COL_TRADE_ITEM_PRICE, row.getDouble("kn"))
                  .addConstant(COL_CURRENCY, currencies.get(curr))
                  .addConstant(COL_TRADE_VAT_PLUS,
                      BeeUtils.same(row.getValue("ps"), "S") ? true : null)
                  .addConstant(COL_TRADE_VAT, row.getDouble("pv"))
                  .addConstant(COL_TRADE_VAT_PERC,
                      BeeUtils.isEmpty(row.getValue("pm")) ? null : true);
            }
            qs.insertData(insert);
          }
        }
      }
    }
    // Item Stocks
    try {
      rs = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
          .getSQLData("SELECT preke AS pr, sum(kiekis) AS lk"
              + " FROM likuciai GROUP BY preke HAVING lk > 0",
              new String[] {"pr", "lk"});
    } catch (BeeException e) {
      logger.error(e);
      return;
    }
    if (rs != null) {
      qs.updateData(new SqlUpdate(TBL_ITEMS)
          .addConstant("ExternalStock", null));

      for (Entry<String, Long> entry : items.entrySet()) {
        String stock = rs.getValueByKey("pr", entry.getKey(), "lk");

        if (!BeeUtils.isEmpty(stock)) {
          qs.updateData(new SqlUpdate(TBL_ITEMS)
              .addConstant("ExternalStock", stock)
              .setWhere(sys.idEquals(TBL_ITEMS, entry.getValue())));
        }
      }
    }

    syncERPTurnovers(remoteAddress, remoteLogin, remotePassword);

  }

  private void syncERPTurnovers(String remoteAddress, String remoteLogin, String remotePassword) {
    // Turnovers
    try {
      SimpleRowSet erpCompanies =
          ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
              .getSQLData("SELECT klientai.klientas AS klientas, klientai.kodas AS kodas"
                  + " FROM klientai"
                  + " WHERE klientai.kodas IS NOT NULL",
                  new String[] {"klientas", "kodas"});
      // @deprecated due 24 month turnover limit
      // ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
      // .getClients();

      if (erpCompanies.isEmpty()) {
        logger.info("Finish import Debts. Clients set from ERP is empty");
        return;
      }

      Map<String, Long> companies =
          getReferences(TBL_COMPANIES, COL_COMPANY_CODE, SqlUtils.notNull(
              TBL_COMPANIES, COL_COMPANY_CODE));

      if (companies.isEmpty()) {
        logger.info("Finish import Debts. Clients set from system by code is empty");
        return;
      }

      SimpleRowSet erpTurnovers =
          ButentWS.connect(remoteAddress, remoteLogin, remotePassword).getTurnovers(
              null, null, BeeConst.STRING_EMPTY);

      if (erpTurnovers.isEmpty()) {
        logger.info("Finish import Turnovers. Turnovers set from ERP is empty");
        return;
      }

      Map<String, Long> compIdByName = new HashMap<>();

      for (String code : companies.keySet()) {
        String name = erpCompanies.getValueByKey("kodas", code, "klientas");

        if (!BeeUtils.isEmpty(name)) {
          compIdByName.put(name, companies.get(code));
        }
      }

      Map<String, Long> series = getReferences(TBL_SALES_SERIES, COL_SERIES_NAME);
      Map<String, Long> currencies = getReferences(TBL_CURRENCIES, COL_CURRENCY_NAME);
      Map<String, Long> users = getReferences(TBL_USERS, COL_EMPLOYER_ID, SqlUtils.notNull(
          TBL_USERS, COL_EMPLOYER_ID));
      Map<String, JustDate> lastestPayments = new HashMap<>();

      SqlSelect salesSelect = new SqlSelect()
          .addFields(TBL_ERP_SALES, COL_TRADE_ERP_INVOICE)
          .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_ERP_SALES, COL_TRADE_DEBT), 0), COL_TRADE_DEBT)
          .addFrom(TBL_ERP_SALES);

      SimpleRowSet sales = qs.getData(salesSelect);

      for (SimpleRow erpInvoice : erpTurnovers) {

        Double debt = BeeUtils.toDoubleOrNull(sales.getValueByKey(COL_TRADE_ERP_INVOICE, erpInvoice
            .getValue("dokumentas"), COL_TRADE_DEBT));

        lastestPayments.put(erpInvoice.getValue("gavejas"), BeeUtils.max(lastestPayments.get(
            erpInvoice.getValue("gavejas")), parseDate(erpInvoice.getValue("apm_data"),
          usr.getDateOrdering())));

        if (BeeUtils.unbox(debt) == BeeUtils.unbox(erpInvoice.getDouble("skola_w"))) {
          continue;
        }

        Long companyId = compIdByName.get(erpInvoice.getValue("gavejas"));

        if (!DataUtils.isId(companyId)) {
          logger.warning("ERP Sync turnovers", erpInvoice.getValue("gavejas"), "not found");
          continue;
        }

        Long currencyId = currencies.get(erpInvoice.getValue("viso_val"));

        if (!DataUtils.isId(currencyId)) {
          logger.warning("ERP Sync turnovers", erpInvoice.getValue("gavejas"), erpInvoice.getValue(
              "viso_val"), "not found");
          continue;
        }

        String docSeries = erpInvoice.getValue("dok_serija");

        if (!BeeUtils.isEmpty(docSeries) && !series.containsKey(docSeries)) {
          series.put(docSeries, qs.insertData(
              new SqlInsert(TBL_SALES_SERIES)
                  .addConstant(COL_SERIES_NAME, docSeries)));
        }

        Long seriesId = series.get(docSeries);
        Long userId = users.get(erpInvoice.getValue("manager"));

        SqlUpdate updateRow =
            new SqlUpdate(TBL_ERP_SALES)
                .addConstant(COL_TRADE_DATE,
                    parseDate(erpInvoice.getValue("data"), usr.getDateOrdering()))
                .addConstant(COL_TRADE_CUSTOMER, companyId)
                .addConstant(COL_TRADE_AMOUNT, erpInvoice.getValue("viso"))
                .addConstant(COL_TRADE_SALE_SERIES, seriesId)
                .addConstant(COL_TRADE_INVOICE_NO, erpInvoice.getValue("kitas_dok"))
                .addConstant(COL_TRADE_CURRENCY, currencyId)
                .addConstant(COL_TRADE_MANAGER, userId)
                .addConstant(COL_TRADE_PAID, BeeUtils.unbox(erpInvoice.getDouble("apm_suma")))
                .addConstant(COL_TRADE_DEBT, BeeUtils.unbox(erpInvoice.getDouble("skola_w")))
                .addConstant(COL_TRADE_PAYMENT_TIME, parseDate(erpInvoice.getValue(
                    "apm_data"), usr.getDateOrdering()))
                .addConstant(COL_TRADE_TERM, parseDate(erpInvoice.getValue(
                    "terminas"), usr.getDateOrdering()))
                .setWhere(
                    SqlUtils.equals(TBL_ERP_SALES, COL_TRADE_ERP_INVOICE, erpInvoice.getValue(
                        "dokumentas")));
        int updateCount = qs.updateData(updateRow);

        if (updateCount == 0) {

          SqlInsert insertRow =
              new SqlInsert(TBL_ERP_SALES)
                  .addConstant(COL_TRADE_DATE,
                      parseDate(erpInvoice.getValue("data"), usr.getDateOrdering()))
                  .addConstant(COL_TRADE_CUSTOMER, companyId)
                  .addConstant(COL_TRADE_AMOUNT, erpInvoice.getValue("viso"))
                  .addConstant(COL_TRADE_SALE_SERIES, seriesId)
                  .addConstant(COL_TRADE_INVOICE_NO, erpInvoice.getValue("kitas_dok"))
                  .addConstant(COL_TRADE_ERP_INVOICE, erpInvoice.getValue("dokumentas"))
                  .addConstant(COL_TRADE_CURRENCY, currencyId)
                  .addConstant(COL_TRADE_MANAGER, userId)
                  .addConstant(COL_TRADE_PAID, BeeUtils.unbox(erpInvoice.getDouble("apm_suma")))
                  .addConstant(COL_TRADE_DEBT, BeeUtils.unbox(erpInvoice.getDouble("skola_w")))
                  .addConstant(COL_TRADE_PAYMENT_TIME, parseDate(erpInvoice.getValue(
                      "apm_data"), usr.getDateOrdering()))
                  .addConstant(COL_TRADE_TERM, parseDate(erpInvoice.getValue(
                      "terminas"), usr.getDateOrdering()));
          qs.insertData(insertRow);
        }
      }

      updateLastestPayments(lastestPayments, compIdByName);
    } catch (BeeException e) {
      logger.error(e);
    }
  }

  private void syncERPStock() {
    // Item Stocks

    String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
    String remoteLogin = prm.getText(PRM_ERP_LOGIN);
    String remotePassword = prm.getText(PRM_ERP_PASSWORD);

    SimpleRowSet rs;

    Map<String, Long> items = new HashMap<>();

    for (SimpleRow row : qs.getData(new SqlSelect()
        .addFields(TBL_ITEMS, COL_ITEM_EXTERNAL_CODE)
        .addField(TBL_ITEMS, sys.getIdName(TBL_ITEMS), COL_ITEM)
        .addFrom(TBL_ITEMS)
        .setWhere(SqlUtils.notNull(TBL_ITEMS, COL_ITEM_EXTERNAL_CODE)))) {

      items.put(row.getValue(COL_ITEM_EXTERNAL_CODE), row.getLong(COL_ITEM));
    }

    try {
      rs = ButentWS.connect(remoteAddress, remoteLogin, remotePassword)
          .getSQLData("SELECT preke AS pr, sum(kiekis) AS lk"
              + " FROM likuciai GROUP BY preke HAVING lk > 0",
              new String[] {"pr", "lk"});
    } catch (BeeException e) {
      logger.error(e);
      return;
    }
    if (rs != null) {
      qs.updateData(new SqlUpdate(TBL_ITEMS)
          .addConstant("ExternalStock", null));

      for (Entry<String, Long> entry : items.entrySet()) {
        String stock = rs.getValueByKey("pr", entry.getKey(), "lk");

        if (!BeeUtils.isEmpty(stock)) {
          qs.updateData(new SqlUpdate(TBL_ITEMS)
              .addConstant("ExternalStock", stock)
              .setWhere(sys.idEquals(TBL_ITEMS, entry.getValue())));
        }
      }
    }
  }

  private double totalActItems(Long actId) {
    double result = BeeConst.DOUBLE_ZERO;

    BeeRowSet items = qs.getViewData(VIEW_TRADE_ACT_ITEMS,
        Filter.and(Filter.equals(COL_TRADE_ACT, actId),
            Filter.isPositive(COL_TRADE_ITEM_QUANTITY),
            Filter.isPositive(COL_TRADE_ITEM_PRICE)));

    if (!DataUtils.isEmpty(items)) {
      Map<Pair<Long, Long>, Double> returnedItems = getReturnedItems(actId);

      Totalizer itemTotalizer = new Totalizer(items.getColumns());

      for (BeeRow item : items) {
        Double total = itemTotalizer.getTotal(item);
        if (BeeUtils.isPositive(total)) {
          result += total;
        }

        if (!returnedItems.isEmpty()) {
          Double qty = returnedItems.get(Pair.of(actId, DataUtils.getLong(items, item,
              COL_TA_ITEM)));

          if (BeeUtils.isPositive(qty)) {
            item.setValue(items.getColumnIndex(COL_TRADE_ITEM_QUANTITY), qty);
            Double returned = itemTotalizer.getTotal(item);

            if (BeeUtils.isPositive(returned)) {
              result -= returned;
            }
          }
        }
      }
    }

    return result;
  }

  private void updateLastestPayments(final Map<String, JustDate> lastestPayments,
      Map<String, Long> compIdByName) {

    for (String companyName : lastestPayments.keySet()) {
      Long companyId = compIdByName.get(companyName);

      if (!DataUtils.isId(companyId)) {
        continue;
      }

      JustDate date = lastestPayments.get(companyName);

      SqlUpdate query = new SqlUpdate(TBL_COMPANIES)
          .addConstant(COL_SALE_LASTEST_PAYMENT, date)
          .setWhere(SqlUtils.and(
              SqlUtils.equals(TBL_COMPANIES, sys.getIdName(TBL_COMPANIES), companyId),
              SqlUtils.or(SqlUtils.less(TBL_COMPANIES, COL_SALE_LASTEST_PAYMENT, date), SqlUtils
                  .isNull(TBL_COMPANIES, COL_SALE_LASTEST_PAYMENT))));
      qs.updateData(query);
    }
  }

  private ResponseObject updateMultiReturnAct(Long actId, BeeRowSet parentActs, BeeRow fifoAct,
                                              List<String> fillCols) {
    int idxSeries = parentActs.getColumnIndex(COL_TA_SERIES);
    int idxNumber = parentActs.getColumnIndex(COL_TA_NUMBER);
    long sourceId = fifoAct.getId();

    Long series = fifoAct.getLong(idxSeries);
    Long operation = getDefaultOperation(TradeActKind.RETURN, series);
    String number = fifoAct.getString(idxNumber);

    Set<BeeColumn> copyColIndexes = new HashSet<>();
    for (String colName : VAR_COPY_TA_COLUMN_NAMES) {
      int index = parentActs.getColumnIndex(colName);
      if (index >= 0 && !fillCols.contains(colName)) {
        copyColIndexes.add(parentActs.getColumn(colName));
      }
    }

    SqlUpdate actUpdate =
        new SqlUpdate(TBL_TRADE_ACTS);

    if (!fillCols.contains(COL_TA_NUMBER)) {
      actUpdate.addConstant(COL_TA_NUMBER, number + "-"
          + getNextChildActNumber(TradeActKind.RETURN, series, sourceId, COL_TA_NUMBER));
    }

    if (operation != null && !fillCols.contains(COL_TA_OPERATION)) {
      actUpdate.addConstant(COL_TA_OPERATION, operation);
    }

    appendUpdateConstants(actUpdate, parentActs, sourceId, copyColIndexes);
    actUpdate.setWhere(sys.idEquals(TBL_TRADE_ACTS, actId));

    if (actUpdate.isEmpty()) {
      return ResponseObject.emptyResponse();
    }

    return qs.updateDataWithResponse(actUpdate);
  }

  private ResponseObject updateRentProjectAct(BeeRowSet parentActs, long fifoId, DateTime actDate) {
    ResponseObject response = ResponseObject.emptyResponse();
    if (parentActs.isEmpty()) {
      return response;
    }

    List<Long> parentIds = parentActs.getRowIds();
    Long combStatus = prm.getRelation(PRM_COMBINED_ACT_STATUS);
    Set<Long> lockActData = getRelatedLockActs(parentIds, fifoId);
    Long returnedActStatus = prm.getRelation(PRM_RETURNED_ACT_STATUS);

    BeeRowSet services = qs.getViewData(VIEW_TRADE_ACT_SERVICES,
            Filter.and(
                    Filter.or(
                            Filter.and(
                                    Filter.isNull(COL_TA_SERVICE_FROM),
                                    Filter.isNull(COL_TA_SERVICE_TO),
                                    Filter.isNull(COL_TIME_UNIT)
                            ),
                            Filter.and(
                                    Filter.isNull(COL_TA_SERVICE_TO),
                                    Filter.notNull(COL_TIME_UNIT)
                            )
                    ),
                    Filter.and(
                            Filter.any(COL_TRADE_ACT, lockActData),
                            Filter.isNull(COL_TA_CONTINUOUS)
                    )
            )
    );

    if (!DataUtils.isEmpty(services)) {
      response = createServices(fifoId, services, actDate);
    }

    if (response.hasErrors()) {
      return response;
    }

    if (DataUtils.isId(combStatus)) {
      IsCondition cond = SqlUtils.and(
          sys.idInList(TBL_TRADE_ACTS, lockActData),
          SqlUtils.notEqual(TBL_TRADE_ACTS, COL_TA_KIND, TradeActKind.RETURN.ordinal()));

      if (DataUtils.isId(returnedActStatus)) {
        cond = SqlUtils.and(cond, SqlUtils.notEqual(TBL_TRADE_ACTS, COL_TA_STATUS,
            returnedActStatus));
      }

      SqlUpdate query = new SqlUpdate(TBL_TRADE_ACTS)
          .addConstant(COL_TA_STATUS, combStatus)
          .setWhere(cond);

      response = qs.updateDataWithResponse(query);
    }

    if (response.hasErrors()) {
      return response;
    }

    return ResponseObject.response(fifoId);
  }
}
