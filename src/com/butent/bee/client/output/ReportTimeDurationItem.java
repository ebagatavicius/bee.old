package com.butent.bee.client.output;

import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.report.ReportFunction;
import com.butent.bee.shared.time.TimeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

public class ReportTimeDurationItem extends ReportNumericItem  {

  public ReportTimeDurationItem(String expression, String caption) {
    super(expression, caption);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object calculate(Object total, ReportValue value, ReportFunction function) {
    Long val = TimeUtils.parseTime(value.getValue());

    if (val != null && val > 0) {
      switch (function) {
        case COUNT:
          if (total == null) {
            return 1;
          }
          return (int) total + 1;
        case LIST:
          if (total == null) {
            return new TreeSet<>(Collections.singleton(value));
          }
          ((Collection<ReportValue>) total).add(value);
          break;
        case MAX:
          if (total == null) {
            return TimeUtils.renderTime(val, false);
          } else {
            Long totalDuration = getTotalParseTimeValue(total);
            return TimeUtils.renderTime(
                val.compareTo(totalDuration) >= 0 ? val : totalDuration, false);
          }
        case MIN:
          if (total == null) {
            return TimeUtils.renderTime(val, false);
          } else {
            Long totalDuration = getTotalParseTimeValue(total);
            return TimeUtils.renderTime(
                val.compareTo(totalDuration) <= 0 ? val : totalDuration, false);
          }
        case SUM:
          if (total == null) {
            return TimeUtils.renderTime(val, false);
          } else {
            return TimeUtils.renderTime(val + getTotalParseTimeValue(total), false);
          }
        default:
          return super.calculate(total, value, function);
      }
    }
    return total;
  }

  private static Long getTotalParseTimeValue(Object total) {
    if (total instanceof String) {
      return TimeUtils.parseTime((String) total);
    } else {
      return (Long) total;
    }
  }

  @Override
  public ReportValue evaluate(SimpleRowSet.SimpleRow row) {
    return ReportValue.of(row.getValue(getExpression()));
  }

  @Override
  public String getStyle() {
    return STYLE_TEXT;
  }

}
