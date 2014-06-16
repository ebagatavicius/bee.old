package com.butent.bee.server.modules.calendar;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.ViewDeleteEvent;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewModifyEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEvent.ViewUpdateEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.NewsHelper;
import com.butent.bee.server.news.UsageQueryProvider;
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
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants.ReminderMethod;
import com.butent.bee.shared.modules.calendar.CalendarConstants.AppointmentStatus;
import com.butent.bee.shared.modules.calendar.CalendarConstants.CalendarVisibility;
import com.butent.bee.shared.modules.calendar.CalendarConstants.ItemType;
import com.butent.bee.shared.modules.calendar.CalendarConstants.Report;
import com.butent.bee.shared.modules.calendar.CalendarConstants.ViewType;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.modules.calendar.CalendarTask;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.modules.tasks.TaskType;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.Headline;
import com.butent.bee.shared.news.HeadlineProducer;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

@Singleton
@Lock(LockType.READ)
public class CalendarModuleBean implements BeeModule {

  private static final Filter VALID_APPOINTMENT = Filter.and(Filter.notNull(COL_START_DATE_TIME),
      Filter.compareWithColumn(COL_START_DATE_TIME, Operator.LT, COL_END_DATE_TIME));

  private static BeeLogger logger = LogUtils.getLogger(CalendarModuleBean.class);

  private static String formatMinutes(int minutes) {
    return BeeUtils.toString(minutes / TimeUtils.MINUTES_PER_HOUR) + TimeUtils.TIME_FIELD_SEPARATOR
        + TimeUtils.padTwo(minutes % TimeUtils.MINUTES_PER_HOUR);
  }

  private static void formatTimeColumns(BeeRowSet rowSet, int colFrom, int colTo) {
    for (BeeRow row : rowSet.getRows()) {
      for (int c = colFrom; c <= colTo; c++) {
        Integer value = row.getInteger(c);
        if (value != null) {
          row.setValue(c, formatMinutes(value));
        }
      }
    }
  }

  private static Filter getAttendeeFilter(List<Long> attendeeTypes, List<Long> attendees) {
    if (!BeeUtils.isEmpty(attendeeTypes) || !BeeUtils.isEmpty(attendees)) {
      return Filter.or(Filter.any(COL_ATTENDEE_TYPE, attendeeTypes),
          Filter.idIn(attendees));
    } else if (!BeeUtils.isEmpty(attendeeTypes)) {
      return Filter.any(COL_ATTENDEE_TYPE, attendeeTypes);
    } else if (!BeeUtils.isEmpty(attendees)) {
      return Filter.idIn(attendees);
    } else {
      return null;
    }
  }

  private static Range<DateTime> getDateRange(JustDate lower, JustDate upper) {
    if (lower != null && upper != null) {
      return Range.closedOpen(lower.getDateTime(), upper.getDateTime());
    } else if (lower != null) {
      return Range.atLeast(lower.getDateTime());
    } else if (upper != null) {
      return Range.lessThan(upper.getDateTime());
    } else {
      return Range.all();
    }
  }

  private static Range<Integer> getHourRange(Integer lower, Integer upper) {
    if (BeeUtils.isPositive(lower) && BeeUtils.isPositive(upper)) {
      return Range.closedOpen(lower, upper);
    } else if (BeeUtils.isPositive(lower)) {
      return Range.atLeast(lower);
    } else if (BeeUtils.isPositive(upper)) {
      return Range.lessThan(upper);
    } else {
      return Range.all();
    }
  }

  private static Map<Integer, Integer> splitByHour(DateTime start, DateTime end,
      Range<DateTime> dateRange, Range<Integer> hourRange) {
    Map<Integer, Integer> result = Maps.newHashMap();
    if (start == null || end == null) {
      return result;
    }

    DateTime dt = DateTime.copyOf(start);
    while (TimeUtils.isLess(dt, end)) {
      int hour = dt.getHour();
      DateTime next = TimeUtils.nextHour(dt, 0);

      int value = BeeUtils.unbox(result.get(hour));

      if (TimeUtils.isLess(next, end)) {
        if (dateRange.contains(dt) && hourRange.contains(hour)) {
          result.put(hour, TimeUtils.minuteDiff(dt, next) + value);
        }
        dt.setTime(next.getTime());

      } else {
        if (dateRange.contains(dt) && hourRange.contains(hour)) {
          result.put(hour, TimeUtils.minuteDiff(dt, end) + value);
        }
        break;
      }
    }
    return result;
  }

  private static Map<YearMonth, Integer> splitByYearMonth(DateTime start, DateTime end,
      Range<DateTime> range) {
    Map<YearMonth, Integer> result = Maps.newHashMap();
    if (start == null || end == null) {
      return result;
    }

    if (TimeUtils.sameMonth(start, end) && range.contains(start) && range.contains(end)) {
      result.put(new YearMonth(start), TimeUtils.minuteDiff(start, end));
      return result;
    }

    DateTime dt = DateTime.copyOf(start);
    while (TimeUtils.isLess(dt, end)) {
      YearMonth ym = new YearMonth(dt);
      DateTime next = TimeUtils.nextDay(dt).getDateTime();

      int value = BeeUtils.unbox(result.get(ym));

      if (TimeUtils.isLess(next, end)) {
        if (range.contains(dt)) {
          result.put(ym, TimeUtils.minuteDiff(dt, next) + value);
        }
        dt.setTime(next.getTime());
      } else {
        if (range.contains(dt)) {
          result.put(ym, TimeUtils.minuteDiff(dt, end) + value);
        }
        break;
      }
    }
    return result;
  }

  private static void totalColumns(BeeRowSet rowSet, int colFrom, int colTo, int colTotal) {
    for (BeeRow row : rowSet.getRows()) {
      int sum = 0;
      for (int c = colFrom; c <= colTo; c++) {
        Integer value = row.getInteger(c);
        if (value != null) {
          sum += value;
        }
      }
      row.setValue(colTotal, sum);
    }
  }

  private static void totalRows(BeeRowSet rowSet, int colFrom, int colTo, long rowId,
      String caption, int colCaption) {

    Integer[] totals = new Integer[colTo - colFrom + 1];
    for (int i = 0; i < totals.length; i++) {
      totals[i] = 0;
    }

    for (BeeRow row : rowSet.getRows()) {
      for (int c = colFrom; c <= colTo; c++) {
        Integer value = row.getInteger(c);
        if (value != null) {
          totals[c - colFrom] += value;
        }
      }
    }

    String[] arr = new String[rowSet.getNumberOfColumns()];
    if (!BeeUtils.isEmpty(caption)) {
      arr[colCaption] = caption;
    }
    for (int c = colFrom; c <= colTo; c++) {
      arr[c] = BeeUtils.toString(totals[c - colFrom]);
    }

    rowSet.addRow(rowId, arr);
  }

  @EJB
  SystemBean sys;
  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  DataEditorBean deb;
  @EJB
  MailModuleBean mail;
  @EJB
  ParamHolderBean prm;

  @EJB
  NewsBean news;

  @Resource
  TimerService timerService;

  private final Multimap<Long, Timer> notificationTimers = HashMultimap.create();

  @Lock(LockType.WRITE)
  public void createNotificationTimers(Pair<String, Long> idInfo) {
    IsCondition wh = null;
    String reminderIdName = sys.getIdName(TBL_APPOINTMENT_REMINDERS);

    Collection<Timer> timers = null;

    if (idInfo == null) {
      timers = Lists.newArrayList(notificationTimers.values());
      notificationTimers.clear();

    } else {
      String idName = idInfo.getA();
      Long id = idInfo.getB();

      if (BeeUtils.same(idName, TBL_APPOINTMENTS)) {
        timers = notificationTimers.removeAll(id);
        wh = SqlUtils.equals(TBL_APPOINTMENT_REMINDERS, COL_APPOINTMENT, id);

      } else if (BeeUtils.same(idName, TBL_APPOINTMENT_REMINDERS)) {
        Long appointmentId = null;
        Timer timer = null;

        for (Entry<Long, Timer> entry : notificationTimers.entries()) {
          timer = entry.getValue();

          try {
            if (Objects.equal(timer.getInfo(), id)) {
              appointmentId = entry.getKey();
              break;
            }
          } catch (NoSuchObjectLocalException e) {
            logger.warning(e);
          }
        }
        if (appointmentId != null) {
          timers = Lists.newArrayList(timer);
          notificationTimers.remove(appointmentId, timer);
        }
        wh = SqlUtils.equals(TBL_APPOINTMENT_REMINDERS, reminderIdName, id);
      }
    }
    if (!BeeUtils.isEmpty(timers)) {
      for (Timer timer : timers) {
        try {
          logger.debug("Canceled timer:", timer.getInfo());
          timer.cancel();
        } catch (NoSuchObjectLocalException e) {
          logger.warning(e);
        }
      }
    }
    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_APPOINTMENTS, COL_START_DATE_TIME)
        .addFields(TBL_APPOINTMENT_REMINDERS, reminderIdName, COL_APPOINTMENT,
            COL_HOURS, COL_MINUTES, COL_SCHEDULED)
        .addField(TBL_REMINDER_TYPES, COL_HOURS, "defHours")
        .addField(TBL_REMINDER_TYPES, COL_MINUTES, "defMinutes")
        .addFrom(TBL_APPOINTMENTS)
        .addFromInner(TBL_APPOINTMENT_REMINDERS,
            sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_REMINDERS, COL_APPOINTMENT))
        .addFromInner(TBL_REMINDER_TYPES,
            sys.joinTables(TBL_REMINDER_TYPES, TBL_APPOINTMENT_REMINDERS,
                COL_REMINDER_TYPE))
        .setWhere(SqlUtils.and(wh,
            SqlUtils.more(TBL_APPOINTMENTS, COL_START_DATE_TIME, System.currentTimeMillis()),
            SqlUtils.notEqual(TBL_APPOINTMENTS, COL_STATUS,
                AppointmentStatus.CANCELED.ordinal()))));

    for (SimpleRow row : data) {
      Long start = row.getLong(COL_SCHEDULED);

      if (start == null) {
        long offset = BeeUtils.unbox(row.getInt(COL_HOURS)) * TimeUtils.MILLIS_PER_HOUR
            + BeeUtils.unbox(row.getInt(COL_MINUTES)) * TimeUtils.MILLIS_PER_MINUTE;

        if (offset == 0) {
          offset = BeeUtils.unbox(row.getInt("defHours")) * TimeUtils.MILLIS_PER_HOUR
              + BeeUtils.unbox(row.getInt("defMinutes")) * TimeUtils.MILLIS_PER_MINUTE;
        }
        if (offset != 0) {
          start = BeeUtils.unbox(row.getLong(COL_START_DATE_TIME)) - offset;
        }
      }
      if (start != null) {
        DateTime time = TimeUtils.toDateTimeOrNull(start);
        long from = BeeUtils.unbox(prm.getTime(PRM_REMINDER_TIME_FROM));
        long until = BeeUtils.unbox(prm.getTime(PRM_REMINDER_TIME_UNTIL));

        if (from < until) {
          int current = TimeUtils.minutesSinceDayStarted(time) * TimeUtils.MILLIS_PER_MINUTE;

          if (current < from || current > until) {
            time = new DateTime((current < from) ? TimeUtils.previousDay(time) : time.getDate());
            time.setTime(time.getTime() + until);
          }
        }
        if (time.getTime() > System.currentTimeMillis()) {
          notificationTimers.put(row.getLong(COL_APPOINTMENT),
              timerService.createSingleActionTimer(time.getJava(),
                  new TimerConfig(row.getLong(reminderIdName), false)));

          logger.debug("Created timer:", time, row.getValue(reminderIdName));
        }
      }
    }
  }

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> results = Lists.newArrayList();

    if (usr.isModuleVisible(Module.CALENDAR.getName())) {
      Filter filter = Filter.or(
          Filter.anyContains(Sets.newHashSet(COL_SUMMARY, COL_DESCRIPTION,
              COL_APPOINTMENT_LOCATION, ALS_COMPANY_NAME, COL_VEHICLE_NUMBER), query),
          Filter.anyItemContains(COL_STATUS, AppointmentStatus.class, query));

      List<BeeRow> appointments = getAppointments(filter,
          new Order(COL_START_DATE_TIME, false), true);
      if (!BeeUtils.isEmpty(appointments)) {
        for (BeeRow row : appointments) {
          results.add(new SearchResult(VIEW_APPOINTMENTS, row));
        }
      }
    }
    return results;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    if (BeeUtils.same(svc, SVC_GET_USER_CALENDAR)) {
      response = getUserCalendar(reqInfo);
    } else if (BeeUtils.same(svc, SVC_CREATE_APPOINTMENT)) {
      response = createAppointment(reqInfo);
    } else if (BeeUtils.same(svc, SVC_UPDATE_APPOINTMENT)) {
      response = updateAppointment(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_CALENDAR_ITEMS)) {
      response = getCalendarItems(reqInfo);
    } else if (BeeUtils.same(svc, SVC_SAVE_ACTIVE_VIEW)) {
      response = saveActiveView(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_OVERLAPPING_APPOINTMENTS)) {
      response = getOverlappingAppointments(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_REPORT_OPTIONS)) {
      response = getReportOptions(reqInfo);
    } else if (BeeUtils.same(svc, SVC_DO_REPORT)) {
      response = doReport(reqInfo);

    } else {
      String msg = BeeUtils.joinWords("Calendar service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    return Lists.newArrayList(
        BeeParameter.createTimeOfDay(module, PRM_REMINDER_TIME_FROM, false,
            TimeUtils.parseTime("8:00")),
        BeeParameter.createTimeOfDay(module, PRM_REMINDER_TIME_UNTIL, false,
            TimeUtils.parseTime("18:00")));
  }

  @Override
  public Module getModule() {
    return Module.CALENDAR;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public void init() {
    createNotificationTimers(null);

    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      public void setRowProperties(ViewQueryEvent event) {
        if (event.isAfter() && BeeUtils.same(event.getTargetName(), VIEW_APPOINTMENTS)
            && !DataUtils.isEmpty(event.getRowset())) {
          setAppopintmentProperties(event.getRowset());
        }
      }

      @Subscribe
      public void updateTimers(ViewModifyEvent event) {
        if (event.isAfter()) {
          if (BeeUtils.same(event.getTargetName(), TBL_REMINDER_TYPES)) {
            if (event instanceof ViewDeleteEvent
                || event instanceof ViewUpdateEvent
                && (DataUtils.contains(((ViewUpdateEvent) event).getColumns(), COL_HOURS)
                || DataUtils.contains(((ViewUpdateEvent) event).getColumns(), COL_MINUTES))) {

              createNotificationTimers(null);
            }

          } else if (BeeUtils.same(event.getTargetName(), TBL_APPOINTMENTS)) {
            if (event instanceof ViewDeleteEvent) {
              for (long id : ((ViewDeleteEvent) event).getIds()) {
                createNotificationTimers(Pair.of(TBL_APPOINTMENTS, id));
              }
            } else if (event instanceof ViewUpdateEvent) {
              ViewUpdateEvent ev = (ViewUpdateEvent) event;

              if (DataUtils.contains(ev.getColumns(), COL_STATUS)
                  || DataUtils.contains(ev.getColumns(), COL_START_DATE_TIME)) {

                createNotificationTimers(Pair.of(TBL_APPOINTMENTS, ev.getRow().getId()));
              }
            }

          } else if (BeeUtils.same(event.getTargetName(), TBL_APPOINTMENT_REMINDERS)) {
            if (event instanceof ViewDeleteEvent) {
              for (long id : ((ViewDeleteEvent) event).getIds()) {
                createNotificationTimers(Pair.of(TBL_APPOINTMENT_REMINDERS, id));
              }
            } else if (event instanceof ViewUpdateEvent) {
              ViewUpdateEvent ev = (ViewUpdateEvent) event;

              if (DataUtils.contains(ev.getColumns(), COL_REMINDER_TYPE)
                  || DataUtils.contains(ev.getColumns(), COL_HOURS)
                  || DataUtils.contains(ev.getColumns(), COL_MINUTES)
                  || DataUtils.contains(ev.getColumns(), COL_SCHEDULED)) {

                createNotificationTimers(Pair.of(TBL_APPOINTMENT_REMINDERS, ev.getRow().getId()));
              }
            } else if (event instanceof ViewInsertEvent) {
              createNotificationTimers(Pair.of(TBL_APPOINTMENT_REMINDERS,
                  ((ViewInsertEvent) event).getRow().getId()));
            }
          }
        }
      }
    });

    news.registerUsageQueryProvider(Feed.APPOINTMENTS_MY, new UsageQueryProvider() {
      @Override
      public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
          DateTime startDate) {

        Long companyPerson = usr.getCompanyPerson(userId);
        if (companyPerson == null) {
          return null;
        }

        String idColumn = sys.getIdName(TBL_APPOINTMENTS);
        String usageTable = NewsConstants.getUsageTable(TBL_APPOINTMENTS);

        return new SqlSelect()
            .addFields(TBL_APPOINTMENTS, idColumn)
            .addMax(usageTable, NewsConstants.COL_USAGE_ACCESS)
            .addFrom(TBL_APPOINTMENTS)
            .addFromInner(usageTable, news.joinUsage(TBL_APPOINTMENTS))
            .addFromLeft(TBL_APPOINTMENT_OWNERS,
                sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_OWNERS, COL_APPOINTMENT))
            .addFromLeft(TBL_APPOINTMENT_ATTENDEES,
                sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_ATTENDEES, COL_APPOINTMENT))
            .addFromLeft(TBL_ATTENDEES,
                sys.joinTables(TBL_ATTENDEES, TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE))
            .setWhere(
                SqlUtils.and(SqlUtils.equals(usageTable, NewsConstants.COL_USAGE_USER, userId),
                    SqlUtils.or(
                        SqlUtils.equals(TBL_APPOINTMENT_OWNERS, COL_APPOINTMENT_OWNER, userId),
                        SqlUtils.equals(TBL_ATTENDEES, COL_COMPANY_PERSON, companyPerson))))
            .addGroup(TBL_APPOINTMENTS, idColumn);
      }

      @Override
      public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
          DateTime startDate) {
        Long companyPerson = usr.getCompanyPerson(userId);
        if (companyPerson == null) {
          return null;
        }

        String idColumn = sys.getIdName(TBL_APPOINTMENTS);
        String usageTable = NewsConstants.getUsageTable(TBL_APPOINTMENTS);

        SqlSelect subquery = new SqlSelect()
            .addFields(TBL_APPOINTMENTS, idColumn)
            .addFields(usageTable, NewsConstants.COL_USAGE_UPDATE)
            .addFrom(TBL_APPOINTMENTS)
            .addFromInner(usageTable, news.joinUsage(TBL_APPOINTMENTS))
            .addFromLeft(TBL_APPOINTMENT_OWNERS,
                sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_OWNERS, COL_APPOINTMENT))
            .addFromLeft(TBL_APPOINTMENT_ATTENDEES,
                sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_ATTENDEES, COL_APPOINTMENT))
            .addFromLeft(TBL_ATTENDEES,
                sys.joinTables(TBL_ATTENDEES, TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE))
            .setWhere(SqlUtils.and(NewsHelper.getUpdatesCondition(usageTable, userId, startDate),
                SqlUtils.or(
                    SqlUtils.equals(TBL_APPOINTMENT_OWNERS, COL_APPOINTMENT_OWNER, userId),
                    SqlUtils.equals(TBL_ATTENDEES, COL_COMPANY_PERSON, companyPerson))));

        usageTable = NewsConstants.getUsageTable(TBL_APPOINTMENT_ATTENDEES);

        subquery.addUnion(new SqlSelect()
            .addFields(TBL_APPOINTMENTS, idColumn)
            .addFields(usageTable, NewsConstants.COL_USAGE_UPDATE)
            .addFrom(TBL_APPOINTMENTS)
            .addFromInner(TBL_APPOINTMENT_ATTENDEES,
                sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_ATTENDEES, COL_APPOINTMENT))
            .addFromInner(TBL_ATTENDEES,
                sys.joinTables(TBL_ATTENDEES, TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE))
            .addFromInner(usageTable, news.joinUsage(TBL_APPOINTMENT_ATTENDEES))
            .setWhere(SqlUtils.and(NewsHelper.getUpdatesCondition(usageTable, userId, startDate),
                SqlUtils.equals(TBL_ATTENDEES, COL_COMPANY_PERSON, companyPerson))));

        String alias = SqlUtils.uniqueName();

        return new SqlSelect()
            .addFields(alias, idColumn)
            .addMax(alias, NewsConstants.COL_USAGE_UPDATE)
            .addFrom(subquery, alias)
            .addGroup(alias, idColumn);
      }
    });

    HeadlineProducer headlineProducer = new HeadlineProducer() {
      @Override
      public Headline produce(Feed feed, long userId, BeeRowSet rowSet, IsRow row, boolean isNew) {
        String caption = DataUtils.getString(rowSet, row, COL_SUMMARY);
        if (BeeUtils.isEmpty(caption)) {
          caption = BeeUtils.bracket(row.getId());
        }

        List<String> subtitles = Lists.newArrayList();

        String period = TimeUtils.renderPeriod(
            DataUtils.getDateTime(rowSet, row, COL_START_DATE_TIME),
            DataUtils.getDateTime(rowSet, row, COL_END_DATE_TIME));
        if (!BeeUtils.isEmpty(period)) {
          subtitles.add(period);
        }

        AppointmentStatus status = EnumUtils.getEnumByIndex(AppointmentStatus.class,
            DataUtils.getInteger(rowSet, row, COL_STATUS));
        if (status != null) {
          subtitles.add(status.getCaption(usr.getLocalizableConstants(userId)));
        }

        return Headline.create(row.getId(), caption, subtitles, isNew);
      }
    };

    news.registerHeadlineProducer(Feed.APPOINTMENTS_MY, headlineProducer);
    news.registerHeadlineProducer(Feed.APPOINTMENTS_ALL, headlineProducer);
  }

  private boolean checkTable(String name) {
    return sys.isTable(name) && sys.getTable(name).isActive();
  }

  private ResponseObject createAppointment(RequestInfo reqInfo) {
    BeeRowSet rowSet = BeeRowSet.restore(reqInfo.getContent());
    if (rowSet.isEmpty()) {
      return ResponseObject.error(SVC_CREATE_APPOINTMENT, ": rowSet is empty");
    }

    ResponseObject response = deb.commitRow(rowSet);
    if (response.hasErrors()) {
      return response;
    }

    long appId = ((BeeRow) response.getResponse()).getId();

    for (Map.Entry<String, String> entry : APPOINTMENT_CHILDREN.entrySet()) {
      String childTable = entry.getKey();

      List<Long> children = DataUtils.parseIdList(rowSet.getTableProperty(childTable));
      if (!children.isEmpty()) {
        for (long child : children) {
          qs.insertData(new SqlInsert(childTable).addConstant(COL_APPOINTMENT, appId)
              .addConstant(entry.getValue(), child));
        }
      }
    }

    return response;
  }

  private ResponseObject doReport(RequestInfo reqInfo) {
    Integer paramRep = BeeUtils.toIntOrNull(reqInfo.getParameter(PARAM_REPORT));
    if (!BeeUtils.isOrdinal(Report.class, paramRep)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_REPORT);
    }

    BeeRowSet rowSet = BeeRowSet.restore(reqInfo.getContent());
    if (rowSet.isEmpty()) {
      return ResponseObject.error(reqInfo.getService(), ": options rowSet is empty");
    }

    BeeRow row = rowSet.getRow(0);

    JustDate lowerDate = DataUtils.getDate(rowSet, row, COL_LOWER_DATE);
    JustDate upperDate = DataUtils.getDate(rowSet, row, COL_UPPER_DATE);

    Integer lowerHour = DataUtils.getInteger(rowSet, row, COL_LOWER_HOUR);
    Integer upperHour = DataUtils.getInteger(rowSet, row, COL_UPPER_HOUR);

    String atpList = DataUtils.getString(rowSet, row, COL_ATTENDEE_TYPES);
    String attList = DataUtils.getString(rowSet, row, COL_ATTENDEES);

    SqlUpdate update = new SqlUpdate(TBL_REPORT_OPTIONS)
        .addConstant(sys.getVersionName(TBL_REPORT_OPTIONS), System.currentTimeMillis())
        .addConstant(COL_CAPTION, DataUtils.getString(rowSet, row, COL_CAPTION))
        .addConstant(COL_LOWER_DATE, lowerDate)
        .addConstant(COL_UPPER_DATE, upperDate)
        .addConstant(COL_LOWER_HOUR, lowerHour)
        .addConstant(COL_UPPER_HOUR, upperHour)
        .addConstant(COL_ATTENDEE_TYPES, atpList)
        .addConstant(COL_ATTENDEES, attList)
        .setWhere(sys.idEquals(TBL_REPORT_OPTIONS, row.getId()));

    ResponseObject response = qs.updateDataWithResponse(update);
    if (response.hasErrors()) {
      return response;
    }

    List<Long> attendeeTypes = DataUtils.parseIdList(atpList);
    List<Long> attendees = DataUtils.parseIdList(attList);

    Report report = Report.values()[paramRep];
    BeeRowSet result;

    switch (report) {
      case BUSY_HOURS:
        result = getAttendeesByHour(lowerDate, upperDate, lowerHour, upperHour,
            attendeeTypes, attendees, false);
        break;
      case BUSY_MONTHS:
        result = getAttendeesByMonth(lowerDate, upperDate, attendeeTypes, attendees, false);
        break;
      case CANCEL_HOURS:
        result = getAttendeesByHour(lowerDate, upperDate, lowerHour, upperHour,
            attendeeTypes, attendees, true);
        break;
      case CANCEL_MONTHS:
        result = getAttendeesByMonth(lowerDate, upperDate, attendeeTypes, attendees, true);
        break;
      default:
        Assert.untouchable();
        result = null;
    }

    if (result == null) {
      return ResponseObject.response(BeeConst.STRING_EMPTY);
    } else {
      return ResponseObject.response(result);
    }
  }

  private List<BeeRow> getAppointments(Filter filter, Order order, boolean checkVisibility) {
    List<BeeRow> result = Lists.newArrayList();

    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return result;
    }

    Filter queryFilter;

    if (checkVisibility) {
      Filter visible = Filter.or().add(
          Filter.equals(COL_CREATOR, userId),
          Filter.in(sys.getIdName(TBL_APPOINTMENTS), VIEW_APPOINTMENT_OWNERS, COL_APPOINTMENT,
              Filter.equals(COL_APPOINTMENT_OWNER, userId)),
          Filter.isNull(COL_VISIBILITY),
          Filter.isEqual(COL_VISIBILITY, IntegerValue.of(CalendarVisibility.PUBLIC)));

      queryFilter = Filter.and(filter, visible);

    } else {
      queryFilter = filter;
    }

    BeeRowSet appointments = qs.getViewData(VIEW_APPOINTMENTS, queryFilter, order);
    if (!DataUtils.isEmpty(appointments)) {
      result.addAll(appointments.getRows());
    }

    return result;
  }

  private SimpleRowSet getAttendeePeriods(JustDate lowerDate, JustDate upperDate,
      List<Long> attendeeTypes, List<Long> attendees, boolean canceled) {

    SqlSelect ss = new SqlSelect()
        .addFields(TBL_APPOINTMENTS, COL_START_DATE_TIME, COL_END_DATE_TIME)
        .addFields(TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE)
        .addFrom(TBL_APPOINTMENTS)
        .addFromInner(TBL_APPOINTMENT_ATTENDEES,
            sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_ATTENDEES, COL_APPOINTMENT));

    int statusCanceled = AppointmentStatus.CANCELED.ordinal();
    IsCondition statusCondition = canceled
        ? SqlUtils.equals(TBL_APPOINTMENTS, COL_STATUS, statusCanceled)
        : SqlUtils.notEqual(TBL_APPOINTMENTS, COL_STATUS, statusCanceled);

    HasConditions where = SqlUtils.and(statusCondition);

    if (!BeeUtils.isEmpty(attendeeTypes) || !BeeUtils.isEmpty(attendees)) {
      HasConditions attCondition = SqlUtils.or();

      if (!BeeUtils.isEmpty(attendeeTypes)) {
        ss.addFromInner(TBL_ATTENDEES,
            sys.joinTables(TBL_ATTENDEES, TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE));

        for (Long atpId : attendeeTypes) {
          attCondition.add(SqlUtils.equals(TBL_ATTENDEES, COL_ATTENDEE_TYPE, atpId));
        }
      }

      if (!BeeUtils.isEmpty(attendees)) {
        for (Long attId : attendees) {
          attCondition.add(SqlUtils.equals(TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE, attId));
        }
      }

      where.add(attCondition);
    }

    if (lowerDate != null) {
      where.add(SqlUtils.more(TBL_APPOINTMENTS, COL_END_DATE_TIME, lowerDate));
    }
    if (upperDate != null) {
      where.add(SqlUtils.less(TBL_APPOINTMENTS, COL_START_DATE_TIME, upperDate));
    }

    ss.setWhere(where);

    return qs.getData(ss);
  }

  private BeeRowSet getAttendeesByHour(JustDate lowerDate, JustDate upperDate,
      Integer lowerHour, Integer upperHour, List<Long> attendeeTypes, List<Long> attendees,
      boolean canceled) {

    SimpleRowSet data =
        getAttendeePeriods(lowerDate, upperDate, attendeeTypes, attendees, canceled);
    if (data == null || data.getNumberOfRows() <= 0) {
      return null;
    }

    int startIndex = data.getColumnIndex(COL_START_DATE_TIME);
    int endIndex = data.getColumnIndex(COL_END_DATE_TIME);
    int attIndex = data.getColumnIndex(COL_ATTENDEE);

    Table<Long, Integer, Integer> table = HashBasedTable.create();

    Range<DateTime> dateRange = getDateRange(lowerDate, upperDate);
    Range<Integer> hourRange = getHourRange(lowerHour, upperHour);

    for (int r = 0; r < data.getNumberOfRows(); r++) {
      DateTime start = data.getDateTime(r, startIndex);
      DateTime end = data.getDateTime(r, endIndex);

      Long attendee = data.getLong(r, attIndex);
      Map<Integer, Integer> map = splitByHour(start, end, dateRange, hourRange);

      for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
        int hour = entry.getKey();
        int minutes = entry.getValue();

        Integer value = table.get(attendee, hour);
        if (value == null) {
          table.put(attendee, hour, minutes);
        } else {
          table.put(attendee, hour, value + minutes);
        }
      }
    }
    if (table.isEmpty()) {
      return null;
    }

    List<Integer> hours = Lists.newArrayList(table.columnKeySet());
    Collections.sort(hours);

    BeeRowSet attRowSet = qs.getViewData(VIEW_ATTENDEES,
        getAttendeeFilter(attendeeTypes, attendees));
    if (attRowSet.isEmpty()) {
      return null;
    }

    BeeRowSet result = new BeeRowSet(new BeeColumn(ValueType.TEXT, ALS_ATTENDEE_TYPE_NAME),
        new BeeColumn(ValueType.TEXT, COL_ATTENDEE_NAME));
    for (Integer hour : hours) {
      result.addColumn(ValueType.TEXT, hour.toString());
    }

    if (hours.size() > 1) {
      result.addColumn(ValueType.TEXT, usr.getLocalizableConstants().calTotal());
    }
    int columnCount = result.getNumberOfColumns();

    for (BeeRow attRow : attRowSet.getRows()) {
      Map<Integer, Integer> tableRow = table.row(attRow.getId());
      if (tableRow == null || tableRow.isEmpty()) {
        continue;
      }

      String[] values = new String[result.getNumberOfColumns()];
      values[0] = DataUtils.getString(attRowSet, attRow, ALS_ATTENDEE_TYPE_NAME);
      values[1] = DataUtils.getString(attRowSet, attRow, COL_ATTENDEE_NAME);

      for (int c = 0; c < hours.size(); c++) {
        Integer minutes = tableRow.get(hours.get(c));
        values[c + 2] = (minutes == null) ? null : minutes.toString();
      }
      result.addRow(attRow.getId(), values);
    }

    if (hours.size() > 1) {
      totalColumns(result, 2, columnCount - 2, columnCount - 1);
    }
    if (result.getNumberOfRows() > 1) {
      totalRows(result, 2, columnCount - 1, 0, usr.getLocalizableConstants().totalOf() + ":", 0);
    }
    formatTimeColumns(result, 2, columnCount - 1);

    return result;
  }

  private BeeRowSet getAttendeesByMonth(JustDate lowerDate, JustDate upperDate,
      List<Long> attendeeTypes, List<Long> attendees, boolean canceled) {

    SimpleRowSet data =
        getAttendeePeriods(lowerDate, upperDate, attendeeTypes, attendees, canceled);
    if (data == null || data.getNumberOfRows() <= 0) {
      return null;
    }

    int startIndex = data.getColumnIndex(COL_START_DATE_TIME);
    int endIndex = data.getColumnIndex(COL_END_DATE_TIME);
    int attIndex = data.getColumnIndex(COL_ATTENDEE);

    Table<Long, YearMonth, Integer> table = HashBasedTable.create();

    Range<DateTime> range = getDateRange(lowerDate, upperDate);

    for (int r = 0; r < data.getNumberOfRows(); r++) {
      DateTime start = data.getDateTime(r, startIndex);
      DateTime end = data.getDateTime(r, endIndex);

      Long attendee = data.getLong(r, attIndex);

      for (Map.Entry<YearMonth, Integer> entry : splitByYearMonth(start, end, range).entrySet()) {
        YearMonth ym = entry.getKey();
        int minutes = entry.getValue();

        Integer value = table.get(attendee, ym);
        if (value == null) {
          table.put(attendee, ym, minutes);
        } else {
          table.put(attendee, ym, value + minutes);
        }
      }
    }
    if (table.isEmpty()) {
      return null;
    }

    List<YearMonth> months = Lists.newArrayList(table.columnKeySet());
    Collections.sort(months);

    BeeRowSet attRowSet = qs.getViewData(VIEW_ATTENDEES,
        getAttendeeFilter(attendeeTypes, attendees));
    if (attRowSet.isEmpty()) {
      return null;
    }

    BeeRowSet result = new BeeRowSet(new BeeColumn(ValueType.TEXT, ALS_ATTENDEE_TYPE_NAME),
        new BeeColumn(ValueType.TEXT, COL_ATTENDEE_NAME));
    for (YearMonth ym : months) {
      result.addColumn(ValueType.TEXT, ym.toString());
    }

    if (months.size() > 1) {
      result.addColumn(ValueType.TEXT, usr.getLocalizableConstants().calTotal());
    }
    int columnCount = result.getNumberOfColumns();

    for (BeeRow attRow : attRowSet.getRows()) {
      Map<YearMonth, Integer> tableRow = table.row(attRow.getId());
      if (tableRow == null || tableRow.isEmpty()) {
        continue;
      }

      String[] values = new String[columnCount];
      values[0] = DataUtils.getString(attRowSet, attRow, ALS_ATTENDEE_TYPE_NAME);
      values[1] = DataUtils.getString(attRowSet, attRow, COL_ATTENDEE_NAME);

      for (int c = 0; c < months.size(); c++) {
        Integer minutes = tableRow.get(months.get(c));
        values[c + 2] = (minutes == null) ? null : minutes.toString();
      }
      result.addRow(attRow.getId(), values);
    }

    if (months.size() > 1) {
      totalColumns(result, 2, columnCount - 2, columnCount - 1);
    }
    if (result.getNumberOfRows() > 1) {
      totalRows(result, 2, columnCount - 1, 0, usr.getLocalizableConstants().totalOf() + ":", 0);
    }
    formatTimeColumns(result, 2, columnCount - 1);

    return result;
  }

  private BeeRowSet getCalendarAttendees(long userId, long calendarId) {
    Filter calFilter = Filter.equals(COL_CALENDAR, calendarId);

    BeeRowSet calAttTypes = qs.getViewData(VIEW_CAL_ATTENDEE_TYPES, calFilter);
    BeeRowSet calAttendees = qs.getViewData(VIEW_CALENDAR_ATTENDEES, calFilter);

    CompoundFilter attFilter = Filter.or();
    if (!calAttTypes.isEmpty()) {
      attFilter.add(Filter.any(COL_ATTENDEE_TYPE,
          DataUtils.getDistinct(calAttTypes, COL_ATTENDEE_TYPE)));
    }
    if (!calAttendees.isEmpty()) {
      attFilter.add(Filter.idIn(DataUtils.getDistinct(calAttendees, COL_ATTENDEE)));
    }

    if (attFilter.isEmpty()) {
      Long cp = usr.getCompanyPerson(userId);

      if (cp != null) {
        Filter cpFilter = Filter.equals(COL_COMPANY_PERSON, cp);
        BeeRowSet attendees = qs.getViewData(VIEW_ATTENDEES, cpFilter);
        if (!attendees.isEmpty()) {
          return attendees;
        }
      }
    }

    return qs.getViewData(VIEW_ATTENDEES, attFilter);
  }

  private ResponseObject getCalendarItems(RequestInfo reqInfo) {
    long calendarId = BeeUtils.toLong(reqInfo.getParameter(PARAM_CALENDAR_ID));
    if (!DataUtils.isId(calendarId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_CALENDAR_ID);
    }

    Filter calFilter = Filter.equals(COL_CALENDAR, calendarId);
    BeeRowSet calAppTypes = qs.getViewData(VIEW_CAL_APPOINTMENT_TYPES, calFilter);

    CompoundFilter appFilter = Filter.and();
    appFilter.add(VALID_APPOINTMENT);
    appFilter.add(Filter.isNotEqual(COL_STATUS, IntegerValue.of(AppointmentStatus.CANCELED)));

    Long startTime = BeeUtils.toLongOrNull(reqInfo.getParameter(PARAM_START_TIME));
    Long endTime = BeeUtils.toLongOrNull(reqInfo.getParameter(PARAM_END_TIME));

    if (startTime != null) {
      appFilter.add(Filter.isMore(COL_END_DATE_TIME, new LongValue(startTime)));
    }
    if (endTime != null) {
      appFilter.add(Filter.isLess(COL_START_DATE_TIME, new LongValue(endTime)));
    }

    if (!calAppTypes.isEmpty()) {
      appFilter.add(Filter.any(COL_APPOINTMENT_TYPE,
          DataUtils.getDistinct(calAppTypes, COL_APPOINTMENT_TYPE)));
    }

    long millis = System.currentTimeMillis();

    List<BeeRow> appointments = getAppointments(appFilter, null, false);
    long appDuration = System.currentTimeMillis() - millis;

    List<CalendarTask> tasks = getCalendarTasks(calendarId, startTime, endTime);
    long taskDuration = System.currentTimeMillis() - millis - appDuration;

    logger.info(reqInfo.getService(), calendarId,
        (startTime == null) ? BeeConst.STRING_EMPTY : new DateTime(startTime).toCompactString(),
        (endTime == null) ? BeeConst.STRING_EMPTY : new DateTime(endTime).toCompactString(),
        BeeConst.STRING_EQ, appointments.size(), BeeConst.STRING_PLUS, tasks.size(),
        BeeConst.STRING_LEFT_BRACKET, appDuration, BeeConst.STRING_PLUS, taskDuration,
        BeeConst.STRING_EQ, appDuration + taskDuration, BeeConst.STRING_RIGHT_BRACKET);

    Map<String, Object> result = Maps.newHashMap();
    if (!appointments.isEmpty()) {
      result.put(ItemType.APPOINTMENT.name(), appointments);
    }
    if (!tasks.isEmpty()) {
      result.put(ItemType.TASK.name(), tasks);
    }

    if (result.isEmpty()) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result);
    }
  }

  private List<CalendarTask> getCalendarTasks(long calendarId, Long startMillis, Long endMillis) {
    List<CalendarTask> tasks = Lists.newArrayList();

    BeeRowSet calRowSet = qs.getViewData(VIEW_CALENDARS, Filter.compareId(calendarId));
    if (DataUtils.isEmpty(calRowSet)) {
      logger.warning("calendar", calendarId, "not found");
      return tasks;
    }

    BeeRow calendar = calRowSet.getRow(0);
    Long calendarOwner = DataUtils.getLong(calRowSet, calendar, COL_CALENDAR_OWNER);

    boolean assigned;
    boolean delegated;
    boolean observed;

    if (DataUtils.isId(calendarOwner)) {
      assigned = BeeUtils.isTrue(DataUtils.getBoolean(calRowSet, calendar, COL_ASSIGNED_TASKS));
      delegated = BeeUtils.isTrue(DataUtils.getBoolean(calRowSet, calendar, COL_DELEGATED_TASKS));
      observed = BeeUtils.isTrue(DataUtils.getBoolean(calRowSet, calendar, COL_OBSERVED_TASKS));
    } else {
      assigned = false;
      delegated = false;
      observed = false;
    }

    Set<Long> executors = Sets.newHashSet();
    Map<Long, ColorAndStyle> executorAppearance = Maps.newHashMap();

    SimpleRowSet exGroupData = qs.getData(new SqlSelect()
        .setDistinctMode(true)
        .addFields(TBL_USER_GROUPS, COL_UG_USER)
        .addFields(TBL_CAL_EXECUTOR_GROUPS, COL_BACKGROUND, COL_FOREGROUND, COL_STYLE)
        .addFrom(TBL_CAL_EXECUTOR_GROUPS)
        .addFromInner(TBL_USER_GROUPS,
            SqlUtils.join(TBL_CAL_EXECUTOR_GROUPS, COL_EXECUTOR_GROUP,
                TBL_USER_GROUPS, COL_UG_GROUP))
        .setWhere(SqlUtils.equals(TBL_CAL_EXECUTOR_GROUPS, COL_CALENDAR, calendarId)));

    if (!DataUtils.isEmpty(exGroupData)) {
      for (SimpleRow exRow : exGroupData) {
        Long user = exRow.getLong(COL_UG_USER);
        executors.add(user);

        ColorAndStyle cs = ColorAndStyle.maybeCreate(exRow.getValue(COL_BACKGROUND),
            exRow.getValue(COL_FOREGROUND), exRow.getLong(COL_STYLE));
        if (cs != null) {
          if (executorAppearance.containsKey(user)) {
            cs.merge(executorAppearance.get(user), false);
          }
          executorAppearance.put(user, cs);
        }
      }
    }

    SimpleRowSet executorData = qs.getData(new SqlSelect()
        .addFields(TBL_CALENDAR_EXECUTORS, COL_EXECUTOR_USER,
            COL_BACKGROUND, COL_FOREGROUND, COL_STYLE)
        .addFrom(TBL_CALENDAR_EXECUTORS)
        .setWhere(SqlUtils.equals(TBL_CALENDAR_EXECUTORS, COL_CALENDAR, calendarId)));

    if (!DataUtils.isEmpty(executorData)) {
      for (SimpleRow exRow : executorData) {
        Long user = exRow.getLong(COL_EXECUTOR_USER);
        executors.add(user);

        ColorAndStyle cs = ColorAndStyle.maybeCreate(exRow.getValue(COL_BACKGROUND),
            exRow.getValue(COL_FOREGROUND), exRow.getLong(COL_STYLE));
        if (cs != null) {
          if (executorAppearance.containsKey(user)) {
            cs.merge(executorAppearance.get(user), false);
          }
          executorAppearance.put(user, cs);
        }
      }
    }

    if (!assigned && !delegated && !observed && executors.isEmpty()) {
      return tasks;
    }

    EnumMap<TaskType, ColorAndStyle> typeAppearance = Maps.newEnumMap(TaskType.class);

    String source = TaskConstants.TBL_TASKS;
    String idName = sys.getIdName(source);

    HasConditions where = SqlUtils.and();
    where.add(SqlUtils.notEqual(source, TaskConstants.COL_STATUS, TaskStatus.CANCELED.ordinal()));

    if (startMillis != null) {
      where.add(SqlUtils.more(source, TaskConstants.COL_FINISH_TIME, startMillis));
    }
    if (endMillis != null) {
      where.add(SqlUtils.less(source, TaskConstants.COL_START_TIME, endMillis));
    }

    HasConditions match = SqlUtils.or();
    if (assigned) {
      match.add(SqlUtils.equals(source, TaskConstants.COL_EXECUTOR, calendarOwner));
      if (executors.contains(calendarOwner)) {
        executors.remove(calendarOwner);
      }

      ColorAndStyle cs = ColorAndStyle.maybeCreate(
          DataUtils.getString(calRowSet, calendar, COL_ASSIGNED_TASKS_BACKGROUND),
          DataUtils.getString(calRowSet, calendar, COL_ASSIGNED_TASKS_FOREGROUND),
          DataUtils.getLong(calRowSet, calendar, COL_ASSIGNED_TASKS_STYLE));
      if (cs != null) {
        typeAppearance.put(TaskType.ASSIGNED, cs);
      }
    }

    if (delegated) {
      if (executors.contains(calendarOwner)) {
        match.add(SqlUtils.equals(source, TaskConstants.COL_OWNER, calendarOwner));
      } else {
        match.add(SqlUtils.and(
            SqlUtils.equals(source, TaskConstants.COL_OWNER, calendarOwner),
            SqlUtils.notEqual(source, TaskConstants.COL_EXECUTOR, calendarOwner)));
      }

      ColorAndStyle cs = ColorAndStyle.maybeCreate(
          DataUtils.getString(calRowSet, calendar, COL_DELEGATED_TASKS_BACKGROUND),
          DataUtils.getString(calRowSet, calendar, COL_DELEGATED_TASKS_FOREGROUND),
          DataUtils.getLong(calRowSet, calendar, COL_DELEGATED_TASKS_STYLE));
      if (cs != null) {
        typeAppearance.put(TaskType.DELEGATED, cs);
      }
    }

    if (observed) {
      match.add(SqlUtils.and(
          SqlUtils.notEqual(source, TaskConstants.COL_OWNER, calendarOwner),
          SqlUtils.notEqual(source, TaskConstants.COL_EXECUTOR, calendarOwner),
          SqlUtils.in(source, idName, TaskConstants.TBL_TASK_USERS, TaskConstants.COL_TASK,
              SqlUtils
                  .equals(TaskConstants.TBL_TASK_USERS, COL_USER, calendarOwner))));

      ColorAndStyle cs = ColorAndStyle.maybeCreate(
          DataUtils.getString(calRowSet, calendar, COL_OBSERVED_TASKS_BACKGROUND),
          DataUtils.getString(calRowSet, calendar, COL_OBSERVED_TASKS_FOREGROUND),
          DataUtils.getLong(calRowSet, calendar, COL_OBSERVED_TASKS_STYLE));
      if (cs != null) {
        typeAppearance.put(TaskType.OBSERVED, cs);
      }
    }

    if (!executors.isEmpty()) {
      match.add(SqlUtils.inList(source, TaskConstants.COL_EXECUTOR, executors));
    }

    where.add(match);

    SqlSelect query = new SqlSelect()
        .addFields(source, idName, TaskConstants.COL_START_TIME, TaskConstants.COL_FINISH_TIME,
            TaskConstants.COL_SUMMARY, TaskConstants.COL_DESCRIPTION,
            TaskConstants.COL_PRIORITY, TaskConstants.COL_STATUS,
            TaskConstants.COL_OWNER, TaskConstants.COL_EXECUTOR)
        .addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_COMPANY_NAME)
        .addFrom(source)
        .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, source, COL_COMPANY))
        .setWhere(where)
        .addOrder(source, TaskConstants.COL_START_TIME, TaskConstants.COL_FINISH_TIME, idName);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return tasks;
    }

    for (SimpleRow row : data) {
      Long id = row.getLong(idName);
      if (!DataUtils.isId(id)) {
        continue;
      }

      Long owner = row.getLong(TaskConstants.COL_OWNER);
      Long executor = row.getLong(TaskConstants.COL_EXECUTOR);

      Set<Long> observers = Sets.newHashSet();

      Long[] taskUsers = qs.getLongColumn(new SqlSelect()
          .addFields(TaskConstants.TBL_TASK_USERS, COL_USER)
          .addFrom(TaskConstants.TBL_TASK_USERS)
          .setWhere(SqlUtils.and(
              SqlUtils.equals(TaskConstants.TBL_TASK_USERS, TaskConstants.COL_TASK, id),
              SqlUtils.notEqual(TaskConstants.TBL_TASK_USERS, COL_USER, owner),
              SqlUtils.notEqual(TaskConstants.TBL_TASK_USERS, COL_USER,
                  executor))));

      if (taskUsers != null && taskUsers.length > 0) {
        for (Long user : taskUsers) {
          observers.add(user);
        }
      }

      TaskType type;
      if (!DataUtils.isId(calendarOwner)) {
        type = TaskType.ALL;
      } else if (Objects.equal(executor, calendarOwner)) {
        type = TaskType.ASSIGNED;
      } else if (Objects.equal(owner, calendarOwner)) {
        type = TaskType.DELEGATED;
      } else if (observers.contains(calendarOwner)) {
        type = TaskType.OBSERVED;
      } else {
        type = TaskType.ALL;
      }

      CalendarTask task = new CalendarTask(type, id, row);

      if (!observers.isEmpty()) {
        task.setObservers(observers);
      }

      ColorAndStyle cs;
      if (executor != null && executorAppearance.containsKey(executor)) {
        cs = executorAppearance.get(executor);
      } else {
        cs = null;
      }

      if (typeAppearance.containsKey(type)) {
        if (cs == null) {
          cs = typeAppearance.get(type);
        } else {
          cs = cs.copy();
          cs.merge(typeAppearance.get(type), false);
        }
      }

      if (cs != null) {
        task.setBackground(cs.getBackground());
        task.setForeground(cs.getForeground());
        task.setStyle(cs.getStyle());
      }

      tasks.add(task);
    }

    return tasks;
  }

  private ResponseObject getOverlappingAppointments(RequestInfo reqInfo) {
    String appId = reqInfo.getParameter(PARAM_APPOINTMENT_ID);

    String start = reqInfo.getParameter(PARAM_APPOINTMENT_START);
    if (!BeeUtils.isLong(start)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_APPOINTMENT_START);
    }
    String end = reqInfo.getParameter(PARAM_APPOINTMENT_END);
    if (!BeeUtils.isLong(end)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_APPOINTMENT_END);
    }

    List<Long> resources = DataUtils.parseIdList(reqInfo.getParameter(PARAM_ATTENDEES));
    if (resources.isEmpty()) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_ATTENDEES);
    }

    CompoundFilter filter = Filter.and();
    filter.add(Filter.isNotEqual(COL_STATUS, IntegerValue.of(AppointmentStatus.CANCELED)));

    if (BeeUtils.isLong(appId)) {
      filter.add(Filter.compareId(Operator.NE, BeeUtils.toLong(appId)));
    }

    filter.add(Filter.isMore(COL_END_DATE_TIME, new LongValue(BeeUtils.toLong(start))));
    filter.add(Filter.isLess(COL_START_DATE_TIME, new LongValue(BeeUtils.toLong(end))));

    filter.add(Filter.in(sys.getIdName(TBL_APPOINTMENTS), VIEW_APPOINTMENT_ATTENDEES,
        COL_APPOINTMENT, Filter.any(COL_ATTENDEE, resources)));

    BeeRowSet appointments = qs.getViewData(VIEW_APPOINTMENTS, filter);
    return ResponseObject.response(appointments);
  }

  private ResponseObject getReportOptions(RequestInfo reqInfo) {
    Integer report = BeeUtils.toIntOrNull(reqInfo.getParameter(PARAM_REPORT));
    if (!BeeUtils.isOrdinal(Report.class, report)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_REPORT);
    }

    long userId = usr.getCurrentUserId();

    Filter filter = Filter.and(Filter.equals(COL_USER, userId),
        Filter.isEqual(COL_REPORT, new IntegerValue(report)));

    BeeRowSet rowSet = qs.getViewData(VIEW_REPORT_OPTIONS, filter);
    if (rowSet.getNumberOfRows() == 1) {
      return ResponseObject.response(rowSet.getRow(0));
    }

    SqlInsert sqlInsert = new SqlInsert(TBL_REPORT_OPTIONS).addConstant(COL_USER, userId)
        .addConstant(COL_REPORT, report);

    ResponseObject response = qs.insertDataWithResponse(sqlInsert);
    if (response.hasErrors()) {
      return response;
    }

    BeeRowSet result = qs.getViewData(VIEW_REPORT_OPTIONS, filter);
    if (result.getNumberOfRows() != 1) {
      return ResponseObject.error(reqInfo.getService(), COL_USER, userId, COL_REPORT, report,
          "report options not created");
    }
    return ResponseObject.response(result.getRow(0));
  }

  private BeeRowSet getUserCalAttendees(long ucId, long userId, long calendarId, boolean isNew) {
    BeeRowSet attendees = qs.getViewData(VIEW_ATTENDEES);
    if (DataUtils.isEmpty(attendees)) {
      logger.warning("attendees not available, user", userId);
      return null;
    }

    List<Long> attIds = attendees.getRowIds();

    Filter filter = Filter.and(Filter.equals(COL_USER_CALENDAR, ucId),
        Filter.any(COL_ATTENDEE, attIds));

    if (!isNew) {
      BeeRowSet ucAttendees = qs.getViewData(VIEW_USER_CAL_ATTENDEES, filter);
      if (!ucAttendees.isEmpty()) {
        return ucAttendees;
      }
    }

    BeeRowSet calendarAttendees = getCalendarAttendees(userId, calendarId);
    if (DataUtils.isEmpty(calendarAttendees)) {
      logger.warning("calendar attendees not available, calendar", calendarId, "user", userId);
      return null;
    }

    int ord = 0;
    for (BeeRow row : calendarAttendees.getRows()) {
      SqlInsert sqlInsert = new SqlInsert(TBL_USER_CAL_ATTENDEES)
          .addConstant(COL_USER_CALENDAR, ucId)
          .addConstant(COL_ATTENDEE, row.getId())
          .addConstant(COL_ENABLED, true)
          .addConstant(COL_ORDINAL, ord++);

      qs.insertDataWithResponse(sqlInsert);
    }

    return qs.getViewData(VIEW_USER_CAL_ATTENDEES, filter);
  }

  private ResponseObject getUserCalendar(RequestInfo reqInfo) {
    if (!checkTable(TBL_USER_CALENDARS)) {
      return ResponseObject.error("table not active:", TBL_USER_CALENDARS);
    }

    long calendarId = BeeUtils.toLong(reqInfo.getParameter(PARAM_CALENDAR_ID));
    if (!DataUtils.isId(calendarId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), PARAM_CALENDAR_ID);
    }

    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return ResponseObject.error(reqInfo.getService(), "current user not available");
    }

    Filter filter = Filter.and(Filter.equals(COL_CALENDAR, calendarId),
        Filter.equals(COL_USER, userId));

    BeeRowSet ucRowSet = qs.getViewData(VIEW_USER_CALENDARS, filter);
    if (!ucRowSet.isEmpty()) {
      BeeRow row = ucRowSet.getRow(0);
      BeeRowSet ucAttendees = getUserCalAttendees(row.getId(), userId, calendarId, false);
      if (!DataUtils.isEmpty(ucAttendees)) {
        row.setProperty(TBL_USER_CAL_ATTENDEES, ucAttendees.serialize());
      }

      return ResponseObject.response(ucRowSet);
    }

    BeeRowSet calRowSet = qs.getViewData(VIEW_CALENDARS, Filter.compareId(calendarId));
    if (calRowSet.isEmpty()) {
      return ResponseObject.error(reqInfo.getService(), PARAM_CALENDAR_ID, calendarId,
          "calendar not found");
    }

    CalendarSettings settings = CalendarSettings.create(calRowSet.getRow(0),
        calRowSet.getColumns());

    SqlInsert sqlInsert = new SqlInsert(TBL_USER_CALENDARS)
        .addConstant(COL_CALENDAR, calendarId)
        .addConstant(COL_USER, userId)
        .addConstant(COL_PIXELS_PER_INTERVAL, settings.getPixelsPerInterval())
        .addConstant(COL_INTERVALS_PER_HOUR, settings.getIntervalsPerHour())
        .addConstant(COL_WORKING_HOUR_START, settings.getWorkingHourStart())
        .addConstant(COL_WORKING_HOUR_END, settings.getWorkingHourEnd())
        .addConstant(COL_SCROLL_TO_HOUR, settings.getScrollToHour())
        .addConstant(COL_DEFAULT_DISPLAYED_DAYS, settings.getDefaultDisplayedDays());

    if (settings.getTimeBlockClickNumber() != null) {
      sqlInsert.addConstant(COL_TIME_BLOCK_CLICK_NUMBER,
          settings.getTimeBlockClickNumber().ordinal());
    }

    if (settings.getMultidayLayout() != null) {
      sqlInsert.addConstant(COL_MULTIDAY_LAYOUT, settings.getMultidayLayout().ordinal());
    }
    if (settings.getMultidayTaskLayout() != null) {
      sqlInsert.addConstant(COL_MULTIDAY_TASK_LAYOUT, settings.getMultidayTaskLayout().ordinal());
    }

    for (ViewType view : ViewType.values()) {
      if (settings.isVisible(view)) {
        sqlInsert.addConstant(view.getColumnId(), true);
      }
    }

    ResponseObject response = qs.insertDataWithResponse(sqlInsert);
    if (response.hasErrors()) {
      return response;
    }

    BeeRowSet result = qs.getViewData(VIEW_USER_CALENDARS, filter);
    if (result.isEmpty()) {
      return ResponseObject.error(SVC_GET_USER_CALENDAR, PARAM_CALENDAR_ID, calendarId,
          "user calendar not created");
    }

    BeeRow row = result.getRow(0);
    BeeRowSet ucAttendees = getUserCalAttendees(row.getId(), userId, calendarId, true);
    if (!DataUtils.isEmpty(ucAttendees)) {
      row.setProperty(TBL_USER_CAL_ATTENDEES, ucAttendees.serialize());
    }

    return ResponseObject.response(result);
  }

  @Timeout
  private void notifyEvent(Timer timer) {
    long reminderId = (Long) timer.getInfo();
    logger.debug("Fired timer:", reminderId);

    IsCondition wh = sys.idEquals(TBL_APPOINTMENT_REMINDERS, reminderId);

    String personContacts = SqlUtils.uniqueName();
    String personEmails = SqlUtils.uniqueName();
    String personEmail = SqlUtils.uniqueName();

    SimpleRow data = qs.getRow(new SqlSelect()
        .addFields(TBL_APPOINTMENTS, COL_START_DATE_TIME)
        .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
        .addFields(TBL_APPOINTMENT_REMINDERS, COL_APPOINTMENT, COL_MESSAGE)
        .addFields(TBL_REMINDER_TYPES, COL_REMINDER_METHOD,
            COL_REMINDER_TEMPLATE_CAPTION, COL_REMINDER_TEMPLATE)
        .addField(personEmails, COL_EMAIL_ADDRESS, personEmail)
        .addFrom(TBL_APPOINTMENTS)
        .addFromInner(TBL_APPOINTMENT_REMINDERS,
            sys.joinTables(TBL_APPOINTMENTS, TBL_APPOINTMENT_REMINDERS, COL_APPOINTMENT))
        .addFromInner(TBL_REMINDER_TYPES,
            sys.joinTables(TBL_REMINDER_TYPES, TBL_APPOINTMENT_REMINDERS,
                COL_REMINDER_TYPE))
        .addFromLeft(VIEW_COMPANIES,
            sys.joinTables(VIEW_COMPANIES, TBL_APPOINTMENTS, COL_COMPANY))
        .addFromLeft(TBL_CONTACTS, sys.joinTables(TBL_CONTACTS, VIEW_COMPANIES, COL_CONTACT))
        .addFromLeft(TBL_EMAILS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
        .addFromLeft(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_APPOINTMENT_REMINDERS,
                COL_RECIPIENT))
        .addFromLeft(TBL_CONTACTS, personContacts,
            sys.joinTables(TBL_CONTACTS, personContacts, TBL_COMPANY_PERSONS, COL_CONTACT))
        .addFromLeft(TBL_EMAILS, personEmails,
            sys.joinTables(TBL_EMAILS, personEmails, personContacts, COL_EMAIL))
        .setWhere(SqlUtils.and(wh,
            SqlUtils.more(TBL_APPOINTMENTS, COL_START_DATE_TIME,
                System.currentTimeMillis() - TimeUtils.MILLIS_PER_MINUTE),
            SqlUtils.notEqual(TBL_APPOINTMENTS, COL_STATUS,
                AppointmentStatus.CANCELED.ordinal()))));

    if (data != null) {
      notificationTimers.remove(data.getLong(COL_APPOINTMENT), timer);
      String error = null;
      String subject = data.getValue(COL_REMINDER_TEMPLATE_CAPTION);
      String template = BeeUtils.notEmpty(data.getValue(COL_MESSAGE),
          data.getValue(COL_REMINDER_TEMPLATE));

      if (BeeUtils.isEmpty(subject)) {
        error = "No reminder caption specified";
      }
      if (BeeUtils.isEmpty(error) && BeeUtils.isEmpty(template)) {
        error = "No reminder message specified";
      }
      if (BeeUtils.isEmpty(error)) {
        ReminderMethod method = EnumUtils.getEnumByIndex(ReminderMethod.class,
            data.getInt(COL_REMINDER_METHOD));

        if (method == ReminderMethod.EMAIL) {
          Long account = mail.getSenderAccountId("CalendarRemainders");
          String email = BeeUtils.notEmpty(data.getValue(personEmail),
              data.getValue(COL_EMAIL_ADDRESS));

          if (!DataUtils.isId(account)) {
            error = "No default account specified";
          } else if (BeeUtils.isEmpty(email)) {
            error = "No recipient email address specified";
          } else {
            ResponseObject response = mail.sendMail(account, email, subject,
                template.replace("{time}", data.getDateTime(COL_START_DATE_TIME).toString()));

            if (response.hasErrors()) {
              error = ArrayUtils.toString(response.getErrors());
            }
          }
        } else {
          error = "Unsupported reminder method: " + method;
        }
      }
      if (!BeeUtils.isEmpty(error)) {
        logger.severe(error);
      }
      qs.updateData(new SqlUpdate(TBL_APPOINTMENT_REMINDERS)
          .addConstant(COL_SENT, System.currentTimeMillis())
          .addConstant(COL_ERROR, error)
          .setWhere(wh));
    }
  }

  private ResponseObject saveActiveView(RequestInfo reqInfo) {
    Long rowId = BeeUtils.toLongOrNull(reqInfo.getParameter(PARAM_USER_CALENDAR_ID));
    if (!DataUtils.isId(rowId)) {
      return ResponseObject.parameterNotFound(SVC_SAVE_ACTIVE_VIEW, PARAM_USER_CALENDAR_ID);
    }

    Integer activeView = BeeUtils.toIntOrNull(reqInfo.getParameter(PARAM_ACTIVE_VIEW));
    if (!BeeUtils.isOrdinal(ViewType.class, activeView)) {
      return ResponseObject.parameterNotFound(SVC_SAVE_ACTIVE_VIEW, PARAM_ACTIVE_VIEW);
    }

    SqlUpdate update = new SqlUpdate(TBL_USER_CALENDARS).addConstant(COL_ACTIVE_VIEW, activeView)
        .setWhere(sys.idEquals(TBL_USER_CALENDARS, rowId));

    return qs.updateDataWithResponse(update);
  }

  private void setAppopintmentProperties(BeeRowSet rowSet) {
    List<Long> appIds = rowSet.getRowIds();

    for (Map.Entry<String, String> entry : APPOINTMENT_CHILDREN.entrySet()) {
      String source = entry.getKey();
      String field = entry.getValue();

      SqlSelect query = new SqlSelect()
          .addFields(source, COL_APPOINTMENT, field)
          .addFrom(source)
          .setWhere(SqlUtils.inList(source, COL_APPOINTMENT, appIds))
          .addOrder(source, COL_APPOINTMENT, field);

      SimpleRowSet data = qs.getData(query);

      if (!DataUtils.isEmpty(data)) {
        Multimap<Long, Long> children = ArrayListMultimap.create();
        for (SimpleRow childRow : data) {
          children.put(childRow.getLong(0), childRow.getLong(1));
        }

        for (BeeRow row : rowSet) {
          if (children.containsKey(row.getId())) {
            row.setProperty(source, DataUtils.buildIdList(children.get(row.getId())));
          }
        }
      }
    }
  }

  private ResponseObject updateAppointment(RequestInfo reqInfo) {
    BeeRowSet newRowSet = BeeRowSet.restore(reqInfo.getContent());
    if (newRowSet.isEmpty()) {
      return ResponseObject.error(reqInfo.getService(), ": rowSet is empty");
    }

    long appId = newRowSet.getRow(0).getId();
    if (!DataUtils.isId(appId)) {
      return ResponseObject.error(reqInfo.getService(), ": invalid row id", appId);
    }

    String viewName = VIEW_APPOINTMENTS;
    BeeRowSet oldRowSet = qs.getViewData(viewName, Filter.compareId(appId));
    if (oldRowSet == null || oldRowSet.isEmpty()) {
      return ResponseObject.error(reqInfo.getService(), ": old row not found", appId);
    }

    BeeRow oldRow = oldRowSet.getRow(0);

    BeeRowSet updated = DataUtils.getUpdated(viewName, oldRowSet.getColumns(), oldRow,
        newRowSet.getRow(0), null);

    ResponseObject response;
    if (updated == null) {
      response = ResponseObject.response(oldRow);
    } else {
      response = deb.commitRow(updated);
      if (response.hasErrors()) {
        return response;
      }
    }

    boolean childrenChanged = false;
    boolean updateReminders = false;

    for (Map.Entry<String, String> entry : APPOINTMENT_CHILDREN.entrySet()) {
      String table = entry.getKey();
      String column = entry.getValue();

      List<Long> oldValues = DataUtils.parseIdList(oldRow.getProperty(table));
      List<Long> newValues = DataUtils.parseIdList(newRowSet.getTableProperty(table));

      if (TBL_APPOINTMENT_REMINDERS.equals(table)) {
        updateReminders = !oldValues.isEmpty() || !newValues.isEmpty();
      }

      if (!BeeUtils.sameElements(oldValues, newValues)
          && updateChildren(table, COL_APPOINTMENT, appId, column, oldValues, newValues)) {
        childrenChanged = true;
      }
    }

    if (childrenChanged) {
      news.maybeRecordUpdate(VIEW_APPOINTMENTS, appId);
    }
    if (updateReminders) {
      createNotificationTimers(Pair.of(TBL_APPOINTMENTS, appId));
    }

    return response;
  }

  private boolean updateChildren(String tblName, String parentRelation, long parentId,
      String columnId, List<Long> oldValues, List<Long> newValues) {

    List<Long> insert = Lists.newArrayList(newValues);
    insert.removeAll(oldValues);

    List<Long> delete = Lists.newArrayList(oldValues);
    delete.removeAll(newValues);

    if (insert.isEmpty() && delete.isEmpty()) {
      return false;

    } else {
      for (Long value : insert) {
        qs.insertData(new SqlInsert(tblName).addConstant(parentRelation, parentId)
            .addConstant(columnId, value));
      }
      for (Long value : delete) {
        IsCondition condition = SqlUtils.equals(tblName, parentRelation, parentId, columnId, value);
        qs.updateData(new SqlDelete(tblName).setWhere(condition));
      }

      return true;
    }
  }
}
