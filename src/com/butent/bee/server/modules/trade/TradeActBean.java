package com.butent.bee.server.modules.trade;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

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
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.BeeConst;
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
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.Totalizer;
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
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class TradeActBean {

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

  public List<SearchResult> doSearch(String query) {
    Set<String> columns = Sets.newHashSet(COL_TRADE_ACT_NAME, COL_TA_NUMBER, COL_OPERATION_NAME,
        COL_STATUS_NAME, ALS_COMPANY_NAME, COL_COMPANY_OBJECT_NAME);
    return qs.getSearchResults(VIEW_TRADE_ACTS, Filter.anyContains(columns, query));
  }

  public ResponseObject doService(String service, RequestInfo reqInfo) {
    ResponseObject response;

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

      case SVC_RETURN_ACT_ITEMS:
        response = doReturnActItems(reqInfo);
        break;

      case SVC_SPLIT_ACT_SERVICES:
        response = splitActServices(reqInfo);
        break;

      case SVC_GET_ACTS_FOR_INVOICE:
        response = getActsForInvoice(reqInfo);
        break;

      case SVC_GET_SERVICES_FOR_INVOICE:
        response = getServicesForInvoice(reqInfo);
        break;

      case SVC_CREATE_ACT_INVOICE:
        response = createInvoice(reqInfo);
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

      default:
        String msg = BeeUtils.joinWords("service not recognized:", svc);
        logger.warning(msg);
        response = ResponseObject.error(msg);
    }

    return response;
  }

  public Collection<BeeParameter> getDefaultParameters() {
    String module = ModuleAndSub.of(Module.TRADE, SubModule.ACTS).getName();

    return Lists.newArrayList(
        BeeParameter.createText(module, PRM_IMPORT_TA_ITEM_RX, false, RX_IMPORT_ACT_ITEM),
        BeeParameter.createNumber(module, PRM_TA_NUMBER_LENGTH, false, 6),
        BeeParameter.createRelation(module, PRM_RETURNED_ACT_STATUS,
            TBL_TRADE_STATUSES, COL_STATUS_NAME));
  }

  public void init() {
    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void fillActNumber(ViewInsertEvent event) {
        if (event.isBefore(VIEW_TRADE_ACTS)
            && !DataUtils.contains(event.getColumns(), COL_TA_NUMBER)) {

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

          if (kind != null && kind.autoNumber() && DataUtils.isId(series)) {
            BeeColumn column = sys.getView(VIEW_TRADE_ACTS).getBeeColumn(COL_TA_NUMBER);
            String number = getNextActNumber(series, column.getPrecision());

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
              Map<Long, Double> returnedItems = getReturnedItems(actId);

              if (!returnedItems.isEmpty()) {
                for (BeeRow row : rowSet) {
                  if (actId.equals(row.getLong(actIndex))) {
                    Double qty = returnedItems.get(row.getLong(itemIndex));

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
    });
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

    if (kind.autoNumber()) {
      Long series = oldRow.getLong(rowSet.getColumnIndex(COL_TA_SERIES));
      int numberIndex = rowSet.getColumnIndex(COL_TA_NUMBER);

      if (DataUtils.isId(series) && oldRow.isNull(numberIndex)) {
        String number = getNextActNumber(series, rowSet.getColumn(numberIndex).getPrecision());
        if (!BeeUtils.isEmpty(number)) {
          newRow.setValue(numberIndex, number);
        }
      }
    }

    Long operation = getDefaultOperation(kind);
    newRow.setValue(rowSet.getColumnIndex(COL_TA_OPERATION), operation);

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

    row.setValue(rowSet.getColumnIndex(COL_TA_DATE), TimeUtils.nowMinutes());

    int index = rowSet.getColumnIndex(COL_TA_UNTIL);
    if (!row.isNull(index) && BeeUtils.isLess(row.getDateTime(index),
        TimeUtils.startOfNextMonth(TimeUtils.today()).getDateTime())) {
      row.clearCell(index);
    }

    row.clearCell(rowSet.getColumnIndex(COL_TA_NUMBER));

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

        qs.insertData(insert);
      }
    }

    return response;
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

  private TradeActKind getActKind(Long actId) {
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

  private Long getDefaultOperation(TradeActKind kind) {
    IsCondition where = SqlUtils.equals(TBL_TRADE_OPERATIONS, COL_OPERATION_KIND, kind.ordinal());

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_OPERATIONS, sys.getIdName(TBL_TRADE_OPERATIONS))
        .addFrom(TBL_TRADE_OPERATIONS)
        .setWhere(SqlUtils.and(where,
            SqlUtils.notNull(TBL_TRADE_OPERATIONS, COL_OPERATION_DEFAULT)));

    List<Long> operations = qs.getLongList(query);
    if (operations.size() == 1) {
      return operations.get(0);
    }

    query.setWhere(where);
    operations = qs.getLongList(query);

    return (operations.size() == 1) ? operations.get(0) : null;
  }

  private String getNextActNumber(long series, int maxLength) {
    IsCondition where = SqlUtils.and(SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_SERIES, series),
        SqlUtils.notNull(TBL_TRADE_ACTS, COL_TA_NUMBER));

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_ACTS, COL_TA_NUMBER)
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

  private ResponseObject getActsForInvoice(RequestInfo reqInfo) {
    String dFrom = reqInfo.getParameter(COL_TA_SERVICE_FROM);
    String dTo = reqInfo.getParameter(COL_TA_SERVICE_TO);

    Long company = reqInfo.getParameterLong(COL_TA_COMPANY);

    LongValue timeFrom =
        BeeUtils.isDigit(dFrom) ? new LongValue(TimeUtils.daysToTime(BeeUtils.toInt(dFrom))) : null;
    LongValue timeTo =
        BeeUtils.isDigit(dTo) ? new LongValue(TimeUtils.daysToTime(BeeUtils.toInt(dTo))) : null;

    CompoundFilter filter = Filter.and();
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

    filter.add(Filter.in(sys.getIdName(VIEW_TRADE_ACTS), VIEW_TRADE_ACT_SERVICES, COL_TRADE_ACT));

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

    Filter filter = Filter.and(Filter.any(COL_TRADE_ACT, actIds),
        Filter.isPositive(COL_TRADE_ITEM_QUANTITY));

    BeeRowSet services = qs.getViewData(VIEW_TRADE_ACT_SERVICES, filter);
    if (DataUtils.isEmpty(services)) {
      return ResponseObject.emptyResponse();
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

    return ResponseObject.response(services);
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
        .addFields(TBL_TRADE_ACTS, COL_TA_PARENT)
        .addFields(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM)
        .addSum(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY, ALS_RETURNED_QTY)
        .addFrom(TBL_TRADE_ACTS)
        .addFromInner(TBL_TRADE_ACT_ITEMS,
            sys.joinTables(TBL_TRADE_ACTS, TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT))
        .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_TRADE_ACTS, COL_TA_PARENT),
            SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_KIND, TradeActKind.RETURN.ordinal())))
        .addGroup(TBL_TRADE_ACTS, COL_TA_PARENT)
        .addGroup(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM);

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
    query.addFields(returnAlias, ALS_RETURNED_QTY);
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
                SqlUtils.field(tmp, ALS_RETURNED_QTY)))
        .setWhere(SqlUtils.positive(tmp, ALS_RETURNED_QTY));

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
          COL_TRADE_ITEM_QUANTITY, ALS_UNIT_NAME, ALS_RETURNED_QTY, ALS_REMAINING_QTY,
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
      query.addSum(tmp, ALS_RETURNED_QTY);
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

    BeeRowSet result = null;
    for (Long actId : acts) {
      BeeRowSet items = getRemainingItems(actId);

      if (!DataUtils.isEmpty(items)) {
        if (result == null) {
          result = items;
        } else {
          result.addRows(items.getRows());
        }
      }
    }

    if (result == null) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result);
    }
  }

  private ResponseObject doReturnActItems(RequestInfo reqInfo) {
    String input = reqInfo.getParameter(VIEW_TRADE_ACT_ITEMS);
    if (BeeUtils.isEmpty(input)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VIEW_TRADE_ACT_ITEMS);
    }

    BeeRowSet parentItems = BeeRowSet.restore(input);
    List<Long> parentIds = DataUtils.getDistinct(parentItems, COL_TRADE_ACT);
    if (BeeUtils.isEmpty(parentIds)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VIEW_TRADE_ACT_ITEMS);
    }

    BeeRowSet parentActs = qs.getViewData(VIEW_TRADE_ACTS, Filter.idIn(parentIds));
    if (DataUtils.isEmpty(parentActs)) {
      return ResponseObject.error(reqInfo.getService(), VIEW_TRADE_ACTS, parentIds, "not found");
    }

    Set<String> copyColNames = Sets.newHashSet(COL_TA_NAME, COL_TA_SERIES, COL_TA_NUMBER,
        COL_TA_COMPANY, COL_TA_CONTACT, COL_TA_OBJECT, COL_TA_CURRENCY, COL_TA_VEHICLE,
        COL_TA_DRIVER);

    Set<Integer> copyColIndexes = new HashSet<>();
    for (String colName : copyColNames) {
      int index = parentActs.getColumnIndex(colName);
      if (index >= 0) {
        copyColIndexes.add(index);
      }
    }

    int itemActIndex = parentItems.getColumnIndex(COL_TRADE_ACT);

    DateTime date = TimeUtils.nowMinutes();
    Long operation = getDefaultOperation(TradeActKind.RETURN);

    Long retStatus = prm.getRelation(PRM_RETURNED_ACT_STATUS);
    int statusIndex = parentActs.getColumnIndex(COL_TA_STATUS);

    for (BeeRow parentAct : parentActs) {
      long parentId = parentAct.getId();

      SqlInsert actInsert = new SqlInsert(TBL_TRADE_ACTS)
          .addConstant(COL_TA_KIND, TradeActKind.RETURN.ordinal())
          .addConstant(COL_TA_PARENT, parentId)
          .addConstant(COL_TA_DATE, date)
          .addConstant(COL_TA_MANAGER, usr.getCurrentUserId());

      if (operation != null) {
        actInsert.addConstant(COL_TA_OPERATION, operation);
      }

      for (int index : copyColIndexes) {
        String value = parentAct.getString(index);
        if (!BeeUtils.isEmpty(value)) {
          actInsert.addConstant(parentActs.getColumnId(index), value);
        }
      }

      ResponseObject actInsResponse = qs.insertDataWithResponse(actInsert);
      if (actInsResponse.hasErrors()) {
        return actInsResponse;
      }

      Long actId = actInsResponse.getResponseAsLong();

      for (BeeRow parentItem : parentItems) {
        if (Objects.equals(parentItem.getLong(itemActIndex), parentId)) {
          SqlInsert itemInsert = new SqlInsert(TBL_TRADE_ACT_ITEMS)
              .addConstant(COL_TRADE_ACT, actId);

          for (int index = 0; index < parentItems.getNumberOfColumns(); index++) {
            if (index != itemActIndex && parentItems.getColumn(index).isEditable()) {
              String value = parentItem.getString(index);
              if (!BeeUtils.isEmpty(value)) {
                itemInsert.addConstant(parentItems.getColumnId(index), value);
              }
            }
          }

          ResponseObject itemInsResponse = qs.insertDataWithResponse(itemInsert);
          if (itemInsResponse.hasErrors()) {
            return itemInsResponse;
          }
        }
      }

      boolean hasItems = !DataUtils.isEmpty(getRemainingItems(parentId));

      if (hasItems) {
        ResponseObject splitResponse = splitActServices(parentId, date.getTime());
        if (splitResponse.hasErrors()) {
          return splitResponse;
        }
      }

      if (!hasItems && DataUtils.isId(retStatus)
          && !retStatus.equals(parentAct.getLong(statusIndex))) {

        SqlUpdate update = new SqlUpdate(TBL_TRADE_ACTS)
            .addConstant(COL_TA_STATUS, retStatus)
            .setWhere(sys.idEquals(TBL_TRADE_ACTS, parentId));

        qs.updateData(update);
      }
    }

    return ResponseObject.response(parentActs.getNumberOfRows());
  }

  private ResponseObject getItemsForSelection(RequestInfo reqInfo) {
    TradeActKind kind = EnumUtils.getEnumByIndex(TradeActKind.class,
        reqInfo.getParameter(COL_TA_KIND));

    if (kind == null) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_TA_KIND);
    }

    Long actId = reqInfo.getParameterLong(COL_TRADE_ACT);
    Long warehouse = reqInfo.getParameterLong(COL_WAREHOUSE);

    String where = reqInfo.getParameter(Service.VAR_VIEW_WHERE);

    CompoundFilter filter = Filter.and();
    filter.add(Filter.isNull(COL_ITEM_IS_SERVICE));

    Set<Long> actItems = getActItems(actId);
    if (!actItems.isEmpty()) {
      filter.add(Filter.idNotIn(actItems));
    }

    if (!BeeUtils.isEmpty(where)) {
      filter.add(Filter.restore(where));
    }

    BeeRowSet items = qs.getViewData(VIEW_ITEMS, filter);
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

        List<BeeRow> hasStock = new ArrayList<>();
        List<BeeRow> noStock = new ArrayList<>();

        for (BeeRow row : items) {
          boolean has = false;

          if (stock.containsRow(row.getId())) {
            for (Map.Entry<Long, Double> entry : stock.row(row.getId()).entrySet()) {
              row.setProperty(PRP_WAREHOUSE_PREFIX + BeeUtils.toString(entry.getKey()),
                  BeeUtils.toString(entry.getValue(), scale));

              if (warehouse != null && warehouse.equals(entry.getKey())) {
                has = BeeUtils.isPositive(entry.getValue());
              }
            }
          }

          if (warehouse != null) {
            if (has) {
              hasStock.add(row);
            } else {
              noStock.add(row);
            }
          }
        }

        if (!hasStock.isEmpty() && !noStock.isEmpty()) {
          items.clearRows();
          items.addRows(hasStock);
          items.addRows(noStock);
        }

        items.setTableProperty(TBL_WAREHOUSES, DataUtils.buildIdList(stock.columnKeySet()));
      }
    }

    return ResponseObject.response(items);
  }

  private BeeRowSet getRemainingItems(long actId) {
    Filter filter = Filter.and(Filter.equals(COL_TRADE_ACT, actId),
        Filter.isPositive(COL_TRADE_ITEM_QUANTITY));

    BeeRowSet parentItems = qs.getViewData(VIEW_TRADE_ACT_ITEMS, filter);
    if (DataUtils.isEmpty(parentItems)) {
      return null;
    }

    Map<Long, Double> returnedItems = getReturnedItems(actId);
    if (BeeUtils.isEmpty(returnedItems)) {
      return parentItems;
    }

    BeeRowSet result = new BeeRowSet(parentItems.getViewName(), parentItems.getColumns());

    int itemIndex = parentItems.getColumnIndex(COL_TA_ITEM);
    int qtyIndex = parentItems.getColumnIndex(COL_TRADE_ITEM_QUANTITY);

    for (BeeRow parentRow : parentItems) {
      double qty = parentRow.getDouble(qtyIndex);
      Double returnedQty = returnedItems.get(parentRow.getLong(itemIndex));

      boolean found = BeeUtils.isPositive(returnedQty);
      if (found) {
        qty -= returnedQty;
      }

      if (BeeUtils.isPositive(qty)) {
        BeeRow row = DataUtils.cloneRow(parentRow);
        if (found) {
          row.setValue(qtyIndex, qty);
        }

        result.addRow(row);
      }
    }

    if (DataUtils.isEmpty(result)) {
      return null;
    } else {
      return result;
    }
  }

  private Map<Long, Double> getReturnedItems(long actId) {
    Map<Long, Double> result = new HashMap<>();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM)
        .addSum(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY)
        .addFrom(TBL_TRADE_ACTS)
        .addFromInner(TBL_TRADE_ACT_ITEMS,
            sys.joinTables(TBL_TRADE_ACTS, TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_PARENT, actId),
            SqlUtils.equals(TBL_TRADE_ACTS, COL_TA_KIND, TradeActKind.RETURN.ordinal())))
        .addGroup(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM);

    SimpleRowSet data = qs.getData(query);
    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        Long item = row.getLong(0);
        Double qty = row.getDouble(1);

        if (DataUtils.isId(item) && BeeUtils.isPositive(qty)) {
          result.put(item, qty);
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

    List<String> groupBy = NameUtils.toList(reqInfo.getParameter(Service.VAR_GROUP_BY));

    SqlSelect rangeQuery = new SqlSelect()
        .addFields(TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_ITEM)
        .addMin(TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_FROM)
        .addMax(TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_TO)
        .addFrom(TBL_TRADE_ACT_INVOICES)
        .addGroup(TBL_TRADE_ACT_INVOICES, COL_TA_INVOICE_ITEM);

    String rangeAlias = "rng_" + SqlUtils.uniqueName();

    HasConditions where = SqlUtils.and(SqlUtils.notNull(rangeAlias, COL_TA_INVOICE_FROM),
        SqlUtils.notNull(rangeAlias, COL_TA_INVOICE_TO));

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
    }

    query.addFields(TBL_SALE_ITEMS, COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE);
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

    SqlSelect plusQuery = getStockQuery(condition, true);
    SimpleRowSet plusData = qs.getData(plusQuery);

    if (!DataUtils.isEmpty(plusData)) {
      for (SimpleRow row : plusData) {
        Double qty = row.getDouble(COL_TRADE_ITEM_QUANTITY);
        if (BeeUtils.nonZero(qty)) {
          result.put(row.getLong(COL_TA_ITEM), row.getLong(COL_OPERATION_WAREHOUSE_TO), qty);
        }
      }
    }

    SqlSelect minusQuery = getStockQuery(condition, false);
    SimpleRowSet minusData = qs.getData(minusQuery);

    if (!DataUtils.isEmpty(minusData)) {
      for (SimpleRow row : minusData) {
        Double qty = row.getDouble(COL_TRADE_ITEM_QUANTITY);

        if (BeeUtils.nonZero(qty)) {
          Long item = row.getLong(COL_TA_ITEM);
          Long warehouse = row.getLong(COL_OPERATION_WAREHOUSE_FROM);

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

    return result;
  }

  private String getStock(IsCondition actCondition, IsCondition itemCondition, Long time,
      Collection<Long> warehouses, String colPrefix) {

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM)
        .addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_FROM, COL_OPERATION_WAREHOUSE_TO)
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
    if (itemCondition != null) {
      query.addFromInner(TBL_ITEMS,
          sys.joinTables(TBL_ITEMS, TBL_TRADE_ACT_ITEMS, COL_TA_ITEM));
      where.add(itemCondition);
    }

    if (time != null) {
      where.add(SqlUtils.less(TBL_TRADE_ACTS, COL_TA_DATE, time));
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
    query.addGroup(TBL_TRADE_OPERATIONS, COL_OPERATION_WAREHOUSE_FROM, COL_OPERATION_WAREHOUSE_TO);

    String tmp = qs.sqlCreateTemp(query);

    Set<Long> ids = qs.getNotNullLongSet(tmp, COL_OPERATION_WAREHOUSE_FROM);
    ids.addAll(qs.getNotNullLongSet(tmp, COL_OPERATION_WAREHOUSE_TO));

    if (!BeeUtils.isEmpty(warehouses)) {
      ids.retainAll(warehouses);
    }

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

  private String getMovement(IsCondition actCondition, IsCondition itemCondition,
      Long startTime, Long endTime, Collection<Long> warehouses) {

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

  private SqlSelect getStockQuery(IsCondition condition, boolean plus) {
    String colWarehouse = plus ? COL_OPERATION_WAREHOUSE_TO : COL_OPERATION_WAREHOUSE_FROM;

    return new SqlSelect()
        .addFields(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM)
        .addFields(TBL_TRADE_OPERATIONS, colWarehouse)
        .addSum(TBL_TRADE_ACT_ITEMS, COL_TRADE_ITEM_QUANTITY)
        .addFrom(TBL_TRADE_ACT_ITEMS)
        .addFromInner(TBL_TRADE_ACTS,
            sys.joinTables(TBL_TRADE_ACTS, TBL_TRADE_ACT_ITEMS, COL_TRADE_ACT))
        .addFromInner(TBL_TRADE_OPERATIONS,
            sys.joinTables(TBL_TRADE_OPERATIONS, TBL_TRADE_ACTS, COL_TA_OPERATION))
        .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_TRADE_OPERATIONS, colWarehouse), condition))
        .addGroup(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM)
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

    if (!categories.isEmpty()) {
      actCondition.add(SqlUtils.in(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, TBL_ITEM_CATEGORIES, COL_ITEM,
          SqlUtils.inList(TBL_ITEM_CATEGORIES, COL_CATEGORY, categories)));
    }
    if (!items.isEmpty()) {
      actCondition.add(SqlUtils.inList(TBL_TRADE_ACT_ITEMS, COL_TA_ITEM, items));
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
      endStock = getStock(actCondition, itemCondition, null, warehouses, PFX_END_STOCK);

    } else if (startDate == null) {
      endStock = getStock(actCondition, itemCondition, endDate, warehouses, PFX_END_STOCK);

    } else if (endDate == null) {
      startStock = getStock(actCondition, itemCondition, startDate, warehouses, PFX_START_STOCK);

    } else {
      startStock = getStock(actCondition, itemCondition, startDate, warehouses, PFX_START_STOCK);
      movement = getMovement(actCondition, itemCondition, startDate, endDate, warehouses);
      endStock = getStock(actCondition, itemCondition, endDate, warehouses, PFX_END_STOCK);
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
            SqlUtils.isNull(TBL_TRADE_ACTS, COL_TA_STATUS),
            SqlUtils.notNull(TBL_TRADE_STATUSES, COL_STATUS_ACTIVE)));

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
            SqlUtils.less(TBL_TRADE_ACT_SERVICES, COL_TA_SERVICE_FROM, endDate)));

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
        SqlUtils.in(TBL_TRADE_ACTS, actIdName, TBL_TRADE_ACT_SERVICES, COL_TRADE_ACT,
            serviceConditions)));

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
      query.addFields(tmp, COL_TRADE_ACT, COL_TRADE_ACT_NAME, COL_TA_DATE, COL_TA_UNTIL,
          COL_SERIES_NAME, COL_TA_NUMBER, COL_OPERATION_NAME, COL_STATUS_NAME,
          ALS_COMPANY_NAME, COL_COMPANY_OBJECT_NAME);

      if (hasManager) {
        query.addFields(tmp, COL_FIRST_NAME, COL_LAST_NAME);
      }

      query.addFields(tmp, ALS_ITEM_TOTAL, COL_TA_SERVICE_TARIFF,
          COL_TA_SERVICE_FROM, COL_TA_SERVICE_TO,
          COL_TA_ITEM, ALS_ITEM_NAME, COL_ITEM_ARTICLE, COL_TIME_UNIT,
          COL_TRADE_ITEM_QUANTITY, ALS_UNIT_NAME, COL_TRADE_ITEM_PRICE,
          COL_TA_SERVICE_FACTOR, COL_TA_SERVICE_DAYS, COL_TA_SERVICE_MIN,
          COL_TRADE_DISCOUNT, ALS_BASE_AMOUNT);

      query.addOrder(tmp, COL_TA_DATE, COL_TRADE_ACT, ALS_ITEM_NAME, COL_ITEM_ARTICLE, COL_TA_ITEM);

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

            order.add(ALS_ITEM_NAME);
            order.add(COL_ITEM_ARTICLE);
            order.add(COL_TA_ITEM);
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

      query.addSum(tmp, COL_TRADE_ITEM_QUANTITY);
      query.addSum(tmp, ALS_BASE_AMOUNT);
    }

    query.addFrom(tmp);
    query.setWhere(SqlUtils.positive(tmp, ALS_BASE_AMOUNT));

    SimpleRowSet data = qs.getData(query);
    qs.sqlDropTemp(tmp);

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(data);
    }
  }

  private void prepareTransferReport(String tmp, Long startDate, Long endDate, String idName) {
    SqlSelect query = new SqlSelect()
        .addFields(tmp, idName, COL_TA_DATE, COL_TA_UNTIL, COL_TA_SERVICE_FROM, COL_TA_SERVICE_TO,
            COL_TIME_UNIT, COL_TRADE_ITEM_QUANTITY, ALS_ITEM_TOTAL, COL_TA_SERVICE_TARIFF,
            COL_TRADE_ITEM_PRICE, COL_TA_SERVICE_FACTOR, COL_TA_SERVICE_DAYS, COL_TA_SERVICE_MIN,
            COL_TRADE_DISCOUNT)
        .addFrom(tmp);

    SimpleRowSet data = qs.getData(query);

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

      JustDate dateTo = row.getDate(COL_TA_SERVICE_TO);
      Range<DateTime> serviceRange = TradeActUtils.createServiceRange(
          row.getDate(COL_TA_SERVICE_FROM), dateTo, tu, reportRange, actRange);

      if (serviceRange != null) {
        SqlUpdate update = new SqlUpdate(tmp)
            .addConstant(COL_TA_SERVICE_FROM, serviceRange.lowerEndpoint().getTime())
            .addConstant(COL_TA_SERVICE_TO, serviceRange.upperEndpoint().getTime());

        Double quantity = row.getDouble(COL_TRADE_ITEM_QUANTITY);
        Double tariff = row.getDouble(COL_TA_SERVICE_TARIFF);
        Double price = row.getDouble(COL_TRADE_ITEM_PRICE);
        Double discount = row.getDouble(COL_TRADE_DISCOUNT);

        if (BeeUtils.isPositive(tariff)) {
          Double p = TradeActUtils.calculateServicePrice(price, dateTo,
              row.getDouble(ALS_ITEM_TOTAL), tariff, priceScale);

          if (BeeUtils.isPositive(p) && !p.equals(price)) {
            price = p;
            update.addConstant(COL_TRADE_ITEM_PRICE, p);
          }
        }

        Double factor = row.getDouble(COL_TA_SERVICE_FACTOR);

        if (tu != null) {
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

        update.setWhere(SqlUtils.equals(tmp, idName, row.getLong(idName)));
        qs.updateData(update);
      }
    }
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
        COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC, COL_TRADE_DISCOUNT);

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
        Double price;
        if (BeeUtils.isPositive(itemTotal) && BeeUtils.isPositive(tariff)) {
          price = TradeActUtils.calculateServicePrice(null, null, itemTotal, tariff, priceScale);
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

  private double totalActItems(Long actId) {
    double result = BeeConst.DOUBLE_ZERO;

    BeeRowSet items = qs.getViewData(VIEW_TRADE_ACT_ITEMS,
        Filter.and(Filter.equals(COL_TRADE_ACT, actId),
            Filter.isPositive(COL_TRADE_ITEM_QUANTITY),
            Filter.isPositive(COL_TRADE_ITEM_PRICE)));

    if (!DataUtils.isEmpty(items)) {
      Map<Long, Double> returnedItems = getReturnedItems(actId);

      Totalizer itemTotalizer = new Totalizer(items.getColumns());

      for (BeeRow item : items) {
        Double total = itemTotalizer.getTotal(item);
        if (BeeUtils.isPositive(total)) {
          result += total;
        }

        if (!returnedItems.isEmpty()) {
          Double qty = returnedItems.get(DataUtils.getLong(items, item, COL_TA_ITEM));

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
}
