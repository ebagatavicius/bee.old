package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import com.butent.bee.shared.IsUnique;
import com.butent.bee.shared.data.value.Value;

public interface HasSummaryChangeHandlers extends HasHandlers, IsUnique {

  HandlerRegistration addSummaryChangeHandler(SummaryChangeEvent.Handler handler);

  Value getSummary();

  boolean summarize();

  void setSummarize(boolean summarize);
}
