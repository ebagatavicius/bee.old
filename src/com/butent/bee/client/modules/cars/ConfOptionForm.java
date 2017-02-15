package com.butent.bee.client.modules.cars;

import static com.butent.bee.shared.modules.cars.CarsConstants.COL_TYPE;

import com.butent.bee.client.composite.ChildSelector;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;

public class ConfOptionForm extends PhotoHandler {
  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    if (widget instanceof ChildSelector) {
      ((ChildSelector) widget).addSelectorHandler(event -> {
        if (event.isOpened()) {
          Filter filter = null;
          Long type = getLongValue(COL_TYPE);

          if (DataUtils.isId(type)) {
            filter = Filter.equals(COL_TYPE, type);
          }
          event.getSelector().setAdditionalFilter(filter);
        }
      });
    }
    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public FormInterceptor getInstance() {
    return new ConfOptionForm();
  }
}
