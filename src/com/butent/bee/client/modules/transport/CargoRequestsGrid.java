package com.butent.bee.client.modules.transport;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.modules.transport.TransportConstants.CargoRequestStatus;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.EnumUtils;

class CargoRequestsGrid extends AbstractGridInterceptor {

  @Override
  public GridInterceptor getInstance() {
    return new CargoRequestsGrid();
  }

  @Override
  public boolean isRowEditable(IsRow row) {
    boolean ok = super.isRowEditable(row);

    if (ok && BeeKeeper.getScreen().getUserInterface() == UserInterface.SELF_SERVICE) {
      CargoRequestStatus status = EnumUtils.getEnumByIndex(CargoRequestStatus.class,
          row.getInteger(getDataIndex(TransportConstants.COL_CARGO_REQUEST_STATUS)));
      ok = status == CargoRequestStatus.NEW;

      if (ok) {
        ok = row.isNull(getDataIndex(TransportConstants.COL_ORDER));
      }
    }

    return ok;
  }

  CargoRequestsGrid() {
  }
}
