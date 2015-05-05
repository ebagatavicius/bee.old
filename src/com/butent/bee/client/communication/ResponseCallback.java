package com.butent.bee.client.communication;

import com.butent.bee.shared.communication.ResponseObject;

public abstract class ResponseCallback {

  private int rpcId;

  public int getRpcId() {
    return rpcId;
  }

  public abstract void onResponse(ResponseObject response);

  public void setRpcId(int rpcId) {
    this.rpcId = rpcId;
  }
}
