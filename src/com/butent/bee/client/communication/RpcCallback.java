package com.butent.bee.client.communication;

import com.butent.bee.client.Callback;

@FunctionalInterface
public interface RpcCallback<T> extends Callback<T> {
}
