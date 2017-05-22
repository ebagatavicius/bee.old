package com.butent.bee.shared.modules.trade;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.finance.TradeAccounts;
import com.butent.bee.shared.ui.HasLocalizedCaption;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public enum OperationType implements HasLocalizedCaption {
  PURCHASE(false, true, true, true, DebtKind.PAYABLE) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypePurchase();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getCostAccount();
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradePayables();
    }

    @Override
    public Long getDebtAccount(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradePayables();
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getVatReceivable();
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradePayables();
    }

    @Override
    public ItemPrice getDefaultPrice() {
      return ItemPrice.COST;
    }
  },

  SALE(true, false, false, false, DebtKind.RECEIVABLE) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypeSale();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getSalesRevenue();
    }

    @Override
    public Long getDebtAccount(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getVatPayable();
    }

    @Override
    public ItemPrice getDefaultPrice() {
      return ItemPrice.SALE;
    }
  },

  TRANSFER(true, true, false, false, null) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypeTransfer();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getDebtAccount(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public ItemPrice getDefaultPrice() {
      return ItemPrice.COST;
    }
  },

  WRITE_OFF(true, false, false, false, null) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypeWriteOff();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getDebtAccount(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return null;
    }

    @Override
    public ItemPrice getDefaultPrice() {
      return ItemPrice.COST;
    }
  },

  POS(true, false, false, false, null) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypePointOfSale();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getSalesRevenue();
    }

    @Override
    public Long getDebtAccount(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getVatPayable();
    }

    @Override
    public ItemPrice getDefaultPrice() {
      return ItemPrice.SALE;
    }
  },

  CUSTOMER_RETURN(false, true, true, false, DebtKind.PAYABLE) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypeCustomerReturn();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getSalesRevenue();
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getDebtAccount(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getVatPayable();
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradeReceivables();
    }

    @Override
    public ItemPrice getDefaultPrice() {
      return ItemPrice.SALE;
    }
  },

  RETURN_TO_SUPPLIER(true, false, false, true, DebtKind.RECEIVABLE) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trdTypeReturnToSupplier();
    }

    @Override
    public Long getAmountDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradePayables();
    }

    @Override
    public Long getAmountCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getCostAccount();
    }

    @Override
    public Long getDebtAccount(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradePayables();
    }

    @Override
    public Long getVatDebit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getTradePayables();
    }

    @Override
    public Long getVatCredit(TradeAccounts tradeAccounts) {
      return (tradeAccounts == null) ? null : tradeAccounts.getVatReceivable();
    }

    @Override
    public ItemPrice getDefaultPrice() {
      return ItemPrice.COST;
    }
  };

  public static Collection<OperationType> getStockProducers() {
    Set<OperationType> producers = new HashSet<>();

    for (OperationType type : values()) {
      if (type.producesStock()) {
        producers.add(type);
      }
    }

    return producers;
  }

  public static Collection<OperationType> getStockConsumers() {
    Set<OperationType> consumers = new HashSet<>();

    for (OperationType type : values()) {
      if (type.consumesStock()) {
        consumers.add(type);
      }
    }

    return consumers;
  }

  private final boolean consumesStock;
  private final boolean producesStock;

  private final boolean providesCost;

  private final boolean requireOperationForPriceCalculation;

  private final DebtKind debtKind;

  OperationType(boolean consumesStock, boolean producesStock, boolean providesCost,
      boolean requireOperationForPriceCalculation, DebtKind debtKind) {

    this.consumesStock = consumesStock;
    this.producesStock = producesStock;

    this.providesCost = providesCost;

    this.requireOperationForPriceCalculation = requireOperationForPriceCalculation;

    this.debtKind = debtKind;
  }

  public boolean consumesStock() {
    return consumesStock;
  }

  public boolean producesStock() {
    return producesStock;
  }

  public boolean providesCost() {
    return providesCost;
  }

  public boolean requireOperationForPriceCalculation() {
    return requireOperationForPriceCalculation;
  }

  public DebtKind getDebtKind() {
    return debtKind;
  }

  public boolean hasDebt() {
    return debtKind != null;
  }

  public abstract Long getAmountDebit(TradeAccounts tradeAccounts);

  public abstract Long getAmountCredit(TradeAccounts tradeAccounts);

  public abstract Long getDebtAccount(TradeAccounts tradeAccounts);

  public abstract Long getVatDebit(TradeAccounts tradeAccounts);

  public abstract Long getVatCredit(TradeAccounts tradeAccounts);

  public abstract ItemPrice getDefaultPrice();
}
