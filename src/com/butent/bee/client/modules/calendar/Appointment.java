package com.butent.bee.client.modules.calendar;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import static com.butent.bee.shared.modules.calendar.CalendarHelper.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.COL_SERVICE_EVENT;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.ALS_COMPANY_NAME;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.cars.CarServiceEvent;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.calendar.CalendarConstants.*;
import com.butent.bee.shared.modules.calendar.CalendarItem;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Appointment extends CalendarItem {

  private static final int BACKGROUND_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      AdministrationConstants.COL_BACKGROUND);
  private static final int FOREGROUND_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      AdministrationConstants.COL_FOREGROUND);
  private static final int COLOR_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      AdministrationConstants.COL_COLOR);

  private static final int COMPANY_NAME_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      ALS_COMPANY_NAME);

  private static final int CREATOR_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS, COL_CREATOR);
  private static final int CREATOR_FIRST_NAME_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      ALS_CREATOR_FIRST_NAME);
  private static final int CREATOR_LAST_NAME_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      ALS_CREATOR_LAST_NAME);

  private static final int SUMMARY_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS, COL_SUMMARY);
  private static final int DESCRIPTION_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_DESCRIPTION);

  private static final int START_DATE_TIME_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_START_DATE_TIME);
  private static final int END_DATE_TIME_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_END_DATE_TIME);

  private static final int APPOINTMENT_TYPE_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_APPOINTMENT_TYPE);
  private static final int STYLE_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS, COL_STYLE);

  private static final int STATUS_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS, COL_STATUS);

  private static final int VEHICLE_MODEL_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_VEHICLE_MODEL);
  private static final int VEHICLE_NUMBER_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_VEHICLE_NUMBER);
  private static final int VEHICLE_BRAND_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_VEHICLE_BRAND);

  private static final int VISIBILITY_INDEX = Data.getColumnIndex(VIEW_APPOINTMENTS,
      COL_VISIBILITY);

  private static final String SIMPLE_HEADER_TEMPLATE;
  private static final String SIMPLE_BODY_TEMPLATE;

  private static final String PARTIAL_HEADER_TEMPLATE;
  private static final String PARTIAL_BODY_TEMPLATE;

  private static final String MULTI_HEADER_TEMPLATE;
  private static final String MULTI_BODY_TEMPLATE;

  private static final String COMPACT_TEMPLATE;
  private static final String TITLE_TEMPLATE;

  private static final String STRING_TEMPLATE;

  private static final String KEY_RESOURCES = "Resources";
  private static final String KEY_OWNERS = "Owners";
  private static final String KEY_PROPERTIES = "Properties";
  private static final String KEY_REMINDERS = "Reminders";

  private static final String KEY_CREATOR_NAME = "CreatorName";

  static {
    SIMPLE_HEADER_TEMPLATE = wrap(COL_SUMMARY);
    SIMPLE_BODY_TEMPLATE = BeeUtils.buildLines(wrap(COL_APPOINTMENT_LOCATION),
        wrap(ALS_COMPANY_NAME), BeeUtils.joinWords(wrap(COL_VEHICLE_BRAND),
            wrap(COL_VEHICLE_MODEL)),
        wrap(COL_VEHICLE_NUMBER), wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES),
        wrap(KEY_OWNERS), wrap(COL_DESCRIPTION));

    PARTIAL_HEADER_TEMPLATE = wrap(COL_SUMMARY);
    PARTIAL_BODY_TEMPLATE = BeeUtils.buildLines(wrap(KEY_PERIOD), wrap(COL_APPOINTMENT_LOCATION),
        wrap(ALS_COMPANY_NAME), BeeUtils.joinWords(wrap(COL_VEHICLE_BRAND),
            wrap(COL_VEHICLE_MODEL)),
        wrap(COL_VEHICLE_NUMBER), wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES),
        wrap(KEY_OWNERS), wrap(COL_DESCRIPTION));

    MULTI_HEADER_TEMPLATE = BeeUtils.joinWords(wrap(KEY_PERIOD), wrap(COL_SUMMARY));
    MULTI_BODY_TEMPLATE = BeeUtils.joinWords(wrap(COL_APPOINTMENT_LOCATION),
        wrap(ALS_COMPANY_NAME), wrap(COL_VEHICLE_BRAND), wrap(COL_VEHICLE_MODEL),
        wrap(COL_VEHICLE_NUMBER), wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES), wrap(KEY_OWNERS));

    COMPACT_TEMPLATE = BeeUtils.joinWords(wrap(COL_SUMMARY), wrap(KEY_PERIOD));

    TITLE_TEMPLATE = BeeUtils.buildLines(wrap(KEY_PERIOD), wrap(COL_STATUS), wrap(COL_SUMMARY),
        wrap(COL_APPOINTMENT_LOCATION), wrap(ALS_COMPANY_NAME),
        wrap(COL_VEHICLE_MODEL), wrap(COL_VEHICLE_NUMBER),
        wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES), wrap(KEY_OWNERS), wrap(COL_DESCRIPTION),
        wrap(KEY_REMINDERS), wrap(KEY_CREATOR_NAME));

    STRING_TEMPLATE = BeeUtils.buildLines(wrap(KEY_PERIOD), wrap(COL_STATUS),
        wrap(COL_SUMMARY), wrap(COL_APPOINTMENT_LOCATION), wrap(ALS_COMPANY_NAME),
        BeeUtils.joinWords(wrap(COL_VEHICLE_BRAND), wrap(COL_VEHICLE_MODEL),
            wrap(COL_VEHICLE_NUMBER)),
        wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES), wrap(KEY_OWNERS), wrap(COL_DESCRIPTION),
        wrap(KEY_REMINDERS));
  }

  private final IsRow row;

  private final List<Long> attendees = new ArrayList<>();
  private final List<Long> owners = new ArrayList<>();
  private final List<Long> properties = new ArrayList<>();
  private final List<Long> reminders = new ArrayList<>();

  private final Long separatedAttendee;

  protected Appointment(IsRow row, Long separatedAttendee) {
    this.row = row;
    this.separatedAttendee = separatedAttendee;

    String attList = row.getProperty(TBL_APPOINTMENT_ATTENDEES);
    if (!BeeUtils.isEmpty(attList)) {
      attendees.addAll(DataUtils.parseIdList(attList));
    }

    String ownerList = row.getProperty(TBL_APPOINTMENT_OWNERS);
    if (!BeeUtils.isEmpty(ownerList)) {
      owners.addAll(DataUtils.parseIdList(ownerList));
    }

    String propList = row.getProperty(TBL_APPOINTMENT_PROPS);
    if (!BeeUtils.isEmpty(propList)) {
      properties.addAll(DataUtils.parseIdList(propList));
    }

    String remindList = row.getProperty(TBL_APPOINTMENT_REMINDERS);
    if (!BeeUtils.isEmpty(remindList)) {
      reminders.addAll(DataUtils.parseIdList(remindList));
    }
  }

  @Override
  public CalendarItem copy() {
    return Appointment.create(getRow(), getSeparatedAttendee());
  }

  public static Appointment create(IsRow row) {
    return Appointment.create(row, null);
  }

  public static Appointment create(IsRow row, Long separatedAttendee) {
    if (DataUtils.isId(Data.getLong(VIEW_APPOINTMENTS, row, COL_SERVICE_EVENT))) {
      return new CarServiceEvent(row, separatedAttendee);
    }
    return new Appointment(row, separatedAttendee);
  }

  public List<Long> getAttendees() {
    return attendees;
  }

  @Override
  public String getBackground() {
    return row.getString(BACKGROUND_INDEX);
  }

  public Long getColor() {
    return row.getLong(COLOR_INDEX);
  }

  @Override
  public String getCompactTemplate() {
    return COMPACT_TEMPLATE;
  }

  @Override
  public String getCompanyName() {
    return row.getString(COMPANY_NAME_INDEX);
  }

  public Long getCreator() {
    return row.getLong(CREATOR_INDEX);
  }

  public String getCreatorName() {
    return BeeUtils.joinWords(row.getString(CREATOR_FIRST_NAME_INDEX),
        row.getString(CREATOR_LAST_NAME_INDEX));
  }

  @Override
  public String getDescription() {
    return row.getString(DESCRIPTION_INDEX);
  }

  @Override
  public String getForeground() {
    return row.getString(FOREGROUND_INDEX);
  }

  @Override
  public long getId() {
    return row.getId();
  }

  @Override
  public ItemType getItemType() {
    return ItemType.APPOINTMENT;
  }

  @Override
  public String getMultiBodyTemplate() {
    return MULTI_BODY_TEMPLATE;
  }

  @Override
  public String getMultiHeaderTemplate() {
    return MULTI_HEADER_TEMPLATE;
  }

  public List<Long> getOwners() {
    return owners;
  }

  @Override
  public String getPartialBodyTemplate() {
    return PARTIAL_BODY_TEMPLATE;
  }

  @Override
  public String getPartialHeaderTemplate() {
    return PARTIAL_HEADER_TEMPLATE;
  }

  public List<Long> getProperties() {
    return properties;
  }

  public List<Long> getReminders() {
    return reminders;
  }

  public IsRow getRow() {
    return row;
  }

  @Override
  public Long getSeparatedAttendee() {
    return separatedAttendee;
  }

  @Override
  public String getSimpleBodyTemplate() {
    return SIMPLE_BODY_TEMPLATE;
  }

  @Override
  public String getSimpleHeaderTemplate() {
    return SIMPLE_HEADER_TEMPLATE;
  }

  @Override
  public AppointmentStatus getStatus() {
    return EnumUtils.getEnumByIndex(AppointmentStatus.class, row.getInteger(STATUS_INDEX));
  }

  @Override
  public String getStringTemplate() {
    return STRING_TEMPLATE;
  }

  @Override
  public Long getStyle() {
    return row.getLong(STYLE_INDEX);
  }

  @Override
  public Map<String, String> getSubstitutes(long calendarId, Map<Long, UserData> users,
      boolean addLabels, Function<HasDateValue, String> dateTimeRenderer,
      BiFunction<HasDateValue, HasDateValue, String> periodRenderer) {

    Map<String, String> result = new HashMap<>();

    List<BeeColumn> columns = CalendarKeeper.getAppointmentViewColumns();

    for (int i = 0; i < columns.size(); i++) {
      BeeColumn column = columns.get(i);
      String key = column.getId();
      String value = DataUtils.render(column, row, i, Format.getDateRenderer(),
          Format.getDateTimeRenderer());

      result.put(wrap(key), build(Localized.getLabel(column), value, addLabels));
    }

    List<String> attNames = new ArrayList<>();
    List<String> ownerNames = new ArrayList<>();
    List<String> propNames = new ArrayList<>();
    List<String> remindNames = new ArrayList<>();

    if (!getAttendees().isEmpty()) {
      for (Long id : getAttendees()) {
        attNames.add(CalendarKeeper.getAttendeeCaption(calendarId, id));
      }
    }

    if (!getOwners().isEmpty()) {
      for (Long id : getOwners()) {
        if (users.containsKey(id)) {
          ownerNames.add(users.get(id).getUserSign());
        }
      }
    }

    if (!getProperties().isEmpty()) {
      for (Long id : getProperties()) {
        propNames.add(CalendarKeeper.getPropertyName(id));
      }
    }

    if (!getReminders().isEmpty()) {
      for (Long id : getReminders()) {
        remindNames.add(CalendarKeeper.getReminderTypeName(id));
      }
    }

    result.put(wrap(KEY_RESOURCES), build(Localized.dictionary().calAttendees(),
        joinChildren(attNames), addLabels));
    result.put(wrap(KEY_OWNERS), build(Localized.dictionary().responsiblePersons(),
        joinChildren(ownerNames), addLabels));
    result.put(wrap(KEY_PROPERTIES), build(Localized.dictionary().calParameters(),
        joinChildren(propNames), addLabels));
    result.put(wrap(KEY_REMINDERS), joinChildren(remindNames));

    result.put(wrap(KEY_PERIOD), build(Localized.dictionary().period(),
        Format.renderPeriod(getStart(), getEnd()), addLabels));

    result.put(wrap(KEY_CREATOR_NAME), build(Localized.dictionary().creator(), getCreatorName(),
        addLabels));

    return result;
  }

  @Override
  public String getSummary() {
    return row.getString(SUMMARY_INDEX);
  }

  @Override
  public String getTitleTemplate() {
    return TITLE_TEMPLATE;
  }

  public Long getType() {
    return row.getLong(APPOINTMENT_TYPE_INDEX);
  }

  public String getVehicleModel() {
    return row.getString(VEHICLE_MODEL_INDEX);
  }

  public String getVehicleNumber() {
    return row.getString(VEHICLE_NUMBER_INDEX);
  }

  public String getVehicleBrand() {
    return row.getString(VEHICLE_BRAND_INDEX);
  }

  public boolean handlesCopyAction(DateTime newStart, DateTime newEnd) {
    return false;
  }

  public boolean handlesOpenAction() {
    return false;
  }

  @Override
  public boolean isEditable(Long userId) {
    CalendarVisibility visibility = getVisibility();
    return (visibility == null || visibility == CalendarVisibility.EDITABLE || isOwner(userId))
        && row.isEditable() && Data.isViewEditable(VIEW_APPOINTMENTS);
  }

  @Override
  public boolean isMovable(Long userId) {
    return isWhole() && isEditable(userId);
  }

  @Override
  public boolean isRemovable(Long userId) {
    return isEditable(userId) && row.isRemovable();
  }

  @Override
  public boolean isResizable(Long userId) {
    return isWhole() && isEditable(userId);
  }

  @Override
  public boolean isVisible(Long userId) {
    if (userId == null) {
      return false;

    } else if (isOwner(userId)) {
      return true;

    } else {
      return getVisibility() != CalendarVisibility.PRIVATE;
    }
  }

  public void setEnd(DateTime end) {
    row.setValue(END_DATE_TIME_INDEX, end);
  }

  public void setStart(DateTime start) {
    row.setValue(START_DATE_TIME_INDEX, start);
  }

  public void updateAttendees(List<Long> ids) {
    attendees.clear();
    if (!BeeUtils.isEmpty(ids)) {
      attendees.addAll(ids);
    }
    row.setProperty(TBL_APPOINTMENT_ATTENDEES, DataUtils.buildIdList(ids));
  }

  @Override
  protected DateTime getEnd() {
    return row.getDateTime(END_DATE_TIME_INDEX);
  }

  @Override
  protected DateTime getStart() {
    return row.getDateTime(START_DATE_TIME_INDEX);
  }

  private CalendarVisibility getVisibility() {
    return EnumUtils.getEnumByIndex(CalendarVisibility.class, row.getInteger(VISIBILITY_INDEX));
  }

  private boolean isOwner(Long userId) {
    return userId != null && (owners.contains(userId) || userId.equals(getCreator()));
  }
}
