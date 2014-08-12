package com.butent.bee.client.modules.classifiers;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.presenter.GridFormPresenter;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;

import java.util.ArrayList;
import java.util.List;

class ItemForm extends AbstractFormInterceptor {

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    if (DataUtils.isNewRow(row)) {
      Widget categoryWidget = form.getWidgetByName("Categories");

      if (categoryWidget instanceof MultiSelector) {
        List<Long> categories = new ArrayList<>();
        ItemsGrid gridHandler = getItemGridHandler(form);

        if (gridHandler != null && gridHandler.getSelectedCategory() != null) {
          categories.add(gridHandler.getSelectedCategory().getId());
        }

        ((MultiSelector) categoryWidget).setIds(categories);
      }
    }

    super.afterRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new ItemForm();
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    ItemsGrid gridHandler = getItemGridHandler(form);

    if (gridHandler != null && gridHandler.showServices()) {
      newRow.setValue(form.getDataIndex(ClassifierConstants.COL_ITEM_IS_SERVICE), 1);
    }
  }

  private static ItemsGrid getItemGridHandler(FormView form) {
    if (form.getViewPresenter() instanceof GridFormPresenter) {
      GridInterceptor gic = ((GridFormPresenter) form.getViewPresenter()).getGridInterceptor();

      if (gic instanceof ItemsGrid) {
        return (ItemsGrid) gic;
      }
    }
    return null;
  }
}