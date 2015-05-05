package com.butent.bee.shared.report;

import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;

public enum ReportFunction implements HasLocalizedCaption {
  MIN() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.least();
    }
  },
  MAX() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.greatest();
    }
  },
  SUM() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.amount();
    }
  },
  COUNT() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.quantity();
    }
  },
  LIST() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.list();
    }
  };

  @Override
  public String getCaption() {
    return getCaption(Localized.getConstants());
  }
}