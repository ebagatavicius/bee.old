package com.butent.bee.client.modules.calendar.dnd;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.MoveEvent;
import com.butent.bee.client.modules.calendar.Appointment;
import com.butent.bee.client.modules.calendar.CalendarPanel;
import com.butent.bee.client.modules.calendar.ItemWidget;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.modules.calendar.CalendarUtils;
import com.butent.bee.client.modules.calendar.view.MonthView;
import com.butent.bee.client.modules.tasks.TasksKeeper;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.style.StyleUtils.ScrollBars;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.values.Overflow;
import com.butent.bee.shared.modules.calendar.CalendarConstants.ItemType;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;

public class MonthMoveController implements MoveEvent.Handler {

  private static final int START_SENSITIVITY_PIXELS = 3;

  private static final int WIDTH_RESERVE_PIXELS = 3;
  private static final int HEIGHT_RESERVE_PIXELS = 3;

  private static final int SELECTED_TODO = -2;

  private final MonthView monthView;

  private int headerHeight;

  private ItemWidget itemWidget;
  private int relativeLeft;
  private int relativeTop;

  private int targetLeft;
  private int targetTop;

  private int targetWidth;
  private int targetHeight;

  private int todoWidth;

  private int pointerOffsetX;
  private int pointerOffsetY;

  private int selectedRow = BeeConst.UNDEF;
  private int selectedColumn = BeeConst.UNDEF;

  private final Map<String, String> overflow = new HashMap<>();

  public MonthMoveController(MonthView monthView) {
    this.monthView = monthView;
  }

  @Override
  public void onMove(MoveEvent event) {
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
          && BeeUtils.betweenExclusive(y, getTargetTop() + getHeaderHeight(),
              getTargetTop() + getTargetHeight())) {

        int left = BeeUtils.clamp(x - getPointerOffsetX() - getTargetLeft(), 0,
            getTargetWidth() - WIDTH_RESERVE_PIXELS);
        if (left != getRelativeLeft()) {
          StyleUtils.setLeft(getItemWidget(), left);
          setRelativeLeft(left);
        }

        int top = BeeUtils.clamp(y - getPointerOffsetY() - getTargetTop(), getHeaderHeight(),
            getTargetHeight() - HEIGHT_RESERVE_PIXELS);
        if (top != getRelativeTop()) {
          StyleUtils.setTop(getItemWidget(), top);
          setRelativeTop(top);
        }
      }

      updatePosition();

    } else if (event.isFinished()) {
      if (getItemWidget() != null) {
        drop();
        setItemWidget(null);

        if (!overflow.isEmpty()) {
          for (Map.Entry<String, String> entry : overflow.entrySet()) {
            Element el = DomUtils.getElementQuietly(entry.getKey());

            if (el != null) {
              String ofv = entry.getValue();
              if (BeeUtils.isEmpty(ofv)) {
                el.getStyle().clearOverflow();
              } else {
                el.getStyle().setProperty(StyleUtils.STYLE_OVERFLOW, ofv);
              }
            }
          }

          overflow.clear();
        }
      }
    }
  }

  public void setHeaderHeight(int headerHeight) {
    this.headerHeight = headerHeight;
  }

  private void drop() {
    if (getSelectedRow() >= 0 && getSelectedColumn() >= 0) {
      JustDate date = monthView.getCellDate(getSelectedRow(), getSelectedColumn());

      CalendarItem item = getItemWidget().getItem();

      if (!TimeUtils.sameDate(date, item.getStartTime())) {
        DateTime start = TimeUtils.combine(date, item.getStartTime());
        DateTime end = new DateTime(start.getTime() + item.getDuration());

        switch (item.getItemType()) {
          case APPOINTMENT:
            monthView.updateAppointment((Appointment) item, start, end, BeeConst.UNDEF,
                BeeConst.UNDEF);
            break;

          case TASK:
            TasksKeeper.extendTask(item.getId(), start, end);
            break;
        }
      }

    } else if (getSelectedRow() == SELECTED_TODO) {
      targetTodo(false);

      if (getItemWidget().getItem().getItemType() == ItemType.APPOINTMENT) {
        CalendarUtils.dropOnTodo((Appointment) getItemWidget().getItem(),
            CalendarUtils.getCalendarPanel(getItemWidget()));
      }
    }

    monthView.getCalendarWidget().refresh(false);
  }

  private int getHeaderHeight() {
    return headerHeight;
  }

  private ItemWidget getItemWidget() {
    return itemWidget;
  }

  private int getPointerOffsetX() {
    return pointerOffsetX;
  }

  private int getPointerOffsetY() {
    return pointerOffsetY;
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

  private int getSelectedRow() {
    return selectedRow;
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

  private void setItemWidget(ItemWidget itemWidget) {
    this.itemWidget = itemWidget;
  }

  private void setPointerOffsetX(int pointerOffsetX) {
    this.pointerOffsetX = pointerOffsetX;
  }

  private void setPointerOffsetY(int pointerOffsetY) {
    this.pointerOffsetY = pointerOffsetY;
  }

  private void setRelativeLeft(int relativeLeft) {
    this.relativeLeft = relativeLeft;
  }

  private void setRelativeTop(int relativeTop) {
    this.relativeTop = relativeTop;
  }

  private void setSelectedColumn(int selectedColumn) {
    this.selectedColumn = selectedColumn;
  }

  private void setSelectedRow(int selectedRow) {
    this.selectedRow = selectedRow;
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

    setTargetLeft(target.getElement().getAbsoluteLeft());
    setTargetTop(target.getElement().getAbsoluteTop());

    overflow.clear();

    int width;
    if (widget.getItem().isRemovable(BeeKeeper.getUser().getUserId())
        && panel != null && panel.isTodoVisible()) {

      width = panel.getElement().getClientWidth();
      setTodoWidth(panel.getWidgetSize(panel.getTodoContainer()));

      for (Widget w = target; w != null; w = w.getParent()) {
        String id = w.getElement().getId();
        if (BeeUtils.isEmpty(id) || panel.getId().equals(id)) {
          break;
        }

        String ofv = w.getElement().getStyle().getOverflow();
        if (!Overflow.VISIBLE.getCssName().equals(ofv)) {
          overflow.put(id, ofv);
          StyleUtils.setOverflow(w, ScrollBars.BOTH, Overflow.VISIBLE);
        }
      }

    } else {
      width = target.getElement().getClientWidth();
      setTodoWidth(0);
    }

    setTargetWidth(width);
    setTargetHeight(target.getElement().getClientHeight());

    setRelativeLeft(widget.getElement().getOffsetLeft());
    setRelativeTop(widget.getElement().getOffsetTop());

    setPointerOffsetX(mover.getStartX() - widget.getElement().getAbsoluteLeft());
    setPointerOffsetY(mover.getStartY() - widget.getElement().getAbsoluteTop());

    StyleUtils.setWidth(getItemWidget(), widget.getOffsetWidth());
    getItemWidget().addStyleName(CalendarStyleManager.DRAG);

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

    int row;
    int col;

    if (getTodoWidth() > 0 && x > getTargetWidth() - getTodoWidth()) {
      row = SELECTED_TODO;
      col = SELECTED_TODO;
    } else {
      row = monthView.getRow(getRelativeTop() + getPointerOffsetY());
      col = monthView.getColumn(x);
    }

    if (getSelectedRow() != row || getSelectedColumn() != col) {
      if (getSelectedRow() >= 0 && getSelectedColumn() >= 0) {
        monthView.setCellStyle(getSelectedRow(), getSelectedColumn(),
            CalendarStyleManager.POSITIONER, false);
      } else if (getSelectedRow() == SELECTED_TODO) {
        targetTodo(false);
      }

      if (row >= 0 && col >= 0) {
        monthView.setCellStyle(row, col, CalendarStyleManager.POSITIONER, true);
      } else if (row == SELECTED_TODO) {
        targetTodo(true);
      }

      setSelectedRow(row);
      setSelectedColumn(col);
    }
  }
}
