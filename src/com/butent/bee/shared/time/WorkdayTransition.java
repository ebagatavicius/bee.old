package com.butent.bee.shared.time;

import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;

public enum WorkdayTransition implements HasCaption {
  NONE(Localized.getConstants().workdayTransitionNone()),
  NEAREST(Localized.getConstants().workdayTransitionNearest()),
  FORWARD(Localized.getConstants().workdayTransitionForward()),
  BACKWARD(Localized.getConstants().workdayTransitionBackward());

  public static final WorkdayTransition DEFAULT = NEAREST;

  private final String caption;

  private WorkdayTransition(String caption) {
    this.caption = caption;
  }

  @Override
  public String getCaption() {
    return caption;
  }
}
