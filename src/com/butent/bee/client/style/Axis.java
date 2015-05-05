package com.butent.bee.client.style;

import com.butent.bee.shared.css.CssAngle;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.utils.BeeUtils;

public enum Axis {
  X("X"), Y("Y"), Z("Z");

  private final String suffix;

  private Axis(String suffix) {
    this.suffix = suffix;
  }

  public String rotate(double value, CssAngle angle) {
    return StyleUtils.TRANSFORM_ROTATE + suffix
        + BeeUtils.parenthesize(CssAngle.format(value, angle));
  }

  public String scale(double value) {
    return StyleUtils.TRANSFORM_SCALE + suffix + BeeUtils.parenthesize(value);
  }

  public String skew(double value, CssAngle angle) {
    return StyleUtils.TRANSFORM_SKEW + suffix
        + BeeUtils.parenthesize(CssAngle.format(value, angle));
  }

  public String translate(double value, CssUnit unit) {
    return StyleUtils.TRANSFORM_TRANSLATE + suffix
        + BeeUtils.parenthesize(StyleUtils.toCssLength(value, unit));
  }

  public String translate(int px) {
    return translate(px, CssUnit.PX);
  }
}
