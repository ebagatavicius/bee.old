package com.butent.bee.client.modules.calendar.view;

import com.google.common.collect.Range;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.modules.calendar.CalendarKeeper;
import com.butent.bee.client.modules.calendar.CalendarUtils;
import com.butent.bee.client.modules.calendar.ItemWidget;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.modules.calendar.CalendarView;
import com.butent.bee.client.modules.calendar.CalendarWidget;
import com.butent.bee.client.modules.calendar.dnd.DayMoveController;
import com.butent.bee.client.modules.calendar.dnd.ResizeController;
import com.butent.bee.client.modules.calendar.layout.ItemAdapter;
import com.butent.bee.client.modules.calendar.layout.ItemPanel;
import com.butent.bee.client.modules.calendar.layout.CalendarLayoutManager;
import com.butent.bee.client.modules.calendar.layout.MultiDayPanel;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Orientation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResourceView extends CalendarView {

  private final ResourceViewHeader viewHeader = new ResourceViewHeader();
  private final MultiDayPanel viewMulti = new MultiDayPanel();
  private final ItemPanel viewBody = new ItemPanel();

  private DayMoveController moveController;
  private ResizeController resizeController;

  public ResourceView() {
    super();
  }

  @Override
  public void attach(CalendarWidget widget) {
    super.attach(widget);

    widget.clear();

    widget.add(viewHeader);
    widget.add(viewMulti);
    widget.add(viewBody);

    createMoveController();
    createResizeController();
  }

  @Override
  public void doLayout(long calendarId) {
    JustDate date = getDate();
    List<Long> attendees = getCalendarWidget().getAttendees();
    int cc = attendees.size();

    viewHeader.setAttendees(calendarId, attendees);
    viewHeader.setDate(date);

    if (TimeUtils.isToday(date)) {
      viewMulti.setColumns(cc, 0, cc - 1);
      viewBody.build(cc, getSettings(), 0, cc - 1);
    } else {
      viewMulti.setColumns(cc, BeeConst.UNDEF, BeeConst.UNDEF);
      viewBody.build(cc, getSettings());
    }

    moveController.setDate(JustDate.copyOf(date));
    moveController.setColumnCount(cc);
    moveController.setSettings(getSettings());

    resizeController.setSettings(getSettings());

    getItemWidgets().clear();

    int multiHeight = BeeConst.UNDEF;

    Map<Long, String> attendeeColors = CalendarKeeper.getAttendeeColors(calendarId);

    for (int i = 0; i < cc; i++) {
      Long id = attendees.get(i);
      String bg = attendeeColors.get(id);

      List<CalendarItem> simple = CalendarUtils.filterSimple(getItems(), date, id);
      if (!simple.isEmpty()) {
        List<ItemAdapter> adapters =
            CalendarLayoutManager.doLayout(simple, i, cc, getSettings());
        addItemsToGrid(calendarId, adapters, false, i, bg);
      }

      List<CalendarItem> multi = CalendarUtils.filterMulti(getItems(), date, 1, id);
      if (!multi.isEmpty()) {
        List<ItemAdapter> adapters = new ArrayList<>();
        for (CalendarItem item : multi) {
          adapters.add(new ItemAdapter(item));
        }

        multiHeight = Math.max(multiHeight,
            CalendarLayoutManager.doMultiLayout(adapters, date, i, cc));
        addItemsToGrid(calendarId, adapters, true, i, bg);
      }
    }

    if (multiHeight > 0) {
      StyleUtils.setHeight(viewMulti.getGrid(), multiHeight);
    } else {
      StyleUtils.clearHeight(viewMulti.getGrid());
    }
  }

  @Override
  public void doScroll() {
    viewBody.doScroll(getSettings(), getItemWidgets());
  }

  @Override
  public void doSizing() {
    if (getCalendarWidget().getOffsetHeight() > 0) {
      StyleUtils.setHeight(viewBody, getCalendarWidget().getOffsetHeight()
          - 2 - viewHeader.getOffsetHeight() - viewMulti.getOffsetHeight());
    }
  }

  @Override
  public Widget getScrollArea() {
    return viewBody.getScrollArea();
  }

  @Override
  public String getStyleName() {
    return CalendarStyleManager.RESOURCE_VIEW;
  }

  @Override
  public Type getType() {
    return Type.RESOURCE;
  }

  @Override
  public Range<DateTime> getVisibleRange() {
    JustDate date = getDate();
    if (date == null) {
      return null;
    } else {
      return Range.closedOpen(date.getDateTime(), TimeUtils.nextDay(date).getDateTime());
    }
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

    } else if (viewBody.isGrid(element)) {
      timeBlockClick(event);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void onClock() {
    viewBody.onClock(getSettings());
  }

  @Override
  public Pair<DateTime, Long> resolveCoordinates(int x, int y) {
    DateTime dateTime = viewBody.getCoordinatesDate(x, y, getSettings(), getDate(), 1);

    List<Long> attendees = getCalendarWidget().getAttendees();
    int columnIndex = viewBody.getColumnIndex(x, attendees.size());

    return Pair.of(dateTime, attendees.get(columnIndex));
  }

  private void addItemsToGrid(long calendarId, List<ItemAdapter> adapters,
      boolean multi, int columnIndex, String bg) {

    Orientation footerOrientation = multi ? null : Orientation.VERTICAL;
    Long userId = BeeKeeper.getUser().getUserId();

    for (ItemAdapter adapter : adapters) {
      ItemWidget widget = new ItemWidget(adapter.getItem(), multi,
          columnIndex, adapter.getHeight(), footerOrientation);

      widget.setLeft(adapter.getLeft());
      widget.setWidth(adapter.getWidth());

      widget.setTop(adapter.getTop());
      widget.setHeight(adapter.getHeight());

      widget.render(calendarId, bg);

      getItemWidgets().add(widget);

      if (multi) {
        viewMulti.getGrid().add(widget);
      } else {
        viewBody.getGrid().add(widget);

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
      moveController = new DayMoveController(this, viewBody.getScrollArea().getElement());
    }
  }

  private void createResizeController() {
    if (resizeController == null) {
      resizeController = new ResizeController(this, viewBody.getScrollArea());
    }
  }

  private void timeBlockClick(Event event) {
    Pair<DateTime, Long> pair = resolveCoordinates(event.getClientX(), event.getClientY());
    if (pair != null) {
      createAppointment(pair.getA(), pair.getB());
    }
  }
}
