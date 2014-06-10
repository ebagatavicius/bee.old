package com.butent.bee.server.modules.transport;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.Invocation;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.ViewDeleteEvent;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.ParameterEvent;
import com.butent.bee.server.modules.ParameterEventHandler;
import com.butent.bee.server.modules.administration.ExchangeUtils;
import com.butent.bee.server.modules.administration.ExtensionIcons;
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
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.modules.transport.TransportConstants.AssessmentStatus;
import com.butent.bee.shared.modules.transport.TransportConstants.OrderStatus;
import com.butent.bee.shared.modules.transport.TransportConstants.VehicleType;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.Headline;
import com.butent.bee.shared.news.HeadlineProducer;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.webservice.ButentWS;
import com.butent.webservice.WSDocument;
import com.butent.webservice.WSDocument.WSDocumentItem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.servlet.http.HttpServletResponse;

@Stateless
@LocalBean
public class TransportModuleBean implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(TransportModuleBean.class);

  private static IsExpression getAssessmentTurnoverExpression(SqlSelect query, String source,
      String defDateSource, String defDateAlias, Long currency) {

    if (DataUtils.isId(currency)) {
      return ExchangeUtils.exchangeFieldTo(query,
          TradeModuleBean.getTotalExpression(source, SqlUtils.field(source, COL_AMOUNT)),
          SqlUtils.field(source, COL_CURRENCY),
          SqlUtils.nvl(SqlUtils.field(source, COL_DATE),
              SqlUtils.field(defDateSource, defDateAlias)),
          SqlUtils.constant(currency));

    } else {
      return ExchangeUtils.exchangeField(query,
          TradeModuleBean.getTotalExpression(source, SqlUtils.field(source, COL_AMOUNT)),
          SqlUtils.field(source, COL_CURRENCY),
          SqlUtils.nvl(SqlUtils.field(source, COL_DATE),
              SqlUtils.field(defDateSource, defDateAlias)));
    }
  }

  private static IsCondition tripCondition(IsCondition where) {
    return SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_EXPEDITION), where);
  }

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
  TransportImports imp;
  @EJB
  TradeModuleBean trd;

  @EJB
  NewsBean news;

  @Resource
  TimerService timerService;

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> result = Lists.newArrayList();

    if (usr.isModuleVisible(Module.TRANSPORT.getName())) {
      List<SearchResult> vehiclesResult = qs.getSearchResults(VIEW_VEHICLES,
          Filter.anyContains(Sets.newHashSet(COL_NUMBER, COL_PARENT_MODEL_NAME, COL_MODEL_NAME,
              COL_OWNER_NAME), query));

      Filter orderCargoFilter = Filter.anyContains(Sets.newHashSet(COL_CARGO_DESCRIPTION,
          COL_NUMBER, COL_CARGO_CMR, COL_CARGO_NOTES, COL_CARGO_DIRECTIONS,
          ALS_LOADING_NUMBER, ALS_LOADING_CONTACT, ALS_LOADING_COMPANY, ALS_LOADING_ADDRESS,
          ALS_LOADING_POST_INDEX, ALS_LOADING_CITY_NAME, ALS_LOADING_COUNTRY_NAME,
          ALS_LOADING_COUNTRY_CODE, ALS_UNLOADING_NUMBER, ALS_UNLOADING_CONTACT,
          ALS_UNLOADING_COMPANY, ALS_UNLOADING_ADDRESS, ALS_UNLOADING_POST_INDEX,
          ALS_UNLOADING_CITY_NAME, ALS_UNLOADING_COUNTRY_NAME, ALS_UNLOADING_COUNTRY_CODE),
          query);

      List<SearchResult> orderCargoResult = qs.getSearchResults(VIEW_ORDER_CARGO,
          orderCargoFilter
          );

      result.addAll(vehiclesResult);
      result.addAll(orderCargoResult);
    }

    return result;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    if (BeeUtils.same(svc, SVC_GET_BEFORE)) {
      long vehicle = BeeUtils.toLong(reqInfo.getParameter(COL_VEHICLE));
      long date = BeeUtils.toLong(reqInfo.getParameter(COL_DATE));

      response = getTripBeforeData(vehicle, date);
    } else if (BeeUtils.same(svc, SVC_GET_UNASSIGNED_CARGOS)) {
      response = getUnassignedCargos(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_PROFIT)) {
      if (reqInfo.hasParameter(COL_TRIP)) {
        response = getTripProfit(BeeUtils.toLong(reqInfo.getParameter(COL_TRIP)));

      } else if (reqInfo.hasParameter(COL_CARGO)) {
        Long cargoId = BeeUtils.toLong(reqInfo.getParameter(COL_CARGO));

        response = getCargoProfit(new SqlSelect().addConstant(cargoId, COL_CARGO));

      } else if (reqInfo.hasParameter(COL_ORDER)) {
        Long orderId = BeeUtils.toLong(reqInfo.getParameter(COL_ORDER));
        String cargo = VIEW_ORDER_CARGO;

        response = getCargoProfit(new SqlSelect()
            .addField(cargo, sys.getIdName(cargo), COL_CARGO)
            .addFrom(cargo)
            .setWhere(SqlUtils.equals(cargo, COL_ORDER, orderId)));

      } else {
        response = ResponseObject.error("Profit of WHAT?");
      }

    } else if (BeeUtils.same(svc, SVC_GET_FX_DATA)) {
      response = getFxData();

    } else if (BeeUtils.same(svc, SVC_GET_SS_DATA)) {
      response = getVehicleTbData(svc, Filter.in(COL_VEHICLE_ID, VIEW_TRIPS, COL_VEHICLE),
          VehicleType.TRUCK, COL_SS_THEME);

    } else if (BeeUtils.same(svc, SVC_GET_DTB_DATA)) {
      response = getDtbData();

    } else if (BeeUtils.same(svc, SVC_GET_TRUCK_TB_DATA)) {
      response = getVehicleTbData(svc, Filter.notNull(COL_IS_TRUCK), VehicleType.TRUCK,
          COL_TRUCK_THEME);

    } else if (BeeUtils.same(svc, SVC_GET_TRAILER_TB_DATA)) {
      response = getVehicleTbData(svc, Filter.notNull(COL_IS_TRAILER), VehicleType.TRAILER,
          COL_TRAILER_THEME);

    } else if (BeeUtils.same(svc, SVC_GET_COLORS)) {
      response = getColors(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_CARGO_USAGE)) {
      response = getCargoUsage(reqInfo.getParameter("ViewName"),
          Codec.beeDeserializeCollection(reqInfo.getParameter("IdList")));

    } else if (BeeUtils.same(svc, SVC_GET_ASSESSMENT_TOTALS)) {
      response = getAssessmentTotals(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ASSESSMENT)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_CURRENCY)),
          BeeUtils.toBoolean(reqInfo.getParameter("isPrimary")));

    } else if (BeeUtils.same(svc, SVC_GET_ASSESSMENT_QUANTITY_REPORT)) {
      response = getAssessmentQuantityReport(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_ASSESSMENT_TURNOVER_REPORT)) {
      response = getAssessmentTurnoverReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_CREATE_INVOICE_ITEMS)) {
      Long saleId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_SALE));
      Long purchaseId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_PURCHASE));
      Long currency = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_CURRENCY));
      Set<Long> ids = DataUtils.parseIdSet(reqInfo.getParameter(Service.VAR_ID));
      Long item = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_ITEM));
      Double creditAmount = BeeUtils.toDoubleOrNull(reqInfo.getParameter(COL_TRADE_AMOUNT));

      if (DataUtils.isId(saleId)) {
        response = createInvoiceItems(saleId, currency, ids, item);
      } else if (BeeUtils.isPositive(creditAmount)) {
        response = createCreditInvoiceItems(purchaseId, currency, ids, item, creditAmount);
      } else {
        response = createPurchaseInvoiceItems(purchaseId, currency, ids, item);
      }
    } else if (BeeUtils.same(svc, SVC_SEND_TO_ERP)) {
      response = sendToERP(reqInfo.getParameter("view_name"),
          DataUtils.parseIdSet(reqInfo.getParameter("IdList")));

    } else if (BeeUtils.same(svc, SVC_SEND_MESSAGE)) {
      response = sendMessage(reqInfo.getParameter(COL_DESCRIPTION),
          Codec.beeDeserializeCollection(reqInfo.getParameter(COL_MOBILE)));

    } else if (BeeUtils.same(svc, SVC_GET_IMPORT_MAPPINGS)) {
      response = getImportMappings(BeeUtils.toLong(reqInfo.getParameter(COL_IMPORT_PROPERTY)),
          reqInfo.getParameter(VAR_MAPPING_TABLE), reqInfo.getParameter(VAR_MAPPING_FIELD));

    } else if (BeeUtils.same(svc, SVC_DO_IMPORT)) {
      response = imp.doImport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_CREDIT_INFO)) {
      response = getCreditInfo(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_CARGO_TOTAL)) {
      response = getCargoTotal(BeeUtils.toLong(reqInfo.getParameter(COL_CARGO)),
          BeeUtils.toLongOrNull(reqInfo.getParameter(COL_CURRENCY)));

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
        BeeParameter.createText(module, PRM_INVOICE_PREFIX, true, null),
        BeeParameter.createCollection(module, PRM_MESSAGE_TEMPLATE, true, null),
        BeeParameter.createText(module, "ERPCreditOperation", false, null),
        BeeParameter.createNumber(module, PRM_ERP_REFRESH_INTERVAL, false, null),
        BeeParameter.createText(module, "SmsServiceAddress", false, null),
        BeeParameter.createText(module, "SmsUserName", false, null),
        BeeParameter.createText(module, "SmsPassword", false, null),
        BeeParameter.createText(module, "SmsServiceId", false, null),
        BeeParameter.createText(module, "SmsDisplayText", false, null));
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
    initTimer();

    prm.registerParameterEventHandler(new ParameterEventHandler() {
      @Subscribe
      public void initTimers(ParameterEvent event) {
        if (BeeUtils.same(event.getParameter(), PRM_ERP_REFRESH_INTERVAL)) {
          TransportModuleBean bean = Invocation.locateRemoteBean(TransportModuleBean.class);

          if (bean != null) {
            bean.initTimer();
          }
        }
      }
    });

    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      public void calcAssessmentAmounts(ViewQueryEvent event) {
        if (BeeUtils.same(event.getTargetName(), VIEW_CHILD_ASSESSMENTS) && event.isAfter()
            && event.getRowset().getNumberOfRows() > 0) {

          for (String tbl : new String[] {TBL_CARGO_INCOMES, TBL_CARGO_EXPENSES}) {
            SqlSelect query = new SqlSelect()
                .addField(TBL_ASSESSMENTS, sys.getIdName(TBL_ASSESSMENTS), COL_ASSESSMENT)
                .addFrom(TBL_ASSESSMENTS)
                .addFromInner(TBL_ORDER_CARGO,
                    sys.joinTables(TBL_ORDER_CARGO, TBL_ASSESSMENTS, COL_CARGO))
                .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
                .addFromInner(tbl, SqlUtils.joinUsing(TBL_ASSESSMENTS, tbl, COL_CARGO))
                .setWhere(sys.idInList(TBL_ASSESSMENTS, event.getRowset().getRowIds()))
                .addGroup(TBL_ASSESSMENTS, sys.getIdName(TBL_ASSESSMENTS));

            IsExpression xpr = ExchangeUtils.exchangeFieldTo(query,
                TradeModuleBean.getTotalExpression(tbl, SqlUtils.field(tbl, COL_AMOUNT)),
                SqlUtils.field(tbl, COL_CURRENCY),
                SqlUtils.nvl(SqlUtils.field(tbl, COL_DATE), SqlUtils.field(TBL_ORDERS, COL_DATE)),
                SqlUtils.field(TBL_ORDER_CARGO, COL_CURRENCY));

            SimpleRowSet rs = qs.getData(query.addSum(xpr, VAR_TOTAL));

            for (BeeRow row : event.getRowset().getRows()) {
              row.setProperty((BeeUtils.same(tbl, TBL_CARGO_INCOMES)) ? VAR_INCOME : VAR_EXPENSE,
                  rs.getValueByKey(COL_ASSESSMENT, BeeUtils.toString(row.getId()), VAR_TOTAL));
            }
          }
        }
      }

      @Subscribe
      public void deleteOrphanCargo(ViewDeleteEvent event) {
        if (BeeUtils.inListSame(event.getTargetName(), VIEW_SHIPMENT_REQUESTS,
            VIEW_CARGO_REQUESTS)) {

          if (event.isBefore()) {
            String tableName;
            String columnName;

            if (BeeUtils.same(event.getTargetName(), VIEW_SHIPMENT_REQUESTS)) {
              tableName = TBL_SHIPMENT_REQUESTS;
              columnName = COL_QUERY_CARGO;
            } else if (BeeUtils.same(event.getTargetName(), VIEW_CARGO_REQUESTS)) {
              tableName = TBL_CARGO_REQUESTS;
              columnName = COL_CARGO_REQUEST_CARGO;
            } else {
              tableName = null;
              columnName = null;
            }

            if (tableName == null) {
              return;
            }

            SqlSelect query =
                new SqlSelect().addFields(tableName, columnName).addFrom(tableName)
                    .addFromInner(TBL_ORDER_CARGO,
                        sys.joinTables(TBL_ORDER_CARGO, tableName, columnName))
                    .setWhere(SqlUtils.and(
                        SqlUtils.inList(tableName, sys.getIdName(tableName), event.getIds()),
                        SqlUtils.isNull(TBL_ORDER_CARGO, COL_ORDER)));

            Long[] cargos = qs.getLongColumn(query);
            if (ArrayUtils.length(cargos) > 0) {
              Set<Long> orphans = Sets.newHashSet(cargos);
              event.setUserObject(orphans);
            } else {
              event.setUserObject(null);
            }

          } else if (event.isAfter() && event.getUserObject() instanceof Collection) {
            Collection<?> orphans = (Collection<?>) event.getUserObject();

            if (!BeeUtils.isEmpty(orphans)) {
              SqlDelete delete =
                  new SqlDelete(TBL_ORDER_CARGO)
                      .setWhere(SqlUtils.inList(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO),
                          orphans));

              int deleteCount = qs.updateData(delete);
              if (deleteCount > 0) {
                logger.debug("deleted", deleteCount, "orphan cargo");
              }
            }
          }
        }
      }

      @Subscribe
      public void fillCargoIncomes(ViewQueryEvent event) {
        if (BeeUtils.same(event.getTargetName(), VIEW_ORDER_CARGO) && event.isAfter()) {
          BeeRowSet rowset = event.getRowset();

          if (!rowset.isEmpty()) {
            SimpleRowSet rs = qs.getData(getCargoIncomeQuery(event.getQuery()
                .resetFields().resetOrder().resetGroup()
                .addField(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO), COL_CARGO)
                .addGroup(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO)), null));

            for (BeeRow row : rowset.getRows()) {
              String cargoId = BeeUtils.toString(row.getId());
              String cargoIncome = rs.getValueByKey(COL_CARGO, cargoId, "CargoIncome");
              String servicesIncome = rs.getValueByKey(COL_CARGO, cargoId, "ServicesIncome");

              row.setProperty(VAR_INCOME, BeeUtils.toString(BeeUtils.toDouble(cargoIncome)
                  + BeeUtils.toDouble(servicesIncome)));
            }
          }
        }
      }

      @Subscribe
      public void fillFuelConsumptions(ViewQueryEvent event) {
        if (BeeUtils.same(event.getTargetName(), TBL_TRIP_ROUTES) && event.isAfter()) {
          BeeRowSet rowset = event.getRowset();

          if (!rowset.isEmpty()) {
            SimpleRowSet rs = qs.getData(getFuelConsumptionsQuery(event.getQuery()
                .resetFields().resetOrder().resetGroup()
                .addFields(TBL_TRIP_ROUTES, sys.getIdName(TBL_TRIP_ROUTES))
                .addGroup(TBL_TRIP_ROUTES, sys.getIdName(TBL_TRIP_ROUTES)), true));

            int colIndex = rowset.getColumnIndex("Consumption");

            for (BeeRow row : rowset.getRows()) {
              row.setValue(colIndex, rs.getValueByKey(sys.getIdName(TBL_TRIP_ROUTES),
                  BeeUtils.toString(row.getId()), "Quantity"));
            }
          }
        }
      }

      @Subscribe
      public void fillTripCargoIncomes(ViewQueryEvent event) {
        if (BeeUtils.same(event.getTargetName(), VIEW_TRIP_CARGO) && event.isAfter()) {
          BeeRowSet rowset = event.getRowset();

          if (!rowset.isEmpty()) {
            String crs = getTripIncome(event.getQuery().resetFields().resetOrder().resetGroup()
                .addFields(VIEW_CARGO_TRIPS, COL_TRIP).addGroup(VIEW_CARGO_TRIPS, COL_TRIP));

            SimpleRowSet rs = qs.getData(new SqlSelect().addAllFields(crs).addFrom(crs));
            qs.sqlDropTemp(crs);

            int cargoIndex = rowset.getColumnIndex(COL_CARGO);

            for (BeeRow row : rowset.getRows()) {
              row.setProperty(VAR_INCOME, rs.getValueByKey(COL_CARGO, row.getString(cargoIndex),
                  "TripIncome"));
            }
          }
        }
      }

      @Subscribe
      public void getFileIcons(ViewQueryEvent event) {
        if (BeeUtils.same(event.getTargetName(), VIEW_CARGO_REQUEST_FILES) && event.isAfter()) {
          ExtensionIcons.setIcons(event.getRowset(), ALS_FILE_NAME, PROP_ICON);
        }
      }

      @Subscribe
      public void getVisibleDrivers(ViewQueryEvent event) {
        if (BeeUtils.same(event.getTargetName(), TBL_DRIVERS) && event.isBefore()) {
          BeeView view = sys.getView(event.getTargetName());

          SqlSelect query = new SqlSelect().setDistinctMode(true)
              .addFields(TBL_DRIVER_GROUPS, COL_DRIVER)
              .addFrom(TBL_DRIVER_GROUPS)
              .addFromInner(TBL_TRANSPORT_GROUPS,
                  sys.joinTables(TBL_TRANSPORT_GROUPS, TBL_DRIVER_GROUPS, COL_GROUP));

          sys.filterVisibleState(query, TBL_TRANSPORT_GROUPS);

          event.getQuery().addFromInner(query, "subq",
              SqlUtils.join(view.getSourceAlias(), view.getSourceIdName(), "subq", COL_DRIVER));
        }
      }

      @Subscribe
      public void getVisibleVehicles(ViewQueryEvent event) {
        if (BeeUtils.same(event.getTargetName(), TBL_VEHICLES) && event.isBefore()) {
          BeeView view = sys.getView(event.getTargetName());

          SqlSelect query = new SqlSelect().setDistinctMode(true)
              .addFields(TBL_VEHICLE_GROUPS, COL_VEHICLE)
              .addFrom(TBL_VEHICLE_GROUPS)
              .addFromInner(TBL_TRANSPORT_GROUPS,
                  sys.joinTables(TBL_TRANSPORT_GROUPS, TBL_VEHICLE_GROUPS, COL_GROUP));

          sys.filterVisibleState(query, TBL_TRANSPORT_GROUPS);

          event.getQuery().addFromInner(query, "subq",
              SqlUtils.join(view.getSourceAlias(), view.getSourceIdName(), "subq", COL_VEHICLE));
        }
      }

      @Subscribe
      public void updateAssessmentRelations(ViewInsertEvent event) {
        String tbl = sys.getViewSource(event.getTargetName());

        if (BeeUtils.inListSame(tbl, TBL_ASSESSMENTS, TBL_ASSESSMENT_FORWARDERS)
            && event.isAfter()) {
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
    });

    HeadlineProducer assessmentsHeadlineProducer = new HeadlineProducer() {

      @Override
      public Headline produce(Feed feed, long userId, BeeRowSet rowSet, IsRow row, boolean isNew) {
        String caption = "";
        String pid = DataUtils.getString(rowSet, row, COL_ASSESSMENT);

        if (!BeeUtils.isEmpty(pid)) {
          caption = BeeUtils.joinWords(caption, usr.getLocalizableConstants().captionPid()
              + BeeConst.STRING_COLON, pid);
        }

        String id = BeeUtils.toString(row.getId());

        caption = BeeUtils.joinWords(caption, usr.getLocalizableConstants().captionId()
            + BeeConst.STRING_COLON, id);

        AssessmentStatus status =
            EnumUtils.getEnumByIndex(AssessmentStatus.class,
                DataUtils.getInteger(rowSet, row, COL_STATUS));

        if (status != null) {
          caption = BeeUtils.joinWords(caption, status.getCaption(
              usr.getLocalizableConstants(userId)));
        }

        String notes = DataUtils.getString(rowSet, row, ALS_ORDER_NOTES);
        String customer = DataUtils.getString(rowSet, row, TransportConstants.ALS_CUSTOMER_NAME);

        caption = BeeUtils.joinWords(caption, notes, customer);

        return Headline.create(row.getId(), caption, isNew);
      }
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

    news.registerUsageQueryProvider(Feed.CARGO_REQUESTS_MY, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.equals(TBL_CARGO_REQUESTS,
            COL_CARGO_REQUEST_MANAGER, userId));
      }

      @Override
      protected List<Pair<String, IsCondition>> getJoins() {
        return NewsHelper.buildJoin(TBL_CARGO_REQUESTS, news.joinUsage(TBL_CARGO_REQUESTS));
      }
    });

    news.registerUsageQueryProvider(Feed.SHIPMENT_REQUESTS_MY, new ExtendedUsageQueryProvider() {
      @Override
      protected List<IsCondition> getConditions(long userId) {
        return NewsHelper.buildConditions(SqlUtils.equals(TBL_SHIPMENT_REQUESTS,
            COL_QUERY_MANAGER, userId));
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

    news.registerUsageQueryProvider(Feed.ASSESSMENT_TRANSPORTATIONS,
        new UsageQueryProvider() {

          @Override
          public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
              DateTime startDate) {
            SqlSelect select = new SqlSelect()
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
            return select;
          }

          @Override
          public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
              DateTime startDate) {
            SqlSelect select = new SqlSelect()
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
            return select;
          }
        });
  }

  public void initTimer() {
    Timer erpTimer = null;

    for (Timer timer : timerService.getTimers()) {
      if (Objects.equal(timer.getInfo(), PRM_ERP_REFRESH_INTERVAL)) {
        erpTimer = timer;
        break;
      }
    }
    if (erpTimer != null) {
      erpTimer.cancel();
    }
    Integer minutes = prm.getInteger(PRM_ERP_REFRESH_INTERVAL);

    if (BeeUtils.isPositive(minutes)) {
      erpTimer = timerService.createIntervalTimer(minutes * TimeUtils.MILLIS_PER_MINUTE,
          minutes * TimeUtils.MILLIS_PER_MINUTE, new TimerConfig(PRM_ERP_REFRESH_INTERVAL, false));

      logger.info("Created ERP refresh timer every", minutes, "minutes starting at",
          erpTimer.getNextTimeout());
    } else {
      if (erpTimer != null) {
        logger.info("Removed ERP timer");
      }
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
        .addFields(TBL_SALES, COL_TRADE_INVOICE_PREFIX, COL_TRADE_INVOICE_NO)
        .addFields(TBL_CARGO_INCOMES, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)
        .addFrom(TBL_CARGO_INCOMES)
        .addFromInner(TBL_SERVICES,
            sys.joinTables(TBL_SERVICES, TBL_CARGO_INCOMES, COL_SERVICE))
        .addFromInner(TBL_SALES,
            sys.joinTables(TBL_SALES, TBL_CARGO_INCOMES, COL_SALE))
        .addFromInner(TBL_ORDER_CARGO,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
        .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .setWhere(SqlUtils.and(wh, SqlUtils.positive(TBL_CARGO_INCOMES, COL_AMOUNT)))
        .addGroup(TBL_ORDERS, COL_ORDER_NO)
        .addGroup(TBL_SALES, COL_TRADE_INVOICE_PREFIX, COL_TRADE_INVOICE_NO)
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
          COL_TRADE_INVOICE_NO, BeeUtils.joinWords(row.getValue(COL_TRADE_INVOICE_PREFIX),
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

    String loadPlace = SqlUtils.uniqueName();
    String unloadPlace = SqlUtils.uniqueName();
    String loadCountry = SqlUtils.uniqueName();
    String unloadCountry = SqlUtils.uniqueName();
    String loadCity = SqlUtils.uniqueName();
    String unloadCity = SqlUtils.uniqueName();

    SqlSelect ss = new SqlSelect()
        .addFields(TBL_ORDERS, COL_ORDER_NO)
        .addFields(TBL_ORDER_CARGO, COL_CARGO_CMR)
        .addFields(TBL_CARGO_INCOMES,
            COL_CARGO, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)
        .addField(loadPlace, COL_DATE, COL_LOADING_PLACE)
        .addField(unloadPlace, COL_DATE, COL_UNLOADING_PLACE)
        .addField(loadPlace, COL_POST_INDEX, loadPlace)
        .addField(unloadPlace, COL_POST_INDEX, unloadPlace)
        .addField(loadCity, COL_CITY_NAME, loadCity)
        .addField(unloadCity, COL_CITY_NAME, unloadCity)
        .addField(loadCountry, COL_COUNTRY_CODE, loadCountry)
        .addField(unloadCountry, COL_COUNTRY_CODE, unloadCountry)
        .addFrom(TBL_CARGO_INCOMES)
        .addFromInner(TBL_SERVICES,
            sys.joinTables(TBL_SERVICES, TBL_CARGO_INCOMES, COL_SERVICE))
        .addFromInner(TBL_ORDER_CARGO,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
        .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_CARGO_PLACES, loadPlace,
            sys.joinTables(TBL_CARGO_PLACES, loadPlace, TBL_ORDER_CARGO, COL_LOADING_PLACE))
        .addFromLeft(TBL_CITIES, loadCity,
            sys.joinTables(TBL_CITIES, loadCity, loadPlace, COL_CITY))
        .addFromLeft(TBL_COUNTRIES, loadCountry,
            sys.joinTables(TBL_COUNTRIES, loadCountry, loadPlace, COL_COUNTRY))
        .addFromLeft(TBL_CARGO_PLACES, unloadPlace,
            sys.joinTables(TBL_CARGO_PLACES, unloadPlace, TBL_ORDER_CARGO,
                COL_UNLOADING_PLACE))
        .addFromLeft(TBL_CITIES, unloadCity,
            sys.joinTables(TBL_CITIES, unloadCity, unloadPlace, COL_CITY))
        .addFromLeft(TBL_COUNTRIES, unloadCountry,
            sys.joinTables(TBL_COUNTRIES, unloadCountry, unloadPlace, COL_COUNTRY))
        .setWhere(wh)
        .addGroup(TBL_ORDERS, COL_ORDER_NO)
        .addGroup(TBL_ORDER_CARGO, COL_CARGO_CMR)
        .addGroup(TBL_CARGO_INCOMES,
            COL_CARGO, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)
        .addGroup(loadPlace, COL_DATE, COL_POST_INDEX)
        .addGroup(unloadPlace, COL_DATE, COL_POST_INDEX)
        .addGroup(loadCity, COL_CITY_NAME)
        .addGroup(unloadCity, COL_CITY_NAME)
        .addGroup(loadCountry, COL_COUNTRY_CODE)
        .addGroup(unloadCountry, COL_COUNTRY_CODE);

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

    String vehicle = SqlUtils.uniqueName();
    String trailer = SqlUtils.uniqueName();

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
        .setWhere(SqlUtils.inList(TBL_CARGO_TRIPS, COL_CARGO,
            Sets.newHashSet(rs.getLongColumn(COL_CARGO)))));

    Multimap<Long, String> drivers = HashMultimap.create();
    Multimap<Long, String> vehicles = HashMultimap.create();

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
    for (SimpleRow row : rs) {
      String xml = XmlUtils.createString("CargoInfo",
          COL_ORDER_NO, row.getValue(COL_ORDER_NO),
          COL_LOADING_PLACE,
          BeeUtils.joinWords(JustDate.get(row.getDateTime(COL_LOADING_PLACE)),
              row.getValue(loadCountry),
              BeeUtils.parenthesize(BeeUtils.joinItems(row.getValue(loadPlace),
                  row.getValue(loadCity)))),
          COL_UNLOADING_PLACE,
          BeeUtils.joinWords(JustDate.get(row.getDateTime(COL_UNLOADING_PLACE)),
              row.getValue(unloadCountry),
              BeeUtils.parenthesize(BeeUtils.joinItems(row.getValue(unloadPlace),
                  row.getValue(unloadCity)))),
          COL_CARGO_CMR, row.getValue(COL_CARGO_CMR),
          COL_DRIVER, BeeUtils.joinItems(drivers.get(row.getLong(COL_CARGO))),
          COL_VEHICLE, BeeUtils.joinItems(vehicles.get(row.getLong(COL_CARGO))));

      SqlInsert insert = new SqlInsert(TBL_SALE_ITEMS)
          .addConstant(COL_SALE, saleId)
          .addConstant(COL_ITEM, row.getLong(COL_ITEM))
          .addConstant(COL_TRADE_ITEM_QUANTITY, 1)
          .addConstant(COL_TRADE_ITEM_PRICE, row.getDouble(COL_AMOUNT))
          .addConstant(COL_TRADE_VAT_PLUS, row.getBoolean(COL_TRADE_VAT_PLUS))
          .addConstant(COL_TRADE_VAT, row.getDouble(COL_TRADE_VAT))
          .addConstant(COL_TRADE_VAT_PERC, row.getBoolean(COL_TRADE_VAT_PERC))
          .addConstant(COL_TRADE_ITEM_NOTE, xml);

      qs.insertData(insert);
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
        .setWhere(SqlUtils.and(wh, SqlUtils.positive(TBL_CARGO_EXPENSES, COL_AMOUNT)))
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

  private ResponseObject getAssessmentTotals(Long assessmentId, Long currency, boolean isPrimary) {
    Assert.state(DataUtils.isId(assessmentId));

    SqlSelect query = null;

    for (String tbl : new String[] {TBL_CARGO_INCOMES, TBL_CARGO_EXPENSES}) {
      for (int i = 0; i < 2; i++) {
        if (i > 0 && !isPrimary) {
          break;
        }
        SqlSelect ss = new SqlSelect()
            .addConstant(tbl + (i > 0 ? VAR_TOTAL : ""), COL_SERVICE)
            .addFrom(tbl)
            .addFromInner(TBL_ASSESSMENTS, SqlUtils.joinUsing(tbl, TBL_ASSESSMENTS, COL_CARGO))
            .addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, tbl, COL_CARGO))
            .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER));

        if (i > 0) {
          ss.setWhere(SqlUtils.equals(TBL_ASSESSMENTS, COL_ASSESSMENT, assessmentId));
        } else {
          ss.setWhere(sys.idEquals(TBL_ASSESSMENTS, assessmentId));
        }
        IsExpression xpr = ExchangeUtils.exchangeFieldTo(ss,
            TradeModuleBean.getTotalExpression(tbl, SqlUtils.field(tbl, COL_AMOUNT)),
            SqlUtils.field(tbl, COL_CURRENCY),
            SqlUtils.nvl(SqlUtils.field(tbl, COL_DATE), SqlUtils.field(TBL_ORDERS, COL_DATE)),
            SqlUtils.constant(currency));

        ss.addSum(xpr, COL_AMOUNT);

        if (query == null) {
          query = ss;
        } else {
          query.setUnionAllMode(true).addUnion(ss);
        }
      }
    }
    return ResponseObject.response(qs.getData(query));
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
        where.add(SqlUtils.moreEqual(TBL_CARGO_PLACES, COL_PLACE_DATE, startDate));
      }
      if (endDate != null) {
        where.add(SqlUtils.less(TBL_CARGO_PLACES, COL_PLACE_DATE, endDate));
      }
    } else {
      where.add(SqlUtils.notNull(TBL_CARGO_PLACES, COL_PLACE_DATE));
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

    String orderDateAlias = "OrdDt" + SqlUtils.uniqueName();
    query.addField(TBL_ORDERS, COL_ORDER_DATE, orderDateAlias);

    if (groupBy.contains(BeeConst.MONTH)) {
      query.addFields(TBL_CARGO_PLACES, COL_PLACE_DATE);
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

    query.addFrom(TBL_ASSESSMENTS);
    query.addFromInner(TBL_ORDER_CARGO,
        sys.joinTables(TBL_ORDER_CARGO, TBL_ASSESSMENTS, COL_CARGO));
    query.addFromInner(TBL_CARGO_PLACES,
        sys.joinTables(TBL_CARGO_PLACES, TBL_ORDER_CARGO, COL_UNLOADING_PLACE));
    query.addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER));

    if (!managers.isEmpty() || groupBy.contains(AR_MANAGER)) {
      query.addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, TBL_ORDERS, COL_ORDER_MANAGER));
    }

    query.setWhere(where);

    String tmp = qs.sqlCreateTemp(query);

    long count;
    if (groupBy.contains(BeeConst.MONTH)) {
      count = qs.setYearMonth(tmp, COL_PLACE_DATE, BeeConst.YEAR, BeeConst.MONTH);
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
        tmp, orderDateAlias, currency);
    subIncome.addSum(incomeXpr, AR_INCOME);

    SqlSelect subExpense = new SqlSelect()
        .addFields(tmp, COL_CARGO)
        .addFrom(tmp)
        .addFromInner(TBL_CARGO_EXPENSES,
            SqlUtils.join(TBL_CARGO_EXPENSES, COL_CARGO, tmp, COL_CARGO))
        .addGroup(tmp, COL_CARGO);

    IsExpression expenseXpr = getAssessmentTurnoverExpression(subExpense, TBL_CARGO_EXPENSES,
        tmp, orderDateAlias, currency);
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

  /**
   * Return SqlSelect query, calculating cargo costs from CargoServices table.
   * 
   * @param flt - query filter with <b>unique</b> "Cargo" values.
   * @return query with columns: "Cargo", "Expense"
   */
  private SqlSelect getCargoCostQuery(SqlSelect flt) {
    String alias = SqlUtils.uniqueName();

    SqlSelect ss = new SqlSelect()
        .addField(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO), COL_CARGO)
        .addFrom(TBL_ORDER_CARGO)
        .addFromInner(flt, alias, sys.joinTables(TBL_ORDER_CARGO, alias, COL_CARGO))
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_CARGO_EXPENSES,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_EXPENSES, COL_CARGO))
        .addGroup(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO));

    ss.addSum(ExchangeUtils.exchangeField(ss,
        TradeModuleBean.getTotalExpression(TBL_CARGO_EXPENSES,
            SqlUtils.field(TBL_CARGO_EXPENSES, COL_AMOUNT)),
        SqlUtils.field(TBL_CARGO_EXPENSES, COL_CURRENCY),
        SqlUtils.nvl(SqlUtils.field(TBL_CARGO_EXPENSES, COL_DATE),
            SqlUtils.field(TBL_ORDERS, COL_DATE))), VAR_EXPENSE);

    return ss;
  }

  /**
   * Return SqlSelect query, calculating cargo incomes from CargoServices table.
   * 
   * @param flt - query filter with <b>unique</b> "Cargo" values.
   * @param currency - currencyId, to which convert amounts.
   * @return query with columns: "Cargo", "CargoIncome", "ServicesIncome"
   */
  private SqlSelect getCargoIncomeQuery(SqlSelect flt, Long currency) {
    String alias = SqlUtils.uniqueName();

    SqlSelect ss = new SqlSelect()
        .addField(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO), COL_CARGO)
        .addFrom(TBL_ORDER_CARGO)
        .addFromInner(flt, alias, sys.joinTables(TBL_ORDER_CARGO, alias, COL_CARGO))
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_CARGO_INCOMES,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
        .addFromLeft(TBL_SERVICES, sys.joinTables(TBL_SERVICES, TBL_CARGO_INCOMES, COL_SERVICE))
        .addGroup(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO));

    IsExpression dateExpr = SqlUtils.nvl(SqlUtils.field(TBL_CARGO_INCOMES, COL_DATE),
        SqlUtils.field(TBL_ORDERS, COL_DATE));

    IsExpression cargoIncome;
    IsExpression servicesIncome;

    if (DataUtils.isId(currency)) {
      cargoIncome = ExchangeUtils.exchangeFieldTo(ss,
          SqlUtils.sqlIf(SqlUtils.isNull(TBL_SERVICES, COL_TRANSPORTATION), null,
              TradeModuleBean.getTotalExpression(TBL_CARGO_INCOMES,
                  SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT))),
          SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY), dateExpr,
          SqlUtils.constant(currency));

      servicesIncome = ExchangeUtils.exchangeFieldTo(ss,
          SqlUtils.sqlIf(SqlUtils.isNull(TBL_SERVICES, COL_TRANSPORTATION),
              TradeModuleBean.getTotalExpression(TBL_CARGO_INCOMES,
                  SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT)), null),
          SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY), dateExpr,
          SqlUtils.constant(currency));
    } else {
      cargoIncome = ExchangeUtils.exchangeField(ss,
          SqlUtils.sqlIf(SqlUtils.isNull(TBL_SERVICES, COL_TRANSPORTATION), null,
              TradeModuleBean.getTotalExpression(TBL_CARGO_INCOMES,
                  SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT))),
          SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY), dateExpr);

      servicesIncome = ExchangeUtils.exchangeField(ss,
          SqlUtils.sqlIf(SqlUtils.isNull(TBL_SERVICES, COL_TRANSPORTATION),
              TradeModuleBean.getTotalExpression(TBL_CARGO_INCOMES,
                  SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT)), null),
          SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY), dateExpr);
    }

    ss.addSum(cargoIncome, "CargoIncome")
        .addSum(servicesIncome, "ServicesIncome");

    return ss;
  }

  private ResponseObject getCargoProfit(SqlSelect flt) {
    SqlSelect ss = getCargoIncomeQuery(flt, null)
        .addEmptyDouble("ServicesCost")
        .addEmptyDouble("TripCost");

    String crsTotals = qs.sqlCreateTemp(ss);
    qs.sqlIndex(crsTotals, COL_CARGO);

    String alias = SqlUtils.uniqueName();

    qs.updateData(new SqlUpdate(crsTotals)
        .setFrom(getCargoCostQuery(flt), alias, SqlUtils.joinUsing(crsTotals, alias, COL_CARGO))
        .addExpression("ServicesCost", SqlUtils.field(alias, VAR_EXPENSE)));

    ss = new SqlSelect()
        .setDistinctMode(true)
        .addFields(TBL_CARGO_TRIPS, COL_TRIP)
        .addFrom(TBL_CARGO_TRIPS)
        .addFromInner(crsTotals, SqlUtils.joinUsing(TBL_CARGO_TRIPS, crsTotals, COL_CARGO));

    String crsIncomes = getTripIncome(ss);
    String crsCosts = getTripCost(ss);

    ss = new SqlSelect()
        .addFields(crsIncomes, COL_TRIP)
        .addSum(TBL_CARGO_TRIPS, "CargoPercent", "TotalPercent")
        .addSum(SqlUtils.sqlIf(SqlUtils.isNull(TBL_CARGO_TRIPS, "CargoPercent"),
            SqlUtils.field(crsIncomes, "TripIncome"), 0), "TotalIncome")
        .addFrom(crsIncomes)
        .addFromInner(TBL_CARGO_TRIPS,
            SqlUtils.joinUsing(crsIncomes, TBL_CARGO_TRIPS, COL_TRIP, COL_CARGO))
        .addGroup(crsIncomes, COL_TRIP);

    String tmp = qs.sqlCreateTemp(ss);
    qs.sqlIndex(tmp, COL_TRIP);

    IsExpression xpr = SqlUtils.multiply(
        SqlUtils.divide(
            SqlUtils.plus(SqlUtils.nvl(SqlUtils.field(crsCosts, "TripCost"), 0),
                SqlUtils.nvl(SqlUtils.field(crsCosts, "FuelCost"), 0)),
            100),
        SqlUtils.sqlIf(SqlUtils.isNull(TBL_CARGO_TRIPS, "CargoPercent"),
            SqlUtils.multiply(
                SqlUtils.minus(100, SqlUtils.nvl(SqlUtils.field(tmp, "TotalPercent"), 0)),
                SqlUtils.divide(SqlUtils.field(crsIncomes, "TripIncome"),
                    SqlUtils.field(tmp, "TotalIncome"))),
            SqlUtils.field(TBL_CARGO_TRIPS, "CargoPercent")));

    ss = new SqlSelect()
        .addFields(crsIncomes, COL_CARGO)
        .addSum(xpr, "Cost")
        .addFrom(crsIncomes)
        .addFromInner(TBL_CARGO_TRIPS,
            SqlUtils.joinUsing(crsIncomes, TBL_CARGO_TRIPS, COL_TRIP, COL_CARGO))
        .addFromInner(crsCosts, SqlUtils.joinUsing(crsIncomes, crsCosts, COL_TRIP))
        .addFromInner(tmp, SqlUtils.joinUsing(crsIncomes, tmp, COL_TRIP))
        .addGroup(crsIncomes, COL_CARGO);

    String crsTripCosts = qs.sqlCreateTemp(ss);
    qs.sqlIndex(crsTripCosts, COL_CARGO);

    qs.sqlDropTemp(tmp);
    qs.sqlDropTemp(crsCosts);
    qs.sqlDropTemp(crsIncomes);

    SqlUpdate su = new SqlUpdate(crsTotals)
        .setFrom(crsTripCosts, SqlUtils.joinUsing(crsTotals, crsTripCosts, COL_CARGO))
        .addExpression("TripCost", SqlUtils.field(crsTripCosts, "Cost"));

    qs.updateData(su);

    qs.sqlDropTemp(crsTripCosts);

    ss = new SqlSelect()
        .addSum(crsTotals, "CargoIncome")
        .addSum(crsTotals, "TripCost")
        .addSum(crsTotals, "ServicesIncome")
        .addSum(crsTotals, "ServicesCost")
        .addFrom(crsTotals);

    SimpleRow res = qs.getRow(ss);

    qs.sqlDropTemp(crsTotals);

    return ResponseObject.response(new String[] {"CargoIncome:", res.getValue("CargoIncome"),
        "TripCost:", res.getValue("TripCost"), "ServicesIncome:", res.getValue("ServicesIncome"),
        "ServicesCost:", res.getValue("ServicesCost")});
  }

  private ResponseObject getCargoTotal(long cargoId, Long currency) {
    String val = null;
    SimpleRow row = qs.getRow(getCargoIncomeQuery(new SqlSelect().addConstant(cargoId, COL_CARGO),
        currency));

    if (row != null) {
      val = BeeUtils.round(row.getValue("CargoIncome"), 2);
    }
    return ResponseObject.response(BeeUtils.notEmpty(val, "0.00"));
  }

  private ResponseObject getCargoUsage(String viewName, String[] ids) {
    String source = sys.getViewSource(viewName);
    IsExpression ref;
    SqlSelect ss = new SqlSelect().addFrom(TBL_CARGO_TRIPS);

    if (BeeUtils.same(source, TBL_TRIPS)) {
      ref = SqlUtils.field(TBL_CARGO_TRIPS, COL_TRIP);

    } else if (BeeUtils.same(source, TBL_ORDER_CARGO)) {
      ref = SqlUtils.field(TBL_CARGO_TRIPS, COL_CARGO);

    } else if (BeeUtils.same(source, TBL_ORDERS)) {
      ss.addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO));
      ref = SqlUtils.field(TBL_ORDER_CARGO, COL_ORDER);

    } else {
      return ResponseObject.error("Table not supported:", source);
    }
    int cnt = qs.sqlCount(ss.setWhere(SqlUtils.inList(ref, (Object[]) ids)));

    return ResponseObject.response(cnt);
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
    Map<String, Object> resp = Maps.newHashMap();

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

  private ResponseObject getDtbData() {
    BeeRowSet settings = getSettings();
    if (settings == null) {
      return ResponseObject.error("user settings not available");
    }

    List<Color> colors = getThemeColors(null);
    settings.setTableProperty(PROP_COLORS, Codec.beeSerialize(colors));

    BeeRowSet countries = qs.getViewData(VIEW_COUNTRIES);
    settings.setTableProperty(PROP_COUNTRIES, countries.serialize());

    BeeRowSet cities = qs.getViewData(VIEW_CITIES, Filter.any(COL_COUNTRY, countries.getRowIds()));
    settings.setTableProperty(PROP_CITIES, cities.serialize());

    BeeRowSet drivers = qs.getViewData(VIEW_DRIVERS);
    if (DataUtils.isEmpty(drivers)) {
      logger.warning(SVC_GET_DTB_DATA, "drivers not available");
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_DRIVERS, drivers.serialize());

    List<Long> driverIds = DataUtils.getRowIds(drivers);

    BeeRowSet absence = qs.getViewData(VIEW_DRIVER_ABSENCE, Filter.any(COL_DRIVER, driverIds));
    if (!DataUtils.isEmpty(absence)) {
      settings.setTableProperty(PROP_ABSENCE, absence.serialize());
    }

    IsCondition tripDriverWhere = SqlUtils.inList(TBL_TRIP_DRIVERS, COL_DRIVER, driverIds);

    SimpleRowSet tripDrivers = getTripDrivers(tripDriverWhere);
    if (DataUtils.isEmpty(tripDrivers)) {
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_TRIP_DRIVERS, tripDrivers.serialize());

    IsCondition tripWhere = SqlUtils.in(TBL_TRIPS, sys.getIdName(TBL_TRIPS),
        TBL_TRIP_DRIVERS, COL_TRIP, tripDriverWhere);

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

    SqlSelect cargoHandlingQuery = getFreightHandlingQuery(tripWhere);

    SimpleRowSet cargoHandling = qs.getData(cargoHandlingQuery);
    if (!DataUtils.isEmpty(cargoHandling)) {
      settings.setTableProperty(PROP_CARGO_HANDLING, cargoHandling.serialize());
    }

    return ResponseObject.response(settings);
  }

  private SqlSelect getFreightHandlingQuery(IsCondition tripWhere) {
    String loadAlias = "load_" + SqlUtils.uniqueName();
    String unlAlias = "unl_" + SqlUtils.uniqueName();

    String colPlaceId = sys.getIdName(TBL_CARGO_PLACES);

    IsCondition handlingWhere = SqlUtils.or(SqlUtils.notNull(loadAlias, COL_PLACE_DATE),
        SqlUtils.notNull(unlAlias, COL_PLACE_DATE));

    return new SqlSelect()
        .addFrom(TBL_TRIPS)
        .addFromInner(TBL_CARGO_TRIPS,
            SqlUtils.join(TBL_CARGO_TRIPS, COL_TRIP, TBL_TRIPS, COL_TRIP_ID))
        .addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
        .addFromInner(TBL_CARGO_HANDLING,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_HANDLING, COL_CARGO))
        .addFromLeft(TBL_CARGO_PLACES, loadAlias,
            SqlUtils.join(loadAlias, colPlaceId, TBL_CARGO_HANDLING, COL_LOADING_PLACE))
        .addFromLeft(TBL_CARGO_PLACES, unlAlias,
            SqlUtils.join(unlAlias, colPlaceId, TBL_CARGO_HANDLING, COL_UNLOADING_PLACE))
        .addFields(TBL_CARGO_HANDLING, COL_CARGO, COL_CARGO_HANDLING_NOTES)
        .addField(loadAlias, COL_PLACE_DATE, loadingColumnAlias(COL_PLACE_DATE))
        .addField(loadAlias, COL_PLACE_COUNTRY, loadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(loadAlias, COL_PLACE_ADDRESS, loadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(loadAlias, COL_PLACE_POST_INDEX, loadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(loadAlias, COL_PLACE_CITY, loadingColumnAlias(COL_PLACE_CITY))
        .addField(loadAlias, COL_PLACE_NUMBER, loadingColumnAlias(COL_PLACE_NUMBER))
        .addField(unlAlias, COL_PLACE_DATE, unloadingColumnAlias(COL_PLACE_DATE))
        .addField(unlAlias, COL_PLACE_COUNTRY, unloadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(unlAlias, COL_PLACE_ADDRESS, unloadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(unlAlias, COL_PLACE_POST_INDEX, unloadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(unlAlias, COL_PLACE_CITY, unloadingColumnAlias(COL_PLACE_CITY))
        .addField(unlAlias, COL_PLACE_NUMBER, unloadingColumnAlias(COL_PLACE_NUMBER))
        .setWhere(SqlUtils.and(tripWhere, handlingWhere))
        .addOrder(TBL_CARGO_HANDLING, COL_CARGO);
  }

  private SqlSelect getFreightQuery(IsCondition where) {
    String loadAlias = "load_" + SqlUtils.uniqueName();
    String unlAlias = "unl_" + SqlUtils.uniqueName();

    String defLoadAlias = "defl_" + SqlUtils.uniqueName();
    String defUnlAlias = "defu_" + SqlUtils.uniqueName();

    String colPlaceId = sys.getIdName(TBL_CARGO_PLACES);

    return new SqlSelect()
        .addFrom(TBL_TRIPS)
        .addFromInner(TBL_CARGO_TRIPS,
            SqlUtils.join(TBL_CARGO_TRIPS, COL_TRIP, TBL_TRIPS, COL_TRIP_ID))
        .addFromLeft(TBL_CARGO_PLACES, loadAlias,
            SqlUtils.join(loadAlias, colPlaceId, TBL_CARGO_TRIPS, COL_LOADING_PLACE))
        .addFromLeft(TBL_CARGO_PLACES, unlAlias,
            SqlUtils.join(unlAlias, colPlaceId, TBL_CARGO_TRIPS, COL_UNLOADING_PLACE))
        .addFromInner(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
        .addFromLeft(TBL_CARGO_PLACES, defLoadAlias,
            SqlUtils.join(defLoadAlias, colPlaceId, TBL_ORDER_CARGO, COL_LOADING_PLACE))
        .addFromLeft(TBL_CARGO_PLACES, defUnlAlias,
            SqlUtils.join(defUnlAlias, colPlaceId, TBL_ORDER_CARGO, COL_UNLOADING_PLACE))
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, TBL_ORDERS, COL_CUSTOMER))
        .addFields(TBL_TRIPS, COL_TRIP_ID, COL_VEHICLE, COL_TRAILER)
        .addFields(TBL_CARGO_TRIPS, COL_CARGO, COL_CARGO_TRIP_ID)
        .addField(TBL_CARGO_TRIPS, sys.getVersionName(TBL_CARGO_TRIPS), ALS_CARGO_TRIP_VERSION)
        .addField(loadAlias, COL_PLACE_DATE, loadingColumnAlias(COL_PLACE_DATE))
        .addField(loadAlias, COL_PLACE_COUNTRY, loadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(loadAlias, COL_PLACE_ADDRESS, loadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(loadAlias, COL_PLACE_POST_INDEX, loadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(loadAlias, COL_PLACE_CITY, loadingColumnAlias(COL_PLACE_CITY))
        .addField(loadAlias, COL_PLACE_NUMBER, loadingColumnAlias(COL_PLACE_NUMBER))
        .addField(unlAlias, COL_PLACE_DATE, unloadingColumnAlias(COL_PLACE_DATE))
        .addField(unlAlias, COL_PLACE_COUNTRY, unloadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(unlAlias, COL_PLACE_ADDRESS, unloadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(unlAlias, COL_PLACE_POST_INDEX, unloadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(unlAlias, COL_PLACE_CITY, unloadingColumnAlias(COL_PLACE_CITY))
        .addField(unlAlias, COL_PLACE_NUMBER, unloadingColumnAlias(COL_PLACE_NUMBER))
        .addFields(TBL_ORDER_CARGO, COL_ORDER, COL_CARGO_DESCRIPTION, COL_CARGO_NOTES)
        .addField(defLoadAlias, COL_PLACE_DATE, defaultLoadingColumnAlias(COL_PLACE_DATE))
        .addField(defLoadAlias, COL_PLACE_COUNTRY, defaultLoadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(defLoadAlias, COL_PLACE_ADDRESS, defaultLoadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(defLoadAlias, COL_PLACE_POST_INDEX,
            defaultLoadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(defLoadAlias, COL_PLACE_CITY, defaultLoadingColumnAlias(COL_PLACE_CITY))
        .addField(defLoadAlias, COL_PLACE_NUMBER, defaultLoadingColumnAlias(COL_PLACE_NUMBER))
        .addField(defUnlAlias, COL_PLACE_DATE, defaultUnloadingColumnAlias(COL_PLACE_DATE))
        .addField(defUnlAlias, COL_PLACE_COUNTRY, defaultUnloadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(defUnlAlias, COL_PLACE_ADDRESS, defaultUnloadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(defUnlAlias, COL_PLACE_POST_INDEX,
            defaultUnloadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(defUnlAlias, COL_PLACE_CITY, defaultUnloadingColumnAlias(COL_PLACE_CITY))
        .addField(defUnlAlias, COL_PLACE_NUMBER, defaultUnloadingColumnAlias(COL_PLACE_NUMBER))
        .addFields(TBL_ORDERS, COL_ORDER_NO, COL_CUSTOMER, COL_STATUS)
        .addField(TBL_ORDERS, COL_ORDER_DATE, ALS_ORDER_DATE)
        .addField(TBL_COMPANIES, COL_COMPANY_NAME, COL_CUSTOMER_NAME)
        .setWhere(where)
        .addOrder(TBL_TRIPS, COL_TRIP_ID);
  }

  /**
   * Return SqlSelect query, calculating trip fuel consumptions from TripRoutes table.
   * 
   * @param flt - query filter with <b>unique</b> TripRoutes ID values.
   * @param routeMode - if true, returns results, grouped by TripRoutes ID, else grouped by Trip ID
   * @return query with two columns: (TripRoutes ID or "Trip") and "Quantity"
   */
  private SqlSelect getFuelConsumptionsQuery(SqlSelect flt, boolean routeMode) {
    String trips = TBL_TRIPS;
    String routes = TBL_TRIP_ROUTES;
    String fuel = VIEW_FUEL_CONSUMPTIONS;
    String temps = VIEW_FUEL_TEMPERATURES;
    String routeId = sys.getIdName(routes);

    IsExpression xpr = SqlUtils.round(
        SqlUtils.sqlIf(SqlUtils.isNull(routes, "Consumption"),
            SqlUtils.plus(
                SqlUtils.divide(
                    SqlUtils.plus(
                        SqlUtils.nvl(
                            SqlUtils.multiply(
                                SqlUtils.field(routes, "Kilometers"),
                                SqlUtils.sqlCase(SqlUtils.field(routes, "Season"), 0,
                                    SqlUtils.field(fuel, "Summer"),
                                    SqlUtils.field(fuel, "Winter")),
                                SqlUtils.plus(1,
                                    SqlUtils.divide(SqlUtils.nvl(SqlUtils.field(temps, "Rate"), 0),
                                        100))),
                            0),
                        SqlUtils.nvl(
                            SqlUtils.multiply(
                                SqlUtils.field(routes, "Kilometers"),
                                SqlUtils.field(routes, "CargoWeight"),
                                SqlUtils.field(fuel, "TonneKilometer")),
                            0)),
                    100),
                SqlUtils.nvl(SqlUtils.multiply(SqlUtils.field(routes, "MotoHours"),
                    SqlUtils.field(fuel, "MotoHour")), 0)),
            SqlUtils.field(routes, "Consumption")), 2);

    String alias = SqlUtils.uniqueName();

    return new SqlSelect()
        .addFields(routes, routeMode ? routeId : "Trip")
        .addSum(xpr, "Quantity")
        .addFrom(routes)
        .addFromInner(flt, alias, SqlUtils.joinUsing(routes, alias, routeId))
        .addFromInner(trips, sys.joinTables(trips, routes, "Trip"))
        .addFromInner(fuel, SqlUtils.joinUsing(trips, fuel, "Vehicle"))
        .addFromLeft(temps,
            SqlUtils.and(sys.joinTables(fuel, temps, "Consumption"),
                SqlUtils.joinUsing(temps, routes, "Season"),
                SqlUtils.or(SqlUtils.isNull(temps, "TempFrom"),
                    SqlUtils.lessEqual(SqlUtils.field(temps, "TempFrom"),
                        SqlUtils.nvl(SqlUtils.field(routes, "Temperature"), 0))),
                SqlUtils.or(SqlUtils.isNull(temps, "TempTo"),
                    SqlUtils.more(SqlUtils.field(temps, "TempTo"),
                        SqlUtils.nvl(SqlUtils.field(routes, "Temperature"), 0)))))
        .setWhere(SqlUtils.and(
            SqlUtils.or(SqlUtils.isNull(fuel, "DateFrom"),
                SqlUtils.joinLessEqual(fuel, "DateFrom", routes, "Date")),
            SqlUtils.or(SqlUtils.isNull(fuel, "DateTo"),
                SqlUtils.joinMore(fuel, "DateTo", routes, "Date"))))
        .addGroup(routes, routeMode ? routeId : "Trip");
  }

  private ResponseObject getFxData() {
    BeeRowSet settings = getSettings();
    if (settings == null) {
      return ResponseObject.error("user settings not available");
    }

    Long theme = settings.getLong(0, settings.getColumnIndex(COL_FX_THEME));
    List<Color> colors = getThemeColors(theme);

    settings.setTableProperty(PROP_COLORS, Codec.beeSerialize(colors));

    BeeRowSet countries = qs.getViewData(VIEW_COUNTRIES);
    settings.setTableProperty(PROP_COUNTRIES, countries.serialize());

    BeeRowSet cities = qs.getViewData(VIEW_CITIES, Filter.any(COL_COUNTRY, countries.getRowIds()));
    settings.setTableProperty(PROP_CITIES, cities.serialize());

    String loadAlias = "load_" + SqlUtils.uniqueName();
    String unlAlias = "unl_" + SqlUtils.uniqueName();

    String colPlaceId = sys.getIdName(TBL_CARGO_PLACES);

    SqlSelect query = new SqlSelect()
        .addFrom(TBL_ORDER_CARGO)
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, TBL_ORDERS, COL_CUSTOMER))
        .addFromLeft(TBL_CARGO_PLACES, loadAlias,
            SqlUtils.join(loadAlias, colPlaceId, TBL_ORDER_CARGO, COL_LOADING_PLACE))
        .addFromLeft(TBL_CARGO_PLACES, unlAlias,
            SqlUtils.join(unlAlias, colPlaceId, TBL_ORDER_CARGO, COL_UNLOADING_PLACE))
        .addFromLeft(TBL_CARGO_TRIPS,
            SqlUtils.join(TBL_CARGO_TRIPS, COL_CARGO, TBL_ORDER_CARGO, COL_CARGO_ID));

    query.addFields(TBL_ORDERS, COL_STATUS, COL_ORDER_DATE, COL_ORDER_NO, COL_CUSTOMER);
    query.addField(TBL_COMPANIES, COL_COMPANY_NAME, COL_CUSTOMER_NAME);

    query.addFields(TBL_ORDER_CARGO, COL_ORDER, COL_CARGO_ID, COL_CARGO_DESCRIPTION,
        COL_CARGO_NOTES);

    query.addField(loadAlias, COL_PLACE_DATE, loadingColumnAlias(COL_PLACE_DATE));
    query.addField(loadAlias, COL_PLACE_COUNTRY, loadingColumnAlias(COL_PLACE_COUNTRY));
    query.addField(loadAlias, COL_PLACE_ADDRESS, loadingColumnAlias(COL_PLACE_ADDRESS));
    query.addField(loadAlias, COL_PLACE_POST_INDEX, loadingColumnAlias(COL_PLACE_POST_INDEX));
    query.addField(loadAlias, COL_PLACE_CITY, loadingColumnAlias(COL_PLACE_CITY));
    query.addField(loadAlias, COL_PLACE_NUMBER, loadingColumnAlias(COL_PLACE_NUMBER));

    query.addField(unlAlias, COL_PLACE_DATE, unloadingColumnAlias(COL_PLACE_DATE));
    query.addField(unlAlias, COL_PLACE_COUNTRY, unloadingColumnAlias(COL_PLACE_COUNTRY));
    query.addField(unlAlias, COL_PLACE_ADDRESS, unloadingColumnAlias(COL_PLACE_ADDRESS));
    query.addField(unlAlias, COL_PLACE_POST_INDEX, unloadingColumnAlias(COL_PLACE_POST_INDEX));
    query.addField(unlAlias, COL_PLACE_CITY, unloadingColumnAlias(COL_PLACE_CITY));
    query.addField(unlAlias, COL_PLACE_NUMBER, unloadingColumnAlias(COL_PLACE_NUMBER));

    Set<Integer> statuses = Sets.newHashSet(OrderStatus.REQUEST.ordinal(),
        OrderStatus.ACTIVE.ordinal());
    IsCondition cargoWhere = SqlUtils.and(SqlUtils.inList(TBL_ORDERS, COL_STATUS, statuses),
        SqlUtils.isNull(TBL_CARGO_TRIPS, COL_CARGO));

    query.setWhere(cargoWhere);

    query.addOrder(TBL_COMPANIES, COL_COMPANY_NAME);
    query.addOrder(TBL_ORDERS, COL_ORDER_DATE, COL_ORDER_NO);
    query.addOrder(loadAlias, COL_PLACE_DATE);
    query.addOrder(unlAlias, COL_PLACE_DATE);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_ORDER_CARGO, data.serialize());

    IsCondition cargoHandlingWhere = SqlUtils.or(SqlUtils.notNull(loadAlias, COL_PLACE_DATE),
        SqlUtils.notNull(unlAlias, COL_PLACE_DATE));

    SqlSelect cargoHandlingQuery = new SqlSelect()
        .addFrom(TBL_ORDER_CARGO)
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_CARGO_TRIPS,
            SqlUtils.join(TBL_CARGO_TRIPS, COL_CARGO, TBL_ORDER_CARGO, COL_CARGO_ID))
        .addFromInner(TBL_CARGO_HANDLING,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_HANDLING, COL_CARGO))
        .addFromLeft(TBL_CARGO_PLACES, loadAlias,
            SqlUtils.join(loadAlias, colPlaceId, TBL_CARGO_HANDLING, COL_LOADING_PLACE))
        .addFromLeft(TBL_CARGO_PLACES, unlAlias,
            SqlUtils.join(unlAlias, colPlaceId, TBL_CARGO_HANDLING, COL_UNLOADING_PLACE))
        .addFields(TBL_CARGO_HANDLING, COL_CARGO, COL_CARGO_HANDLING_NOTES)
        .addField(loadAlias, COL_PLACE_DATE, loadingColumnAlias(COL_PLACE_DATE))
        .addField(loadAlias, COL_PLACE_COUNTRY, loadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(loadAlias, COL_PLACE_ADDRESS, loadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(loadAlias, COL_PLACE_POST_INDEX, loadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(loadAlias, COL_PLACE_CITY, loadingColumnAlias(COL_PLACE_CITY))
        .addField(loadAlias, COL_PLACE_NUMBER, loadingColumnAlias(COL_PLACE_NUMBER))
        .addField(unlAlias, COL_PLACE_DATE, unloadingColumnAlias(COL_PLACE_DATE))
        .addField(unlAlias, COL_PLACE_COUNTRY, unloadingColumnAlias(COL_PLACE_COUNTRY))
        .addField(unlAlias, COL_PLACE_ADDRESS, unloadingColumnAlias(COL_PLACE_ADDRESS))
        .addField(unlAlias, COL_PLACE_POST_INDEX, unloadingColumnAlias(COL_PLACE_POST_INDEX))
        .addField(unlAlias, COL_PLACE_CITY, unloadingColumnAlias(COL_PLACE_CITY))
        .addField(unlAlias, COL_PLACE_NUMBER, unloadingColumnAlias(COL_PLACE_NUMBER))
        .setWhere(SqlUtils.and(cargoWhere, cargoHandlingWhere))
        .addOrder(TBL_CARGO_HANDLING, COL_CARGO);

    SimpleRowSet cargoHandling = qs.getData(cargoHandlingQuery);
    if (!DataUtils.isEmpty(cargoHandling)) {
      settings.setTableProperty(PROP_CARGO_HANDLING, cargoHandling.serialize());
    }

    return ResponseObject.response(settings);
  }

  private ResponseObject getImportMappings(long propId, String tbl, String fld) {
    BeeRowSet rs = qs.getViewData(new SqlSelect()
        .addFields(TBL_IMPORT_MAPPINGS, COL_IMPORT_PROPERTY, COL_IMPORT_VALUE, COL_IMPORT_MAPPING)
        .addField(tbl, fld, COL_IMPORT_MAPPING + COL_IMPORT_VALUE)
        .addFields(TBL_IMPORT_MAPPINGS,
            sys.getIdName(TBL_IMPORT_MAPPINGS), sys.getVersionName(TBL_IMPORT_MAPPINGS))
        .addFrom(TBL_IMPORT_MAPPINGS)
        .addFromLeft(tbl, sys.joinTables(tbl, TBL_IMPORT_MAPPINGS, COL_IMPORT_MAPPING))
        .setWhere(SqlUtils.equals(TBL_IMPORT_MAPPINGS, COL_IMPORT_PROPERTY, propId))
        .addOrder(TBL_IMPORT_MAPPINGS, COL_IMPORT_VALUE), sys.getView(TBL_IMPORT_MAPPINGS));

    return ResponseObject.response(rs);
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

  private List<Color> getThemeColors(Long theme) {
    List<Color> result = Lists.newArrayList();

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

  private ResponseObject getTripBeforeData(long vehicle, long date) {
    String[] resp = new String[2];

    if (date != 0) {
      String trips = TBL_TRIPS;
      String routes = TBL_TRIP_ROUTES;
      String fuels = TBL_TRIP_FUEL_COSTS;
      String consumptions = TBL_TRIP_FUEL_CONSUMPTIONS;
      String tripId = sys.getIdName(trips);

      SimpleRowSet rs = qs.getData(new SqlSelect()
          .addFields(trips,
              tripId, "SpeedometerBefore", "SpeedometerAfter", "FuelBefore", "FuelAfter")
          .addFrom(trips)
          .setWhere(SqlUtils.and(SqlUtils.equals(trips, "Vehicle", vehicle),
              SqlUtils.less(trips, "Date", date))));

      int cnt = rs.getNumberOfRows();

      if (cnt > 0) {
        cnt--;
        Double speedometer = rs.getDouble(cnt, "SpeedometerAfter");
        Double fuel = rs.getDouble(cnt, "FuelAfter");

        if (speedometer == null) {
          Double km = qs.getDouble(new SqlSelect()
              .addSum(routes, "Kilometers")
              .addFrom(routes)
              .setWhere(SqlUtils.equals(routes, "Trip", rs.getLong(cnt, tripId))));

          speedometer = BeeUtils.unbox(rs.getDouble(cnt, "SpeedometerBefore"))
              + BeeUtils.unbox(km);

          Integer scale = BeeUtils.toIntOrNull(qs.sqlValue(VIEW_VEHICLES, "Speedometer", vehicle));

          if (BeeUtils.isPositive(scale) && scale < speedometer) {
            speedometer -= scale;
          }
        }
        if (fuel == null) {
          Double fill = qs.getDouble(new SqlSelect()
              .addSum(fuels, "Quantity")
              .addFrom(fuels)
              .setWhere(SqlUtils.equals(fuels, "Trip", rs.getLong(cnt, tripId))));

          SimpleRow row = qs.getRow(getFuelConsumptionsQuery(new SqlSelect()
              .addFields(routes, sys.getIdName(routes))
              .addFrom(routes)
              .setWhere(SqlUtils.equals(routes, "Trip", rs.getLong(cnt, tripId))), false));

          Double consume = row == null ? null : row.getDouble("Quantity");

          Double addit = qs.getDouble(new SqlSelect()
              .addSum(consumptions, "Quantity")
              .addFrom(consumptions)
              .setWhere(SqlUtils.equals(consumptions, "Trip", rs.getLong(cnt, tripId))));

          fuel = BeeUtils.unbox(rs.getDouble(cnt, "FuelBefore")) + BeeUtils.unbox(fill)
              - BeeUtils.unbox(consume) - BeeUtils.unbox(addit);
        }
        resp[0] = BeeUtils.toString(speedometer);
        resp[1] = BeeUtils.toString(fuel);
      }
    }
    return ResponseObject.response(resp);
  }

  /**
   * Return Temporary table name with calculated trip costs.
   * 
   * @param flt - query filter with <b>unique</b> "Trip" values.
   * @return Temporary table name with following structure: <br>
   *         "Trip" - trip ID <br>
   *         "TripCost" - total trip cost <br>
   *         "FuelCost" - total trip fuel cost considering remainder corrections
   */
  private String getTripCost(SqlSelect flt) {
    String trips = TBL_TRIPS;
    String costs = TBL_TRIP_COSTS;
    String fuels = TBL_TRIP_FUEL_COSTS;
    String routes = TBL_TRIP_ROUTES;
    String consumptions = TBL_TRIP_FUEL_CONSUMPTIONS;
    String tripNativeId = sys.getIdName(trips);
    String alias = SqlUtils.uniqueName();

    // Trip costs
    SqlSelect ss = new SqlSelect()
        .addField(trips, tripNativeId, COL_TRIP)
        .addField(trips, "Date", "TripDate")
        .addFields(trips, "Vehicle", "FuelBefore", "FuelAfter")
        .addEmptyDouble("FuelCost")
        .addFrom(trips)
        .addFromInner(flt, alias, sys.joinTables(trips, alias, COL_TRIP))
        .addFromLeft(costs, sys.joinTables(trips, costs, COL_TRIP))
        .addGroup(trips, tripNativeId, "Date", "Vehicle", "FuelBefore", "FuelAfter");

    ss.addSum(ExchangeUtils.exchangeField(ss, TradeModuleBean.getTotalExpression(costs),
        SqlUtils.field(costs, "Currency"), SqlUtils.field(costs, "Date")), "TripCost");

    String tmpCosts = qs.sqlCreateTemp(ss);
    qs.sqlIndex(tmpCosts, COL_TRIP);

    // Fuel costs
    ss = new SqlSelect()
        .addFields(tmpCosts, COL_TRIP)
        .addSum(fuels, "Quantity")
        .addFrom(tmpCosts)
        .addFromLeft(fuels, SqlUtils.joinUsing(tmpCosts, fuels, COL_TRIP))
        .addGroup(tmpCosts, COL_TRIP);

    ss.addSum(ExchangeUtils.exchangeField(ss, TradeModuleBean.getTotalExpression(fuels),
        SqlUtils.field(fuels, "Currency"), SqlUtils.field(fuels, "Date")), "FuelCost");

    String tmp = qs.sqlCreateTemp(ss);
    qs.sqlIndex(tmp, COL_TRIP);

    qs.updateData(new SqlUpdate(tmpCosts)
        .setFrom(tmp, SqlUtils.joinUsing(tmpCosts, tmp, COL_TRIP))
        .addExpression("FuelCost", SqlUtils.field(tmp, "FuelCost")));

    // Fuel consumptions
    if (qs.sqlExists(tmpCosts, SqlUtils.isNull(tmpCosts, "FuelAfter"))) {
      ss = new SqlSelect()
          .addFields(routes, sys.getIdName(routes))
          .addFrom(routes)
          .addFromInner(tmpCosts, SqlUtils.joinUsing(routes, tmpCosts, COL_TRIP));

      String tmpRoutes = qs.sqlCreateTemp(getFuelConsumptionsQuery(ss, false));
      qs.sqlIndex(tmpRoutes, COL_TRIP);

      String tmpConsumptions = qs.sqlCreateTemp(new SqlSelect()
          .addFields(consumptions, COL_TRIP)
          .addSum(consumptions, "Quantity")
          .addFrom(consumptions)
          .addFromInner(tmpCosts, SqlUtils.joinUsing(consumptions, tmpCosts, COL_TRIP))
          .addGroup(consumptions, COL_TRIP));

      qs.sqlIndex(tmpConsumptions, COL_TRIP);

      qs.updateData(new SqlUpdate(tmpCosts)
          .setFrom(new SqlSelect()
              .addFields(tmp, COL_TRIP, "Quantity")
              .addField(tmpRoutes, "Quantity", "routeQuantity")
              .addField(tmpConsumptions, "Quantity", "consumeQuantity")
              .addFrom(tmp)
              .addFromLeft(tmpRoutes, SqlUtils.joinUsing(tmp, tmpRoutes, COL_TRIP))
              .addFromLeft(tmpConsumptions, SqlUtils.joinUsing(tmp, tmpConsumptions, COL_TRIP)),
              "sub", SqlUtils.joinUsing(tmpCosts, "sub", COL_TRIP))
          .addExpression("FuelAfter", SqlUtils.minus(
              SqlUtils.plus(
                  SqlUtils.nvl(SqlUtils.field(tmpCosts, "FuelBefore"), 0),
                  SqlUtils.nvl(SqlUtils.field("sub", "Quantity"), 0)),
              SqlUtils.nvl(SqlUtils.field("sub", "routeQuantity"), 0),
              SqlUtils.nvl(SqlUtils.field("sub", "consumeQuantity"), 0)))
          .setWhere(SqlUtils.isNull(tmpCosts, "FuelAfter")));

      qs.sqlDropTemp(tmpRoutes);
      qs.sqlDropTemp(tmpConsumptions);
    }
    qs.sqlDropTemp(tmp);

    // Fuel cost correction
    ss = new SqlSelect()
        .addFields(trips, "Vehicle")
        .addField(trips, "Date", "TripDate")
        .addFields(fuels, "Date")
        .addSum(fuels, "Quantity")
        .addFrom(trips)
        .addFromInner(fuels, SqlUtils.join(trips, tripNativeId, fuels, COL_TRIP))
        .addFromInner(new SqlSelect()
            .addFields(trips, "Vehicle")
            .addMax(trips, "Date", "MaxDate")
            .addFrom(trips)
            .addFromInner(tmpCosts, SqlUtils.join(trips, tripNativeId, tmpCosts, COL_TRIP))
            .addGroup(trips, "Vehicle"), "sub",
            SqlUtils.and(SqlUtils.joinUsing(trips, "sub", "Vehicle"),
                SqlUtils.joinLessEqual(trips, "Date", "sub", "MaxDate"),
                SqlUtils.and(SqlUtils.positive(fuels, "Quantity"),
                    SqlUtils.positive(fuels, "Price"))))
        .addGroup(trips, "Vehicle", "Date")
        .addGroup(fuels, "Date");

    ss.addSum(ExchangeUtils.exchangeField(ss, TradeModuleBean.getTotalExpression(fuels),
        SqlUtils.field(fuels, "Currency"), SqlUtils.field(fuels, "Date")), "Sum");

    String tmpFuels = qs.sqlCreateTemp(ss);
    qs.sqlIndex(tmpFuels, "Vehicle");

    for (int i = 0; i < 2; i++) {
      boolean plusMode = i == 0;
      String fld = plusMode ? "FuelBefore" : "FuelAfter";

      tmp = qs.sqlCreateTemp(new SqlSelect()
          .addFields(tmpCosts, COL_TRIP, "TripDate", "Vehicle")
          .addField(tmpCosts, fld, "Remainder")
          .addEmptyDate("Date")
          .addEmptyDouble("Cost")
          .addFrom(tmpCosts)
          .setWhere(SqlUtils.positive(tmpCosts, fld)));

      qs.sqlIndex(tmp, COL_TRIP, "Vehicle");
      int c = 0;

      IsCondition cond = plusMode
          ? SqlUtils.joinLess(tmpFuels, "TripDate", tmp, "TripDate")
          : SqlUtils.joinLessEqual(tmpFuels, "TripDate", tmp, "TripDate");

      do {
        String tmp2 = qs.sqlCreateTemp(new SqlSelect()
            .addFields(tmp, "Vehicle", "TripDate")
            .addMax(tmpFuels, "Date")
            .addFrom(tmp)
            .addFromInner(tmpFuels, SqlUtils.joinUsing(tmp, tmpFuels, "Vehicle"))
            .setWhere(SqlUtils.and(cond,
                SqlUtils.or(SqlUtils.isNull(tmp, "Date"),
                    SqlUtils.joinLess(tmpFuels, "Date", tmp, "Date")),
                SqlUtils.positive(tmp, "Remainder")))
            .addGroup(tmp, "Vehicle", "TripDate"));

        qs.sqlIndex(tmp2, "Vehicle");

        c = qs.updateData(new SqlUpdate(tmp)
            .setFrom(new SqlSelect()
                .addFields(tmp2, "Vehicle", "TripDate", "Date")
                .addFields(tmpFuels, "Quantity", "Sum")
                .addFrom(tmp2)
                .addFromInner(tmpFuels, SqlUtils.joinUsing(tmp2, tmpFuels, "Vehicle", "Date")),
                "sub", SqlUtils.joinUsing(tmp, "sub", "Vehicle", "TripDate"))
            .addExpression("Date", SqlUtils.field("sub", "Date"))
            .addExpression("Cost", SqlUtils.plus(SqlUtils.nvl(SqlUtils.field(tmp, "Cost"), 0),
                SqlUtils.sqlIf(SqlUtils.joinLess(tmp, "Remainder", "sub", "Quantity"),
                    SqlUtils.multiply(SqlUtils.field(tmp, "Remainder"),
                        SqlUtils.divide((Object[]) SqlUtils.fields("sub", "Sum", "Quantity"))),
                    SqlUtils.field("sub", "Sum"))))
            .addExpression("Remainder", SqlUtils.minus(SqlUtils.field(tmp, "Remainder"),
                SqlUtils.field("sub", "Quantity")))
            .setWhere(SqlUtils.positive(tmp, "Remainder")));

        qs.sqlDropTemp(tmp2);

      } while (BeeUtils.isPositive(c));

      IsExpression expr = plusMode
          ? SqlUtils.plus(SqlUtils.nvl(SqlUtils.field(tmpCosts, "FuelCost"), 0),
              SqlUtils.nvl(SqlUtils.field(tmp, "Cost"), 0))
          : SqlUtils.minus(SqlUtils.nvl(SqlUtils.field(tmpCosts, "FuelCost"), 0),
              SqlUtils.nvl(SqlUtils.field(tmp, "Cost"), 0));

      qs.updateData(new SqlUpdate(tmpCosts)
          .setFrom(tmp, SqlUtils.joinUsing(tmpCosts, tmp, COL_TRIP))
          .addExpression("FuelCost", expr));

      qs.sqlDropTemp(tmp);
    }
    qs.sqlDropTemp(tmpFuels);

    return tmpCosts;
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

  /**
   * Return Temporary table name with calculated trip incomes by each cargo.
   * 
   * @param flt - query filter with <b>unique</b> "Trip" values.
   * @return Temporary table name with following structure: <br>
   *         "Trip" - trip ID <br>
   *         "Cargo" - cargo ID <br>
   *         "TripIncome" - total trip income <br>
   */
  private String getTripIncome(SqlSelect flt) {
    String cargoTrips = sys.getViewSource(VIEW_CARGO_TRIPS);
    String alias = SqlUtils.uniqueName();

    String tmp = qs.sqlCreateTemp(getCargoIncomeQuery(new SqlSelect()
        .setDistinctMode(true)
        .addFields(cargoTrips, COL_CARGO)
        .addFrom(cargoTrips)
        .addFromInner(flt, alias, SqlUtils.joinUsing(cargoTrips, alias, COL_TRIP)), null));

    qs.sqlIndex(tmp, COL_CARGO);

    String tmp2 = qs.sqlCreateTemp(new SqlSelect()
        .addFields(tmp, COL_CARGO, "CargoIncome")
        .addSum(cargoTrips, "TripPercent", "TotalPercent")
        .addSum(SqlUtils.sqlIf(SqlUtils.isNull(cargoTrips, "TripPercent"), 1, 0), "CntEmpty")
        .addFrom(tmp)
        .addFromInner(cargoTrips, SqlUtils.joinUsing(tmp, cargoTrips, COL_CARGO))
        .addGroup(tmp, COL_CARGO, "CargoIncome"));

    qs.sqlIndex(tmp2, COL_CARGO);
    qs.sqlDropTemp(tmp);

    IsExpression xpr = SqlUtils.multiply(
        SqlUtils.divide(SqlUtils.field(tmp2, "CargoIncome"), 100),
        SqlUtils.sqlIf(SqlUtils.isNull(cargoTrips, "TripPercent"),
            SqlUtils.divide(
                SqlUtils.minus(100, SqlUtils.nvl(SqlUtils.field(tmp2, "TotalPercent"), 0)),
                SqlUtils.field(tmp2, "CntEmpty")),
            SqlUtils.field(cargoTrips, "TripPercent")));

    tmp = qs.sqlCreateTemp(new SqlSelect()
        .addFields(cargoTrips, COL_CARGO, COL_TRIP)
        .addExpr(xpr, "TripIncome")
        .addFrom(tmp2)
        .addFromInner(cargoTrips, SqlUtils.joinUsing(tmp2, cargoTrips, COL_CARGO))
        .addFromInner(flt, alias, SqlUtils.joinUsing(cargoTrips, alias, COL_TRIP)));

    qs.sqlIndex(tmp, COL_CARGO, COL_TRIP);
    qs.sqlDropTemp(tmp2);

    return tmp;
  }

  private ResponseObject getTripProfit(long tripId) {
    String crs = getTripCost(new SqlSelect().addConstant(tripId, "Trip"));

    SqlSelect ss = new SqlSelect()
        .addSum(crs, "TripCost")
        .addSum(crs, "FuelCost")
        .addFrom(crs);

    SimpleRow res = qs.getRow(ss);

    qs.sqlDropTemp(crs);

    crs = getTripIncome(new SqlSelect().addConstant(tripId, "Trip"));

    ss = new SqlSelect()
        .addSum(crs, "TripIncome")
        .addFrom(crs);

    String tripIncome = qs.getValue(ss);

    qs.sqlDropTemp(crs);

    return ResponseObject.response(new String[] {"TripCost:", res.getValue("TripCost"),
        "FuelCost:", res.getValue("FuelCost"), "TripIncome:", tripIncome});
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

  private SimpleRowSet getVehicleServices(IsCondition condition) {
    SqlSelect query =
        new SqlSelect()
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
      VehicleType vehicleType, String themeColumnName) {

    BeeRowSet settings = getSettings();
    if (settings == null) {
      return ResponseObject.error("user settings not available");
    }

    Long theme = settings.getLong(0, settings.getColumnIndex(themeColumnName));
    List<Color> colors = getThemeColors(theme);
    settings.setTableProperty(PROP_COLORS, Codec.beeSerialize(colors));

    BeeRowSet countries = qs.getViewData(VIEW_COUNTRIES);
    settings.setTableProperty(PROP_COUNTRIES, countries.serialize());

    BeeRowSet cities = qs.getViewData(VIEW_CITIES, Filter.any(COL_COUNTRY, countries.getRowIds()));
    settings.setTableProperty(PROP_CITIES, cities.serialize());

    Order vehicleOrder = new Order(COL_NUMBER, true);
    BeeRowSet vehicles = qs.getViewData(VIEW_VEHICLES, vehicleFilter, vehicleOrder);
    if (DataUtils.isEmpty(vehicles)) {
      logger.warning(svc, "vehicles not available");
      return ResponseObject.response(settings);
    }
    settings.setTableProperty(PROP_VEHICLES, vehicles.serialize());

    List<Long> vehicleIds = DataUtils.getRowIds(vehicles);

    SimpleRowSet vehicleServices = getVehicleServices(SqlUtils.inList(TBL_VEHICLE_SERVICES,
        COL_VEHICLE, vehicleIds));
    if (!DataUtils.isEmpty(vehicleServices)) {
      settings.setTableProperty(PROP_VEHICLE_SERVICES, vehicleServices.serialize());
    }

    IsCondition tripWhere = tripCondition(SqlUtils.inList(TBL_TRIPS,
        vehicleType.getTripVehicleIdColumnName(), vehicleIds));

    SqlSelect tripQuery = getTripQuery(tripWhere);
    tripQuery.addOrder(TBL_TRIPS, vehicleType.getTripVehicleIdColumnName(), COL_TRIP_DATE);

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

    SqlSelect cargoHandlingQuery = getFreightHandlingQuery(tripWhere);

    SimpleRowSet cargoHandling = qs.getData(cargoHandlingQuery);
    if (!DataUtils.isEmpty(cargoHandling)) {
      settings.setTableProperty(PROP_CARGO_HANDLING, cargoHandling.serialize());
    }

    return ResponseObject.response(settings);
  }

  @Timeout
  private void importERPPayments() {
    SimpleRowSet debts = qs.getData(new SqlSelect()
        .addField(TBL_SALES, sys.getIdName(TBL_SALES), COL_SALE)
        .addFields(TBL_SALES, COL_TRADE_PAID)
        .addFrom(TBL_SALES)
        .setWhere(SqlUtils.and(SqlUtils.isNull(TBL_SALES, COL_SALE_PROFORMA),
            SqlUtils.or(SqlUtils.isNull(TBL_SALES, COL_TRADE_PAID),
                SqlUtils.less(TBL_SALES, COL_TRADE_PAID,
                    SqlUtils.field(TBL_SALES, COL_TRADE_AMOUNT))))));

    if (debts.isEmpty()) {
      return;
    }
    StringBuilder ids = new StringBuilder();

    for (SimpleRow row : debts) {
      if (ids.length() > 0) {
        ids.append(",");
      }
      ids.append("'").append(row.getValue(COL_SALE)).append("'");
    }
    String remoteNamespace = prm.getText(PRM_ERP_NAMESPACE);
    String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
    String remoteLogin = prm.getText(PRM_ERP_LOGIN);
    String remotePassword = prm.getText(PRM_ERP_PASSWORD);

    try {
      SimpleRowSet payments = ButentWS.connect(remoteNamespace, remoteAddress, remoteLogin,
          remotePassword)
          .getSQLData("SELECT extern_id AS id, apm_data AS data, apm_suma AS suma"
              + " FROM apyvarta WHERE extern_id IN(" + ids.toString() + ")",
              new String[] {"id", "data", "suma"});

      for (SimpleRow payment : payments) {
        if (!Objects.equal(payment.getDouble("suma"),
            BeeUtils.toDoubleOrNull(debts.getValueByKey(COL_SALE, payment.getValue("id"),
                COL_TRADE_PAID)))) {

          qs.updateData(new SqlUpdate(TBL_SALES)
              .addConstant(COL_TRADE_PAID, payment.getDouble("suma"))
              .addConstant(COL_TRADE_PAYMENT_TIME,
                  TimeUtils.parseDateTime(payment.getValue("data")))
              .setWhere(sys.idEquals(TBL_SALES, payment.getLong("id"))));
        }
      }
    } catch (BeeException e) {
      logger.error(e);
    }
  }

  private ResponseObject sendMessage(String message, String[] recipients) {
    String address = prm.getText("SmsServiceAddress");

    if (BeeUtils.isEmpty(address)) {
      return ResponseObject.error("SmsServiceAddress is empty");
    }
    StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")
        .append("<sms-send>")
        .append("<authentication>")
        .append(XmlUtils.tag("username", prm.getText("SmsUserName")))
        .append(XmlUtils.tag("password", prm.getText("SmsPassword")))
        .append(XmlUtils.tag("serviceId", prm.getText("SmsServiceId")))
        .append("</authentication>")
        .append("<originator>")
        .append(XmlUtils.tag("source", prm.getText("SmsDisplayText")))
        .append("</originator>")
        .append("<sms-messages>");

    for (String phone : recipients) {
      xml.append("<sms>")
          .append(XmlUtils.tag("destination", phone))
          .append(XmlUtils.tag("msg", message))
          .append(XmlUtils.tag("dr", true))
          .append(XmlUtils.tag("id", 0))
          .append(XmlUtils.tag("sendTime", new DateTime().toString()))
          .append("</sms>");
    }
    xml.append("</sms-messages>")
        .append("</sms-send>");

    ResponseObject response = ResponseObject.info(Localized.getConstants().messageSent());
    BufferedWriter wr = null;
    BufferedReader in = null;

    try {
      URL url = new URL(address);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();

      conn.setRequestMethod("POST");
      conn.setDoOutput(true);

      wr = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(),
          BeeConst.CHARSET_UTF8));
      wr.write(xml.toString());
      wr.close();

      if (conn.getResponseCode() != HttpServletResponse.SC_OK) {
        response = ResponseObject.error(Localized.getConstants().error(), conn.getResponseCode(),
            conn.getResponseMessage());
      } else {
        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String input;

        while ((input = in.readLine()) != null) {
          sb.append(input);
        }
        in.close();
        input = sb.toString();
        String status = XmlUtils.getText(input, "status");

        if (!BeeUtils.same(status, "OK")) {
          response = ResponseObject.error(input);
        }
      }
    } catch (IOException e) {
      try {
        if (wr != null) {
          wr.close();
        }
        if (in != null) {
          in.close();
        }
      } catch (IOException ex) {
        logger.error(ex);
      }
      response = ResponseObject.error(e);
    }
    return response;
  }

  private ResponseObject sendToERP(String viewName, Set<Long> ids) {
    if (!sys.isView(viewName)) {
      return ResponseObject.error("Wrong view name");
    }
    String trade = sys.getView(viewName).getSourceName();
    String tradeItems;
    String itemsRelation;

    SqlSelect query = new SqlSelect()
        .addFields(trade, COL_TRADE_DATE, COL_TRADE_INVOICE_PREFIX, COL_TRADE_INVOICE_NO,
            COL_TRADE_NUMBER, COL_TRADE_TERM, COL_TRADE_SUPPLIER, COL_TRADE_CUSTOMER)
        .addField(TBL_CURRENCIES, COL_CURRENCY_NAME, COL_CURRENCY)
        .addField(COL_TRADE_WAREHOUSE_FROM, COL_WAREHOUSE_CODE, COL_TRADE_WAREHOUSE_FROM)
        .addFrom(trade)
        .addFromLeft(TBL_CURRENCIES, sys.joinTables(TBL_CURRENCIES, trade, COL_CURRENCY))
        .addFromLeft(TBL_WAREHOUSES, COL_TRADE_WAREHOUSE_FROM,
            sys.joinTables(TBL_WAREHOUSES, COL_TRADE_WAREHOUSE_FROM, trade,
                COL_TRADE_WAREHOUSE_FROM))
        .setWhere(sys.idInList(trade, ids));

    if (BeeUtils.same(trade, TBL_SALES)) {
      tradeItems = TBL_SALE_ITEMS;
      itemsRelation = COL_SALE;
      query.addFields(trade, COL_SALE_PAYER);

    } else if (BeeUtils.same(trade, TBL_PURCHASES)) {
      tradeItems = TBL_PURCHASE_ITEMS;
      itemsRelation = COL_PURCHASE;
      query.addField(COL_PURCHASE_WAREHOUSE_TO, COL_WAREHOUSE_CODE, COL_PURCHASE_WAREHOUSE_TO)
          .addFromLeft(TBL_WAREHOUSES, COL_PURCHASE_WAREHOUSE_TO,
              sys.joinTables(TBL_WAREHOUSES, COL_PURCHASE_WAREHOUSE_TO, trade,
                  COL_PURCHASE_WAREHOUSE_TO));
    } else {
      return ResponseObject.error("View source not supported:", trade);
    }
    String remoteNamespace = prm.getText(PRM_ERP_NAMESPACE);
    String remoteAddress = prm.getText(PRM_ERP_ADDRESS);
    String remoteLogin = prm.getText(PRM_ERP_LOGIN);
    String remotePassword = prm.getText(PRM_ERP_PASSWORD);

    SimpleRowSet invoices = qs.getData(query.addField(trade, sys.getIdName(trade), itemsRelation));

    Map<Long, String> companies = Maps.newHashMap();
    ResponseObject response = ResponseObject.emptyResponse();

    for (SimpleRow invoice : invoices) {
      for (String col : new String[] {COL_TRADE_SUPPLIER, COL_TRADE_CUSTOMER, COL_SALE_PAYER}) {
        Long id = invoices.hasColumn(col) ? invoice.getLong(col) : null;

        if (DataUtils.isId(id) && !companies.containsKey(id)) {
          SimpleRow data = qs.getRow(new SqlSelect()
              .addFields(TBL_COMPANIES, COL_COMPANY_NAME, COL_COMPANY_CODE, COL_COMPANY_VAT_CODE)
              .addFields(TBL_CONTACTS, COL_ADDRESS, COL_POST_INDEX)
              .addField(TBL_CITIES, COL_CITY_NAME, COL_CITY)
              .addField(TBL_COUNTRIES, COL_COUNTRY_NAME, COL_COUNTRY)
              .addFrom(TBL_COMPANIES)
              .addFromLeft(TBL_CONTACTS, sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT))
              .addFromLeft(TBL_CITIES, sys.joinTables(TBL_CITIES, TBL_CONTACTS, COL_CITY))
              .addFromLeft(TBL_COUNTRIES, sys.joinTables(TBL_COUNTRIES, TBL_CONTACTS, COL_COUNTRY))
              .setWhere(sys.idEquals(TBL_COMPANIES, id)));

          try {
            String company = ButentWS.connect(remoteNamespace, remoteAddress, remoteLogin,
                remotePassword)
                .importClient(data.getValue(COL_COMPANY_NAME), data.getValue(COL_COMPANY_CODE),
                    data.getValue(COL_COMPANY_VAT_CODE), data.getValue(COL_ADDRESS),
                    data.getValue(COL_POST_INDEX), data.getValue(COL_CITY),
                    data.getValue(COL_COUNTRY));

            companies.put(id, company);

          } catch (BeeException e) {
            response.addError(e);
          }
        }
      }
      if (response.hasErrors()) {
        break;
      }
      String operation;
      String warehouse;
      String client;

      if (invoices.hasColumn(COL_PURCHASE_WAREHOUSE_TO)) {
        operation = prm.getText("ERPCreditOperation");
        warehouse = invoice.getValue(COL_PURCHASE_WAREHOUSE_TO);
        client = companies.get(invoice.getLong(COL_TRADE_SUPPLIER));
      } else {
        operation = prm.getText("ERPOperation");
        warehouse = prm.getText("ERPWarehouse");
        client = companies.get(invoice.getLong(COL_TRADE_CUSTOMER));
      }
      WSDocument doc = new WSDocument(invoice.getValue(itemsRelation),
          invoice.getDateTime(COL_TRADE_DATE), operation, client, warehouse);

      if (invoices.hasColumn(COL_SALE_PAYER)) {
        doc.setPayer(companies.get(invoice.getLong(COL_SALE_PAYER)));
      }
      doc.setNumber(invoice.getValue(COL_TRADE_NUMBER));
      doc.setInvoice(invoice.getValue(COL_TRADE_INVOICE_PREFIX),
          invoice.getValue(COL_TRADE_INVOICE_NO));
      doc.setSupplier(companies.get(invoice.getLong(COL_TRADE_SUPPLIER)));
      doc.setCustomer(companies.get(invoice.getLong(COL_TRADE_CUSTOMER)));
      doc.setTerm(invoice.getDate(COL_TRADE_TERM));
      doc.setCurrency(invoice.getValue(COL_CURRENCY));

      SimpleRowSet items = qs.getData(new SqlSelect()
          .addFields(TBL_ITEMS, COL_ITEM_NAME, COL_ITEM_EXTERNAL_CODE)
          .addFields(tradeItems, COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
              COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC, COL_TRADE_ITEM_NOTE)
          .addFrom(tradeItems)
          .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, tradeItems, COL_ITEM))
          .setWhere(SqlUtils.equals(tradeItems, itemsRelation, invoice.getLong(itemsRelation))));

      for (SimpleRow item : items) {
        if (BeeUtils.isEmpty(item.getValue(COL_ITEM_EXTERNAL_CODE))) {
          response.addError("Item", BeeUtils.bracket(item.getValue(COL_ITEM_NAME)),
              "does not have ERP code");
          break;
        }
        WSDocumentItem wsItem = doc.addItem(item.getValue(COL_ITEM_EXTERNAL_CODE),
            item.getValue(COL_TRADE_ITEM_QUANTITY));

        wsItem.setPrice(item.getValue(COL_TRADE_ITEM_PRICE));
        wsItem.setVat(item.getValue(COL_TRADE_VAT), item.getBoolean(COL_TRADE_VAT_PERC),
            item.getBoolean(COL_TRADE_VAT_PLUS));
        wsItem.setNote(item.getValue(COL_TRADE_ITEM_NOTE));
      }
      if (response.hasErrors()) {
        break;
      }
      try {
        ButentWS.connect(remoteNamespace, remoteAddress, remoteLogin, remotePassword)
            .importDoc(doc);
      } catch (BeeException e) {
        response.addError(e);
        break;
      }
      qs.updateData(new SqlUpdate(trade)
          .addConstant(COL_TRADE_EXPORTED, System.currentTimeMillis())
          .setWhere(sys.idEquals(trade, invoice.getLong(itemsRelation))));
    }
    if (response.hasErrors()) {
      response.log(logger);
    }
    return response;
  }
}
