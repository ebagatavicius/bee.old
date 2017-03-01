package com.butent.bee.client.modules.transport.charts;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.Rectangle;
import com.butent.bee.client.dom.Rulers;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.logical.MoveEvent;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.transport.TransportHandler;
import com.butent.bee.client.modules.transport.charts.Filterable.FilterType;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.client.timeboard.TimeBoardRowLayout;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.DndDiv;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DriverTimeBoard extends ChartBase {

  private static final class Absence implements HasDateRange {
    private final String name;
    private final String label;

    private final String background;
    private final String foreground;

    private final String notes;

    private final Range<JustDate> range;

    private Absence(JustDate dateFrom, JustDate dateTo, String name, String label,
        String background, String foreground, String notes) {
      this.name = name;
      this.label = label;

      this.background = background;
      this.foreground = foreground;

      this.notes = notes;

      this.range = Range.closed(dateFrom, BeeUtils.max(dateFrom, dateTo));
    }

    @Override
    public Range<JustDate> getRange() {
      return range;
    }
  }

  private static final class DriverTrip implements HasDateRange {
    private static final String dateFromLabel =
        Data.getColumnLabel(VIEW_TRIP_DRIVERS, COL_TRIP_DRIVER_FROM);
    private static final String dateToLabel =
        Data.getColumnLabel(VIEW_TRIP_DRIVERS, COL_TRIP_DRIVER_TO);
    private static final String noteLabel =
        Data.getColumnLabel(VIEW_TRIP_DRIVERS, COL_TRIP_DRIVER_NOTE);

    private final Long tripId;

    private final DateTime dateFrom;
    private final DateTime dateTo;

    private final String title;

    private Range<JustDate> range;

    private DriverTrip(Long tripId, DateTime dateFrom, DateTime dateTo, String note) {
      this.tripId = tripId;

      this.dateFrom = dateFrom;
      this.dateTo = dateTo;

      this.title = TimeBoardHelper.buildTitle(dateFromLabel, Format.renderDateTime(dateFrom),
          dateToLabel, Format.renderDateTime(dateTo), noteLabel, note);

      this.range = TimeBoardHelper.getActivity(JustDate.get(dateFrom), JustDate.get(dateTo));
    }

    @Override
    public Range<JustDate> getRange() {
      return range;
    }

    private void adjustRange(Range<JustDate> tripRange) {
      if (!TimeBoardHelper.isNormalized(getRange()) && TimeBoardHelper.isNormalized(tripRange)) {
        JustDate start = BeeUtils.nvl(JustDate.get(this.dateFrom), tripRange.lowerEndpoint());
        JustDate end = BeeUtils.nvl(JustDate.get(this.dateTo), tripRange.upperEndpoint());

        setRange(TimeBoardHelper.getActivity(start, end));
      }
    }

    private void setRange(Range<JustDate> range) {
      this.range = range;
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(DriverTimeBoard.class);

  static final String SUPPLIER_KEY = "driver_time_board";
  private static final String DATA_SERVICE = SVC_GET_DTB_DATA;

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "tr-dtb-";

  private static final String STYLE_DRIVER_PREFIX = STYLE_PREFIX + "Driver-";
  private static final String STYLE_DRIVER_ROW_SEPARATOR = STYLE_DRIVER_PREFIX + "row-sep";
  private static final String STYLE_DRIVER_PANEL = STYLE_DRIVER_PREFIX + "panel";
  private static final String STYLE_DRIVER_LABEL = STYLE_DRIVER_PREFIX + "label";
  private static final String STYLE_DRIVER_OVERLAP = STYLE_DRIVER_PREFIX + "overlap";
  private static final String STYLE_DRIVER_DRAG = STYLE_DRIVER_PREFIX + "drag";

  private static final String STYLE_TRIP_PREFIX = STYLE_PREFIX + "Trip-";
  private static final String STYLE_TRIP_PANEL = STYLE_TRIP_PREFIX + "panel";
  private static final String STYLE_TRIP_VOID = STYLE_TRIP_PREFIX + "void";
  private static final String STYLE_TRIP_INFO = STYLE_TRIP_PREFIX + "info";

  private static final String STYLE_ABSENCE_PREFIX = STYLE_PREFIX + "Absence-";
  private static final String STYLE_ABSENCE_PANEL = STYLE_ABSENCE_PREFIX + "panel";
  private static final String STYLE_ABSENCE_LABEL = STYLE_ABSENCE_PREFIX + "label";

  private static final String STYLE_INACTIVE = STYLE_PREFIX + "Inactive";
  private static final String STYLE_OVERLAP = STYLE_PREFIX + "Overlap";

  private static final Set<String> SETTINGS_COLUMNS_TRIGGERING_REFRESH =
      Sets.newHashSet(COL_DTB_MIN_DATE, COL_DTB_MAX_DATE,
          COL_DTB_TRANSPORT_GROUPS, COL_DTB_COMPLETED_TRIPS);

  static void open(final ViewCallback callback) {
    BeeKeeper.getRpc().makePostRequest(TransportHandler.createArgs(DATA_SERVICE),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            DriverTimeBoard dtb = new DriverTimeBoard();
            dtb.onCreate(response, callback);
          }
        });
  }

  private final List<Driver> drivers = new ArrayList<>();

  private final Multimap<Long, DriverTrip> driverTrips = ArrayListMultimap.create();
  private final Multimap<Long, Absence> driverAbsence = ArrayListMultimap.create();

  private final Map<Long, Trip> trips = new HashMap<>();
  private final Multimap<Long, Freight> freights = ArrayListMultimap.create();

  private int driverWidth = BeeConst.UNDEF;
  private Color itemColor;

  private DriverTimeBoard() {
    super();
    addStyleName(STYLE_PREFIX + "View");

    addRelevantDataViews(VIEW_DRIVERS, VIEW_DRIVER_ABSENCE, VIEW_ABSENCE_TYPES, VIEW_TRIP_DRIVERS,
        VIEW_TRIPS);
  }

  @Override
  public String getCaption() {
    return Localized.dictionary().driverTimeBoard();
  }

  @Override
  public String getIdPrefix() {
    return "tr-dtb";
  }

  @Override
  public String getSupplierKey() {
    return SUPPLIER_KEY;
  }

  @Override
  public void handleAction(Action action) {
    if (Action.ADD.equals(action)) {
      RowFactory.createRow(VIEW_DRIVERS, Modality.DISABLED);
    } else {
      super.handleAction(action);
    }
  }

  @Override
  protected boolean filter(FilterType filterType) {
    boolean filtered = false;

    List<ChartData> selectedData = FilterHelper.getSelectedData(getFilterData());
    if (selectedData.isEmpty()) {
      resetFilter(filterType);
      return filtered;
    }

    ChartData driverData = FilterHelper.getDataByType(selectedData, ChartData.Type.DRIVER);
    ChartData groupData = FilterHelper.getDataByType(selectedData, ChartData.Type.DRIVER_GROUP);

    ChartData truckData = FilterHelper.getDataByType(selectedData, ChartData.Type.TRUCK);
    ChartData trailerData = FilterHelper.getDataByType(selectedData, ChartData.Type.TRAILER);

    ChartData tripData = FilterHelper.getDataByType(selectedData, ChartData.Type.TRIP);
    ChartData tripStatusData = FilterHelper.getDataByType(selectedData, ChartData.Type.TRIP_STATUS);
    ChartData departureData = FilterHelper.getDataByType(selectedData,
        ChartData.Type.TRIP_DEPARTURE);
    ChartData arrivalData = FilterHelper.getDataByType(selectedData, ChartData.Type.TRIP_ARRIVAL);

    CargoMatcher cargoMatcher = CargoMatcher.maybeCreate(selectedData);
    PlaceMatcher placeMatcher = PlaceMatcher.maybeCreate(selectedData);

    boolean freightRequired = cargoMatcher != null || placeMatcher != null;
    boolean tripRequired = freightRequired || truckData != null || trailerData != null
        || tripData != null || tripStatusData != null
        || departureData != null || arrivalData != null;

    for (Driver driver : drivers) {
      boolean driverMatch = FilterHelper.matches(driverData, driver.getItemName())
          && FilterHelper.matchesAny(groupData, driver.getGroups());

      boolean hasTrips = driverMatch && driverTrips.containsKey(driver.getId());
      if (driverMatch && !hasTrips && tripRequired) {
        driverMatch = false;
      }

      if (driverMatch && hasTrips) {
        int tripCount = 0;

        for (DriverTrip driverTrip : driverTrips.get(driver.getId())) {
          Long tripId = driverTrip.tripId;
          Trip trip = trips.get(tripId);
          if (trip == null) {
            continue;
          }

          boolean tripMatch = FilterHelper.matches(tripData, tripId)
              && FilterHelper.matches(tripStatusData, trip.getStatus())
              && FilterHelper.matches(departureData, trip.getTripDeparture())
              && FilterHelper.matches(arrivalData, trip.getTripArrival())
              && FilterHelper.matches(truckData, trip.getTruckId())
              && FilterHelper.matches(trailerData, trip.getTrailerId());

          boolean hasFreights = tripMatch && freights.containsKey(tripId);
          if (tripMatch && !hasFreights && freightRequired) {
            tripMatch = false;
          }

          if (tripMatch && hasFreights) {
            int freightCount = 0;

            for (Freight freight : freights.get(tripId)) {
              boolean freightMatch = cargoMatcher == null || cargoMatcher.matches(freight);

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
          driverMatch = false;
        }
      }

      driver.setMatch(filterType, driverMatch);

      if (driverMatch) {
        filtered = true;
      }
    }

    return filtered;
  }

  @Override
  protected Collection<? extends HasDateRange> getChartItems() {
    if (isFiltered()) {
      List<HasDateRange> result = new ArrayList<>();

      for (Driver driver : drivers) {
        if (isItemVisible(driver) && driverTrips.containsKey(driver.getId())) {
          for (DriverTrip driverTrip : driverTrips.get(driver.getId())) {
            if (isItemVisible(trips.get(driverTrip.tripId))) {
              result.add(driverTrip);
            }
          }
        }
      }

      return result;

    } else {
      return driverTrips.values();
    }
  }

  @Override
  protected String getDataService() {
    return DATA_SERVICE;
  }

  @Override
  protected String getFilterDataTypesColumnName() {
    return COL_DTB_FILTER_DATA_TYPES;
  }

  @Override
  protected String getFiltersColumnName() {
    return COL_DTB_FILTERS;
  }

  @Override
  protected String getFooterHeightColumnName() {
    return COL_DTB_FOOTER_HEIGHT;
  }

  @Override
  protected String getHeaderHeightColumnName() {
    return COL_DTB_HEADER_HEIGHT;
  }

  @Override
  protected String getRefreshLocalChangesColumnName() {
    return COL_DTB_REFRESH_LOCAL_CHANGES;
  }

  @Override
  protected String getRefreshRemoteChangesColumnName() {
    return COL_DTB_REFRESH_REMOTE_CHANGES;
  }

  @Override
  protected String getRowHeightColumnName() {
    return COL_DTB_PIXELS_PER_ROW;
  }

  @Override
  protected Collection<String> getSettingsColumnsTriggeringRefresh() {
    return SETTINGS_COLUMNS_TRIGGERING_REFRESH;
  }

  @Override
  protected String getSettingsFormName() {
    return FORM_DTB_SETTINGS;
  }

  @Override
  protected String getShowAdditionalInfoColumnName() {
    return COL_DTB_ADDITIONAL_INFO;
  }

  @Override
  protected String getShowCountryFlagsColumnName() {
    return COL_DTB_COUNTRY_FLAGS;
  }

  @Override
  protected String getShowOrderCustomerColumnName() {
    return null;
  }

  @Override
  protected String getShowOderNoColumnName() {
    return null;
  }

  @Override
  protected String getShowPlaceInfoColumnName() {
    return COL_DTB_PLACE_INFO;
  }

  @Override
  protected String getShowPlaceCitiesColumnName() {
    return COL_DTB_PLACE_CITIES;
  }

  @Override
  protected String getShowPlaceCodesColumnName() {
    return COL_DTB_PLACE_CODES;
  }

  @Override
  protected String getStripOpacityColumnName() {
    return COL_DTB_STRIP_OPACITY;
  }

  @Override
  protected String getThemeColumnName() {
    return null;
  }

  @Override
  protected void initData(Map<String, String> properties) {
    drivers.clear();
    driverTrips.clear();
    driverAbsence.clear();

    trips.clear();
    freights.clear();

    if (BeeUtils.isEmpty(properties)) {
      return;
    }

    long millis = System.currentTimeMillis();
    BeeRowSet brs = BeeRowSet.getIfPresent(properties, PROP_DRIVERS);

    if (!DataUtils.isEmpty(brs)) {
      int firstNameIndex = brs.getColumnIndex(ClassifierConstants.COL_FIRST_NAME);
      int lastNameIndex = brs.getColumnIndex(ClassifierConstants.COL_LAST_NAME);

      int startDateIndex = brs.getColumnIndex(COL_DRIVER_START_DATE);
      int endDateIndex = brs.getColumnIndex(COL_DRIVER_END_DATE);

      int experienceIndex = brs.getColumnIndex(COL_DRIVER_EXPERIENCE);
      int notesIndex = brs.getColumnIndex(COL_DRIVER_NOTES);

      for (BeeRow row : brs.getRows()) {
        drivers.add(new Driver(row.getId(),
            row.getString(firstNameIndex), row.getString(lastNameIndex),
            row.getDate(startDateIndex), row.getDate(endDateIndex),
            row.getDate(experienceIndex), row.getString(notesIndex),
            DataUtils.parseIdSet(row.getProperty(PROP_DRIVER_GROUPS))));
      }

      logger.debug(PROP_DRIVERS, drivers.size(), TimeUtils.elapsedMillis(millis));
    }

    if (drivers.isEmpty()) {
      return;
    }

    millis = System.currentTimeMillis();
    brs = BeeRowSet.getIfPresent(properties, PROP_ABSENCE);

    if (!DataUtils.isEmpty(brs)) {
      int driverIndex = brs.getColumnIndex(COL_DRIVER);

      int dateFromIndex = brs.getColumnIndex(COL_ABSENCE_FROM);
      int dateToIndex = brs.getColumnIndex(COL_ABSENCE_TO);

      int nameIndex = brs.getColumnIndex(ALS_ABSENCE_NAME);
      int labelIndex = brs.getColumnIndex(ALS_ABSENCE_LABEL);

      int bgIndex = brs.getColumnIndex(AdministrationConstants.COL_BACKGROUND);
      int fgIndex = brs.getColumnIndex(AdministrationConstants.COL_FOREGROUND);

      int notesIndex = brs.getColumnIndex(COL_ABSENCE_NOTES);

      for (BeeRow row : brs.getRows()) {
        Long driverId = row.getLong(driverIndex);
        JustDate dateFrom = row.getDate(dateFromIndex);

        if (DataUtils.isId(driverId) && dateFrom != null) {
          driverAbsence.put(driverId, new Absence(dateFrom, row.getDate(dateToIndex),
              row.getString(nameIndex), row.getString(labelIndex),
              row.getString(bgIndex), row.getString(fgIndex), row.getString(notesIndex)));
        }
      }

      logger.debug(PROP_ABSENCE, driverAbsence.size(), TimeUtils.elapsedMillis(millis));
    }

    millis = System.currentTimeMillis();
    SimpleRowSet srs = SimpleRowSet.getIfPresent(properties, PROP_FREIGHTS);

    if (!DataUtils.isEmpty(srs)) {
      for (SimpleRow row : srs) {
        Pair<JustDate, JustDate> handlingSpan = getCargoHandlingSpan(row.getLong(COL_CARGO));
        freights.put(row.getLong(COL_TRIP_ID),
            Freight.create(row, handlingSpan.getA(), handlingSpan.getB()));
      }
      logger.debug(PROP_FREIGHTS, freights.size(), TimeUtils.elapsedMillis(millis));
    }

    Multimap<Long, Driver> tripDrivers = HashMultimap.create();

    millis = System.currentTimeMillis();
    srs = SimpleRowSet.getIfPresent(properties, PROP_TRIP_DRIVERS);

    if (!DataUtils.isEmpty(srs)) {
      for (SimpleRow row : srs) {
        Long driverId = row.getLong(COL_DRIVER);
        Long tripId = row.getLong(COL_TRIP);

        DateTime dateFrom = row.getDateTime(COL_TRIP_DRIVER_FROM);
        DateTime dateTo = row.getDateTime(COL_TRIP_DRIVER_TO);

        String note = row.getValue(COL_TRIP_DRIVER_NOTE);

        if (DataUtils.isId(driverId) && DataUtils.isId(tripId)) {
          driverTrips.put(driverId, new DriverTrip(tripId, dateFrom, dateTo, note));

          tripDrivers.put(tripId, new Driver(driverId,
              row.getValue(ClassifierConstants.COL_FIRST_NAME),
              row.getValue(ClassifierConstants.COL_LAST_NAME),
              dateFrom, dateTo, note));
        }
      }

      logger.debug(PROP_TRIP_DRIVERS, driverTrips.size(), TimeUtils.elapsedMillis(millis));
    }

    millis = System.currentTimeMillis();
    srs = SimpleRowSet.getIfPresent(properties, PROP_TRIPS);

    if (!DataUtils.isEmpty(srs)) {
      for (SimpleRow row : srs) {
        Long tripId = row.getLong(COL_TRIP_ID);

        Collection<Driver> td = BeeUtils.getIfContains(tripDrivers, tripId);

        if (freights.containsKey(tripId)) {
          JustDate minDate = null;
          JustDate maxDate = null;

          int cargoCount = 0;

          Collection<String> tripCustomers = new ArrayList<>();
          Collection<String> tripManagers = new ArrayList<>();

          for (Freight freight : freights.get(tripId)) {
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

          Trip trip = new Trip(row, td, minDate, maxDate, cargoCount, tripCustomers, tripManagers);
          trips.put(tripId, trip);

          for (Freight freight : freights.get(tripId)) {
            freight.adjustRange(trip.getRange());

            freight.setTripTitle(trip.getTitle());
            freight.setEditable(trip.isEditable());
          }

        } else {
          trips.put(tripId, new Trip(row, td));
        }
      }

      logger.debug(PROP_TRIPS, trips.size(), TimeUtils.elapsedMillis(millis));
    }

    millis = System.currentTimeMillis();
    for (DriverTrip driverTrip : driverTrips.values()) {
      Trip trip = trips.get(driverTrip.tripId);
      if (trip != null) {
        driverTrip.adjustRange(trip.getRange());
      }
    }

    logger.debug("driver trips adjust range", TimeUtils.elapsedMillis(millis));
  }

  @Override
  protected boolean persistFilter() {
    return FilterHelper.persistFilter(drivers)
        | FilterHelper.persistFilter(trips.values())
        | FilterHelper.persistFilter(freights.values());
  }

  @Override
  protected void prepareChart(Size canvasSize) {
    setDriverWidth(TimeBoardHelper.getPixels(getSettings(), COL_DTB_PIXELS_PER_DRIVER, 140,
        TimeBoardHelper.DEFAULT_MOVER_WIDTH + 1, canvasSize.getWidth() / 3));

    setChartLeft(getDriverWidth());
    setChartWidth(canvasSize.getWidth() - getChartLeft() - getChartRight());

    setDayColumnWidth(TimeBoardHelper.getPixels(getSettings(), COL_DTB_PIXELS_PER_DAY,
        getDefaultDayColumnWidth(getChartWidth()), 1, getChartWidth()));

    Long colorId = TimeBoardHelper.getLong(getSettings(), COL_DTB_COLOR);
    setItemColor((colorId == null) ? null : findColor(colorId));
  }

  @Override
  protected List<ChartData> prepareFilterData() {
    List<ChartData> data = new ArrayList<>();
    if (drivers.isEmpty()) {
      return data;
    }

    ChartData driverData = new ChartData(ChartData.Type.DRIVER);
    ChartData groupData = new ChartData(ChartData.Type.DRIVER_GROUP);

    ChartData truckData = new ChartData(ChartData.Type.TRUCK);
    ChartData trailerData = new ChartData(ChartData.Type.TRAILER);

    ChartData tripData = new ChartData(ChartData.Type.TRIP);
    ChartData tripStatusData = new ChartData(ChartData.Type.TRIP_STATUS);
    ChartData departureData = new ChartData(ChartData.Type.TRIP_DEPARTURE);
    ChartData arrivalData = new ChartData(ChartData.Type.TRIP_ARRIVAL);

    ChartData customerData = new ChartData(ChartData.Type.CUSTOMER);
    ChartData managerData = new ChartData(ChartData.Type.MANAGER);

    ChartData orderData = new ChartData(ChartData.Type.ORDER);
    ChartData orderStatusData = new ChartData(ChartData.Type.ORDER_STATUS);

    ChartData cargoData = new ChartData(ChartData.Type.CARGO);
    ChartData cargoTypeData = new ChartData(ChartData.Type.CARGO_TYPE);

    ChartData loadData = new ChartData(ChartData.Type.LOADING);
    ChartData unloadData = new ChartData(ChartData.Type.UNLOADING);
    ChartData placeData = new ChartData(ChartData.Type.PLACE);

    Set<Long> processedTrips = new HashSet<>();

    for (Driver driver : drivers) {
      driverData.add(driver.getItemName());

      if (!BeeUtils.isEmpty(driver.getGroups())) {
        for (Long group : driver.getGroups()) {
          groupData.add(getTransportGroupName(group), group);
        }
      }

      if (!driverTrips.containsKey(driver.getId())) {
        continue;
      }

      for (DriverTrip driverTrip : driverTrips.get(driver.getId())) {
        Long tripId = driverTrip.tripId;
        if (processedTrips.contains(tripId)) {
          continue;
        }
        processedTrips.add(tripId);

        Trip trip = trips.get(tripId);
        if (trip == null) {
          continue;
        }

        if (DataUtils.isId(trip.getTruckId())) {
          truckData.add(trip.getTruckNumber(), trip.getTruckId());
        }
        if (DataUtils.isId(trip.getTrailerId())) {
          trailerData.add(trip.getTrailerNumber(), trip.getTrailerId());
        }

        tripData.add(trip.getTripNo(), tripId);
        tripStatusData.add(trip.getStatus());
        departureData.add(trip.getTripDeparture());
        arrivalData.add(trip.getTripArrival());

        if (!freights.containsKey(tripId)) {
          continue;
        }

        for (Freight freight : freights.get(trip.getTripId())) {
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

    data.add(driverData);
    data.add(groupData);

    data.add(truckData);
    data.add(trailerData);

    data.add(tripData);
    data.add(tripStatusData);
    data.add(departureData);
    data.add(arrivalData);

    data.add(customerData);
    data.add(managerData);

    data.add(orderData);
    data.add(orderStatusData);

    data.add(cargoData);
    data.add(cargoTypeData);

    data.add(loadData);
    data.add(unloadData);
    data.add(placeData);

    return data;
  }

  @Override
  protected void renderContent(ComplexPanel panel) {
    List<TimeBoardRowLayout> driverLayout = doLayout();

    int rc = TimeBoardRowLayout.countRows(driverLayout, 1);
    initContent(panel, rc);

    if (driverLayout.isEmpty()) {
      return;
    }

    int calendarWidth = getCalendarWidth();

    Double opacity = TimeBoardHelper.getOpacity(getSettings(), COL_DTB_ITEM_OPACITY);

    Edges margins = new Edges();
    margins.setBottom(TimeBoardHelper.ROW_SEPARATOR_HEIGHT);

    Widget driverWidget;
    Widget offWidget;
    Widget itemWidget;
    Widget overlapWidget;

    int rowIndex = 0;
    for (TimeBoardRowLayout layout : driverLayout) {

      int size = layout.getSize(1);
      int top = rowIndex * getRowHeight();

      if (rowIndex > 0) {
        TimeBoardHelper.addRowSeparator(panel, STYLE_DRIVER_ROW_SEPARATOR, top, 0,
            getChartLeft() + calendarWidth);
      }

      Driver driver = drivers.get(layout.getDataIndex());
      String driverName = driver.getItemName();

      driverWidget = createDriverWidget(driver, layout.hasOverlap());
      addDriverWidget(panel, driverWidget, rowIndex, rowIndex + size - 1);

      for (int i = 1; i < size; i++) {
        TimeBoardHelper.addRowSeparator(panel, top + getRowHeight() * i, getChartLeft(),
            calendarWidth);
      }

      for (HasDateRange item : layout.getInactivity()) {
        offWidget = new CustomDiv(STYLE_INACTIVE);
        if (!BeeUtils.isEmpty(driver.getTitle())) {
          offWidget.setTitle(BeeUtils.buildLines(driverName, driver.getTitle()));
        }

        Rectangle rectangle = getRectangle(item.getRange(), rowIndex, rowIndex + size - 1);
        TimeBoardHelper.apply(offWidget, rectangle, margins);

        panel.add(offWidget);
      }

      for (int i = 0; i < layout.getRows().size(); i++) {
        for (HasDateRange item : layout.getRows().get(i).getRowItems()) {

          if (item instanceof DriverTrip) {
            itemWidget = createTripWidget(driverName, (DriverTrip) item);
          } else if (item instanceof Absence) {
            itemWidget = createAbsenceWidget(driverName, (Absence) item);
          } else {
            itemWidget = null;
          }

          if (itemWidget != null) {
            Rectangle rectangle = getRectangle(item.getRange(), rowIndex + i);
            TimeBoardHelper.apply(itemWidget, rectangle, margins);
            if (opacity != null) {
              StyleUtils.setOpacity(itemWidget, opacity);
            }

            panel.add(itemWidget);
          }

          Set<Range<JustDate>> overlap = layout.getOverlap(item.getRange());

          for (Range<JustDate> over : overlap) {
            overlapWidget = new CustomDiv(STYLE_OVERLAP);

            Rectangle rectangle = getRectangle(over, rowIndex + i);
            TimeBoardHelper.apply(overlapWidget, rectangle, margins);

            panel.add(overlapWidget);
          }
        }
      }

      rowIndex += size;
    }
  }

  @Override
  protected void renderMovers(ComplexPanel panel, int height) {
    Mover driverMover = TimeBoardHelper.createHorizontalMover();
    StyleUtils.setLeft(driverMover, getChartLeft() - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
    StyleUtils.setHeight(driverMover, height);

    driverMover.addMoveHandler(this::onDriverResize);

    panel.add(driverMover);
  }

  @Override
  protected void resetFilter(FilterType filterType) {
    FilterHelper.resetFilter(drivers, filterType);
    FilterHelper.resetFilter(trips.values(), filterType);
    FilterHelper.resetFilter(freights.values(), filterType);
  }

  @Override
  protected void setItemWidgetColor(HasDateRange item, Widget widget) {
    if (getItemColor() != null) {
      UiHelper.setColor(widget, getItemColor());
    }
  }

  @Override
  protected void updateMaxRange() {
    super.updateMaxRange();

    if (!driverAbsence.isEmpty()) {
      Range<JustDate> absenceSpan = TimeBoardHelper.getSpan(driverAbsence.values());
      if (TimeBoardHelper.isNormalized(absenceSpan)) {
        extendMaxRange(absenceSpan.lowerEndpoint(), absenceSpan.upperEndpoint());
      }
    }

    clampMaxRange(COL_DTB_MIN_DATE, COL_DTB_MAX_DATE);
  }

  private void addDriverWidget(HasWidgets panel, Widget widget, int firstRow, int lastRow) {
    Rectangle rectangle = TimeBoardHelper.getRectangle(0, getDriverWidth(), firstRow, lastRow,
        getRowHeight());

    Edges margins = new Edges();
    margins.setRight(TimeBoardHelper.DEFAULT_MOVER_WIDTH);
    margins.setBottom(TimeBoardHelper.ROW_SEPARATOR_HEIGHT);

    TimeBoardHelper.apply(widget, rectangle, margins);
    panel.add(widget);
  }

  private Widget createAbsenceWidget(String driverName, Absence da) {
    Flow panel = new Flow();
    panel.addStyleName(STYLE_ABSENCE_PANEL);

    panel.setTitle(BeeUtils.buildLines(driverName, TimeBoardHelper.getRangeLabel(da.getRange()),
        da.name, da.notes));

    if (!BeeUtils.isEmpty(da.background)) {
      StyleUtils.setBackgroundColor(panel, da.background);
    }
    if (!BeeUtils.isEmpty(da.foreground)) {
      StyleUtils.setColor(panel, da.foreground);
    }

    if (!BeeUtils.isEmpty(da.label)) {
      Range<JustDate> range = TimeBoardHelper.normalizedIntersection(da.getRange(),
          getVisibleRange());
      int dayCount = TimeBoardHelper.getSize(range);

      int dayWidth = getDayColumnWidth();
      int panelWidth = dayCount * dayWidth;

      Size labelSize = Rulers.getLineSize(null, da.label.trim(), false);
      int labelWidth = labelSize.getWidth();
      int labelTop = Math.max((getRowHeight() - labelSize.getHeight()) / 2, 0);

      int incr = (labelWidth % dayWidth > 0) ? 1 : 0;
      int step = Math.min(panelWidth, (labelWidth / dayWidth + incr) * dayWidth);

      if (step > 0) {
        for (int x = 0; x <= panelWidth - step; x += step) {
          CustomDiv label = new CustomDiv(STYLE_ABSENCE_LABEL);
          label.setHtml(da.label);

          StyleUtils.setLeft(label, x);
          StyleUtils.setWidth(label, step);
          StyleUtils.setTop(label, labelTop);

          panel.add(label);
        }
      }
    }

    return panel;
  }

  private static Widget createDriverWidget(Driver driver, boolean hasOverlap) {
    Simple panel = new Simple();
    panel.addStyleName(STYLE_DRIVER_PANEL);
    if (hasOverlap) {
      panel.addStyleName(STYLE_DRIVER_OVERLAP);
    }

    DndDiv widget = new DndDiv(STYLE_DRIVER_LABEL);
    widget.setHtml(driver.getItemName());

    UiHelper.maybeSetTitle(widget, driver.getTitle());

    bindOpener(widget, VIEW_DRIVERS, driver.getId());

    DndHelper.makeSource(widget, DATA_TYPE_DRIVER, driver, STYLE_DRIVER_DRAG);

    panel.add(widget);

    return panel;
  }

  private Widget createTripWidget(String driverName, DriverTrip item) {
    Flow panel = new Flow();
    panel.addStyleName(STYLE_TRIP_PANEL);
    setItemWidgetColor(item, panel);

    Long tripId = item.tripId;
    Trip trip = trips.get(tripId);

    List<String> titleLines = new ArrayList<>();
    String tripInfo = null;
    if (trip != null) {
      titleLines.add(trip.getTitle());
      tripInfo = BeeUtils.joinItems(trip.getTruckNumber(), trip.getTrailerNumber(),
          trip.getCustomerNames());
    }

    if (!BeeUtils.isEmpty(item.title)) {
      titleLines.add(BeeConst.STRING_NBSP);
      titleLines.add(driverName);
      titleLines.add(item.title);
    }

    String title = BeeUtils.buildLines(titleLines);
    panel.setTitle(title);

    bindOpener(panel, VIEW_TRIPS, item.tripId);

    Range<JustDate> range = TimeBoardHelper.normalizedIntersection(item.getRange(),
        getVisibleRange());
    if (range == null) {
      return panel;
    }

    renderTrip(panel, title, tripInfo, BeeUtils.getIfContains(freights, tripId), range,
        STYLE_TRIP_VOID, STYLE_TRIP_INFO);

    return panel;
  }

  private List<TimeBoardRowLayout> doLayout() {
    List<TimeBoardRowLayout> result = new ArrayList<>();
    Range<JustDate> range = getVisibleRange();

    for (int driverIndex = 0; driverIndex < drivers.size(); driverIndex++) {
      Driver driver = drivers.get(driverIndex);

      if (isItemVisible(driver) && TimeBoardHelper.isActive(driver, range)) {
        Long driverId = driver.getId();
        TimeBoardRowLayout layout = new TimeBoardRowLayout(driverIndex);

        layout.addItems(driverId, getDriverTripsForLayout(driverId, range), range);
        layout.addItems(driverId, getAbsence(driverId, range), range);

        layout.addInactivity(TimeBoardHelper.getInactivity(driver, range), range);

        result.add(layout);
      }
    }

    return result;
  }

  private List<HasDateRange> getAbsence(long driverId, Range<JustDate> range) {
    List<HasDateRange> absence = new ArrayList<>();

    if (driverAbsence.containsKey(driverId)) {
      absence.addAll(TimeBoardHelper.getActiveItems(driverAbsence.get(driverId), range));
    }
    return absence;
  }

  private List<HasDateRange> getDriverTripsForLayout(long driverId, Range<JustDate> range) {
    List<HasDateRange> dts = new ArrayList<>();

    if (driverTrips.containsKey(driverId)) {
      if (isFiltered()) {
        for (DriverTrip driverTrip : driverTrips.get(driverId)) {
          if (TimeBoardHelper.hasRangeAndIsActive(driverTrip, range)
              && isItemVisible(trips.get(driverTrip.tripId))) {
            dts.add(driverTrip);
          }
        }

      } else {
        dts.addAll(TimeBoardHelper.getActiveItems(driverTrips.get(driverId), range));
      }
    }
    return dts;
  }

  private int getDriverWidth() {
    return driverWidth;
  }

  private Color getItemColor() {
    return itemColor;
  }

  private void onDriverResize(MoveEvent event) {
    int delta = event.getDeltaX();

    Element resizer = ((Mover) event.getSource()).getElement();
    int oldLeft = StyleUtils.getLeft(resizer);

    int maxLeft = getLastResizableColumnMaxLeft(0);
    int newLeft = BeeUtils.clamp(oldLeft + delta, 1, maxLeft);

    if (newLeft != oldLeft || event.isFinished()) {
      if (newLeft != oldLeft) {
        StyleUtils.setLeft(resizer, newLeft);
      }

      int px = newLeft + TimeBoardHelper.DEFAULT_MOVER_WIDTH;
      if (event.isFinished() && updateSetting(COL_DTB_PIXELS_PER_DRIVER, px)) {
        setDriverWidth(px);
        render(false);
      }
    }
  }

  private void setDriverWidth(int driverWidth) {
    this.driverWidth = driverWidth;
  }

  private void setItemColor(Color itemColor) {
    this.itemColor = itemColor;
  }
}
