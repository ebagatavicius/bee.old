package com.butent.bee.client.view.edit;

import com.google.gwt.event.shared.HandlerRegistration;

public interface HasEditChangeHandlers {
  HandlerRegistration addEditChangeHandler(EditChangeHandler handler);
}
