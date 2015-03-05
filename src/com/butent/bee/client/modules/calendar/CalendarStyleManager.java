package com.butent.bee.client.modules.calendar;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

public final class CalendarStyleManager {

  public static final String DAY_VIEW = BeeConst.CSS_CLASS_PREFIX + "cal";
  public static final String RESOURCE_VIEW = DAY_VIEW;
  public static final String MONTH_VIEW = BeeConst.CSS_CLASS_PREFIX + "cal-MonthView";

  public static final String CALENDAR_HEADER = "calendar-header";
  public static final String DATE_CELL = "date-cell";

  public static final String DAY_CELL_CONTAINER = "day-cell-container";

  public static final String DAY_CELL = "day-cell";
  public static final String DAY_CELL_WEEKEND = "day-cell-weekend";

  public static final String MULTI_DAY_PANEL = "multiDayPanel";
  public static final String MULTI_DAY_GRID_CELL = "centerDayContainerCell";
  public static final String MULTI_DAY_GRID = "multiDayGrid";

  public static final String TIMELINE_EMPTY_CELL = "leftEmptyCell";
  public static final String SCROLLBAR_EMPTY_CELL = "rightEmptyCell";

  public static final String SCROLL_AREA = "scroll-area";
  public static final String APPOINTMENT_PANEL = "appointment-panel";
  public static final String TIME_STRIP = "time-strip";
  public static final String APPOINTMENT_GRID = "appointment-grid";

  public static final String HOUR_PANEL = "hour-panel";
  public static final String HOUR_LAYOUT = "hour-layout";
  public static final String HOUR_LABEL = "hour-label";

  public static final String MAJOR_TIME_INTERVAL = "major-time-interval";
  public static final String MINOR_TIME_INTERVAL = "minor-time-interval";

  public static final String WORKING_HOURS = "working-hours";
  public static final String NON_WORKING = "non-working";

  public static final String RESOURCE_CAPTION_CONTAINER = "caption-container";
  public static final String RESOURCE_CAPTION_CELL = "caption-cell";

  public static final String COLUMN_SEPARATOR = "column-separator";

  public static final String HEADER = "header";
  public static final String BODY = "body";
  public static final String FOOTER = "footer";

  public static final String APPOINTMENT = "appointment";
  public static final String APPOINTMENT_SMALL = "appointment-small";
  public static final String APPOINTMENT_BIG = "appointment-big";
  public static final String APPOINTMENT_MULTIDAY = "appointment-multiday";

  public static final String TASK = "task";
  public static final String PARTIAL = "partial";

  public static final String STATUS_PREFIX = "status-";

  public static final String MOVABLE = "movable";
  public static final String RESIZABLE = "resizable";

  public static final String MONTH_GRID = "grid";
  public static final String MONTH_CANVAS = "canvas";

  public static final String WEEKDAY_LABEL = "weekDayLabel";

  public static final String MONTH_CELL = "dayCell";
  public static final String MONTH_CELL_LABEL = "dayCellLabel";

  public static final String MORE_LABEL = "moreAppointments";
  public static final String MORE_POPUP = BeeConst.CSS_CLASS_PREFIX + "cal-morePopup";
  public static final String MORE_PANEL = "morePanel";

  public static final String DISABLED = "disabled";

  public static final String FIRST_COLUMN = "firstColumn";
  public static final String LAST_COLUMN = "lastColumn";

  public static final String NOW_POINTER = "nowPointer";
  public static final String NOW_MARKER = "nowMarker";

  public static final String TODAY = BeeConst.CSS_CLASS_PREFIX + "cal-today";
  public static final String TODAY_MARKER = BeeConst.CSS_CLASS_PREFIX + "cal-todayMarker";

  public static final String DRAG = "drag";
  public static final String POSITIONER = "positioner";
  public static final String TARGET = "target";

  static void applyStyle(Widget widget, String styles) {
    if (!BeeUtils.isEmpty(styles)) {
      StyleUtils.updateStyle(widget, styles);
    }
  }

  static void applyStyle(ItemWidget widget, String panelStyle,
      String headerStyle, String bodyStyle, String footerStyle) {
    if (!BeeUtils.isEmpty(panelStyle)) {
      StyleUtils.updateStyle(widget, panelStyle);
    }

    if (!BeeUtils.isEmpty(headerStyle)) {
      StyleUtils.updateStyle(widget.getHeaderPanel(), headerStyle);
    }
    if (!BeeUtils.isEmpty(bodyStyle)) {
      StyleUtils.updateStyle(widget.getBodyPanel(), bodyStyle);
    }
    if (!BeeUtils.isEmpty(footerStyle)) {
      StyleUtils.updateStyle(widget.getFooterPanel(), footerStyle);
    }
  }

  private CalendarStyleManager() {
  }
}
