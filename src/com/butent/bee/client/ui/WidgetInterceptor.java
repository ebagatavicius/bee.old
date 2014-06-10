package com.butent.bee.client.ui;

import com.google.gwt.xml.client.Element;

import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;

public interface WidgetInterceptor {

  void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback);

  boolean beforeCreateWidget(String name, Element description);
  
  IdentifiableWidget createCustomWidget(String name, Element description);
}
