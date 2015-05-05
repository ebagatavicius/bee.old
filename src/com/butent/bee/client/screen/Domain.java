package com.butent.bee.client.screen;

import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;

public enum Domain implements HasCaption {
  NEWS(FontAwesome.RSS, Localized.getConstants().domainNews(), false, false, 100),
  FAVORITES(FontAwesome.STAR_O, null, false, false, 60),
  WORKSPACES(FontAwesome.NEWSPAPER_O, Localized.getConstants().workspaces(), false, false, 100),
  REPORTS(FontAwesome.FILE_TEXT_O, Localized.getConstants().reports(), false, false, 100),
  CALENDAR(FontAwesome.CALENDAR, null, true, true, 400),
  MAIL(FontAwesome.ENVELOPE_O, null, true, true, 200),
  ONLINE(FontAwesome.USERS, Localized.getConstants().domainOnline(), false, false, 100),
  ROOMS(FontAwesome.COMMENTS_O, Localized.getConstants().domainRooms(), false, false, 100),
  ADMIN(FontAwesome.MAGIC, "Admin", false, true, 300);

  private final FontAwesome icon;
  private final String caption;

  private final boolean closable;
  private final boolean removable;

  private final int minHeight;

  private Domain(FontAwesome icon, String caption, boolean closable, boolean removable,
      int minHeight) {

    this.icon = icon;
    this.caption = caption;

    this.closable = closable;
    this.removable = removable;

    this.minHeight = minHeight;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  FontAwesome getIcon() {
    return icon;
  }

  int getMinHeight() {
    return minHeight;
  }

  boolean isClosable() {
    return closable;
  }

  boolean isRemovable() {
    return removable;
  }
}
