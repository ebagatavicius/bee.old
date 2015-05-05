package com.butent.bee.client.modules.projects;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.TreeView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.projects.ProjectStatus;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;

class AllProjectsGrid extends AbstractGridInterceptor implements SelectionHandler<IsRow> {

  private static final String FILTER_KEY = "f1";

  private final Long userId = BeeKeeper.getUser().getUserId();
  private TreeView treeView;
  private IsRow selectedCategory;

  private static Filter getFilter(Long category) {
    if (category == null) {
      return null;
    } else {
      return Filter.equals(COL_PROJECT_CATEGORY, category);
    }
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof TreeView) {
      setTreeView((TreeView) widget);
      getTreeView().addSelectionHandler(this);
    }
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {
    Provider provider = presenter.getDataProvider();

    int idxStatus = provider.getColumnIndex(COL_PROJECT_STATUS);
    int idxOwner = provider.getColumnIndex(COL_PROJECT_OWNER);

    int statusValue = BeeUtils.unbox(activeRow.getInteger(idxStatus));
    long ownerValue = BeeUtils.unbox(activeRow.getLong(idxOwner));

    boolean active = EnumUtils.getEnumByIndex(ProjectStatus.class, statusValue)
        == ProjectStatus.ACTIVE;

    boolean owner = ownerValue == BeeUtils.unbox(userId);

    if (active) {
      presenter.getGridView().notifyWarning(
          BeeUtils.joinWords(Localized.getConstants().project(), activeRow.getId(),
              Localized.getConstants().prjStatusActive())
          );
      return GridInterceptor.DeleteMode.CANCEL;
    } else if (owner) {
      return GridInterceptor.DeleteMode.SINGLE;
    } else {
      presenter.getGridView().notifyWarning(Localized.getConstants().prjDeleteCanManager());
      return GridInterceptor.DeleteMode.CANCEL;
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new AllProjectsGrid();
  }

  @Override
  public void onSelection(SelectionEvent<IsRow> event) {
    if (event != null && getGridPresenter() != null) {
      Long category = null;
      setSelectedCategory(event.getSelectedItem());

      if (getSelectedCategory() != null) {
        category = getSelectedCategory().getId();
      }

      getGridPresenter().getDataProvider().setParentFilter(FILTER_KEY, getFilter(category));
      getGridPresenter().refresh(true);

    }
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
