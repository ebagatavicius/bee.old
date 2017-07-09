package com.butent.bee.shared.ui;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

public enum WindowType implements HasLocalizedCaption {
  NEW_TAB("new-tab") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.windowNewTab();
    }
  },

  ON_TOP("on-top") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.windowOnTop();
    }
  },

  DETACHED("detached") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.windowDetached();
    }
  },

  MODAL("modal") {
    @Override
    public String getCaption(Dictionary dictionary) {
      return dictionary.windowModal();
    }
  };

  private final String code;

  WindowType(String code) {
    this.code = code;
  }

  public static final WindowType DEFAULT_GRID_EDIT = ON_TOP;
  public static final WindowType DEFAULT_GRID_NEW_ROW = ON_TOP;

  public static final WindowType DEFAULT_CHILD_EDIT = MODAL;
  public static final WindowType DEFAULT_CHILD_NEW_ROW = MODAL;

  public static WindowType parse(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;

    } else {
      for (WindowType type : values()) {
        if (BeeUtils.same(type.code, input)) {
          return type;
        }
      }

      return EnumUtils.getEnumByName(WindowType.class, input);
    }
  }
}
