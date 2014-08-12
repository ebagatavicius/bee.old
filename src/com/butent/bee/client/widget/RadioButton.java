package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.Event;

/**
 * Implements a mutually-exclusive selection radio button user interface component.
 */

public class RadioButton extends CheckBox {

  private Boolean oldValue;

  public RadioButton(String name) {
    super(Document.get().createRadioInputElement(name));

    sinkEvents(Event.ONCLICK | Event.ONMOUSEUP | Event.ONBLUR | Event.ONKEYDOWN);
  }

  public RadioButton(String name, String label) {
    this(name);
    setHtml(label);
  }

  @Override
  public String getIdPrefix() {
    return "rb";
  }

  @Override
  public void onBrowserEvent(Event event) {
    switch (event.getTypeInt()) {
      case Event.ONMOUSEUP:
      case Event.ONBLUR:
      case Event.ONKEYDOWN:
        oldValue = getValue();
        break;

      case Event.ONCLICK:
        EventTarget target = event.getEventTarget();

        if (Element.is(target) && getLabelElem().isOrHasChild(Element.as(target))) {
          oldValue = getValue();
        } else {
          super.onBrowserEvent(event);
          ValueChangeEvent.fireIfNotEqual(this, oldValue, getValue());
        }
        return;
    }

    super.onBrowserEvent(event);
  }

  @Override
  public void sinkEvents(int eventBitsToAdd) {
    if (isOrWasAttached()) {
      Event.sinkEvents(getInputElem(), eventBitsToAdd | Event.getEventsSunk(getInputElem()));
      Event.sinkEvents(getLabelElem(), eventBitsToAdd | Event.getEventsSunk(getLabelElem()));
    } else {
      super.sinkEvents(eventBitsToAdd);
    }
  }

  @Override
  protected void ensureDomEventHandlers() {
  }

  @Override
  protected String getDefaultStyleName() {
    return "bee-RadioButton";
  }
}
