package com.butent.bee.client.modules.transport.charts;

import com.google.common.collect.Sets;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.modules.transport.TransportHandler;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Set;

final class TruckTimeBoard extends VehicleTimeBoard {

  static final String SUPPLIER_KEY = "truck_time_board";
  private static final String DATA_SERVICE = SVC_GET_TRUCK_TB_DATA;
  private static final String FILTER_DATA_SERVICE = SVC_GET_TRUCK_TB_FILTER_DATA;

  private static final Set<String> SETTINGS_COLUMNS_TRIGGERING_REFRESH =
      Sets.newHashSet(COL_TRUCK_MIN_DATE, COL_TRUCK_MAX_DATE,
          COL_TRUCK_TRANSPORT_GROUPS, COL_TRUCK_COMPLETED_TRIPS, COL_FILTER_DEPENDS_ON_DATA);

  static void open(final ViewCallback callback) {
    BeeKeeper.getRpc().makePostRequest(TransportHandler.createArgs(SVC_GET_SETTINGS),
        settingsResponse -> {
          if (!settingsResponse.hasErrors()) {
            TruckTimeBoard ttb = new TruckTimeBoard(settingsResponse);

            ttb.requestData(response -> ttb.onCreate(response, callback));
          }
        });
  }

  private TruckTimeBoard(ResponseObject settingsResponse) {
    super(settingsResponse);
  }

  @Override
  public String getCaption() {
    return Localized.dictionary().truckTimeBoard();
  }

  @Override
  public String getIdPrefix() {
    return "truck-tb";
  }

  @Override
  public String getSupplierKey() {
    return SUPPLIER_KEY;
  }

  @Override
  protected String getAdditionalInfo(Trip trip) {
    return BeeUtils.joinItems(trip.getCustomerNames(), trip.getDriverNames(),
        trip.getTrailerNumber());
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
    return COL_TRUCK_PIXELS_PER_DAY;
  }

  @Override
  protected String getFilterDataTypesColumnName() {
    return COL_TRUCK_FILTER_DATA_TYPES;
  }

  @Override
  protected String getFiltersColumnName() {
    return COL_TRUCK_FILTERS;
  }

  @Override
  protected String getFilterService() {
    return FILTER_DATA_SERVICE;
  }

  @Override
  protected String getFooterHeightColumnName() {
    return COL_TRUCK_FOOTER_HEIGHT;
  }

  @Override
  protected String getHeaderHeightColumnName() {
    return COL_TRUCK_HEADER_HEIGHT;
  }

  @Override
  protected String getInfoWidthColumnName() {
    return COL_TRUCK_PIXELS_PER_INFO;
  }

  @Override
  protected String getItemOpacityColumnName() {
    return COL_TRUCK_ITEM_OPACITY;
  }

  @Override
  protected String getNumberWidthColumnName() {
    return COL_TRUCK_PIXELS_PER_NUMBER;
  }

  @Override
  protected String getRefreshLocalChangesColumnName() {
    return COL_TRUCK_REFRESH_LOCAL_CHANGES;
  }

  @Override
  protected String getRefreshRemoteChangesColumnName() {
    return COL_TRUCK_REFRESH_REMOTE_CHANGES;
  }

  @Override
  protected String getRowHeightColumnName() {
    return COL_TRUCK_PIXELS_PER_ROW;
  }

  @Override
  protected String getSeparateCargoColumnName() {
    return COL_TRUCK_SEPARATE_CARGO;
  }

  @Override
  protected Collection<String> getSettingsColumnsTriggeringRefresh() {
    return SETTINGS_COLUMNS_TRIGGERING_REFRESH;
  }

  @Override
  protected String getSettingsFormName() {
    return FORM_TRUCK_SETTINGS;
  }

  @Override
  protected String getSettingsMaxDate() {
    return COL_TRUCK_MAX_DATE;
  }

  @Override
  protected String getSettingsMinDate() {
    return COL_TRUCK_MIN_DATE;
  }

  @Override
  protected String getShowAdditionalInfoColumnName() {
    return COL_TRUCK_ADDITIONAL_INFO;
  }

  @Override
  protected String getShowCountryFlagsColumnName() {
    return COL_TRUCK_COUNTRY_FLAGS;
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
    return COL_TRUCK_PLACE_INFO;
  }

  @Override
  protected String getShowPlaceCitiesColumnName() {
    return COL_TRUCK_PLACE_CITIES;
  }

  @Override
  protected String getShowPlaceCodesColumnName() {
    return COL_TRUCK_PLACE_CODES;
  }

  @Override
  protected String getStripOpacityColumnName() {
    return COL_TRUCK_STRIP_OPACITY;
  }

  @Override
  protected String getThemeColumnName() {
    return COL_TRUCK_THEME;
  }

  @Override
  protected void updateMaxRange() {
    super.updateMaxRange();
    clampMaxRange(COL_TRUCK_MIN_DATE, COL_TRUCK_MAX_DATE);
  }
}
