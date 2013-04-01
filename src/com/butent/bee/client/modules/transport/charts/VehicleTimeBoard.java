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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.Rectangle;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.logical.MoveEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.transport.charts.Filterable.FilterType;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.BeeLabel;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.DndDiv;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.commons.CommonsConstants;
import com.butent.bee.shared.modules.transport.TransportConstants.OrderStatus;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

abstract class VehicleTimeBoard extends ChartBase {

  private static final BeeLogger logger = LogUtils.getLogger(VehicleTimeBoard.class);

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

  private boolean separateCargo = false;

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
    Rectangle rectangle = ChartHelper.getRectangle(getNumberWidth(), getInfoWidth(),
        firstRow, lastRow, getRowHeight());

    Edges margins = new Edges();
    margins.setRight(ChartHelper.DEFAULT_MOVER_WIDTH);
    margins.setBottom(ChartHelper.ROW_SEPARATOR_HEIGHT);

    ChartHelper.apply(widget.asWidget(), rectangle, margins);

    panel.add(widget.asWidget());

    infoPanels.add(widget.getId());
  }

  @Override
  protected void clearFilter() {
    resetFilter(FilterType.PERSISTENT);
    super.clearFilter();

    render(false);
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
  protected boolean filter(DialogBox dialog) {
    persistFilter();

    dialog.close();
    render(false);

    return true;
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
        if (vehicle.matched(FilterType.PERSISTENT) && trips.containsKey(vehicle.getId())) {

          if (separateCargo()) {
            for (Trip trip : trips.get(vehicle.getId())) {
              if (trip.matched(FilterType.PERSISTENT) && freights.containsKey(trip.getTripId())) {
                result.addAll(ChartHelper.filterItems(freights.get(trip.getTripId()),
                    FilterType.PERSISTENT));
              }
            }

          } else {
            result.addAll(ChartHelper.filterItems(trips.get(vehicle.getId()),
                FilterType.PERSISTENT));
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

  @Override
  protected Set<Action> getEnabledActions() {
    return EnumSet.of(Action.REFRESH, Action.ADD, Action.CONFIGURE, Action.FILTER);
  }

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
  protected void initData(BeeRowSet rowSet) {
    vehicles.clear();
    trips.clear();
    freights.clear();
    services.clear();

    if (rowSet == null) {
      updateMaxRange();
      return;
    }

    String serialized = rowSet.getTableProperty(PROP_VEHICLES);
    if (!BeeUtils.isEmpty(serialized)) {
      BeeRowSet brs = BeeRowSet.restore(serialized);
      for (BeeRow row : brs.getRows()) {
        vehicles.add(new Vehicle(row));
      }
    }

    Multimap<Long, Driver> drivers = HashMultimap.create();

    serialized = rowSet.getTableProperty(PROP_DRIVERS);
    if (!BeeUtils.isEmpty(serialized)) {
      SimpleRowSet srs = SimpleRowSet.restore(serialized);

      for (SimpleRow row : srs) {
        drivers.put(row.getLong(COL_TRIP), new Driver(row.getLong(COL_DRIVER),
            row.getValue(CommonsConstants.COL_FIRST_NAME),
            row.getValue(CommonsConstants.COL_LAST_NAME),
            row.getDate(COL_TRIP_DRIVER_FROM), row.getDate(COL_TRIP_DRIVER_TO)));
      }
    }

    serialized = rowSet.getTableProperty(PROP_FREIGHTS);
    if (!BeeUtils.isEmpty(serialized)) {
      SimpleRowSet srs = SimpleRowSet.restore(serialized);

      for (SimpleRow row : srs) {
        Pair<JustDate, JustDate> handlingSpan = getCargoHandlingSpan(row.getLong(COL_CARGO));
        freights.put(row.getLong(COL_TRIP_ID),
            Freight.create(row, handlingSpan.getA(), handlingSpan.getB()));
      }
    }

    serialized = rowSet.getTableProperty(PROP_TRIPS);
    if (!BeeUtils.isEmpty(serialized)) {
      SimpleRowSet srs = SimpleRowSet.restore(serialized);
      int index = srs.getColumnIndex(vehicleType.getTripVehicleIdColumnName());

      for (SimpleRow row : srs) {
        Long tripId = row.getLong(COL_TRIP_ID);

        Collection<Driver> tripDrivers = BeeUtils.getIfContains(drivers, tripId);
        int cargoCount = 0;

        if (freights.containsKey(tripId)) {
          JustDate maxDate = null;
          for (Freight freight : freights.get(tripId)) {
            maxDate = BeeUtils.max(maxDate, freight.getMaxDate());
            cargoCount++;
          }

          Trip trip = new Trip(row, maxDate, tripDrivers, cargoCount);
          trips.put(row.getLong(index), trip);

          for (Freight freight : freights.get(tripId)) {
            freight.adjustRange(trip.getRange());
            freight.setTripTitle(trip.getTitle());
          }

        } else {
          trips.put(row.getLong(index), new Trip(row, null, tripDrivers, cargoCount));
        }
      }
    }

    serialized = rowSet.getTableProperty(PROP_VEHICLE_SERVICES);
    if (!BeeUtils.isEmpty(serialized)) {
      SimpleRowSet srs = SimpleRowSet.restore(serialized);
      for (SimpleRow row : srs) {
        VehicleService service = new VehicleService(row);
        services.put(service.getVehicleId(), service);
      }
    }

    setSeparateCargo(ChartHelper.getBoolean(getSettings(), getSeparateCargoColumnName()));
    updateMaxRange();

    logger.debug(getCaption(), vehicles.size(), trips.size(), drivers.size(), freights.size(),
        services.size());
  }

  @Override
  protected List<ChartData> initFilter() {
    return prepareFilterData(null);
  }

  @Override
  protected Collection<? extends HasDateRange> initItems(SimpleRowSet data) {
    return null;
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
  protected void onFilterSelection(HasWidgets dataContainer, ChartData.Type dataType) {
    setFilter(FilterType.TENTATIVE);

    List<ChartData> allData = getFilterData();
    List<ChartData> tentativeData = prepareFilterData(FilterType.TENTATIVE);

    ChartData.Type typeOfSingleDataHavingSelection = null;
    for (ChartData data : allData) {
      if (data.hasSelection()) {
        if (typeOfSingleDataHavingSelection == null) {
          typeOfSingleDataHavingSelection = data.getType();
        } else {
          typeOfSingleDataHavingSelection = null;
          break;
        }
      }
    }

    for (ChartData data : allData) {
      ChartData.Type type = data.getType();

      FilterDataWidget dataWidget = FilterHelper.getDataWidget(dataContainer, type);
      if (dataWidget == null) {
        continue;
      }

      ChartData tentative = FilterHelper.getDataByType(tentativeData, data.getType());

      int size = data.getItems().size();
      boolean changed = false;

      for (int i = 0; i < size; i++) {
        ChartData.Item item = data.getItems().get(i);

        boolean enabled;
        if (typeOfSingleDataHavingSelection != null && typeOfSingleDataHavingSelection == type) {
          enabled = true;
        } else if (tentative == null) {
          enabled = false;
        } else {
          enabled = tentative.contains(item.getName());
        }

        boolean selected = item.isSelected();

        if (data.setItemEnabled(item, enabled)) {
          if (enabled) {
            dataWidget.addItem(item, i);
          } else {
            dataWidget.removeItem(i, selected);
          }

          changed = true;
        }
      }

      if (changed) {
        dataWidget.refresh();
      }
    }
  }

  @Override
  protected void prepareChart(Size canvasSize) {
    setNumberWidth(ChartHelper.getPixels(getSettings(), getNumberWidthColumnName(), 80,
        ChartHelper.DEFAULT_MOVER_WIDTH + 1, canvasSize.getWidth() / 3));

    if (isInfoColumnVisible()) {
      setInfoWidth(ChartHelper.getPixels(getSettings(), getInfoWidthColumnName(), 120,
          ChartHelper.DEFAULT_MOVER_WIDTH + 1, canvasSize.getWidth() / 3));
    } else {
      setInfoWidth(0);
    }

    setChartLeft(getNumberWidth() + getInfoWidth());
    setChartWidth(canvasSize.getWidth() - getChartLeft() - getChartRight());

    setDayColumnWidth(ChartHelper.getPixels(getSettings(), getDayWidthColumnName(), 20,
        1, getChartWidth()));

    boolean sc = ChartHelper.getBoolean(getSettings(), getSeparateCargoColumnName());
    if (separateCargo() != sc) {
      setSeparateCargo(sc);
      updateMaxRange();
    }
  }

  @Override
  protected void renderContent(ComplexPanel panel) {
    renderContentInit();

    List<ChartRowLayout> vehicleLayout = doLayout();

    int rc = ChartRowLayout.countRows(vehicleLayout, 1);
    initContent(panel, rc);

    if (vehicleLayout.isEmpty()) {
      return;
    }

    int calendarWidth = getCalendarWidth();

    Double opacity = ChartHelper.getOpacity(getSettings(), getItemOpacityColumnName());

    Edges margins = new Edges();
    margins.setBottom(ChartHelper.ROW_SEPARATOR_HEIGHT);

    Widget offWidget;
    Widget itemWidget;
    Widget overlapWidget;

    int rowIndex = 0;
    for (ChartRowLayout layout : vehicleLayout) {

      int vehicleIndex = layout.getDataIndex();

      int size = layout.getSize(1);
      int lastRow = rowIndex + size - 1;

      int top = rowIndex * getRowHeight();

      if (rowIndex > 0) {
        ChartHelper.addRowSeparator(panel, STYLE_VEHICLE_ROW_SEPARATOR, top, 0,
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
        ChartHelper.apply(offWidget, rectangle, margins);

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
            ChartHelper.apply(itemWidget, rectangle, margins);
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
              ChartHelper.apply(overlapWidget, rectangle, margins);

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

  protected void renderInfoCell(ChartRowLayout layout, Vehicle vehicle, ComplexPanel panel,
      int firstRow, int lastRow) {
    IdentifiableWidget infoWidget = createInfoWidget(vehicle, layout.hasOverlap());
    addInfoWidget(panel, infoWidget, firstRow, lastRow);
  }

  @Override
  protected void renderMovers(ComplexPanel panel, int height) {
    Mover numberMover = ChartHelper.createHorizontalMover();
    StyleUtils.setLeft(numberMover, getNumberWidth() - ChartHelper.DEFAULT_MOVER_WIDTH);
    StyleUtils.setHeight(numberMover, height);

    numberMover.addMoveHandler(new MoveEvent.Handler() {
      @Override
      public void onMove(MoveEvent event) {
        onNumberResize(event);
      }
    });

    panel.add(numberMover);

    if (isInfoColumnVisible()) {
      Mover infoMover = ChartHelper.createHorizontalMover();
      StyleUtils.setLeft(infoMover, getChartLeft() - ChartHelper.DEFAULT_MOVER_WIDTH);
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
      ChartHelper.addRowSeparator(panel, (rowIndex + 1) * getRowHeight(), getChartLeft(),
          getCalendarWidth());
    }
  }

  @Override
  protected boolean setData(ResponseObject response) {
    boolean ok = super.setData(response);
    if (!ok) {
      return ok;
    }

    boolean filtered = setFilter(FilterType.TENTATIVE);
    if (filtered) {
      persistFilter();
    }

    return ok;
  }

  private void addNumberWidget(HasWidgets panel, IdentifiableWidget widget,
      int firstRow, int lastRow) {

    Rectangle rectangle = ChartHelper.getRectangle(0, getNumberWidth(), firstRow, lastRow,
        getRowHeight());

    Edges margins = new Edges();
    margins.setRight(ChartHelper.DEFAULT_MOVER_WIDTH);
    margins.setBottom(ChartHelper.ROW_SEPARATOR_HEIGHT);

    ChartHelper.apply(widget.asWidget(), rectangle, margins);

    panel.add(widget.asWidget());

    numberPanels.add(widget.getId());
  }

  private Widget createFreightWidget(Freight freight) {
    Flow panel = new Flow();
    panel.addStyleName(STYLE_FREIGHT_PANEL);
    setItemWidgetColor(freight, panel);
    
    panel.setTitle(freight.getCargoAndTripTitle());

    bindCargoOpener(freight, panel);

    DndHelper.makeSource(panel, DATA_TYPE_FREIGHT, freight, STYLE_FREIGHT_DRAG);
    freight.makeTarget(panel, STYLE_FREIGHT_DRAG_OVER);
    
    renderCargoShipment(panel, freight, freight.getTripTitle());

    return panel;
  }

  private IdentifiableWidget createInfoWidget(Vehicle vehicle, boolean hasOverlap) {
    Simple panel = new Simple();
    panel.addStyleName(STYLE_INFO_PANEL);
    if (hasOverlap) {
      panel.addStyleName(STYLE_INFO_OVERLAP);
    }

    final Long vehicleId = vehicle.getId();

    BeeLabel label = new BeeLabel(vehicle.getInfo());
    label.addStyleName(STYLE_INFO_LABEL);

    UiHelper.maybeSetTitle(label, vehicle.getTitle());

    label.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        openDataRow(event, VIEW_VEHICLES, vehicleId);
      }
    });

    panel.add(label);

    return panel;
  }

  private IdentifiableWidget createNumberWidget(Vehicle vehicle, boolean hasOverlap) {
    Simple panel = new Simple();
    panel.addStyleName(STYLE_NUMBER_PANEL);
    if (hasOverlap) {
      panel.addStyleName(STYLE_NUMBER_OVERLAP);
    }

    final Long vehicleId = vehicle.getId();

    DndDiv label = new DndDiv(STYLE_NUMBER_LABEL);
    label.setText(vehicle.getNumber());

    UiHelper.maybeSetTitle(label, vehicle.getTitle());

    label.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        openDataRow(event, VIEW_VEHICLES, vehicleId);
      }
    });

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

    final Long tripId = trip.getTripId();

    panel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        openDataRow(event, VIEW_TRIPS, tripId);
      }
    });

    DndHelper.makeSource(panel, DATA_TYPE_TRIP, trip, STYLE_TRIP_DRAG);
    trip.makeTarget(panel, STYLE_TRIP_DRAG_OVER);

    Range<JustDate> tripRange =
        ChartHelper.normalizedIntersection(trip.getRange(), getVisibleRange());
    if (tripRange == null) {
      return panel;
    }

    List<Range<JustDate>> voidRanges;

    if (freights.containsKey(tripId)) {
      Multimap<JustDate, CargoEvent> tripLayout = splitTripByDate(tripId, tripRange);
      Set<JustDate> eventDates = tripLayout.keySet();

      for (JustDate date : eventDates) {
        Multimap<Long, CargoEvent> dayLayout = CargoEvent.splitByCountry(tripLayout.get(date));
        if (!dayLayout.isEmpty()) {
          Widget dayWidget = createShipmentDayPanel(dayLayout, trip.getTitle());

          StyleUtils.setLeft(dayWidget, getRelativeLeft(tripRange, date));
          StyleUtils.setWidth(dayWidget, getDayColumnWidth());

          panel.add(dayWidget);
        }
      }

      voidRanges = getVoidRanges(tripRange, eventDates, freights.get(tripId));

    } else {
      voidRanges = Lists.newArrayList();
      voidRanges.add(tripRange);
    }

    for (Range<JustDate> voidRange : voidRanges) {
      Widget voidWidget = new CustomDiv(STYLE_TRIP_VOID);

      StyleUtils.setLeft(voidWidget, getRelativeLeft(tripRange, voidRange.lowerEndpoint()));
      StyleUtils.setWidth(voidWidget, ChartHelper.getSize(voidRange) * getDayColumnWidth());

      panel.add(voidWidget);
    }

    return panel;
  }

  private List<ChartRowLayout> doLayout() {
    List<ChartRowLayout> result = Lists.newArrayList();
    Range<JustDate> range = getVisibleRange();

    for (int vehicleIndex = 0; vehicleIndex < vehicles.size(); vehicleIndex++) {
      Vehicle vehicle = vehicles.get(vehicleIndex);
      Long vehicleId = vehicle.getId();

      if (ChartHelper.isActive(vehicle, range) && vehicle.matched(FilterType.PERSISTENT)) {
        ChartRowLayout layout = new ChartRowLayout(vehicleIndex);

        Collection<Trip> vehicleTrips = getTripsForLayout(vehicleId,
            separateCargo() ? null : range);

        for (Trip trip : vehicleTrips) {
          if (separateCargo()) {
            List<Freight> tripFreights = getFreightsForLayout(trip.getTripId(), range);
            if (!tripFreights.isEmpty()) {
              layout.addItems(getGroupIdForFreightLayout(trip), tripFreights, range,
                  ChartRowLayout.FREIGHT_BLENDER);
            }

          } else {
            layout.addItem(getGroupIdForTripLayout(trip), trip, range, null);
          }
        }

        if (layout.isEmpty() && !layoutIdleVehicles()) {
          continue;
        }

        layout.addInactivity(ChartHelper.getInactivity(vehicle, range), range);
        if (services.containsKey(vehicleId)) {
          layout.addInactivity(ChartHelper.getActiveItems(services.get(vehicleId), range), range);
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
      if (isFiltered() && !freight.matched(FilterType.PERSISTENT)) {
        continue;
      }

      if (range != null && !BeeUtils.intersects(freight.getRange(), range)) {
        continue;
      }
      result.add(freight);
    }

    return result;
  }

  private List<Trip> getTripsForLayout(Long vehicleId, Range<JustDate> range) {
    List<Trip> result = Lists.newArrayList();
    if (!trips.containsKey(vehicleId)) {
      return result;
    }

    for (Trip trip : trips.get(vehicleId)) {
      if (isFiltered() && !trip.matched(FilterType.PERSISTENT)) {
        continue;
      }

      if (range != null && !BeeUtils.intersects(trip.getRange(), range)) {
        continue;
      }
      result.add(trip);
    }

    return result;
  }

  private List<Range<JustDate>> getVoidRanges(Range<JustDate> tripRange,
      Set<JustDate> eventDates, Collection<Freight> tripFreights) {

    List<Range<JustDate>> result = Lists.newArrayList();
    int tripDays = ChartHelper.getSize(tripRange);

    Set<JustDate> usedDates = Sets.newHashSet();

    if (!BeeUtils.isEmpty(eventDates)) {
      if (eventDates.size() >= tripDays) {
        return result;
      }
      usedDates.addAll(eventDates);
    }

    if (!BeeUtils.isEmpty(tripFreights)) {
      for (Freight freight : tripFreights) {
        if (ChartHelper.isActive(freight, tripRange)) {
          Range<JustDate> freightRange =
              ChartHelper.normalizedIntersection(freight.getRange(), tripRange);
          if (freightRange == null) {
            continue;
          }

          int freightDays = ChartHelper.getSize(freightRange);
          if (freightDays >= tripDays) {
            return result;
          }

          for (int i = 0; i < freightDays; i++) {
            usedDates.add(TimeUtils.nextDay(freightRange.lowerEndpoint(), i));
          }

          if (usedDates.size() >= tripDays) {
            return result;
          }
        }
      }
    }

    if (BeeUtils.isEmpty(usedDates)) {
      result.add(tripRange);
      return result;
    }

    List<JustDate> dates = Lists.newArrayList(usedDates);
    Collections.sort(dates);

    for (int i = 0; i < dates.size(); i++) {
      JustDate date = dates.get(i);

      if (i == 0 && TimeUtils.isMore(date, tripRange.lowerEndpoint())) {
        result.add(Range.closed(tripRange.lowerEndpoint(), TimeUtils.previousDay(date)));
      }

      if (i > 0 && TimeUtils.dayDiff(dates.get(i - 1), date) > 1) {
        result.add(Range.closed(TimeUtils.nextDay(dates.get(i - 1)), TimeUtils.previousDay(date)));
      }

      if (i == dates.size() - 1 && TimeUtils.isLess(date, tripRange.upperEndpoint())) {
        result.add(Range.closed(TimeUtils.nextDay(date), tripRange.upperEndpoint()));
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
      int infoPx = newLeft - getNumberWidth() + ChartHelper.DEFAULT_MOVER_WIDTH;

      if (newLeft != oldLeft) {
        StyleUtils.setLeft(resizer, newLeft);

        for (String id : infoPanels) {
          StyleUtils.setWidth(id, infoPx - ChartHelper.DEFAULT_MOVER_WIDTH);
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
      maxLeft = getChartLeft() - ChartHelper.DEFAULT_MOVER_WIDTH * 2 - 1;
    } else {
      maxLeft = getLastResizableColumnMaxLeft(0);
    }

    int newLeft = BeeUtils.clamp(oldLeft + delta, 1, maxLeft);

    if (newLeft != oldLeft || event.isFinished()) {
      int numberPx = newLeft + ChartHelper.DEFAULT_MOVER_WIDTH;
      int infoPx = isInfoColumnVisible() ? getChartLeft() - numberPx : BeeConst.UNDEF;

      if (newLeft != oldLeft) {
        StyleUtils.setLeft(resizer, newLeft);

        for (String id : numberPanels) {
          StyleUtils.setWidth(id, numberPx - ChartHelper.DEFAULT_MOVER_WIDTH);
        }

        if (isInfoColumnVisible()) {
          for (String id : infoPanels) {
            Element element = Document.get().getElementById(id);
            if (element != null) {
              StyleUtils.setLeft(element, numberPx);
              StyleUtils.setWidth(element, infoPx - ChartHelper.DEFAULT_MOVER_WIDTH);
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

  private void persistFilter() {
    for (Vehicle vehicle : vehicles) {
      vehicle.persistFilter();
    }

    for (Trip trip : trips.values()) {
      trip.persistFilter();
    }

    for (Freight freight : freights.values()) {
      freight.persistFilter();
    }
  }

  private List<ChartData> prepareFilterData(FilterType filterType) {
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
      if (filterType != null && !vehicle.matched(filterType)) {
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
        if (filterType != null && !trip.matched(filterType)) {
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
            driverData.add(driver.getItemName(), driver.getId());
          }
        }

        if (!freights.containsKey(trip.getTripId())) {
          continue;
        }

        for (Freight freight : freights.get(trip.getTripId())) {
          if (filterType != null && !freight.matched(filterType)) {
            continue;
          }

          customerData.add(freight.getCustomerName(), freight.getCustomerId());
          orderData.add(freight.getOrderName(), freight.getOrderId());

          OrderStatus status = freight.getOrderStatus();
          if (status != null) {
            statusData.add(status.getCaption(), (long) status.ordinal());
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

          cargoData.add(freight.getCargoDescription(), freight.getCargoId());

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

    for (ChartData cd : data) {
      cd.prepare();
    }

    return FilterHelper.notEmptyData(data);
  }

  private void resetFilter(FilterType filterType) {
    for (Vehicle vehicle : vehicles) {
      vehicle.setMatch(filterType, true);
    }

    for (Trip trip : trips.values()) {
      trip.setMatch(filterType, true);
    }

    for (Freight freight : freights.values()) {
      freight.setMatch(filterType, true);
    }
  }

  private boolean separateCargo() {
    return separateCargo;
  }

  private boolean setFilter(FilterType filterType) {
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

    ChartData customerData = FilterHelper.getDataByType(selectedData, ChartData.Type.CUSTOMER);
    ChartData orderData = FilterHelper.getDataByType(selectedData, ChartData.Type.ORDER);
    ChartData statusData = FilterHelper.getDataByType(selectedData, ChartData.Type.ORDER_STATUS);

    ChartData cargoData = FilterHelper.getDataByType(selectedData, ChartData.Type.CARGO);

    ChartData tripData = FilterHelper.getDataByType(selectedData, ChartData.Type.TRIP);
    ChartData driverData = FilterHelper.getDataByType(selectedData, ChartData.Type.DRIVER);

    ChartData loadData = FilterHelper.getDataByType(selectedData, ChartData.Type.LOADING);
    ChartData unloadData = FilterHelper.getDataByType(selectedData, ChartData.Type.UNLOADING);
    ChartData placeData = FilterHelper.getDataByType(selectedData, ChartData.Type.PLACE);

    boolean checkLoad = loadData != null || placeData != null;
    boolean checkUnload = unloadData != null || placeData != null;

    boolean freightRequired = customerData != null || orderData != null || statusData != null
        || cargoData != null || checkLoad || checkUnload;
    boolean tripRequired = freightRequired || otherVehicleData != null || tripData != null
        || driverData != null;

    for (Vehicle vehicle : vehicles) {
      boolean vehicleMatch = vehicle.filter(filterType, selectedData);
      if (vehicleMatch && vehicleData != null) {
        vehicleMatch = vehicleData.contains(vehicle.getId());
      }

      boolean hasTrips = trips.containsKey(vehicle.getId());
      if (vehicleMatch && !hasTrips && tripRequired) {
        vehicleMatch = false;
      }

      if (vehicleMatch && hasTrips) {
        int tripCount = 0;

        for (Trip trip : trips.get(vehicle.getId())) {
          boolean tripMatch = trip.filter(filterType, selectedData);

          if (tripMatch && otherVehicleData != null) {
            Long otherVehicleId = trip.getVehicleId(otherVehicleType);
            tripMatch = otherVehicleId != null && otherVehicleData.contains(otherVehicleId);
          }

          boolean hasFreights = freights.containsKey(trip.getTripId());
          if (tripMatch && !hasFreights && freightRequired) {
            tripMatch = false;
          }

          if (tripMatch && hasFreights) {
            int freightCount = 0;

            for (Freight freight : freights.get(trip.getTripId())) {
              boolean freightMatch = freight.filter(filterType, selectedData);

              if (freightMatch && (checkLoad || checkUnload)) {
                boolean ok = false;
                String info;

                if (checkLoad) {
                  info = Places.getLoadingPlaceInfo(freight);
                  ok = FilterHelper.containsName(loadData, info)
                      || FilterHelper.containsName(placeData, info);
                }
                if (!ok && checkUnload) {
                  info = Places.getUnloadingPlaceInfo(freight);
                  ok = FilterHelper.containsName(unloadData, info)
                      || FilterHelper.containsName(placeData, info);
                }

                if (!ok && hasCargoHandling(freight.getCargoId())) {
                  for (CargoHandling ch : getCargoHandling(freight.getCargoId())) {
                    if (checkLoad) {
                      info = Places.getLoadingPlaceInfo(ch);
                      ok = FilterHelper.containsName(loadData, info)
                          || FilterHelper.containsName(placeData, info);
                    }
                    if (!ok && checkUnload) {
                      info = Places.getUnloadingPlaceInfo(ch);
                      ok = FilterHelper.containsName(unloadData, info)
                          || FilterHelper.containsName(placeData, info);
                    }

                    if (ok) {
                      break;
                    }
                  }
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

  private void setInfoWidth(int infoWidth) {
    this.infoWidth = infoWidth;
  }

  private void setNumberWidth(int numberWidth) {
    this.numberWidth = numberWidth;
  }

  private void setSeparateCargo(boolean separateCargo) {
    this.separateCargo = separateCargo;
  }

  private Multimap<JustDate, CargoEvent> splitTripByDate(Long tripId, Range<JustDate> range) {
    Multimap<JustDate, CargoEvent> result = ArrayListMultimap.create();
    if (tripId == null || range == null || range.isEmpty() || !freights.containsKey(tripId)) {
      return result;
    }

    Collection<Freight> tripCargos = freights.get(tripId);
    for (Freight freight : tripCargos) {
      result.putAll(splitCargoByDate(freight, range));
    }

    return result;
  }
}
