package com.butent.bee.client.ui;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.IsWidget;

public interface IdentifiableWidget extends IsWidget, HasIdentity {

  void addStyleName(String style);

  Element getElement();

  void removeStyleName(String style);

  void setStyleName(String style, boolean add);
}
