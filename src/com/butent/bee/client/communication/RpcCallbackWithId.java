package com.butent.bee.client.communication;

public abstract class RpcCallbackWithId<T> implements RpcCallback<T>, HasRpcId {

  private int rpcId;

  @Override
  public int getRpcId() {
    return rpcId;
  }

  @Override
  public void setRpcId(int rpcId) {
    this.rpcId = rpcId;
  }
}
