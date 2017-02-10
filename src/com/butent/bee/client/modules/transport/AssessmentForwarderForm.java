package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.utils.BeeUtils;

public class AssessmentForwarderForm extends PrintFormInterceptor {

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, VAR_INCOME) && widget instanceof DataSelector) {
      ((DataSelector) widget).addSelectorHandler(event -> {
        if (event.isOpened()) {
          FormView form = ViewHelper.getForm(getGridView());

          if (form != null) {
            event.getSelector().setAdditionalFilter(Filter.equals(COL_CARGO,
                form.getLongValue(COL_CARGO)));
          }
        }
      });
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public FormInterceptor getPrintFormInterceptor() {
    return new AssessmentForwarderPrintForm();
  }

  @Override
  public FormInterceptor getInstance() {
    return new AssessmentForwarderForm();
  }

}
