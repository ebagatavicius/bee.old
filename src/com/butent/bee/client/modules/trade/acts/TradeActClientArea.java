package com.butent.bee.client.modules.trade.acts;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.cli.Shell;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.logging.ClientLogManager;
import com.butent.bee.client.modules.administration.PasswordService;
import com.butent.bee.client.screen.ScreenImpl;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.StringList;

import java.util.Collections;

public class TradeActClientArea extends ScreenImpl {

  public TradeActClientArea() {
    super();
  }

  @Override
  public UserInterface getUserInterface() {
    return UserInterface.TRADE_ACTS;
  }

  @Override
  public void start(UserData userData) {
    super.start(userData);

    Data.setVisibleViews(StringList.of(VIEW_TRADE_ACTS,
        VIEW_TRADE_ACT_ITEMS, VIEW_TRADE_ACT_SERVICES, VIEW_TRADE_ACT_INVOICES,
        VIEW_SALES, VIEW_SALE_ITEMS, VIEW_INVOICE_TRADE_ACTS));
    Data.setEditableViews(Collections.singleton(BeeConst.NONE));
  }

  @Override
  public void updateMenu(IdentifiableWidget widget) {
  }

  @Override
  protected String getScreenStyle() {
    return TradeActKeeper.STYLE_PREFIX + "client-area";
  }

  @Override
  protected Pair<? extends IdentifiableWidget, Integer> initEast() {
    return Pair.of(ClientLogManager.getLogPanel(), 0);
  }

  @Override
  protected void createExpanders() {
  }

  @Override
  protected Panel createMenuPanel() {
    return null;
  }

  @Override
  protected Widget createSearch() {
    return null;
  }

  @Override
  protected Pair<? extends IdentifiableWidget, Integer> initWest() {
    Shell shell = new Shell(TradeActKeeper.STYLE_PREFIX + "shell");
    shell.restore();

    Simple wrapper = new Simple(shell);
    return Pair.of(wrapper, 0);
  }

  @Override
  protected void onUserSignatureClick() {
    PasswordService.change();
  }
}
