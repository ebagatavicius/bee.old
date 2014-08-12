package com.butent.bee.client.rights;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.i18n.Collator;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RightsObjectType;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class DataRightsHandler extends MultiStateForm {

  private static final Comparator<RightsObject> dataComparator = new Comparator<RightsObject>() {
    @Override
    public int compare(RightsObject o1, RightsObject o2) {
      return ComparisonChain.start()
          .compare(o1.getModuleAndSub(), o2.getModuleAndSub())
          .compare(o1.getCaption(), o2.getCaption(), Collator.CASE_INSENSITIVE_NULLS_FIRST)
          .compare(o1.getName(), o2.getName())
          .result();
    }
  };

  DataRightsHandler() {
  }

  @Override
  public FormInterceptor getInstance() {
    return new DataRightsHandler();
  }

  @Override
  protected RightsObjectType getObjectType() {
    return RightsObjectType.DATA;
  }

  @Override
  protected List<RightsState> getRightsStates() {
    return Lists.newArrayList(RightsState.CREATE, RightsState.VIEW, RightsState.EDIT,
        RightsState.DELETE);
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
    List<RightsObject> result = new ArrayList<>();

    Collection<DataInfo> views = Data.getDataInfoProvider().getViews();
    for (DataInfo view : views) {
      ModuleAndSub ms = getFirstVisibleModule(view.getModule());

      if (ms == null) {
        warning("view", view.getViewName(), "module", view.getModule(), "not recognized");
      } else {
        String viewName = view.getViewName();
        String caption = BeeUtils.notEmpty(Localized.maybeTranslate(view.getCaption()), viewName);

        RightsObject viewObject = new RightsObject(viewName, caption, ms);
        result.add(viewObject);
      }
    }

    Collections.sort(result, dataComparator);
    consumer.accept(result);
  }
}
