package com.butent.bee.client.grid;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

public enum CellKind {
  LABEL("label", "LabelCell"), INPUT("input", "InputCell");

  public static CellKind parse(String code) {
    for (CellKind kind : CellKind.values()) {
      if (BeeUtils.same(kind.getCode(), code)) {
        return kind;
      }
    }
    return null;
  }

  private final String code;
  private final String styleName;

  private CellKind(String code, String styleSuffix) {
    this.code = code;
    this.styleName = BeeConst.CSS_CLASS_PREFIX + styleSuffix;
  }

  public String getCode() {
    return code;
  }

  public String getStyleName() {
    return styleName;
  }
}
