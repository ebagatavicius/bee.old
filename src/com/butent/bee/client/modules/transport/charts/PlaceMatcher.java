package com.butent.bee.client.modules.transport.charts;

import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;

final class PlaceMatcher {

  static PlaceMatcher maybeCreate(Collection<ChartData> data) {
    if (BeeUtils.isEmpty(data)) {
      return null;
    }

    ChartData loadData = FilterHelper.getDataByType(data, ChartData.Type.LOADING);
    ChartData unloadData = FilterHelper.getDataByType(data, ChartData.Type.UNLOADING);
    ChartData placeData = FilterHelper.getDataByType(data, ChartData.Type.PLACE);

    if (BeeUtils.anyNotNull(loadData, unloadData, placeData)) {
      return new PlaceMatcher(loadData, unloadData, placeData);
    } else {
      return null;
    }
  }

  private final ChartData loadData;
  private final ChartData unloadData;
  private final ChartData placeData;

  private final boolean checkLoad;
  private final boolean checkUnload;

  private PlaceMatcher(ChartData loadData, ChartData unloadData, ChartData placeData) {
    this.loadData = loadData;
    this.unloadData = unloadData;
    this.placeData = placeData;

    this.checkLoad = loadData != null || placeData != null;
    this.checkUnload = unloadData != null || placeData != null;
  }

  boolean matches(HasShipmentInfo item) {
    if (item == null) {
      return false;
    }

    if (checkLoad) {
      String info = Places.getLoadingPlaceInfo(item);

      if (!BeeUtils.isEmpty(info)) {
        if (loadData != null && loadData.contains(info)) {
          return true;
        }
        if (placeData != null && placeData.contains(info)) {
          return true;
        }
      }
    }

    if (checkUnload) {
      String info = Places.getUnloadingPlaceInfo(item);

      if (!BeeUtils.isEmpty(info)) {
        if (unloadData != null && unloadData.contains(info)) {
          return true;
        }
        if (placeData != null && placeData.contains(info)) {
          return true;
        }
      }
    }

    return false;
  }

  boolean matchesAnyOf(Collection<? extends HasShipmentInfo> items) {
    if (items != null) {
      for (HasShipmentInfo item : items) {
        if (matches(item)) {
          return true;
        }
      }
    }
    return false;
  }
}
