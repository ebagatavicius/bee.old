package com.butent.bee.server.modules.projects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.projects.ProjectConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.server.communication.ChatBean;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.ExchangeUtils;
import com.butent.bee.server.modules.classifiers.TimerBuilder;
import com.butent.bee.server.modules.tasks.TasksModuleBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
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
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.projects.ProjectStatus;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.CronExpression;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.WorkdayTransition;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

/**
 * Server-side Projects module bean.
 */
@Singleton
@Lock(LockType.READ)
@LocalBean
public class ProjectsModuleBean extends TimerBuilder implements BeeModule {

  @EJB
  SystemBean sys;

  @EJB
  QueryServiceBean qs;

  @EJB
  NewsBean news;

  @EJB
  ParamHolderBean prm;

  @EJB
  UserServiceBean usr;

  @EJB
  TasksModuleBean tasksBean;

  @EJB
  ChatBean chat;

  @Resource
  TimerService timerService;

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> result = new ArrayList<>();

    List<SearchResult> tasksSr =
        qs.getSearchResults(VIEW_PROJECTS,
            Filter.anyContains(Sets.newHashSet(COL_PROJECT_NAME,
                ClassifierConstants.ALS_CONTACT_FIRST_NAME,
                ClassifierConstants.ALS_CONTACT_LAST_NAME,
                ProjectConstants.ALS_OWNER_FIRST_NAME,
                ProjectConstants.ALS_OWNER_LAST_NAME, ClassifierConstants.ALS_COMPANY_NAME),
                query));

    List<SearchResult> pfSr = qs.getSearchResults(VIEW_PROJECT_FILES,
        Filter.anyContains(Sets.newHashSet(AdministrationConstants.COL_FILE_CAPTION,
            AdministrationConstants.ALS_FILE_NAME), query));
    result.addAll(pfSr);

    result.addAll(tasksSr);

    return result;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    switch (svc) {
      case SVC_GET_PROJECT_CHART_DATA:
        response = getProjectChartData(reqInfo);
        break;
      case SVC_GET_TIME_UNITS:
        response = getTimeUnits();
        break;
      case SVC_PROJECT_REPORT:
        response = getReportData();
        break;
      case SVC_CREATE_INVOICE_ITEMS:
        response = createInvoiceItems(reqInfo);
        break;
      default:
        break;
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    List<BeeParameter> params = Lists.newArrayList(
        BeeParameter.createNumber(module, PRM_PROJECT_COMMON_RATE, false,
            BeeConst.DOUBLE_ZERO),
        BeeParameter.createRelation(module, PRM_PROJECT_HOUR_UNIT,
            ClassifierConstants.TBL_UNITS, ClassifierConstants.COL_UNIT_NAME));
    return params;
  }

  @Override
  public Module getModule() {
    return Module.PROJECTS;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public TimerService getTimerService() {
    return timerService;
  }

  @Override
  public void init() {
    sys.registerDataEventHandler(new DataEventHandler() {

      @Subscribe
      @AllowConcurrentEvents
      public void setRowProperties(ViewQueryEvent event) {
        if (event.isAfter(VIEW_PROJECTS) && event.hasData()) {
          Long userId = usr.getCurrentUserId();
          BeeRowSet rowSet = event.getRowset();
          if (!DataUtils.isEmpty(rowSet)) {

            List projectsIdList = rowSet.getRowIds();

            SqlSelect puQuery = new SqlSelect().addFrom(TBL_PROJECT_USAGE)
                .addFields(TBL_PROJECT_USAGE, COL_PROJECT, ProjectConstants.COL_ACCESS)
                .setWhere(SqlUtils.and(
                    SqlUtils.inList(TBL_PROJECT_USAGE, COL_PROJECT, projectsIdList),
                    SqlUtils.equals(TBL_PROJECT_USAGE, AdministrationConstants.COL_USER, userId)));

            SimpleRowSet puData = qs.getData(puQuery);
            int projectIndex = puData.getColumnIndex(COL_PROJECT);
            for (SimpleRow tuRow : puData) {
              BeeRow row = rowSet.getRowById(tuRow.getLong(projectIndex));
              row.setProperty(ProjectConstants.PROP_LAST_ACCESS, userId,
                  tuRow.getValue(puData.getColumnIndex(ProjectConstants.COL_ACCESS)));
            }

            SqlSelect peQuery = new SqlSelect().addFrom(TBL_PROJECT_EVENTS)
                .addFields(TBL_PROJECT_EVENTS, COL_PROJECT)
                .addMax(TBL_PROJECT_EVENTS, ProjectConstants.COL_PUBLISH_TIME)
                .addGroup(TBL_PROJECT_EVENTS, COL_PROJECT)
                .setWhere(SqlUtils.and(
                    SqlUtils.inList(TBL_PROJECT_EVENTS, COL_PROJECT, projectsIdList),
                    SqlUtils.notEqual(TBL_PROJECT_EVENTS, ProjectConstants.COL_PUBLISHER, userId)));

            SimpleRowSet peData = qs.getData(peQuery);
            projectIndex = peData.getColumnIndex(COL_PROJECT);
            int publishIndex = peData.getColumnIndex(ProjectConstants.COL_PUBLISH_TIME);

            for (SimpleRow teRow : peData) {
              BeeRow row = rowSet.getRowById(teRow.getLong(projectIndex));
              row.setProperty(ProjectConstants.PROP_LAST_PUBLISH, teRow.getValue(publishIndex));

            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillProjectsTimeData(ViewQueryEvent event) {
        if (event.isAfter(VIEW_PROJECTS, VIEW_PROJECT_STAGES) && event.hasData()) {
          BeeRowSet viewRows = event.getRowset();

          int idxExpectedTasksDuration = DataUtils.getColumnIndex(COL_EXPECTED_TASKS_DURATION,
              viewRows.getColumns(), false);
          int idxActualTasksDuration = DataUtils.getColumnIndex(COL_ACTUAL_TASKS_DURATION,
              viewRows.getColumns(), false);

          int idxActualExpenses = DataUtils.getColumnIndex(TaskConstants.COL_ACTUAL_EXPENSES,
              viewRows.getColumns(), false);

          List<Long> rowIds = viewRows.getRowIds();

          SimpleRowSet times = getProjectsTasksTimesAndExpenses(rowIds, event.getTargetName());

          for (int i = 0; i < times.getNumberOfRows(); i++) {
            Long rowId = times.getLong(i, ALS_ROW_ID);
            Long expectedTaskDuration = times.getLong(i, COL_EXPECTED_TASKS_DURATION);
            Long actualTaskDuration = times.getLong(i, COL_ACTUAL_TASKS_DURATION);
            Double actualExpenses = times.getDouble(i, TaskConstants.COL_ACTUAL_EXPENSES);

            if (!DataUtils.isId(rowId)) {
              continue;
            }

            IsRow row = viewRows.getRowById(rowId);

            if (row == null) {
              continue;
            }

            if (!BeeUtils.isNegative(idxExpectedTasksDuration)) {
              row.setValue(idxExpectedTasksDuration, expectedTaskDuration);
            }

            if (!BeeUtils.isNegative(idxActualTasksDuration)) {
              row.setValue(idxActualTasksDuration, actualTaskDuration);
            }

            if (!BeeUtils.isNegative(idxActualExpenses)) {
              row.setValue(idxActualExpenses, actualExpenses);
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillProjectsTimeUnits(ViewQueryEvent event) {
        if (event.isAfter(VIEW_PROJECTS, VIEW_PROJECT_STAGES) && event.hasData()) {
          BeeRowSet viewRows = event.getRowset();

          ResponseObject units = getTimeUnits();

          if (units == null) {
            return;
          }

          if (!units.hasResponse(BeeRowSet.class)) {
            return;
          }

          BeeRowSet unitsRs = (BeeRowSet) units.getResponse();

          for (BeeRow row : viewRows) {
            row.setProperty(PROP_TIME_UNITS, unitsRs.serialize());
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillProjectsOverdue(ViewQueryEvent event) {
        if (event.isAfter(VIEW_PROJECTS) && event.hasData()) {
          BeeRowSet viewRows = event.getRowset();

          for (BeeRow row : viewRows) {
            long startTime = 0;
            JustDate time =
                row.getDate(
                    DataUtils.getColumnIndex(COL_PROJECT_START_DATE, viewRows.getColumns()));

            if (time != null) {
              startTime = time.getDateTime().getTime();
            }
            long finishTime = 0;
            time =
                row.getDate(DataUtils.getColumnIndex(COL_PROJECT_END_DATE, viewRows.getColumns()));

            if (time != null) {
              finishTime = time.getDateTime().getTime();
            }

            int projectStatus =
                BeeUtils.unbox(row.getInteger(DataUtils
                    .getColumnIndex(COL_PROJECT_STATUS, viewRows.getColumns())));
            long nowDate = new DateTime().getDateTime().getTime();
            if (projectStatus == ProjectStatus.APPROVED.ordinal()
                || projectStatus == ProjectStatus.SUSPENDED.ordinal()) {
              if (row.getDateTime(DataUtils.getColumnIndex(COL_PROJECT_APPROVED_DATE, viewRows
                  .getColumns())) != null) {
                nowDate =
                    row.getDateTime(
                        DataUtils.getColumnIndex(COL_PROJECT_APPROVED_DATE, viewRows
                            .getColumns())).getTime();
              }
            }
            long timeDiff =
                (finishTime - startTime) == 0L ? TimeUtils.MILLIS_PER_DAY : finishTime - startTime;

            double overdue =
                BeeUtils.round((100.0 * (nowDate - startTime) / timeDiff) - 100.0,
                    2);
            if (overdue < 0) {
              overdue = 0.0;
            }
            row.setValue(viewRows.getColumnIndex(COL_OVERDUE), Double.valueOf(overdue));
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void updateTimers(DataEvent.ViewModifyEvent event) {
        if (event.isAfter(VIEW_PROJECT_DATES)) {
          if (event instanceof DataEvent.ViewInsertEvent) {
            DataEvent.ViewInsertEvent ev = (DataEvent.ViewInsertEvent) event;
            if (DataUtils.contains(ev.getColumns(), COL_DATES_START_DATE)) {
              createOrUpdateTimers(TIMER_REMIND_PROJECT_DATES,
                  VIEW_PROJECT_DATES, ev.getRow().getId());
            }

          } else if (event instanceof DataEvent.ViewUpdateEvent) {
            DataEvent.ViewUpdateEvent ev = (DataEvent.ViewUpdateEvent) event;
            if (DataUtils.contains(ev.getColumns(), COL_DATES_START_DATE)) {
              createOrUpdateTimers(TIMER_REMIND_PROJECT_DATES,
                  VIEW_PROJECT_DATES, ev.getRow().getId());
            }
          }
        }
      }
    });

    news.registerUsageQueryProvider(Feed.PROJECT, new ProjectsUsageQueryProvider());

    BeeView.registerConditionProvider(FILTER_OVERDUE_CREATION,
        new BeeView.ConditionProvider() {

          @Override
          public IsCondition getCondition(BeeView view, List<String> args) {
            Double lower = BeeUtils.toDoubleOrNull(BeeUtils.getQuietly(args, 0));
            Double upper = BeeUtils.toDoubleOrNull(BeeUtils.getQuietly(args, 1));

            HasConditions conditions = SqlUtils.and();

            long nowDate = new JustDate().getTime();
            IsCondition cond =
                SqlUtils.isNull(SqlUtils.field(TBL_PROJECTS,
                    COL_PROJECT_APPROVED_DATE));
            IsExpression nowExpression =
                SqlUtils.sqlIf(cond, nowDate, SqlUtils.field(TBL_PROJECTS,
                    COL_PROJECT_APPROVED_DATE));

            IsExpression isExpression =
                SqlUtils.minus(SqlUtils.divide(SqlUtils.multiply(SqlUtils.constant(100.0), SqlUtils
                        .minus(nowExpression, SqlUtils.field(TBL_PROJECTS, COL_DATES_START_DATE))),
                    SqlUtils
                        .minus(SqlUtils.field(TBL_PROJECTS, COL_DATES_END_DATE), SqlUtils.field(
                            TBL_PROJECTS, COL_DATES_START_DATE))), SqlUtils.constant(100.0));

            IsExpression expression =
                SqlUtils.sqlIf(SqlUtils.lessEqual(isExpression, 0.0), 0.0, isExpression);

            if (lower != null) {
              IsCondition conditionLower = SqlUtils.moreEqual(expression, lower);
              conditions.add(conditionLower);
            }

            if (upper != null) {
              IsCondition conditionUpper = SqlUtils.less(expression, upper);
              conditions.add(conditionUpper);
            }

            return conditions;
          }
        });

    buildTimers(TIMER_REMIND_PROJECT_DATES);
  }

  @Override
  public void onTimeout(String timerInfo) {
    if (BeeUtils.isPrefix(timerInfo, TIMER_REMIND_PROJECT_DATES)) {
      Long dateId = BeeUtils.toLong(BeeUtils.removePrefix(timerInfo, TIMER_REMIND_PROJECT_DATES));

      if (DataUtils.isId(dateId)) {
        notifyProjectDate(dateId);
      }
    }
  }

  @Override
  protected List<Timer> createTimers(String timerIdentifier, IsCondition wh) {
    List<Timer> timersList = new ArrayList<>();
    if (BeeUtils.same(timerIdentifier, TIMER_REMIND_PROJECT_DATES)) {
      SimpleRowSet data = qs.getData(new SqlSelect()
          .addFields(VIEW_PROJECT_DATES, sys.getIdName(VIEW_PROJECT_DATES), COL_DATES_START_DATE,
              COL_DATES_NOTE)
          .addFrom(VIEW_PROJECT_DATES)
          .setWhere(SqlUtils.and(wh,
              SqlUtils.more(VIEW_PROJECT_DATES, COL_DATES_START_DATE,
                  Value.getValue(System.currentTimeMillis())))));

      for (SimpleRowSet.SimpleRow row : data) {
        Long timerId = row.getLong(sys.getIdName(VIEW_PROJECT_DATES));
        DateTime timerTime = TimeUtils.toDateTimeOrNull(row.getValue(COL_DATES_START_DATE));

        if (timerTime == null || !timerTime.hasTimePart()) {
          continue;
        }

        if (timerTime.getTime() > System.currentTimeMillis()) {
          Timer timer = timerService.createSingleActionTimer(timerTime.getJava(),
              new TimerConfig(TIMER_REMIND_PROJECT_DATES + timerId, false));

          if (timer != null) {
            timersList.add(timer);
          }
        }
      }
    }
    return timersList;
  }

  @Override
  protected Pair<IsCondition, List<String>> getConditionAndTimerIdForUpdate(String timerIdentifier,
      String viewName, Long relationId) {
    if (BeeUtils.same(timerIdentifier, TIMER_REMIND_PROJECT_DATES)) {
      IsCondition wh;

      if (BeeUtils.same(viewName, VIEW_PROJECT_DATES)) {
        wh = SqlUtils.equals(VIEW_PROJECT_DATES, sys.getIdName(VIEW_PROJECT_DATES), relationId);
        List<String> timerIdentifiersIds = new ArrayList<>();
        timerIdentifiersIds.add(timerIdentifier + relationId);
        return Pair.of(wh, timerIdentifiersIds);
      }
    }
    return null;
  }

  private static void fillUnitProperties(BeeRowSet units, long defUnit) {
    for (BeeRow row : units) {
      row.setProperty(PROP_REAL_FACTOR, getUnitRealFactor(units, row.getId(), defUnit));
    }
  }

  private static String getUnitRealFactor(BeeRowSet units, long id, long defUnit) {

    if (id == defUnit) {
      return BeeUtils.toString(BeeConst.DOUBLE_ONE);
    }

    BeeRow row = units.getRowById(id);

    if (!BeeUtils.isEmpty(row.getProperty(PROP_REAL_FACTOR))) {
      return row.getProperty(PROP_REAL_FACTOR);
    }

    int idxFact = units.getColumnIndex(ClassifierConstants.COL_UNIT_FACTOR);
    int idxParent = units.getColumnIndex(ClassifierConstants.COL_BASE_UNIT);

    double factValue = BeeConst.DOUBLE_ONE;
    long parent = BeeConst.LONG_UNDEF;

    if (!BeeConst.isUndef(idxFact)) {
      factValue = BeeUtils.unbox(row.getDouble(idxFact));
    }

    if (!BeeConst.isUndef(idxParent)) {
      parent = BeeUtils.unbox(row.getLong(idxParent));
    }

    if (!DataUtils.isId(parent)) {
      return BeeUtils.toString(factValue);
    }

    double result = factValue * BeeUtils.toDouble(getUnitRealFactor(units, parent, defUnit));

    return BeeUtils.toString(result);
  }

  private static void insertOrderedChartData(SimpleRowSet chartData, String[] data) {
    long stage1 =
        BeeUtils.unbox(BeeUtils.toLongOrNull(data[chartData.getColumnIndex(ALS_CHART_ID)]));

    for (int i = 0; i < chartData.getNumberOfRows(); i++) {
      long stage2 = BeeUtils.unbox(chartData.getLong(i, ALS_CHART_ID));

      if (stage1 < stage2) {
        chartData.getRows().add(i, data);
        return;
      }
    }

    chartData.getRows().add(data);
  }

  private ResponseObject createInvoiceItems(RequestInfo reqInfo) {
    Long saleId = BeeUtils.toLongOrNull(reqInfo.getParameter(TradeConstants.COL_SALE));
    Long currency = BeeUtils.toLongOrNull(reqInfo.getParameter(TradeConstants.COL_TRADE_CURRENCY));
    Set<Long> ids = DataUtils.parseIdSet(reqInfo.getParameter(Service.VAR_ID));

    if (!DataUtils.isId(saleId)) {
      return ResponseObject.error("Wrong account ID");
    }
    if (!DataUtils.isId(currency)) {
      return ResponseObject.error("Wrong currency ID");
    }
    if (BeeUtils.isEmpty(ids)) {
      return ResponseObject.error("Empty ID list");
    }

    IsCondition where = sys.idInList(TBL_PROJECT_INCOMES, ids);

    SqlSelect query = new SqlSelect();
    query.addFields(TBL_PROJECT_INCOMES, TradeConstants.COL_TRADE_VAT_PLUS,
        TradeConstants.COL_TRADE_VAT, TradeConstants.COL_TRADE_VAT_PERC, COL_INCOME_ITEM,
        TradeConstants.COL_TRADE_ITEM_QUANTITY, COL_INCOME_NOTE)
        .addFrom(TBL_PROJECT_INCOMES)
        .setWhere(where);

    IsExpression vatExch =
        ExchangeUtils.exchangeFieldTo(query, TBL_PROJECT_INCOMES, TradeConstants.COL_TRADE_VAT,
            TradeConstants.COL_TRADE_CURRENCY, COL_INCOME_DATE, currency);

    String vatAlias = "Vat_" + SqlUtils.uniqueName();

    String priceAlias = "Price_" + SqlUtils.uniqueName();
    IsExpression priceExch =
        ExchangeUtils.exchangeFieldTo(query, TBL_PROJECT_INCOMES,
            TradeConstants.COL_TRADE_ITEM_PRICE, TradeConstants.COL_TRADE_CURRENCY,
            COL_INCOME_DATE, currency);

    query.addExpr(priceExch, priceAlias)
        .addExpr(vatExch, vatAlias)
        .addOrder(TBL_PROJECT_INCOMES, sys.getIdName(TBL_PROJECT_INCOMES));

    LogUtils.getRootLogger().info(query.getQuery());

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.error(TBL_PROJECT_INCOMES, ids, "not found");
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
        vat = row.getDouble(vatAlias);
      }

      if (BeeUtils.nonZero(vat)) {
        insert.addConstant(TradeConstants.COL_TRADE_VAT, vat);
      }

      Boolean vatPlus = row.getBoolean(TradeConstants.COL_TRADE_VAT_PLUS);

      if (BeeUtils.isTrue(vatPlus)) {
        insert.addConstant(TradeConstants.COL_TRADE_VAT_PLUS, vatPlus);
      }

      Double quantity = row.getDouble(TradeConstants.COL_TRADE_ITEM_QUANTITY);
      Double price = row.getDouble(priceAlias);

      insert.addConstant(TradeConstants.COL_TRADE_ITEM_QUANTITY, BeeUtils.unbox(quantity));

      if (price != null) {
        insert.addConstant(TradeConstants.COL_TRADE_ITEM_PRICE, price);
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
      SqlUpdate update = new SqlUpdate(TBL_PROJECT_INCOMES)
          .addConstant(COL_INCOME_SALE, saleId)
          .setWhere(where);

      ResponseObject updResponse = qs.updateDataWithResponse(update);
      if (updResponse.hasErrors()) {
        response.addMessagesFrom(updResponse);
      }
    }

    return response;
  }

  private ResponseObject getProjectChartData(RequestInfo req) {
    Long projectId = BeeUtils.toLongOrNull(req.getParameter(VAR_PROJECT));

    if (!DataUtils.isId(projectId)) {
      return ResponseObject.error(projectId);
    }

    final SimpleRowSet chartData =
        new SimpleRowSet(new String[] {
            ALS_VIEW_NAME, ALS_CHART_ID,
            ALS_CHART_CAPTION, ALS_CHART_START, ALS_CHART_END, ALS_CHART_FLOW_COLOR,
            ALS_TASK_STATUS, PROP_RS});

    BeeRowSet rs = qs.getViewData(VIEW_PROJECT_DATES, Filter.equals(COL_PROJECT, projectId),
        Order.ascending(COL_DATES_START_DATE));

    int idxColor = rs.getColumnIndex(COL_DATES_COLOR);
    int idxCaption = rs.getColumnIndex(COL_DATES_NOTE);
    int idxStartDate = rs.getColumnIndex(COL_DATES_START_DATE);

    for (BeeRow rsRow : rs) {
      DateTime startDate = rsRow.getDateTime(idxStartDate);

      BeeRowSet newRow = new BeeRowSet(VIEW_PROJECT_DATES, rs.getColumns());
      newRow.addRow(rsRow);

      chartData.addRow(new String[] {
          VIEW_PROJECT_DATES,
          null,
          rsRow.getString(idxCaption),
          startDate == null ? null : BeeUtils.toString(startDate.getDate().getDays()),
          null,
          rsRow.getString(idxColor), null, Codec.beeSerialize(newRow)});
    }

    rs = qs.getViewData(VIEW_PROJECT_STAGES, Filter.equals(COL_PROJECT, projectId),
        Order.ascending(sys.getIdName(VIEW_PROJECT_STAGES), COL_STAGE_START_DATE));

    int idxStageName = rs.getColumnIndex(COL_STAGE_NAME);
    int idxStageStart = rs.getColumnIndex(COL_STAGE_START_DATE);
    int idxStageEnd = rs.getColumnIndex(COL_STAGE_END_DATE);
    int idxTasksTimeHigh = rs.getColumnIndex(ALS_HIGH_TASKS_DATE);
    int idxTasksTimeLow = rs.getColumnIndex(ALS_LOW_TASKS_DATE);

    for (BeeRow rsRow : rs) {
      String stage = BeeUtils.toString(rsRow.getId());
      String stageName = BeeUtils.isNegative(idxStageName) ? stage : rsRow.getString(idxStageName);

      String stageStart = rsRow.getString(idxStageStart);
      String stageEnd = rsRow.getString(idxStageEnd);

      if (BeeUtils.isEmpty(stageStart) && rsRow.getDateTime(idxTasksTimeLow) != null) {
        stageStart = BeeUtils.toString(rsRow.getDateTime(idxTasksTimeLow).getDate().getDays());
      }

      if (BeeUtils.isEmpty(stageEnd) && rsRow.getDateTime(idxTasksTimeHigh) != null) {
        stageEnd = BeeUtils.toString(rsRow.getDateTime(idxTasksTimeHigh).getDate().getDays());
      }

      BeeRowSet newRow = new BeeRowSet(VIEW_PROJECT_STAGES, rs.getColumns());
      newRow.addRow(rsRow);

      chartData.addRow(new String[] {
          VIEW_PROJECT_STAGES,
          stage, stageName, stageStart, stageEnd, null, null, Codec.beeSerialize(newRow)
      });
    }

    rs = qs.getViewData(TaskConstants.VIEW_TASKS, Filter.equals(COL_PROJECT, projectId),
        Order.ascending(sys.getIdName(TaskConstants.VIEW_TASKS), TaskConstants.COL_START_TIME));

    int idxStage = rs.getColumnIndex(COL_PROJECT_STAGE);
    int idxSummary = rs.getColumnIndex(TaskConstants.COL_SUMMARY);
    int idxStartTime = rs.getColumnIndex(TaskConstants.COL_START_TIME);
    int idxFinishTime = rs.getColumnIndex(TaskConstants.COL_FINISH_TIME);
    int indTaskStatus = rs.getColumnIndex(TaskConstants.COL_STATUS);
    DateTime timeNow = new DateTime();

    for (BeeRow rsRow : rs) {
      String stage = rsRow.getString(idxStage);
      String startTime = rsRow.getDateTime(idxStartTime) == null ? null
          : BeeUtils.toString(rsRow.getDateTime(idxStartTime).getDate().getDays());
      String finishTime = rsRow.getDateTime(idxFinishTime) == null ? null
          : BeeUtils.toString(rsRow.getDateTime(idxFinishTime).getDate().getDays());
      String taskStatus = BeeConst.STRING_EMPTY;

      if (rsRow.getInteger(indTaskStatus) == TaskStatus.ACTIVE.ordinal()
          || rsRow.getInteger(indTaskStatus) == TaskStatus.NOT_VISITED.ordinal()) {

        if (timeNow.getDate().getDays() < BeeUtils.toInt(finishTime)) {
          taskStatus = TaskConstants.VAR_TASK_ACTIVE;
        } else {
          taskStatus = TaskConstants.VAR_TASK_LATE;
        }

      } else if (rsRow.getInteger(indTaskStatus) == TaskStatus.COMPLETED.ordinal()) {
        taskStatus = TaskConstants.VAR_TASK_COMPLETED;
      }

      BeeRowSet newRow = new BeeRowSet(VIEW_TASKS, rs.getColumns());
      newRow.addRow(rsRow);

      insertOrderedChartData(chartData, new String[] {
          TaskConstants.VIEW_TASKS,
          stage, rsRow.getString(idxSummary),
          startTime, finishTime, null, taskStatus, Codec.beeSerialize(newRow)
      });
    }
    rs =
        qs.getViewData(TaskConstants.VIEW_RECURRING_TASKS, Filter.equals(COL_PROJECT, projectId),
            Order.ascending(sys.getIdName(TaskConstants.VIEW_RECURRING_TASKS),
                TaskConstants.COL_RT_SCHEDULE_FROM));

    int idxStart = rs.getColumnIndex(TaskConstants.COL_RT_SCHEDULE_FROM);
    int idxFinish = rs.getColumnIndex(TaskConstants.COL_RT_SCHEDULE_UNTIL);
    idxSummary = rs.getColumnIndex(TaskConstants.COL_SUMMARY);

    for (BeeRow rsRow : rs) {

      Long recTask = rsRow.getId();
      JustDate from = rsRow.getDate(idxStart);
      JustDate until = rsRow.getDate(idxFinish);

      CronExpression.Builder builder = new CronExpression.Builder(from, until)
          .id(BeeUtils.toString(recTask))
          .dayOfMonth(DataUtils.getString(rs, rsRow, COL_RT_DAY_OF_MONTH))
          .month(DataUtils.getString(rs, rsRow, COL_RT_MONTH))
          .dayOfWeek(DataUtils.getString(rs, rsRow, COL_RT_DAY_OF_WEEK))
          .year(DataUtils.getString(rs, rsRow, COL_RT_YEAR))
          .workdayTransition(EnumUtils.getEnumByIndex(WorkdayTransition.class,
              DataUtils.getInteger(rs, rsRow, COL_RT_WORKDAY_TRANSITION)));

      CronExpression cron = builder.build();

      JustDate min;
      JustDate max;

      if (from == null && until == null) {
        min = TimeUtils.startOfMonth();
        max = TimeUtils.endOfMonth(min, 12);
      } else if (from == null) {
        min = TimeUtils.startOfMonth(until);
        max = until;
      } else if (until == null) {
        min = TimeUtils.max(from, TimeUtils.startOfPreviousMonth(TimeUtils.today()));
        max = TimeUtils.endOfMonth(min, 12);
      } else {
        min = from;
        max = TimeUtils.max(from, until);
      }
      List<JustDate> cronDates = cron.getDates(min, max);

      for (JustDate date : cronDates) {
        BeeRowSet newRow = new BeeRowSet(VIEW_RECURRING_TASKS, rs.getColumns());
        newRow.addRow(rsRow);

        insertOrderedChartData(chartData, new String[] {
            TaskConstants.VIEW_RECURRING_TASKS,
            null, rsRow.getString(idxSummary),
            BeeUtils.toString(date.getDate().getDays()),
            BeeUtils.toString(date.getDate().getDays()), null,
            null, Codec.beeSerialize(newRow)
        });
      }

    }

    return ResponseObject.response(chartData);
  }

  private SimpleRowSet getProjectsTasksTimesAndExpenses(List<Long> ids, String viewName) {
    SimpleRowSet taskExpectedTimes = getTasksExpectedTimes(ids, viewName);
    SimpleRowSet taskActualTimesAndExpenses = getTasksActualTimesAndExpenses(ids, viewName);

    SimpleRowSet result = new SimpleRowSet(new String[] {
        ALS_ROW_ID, COL_EXPECTED_TASKS_DURATION, COL_ACTUAL_TASKS_DURATION,
        TaskConstants.COL_ACTUAL_EXPENSES
    });

    Map<Long, Integer> rowTimesHash = new HashMap<>();

    for (int i = 0; i < taskExpectedTimes.getNumberOfRows(); i++) {
      Long rowId = taskExpectedTimes.getLong(i, ALS_ROW_ID);
      Long time = taskExpectedTimes.getLong(i, COL_EXPECTED_TASKS_DURATION);

      Integer index = rowTimesHash.get(rowId);

      if (index == null) {
        result.addEmptyRow();
        index = result.getNumberOfRows() - 1;
        rowTimesHash.put(rowId, index);
      }

      result.setValue(index, ALS_ROW_ID, BeeUtils.toString(rowId));
      // double timeInHours =
      // Double.valueOf(time.doubleValue()) / Double.valueOf(TimeUtils.MILLIS_PER_HOUR);

      result.setValue(index, COL_EXPECTED_TASKS_DURATION, BeeUtils.toString(time));
    }

    for (int i = 0; i < taskActualTimesAndExpenses.getNumberOfRows(); i++) {
      Long rowId = taskActualTimesAndExpenses.getLong(i, ALS_ROW_ID);
      Long time = taskActualTimesAndExpenses.getLong(i, COL_ACTUAL_TASKS_DURATION);
      Double expenses = taskActualTimesAndExpenses.getDouble(i, TaskConstants.COL_ACTUAL_EXPENSES);

      Integer index = rowTimesHash.get(rowId);

      if (index == null) {
        result.addEmptyRow();
        index = result.getNumberOfRows() - 1;
        rowTimesHash.put(rowId, index);
      }

      result.setValue(index, ALS_ROW_ID, BeeUtils.toString(rowId));

      // double timeInHours =
      // Double.valueOf(time.doubleValue()) / Double.valueOf(TimeUtils.MILLIS_PER_HOUR);
      result.setValue(index, COL_ACTUAL_TASKS_DURATION, BeeUtils.toString(time));

      result.setValue(index, TaskConstants.COL_ACTUAL_EXPENSES, BeeUtils.toString(expenses));
    }

    return result;
  }

  private SimpleRowSet getTasksExpectedTimes(List<Long> ids, String viewName) {
    String filterColumn = BeeConst.EMPTY;
    SimpleRowSet result = new SimpleRowSet(new String[] {
        ALS_ROW_ID, COL_EXPECTED_TASKS_DURATION
    });

    switch (viewName) {
      case VIEW_PROJECTS:
        filterColumn = COL_PROJECT;
        break;
      case VIEW_PROJECT_STAGES:
        filterColumn = COL_PROJECT_STAGE;
        break;
      default:
        return result;
    }

    Filter idFilter = Filter.any(filterColumn, ids);
    Filter durationFilter = Filter.notNull(TaskConstants.COL_EXPECTED_DURATION);

    BeeRowSet tasks =
        qs.getViewData(TaskConstants.VIEW_TASKS, Filter.and(idFilter, durationFilter), null,
            Lists.newArrayList(filterColumn, TaskConstants.COL_EXPECTED_DURATION));

    if (tasks.isEmpty()) {
      return result;
    }

    Map<Long, Long> times = new HashMap<>();
    int idxExpectedDuration =
        DataUtils.getColumnIndex(TaskConstants.COL_EXPECTED_DURATION, tasks.getColumns(), false);
    int idxId = DataUtils.getColumnIndex(filterColumn, tasks.getColumns(), false);

    if (BeeUtils.isNegative(idxExpectedDuration) || BeeUtils.isNegative(idxId)) {
      return result;
    }

    for (IsRow row : tasks) {
      Long id = row.getLong(idxId);
      String newTime = row.getString(idxExpectedDuration);

      Long newTimeMls = TimeUtils.parseTime(newTime);
      Long currentTime = times.get(id);

      if (currentTime == null) {
        currentTime = Long.valueOf(0);
      }

      currentTime += newTimeMls;

      times.put(id, currentTime);
    }

    for (Long id : times.keySet()) {
      result.addRow(new String[] {
          BeeUtils.toString(id),
          BeeUtils.toString(times.get(id))});
    }

    return result;
  }

  private ResponseObject getReportData() {
    SqlSelect select = new SqlSelect();
    select.addField(TaskConstants.TBL_TASKS, sys.getIdName(TaskConstants.TBL_TASKS),
        TaskConstants.COL_TASK);
    select.addField(TaskConstants.TBL_TASKS, TaskConstants.COL_STATUS,
        ALS_TASK_STATUS);

    select.addField(TaskConstants.TBL_TASKS, ProjectConstants.COL_PROJECT,
        ProjectConstants.COL_PROJECT);
    select.addFields(TaskConstants.TBL_TASKS, TaskConstants.COL_EXPECTED_EXPENSES,
        TaskConstants.COL_EXPECTED_DURATION);

    select.addFields(ProjectConstants.TBL_PROJECTS,

        ProjectConstants.COL_PROJECT_NAME,
        ProjectConstants.COL_PROJECT_STATUS,
        ProjectConstants.COL_PROJECT_TYPE,
        ProjectConstants.COL_PROJECT_PRIORITY,
        ProjectConstants.COL_PROJECT_START_DATE,
        ProjectConstants.COL_PROJECT_END_DATE);
    select.addField(TBL_PROJECT_STAGES, COL_STAGE_NAME, ALS_STAGE_NAME);

    select.addExpr(SqlUtils.concat(SqlUtils.nvl(SqlUtils.field(ClassifierConstants.TBL_PERSONS,
        ClassifierConstants.COL_FIRST_NAME), SqlUtils.constant(BeeConst.STRING_EMPTY)),
        SqlUtils.constant(BeeConst.STRING_SPACE), SqlUtils.nvl(SqlUtils.field(
            ClassifierConstants.TBL_PERSONS,
            ClassifierConstants.COL_LAST_NAME), SqlUtils.constant(BeeConst.STRING_EMPTY))),
        ProjectConstants.COL_PROJECT_OWNER);

    select.addField(ClassifierConstants.TBL_COMPANIES, ClassifierConstants.COL_COMPANY_NAME,
        ClassifierConstants.ALS_COMPANY_NAME);

    select.addEmptyDouble(TaskConstants.COL_ACTUAL_DURATION);
    select.addEmptyDouble(TaskConstants.COL_ACTUAL_EXPENSES);
    select.addEmptyDouble(ALS_PROFIT);
    select.addEmptyString(ALS_TERM, 100);

    select.addFrom(TaskConstants.TBL_TASKS);
    select.addFromInner(ProjectConstants.TBL_PROJECTS, SqlUtils.join(TaskConstants.TBL_TASKS,
        ProjectConstants.COL_PROJECT, ProjectConstants.TBL_PROJECTS, sys
            .getIdName(ProjectConstants.TBL_PROJECTS)));

    select.addFromLeft(ClassifierConstants.TBL_COMPANIES, sys.joinTables(
        ClassifierConstants.TBL_COMPANIES, TBL_PROJECTS, COL_COMAPNY));

    select.addFromLeft(TBL_PROJECT_STAGES, sys.joinTables(
        TBL_PROJECT_STAGES, TaskConstants.TBL_TASKS, COL_PROJECT_STAGE));

    select.addFromLeft(AdministrationConstants.TBL_USERS, sys.joinTables(
        AdministrationConstants.TBL_USERS, TBL_PROJECTS,
        COL_PROJECT_OWNER));

    select.addFromLeft(ClassifierConstants.TBL_COMPANY_PERSONS, sys.joinTables(
        ClassifierConstants.TBL_COMPANY_PERSONS, AdministrationConstants.TBL_USERS,
        ClassifierConstants.COL_COMPANY_PERSON));

    select.addFromLeft(ClassifierConstants.TBL_PERSONS, sys.joinTables(
        ClassifierConstants.TBL_PERSONS, ClassifierConstants.TBL_COMPANY_PERSONS,
        ClassifierConstants.COL_PERSON));

    select.addFromLeft(ClassifierConstants.TBL_UNITS, sys.joinTables(ClassifierConstants.TBL_UNITS,
        TBL_PROJECTS, COL_PROJECT_TIME_UNIT));

    SimpleRowSet rqs = qs.getData(select);

    if (rqs.isEmpty()) {
      return ResponseObject.response(rqs);
    }

    List<String> colTaskData = Lists.newArrayList(rqs.getColumn(TaskConstants.COL_TASK));
    List<Long> taskIds =
        DataUtils.parseIdList(BeeUtils.join(BeeConst.STRING_COMMA, colTaskData));

    if (taskIds.isEmpty()) {
      return ResponseObject.response(rqs);
    }

    SimpleRowSet timesData = tasksBean.getTaskActualTimesAndExpenses(taskIds);

    Map<String, String> times = Maps.newHashMap();
    Map<String, String> expenses = Maps.newHashMap();

    if (!timesData.isEmpty()) {
      times = Codec.deserializeLinkedHashMap(timesData.getValue(0, COL_ACTUAL_DURATION));
      expenses = Codec.deserializeLinkedHashMap(timesData.getValue(0, COL_ACTUAL_EXPENSES));
    }

    for (int i = 0; i < rqs.getNumberOfRows(); i++) {
      String taskId = rqs.getValue(i, TaskConstants.COL_TASK);
      double time =
          (double) BeeUtils.toLong(times.get(taskId)) / (double) TimeUtils.MILLIS_PER_HOUR;
      double exp = BeeUtils.toDouble(expenses.get(taskId));
      double expExp = BeeUtils.unbox(rqs.getDouble(i, TaskConstants.COL_EXPECTED_EXPENSES));
      double profit = expExp - exp;
      String expTimeStr = rqs.getValue(i, TaskConstants.COL_EXPECTED_DURATION);
      double expTime = BeeConst.DOUBLE_ZERO;

      JustDate startDate = null;
      JustDate endDate = null;

      if (!BeeUtils.isEmpty(expTimeStr)) {
        expTime = (double) TimeUtils.parseTime(expTimeStr) / (double) TimeUtils.MILLIS_PER_HOUR;
      }

      if (rqs.getDateTime(i, COL_PROJECT_START_DATE) != null) {
        startDate = new JustDate(rqs.getDateTime(i, COL_PROJECT_START_DATE));
      }

      if (rqs.getDateTime(i, COL_PROJECT_END_DATE) != null) {
        endDate = new JustDate(rqs.getDateTime(i, COL_PROJECT_END_DATE));
      }

      rqs.setValue(i, TaskConstants.COL_ACTUAL_DURATION, BeeUtils.toString(time));
      rqs.setValue(i, TaskConstants.COL_ACTUAL_EXPENSES, BeeUtils.toString(exp));
      rqs.setValue(i, TaskConstants.COL_EXPECTED_DURATION, BeeUtils.toString(expTime));
      rqs.setValue(i, ALS_TERM, BeeUtils.joinWords(startDate,
          BeeConst.STRING_MINUS, endDate));
      rqs.setValue(i, ALS_PROFIT, BeeUtils.toString(profit));
    }

    return ResponseObject.response(rqs);
  }

  private SimpleRowSet getTasksActualTimesAndExpenses(List<Long> ids, String viewName) {
    String filterColumn = BeeConst.EMPTY;
    SimpleRowSet result = new SimpleRowSet(new String[] {
        ALS_ROW_ID, COL_ACTUAL_TASKS_DURATION, TaskConstants.COL_ACTUAL_EXPENSES
    });

    switch (viewName) {
      case VIEW_PROJECTS:
        filterColumn = COL_PROJECT;
        break;
      case VIEW_PROJECT_STAGES:
        filterColumn = COL_PROJECT_STAGE;
        break;
      default:
        return result;
    }

    Filter idFilter = Filter.any(filterColumn, ids);
    Filter durationFilter = Filter.notNull(TaskConstants.COL_DURATION);

    BeeRowSet taskEvents =
        qs.getViewData(TaskConstants.VIEW_TASK_DURATIONS, Filter.and(idFilter, durationFilter),
            null, Lists.newArrayList(filterColumn, TaskConstants.COL_DURATION, COL_RATE));

    if (taskEvents.isEmpty()) {
      return result;
    }

    Double defaultRate = prm.getDouble(ProjectConstants.PRM_PROJECT_COMMON_RATE);

    Map<Long, Long> times = new HashMap<>();
    Map<Long, Double> expenses = new HashMap<>();

    int idxEventDuration =
        DataUtils.getColumnIndex(TaskConstants.COL_DURATION, taskEvents.getColumns(), false);
    int idxId = DataUtils.getColumnIndex(filterColumn, taskEvents.getColumns(), false);
    int idxRate =
        DataUtils.getColumnIndex(COL_RATE, taskEvents.getColumns(), false);

    if (BeeUtils.isNegative(idxEventDuration) || BeeUtils.isNegative(idxId)) {
      return result;
    }

    for (IsRow row : taskEvents) {
      Long id = row.getLong(idxId);
      String newTime = row.getString(idxEventDuration);

      if (BeeUtils.isEmpty(newTime)) {
        continue;
      }

      Long newTimeMls = TimeUtils.parseTime(newTime);
      Long currentTime = times.get(id);

      if (currentTime == null) {
        currentTime = Long.valueOf(0);
      }

      currentTime += newTimeMls;

      times.put(id, currentTime);

      if (BeeUtils.isNegative(idxRate)) {
        continue;
      }

      Double rate = row.getDouble(idxRate);
      double currentTimeInHrs = Double.valueOf(newTimeMls)
          / Double.valueOf(TimeUtils.MILLIS_PER_HOUR);
      Double currentExpense = expenses.get(id);

      if (!BeeUtils.isPositive(rate)) {
        if (BeeUtils.isDouble(defaultRate)) {
          rate = defaultRate;
        } else {
          rate = Double.valueOf(BeeConst.DOUBLE_ZERO);
        }
      }

      if (currentExpense == null) {
        currentExpense = Double.valueOf(BeeConst.DOUBLE_ZERO);
      }

      currentExpense += rate * currentTimeInHrs;
      expenses.put(id, currentExpense);

    }

    for (Long id : ids) {
      Long timeMills = 0L;
      double expense = 0.0;

      if (times.containsKey(id)) {
        timeMills = times.get(id);
      }

      if (expenses.containsKey(id)) {
        expense = expenses.get(id) == null ? 0.0 : expenses.get(id);
      }

      result.addRow(new String[] {
          BeeUtils.toString(id),
          BeeUtils.toString(timeMills),
          BeeUtils.toString(expense)
      });
    }
    return result;

  }

  private ResponseObject getTimeUnits() {
    Long defUnit = prm.getRelation(PRM_PROJECT_HOUR_UNIT);

    if (!DataUtils.isId(defUnit)) {
      return ResponseObject.emptyResponse();
    }

    BeeRowSet units = qs.getViewData(ClassifierConstants.TBL_UNITS, Filter.compareId(defUnit));

    List<Long> idFilter = Lists.newArrayList(defUnit);

    while (!idFilter.isEmpty()) {
      BeeRowSet relUnits =
          qs.getViewData(ClassifierConstants.TBL_UNITS, Filter.any(
              ClassifierConstants.COL_BASE_UNIT, idFilter));

      idFilter.clear();
      if (!relUnits.isEmpty()) {
        idFilter.addAll(relUnits.getRowIds());
        units.addRows(relUnits.getRows());
      }
    }

    fillUnitProperties(units, defUnit);

    return ResponseObject.response(units);
  }

  private void notifyProjectDate(Long projectDateId) {
    if (DataUtils.isId(projectDateId)) {
      SimpleRowSet data = qs.getData(new SqlSelect()
          .addFields(VIEW_PROJECT_DATES, COL_DATES_NOTE, COL_DATES_START_DATE)
          .addFields(VIEW_PROJECTS, sys.getIdName(VIEW_PROJECTS), COL_PROJECT_NAME, COL_OWNER)
          .addFrom(VIEW_PROJECT_DATES)
          .addFromInner(VIEW_PROJECTS,
              sys.joinTables(VIEW_PROJECTS, VIEW_PROJECT_DATES, COL_PROJECT))
          .setWhere(SqlUtils.equals(
              VIEW_PROJECT_DATES, sys.getIdName(VIEW_PROJECT_DATES), projectDateId)));

      for (SimpleRow row : data) {
        Long userId = row.getLong(COL_OWNER);
        Dictionary dic = usr.getDictionary(userId);
        DateTimeFormatInfo dtfInfo = usr.getDateTimeFormatInfo(userId);

        Map<String, String> linkData = Maps.newHashMap();
        linkData.put(VIEW_PROJECTS, row.getValue(sys.getIdName(VIEW_PROJECTS)));

        if (DataUtils.isId(userId) && dic != null) {
          chat.putMessage(
              BeeUtils.joinWords(dic.project(),
                  BeeConst.STRING_QUOT + row.getValue(COL_PROJECT_NAME) + BeeConst.STRING_QUOT
                      + BeeConst.STRING_POINT,
                  dic.prjDates() + BeeConst.STRING_COLON,
                  Formatter.renderDateTime(dtfInfo, row.getDateTime(COL_DATES_START_DATE))
                      + BeeConst.STRING_POINT,
                  BeeUtils.isEmpty(row.getValue(COL_DATES_NOTE))
                      ? "" : BeeUtils.joinWords(dic.note() + BeeConst.STRING_COLON,
                      row.getValue(COL_DATES_NOTE) + BeeConst.STRING_POINT)),
              userId,
              linkData);
        }
      }
    }
  }
}
