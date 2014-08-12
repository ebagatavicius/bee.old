package com.butent.bee.client.modules.trade;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.rights.Module;

public final class TradeKeeper {

  public static final String STYLE_PREFIX = StyleUtils.CLASS_NAME_PREFIX + "trade-";

  public static ParameterList createArgs(String name) {
    ParameterList args = BeeKeeper.getRpc().createParameters(Module.TRADE.getName());
    args.addQueryItem(AdministrationConstants.METHOD, name);
    return args;
  }

  public static void register() {
    GridFactory.registerGridInterceptor(TradeConstants.VIEW_PURCHASE_ITEMS, new TradeItemsGrid());
    GridFactory.registerGridInterceptor(TradeConstants.VIEW_SALE_ITEMS, new TradeItemsGrid());
  }

  private TradeKeeper() {
  }
}
