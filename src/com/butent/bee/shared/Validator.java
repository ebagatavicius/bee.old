package com.butent.bee.shared;

public interface Validator<T> {

  boolean isRequired();

  String getMessage(T value);

  void setRequired(boolean required);

  boolean validate(T value);
}
