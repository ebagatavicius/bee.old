package com.butent.bee.client.output;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.widget.InputSpinner;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.report.ReportFunction;
import com.butent.bee.shared.report.ResultCalculator;
import com.butent.bee.shared.report.ResultValue;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

public class ReportNumericItem extends ReportItem {

  private static final String PRECISION = "PRECISION";

  private int precision;
  private InputSpinner precisionWidget;

  public ReportNumericItem(String expression, String caption) {
    super(expression, caption);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object calculate(Object total, ResultValue value, ReportFunction function) {
    BigDecimal val = BeeUtils.toDecimalOrNull(value.getValue());

    if (val != null) {
      switch (function) {
        case MAX:
        case MIN:
        case SUM:
          ResultCalculator<BigDecimal> calculator;

          if (total == null) {
            calculator = new ResultCalculator<BigDecimal>() {
              @Override
              public ResultCalculator calculate(ReportFunction fnc, BigDecimal val) {
                BigDecimal res = getResult();

                switch (fnc) {
                  case MAX:
                    if (Objects.isNull(res) || res.compareTo(val) == BeeConst.COMPARE_LESS) {
                      setResult(val);
                    }
                    break;
                  case MIN:
                    if (Objects.isNull(res) || res.compareTo(val) == BeeConst.COMPARE_MORE) {
                      setResult(val);
                    }
                    break;
                  case SUM:
                    setResult(Objects.isNull(res) ? val : res.add(val));
                    break;
                  default:
                    Assert.unsupported();
                    break;
                }
                return this;
              }
            };
          } else {
            calculator = (ResultCalculator<BigDecimal>) total;
          }
          return calculator.calculate(function, val);
        default:
          return super.calculate(total, value, function);
      }
    }
    return total;
  }

  @Override
  public void deserialize(String data) {
    Map<String, String> map = Codec.deserializeLinkedHashMap(data);

    if (!BeeUtils.isEmpty(map)) {
      setPrecision(BeeUtils.toInt(map.get(PRECISION)));
    }
  }

  @Override
  public ResultValue evaluate(SimpleRow row, Dictionary dictionary) {
    return ResultValue.of(BeeUtils.round(row.getValue(getExpression()), getPrecision()));
  }

  @Override
  public EnumSet<ReportFunction> getAvailableFunctions() {
    EnumSet<ReportFunction> functions = super.getAvailableFunctions();
    functions.add(ReportFunction.SUM);
    return functions;
  }

  @Override
  public String getOptionsCaption() {
    return Localized.dictionary().precision();
  }

  @Override
  public Widget getOptionsWidget() {
    if (precisionWidget == null) {
      precisionWidget = new InputSpinner(0, 5);
    }
    precisionWidget.setValue(getPrecision());
    return precisionWidget;
  }

  public int getPrecision() {
    return precision;
  }

  @Override
  public String getStyle() {
    return STYLE_NUM;
  }

  @Override
  public String saveOptions() {
    if (precisionWidget != null) {
      setPrecision(precisionWidget.getIntValue());
    }
    return super.saveOptions();
  }

  @Override
  public String serialize() {
    return serialize(Codec.beeSerialize(Collections.singletonMap(PRECISION, getPrecision())));
  }

  public ReportNumericItem setPrecision(int prec) {
    this.precision = Assert.nonNegative(prec);
    return this;
  }
}
