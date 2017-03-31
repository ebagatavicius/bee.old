package com.butent.bee.client.modules.transport;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.data.RowInsertCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TripCargoGrid extends PercentEditor {

  private static final class Action implements ClickHandler {

    private final GridView gridView;
    private final int cargoIndex;
    private final int tripIndex;
    private final DialogBox dialog;

    private Action(GridView gridView) {
      CellGrid grd = gridView.getGrid();
      this.gridView = gridView;
      this.cargoIndex = DataUtils.getColumnIndex(COL_CARGO, gridView.getDataColumns());
      this.tripIndex = DataUtils.getColumnIndex(COL_TRIP, gridView.getDataColumns());

      this.dialog = DialogBox.create(Localized.dictionary().trAssignCargo());
      dialog.setHideOnEscape(true);

      HtmlTable container = new HtmlTable();
      container.setBorderSpacing(5);

      container.setText(0, 0, Localized.dictionary().trCargoSelectCargo());

      Relation relation = Relation.create(VIEW_WAITING_CARGO,
          Lists.newArrayList("OrderNo", "CustomerName", "LoadingPostIndex", "LoadingCountryName",
              "UnloadingPostIndex", "UnloadingCountryName"));
      relation.disableNewRow();

      CompoundFilter filter = Filter.and();

      for (IsRow row : grd.getRowData()) {
        filter.add(Filter.compareId(Operator.NE, row.getLong(cargoIndex)));
      }
      relation.setFilter(filter);
      relation.setCaching(Relation.Caching.QUERY);

      final UnboundSelector selector = UnboundSelector.create(relation,
          Lists.newArrayList("OrderNo", "Description"));

      selector.addEditStopHandler(event -> {
        if (event.isChanged()) {
          addCargo(BeeUtils.toLong(selector.getValue()));
        }
      });
      container.setWidget(0, 1, selector);

      Button orderButton = new Button(Localized.dictionary().newTransportationOrder(), this);
      container.setWidget(1, 0, orderButton);

      dialog.setWidget(container);
    }

    @Override
    public void onClick(ClickEvent clickEvent) {
      RowFactory.createRow(TBL_ORDERS, Modality.ENABLED, new RowInsertCallback(TBL_ORDERS) {
        @Override
        public void onSuccess(BeeRow result) {
          super.onSuccess(result);
          final long orderId = result.getId();

          Queries.getRowSet(TBL_ORDER_CARGO, Collections.singletonList(COL_ORDER),
              Filter.equals(COL_ORDER, orderId), new Queries.RowSetCallback() {
                @Override
                public void onSuccess(final BeeRowSet res) {
                  if (DataUtils.isEmpty(res)) {
                    Queries.deleteRow(TBL_ORDERS, orderId);
                    gridView.notifyWarning(Localized.dictionary().noData());
                    return;
                  }
                  gridView.ensureRelId(tripId -> {
                    for (BeeRow row : res) {
                      insertCargo(tripId, row.getId());
                    }
                  });
                  dialog.close();
                }
              });
        }
      });
    }

    private void addCargo(final long cargoId) {
      if (!DataUtils.isId(cargoId)) {
        return;
      }
      dialog.close();

      gridView.ensureRelId(result -> insertCargo(result, cargoId));
    }

    private void insertCargo(long tripId, long cargoId) {
      List<BeeColumn> columns = DataUtils.getColumns(gridView.getDataColumns(), tripIndex,
          cargoIndex);
      List<String> values = Queries.asList(tripId, cargoId);

      Queries.insert(gridView.getViewName(), columns, values, null, new RowCallback() {
        @Override
        public void onSuccess(BeeRow row) {
          RowInsertEvent.fire(BeeKeeper.getBus(), gridView.getViewName(), row, gridView.getId());
          gridView.getGrid().insertRow(row, false);
        }
      });
    }
  }

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {
    if (BeeUtils.in(columnName, ALS_LOADING_DATE, ALS_UNLOADING_DATE)) {
      column.setSortable(true);
      column.setSortBy(Arrays.asList(COL_CARGO + columnName));
    }
    return super.afterCreateColumn(columnName, dataColumns, column, header, footer, editableColumn);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    Action action = new Action(presenter.getGridView());
    action.dialog.focusOnOpen(action.dialog.getContent());

    CellGrid grd = presenter.getGridView().getGrid();
    action.dialog.showAt(grd.getAbsoluteLeft(), grd.getAbsoluteTop());

    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return new TripCargoGrid();
  }

  @Override
  public void onDataReceived(List<? extends IsRow> rows) {
    super.onDataReceived(rows);
    Order order = getGridView().getGrid().getSortOrder();

    if (rows != null && (!BeeConst.isUndef(order.getIndex(ALS_LOADING_DATE))
        || !BeeConst.isUndef(order.getIndex(ALS_UNLOADING_DATE)))) {
      Collections.sort(rows, (row1, row2) -> {
        int result = BeeConst.COMPARE_EQUAL;

        for (Order.Column column : order.getColumns()) {
          if (result == BeeConst.COMPARE_EQUAL) {
            if (BeeUtils.in(column.getName(), ALS_LOADING_DATE, ALS_UNLOADING_DATE)) {
              String propertyKey = BeeUtils.removePrefix(column.getName(), COL_CARGO);
              Long date1 = BeeUtils.toLong(row1.getProperty(propertyKey));
              Long date2 = BeeUtils.toLong(row2.getProperty(propertyKey));

              if (order.isAscending(column.getName())) {
                result = date1.compareTo(date2);
              } else {
                result = date2.compareTo(date1);
              }
            } else {
              for (String sortBy : column.getSources()) {
                if (result == BeeConst.COMPARE_EQUAL) {
                  if (order.isAscending(column.getName())) {
                    result = row1.getString(getDataIndex(sortBy))
                        .compareTo(row2.getString(getDataIndex(sortBy)));
                  } else {
                    result = row2.getString(getDataIndex(sortBy))
                        .compareTo(row1.getString(getDataIndex(sortBy)));
                  }
                }
              }
            }
          }
        }
        return result;
      });
    }
  }
}
