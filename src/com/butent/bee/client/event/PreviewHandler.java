package com.butent.bee.client.event;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import com.butent.bee.shared.IsUnique;

public interface PreviewHandler extends EventHandler, IsUnique {

  Element getElement();

  boolean isModal();

  void onEventPreview(NativePreviewEvent event, Node targetNode);
}
