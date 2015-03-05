package com.butent.bee.client.logging;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.shared.BeeConst;

/**
 * Manages a specific type of flow panel to contain log entries.
 */

class LogPanel extends Flow {

  LogPanel() {
    super();
    setStyleName(BeeConst.CSS_CLASS_PREFIX + "LogArea");
    sinkEvents(Event.ONCLICK);
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (EventUtils.hasModifierKey(event) && getWidgetCount() > 0) {
      Widget target = DomUtils.getChildByElement(this, Element.as(event.getEventTarget()));
      event.preventDefault();

      if (target == null || equals(target) || getWidgetCount() <= 1) {
        clear();
      } else {
        Widget child;
        while (getWidgetCount() > 0) {
          child = getWidget(0);
          if (target.equals(child)) {
            break;
          }
          remove(child);
        }
      }
      return;
    }
    super.onBrowserEvent(event);
  }
}
