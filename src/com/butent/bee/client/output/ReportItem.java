package com.butent.bee.client.output;

import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.TreeSet;

public abstract class ReportItem {

  public enum Function implements HasLocalizedCaption {
    MIN() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return "Mažiausia";
      }
    },
    MAX() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return "Didžiausia";
      }
    },
    SUM() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return "Suma";
      }
    },
    COUNT() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return "Kiekis";
      }
    },
    LIST() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return "Sąrašas";
      }
    };

    @Override
    public String getCaption() {
      return getCaption(Localized.getConstants());
    }
  }

  final String name;
  final String caption;
  String expression;
  String options;
  Function function;
  boolean colSummary;
  boolean rowSummary;

  protected ReportItem(String name, String caption) {
    this.name = Assert.notEmpty(name);
    this.caption = BeeUtils.notEmpty(caption, this.name);
    setExpression(name);
  }

  @SuppressWarnings("unchecked")
  public Object calculate(Object total, String value) {
    if (!BeeUtils.isEmpty(value)) {
      switch (getFunction()) {
        case COUNT:
          if (total == null) {
            return 1;
          }
          return (int) total + 1;
        case LIST:
          if (total == null) {
            return new TreeSet<>(Arrays.asList(value));
          }
          ((Collection<String>) total).add(value);
          break;
        case MAX:
          if (total == null || value.compareTo((String) total) > 0) {
            return value;
          }
          break;
        case MIN:
          if (total == null || value.compareTo((String) total) < 0) {
            return value;
          }
          break;
        case SUM:
          Assert.unsupported();
          break;
      }
    }
    return total;
  }

  public abstract ReportItem create();

  public ReportItem enableCalculation() {
    function = Function.MAX;
    return this;
  }

  public abstract String evaluate(SimpleRow row);

  public EnumSet<Function> getAvailableFunctions() {
    return EnumSet.of(Function.MIN, Function.MAX, Function.COUNT, Function.LIST);
  }

  public String getCaption() {
    return caption;
  }

  public String getExpression() {
    return expression;
  }

  public Function getFunction() {
    return function;
  }

  public String getName() {
    return name;
  }

  public String getOptions() {
    return options;
  }

  public String getOptionsCaption() {
    return null;
  }

  public Editor getOptionsEditor() {
    return null;
  }

  public boolean isColSummary() {
    return getFunction() != null && colSummary;
  }

  public boolean isRowSummary() {
    return getFunction() != null && rowSummary;
  }

  public ReportItem setColSummary(boolean isSummary) {
    this.colSummary = isSummary;
    return this;
  }

  public ReportItem setExpression(String xpr) {
    this.expression = xpr;
    return this;
  }

  public ReportItem setFunction(Function fnc) {
    Assert.notNull(getFunction());
    this.function = Assert.notNull(fnc);
    return this;
  }

  public ReportItem setOptions(String opt) {
    this.options = opt;
    return this;
  }

  public ReportItem setRowSummary(boolean isSummary) {
    this.rowSummary = isSummary;
    return this;
  }

  @SuppressWarnings("unchecked")
  public Object summarize(Object total, Object value) {
    if (value != null) {
      if (total == null) {
        return value;
      }
      switch (getFunction()) {
        case COUNT:
          return (int) total + (int) value;
        case LIST:
          ((Collection<String>) total).addAll((Collection<String>) value);
          break;
        case SUM:
          Assert.unsupported();
          break;
        default:
          return calculate(total, (String) value);
      }
    }
    return total;
  }
}
