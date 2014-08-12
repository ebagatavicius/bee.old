package com.butent.bee.shared.ui;

import com.butent.bee.shared.io.Paths;
import com.butent.bee.shared.time.TimeUtils;

public final class UiConstants {

  public static final String ATTR_NAME = "name";
  public static final String ATTR_SOURCE = "source";

  public static final String ATTR_VIEW_NAME = "viewName";

  public static final String ATTR_FILTER = "filter";
  public static final String ATTR_CURRENT_USER_FILTER = "currentUserFilter";
  public static final String ATTR_ORDER = "order";

  public static final String ATTR_CAPTION = "caption";
  public static final String ATTR_LABEL = "label";
  public static final String ATTR_FAVORITE = "favorite";

  public static final String ATTR_VALUE = "value";
  public static final String ATTR_READ_ONLY = "readOnly";

  public static final String ATTR_CLASS = "class";
  public static final String ATTR_STYLE = "style";

  public static final String ATTR_HORIZONTAL_ALIGNMENT = "horizontalAlignment";
  public static final String ATTR_VERTICAL_ALIGNMENT = "verticalAlignment";

  public static final String ATTR_WHITE_SPACE = "whiteSpace";

  public static final String ATTR_FORMAT = "format";
  public static final String ATTR_SCALE = "scale";

  public static final String ATTR_CACHE_DESCRIPTION = "cacheDescription";

  public static final String ATTR_HTML = "html";
  public static final String ATTR_TEXT = "text";

  public static final String ATTR_NEW_ROW_FORM = "newRowForm";
  public static final String ATTR_NEW_ROW_COLUMNS = "newRowColumns";
  public static final String ATTR_NEW_ROW_CAPTION = "newRowCaption";
  public static final String ATTR_NEW_ROW_ENABLED = "newRowEnabled";

  public static final String ATTR_EDIT_FORM = "editForm";
  public static final String ATTR_EDIT_POPUP = "editPopup";
  public static final String ATTR_EDIT_ENABLED = "editEnabled";
  public static final String ATTR_EDIT_KEY = "editKey";

  public static final String ATTR_EDIT_SOURCE = "editSource";
  public static final String ATTR_EDIT_VIEW_NAME = "editViewName";

  public static final String ATTR_PROPERTY = "property";

  public static final String ATTR_VISIBLE = "visible";
  public static final String ATTR_MODULE = "module";
  public static final String ATTR_DATA = "data";
  public static final String ATTR_FOR = "for";

  public static final String ATTR_GRID_NAME = "gridName";

  public static final String TAG_ROW = "row";
  public static final String TAG_COL = "col";
  public static final String TAG_CELL = "cell";

  public static final String TAG_CHILD_GRID = "ChildGrid";
  public static final String TAG_GRID_PANEL = "GridPanel";

  public static final String TAG_DATA_TREE = "DataTree";
  public static final String TAG_MULTI_SELECTOR = "MultiSelector";

  public static final int MAX_PASSWORD_LENGTH = 30;

  public static final String PATTERN_UNSIGNED_INT = "\\d*";
  public static final String PATTERN_UNSIGNED_NUM = "(\\d*|\\d+\\.\\d+)";
  public static final String PATTERN_DATE =
      "([0-9]{2}|[0-9]{4})[- .](0[1-9]|1[012])[- .](0[1-9]|1[0-9]|2[0-9]|3[01])";
  public static final String PATTERN_DATE_TIME = PATTERN_DATE
      + "(\\s+(0[0-9]|1[0-9]|2[0-3])([: ][0-5][0-9])?)?";

  public static String wtfplLabel() {
    return "UAB \"Būtenta\" &copy; 2010 - " + TimeUtils.today().getYear();
  }

  public static String wtfplLogo() {
    return Paths.buildPath(Paths.IMAGE_DIR, "butent_arrow.png");
  }

  public static String wtfplUrl() {
    return "http://www.butent.com";
  }

  private UiConstants() {
  }
}
