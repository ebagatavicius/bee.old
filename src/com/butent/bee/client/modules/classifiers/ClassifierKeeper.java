package com.butent.bee.client.modules.classifiers;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.WidgetFactory;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.RowTransformEvent;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;

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
    ParameterList args = BeeKeeper.getRpc().createParameters(Module.CLASSIFIERS.getName());
    args.addQueryItem(AdministrationConstants.METHOD, method);
    return args;
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
        WidgetFactory.createAndShow(key);
      }
    });

    FormFactory.registerFormInterceptor("Item", new ItemForm());
    FormFactory.registerFormInterceptor(FORM_PERSON, new PersonForm());
    FormFactory.registerFormInterceptor(FORM_COMPANY, new CompanyForm());

    SelectorEvent.register(new ClassifierSelector());

    BeeKeeper.getBus().registerRowTransformHandler(new RowTransformHandler(), false);
  }

  private ClassifierKeeper() {
  }
}
