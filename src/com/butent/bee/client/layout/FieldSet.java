package com.butent.bee.client.layout;

import com.google.gwt.dom.client.Document;

import com.butent.bee.client.widget.Legend;

public class FieldSet extends CustomComplex {

  public FieldSet() {
    super(Document.get().createFieldSetElement());
  }

  public FieldSet(String styleName) {
    super(Document.get().createFieldSetElement(), styleName);
  }

  public FieldSet(String styleName, Legend legend) {
    this(styleName);
    if (legend != null) {
      add(legend);
    }
  }

  @Override
  public String getIdPrefix() {
    return "fieldset";
  }
}
