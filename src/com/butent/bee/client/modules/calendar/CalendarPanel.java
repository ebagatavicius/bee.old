package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.datepicker.DatePicker;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.VisibilityChangeEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.modules.calendar.CalendarView.Type;
import com.butent.bee.client.modules.calendar.dnd.TodoMoveController;
import com.butent.bee.client.modules.calendar.event.AppointmentEvent;
import com.butent.bee.client.modules.calendar.view.MonthView;
import com.butent.bee.client.modules.calendar.view.ResourceView;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.screen.HandlesStateChange;
import com.butent.bee.client.screen.HasDomain;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasRowId;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.PredefinedFormat;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class CalendarPanel extends Split implements AppointmentEvent.Handler, Presenter, View,
    Printable, VisibilityChangeEvent.Handler, HasWidgetSupplier, HandlesStateChange, HasDomain,
    HandlesAllDataEvents {

  private static final BeeLogger logger = LogUtils.getLogger(CalendarPanel.class);

  private static final String STYLE_PANEL = BeeConst.CSS_CLASS_PREFIX + "cal-Panel";
  private static final String STYLE_PREFIX = STYLE_PANEL + "-";

  private static final String STYLE_CONTROLS = STYLE_PREFIX + "controls";

  private static final String STYLE_TODAY = STYLE_PREFIX + "today";

  private static final String STYLE_NAV_CONTAINER = STYLE_PREFIX + "navContainer";
  private static final String STYLE_NAV_ITEM = STYLE_PREFIX + "navItem";
  private static final String STYLE_NAV_PREV = STYLE_PREFIX + "navPrev";
  private static final String STYLE_NAV_NEXT = STYLE_PREFIX + "navNext";

  private static final String STYLE_DATE = STYLE_PREFIX + "date";

  private static final String STYLE_VIEW_PREFIX = STYLE_PREFIX + "view-";

  private static final String STYLE_CALENDAR = STYLE_PREFIX + "calendar";

  private static final String STYLE_TODO_PREFIX = STYLE_PREFIX + "todo-";
  private static final String STYLE_TODO_CONTAINER = STYLE_TODO_PREFIX + "container";
  private static final String STYLE_TODO_HIDDEN = STYLE_TODO_PREFIX + "hidden";

  private static final EnumSet<UiOption> uiOptions = EnumSet.of(UiOption.VIEW);

  private static boolean hasNonLocalAppointment(ModificationEvent<?> event) {
    return event.isSpookyActionAtADistance() && event.hasView(VIEW_APPOINTMENTS);
  }

  private final long calendarId;

  private final HeaderView header;
  private final CalendarWidget calendar;

  private final Label dateBox;
  private final TabBar viewTabs;

  private final Flow todoContainer;
  private TodoMoveController todoMoveController;

  private BiConsumer<DateTime, Long> timePicker;

  private final List<ViewType> views = new ArrayList<>();

  private final List<HandlerRegistration> registry = new ArrayList<>();

  private boolean enabled = true;

  public CalendarPanel(long calendarId, String caption, CalendarSettings settings,
      BeeRowSet ucAttendees) {

    super(BeeConst.UNDEF);

    addStyleName(STYLE_PANEL);
    addStyleName(UiOption.getStyleName(uiOptions));

    this.calendarId = calendarId;

    this.calendar = new CalendarWidget(calendarId, settings);

    calendar.addOpenHandler(event -> {
      CalendarItem item = event.getTarget();

      switch (item.getItemType()) {
        case APPOINTMENT:
          CalendarKeeper.openAppointment((Appointment) item, getCalendarId());
          break;
        case TASK:
          RowEditor.open(TaskConstants.VIEW_TASKS, item.getId(), Opener.MODAL);
          break;
      }
    });

    calendar.addTimeBlockClickHandler(
        event -> {
          if (Objects.nonNull(timePicker)) {
            timePicker.accept(event.getStart(), event.getAttendeeId());
          } else {
            CalendarKeeper.createAppointment(getCalendarId(), event.getStart(), null,
                event.getAttendeeId(), null, null);
          }
        });

    calendar.addUpdateHandler(event -> {
      if (!updateAppointment(event.getAppointment(), event.getNewStart(), event.getNewEnd(),
          event.getOldColumnIndex(), event.getNewColumnIndex())) {
        event.setCanceled(true);
      }
    });

    calendar.addCopyHandler(event -> {
      copyAppointment(event.getAppointment(), event.getNewStart(), event.getNewEnd());
      event.setCanceled(true);
    });

    this.header = new HeaderImpl();
    header.create(caption, false, true, null, uiOptions,
        EnumSet.of(Action.REFRESH, Action.CONFIGURE, Action.PRINT), Action.NO_ACTIONS,
        Action.NO_ACTIONS);
    header.setViewPresenter(this);

    if (BeeKeeper.getUser().isDataVisible(TaskConstants.VIEW_TODO_LIST)) {
      Button todoListCommand = new Button(Localized.dictionary().crmTodoList());
      todoListCommand.addClickHandler(event -> showTodoList());
      header.addCommandItem(todoListCommand);
    }

    this.dateBox = new Label();
    dateBox.addStyleName(STYLE_DATE);

    dateBox.addClickHandler(event -> pickDate());

    this.viewTabs = new TabBar(STYLE_VIEW_PREFIX, Orientation.HORIZONTAL);
    viewTabs.setKeyboardNavigationEnabled(false);

    viewTabs.addSelectionHandler(event -> {
      Integer index = event.getSelectedItem();
      if (BeeUtils.isIndex(views, index)) {
        activateView(views.get(index));
      }
    });

    Timer timer = new Timer() {
      @Override
      public void run() {
        calendar.onClock();
      }
    };
    timer.scheduleRepeating(TimeUtils.MILLIS_PER_MINUTE);

    addNorth(header, header.getHeight());

    Label today = new Label(Localized.dictionary().calToday());
    today.addStyleName(STYLE_TODAY);

    today.addClickHandler(event -> setDate(TimeUtils.today(), true));

    Label prev = new Label();
    prev.getElement().setInnerText("<");

    prev.addStyleName(STYLE_NAV_ITEM);
    prev.addStyleName(STYLE_NAV_PREV);

    prev.addClickHandler(event -> navigate(false));

    EventUtils.setClickSensitivityMillis(prev, 100);

    Label next = new Label();
    next.getElement().setInnerText(">");

    next.addStyleName(STYLE_NAV_ITEM);
    next.addStyleName(STYLE_NAV_NEXT);

    next.addClickHandler(event -> navigate(true));

    EventUtils.setClickSensitivityMillis(next, 100);

    Flow controls = new Flow();
    controls.addStyleName(STYLE_CONTROLS);

    controls.add(today);

    Horizontal nav = new Horizontal();
    nav.addStyleName(STYLE_NAV_CONTAINER);

    nav.add(prev);
    nav.add(dateBox);
    nav.add(next);
    controls.add(nav);

    controls.add(viewTabs);
    addNorth(controls, 40);

    this.todoContainer = new Flow(STYLE_TODO_CONTAINER);
    addEast(todoContainer, 0, 2);

    addStyleName(STYLE_TODO_HIDDEN);

    Simple container = new Simple();
    container.addStyleName(STYLE_CALENDAR);
    container.setWidget(calendar);
    add(container);

    registry.add(AppointmentEvent.register(this));
    registry.add(VisibilityChangeEvent.register(this));

    registry.addAll(BeeKeeper.getBus().registerDataHandler(this, false));

    updateUcAttendees(ucAttendees, false);

    int viewIndex = updateViews(settings);
    activateView(views.get(viewIndex));
  }

  @Override
  public com.google.gwt.event.shared.HandlerRegistration addReadyHandler(
      ReadyEvent.Handler handler) {
    ReadyEvent.maybeDelegate(this);
    return addHandler(handler, ReadyEvent.getType());
  }

  public long getCalendarId() {
    return calendarId;
  }

  public CalendarView getCalendarView() {
    return calendar.getView();
  }

  @Override
  public String getCaption() {
    return header.getCaption();
  }

  @Override
  public Domain getDomain() {
    return Domain.CALENDAR;
  }

  @Override
  public String getEventSource() {
    return null;
  }

  @Override
  public HeaderView getHeader() {
    return header;
  }

  @Override
  public View getMainView() {
    return this;
  }

  @Override
  public Element getPrintElement() {
    return getElement();
  }

  @Override
  public String getSupplierKey() {
    return CalendarKeeper.getCalendarSupplierKey(getCalendarId());
  }

  public Flow getTodoContainer() {
    return todoContainer;
  }

  @Override
  public String getViewKey() {
    return getSupplierKey();
  }

  @Override
  public Presenter getViewPresenter() {
    return this;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case REFRESH:
        refresh(true);
        break;

      case CONFIGURE:
        CalendarKeeper.editSettings(getCalendarId(), this);
        break;

      case CANCEL:
      case CLOSE:
        BeeKeeper.getScreen().closeWidget(this);
        break;

      case PRINT:
        Printer.print(this);
        break;

      default:
        logger.warning(NameUtils.getName(this), action, "not implemented");
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public boolean isTodoVisible() {
    return !todoContainer.isEmpty() && getWidgetSize(todoContainer) > 0;
  }

  @Override
  public void onAppointment(AppointmentEvent event) {
    if (event.isRelevant(calendar)) {
      if (!DomUtils.isVisible(getElement())) {
        calendar.suspendLayout();
      }
      if (event.isUpdated()) {
        calendar.removeItem(ItemType.APPOINTMENT, event.getAppointment().getId(), false);
      }
      calendar.addItem(event.getAppointment());
    }
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (hasNonLocalAppointment(event) || isRelevant(event)) {
      refresh(false);
    }
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (hasNonLocalAppointment(event) || isRelevant(event)) {
      refresh(false);
    }
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    if (event.hasView(VIEW_APPOINTMENTS) || event.hasView(TaskConstants.VIEW_TASKS)) {
      ItemType type = event.hasView(VIEW_APPOINTMENTS) ? ItemType.APPOINTMENT : ItemType.TASK;

      boolean removed = false;
      for (RowInfo rowInfo : event.getRows()) {
        removed |= calendar.removeItem(type, rowInfo.getId(), false);
      }

      if (removed) {
        refreshCalendar(false);
      }

    } else if (event.hasView(VIEW_CALENDARS)) {
      for (RowInfo rowInfo : event.getRows()) {
        if (Objects.equals(rowInfo.getId(), getCalendarId())) {
          handleAction(Action.CLOSE);
          break;
        }
      }
    }
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    boolean ok;
    String id = source.getId();

    if (getId().equals(id)) {
      int height = source.getClientHeight() + getPrintHeightAdjustment();
      StyleUtils.setSize(target, source.getClientWidth(), height);
      ok = true;

    } else if (calendar.getElement().isOrHasChild(source)) {
      if (StyleUtils.hasClassName(source, CalendarStyleManager.SCROLL_AREA)) {
        int height = source.getClientHeight() + getPrintHeightAdjustment();
        StyleUtils.setHeight(target, height);

        int start = CalendarUtils.getStartPixels(getSettings(),
            calendar.getView().getItemWidgets());
        if (start > 0) {
          target.setScrollTop(start);
        }
      }
      ok = true;

    } else if (dateBox.getId().equals(id)) {
      ok = true;

    } else if (viewTabs.getId().equals(id)) {
      ok = false;

    } else if (StyleUtils.hasAnyClass(source, Sets.newHashSet(STYLE_TODAY, STYLE_NAV_ITEM))) {
      ok = false;

    } else if (header.asWidget().getElement().isOrHasChild(source)) {
      ok = header.onPrint(source, target);

    } else {
      ok = true;
    }

    return ok;
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (event.hasView(VIEW_APPOINTMENTS) || event.hasView(TaskConstants.VIEW_TASKS)) {
      ItemType type = event.hasView(VIEW_APPOINTMENTS) ? ItemType.APPOINTMENT : ItemType.TASK;
      boolean removed = calendar.removeItem(type, event.getRowId(), false);

      if (removed) {
        refreshCalendar(false);
      }

    } else if (event.hasView(VIEW_CALENDARS)) {
      if (Objects.equals(event.getRowId(), getCalendarId())) {
        handleAction(Action.CLOSE);
      }
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (hasNonLocalAppointment(event) || isRelevant(event)) {
      refresh(false);
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (hasNonLocalAppointment(event) || isRelevant(event)) {
      refresh(false);
    }
  }

  @Override
  public void onStateChange(State state) {
    if (State.ACTIVATED.equals(state)) {
      CalendarKeeper.onActivatePanel(this);
    } else if (State.REMOVED.equals(state)) {
      CalendarKeeper.onRemovePanel(getId(), getCalendarId());
    }
  }

  @Override
  public void onViewUnload() {
  }

  @Override
  public void onVisibilityChange(VisibilityChangeEvent event) {
    if (event.isVisible() && DomUtils.isOrHasAncestor(getElement(), event.getId())) {
      calendar.resumeLayout();
    }
  }

  @Override
  public boolean reactsTo(Action action) {
    return EnumUtils.in(action,
        Action.REFRESH, Action.CONFIGURE, Action.CANCEL, Action.CLOSE, Action.PRINT);
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void setEventSource(String eventSource) {
  }

  public void setTimePickerCallback(BiConsumer<DateTime, Long> callback) {
    this.timePicker = callback;
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
  }

  @Override
  protected void onUnload() {
    EventUtils.clearRegistry(registry);

    CalendarKeeper.saveActiveView(getSettings());
    onViewUnload();

    super.onUnload();
  }

  CalendarSettings getSettings() {
    return calendar.getSettings();
  }

  void setDate(JustDate date, boolean sync) {
    if (date != null && !date.equals(calendar.getDate())) {
      calendar.update(calendar.getType(), date, calendar.getDisplayedDays());
      if (sync) {
        CalendarKeeper.synchronizeDate(calendarId, date, false);
      }
      refreshDateBox();
    }
  }

  void updateSettings(BeeRow row, List<BeeColumn> columns, boolean requery) {
    getSettings().loadFrom(row, columns);

    int viewIndex = updateViews(getSettings());
    boolean updated = activateView(views.get(viewIndex));

    if (requery) {
      refresh(true);
    } else if (!updated) {
      refreshCalendar(true);
    }
  }

  void updateUcAttendees(BeeRowSet ucAttendees, boolean refresh) {
    List<Long> attIds = new ArrayList<>();
    if (!DataUtils.isEmpty(ucAttendees)) {
      for (BeeRow row : ucAttendees.getRows()) {
        if (BeeUtils.isTrue(DataUtils.getBoolean(ucAttendees, row, COL_ENABLED))) {
          attIds.add(DataUtils.getLong(ucAttendees, row, COL_ATTENDEE));
        }
      }
    }
    calendar.setAttendees(attIds, refresh);
  }

  private boolean activateView(ViewType view) {
    if (view == null) {
      return false;
    }

    Type type = null;

    JustDate date = calendar.getDate();
    int days = calendar.getDisplayedDays();

    switch (view) {
      case DAY:
        type = Type.DAY;
        days = 1;
        break;

      case DAYS:
        type = Type.DAY;
        days = getSettings().getDefaultDisplayedDays();
        break;

      case WORK_WEEK:
        type = Type.DAY;
        date = TimeUtils.startOfWeek(calendar.getDate());
        days = 5;
        break;

      case WEEK:
        type = Type.DAY;
        days = 7;
        break;

      case MONTH:
        type = Type.MONTH;
        days = BeeConst.UNDEF;
        break;

      case RESOURCES:
        type = Type.RESOURCE;
        days = 1;
        break;
    }

    boolean changed = calendar.update(type, date, days);
    if (changed) {
      CalendarKeeper.synchronizeDate(calendarId, date, false);
      refreshDateBox();
    }

    getSettings().setActiveView(view);
    return changed;
  }

  private int getPrintHeightAdjustment() {
    Widget scrollArea = calendar.getView().getScrollArea();
    if (scrollArea == null) {
      return 0;
    }

    int start = CalendarUtils.getStartPixels(getSettings(), calendar.getView().getItemWidgets());
    int end = CalendarUtils.getEndPixels(getSettings(), calendar.getView().getItemWidgets());

    int height;
    if (end > start) {
      height = end - start + 2;
    } else if (start > 0) {
      height = scrollArea.getElement().getScrollHeight() - start;
    } else {
      height = scrollArea.getElement().getScrollHeight();
    }

    return height - scrollArea.getElement().getClientHeight();
  }

  private boolean isRelevant(ModificationEvent<?> event) {
    if (event.hasView(VIEW_CALENDARS)) {
      if (event instanceof HasRowId) {
        return Objects.equals(((HasRowId) event).getRowId(), getCalendarId());
      } else {
        return true;
      }

    } else if (event.hasView(TaskConstants.VIEW_TASKS)) {
      return CalendarKeeper.showsTasks(getCalendarId());

    } else {
      return false;
    }
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
      int days = (calendar.getView() instanceof ResourceView)
          ? 1 : Math.max(calendar.getDisplayedDays(), 1);
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
    setDate(newDate, true);
  }

  private void pickDate() {
    final Popup popup = new Popup(OutsideClick.CLOSE);
    DatePicker datePicker = new DatePicker(calendar.getDate(), MIN_DATE, MAX_DATE);

    datePicker.addValueChangeHandler(event -> {
      popup.close();
      setDate(event.getValue(), true);
    });

    popup.setWidget(datePicker);
    popup.showRelativeTo(dateBox.getElement());
  }

  private void refresh(boolean scroll) {
    calendar.loadItems(true, scroll);
  }

  private void refreshCalendar(boolean scroll) {
    if (!DomUtils.isVisible(getElement())) {
      calendar.suspendLayout();
    }
    calendar.refresh(scroll);
  }

  private void refreshDateBox() {
    Type type = calendar.getType();

    JustDate date = calendar.getDate();
    int days = calendar.getDisplayedDays();

    String html;
    if (date == null) {
      html = BeeConst.STRING_EMPTY;

    } else if (Type.MONTH.equals(type)) {
      html = Format.render(PredefinedFormat.YEAR_MONTH_STANDALONE, date);

    } else if (type == null || Type.RESOURCE.equals(type) || days <= 1) {
      html = Format.renderDateFull(date);

    } else {
      String from = Format.render(PredefinedFormat.YEAR_MONTH_DAY, date);

      JustDate end = TimeUtils.nextDay(date, days - 1);
      PredefinedFormat pf;

      if (TimeUtils.sameMonth(date, end)) {
        pf = PredefinedFormat.DAY;
      } else if (date.getYear() == end.getYear()) {
        pf = PredefinedFormat.MONTH_DAY;
      } else {
        pf = PredefinedFormat.YEAR_MONTH_DAY;
      }

      html = from + " - " + Format.render(pf, end);
    }

    dateBox.setHtml(html);
  }

  private void showTodoList() {
    if (todoContainer.isEmpty()) {
      if (getOffsetWidth() < 100) {
        BeeKeeper.getScreen().notifyWarning("NO");
        return;
      }

      GridInterceptor interceptor = GridFactory.getGridInterceptor(GRID_CALENDAR_TODO);
      String supplierKey = GridFactory.getSupplierKey(GRID_CALENDAR_TODO, interceptor);

      GridFactory.createGrid(GRID_CALENDAR_TODO, supplierKey, interceptor,
          EnumSet.of(UiOption.EMBEDDED), null, presenter -> {
            if (!todoContainer.isEmpty()) {
              todoContainer.clear();
            }

            int size = Math.min(getOffsetWidth() / 3, 320);
            setWidgetSize(todoContainer, size);
            todoContainer.add(presenter.getMainView());

            removeStyleName(STYLE_TODO_HIDDEN);
          });

      if (todoMoveController == null) {
        todoMoveController = new TodoMoveController(todoContainer, this);
      }

    } else {
      todoContainer.clear();

      addStyleName(STYLE_TODO_HIDDEN);
      setWidgetSize(todoContainer, 0);
    }
  }

  private void copyAppointment(Appointment appointment, DateTime start, DateTime end) {
    if (appointment.handlesCopyAction(start, end)) {
      refreshCalendar(false);
      return;
    }
    final String propList = appointment.getRow().getProperty(TBL_APPOINTMENT_PROPS);
    final Long reminderType = BeeUtils.toLong(
        appointment.getRow().getProperty(TBL_APPOINTMENT_REMINDERS));

    CalendarUtils.saveAppointment(null, true, null, appointment.getRow(), start, end,
        propList, reminderType, BeeKeeper.getScreen(), null);
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

      List<Long> attendees = new ArrayList<>(appointment.getAttendees());

      boolean add = !attendees.contains(newAttendee);

      attendees.remove(oldAttendee);
      if (add) {
        attendees.add(newAttendee);
      }

      appointment.updateAttendees(attendees);

      String viewName = VIEW_APPOINTMENT_ATTENDEES;
      long appId = appointment.getId();

      Queries.delete(viewName, Filter.and(Filter.equals(COL_APPOINTMENT, appId),
          Filter.equals(COL_ATTENDEE, oldAttendee)), null);

      if (add) {
        List<BeeColumn> columns = Lists.newArrayList(Data.getColumn(viewName, COL_APPOINTMENT),
            Data.getColumn(viewName, COL_ATTENDEE));
        List<String> values = Lists.newArrayList(Long.toString(appId), Long.toString(newAttendee));

        Queries.insert(viewName, columns, values);
      }
      changed = true;
    }

    if (appointment.getStartTime().equals(newStart) && appointment.getEndTime().equals(newEnd)) {
      return changed;
    }

    String viewName = VIEW_APPOINTMENTS;
    IsRow row = appointment.getRow();

    List<BeeColumn> columns = Lists.newArrayList(Data.getColumn(viewName, COL_START_DATE_TIME),
        Data.getColumn(viewName, COL_END_DATE_TIME));

    List<String> oldValues = Lists.newArrayList(Data.getString(viewName, row, COL_START_DATE_TIME),
        Data.getString(viewName, row, COL_END_DATE_TIME));
    List<String> newValues = Lists.newArrayList(BeeUtils.toString(newStart.getTime()),
        BeeUtils.toString(newEnd.getTime()));

    Queries.update(viewName, row.getId(), row.getVersion(), columns, oldValues, newValues, null,
        new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            row.setVersion(result.getVersion());
            RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_APPOINTMENTS, result);
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

    for (ViewType view : ViewType.values()) {
      if (!anyVisible || settings.isVisible(view)) {
        if (ViewType.DAYS.equals(view)) {
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
