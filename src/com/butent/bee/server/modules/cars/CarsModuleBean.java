package com.butent.bee.server.modules.cars;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.COL_BRANCH_NAME;
import static com.butent.bee.shared.modules.cars.CarsConstants.COL_DESCRIPTION;
import static com.butent.bee.shared.modules.cars.CarsConstants.COL_GROUP;
import static com.butent.bee.shared.modules.cars.CarsConstants.COL_GROUP_NAME;
import static com.butent.bee.shared.modules.cars.CarsConstants.COL_ORDINAL;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.Invocation;
import com.butent.bee.server.data.DataEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.trade.StockReservationsProvider;
import com.butent.bee.server.modules.trade.TradeModuleBean;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.IsQuery;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.Defaults;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.cars.Bundle;
import com.butent.bee.shared.modules.cars.ConfInfo;
import com.butent.bee.shared.modules.cars.Configuration;
import com.butent.bee.shared.modules.cars.Dimension;
import com.butent.bee.shared.modules.cars.Option;
import com.butent.bee.shared.modules.cars.Specification;
import com.butent.bee.shared.modules.trade.ItemQuantities;
import com.butent.bee.shared.modules.trade.TradeDocument;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class CarsModuleBean implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(CarsModuleBean.class);

  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;
  @EJB
  UserServiceBean usr;
  @EJB
  TradeModuleBean trd;

  @Override
  public ResponseObject doService(String service, RequestInfo reqInfo) {
    ResponseObject response;

    switch (service) {
      case SVC_GET_CONFIGURATION:
        response = getConfiguration(reqInfo.getParameterLong(COL_BRANCH));
        break;

      case SVC_SAVE_DIMENSIONS:
        Pair<String, String> pair = Pair.restore(reqInfo.getParameter(TBL_CONF_DIMENSIONS));

        response = saveDimensions(reqInfo.getParameterLong(COL_BRANCH),
            Codec.deserializeIdList(pair.getA()), Codec.deserializeIdList(pair.getB()));
        break;

      case SVC_SET_BUNDLE:
        response = setBundle(reqInfo.getParameterLong(COL_BRANCH),
            Bundle.restore(reqInfo.getParameter(COL_BUNDLE)),
            ConfInfo.restore(reqInfo.getParameter(Service.VAR_DATA)),
            Codec.unpack(reqInfo.getParameter(COL_BLOCKED)));
        break;

      case SVC_DELETE_BUNDLES:
        qs.updateData(new SqlDelete(TBL_CONF_BRANCH_BUNDLES)
            .setWhere(SqlUtils.and(SqlUtils.equals(TBL_CONF_BRANCH_BUNDLES, COL_BRANCH,
                reqInfo.getParameterLong(COL_BRANCH)),
                SqlUtils.in(TBL_CONF_BRANCH_BUNDLES, COL_BUNDLE,
                    new SqlSelect()
                        .addFields(TBL_CONF_BUNDLES, sys.getIdName(TBL_CONF_BUNDLES))
                        .addFrom(TBL_CONF_BUNDLES)
                        .setWhere(SqlUtils.inList(TBL_CONF_BUNDLES, COL_KEY, (Object[])
                            Codec.beeDeserializeCollection(reqInfo.getParameter(COL_KEY))))))));

        response = ResponseObject.emptyResponse();
        break;

      case SVC_DELETE_OPTION:
        qs.updateData(new SqlDelete(TBL_CONF_BRANCH_OPTIONS)
            .setWhere(SqlUtils.equals(TBL_CONF_BRANCH_OPTIONS,
                COL_BRANCH, reqInfo.getParameterLong(COL_BRANCH),
                COL_OPTION, reqInfo.getParameterLong(COL_OPTION))));

        response = ResponseObject.emptyResponse();
        break;

      case SVC_SET_OPTION:
        response = setOption(reqInfo.getParameterLong(COL_BRANCH),
            reqInfo.getParameterLong(COL_OPTION),
            ConfInfo.restore(reqInfo.getParameter(Service.VAR_DATA)));
        break;

      case SVC_SET_RELATION:
        response = setRelation(reqInfo.getParameterLong(COL_BRANCH), reqInfo.getParameter(COL_KEY),
            reqInfo.getParameterLong(COL_OPTION),
            ConfInfo.restore(reqInfo.getParameter(Service.VAR_DATA)),
            reqInfo.getParameter(COL_PACKET));
        break;

      case SVC_DELETE_RELATION:
        qs.updateData(new SqlDelete(TBL_CONF_RELATIONS)
            .setWhere(sys.idEquals(TBL_CONF_RELATIONS, qs.getLong(new SqlSelect()
                .addFields(TBL_CONF_RELATIONS, sys.getIdName(TBL_CONF_RELATIONS))
                .addFrom(TBL_CONF_BRANCH_BUNDLES)
                .addFromInner(TBL_CONF_BUNDLES, SqlUtils.and(sys.joinTables(TBL_CONF_BUNDLES,
                    TBL_CONF_BRANCH_BUNDLES, COL_BUNDLE),
                    SqlUtils.equals(TBL_CONF_BUNDLES, COL_KEY, reqInfo.getParameter(COL_KEY))))
                .addFromInner(TBL_CONF_BRANCH_OPTIONS,
                    SqlUtils.and(SqlUtils.joinUsing(TBL_CONF_BRANCH_BUNDLES,
                        TBL_CONF_BRANCH_OPTIONS, COL_BRANCH),
                        SqlUtils.equals(TBL_CONF_BRANCH_OPTIONS, COL_OPTION,
                            reqInfo.getParameterLong(COL_OPTION))))
                .addFromInner(TBL_CONF_RELATIONS,
                    SqlUtils.and(sys.joinTables(TBL_CONF_BRANCH_BUNDLES, TBL_CONF_RELATIONS,
                        COL_BRANCH_BUNDLE), sys.joinTables(TBL_CONF_BRANCH_OPTIONS,
                        TBL_CONF_RELATIONS, COL_BRANCH_OPTION)))
                .setWhere(SqlUtils.equals(TBL_CONF_BRANCH_BUNDLES, COL_BRANCH,
                    reqInfo.getParameterLong(COL_BRANCH)))))));

        response = ResponseObject.emptyResponse();
        break;

      case SVC_SET_RESTRICTIONS:
        Map<Long, Map<Long, Boolean>> data = new HashMap<>();

        Codec.deserializeHashMap(reqInfo.getParameter(TBL_CONF_RESTRICTIONS)).forEach((k, v) -> {
          Map<Long, Boolean> map = new HashMap<>();

          Codec.deserializeHashMap(v).forEach((k2, v2) ->
              map.put(BeeUtils.toLong(k2), BeeUtils.toBoolean(v2)));

          data.put(BeeUtils.toLong(k), map);
        });
        response = setRestrictions(reqInfo.getParameterLong(COL_BRANCH), data);
        break;

      case SVC_SAVE_OBJECT:
        response = saveObject(Specification.restore(reqInfo.getParameter(COL_OBJECT)));
        break;

      case SVC_SAVE_OBJECT_INFO:
        String key = reqInfo.getParameter(COL_KEY);

        response = qs.updateDataWithResponse(new SqlUpdate(TBL_CONF_OBJECTS)
            .addConstant(key, reqInfo.getParameter(key))
            .setWhere(sys.idEquals(TBL_CONF_OBJECTS, reqInfo.getParameterLong(COL_OBJECT))));
        break;

      case SVC_GET_OBJECT:
        response = getObject(reqInfo.getParameterLong(COL_OBJECT));
        break;

      case SVC_GET_CALENDAR:
        response = getCalendar();
        break;

      case SVC_CREATE_INVOICE:
        TradeDocument doc = TradeDocument.restore(reqInfo.getParameter(VAR_DOCUMENT));
        Map<Long, Double> items = new HashMap<>();
        Map<Long, Double> jobs = new HashMap<>();

        ImmutableMap.of(TBL_SERVICE_ORDER_ITEMS, items, TBL_SERVICE_ORDER_JOBS, jobs)
            .forEach((view, map) -> Codec.deserializeHashMap(reqInfo.getParameter(view))
                .forEach((id, qty) -> map.put(BeeUtils.toLong(id), BeeUtils.toDouble(qty))));

        response = createInvoice(doc, items, jobs);
        break;

      default:
        String msg = BeeUtils.joinWords("Cars service not recognized:", service);
        logger.warning(msg);
        response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    return Arrays.asList(
        BeeParameter.createRelation(module, PRM_SERVICE_WAREHOUSE, true, VIEW_WAREHOUSES,
            COL_WAREHOUSE_CODE),
        BeeParameter.createRelation(module, PRM_SERVICE_TRADE_OPERATION, true,
            VIEW_TRADE_OPERATIONS, COL_OPERATION_NAME)
    );
  }

  @Override
  public Module getModule() {
    return Module.CARS;
  }

  public Multimap<Long, ItemQuantities> getServiceReservations(Long warehouse,
      Collection<Long> items) {

    IsExpression xpr = SqlUtils.aggregate(SqlConstants.SqlFunction.SUM,
        SqlUtils.field(TBL_SERVICE_INVOICES, COL_TRADE_ITEM_QUANTITY));

    SqlSelect query = new SqlSelect()
        .addFields(TBL_SERVICE_ORDERS, COL_DATE)
        .addFields(TBL_SERVICE_ORDER_ITEMS, COL_ITEM, COL_TRADE_ITEM_QUANTITY)
        .addExpr(xpr, ALS_COMPLETED)
        .addFrom(TBL_SERVICE_ORDER_ITEMS)
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_SERVICE_ORDER_ITEMS, COL_ITEM))
        .addFromInner(TBL_SERVICE_ORDERS,
            sys.joinTables(TBL_SERVICE_ORDERS, TBL_SERVICE_ORDER_ITEMS, COL_SERVICE_ORDER))
        .addFromLeft(TBL_SERVICE_INVOICES,
            sys.joinTables(TBL_SERVICE_ORDER_ITEMS, TBL_SERVICE_INVOICES, COL_SERVICE_ITEM))
        .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_SERVICE_ORDER_ITEMS, COL_RESERVE),
            SqlUtils.isNull(TBL_ITEMS, COL_ITEM_IS_SERVICE),
            SqlUtils.positive(TBL_SERVICE_ORDER_ITEMS, COL_TRADE_ITEM_QUANTITY)))
        .addGroup(TBL_SERVICE_ORDERS, COL_DATE)
        .addGroup(TBL_SERVICE_ORDER_ITEMS, COL_ITEM, COL_TRADE_ITEM_QUANTITY)
        .setHaving(SqlUtils.or(SqlUtils.isNull(xpr),
            SqlUtils.more(TBL_SERVICE_ORDER_ITEMS, COL_TRADE_ITEM_QUANTITY, xpr)));

    if (DataUtils.isId(warehouse)) {
      query.setWhere(SqlUtils.and(query.getWhere(),
          SqlUtils.equals(TBL_SERVICE_ORDERS, COL_WAREHOUSE, warehouse)));
    }
    if (!BeeUtils.isEmpty(items)) {
      query.setWhere(SqlUtils.and(query.getWhere(),
          SqlUtils.inList(TBL_SERVICE_ORDER_ITEMS, COL_ITEM, items)));
    }
    Multimap<Long, ItemQuantities> reservations = HashMultimap.create();

    qs.getData(query).forEach(row -> {
      Long item = row.getLong(COL_ITEM);
      ItemQuantities existing = BeeUtils.peek(reservations.get(item));

      if (Objects.isNull(existing)) {
        existing = new ItemQuantities(null);
        reservations.put(item, existing);
      }
      existing.addReserved(row.getDateTime(COL_DATE),
          row.getDouble(COL_TRADE_ITEM_QUANTITY) - BeeUtils.unbox(row.getDouble(ALS_COMPLETED)));
    });
    return reservations;
  }

  public Map<String, Double> getServiceReservationsInfo(Long warehouse, Long item, DateTime date) {
    IsExpression xpr = SqlUtils.aggregate(SqlConstants.SqlFunction.SUM,
        SqlUtils.field(TBL_SERVICE_INVOICES, COL_TRADE_ITEM_QUANTITY));

    SqlSelect query = new SqlSelect()
        .addFields(TBL_SERVICE_ORDERS, COL_DATE, COL_ORDER_NO)
        .addFields(TBL_SERVICE_ORDER_ITEMS, COL_TRADE_ITEM_QUANTITY)
        .addExpr(xpr, ALS_COMPLETED)
        .addFrom(TBL_SERVICE_ORDER_ITEMS)
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_SERVICE_ORDER_ITEMS, COL_ITEM))
        .addFromInner(TBL_SERVICE_ORDERS,
            sys.joinTables(TBL_SERVICE_ORDERS, TBL_SERVICE_ORDER_ITEMS, COL_SERVICE_ORDER))
        .addFromLeft(TBL_SERVICE_INVOICES,
            sys.joinTables(TBL_SERVICE_ORDER_ITEMS, TBL_SERVICE_INVOICES, COL_SERVICE_ITEM))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_SERVICE_ORDER_ITEMS, COL_ITEM, item),
            SqlUtils.notNull(TBL_SERVICE_ORDER_ITEMS, COL_RESERVE),
            SqlUtils.isNull(TBL_ITEMS, COL_ITEM_IS_SERVICE),
            SqlUtils.positive(TBL_SERVICE_ORDER_ITEMS, COL_TRADE_ITEM_QUANTITY)))
        .addGroup(TBL_SERVICE_ORDERS, COL_DATE, COL_ORDER_NO)
        .addGroup(TBL_SERVICE_ORDER_ITEMS, COL_TRADE_ITEM_QUANTITY)
        .setHaving(SqlUtils.or(SqlUtils.isNull(xpr),
            SqlUtils.more(TBL_SERVICE_ORDER_ITEMS, COL_TRADE_ITEM_QUANTITY, xpr)))
        .addOrder(TBL_SERVICE_ORDERS, COL_DATE, COL_ORDER_NO);

    if (DataUtils.isId(warehouse)) {
      query.setWhere(SqlUtils.and(query.getWhere(),
          SqlUtils.equals(TBL_SERVICE_ORDERS, COL_WAREHOUSE, warehouse)));
    }
    if (Objects.nonNull(date)) {
      query.setWhere(SqlUtils.and(query.getWhere(),
          SqlUtils.less(TBL_SERVICE_ORDERS, COL_DATE, date)));
    }
    Map<String, Double> map = new LinkedHashMap<>();

    qs.getData(query).forEach(row -> {
      String key = BeeUtils.joinItems(row.getDateTime(COL_DATE).toCompactString(),
          row.getValue(COL_ORDER_NO));

      map.put(key, BeeUtils.unbox(map.get(key))
          + row.getDouble(COL_TRADE_ITEM_QUANTITY) - BeeUtils.unbox(row.getDouble(ALS_COMPLETED)));
    });
    return map;
  }

  @Override
  public void init() {
    TradeModuleBean.registerStockReservationsProvider(ModuleAndSub.of(getModule(),
        SubModule.SERVICE), new StockReservationsProvider() {

      @Override
      public Map<String, Double> getItemReservationsInfo(Long warehouse, Long item, DateTime date) {
        return Invocation.locateRemoteBean(CarsModuleBean.class)
            .getServiceReservationsInfo(warehouse, item, date);
      }

      @Override
      public Multimap<Long, ItemQuantities> getStockReservations(Long warehouse,
          Collection<Long> items) {
        return Invocation.locateRemoteBean(CarsModuleBean.class)
            .getServiceReservations(warehouse, items);
      }
    });

    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void setRowProperties(DataEvent.ViewQueryEvent event) {
        if (event.isAfter(TBL_CAR_ORDERS, TBL_CONF_TEMPLATES) && event.hasData()) {
          BeeRowSet rowSet = event.getRowset();
          int idxObj = rowSet.getColumnIndex(COL_OBJECT);
          int idxCarObj = idxObj;
          Set<Long> objects = rowSet.getDistinctLongs(idxObj);

          if (event.isTarget(TBL_CAR_ORDERS)) {
            idxCarObj = rowSet.getColumnIndex(COL_CAR + COL_OBJECT);
            objects.addAll(rowSet.getDistinctLongs(idxCarObj));
          }
          SimpleRowSet rs = qs.getData(new SqlSelect()
              .addField(TBL_CONF_OBJECTS, sys.getIdName(TBL_CONF_OBJECTS), COL_OBJECT)
              .addFields(TBL_CONF_OBJECTS, COL_BRANCH_NAME)
              .addFields(TBL_CONF_OPTIONS, COL_OPTION_NAME)
              .addFields(TBL_CONF_GROUPS, COL_GROUP_NAME)
              .addFrom(TBL_CONF_OBJECTS)
              .addFromLeft(TBL_CONF_OBJECT_OPTIONS, SqlUtils.and(sys.joinTables(TBL_CONF_OBJECTS,
                  TBL_CONF_OBJECT_OPTIONS, COL_OBJECT), SqlUtils.isNull(TBL_CONF_OBJECT_OPTIONS,
                  COL_PRICE)))
              .addFromLeft(TBL_CONF_OPTIONS,
                  sys.joinTables(TBL_CONF_OPTIONS, TBL_CONF_OBJECT_OPTIONS, COL_OPTION))
              .addFromLeft(TBL_CONF_GROUPS,
                  sys.joinTables(TBL_CONF_GROUPS, TBL_CONF_OPTIONS, COL_GROUP))
              .setWhere(sys.idInList(TBL_CONF_OBJECTS, objects))
              .addOrder(TBL_CONF_OBJECT_OPTIONS, sys.getIdName(TBL_CONF_OBJECT_OPTIONS)));

          Table<Long, String, String> table = HashBasedTable.create();

          for (SimpleRowSet.SimpleRow row : rs) {
            Long obj = row.getLong(COL_OBJECT);
            table.put(obj, usr.getDictionary().category(), row.getValue(COL_BRANCH_NAME));

            String opt = row.getValue(COL_OPTION_NAME);

            if (!BeeUtils.isEmpty(opt)) {
              table.put(obj, row.getValue(COL_GROUP_NAME), opt);
            }
          }
          for (BeeRow beeRow : rowSet) {
            Long obj = BeeUtils.nvl(beeRow.getLong(idxCarObj), beeRow.getLong(idxObj));

            if (DataUtils.isId(obj)) {
              table.row(obj).forEach((key, val) -> beeRow.setProperty(COL_OPTION + key, val));
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void calcAmounts(DataEvent.ViewQueryEvent event) {
        if (event.isAfter(TBL_SERVICE_ORDERS) && event.hasData()) {
          BeeRowSet rowSet = event.getRowset();
          Collection<Long> ids = rowSet.getRowIds();

          for (String tbl : new String[] {TBL_SERVICE_ORDER_ITEMS, TBL_SERVICE_ORDER_JOBS}) {
            SimpleRowSet data = qs.getData(new SqlSelect()
                .addFields(tbl, COL_SERVICE_ORDER)
                .addSum(TradeModuleBean.getTotal(tbl), VAR_TOTAL)
                .addSum(TradeModuleBean.getDiscount(tbl), COL_TRADE_DISCOUNT)
                .addSum(TradeModuleBean.getVat(tbl), COL_TRADE_VAT)
                .addFrom(tbl)
                .addFromInner(TBL_SERVICE_ORDERS,
                    sys.joinTables(TBL_SERVICE_ORDERS, tbl, COL_SERVICE_ORDER))
                .setWhere(SqlUtils.inList(tbl, COL_SERVICE_ORDER, ids))
                .addGroup(tbl, COL_SERVICE_ORDER));

            for (BeeRow row : rowSet) {
              Stream.of(VAR_TOTAL, COL_TRADE_DISCOUNT, COL_TRADE_VAT).forEach(col ->
                  row.setProperty(tbl + col, data.getValueByKey(COL_SERVICE_ORDER,
                      BeeUtils.toString(row.getId()), col)));
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void notifyAppointmentRemoval(DataEvent.ViewDeleteEvent event) {
        if (event.isTarget(TBL_SERVICE_EVENTS)) {
          if (event.isBefore()) {
            RowInfoList rowInfos = new RowInfoList();

            qs.getLongSet(new SqlSelect()
                .addFields(TBL_SERVICE_EVENTS, COL_APPOINTMENT)
                .addFrom(TBL_SERVICE_EVENTS)
                .setWhere(sys.idInList(TBL_SERVICE_EVENTS, event.getIds())))
                .forEach(id -> rowInfos.add(new RowInfo(id, DataUtils.NEW_ROW_VERSION)));

            event.setUserObject(rowInfos);
            return;
          }
          MultiDeleteEvent.fire(Endpoint.getModificationShooter(), VIEW_APPOINTMENTS,
              (RowInfoList) event.getUserObject());
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void setEventAttendees(DataEvent.ViewQueryEvent event) {
        if (event.isAfter(TBL_SERVICE_EVENTS) && event.hasData()) {
          BeeRowSet rowSet = event.getRowset();
          int idx = rowSet.getColumnIndex(COL_APPOINTMENT);

          SimpleRowSet rs = qs.getData(new SqlSelect()
              .addFields(TBL_APPOINTMENT_ATTENDEES, COL_APPOINTMENT, COL_ATTENDEE)
              .addFrom(TBL_APPOINTMENT_ATTENDEES)
              .setWhere(SqlUtils.inList(TBL_APPOINTMENT_ATTENDEES, COL_APPOINTMENT,
                  rowSet.getDistinctLongs(idx))));

          Multimap<Long, Long> attendees = HashMultimap.create();
          rs.forEach(row -> attendees.put(row.getLong(COL_APPOINTMENT), row.getLong(COL_ATTENDEE)));

          for (BeeRow row : rowSet) {
            row.setProperty(TBL_ATTENDEES, DataUtils.buildIdList(attendees.get(row.getLong(idx))));
          }
        }
      }
    });
  }

  public ResponseObject setBundle(Long branchId, Bundle bundle, ConfInfo info, boolean blocked) {
    int c = 0;
    Long bundleId = qs.getLong(new SqlSelect()
        .addFields(TBL_CONF_BUNDLES, sys.getIdName(TBL_CONF_BUNDLES))
        .addFrom(TBL_CONF_BUNDLES)
        .setWhere(SqlUtils.equals(TBL_CONF_BUNDLES, COL_KEY, bundle.getKey())));

    if (!DataUtils.isId(bundleId)) {
      bundleId = qs.insertData(new SqlInsert(TBL_CONF_BUNDLES)
          .addConstant(COL_KEY, bundle.getKey()));

      for (Option option : bundle.getOptions()) {
        qs.insertData(new SqlInsert(TBL_CONF_BUNDLE_OPTIONS)
            .addConstant(COL_BUNDLE, bundleId)
            .addConstant(COL_OPTION, option.getId()));
      }
    } else {
      c = qs.updateData(new SqlUpdate(TBL_CONF_BRANCH_BUNDLES)
          .addConstant(COL_PRICE, info.getPrice())
          .addConstant(COL_DESCRIPTION, info.getDescription())
          .addConstant(COL_CRITERIA, Codec.beeSerialize(info.getCriteria()))
          .addConstant(COL_PHOTO, info.getPhoto())
          .addConstant(COL_BLOCKED, blocked)
          .setWhere(SqlUtils.equals(TBL_CONF_BRANCH_BUNDLES, COL_BRANCH, branchId, COL_BUNDLE,
              bundleId)));
    }
    if (!BeeUtils.isPositive(c)) {
      qs.insertData(new SqlInsert(TBL_CONF_BRANCH_BUNDLES)
          .addConstant(COL_BRANCH, branchId)
          .addConstant(COL_BUNDLE, bundleId)
          .addNotEmpty(COL_PRICE, info.getPrice())
          .addNotEmpty(COL_DESCRIPTION, info.getDescription())
          .addConstant(COL_CRITERIA, Codec.beeSerialize(info.getCriteria()))
          .addNotNull(COL_PHOTO, info.getPhoto())
          .addConstant(COL_BLOCKED, blocked));
    }
    return ResponseObject.emptyResponse();
  }

  public ResponseObject setOption(Long branchId, Long optionId, ConfInfo info) {
    int c = qs.updateData(new SqlUpdate(TBL_CONF_BRANCH_OPTIONS)
        .addConstant(COL_PRICE, info.getPrice())
        .addConstant(COL_DESCRIPTION, info.getDescription())
        .addConstant(COL_CRITERIA, Codec.beeSerialize(info.getCriteria()))
        .addConstant(COL_PHOTO, info.getPhoto())
        .setWhere(SqlUtils.equals(TBL_CONF_BRANCH_OPTIONS, COL_BRANCH, branchId, COL_OPTION,
            optionId)));

    if (!BeeUtils.isPositive(c)) {
      qs.insertData(new SqlInsert(TBL_CONF_BRANCH_OPTIONS)
          .addConstant(COL_BRANCH, branchId)
          .addConstant(COL_OPTION, optionId)
          .addNotEmpty(COL_PRICE, info.getPrice())
          .addNotEmpty(COL_DESCRIPTION, info.getDescription())
          .addConstant(COL_CRITERIA, Codec.beeSerialize(info.getCriteria()))
          .addNotNull(COL_PHOTO, info.getPhoto()));
    }
    return ResponseObject.emptyResponse();
  }

  public ResponseObject setRelation(Long branchId, String key, Long optionId, ConfInfo info,
      String packet) {
    SimpleRowSet.SimpleRow row = qs.getRow(new SqlSelect()
        .addField(TBL_CONF_BRANCH_BUNDLES, sys.getIdName(TBL_CONF_BRANCH_BUNDLES),
            COL_BRANCH_BUNDLE)
        .addField(TBL_CONF_BRANCH_OPTIONS, sys.getIdName(TBL_CONF_BRANCH_OPTIONS),
            COL_BRANCH_OPTION)
        .addFields(TBL_CONF_RELATIONS, sys.getIdName(TBL_CONF_RELATIONS))
        .addFrom(TBL_CONF_BRANCH_BUNDLES)
        .addFromInner(TBL_CONF_BUNDLES,
            SqlUtils.and(sys.joinTables(TBL_CONF_BUNDLES, TBL_CONF_BRANCH_BUNDLES, COL_BUNDLE),
                SqlUtils.equals(TBL_CONF_BUNDLES, COL_KEY, key)))
        .addFromLeft(TBL_CONF_BRANCH_OPTIONS,
            SqlUtils.and(SqlUtils.joinUsing(TBL_CONF_BRANCH_BUNDLES, TBL_CONF_BRANCH_OPTIONS,
                COL_BRANCH), SqlUtils.equals(TBL_CONF_BRANCH_OPTIONS, COL_OPTION, optionId)))
        .addFromLeft(TBL_CONF_RELATIONS, SqlUtils.and(sys.joinTables(TBL_CONF_BRANCH_BUNDLES,
            TBL_CONF_RELATIONS, COL_BRANCH_BUNDLE), sys.joinTables(TBL_CONF_BRANCH_OPTIONS,
            TBL_CONF_RELATIONS, COL_BRANCH_OPTION)))
        .setWhere(SqlUtils.equals(TBL_CONF_BRANCH_BUNDLES, COL_BRANCH, branchId)));

    Assert.notNull(row);
    Long relationId = row.getLong(sys.getIdName(TBL_CONF_RELATIONS));

    if (DataUtils.isId(relationId)) {
      qs.updateData(new SqlUpdate(TBL_CONF_RELATIONS)
          .addConstant(COL_PRICE, info.getPrice())
          .addConstant(COL_DESCRIPTION, info.getDescription())
          .addConstant(COL_CRITERIA, Codec.beeSerialize(info.getCriteria()))
          .addConstant(COL_PHOTO, info.getPhoto())
          .addConstant(COL_PACKET, packet)
          .setWhere(sys.idEquals(TBL_CONF_RELATIONS, relationId)));
    } else {
      Long branchOptionId = row.getLong(COL_BRANCH_OPTION);

      if (!DataUtils.isId(branchOptionId)) {
        branchOptionId = qs.insertData(new SqlInsert(TBL_CONF_BRANCH_OPTIONS)
            .addConstant(COL_BRANCH, branchId)
            .addConstant(COL_OPTION, optionId));
      }
      qs.insertData(new SqlInsert(TBL_CONF_RELATIONS)
          .addConstant(COL_BRANCH_BUNDLE, row.getLong(COL_BRANCH_BUNDLE))
          .addConstant(COL_BRANCH_OPTION, branchOptionId)
          .addNotEmpty(COL_PRICE, info.getPrice())
          .addNotEmpty(COL_DESCRIPTION, info.getDescription())
          .addNotEmpty(COL_PACKET, packet)
          .addConstant(COL_CRITERIA, Codec.beeSerialize(info.getCriteria()))
          .addNotNull(COL_PHOTO, info.getPhoto()));
    }
    return ResponseObject.emptyResponse();
  }

  public ResponseObject setRestrictions(Long branchId, Map<Long, Map<Long, Boolean>> data) {
    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(TBL_CONF_BRANCH_OPTIONS, COL_OPTION)
        .addField(TBL_CONF_BRANCH_OPTIONS, sys.getIdName(TBL_CONF_BRANCH_OPTIONS),
            COL_BRANCH_OPTION)
        .addField(TBL_CONF_RESTRICTIONS, COL_OPTION, TBL_CONF_RELATIONS + COL_OPTION)
        .addFields(TBL_CONF_RESTRICTIONS, COL_DENIED)
        .addFrom(TBL_CONF_BRANCH_OPTIONS)
        .addFromLeft(TBL_CONF_RESTRICTIONS,
            sys.joinTables(TBL_CONF_BRANCH_OPTIONS, TBL_CONF_RESTRICTIONS, COL_BRANCH_OPTION))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_CONF_BRANCH_OPTIONS, COL_BRANCH, branchId),
            SqlUtils.inList(TBL_CONF_BRANCH_OPTIONS, COL_OPTION, data.keySet()))));

    Map<Long, Pair<Long, Map<Long, Boolean>>> map = new HashMap<>();

    for (SimpleRowSet.SimpleRow row : rs) {
      Long option = row.getLong(COL_OPTION);

      if (!map.containsKey(option)) {
        map.put(option, Pair.of(row.getLong(COL_BRANCH_OPTION), new HashMap<>()));
      }
      Long relatedOption = row.getLong(TBL_CONF_RELATIONS + COL_OPTION);

      if (DataUtils.isId(relatedOption)) {
        map.get(option).getB().put(relatedOption, BeeUtils.unbox(row.getBoolean(COL_DENIED)));
      }
    }
    for (Long option : map.keySet()) {
      Map<Long, Boolean> restrictions = data.remove(option);
      Long branchOption = map.get(option).getA();

      for (Map.Entry<Long, Boolean> entry : map.get(option).getB().entrySet()) {
        Long opt = entry.getKey();
        Boolean denied = restrictions.remove(opt);

        if (!Objects.equals(denied, entry.getValue())) {
          IsQuery query;
          IsCondition clause = SqlUtils.equals(TBL_CONF_RESTRICTIONS, COL_BRANCH_OPTION,
              branchOption, COL_OPTION, opt);

          if (denied == null) {
            query = new SqlDelete(TBL_CONF_RESTRICTIONS)
                .setWhere(clause);
          } else {
            query = new SqlUpdate(TBL_CONF_RESTRICTIONS)
                .addConstant(COL_DENIED, denied)
                .setWhere(clause);
          }
          qs.updateData(query);
        }
      }
      for (Long opt : restrictions.keySet()) {
        qs.insertData(new SqlInsert(TBL_CONF_RESTRICTIONS)
            .addConstant(COL_BRANCH_OPTION, branchOption)
            .addConstant(COL_OPTION, opt)
            .addConstant(COL_DENIED, restrictions.get(opt)));
      }
    }
    for (Long option : data.keySet()) {
      Map<Long, Boolean> restrictions = data.get(option);

      if (!BeeUtils.isEmpty(restrictions)) {
        Long branchOption = qs.insertData(new SqlInsert(TBL_CONF_BRANCH_OPTIONS)
            .addConstant(COL_BRANCH, branchId)
            .addConstant(COL_OPTION, option));

        for (Long opt : restrictions.keySet()) {
          qs.insertData(new SqlInsert(TBL_CONF_RESTRICTIONS)
              .addConstant(COL_BRANCH_OPTION, branchOption)
              .addConstant(COL_OPTION, opt)
              .addConstant(COL_DENIED, restrictions.get(opt)));
        }
      }
    }
    return ResponseObject.emptyResponse();
  }

  private ResponseObject createInvoice(TradeDocument document, Map<Long, Double> items,
      Map<Long, Double> jobs) {
    ResponseObject response = trd.createDocument(document);

    if (response.hasErrors()) {
      return response;
    }
    Long tradeId = response.getResponseAsLong();

    ImmutableMap.of(COL_SERVICE_ITEM, items, COL_SERVICE_JOB, jobs).forEach((col, map) ->
        map.forEach((id, qty) -> qs.insertData(new SqlInsert(TBL_SERVICE_INVOICES)
            .addConstant(COL_TRADE_DOCUMENT, tradeId)
            .addConstant(col, id)
            .addConstant(COL_TRADE_ITEM_QUANTITY, qty))));

    return ResponseObject.response(tradeId);
  }

  private ResponseObject getCalendar() {
    Long calendarId = qs.getId(VIEW_CALENDARS, COL_CALENDAR_IS_SERVICE, true);

    if (!DataUtils.isId(calendarId)) {
      SqlInsert si = new SqlInsert(VIEW_CALENDARS)
          .addConstant(COL_CALENDAR_NAME, usr.getDictionary().carService())
          .addConstant(COL_CALENDAR_IS_SERVICE, true);

      Map<String, Pair<Defaults.DefaultExpression, Object>> defs =
          sys.getTableDefaults(VIEW_CALENDARS);

      if (!BeeUtils.isEmpty(defs)) {
        defs.forEach((field, pair) -> {
          if (Objects.isNull(pair.getA())) {
            si.addConstant(field, pair.getB());
          }
        });
      }
      calendarId = qs.insertData(si);
    }
    return ResponseObject.response(calendarId);
  }

  private ResponseObject getConfiguration(Long branchId) {
    Configuration configuration = new Configuration();

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_CONF_DIMENSIONS, COL_GROUP, COL_ORDINAL)
        .addFields(TBL_CONF_GROUPS, COL_GROUP_NAME, COL_REQUIRED)
        .addFrom(TBL_CONF_DIMENSIONS)
        .addFromInner(TBL_CONF_GROUPS,
            sys.joinTables(TBL_CONF_GROUPS, TBL_CONF_DIMENSIONS, COL_GROUP))
        .setWhere(SqlUtils.equals(TBL_CONF_DIMENSIONS, COL_BRANCH, branchId)));

    for (SimpleRowSet.SimpleRow row : data) {
      configuration.addDimension(new Dimension(row.getLong(COL_GROUP),
              row.getValue(COL_GROUP_NAME)).setRequired(row.getBoolean(COL_REQUIRED)),
          row.getInt(COL_ORDINAL));
    }
    data = qs.getData(new SqlSelect()
        .addFields(TBL_CONF_BRANCH_BUNDLES, COL_PRICE, COL_BLOCKED)
        .addField(TBL_CONF_BRANCH_BUNDLES, COL_DESCRIPTION, COL_BUNDLE + COL_DESCRIPTION)
        .addField(TBL_CONF_BRANCH_BUNDLES, COL_CRITERIA, COL_BUNDLE + COL_CRITERIA)
        .addField(TBL_CONF_BRANCH_BUNDLES, COL_PHOTO, COL_BUNDLE + COL_PHOTO)
        .addFields(TBL_CONF_BUNDLE_OPTIONS, COL_BUNDLE, COL_OPTION)
        .addFields(TBL_CONF_OPTIONS, COL_GROUP, COL_OPTION_NAME, COL_CODE, COL_CODE2,
            COL_DESCRIPTION, COL_PHOTO)
        .addFields(TBL_CONF_GROUPS, COL_GROUP_NAME, COL_REQUIRED)
        .addFrom(TBL_CONF_BRANCH_BUNDLES)
        .addFromInner(TBL_CONF_BUNDLE_OPTIONS,
            SqlUtils.joinUsing(TBL_CONF_BRANCH_BUNDLES, TBL_CONF_BUNDLE_OPTIONS, COL_BUNDLE))
        .addFromInner(TBL_CONF_OPTIONS,
            sys.joinTables(TBL_CONF_OPTIONS, TBL_CONF_BUNDLE_OPTIONS, COL_OPTION))
        .addFromInner(TBL_CONF_GROUPS, sys.joinTables(TBL_CONF_GROUPS, TBL_CONF_OPTIONS, COL_GROUP))
        .setWhere(SqlUtils.equals(TBL_CONF_BRANCH_BUNDLES, COL_BRANCH, branchId)));

    Multimap<Long, Option> bundleOptions = HashMultimap.create();
    Map<Long, Pair<Bundle, Pair<ConfInfo, Boolean>>> bundles = new HashMap<>();

    for (SimpleRowSet.SimpleRow row : data) {
      Long id = row.getLong(COL_BUNDLE);
      bundleOptions.put(id, new Option(row));

      if (!bundles.containsKey(id)) {
        bundles.put(id, Pair.of(null, Pair.of(ConfInfo.of(row.getValue(COL_PRICE),
            row.getValue(COL_BUNDLE + COL_DESCRIPTION))
            .setCriteria(Codec.deserializeLinkedHashMap(row.getValue(COL_BUNDLE + COL_CRITERIA)))
            .setPhoto(row.getLong(COL_BUNDLE + COL_PHOTO)), row.getBoolean(COL_BLOCKED))));
      }
    }
    for (Long bundleId : bundles.keySet()) {
      Bundle bundle = new Bundle(bundleOptions.get(bundleId));
      Pair<Bundle, Pair<ConfInfo, Boolean>> pair = bundles.get(bundleId);
      pair.setA(bundle);
      configuration.setBundleInfo(bundle, pair.getB().getA(), pair.getB().getB());
    }
    data = qs.getData(new SqlSelect()
        .addField(TBL_CONF_BRANCH_OPTIONS, sys.getIdName(TBL_CONF_BRANCH_OPTIONS),
            COL_BRANCH_OPTION)
        .addFields(TBL_CONF_BRANCH_OPTIONS, COL_OPTION)
        .addField(TBL_CONF_BRANCH_OPTIONS, COL_PRICE, COL_OPTION + COL_PRICE)
        .addField(TBL_CONF_BRANCH_OPTIONS, COL_DESCRIPTION, COL_OPTION + COL_DESCRIPTION)
        .addField(TBL_CONF_BRANCH_OPTIONS, COL_CRITERIA, COL_OPTION + COL_CRITERIA)
        .addField(TBL_CONF_BRANCH_OPTIONS, COL_PHOTO, COL_OPTION + COL_PHOTO)
        .addFields(TBL_CONF_OPTIONS, COL_GROUP, COL_OPTION_NAME, COL_CODE, COL_CODE2,
            COL_DESCRIPTION, COL_PHOTO)
        .addFields(TBL_CONF_GROUPS, COL_GROUP_NAME, COL_REQUIRED)
        .addFields(TBL_CONF_RELATIONS, COL_PRICE, COL_PACKET)
        .addField(TBL_CONF_RELATIONS, COL_DESCRIPTION, TBL_CONF_RELATIONS + COL_DESCRIPTION)
        .addField(TBL_CONF_RELATIONS, COL_CRITERIA, TBL_CONF_RELATIONS + COL_CRITERIA)
        .addField(TBL_CONF_RELATIONS, COL_PHOTO, TBL_CONF_RELATIONS + COL_PHOTO)
        .addFields(TBL_CONF_BRANCH_BUNDLES, COL_BUNDLE)
        .addFrom(TBL_CONF_BRANCH_OPTIONS)
        .addFromInner(TBL_CONF_OPTIONS,
            sys.joinTables(TBL_CONF_OPTIONS, TBL_CONF_BRANCH_OPTIONS, COL_OPTION))
        .addFromInner(TBL_CONF_GROUPS, sys.joinTables(TBL_CONF_GROUPS, TBL_CONF_OPTIONS, COL_GROUP))
        .addFromLeft(TBL_CONF_RELATIONS,
            sys.joinTables(TBL_CONF_BRANCH_OPTIONS, TBL_CONF_RELATIONS, COL_BRANCH_OPTION))
        .addFromLeft(TBL_CONF_BRANCH_BUNDLES,
            sys.joinTables(TBL_CONF_BRANCH_BUNDLES, TBL_CONF_RELATIONS, COL_BRANCH_BUNDLE))
        .setWhere(SqlUtils.equals(TBL_CONF_BRANCH_OPTIONS, COL_BRANCH, branchId)));

    Map<Long, Option> branchOptions = new HashMap<>();

    for (SimpleRowSet.SimpleRow row : data) {
      Long branchOption = row.getLong(COL_BRANCH_OPTION);

      if (!branchOptions.containsKey(branchOption)) {
        Option option = new Option(row);
        branchOptions.put(branchOption, option);
        configuration.setOptionInfo(option, ConfInfo.of(row.getValue(COL_OPTION + COL_PRICE),
            row.getValue(COL_OPTION + COL_DESCRIPTION))
            .setCriteria(Codec.deserializeLinkedHashMap(row.getValue(COL_OPTION + COL_CRITERIA)))
            .setPhoto(row.getLong(COL_OPTION + COL_PHOTO)));
      }
      if (DataUtils.isId(row.getLong(COL_BUNDLE))) {
        configuration.setRelationInfo(branchOptions.get(branchOption),
            bundles.get(row.getLong(COL_BUNDLE)).getA(), ConfInfo.of(row.getValue(COL_PRICE),
                row.getValue(TBL_CONF_RELATIONS + COL_DESCRIPTION))
                .setCriteria(Codec.deserializeLinkedHashMap(row.getValue(TBL_CONF_RELATIONS
                    + COL_CRITERIA))).setPhoto(row.getLong(TBL_CONF_RELATIONS + COL_PHOTO)),
            row.getValue(COL_PACKET));
      }
    }
    if (!branchOptions.isEmpty()) {
      Map<Long, Option> optMap = new HashMap<>();
      branchOptions.values().forEach(opt -> optMap.put(opt.getId(), opt));

      data = qs.getData(new SqlSelect()
          .addFields(TBL_CONF_PACKET_OPTIONS, COL_PACKET, COL_OPTION)
          .addFields(TBL_CONF_OPTIONS, COL_GROUP, COL_OPTION_NAME, COL_CODE, COL_CODE2)
          .addFields(TBL_CONF_GROUPS, COL_GROUP_NAME)
          .addFrom(TBL_CONF_PACKET_OPTIONS)
          .addFromInner(TBL_CONF_OPTIONS,
              sys.joinTables(TBL_CONF_OPTIONS, TBL_CONF_PACKET_OPTIONS, COL_OPTION))
          .addFromInner(TBL_CONF_GROUPS,
              sys.joinTables(TBL_CONF_GROUPS, TBL_CONF_OPTIONS, COL_GROUP))
          .setWhere(SqlUtils.inList(TBL_CONF_PACKET_OPTIONS, COL_PACKET, optMap.keySet())));

      data.forEach(simpleRow -> configuration.getPackets(optMap.get(simpleRow.getLong(COL_PACKET)))
          .add(new Option(simpleRow)));
    }
    data = qs.getData(new SqlSelect()
        .addFields(TBL_CONF_RESTRICTIONS, COL_BRANCH_OPTION, COL_OPTION, COL_DENIED)
        .addFields(TBL_CONF_OPTIONS, COL_GROUP, COL_OPTION_NAME, COL_CODE, COL_CODE2,
            COL_DESCRIPTION, COL_PHOTO)
        .addFields(TBL_CONF_GROUPS, COL_GROUP_NAME, COL_REQUIRED)
        .addFrom(TBL_CONF_RESTRICTIONS)
        .addFromInner(TBL_CONF_OPTIONS,
            sys.joinTables(TBL_CONF_OPTIONS, TBL_CONF_RESTRICTIONS, COL_OPTION))
        .addFromInner(TBL_CONF_GROUPS, sys.joinTables(TBL_CONF_GROUPS, TBL_CONF_OPTIONS, COL_GROUP))
        .addFromInner(TBL_CONF_BRANCH_OPTIONS,
            sys.joinTables(TBL_CONF_BRANCH_OPTIONS, TBL_CONF_RESTRICTIONS, COL_BRANCH_OPTION))
        .setWhere(SqlUtils.equals(TBL_CONF_BRANCH_OPTIONS, COL_BRANCH, branchId)));

    for (SimpleRowSet.SimpleRow row : data) {
      Option option = new Option(row);
      configuration.setRestriction(branchOptions.get(row.getLong(COL_BRANCH_OPTION)), option,
          BeeUtils.unbox(row.getBoolean(COL_DENIED)));
    }
    return ResponseObject.response(configuration);
  }

  private ResponseObject getObject(Long objectId) {
    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(TBL_CONF_OBJECTS, COL_BRANCH, COL_BRANCH_NAME, COL_CRITERIA)
        .addField(TBL_CONF_OBJECTS, COL_DESCRIPTION, COL_BUNDLE + COL_DESCRIPTION)
        .addField(TBL_CONF_OBJECTS, COL_PRICE, COL_BUNDLE + COL_PRICE)
        .addFields(TBL_CONF_OBJECT_OPTIONS, COL_OPTION, COL_PRICE)
        .addFields(TBL_CONF_OPTIONS, COL_GROUP, COL_OPTION_NAME, COL_CODE, COL_CODE2,
            COL_DESCRIPTION, COL_PHOTO_CODE, COL_PHOTO)
        .addFields(TBL_CONF_GROUPS, COL_GROUP_NAME, COL_REQUIRED)
        .addFrom(TBL_CONF_OBJECTS)
        .addFromInner(TBL_CONF_OBJECT_OPTIONS,
            sys.joinTables(TBL_CONF_OBJECTS, TBL_CONF_OBJECT_OPTIONS, COL_OBJECT))
        .addFromInner(TBL_CONF_OPTIONS,
            sys.joinTables(TBL_CONF_OPTIONS, TBL_CONF_OBJECT_OPTIONS, COL_OPTION))
        .addFromInner(TBL_CONF_GROUPS, sys.joinTables(TBL_CONF_GROUPS, TBL_CONF_OPTIONS, COL_GROUP))
        .setWhere(sys.idEquals(TBL_CONF_OBJECTS, objectId))
        .addOrder(TBL_CONF_OBJECT_OPTIONS, sys.getIdName(TBL_CONF_OBJECT_OPTIONS)));

    Specification specification = null;
    Integer bundlePrice = null;
    List<Option> bundleOptions = new ArrayList<>();

    for (SimpleRowSet.SimpleRow row : rs) {
      if (Objects.isNull(specification)) {
        specification = new Specification();
        specification.setId(objectId);
        specification.setBranch(row.getLong(COL_BRANCH), row.getValue(COL_BRANCH_NAME));
        specification.setDescription(row.getValue(COL_BUNDLE + COL_DESCRIPTION));
        specification.setCriteria(Codec.deserializeLinkedHashMap(row.getValue(COL_CRITERIA)));
        bundlePrice = row.getInt(COL_BUNDLE + COL_PRICE);
      }
      Integer price = row.getInt(COL_PRICE);
      Option option = new Option(row);

      if (Objects.isNull(price)) {
        if (BeeUtils.allNotNull(row.getValue(COL_PHOTO_CODE), option.getPhoto())) {
          specification.getPhotos().put(row.getValue(COL_PHOTO_CODE), option.getPhoto());
        }
        bundleOptions.add(option);
      } else {
        specification.addOption(option, price);
      }
    }
    if (Objects.nonNull(specification)) {
      if (!BeeUtils.isEmpty(bundleOptions)) {
        specification.setBundle(new Bundle(bundleOptions), bundlePrice);
      }
      if (DataUtils.isId(specification.getBranchId())) {
        Long branchBundle = null;
        Long bundlePhoto = null;

        if (Objects.nonNull(specification.getBundle())) {
          SimpleRowSet.SimpleRow row = qs.getRow(new SqlSelect()
              .addField(TBL_CONF_BRANCH_BUNDLES, sys.getIdName(TBL_CONF_BRANCH_BUNDLES),
                  COL_BRANCH_BUNDLE)
              .addFields(TBL_CONF_BRANCH_BUNDLES, COL_PHOTO)
              .addFrom(TBL_CONF_BRANCH_BUNDLES)
              .addFromInner(TBL_CONF_BUNDLES,
                  SqlUtils.and(sys.joinTables(TBL_CONF_BUNDLES, TBL_CONF_BRANCH_BUNDLES,
                      COL_BUNDLE), SqlUtils.equals(TBL_CONF_BUNDLES, COL_KEY,
                      specification.getBundle().getKey())))
              .setWhere(SqlUtils.equals(TBL_CONF_BRANCH_BUNDLES, COL_BRANCH,
                  specification.getBranchId())));

          if (Objects.nonNull(row)) {
            branchBundle = row.getLong(COL_BRANCH_BUNDLE);
            bundlePhoto = row.getLong(COL_PHOTO);
          }
        }
        String idName = sys.getIdName(TBL_CONF_PRICELIST);

        rs = qs.getData(new SqlSelect()
            .addFields(TBL_CONF_PRICELIST, idName, COL_BRANCH, COL_PHOTO_CODE, COL_PHOTO)
            .addFrom(TBL_CONF_PRICELIST));

        SimpleRowSet.SimpleRow row = rs.getRowByKey(idName,
            BeeUtils.toString(specification.getBranchId()));

        if (BeeUtils.allNotNull(row.getValue(COL_PHOTO_CODE), bundlePhoto)) {
          specification.getPhotos().put(row.getValue(COL_PHOTO_CODE), bundlePhoto);
        }
        if (DataUtils.isId(branchBundle)) {
          Set<Long> opts = specification.getOptions().stream()
              .filter(o -> o.getDimension().isRequired())
              .map(Option::getId).collect(Collectors.toSet());

          if (!opts.isEmpty()) {
            SimpleRowSet photoRs = qs.getData(new SqlSelect()
                .addFields(TBL_CONF_OPTIONS, COL_PHOTO_CODE)
                .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_CONF_RELATIONS, COL_PHOTO),
                    SqlUtils.field(TBL_CONF_BRANCH_OPTIONS, COL_PHOTO),
                    SqlUtils.field(TBL_CONF_OPTIONS, COL_PHOTO)), COL_PHOTO)
                .addFrom(TBL_CONF_BRANCH_OPTIONS)
                .addFromInner(TBL_CONF_OPTIONS, SqlUtils.and(sys.joinTables(TBL_CONF_OPTIONS,
                    TBL_CONF_BRANCH_OPTIONS, COL_OPTION), SqlUtils.notNull(TBL_CONF_OPTIONS,
                    COL_PHOTO_CODE)))
                .addFromLeft(TBL_CONF_RELATIONS,
                    SqlUtils.and(sys.joinTables(TBL_CONF_BRANCH_OPTIONS, TBL_CONF_RELATIONS,
                        COL_BRANCH_OPTION), SqlUtils.equals(TBL_CONF_RELATIONS, COL_BRANCH_BUNDLE,
                        branchBundle)))
                .setWhere(SqlUtils.and(SqlUtils.equals(TBL_CONF_BRANCH_OPTIONS, COL_BRANCH,
                    specification.getBranchId()), SqlUtils.inList(TBL_CONF_BRANCH_OPTIONS,
                    COL_OPTION, opts))));

            for (SimpleRowSet.SimpleRow photoRow : photoRs) {
              if (DataUtils.isId(photoRow.getLong(COL_PHOTO))) {
                specification.getPhotos().put(photoRow.getValue(COL_PHOTO_CODE),
                    photoRow.getLong(COL_PHOTO));
              }
            }
          }
        }
        while (Objects.nonNull(row)) {
          if (BeeUtils.allNotNull(row.getValue(COL_PHOTO_CODE), row.getLong(COL_PHOTO))) {
            specification.getPhotos().putIfAbsent(row.getValue(COL_PHOTO_CODE),
                row.getLong(COL_PHOTO));
          }
          String id = row.getValue(COL_BRANCH);
          row = DataUtils.isId(id) ? rs.getRowByKey(idName, id) : null;
        }
      }
    }
    return ResponseObject.response(specification);
  }

  private ResponseObject saveDimensions(Long branchId, List<Long> rows, List<Long> cols) {
    String idName = sys.getIdName(TBL_CONF_DIMENSIONS);

    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_CONF_DIMENSIONS, idName, COL_GROUP, COL_ORDINAL)
        .addFrom(TBL_CONF_DIMENSIONS)
        .setWhere(SqlUtils.equals(TBL_CONF_DIMENSIONS, COL_BRANCH, branchId)));

    Set<Long> usedIds = new HashSet<>();
    List<Pair<Long, Integer>> list = new ArrayList<>();

    for (int i = 0; i < rows.size(); i++) {
      list.add(Pair.of(rows.get(i), i));
    }
    for (int i = 0; i < cols.size(); i++) {
      list.add(Pair.of(cols.get(i), (i + 1) * (-1)));
    }
    for (Pair<Long, Integer> pair : list) {
      boolean found = false;

      for (SimpleRowSet.SimpleRow row : data) {
        Long id = row.getLong(idName);
        found = !usedIds.contains(id) && Objects.equals(pair.getA(), row.getLong(COL_GROUP));

        if (found) {
          if (!Objects.equals(row.getInt(COL_ORDINAL), pair.getB())) {
            qs.updateData(new SqlUpdate(TBL_CONF_DIMENSIONS)
                .addConstant(COL_ORDINAL, pair.getB())
                .setWhere(sys.idEquals(TBL_CONF_DIMENSIONS, id)));
          }
          usedIds.add(id);
          break;
        }
      }
      if (!found) {
        qs.insertData(new SqlInsert(TBL_CONF_DIMENSIONS)
            .addConstant(COL_BRANCH, branchId)
            .addConstant(COL_GROUP, pair.getA())
            .addConstant(COL_ORDINAL, pair.getB()));
      }
    }
    List<Long> unusedIds = Arrays.stream(data.getLongColumn(idName))
        .filter(id -> !usedIds.contains(id))
        .collect(Collectors.toList());

    if (!BeeUtils.isEmpty(unusedIds)) {
      qs.updateData(new SqlDelete(TBL_CONF_DIMENSIONS)
          .setWhere(sys.idInList(TBL_CONF_DIMENSIONS, unusedIds)));
    }
    return ResponseObject.emptyResponse();
  }

  private ResponseObject saveObject(Specification specification) {
    long objectId = qs.insertData(new SqlInsert(TBL_CONF_OBJECTS)
        .addConstant(COL_BRANCH, specification.getBranchId())
        .addConstant(COL_BRANCH_NAME, specification.getBranchName())
        .addConstant(COL_DESCRIPTION, specification.getDescription())
        .addConstant(COL_CRITERIA, Codec.beeSerialize(specification.getCriteria()))
        .addConstant(COL_PRICE, specification.getBundlePrice()));

    if (specification.getBundle() != null) {
      for (Option option : specification.getBundle().getOptions()) {
        qs.insertData(new SqlInsert(TBL_CONF_OBJECT_OPTIONS)
            .addConstant(COL_OBJECT, objectId)
            .addConstant(COL_OPTION, option.getId()));
      }
    }
    for (Option option : specification.getOptions()) {
      qs.insertData(new SqlInsert(TBL_CONF_OBJECT_OPTIONS)
          .addConstant(COL_OBJECT, objectId)
          .addConstant(COL_OPTION, option.getId())
          .addConstant(COL_PRICE, specification.getOptionPrice(option)));
    }
    return ResponseObject.response(objectId);
  }
}
