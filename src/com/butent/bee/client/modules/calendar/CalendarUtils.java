package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Range;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.event.Modifiers;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.calendar.event.AppointmentEvent;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CalendarUtils {

  public static Element createSourceElement(ItemWidget itemWidget) {
    Element element = Document.get().createDivElement();
    element.addClassName(CalendarStyleManager.SOURCE);
    element.setInnerHTML(itemWidget.getElement().getInnerHTML());
    element.addClassName(itemWidget.getStyleName());
    StyleUtils.makeAbsolute(element);
    StyleUtils.setLeft(element, itemWidget.getElement().getOffsetLeft());
    StyleUtils.setTop(element, itemWidget.getElement().getOffsetTop());
    StyleUtils.setWidth(element, itemWidget.getElement().getOffsetWidth());
    StyleUtils.setHeight(element, itemWidget.getElement().getOffsetHeight());
    return element;

  }

  public static void dropOnTodo(final Appointment appointment, final CalendarPanel panel) {
    Assert.notNull(appointment);

    DataInfo srcInfo = Data.getDataInfo(VIEW_APPOINTMENTS);
    DataInfo dstInfo = Data.getDataInfo(TaskConstants.VIEW_TODO_LIST);

    IsRow srcRow = appointment.getRow();
    BeeRow dstRow = RowFactory.createEmptyRow(dstInfo, true);

    Map<String, String> colNames = new HashMap<>();

    colNames.put(COL_SUMMARY, TaskConstants.COL_SUMMARY);
    colNames.put(COL_DESCRIPTION, TaskConstants.COL_DESCRIPTION);

    colNames.put(COL_START_DATE_TIME, TaskConstants.COL_START_TIME);
    colNames.put(COL_END_DATE_TIME, TaskConstants.COL_FINISH_TIME);

    for (Map.Entry<String, String> entry : colNames.entrySet()) {
      int srcIndex = srcInfo.getColumnIndex(entry.getKey());
      String value = BeeConst.isUndef(srcIndex) ? null : srcRow.getString(srcIndex);

      if (!BeeUtils.isEmpty(value)) {
        int dstIndex = dstInfo.getColumnIndex(entry.getValue());
        if (!BeeConst.isUndef(dstIndex)) {
          dstRow.setValue(dstIndex, value);
        }
      }
    }

    if (!appointment.isMultiDay()) {
      long minutes = appointment.getDuration() / TimeUtils.MILLIS_PER_MINUTE;
      if (minutes > 0) {
        dstRow.setValue(dstInfo.getColumnIndex(TaskConstants.COL_EXPECTED_DURATION),
            TimeUtils.renderMinutes(BeeUtils.toInt(minutes), true));
      }
    }

    Long company = srcRow.getLong(srcInfo.getColumnIndex(ClassifierConstants.COL_COMPANY));
    if (DataUtils.isId(company)) {
      RelationUtils.copyWithDescendants(srcInfo, ClassifierConstants.COL_COMPANY, srcRow,
          dstInfo, ClassifierConstants.COL_COMPANY, dstRow);
    }

    Long contact = srcRow.getLong(srcInfo.getColumnIndex(ClassifierConstants.COL_COMPANY_PERSON));
    if (DataUtils.isId(contact)) {
      RelationUtils.copyWithDescendants(srcInfo, ClassifierConstants.COL_COMPANY_PERSON, srcRow,
          dstInfo, ClassifierConstants.COL_CONTACT, dstRow);
    }

    RowFactory.createRow(dstInfo, dstRow, Modality.ENABLED, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        Queries.deleteRowAndFire(VIEW_APPOINTMENTS, appointment.getId());

        if (panel != null) {
          GridView grid = ViewHelper.getChildGrid(panel.getTodoContainer(), GRID_CALENDAR_TODO);
          if (grid != null && !grid.getGrid().containsRow(result.getId())) {
            grid.getViewPresenter().handleAction(Action.REFRESH);
          }
        }
      }
    });
  }

  public static List<CalendarItem> filterByAttendee(Collection<CalendarItem> input, long id) {
    List<CalendarItem> result = new ArrayList<>();

    for (CalendarItem item : input) {
      switch (item.getItemType()) {
        case APPOINTMENT:
          if (((Appointment) item).getAttendees().contains(id)) {
            result.add(item);
          }
          break;

        case TASK:
          break;
      }
    }
    return result;
  }

  public static List<CalendarItem> filterByAttendees(Collection<CalendarItem> input,
      Collection<Long> attIds, boolean separate) {
    List<CalendarItem> result = new ArrayList<>();

    for (CalendarItem item : input) {
      switch (item.getItemType()) {
        case APPOINTMENT:
          Appointment appointment = (Appointment) item;

          for (Long id : appointment.getAttendees()) {
            if (attIds.contains(id)) {
              if (separate) {
                Appointment copy = Appointment.create(appointment.getRow(), id);
                result.add(copy);
              } else {
                result.add(appointment);
                break;
              }
            }
          }
          break;

        case TASK:
          result.add(item);
          break;
      }
    }
    return result;
  }

  public static List<CalendarItem> filterByRange(Collection<CalendarItem> input, JustDate date,
      int days) {
    List<CalendarItem> result = new ArrayList<>();

    long min = TimeUtils.startOfDay(date).getTime();
    long max = TimeUtils.startOfDay(date, days).getTime();

    for (CalendarItem item : input) {
      if (intersects(item, min, max)) {
        result.add(item);
      }
    }
    return result;
  }

  public static List<CalendarItem> filterMulti(Collection<CalendarItem> input, JustDate date,
      int days) {
    List<CalendarItem> result = new ArrayList<>();

    long min = TimeUtils.startOfDay(date).getTime();
    long max = TimeUtils.startOfDay(date, days).getTime();

    for (CalendarItem item : input) {
      if (intersects(item, min, max) && item.isMultiDay()) {
        result.add(item);
      }
    }
    return result;
  }

  public static List<CalendarItem> filterMulti(Collection<CalendarItem> input, JustDate date,
      int days, Collection<Long> attIds, boolean separate) {
    List<CalendarItem> lst = filterMulti(input, date, days);
    if (lst.isEmpty()) {
      return lst;
    }
    return filterByAttendees(lst, attIds, separate);
  }

  public static List<CalendarItem> filterMulti(Collection<CalendarItem> input, JustDate date,
      int days, long id) {
    List<CalendarItem> lst = filterMulti(input, date, days);
    if (lst.isEmpty()) {
      return lst;
    }
    return filterByAttendee(lst, id);
  }

  public static List<CalendarItem> filterSimple(Collection<CalendarItem> input, JustDate date) {
    List<CalendarItem> result = new ArrayList<>();

    long min = TimeUtils.startOfDay(date).getTime();
    long max = TimeUtils.startOfDay(date, 1).getTime();

    for (CalendarItem item : input) {
      if (item.getStartMillis() >= min && item.getEndMillis() <= max) {
        result.add(item);
      }
    }
    return result;
  }

  public static List<CalendarItem> filterSimple(Collection<CalendarItem> input, JustDate date,
      Collection<Long> attIds, boolean separate) {
    List<CalendarItem> lst = filterSimple(input, date);
    if (lst.isEmpty()) {
      return lst;
    }
    return filterByAttendees(lst, attIds, separate);
  }

  public static List<CalendarItem> filterSimple(Collection<CalendarItem> input, JustDate date,
      long id) {
    List<CalendarItem> lst = filterSimple(input, date);
    if (lst.isEmpty()) {
      return lst;
    }
    return filterByAttendee(lst, id);
  }

  public static ItemWidget findWidget(Collection<ItemWidget> widgets, Element element) {
    if (widgets.isEmpty() || element == null) {
      return null;
    }

    for (ItemWidget widget : widgets) {
      if (widget.getElement().isOrHasChild(element)) {
        return widget;
      }
    }
    return null;
  }

  public static CalendarPanel getCalendarPanel(Widget widget) {
    for (Widget p = widget; p != null; p = p.getParent()) {
      if (p instanceof CalendarPanel) {
        return (CalendarPanel) p;
      }
    }
    return null;
  }

  public static int getColumnWidth(Widget widget, int columnCount) {
    int totalWidth = widget.getElement().getClientWidth();
    if (columnCount <= 1) {
      return totalWidth;
    } else {
      return totalWidth * (100 / columnCount) / 100;
    }
  }

  public static int getEndHour(Collection<ItemWidget> widgets) {
    int result = BeeConst.UNDEF;
    if (widgets == null) {
      return result;
    }

    for (ItemWidget widget : widgets) {
      if (!widget.isMulti()) {
        DateTime end = widget.getItem().getEndTime();
        int hour = end.getHour();
        if (end.getMinute() > 0) {
          hour++;
        } else if (hour == 0) {
          hour = TimeUtils.HOURS_PER_DAY;
        }

        if (BeeConst.isUndef(result)) {
          result = hour;
        } else {
          result = Math.max(result, hour);
        }
      }
    }
    return result;
  }

  public static int getEndPixels(CalendarSettings settings, Collection<ItemWidget> widgets) {
    Assert.notNull(settings);

    int hour = settings.getWorkingHourEnd();
    int maxHour = getEndHour(widgets);

    if (hour > 0 || maxHour > 0) {
      hour = Math.max(hour, maxHour);
    } else {
      hour = TimeUtils.HOURS_PER_DAY;
    }

    return hour * settings.getHourHeight();
  }

  public static int getIntervalStartPixels(DateTime dt, CalendarSettings settings) {
    Assert.notNull(dt);
    Assert.notNull(settings);

    int mpi = TimeUtils.MINUTES_PER_HOUR / settings.getIntervalsPerHour();

    return dt.getHour() * settings.getHourHeight()
        + dt.getMinute() / mpi * settings.getPixelsPerInterval();
  }

  public static ItemWidget getItemWidget(Widget child) {
    for (Widget widget = child; widget != null; widget = widget.getParent()) {
      if (widget instanceof ItemWidget) {
        return (ItemWidget) widget;
      }
    }
    return null;
  }

  public static int getMinutes(int y, CalendarSettings settings) {
    int hour = y / settings.getHourHeight();

    int interval = (y - hour * settings.getHourHeight()) / settings.getPixelsPerInterval();
    int minute = interval * TimeUtils.MINUTES_PER_HOUR / settings.getIntervalsPerHour();

    return hour * TimeUtils.MINUTES_PER_HOUR + minute;
  }

  public static int getNowY(CalendarSettings settings) {
    DateTime now = new DateTime();
    int hourHeight = settings.getHourHeight();

    return now.getHour() * hourHeight + now.getMinute() * hourHeight / TimeUtils.MINUTES_PER_HOUR;
  }

  public static int getStartHour(Collection<ItemWidget> widgets) {
    int result = BeeConst.UNDEF;
    if (widgets == null) {
      return result;
    }

    for (ItemWidget widget : widgets) {
      if (!widget.isMulti()) {
        int hour = widget.getItem().getStartTime().getHour();
        if (BeeConst.isUndef(result)) {
          result = hour;
        } else {
          result = Math.min(result, hour);
        }
      }
    }
    return result;
  }

  public static int getStartPixels(CalendarSettings settings, Collection<ItemWidget> widgets) {
    Assert.notNull(settings);

    int hour = settings.getScrollToHour();
    if (hour <= 0) {
      hour = Math.max(settings.getWorkingHourStart(), 0);
    }

    int minHour = getStartHour(widgets);
    if (minHour >= 0) {
      hour = Math.min(hour, minHour);
    }

    return hour * settings.getHourHeight();
  }

  public static int getTodayColumn(JustDate date, int days) {
    int diff = TimeUtils.dayDiff(date, TimeUtils.today());
    return BeeUtils.betweenExclusive(diff, 0, days) ? diff : BeeConst.UNDEF;
  }

  public static boolean isCopying(Modifiers modifiers, ItemWidget itemWidget) {
    if (Modifiers.isNotEmpty(modifiers)) {
      if ((modifiers.isCtrlKey() || modifiers.isAltKey())
          && itemWidget.getItem().getItemType().equals(ItemType.APPOINTMENT)) {
        return true;
      }
    }

    return false;
  }

  public static String renderRange(Range<DateTime> range) {
    return (range == null) ? BeeConst.STRING_EMPTY
        : Format.renderPeriod(range.lowerEndpoint(), range.upperEndpoint());
  }

  private static boolean intersects(CalendarItem item, long min, long max) {
    return item.getStartMillis() < max && item.getEndMillis() > min;
  }

  public static boolean saveAppointment(final RowCallback callback, boolean isNew,
      AppointmentBuilder appointmentBuilder, IsRow originalRow,
      DateTime statDateTime, DateTime endDateTime, String propList, Long reminderType,
      NotificationListener notificationListener, FormView appointmentView) {

    BeeRow row = DataUtils.cloneRow(originalRow);

    final String viewName = VIEW_APPOINTMENTS;

    Data.setValue(viewName, row, COL_START_DATE_TIME, statDateTime);

    Data.setValue(viewName, row, COL_END_DATE_TIME, endDateTime);

    if (appointmentBuilder != null) {
      if (!appointmentBuilder.getColors().isEmpty()) {
        int index = appointmentBuilder.getColorWidget().getSelectedTab();
        if (!BeeUtils.isIndex(appointmentBuilder.getColors(), index)
            && isEmptyAppointmentColumn(row, AdministrationConstants.COL_COLOR)) {
          index = 0;
        }
        if (BeeUtils.isIndex(appointmentBuilder.getColors(), index)) {
          Data.setValue(viewName, row, AdministrationConstants.COL_COLOR,
              appointmentBuilder.getColors().get(index));
        }
      }
    }

    if (appointmentView != null) {
      if (isNew) {
        row.setChildren(appointmentView.getChildrenForInsert());
      } else {
        row.setChildren(appointmentView.getChildrenForUpdate());
      }
    }

    BeeRowSet rowSet;
    List<BeeColumn> columns = CalendarKeeper.getAppointmentViewColumns();
    if (isNew) {
      rowSet = DataUtils.createRowSetForInsert(viewName, columns, row, null, true);
    } else {
      rowSet = new BeeRowSet(viewName, columns);
      rowSet.addRow(row);
    }

    if (!BeeUtils.isEmpty(propList)) {
      rowSet.setTableProperty(TBL_APPOINTMENT_PROPS, propList);
    }

    final String attList = row.getProperty(TBL_APPOINTMENT_ATTENDEES);
    if (!BeeUtils.isEmpty(attList)) {
      rowSet.setTableProperty(TBL_APPOINTMENT_ATTENDEES, attList);
    }

    final String remindList = DataUtils.isId(reminderType) ? reminderType.toString() : null;
    if (!BeeUtils.isEmpty(remindList)) {
      rowSet.setTableProperty(TBL_APPOINTMENT_REMINDERS, remindList);
    }

    final String ownerList = row.getProperty(TBL_APPOINTMENT_OWNERS);
    if (!BeeUtils.isEmpty(ownerList)) {
      rowSet.setTableProperty(TBL_APPOINTMENT_OWNERS, ownerList);
    }

    final String svc = isNew ? SVC_CREATE_APPOINTMENT : SVC_UPDATE_APPOINTMENT;
    ParameterList params = CalendarKeeper.createArgs(svc);

    BeeKeeper.getRpc().sendText(params, Codec.beeSerialize(rowSet), new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasErrors()) {
          notificationListener.notifySevere(response.getErrors());

        } else if (!response.hasResponse(BeeRow.class)) {
          notificationListener.notifySevere(svc, ": response not a BeeRow");

        } else {
          BeeRow result = BeeRow.restore((String) response.getResponse());
          if (result == null) {
            notificationListener.notifySevere(svc, ": cannot restore row");
          } else {

            if (!BeeUtils.isEmpty(attList)) {
              result.setProperty(TBL_APPOINTMENT_ATTENDEES, attList);
            }
            if (!BeeUtils.isEmpty(ownerList)) {
              result.setProperty(TBL_APPOINTMENT_OWNERS, ownerList);
            }
            if (!BeeUtils.isEmpty(propList)) {
              result.setProperty(TBL_APPOINTMENT_PROPS, propList);
            }
            if (!BeeUtils.isEmpty(remindList)) {
              result.setProperty(TBL_APPOINTMENT_REMINDERS, remindList);
            }

            if (isNew) {
              RowInsertEvent.fire(BeeKeeper.getBus(), viewName, result,
                  appointmentBuilder != null ? appointmentBuilder.getFormView().getId() : null);
            } else {
              RowUpdateEvent.fire(BeeKeeper.getBus(), viewName, result);
            }

            Appointment appointment = Appointment.create(result);
            State state = isNew ? State.CREATED : State.CHANGED;
            AppointmentEvent.fire(appointment, state);

            if (callback != null) {
              callback.onSuccess(result);
            }
          }
        }
        if (appointmentBuilder != null) {
          appointmentBuilder.setSaving(false);
        }
      }
    });

    return true;
  }

  public static boolean isEmptyAppointmentColumn(IsRow row, String columnId) {
    return BeeUtils.isEmpty(Data.getString(VIEW_APPOINTMENTS, row, columnId));
  }

  public static void updateWidgetStyleByModifiers(Modifiers modifiers, ItemWidget itemWidget) {
    if (isCopying(modifiers, itemWidget)) {
      itemWidget.addStyleName(CalendarStyleManager.COPY);
    } else {
      itemWidget.removeStyleName(CalendarStyleManager.COPY);
    }
  }

  private CalendarUtils() {
  }
}
