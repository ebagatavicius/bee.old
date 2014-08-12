package com.butent.bee.shared.ui;

public enum Orientation implements HasCaption {
  HORIZONTAL, VERTICAL;

  @Override
  public String getCaption() {
    return name().toLowerCase();
  }

  public boolean isVertical() {
    return this == VERTICAL;
  }

  public Orientation invert() {
    return isVertical() ? HORIZONTAL : VERTICAL;
  }
}
