package com.butent.bee.client.modules.administration;

import com.google.gwt.user.client.ui.HasWidgets;

import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.rights.RightsHelper;
import com.butent.bee.client.rights.RightsObject;
import com.butent.bee.client.rights.RightsTable;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependencyRightsForm extends AbstractFormInterceptor {
  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "Rights-";
  private static final String STYLE_SUFFIX_CELL = "-cell";
  private static final String STYLE_OBJECT_SELECTOR = STYLE_PREFIX + "object-selector";
  private static final String STYLE_OBJECT_SELECTOR_CELL = STYLE_OBJECT_SELECTOR
      + STYLE_SUFFIX_CELL;

  private final Map<String, String> dependency = new HashMap<>();

  private RightsTable table;
  private List<RightsObject> rightsObjects;

  @Override
  public void afterCreate(FormView form) {
    HasWidgets rightsWidget = form.getRootWidget() instanceof HasWidgets
        ? (HasWidgets) form.getRootWidget()
        : null;

    if (rightsWidget == null) {
      super.afterCreate(form);
      return;
    }
    rightsWidget.clear();
    rightsObjects = RightsHelper.getRightObjects();

    if (BeeUtils.isEmpty(rightsObjects)) {
      form.notifyInfo(Localized.dictionary().noData());
      return;
    }
    dependency.clear();
    Global.getParameterMap(AdministrationConstants.PRM_RECORD_DEPENDENCY)
        .forEach((object, dependence) -> {
          if (BeeUtils.same(dependence, object)) {
            LogUtils.getRootLogger().warning(AdministrationConstants.PRM_RECORD_DEPENDENCY,
                "values {", object, dependence, "} are malicious loop and it will be removed.");
            return;
          }
          dependency.put(object, dependence);
        });

    ensureTable();
    populateTable();
    rightsWidget.add(table);

    super.afterCreate(form);
  }

  @Override
  public FormInterceptor getInstance() {
    return new DependencyRightsForm();
  }

  private static boolean hasObjectRelatedField(String objectName) {
    DataInfo view = Data.getDataInfo(objectName);
    Assert.notNull(view);

    for (BeeColumn col : view.getColumns()) {
      if (view.hasRelation(col.getId())) {
        return true;
      }
    }

    return false;
  }

  private void createSelector(int row, int col, RightsObject object) {
    Label selector = dependency.containsKey(object.getName())
        ? new Label(RightsHelper.buildDependencyName(dependency, object.getName()))
        : new FaLabel(FontAwesome.LINK);

    selector.setStyleName(STYLE_OBJECT_SELECTOR);
    selector.setTitle(dependency.containsKey(object.getName())
        ? Localized.dictionary().actionChange()
        : Localized.dictionary().actionAdd());
    selector.addClickHandler(event -> renderRelations(selector, object.getName(),
        value -> onSelectorChange(col - 1, value, object)));

    table.setWidget(row, col, selector,
        STYLE_OBJECT_SELECTOR_CELL);
    table.getCellFormatter().addStyleName(row, col, RightsTable.STYLE_MSO_CELL);
  }

  private void ensureTable() {
    if (table == null) {
      table = new RightsTable() {
        @Override
        public int getValueStartCol() {
          return 3;
        }
      };
    } else if (!table.isEmpty()) {
      table.clear();
    }
  }

  private void onModuleSelected(ModuleAndSub moduleAndSub) {
    renderSelection(RightsTable.MODULE_COL + 1,
        RightsHelper.filterByModule(rightsObjects, moduleAndSub));
  }

  private void onSelectorChange(int col, String value, RightsObject object) {
    if (BeeUtils.isEmpty(value)) {
      dependency.remove(object.getName());
    } else {
      dependency.put(object.getName(), value);
    }
    Global.setParameter(AdministrationConstants.PRM_RECORD_DEPENDENCY,
        Codec.beeSerialize(dependency), false);
    renderSelection(col, RightsHelper.filterByModule(rightsObjects, object.getModuleAndSub()));
  }

  private void populateTable() {
    List<ModuleAndSub> modules = RightsHelper.getModules(rightsObjects);

    if (modules.isEmpty()) {
      return;
    }
    int row = table.getValueStartRow();

    for (ModuleAndSub ms : modules) {
      table.addModuleWidget(row, ms);
      row++;
    }
    table.setModuleSelectionHandler(this::onModuleSelected);

    ModuleAndSub ms = modules.get(0);
    table.setSelectedModule(ms);
  }

  private void renderRelations(Label label, String objectName, Callback<String> selected) {
    DataInfo view = Data.getDataInfo(objectName);
    Assert.notNull(view);

    String colLabel = "Label";
    String colColumn = "Column";
    String caption = BeeUtils.joinWords(Localized.dictionary().actionChange(), label.getText(),
        Localized.dictionary().to().toLowerCase());
    SimpleRowSet options = new SimpleRowSet(new String[] {colLabel, colColumn});

    view.getColumns().forEach(column -> {

      if (!column.isForeign() && view.hasRelation(column.getId())
          && !BeeUtils.same(column.getId(), dependency.get(objectName))) {
        String relation = view.getRelation(column.getId());

        if (Data.getDataInfo(relation, false) == null) {
          return;
        }

        if (RightsHelper.isMaliciousDependencyLoop(dependency, objectName, relation)) {
          return;
        }
        String viewCaption = BeeUtils.parenthesize(Data.getViewCaption(relation));
        String dependence = BeeUtils.join(" → ",
            BeeUtils.joinWords(Localized.getLabel(column), viewCaption),
            RightsHelper.buildDependencyName(dependency, relation));
        options.addRow(new String[] {dependence, column.getId()});
      }
    });

    if (options.getNumberOfRows() < 1 && label instanceof FaLabel) {
      getFormView().notifyWarning(Localized.dictionary().actionCanNotBeExecuted());
      return;
    } else if (label instanceof FaLabel) {
      caption = Localized.dictionary().recordDependencyNew(Data.getViewCaption(objectName));
    } else {
      options.addRow(new String[] {Localized.dictionary().actionRemove(), BeeConst.STRING_EMPTY});
    }

    Global.choice(caption, null, Arrays.asList(options
        .getColumn(colLabel)), choseId -> {

      if (selected == null) {
        return;
      }
      selected.onSuccess(options.getValue(choseId, colColumn));
    });
  }

  private void renderSelection(int col, List<RightsObject> columnObjects) {
    int row = table.getValueStartRow();

    for (RightsObject object : columnObjects) {

      if (!hasObjectRelatedField(object.getName())) {
        continue;
      }
      table.addRightObjectWidget(row, col, object);
      createSelector(row, col + 1, object);
      row++;
    }
    table.setText(table.getValueStartRow() - 1, col + 1, Localized.dictionary().recordDependent(),
        RightsTable.STYLE_MSO_CELL);
  }
}
