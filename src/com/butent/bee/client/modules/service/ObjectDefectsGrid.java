package com.butent.bee.client.modules.service;

import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.ui.Action;

public class ObjectDefectsGrid extends AbstractGridInterceptor {

  ObjectDefectsGrid() {
  }

  @Override
  public boolean beforeAction(Action action, GridPresenter presenter) {
    if (action == Action.ADD) {
      DefectBuilder.start(getGridView());
      return false;
    } else {
      return super.beforeAction(action, presenter);
    }
  }
  
  @Override
  public GridInterceptor getInstance() {
    return new ObjectDefectsGrid();
  }
}
