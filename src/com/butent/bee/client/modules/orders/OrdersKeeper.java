package com.butent.bee.client.modules.orders;

import static com.butent.bee.shared.modules.orders.OrdersConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;

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
    FormFactory.registerFormInterceptor(COL_ORDER, new OrderForm());
    FormFactory.registerFormInterceptor("OrderInvoice", new OrderInvoiceForm());
    FormFactory.registerFormInterceptor(FORM_NEW_ORDER_INVOICE, new NewOrderInvoiceForm());

    GridFactory.registerGridInterceptor(VIEW_ORDER_SALES, new OrderInvoiceBuilder());
    GridFactory.registerGridInterceptor(GRID_ORDERS_INVOICES, new OrdersInvoicesGrid());
    GridFactory.registerGridInterceptor(VIEW_ORDER_TMPL_ITEMS, new OrderTmplItemsGrid());
    GridFactory.registerGridInterceptor(VIEW_ORDERS, new OrdersGrid());

    SelectorEvent.register(new OrdersSelectorHandler());

    if (BeeUtils.unbox(Global.getParameterBoolean(PRM_NOTIFY_ABOUT_DEBTS))) {
      OrdersObserver.register();
    }
  }

  private OrdersKeeper() {

  }
}