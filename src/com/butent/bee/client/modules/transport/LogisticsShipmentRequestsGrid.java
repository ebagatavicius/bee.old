package com.butent.bee.client.modules.transport;

import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.ui.UserInterface;

public class LogisticsShipmentRequestsGrid extends ShipmentRequestsGrid {

  @Override
  public GridInterceptor getInstance() {
    return new LogisticsShipmentRequestsGrid();
  }

  @Override
  public UserInterface getRequestUserInterface() {
    return UserInterface.SELF_SERVICE_LOG;
  }

}
