package com.butent.bee.client.screen;

import com.google.gwt.resources.client.ImageResource;

import com.butent.bee.client.Global;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;

public enum Domain implements HasCaption {
  NEWS(Global.getImages().silverFeed(), Localized.getConstants().domainNews(), false, false),
  FAVORITES(Global.getImages().silverBookmark(), null, false, false),
  WORKSPACES(Global.getImages().workspace(), Localized.getConstants().workspaces(), false, false),
  REPORTS(Global.getImages().report(), Localized.getConstants().reports(), false, false),
  CALENDAR(Global.getImages().silverCalendar(), null, true, true),
  MAIL(Global.getImages().silverMail24(), null, true, true),
  ONLINE(Global.getImages().silverUser(), Localized.getConstants().domainOnline(), false, false),
  ROOMS(Global.getImages().silverComments(), Localized.getConstants().domainRooms(), false, false),
  ADMIN(Global.getImages().silverConfigure(), "Admin", false, true);

  private final ImageResource imageResource;
  private final String caption;

  private final boolean closable;
  private final boolean removable;

  private Domain(ImageResource imageResource, String caption, boolean closable, boolean removable) {
    this.imageResource = imageResource;
    this.caption = caption;

    this.closable = closable;
    this.removable = removable;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  ImageResource getImageResource() {
    return imageResource;
  }

  boolean isClosable() {
    return closable;
  }

  boolean isRemovable() {
    return removable;
  }
}
