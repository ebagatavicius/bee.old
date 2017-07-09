package com.butent.bee.client.modules.transport.charts;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_VEHICLE_BRAND_NAME;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.DndTarget;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class Vehicle extends Filterable implements HasDateRange, HasItemName {

  private static final int numberIndex = Data.getColumnIndex(VIEW_VEHICLES, COL_NUMBER);

  private static final int brandNameIndex = Data.getColumnIndex(VIEW_VEHICLES,
      COL_VEHICLE_BRAND_NAME);
  private static final int modelNameIndex = Data.getColumnIndex(VIEW_VEHICLES, COL_MODEL_NAME);
  private static final String modelLabel = Data.getColumnLabel(VIEW_VEHICLES, COL_MODEL);
  private static final int modelIdIndex = Data.getColumnIndex(VIEW_VEHICLES, COL_MODEL);

  private static final int typeNameIndex = Data.getColumnIndex(VIEW_VEHICLES, COL_TYPE_NAME);
  private static final int typeIdIndex = Data.getColumnIndex(VIEW_VEHICLES,
      COL_VEHICLE_SERVICE_TYPE);

  private static final String managerLabel = Data.getColumnLabel(VIEW_TRANSPORT_GROUPS,
      COL_GROUP_MANAGER);

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
  private final Long modelId;
  private final String type;
  private final Long typeId;

  private final Set<Long> groups;

  private final Range<JustDate> range;

  private final String itemName;

  Vehicle(BeeRow row) {
    this.row = row;

    this.id = row.getId();
    this.number = row.getString(numberIndex);
    this.model = BeeUtils.joinWords(row.getString(brandNameIndex),
        row.getString(modelNameIndex));
    this.modelId = row.getLong(modelIdIndex);

    this.type = BeeUtils.trim(row.getString(typeNameIndex));
    this.typeId = row.getLong(typeIdIndex);

    this.groups = DataUtils.parseIdSet(row.getProperty(PROP_VEHICLE_GROUPS));

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

  Set<Long> getGroups() {
    return groups;
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

  Long getManager() {
    return BeeUtils.toLongOrNull(row.getProperty(PROP_VEHICLE_MANAGER));
  }

  String getManagerName() {
    Long manager = getManager();
    if (manager == null) {
      return null;
    } else {
      return Global.getUsers().getSignature(manager);
    }
  }

  String getMessage(String caption) {
    return TimeBoardHelper.buildTitle(caption, getNumber(),
        modelLabel, getModel(),
        managerLabel, getManagerName(),
        notesLabel, getNotes());
  }

  String getModel() {
    return model;
  }

  Long getModelId() {
    return modelId;
  }

  String getNotes() {
    return BeeUtils.trim(row.getString(notesIndex));
  }

  String getNumber() {
    return number;
  }

  String getTitle() {
    return TimeBoardHelper.buildTitle(managerLabel, getManagerName(), notesLabel, getNotes());
  }

  String getType() {
    return type;
  }

  Long getTypeId() {
    return typeId;
  }

  void makeTarget(final DndTarget widget, final String overStyle, final VehicleType vehicleType) {
    DndHelper.makeTarget(widget, acceptsDropTypes, overStyle,
        input -> isTarget(vehicleType, input), (t, u) -> {
          widget.asWidget().removeStyleName(overStyle);
          acceptDrop(vehicleType, u);
        });
  }

  private void acceptDrop(VehicleType vehicleType, Object data) {
    if (DndHelper.isDataType(DATA_TYPE_TRIP)) {
      ((Trip) data).maybeUpdateVehicle(vehicleType, this);

    } else if (DndHelper.isDataType(DATA_TYPE_FREIGHT)) {
      final Freight freight = (Freight) data;
      String title = freight.getCargoAndTripTitle();

      Trip.createForCargo(this, freight, title, new RowCallback() {
        @Override
        public void onSuccess(final BeeRow tripRow) {
          freight.updateTrip(tripRow.getId(), new RowCallback() {

            @Override
            public void onSuccess(BeeRow ct) {
              afterAssignToNewTrip(freight, tripRow);
            }
          });
        }
      });

    } else if (DndHelper.isDataType(DATA_TYPE_ORDER_CARGO)) {
      final OrderCargo orderCargo = (OrderCargo) data;
      String title = orderCargo.getTitle();

      Trip.createForCargo(this, orderCargo, title, new RowCallback() {
        @Override
        public void onSuccess(final BeeRow tripRow) {
          orderCargo.assignToTrip(tripRow.getId(), new RowCallback() {

            @Override
            public void onSuccess(BeeRow ct) {
              afterAssignToNewTrip(orderCargo, tripRow);
            }
          });
        }
      });
    }
  }

  private void afterAssignToNewTrip(OrderCargo orderCargo, final BeeRow tripRow) {
    orderCargo.maybeUpdateManager(getManager(), um -> {
      Set<String> viewNames = new HashSet<>();
      viewNames.add(VIEW_TRIPS);
      viewNames.add(VIEW_CARGO_TRIPS);

      if (BeeUtils.isTrue(um)) {
        viewNames.add(VIEW_ORDERS);
      }

      DataChangeEvent.fireRefresh(BeeKeeper.getBus(), viewNames);
      RowEditor.open(VIEW_TRIPS, tripRow, Opener.MODAL);
    });
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
