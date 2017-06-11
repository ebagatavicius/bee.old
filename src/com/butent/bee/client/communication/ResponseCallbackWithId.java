package com.butent.bee.client.communication;

public abstract class ResponseCallbackWithId implements ResponseCallback, HasRpcId {

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
