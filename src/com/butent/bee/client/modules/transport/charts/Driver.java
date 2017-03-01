package com.butent.bee.client.modules.transport.charts;

import com.google.common.collect.Range;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class Driver extends Filterable implements HasDateRange, HasItemName {

  private static final String startDateLabel =
      Data.getColumnLabel(VIEW_DRIVERS, COL_DRIVER_START_DATE);
  private static final String endDateLabel = Data.getColumnLabel(VIEW_DRIVERS, COL_DRIVER_END_DATE);
  private static final String experienceLabel =
      Data.getColumnLabel(VIEW_DRIVERS, COL_DRIVER_EXPERIENCE);
  private static final String notesLabel = Data.getColumnLabel(VIEW_DRIVERS, COL_DRIVER_NOTES);

  static String getNames(String separator, Collection<Driver> drivers) {
    if (BeeUtils.isEmpty(drivers)) {
      return null;
    }

    String sep = BeeUtils.notEmpty(separator, BeeConst.DEFAULT_LIST_SEPARATOR);
    StringBuilder sb = new StringBuilder();

    for (Driver driver : drivers) {
      if (driver != null) {
        if (sb.length() > 0) {
          sb.append(sep);
        }
        sb.append(driver.getItemName());
      }
    }
    return sb.toString();
  }

  private final Long id;
  private final String itemName;

  private final Set<Long> groups = new HashSet<>();

  private final Range<JustDate> range;

  private final String title;

  Driver(Long driverId, String firstName, String lastName, DateTime startDate, DateTime endDate,
      String notes) {
    this(driverId, firstName, lastName, JustDate.get(startDate), JustDate.get(endDate), null,
        notes, null);
  }

  Driver(Long driverId, String firstName, String lastName, JustDate startDate, JustDate endDate,
      JustDate experience, String notes, Set<Long> groups) {

    this.id = driverId;
    this.itemName = BeeUtils.joinWords(firstName, lastName);

    if (!BeeUtils.isEmpty(groups)) {
      this.groups.addAll(groups);
    }

    this.range = TimeBoardHelper.getActivity(startDate, endDate);

    this.title = TimeBoardHelper.buildTitle(startDateLabel, Format.renderDateTime(startDate),
        endDateLabel, Format.renderDateTime(endDate),
        experienceLabel, Format.renderDateTime(experience), notesLabel, notes);
  }

  @Override
  public String getItemName() {
    return itemName;
  }

  @Override
  public Range<JustDate> getRange() {
    return range;
  }

  Set<Long> getGroups() {
    return groups;
  }

  Long getId() {
    return id;
  }

  String getTitle() {
    return title;
  }
}
