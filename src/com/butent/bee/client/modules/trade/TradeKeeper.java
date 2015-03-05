package com.butent.bee.client.modules.trade;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.modules.trade.acts.TradeActKeeper;
import com.butent.bee.client.style.ColorStyleProvider;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.grid.interceptor.UniqueChildInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;

public final class TradeKeeper {

  public static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "trade-";

  public static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.TRADE, method);
  }

  public static void register() {
    GridFactory.registerGridInterceptor(VIEW_PURCHASE_ITEMS, new TradeItemsGrid());
    GridFactory.registerGridInterceptor(VIEW_SALE_ITEMS, new TradeItemsGrid());

    GridFactory.registerGridInterceptor(GRID_SERIES_MANAGERS,
        UniqueChildInterceptor.forUsers(Localized.getConstants().managers(),
            COL_SERIES, COL_TRADE_MANAGER));

    FormFactory.registerFormInterceptor(FORM_SALES_INVOICE, new SalesInvoiceForm());

    ColorStyleProvider csp = ColorStyleProvider.createDefault(VIEW_TRADE_OPERATIONS);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_OPERATIONS, COL_BACKGROUND, csp);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_OPERATIONS, COL_FOREGROUND, csp);

    csp = ColorStyleProvider.createDefault(VIEW_TRADE_STATUSES);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_STATUSES, COL_BACKGROUND, csp);
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_STATUSES, COL_FOREGROUND, csp);

    if (ModuleAndSub.of(Module.TRADE, SubModule.ACTS).isEnabled()) {
      TradeActKeeper.register();
    }
  }

  private TradeKeeper() {
  }
}
