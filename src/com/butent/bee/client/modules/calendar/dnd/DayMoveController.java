package com.butent.bee.client.modules.calendar.dnd;

import com.google.common.collect.Range;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.event.logical.MoveEvent;
import com.butent.bee.client.modules.calendar.Appointment;
import com.butent.bee.client.modules.calendar.CalendarPanel;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.modules.calendar.CalendarUtils;
import com.butent.bee.client.modules.calendar.CalendarView;
import com.butent.bee.client.modules.calendar.ItemWidget;
import com.butent.bee.client.modules.tasks.TasksKeeper;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.calendar.CalendarConstants.ItemType;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

public class DayMoveController implements MoveEvent.Handler {

  private static final int START_SENSITIVITY_PIXELS = 3;

  private static final int SELECTED_TODO = -2;

  private final CalendarView calendarView;
  private final Element scrollArea;

  private CalendarSettings settings;

  private JustDate date;
  private int columnCount;

  private ItemWidget itemWidget;
  private int relativeLeft;
  private int relativeTop;

  private int sourceWidth;
  private int sourceHeight;

  private int targetLeft;
  private int targetTop;

  private int targetWidth;
  private int targetHeight;

  private int todoWidth;

  private int columnWidth;

  private boolean scrollEnabled;

  private int pointerOffsetX;
  private int pointerOffsetY;

  private Element positioner;
  private int selectedColumn = BeeConst.UNDEF;
  private int selectedMinutes = BeeConst.UNDEF;

  private int maxTop;

  public DayMoveController(CalendarView calendarView, Element scrollArea) {
    super();
    this.calendarView = calendarView;
    this.scrollArea = scrollArea;
  }

  @Override
  public void onMove(MoveEvent event) {
    if (getSettings() == null) {
      return;
    }

    Mover mover = event.getMover();
    if (mover == null) {
      return;
    }

    if (event.isMoving()) {
      if (getItemWidget() == null) {
        if (!startDrag(mover)) {
          return;
        }
      }

      int x = mover.getCurrentX();
      int y = mover.getCurrentY();

      if (BeeUtils.betweenExclusive(x, getTargetLeft(), getTargetLeft() + getTargetWidth())
          && BeeUtils.betweenExclusive(y, getTargetTop(), getTargetTop() + getTargetHeight())) {

        int left = BeeUtils.clamp(x - getPointerOffsetX() - getTargetLeft(), 0,
            getTargetWidth() - getSourceWidth());
        if (left != getRelativeLeft()) {
          StyleUtils.setLeft(getItemWidget(), left);
          setRelativeLeft(left);
        }

        int top = BeeUtils.clamp(y - getPointerOffsetY() - getTargetTop(), 0,
            getTargetHeight() - getSourceHeight());
        if (top != getRelativeTop()) {
          int dy = top - getRelativeTop();

          StyleUtils.setTop(getItemWidget(), top);
          setRelativeTop(top);

          if (isScrollEnabled()) {
            maybeScroll(dy);
          }
        }
      }

      updatePosition();

    } else if (event.isFinished()) {
      if (getPositioner() != null) {
        getPositioner().removeFromParent();
        setPositioner(null);
      }

      if (getItemWidget() != null) {
        drop();
        setItemWidget(null);
      }
    }
  }

  public void setColumnCount(int columnCount) {
    this.columnCount = columnCount;
  }

  public void setDate(JustDate date) {
    this.date = date;
  }

  public void setSettings(CalendarSettings settings) {
    this.settings = settings;
  }

  private void drop() {
    if (getSelectedColumn() >= 0) {
      Range<DateTime> range = getRange(getSelectedColumn(), getSelectedMinutes());
      CalendarItem item = getItemWidget().getItem();

      switch (item.getItemType()) {
        case APPOINTMENT:
          calendarView.updateAppointment((Appointment) item,
              range.lowerEndpoint(), range.upperEndpoint(),
              getItemWidget().getColumnIndex(), getSelectedColumn());
          break;

        case TASK:
          TasksKeeper.extendTask(item.getId(), range.lowerEndpoint(), range.upperEndpoint());
          break;
      }

    } else if (getSelectedColumn() == SELECTED_TODO) {
      targetTodo(false);

      if (getItemWidget().getItem().getItemType() == ItemType.APPOINTMENT) {
        CalendarUtils.dropOnTodo((Appointment) getItemWidget().getItem(),
            CalendarUtils.getCalendarPanel(getItemWidget()));
      }
    }

    calendarView.getCalendarWidget().refresh(false);
  }

  private ItemWidget getItemWidget() {
    return itemWidget;
  }

  private int getColumnCount() {
    return columnCount;
  }

  private int getColumnWidth() {
    return columnWidth;
  }

  private JustDate getDate() {
    return date;
  }

  private int getMaxTop() {
    return maxTop;
  }

  private int getPointerOffsetX() {
    return pointerOffsetX;
  }

  private int getPointerOffsetY() {
    return pointerOffsetY;
  }

  private Element getPositioner() {
    return positioner;
  }

  private Range<DateTime> getRange(int column, int minutes) {
    long startTime = getDate().getDateTime().getTime();

    if (column > 0 && CalendarView.Type.DAY.equals(calendarView.getType())) {
      startTime += TimeUtils.MILLIS_PER_DAY * column;
    }
    if (minutes > 0) {
      startTime += TimeUtils.MILLIS_PER_MINUTE * minutes;
    }

    DateTime start = new DateTime(startTime);
    DateTime end = new DateTime(startTime + getItemWidget().getItem().getDuration());

    return Range.closedOpen(start, end);
  }

  private int getRelativeLeft() {
    return relativeLeft;
  }

  private int getRelativeTop() {
    return relativeTop;
  }

  private int getSelectedColumn() {
    return selectedColumn;
  }

  private int getSelectedMinutes() {
    return selectedMinutes;
  }

  private CalendarSettings getSettings() {
    return settings;
  }

  private int getSourceHeight() {
    return sourceHeight;
  }

  private int getSourceWidth() {
    return sourceWidth;
  }

  private int getTargetHeight() {
    return targetHeight;
  }

  private int getTargetLeft() {
    return targetLeft;
  }

  private int getTargetTop() {
    return targetTop;
  }

  private int getTargetWidth() {
    return targetWidth;
  }

  private int getTodoWidth() {
    return todoWidth;
  }

  private boolean isScrollEnabled() {
    return scrollEnabled;
  }

  private void maybeScroll(int dy) {
    int oldPos = scrollArea.getScrollTop();
    int newPos = BeeConst.UNDEF;

    if (oldPos > 0 && getRelativeTop() < oldPos && dy < 0) {
      newPos = getRelativeTop();

    } else if (dy > 0) {
      int clientHeight = scrollArea.getClientHeight();

      if (getRelativeTop() + getSourceHeight() > oldPos + clientHeight
          && getRelativeTop() > oldPos + clientHeight / 2) {
        newPos = Math.min(oldPos + dy, scrollArea.getScrollHeight() - clientHeight);
      }
    }

    if (newPos >= 0 && newPos != oldPos) {
      scrollArea.setScrollTop(newPos);
      setTargetTop(getItemWidget().getParent().getAbsoluteTop());
    }
  }

  private void setItemWidget(ItemWidget itemWidget) {
    this.itemWidget = itemWidget;
  }

  private void setColumnWidth(int columnWidth) {
    this.columnWidth = columnWidth;
  }

  private void setMaxTop(int maxTop) {
    this.maxTop = maxTop;
  }

  private void setPointerOffsetX(int pointerOffsetX) {
    this.pointerOffsetX = pointerOffsetX;
  }

  private void setPointerOffsetY(int pointerOffsetY) {
    this.pointerOffsetY = pointerOffsetY;
  }

  private void setPositioner(Element positioner) {
    this.positioner = positioner;
  }

  private void setRelativeLeft(int relativeLeft) {
    this.relativeLeft = relativeLeft;
  }

  private void setRelativeTop(int relativeTop) {
    this.relativeTop = relativeTop;
  }

  private void setScrollEnabled(boolean scrollEnabled) {
    this.scrollEnabled = scrollEnabled;
  }

  private void setSelectedColumn(int selectedColumn) {
    this.selectedColumn = selectedColumn;
  }

  private void setSelectedMinutes(int selectedMinutes) {
    this.selectedMinutes = selectedMinutes;
  }

  private void setSourceHeight(int sourceHeight) {
    this.sourceHeight = sourceHeight;
  }

  private void setSourceWidth(int sourceWidth) {
    this.sourceWidth = sourceWidth;
  }

  private void setTargetHeight(int targetHeight) {
    this.targetHeight = targetHeight;
  }

  private void setTargetLeft(int targetLeft) {
    this.targetLeft = targetLeft;
  }

  private void setTargetTop(int targetTop) {
    this.targetTop = targetTop;
  }

  private void setTargetWidth(int targetWidth) {
    this.targetWidth = targetWidth;
  }

  private void setTodoWidth(int todoWidth) {
    this.todoWidth = todoWidth;
  }

  private boolean startDrag(Mover mover) {
    if (Math.abs(mover.getStartX() - mover.getCurrentX()) < START_SENSITIVITY_PIXELS
        && Math.abs(mover.getStartY() - mover.getCurrentY()) < START_SENSITIVITY_PIXELS) {
      return false;
    }

    ItemWidget widget = CalendarUtils.getItemWidget(mover);
    if (widget == null) {
      return false;
    }

    setItemWidget(widget);

    Widget target = widget.getParent();
    CalendarPanel panel = CalendarUtils.getCalendarPanel(widget);

    setColumnWidth(CalendarUtils.getColumnWidth(target, getColumnCount()));

    setSourceWidth(Math.max(widget.getOffsetWidth(), getColumnWidth()));
    setSourceHeight(widget.getOffsetHeight());

    setTargetLeft(target.getElement().getAbsoluteLeft());
    setTargetTop(target.getElement().getAbsoluteTop());

    int width;
    if (widget.getItem().isRemovable(BeeKeeper.getUser().getUserId())
        && panel != null && panel.isTodoVisible()) {

      width = panel.getElement().getClientWidth();
      setTodoWidth(panel.getWidgetSize(panel.getTodoContainer()));

    } else {
      width = target.getElement().getClientWidth();
      setTodoWidth(0);
    }

    setTargetWidth(width);
    setTargetHeight(target.getElement().getClientHeight());

    setScrollEnabled(scrollArea != null
        && scrollArea.getScrollHeight() > scrollArea.getOffsetHeight());

    setRelativeLeft(widget.getElement().getAbsoluteLeft() - getTargetLeft());
    setRelativeTop(widget.getElement().getAbsoluteTop() - getTargetTop());

    setPointerOffsetX(mover.getStartX() - widget.getElement().getAbsoluteLeft());
    setPointerOffsetY(mover.getStartY() - widget.getElement().getAbsoluteTop());

    long maxMillis = TimeUtils.startOfDay(widget.getItem().getStartTime(), 1).getTime()
        - widget.getItem().getDuration();
    setMaxTop(CalendarUtils.getIntervalStartPixels(new DateTime(maxMillis), getSettings()));

    StyleUtils.setWidth(getItemWidget(), getSourceWidth());
    getItemWidget().addStyleName(CalendarStyleManager.DRAG);

    Element ghost = Document.get().createDivElement();
    ghost.addClassName(CalendarStyleManager.POSITIONER);

    StyleUtils.makeAbsolute(ghost);
    StyleUtils.setLeft(ghost, -getColumnWidth() * 2);
    StyleUtils.setSize(ghost, getColumnWidth(), getSourceHeight());

    target.getElement().appendChild(ghost);
    setPositioner(ghost);

    return true;
  }

  private void targetTodo(boolean set) {
    CalendarPanel panel = CalendarUtils.getCalendarPanel(getItemWidget());
    if (panel != null) {
      panel.getTodoContainer().setStyleName(CalendarStyleManager.TARGET, set);
    }
  }

  private void updatePosition() {
    int x = getRelativeLeft() + getPointerOffsetX();
    int y = getRelativeTop() + getPointerOffsetY();

    int column;
    int minutes;

    if (getTodoWidth() > 0 && x > getTargetWidth() - getTodoWidth()) {
      column = SELECTED_TODO;
      minutes = SELECTED_TODO;

    } else {
      column = BeeUtils.clamp(x / getColumnWidth(), 0, getColumnCount() - 1);

      y = Math.min(y / getSettings().getPixelsPerInterval() * getSettings().getPixelsPerInterval(),
          getMaxTop());
      minutes = CalendarUtils.getMinutes(y, getSettings());
    }

    if (getSelectedColumn() != column || getSelectedMinutes() != minutes) {
      if (column >= 0) {
        if (getPositioner() != null) {
          StyleUtils.setLeft(getPositioner(), column * (100 / getColumnCount()), CssUnit.PCT);
          StyleUtils.setTop(getPositioner(), y);

          String text = CalendarUtils.renderRange(getRange(column, minutes));
          getPositioner().setInnerText(text);
        }

        if (getSelectedColumn() == SELECTED_TODO) {
          targetTodo(false);
        }

      } else if (column == SELECTED_TODO) {
        if (getPositioner() != null) {
          getPositioner().setInnerText(Localized.getConstants().crmTodoItem());
        }

        targetTodo(true);
      }

      setSelectedColumn(column);
      setSelectedMinutes(minutes);
    }
  }
}
