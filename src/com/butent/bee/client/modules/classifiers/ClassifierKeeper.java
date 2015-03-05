package com.butent.bee.client.modules.classifiers;

import com.google.common.collect.Lists;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.presenter.TreePresenter;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.TreeView;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public final class ClassifierKeeper {

  private static class RowTransformHandler implements RowTransformEvent.Handler {
    @Override
    public void onRowTransform(RowTransformEvent event) {
      if (event.hasView(VIEW_COMPANIES)) {
        event.setResult(DataUtils.join(Data.getDataInfo(VIEW_COMPANIES), event.getRow(),
            Lists.newArrayList(COL_COMPANY_NAME, COL_COMPANY_CODE, COL_PHONE, COL_EMAIL_ADDRESS,
                COL_ADDRESS, ALS_CITY_NAME, ALS_COUNTRY_NAME), BeeConst.STRING_SPACE));

      } else if (event.hasView(VIEW_PERSONS)) {
        event.setResult(DataUtils.join(Data.getDataInfo(VIEW_PERSONS), event.getRow(),
            Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME, COL_PHONE, COL_EMAIL_ADDRESS,
                COL_ADDRESS, ALS_CITY_NAME, ALS_COUNTRY_NAME), BeeConst.STRING_SPACE));
      }
    }
  }

  static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.CLASSIFIERS, method);
  }

  private static class VehiclesGridHandler extends AbstractGridInterceptor
      implements SelectionHandler<IsRow> {

    private static final String FILTER_KEY = "f1";
    private IsRow selectedModel;
    private TreePresenter modelTree;

    @Override
    public void afterCreateWidget(String name, IdentifiableWidget widget,
        WidgetDescriptionCallback callback) {
      if (widget instanceof TreeView && BeeUtils.same(name, "VehicleModels")) {
        ((TreeView) widget).addSelectionHandler(this);
        modelTree = ((TreeView) widget).getTreePresenter();
      }
    }

    @Override
    public VehiclesGridHandler getInstance() {
      return new VehiclesGridHandler();
    }

    @Override
    public void onSelection(SelectionEvent<IsRow> event) {
      if (event == null) {
        return;
      }
      if (getGridPresenter() != null) {
        Long model = null;
        setSelectedModel(event.getSelectedItem());

        if (getSelectedModel() != null) {
          model = getSelectedModel().getId();
        }
        getGridPresenter().getDataProvider().setParentFilter(FILTER_KEY, getFilter(model));
        getGridPresenter().refresh(true);
      }
    }

    @Override
    public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
      IsRow model = getSelectedModel();

      if (model != null) {
        List<BeeColumn> cols = getGridPresenter().getDataColumns();
        newRow.setValue(DataUtils.getColumnIndex("Model", cols), model.getId());
        newRow.setValue(DataUtils.getColumnIndex("ParentModelName", cols),
            getModelValue(model, "ParentName"));
        newRow.setValue(DataUtils.getColumnIndex("ModelName", cols),
            getModelValue(model, "Name"));
      }
      return true;
    }

    private static Filter getFilter(Long model) {
      if (model == null) {
        return null;
      } else {
        Value value = new LongValue(model);

        return Filter.or(Filter.isEqual("ParentModel", value),
            Filter.isEqual("Model", value));
      }
    }

    private String getModelValue(IsRow model, String colName) {
      if (BeeUtils.allNotNull(model, modelTree, modelTree.getDataColumns())) {
        return model.getString(DataUtils.getColumnIndex(colName, modelTree.getDataColumns()));
      }
      return null;
    }

    private IsRow getSelectedModel() {
      return selectedModel;
    }

    private void setSelectedModel(IsRow selectedModel) {
      this.selectedModel = selectedModel;
    }
  }

  public static void register() {
    GridFactory.registerGridSupplier(ItemsGrid.getSupplierKey(false), GRID_ITEMS,
        new ItemsGrid(false));
    GridFactory.registerGridSupplier(ItemsGrid.getSupplierKey(true), GRID_ITEMS,
        new ItemsGrid(true));

    MenuService.ITEMS.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        String key = ItemsGrid.getSupplierKey(BeeUtils.startsSame(parameters, "s"));
        ViewFactory.createAndShow(key);
      }
    });

    GridFactory.registerGridInterceptor(VIEW_VEHICLES, new VehiclesGridHandler());

    FormFactory.registerFormInterceptor("Item", new ItemForm());
    FormFactory.registerFormInterceptor(FORM_PERSON, new PersonForm());
    FormFactory.registerFormInterceptor(FORM_COMPANY, new CompanyForm());
    FormFactory.registerFormInterceptor("Holidays", new HolidaysForm());
    FormFactory.registerFormInterceptor(FORM_COMPANY_ACTION, new CompanyActionForm());

    SelectorEvent.register(new ClassifierSelector());

    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler(), false);
  }

  private ClassifierKeeper() {
  }
}
