package com.butent.bee.shared.ui;

import com.butent.bee.shared.utils.BeeUtils;

/**
 * Contains a list of possible cell types.
 */

public enum CellType {
  HTML("html"), INPUT("input"), DIV("div");

  public static CellType getByCode(String code) {
    if (!BeeUtils.isEmpty(code)) {
      for (CellType type : CellType.values()) {
        if (BeeUtils.same(type.getCode(), code)) {
          return type;
        }
      }
    }
    return null;
  }

  private final String code;

  private CellType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
