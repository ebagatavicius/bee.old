package com.butent.bee.client.modules.transport.charts;

import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumMap;

abstract class Filterable {

  private final EnumMap<Filterable.FilterType, Boolean> filterResults =
      new EnumMap<>(Filterable.FilterType.class);

  enum FilterType {
    TENTATIVE, PERSISTENT
  }

  boolean matched(FilterType filterType) {
    if (filterType != null && filterResults.containsKey(filterType)) {
      return BeeUtils.unbox(filterResults.get(filterType));
    } else {
      return true;
    }
  }

  boolean persistFilter() {
    boolean match = matched(FilterType.TENTATIVE);
    setMatch(FilterType.PERSISTENT, match);

    return match;
  }

  boolean setMatch(FilterType filterType, boolean match) {
    if (matched(filterType) == match) {
      return false;
    } else {
      filterResults.put(filterType, match);
      return true;
    }
  }
}
