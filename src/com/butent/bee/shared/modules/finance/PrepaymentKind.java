package com.butent.bee.shared.modules.finance;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;

public enum PrepaymentKind implements HasLocalizedCaption {
  CUSTOMERS {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.prepaymentCustomersShort();
    }
  },

  SUPPLIERS {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.prepaymentSuppliersShort();
    }
  };

  public String getStyleSuffix() {
    return name().toLowerCase();
  }
}
