package com.butent.bee.server.modules.transport;

import com.google.common.base.Splitter;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.TreeMultimap;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.data.BeeTable;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.ExchangeUtils;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.modules.trade.TradeModuleBean;
import com.butent.bee.server.news.ExtendedUsageQueryProvider;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.NewsHelper;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.Triplet;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.modules.transport.TransportUtils;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.Headline;
import com.butent.bee.shared.news.HeadlineProducer;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.websocket.messages.ModificationMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;

@Stateless
@LocalBean
public class TransportModuleBean implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(TransportModuleBean.class);

  @EJB
  DataEditorBean deb;
  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;
  @EJB
  ParamHolderBean prm;
  @EJB
  TradeModuleBean trd;
  @EJB
  NewsBean news;
  @EJB
  TransportReportsBean rep;
  @EJB
  MailModuleBean mail;
  @EJB
  ConcurrencyBean cb;

  private static IsExpression getAssessmentTurnoverExpression(SqlSelect query, String source,
      String defDateSource, String defDateAlias, Long currency, boolean woVat) {

    IsExpression amountExpr = SqlUtils.field(source, COL_AMOUNT);

    if (woVat) {
      amountExpr = TradeModuleBean.getWithoutVatExpression(source, amountExpr);
    } else {
      amountExpr = TradeModuleBean.getTotalExpression(source, amountExpr);
    }
    if (DataUtils.isId(currency)) {
      return ExchangeUtils.exchangeFieldTo(query, amountExpr, SqlUtils.field(source, COL_CURRENCY),
          SqlUtils.nvl(SqlUtils.field(source, COL_DATE),
              SqlUtils.field(defDateSource, defDateAlias)),
          SqlUtils.constant(currency));

    } else {
      return ExchangeUtils.exchangeField(query, amountExpr, SqlUtils.field(source, COL_CURRENCY),
          SqlUtils.nvl(SqlUtils.field(source, COL_DATE),
              SqlUtils.field(defDateSource, defDateAlias)));
    }
  }

  private static IsCondition getChartTripCondition(Range<?> period, Boolean completed) {
    HasConditions conditions = SqlUtils.and();

    if (period != null) {
      conditions.add(SqlUtils.anyIntersects(TBL_TRIPS, TRIP_DATE_COLUMNS, period));
    }

    if (!BeeUtils.isTrue(completed)) {
      conditions.add(SqlUtils.notEqual(TBL_TRIPS, COL_TRIP_STATUS, TripStatus.COMPLETED));
    }

    return conditions.isEmpty() ? null : conditions;
  }

  private static Filter getChartTripFilter(Range<Value> period, Boolean completed) {
    CompoundFilter filter = Filter.and();

    if (period != null) {
      filter.add(Filter.anyIntersects(TRIP_DATE_COLUMNS, period));
    }

    if (!BeeUtils.isTrue(completed)) {
      filter.add(Filter.notEquals(COL_TRIP_STATUS, TripStatus.COMPLETED));
    }

    return filter.isEmpty() ? null : filter;
  }

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> result = new ArrayList<>();

    List<SearchResult> vehiclesResult = qs.getSearchResults(VIEW_VEHICLES,
        Filter.anyContains(Sets.newHashSet(COL_NUMBER, COL_VEHICLE_BRAND_NAME, COL_MODEL_NAME,
            COL_OWNER_NAME), query));

    List<SearchResult> orderCargoResult = qs.getSearchResults(VIEW_ORDER_CARGO,
        Filter.anyContains(Sets.newHashSet(COL_CARGO_DESCRIPTION,
            COL_NUMBER, ALS_CARGO_CMR_NUMBER, ALS_CARGO_NOTES, COL_CARGO_DIRECTIONS), query));

    result.addAll(vehiclesResult);
    result.addAll(orderCargoResult);

    if (usr.isModuleVisible(ModuleAndSub.of(Module.TRANSPORT, SubModule.LOGISTICS))) {
      result.addAll(qs.getSearchResults(VIEW_ASSESSMENTS,
          Filter.compareId(BeeUtils.toLong(query))));
    }

    return result;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response;

    if (BeeUtils.same(svc, SVC_GET_BEFORE)) {
      response = getTripBeforeData(BeeUtils.toLong(reqInfo.getParameter(COL_VEHICLE)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_DATE)));

    } else if (BeeUtils.same(svc, SVC_GET_UNASSIGNED_CARGOS)) {
      response = getUnassignedCargos(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_ROUTE)) {
      String tmp = rep.getTripRoutes(new SqlSelect()
          .addExpr(SqlUtils.constant(BeeUtils.toLong(reqInfo.getParameter(COL_TRIP))), COL_TRIP));

      response = ResponseObject.response(qs.getValue(new SqlSelect()
          .addFields(tmp, COL_TRIP_ROUTE)
          .addFrom(tmp)));

      qs.sqlDropTemp(tmp);

    } else if (BeeUtils.same(svc, SVC_GENERATE_DAILY_COSTS)) {
      response = generateDailyCosts(BeeUtils.toLong(reqInfo.getParameter(COL_TRIP)));

    } else if (BeeUtils.same(svc, SVC_GENERATE_ROUTE)) {
      response = generateTripRoute(BeeUtils.toLong(reqInfo.getParameter(COL_TRIP)));

    } else if (BeeUtils.same(svc, SVC_GET_FX_DATA)) {
      response = getFxData();

    } else if (BeeUtils.same(svc, SVC_GET_SS_DATA)) {
      response = getVehicleTbData(svc, null, true, VehicleType.TRUCK,
          COL_SS_THEME, COL_SS_MIN_DATE, COL_SS_MAX_DATE,
          COL_SS_TRANSPORT_GROUPS, COL_SS_COMPLETED_TRIPS);

    } else if (BeeUtils.same(svc, SVC_GET_DTB_DATA)) {
      response = getDtbData();

    } else if (BeeUtils.same(svc, SVC_GET_TRUCK_TB_DATA)) {
      response = getVehicleTbData(svc, Filter.notNull(COL_IS_TRUCK), false, VehicleType.TRUCK,
          COL_TRUCK_THEME, COL_TRUCK_MIN_DATE, COL_TRUCK_MAX_DATE,
          COL_TRUCK_TRANSPORT_GROUPS, COL_TRUCK_COMPLETED_TRIPS);

    } else if (BeeUtils.same(svc, SVC_GET_TRAILER_TB_DATA)) {
      response = getVehicleTbData(svc, Filter.notNull(COL_IS_TRAILER), false, VehicleType.TRAILER,
          COL_TRAILER_THEME, COL_TRAILER_MIN_DATE, COL_TRAILER_MAX_DATE,
          COL_TRAILER_TRANSPORT_GROUPS, COL_TRAILER_COMPLETED_TRIPS);

    } else if (BeeUtils.same(svc, SVC_GET_COLORS)) {
      response = getColors(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_CARGO_USAGE)) {
      response = getCargoUsage(reqInfo.getParameter(Service.VAR_VIEW_NAME),
          DataUtils.parseIdList(reqInfo.getParameter(Service.VAR_VIEW_ROW_ID)),
          reqInfo.getParameter(Service.VAR_COLUMN));

    } else if (BeeUtils.same(svc, SVC_GET_CARGO_PLACES)) {
      String als = "tmpSubQuery";

      response = ResponseObject.response(qs.getData(new SqlSelect()
          .addFields(als, COL_CARGO, VAR_UNLOADING)
          .addAllFields(TBL_CARGO_PLACES)
          .addField(TBL_CITIES, COL_CITY_NAME, ALS_CITY_NAME)
          .addField(TBL_COUNTRIES, COL_COUNTRY_NAME, ALS_COUNTRY_NAME)
          .addField(TBL_COUNTRIES, COL_COUNTRY_CODE, COL_COUNTRY + COL_COUNTRY_CODE)
          .addFrom(TBL_CARGO_PLACES)
          .addFromLeft(TBL_CITIES, sys.joinTables(TBL_CITIES, TBL_CARGO_PLACES, COL_PLACE_CITY))
          .addFromLeft(TBL_COUNTRIES,
              sys.joinTables(TBL_COUNTRIES, TBL_CARGO_PLACES, COL_PLACE_COUNTRY))
          .addFromInner(getHandlingQuery(sys.idInList(TBL_CARGO_TRIPS,
              DataUtils.parseIdList(reqInfo.getParameter(COL_CARGO_TRIP))), true), als,
              SqlUtils.joinUsing(TBL_CARGO_PLACES, als, sys.getIdName(TBL_CARGO_PLACES)))
          .addOrder(TBL_CARGO_PLACES, COL_PLACE_ORDINAL, COL_PLACE_DATE)
          .addOrder(als, VAR_UNLOADING)));

    } else if (BeeUtils.same(svc, SVC_GET_ASSESSMENT_QUANTITY_REPORT)) {
      response = getAssessmentQuantityReport(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_ASSESSMENT_TURNOVER_REPORT)) {
      response = getAssessmentTurnoverReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_CREATE_INVOICE_ITEMS)) {
      Long saleId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_SALE));
      Long purchaseId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_PURCHASE));
      Long currency = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_CURRENCY));
      Set<Long> ids = DataUtils.parseIdSet(reqInfo.getParameter(Service.VAR_DATA));
      Long item = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ITEM));
      Double creditAmount = BeeUtils.toDoubleOrNull(reqInfo.getParameter(COL_TRADE_AMOUNT));

      if (DataUtils.isId(saleId)) {
        response = createInvoiceItems(saleId, currency, ids, item);
      } else if (BeeUtils.isPositive(creditAmount)) {
        response = createCreditInvoiceItems(purchaseId, currency, ids, item, creditAmount);

      } else if (BeeUtils.same(reqInfo.getParameter(Service.VAR_TABLE), TBL_TRIP_COSTS)) {
        response = createTripInvoiceItems(purchaseId, currency, ids, item);
      } else {
        response = createPurchaseInvoiceItems(purchaseId, currency, ids, item);
      }
    } else if (BeeUtils.same(svc, SVC_SEND_MESSAGE)) {
      response = sendMessage(reqInfo.getParameter(COL_DESCRIPTION),
          Codec.beeDeserializeCollection(reqInfo.getParameter(COL_MOBILE)));

    } else if (BeeUtils.same(svc, SVC_GET_CREDIT_INFO)) {
      response = getCreditInfo(reqInfo);

    } else if (BeeUtils.same(svc, SVC_UPDATE_PERCENT)) {
      response = updatePercent(reqInfo.getParameterLong(COL_CARGO_TRIP),
          reqInfo.getParameter(Service.VAR_COLUMN),
          reqInfo.getParameterDouble(COL_AMOUNT),
          BeeUtils.toBoolean(reqInfo.getParameter(Service.VAR_CHECK)));

    } else if (BeeUtils.same(svc, SVC_TRIP_PROFIT_REPORT)) {
      response = rep.getTripProfitReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_FUEL_USAGE_REPORT)) {
      response = rep.getFuelUsageReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_VEHICLE_BUSY_DATES)) {
      response = getVehicleBusyDates(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_VEHICLE)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TRAILER)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TRIP_DATE_FROM)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TRIP_DATE_TO)));

    } else if (BeeUtils.same(svc, SVC_GET_DRIVER_BUSY_DATES)) {
      response = getDriverBusyDates(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_DRIVER)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TRIP_DATE_FROM)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_TRIP_DATE_TO)));

    } else if (BeeUtils.same(svc, SVC_CREATE_USER)) {
      response = createUser(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_TRIP_INFO)) {
      response = rep.getTripInfo(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_TEXT_CONSTANT)) {
      response = getTextConstant(reqInfo);

    } else if (BeeUtils.same(svc, SVC_TRIP_COSTS_REPORT)) {
      response = rep.getTripCostsReport(reqInfo);
    } else {
      String msg = BeeUtils.joinWords("Transport service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    return Lists.newArrayList(
        BeeParameter.createBoolean(module, PRM_EXCLUDE_VAT),
        BeeParameter.createText(module, PRM_TRIP_PREFIX, true, null),
        BeeParameter.createRelation(module, PRM_INVOICE_PREFIX, true, TBL_SALES_SERIES,
            COL_SERIES_NAME),
        BeeParameter.createMap(module, PRM_MESSAGE_TEMPLATE, true, null),
        BeeParameter.createText(module, "SmsServiceAddress"),
        BeeParameter.createText(module, "SmsUserName"),
        BeeParameter.createText(module, "SmsPassword"),
        BeeParameter.createText(module, "SmsServiceId"),
        BeeParameter.createText(module, "SmsDisplayText"),
        BeeParameter.createMap(module, "SmsRequestHeaders"),
        BeeParameter.createRelation(module, PRM_SELF_SERVICE_ROLE, TBL_ROLES, COL_ROLE_NAME),
        BeeParameter.createRelation(module, PRM_CARGO_TYPE, true, TBL_CARGO_TYPES,
            COL_CARGO_TYPE_NAME),
        BeeParameter.createRelation(module, PRM_CARGO_SERVICE, TBL_SERVICES, COL_SERVICE_NAME));
  }

  @Override
  public Module getModule() {
    return Module.TRANSPORT;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public void init() {
    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void calcAssessmentAmounts(ViewQueryEvent event) {
        if (event.isAfter(VIEW_ASSESSMENTS, VIEW_CHILD_ASSESSMENTS) && event.hasData()) {
          BeeRowSet rowSet = event.getRowset();
          SqlSelect query = null;

          for (int i = 0; i < 2; i++) {
            String prfx = "";
            IsExpression xpr;
            IsCondition clause;

            if (i > 0) {
              if (event.isTarget(VIEW_CHILD_ASSESSMENTS)) {
                break;
              }
              prfx = VIEW_CHILD_ASSESSMENTS;
              xpr = SqlUtils.field(TBL_ASSESSMENTS, COL_ASSESSMENT);
              clause = SqlUtils.inList(TBL_ASSESSMENTS, COL_ASSESSMENT, rowSet.getRowIds());
            } else {
              xpr = SqlUtils.field(TBL_ASSESSMENTS, sys.getIdName(TBL_ASSESSMENTS));
              clause = sys.idInList(TBL_ASSESSMENTS, rowSet.getRowIds());
            }
            for (String tbl : new String[] {TBL_CARGO_INCOMES, TBL_CARGO_EXPENSES}) {
              SqlSelect ss = new SqlSelect()
                  .addConstant(prfx + tbl, COL_AMOUNT)
                  .addExpr(xpr, COL_ASSESSMENT)
                  .addFrom(TBL_ASSESSMENTS)
                  .addFromInner(TBL_ORDER_CARGO,
                      sys.joinTables(TBL_ORDER_CARGO, TBL_ASSESSMENTS, COL_CARGO))
                  .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
                  .addFromInner(tbl, SqlUtils.joinUsing(TBL_ASSESSMENTS, tbl, COL_CARGO))
                  .setWhere(clause)
                  .addGroup(xpr);

              IsExpression amountExpr = SqlUtils.field(tbl, COL_AMOUNT);
              IsExpression vatExpr = TradeModuleBean.getVatExpression(tbl, amountExpr);

              if (BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT))) {
                amountExpr = TradeModuleBean.getWithoutVatExpression(tbl, amountExpr);
              } else {
                amountExpr = TradeModuleBean.getTotalExpression(tbl, amountExpr);
              }
              Stream.of(amountExpr, vatExpr).forEach(expr -> {
                IsExpression x = ExchangeUtils.exchangeFieldTo(ss, expr,
                    SqlUtils.field(tbl, COL_CURRENCY), SqlUtils.nvl(SqlUtils.field(tbl, COL_DATE),
                        SqlUtils.field(TBL_ORDERS, COL_DATE)),
                    SqlUtils.constant(prm.getRelation(PRM_CURRENCY)));

                ss.addSum(x, expr == vatExpr ? COL_TRADE_VAT : VAR_TOTAL);
              });
              if (query == null) {
                query = ss;
              } else {
                query.addUnion(ss);
              }
            }
          }
          Table<Long, String, String> cache = HashBasedTable.create();

          for (SimpleRow row : qs.getData(query)) {
            Stream.of(VAR_TOTAL, COL_TRADE_VAT).forEach(name ->
                cache.put(row.getLong(COL_ASSESSMENT), row.getValue(COL_AMOUNT) + name,
                    BeeUtils.nvl(BeeUtils.round(row.getValue(name), 2), "")));
          }
          for (BeeRow row : rowSet.getRows()) {
            cache.row(row.getId()).forEach(row::setProperty);
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillCargoIncomes(ViewQueryEvent event) {
        if (event.isAfter(VIEW_ORDER_CARGO, VIEW_ALL_CARGO, VIEW_SHIPMENT_REQUESTS)
            && event.hasData()) {
          BeeRowSet rowSet = event.getRowset();
          Collection<Long> cargoIds;
          Function<BeeRow, Long> valueSupplier;

          switch (event.getTargetName()) {
            case VIEW_ORDER_CARGO:
              cargoIds = rowSet.getRowIds();
              valueSupplier = BeeRow::getId;
              break;
            case VIEW_ALL_CARGO:
            case VIEW_SHIPMENT_REQUESTS:
              int idx = rowSet.getColumnIndex(COL_CARGO);
              cargoIds = rowSet.getDistinctLongs(idx);
              valueSupplier = row -> row.getLong(idx);
              break;
            default:
              return;
          }
          SimpleRowSet rs = qs.getData(rep.getCargoAmountsQuery(new SqlSelect()
                  .addField(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO), COL_CARGO)
                  .addFrom(TBL_ORDER_CARGO)
                  .setWhere(sys.idInList(TBL_ORDER_CARGO, cargoIds)), TBL_CARGO_INCOMES,
              prm.getRelation(PRM_CURRENCY), BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT)),
              true));

          for (BeeRow row : rowSet.getRows()) {
            SimpleRow r = rs.getRowByKey(COL_CARGO, BeeUtils.toString(valueSupplier.apply(row)));

            row.setProperty(VAR_INCOME,
                BeeUtils.toString(BeeUtils.unbox(r.getDouble(COL_TRANSPORTATION))
                    + BeeUtils.unbox(r.getDouble(COL_SERVICE))));
            row.setProperty(COL_TRANSPORTATION + VAR_INCOME,
                BeeUtils.toString(BeeUtils.unbox(r.getDouble(COL_TRANSPORTATION))));
            row.setProperty(COL_TRADE_VAT,
                BeeUtils.toString(BeeUtils.unbox(r.getDouble(COL_TRANSPORTATION + COL_TRADE_VAT))
                    + BeeUtils.unbox(r.getDouble(COL_SERVICE + COL_TRADE_VAT))));
            row.setProperty(COL_TRANSPORTATION + COL_TRADE_VAT,
                BeeUtils.toString(BeeUtils.unbox(r.getDouble(COL_TRANSPORTATION + COL_TRADE_VAT))));
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillCargoPlaces(ViewQueryEvent event) {
        if (event.isAfter(VIEW_ORDER_CARGO, VIEW_ALL_CARGO, VIEW_ASSESSMENTS,
            TBL_ASSESSMENT_FORWARDERS, VIEW_CARGO_SALES, VIEW_CARGO_CREDIT_SALES,
            VIEW_CARGO_PURCHASES, VIEW_CARGO_TRIPS, VIEW_TRIP_CARGO, VIEW_SHIPMENT_REQUESTS)
            && event.hasData()) {

          BeeRowSet rowSet = event.getRowset();
          BeeView view = sys.getView(event.getTargetName());

          String keyColumn;
          IsCondition clause;
          Function<BeeRow, Long> valueSupplier;

          if (Objects.equals(view.getSourceName(), TBL_CARGO_TRIPS)) {
            keyColumn = COL_CARGO_TRIP;
            clause = sys.idInList(TBL_CARGO_TRIPS, rowSet.getRowIds());
            valueSupplier = BeeRow::getId;

          } else if (Objects.equals(view.getSourceName(), TBL_ORDER_CARGO)) {
            keyColumn = COL_CARGO;
            clause = sys.idInList(TBL_ORDER_CARGO, rowSet.getRowIds());
            valueSupplier = BeeRow::getId;

          } else if (view.hasColumn(COL_CARGO_TRIP)) {
            keyColumn = COL_CARGO_TRIP;
            int idx = rowSet.getColumnIndex(keyColumn);
            clause = sys.idInList(TBL_CARGO_TRIPS, rowSet.getDistinctLongs(idx));
            valueSupplier = row -> row.getLong(idx);

          } else if (view.hasColumn(COL_CARGO)) {
            keyColumn = COL_CARGO;
            int idx = rowSet.getColumnIndex(keyColumn);
            clause = sys.idInList(TBL_ORDER_CARGO, rowSet.getDistinctLongs(idx));
            valueSupplier = row -> row.getLong(idx);
          } else {
            return;
          }
          Table<Long, String, String> places = getExtremes(clause, keyColumn);

          rowSet.getRows().forEach(beeRow ->
              places.row(valueSupplier.apply(beeRow)).forEach(beeRow::setProperty));
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillFuelConsumptions(ViewQueryEvent event) {
        if (event.isAfter(TBL_TRIP_ROUTES) && event.hasData()) {
          BeeRowSet rowset = event.getRowset();
          int colIndex = DataUtils.getColumnIndex(COL_ROUTE_CONSUMPTION, rowset.getColumns());

          if (BeeConst.isUndef(colIndex)) {
            return;
          }
          SimpleRowSet rs = qs.getData(rep.getFuelConsumptionsQuery(event.getQuery()
              .resetFields().resetOrder().resetGroup()
              .addFields(TBL_TRIP_ROUTES, sys.getIdName(TBL_TRIP_ROUTES))
              .addGroup(TBL_TRIP_ROUTES, sys.getIdName(TBL_TRIP_ROUTES)), true));

          for (BeeRow row : rowset.getRows()) {
            row.setValue(colIndex, rs.getValueByKey(sys.getIdName(TBL_TRIP_ROUTES),
                BeeUtils.toString(row.getId()), COL_ROUTE_CONSUMPTION));
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillPercents(ViewQueryEvent event) {
        if (event.isAfter(VIEW_TRIP_CARGO, VIEW_CARGO_TRIPS) && event.hasData()) {
          BeeRowSet rowset = event.getRowset();
          int cargoIndex = rowset.getColumnIndex(COL_CARGO);
          int tripIndex = rowset.getColumnIndex(COL_TRIP);

          if (BeeConst.isUndef(cargoIndex) || BeeConst.isUndef(tripIndex)) {
            return;
          }
          SqlSelect trips = null;

          for (Long tripId : rowset.getDistinctLongs(tripIndex)) {
            SqlSelect subQuery = new SqlSelect().addConstant(tripId, COL_TRIP);

            if (Objects.isNull(trips)) {
              trips = subQuery;
            } else {
              trips.addUnion(subQuery);
            }
          }
          Table<Long, Long, Triplet<String, String, String>> amounts = HashBasedTable.create();

          String crs = rep.getTripIncomes(trips, prm.getRelation(PRM_CURRENCY),
              BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT)));
          SimpleRowSet rs = qs.getData(new SqlSelect().addAllFields(crs).addFrom(crs));
          qs.sqlDropTemp(crs);

          for (SimpleRow row : rs) {
            amounts.put(row.getLong(COL_TRIP), row.getLong(COL_CARGO),
                Triplet.of(BeeUtils.round(BeeUtils.nvl(row.getValue(COL_TRANSPORTATION), "0"), 2)
                        + " (" + BeeUtils.removeTrailingZeros(
                    BeeUtils.round(row.getValue(COL_TRIP_PERCENT), 2)) + "%)",
                    BeeUtils.round(row.getValue(COL_SERVICE), 2), null));
          }
          crs = rep.getCargoTripPercents(COL_TRIP, trips);
          rs = qs.getData(new SqlSelect().addAllFields(crs).addFrom(crs));
          qs.sqlDropTemp(crs);

          for (SimpleRow row : rs) {
            if (!amounts.contains(row.getLong(COL_TRIP), row.getLong(COL_CARGO))) {
              amounts.put(row.getLong(COL_TRIP), row.getLong(COL_CARGO), Triplet.empty());
            }
            amounts.get(row.getLong(COL_TRIP), row.getLong(COL_CARGO))
                .setC(BeeUtils.removeTrailingZeros(BeeUtils.round(row.getValue(COL_CARGO_PERCENT),
                    2)) + "%");
          }
          for (BeeRow row : rowset.getRows()) {
            Triplet<String, String, String> t = amounts.get(row.getLong(tripIndex),
                row.getLong(cargoIndex));
            row.setProperty(VAR_INCOME, t.getA());
            row.setProperty(COL_CARGO + VAR_INCOME, t.getB());
            row.setProperty(VAR_EXPENSE, t.getC());
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillTripNumber(ViewInsertEvent event) {
        if (event.isBefore(VIEW_TRIPS, VIEW_EXPEDITION_TRIPS)
            && BeeConst.isUndef(DataUtils.getColumnIndex(COL_TRIP_NO, event.getColumns()))) {

          BeeParameter parameter = prm.getParameter(PRM_TRIP_PREFIX);
          String prefix;

          if (parameter.supportsUsers()) {
            Long userId;

            if (!BeeConst.isUndef(DataUtils.getColumnIndex(COL_TRIP_MANAGER, event.getColumns()))) {
              userId = DataUtils.getLong(event.getColumns(), event.getRow(), COL_TRIP_MANAGER);
            } else {
              userId = usr.getCurrentUserId();
            }
            prefix = parameter.getText(userId);
          } else {
            prefix = parameter.getText();
          }
          if (!BeeUtils.isEmpty(prefix)) {
            event.addValue(new BeeColumn(COL_TRIP_NO),
                Value.getValue(qs.getNextNumber(TBL_TRIPS, COL_TRIP_NO, prefix, null)));
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void informShipmentRequestCustomer(DataEvent.ViewUpdateEvent event) {
        if (event.isAfter(VIEW_SHIPMENT_REQUESTS)) {
          int col = DataUtils.getColumnIndex(COL_QUERY_STATUS, event.getColumns());

          if (!BeeConst.isUndef(col)) {
            Integer status = event.getRow().getInteger(col);
            TextConstant constant = null;

            if (ShipmentRequestStatus.CONFIRMED.is(status)) {
              constant = TextConstant.REQUEST_CONFIRMED_MAIL_CONTENT;
            } else if (ShipmentRequestStatus.LOST.is(status)) {
              constant = TextConstant.REQUEST_LOST_MAIL_CONTENT;
            }
            if (Objects.isNull(constant)) {
              return;
            }
            BeeRowSet info = qs.getViewData(VIEW_SHIPMENT_REQUESTS,
                Filter.compareId(event.getRow().getId()));

            String email = BeeUtils.notEmpty(info.getString(0, COL_PERSON + COL_EMAIL),
                info.getString(0, COL_QUERY_CUSTOMER_EMAIL));

            if (BeeUtils.isEmpty(email)) {
              return;
            }
            Long accountId = prm.getRelation(MailConstants.PRM_DEFAULT_ACCOUNT);

            if (!DataUtils.isId(accountId)) {
              logger.warning("Default mail account not found");
              return;
            }

            String text = getTextConstant(constant, info.getInteger(0, COL_USER_LOCALE));

            cb.asynchronousCall(new ConcurrencyBean.AsynchronousRunnable() {
              @Override
              public void run() {
                String id = BeeUtils.toString(event.getRow().getId());

                mail.sendMail(accountId, email, null,
                    text.replace("[CONTRACT_ID]", id).replace("{CONTRACT_ID}", id));
              }
            });
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void updateAssessmentRelations(ViewInsertEvent event) {
        String tbl = sys.getViewSource(event.getTargetName());

        if (BeeUtils.inList(tbl, TBL_ASSESSMENTS, TBL_ASSESSMENT_FORWARDERS) && event.isAfter()) {
          String fld;
          String tblFrom;
          String joinFrom;

          if (BeeUtils.same(tbl, TBL_ASSESSMENTS)) {
            fld = COL_ORDER;
            tblFrom = TBL_ORDER_CARGO;
            joinFrom = COL_CARGO;
          } else {
            fld = COL_TRIP;
            tblFrom = TBL_CARGO_TRIPS;
            joinFrom = COL_CARGO_TRIP;
          }
          IsCondition clause = sys.idEquals(tbl, event.getRow().getId());

          qs.updateData(new SqlUpdate(tbl)
              .addExpression(fld, new SqlSelect()
                  .addFields(tblFrom, fld)
                  .addFrom(tbl)
                  .addFromInner(tblFrom, sys.joinTables(tblFrom, tbl, joinFrom))
                  .setWhere(clause))
              .setWhere(clause));
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void onOrderStatusChange(DataEvent.ViewUpdateEvent event) {
        if (event.isAfter(VIEW_ORDERS, VIEW_ASSESSMENTS)) {
          int col = DataUtils.getColumnIndex(event.isAfter(VIEW_ORDERS)
              ? COL_STATUS : COL_ORDER + COL_STATUS, event.getColumns());
          Long  orderId;

          if (event.isAfter(VIEW_ORDERS)) {
            orderId = event.getRow().getId();
          } else {
            orderId = qs.getLongById(TBL_ASSESSMENTS, event.getRow().getId(), COL_ORDER);
          }
          if (!BeeConst.isUndef(col) && DataUtils.isId(orderId)) {
            Integer status = event.getRow().getInteger(col);
            ShipmentRequestStatus shipmentStatus = null;
            AssessmentStatus assessmentStatus = null;
            TextConstant textConstant = null;

            if (OrderStatus.COMPLETED.is(status)) {
              shipmentStatus = ShipmentRequestStatus.COMPLETED;
              assessmentStatus = AssessmentStatus.COMPLETED;
              textConstant = TextConstant.COMPLETED_ORDER_EMAIL_CONTENT;

            } else if (OrderStatus.ACTIVE.is(status)) {
              shipmentStatus = ShipmentRequestStatus.CONFIRMED;
              assessmentStatus = AssessmentStatus.APPROVED;

            } else if (OrderStatus.CANCELED.is(status)) {
              shipmentStatus = ShipmentRequestStatus.REJECTED;
            }
            if (shipmentStatus != null) {
              qs.updateData(new SqlUpdate(TBL_SHIPMENT_REQUESTS)
                  .addConstant(COL_STATUS, shipmentStatus)
                  .setWhere(SqlUtils.and(
                      SqlUtils.notEqual(TBL_SHIPMENT_REQUESTS, COL_STATUS, shipmentStatus),
                      SqlUtils.in(TBL_SHIPMENT_REQUESTS, COL_CARGO, VIEW_ORDER_CARGO,
                          sys.getIdName(VIEW_ORDER_CARGO),
                          SqlUtils.equals(VIEW_ORDER_CARGO, COL_ORDER, orderId)))));
            }
            if (assessmentStatus != null) {
              qs.updateData(new SqlUpdate(VIEW_ASSESSMENTS)
                  .addConstant(COL_STATUS, assessmentStatus)
                  .setWhere(SqlUtils.and(
                      SqlUtils.notEqual(VIEW_ASSESSMENTS, COL_STATUS, assessmentStatus),
                      SqlUtils.in(VIEW_ASSESSMENTS, COL_CARGO, VIEW_ORDER_CARGO,
                          sys.getIdName(VIEW_ORDER_CARGO),
                          SqlUtils.equals(VIEW_ORDER_CARGO, COL_ORDER, orderId)))));
            }
            if (textConstant != null) {
              SqlSelect emailSql = new SqlSelect()
                  .addFields(TBL_EMAILS, COL_EMAIL)
                  .addFrom(TBL_EMAILS)
                  .addFromLeft(TBL_CONTACTS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL));

              String email = qs.getValue(emailSql.copyOf()
                  .addFromLeft(VIEW_COMPANIES,
                      sys.joinTables(TBL_CONTACTS, VIEW_COMPANIES, COL_CONTACT))
                  .addFromLeft(VIEW_ORDERS,
                      sys.joinTables(VIEW_COMPANIES, VIEW_ORDERS, COL_CUSTOMER))
                  .setWhere(sys.idEquals(VIEW_ORDERS, orderId)));

              if (BeeUtils.isEmpty(email)) {
                email = qs.getValue(emailSql.copyOf()
                    .addFromLeft(VIEW_COMPANY_PERSONS,
                        sys.joinTables(TBL_CONTACTS, VIEW_COMPANY_PERSONS, COL_CONTACT))
                    .addFromLeft(VIEW_ORDERS, sys.joinTables(VIEW_COMPANY_PERSONS, VIEW_ORDERS,
                        COL_CUSTOMER + COL_PERSON))
                    .setWhere(sys.idEquals(VIEW_ORDERS, orderId)));
              }
              if (BeeUtils.isEmpty(email)) {
                return;
              }
              Long accountId = prm.getRelation(MailConstants.PRM_DEFAULT_ACCOUNT);

              if (!DataUtils.isId(accountId)) {
                logger.warning("Default mail account not found");
                return;
              }
              String text = getTextConstant(textConstant, usr.getSupportedLocale().ordinal());

              if (BeeUtils.isEmpty(text)) {
                return;
              }
              mail.sendMail(accountId, email, null, text);
            }
          }
        }
      }
    });

    BeeView.registerConditionProvider(PROP_CARGO_HANDLING, (view, args) -> {
      String col = BeeUtils.getQuietly(args, 0);
      String val = BeeUtils.getQuietly(args, 1);

      if (BeeUtils.anyEmpty(col, val)) {
        return null;
      }
      String keyColumn;
      IsExpression keyExpression;

      if (Objects.equals(view.getSourceName(), TBL_CARGO_TRIPS)) {
        keyColumn = COL_CARGO_TRIP;
        keyExpression = SqlUtils.field(view.getSourceAlias(), view.getSourceIdName());

      } else if (Objects.equals(view.getSourceName(), TBL_ORDER_CARGO)) {
        keyColumn = COL_CARGO;
        keyExpression = SqlUtils.field(view.getSourceAlias(), view.getSourceIdName());

      } else if (view.hasColumn(COL_CARGO_TRIP)) {
        keyColumn = COL_CARGO_TRIP;
        keyExpression = view.getColumnExpression(keyColumn);

        if (Objects.isNull(keyExpression)) {
          keyExpression = SqlUtils.field(view.getColumnSource(keyColumn),
              view.getColumnField(keyColumn));
        }
      } else if (view.hasColumn(COL_CARGO)) {
        keyColumn = COL_CARGO;
        keyExpression = view.getColumnExpression(keyColumn);

        if (Objects.isNull(keyExpression)) {
          keyExpression = SqlUtils.field(view.getColumnSource(keyColumn),
              view.getColumnField(keyColumn));
        }
      } else {
        return null;
      }
      int mode;

      if (BeeUtils.isPrefix(col, VAR_UNLOADING)) {
        mode = 1;
        col = BeeUtils.removePrefix(col, VAR_UNLOADING);
      } else {
        mode = 0;
        col = BeeUtils.removePrefix(col, VAR_LOADING);
      }
      String als = "tmpSubQuery";

      SqlSelect query = new SqlSelect().setDistinctMode(true)
          .addFields(als, keyColumn)
          .addFrom(TBL_CARGO_PLACES)
          .addFromInner(getHandlingQuery(null, Objects.equals(keyColumn, COL_CARGO_TRIP)), als,
              SqlUtils.and(SqlUtils.joinUsing(TBL_CARGO_PLACES, als,
                  sys.getIdName(TBL_CARGO_PLACES)), SqlUtils.equals(als, VAR_UNLOADING, mode)));

      BeeTable table = sys.getTable(TBL_CARGO_PLACES);
      IsCondition clause;

      if (Objects.equals(col, ALS_CITY_NAME)) {
        clause = SqlUtils.or(SqlUtils.and(SqlUtils.isNull(TBL_CITIES, COL_CITY_NAME),
            SqlUtils.contains(TBL_CARGO_PLACES, COL_PLACE_CITY + VAR_UNBOUND, val)),
            SqlUtils.contains(TBL_CITIES, COL_CITY_NAME, val));
        query.addFromLeft(TBL_CITIES,
            sys.joinTables(TBL_CITIES, TBL_CARGO_PLACES, COL_PLACE_CITY));

      } else if (Objects.equals(col, ALS_COUNTRY_NAME)) {
        clause = SqlUtils.or(SqlUtils.and(SqlUtils.isNull(TBL_COUNTRIES, COL_COUNTRY_NAME),
            SqlUtils.contains(TBL_CARGO_PLACES, COL_PLACE_COUNTRY + VAR_UNBOUND, val)),
            SqlUtils.contains(TBL_COUNTRIES, COL_COUNTRY_NAME, val));
        query.addFromLeft(TBL_COUNTRIES,
            sys.joinTables(TBL_COUNTRIES, TBL_CARGO_PLACES, COL_PLACE_COUNTRY));

      } else if (Objects.equals(col, COL_PLACE_DATE)) {
        String start = null;
        String end = null;
        int i = 0;

        for (String s : Splitter.on(BeeConst.CHAR_COMMA).trimResults().split(val)) {
          if (i == 0) {
            start = s;
          } else if (i == 1) {
            end = s;
          }

          i++;
        }

        if (BeeUtils.isEmpty(end)) {
          clause = SqlUtils.moreEqual(TBL_CARGO_PLACES, col, start);

        } else if (BeeUtils.isEmpty(start)) {
          clause = SqlUtils.lessEqual(TBL_CARGO_PLACES, col, end);

        } else {
          clause = SqlUtils.and(SqlUtils.moreEqual(TBL_CARGO_PLACES, col, start),
              SqlUtils.lessEqual(TBL_CARGO_PLACES, col, end));

        }
      } else if (table.hasField(col)) {
        clause = SqlUtils.contains(TBL_CARGO_PLACES, col, val);
      } else {
        return null;
      }
      return SqlUtils.and(SqlUtils.notNull(keyExpression),
          SqlUtils.in(keyExpression, query.setWhere(clause)));
    });

    HeadlineProducer assessmentsHeadlineProducer =
        (feed, userId, rowSet, row, isNew, constants, dtfInfo) -> {
          String caption = "";
          String pid = DataUtils.getString(rowSet, row, COL_ASSESSMENT);

          if (!BeeUtils.isEmpty(pid)) {
            caption = BeeUtils.joinWords(caption, constants.captionPid() + BeeConst.STRING_COLON,
                pid);
          }

          String id = BeeUtils.toString(row.getId());

          caption = BeeUtils.joinWords(caption, constants.captionId() + BeeConst.STRING_COLON, id);

          AssessmentStatus status =
              EnumUtils.getEnumByIndex(AssessmentStatus.class,
                  DataUtils.getInteger(rowSet, row, COL_STATUS));

          if (status != null) {
            caption = BeeUtils.joinWords(caption, status.getCaption(constants));
          }

          String notes = DataUtils.getString(rowSet, row, ALS_ORDER_NOTES);
          String customer = DataUtils.getString(rowSet, row, TransportConstants.ALS_CUSTOMER_NAME);

          caption = BeeUtils.joinWords(caption, notes, customer);

          return Headline.create(row.getId(), caption, isNew);
        };

    news.registerUsageQueryProvider(Feed.ORDER_CARGO, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.notNull(TBL_ORDER_CARGO, COL_ORDER));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_ORDER_CARGO, news.joinUsage(TBL_ORDER_CARGO));
      }
    });

    news.registerUsageQueryProvider(Feed.TRANSPORTATION_ORDERS_MY,
        new ExtendedUsageQueryProvider() {
          @Override
          protected List<IsCondition> getConditions(long userId) {
            return NewsHelper.buildConditions(SqlUtils.equals(TBL_ORDERS, COL_ORDER_MANAGER,
                userId));
          }

          @Override
          protected List<Pair<String, IsCondition>> getJoins() {
            return NewsHelper.buildJoin(TBL_ORDERS, news.joinUsage(TBL_ORDERS));
          }
        });

    news.registerUsageQueryProvider(Feed.TRIPS, new ExtendedUsageQueryProvider() {
      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_TRIPS, news.joinUsage(TBL_TRIPS));
      }

      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.isNull(TBL_TRIPS, COL_EXPEDITION));
      }
    });

    news.registerUsageQueryProvider(Feed.TRIPS_MY, new ExtendedUsageQueryProvider() {
      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_TRIPS, news.joinUsage(TBL_TRIPS));
      }

      @Override
      protected List<IsCondition> getConditions(long userId) {
        SqlSelect vehicleQuery = new SqlSelect().setDistinctMode(true)
            .addFields(TBL_VEHICLE_GROUPS, COL_VEHICLE)
            .addFrom(TBL_VEHICLE_GROUPS)
            .addFromInner(TBL_TRANSPORT_GROUPS,
                sys.joinTables(TBL_TRANSPORT_GROUPS, TBL_VEHICLE_GROUPS, COL_GROUP))
            .setWhere(SqlUtils.equals(TBL_TRANSPORT_GROUPS, COL_GROUP_MANAGER, userId));

        IsCondition userCondition = SqlUtils.or(
            SqlUtils.equals(TBL_TRIPS, COL_TRIP_MANAGER, userId),
            SqlUtils.in(TBL_TRIPS, COL_VEHICLE, vehicleQuery));

        return NewsHelper.buildConditions(SqlUtils.isNull(TBL_TRIPS, COL_EXPEDITION),
            userCondition);
      }
    });

    news.registerUsageQueryProvider(Feed.SHIPMENT_REQUESTS_UNREGISTERED_MY,
        new ShipmentRequestsUsageQueryProvider());

    news.registerUsageQueryProvider(Feed.SHIPMENT_REQUESTS_MY,
        new ShipmentRequestsUsageQueryProvider());

    news.registerUsageQueryProvider(Feed.SHIPMENT_REQUESTS_UNREGISTERED_ALL,
        new ExtendedUsageQueryProvider() {
          @Override
          protected List<IsCondition> getConditions(long userId) {
            return NewsHelper.buildConditions(SqlUtils.isNull(TBL_SHIPMENT_REQUESTS,
                COL_COMPANY_PERSON));
          }

          @Override
          protected List<Pair<String, IsCondition>> getJoins() {
            return NewsHelper.buildJoin(TBL_SHIPMENT_REQUESTS,
                news.joinUsage(TBL_SHIPMENT_REQUESTS));
          }
        });

    news.registerUsageQueryProvider(Feed.SHIPMENT_REQUESTS_ALL, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.notNull(TBL_SHIPMENT_REQUESTS,
            COL_COMPANY_PERSON));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_SHIPMENT_REQUESTS, news.joinUsage(TBL_SHIPMENT_REQUESTS));
      }
    });

    news.registerHeadlineProducer(Feed.ASSESSMENT_REQUESTS_ALL, assessmentsHeadlineProducer);

    news.registerUsageQueryProvider(Feed.ASSESSMENT_REQUESTS_ALL,
        new AssesmentRequestsUsageQueryProvider(false));

    news.registerHeadlineProducer(Feed.ASSESSMENT_REQUESTS_MY, assessmentsHeadlineProducer);

    news.registerUsageQueryProvider(Feed.ASSESSMENT_REQUESTS_MY,
        new AssesmentRequestsUsageQueryProvider(true));

    news.registerHeadlineProducer(Feed.ASSESSMENT_ORDERS_ALL, assessmentsHeadlineProducer);

    news.registerUsageQueryProvider(Feed.ASSESSMENT_ORDERS_ALL,
        new AssesmentRequestsUsageQueryProvider(false, true));

    news.registerHeadlineProducer(Feed.ASSESSMENT_ORDERS_MY, assessmentsHeadlineProducer);

    news.registerUsageQueryProvider(Feed.ASSESSMENT_ORDERS_MY,
        new AssesmentRequestsUsageQueryProvider(true, true));

    news.registerUsageQueryProvider(Feed.CARGO_SALES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.isNull(TBL_CARGO_INCOMES, COL_SALE));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_CARGO_INCOMES, news.joinUsage(TBL_CARGO_INCOMES));
      }
    });

    news.registerUsageQueryProvider(Feed.CARGO_CREDIT_SALES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.isNull(TBL_CARGO_INCOMES, COL_PURCHASE),
            SqlUtils.isNull(TBL_SALES, COL_SALE_PROFORMA),
            SqlUtils.notNull(TBL_CARGO_INCOMES, COL_SALE));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoins(TBL_CARGO_INCOMES, news.joinUsage(TBL_CARGO_INCOMES),
            TBL_SALES, sys.joinTables(TBL_SALES, TBL_CARGO_INCOMES, COL_SALE));
      }
    });

    news.registerUsageQueryProvider(Feed.CARGO_PURCHASES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.isNull(TBL_CARGO_EXPENSES, COL_PURCHASE));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_CARGO_EXPENSES, news.joinUsage(TBL_CARGO_EXPENSES));
      }
    });

    news.registerUsageQueryProvider(Feed.CARGO_INVOICES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.isNull(TBL_SALES, COL_SALE_PROFORMA));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoins(TBL_SALES, news.joinUsage(TBL_SALES),
            TBL_CARGO_INCOMES, sys.joinTables(TBL_SALES, TBL_CARGO_INCOMES, COL_SALE));
      }
    });

    news.registerUsageQueryProvider(Feed.CARGO_PROFORMA_INVOICES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.notNull(TBL_SALES, COL_SALE_PROFORMA));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoins(TBL_SALES, news.joinUsage(TBL_SALES),
            TBL_CARGO_INCOMES, sys.joinTables(TBL_SALES, TBL_CARGO_INCOMES, COL_SALE));
      }
    });

    news.registerUsageQueryProvider(Feed.CARGO_CREDIT_INVOICES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return null;
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoins(TBL_PURCHASES, news.joinUsage(TBL_PURCHASES),
            TBL_CARGO_INCOMES, sys.joinTables(TBL_PURCHASES, TBL_CARGO_INCOMES, COL_PURCHASE));
      }
    });

    news.registerUsageQueryProvider(Feed.CARGO_PURCHASE_INVOICES, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return null;
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoins(TBL_PURCHASES, news.joinUsage(TBL_PURCHASES),
            TBL_CARGO_EXPENSES, sys.joinTables(TBL_PURCHASES, TBL_CARGO_EXPENSES, COL_PURCHASE));
      }
    });

    news.registerUsageQueryProvider(Feed.ASSESSMENT_TRANSPORTATIONS, new UsageQueryProvider() {
      @Override
      public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
          DateTime startDate) {
        return new SqlSelect()
            .addFields(TBL_TRIP_USAGE, COL_TRIP)
            .addMax(TBL_TRIP_USAGE, NewsConstants.COL_USAGE_ACCESS)
            .addFrom(TBL_TRIP_USAGE)
            .addFromInner(TBL_TRIPS, SqlUtils.join(TBL_TRIPS, sys.getIdName(TBL_TRIPS),
                TBL_TRIP_USAGE, COL_TRIP))
            .addFromLeft(
                TBL_ASSESSMENT_FORWARDERS,
                SqlUtils.join(TBL_ASSESSMENT_FORWARDERS, COL_TRIP, TBL_TRIPS, sys
                    .getIdName(TBL_TRIPS)))
            .addFromLeft(
                TBL_CARGO_TRIPS,
                SqlUtils.join(TBL_CARGO_TRIPS, COL_TRIP, TBL_TRIPS, sys
                    .getIdName(TBL_TRIPS)))
            .addFromLeft(TBL_ASSESSMENTS,
                SqlUtils.join(TBL_ASSESSMENTS, COL_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
            .setWhere(SqlUtils.and(
                SqlUtils.isNull(TBL_ASSESSMENT_FORWARDERS, COL_TRIP),
                SqlUtils.notNull(TBL_ASSESSMENTS, COL_CARGO),
                SqlUtils.equals(TBL_TRIP_USAGE, NewsConstants.COL_UF_USER, userId),
                SqlUtils.notNull(TBL_TRIP_USAGE, NewsConstants.COL_USAGE_ACCESS)))
            .addGroup(TBL_TRIP_USAGE, COL_TRIP);
      }

      @Override
      public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
          DateTime startDate) {
        return new SqlSelect()
            .addFields(TBL_TRIP_USAGE, COL_TRIP)
            .addMax(TBL_TRIP_USAGE, NewsConstants.COL_USAGE_UPDATE)
            .addFrom(TBL_TRIP_USAGE)
            .addFromInner(TBL_TRIPS, SqlUtils.join(TBL_TRIPS, sys.getIdName(TBL_TRIPS),
                TBL_TRIP_USAGE, COL_TRIP))
            .addFromLeft(
                TBL_ASSESSMENT_FORWARDERS,
                SqlUtils.join(TBL_ASSESSMENT_FORWARDERS, COL_TRIP, TBL_TRIPS, sys
                    .getIdName(TBL_TRIPS)))
            .addFromLeft(
                TBL_CARGO_TRIPS,
                SqlUtils.join(TBL_CARGO_TRIPS, COL_TRIP, TBL_TRIPS, sys
                    .getIdName(TBL_TRIPS)))
            .addFromLeft(TBL_ASSESSMENTS,
                SqlUtils.join(TBL_ASSESSMENTS, COL_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
            .setWhere(SqlUtils.and(
                SqlUtils.isNull(TBL_ASSESSMENT_FORWARDERS, COL_TRIP),
                SqlUtils.notNull(TBL_ASSESSMENTS, COL_CARGO),
                SqlUtils.notEqual(TBL_TRIP_USAGE, NewsConstants.COL_UF_USER, userId),
                SqlUtils.more(TBL_TRIP_USAGE, NewsConstants.COL_USAGE_UPDATE,
                    NewsHelper.getStartTime(startDate))))
            .addGroup(TBL_TRIP_USAGE, COL_TRIP);
      }
    });
  }

  @Schedule(persistent = false)
  private void checkRequestStatus() {
    SqlSelect query = new SqlSelect().setDistinctMode(true)
        .addField(TBL_SHIPMENT_REQUESTS, sys.getIdName(TBL_SHIPMENT_REQUESTS), "id")
        .addField(TBL_SHIPMENT_REQUESTS, sys.getVersionName(TBL_SHIPMENT_REQUESTS), "version")
        .addFields(TBL_SHIPMENT_REQUESTS, COL_QUERY_STATUS)
        .addFrom(TBL_SHIPMENT_REQUESTS)
        .addFromLeft(TBL_ORDER_CARGO,
            sys.joinTables(TBL_ORDER_CARGO, TBL_SHIPMENT_REQUESTS, COL_CARGO))
        .addFromLeft(TBL_CARGO_LOADING,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_LOADING, COL_CARGO))
        .addFromLeft(TBL_CARGO_PLACES,
            sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_LOADING, COL_LOADING_PLACE))
        .setWhere(SqlUtils.and(SqlUtils.not(SqlUtils.inList(TBL_SHIPMENT_REQUESTS, COL_QUERY_STATUS,
            ShipmentRequestStatus.CONFIRMED, ShipmentRequestStatus.LOST)),
            SqlUtils.or(SqlUtils.isNull(TBL_CARGO_PLACES, COL_PLACE_DATE),
                SqlUtils.less(TBL_CARGO_PLACES, COL_PLACE_DATE, TimeUtils.startOfDay(1)))));

    SimpleRowSet expired = qs.getData(query);

    if (!DataUtils.isEmpty(expired)) {
      BeeColumn col = DataUtils.getColumn(COL_QUERY_STATUS,
          sys.getView(VIEW_SHIPMENT_REQUESTS).getRowSetColumns());

      for (SimpleRow row : expired) {
        BeeRowSet rs = DataUtils.getUpdated(VIEW_SHIPMENT_REQUESTS, row.getLong("id"),
            row.getLong("version"), col, row.getValue(COL_QUERY_STATUS),
            BeeUtils.toString(ShipmentRequestStatus.LOST.ordinal()));

        deb.commitRow(rs, RowInfo.class);
      }
      DataChangeEvent.fireRefresh((event, locality) ->
          Endpoint.sendToAll(new ModificationMessage(event)), VIEW_SHIPMENT_REQUESTS);
    }
  }

  private ResponseObject createCreditInvoiceItems(Long purchaseId, Long currency, Set<Long> idList,
      Long mainItem, Double amount) {

    if (!DataUtils.isId(purchaseId)) {
      return ResponseObject.error("Wrong account ID");
    }
    if (!BeeUtils.isPositive(amount)) {
      return ResponseObject.error("Wrong amount");
    }
    if (!DataUtils.isId(currency)) {
      return ResponseObject.error("Wrong currency ID");
    }
    if (BeeUtils.isEmpty(idList)) {
      return ResponseObject.error("Empty ID list");
    }
    IsCondition wh = sys.idInList(TBL_CARGO_INCOMES, idList);

    SqlSelect ss = new SqlSelect()
        .addFields(TBL_ORDERS, COL_ORDER_NO)
        .addFields(TBL_SALES_SERIES, COL_SERIES_NAME)
        .addFields(TBL_SALES, COL_TRADE_INVOICE_NO)
        .addFields(TBL_CARGO_INCOMES, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)
        .addFrom(TBL_CARGO_INCOMES)
        .addFromInner(TBL_SERVICES,
            sys.joinTables(TBL_SERVICES, TBL_CARGO_INCOMES, COL_SERVICE))
        .addFromInner(TBL_SALES,
            sys.joinTables(TBL_SALES, TBL_CARGO_INCOMES, COL_SALE))
        .addFromLeft(TBL_SALES_SERIES,
            sys.joinTables(TBL_SALES_SERIES, TBL_SALES, COL_TRADE_SALE_SERIES))
        .addFromInner(TBL_ORDER_CARGO,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
        .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .setWhere(SqlUtils.and(wh, SqlUtils.positive(TBL_CARGO_INCOMES, COL_AMOUNT)))
        .addGroup(TBL_ORDERS, COL_ORDER_NO)
        .addGroup(TBL_SALES_SERIES, COL_SERIES_NAME)
        .addGroup(TBL_SALES, COL_TRADE_INVOICE_NO)
        .addGroup(TBL_CARGO_INCOMES, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC);

    if (DataUtils.isId(mainItem)) {
      ss.addConstant(mainItem, COL_ITEM);
    } else {
      ss.addFields(TBL_SERVICES, COL_ITEM)
          .addGroup(TBL_SERVICES, COL_ITEM);
    }
    IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
        SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT),
        SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY),
        SqlUtils.nvl(SqlUtils.field(TBL_CARGO_INCOMES, COL_DATE),
            SqlUtils.field(TBL_ORDERS, COL_ORDER_DATE)), SqlUtils.constant(currency));

    SimpleRowSet rs = qs.getData(ss.addSum(xpr, COL_AMOUNT));
    ResponseObject response = new ResponseObject();

    double totalAmount = 0;

    for (Double n : rs.getDoubleColumn(COL_AMOUNT)) {
      totalAmount += BeeUtils.unbox(n);
    }
    for (SimpleRow row : rs) {
      String xml = XmlUtils.createString("CreditInfo",
          COL_ORDER_NO, row.getValue(COL_ORDER_NO),
          COL_TRADE_INVOICE_NO, BeeUtils.joinWords(row.getValue(COL_SERIES_NAME),
              row.getValue(COL_TRADE_INVOICE_NO)));

      SqlInsert insert = new SqlInsert(TBL_PURCHASE_ITEMS)
          .addConstant(COL_PURCHASE, purchaseId)
          .addConstant(COL_ITEM, row.getLong(COL_ITEM))
          .addConstant(COL_TRADE_ITEM_QUANTITY, 1)
          .addConstant(COL_TRADE_ITEM_PRICE,
              BeeUtils.round(amount * row.getDouble(COL_AMOUNT) / totalAmount, 2))
          .addConstant(COL_TRADE_VAT_PLUS, row.getBoolean(COL_TRADE_VAT_PLUS))
          .addConstant(COL_TRADE_VAT, row.getDouble(COL_TRADE_VAT))
          .addConstant(COL_TRADE_VAT_PERC, row.getBoolean(COL_TRADE_VAT_PERC))
          .addConstant(COL_TRADE_ITEM_NOTE, xml);

      qs.insertData(insert);
    }
    return response.addErrorsFrom(qs.updateDataWithResponse(new SqlUpdate(TBL_CARGO_INCOMES)
        .addConstant(COL_PURCHASE, purchaseId)
        .setWhere(wh)));
  }

  private ResponseObject createInvoiceItems(Long saleId, Long currency, Set<Long> idList,
      Long mainItem) {
    if (!DataUtils.isId(saleId)) {
      return ResponseObject.error("Wrong account ID");
    }
    if (!DataUtils.isId(currency)) {
      return ResponseObject.error("Wrong currency ID");
    }
    if (BeeUtils.isEmpty(idList)) {
      return ResponseObject.error("Empty ID list");
    }
    IsCondition wh = sys.idInList(TBL_CARGO_INCOMES, idList);

    SqlSelect ss = new SqlSelect()
        .addFields(TBL_ORDERS, COL_ORDER_NO, COL_ORDER_NOTES)
        .addFields(TBL_ORDER_CARGO, COL_NUMBER)
        .addFields(TBL_CARGO_INCOMES,
            COL_CARGO, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)
        .addField(TBL_ASSESSMENTS, sys.getIdName(TBL_ASSESSMENTS), COL_ASSESSMENT)
        .addField(DocumentConstants.TBL_DOCUMENTS, DocumentConstants.COL_DOCUMENT_NUMBER,
            ALS_CARGO_CMR_NUMBER)
        .addFrom(TBL_CARGO_INCOMES)
        .addFromInner(TBL_SERVICES,
            sys.joinTables(TBL_SERVICES, TBL_CARGO_INCOMES, COL_SERVICE))
        .addFromInner(TBL_ORDER_CARGO,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
        .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_ASSESSMENTS,
            sys.joinTables(TBL_ORDER_CARGO, TBL_ASSESSMENTS, COL_CARGO))
        .addFromLeft(DocumentConstants.TBL_DOCUMENTS,
            sys.joinTables(DocumentConstants.TBL_DOCUMENTS, TBL_ORDER_CARGO, COL_CARGO_CMR))
        .setWhere(wh);

    if (DataUtils.isId(mainItem)) {
      ss.addConstant(mainItem, COL_ITEM)
          .addConstant(true, COL_TRANSPORTATION);
    } else {
      ss.addFields(TBL_SERVICES, COL_ITEM, COL_TRANSPORTATION);
    }
    IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
        SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT),
        SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY),
        SqlUtils.nvl(SqlUtils.field(TBL_CARGO_INCOMES, COL_DATE),
            SqlUtils.field(TBL_ORDERS, COL_ORDER_DATE)), SqlUtils.constant(currency));

    SimpleRowSet rs = qs.getData(ss.addExpr(xpr, COL_AMOUNT));
    ResponseObject response = new ResponseObject();

    Multimap<Long, String> drivers = HashMultimap.create();
    Multimap<Long, String> vehicles = HashMultimap.create();

    String vehicle = SqlUtils.uniqueName();
    String trailer = SqlUtils.uniqueName();

    IsCondition clause = SqlUtils.inList(TBL_CARGO_TRIPS, COL_CARGO,
        Arrays.asList(rs.getLongColumn(COL_CARGO)));

    SimpleRowSet tripData = qs.getData(new SqlSelect()
        .addFields(TBL_CARGO_TRIPS, COL_CARGO)
        .addField(vehicle, COL_VEHICLE_NUMBER, vehicle)
        .addField(trailer, COL_VEHICLE_NUMBER, trailer)
        .addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME)
        .addFrom(TBL_CARGO_TRIPS)
        .addFromInner(TBL_TRIPS, sys.joinTables(TBL_TRIPS, TBL_CARGO_TRIPS, COL_TRIP))
        .addFromLeft(TBL_VEHICLES, vehicle,
            sys.joinTables(TBL_VEHICLES, vehicle, TBL_TRIPS, COL_VEHICLE))
        .addFromLeft(TBL_VEHICLES, trailer,
            sys.joinTables(TBL_VEHICLES, trailer, TBL_TRIPS, COL_TRAILER))
        .addFromLeft(TBL_TRIP_DRIVERS, sys.joinTables(TBL_TRIPS, TBL_TRIP_DRIVERS, COL_TRIP))
        .addFromLeft(TBL_DRIVERS, sys.joinTables(TBL_DRIVERS, TBL_TRIP_DRIVERS, COL_DRIVER))
        .addFromLeft(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_DRIVERS, COL_COMPANY_PERSON))
        .addFromLeft(TBL_PERSONS,
            sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
        .setWhere(SqlUtils.and(clause, SqlUtils.isNull(TBL_TRIPS, COL_EXPEDITION))));

    for (SimpleRow trip : tripData) {
      Long cargo = trip.getLong(COL_CARGO);
      String txt = BeeUtils.joinWords(trip.getValue(COL_FIRST_NAME), trip.getValue(COL_LAST_NAME));

      if (!BeeUtils.isEmpty(txt)) {
        drivers.put(cargo, txt);
      }
      txt = BeeUtils.join("/", trip.getValue(vehicle), trip.getValue(trailer));

      if (!BeeUtils.isEmpty(txt)) {
        vehicles.put(cargo, txt);
      }
    }
    tripData = qs.getData(new SqlSelect()
        .addFields(TBL_CARGO_TRIPS, COL_CARGO)
        .addFields(TBL_TRIPS, COL_FORWARDER_VEHICLE, COL_FORWARDER_DRIVER)
        .addFrom(TBL_CARGO_TRIPS)
        .addFromInner(TBL_TRIPS, sys.joinTables(TBL_TRIPS, TBL_CARGO_TRIPS, COL_TRIP))
        .setWhere(SqlUtils.and(clause, SqlUtils.notNull(TBL_TRIPS, COL_EXPEDITION))));

    for (SimpleRow trip : tripData) {
      Long cargo = trip.getLong(COL_CARGO);
      String txt = trip.getValue(COL_FORWARDER_DRIVER);

      if (!BeeUtils.isEmpty(txt)) {
        drivers.put(cargo, txt);
      }
      txt = trip.getValue(COL_FORWARDER_VEHICLE);

      if (!BeeUtils.isEmpty(txt)) {
        vehicles.put(cargo, txt);
      }
    }
    Table<Long, String, String> places = getExtremes(sys.idInList(TBL_ORDER_CARGO,
        Arrays.asList(rs.getLongColumn(COL_CARGO))), COL_CARGO);

    String[] tableFields = new String[] {
        COL_ITEM, COL_TRADE_VAT_PLUS, COL_TRADE_VAT,
        COL_TRADE_VAT_PERC};

    String[] group = DataUtils.isId(mainItem) ? tableFields : rs.getColumnNames();
    Map<String, Multimap<String, String>> map = new HashMap<>();
    Map<String, Double> amounts = new HashMap<>();

    DateTimeFormatInfo dateTimeFormatInfo = usr.getDateTimeFormatInfo();

    for (SimpleRow row : rs) {
      String key = "";

      for (String fld : group) {
        if (!fld.equals(COL_AMOUNT)) {
          key += fld + row.getValue(fld);
        }
      }
      if (!map.containsKey(key)) {
        map.put(key, TreeMultimap.create());
      }
      Multimap<String, String> valueMap = map.get(key);

      for (String fld : tableFields) {
        String value = row.getValue(fld);

        if (!BeeUtils.isEmpty(value)) {
          valueMap.put(fld, value);
        }
      }
      for (String fld : new String[] {
          COL_ORDER_NO, COL_ASSESSMENT, ALS_CARGO_CMR_NUMBER, COL_NUMBER}) {
        String value = row.getValue(fld);

        if (!BeeUtils.isEmpty(value)) {
          valueMap.put(fld, value);
        }
      }
      if (BeeUtils.unbox(row.getBoolean(COL_TRANSPORTATION))) {
        Long cargo = row.getLong(COL_CARGO);

        String value = BeeUtils.join("\n", row.getValue(COL_ORDER_NOTES), BeeUtils.join("-",
            BeeUtils.joinWords(places.get(cargo, ALS_LOADING_COUNTRY_CODE),
                BeeUtils.parenthesize(places.get(cargo, ALS_LOADING_COUNTRY_NAME))),
            BeeUtils.joinWords(places.get(cargo, ALS_UNLOADING_COUNTRY_CODE),
                BeeUtils.parenthesize(places.get(cargo, ALS_UNLOADING_COUNTRY_NAME)))));

        if (!BeeUtils.isEmpty(value)) {
          valueMap.put(COL_ORDER_NOTES, value);
        }
        for (String fld : new String[] {ALS_LOADING_DATE, ALS_UNLOADING_DATE}) {
          DateTime time = TimeUtils.toDateTimeOrNull(places.get(cargo, fld));

          if (time != null) {
            valueMap.put(fld, Formatter.renderDate(dateTimeFormatInfo, time));
          }
        }

        if (drivers.containsKey(cargo)) {
          valueMap.putAll(COL_DRIVER, drivers.get(cargo));
        }
        if (vehicles.containsKey(cargo)) {
          valueMap.putAll(COL_VEHICLE, vehicles.get(cargo));
        }
      }
      amounts.put(key, BeeUtils.unbox(amounts.get(key))
          + BeeUtils.unbox(row.getDouble(COL_AMOUNT)));
    }
    for (Entry<String, Multimap<String, String>> entry : map.entrySet()) {
      Multimap<String, String> values = entry.getValue();

      SqlInsert insert = new SqlInsert(TBL_SALE_ITEMS)
          .addConstant(COL_SALE, saleId)
          .addConstant(COL_TRADE_ITEM_QUANTITY, 1)
          .addConstant(COL_TRADE_ITEM_PRICE, BeeUtils.round(amounts.get(entry.getKey()), 2));

      List<String> nodes = new ArrayList<>();

      for (String fld : values.keySet()) {
        if (ArrayUtils.contains(tableFields, fld)) {
          insert.addConstant(fld, BeeUtils.peek(values.get(fld)));
        } else {
          nodes.add(fld);
          nodes.add(BeeUtils.joinItems(values.get(fld)));
        }
      }
      qs.insertData(insert.addConstant(COL_TRADE_ITEM_NOTE,
          XmlUtils.createString("CargoInfo", nodes.toArray(new String[0]))));
    }
    return response.addErrorsFrom(qs.updateDataWithResponse(new SqlUpdate(TBL_CARGO_INCOMES)
        .addConstant(COL_SALE, saleId)
        .setWhere(wh)));
  }

  private ResponseObject createPurchaseInvoiceItems(Long purchaseId, Long currency,
      Set<Long> idList, Long mainItem) {

    if (!DataUtils.isId(purchaseId)) {
      return ResponseObject.error("Wrong account ID");
    }
    if (!DataUtils.isId(currency)) {
      return ResponseObject.error("Wrong currency ID");
    }
    if (BeeUtils.isEmpty(idList)) {
      return ResponseObject.error("Empty ID list");
    }
    IsCondition wh = sys.idInList(TBL_CARGO_EXPENSES, idList);

    SqlSelect ss = new SqlSelect()
        .addFields(TBL_CARGO_EXPENSES, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)
        .addFrom(TBL_CARGO_EXPENSES)
        .addFromInner(TBL_SERVICES,
            sys.joinTables(TBL_SERVICES, TBL_CARGO_EXPENSES, COL_SERVICE))
        .addFromInner(TBL_ORDER_CARGO,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_EXPENSES, COL_CARGO))
        .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .setWhere(wh)
        .addGroup(TBL_CARGO_EXPENSES, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC);

    if (DataUtils.isId(mainItem)) {
      ss.addConstant(mainItem, COL_ITEM);
    } else {
      ss.addFields(TBL_SERVICES, COL_ITEM)
          .addGroup(TBL_SERVICES, COL_ITEM);
    }
    IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
        SqlUtils.field(TBL_CARGO_EXPENSES, COL_AMOUNT),
        SqlUtils.field(TBL_CARGO_EXPENSES, COL_CURRENCY),
        SqlUtils.nvl(SqlUtils.field(TBL_CARGO_EXPENSES, COL_DATE),
            SqlUtils.field(TBL_ORDERS, COL_ORDER_DATE)), SqlUtils.constant(currency));

    SimpleRowSet rs = qs.getData(ss.addSum(xpr, COL_AMOUNT));
    ResponseObject response = new ResponseObject();

    for (SimpleRow row : rs) {
      SqlInsert insert = new SqlInsert(TBL_PURCHASE_ITEMS)
          .addConstant(COL_PURCHASE, purchaseId)
          .addConstant(COL_ITEM, row.getLong(COL_ITEM))
          .addConstant(COL_TRADE_ITEM_QUANTITY, 1)
          .addConstant(COL_TRADE_ITEM_PRICE, BeeUtils.round(row.getDouble(COL_AMOUNT), 2))
          .addConstant(COL_TRADE_VAT_PLUS, row.getBoolean(COL_TRADE_VAT_PLUS))
          .addConstant(COL_TRADE_VAT, row.getDouble(COL_TRADE_VAT))
          .addConstant(COL_TRADE_VAT_PERC, row.getBoolean(COL_TRADE_VAT_PERC));

      qs.insertData(insert);
    }
    return response.addErrorsFrom(qs.updateDataWithResponse(new SqlUpdate(TBL_CARGO_EXPENSES)
        .addConstant(COL_PURCHASE, purchaseId)
        .setWhere(wh)));
  }

  private ResponseObject createTripInvoiceItems(Long purchaseId, Long currency, Set<Long> idList,
      Long mainItem) {

    IsCondition wh = sys.idInList(TBL_TRIP_COSTS, idList);

    SqlSelect ss = new SqlSelect()
        .addConstant(purchaseId, COL_PURCHASE)
        .addFields(TBL_TRIP_COSTS, COL_TRADE_VAT_PLUS, COL_TRADE_VAT_PERC)
        .addField(TBL_TRIP_COSTS, COL_COSTS_NOTE, COL_TRADE_ITEM_NOTE)
        .addFrom(TBL_TRIP_COSTS)
        .setWhere(wh)
        .addGroup(TBL_TRIP_COSTS, COL_TRADE_VAT_PLUS, COL_COSTS_VAT, COL_TRADE_VAT_PERC,
            COL_COSTS_NOTE);

    if (DataUtils.isId(mainItem)) {
      IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
          TradeModuleBean.getAmountExpression(TBL_TRIP_COSTS),
          SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_CURRENCY),
          SqlUtils.field(TBL_TRIP_COSTS, COL_DATE), SqlUtils.constant(currency));

      ss.addConstant(mainItem, COL_ITEM)
          .addConstant(1, COL_TRADE_ITEM_QUANTITY)
          .addSum(xpr, COL_TRADE_ITEM_PRICE);
    } else {
      IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
          SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_PRICE),
          SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_CURRENCY),
          SqlUtils.field(TBL_TRIP_COSTS, COL_DATE), SqlUtils.constant(currency));

      ss.addFields(TBL_TRIP_COSTS, COL_ITEM)
          .addSum(TBL_TRIP_COSTS, COL_COSTS_QUANTITY, COL_TRADE_ITEM_QUANTITY)
          .addMax(xpr, COL_TRADE_ITEM_PRICE)
          .addGroup(TBL_TRIP_COSTS, COL_ITEM, COL_COSTS_PRICE);
    }
    IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
        SqlUtils.field(TBL_TRIP_COSTS, COL_TRADE_VAT),
        SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_CURRENCY),
        SqlUtils.field(TBL_TRIP_COSTS, COL_DATE), SqlUtils.constant(currency));

    ss.addMax(SqlUtils.sqlIf(SqlUtils.isNull(TBL_TRIP_COSTS, COL_TRADE_VAT_PERC), xpr,
        SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_VAT)), COL_TRADE_VAT);

    qs.loadData(TBL_PURCHASE_ITEMS, ss);

    ResponseObject response = new ResponseObject();

    return response.addErrorsFrom(qs.updateDataWithResponse(new SqlUpdate(TBL_TRIP_COSTS)
        .addConstant(COL_PURCHASE, purchaseId)
        .setWhere(wh)));
  }

  private ResponseObject createUser(RequestInfo reqInfo) {
    String login = reqInfo.getParameter(COL_LOGIN);
    if (BeeUtils.isEmpty(login)) {
      return ResponseObject.parameterNotFound(SVC_CREATE_USER, COL_LOGIN);
    }
    String password = reqInfo.getParameter(COL_PASSWORD);
    if (BeeUtils.isEmpty(password)) {
      return ResponseObject.parameterNotFound(SVC_CREATE_USER, COL_PASSWORD);
    }
    if (usr.isUser(login)) {
      return ResponseObject.warning(usr.getDictionary()
          .valueExists(BeeUtils.joinWords(usr.getDictionary().user(), login)));
    }
    Long role = prm.getRelation(PRM_SELF_SERVICE_ROLE);

    if (!DataUtils.isId(role)) {
      return ResponseObject.parameterNotFound(SVC_CREATE_USER, PRM_SELF_SERVICE_ROLE);
    }
    ResponseObject resp = ResponseObject.info(usr.getDictionary().newUser(), login);
    String email;

    try {
      email = new InternetAddress(reqInfo.getParameter(COL_EMAIL), true).getAddress();
    } catch (AddressException e) {
      return ResponseObject.error(e);
    }
    Long accountId = prm.getRelation(MailConstants.PRM_DEFAULT_ACCOUNT);

    if (!DataUtils.isId(accountId)) {
      return ResponseObject.parameterNotFound(SVC_CREATE_USER, MailConstants.PRM_DEFAULT_ACCOUNT);
    }
    Long user = qs.insertData(new SqlInsert(TBL_USERS)
        .addConstant(COL_LOGIN, login)
        .addConstant(COL_PASSWORD, Codec.encodePassword(password))
        .addConstant(COL_COMPANY_PERSON, reqInfo.getParameter(COL_COMPANY_PERSON))
        .addConstant(COL_USER_INTERFACE, UserInterface.SELF_SERVICE));

    qs.insertData(new SqlInsert(TBL_USER_ROLES)
        .addConstant(COL_USER, user)
        .addConstant(COL_ROLE, role));

    String text = getTextConstant(TextConstant.REGISTRATION_MAIL_CONTENT,
        reqInfo.getParameterInt(COL_USER_LOCALE));

    if (!BeeUtils.isEmpty(text)) {
      cb.asynchronousCall(new ConcurrencyBean.AsynchronousRunnable() {
        @Override
        public void run() {
          mail.sendMail(accountId, email, Localized.dictionary().registration(),
              text.replace("[LOGIN]", login).replace("{LOGIN}", login)
                  .replace("[PASSWORD]", password).replace("{PASSWORD}", password));
        }
      });
    }
    return resp;
  }

  private ResponseObject generateDailyCosts(long tripId) {
    Long mainCountry = prm.getRelation(PRM_COUNTRY);

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(TBL_COUNTRY_NORMS, COL_COUNTRY, COL_DAILY_COSTS_ITEM)
        .addFields(TBL_COUNTRY_DAILY_COSTS, COL_AMOUNT, COL_CURRENCY)
        .addExpr(SqlUtils.sqlIf(SqlUtils.or(
            SqlUtils.isNull(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_FROM),
            SqlUtils.joinLess(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_FROM,
                TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_DATE)),
            SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_DATE),
            SqlUtils.field(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_FROM)), COL_ROUTE_DEPARTURE_DATE)
        .addExpr(SqlUtils.sqlIf(
            SqlUtils.or(SqlUtils.isNull(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_TO),
                SqlUtils.joinMore(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_TO,
                    TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_DATE)),
            SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_DATE),
            SqlUtils.field(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_TO)), COL_ROUTE_ARRIVAL_DATE)
        .addFrom(TBL_TRIP_ROUTES)
        .addFromInner(TBL_COUNTRY_NORMS,
            SqlUtils.compare(SqlUtils.sqlIf(SqlUtils.equals(TBL_TRIP_ROUTES,
                COL_ROUTE_ARRIVAL_COUNTRY, mainCountry),
                SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_COUNTRY),
                SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_COUNTRY)), Operator.EQ,
                SqlUtils.field(TBL_COUNTRY_NORMS, COL_COUNTRY)))
        .addFromInner(TBL_COUNTRY_DAILY_COSTS, SqlUtils.and(
            sys.joinTables(TBL_COUNTRY_NORMS, TBL_COUNTRY_DAILY_COSTS, COL_COUNTRY_NORM),
            SqlUtils.notNull(TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_DATE),
            SqlUtils.joinMoreEqual(TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_DATE, TBL_TRIP_ROUTES,
                COL_ROUTE_DEPARTURE_DATE),
            SqlUtils.or(SqlUtils.isNull(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_FROM),
                SqlUtils.joinLess(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_FROM,
                    TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_DATE)),
            SqlUtils.or(SqlUtils.isNull(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_TO),
                SqlUtils.joinMore(TBL_COUNTRY_DAILY_COSTS, COL_TRIP_DATE_TO,
                    TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_DATE))))
        .setWhere(SqlUtils.equals(TBL_TRIP_ROUTES, COL_TRIP, tripId))
        .addOrderDesc(null, COL_ROUTE_DEPARTURE_DATE, COL_ROUTE_ARRIVAL_DATE,
            sys.getIdName(TBL_TRIP_ROUTES)));

    qs.updateData(new SqlDelete(TBL_TRIP_COSTS)
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TRIP_COSTS, COL_TRIP, tripId),
            SqlUtils.in(TBL_TRIP_COSTS, COL_COSTS_ITEM, TBL_COUNTRY_NORMS, COL_DAILY_COSTS_ITEM))));

    Map<String, Map<String, String>> map = new HashMap<>();
    JustDate lastArrivalTime = null;
    String lastKey = null;

    for (SimpleRow row : rs) {
      JustDate arrivalTime = row.getDateTime(COL_ROUTE_ARRIVAL_DATE).getDate();
      String key;

      if (TimeUtils.sameDate(arrivalTime, lastArrivalTime)) {
        key = lastKey;
      } else {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(COL_COSTS_COUNTRY, row.getValue(COL_COUNTRY));
        values.put(COL_COSTS_ITEM, row.getValue(COL_DAILY_COSTS_ITEM));
        values.put(COL_COSTS_PRICE, row.getValue(COL_AMOUNT));
        values.put(COL_COSTS_CURRENCY, row.getValue(COL_CURRENCY));

        key = Codec.md5(BeeUtils.joinItems(arrivalTime, BeeUtils.joinItems(values.values())));

        if (!map.containsKey(key)) {
          map.put(key, values);
        }
        lastArrivalTime = arrivalTime;
        lastKey = key;
      }
      Map<String, String> values = map.get(key);

      values.put(COL_COSTS_QUANTITY,
          BeeUtils.toString(BeeUtils.toInt(values.get(COL_COSTS_QUANTITY))
              + TimeUtils.dayDiff(row.getDateTime(COL_ROUTE_DEPARTURE_DATE), arrivalTime)));
    }
    if (!BeeUtils.isEmpty(lastKey)) {
      Map<String, String> values = map.get(lastKey);
      values.put(COL_COSTS_QUANTITY,
          BeeUtils.toString(BeeUtils.toInt(values.get(COL_COSTS_QUANTITY)) + 1));
    }
    Map<String, Map<String, String>> newMap = new HashMap<>();

    for (Map<String, String> values : map.values()) {
      int qty = Math.max(BeeUtils.toInt(values.get(COL_COSTS_QUANTITY)), 1);
      values.remove(COL_COSTS_QUANTITY);
      String key = Codec.md5(BeeUtils.joinItems(values.values()));

      if (newMap.containsKey(key)) {
        qty += BeeUtils.toInt(newMap.get(key).get(COL_COSTS_QUANTITY));
      } else {
        newMap.put(key, values);
      }
      newMap.get(key).put(COL_COSTS_QUANTITY, BeeUtils.toString(qty));
    }
    JustDate date = qs.getDateTime(new SqlSelect()
        .addFields(TBL_TRIPS, COL_TRIP_DATE)
        .addFrom(TBL_TRIPS)
        .setWhere(sys.idEquals(TBL_TRIPS, tripId))).getDate();

    Long payment = ArrayUtils.getQuietly(qs.getLongColumn(new SqlSelect()
        .addFields(TBL_PAYMENT_TYPES, sys.getIdName(TBL_PAYMENT_TYPES))
        .addFrom(TBL_PAYMENT_TYPES)
        .setWhere(SqlUtils.notNull(TBL_PAYMENT_TYPES, COL_PAYMENT_CASH))), 0);

    Long[] drivers = qs.getLongColumn(new SqlSelect()
        .addFields(TBL_TRIP_DRIVERS, sys.getIdName(TBL_TRIP_DRIVERS))
        .addFrom(TBL_TRIP_DRIVERS)
        .setWhere(SqlUtils.equals(TBL_TRIP_DRIVERS, COL_TRIP, tripId)));

    for (Long driver : drivers) {
      for (Map<String, String> values : newMap.values()) {
        SqlInsert insert = new SqlInsert(TBL_TRIP_COSTS)
            .addConstant(COL_TRIP, tripId)
            .addConstant(COL_COSTS_DATE, date)
            .addConstant(COL_DRIVER, driver)
            .addNotNull(COL_PAYMENT_TYPE, payment);

        for (Entry<String, String> entry : values.entrySet()) {
          insert.addConstant(entry.getKey(), entry.getValue());
        }
        qs.insertData(insert);
      }
    }
    return ResponseObject.info(Localized.dictionary().createdRows(drivers.length * newMap.size()));
  }

  private ResponseObject generateTripRoute(long tripId) {
    Long currentCargo = null;
    double currentWeight = 0;
    DateTime currentDate = null;
    Long currentCountry = null;
    Long currentCity = null;

    Multimap<Long, Map<String, Object>> data = LinkedListMultimap.create();
    Map<Long, Pair<Integer, Integer>> stack = new HashMap<>();
    Set<Long> nonPartials = new HashSet<>();

    String als = "tmpSubQuery";

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(als, COL_CARGO_TRIP, VAR_UNLOADING)
        .addFields(TBL_ORDER_CARGO, COL_CARGO_PARTIAL)
        .addFields(TBL_CARGO_PLACES, COL_PLACE_DATE, COL_PLACE_COUNTRY, COL_PLACE_CITY,
            COL_LOADED_KILOMETERS, COL_EMPTY_KILOMETERS, COL_ROUTE_WEIGHT)
        .addFrom(TBL_CARGO_PLACES)
        .addFromInner(getHandlingQuery(sys.idEquals(TBL_TRIPS, tripId), true), als,
            SqlUtils.joinUsing(TBL_CARGO_PLACES, als, sys.getIdName(TBL_CARGO_PLACES)))
        .addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, als, COL_CARGO))
        .addOrder(TBL_CARGO_PLACES, COL_PLACE_ORDINAL, COL_PLACE_DATE)
        .addOrder(als, VAR_UNLOADING));

    for (SimpleRow row : rs) {
      Long orderCargo = row.getLong(COL_CARGO_TRIP);
      boolean loaded = stack.containsKey(orderCargo);
      boolean unloading = BeeUtils.unbox(row.getBoolean(VAR_UNLOADING));

      if (unloading && !loaded) {
        continue;
      }
      double weight = BeeUtils.unbox(row.getDouble(COL_ROUTE_WEIGHT));
      DateTime date = row.getDateTime(COL_PLACE_DATE);
      Long country = row.getLong(COL_PLACE_COUNTRY);
      Long city = row.getLong(COL_PLACE_CITY);

      if (!BeeUtils.isEmpty(stack) && TimeUtils.isMore(date, currentDate)) {
        Map<String, Object> rec = new HashMap<>();
        data.put(unloading || !BeeUtils.isPositive(currentWeight) ? orderCargo : currentCargo, rec);

        rec.put(COL_ROUTE_DEPARTURE_DATE, currentDate);
        rec.put(COL_ROUTE_DEPARTURE_COUNTRY, currentCountry);
        rec.put(COL_ROUTE_DEPARTURE_CITY, currentCity);
        rec.put(COL_ROUTE_WEIGHT, currentWeight);
        rec.put(COL_ROUTE_ARRIVAL_DATE, date);
        rec.put(COL_ROUTE_ARRIVAL_COUNTRY, country);
        rec.put(COL_ROUTE_ARRIVAL_CITY, city);
      }
      currentDate = date;
      currentCountry = country;
      currentCity = city;

      if (unloading) {
        Pair<Integer, Integer> pair = stack.get(orderCargo);

        int emptyKm = BeeUtils.unbox(row.getInt(COL_EMPTY_KILOMETERS));
        pair.setA(pair.getA() + BeeUtils.unbox(row.getInt(COL_LOADED_KILOMETERS)) + emptyKm);
        pair.setB(pair.getB() + emptyKm);

        currentWeight -= weight;
      } else {
        if (!loaded) {
          if (!BeeUtils.unbox(row.getBoolean(COL_CARGO_PARTIAL))) {
            nonPartials.add(orderCargo);
          }
          stack.put(orderCargo, Pair.of(0, 0));
        }
        currentCargo = orderCargo;
        currentWeight += weight;
      }
    }
    if (currentWeight > 0) {
      Map<String, Object> rec = new HashMap<>();
      data.put(currentCargo, rec);

      rec.put(COL_ROUTE_DEPARTURE_DATE, currentDate);
      rec.put(COL_ROUTE_DEPARTURE_COUNTRY, currentCountry);
      rec.put(COL_ROUTE_DEPARTURE_CITY, currentCity);
      rec.put(COL_ROUTE_WEIGHT, currentWeight);
    }
    for (Long orderCargo : stack.keySet()) {
      if (data.containsKey(orderCargo)) {
        Collection<Map<String, Object>> recs = data.get(orderCargo);
        Map<String, Object> info = new HashMap<>();

        info.put(COL_ROUTE_KILOMETERS, stack.get(orderCargo).getA().doubleValue() / recs.size());
        info.put(COL_EMPTY_KILOMETERS, stack.get(orderCargo).getB().doubleValue() / recs.size());
        info.put(COL_ROUTE_CARGO, nonPartials.isEmpty() || nonPartials.contains(orderCargo)
            ? orderCargo : nonPartials.iterator().next());

        for (Map<String, Object> rec : recs) {
          rec.putAll(info);
        }
      }
    }
    if (data.isEmpty()) {
      return ResponseObject.warning(Localized.dictionary().noData());
    }
    qs.updateData(new SqlDelete(TBL_TRIP_ROUTES)
        .setWhere(SqlUtils.equals(TBL_TRIP_ROUTES, COL_TRIP, tripId)));

    for (Map<String, Object> rec : data.values()) {
      SqlInsert insert = new SqlInsert(TBL_TRIP_ROUTES)
          .addConstant(COL_TRIP, tripId);

      for (Entry<String, Object> entry : rec.entrySet()) {
        insert.addConstant(entry.getKey(), entry.getValue());

        if (Objects.equals(entry.getKey(), COL_ROUTE_DEPARTURE_DATE)) {
          insert.addConstant(COL_ROUTE_SEASON,
              BeeUtils.betweenInclusive(((DateTime) entry.getValue()).getMonth(), 4, 10)
                  ? FuelSeason.SUMMER.ordinal() : FuelSeason.WINTER.ordinal());
        }
      }
      qs.insertData(insert);
    }
    return ResponseObject.emptyResponse();
  }

  private ResponseObject getAssessmentQuantityReport(RequestInfo reqInfo) {
    Long startDate = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_FROM));
    Long endDate = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_TO));

    Set<Long> departments = DataUtils.parseIdSet(reqInfo.getParameter(AR_DEPARTMENT));
    Set<Long> managers = DataUtils.parseIdSet(reqInfo.getParameter(AR_MANAGER));

    List<String> groupBy = NameUtils.toList(reqInfo.getParameter(Service.VAR_GROUP_BY));

    HasConditions where = SqlUtils.and(SqlUtils.notNull(TBL_ASSESSMENTS, COL_ASSESSMENT_STATUS));

    if (startDate != null) {
      where.add(SqlUtils.moreEqual(TBL_ORDERS, COL_ORDER_DATE, startDate));
    }
    if (endDate != null) {
      where.add(SqlUtils.less(TBL_ORDERS, COL_ORDER_DATE, endDate));
    }

    if (!departments.isEmpty()) {
      where.add(SqlUtils.inList(TBL_ASSESSMENTS, COL_DEPARTMENT, departments));
    }
    if (!managers.isEmpty()) {
      where.add(SqlUtils.inList(TBL_USERS, COL_COMPANY_PERSON, managers));
    }

    SqlSelect query = new SqlSelect();
    query.addFields(TBL_ASSESSMENTS, COL_ASSESSMENT_STATUS, COL_ASSESSMENT);

    if (groupBy.contains(BeeConst.MONTH)) {
      query.addFields(TBL_ORDERS, COL_ORDER_DATE);
      query.addEmptyNumeric(BeeConst.YEAR, 4, 0);
      query.addEmptyNumeric(BeeConst.MONTH, 2, 0);
    }

    if (groupBy.contains(AR_DEPARTMENT)) {
      query.addFields(TBL_ASSESSMENTS, COL_DEPARTMENT);
    }
    if (groupBy.contains(AR_MANAGER)) {
      query.addFields(TBL_USERS, COL_COMPANY_PERSON);
    }

    query.addFrom(TBL_ASSESSMENTS);
    if (!managers.isEmpty() || groupBy.contains(AR_MANAGER) || groupBy.contains(BeeConst.MONTH)
        || startDate != null || endDate != null) {

      query.addFromInner(TBL_ORDER_CARGO,
          sys.joinTables(TBL_ORDER_CARGO, TBL_ASSESSMENTS, COL_CARGO));
      query.addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER));

      if (!managers.isEmpty() || groupBy.contains(AR_MANAGER)) {
        query.addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, TBL_ORDERS, COL_ORDER_MANAGER));
      }
    }

    query.setWhere(where);

    String tmp = qs.sqlCreateTemp(query);

    long count;
    if (groupBy.contains(BeeConst.MONTH)) {
      count = qs.setYearMonth(tmp, COL_ORDER_DATE, BeeConst.YEAR, BeeConst.MONTH);
    } else {
      count = qs.sqlCount(tmp, null);
    }

    if (count <= 0) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.emptyResponse();
    }

    query = new SqlSelect();
    query.addFrom(tmp);

    for (String by : groupBy) {
      switch (by) {
        case BeeConst.MONTH:
          query.addFields(tmp, BeeConst.YEAR, BeeConst.MONTH);
          query.addGroup(tmp, BeeConst.YEAR, BeeConst.MONTH);
          query.addOrder(tmp, BeeConst.YEAR, BeeConst.MONTH);
          break;

        case AR_DEPARTMENT:
          query.addFields(tmp, COL_DEPARTMENT);
          query.addFields(TBL_DEPARTMENTS, COL_DEPARTMENT_NAME);

          query.addFromLeft(TBL_DEPARTMENTS,
              SqlUtils.join(TBL_DEPARTMENTS, sys.getIdName(TBL_DEPARTMENTS), tmp, COL_DEPARTMENT));

          query.addGroup(tmp, COL_DEPARTMENT);
          query.addGroup(TBL_DEPARTMENTS, COL_DEPARTMENT_NAME);
          query.addOrder(TBL_DEPARTMENTS, COL_DEPARTMENT_NAME);
          break;

        case AR_MANAGER:
          query.addFields(tmp, COL_COMPANY_PERSON);
          query.addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);

          query.addFromLeft(TBL_COMPANY_PERSONS,
              SqlUtils.join(TBL_COMPANY_PERSONS, sys.getIdName(TBL_COMPANY_PERSONS),
                  tmp, COL_COMPANY_PERSON));
          query.addFromLeft(TBL_PERSONS,
              sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON));

          query.addGroup(tmp, COL_COMPANY_PERSON);
          query.addGroup(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);

          query.addOrder(TBL_PERSONS, COL_LAST_NAME, COL_FIRST_NAME);
          query.addOrder(tmp, COL_COMPANY_PERSON);
          break;
      }
    }

    query.addCount(AR_RECEIVED);

    IsExpression xpr = SqlUtils.field(tmp, COL_ASSESSMENT_STATUS);

    query.addSum(SqlUtils.sqlCase(xpr, AssessmentStatus.ANSWERED.ordinal(), 1, 0), AR_ANSWERED);
    query.addSum(SqlUtils.sqlCase(xpr, AssessmentStatus.LOST.ordinal(), 1, 0), AR_LOST);
    query.addSum(SqlUtils.sqlCase(xpr, AssessmentStatus.APPROVED.ordinal(), 1, 0), AR_APPROVED);

    query.addSum(SqlUtils.sqlIf(SqlUtils.isNull(tmp, COL_ASSESSMENT), 0, 1), AR_SECONDARY);

    SimpleRowSet result = qs.getData(query);
    qs.sqlDropTemp(tmp);

    if (DataUtils.isEmpty(result)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result);
    }
  }

  private ResponseObject getAssessmentTurnoverReport(RequestInfo reqInfo) {
    Long startDate = reqInfo.getParameterLong(Service.VAR_FROM);
    Long endDate = reqInfo.getParameterLong(Service.VAR_TO);

    Long currency = reqInfo.getParameterLong(COL_CURRENCY);

    Set<Long> departments = DataUtils.parseIdSet(reqInfo.getParameter(AR_DEPARTMENT));
    Set<Long> managers = DataUtils.parseIdSet(reqInfo.getParameter(AR_MANAGER));
    Set<Long> customers = DataUtils.parseIdSet(reqInfo.getParameter(AR_CUSTOMER));

    List<String> groupBy = NameUtils.toList(reqInfo.getParameter(Service.VAR_GROUP_BY));

    HasConditions where = SqlUtils.and(
        SqlUtils.equals(TBL_ORDERS, COL_STATUS, OrderStatus.COMPLETED.ordinal()),
        SqlUtils.in(TBL_ASSESSMENTS, COL_CARGO, TBL_CARGO_INCOMES, COL_CARGO,
            SqlUtils.notNull(TBL_CARGO_INCOMES, COL_SALE)),
        SqlUtils.not(SqlUtils.in(TBL_ASSESSMENTS, COL_CARGO, TBL_CARGO_INCOMES, COL_CARGO,
            SqlUtils.isNull(TBL_CARGO_INCOMES, COL_SALE))));

    if (startDate != null || endDate != null) {
      if (startDate != null) {
        where.add(SqlUtils.moreEqual(TBL_ORDERS, COL_ORDER_DATE, startDate));
      }
      if (endDate != null) {
        where.add(SqlUtils.less(TBL_ORDERS, COL_ORDER_DATE, endDate));
      }
    } else {
      where.add(SqlUtils.notNull(TBL_ORDERS, COL_ORDER_DATE));
    }

    if (!departments.isEmpty()) {
      where.add(SqlUtils.inList(TBL_ASSESSMENTS, COL_DEPARTMENT, departments));
    }
    if (!managers.isEmpty()) {
      where.add(SqlUtils.inList(TBL_USERS, COL_COMPANY_PERSON, managers));
    }
    if (!customers.isEmpty()) {
      where.add(SqlUtils.inList(TBL_ORDERS, COL_CUSTOMER, customers));
    }

    SqlSelect query = new SqlSelect();
    query.addFields(TBL_ASSESSMENTS, COL_ASSESSMENT, COL_CARGO);

    query.addFields(TBL_ORDERS, COL_ORDER_DATE);

    if (groupBy.contains(BeeConst.MONTH)) {
      query.addEmptyNumeric(BeeConst.YEAR, 4, 0);
      query.addEmptyNumeric(BeeConst.MONTH, 2, 0);
    }

    if (groupBy.contains(AR_DEPARTMENT)) {
      query.addFields(TBL_ASSESSMENTS, COL_DEPARTMENT);
    }
    if (groupBy.contains(AR_MANAGER)) {
      query.addFields(TBL_USERS, COL_COMPANY_PERSON);
    }
    if (groupBy.contains(AR_CUSTOMER)) {
      query.addFields(TBL_ORDERS, COL_CUSTOMER);
    }

    query.addFrom(TBL_ASSESSMENTS)
        .addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, TBL_ASSESSMENTS, COL_CARGO))
        .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER));

    if (!managers.isEmpty() || groupBy.contains(AR_MANAGER)) {
      query.addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, TBL_ORDERS, COL_ORDER_MANAGER));
    }

    query.setWhere(where);

    String tmp = qs.sqlCreateTemp(query);

    long count;
    if (groupBy.contains(BeeConst.MONTH)) {
      count = qs.setYearMonth(tmp, COL_ORDER_DATE, BeeConst.YEAR, BeeConst.MONTH);
    } else {
      count = qs.sqlCount(tmp, null);
    }

    if (count <= 0) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.emptyResponse();
    }

    query = new SqlSelect();
    query.addFrom(tmp);

    for (String by : groupBy) {
      switch (by) {
        case BeeConst.MONTH:
          query.addFields(tmp, BeeConst.YEAR, BeeConst.MONTH);
          query.addGroup(tmp, BeeConst.YEAR, BeeConst.MONTH);
          query.addOrder(tmp, BeeConst.YEAR, BeeConst.MONTH);
          break;

        case AR_DEPARTMENT:
          query.addFields(tmp, COL_DEPARTMENT);
          query.addFields(TBL_DEPARTMENTS, COL_DEPARTMENT_NAME);

          query.addFromLeft(TBL_DEPARTMENTS,
              SqlUtils.join(TBL_DEPARTMENTS, sys.getIdName(TBL_DEPARTMENTS), tmp, COL_DEPARTMENT));

          query.addGroup(tmp, COL_DEPARTMENT);
          query.addGroup(TBL_DEPARTMENTS, COL_DEPARTMENT_NAME);
          query.addOrder(TBL_DEPARTMENTS, COL_DEPARTMENT_NAME);
          break;

        case AR_MANAGER:
          query.addFields(tmp, COL_COMPANY_PERSON);
          query.addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);

          query.addFromLeft(TBL_COMPANY_PERSONS,
              SqlUtils.join(TBL_COMPANY_PERSONS, sys.getIdName(TBL_COMPANY_PERSONS),
                  tmp, COL_COMPANY_PERSON));
          query.addFromLeft(TBL_PERSONS,
              sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON));

          query.addGroup(tmp, COL_COMPANY_PERSON);
          query.addGroup(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);

          query.addOrder(TBL_PERSONS, COL_LAST_NAME, COL_FIRST_NAME);
          query.addOrder(tmp, COL_COMPANY_PERSON);
          break;

        case AR_CUSTOMER:
          query.addFields(tmp, COL_CUSTOMER);
          query.addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME);

          query.addFromLeft(TBL_COMPANIES,
              SqlUtils.join(TBL_COMPANIES, sys.getIdName(TBL_COMPANIES), tmp, COL_CUSTOMER));

          query.addGroup(tmp, COL_CUSTOMER);
          query.addGroup(TBL_COMPANIES, COL_COMPANY_NAME);
          query.addOrder(TBL_COMPANIES, COL_COMPANY_NAME);
          break;
      }
    }

    query.addCount(AR_RECEIVED);

    if (!DataUtils.isId(currency)) {
      currency = prm.getRelation(PRM_CURRENCY);
    }

    SqlSelect subIncome = new SqlSelect()
        .addFields(tmp, COL_CARGO)
        .addFrom(tmp)
        .addFromInner(TBL_CARGO_INCOMES,
            SqlUtils.join(TBL_CARGO_INCOMES, COL_CARGO, tmp, COL_CARGO))
        .addGroup(tmp, COL_CARGO);

    IsExpression incomeXpr = getAssessmentTurnoverExpression(subIncome, TBL_CARGO_INCOMES,
        tmp, COL_ORDER_DATE, currency, BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT)));
    subIncome.addSum(incomeXpr, AR_INCOME);

    SqlSelect subExpense = new SqlSelect()
        .addFields(tmp, COL_CARGO)
        .addFrom(tmp)
        .addFromInner(TBL_CARGO_EXPENSES,
            SqlUtils.join(TBL_CARGO_EXPENSES, COL_CARGO, tmp, COL_CARGO))
        .addGroup(tmp, COL_CARGO);

    IsExpression expenseXpr = getAssessmentTurnoverExpression(subExpense, TBL_CARGO_EXPENSES,
        tmp, COL_ORDER_DATE, currency, BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT)));
    subExpense.addSum(expenseXpr, AR_EXPENSE);

    String incomeAlias = "Inc" + SqlUtils.uniqueName();
    String expenseAlias = "Exp" + SqlUtils.uniqueName();

    query.addFromLeft(subIncome, incomeAlias,
        SqlUtils.join(incomeAlias, COL_CARGO, tmp, COL_CARGO));
    query.addFromLeft(subExpense, expenseAlias,
        SqlUtils.join(expenseAlias, COL_CARGO, tmp, COL_CARGO));

    query.addSum(incomeAlias, AR_INCOME);
    query.addSum(expenseAlias, AR_EXPENSE);

    IsCondition condition = SqlUtils.isNull(tmp, COL_ASSESSMENT);
    query.addSum(SqlUtils.sqlIf(condition, 0, 1), AR_SECONDARY);

    query.addSum(SqlUtils.sqlIf(condition, 0, SqlUtils.field(incomeAlias, AR_INCOME)),
        AR_SECONDARY_INCOME);
    query.addSum(SqlUtils.sqlIf(condition, 0, SqlUtils.field(expenseAlias, AR_EXPENSE)),
        AR_SECONDARY_EXPENSE);

    SimpleRowSet result = qs.getData(query);
    qs.sqlDropTemp(tmp);

    if (DataUtils.isEmpty(result)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result);
    }
  }

  private ResponseObject getCargoUsage(String viewName, List<Long> ids, String saleColumn) {
    String source = sys.getViewSource(viewName);
    SqlSelect ss;
    IsExpression ref = null;

    if (!BeeUtils.isEmpty(saleColumn)) {
      ss = new SqlSelect()
          .addFrom(TBL_CARGO_INCOMES)
          .setWhere(SqlUtils.notNull(TBL_CARGO_INCOMES, saleColumn));

      switch (source) {
        case TBL_ORDER_CARGO:
          ref = SqlUtils.field(TBL_CARGO_INCOMES, COL_CARGO);
          break;
        case TBL_ORDERS:
          ss.addFromInner(TBL_ORDER_CARGO,
              sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO));
          ref = SqlUtils.field(TBL_ORDER_CARGO, COL_ORDER);
          break;
      }
    } else {
      ss = new SqlSelect().addFrom(TBL_CARGO_TRIPS);

      switch (source) {
        case TBL_TRIPS:
          ref = SqlUtils.field(TBL_CARGO_TRIPS, COL_TRIP);
          break;
        case TBL_ORDER_CARGO:
          ref = SqlUtils.field(TBL_CARGO_TRIPS, COL_CARGO);
          break;
        case TBL_ORDERS:
          ss.addFromInner(TBL_ORDER_CARGO,
              sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO));
          ref = SqlUtils.field(TBL_ORDER_CARGO, COL_ORDER);
          break;
      }
    }
    if (Objects.isNull(ref)) {
      return ResponseObject.error("Table not supported:", source);
    }
    return ResponseObject.response(qs.sqlCount(ss.setWhere(SqlUtils.and(SqlUtils.inList(ref, ids),
        ss.getWhere()))));
  }

  private Map<String, String> getCityNames() {
    Map<String, String> result = new HashMap<>();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_CITIES, sys.getIdName(TBL_CITIES), COL_CITY_NAME)
        .addFrom(TBL_CITIES);

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        result.put(row.getValue(0), row.getValue(1));
      }
    }

    return result;
  }

  private ResponseObject getColors(RequestInfo reqInfo) {
    Long theme;
    if (reqInfo.hasParameter(Service.VAR_ID)) {
      theme = BeeUtils.toLong(reqInfo.getParameter(Service.VAR_ID));
    } else {
      theme = null;
    }

    return ResponseObject.response(getThemeColors(theme));
  }

  @SuppressWarnings("unchecked")
  private ResponseObject getCreditInfo(RequestInfo reqInfo) {
    Long company = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_COMPANY));

    if (!DataUtils.isId(company)) {
      Long incomeId = BeeUtils.toLongOrNull(reqInfo.getParameter(TBL_CARGO_INCOMES));

      if (!DataUtils.isId(incomeId)) {
        return ResponseObject.emptyResponse();
      }
      SimpleRow row = qs.getRow(new SqlSelect()
          .addFields(TBL_ORDERS, COL_CUSTOMER, COL_PAYER)
          .addFrom(TBL_CARGO_INCOMES)
          .addFromInner(TBL_ORDER_CARGO,
              sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
          .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
          .setWhere(sys.idEquals(TBL_CARGO_INCOMES, incomeId)));

      if (row != null) {
        company = BeeUtils.nvl(row.getLong(COL_PAYER), row.getLong(COL_CUSTOMER));
      }
    }
    Map<String, Object> resp = new HashMap<>();

    if (DataUtils.isId(company)) {
      ResponseObject response = trd.getCreditInfo(company);

      if (response.hasErrors()) {
        return response;
      }
      resp.putAll((Map<? extends String, ?>) response.getResponse());
      Long curr = (Long) resp.get(COL_COMPANY_LIMIT_CURRENCY);

      SqlSelect query = new SqlSelect()
          .addFrom(TBL_ORDERS)
          .addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
          .addFromInner(TBL_CARGO_INCOMES,
              sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
          .setWhere(SqlUtils.and(SqlUtils.or(SqlUtils.equals(TBL_ORDERS, COL_PAYER, company),
              SqlUtils.and(SqlUtils.isNull(TBL_ORDERS, COL_PAYER),
                  SqlUtils.equals(TBL_ORDERS, COL_CUSTOMER, company))),
              SqlUtils.isNull(TBL_CARGO_INCOMES, COL_SALE)));

      IsExpression cargoIncome;
      IsExpression dateExpr = SqlUtils.nvl(SqlUtils.field(TBL_CARGO_INCOMES, COL_DATE),
          SqlUtils.field(TBL_ORDERS, COL_DATE));

      if (DataUtils.isId(curr)) {
        cargoIncome = ExchangeUtils.exchangeFieldTo(query,
            TradeModuleBean.getTotalExpression(TBL_CARGO_INCOMES,
                SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT)),
            SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY), dateExpr, SqlUtils.constant(curr));
      } else {
        cargoIncome = ExchangeUtils.exchangeField(query,
            TradeModuleBean.getTotalExpression(TBL_CARGO_INCOMES,
                SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT)),
            SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY), dateExpr);
      }
      resp.put(VAR_INCOME, BeeUtils.round(qs.getValue(query.addSum(cargoIncome, VAR_INCOME)), 2));
    }
    return ResponseObject.response(resp);
  }

  private ResponseObject getDriverBusyDates(Long driver, Long from, Long to) {
    SqlSelect query = new SqlSelect()
        .addField(TBL_TRIPS, COL_TRIP_NO, COL_NOTES)
        .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_FROM),
            SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE)), COL_ABSENCE_FROM)
        .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_TO),
            SqlUtils.field(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE)), COL_ABSENCE_TO)
        .addFrom(TBL_TRIPS)
        .addFromInner(TBL_TRIP_DRIVERS,
            SqlUtils.and(sys.joinTables(TBL_TRIPS, TBL_TRIP_DRIVERS, COL_TRIP),
                SqlUtils.equals(TBL_TRIP_DRIVERS, COL_DRIVER, driver)))
        .setWhere(SqlUtils.or(
            SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_TRIP_DATE_TO),
                SqlUtils.isNull(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE)),
            SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_TRIP_DATE_TO),
                SqlUtils.more(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE, from)),
            SqlUtils.and(SqlUtils.notNull(TBL_TRIPS, COL_TRIP_DATE_TO),
                SqlUtils.more(TBL_TRIPS, COL_TRIP_DATE_TO, from))))
        .addOrder(null, COL_ABSENCE_FROM, COL_ABSENCE_TO);

    if (Objects.nonNull(to)) {
      query.setWhere(SqlUtils.and(query.getWhere(),
          SqlUtils.or(SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_TRIP_DATE_FROM),
              SqlUtils.less(TBL_TRIPS, COL_TRIP_DATE, to)),
              SqlUtils.and(SqlUtils.notNull(TBL_TRIPS, COL_TRIP_DATE_FROM),
                  SqlUtils.less(TBL_TRIPS, COL_TRIP_DATE_FROM, to)))));
    }
    query.addUnion(new SqlSelect()
        .addExpr(SqlUtils.concat(SqlUtils.field(TBL_ABSENCE_TYPES, COL_ABSENCE_NAME), "' '",
            SqlUtils.nvl(SqlUtils.field(TBL_DRIVER_ABSENCE, COL_ABSENCE_NOTES), "''")), COL_NOTES)
        .addFields(TBL_DRIVER_ABSENCE, COL_ABSENCE_FROM, COL_ABSENCE_TO)
        .addFrom(TBL_DRIVER_ABSENCE)
        .addFromInner(TBL_ABSENCE_TYPES,
            sys.joinTables(TBL_ABSENCE_TYPES, TBL_DRIVER_ABSENCE, COL_ABSENCE))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_DRIVER_ABSENCE, COL_DRIVER, driver),
            SqlUtils.more(TBL_DRIVER_ABSENCE, COL_ABSENCE_TO, from),
            Objects.nonNull(to) ? SqlUtils.less(TBL_DRIVER_ABSENCE, COL_ABSENCE_FROM, to) : null)));

    List<String> messages = new ArrayList<>();
    Dictionary loc = usr.getDictionary();

    for (SimpleRow row : qs.getData(query)) {
      messages.add(BeeUtils.joinWords(loc.dateFromShort(), row.getDate(COL_ABSENCE_FROM),
          Objects.isNull(row.getDate(COL_ABSENCE_TO))
              ? null : loc.dateToShort() + " " + row.getDate(COL_ABSENCE_TO),
          row.getValue(COL_NOTES)));
    }

    return ResponseObject.response(messages);
  }

  private Multimap<Long, Long> getDriverGroups(IsCondition condition) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_DRIVER_GROUPS, COL_DRIVER, COL_GROUP)
        .addFrom(TBL_DRIVER_GROUPS)
        .setWhere(condition);

    SimpleRowSet data = qs.getData(query);

    Multimap<Long, Long> result = HashMultimap.create();
    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        result.put(row.getLong(COL_DRIVER), row.getLong(COL_GROUP));
      }
    }

    return result;
  }

  private ResponseObject getDtbData() {
    BeeRowSet settings = getSettings();
    if (DataUtils.isEmpty(settings)) {
      return ResponseObject.error("user settings not available");
    }

    JustDate minDate = settings.getDate(0, COL_DTB_MIN_DATE);
    JustDate maxDate = settings.getDate(0, COL_DTB_MAX_DATE);

    String filterGroups = settings.getString(0, COL_DTB_TRANSPORT_GROUPS);
    Boolean completedTrips = settings.getBoolean(0, COL_DTB_COMPLETED_TRIPS);

    Range<Value> period = TransportUtils.getChartPeriod(minDate, maxDate);

    List<Color> colors = getThemeColors(null);
    settings.setTableProperty(PROP_COLORS, Codec.beeSerialize(colors));

    BeeRowSet transportGroups = qs.getViewData(VIEW_TRANSPORT_GROUPS);
    if (!DataUtils.isEmpty(transportGroups)) {
      settings.setTableProperty(PROP_TRANSPORT_GROUPS, transportGroups.serialize());
    }

    BeeRowSet cargoTypes = qs.getViewData(VIEW_CARGO_TYPES);
    if (!DataUtils.isEmpty(cargoTypes)) {
      settings.setTableProperty(PROP_CARGO_TYPES, cargoTypes.serialize());
    }

    BeeRowSet countries = qs.getViewData(VIEW_COUNTRIES);
    settings.setTableProperty(PROP_COUNTRIES, countries.serialize());

    Map<String, String> cityNames = getCityNames();
    if (!BeeUtils.isEmpty(cityNames)) {
      settings.setTableProperty(PROP_CITIES, Codec.beeSerialize(cityNames));
    }

    Filter driverFilter = null;
    if (!BeeUtils.isEmpty(filterGroups) && !DataUtils.isEmpty(transportGroups)) {
      Set<Long> groups = DataUtils.parseIdSet(filterGroups);
      groups.retainAll(transportGroups.getRowIds());

      if (!groups.isEmpty()) {
        Set<Long> ids = qs.getDistinctLongs(TBL_DRIVER_GROUPS, COL_DRIVER,
            SqlUtils.inList(TBL_DRIVER_GROUPS, COL_GROUP, groups));

        if (BeeUtils.isEmpty(ids)) {
          driverFilter = Filter.isFalse();
        } else {
          driverFilter = Filter.idIn(ids);
        }
      }
    }

    BeeRowSet drivers = qs.getViewData(VIEW_DRIVERS, driverFilter);
    if (DataUtils.isEmpty(drivers)) {
      logger.warning(SVC_GET_DTB_DATA, filterGroups, "drivers not available");
      return ResponseObject.response(settings);
    }

    List<Long> driverIds = DataUtils.getRowIds(drivers);

    IsCondition groupWhere = SqlUtils.inList(TBL_DRIVER_GROUPS, COL_DRIVER, driverIds);
    Multimap<Long, Long> driverGroups = getDriverGroups(groupWhere);
    if (!driverGroups.isEmpty()) {
      for (BeeRow row : drivers) {
        if (driverGroups.containsKey(row.getId())) {
          row.setProperty(PROP_DRIVER_GROUPS,
              DataUtils.buildIdList(driverGroups.get(row.getId())));
        }
      }
    }

    settings.setTableProperty(PROP_DRIVERS, drivers.serialize());

    BeeRowSet absence = qs.getViewData(VIEW_DRIVER_ABSENCE,
        Filter.and(Filter.any(COL_DRIVER, driverIds),
            Filter.anyIntersects(Lists.newArrayList(COL_ABSENCE_FROM, COL_ABSENCE_TO), period)));
    if (!DataUtils.isEmpty(absence)) {
      settings.setTableProperty(PROP_ABSENCE, absence.serialize());
    }

    IsCondition tripDriverWhere = SqlUtils.inList(TBL_TRIP_DRIVERS, COL_DRIVER, driverIds);
    IsCondition chartTripCondition = getChartTripCondition(period, completedTrips);

    SimpleRowSet tripDrivers = getTripDrivers(SqlUtils.and(tripDriverWhere, chartTripCondition));
    if (DataUtils.isEmpty(tripDrivers)) {
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_TRIP_DRIVERS, tripDrivers.serialize());

    IsCondition tripWhere = SqlUtils.and(chartTripCondition,
        SqlUtils.in(TBL_TRIPS, sys.getIdName(TBL_TRIPS),
            TBL_TRIP_DRIVERS, COL_TRIP, tripDriverWhere));

    SqlSelect tripQuery = getTripQuery(tripWhere);
    tripQuery.addOrder(TBL_TRIPS, COL_TRIP_DATE);

    SimpleRowSet trips = qs.getData(tripQuery);
    if (DataUtils.isEmpty(trips)) {
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_TRIPS, trips.serialize());

    SqlSelect freightQuery = getFreightQuery(tripWhere);

    SimpleRowSet freights = qs.getData(freightQuery);
    if (DataUtils.isEmpty(freights)) {
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_FREIGHTS, freights.serialize());

    SimpleRowSet cargoHandling = getFreightHandlingData(tripWhere, true);

    if (!DataUtils.isEmpty(cargoHandling)) {
      settings.setTableProperty(PROP_CARGO_HANDLING, cargoHandling.serialize());
    }

    return ResponseObject.response(settings);
  }

  private Table<Long, String, String> getExtremes(IsCondition clause, String keyColumn) {
    Table<Long, String, String> data = HashBasedTable.create();
    Table<Long, String, SimpleRowSet> handle = HashBasedTable.create();

    String als = "tmpSubQuery";

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addFields(als, keyColumn, VAR_UNLOADING, VAR_PARAMETER_DEFAULT)
        .addAllFields(TBL_CARGO_PLACES)
        .addField(TBL_CITIES, COL_CITY_NAME, ALS_CITY_NAME)
        .addField(TBL_COUNTRIES, COL_COUNTRY_NAME, ALS_COUNTRY_NAME)
        .addField(TBL_COUNTRIES, COL_COUNTRY_CODE, COL_COUNTRY + COL_COUNTRY_CODE)
        .addFrom(TBL_CARGO_PLACES)
        .addFromLeft(TBL_CITIES, sys.joinTables(TBL_CITIES, TBL_CARGO_PLACES, COL_PLACE_CITY))
        .addFromLeft(TBL_COUNTRIES,
            sys.joinTables(TBL_COUNTRIES, TBL_CARGO_PLACES, COL_PLACE_COUNTRY))
        .addFromInner(getHandlingQuery(clause, Objects.equals(keyColumn, COL_CARGO_TRIP)), als,
            SqlUtils.joinUsing(TBL_CARGO_PLACES, als, sys.getIdName(TBL_CARGO_PLACES)))
        .addOrder(TBL_CARGO_PLACES, COL_PLACE_DATE));

    String[] calc = new String[] {COL_LOADED_KILOMETERS, COL_EMPTY_KILOMETERS};
    Multimap<Long, Long> defaults = HashMultimap.create();

    for (SimpleRow row : rs) {
      String prfx;
      int cmpr;

      if (BeeUtils.unbox(row.getBoolean(VAR_UNLOADING))) {
        prfx = VAR_UNLOADING;
        cmpr = BeeConst.COMPARE_LESS;
      } else {
        prfx = VAR_LOADING;
        cmpr = BeeConst.COMPARE_MORE;
      }
      Long key = row.getLong(keyColumn);

      if (!BeeUtils.unbox(row.getBoolean(VAR_PARAMETER_DEFAULT))
          || defaults.put(key, row.getLong(sys.getIdName(TBL_CARGO_PLACES)))) {
        Arrays.stream(calc).forEach(col -> data.put(key, col,
            BeeUtils.toString(BeeUtils.toDouble(data.get(key, col))
                + BeeUtils.unbox(row.getDouble(col)))));
      }
      if (!data.contains(key, prfx + COL_PLACE_DATE)
          || TimeUtils.toDateTimeOrNull(data.get(key, prfx + COL_PLACE_DATE))
          .compareTo(row.getDateTime(COL_PLACE_DATE)) == cmpr) {

        Arrays.stream(rs.getColumnNames())
            .filter(col -> !BeeUtils.inList(col, keyColumn, VAR_UNLOADING)
                && !ArrayUtils.contains(calc, col))
            .forEach(col -> data.put(key, prfx + col, BeeUtils.nvl(row.getValue(col), "")));
      }
      if (!handle.contains(key, prfx)) {
        handle.put(key, prfx, new SimpleRowSet(row.getColumnNames()));
      }

      handle.get(key, prfx).addRow(row.getValues());
    }
    handle.rowKeySet().forEach(key ->
        handle.row(key).forEach((prfx, h) ->
            data.put(key, prfx, h.serialize())));

    return data;
  }

  private SimpleRowSet getFreightHandlingData(IsCondition clause, boolean includeRevising) {
    String als = "tmpSubQuery";

    return qs.getData(new SqlSelect()
        .addFields(als, COL_CARGO, VAR_UNLOADING)
        .addFields(TBL_CARGO_PLACES, COL_PLACE_DATE, COL_PLACE_COUNTRY, COL_PLACE_CITY,
            COL_PLACE_ADDRESS, COL_PLACE_POST_INDEX, COL_PLACE_NUMBER)
        .addFrom(TBL_CARGO_PLACES)
        .addFromInner(getHandlingQuery(clause, includeRevising), als,
            SqlUtils.joinUsing(TBL_CARGO_PLACES, als, sys.getIdName(TBL_CARGO_PLACES)))
        .addOrder(TBL_CARGO_PLACES, COL_PLACE_ORDINAL, COL_PLACE_DATE));
  }

  private SqlSelect getFreightQuery(IsCondition where) {
    return new SqlSelect()
        .addFields(TBL_TRIPS, COL_TRIP_ID, COL_VEHICLE)
        .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_CARGO_TRIPS, COL_TRAILER),
            SqlUtils.field(TBL_TRIPS, COL_TRAILER)), COL_TRAILER)
        .addFields(TBL_CARGO_TRIPS, COL_CARGO, COL_CARGO_TRIP_ID)
        .addField(TBL_CARGO_TRIPS, sys.getVersionName(TBL_CARGO_TRIPS), ALS_CARGO_TRIP_VERSION)
        .addFields(TBL_ORDER_CARGO, COL_ORDER, COL_CARGO_TYPE, COL_CARGO_DESCRIPTION,
            COL_CARGO_NOTES)
        .addFields(TBL_ORDERS, COL_ORDER_NO, COL_CUSTOMER, COL_ORDER_MANAGER, COL_STATUS)
        .addField(TBL_ORDERS, COL_ORDER_DATE, ALS_ORDER_DATE)
        .addField(TBL_COMPANIES, COL_COMPANY_NAME, COL_CUSTOMER_NAME)
        .addFrom(TBL_TRIPS)
        .addFromInner(TBL_CARGO_TRIPS,
            SqlUtils.join(TBL_CARGO_TRIPS, COL_TRIP, TBL_TRIPS, COL_TRIP_ID))
        .addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, TBL_ORDERS, COL_CUSTOMER))
        .setWhere(where)
        .addOrder(TBL_TRIPS, COL_TRIP_ID);
  }

  private ResponseObject getFxData() {
    BeeRowSet settings = getSettings();
    if (settings == null) {
      return ResponseObject.error("user settings not available");
    }

    Long theme = settings.getLong(0, settings.getColumnIndex(COL_FX_THEME));
    List<Color> colors = getThemeColors(theme);
    settings.setTableProperty(PROP_COLORS, Codec.beeSerialize(colors));

    BeeRowSet cargoTypes = qs.getViewData(VIEW_CARGO_TYPES);
    if (!DataUtils.isEmpty(cargoTypes)) {
      settings.setTableProperty(PROP_CARGO_TYPES, cargoTypes.serialize());
    }

    BeeRowSet countries = qs.getViewData(VIEW_COUNTRIES);
    settings.setTableProperty(PROP_COUNTRIES, countries.serialize());

    Map<String, String> cityNames = getCityNames();
    if (!BeeUtils.isEmpty(cityNames)) {
      settings.setTableProperty(PROP_CITIES, Codec.beeSerialize(cityNames));
    }
    IsCondition cargoWhere = SqlUtils.and(SqlUtils.isNull(TBL_CARGO_TRIPS, COL_CARGO),
        SqlUtils.inList(TBL_ORDERS, COL_STATUS, OrderStatus.REQUEST, OrderStatus.ACTIVE));

    SqlSelect query = new SqlSelect()
        .addFields(TBL_ORDERS, COL_STATUS, COL_ORDER_DATE, COL_ORDER_NO, COL_CUSTOMER,
            COL_ORDER_MANAGER)
        .addField(TBL_COMPANIES, COL_COMPANY_NAME, COL_CUSTOMER_NAME)
        .addFields(TBL_ORDER_CARGO, COL_ORDER, COL_CARGO_ID, COL_CARGO_TYPE, COL_CARGO_DESCRIPTION,
            COL_CARGO_NOTES)
        .addFrom(TBL_ORDER_CARGO)
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, TBL_ORDERS, COL_CUSTOMER))
        .addFromLeft(TBL_CARGO_TRIPS, sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
        .setWhere(cargoWhere)
        .addOrder(TBL_COMPANIES, COL_COMPANY_NAME)
        .addOrder(TBL_ORDERS, COL_ORDER_DATE, COL_ORDER_NO);

    SimpleRowSet data = qs.getData(query);

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_ORDER_CARGO, data.serialize());

    SimpleRowSet cargoHandling = getFreightHandlingData(cargoWhere, false);

    if (!DataUtils.isEmpty(cargoHandling)) {
      settings.setTableProperty(PROP_CARGO_HANDLING, cargoHandling.serialize());
    }
    return ResponseObject.response(settings);
  }

  private SqlSelect getHandlingQuery(IsCondition clause, boolean includeRevising) {
    SqlSelect query = new SqlSelect()
        .addField(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO), COL_CARGO)
        .addField(TBL_CARGO_TRIPS, sys.getIdName(TBL_CARGO_TRIPS), COL_CARGO_TRIP)
        .addFields(TBL_CARGO_PLACES, sys.getIdName(TBL_CARGO_PLACES))
        .addFrom(TBL_ORDER_CARGO)
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_CARGO_TRIPS, sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
        .addFromLeft(TBL_TRIPS, sys.joinTables(TBL_TRIPS, TBL_CARGO_TRIPS, COL_TRIP))
        .setWhere(SqlUtils.and(clause, SqlUtils.notNull(TBL_CARGO_PLACES, COL_PLACE_DATE)));

    SqlSelect loading = query.copyOf()
        .addConstant(0, VAR_UNLOADING)
        .addConstant(1, VAR_PARAMETER_DEFAULT)
        .addFromInner(TBL_CARGO_LOADING,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_LOADING, COL_CARGO))
        .addFromInner(TBL_CARGO_PLACES,
            sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_LOADING, COL_LOADING_PLACE));

    SqlSelect unloading = query.copyOf()
        .addConstant(1, VAR_UNLOADING)
        .addConstant(1, VAR_PARAMETER_DEFAULT)
        .addFromInner(TBL_CARGO_UNLOADING,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_UNLOADING, COL_CARGO))
        .addFromInner(TBL_CARGO_PLACES,
            sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_UNLOADING, COL_UNLOADING_PLACE));

    loading.addUnion(unloading);

    if (includeRevising) {
      String als = "alsHandling";
      IsCondition wh = SqlUtils.and(query.getWhere(), SqlUtils.isNull(als, COL_CARGO_TRIP));

      loading.addFromLeft(TBL_CARGO_LOADING, als,
          sys.joinTables(TBL_CARGO_TRIPS, als, COL_CARGO_TRIP))
          .setWhere(wh);

      unloading.addFromLeft(TBL_CARGO_UNLOADING, als,
          sys.joinTables(TBL_CARGO_TRIPS, als, COL_CARGO_TRIP))
          .setWhere(wh);

      loading.addUnion(query.copyOf()
          .addConstant(0, VAR_UNLOADING)
          .addConstant(0, VAR_PARAMETER_DEFAULT)
          .addFromInner(TBL_CARGO_LOADING,
              sys.joinTables(TBL_CARGO_TRIPS, TBL_CARGO_LOADING, COL_CARGO_TRIP))
          .addFromInner(TBL_CARGO_PLACES,
              sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_LOADING, COL_LOADING_PLACE)))

          .addUnion(query.copyOf()
              .addConstant(1, VAR_UNLOADING)
              .addConstant(0, VAR_PARAMETER_DEFAULT)
              .addFromInner(TBL_CARGO_UNLOADING,
                  sys.joinTables(TBL_CARGO_TRIPS, TBL_CARGO_UNLOADING, COL_CARGO_TRIP))
              .addFromInner(TBL_CARGO_PLACES,
                  sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_UNLOADING, COL_UNLOADING_PLACE)));
    }
    return loading;
  }

  private BeeRowSet getSettings() {
    long userId = usr.getCurrentUserId();
    Filter filter = Filter.equals(COL_USER, userId);

    BeeRowSet rowSet = qs.getViewData(VIEW_TRANSPORT_SETTINGS, filter);
    if (!DataUtils.isEmpty(rowSet)) {
      return rowSet;
    }

    SqlInsert sqlInsert = new SqlInsert(TBL_TRANSPORT_SETTINGS)
        .addConstant(COL_USER, userId);

    ResponseObject response = qs.insertDataWithResponse(sqlInsert);
    if (response.hasErrors()) {
      return null;
    }

    return qs.getViewData(VIEW_TRANSPORT_SETTINGS, filter);
  }

  private ResponseObject getTextConstant(RequestInfo reqInfo) {

    Integer textConstant = Assert.notNull(reqInfo.getParameterInt(COL_TEXT_CONSTANT));
    Integer userLocale = reqInfo.getParameterInt(COL_USER_LOCALE);
    TextConstant constant = EnumUtils.getEnumByIndex(TextConstant.class, textConstant);

    return ResponseObject.response(getTextConstant(constant, userLocale));
  }

  public String getTextConstant(TextConstant constant, Integer userLocale) {
    BeeRowSet rowSet = qs.getViewData(VIEW_TEXT_CONSTANTS,
        Filter.equals(COL_TEXT_CONSTANT, constant));

    SupportedLocale locale = EnumUtils.getEnumByIndex(SupportedLocale.class, userLocale);
    String localizedContent = Localized.column(COL_TEXT_CONTENT, locale.getLanguage());
    String text;

    if (DataUtils.isEmpty(rowSet)) {
      text = constant.getDefaultContent(Localizations.getDictionary(
          SupportedLocale.parse(locale.getLanguage())));
    } else if (BeeConst.isUndef(DataUtils.getColumnIndex(localizedContent, rowSet.getColumns()))) {
      text = rowSet.getString(0, COL_TEXT_CONTENT);
    } else {
      text = BeeUtils.notEmpty(rowSet.getString(0, localizedContent),
          rowSet.getString(0, COL_TEXT_CONTENT));
    }
    return text;
  }

  private List<Color> getThemeColors(Long theme) {
    List<Color> result = new ArrayList<>();

    BeeRowSet rowSet;
    if (theme != null) {
      rowSet = qs.getViewData(VIEW_THEME_COLORS, Filter.equals(COL_THEME, theme));
    } else {
      rowSet = null;
    }

    if (DataUtils.isEmpty(rowSet)) {
      rowSet = qs.getViewData(VIEW_COLORS);
      if (DataUtils.isEmpty(rowSet)) {
        return result;
      }
    }

    int bgIndex = rowSet.getColumnIndex(COL_BACKGROUND);
    int fgIndex = rowSet.getColumnIndex(COL_FOREGROUND);

    for (BeeRow row : rowSet.getRows()) {
      String bg = row.getString(bgIndex);
      String fg = row.getString(fgIndex);

      if (!BeeUtils.isEmpty(bg)) {
        result.add(new Color(row.getId(), bg.trim(), BeeUtils.trim(fg)));
      }
    }
    return result;
  }

  private ResponseObject getTripBeforeData(long vehicle, Long date) {
    Pair<String, String> pair = Pair.empty();

    if (Objects.nonNull(date)) {
      SimpleRow row = qs.getRow(new SqlSelect()
          .addField(TBL_TRIPS, sys.getIdName(TBL_TRIPS), COL_TRIP)
          .addFields(TBL_TRIPS, COL_SPEEDOMETER_BEFORE, COL_SPEEDOMETER_AFTER, COL_FUEL_BEFORE,
              COL_FUEL_AFTER)
          .addFields(TBL_VEHICLES, COL_SPEEDOMETER)
          .addFrom(TBL_TRIPS)
          .addFromInner(TBL_VEHICLES,
              SqlUtils.and(sys.joinTables(TBL_VEHICLES, TBL_TRIPS, COL_VEHICLE),
                  sys.idEquals(TBL_VEHICLES, vehicle)))
          .setWhere(SqlUtils.less(TBL_TRIPS, COL_DATE, date))
          .addOrderDesc(TBL_TRIPS, COL_DATE)
          .setLimit(1));

      if (Objects.nonNull(row)) {
        Long tripId = row.getLong(COL_TRIP);
        Double speedometer = row.getDouble(COL_SPEEDOMETER_AFTER);
        Double fuel = row.getDouble(COL_FUEL_AFTER);

        if (Objects.isNull(speedometer)) {
          Double km = qs.getDouble(new SqlSelect()
              .addSum(TBL_TRIP_ROUTES, COL_ROUTE_KILOMETERS)
              .addFrom(TBL_TRIP_ROUTES)
              .setWhere(SqlUtils.equals(TBL_TRIP_ROUTES, COL_TRIP, tripId)));

          speedometer = BeeUtils.unbox(row.getDouble(COL_SPEEDOMETER_BEFORE)) + BeeUtils.unbox(km);
          Integer scale = row.getInt(COL_SPEEDOMETER);

          if (BeeUtils.isPositive(scale) && scale < speedometer) {
            speedometer -= scale;
          }
        }
        if (Objects.isNull(fuel)) {
          Double fill = qs.getDouble(new SqlSelect()
              .addSum(TBL_TRIP_FUEL_COSTS, COL_COSTS_QUANTITY)
              .addFrom(TBL_TRIP_FUEL_COSTS)
              .setWhere(SqlUtils.equals(TBL_TRIP_FUEL_COSTS, COL_TRIP, tripId)));

          SimpleRow cons = qs.getRow(rep.getFuelConsumptionsQuery(new SqlSelect()
              .addFields(TBL_TRIP_ROUTES, sys.getIdName(TBL_TRIP_ROUTES))
              .addFrom(TBL_TRIP_ROUTES)
              .setWhere(SqlUtils.equals(TBL_TRIP_ROUTES, COL_TRIP, tripId)), false));

          Double consume = Objects.isNull(cons) ? null : cons.getDouble(COL_ROUTE_CONSUMPTION);

          Double addit = qs.getDouble(new SqlSelect()
              .addSum(TBL_TRIP_FUEL_CONSUMPTIONS, COL_COSTS_QUANTITY)
              .addFrom(TBL_TRIP_FUEL_CONSUMPTIONS)
              .setWhere(SqlUtils.equals(TBL_TRIP_FUEL_CONSUMPTIONS, COL_TRIP, tripId)));

          fuel = BeeUtils.unbox(row.getDouble(COL_FUEL_BEFORE)) + BeeUtils.unbox(fill)
              - BeeUtils.unbox(consume) - BeeUtils.unbox(addit);
        }
        pair.setA(BeeUtils.toString(speedometer));
        pair.setB(BeeUtils.toString(fuel));
      }
    }
    return ResponseObject.response(pair);
  }

  private SimpleRowSet getTripDrivers(IsCondition condition) {
    SqlSelect query = new SqlSelect()
        .addFrom(TBL_TRIP_DRIVERS)
        .addFromLeft(TBL_TRIPS, sys.joinTables(TBL_TRIPS, TBL_TRIP_DRIVERS, COL_TRIP))
        .addFromLeft(TBL_DRIVERS, sys.joinTables(TBL_DRIVERS, TBL_TRIP_DRIVERS, COL_DRIVER))
        .addFromLeft(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_DRIVERS, COL_DRIVER_PERSON))
        .addFromLeft(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON));

    query.addFields(TBL_TRIP_DRIVERS, COL_TRIP, COL_DRIVER,
        COL_TRIP_DRIVER_FROM, COL_TRIP_DRIVER_TO, COL_TRIP_DRIVER_NOTE);
    query.addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME);

    if (condition != null) {
      query.setWhere(condition);
    }

    return qs.getData(query);
  }

  private SqlSelect getTripQuery(IsCondition where) {
    String truckJoinAlias = "truck_" + SqlUtils.uniqueName();
    String trailerJoinAlias = "trail_" + SqlUtils.uniqueName();

    return new SqlSelect().addFrom(TBL_TRIPS)
        .addFromLeft(TBL_VEHICLES, truckJoinAlias,
            SqlUtils.join(truckJoinAlias, COL_VEHICLE_ID, TBL_TRIPS, COL_VEHICLE))
        .addFromLeft(TBL_VEHICLES, trailerJoinAlias,
            SqlUtils.join(trailerJoinAlias, COL_VEHICLE_ID, TBL_TRIPS, COL_TRAILER))
        .addFields(TBL_TRIPS, COL_TRIP_ID, COL_TRIP_NO, COL_VEHICLE, COL_TRAILER,
            COL_TRIP_DATE, COL_TRIP_PLANNED_END_DATE, COL_TRIP_DATE_FROM, COL_TRIP_DATE_TO,
            COL_TRIP_STATUS, COL_TRIP_NOTES)
        .addField(TBL_TRIPS, sys.getVersionName(TBL_TRIPS), ALS_TRIP_VERSION)
        .addField(truckJoinAlias, COL_NUMBER, ALS_VEHICLE_NUMBER)
        .addField(trailerJoinAlias, COL_NUMBER, ALS_TRAILER_NUMBER)
        .setWhere(where);
  }

  private ResponseObject getUnassignedCargos(RequestInfo reqInfo) {
    long orderId = BeeUtils.toLong(reqInfo.getParameter(COL_ORDER));

    SqlSelect query = new SqlSelect()
        .addField(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO), COL_CARGO)
        .addFrom(TBL_ORDER_CARGO)
        .addFromLeft(TBL_CARGO_TRIPS, sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_ORDER_CARGO, COL_ORDER, orderId),
            SqlUtils.isNull(TBL_CARGO_TRIPS, COL_CARGO)));

    return ResponseObject.response(qs.getColumn(query));
  }

  private ResponseObject getVehicleBusyDates(Long vehicle, Long trailer, Long from, Long to) {
    SqlSelect select = null;

    for (Pair<String, Long> pair : Arrays.asList(Pair.of(COL_VEHICLE, vehicle),
        Pair.of(COL_TRAILER, trailer))) {

      if (DataUtils.isId(pair.getB())) {
        SqlSelect query = new SqlSelect()
            .addFields(TBL_VEHICLES, COL_VEHICLE_NUMBER)
            .addField(TBL_TRIPS, COL_TRIP_NO, COL_NOTES)
            .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_FROM),
                SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE)), COL_ABSENCE_FROM)
            .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_TO),
                SqlUtils.field(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE)), COL_ABSENCE_TO)
            .addFrom(TBL_TRIPS)
            .addFromInner(TBL_VEHICLES,
                SqlUtils.and(sys.joinTables(TBL_VEHICLES, TBL_TRIPS, pair.getA()),
                    sys.idEquals(TBL_VEHICLES, pair.getB())))
            .setWhere(SqlUtils.or(
                SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_TRIP_DATE_TO),
                    SqlUtils.isNull(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE)),
                SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_TRIP_DATE_TO),
                    SqlUtils.more(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE, from)),
                SqlUtils.and(SqlUtils.notNull(TBL_TRIPS, COL_TRIP_DATE_TO),
                    SqlUtils.more(TBL_TRIPS, COL_TRIP_DATE_TO, from))));

        if (Objects.nonNull(to)) {
          query.setWhere(SqlUtils.and(query.getWhere(),
              SqlUtils.or(SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_TRIP_DATE_FROM),
                  SqlUtils.less(TBL_TRIPS, COL_TRIP_DATE, to)),
                  SqlUtils.and(SqlUtils.notNull(TBL_TRIPS, COL_TRIP_DATE_FROM),
                      SqlUtils.less(TBL_TRIPS, COL_TRIP_DATE_FROM, to)))));
        }
        if (Objects.isNull(select)) {
          select = query.setUnionAllMode(false);
        } else {
          select.addUnion(query);
        }
        select.addUnion(new SqlSelect()
            .addFields(TBL_VEHICLES, COL_VEHICLE_NUMBER)
            .addExpr(SqlUtils
                .concat(SqlUtils.field(TBL_VEHICLE_SERVICE_TYPES, COL_VEHICLE_SERVICE_NAME), "' '",
                    SqlUtils.nvl(SqlUtils.field(TBL_VEHICLE_SERVICES, COL_VEHICLE_SERVICE_NOTES),
                        "''")), COL_NOTES)
            .addField(TBL_VEHICLE_SERVICES, COL_VEHICLE_SERVICE_DATE, COL_ABSENCE_FROM)
            .addField(TBL_VEHICLE_SERVICES, COL_VEHICLE_SERVICE_DATE_TO, COL_ABSENCE_TO)
            .addFrom(TBL_VEHICLE_SERVICES)
            .addFromInner(TBL_VEHICLES,
                SqlUtils.and(sys.joinTables(TBL_VEHICLES, TBL_VEHICLE_SERVICES, COL_VEHICLE),
                    sys.idEquals(TBL_VEHICLES, pair.getB())))
            .addFromInner(TBL_VEHICLE_SERVICE_TYPES,
                sys.joinTables(TBL_VEHICLE_SERVICE_TYPES, TBL_VEHICLE_SERVICES,
                    COL_VEHICLE_SERVICE_TYPE))
            .setWhere(SqlUtils.and(
                SqlUtils.or(SqlUtils.isNull(TBL_VEHICLE_SERVICES, COL_VEHICLE_SERVICE_DATE_TO),
                    SqlUtils.more(TBL_VEHICLE_SERVICES, COL_VEHICLE_SERVICE_DATE_TO, from)),
                Objects.nonNull(to)
                    ? SqlUtils.less(TBL_VEHICLE_SERVICES, COL_VEHICLE_SERVICE_DATE, to) : null)));
      }
    }
    List<String> messages = new ArrayList<>();
    Dictionary loc = usr.getDictionary();

    for (SimpleRow row : qs.getData(select
        .addOrder(null, COL_VEHICLE_NUMBER, COL_ABSENCE_FROM, COL_ABSENCE_TO))) {

      messages.add(BeeUtils.joinWords(row.getValue(COL_VEHICLE_NUMBER),
          loc.dateFromShort(), row.getDate(COL_ABSENCE_FROM),
          Objects.isNull(row.getDate(COL_ABSENCE_TO))
              ? null : loc.dateToShort() + " " + row.getDate(COL_ABSENCE_TO),
          row.getValue(COL_NOTES)));
    }
    return ResponseObject.response(messages);
  }

  private Multimap<Long, Long> getVehicleGroups(IsCondition condition) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_VEHICLE_GROUPS, COL_VEHICLE, COL_GROUP)
        .addFrom(TBL_VEHICLE_GROUPS)
        .setWhere(condition);

    SimpleRowSet data = qs.getData(query);

    Multimap<Long, Long> result = HashMultimap.create();
    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        result.put(row.getLong(COL_VEHICLE), row.getLong(COL_GROUP));
      }
    }

    return result;
  }

  private Map<Long, Long> getVehicleManagers(IsCondition condition) {
    SqlSelect query = new SqlSelect()
        .addFrom(TBL_VEHICLE_GROUPS)
        .addFromInner(TBL_TRANSPORT_GROUPS,
            sys.joinTables(TBL_TRANSPORT_GROUPS, TBL_VEHICLE_GROUPS, COL_GROUP))
        .addFields(TBL_VEHICLE_GROUPS, COL_VEHICLE)
        .addMax(TBL_TRANSPORT_GROUPS, COL_GROUP_MANAGER)
        .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_TRANSPORT_GROUPS, COL_GROUP_MANAGER),
            condition))
        .addGroup(TBL_VEHICLE_GROUPS, COL_VEHICLE)
        .setHaving(SqlUtils.equals(SqlUtils.aggregate(SqlFunction.COUNT_DISTINCT,
            SqlUtils.field(TBL_TRANSPORT_GROUPS, COL_GROUP_MANAGER)), 1));

    SimpleRowSet data = qs.getData(query);

    Map<Long, Long> result = new HashMap<>();
    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        result.put(row.getLong(COL_VEHICLE), row.getLong(COL_GROUP_MANAGER));
      }
    }

    return result;
  }

  private SimpleRowSet getVehicleServices(IsCondition condition) {
    SqlSelect query = new SqlSelect()
        .addFrom(TBL_VEHICLE_SERVICES)
        .addFromLeft(TBL_VEHICLES,
            sys.joinTables(TBL_VEHICLES, TBL_VEHICLE_SERVICES, COL_VEHICLE))
        .addFromLeft(TBL_VEHICLE_SERVICE_TYPES,
            sys.joinTables(TBL_VEHICLE_SERVICE_TYPES, TBL_VEHICLE_SERVICES,
                COL_VEHICLE_SERVICE_TYPE));

    query.addFields(TBL_VEHICLE_SERVICES, COL_VEHICLE, COL_VEHICLE_SERVICE_DATE,
        COL_VEHICLE_SERVICE_DATE_TO, COL_VEHICLE_SERVICE_NOTES);
    query.addFields(TBL_VEHICLES, COL_NUMBER);
    query.addFields(TBL_VEHICLE_SERVICE_TYPES, COL_VEHICLE_SERVICE_NAME);

    query.addOrder(TBL_VEHICLE_SERVICES, COL_VEHICLE, COL_VEHICLE_SERVICE_DATE);

    if (condition != null) {
      query.setWhere(condition);
    }

    return qs.getData(query);
  }

  private ResponseObject getVehicleTbData(String svc, Filter vehicleFilter,
      boolean filterVehiclesByTrip, VehicleType vehicleType, String themeColumnName,
      String minDateColumnName, String maxDateColumnName,
      String groupsColumnName, String completedTripsColumnName) {

    BeeRowSet settings = getSettings();
    if (settings == null) {
      return ResponseObject.error("user settings not available");
    }

    Long theme = settings.getLong(0, settings.getColumnIndex(themeColumnName));

    JustDate minDate = settings.getDate(0, minDateColumnName);
    JustDate maxDate = settings.getDate(0, maxDateColumnName);

    String filterGroups = settings.getString(0, groupsColumnName);
    Boolean completedTrips = settings.getBoolean(0, completedTripsColumnName);

    Range<Value> period = TransportUtils.getChartPeriod(minDate, maxDate);

    String tripVehicleIdColumnName = (vehicleType == null)
        ? COL_VEHICLE : vehicleType.getTripVehicleIdColumnName();

    List<Color> colors = getThemeColors(theme);
    settings.setTableProperty(PROP_COLORS, Codec.beeSerialize(colors));

    BeeRowSet transportGroups = qs.getViewData(VIEW_TRANSPORT_GROUPS);
    if (!DataUtils.isEmpty(transportGroups)) {
      settings.setTableProperty(PROP_TRANSPORT_GROUPS, transportGroups.serialize());
    }

    BeeRowSet cargoTypes = qs.getViewData(VIEW_CARGO_TYPES);
    if (!DataUtils.isEmpty(cargoTypes)) {
      settings.setTableProperty(PROP_CARGO_TYPES, cargoTypes.serialize());
    }

    BeeRowSet countries = qs.getViewData(VIEW_COUNTRIES);
    settings.setTableProperty(PROP_COUNTRIES, countries.serialize());

    Map<String, String> cityNames = getCityNames();
    if (!BeeUtils.isEmpty(cityNames)) {
      settings.setTableProperty(PROP_CITIES, Codec.beeSerialize(cityNames));
    }

    Filter vehicleTripFilter;
    if (filterVehiclesByTrip) {
      vehicleTripFilter = Filter.in(COL_VEHICLE_ID, VIEW_TRIPS, tripVehicleIdColumnName,
          getChartTripFilter(period, completedTrips));
    } else {
      vehicleTripFilter = null;
    }

    Filter vehicleGroupFilter = null;
    if (!BeeUtils.isEmpty(filterGroups) && !DataUtils.isEmpty(transportGroups)) {
      Set<Long> groups = DataUtils.parseIdSet(filterGroups);
      groups.retainAll(transportGroups.getRowIds());

      if (!groups.isEmpty()) {
        Set<Long> ids = qs.getDistinctLongs(TBL_VEHICLE_GROUPS, COL_VEHICLE,
            SqlUtils.inList(TBL_VEHICLE_GROUPS, COL_GROUP, groups));

        if (BeeUtils.isEmpty(ids)) {
          vehicleGroupFilter = Filter.isFalse();
        } else {
          vehicleGroupFilter = Filter.idIn(ids);
        }
      }
    }

    Order vehicleOrder = new Order(COL_NUMBER, true);

    BeeRowSet vehicles = qs.getViewData(VIEW_VEHICLES,
        Filter.and(vehicleFilter, vehicleTripFilter, vehicleGroupFilter), vehicleOrder);
    if (DataUtils.isEmpty(vehicles)) {
      logger.warning(svc, vehicleTripFilter, filterGroups, "vehicles not available");
      return ResponseObject.response(settings);
    }

    List<Long> vehicleIds = DataUtils.getRowIds(vehicles);
    IsCondition groupWhere = SqlUtils.inList(TBL_VEHICLE_GROUPS, COL_VEHICLE, vehicleIds);

    Multimap<Long, Long> vehicleGroups = getVehicleGroups(groupWhere);
    if (!vehicleGroups.isEmpty()) {
      for (BeeRow row : vehicles) {
        if (vehicleGroups.containsKey(row.getId())) {
          row.setProperty(PROP_VEHICLE_GROUPS,
              DataUtils.buildIdList(vehicleGroups.get(row.getId())));
        }
      }
    }

    Map<Long, Long> vehicleManagers = getVehicleManagers(groupWhere);
    if (!BeeUtils.isEmpty(vehicleManagers)) {
      for (BeeRow row : vehicles) {
        Long manager = vehicleManagers.get(row.getId());
        if (manager != null) {
          row.setProperty(PROP_VEHICLE_MANAGER, BeeUtils.toString(manager));
        }
      }
    }

    settings.setTableProperty(PROP_VEHICLES, vehicles.serialize());

    SimpleRowSet vehicleServices = getVehicleServices(SqlUtils.inList(TBL_VEHICLE_SERVICES,
        COL_VEHICLE, vehicleIds));
    if (!DataUtils.isEmpty(vehicleServices)) {
      settings.setTableProperty(PROP_VEHICLE_SERVICES, vehicleServices.serialize());
    }

    IsCondition tripWhere = SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_EXPEDITION),
        SqlUtils.inList(TBL_TRIPS, tripVehicleIdColumnName, vehicleIds),
        getChartTripCondition(period, completedTrips));

    SqlSelect tripQuery = getTripQuery(tripWhere);
    tripQuery.addOrder(TBL_TRIPS, tripVehicleIdColumnName, COL_TRIP_DATE);

    SimpleRowSet trips = qs.getData(tripQuery);
    if (DataUtils.isEmpty(trips)) {
      logger.warning(svc, "trips not available");
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_TRIPS, trips.serialize());

    SimpleRowSet drivers = getTripDrivers(tripWhere);
    if (!DataUtils.isEmpty(drivers)) {
      settings.setTableProperty(PROP_TRIP_DRIVERS, drivers.serialize());
    }

    SqlSelect freightQuery = getFreightQuery(tripWhere);

    SimpleRowSet freights = qs.getData(freightQuery);
    if (DataUtils.isEmpty(freights)) {
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_FREIGHTS, freights.serialize());

    SimpleRowSet cargoHandling = getFreightHandlingData(tripWhere, true);

    if (!DataUtils.isEmpty(cargoHandling)) {
      settings.setTableProperty(PROP_CARGO_HANDLING, cargoHandling.serialize());
    }
    return ResponseObject.response(settings);
  }

  private ResponseObject sendMessage(String message, String[] recipients) {
    String address = prm.getText("SmsServiceAddress");

    if (BeeUtils.isEmpty(address)) {
      return ResponseObject.error("SmsServiceAddress is empty");
    }
    JsonArrayBuilder jsonRecipients = Json.createArrayBuilder();

    for (String phone : recipients) {
      jsonRecipients.add(Json.createObjectBuilder()
          .add("from", prm.getText("SmsDisplayText"))
          .add("to", phone)
          .add("text", message));
    }
    JsonObjectBuilder json = Json.createObjectBuilder()
        .add("username", prm.getText("SmsUserName"))
        .add("password", prm.getText("SmsPassword"))
        .add("service_id", prm.getText("SmsServiceId"))
        .add("time", new DateTime().getTime())
        .add("message", jsonRecipients);

    Client client = ClientBuilder.newClient();

    Invocation.Builder builder = client.target(address)
        .request(MediaType.APPLICATION_JSON_TYPE);

    Map<String, String> headers = prm.getMap("SmsRequestHeaders");

    if (!BeeUtils.isEmpty(headers)) {
      for (Entry<String, String> entry : headers.entrySet()) {
        builder.header(entry.getKey(), entry.getValue());
      }
    }
    JsonObject jsonResponse = builder.post(Entity.json(json.build().toString()), JsonObject.class);

    ResponseObject response;

    if (jsonResponse.containsKey("response")) {
      JsonValue resp = jsonResponse.get("response");

      if (resp.getValueType() == JsonValue.ValueType.OBJECT) {
        response = ResponseObject.info(Localized.dictionary().messageSent(), resp);
      } else {
        response = ResponseObject.error(resp);
      }
    } else {
      response = ResponseObject.error("Unknown response:", jsonResponse);
    }
    return response;
  }

  private ResponseObject updatePercent(Long id, String column, Double amount, boolean percentMode) {
    String keyName;
    Function<Long, Double> totalSupplier;

    switch (column) {
      case COL_TRIP_PERCENT:
        keyName = COL_CARGO;
        totalSupplier = value -> BeeUtils.unbox(qs.getDouble(new SqlSelect()
            .addFields("als", COL_TRANSPORTATION)
            .addFrom(rep.getCargoAmountsQuery(new SqlSelect().addConstant(value, keyName),
                TBL_CARGO_INCOMES, prm.getRelation(PRM_CURRENCY),
                BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT)), false), "als")));
        break;
      case COL_CARGO_PERCENT:
        keyName = COL_TRIP;
        totalSupplier = value -> {
          String tmp = rep.getTripCosts(new SqlSelect().addConstant(value, keyName),
              prm.getRelation(PRM_CURRENCY), BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT)));
          SimpleRow row = qs.getRow(new SqlSelect().addAllFields(tmp).addFrom(tmp));
          qs.sqlDropTemp(tmp);

          return BeeUtils.unbox(row.getDouble("DailyCosts"))
              + BeeUtils.unbox(row.getDouble("RoadCosts"))
              + BeeUtils.unbox(row.getDouble("OtherCosts"))
              + BeeUtils.unbox(row.getDouble("FuelCosts"));
        };
        break;
      default:
        Assert.unsupported(column);
        return null;
    }
    long keyValue = qs.getLong(new SqlSelect()
        .addFields(TBL_CARGO_TRIPS, keyName)
        .addFrom(TBL_CARGO_TRIPS)
        .setWhere(sys.idEquals(TBL_CARGO_TRIPS, id)));

    if (!DataUtils.isId(keyValue)) {
      return ResponseObject.error(Localized.dictionary().noData());
    }
    double availablePercent = BeeUtils.max(100 - BeeUtils.unbox(qs.getDouble(new SqlSelect()
        .addSum(TBL_CARGO_TRIPS, column)
        .addFrom(TBL_CARGO_TRIPS)
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_CARGO_TRIPS, keyName, keyValue),
            SqlUtils.notEqual(TBL_CARGO_TRIPS, sys.getIdName(TBL_CARGO_TRIPS), id))))), 0.0);

    Double percent = null;

    if (Objects.nonNull(amount)) {
      percent = Math.max(amount, 0);
    }
    if (BeeUtils.isPositive(percent)) {
      if (percentMode) {
        if (percent > availablePercent) {
          return ResponseObject.error(percent + "% > " + availablePercent + "%");
        }
      } else {
        double total = totalSupplier.apply(keyValue);
        double availableAmount = BeeUtils.round(total / 100 * availablePercent, 2);

        if (percent > availableAmount) {
          return ResponseObject.error(percent + " > " + availableAmount);
        }
        percent = percent / total * 100;
      }
    }
    qs.updateData(new SqlUpdate(TBL_CARGO_TRIPS)
        .addConstant(column, percent)
        .setWhere(sys.idEquals(TBL_CARGO_TRIPS, id)));

    return ResponseObject.emptyResponse();
  }
}
