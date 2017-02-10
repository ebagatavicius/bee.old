package com.butent.bee.client.data;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.Defaults;
import com.butent.bee.shared.utils.BeeUtils;

public class ClientDefaults extends Defaults {

  private static final Pair<Long, String> currency = Pair.empty();

  @Override
  public Object getValue(DefaultExpression defExpr, Object defValue) {
    Object value = null;

    if (defExpr == null) {
      value = defValue;
    } else {
      switch (defExpr) {
        case CURRENT_USER:
          value = BeeKeeper.getUser().getUserId();
          break;

        case MAIN_CURRENCY:
          value = getCurrency();
          break;

        case NEXT_NUMBER:
          break;

        default:
          value = super.getValue(defExpr, defValue);
          break;
      }
    }
    return value;
  }

  public static Long getCurrency() {
    return currency.getA();
  }

  public static String getCurrencyName() {
    return currency.getB();
  }

  public static void setCurrency(Pair<String, String> currencyInfo) {
    currency.setA(BeeUtils.toLongOrNull(currencyInfo.getA()));
    currency.setB(currencyInfo.getB());
  }
}
