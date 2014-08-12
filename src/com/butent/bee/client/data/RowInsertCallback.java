package com.butent.bee.client.data;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.event.RowInsertEvent;

public class RowInsertCallback extends RowCallback {

  private final String viewName;
  private final String sourceId;

  public RowInsertCallback(String viewName, String sourceId) {
    super();

    this.viewName = viewName;
    this.sourceId = sourceId;
  }

  @Override
  public void onSuccess(BeeRow result) {
    RowInsertEvent.fire(BeeKeeper.getBus(), viewName, result, sourceId);
  }
}
