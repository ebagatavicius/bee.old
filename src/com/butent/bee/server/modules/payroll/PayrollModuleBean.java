package com.butent.bee.server.modules.payroll;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.output.ReportDateItem;
import com.butent.bee.client.output.ReportItem;
import com.butent.bee.server.Invocation;
import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.AdministrationModuleBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.RangeMap;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.i18n.DateOrdering;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.finance.FinanceConstants;
import com.butent.bee.shared.modules.payroll.Earnings;
import com.butent.bee.shared.modules.payroll.PayrollConstants.WorkScheduleKind;
import com.butent.bee.shared.modules.payroll.PayrollUtils;
import com.butent.bee.shared.modules.payroll.WorkScheduleSummary;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.report.ReportInfo;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateRange;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeRange;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.webservice.ButentWS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;

@Stateless
@LocalBean
public class PayrollModuleBean implements BeeModule, ConcurrencyBean.HasTimerService {

  private static final String EVENT_STATUS_OK = "OK";

  private static BeeLogger logger = LogUtils.getLogger(PayrollModuleBean.class);

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  AdministrationModuleBean adm;
  @EJB
  ParamHolderBean prm;
  @EJB
  UserServiceBean usr;
  @EJB
  ConcurrencyBean cb;

  @Resource
  TimerService timerService;

  @Resource
  EJBContext ctx;

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> result = new ArrayList<>();

    if (BeeUtils.isPositiveInt(query)) {
      result.addAll(qs.getSearchResults(VIEW_EMPLOYEES,
          Filter.equals(COL_TAB_NUMBER, BeeUtils.toInt(query))));

    } else {
      result.addAll(qs.getSearchResults(VIEW_EMPLOYEES,
          Filter.anyContains(Sets.newHashSet(COL_FIRST_NAME, COL_LAST_NAME,
              ALS_DEPARTMENT_NAME, ALS_POSITION_NAME), query)));

      result.addAll(qs.getSearchResults(VIEW_LOCATIONS,
          Filter.anyContains(Sets.newHashSet(COL_LOCATION_NAME, ALS_COMPANY_NAME,
              ALS_LOCATION_MANAGER_FIRST_NAME, ALS_LOCATION_MANAGER_LAST_NAME,
              COL_ADDRESS), query)));
    }

    return result;
  }

  @Override
  public ResponseObject doService(String service, RequestInfo reqInfo) {
    ResponseObject response;

    String svc = BeeUtils.trim(service);
    switch (svc) {
      case SVC_GET_SCHEDULE_OVERLAP:
        response = getScheduleOverlap(reqInfo);
        break;

      case SVC_GET_SCHEDULED_MONTHS:
        response = getScheduledMonths(reqInfo);
        break;

      case SVC_GET_EARNINGS:
        response = getEarnings(reqInfo);
        break;

      case SVC_PAYROLL_FUND_REPORT:
        response = getTimeSheetPayrollReportData(reqInfo);
        break;
      default:
        String msg = BeeUtils.joinWords("service not recognized:", svc);
        logger.warning(msg);
        response = ResponseObject.error(msg);
    }

    return response;
  }

  @Override
  public void ejbTimeout(Timer timer) {
    if (cb.isParameterTimer(timer, PRM_ERP_SYNC_PAYROLL_DATA + SFX_TIME)
      || cb.isParameterTimer(timer, PRM_ERP_SYNC_PAYROLL_DATA + SFX_HOURS)) {
      cb.asynchronousCall(new ConcurrencyBean.AsynchronousRunnable() {
        @Override
        public void run() {
          PayrollModuleBean bean =
            Assert.notNull(Invocation.locateRemoteBean(PayrollModuleBean.class));
          bean.importERPData();
        }
      });
    }
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    return Arrays.asList(
      BeeParameter.createText(getModule().getName(), PRM_ERP_SYNC_PAYROLL_DATA + SFX_HOURS),
      BeeParameter.createNumber(getModule().getName(), PRM_ERP_SYNC_PAYROLL_DATA + SFX_TIME),
      BeeParameter.createBoolean(getModule().getName(), PRM_ERP_SYNC_EMPLOYEES, false, true),
      BeeParameter.createBoolean(getModule().getName(), PRM_ERP_SYNC_LOCATIONS),
      BeeParameter.createBoolean(getModule().getName(), PRM_ERP_SYNC_TIME_CARDS),
      BeeParameter.createNumber(getModule().getName(), PRM_ERP_SYNC_PAYROLL_DELTA_HOURS),
      BeeParameter.createBoolean(getModule().getName(), PRM_REST_EARNINGS_FUNDS_ONLY)
    );
  }

  public ResponseObject getEmployeeEarnings(String companyCode, Integer tabNumber,
      Integer year, Integer month) {

    if (BeeUtils.isEmpty(companyCode)) {
      return ResponseObject.error("company code not specified");
    }
    if (!TimeUtils.isYear(year)) {
      return ResponseObject.error("year not specified");
    }
    if (!TimeUtils.isMonth(month)) {
      return ResponseObject.error("month not specified");
    }

    ResponseObject ecr = getEmployeeCondition(companyCode, tabNumber);
    if (ecr.hasErrors()) {
      return ecr;
    }

    if (!(ecr.getResponse() instanceof IsCondition)) {
      return ResponseObject.error("cannot filter employees", companyCode, tabNumber);
    }

    boolean fundsOnly = BeeUtils.isTrue(prm.getBoolean(PRM_REST_EARNINGS_FUNDS_ONLY));
    String emplIdName = sys.getIdName(TBL_EMPLOYEES);

    IsCondition employeeCondition = (IsCondition) ecr.getResponse();

    SqlSelect employeeQuery = new SqlSelect()
        .addFields(TBL_EMPLOYEES, emplIdName, COL_TAB_NUMBER)
        .addFrom(TBL_EMPLOYEES)
        .addFromLeft(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_EMPLOYEES, COL_COMPANY_PERSON))
        .setWhere(employeeCondition);

    SimpleRowSet employeeData = qs.getData(employeeQuery);
    if (DataUtils.isEmpty(employeeData)) {
      return ResponseObject.info("employees found", companyCode, tabNumber);
    }

    YearMonth ym = new YearMonth(year, month);
    Set<JustDate> holidays = getHolidays(ym);

    Long currency = prm.getRelation(PRM_CURRENCY);

    Table<Integer, String, Double> table = HashBasedTable.create();

    Map<Long, String> locationNames = new HashMap<>();

    for (SimpleRow row : employeeData) {
      Long employeeId = row.getLong(emplIdName);
      Integer tnr = row.getInt(COL_TAB_NUMBER);

      if (DataUtils.isId(employeeId) && BeeUtils.isPositive(tnr)) {
        List<Earnings> earnings = fundsOnly
          ? new ArrayList<>() : getEarnings(ym, holidays, employeeId, null, currency);

        if (fundsOnly) {
          DateRange range = ym.getRange();
          for (SimpleRow fundRow : qs.getData(new SqlSelect()
            .addAllFields(TBL_EMPLOYEE_OBJECTS)
            .addFrom(TBL_EMPLOYEE_OBJECTS)
            .setWhere(
              SqlUtils.and(
                SqlUtils.equals(TBL_EMPLOYEE_OBJECTS, COL_EMPLOYEE, employeeId),
                SqlUtils.notNull(TBL_EMPLOYEE_OBJECTS, COL_EMPLOYEE_OBJECT_FROM),
                SqlUtils.and(
                  SqlUtils.lessEqual(TBL_EMPLOYEE_OBJECTS, COL_EMPLOYEE_OBJECT_FROM,
                    range.getMinDate()),
                  SqlUtils.or(
                  SqlUtils.moreEqual(TBL_EMPLOYEE_OBJECTS, COL_EMPLOYEE_OBJECT_UNTIL,
                    range.getMaxDate()),
                    SqlUtils.isNull(TBL_EMPLOYEE_OBJECTS, COL_EMPLOYEE_OBJECT_UNTIL)
                  )
                )
              )
            )
            )) {
            Earnings item = new Earnings(employeeId, fundRow.getLong(COL_SUBSTITUTE_FOR), fundRow
              .getLong(COL_OBJECT));
            item.setDateFrom(fundRow.getDate(COL_EMPLOYEE_OBJECT_FROM));
            item.setDateUntil(fundRow.getDate(COL_EMPLOYEE_OBJECT_UNTIL));
            item.setSalaryFund(adm.maybeExchange(fundRow.getLong(COL_CURRENCY), currency,
              fundRow.getDouble(COL_EMPLOYEE_OBJECT_FUND), null));
            item.setHourlyWage(adm.maybeExchange(fundRow.getLong(COL_CURRENCY), currency,
              fundRow.getDouble(COL_WAGE), null));

            earnings.add(item);
          }
        }

        if (!BeeUtils.isEmpty(earnings)) {
          for (Earnings item : earnings) {
            Double amount = fundsOnly ? item.getSalaryFund() : item.total();

            if (BeeUtils.isPositive(amount)) {
              Long objectId = item.getObjectId();
              String objectName = locationNames.get(objectId);

              if (BeeUtils.isEmpty(objectName) && DataUtils.isId(objectId)) {
                objectName = qs.getValueById(TBL_LOCATIONS, objectId, COL_LOCATION_NAME);
                if (!BeeUtils.isEmpty(objectName)) {
                  locationNames.put(objectId, objectName);
                }
              }

              if (!BeeUtils.isEmpty(objectName)) {
                if (table.contains(tnr, objectName)) {
                  amount += table.get(tnr, objectName);
                }

                table.put(tnr, objectName, amount);
              }
            }
          }
        }
      }
    }

    if (table.isEmpty()) {
      return ResponseObject.info("employee earnings not found", companyCode, tabNumber, ym);
    } else {
      return ResponseObject.response(table);
    }
  }

  @Override
  public Module getModule() {
    return Module.PAYROLL;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  public ResponseObject getWorkSchedule(String companyCode, Integer tabNumber, DateRange range,
      WorkScheduleKind kind) {

    if (BeeUtils.isEmpty(companyCode)) {
      return ResponseObject.error("company code not specified");
    }
    if (range == null) {
      return ResponseObject.error("date range not specified");
    }
    if (kind == null) {
      return ResponseObject.error("schedule kind not specified");
    }

    ResponseObject ecr = getEmployeeCondition(companyCode, tabNumber);
    if (ecr.hasErrors()) {
      return ecr;
    }

    if (!(ecr.getResponse() instanceof IsCondition)) {
      return ResponseObject.error("cannot filter employees", companyCode, tabNumber);
    }

    IsCondition employeeCondition = (IsCondition) ecr.getResponse();

    JustDate from = range.getMinDate();
    JustDate until = range.getMaxDate();

    SimpleRowSet tableLocks = qs.getData(new SqlSelect()
      .addAllFields(TBL_WORK_SCHEDULE_LOCKS)
      .addFrom(TBL_WORK_SCHEDULE_LOCKS)
      .addFromLeft(TBL_EMPLOYEES,
        sys.joinTables(TBL_EMPLOYEES, TBL_WORK_SCHEDULE_LOCKS, COL_EMPLOYEE))
      .addFromLeft(TBL_COMPANY_PERSONS,
        sys.joinTables(TBL_COMPANY_PERSONS, TBL_EMPLOYEES, COL_COMPANY_PERSON))
      .setWhere(SqlUtils.and(employeeCondition,
        SqlUtils.equals(TBL_WORK_SCHEDULE_LOCKS, COL_WORK_SCHEDULE_KIND, kind),
        SqlUtils.moreEqual(TBL_WORK_SCHEDULE_LOCKS, COL_WOKR_SCHEDULE_LOCK, from),
        SqlUtils.lessEqual(TBL_WORK_SCHEDULE_LOCKS, COL_WOKR_SCHEDULE_LOCK, until)))
    );

    if (DataUtils.isEmpty(tableLocks)) {
      return ResponseObject.info("work schedule not found", companyCode, tabNumber, range);
    }

    SimpleRowSet data = null;

    for (SimpleRow lock : tableLocks) {
      DateRange lockRange = YearMonth.of(lock.getDate(COL_WOKR_SCHEDULE_LOCK)).getRange();

      SqlSelect query = new SqlSelect()
        .addFields(TBL_EMPLOYEES, COL_TAB_NUMBER)
        .addFields(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE,
          COL_TIME_RANGE_CODE, COL_TIME_CARD_CODE,
          COL_WORK_SCHEDULE_FROM, COL_WORK_SCHEDULE_UNTIL, COL_WORK_SCHEDULE_DURATION)
        .addField(TBL_TIME_RANGES, COL_TR_FROM, ALS_TR_FROM)
        .addField(TBL_TIME_RANGES, COL_TR_UNTIL, ALS_TR_UNTIL)
        .addField(TBL_TIME_RANGES, COL_TR_DURATION, ALS_TR_DURATION)
        .addFields(TBL_TIME_CARD_CODES, COL_TC_CODE, COL_TC_DURATION_TYPE)
        .addFrom(TBL_WORK_SCHEDULE)
        .addFromLeft(TBL_TIME_RANGES,
          sys.joinTables(TBL_TIME_RANGES, TBL_WORK_SCHEDULE, COL_TIME_RANGE_CODE))
        .addFromLeft(TBL_TIME_CARD_CODES,
          sys.joinTables(TBL_TIME_CARD_CODES, TBL_WORK_SCHEDULE, COL_TIME_CARD_CODE))
        .addFromLeft(TBL_EMPLOYEES,
          sys.joinTables(TBL_EMPLOYEES, TBL_WORK_SCHEDULE, COL_EMPLOYEE))
        .addFromLeft(TBL_COMPANY_PERSONS,
          sys.joinTables(TBL_COMPANY_PERSONS, TBL_EMPLOYEES, COL_COMPANY_PERSON))
        .setWhere(
          SqlUtils.and(
            SqlUtils.equals(TBL_WORK_SCHEDULE, COL_EMPLOYEE, lock.getLong(COL_EMPLOYEE)),
            SqlUtils.equals(TBL_WORK_SCHEDULE, COL_OBJECT, lock.getLong(COL_OBJECT)),
            SqlUtils.equals(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_KIND, kind),
            SqlUtils.moreEqual(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE, lockRange.getMinDate()),
            SqlUtils.lessEqual(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE, lockRange.getMaxDate())
          )
        );

      if (data == null) {
        data = qs.getData(query);
      } else {
        data.append(qs.getData(query));
      }
    }
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.info("work schedule not found", companyCode, tabNumber, range);
    }

    Table<Integer, JustDate, WorkScheduleSummary> table = HashBasedTable.create();

    long duration;
    String tcCode;
    WorkScheduleSummary wss;

    for (SimpleRow row : data) {
      if (DataUtils.isId(row.getLong(COL_TIME_CARD_CODE))) {
        duration = BeeConst.LONG_UNDEF;
        tcCode = row.getValue(COL_TC_CODE);

        if (row.getEnum(COL_TC_DURATION_TYPE, TcDurationType.class) != null) {
          TcDurationType t = row.getEnum(COL_TC_DURATION_TYPE, TcDurationType.class);

          switch (t) {
            case PART_TIME:
              duration = TimeUtils.parseTime(row.getValue(COL_WORK_SCHEDULE_DURATION));
              break;
            case FULL_TIME:
              duration = TimeUtils.parseTime(TcDurationType.getDefaultTimeOfDay());
              break;
              default:
                break;
          }
        } else if (BeeUtils.isPositive(TimeUtils.parseTime(
          row.getValue(COL_WORK_SCHEDULE_DURATION)))) {
          duration = TimeUtils.parseTime(row.getValue(COL_WORK_SCHEDULE_DURATION));
        }

      } else if (DataUtils.isId(row.getLong(COL_TIME_RANGE_CODE))) {
        duration = PayrollUtils.getMillis(row.getValue(ALS_TR_FROM), row.getValue(ALS_TR_UNTIL),
            row.getValue(ALS_TR_DURATION));
        tcCode = null;

      } else {
        duration = PayrollUtils.getMillis(row.getValue(COL_WORK_SCHEDULE_FROM),
            row.getValue(COL_WORK_SCHEDULE_UNTIL), row.getValue(COL_WORK_SCHEDULE_DURATION));
        tcCode = null;
      }

      if (duration > 0 || !BeeUtils.isEmpty(tcCode)) {
        Integer tnr = row.getInt(COL_TAB_NUMBER);
        JustDate date = row.getDate(COL_WORK_SCHEDULE_DATE);

        if (BeeUtils.isPositive(tnr) && date != null) {
          if (table.contains(tnr, date)) {
            wss = table.get(tnr, date);
          } else {
            wss = new WorkScheduleSummary();
            table.put(tnr, date, wss);
          }

          if (duration > 0) {
            wss.addMillis(duration);
          }

          if (!BeeUtils.isEmpty(tcCode)) {
            wss.addTimeCardCode(tcCode);
          }
        }
      }
    }

    return ResponseObject.response(table);
  }

  @Override
  public TimerService getTimerService() {
    return timerService;
  }

  @Override
  public void init() {
    cb.createIntervalTimer(this.getClass(), PRM_ERP_SYNC_PAYROLL_DATA + SFX_TIME);
    cb.createCalendarTimer(this.getClass(), PRM_ERP_SYNC_PAYROLL_DATA + SFX_HOURS);
  }

  public void importERPData() {
    if (BeeUtils.isFalse(prm.getBoolean(PRM_ERP_SYNC_EMPLOYEES))
      && BeeUtils.isFalse(prm.getBoolean(PRM_ERP_SYNC_EMPLOYEES))
      && BeeUtils.isFalse(prm.getBoolean(PRM_ERP_SYNC_EMPLOYEES))) {
      logger.warning(PRM_ERP_SYNC_PAYROLL_DATA, "nothing not configure");
      return;
    }

    long historyId = BeeConst.UNDEF;

    Long companyId = prm.getRelation(PRM_COMPANY);
    String erpAddress = prm.getValue(PRM_ERP_ADDRESS);
    String erpLogin = prm.getValue(PRM_ERP_LOGIN);
    String erpPassword = prm.getValue(PRM_ERP_PASSWORD);
    DateTime lastSyncTime = getErpLastSyncDate();

    Map<String, Long> departments = getReferences(TBL_COMPANY_DEPARTMENTS, "Name",
      SqlUtils.equals(TBL_COMPANY_DEPARTMENTS, COL_COMPANY, companyId));
    Map<String, Long> positions = getReferences(TBL_POSITIONS, COL_POSITION_NAME);
    SimpleRowSet employees = getEmployees(companyId);
    StringBuilder syncLog = new StringBuilder();

    syncLog.append(erpAddress + BeeConst.STRING_EOL);

    try {
      historyId = sys.eventStart(PRM_ERP_SYNC_PAYROLL_DATA);
      if (BeeUtils.isTrue(prm.getBoolean(PRM_ERP_SYNC_EMPLOYEES))) {
        SimpleRowSet erpEmployees = getErpEmployees(historyId, erpAddress, erpLogin, erpPassword,
          lastSyncTime);

        if (erpEmployees == null || erpEmployees.isEmpty()) {
          syncLog.append(BeeUtils.joinWords("Employees data ", lastSyncTime.toString(), "empty")
            + BeeConst.STRING_EOL);
        } else {
          syncEmployees(companyId, erpEmployees, employees, departments, positions, syncLog);
        }
      }

      if (BeeUtils.isTrue(prm.getBoolean(PRM_ERP_SYNC_LOCATIONS))) {
        SimpleRowSet erpLocations = ButentWS.connect(erpAddress, erpLogin, erpPassword)
          .getObjects();

        int locNew = 0;
        Map<String, Long> locations = getReferences(TBL_LOCATIONS, COL_LOCATION_NAME);

        for (SimpleRow row : erpLocations) {
          String location = row.getValue("objektas");

          if (!locations.containsKey(location)) {
            locations.put(location, qs.insertData(new SqlInsert(TBL_LOCATIONS)
              .addConstant(COL_LOCATION_NAME, location)
              .addConstant(COL_LOCATION_STATUS, ObjectStatus.INACTIVE.ordinal())));
            locNew++;
          }
        }
        syncLog.append(BeeConst.STRING_EOL + "Locations created " + locNew);
      }

      if (BeeUtils.isTrue(prm.getBoolean(PRM_ERP_SYNC_TIME_CARDS))) {
        SimpleRowSet erpTimeCards = ButentWS.connect(erpAddress, erpLogin, erpPassword)
          .getTimeCards(lastSyncTime);

        importTimeCards(companyId, erpTimeCards, syncLog);
      }
    } catch (Throwable e) {
      ctx.setRollbackOnly();

      if (!BeeConst.isUndef(historyId)) {
        sys.eventError(historyId, e, syncLog.toString());
      }

      logger.error(e);
      return;
    }

    if (!BeeConst.isUndef(historyId)) {
      sys.eventEnd(historyId, EVENT_STATUS_OK, syncLog.toString());
    }
  }

  private static IsCondition getScheduleKindCondition(WorkScheduleKind kind) {
    return SqlUtils.equals(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_KIND, kind);
  }

  private ResponseObject getEarnings(RequestInfo reqInfo) {
    Integer year = reqInfo.getParameterInt(Service.VAR_YEAR);
    if (!TimeUtils.isYear(year)) {
      return ResponseObject.parameterNotFound(reqInfo.getSubService(), Service.VAR_YEAR);
    }

    Integer month = reqInfo.getParameterInt(Service.VAR_MONTH);
    if (!TimeUtils.isMonth(month)) {
      return ResponseObject.parameterNotFound(reqInfo.getSubService(), Service.VAR_MONTH);
    }

    Long employeeId = reqInfo.getParameterLong(COL_EMPLOYEE);
    Long objectId = reqInfo.getParameterLong(COL_PAYROLL_OBJECT);

    YearMonth ym = new YearMonth(year, month);
    Set<JustDate> holidays = getHolidays(ym);

    Long currency = prm.getRelation(PRM_CURRENCY);

    List<Earnings> result = getEarnings(ym, holidays, employeeId, objectId, currency);

    if (result.isEmpty()) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.responseWithSize(result);
    }
  }

  private List<Earnings> getEarnings(YearMonth ym, Set<JustDate> holidays,
      Long employeeId, Long objectId, Long currency) {

    List<Earnings> result = new ArrayList<>();

    JustDate from = ym.getDate();
    JustDate until = ym.getLast();

    IsCondition wsCondition = getScheduleEarningsCondition(from, until, employeeId, null, objectId);

    SqlSelect query = new SqlSelect().setDistinctMode(true)
        .addFields(TBL_WORK_SCHEDULE, COL_EMPLOYEE, COL_SUBSTITUTE_FOR, COL_PAYROLL_OBJECT)
        .addFrom(TBL_WORK_SCHEDULE)
        .setWhere(wsCondition);

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        Long empl = row.getLong(COL_EMPLOYEE);
        Long subst = row.getLong(COL_SUBSTITUTE_FOR);
        Long obj = row.getLong(COL_PAYROLL_OBJECT);

        Map<DateRange, Pair<Double, Double>> fundsAndWages = getFundsAndWages(from, until,
            empl, subst, obj, currency);

        fundsAndWages.forEach((range, pair) -> {
          Earnings earnings = new Earnings(empl, subst, obj);

          earnings.setDateFrom(range.getMinDate());
          earnings.setDateUntil(range.getMaxDate());

          if (pair != null) {
            earnings.setSalaryFund(pair.getA());
            earnings.setHourlyWage(pair.getB());
          }

          setScheduledTime(earnings, holidays);

          result.add(earnings);
        });
      }
    }

    return result;
  }

  private SimpleRowSet getEmployees(Long company) {
    return qs.getData(new SqlSelect()
      .addField(TBL_COMPANY_PERSONS, sys.getIdName(TBL_COMPANY_PERSONS), COL_COMPANY_PERSON)
      .addFields(TBL_COMPANY_PERSONS, COL_PERSON, COL_CONTACT)
      .addExpr(SqlUtils.concat(SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME), "' '",
        SqlUtils.field(TBL_PERSONS, COL_LAST_NAME)), COL_FIRST_NAME)
      .addField(TBL_PERSONS, COL_CONTACT, COL_PERSON + COL_CONTACT)
      .addFields(TBL_EMPLOYEES, COL_TAB_NUMBER)
      .addField(TBL_EMPLOYEES, sys.getIdName(TBL_EMPLOYEES), COL_EMPLOYEE)
      .addFrom(TBL_COMPANY_PERSONS)
      .addFromInner(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
      .addFromLeft(TBL_EMPLOYEES,
        sys.joinTables(TBL_COMPANY_PERSONS, TBL_EMPLOYEES, COL_COMPANY_PERSON))
      .setWhere(SqlUtils.equals(TBL_COMPANY_PERSONS, COL_COMPANY, company)));
  }

  private ResponseObject getEmployeeCondition(String companyCode, Integer tabNumber) {
    SqlSelect companyQuery = new SqlSelect()
        .addFields(TBL_COMPANIES, sys.getIdName(TBL_COMPANIES))
        .addFrom(TBL_COMPANIES)
        .setWhere(SqlUtils.equals(TBL_COMPANIES, COL_COMPANY_CODE, companyCode));

    List<Long> companies = qs.getLongList(companyQuery);

    if (BeeUtils.isEmpty(companies)) {
      return ResponseObject.error("Company code", companyCode, "not found");

    } else if (companies.size() > 1) {
      return ResponseObject.error("Company code", companyCode, "found", companies.size(),
          "companies", companies);

    } else {
      Long company = companies.get(0);

      IsCondition companyWhere = SqlUtils.equals(TBL_COMPANY_PERSONS, COL_COMPANY, company);

      if (BeeUtils.isPositive(tabNumber)) {
        return ResponseObject.response(SqlUtils.and(companyWhere,
            SqlUtils.equals(TBL_EMPLOYEES, COL_TAB_NUMBER, tabNumber)));

      } else {
        return ResponseObject.response(companyWhere);
      }
    }
  }

  private Map<DateRange, Pair<Double, Double>> getFundsAndWages(JustDate from, JustDate until,
      Long employeeId, Long substituteFor, Long objectId, Long currency) {

    RangeMap<JustDate, Double> funds = RangeMap.create();
    RangeMap<JustDate, Double> wages = RangeMap.create();

    SimpleRow emplRow = qs.getRow(TBL_EMPLOYEES, BeeUtils.nvl(substituteFor, employeeId));
    if (emplRow != null) {
      Double salary = emplRow.getDouble(COL_SALARY);
      if (BeeUtils.isPositive(salary)) {
        Double wage = adm.maybeExchange(emplRow.getLong(COL_CURRENCY), currency, salary, null);

        if (BeeUtils.isPositive(wage)) {
          wages.put(DateRange.all(), wage);
        }
      }
    }

    HasConditions where = SqlUtils.and();
    where.add(SqlUtils.equals(TBL_EMPLOYEE_OBJECTS, COL_PAYROLL_OBJECT, objectId));

    boolean substitution = DataUtils.isId(substituteFor)
        && !Objects.equals(employeeId, substituteFor);

    if (substitution) {
      where.add(SqlUtils.or(
          SqlUtils.equals(TBL_EMPLOYEE_OBJECTS, COL_EMPLOYEE, employeeId,
              COL_SUBSTITUTE_FOR, substituteFor),
          SqlUtils.and(SqlUtils.equals(TBL_EMPLOYEE_OBJECTS, COL_EMPLOYEE, substituteFor),
              SqlUtils.or(SqlUtils.isNull(TBL_EMPLOYEE_OBJECTS, COL_SUBSTITUTE_FOR),
                  SqlUtils.equals(TBL_EMPLOYEE_OBJECTS, COL_SUBSTITUTE_FOR, substituteFor)))));

    } else {
      where.add(SqlUtils.equals(TBL_EMPLOYEE_OBJECTS, COL_EMPLOYEE, employeeId));
      where.add(SqlUtils.or(SqlUtils.isNull(TBL_EMPLOYEE_OBJECTS, COL_SUBSTITUTE_FOR),
          SqlUtils.equals(TBL_EMPLOYEE_OBJECTS, COL_SUBSTITUTE_FOR, employeeId)));
    }

    where.add(SqlUtils.or(SqlUtils.positive(TBL_EMPLOYEE_OBJECTS, COL_EMPLOYEE_OBJECT_FUND),
        SqlUtils.positive(TBL_EMPLOYEE_OBJECTS, COL_WAGE)));

    SqlSelect query = new SqlSelect()
        .addFields(TBL_EMPLOYEE_OBJECTS, COL_EMPLOYEE, COL_SUBSTITUTE_FOR,
            COL_EMPLOYEE_OBJECT_FROM, COL_EMPLOYEE_OBJECT_UNTIL,
            COL_EMPLOYEE_OBJECT_FUND, COL_WAGE, COL_CURRENCY)
        .addFrom(TBL_EMPLOYEE_OBJECTS)
        .setWhere(where);

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      DateTime rateDt = TimeUtils.startOfDay(until);

      boolean foundFund = false;
      boolean foundWage = false;

      for (int i = 0; i < 2; i++) {
        for (SimpleRow row : data) {
          JustDate min = BeeUtils.max(row.getDate(COL_EMPLOYEE_OBJECT_FROM), from);
          JustDate max = BeeUtils.min(row.getDate(COL_EMPLOYEE_OBJECT_UNTIL), until);

          if (DateRange.isValidClosedRange(min, max)) {
            if (substitution) {
              boolean match = Objects.equals(row.getLong(COL_EMPLOYEE), employeeId)
                  && Objects.equals(row.getLong(COL_SUBSTITUTE_FOR), substituteFor);

              if (match != (i == 0)) {
                continue;
              }
            }

            Long cFr = row.getLong(COL_CURRENCY);

            if (i == 0 || !foundFund) {
              Double fund = adm.maybeExchange(cFr, currency,
                  row.getDouble(COL_EMPLOYEE_OBJECT_FUND), rateDt);

              if (BeeUtils.isPositive(fund)) {
                funds.put(DateRange.closed(min, max), fund);
                foundFund = true;
              }
            }

            if (i == 0 || !foundWage) {
              Double wage = adm.maybeExchange(cFr, currency, row.getDouble(COL_WAGE), rateDt);
              if (BeeUtils.isPositive(wage)) {
                wages.put(DateRange.closed(min, max), wage);
                foundWage = true;
              }
            }
          }
        }

        if (!substitution || foundFund && foundWage) {
          break;
        }
      }
    }

    Map<DateRange, Pair<Double, Double>> fundsAndWages = new HashMap<>();

    if (!funds.isEmpty() || !wages.isEmpty()) {
      JustDate lower = JustDate.copyOf(from);
      Double fund = funds.get(lower);
      Double wage = wages.get(lower);

      for (int d = from.getDays() + 1; d <= until.getDays(); d++) {
        JustDate date = new JustDate(d);

        Double f = funds.get(date);
        Double w = wages.get(date);

        if (!Objects.equals(fund, f) || !Objects.equals(wage, w)) {
          if (BeeUtils.isPositive(fund) || BeeUtils.isPositive(wage)) {
            fundsAndWages.put(DateRange.closed(lower, new JustDate(d - 1)), Pair.of(fund, wage));
          }

          lower = date;
          fund = f;
          wage = w;
        }
      }

      if (BeeUtils.isPositive(fund) || BeeUtils.isPositive(wage)) {
        fundsAndWages.put(DateRange.closed(lower, until), Pair.of(fund, wage));
      }
    }

    if (fundsAndWages.isEmpty()) {
      Double fund = null;
      Double wage = null;

      fundsAndWages.put(DateRange.closed(from, until), Pair.of(fund, wage));
    }

    return fundsAndWages;
  }

  private Set<JustDate> getHolidays(YearMonth ym) {
    return getHolidays(ym.getDate(), ym.getLast());
  }

  private Set<JustDate> getHolidays(JustDate from, JustDate until) {
    Set<JustDate> holidays = new HashSet<>();

    Long country = prm.getRelation(PRM_COUNTRY);

    if (DataUtils.isId(country)) {
      HasConditions where = SqlUtils.and();
      where.add(SqlUtils.equals(TBL_HOLIDAYS, COL_HOLY_COUNTRY, country));

      if (from != null) {
        where.add(SqlUtils.moreEqual(TBL_HOLIDAYS, COL_HOLY_DAY, from.getDays()));
      }
      if (until != null) {
        where.add(SqlUtils.lessEqual(TBL_HOLIDAYS, COL_HOLY_DAY, until.getDays()));
      }

      SqlSelect holidayQuery = new SqlSelect()
          .addFields(TBL_HOLIDAYS, COL_HOLY_DAY)
          .addFrom(TBL_HOLIDAYS)
          .setWhere(where);

      Integer[] days = qs.getIntColumn(holidayQuery);
      if (days != null) {
        for (Integer day : days) {
          if (BeeUtils.isPositive(day)) {
            holidays.add(new JustDate(day));
          }
        }
      }
    }

    return holidays;
  }

  private SimpleRowSet getErpEmployees(long historyId, String erpAddress, String erpLogin,
                                       String erpPassword, DateTime lastSyncDate) {
    try {
      return ButentWS.connect(erpAddress, erpLogin, erpPassword).getEmployees(lastSyncDate);
    } catch (BeeException e) {
      ctx.setRollbackOnly();
      sys.eventError(historyId, e);
      return null;
    }
  }

  private DateTime getErpLastSyncDate() {
    DateTime lastSyncTime = qs.getDateTime(new SqlSelect()
      .addMax(TBL_EVENT_HISTORY, COL_EVENT_STARTED)
      .addFrom(TBL_EVENT_HISTORY)
      .setWhere(SqlUtils.and(SqlUtils.equals(TBL_EVENT_HISTORY, COL_EVENT,
        PRM_ERP_SYNC_PAYROLL_DATA),
        SqlUtils.startsWith(TBL_EVENT_HISTORY, COL_EVENT_RESULT, EVENT_STATUS_OK))));

    Integer delta = prm.getInteger(PRM_ERP_SYNC_PAYROLL_DELTA_HOURS);

    if (lastSyncTime != null && BeeUtils.isPositive(delta)) {
      lastSyncTime = TimeUtils.nextHour(lastSyncTime, -delta);
    }

    return lastSyncTime;
  }

  private static IsCondition getScheduleEarningsCondition(JustDate from, JustDate until,
      Long employeeId, Long substituteFor, Long objectId) {

    HasConditions conditions = SqlUtils.and();

    if (from != null) {
      conditions.add(SqlUtils.moreEqual(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE, from));
    }
    if (until != null) {
      conditions.add(SqlUtils.lessEqual(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE, until));
    }

    if (DataUtils.isId(employeeId)) {
      IsCondition emplCondition = SqlUtils.equals(TBL_WORK_SCHEDULE, COL_EMPLOYEE, employeeId);

      if (DataUtils.isId(substituteFor) && !Objects.equals(employeeId, substituteFor)) {
        IsCondition substCondition = SqlUtils.equals(TBL_WORK_SCHEDULE, COL_SUBSTITUTE_FOR,
            substituteFor);

        if (WorkScheduleKind.PLANNED.isSubstitutionEnabled()) {
          conditions.add(emplCondition);
          conditions.add(substCondition);

        } else {
          conditions.add(
              SqlUtils.or(
                  SqlUtils.and(getScheduleKindCondition(WorkScheduleKind.PLANNED),
                      SqlUtils.equals(TBL_WORK_SCHEDULE, COL_EMPLOYEE, substituteFor)),
                  SqlUtils.and(getScheduleKindCondition(WorkScheduleKind.ACTUAL),
                      emplCondition, substCondition)));
        }

      } else {
        conditions.add(emplCondition);
      }
    }

    if (DataUtils.isId(objectId)) {
      conditions.add(SqlUtils.equals(TBL_WORK_SCHEDULE, COL_PAYROLL_OBJECT, objectId));
    }

    conditions.add(SqlUtils.or(
        SqlUtils.notNull(TBL_WORK_SCHEDULE, COL_TIME_RANGE_CODE),
        SqlUtils.notNull(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_FROM, COL_WORK_SCHEDULE_UNTIL),
        SqlUtils.notNull(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DURATION)));

    return conditions;
  }

  private Map<String, Long> getReferences(String tableName, String keyName) {
    return getReferences(tableName, keyName, null);
  }

  private Map<String, Long> getReferences(String tableName, String keyName, IsCondition clause) {
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

  private ResponseObject getScheduledMonths(RequestInfo reqInfo) {
    Long employeeId = reqInfo.getParameterLong(COL_EMPLOYEE);
    Long objectId = reqInfo.getParameterLong(COL_PAYROLL_OBJECT);

    IsCondition where = getScheduleEarningsCondition(null, null, employeeId, null, objectId);

    SqlSelect query = new SqlSelect().setDistinctMode(true)
        .addFields(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE)
        .addFrom(TBL_WORK_SCHEDULE)
        .setWhere(where);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.emptyResponse();
    }

    List<YearMonth> months = new ArrayList<>();

    for (SimpleRow row : data) {
      JustDate date = row.getDate(COL_WORK_SCHEDULE_DATE);

      if (date != null) {
        YearMonth ym = new YearMonth(date);
        if (!months.contains(ym)) {
          months.add(ym);
        }
      }
    }

    if (months.size() > 1) {
      Collections.sort(months);
    }

    StringBuilder sb = new StringBuilder();

    for (YearMonth ym : months) {
      if (sb.length() > 0) {
        sb.append(BeeConst.CHAR_COMMA);
      }
      sb.append(ym.serialize());
    }

    return ResponseObject.response(sb.toString()).setSize(months.size());
  }

  private ResponseObject getScheduleOverlap(RequestInfo reqInfo) {
    String relationColumn = reqInfo.getParameter(Service.VAR_COLUMN);
    if (BeeUtils.isEmpty(relationColumn)) {
      return ResponseObject.parameterNotFound(reqInfo.getSubService(), Service.VAR_COLUMN);
    }

    Long relId = reqInfo.getParameterLong(Service.VAR_VALUE);
    if (!DataUtils.isId(relId)) {
      return ResponseObject.parameterNotFound(reqInfo.getSubService(), Service.VAR_VALUE);
    }

    WorkScheduleKind kind = EnumUtils.getEnumByIndex(WorkScheduleKind.class,
        reqInfo.getParameter(COL_WORK_SCHEDULE_KIND));
    if (kind == null) {
      return ResponseObject.parameterNotFound(reqInfo.getSubService(), COL_WORK_SCHEDULE_KIND);
    }

    String partitionColumn;
    switch (relationColumn) {
      case COL_PAYROLL_OBJECT:
        partitionColumn = COL_EMPLOYEE;
        break;

      case COL_EMPLOYEE:
        partitionColumn = COL_PAYROLL_OBJECT;
        break;

      default:
        return ResponseObject.error(reqInfo.getSubService(), "unrecognized relation column",
            relationColumn);
    }

    Integer from = reqInfo.getParameterInt(Service.VAR_FROM);
    JustDate dateFrom = (from == null) ? null : new JustDate(from);

    Integer to = reqInfo.getParameterInt(Service.VAR_TO);
    JustDate dateUntil = (to == null) ? null : new JustDate(to);

    HasConditions wsWhere = SqlUtils.and();
    wsWhere.add(SqlUtils.equals(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_KIND, kind));

    if (dateFrom != null) {
      wsWhere.add(SqlUtils.moreEqual(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE, dateFrom));
    }
    if (dateUntil != null) {
      wsWhere.add(SqlUtils.lessEqual(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE, dateUntil));
    }

    IsCondition relWhere = SqlUtils.equals(TBL_WORK_SCHEDULE, relationColumn, relId);

    SqlSelect subQuery = new SqlSelect().setDistinctMode(true)
        .addFields(TBL_WORK_SCHEDULE, partitionColumn, COL_WORK_SCHEDULE_DATE)
        .addFrom(TBL_WORK_SCHEDULE);

    String subAlias = SqlUtils.uniqueName();

    SqlSelect query = new SqlSelect().setDistinctMode(true)
        .addFields(TBL_WORK_SCHEDULE, partitionColumn, COL_WORK_SCHEDULE_DATE)
        .addFrom(TBL_WORK_SCHEDULE);

    switch (relationColumn) {
      case COL_PAYROLL_OBJECT:
        subQuery.setWhere(SqlUtils.and(SqlUtils.notEqual(TBL_WORK_SCHEDULE, relationColumn, relId),
            wsWhere));

        query.addFromInner(subQuery, subAlias, SqlUtils.joinUsing(TBL_WORK_SCHEDULE, subAlias,
            partitionColumn, COL_WORK_SCHEDULE_DATE));
        query.setWhere(SqlUtils.and(relWhere, wsWhere));
        break;

      case COL_EMPLOYEE:
        subQuery.setWhere(SqlUtils.and(relWhere, wsWhere));

        SqlSelect datesQuery = new SqlSelect()
            .addFields(subAlias, COL_WORK_SCHEDULE_DATE)
            .addFrom(subQuery, subAlias)
            .addGroup(subAlias, COL_WORK_SCHEDULE_DATE)
            .setHaving(SqlUtils.more(SqlUtils.aggregate(SqlFunction.COUNT, null), 1));

        String datesAlias = SqlUtils.uniqueName();

        query.addFromInner(datesQuery, datesAlias,
            SqlUtils.joinUsing(TBL_WORK_SCHEDULE, datesAlias, COL_WORK_SCHEDULE_DATE));
        query.setWhere(relWhere);
        break;
    }

    SimpleRowSet candidates = qs.getData(query);
    if (DataUtils.isEmpty(candidates)) {
      return ResponseObject.emptyResponse();
    }

    Multimap<Long, Integer> overlap = HashMultimap.create();

    long objId = BeeConst.LONG_UNDEF;
    long emplId = BeeConst.LONG_UNDEF;

    switch (relationColumn) {
      case COL_PAYROLL_OBJECT:
        objId = relId;
        break;

      case COL_EMPLOYEE:
        emplId = relId;
        break;
    }

    for (SimpleRow row : candidates) {
      Long partId = row.getLong(partitionColumn);
      JustDate date = row.getDate(COL_WORK_SCHEDULE_DATE);

      if (DataUtils.isId(partId) && date != null && date.getDays() > 0) {
        switch (relationColumn) {
          case COL_PAYROLL_OBJECT:
            emplId = partId;
            break;

          case COL_EMPLOYEE:
            objId = partId;
            break;
        }

        if (overlaps(objId, emplId, date, kind)) {
          overlap.put(partId, -date.getDays());
        } else {
          overlap.put(partId, date.getDays());
        }
      }
    }

    if (overlap.isEmpty()) {
      return ResponseObject.emptyResponse();

    } else {
      StringBuilder sb = new StringBuilder();

      for (long partId : overlap.keySet()) {
        if (sb.length() > 0) {
          sb.append(BeeConst.DEFAULT_ROW_SEPARATOR);
        }
        sb.append(BeeUtils.join(BeeConst.DEFAULT_VALUE_SEPARATOR,
            partId, BeeUtils.joinInts(overlap.get(partId))));
      }

      return ResponseObject.response(sb.toString());
    }
  }

  private ResponseObject getTimeSheetPayrollReportData(RequestInfo reqInfo) {
    ResponseObject resp;
    Long currency = reqInfo.getParameterLong(COL_CURRENCY);
    ReportInfo report = ReportInfo.restore(reqInfo.getParameter(Service.VAR_DATA));

    if (!DataUtils.isId(currency)) {
      currency = prm.getRelation(PRM_CURRENCY);
    }

    YearMonth filterFrom = null;
    YearMonth filterTo = null;

    for (ReportItem item : report.getFilterItems()) {
      if (item instanceof ReportDateItem) {
        ReportDateItem dtItem = (ReportDateItem) item;

        if (BeeUtils.same(dtItem.getExpression(), COL_DATE_FROM) && dtItem.getFilter() != null) {
          filterFrom = new YearMonth(new JustDate(BeeUtils.toInt(dtItem.getFilter())));
        } else if (BeeUtils.same(dtItem.getExpression(), COL_DATE_TO)
          && dtItem.getFilter() != null) {
          filterTo = new YearMonth(new JustDate(BeeUtils.toInt(dtItem.getFilter())));
        }
      }
    }

    if (filterFrom == null || filterTo == null || filterFrom.compareTo(filterTo) > 0) {
      return ResponseObject.error(usr.getDictionary().invalidPeriod(filterFrom, filterTo));
    }


    String currencyName = qs.getValue(new SqlSelect().addFields(TBL_CURRENCIES, COL_CURRENCY_NAME)
      .addFrom(TBL_CURRENCIES).setWhere(sys.idEquals(TBL_CURRENCIES, currency)));

    String tblMangerPerson = SqlUtils.uniqueName();
    String tblManagerCompanyPerson = SqlUtils.uniqueName();
    String[] reportTableHeader = new String[]{
      ALS_REPORT_TIME_PERIOD, COL_OSF_AMOUNT, TradeConstants.COL_TRADE_PAID, COL_CURRENCY,
      TradeConstants.COL_TRADE_DEBT, COL_LOCATION_NAME, COL_LOCATION_STATUS, COL_COMPANY_CODE,
      COL_COMPANY_NAME, ALS_COMPANY_TYPE_NAME, COL_FIRST_NAME, COL_LAST_NAME,
      ALS_LOCATION_MANAGER_FIRST_NAME, ALS_LOCATION_MANAGER_LAST_NAME,
      FinanceConstants.ALS_EMPLOYEE_FIRST_NAME, FinanceConstants.ALS_EMPLOYEE_LAST_NAME,
    };

    TreeBasedTable<YearMonth, Long, Double> fundAmounts = TreeBasedTable.create();
    TreeBasedTable<YearMonth, String, Double> employeeAmounts = TreeBasedTable.create();
    TreeBasedTable<YearMonth, Long, Double> fundDebt = TreeBasedTable.create();
    TreeBasedTable<YearMonth, Long, Double> objectAmounts = TreeBasedTable.create();
    Multimap<YearMonth, Long> relObjects = HashMultimap.create();
    Multimap<YearMonth, Long> relEmployees = HashMultimap.create();
    Multimap<Long, Long> objectEmployees = HashMultimap.create();
    Multimap<Long, Long> employeeObjects = HashMultimap.create();

    SqlSelect objectFunds = new SqlSelect()
      .addAllFields(TBL_OBJECT_SALARY_FUND)
      .addFields(TBL_LOCATIONS, COL_LOCATION_NAME, COL_LOCATION_STATUS)
      .addFields(TBL_COMPANIES, COL_COMPANY_CODE, COL_COMPANY_NAME)
      .addField(TBL_COMPANY_TYPES, COL_COMPANY_TYPE_NAME, ALS_COMPANY_TYPE_NAME)
      .addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME)
      .addField(tblMangerPerson, COL_FIRST_NAME, ALS_LOCATION_MANAGER_FIRST_NAME)
      .addField(tblMangerPerson, COL_LAST_NAME, ALS_LOCATION_MANAGER_LAST_NAME)
      .addFrom(TBL_OBJECT_SALARY_FUND)
      .addFromLeft(TBL_LOCATIONS, sys.joinTables(TBL_LOCATIONS, TBL_OBJECT_SALARY_FUND,
        COL_OBJECT))
      .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, TBL_LOCATIONS, COL_COMPANY))
      .addFromLeft(TBL_COMPANY_TYPES, sys.joinTables(TBL_COMPANY_TYPES, TBL_COMPANIES,
        COL_COMPANY_TYPE))
      .addFromLeft(TBL_COMPANY_PERSONS, sys.joinTables(TBL_COMPANY_PERSONS, TBL_LOCATIONS,
        COL_COMPANY_PERSON))
      .addFromLeft(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
      .addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, TBL_LOCATIONS, COL_LOCATION_MANAGER))
      .addFromLeft(TBL_COMPANY_PERSONS, tblManagerCompanyPerson,
        sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS, COL_COMPANY_PERSON))
      .addFromLeft(TBL_PERSONS, tblMangerPerson, sys.joinTables(TBL_PERSONS,
        tblManagerCompanyPerson, COL_PERSON))
      .setWhere(
        SqlUtils.and(
          SqlUtils.notNull(TBL_OBJECT_SALARY_FUND, COL_OSF_YEAR_FROM),
          SqlUtils.notNull(TBL_OBJECT_SALARY_FUND, COL_OSF_MONTH_FROM),
          SqlUtils.notNull(TBL_OBJECT_SALARY_FUND, COL_OSF_YEAR_UNTIL),
          SqlUtils.notNull(TBL_OBJECT_SALARY_FUND, COL_OSF_MONTH_UNTIL),
          SqlUtils.and(
            SqlUtils.or(
              SqlUtils.more(TBL_OBJECT_SALARY_FUND, COL_OSF_YEAR_FROM, filterFrom.getYear()),
              SqlUtils.and(
                SqlUtils.equals(TBL_OBJECT_SALARY_FUND, COL_OSF_YEAR_FROM, filterFrom.getYear()),
                SqlUtils.moreEqual(TBL_OBJECT_SALARY_FUND, COL_OSF_MONTH_FROM,
                  filterFrom.getMonth())
              )
            ),
            SqlUtils.or(
              SqlUtils.less(TBL_OBJECT_SALARY_FUND, COL_OSF_YEAR_UNTIL, filterTo.getYear()),
              SqlUtils.and(
                SqlUtils.equals(TBL_OBJECT_SALARY_FUND, COL_OSF_YEAR_UNTIL, filterTo.getYear()),
                SqlUtils.lessEqual(TBL_OBJECT_SALARY_FUND, COL_OSF_MONTH_UNTIL,
                  filterTo.getMonth())
              )
            )
          )
        )
      )
      .addOrder(TBL_OBJECT_SALARY_FUND, COL_OSF_YEAR_FROM, COL_OSF_MONTH_FROM);


    SqlSelect employeeObjectsQuery = new SqlSelect()
      .addAllFields(TBL_EMPLOYEE_OBJECTS)
      .addField(TBL_PERSONS, COL_FIRST_NAME, FinanceConstants.ALS_EMPLOYEE_FIRST_NAME)
      .addField(TBL_PERSONS, COL_LAST_NAME, FinanceConstants.ALS_EMPLOYEE_LAST_NAME)
      .addFrom(TBL_EMPLOYEE_OBJECTS)
      .addFromLeft(TBL_EMPLOYEES, sys.joinTables(TBL_EMPLOYEES, TBL_EMPLOYEE_OBJECTS, COL_EMPLOYEE))
      .addFromLeft(TBL_COMPANY_PERSONS, sys.joinTables(TBL_COMPANY_PERSONS, TBL_EMPLOYEES,
        COL_COMPANY_PERSON))
      .addFromLeft(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
      .addOrder(TBL_EMPLOYEE_OBJECTS, COL_DATE_FROM);


    SimpleRowSet objectFundData = qs.getData(objectFunds);

    if (DataUtils.isEmpty(objectFundData)) {
      return ResponseObject.emptyResponse();
    }

    employeeObjectsQuery.setWhere(SqlUtils.inList(TBL_EMPLOYEE_OBJECTS, COL_OBJECT,
      (Object[]) objectFundData.getColumn(COL_OBJECT)));

    SimpleRowSet employeeObjectData = qs.getData(employeeObjectsQuery);
    SimpleRowSet reportData = new SimpleRowSet(reportTableHeader);
    YearMonth minDate = null;
    YearMonth maxDate = null;
    Map<Long, YearMonth> minObjectDates = new HashMap<>();
    Map<Long, YearMonth> maxObjectDates = new HashMap<>();

    for (SimpleRow row : objectFundData) {
      Long objectId = row.getLong(COL_OBJECT);

      if (!DataUtils.isId(objectId)) {
        continue;
      }

      Range<YearMonth> fundRange =
        Range.closedOpen(
          new YearMonth(row.getInt(COL_OSF_YEAR_FROM), row.getInt(COL_OSF_MONTH_FROM)),
          new YearMonth(row.getInt(COL_OSF_YEAR_UNTIL), row.getInt(COL_OSF_MONTH_UNTIL))
        );

      if (minDate == null) {
        minDate = fundRange.lowerEndpoint();
        maxDate = fundRange.upperEndpoint();
      } else if (maxDate.compareTo(fundRange.upperEndpoint()) < 0) {
        maxDate = fundRange.upperEndpoint();
      }

      YearMonth minObjectDate = minObjectDates.get(objectId);
      YearMonth maxObjectDate = maxObjectDates.get(objectId);

      if (minObjectDate == null) {
        minObjectDates.put(objectId, fundRange.lowerEndpoint());
        maxObjectDates.put(objectId, fundRange.upperEndpoint());
      } else if (maxObjectDate.compareTo(fundRange.upperEndpoint()) < 0) {
        maxObjectDates.put(objectId, fundRange.upperEndpoint());
      }

      for (YearMonth startFoundRange = fundRange.lowerEndpoint();
           fundRange.contains(startFoundRange);
           startFoundRange = startFoundRange.nextMonth()) {
        double fundAmount = BeeUtils.unbox(fundAmounts.get(startFoundRange, objectId));
        double newAmount = BeeUtils.unbox(
          adm.maybeExchange(row.getLong(COL_CURRENCY), currency, row.getDouble(COL_OSF_AMOUNT),
            startFoundRange.getDate().getDateTime()));

        relObjects.put(startFoundRange, objectId);
        fundAmounts.put(startFoundRange, objectId, fundAmount + newAmount);
      }
    }

    for (SimpleRow row : employeeObjectData) {
      Long objectId = row.getLong(COL_OBJECT);
      Long employeeId = row.getLong(COL_EMPLOYEE);

      if (!DataUtils.isId(objectId) || !DataUtils.isId(employeeId)) {
        continue;
      }

      String amountKey = BeeUtils.joinItems(objectId, employeeId);
      objectEmployees.put(objectId, employeeId);
      employeeObjects.put(employeeId, objectId);

      YearMonth employeeWorkFrom = row.getDate(COL_EMPLOYEE_OBJECT_FROM) == null
        ? minObjectDates.get(objectId) : new YearMonth(row.getDate(COL_EMPLOYEE_OBJECT_FROM));
      YearMonth employeeWorkTo = row.getDate(COL_EMPLOYEE_OBJECT_UNTIL) == null
        ? maxObjectDates.get(objectId) : new YearMonth(row.getDate(COL_EMPLOYEE_OBJECT_UNTIL));

      if (employeeWorkFrom.compareTo(employeeWorkTo) > 0) {
       employeeWorkTo = employeeWorkFrom;
      }

      Range<YearMonth> workRange = Range.closedOpen(employeeWorkFrom, employeeWorkTo);

      if (minDate.compareTo(workRange.lowerEndpoint()) > 0) {
        minDate = workRange.lowerEndpoint();
      }

      if (maxDate.compareTo(workRange.upperEndpoint()) < 0) {
        maxDate = workRange.upperEndpoint();
      }

      for (YearMonth startWork = workRange.lowerEndpoint(); workRange.contains(startWork);
           startWork = startWork.nextMonth()) {

        double salary = BeeUtils.unbox(employeeAmounts.get(startWork, amountKey));
        double newSalary = BeeUtils.unbox(
          adm.maybeExchange(row.getLong(COL_CURRENCY), currency,
            row.getDouble(COL_EMPLOYEE_OBJECT_FUND), startWork.getDate().getDateTime()));
        double objectAmount = BeeUtils.unbox(objectAmounts.get(startWork, objectId));

        employeeAmounts.put(startWork, amountKey, salary + newSalary);
        objectAmounts.put(startWork, objectId, objectAmount + newSalary);

        if (!relObjects.containsKey(startWork)) {
          relEmployees.put(startWork, employeeId);
        }
      }
    }

    Range<YearMonth> reportRange = Range.closedOpen(filterFrom, filterTo);

    for (YearMonth reportDate = reportRange.lowerEndpoint(); reportRange.contains(reportDate);
         reportDate = reportDate.nextMonth()) {

      Collection<Long> employees = relEmployees.get(reportDate);
      Collection<Long> objects = relObjects.get(reportDate);


      for (Long objectOrEmployee : BeeUtils.isEmpty(employees) ? objects : employees) {
        Collection<Long> dependencies = BeeUtils.isEmpty(employees)
          ? objectEmployees.get(objectOrEmployee) : employeeObjects.get(objectOrEmployee);

        for (Long employeeOrObject : dependencies) {
          Long objectId = BeeUtils.isEmpty(employees) ? objectOrEmployee : employeeOrObject;
          Long employeeId = BeeUtils.isEmpty(employees) ? employeeOrObject : objectOrEmployee;

          if (!DataUtils.isId(objectId) || !DataUtils.isId(employeeId)) {
            continue;
          }

          String amountKey = BeeUtils.joinItems(objectId, employeeId);

          SimpleRow reportRow = reportData.addEmptyRow();
          reportRow.setValue(ALS_REPORT_TIME_PERIOD,
            BeeUtils.toString(reportDate.getDate().getTime()));
          reportRow.setValue(COL_OSF_AMOUNT,
            BeeUtils.toString(BeeUtils.unbox(fundAmounts.get(reportDate, objectId))));

          reportRow.setValue(COL_CURRENCY, currencyName);

          reportRow.setValue(TradeConstants.COL_TRADE_PAID,
            BeeUtils.toString(BeeUtils.unbox(employeeAmounts.get(reportDate, amountKey))));

          Double debt = BeeUtils.nvl(fundDebt.get(reportDate, objectId), fundDebt.get(reportDate
              .previousMonth(), objectId),
            fundAmounts.get(reportDate, objectId));
          Double totalPaid = BeeUtils.unbox(objectAmounts.get(reportDate, objectId));

          if (debt != null && fundAmounts.get(reportDate, objectId) != null
            && fundDebt.get(reportDate, objectId) == null) {
            fundDebt.put(reportDate, objectId, debt - totalPaid);
            reportRow.setValue(TradeConstants.COL_TRADE_DEBT,
              BeeUtils.toString(BeeUtils.unbox(fundDebt.get(reportDate, objectId))));
          } else if (debt != null && fundDebt.get(reportDate, objectId) != null
            && fundAmounts.get(reportDate, objectId) != null) {
            reportRow.setValue(TradeConstants.COL_TRADE_DEBT,
              BeeUtils.toString(BeeUtils.unbox(fundDebt.get(reportDate, objectId))));
          } else {
            reportRow.setValue(TradeConstants.COL_TRADE_DEBT,
              BeeUtils.toString(-totalPaid));
          }
          reportRow.setValue(COL_LOCATION_NAME, objectFundData.getValueByKey(COL_OBJECT,
            BeeUtils.toString(objectId), COL_LOCATION_NAME));
          reportRow.setValue(COL_LOCATION_STATUS,
            objectFundData.getValueByKey(COL_OBJECT, BeeUtils.toString(objectId),
              COL_LOCATION_STATUS));

          reportRow.setValue(COL_COMPANY_CODE,
            objectFundData.getValueByKey(COL_OBJECT, BeeUtils.toString(objectId),
              COL_COMPANY_CODE));

          reportRow.setValue(COL_COMPANY_NAME,
            objectFundData.getValueByKey(COL_OBJECT, BeeUtils.toString(objectId),
              COL_COMPANY_NAME));

          reportRow.setValue(ALS_COMPANY_TYPE_NAME,
            objectFundData.getValueByKey(COL_OBJECT, BeeUtils.toString(objectId),
              ALS_COMPANY_TYPE_NAME));

          reportRow.setValue(COL_FIRST_NAME,
            objectFundData.getValueByKey(COL_OBJECT, BeeUtils.toString(objectId),
              COL_FIRST_NAME));

          reportRow.setValue(COL_LAST_NAME,
            objectFundData.getValueByKey(COL_OBJECT, BeeUtils.toString(objectId),
              COL_LAST_NAME));

          reportRow.setValue(ALS_LOCATION_MANAGER_FIRST_NAME,
            objectFundData.getValueByKey(COL_OBJECT, BeeUtils.toString(objectId),
              ALS_LOCATION_MANAGER_FIRST_NAME));

          reportRow.setValue(ALS_LOCATION_MANAGER_LAST_NAME,
            objectFundData.getValueByKey(COL_OBJECT, BeeUtils.toString(objectId),
              ALS_LOCATION_MANAGER_LAST_NAME));

          reportRow.setValue(FinanceConstants.ALS_EMPLOYEE_FIRST_NAME, employeeObjectData
            .getValueByKey(COL_EMPLOYEE,
              BeeUtils.toString(employeeId),
              FinanceConstants.ALS_EMPLOYEE_FIRST_NAME));

          reportRow.setValue(FinanceConstants.ALS_EMPLOYEE_LAST_NAME, employeeObjectData
            .getValueByKey(COL_EMPLOYEE,
              BeeUtils.toString(employeeId),
              FinanceConstants.ALS_EMPLOYEE_LAST_NAME));

          logger.debug("Create report row", reportRow.getDate(ALS_REPORT_TIME_PERIOD).toString(),
            objectId, employeeId, reportRow.getValues());
        }
      }
    }
    return ResponseObject.response(report.getResult(reportData,
        Localizations.getDictionary(reqInfo.getParameter(VAR_LOCALE))));
  }

  private void importTimeCards(Long companyId, SimpleRowSet erpTimeCards, StringBuilder log) {
    SimpleRowSet employees = qs.getData(new SqlSelect()
      .addField(TBL_EMPLOYEES, sys.getIdName(TBL_EMPLOYEES), COL_EMPLOYEE)
      .addFields(TBL_EMPLOYEES, COL_TAB_NUMBER)
      .addFrom(TBL_EMPLOYEES)
      .addFromInner(TBL_COMPANY_PERSONS,
        sys.joinTables(TBL_COMPANY_PERSONS, TBL_EMPLOYEES, COL_COMPANY_PERSON))
      .setWhere(SqlUtils.equals(TBL_COMPANY_PERSONS, COL_COMPANY, companyId)));

    Map<String, Long> tcCodes = getReferences(VIEW_TIME_CARD_CODES, COL_TC_CODE);

    int cds = 0;
    int ins = 0;
    int upd = 0;
    int del = 0;

    for (SimpleRow timeCard : erpTimeCards) {
      Long id = timeCard.getLong("D_TAB_ID");
      String tabNumber = timeCard.getValue("TAB_NR");

      if (BeeUtils.isEmpty(tabNumber)) {
        del += qs.updateData(new SqlDelete(VIEW_TIME_CARD_CHANGES)
          .setWhere(SqlUtils.equals(VIEW_TIME_CARD_CHANGES, COL_COSTS_EXTERNAL_ID, id)));
        continue;
      }
      Long employee = BeeUtils.toLongOrNull(employees.getValueByKey(COL_TAB_NUMBER, tabNumber,
        COL_EMPLOYEE));

      if (!DataUtils.isId(employee) || BeeUtils.isEmpty(timeCard.getValue("DATA_NUO"))
        || BeeUtils.isEmpty(timeCard.getValue("DATA_IKI"))) {
        continue;
      }
      String code = timeCard.getValue("TAB_KODAS");

      if (!tcCodes.containsKey(code)) {
        tcCodes.put(code, qs.insertData(new SqlInsert(VIEW_TIME_CARD_CODES)
          .addConstant(COL_TC_CODE, code)
          .addConstant(COL_TC_NAME,
            sys.clampValue(VIEW_TIME_CARD_CODES, COL_TC_NAME, timeCard.getValue("PAVAD")))));
        cds++;
      }
      int c = qs.updateData(new SqlUpdate(VIEW_TIME_CARD_CHANGES)
        .addConstant(COL_EMPLOYEE, employee)
        .addConstant(COL_TIME_CARD_CODE, tcCodes.get(code))
        .addConstant(COL_TIME_CARD_CHANGES_FROM,
          TimeUtils.parseDate(timeCard.getValue("DATA_NUO"), DateOrdering.YMD))
        .addConstant(COL_TIME_CARD_CHANGES_UNTIL,
          TimeUtils.parseDate(timeCard.getValue("DATA_IKI"), DateOrdering.YMD))
        .addConstant(COL_NOTES, timeCard.getValue("ISAK_PAVAD"))
        .setWhere(SqlUtils.equals(VIEW_TIME_CARD_CHANGES, COL_COSTS_EXTERNAL_ID, id)));

      if (BeeUtils.isPositive(c)) {
        upd++;
      } else {
        qs.insertData(new SqlInsert(VIEW_TIME_CARD_CHANGES)
          .addConstant(COL_EMPLOYEE, employee)
          .addConstant(COL_TIME_CARD_CODE, tcCodes.get(code))
          .addConstant(COL_TIME_CARD_CHANGES_FROM,
            TimeUtils.parseDate(timeCard.getValue("DATA_NUO"), DateOrdering.YMD))
          .addConstant(COL_TIME_CARD_CHANGES_UNTIL,
            TimeUtils.parseDate(timeCard.getValue("DATA_IKI"), DateOrdering.YMD))
          .addConstant(COL_NOTES, timeCard.getValue("ISAK_PAVAD"))
          .addConstant(COL_COSTS_EXTERNAL_ID, id));
        ins++;
      }
    }
    log.append(BeeUtils.join(BeeConst.STRING_EOL, cds > 0
        ? VIEW_TIME_CARD_CODES + ": +" + cds : null,
      (ins + upd + del) > 0 ? VIEW_TIME_CARD_CHANGES + ":" + (ins > 0 ? " +" + ins : "")
        + (upd > 0 ? " " + upd : "") + (del > 0 ? " -" + del : "") : null));
  }

  private boolean overlaps(long objId, long emplId, JustDate date, WorkScheduleKind kind) {
    Set<TimeRange> objRanges = new HashSet<>();
    Set<TimeRange> otherRanges = new HashSet<>();

    Set<Long> objTrIds = new HashSet<>();
    Set<Long> otherTrIds = new HashSet<>();

    Filter filter = Filter.and(Filter.equals(COL_EMPLOYEE, emplId),
        Filter.equals(COL_WORK_SCHEDULE_DATE, date),
        Filter.equals(COL_WORK_SCHEDULE_KIND, kind));

    BeeRowSet rowSet = qs.getViewData(VIEW_WORK_SCHEDULE, filter);

    if (!DataUtils.isEmpty(rowSet)) {
      int objIndex = rowSet.getColumnIndex(COL_PAYROLL_OBJECT);

      int trIndex = rowSet.getColumnIndex(COL_TIME_RANGE_CODE);
      int tcIndex = rowSet.getColumnIndex(COL_TIME_CARD_CODE);

      int fromIndex = rowSet.getColumnIndex(COL_WORK_SCHEDULE_FROM);
      int untilIndex = rowSet.getColumnIndex(COL_WORK_SCHEDULE_UNTIL);
      int durIndex = rowSet.getColumnIndex(COL_WORK_SCHEDULE_DURATION);

      int trFromIndex = rowSet.getColumnIndex(ALS_TR_FROM);
      int trUntilIndex = rowSet.getColumnIndex(ALS_TR_UNTIL);
      int trDurIndex = rowSet.getColumnIndex(ALS_TR_DURATION);

      String from;
      String until;
      String duration;

      for (BeeRow row : rowSet) {
        if (!DataUtils.isId(row.getLong(tcIndex))) {
          boolean isObj = Objects.equals(objId, row.getLong(objIndex));

          Long trId = row.getLong(trIndex);

          if (DataUtils.isId(trId)) {
            if (isObj) {
              if (otherTrIds.contains(trId)) {
                return true;
              }
              objTrIds.add(trId);

            } else {
              if (objTrIds.contains(trId)) {
                return true;
              }
              otherTrIds.add(trId);
            }

            from = row.getString(trFromIndex);
            until = row.getString(trUntilIndex);
            duration = row.getString(trDurIndex);

          } else {
            from = row.getString(fromIndex);
            until = row.getString(untilIndex);
            duration = row.getString(durIndex);
          }

          if (!BeeUtils.isEmpty(from)) {
            TimeRange range = TimeRange.of(from, until, duration);

            if (range != null) {
              if (isObj) {
                objRanges.add(range);
              } else {
                otherRanges.add(range);
              }
            }
          }
        }
      }
    }

    if (BeeUtils.intersects(objTrIds, otherTrIds)) {
      return true;

    } else if (!objRanges.isEmpty() && !otherRanges.isEmpty()) {
      for (TimeRange tr : objRanges) {
        if (BeeUtils.intersects(otherRanges, tr.getRange())) {
          return true;
        }
      }
    }

    return false;
  }

  private void setScheduledTime(Earnings earnings, Set<JustDate> holidays) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_KIND, COL_EMPLOYEE, COL_SUBSTITUTE_FOR,
            COL_WORK_SCHEDULE_DATE, COL_TIME_RANGE_CODE,
            COL_WORK_SCHEDULE_FROM, COL_WORK_SCHEDULE_UNTIL, COL_WORK_SCHEDULE_DURATION)
        .addField(TBL_TIME_RANGES, COL_TR_FROM, ALS_TR_FROM)
        .addField(TBL_TIME_RANGES, COL_TR_UNTIL, ALS_TR_UNTIL)
        .addField(TBL_TIME_RANGES, COL_TR_DURATION, ALS_TR_DURATION)
        .addFrom(TBL_WORK_SCHEDULE)
        .addFromLeft(TBL_TIME_RANGES,
            sys.joinTables(TBL_TIME_RANGES, TBL_WORK_SCHEDULE, COL_TIME_RANGE_CODE))
        .setWhere(getScheduleEarningsCondition(earnings.getDateFrom(), earnings.getDateUntil(),
            earnings.getEmployeeId(), earnings.getSubstituteFor(), earnings.getObjectId()));

    SimpleRowSet data = qs.getData(query);
    if (!DataUtils.isEmpty(data)) {

      Map<JustDate, Long> planned = new HashMap<>();
      Map<JustDate, Long> actual = new HashMap<>();

      boolean substitutePlanned = earnings.isSubstitution()
          && !WorkScheduleKind.PLANNED.isSubstitutionEnabled();

      Long millis;
      Long value;
      boolean ok;

      for (SimpleRow row : data) {
        WorkScheduleKind kind = EnumUtils.getEnumByIndex(WorkScheduleKind.class,
            row.getInt(COL_WORK_SCHEDULE_KIND));

        Long empl = row.getLong(COL_EMPLOYEE);
        Long subst = row.getLong(COL_SUBSTITUTE_FOR);

        if (substitutePlanned && kind == WorkScheduleKind.PLANNED) {
          ok = subst == null && Objects.equals(empl, earnings.getSubstituteFor());
        } else {
          ok = Objects.equals(empl, earnings.getEmployeeId())
              && Objects.equals(subst, earnings.getSubstituteFor());
        }

        if (ok) {
          JustDate date = row.getDate(COL_WORK_SCHEDULE_DATE);

          if (DataUtils.isId(row.getLong(COL_TIME_RANGE_CODE))) {
            millis = PayrollUtils.getMillis(row.getValue(ALS_TR_FROM), row.getValue(ALS_TR_UNTIL),
                row.getValue(ALS_TR_DURATION));
          } else {
            millis = PayrollUtils.getMillis(row.getValue(COL_WORK_SCHEDULE_FROM),
                row.getValue(COL_WORK_SCHEDULE_UNTIL), row.getValue(COL_WORK_SCHEDULE_DURATION));
          }

          if (BeeUtils.isPositive(millis) && kind != null && date != null) {
            switch (kind) {
              case PLANNED:
                value = planned.get(date);
                if (value != null) {
                  millis += value;
                }

                planned.put(date, millis);
                break;

              case ACTUAL:
                value = actual.get(date);
                if (value != null) {
                  millis += value;
                }

                actual.put(date, millis);
                break;
            }
          }
        }
      }

      if (!planned.isEmpty()) {
        earnings.setPlannedDays(planned.size());
        earnings.setPlannedMillis(planned.values().stream().mapToLong(Long::longValue).sum());
      }

      if (!actual.isEmpty()) {
        earnings.setActualDays(actual.size());
        earnings.setActualMillis(actual.values().stream().mapToLong(Long::longValue).sum());

        if (BeeUtils.intersects(actual.keySet(), holidays)) {
          Map<JustDate, Long> holy = actual.entrySet().stream()
              .filter(entry -> holidays.contains(entry.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

          earnings.setHolyDays(holy.size());
          earnings.setHolyMillis(holy.values().stream().mapToLong(Long::longValue).sum());
        }
      }
    }
  }

  private void syncEmployees(Long companyId, SimpleRowSet erpEmployees, SimpleRowSet employees,
                             Map<String, Long> departments, Map<String, Long> positions,
                             StringBuilder log) throws Throwable {
    String tabNo = null;
    int createdEmployeeCount = 0;
    int updatedEmployeeCount = 0;
    int createdPositionCount = 0;
    int createdDepartmentCount = 0;

    for (SimpleRow erpEmployee : erpEmployees) {
      tabNo = erpEmployee.getValue("CODE");
      SimpleRow employee = employees.getRowByKey(COL_TAB_NUMBER, tabNo);

      Long personId;
      Long personContactId = null;
      Long companyPersonId;
      Long contactId = null;
      Long employeeId;
      String fullName = BeeUtils.joinWords(erpEmployee.getValue("NAME"),
        erpEmployee.getValue("SURNAME"));

      if (employee == null) {
        employee = employees.getRowByKey(COL_FIRST_NAME, fullName);

        if (employee != null) {
          employeeId = employee.getLong(COL_EMPLOYEE);

          if (DataUtils.isId(employeeId)) {
            qs.updateData(new SqlUpdate(TBL_EMPLOYEES)
              .addConstant(COL_TAB_NUMBER, tabNo)
              .setWhere(sys.idEquals(TBL_EMPLOYEES, employeeId)));
          } else {
            if (qs.updateData(new SqlUpdate(TBL_EMPLOYEES)
              .addConstant(COL_TAB_NUMBER, tabNo)
              .setWhere(SqlUtils.equals(TBL_EMPLOYEES, COL_COMPANY_PERSON,
                employee.getLong(COL_COMPANY_PERSON)))) < 1) {

              employeeId = qs.insertData(new SqlInsert(TBL_EMPLOYEES)
                .addConstant(COL_COMPANY_PERSON, employee.getLong(COL_COMPANY_PERSON))
                .addConstant(COL_TAB_NUMBER, tabNo));
            } else {
              employeeId = qs.getLong(new SqlSelect()
                .addFields(TBL_EMPLOYEES, sys.getIdName(TBL_EMPLOYEES))
                .addFrom(TBL_EMPLOYEES)
                .setWhere(
                  SqlUtils.and(
                    SqlUtils.equals(TBL_EMPLOYEES, COL_COMPANY_PERSON,
                      employee.getLong(COL_COMPANY_PERSON)),
                    SqlUtils.equals(TBL_EMPLOYEES, COL_TAB_NUMBER, tabNo))));
            }
          }
        }
      }
      if (employee == null) {
        personId = qs.insertData(new SqlInsert(TBL_PERSONS)
          .addConstant(COL_FIRST_NAME, erpEmployee.getValue("NAME"))
          .addConstant(COL_LAST_NAME, erpEmployee.getValue("SURNAME")));

        companyPersonId = qs.insertData(new SqlInsert(TBL_COMPANY_PERSONS)
          .addConstant(COL_COMPANY, companyId)
          .addConstant(COL_PERSON, personId));

        employeeId = qs.insertData(new SqlInsert(TBL_EMPLOYEES)
          .addConstant(COL_COMPANY_PERSON, companyPersonId)
          .addConstant(COL_TAB_NUMBER, tabNo));
        createdEmployeeCount++;
      } else {
        personId = employee.getLong(COL_PERSON);
        personContactId = employee.getLong(COL_PERSON + COL_CONTACT);
        companyPersonId = employee.getLong(COL_COMPANY_PERSON);
        contactId = employee.getLong(COL_CONTACT);
        employeeId = employee.getLong(COL_EMPLOYEE);
        updatedEmployeeCount++;
      }

      if (!DataUtils.isId(employeeId)) {
        logger.debug("ERPEmployees err ", tabNo, fullName);
        continue;
      }
      String address = erpEmployee.getValue("ADDRESS1");

      if (!BeeUtils.isEmpty(address)) {
        if (!DataUtils.isId(personContactId)) {
          personContactId = qs.insertData(new SqlInsert(TBL_CONTACTS)
            .addConstant(COL_ADDRESS, address));

          qs.updateData(new SqlUpdate(TBL_PERSONS)
            .addConstant(COL_CONTACT, personContactId)
            .setWhere(sys.idEquals(TBL_PERSONS, personId)));
        } else {
          qs.updateData(new SqlUpdate(TBL_CONTACTS)
            .addConstant(COL_ADDRESS, address)
            .setWhere(sys.idEquals(TBL_CONTACTS, personContactId)));
        }
      }
      String phone = erpEmployee.getValue("MOBILEPHONE");

      if (!BeeUtils.isEmpty(phone)) {
        if (!DataUtils.isId(contactId)) {
          contactId = qs.insertData(new SqlInsert(TBL_CONTACTS)
            .addConstant(COL_MOBILE, phone));

          qs.updateData(new SqlUpdate(TBL_COMPANY_PERSONS)
            .addConstant(COL_CONTACT, contactId)
            .setWhere(sys.idEquals(TBL_COMPANY_PERSONS, companyPersonId)));
        } else {
          qs.updateData(new SqlUpdate(TBL_CONTACTS)
            .addConstant(COL_MOBILE, phone)
            .setWhere(sys.idEquals(TBL_CONTACTS, contactId)));
        }
      }
      String email = BeeUtils.normalize(erpEmployee.getValue("EMAIL"));

      if (!BeeUtils.isEmpty(email)) {
        Long emailId = qs.getLong(new SqlSelect()
          .addFields(TBL_EMAILS, sys.getIdName(TBL_EMAILS))
          .addFrom(TBL_EMAILS)
          .setWhere(SqlUtils.equals(TBL_EMAILS, COL_EMAIL_ADDRESS, email)));

        if (!DataUtils.isId(emailId)) {
          emailId = qs.insertData(new SqlInsert(TBL_EMAILS)
            .addConstant(COL_EMAIL_ADDRESS, email));
        }
        if (DataUtils.isId(emailId)) {
          if (!DataUtils.isId(contactId)) {
            contactId = qs.insertData(new SqlInsert(TBL_CONTACTS)
              .addConstant(COL_EMAIL, emailId));

            qs.updateData(new SqlUpdate(TBL_COMPANY_PERSONS)
              .addConstant(COL_CONTACT, contactId)
              .setWhere(sys.idEquals(TBL_COMPANY_PERSONS, companyPersonId)));
          } else {
            qs.updateData(new SqlUpdate(TBL_CONTACTS)
              .addConstant(COL_EMAIL, emailId)
              .setWhere(sys.idEquals(TBL_CONTACTS, contactId)));
          }
        }
      }
      String department = erpEmployee.getValue("DEPARTCODE");

      if (!BeeUtils.isEmpty(department) && !departments.containsKey(department)) {
        departments.put(department, qs.insertData(new SqlInsert(TBL_COMPANY_DEPARTMENTS)
          .addConstant(COL_COMPANY, companyId)
          .addConstant("Name", department)));
        createdDepartmentCount++;
      }
      String position = erpEmployee.getValue("POSITIONCODE");

      if (!BeeUtils.isEmpty(position) && !positions.containsKey(position)) {
        positions.put(position, qs.insertData(new SqlInsert(TBL_POSITIONS)
          .addConstant(COL_POSITION_NAME, position)));
        createdPositionCount++;
      }
      qs.updateData(new SqlUpdate(TBL_PERSONS)
        .addConstant(COL_DATE_OF_BIRTH,
          TimeUtils.parseDate(erpEmployee.getValue("BIRTHDAY"), DateOrdering.YMD))
        .setWhere(sys.idEquals(TBL_PERSONS, personId)));

      qs.updateData(new SqlUpdate(TBL_COMPANY_PERSONS)
        .addConstant(AdministrationConstants.COL_DEPARTMENT, departments.get(department))
        .addConstant(COL_POSITION, positions.get(position))
        .addConstant(COL_DATE_OF_EMPLOYMENT,
          TimeUtils.parseDate(erpEmployee.getValue("DIRBA_NUO"), DateOrdering.YMD))
        .addConstant(COL_DATE_OF_DISMISSAL,
          TimeUtils.parseDate(erpEmployee.getValue("DISMISSED"), DateOrdering.YMD))
        .setWhere(sys.idEquals(TBL_COMPANY_PERSONS, companyPersonId)));

      qs.updateData(new SqlUpdate(TBL_EMPLOYEES)
        .addConstant(COL_PART_TIME, erpEmployee.getDecimal("ETATAS"))
        .setWhere(sys.idEquals(TBL_EMPLOYEES, employeeId)));
    }

    log.append(BeeUtils.join(BeeConst.STRING_EOL,
      createdDepartmentCount > 0 ? TBL_COMPANY_DEPARTMENTS + ": +" + createdDepartmentCount : null,
      createdPositionCount > 0 ? TBL_POSITIONS + ": +" + createdPositionCount : null,
      (createdEmployeeCount + updatedEmployeeCount) > 0 ? TBL_EMPLOYEES + ":"
        + (createdEmployeeCount > 0 ? " +" + createdEmployeeCount : "")
        + (updatedEmployeeCount > 0 ? " " + updatedEmployeeCount : "") : null));
  }
}
