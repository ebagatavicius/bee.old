package com.butent.bee.shared.modules.projects;

import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;

/**
 * User types of project.
 */
@Deprecated
public enum ProjectUserType implements HasCaption {
  /**
   * The team members of project.
   */
  PARTICIPANT(Localized.getConstants().prjParticipant()),

  /**
   * Read-only access members of project.
   */
  OBSERVER(Localized.getConstants().prjObserver());

  private final String caption;

  private ProjectUserType(String caption) {
    this.caption = caption;
  }

  @Override
  public String getCaption() {
    return caption;
  }

}
