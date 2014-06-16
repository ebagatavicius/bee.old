package com.butent.bee.client.modules.transport;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.cli.Shell;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.logging.ClientLogManager;
import com.butent.bee.client.modules.administration.PasswordService;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.screen.ScreenImpl;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.ui.UserInterface;

import java.util.Map;

public class SelfServiceScreen extends ScreenImpl {

  private final class ActivationCallback implements PresenterCallback {
    private final String viewKey;

    private ActivationCallback(String viewKey) {
      this.viewKey = viewKey;
    }

    @Override
    public void onCreate(Presenter presenter) {
      if (activeViews.containsKey(viewKey)) {
        Widget widget = DomUtils.getChildQuietly(getWorkspace(), activeViews.get(viewKey));
        if (widget instanceof IdentifiableWidget) {
          closeWidget((IdentifiableWidget) widget);
        }
      }

      activeViews.put(viewKey, presenter.getWidget().getId());
      showInNewPlace(presenter.getWidget());
    }
  }

  private static final String STYLE_PREFIX = "bee-tr-SelfService-";

  private final Map<String, String> activeViews = Maps.newHashMap();

  public SelfServiceScreen() {
    super();
  }

  @Override
  public UserInterface getUserInterface() {
    return UserInterface.SELF_SERVICE;
  }

  @Override
  public void start(UserData userData) {
    super.start(userData);

    Data.setVisibleViews(Sets.newHashSet(VIEW_CARGO_REQUESTS, VIEW_CARGO_REQUEST_FILES,
        VIEW_CARGO_REQUEST_TEMPLATES, VIEW_ORDERS, VIEW_CARGO_INVOICES,
        VIEW_CARGO_PURCHASE_INVOICES));
    Data.setEditableViews(Sets.newHashSet(VIEW_CARGO_REQUESTS, VIEW_CARGO_REQUEST_FILES,
        VIEW_CARGO_REQUEST_TEMPLATES));

    Data.setColumnReadOnly(VIEW_CARGO_REQUESTS, COL_CARGO_REQUEST_DATE);
    Data.setColumnReadOnly(VIEW_CARGO_REQUESTS, COL_CARGO_REQUEST_USER);
    Data.setColumnReadOnly(VIEW_CARGO_REQUESTS, COL_CARGO_REQUEST_STATUS);

    Data.setColumnReadOnly(VIEW_CARGO_REQUEST_TEMPLATES, COL_CARGO_REQUEST_TEMPLATE_USER);

    GridFactory.hideColumn(VIEW_CARGO_REQUESTS, COL_CARGO_REQUEST_USER);
    GridFactory.hideColumn(VIEW_CARGO_REQUEST_TEMPLATES, COL_CARGO_REQUEST_TEMPLATE_USER);

    GridFactory.hideColumn(VIEW_CARGO_INVOICES, "Select");
    GridFactory.hideColumn(VIEW_CARGO_PURCHASE_INVOICES, "Select");

    FormFactory.hideWidget(DocumentConstants.FORM_DOCUMENT, "DocumentRelations");

    addCommandItem(new Button(Localized.getConstants().trSelfServiceCommandNewRequest(),
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            RowFactory.createRow(VIEW_CARGO_REQUESTS);
          }
        }));

    addCommandItem(new Button(Localized.getConstants().trSelfServiceCommandRequests(),
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            openGrid(VIEW_CARGO_REQUESTS, true, COL_CARGO_REQUEST_USER);
          }
        }));

    addCommandItem(new Button(Localized.getConstants().trSelfServiceCommandTemplates(),
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            openGrid(VIEW_CARGO_REQUEST_TEMPLATES, true, COL_CARGO_REQUEST_TEMPLATE_USER);
          }
        }));

    addCommandItem(new Button(Localized.getConstants().trSelfServiceCommandHistory(),
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            Value company = new LongValue(BeeKeeper.getUser().getCompany());

            Filter orderFilter = Filter.or(Filter.isEqual(COL_CUSTOMER, company),
                Filter.isEqual(COL_PAYER, company));
            openGrid(VIEW_ORDERS, orderFilter);

            Filter saleFilter = Filter.or(
                Filter.isEqual(TradeConstants.COL_TRADE_CUSTOMER, company),
                Filter.isEqual(TradeConstants.COL_SALE_PAYER, company));
            openGrid("CargoProformaInvoices", saleFilter);
            openGrid(VIEW_CARGO_INVOICES, saleFilter);

            Filter purchaseFilter = Filter.isEqual(TradeConstants.COL_TRADE_SUPPLIER, company);
            openGrid(VIEW_CARGO_PURCHASE_INVOICES, purchaseFilter);
          }
        }));
  }

  @Override
  public void updateMenu(IdentifiableWidget widget) {
  }

  @Override
  protected String getScreenStyle() {
    return STYLE_PREFIX + "screen";
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
  protected Pair<? extends IdentifiableWidget, Integer> initNorth() {
    Pair<? extends IdentifiableWidget, Integer> north = super.initNorth();

    if (north != null && north.getA() != null) {
      Binder.addMouseDownHandler(north.getA().asWidget(), new MouseDownHandler() {
        @Override
        public void onMouseDown(MouseDownEvent event) {
          if (event.getNativeButton() == NativeEvent.BUTTON_RIGHT) {
            int oldSize = getScreenPanel().getDirectionSize(Direction.WEST);
            int newSize = (oldSize > 0) ? 0 : getWidth() / 5;

            getScreenPanel().setDirectionSize(Direction.WEST, newSize, true);
          }
        }
      });
    }

    return north;
  }

  @Override
  protected Pair<? extends IdentifiableWidget, Integer> initWest() {
    Shell shell = new Shell(STYLE_PREFIX + "shell");
    shell.restore();

    Simple wrapper = new Simple(shell);
    return Pair.of(wrapper, 0);
  }

  @Override
  protected void onUserSignatureClick() {
    PasswordService.change();
  }

  private void openGrid(String gridName, boolean intercept, Filter filter) {
    GridOptions gridOptions = (filter == null) ? null : GridOptions.forFilter(filter);
    openGrid(gridName, intercept, gridOptions);
  }

  private void openGrid(String gridName, boolean intercept, GridOptions gridOptions) {
    GridInterceptor gridInterceptor = intercept ? GridFactory.getGridInterceptor(gridName) : null;
    ActivationCallback callback = new ActivationCallback(GridFactory.getSupplierKey(gridName));

    GridFactory.openGrid(gridName, gridInterceptor, gridOptions, callback);
  }

  private void openGrid(String gridName, boolean intercept, String userColumn) {
    GridOptions gridOptions = GridOptions.forCurrentUserFilter(userColumn);
    openGrid(gridName, intercept, gridOptions);
  }

  private void openGrid(String gridName, Filter filter) {
    openGrid(gridName, false, filter);
  }
}
