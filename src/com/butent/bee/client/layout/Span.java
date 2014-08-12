package com.butent.bee.client.layout;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.HasIndexedWidgets;
import com.butent.bee.client.ui.IdentifiableWidget;

/**
 * Enables to manage span elements from Document Object Model.
 */

public class Span extends ComplexPanel implements HasIndexedWidgets, IdentifiableWidget {

  public Span() {
    setElement(Document.get().createSpanElement());
    DomUtils.createId(this, getIdPrefix());
    setStyleName(getDefaultStyleName());
  }

  @Override
  public void add(Widget w) {
    super.add(w, Element.as(getElement()));
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "span";
  }

  @Override
  public void insert(Widget w, int beforeIndex) {
    insert(w, Element.as(getElement()), beforeIndex, true);
  }

  @Override
  public boolean isEmpty() {
    return getWidgetCount() <= 0;
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  protected String getDefaultStyleName() {
    return "bee-Span";
  }
}
