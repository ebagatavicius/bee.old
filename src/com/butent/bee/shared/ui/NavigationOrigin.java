package com.butent.bee.shared.ui;

public enum NavigationOrigin {
  KEYBOARD(false),
  MOUSE(true),
  PAGER(true),
  SCROLLER(true),
  SYSTEM(false);

  private final boolean shiftActiveRow;

  private NavigationOrigin(boolean shiftActiveRow) {
    this.shiftActiveRow = shiftActiveRow;
  }

  public boolean shiftActiveRow() {
    return shiftActiveRow;
  }
}
