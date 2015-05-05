package com.butent.bee.client.data;

import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.shared.data.BeeRow;

public abstract class RowCallback extends RpcCallback<BeeRow> {

  public void onCancel() {
  }
}
