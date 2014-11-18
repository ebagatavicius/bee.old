package com.butent.bee.client.modules.calendar.view;

import com.google.common.collect.Range;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.modules.calendar.Appointment;
import com.butent.bee.client.modules.calendar.CalendarKeeper;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.modules.calendar.CalendarUtils;
import com.butent.bee.client.modules.calendar.CalendarView;
import com.butent.bee.client.modules.calendar.CalendarWidget;
import com.butent.bee.client.modules.calendar.ItemWidget;
import com.butent.bee.client.modules.calendar.dnd.DayMoveController;
import com.butent.bee.client.modules.calendar.dnd.ResizeController;
import com.butent.bee.client.modules.calendar.layout.CalendarLayoutManager;
import com.butent.bee.client.modules.calendar.layout.ItemAdapter;
import com.butent.bee.client.modules.calendar.layout.ItemPanel;
import com.butent.bee.client.modules.calendar.layout.MultiDayPanel;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Orientation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DayView extends CalendarView {

  private final DayViewHeader dayViewHeader = new DayViewHeader();
  private final MultiDayPanel multiDayPanel = new MultiDayPanel();
  private final ItemPanel itemPanel = new ItemPanel();

  private DayMoveController moveController;
  private ResizeController resizeController;

  public DayView() {
    super();
  }

  @Override
  public void attach(CalendarWidget widget) {
    super.attach(widget);

    widget.clear();

    widget.add(dayViewHeader);
    widget.add(multiDayPanel);
    widget.add(itemPanel);

    createMoveController();
    createResizeController();
  }

  @Override
  public void doLayout(long calendarId) {
    JustDate date = getDate();
    int days = getDisplayedDays();

    List<Long> attendees = getCalendarWidget().getAttendees();

    dayViewHeader.setDays(date, days);
    dayViewHeader.setYear(date);

    int todayColumn = CalendarUtils.getTodayColumn(date, days);
    multiDayPanel.setColumns(days, todayColumn, todayColumn);

    itemPanel.build(days, getSettings(), todayColumn, todayColumn);

    moveController.setDate(JustDate.copyOf(date));
    moveController.setColumnCount(days);
    moveController.setSettings(getSettings());

    resizeController.setSettings(getSettings());

    getItemWidgets().clear();

    boolean separate = getSettings().separateAttendees();
    Map<Long, String> attColors = CalendarKeeper.getAttendeeColors(calendarId);

    JustDate tmpDate = JustDate.copyOf(date);

    for (int i = 0; i < days; i++) {
      List<CalendarItem> simple = CalendarUtils.filterSimple(getItems(), tmpDate,
          attendees, separate);

      if (!simple.isEmpty()) {
        List<ItemAdapter> adapters =
            CalendarLayoutManager.doLayout(simple, i, days, getSettings());
        addItemsToGrid(calendarId, adapters, false, i, separate, attColors);
      }

      TimeUtils.moveOneDayForward(tmpDate);
    }

    List<CalendarItem> multi = CalendarUtils.filterMulti(getItems(), date, days,
        attendees, separate);

    if (!multi.isEmpty()) {
      List<ItemAdapter> adapters = new ArrayList<>();
      for (CalendarItem item : multi) {
        adapters.add(new ItemAdapter(item));
      }

      int desiredHeight = CalendarLayoutManager.doMultiLayout(adapters, date, days);
      StyleUtils.setHeight(multiDayPanel.getGrid(), desiredHeight);

      addItemsToGrid(calendarId, adapters, true, BeeConst.UNDEF, separate, attColors);
    } else {
      StyleUtils.clearHeight(multiDayPanel.getGrid());
    }
  }

  @Override
  public void doScroll() {
    itemPanel.doScroll(getSettings(), getItemWidgets());
  }

  @Override
  public void doSizing() {
    if (getCalendarWidget().getOffsetHeight() > 0) {
      StyleUtils.setHeight(itemPanel, getCalendarWidget().getOffsetHeight()
          - 2 - dayViewHeader.getOffsetHeight() - multiDayPanel.getOffsetHeight());
    }
  }

  @Override
  public Widget getScrollArea() {
    return itemPanel.getScrollArea();
  }

  @Override
  public String getStyleName() {
    return CalendarStyleManager.DAY_VIEW;
  }

  @Override
  public Type getType() {
    return Type.DAY;
  }

  @Override
  public Range<DateTime> getVisibleRange() {
    JustDate date = getDate();
    if (date == null) {
      return null;
    }

    int days = Math.max(getDisplayedDays(), 1);
    return Range.closedOpen(date.getDateTime(), TimeUtils.nextDay(date, days).getDateTime());
  }

  @Override
  public boolean onClick(long calendarId, Element element, Event event) {
    ItemWidget widget = CalendarUtils.findWidget(getItemWidgets(), element);

    if (widget != null) {
      if (widget.canClick(element)) {
        openItem(widget.getItem());
        return true;
      } else {
        return false;
      }

    } else if (itemPanel.isGrid(element)) {
      timeBlockClick(event);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void onClock() {
    itemPanel.onClock(getSettings());
  }

  private void addItemsToGrid(long calendarId, List<ItemAdapter> adapters,
      boolean multi, int columnIndex, boolean separate, Map<Long, String> attColors) {

    Orientation footerOrientation = multi ? null : Orientation.VERTICAL;
    Long userId = BeeKeeper.getUser().getUserId();

    for (ItemAdapter adapter : adapters) {
      ItemWidget widget = new ItemWidget(adapter.getItem(), multi,
          columnIndex, adapter.getHeight(), footerOrientation);

      widget.setLeft(adapter.getLeft());
      widget.setWidth(adapter.getWidth());

      widget.setTop(adapter.getTop());
      widget.setHeight(adapter.getHeight());

      String bg = null;
      if (separate && attColors != null && widget.isAppointment()) {
        Long sa = ((Appointment) widget.getItem()).getSeparatedAttendee();
        if (sa != null) {
          bg = attColors.get(sa);
        }
      }

      widget.render(calendarId, bg);

      getItemWidgets().add(widget);

      if (multi) {
        multiDayPanel.getGrid().add(widget);
      } else {
        itemPanel.getGrid().add(widget);

        if (widget.getItem().isMovable(userId)) {
          widget.getMoveHandle().addMoveHandler(moveController);
          widget.getMoveHandle().addStyleName(CalendarStyleManager.MOVABLE);
        }
        if (widget.getItem().isResizable(userId)) {
          widget.getResizeHandle().addMoveHandler(resizeController);
          widget.getResizeHandle().addStyleName(CalendarStyleManager.RESIZABLE);
        }
      }
    }
  }

  private void createMoveController() {
    if (moveController == null) {
      moveController = new DayMoveController(this, itemPanel.getScrollArea().getElement());
    }
  }

  private void createResizeController() {
    if (resizeController == null) {
      resizeController = new ResizeController(this, itemPanel.getScrollArea());
    }
  }

  private void timeBlockClick(Event event) {
    DateTime dateTime = itemPanel.getCoordinatesDate(event.getClientX(), event.getClientY(),
        getSettings(), getDate(), getDisplayedDays());
    createAppointment(dateTime, null);
  }
}
