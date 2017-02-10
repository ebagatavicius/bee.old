package com.butent.bee.shared.modules.finance.analysis;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum AnalysisValueType implements HasLocalizedCaption {

  ACTUAL('a', true, false) {
    @Override
    public String getAbbreviation() {
      return Localized.dictionary().finAnalysisValueActualShort();
    }

    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.finAnalysisValueActual();
    }

    @Override
    public String render(AnalysisValue analysisValue, int scale) {
      if (!BeeUtils.isEmpty(analysisValue.getActualValue()) && validScale(scale)) {
        return BeeUtils.toString(analysisValue.getActualNumber(), scale);
      } else {
        return analysisValue.getActualValue();
      }
    }
  },

  BUDGET('b', false, true) {
    @Override
    public String getAbbreviation() {
      return Localized.dictionary().finAnalysisValueBudgetShort();
    }

    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.finAnalysisValueBudget();
    }

    @Override
    public String render(AnalysisValue analysisValue, int scale) {
      if (!BeeUtils.isEmpty(analysisValue.getBudgetValue()) && validScale(scale)) {
        return BeeUtils.toString(analysisValue.getBudgetNumber(), scale);
      } else {
        return analysisValue.getBudgetValue();
      }
    }
  },

  DIFFERENCE('d', true, true) {
    @Override
    public String getAbbreviation() {
      return Localized.dictionary().finAnalysisValueDifferenceShort();
    }

    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.finAnalysisValueDifference();
    }

    @Override
    public String render(AnalysisValue analysisValue, int scale) {
      double v = analysisValue.getActualNumber() - analysisValue.getBudgetNumber();

      if (validScale(scale)) {
        return BeeUtils.toString(v, scale);
      } else {
        return BeeUtils.toString(v);
      }
    }
  },

  PERCENTAGE('p', true, true, 1) {
    @Override
    public String getAbbreviation() {
      return Localized.dictionary().finAnalysisValuePercentageShort();
    }

    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.finAnalysisValuePercentage();
    }

    @Override
    public String render(AnalysisValue analysisValue, int scale) {
      double budget = analysisValue.getBudgetNumber();
      if (BeeUtils.isZero(budget)) {
        return BeeConst.STRING_EMPTY;

      } else {
        double v = analysisValue.getActualNumber() * BeeConst.DOUBLE_ONE_HUNDRED / budget;

        if (validScale(scale)) {
          return BeeUtils.toString(v, scale);
        } else {
          return BeeUtils.toString(v, getDefaultScale());
        }
      }
    }
  };

  public static AnalysisValueType parse(char c) {
    for (AnalysisValueType av : values()) {
      if (av.code == c) {
        return av;
      }
    }
    return null;
  }

  public static AnalysisValueType parse(String s) {
    if (!BeeUtils.isEmpty(s) && s.trim().length() == 1) {
      return parse(s.trim().charAt(0));
    } else {
      return null;
    }
  }

  private static boolean validScale(int scale) {
    return scale >= 0;
  }

  public static final AnalysisValueType DEFAULT = ACTUAL;

  private final char code;

  private final boolean needsActual;
  private final boolean needsBudget;

  private final int defaultScale;

  AnalysisValueType(char code, boolean needsActual, boolean needsBudget) {
    this(code, needsActual, needsBudget, BeeConst.UNDEF);
  }

  AnalysisValueType(char code, boolean needsActual, boolean needsBudget, int defaultScale) {
    this.code = code;
    this.needsActual = needsActual;
    this.needsBudget = needsBudget;
    this.defaultScale = defaultScale;
  }

  public abstract String getAbbreviation();

  public char getCode() {
    return code;
  }

  public int getDefaultScale() {
    return defaultScale;
  }

  public boolean hasScale() {
    return !BeeConst.isUndef(defaultScale);
  }

  public boolean needsActual() {
    return needsActual;
  }

  public boolean needsBudget() {
    return needsBudget;
  }

  public abstract String render(AnalysisValue analysisValue, int scale);
}
