package com.butent.bee.shared.modules.trade;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TradeDocumentData implements BeeSerializable {

  private enum Serial {
    COMPANIES, BANK_ACCOUNTS, ITEMS, CURRENCY_RATES, TOTAL_IN_WORDS
  }

  public static TradeDocumentData restore(String s) {
    TradeDocumentData tdd = new TradeDocumentData();
    tdd.deserialize(s);
    return tdd;
  }

  private BeeRowSet companies;
  private BeeRowSet bankAccounts;

  private BeeRowSet items;

  private Map<String, Double> currencyRates;
  private Map<String, String> totalInWords;

  public TradeDocumentData(BeeRowSet companies, BeeRowSet bankAccounts, BeeRowSet items,
      Map<String, Double> currencyRates, Map<String, String> totalInWords) {

    this.companies = companies;
    this.bankAccounts = bankAccounts;
    this.items = items;

    this.currencyRates = currencyRates;
    this.totalInWords = totalInWords;
  }

  private TradeDocumentData() {
  }

  public boolean containsCompany(Long id) {
    if (DataUtils.isId(id) && !DataUtils.isEmpty(getCompanies())) {
      return getCompanies().containsRow(id);
    } else {
      return false;
    }
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
        case COMPANIES:
          setCompanies(BeeRowSet.maybeRestore(value));
          break;

        case BANK_ACCOUNTS:
          setBankAccounts(BeeRowSet.maybeRestore(value));
          break;

        case ITEMS:
          setItems(BeeRowSet.maybeRestore(value));
          break;

        case CURRENCY_RATES:
          this.currencyRates = new HashMap<>();

          Map<String, String> map = Codec.deserializeMap(value);
          for (Map.Entry<String, String> entry : map.entrySet()) {
            getCurrencyRates().put(entry.getKey(), BeeUtils.toDoubleOrNull(entry.getValue()));
          }
          break;

        case TOTAL_IN_WORDS:
          setTotalInWords(Codec.deserializeMap(value));
          break;
      }
    }
  }

  public BeeRowSet getBankAccounts() {
    return bankAccounts;
  }

  public BeeRowSet getCompanies() {
    return companies;
  }

  public BeeRowSet getCompanyBankAccounts(Long id) {
    if (DataUtils.isId(id) && !DataUtils.isEmpty(getBankAccounts())) {
      BeeRowSet result = new BeeRowSet(getBankAccounts().getViewName(),
          getBankAccounts().getColumns());

      int index = getBankAccounts().getColumnIndex(ClassifierConstants.COL_COMPANY);
      for (BeeRow row : getBankAccounts()) {
        if (Objects.equals(id, row.getLong(index))) {
          result.addRow(row);
        }
      }

      return result;

    } else {
      return null;
    }
  }

  public String getCompanyValue(Long id, String colName) {
    if (DataUtils.isId(id) && !BeeUtils.isEmpty(colName) && !DataUtils.isEmpty(getCompanies())) {
      BeeRow row = getCompanies().getRowById(id);
      int index = getCompanies().getColumnIndex(colName);

      if (row != null && !BeeConst.isUndef(index)) {
        return row.getString(index);
      }
    }
    return null;
  }

  public Map<String, Double> getCurrencyRates() {
    return currencyRates;
  }

  public BeeRowSet getItems() {
    return items;
  }

  public Map<String, String> getTotalInWords() {
    return totalInWords;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case COMPANIES:
          arr[i++] = getCompanies();
          break;

        case BANK_ACCOUNTS:
          arr[i++] = getBankAccounts();
          break;

        case ITEMS:
          arr[i++] = getItems();
          break;

        case CURRENCY_RATES:
          arr[i++] = getCurrencyRates();
          break;

        case TOTAL_IN_WORDS:
          arr[i++] = getTotalInWords();
          break;
      }
    }

    return Codec.beeSerialize(arr);
  }

  private void setBankAccounts(BeeRowSet bankAccounts) {
    this.bankAccounts = bankAccounts;
  }

  private void setCompanies(BeeRowSet companies) {
    this.companies = companies;
  }

  private void setItems(BeeRowSet items) {
    this.items = items;
  }

  private void setTotalInWords(Map<String, String> totalInWords) {
    this.totalInWords = totalInWords;
  }
}
