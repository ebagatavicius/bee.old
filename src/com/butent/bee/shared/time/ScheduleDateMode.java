package com.butent.bee.shared.time;

import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;

public enum ScheduleDateMode implements HasCaption {
  INCLUDE(Localized.getConstants().scheduleDateInclude()),
  EXCLUDE(Localized.getConstants().scheduleDateExclude()),
  WORK(Localized.getConstants().scheduleDateWork()),
  NON_WORK(Localized.getConstants().scheduleDateNonWork());

  private final String caption;

  private ScheduleDateMode(String caption) {
    this.caption = caption;
  }

  @Override
  public String getCaption() {
    return caption;
  }
}
