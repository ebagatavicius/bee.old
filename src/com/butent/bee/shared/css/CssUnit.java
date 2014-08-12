package com.butent.bee.shared.css;

import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum CssUnit implements HasCaption {
  PX("px", Type.ABSOLUTE),
  PCT("%", Type.CONTAINER_PERCENTAGE),
  EM("em", Type.FONT_RELATIVE),
  EX("ex", Type.FONT_RELATIVE),
  PT("pt", Type.ABSOLUTE),
  PC("pc", Type.ABSOLUTE),
  IN("in", Type.ABSOLUTE),
  CM("cm", Type.ABSOLUTE),
  MM("mm", Type.ABSOLUTE),
  CH("ch", Type.FONT_RELATIVE),
  REM("rem", Type.FONT_RELATIVE),
  VW("vw", Type.VIEWPORT_PERCENTAGE),
  VH("vh", Type.VIEWPORT_PERCENTAGE),
  VMIN("vmin", Type.VIEWPORT_PERCENTAGE),
  VMAX("vmax", Type.VIEWPORT_PERCENTAGE);

  public enum Type {
    ABSOLUTE, FONT_RELATIVE, VIEWPORT_PERCENTAGE, CONTAINER_PERCENTAGE
  }

  public static final CssUnit DEFAULT = PX;

  public static String format(double value, CssUnit unit) {
    return BeeUtils.toString(value) + normalize(unit).getCaption();
  }

  public static String format(int value, CssUnit unit) {
    return BeeUtils.toString(value) + normalize(unit).getCaption();
  }

  public static CssUnit normalize(CssUnit unit) {
    return (unit == null) ? DEFAULT : unit;
  }

  public static CssUnit parse(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }

    for (CssUnit unit : CssUnit.values()) {
      if (BeeUtils.same(unit.getCaption(), input)) {
        return unit;
      }
    }
    return null;
  }

  public static CssUnit parse(String input, CssUnit defUnit) {
    return BeeUtils.nvl(parse(input), defUnit);
  }

  private final String caption;
  private final Type type;

  private CssUnit(String caption, Type type) {
    this.caption = caption;
    this.type = type;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  public Type getType() {
    return type;
  }

  public boolean isPercentage() {
    return type == Type.CONTAINER_PERCENTAGE || type == Type.VIEWPORT_PERCENTAGE;
  }
}
