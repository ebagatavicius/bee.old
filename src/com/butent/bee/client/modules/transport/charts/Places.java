package com.butent.bee.client.modules.transport.charts;

import com.butent.bee.client.images.Flags;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

final class Places {

  private static BeeRowSet countries;
  private static BeeRowSet cities;

  private static int countryCodeIndex = BeeConst.UNDEF;
  private static int countryNameIndex = BeeConst.UNDEF;
  private static int cityNameIndex = BeeConst.UNDEF;

  private static ValueType placeDateType = ValueType.DATE_TIME;

  static String getCountryFlag(Long countryId) {
    if (!DataUtils.isId(countryId) || DataUtils.isEmpty(countries)) {
      return null;
    }

    BeeRow row = countries.getRowById(countryId);
    if (row == null) {
      return null;
    }

    String code = row.getString(countryCodeIndex);
    if (BeeUtils.isEmpty(code)) {
      return null;
    } else {
      return Flags.get(code);
    }
  }

  static String getCountryLabel(Long countryId) {
    if (!DataUtils.isId(countryId) || DataUtils.isEmpty(countries)) {
      return null;
    }

    BeeRow row = countries.getRowById(countryId);
    if (row == null) {
      return null;
    }

    String label = row.getString(countryCodeIndex);
    if (BeeUtils.isEmpty(label)) {
      return BeeUtils.trim(row.getString(countryNameIndex));
    } else {
      return BeeUtils.trim(label).toUpperCase();
    }
  }

  static String getCityLabel(Long cityId) {
    if (!DataUtils.isId(cityId) || DataUtils.isEmpty(cities)) {
      return null;
    }

    BeeRow row = cities.getRowById(cityId);
    if (row == null) {
      return null;
    }

    String label = row.getString(cityNameIndex);
    if (BeeUtils.isEmpty(label)) {
      return BeeConst.STRING_EMPTY;
    } else {
      return BeeUtils.trim(label);
    }
  }

  static JustDate getLoadingDate(SimpleRow row, String colName) {
    if (getPlaceDateType() == ValueType.DATE_TIME) {
      return JustDate.get(row.getDateTime(colName));
    } else {
      return row.getDate(colName);
    }
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

  static ValueType getPlaceDateType() {
    return Places.placeDateType;
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

  static JustDate getUnloadingDate(SimpleRow row, String colName) {
    if (getPlaceDateType() == ValueType.DATE_TIME) {
      return JustDate.get(row.getDateTime(colName));
    } else {
      return row.getDate(colName);
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

  static void setCountries(BeeRowSet rowSet) {
    if (!DataUtils.isEmpty(rowSet)) {
      Places.countries = rowSet;

      if (BeeConst.isUndef(countryCodeIndex)) {
        Places.countryCodeIndex = rowSet.getColumnIndex(ClassifierConstants.COL_COUNTRY_CODE);
        Places.countryNameIndex = rowSet.getColumnIndex(ClassifierConstants.COL_COUNTRY_NAME);
      }
    }
  }

  static void setCities(BeeRowSet rowSet) {
    if (!DataUtils.isEmpty(rowSet)) {
      Places.cities = rowSet;

      if (BeeConst.isUndef(cityNameIndex)) {
        Places.cityNameIndex = rowSet.getColumnIndex(ClassifierConstants.COL_CITY_NAME);
      }
    }
  }

  private Places() {
  }
}
