package com.butent.bee.client.view.search;

import com.butent.bee.shared.data.filter.Filter;

import java.util.function.Consumer;

public interface FilterConsumer {

  void hasAnyRows(Filter filter, Consumer<Boolean> callback);

  void tryFilter(Filter filter, Consumer<Boolean> callback, boolean notify);
}
