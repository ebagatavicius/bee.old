package com.butent.bee.client.modules.classifiers;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.gwt.event.logical.shared.SelectionEvent;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.tree.Tree;
import com.butent.bee.client.tree.TreeItem;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;

import java.util.Comparator;
import java.util.Objects;

public class VehiclesGrid extends AbstractGridInterceptor {

  Tree tree;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (widget instanceof Tree) {
      tree = (Tree) widget;
      tree.addSelectionHandler(this::handleSelection);
      initTree();
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public VehiclesGrid getInstance() {
    return new VehiclesGrid();
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
    if (Objects.nonNull(tree)) {
      TreeItem item = tree.getSelectedItem();

      if (Objects.nonNull(item) && item.getUserObject() instanceof IsRow) {
        RelationUtils.updateRow(Data.getDataInfo(getViewName()), COL_MODEL, newRow,
            Data.getDataInfo(TBL_VEHICLE_MODELS), (IsRow) item.getUserObject(), true);
      }
    }
    return true;
  }

  private void handleSelection(SelectionEvent<TreeItem> selectionEvent) {
    TreeItem item = selectionEvent.getSelectedItem();
    Filter filter;

    if (Objects.isNull(item)) {
      filter = null;
    } else if (item.getUserObject() instanceof IsRow) {
      filter = Filter.equals(COL_MODEL, ((IsRow) item.getUserObject()).getId());
    } else {
      filter = Filter.equals(COL_VEHICLE_BRAND_NAME, (String) item.getUserObject());
    }
    if (getGridPresenter().getDataProvider().setDefaultParentFilter(filter)) {
      getGridPresenter().refresh(false, true);
    }
  }

  private void initTree() {
    Queries.getRowSet(TBL_VEHICLE_MODELS, null, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        int brandName = result.getColumnIndex(COL_VEHICLE_BRAND_NAME);
        int modelName = result.getColumnIndex(COL_VEHICLE_MODEL_NAME);

        Multimap<String, IsRow> map = TreeMultimap.create(String::compareTo,
            Comparator.comparing(o -> o.getString(modelName)));

        result.forEach(row -> map.put(row.getString(brandName), row));

        tree.clear();

        map.keySet().forEach(brand -> {
          TreeItem branch = tree.addItem(brand);
          branch.setUserObject(brand);

          map.get(brand).forEach(model ->
              branch.addItem(new TreeItem("<i>" + model.getString(modelName) + "</i>", model)));
        });
      }
    });
  }
}
