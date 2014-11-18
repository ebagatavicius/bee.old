package com.butent.bee.client.modules.classifiers;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.TreeView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class ItemsGrid extends AbstractGridInterceptor implements SelectionHandler<IsRow> {

  private static final String FILTER_KEY = "f1";

  static String getSupplierKey(boolean services) {
    return BeeUtils.join(BeeConst.STRING_UNDER, GRID_ITEMS, services ? "services" : "goods");
  }

  private static Filter getFilter(Long category) {
    if (category == null) {
      return null;
    } else {
      return Filter.or(Filter.equals(COL_ITEM_TYPE, category),
          Filter.equals(COL_ITEM_GROUP, category),
          Filter.in(Data.getIdColumn(VIEW_ITEMS),
              VIEW_ITEM_CATEGORIES, COL_ITEM, Filter.equals(COL_CATEGORY, category)));
    }
  }

  private final boolean services;

  private TreeView treeView;
  private IsRow selectedCategory;

  ItemsGrid(boolean services) {
    this.services = services;
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
  public ColumnDescription beforeCreateColumn(GridView gridView,
      ColumnDescription columnDescription) {

    String id = columnDescription.getId();

    if (showServices()) {
      switch (id) {
        case COL_ITEM_WEIGHT:
        case COL_ITEM_AREA:
          return null;
      }

    } else {
      switch (id) {
        case COL_TIME_UNIT:
        case COL_ITEM_DPW:
        case COL_ITEM_MIN_TERM:
          return null;
      }
    }

    return super.beforeCreateColumn(gridView, columnDescription);
  }

  @Override
  public String getCaption() {
    if (showServices()) {
      return Localized.getConstants().services();
    } else {
      return Localized.getConstants().goods();
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new ItemsGrid(services);
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
  public boolean initDescription(GridDescription gridDescription) {
    gridDescription.setCaption(null);

    Filter filter;
    if (showServices()) {
      filter = Filter.notNull(COL_ITEM_IS_SERVICE);
    } else {
      filter = Filter.isNull(COL_ITEM_IS_SERVICE);
    }

    gridDescription.setFilter(filter);
    return true;
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

  public boolean showServices() {
    return services;
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