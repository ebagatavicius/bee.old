package com.butent.bee.client.output;

import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.widget.InputSpinner;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.utils.BeeUtils;

import java.math.BigDecimal;
import java.util.EnumSet;

public class ReportNumericItem extends ReportItem {

  public ReportNumericItem(String name, String caption, int precision) {
    super(name, caption);
    setOptions(BeeUtils.toString(precision));
  }

  @Override
  public Object calculate(Object total, String value) {
    BigDecimal val = BeeUtils.toDecimalOrNull(value);

    if (val != null) {
      switch (getFunction()) {
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
          return super.calculate(total, value);
      }
    }
    return total;
  }

  @Override
  public ReportItem create() {
    return new ReportNumericItem(getName(), getCaption(), BeeUtils.toInt(getOptions()));
  }

  @Override
  public ReportItem enableCalculation() {
    function = Function.SUM;
    setRowSummary(true);
    setColSummary(true);
    return this;
  }

  @Override
  public String evaluate(SimpleRow row) {
    return BeeUtils.round(row.getValue(getName()), BeeUtils.toInt(getOptions()));
  }

  @Override
  public EnumSet<Function> getAvailableFunctions() {
    EnumSet<Function> functions = super.getAvailableFunctions();
    functions.add(Function.SUM);
    return functions;
  }

  @Override
  public String getOptionsCaption() {
    return "Tikslumas";
  }

  @Override
  public Editor getOptionsEditor() {
    InputSpinner editor = new InputSpinner(0, 5);
    editor.setValue(getOptions());
    return editor;
  }

  @Override
  public Object summarize(Object total, Object value) {
    if (value != null) {
      if (total == null) {
        return value;
      }
      switch (getFunction()) {
        case MAX:
          return ((BigDecimal) value).max((BigDecimal) total);
        case MIN:
          return ((BigDecimal) value).min((BigDecimal) total);
        case SUM:
          return ((BigDecimal) value).add((BigDecimal) total);
        default:
          return super.summarize(total, value);
      }
    }
    return total;
  }
}
