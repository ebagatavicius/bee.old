package com.butent.bee.client.layout;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;

/**
 * Enables to use panel that lays all of its widgets out in a single vertical column.
 */

public class Vertical extends CellVector  {

  public Vertical() {
    super();
    addStyleName("bee-Vertical");
  }

  @Override
  public void add(Widget w) {
    Element tr = DOM.createTR();
    Element td = createDefaultCell();
    DOM.appendChild(tr, td);
    DOM.appendChild(getBody(), tr);

    add(w, td);
  }

  @Override
  public String getIdPrefix() {
    return "vert";
  }

  @Override
  public void insert(Widget w, int beforeIndex) {
    checkIndexBoundsForInsertion(beforeIndex);

    Element tr = DOM.createTR();
    Element td = createDefaultCell();
    DOM.appendChild(tr, td);

    DOM.insertChild(getBody(), tr, beforeIndex);
    insert(w, td, beforeIndex, false);
  }

  @Override
  public boolean remove(Widget w) {
    Element td = w.getElement().getParentElement();
    boolean removed = super.remove(w);
    if (removed) {
      getBody().removeChild(td.getParentElement());
    }
    return removed;
  }
}
