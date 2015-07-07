package com.butent.bee.client.modules.transport;

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

class NewSimpleTransportationOrder extends AbstractFormInterceptor {

  private Long cargoService;

  NewSimpleTransportationOrder() {
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (COL_ORDER_DATE.equals(name) && widget instanceof InputDateTime) {
      ((InputDateTime) widget).setDateTime(TimeUtils.nowMinutes());
    }
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    Double amount = getNumber(COL_AMOUNT);

    if (BeeUtils.isPositive(amount) && DataUtils.isId(cargoService) && DataUtils.hasId(result)) {
      String currency = null;
      Widget currencyWidget = getFormView().getWidgetByName(AdministrationConstants.COL_CURRENCY);
      if (currencyWidget instanceof Editor) {
        currency = ((Editor) currencyWidget).getValue();
      }

      boolean vatPlus = getBoolean(TradeConstants.COL_TRADE_VAT_PLUS);
      Double vat = getNumber(TradeConstants.COL_TRADE_VAT);
      boolean vatPercent = getBoolean(TradeConstants.COL_TRADE_VAT_PERC);

      List<BeeColumn> columns = new ArrayList<>();
      List<String> values = new ArrayList<>();

      columns.add(Data.getColumn(VIEW_CARGO_INCOMES, COL_CARGO));
      values.add(BeeUtils.toString(result.getId()));

      columns.add(Data.getColumn(VIEW_CARGO_INCOMES, COL_SERVICE));
      values.add(BeeUtils.toString(cargoService));

      columns.add(Data.getColumn(VIEW_CARGO_INCOMES, COL_AMOUNT));
      values.add(BeeUtils.toString(amount, Data.getColumnScale(VIEW_CARGO_INCOMES, COL_AMOUNT)));

      if (DataUtils.isId(currency)) {
        columns.add(Data.getColumn(VIEW_CARGO_INCOMES, AdministrationConstants.COL_CURRENCY));
        values.add(currency);
      }

      if (vatPlus) {
        columns.add(Data.getColumn(VIEW_CARGO_INCOMES, TradeConstants.COL_TRADE_VAT_PLUS));
        values.add(BooleanValue.pack(vatPlus));
      }
      if (BeeUtils.isDouble(vat)) {
        columns.add(Data.getColumn(VIEW_CARGO_INCOMES, TradeConstants.COL_TRADE_VAT));
        values.add(BeeUtils.toString(vat,
            Data.getColumnScale(VIEW_CARGO_INCOMES, TradeConstants.COL_TRADE_VAT)));
      }
      if (vatPercent) {
        columns.add(Data.getColumn(VIEW_CARGO_INCOMES, TradeConstants.COL_TRADE_VAT_PERC));
        values.add(BooleanValue.pack(vatPercent));
      }

      Queries.insert(VIEW_CARGO_INCOMES, columns, values);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new NewSimpleTransportationOrder();
  }

  @Override
  public void onReadyForInsert(final HasHandlers listener, final ReadyForInsertEvent event) {
    event.consume();

    DateTime orderDate = null;
    Widget dateWidget = getFormView().getWidgetByName(COL_ORDER_DATE);
    if (dateWidget instanceof InputDateTime) {
      orderDate = ((InputDateTime) dateWidget).getDateTime();
    }

    String customer = null;
    Widget customerWidget = getFormView().getWidgetByName(COL_CUSTOMER);
    if (customerWidget instanceof Editor) {
      customer = ((Editor) customerWidget).getValue();
    }

    if (!DataUtils.isId(customer)) {
      getFormView().notifySevere(Data.getColumnLabel(VIEW_ORDERS, COL_CUSTOMER),
          Localized.getConstants().valueRequired());
      return;
    }

    List<BeeColumn> orderColumns = new ArrayList<>();
    List<String> orderValues = new ArrayList<>();

    if (orderDate != null) {
      orderColumns.add(Data.getColumn(VIEW_ORDERS, COL_ORDER_DATE));
      orderValues.add(orderDate.serialize());
    }

    orderColumns.add(Data.getColumn(VIEW_ORDERS, COL_CUSTOMER));
    orderValues.add(customer);

    Queries.insert(VIEW_ORDERS, orderColumns, orderValues, null, new RowCallback() {
      @Override
      public void onSuccess(BeeRow orderRow) {
        event.add(Data.getColumn(getViewName(), COL_ORDER), orderRow.getId());

        if (BeeUtils.isPositive(getNumber(COL_AMOUNT))) {
          Global.getParameter(PRM_CARGO_SERVICE, new Consumer<String>() {
            @Override
            public void accept(String input) {
              cargoService = BeeUtils.toLongOrNull(input);
              listener.fireEvent(event);
            }
          });

        } else {
          listener.fireEvent(event);
        }
      }
    });
  }

  private boolean getBoolean(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof HasCheckedness) {
      return ((HasCheckedness) widget).isChecked();
    } else {
      return false;
    }
  }

  private Double getNumber(String name) {
    Widget widget = getFormView().getWidgetByName(name);
    if (widget instanceof InputNumber) {
      return ((InputNumber) widget).getNumber();
    } else {
      return null;
    }
  }
}
