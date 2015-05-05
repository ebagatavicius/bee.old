package com.butent.bee.client.modules.service;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.view.HasGridView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.service.ServiceConstants;

import java.util.ArrayList;
import java.util.List;

public class ServiceDefectForm extends PrintFormInterceptor {

  ServiceDefectForm() {
  }

  @Override
  public FormInterceptor getInstance() {
    return new ServiceDefectForm();
  }

  @Override
  public FormInterceptor getPrintFormInterceptor() {
    Widget widget = getFormView().getWidgetByName(ServiceConstants.VIEW_SERVICE_DEFECT_ITEMS);

    List<? extends IsRow> items;
    if (widget instanceof HasGridView) {
      items = ((HasGridView) widget).getGridView().getRowData();
    } else {
      items = new ArrayList<>();
    }

    return new PrintServiceDefect(items);
  }
}
