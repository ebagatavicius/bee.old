package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Lists;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import static com.butent.bee.shared.modules.calendar.CalendarConstants.COL_DESCRIPTION;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.DataCache;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.InputBoxes;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.RowActionEvent;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.modules.calendar.view.AppointmentForm;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.style.ColorStyleProvider;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.Command;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.UniqueChildInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class CalendarKeeper {

  private static final class CalendarOpener extends Command {
    private final long id;
    private final String name;
    private final ViewCallback callback;

    private CalendarOpener(long id, String name, ViewCallback callback) {
      super();
      this.id = id;
      this.name = name;
      this.callback = callback;
    }

    @Override
    public void execute() {
      getUserCalendar(id, new Queries.RowSetCallback() {
        @Override
        public void onSuccess(BeeRowSet result) {
          BeeRow row = result.getRow(0);
          BeeRowSet ucAttendees = BeeRowSet.restore(row.getProperty(TBL_USER_CAL_ATTENDEES));

          CalendarSettings settings = CalendarSettings.create(row, result.getColumns());
          CalendarPanel calendarPanel = new CalendarPanel(id, name, settings, ucAttendees);

          onCreatePanel(id, row.getId(), name, ucAttendees);

          callback.onSuccess(calendarPanel);
        }
      });
    }
  }

  private static class RowActionHandler implements RowActionEvent.Handler {
    @Override
    public void onRowAction(final RowActionEvent event) {
      if (event.isCellClick() || event.isEditRow() || event.isOpenFavorite()) {
        if (event.hasView(VIEW_CALENDARS)) {
          event.consume();
          Long calId = event.getRowId();

          if (DataUtils.isId(calId)) {
            String calName;
            if (event.hasRow()) {
              calName = Data.getString(VIEW_CALENDARS, event.getRow(), COL_CALENDAR_NAME);
            } else {
              calName = event.getOptions();
            }

            openCalendar(calId, calName, result -> BeeKeeper.getScreen().showInNewPlace(result));
          }

        } else if (event.hasView(VIEW_APPOINTMENTS)) {
          event.consume();
          if (event.hasRow()) {
            ensureData(new Command() {
              @Override
              public void execute() {
                openAppointment(Appointment.create(event.getRow()), null);
              }
            });
          }
        }
      }
    }
  }

  private static class RowTransformHandler implements RowTransformEvent.Handler {
    @Override
    public void onRowTransform(RowTransformEvent event) {
      if (event.hasView(VIEW_CALENDARS)) {
        event.setResult(DataUtils.join(Data.getDataInfo(VIEW_CALENDARS), event.getRow(),
            Lists.newArrayList(COL_CALENDAR_NAME, COL_DESCRIPTION, ALS_OWNER_FIRST_NAME,
                ALS_OWNER_LAST_NAME), BeeConst.STRING_SPACE));

      } else if (event.hasView(VIEW_APPOINTMENTS)) {
        event.setResult(ITEM_RENDERER.renderString(BeeConst.UNDEF,
            Appointment.create(event.getRow())));
      }
    }
  }

  static final DataCache CACHE = new DataCache();

  private static final List<String> CACHED_VIEWS =
      Lists.newArrayList(VIEW_CONFIGURATION, VIEW_APPOINTMENT_TYPES, VIEW_ATTENDEES,
          VIEW_EXTENDED_PROPERTIES, VIEW_REMINDER_TYPES,
          VIEW_THEMES, VIEW_THEME_COLORS, VIEW_ATTENDEE_PROPS,
          VIEW_APPOINTMENT_STYLES, VIEW_CALENDARS, VIEW_CAL_APPOINTMENT_TYPES,
          VIEW_CALENDAR_EXECUTORS, VIEW_CAL_EXECUTOR_GROUPS);

  private static final ItemRenderer ITEM_RENDERER = new ItemRenderer();

  private static final ReportManager REPORT_MANAGER = new ReportManager();

  private static final SelectorHandler SELECTOR_HANDLER = new SelectorHandler();

  private static final BeeLogger logger = LogUtils.getLogger(CalendarKeeper.class);

  private static final Map<String, Long> activePanels = new HashMap<>();
  private static final Map<Long, String> activeControllers = new HashMap<>();

  private static FormView settingsForm;

  private static boolean dataLoaded;

  public static void createAppointment(final Consumer<BeeRow> initializer, final String duration,
      final RowCallback callback) {

    ensureData(new Command() {
      @Override
      public void execute() {
        createAppointment(null, null, duration, null, initializer, callback);
      }
    });
  }

  public static void createAppointment(final Long calendarId, final DateTime start,
      final String duration, final Long attendeeId, final Consumer<BeeRow> initializer,
      final RowCallback callback) {
    createAppointment(calendarId, start, duration, attendeeId, initializer, callback, null, null);
  }

  public static void createAppointment(final Long calendarId, final DateTime start,
      final String duration, final Long attendeeId, final Consumer<BeeRow> initializer,
      final RowCallback callback, IsRow projectRow, String form) {

    if (Data.isViewEditable(VIEW_APPOINTMENTS)
        && BeeKeeper.getUser().canCreateData(VIEW_APPOINTMENTS)) {

      Long type = null;
      if (calendarId != null) {
        if (BeeUtils.unbox(CACHE.getBoolean(VIEW_CALENDARS, calendarId, COL_CALENDAR_IS_SERVICE))) {
          DataInfo dataInfo = Data.getDataInfo(TBL_SERVICE_EVENTS);
          BeeRow newRow = RowFactory.createEmptyRow(dataInfo);
          newRow.setValue(dataInfo.getColumnIndex(COL_START_DATE_TIME), start);
          newRow.setProperty(TBL_ATTENDEES, attendeeId);
          RowFactory.createRow(dataInfo, newRow, null);
          return;
        }
        BeeRowSet rowSet = CACHE.getRowSet(VIEW_CAL_APPOINTMENT_TYPES);
        if (rowSet != null) {
          for (BeeRow row : rowSet.getRows()) {
            if (Data.equals(VIEW_CAL_APPOINTMENT_TYPES, row, COL_CALENDAR, calendarId)) {
              type = Data.getLong(VIEW_CAL_APPOINTMENT_TYPES, row, COL_APPOINTMENT_TYPE);
              break;
            }
          }
        }
      }

      if (type == null) {
        type = getDefaultAppointmentType();
      }
      final BeeRow typeRow = (type == null) ? null : CACHE.getRow(VIEW_APPOINTMENT_TYPES, type);

      String formName;
      if (BeeUtils.isEmpty(form)) {
        formName = (typeRow == null) ? null : Data.getString(VIEW_APPOINTMENT_TYPES, typeRow,
            COL_APPOINTMENT_CREATOR);
        if (BeeUtils.isEmpty(formName)) {
          formName = DEFAULT_NEW_APPOINTMENT_FORM;
        }
      } else {
        formName = form;
      }

      final AppointmentBuilder builder = new AppointmentBuilder(true);

      FormFactory.createFormView(formName, VIEW_APPOINTMENTS,
          getAppointmentViewColumns(), false, builder, (formDescription, result) -> {
            if (result != null) {
              result.start(null);

              Long att = null;
              if (DataUtils.isId(attendeeId)) {
                att = attendeeId;

              } else if (DataUtils.isId(calendarId)) {
                CalendarController controller = getController(calendarId);
                if (controller != null) {
                  List<Long> attendees = controller.getAttendees();
                  if (!attendees.isEmpty()) {
                    att = attendees.get(0);
                    builder.setUcAttendees(attendees);
                  }
                }
              }

              BeeRow row = AppointmentBuilder.createEmptyRow(typeRow, start,
                  getCalendarVisibility(calendarId));
              if (att != null) {
                row.setProperty(TBL_APPOINTMENT_ATTENDEES, BeeUtils.toString(att));
              }

              if (initializer != null) {
                initializer.accept(row);
              }

              result.updateRow(row, false);

              builder.setAppointmentView(result);
              builder.setRequiredFields(formDescription.getOptions());

              if (BeeUtils.isEmpty(duration)) {
                builder.initPeriod(start);
              } else {
                builder.setDuration(duration);
              }

              if (projectRow != null) {
                builder.setProjectData(projectRow);
              }

              boolean companyAndVehicle = builder.isRequired(ClassifierConstants.COL_COMPANY)
                  && builder.isRequired(COL_VEHICLE);
              SELECTOR_HANDLER.setCompanyHandlerEnabled(companyAndVehicle);
              SELECTOR_HANDLER.setVehicleHandlerEnabled(companyAndVehicle);

              Global.inputWidget(getAppointmentViewInfo().getNewRowCaption(), result,
                  builder.getModalCallback(callback), RowFactory.DIALOG_STYLE, null,
                  EnumSet.of(Action.PRINT));
            }
          });
    }
  }

  public static void ensureData(final Command command) {
    if (isDataLoaded()) {
      command.execute();

    } else {
      final long startMillis = System.currentTimeMillis();

      CACHE.getData(CACHED_VIEWS, result -> {
        setDataLoaded();
        logger.debug("calendar cache loaded", result, TimeUtils.elapsedMillis(startMillis));
        command.execute();
      });
    }
  }

  public static String getAttendeeCaption(long calendarId, long attId) {
    if (activeControllers.containsKey(calendarId)) {
      CalendarController controller = getController(calendarId);
      if (controller != null) {
        String caption = controller.getAttendeeCaption(attId);
        if (!BeeUtils.isEmpty(caption)) {
          return caption;
        }
      }
    }
    return getAttendeeName(attId);
  }

  public static Map<Long, String> getAttendeeColors(long calendarId) {
    if (activeControllers.containsKey(calendarId)) {
      CalendarController controller = getController(calendarId);
      if (controller != null) {
        return controller.getAttendeeColors();
      }
    }
    return new HashMap<>();
  }

  public static String getPropertyName(long id) {
    return CACHE.getString(VIEW_EXTENDED_PROPERTIES, id, COL_PROPERTY_NAME);
  }

  public static String getReminderTypeName(long id) {
    return CACHE.getString(VIEW_REMINDER_TYPES, id, COL_REMINDER_NAME);
  }

  public static boolean isDataLoaded() {
    return dataLoaded;
  }

  public static void register() {
    FormFactory.registerFormInterceptor(FORM_APPOINTMENT, new AppointmentForm());

    GridFactory.registerGridInterceptor(GRID_CALENDAR_EXECUTORS,
        UniqueChildInterceptor.forUsers(Localized.dictionary().calAddExecutors(),
            COL_CALENDAR, COL_EXECUTOR_USER));

    GridFactory.registerGridInterceptor(GRID_CAL_EXECUTOR_GROUPS,
        UniqueChildInterceptor.forUserGroups(Localized.dictionary().calAddExecutorGroups(),
            COL_CALENDAR, COL_EXECUTOR_GROUP));

    GridFactory.registerGridInterceptor(GRID_APPOINTMENT_ATTENDEES, new UniqueChildInterceptor(
        Localized.dictionary().calAddAttendees(), COL_APPOINTMENT, COL_ATTENDEE, VIEW_ATTENDEES,
        Lists.newArrayList(COL_ATTENDEE_NAME),
        Lists.newArrayList(COL_ATTENDEE_NAME, ALS_ATTENDEE_TYPE_NAME)));

    GridFactory.registerGridInterceptor(GRID_APPOINTMENT_OWNERS,
        UniqueChildInterceptor.forUsers(Localized.dictionary().calAddOwners(),
            COL_APPOINTMENT, COL_APPOINTMENT_OWNER));

    GridFactory.registerGridInterceptor(GRID_APPOINTMENT_PROPS, new UniqueChildInterceptor(
        Localized.dictionary().calAddParameters(), COL_APPOINTMENT, COL_APPOINTMENT_PROPERTY,
        VIEW_EXTENDED_PROPERTIES, Lists.newArrayList(COL_PROPERTY_NAME),
        Lists.newArrayList(COL_PROPERTY_NAME, ALS_PROPERTY_GROUP_NAME)));

    ConditionalStyle.registerGridColumnStyleProvider(GRID_APPOINTMENTS,
        ALS_COLOR_NAME, ColorStyleProvider.createDefault(VIEW_APPOINTMENTS));
    ConditionalStyle.registerGridColumnStyleProvider(GRID_ATTENDEES,
        ALS_COLOR_NAME, ColorStyleProvider.createDefault(VIEW_ATTENDEES));

    ColorStyleProvider styleProvider = ColorStyleProvider.createDefault(VIEW_CALENDAR_EXECUTORS);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_CALENDAR_EXECUTORS,
        COL_BACKGROUND, styleProvider);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_CALENDAR_EXECUTORS,
        COL_FOREGROUND, styleProvider);

    styleProvider = ColorStyleProvider.createDefault(VIEW_CAL_EXECUTOR_GROUPS);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_CAL_EXECUTOR_GROUPS,
        COL_BACKGROUND, styleProvider);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_CAL_EXECUTOR_GROUPS,
        COL_FOREGROUND, styleProvider);

    BeeKeeper.getBus().registerDataHandler(CACHE, true);
    BeeKeeper.getBus().registerRowActionHandler(new RowActionHandler());
    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler());

    SelectorEvent.register(SELECTOR_HANDLER);

    RowEditor.registerHasDelegate(VIEW_CALENDARS);
    RowEditor.registerHasDelegate(VIEW_APPOINTMENTS);

    MenuService.CALENDAR_REPORTS.setHandler(parameters -> REPORT_MANAGER
        .onSelectReport(EnumUtils.getEnumByIndex(Report.class, parameters)));
  }

  public static void setDataLoaded() {
    CalendarKeeper.dataLoaded = true;
  }

  public static void openCalendar(final long id, final ViewCallback callback) {
    Queries.getValue(VIEW_CALENDARS, id, COL_CALENDAR_NAME, new RpcCallback<String>() {
      @Override
      public void onSuccess(String result) {
        if (!BeeUtils.isEmpty(result)) {
          openCalendar(id, result, callback);
        }
      }
    });
  }

  static ParameterList createArgs(String service) {
    return BeeKeeper.getRpc().createParameters(Module.CALENDAR, service);
  }

  static void editSettings(long id, final CalendarPanel calendarPanel) {
    getUserCalendar(id, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (getSettingsForm() == null) {
          createSettingsForm(result, calendarPanel);
        } else {
          openSettingsForm(result, calendarPanel);
        }
      }
    });
  }

  static BeeRow getAppointmentTypeRow(CalendarItem item) {
    if (item.getItemType() != ItemType.APPOINTMENT) {
      return null;
    }

    Long type = ((Appointment) item).getType();
    if (type == null) {
      type = getDefaultAppointmentType();
    }

    BeeRowSet rowSet = CACHE.getRowSet(VIEW_APPOINTMENT_TYPES);

    BeeRow row = null;
    if (rowSet != null && !rowSet.isEmpty()) {
      if (type != null) {
        row = rowSet.getRowById(type);
      }
      if (row == null && rowSet.getNumberOfRows() == 1) {
        row = rowSet.getRow(0);
      }
    }
    return row;
  }

  static List<BeeColumn> getAppointmentViewColumns() {
    return Data.getColumns(VIEW_APPOINTMENTS);
  }

  static DataInfo getAppointmentViewInfo() {
    return Data.getDataInfo(VIEW_APPOINTMENTS);
  }

  static BeeRowSet getAttendeeProps() {
    return CACHE.getRowSet(VIEW_ATTENDEE_PROPS);
  }

  static BeeRowSet getAttendees() {
    return CACHE.getRowSet(VIEW_ATTENDEES);
  }

  static String getCalendarSupplierKey(long calendarId) {
    return ViewFactory.SupplierKind.CALENDAR.getKey(BeeUtils.toString(calendarId));
  }

  static Long getDefaultAppointmentType() {
    BeeRowSet rowSet = CACHE.getRowSet(VIEW_CONFIGURATION);
    if (rowSet != null) {
      for (BeeRow row : rowSet.getRows()) {
        Long type = Data.getLong(VIEW_CONFIGURATION, row, COL_APPOINTMENT_TYPE);
        if (type != null) {
          return type;
        }
      }
    }
    return null;
  }

  static BeeRowSet getExtendedProperties() {
    return CACHE.getRowSet(VIEW_EXTENDED_PROPERTIES);
  }

  static BeeRowSet getReminderTypes() {
    return CACHE.getRowSet(VIEW_REMINDER_TYPES);
  }

  static BeeRowSet getThemeColors() {
    return CACHE.getRowSet(VIEW_THEME_COLORS);
  }

  static BeeRowSet getThemes() {
    return CACHE.getRowSet(VIEW_THEMES);
  }

  static boolean isAttendeeOpaque(long id) {
    BeeRow row = CACHE.getRow(VIEW_ATTENDEES, id);
    if (row == null) {
      return false;
    }

    Integer value = Data.getInteger(VIEW_ATTENDEES, row, COL_TRANSPARENCY);
    if (value != null) {
      return Transparency.isOpaque(value);
    } else {
      return Transparency.isOpaque(Data.getInteger(VIEW_ATTENDEES, row, ALS_TYPE_TRANSPARENCY));
    }
  }

  static void onActivateController(long calendarId) {
    IdentifiableWidget iw = BeeKeeper.getScreen().getActiveWidget();
    if (iw instanceof CalendarPanel && ((CalendarPanel) iw).getCalendarId() == calendarId) {
      return;
    }

    for (Map.Entry<String, Long> entry : activePanels.entrySet()) {
      if (entry.getValue() == calendarId) {
        Widget panel = DomUtils.getWidget(entry.getKey());
        if (panel instanceof CalendarPanel) {
          BeeKeeper.getScreen().activateWidget((CalendarPanel) panel);
          break;
        }
      }
    }
  }

  static void onActivatePanel(CalendarPanel calendarPanel) {
    if (!activePanels.containsKey(calendarPanel.getId())) {
      activePanels.put(calendarPanel.getId(), calendarPanel.getCalendarId());
    }

    BeeKeeper.getScreen().activateDomainEntry(Domain.CALENDAR, calendarPanel.getCalendarId());
  }

  static void onRemoveController(long calendarId) {
    activeControllers.remove(calendarId);

    Set<CalendarPanel> panels = getActivePanels(calendarId);
    for (CalendarPanel panel : panels) {
      BeeKeeper.getScreen().closeWidget(panel);
    }
  }

  static void onRemovePanel(String panelId, long calendarId) {
    activePanels.remove(panelId);

    if (!activePanels.containsValue(calendarId) && activeControllers.containsKey(calendarId)) {
      activeControllers.remove(calendarId);
      BeeKeeper.getScreen().removeDomainEntry(Domain.CALENDAR, calendarId);
    }
  }

  static void openAppointment(final Appointment appointment, final Long calendarId) {
    openAppointment(appointment, calendarId, null);
  }

  public static void openAppointment(final Appointment appointment, final Long calendarId,
      String form) {
    Assert.notNull(appointment);

    if (!appointment.isVisible(BeeKeeper.getUser().getUserId())) {
      BeeKeeper.getScreen().notifyInfo(CalendarVisibility.PRIVATE.getCaption());
      return;
    }
    if (appointment.handlesOpenAction()) {
      return;
    }
    BeeRow typeRow = getAppointmentTypeRow(appointment);
    String formName;
    if (BeeUtils.isEmpty(form)) {
      formName = (typeRow == null) ? null : Data.getString(VIEW_APPOINTMENT_TYPES, typeRow,
          COL_APPOINTMENT_EDITOR);
      if (BeeUtils.isEmpty(formName)) {
        formName = DEFAULT_EDIT_APPOINTMENT_FORM;
      }
    } else {
      formName = form;
    }

    final AppointmentBuilder builder = new AppointmentBuilder(false);

    FormFactory.createFormView(formName, VIEW_APPOINTMENTS,
        getAppointmentViewColumns(), false, builder, (formDescription, result) -> {
          if (result != null) {
            result.start(null);
            if (!appointment.isEditable(BeeKeeper.getUser().getUserId())) {
              result.setEnabled(false);
            }

            result.updateRow(DataUtils.cloneRow(appointment.getRow()), false);

            builder.setProperties(appointment.getProperties());
            builder.setReminders(appointment.getReminders());

            builder.setColor(appointment.getColor());

            builder.setRequiredFields(formDescription.getOptions());

            builder.setAppointmentView(result);

            boolean companyAndVehicle = builder.isRequired(ClassifierConstants.COL_COMPANY)
                && builder.isRequired(COL_VEHICLE);
            SELECTOR_HANDLER.setCompanyHandlerEnabled(companyAndVehicle);
            SELECTOR_HANDLER.setVehicleHandlerEnabled(companyAndVehicle);

            if (DataUtils.isId(calendarId)) {
              CalendarController controller = getController(calendarId);
              if (controller != null) {
                builder.setUcAttendees(controller.getAttendees());
              }
            }

            Set<Action> enabledActions;

            if (Data.isViewEditable(VIEW_APPOINTMENTS)
                && BeeKeeper.getUser().canDeleteData(VIEW_APPOINTMENTS)
                && appointment.isRemovable(BeeKeeper.getUser().getUserId())) {
              enabledActions = EnumSet.of(Action.DELETE, Action.PRINT);
            } else {
              enabledActions = EnumSet.of(Action.PRINT);
            }

            String caption = result.isEnabled() ? result.getCaption()
                : BeeUtils.joinWords(result.getCaption(),
                BeeUtils.bracket(Localized.dictionary().rowIsReadOnly().trim()));

            DialogBox box = Global.inputWidget(caption, result, builder.getModalCallback(null),
                RowEditor.DIALOG_STYLE, null, enabledActions);

            box.addOpenHandler(event -> result.refresh());

            Global.getNewsAggregator().onAccess(VIEW_APPOINTMENTS, appointment.getId());
          }
        });
  }

  static void renderItem(long calendarId, ItemWidget itemWidget, boolean multi) {
    CalendarItem item = itemWidget.getItem();

    BeeRow row = getAppointmentTypeRow(item);
    if (row == null) {
      if (multi) {
        ITEM_RENDERER.renderMulti(calendarId, itemWidget);
      } else {
        ITEM_RENDERER.renderSimple(calendarId, itemWidget);
      }

    } else {
      String viewName = VIEW_APPOINTMENT_TYPES;
      String header = Data.getString(viewName, row, multi ? COL_MULTI_HEADER : COL_SIMPLE_HEADER);
      String body = Data.getString(viewName, row, multi ? COL_MULTI_BODY : COL_SIMPLE_BODY);
      String title = Data.getString(viewName, row, COL_APPOINTMENT_TITLE);

      ITEM_RENDERER.render(calendarId, itemWidget, header, body, title, multi);
    }

    BeeRow styleRow = getStyleRow(item, row);
    if (styleRow != null) {
      String viewName = VIEW_APPOINTMENT_STYLES;
      String panel = Data.getString(viewName, styleRow, multi ? COL_MULTI : COL_SIMPLE);

      String header = Data.getString(viewName, styleRow, COL_HEADER);
      String body = Data.getString(viewName, styleRow, COL_BODY);
      String footer = Data.getString(viewName, styleRow, COL_FOOTER);

      CalendarStyleManager.applyStyle(itemWidget, panel, header, body, footer);
    }
  }

  static void renderCompact(long calendarId, ItemWidget panel, Widget htmlWidget,
      Widget titleWidget) {

    BeeRow row = getAppointmentTypeRow(panel.getItem());

    String compact;
    String title;

    if (row == null) {
      compact = null;
      title = null;
    } else {
      String viewName = VIEW_APPOINTMENT_TYPES;
      compact = Data.getString(viewName, row, COL_APPOINTMENT_COMPACT);
      title = Data.getString(viewName, row, COL_APPOINTMENT_TITLE);
    }

    ITEM_RENDERER.renderCompact(calendarId, panel.getItem(), compact, htmlWidget,
        title, titleWidget);

    BeeRow styleRow = getStyleRow(panel.getItem(), row);
    if (styleRow != null) {
      String styles = Data.getString(VIEW_APPOINTMENT_STYLES, styleRow, COL_COMPACT);
      CalendarStyleManager.applyStyle(panel, styles);
    }
  }

  static void saveActiveView(final CalendarSettings settings) {
    if (settings != null && settings.getActiveView() != null) {
      ParameterList params = createArgs(SVC_SAVE_ACTIVE_VIEW);
      params.addQueryItem(PARAM_USER_CALENDAR_ID, settings.getId());
      params.addQueryItem(PARAM_ACTIVE_VIEW, settings.getActiveView().ordinal());

      BeeKeeper.getRpc().makeRequest(params);
    }
  }

  static boolean showsTasks(long calendarId) {
    Boolean value = CACHE.getBoolean(VIEW_CALENDARS, calendarId, COL_ASSIGNED_TASKS);
    if (BeeUtils.isTrue(value)) {
      return true;
    }

    value = CACHE.getBoolean(VIEW_CALENDARS, calendarId, COL_DELEGATED_TASKS);
    if (BeeUtils.isTrue(value)) {
      return true;
    }

    value = CACHE.getBoolean(VIEW_CALENDARS, calendarId, COL_OBSERVED_TASKS);
    if (BeeUtils.isTrue(value)) {
      return true;
    }

    Filter filter = Filter.equals(COL_CALENDAR, calendarId);
    return CACHE.contains(VIEW_CALENDAR_EXECUTORS, filter)
        || CACHE.contains(VIEW_CAL_EXECUTOR_GROUPS, filter);
  }

  static void synchronizeDate(long calendarId, JustDate date, boolean sourceIsController) {
    if (date == null) {
      return;
    }

    if (sourceIsController) {
      Set<CalendarPanel> panels = getActivePanels(calendarId);
      for (CalendarPanel panel : panels) {
        panel.setDate(date, false);
      }
    } else {
      CalendarController controller = getController(calendarId);
      if (controller != null) {
        controller.setDate(date);
      }
    }
  }

  static void updatePanels(long calendarId, BeeRowSet ucAttendees) {
    Set<CalendarPanel> panels = getActivePanels(calendarId);
    for (CalendarPanel panel : panels) {
      panel.updateUcAttendees(ucAttendees, true);
    }
  }

  private static void createSettingsForm(final BeeRowSet rowSet, final CalendarPanel cp) {
    FormFactory.createFormView(FORM_CALENDAR_SETTINGS, null, rowSet.getColumns(), false,
        (formDescription, result) -> {
          if (result != null && getSettingsForm() == null) {
            setSettingsForm(result);
            getSettingsForm().setEditing(true);
            getSettingsForm().start(null);
          }

          openSettingsForm(rowSet, cp);
        });
  }

  private static Set<CalendarPanel> getActivePanels(long calendarId) {
    Set<CalendarPanel> panels = new HashSet<>();

    for (Map.Entry<String, Long> entry : activePanels.entrySet()) {
      if (entry.getValue() == calendarId) {
        Widget widget = DomUtils.getWidget(entry.getKey());
        if (widget instanceof CalendarPanel) {
          panels.add((CalendarPanel) widget);
        } else {
          logger.warning("Calendar panel", entry.getKey(), entry.getValue(), "not found");
        }
      }
    }
    return panels;
  }

  private static String getAttendeeName(long id) {
    return CACHE.getString(VIEW_ATTENDEES, id, COL_ATTENDEE_NAME);
  }

  private static CalendarVisibility getCalendarVisibility(Long calendarId) {
    if (DataUtils.isId(calendarId)) {
      Integer value = CACHE.getInteger(VIEW_CALENDARS, calendarId, COL_VISIBILITY);
      return EnumUtils.getEnumByIndex(CalendarVisibility.class, value);
    } else {
      return null;
    }
  }

  private static CalendarController getController(long calendarId) {
    String ccId = activeControllers.get(calendarId);
    if (BeeUtils.isEmpty(ccId)) {
      return null;
    }

    Widget widget = DomUtils.getWidget(ccId);
    if (widget instanceof CalendarController) {
      return (CalendarController) widget;
    } else {
      return null;
    }
  }

  private static FormView getSettingsForm() {
    return settingsForm;
  }

  private static BeeRow getStyleRow(CalendarItem item, BeeRow typeRow) {
    Long style = item.getStyle();
    if (style == null && typeRow != null) {
      style = Data.getLong(VIEW_APPOINTMENT_TYPES, typeRow, COL_STYLE);
    }

    if (style == null) {
      return null;
    } else {
      return CACHE.getRow(VIEW_APPOINTMENT_STYLES, style);
    }
  }

  private static void getUserCalendar(long id, final Queries.RowSetCallback callback) {
    ParameterList params = createArgs(SVC_GET_USER_CALENDAR);
    params.addQueryItem(PARAM_CALENDAR_ID, id);

    BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(BeeRowSet.class)) {
          callback.onSuccess(BeeRowSet.restore((String) response.getResponse()));
        }
      }
    });
  }

  private static void onCreatePanel(long calendarId, long ucId, String caption,
      BeeRowSet ucAttendees) {

    if (!BeeKeeper.getScreen().containsDomainEntry(Domain.CALENDAR, calendarId)) {
      CalendarController calendarController = new CalendarController(calendarId, ucId,
          caption, ucAttendees);
      activeControllers.put(calendarId, calendarController.getId());

      BeeKeeper.getScreen().addDomainEntry(Domain.CALENDAR, calendarController, calendarId,
          caption);
    }
  }

  private static void openCalendar(final long id, final String name, ViewCallback callback) {
    CalendarOpener opener = new CalendarOpener(id, name, callback);
    ensureData(opener);
  }

  private static void openSettingsForm(final BeeRowSet rowSet, final CalendarPanel cp) {
    if (getSettingsForm() == null || getSettingsForm().asWidget().isAttached()) {
      return;
    }

    final BeeRow oldRow = rowSet.getRow(0);
    final BeeRow newRow = DataUtils.cloneRow(oldRow);

    if (cp.getSettings().getActiveView() != null) {
      Data.setValue(VIEW_USER_CALENDARS, newRow, COL_ACTIVE_VIEW,
          cp.getSettings().getActiveView().ordinal());
    }

    getSettingsForm().updateRow(newRow, false);

    String caption = getSettingsForm().getCaption();

    Global.inputWidget(caption, getSettingsForm(), new InputCallback() {
      @Override
      public String getErrorMessage() {
        if (getSettingsForm().checkOnSave(null)
            && getSettingsForm().validate(getSettingsForm(), true)) {
          return null;
        } else {
          return InputBoxes.SILENT_ERROR;
        }
      }

      @Override
      public void onClose(CloseCallback closeCallback) {
        if (getSettingsForm().checkOnClose(null)) {
          getSettingsForm().onClose(closeCallback);
        }
      }

      @Override
      public void onSuccess() {
        int updCount = Queries.update(VIEW_USER_CALENDARS, rowSet.getColumns(), oldRow, newRow,
            getSettingsForm().getChildrenForUpdate(), null);

        if (updCount > 0) {
          boolean requery = false;

          List<String> colNames = Lists.newArrayList(COL_MULTIDAY_LAYOUT,
              COL_MULTIDAY_TASK_LAYOUT, COL_WORKING_HOUR_START, COL_WORKING_HOUR_END);

          for (String colName : colNames) {
            int index = rowSet.getColumnIndex(colName);
            if (!Objects.equals(oldRow.getString(index), newRow.getString(index))) {
              requery = true;
              break;
            }
          }

          cp.updateSettings(newRow, rowSet.getColumns(), requery);
        }
      }
    });
  }

  private static void setSettingsForm(FormView settingsForm) {
    CalendarKeeper.settingsForm = settingsForm;
  }

  private CalendarKeeper() {
  }
}
