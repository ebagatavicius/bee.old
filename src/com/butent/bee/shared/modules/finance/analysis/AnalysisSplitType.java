package com.butent.bee.shared.modules.finance.analysis;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.modules.finance.FinanceConstants;
import com.butent.bee.shared.time.MonthRange;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public enum AnalysisSplitType implements HasLocalizedCaption {

  MONTH(Kind.PERIOD, 1) {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.month();
    }

    @Override
    public MonthRange getMonthRange(AnalysisSplitValue splitValue) {
      if (splitValue == null) {
        return super.getMonthRange(splitValue);
      } else {
        return MonthRange.month(splitValue.getYearMonth());
      }
    }
  },

  QUARTER(Kind.PERIOD, 3) {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.quarter();
    }

    @Override
    public MonthRange getMonthRange(AnalysisSplitValue splitValue) {
      if (splitValue == null) {
        return super.getMonthRange(splitValue);
      } else {
        return MonthRange.quarter(splitValue.getYearQuarter());
      }
    }
  },

  YEAR(Kind.PERIOD, 12) {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.year();
    }

    @Override
    public MonthRange getMonthRange(AnalysisSplitValue splitValue) {
      if (splitValue == null) {
        return super.getMonthRange(splitValue);
      } else {
        return MonthRange.year(splitValue.getYear());
      }
    }
  },

  EMPLOYEE(Kind.FILTER) {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.employee();
    }

    @Override
    public String getBudgetColumn() {
      return FinanceConstants.COL_BUDGET_ENTRY_EMPLOYEE;
    }

    @Override
    public String getFinColumn() {
      return FinanceConstants.COL_FIN_EMPLOYEE;
    }
  },

  DIMENSION_01(Kind.DIMENSION, 1),
  DIMENSION_02(Kind.DIMENSION, 2),
  DIMENSION_03(Kind.DIMENSION, 3),
  DIMENSION_04(Kind.DIMENSION, 4),
  DIMENSION_05(Kind.DIMENSION, 5),
  DIMENSION_06(Kind.DIMENSION, 6),
  DIMENSION_07(Kind.DIMENSION, 7),
  DIMENSION_08(Kind.DIMENSION, 8),
  DIMENSION_09(Kind.DIMENSION, 9),
  DIMENSION_10(Kind.DIMENSION, 10);

  private enum Kind {
    PERIOD, FILTER, DIMENSION
  }

  public static final AnalysisSplitType[] PERIODS_INCREASING =
      new AnalysisSplitType[] {MONTH, QUARTER, YEAR};

  public static boolean validateSplits(List<AnalysisSplitType> splits) {
    if (BeeUtils.isEmpty(splits)) {
      return true;

    } else if (splits.contains(null)) {
      return false;

    } else if (BeeUtils.size(splits) > 1) {
      if (!BeeUtils.hasDistinctElements(splits)) {
        return false;
      }

      int lastPeriod = BeeConst.UNDEF;

      for (AnalysisSplitType split : splits) {
        if (split.kind == Kind.PERIOD) {
          if (!BeeConst.isUndef(lastPeriod) && split.index >= lastPeriod) {
            return false;
          }
          lastPeriod = split.index;
        }
      }
    }

    return true;
  }

  private final Kind kind;
  private final int index;

  AnalysisSplitType(Kind kind) {
    this(kind, BeeConst.UNDEF);
  }

  AnalysisSplitType(Kind kind, int index) {
    this.kind = kind;
    this.index = index;
  }

  @Override
  public String getCaption(Dictionary dictionary) {
    if (kind == Kind.DIMENSION) {
      return Dimensions.singular(index);
    } else {
      return null;
    }
  }

  public String getBudgetColumn() {
    if (kind == Kind.DIMENSION) {
      return Dimensions.getRelationColumn(index);
    } else {
      return null;
    }
  }

  public String getFinColumn() {
    switch (kind) {
      case PERIOD:
        return FinanceConstants.COL_FIN_DATE;
      case DIMENSION:
        return Dimensions.getRelationColumn(index);
      default:
        return null;
    }
  }

  public MonthRange getMonthRange(AnalysisSplitValue splitValue) {
    return null;
  }

  public int getIndex() {
    return index;
  }

  public boolean isDimension() {
    return kind == Kind.DIMENSION;
  }

  public boolean isPeriod() {
    return kind == Kind.PERIOD;
  }

  public boolean isVisible() {
    if (kind == Kind.DIMENSION) {
      return Dimensions.isObserved(index);
    } else {
      return true;
    }
  }
}
