package com.butent.bee.client.modules.documents;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.TreeView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.ui.GridDescription;

import java.util.List;

public class DocumentTemplatesGrid extends AbstractGridInterceptor implements
    SelectionHandler<IsRow> {

  private static final String FILTER_KEY = "f1";
  private TreeView categoryTree;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof TreeView) {
      categoryTree = (TreeView) widget;
      categoryTree.addSelectionHandler(this);
    }
  }

  @Override
  public BeeRowSet getInitialRowSet(GridDescription gridDescription) {
    return Data.createRowSet(gridDescription.getViewName());
  }

  @Override
  public DocumentTemplatesGrid getInstance() {
    return new DocumentTemplatesGrid();
  }

  @Override
  public List<String> getParentLabels() {
    if (categoryTree == null || categoryTree.getSelectedItem() == null) {
      return super.getParentLabels();
    } else {
      return categoryTree.getPathLabels(categoryTree.getSelectedItem().getId(), ALS_CATEGORY_NAME);
    }
  }

  @Override
  public void onSelection(SelectionEvent<IsRow> event) {
    if (event != null) {
      Long category = (event.getSelectedItem() == null) ? null : event.getSelectedItem().getId();
      Filter flt;

      if (category != null) {
        flt = Filter.equals(COL_DOCUMENT_CATEGORY, category);
      } else {
        flt = Filter.isFalse();
      }
      getGridPresenter().getDataProvider().setParentFilter(FILTER_KEY, flt);
      getGridPresenter().refresh(true);
    }
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
    if (categoryTree != null) {
      IsRow category = categoryTree.getSelectedItem();

      if (category != null) {
        newRow.setValue(gridView.getDataIndex(COL_DOCUMENT_CATEGORY), category.getId());
        newRow.setValue(gridView.getDataIndex(ALS_CATEGORY_NAME),
            category.getString(DataUtils.getColumnIndex(ALS_CATEGORY_NAME,
                categoryTree.getTreePresenter().getDataColumns())));
      }
    }
    return true;
  }
}
