package com.butent.bee.shared.menu;

/**
 * Contains list of constants for various types of menus and checks whether a given menu object fits
 * certain criteria (for example {@code isRootLevel} or {@code isValidLayout}.
 */

public final class MenuConstants {

  /**
   * Contains a list of possible menu types.
   */
  public enum BarType {
    TABLE, FLOW, LIST, OLIST, ULIST, DLIST
  }

  /**
   * Lists all possible menu item types.
   */
  public enum ItemType {
    LABEL, BUTTON, RADIO, HTML, OPTION, LI, DT, DD, ROW
  }

  public static final int MAX_MENU_DEPTH = 4;
  public static final int ROOT_MENU_INDEX = 0;

  public static final int DEFAULT_ROOT_LIMIT = 0;
  public static final int DEFAULT_ITEM_LIMIT = 0;

  public static final String LAYOUT_MENU_HOR = "menu horizontal";
  public static final String LAYOUT_MENU_VERT = "menu vertical";

  public static final String LAYOUT_TREE = "simple tree";

  public static final String LAYOUT_LIST = "option list";
  public static final String LAYOUT_ORDERED_LIST = "ordered list";
  public static final String LAYOUT_UNORDERED_LIST = "unordered list";
  public static final String LAYOUT_DEFINITION_LIST = "definition list";

  public static final String LAYOUT_RADIO_HOR = "radio horizontal";
  public static final String LAYOUT_RADIO_VERT = "radio vertical";
  public static final String LAYOUT_BUTTONS_HOR = "buttons horizontal";
  public static final String LAYOUT_BUTTONS_VERT = "buttons vertical";

  public static final String DEFAULT_ROOT_LAYOUT = LAYOUT_MENU_HOR;
  public static final String DEFAULT_ITEM_LAYOUT = LAYOUT_MENU_VERT;

  public static final int SEPARATOR_BEFORE = 1;
  public static final int SEPARATOR_AFTER = 2;

  public static boolean isRootLevel(int idx) {
    return idx == ROOT_MENU_INDEX;
  }

  public static boolean isSeparatorAfter(int sep) {
    return (sep & SEPARATOR_AFTER) != 0;
  }

  public static boolean isSeparatorBefore(int sep) {
    return (sep & SEPARATOR_BEFORE) != 0;
  }

  private MenuConstants() {
  }
}
