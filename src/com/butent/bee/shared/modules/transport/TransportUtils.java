package com.butent.bee.shared.modules.transport;

import com.google.common.collect.Range;

import com.butent.bee.shared.data.value.DateValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;

public final class TransportUtils {

  public static Range<Value> getChartPeriod(JustDate from, JustDate to) {
    DateValue lower = (from == null) ? null : new DateValue(from);
    DateValue upper = (to == null) ? null : new DateValue(to);

    Range<Value> period;

    if (from == null && to == null) {
      JustDate min = TimeUtils.startOfMonth(TimeUtils.today(), -1);
      period = Range.atLeast(new DateValue(min));

    } else if (from == null) {
      period = Range.lessThan(upper);

    } else if (to == null) {
      period = Range.atLeast(lower);

    } else if (Objects.equals(from, to)) {
      period = Range.singleton(lower);

    } else if (BeeUtils.isMore(from, to)) {
      period = Range.closedOpen(upper, lower);

    } else {
      period = Range.closedOpen(lower, upper);
    }

    return period;
  }

  private TransportUtils() {
  }
}
