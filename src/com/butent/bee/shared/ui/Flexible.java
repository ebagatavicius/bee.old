package com.butent.bee.shared.ui;

public interface Flexible {

  int clampSize(Orientation orientation, int size);

  Flexibility getFlexibility();

  int getHypotheticalSize(Orientation orientation, boolean flexible);

  boolean isFlexible();

  void setFlexibility(Flexibility flexibility);

  boolean updateSize(Orientation orientation, int size);
}
