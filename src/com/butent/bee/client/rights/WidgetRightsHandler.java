package com.butent.bee.client.rights;

import com.google.common.collect.Lists;

import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.rights.RightsObjectType;
import com.butent.bee.shared.rights.RightsState;

import java.util.List;

final class WidgetRightsHandler extends MultiRoleForm {

  WidgetRightsHandler() {
  }

  @Override
  public FormInterceptor getInstance() {
    return new WidgetRightsHandler();
  }

  @Override
  protected RightsObjectType getObjectType() {
    return RightsObjectType.WIDGET;
  }

  @Override
  protected RightsState getRightsState() {
    return RightsState.VIEW;
  }

  @Override
  protected void initObjects(Consumer<List<RightsObject>> consumer) {
    List<RightsObject> result = Lists.newArrayList();

    for (RegulatedWidget widget : RegulatedWidget.values()) {
      ModuleAndSub ms = widget.getModuleAndSub();

      if (ms == null || ms.isEnabled()) {
        result.add(new RightsObject(widget.getName(), widget.getCaption(), ms));
      }
    }

    consumer.accept(result);
  }
}
