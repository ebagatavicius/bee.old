package com.butent.bee.shared.modules.calendar;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public final class CalendarHelper {

  public static final String KEY_PERIOD = "Period";

  private static final String LABEL_SEPARATOR = BeeConst.STRING_COLON + BeeConst.STRING_SPACE;
  private static final String CHILD_SEPARATOR = ", ";

  private static final String SUBSTITUTE_PREFIX = "{";
  private static final String SUBSTITUTE_SUFFIX = "}";

  public static String build(String label, String value, boolean addLabel) {
    if (addLabel) {
      return join(label, value);
    } else {
      return BeeUtils.trim(value);
    }
  }

  public static boolean hasSubstitutes(String s) {
    return s != null && s.contains(SUBSTITUTE_PREFIX) && s.contains(SUBSTITUTE_SUFFIX);
  }

  public static String join(String label, String value) {
    if (BeeUtils.isEmpty(value)) {
      return BeeConst.STRING_EMPTY;
    } else if (BeeUtils.isEmpty(label)) {
      return BeeUtils.trim(value);
    } else {
      return label + LABEL_SEPARATOR + BeeUtils.trim(value);
    }
  }

  public static String joinChildren(List<String> children) {
    return BeeUtils.join(CHILD_SEPARATOR, children);
  }

  public static String wrap(String s) {
    return SUBSTITUTE_PREFIX + s.trim() + SUBSTITUTE_SUFFIX;
  }

  private CalendarHelper() {
  }
}
