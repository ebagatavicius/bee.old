package com.butent.bee.shared.modules.payroll;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.EnumUtils;

public final class PayrollConstants {

  public enum ObjectStatus implements HasLocalizedCaption {
    INACTIVE {
      @Override
      public String getCaption(Dictionary dictionary) {
        return dictionary.objectStatusInactive();
      }
    },
    ACTIVE {
      @Override
      public String getCaption(Dictionary dictionary) {
        return dictionary.objectStatusActive();
      }
    }
  }

  /**
   * Duration types of time card. Time card duration type sets time counting conditions in payroll
   * accounting. There are some time duration types for accounting can be used depend of
   * work accounting rules. The usually using types are full-time work, part-time work or
   * absences from work.
   */
  public enum TcDurationType implements HasLocalizedCaption {

    /**
     * Duration type of Time card to account payroll with full-time (all day) work.
     */
    FULL_TIME {
      @Override
      public String getCaption(Dictionary dictionary) {
        return dictionary.payrollFullTime();
      }
    },
    /**
     * Duration type of Time card to account payroll with specific work time or other part-time
     * value.
     */
    PART_TIME {
      @Override
      public String getCaption(Dictionary dictionary) {
        return dictionary.payrollPartTime();
      }
    },

    /**
     * Duration type of Time card to account payroll with absence from work.
     */
    ABSENCE {
      @Override
      public String getCaption(Dictionary dictionary) {
        return dictionary.payrollAbsence();
      }
    };

//    TODO: Refactor this item. Required detailed technical spec.
    public static String getDefaultTimeOfDay() {
      return String.valueOf(BeeConst.CHAR_EIGHT);
    }
  }

  public enum WorkScheduleKind implements HasLocalizedCaption {
    PLANNED {
      @Override
      public String getCaption(Dictionary dictionary) {
        return dictionary.workSchedulePlannedShort();
      }

      @Override
      public String getClearDataQuestion(Dictionary dictionary) {
        return dictionary.clearWorkScheduleQuestion();
      }

      @Override
      public String getStorageKeyPrefix() {
        return "WorkSchedule";
      }

      @Override
      public String getTccColumnName() {
        return COL_TC_WS_PLANNED;
      }

      @Override
      public boolean isExtensionEnabled() {
        return true;
      }

      @Override
      public boolean isSubstitutionEnabled() {
        return false;
      }
    },

    ACTUAL {
      @Override
      public String getCaption(Dictionary dictionary) {
        return dictionary.workScheduleActualShort();
      }

      @Override
      public String getEditFormName() {
        return FORM_ACTUAL_WORK_SCHEDULE_EDITOR;
      }

      @Override
      public String getClearDataQuestion(Dictionary dictionary) {
        return dictionary.clearTimeSheetQuestion();
      }

      @Override
      public String getStorageKeyPrefix() {
        return "TimeSheet";
      }

      @Override
      public String getTccColumnName() {
        return COL_TC_WS_ACTUAL;
      }

      @Override
      public boolean isExtensionEnabled() {
        return false;
      }

      @Override
      public boolean isSubstitutionEnabled() {
        return true;
      }
    };

    public abstract String getClearDataQuestion(Dictionary dictionary);

    /**
     * Returns default edit form name for Work schedule editor.
     * @return the edit form name for Work schedule editor.
     */
    public String getEditFormName() {
      return FORM_WORK_SCHEDULE_EDITOR;
    }

    public abstract String getStorageKeyPrefix();

    public String getStyleSuffix() {
      return name().toLowerCase();
    }

    public abstract String getTccColumnName();

    public abstract boolean isExtensionEnabled();

    public abstract boolean isSubstitutionEnabled();
  }

  public static final String SVC_GET_SCHEDULE_OVERLAP = "getScheduleOverlap";
  public static final String SVC_GET_SCHEDULED_MONTHS = "getScheduledMonths";
  public static final String SVC_GET_EARNINGS = "getEarnings";
  public static final String SVC_PAYROLL_FUND_REPORT = "PayrollFundReport";

  public static final String TBL_EMPLOYEES = "Employees";
  public static final String TBL_LOCATIONS = "Locations";
  public static final String TBL_EMPLOYEE_OBJECTS = "EmployeeObjects";

  public static final String TBL_WORK_SCHEDULE = "WorkSchedule";

  public static final String TBL_OBJECT_SALARY_FUND = "ObjectSalaryFund";

  public static final String TBL_TIME_CARD_CODES = "TimeCardCodes";
  public static final String TBL_TIME_RANGES = "TimeRanges";

  public static final String VIEW_EMPLOYEES = "Employees";
  public static final String VIEW_LOCATIONS = "Locations";
  public static final String VIEW_EMPLOYEE_OBJECTS = "EmployeeObjects";

  public static final String VIEW_WORK_SCHEDULE = "WorkSchedule";

  public static final String VIEW_TIME_CARD_CHANGES = "TimeCardChanges";

  public static final String VIEW_TIME_CARD_CODES = "TimeCardCodes";
  public static final String VIEW_TIME_RANGES = "TimeRanges";
  public static final String VIEW_WORK_SCHEDULE_LOCKS = "WorkScheduleLocks";

  public static final String COL_TAB_NUMBER = "TabNumber";
  public static final String COL_SALARY = "Salary";
  public static final String COL_PART_TIME = "PartTime";

  public static final String COL_LOCATION_NAME = "LocationName";
  public static final String COL_LOCATION_MANAGER = "Manager";
  public static final String COL_LOCATION_STATUS = "Status";

  public static final String COL_EMPLOYEE = "Employee";
  public static final String COL_PAYROLL_OBJECT = "Object";

  public static final String COL_EMPLOYEE_OBJECT_FROM = "DateFrom";
  public static final String COL_EMPLOYEE_OBJECT_UNTIL = "DateUntil";
  public static final String COL_EMPLOYEE_OBJECT_FUND = "Fund";
  public static final String COL_EMPLOYEE_OBJECT_NOTE = "Note";
  public static final String COL_WOKR_SCHEDULE_LOCK = "Lock";

  public static final String COL_SUBSTITUTE_FOR = "SubstituteFor";
  public static final String COL_WAGE = "Wage";

  public static final String COL_TIME_CARD_CHANGES_FROM = "DateFrom";
  public static final String COL_TIME_CARD_CHANGES_UNTIL = "DateUntil";

  public static final String COL_DATE_OF_EMPLOYMENT = "DateOfEmployment";
  public static final String COL_DATE_OF_DISMISSAL = "DateOfDismissal";
  public static final String COL_DEPARTMENT = "Department";

  public static final String COL_WORK_SCHEDULE_KIND = "Kind";
  public static final String COL_WORK_SCHEDULE_DATE = "Date";
  public static final String COL_WORK_SCHEDULE_FROM = "TimeFrom";
  public static final String COL_WORK_SCHEDULE_UNTIL = "TimeUntil";
  public static final String COL_WORK_SCHEDULE_DURATION = "Duration";
  public static final String COL_WORK_SCHEDULE_NOTE = "Note";

  public static final String COL_TIME_RANGE_CODE = "TimeRangeCode";
  public static final String COL_TIME_CARD_CODE = "TimeCardCode";

  public static final String COL_TC_CODE = "TcCode";
  public static final String COL_TC_NAME = "TcName";
  public static final String COL_TC_WS_PLANNED = "WsPlanned";
  public static final String COL_TC_WS_ACTUAL = "WsActual";
  public static final String COL_TC_DESCRIPTION = "Description";

  public static final String COL_TC_DURATION_TYPE = "TcDurationType";

  public static final String COL_TR_CODE = "TrCode";
  public static final String COL_TR_NAME = "TrName";
  public static final String COL_TR_FROM = "TimeFrom";
  public static final String COL_TR_UNTIL = "TimeUntil";
  public static final String COL_TR_DURATION = "Duration";
  public static final String COL_TR_DESCRIPTION = "Description";

  public static final String COL_OSF_YEAR_FROM = "YearFrom";
  public static final String COL_OSF_MONTH_FROM = "MonthFrom";
  public static final String COL_OSF_YEAR_UNTIL = "YearUntil";
  public static final String COL_OSF_MONTH_UNTIL = "MonthUntil";

  public static final String COL_OSF_AMOUNT = "Amount";

  public static final String ALS_DEPARTMENT_NAME = "DepartmentName";

  public static final String ALS_LOCATION_MANAGER_FIRST_NAME = "ManagerFirstName";
  public static final String ALS_LOCATION_MANAGER_LAST_NAME = "ManagerLastName";

  public static final String ALS_REPORT_TIME_PERIOD = "ReportTimePeriod";

  public static final String ALS_TC_BACKGROUND = "TcBackground";
  public static final String ALS_TC_FOREGROUND = "TcForeground";

  public static final String ALS_TR_BACKGROUND = "TrBackground";
  public static final String ALS_TR_FOREGROUND = "TrForeground";

  public static final String ALS_TR_FROM = "TrTimeFrom";
  public static final String ALS_TR_UNTIL = "TrTimeUntil";
  public static final String ALS_TR_DURATION = "TrDuration";

  public static final String ALS_TR_USAGE = "TrUsage";

  public static final String PROP_TC_DURATION = "TcDuration";

  public static final String GRID_TIME_CARD_CHANGES = "TimeCardChanges";

  public static final String GRID_TIME_CARD_CODES = "TimeCardCodes";
  public static final String GRID_TIME_RANGES = "TimeRanges";
  public static final String GRID_WORK_SCHEDULE_DAY = "WorkScheduleDay";

  public static final String FORM_LOCATION = "Location";
  public static final String FORM_EMPLOYEE = "Employee";
  public static final String FORM_WORK_SCHEDULE = "WorkSchedule";
  public static final String FORM_TIME_SHEET = "TimeSheet";
  public static final String FORM_WORK_SCHEDULE_EDITOR = "WorkScheduleEditor";
  public static final String FORM_ACTUAL_WORK_SCHEDULE_EDITOR = "ActualWorkScheduleEditor";
  public static final String FORM_NEW_SUBSTITUTION = "NewSubstitution";

  public static final String PRM_ERP_SYNC_PAYROLL_DATA = "ERPSyncPayrollData";
  public static final String PRM_ERP_SYNC_PAYROLL_DELTA_HOURS = "ERPSyncPayrollDeltaHours";
  public static final String PRM_ERP_SYNC_EMPLOYEES = "ERPSyncEmployees";
  public static final String PRM_ERP_SYNC_LOCATIONS = "ERPSyncLocations";
  public static final String PRM_ERP_SYNC_TIME_CARDS = "ERPSyncTimeCards";


  public static final String SFX_HOURS = "Hours";
  public static final String SFX_TIME = "Time";

  public static void register() {
    EnumUtils.register(ObjectStatus.class);
    EnumUtils.register(WorkScheduleKind.class);
    EnumUtils.register(TcDurationType.class);
  }

  private PayrollConstants() {
  }
}
