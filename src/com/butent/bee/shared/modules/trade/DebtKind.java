package com.butent.bee.shared.modules.trade;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.modules.finance.PrepaymentKind;
import com.butent.bee.shared.ui.HasLocalizedCaption;

public enum DebtKind implements HasLocalizedCaption {
  PAYABLE {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.trdAccountsPayable();
    }

    @Override
    public String getPayerLabel(Dictionary dictionary) {
      return dictionary.supplier();
    }

    @Override
    public PrepaymentKind getPrepaymentKind() {
      return PrepaymentKind.SUPPLIERS;
    }

    @Override
    public String tradeDebtsMainGrid() {
      return GRID_TRADE_PAYABLES;
    }

    @Override
    public String tradeDebtsOtherGrid() {
      return GRID_TRADE_RECEIVABLES;
    }

    @Override
    public String tradeDocumentCompanyColumn() {
      return COL_TRADE_SUPPLIER;
    }
  },

  RECEIVABLE {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.trdAccountsReceivable();
    }

    @Override
    public String getPayerLabel(Dictionary dictionary) {
      return dictionary.payer();
    }

    @Override
    public PrepaymentKind getPrepaymentKind() {
      return PrepaymentKind.CUSTOMERS;
    }

    @Override
    public String tradeDebtsMainGrid() {
      return GRID_TRADE_RECEIVABLES;
    }

    @Override
    public String tradeDebtsOtherGrid() {
      return GRID_TRADE_PAYABLES;
    }

    @Override
    public String tradeDocumentCompanyColumn() {
      return COL_TRADE_CUSTOMER;
    }
  };

  public abstract String getPayerLabel(Dictionary dictionary);

  public abstract PrepaymentKind getPrepaymentKind();

  public String getStyleSuffix() {
    return name().toLowerCase();
  }

  public abstract String tradeDebtsMainGrid();

  public abstract String tradeDebtsOtherGrid();

  public abstract String tradeDocumentCompanyColumn();
}
