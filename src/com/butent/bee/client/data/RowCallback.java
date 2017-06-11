package com.butent.bee.client.data;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;

@FunctionalInterface
public interface RowCallback extends RpcCallback<BeeRow> {

  static RowCallback refreshRow(String viewName) {
    return refreshRow(viewName, false);
  }

  static RowCallback refreshRow(final String viewName, final boolean refreshChildren) {
    return result -> RowUpdateEvent.fire(BeeKeeper.getBus(), viewName, result, refreshChildren);
  }

  static RowCallback refreshView(String viewName) {
    return refreshView(viewName, null);
  }

  static RowCallback refreshView(final String viewName, final Long parentId) {
    return result -> DataChangeEvent.fireRefresh(BeeKeeper.getBus(), viewName, parentId);
  }

  default void onCancel() {
  }
}
