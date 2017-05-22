package com.butent.bee.client.modules.service;

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_ITEM;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_ITEM_COST;
import static com.butent.bee.shared.modules.orders.OrdersConstants.PRP_AMOUNT_WO_VAT;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.COL_TRADE_ITEM_QUANTITY;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.service.ServiceUtils;
import com.butent.bee.shared.utils.BeeUtils;

public class MaintenancePayrollForm extends AbstractFormInterceptor
    implements SelectorEvent.Handler {

  private static final String LABEL_NAME = "Label";

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {
    if (BeeUtils.same(name, COL_REPAIRER) && widget instanceof DataSelector) {
      ((DataSelector) widget).addSelectorHandler(this);
      ServiceHelper.setRepairerFilter(widget);
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public FormInterceptor getInstance() {
    return new MaintenancePayrollForm();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.hasRelatedView(ClassifierConstants.VIEW_COMPANY_PERSONS) && event.isChanged()
        && DataUtils.isId(event.getValue())) {
      ParameterList params = ServiceKeeper.createArgs(SVC_GET_REPAIRER_TARIFF);
      params.addDataItem(COL_REPAIRER, event.getValue());

      BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
            @Override
            public void onResponse(ResponseObject response) {
              if (!response.hasErrors()) {
                getFormView().updateCell(COL_PAYROLL_TARIFF, response.getResponseAsString());
              }
            }
          });
    }
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
    event.consume();
    Queries.getRowSet(TBL_SERVICE_ITEMS, null, Filter.equals(COL_SERVICE_MAINTENANCE,
        BeeUtils.toLong(event.getValue(COL_SERVICE_MAINTENANCE))), new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet itemsRowSet) {
        if (!itemsRowSet.isEmpty()) {
          double totalBasicAmount = 0;
          int costIndex = itemsRowSet.getColumnIndex(COL_ITEM + COL_ITEM_COST);
          int quantityIndex = itemsRowSet.getColumnIndex(COL_TRADE_ITEM_QUANTITY);

          for (BeeRow itemRow : itemsRowSet) {
            totalBasicAmount += ServiceUtils.calculateBasicAmount(
                BeeUtils.unbox(itemRow.getPropertyDouble(PRP_AMOUNT_WO_VAT)),
                BeeUtils.unbox(itemRow.getDouble(costIndex)),
                BeeUtils.unbox(itemRow.getDouble(quantityIndex)));
          }
          event.getColumns().add(DataUtils.getColumn(COL_PAYROLL_BASIC_AMOUNT, getDataColumns()));
          event.getColumns().add(DataUtils.getColumn(COL_PAYROLL_SALARY, getDataColumns()));
          event.getValues().add(BeeUtils.toString(totalBasicAmount));
          event.getValues().add(BeeUtils.toString(ServiceUtils.calculateSalary(BeeUtils.toDouble(
              event.getValue(COL_PAYROLL_TARIFF)), totalBasicAmount)));
        }
        listener.fireEvent(event);
      }
    });
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    GridView parentGrid = getGridView();

    if (parentGrid != null) {
      IsRow parentRow = ViewHelper.getFormRow(parentGrid.asWidget());
      if (parentRow != null) {
        form.getWidgetBySource(COL_SERVICE_MAINTENANCE).setVisible(false);
        newRow.setValue(getDataIndex(COL_SERVICE_MAINTENANCE), parentRow.getId());
        Widget maintenanceLabelWidget = form.getWidgetByName(COL_SERVICE_MAINTENANCE + LABEL_NAME);

        if (maintenanceLabelWidget instanceof Label) {
          maintenanceLabelWidget.setVisible(false);
        }
      }
    }
    super.onStartNewRow(form, oldRow, newRow);
  }
}