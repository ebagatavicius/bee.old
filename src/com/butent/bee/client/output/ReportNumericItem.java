package com.butent.bee.client.output;

import com.butent.bee.client.widget.InputSpinner;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.report.ReportFunction;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

public class ReportNumericItem extends ReportItem {

  private static final String PRECISION = "PRECISION";

  private int precision;
  private InputSpinner precisionWidget;

  public ReportNumericItem(String name, String caption) {
    super(name, caption);
  }

  @Override
  public Object calculate(Object total, ReportValue value, ReportFunction function) {
    BigDecimal val = BeeUtils.toDecimalOrNull(value.getValue());

    if (val != null) {
      switch (function) {
        case MAX:
          if (total == null) {
            return val;
          }
          return val.max((BigDecimal) total);
        case MIN:
          if (total == null) {
            return val;
          }
          return val.min((BigDecimal) total);
        case SUM:
          if (total == null) {
            return val;
          }
          return val.add((BigDecimal) total);
        default:
          return super.calculate(total, value, function);
      }
    }
    return total;
  }

  @Override
  public void deserialize(String data) {
    Map<String, String> map = Codec.deserializeMap(data);

    if (!BeeUtils.isEmpty(map)) {
      setPrecision(BeeUtils.toInt(map.get(PRECISION)));
    }
  }

  @Override
  public ReportValue evaluate(SimpleRow row) {
    return ReportValue.of(BeeUtils.round(row.getValue(getName()), getPrecision()));
  }

  @Override
  public EnumSet<ReportFunction> getAvailableFunctions() {
    EnumSet<ReportFunction> functions = super.getAvailableFunctions();
    functions.add(ReportFunction.SUM);
    return functions;
  }

  @Override
  public String getOptionsCaption() {
    return Localized.getConstants().precision();
  }

  @Override
  public InputSpinner getOptionsWidget() {
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
    return serialize(Codec.beeSerialize(Collections.singletonMap(PRECISION, precision)));
  }

  public ReportNumericItem setPrecision(int prec) {
    this.precision = Assert.nonNegative(prec);
    return this;
  }

  @Override
  public Object summarize(Object total, Object value, ReportFunction function) {
    if (value != null) {
      if (total == null) {
        return super.summarize(total, value, function);
      }
      switch (function) {
        case MAX:
          return ((BigDecimal) value).max((BigDecimal) total);
        case MIN:
          return ((BigDecimal) value).min((BigDecimal) total);
        case SUM:
          return ((BigDecimal) value).add((BigDecimal) total);
        default:
          return super.summarize(total, value, function);
      }
    }
    return total;
  }
}
