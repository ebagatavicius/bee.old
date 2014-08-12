package com.butent.bee.client.view.search;

import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.filter.Filter;

public interface FilterConsumer {
  void tryFilter(Filter filter, Consumer<Boolean> callback, boolean notify);
}
