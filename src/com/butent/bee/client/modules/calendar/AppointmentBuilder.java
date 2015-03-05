package com.butent.bee.client.modules.calendar;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Element;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.InputBoxes;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.i18n.DateTimeFormat;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.calendar.event.AppointmentEvent;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputTime;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasItems;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class AppointmentBuilder extends AbstractFormInterceptor implements SelectorEvent.Handler {

  private class DateOrTimeWidgetHandler implements BlurHandler {
    @Override
    public void onBlur(BlurEvent event) {
      checkOverlap(true);
    }
  }

  private final class ModalCallback extends InputCallback {

    final RowCallback saveCallback;

    private ModalCallback(RowCallback saveCallback) {
      this.saveCallback = saveCallback;
    }

    @Override
    public String getErrorMessage() {
      if (getFormView().checkOnSave(null) && AppointmentBuilder.this.validate()) {
        return null;
      } else {
        return InputBoxes.SILENT_ERROR;
      }
    }

    @Override
    public void onClose(final CloseCallback closeCallback) {
      Assert.notNull(closeCallback);
      if (!getFormView().checkOnClose(null)) {
        return;
      }

      IsRow oldRow = getFormView().getOldRow();
      IsRow newRow = getFormView().getActiveRow();

      if (oldRow == null || newRow == null) {
        closeCallback.onClose();
        return;
      }

      List<String> changes = new ArrayList<>();

      BeeRowSet rowSet = DataUtils.getUpdated(VIEW_APPOINTMENTS,
          CalendarKeeper.getAppointmentViewColumns(), oldRow, newRow, null);
      if (!DataUtils.isEmpty(rowSet)) {
        changes.addAll(Localized.getLabels(rowSet.getColumns()));
      }

      Long oldService = null;
      Long oldRepair = null;
      for (Long prop : DataUtils.parseIdSet(oldRow.getProperty(TBL_APPOINTMENT_PROPS))) {
        if (serviceTypes.contains(prop)) {
          oldService = prop;
        } else if (repairTypes.contains(prop)) {
          oldRepair = prop;
        }
      }

      Long newService = getSelectedId(getServiceTypeWidgetId(), serviceTypes);
      if (oldService == null && newService != null && newService.equals(defaultServiceType)) {
        newService = null;
      }
      Long newRepair = getSelectedId(getRepairTypeWidgetId(), repairTypes);
      if (oldRepair == null && newRepair != null && newRepair.equals(defaultRepairType)) {
        newRepair = null;
      }

      if (!Objects.equals(oldService, newService)) {
        changes.add(Localized.getConstants().calServiceType());
      }
      if (!Objects.equals(oldRepair, newRepair)) {
        changes.add(Localized.getConstants().calRepairType());
      }

      if (!isNew && !DataUtils.sameIdSet(oldRow.getProperty(TBL_APPOINTMENT_ATTENDEES),
          newRow.getProperty(TBL_APPOINTMENT_ATTENDEES))) {
        changes.add(Localized.getConstants().calAttendees());
      }
      if (!DataUtils.sameIdSet(oldRow.getProperty(TBL_APPOINTMENT_OWNERS),
          newRow.getProperty(TBL_APPOINTMENT_OWNERS))) {
        changes.add(Localized.getConstants().responsiblePersons());
      }

      DateTime oldStart = Data.getDateTime(VIEW_APPOINTMENTS, oldRow, COL_START_DATE_TIME);
      DateTime newStart = getStart();
      if (!Objects.equals(oldStart, newStart)) {
        changes.add(Localized.getConstants().calAppointmentStart());
      }

      DateTime oldEnd = Data.getDateTime(VIEW_APPOINTMENTS, oldRow, COL_END_DATE_TIME);
      DateTime newEnd = getEnd(newStart);
      if (!Objects.equals(oldEnd, newEnd) && !isNew) {
        changes.add(Localized.getConstants().calAppointmentEnd());
      }

      List<Long> reminders = new ArrayList<>();
      Long reminderType = getSelectedId(getReminderWidgetId(), reminderTypes);
      if (reminderType != null) {
        reminders.add(reminderType);
      }
      if (!DataUtils.sameIdSet(oldRow.getProperty(TBL_APPOINTMENT_REMINDERS), reminders)) {
        changes.add(Localized.getConstants().calReminder());
      }

      Long oldColor = Data.getLong(VIEW_APPOINTMENTS, oldRow, AdministrationConstants.COL_COLOR);
      Long newColor = BeeUtils.getQuietly(colors, colorWidget.getSelectedTab());
      if (oldColor != null && newColor != null && !oldColor.equals(newColor)) {
        changes.add(Localized.getConstants().color());
      }

      if (changes.isEmpty()) {
        closeCallback.onClose();
        return;
      }

      List<String> messages = new ArrayList<>();

      String msg = isNew ? Localized.getConstants().newValues()
          : Localized.getConstants().changedValues();
      messages.add(msg + BeeConst.STRING_SPACE
          + BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR, changes));

      messages.add(isNew ? Localized.getConstants().calCreateNewAppointment()
          : Localized.getConstants().saveChanges());

      DecisionCallback callback = new DecisionCallback() {
        @Override
        public void onCancel() {
          UiHelper.focus(getFormView().asWidget());
        }

        @Override
        public void onConfirm() {
          closeCallback.onSave();
        }

        @Override
        public void onDeny() {
          closeCallback.onClose();
        }
      };

      String cap = isNew
          ? CalendarKeeper.getAppointmentViewInfo().getNewRowCaption() : getFormView().getCaption();
      Global.decide(cap, messages, callback, DialogConstants.DECISION_YES);
    }

    @Override
    public void onDelete(final DialogBox dialog) {
      IsRow row = getFormView().getActiveRow();
      if (row == null) {
        return;
      }

      final long id = row.getId();
      final long version = row.getVersion();

      if (!DataUtils.isId(id)) {
        return;
      }

      Global.confirmDelete(Data.getString(VIEW_APPOINTMENTS, row, COL_SUMMARY), Icon.WARNING,
          Collections.singletonList(Localized.getConstants().calDeleteAppointment()),
          new ConfirmationCallback() {
            @Override
            public void onConfirm() {
              dialog.close();

              Queries.deleteRow(VIEW_APPOINTMENTS, id, version, new Queries.IntCallback() {
                @Override
                public void onFailure(String... reason) {
                  BeeKeeper.getScreen().notifySevere(reason);
                }

                @Override
                public void onSuccess(Integer result) {
                  RowDeleteEvent.fire(BeeKeeper.getBus(), VIEW_APPOINTMENTS, id);
                }
              });
            }
          });
    }

    @Override
    public void onSuccess() {
      AppointmentBuilder.this.save(saveCallback);
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(AppointmentBuilder.class);

  private static final String NAME_SERVICE_TYPE = "ServiceType";
  private static final String NAME_REPAIR_TYPE = "RepairType";

  private static final String NAME_RESOURCES = "Resources";
  private static final String NAME_OVERLAP = "Overlap";

  private static final String NAME_START_DATE = "StartDate";
  private static final String NAME_START_TIME = "StartTime";
  private static final String NAME_END_DATE = "EndDate";
  private static final String NAME_END_TIME = "EndTime";

  private static final String NAME_HOURS = "Hours";
  private static final String NAME_MINUTES = "Minutes";

  private static final String NAME_COLORS = "Colors";

  private static final String NAME_REMINDER = "Reminder";

  private static final String NAME_BUILD_SEPARATOR = "BuildSeparator";
  private static final String NAME_BUILD = "Build";
  private static final String NAME_BUILD_INFO = "BuildInfo";

  private static final String STYLE_COLOR_BAR_PREFIX = BeeConst.CSS_CLASS_PREFIX
      + "cal-ColorBar-";

  static BeeRow createEmptyRow(BeeRow typeRow, DateTime start, CalendarVisibility visibility) {
    BeeRow row = RowFactory.createEmptyRow(CalendarKeeper.getAppointmentViewInfo(), true);

    if (typeRow != null) {
      RelationUtils.updateRow(CalendarKeeper.getAppointmentViewInfo(), COL_APPOINTMENT_TYPE, row,
          Data.getDataInfo(VIEW_APPOINTMENT_TYPES), typeRow, true);
    }

    if (start != null) {
      Data.setValue(VIEW_APPOINTMENTS, row, COL_START_DATE_TIME, start);
    }

    if (visibility != null) {
      Data.setValue(VIEW_APPOINTMENTS, row, COL_VISIBILITY, visibility.ordinal());
    }

    return row;
  }

  private final boolean isNew;

  private ModalCallback modalCallback;

  private final DateOrTimeWidgetHandler dateOrTimeWidgetHandler = new DateOrTimeWidgetHandler();

  private final List<Long> serviceTypes = new ArrayList<>();
  private Long defaultServiceType;

  private final List<Long> repairTypes = new ArrayList<>();
  private Long defaultRepairType;

  private final List<Long> colors = new ArrayList<>();

  private final SetMultimap<Long, Long> serviceResources = HashMultimap.create();
  private final SetMultimap<Long, Long> repairResources = HashMultimap.create();

  private final List<Long> reminderTypes = new ArrayList<>();

  private String serviceTypeWidgetId;
  private String repairTypeWidgetId;

  private String resourceWidgetId;
  private String overlapWidgetId;

  private String startDateWidgetId;
  private String startTimeWidgetId;
  private String endDateWidgetId;
  private String endTimeWidgetId;

  private String hourWidgetId;
  private String minuteWidgetId;

  private String reminderWidgetId;

  private String buildInfoWidgetId;

  private final TabBar colorWidget = new TabBar(STYLE_COLOR_BAR_PREFIX, Orientation.HORIZONTAL);

  private boolean saving;

  private boolean overlapVisible;
  private final List<Appointment> overlappingAppointments = new ArrayList<>();

  private DateTime lastCheckStart;
  private DateTime lastCheckEnd;

  private final Set<String> requiredFields = new HashSet<>();

  private final List<Long> ucAttendees = new ArrayList<>();

  AppointmentBuilder(boolean isNew) {
    super();
    this.isNew = isNew;

    addColorHandlers();
  }

  @Override
  public void afterCreate(FormView form) {
    loadProperties();
    loadReminders();
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, NAME_SERVICE_TYPE)) {
      setServiceTypeWidgetId(widget.getId());

    } else if (BeeUtils.same(name, NAME_REPAIR_TYPE)) {
      setRepairTypeWidgetId(widget.getId());
      if (widget instanceof HasChangeHandlers) {
        ((HasChangeHandlers) widget).addChangeHandler(new ChangeHandler() {
          @Override
          public void onChange(ChangeEvent event) {
            updateDuration();
            checkOverlap(true);
          }
        });
      }

    } else if (BeeUtils.same(name, NAME_RESOURCES)) {
      setResourceWidgetId(widget.getId());
      if (widget instanceof DataSelector) {
        ((DataSelector) widget).addSelectorHandler(this);
      }

    } else if (BeeUtils.same(name, NAME_OVERLAP)) {
      setOverlapWidgetId(widget.getId());
      if (widget instanceof HasClickHandlers) {
        ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            showOverlappingAppointments();
          }
        });
      }
      widget.asWidget().getElement().getStyle().setVisibility(Visibility.HIDDEN);
      setOverlapVisible(false);

    } else if (BeeUtils.same(name, NAME_START_DATE)) {
      setStartDateWidgetId(widget.getId());
      if (widget instanceof Editor) {
        ((Editor) widget).addBlurHandler(dateOrTimeWidgetHandler);
      }
      if (widget instanceof InputDate) {
        setDateBounds((InputDate) widget);
      }

    } else if (BeeUtils.same(name, NAME_START_TIME)) {
      setStartTimeWidgetId(widget.getId());
      if (widget instanceof Editor) {
        ((Editor) widget).addBlurHandler(dateOrTimeWidgetHandler);
      }

    } else if (BeeUtils.same(name, NAME_END_DATE)) {
      setEndDateWidgetId(widget.getId());
      if (widget instanceof Editor) {
        ((Editor) widget).addBlurHandler(dateOrTimeWidgetHandler);
      }
      if (widget instanceof InputDate) {
        setDateBounds((InputDate) widget);
        ((InputDate) widget).addFocusHandler(new FocusHandler() {
          @Override
          public void onFocus(FocusEvent event) {
            onEndDateFocus((InputDate) event.getSource());
          }
        });
      }

    } else if (BeeUtils.same(name, NAME_END_TIME)) {
      setEndTimeWidgetId(widget.getId());
      if (widget instanceof Editor) {
        ((Editor) widget).addBlurHandler(dateOrTimeWidgetHandler);
      }
      if (widget instanceof InputTime) {
        ((InputTime) widget).addFocusHandler(new FocusHandler() {
          @Override
          public void onFocus(FocusEvent event) {
            onEndTimeFocus((InputTime) event.getSource());
          }
        });
      }

    } else if (BeeUtils.same(name, NAME_HOURS)) {
      setHourWidgetId(widget.getId());
      if (widget instanceof Editor) {
        ((Editor) widget).addBlurHandler(dateOrTimeWidgetHandler);
      }
    } else if (BeeUtils.same(name, NAME_MINUTES)) {
      setMinuteWidgetId(widget.getId());
      if (widget instanceof Editor) {
        ((Editor) widget).addBlurHandler(dateOrTimeWidgetHandler);
      }

    } else if (BeeUtils.same(name, NAME_COLORS) && widget instanceof HasWidgets) {
      ((HasWidgets) widget).add(colorWidget);
      initColorWidget();

    } else if (BeeUtils.same(name, NAME_REMINDER)) {
      setReminderWidgetId(widget.getId());

    } else if (BeeUtils.same(name, NAME_BUILD)) {
      if (widget instanceof HasClickHandlers) {
        ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            buildIncrementally();
          }
        });
      }
    } else if (BeeUtils.same(name, NAME_BUILD_INFO)) {
      setBuildInfoWidgetId(widget.getId());
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    if (row == null) {
      return;
    }

    DateTime start = Data.getDateTime(VIEW_APPOINTMENTS, row, COL_START_DATE_TIME);
    DateTime end = Data.getDateTime(VIEW_APPOINTMENTS, row, COL_END_DATE_TIME);

    if (!BeeUtils.isEmpty(getStartDateWidgetId())) {
      getInputDate(getStartDateWidgetId()).setDate(start);
    }
    if (!BeeUtils.isEmpty(getStartTimeWidgetId())) {
      getInputTime(getStartTimeWidgetId()).setTime(start);
    }

    if (!BeeUtils.isEmpty(getEndDateWidgetId())) {
      getInputDate(getEndDateWidgetId()).setDate(end);
    }
    if (!BeeUtils.isEmpty(getEndTimeWidgetId())) {
      getInputTime(getEndTimeWidgetId()).setTime(end);
    }
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    switch (action) {
      case SAVE:
        if (!isSaving() && validate() && save(null)) {
          presenter.handleAction(Action.CLOSE);
        }
        return false;

      case REFRESH:
        loadProperties();
        loadReminders();

        getFormView().refresh(true, true);
        return false;

      default:
        return true;
    }
  }

  @Override
  public boolean beforeCreateWidget(String name, Element description) {
    return isNew || BeeUtils.isEmpty(name)
        || !BeeUtils.inListSame(name, NAME_BUILD_SEPARATOR, NAME_BUILD, NAME_BUILD_INFO);
  }

  @Override
  public FormInterceptor getInstance() {
    Assert.untouchable();
    return null;
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isOpened()) {
      Set<Long> include = new HashSet<>(ucAttendees);
      Set<Long> exclude = new HashSet<>();

      if (!serviceResources.isEmpty() && !BeeUtils.isEmpty(getServiceTypeWidgetId())) {
        Long serviceType = getSelectedId(getServiceTypeWidgetId(), serviceTypes);
        if (serviceType != null) {
          for (Long resource : serviceResources.keySet()) {
            if (!serviceResources.containsEntry(resource, serviceType)) {
              exclude.add(resource);
            }
          }
        }
      }

      if (!repairResources.isEmpty() && !BeeUtils.isEmpty(getRepairTypeWidgetId())) {
        Long repairType = getSelectedId(getRepairTypeWidgetId(), repairTypes);
        if (repairType != null) {
          for (Long resource : repairResources.keySet()) {
            if (!repairResources.containsEntry(resource, repairType)) {
              exclude.add(resource);
            }
          }
        }
      }

      Filter filter;
      if (include.isEmpty()) {
        filter = exclude.isEmpty() ? null : Filter.idNotIn(exclude);

      } else if (exclude.isEmpty()) {
        filter = Filter.idIn(include);

      } else {
        include.removeAll(exclude);
        filter = include.isEmpty() ? Filter.isFalse() : Filter.idIn(include);
      }

      event.getSelector().setAdditionalFilter(filter);
    }
  }

  @Override
  public void onStart(FormView form) {
    form.setEditing(true);
  }

  ModalCallback getModalCallback(RowCallback saveCallback) {
    if (saveCallback == null) {
      if (modalCallback == null) {
        setModalCallback(new ModalCallback(null));
      }
      return modalCallback;

    } else {
      return new ModalCallback(saveCallback);
    }
  }

  void initPeriod(DateTime start) {
    if (start == null) {
      return;
    }

    InputTime inputTime = getInputTime(getEndTimeWidgetId());
    if (inputTime == null) {
      return;
    }

    String options = inputTime.getOptions();
    if (!BeeUtils.isPositiveInt(options)) {
      return;
    }

    int minutes = TimeUtils.minutesSinceDayStarted(start) + BeeUtils.toInt(options);
    if (minutes < TimeUtils.MINUTES_PER_DAY) {
      inputTime.setMinutes(minutes);
    }
  }

  boolean isRequired(String name) {
    return requiredFields.contains(name);
  }

  void setColor(Long color) {
    if (color != null && colors.contains(color)) {
      colorWidget.selectTab(colors.indexOf(color));
    }
  }

  void setDuration(String duration) {
    List<Integer> fields = TimeUtils.parseFields(duration);

    if (fields.size() >= 1 && !BeeUtils.isEmpty(getHourWidgetId())) {
      Integer hours = fields.get(0);

      if (BeeUtils.isPositive(hours)) {
        Widget widget = getWidget(getHourWidgetId());
        if (widget instanceof Editor) {
          ((Editor) widget).setValue(BeeUtils.toString(hours));
        }
      }
    }

    if (fields.size() >= 2 && !BeeUtils.isEmpty(getMinuteWidgetId())) {
      Integer minutes = fields.get(1);

      if (BeeUtils.isPositive(minutes)) {
        Widget widget = getWidget(getMinuteWidgetId());
        if (widget instanceof Editor) {
          ((Editor) widget).setValue(BeeUtils.toString(minutes));
        }
      }
    }
  }

  void setProperties(List<Long> properties) {
    int serviceTypeIndex = BeeConst.UNDEF;
    int repairTypeIndex = BeeConst.UNDEF;

    for (Long id : properties) {
      if (serviceTypes.contains(id)) {
        serviceTypeIndex = serviceTypes.indexOf(id);
      } else if (repairTypes.contains(id)) {
        repairTypeIndex = repairTypes.indexOf(id);
      }
    }

    setSelectedIndex(getListBox(getServiceTypeWidgetId()), serviceTypeIndex);
    setSelectedIndex(getListBox(getRepairTypeWidgetId()), repairTypeIndex);
  }

  void setReminders(List<Long> reminders) {
    int reminderIndex = BeeConst.UNDEF;

    for (Long id : reminders) {
      if (reminderTypes.contains(id)) {
        reminderIndex = reminderTypes.indexOf(id);
      }
    }

    setSelectedIndex(getListBox(getReminderWidgetId()), reminderIndex);
  }

  void setRequiredFields(String fieldNames) {
    BeeUtils.overwrite(requiredFields, NameUtils.toSet(fieldNames));
  }

  void setUcAttendees(List<Long> attendees) {
    BeeUtils.overwrite(ucAttendees, attendees);
  }

  private void addColorHandlers() {
    colorWidget.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
      @Override
      public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
        Widget widget = colorWidget.getSelectedWidget();
        if (widget != null) {
          widget.getElement().setInnerHTML(BeeConst.STRING_EMPTY);
        }
      }
    });

    colorWidget.addSelectionHandler(new SelectionHandler<Integer>() {
      @Override
      public void onSelection(SelectionEvent<Integer> event) {
        Widget widget = colorWidget.getSelectedWidget();
        if (widget != null) {
          widget.getElement().setInnerHTML("X");
        }
      }
    });
  }

  private void buildIncrementally() {
    if (isSaving() || !validate()) {
      return;
    }

    save(new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        reset(result);
      }
    });
  }

  private void checkOverlap(boolean whenPeriodChanged) {
    List<Long> resources = getResources(getFormView().getActiveRow());
    if (resources.isEmpty()) {
      hideOverlap();
      return;
    }

    List<Long> opaqueResources = new ArrayList<>();
    for (Long id : resources) {
      if (CalendarKeeper.isAttendeeOpaque(id)) {
        opaqueResources.add(id);
      }
    }
    if (opaqueResources.isEmpty()) {
      hideOverlap();
      return;
    }

    DateTime start = getStart();
    DateTime end = getEnd(start);
    if (start == null || end == null || TimeUtils.isLeq(end, start)) {
      hideOverlap();
      return;
    }

    if (whenPeriodChanged && start.equals(getLastCheckStart()) && end.equals(getLastCheckEnd())) {
      return;
    }
    setLastCheckStart(start);
    setLastCheckEnd(end);

    ParameterList params = CalendarKeeper.createArgs(SVC_GET_OVERLAPPING_APPOINTMENTS);
    if (!isNew) {
      params.addQueryItem(PARAM_APPOINTMENT_ID, getFormView().getActiveRow().getId());
    }
    params.addQueryItem(PARAM_APPOINTMENT_START, start.getTime());
    params.addQueryItem(PARAM_APPOINTMENT_END, end.getTime());
    params.addQueryItem(PARAM_ATTENDEES, DataUtils.buildIdList(opaqueResources));

    BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        overlappingAppointments.clear();

        if (response.hasResponse(BeeRowSet.class)) {
          BeeRowSet rowSet = BeeRowSet.restore((String) response.getResponse());
          for (BeeRow row : rowSet.getRows()) {
            Appointment app = new Appointment(row);
            overlappingAppointments.add(app);
          }
        }
        showOverlap(!overlappingAppointments.isEmpty());
      }
    });
  }

  private String getBuildInfoWidgetId() {
    return buildInfoWidgetId;
  }

  private int getDuration() {
    int result = 0;

    if (!BeeUtils.isEmpty(getHourWidgetId())) {
      Widget widget = getWidget(getHourWidgetId());
      if (widget instanceof Editor) {
        String hours = ((Editor) widget).getNormalizedValue();
        if (BeeUtils.isPositiveInt(hours)) {
          result += BeeUtils.toInt(hours) * 60;
        }
      }
    }

    if (!BeeUtils.isEmpty(getMinuteWidgetId())) {
      Widget widget = getWidget(getMinuteWidgetId());
      if (widget instanceof Editor) {
        String minutes = ((Editor) widget).getNormalizedValue();
        if (BeeUtils.isPositiveInt(minutes)) {
          result += BeeUtils.toInt(minutes);
        }
      }
    }
    return result;
  }

  private DateTime getEnd(DateTime start) {
    HasDateValue datePart = BeeUtils.isEmpty(getEndDateWidgetId())
        ? null : getInputDate(getEndDateWidgetId()).getDate();
    Long timeMillis = getMillis(getEndTimeWidgetId());

    if (datePart == null && BeeUtils.isPositive(timeMillis)) {
      datePart = start;
    }

    if (datePart != null) {
      return TimeUtils.combine(datePart, timeMillis);
    } else {
      int duration = getDuration();
      if (start != null && duration > 0) {
        return new DateTime(start.getTime() + duration * TimeUtils.MILLIS_PER_MINUTE);
      } else {
        return null;
      }
    }
  }

  private String getEndDateWidgetId() {
    return endDateWidgetId;
  }

  private String getEndTimeWidgetId() {
    return endTimeWidgetId;
  }

  private String getHourWidgetId() {
    return hourWidgetId;
  }

  private InputDate getInputDate(String id) {
    Widget widget = getWidget(id);
    return (widget instanceof InputDate) ? (InputDate) widget : null;
  }

  private InputTime getInputTime(String id) {
    Widget widget = getWidget(id);
    return (widget instanceof InputTime) ? (InputTime) widget : null;
  }

  private DateTime getLastCheckEnd() {
    return lastCheckEnd;
  }

  private DateTime getLastCheckStart() {
    return lastCheckStart;
  }

  private ListBox getListBox(String id) {
    if (BeeUtils.isEmpty(id)) {
      return null;
    }
    Widget widget = getWidget(id);
    return (widget instanceof ListBox) ? (ListBox) widget : null;
  }

  private Long getMillis(String id) {
    InputTime inputTime = getInputTime(id);
    return (inputTime == null) ? null : inputTime.getMillis();
  }

  private String getMinuteWidgetId() {
    return minuteWidgetId;
  }

  private String getOverlapWidgetId() {
    return overlapWidgetId;
  }

  private String getReminderWidgetId() {
    return reminderWidgetId;
  }

  private String getRepairTypeWidgetId() {
    return repairTypeWidgetId;
  }

  private static List<Long> getResources(IsRow row) {
    return DataUtils.parseIdList(row.getProperty(TBL_APPOINTMENT_ATTENDEES));
  }

  private String getResourceWidgetId() {
    return resourceWidgetId;
  }

  private Long getSelectedId(String widgetId, List<Long> rowIds) {
    ListBox listBox = getListBox(widgetId);

    if (listBox != null) {
      int index = listBox.getSelectedIndex();
      if (BeeUtils.isIndex(rowIds, index)) {
        return rowIds.get(index);
      }
    }
    return null;
  }

  private String getServiceTypeWidgetId() {
    return serviceTypeWidgetId;
  }

  private DateTime getStart() {
    HasDateValue datePart = BeeUtils.isEmpty(getStartDateWidgetId())
        ? null : getInputDate(getStartDateWidgetId()).getDate();
    if (datePart == null) {
      return null;
    }

    Long timeMillis = getMillis(getStartTimeWidgetId());
    return TimeUtils.combine(datePart, timeMillis);
  }

  private String getStartDateWidgetId() {
    return startDateWidgetId;
  }

  private String getStartTimeWidgetId() {
    return startTimeWidgetId;
  }

  private Widget getWidget(String id) {
    Widget widget = DomUtils.getChildQuietly(getFormView().asWidget(), id);
    if (widget == null) {
      logger.warning("widget not found: id", id);
    }
    return widget;
  }

  private boolean hasValue(String widgetId) {
    Widget widget = getWidget(widgetId);

    if (widget instanceof ListBox) {
      return ((ListBox) widget).getSelectedIndex() >= 0;
    } else {
      return false;
    }
  }

  private void hideOverlap() {
    showOverlap(false);
  }

  private void initColorWidget() {
    if (colorWidget.getItemCount() > 0) {
      colorWidget.clear();
    }
    if (!colors.isEmpty()) {
      colors.clear();
    }

    BeeRowSet themeColors = CalendarKeeper.getThemeColors();
    if (themeColors == null || themeColors.isEmpty()) {
      logger.warning("theme colors not found");
      return;
    }

    Long theme = null;

    Long defAppType = CalendarKeeper.getDefaultAppointmentType();
    if (defAppType != null) {
      theme = CalendarKeeper.CACHE.getLong(VIEW_APPOINTMENT_TYPES, defAppType,
          AdministrationConstants.COL_THEME);
    }

    String viewName = themeColors.getViewName();
    if (!DataUtils.isId(theme)) {
      theme = Data.getLong(viewName, themeColors.getRow(0), AdministrationConstants.COL_THEME);
    }

    for (BeeRow row : themeColors.getRows()) {
      if (DataUtils.isId(theme) && !theme.equals(Data.getLong(viewName, row,
          AdministrationConstants.COL_THEME))) {
        continue;
      }

      Long color = Data.getLong(viewName, row, AdministrationConstants.COL_COLOR);

      String bc = Data.getString(viewName, row, AdministrationConstants.COL_BACKGROUND);
      String fc = Data.getString(viewName, row, AdministrationConstants.COL_FOREGROUND);

      Label item = new Label();
      item.getElement().getStyle().setBackgroundColor(bc);
      if (!BeeUtils.isEmpty(fc)) {
        item.getElement().getStyle().setColor(fc);
      }

      colorWidget.addItem(item, StyleUtils.NAME_FOCUSABLE);
      colors.add(color);
    }

    if (colorWidget.getItemCount() > 0) {
      if (isNew && DataUtils.isId(theme)) {
        Long defColor = CalendarKeeper.CACHE.getLong(AdministrationConstants.VIEW_THEMES, theme,
            AdministrationConstants.COL_DEFAULT_COLOR);
        if (defColor != null && colors.contains(defColor)) {
          colorWidget.selectTab(colors.indexOf(defColor));
        }
      }
    } else {
      logger.warning("theme", theme, "colors not found");
    }
  }

  private void initPropWidget(Widget widget, BeeRowSet rowSet, List<Long> rowIds, Long def) {
    if (widget instanceof ListBox && !rowIds.isEmpty()) {
      final ListBox listBox = (ListBox) widget;
      if (listBox.getItemCount() > 0) {
        listBox.clear();
      }

      for (long id : rowIds) {
        BeeRow row = rowSet.getRowById(id);
        String item = Data.getString(rowSet.getViewName(), row, COL_PROPERTY_NAME);
        listBox.addItem(item);
      }

      if (isNew) {
        int index = (def == null) ? BeeConst.UNDEF : rowIds.indexOf(def);
        setSelectedIndex(listBox, index);
      }
    }
  }

  private void initReminderWidget(Widget widget, BeeRowSet rowSet) {
    if (widget instanceof ListBox && !reminderTypes.isEmpty()) {
      final ListBox listBox = (ListBox) widget;
      if (listBox.getItemCount() > 0) {
        listBox.clear();
      }

      String viewName = rowSet.getViewName();
      for (long id : reminderTypes) {
        BeeRow row = rowSet.getRowById(id);
        String item = Data.getString(viewName, row, AdministrationConstants.COL_REMINDER_NAME);
        listBox.addItem(BeeUtils.trimRight(item));
      }

      if (isNew) {
        setSelectedIndex(listBox, BeeConst.UNDEF);
      }
    }
  }

  private static boolean isEmpty(IsRow row, String columnId) {
    return BeeUtils.isEmpty(Data.getString(VIEW_APPOINTMENTS, row, columnId));
  }

  private boolean isOverlapVisible() {
    return overlapVisible;
  }

  private boolean isSaving() {
    return saving;
  }

  private void loadProperties() {
    BeeRowSet properties = CalendarKeeper.getExtendedProperties();

    serviceTypes.clear();
    repairTypes.clear();

    defaultServiceType = null;
    defaultRepairType = null;

    if (properties == null || properties.isEmpty()) {
      logger.warning("extended properties not available");
      return;
    }

    String viewName = properties.getViewName();
    for (BeeRow row : properties.getRows()) {
      long id = row.getId();

      String groupName = Data.getString(viewName, row, ALS_PROPERTY_GROUP_NAME);
      boolean isDef = Objects.equals(Data.getLong(viewName, row, COL_DEFAULT_PROPERTY), id);

      if (BeeUtils.containsSame(groupName, "serv")) {
        serviceTypes.add(id);
        if (isDef) {
          defaultServiceType = id;
        }

      } else if (BeeUtils.containsSame(groupName, "rem")) {
        repairTypes.add(id);
        if (isDef) {
          defaultRepairType = id;
        }
      }
    }

    loadResourceProperties();

    if (!BeeUtils.isEmpty(getServiceTypeWidgetId())) {
      initPropWidget(getWidget(getServiceTypeWidgetId()), properties, serviceTypes,
          defaultServiceType);
    }
    if (!BeeUtils.isEmpty(getRepairTypeWidgetId())) {
      initPropWidget(getWidget(getRepairTypeWidgetId()), properties, repairTypes,
          defaultRepairType);
    }
  }

  private void loadReminders() {
    BeeRowSet rowSet = CalendarKeeper.getReminderTypes();
    if (rowSet == null || rowSet.isEmpty()) {
      logger.warning("reminder types not available");
      return;
    }

    BeeUtils.overwrite(reminderTypes, DataUtils.getRowIds(rowSet));
    if (!BeeUtils.isEmpty(getReminderWidgetId())) {
      initReminderWidget(getWidget(getReminderWidgetId()), rowSet);
    }
  }

  private void loadResourceProperties() {
    BeeRowSet rowSet = CalendarKeeper.getAttendeeProps();

    if (!serviceResources.isEmpty()) {
      serviceResources.clear();
    }
    if (!repairResources.isEmpty()) {
      repairResources.clear();
    }
    if (rowSet == null || rowSet.isEmpty()) {
      return;
    }

    String viewName = rowSet.getViewName();
    for (BeeRow row : rowSet.getRows()) {
      Long property = Data.getLong(viewName, row, COL_ATTENDEE_PROPERTY);
      Long resource = Data.getLong(viewName, row, COL_ATTENDEE);

      if (serviceTypes.contains(property)) {
        serviceResources.put(resource, property);
      } else if (repairTypes.contains(property)) {
        repairResources.put(resource, property);
      }
    }
  }

  private void onEndDateFocus(InputDate widget) {
    if (widget == null) {
      return;
    }

    JustDate start = getInputDate(getStartDateWidgetId()).getDate();
    widget.setMinDate(BeeUtils.nvl(start, MIN_DATE));
  }

  private void onEndTimeFocus(InputTime widget) {
    if (widget == null) {
      return;
    }

    DateTime start = getStart();
    if (start == null) {
      widget.setMinValue(null);
      return;
    }

    HasDateValue endDate = BeeUtils.isEmpty(getEndDateWidgetId())
        ? null : getInputDate(getEndDateWidgetId()).getDate();

    int min = 0;
    if (endDate == null || TimeUtils.sameDate(start, endDate)) {
      min = TimeUtils.minutesSinceDayStarted(start) + widget.getNormalizedStep();
      if (min >= TimeUtils.MINUTES_PER_DAY) {
        min = 0;
      }
    }

    widget.setMinMinutes(min);
  }

  private void refreshResourceWidget(IsRow row) {
    Widget selector = getWidget(getResourceWidgetId());
    if (selector instanceof MultiSelector) {
      ((MultiSelector) selector).render(row);
    }
  }

  private void reset(BeeRow createdRow) {
    IsRow row = DataUtils.cloneRow(createdRow);
    row.setId(DataUtils.NEW_ROW_ID);
    row.setVersion(DataUtils.NEW_ROW_VERSION);

    StringBuilder info = new StringBuilder();
    String separator = BeeConst.DEFAULT_LIST_SEPARATOR;

    ListBox listBox = getListBox(getRepairTypeWidgetId());
    if (listBox != null) {
      int index = listBox.getSelectedIndex();
      if (index >= 0) {
        info.append(listBox.getItemText(index)).append(separator);
      }
      listBox.deselect();

      Long serviceType = getSelectedId(getServiceTypeWidgetId(), serviceTypes);
      if (serviceType == null) {
        row.clearProperty(TBL_APPOINTMENT_PROPS);
      } else {
        row.setProperty(TBL_APPOINTMENT_PROPS, serviceType.toString());
      }
    }

    boolean wasOpaque = false;

    List<Long> resources = getResources(row);
    if (!resources.isEmpty()) {
      BeeRowSet attendees = CalendarKeeper.getAttendees();
      for (long attId : resources) {
        info.append(attendees.getStringByRowId(attId, COL_ATTENDEE_NAME)).append(separator);
        if (!wasOpaque && CalendarKeeper.isAttendeeOpaque(attId)) {
          wasOpaque = true;
        }
      }

      row.clearProperty(TBL_APPOINTMENT_ATTENDEES);
      refreshResourceWidget(row);

      hideOverlap();
    }

    DateTime start = Data.getDateTime(VIEW_APPOINTMENTS, createdRow, COL_START_DATE_TIME);
    DateTime end = Data.getDateTime(VIEW_APPOINTMENTS, createdRow, COL_END_DATE_TIME);

    DateTimeFormat format = DateTimeFormat.getFormat("MMM d HH:mm");
    if (start != null) {
      info.append(format.format(start));
    }
    info.append(" - ");
    if (end != null) {
      info.append(format.format(end));
    }

    listBox = getListBox(getReminderWidgetId());
    if (listBox != null) {
      int index = listBox.getSelectedIndex();
      if (index >= 0) {
        info.append(separator).append(listBox.getItemText(index));
      }
      listBox.deselect();

      row.clearProperty(TBL_APPOINTMENT_REMINDERS);
    }

    Widget widget = BeeUtils.isEmpty(getHourWidgetId()) ? null : getWidget(getHourWidgetId());
    if (widget instanceof Editor) {
      ((Editor) widget).setValue(BeeConst.STRING_ZERO);
    }
    widget = BeeUtils.isEmpty(getMinuteWidgetId()) ? null : getWidget(getMinuteWidgetId());
    if (widget instanceof Editor) {
      ((Editor) widget).setValue(BeeConst.STRING_ZERO);
    }

    widget = BeeUtils.isEmpty(getBuildInfoWidgetId()) ? null : getWidget(getBuildInfoWidgetId());
    if (widget instanceof HasItems) {
      ((HasItems) widget).addItem(info.toString());
    }

    if (wasOpaque && end != null) {
      Data.setValue(VIEW_APPOINTMENTS, row, COL_START_DATE_TIME, end);
      Data.clearCell(VIEW_APPOINTMENTS, row, COL_END_DATE_TIME);
    }
    Data.clearCell(VIEW_APPOINTMENTS, row, COL_DESCRIPTION);

    getFormView().updateRow(row, false);
  }

  private boolean save(final RowCallback callback) {
    if (isSaving()) {
      return false;
    }
    setSaving(true);

    AutocompleteProvider.retainValues(getFormView());

    BeeRow row = DataUtils.cloneRow(getFormView().getActiveRow());
    final String viewName = VIEW_APPOINTMENTS;

    DateTime start = getStart();
    Data.setValue(viewName, row, COL_START_DATE_TIME, start);

    DateTime end = getEnd(start);
    Data.setValue(viewName, row, COL_END_DATE_TIME, end);

    if (!colors.isEmpty()) {
      int index = colorWidget.getSelectedTab();
      if (!BeeUtils.isIndex(colors, index) && isEmpty(row, AdministrationConstants.COL_COLOR)) {
        index = 0;
      }
      if (BeeUtils.isIndex(colors, index)) {
        Data.setValue(viewName, row, AdministrationConstants.COL_COLOR, colors.get(index));
      }
    }

    BeeRowSet rowSet;
    List<BeeColumn> columns = CalendarKeeper.getAppointmentViewColumns();
    if (isNew) {
      rowSet = DataUtils.createRowSetForInsert(viewName, columns, row);
    } else {
      rowSet = new BeeRowSet(viewName, columns);
      rowSet.addRow(row);
    }

    final String propList = DataUtils.buildIdList(getSelectedId(getServiceTypeWidgetId(),
        serviceTypes), getSelectedId(getRepairTypeWidgetId(), repairTypes));
    if (!BeeUtils.isEmpty(propList)) {
      rowSet.setTableProperty(TBL_APPOINTMENT_PROPS, propList);
    }

    final String attList = row.getProperty(TBL_APPOINTMENT_ATTENDEES);
    if (!BeeUtils.isEmpty(attList)) {
      rowSet.setTableProperty(TBL_APPOINTMENT_ATTENDEES, attList);
    }

    Long reminderType = getSelectedId(getReminderWidgetId(), reminderTypes);
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
          getFormView().notifySevere(response.getErrors());

        } else if (!response.hasResponse(BeeRow.class)) {
          getFormView().notifySevere(svc, ": response not a BeeRow");

        } else {
          BeeRow result = BeeRow.restore((String) response.getResponse());
          if (result == null) {
            getFormView().notifySevere(svc, ": cannot restore row");
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
              RowInsertEvent.fire(BeeKeeper.getBus(), viewName, result, getFormView().getId());
            } else {
              RowUpdateEvent.fire(BeeKeeper.getBus(), viewName, result);
            }

            Appointment appointment = new Appointment(result);
            State state = isNew ? State.CREATED : State.CHANGED;
            AppointmentEvent.fire(appointment, state);

            if (callback != null) {
              callback.onSuccess(result);
            }
          }
        }
        setSaving(false);
      }
    });

    return true;
  }

  private void setBuildInfoWidgetId(String buildInfoWidgetId) {
    this.buildInfoWidgetId = buildInfoWidgetId;
  }

  private static void setDateBounds(InputDate widget) {
    if (widget.getMinDate() == null) {
      widget.setMinDate(MIN_DATE);
    }

    if (widget.getMaxDate() == null) {
      widget.setMaxDate(MAX_DATE);
    }
  }

  private void setEndDateWidgetId(String endDateWidgetId) {
    this.endDateWidgetId = endDateWidgetId;
  }

  private void setEndTimeWidgetId(String endTimeWidgetId) {
    this.endTimeWidgetId = endTimeWidgetId;
  }

  private void setHourWidgetId(String hourWidgetId) {
    this.hourWidgetId = hourWidgetId;
  }

  private void setLastCheckEnd(DateTime lastCheckEnd) {
    this.lastCheckEnd = lastCheckEnd;
  }

  private void setLastCheckStart(DateTime lastCheckStart) {
    this.lastCheckStart = lastCheckStart;
  }

  private void setMinuteWidgetId(String minuteWidgetId) {
    this.minuteWidgetId = minuteWidgetId;
  }

  private void setModalCallback(ModalCallback modalCallback) {
    this.modalCallback = modalCallback;
  }

  private void setOverlapVisible(boolean overlapVisible) {
    this.overlapVisible = overlapVisible;
  }

  private void setOverlapWidgetId(String overlapWidgetId) {
    this.overlapWidgetId = overlapWidgetId;
  }

  private void setReminderWidgetId(String reminderWidgetId) {
    this.reminderWidgetId = reminderWidgetId;
  }

  private void setRepairTypeWidgetId(String repairTypeWidgetId) {
    this.repairTypeWidgetId = repairTypeWidgetId;
  }

  private void setResourceWidgetId(String resourceWidgetId) {
    this.resourceWidgetId = resourceWidgetId;
  }

  private void setSaving(boolean saving) {
    this.saving = saving;
  }

  private static void setSelectedIndex(final ListBox listBox, int index) {
    if (listBox == null || listBox.isEmpty()) {
      return;
    }

    if (listBox.isIndex(index)) {
      listBox.setSelectedIndex(index);
    } else {
      listBox.deselect();
    }
  }

  private void setServiceTypeWidgetId(String serviceTypeWidgetId) {
    this.serviceTypeWidgetId = serviceTypeWidgetId;
  }

  private void setStartDateWidgetId(String startDateWidgetId) {
    this.startDateWidgetId = startDateWidgetId;
  }

  private void setStartTimeWidgetId(String startTimeWidgetId) {
    this.startTimeWidgetId = startTimeWidgetId;
  }

  private void showOverlap(boolean show) {
    if (isOverlapVisible() != show && !BeeUtils.isEmpty(getOverlapWidgetId())) {
      Widget widget = getWidget(getOverlapWidgetId());
      if (widget != null) {
        widget.getElement().getStyle().setVisibility(show ? Visibility.VISIBLE : Visibility.HIDDEN);
        setOverlapVisible(show);
      }
    }
  }

  private void showOverlappingAppointments() {
    if (BeeUtils.isEmpty(overlappingAppointments)) {
      return;
    }

    Flow panel = new Flow();
    panel.addStyleName(CalendarStyleManager.MORE_PANEL);

    for (Appointment appointment : overlappingAppointments) {
      ItemWidget widget = new ItemWidget(appointment, appointment.isMultiDay());
      widget.render(BeeConst.UNDEF, null);

      panel.add(widget);
    }

    DialogBox dialog = DialogBox.create(Localized.getConstants().calOverlappingAppointments(),
        CalendarStyleManager.MORE_POPUP);
    dialog.setWidget(panel);

    if (BeeUtils.isEmpty(getOverlapWidgetId())) {
      dialog.center();
    } else {
      dialog.showRelativeTo(getWidget(getOverlapWidgetId()).getElement());
    }
  }

  private void updateDuration() {
    ListBox listBox = getListBox(getRepairTypeWidgetId());
    String propName = (listBox == null) ? null : listBox.getValue();

    BeeRowSet properties = CalendarKeeper.getExtendedProperties();
    if (BeeUtils.isEmpty(propName) || properties == null) {
      return;
    }

    Integer hours = null;
    Integer minutes = null;

    String viewName = properties.getViewName();
    for (BeeRow row : properties.getRows()) {
      if (BeeUtils.equalsTrim(propName, Data.getString(viewName, row, COL_PROPERTY_NAME))) {
        hours = Data.getInteger(viewName, row, COL_HOURS);
        minutes = Data.getInteger(viewName, row, COL_MINUTES);
        break;
      }
    }

    if (BeeUtils.isPositive(hours) || BeeUtils.isPositive(minutes)) {
      if (!BeeUtils.isEmpty(getHourWidgetId())) {
        Widget widget = getWidget(getHourWidgetId());
        if (widget instanceof Editor) {
          String hv = BeeUtils.isPositive(hours) ? hours.toString() : BeeConst.STRING_ZERO;
          ((Editor) widget).setValue(hv);
        }
      }

      if (!BeeUtils.isEmpty(getMinuteWidgetId())) {
        Widget widget = getWidget(getMinuteWidgetId());
        if (widget instanceof Editor) {
          String mv = BeeUtils.isPositive(minutes) ? minutes.toString() : BeeConst.STRING_ZERO;
          ((Editor) widget).setValue(mv);
        }
      }
    }
  }

  private boolean validate() {
    if (!getFormView().validate(getFormView(), true)) {
      return false;
    }

    IsRow row = getFormView().getActiveRow();
    if (row == null) {
      return false;
    }

    if (isRequired(ClassifierConstants.COL_COMPANY)
        && isEmpty(row, ClassifierConstants.COL_COMPANY)) {
      getFormView().notifySevere(Localized.getConstants().calEnterClient());
      return false;
    }
    if (isRequired(COL_VEHICLE) && isEmpty(row, COL_VEHICLE)) {
      getFormView().notifySevere(Localized.getConstants().calEnterVehicle());
      return false;
    }

    if (!BeeUtils.isEmpty(getServiceTypeWidgetId()) && isRequired(NAME_SERVICE_TYPE)
        && !hasValue(getServiceTypeWidgetId())) {
      getFormView().notifySevere(Localized.getConstants().calEnterServiceType());
      return false;
    }
    if (!BeeUtils.isEmpty(getRepairTypeWidgetId()) && isRequired(NAME_REPAIR_TYPE)
        && !hasValue(getRepairTypeWidgetId())) {
      getFormView().notifySevere(Localized.getConstants().calEnterRepairType());
      return false;
    }

    if (isRequired(NAME_RESOURCES) && getResources(row).isEmpty()) {
      getFormView().notifySevere(Localized.getConstants().calEnterAttendees());
      return false;
    }

    DateTime start = getStart();
    DateTime end = getEnd(start);

    if (start == null) {
      getFormView().notifySevere(Localized.getConstants().calEnterPlannedStartTime());
      return false;
    }
    if (end == null) {
      getFormView().notifySevere(Localized.getConstants().calEnterDurationOrPlannedEndDate());
      return false;
    }
    if (TimeUtils.isLeq(end, start)) {
      getFormView().notifySevere(Localized.getConstants().calPlannedEndDateMustBeGreater());
      return false;
    }

    return true;
  }
}
