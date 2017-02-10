package com.butent.bee.client.modules.transport.charts;

import com.google.common.collect.Sets;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HasWidgets;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.transport.TransportHandler;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.client.timeboard.TimeBoardRowLayout;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.transport.TransportConstants.VehicleType;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class ShippingSchedule extends VehicleTimeBoard {

  static final String SUPPLIER_KEY = "shipping_schedule";
  private static final String DATA_SERVICE = SVC_GET_SS_DATA;

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "tr-ss-";

  private static final String STYLE_TRIP_GROUP_PREFIX = STYLE_PREFIX + "trip-Group-";
  private static final String STYLE_TRIP_GROUP_ROW_SEPARATOR = STYLE_TRIP_GROUP_PREFIX + "row-sep";
  private static final String STYLE_TRIP_GROUP_PANEL = STYLE_TRIP_GROUP_PREFIX + "panel";
  private static final String STYLE_TRIP_GROUP_LABEL = STYLE_TRIP_GROUP_PREFIX + "label";
  private static final String STYLE_TRIP_GROUP_TRAILER = STYLE_TRIP_GROUP_PREFIX + "trailer";
  private static final String STYLE_TRIP_GROUP_OVERLAP = STYLE_TRIP_GROUP_PREFIX + "overlap";
  private static final String STYLE_TRIP_GROUP_DRAG_OVER = STYLE_TRIP_GROUP_PREFIX + "dragOver";

  private static final Set<String> SETTINGS_COLUMNS_TRIGGERING_REFRESH =
      Sets.newHashSet(COL_SS_MIN_DATE, COL_SS_MAX_DATE,
          COL_SS_TRANSPORT_GROUPS, COL_SS_COMPLETED_TRIPS);

  static void open(final ViewCallback callback) {
    BeeKeeper.getRpc().makePostRequest(TransportHandler.createArgs(DATA_SERVICE),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            ShippingSchedule ss = new ShippingSchedule();
            ss.onCreate(response, callback);
          }
        });
  }

  private boolean separateTrips;

  private final Map<Integer, Long> tripsByRow = new HashMap<>();

  private ShippingSchedule() {
    super();
    addStyleName(STYLE_PREFIX + "View");
  }

  @Override
  public String getCaption() {
    return Localized.dictionary().shippingSchedule();
  }

  @Override
  public String getIdPrefix() {
    return "tr-ss";
  }

  @Override
  public String getSupplierKey() {
    return SUPPLIER_KEY;
  }

  @Override
  public void handleAction(Action action) {
    if (Action.ADD.equals(action)) {
      RowFactory.createRow(VIEW_TRIPS, Modality.DISABLED);
    } else {
      super.handleAction(action);
    }
  }

  @Override
  protected BeeRow createNewTripRow(DataInfo dataInfo, int rowIndex, JustDate date) {
    BeeRow newRow = super.createNewTripRow(dataInfo, rowIndex, date);

    if (tripsByRow.containsKey(rowIndex)) {
      Trip trip = findTripById(tripsByRow.get(rowIndex));

      if (trip != null && trip.getTrailerId() != null) {
        newRow.setValue(dataInfo.getColumnIndex(VehicleType.TRAILER.getTripVehicleIdColumnName()),
            trip.getTrailerId());
        newRow.setValue(dataInfo.getColumnIndex(VehicleType.TRAILER
            .getTripVehicleNumberColumnName()), trip.getTrailerNumber());
      }
    }
    return newRow;
  }

  @Override
  protected String getAdditionalInfo(Trip trip) {
    return BeeUtils.joinItems(trip.getCustomerNames(), trip.getDriverNames());
  }

  @Override
  protected String getDataService() {
    return DATA_SERVICE;
  }

  @Override
  protected String getDataType() {
    return DATA_TYPE_TRUCK;
  }

  @Override
  protected String getDayWidthColumnName() {
    return COL_SS_PIXELS_PER_DAY;
  }

  @Override
  protected String getFilterDataTypesColumnName() {
    return COL_SS_FILTER_DATA_TYPES;
  }

  @Override
  protected String getFiltersColumnName() {
    return COL_SS_FILTERS;
  }

  @Override
  protected String getFooterHeightColumnName() {
    return COL_SS_FOOTER_HEIGHT;
  }

  @Override
  protected Long getGroupIdForFreightLayout(Trip trip) {
    return separateTrips() ? trip.getTripId() : trip.getTruckId();
  }

  @Override
  protected Long getGroupIdForTripLayout(Trip trip) {
    return separateTrips() ? trip.getTripId() : trip.getTruckId();
  }

  @Override
  protected String getHeaderHeightColumnName() {
    return COL_SS_HEADER_HEIGHT;
  }

  @Override
  protected String getInfoWidthColumnName() {
    return COL_SS_PIXELS_PER_TRIP;
  }

  @Override
  protected String getItemOpacityColumnName() {
    return COL_SS_ITEM_OPACITY;
  }

  @Override
  protected String getNumberWidthColumnName() {
    return COL_SS_PIXELS_PER_TRUCK;
  }

  @Override
  protected String getRefreshLocalChangesColumnName() {
    return COL_SS_REFRESH_LOCAL_CHANGES;
  }

  @Override
  protected String getRefreshRemoteChangesColumnName() {
    return COL_SS_REFRESH_REMOTE_CHANGES;
  }

  @Override
  protected String getRowHeightColumnName() {
    return COL_SS_PIXELS_PER_ROW;
  }

  @Override
  protected String getSeparateCargoColumnName() {
    return COL_SS_SEPARATE_CARGO;
  }

  @Override
  protected Collection<String> getSettingsColumnsTriggeringRefresh() {
    return SETTINGS_COLUMNS_TRIGGERING_REFRESH;
  }

  @Override
  protected String getSettingsFormName() {
    return FORM_SS_SETTINGS;
  }

  @Override
  protected String getShowAdditionalInfoColumnName() {
    return COL_SS_ADDITIONAL_INFO;
  }

  @Override
  protected String getShowCountryFlagsColumnName() {
    return COL_SS_COUNTRY_FLAGS;
  }

  @Override
  protected String getShowOrderCustomerColumnName() {
    return COL_SS_ORDER_CUSTOMER;
  }

  @Override
  protected String getShowOderNoColumnName() {
    return COL_SS_ORDER_NO;
  }

  @Override
  protected String getShowPlaceInfoColumnName() {
    return COL_SS_PLACE_INFO;
  }

  @Override
  protected String getShowPlaceCitiesColumnName() {
    return COL_SS_PLACE_CITIES;
  }

  @Override
  protected String getShowPlaceCodesColumnName() {
    return COL_SS_PLACE_CODES;
  }

  @Override
  protected String getStripOpacityColumnName() {
    return COL_SS_STRIP_OPACITY;
  }

  @Override
  protected String getThemeColumnName() {
    return COL_SS_THEME;
  }

  @Override
  protected boolean isInfoColumnVisible() {
    return separateTrips();
  }

  @Override
  protected boolean layoutIdleVehicles() {
    return false;
  }

  @Override
  protected void prepareChart(Size canvasSize) {
    setSeparateTrips(TimeBoardHelper.getBoolean(getSettings(), COL_SS_SEPARATE_TRIPS));
    super.prepareChart(canvasSize);
  }

  @Override
  protected void renderContentInit() {
    super.renderContentInit();
    tripsByRow.clear();
  }

  @Override
  protected void renderInfoCell(TimeBoardRowLayout layout, Vehicle vehicle, ComplexPanel panel,
      int firstRow, int lastRow) {

    for (TimeBoardRowLayout.GroupLayout group : layout.getGroups()) {
      Trip trip = findTripById(group.getGroupId());

      if (trip != null) {
        IdentifiableWidget tripGroupWidget = createTripGroupWidget(trip, group.hasOverlap());
        addTripGroupWidget(panel, tripGroupWidget, trip.getTripId(),
            firstRow + group.getFirstRow(), firstRow + group.getLastRow());
      }
    }
  }

  @Override
  protected void renderRowSeparators(ComplexPanel panel, int firstRow, int lastRow) {
    Long lastTrip = tripsByRow.get(firstRow);

    for (int rowIndex = firstRow + 1; rowIndex <= lastRow; rowIndex++) {
      int top = rowIndex * getRowHeight();
      Long currentTrip = tripsByRow.get(rowIndex);

      if (Objects.equals(lastTrip, currentTrip)) {
        TimeBoardHelper.addRowSeparator(panel, top, getChartLeft(), getCalendarWidth());
      } else {
        TimeBoardHelper.addRowSeparator(panel, STYLE_TRIP_GROUP_ROW_SEPARATOR, top,
            getNumberWidth(), getInfoWidth() + getCalendarWidth());
        lastTrip = currentTrip;
      }
    }
  }

  @Override
  protected void updateMaxRange() {
    super.updateMaxRange();
    clampMaxRange(COL_SS_MIN_DATE, COL_SS_MAX_DATE);
  }

  private void addTripGroupWidget(HasWidgets panel, IdentifiableWidget widget, Long tripId,
      int firstRow, int lastRow) {
    addInfoWidget(panel, widget, firstRow, lastRow);

    for (int row = firstRow; row <= lastRow; row++) {
      tripsByRow.put(row, tripId);
    }
  }

  private static IdentifiableWidget createTripGroupWidget(Trip trip, boolean hasOverlap) {
    Flow panel = new Flow();
    panel.addStyleName(STYLE_TRIP_GROUP_PANEL);
    if (hasOverlap) {
      panel.addStyleName(STYLE_TRIP_GROUP_OVERLAP);
    }

    if (trip.isEditable()) {
      trip.makeTarget(panel, STYLE_TRIP_GROUP_DRAG_OVER);
    }

    Label label = new Label(trip.getTripNo());
    label.addStyleName(STYLE_TRIP_GROUP_LABEL);

    label.setTitle(trip.getTitle());

    bindOpener(label, VIEW_TRIPS, trip.getTripId());

    panel.add(label);

    if (trip.getTrailerId() != null) {
      Label trailer = new Label(trip.getTrailerNumber());
      trailer.addStyleName(STYLE_TRIP_GROUP_TRAILER);

      bindOpener(trailer, VIEW_VEHICLES, trip.getTrailerId());

      panel.add(trailer);
    }

    return panel;
  }

  private boolean separateTrips() {
    return separateTrips;
  }

  private void setSeparateTrips(boolean separateTrips) {
    this.separateTrips = separateTrips;
  }
}
