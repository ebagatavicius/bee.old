package com.butent.bee.shared.modules.projects;

import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;

/**
 * Priorities of project.
 */
public enum ProjectPriority implements HasCaption {
  /**
   * Low priority type of project.
   */
  LOW(Localized.getConstants().lowPriority()),

  /**
   * Medium priority type of project.
   */
  MEDIUM(Localized.getConstants().mediumPriority()),

  /**
   * High priority type of project.
   */
  HIGH(Localized.getConstants().highPriority());

  private String caption;

  private ProjectPriority(String caption) {
    this.caption = caption;
  }

  @Override
  public String getCaption() {
    return caption;
  }

}
