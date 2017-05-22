package com.butent.bee.client.rights;

import com.google.common.collect.ComparisonChain;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.i18n.Collator;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RightsHelper {

  private static final Comparator<RightsObject> dataComparator = (o1, o2) -> ComparisonChain.start()
      .compare(o1.getModuleAndSub(), o2.getModuleAndSub())
      .compare(o1.getCaption(), o2.getCaption(), Collator.CASE_INSENSITIVE_NULLS_FIRST)
      .compare(o1.getName(), o2.getName())
      .result();

  private RightsHelper() {

  }

  public static String buildDependencyName(Map<String, String> table, String objectName) {
    StringBuilder builder = new StringBuilder();

    if (!table.containsKey(objectName)) {
      return builder.toString();
    }

    DataInfo view = Data.getDataInfo(objectName);

    if (view == null) {
      return builder.toString();
    }

    BeeColumn column = view.getColumn(table.get(objectName));

    if (column == null || !view.hasRelation(column.getId())) {
      return builder.toString();
    }

    if (isMaliciousDependencyLoop(table, objectName, view.getRelation(column.getId()))) {
      return builder.toString();
    }

    builder.append(BeeUtils.join(" → ",
        BeeUtils.joinWords(Localized.getLabel(column),
            BeeUtils.parenthesize(Data.getViewCaption(view.getRelation(column.getId())))),
        buildDependencyName(table, view.getRelation(column.getId()))));

    return builder.toString();
  }

  public static List<RightsObject> filterByModule(List<RightsObject> objects,
      ModuleAndSub moduleAndSub) {
    List<RightsObject> result = new ArrayList<>();

    for (RightsObject object : objects) {
      if (!object.hasParent() && Objects.equals(object.getModuleAndSub(), moduleAndSub)) {
        result.add(object);
      }
    }

    return result;
  }

  public static ModuleAndSub getFirstVisibleModule(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;

    } else {
      List<ModuleAndSub> list = ModuleAndSub.parseList(input);

      for (ModuleAndSub ms : list) {
        if (BeeKeeper.getUser().isModuleVisible(ms)) {
          return ms;
        }
      }
      return null;
    }
  }

  public static List<RightsObject> getRightObjects() {
    List<RightsObject> result = new ArrayList<>();

    Collection<DataInfo> views = Data.getDataInfoProvider().getViews();
    for (DataInfo view : views) {
      ModuleAndSub ms = getFirstVisibleModule(view.getModule());

      if (ms != null) {
        String viewName = view.getViewName();
        String caption = BeeUtils.notEmpty(Localized.maybeTranslate(view.getCaption()), viewName);

        RightsObject viewObject = new RightsObject(viewName, caption, ms);
        result.add(viewObject);
      }
    }
    result.sort(dataComparator);
    return result;
  }

  public static List<ModuleAndSub> getModules(List<RightsObject> objects) {
    List<ModuleAndSub> modules = new ArrayList<>();
    boolean emptyModule = false;

    for (RightsObject object : objects) {
      if (object.getModuleAndSub() == null) {
        emptyModule = true;
      } else if (!modules.contains(object.getModuleAndSub())) {
        modules.add(object.getModuleAndSub());
      }
    }

    if (modules.size() > 1) {
      Collections.sort(modules);
    }
    if (emptyModule) {
      modules.add(null);
    }

    return modules;
  }

  /*
* Floyd's Cycle-Finding Algorithm
*/
  public static boolean isMaliciousDependencyLoop(Map<String, String> table, String view,
      String dependency) {
    Assert.notEmpty(view);
    Assert.notEmpty(dependency);

    String tortoise = view;
    String hare = view;

    while (!BeeUtils.isEmpty(hare)) {
      hare = BeeUtils.same(hare, view) ? dependency : Data.getColumnRelation(hare, table.get(hare));

      if (BeeUtils.isEmpty(hare)) {
        return false;
      }
      hare = BeeUtils.same(hare, view) ? dependency : Data.getColumnRelation(hare, table.get(hare));
      tortoise = BeeUtils.same(tortoise, view)
          ? dependency : Data.getColumnRelation(tortoise, table.get(tortoise));

      if (BeeUtils.same(hare, tortoise)) {
        return true;
      }
    }
    return false;
  }
}
