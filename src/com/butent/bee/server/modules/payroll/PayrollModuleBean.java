### Conflict
package com.butent.bee.server.modules.payroll;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.Invocation;
import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.http.RequestInfo;
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
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.payroll.Earnings;
import com.butent.bee.shared.modules.payroll.PayrollConstants.ObjectStatus;
import com.butent.bee.shared.modules.payroll.PayrollConstants.WorkScheduleKind;
import com.butent.bee.shared.modules.payroll.PayrollUtils;
import com.butent.bee.shared.modules.payroll.WorkScheduleSummary;
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

  private static BeeLogger logger = LogUtils.getLogger(PayrollModuleBean.class);

  private static final String PRM_ERP_SYNC_HOURS = "VitarestaSyncHours";

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  ConcurrencyBean cb;
  @EJB
  AdministrationModuleBean adm;
  @EJB
  ParamHolderBean prm;

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

      default:
        String msg = BeeUtils.joinWords("service not recognized:", svc);
        logger.warning(msg);
        response = ResponseObject.error(msg);
    }

    return response;
  }

  @Override
  public void ejbTimeout(Timer timer) {
    if (cb.isParameterTimer(timer, PRM_ERP_SYNC_HOURS)) {
      cb.asynchronousCall(new ConcurrencyBean.AsynchronousRunnable() {
        @Override
        public String getId() {
          return BeeUtils.join("-", PayrollModuleBean.class.getSimpleName(), PRM_ERP_SYNC_HOURS);
        }

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
    String module = getModule().getName();

    return Collections.singleton(BeeParameter.createNumber(module, PRM_ERP_SYNC_HOURS));
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
        List<Earnings> earnings = getEarnings(ym, holidays, employeeId, null, currency);

        if (!BeeUtils.isEmpty(earnings)) {
          for (Earnings item : earnings) {
            Double amount = item.total();

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

    SqlSelect query = new SqlSelect()
        .addFields(TBL_EMPLOYEES, COL_TAB_NUMBER)
        .addFields(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE,
            COL_TIME_RANGE_CODE, COL_TIME_CARD_CODE,
            COL_WORK_SCHEDULE_FROM, COL_WORK_SCHEDULE_UNTIL, COL_WORK_SCHEDULE_DURATION)
        .addField(TBL_TIME_RANGES, COL_TR_FROM, ALS_TR_FROM)
        .addField(TBL_TIME_RANGES, COL_TR_UNTIL, ALS_TR_UNTIL)
        .addField(TBL_TIME_RANGES, COL_TR_DURATION, ALS_TR_DURATION)
        .addFields(TBL_TIME_CARD_CODES, COL_TC_CODE)
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
            SqlUtils.and(employeeCondition,
                SqlUtils.equals(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_KIND, kind),
                SqlUtils.moreEqual(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE, from),
                SqlUtils.lessEqual(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE, until)));

    SimpleRowSet data = qs.getData(query);
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
          } else {
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

  public void importERPData() {
    long historyId = sys.eventStart(PRM_ERP_SYNC_HOURS);

    SimpleRowSet companies = qs.getData(new SqlSelect()
        .addField(TBL_COMPANIES, sys.getIdName(TBL_COMPANIES), COL_COMPANY)
        .addFields(TBL_COMPANIES, COL_COMPANY_NAME, PRM_ERP_ADDRESS, PRM_ERP_LOGIN,
            PRM_ERP_PASSWORD)
        .addFrom(TBL_COMPANIES)
        .setWhere(SqlUtils.notNull(TBL_COMPANIES, PRM_ERP_ADDRESS)));

    Map<String, Long> positions = getReferences(TBL_POSITIONS, COL_POSITION_NAME);
    String companyDepartments = "CompanyDepartments";
    String log = null;

    DateTime lastSyncTime = qs.getDateTime(new SqlSelect()
        .addMax(TBL_EVENT_HISTORY, COL_EVENT_STARTED)
        .addFrom(TBL_EVENT_HISTORY)
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_EVENT_HISTORY, COL_EVENT,
            PRM_ERP_SYNC_HOURS),
            SqlUtils.startsWith(TBL_EVENT_HISTORY, COL_EVENT_RESULT, "OK"))));

    for (SimpleRow companyInfo : companies) {
      Long company = companyInfo.getLong(COL_COMPANY);
      SimpleRowSet rs = null;
      String erpAddress = companyInfo.getValue(PRM_ERP_ADDRESS);
      String erpLogin = companyInfo.getValue(PRM_ERP_LOGIN);
      String erpPassword = companyInfo.getValue(PRM_ERP_PASSWORD);

      try {
        rs = ButentWS.connect(erpAddress, erpLogin, erpPassword)
            .getEmployees(lastSyncTime);
      } catch (BeeException e) {
        ctx.setRollbackOnly();
        sys.eventError(historyId, e);
        return;
      }
      int emplNew = 0;
      int emplUpd = 0;
      int posNew = 0;
      int deptNew = 0;
      int locNew = 0;
      String cardsInfo = null;
      Map<String, Long> departments = getReferences(companyDepartments, "Name",
          SqlUtils.equals(companyDepartments, COL_COMPANY, company));

      SimpleRowSet employees = qs.getData(new SqlSelect()
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

      String tabNr = null;

      try {
        for (SimpleRow row : rs) {
          tabNr = row.getValue("CODE");
          SimpleRow info = employees.getRowByKey(COL_TAB_NUMBER, tabNr);

          Long person;
          Long personContact = null;
          Long companyPerson;
          Long contact = null;
          Long employee;

          if (info == null) {
            info = employees.getRowByKey(COL_FIRST_NAME,
                BeeUtils.joinWords(row.getValue("NAME"), row.getValue("SURNAME")));

            if (info != null) {
              employee = info.getLong(COL_EMPLOYEE);

              if (DataUtils.isId(employee)) {
                qs.updateData(new SqlUpdate(TBL_EMPLOYEES)
                    .addConstant(COL_TAB_NUMBER, tabNr)
                    .setWhere(sys.idEquals(TBL_EMPLOYEES, employee)));
              } else {
                employee = qs.insertData(new SqlInsert(TBL_EMPLOYEES)
                    .addConstant(COL_COMPANY_PERSON, info.getLong(COL_COMPANY_PERSON))
                    .addConstant(COL_TAB_NUMBER, tabNr));
              }
            }
          }
          if (info == null) {
            person = qs.insertData(new SqlInsert(TBL_PERSONS)
                .addConstant(COL_FIRST_NAME, row.getValue("NAME"))
                .addConstant(COL_LAST_NAME, row.getValue("SURNAME")));

            companyPerson = qs.insertData(new SqlInsert(TBL_COMPANY_PERSONS)
                .addConstant(COL_COMPANY, company)
                .addConstant(COL_PERSON, person));

            employee = qs.insertData(new SqlInsert(TBL_EMPLOYEES)
                .addConstant(COL_COMPANY_PERSON, companyPerson)
                .addConstant(COL_TAB_NUMBER, tabNr));
            emplNew++;
          } else {
            person = info.getLong(COL_PERSON);
            personContact = info.getLong(COL_PERSON + COL_CONTACT);
            companyPerson = info.getLong(COL_COMPANY_PERSON);
            contact = info.getLong(COL_CONTACT);
            employee = info.getLong(COL_EMPLOYEE);
            emplUpd++;
          }
          String address = row.getValue("ADDRESS1");

          if (!BeeUtils.isEmpty(address)) {
            if (!DataUtils.isId(personContact)) {
              personContact = qs.insertData(new SqlInsert(TBL_CONTACTS)
                  .addConstant(COL_ADDRESS, address));

              qs.updateData(new SqlUpdate(TBL_PERSONS)
                  .addConstant(COL_CONTACT, personContact)
                  .setWhere(sys.idEquals(TBL_PERSONS, person)));
            } else {
              qs.updateData(new SqlUpdate(TBL_CONTACTS)
                  .addConstant(COL_ADDRESS, address)
                  .setWhere(sys.idEquals(TBL_CONTACTS, personContact)));
            }
          }
          String phone = row.getValue("MOBILEPHONE");

          if (!BeeUtils.isEmpty(phone)) {
            if (!DataUtils.isId(contact)) {
              contact = qs.insertData(new SqlInsert(TBL_CONTACTS)
                  .addConstant(COL_MOBILE, phone));

              qs.updateData(new SqlUpdate(TBL_COMPANY_PERSONS)
                  .addConstant(COL_CONTACT, contact)
                  .setWhere(sys.idEquals(TBL_COMPANY_PERSONS, companyPerson)));
            } else {
              qs.updateData(new SqlUpdate(TBL_CONTACTS)
                  .addConstant(COL_MOBILE, phone)
                  .setWhere(sys.idEquals(TBL_CONTACTS, contact)));
            }
          }
          String email = BeeUtils.normalize(row.getValue("EMAIL"));

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
              if (!DataUtils.isId(contact)) {
                contact = qs.insertData(new SqlInsert(TBL_CONTACTS)
                    .addConstant(COL_EMAIL, emailId));

                qs.updateData(new SqlUpdate(TBL_COMPANY_PERSONS)
                    .addConstant(COL_CONTACT, contact)
                    .setWhere(sys.idEquals(TBL_COMPANY_PERSONS, companyPerson)));
              } else {
                qs.updateData(new SqlUpdate(TBL_CONTACTS)
                    .addConstant(COL_EMAIL, emailId)
                    .setWhere(sys.idEquals(TBL_CONTACTS, contact)));
              }
            }
          }
          String department = row.getValue("DEPARTCODE");

          if (!BeeUtils.isEmpty(department) && !departments.containsKey(department)) {
            departments.put(department, qs.insertData(new SqlInsert(companyDepartments)
                .addConstant(COL_COMPANY, company)
                .addConstant("Name", department)));
            deptNew++;
          }
          String position = row.getValue("POSITIONCODE");

          if (!BeeUtils.isEmpty(position) && !positions.containsKey(position)) {
            positions.put(position, qs.insertData(new SqlInsert(TBL_POSITIONS)
                .addConstant(COL_POSITION_NAME, position)));
            posNew++;
          }
          qs.updateData(new SqlUpdate(TBL_PERSONS)
              .addConstant(COL_DATE_OF_BIRTH, TimeUtils.parseDate(row.getValue("BIRTHDAY")))
              .setWhere(sys.idEquals(TBL_PERSONS, person)));

          qs.updateData(new SqlUpdate(TBL_COMPANY_PERSONS)
              .addConstant(COL_DEPARTMENT, departments.get(department))
              .addConstant(COL_POSITION, positions.get(position))
              .addConstant(COL_DATE_OF_EMPLOYMENT, TimeUtils.parseDate(row.getValue("DIRBA_NUO")))
              .addConstant(COL_DATE_OF_DISMISSAL, TimeUtils.parseDate(row.getValue("DISMISSED")))
              .setWhere(sys.idEquals(TBL_COMPANY_PERSONS, companyPerson)));

          qs.updateData(new SqlUpdate(TBL_EMPLOYEES)
              .addConstant(COL_PART_TIME, row.getDecimal("ETATAS"))
              .setWhere(sys.idEquals(TBL_EMPLOYEES, employee)));
        }
        locNew = importLocations(erpAddress, erpLogin, erpPassword);
        cardsInfo = importTimeCards(erpAddress, erpLogin, erpPassword, lastSyncTime, company);

      } catch (Throwable e) {
        ctx.setRollbackOnly();
        sys.eventError(historyId, e, BeeUtils.join(": ", COL_TAB_NUMBER, tabNr));
        return;
      }
      log = BeeUtils.join(BeeConst.STRING_EOL, log, companyInfo.getValue(COL_COMPANY_NAME),
          deptNew > 0 ? companyDepartments + ": +" + deptNew : null,
          posNew > 0 ? TBL_POSITIONS + ": +" + posNew : null,
          locNew > 0 ? TBL_LOCATIONS + ": +" + locNew : null,
          (emplNew + emplUpd) > 0 ? TBL_EMPLOYEES + ":" + (emplNew > 0 ? " +" + emplNew : "")
              + (emplUpd > 0 ? " " + emplUpd : "") : null, cardsInfo);
    }
    sys.eventEnd(historyId, "OK", log);
  }

  @Override
  public void init() {

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

  private static IsCondition getScheduleKindCondition(WorkScheduleKind kind) {
    return SqlUtils.equals(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_KIND, kind);
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

  private RangeMap<JustDate, Double> getWages(long employee, long object,
      JustDate from, JustDate until, Long currency) {

    RangeMap<JustDate, Double> result = RangeMap.create();

    SimpleRow emplRow = qs.getRow(TBL_EMPLOYEES, employee);
    if (emplRow != null) {
      Double salary = emplRow.getDouble(COL_SALARY);
      if (BeeUtils.isPositive(salary)) {
        Double v = adm.maybeExchange(emplRow.getLong(COL_CURRENCY), currency, salary, null);

        if (BeeUtils.isPositive(v)) {
          result.put(DateRange.all(), v);
        }
      }
    }

    SqlSelect query = new SqlSelect()
        .addFields(TBL_EMPLOYEE_OBJECTS, COL_EMPLOYEE_OBJECT_FROM, COL_EMPLOYEE_OBJECT_UNTIL,
            COL_WAGE, COL_CURRENCY)
        .addFrom(TBL_EMPLOYEE_OBJECTS)
        .setWhere(
            SqlUtils.and(
                SqlUtils.equals(TBL_EMPLOYEE_OBJECTS,
                    COL_EMPLOYEE, employee, COL_PAYROLL_OBJECT, object),
                SqlUtils.positive(TBL_EMPLOYEE_OBJECTS, COL_WAGE)));

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      for (SimpleRow row : data) {
        JustDate min = BeeUtils.max(row.getDate(COL_EMPLOYEE_OBJECT_FROM), from);
        JustDate max = BeeUtils.min(row.getDate(COL_EMPLOYEE_OBJECT_UNTIL), until);

        if (DateRange.isValidClosedRange(min, max)) {
          Double v = adm.maybeExchange(row.getLong(COL_CURRENCY), currency,
              row.getDouble(COL_WAGE), null);

          if (BeeUtils.isPositive(v)) {
            result.put(DateRange.closed(min, max), v);
          }
        }
      }
    }

    return result;
  }

  private int importLocations(String erpAddress, String erpLogin, String erpPassword)
      throws BeeException {

    SimpleRowSet rs = ButentWS.connect(erpAddress, erpLogin, erpPassword)
        .getObjects();

    int locNew = 0;
    Map<String, Long> locations = getReferences(TBL_LOCATIONS, COL_LOCATION_NAME);

    for (SimpleRow row : rs) {
      String location = row.getValue("objektas");

      if (!locations.containsKey(location)) {
        locations.put(location, qs.insertData(new SqlInsert(TBL_LOCATIONS)
            .addConstant(COL_LOCATION_NAME, location)
            .addConstant(COL_LOCATION_STATUS, ObjectStatus.INACTIVE.ordinal())));
        locNew++;
      }
    }
    return locNew;
  }

  private String importTimeCards(String erpAddress, String erpLogin, String erpPassword,
      DateTime lastSyncTime, Long company) throws BeeException {
    SimpleRowSet rs = ButentWS.connect(erpAddress, erpLogin, erpPassword)
        .getTimeCards(lastSyncTime);

    SimpleRowSet employees = qs.getData(new SqlSelect()
        .addField(TBL_EMPLOYEES, sys.getIdName(TBL_EMPLOYEES), COL_EMPLOYEE)
        .addFields(TBL_EMPLOYEES, COL_TAB_NUMBER)
        .addFrom(TBL_EMPLOYEES)
        .addFromInner(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_EMPLOYEES, COL_COMPANY_PERSON))
        .setWhere(SqlUtils.equals(TBL_COMPANY_PERSONS, COL_COMPANY, company)));

    Map<String, Long> tcCodes = getReferences(VIEW_TIME_CARD_CODES, COL_TC_CODE);

    int cds = 0;
    int ins = 0;
    int upd = 0;
    int del = 0;

    for (SimpleRow row : rs) {
      Long id = row.getLong("D_TAB_ID");
      String tabNumber = row.getValue("TAB_NR");

      if (BeeUtils.isEmpty(tabNumber)) {
        del += qs.updateData(new SqlDelete(VIEW_TIME_CARD_CHANGES)
            .setWhere(SqlUtils.equals(VIEW_TIME_CARD_CHANGES, COL_COSTS_EXTERNAL_ID, id)));
        continue;
      }
      Long employee = BeeUtils.toLongOrNull(employees.getValueByKey(COL_TAB_NUMBER, tabNumber,
          COL_EMPLOYEE));

      if (!DataUtils.isId(employee)) {
        continue;
      }
      String code = row.getValue("TAB_KODAS");

      if (!tcCodes.containsKey(code)) {
        tcCodes.put(code, qs.insertData(new SqlInsert(VIEW_TIME_CARD_CODES)
            .addConstant(COL_TC_CODE, code)
            .addConstant(COL_TC_NAME,
                sys.clampValue(VIEW_TIME_CARD_CODES, COL_TC_NAME, row.getValue("PAVAD")))));
        cds++;
      }
      int c = qs.updateData(new SqlUpdate(VIEW_TIME_CARD_CHANGES)
          .addConstant(COL_EMPLOYEE, employee)
          .addConstant(COL_TIME_CARD_CODE, tcCodes.get(code))
          .addConstant(COL_TIME_CARD_CHANGES_FROM, TimeUtils.parseDate(row.getValue("DATA_NUO")))
          .addConstant(COL_TIME_CARD_CHANGES_UNTIL, TimeUtils.parseDate(row.getValue("DATA_IKI")))
          .addConstant(COL_NOTES, row.getValue("ISAK_PAVAD"))
          .setWhere(SqlUtils.equals(VIEW_TIME_CARD_CHANGES, COL_COSTS_EXTERNAL_ID, id)));

      if (BeeUtils.isPositive(c)) {
        upd++;
      } else {
        qs.insertData(new SqlInsert(VIEW_TIME_CARD_CHANGES)
            .addConstant(COL_EMPLOYEE, employee)
            .addConstant(COL_TIME_CARD_CODE, tcCodes.get(code))
            .addConstant(COL_TIME_CARD_CHANGES_FROM, TimeUtils.parseDate(row.getValue("DATA_NUO")))
            .addConstant(COL_TIME_CARD_CHANGES_UNTIL, TimeUtils.parseDate(row.getValue("DATA_IKI")))
            .addConstant(COL_NOTES, row.getValue("ISAK_PAVAD"))
            .addConstant(COL_COSTS_EXTERNAL_ID, id));
        ins++;
      }
    }
    return BeeUtils.join(BeeConst.STRING_EOL, cds > 0 ? VIEW_TIME_CARD_CODES + ": +" + cds : null,
        (ins + upd + del) > 0 ? VIEW_TIME_CARD_CHANGES + ":" + (ins > 0 ? " +" + ins : "")
            + (upd > 0 ? " " + upd : "") + (del > 0 ? " -" + del : "") : null);
  }

  private ResponseObject initializeEarnings(RequestInfo reqInfo) {
    Integer year = reqInfo.getParameterInt(COL_EARNINGS_YEAR);
    if (!TimeUtils.isYear(year)) {
      return ResponseObject.parameterNotFound(reqInfo.getSubService(), COL_EARNINGS_YEAR);
    }

    Integer month = reqInfo.getParameterInt(COL_EARNINGS_MONTH);
    if (!TimeUtils.isMonth(month)) {
      return ResponseObject.parameterNotFound(reqInfo.getSubService(), COL_EARNINGS_MONTH);
    }

    int result = 0;

    JustDate from = new JustDate(year, month, 1);
    JustDate until = TimeUtils.endOfMonth(from);

    HasConditions scheduleWhere =
        SqlUtils.and(
            SqlUtils.moreEqual(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE, from),
            SqlUtils.lessEqual(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DATE, until),
            SqlUtils.or(
                SqlUtils.notNull(TBL_WORK_SCHEDULE, COL_TIME_RANGE_CODE),
                SqlUtils.notNull(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_FROM),
                SqlUtils.notNull(TBL_WORK_SCHEDULE, COL_WORK_SCHEDULE_DURATION)));

    SqlSelect wsEmplQuery = new SqlSelect().setDistinctMode(true)
        .addFields(TBL_WORK_SCHEDULE, COL_EMPLOYEE, COL_PAYROLL_OBJECT)
        .addFrom(TBL_WORK_SCHEDULE)
        .setWhere(scheduleWhere);

    SimpleRowSet scheduledEmployees = qs.getData(wsEmplQuery);
    if (DataUtils.isEmpty(scheduledEmployees)) {
      return ResponseObject.response(result);
    }

    Set<Pair<Long, Long>> existingEmployees = new HashSet<>();

    SqlSelect earnEmplQuery = new SqlSelect().setDistinctMode(true)
        .addFields(TBL_EMPLOYEE_EARNINGS, COL_EMPLOYEE, COL_PAYROLL_OBJECT)
        .addFrom(TBL_EMPLOYEE_EARNINGS)
        .setWhere(SqlUtils.equals(TBL_EMPLOYEE_EARNINGS,
            COL_EARNINGS_YEAR, year, COL_EARNINGS_MONTH, month));

    SimpleRowSet earnEmplData = qs.getData(earnEmplQuery);
    if (!DataUtils.isEmpty(earnEmplData)) {
      for (SimpleRow row : earnEmplData) {
        Long employee = row.getLong(COL_EMPLOYEE);
        Long object = row.getLong(COL_PAYROLL_OBJECT);

        if (DataUtils.isId(employee) && DataUtils.isId(object)) {
          existingEmployees.add(Pair.of(employee, object));
        }
      }
    }

    for (SimpleRow row : scheduledEmployees) {
      Long employee = row.getLong(COL_EMPLOYEE);
      Long object = row.getLong(COL_PAYROLL_OBJECT);

      if (DataUtils.isId(employee) && DataUtils.isId(object)
          && !existingEmployees.contains(Pair.of(employee, object))) {

        SqlInsert insert = new SqlInsert(TBL_EMPLOYEE_EARNINGS)
            .addConstant(COL_EMPLOYEE, employee)
            .addConstant(COL_PAYROLL_OBJECT, object)
            .addConstant(COL_EARNINGS_YEAR, year)
            .addConstant(COL_EARNINGS_MONTH, month);

        ResponseObject response = qs.insertDataWithResponse(insert);
        if (response.hasErrors()) {
          return response;
        }

        result++;
      }
    }

    SqlSelect wsObjQuery = new SqlSelect().setDistinctMode(true)
        .addFields(TBL_WORK_SCHEDULE, COL_PAYROLL_OBJECT)
        .addFrom(TBL_WORK_SCHEDULE)
        .setWhere(scheduleWhere);

    Set<Long> scheduledObjects = qs.getLongSet(wsObjQuery);

    SqlSelect earnObjQuery = new SqlSelect().setDistinctMode(true)
        .addFields(TBL_OBJECT_EARNINGS, COL_PAYROLL_OBJECT)
        .addFrom(TBL_OBJECT_EARNINGS)
        .setWhere(SqlUtils.equals(TBL_OBJECT_EARNINGS,
            COL_EARNINGS_YEAR, year, COL_EARNINGS_MONTH, month));

    Set<Long> existingObjects = qs.getLongSet(earnObjQuery);
    scheduledObjects.removeAll(existingObjects);

    for (Long object : scheduledObjects) {
      if (DataUtils.isId(object)) {
        SqlInsert insert = new SqlInsert(TBL_OBJECT_EARNINGS)
            .addConstant(COL_PAYROLL_OBJECT, object)
            .addConstant(COL_EARNINGS_YEAR, year)
            .addConstant(COL_EARNINGS_MONTH, month);

        ResponseObject response = qs.insertDataWithResponse(insert);
        if (response.hasErrors()) {
          return response;
        }

        result++;
      }
    }

    return ResponseObject.response(result);
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
        earnings.setPlannedMillis(planned.values().stream().mapToLong(n -> n.longValue()).sum());
      }

      if (!actual.isEmpty()) {
        earnings.setActualDays(actual.size());
        earnings.setActualMillis(actual.values().stream().mapToLong(n -> n.longValue()).sum());

        if (BeeUtils.intersects(actual.keySet(), holidays)) {
          Map<JustDate, Long> holy = actual.entrySet().stream()
              .filter(entry -> holidays.contains(entry.getKey()))
              .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

          earnings.setHolyDays(holy.size());
          earnings.setHolyMillis(holy.values().stream().mapToLong(n -> n.longValue()).sum());
        }
      }
    }
  }
}
