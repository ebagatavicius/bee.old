package com.butent.bee.client.menu;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.HasIdentity;
import com.butent.bee.shared.BeeConst;

/**
 * Handles a menu items separating user interface component.
 */

public class MenuSeparator extends UIObject implements HasIdentity {

  private static final String STYLENAME_DEFAULT = BeeConst.CSS_CLASS_PREFIX
      + "MenuItemSeparator";

  private MenuBar parentMenu;

  public MenuSeparator() {
    Element elem = DOM.createDiv();
    setElement(elem);

    setStyleName(elem, STYLENAME_DEFAULT);
    DomUtils.createId(this, getIdPrefix());
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "sep";
  }

  public MenuBar getParentMenu() {
    return parentMenu;
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  void setParentMenu(MenuBar parentMenu) {
    this.parentMenu = parentMenu;
  }
}
