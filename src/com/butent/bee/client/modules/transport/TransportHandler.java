package com.butent.bee.client.modules.transport;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_COMPANY_PERSON;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.trade.InvoiceForm;
import com.butent.bee.client.modules.trade.InvoicesGrid;
import com.butent.bee.client.modules.transport.charts.ChartBase;
import com.butent.bee.client.output.Report;
import com.butent.bee.client.output.ReportItem;
import com.butent.bee.client.output.ReportUtils;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.style.ColorStyleProvider;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.FileGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.grid.interceptor.TreeGridInterceptor;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.report.ReportInfo;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class TransportHandler {

  static final class Profit extends Image implements ClickHandler {
    private final String column;
    private final String value;

    Profit(String column, String value) {
      super(Global.getImages().silverProfit());
      setTitle(Localized.dictionary().profit());
      addClickHandler(this);
      this.column = column;
      this.value = value;
    }

    @Override
    public void onClick(ClickEvent event) {
      Report report = Report.TRANSPORT_TRIP_PROFIT;

      ReportUtils.getReports(report, reports -> {
        Consumer<ReportInfo> processor = reportInfo -> {
          for (ReportItem item : report.getItems()) {
            if (Objects.equals(item.getExpression(), column)) {
              reportInfo.getFilterItems().clear();
              reportInfo.getFilterItems().add(item.setFilter(value));
              break;
            }
          }
          report.showModal(new ReportParameters(Collections.singletonMap(COL_RS_REPORT,
              reportInfo.serialize())));
        };

        if (reports.size() > 1) {
          List<String> caps = new ArrayList<>();

          for (ReportInfo rep : reports) {
            caps.add(rep.getCaption());
          }
          Global.choice(Localized.dictionary().report(), null, caps,
              idx -> processor.accept(reports.get(idx)));
        } else {
          processor.accept(BeeUtils.peek(reports));
        }
      });
    }
  }

  private static class RowTransformHandler implements RowTransformEvent.Handler {
    @Override
    public void onRowTransform(RowTransformEvent event) {
      if (event.hasView(VIEW_ASSESSMENTS)) {
        event.setResult(DataUtils.join(Data.getDataInfo(VIEW_ASSESSMENTS), event.getRow(),
            Arrays.asList("ID", COL_STATUS, COL_DATE, "CustomerName", "OrderNotes"),
            BeeConst.STRING_SPACE, Format.getDateRenderer(), Format.getDateTimeRenderer()));
      }
    }
  }

  private static class SparePartsGridHandler extends TreeGridInterceptor {

    @Override
    public SparePartsGridHandler getInstance() {
      return new SparePartsGridHandler();
    }

    @Override
    public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
      IsRow type = getSelectedTreeItem();

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

    @Override
    protected Filter getFilter(Long type) {
      if (type == null) {
        return null;
      } else {
        return Filter.equals("Type", type);
      }
    }

    private String getTypeValue(IsRow type, String colName) {
      if (BeeUtils.allNotNull(type, getTreeDataColumns())) {
        return type.getString(getTreeColumnIndex(colName));
      }
      return null;
    }
  }

  private TransportHandler() {
  }

  public static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.TRANSPORT, method);
  }

  public static void register() {
    MenuService.ASSESSMENTS_GRID.setHandler(
        parameters -> openAssessment(parameters, ViewHelper.getPresenterCallback()));

    ViewFactory.registerSupplier(GridFactory.getSupplierKey(GRID_ASSESSMENT_REQUESTS,
        new AssessmentRequestsGrid()), callback -> openAssessment(GRID_ASSESSMENT_REQUESTS,
        ViewFactory.getPresenterCallback(callback)));
    ViewFactory.registerSupplier(GridFactory.getSupplierKey(GRID_ASSESSMENT_ORDERS, null),
        callback -> openAssessment(GRID_ASSESSMENT_ORDERS,
            ViewFactory.getPresenterCallback(callback)));

    SelectorEvent.register(new TransportSelectorHandler());

    Global.getNewsAggregator().registerFilterHandler(Feed.SHIPMENT_REQUESTS_MY,
        (gridOptions, presenterCallback) -> GridFactory.openGrid(GRID_SHIPMENT_REQUESTS,
            GridFactory.getGridInterceptor(GRID_SHIPMENT_REQUESTS), gridOptions,
            presenterCallback));

    Global.getNewsAggregator().registerFilterHandler(Feed.SHIPMENT_REQUESTS_ALL,
        (gridOptions, presenterCallback) -> GridFactory.openGrid(GRID_SHIPMENT_REQUESTS,
            GridFactory.getGridInterceptor(GRID_SHIPMENT_REQUESTS), gridOptions,
            presenterCallback));

    GridFactory.registerGridInterceptor(VIEW_SPARE_PARTS, new SparePartsGridHandler());

    GridFactory.registerGridInterceptor(VIEW_ORDERS, new CargoInvoiceChecker());
    GridFactory.registerGridInterceptor(VIEW_ORDER_CARGO, new CargoInvoiceChecker());
    GridFactory.registerGridInterceptor(VIEW_TRIPS, new CargoTripChecker());
    GridFactory.registerGridInterceptor(VIEW_EXPEDITION_TRIPS, new CargoTripChecker());

    GridFactory.registerGridInterceptor("CargoDocuments", new TransportDocumentsGrid(COL_CARGO));
    GridFactory.registerGridInterceptor("TranspOrderDocuments",
        new TransportDocumentsGrid(COL_TRANSPORTATION_ORDER));
    GridFactory.registerGridInterceptor("TripDocuments", new TransportDocumentsGrid(COL_TRIP));

    ConditionalStyle.registerGridColumnStyleProvider(VIEW_ABSENCE_TYPES, COL_ABSENCE_COLOR,
        ColorStyleProvider.createDefault(VIEW_ABSENCE_TYPES));
    ConditionalStyle.registerGridColumnStyleProvider(VIEW_CARGO_TYPES, COL_CARGO_TYPE_COLOR,
        ColorStyleProvider.createDefault(VIEW_CARGO_TYPES));

    GridFactory.registerGridInterceptor(VIEW_CARGO_SALES, new CargoSalesGrid());
    GridFactory.registerGridInterceptor(VIEW_CARGO_CREDIT_SALES, new CargoCreditSalesGrid());
    GridFactory.registerGridInterceptor(VIEW_CARGO_PURCHASES, new CargoPurchasesGrid());
    GridFactory.registerGridInterceptor(VIEW_TRIP_PURCHASES, new TripPurchasesGrid());

    GridFactory.registerGridInterceptor(VIEW_CARGO_INVOICES, new InvoicesGrid());
    GridFactory.registerGridInterceptor(VIEW_CARGO_CREDIT_INVOICES, new InvoicesGrid());
    GridFactory.registerGridInterceptor(VIEW_CARGO_PURCHASE_INVOICES, new InvoicesGrid());
    GridFactory.registerGridInterceptor(VIEW_TRIP_PURCHASE_INVOICES, new InvoicesGrid());

    GridFactory.registerGridInterceptor(VIEW_CARGO_FILES,
        new FileGridInterceptor(COL_CARGO, AdministrationConstants.COL_FILE,
            AdministrationConstants.COL_FILE_CAPTION, AdministrationConstants.ALS_FILE_NAME));

    GridFactory.registerGridInterceptor(TBL_CARGO_LOADING, new CargoHandlingGrid());
    GridFactory.registerGridInterceptor(TBL_CARGO_UNLOADING, new CargoHandlingGrid());

    FormFactory.registerFormInterceptor(FORM_ORDER, new TransportationOrderForm());
    FormFactory.registerFormInterceptor(FORM_NEW_SIMPLE_ORDER, new NewSimpleTransportationOrder());
    FormFactory.registerFormInterceptor(FORM_TRIP, new TripForm());
    FormFactory.registerFormInterceptor(FORM_EXPEDITION_TRIP, new TripForm());

    FormFactory.registerFormInterceptor(FORM_TEXT_CONSTANT, new TextConstantForm());

    FormFactory.registerFormInterceptor(FORM_CARGO, new OrderCargoForm());

    FormFactory.registerPreloader(FORM_CARGO, OrderCargoForm::preload);

    FormFactory.registerFormInterceptor(FORM_ASSESSMENT, new AssessmentForm());
    FormFactory.registerFormInterceptor(FORM_ASSESSMENT_FORWARDER, new AssessmentForwarderForm());
    FormFactory.registerFormInterceptor(FORM_ASSESSMENT_TRANSPORTATION,
        new AssessmentTransportationForm());

    FormFactory.registerFormInterceptor(FORM_CARGO_INVOICE, new CargoInvoiceForm());
    FormFactory.registerFormInterceptor(FORM_CARGO_PURCHASE_INVOICE,
        new CargoPurchaseInvoiceForm());
    FormFactory.registerFormInterceptor(FORM_TRIP_PURCHASE_INVOICE, new InvoiceForm(null));

    FormFactory.registerFormInterceptor(FORM_SHIPMENT_REQUEST, new ShipmentRequestForm());
    FormFactory.registerFormInterceptor(FORM_CARGO_PLACE, new CargoPlaceForm());
    FormFactory.registerFormInterceptor(FORM_CARGO_PLACE_UNBOUND, new CargoPlaceForm());

    BeeKeeper.getBus().registerRowActionHandler(new TransportActionHandler());

    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler());

    ChartBase.registerBoards();
    CargoIncomesObserver.register();
  }

  private static void openAssessment(String gridName, PresenterCallback callback) {
    GridInterceptor interceptor;

    switch (gridName) {
      case GRID_ASSESSMENT_REQUESTS:
        interceptor = new AssessmentRequestsGrid();
        break;

      case GRID_ASSESSMENT_ORDERS:
        interceptor = new AssessmentOrdersGrid();
        break;

      default:
        Global.showError(Localized.dictionary().dataNotAvailable(gridName));
        return;
    }
    Long userPerson = BeeKeeper.getUser().getUserData().getCompanyPerson();

    Queries.getRowSet(TBL_DEPARTMENT_EMPLOYEES, Collections.singletonList(COL_DEPARTMENT),
        Filter.and(Filter.equals(COL_COMPANY_PERSON, userPerson),
            Filter.notNull(COL_DEPARTMENT_HEAD)), result -> {

          Set<Long> departments = new HashSet<>();
          result.forEach(row -> departments.add(row.getLong(0)));

          GridFactory.openGrid(gridName, interceptor,
              GridOptions.forFilter(Filter.or(Filter.equals(COL_COMPANY_PERSON, userPerson),
                  Filter.notNull(COL_USER), Filter.any(COL_DEPARTMENT, departments))), callback);
        });
  }
}
