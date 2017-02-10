package com.butent.bee.client.modules.transport;

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.ClientDefaults;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.data.value.DateValue;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

class NewSimpleTransportationOrder extends AbstractFormInterceptor {

  private class CTGrid extends CargoTripsGrid {

    @Override
    public void afterRender(GridView gridView, RenderingEvent event) {
      super.afterRender(gridView, event);

      int index = gridView.getDataIndex(COL_TRIP_MANAGER);
      Long orderId = getOrderId();

      if (!gridView.isEmpty() && !BeeConst.isUndef(index) && DataUtils.isId(orderId)) {
        Set<Long> managers = new HashSet<>();

        for (IsRow row : gridView.getRowData()) {
          Long manager = row.getLong(index);

          if (DataUtils.isId(manager) && !BeeKeeper.getUser().is(manager)) {
            managers.add(manager);
          }
        }

        if (managers.size() == 1) {
          Queries.update(VIEW_ORDERS, orderId, COL_ORDER_MANAGER,
              new LongValue(BeeUtils.peek(managers)), new Queries.IntCallback() {
                @Override
                public void onSuccess(Integer result) {
                  if (BeeUtils.isPositive(result)) {
                    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_ORDERS);
                  }
                }
              });
        }
      }
    }

    @Override
    public GridInterceptor getInstance() {
      return new CTGrid();
    }

    @Override
    public void onLoad(GridView gridView) {
    }

    @Override
    protected void getTripFilter(Consumer<Filter> consumer) {
      getCTFilter(filter -> consumer.accept(Filter.and(getExclusionFilter(), filter)));
    }

    private void getCTFilter(final Consumer<Filter> consumer) {
      FormView formView = NewSimpleTransportationOrder.this.getFormView();
      if (formView == null) {
        return;
      }

      DateTime from = getOrderDate();
      DateTime until = null;

      if (from == null && until == null) {
        consumer.accept(null);
        return;
      }

      if (from != null && until != null && !BeeUtils.isLess(from, until)) {
        consumer.accept(Filter.isFalse());
        return;
      }

      final CompoundFilter filter = Filter.and();

      if (from != null) {
        filter.add(Filter.isLess(COL_TRIP_DATE, new DateTimeValue(TimeUtils.startOfNextDay(from))));
        filter.add(Filter.or(Filter.isNull(COL_TRIP_DATE_FROM),
            Filter.isMoreEqual(COL_TRIP_DATE_FROM, new DateValue(from.getDate()))));

        if (until == null) {
          filter.add(Filter.or(Filter.isNull(COL_TRIP_PLANNED_END_DATE),
              Filter.isMoreEqual(COL_TRIP_PLANNED_END_DATE, new DateValue(from.getDate()))));
        }
      }

      if (until != null) {
        if (from == null) {
          filter.add(Filter.isLess(COL_TRIP_DATE,
              new DateTimeValue(TimeUtils.startOfNextDay(until))));
        }

        filter.add(Filter.or(Filter.isNull(COL_TRIP_DATE_FROM),
            Filter.isLessEqual(COL_TRIP_DATE_FROM, new DateValue(until.getDate()))));
        filter.add(Filter.or(Filter.isNull(COL_TRIP_PLANNED_END_DATE),
            Filter.isMoreEqual(COL_TRIP_PLANNED_END_DATE, new DateValue(until.getDate()))));
      }
      consumer.accept(filter);
    }
  }

  NewSimpleTransportationOrder() {
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (COL_ORDER_DATE.equals(name) && widget instanceof InputDateTime) {
      ((InputDateTime) widget).setDateTime(TimeUtils.nowMinutes());

    } else if (AdministrationConstants.COL_CURRENCY.equals(name)
        && widget instanceof UnboundSelector && DataUtils.isId(ClientDefaults.getCurrency())) {

      ((UnboundSelector) widget).setValue(ClientDefaults.getCurrency(), false);

    } else if (VIEW_CARGO_TRIPS.equals(name) && widget instanceof ChildGrid) {
      ((ChildGrid) widget).setGridInterceptor(new CTGrid());
    }
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    Double amount = getNumber(COL_AMOUNT);
    Long cargoService = Global.getParameterRelation(PRM_CARGO_SERVICE);

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

      Queries.insertAndFire(VIEW_CARGO_INCOMES, columns, values);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new NewSimpleTransportationOrder();
  }

  @Override
  public boolean isWidgetEditable(EditableWidget editableWidget, IsRow row) {
    if (editableWidget != null && !editableWidget.hasColumn()) {
      return !DataUtils.hasId(row);
    } else {
      return super.isWidgetEditable(editableWidget, row);
    }
  }

  @Override
  public void onReadyForInsert(final HasHandlers listener, final ReadyForInsertEvent event) {
    event.consume();

    DateTime orderDate = getOrderDate();
    String orderNumber = getOrderNumber();

    String customer = null;
    Widget customerWidget = getFormView().getWidgetByName(COL_CUSTOMER);
    if (customerWidget instanceof Editor) {
      customer = ((Editor) customerWidget).getValue();
    }

    if (!DataUtils.isId(customer)) {
      getFormView().notifySevere(Localized.dictionary()
          .fieldRequired(Data.getColumnLabel(VIEW_ORDERS, COL_CUSTOMER)));
      return;
    }

    List<BeeColumn> orderColumns = new ArrayList<>();
    List<String> orderValues = new ArrayList<>();

    if (orderDate != null) {
      orderColumns.add(Data.getColumn(VIEW_ORDERS, COL_ORDER_DATE));
      orderValues.add(orderDate.serialize());
    }
    if (!BeeUtils.isEmpty(orderNumber)) {
      orderColumns.add(Data.getColumn(VIEW_ORDERS, COL_ORDER_NO));
      orderValues.add(orderNumber);
    }

    orderColumns.add(Data.getColumn(VIEW_ORDERS, COL_CUSTOMER));
    orderValues.add(customer);

    Queries.insert(VIEW_ORDERS, orderColumns, orderValues, null, new RowCallback() {
      @Override
      public void onSuccess(BeeRow orderRow) {
        event.add(Data.getColumn(getViewName(), COL_ORDER), orderRow.getId());
        listener.fireEvent(event);
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

  private DateTime getOrderDate() {
    if (getFormView() == null) {
      return null;
    }

    Widget dateWidget = getFormView().getWidgetByName(COL_ORDER_DATE);
    if (dateWidget instanceof InputDateTime) {
      return ((InputDateTime) dateWidget).getDateTime();
    } else {
      return null;
    }
  }

  private Long getOrderId() {
    return getLongValue(COL_ORDER);
  }

  private String getOrderNumber() {
    if (getFormView() == null) {
      return null;
    }

    Widget numberWidget = getFormView().getWidgetByName(COL_ORDER_NO);
    if (numberWidget instanceof Editor) {
      return BeeUtils.trim(((Editor) numberWidget).getValue());
    } else {
      return null;
    }
  }
}
