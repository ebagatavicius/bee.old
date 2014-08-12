package com.butent.bee.shared.ui;

import com.butent.bee.shared.utils.BeeUtils;

public enum RefreshType {
  CELL("cell"), ROW("row");

  public static final String ATTR_UPDATE_MODE = "updateMode";

  public static RefreshType getByCode(String code) {
    if (!BeeUtils.isEmpty(code)) {
      for (RefreshType type : RefreshType.values()) {
        if (BeeUtils.same(type.getCode(), code)) {
          return type;
        }
      }
    }
    return null;
  }

  private final String code;

  private RefreshType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
