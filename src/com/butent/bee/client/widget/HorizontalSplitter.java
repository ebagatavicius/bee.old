package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;

/**
 * Implements a panel that arranges two parts of it horizontally and allows the user to
 * interactively change the width of the area dedicated to each of the two parts.
 */

public class HorizontalSplitter extends Splitter {

  public HorizontalSplitter(Widget target, Element targetContainer, boolean reverse, int size) {
    super(target, targetContainer, reverse, size);
    getElement().getStyle().setPropertyPx("width", size);
    setStyleName("bee-HSplitter");
  }

  @Override
  public void createId() {
    DomUtils.createId(this, "h-splitter");
  }

  @Override
  public int getAbsolutePosition() {
    return getAbsoluteLeft();
  }

  @Override
  public int getEventPosition(Event event) {
    return event.getClientX();
  }

  @Override
  public int getTargetPosition() {
    return getTargetContainer().getAbsoluteLeft();
  }

  @Override
  public int getTargetSize() {
    return getTargetContainer().getOffsetWidth();
  }

}
