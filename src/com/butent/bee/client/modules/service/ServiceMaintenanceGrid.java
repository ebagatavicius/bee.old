package com.butent.bee.client.modules.service;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.UserInfo;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.modules.service.ServiceMaintenanceType;
import com.butent.bee.shared.ui.GridDescription;

public class ServiceMaintenanceGrid extends AbstractGridInterceptor {

  private final UserInfo currentUser;
  private final ServiceMaintenanceType type;

  protected ServiceMaintenanceGrid(ServiceMaintenanceType type) {
    this.type = type;
    this.currentUser = BeeKeeper.getUser();
  }

  @Override
  public String getCaption() {
    return type.getCaption();
  }

  @Override
  public GridInterceptor getInstance() {
    return new ServiceMaintenanceGrid(type);
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    if (currentUser != null) {
      gridDescription.setFilter(type.getFilter(new LongValue(currentUser.getUserId())));
    }
    return true;
  }
}
