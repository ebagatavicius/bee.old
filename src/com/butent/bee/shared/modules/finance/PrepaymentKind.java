package com.butent.bee.shared.modules.finance;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;

public enum PrepaymentKind implements HasLocalizedCaption {
  CUSTOMERS {
    @Override
    public String defaultAccountColumn() {
      return COL_ADVANCE_PAYMENTS_RECEIVED;
    }

    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.prepaymentCustomersShort();
    }

    @Override
    public NormalBalance normalBalance() {
      return NormalBalance.CREDIT;
    }

    @Override
    public String tradePaymentsMainGrid() {
      return GRID_OUTSTANDING_PREPAYMENT_RECEIVED;
    }

    @Override
    public String tradePaymentsOtherGrid() {
      return GRID_OUTSTANDING_PREPAYMENT_GIVEN;
    }
  },

  SUPPLIERS {
    @Override
    public String defaultAccountColumn() {
      return COL_ADVANCE_PAYMENTS_GIVEN;
    }

    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.prepaymentSuppliersShort();
    }

    @Override
    public NormalBalance normalBalance() {
      return NormalBalance.DEBIT;
    }

    @Override
    public String tradePaymentsMainGrid() {
      return GRID_OUTSTANDING_PREPAYMENT_GIVEN;
    }

    @Override
    public String tradePaymentsOtherGrid() {
      return GRID_OUTSTANDING_PREPAYMENT_RECEIVED;
    }
  };

  public abstract String defaultAccountColumn();

  public abstract NormalBalance normalBalance();

  public String styleSuffix() {
    return name().toLowerCase();
  }

  public abstract String tradePaymentsMainGrid();

  public abstract String tradePaymentsOtherGrid();
}
