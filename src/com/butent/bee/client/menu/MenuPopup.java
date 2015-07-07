package com.butent.bee.client.menu;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.Theme;
import com.butent.bee.shared.BeeConst;

public class MenuPopup extends Popup {

  private static final String STYLENAME_DEFAULT = BeeConst.CSS_CLASS_PREFIX + "MenuPopup";

  private MenuBar parentMenu;
  private MenuItem parentItem;

  public MenuPopup(MenuBar bar, MenuItem item) {
    super(OutsideClick.CLOSE);
    setStyleName(STYLENAME_DEFAULT);
    addStyleDependentName(item.getSubMenu().getItemType().name().toLowerCase());

    int lineHeight = Theme.getSubMenuLineHeight();
    if (lineHeight > 0) {
      int cnt = item.getSubMenu().getItemCount();
      int max = BeeKeeper.getScreen().getScreenPanel().getCenterHeight();

      if (cnt * (lineHeight + 8) < max) {
        StyleUtils.setLineHeight(this, lineHeight);
      }
    }

    this.parentMenu = bar;
    this.parentItem = item;

    setWidget(parentItem.getSubMenu());
  }

  @Override
  public String getIdPrefix() {
    return "menupopup";
  }

  public MenuItem getParentItem() {
    return parentItem;
  }

  public MenuBar getParentMenu() {
    return parentMenu;
  }

  public void setParentItem(MenuItem parentItem) {
    this.parentItem = parentItem;
  }

  public void setParentMenu(MenuBar parentMenu) {
    this.parentMenu = parentMenu;
  }
}
