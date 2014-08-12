package com.butent.bee.client.modules.transport.charts;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.modules.transport.TransportHandler;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.i18n.Localized;

final class TruckTimeBoard extends VehicleTimeBoard {

  static final String SUPPLIER_KEY = "truck_time_board";
  private static final String DATA_SERVICE = SVC_GET_TRUCK_TB_DATA;

  static void open(final ViewCallback callback) {
    BeeKeeper.getRpc().makePostRequest(TransportHandler.createArgs(DATA_SERVICE),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            TruckTimeBoard ss = new TruckTimeBoard();
            ss.onCreate(response, callback);
          }
        });
  }

  private TruckTimeBoard() {
    super();
  }

  @Override
  public String getCaption() {
    return Localized.getConstants().truckTimeBoard();
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
  protected String getRowHeightColumnName() {
    return COL_TRUCK_PIXELS_PER_ROW;
  }

  @Override
  protected String getSeparateCargoColumnName() {
    return COL_TRUCK_SEPARATE_CARGO;
  }

  @Override
  protected String getSettingsFormName() {
    return FORM_TRUCK_SETTINGS;
  }

  @Override
  protected String getShowCountryFlagsColumnName() {
    return COL_TRUCK_COUNTRY_FLAGS;
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
}
