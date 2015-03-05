package com.butent.bee.client.widget;

import com.google.common.base.CharMatcher;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.TimeUtils;

public class InputTimeOfDay extends InputTime {

  public InputTimeOfDay() {
    super();
  }

  @Override
  public String getIdPrefix() {
    return "tod";
  }

  @Override
  protected CharMatcher getDefaultCharMatcher() {
    return CharMatcher.inRange(BeeConst.CHAR_ZERO, BeeConst.CHAR_NINE)
        .or(CharMatcher.is(TimeUtils.TIME_FIELD_SEPARATOR));
  }

  @Override
  protected int getDefaultMaxLength() {
    return 5;
  }

  @Override
  protected long getDefaultMaxMillis() {
    return TimeUtils.MILLIS_PER_DAY - TimeUtils.MILLIS_PER_MINUTE;
  }

  @Override
  protected String getDefaultStyleName() {
    return BeeConst.CSS_CLASS_PREFIX + "InputTimeOfDay";
  }
}
