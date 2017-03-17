package com.butent.bee.client.modules.transport;

import com.google.common.collect.Sets;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.cli.Shell;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.logging.ClientLogManager;
import com.butent.bee.client.modules.administration.PasswordService;
import com.butent.bee.client.output.ReportUtils;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.screen.ScreenImpl;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.HashMap;
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

      activeViews.put(viewKey, presenter.getMainView().getId());
      showInNewPlace(presenter.getMainView());
    }
  }

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "tr-SelfService-";

  private final Map<String, String> activeViews = new HashMap<>();

  @Override
  public void closeAll() {
    super.closeAll();
    activeViews.clear();
  }

  @Override
  public UserInterface getUserInterface() {
    return UserInterface.SELF_SERVICE;
  }

  @Override
  public void start(UserData userData) {
    super.start(userData);

    Data.setVisibleViews(Sets.newHashSet(VIEW_SHIPMENT_REQUESTS, TBL_CARGO_LOADING,
        TBL_CARGO_UNLOADING, VIEW_CARGO_FILES, VIEW_CARGO_INVOICES));

    Data.setReadOnlyViews(Collections.singleton(VIEW_CARGO_INVOICES));

    Data.setColumnReadOnly(VIEW_SHIPMENT_REQUESTS, ClassifierConstants.COL_COMPANY_PERSON);

    GridFactory.hideColumn("ShipmentRegisteredRequests", COL_QUERY_STATUS);
    GridFactory.hideColumn("ShipmentRegisteredRequests", COL_QUERY_REASON);
    GridFactory.hideColumn(VIEW_CARGO_INVOICES, "Select");

    FormFactory.hideWidget(FORM_SHIPMENT_REQUEST, COL_ORDER_ID);
    FormFactory.hideWidget(FORM_SHIPMENT_REQUEST, COL_STATUS);
    FormFactory.hideWidget(FORM_SHIPMENT_REQUEST, "AdditionalInfo");
    FormFactory.hideWidget(FORM_SHIPMENT_REQUEST, "RelatedMessages");
    FormFactory.hideWidget(FORM_SHIPMENT_REQUEST, VIEW_CARGO_INCOMES);

    if (getCommandPanel() != null) {
      getCommandPanel().clear();
    }
    addCommandItem(new Button(Localized.dictionary().trSelfServiceCommandNewRequest(),
        event -> {
          DataInfo info = Data.getDataInfo(VIEW_SHIPMENT_REQUESTS);
          BeeRow row = RowFactory.createEmptyRow(info, true);

          RowFactory.createRow(info, row, Modality.ENABLED, new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              openRequests();
              showSuccessInfo(result);
            }
          });
        }));
    addCommandItem(new Button(Localized.dictionary().trSelfServiceCommandRequests(),
        event -> openRequests()));

    addCommandItem(new Button(Localized.dictionary().ecInvoices(),
        event -> openGrid(VIEW_CARGO_INVOICES, Filter.or(
            Filter.equals(TradeConstants.COL_TRADE_CUSTOMER, BeeKeeper.getUser().getCompany()),
            Filter.equals(TradeConstants.COL_SALE_PAYER, BeeKeeper.getUser().getCompany())),
            new AbstractGridInterceptor() {
              @Override
              public GridInterceptor getInstance() {
                return null;
              }

              @Override
              public void onEditStart(EditStartEvent ev) {
                ev.consume();
                Queries.getRowSet(TradeConstants.VIEW_SALE_FILES, null,
                    Filter.equals(TradeConstants.COL_SALE, ev.getRowValue().getId()),
                    new Queries.RowSetCallback() {
                      @Override
                      public void onSuccess(BeeRowSet result) {
                        if (!DataUtils.isEmpty(result)) {
                          int r = result.getNumberOfRows() - 1;
                          FileInfo fileInfo = new FileInfo(result.getLong(r, COL_FILE),
                              result.getString(r, COL_FILE_HASH),
                              result.getString(r, ALS_FILE_NAME),
                              result.getLong(r, ALS_FILE_SIZE),
                              result.getString(r, ALS_FILE_TYPE));

                          fileInfo.setCaption(result.getString(r, COL_FILE_CAPTION));

                          ReportUtils.preview(fileInfo);
                        }
                      }
                    });
              }
            })));
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
  protected Pair<? extends IdentifiableWidget, Integer> initWest() {
    Shell shell = new Shell(STYLE_PREFIX + "shell");
    shell.restore();

    Simple wrapper = new Simple(shell);
    return Pair.of(wrapper, 0);
  }

  @Override
  protected void onUserSignatureClick(ClickEvent event) {
    PasswordService.change();
  }

  private void openGrid(String gridName, Filter filter, GridInterceptor interceptor) {
    GridOptions gridOptions = (filter == null) ? null : GridOptions.forFilter(filter);
    GridInterceptor gridInterceptor = BeeUtils.nvl(interceptor,
        GridFactory.getGridInterceptor(gridName));
    ActivationCallback callback = new ActivationCallback(GridFactory.getSupplierKey(gridName,
        gridInterceptor));

    GridFactory.openGrid(gridName, gridInterceptor, gridOptions, callback);
  }

  private void openRequests() {
    openGrid(GRID_SHIPMENT_REQUESTS, Filter.isEqual(ClassifierConstants.COL_COMPANY_PERSON,
        Value.getValue(BeeKeeper.getUser().getUserData().getCompanyPerson())), null);
  }

  private static void showSuccessInfo(BeeRow result) {
    ParameterList args = TransportHandler.createArgs(SVC_GET_TEXT_CONSTANT);
    args.addDataItem(COL_TEXT_CONSTANT, TextConstant.SUMBMITTED_REQUEST_CONTENT.ordinal());
    args.addDataItem(COL_USER_LOCALE, result.getInteger(
        Data.getColumnIndex(VIEW_SHIPMENT_REQUESTS, COL_USER_LOCALE)));

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        String message = (String) response.getResponse();
        if (!BeeUtils.isEmpty(message)) {
          Global.showInfo(message);
        }
      }
    });
  }
}
