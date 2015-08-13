package com.butent.bee.client.modules.orders;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.shared.rights.Module;

/**
 * Client-side projects module handler.
 */
public final class OrdersKeeper {

  /**
   * Creates rpc parameters of orders module.
   * 
   * @param method name of method.
   * @return rpc parameters to call queries of server-side.
   */
  public static ParameterList createSvcArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.ORDERS, method);
  }

  /**
   * Register orders client-side module handler.
   */
  public static void register() {

  }

  private OrdersKeeper() {

  }
}