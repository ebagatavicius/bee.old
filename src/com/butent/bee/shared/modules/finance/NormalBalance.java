package com.butent.bee.shared.modules.finance;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;

public enum NormalBalance implements HasLocalizedCaption {
  DEBIT {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.debit();
    }
  },

  CREDIT {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.credit();
    }
  };

  public static final NormalBalance DEFAULT = DEBIT;
}
