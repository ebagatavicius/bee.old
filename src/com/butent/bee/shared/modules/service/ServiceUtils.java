package com.butent.bee.shared.modules.service;

import com.butent.bee.shared.utils.BeeUtils;

public final class ServiceUtils {

  public static double calculateBasicAmount(double amountWithoutVat, double cost, double quantity) {
    return BeeUtils.round(amountWithoutVat, 2) - (cost * quantity);
  }

  public static double calculateSalary(double tariff, double basicAmount) {
    return BeeUtils.isDouble(tariff) ? basicAmount * tariff / 100 : 0;
  }

  public static double calculateTariff(double salary, double basicAmount) {
    return salary * 100 / basicAmount;
  }

  private ServiceUtils() {
  }
}