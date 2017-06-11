package com.butent.bee.client.data;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.event.RowInsertEvent;

public class RowInsertCallback implements RowCallback {

  private final String viewName;
  private final String sourceId;

  private final RowCallback chain;

  public RowInsertCallback(String viewName) {
    this(viewName, null);
  }

  public RowInsertCallback(String viewName, String sourceId) {
    this(viewName, sourceId, null);
  }

  public RowInsertCallback(String viewName, String sourceId, RowCallback chain) {
    super();

    this.viewName = viewName;
    this.sourceId = sourceId;

    this.chain = chain;
  }

  @Override
  public void onSuccess(BeeRow result) {
    RowInsertEvent.fire(BeeKeeper.getBus(), viewName, result, sourceId);

    if (chain != null) {
      chain.onSuccess(result);
    }
  }
}
