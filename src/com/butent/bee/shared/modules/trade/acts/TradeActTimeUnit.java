package com.butent.bee.shared.modules.trade.acts;

import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;

public enum TradeActTimeUnit implements HasLocalizedCaption {
  DAY {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.taTimeUnitDay();
    }
  },
  MONTH {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.taTimeUnitMonth();
    }
  };

  @Override
  public String getCaption() {
    return getCaption(Localized.getConstants());
  }
}
