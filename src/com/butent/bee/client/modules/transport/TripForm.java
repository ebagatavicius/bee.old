package com.butent.bee.client.modules.transport;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.modules.transport.TransportHandler.Profit;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.validation.CellValidation;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditEndEvent;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.TextLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TripForm extends PrintFormInterceptor {

  private FaLabel copyAction;
  private Long defaultDriver;
  private UnboundSelector driver;

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget,
      IdentifiableWidget widget) {

    if (BeeUtils.same(editableWidget.getColumnId(), COL_VEHICLE)) {
      editableWidget.addCellValidationHandler(event -> {
        if (event.isCellValidation() && event.isPostValidation()) {
          CellValidation cellValidation = event.getCellValidation();
          getBeforeInfo(cellValidation.getNewValue(), cellValidation.getRow());
        }
        return true;
      });
    }
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof ChildGrid) {
      switch (name) {
        case TBL_TRIP_DRIVERS:
          ((ChildGrid) widget).setGridInterceptor(new TripDriversGrid(this));
          break;

        case VIEW_TRIP_CARGO:
          ((ChildGrid) widget).setGridInterceptor(new TripCargoGrid());
          break;

        case TBL_TRIP_COSTS:
          ((ChildGrid) widget).setGridInterceptor(new TripCostsGrid());
          break;

        case TBL_TRIP_ROUTES:
          ((ChildGrid) widget).setGridInterceptor(new TripRoutesGrid());
          break;

        case TBL_TRIP_FUEL_COSTS:
          ((ChildGrid) widget).setGridInterceptor(new TripFuelCostsGrid());
          ((ChildGrid) widget).addReadyHandler(re -> {
            GridView gridView = ViewHelper.getChildGrid(getFormView(), TBL_TRIP_FUEL_COSTS);

            if (gridView != null) {
              gridView.getGrid().addMutationHandler(mu ->
                  refreshFuelConsumption(getActiveRow(), null, null));
            }
          });
          break;
      }
    } else if (BeeUtils.same(name, COL_TRIP_ROUTE) && widget instanceof HasClickHandlers) {
      ((HasClickHandlers) widget).addClickHandler(clickEvent -> {
        final Long tripId = getActiveRowId();

        if (DataUtils.isId(tripId)) {
          ParameterList args = TransportHandler.createArgs(SVC_GET_ROUTE);
          args.addDataItem(COL_TRIP, tripId);

          BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
            @Override
            public void onResponse(ResponseObject response) {
              if (response.hasErrors()) {
                response.notify(getFormView());
                return;
              }
              IsRow row = getActiveRow();

              if (row != null && Objects.equals(row.getId(), tripId)) {
                String route = response.getResponseAsString();

                if (BeeUtils.isEmpty(route)) {
                  getFormView().notifyWarning(Localized.dictionary().noData());
                  return;
                }
                Data.setValue(getViewName(), row, COL_TRIP_ROUTE, route);
                getFormView().refreshBySource(COL_TRIP_ROUTE);
              }
            }
          });
        }
      });
    } else if (BeeUtils.same(name, COL_DRIVER) && widget instanceof UnboundSelector) {
      driver = (UnboundSelector) widget;
    }
  }

  @Override
  public void afterInsertRow(final IsRow trip, boolean forced) {
    if (driver != null && DataUtils.isId(driver.getValue())) {
      Queries.insert(VIEW_TRIP_DRIVERS, Data.getColumns(VIEW_TRIP_DRIVERS,
          Arrays.asList(COL_TRIP, COL_DRIVER)), Arrays.asList(BeeUtils.toString(trip.getId()),
          driver.getValue()), null, new RowCallback() {
        @Override
        public void onSuccess(BeeRow res) {
          setMainDriver(trip, res.getId());
        }
      });
    }
    showDriver(false);
    super.afterInsertRow(trip, forced);
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    refreshFuelConsumption(row, null, null);
  }

  @Override
  public TripForm getInstance() {
    return new TripForm();
  }

  @Override
  public FormInterceptor getPrintFormInterceptor() {
    return new PrintTripForm();
  }

  @Override
  public void onEditEnd(EditEndEvent event, Object source) {
    String colId = event.getColumnId();

    if (BeeUtils.inList(colId, COL_FUEL_BEFORE, COL_FUEL_AFTER) && event.valueChanged()) {
      refreshFuelConsumption(getActiveRow(), colId, BeeUtils.toDoubleOrNull(event.getNewValue()));
    }
  }

  @Override
  public void onReadyForInsert(final HasHandlers listener, final ReadyForInsertEvent event) {
    if (checkVehicle(listener, event, event.getColumns(), event.getValues())) {
      event.consume();
    } else {
      super.onReadyForInsert(listener, event);
    }
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    if (checkVehicle(listener, event, event.getColumns(), event.getNewValues())) {
      event.consume();
    } else {
      super.onSaveChanges(listener, event);
    }
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();
    header.addCommandItem(new Profit(COL_TRIP_NO, row.getString(form.getDataIndex(COL_TRIP_NO))));
    header.addCommandItem(getCopyAction());

    showDriver(false);
    return true;
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    form.getViewPresenter().getHeader().clearCommandPanel();
    showDriver(true);
    getBeforeInfo(DataUtils.getStringQuietly(newRow, form.getDataIndex(COL_VEHICLE)), newRow);
  }

  @Override
  public boolean saveOnPrintNewRow() {
    return true;
  }

  void checkDriver(final HasHandlers listener, final GwtEvent<?> event, final Long driverId) {
    if (!DataUtils.isId(driverId)) {
      listener.fireEvent(event);
      return;
    }
    ParameterList args = TransportHandler.createArgs(SVC_GET_DRIVER_BUSY_DATES);
    args.addDataItem(COL_DRIVER, driverId);
    args.addDataItem(COL_TRIP_DATE_FROM, BeeUtils.nvl(getDateValue(COL_TRIP_DATE_FROM),
        getDateTimeValue(COL_TRIP_DATE).getDate()).getTime());

    JustDate to = BeeUtils.nvl(getDateValue(COL_TRIP_DATE_TO),
        getDateValue(COL_TRIP_PLANNED_END_DATE));

    if (to != null) {
      args.addDataItem(COL_TRIP_DATE_TO, to.getTime());
    }
    BeeKeeper.getRpc().makeRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getFormView());

        if (response.hasErrors()) {
          return;
        }
        final List<String> messages = Arrays.asList(Codec.beeDeserializeCollection(response
            .getResponseAsString()));

        if (BeeUtils.isEmpty(messages)) {
          listener.fireEvent(event);
        } else {
          Queries.getRow(TBL_DRIVERS, Filter.compareId(driverId),
              Arrays.asList(ClassifierConstants.COL_FIRST_NAME, ClassifierConstants.COL_LAST_NAME),
              new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              Global.confirm(Localized.dictionary().employment()
                      + " (" + BeeUtils.joinWords(result.getValues()) + ")", Icon.WARNING, messages,
                  () -> listener.fireEvent(event));
            }
          });
        }
      }
    });
  }

  void setMainDriver(IsRow tripRow, Long driverId) {
    Queries.update(getViewName(), tripRow.getId(), tripRow.getVersion(),
        DataUtils.getColumns(getFormView().getDataColumns(),
            Collections.singletonList(COL_MAIN_DRIVER)),
        Collections.singletonList(tripRow.getString(getDataIndex(COL_MAIN_DRIVER))),
        Collections.singletonList(BeeUtils.toString(driverId)), null,
        new RowUpdateCallback(getViewName()) {
          @Override
          public void onSuccess(BeeRow result) {
            super.onSuccess(result);
            Widget drivers = getFormView().getWidgetByName(TBL_TRIP_DRIVERS);

            if (drivers instanceof ChildGrid) {
              ((ChildGrid) drivers).getPresenter().refresh(true, false);
            }
          }
        });
  }

  private boolean checkVehicle(final HasHandlers listener, final GwtEvent<?> event,
      List<BeeColumn> columns, List<String> values) {

    final Long driverId = driver != null ? BeeUtils.toLongOrNull(driver.getValue()) : null;
    String vehicle = null;
    String trailer = null;
    int i = 0;

    for (BeeColumn column : columns) {
      if (BeeUtils.same(column.getId(), COL_VEHICLE)) {
        vehicle = values.get(i);
      } else if (BeeUtils.same(column.getId(), COL_TRAILER)) {
        trailer = values.get(i);
      }
      i++;
    }
    if (DataUtils.isId(vehicle) || DataUtils.isId(trailer)) {
      ParameterList args = TransportHandler.createArgs(SVC_GET_VEHICLE_BUSY_DATES);
      args.addNotEmptyData(COL_VEHICLE, vehicle);
      args.addNotEmptyData(COL_TRAILER, trailer);
      args.addDataItem(COL_TRIP_DATE_FROM, BeeUtils.nvl(getDateValue(COL_TRIP_DATE_FROM),
          getDateTimeValue(COL_TRIP_DATE).getDate()).getTime());

      JustDate to = BeeUtils.nvl(getDateValue(COL_TRIP_DATE_TO),
          getDateValue(COL_TRIP_PLANNED_END_DATE));

      if (to != null) {
        args.addDataItem(COL_TRIP_DATE_TO, to.getTime());
      }
      BeeKeeper.getRpc().makeRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          response.notify(getFormView());

          if (response.hasErrors()) {
            return;
          }
          final List<String> messages = Arrays.asList(Codec.beeDeserializeCollection(response
              .getResponseAsString()));

          if (BeeUtils.isEmpty(messages)) {
            checkDriver(listener, event, driverId);
          } else {
            Global.confirm(Localized.dictionary().employment(), Icon.WARNING, messages,
                () -> checkDriver(listener, event, driverId));
          }
        }
      });
    } else if (DataUtils.isId(driverId)) {
      checkDriver(listener, event, driverId);
    } else {
      return false;
    }
    return true;
  }

  private void getBeforeInfo(String vehicleId, final IsRow row) {
    if (DataUtils.isId(vehicleId)) {
      ParameterList args = TransportHandler.createArgs(SVC_GET_BEFORE);
      args.addDataItem(COL_VEHICLE, vehicleId);
      args.addNotEmptyData(COL_DATE, row.getString(getDataIndex(COL_DATE)));

      BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          response.notify(getFormView());

          if (response.hasErrors()) {
            return;
          }
          Pair<String, String> pair = Pair.restore(response.getResponseAsString());
          row.setValue(getDataIndex(COL_SPEEDOMETER_BEFORE), pair.getA());
          row.setValue(getDataIndex(COL_FUEL_BEFORE), pair.getB());
          getFormView().refresh(false, false);
        }
      });
    }
  }

  private IdentifiableWidget getCopyAction() {
    if (copyAction == null) {
      copyAction = new FaLabel(FontAwesome.COPY);
      copyAction.setTitle(Localized.dictionary().actionCopy());

      copyAction.addClickHandler(clickEvent -> {
        DataInfo info = Data.getDataInfo(getViewName());
        BeeRow newRow = RowFactory.createEmptyRow(info, true);

        for (String col : new String[] {
            COL_VEHICLE, "VehicleNumber", COL_EXPEDITION, "ExpeditionType",
            COL_FORWARDER, "ForwarderName", COL_FORWARDER_VEHICLE, COL_FORWARDER_DRIVER}) {

          int idx = info.getColumnIndex(col);

          if (!BeeConst.isUndef(idx)) {
            newRow.setValue(idx, getStringValue(col));
          }
        }
        TripForm interceptor = getInstance();
        interceptor.defaultDriver = getLongValue(COL_DRIVER);

        RowFactory.createRow(info.getNewRowForm(), info.getNewRowCaption(), info, newRow,
            Modality.ENABLED, null, interceptor, null, null);
      });
    }
    return copyAction;
  }

  private void refreshFuelConsumption(IsRow row, String colId, Double fuel) {
    Widget widget = getFormView().getWidgetByName(TBL_FUEL_CONSUMPTIONS);

    if (!(widget instanceof TextLabel) || row == null) {
      return;
    }
    ((TextLabel) widget).clear();
    Double fuelBefore = row.getDouble(Data.getColumnIndex(getViewName(), COL_FUEL_BEFORE));
    Double fuelAfter = row.getDouble(Data.getColumnIndex(getViewName(), COL_FUEL_AFTER));

    if (BeeUtils.same(colId, COL_FUEL_BEFORE)) {
      fuelBefore = fuel;
    } else if (BeeUtils.same(colId, COL_FUEL_AFTER)) {
      fuelAfter = fuel;
    }

    if (BeeUtils.isDouble(fuelBefore) && BeeUtils.isDouble(fuelAfter)) {
      double fuelConsumption = fuelBefore - fuelAfter;
      GridView grid = ViewHelper.getChildGrid(getFormView(), TBL_TRIP_FUEL_COSTS);

      if (grid != null && !grid.isEmpty()) {
        int fillIndex = Data.getColumnIndex(TBL_TRIP_FUEL_COSTS, COL_COSTS_QUANTITY);

        for (IsRow childRow : grid.getRowData()) {
          fuelConsumption += BeeUtils.toDouble(childRow.getString(fillIndex));
        }
      }
      if (widget instanceof TextLabel) {
        ((TextLabel) widget).setValue(BeeConst.STRING_LEFT_PARENTHESIS
            + BeeConst.CHAR_SPACE + BeeUtils.round(fuelConsumption)
            + BeeConst.CHAR_SPACE + BeeConst.STRING_RIGHT_PARENTHESIS);
      }
    }
  }

  private void showDriver(boolean show) {
    if (driver != null) {
      driver.clearValue();
      driver.setEnabled(show);
      driver.getParent().setVisible(show);

      if (show && DataUtils.isId(defaultDriver)) {
        driver.setValue(defaultDriver, false);
        defaultDriver = null;
      }
    }
  }
}