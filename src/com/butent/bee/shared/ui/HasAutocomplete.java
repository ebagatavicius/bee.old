package com.butent.bee.shared.ui;

import com.butent.bee.shared.HasName;
import com.butent.bee.shared.html.Autocomplete;

public interface HasAutocomplete extends HasName, HasStringValue {

  String ATTR_AUTOCOMPLETE = "autocomplete";

  String ATTR_AUTOCOMPLETE_KEY = "autocompleteKey";

  String ATTR_AUTOCOMPLETE_SECTION = "autocompleteSection";
  String ATTR_AUTOCOMPLETE_HINT = "autocompleteHint";
  String ATTR_AUTOCOMPLETE_CONTACT = "autocompleteContact";
  String ATTR_AUTOCOMPLETE_FIELD = "autocompleteField";

  boolean isMultiline();

  String getAutocomplete();

  void setAutocomplete(Autocomplete autocomplete);

  void setAutocomplete(String ac);
}
