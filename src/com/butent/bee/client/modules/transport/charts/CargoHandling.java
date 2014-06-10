package com.butent.bee.client.modules.transport.charts;

import com.google.common.collect.Range;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;

class CargoHandling implements HasDateRange, HasShipmentInfo {

  private static final String notesLabel =
      Data.getColumnLabel(VIEW_CARGO_HANDLING, COL_CARGO_HANDLING_NOTES);

  private final JustDate loadingDate;
  private final Long loadingCountry;
  private final String loadingPlace;
  private final String loadingPostIndex;
  private final Long loadingCity;
  private final String loadingNumber;

  private final JustDate unloadingDate;
  private final Long unloadingCountry;
  private final String unloadingPlace;
  private final String unloadingPostIndex;
  private final Long unloadingCity;
  private final String unloadingNumber;

  private final String notes;

  private final Range<JustDate> range;

  CargoHandling(SimpleRow row) {
    this.loadingDate = Places.getLoadingDate(row, loadingColumnAlias(COL_PLACE_DATE));
    this.loadingCountry = row.getLong(loadingColumnAlias(COL_PLACE_COUNTRY));
    this.loadingPlace = row.getValue(loadingColumnAlias(COL_PLACE_ADDRESS));
    this.loadingPostIndex = row.getValue(loadingColumnAlias(COL_PLACE_POST_INDEX));
    this.loadingCity = row.getLong(loadingColumnAlias(COL_PLACE_CITY));
    this.loadingNumber = row.getValue(loadingColumnAlias(COL_PLACE_NUMBER));

    this.unloadingDate = Places.getUnloadingDate(row, unloadingColumnAlias(COL_PLACE_DATE));
    this.unloadingCountry = row.getLong(unloadingColumnAlias(COL_PLACE_COUNTRY));
    this.unloadingPlace = row.getValue(unloadingColumnAlias(COL_PLACE_ADDRESS));
    this.unloadingPostIndex = row.getValue(unloadingColumnAlias(COL_PLACE_POST_INDEX));
    this.unloadingCity = row.getLong(unloadingColumnAlias(COL_PLACE_CITY));
    this.unloadingNumber = row.getValue(unloadingColumnAlias(COL_PLACE_NUMBER));

    this.notes = row.getValue(COL_CARGO_HANDLING_NOTES);

    this.range = TimeBoardHelper.getActivity(this.loadingDate, this.unloadingDate);
  }

  @Override
  public Long getLoadingCountry() {
    return loadingCountry;
  }

  @Override
  public JustDate getLoadingDate() {
    return loadingDate;
  }

  @Override
  public String getLoadingPlace() {
    return loadingPlace;
  }

  @Override
  public String getLoadingPostIndex() {
    return loadingPostIndex;
  }

  @Override
  public Long getLoadingCity() {
    return loadingCity;
  }

  @Override
  public String getLoadingNumber() {
    return loadingNumber;
  }

  @Override
  public Range<JustDate> getRange() {
    return range;
  }

  @Override
  public Long getUnloadingCountry() {
    return unloadingCountry;
  }

  @Override
  public JustDate getUnloadingDate() {
    return unloadingDate;
  }

  @Override
  public String getUnloadingPlace() {
    return unloadingPlace;
  }

  @Override
  public String getUnloadingPostIndex() {
    return unloadingPostIndex;
  }

  @Override
  public Long getUnloadingCity() {
    return unloadingCity;
  }

  @Override
  public String getUnloadingNumber() {
    return unloadingNumber;
  }

  String getTitle(String loadInfo, String unloadInfo) {
    return TimeBoardHelper.buildTitle(Localized.getConstants().intermediateLoading(), loadInfo,
        Localized.getConstants().intermediateUnloading(), unloadInfo,
        notesLabel, notes);
  }
}
