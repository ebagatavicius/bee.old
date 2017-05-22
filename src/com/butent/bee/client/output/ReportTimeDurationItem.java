package com.butent.bee.client.output;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.report.ReportFunction;
import com.butent.bee.shared.report.ResultCalculator;
import com.butent.bee.shared.report.ResultValue;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;

public class ReportTimeDurationItem extends ReportNumericItem {

  public ReportTimeDurationItem(String expression, String caption) {
    super(expression, caption);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object calculate(Object total, ResultValue value, ReportFunction function) {
    Long val = BeeUtils.toLongOrNull(value.getValue());

    if (BeeUtils.isPositive(val)) {
      switch (function) {
        case MAX:
        case MIN:
        case SUM:
          ResultCalculator<Long> calculator;

          if (total == null) {
            calculator = new ResultCalculator<Long>() {
              @Override
              public ResultCalculator calculate(ReportFunction fnc, Long val) {
                Long res = getResult();

                switch (fnc) {
                  case MAX:
                    if (Objects.isNull(res) || res < val) {
                      setResult(val);
                    }
                    break;
                  case MIN:
                    if (Objects.isNull(res) || res > val) {
                      setResult(val);
                    }
                    break;
                  case SUM:
                    setResult(Objects.isNull(res) ? val : res + val);
                    break;
                  default:
                    Assert.unsupported();
                    break;
                }
                return this;
              }

              @Override
              public String getString() {
                return TimeUtils.renderTime(getResult(), false);
              }
            };
          } else {
            calculator = (ResultCalculator<Long>) total;
          }
          return calculator.calculate(function, val);
        default:
          return super.calculate(total, value, function);
      }
    }
    return total;
  }

  @Override
  public ResultValue evaluate(SimpleRowSet.SimpleRow row, Dictionary dictionary) {
    ResultValue value;
    String time = row.getValue(getExpression());
    Long val = TimeUtils.parseTime(time);

    if (BeeUtils.isPositive(val)) {
      value = ResultValue.of(BeeUtils.padLeft(BeeUtils.toString(val), 15, BeeConst.CHAR_SPACE))
          .setDisplay(time);
    } else {
      value = ResultValue.empty();
    }
    return value;
  }

  @Override
  public Widget getOptionsWidget() {
    return null;
  }
}
