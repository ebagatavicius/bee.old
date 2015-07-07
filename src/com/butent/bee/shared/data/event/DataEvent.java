package com.butent.bee.shared.data.event;

/**
 * Determines that data events are related to operations with views.
 */

public interface DataEvent {
  boolean hasView(String view);
}
