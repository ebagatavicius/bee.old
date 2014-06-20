package com.butent.bee.shared.modules.administration;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.modules.ParameterType;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.RightsObjectType;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.time.ScheduleDateMode;
import com.butent.bee.shared.time.WorkdayTransition;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;

public final class AdministrationConstants {

  public static final List<BeeColumn> HISTORY_COLUMNS = Lists.newArrayList(
      new BeeColumn(ValueType.DATE_TIME, AdministrationConstants.AUDIT_FLD_TIME, false),
      new BeeColumn(ValueType.TEXT, AdministrationConstants.COL_USER, true),
      new BeeColumn(ValueType.LONG, AdministrationConstants.AUDIT_FLD_TX, false),
      new BeeColumn(ValueType.TEXT, AdministrationConstants.AUDIT_FLD_MODE, false),
      new BeeColumn(ValueType.TEXT, AdministrationConstants.COL_OBJECT, false),
      new BeeColumn(ValueType.LONG, AdministrationConstants.AUDIT_FLD_ID, false),
      new BeeColumn(ValueType.TEXT, AdministrationConstants.AUDIT_FLD_FIELD, false),
      new BeeColumn(ValueType.TEXT, AdministrationConstants.AUDIT_FLD_VALUE, true),
      new BeeColumn(ValueType.TEXT, AdministrationConstants.COL_RELATION, true));

  public enum ReminderMethod implements HasCaption {
    EMAIL, SMS;

    @Override
    public String getCaption() {
      return this.name().toLowerCase();
    }
  }

  public enum UserGroupVisibility implements HasCaption {
    PRIVATE(Localized.getConstants().userGroupPrivate()),
    PUBLIC(Localized.getConstants().userGroupPublic());

    private final String caption;

    private UserGroupVisibility(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public static void register() {
    EnumUtils.register(Feed.class);
    EnumUtils.register(ParameterType.class);
    EnumUtils.register(ReminderMethod.class);
    EnumUtils.register(RightsObjectType.class);
    EnumUtils.register(RightsState.class);
    EnumUtils.register(SupportedLocale.class);
    EnumUtils.register(UserGroupVisibility.class);
    EnumUtils.register(UserInterface.class);

    EnumUtils.register(ScheduleDateMode.class);
    EnumUtils.register(WorkdayTransition.class);
  }

  public static final String METHOD = "Service";

  public static final String PARAMETERS_PREFIX = "parameters_";

  public static final String SVC_GET_PARAMETER = PARAMETERS_PREFIX + "parameter";
  public static final String SVC_GET_PARAMETERS = PARAMETERS_PREFIX + "get";
  public static final String SVC_RESET_PARAMETER = PARAMETERS_PREFIX + "reset";
  public static final String SVC_SET_PARAMETER = PARAMETERS_PREFIX + "set";

  public static final String SVC_NUMBER_TO_WORDS = "GetNumberInWords";

  public static final String VAR_AMOUNT = Service.RPC_VAR_PREFIX + "amount";
  public static final String VAR_LOCALE = Service.RPC_VAR_PREFIX + "locale";

  public static final String VAR_PARAMETERS_MODULE = Service.RPC_VAR_PREFIX + "module";
  public static final String VAR_PARAMETER = Service.RPC_VAR_PREFIX + "parameters";
  public static final String VAR_PARAMETER_VALUE = Service.RPC_VAR_PREFIX + "value";

  public static final String SVC_GET_HISTORY = "get_history";
  public static final String VAR_HISTORY_VIEW = Service.RPC_VAR_PREFIX + "history_view";
  public static final String VAR_HISTORY_IDS = Service.RPC_VAR_PREFIX + "history_ids";

  public static final String VAR_DATE_LOW = Service.RPC_VAR_PREFIX + "date_low";
  public static final String VAR_DATE_HIGH = Service.RPC_VAR_PREFIX + "date_high";

  public static final String VAR_BASE_ROLE = Service.RPC_VAR_PREFIX + "base_role";

  public static final String SVC_GET_CURRENT_EXCHANGE_RATE = "get_current_exchange_rate";
  public static final String SVC_GET_EXCHANGE_RATE = "get_exchange_rate";
  public static final String SVC_GET_LIST_OF_CURRENCIES = "get_list_of_currencies";
  public static final String SVC_GET_EXCHANGE_RATES_BY_CURRENCY =
      "get_exchange_rates_by_currency";

  public static final String SVC_UPDATE_EXCHANGE_RATES = "update_exchange_rates";

  public static final String SVC_BLOCK_HOST = "block_host";
  public static final String SVC_CREATE_USER = "create_user";
  public static final String SVC_COPY_RIGHTS = "copy_rights";

  public static final String AUDIT_SUFFIX = "AUDIT";
  public static final String AUDIT_USER = "bee.user";
  public static final String AUDIT_FLD_TIME = "Time";
  public static final String AUDIT_FLD_USER = "UserId";
  public static final String AUDIT_FLD_TX = "TransactionId";
  public static final String AUDIT_FLD_MODE = "Mode";
  public static final String AUDIT_FLD_ID = "RecordId";
  public static final String AUDIT_FLD_FIELD = "Field";
  public static final String AUDIT_FLD_VALUE = "Value";

  public static final String TBL_USERS = "Users";
  public static final String TBL_USER_SETTINGS = "UserSettings";
  public static final String TBL_ROLES = "Roles";
  public static final String TBL_USER_ROLES = "UserRoles";
  public static final String TBL_OBJECTS = "Objects";
  public static final String TBL_RIGHTS = "Rights";
  public static final String TBL_USER_HISTORY = "UserHistory";
  public static final String TBL_USER_GROUPS = "UserGroups";

  public static final String TBL_FILES = "Files";
  public static final String TBL_FILE_PARTS = "FileParts";

  public static final String TBL_REMINDER_TYPES = "ReminderTypes";

  public static final String TBL_RELATIONS = "Relations";

  public static final String TBL_IP_FILTERS = "IpFilters";

  public static final String TBL_CURRENCIES = "Currencies";
  public static final String TBL_CURRENCY_RATES = "CurrencyRates";

  public static final String TBL_DEPARTMENTS = "Departments";
  public static final String TBL_DEPARTMENT_EMPLOYEES = "DepartmentEmployees";

  public static final String TBL_AUTOCOMPLETE = "Autocomplete";

  public static final String VIEW_USERS = "Users";
  public static final String VIEW_USER_SETTINGS = "UserSettings";
  public static final String VIEW_USER_GROUP_SETTINGS = "UserGroupSettings";
  public static final String VIEW_USER_GROUP_MEMBERS = "UserGroupMembers";

  public static final String VIEW_REMINDER_TYPES = "ReminderTypes";

  public static final String VIEW_COLORS = "Colors";
  public static final String VIEW_THEMES = "Themes";
  public static final String VIEW_THEME_COLORS = "ThemeColors";

  public static final String VIEW_CURRENCIES = "Currencies";
  public static final String VIEW_CURRENCY_RATES = "CurrencyRates";

  public static final String VIEW_RELATIONS = "Relations";

  public static final String VIEW_IP_FILTERS = "IpFilters";

  public static final String VIEW_AUTOCOMPLETE = "Autocomplete";

  public static final String VIEW_ROLES = "Roles";
  public static final String VIEW_RIGHTS = "Rights";
  public static final String VIEW_USER_ROLES = "UserRoles";

  public static final String VIEW_PARAMETERS = "Parameters";
  public static final String VIEW_USER_PARAMETERS = "UserParameters";

  public static final String VIEW_FILTERS = "Filters";

  public static final String VIEW_FAVORITES = "Favorites";
  public static final String VIEW_WORKSPACES = "Workspaces";
  public static final String VIEW_REPORT_SETTINGS = "ReportSettings";

  public static final String GRID_HISTORY = "History";
  public static final String GRID_USER_GROUP_MEMBERS = "UserGroupMembers";

  public static final String GRID_ROLE_USERS = "RoleUsers";

  public static final String GRID_COLORS = "Colors";
  public static final String GRID_THEMES = "Themes";
  public static final String GRID_THEME_COLORS = "ThemeColors";

  public static final String COL_RELATION = "Relation";

  public static final String COL_LOGIN = "Login";
  public static final String COL_PASSWORD = "Password";
  public static final String COL_USER_LOCALE = "Locale";
  public static final String COL_USER_INTERFACE = "Interface";
  public static final String COL_USER_BLOCK_AFTER = "BlockAfter";
  public static final String COL_USER_BLOCK_BEFORE = "BlockBefore";
  public static final String COL_REMOTE_HOST = "Host";
  public static final String COL_USER_AGENT = "Agent";
  public static final String COL_LOGGED_IN = "LoggedIn";
  public static final String COL_LOGGED_OUT = "LoggedOut";

  public static final String COL_ROLE_NAME = "Name";
  public static final String COL_USER = "User";
  public static final String COL_ROLE = "Role";

  public static final String COL_OBJECT_TYPE = "Type";
  public static final String COL_OBJECT = "Object";
  public static final String COL_OBJECT_NAME = "Name";
  public static final String COL_STATE = "State";

  public static final String COL_FILE = "File";
  public static final String COL_FILE_NAME = "Name";
  public static final String COL_FILE_HASH = "Hash";
  public static final String COL_FILE_REPO = "Repository";
  public static final String COL_FILE_SIZE = "Size";
  public static final String COL_FILE_TYPE = "Type";
  public static final String COL_FILE_PART = "Part";

  public static final String COL_FILE_CAPTION = "Caption";

  public static final String COL_REMINDER_NAME = "Name";
  public static final String COL_REMINDER_METHOD = "Method";
  public static final String COL_REMINDER_HOURS = "Hours";
  public static final String COL_REMINDER_MINUTES = "Minutes";
  public static final String COL_REMINDER_TEMPLATE_CAPTION = "Caption";
  public static final String COL_REMINDER_TEMPLATE = "Template";

  public static final String COL_COLOR = "Color";
  public static final String COL_COLOR_NAME = "Name";
  public static final String COL_DEFAULT_COLOR = "DefaultColor";
  public static final String COL_BACKGROUND = "Background";
  public static final String COL_FOREGROUND = "Foreground";

  public static final String COL_THEME = "Theme";

  public static final String COL_FAVORITE_USER = "User";

  public static final String COL_FILTER_USER = "User";
  public static final String COL_FILTER_KEY = "Key";
  public static final String COL_FILTER_ORDINAL = "Ordinal";

  public static final String COL_CURRENCY = "Currency";
  public static final String COL_CURRENCY_NAME = "Name";
  public static final String COL_CURRENCY_UPDATE_TAG = "UpdateTag";

  public static final String COL_CURRENCY_RATE_CURRENCY = "Currency";
  public static final String COL_CURRENCY_RATE_DATE = "Date";
  public static final String COL_CURRENCY_RATE_QUANTITY = "Quantity";
  public static final String COL_CURRENCY_RATE = "Rate";

  public static final String COL_IP_FILTER_HOST = "Host";
  public static final String COL_IP_FILTER_BLOCK_AFTER = "BlockAfter";
  public static final String COL_IP_FILTER_BLOCK_BEFORE = "BlockBefore";

  public static final String COL_AUTOCOMPLETE_USER = "User";
  public static final String COL_AUTOCOMPLETE_KEY = "Key";
  public static final String COL_AUTOCOMPLETE_VALUE = "Value";

  public static final String COL_USER_GROUP_SETTINGS_NAME = "Name";
  public static final String COL_USER_GROUP_SETTINGS_OWNER = "Owner";
  public static final String COL_USER_GROUP_SETTINGS_VISIBILITY = "Visibility";

  public static final String COL_UG_GROUP = "Group";
  public static final String COL_UG_USER = "User";

  public static final String COL_DEPARTMENT = "Department";
  public static final String COL_DEPARTMENT_NAME = "DepartmentName";
  public static final String COL_DEPARTMENT_HEAD = "DepartmentHead";

  public static final String COL_RS_REPORT = "Report";
  public static final String COL_RS_USER = "User";
  public static final String COL_RS_CAPTION = "Caption";
  public static final String COL_RS_PARAMETERS = "Parameters";

  public static final String COL_OPEN_IN_NEW_TAB = "OpenInNewTab";
  public static final String COL_WORKSPACE_CONTINUE = "WorkspaceContinue";
  public static final String COL_LAST_WORKSPACE = "LastWorkspace";

  public static final String COL_USER_STYLE = "Style";

  public static final String ALS_FILE_NAME = "FileName";
  public static final String ALS_FILE_SIZE = "FileSize";
  public static final String ALS_FILE_TYPE = "FileType";

  public static final String ALS_COLOR_NAME = "ColorName";
  public static final String ALS_DEFAULT_COLOR_NAME = "DefaultColorName";
  public static final String ALS_DEFAULT_BACKGROUND = "DefaultBackground";
  public static final String ALS_DEFAULT_FOREGROUND = "DefaultForeground";

  public static final String ALS_OBJECT_TYPE = "ObjectType";
  public static final String ALS_OBJECT_NAME = "ObjectName";

  public static final String ALS_CURRENCY_NAME = "CurrencyName";

  public static final String FORM_USER = "User";
  public static final String FORM_USER_SETTINGS = "UserSettings";
  public static final String FORM_DEPARTMENT = "Department";
  public static final String FORM_NEW_ROLE = "NewRole";

  public static final String PRM_SQL_MESSAGES = "SQLMessages";

  public static final String PRM_COMPANY = "CompanyName";
  public static final String PRM_CURRENCY = "MainCurrency";
  public static final String PRM_VAT_PERCENT = "VATPercent";

  public static final String PRM_ERP_NAMESPACE = "ERPNamespace";
  public static final String PRM_ERP_ADDRESS = "ERPAddress";
  public static final String PRM_ERP_LOGIN = "ERPLogin";
  public static final String PRM_ERP_PASSWORD = "ERPPassword";

  public static final String PRM_WS_LB_EXCHANGE_RATES_ADDRESS = "WSLBExchangeRates";

  public static final String PRM_URL = "Url";

  public static final String PROP_ICON = "Icon";

  public static final String STYLE_SHEET = "commons";

  private AdministrationConstants() {
  }
}
