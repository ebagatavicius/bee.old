package com.butent.bee.client.modules.documents;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.TreeView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

final class DocumentsGrid extends AbstractGridInterceptor implements
    SelectionHandler<IsRow> {

  private static final String FILTER_KEY = "f1";

  private static Filter getFilter(Long category) {
    if (category == null) {
      return null;
    } else {
      return Filter.equals(COL_DOCUMENT_CATEGORY, category);
    }
  }

  private TreeView treeView;
  private IsRow selectedCategory;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof TreeView) {
      setTreeView((TreeView) widget);
      getTreeView().addSelectionHandler(this);
    }
  }

  @Override
  public DocumentsGrid getInstance() {
    return new DocumentsGrid();
  }

  @Override
  public List<String> getParentLabels() {
    if (getSelectedCategory() == null || getTreeView() == null) {
      return super.getParentLabels();
    } else {
      return getTreeView().getPathLabels(getSelectedCategory().getId(), COL_CATEGORY_NAME);
    }
  }

  @Override
  public void onSelection(SelectionEvent<IsRow> event) {
    if (event != null && getGridPresenter() != null) {
      setSelectedCategory(event.getSelectedItem());
      Long category = (getSelectedCategory() == null) ? null : getSelectedCategory().getId();

      getGridPresenter().getDataProvider().setParentFilter(FILTER_KEY, getFilter(category));
      getGridPresenter().refresh(false);
    }
  }

  String getCategoryValue(IsRow category, String colName) {
    if (BeeUtils.allNotNull(category, getTreeView())) {
      return category.getString(Data.getColumnIndex(getTreeView().getViewName(), colName));
    }
    return null;
  }

  IsRow getSelectedCategory() {
    return selectedCategory;
  }

  private TreeView getTreeView() {
    return treeView;
  }

  private void setSelectedCategory(IsRow selectedCategory) {
    this.selectedCategory = selectedCategory;
  }

  private void setTreeView(TreeView treeView) {
    this.treeView = treeView;
  }
}