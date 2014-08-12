package com.butent.bee.client.ui;

import com.google.gwt.user.client.ui.HasEnabled;

public interface EnablableWidget extends HasEnabled {
  void setStyleName(String style, boolean add);
}
