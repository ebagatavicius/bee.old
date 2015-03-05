package com.butent.bee.shared.modules.ec;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class EcFinInfo implements BeeSerializable {

  private enum Serial {
    CREDIT_LIMIT, DAYS, DEBT, MAXED_OUT, TOTAL_TAKEN, ORDERS, INVOICES, UNSUPPLIED_ITEMS
  }

  public static EcFinInfo restore(String s) {
    EcFinInfo efi = new EcFinInfo();
    efi.deserialize(s);
    return efi;
  }

  private Double creditLimit;
  private Integer daysForPayment;

  private Double debt;
  private Double maxedOut;

  private Double totalTaken;

  private final List<EcOrder> orders = new ArrayList<>();
  private final List<EcInvoice> invoices = new ArrayList<>();

  private BeeRowSet unsuppliedItems;

  public EcFinInfo() {
    super();
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case CREDIT_LIMIT:
          setCreditLimit(BeeUtils.toDoubleOrNull(value));
          break;

        case DAYS:
          setDaysForPayment(BeeUtils.toIntOrNull(value));
          break;

        case DEBT:
          setDebt(BeeUtils.toDoubleOrNull(value));
          break;

        case MAXED_OUT:
          setMaxedOut(BeeUtils.toDoubleOrNull(value));
          break;

        case TOTAL_TAKEN:
          setTotalTaken(BeeUtils.toDoubleOrNull(value));
          break;

        case ORDERS:
          orders.clear();
          String[] ordArr = Codec.beeDeserializeCollection(value);
          if (ordArr != null) {
            for (String ord : ordArr) {
              orders.add(EcOrder.restore(ord));
            }
          }
          break;

        case INVOICES:
          invoices.clear();
          String[] invArr = Codec.beeDeserializeCollection(value);
          if (invArr != null) {
            for (String inv : invArr) {
              invoices.add(EcInvoice.restore(inv));
            }
          }
          break;

        case UNSUPPLIED_ITEMS:
          if (BeeUtils.isEmpty(value)) {
            setUnsuppliedItems(null);
          } else {
            setUnsuppliedItems(BeeRowSet.restore(value));
          }
          break;
      }
    }
  }

  public Double getCreditLimit() {
    return creditLimit;
  }

  public Integer getDaysForPayment() {
    return daysForPayment;
  }

  public Double getDebt() {
    return debt;
  }

  public List<EcInvoice> getInvoices() {
    return invoices;
  }

  public Double getMaxedOut() {
    return maxedOut;
  }

  public List<EcOrder> getOrders() {
    return orders;
  }

  public Double getTotalTaken() {
    return totalTaken;
  }

  public BeeRowSet getUnsuppliedItems() {
    return unsuppliedItems;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case CREDIT_LIMIT:
          arr[i++] = getCreditLimit();
          break;

        case DAYS:
          arr[i++] = getDaysForPayment();
          break;

        case DEBT:
          arr[i++] = getDebt();
          break;

        case MAXED_OUT:
          arr[i++] = getMaxedOut();
          break;

        case TOTAL_TAKEN:
          arr[i++] = getTotalTaken();
          break;

        case ORDERS:
          arr[i++] = getOrders();
          break;

        case INVOICES:
          arr[i++] = getInvoices();
          break;

        case UNSUPPLIED_ITEMS:
          arr[i++] = getUnsuppliedItems();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setCreditLimit(Double creditLimit) {
    this.creditLimit = creditLimit;
  }

  public void setDaysForPayment(Integer daysForPayment) {
    this.daysForPayment = daysForPayment;
  }

  public void setDebt(Double debt) {
    this.debt = debt;
  }

  public void setMaxedOut(Double maxedOut) {
    this.maxedOut = maxedOut;
  }

  public void setTotalTaken(Double totalTaken) {
    this.totalTaken = totalTaken;
  }

  public void setUnsuppliedItems(BeeRowSet unsuppliedItems) {
    this.unsuppliedItems = unsuppliedItems;
  }
}
