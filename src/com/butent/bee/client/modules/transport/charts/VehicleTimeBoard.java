package com.butent.bee.client.modules.transport.charts;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.Rectangle;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.logical.MoveEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.transport.charts.Filterable.FilterType;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.client.timeboard.TimeBoardRowLayout;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.DndDiv;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class VehicleTimeBoard extends ChartBase {

  private static final String STYLE_PREFIX = "bee-tr-vtb-";

  private static final String STYLE_VEHICLE_PREFIX = STYLE_PREFIX + "Vehicle-";
  private static final String STYLE_VEHICLE_ROW_SEPARATOR = STYLE_VEHICLE_PREFIX + "row-sep";

  private static final String STYLE_NUMBER_PREFIX = STYLE_PREFIX + "Number-";
  private static final String STYLE_NUMBER_PANEL = STYLE_NUMBER_PREFIX + "panel";
  private static final String STYLE_NUMBER_LABEL = STYLE_NUMBER_PREFIX + "label";
  private static final String STYLE_NUMBER_OVERLAP = STYLE_NUMBER_PREFIX + "overlap";

  private static final String STYLE_INFO_PREFIX = STYLE_PREFIX + "Info-";
  private static final String STYLE_INFO_PANEL = STYLE_INFO_PREFIX + "panel";
  private static final String STYLE_INFO_LABEL = STYLE_INFO_PREFIX + "label";
  private static final String STYLE_INFO_OVERLAP = STYLE_INFO_PREFIX + "overlap";

  private static final String STYLE_VEHICLE_DRAG = STYLE_VEHICLE_PREFIX + "drag";
  private static final String STYLE_VEHICLE_DRAG_OVER = STYLE_VEHICLE_PREFIX + "dragOver";

  private static final String STYLE_TRIP_PREFIX = STYLE_PREFIX + "Trip-";
  private static final String STYLE_TRIP_PANEL = STYLE_TRIP_PREFIX + "panel";
  private static final String STYLE_TRIP_VOID = STYLE_TRIP_PREFIX + "void";

  private static final String STYLE_TRIP_DRAG = STYLE_TRIP_PREFIX + "drag";
  private static final String STYLE_TRIP_DRAG_OVER = STYLE_TRIP_PREFIX + "dragOver";

  private static final String STYLE_FREIGHT_PREFIX = STYLE_PREFIX + "Freight-";
  private static final String STYLE_FREIGHT_PANEL = STYLE_FREIGHT_PREFIX + "panel";

  private static final String STYLE_FREIGHT_DRAG = STYLE_FREIGHT_PREFIX + "drag";
  private static final String STYLE_FREIGHT_DRAG_OVER = STYLE_FREIGHT_PREFIX + "dragOver";

  private static final String STYLE_SERVICE_PREFIX = STYLE_PREFIX + "Service-";
  private static final String STYLE_SERVICE_PANEL = STYLE_SERVICE_PREFIX + "panel";
  private static final String STYLE_SERVICE_LABEL = STYLE_SERVICE_PREFIX + "label";

  private static final String STYLE_INACTIVE = STYLE_PREFIX + "Inactive";
  private static final String STYLE_OVERLAP = STYLE_PREFIX + "Overlap";

  private final List<Vehicle> vehicles = Lists.newArrayList();

  private final Multimap<Long, Trip> trips = ArrayListMultimap.create();

  private final Multimap<Long, Freight> freights = ArrayListMultimap.create();

  private final Multimap<Long, VehicleService> services = ArrayListMultimap.create();

  private int numberWidth = BeeConst.UNDEF;
  private int infoWidth = BeeConst.UNDEF;

  private boolean separateCargo;

  private final Set<String> numberPanels = Sets.newHashSet();
  private final Set<String> infoPanels = Sets.newHashSet();

  private final List<Integer> vehicleIndexesByRow = Lists.newArrayList();

  private final VehicleType vehicleType;
  private final VehicleType otherVehicleType;

  protected VehicleTimeBoard() {
    super();

    addStyleName(STYLE_PREFIX + "View");

    addRelevantDataViews(VIEW_VEHICLES, VIEW_TRIPS, VIEW_TRIP_DRIVERS, VIEW_VEHICLE_SERVICES);

    if (getDataType().equals(DATA_TYPE_TRAILER)) {
      this.vehicleType = VehicleType.TRAILER;
      this.otherVehicleType = VehicleType.TRUCK;
    } else {
      this.vehicleType = VehicleType.TRUCK;
      this.otherVehicleType = VehicleType.TRAILER;
    }
  }

  @Override
  public void handleAction(Action action) {
    if (Action.ADD.equals(action)) {
      RowFactory.createRow(VIEW_VEHICLES);
    } else {
      super.handleAction(action);
    }
  }

  protected void addInfoWidget(HasWidgets panel, IdentifiableWidget widget,
      int firstRow, int lastRow) {
    Rectangle rectangle = TimeBoardHelper.getRectangle(getNumberWidth(), getInfoWidth(),
        firstRow, lastRow, getRowHeight());

    Edges margins = new Edges();
    margins.setRight(TimeBoardHelper.DEFAULT_MOVER_WIDTH);
    margins.setBottom(TimeBoardHelper.ROW_SEPARATOR_HEIGHT);

    TimeBoardHelper.apply(widget.asWidget(), rectangle, margins);

    panel.add(widget.asWidget());

    infoPanels.add(widget.getId());
  }

  protected BeeRow createNewTripRow(DataInfo dataInfo, int rowIndex, JustDate date) {
    BeeRow newRow = RowFactory.createEmptyRow(dataInfo, true);

    if (TimeUtils.isMore(date, TimeUtils.today())) {
      newRow.setValue(dataInfo.getColumnIndex(COL_TRIP_DATE), date.getDateTime());
    }

    Integer vehicleIndex = BeeUtils.getQuietly(vehicleIndexesByRow, rowIndex);
    Vehicle vehicle = (vehicleIndex == null) ? null : BeeUtils.getQuietly(vehicles, vehicleIndex);

    if (vehicle != null) {
      newRow.setValue(dataInfo.getColumnIndex(vehicleType.getTripVehicleIdColumnName()),
          vehicle.getId());
      newRow.setValue(dataInfo.getColumnIndex(vehicleType.getTripVehicleNumberColumnName()),
          vehicle.getNumber());
    }

    return newRow;
  }

  @Override
  protected boolean filter(FilterType filterType) {
    boolean filtered = false;

    List<ChartData> selectedData = FilterHelper.getSelectedData(getFilterData());
    if (selectedData.isEmpty()) {
      resetFilter(filterType);
      return filtered;
    }

    ChartData vehicleData = FilterHelper.getDataByType(selectedData,
        isTrailerPark() ? ChartData.Type.TRAILER : ChartData.Type.TRUCK);
    ChartData otherVehicleData = FilterHelper.getDataByType(selectedData,
        isTrailerPark() ? ChartData.Type.TRUCK : ChartData.Type.TRAILER);

    ChartData typeData = FilterHelper.getDataByType(selectedData, ChartData.Type.VEHICLE_TYPE);
    ChartData modelData = FilterHelper.getDataByType(selectedData, ChartData.Type.VEHICLE_MODEL);

    CargoMatcher cargoMatcher = CargoMatcher.maybeCreate(selectedData);

    ChartData tripData = FilterHelper.getDataByType(selectedData, ChartData.Type.TRIP);
    ChartData driverData = FilterHelper.getDataByType(selectedData, ChartData.Type.DRIVER);

    PlaceMatcher placeMatcher = PlaceMatcher.maybeCreate(selectedData);

    boolean freightRequired = cargoMatcher != null || placeMatcher != null;
    boolean tripRequired = freightRequired || otherVehicleData != null || tripData != null
        || driverData != null;

    for (Vehicle vehicle : vehicles) {
      boolean vehicleMatch = FilterHelper.matches(vehicleData, vehicle.getId())
          && FilterHelper.matches(typeData, vehicle.getType())
          && FilterHelper.matches(modelData, vehicle.getModel());

      boolean hasTrips = vehicleMatch && trips.containsKey(vehicle.getId());
      if (vehicleMatch && !hasTrips && tripRequired) {
        vehicleMatch = false;
      }

      if (vehicleMatch && hasTrips) {
        int tripCount = 0;

        for (Trip trip : trips.get(vehicle.getId())) {
          boolean tripMatch = FilterHelper.matches(tripData, trip.getTripId())
              && FilterHelper.matches(otherVehicleData, trip.getVehicleId(otherVehicleType))
              && trip.matchesDrivers(driverData);

          boolean hasFreights = tripMatch && freights.containsKey(trip.getTripId());
          if (tripMatch && !hasFreights && freightRequired) {
            tripMatch = false;
          }

          if (tripMatch && hasFreights) {
            int freightCount = 0;

            for (Freight freight : freights.get(trip.getTripId())) {
              boolean freightMatch = (cargoMatcher == null) ? true : cargoMatcher.matches(freight);

              if (freightMatch && placeMatcher != null) {
                boolean ok = placeMatcher.matches(freight);
                if (!ok && hasCargoHandling(freight.getCargoId())) {
                  ok = placeMatcher.matchesAnyOf(getCargoHandling(freight.getCargoId()));
                }

                if (!ok) {
                  freightMatch = false;
                }
              }

              freight.setMatch(filterType, freightMatch);
              if (freightMatch) {
                freightCount++;
              } else {
                filtered = true;
              }
            }

            if (freightCount <= 0) {
              tripMatch = false;
            }
          }

          trip.setMatch(filterType, tripMatch);
          if (tripMatch) {
            tripCount++;
          } else {
            filtered = true;
          }
        }

        if (tripCount <= 0) {
          vehicleMatch = false;
        }
      }

      vehicle.setMatch(filterType, vehicleMatch);
      if (!vehicleMatch) {
        filtered = true;
      }
    }

    return filtered;
  }

  protected Trip findTripById(Long tripId) {
    if (DataUtils.isId(tripId)) {
      for (Trip trip : trips.values()) {
        if (Objects.equal(trip.getTripId(), tripId)) {
          return trip;
        }
      }
    }
    return null;
  }

  @Override
  protected Collection<? extends HasDateRange> getChartItems() {
    if (isFiltered()) {
      List<HasDateRange> result = Lists.newArrayList();

      for (Vehicle vehicle : vehicles) {
        if (isItemVisible(vehicle) && trips.containsKey(vehicle.getId())) {

          if (separateCargo()) {
            for (Trip trip : trips.get(vehicle.getId())) {
              if (isItemVisible(trip) && freights.containsKey(trip.getTripId())) {
                result.addAll(FilterHelper.getPersistentItems(freights.get(trip.getTripId())));
              }
            }

          } else {
            result.addAll(FilterHelper.getPersistentItems(trips.get(vehicle.getId())));
          }
        }
      }

      return result;

    } else if (separateCargo()) {
      return freights.values();

    } else {
      return trips.values();
    }
  }

  protected abstract String getDataType();

  protected abstract String getDayWidthColumnName();

  protected Long getGroupIdForFreightLayout(Trip trip) {
    return trip.getVehicleId(vehicleType);
  }

  protected Long getGroupIdForTripLayout(Trip trip) {
    return trip.getVehicleId(vehicleType);
  }

  protected int getInfoWidth() {
    return infoWidth;
  }

  protected abstract String getInfoWidthColumnName();

  protected abstract String getItemOpacityColumnName();

  protected int getNumberWidth() {
    return numberWidth;
  }

  protected abstract String getNumberWidthColumnName();

  protected abstract String getSeparateCargoColumnName();

  @Override
  protected void initData(Map<String, String> properties) {
    vehicles.clear();
    trips.clear();
    freights.clear();
    services.clear();

    if (BeeUtils.isEmpty(properties)) {
      return;
    }

    BeeRowSet brs = BeeRowSet.getIfPresent(properties, PROP_VEHICLES);
    if (!DataUtils.isEmpty(brs)) {
      for (BeeRow row : brs.getRows()) {
        vehicles.add(new Vehicle(row));
      }
    }

    Multimap<Long, Driver> drivers = HashMultimap.create();

    SimpleRowSet srs = SimpleRowSet.getIfPresent(properties, PROP_TRIP_DRIVERS);
    if (!DataUtils.isEmpty(srs)) {
      for (SimpleRow row : srs) {
        drivers.put(row.getLong(COL_TRIP), new Driver(row.getLong(COL_DRIVER),
            row.getValue(ClassifierConstants.COL_FIRST_NAME),
            row.getValue(ClassifierConstants.COL_LAST_NAME),
            row.getDateTime(COL_TRIP_DRIVER_FROM), row.getDateTime(COL_TRIP_DRIVER_TO),
            row.getValue(COL_TRIP_DRIVER_NOTE)));
      }
    }

    srs = SimpleRowSet.getIfPresent(properties, PROP_FREIGHTS);
    if (!DataUtils.isEmpty(srs)) {
      for (SimpleRow row : srs) {
        Pair<JustDate, JustDate> handlingSpan = getCargoHandlingSpan(row.getLong(COL_CARGO));
        freights.put(row.getLong(COL_TRIP_ID),
            Freight.create(row, handlingSpan.getA(), handlingSpan.getB()));
      }
    }

    srs = SimpleRowSet.getIfPresent(properties, PROP_TRIPS);
    if (!DataUtils.isEmpty(srs)) {
      int index = srs.getColumnIndex(vehicleType.getTripVehicleIdColumnName());

      for (SimpleRow row : srs) {
        Long tripId = row.getLong(COL_TRIP_ID);

        Collection<Driver> tripDrivers = BeeUtils.getIfContains(drivers, tripId);
        int cargoCount = 0;

        if (freights.containsKey(tripId)) {
          JustDate minDate = null;
          JustDate maxDate = null;

          for (Freight freight : freights.get(tripId)) {
            minDate = BeeUtils.min(minDate, freight.getMinDate());
            maxDate = BeeUtils.max(maxDate, freight.getMaxDate());
            cargoCount++;
          }

          Trip trip = new Trip(row, minDate, maxDate, tripDrivers, cargoCount);
          trips.put(row.getLong(index), trip);

          for (Freight freight : freights.get(tripId)) {
            freight.adjustRange(trip.getRange());

            freight.setTripTitle(trip.getTitle());
            freight.setEditable(trip.isEditable());
          }

        } else {
          trips.put(row.getLong(index), new Trip(row, tripDrivers, cargoCount));
        }
      }
    }

    srs = SimpleRowSet.getIfPresent(properties, PROP_VEHICLE_SERVICES);
    if (!DataUtils.isEmpty(srs)) {
      for (SimpleRow row : srs) {
        VehicleService service = new VehicleService(row);
        services.put(service.getVehicleId(), service);
      }
    }

    setSeparateCargo(TimeBoardHelper.getBoolean(getSettings(), getSeparateCargoColumnName()));
  }

  protected boolean isInfoColumnVisible() {
    return true;
  }

  protected boolean layoutIdleVehicles() {
    return true;
  }

  @Override
  protected void onDoubleClickChart(int row, JustDate date) {
    if (BeeUtils.isIndex(vehicleIndexesByRow, row) && TimeUtils.isMeq(date, TimeUtils.today())) {
      DataInfo dataInfo = Data.getDataInfo(VIEW_TRIPS);
      BeeRow newRow = createNewTripRow(dataInfo, row, date);

      RowFactory.createRow(dataInfo, newRow);
    }
  }

  @Override
  protected boolean persistFilter() {
    return FilterHelper.persistFilter(vehicles)
        | FilterHelper.persistFilter(trips.values())
        | FilterHelper.persistFilter(freights.values());
  }

  @Override
  protected void prepareChart(Size canvasSize) {
    setNumberWidth(TimeBoardHelper.getPixels(getSettings(), getNumberWidthColumnName(), 80,
        TimeBoardHelper.DEFAULT_MOVER_WIDTH + 1, canvasSize.getWidth() / 3));

    if (isInfoColumnVisible()) {
      setInfoWidth(TimeBoardHelper.getPixels(getSettings(), getInfoWidthColumnName(), 120,
          TimeBoardHelper.DEFAULT_MOVER_WIDTH + 1, canvasSize.getWidth() / 3));
    } else {
      setInfoWidth(0);
    }

    setChartLeft(getNumberWidth() + getInfoWidth());
    setChartWidth(canvasSize.getWidth() - getChartLeft() - getChartRight());

    setDayColumnWidth(TimeBoardHelper.getPixels(getSettings(), getDayWidthColumnName(), 20,
        1, getChartWidth()));

    boolean sc = TimeBoardHelper.getBoolean(getSettings(), getSeparateCargoColumnName());
    if (separateCargo() != sc) {
      setSeparateCargo(sc);
      updateMaxRange();
    }
  }

  @Override
  protected List<ChartData> prepareFilterData(FilterType filterType) {
    List<ChartData> data = Lists.newArrayList();
    if (vehicles.isEmpty()) {
      return data;
    }

    ChartData truckData = new ChartData(ChartData.Type.TRUCK);
    ChartData trailerData = new ChartData(ChartData.Type.TRAILER);

    ChartData modelData = new ChartData(ChartData.Type.VEHICLE_MODEL);
    ChartData typeData = new ChartData(ChartData.Type.VEHICLE_TYPE);

    ChartData customerData = new ChartData(ChartData.Type.CUSTOMER);
    ChartData orderData = new ChartData(ChartData.Type.ORDER);
    ChartData statusData = new ChartData(ChartData.Type.ORDER_STATUS);

    ChartData cargoData = new ChartData(ChartData.Type.CARGO);

    ChartData tripData = new ChartData(ChartData.Type.TRIP);

    ChartData loadData = new ChartData(ChartData.Type.LOADING);
    ChartData unloadData = new ChartData(ChartData.Type.UNLOADING);
    ChartData placeData = new ChartData(ChartData.Type.PLACE);

    ChartData driverData = new ChartData(ChartData.Type.DRIVER);

    for (Vehicle vehicle : vehicles) {
      if (!vehicle.matched(filterType)) {
        continue;
      }

      String vehicleName = vehicle.getItemName();
      if (isTrailerPark()) {
        trailerData.add(vehicleName, vehicle.getId());
      } else {
        truckData.add(vehicleName, vehicle.getId());
      }

      modelData.add(vehicle.getModel());
      typeData.add(vehicle.getType());

      if (!trips.containsKey(vehicle.getId())) {
        continue;
      }

      for (Trip trip : trips.get(vehicle.getId())) {
        if (!trip.matched(filterType)) {
          continue;
        }

        tripData.add(trip.getItemName(), trip.getTripId());

        String otherVehicleNumber = trip.getVehicleNumber(otherVehicleType);
        if (!BeeUtils.isEmpty(otherVehicleNumber)) {
          if (isTrailerPark()) {
            truckData.add(otherVehicleNumber, trip.getVehicleId(otherVehicleType));
          } else {
            trailerData.add(otherVehicleNumber, trip.getVehicleId(otherVehicleType));
          }
        }

        if (trip.hasDrivers()) {
          for (Driver driver : trip.getDrivers()) {
            driverData.add(driver.getItemName());
          }
        }

        if (!freights.containsKey(trip.getTripId())) {
          continue;
        }

        for (Freight freight : freights.get(trip.getTripId())) {
          if (!freight.matched(filterType)) {
            continue;
          }

          customerData.add(freight.getCustomerName(), freight.getCustomerId());
          orderData.add(freight.getOrderName(), freight.getOrderId());
          statusData.addNotNull(freight.getOrderStatus());

          cargoData.add(freight.getCargoDescription(), freight.getCargoId());

          String loading = Places.getLoadingPlaceInfo(freight);
          if (!BeeUtils.isEmpty(loading)) {
            loadData.add(loading);
            placeData.add(loading);
          }

          String unloading = Places.getUnloadingPlaceInfo(freight);
          if (!BeeUtils.isEmpty(unloading)) {
            unloadData.add(unloading);
            placeData.add(unloading);
          }

          if (hasCargoHandling(freight.getCargoId())) {
            for (CargoHandling ch : getCargoHandling(freight.getCargoId())) {
              loading = Places.getLoadingPlaceInfo(ch);
              if (!BeeUtils.isEmpty(loading)) {
                loadData.add(loading);
                placeData.add(loading);
              }

              unloading = Places.getUnloadingPlaceInfo(ch);
              if (!BeeUtils.isEmpty(unloading)) {
                unloadData.add(unloading);
                placeData.add(unloading);
              }
            }
          }
        }
      }
    }

    data.add(isTrailerPark() ? trailerData : truckData);
    data.add(modelData);
    data.add(typeData);

    data.add(customerData);
    data.add(orderData);
    data.add(statusData);

    data.add(cargoData);

    data.add(tripData);

    data.add(loadData);
    data.add(unloadData);
    data.add(placeData);

    data.add(driverData);

    data.add(isTrailerPark() ? truckData : trailerData);

    return data;
  }

  @Override
  protected void renderContent(ComplexPanel panel) {
    renderContentInit();

    List<TimeBoardRowLayout> vehicleLayout = doLayout();

    int rc = TimeBoardRowLayout.countRows(vehicleLayout, 1);
    initContent(panel, rc);

    if (vehicleLayout.isEmpty()) {
      return;
    }

    int calendarWidth = getCalendarWidth();

    Double opacity = TimeBoardHelper.getOpacity(getSettings(), getItemOpacityColumnName());

    Edges margins = new Edges();
    margins.setBottom(TimeBoardHelper.ROW_SEPARATOR_HEIGHT);

    Widget offWidget;
    Widget itemWidget;
    Widget overlapWidget;

    int rowIndex = 0;
    for (TimeBoardRowLayout layout : vehicleLayout) {

      int vehicleIndex = layout.getDataIndex();

      int size = layout.getSize(1);
      int lastRow = rowIndex + size - 1;

      int top = rowIndex * getRowHeight();

      if (rowIndex > 0) {
        TimeBoardHelper.addRowSeparator(panel, STYLE_VEHICLE_ROW_SEPARATOR, top, 0,
            getChartLeft() + calendarWidth);
      }

      Vehicle vehicle = vehicles.get(vehicleIndex);
      Assert.notNull(vehicle, "vehicle not found");

      boolean hasOverlap = layout.hasOverlap();

      IdentifiableWidget numberWidget = createNumberWidget(vehicle, hasOverlap);
      addNumberWidget(panel, numberWidget, rowIndex, lastRow);

      if (isInfoColumnVisible()) {
        renderInfoCell(layout, vehicle, panel, rowIndex, lastRow);
      }

      if (size > 1) {
        renderRowSeparators(panel, rowIndex, lastRow);
      }

      for (HasDateRange item : layout.getInactivity()) {
        if (item instanceof VehicleService) {
          offWidget = ((VehicleService) item).createWidget(this, STYLE_SERVICE_PANEL,
              STYLE_SERVICE_LABEL);
        } else {
          offWidget = new CustomDiv(STYLE_INACTIVE);
          UiHelper.maybeSetTitle(offWidget, vehicle.getInactivityTitle(item.getRange()));
        }

        Rectangle rectangle = getRectangle(item.getRange(), rowIndex, lastRow);
        TimeBoardHelper.apply(offWidget, rectangle, margins);

        panel.add(offWidget);
      }

      for (int i = 0; i < layout.getRows().size(); i++) {
        for (HasDateRange item : layout.getRows().get(i).getRowItems()) {

          if (item instanceof Trip) {
            itemWidget = createTripWidget((Trip) item);
          } else if (item instanceof Freight) {
            itemWidget = createFreightWidget((Freight) item);
          } else {
            itemWidget = null;
          }

          if (itemWidget != null) {
            Rectangle rectangle = getRectangle(item.getRange(), rowIndex + i);
            TimeBoardHelper.apply(itemWidget, rectangle, margins);

            styleItemWidget(item, itemWidget);
            if (opacity != null) {
              StyleUtils.setOpacity(itemWidget, opacity);
            }

            panel.add(itemWidget);
          }

          if (hasOverlap) {
            Set<Range<JustDate>> overlap = layout.getOverlap(item.getRange());

            for (Range<JustDate> over : overlap) {
              overlapWidget = new CustomDiv(STYLE_OVERLAP);

              Rectangle rectangle = getRectangle(over, rowIndex + i);
              TimeBoardHelper.apply(overlapWidget, rectangle, margins);

              panel.add(overlapWidget);
            }
          }
        }
      }

      for (int i = 0; i < size; i++) {
        vehicleIndexesByRow.add(vehicleIndex);
      }

      rowIndex += size;
    }
  }

  protected void renderContentInit() {
    numberPanels.clear();
    infoPanels.clear();

    vehicleIndexesByRow.clear();
  }

  protected void renderInfoCell(TimeBoardRowLayout layout, Vehicle vehicle, ComplexPanel panel,
      int firstRow, int lastRow) {
    IdentifiableWidget infoWidget = createInfoWidget(vehicle, layout.hasOverlap());
    addInfoWidget(panel, infoWidget, firstRow, lastRow);
  }

  @Override
  protected void renderMovers(ComplexPanel panel, int height) {
    Mover numberMover = TimeBoardHelper.createHorizontalMover();
    StyleUtils.setLeft(numberMover, getNumberWidth() - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
    StyleUtils.setHeight(numberMover, height);

    numberMover.addMoveHandler(new MoveEvent.Handler() {
      @Override
      public void onMove(MoveEvent event) {
        onNumberResize(event);
      }
    });

    panel.add(numberMover);

    if (isInfoColumnVisible()) {
      Mover infoMover = TimeBoardHelper.createHorizontalMover();
      StyleUtils.setLeft(infoMover, getChartLeft() - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
      StyleUtils.setHeight(infoMover, height);

      infoMover.addMoveHandler(new MoveEvent.Handler() {
        @Override
        public void onMove(MoveEvent event) {
          onInfoResize(event);
        }
      });

      panel.add(infoMover);
    }
  }

  protected void renderRowSeparators(ComplexPanel panel, int firstRow, int lastRow) {
    for (int rowIndex = firstRow; rowIndex < lastRow; rowIndex++) {
      TimeBoardHelper.addRowSeparator(panel, (rowIndex + 1) * getRowHeight(), getChartLeft(),
          getCalendarWidth());
    }
  }

  @Override
  protected void resetFilter(FilterType filterType) {
    FilterHelper.resetFilter(vehicles, filterType);
    FilterHelper.resetFilter(trips.values(), filterType);
    FilterHelper.resetFilter(freights.values(), filterType);
  }

  private void addNumberWidget(HasWidgets panel, IdentifiableWidget widget,
      int firstRow, int lastRow) {

    Rectangle rectangle = TimeBoardHelper.getRectangle(0, getNumberWidth(), firstRow, lastRow,
        getRowHeight());

    Edges margins = new Edges();
    margins.setRight(TimeBoardHelper.DEFAULT_MOVER_WIDTH);
    margins.setBottom(TimeBoardHelper.ROW_SEPARATOR_HEIGHT);

    TimeBoardHelper.apply(widget.asWidget(), rectangle, margins);

    panel.add(widget.asWidget());

    numberPanels.add(widget.getId());
  }

  private Widget createFreightWidget(Freight freight) {
    Flow panel = new Flow();
    panel.addStyleName(STYLE_FREIGHT_PANEL);
    setItemWidgetColor(freight, panel);

    panel.setTitle(freight.getCargoAndTripTitle());

    bindOpener(panel, VIEW_ORDER_CARGO, freight.getCargoId());

    if (freight.isEditable()) {
      DndHelper.makeSource(panel, DATA_TYPE_FREIGHT, freight, STYLE_FREIGHT_DRAG);
      freight.makeTarget(panel, STYLE_FREIGHT_DRAG_OVER);
    }

    if (hasCargoHandling(freight.getCargoId())) {
      styleItemHasHandling(panel);
    }

    renderCargoShipment(panel, freight, freight.getTripTitle());

    return panel;
  }

  private IdentifiableWidget createInfoWidget(Vehicle vehicle, boolean hasOverlap) {
    Simple panel = new Simple();
    panel.addStyleName(STYLE_INFO_PANEL);
    if (hasOverlap) {
      panel.addStyleName(STYLE_INFO_OVERLAP);
    }

    Label label = new Label(vehicle.getInfo());
    label.addStyleName(STYLE_INFO_LABEL);

    UiHelper.maybeSetTitle(label, vehicle.getTitle());

    bindOpener(label, VIEW_VEHICLES, vehicle.getId());

    panel.add(label);

    return panel;
  }

  private IdentifiableWidget createNumberWidget(Vehicle vehicle, boolean hasOverlap) {
    Simple panel = new Simple();
    panel.addStyleName(STYLE_NUMBER_PANEL);
    if (hasOverlap) {
      panel.addStyleName(STYLE_NUMBER_OVERLAP);
    }

    DndDiv label = new DndDiv(STYLE_NUMBER_LABEL);
    label.setHtml(vehicle.getNumber());

    UiHelper.maybeSetTitle(label, vehicle.getTitle());

    bindOpener(label, VIEW_VEHICLES, vehicle.getId());

    panel.add(label);

    DndHelper.makeSource(label, getDataType(), vehicle, STYLE_VEHICLE_DRAG);
    vehicle.makeTarget(panel, STYLE_VEHICLE_DRAG_OVER, vehicleType);

    return panel;
  }

  private Widget createTripWidget(Trip trip) {
    Flow panel = new Flow();
    panel.addStyleName(STYLE_TRIP_PANEL);
    setItemWidgetColor(trip, panel);

    panel.setTitle(trip.getTitle());

    Long tripId = trip.getTripId();
    bindOpener(panel, VIEW_TRIPS, tripId);

    if (trip.isEditable()) {
      DndHelper.makeSource(panel, DATA_TYPE_TRIP, trip, STYLE_TRIP_DRAG);
      trip.makeTarget(panel, STYLE_TRIP_DRAG_OVER);
    }

    Range<JustDate> tripRange =
        TimeBoardHelper.normalizedIntersection(trip.getRange(), getVisibleRange());
    if (tripRange == null) {
      return panel;
    }

    renderTrip(panel, trip.getTitle(), BeeUtils.getIfContains(freights, tripId), tripRange,
        STYLE_TRIP_VOID);

    return panel;
  }

  private List<TimeBoardRowLayout> doLayout() {
    List<TimeBoardRowLayout> result = Lists.newArrayList();
    Range<JustDate> range = getVisibleRange();

    for (int vehicleIndex = 0; vehicleIndex < vehicles.size(); vehicleIndex++) {
      Vehicle vehicle = vehicles.get(vehicleIndex);

      if (isItemVisible(vehicle) && TimeBoardHelper.isActive(vehicle, range)) {
        Long vehicleId = vehicle.getId();
        TimeBoardRowLayout layout = new TimeBoardRowLayout(vehicleIndex);

        Collection<Trip> vehicleTrips = getTripsForLayout(vehicleId,
            separateCargo() ? null : range);

        for (Trip trip : vehicleTrips) {
          if (separateCargo()) {
            List<Freight> tripFreights = getFreightsForLayout(trip.getTripId(), range);
            if (!tripFreights.isEmpty()) {
              layout.addItems(getGroupIdForFreightLayout(trip), tripFreights, range,
                  Freight.getBlender());
            }

          } else {
            layout.addItem(getGroupIdForTripLayout(trip), trip, range, null);
          }
        }

        if (layout.isEmpty() && !layoutIdleVehicles()) {
          continue;
        }

        layout.addInactivity(TimeBoardHelper.getInactivity(vehicle, range), range);
        if (services.containsKey(vehicleId)) {
          layout.addInactivity(TimeBoardHelper.getActiveItems(services.get(vehicleId), range),
              range);
        }

        result.add(layout);
      }
    }
    return result;
  }

  private List<Freight> getFreightsForLayout(Long tripId, Range<JustDate> range) {
    List<Freight> result = Lists.newArrayList();
    if (!freights.containsKey(tripId)) {
      return result;
    }

    for (Freight freight : freights.get(tripId)) {
      if (isItemVisible(freight) && TimeBoardHelper.hasRangeAndIsActive(freight, range)) {
        result.add(freight);
      }
    }

    return result;
  }

  private List<Trip> getTripsForLayout(Long vehicleId, Range<JustDate> range) {
    List<Trip> result = Lists.newArrayList();
    if (!trips.containsKey(vehicleId)) {
      return result;
    }

    for (Trip trip : trips.get(vehicleId)) {
      if (isItemVisible(trip) && TimeBoardHelper.hasRangeAndIsActive(trip, range)) {
        result.add(trip);
      }
    }

    return result;
  }

  private boolean isTrailerPark() {
    return vehicleType == VehicleType.TRAILER;
  }

  private void onInfoResize(MoveEvent event) {
    int delta = event.getDeltaX();

    Element resizer = ((Mover) event.getSource()).getElement();
    int oldLeft = StyleUtils.getLeft(resizer);

    int maxLeft = getLastResizableColumnMaxLeft(getNumberWidth());
    int newLeft = BeeUtils.clamp(oldLeft + delta, getNumberWidth() + 1, maxLeft);

    if (newLeft != oldLeft || event.isFinished()) {
      int infoPx = newLeft - getNumberWidth() + TimeBoardHelper.DEFAULT_MOVER_WIDTH;

      if (newLeft != oldLeft) {
        StyleUtils.setLeft(resizer, newLeft);

        for (String id : infoPanels) {
          StyleUtils.setWidth(DomUtils.getElement(id),
              infoPx - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
        }
      }

      if (event.isFinished() && updateSetting(getInfoWidthColumnName(), infoPx)) {
        setInfoWidth(infoPx);
        render(false);
      }
    }
  }

  private void onNumberResize(MoveEvent event) {
    int delta = event.getDeltaX();

    Element resizer = ((Mover) event.getSource()).getElement();
    int oldLeft = StyleUtils.getLeft(resizer);

    int maxLeft;
    if (isInfoColumnVisible()) {
      maxLeft = getChartLeft() - TimeBoardHelper.DEFAULT_MOVER_WIDTH * 2 - 1;
    } else {
      maxLeft = getLastResizableColumnMaxLeft(0);
    }

    int newLeft = BeeUtils.clamp(oldLeft + delta, 1, maxLeft);

    if (newLeft != oldLeft || event.isFinished()) {
      int numberPx = newLeft + TimeBoardHelper.DEFAULT_MOVER_WIDTH;
      int infoPx = isInfoColumnVisible() ? getChartLeft() - numberPx : BeeConst.UNDEF;

      if (newLeft != oldLeft) {
        StyleUtils.setLeft(resizer, newLeft);

        for (String id : numberPanels) {
          StyleUtils.setWidth(DomUtils.getElement(id),
              numberPx - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
        }

        if (isInfoColumnVisible()) {
          for (String id : infoPanels) {
            Element element = Document.get().getElementById(id);
            if (element != null) {
              StyleUtils.setLeft(element, numberPx);
              StyleUtils.setWidth(element, infoPx - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
            }
          }
        }
      }

      if (event.isFinished()) {
        if (isInfoColumnVisible()) {
          if (updateSettings(getNumberWidthColumnName(), numberPx,
              getInfoWidthColumnName(), infoPx)) {
            setNumberWidth(numberPx);
            setInfoWidth(infoPx);
          }

        } else if (updateSetting(getNumberWidthColumnName(), numberPx)) {
          setNumberWidth(numberPx);
          render(false);
        }
      }
    }
  }

  private boolean separateCargo() {
    return separateCargo;
  }

  private void setInfoWidth(int infoWidth) {
    this.infoWidth = infoWidth;
  }

  private void setNumberWidth(int numberWidth) {
    this.numberWidth = numberWidth;
  }

  private void setSeparateCargo(boolean separateCargo) {
    this.separateCargo = separateCargo;
  }
}
