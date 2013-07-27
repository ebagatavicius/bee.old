package com.butent.bee.client.modules.ec;

import com.butent.bee.client.modules.ec.render.CategoryFullNameRenderer;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.view.grid.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.GridInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class EcDiscountHandler extends AbstractGridInterceptor {
  
  EcDiscountHandler() {
  }

  @Override
  public GridInterceptor getInstance() {
    return new EcDiscountHandler();
  }
  
  @Override
  public AbstractCellRenderer getRenderer(String columnName, List<? extends IsColumn> dataColumns,
      ColumnDescription columnDescription) {

    if (BeeUtils.same(columnName, EcConstants.COL_DISCOUNT_CATEGORY)) {
      int index = DataUtils.getColumnIndex(EcConstants.COL_DISCOUNT_CATEGORY, dataColumns);
      return new CategoryFullNameRenderer(index, columnDescription.getOptions());
    } else {
      return super.getRenderer(columnName, dataColumns, columnDescription);
    }
  }
}
