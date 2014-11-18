package com.butent.bee.client.modules.calendar.view;

import com.google.common.collect.Range;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.calendar.CalendarFormat;
import com.butent.bee.client.modules.calendar.CalendarKeeper;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.modules.calendar.CalendarUtils;
import com.butent.bee.client.modules.calendar.CalendarView;
import com.butent.bee.client.modules.calendar.CalendarWidget;
import com.butent.bee.client.modules.calendar.ItemWidget;
import com.butent.bee.client.modules.calendar.dnd.MonthMoveController;
import com.butent.bee.client.modules.calendar.layout.DayLayoutDescription;
import com.butent.bee.client.modules.calendar.layout.ItemLayoutDescription;
import com.butent.bee.client.modules.calendar.layout.ItemStackingManager;
import com.butent.bee.client.modules.calendar.layout.MonthLayoutDescription;
import com.butent.bee.client.modules.calendar.layout.WeekLayoutDescription;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonthView extends CalendarView {

  public static final Comparator<CalendarItem> ITEM_COMPARATOR = new Comparator<CalendarItem>() {
    @Override
    public int compare(CalendarItem x, CalendarItem y) {
      int result = Boolean.compare(x.isMultiDay(), y.isMultiDay());
      if (result == BeeConst.COMPARE_EQUAL) {
        result = x.compareTo(y);
      }
      return result;
    }
  };

  private static final int ITEM_HEIGHT = 17;
  private static final int ITEM_MARGIN_TOP = 3;

  private static final double ITEM_MARGIN_LEFT = 0.3;
  private static final double ITEM_MARGIN_RIGHT = 0.3;

  private static final double BAR_MARGIN_LEFT = 0.25;
  private static final double BAR_MIN_WIDTH = 0.1;

  private static final int PERCENT_SCALE = 3;

  private final HtmlTable grid = new HtmlTable();
  private final Flow canvas = new Flow();

  private final Map<String, List<CalendarItem>> moreLabels = new HashMap<>();

  private MonthMoveController moveController;

  private JustDate firstDate;
  private int requiredRows;

  private int cellOffsetHeight;
  private int cellHeight;

  private int weekDayHeaderHeight;
  private int dayHeaderHeight;

  private int maxCellItems;

  public MonthView() {
    super();

    grid.addStyleName(CalendarStyleManager.MONTH_GRID);
    canvas.addStyleName(CalendarStyleManager.MONTH_CANVAS);
  }

  @Override
  public void attach(CalendarWidget widget) {
    super.attach(widget);

    widget.clear();

    widget.add(grid);
    widget.add(canvas);

    if (moveController == null) {
      moveController = new MonthMoveController(this);
    }
  }

  @Override
  public void doLayout(long calendarId) {
    grid.clear();

    canvas.clear();

    getItemWidgets().clear();
    moreLabels.clear();

    buildGrid();

    calculateHeights();

    this.maxCellItems = cellHeight / (ITEM_HEIGHT + ITEM_MARGIN_TOP);

    moveController.setHeaderHeight(weekDayHeaderHeight);

    List<Long> attendees = getCalendarWidget().getAttendees();
    boolean separate = getSettings().separateAttendees();
    Map<Long, String> attColors = CalendarKeeper.getAttendeeColors(calendarId);

    List<CalendarItem> byRange = CalendarUtils.filterByRange(getItems(), firstDate,
        requiredRows * TimeUtils.DAYS_PER_WEEK);
    List<CalendarItem> filtered = CalendarUtils.filterByAttendees(byRange, attendees, separate);

    Collections.sort(filtered, ITEM_COMPARATOR);
    MonthLayoutDescription monthLayoutDescription = new MonthLayoutDescription(firstDate,
        requiredRows, filtered, maxCellItems - 1);

    WeekLayoutDescription[] weeks = monthLayoutDescription.getWeekDescriptions();
    for (int i = 0; i < requiredRows; i++) {
      WeekLayoutDescription weekDescription = weeks[i];
      if (weekDescription != null) {
        layOnTopItems(calendarId, weekDescription, i, separate, attColors);
        layOnWeekDaysItems(calendarId, weekDescription, i, separate, attColors);
      }
    }
  }

  @Override
  public void doScroll() {
  }

  @Override
  public void doSizing() {
  }

  public JustDate getCellDate(int row, int col) {
    return TimeUtils.nextDay(firstDate, row * TimeUtils.DAYS_PER_WEEK + col);
  }

  public int getColumn(int x) {
    double columnWidth = getColumnWidth();
    if (x > 0 && columnWidth > 0) {
      return BeeUtils.clamp(BeeUtils.floor(x / columnWidth), 0, TimeUtils.DAYS_PER_WEEK - 1);
    } else {
      return 0;
    }
  }

  public JustDate getFirstDate() {
    return firstDate;
  }

  public int getRow(int y) {
    double rowHeight = getRowHeight();
    if (y > weekDayHeaderHeight && rowHeight > 0) {
      return BeeUtils.clamp(BeeUtils.floor((y - weekDayHeaderHeight) / rowHeight),
          0, requiredRows - 1);
    } else {
      return 0;
    }
  }

  @Override
  public Widget getScrollArea() {
    return null;
  }

  @Override
  public String getStyleName() {
    return CalendarStyleManager.MONTH_VIEW;
  }

  @Override
  public Type getType() {
    return Type.MONTH;
  }

  @Override
  public Range<DateTime> getVisibleRange() {
    JustDate date = getDate();
    if (date == null) {
      return null;
    }

    JustDate start = calculateFirstDate(date);
    int rows = calculateRequiredRows(date);

    return Range.closedOpen(start.getDateTime(),
        TimeUtils.nextDay(start, rows * TimeUtils.DAYS_PER_WEEK).getDateTime());
  }

  @Override
  public boolean onClick(long calendarId, Element element, Event event) {
    if (element.equals(canvas.getElement())) {
      dayClicked(event);
      return true;

    } else if (moreLabels.containsKey(element.getId())) {
      showItems(calendarId, moreLabels.get(element.getId()));
      return true;

    } else {
      ItemWidget widget = CalendarUtils.findWidget(getItemWidgets(), element);
      if (widget != null
          && (widget.isMulti() || !widget.getCompactBar().getElement().isOrHasChild(element))) {
        openItem(widget.getItem());
        return true;
      }
    }
    return false;
  }

  @Override
  public void onClock() {
  }

  public void setCellStyle(int row, int col, String styleName, boolean add) {
    grid.getCellFormatter().setStyleName(row + 1, col, styleName, add);
  }

  private void buildCell(int row, int col, String text, boolean isToday, boolean currentMonth) {
    Label label = new Label(text);
    label.addStyleName(CalendarStyleManager.MONTH_CELL_LABEL);

    grid.setWidget(row, col, label);
    grid.getCellFormatter().addStyleName(row, col, CalendarStyleManager.MONTH_CELL);

    if (isToday) {
      label.addStyleName(CalendarStyleManager.TODAY);
      grid.getCellFormatter().addStyleName(row, col, CalendarStyleManager.TODAY);
    }
    if (!currentMonth) {
      label.addStyleName(CalendarStyleManager.DISABLED);
      grid.getCellFormatter().addStyleName(row, col, CalendarStyleManager.DISABLED);
    }

    switch (col) {
      case 0:
        grid.getCellFormatter().addStyleName(row, col, CalendarStyleManager.FIRST_COLUMN);
        break;
      case TimeUtils.DAYS_PER_WEEK - 1:
        grid.getCellFormatter().addStyleName(row, col, CalendarStyleManager.LAST_COLUMN);
        break;
    }
  }

  private void buildGrid() {
    for (int i = 0; i < TimeUtils.DAYS_PER_WEEK; i++) {
      grid.setHtml(0, i, CalendarFormat.getDayOfWeekNames()[i]);
      grid.getCellFormatter().setStyleName(0, i, CalendarStyleManager.WEEKDAY_LABEL);
    }

    JustDate date = getDate();
    int month = date.getMonth();

    this.firstDate = calculateFirstDate(date);
    this.requiredRows = calculateRequiredRows(date);

    JustDate today = TimeUtils.today();
    JustDate tmpDate = JustDate.copyOf(firstDate);

    for (int i = 1; i <= requiredRows; i++) {
      for (int j = 0; j < TimeUtils.DAYS_PER_WEEK; j++) {
        String dayCaption = BeeUtils.toString(tmpDate.getDom());

        if (j == 0) {
          CustomDiv woyDiv = new CustomDiv(CalendarStyleManager.MONTH_CELL_LABEL);
          woyDiv.addStyleName(CalendarStyleManager.DAY_CELL);
          woyDiv.setText(BeeUtils.joinWords(BeeUtils.toString(TimeUtils.weekOfYear(tmpDate)),
              Localized.getConstants().unitWeekShort()));
          dayCaption = woyDiv.toString();
          dayCaption += BeeUtils.toString(tmpDate.getDom());
        }

        buildCell(i, j, dayCaption, tmpDate.equals(today),
            tmpDate.getMonth() == month);
        TimeUtils.moveOneDayForward(tmpDate);
      }
    }
  }

  private static JustDate calculateFirstDate(JustDate dayInMonth) {
    JustDate date = TimeUtils.startOfMonth(dayInMonth);
    return TimeUtils.startOfWeek(date, (date.getDow() > 1) ? 0 : -1);
  }

  private void calculateHeights() {
    int gridHeight = grid.getOffsetHeight();

    this.weekDayHeaderHeight = grid.getRowFormatter().getElement(0).getOffsetHeight();
    this.dayHeaderHeight =
        grid.getCellFormatter().getElement(1, 0).getFirstChildElement().getOffsetHeight();

    this.cellOffsetHeight = (gridHeight - weekDayHeaderHeight) / requiredRows;
    this.cellHeight = cellOffsetHeight - dayHeaderHeight;
  }

  private static int calculateRequiredRows(JustDate dayInMonth) {
    int rows = 5;

    JustDate firstOfTheMonth = TimeUtils.startOfMonth(dayInMonth);
    JustDate firstDayInCalendar = calculateFirstDate(dayInMonth);

    if (firstDayInCalendar.getMonth() != firstOfTheMonth.getMonth()) {
      JustDate lastDayOfPreviousMonth = TimeUtils.previousDay(firstOfTheMonth);
      int prevMonthOverlap = TimeUtils.dayDiff(firstDayInCalendar, lastDayOfPreviousMonth) + 1;

      JustDate firstOfNextMonth = TimeUtils.startOfNextMonth(firstOfTheMonth);
      int daysInMonth = TimeUtils.dayDiff(firstOfTheMonth, firstOfNextMonth);

      if (prevMonthOverlap + daysInMonth > 35) {
        rows = 6;
      }
    }
    return rows;
  }

  private void dayClicked(Event event) {
    int row = getRow(event.getClientY() - canvas.getElement().getAbsoluteTop());

    int x = event.getClientX() - canvas.getElement().getAbsoluteLeft();
    int col = getColumn(x);

    DateTime start = getCellDate(row, col).getDateTime();

    double colWidth = getColumnWidth();
    double h = BeeUtils.rescale(x - col * colWidth, 0, colWidth, 0, 24);
    int hour = BeeUtils.clamp(BeeUtils.round(h), 0, 23);
    if (hour > 0) {
      start.setHour(hour);
    }

    createAppointment(start, null);
  }

  private double getColumnWidth() {
    return (double) canvas.getElement().getClientWidth() / TimeUtils.DAYS_PER_WEEK;
  }

  private double getRowHeight() {
    return (canvas.getElement().getClientHeight() - weekDayHeaderHeight) / (double) requiredRows;
  }

  private void layOnItem(long calendarId, CalendarItem item, boolean multi,
      int colStart, int colEnd, int row, int cellPosition, boolean separate,
      Map<Long, String> attColors) {

    Long sa = separate ? item.getSeparatedAttendee() : null;
    String bg = (sa != null && attColors != null) ? attColors.get(sa) : null;

    ItemWidget widget = new ItemWidget(item, multi);
    if (multi) {
      widget.render(calendarId, bg);
    } else {
      widget.renderCompact(calendarId, bg);
    }

    placeItemInGrid(widget, item, multi, colStart, colEnd, row, cellPosition);

    if (!multi && item.isMovable(BeeKeeper.getUser().getUserId())) {
      widget.getCompactBar().addMoveHandler(moveController);
      widget.getCompactBar().addStyleName(CalendarStyleManager.MOVABLE);
    }

    getItemWidgets().add(widget);
    canvas.add(widget);
  }

  private void layOnMoreLabel(List<CalendarItem> items, int dayOfWeek, int weekOfMonth) {
    Label more = new Label("+ " + items.size());
    more.setStyleName(CalendarStyleManager.MORE_LABEL);

    placeItemInGrid(more, null, false, dayOfWeek, dayOfWeek, weekOfMonth, maxCellItems);

    canvas.add(more);
    moreLabels.put(more.getId(), items);
  }

  private void layOnTopItems(long calendarId, WeekLayoutDescription weekDescription,
      int weekOfMonth, boolean separate, Map<Long, String> attColors) {

    ItemStackingManager manager = weekDescription.getTopItemsManager();
    for (int layer = 0; layer < maxCellItems; layer++) {
      List<ItemLayoutDescription> descriptions = manager.getDescriptionsInLayer(layer);
      if (descriptions == null) {
        break;
      }

      for (ItemLayoutDescription description : descriptions) {
        layOnItem(calendarId, description.getItem(), true,
            description.getWeekStartDay(), description.getWeekEndDay(), weekOfMonth, layer,
            separate, attColors);
      }
    }
  }

  private void layOnWeekDaysItems(long calendarId, WeekLayoutDescription week,
      int weekOfMonth, boolean separate, Map<Long, String> attColors) {

    ItemStackingManager manager = week.getTopItemsManager();

    for (int dayOfWeek = 0; dayOfWeek < TimeUtils.DAYS_PER_WEEK; dayOfWeek++) {
      DayLayoutDescription dayItems = week.getDayLayoutDescription(dayOfWeek);
      int layer = manager.lowestLayerIndex(dayOfWeek);

      if (dayItems != null) {
        int count = dayItems.getItemCount();

        for (int i = 0; i < count; i++) {
          CalendarItem item = dayItems.getItems().get(i);
          layer = manager.nextLowestLayerIndex(dayOfWeek, layer);

          if (layer > maxCellItems - 1) {
            int remaining = count + manager.countOverLimit(dayOfWeek) - i;

            if (remaining == 1) {
              layOnItem(calendarId, item, false, dayOfWeek, dayOfWeek, weekOfMonth,
                  layer, separate, attColors);
            } else {
              List<CalendarItem> overLimit = manager.getOverLimit(dayOfWeek);
              overLimit.addAll(new ArrayList<>(dayItems.getItems().subList(i, count)));

              layOnMoreLabel(overLimit, dayOfWeek, weekOfMonth);
            }
            break;
          }

          layOnItem(calendarId, item, false, dayOfWeek, dayOfWeek, weekOfMonth,
              layer, separate, attColors);
          layer++;
        }

      } else if (manager.countOverLimit(dayOfWeek) > 0) {
        layOnMoreLabel(manager.getOverLimit(dayOfWeek), dayOfWeek, weekOfMonth);
      }
    }
  }

  private void placeItemInGrid(Widget widget, CalendarItem item, boolean multi,
      int colStart, int colEnd, int row, int cellPosition) {

    double colWidth = 100d / TimeUtils.DAYS_PER_WEEK;

    double left = colStart * colWidth;
    double width = (colEnd - colStart + 1) * colWidth;

    double marginLeft = ITEM_MARGIN_LEFT;
    double marginRight = ITEM_MARGIN_RIGHT;

    if (item != null) {
      DateTime start = item.getStartTime();
      DateTime end = item.getEndTime();

      int startMinutes = TimeUtils.minutesSinceDayStarted(start);
      int endMinutes = TimeUtils.minutesSinceDayStarted(end);

      if (multi) {
        if (TimeUtils.sameDate(start, getCellDate(row, colStart)) && startMinutes > 0) {
          marginLeft = startMinutes * colWidth / TimeUtils.MINUTES_PER_DAY;
        }
        if (TimeUtils.sameDate(end, getCellDate(row, colEnd)) && endMinutes > 0) {
          marginRight =
              (TimeUtils.MINUTES_PER_DAY - endMinutes) * colWidth / TimeUtils.MINUTES_PER_DAY;
        }

      } else if (widget instanceof ItemWidget) {
        Widget bar = ((ItemWidget) widget).getCompactBar();

        double barFrom = BeeUtils.clamp(BeeUtils.div(startMinutes, TimeUtils.MINUTES_PER_DAY),
            BAR_MARGIN_LEFT, 1 - BAR_MIN_WIDTH);
        double barTo;
        if (TimeUtils.sameDate(start, end)) {
          barTo = BeeUtils.clamp(BeeUtils.div(endMinutes, TimeUtils.MINUTES_PER_DAY),
              barFrom + BAR_MIN_WIDTH, 1);
        } else {
          barTo = 1;
        }

        StyleUtils.setLeft(bar, BeeUtils.round(barFrom * 100, PERCENT_SCALE), CssUnit.PCT);
        StyleUtils.setRight(bar, BeeUtils.round((1 - barTo) * 100, PERCENT_SCALE), CssUnit.PCT);
      }
    }

    left += marginLeft;
    width -= marginLeft + marginRight;

    int y = weekDayHeaderHeight + row * cellOffsetHeight + dayHeaderHeight + ITEM_MARGIN_TOP
        + cellPosition * (ITEM_HEIGHT + ITEM_MARGIN_TOP);
    double top = 100d * y / grid.getOffsetHeight();

    StyleUtils.setLeft(widget, BeeUtils.round(left, PERCENT_SCALE), CssUnit.PCT);
    StyleUtils.setWidth(widget, BeeUtils.round(width, PERCENT_SCALE), CssUnit.PCT);

    StyleUtils.setTop(widget, BeeUtils.round(top, PERCENT_SCALE), CssUnit.PCT);
  }

  private void showItems(long calendarId, List<CalendarItem> items) {
    if (BeeUtils.isEmpty(items)) {
      return;
    }

    final Flow panel = new Flow();
    panel.addStyleName(CalendarStyleManager.MORE_PANEL);

    for (CalendarItem item : items) {
      ItemWidget widget = new ItemWidget(item, item.isMultiDay());
      widget.render(calendarId, null);

      panel.add(widget);
    }

    final DialogBox dialog = DialogBox.create(Localized.getConstants().calSelectAppointment(),
        CalendarStyleManager.MORE_POPUP);

    Binder.addMouseDownHandler(panel, new MouseDownHandler() {
      @Override
      public void onMouseDown(MouseDownEvent event) {
        if (event.getNativeButton() == NativeEvent.BUTTON_LEFT) {
          CalendarItem item = null;

          Element element = EventUtils.getEventTargetElement(event);
          for (int i = 0; i < panel.getWidgetCount(); i++) {
            if (panel.getWidget(i).getElement().isOrHasChild(element)) {
              item = ((ItemWidget) panel.getWidget(i)).getItem();
              dialog.close();
              break;
            }
          }

          if (item != null) {
            event.stopPropagation();
            openItem(item);
          }
        }
      }
    });

    dialog.setAnimationEnabled(true);

    dialog.setWidget(panel);
    dialog.center();
  }
}