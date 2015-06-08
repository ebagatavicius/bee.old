package com.butent.bee.client.modules.transport.charts;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.DropEvent;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.DndTarget;
import com.butent.bee.client.timeboard.Blender;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.modules.transport.TransportConstants.OrderStatus;
import com.butent.bee.shared.modules.transport.TransportConstants.VehicleType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

final class Freight extends OrderCargo {

  private static final Set<String> acceptsDropTypes =
      ImmutableSet.of(DATA_TYPE_FREIGHT, DATA_TYPE_ORDER_CARGO);

  private static final Blender blender = new Blender() {
    @Override
    public boolean willItBlend(HasDateRange x, HasDateRange y) {
      if (x instanceof Freight && y instanceof Freight) {
        return Objects.equals(((Freight) x).getTripId(), ((Freight) y).getTripId());
      } else {
        return false;
      }
    }
  };

  static Freight create(SimpleRow row, JustDate minLoad, JustDate maxUnload) {
    return new Freight(row.getLong(COL_ORDER),
        EnumUtils.getEnumByIndex(OrderStatus.class, row.getInt(COL_STATUS)),
        row.getDateTime(ALS_ORDER_DATE), row.getValue(COL_ORDER_NO),
        row.getLong(COL_CUSTOMER), row.getValue(COL_CUSTOMER_NAME),
        row.getLong(COL_CARGO), row.getLong(COL_CARGO_TYPE),
        row.getValue(COL_CARGO_DESCRIPTION), row.getValue(COL_CARGO_NOTES),
        BeeUtils.nvl(Places.getLoadingDate(row, loadingColumnAlias(COL_PLACE_DATE)),
            Places.getLoadingDate(row, defaultLoadingColumnAlias(COL_PLACE_DATE)), minLoad),
        BeeUtils.nvl(row.getLong(loadingColumnAlias(COL_PLACE_COUNTRY)),
            row.getLong(defaultLoadingColumnAlias(COL_PLACE_COUNTRY))),
        BeeUtils.nvl(row.getValue(loadingColumnAlias(COL_PLACE_ADDRESS)),
            row.getValue(defaultLoadingColumnAlias(COL_PLACE_ADDRESS))),
        BeeUtils.nvl(row.getValue(ALS_LOADING_POST_INDEX),
            row.getValue(defaultLoadingColumnAlias(COL_PLACE_POST_INDEX))),
        BeeUtils.nvl(row.getLong(loadingColumnAlias(COL_PLACE_CITY)),
            row.getLong(defaultLoadingColumnAlias(COL_PLACE_CITY))),
        BeeUtils.nvl(row.getValue(loadingColumnAlias(COL_PLACE_NUMBER)),
            row.getValue(defaultLoadingColumnAlias(COL_PLACE_NUMBER))),
        BeeUtils.nvl(Places.getUnloadingDate(row, unloadingColumnAlias(COL_PLACE_DATE)),
            Places.getUnloadingDate(row, defaultUnloadingColumnAlias(COL_PLACE_DATE)), maxUnload),
        BeeUtils.nvl(row.getLong(unloadingColumnAlias(COL_PLACE_COUNTRY)),
            row.getLong(defaultUnloadingColumnAlias(COL_PLACE_COUNTRY))),
        BeeUtils.nvl(row.getValue(unloadingColumnAlias(COL_PLACE_ADDRESS)),
            row.getValue(defaultUnloadingColumnAlias(COL_PLACE_ADDRESS))),
        BeeUtils.nvl(row.getValue(ALS_UNLOADING_POST_INDEX),
            row.getValue(defaultUnloadingColumnAlias(COL_PLACE_POST_INDEX))),
        BeeUtils.nvl(row.getLong(unloadingColumnAlias(COL_PLACE_CITY)),
            row.getLong(defaultUnloadingColumnAlias(COL_PLACE_CITY))),
        BeeUtils.nvl(row.getValue(unloadingColumnAlias(COL_PLACE_NUMBER)),
            row.getValue(defaultUnloadingColumnAlias(COL_PLACE_NUMBER))),
        row.getLong(COL_TRIP_ID), row.getLong(COL_VEHICLE), row.getLong(COL_TRAILER),
        row.getLong(COL_CARGO_TRIP_ID), row.getLong(ALS_CARGO_TRIP_VERSION));
  }

  static Blender getBlender() {
    return blender;
  }

  private final Long tripId;
  private final Long truckId;

  private final Long trailerId;
  private final Long cargoTripId;

  private final Long cargoTripVersion;

  private String tripTitle;

  private boolean editable;

  private Freight(Long orderId, OrderStatus orderStatus, DateTime orderDate, String orderNo,
      Long customerId, String customerName,
      Long cargoId, Long cargoType, String cargoDescription, String notes,
      JustDate loadingDate, Long loadingCountry, String loadingPlace, String loadingPostIndex,
      Long loadingCity, String loadingNumber,
      JustDate unloadingDate, Long unloadingCountry, String unloadingPlace,
      String unloadingPostIndex, Long unloadingCity,
      String unloadingNumber, Long tripId, Long truckId, Long trailerId, Long cargoTripId,
      Long cargoTripVersion) {

    super(orderId, orderStatus, orderDate, orderNo, customerId, customerName,
        cargoId, cargoType, cargoDescription, notes,
        loadingDate, loadingCountry, loadingPlace, loadingPostIndex, loadingCity, loadingNumber,
        unloadingDate, unloadingCountry, unloadingPlace, unloadingPostIndex, unloadingCity,
        unloadingNumber);

    this.tripId = tripId;
    this.truckId = truckId;
    this.trailerId = trailerId;

    this.cargoTripId = cargoTripId;
    this.cargoTripVersion = cargoTripVersion;
  }

  @Override
  public Long getColorSource() {
    return tripId;
  }

  String getCargoAndTripTitle() {
    if (!BeeUtils.isEmpty(getTripTitle())) {
      return BeeUtils.buildLines(getTitle(), BeeConst.STRING_NBSP, getTripTitle());
    } else {
      return getTitle();
    }
  }

  Long getCargoTripId() {
    return cargoTripId;
  }

  Long getCargoTripVersion() {
    return cargoTripVersion;
  }

  Long getTrailerId() {
    return trailerId;
  }

  Long getTripId() {
    return tripId;
  }

  String getTripTitle() {
    return tripTitle;
  }

  Long getTruckId() {
    return truckId;
  }

  Long getVehicleId(VehicleType vehicleType) {
    switch (vehicleType) {
      case TRUCK:
        return getTruckId();
      case TRAILER:
        return getTrailerId();
      default:
        return null;
    }
  }

  boolean isEditable() {
    return editable;
  }

  void makeTarget(final DndTarget widget, final String overStyle) {
    DndHelper.makeTarget(widget, acceptsDropTypes, overStyle,
        new Predicate<Object>() {
          @Override
          public boolean apply(Object input) {
            return Freight.this.isTarget(input);
          }
        }, new BiConsumer<DropEvent, Object>() {
          @Override
          public void accept(DropEvent t, Object u) {
            widget.asWidget().removeStyleName(overStyle);
            Freight.this.acceptDrop(u);
          }
        });
  }

  void setEditable(boolean editable) {
    this.editable = editable;
  }

  void setTripTitle(String tripTitle) {
    this.tripTitle = tripTitle;
  }

  void updateTrip(Long newTripId, boolean fire) {
    if (!DataUtils.isId(newTripId) || Objects.equals(getTripId(), newTripId)) {
      return;
    }

    String viewName = VIEW_CARGO_TRIPS;
    List<BeeColumn> columns = Data.getColumns(viewName, Lists.newArrayList(COL_TRIP));

    RowCallback callback = fire ? new RowUpdateCallback(viewName) : null;

    Queries.update(viewName, getCargoTripId(), getCargoTripVersion(), columns,
        Queries.asList(getTripId()), Queries.asList(newTripId), null, callback);
  }

  private void acceptDrop(Object data) {
    if (DndHelper.isDataType(DATA_TYPE_FREIGHT)) {
      final Freight freight = (Freight) data;
      String title = freight.getTitle();

      Trip.maybeAssignCargo(title, getTripTitle(), new ConfirmationCallback() {
        @Override
        public void onConfirm() {
          freight.updateTrip(Freight.this.getTripId(), true);
        }
      });

    } else if (DndHelper.isDataType(DATA_TYPE_ORDER_CARGO)) {
      final OrderCargo orderCargo = (OrderCargo) data;
      String title = orderCargo.getTitle();

      Trip.maybeAssignCargo(title, getTripTitle(), new ConfirmationCallback() {
        @Override
        public void onConfirm() {
          orderCargo.assignToTrip(Freight.this.getTripId(), true);
        }
      });
    }
  }

  private boolean isTarget(Object data) {
    if (DndHelper.isDataType(DATA_TYPE_FREIGHT) && data instanceof Freight) {
      return !Objects.equals(getTripId(), ((Freight) data).getTripId());
    } else {
      return DndHelper.isDataType(DATA_TYPE_ORDER_CARGO) && data instanceof OrderCargo;
    }
  }
}
