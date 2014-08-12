package com.butent.bee.client.view.search;

import com.butent.bee.client.widget.InputTime;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.time.DateTime;

public class VersionFilterSupplier extends DateTimeFilterSupplier {

  public VersionFilterSupplier(String viewName, BeeColumn column, String label, String options) {
    super(viewName, column, label, options);
  }

  @Override
  protected InputTime createInputTime() {
    return new InputTime();
  }

  @Override
  protected Filter buildFilter(DateTime start, DateTime end) {
    if (start == null && end == null) {
      return null;
    } else if (end == null) {
      return Filter.compareVersion(Operator.GE, start.getTime());
    } else if (start == null) {
      return Filter.compareVersion(Operator.LT, end.getTime());
    } else {
      return Filter.and(Filter.compareVersion(Operator.GE, start.getTime()),
          Filter.compareVersion(Operator.LT, end.getTime()));
    }
  }

  @Override
  protected Filter getEmptinessFilter(String columnId, Boolean emptiness) {
    if (emptiness == null) {
      return null;
    } else if (emptiness) {
      return Filter.compareVersion(Operator.LE, 0);
    } else {
      return Filter.compareVersion(Operator.GT, 0);
    }
  }
}
