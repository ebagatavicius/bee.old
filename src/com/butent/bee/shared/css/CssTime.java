package com.butent.bee.shared.css;

import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum CssTime implements HasCaption {
  S("s"), MS("ms");

  public static final CssTime DEFAULT = MS;

  public static String format(double value, CssTime time) {
    return BeeUtils.toString(value) + normalize(time).getCaption();
  }

  public static String format(int value, CssTime time) {
    return BeeUtils.toString(value) + normalize(time).getCaption();
  }

  public static CssTime normalize(CssTime time) {
    return (time == null) ? DEFAULT : time;
  }

  public static CssTime parse(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }

    for (CssTime time : CssTime.values()) {
      if (BeeUtils.same(time.getCaption(), input)) {
        return time;
      }
    }
    return null;
  }

  public static CssTime parse(String input, CssTime defTime) {
    return BeeUtils.nvl(parse(input), defTime);
  }

  private final String caption;

  private CssTime(String caption) {
    this.caption = caption;
  }

  @Override
  public String getCaption() {
    return caption;
  }
}
