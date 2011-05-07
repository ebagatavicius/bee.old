package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;

/**
 * Implements a panel that arranges two parts of it vertically and allows the user to interactively
 * change the height of the area dedicated to each of the two parts.
 */

public class VerticalSplitter extends Splitter {
  public VerticalSplitter(Widget target, Element targetContainer, boolean reverse, int size) {
    super(target, targetContainer, reverse, size);
    getElement().getStyle().setPropertyPx("height", size);
    setStyleName("bee-VSplitter");
  }

  @Override
  public void createId() {
    DomUtils.createId(this, "v-splitter");
  }

  @Override
  public int getAbsolutePosition() {
    return getAbsoluteTop();
  }

  @Override
  public int getEventPosition(Event event) {
    return event.getClientY();
  }

  @Override
  public int getTargetPosition() {
    return getTargetContainer().getAbsoluteTop();
  }

  @Override
  public int getTargetSize() {
    return getTargetContainer().getOffsetHeight();
  }

}
