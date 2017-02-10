package com.butent.bee.shared.modules.finance.analysis;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.modules.finance.FinanceConstants;
import com.butent.bee.shared.ui.HasLocalizedCaption;

public enum IndicatorSource implements HasLocalizedCaption {
  AMOUNT {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.amount();
    }

    @Override
    public String getSourceColumn() {
      return FinanceConstants.COL_FIN_AMOUNT;
    }

    @Override
    public String getCurrencyColumn() {
      return FinanceConstants.COL_FIN_CURRENCY;
    }

    @Override
    public int getDefaultScale() {
      return 2;
    }
  },

  QUANTITY {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.quantity();
    }

    @Override
    public String getSourceColumn() {
      return FinanceConstants.COL_FIN_QUANTITY;
    }

    @Override
    public String getCurrencyColumn() {
      return null;
    }

    @Override
    public int getDefaultScale() {
      return 0;
    }
  };

  public static final IndicatorSource DEFAULT = AMOUNT;

  public abstract String getSourceColumn();

  public abstract String getCurrencyColumn();

  public boolean hasCurrency() {
    return getCurrencyColumn() != null;
  }

  public abstract int getDefaultScale();
}
