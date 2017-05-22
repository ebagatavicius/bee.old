package com.butent.bee.client.rights;

import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.rights.RightsObjectType;

import java.util.List;
import java.util.function.Consumer;

final class DataRightsHandler extends MultiStateForm {

  @Override
  public FormInterceptor getInstance() {
    return new DataRightsHandler();
  }

  @Override
  protected RightsObjectType getObjectType() {
    return RightsObjectType.DATA;
  }

  @Override
  protected int getValueStartCol() {
    return 3;
  }

  @Override
  protected boolean hasValue(RightsObject object) {
    return true;
  }

  @Override
  protected void initObjects(Consumer<List<RightsObject>> consumer) {
    consumer.accept(RightsHelper.getRightObjects());
  }
}
