package com.butent.bee.server.modules.tasks;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.server.Config;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.ExtensionIcons;
import com.butent.bee.server.modules.administration.FileStorageBean;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.css.values.WhiteSpace;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.DateValue;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.Element;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.Tbody;
import com.butent.bee.shared.html.builder.elements.Td;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants.ReminderMethod;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskEvent;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.Headline;
import com.butent.bee.shared.news.HeadlineProducer;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.CronExpression;
import com.butent.bee.shared.time.DateRange;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.ScheduleDateRange;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.time.WorkdayTransition;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.Timer;

@Stateless
@LocalBean
public class TasksModuleBean implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(TasksModuleBean.class);

  @EJB
  SystemBean sys;
  @EJB
  DataEditorBean deb;
  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  NewsBean news;
  @EJB
  FileStorageBean fs;
  @EJB
  ParamHolderBean prm;
  @EJB
  MailModuleBean mail;

  @Resource
  EJBContext ctx;

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> result = new ArrayList<>();

    List<SearchResult> tasksSr = qs.getSearchResults(VIEW_TASKS,
        Filter.anyContains(Sets.newHashSet(COL_SUMMARY, COL_DESCRIPTION,
            ALS_COMPANY_NAME, ALS_EXECUTOR_FIRST_NAME, ALS_EXECUTOR_LAST_NAME),
            query));
    result.addAll(tasksSr);

    List<SearchResult> rtSr = qs.getSearchResults(VIEW_RECURRING_TASKS,
        Filter.anyContains(Sets.newHashSet(COL_SUMMARY, COL_DESCRIPTION, ALS_COMPANY_NAME),
            query));
    result.addAll(rtSr);

    return result;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    if (BeeUtils.isPrefix(svc, CRM_TASK_PREFIX)) {
      response = doTaskEvent(BeeUtils.removePrefix(svc, CRM_TASK_PREFIX), reqInfo);

    } else if (BeeUtils.same(svc, SVC_ACCESS_TASK)) {
      response = accessTask(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_TASK_DATA)) {
      response = getTaskData(reqInfo);

    } else if (BeeUtils.same(svc, SVC_EXTEND_TASK)) {
      response = extendTask(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_CHANGED_TASKS)) {
      response = getChangedTasks();

    } else if (BeeUtils.same(svc, SVC_TASKS_REPORTS_COMPANY_TIMES)) {
      response = getCompanyTimesReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_TASKS_REPORTS_TYPE_HOURS)) {
      response = getTypeHoursReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_TASKS_REPORTS_USERS_HOURS)) {
      response = getUsersHoursReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_REQUEST_FILES)) {
      response = getRequestFiles(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_REQUEST)));

    } else if (BeeUtils.same(svc, SVC_RT_GET_SCHEDULING_DATA)) {
      response = getSchedulingData(reqInfo);

    } else if (BeeUtils.same(svc, SVC_RT_SPAWN)) {
      response = spawnTasks(reqInfo);

    } else if (BeeUtils.same(svc, SVC_RT_COPY)) {
      response = copyRecurringTask(reqInfo);

    } else if (BeeUtils.same(svc, SVC_RT_SCHEDULE)) {
      response = scheduleTasks(reqInfo);

    } else if (BeeUtils.same(svc, SVC_CONFIRM_TASKS)) {
      response = confirmTasks(reqInfo);
    } else {
      String msg = BeeUtils.joinWords("CRM service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();
    List<BeeParameter> params = Lists.newArrayList(
        BeeParameter.createText(module, PRM_END_OF_WORK_DAY)
        );
    return params;
  }

  @Override
  public Module getModule() {
    return Module.TASKS;
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

        if (event.isTarget(VIEW_RT_FILES)) {
          ExtensionIcons.setIcons(event.getRowset(), ALS_FILE_NAME, PROP_ICON);

        } else if (event.isTarget(VIEW_TASKS) || event.isTarget(VIEW_RELATED_TASKS)) {
          BeeRowSet rowSet = event.getRowset();

          if (!rowSet.isEmpty()) {
            Set<Long> taskIds = new HashSet<>();
            Long id;

            if (rowSet.getNumberOfRows() < 100) {
              for (BeeRow row : rowSet.getRows()) {
                switch (event.getTargetName()) {
                  case VIEW_TASKS:
                    id = row.getId();
                    break;
                  case VIEW_RELATED_TASKS:
                    id = row.getLong(rowSet.getColumnIndex(COL_TASK));
                    break;
                  default:
                    id = null;
                }

                if (DataUtils.isId(id)) {
                  taskIds.add(id);
                }
              }
            }

            SqlSelect tuQuery = new SqlSelect().addFrom(TBL_TASK_USERS)
                .addFields(TBL_TASK_USERS, COL_TASK, COL_LAST_ACCESS, COL_STAR);

            IsCondition uwh = SqlUtils.equals(TBL_TASK_USERS, COL_USER, usr.getCurrentUserId());

            if (taskIds.isEmpty()) {
              tuQuery.setWhere(uwh);
            } else {
              tuQuery.setWhere(SqlUtils.and(uwh,
                  SqlUtils.inList(TBL_TASK_USERS, COL_TASK, taskIds)));
            }

            SimpleRowSet tuData = qs.getData(tuQuery);
            int taskIndex = tuData.getColumnIndex(COL_TASK);
            int accessIndex = tuData.getColumnIndex(COL_LAST_ACCESS);
            int starIndex = tuData.getColumnIndex(COL_STAR);

            for (SimpleRow tuRow : tuData) {
              long taskId = tuRow.getLong(taskIndex);
              BeeRow row = event.isTarget(VIEW_RELATED_TASKS)
                  ? rowSet.findRow(Filter.equals(COL_TASK, taskId)) : rowSet.getRowById(taskId);

              if (row != null) {
                row.setProperty(PROP_USER, BeeConst.STRING_PLUS);

                if (tuRow.getValue(accessIndex) != null) {
                  row.setProperty(PROP_LAST_ACCESS, tuRow.getValue(accessIndex));
                }
                if (tuRow.getValue(starIndex) != null) {
                  row.setProperty(PROP_STAR, tuRow.getValue(starIndex));
                }
              }
            }

            SqlSelect teQuery = new SqlSelect().addFrom(TBL_TASK_EVENTS)
                .addFields(TBL_TASK_EVENTS, COL_TASK)
                .addMax(TBL_TASK_EVENTS, COL_PUBLISH_TIME)
                .addGroup(TBL_TASK_EVENTS, COL_TASK);

            if (!taskIds.isEmpty()) {
              teQuery.setWhere(SqlUtils.inList(TBL_TASK_EVENTS, COL_TASK, taskIds));
            }

            SimpleRowSet teData = qs.getData(teQuery);
            taskIndex = teData.getColumnIndex(COL_TASK);
            int publishIndex = teData.getColumnIndex(COL_PUBLISH_TIME);

            for (SimpleRow teRow : teData) {
              if (teRow.getValue(publishIndex) != null) {
                long taskId = teRow.getLong(taskIndex);
                BeeRow row = event.isTarget(VIEW_RELATED_TASKS)
                    ? rowSet.findRow(Filter.equals(COL_TASK, taskId)) : rowSet.getRowById(taskId);

                if (row != null) {
                  row.setProperty(PROP_LAST_PUBLISH, teRow.getValue(publishIndex));
                }
              }
            }
          }
        }
      }

      @Subscribe
      public void fillTasksTimeData(ViewQueryEvent event) {
        if (event.isBefore()) {
          return;
        }

        if (!BeeUtils.same(VIEW_TASKS, event.getTargetName())) {
          return;
        }

        BeeRowSet taskRows = event.getRowset();

        if (taskRows.isEmpty()) {
          return;
        }

        int idxActualDuration = DataUtils.getColumnIndex(COL_ACTUAL_DURATION,
            taskRows.getColumns(), false);
        int idxActualExpenses = DataUtils.getColumnIndex(COL_ACTUAL_EXPENSES,
            taskRows.getColumns(), false);

        List<Long> rowIds = taskRows.getRowIds();

        SimpleRowSet timesData = getTaskActualTimesAndExpenses(rowIds);

        if (timesData.isEmpty()) {
          return;
        }

        Map<String, String> times =
            Codec.deserializeMap(timesData.getValue(0, COL_ACTUAL_DURATION));
        Map<String, String> expenses =
            Codec.deserializeMap(timesData.getValue(0, COL_ACTUAL_EXPENSES));

        for (BeeRow row : taskRows) {
          if (row == null) {
            continue;
          }

          if (!BeeUtils.isNegative(idxActualDuration)) {
            row.setValue(idxActualDuration, times.get(BeeUtils.toString(row.getId())));
          }

          if (!BeeUtils.isNegative(idxActualExpenses)) {
            row.setValue(idxActualExpenses, expenses.get(BeeUtils.toString(row.getId())));
          }
        }
      }
    });

    TaskUsageQueryProvider usageQueryProvider = new TaskUsageQueryProvider();

    news.registerUsageQueryProvider(Feed.TASKS_ALL, usageQueryProvider);
    news.registerUsageQueryProvider(Feed.TASKS_ASSIGNED, usageQueryProvider);
    news.registerUsageQueryProvider(Feed.TASKS_DELEGATED, usageQueryProvider);
    news.registerUsageQueryProvider(Feed.TASKS_OBSERVED, usageQueryProvider);

    HeadlineProducer headlineProducer = new HeadlineProducer() {
      @Override
      public Headline produce(Feed feed, long userId, BeeRowSet rowSet, IsRow row, boolean isNew) {
        String caption = DataUtils.getString(rowSet, row, COL_SUMMARY);
        if (BeeUtils.isEmpty(caption)) {
          caption = BeeUtils.bracket(row.getId());
        }

        List<String> subtitles = new ArrayList<>();

        DateTime finish = DataUtils.getDateTime(rowSet, row, COL_FINISH_TIME);
        if (finish != null) {
          subtitles.add(finish.toCompactString());
        }

        TaskStatus status = EnumUtils.getEnumByIndex(TaskStatus.class,
            DataUtils.getInteger(rowSet, row, COL_STATUS));
        if (status != null) {
          subtitles.add(status.getCaption(usr.getLocalizableConstants(userId)));
        }

        if (feed != Feed.TASKS_ASSIGNED) {
          subtitles.add(BeeUtils.joinWords(
              DataUtils.getString(rowSet, row, ALS_EXECUTOR_FIRST_NAME),
              DataUtils.getString(rowSet, row, ALS_EXECUTOR_LAST_NAME)));
        }

        return Headline.create(row.getId(), caption, subtitles, isNew);
      }
    };

    news.registerHeadlineProducer(Feed.TASKS_ALL, headlineProducer);
    news.registerHeadlineProducer(Feed.TASKS_ASSIGNED, headlineProducer);
    news.registerHeadlineProducer(Feed.TASKS_DELEGATED, headlineProducer);
    news.registerHeadlineProducer(Feed.TASKS_OBSERVED, headlineProducer);

    BeeView.registerConditionProvider(FILTER_TASKS_NEW, new BeeView.ConditionProvider() {
      @Override
      public IsCondition getCondition(BeeView view, List<String> args) {
        Long userId = usr.getCurrentUserId();
        if (userId == null) {
          return null;
        }

        return SqlUtils.in(TBL_TASKS, COL_TASK_ID, TBL_TASK_USERS, COL_TASK,
            SqlUtils.and(SqlUtils.equals(TBL_TASK_USERS, COL_USER, userId),
                SqlUtils.isNull(TBL_TASK_USERS, COL_LAST_ACCESS)));
      }
    });

    BeeView.registerConditionProvider(FILTER_TASKS_UPDATED, new BeeView.ConditionProvider() {
      @Override
      public IsCondition getCondition(BeeView view, List<String> args) {
        Long userId = usr.getCurrentUserId();
        if (userId == null) {
          return null;
        }

        SqlSelect query = new SqlSelect().setDistinctMode(true)
            .addFields(TBL_TASK_EVENTS, COL_TASK)
            .addFrom(TBL_TASK_EVENTS)
            .addFromInner(TBL_TASK_USERS,
                SqlUtils.join(TBL_TASK_EVENTS, COL_TASK, TBL_TASK_USERS, COL_TASK))
            .setWhere(SqlUtils.and(
                SqlUtils.notEqual(TBL_TASK_EVENTS, COL_PUBLISHER, userId),
                SqlUtils.equals(TBL_TASK_USERS, COL_USER, userId),
                SqlUtils.joinMore(TBL_TASK_EVENTS, COL_PUBLISH_TIME,
                    TBL_TASK_USERS, COL_LAST_ACCESS)));

        return SqlUtils.in(TBL_TASKS, COL_TASK_ID, query);
      }
    });
  }

  public SimpleRowSet getTaskActualTimesAndExpenses(List<Long> ids) {
    SimpleRowSet result = new SimpleRowSet(new String[] {
        COL_ACTUAL_DURATION, COL_ACTUAL_EXPENSES
    });

    Filter idFilter = Filter.any(COL_TASK, ids);
    Filter durationFilter = Filter.notNull(COL_DURATION);

    BeeRowSet taskEvents =
        qs.getViewData(VIEW_TASK_DURATIONS, Filter.and(idFilter, durationFilter),
            null, Lists.newArrayList(COL_TASK, COL_DURATION, ProjectConstants.COL_RATE));

    if (taskEvents.isEmpty()) {
      return result;
    }
    Double defaultRate = prm.getDouble(ProjectConstants.PRM_PROJECT_COMMON_RATE);

    Map<Long, Long> times = new HashMap<>();
    Map<Long, Double> expenses = new HashMap<>();

    int idxEventDuration =
        DataUtils.getColumnIndex(COL_DURATION, taskEvents.getColumns(), false);
    int idxRate =
        DataUtils.getColumnIndex(ProjectConstants.COL_RATE, taskEvents.getColumns(), false);
    int idxId = DataUtils.getColumnIndex(COL_TASK, taskEvents.getColumns(), false);

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

    result.addRow(new String[] {
        Codec.beeSerialize(times),
        Codec.beeSerialize(expenses)
    });

    return result;

  }

  private ResponseObject accessTask(RequestInfo reqInfo) {
    Long taskId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_TASK_ID));
    if (!DataUtils.isId(taskId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_TASK_ID);
    }

    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return ResponseObject.error(reqInfo.getService(), "user id not available");
    }

    IsCondition where = SqlUtils.equals(TBL_TASK_USERS, COL_TASK, taskId, COL_USER, userId);
    if (qs.sqlExists(TBL_TASK_USERS, where)) {
      return registerTaskVisit(taskId, userId, System.currentTimeMillis());
    } else {
      if (!usr.isAdministrator()) {
        logger.warning("task", taskId, "access by unauthorized user", userId);
      }
      return ResponseObject.emptyResponse();
    }
  }

  private void addTaskProperties(BeeRow row, List<BeeColumn> columns, Collection<Long> taskUsers,
      Long eventId, Collection<String> propNames, boolean addRelations) {
    long taskId = row.getId();

    if (propNames.contains(PROP_OBSERVERS) && !BeeUtils.isEmpty(taskUsers)) {
      Long owner = row.getLong(DataUtils.getColumnIndex(COL_OWNER, columns));
      Long executor = row.getLong(DataUtils.getColumnIndex(COL_EXECUTOR, columns));

      List<Long> observers = new ArrayList<>();
      for (Long user : taskUsers) {
        if (!Objects.equals(user, owner) && !Objects.equals(user, executor)) {
          observers.add(user);
        }
      }

      if (!observers.isEmpty()) {
        row.setProperty(PROP_OBSERVERS, DataUtils.buildIdList(observers));
      }
    }

    if (addRelations) {
      Multimap<String, Long> taskRelations = getRelations(COL_TASK, taskId);
      for (String property : taskRelations.keySet()) {
        row.setProperty(property, DataUtils.buildIdList(taskRelations.get(property)));
      }
    }

    if (propNames.contains(PROP_FILES)) {
      List<FileInfo> files = getTaskFiles(taskId);
      if (!files.isEmpty()) {
        row.setProperty(PROP_FILES, Codec.beeSerialize(files));
      }
    }

    if (propNames.contains(PROP_EVENTS)) {
      BeeRowSet events = qs.getViewData(VIEW_TASK_EVENTS, Filter.equals(COL_TASK, taskId));
      if (!DataUtils.isEmpty(events)) {
        row.setProperty(PROP_EVENTS, events.serialize());
      }
    }

    if (eventId != null) {
      row.setProperty(PROP_LAST_EVENT_ID, BeeUtils.toString(eventId));
    }
  }

  @Schedule(hour = "5", persistent = false)
  private void checkTaskStatus() {
    if (!Config.isInitialized()) {
      return;
    }
    logger.info("check task status timeout");

    int count = maybeUpdateTaskStatus();
    logger.info("check task status updated", count, "tasks");
  }

  private ResponseObject commitTaskData(BeeRowSet data, Collection<Long> oldUsers,
      boolean checkUsers, Set<String> updatedRelations, Long eventId) {

    ResponseObject response;
    BeeRow row = data.getRow(0);

    List<Long> newUsers;
    if (checkUsers) {
      newUsers = TaskUtils.getTaskUsers(row, data.getColumns());
      if (!BeeUtils.sameElements(oldUsers, newUsers)) {
        updateTaskUsers(row.getId(), oldUsers, newUsers);
      }
    } else {
      newUsers = new ArrayList<>(oldUsers);
    }

    if (!BeeUtils.isEmpty(updatedRelations)) {
      updateTaskRelations(row.getId(), updatedRelations, row);
    }

    Set<String> propNames = Sets.newHashSet(PROP_OBSERVERS, PROP_FILES, PROP_EVENTS);
    boolean addRelations = true;

    Map<Integer, String> shadow = row.getShadow();
    if (shadow != null && !shadow.isEmpty()) {
      List<BeeColumn> columns = new ArrayList<>();
      List<String> oldValues = new ArrayList<>();
      List<String> newValues = new ArrayList<>();

      for (Map.Entry<Integer, String> entry : shadow.entrySet()) {
        columns.add(data.getColumn(entry.getKey()));

        oldValues.add(entry.getValue());
        newValues.add(row.getString(entry.getKey()));
      }

      BeeRow updRow = new BeeRow(row.getId(), row.getVersion(), oldValues);
      for (int i = 0; i < columns.size(); i++) {
        updRow.preliminaryUpdate(i, newValues.get(i));
      }

      BeeRowSet updated = new BeeRowSet(data.getViewName(), columns);
      updated.addRow(updRow);

      response = deb.commitRow(updated);
      if (!response.hasErrors() && response.hasResponse(BeeRow.class)) {
        addTaskProperties((BeeRow) response.getResponse(), data.getColumns(), newUsers, eventId,
            propNames, addRelations);
      }

    } else {
      response = getTaskData(row.getId(), eventId, propNames, addRelations);
    }

    return response;
  }

  private ResponseObject copyRecurringTask(RequestInfo reqInfo) {
    Long rtId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_RT_ID));
    if (!DataUtils.isId(rtId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_RT_ID);
    }

    BeeRowSet rowSet = qs.getViewData(VIEW_RECURRING_TASKS, Filter.compareId(rtId));
    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.error(reqInfo.getService(), VIEW_RECURRING_TASKS, rtId,
          "not available");
    }

    BeeRow row = rowSet.getRow(0);

    DataUtils.setValue(rowSet, row, COL_RT_SCHEDULE_FROM,
        Integer.toString(TimeUtils.today().getDays() + 1));
    DataUtils.setValue(rowSet, row, COL_RT_SCHEDULE_UNTIL, null);

    BeeRowSet insert = DataUtils.createRowSetForInsert(rowSet.getViewName(), rowSet.getColumns(),
        row);
    ResponseObject response = deb.commitRow(insert);

    if (!response.hasErrors() && response.hasResponse(BeeRow.class)) {
      long newId = ((BeeRow) response.getResponse()).getId();

      qs.copyData(TBL_RT_EXECUTORS, COL_RTEX_RECURRING_TASK, rtId, newId);
      qs.copyData(TBL_RT_EXECUTOR_GROUPS, COL_RTEXGR_RECURRING_TASK, rtId, newId);
      qs.copyData(TBL_RT_OBSERVERS, COL_RTOB_RECURRING_TASK, rtId, newId);
      qs.copyData(TBL_RT_OBSERVER_GROUPS, COL_RTOBGR_RECURRING_TASK, rtId, newId);

      qs.copyData(TBL_RT_DATES, COL_RTD_RECURRING_TASK, rtId, newId);
      qs.copyData(TBL_RT_FILES, COL_RTD_RECURRING_TASK, rtId, newId);

      qs.copyData(TBL_RELATIONS, COL_RECURRING_TASK, rtId, newId);
    }

    return response;
  }

  private ResponseObject confirmTasks(RequestInfo reqInfo) {
    ResponseObject response = null;

    String taskIdListData = reqInfo.getParameter(VAR_TASK_DATA);
    if (BeeUtils.isEmpty(taskIdListData)) {
      String msg = BeeUtils.joinWords("Task data not received:", SVC_CONFIRM_TASKS);
      logger.warning(msg);
      response = ResponseObject.error(msg);
      return response;
    }

    List<Long> taskIdList = Codec.deserializeIdList(taskIdListData);
    DataInfo info = sys.getDataInfo(VIEW_TASKS);
    response = ResponseObject.emptyResponse();
    long user = BeeUtils.unbox(usr.getCurrentUserId());
    long now = System.currentTimeMillis();

    /* Don't trust client only */
    BeeRowSet oldTaskData = qs.getViewData(VIEW_TASKS, Filter.idIn(taskIdList));
    if (!TaskUtils.canConfirmTasks(info, oldTaskData.getRows(), user, response)) {
      return response;
    }

    String comment = reqInfo.getParameter(VAR_TASK_COMMENT);
    String notes = reqInfo.getParameter(VAR_TASK_NOTES);

    String eventNote;
    if (BeeUtils.isEmpty(notes)) {
      eventNote = null;
    } else {
      eventNote = BeeUtils.buildLines(Codec.beeDeserializeCollection(notes));
    }

    DateTime approved = new DateTime();

    if (reqInfo.hasParameter(VAR_TASK_APPROVED_TIME)) {
      String strTime = reqInfo.getParameter(VAR_TASK_APPROVED_TIME);

      if (!BeeUtils.isEmpty(strTime)) {
        approved = DateTime.restore(strTime);
      }
    }

    for (Long taskId : taskIdList) {
      response = registerTaskEvent(BeeUtils.unbox(taskId), user, TaskEvent.APPROVE, comment,
          eventNote, null, null, now);

      if (response.hasErrors()) {
        logger.severe("Confirmation failed");
        ctx.setRollbackOnly();
        return response;
      }
    }

    SqlUpdate update = new SqlUpdate(TBL_TASKS)
        .addConstant(COL_STATUS, TaskStatus.APPROVED.ordinal())
        .addConstant(COL_APPROVED, approved)
        .setWhere(SqlUtils.inList(TBL_TASKS, sys.getIdName(TBL_TASKS), taskIdList));
    response = qs.updateDataWithResponse(update);

    if (response.hasErrors()) {
      logger.severe("Confirmation failed");
      ctx.setRollbackOnly();
      return response;
    }

    return response;
  }

  private ResponseObject createTaskRelations(long taskId, Map<String, String> properties) {
    int count = 0;
    if (BeeUtils.isEmpty(properties)) {
      return ResponseObject.response(count);
    }

    ResponseObject response = new ResponseObject();
    List<RowChildren> children = new ArrayList<>();

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      String relation = TaskUtils.translateTaskPropertyToRelation(entry.getKey());

      if (BeeUtils.allNotEmpty(relation, entry.getValue())) {
        children.add(RowChildren.create(TBL_RELATIONS, COL_TASK, null,
            relation, entry.getValue()));
      }
    }

    if (!BeeUtils.isEmpty(children)) {
      count = deb.commitChildren(taskId, children, response);
    }

    return response.setResponse(count);
  }

  private ResponseObject createTasks(BeeRowSet data, BeeRow row, long owner) {
    ResponseObject response = null;

    Map<String, String> properties = row.getProperties();

    List<Long> executors = DataUtils.parseIdList(properties.get(PROP_EXECUTORS));
    List<Long> observers = DataUtils.parseIdList(properties.get(PROP_OBSERVERS));

    Set<Long> executorMembers = getUserGroupMembers(properties.get(PROP_EXECUTOR_GROUPS));
    if (!executorMembers.isEmpty()) {
      for (Long member : executorMembers) {
        if (!executors.contains(member) && !observers.contains(member)) {
          executors.add(member);
        }
      }
    }

    Set<Long> observerMembers = getUserGroupMembers(properties.get(PROP_OBSERVER_GROUPS));
    if (!observerMembers.isEmpty()) {
      for (Long member : observerMembers) {
        if (!observers.contains(member)) {
          observers.add(member);
        }
      }
    }

    DateTime start = row.getDateTime(data.getColumnIndex(COL_START_TIME));

    List<Long> tasks = new ArrayList<>();

    for (long executor : executors) {
      BeeRow newRow = DataUtils.cloneRow(row);
      newRow.setValue(data.getColumnIndex(COL_EXECUTOR), executor);

      TaskStatus status;
      if (TaskUtils.isScheduled(start)) {
        status = TaskStatus.SCHEDULED;
      } else {
        status = (executor == owner) ? TaskStatus.ACTIVE : TaskStatus.NOT_VISITED;
      }
      newRow.setValue(data.getColumnIndex(COL_STATUS), status.ordinal());

      BeeRowSet rowSet = new BeeRowSet(VIEW_TASKS, data.getColumns());
      rowSet.addRow(newRow);

      response = deb.commitRow(rowSet, RowInfo.class);
      if (response.hasErrors()) {
        break;
      }

      long taskId = ((RowInfo) response.getResponse()).getId();

      response = registerTaskEvent(taskId, owner, TaskEvent.CREATE, System.currentTimeMillis());
      if (!response.hasErrors()) {
        createTaskUser(taskId, owner, System.currentTimeMillis());
      }

      if (!response.hasErrors() && executor != owner) {
        response = createTaskUser(taskId, executor, null);
      }

      if (!response.hasErrors()) {
        for (long obsId : observers) {
          if (obsId != owner && obsId != executor) {
            response = createTaskUser(taskId, obsId, null);
            if (response.hasErrors()) {
              break;
            }
          }
        }
      }

      if (!response.hasErrors()) {
        response = createTaskRelations(taskId, properties);
      }
      if (!response.hasErrors()) {
        tasks.add(taskId);
      }

      if (response.hasErrors()) {
        break;
      }
    }

    if (response == null) {
      response = ResponseObject.emptyResponse();
    } else if (!response.hasErrors() && !tasks.isEmpty()) {
      response = ResponseObject.response(DataUtils.buildIdList(tasks));
    }

    return response;
  }

  private ResponseObject createTaskUser(long taskId, long userId, Long millis) {
    SqlInsert si = new SqlInsert(TBL_TASK_USERS)
        .addConstant(COL_TASK, taskId)
        .addConstant(COL_USER, userId);

    if (millis != null) {
      si.addConstant(COL_LAST_ACCESS, millis);
    }

    return qs.insertDataWithResponse(si);
  }

  private ResponseObject doTaskEvent(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    TaskEvent event = EnumUtils.getEnumByName(TaskEvent.class, svc);
    if (event == null) {
      String msg = BeeUtils.joinWords("Task service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
      return response;
    }

    String dataParam = reqInfo.getParameter(VAR_TASK_DATA);
    if (BeeUtils.isEmpty(dataParam)) {
      String msg = BeeUtils.joinWords("Task data not received:", svc, event);
      logger.warning(msg);
      response = ResponseObject.error(msg);
      return response;
    }

    BeeRowSet taskData = BeeRowSet.restore(dataParam);
    BeeRow taskRow = taskData.getRow(0);

    long taskId = taskRow.getId();

    long currentUser = usr.getCurrentUserId();
    long now = System.currentTimeMillis();

    Long eventId = null;
    String eventNote;

    String notes = reqInfo.getParameter(VAR_TASK_NOTES);
    if (BeeUtils.isEmpty(notes)) {
      eventNote = null;
    } else {
      eventNote = BeeUtils.buildLines(Codec.beeDeserializeCollection(notes));
    }

    Set<Long> oldUsers = DataUtils.parseIdSet(reqInfo.getParameter(VAR_TASK_USERS));
    Set<String> updatedRelations = NameUtils.toSet(reqInfo.getParameter(VAR_TASK_RELATIONS));

    switch (event) {
      case CREATE:
        response = createTasks(taskData, taskRow, currentUser);

        if (!response.hasErrors() && response.hasResponse(String.class)) {
          Set<Long> createdTasks = DataUtils.parseIdSet(response.getResponseAsString());
          Long senderAccountId = mail.getSenderAccountId("create task:");

          if (senderAccountId != null) {
            boolean pref = BeeConst.isTrue(taskRow.getProperty(PROP_MAIL));

            for (Long id : createdTasks) {
              ResponseObject mailResponse = mailNewTask(senderAccountId, id, pref, false);
              response.addMessagesFrom(mailResponse);
            }
          }

          response.setResponse(qs.getViewData(VIEW_TASKS, Filter.idIn(createdTasks)));
        }
        break;

      case VISIT:
        if (reqInfo.hasParameter(VAR_TASK_VISITED)) {
          response = registerTaskEvent(taskId, currentUser, event, now);
        }
        if (response == null || !response.hasErrors()) {
          response = commitTaskData(taskData, oldUsers, false, updatedRelations, eventId);
        }
        break;

      case FORWARD:
      case EXTEND:
      case EDIT:
      case COMMENT:
      case SUSPEND:
      case CANCEL:
      case COMPLETE:
      case APPROVE:
      case RENEW:
      case ACTIVATE:
        Long finishTime = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_TASK_FINISH_TIME));

        response = registerTaskEvent(taskId, currentUser, event, reqInfo, eventNote, finishTime,
            now);
        if (response.hasResponse(Long.class)) {
          eventId = (Long) response.getResponse();
        }
        if (!response.hasErrors()) {
          response = registerTaskVisit(taskId, currentUser, now);
        }

        if (!response.hasErrors()) {
          response = commitTaskData(taskData, oldUsers, true, updatedRelations, eventId);
        }

        if (!response.hasErrors() && event == TaskEvent.FORWARD
            && !Objects.equals(currentUser, DataUtils.getLong(taskData, taskRow, COL_EXECUTOR))) {
          Long senderAccountId = mail.getSenderAccountId("forward task:");

          if (senderAccountId != null) {
            ResponseObject mailResponse = mailNewTask(senderAccountId, taskId, true, false);
            response.addMessagesFrom(mailResponse);
          }
        }
        break;
    }

    if (response.hasErrors()) {
      ctx.setRollbackOnly();
    }
    return response;
  }

  private ResponseObject extendTask(RequestInfo reqInfo) {
    Long taskId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_TASK_ID));
    if (!DataUtils.isId(taskId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_TASK_ID);
    }

    Long startMillis = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_START_TIME));
    Long endMillis = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_FINISH_TIME));

    if (!BeeUtils.isPositive(startMillis) && !BeeUtils.isPositive(endMillis)) {
      return ResponseObject.error(reqInfo.getService(), COL_START_TIME, "or", COL_FINISH_TIME,
          "not specified");
    }

    String eventNote;
    String notes = reqInfo.getParameter(VAR_TASK_NOTES);
    if (BeeUtils.isEmpty(notes)) {
      eventNote = null;
    } else {
      eventNote = BeeUtils.buildLines(Codec.beeDeserializeCollection(notes));
    }

    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return ResponseObject.error(reqInfo.getService(), "user id not available");
    }

    BeeRowSet data = qs.getViewData(VIEW_TASKS, Filter.compareId(taskId));
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.error(reqInfo.getService(), "task", taskId, "not found");
    }

    BeeRow row = data.getRow(0);

    if (BeeUtils.isPositive(startMillis)) {
      row.preliminaryUpdate(data.getColumnIndex(COL_START_TIME), startMillis.toString());
    }

    Long oldEnd = null;
    if (BeeUtils.isPositive(endMillis)) {
      int index = data.getColumnIndex(COL_FINISH_TIME);
      Long value = row.getLong(index);

      if (value != null && !value.equals(endMillis)) {
        oldEnd = value;
      }

      row.preliminaryUpdate(index, endMillis.toString());
    }

    long now = System.currentTimeMillis();
    Long eventId = null;

    ResponseObject response = registerTaskEvent(taskId, userId, TaskEvent.EXTEND, reqInfo,
        eventNote, oldEnd, now);
    if (response.hasResponse(Long.class)) {
      eventId = (Long) response.getResponse();
    }
    if (!response.hasErrors()) {
      response = registerTaskVisit(taskId, userId, now);
    }

    if (!response.hasErrors()) {
      Set<Long> users = Collections.emptySet();
      Set<String> relations = Collections.emptySet();

      response = commitTaskData(data, users, false, relations, eventId);
    }

    return response;
  }

  private ResponseObject getChangedTasks() {
    IsCondition uwh = SqlUtils.equals(TBL_TASK_USERS, COL_USER, usr.getCurrentUserId());

    SqlSelect tNewQuery = new SqlSelect().setDistinctMode(true)
        .addFrom(TBL_TASK_USERS)
        .addFields(TBL_TASK_USERS, COL_TASK)
        .setWhere(SqlUtils.and(uwh, SqlUtils.isNull(TBL_TASK_USERS, COL_LAST_ACCESS)));

    Long[] newTasks = qs.getLongColumn(tNewQuery);
    logger.debug("new tasks", newTasks);

    String idName = sys.getIdName(TBL_TASKS);

    SqlSelect inner = new SqlSelect()
        .addFrom(TBL_TASKS)
        .addFromInner(TBL_TASK_USERS, sys.joinTables(TBL_TASKS, TBL_TASK_USERS, COL_TASK))
        .addFromInner(TBL_TASK_EVENTS, sys.joinTables(TBL_TASKS, TBL_TASK_EVENTS, COL_TASK))
        .addFields(TBL_TASKS, idName)
        .addMax(TBL_TASK_USERS, COL_LAST_ACCESS)
        .addMax(TBL_TASK_EVENTS, COL_PUBLISH_TIME)
        .addGroup(TBL_TASKS, idName)
        .setWhere(SqlUtils.and(uwh, SqlUtils.notNull(TBL_TASK_USERS, COL_LAST_ACCESS)));

    String alias = "tupd_" + SqlUtils.uniqueName();

    SqlSelect tUpdQuery = new SqlSelect().setDistinctMode(true)
        .addFrom(inner, alias)
        .addFields(alias, idName)
        .setWhere(SqlUtils.more(alias, COL_PUBLISH_TIME, SqlUtils.field(alias, COL_LAST_ACCESS)));

    Long[] updTasks = qs.getLongColumn(tUpdQuery);
    logger.debug("upd tasks", updTasks);

    List<Long> result = new ArrayList<>();

    long cntNew = (newTasks == null) ? 0 : newTasks.length;
    result.add(cntNew);
    if (cntNew > 0) {
      result.addAll(Lists.newArrayList(newTasks));
    }

    if (updTasks != null && updTasks.length > 0) {
      result.addAll(Lists.newArrayList(updTasks));
    }

    return ResponseObject.response(Joiner.on(BeeConst.CHAR_COMMA).join(result));
  }

  private ResponseObject getCompanyTimesReport(RequestInfo reqInfo) {
    LocalizableConstants constants = usr.getLocalizableConstants();
    boolean hideZeroTimes = false;

    SqlSelect companiesListQuery =
        new SqlSelect()
            .addFields(TBL_COMPANIES, sys.getIdName(TBL_COMPANIES))
            .addFields(TBL_COMPANIES, COL_COMPANY_NAME)
            .addField(TBL_COMPANY_TYPES, COL_COMPANY_TYPE_NAME, ALS_COMPANY_TYPE)
            .addFrom(TBL_COMPANIES)
            .addFromLeft(TBL_COMPANY_TYPES,
                sys.joinTables(TBL_COMPANY_TYPES, TBL_COMPANIES, COL_COMPANY_TYPE))
            .setWhere(SqlUtils.sqlTrue())
            .addOrder(TBL_COMPANIES, COL_COMPANY_NAME);

    if (reqInfo.hasParameter(VAR_TASK_COMPANY)) {
      companiesListQuery.setWhere(SqlUtils.and(companiesListQuery.getWhere(), SqlUtils.inList(
          TBL_COMPANIES, sys.getIdName(TBL_COMPANIES), DataUtils
              .parseIdList(reqInfo.getParameter(VAR_TASK_COMPANY)))));
    }

    if (reqInfo.hasParameter(VAR_TASK_DURATION_HIDE_ZEROS)) {
      hideZeroTimes = true;
    }

    SimpleRowSet companiesListSet = qs.getData(companiesListQuery);
    SimpleRowSet result = new SimpleRowSet(new String[] {COL_COMPANY_NAME, COL_DURATION});
    long totalTimeMls = 0;

    result.addRow(new String[] {constants.client(), constants.crmSpentTime()});

    /* Register times in tasks without company */
    companiesListSet.addRow(new String[] {null, "—", null});

    for (int i = 0; i < companiesListSet.getNumberOfRows(); i++) {
      String compFullName =
          companiesListSet.getValue(i, COL_COMPANY_NAME)
              + (!BeeUtils.isEmpty(companiesListSet.getValue(i, ALS_COMPANY_TYPE))
                  ? ", " + companiesListSet.getValue(i, ALS_COMPANY_TYPE) : "");
      String dTime = "0:00";

      SqlSelect companyTimesQuery = new SqlSelect()
          .addFields(TBL_EVENT_DURATIONS, COL_DURATION)
          .addFrom(TBL_TASK_EVENTS)
          .addFromRight(TBL_EVENT_DURATIONS,
              sys.joinTables(TBL_EVENT_DURATIONS, TBL_TASK_EVENTS, COL_EVENT_DURATION))
          .addFromLeft(TBL_DURATION_TYPES,
              sys.joinTables(TBL_DURATION_TYPES, TBL_EVENT_DURATIONS, COL_DURATION_TYPE))
          .addFromLeft(TBL_TASKS,
              sys.joinTables(TBL_TASKS, TBL_TASK_EVENTS, COL_TASK))
          .addFromLeft(TBL_COMPANIES,
              sys.joinTables(TBL_COMPANIES, TBL_TASKS, COL_COMPANY))
          .addFromLeft(TBL_USERS,
              sys.joinTables(TBL_USERS, TBL_TASK_EVENTS, COL_PUBLISHER))
          .setWhere(
              SqlUtils.equals(TBL_COMPANIES, sys
                  .getIdName(TBL_COMPANIES), companiesListSet.getValue(i, sys
                  .getIdName(TBL_COMPANIES))));

      if (reqInfo.hasParameter(VAR_TASK_DURATION_DATE_FROM)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_DATE_FROM))) {
          companyTimesQuery.setWhere(SqlUtils.and(companyTimesQuery.getWhere(), SqlUtils
              .moreEqual(TBL_EVENT_DURATIONS, COL_DURATION_DATE, reqInfo
                  .getParameter(VAR_TASK_DURATION_DATE_FROM))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_DURATION_DATE_TO)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_DATE_TO))) {
          companyTimesQuery.setWhere(SqlUtils.and(companyTimesQuery.getWhere(), SqlUtils
              .lessEqual(TBL_EVENT_DURATIONS, COL_DURATION_DATE, reqInfo
                  .getParameter(VAR_TASK_DURATION_DATE_TO))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_PUBLISHER)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_PUBLISHER))) {
          companyTimesQuery.setWhere(SqlUtils.and(companyTimesQuery.getWhere(), SqlUtils.inList(
              TBL_USERS, sys.getIdName(TBL_USERS), DataUtils
                  .parseIdList(reqInfo.getParameter(VAR_TASK_PUBLISHER)))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_DURATION_TYPE)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_TYPE))) {
          companyTimesQuery.setWhere(SqlUtils.and(companyTimesQuery.getWhere(), SqlUtils.inList(
              TBL_DURATION_TYPES, sys.getIdName(TBL_DURATION_TYPES), DataUtils
                  .parseIdList(reqInfo.getParameter(VAR_TASK_DURATION_TYPE)))));
        }
      }

      SimpleRowSet companyTimes = qs.getData(companyTimesQuery);
      long dTimeMls = TimeUtils.parseTime(dTime);

      for (int j = 0; j < companyTimes.getNumberOfRows(); j++) {
        Long timeMls = TimeUtils.parseTime(companyTimes.getValue(j, companyTimes
            .getColumnIndex(COL_DURATION)));
        dTimeMls += timeMls;
      }

      totalTimeMls += dTimeMls;

      if (!(hideZeroTimes && dTimeMls <= 0)) {
        dTime = new DateTime(dTimeMls).toUtcTimeString();
        result.addRow(new String[] {compFullName, dTime});
      }
    }

    result.addRow(new String[] {constants.totalOf() + ":",
        new DateTime(totalTimeMls).toUtcTimeString()});

    ResponseObject resp = ResponseObject.response(result);
    return resp;
  }

  private Set<Long> getRecurringTaskExecutors(long rtId) {
    Set<Long> result = new HashSet<>();

    Long[] users = qs.getLongColumn(new SqlSelect()
        .addFields(TBL_RT_EXECUTORS, COL_RTEX_USER)
        .addFrom(TBL_RT_EXECUTORS)
        .setWhere(SqlUtils.equals(TBL_RT_EXECUTORS, COL_RTEX_RECURRING_TASK, rtId)));

    if (users != null) {
      for (Long user : users) {
        result.add(user);
      }
    }

    users = qs.getLongColumn(new SqlSelect()
        .setDistinctMode(true)
        .addFields(TBL_USER_GROUPS, COL_UG_USER)
        .addFrom(TBL_RT_EXECUTOR_GROUPS)
        .addFromInner(TBL_USER_GROUPS,
            SqlUtils.join(TBL_RT_EXECUTOR_GROUPS, COL_RTEXGR_GROUP,
                TBL_USER_GROUPS, COL_UG_GROUP))
        .setWhere(SqlUtils.equals(TBL_RT_EXECUTOR_GROUPS, COL_RTEXGR_RECURRING_TASK, rtId)));

    if (users != null) {
      for (Long user : users) {
        result.add(user);
      }
    }

    return result;
  }

  private SimpleRowSet getRecurringTaskFileData(long rtId) {
    return qs.getData(new SqlSelect()
        .addFields(TBL_RT_FILES, COL_FILE, COL_FILE_CAPTION)
        .addFrom(TBL_RT_FILES)
        .setWhere(SqlUtils.equals(TBL_RT_FILES, COL_RTF_RECURRING_TASK, rtId)));
  }

  private Set<Long> getRecurringTaskObservers(long rtId) {
    Set<Long> result = new HashSet<>();

    Long[] users = qs.getLongColumn(new SqlSelect()
        .addFields(TBL_RT_OBSERVERS, COL_RTOB_USER)
        .addFrom(TBL_RT_OBSERVERS)
        .setWhere(SqlUtils.equals(TBL_RT_OBSERVERS, COL_RTOB_RECURRING_TASK, rtId)));

    if (users != null) {
      for (Long user : users) {
        result.add(user);
      }
    }

    users = qs.getLongColumn(new SqlSelect()
        .setDistinctMode(true)
        .addFields(TBL_USER_GROUPS, COL_UG_USER)
        .addFrom(TBL_RT_OBSERVER_GROUPS)
        .addFromInner(TBL_USER_GROUPS,
            SqlUtils.join(TBL_RT_OBSERVER_GROUPS, COL_RTOBGR_GROUP,
                TBL_USER_GROUPS, COL_UG_GROUP))
        .setWhere(SqlUtils.equals(TBL_RT_OBSERVER_GROUPS, COL_RTOBGR_RECURRING_TASK, rtId)));

    if (users != null) {
      for (Long user : users) {
        result.add(user);
      }
    }

    return result;
  }

  private Multimap<String, Long> getRelations(String filterColumn, long filterValue) {
    Multimap<String, Long> res = HashMultimap.create();

    for (String relation : TaskUtils.getRelations()) {
      Long[] ids = qs.getRelatedValues(TBL_RELATIONS, filterColumn, filterValue,
          relation);

      if (ids != null && ids.length > 0) {
        String property = TaskUtils.translateRelationToTaskProperty(relation);

        for (Long id : ids) {
          res.put(property, id);
        }
      }
    }
    return res;
  }

  private ResponseObject getRequestFiles(Long requestId) {
    Assert.state(DataUtils.isId(requestId));

    SimpleRowSet data =
        qs.getData(new SqlSelect()
            .addFields(TBL_REQUEST_FILES, COL_FILE, COL_CAPTION)
            .addFields(TBL_FILES, COL_FILE_NAME,
                COL_FILE_SIZE, COL_FILE_TYPE)
            .addFrom(TBL_REQUEST_FILES)
            .addFromInner(TBL_FILES,
                sys.joinTables(TBL_FILES, TBL_REQUEST_FILES, COL_FILE))
            .setWhere(SqlUtils.equals(TBL_REQUEST_FILES, COL_REQUEST, requestId)));

    List<FileInfo> files = new ArrayList<>();

    for (SimpleRow file : data) {
      FileInfo sf = new FileInfo(file.getLong(COL_FILE),
          BeeUtils.notEmpty(file.getValue(COL_CAPTION),
              file.getValue(COL_FILE_NAME)),
          file.getLong(COL_FILE_SIZE),
          file.getValue(COL_FILE_TYPE));

      sf.setIcon(ExtensionIcons.getIcon(sf.getName()));
      files.add(sf);
    }
    return ResponseObject.response(files);
  }

  private ResponseObject getSchedulingData(RequestInfo reqInfo) {
    Long rtId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_RT_ID));
    if (!DataUtils.isId(rtId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_RT_ID);
    }

    Map<String, String> data = new HashMap<>();

    Set<Long> executors = getRecurringTaskExecutors(rtId);
    if (!executors.isEmpty()) {
      BeeRowSet users = qs.getViewData(VIEW_USERS, Filter.idIn(executors));
      if (!DataUtils.isEmpty(users)) {
        data.put(users.getViewName(), users.serialize());
      }
    }

    BeeRowSet rtDates = qs.getViewData(VIEW_RT_DATES, Filter.equals(COL_RTD_RECURRING_TASK, rtId));
    if (!DataUtils.isEmpty(rtDates)) {
      data.put(rtDates.getViewName(), rtDates.serialize());
    }

    BeeRowSet tasks = qs.getViewData(VIEW_TASKS, Filter.equals(COL_RECURRING_TASK, rtId),
        Order.ascending(ALS_EXECUTOR_LAST_NAME, ALS_EXECUTOR_FIRST_NAME));
    if (!DataUtils.isEmpty(tasks)) {
      data.put(tasks.getViewName(), tasks.serialize());
    }

    if (data.isEmpty()) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(data);
    }
  }

  private Set<JustDate> getSpawnedDates(long rtId, DateRange range) {
    Set<JustDate> dates = new HashSet<>();

    DateTime[] startTimes = qs.getDateTimeColumn(new SqlSelect().setDistinctMode(true)
        .addFields(TBL_TASKS, COL_START_TIME)
        .addFrom(TBL_TASKS)
        .setWhere(SqlUtils.equals(TBL_TASKS, COL_RECURRING_TASK, rtId)));

    if (startTimes != null && startTimes.length > 0) {
      for (DateTime startTime : startTimes) {
        JustDate date = JustDate.get(startTime);
        if (date == null) {
          continue;
        }

        if (range != null && !range.contains(date)) {
          continue;
        }

        dates.add(date);
      }
    }

    return dates;
  }

  // private SimpleRowSet getTaskActualTimesAndExpenses(List<Long> ids) {
  // SimpleRowSet result = new SimpleRowSet(new String[] {
  // COL_TASK, COL_ACTUAL_DURATION, COL_ACTUAL_EXPENSES
  // });
  //
  // SimpleRowSet data = getTaskEventTimeAndExpensesData(ids);
  //
  // if (data.isEmpty()) {
  // return result;
  // }
  //
  // Map<String, String> times = Codec.deserializeMap(data.getValue(0, COL_ACTUAL_DURATION));
  // Map<String, String> expenses = Codec.deserializeMap(data.getValue(0, COL_ACTUAL_EXPENSES));
  //
  // for (Long id : ids) {
  // Long time = 0L;
  // double expense = 0.0;
  //
  // if (times.containsKey(BeeUtils.toString(id))) {
  // time = BeeUtils.toLong(times.get(BeeUtils.toString(id)));
  //
  // }
  //
  // if (expenses.containsKey(BeeUtils.toString(id))) {
  // expense = BeeUtils.toDouble(expenses.get(BeeUtils.toString(id)));
  // }
  //
  // result.addRow(new String[] {
  // BeeUtils.toString(id),
  // BeeUtils.toString(time),
  // BeeUtils.toString(expense)
  // });
  // }
  //
  // return result;
  // }

  private ResponseObject getTaskData(long taskId, Long eventId, Collection<String> propNames,
      boolean addRelations) {

    BeeRowSet rowSet = qs.getViewData(VIEW_TASKS, Filter.compareId(taskId));
    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.error(SVC_GET_TASK_DATA, "task id: " + taskId + " not found");
    }

    BeeRow data = rowSet.getRow(0);
    addTaskProperties(data, rowSet.getColumns(), getTaskUsers(taskId), eventId, propNames,
        addRelations);

    return ResponseObject.response(data);
  }

  private ResponseObject getTaskData(RequestInfo reqInfo) {
    long taskId = BeeUtils.toLong(reqInfo.getParameter(VAR_TASK_ID));
    if (!DataUtils.isId(taskId)) {
      String msg = BeeUtils.joinWords(reqInfo.getService(), "task id not received");
      logger.warning(msg);
      return ResponseObject.error(msg);
    }

    Set<String> propNames = new HashSet<>();

    String propList = reqInfo.getParameter(VAR_TASK_PROPERTIES);
    if (!BeeUtils.isEmpty(propList)) {
      propNames.addAll(NameUtils.toSet(propList));
    }

    boolean addRelations = reqInfo.hasParameter(VAR_TASK_RELATIONS);

    return getTaskData(taskId, null, propNames, addRelations);
  }

  private List<FileInfo> getTaskFiles(long taskId) {
    List<FileInfo> result = new ArrayList<>();

    BeeRowSet rowSet = qs.getViewData(VIEW_TASK_FILES, Filter.equals(COL_TASK, taskId));
    if (rowSet == null || rowSet.isEmpty()) {
      return result;
    }

    for (BeeRow row : rowSet.getRows()) {
      FileInfo sf = new FileInfo(DataUtils.getLong(rowSet, row, COL_FILE),
          DataUtils.getString(rowSet, row, ALS_FILE_NAME),
          DataUtils.getLong(rowSet, row, ALS_FILE_SIZE),
          DataUtils.getString(rowSet, row, ALS_FILE_TYPE));

      Long teId = DataUtils.getLong(rowSet, row, COL_TASK_EVENT);
      if (teId != null) {
        sf.setRelatedId(teId);
      }

      String caption = DataUtils.getString(rowSet, row, COL_CAPTION);
      if (!BeeUtils.isEmpty(caption)) {
        sf.setCaption(caption);
      }

      sf.setIcon(ExtensionIcons.getIcon(sf.getName()));
      result.add(sf);
    }

    return result;
  }

  private List<Long> getTaskUsers(long taskId) {
    if (!DataUtils.isId(taskId)) {
      return new ArrayList<>();
    }

    SqlSelect query = new SqlSelect()
        .addFrom(TBL_TASK_USERS)
        .addFields(TBL_TASK_USERS, COL_USER)
        .setWhere(SqlUtils.equals(TBL_TASK_USERS, COL_TASK, taskId))
        .addOrder(TBL_TASK_USERS, sys.getIdName(TBL_TASK_USERS));

    return Lists.newArrayList(qs.getLongColumn(query));
  }

  private ResponseObject getTypeHoursReport(RequestInfo reqInfo) {
    LocalizableConstants constants = usr.getLocalizableConstants();
    SqlSelect durationTypes = new SqlSelect()
        .addFrom(TBL_DURATION_TYPES)
        .addFields(TBL_DURATION_TYPES, COL_DURATION_TYPE_NAME)
        .setWhere(SqlUtils.sqlTrue())
        .addOrder(TBL_DURATION_TYPES, COL_DURATION_TYPE_NAME);

    boolean hideTimeZeros = false;

    if (reqInfo.hasParameter(VAR_TASK_DURATION_TYPE)) {
      durationTypes.setWhere(SqlUtils.and(durationTypes.getWhere(), SqlUtils.inList(
          TBL_DURATION_TYPES, sys.getIdName(TBL_DURATION_TYPES), DataUtils.parseIdList(reqInfo
              .getParameter(VAR_TASK_DURATION_TYPE)))));
    }

    if (reqInfo.hasParameter(VAR_TASK_DURATION_HIDE_ZEROS)) {
      hideTimeZeros = true;
    }

    SimpleRowSet dTypesList = qs.getData(durationTypes);
    SimpleRowSet result = new SimpleRowSet(new String[] {COL_DURATION_TYPE_NAME, COL_DURATION});

    result.addRow(new String[] {constants.crmDurationType(), constants.crmSpentTime()});
    Assert.notNull(dTypesList);

    long totalTimeMls = 0;

    for (int i = 0; i < dTypesList.getNumberOfRows(); i++) {
      String dType = dTypesList.getValue(i, dTypesList.getColumnIndex(COL_DURATION_TYPE_NAME));
      String dTime = "0:00";

      SqlSelect dTypeTime = new SqlSelect()
          .addFrom(TBL_TASK_EVENTS)
          .addFromRight(TBL_EVENT_DURATIONS,
              sys.joinTables(TBL_EVENT_DURATIONS, TBL_TASK_EVENTS, COL_EVENT_DURATION))
          .addFromLeft(TBL_DURATION_TYPES,
              sys.joinTables(TBL_DURATION_TYPES, TBL_EVENT_DURATIONS, COL_DURATION_TYPE))
          .addFromLeft(TBL_TASKS,
              sys.joinTables(TBL_TASKS, TBL_TASK_EVENTS, COL_TASK))
          .addFromLeft(TBL_COMPANIES,
              sys.joinTables(TBL_COMPANIES, TBL_TASKS, COL_COMPANY))
          .addFromLeft(TBL_USERS,
              sys.joinTables(TBL_USERS, TBL_TASK_EVENTS, COL_PUBLISHER))
          .addFields(TBL_DURATION_TYPES, COL_DURATION_TYPE_NAME)
          .addFields(TBL_EVENT_DURATIONS, COL_DURATION)
          .setWhere(SqlUtils.equals(TBL_DURATION_TYPES, COL_DURATION_TYPE_NAME, dType));

      if (reqInfo.hasParameter(VAR_TASK_DURATION_DATE_FROM)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_DATE_FROM))) {
          dTypeTime.setWhere(SqlUtils.and(dTypeTime.getWhere(), SqlUtils
              .moreEqual(TBL_EVENT_DURATIONS, COL_DURATION_DATE, reqInfo
                  .getParameter(VAR_TASK_DURATION_DATE_FROM))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_DURATION_DATE_TO)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_DATE_TO))) {
          dTypeTime.setWhere(SqlUtils.and(dTypeTime.getWhere(), SqlUtils
              .lessEqual(TBL_EVENT_DURATIONS, COL_DURATION_DATE, reqInfo
                  .getParameter(VAR_TASK_DURATION_DATE_TO))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_COMPANY)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_COMPANY))) {
          dTypeTime.setWhere(SqlUtils.and(dTypeTime.getWhere(), SqlUtils.inList(
              TBL_COMPANIES, sys.getIdName(TBL_COMPANIES),
              DataUtils.parseIdList(reqInfo.getParameter(VAR_TASK_COMPANY)))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_PUBLISHER)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_PUBLISHER))) {
          dTypeTime.setWhere(SqlUtils.and(dTypeTime.getWhere(), SqlUtils.inList(
              TBL_USERS, sys.getIdName(TBL_USERS), DataUtils
                  .parseIdList(reqInfo.getParameter(VAR_TASK_PUBLISHER)))));
        }
      }

      SimpleRowSet dTypeTimes = qs.getData(dTypeTime);
      Assert.notNull(dTypeTimes);

      long dTimeMls = TimeUtils.parseTime(dTime);

      for (int j = 0; j < dTypeTimes.getNumberOfRows(); j++) {
        Long timeMls = TimeUtils.parseTime(dTypeTimes.getValue(j, dTypeTimes
            .getColumnIndex(COL_DURATION)));
        dTimeMls += timeMls;
      }

      totalTimeMls += dTimeMls;

      if (!(hideTimeZeros && dTimeMls <= 0)) {
        dTime = new DateTime(dTimeMls).toUtcTimeString();
        result.addRow(new String[] {dType, dTime});
      }
    }

    result.addRow(new String[] {
        constants.totalOf() + ":", new DateTime(totalTimeMls).toUtcTimeString()});

    ResponseObject resp = ResponseObject.response(result);
    return resp;
  }

  private Set<Long> getUserGroupMembers(String groupList) {
    Set<Long> users = new HashSet<>();

    Set<Long> groups = DataUtils.parseIdSet(groupList);
    if (groups.isEmpty()) {
      return users;
    }

    SqlSelect query = new SqlSelect()
        .setDistinctMode(true)
        .addFields(TBL_USER_GROUPS, COL_UG_USER)
        .addFrom(TBL_USER_GROUPS)
        .setWhere(SqlUtils.inList(TBL_USER_GROUPS,
            COL_UG_GROUP, groups));

    Long[] members = qs.getLongColumn(query);
    if (members != null) {
      for (Long member : members) {
        if (member != null) {
          users.add(member);
        }
      }
    }

    return users;
  }

  private ResponseObject getUsersHoursReport(RequestInfo reqInfo) {
    LocalizableConstants constants = usr.getLocalizableConstants();
    SqlSelect userListQuery =
        new SqlSelect()
            .addFields(TBL_USERS, sys.getIdName(TBL_USERS))
            .addFields(TBL_PERSONS,
                COL_FIRST_NAME,
                COL_LAST_NAME)
            .addFrom(TBL_USERS)
            .addFromLeft(
                TBL_COMPANY_PERSONS,
                sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS,
                    COL_COMPANY_PERSON))
            .addFromLeft(
                TBL_PERSONS,
                sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS,
                    COL_PERSON)).setWhere(SqlUtils.sqlTrue())
            .setWhere(SqlUtils.sqlTrue())
            .addOrder(TBL_PERSONS, COL_FIRST_NAME,
                COL_LAST_NAME);

    boolean hideTimeZeros = false;

    if (reqInfo.hasParameter(VAR_TASK_PUBLISHER)) {
      userListQuery.setWhere(SqlUtils.and(userListQuery.getWhere(), SqlUtils.inList(
          TBL_USERS, sys.getIdName(TBL_USERS), DataUtils
              .parseIdList(reqInfo.getParameter(VAR_TASK_PUBLISHER)))));
    }

    if (reqInfo.hasParameter(VAR_TASK_DURATION_HIDE_ZEROS)) {
      hideTimeZeros = true;
    }

    SimpleRowSet usersListSet = qs.getData(userListQuery);
    SimpleRowSet result = new SimpleRowSet(new String[] {COL_USER, COL_DURATION});
    long totalTimeMls = 0;

    result.addRow(new String[] {
        constants.userFullName(), constants.crmSpentTime()});

    for (int i = 0; i < usersListSet.getNumberOfRows(); i++) {
      String userFullName =
          (!BeeUtils.isEmpty(usersListSet.getValue(i, COL_FIRST_NAME))
              ? usersListSet.getValue(i, COL_FIRST_NAME) : "") + " "
              + (!BeeUtils.isEmpty(usersListSet.getValue(i, COL_LAST_NAME))
                  ? usersListSet.getValue(i, COL_LAST_NAME) : "");

      userFullName = BeeUtils.isEmpty(userFullName) ? "—" : userFullName;
      String dTime = "0:00";

      SqlSelect userTimesQuery = new SqlSelect()
          .addFields(TBL_EVENT_DURATIONS, COL_DURATION)
          .addFrom(TBL_TASK_EVENTS)
          .addFromRight(TBL_EVENT_DURATIONS,
              sys.joinTables(TBL_EVENT_DURATIONS, TBL_TASK_EVENTS, COL_EVENT_DURATION))
          .addFromLeft(TBL_DURATION_TYPES,
              sys.joinTables(TBL_DURATION_TYPES, TBL_EVENT_DURATIONS, COL_DURATION_TYPE))
          .addFromLeft(TBL_TASKS,
              sys.joinTables(TBL_TASKS, TBL_TASK_EVENTS, COL_TASK))
          .addFromLeft(TBL_COMPANIES,
              sys.joinTables(TBL_COMPANIES, TBL_TASKS, COL_COMPANY))
          .addFromLeft(TBL_USERS,
              sys.joinTables(TBL_USERS, TBL_TASK_EVENTS, COL_PUBLISHER))
          .setWhere(
              SqlUtils.equals(TBL_USERS, sys
                  .getIdName(TBL_USERS), usersListSet.getValue(i, sys
                  .getIdName(TBL_USERS))));

      if (reqInfo.hasParameter(VAR_TASK_DURATION_DATE_FROM)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_DATE_FROM))) {
          userTimesQuery.setWhere(SqlUtils.and(userTimesQuery.getWhere(), SqlUtils
              .moreEqual(TBL_EVENT_DURATIONS, COL_DURATION_DATE, reqInfo
                  .getParameter(VAR_TASK_DURATION_DATE_FROM))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_DURATION_DATE_TO)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_DATE_TO))) {
          userTimesQuery.setWhere(SqlUtils.and(userTimesQuery.getWhere(), SqlUtils
              .lessEqual(TBL_EVENT_DURATIONS, COL_DURATION_DATE, reqInfo
                  .getParameter(VAR_TASK_DURATION_DATE_TO))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_COMPANY)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_COMPANY))) {
          userTimesQuery.setWhere(SqlUtils.and(userTimesQuery.getWhere(), SqlUtils.inList(
              TBL_COMPANIES, sys.getIdName(TBL_COMPANIES),
              DataUtils.parseIdList(reqInfo.getParameter(VAR_TASK_COMPANY)))));
        }
      }

      if (reqInfo.hasParameter(VAR_TASK_DURATION_TYPE)) {
        if (!BeeUtils.isEmpty(reqInfo.getParameter(VAR_TASK_DURATION_TYPE))) {
          userTimesQuery.setWhere(SqlUtils.and(userTimesQuery.getWhere(), SqlUtils.inList(
              TBL_DURATION_TYPES, sys.getIdName(TBL_DURATION_TYPES), DataUtils
                  .parseIdList(reqInfo.getParameter(VAR_TASK_DURATION_TYPE)))));
        }
      }

      SimpleRowSet companyTimes = qs.getData(userTimesQuery);
      long dTimeMls = TimeUtils.parseTime(dTime);

      for (int j = 0; j < companyTimes.getNumberOfRows(); j++) {
        Long timeMls = TimeUtils.parseTime(companyTimes.getValue(j, companyTimes
            .getColumnIndex(COL_DURATION)));
        dTimeMls += timeMls;
      }

      totalTimeMls += dTimeMls;

      if (!(hideTimeZeros && dTimeMls <= 0)) {
        dTime = new DateTime(dTimeMls).toUtcTimeString();
        result.addRow(new String[] {userFullName, dTime});
      }
    }

    result.addRow(new String[] {
        constants.totalOf() + ":", new DateTime(totalTimeMls).toUtcTimeString()});

    ResponseObject resp = ResponseObject.response(result);
    return resp;
  }

  private ResponseObject mailNewTask(Long senderAccountId, long taskId,
      boolean ownerPreference, boolean automatic) {

    ResponseObject response = ResponseObject.emptyResponse();
    String label = "mail new task";

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TASKS, COL_SUMMARY, COL_DESCRIPTION, COL_OWNER,
            COL_EXECUTOR, COL_START_TIME, COL_FINISH_TIME)
        .addFrom(TBL_TASKS);

    HasConditions where = SqlUtils.and(SqlUtils.equals(TBL_TASKS, COL_TASK_ID, taskId));

    if (!ownerPreference) {
      query.addFromInner(TBL_USER_SETTINGS,
          SqlUtils.join(TBL_USER_SETTINGS, COL_USER, TBL_TASKS, COL_EXECUTOR));
      where.add(SqlUtils.notNull(TBL_USER_SETTINGS, COL_MAIL_ASSIGNED_TASKS));
    }

    if (!automatic) {
      where.add(SqlUtils.notEqual(TBL_TASKS, COL_OWNER, SqlUtils.field(TBL_TASKS, COL_EXECUTOR)));
    }

    query.setWhere(where);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return response;
    }

    SimpleRow row = data.getRow(0);

    Long executor = row.getLong(COL_EXECUTOR);

    String recipientEmail = usr.getUserEmail(executor, false);
    if (BeeUtils.isEmpty(recipientEmail)) {
      logger.warning(label, taskId, "executor", executor, "email not available");
      return response;
    }

    LocalizableConstants constants = usr.getLocalizableConstants(executor);
    if (constants == null) {
      logger.warning(label, taskId, "executor", executor, "localization not available");
      return response;
    }

    Document document = taskToHtml(taskId, row.getDateTime(COL_START_TIME),
        row.getDateTime(COL_FINISH_TIME), row.getValue(COL_SUMMARY),
        row.getValue(COL_DESCRIPTION), row.getLong(COL_OWNER), executor, constants);
    String content = document.buildLines();

    logger.info(label, taskId, "mail to", executor, recipientEmail);

    ResponseObject mailResponse = mail.sendMail(senderAccountId, recipientEmail,
        constants.crmMailTaskSubject(), content);

    if (mailResponse.hasErrors()) {
      response.addWarning("Send mail failed");
    }
    return response;
  }

  private void mailScheduledTasks(Set<Long> tasks) {
    if (BeeUtils.isEmpty(tasks)) {
      return;
    }

    Long senderAccountId = mail.getSenderAccountId("scheduled tasks:");
    if (senderAccountId == null) {
      return;
    }

    HasConditions where = SqlUtils.and(
        SqlUtils.inList(TBL_TASKS, COL_TASK_ID, tasks),
        SqlUtils.or(
            SqlUtils.notNull(TBL_RECURRING_TASKS, COL_RT_COPY_BY_MAIL),
            SqlUtils.notNull(TBL_USER_SETTINGS, COL_MAIL_ASSIGNED_TASKS)));

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TASKS, COL_TASK_ID)
        .addFrom(TBL_TASKS)
        .addFromLeft(TBL_RECURRING_TASKS,
            sys.joinTables(TBL_RECURRING_TASKS, TBL_TASKS, COL_RECURRING_TASK))
        .addFromLeft(TBL_USER_SETTINGS,
            SqlUtils.join(TBL_USER_SETTINGS, COL_USER, TBL_TASKS, COL_EXECUTOR))
        .setWhere(where);

    Long[] ids = qs.getLongColumn(query);
    if (ids == null || ids.length == 0) {
      return;
    }

    for (Long id : ids) {
      ResponseObject response = mailNewTask(senderAccountId, id, true, true);
      if (response.hasErrors() || response.hasWarnings()) {
        logger.warning("mail scheduled tasks canceled");
        break;
      }
    }
  }

  private int maybeUpdateTaskStatus() {
    int updated = 0;

    IsCondition where = SqlUtils.and(
        SqlUtils.equals(TBL_TASKS, COL_STATUS, TaskStatus.SCHEDULED.ordinal()),
        SqlUtils.less(TBL_TASKS, COL_START_TIME, TimeUtils.startOfDay(1)));

    SqlUpdate activate = new SqlUpdate(TBL_TASKS)
        .addConstant(COL_STATUS, TaskStatus.ACTIVE.ordinal())
        .setWhere(SqlUtils.and(where, SqlUtils.equals(TBL_TASKS, COL_EXECUTOR,
            SqlUtils.field(TBL_TASKS, COL_OWNER))));

    int activated = qs.updateData(activate);
    if (activated > 0) {
      updated += activated;
    }

    SqlUpdate pending = new SqlUpdate(TBL_TASKS)
        .addConstant(COL_STATUS, TaskStatus.NOT_VISITED.ordinal())
        .setWhere(SqlUtils.and(where, SqlUtils.notEqual(TBL_TASKS, COL_EXECUTOR,
            SqlUtils.field(TBL_TASKS, COL_OWNER))));

    int notVisited = qs.updateData(pending);
    if (notVisited > 0) {
      updated += notVisited;
    }

    return updated;
  }

  @Schedule(hour = "4", persistent = false)
  private void recurringTaskSchedulingTimeout() {
    if (!Config.isInitialized()) {
      return;
    }
    logger.info("recurring task scheduling timeout ");

    Set<Long> tasks = scheduleRecurringTasks(DateRange.day(TimeUtils.today()));
    logger.info("recurring task scheduler created", tasks.size(), "tasks");

    if (!tasks.isEmpty()) {
      mailScheduledTasks(tasks);
    }
  }

  private ResponseObject registerTaskDuration(long durationType, RequestInfo reqInfo) {
    Long date = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_TASK_DURATION_DATE));
    if (date == null) {
      return ResponseObject.error("task duration date not specified");
    }

    String time = reqInfo.getParameter(VAR_TASK_DURATION_TIME);
    if (BeeUtils.isEmpty(time)) {
      return ResponseObject.error("task duration time not specified");
    }

    return qs.insertDataWithResponse(new SqlInsert(TBL_EVENT_DURATIONS)
        .addConstant(COL_DURATION_TYPE, durationType)
        .addConstant(COL_DURATION_DATE, date)
        .addConstant(COL_DURATION, time));
  }

  private ResponseObject registerTaskEvent(long taskId, long userId, TaskEvent event, long millis) {
    return registerTaskEvent(taskId, userId, event, null, null, null, null, millis);
  }

  private ResponseObject registerTaskEvent(long taskId, long userId, TaskEvent event,
      RequestInfo reqInfo, String note, Long finishTime, long millis) {
    String comment = reqInfo.getParameter(VAR_TASK_COMMENT);

    Long durationId = null;
    Long durationType = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_TASK_DURATION_TYPE));

    if (DataUtils.isId(durationType)) {
      ResponseObject response = registerTaskDuration(durationType, reqInfo);
      if (response.hasErrors()) {
        return response;
      } else if (response.hasResponse(Long.class)) {
        durationId = (Long) response.getResponse();
      }
    }

    return registerTaskEvent(taskId, userId, event, comment, note, finishTime, durationId, millis);
  }

  private ResponseObject registerTaskEvent(long taskId, long userId, TaskEvent event,
      String comment, String note, Long finishTime, Long durationId, long millis) {

    SqlInsert si = new SqlInsert(TBL_TASK_EVENTS)
        .addConstant(COL_TASK, taskId)
        .addConstant(COL_PUBLISHER, userId)
        .addConstant(COL_PUBLISH_TIME, millis)
        .addConstant(TaskConstants.COL_EVENT, event.ordinal());

    if (!BeeUtils.isEmpty(comment)) {
      si.addConstant(COL_COMMENT, comment);
    }
    if (!BeeUtils.isEmpty(note)) {
      si.addConstant(COL_EVENT_NOTE, note);
    }

    if (BeeUtils.isPositive(finishTime)) {
      si.addConstant(COL_FINISH_TIME, finishTime);
    }

    if (DataUtils.isId(durationId)) {
      si.addConstant(COL_EVENT_DURATION, durationId);
    }

    return qs.insertDataWithResponse(si);
  }

  private ResponseObject registerTaskVisit(long taskId, long userId, long millis) {
    IsCondition where = SqlUtils.equals(TBL_TASK_USERS, COL_TASK, taskId, COL_USER, userId);

    return qs.updateDataWithResponse(new SqlUpdate(TBL_TASK_USERS)
        .addConstant(COL_LAST_ACCESS, millis)
        .setWhere(where));
  }

  private Set<Long> scheduleRecurringTasks(DateRange defRange) {
    Set<Long> result = new HashSet<>();

    String label = "scheduling tasks";

    JustDate defStart = defRange.getMinDate();
    JustDate defEnd = defRange.getMaxDate();

    Filter filter = Filter.and(
        Filter.or(Filter.isNull(COL_RT_SCHEDULE_DAYS),
            Filter.isPositive(COL_RT_SCHEDULE_DAYS)),
        Filter.or(Filter.isNull(COL_RT_SCHEDULE_UNTIL),
            Filter.isMoreEqual(COL_RT_SCHEDULE_UNTIL, new DateValue(defStart))));

    BeeRowSet rtData = qs.getViewData(VIEW_RECURRING_TASKS, filter);
    if (DataUtils.isEmpty(rtData)) {
      logger.info(label, defRange, "no active recurring tasks found");
      return result;
    }

    List<BeeColumn> taskColumns = sys.getView(VIEW_TASKS).getRowSetColumns();
    if (BeeUtils.isEmpty(taskColumns)) {
      logger.severe(label, defRange, "task columns not available");
      return result;
    }

    for (BeeRow rtRow : rtData.getRows()) {
      long rtId = rtRow.getId();

      Set<Long> executors = getRecurringTaskExecutors(rtId);
      if (executors.isEmpty()) {
        logger.debug(label, rtId, "has no executors");
        continue;
      }

      JustDate from = DataUtils.getDate(rtData, rtRow, COL_RT_SCHEDULE_FROM);
      if (from == null) {
        logger.warning(label, rtId, COL_RT_SCHEDULE_FROM, "is null");
        continue;
      }

      JustDate until = DataUtils.getDate(rtData, rtRow, COL_RT_SCHEDULE_UNTIL);
      if (until != null && TimeUtils.isLess(until, from)) {
        logger.warning(label, rtId, "invalid range", COL_RT_SCHEDULE_FROM, from,
            COL_RT_SCHEDULE_UNTIL, until);
        continue;
      }

      Integer days = DataUtils.getInteger(rtData, rtRow, COL_RT_SCHEDULE_DAYS);
      if (BeeUtils.isNegative(days)) {
        logger.debug(label, rtId, COL_RT_SCHEDULE_DAYS, days, "not scheduled");
        continue;
      }

      JustDate min = TimeUtils.max(defStart, from);
      JustDate max;

      if (until == null) {
        max = BeeUtils.isPositive(days) ? TimeUtils.nextDay(defEnd, days) : defEnd;
      } else if (BeeUtils.isPositive(days)) {
        max = TimeUtils.min(TimeUtils.nextDay(defEnd, days), until);
      } else {
        max = TimeUtils.min(defEnd, until);
      }

      if (TimeUtils.isLess(max, min)) {
        logger.debug(label, rtId, "min", min, "max", max, "out of range", defRange);
        continue;
      }

      CronExpression.Builder builder = new CronExpression.Builder(from, until)
          .id(BeeUtils.toString(rtId))
          .dayOfMonth(DataUtils.getString(rtData, rtRow, COL_RT_DAY_OF_MONTH))
          .month(DataUtils.getString(rtData, rtRow, COL_RT_MONTH))
          .dayOfWeek(DataUtils.getString(rtData, rtRow, COL_RT_DAY_OF_WEEK))
          .year(DataUtils.getString(rtData, rtRow, COL_RT_YEAR))
          .workdayTransition(EnumUtils.getEnumByIndex(WorkdayTransition.class,
              DataUtils.getInteger(rtData, rtRow, COL_RT_WORKDAY_TRANSITION)));

      BeeRowSet rtDates = qs.getViewData(VIEW_RT_DATES,
          Filter.equals(COL_RTD_RECURRING_TASK, rtId));

      if (!DataUtils.isEmpty(rtDates)) {
        List<ScheduleDateRange> scheduleDateRanges = TaskUtils.getScheduleDateRanges(rtDates);

        for (ScheduleDateRange sdr : scheduleDateRanges) {
          builder.rangeMode(sdr.getRange(), sdr.getMode());
        }
      }

      CronExpression cron = builder.build();

      List<JustDate> cronDates = cron.getDates(min, max);
      if (cronDates.isEmpty()) {
        logger.debug(label, rtId, "no cron dates in", min, max);
        continue;
      }

      Set<JustDate> spawnedDates = getSpawnedDates(rtId, DateRange.closed(min, max));
      if (!spawnedDates.isEmpty()) {
        cronDates.removeAll(spawnedDates);
        if (cronDates.isEmpty()) {
          logger.debug(label, rtId, "all dates already spawned", spawnedDates);
          continue;
        }
      }

      Set<Long> observers = getRecurringTaskObservers(rtId);

      Multimap<String, Long> relations = getRelations(COL_RECURRING_TASK, rtId);
      SimpleRowSet fileData = getRecurringTaskFileData(rtId);

      for (JustDate date : cronDates) {
        Set<Long> tasks = spawnTasks(rtData.getColumns(), rtRow, date,
            executors, observers, relations, fileData, taskColumns);

        if (BeeUtils.isEmpty(tasks)) {
          logger.severe(label, rtId, date, "no tasks created");
          return result;
        }

        result.addAll(tasks);
        logger.info("recurring task", rtId, "scheduled", date, tasks);
      }
    }

    return result;
  }

  private ResponseObject scheduleTasks(RequestInfo reqInfo) {
    Integer from = BeeUtils.toIntOrNull(reqInfo.getParameter(COL_RT_SCHEDULE_FROM));
    if (!BeeUtils.isPositive(from)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_RT_SCHEDULE_FROM);
    }

    Integer until = BeeUtils.toIntOrNull(reqInfo.getParameter(COL_RT_SCHEDULE_UNTIL));
    if (BeeUtils.isPositive(until) && from > until) {
      return ResponseObject.error(reqInfo.getService(), "invalid range", from, until);
    }

    DateRange range;
    if (until == null) {
      range = DateRange.day(new JustDate(from));
    } else {
      range = DateRange.closed(new JustDate(from), new JustDate(until));
    }

    Set<Long> tasks = scheduleRecurringTasks(range);
    if (!tasks.isEmpty()) {
      mailScheduledTasks(tasks);
    }

    return ResponseObject.response(tasks.size());
  }

  private int sendTaskReminders(long timeRemaining) {
    int count = 0;
    String label = "task reminders:";

    Long accountId = mail.getSenderAccountId(label);

    if (!DataUtils.isId(accountId)) {
      return count;
    }
    Set<Integer> statusValues = Sets.newHashSet(TaskStatus.NOT_VISITED.ordinal(),
        TaskStatus.ACTIVE.ordinal(), TaskStatus.SCHEDULED.ordinal(),
        TaskStatus.SUSPENDED.ordinal());

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TASKS, COL_TASK_ID, COL_SUMMARY, COL_DESCRIPTION, COL_OWNER,
            COL_EXECUTOR, COL_START_TIME, COL_FINISH_TIME, COL_REMINDER_TIME, COL_REMINDER_SENT)
        .addFields(TBL_REMINDER_TYPES, COL_REMINDER_HOURS,
            COL_REMINDER_MINUTES)
        .addFrom(TBL_TASKS)
        .addFromInner(TBL_REMINDER_TYPES,
            sys.joinTables(TBL_REMINDER_TYPES, TBL_TASKS, COL_REMINDER))
        .setWhere(SqlUtils.and(
            SqlUtils.inList(TBL_TASKS, COL_STATUS, statusValues),
            SqlUtils.more(TBL_TASKS, COL_FINISH_TIME, System.currentTimeMillis()),
            SqlUtils.equals(TBL_REMINDER_TYPES,
                COL_REMINDER_METHOD,
                ReminderMethod.EMAIL.ordinal())))
        .addOrder(TBL_TASKS, COL_TASK_ID);

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return count;
    }

    for (SimpleRow row : data) {
      long reminderMillis;

      DateTime reminderTime = row.getDateTime(COL_REMINDER_TIME);
      if (reminderTime == null) {
        reminderMillis = row.getLong(COL_FINISH_TIME);

        Integer hours = row.getInt(COL_REMINDER_HOURS);
        Integer minutes = row.getInt(COL_REMINDER_MINUTES);

        if (BeeUtils.isPositive(hours) || BeeUtils.isPositive(minutes)) {
          if (BeeUtils.isPositive(hours)) {
            reminderMillis -= hours * TimeUtils.MILLIS_PER_HOUR;
          }
          if (BeeUtils.isPositive(minutes)) {
            reminderMillis -= minutes * TimeUtils.MILLIS_PER_MINUTE;
          }
        } else {
          reminderMillis -= TimeUtils.MILLIS_PER_DAY;
        }

      } else {
        reminderMillis = reminderTime.getTime();
      }

      boolean ok = System.currentTimeMillis() + timeRemaining / 2 > reminderMillis;
      if (ok) {
        Long sent = row.getLong(COL_REMINDER_SENT);
        if (sent != null) {
          ok = reminderMillis > sent + timeRemaining;
        }
      }
      if (!ok) {
        continue;
      }

      Long taskId = row.getLong(COL_TASK_ID);
      Long executor = row.getLong(COL_EXECUTOR);
      if (!DataUtils.isId(taskId) || !DataUtils.isId(executor)) {
        continue;
      }

      String recipientEmail = usr.getUserEmail(executor, false);
      if (BeeUtils.isEmpty(recipientEmail)) {
        logger.warning(label, "task", taskId, "executor", executor, "email not available");
        continue;
      }

      LocalizableConstants constants = usr.getLocalizableConstants(executor);
      if (constants == null) {
        logger.warning(label, "task", taskId, "executor", executor, "localization not available");
        continue;
      }

      Document document = taskToHtml(taskId, row.getDateTime(COL_START_TIME),
          row.getDateTime(COL_FINISH_TIME), row.getValue(COL_SUMMARY),
          row.getValue(COL_DESCRIPTION), row.getLong(COL_OWNER), executor, constants);
      String content = document.buildLines();

      logger.info(label, taskId, "mail to", executor, recipientEmail);

      ResponseObject mailResponse = mail.sendMail(accountId, recipientEmail,
          constants.crmReminderMailSubject(), content);

      if (mailResponse.hasErrors()) {
        logger.severe(label, "mail error - canceled");
        break;
      }

      count++;

      ResponseObject updateResponse = qs.updateDataWithResponse(new SqlUpdate(TBL_TASKS)
          .addConstant(COL_REMINDER_SENT, System.currentTimeMillis())
          .setWhere(SqlUtils.equals(TBL_TASKS, COL_TASK_ID, taskId)));

      if (updateResponse.hasErrors()) {
        updateResponse.log(logger);
        logger.severe(label, "update error - canceled");
        break;
      }
    }

    return count;
  }

  private Set<Long> spawnTasks(List<BeeColumn> rtColumns, BeeRow rtRow, JustDate date,
      Set<Long> executors, Set<Long> observers, Multimap<String, Long> relations,
      SimpleRowSet fileData, List<BeeColumn> taskColumns) {

    long rtId = rtRow.getId();

    List<BeeColumn> columns = new ArrayList<>();
    List<String> values = new ArrayList<>();

    columns.add(DataUtils.getColumn(COL_SUMMARY, taskColumns));
    values.add(DataUtils.getString(rtColumns, rtRow, COL_SUMMARY));

    String description = DataUtils.getString(rtColumns, rtRow, COL_DESCRIPTION);
    if (!BeeUtils.isEmpty(description)) {
      columns.add(DataUtils.getColumn(COL_DESCRIPTION, taskColumns));
      values.add(description.trim());
    }

    Long type = DataUtils.getLong(rtColumns, rtRow, COL_TASK_TYPE);
    if (DataUtils.isId(type)) {
      columns.add(DataUtils.getColumn(COL_TASK_TYPE, taskColumns));
      values.add(type.toString());
    }

    columns.add(DataUtils.getColumn(COL_PRIORITY, taskColumns));
    values.add(DataUtils.getString(rtColumns, rtRow, COL_PRIORITY));

    Long startAt = TimeUtils.parseTime(DataUtils.getString(rtColumns, rtRow, COL_RT_START_AT));
    DateTime startTime = TimeUtils.combine(date, startAt);

    columns.add(DataUtils.getColumn(COL_START_TIME, taskColumns));
    values.add(BeeUtils.toString(startTime.getTime()));

    Integer durationDays = DataUtils.getInteger(rtColumns, rtRow, COL_RT_DURATION_DAYS);
    Long durationMillis = TimeUtils.parseTime(DataUtils.getString(rtColumns, rtRow,
        COL_RT_DURATION_TIME));

    if (!BeeUtils.isPositive(durationDays) && !BeeUtils.isPositive(durationMillis)) {
      durationDays = 1;
    }

    long finishMillis = startTime.getTime();
    if (BeeUtils.isPositive(durationDays)) {
      finishMillis += durationDays * TimeUtils.MILLIS_PER_DAY;
    }
    if (BeeUtils.isPositive(durationMillis)) {
      finishMillis += durationMillis;
    }

    columns.add(DataUtils.getColumn(COL_FINISH_TIME, taskColumns));
    values.add(BeeUtils.toString(finishMillis));

    Long owner = DataUtils.getLong(rtColumns, rtRow, COL_OWNER);
    columns.add(DataUtils.getColumn(COL_OWNER, taskColumns));
    values.add(owner.toString());

    columns.add(DataUtils.getColumn(COL_EXECUTOR, taskColumns));
    values.add(null);

    Long company = DataUtils.getLong(rtColumns, rtRow, COL_COMPANY);
    if (DataUtils.isId(company)) {
      columns.add(DataUtils.getColumn(COL_COMPANY, taskColumns));
      values.add(company.toString());
    }

    Long contact = DataUtils.getLong(rtColumns, rtRow, COL_CONTACT);
    if (DataUtils.isId(contact)) {
      columns.add(DataUtils.getColumn(COL_CONTACT, taskColumns));
      values.add(contact.toString());
    }

    Long project = DataUtils.getLong(rtColumns, rtRow, ProjectConstants.COL_PROJECT);
    if (DataUtils.isId(project)) {
      columns.add(DataUtils.getColumn(ProjectConstants.COL_PROJECT, taskColumns));
      values.add(project.toString());
    }

    Long reminder = DataUtils.getLong(rtColumns, rtRow, COL_RT_REMINDER);
    if (DataUtils.isId(reminder)) {
      columns.add(DataUtils.getColumn(COL_REMINDER, taskColumns));
      values.add(reminder.toString());
    }

    Integer remindBefore = DataUtils.getInteger(rtColumns, rtRow, COL_RT_REMIND_BEFORE);
    Long remindAt = TimeUtils.parseTime(DataUtils.getString(rtColumns, rtRow,
        COL_RT_REMIND_AT));

    if (BeeUtils.isPositive(remindBefore) || BeeUtils.isPositive(remindAt)) {
      long reminderMillis = finishMillis;

      if (BeeUtils.isPositive(remindBefore)) {
        reminderMillis -= remindBefore * TimeUtils.MILLIS_PER_DAY;
      }
      if (BeeUtils.isPositive(remindAt)) {
        reminderMillis = TimeUtils.combine(new DateTime(reminderMillis), remindAt).getTime();
        if (reminderMillis >= finishMillis) {
          reminderMillis -= TimeUtils.MILLIS_PER_DAY;
        }
      }

      columns.add(DataUtils.getColumn(COL_REMINDER_TIME, taskColumns));
      values.add(BeeUtils.toString(reminderMillis));
    }

    columns.add(DataUtils.getColumn(COL_STATUS, taskColumns));
    values.add(null);

    columns.add(DataUtils.getColumn(COL_RECURRING_TASK, taskColumns));
    values.add(BeeUtils.toString(rtId));

    BeeRowSet taskData = new BeeRowSet(VIEW_TASKS, columns);
    BeeRow taskRow = new BeeRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION, values);

    taskRow.setProperty(PROP_EXECUTORS, DataUtils.buildIdList(executors));
    if (!observers.isEmpty()) {
      taskRow.setProperty(PROP_OBSERVERS, DataUtils.buildIdList(observers));
    }

    if (!relations.isEmpty()) {
      for (String property : relations.keySet()) {
        taskRow.setProperty(property, DataUtils.buildIdList(relations.get(property)));
      }
    }

    ResponseObject response = createTasks(taskData, taskRow, owner);
    if (response.hasErrors() || !response.hasResponse()) {
      response.log(logger);

      Set<Long> result = new HashSet<>();
      return result;
    }

    Set<Long> tasks = DataUtils.parseIdSet(response.getResponseAsString());

    if (!DataUtils.isEmpty(fileData)) {
      for (SimpleRow fileRow : fileData) {
        for (Long taskId : tasks) {
          SqlInsert si = new SqlInsert(TBL_TASK_FILES)
              .addConstant(COL_TASK, taskId)
              .addConstant(COL_FILE, fileRow.getLong(COL_FILE))
              .addConstant(COL_CAPTION, fileRow.getLong(COL_FILE_CAPTION));

          qs.insertData(si);
        }
      }
    }

    return tasks;
  }

  private ResponseObject spawnTasks(RequestInfo reqInfo) {
    Long rtId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_RT_ID));
    if (!DataUtils.isId(rtId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_RT_ID);
    }

    Integer dayNumber = BeeUtils.toIntOrNull(reqInfo.getParameter(VAR_RT_DAY));
    if (!BeeUtils.isPositive(dayNumber)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VAR_RT_DAY);
    }

    Long executor = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_EXECUTOR));

    BeeRowSet rtData = qs.getViewData(VIEW_RECURRING_TASKS, Filter.compareId(rtId));
    if (DataUtils.isEmpty(rtData)) {
      return ResponseObject.error(reqInfo.getService(), VIEW_RECURRING_TASKS, rtId,
          "not available");
    }

    Set<Long> executors;
    if (DataUtils.isId(executor)) {
      executors = Sets.newHashSet(executor);
    } else {
      executors = getRecurringTaskExecutors(rtId);
      if (executors.isEmpty()) {
        return ResponseObject.error(reqInfo.getService(), rtId, "executors not available");
      }
    }

    Set<Long> observers = getRecurringTaskObservers(rtId);

    Multimap<String, Long> relations = getRelations(COL_RECURRING_TASK, rtId);
    SimpleRowSet fileData = getRecurringTaskFileData(rtId);

    List<BeeColumn> taskColumns = sys.getView(VIEW_TASKS).getRowSetColumns();

    Set<Long> tasks = spawnTasks(rtData.getColumns(), rtData.getRow(0), new JustDate(dayNumber),
        executors, observers, relations, fileData, taskColumns);
    if (tasks.isEmpty()) {
      return ResponseObject.emptyResponse();
    }

    mailScheduledTasks(tasks);

    BeeRowSet result = qs.getViewData(VIEW_TASKS, Filter.idIn(tasks));
    return ResponseObject.response(result);
  }

  @Schedule(minute = "0,30", hour = "*", persistent = false)
  private void taskReminderTimeout(Timer timer) {
    if (!Config.isInitialized()) {
      return;
    }
    long timeRemaining = timer.getTimeRemaining();
    logger.info("task reminder timeout, time remainining", timeRemaining);

    int count = sendTaskReminders(timeRemaining);
    logger.info("sent", count, "task reminders");
  }

  private Document taskToHtml(long taskId, DateTime startTime, DateTime finishTime,
      String summary, String description, Long owner, Long executor,
      LocalizableConstants constants) {

    String caption = BeeUtils.joinWords(constants.crmTask(), taskId);

    Document doc = new Document();

    doc.getHead().append(
        meta().encodingDeclarationUtf8(),
        title().text(caption));

    Div panel = div();
    doc.getBody().append(panel);

    panel.append(h3().text(caption));

    Tbody fields = tbody().append(
        tr().append(
            td().text(constants.crmStartDate()), td().text(TimeUtils.renderCompact(startTime))),
        tr().append(
            td().text(constants.crmFinishDate()), td().text(TimeUtils.renderCompact(finishTime))),
        tr().append(
            td().text(constants.crmTaskSubject()), td().text(BeeUtils.trim(summary))));

    if (!BeeUtils.isEmpty(description)) {
      fields.append(tr().append(
          td().verticalAlign(VerticalAlign.TOP).text(constants.crmTaskDescription()),
          td().whiteSpace(WhiteSpace.PRE_LINE).text(BeeUtils.trim(description))));
    }

    if (owner != null) {
      fields.append(tr().append(
          td().text(constants.crmTaskManager()),
          td().text(usr.getUserSign(owner))));
    }

    List<Long> taskUsers = getTaskUsers(taskId);
    taskUsers.remove(owner);
    taskUsers.remove(executor);

    if (!taskUsers.isEmpty()) {
      for (int i = 0; i < taskUsers.size(); i++) {
        Td td = td();
        if (i == 0) {
          td.text(constants.crmTaskObservers());
        }

        fields.append(tr().append(td, td().text(usr.getUserSign(taskUsers.get(i)))));
      }
    }

    List<Element> cells = fields.queryTag(Tags.TD);
    for (Element cell : cells) {
      if (cell.index() == 0) {
        cell.setPaddingRight(1, CssUnit.EM);
        cell.setFontWeight(FontWeight.BOLDER);
      }
    }

    panel.append(table().append(fields));

    return doc;
  }

  private ResponseObject updateTaskRelations(long taskId, Set<String> updatedRelations,
      BeeRow row) {
    ResponseObject response = new ResponseObject();
    List<RowChildren> children = new ArrayList<>();

    for (String property : updatedRelations) {
      String relation = TaskUtils.translateTaskPropertyToRelation(property);

      if (!BeeUtils.isEmpty(relation)) {
        children.add(RowChildren.create(TBL_RELATIONS, COL_TASK, taskId,
            relation, row.getProperty(property)));
      }
    }
    int count = 0;

    if (!BeeUtils.isEmpty(children)) {
      count = deb.commitChildren(taskId, children, response);
    }
    return response.setResponse(count);
  }

  private void updateTaskUsers(long taskId, Collection<Long> oldUsers, Collection<Long> newUsers) {
    List<Long> insert = new ArrayList<>(newUsers);
    insert.removeAll(oldUsers);

    List<Long> delete = new ArrayList<>(oldUsers);
    delete.removeAll(newUsers);

    for (Long user : insert) {
      createTaskUser(taskId, user, null);
    }

    String tblName = TBL_TASK_USERS;
    for (Long user : delete) {
      IsCondition condition = SqlUtils.equals(tblName, COL_TASK, taskId, COL_USER, user);
      qs.updateData(new SqlDelete(tblName).setWhere(condition));
    }
  }
}
