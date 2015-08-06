package com.butent.bee.shared.modules.tasks;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.EnumUtils;

public final class TaskConstants {

  public enum TaskEvent implements HasCaption {
    CREATE(Localized.getConstants().crmTaskEventCreated(), Localized.getConstants().crmNewTask(),
        FontAwesome.CODE_FORK),
    VISIT(Localized.getConstants().crmTaskEventVisited(), null, null),
    ACTIVATE(Localized.getConstants().crmTaskForwardedForExecution(), Localized.getConstants()
        .crmTaskForwardForExecution(), FontAwesome.ARROW_CIRCLE_RIGHT),
    COMMENT(Localized.getConstants().crmTaskComment(), Localized.getConstants().crmActionComment(),
        FontAwesome.COMMENT_O),
    EXTEND(Localized.getConstants().crmTaskEventExtended(), Localized.getConstants()
        .crmTaskChangeTerm(), FontAwesome.CLOCK_O),
    SUSPEND(Localized.getConstants().crmTaskStatusSuspended(), Localized.getConstants()
        .crmActionSuspend(), FontAwesome.MINUS_CIRCLE),
    RENEW(Localized.getConstants().crmTaskEventRenewed(), Localized.getConstants()
        .crmTaskReturnExecution(), FontAwesome.ARROW_CIRCLE_RIGHT),
    FORWARD(Localized.getConstants().crmTaskEventForwarded(), Localized.getConstants()
        .crmActionForward(), FontAwesome.ARROW_CIRCLE_O_RIGHT),
    CANCEL(Localized.getConstants().crmTaskStatusCanceled(), Localized.getConstants()
        .crmTaskCancel(), FontAwesome.BAN),
    COMPLETE(Localized.getConstants().crmTaskStatusCompleted(), Localized.getConstants()
        .crmActionFinish(), FontAwesome.CHECK_CIRCLE_O),
    APPROVE(Localized.getConstants().crmTaskEventApproved(), Localized.getConstants()
        .crmTaskConfirm(), FontAwesome.CHECK_SQUARE_O),
    EDIT(Localized.getConstants().crmTaskEventEdited(), null, null),
    OUT_OF_OBSERVERS(Localized.getConstants().crmTaskOutOfObservers(), Localized.getConstants()
        .crmTaskOutOfObservers(),
        FontAwesome.USER_TIMES);

    private final String caption;
    private final String commandLabel;
    private final FontAwesome commandIcon;

    private TaskEvent(String caption, String commandLabel, FontAwesome commandIcon) {
      this.caption = caption;
      this.commandLabel = commandLabel;
      this.commandIcon = commandIcon;
    }

    @Override
    public String getCaption() {
      return caption;
    }

    public String getCommandLabel() {
      return commandLabel;
    }

    public FontAwesome getCommandIcon() {
      return commandIcon;
    }
  }

  public enum TaskPriority implements HasCaption {
    LOW(Localized.getConstants().crmTaskPriorityLow()), MEDIUM(Localized.getConstants()
        .crmTaskPriorityMedium()), HIGH(Localized.getConstants().crmTaskPriorityHigh());

    private final String caption;

    private TaskPriority(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public enum TaskStatus implements HasLocalizedCaption {
    NOT_VISITED {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.crmTaskStatusNotVisited();
      }
    },
    ACTIVE {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.crmTaskStatusActive();
      }
    },
    SCHEDULED {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.crmTaskStatusScheduled();
      }
    },
    SUSPENDED {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.crmTaskStatusSuspended();
      }
    },
    COMPLETED {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.crmTaskStatusCompleted();
      }
    },
    APPROVED {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.crmTaskStatusApproved();
      }
    },
    CANCELED {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.crmTaskStatusCanceled();
      }
    };

    public static boolean in(int status, TaskStatus... statuses) {
      for (TaskStatus ts : statuses) {
        if (ts.ordinal() == status) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String getCaption() {
      return getCaption(Localized.getConstants());
    }

    public boolean is(Integer status) {
      return status != null && ordinal() == status;
    }
  }

  public enum ToDoVisibility implements HasCaption {
    PUBLIC(Localized.getConstants().calPublic()),
    PRIVATE(Localized.getConstants().calPrivate());

    private final String caption;

    private ToDoVisibility(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public static void register() {
    EnumUtils.register(TaskPriority.class);
    EnumUtils.register(TaskEvent.class);
    EnumUtils.register(TaskStatus.class);
    EnumUtils.register(ToDoVisibility.class);
  }

  public static final String CRM_TASK_PREFIX = "task_";

  public static final String SVC_GET_TASK_DATA = "get_task_data";
  public static final String SVC_GET_CHANGED_TASKS = "get_changed_tasks";
  public static final String SVC_ACCESS_TASK = "access_task";
  public static final String SVC_EXTEND_TASK = "extend_task";

  public static final String SVC_CONFIRM_TASKS = "confirm_tasks";

  public static final String SVC_TASKS_REPORTS_PREFIX = "get_tasks_reports_";
  public static final String SVC_TASKS_REPORTS_COMPANY_TIMES = SVC_TASKS_REPORTS_PREFIX
      + "company_times";
  public static final String SVC_TASKS_REPORTS_TYPE_HOURS = SVC_TASKS_REPORTS_PREFIX + "type_hours";
  public static final String SVC_TASKS_REPORTS_USERS_HOURS = SVC_TASKS_REPORTS_PREFIX
      + "users_hours";

  public static final String SVC_GET_REQUEST_FILES = "get_request_files";

  public static final String SVC_RT_GET_SCHEDULING_DATA = "rt_get_scheduling_data";
  public static final String SVC_RT_SPAWN = "rt_spawn";
  public static final String SVC_RT_SCHEDULE = "rt_schedule";
  public static final String SVC_RT_COPY = "rt_copy";

  public static final String VAR_TASK_DATA = Service.RPC_VAR_PREFIX + "task_data";
  public static final String VAR_TASK_ID = Service.RPC_VAR_PREFIX + "task_id";
  public static final String VAR_TASK_APPROVED_TIME = Service.RPC_VAR_PREFIX + "task_approved";

  public static final String VAR_TASK_COMMENT = Service.RPC_VAR_PREFIX + "task_comment";
  public static final String VAR_TASK_NOTES = Service.RPC_VAR_PREFIX + "task_notes";
  public static final String VAR_TASK_FINISH_TIME = Service.RPC_VAR_PREFIX + "task_finish_time";
  public static final String VAR_TASK_PUBLISHER = Service.RPC_VAR_PREFIX + "task_publisher";
  public static final String VAR_TASK_COMPANY = Service.RPC_VAR_PREFIX + "task_company";
  public static final String VAR_TASK_ACTIVE = "Active";
  public static final String VAR_TASK_LATE = "Late";
  public static final String VAR_TASK_COMPLETED = "Completed";
  public static final String VAR_TASK_SHEDULED = "Sheduled";

  public static final String VAR_TASK_DURATION_DATE = Service.RPC_VAR_PREFIX + "task_duration_date";
  public static final String VAR_TASK_DURATION_TIME = Service.RPC_VAR_PREFIX + "task_duration_time";
  public static final String VAR_TASK_DURATION_TYPE = Service.RPC_VAR_PREFIX + "task_duration_type";
  public static final String VAR_TASK_DURATION_DATE_FROM = VAR_TASK_DURATION_DATE + "_from";
  public static final String VAR_TASK_DURATION_DATE_TO = VAR_TASK_DURATION_DATE + "_to";
  public static final String VAR_TASK_DURATION_HIDE_ZEROS = Service.RPC_VAR_PREFIX
      + "task_duration_hide_zeros";

  public static final String VAR_TASK_RELATIONS = Service.RPC_VAR_PREFIX + "task_relations";
  public static final String VAR_TASK_USERS = Service.RPC_VAR_PREFIX + "task_users";
  public static final String VAR_TASK_PROPERTIES = Service.RPC_VAR_PREFIX + "task_properties";

  public static final String VAR_TASK_VISITED = Service.RPC_VAR_PREFIX + "task_visited";

  public static final String VAR_RT_ID = Service.RPC_VAR_PREFIX + "rt_id";
  public static final String VAR_RT_DAY = Service.RPC_VAR_PREFIX + "rt_day";

  public static final String TBL_REQUESTS = "Requests";
  public static final String TBL_REQUEST_FILES = "RequestFiles";

  public static final String TBL_TASKS = "Tasks";
  public static final String TBL_TASK_USERS = "TaskUsers";
  public static final String TBL_TASK_EVENTS = "TaskEvents";
  public static final String TBL_TASK_FILES = "TaskFiles";

  public static final String TBL_TASK_TYPES = "TaskTypes";

  public static final String TBL_DURATION_TYPES = "DurationTypes";
  public static final String TBL_EVENT_DURATIONS = "EventDurations";

  public static final String TBL_RECURRING_TASKS = "RecurringTasks";

  public static final String TBL_RT_DATES = "RTDates";
  public static final String TBL_RT_FILES = "RTFiles";
  public static final String TBL_RT_EXECUTORS = "RTExecutors";
  public static final String TBL_RT_EXECUTOR_GROUPS = "RTExecutorGroups";
  public static final String TBL_RT_OBSERVERS = "RTObservers";
  public static final String TBL_RT_OBSERVER_GROUPS = "RTObserverGroups";

  public static final String VIEW_TASKS = "Tasks";
  public static final String VIEW_TASK_FILES = "TaskFiles";
  public static final String VIEW_TASK_USERS = "TaskUsers";
  public static final String VIEW_TASK_EVENTS = "TaskEvents";
  public static final String VIEW_TASK_DURATIONS = "TaskDurations";

  public static final String VIEW_TASK_TEMPLATES = "TaskTemplates";
  public static final String VIEW_TASK_TYPES = "TaskTypes";

  public static final String VIEW_DURATION_TYPES = "DurationTypes";

  public static final String VIEW_RECURRING_TASKS = "RecurringTasks";
  public static final String VIEW_RT_DATES = "RTDates";
  public static final String VIEW_RT_FILES = "RTFiles";

  public static final String VIEW_REQUEST_FILES = "RequestFiles";

  public static final String VIEW_RELATED_TASKS = "RelatedTasks";
  public static final String VIEW_RELATED_RECURRING_TASKS = "RelatedRecurringTasks";

  public static final String VIEW_TODO_LIST = "TodoList";

  public static final String COL_START_TIME = "StartTime";
  public static final String COL_FINISH_TIME = "FinishTime";

  public static final String COL_TASK_TYPE = "Type";
  public static final String COL_PRIORITY = "Priority";

  public static final String COL_OWNER = "Owner";
  public static final String COL_EXECUTOR = "Executor";
  public static final String COL_TASK_EXECUTOR = "TaskExecutor";

  public static final String COL_TASK_ID = "TaskID";

  public static final String COL_TASK = "Task";

  public static final String COL_TASK_TEMPLATE_NAME = "Name";
  public static final String COL_TASK_TYPE_NAME = "Name";

  public static final String COL_SUMMARY = "Summary";
  public static final String COL_DESCRIPTION = "Description";
  public static final String COL_CAPTION = "Caption";
  public static final String COL_TASK_COMPANY = "Company";

  public static final String COL_PARENT = "Parent";
  public static final String COL_ORDER = "Order";

  public static final String COL_FILE_DATE = "FileDate";
  public static final String COL_FILE_VERSION = "FileVersion";

  public static final String COL_EXPIRES = "Expires";

  public static final String COL_REMINDER = "Reminder";
  public static final String COL_REMINDER_TIME = "ReminderTime";
  public static final String COL_REMINDER_SENT = "ReminderSent";
  public static final String COL_STATUS = "Status";
  public static final String COL_EXPECTED_DURATION = "ExpectedDuration";

  public static final String COL_PUBLISH_TIME = "PublishTime";
  public static final String COL_PUBLISHER = "Publisher";

  public static final String COL_COMMENT = "Comment";

  public static final String COL_TASK_EVENT = "TaskEvent";

  public static final String COL_DURATION_DATE = "DurationDate";
  public static final String COL_DURATION_TYPE = "DurationType";
  public static final String COL_DURATION = "Duration";

  public static final String COL_DURATION_TYPE_NAME = "Name";

  public static final String COL_EVENT = "Event";
  public static final String COL_EVENT_NOTE = "EventNote";
  public static final String COL_EVENT_DATA = "EventData";
  public static final String COL_EVENT_DURATION = "EventDuration";

  public static final String COL_LAST_ACCESS = "LastAccess";
  public static final String COL_STAR = "Star";

  public static final String COL_COMPLETED = "Completed";
  public static final String COL_APPROVED = "Approved";
  public static final String COL_ACTUAL_DURATION = "ActualDuration";
  public static final String COL_ACTUAL_EXPENSES = "ActualExpenses";
  public static final String COL_EXPECTED_EXPENSES = "ExpectedExpenses";

  public static final String COL_REQUEST = "Request";
  public static final String COL_REQUEST_CONTACTS = "Contacts";
  public static final String COL_REQUEST_CONTENT = "Content";
  public static final String COL_REQUEST_CUSTOMER = "Customer";
  public static final String COL_REQUEST_CUSTOMER_NAME = "CustomerName";
  public static final String COL_REQUEST_CUSTOMER_PERSON = "CustomerPerson";
  public static final String COL_REQUEST_DATE = "Date";
  public static final String COL_REQUEST_MANAGER = "Manager";
  public static final String COL_REQUEST_RESULT = "Result";
  public static final String COL_REQUEST_RESULT_PROPERTIES = "ResultProperties";
  public static final String COL_REQUEST_FINISHED = "Finished";

  public static final String COL_RECURRING_TASK = "RecurringTask";

  public static final String COL_RT_SCHEDULE_FROM = "ScheduleFrom";
  public static final String COL_RT_SCHEDULE_UNTIL = "ScheduleUntil";
  public static final String COL_RT_SCHEDULE_DAYS = "ScheduleDays";
  public static final String COL_RT_WORKDAY_TRANSITION = "WorkdayTransition";
  public static final String COL_RT_DAY_OF_MONTH = "DayOfMonth";
  public static final String COL_RT_MONTH = "Month";
  public static final String COL_RT_DAY_OF_WEEK = "DayOfWeek";
  public static final String COL_RT_YEAR = "Year";
  public static final String COL_RT_START_AT = "StartAt";
  public static final String COL_RT_DURATION_DAYS = "DurationDays";
  public static final String COL_RT_DURATION_TIME = "DurationTime";
  public static final String COL_RT_REMINDER = "Reminder";
  public static final String COL_RT_REMIND_BEFORE = "RemindBefore";
  public static final String COL_RT_REMIND_AT = "RemindAt";
  public static final String COL_RT_COPY_BY_MAIL = "CopyByMail";

  public static final String COL_RTD_RECURRING_TASK = "RecurringTask";
  public static final String COL_RTD_FROM = "DateFrom";
  public static final String COL_RTD_UNTIL = "DateUntil";
  public static final String COL_RTD_MODE = "Mode";

  public static final String COL_RTF_RECURRING_TASK = "RecurringTask";

  public static final String COL_RTEX_RECURRING_TASK = "RecurringTask";
  public static final String COL_RTEX_USER = "User";
  public static final String COL_RTEXGR_RECURRING_TASK = "RecurringTask";
  public static final String COL_RTEXGR_GROUP = "Group";

  public static final String COL_RTOB_RECURRING_TASK = "RecurringTask";
  public static final String COL_RTOB_USER = "User";
  public static final String COL_RTOBGR_RECURRING_TASK = "RecurringTask";
  public static final String COL_RTOBGR_GROUP = "Group";

  public static final String COL_MAIL_ASSIGNED_TASKS = "MailAssignedTasks";

  public static final String ALS_CONTACT_FIRST_NAME = "ContactFirstName";
  public static final String ALS_CONTACT_LAST_NAME = "ContactLastName";

  public static final String ALS_EXECUTOR_FIRST_NAME = "ExecutorFirstName";
  public static final String ALS_EXECUTOR_LAST_NAME = "ExecutorLastName";

  public static final String ALS_OWNER_FIRST_NAME = "OwnerFirstName";
  public static final String ALS_OWNER_LAST_NAME = "OwnerLastName";

  public static final String ALS_PUBLISHER_FIRST_NAME = "PublisherFirstName";
  public static final String ALS_PUBLISHER_LAST_NAME = "PublisherLastName";

  public static final String ALS_PERSON_FIRST_NAME = "PersonFirstName";
  public static final String ALS_PERSON_LAST_NAME = "PersonLastName";
  public static final String ALS_PERSON_COMPANY_NAME = "PersonCompanyName";

  public static final String ALS_TASK_SUBJECT = "TaskSubject";
  public static final String ALS_TASK_TYPE_NAME = "TypeName";
  public static final String ALS_TASK_TYPE_BACKGROUND = "TypeBackground";
  public static final String ALS_TASK_TYPE_FOREGROUND = "TypeForeground";
  public static final String ALS_PROJECT_OWNER = "ProjectOwner";
  public static final String ALS_PROJECT_STATUS = "ProjectStatus";

  public static final String ALS_LAST_SPAWN = "LastSpawn";

  public static final String PROP_EXECUTORS = "Executors";
  public static final String PROP_EXECUTOR_GROUPS = "ExecutorGroups";
  public static final String PROP_OBSERVERS = "Observers";
  public static final String PROP_OBSERVER_GROUPS = "ObserverGroups";

  public static final String PROP_COMPANIES = "Companies";
  public static final String PROP_PERSONS = "Persons";
  public static final String PROP_DOCUMENTS = "Documents";
  public static final String PROP_APPOINTMENTS = "Appointments";
  public static final String PROP_DISCUSSIONS = "Discussions";
  public static final String PROP_SERVICE_OBJECTS = "ServiceObjects";
  public static final String PROP_PROJECTS = "Projects";
  public static final String PROP_PROJECT_STAGES = "ProjectStages";
  public static final String PROP_TASKS = "Tasks";

  public static final String PROP_FILES = "Files";
  public static final String PROP_EVENTS = "Events";

  public static final String PROP_USER = "User";
  public static final String PROP_STAR = "Star";
  public static final String PROP_LAST_ACCESS = "LastAccess";
  public static final String PROP_LAST_PUBLISH = "LastPublish";

  public static final String PROP_LAST_EVENT_ID = "LastEventId";

  public static final String PROP_MAIL = "Mail";

  public static final String GRID_TASKS = "Tasks";
  public static final String GRID_TODO_LIST = "TodoList";

  public static final String GRID_TASKS_TYPE_HOURS_REPORT = "TasksTypeHoursReport";

  public static final String GRID_RECURRING_TASKS = "RecurringTasks";
  public static final String GRID_CHILD_RECURRING_TASKS = "ChildRecurringTasks";
  public static final String GRID_RT_FILES = "RTFiles";

  public static final String GRID_RELATED_TASKS = "RelatedTasks";
  public static final String GRID_CHILD_TASKS = "ChildTasks";
  public static final String GRID_CHILD_REQUESTS = "ChildRequests";
  public static final String GRID_RELATED_RECURRING_TASKS = "RelatedRecurringTasks";

  public static final String GRID_REQUESTS = "Requests";

  public static final String GRID_TASK_TYPES = "TaskTypes";
  public static final String GRID_TASK_TEMPLATES = "TaskTemplates";

  public static final String FORM_NEW_TASK = "NewTask";
  public static final String FORM_TASK = "Task";

  public static final String FORM_RECURRING_TASK = "RecurringTask";

  public static final String FORM_TASKS_REPORT = "TasksReport";

  public static final String FORM_NEW_REQUEST = "NewRequest";
  public static final String FORM_REQUEST = "Request";

  public static final String CRM_STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "crm-";

  public static final String FILTER_TASKS_NEW = "tasks_new";
  public static final String FILTER_TASKS_UPDATED = "tasks_updated";

  public static final String PRM_END_OF_WORK_DAY = "EndOfWorkDay";

  private TaskConstants() {
  }
}
