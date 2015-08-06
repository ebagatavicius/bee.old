package com.butent.bee.client.modules.transport;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_COMPANY_PERSON;
import static com.butent.bee.shared.modules.trade.TradeConstants.VAR_TOTAL;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.modules.trade.InvoiceForm;
import com.butent.bee.client.modules.trade.InvoicesGrid;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.modules.transport.charts.ChartBase;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.presenter.TreePresenter;
import com.butent.bee.client.render.ProvidesGridColumnRenderer;
import com.butent.bee.client.render.RendererFactory;
import com.butent.bee.client.style.ColorStyleProvider;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.TreeView;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.ViewSupplier;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.FileGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TransportHandler {

  private static class CargoGridHandler extends AbstractGridInterceptor {
    @Override
    public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
        Collection<RowInfo> selectedRows, DeleteMode defMode) {

      return new CargoTripChecker().getDeleteMode(presenter, activeRow, selectedRows, defMode);
    }

    @Override
    public GridInterceptor getInstance() {
      return new CargoGridHandler();
    }
  }

  static final class Profit extends Image implements ClickHandler {
    private final String idName;
    private final long id;

    public Profit(String idName, long id) {
      super(Global.getImages().silverProfit());
      setTitle(Localized.getConstants().profit());
      addClickHandler(this);
      this.idName = idName;
      this.id = id;
    }

    @Override
    public void onClick(ClickEvent event) {
      ParameterList args = TransportHandler.createArgs(SVC_GET_PROFIT);
      args.addDataItem(idName, id);

      BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          Assert.notNull(response);

          if (response.hasErrors()) {
            Global.showError(Lists.newArrayList(response.getErrors()));

          } else if (response.hasArrayResponse(String.class)) {
            String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());
            List<String> messages = new ArrayList<>();

            if (arr != null && arr.length % 2 == 0) {
              for (int i = 0; i < arr.length; i += 2) {
                messages.add(BeeUtils.joinWords(arr[i],
                    BeeUtils.isDouble(arr[i + 1]) ? BeeUtils.round(arr[i + 1], 2) : arr[i + 1]));
              }
            }
            Global.showInfo(messages);

          } else {
            Global.showError("Unknown response");
          }
        }
      });
    }
  }

  private static class RowTransformHandler implements RowTransformEvent.Handler {
    @Override
    public void onRowTransform(RowTransformEvent event) {
      if (event.hasView(VIEW_ASSESSMENTS)) {
        event.setResult(DataUtils.join(Data.getDataInfo(VIEW_ASSESSMENTS), event.getRow(),
            Lists.newArrayList("ID", COL_STATUS, COL_DATE, "CustomerName", "OrderNotes"),
            BeeConst.STRING_SPACE));
      }
    }
  }

  private static class SparePartsGridHandler extends AbstractGridInterceptor
      implements SelectionHandler<IsRow> {

    private static final String FILTER_KEY = "f1";
    private IsRow selectedType;
    private TreePresenter typeTree;

    @Override
    public void afterCreateWidget(String name, IdentifiableWidget widget,
        WidgetDescriptionCallback callback) {
      if (widget instanceof TreeView && BeeUtils.same(name, "SparePartTypes")) {
        ((TreeView) widget).addSelectionHandler(this);
        typeTree = ((TreeView) widget).getTreePresenter();
      }
    }

    @Override
    public SparePartsGridHandler getInstance() {
      return new SparePartsGridHandler();
    }

    @Override
    public void onSelection(SelectionEvent<IsRow> event) {
      if (event == null) {
        return;
      }
      if (getGridPresenter() != null) {
        Long type = null;
        setSelectedType(event.getSelectedItem());

        if (getSelectedType() != null) {
          type = getSelectedType().getId();
        }
        getGridPresenter().getDataProvider().setParentFilter(FILTER_KEY, getFilter(type));
        getGridPresenter().refresh(true);
      }
    }

    @Override
    public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
      IsRow type = getSelectedType();

      if (type != null) {
        List<BeeColumn> cols = getGridPresenter().getDataColumns();
        newRow.setValue(DataUtils.getColumnIndex("Type", cols), type.getId());
        newRow.setValue(DataUtils.getColumnIndex("ParentTypeName", cols),
            getTypeValue(type, "ParentName"));
        newRow.setValue(DataUtils.getColumnIndex("TypeName", cols),
            getTypeValue(type, "Name"));
      }
      return true;
    }

    private static Filter getFilter(Long type) {
      if (type == null) {
        return null;
      } else {
        return Filter.equals("Type", type);
      }
    }

    private IsRow getSelectedType() {
      return selectedType;
    }

    private String getTypeValue(IsRow type, String colName) {
      if (BeeUtils.allNotNull(type, typeTree, typeTree.getDataColumns())) {
        return type.getString(DataUtils.getColumnIndex(colName, typeTree.getDataColumns()));
      }
      return null;
    }

    private void setSelectedType(IsRow selectedType) {
      this.selectedType = selectedType;
    }
  }

  private static boolean bindExpensesToIncomes;

  private TransportHandler() {
  }

  public static boolean bindExpensesToIncomes() {
    return bindExpensesToIncomes;
  }

  public static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.TRANSPORT, method);
  }

  public static void register() {
    MenuService.ASSESSMENTS_GRID.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        openAssessment(parameters, ViewHelper.getPresenterCallback());
      }
    });

    ViewFactory.registerSupplier(GridFactory.getSupplierKey(GRID_ASSESSMENT_REQUESTS, null),
        new ViewSupplier() {
          @Override
          public void create(ViewCallback callback) {
            openAssessment(GRID_ASSESSMENT_REQUESTS, ViewFactory.getPresenterCallback(callback));
          }
        });
    ViewFactory.registerSupplier(GridFactory.getSupplierKey(GRID_ASSESSMENT_ORDERS, null),
        new ViewSupplier() {
          @Override
          public void create(ViewCallback callback) {
            openAssessment(GRID_ASSESSMENT_ORDERS, ViewFactory.getPresenterCallback(callback));
          }
        });

    SelectorEvent.register(new TransportSelectorHandler());

    GridFactory.registerGridInterceptor(VIEW_SPARE_PARTS, new SparePartsGridHandler());
    GridFactory.registerGridInterceptor(VIEW_CARGO_TRIPS, new CargoTripsGrid());

    GridFactory.registerGridInterceptor(VIEW_ORDERS, new CargoTripChecker());
    GridFactory.registerGridInterceptor(VIEW_TRIPS, new CargoTripChecker());
    GridFactory.registerGridInterceptor(VIEW_EXPEDITION_TRIPS, new CargoTripChecker());

    GridFactory.registerGridInterceptor(VIEW_ORDER_CARGO, new CargoGridHandler());

    GridFactory.registerGridInterceptor("CargoDocuments", new TransportDocumentsGrid(COL_CARGO));
    GridFactory.registerGridInterceptor("TranspOrderDocuments",
        new TransportDocumentsGrid(COL_TRANSPORTATION_ORDER));
    GridFactory.registerGridInterceptor("TripDocuments", new TransportDocumentsGrid(COL_TRIP));

    GridFactory.registerGridInterceptor(GRID_SHIPMENT_REQUESTS, new ShipmentRequestsGrid());
    GridFactory.registerGridInterceptor(GRID_LOGISTICS_CARGO_REQUESTS,
        new LogisticsShipmentRequestsGrid());

    ProvidesGridColumnRenderer provider = new CargoPlaceRenderer.Provider();
    String loading = "Loading";
    String unloading = "Unloading";

    RendererFactory.registerGcrProvider(VIEW_CARGO_HANDLING, loading, provider);
    RendererFactory.registerGcrProvider(VIEW_CARGO_HANDLING, unloading, provider);
    RendererFactory.registerGcrProvider(VIEW_ALL_CARGO, loading, provider);
    RendererFactory.registerGcrProvider(VIEW_ALL_CARGO, unloading, provider);
    RendererFactory.registerGcrProvider(VIEW_ORDER_CARGO, loading, provider);
    RendererFactory.registerGcrProvider(VIEW_ORDER_CARGO, unloading, provider);
    RendererFactory.registerGcrProvider(VIEW_CARGO_TRIPS, loading, provider);
    RendererFactory.registerGcrProvider(VIEW_CARGO_TRIPS, unloading, provider);
    RendererFactory.registerGcrProvider(VIEW_TRIP_CARGO, loading, provider);
    RendererFactory.registerGcrProvider(VIEW_TRIP_CARGO, unloading, provider);

    ConditionalStyle.registerGridColumnStyleProvider(VIEW_ABSENCE_TYPES, COL_ABSENCE_COLOR,
        ColorStyleProvider.createDefault(VIEW_ABSENCE_TYPES));
    ConditionalStyle.registerGridColumnStyleProvider(VIEW_CARGO_TYPES, COL_CARGO_TYPE_COLOR,
        ColorStyleProvider.createDefault(VIEW_CARGO_TYPES));

    TradeUtils.registerTotalRenderer(TBL_TRIP_COSTS, VAR_TOTAL);
    TradeUtils.registerTotalRenderer(TBL_TRIP_FUEL_COSTS, VAR_TOTAL);
    TradeUtils.registerTotalRenderer(TBL_CARGO_INCOMES, VAR_TOTAL);
    TradeUtils.registerTotalRenderer(TBL_CARGO_EXPENSES, VAR_TOTAL);
    TradeUtils.registerTotalRenderer(VIEW_CARGO_SALES, VAR_TOTAL);
    TradeUtils.registerTotalRenderer(VIEW_CARGO_CREDIT_SALES, VAR_TOTAL);
    TradeUtils.registerTotalRenderer(VIEW_CARGO_PURCHASES, VAR_TOTAL);
    TradeUtils.registerTotalRenderer(VIEW_TRIP_PURCHASES, VAR_TOTAL);
    TradeUtils.registerTotalRenderer("UnassignedTripCosts", VAR_TOTAL);
    TradeUtils.registerTotalRenderer("UnassignedFuelCosts", VAR_TOTAL);

    GridFactory.registerGridInterceptor(VIEW_CARGO_SALES, new CargoSalesGrid());
    GridFactory.registerGridInterceptor(VIEW_CARGO_CREDIT_SALES, new CargoCreditSalesGrid());
    GridFactory.registerGridInterceptor(VIEW_CARGO_PURCHASES, new CargoPurchasesGrid());
    GridFactory.registerGridInterceptor(VIEW_TRIP_PURCHASES, new TripPurchasesGrid());

    GridFactory.registerGridInterceptor(VIEW_CARGO_INVOICES, new InvoicesGrid());
    GridFactory.registerGridInterceptor(VIEW_CARGO_CREDIT_INVOICES, new InvoicesGrid());
    GridFactory.registerGridInterceptor(VIEW_CARGO_PURCHASE_INVOICES, new InvoicesGrid());
    GridFactory.registerGridInterceptor(VIEW_TRIP_PURCHASE_INVOICES, new InvoicesGrid());

    GridFactory.registerGridInterceptor(VIEW_CARGO_REQUESTS, new CargoRequestsGrid());
    GridFactory.registerGridInterceptor(VIEW_CARGO_REQUEST_FILES,
        new FileGridInterceptor(COL_CRF_REQUEST, AdministrationConstants.COL_FILE,
            AdministrationConstants.COL_FILE_CAPTION, AdministrationConstants.ALS_FILE_NAME));

    if (!BeeKeeper.getUser().isAdministrator()) {
      Filter mngFilter = Filter.or(BeeKeeper.getUser().getFilter(COL_ORDER_MANAGER),
          Filter.isNull(COL_ORDER_MANAGER));

      GridFactory.registerImmutableFilter(VIEW_ORDERS, mngFilter);
      GridFactory.registerImmutableFilter(VIEW_ALL_CARGO, mngFilter);
    }

    FormFactory.registerFormInterceptor(FORM_ORDER, new TransportationOrderForm());
    FormFactory.registerFormInterceptor(FORM_NEW_SIMPLE_ORDER, new NewSimpleTransportationOrder());
    FormFactory.registerFormInterceptor(FORM_TRIP, new TripForm());
    FormFactory.registerFormInterceptor(FORM_EXPEDITION_TRIP, new TripForm());

    FormFactory.registerFormInterceptor(FORM_CARGO, new OrderCargoForm());

    final Consumer<ScheduledCommand> assessmentConsumer = new Consumer<ScheduledCommand>() {
      @Override
      public void accept(final ScheduledCommand command) {
        Global.getParameter(PRM_BIND_EXPENSES_TO_INCOMES, new Consumer<String>() {
          @Override
          public void accept(String prm) {
            bindExpensesToIncomes = BeeUtils.unbox(BeeUtils.toBoolean(prm));
            command.execute();
          }
        });
      }
    };
    FormFactory.registerPreloader(FORM_CARGO, new Consumer<ScheduledCommand>() {
      @Override
      public void accept(final ScheduledCommand command) {
        OrderCargoForm.preload(new ScheduledCommand() {
          @Override
          public void execute() {
            assessmentConsumer.accept(command);
          }
        });
      }
    });
    FormFactory.registerPreloader(FORM_ASSESSMENT, assessmentConsumer);
    FormFactory.registerPreloader(FORM_ASSESSMENT_FORWARDER, assessmentConsumer);

    FormFactory.registerFormInterceptor(FORM_ASSESSMENT, new AssessmentForm());
    FormFactory.registerFormInterceptor(FORM_ASSESSMENT_FORWARDER, new AssessmentForwarderForm());
    FormFactory.registerFormInterceptor(FORM_ASSESSMENT_TRANSPORTATION,
        new AssessmentTransportationForm());

    FormFactory.registerFormInterceptor(FORM_CARGO_INVOICE, new CargoInvoiceForm());
    FormFactory.registerFormInterceptor(FORM_CARGO_PURCHASE_INVOICE,
        new CargoPurchaseInvoiceForm());
    FormFactory.registerFormInterceptor(FORM_TRIP_PURCHASE_INVOICE, new InvoiceForm(null));

    FormFactory.registerFormInterceptor(FORM_REGISTRATION, new TransportRegistrationForm());
    FormFactory.registerFormInterceptor(FORM_SHIPMENT_REQUEST, new ShipmentRequestForm());
    FormFactory.registerFormInterceptor(FORM_NEW_CARGO_REQUEST, new CargoRequestForm());
    FormFactory.registerFormInterceptor(FORM_CARGO_REQUEST, new CargoRequestForm());

    BeeKeeper.getBus().registerRowActionHandler(new TransportActionHandler(), false);

    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler(), false);

    ChartBase.registerBoards();
    CargoIncomesObserver.register();
  }

  private static void openAssessment(final String gridName, final PresenterCallback callback) {
    final GridInterceptor interceptor;

    switch (gridName) {
      case GRID_ASSESSMENT_REQUESTS:
        interceptor = null;
        break;

      case GRID_ASSESSMENT_ORDERS:
        interceptor = new AssessmentOrdersGrid();
        break;

      default:
        Global.showError(Localized.getMessages().dataNotAvailable(gridName));
        return;
    }
    final Long userPerson = BeeKeeper.getUser().getUserData().getCompanyPerson();

    Queries.getRowSet(TBL_DEPARTMENT_EMPLOYEES, Lists.newArrayList(COL_DEPARTMENT),
        Filter.and(Filter.equals(COL_COMPANY_PERSON, userPerson),
            Filter.notNull(COL_DEPARTMENT_HEAD)), new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            Set<Long> departments = new HashSet<>();
            Long user = BeeKeeper.getUser().getUserId();

            for (BeeRow row : result) {
              departments.add(row.getLong(0));
            }
            GridFactory.openGrid(gridName, interceptor,
                GridOptions.forFilter(Filter.or(Lists.newArrayList(
                    Filter.equals(COL_COMPANY_PERSON, userPerson),
                    Filter.any(COL_DEPARTMENT, departments),
                    Filter.equals(COL_USER, user), Filter.equals(COL_GROUP, user)))),
                callback);
          }
        });
  }
}
