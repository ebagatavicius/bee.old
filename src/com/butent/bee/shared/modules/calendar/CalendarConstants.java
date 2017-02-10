package com.butent.bee.shared.modules.calendar;

import com.google.common.collect.ImmutableMap;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Map;

public final class CalendarConstants {

  public enum AppointmentStatus implements HasLocalizedCaption {
    TENTATIVE {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.calAppointmentStatusTentative();
      }
    },
    CONFIRMED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.calAppointmentStatusConfirmed();
      }
    },
    DELAYED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.calAppointmentStatusDelayed();
      }
    },
    CANCELED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.calAppointmentStatusCanceled();
      }
    },
    RUNNING {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.calAppointmentStatusRunning();
      }
    },
    COMPLETED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.calAppointmentStatusCompleted();
      }
    }
  }

  public enum ItemType {
    APPOINTMENT, TASK
  }

  public enum MultidayLayout implements HasCaption {
    HORIZONTAL(Localized.dictionary().calMultidayLayoutHorizontal()),
    VERTICAL(Localized.dictionary().calMultidayLayoutVertical()),
    WORKING_HOURS(Localized.dictionary().calMultidayLayoutWorkingHours()),
    LAST_DAY(Localized.dictionary().calMultidayLayoutLastDay());

    private final String caption;

    MultidayLayout(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public enum Report implements HasCaption {
    BUSY_MONTHS(Localized.dictionary().calReportTypeBusyMonths()),
    BUSY_HOURS(Localized.dictionary().calReportTypeBusyHours()),
    CANCEL_MONTHS(Localized.dictionary().calReportTypeCancelMonths()),
    CANCEL_HOURS(Localized.dictionary().calReportTypeCancelHours());

    private final String caption;

    Report(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public enum Transparency implements HasCaption {
    OPAQUE(Localized.dictionary().calOpaque()),
    TRANSPARENT(Localized.dictionary().calTransparent());

    public static boolean isOpaque(Integer value) {
      return value != null && value == OPAQUE.ordinal();
    }

    private final String caption;

    Transparency(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public enum CalendarVisibility implements HasCaption {
    PUBLIC(Localized.dictionary().calPublic()),
    PRIVATE(Localized.dictionary().calPrivate()),
    EDITABLE(Localized.dictionary().calEditable());

    private final String caption;

    CalendarVisibility(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public enum TimeBlockClick implements HasCaption {
    SINGLE, DOUBLE;

    @Override
    public String getCaption() {
      return Integer.toString(this.ordinal() + 1);
    }
  }

  public enum ViewType implements HasCaption {
    DAY("DayView", Localized.dictionary().calDayView()),
    DAYS("DaysView", Localized.dictionary().calDaysView()),
    WORK_WEEK("WorkWeekView", Localized.dictionary().calWorkWeekView()),
    WEEK("WeekView", Localized.dictionary().calWeekView()),
    MONTH("MonthView", Localized.dictionary().calMonthView()),
    RESOURCES("ResourceView", Localized.dictionary().calResourceView());

    private final String columnId;
    private final String caption;

    ViewType(String columnId, String caption) {
      this.columnId = columnId;
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }

    public String getCaption(int days) {
      if (this == DAYS) {
        return BeeUtils.toString(days)
            + ((days < 10) ? " " + Localized.dictionary().unitDays().toLowerCase() : " "
                + Localized.dictionary().unitDaysShort().toLowerCase());
      } else {
        return caption;
      }
    }

    public String getColumnId() {
      return columnId;
    }
  }

  public static void register() {
    EnumUtils.register(AppointmentStatus.class);
    EnumUtils.register(MultidayLayout.class);
    EnumUtils.register(TimeBlockClick.class);
    EnumUtils.register(Transparency.class);
    EnumUtils.register(CalendarVisibility.class);
  }

  public static final String SVC_GET_USER_CALENDAR = "get_user_calendar";
  public static final String SVC_CREATE_APPOINTMENT = "create_appointment";
  public static final String SVC_UPDATE_APPOINTMENT = "update_appointment";
  public static final String SVC_GET_CALENDAR_ITEMS = "get_calendar_items";
  public static final String SVC_SAVE_ACTIVE_VIEW = "save_active_view";
  public static final String SVC_GET_OVERLAPPING_APPOINTMENTS = "get_overlapping_appointments";
  public static final String SVC_GET_REPORT_OPTIONS = "get_report_options";
  public static final String SVC_DO_REPORT = "do_report";

  public static final String PARAM_CALENDAR_ID = "calendar_id";
  public static final String PARAM_USER_CALENDAR_ID = "user_calendar_id";
  public static final String PARAM_ACTIVE_VIEW = "active_view";
  public static final String PARAM_APPOINTMENT_ID = "appointment_id";
  public static final String PARAM_APPOINTMENT_START = "appointment_start";
  public static final String PARAM_APPOINTMENT_END = "appointment_end";
  public static final String PARAM_APPOINTMENT_SEPARATED = "appointment_separated";
  public static final String PARAM_ATTENDEES = "attendees";
  public static final String PARAM_REPORT = "report";
  public static final String PARAM_START_TIME = "start_time";
  public static final String PARAM_END_TIME = "end_time";

  public static final String TBL_USER_CALENDARS = "UserCalendars";
  public static final String TBL_USER_CAL_ATTENDEES = "UserCalAttendees";
  public static final String TBL_CONFIGURATION = "Configuration";

  public static final String TBL_APPOINTMENTS = "Appointments";
  public static final String TBL_APPOINTMENT_PROPS = "AppointmentProps";
  public static final String TBL_APPOINTMENT_ATTENDEES = "AppointmentAttendees";
  public static final String TBL_APPOINTMENT_OWNERS = "AppointmentOwners";
  public static final String TBL_APPOINTMENT_REMINDERS = "AppointmentReminders";
  public static final String TBL_APPOINTMENT_TYPES = "AppointmentTypes";

  public static final String TBL_ATTENDEES = "Attendees";

  public static final String TBL_REPORT_OPTIONS = "ReportOptions";

  public static final String TBL_CALENDAR_EXECUTORS = "CalendarExecutors";
  public static final String TBL_CAL_EXECUTOR_GROUPS = "CalExecutorGroups";

  public static final String VIEW_CALENDARS = "Calendars";
  public static final String VIEW_USER_CALENDARS = "UserCalendars";
  public static final String VIEW_USER_CAL_ATTENDEES = "UserCalAttendees";
  public static final String VIEW_CONFIGURATION = "Configuration";

  public static final String VIEW_PROPERTY_GROUPS = "PropertyGroups";
  public static final String VIEW_EXTENDED_PROPERTIES = "ExtendedProperties";

  public static final String VIEW_ATTENDEES = "Attendees";
  public static final String VIEW_ATTENDEE_PROPS = "AttendeeProps";
  public static final String VIEW_ATTENDEE_TYPES = "AttendeeTypes";

  public static final String VIEW_APPOINTMENTS = "Appointments";
  public static final String VIEW_APPOINTMENT_TYPES = "AppointmentTypes";

  public static final String VIEW_APPOINTMENT_ATTENDEES = "AppointmentAttendees";
  public static final String VIEW_APPOINTMENT_OWNERS = "AppointmentOwners";

  public static final String VIEW_APPOINTMENT_STYLES = "AppointmentStyles";

  public static final String VIEW_CAL_APPOINTMENT_TYPES = "CalAppointmentTypes";
  public static final String VIEW_CAL_ATTENDEE_TYPES = "CalAttendeeTypes";
  public static final String VIEW_CALENDAR_ATTENDEES = "CalendarAttendees";
  public static final String VIEW_CALENDAR_EXECUTORS = "CalendarExecutors";
  public static final String VIEW_CAL_EXECUTOR_GROUPS = "CalExecutorGroups";

  public static final String VIEW_REPORT_OPTIONS = "ReportOptions";

  public static final String FORM_APPOINTMENT = "Appointment";

  public static final String GRID_CALENDAR_EXECUTORS = "CalendarExecutors";
  public static final String GRID_CAL_EXECUTOR_GROUPS = "CalExecutorGroups";

  public static final String GRID_APPOINTMENTS = "Appointments";
  public static final String GRID_APPOINTMENT_ATTENDEES = "AppointmentAttendees";
  public static final String GRID_APPOINTMENT_OWNERS = "AppointmentOwners";
  public static final String GRID_APPOINTMENT_PROPS = "AppointmentProps";

  public static final String GRID_ATTENDEES = "Attendees";

  public static final String GRID_CALENDAR_TODO = "CalendarTodo";

  public static final String FORM_CALENDAR_SETTINGS = "CalendarSettings";

  public static final String DEFAULT_NEW_APPOINTMENT_FORM = "SimpleAppointment";
  public static final String DEFAULT_EDIT_APPOINTMENT_FORM = "SimpleAppointment";

  public static final String COL_CALENDAR = "Calendar";
  public static final String COL_USER_CALENDAR = "UserCalendar";

  public static final String COL_CALENDAR_NAME = "Name";
  public static final String COL_CALENDAR_OWNER = "Owner";
  public static final String COL_CALENDAR_IS_SERVICE = "IsService";

  public static final String COL_DEFAULT_DISPLAYED_DAYS = "DefaultDisplayedDays";

  public static final String COL_PIXELS_PER_INTERVAL = "PixelsPerInterval";
  public static final String COL_INTERVALS_PER_HOUR = "IntervalsPerHour";

  public static final String COL_WORKING_HOUR_START = "WorkingHourStart";
  public static final String COL_WORKING_HOUR_END = "WorkingHourEnd";
  public static final String COL_SCROLL_TO_HOUR = "ScrollToHour";

  public static final String COL_TIME_BLOCK_CLICK_NUMBER = "TimeBlockClickNumber";

  public static final String COL_SEPARATE_ATTENDEES = "SeparateAttendees";

  public static final String COL_MULTIDAY_LAYOUT = "MultidayLayout";
  public static final String COL_MULTIDAY_TASK_LAYOUT = "MultidayTaskLayout";

  public static final String ALS_COMPANY_PHONE = "CompanyPhone";
  public static final String ALS_COMPANY_EMAIL = "CompanyEmail";

  public static final String COL_APPOINTMENT_LOCATION = "Location";

  public static final String COL_APPOINTMENT = "Appointment";
  public static final String COL_APPOINTMENT_TYPE = "AppointmentType";

  public static final String COL_APPOINTMENT_TYPE_NAME = "Name";
  public static final String COL_APPOINTMENT_TYPE_DURATION = "PlannedDuration";

  public static final String COL_ATTENDEE = "Attendee";
  public static final String COL_ATTENDEE_NAME = "Name";
  public static final String COL_ATTENDEE_TYPE = "AttendeeType";

  public static final String ALS_ATTENDEE_NAME = "AttendeeName";
  public static final String ALS_ATTENDEE_ORDINAL = "AttendeeOrdinal";
  public static final String ALS_ATTENDEE_COLOR = "AttendeeColor";
  public static final String ALS_ATTENDEE_BACKGROUND = "AttendeeBackground";
  public static final String ALS_ATTENDEE_FOREGROUND = "AttendeeForeground";

  public static final String COL_ATTENDEE_TYPE_NAME = "Name";
  public static final String ALS_ATTENDEE_TYPE_NAME = "TypeName";
  public static final String ALS_APPOINTMENT_TYPE_NAME = "TypeName";

  public static final String COL_PROPERTY_NAME = "Name";
  public static final String COL_PROPERTY_GROUP = "PropertyGroup";
  public static final String COL_DEFAULT_PROPERTY = "DefaultProperty";

  public static final String ALS_PERSON_FIRST_NAME = "PersonFirstName";
  public static final String ALS_PERSON_LAST_NAME = "PersonLastName";
  public static final String ALS_PROPERTY_NAME = "PropertyName";
  public static final String ALS_PROPERTY_GROUP_NAME = "GroupName";

  public static final String COL_ATTENDEE_PROPERTY = "Property";
  public static final String COL_APPOINTMENT_PROPERTY = "Property";

  public static final String COL_HOURS = "Hours";
  public static final String COL_MINUTES = "Minutes";
  public static final String COL_SCHEDULED = "Scheduled";

  public static final String COL_ALIAS_DEF_HOURS = "defHours";
  public static final String COL_ALIAS_DEL_MINUTES = "defMinutes";

  public static final String COL_MESSAGE = "Message";
  public static final String COL_SENT = "Sent";
  public static final String COL_ERROR = "Error";
  public static final String COL_RECIPIENT = "Recipient";

  public static final String COL_START_DATE_TIME = "StartDateTime";
  public static final String COL_END_DATE_TIME = "EndDateTime";

  public static final String COL_VEHICLE = "Vehicle";
  public static final String COL_VEHICLE_OWNER = "VehicleOwner";
  public static final String COL_VEHICLE_NUMBER = "VehicleNumber";
  public static final String COL_VEHICLE_PARENT_MODEL = "VehicleParentModel";
  public static final String COL_VEHICLE_MODEL = "VehicleModel";

  public static final String COL_SUMMARY = "Summary";
  public static final String COL_DESCRIPTION = "Description";
  public static final String COL_ACTION_RESULT = "ActionResult";
  public static final String COL_ACTION_REMINDED = "ActionReminded";

  public static final String COL_REMINDER_TYPE = "ReminderType";

  public static final String COL_STATUS = "Status";

  public static final String ALS_OWNER_FIRST_NAME = "OwnerFirstName";
  public static final String ALS_OWNER_LAST_NAME = "OwnerLastName";
  public static final String ALS_OWNER_EMAIL = "OwnerEmail";

  public static final String COL_SIMPLE_HEADER = "SimpleHeader";
  public static final String COL_SIMPLE_BODY = "SimpleBody";
  public static final String COL_MULTI_HEADER = "MultiHeader";
  public static final String COL_MULTI_BODY = "MultiBody";

  public static final String COL_APPOINTMENT_COMPACT = "AppointmentCompact";
  public static final String COL_APPOINTMENT_TITLE = "AppointmentTitle";

  public static final String COL_STYLE = "Style";

  public static final String COL_SIMPLE = "Simple";
  public static final String COL_MULTI = "Multi";
  public static final String COL_COMPACT = "Compact";

  public static final String COL_HEADER = "Header";
  public static final String COL_BODY = "Body";
  public static final String COL_FOOTER = "Footer";

  public static final String COL_ACTIVE_VIEW = "ActiveView";

  public static final String COL_TRANSPARENCY = "Transparency";
  public static final String ALS_TYPE_TRANSPARENCY = "TypeTransparency";

  public static final String COL_VISIBILITY = "Visibility";
  public static final String COL_CREATED = "Created";
  public static final String COL_CREATOR = "Creator";
  public static final String ALS_CREATOR_COMPANY_PERSON = "CreatorCompanyPerson";
  public static final String ALS_CREATOR_FIRST_NAME = "CreatorFirstName";
  public static final String ALS_CREATOR_LAST_NAME = "CreatorLastName";

  public static final String COL_REPORT = "Report";
  public static final String COL_CAPTION = "Caption";
  public static final String COL_LOWER_DATE = "LowerDate";
  public static final String COL_UPPER_DATE = "UpperDate";
  public static final String COL_LOWER_HOUR = "LowerHour";
  public static final String COL_UPPER_HOUR = "UpperHour";
  public static final String COL_ATTENDEE_TYPES = "AttendeeTypes";
  public static final String COL_ATTENDEES = "Attendees";

  public static final String COL_APPOINTMENTS_COUNT = "AppointmentsCount";
  public static final String COL_APPOINTMENT_CREATOR = "AppointmentCreator";
  public static final String COL_APPOINTMENT_EDITOR = "AppointmentEditor";

  public static final String COL_ENABLED = "Enabled";
  public static final String COL_ORDINAL = "Ordinal";

  public static final String COL_ASSIGNED_TASKS = "AssignedTasks";
  public static final String COL_ASSIGNED_TASKS_BACKGROUND = "AssignedTasksBackground";
  public static final String COL_ASSIGNED_TASKS_FOREGROUND = "AssignedTasksForeground";
  public static final String COL_ASSIGNED_TASKS_STYLE = "AssignedTasksStyle";

  public static final String COL_DELEGATED_TASKS = "DelegatedTasks";
  public static final String COL_DELEGATED_TASKS_BACKGROUND = "DelegatedTasksBackground";
  public static final String COL_DELEGATED_TASKS_FOREGROUND = "DelegatedTasksForeground";
  public static final String COL_DELEGATED_TASKS_STYLE = "DelegatedTasksStyle";

  public static final String COL_OBSERVED_TASKS = "ObservedTasks";
  public static final String COL_OBSERVED_TASKS_BACKGROUND = "ObservedTasksBackground";
  public static final String COL_OBSERVED_TASKS_FOREGROUND = "ObservedTasksForeground";
  public static final String COL_OBSERVED_TASKS_STYLE = "ObservedTasksStyle";

  public static final String COL_EXECUTOR_USER = "User";
  public static final String COL_EXECUTOR_GROUP = "Group";

  public static final String COL_APPOINTMENT_OWNER = "Owner";

  public static final Map<String, String> APPOINTMENT_CHILDREN =
      ImmutableMap.of(TBL_APPOINTMENT_ATTENDEES, COL_ATTENDEE,
          TBL_APPOINTMENT_OWNERS, COL_APPOINTMENT_OWNER,
          TBL_APPOINTMENT_PROPS, COL_APPOINTMENT_PROPERTY,
          TBL_APPOINTMENT_REMINDERS, COL_REMINDER_TYPE);

  public static final String NAME_START = "Start";
  public static final String NAME_END = "End";

  public static final String PRM_REMINDER_TIME_FROM = "ReminderTimeFrom";
  public static final String PRM_REMINDER_TIME_UNTIL = "ReminderTimeUntil";

  public static final JustDate MIN_DATE = new JustDate(2010, 1, 1);
  public static final JustDate MAX_DATE = TimeUtils.endOfMonth(TimeUtils.today(), 12);

  private CalendarConstants() {
  }
}
