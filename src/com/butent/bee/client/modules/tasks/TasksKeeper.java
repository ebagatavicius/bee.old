package com.butent.bee.client.modules.tasks;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.NewsAggregator.HeadlineAccessor;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.modules.tasks.TasksReportsInterceptor.ReportType;
import com.butent.bee.client.style.ColorStyleProvider;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.grid.interceptor.FileGridInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskEvent;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.modules.tasks.TaskType;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateRange;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TasksKeeper {

  private static final String COMPANY_TIMES_REPORT = "companytimes";
  private static final String TYPE_HOURS_REPORT = "typehours";
  private static final String USERS_HOURS_REPORT = "usershours";

  private static class RowTransformHandler implements RowTransformEvent.Handler {

    private final List<String> taskColumns = Lists.newArrayList(COL_SUMMARY,
        ClassifierConstants.ALS_COMPANY_NAME, ALS_EXECUTOR_FIRST_NAME, ALS_EXECUTOR_LAST_NAME,
        COL_FINISH_TIME, COL_STATUS);

    private DataInfo taskViewInfo;

    @Override
    public void onRowTransform(RowTransformEvent event) {
      if (event.hasView(VIEW_TASKS)) {
        event.setResult(DataUtils.join(getTaskViewInfo(), event.getRow(), taskColumns,
            BeeConst.STRING_SPACE));
      }
    }

    private DataInfo getTaskViewInfo() {
      if (this.taskViewInfo == null) {
        this.taskViewInfo = Data.getDataInfo(VIEW_TASKS);
      }
      return this.taskViewInfo;
    }
  }

  public static void extendTask(final long taskId, final DateTime start, final DateTime finish) {
    Queries.getRow(VIEW_TASKS, taskId, new RowCallback() {
      @Override
      public void onSuccess(final BeeRow row) {
        final TaskDialog dialog = new TaskDialog(Localized.getConstants().crmTaskTermChange());

        TaskStatus status = EnumUtils.getEnumByIndex(TaskStatus.class,
            Data.getInteger(VIEW_TASKS, row, COL_STATUS));
        final boolean isScheduled = status == TaskStatus.SCHEDULED;

        final String startId = isScheduled
            ? dialog.addDateTime(Localized.getConstants().crmStartDate(), true, start) : null;
        final String endId = dialog.addDateTime(Localized.getConstants().crmFinishDate(), true,
            finish);

        final String cid = dialog.addComment(false);

        dialog.addAction(Localized.getConstants().crmTaskChangeTerm(), new ScheduledCommand() {
          @Override
          public void execute() {
            DateTime oldStart = Data.getDateTime(VIEW_TASKS, row, COL_START_TIME);
            DateTime oldEnd = Data.getDateTime(VIEW_TASKS, row, COL_FINISH_TIME);

            DateTime newStart = (startId == null) ? oldStart
                : BeeUtils.nvl(dialog.getDateTime(startId), oldStart);
            DateTime newEnd = dialog.getDateTime(endId);

            if (newEnd == null) {
              Global.showError(Localized.getConstants().crmEnterFinishDate());
              return;
            }

            if (Objects.equals(newStart, oldStart) && Objects.equals(newEnd, oldEnd)) {
              Global.showError(Localized.getConstants().crmTermNotChanged());
              return;
            }

            if (TimeUtils.isLeq(newEnd, newStart)) {
              Global.showError(Localized.getConstants().crmFinishDateMustBeGreaterThanStart());
              return;
            }

            DateTime now = TimeUtils.nowMinutes();
            if (TimeUtils.isLess(newEnd, TimeUtils.nowMinutes())) {
              Global.showError("Time travel not supported",
                  Lists.newArrayList(Localized.getConstants().crmFinishDateMustBeGreaterThan()
                      + " "
                      + now.toCompactString()));
              return;
            }

            List<String> notes = new ArrayList<>();

            ParameterList params = createArgs(SVC_EXTEND_TASK);
            params.addQueryItem(VAR_TASK_ID, taskId);

            if (startId != null && newStart != null && !Objects.equals(newStart, oldStart)) {
              params.addQueryItem(COL_START_TIME, newStart.getTime());
              notes.add(TaskUtils.getUpdateNote(Localized.getConstants().crmStartDate(),
                  TimeUtils.renderCompact(oldStart), TimeUtils.renderCompact(newStart)));
            }

            if (!Objects.equals(newEnd, oldEnd)) {
              params.addQueryItem(COL_FINISH_TIME, newEnd.getTime());
              notes.add(TaskUtils.getUpdateNote(Localized.getConstants().crmFinishDate(),
                  TimeUtils.renderCompact(oldEnd), TimeUtils.renderCompact(newEnd)));
            }

            String comment = dialog.getComment(cid);
            if (!BeeUtils.isEmpty(comment)) {
              params.addDataItem(VAR_TASK_COMMENT, comment);
            }

            if (!notes.isEmpty()) {
              params.addDataItem(VAR_TASK_NOTES, Codec.beeSerialize(notes));
            }

            BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject response) {
                if (Queries.checkRowResponse(SVC_EXTEND_TASK, VIEW_TASKS, response)) {
                  BeeRow updatedRow = BeeRow.restore(response.getResponseAsString());
                  RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TASKS, updatedRow);
                }
              }
            });

            dialog.close();
          }
        });

        dialog.display(endId);
      }
    });
  }

  public static void register() {
    FormFactory.registerFormInterceptor(FORM_NEW_TASK, new TaskBuilder());
    FormFactory.registerFormInterceptor(FORM_TASK, new TaskEditor());

    FormFactory.registerFormInterceptor(FORM_RECURRING_TASK, new RecurringTaskHandler());

    FormFactory.registerFormInterceptor(FORM_NEW_REQUEST, new RequestBuilder());
    FormFactory.registerFormInterceptor(FORM_REQUEST, new RequestEditor());

    GridFactory.registerGridInterceptor(GRID_TODO_LIST, new TodoListInterceptor());

    GridFactory.registerGridInterceptor(GRID_CHILD_REQUESTS, new ChildRequestsGrid());

    GridFactory.registerGridInterceptor(GRID_RECURRING_TASKS, new RecurringTasksGrid());
    GridFactory.registerGridInterceptor(GRID_RT_FILES,
        new FileGridInterceptor(COL_RTF_RECURRING_TASK, AdministrationConstants.COL_FILE,
            AdministrationConstants.COL_FILE_CAPTION, AdministrationConstants.ALS_FILE_NAME));

    GridFactory.registerGridInterceptor(GRID_RELATED_TASKS, new RelatedTasksGrid());
    GridFactory.registerGridInterceptor(GRID_RELATED_RECURRING_TASKS,
        new RelatedRecurringTasksGrid());

    GridFactory.registerGridInterceptor(GRID_CHILD_TASKS, new ChildTasksGrid());
    GridFactory.registerGridInterceptor(GRID_CHILD_RECURRING_TASKS, new ChildRecurringTasksGrid());

    for (TaskType tt : TaskType.values()) {
      GridFactory.registerGridSupplier(tt.getSupplierKey(), GRID_TASKS,
          new TasksGrid(tt, tt.getCaption()));
    }

    MenuService.TASK_LIST.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        TaskType type = TaskType.getByPrefix(parameters);

        if (type == null) {
          Global.showError(Lists.newArrayList(GRID_TASKS, "Type not recognized:", parameters));
        } else {
          ViewFactory.createAndShow(type.getSupplierKey());
        }
      }
    });

    MenuService.TASK_REPORTS.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        if (BeeUtils.startsSame(parameters, COMPANY_TIMES_REPORT)) {
          FormFactory.openForm(FORM_TASKS_REPORT,
              new TasksReportsInterceptor(ReportType.COMPANY_TIMES));
        } else if (BeeUtils.startsSame(parameters, TYPE_HOURS_REPORT)) {
          FormFactory.openForm(FORM_TASKS_REPORT,
              new TasksReportsInterceptor(ReportType.TYPE_HOURS));
        } else if (BeeUtils.startsSame(parameters, USERS_HOURS_REPORT)) {
          FormFactory.openForm(FORM_TASKS_REPORT,
              new TasksReportsInterceptor(ReportType.USERS_HOURS));
        } else {
          Global.showError("Service type '" + parameters + "' not found");
        }
      }
    });

    for (ReportType reportType : ReportType.values()) {
      reportType.register();
    }

    SelectorEvent.register(new TaskSelectorHandler());

    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler(), false);

    Global.getNewsAggregator().registerFilterHandler(Feed.TASKS_ASSIGNED,
        TasksGrid.getFeedFilterHandler(Feed.TASKS_ASSIGNED));
    Global.getNewsAggregator().registerFilterHandler(Feed.TASKS_DELEGATED,
        TasksGrid.getFeedFilterHandler(Feed.TASKS_DELEGATED));
    Global.getNewsAggregator().registerFilterHandler(Feed.TASKS_OBSERVED,
        TasksGrid.getFeedFilterHandler(Feed.TASKS_OBSERVED));
    Global.getNewsAggregator().registerFilterHandler(Feed.TASKS_ALL,
        TasksGrid.getFeedFilterHandler(Feed.TASKS_ALL));

    Global.getNewsAggregator().registerAccessHandler(VIEW_TASKS, new HeadlineAccessor() {
      @Override
      public boolean read(Long id) {
        return false;
      }

      @Override
      public void access(Long id) {
        ParameterList params = createArgs(SVC_ACCESS_TASK);
        params.addQueryItem(VAR_TASK_ID, id);

        BeeKeeper.getRpc().makeRequest(params);
      }
    });

    ColorStyleProvider styleProvider = ColorStyleProvider.createDefault(VIEW_TASK_TYPES);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TASK_TYPES,
        AdministrationConstants.COL_BACKGROUND, styleProvider);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TASK_TYPES,
        AdministrationConstants.COL_FOREGROUND, styleProvider);

    Map<String, String> containsTaskType = new HashMap<>();

    containsTaskType.put(VIEW_TASKS, GRID_TASKS);
    containsTaskType.put(VIEW_RELATED_TASKS, GRID_RELATED_TASKS);
    containsTaskType.put(VIEW_RECURRING_TASKS, GRID_RECURRING_TASKS);
    containsTaskType.put(VIEW_RELATED_RECURRING_TASKS, GRID_RELATED_RECURRING_TASKS);
    containsTaskType.put(VIEW_TASK_TEMPLATES, GRID_TASK_TEMPLATES);

    for (Map.Entry<String, String> entry : containsTaskType.entrySet()) {
      styleProvider = ColorStyleProvider.create(entry.getKey(),
          ALS_TASK_TYPE_BACKGROUND, ALS_TASK_TYPE_FOREGROUND);
      ConditionalStyle.registerGridColumnStyleProvider(entry.getValue(), COL_TASK_TYPE,
          styleProvider);
    }
  }

  public static void scheduleTasks(final DateRange range) {
    Assert.notNull(range);

    ParameterList params = createArgs(SVC_RT_SCHEDULE);
    params.addQueryItem(COL_RT_SCHEDULE_FROM, range.getMinDays());
    if (range.size() > 1) {
      params.addQueryItem(COL_RT_SCHEDULE_UNTIL, range.getMaxDays());
    }

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse() && BeeUtils.isPositiveInt(response.getResponseAsString())) {
          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TASKS);
          BeeKeeper.getScreen().notifyInfo(BeeUtils.joinWords(range, "sheduled",
              response.getResponseAsString(), "tasks"));

        } else {
          BeeKeeper.getScreen().notifyWarning(range.toString(), "no tasks sheduled");
        }
      }
    });
  }

  static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.TASKS, method);
  }

  static ParameterList createTaskRequestParameters(TaskEvent event) {
    return createArgs(CRM_TASK_PREFIX + event.name());
  }

  private TasksKeeper() {
  }
}
