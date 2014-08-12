package com.butent.bee.client.view.search;

import com.google.common.collect.ImmutableSet;

import com.butent.bee.shared.data.filter.Filter;

public interface FilterHandler {

  Filter getEffectiveFilter(ImmutableSet<String> exclusions);

  void onFilterChange();
}
