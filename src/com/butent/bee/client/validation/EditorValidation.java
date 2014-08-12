package com.butent.bee.client.validation;

public enum EditorValidation {
  NONE, INPUT, NEW_VALUE, ALL;

  public boolean validateInput() {
    return this == INPUT || this == ALL;
  }

  public boolean validateNewValue() {
    return this == NEW_VALUE || this == ALL;
  }
}
