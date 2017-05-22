package com.butent.bee.client.rights;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.i18n.Collator;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RightsObjectType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

final class FieldRightsHandler extends MultiStateForm {

  private static final Comparator<RightsObject> fieldComparator = (o1, o2)
    -> ComparisonChain.start()
      .compare(o1.getModuleAndSub(), o2.getModuleAndSub(), Ordering.natural().nullsLast())
      .compare(o1.getParent(), o2.getParent(), Ordering.natural().nullsFirst())
      .compare(o1.getCaption(), o2.getCaption(), Collator.CASE_INSENSITIVE_NULLS_FIRST)
      .compare(o1.getName(), o2.getName())
      .result();

  @Override
  public FormInterceptor getInstance() {
    return new FieldRightsHandler();
  }

  @Override
  protected RightsObjectType getObjectType() {
    return RightsObjectType.FIELD;
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
    List<RightsObject> result = new ArrayList<>();

    Collection<DataInfo> views = Data.getDataInfoProvider().getViews();
    for (DataInfo view : views) {
      String module = view.getModule();
      ModuleAndSub ms = RightsHelper.getFirstVisibleModule(module);

      if (ms != null || Module.NEVER_MIND.equals(module)) {
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
    result.sort(fieldComparator);
    consumer.accept(result);
  }
}
