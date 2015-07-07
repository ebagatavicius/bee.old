package com.butent.bee.client.menu;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.CloseEvent;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.menu.MenuConstants;
import com.butent.bee.shared.menu.MenuConstants.BarType;
import com.butent.bee.shared.menu.MenuConstants.ItemType;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains menu object and core menu handling methods like {@code addItem} or {@code doCommand}.
 */

public class MenuBar extends Widget implements IdentifiableWidget, CloseEvent.Handler {

  /**
   * Specifies implementing classes to have an image resource.
   */

  private static Widget subMenuIcon;

  private static final String STYLENAME_DEFAULT = BeeConst.CSS_CLASS_PREFIX + "MenuBar";
  private static final String STYLENAME_ROOT = BeeConst.CSS_CLASS_PREFIX + "MenuRoot";

  static {
    subMenuIcon = new FaLabel(FontAwesome.CARET_RIGHT);
  }

  private final List<UIObject> allItems = new ArrayList<>();
  private final List<MenuItem> items = new ArrayList<>();

  private Element body;

  private MenuBar parentMenu;
  private MenuPopup popup;
  private MenuItem selectedItem;
  private MenuBar childMenu;

  private final int level;
  private final boolean vertical;

  private final BarType barType;
  private final ItemType itemType;
  private final String name = NameUtils.createUniqueName("mb-");

  public MenuBar() {
    this(0);
  }

  public MenuBar(int level) {
    this(level, false);
  }

  public MenuBar(int level, boolean vert) {
    this(level, vert, null);
  }

  public MenuBar(int level, boolean vert, BarType bt) {
    this(level, vert, bt, null);
  }

  public MenuBar(int level, boolean vert, BarType bt, ItemType it) {
    this(level, vert, bt, it, false);
  }

  public MenuBar(int level, boolean vert, BarType bt, ItemType it, boolean wheel) {
    this.level = level;
    this.vertical = vert;

    this.barType = (bt == null) ? BarType.TABLE : bt;
    this.itemType = (it == null) ? MenuItem.DEFAULT_TYPE : it;

    init(wheel);
    DomUtils.createId(this, getIdPrefix());
  }

  public MenuItem addItem(MenuItem item) {
    return insertItem(item, allItems.size());
  }

  public MenuItem addItem(String text, ItemType type, MenuBar mb) {
    return addItem(new MenuItem(this, text, type, mb));
  }

  public MenuItem addItem(String text, ItemType type, MenuCommand cmd) {
    return addItem(new MenuItem(this, text, type, cmd));
  }

  public MenuItem addItem(String text, MenuBar mb) {
    return addItem(new MenuItem(this, text, getItemType(), mb));
  }

  public MenuItem addItem(String text, MenuCommand cmd) {
    return addItem(new MenuItem(this, text, getItemType(), cmd));
  }

  public MenuSeparator addSeparator() {
    return addSeparator(new MenuSeparator());
  }

  public MenuSeparator addSeparator(MenuSeparator separator) {
    return insertSeparator(separator, allItems.size());
  }

  public void clearItems() {
    closeAll();

    selectItem(null);

    Element container = getItemContainerElement();
    while (DOM.getChildCount(container) > 0) {
      container.removeChild(DOM.getChild(container, 0));
    }

    items.clear();
    allItems.clear();
  }

  public void closePopup() {
    if (popup != null) {
      popup.close();
    }
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "menubar";
  }

  public MenuItem getItem(int index) {
    if (index >= 0 && index < items.size()) {
      return items.get(index);
    } else {
      return null;
    }
  }

  public int getItemCount() {
    return items.size();
  }

  public List<MenuItem> getItems() {
    return items;
  }

  public ItemType getItemType() {
    return itemType;
  }

  public int getLevel() {
    return level;
  }

  public String getName() {
    return name;
  }

  public MenuItem getSelectedItem() {
    return selectedItem;
  }

  public boolean isVertical() {
    return vertical;
  }

  public void moveDown() {
    if (selectFirstItemIfNoneSelected()) {
      return;
    }

    if (vertical) {
      selectNextItem();
    } else if (hasSubMenu(selectedItem)) {
      activateSubMenu(selectedItem);
    } else if (parentMenu != null) {
      parentMenu.focus();
      parentMenu.selectNextItem();
    }
  }

  public void moveLeft() {
    if (selectFirstItemIfNoneSelected()) {
      return;
    }

    if (!vertical) {
      selectPrevItem();
    } else if (parentMenu != null) {
      parentMenu.focus();
      parentMenu.selectPrevItem();
    } else if (hasSubMenu(selectedItem)) {
      activateSubMenu(selectedItem, true);
    }
  }

  public void moveRight() {
    if (selectFirstItemIfNoneSelected()) {
      return;
    }

    if (!vertical) {
      selectNextItem();
    } else if (hasSubMenu(selectedItem)) {
      activateSubMenu(selectedItem);
    } else if (parentMenu != null) {
      parentMenu.focus();
      parentMenu.selectNextItem();
    }
  }

  public void moveUp() {
    if (selectFirstItemIfNoneSelected()) {
      return;
    }

    if (vertical) {
      selectPrevItem();
    } else if (parentMenu != null) {
      parentMenu.focus();
      parentMenu.selectPrevItem();
    } else if (hasSubMenu(selectedItem)) {
      activateSubMenu(selectedItem, true);
    }
  }

  @Override
  public void onBrowserEvent(Event event) {
    Element target = DOM.eventGetTarget(event);
    int type = DOM.eventGetType(event);

    if (type == Event.ONBLUR) {
      if (childMenu == null) {
        selectItem(null);
      }
      super.onBrowserEvent(event);
      return;
    }

    MenuItem item = findItem(target);
    if (item == null && !EventUtils.isKeyEvent(type) && type != Event.ONMOUSEWHEEL) {
      super.onBrowserEvent(event);
      return;
    }

    switch (type) {
      case Event.ONCLICK:
        if (!DomUtils.isLabelElement(target)) {
          if (hasCommand(item)) {
            doCommand(item);
          } else if (hasSubMenu(item)) {
            selectItem(item);
            openSubMenu(item);
          }
        }
        break;

      case Event.ONMOUSEWHEEL:
        int mwv = event.getMouseWheelVelocityY();
        if (mwv != 0) {
          if (mwv > 0) {
            selectNextItem();
          } else {
            selectPrevItem();
          }
          eatEvent(event);
        }
        break;

      case Event.ONKEYPRESS:
        if (moveTo(event.getCharCode())) {
          eatEvent(event);
        }
        break;

      case Event.ONKEYDOWN:
        int keyCode = event.getKeyCode();

        switch (keyCode) {
          case KeyCodes.KEY_LEFT:
            moveLeft();
            eatEvent(event);
            break;

          case KeyCodes.KEY_RIGHT:
            moveRight();
            eatEvent(event);
            break;

          case KeyCodes.KEY_UP:
            moveUp();
            eatEvent(event);
            break;

          case KeyCodes.KEY_DOWN:
            moveDown();
            eatEvent(event);
            break;

          case KeyCodes.KEY_HOME:
          case KeyCodes.KEY_PAGEUP:
            if (items.size() > 1) {
              selectItem(0);
              eatEvent(event);
            }
            break;

          case KeyCodes.KEY_END:
          case KeyCodes.KEY_PAGEDOWN:
            if (items.size() > 1) {
              selectItem(items.size() - 1);
              eatEvent(event);
            }
            break;

          case KeyCodes.KEY_ESCAPE:
            closeAll();
            eatEvent(event);
            break;

          case KeyCodes.KEY_TAB:
            closeAll();
            break;

          case KeyCodes.KEY_DELETE:
          case KeyCodes.KEY_BACKSPACE:
            if (parentMenu != null) {
              parentMenu.focus();
              parentMenu.closeChildMenu();
              eatEvent(event);
            }
            break;

          case KeyCodes.KEY_ENTER:
            if (!selectFirstItemIfNoneSelected()) {
              if (hasCommand(selectedItem)) {
                doCommand(selectedItem);
              } else if (hasSubMenu(selectedItem)) {
                activateSubMenu(selectedItem);
              }
              eatEvent(event);
            }
            break;
        }
        break;
    }

    super.onBrowserEvent(event);
  }

  @Override
  public void onClose(CloseEvent event) {
    childMenu = null;
    popup = null;

    if (event.userCaused() && parentMenu == null) {
      selectItem(null);
    }
  }

  public void prepare() {
    if (barType == BarType.LIST && items.size() > 1) {
      SelectElement.as(body).setSize(items.size());
    }
  }

  public void selectFirstItem() {
    if (!items.isEmpty()) {
      selectItem(items.get(0));
    }
  }

  public void selectItem(MenuItem item) {
    if (item == selectedItem) {
      return;
    }

    if (selectedItem != null) {
      selectedItem.setSelected(false);
      closeChildMenu();
    }

    if (item != null) {
      DomUtils.scrollIntoView(item.getElement());
      item.setSelected(true);
    }

    selectedItem = item;
  }

  public void selectLastItem() {
    if (!items.isEmpty()) {
      selectItem(items.get(items.size() - 1));
    }
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void updateSubmenuIcon(MenuItem item) {
    if (barType != BarType.TABLE) {
      return;
    }
    if (!vertical) {
      return;
    }

    MenuBar submenu = item.getSubMenu();
    if (submenu == null) {
      return;
    }

    int idx = allItems.indexOf(item);
    if (idx == -1) {
      return;
    }

    Element container = getItemContainerElement();
    Element tr = DOM.getChild(container, idx);
    int tdCount = DOM.getChildCount(tr);
    if (tdCount == 1) {
      Element td = DOM.createTD();
      td.setPropertyString("vAlign", "middle");
      td.setInnerHTML(subMenuIcon.toString());
      setStyleName(td, "subMenuIcon");
      DOM.appendChild(tr, td);
    }
  }

  @Override
  protected void onDetach() {
    closePopup();
    super.onDetach();
  }

  private void activateSubMenu(MenuItem item) {
    activateSubMenu(item, false);
  }

  private void activateSubMenu(MenuItem item, boolean end) {
    if (!hasSubMenu(item)) {
      return;
    }

    openSubMenu(item);

    if (end) {
      item.getSubMenu().selectLastItemIfNoneSelected();
    } else {
      item.getSubMenu().selectFirstItemIfNoneSelected();
    }

    if (hasSubMenu(item.getSubMenu().selectedItem)) {
      item.getSubMenu().openSubMenu(item.getSubMenu().selectedItem);
    }

    item.getSubMenu().focus();
  }

  private void addItemElement(int beforeIndex, Element elem) {
    if (barType != BarType.TABLE) {
      DOM.insertChild(body, elem, beforeIndex);
      return;

    } else if (vertical) {
      if (DomUtils.isTableRowElement(elem)) {
        DOM.insertChild(body, elem, beforeIndex);
      } else {
        Element tr = DOM.createTR();
        DOM.insertChild(body, tr, beforeIndex);
        if (DomUtils.isTableCellElement(elem)) {
          DOM.appendChild(tr, elem);
        } else {
          Element td = DOM.createTD();
          DOM.appendChild(td, elem);
          DOM.appendChild(tr, td);
        }
      }

    } else {
      Element tr = DOM.getChild(body, 0);
      if (DomUtils.isTableCellElement(elem)) {
        DOM.insertChild(tr, elem, beforeIndex);
      } else {
        Element td = DOM.createTD();
        DOM.appendChild(td, elem);
        DOM.insertChild(tr, td, beforeIndex);
      }
    }
  }

  private void closeAll() {
    selectItem(null);
    closeChildMenu();
    if (parentMenu != null) {
      parentMenu.closeAll();
    }
  }

  private void closeChildMenu() {
    if (childMenu != null) {
      childMenu.closeChildMenu();
    }
    closePopup();
    childMenu = null;
  }

  private void doCommand(MenuItem item) {
    closeAll();
    Scheduler.get().scheduleDeferred(item.getCommand());
  }

  private static void eatEvent(Event event) {
    event.stopPropagation();
    event.preventDefault();
  }

  private MenuItem findItem(Element elem) {
    for (MenuItem item : items) {
      if (item.getElement().isOrHasChild(elem)
          || DomUtils.isTdElement(elem) && elem.equals(item.getElement().getParentElement())) {
        return item;
      }
    }
    return null;
  }

  private void focus() {
    DomUtils.setFocus(this, true);
  }

  private Element getItemContainerElement() {
    if (vertical) {
      return body;
    } else {
      return DOM.getChild(body, 0);
    }
  }

  private static boolean hasCommand(MenuItem item) {
    if (item == null) {
      return false;
    } else {
      return item.getCommand() != null;
    }
  }

  private static boolean hasSubMenu(MenuItem item) {
    if (item == null) {
      return false;
    } else {
      return item.getSubMenu() != null;
    }
  }

  private void init(boolean wheel) {

    Element elem;
    switch (barType) {
      case FLOW:
        elem = DOM.createDiv();
        body = elem;
        break;

      case LIST:
        elem = DOM.createSelect();
        body = elem;
        break;

      case OLIST:
        elem = Document.get().createOLElement().cast();
        body = elem;
        break;

      case ULIST:
        elem = Document.get().createULElement().cast();
        body = elem;
        break;

      case DLIST:
        elem = Document.get().createDLElement().cast();
        body = elem;
        break;

      default:
        elem = DOM.createTable();
        body = DOM.createTBody();
        DOM.appendChild(elem, body);

        if (!isVertical()) {
          Element tr = DOM.createTR();
          DOM.appendChild(body, tr);
        }
    }

    Element outer = DOM.createDiv();
    DomUtils.makeFocusable(outer);
    DOM.appendChild(outer, elem);
    setElement(outer);

    sinkEvents(Event.ONCLICK | Event.ONKEYDOWN | Event.ONKEYPRESS | Event.ONBLUR);
    if (wheel) {
      sinkEvents(Event.ONMOUSEWHEEL);
    }

    setStyleName(MenuConstants.isRootLevel(getLevel()) ? STYLENAME_ROOT : STYLENAME_DEFAULT);
    if (getLevel() > 0) {
      addStyleDependentName("level-" + getLevel());
    }
    if (isVertical()) {
      addStyleDependentName(StyleUtils.SUFFIX_VERTICAL);
    } else {
      addStyleDependentName(StyleUtils.SUFFIX_HORIZONTAL);
    }

    addStyleDependentName(barType.name().toLowerCase());

    StyleUtils.hideOutline(outer);
  }

  private MenuItem insertItem(MenuItem item, int beforeIndex) {
    allItems.add(beforeIndex, item);
    int itemsIndex = 0;
    for (int i = 0; i < beforeIndex; i++) {
      if (allItems.get(i) instanceof MenuItem) {
        itemsIndex++;
      }
    }
    items.add(itemsIndex, item);

    addItemElement(beforeIndex, item.getElement());
    item.setParentMenu(this);
    item.setSelected(false);
    updateSubmenuIcon(item);

    return item;
  }

  private MenuSeparator insertSeparator(MenuSeparator separator, int beforeIndex) {
    addItemElement(beforeIndex, separator.getElement());
    separator.setParentMenu(this);
    allItems.add(beforeIndex, separator);

    return separator;
  }

  private boolean moveTo(int code) {
    boolean ok = false;
    if (code <= BeeConst.CHAR_SPACE) {
      return ok;
    }

    int n = items.size();
    if (n <= 1) {
      return ok;
    }

    int oldIdx = (selectedItem == null) ? -1 : items.indexOf(selectedItem);
    int idx = oldIdx;

    String search = new String(new char[] {(char) code});
    String txt;

    for (int i = 0; i < ((oldIdx >= 0) ? n - 1 : n); i++) {
      idx++;
      if (idx < 0 || idx >= n) {
        idx = 0;
      }
      if (idx == oldIdx) {
        continue;
      }

      txt = items.get(idx).getElement().getInnerText();
      if (BeeUtils.isEmpty(txt)) {
        continue;
      }
      if (txt.substring(0, 1).equalsIgnoreCase(search)) {
        ok = true;
        break;
      }
    }

    if (ok) {
      selectItem(idx);
    }
    return ok;
  }

  private void openPopup(MenuItem item) {
    popup = new MenuPopup(this, item);

    childMenu = item.getSubMenu();
    childMenu.parentMenu = this;
    childMenu.selectItem(null);

    popup.addCloseHandler(this);

    popup.setPopupPositionAndShow(new MenuPositionCallback(getElement(),
        item.getElement(), popup, vertical));
  }

  private void openSubMenu(MenuItem item) {
    if (childMenu == null) {
      openPopup(item);
    } else if (item.getSubMenu() != childMenu) {
      closeChildMenu();
      openPopup(item);
    }
  }

  private boolean selectFirstItemIfNoneSelected() {
    if (selectedItem == null) {
      if (!items.isEmpty()) {
        MenuItem item = items.get(0);
        selectItem(item);
      }
      return true;
    }
    return false;
  }

  private void selectItem(int index) {
    Assert.isIndex(items, index);
    if (selectedItem != null && index == items.indexOf(selectedItem)) {
      return;
    }

    MenuItem item = items.get(index);
    selectItem(item);

    if (hasSubMenu(item)) {
      openSubMenu(item);
    }
  }

  private boolean selectLastItemIfNoneSelected() {
    if (selectedItem == null) {
      if (!items.isEmpty()) {
        MenuItem item = items.get(items.size() - 1);
        selectItem(item);
      }
      return true;
    }
    return false;
  }

  private void selectNextItem() {
    if (selectedItem == null || items.size() <= 1) {
      return;
    }

    int index = items.indexOf(selectedItem);
    if (index < items.size() - 1) {
      index++;
    } else {
      index = 0;
    }

    selectItem(index);
  }

  private void selectPrevItem() {
    if (selectedItem == null || items.size() <= 1) {
      return;
    }

    int index = items.indexOf(selectedItem);
    if (index > 0) {
      index--;
    } else {
      index = items.size() - 1;
    }

    selectItem(index);
  }
}
