package com.butent.bee.client.modules.transport.charts;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.timeboard.HasColorSource;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.transport.TransportConstants.*;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

class OrderCargo extends Filterable implements HasDateRange, HasColorSource, HasShipmentInfo,
    HasCargoType {

  private static final String customerLabel = Data.getColumnLabel(VIEW_ORDERS, COL_CUSTOMER);
  private static final String managerLabel = Data.getColumnLabel(VIEW_ORDERS, COL_ORDER_MANAGER);
  private static final String notesLabel = Data.getColumnLabel(VIEW_ORDER_CARGO, ALS_CARGO_NOTES);

  private static final String orderDateLabel = Data.getColumnLabel(VIEW_ORDERS, COL_ORDER_DATE);
  private static final String orderStatusLabel = Data.getColumnLabel(VIEW_ORDERS, COL_STATUS);

  static OrderCargo create(SimpleRow row, JustDate minLoad, JustDate maxUnload) {
    OrderCargo orderCargo = new OrderCargo(row.getLong(COL_ORDER),
        EnumUtils.getEnumByIndex(OrderStatus.class, row.getInt(COL_STATUS)),
        row.getDateTime(COL_ORDER_DATE), row.getValue(COL_ORDER_NO),
        row.getLong(COL_CUSTOMER), row.getValue(COL_CUSTOMER_NAME),
        row.getLong(COL_ORDER_MANAGER),
        row.getLong(COL_CARGO_ID), row.getLong(COL_CARGO_TYPE),
        row.getValue(COL_CARGO_DESCRIPTION), row.getValue(COL_CARGO_NOTES), minLoad, maxUnload);

    if (!TimeBoardHelper.isNormalized(orderCargo.getRange()) && orderCargo.getOrderDate() != null) {
      JustDate start = BeeUtils.nvl(orderCargo.getLoadingDate(),
          orderCargo.getUnloadingDate(), orderCargo.getOrderDate().getDate());
      JustDate end = BeeUtils.nvl(orderCargo.getUnloadingDate(), start);

      orderCargo.setRange(TimeBoardHelper.getActivity(start, end));
    }

    return orderCargo;
  }

  private final Long orderId;
  private final OrderStatus orderStatus;
  private final DateTime orderDate;

  private final String orderNo;

  private final Long customerId;
  private final String customerName;

  private final Long manager;

  private final Long cargoId;

  private final Long cargoType;
  private final String cargoDescription;

  private final String notes;

  private final JustDate loadingDate;
  private final JustDate unloadingDate;

  private final String orderName;

  private Range<JustDate> range;

  protected OrderCargo(Long orderId, OrderStatus orderStatus, DateTime orderDate, String orderNo,
      Long customerId, String customerName, Long manager,
      Long cargoId, Long cargoType, String cargoDescription, String notes,
      JustDate loadingDate, JustDate unloadingDate) {

    super();

    this.orderId = orderId;
    this.orderStatus = orderStatus;
    this.orderDate = orderDate;
    this.orderNo = orderNo;

    this.customerId = customerId;
    this.customerName = customerName;
    this.manager = manager;

    this.cargoId = cargoId;
    this.cargoType = cargoType;
    this.cargoDescription = cargoDescription;

    this.notes = notes;

    this.loadingDate = loadingDate;
    this.unloadingDate = unloadingDate;

    this.orderName = BeeUtils.joinWords(this.orderNo, Format.renderDate(this.orderDate));

    this.range = TimeBoardHelper.getActivity(loadingDate, unloadingDate);
  }

  @Override
  public Long getCargoType() {
    return cargoType;
  }

  @Override
  public Long getColorSource() {
    return orderId;
  }

  @Override
  public Long getLoadingCountry() {
    return null;
  }

  @Override
  public JustDate getLoadingDate() {
    return null;
  }

  @Override
  public String getLoadingNumber() {
    return null;
  }

  @Override
  public String getLoadingPostIndex() {
    return null;
  }

  @Override
  public Long getLoadingCity() {
    return null;
  }

  @Override
  public String getLoadingPlace() {
    return null;
  }

  @Override
  public Range<JustDate> getRange() {
    return range;
  }

  @Override
  public Long getUnloadingCountry() {
    return null;
  }

  @Override
  public JustDate getUnloadingDate() {
    return null;
  }

  @Override
  public String getUnloadingNumber() {
    return null;
  }

  @Override
  public String getUnloadingPostIndex() {
    return null;
  }

  @Override
  public Long getUnloadingCity() {
    return null;
  }

  @Override
  public String getUnloadingPlace() {
    return null;
  }

  void adjustRange(Range<JustDate> defaultRange) {
    if (defaultRange == null) {
      return;
    }
    if (loadingDate != null && unloadingDate != null) {
      return;
    }

    JustDate lower = BeeUtils.nvl(loadingDate, BeeUtils.getLowerEndpoint(defaultRange));
    JustDate upper = BeeUtils.nvl(unloadingDate, BeeUtils.getUpperEndpoint(defaultRange));

    setRange(TimeBoardHelper.getActivity(lower, upper));
  }

  void assignToTrip(Long tripId, RowCallback callback) {
    if (DataUtils.isId(tripId)) {
      String viewName = VIEW_CARGO_TRIPS;

      List<BeeColumn> columns = Data.getColumns(viewName, Lists.newArrayList(COL_CARGO, COL_TRIP));
      List<String> values = Queries.asList(getCargoId(), tripId);

      Queries.insert(viewName, columns, values, null, callback);
    }
  }

  String getCargoDescription() {
    return cargoDescription;
  }

  Long getCargoId() {
    return cargoId;
  }

  Long getCargoTripId() {
    return null;
  }

  Long getCustomerId() {
    return customerId;
  }

  String getCustomerName() {
    return customerName;
  }

  Long getManager() {
    return manager;
  }

  String getManagerName() {
    if (manager == null) {
      return null;
    } else {
      return Global.getUsers().getSignature(manager);
    }
  }

  JustDate getMaxDate() {
    return BeeUtils.max(loadingDate, unloadingDate);
  }

  JustDate getMinDate() {
    return BeeUtils.min(loadingDate, unloadingDate);
  }

  DateTime getOrderDate() {
    return orderDate;
  }

  Long getOrderId() {
    return orderId;
  }

  String getOrderName() {
    return orderName;
  }

  String getOrderNo() {
    return orderNo;
  }

  OrderStatus getOrderStatus() {
    return orderStatus;
  }

  String getOrderTitle() {
    return TimeBoardHelper.buildTitle(orderDateLabel, Format.renderDate(getOrderDate()),
        orderStatusLabel, (getOrderStatus() == null) ? null : getOrderStatus().getCaption());
  }

  String getTitle() {
    return TimeBoardHelper.buildTitle(/* cargoLabel, cargoDescription, */
        Localized.dictionary().cargoLoading(), Places.getLoadingInfo(this),
        Localized.dictionary().cargoUnloading(), Places.getUnloadingInfo(this),
        Localized.dictionary().trOrder(), orderNo,
        customerLabel, customerName,
        managerLabel, getManagerName(),
        notesLabel, notes);
  }

  void maybeUpdateManager(Long newManager, final Consumer<Boolean> callback) {
    if (DataUtils.isId(newManager) && !Objects.equals(newManager, getManager())) {
      Queries.update(VIEW_ORDERS, getOrderId(), COL_ORDER_MANAGER, new LongValue(newManager),
          new Queries.IntCallback() {
            @Override
            public void onSuccess(Integer result) {
              callback.accept(BeeUtils.isPositive(result));
            }
          });

    } else {
      callback.accept(false);
    }
  }

  private void setRange(Range<JustDate> range) {
    this.range = range;
  }
}
