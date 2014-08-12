package com.butent.bee.shared;

/**
 * Requires implementing classes to have methods for getting and setting minimum and maximum values.
 */

public interface HasBounds {

  String ATTR_MIN_VALUE = "minValue";
  String ATTR_MAX_VALUE = "maxValue";

  String getMaxValue();

  String getMinValue();

  void setMaxValue(String maxValue);

  void setMinValue(String minValue);
}
