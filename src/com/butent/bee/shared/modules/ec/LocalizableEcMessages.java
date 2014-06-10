package com.butent.bee.shared.modules.ec;

public interface LocalizableEcMessages {

  String ecCategoryMigrate(String source, String destination);

  String ecInMyCart(int count);

  String ecLocateAnalogs(int count);

  String ecOrderId(String id);

  String ecOrderTotal(String amount, String currency);

  String ecRegistrationMailContent(String login, String password, String url);

  String ecRegistrationMailSubject(String companyName);

  String ecRemoveCartItem(String cart, String item);

  String ecSearchDidNotMatch(String query);

  String ecUpdateCartItem(String cart, String item, int quantity);
}
