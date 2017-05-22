package com.butent.bee.client.modules.transport;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.client.validation.CellValidation;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TripRoutesGrid extends AbstractGridInterceptor {
  private String viewName;
  private Integer speedFromIndex;
  private Integer speedToIndex;
  private BeeColumn speedToColumn;
  private Integer kmIndex;
  private BeeColumn kmColumn;

  private List<DataSelector> cargoSelectors = new ArrayList<>();

  private Long trip;
  private Integer scale;
  private FaLabel road = new FaLabel(FontAwesome.ROAD);

  @Override
  public boolean afterCreateColumn(final String columnId, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      final EditableColumn editableColumn) {

    if (BeeUtils.inList(columnId, "SpeedometerFrom", "SpeedometerTo", COL_ROUTE_KILOMETERS)
        && editableColumn != null) {

      editableColumn.addCellValidationHandler(new CellValidateEvent.Handler() {
        @Override
        public Boolean validateCell(CellValidateEvent event) {
          if (event.isCellValidation() && event.isPostValidation()) {
            CellValidation cv = event.getCellValidation();
            IsRow row = cv.getRow();

            BeeColumn updColumn;
            int updIndex;
            Double updValue;
            double newVal = BeeUtils.toDouble(cv.getNewValue());

            if (Objects.equals(columnId, COL_ROUTE_KILOMETERS)) {
              updValue = row.getDouble(speedFromIndex);
              updColumn = speedToColumn;
              updIndex = speedToIndex;
            } else {
              if (Objects.equals(columnId, "SpeedometerFrom")) {
                newVal = 0 - newVal;
                updValue = row.getDouble(speedToIndex);
              } else {
                updValue = 0 - BeeUtils.unbox(row.getDouble(speedFromIndex));
              }
              updColumn = kmColumn;
              updIndex = kmIndex;
            }
            updValue = BeeUtils.unbox(updValue) + newVal;

            if (BeeUtils.isPositive(scale)) {
              if (updValue < 0) {
                updValue += scale;
              } else if (updValue >= scale) {
                updValue -= scale;
              }
            } else if (updValue < 0) {
              updValue = null;
            }
            if (event.isNewRow()) {
              row.setValue(updIndex, updValue);

            } else {
              List<BeeColumn> cols = Lists.newArrayList(cv.getColumn(), updColumn);
              List<String> oldValues = Lists.newArrayList(cv.getOldValue(),
                  row.getString(updIndex));
              List<String> newValues = Lists.newArrayList(cv.getNewValue(),
                  BeeUtils.toString(updValue));

              Queries.update(viewName, row.getId(), row.getVersion(), cols, oldValues, newValues,
                  null, new RowUpdateCallback(viewName));
              return null;
            }
          }
          return true;
        }
      });
    }
    return true;
  }

  @Override
  public void afterCreateEditor(String source, Editor editor, boolean embedded) {
    if (BeeUtils.same(source, COL_ROUTE_CARGO) && editor instanceof DataSelector) {
      cargoSelectors.add((DataSelector) editor);
      setCargoSelectorFilter((DataSelector) editor);
    }
    super.afterCreateEditor(source, editor, embedded);
  }

  @Override
  public void afterCreatePresenter(final GridPresenter presenter) {
    road.setTitle(Localized.dictionary().trGenerateRoute());

    road.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        Global.confirm(Localized.dictionary().trGenerateRoute(), new ConfirmationCallback() {
          @Override
          public void onConfirm() {
            ParameterList args = TransportHandler.createArgs(SVC_GENERATE_ROUTE);
            args.addDataItem(COL_TRIP, trip);

            BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
              @Override
              public void onResponse(ResponseObject response) {
                response.notify(presenter.getGridView());

                if (response.hasErrors()) {
                  return;
                }
                presenter.refresh(false, false);
              }
            });
          }
        });
      }
    });
    presenter.getHeader().addCommandItem(road);
    super.afterCreatePresenter(presenter);
  }

  @Override
  public void beforeCreate(List<? extends IsColumn> dataColumns,
      GridDescription gridDescription) {

    viewName = gridDescription.getViewName();
    speedFromIndex = Data.getColumnIndex(viewName, "SpeedometerFrom");
    speedToIndex = Data.getColumnIndex(viewName, "SpeedometerTo");
    speedToColumn = new BeeColumn(ValueType.NUMBER, "SpeedometerTo");
    kmIndex = Data.getColumnIndex(viewName, COL_ROUTE_KILOMETERS);
    kmColumn = new BeeColumn(ValueType.NUMBER, COL_ROUTE_KILOMETERS);
  }

  @Override
  public GridInterceptor getInstance() {
    return null;
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    if (event.getRow() == null) {
      scale = null;
    } else {
      scale = Data.getInteger(event.getViewName(), event.getRow(), "Speedometer");
    }
    trip = event.getRowId();

    for (DataSelector selector : cargoSelectors) {
      setCargoSelectorFilter(selector);
    }
    road.setVisible(DataUtils.isId(trip));
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
    for (Pair<String, String> pair : Arrays.asList(
        Pair.of(COL_ROUTE_DEPARTURE_COUNTRY, COL_ROUTE_ARRIVAL_COUNTRY),
        Pair.of(COL_ROUTE_DEPARTURE_CITY, COL_ROUTE_ARRIVAL_CITY))) {

      Long newId = newRow.getLong(getDataIndex(pair.getA()));

      if (DataUtils.isId(newId)
          && !Objects.equals(newId, oldRow.getLong(getDataIndex(pair.getA())))
          && Objects.equals(newId, oldRow.getLong(getDataIndex(pair.getB())))) {

        newRow.setValue(getDataIndex(pair.getA() + "Name"),
            oldRow.getString(getDataIndex(pair.getB() + "Name")));
      }
    }
    return super.onStartNewRow(gridView, oldRow, newRow, copy);
  }

  private void setCargoSelectorFilter(DataSelector selector) {
    if (DataUtils.isId(trip)) {
      selector.setAdditionalFilter(Filter.equals(COL_TRIP, trip));
    } else {
      selector.setAdditionalFilter(Filter.isFalse());
    }
  }
}
