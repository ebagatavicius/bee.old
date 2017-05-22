package com.butent.bee.client.modules.service;

import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.grid.interceptor.TreeGridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.modules.service.ServiceConstants;

public class ServiceObjectsGrid extends TreeGridInterceptor {

  ServiceObjectsGrid() {
  }

  @Override
  public GridInterceptor getInstance() {
    return new ServiceObjectsGrid();
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
    int categoryIdx = gridView.getDataIndex(ServiceConstants.COL_SERVICE_CATEGORY);
    int nameIdx = gridView.getDataIndex(ServiceConstants.ALS_SERVICE_CATEGORY_NAME);

    if (oldRow != null) {
      newRow.setValue(categoryIdx, oldRow.getString(categoryIdx));
      newRow.setValue(nameIdx, oldRow.getString(nameIdx));

    } else if (getTreeView() != null && getSelectedTreeItem() != null) {
      IsRow category = getSelectedTreeItem();

      if (category != null) {
        newRow.setValue(categoryIdx, category.getId());
        newRow.setValue(nameIdx,
            category.getString(getTreeColumnIndex(ServiceConstants.COL_SERVICE_CATEGORY_NAME)));
      }
    }
    return true;
  }

  @Override
  protected Filter getFilter(Long treeItemId) {
    if (treeItemId != null) {
      return Filter.equals(ServiceConstants.COL_SERVICE_CATEGORY, treeItemId);
    } else {
      return Filter.isFalse();
    }
  }
}
