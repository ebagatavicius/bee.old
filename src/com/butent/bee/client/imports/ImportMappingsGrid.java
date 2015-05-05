package com.butent.bee.client.imports;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

public class ImportMappingsGrid extends AbstractGridInterceptor {

  private final String viewName;

  public ImportMappingsGrid(String viewName) {
    this.viewName = viewName;
  }

  @Override
  public ColumnDescription beforeCreateColumn(GridView gridView, ColumnDescription description) {
    if (BeeUtils.same(description.getId(), AdministrationConstants.COL_IMPORT_MAPPING)) {
      Relation relation = Data.getRelation(viewName);

      if (relation == null) {
        LogUtils.getRootLogger().severe("Missing relation info:", viewName);
      } else {
        description.setRelation(relation);
      }
    }
    return super.beforeCreateColumn(gridView, description);
  }

  @Override
  public GridInterceptor getInstance() {
    return null;
  }
}
