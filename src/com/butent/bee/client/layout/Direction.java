package com.butent.bee.client.layout;

import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Contains available directions for layout elements creation.
 */

public enum Direction {
  NORTH, EAST, SOUTH, WEST, CENTER;

  public static Direction parse(String input) {
    for (Direction direction : Direction.values()) {
      if (BeeUtils.inListSame(input, direction.brief(), direction.name())) {
        return direction;
      }
    }
    return null;
  }

  public String brief() {
    return name().substring(0, 1);
  }

  public Orientation getOrientation() {
    if (isHorizontal()) {
      return Orientation.HORIZONTAL;
    } else if (isVertical()) {
      return Orientation.VERTICAL;
    } else {
      return null;
    }
  }

  public String getStyleSuffix() {
    return BeeUtils.proper(name());
  }

  public boolean isCenter() {
    return this == CENTER;
  }

  public boolean isHorizontal() {
    return this == EAST || this == WEST;
  }

  public boolean isVertical() {
    return this == NORTH || this == SOUTH;
  }

  public Direction opposite() {
    switch (this) {
      case NORTH:
        return SOUTH;
      case SOUTH:
        return NORTH;
      case EAST:
        return WEST;
      case WEST:
        return EAST;
      default:
        return null;
    }
  }
}
