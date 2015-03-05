package com.butent.bee.client.screen;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.BeeConst;

import elemental.js.dom.JsElement;

public final class BodyPanel extends ComplexPanel {

  private static BodyPanel singleton;

  public static void conceal(Element el) {
    get().concealment.appendChild(el);
  }

  public static void conceal(JsElement el) {
    conceal(Element.as(el));
  }

  public static BodyPanel get() {
    if (singleton == null) {
      singleton = new BodyPanel();
    }
    return singleton;
  }

  private final Element concealment;

  private BodyPanel() {
    super();
    setElement(Document.get().getBody());
    addStyleName(BeeConst.CSS_CLASS_PREFIX + "Body");

    this.concealment = Document.get().createDivElement();
    StyleUtils.makeAbsolute(concealment);
    StyleUtils.setTop(concealment, -2000);
    StyleUtils.fullWidth(concealment);
    concealment.addClassName(BeeConst.CSS_CLASS_PREFIX + "Concealment");

    getElement().appendChild(concealment);

    onAttach();
  }

  @Override
  public void add(Widget w) {
    add(w, Element.as(getElement()));
  }
}
