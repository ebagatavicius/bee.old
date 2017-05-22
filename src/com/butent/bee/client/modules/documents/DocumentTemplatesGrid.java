package com.butent.bee.client.modules.documents;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;

import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.TreeGridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;

import java.util.List;

public class DocumentTemplatesGrid extends TreeGridInterceptor {

  @Override
  public DocumentTemplatesGrid getInstance() {
    return new DocumentTemplatesGrid();
  }

  @Override
  public List<String> getParentLabels() {
    if (getTreeView() == null || getSelectedTreeItem() == null) {
      return super.getParentLabels();
    } else {
      return getTreeView().getPathLabels(getSelectedTreeItem().getId(), ALS_CATEGORY_NAME);
    }
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
    if (getSelectedTreeItem() != null) {
      IsRow category = getSelectedTreeItem();

      if (category != null) {
        newRow.setValue(gridView.getDataIndex(COL_DOCUMENT_CATEGORY), category.getId());
        newRow.setValue(gridView.getDataIndex(ALS_CATEGORY_NAME),
            category.getString(getTreeColumnIndex(ALS_CATEGORY_NAME)));
      }
    }
    return true;
  }

  @Override
  protected Filter getFilter(Long treeItemId) {
    if (treeItemId != null) {
      return Filter.equals(COL_DOCUMENT_CATEGORY, treeItemId);
    } else {
      return null;
    }
  }
}
