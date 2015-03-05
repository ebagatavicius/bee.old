package com.butent.bee.client.menu;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.UIObject;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.HasIdentity;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.menu.MenuConstants.ItemType;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Contains attributes and getting and setting methods for a single menu item.
 */

public class MenuItem extends UIObject implements HasIdentity {

  public static final ItemType DEFAULT_TYPE = ItemType.LABEL;

  private static final String STYLENAME_DEFAULT = BeeConst.CSS_CLASS_PREFIX + "MenuItem";
  private static final String STYLENAME_SELECTED = "selected";

  private Scheduler.ScheduledCommand command;

  private MenuBar parentMenu;
  private MenuBar subMenu;

  private ItemType itemType;

  public MenuItem(MenuBar parent, String text, MenuBar subMenu) {
    init(parent, text, getDefaultType(parent));
    setSubMenu(subMenu);
  }

  public MenuItem(MenuBar parent, String text, ItemType type, MenuBar subMenu) {
    init(parent, text, type);
    setSubMenu(subMenu);
  }

  public MenuItem(MenuBar parent, String text, ItemType type, Scheduler.ScheduledCommand cmd) {
    init(parent, text, type);
    setCommand(cmd);
  }

  public MenuItem(MenuBar parent, String text, Scheduler.ScheduledCommand cmd) {
    init(parent, text, getDefaultType(parent));
    setCommand(cmd);
  }

  public Scheduler.ScheduledCommand getCommand() {
    return command;
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "menuitem";
  }

  public ItemType getItemType() {
    return itemType;
  }

  public MenuBar getParentMenu() {
    return parentMenu;
  }

  public MenuBar getSubMenu() {
    return subMenu;
  }

  public void setCommand(Scheduler.ScheduledCommand cmd) {
    this.command = cmd;
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setItemType(ItemType it) {
    this.itemType = it;
  }

  public void setParentMenu(MenuBar parentMenu) {
    this.parentMenu = parentMenu;
  }

  public void setSelected(boolean selected) {
    if (selected) {
      addStyleDependentName(STYLENAME_SELECTED);
    } else {
      removeStyleDependentName(STYLENAME_SELECTED);
    }

    switch (getItemType()) {
      case RADIO:
        DomUtils.setCheckValue(getElement(), selected);
        break;
      case OPTION:
        DomUtils.setSelected(getElement(), selected);
        break;
      default:
    }
  }

  public void setSubMenu(MenuBar subMenu) {
    this.subMenu = subMenu;
    if (this.parentMenu != null) {
      this.parentMenu.updateSubmenuIcon(this);
    }

    if (subMenu != null) {
      subMenu.getElement().setTabIndex(-1);
    }
  }

  private static ItemType getDefaultType(MenuBar parent) {
    ItemType w = null;

    if (parent != null) {
      w = parent.getItemType();
    }
    if (w == null) {
      w = DEFAULT_TYPE;
    }

    return w;
  }

  private void init(MenuBar parent, String text, ItemType type) {
    Element elem;

    switch (type) {
      case LABEL:
        elem = DomUtils.createSpan(text);
        break;
      case HTML:
        elem = DomUtils.createDiv(text);
        break;
      case BUTTON:
        elem = DomUtils.createButton(text);
        break;
      case RADIO:
        elem = DomUtils.createRadio(parent.getName(), text);
        break;
      case OPTION:
        elem = DomUtils.createOption(text);
        break;
      case LI:
        elem = DomUtils.createListItem(text);
        break;
      case DT:
        elem = DomUtils.createDefinitionItem(true, text);
        break;
      case DD:
        elem = DomUtils.createDefinitionItem(false, text);
        break;
      case ROW:
        if (BeeUtils.isEmpty(text)) {
          elem = DomUtils.createTableRow().cast();
        } else {
          elem = DomUtils.createTableRow(Lists.newArrayList(text));
        }
        break;
      default:
        Assert.untouchable();
        elem = null;
    }

    setElement(elem);

    setStyleName(STYLENAME_DEFAULT);
    addStyleDependentName(type.toString().toLowerCase());

    DomUtils.createId(this, getIdPrefix());

    setParentMenu(parent);
    setItemType(type);
  }
}
