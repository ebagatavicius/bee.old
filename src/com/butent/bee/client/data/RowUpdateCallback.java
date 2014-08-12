package com.butent.bee.client.data;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.event.RowUpdateEvent;

public class RowUpdateCallback extends RowCallback {

  private final String viewName;

  public RowUpdateCallback(String viewName) {
    super();
    this.viewName = viewName;
  }

  @Override
  public void onSuccess(BeeRow result) {
    RowUpdateEvent.fire(BeeKeeper.getBus(), viewName, result);
  }
}
