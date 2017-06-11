package com.butent.bee.client.communication;

import com.butent.bee.shared.communication.ResponseObject;

@FunctionalInterface
public interface ResponseCallback {
  void onResponse(ResponseObject response);
}
