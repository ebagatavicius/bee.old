package com.butent.bee.client.modules.discussions;

import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.FileLinkRenderer;
import com.butent.bee.client.utils.BrowsingContext;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class DiscussionFilesGrid extends AbstractGridInterceptor {

  DiscussionFilesGrid() {
  }

  @Override
  public GridInterceptor getInstance() {
    return new DiscussionFilesGrid();
  }

  @Override
  public AbstractCellRenderer getRenderer(String columnName, List<? extends IsColumn> dataColumns,
      ColumnDescription columnDescription, CellSource cellSource) {

    if (BeeUtils.same(columnName, AdministrationConstants.COL_FILE)) {
      return new FileLinkRenderer(
          DataUtils.getColumnIndex(AdministrationConstants.COL_FILE_HASH, dataColumns),
          DataUtils.getColumnIndex(AdministrationConstants.COL_FILE_CAPTION, dataColumns),
          DataUtils.getColumnIndex(AdministrationConstants.ALS_FILE_NAME, dataColumns));
    } else {
      return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
    }
  }

  @Override
  public void onLoad(GridView gridView) {
    List<? extends IsRow> rowData = gridView.getRowData();

    if (rowData != null && BeeUtils.isPositive(rowData.size())) {
      if (rowData.size() == 1) {
        openFileLink(gridView, rowData.get(rowData.size() - 1));
      }
    }
  }

  private static void openFileLink(GridView gridView, IsRow row) {
    String url = FileUtils.getUrl(row.getString(gridView.getDataIndex(AdministrationConstants
            .COL_FILE_HASH)),
        row.getString(gridView.getDataIndex(AdministrationConstants.COL_FILE_CAPTION)));

    BrowsingContext.open(url);
    gridView.getViewPresenter().handleAction(Action.CLOSE);
  }

}
