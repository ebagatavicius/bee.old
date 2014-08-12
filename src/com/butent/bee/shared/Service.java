package com.butent.bee.shared;

import com.butent.bee.shared.utils.BeeUtils;

/**
 * Contains service tag descriptions and type identification methods.
 */

public final class Service {

  public static final String PROPERTY_AUDIT_OFF = "DisableAuditing";
  public static final String PROPERTY_MODULES = "Modules";
  public static final String PROPERTY_VIEW_MODULES = "ViewModules";

  public static final String RPC_SERVICE_PREFIX = "rpc_";
  public static final String COMPOSITE_SERVICE_PREFIX = "comp_";
  public static final String UPLOAD_SERVICE_PREFIX = "upl_";
  public static final String EXPORT_SERVICE_PREFIX = "exp_";

  public static final String DB_SERVICE_PREFIX = RPC_SERVICE_PREFIX + "db_";
  public static final String DB_JDBC = DB_SERVICE_PREFIX + "jdbc";

  public static final String DB_META_SERVICE_PREFIX = DB_SERVICE_PREFIX + "meta_";
  public static final String DB_PING = DB_META_SERVICE_PREFIX + "ping";
  public static final String DB_INFO = DB_META_SERVICE_PREFIX + "info";
  public static final String DB_TABLES = DB_META_SERVICE_PREFIX + "tables";
  public static final String DB_KEYS = DB_META_SERVICE_PREFIX + "keys";
  public static final String DB_PRIMARY = DB_META_SERVICE_PREFIX + "primary";

  public static final String SYS_SERVICE_PREFIX = RPC_SERVICE_PREFIX + "sys_";
  public static final String GET_CLASS_INFO = SYS_SERVICE_PREFIX + "class_info";
  public static final String GET_RESOURCE = SYS_SERVICE_PREFIX + "get_resource";
  public static final String SAVE_RESOURCE = SYS_SERVICE_PREFIX + "save_resource";
  public static final String GET_DIGEST = SYS_SERVICE_PREFIX + "get_digest";
  public static final String GET_FILES = SYS_SERVICE_PREFIX + "get_files";
  public static final String GET_FLAGS = SYS_SERVICE_PREFIX + "get_flags";
  public static final String RUN = SYS_SERVICE_PREFIX + "run";

  public static final String INVOKE = RPC_SERVICE_PREFIX + "invoke";

  public static final String LOGIN = RPC_SERVICE_PREFIX + "login";
  public static final String LOGOUT = RPC_SERVICE_PREFIX + "logout";

  public static final String GET_MENU = RPC_SERVICE_PREFIX + "get_menu";
  public static final String GET_ROOM = RPC_SERVICE_PREFIX + "get_room";

  public static final String WHERE_AM_I = RPC_SERVICE_PREFIX + "where_am_i";

  public static final String DATA_SERVICE_PREFIX = RPC_SERVICE_PREFIX + "data_";

  public static final String GET_GRID = DATA_SERVICE_PREFIX + "get_grid";
  public static final String GET_FORM = DATA_SERVICE_PREFIX + "get_form";
  public static final String GET_DECORATORS = DATA_SERVICE_PREFIX + "get_decorators";

  public static final String REBUILD = DATA_SERVICE_PREFIX + "rebuild";
  public static final String MAIL = DATA_SERVICE_PREFIX + "mail";
  public static final String DO_SQL = DATA_SERVICE_PREFIX + "do_sql";
  public static final String QUERY = DATA_SERVICE_PREFIX + "query";
  public static final String GET_VALUE = DATA_SERVICE_PREFIX + "get_value";
  public static final String GET_DATA = DATA_SERVICE_PREFIX + "get_data";
  public static final String GET_DATA_INFO = DATA_SERVICE_PREFIX + "data_info";
  public static final String GET_VIEW_INFO = DATA_SERVICE_PREFIX + "view_info";
  public static final String GET_TABLE_INFO = DATA_SERVICE_PREFIX + "table_info";
  public static final String GENERATE = DATA_SERVICE_PREFIX + "generate";
  public static final String COUNT_ROWS = DATA_SERVICE_PREFIX + "row_count";
  public static final String DELETE = DATA_SERVICE_PREFIX + "delete";
  public static final String DELETE_ROWS = DATA_SERVICE_PREFIX + "delete_rows";
  public static final String UPDATE_CELL = DATA_SERVICE_PREFIX + "update_cell";
  public static final String UPDATE_ROW = DATA_SERVICE_PREFIX + "update_row";
  public static final String UPDATE = DATA_SERVICE_PREFIX + "update";
  public static final String INSERT_ROW = DATA_SERVICE_PREFIX + "insert_row";
  public static final String INSERT_ROWS = DATA_SERVICE_PREFIX + "insert_rows";
  public static final String INSERT_ROW_SILENTLY = DATA_SERVICE_PREFIX + "insert_row_silently";
  public static final String SEARCH = DATA_SERVICE_PREFIX + "search";
  public static final String HISTOGRAM = DATA_SERVICE_PREFIX + "histogram";
  public static final String GET_RELATED_VALUES = DATA_SERVICE_PREFIX + "get_related_values";
  public static final String UPDATE_RELATED_VALUES = DATA_SERVICE_PREFIX + "update_related_values";

  public static final String IMPORT_CSV_COMPANIES = DATA_SERVICE_PREFIX + "import_csv_companies";

  public static final String GET_DSNS = DATA_SERVICE_PREFIX + "get_dsns";
  public static final String SWITCH_DSN = DATA_SERVICE_PREFIX + "switch_dsn";

  public static final String GET_AUTOCOMPLETE = DATA_SERVICE_PREFIX + "get_autocomplete";
  public static final String UPDATE_AUTOCOMPLETE = DATA_SERVICE_PREFIX + "update_autocomplete";

  public static final String GET_NEWS = DATA_SERVICE_PREFIX + "get_news";
  public static final String SUBSCRIBE_TO_FEEDS = DATA_SERVICE_PREFIX + "subscribe_to_feeds";
  public static final String ACCESS = DATA_SERVICE_PREFIX + "access";

  public static final String GET_STATE_RIGHTS = DATA_SERVICE_PREFIX + "get_state_rights";
  public static final String GET_ROLE_RIGHTS = DATA_SERVICE_PREFIX + "get_role_rights";
  public static final String SET_STATE_RIGHTS = DATA_SERVICE_PREFIX + "set_state_rights";
  public static final String SET_ROLE_RIGHTS = DATA_SERVICE_PREFIX + "set_role_rights";

  public static final String SET_ROW_RIGHTS = DATA_SERVICE_PREFIX + "set_row_rights";

  public static final String UPLOAD_FILE = UPLOAD_SERVICE_PREFIX + "file";
  public static final String UPLOAD_TEMP_FILE = UPLOAD_SERVICE_PREFIX + "temp_file";
  public static final String UPLOAD_PHOTO = UPLOAD_SERVICE_PREFIX + "photo";
  public static final String DELETE_PHOTO = UPLOAD_SERVICE_PREFIX + "delete_photo";

  public static final String EXPORT_WORKBOOK = EXPORT_SERVICE_PREFIX + "workbook";

  public static final String RPC_VAR_PREFIX = "bee_";
  public static final String RPC_VAR_SYS_PREFIX = RPC_VAR_PREFIX + "sys_";

  public static final String RPC_VAR_SVC = RPC_VAR_SYS_PREFIX + "svc";
  public static final String RPC_VAR_QID = RPC_VAR_SYS_PREFIX + "qid";
  public static final String RPC_VAR_SID = RPC_VAR_SYS_PREFIX + "sid";
  public static final String RPC_VAR_OPT = RPC_VAR_SYS_PREFIX + "opt";

  public static final String RPC_VAR_CTP = RPC_VAR_SYS_PREFIX + "ctp";
  public static final String RPC_VAR_URI = RPC_VAR_SYS_PREFIX + "uri";
  public static final String RPC_VAR_MD5 = RPC_VAR_SYS_PREFIX + "md5";

  public static final String RPC_VAR_MSG = RPC_VAR_SYS_PREFIX + "msg";
  public static final String RPC_VAR_PRM_CNT = RPC_VAR_SYS_PREFIX + "p_c";
  public static final String RPC_VAR_PRM = RPC_VAR_SYS_PREFIX + "prm";

  public static final String RPC_VAR_METH = RPC_VAR_SYS_PREFIX + "meth";

  public static final String VAR_USER = RPC_VAR_PREFIX + "user";

  public static final String VAR_JDBC_QUERY = RPC_VAR_PREFIX + "jdbc_query";
  public static final String VAR_CONNECTION_AUTO_COMMIT = RPC_VAR_PREFIX + "conn_auto_commit";
  public static final String VAR_CONNECTION_READ_ONLY = RPC_VAR_PREFIX + "conn_read_only";
  public static final String VAR_CONNECTION_HOLDABILITY = RPC_VAR_PREFIX + "conn_holdability";
  public static final String VAR_CONNECTION_TRANSACTION_ISOLATION = RPC_VAR_PREFIX + "conn_ti";
  public static final String VAR_STATEMENT_CURSOR_NAME = RPC_VAR_PREFIX + "stmt_cursor";
  public static final String VAR_STATEMENT_ESCAPE_PROCESSING = RPC_VAR_PREFIX + "stmt_escape";
  public static final String VAR_STATEMENT_FETCH_DIRECTION = RPC_VAR_PREFIX + "stmt_fetch_dir";
  public static final String VAR_STATEMENT_FETCH_SIZE = RPC_VAR_PREFIX + "stmt_fetch_size";
  public static final String VAR_STATEMENT_MAX_FIELD_SIZE = RPC_VAR_PREFIX + "stmt_field_size";
  public static final String VAR_STATEMENT_MAX_ROWS = RPC_VAR_PREFIX + "stmt_max_rows";
  public static final String VAR_STATEMENT_POOLABLE = RPC_VAR_PREFIX + "stmt_poolable";
  public static final String VAR_STATEMENT_QUERY_TIMEOUT = RPC_VAR_PREFIX + "stmt_timeout";
  public static final String VAR_STATEMENT_RS_TYPE = RPC_VAR_PREFIX + "stmt_rs_type";
  public static final String VAR_STATEMENT_RS_CONCURRENCY = RPC_VAR_PREFIX + "stmt_rs_concurrency";
  public static final String VAR_STATEMENT_RS_HOLDABILITY = RPC_VAR_PREFIX + "stmt_rs_holdability";
  public static final String VAR_RESULT_SET_FETCH_DIRECTION = RPC_VAR_PREFIX + "rs_fetch_dir";
  public static final String VAR_RESULT_SET_FETCH_SIZE = RPC_VAR_PREFIX + "rs_fetch_size";
  public static final String VAR_JDBC_RETURN = RPC_VAR_PREFIX + "jdbc_return";

  public static final String VAR_VIEW_NAME = RPC_VAR_PREFIX + "view_name";
  public static final String VAR_VIEW_WHERE = RPC_VAR_PREFIX + "view_where";
  public static final String VAR_VIEW_ORDER = RPC_VAR_PREFIX + "view_order";
  public static final String VAR_VIEW_OFFSET = RPC_VAR_PREFIX + "view_offset";
  public static final String VAR_VIEW_LIMIT = RPC_VAR_PREFIX + "view_limit";
  public static final String VAR_VIEW_STATES = RPC_VAR_PREFIX + "view_states";
  public static final String VAR_VIEW_ROWS = RPC_VAR_PREFIX + "view_rows";
  public static final String VAR_VIEW_COLUMNS = RPC_VAR_PREFIX + "view_columns";

  public static final String VAR_VIEW_SIZE = RPC_VAR_PREFIX + "view_size";
  public static final String VAR_VIEW_ROW_ID = RPC_VAR_PREFIX + "view_row_id";
  public static final String VAR_VIEW_LIST = RPC_VAR_PREFIX + "view_list";

  public static final String VAR_CATALOG = RPC_VAR_PREFIX + "catalog";
  public static final String VAR_SCHEMA = RPC_VAR_PREFIX + "schema";

  public static final String VAR_TYPE = RPC_VAR_PREFIX + "type";
  public static final String VAR_CHECK = RPC_VAR_PREFIX + "check";

  public static final String VAR_TABLE = RPC_VAR_PREFIX + "table";
  public static final String VAR_COLUMN = RPC_VAR_PREFIX + "column";
  public static final String VAR_VALUE = RPC_VAR_PREFIX + "value";
  public static final String VAR_ID = RPC_VAR_PREFIX + "id";

  public static final String VAR_FILTER_COLUMN = RPC_VAR_PREFIX + "filter_column";
  public static final String VAR_VALUE_COLUMN = RPC_VAR_PREFIX + "value_column";

  public static final String VAR_CHILDREN = RPC_VAR_PREFIX + "children";

  public static final String VAR_FILE_ID = RPC_VAR_PREFIX + "file_id";
  public static final String VAR_FILE_NAME = RPC_VAR_PREFIX + "file_name";
  public static final String VAR_FILE_TYPE = RPC_VAR_PREFIX + "file_type";
  public static final String VAR_FILE_SIZE = RPC_VAR_PREFIX + "file_size";

  public static final String VAR_OLD_VALUE = RPC_VAR_PREFIX + "old_value";
  public static final String VAR_OPTIONS = RPC_VAR_PREFIX + "options";

  public static final String VAR_DSN = RPC_VAR_PREFIX + "dsn";
  public static final String VAR_UI = RPC_VAR_PREFIX + "ui";

  public static final String VAR_PROGRESS = RPC_VAR_PREFIX + "progress";
  public static final String VAR_FEED = RPC_VAR_PREFIX + "feed";

  public static final String VAR_RIGHTS = RPC_VAR_PREFIX + "rights";

  public static final String VAR_FROM = RPC_VAR_PREFIX + "from";
  public static final String VAR_TO = RPC_VAR_PREFIX + "to";
  public static final String VAR_GROUP_BY = RPC_VAR_PREFIX + "group_by";
  public static final String VAR_OPERATOR = RPC_VAR_PREFIX + "operator";

  public static final String VAR_DATA = RPC_VAR_PREFIX + "data";

  public static final String VIEW_COLUMN_SEPARATOR = " ";

  /**
   * Returns true if {@code svc} value starts with {@link #DATA_SERVICE_PREFIX}.
   *
   * @param svc name of service
   * @return true if name of service starts with {@link #DATA_SERVICE_PREFIX}
   */
  public static boolean isDataService(String svc) {
    Assert.notEmpty(svc);
    return svc.startsWith(DATA_SERVICE_PREFIX);
  }

  /**
   * Returns true if {@code svc} value starts with {@link #DB_META_SERVICE_PREFIX}.
   *
   * @param svc name of service
   * @return true if name of service starts with {@link #DB_META_SERVICE_PREFIX}
   */
  public static boolean isDbMetaService(String svc) {
    Assert.notEmpty(svc);
    return svc.startsWith(DB_META_SERVICE_PREFIX);
  }

  /**
   * Returns true if {@code svc} value starts with {@link #DB_SERVICE_PREFIX}.
   *
   * @param svc name of service;
   * @return true if name of service starts with {@link #DB_SERVICE_PREFIX}
   */
  public static boolean isDbService(String svc) {
    Assert.notEmpty(svc);
    return svc.startsWith(DB_SERVICE_PREFIX);
  }

  /**
   * Returns true if {@code svc} value has {@link #INVOKE}.
   *
   * @param svc name of service;
   * @return true if name of service starts {@link #INVOKE}
   */
  public static boolean isInvocation(String svc) {
    return BeeUtils.same(svc, INVOKE);
  }

  /**
   * Returns true if {@code svc} value starts with {@link #SYS_SERVICE_PREFIX}.
   *
   * @param svc name of service
   * @return true if name of service starts with {@link #SYS_SERVICE_PREFIX};
   */
  public static boolean isSysService(String svc) {
    Assert.notEmpty(svc);
    return svc.startsWith(SYS_SERVICE_PREFIX);
  }

  private Service() {
  }
}
