package com.butent.bee.client.modules.transport.charts;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.gwt.event.dom.client.DropEvent;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.DndTarget;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;
import java.util.Set;

class Vehicle extends Filterable implements HasDateRange, HasItemName {

  private static final int numberIndex = Data.getColumnIndex(VIEW_VEHICLES, COL_NUMBER);

  private static final int parentModelNameIndex =
      Data.getColumnIndex(VIEW_VEHICLES, COL_PARENT_MODEL_NAME);
  private static final int modelNameIndex = Data.getColumnIndex(VIEW_VEHICLES, COL_MODEL_NAME);
  private static final String modelLabel = Data.getColumnLabel(VIEW_VEHICLES, COL_MODEL);

  private static final int typeNameIndex = Data.getColumnIndex(VIEW_VEHICLES, COL_TYPE_NAME);

  private static final int notesIndex = Data.getColumnIndex(VIEW_VEHICLES, COL_VEHICLE_NOTES);
  private static final String notesLabel = Data.getColumnLabel(VIEW_VEHICLES, COL_VEHICLE_NOTES);

  private static final int startIndex =
      Data.getColumnIndex(VIEW_VEHICLES, COL_VEHICLE_START_DATE);
  private static final int endIndex = Data.getColumnIndex(VIEW_VEHICLES, COL_VEHICLE_END_DATE);
  private static final String startLabel =
      Data.getColumnLabel(VIEW_VEHICLES, COL_VEHICLE_START_DATE);
  private static final String endLabel = Data.getColumnLabel(VIEW_VEHICLES, COL_VEHICLE_END_DATE);

  private static final Set<String> acceptsDropTypes =
      ImmutableSet.of(DATA_TYPE_TRIP, DATA_TYPE_FREIGHT, DATA_TYPE_ORDER_CARGO);

  private final BeeRow row;

  private final Long id;
  private final String number;
  private final String model;
  private final String type;

  private final Range<JustDate> range;

  private final String itemName;

  Vehicle(BeeRow row) {
    this.row = row;

    this.id = row.getId();
    this.number = row.getString(numberIndex);
    this.model = BeeUtils.joinWords(row.getString(parentModelNameIndex),
        row.getString(modelNameIndex));
    this.type = BeeUtils.trim(row.getString(typeNameIndex));

    this.range = TimeBoardHelper.getActivity(row.getDate(startIndex), row.getDate(endIndex));

    this.itemName = BeeUtils.joinWords(number, model);
  }

  @Override
  public String getItemName() {
    return itemName;
  }

  @Override
  public Range<JustDate> getRange() {
    return range;
  }

  Long getId() {
    return id;
  }

  String getInactivityTitle(Range<JustDate> inactivity) {
    if (inactivity == null || getRange() == null) {
      return BeeConst.STRING_EMPTY;

    } else if (inactivity.hasUpperBound() && getRange().hasLowerBound()
        && BeeUtils.isLess(inactivity.upperEndpoint(), getRange().lowerEndpoint())) {
      return TimeBoardHelper.buildTitle(startLabel, getRange().lowerEndpoint());

    } else if (inactivity.hasLowerBound() && getRange().hasUpperBound()
        && BeeUtils.isMore(inactivity.lowerEndpoint(), getRange().upperEndpoint())) {
      return TimeBoardHelper.buildTitle(endLabel, getRange().upperEndpoint());

    } else {
      return BeeConst.STRING_EMPTY;
    }
  }

  String getInfo() {
    return BeeUtils.joinWords(getModel(), getNotes());
  }

  String getMessage(String caption) {
    return TimeBoardHelper.buildTitle(caption, getNumber(), modelLabel, getModel(),
        notesLabel, getNotes());
  }

  String getModel() {
    return model;
  }

  String getNotes() {
    return BeeUtils.trim(row.getString(notesIndex));
  }

  String getNumber() {
    return number;
  }

  String getTitle() {
    return getNotes();
  }

  String getType() {
    return type;
  }

  void makeTarget(final DndTarget widget, final String overStyle, final VehicleType vehicleType) {
    DndHelper.makeTarget(widget, acceptsDropTypes, overStyle,
        new Predicate<Object>() {
          @Override
          public boolean apply(Object input) {
            return Vehicle.this.isTarget(vehicleType, input);
          }
        }, new BiConsumer<DropEvent, Object>() {
          @Override
          public void accept(DropEvent t, Object u) {
            widget.asWidget().removeStyleName(overStyle);
            Vehicle.this.acceptDrop(vehicleType, u);
          }
        });
  }

  private void acceptDrop(VehicleType vehicleType, Object data) {
    if (DndHelper.isDataType(DATA_TYPE_TRIP)) {
      ((Trip) data).maybeUpdateVehicle(vehicleType, this);

    } else if (DndHelper.isDataType(DATA_TYPE_FREIGHT)) {
      final Freight freight = (Freight) data;
      String title = freight.getCargoAndTripTitle();

      Trip.createForCargo(this, freight, title, false, new IdCallback() {
        @Override
        public void onSuccess(Long result) {
          freight.updateTrip(result, true);
        }
      });

    } else if (DndHelper.isDataType(DATA_TYPE_ORDER_CARGO)) {
      final OrderCargo orderCargo = (OrderCargo) data;
      String title = orderCargo.getTitle();

      Trip.createForCargo(this, orderCargo, title, false, new IdCallback() {
        @Override
        public void onSuccess(Long result) {
          orderCargo.assignToTrip(result, true);
        }
      });
    }
  }

  private boolean isTarget(VehicleType vehicleType, Object data) {
    if (DndHelper.isDataType(DATA_TYPE_TRIP) && data instanceof Trip) {
      return !Objects.equals(getId(), ((Trip) data).getVehicleId(vehicleType));

    } else if (DndHelper.isDataType(DATA_TYPE_FREIGHT) && data instanceof Freight) {
      return vehicleType == VehicleType.TRUCK
          && !Objects.equals(getId(), ((Freight) data).getVehicleId(vehicleType));

    } else if (DndHelper.isDataType(DATA_TYPE_ORDER_CARGO) && data instanceof OrderCargo) {
      return vehicleType == VehicleType.TRUCK;

    } else {
      return false;
    }
  }
}
