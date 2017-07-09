package com.butent.bee.client.modules.transport.charts;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.images.Flags;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;

final class Places {

  private static final Map<String, String> countryNames = new HashMap<>();
  private static final Map<String, String> countryCodes = new HashMap<>();

  private static Map<String, String> cities = new HashMap<>();

  static String getCountryFlag(Long countryId) {
    return (countryId == null) ? null : Flags.get(countryCodes.get(countryId.toString()));
  }

  static String getCountryLabel(Long countryId) {
    if (countryId == null) {
      return null;

    } else {
      String label = countryCodes.get(countryId.toString());
      return (label == null) ? countryNames.get(countryId.toString()) : label;
    }
  }

  static String getCityLabel(Long cityId) {
    if (cityId == null) {
      return null;
    } else {
      return cities.get(cityId.toString());
    }
  }

  static JustDate getDate(SimpleRow row, String colName) {
    return JustDate.get(row.getDateTime(colName));
  }

  static String getLoadingInfo(HasShipmentInfo item) {
    if (item == null) {
      return null;
    } else {
      return BeeUtils.joinWords(item.getLoadingDate(), getLoadingPlaceInfo(item));
    }
  }

  static String getLoadingPlaceInfo(HasShipmentInfo item) {
    if (item == null) {
      return null;
    } else {
      return getPlaceInfo(item.getLoadingCountry(), item.getLoadingPlace(),
          item.getLoadingPostIndex(), item.getLoadingCity(),
          item.getLoadingNumber());
    }
  }

  static String getPlaceInfo(Long countryId, String placeName, String postIndex, Long cityId,
      String number) {

    String countryLabel = getCountryLabel(countryId);
    String cityLabel = getCityLabel(cityId);

    if (BeeUtils.isEmpty(countryLabel) || BeeUtils.containsSame(placeName, countryLabel)
        || BeeUtils.containsSame(number, countryLabel)) {

      return BeeUtils.joinNoDuplicates(BeeConst.STRING_SPACE, placeName, postIndex, cityLabel,
          number);

    } else {
      return BeeUtils.joinNoDuplicates(BeeConst.STRING_SPACE, countryLabel, placeName, postIndex,
          cityLabel, number);
    }
  }

  static String getUnloadingInfo(HasShipmentInfo item) {
    if (item == null) {
      return null;
    } else {
      return BeeUtils.joinWords(item.getUnloadingDate(), getUnloadingPlaceInfo(item));
    }
  }

  static String getUnloadingPlaceInfo(HasShipmentInfo item) {
    if (item == null) {
      return null;
    } else {
      return getPlaceInfo(item.getUnloadingCountry(), item.getUnloadingPlace(),
          item.getUnloadingPostIndex(), item.getUnloadingCity(),
          item.getUnloadingNumber());
    }
  }

  static int setCountries(SimpleRowSet rowSet) {
    if (!DataUtils.isEmpty(rowSet)) {
      countryCodes.clear();
      countryNames.clear();

      for (SimpleRow row : rowSet) {
        String key = row.getValue(Data.getIdColumn(ClassifierConstants.VIEW_COUNTRIES));

        String code = row.getValue(ClassifierConstants.COL_COUNTRY_CODE);
        if (!BeeUtils.isEmpty(code)) {
          countryCodes.put(key, code.trim());
        }

        String name = row.getValue(ClassifierConstants.COL_COUNTRY_NAME);
        if (!BeeUtils.isEmpty(name)) {
          countryNames.put(key, name.trim());
        }
      }

      return rowSet.getNumberOfRows();

    } else {
      return 0;
    }
  }

  static int setCities(Map<String, String> map) {
    if (map != null) {
      Places.cities = map;
      return map.size();

    } else {
      return 0;
    }
  }

  private Places() {
  }
}
