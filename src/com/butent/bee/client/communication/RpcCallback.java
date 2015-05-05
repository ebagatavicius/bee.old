package com.butent.bee.client.communication;

import com.butent.bee.client.Callback;

public abstract class RpcCallback<T> extends Callback<T> {

  private int rpcId;

  public int getRpcId() {
    return rpcId;
  }

  public void setRpcId(int rpcId) {
    this.rpcId = rpcId;
  }
}
