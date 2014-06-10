package com.butent.bee.client.modules.transport.charts;

import com.google.common.collect.Range;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

class VehicleService implements HasDateRange {

  private final Long vehicleId;
  private final String vehicleNumber;

  private final String name;
  private final String notes;

  private final Range<JustDate> range;

  VehicleService(SimpleRow row) {
    super();

    this.vehicleId = row.getLong(COL_VEHICLE);
    this.vehicleNumber = row.getValue(COL_NUMBER);

    this.name = row.getValue(COL_VEHICLE_SERVICE_NAME);
    this.notes = row.getValue(COL_VEHICLE_SERVICE_NOTES);

    JustDate start = row.getDate(COL_VEHICLE_SERVICE_DATE);
    JustDate end = row.getDate(COL_VEHICLE_SERVICE_DATE_TO);

    this.range = Range.closed(start, BeeUtils.max(start, end));
  }

  @Override
  public Range<JustDate> getRange() {
    return range;
  }

  Widget createWidget(final ChartBase chart, String containerStyle, String labelStyle) {
    Flow panel = new Flow();
    panel.addStyleName(containerStyle);

    panel.setTitle(BeeUtils.buildLines(TimeBoardHelper.getRangeLabel(getRange()), getName(),
        getNotes()));

    panel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        chart.openDataRow(event, VIEW_VEHICLES, getVehicleId());
      }
    });

    Label label = new Label(getName());
    label.addStyleName(labelStyle);

    panel.add(label);

    return panel;
  }

  String getName() {
    return name;
  }

  String getNotes() {
    return notes;
  }

  Long getVehicleId() {
    return vehicleId;
  }

  String getVehicleNumber() {
    return vehicleNumber;
  }
}
