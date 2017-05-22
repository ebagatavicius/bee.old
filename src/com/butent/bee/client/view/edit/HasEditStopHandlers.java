package com.butent.bee.client.view.edit;

import com.google.gwt.event.shared.HandlerRegistration;

@FunctionalInterface
public interface HasEditStopHandlers {
  HandlerRegistration addEditStopHandler(EditStopEvent.Handler handler);
}
