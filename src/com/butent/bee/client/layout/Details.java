package com.butent.bee.client.layout;

import com.google.gwt.dom.client.Element;

import com.butent.bee.client.widget.Summary;

import elemental.client.Browser;
import elemental.html.DetailsElement;
import elemental.js.html.JsDetailsElement;

public class Details extends CustomComplex {

  private static Element createElement(boolean open) {
    DetailsElement detailsElement = Browser.getDocument().createDetailsElement();
    detailsElement.setOpen(open);
    return (Element) detailsElement;
  }

  public Details(boolean open) {
    super(createElement(open));
  }

  public Details(boolean open, String styleName) {
    super(createElement(open), styleName);
  }

  public Details(boolean open, String styleName, Summary summary) {
    this(open, styleName);
    if (summary != null) {
      add(summary);
    }
  }

  @Override
  public String getIdPrefix() {
    return "details";
  }

  public boolean isOpen() {
    return getDetailsElement().isOpen();
  }

  public void setOpen(boolean open) {
    getDetailsElement().setOpen(open);
  }

  private DetailsElement getDetailsElement() {
    return (JsDetailsElement) getElement().cast();
  }
}
