package com.butent.bee.shared.css;

import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum CssFrequency implements HasCaption {
  HZ("Hz"), KHZ("kHz");

  public static final CssFrequency DEFAULT = HZ;

  public static String format(double value, CssFrequency frequency) {
    return BeeUtils.toString(value) + normalize(frequency).getCaption();
  }

  public static String format(int value, CssFrequency frequency) {
    return BeeUtils.toString(value) + normalize(frequency).getCaption();
  }

  public static CssFrequency normalize(CssFrequency frequency) {
    return (frequency == null) ? DEFAULT : frequency;
  }

  public static CssFrequency parse(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }

    for (CssFrequency frequency : CssFrequency.values()) {
      if (BeeUtils.same(frequency.getCaption(), input)) {
        return frequency;
      }
    }
    return null;
  }

  public static CssFrequency parse(String input, CssFrequency defFrequency) {
    return BeeUtils.nvl(parse(input), defFrequency);
  }

  private final String caption;

  private CssFrequency(String caption) {
    this.caption = caption;
  }

  @Override
  public String getCaption() {
    return caption;
  }
}
