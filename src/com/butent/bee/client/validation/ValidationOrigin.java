package com.butent.bee.client.validation;

public enum ValidationOrigin {
  CELL, FORM, GRID;

  public boolean isCell() {
    return this == CELL;
  }

  public boolean isForm() {
    return this == FORM;
  }

  public boolean isGrid() {
    return this == GRID;
  }
}
