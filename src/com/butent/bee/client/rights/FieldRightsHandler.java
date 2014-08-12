package com.butent.bee.client.rights;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.i18n.Collator;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RightsObjectType;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class FieldRightsHandler extends MultiStateForm {

  private static final Comparator<RightsObject> fieldComparator = new Comparator<RightsObject>() {
    @Override
    public int compare(RightsObject o1, RightsObject o2) {
      return ComparisonChain.start()
          .compare(o1.getModuleAndSub(), o2.getModuleAndSub(), Ordering.natural().nullsLast())
          .compare(o1.getParent(), o2.getParent(), Ordering.natural().nullsFirst())
          .compare(o1.getCaption(), o2.getCaption(), Collator.CASE_INSENSITIVE_NULLS_FIRST)
          .compare(o1.getName(), o2.getName())
          .result();
    }
  };

  FieldRightsHandler() {
  }

  @Override
  public FormInterceptor getInstance() {
    return new FieldRightsHandler();
  }

  @Override
  protected RightsObjectType getObjectType() {
    return RightsObjectType.FIELD;
  }

  @Override
  protected List<RightsState> getRightsStates() {
    return Lists.newArrayList(RightsState.VIEW, RightsState.EDIT);
  }

  @Override
  protected int getValueStartCol() {
    return 4;
  }

  @Override
  protected boolean hasValue(RightsObject object) {
    return object.hasParent();
  }

  @Override
  protected void initObjects(Consumer<List<RightsObject>> consumer) {
    List<RightsObject> result = Lists.newArrayList();

    Collection<DataInfo> views = Data.getDataInfoProvider().getViews();
    for (DataInfo view : views) {
      ModuleAndSub ms = getFirstVisibleModule(view.getModule());

      if (ms != null) {
        String viewName = view.getViewName();
        String caption = BeeUtils.notEmpty(Localized.maybeTranslate(view.getCaption()), viewName);

        RightsObject viewObject = new RightsObject(viewName, caption, ms);
        result.add(viewObject);

        List<BeeColumn> columns = view.getColumns();
        for (BeeColumn column : columns) {
          if (!column.isForeign() || column.isEditable()) {
            result.add(new RightsObject(column.getId(), Localized.getLabel(column), viewName));
          }
        }
      }
    }

    Collections.sort(result, fieldComparator);
    consumer.accept(result);
  }
}
