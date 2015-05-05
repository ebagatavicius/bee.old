package com.butent.bee.client.modules.projects;

import com.google.common.collect.Maps;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.grid.column.CalculatedColumn;
import com.butent.bee.client.modules.trade.acts.ItemPricePicker;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowFunction;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants.ProjectEvent;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectIncomesGrid extends AbstractGridInterceptor {

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    switch (columnName) {
      case PROP_ITEM_PRICES:
        if (column instanceof CalculatedColumn) {

          int idxItemPrice = DataUtils.getColumnIndex(COL_PROJECT_ITEM_PRICE, dataColumns);
          CellSource cellSource = CellSource.forColumn(dataColumns.get(idxItemPrice), idxItemPrice);

          RowFunction<Long> currencyFunction = new RowFunction<Long>() {

            @Override
            public Long apply(IsRow activeRow) {
              if (getGridPresenter() == null) {
                return null;
              }

              GridView grid = getGridPresenter().getGridView();

              if (grid == null && activeRow == null) {
                return ViewHelper.getParentValueLong(getGridPresenter().getMainView().asWidget(),
                  VIEW_PROJECTS, COL_PROJECT_CURENCY);
              }

              return activeRow.getLong(
                  grid.getDataIndex(COL_PROJECT_INCOME_CURENCY));
            }
          };

          ItemPricePicker pricePicker =
              new ItemPricePicker(cellSource, dataColumns, currencyFunction);
          ((HasCellRenderer) column).setRenderer(pricePicker);
        }
        break;

      default:
        break;
    }

    return super.afterCreateColumn(columnName, dataColumns, column, header, footer, editableColumn);
  }


  @Override
  public void beforeRefresh(GridPresenter presenter) {
    initHeader(presenter);
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    initHeader(presenter);
    super.afterCreatePresenter(presenter);
  }

  @Override
  public GridInterceptor getInstance() {
    return new ProjectIncomesGrid();
  }

  private void createInvoice() {
    final GridPresenter presenter = getGridPresenter();
    final Set<Long> ids = new HashSet<>();

    for (RowInfo row : presenter.getGridView().getSelectedRows(SelectedRows.ALL)) {
      ids.add(row.getId());
    }
    if (ids.isEmpty()) {
      presenter.getGridView().notifyWarning(Localized.getConstants().selectAtLeastOneRow());
      return;
    }

    Queries.getRowSet(VIEW_PROJECT_INCOMES, null, Filter.and(Filter.idIn(ids), Filter
        .isNull(TradeConstants.COL_SALE)), new RowSetCallback() {

      @Override
      public void onSuccess(final BeeRowSet result) {
        if (result.isEmpty()) {
          presenter.getGridView().notifyWarning(Localized.getConstants().selectAtLeastOneRow());
        }

        final FormView parentForm = ViewHelper.getForm(presenter.getMainView());
        Pair<Long, String> customer = null;
        Pair<Long, String> currency = null;

        final DataInfo salesInfo = Data.getDataInfo(VIEW_PROJECT_INVOICES);
        final BeeRow newSalesRow = RowFactory.createEmptyRow(salesInfo, true);

        newSalesRow.setValue(salesInfo.getColumnIndex(TradeConstants.COL_TRADE_MANAGER), BeeKeeper
            .getUser().getUserId());

        newSalesRow.setValue(salesInfo.getColumnIndex(TradeConstants.COL_TRADE_MANAGER
            + ClassifierConstants.COL_FIRST_NAME), BeeKeeper.getUser().getFirstName());

        newSalesRow.setValue(salesInfo.getColumnIndex(TradeConstants.COL_TRADE_MANAGER
            + ClassifierConstants.COL_LAST_NAME), BeeKeeper.getUser().getLastName());

        if (parentForm != null && parentForm.getActiveRow() != null) {
          IsRow projectRow = parentForm.getActiveRow();
          DataInfo projectDataInfo = Data.getDataInfo(parentForm.getViewName());

          customer = Pair.of(
              projectRow.getLong(projectDataInfo.getColumnIndex(ClassifierConstants.COL_COMPANY)),
              projectRow.getString(projectDataInfo
                  .getColumnIndex(ClassifierConstants.ALS_COMPANY_NAME)));

          currency =
              Pair.of(
                  projectRow.getLong(projectDataInfo
                      .getColumnIndex(AdministrationConstants.COL_CURRENCY)),
                  projectRow.getString(projectDataInfo
                      .getColumnIndex(AdministrationConstants.ALS_CURRENCY_NAME))
                  );
        }

        if (customer != null) {
          newSalesRow.setValue(salesInfo.getColumnIndex(TradeConstants.COL_TRADE_CUSTOMER),
              customer.getA());
          newSalesRow.setValue(salesInfo.getColumnIndex(TradeConstants.ALS_CUSTOMER_NAME), customer
              .getB());
        }

        if (currency != null) {
          newSalesRow.setValue(salesInfo.getColumnIndex(TradeConstants.COL_TRADE_CURRENCY),
              currency.getA());
          newSalesRow.setValue(salesInfo.getColumnIndex(AdministrationConstants.ALS_CURRENCY_NAME),
              currency.getB());
        }

        RowFactory.createRow(FORM_NEW_PROJECT_INVOICE, null, salesInfo, newSalesRow, null,
            new AbstractFormInterceptor() {

              @Override
              public FormInterceptor getInstance() {
                return this;
              }
            }, new RowCallback() {

              @Override
              public void onSuccess(final BeeRow row) {
                ParameterList args = ProjectsKeeper.createSvcArgs(SVC_CREATE_INVOICE_ITEMS);
                args.addDataItem(TradeConstants.COL_SALE, row.getId());
                args.addDataItem(TradeConstants.COL_TRADE_CURRENCY, row.getLong(salesInfo
                    .getColumnIndex(TradeConstants.COL_TRADE_CURRENCY)));
                args.addDataItem(Service.VAR_ID, DataUtils.buildIdList(result.getRowIds()));

                BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {

                  @Override
                  public void onResponse(ResponseObject response) {
                    response.notify(presenter.getGridView());

                    if (response.hasErrors()) {
                      return;
                    }

                    Popup popup = UiHelper.getParentPopup(presenter.getGridView().getGrid());

                    if (popup != null) {
                      popup.close();
                    }
                    // Data.onViewChange(presenter.getViewName(),
                    // DataChangeEvent.CANCEL_RESET_REFRESH);
                    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_PROJECT_INCOMES);
                    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_PROJECT_INVOICES);

                    Map<String, Map<String, String>> oldData = Maps.newHashMap();
                    Map<String, Map<String, String>> newData = Maps.newHashMap();
                    Map<String, String> commentData = Maps.newHashMap();

                    commentData.put(TradeConstants.COL_SALE, BeeUtils.toString(row.getId()));
                    oldData.put(VIEW_PROJECT_INVOICES, commentData);
                    newData.put(VIEW_PROJECT_INVOICES, commentData);

                    ProjectsHelper.registerProjectEvent(VIEW_PROJECT_EVENTS, ProjectEvent.EDIT,
                        parentForm.getActiveRowId(), BeeConst.STRING_EMPTY,
                        newData, oldData);
                  }
                });
              }
            });
      }
    });
  }

  private void initHeader(GridPresenter presenter) {
    GridView gridView = presenter.getGridView();
    presenter.getHeader().clearCommandPanel();
    IsRow row = presenter.getActiveRow();
    final FormView parentForm = ViewHelper.getForm(presenter.getMainView());
    long owner = BeeConst.LONG_UNDEF;
    int idxOwner = getDataIndex(COL_PROJECT_OWNER);

    if (row != null && !BeeConst.isUndef(idxOwner)) {
      owner = BeeUtils.unbox(row.getLong(idxOwner));
    } else if (parentForm != null) {
      owner = BeeUtils.unbox(parentForm.getLongValue(COL_PROJECT_OWNER));
    }

    if (gridView == null || gridView.isReadOnly()
        || !BeeKeeper.getUser().canCreateData(VIEW_PROJECT_INCOMES)
        || owner != BeeKeeper.getUser().getUserId()) {

      return;
    }
    FaLabel createInvoiceButton = new FaLabel(FontAwesome.LIST_ALT);
    createInvoiceButton.setTitle(Localized.getConstants().createInvoice());

    createInvoiceButton.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg0) {
        createInvoice();
      }
    });

    presenter.getHeader().addCommandItem(createInvoiceButton);
  }

}
