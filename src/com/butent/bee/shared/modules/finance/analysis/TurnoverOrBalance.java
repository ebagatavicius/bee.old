package com.butent.bee.shared.modules.finance.analysis;

import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.modules.finance.FinanceConstants;
import com.butent.bee.shared.modules.finance.NormalBalance;
import com.butent.bee.shared.time.MonthRange;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum TurnoverOrBalance implements HasLocalizedCaption {
  TURNOVER {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.finTurnover();
    }

    @Override
    public String getIndicatorName(Dictionary dictionary, String accountName) {
      return dictionary.finIndicatorNameTurnover(accountName);
    }

    @Override
    public String getIndicatorAbbreviation(Dictionary dictionary, String accountCode) {
      return dictionary.finIndicatorAbbreviationTurnover(accountCode);
    }
  },

  DEBIT_ONLY {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.debit();
    }

    @Override
    public Filter getPlusFilter(String debitColumn, String creditColumn, String accountCode,
        NormalBalance normalBalance) {

      return Filter.startsWith(debitColumn, accountCode);
    }

    @Override
    public Filter getMinusFilter(String debitColumn, String creditColumn, String accountCode,
        NormalBalance normalBalance) {

      return null;
    }

    @Override
    public String getIndicatorName(Dictionary dictionary, String accountName) {
      return dictionary.finIndicatorNameDebit(accountName);
    }

    @Override
    public String getIndicatorAbbreviation(Dictionary dictionary, String accountCode) {
      return dictionary.finIndicatorAbbreviationDebit(accountCode);
    }
  },

  CREDIT_ONLY {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.credit();
    }

    @Override
    public Filter getPlusFilter(String debitColumn, String creditColumn, String accountCode,
        NormalBalance normalBalance) {

      return Filter.startsWith(creditColumn, accountCode);
    }

    @Override
    public Filter getMinusFilter(String debitColumn, String creditColumn, String accountCode,
        NormalBalance normalBalance) {

      return null;
    }

    @Override
    public String getIndicatorName(Dictionary dictionary, String accountName) {
      return dictionary.finIndicatorNameCredit(accountName);
    }

    @Override
    public String getIndicatorAbbreviation(Dictionary dictionary, String accountCode) {
      return dictionary.finIndicatorAbbreviationCredit(accountCode);
    }
  },

  OPENING_BALANCE {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.finOpeningBalanceShort();
    }

    @Override
    public Filter getRangeFilter(String column, MonthRange range) {
      if (range == null) {
        return null;
      }

      YearMonth ym = range.getMinMonth();
      if (ym != null && BeeUtils.isMore(ym, FinanceConstants.ANALYSIS_MIN_YEAR_MONTH)) {
        return Filter.isLess(column, new DateTimeValue(ym.getDate().getDateTime()));
      } else {
        return null;
      }
    }

    @Override
    public String getIndicatorName(Dictionary dictionary, String accountName) {
      return dictionary.finIndicatorNameOpeningBalance(accountName);
    }

    @Override
    public String getIndicatorAbbreviation(Dictionary dictionary, String accountCode) {
      return dictionary.finIndicatorAbbreviationOpeningBalance(accountCode);
    }
  },

  CLOSING_BALANCE {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.finClosingBalanceShort();
    }

    @Override
    public Filter getRangeFilter(String column, MonthRange range) {
      if (range == null) {
        return null;
      }

      YearMonth ym = range.getMaxMonth();
      if (ym != null && BeeUtils.isLess(ym, FinanceConstants.ANALYSIS_MAX_YEAR_MONTH)) {
        return Filter.isLess(column, new DateTimeValue(ym.nextMonth().getDate().getDateTime()));
      } else {
        return null;
      }
    }

    @Override
    public String getIndicatorName(Dictionary dictionary, String accountName) {
      return dictionary.finIndicatorNameClosingBalance(accountName);
    }

    @Override
    public String getIndicatorAbbreviation(Dictionary dictionary, String accountCode) {
      return dictionary.finIndicatorAbbreviationClosingBalance(accountCode);
    }
  };

  public static final TurnoverOrBalance DEFAULT = TURNOVER;

  public static boolean isBalance(TurnoverOrBalance turnoverOrBalance) {
    return turnoverOrBalance == OPENING_BALANCE || turnoverOrBalance == CLOSING_BALANCE;
  }

  public Filter getPlusFilter(String debitColumn, String creditColumn,
      String accountCode, NormalBalance normalBalance) {

    if (normalBalance == NormalBalance.DEBIT) {
      return Filter.startsWith(debitColumn, accountCode);
    } else if (normalBalance == NormalBalance.CREDIT) {
      return Filter.startsWith(creditColumn, accountCode);
    } else {
      return null;
    }
  }

  public Filter getMinusFilter(String debitColumn, String creditColumn,
      String accountCode, NormalBalance normalBalance) {

    if (normalBalance == NormalBalance.DEBIT) {
      return Filter.startsWith(creditColumn, accountCode);
    } else if (normalBalance == NormalBalance.CREDIT) {
      return Filter.startsWith(debitColumn, accountCode);
    } else {
      return null;
    }
  }

  public Filter getRangeFilter(String column, MonthRange range) {
    return AnalysisUtils.getFilter(column, range);
  }

  public abstract String getIndicatorName(Dictionary dictionary, String accountName);

  public abstract String getIndicatorAbbreviation(Dictionary dictionary, String accountCode);
}
