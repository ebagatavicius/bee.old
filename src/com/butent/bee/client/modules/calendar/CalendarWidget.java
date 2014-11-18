package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Range;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.HasOpenHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;

import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.calendar.event.HasTimeBlockClickHandlers;
import com.butent.bee.client.modules.calendar.event.HasUpdateHandlers;
import com.butent.bee.client.modules.calendar.event.TimeBlockClickEvent;
import com.butent.bee.client.modules.calendar.event.UpdateEvent;
import com.butent.bee.client.modules.calendar.view.DayView;
import com.butent.bee.client.modules.calendar.view.MonthView;
import com.butent.bee.client.modules.calendar.view.ResourceView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasState;
import com.butent.bee.shared.State;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.calendar.CalendarConstants.ItemType;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarWidget extends Flow implements HasOpenHandlers<CalendarItem>,
    HasTimeBlockClickHandlers, HasUpdateHandlers, ReadyEvent.HasReadyHandlers, HasState {

  private static final BeeLogger logger = LogUtils.getLogger(CalendarWidget.class);

  private final long calendarId;

  private final CalendarSettings settings;

  private final CalendarDataManager dataManager;
  private final List<Long> attendees = new ArrayList<>();

  private final Map<CalendarView.Type, CalendarView> viewCache = new HashMap<>();

  private final Timer resizeTimer = new Timer() {
    @Override
    public void run() {
      doLayout();
      doSizing();
    }
  };

  private CalendarView view;

  private JustDate date;
  private int displayedDays = BeeConst.UNDEF;

  private boolean layoutSuspended;
  private boolean layoutPending;
  private boolean scrollPending;

  private State state;

  public CalendarWidget(long calendarId, CalendarSettings settings) {
    super();

    this.calendarId = calendarId;
    this.settings = settings;
    this.dataManager = new CalendarDataManager();

    this.date = TimeUtils.today();

    sinkEvents(Event.ONMOUSEDOWN | Event.ONDBLCLICK);
  }

  public void addItem(CalendarItem item) {
    dataManager.addItem(item, settings);
    refresh(false);
  }

  @Override
  public HandlerRegistration addOpenHandler(OpenHandler<CalendarItem> handler) {
    return addHandler(handler, OpenEvent.getType());
  }

  @Override
  public HandlerRegistration addReadyHandler(ReadyEvent.Handler handler) {
    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public HandlerRegistration addTimeBlockClickHandler(TimeBlockClickEvent.Handler handler) {
    return addHandler(handler, TimeBlockClickEvent.getType());
  }

  @Override
  public HandlerRegistration addUpdateHandler(UpdateEvent.Handler handler) {
    return addHandler(handler, UpdateEvent.getType());
  }

  public List<Long> getAttendees() {
    return attendees;
  }

  public JustDate getDate() {
    return date;
  }

  public int getDisplayedDays() {
    return displayedDays;
  }

  public List<CalendarItem> getItems() {
    return dataManager.getItems();
  }

  public CalendarSettings getSettings() {
    return settings;
  }

  @Override
  public State getState() {
    return state;
  }

  public CalendarView.Type getType() {
    if (getView() == null) {
      return null;
    } else {
      return getView().getType();
    }
  }

  public CalendarView getView() {
    return view;
  }

  public void loadItems(boolean force, final boolean scroll) {
    if (getView() != null) {
      final long startMillis = System.currentTimeMillis();
      final Range<DateTime> range = getView().getVisibleRange();

      dataManager.loadItems(calendarId, range, settings, force, new IntCallback() {
        @Override
        public void onSuccess(Integer result) {
          logger.debug("load", CalendarUtils.renderRange(range), result,
              TimeUtils.elapsedMillis(startMillis));
          refresh(scroll);

          if (getState() == null) {
            setState(State.INITIALIZED);

            if (isAttached()) {
              ReadyEvent.fire(CalendarWidget.this);
            }
          }
        }
      });
    }
  }

  @Override
  public void onBrowserEvent(Event event) {
    int eventType = event.getTypeInt();

    switch (eventType) {
      case Event.ONDBLCLICK:
        if (onDoubleClick(EventUtils.getEventTargetElement(event), event)) {
          event.stopPropagation();
          return;
        }
        break;

      case Event.ONMOUSEDOWN:
        if (event.getButton() == NativeEvent.BUTTON_LEFT
            && EventUtils.isCurrentTarget(event, getElement())) {
          if (onMouseDown(EventUtils.getEventTargetElement(event), event)) {
            event.stopPropagation();
            event.preventDefault();
            return;
          }
        }
        break;
    }

    super.onBrowserEvent(event);
  }

  public void onClock() {
    if (getView() != null) {
      getView().onClock();
    }
  }

  @Override
  public void onResize() {
    resizeTimer.schedule(100);
  }

  public void refresh(boolean scroll) {
    if (layoutSuspended) {
      layoutPending = true;
      scrollPending = scroll;
      return;
    }

    dataManager.sort();

    doLayout();
    doSizing();

    if (scroll) {
      doScroll();
    }
  }

  public boolean removeItem(ItemType type, long id, boolean refresh) {
    boolean removed = dataManager.removeItem(type, id);
    if (removed && refresh) {
      refresh(false);
    }
    return removed;
  }

  public void resumeLayout() {
    layoutSuspended = false;
    if (layoutPending) {
      refresh(scrollPending);
    }
  }

  public void setAttendees(Collection<Long> atts, boolean refresh) {
    this.attendees.clear();
    this.attendees.addAll(atts);

    if (refresh) {
      refresh(true);
    }
  }

  @Override
  public void setState(State state) {
    this.state = state;
  }

  public void suspendLayout() {
    layoutSuspended = true;
  }

  public boolean update(CalendarView.Type viewType, JustDate newDate, int days) {
    boolean changed = false;

    if (viewType != null && !viewType.equals(getType())) {
      setType(viewType);
      changed = true;
    }

    if (newDate != null && !newDate.equals(getDate())) {
      setDate(newDate);
      changed = true;
    }

    if (days != getDisplayedDays()) {
      setDisplayedDays(days);
      changed = true;
    }

    if (changed) {
      loadItems(false, true);
    }

    return changed;
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    if (getState() == State.INITIALIZED) {
      ReadyEvent.fire(this);
    }
  }

  private void doLayout() {
    if (getView() != null) {
      long startMillis = System.currentTimeMillis();

      getView().doLayout(calendarId);

      logger.debug("layout", getView().getItemWidgets().size(),
          TimeUtils.elapsedMillis(startMillis));
    }
  }

  private void doScroll() {
    if (getView() != null) {
      getView().doScroll();
    }
  }

  private void doSizing() {
    if (getView() != null) {
      getView().doSizing();
    }
  }

  private boolean onDoubleClick(Element element, Event event) {
    if (getView() != null && settings.isDoubleClick()) {
      return getView().onClick(calendarId, element, event);
    } else {
      return false;
    }
  }

  private boolean onMouseDown(Element element, Event event) {
    if (getView() != null && settings.isSingleClick()) {
      return getView().onClick(calendarId, element, event);
    } else {
      return false;
    }
  }

  private void setDate(JustDate date) {
    this.date = date;
  }

  private void setDisplayedDays(int displayedDays) {
    this.displayedDays = displayedDays;
  }

  private void setType(CalendarView.Type viewType) {
    CalendarView cached = viewCache.get(viewType);
    if (cached != null) {
      setView(cached);
      return;
    }

    CalendarView cv;

    switch (viewType) {
      case DAY:
        cv = new DayView();
        break;

      case MONTH:
        cv = new MonthView();
        break;

      case RESOURCE:
        cv = new ResourceView();
        break;

      default:
        Assert.untouchable();
        cv = null;
    }

    viewCache.put(viewType, cv);
    setView(cv);
  }

  private void setView(CalendarView view) {
    this.view = view;

    view.attach(this);

    setStyleName(view.getStyleName());
  }
}