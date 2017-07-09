package com.butent.bee.client.modules.transport.charts;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
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
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.DndDiv;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CustomProperties;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.transport.TransportConstants.VehicleType;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

abstract class VehicleTimeBoard extends ChartBase {

  private static final BeeLogger logger = LogUtils.getLogger(VehicleTimeBoard.class);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "tr-vtb-";

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
  private static final String STYLE_TRIP_INFO = STYLE_TRIP_PREFIX + "info";

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

  private static final List<ChartDataType> TRIP_DATA_FILTERS = Arrays.asList(ChartDataType.TRIP,
      ChartDataType.TRIP_STATUS, ChartDataType.TRIP_DEPARTURE, ChartDataType.TRIP_ARRIVAL,
      ChartDataType.TRUCK, ChartDataType.TRAILER, ChartDataType.DRIVER);

  private static final List<ChartDataType> FREIGHT_DATA_FILTERS = Arrays.asList(
      ChartDataType.CUSTOMER, ChartDataType.MANAGER, ChartDataType.ORDER,
      ChartDataType.ORDER_STATUS, ChartDataType.CARGO, ChartDataType.CARGO_TYPE,
      ChartDataType.LOADING, ChartDataType.UNLOADING, ChartDataType.PLACE);

  private static final List<ChartDataType> HANDLING_DATA_FILTERS = Arrays.asList(
      ChartDataType.LOADING, ChartDataType.UNLOADING, ChartDataType.PLACE);

  private static final Set<ChartDataType> AVAILABLE_TYPES = EnumSet.allOf(ChartDataType.class)
      .stream().filter(type -> !type.equals(ChartDataType.DRIVER_GROUP))
      .collect(Collectors.toSet());

  private final List<Vehicle> vehicles = new ArrayList<>();

  private final Multimap<Long, Trip> trips = ArrayListMultimap.create();

  private final Multimap<Long, Freight> freights = ArrayListMultimap.create();

  private final Multimap<Long, VehicleService> services = ArrayListMultimap.create();

  private int numberWidth = BeeConst.UNDEF;
  private int infoWidth = BeeConst.UNDEF;

  private boolean separateCargo;

  private final Set<String> numberPanels = new HashSet<>();
  private final Set<String> infoPanels = new HashSet<>();

  private final List<Integer> vehicleIndexesByRow = new ArrayList<>();

  private final VehicleType vehicleType;
  private final VehicleType otherVehicleType;

  protected VehicleTimeBoard(ResponseObject settingsResponse) {
    super(settingsResponse);

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
      RowFactory.createRow(VIEW_VEHICLES, Modality.DISABLED);
    } else {
      super.handleAction(action);
    }
  }

  @Override
  protected void addFilterSettingParams(ParameterList params) {
    params.addDataItem(PROP_TRIPS, BeeUtils.intersects(getEnabledFilterDataTypes(),
        TRIP_DATA_FILTERS));
    params.addDataItem(PROP_FREIGHTS, BeeUtils.intersects(getEnabledFilterDataTypes(),
        FREIGHT_DATA_FILTERS));
    params.addDataItem(PROP_CARGO_HANDLING, BeeUtils.intersects(getEnabledFilterDataTypes(),
        HANDLING_DATA_FILTERS));
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
    List<ChartData> selectedData = FilterHelper.getSelectedData(getFilterData());
    if (selectedData.isEmpty()) {
      resetFilter(filterType);
      return false;
    }

    ChartData groupData = FilterHelper.getDataByType(selectedData, ChartDataType.VEHICLE_GROUP);

    CargoMatcher cargoMatcher = CargoMatcher.maybeCreate(selectedData);

    ChartData tripData = FilterHelper.getDataByType(selectedData, ChartDataType.TRIP);
    ChartData tripStatusData = FilterHelper.getDataByType(selectedData, ChartDataType.TRIP_STATUS);
    ChartData departureData = FilterHelper.getDataByType(selectedData,
        ChartDataType.TRIP_DEPARTURE);
    ChartData arrivalData = FilterHelper.getDataByType(selectedData, ChartDataType.TRIP_ARRIVAL);

    ChartData driverData = FilterHelper.getDataByType(selectedData, ChartDataType.DRIVER);

    PlaceMatcher placeMatcher = PlaceMatcher.maybeCreate(selectedData);

    boolean freightRequired = cargoMatcher != null || placeMatcher != null;
    boolean tripRequired = freightRequired
        || tripData != null || tripStatusData != null
        || departureData != null || arrivalData != null || driverData != null;

    for (Vehicle vehicle : vehicles) {
      boolean vehicleMatch = FilterHelper.matchesAny(groupData, vehicle.getGroups());

      boolean hasTrips = vehicleMatch && trips.containsKey(vehicle.getId());
      if (vehicleMatch && !hasTrips && tripRequired) {
        vehicleMatch = false;
      }

      if (vehicleMatch && hasTrips) {
        int tripCount = 0;

        for (Trip trip : trips.get(vehicle.getId())) {
          boolean tripMatch = FilterHelper.matches(tripData, trip.getTripId())
              && FilterHelper.matches(tripStatusData, trip.getStatus())
              && FilterHelper.matches(departureData, trip.getTripDeparture())
              && FilterHelper.matches(arrivalData, trip.getTripArrival())
              && trip.matchesDrivers(driverData);

          boolean hasFreights = tripMatch && freights.containsKey(trip.getTripId());
          if (tripMatch && !hasFreights && freightRequired) {
            tripMatch = false;
          }

          if (tripMatch && hasFreights) {
            int freightCount = 0;

            for (Freight freight : freights.get(trip.getTripId())) {
              boolean freightMatch = cargoMatcher == null || cargoMatcher.matches(freight);

              if (freightMatch && placeMatcher != null) {
                boolean ok = placeMatcher.matches(freight);
                if (!ok) {
                  ok = placeMatcher.matchesAnyOf(getCargoHandling(null, freight.getCargoId(),
                      freight.getCargoTripId()));
                }

                if (!ok) {
                  freightMatch = false;
                }
              }

              freight.setMatch(filterType, freightMatch);
              if (freightMatch) {
                freightCount++;
              }
            }

            if (freightCount <= 0) {
              tripMatch = false;
            }
          }

          trip.setMatch(filterType, tripMatch);
          if (tripMatch) {
            tripCount++;
          }
        }

        if (tripCount <= 0) {
          vehicleMatch = false;
        }
      }

      vehicle.setMatch(filterType, vehicleMatch);
    }

    return true;
  }

  protected Trip findTripById(Long tripId) {
    if (DataUtils.isId(tripId)) {
      for (Trip trip : trips.values()) {
        if (Objects.equals(trip.getTripId(), tripId)) {
          return trip;
        }
      }
    }
    return null;
  }

  protected abstract String getAdditionalInfo(Trip trip);

  @Override
  protected Set<ChartDataType> getAllFilterTypes() {
    return AVAILABLE_TYPES;
  }

  @Override
  protected Collection<? extends HasDateRange> getChartItems() {
    if (isFiltered()) {
      List<HasDateRange> result = new ArrayList<>();

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
      List<HasDateRange> result = new ArrayList<>();
      if (!freights.isEmpty()) {
        result.addAll(freights.values());
      }

      for (Trip trip : trips.values()) {
        if (!trip.hasCargo()) {
          result.add(trip);
        }
      }

      return result;

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

    vehicles.addAll(deserializeVehicles(properties));
    freights.putAll(deserializeFreight(properties, getCargoHandling()));
    trips.putAll(deserializeTrips(properties, freights));
    services.putAll(deserializeServices(properties));

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

      RowFactory.createRow(dataInfo, newRow, Modality.DISABLED);
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

    setDayColumnWidth(TimeBoardHelper.getPixels(getSettings(), getDayWidthColumnName(),
        getDefaultDayColumnWidth(getChartWidth()), 1, getChartWidth()));

    boolean sc = TimeBoardHelper.getBoolean(getSettings(), getSeparateCargoColumnName());
    if (separateCargo() != sc) {
      setSeparateCargo(sc);
      updateMaxRange();
    }
  }

  @Override
  protected List<ChartData> prepareFilterData(ResponseObject response) {
    if (response != null && response.getResponse() != null) {
      BeeRowSet rowSet = BeeRowSet.restore((String) response.getResponse());
      CustomProperties properties = rowSet.getTableProperties();

      if (properties == null) {
        return new ArrayList<>();
      }

      deserializeCountriesAndCities(properties.get(PROP_COUNTRIES), properties.get(PROP_CITIES));
      Multimap<Long, CargoHandling> cargoHandlingData = deserializeCargoHandling(properties
          .get(PROP_CARGO_HANDLING));

      Multimap<Long, Freight> freightData = deserializeFreight(properties, cargoHandlingData);
      return prepareFilterData(deserializeVehicles(properties),
          deserializeTrips(properties, freightData), freightData, cargoHandlingData);

    } else {
      boolean handlingRequired = BeeUtils.intersects(getEnabledFilterDataTypes(),
          HANDLING_DATA_FILTERS);
      boolean freightsRequired = BeeUtils.intersects(getEnabledFilterDataTypes(),
          FREIGHT_DATA_FILTERS) || handlingRequired;
      boolean tripRequired = BeeUtils.intersects(getEnabledFilterDataTypes(),
          TRIP_DATA_FILTERS) || freightsRequired;

      return prepareFilterData(vehicles, tripRequired ? trips : ArrayListMultimap.create(),
          freightsRequired ? freights : ArrayListMultimap.create(),
          handlingRequired ? getCargoHandling() : ArrayListMultimap.create());
    }
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
          offWidget = ((VehicleService) item).createWidget(STYLE_SERVICE_PANEL,
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

    numberMover.addMoveHandler(this::onNumberResize);

    panel.add(numberMover);

    if (isInfoColumnVisible()) {
      Mover infoMover = TimeBoardHelper.createHorizontalMover();
      StyleUtils.setLeft(infoMover, getChartLeft() - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
      StyleUtils.setHeight(infoMover, height);

      infoMover.addMoveHandler(this::onInfoResize);

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

    renderCargoShipment(panel, freight, freight.getTripTitle(), STYLE_TRIP_INFO);

    return panel;
  }

  private static IdentifiableWidget createInfoWidget(Vehicle vehicle, boolean hasOverlap) {
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

    renderTrip(panel, trip.getTitle(), getAdditionalInfo(trip),
        BeeUtils.getIfContains(freights, tripId), tripRange,
        STYLE_TRIP_VOID, STYLE_TRIP_INFO);

    return panel;
  }

  private Multimap<Long, Driver> deserializeDrivers(Map<String, String> properties) {
    Multimap<Long, Driver> drivers = HashMultimap.create();

    long millis = System.currentTimeMillis();
    SimpleRowSet srs = SimpleRowSet.getIfPresent(properties, PROP_TRIP_DRIVERS);

    if (!DataUtils.isEmpty(srs)) {
      for (SimpleRow row : srs) {
        drivers.put(row.getLong(COL_TRIP), new Driver(row.getLong(COL_DRIVER),
            row.getValue(ClassifierConstants.COL_FIRST_NAME),
            row.getValue(ClassifierConstants.COL_LAST_NAME),
            row.getDateTime(COL_TRIP_DRIVER_FROM), row.getDateTime(COL_TRIP_DRIVER_TO),
            row.getValue(COL_TRIP_DRIVER_NOTE)));
      }
      logger.debug(PROP_TRIP_DRIVERS, drivers.size(), TimeUtils.elapsedMillis(millis));
    }
    return drivers;
  }

  private Multimap<Long, Freight> deserializeFreight(Map<String, String> properties,
      Multimap<Long, CargoHandling> cargoHandling) {
    Multimap<Long, Freight> freightsData = ArrayListMultimap.create();
    long millis = System.currentTimeMillis();
    SimpleRowSet srs = SimpleRowSet.getIfPresent(properties, PROP_FREIGHTS);

    if (!DataUtils.isEmpty(srs)) {
      for (SimpleRow row : srs) {
        Pair<JustDate, JustDate> handlingSpan = getCargoHandlingSpan(cargoHandling,
            row.getLong(COL_CARGO), row.getLong(COL_CARGO_TRIP_ID));

        freightsData.put(row.getLong(COL_TRIP_ID),
            Freight.create(row, handlingSpan.getA(), handlingSpan.getB()));
      }
      logger.debug(PROP_FREIGHTS, freightsData.size(), TimeUtils.elapsedMillis(millis));
    }
    return freightsData;
  }

  private  Multimap<Long, VehicleService> deserializeServices(Map<String, String> properties) {
    Multimap<Long, VehicleService> servicesData = ArrayListMultimap.create();
    long millis = System.currentTimeMillis();
    SimpleRowSet srs = SimpleRowSet.getIfPresent(properties, PROP_VEHICLE_SERVICES);

    if (!DataUtils.isEmpty(srs)) {
      for (SimpleRow row : srs) {
        VehicleService service = new VehicleService(row);
        servicesData.put(service.getVehicleId(), service);
      }
      logger.debug(PROP_VEHICLE_SERVICES, servicesData.size(), TimeUtils.elapsedMillis(millis));
    }
    return servicesData;
  }

  private Multimap<Long, Trip> deserializeTrips(Map<String, String> properties,
      Multimap<Long, Freight> freightsData) {
    Multimap<Long, Driver> driversData = deserializeDrivers(properties);

    long millis = System.currentTimeMillis();
    Multimap<Long, Trip> tripsData = ArrayListMultimap.create();

    SimpleRowSet srs = SimpleRowSet.getIfPresent(properties, PROP_TRIPS);

    if (!DataUtils.isEmpty(srs)) {
      int index = srs.getColumnIndex(vehicleType.getTripVehicleIdColumnName());

      for (SimpleRow row : srs) {
        Long tripId = row.getLong(COL_TRIP_ID);

        Collection<Driver> tripDrivers = BeeUtils.getIfContains(driversData, tripId);

        if (freightsData.containsKey(tripId)) {
          JustDate minDate = null;
          JustDate maxDate = null;

          int cargoCount = 0;

          Collection<String> tripCustomers = new ArrayList<>();
          Collection<String> tripManagers = new ArrayList<>();

          for (Freight freight : freightsData.get(tripId)) {
            minDate = BeeUtils.min(minDate, freight.getMinDate());
            maxDate = BeeUtils.max(maxDate, freight.getMaxDate());

            cargoCount++;

            String customerName = freight.getCustomerName();
            if (!BeeUtils.isEmpty(customerName) && !tripCustomers.contains(customerName)) {
              tripCustomers.add(customerName);
            }

            String managerName = freight.getManagerName();
            if (!BeeUtils.isEmpty(managerName) && !tripManagers.contains(managerName)) {
              tripManagers.add(managerName);
            }
          }

          Trip trip = new Trip(row, tripDrivers, minDate, maxDate, cargoCount,
              tripCustomers, tripManagers);
          tripsData.put(row.getLong(index), trip);

          for (Freight freight : freightsData.get(tripId)) {
            freight.adjustRange(trip.getRange());

            freight.setTripTitle(trip.getTitle());
            freight.setEditable(trip.isEditable());
          }

        } else {
          tripsData.put(row.getLong(index), new Trip(row, tripDrivers));
        }
      }

      logger.debug(PROP_TRIPS, tripsData.size(), TimeUtils.elapsedMillis(millis));
    }
    return tripsData;
  }

  private static List<Vehicle> deserializeVehicles(Map<String, String> properties) {
    List<Vehicle> vehiclesData = new ArrayList<>();
    long millis = System.currentTimeMillis();
    BeeRowSet brs = BeeRowSet.getIfPresent(properties, PROP_VEHICLES);

    if (!DataUtils.isEmpty(brs)) {
      for (BeeRow row : brs) {
        vehiclesData.add(new Vehicle(row));
      }
      logger.debug(PROP_VEHICLES, vehiclesData.size(), TimeUtils.elapsedMillis(millis));
    }
    return vehiclesData;
  }

  private List<TimeBoardRowLayout> doLayout() {
    List<TimeBoardRowLayout> result = new ArrayList<>();
    Range<JustDate> range = getVisibleRange();

    for (int vehicleIndex = 0; vehicleIndex < vehicles.size(); vehicleIndex++) {
      Vehicle vehicle = vehicles.get(vehicleIndex);

      if (isItemVisible(vehicle) && TimeBoardHelper.isActive(vehicle, range)) {
        Long vehicleId = vehicle.getId();
        TimeBoardRowLayout layout = new TimeBoardRowLayout(vehicleIndex);

        Collection<Trip> vehicleTrips = getTripsForLayout(vehicleId, range);

        for (Trip trip : vehicleTrips) {
          if (separateCargo() && trip.hasCargo()) {
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
    List<Freight> result = new ArrayList<>();
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
    List<Trip> result = new ArrayList<>();

    if (range != null && trips.containsKey(vehicleId)) {
      for (Trip trip : trips.get(vehicleId)) {
        boolean ok = isItemVisible(trip) && trip.getRange() != null;

        if (ok) {
          if (separateCargo()) {
            ok = trip.hasCargo() || range.isConnected(trip.getRange());
          } else {
            ok = range.isConnected(trip.getRange());
          }

          if (ok) {
            result.add(trip);
          }
        }
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

  private List<ChartData> prepareFilterData(List<Vehicle> vehiclesData,
      Multimap<Long, Trip> tripsData, Multimap<Long, Freight> freightsData,
      Multimap<Long, CargoHandling> cargoHandlingData) {
    List<ChartData> data = new ArrayList<>();
    if (vehiclesData.isEmpty()) {
      return data;
    }

    ChartData truckData = new ChartData(ChartDataType.TRUCK);
    ChartData trailerData = new ChartData(ChartDataType.TRAILER);

    ChartData groupData = new ChartData(ChartDataType.VEHICLE_GROUP);

    ChartData modelData = new ChartData(ChartDataType.VEHICLE_MODEL);
    ChartData typeData = new ChartData(ChartDataType.VEHICLE_TYPE);

    ChartData customerData = new ChartData(ChartDataType.CUSTOMER);
    ChartData managerData = new ChartData(ChartDataType.MANAGER);

    ChartData orderData = new ChartData(ChartDataType.ORDER);
    ChartData orderStatusData = new ChartData(ChartDataType.ORDER_STATUS);

    ChartData cargoData = new ChartData(ChartDataType.CARGO);
    ChartData cargoTypeData = new ChartData(ChartDataType.CARGO_TYPE);

    ChartData tripData = new ChartData(ChartDataType.TRIP);
    ChartData tripStatusData = new ChartData(ChartDataType.TRIP_STATUS);
    ChartData departureData = new ChartData(ChartDataType.TRIP_DEPARTURE);
    ChartData arrivalData = new ChartData(ChartDataType.TRIP_ARRIVAL);

    ChartData loadData = new ChartData(ChartDataType.LOADING);
    ChartData unloadData = new ChartData(ChartDataType.UNLOADING);
    ChartData placeData = new ChartData(ChartDataType.PLACE);

    ChartData driverData = new ChartData(ChartDataType.DRIVER);

    for (Vehicle vehicle : vehiclesData) {
      String vehicleName = vehicle.getItemName();
      if (isTrailerPark()) {
        trailerData.add(vehicleName, vehicle.getId());
      } else {
        truckData.add(vehicleName, vehicle.getId());
      }

      if (!BeeUtils.isEmpty(vehicle.getGroups())) {
        for (Long group : vehicle.getGroups()) {
          groupData.add(getTransportGroupName(group), group);
        }
      }

      modelData.add(vehicle.getModel(), vehicle.getModelId());
      typeData.add(vehicle.getType(), vehicle.getTypeId());

      if (!tripsData.containsKey(vehicle.getId())) {
        continue;
      }

      for (Trip trip : tripsData.get(vehicle.getId())) {
        tripData.add(trip.getTripNo(), trip.getTripId());
        tripStatusData.add(trip.getStatus());
        departureData.add(trip.getTripDeparture());
        arrivalData.add(trip.getTripArrival());

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
            driverData.add(driver.getItemName(), driver.getId());
          }
        }
        if (!freightsData.containsKey(trip.getTripId())) {
          continue;
        }

        for (Freight freight : freightsData.get(trip.getTripId())) {
          customerData.add(freight.getCustomerName(), freight.getCustomerId());
          managerData.addUser(freight.getManager());

          orderData.add(freight.getOrderName(), freight.getOrderId());
          orderStatusData.add(freight.getOrderStatus());

          cargoData.add(freight.getCargoDescription(), freight.getCargoId());
          if (DataUtils.isId(freight.getCargoType())) {
            cargoTypeData.add(getCargoTypeName(freight.getCargoType()), freight.getCargoType());
          }

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

          Collection<CargoHandling> cargoHandling =
              getCargoHandling(cargoHandlingData, freight.getCargoId(), freight.getCargoTripId());

          if (!BeeUtils.isEmpty(cargoHandling)) {
            for (CargoHandling ch : cargoHandling) {
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
    data.add(groupData);

    data.add(modelData);
    data.add(typeData);

    data.add(customerData);
    data.add(managerData);

    data.add(orderData);
    data.add(orderStatusData);

    data.add(cargoData);
    data.add(cargoTypeData);

    data.add(tripData);
    data.add(tripStatusData);
    data.add(departureData);
    data.add(arrivalData);

    data.add(loadData);
    data.add(unloadData);
    data.add(placeData);

    data.add(driverData);

    data.add(isTrailerPark() ? truckData : trailerData);

    return data;
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
