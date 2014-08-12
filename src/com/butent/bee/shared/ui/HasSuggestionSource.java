package com.butent.bee.shared.ui;

public interface HasSuggestionSource extends HasStringValue {

  String getSuggestionSource();

  boolean isMultiline();

  void setSuggestionSource(String suggestionSource);
}
