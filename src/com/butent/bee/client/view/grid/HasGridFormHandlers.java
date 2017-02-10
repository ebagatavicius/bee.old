package com.butent.bee.client.view.grid;

import com.google.gwt.event.shared.HandlerRegistration;

@FunctionalInterface
public interface HasGridFormHandlers {
  HandlerRegistration addGridFormHandler(GridFormEvent.Handler handler);
}
