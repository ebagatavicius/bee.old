package com.butent.bee.client.modules.orders;

import static com.butent.bee.shared.modules.orders.OrdersConstants.*;

import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.orders.OrdersConstants.OrdersStatus;

import java.util.Objects;

public class OrderItemsPicker extends ReservationItemsPicker {

  @Override
  protected void addAdditionalFilter(ParameterList params) {
  }

  @Override
  public Long getWarehouseFrom(IsRow row) {
    int warehouseIdx = Data.getColumnIndex(VIEW_ORDERS, ClassifierConstants.COL_WAREHOUSE);
    if (row == null || BeeConst.isUndef(warehouseIdx)) {
      return null;
    }

    return row.getLong(warehouseIdx);
  }

  @Override
  public boolean setIsOrder(IsRow row) {
    int statusIdx = Data.getColumnIndex(VIEW_ORDERS, COL_ORDERS_STATUS);
    if (row == null || BeeConst.isUndef(statusIdx)) {
      return false;
    }

    return Objects.equals(row.getInteger(statusIdx), OrdersStatus.APPROVED.ordinal());
  }
}
