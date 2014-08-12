package com.butent.bee.shared.utils;

import com.google.common.collect.Range;

public class RangeOptions {

  public static final boolean DEFAULT_LOWER_OPEN = false;
  public static final boolean DEFAULT_UPPER_OPEN = true;

  public static final boolean DEFAULT_LOWER_REQUIRED = false;
  public static final boolean DEFAULT_UPPER_REQUIRED = false;

  public static final char OPT_LOWER_OPEN = '(';
  public static final char OPT_LOWER_CLOSED = '[';

  public static final char OPT_UPPER_OPEN = ')';
  public static final char OPT_UPPER_CLOSED = ']';

  public static final char OPT_LOWER_REQUIRED = '-';
  public static final char OPT_UPPER_REQUIRED = '+';

  public static boolean hasLowerOpen(String opt) {
    return hasLowerOpen(opt, DEFAULT_LOWER_OPEN);
  }

  public static boolean hasLowerOpen(String opt, boolean def) {
    if (BeeUtils.contains(opt, OPT_LOWER_OPEN)) {
      return true;
    } else if (BeeUtils.contains(opt, OPT_LOWER_CLOSED)) {
      return false;
    } else {
      return def;
    }
  }

  public static boolean hasLowerRequired(String opt) {
    return hasLowerRequired(opt, DEFAULT_LOWER_REQUIRED);
  }

  public static boolean hasLowerRequired(String opt, boolean def) {
    if (BeeUtils.contains(opt, OPT_LOWER_REQUIRED)) {
      return true;
    } else {
      return def;
    }
  }

  public static boolean hasUpperOpen(String opt) {
    return hasUpperOpen(opt, DEFAULT_UPPER_OPEN);
  }

  public static boolean hasUpperOpen(String opt, boolean def) {
    if (BeeUtils.contains(opt, OPT_UPPER_OPEN)) {
      return true;
    } else if (BeeUtils.contains(opt, OPT_UPPER_CLOSED)) {
      return false;
    } else {
      return def;
    }
  }

  public static boolean hasUpperRequired(String opt) {
    return hasUpperRequired(opt, DEFAULT_UPPER_REQUIRED);
  }

  public static boolean hasUpperRequired(String opt, boolean def) {
    if (BeeUtils.contains(opt, OPT_UPPER_REQUIRED)) {
      return true;
    } else {
      return def;
    }
  }

  private boolean lowerOpen;
  private boolean upperOpen;

  private boolean lowerRequired;
  private boolean upperRequired;

  public RangeOptions() {
    this(DEFAULT_LOWER_OPEN, DEFAULT_UPPER_OPEN, DEFAULT_LOWER_REQUIRED, DEFAULT_UPPER_REQUIRED);
  }

  public RangeOptions(boolean lowerOpen, boolean upperOpen, boolean required) {
    this(lowerOpen, upperOpen, required, required);
  }

  public RangeOptions(boolean lowerOpen, boolean upperOpen, boolean lowerRequired,
      boolean upperRequired) {
    super();
    this.lowerOpen = lowerOpen;
    this.upperOpen = upperOpen;
    this.lowerRequired = lowerRequired;
    this.upperRequired = upperRequired;
  }

  public RangeOptions(boolean lowerOpen, boolean upperOpen, String optRequired) {
    this(lowerOpen, upperOpen, hasLowerRequired(optRequired), hasUpperRequired(optRequired));
  }

  public RangeOptions(String opt) {
    this(hasLowerOpen(opt), hasUpperOpen(opt), hasLowerRequired(opt), hasUpperRequired(opt));
  }

  public RangeOptions(String optOpen, boolean required) {
    this(optOpen, required, required);
  }

  public RangeOptions(String optOpen, boolean lowerRequired, boolean upperRequired) {
    this(hasLowerOpen(optOpen), hasUpperOpen(optOpen), lowerRequired, upperRequired);
  }

  public <C extends Comparable<?>> boolean contains(C lower, C upper, C value) {
    if (value == null) {
      return false;
    } else if (lower == null && upper == null) {
      return !isLowerRequired() && !isUpperRequired();
    } else {
      Range<C> range = getRange(lower, upper);
      return (range == null) ? false : range.contains(value);
    }
  }

  public <C extends Comparable<?>> Range<C> getRange(C lower, C upper) {
    Range<C> range = null;

    if (lower == null) {
      if (!lowerRequired) {
        if (upper == null) {
          if (!upperRequired) {
            range = Range.all();
          }
        } else {
          range = upperOpen ? Range.lessThan(upper) : Range.atMost(upper);
        }
      }

    } else if (upper == null) {
      if (!upperRequired) {
        range = lowerOpen ? Range.greaterThan(lower) : Range.atLeast(lower);
      }

    } else if (lowerOpen) {
      range = upperOpen ? Range.open(lower, upper) : Range.openClosed(lower, upper);
    } else {
      range = upperOpen ? Range.closedOpen(lower, upper) : Range.closed(lower, upper);
    }

    return range;
  }

  public boolean isLowerOpen() {
    return lowerOpen;
  }

  public boolean isLowerRequired() {
    return lowerRequired;
  }

  public boolean isUpperOpen() {
    return upperOpen;
  }

  public boolean isUpperRequired() {
    return upperRequired;
  }

  public void setLowerOpen(boolean lowerOpen) {
    this.lowerOpen = lowerOpen;
  }

  public void setLowerRequired(boolean lowerRequired) {
    this.lowerRequired = lowerRequired;
  }

  public void setUpperOpen(boolean upperOpen) {
    this.upperOpen = upperOpen;
  }

  public void setUpperRequired(boolean upperRequired) {
    this.upperRequired = upperRequired;
  }
}
