package com.butent.bee.client.modules.service;

import com.google.common.collect.Range;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.modules.service.ServiceConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

class ServiceDateWrapper implements HasDateRange {

  private final Long objectId;

  private final DateTime from;
  private final DateTime until;

  private final Range<JustDate> range;

  private final String color;
  private final String note;

  private final String title;

  ServiceDateWrapper(SimpleRow row) {
    this.objectId = row.getLong(ServiceConstants.COL_SERVICE_OBJECT);

    this.from = row.getDateTime(ServiceConstants.COL_SERVICE_DATE_FROM);
    this.until = row.getDateTime(ServiceConstants.COL_SERVICE_DATE_UNTIL);

    this.color = row.getValue(ServiceConstants.COL_SERVICE_DATE_COLOR);
    this.note = row.getValue(ServiceConstants.COL_SERVICE_DATE_NOTE);

    this.range = TimeBoardHelper.getRange(from, until);

    String period = BeeUtils.isMore(until, from)
        ? Format.renderPeriod(from, until) : Format.renderDateTime(from);

    this.title = BeeUtils.buildLines(period, note);
  }

  @Override
  public Range<JustDate> getRange() {
    return range;
  }

  String getColor() {
    return color;
  }

  DateTime getFrom() {
    return from;
  }

  String getNote() {
    return note;
  }

  Long getObjectId() {
    return objectId;
  }

  String getTitle() {
    return title;
  }

  DateTime getUntil() {
    return until;
  }
}
