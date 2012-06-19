package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.calendar.Calendar;
import com.butent.bee.client.calendar.monthview.MonthView;
import com.butent.bee.client.calendar.resourceview.ResourceView;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.datepicker.DatePicker;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.i18n.DateTimeFormat;
import com.butent.bee.client.layout.Complex;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.calendar.CalendarView.Type;
import com.butent.bee.client.modules.calendar.event.AppointmentEvent;
import com.butent.bee.client.modules.calendar.event.TimeBlockClickEvent;
import com.butent.bee.client.modules.calendar.event.UpdateEvent;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.widget.Html;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.ComparisonFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumSet;
import java.util.List;

public class CalendarPanel extends Complex implements AppointmentEvent.Handler, Presenter, View {

  private static final String STYLE_PANEL = "bee-cal-Panel";
  private static final String STYLE_CONTROLS = "bee-cal-Panel-controls";

  private static final String STYLE_TODAY = "bee-cal-Panel-today";

  private static final String STYLE_NAV_CONTAINER = "bee-cal-Panel-navContainer";
  private static final String STYLE_NAV_ITEM = "bee-cal-Panel-navItem";
  private static final String STYLE_NAV_PREV = "bee-cal-Panel-navPrev";
  private static final String STYLE_NAV_NEXT = "bee-cal-Panel-navNext";

  private static final String STYLE_DATE = "bee-cal-Panel-date";

  private static final String STYLE_VIEW_PREFIX = "bee-cal-Panel-view-";

  private static final String STYLE_CALENDAR = "bee-cal-Panel-calendar";

  private static final DateTimeFormat DATE_FORMAT =
      DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_FULL);
  private static final DateTimeFormat MONTH_FORMAT =
      DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.YEAR_MONTH);

  private final long calendarId;

  private final Calendar calendar;
  private final Html dateBox;

  private final TabBar viewTabs = new TabBar(STYLE_VIEW_PREFIX, false);
  private final List<CalendarConstants.View> views = Lists.newArrayList();

  private final Timer timer;

  private HandlerRegistration appointmentEventRegistration;

  private boolean enabled = true;

  public CalendarPanel(long calendarId, String caption, CalendarSettings settings) {
    super();
    addStyleName(STYLE_PANEL);

    this.calendarId = calendarId;

    this.calendar = new Calendar(settings);

    calendar.addOpenHandler(new OpenHandler<Appointment>() {
      public void onOpen(OpenEvent<Appointment> event) {
        CalendarKeeper.openAppointment(event.getTarget(), false);
      }
    });

    calendar.addTimeBlockClickHandler(new TimeBlockClickEvent.Handler() {
      public void onTimeBlockClick(TimeBlockClickEvent event) {
        CalendarKeeper.createAppointment(event.getStart(), event.getAttendeeId(), false);
      }
    });

    calendar.addUpdateHandler(new UpdateEvent.Handler() {
      @Override
      public void onUpdate(UpdateEvent event) {
        if (!updateAppointment(event.getAppointment(), event.getNewStart(), event.getNewEnd(),
            event.getOldColumnIndex(), event.getNewColumnIndex())) {
          event.setCanceled(true);
        }
      }
    });

    calendar.suspendLayout();

    HeaderView header = GWT.create(HeaderImpl.class);
    header.create(caption, false, true, EnumSet.of(UiOption.ROOT),
        EnumSet.of(Action.REFRESH, Action.CONFIGURE), null);
    header.setViewPresenter(this);

    this.dateBox = new Html();
    dateBox.addStyleName(STYLE_DATE);

    dateBox.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        pickDate();
      }
    });

    int viewIndex = updateViews(settings);
    activateView(views.get(viewIndex), false, true);

    viewTabs.addSelectionHandler(new SelectionHandler<Integer>() {
      public void onSelection(SelectionEvent<Integer> event) {
        Integer index = event.getSelectedItem();
        if (BeeUtils.isIndex(views, index)) {
          activateView(views.get(index), true, false);
        }
      }
    });

    this.timer = new Timer() {
      @Override
      public void run() {
        calendar.onClock();
      }
    };
    timer.scheduleRepeating(TimeUtils.MILLIS_PER_MINUTE);

    add(header);

    Html today = new Html("Šiandien");
    today.addStyleName(STYLE_TODAY);

    today.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        setDate(TimeUtils.today());
      }
    });

    Html prev = new Html("<");
    prev.addStyleName(STYLE_NAV_ITEM);
    prev.addStyleName(STYLE_NAV_PREV);

    prev.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        navigate(false);
      }
    });

    Html next = new Html(">");
    next.addStyleName(STYLE_NAV_ITEM);
    next.addStyleName(STYLE_NAV_NEXT);

    next.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        navigate(true);
      }
    });

    Flow controls = new Flow();
    controls.addStyleName(STYLE_CONTROLS);

    controls.add(today);

    Horizontal nav = new Horizontal();
    nav.addStyleName(STYLE_NAV_CONTAINER);
    nav.add(prev);
    nav.add(next);
    controls.add(nav);

    controls.add(dateBox);

    controls.add(viewTabs);
    add(controls);

    Simple container = new Simple();
    container.addStyleName(STYLE_CALENDAR);
    container.setWidget(calendar);
    add(container);

    refreshDateBox();

    setAppointmentEventRegistration(AppointmentEvent.register(this));

    loadAppointments();
  }

  public String getEventSource() {
    return null;
  }

  public Presenter getViewPresenter() {
    return this;
  }

  public Widget getWidget() {
    return this;
  }

  public String getWidgetId() {
    return getId();
  }

  public void handleAction(Action action) {
    switch (action) {
      case REFRESH:
        refresh();
        break;

      case CONFIGURE:
        CalendarKeeper.editSettings(calendarId, this);
        break;

      case CLOSE:
        BeeKeeper.getScreen().closeView(this);
        break;

      default:
        BeeKeeper.getLog().info(action, "not implemented");
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onAppointment(AppointmentEvent event) {
    if (event.isUpdated()) {
      calendar.removeAppointment(event.getAppointment().getId(), false);
    }
    calendar.addAppointment(event.getAppointment(), true);
  }

  @Override
  public void onResize() {
    super.onResize();
    calendar.onResize();
  }

  public void onViewUnload() {
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setEventSource(String eventSource) {
  }

  public void setViewPresenter(Presenter viewPresenter) {
  }

  @Override
  protected void onUnload() {
    if (!BeeKeeper.getScreen().isTemporaryDetach()) {
      if (getAppointmentEventRegistration() != null) {
        getAppointmentEventRegistration().removeHandler();
        setAppointmentEventRegistration(null);
      }
      
      CalendarKeeper.saveActiveView(getSettings());
    }
    super.onUnload();
  }
  
  CalendarSettings getSettings() {
    return calendar.getSettings();
  }

  void updateSettings(BeeRow row, List<BeeColumn> columns) {
    getSettings().loadFrom(row, columns);

    int viewIndex = updateViews(getSettings());
    activateView(views.get(viewIndex), true, true);
  }

  private void activateView(CalendarConstants.View view, boolean refresh, boolean force) {
    if (view == null) {
      return;
    }
    if (!force && view.equals(calendar.getSettings().getActiveView())) {
      return;
    }

    Type type = null;

    JustDate date = null;
    int days = getSettings().getDefaultDisplayedDays();

    switch (view) {
      case DAY:
        type = Type.DAY;
        days = 1;
        break;

      case DAYS:
        type = Type.DAY;
        break;

      case WORK_WEEK:
        date = TimeUtils.startOfWeek(calendar.getDate());
        type = Type.DAY;
        days = 5;
        break;

      case WEEK:
        type = Type.DAY;
        days = 7;
        break;

      case MONTH:
        type = Type.MONTH;
        break;

      case RESOURCES:
        type = Type.RESOURCE;
        break;
    }

    if (refresh) {
      calendar.suspendLayout();
    }

    if (date != null) {
      calendar.setDate(date, days);
    }
    calendar.setType(type, days);

    if (refresh) {
      calendar.refresh();
      calendar.resumeLayout();
    }

    refreshDateBox();

    getSettings().setActiveView(view);
  }

  private HandlerRegistration getAppointmentEventRegistration() {
    return appointmentEventRegistration;
  }

  private void loadAppointments() {
    ParameterList params =
        CalendarKeeper.createRequestParameters(CalendarConstants.SVC_GET_CALENDAR_APPOINTMENTS);
    params.addQueryItem(CalendarConstants.PARAM_CALENDAR_ID, calendarId);

    BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(BeeRowSet.class)) {
          setAppointments(BeeRowSet.restore((String) response.getResponse()));
        }
      }
    });
  }

  private void navigate(boolean forward) {
    JustDate oldDate = calendar.getDate();
    JustDate newDate;

    if (calendar.getView() instanceof MonthView) {
      if (forward) {
        newDate = TimeUtils.startOfNextMonth(oldDate);
      } else {
        newDate = TimeUtils.startOfPreviousMonth(oldDate);
      }

    } else {
      int days =
          (calendar.getView() instanceof ResourceView) ? 1 : Math.max(calendar.getDisplayedDays(),
              1);
      int shift = days;
      if (days == 5) {
        shift = 7;
      }
      if (!forward) {
        shift = -shift;
      }

      newDate = TimeUtils.nextDay(oldDate, shift);
      if (days == 5) {
        newDate = TimeUtils.startOfWeek(newDate);
      }
    }
    setDate(newDate);
  }

  private void pickDate() {
    final Popup popup = new Popup(true, false);
    DatePicker datePicker = new DatePicker(calendar.getDate());

    datePicker.addValueChangeHandler(new ValueChangeHandler<JustDate>() {
      @Override
      public void onValueChange(ValueChangeEvent<JustDate> event) {
        popup.hide();
        setDate(event.getValue());
      }
    });

    popup.setWidget(datePicker);
    popup.showRelativeTo(dateBox);
  }

  private void refresh() {
    loadAppointments();
  }

  private void refreshDateBox() {
    JustDate date = calendar.getDate();
    DateTimeFormat format = Type.MONTH.equals(calendar.getType()) ? MONTH_FORMAT : DATE_FORMAT;

    dateBox.setHTML(format.format(date));
  }

  private void setAppointmentEventRegistration(HandlerRegistration appointmentEventRegistration) {
    this.appointmentEventRegistration = appointmentEventRegistration;
  }

  private void setAppointments(BeeRowSet rowSet) {
    calendar.suspendLayout();

    String property = rowSet.getTableProperty(CalendarConstants.VIEW_ATTENDEES);
    if (!BeeUtils.isEmpty(property)) {
      calendar.setAttendees(DataUtils.parseList(property));
    }

    List<Appointment> appointments = Lists.newArrayList();
    for (BeeRow row : rowSet.getRows()) {
      Appointment app = new Appointment(row,
          row.getProperty(CalendarConstants.VIEW_APPOINTMENT_ATTENDEES),
          row.getProperty(CalendarConstants.VIEW_APPOINTMENT_PROPS),
          row.getProperty(CalendarConstants.VIEW_APPOINTMENT_REMINDERS));

      appointments.add(app);
    }

    calendar.setAppointments(appointments);
    calendar.resumeLayout();

    BeeKeeper.getLog().debug(calendarId, "loaded", appointments.size(), "appointments");
  }

  private void setDate(JustDate date) {
    if (date != null) {
      if (!date.equals(calendar.getDate())) {
        calendar.setDate(date);
      }
      refreshDateBox();
    }
  }

  private boolean updateAppointment(Appointment appointment, DateTime newStart, DateTime newEnd,
      int oldColumnIndex, int newColumnIndex) {
    boolean changed = false;

    if (Type.RESOURCE.equals(calendar.getView().getType())
        && oldColumnIndex != newColumnIndex
        && BeeUtils.isIndex(calendar.getAttendees(), oldColumnIndex)
        && BeeUtils.isIndex(calendar.getAttendees(), newColumnIndex)) {

      long oldAttendee = calendar.getAttendees().get(oldColumnIndex);
      long newAttendee = calendar.getAttendees().get(newColumnIndex);

      boolean add = !appointment.getAttendees().contains(newAttendee);

      appointment.getAttendees().remove(oldAttendee);
      if (add) {
        appointment.getAttendees().add(newAttendee);
      }

      String viewName = CalendarConstants.VIEW_APPOINTMENT_ATTENDEES;
      long appId = appointment.getId();

      Queries.delete(viewName, Filter.and(
          ComparisonFilter.isEqual(CalendarConstants.COL_APPOINTMENT, new LongValue(appId)),
          ComparisonFilter.isEqual(CalendarConstants.COL_ATTENDEE, new LongValue(oldAttendee))),
          null);

      if (add) {
        List<BeeColumn> columns = Lists.newArrayList(
            Data.getColumn(viewName, CalendarConstants.COL_APPOINTMENT),
            Data.getColumn(viewName, CalendarConstants.COL_ATTENDEE));
        List<String> values = Lists.newArrayList(Long.toString(appId), Long.toString(newAttendee));

        Queries.insert(viewName, columns, values, null);
      }
      changed = true;
    }

    if (appointment.getStart().equals(newStart) && appointment.getEnd().equals(newEnd)) {
      return changed;
    }

    String viewName = CalendarConstants.VIEW_APPOINTMENTS;
    final BeeRow row = appointment.getRow();

    List<BeeColumn> columns = Lists.newArrayList(
        Data.getColumn(viewName, CalendarConstants.COL_START_DATE_TIME),
        Data.getColumn(viewName, CalendarConstants.COL_END_DATE_TIME));

    List<String> oldValues = Lists.newArrayList(
        Data.getString(viewName, row, CalendarConstants.COL_START_DATE_TIME),
        Data.getString(viewName, row, CalendarConstants.COL_END_DATE_TIME));
    List<String> newValues = Lists.newArrayList(
        BeeUtils.toString(newStart.getTime()),
        BeeUtils.toString(newEnd.getTime()));

    Queries.update(viewName, row.getId(), row.getVersion(), columns, oldValues, newValues,
        new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            row.setVersion(result.getVersion());
          }
        });

    appointment.setStart(newStart);
    appointment.setEnd(newEnd);

    return true;
  }

  private int updateViews(CalendarSettings settings) {
    if (!views.isEmpty()) {
      viewTabs.clear();
      views.clear();
    }

    boolean anyVisible = settings.isAnyVisible();
    String caption;

    for (CalendarConstants.View view : CalendarConstants.View.values()) {
      if (!anyVisible || settings.isVisible(view)) {
        if (CalendarConstants.View.DAYS.equals(view)) {
          caption = view.getCaption(settings.getDefaultDisplayedDays());
        } else {
          caption = view.getCaption();
        }

        viewTabs.addItem(caption);
        views.add(view);
      }
    }

    int index;
    if (settings.getActiveView() != null && views.contains(settings.getActiveView())) {
      index = views.indexOf(settings.getActiveView());
    } else {
      index = 0;
    }

    viewTabs.selectTab(index, false);
    return index;
  }
}
