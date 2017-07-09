package com.butent.bee.client.modules.transport.charts;

import com.butent.bee.shared.modules.transport.TransportConstants.ChartDataType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;

final class CargoMatcher {

  static CargoMatcher maybeCreate(Collection<ChartData> data) {
    if (BeeUtils.isEmpty(data)) {
      return null;
    }

    ChartData customerData = FilterHelper.getDataByType(data, ChartDataType.CUSTOMER);
    ChartData managerData = FilterHelper.getDataByType(data, ChartDataType.MANAGER);

    ChartData orderData = FilterHelper.getDataByType(data, ChartDataType.ORDER);
    ChartData statusData = FilterHelper.getDataByType(data, ChartDataType.ORDER_STATUS);

    ChartData cargoData = FilterHelper.getDataByType(data, ChartDataType.CARGO);
    ChartData cargoTypeData = FilterHelper.getDataByType(data, ChartDataType.CARGO_TYPE);

    if (BeeUtils.anyNotNull(customerData, managerData, orderData, statusData,
        cargoData, cargoTypeData)) {

      return new CargoMatcher(customerData, managerData, orderData, statusData,
          cargoData, cargoTypeData);

    } else {
      return null;
    }
  }

  private final ChartData customerData;
  private final ChartData managerData;

  private final ChartData orderData;
  private final ChartData statusData;

  private final ChartData cargoData;
  private final ChartData cargoTypeData;

  private CargoMatcher(ChartData customerData, ChartData managerData, ChartData orderData,
      ChartData statusData, ChartData cargoData, ChartData cargoTypeData) {

    this.customerData = customerData;
    this.managerData = managerData;

    this.orderData = orderData;
    this.statusData = statusData;

    this.cargoData = cargoData;
    this.cargoTypeData = cargoTypeData;
  }

  boolean matches(OrderCargo cargo) {
    if (cargo == null) {
      return false;
    }

    if (customerData != null && !customerData.contains(cargo.getCustomerId())) {
      return false;
    }
    if (managerData != null && !managerData.contains(cargo.getManager())) {
      return false;
    }
    if (orderData != null && !orderData.contains(cargo.getOrderId())) {
      return false;
    }
    if (statusData != null && !statusData.contains(cargo.getOrderStatus())) {
      return false;
    }

    if (cargoData != null && !cargoData.contains(cargo.getCargoDescription())) {
      return false;
    }
    if (cargoTypeData != null && !cargoTypeData.contains(cargo.getCargoType())) {
      return false;
    }

    return true;
  }
}
