package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.client.calendar.Attendee;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class Appointment implements Comparable<Appointment> {

  private final IsRow row;
  
  private final List<Attendee> attendees = Lists.newArrayList();
  private final List<String> properties = Lists.newArrayList();

  public Appointment(IsRow row) {
    this.row = row;
  }

  public Appointment clone() {
    Appointment clone = new Appointment(DataUtils.cloneRow(row));
    if (!getAttendees().isEmpty()) {
      clone.getAttendees().addAll(getAttendees());
    }

    return clone;
  }

  public int compareTo(Appointment appointment) {
    int compare = BeeUtils.compare(getStart(), appointment.getStart());
    if (compare == BeeConst.COMPARE_EQUAL) {
      compare = BeeUtils.compare(appointment.getEnd(), getEnd());
    }
    return compare;
  }
  
  public List<Attendee> getAttendees() {
    return attendees;
  }

  public String getBackground() {
    return row.getString(CalendarKeeper.getAppointmentColumnIndex(COL_BACKGROUND));
  }

  public String getCompanyName() {
    return row.getString(CalendarKeeper.getAppointmentColumnIndex(COL_COMPANY_NAME));
  }
  
  public String getDescription() {
    return row.getString(CalendarKeeper.getAppointmentColumnIndex(COL_DESCRIPTION));
  }

  public DateTime getEnd() {
    return row.getDateTime(CalendarKeeper.getAppointmentColumnIndex(COL_END_DATE_TIME));
  }

  public String getForeground() {
    return row.getString(CalendarKeeper.getAppointmentColumnIndex(COL_FOREGROUND));
  }
  
  public long getId() {
    return row.getId();
  }

  public List<String> getProperties() {
    return properties;
  }

  public DateTime getStart() {
    return row.getDateTime(CalendarKeeper.getAppointmentColumnIndex(COL_START_DATE_TIME));
  }

  public String getSummary() {
    return row.getString(CalendarKeeper.getAppointmentColumnIndex(COL_SUMMARY));
  }

  public String getVehicleModel() {
    return row.getString(CalendarKeeper.getAppointmentColumnIndex(COL_VEHICLE_MODEL));
  }

  public String getVehicleNumber() {
    return row.getString(CalendarKeeper.getAppointmentColumnIndex(COL_VEHICLE_NUMBER));
  }

  public String getVehicleParentModel() {
    return row.getString(CalendarKeeper.getAppointmentColumnIndex(COL_VEHICLE_PARENT_MODEL));
  }
  
  public boolean isAllDay() {
    return isMultiDay();
  }

  public boolean isMultiDay() {
    if (getStart() != null) {
      return !TimeUtils.sameDate(getStart(), getEnd());
    } else {
      return false;
    }
  }

  public void setEnd(DateTime end) {
    row.setValue(CalendarKeeper.getAppointmentColumnIndex(COL_END_DATE_TIME), end);
  }

  public void setStart(DateTime start) {
    row.setValue(CalendarKeeper.getAppointmentColumnIndex(COL_START_DATE_TIME), start);
  }
}
