package com.butent.bee.shared.modules.projects;

import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.EnumUtils;

/**
 * Constants of Projects module.
 */
public final class ProjectConstants {

  public enum ProjectEvent implements HasCaption {
    COMMENT(Localized.dictionary().comment(), Localized.dictionary().crmActionComment(),
        FontAwesome.COMMENT_O),
    EDIT(Localized.dictionary().prjEventEdited(), null, null);

    private final String caption;
    private final String commandLabel;
    private final FontAwesome commandIcon;

    ProjectEvent(String caption, String commandLabel, FontAwesome commandIcon) {
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

  public static final String ALS_PROJECT_START_DATE = "ProjectStartDate";
  public static final String ALS_PROJECT_END_DATE = "ProjectEndDate";
  public static final String ALS_PROJECT_COMPANY_NAME = "CompanyName";
  public static final String ALS_STAGE_NAME = "StageName";
  public static final String ALS_STAGE_START = "StageStart";
  public static final String ALS_STAGE_END = "StageEnd";
  public static final String ALS_CHART_FLOW_COLOR = "ChartFlowColor";
  public static final String ALS_VIEW_NAME = "ViewName";
  public static final String ALS_TASK_STATUS = "TaskStatus";
  public static final String ALS_OWNER_FIRST_NAME = "OwnerFirstName";
  public static final String ALS_OWNER_LAST_NAME = "OwnerLastName";
  public static final String ALS_OWNER_PERSON = "OwnerPerson";
  public static final String ALS_TERM = "Term";
  public static final String ALS_PROFIT = "Profit";
  public static final String ALS_STAGES_COUNT = "StagesCount";
  public static final String ALS_PROJECT_NAME = "ProjectName";
  public static final String ALS_PROJECT_STATUS = "ProjectStatus";

  public static final String ALS_CHART_START = "ChartStart";
  public static final String ALS_CHART_END = "ChartEnd";
  public static final String ALS_CHART_CAPTION = "ChartCaption";
  public static final String ALS_CHART_ID = "ChartID";

  public static final String ALS_PUBLISHER_FIRST_NAME = "PublisherFirstName";
  public static final String ALS_PUBLISHER_LAST_NAME = "PublisherLastName";
  public static final String ALS_FILTERED_PROJECT_USER = "FilteredProjectUser";
  public static final String ALS_FILTERED_OWNER_USER = "FilteredOwnerUser";
  public static final String ALS_FILTERED_VISITED_USER = "FilteredVisitedUser";
  public static final String ALS_ACTUAL_TIME_DIFFERENCE = "ActualTimeDifference";
  public static final String ALS_CATEGORY_NAME = "CategoryName";
  public static final String ALS_EXPECTED_TASKS_EXPENSES = "ExpectedTasksExpenses";
  public static final String ALS_ACTUAL_TASKS_EXPENSES = "ActualExpenses";
  public static final String ALS_COMPANY_TYPE_NAME = "CompanyTypeName";

  public static final String ALS_ROW_ID = "RowID";
  public static final String ALS_TASK_COUNT = "TaskCount";
  public static final String ALS_PROJECT_OVERDUE = "Overdue";
  public static final String ALS_LOW_TASKS_DATE = "LowTasksDate";
  public static final String ALS_HIGH_TASKS_DATE = "HighTasksDate";

  public static final String COL_EXPECTED_TASKS_DURATION = "ExpectedTasksDuration";
  public static final String COL_ACTUAL_TASKS_DURATION = "ActualTasksDuration";

  public static final String COL_PROJECT = "Project";
  public static final String COL_PROJECT_ID = "ProjectID";
  public static final String COL_PROJECT_NAME = "Name";
  public static final String COL_PROJECT_END_DATE = "EndDate";
  public static final String COL_PROJECT_APPROVED_DATE = "ApprovedDate";
  public static final String COL_PROJECT_STAGE = "ProjectStage";
  public static final String COL_PROJECT_START_DATE = "StartDate";
  public static final String COL_PROJECT_STATUS = "Status";
  public static final String COL_PROJECT_PRIORITY = "Priority";
  public static final String COL_PROJECT_OWNER = "Owner";
  public static final String COL_PROJECT_CATEGORY = "Category";
  public static final String COL_PROJECT_PRICE = "Price";
  public static final String COL_PROJECT_ITEM_PRICE = "Price";
  public static final String COL_PROJECT_CURENCY = "Currency";
  public static final String COL_PROJECT_INCOME_CURENCY = "Currency";
  public static final String COL_NOTES = "Notes";

  public static final String COL_DEFAULT_PROJECT_TEMPLATE_STAGE = "DefaultTMLStage";

  public static final String COL_INCOME_NOTE = "Note";
  public static final String COL_INCOME_DATE = "Date";
  public static final String COL_INCOME_ITEM = "Item";
  public static final String COL_INCOME_SALE = "Sale";

  public static final String COL_PUBLISHER = "Publisher";
  public static final String COL_PUBLISH_TIME = "PublishTime";
  public static final String COL_PROJECT_EVENT = "ProjectEvent";
  public static final String COL_PROJECT_TYPE = "Type";
  public static final String COL_PROGRESS = "Progress";
  public static final String COL_COMMENT = "Comment";
  public static final String COL_EXPENSES = "Expenses";
  public static final String COL_EVENT = "Event";
  public static final String COL_EVENT_PROPERTIES = "Properties";
  public static final String COL_CAPTION = "Caption";
  public static final String COL_COMAPNY = "Company";
  public static final String COL_EXPECTED_DURATION = "ExpectedDuration";
  public static final String COL_CONTRACT_PRICE = "ContractPrice";
  public static final String COL_PROJECT_TIME_UNIT = "TimeUnit";
  public static final String COL_DESCRIPTION = "Description";

  public static final String COL_OVERDUE = "Overdue";
  public static final String COL_PROJECT_TEMPLATE = "ProjectTemplate";

  public static final String COL_PSC_FOOTER_HEIGHT = "PSCFooterHeight";
  public static final String COL_PSC_HEADER_HEIGHT = "PSCHeaderHeight";
  public static final String COL_PSC_ROW_HEIGHT = "PSCRowHeight";
  public static final String COL_PSC_STRIP_OPACITY = "PSCStripOpacity";

  public static final String COL_ACCESS = "Access";

  public static final String COL_STAGE_NAME = "Name";
  public static final String COL_STAGE_START_DATE = "StartDate";
  public static final String COL_STAGE_END_DATE = "EndDate";
  public static final String COL_STAGE_TEMPLATE = "StageTemplate";

  public static final String COL_DATES_START_DATE = "StartDate";
  public static final String COL_DATES_END_DATE = "EndDate";
  public static final String COL_DATES_COLOR = "Color";
  public static final String COL_DATES_NOTE = "Note";
  public static final String COL_TREE_NAME = "Name";

  public static final String COL_DOCUMENT_REQUIRED = "DocumentRequired";

  public static final String COL_USER_TYPE = "UserType";
  public static final String COL_RATE = "Rate";

  public static final String FILTER_OVERDUE_CREATION = "overdue_creation";
  public static final String FILTERSUPPLIER_SLACK_PREFIX = "prj-FilterSupplier-Slack-";

  public static final String FORM_PROJECT = "Project";
  public static final String FORM_PROJECT_STAGE = "ProjectStage";
  public static final String FORM_NEW_COMMENT = "NewProjectComment";
  public static final String FORM_NEW_PROJECT_REASON_COMMENT = "NewProjectReasonComment";
  public static final String FORM_NEW_PROJECT_INVOICE = "NewProjectInvoice";

  public static final String FORM_PROJECT_TEMPLATE = "ProjectTemplate";
  public static final String FORM_NEW_PROJECT_FROM_TEMPLATE = "NewProjectFromTemplate";
  public static final String FORM_NEW_PROJECT_FROM_TASK = "NewProjectFromTask";

  public static final String FORM_PROJECT_DATE = "ProjectDate";

  public static final String GRID_ALL_PROJECTS = "AllProjects";
  public static final String GRID_PROJECTS = "Projects";
  public static final String GRID_PROJECT_USERS = "ProjectUsers";
  public static final String GRID_PROJECT_STAGES = "ProjectStages";
  public static final String GRID_PROJECT_INCOMES = "ProjectIncomes";
  public static final String GRID_PROJECT_INVOICES = "ProjectInvoices";
  public static final String GRID_PROJECT_DATES = "ProjectDates";
  public static final String GRID_PROJECT_APPOINTMENTS = "ProjectAppointments";

  public static final String GRID_PROJECT_TEMPLATE_STAGES = "ProjectTMLStages";
  public static final String GRID_PROJECT_TEMPLATES = "ProjectTemplates";

  public static final String NAME_MODE = "Mode";
  public static final String NAME_SLACK = "Slack";
  public static final String NAME_EXPECTED_TASKS_DURATION = "CalcExpectedTasksDuration";
  public static final String NAME_ACTUAL_TASKS_DURATION = "CalcActualTasksDuration";

  public static final String PROP_USERS = "Users";
  public static final String PROP_TEMPLATE = "Template";
  public static final String PROP_REASON = "Reason";
  public static final String PROP_DOCUMENT = "Document";
  public static final String PROP_DOCUMENT_LINK = "DocumentLink";
  public static final String PROP_REASON_DATA = "ReasonData";
  public static final String PROP_REAL_FACTOR = "RealFactor";
  public static final String PROP_TIME_UNITS = "TimeUnits";
  public static final String PROP_ITEM_PRICES = "ItemPrices";
  public static final String PROP_SELECT = "Select";
  public static final String PROP_RS = "RowSet";

  public static final String PROP_LAST_ACCESS = "LastAccess";
  public static final String PROP_LAST_PUBLISH = "LastPublish";

  public static final String PRM_PROJECT_COMMON_RATE = "ProjectCommonRate";
  public static final String PRM_PROJECT_HOUR_UNIT = "ProjectHourUnit";

  public static final String SVC_PREFFIX = "svc_prj_";
  public static final String SVC_GET_PROJECT_CHART_DATA = SVC_PREFFIX + "GetChartData";
  public static final String SVC_GET_TIME_UNITS = SVC_PREFFIX + "GetTimeUnits";
  public static final String SVC_PROJECT_REPORT = "ProjectReport";
  public static final String SVC_CREATE_INVOICE_ITEMS = "CreateInvoiceItems";

  public static final String STYLE_CONTAINER = "container";
  public static final String STYLE_LATE = "late";
  public static final String STYLE_NOT_LATE = "notLate";
  public static final String STYLE_ALL = "all";
  public static final String STYLE_CANCEL = "cancel";

  public static final String VALUE_LATE = "late";
  public static final String VALUE_SCHEDULED = "scheduled";
  public static final String VAR_PROJECT = "Project";

  public static final String VIEW_PROJECTS = "Projects";
  public static final String VIEW_PROJECT_USERS = "ProjectUsers";
  public static final String VIEW_PROJECT_TEMPLATE_USERS = "ProjectTMLUsers";
  public static final String VIEW_PROJECT_TEMPLATE_CONTACTS = "ProjectTMLContacts";
  public static final String VIEW_PROJECT_STAGES = "ProjectStages";
  public static final String VIEW_PROJECT_DATES = "ProjectDates";
  public static final String VIEW_PROJECT_FILES = "ProjectFiles";
  public static final String VIEW_PROJECT_CONTACTS = "ProjectContacts";
  public static final String VIEW_PROJECT_EVENTS = "ProjectEvents";
  public static final String VIEW_PROJECT_INCOMES = "ProjectIncomes";
  public static final String VIEW_PROJECT_INVOICES = "ProjectInvoices";
  public static final String VIEW_PROJECT_TREE = "ProjectTree";

  public static final String VIEW_PROJECT_TEMPLATES = "ProjectTemplates";
  public static final String VIEW_PROJECT_TEMPLATE_STAGES = "ProjectTMLStages";
  public static final String VIEW_PROJECT_TEMPLATE_DATES = "ProjectTMLDates";
  public static final String VIEW_PROJECT_TEMPLATE_TASK_COPY = "ProjectTMLTaskCopy";

  public static final String TBL_PROJECT_USAGE = "ProjectUsage";
  public static final String TBL_PROJECTS = "Projects";
  public static final String TBL_PROJECT_STAGES = "ProjectStages";
  public static final String TBL_PROJECT_EVENTS = "ProjectEvents";
  public static final String TBL_PROJECT_INCOMES = "ProjectIncomes";
  public static final String TBL_PROJECT_USERS = "ProjectUsers";


  public static final String PROJECT_STATUS_STYLE = "bee-header-caption_state";
  public static final String PROJECT_STATUS_STYLE_ACTIVE = PROJECT_STATUS_STYLE + "_active";
  public static final String PROJECT_STATUS_STYLE_SUSPENDED = PROJECT_STATUS_STYLE + "_suspended";
  public static final String PROJECT_STATUS_STYLE_WITH_BORDER = "_with_border";

  /**
   * Register module Enumerations.
   */
  public static void register() {
    EnumUtils.register(ProjectPriority.class);
    EnumUtils.register(ProjectStatus.class);
    EnumUtils.register(ProjectEvent.class);
  }

  private ProjectConstants() {
  }
}
