package com.butent.bee.client.view.navigation;

import com.butent.bee.client.data.HasDataTable;
import com.butent.bee.client.event.logical.ScopeChangeEvent;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.view.View;

public interface PagerView extends View, ScopeChangeEvent.Handler, Printable {

  void setDisplay(HasDataTable display);

  void start(HasDataTable display);
}
