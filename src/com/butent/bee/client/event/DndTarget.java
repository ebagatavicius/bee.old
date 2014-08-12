package com.butent.bee.client.event;

import com.google.gwt.event.dom.client.HasDragEnterHandlers;
import com.google.gwt.event.dom.client.HasDragLeaveHandlers;
import com.google.gwt.event.dom.client.HasDragOverHandlers;
import com.google.gwt.event.dom.client.HasDropHandlers;

import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.State;

public interface DndTarget extends HasDragEnterHandlers, HasDragLeaveHandlers, HasDragOverHandlers,
    HasDropHandlers, IdentifiableWidget {

  State getTargetState();

  void setTargetState(State targetState);
}
