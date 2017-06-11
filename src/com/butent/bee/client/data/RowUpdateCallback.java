package com.butent.bee.client.data;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.event.RowUpdateEvent;

public class RowUpdateCallback implements RowCallback {

  private final String viewName;
  private final RowCallback chain;

  public RowUpdateCallback(String viewName) {
    this(viewName, null);
  }

  public RowUpdateCallback(String viewName, RowCallback chain) {
    super();

    this.viewName = viewName;
    this.chain = chain;
  }

  @Override
  public void onSuccess(BeeRow result) {
    RowUpdateEvent.fire(BeeKeeper.getBus(), viewName, result);

    if (chain != null) {
      chain.onSuccess(result);
    }
  }
}
