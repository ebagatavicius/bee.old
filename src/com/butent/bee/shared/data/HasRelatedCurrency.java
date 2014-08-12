package com.butent.bee.shared.data;

public interface HasRelatedCurrency {

  String ATTR_CURRENCY_SOURCE = "currencySource";

  String getCurrencySource();

  void setCurrencySource(String currencySource);
}
