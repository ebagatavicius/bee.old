package com.butent.bee.client.modules.transport.charts;

import com.google.common.collect.Range;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;

class CargoHandling implements HasDateRange, HasShipmentInfo {

  private final Long cargoTripId;
  private final boolean unloading;

  private final JustDate date;
  private final Long country;
  private final String place;
  private final String postIndex;
  private final Long city;
  private final String number;

  private final Range<JustDate> range;

  CargoHandling(SimpleRow row) {
    this.cargoTripId = row.getLong(COL_CARGO_TRIP);
    this.unloading = row.isTrue(VAR_UNLOADING);

    this.date = Places.getDate(row, COL_PLACE_DATE);
    this.country = row.getLong(COL_PLACE_COUNTRY);
    this.place = row.getValue(COL_PLACE_ADDRESS);
    this.postIndex = row.getValue(COL_PLACE_POST_INDEX);
    this.city = row.getLong(COL_PLACE_CITY);
    this.number = row.getValue(COL_PLACE_NUMBER);

    this.range = TimeBoardHelper.getActivity(unloading ? null : this.date,
        unloading ? this.date : null);
  }

  @Override
  public Long getLoadingCountry() {
    return unloading ? null : country;
  }

  @Override
  public JustDate getLoadingDate() {
    return unloading ? null : date;
  }

  @Override
  public String getLoadingPlace() {
    return unloading ? null : place;
  }

  @Override
  public String getLoadingPostIndex() {
    return unloading ? null : postIndex;
  }

  @Override
  public Long getLoadingCity() {
    return unloading ? null : city;
  }

  @Override
  public String getLoadingNumber() {
    return unloading ? null : number;
  }

  @Override
  public Range<JustDate> getRange() {
    return range;
  }

  @Override
  public Long getUnloadingCountry() {
    return unloading ? country : null;
  }

  @Override
  public JustDate getUnloadingDate() {
    return unloading ? date : null;
  }

  @Override
  public String getUnloadingPlace() {
    return unloading ? place : null;
  }

  @Override
  public String getUnloadingPostIndex() {
    return unloading ? postIndex : null;
  }

  @Override
  public Long getUnloadingCity() {
    return unloading ? city : null;
  }

  @Override
  public String getUnloadingNumber() {
    return unloading ? number : null;
  }

  Long getCargoTripId() {
    return cargoTripId;
  }

  static String getTitle(String loadInfo, String unloadInfo) {
    return TimeBoardHelper.buildTitle(Localized.dictionary().intermediateLoading(), loadInfo,
        Localized.dictionary().intermediateUnloading(), unloadInfo);
  }

  boolean isLoading() {
    return !unloading;
  }

  boolean isUnloading() {
    return unloading;
  }
}
