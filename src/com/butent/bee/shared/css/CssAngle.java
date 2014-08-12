package com.butent.bee.shared.css;

import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum CssAngle implements HasCaption {
  DEG("deg"),
  GRAD("grad"),
  RAD("rad"),
  TURN("turn");

  public static final CssAngle DEFAULT = DEG;

  public static String format(double value, CssAngle angle) {
    return BeeUtils.toString(value) + normalize(angle).getCaption();
  }

  public static String format(int value, CssAngle angle) {
    return BeeUtils.toString(value) + normalize(angle).getCaption();
  }

  public static CssAngle normalize(CssAngle angle) {
    return (angle == null) ? DEFAULT : angle;
  }

  public static CssAngle parse(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }

    for (CssAngle angle : CssAngle.values()) {
      if (BeeUtils.same(angle.getCaption(), input)) {
        return angle;
      }
    }
    return null;
  }

  public static CssAngle parse(String input, CssAngle defAngle) {
    return BeeUtils.nvl(parse(input), defAngle);
  }

  private final String caption;

  private CssAngle(String caption) {
    this.caption = caption;
  }

  @Override
  public String getCaption() {
    return caption;
  }
}
