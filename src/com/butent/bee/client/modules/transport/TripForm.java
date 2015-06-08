package com.butent.bee.client.modules.transport;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.modules.transport.TransportHandler.Profit;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.client.validation.CellValidation;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Objects;

public class TripForm extends AbstractFormInterceptor {
  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget,
      IdentifiableWidget widget) {
    if (BeeUtils.same(editableWidget.getColumnId(), "Vehicle")) {
      String viewName = getFormView().getViewName();
      final int dateIndex = Data.getColumnIndex(viewName, "Date");
      final int speedIndex = Data.getColumnIndex(viewName, "SpeedometerBefore");
      final int fuelIndex = Data.getColumnIndex(viewName, "FuelBefore");

      editableWidget.addCellValidationHandler(new CellValidateEvent.Handler() {
        @Override
        public Boolean validateCell(CellValidateEvent event) {
          if (event.isCellValidation() && event.isPostValidation()) {
            CellValidation cv = event.getCellValidation();
            String id = cv.getNewValue();

            if (!BeeUtils.isEmpty(id)) {
              final IsRow row = cv.getRow();

              ParameterList args = TransportHandler.createArgs(SVC_GET_BEFORE);
              args.addDataItem(COL_VEHICLE, id);

              if (!row.isNull(dateIndex)) {
                args.addDataItem(COL_DATE, row.getString(dateIndex));
              }
              BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
                @Override
                public void onResponse(ResponseObject response) {
                  Assert.notNull(response);

                  if (response.hasErrors()) {
                    Global.showError(Lists.newArrayList(response.getErrors()));

                  } else if (response.hasArrayResponse(String.class)) {
                    String[] r = Codec.beeDeserializeCollection((String) response.getResponse());
                    row.setValue(speedIndex, r[0]);
                    row.setValue(fuelIndex, r[1]);
                    getFormView().refresh(false, false);

                  } else {
                    Global.showError("Unknown response");
                  }
                }
              });
            }
          }
          return true;
        }
      });
    }
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof ChildGrid) {
      if (BeeUtils.same(name, VIEW_TRIP_CARGO)) {
        ((ChildGrid) widget).setGridInterceptor(new TripCargoGrid(getFormView()));

      } else if (BeeUtils.same(name, TBL_TRIP_ROUTES)) {
        ((ChildGrid) widget).setGridInterceptor(new TripRoutesGrid());
      }
    } else if (BeeUtils.same(name, COL_TRIP_ROUTE) && widget instanceof HasClickHandlers) {
      ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent clickEvent) {
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
                    getFormView().notifyWarning(Localized.getConstants().noData());
                    return;
                  }
                  Data.setValue(getViewName(), row, COL_TRIP_ROUTE, route);
                  getFormView().refreshBySource(COL_TRIP_ROUTE);
                }
              }
            });
          }
        }
      });
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new TripForm();
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();
    header.addCommandItem(new Profit(COL_TRIP, row.getId()));
    return true;
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    form.getViewPresenter().getHeader().clearCommandPanel();
  }
}