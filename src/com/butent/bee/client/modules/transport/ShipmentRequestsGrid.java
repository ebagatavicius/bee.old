package com.butent.bee.client.modules.transport;

import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.ui.UserInterface;

public class ShipmentRequestsGrid extends AbstractGridInterceptor {

  @Override
  public GridInterceptor getInstance() {
    return new ShipmentRequestsGrid();
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
    int idxInterface = gridView.getDataIndex(AdministrationConstants.COL_USER_INTERFACE);

    if (BeeConst.isUndef(idxInterface)) {
      return true;
    }

    if (oldRow != null) {
      oldRow.setValue(idxInterface, UserInterface.normalize(getRequestUserInterface()).ordinal());
    }

    if (newRow != null) {
      newRow.setValue(idxInterface, UserInterface.normalize(getRequestUserInterface()).ordinal());
    }

    return true;
  }

  public UserInterface getRequestUserInterface() {
    return UserInterface.SELF_SERVICE;
  }

}
