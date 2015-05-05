package com.butent.bee.shared.report;

import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;

public enum DateTimeFunction implements HasLocalizedCaption {
  YEAR() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.year();
    }
  },
  QUATER() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.quater();
    }
  },
  MONTH() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.month();
    }
  },
  DAY() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.day();
    }
  },
  DAY_OF_WEEK() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.dayOfWeek();
    }
  },
  DATE() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.date();
    }
  },
  DATETIME() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.dateTime();
    }
  },
  HOUR() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.hour();
    }
  },
  MINUTE() {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.minute();
    }
  };

  @Override
  public String getCaption() {
    return getCaption(Localized.getConstants());
  }
}