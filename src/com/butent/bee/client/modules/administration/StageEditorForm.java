package com.butent.bee.client.modules.administration;

import com.google.gwt.user.client.ui.HasWidgets;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.event.logical.ActiveRowChangeEvent;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class StageEditorForm extends AbstractFormInterceptor {

  private final String dataView;
  private IsRow stage;

  private InputText confirmation;
  private MultiSelector conditions;
  private CheckBox emptyCondition;
  private final Map<String, CheckBox> actions = new LinkedHashMap<>();
  private final Map<String, CheckBox> triggers = new LinkedHashMap<>();

  public StageEditorForm(String dataView) {
    this.dataView = Assert.notEmpty(dataView);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (Objects.equals(name, TBL_STAGES) && widget instanceof GridPanel) {
      ((GridPanel) widget).setGridInterceptor(new AbstractGridInterceptor() {
        @Override
        public boolean initDescription(GridDescription gridDescription) {
          gridDescription.setFilter(Filter.equals(COL_STAGE_VIEW, dataView));
          return super.initDescription(gridDescription);
        }

        @Override
        public GridInterceptor getInstance() {
          return null;
        }

        @Override
        public void onReadyForInsert(GridView gridView, ReadyForInsertEvent event) {
          event.getColumns().add(DataUtils.getColumn(COL_STAGE_VIEW, gridView.getDataColumns()));
          event.getValues().add(dataView);
          super.onReadyForInsert(gridView, event);
        }

        @Override
        public void onActiveRowChange(ActiveRowChangeEvent event) {
          stage = event.getRowValue();
          refresh();
          super.onActiveRowChange(event);
        }
      });
    }
    if (Objects.equals(name, COL_STAGE_CONFIRM) && widget instanceof InputText) {
      confirmation = (InputText) widget;
      confirmation.addValueChangeHandler(event -> {
        if (Objects.nonNull(stage)) {
          Queries.updateCellAndFire(TBL_STAGES, stage.getId(), stage.getVersion(),
              COL_STAGE_CONFIRM,
              stage.getString(Data.getColumnIndex(TBL_STAGES, COL_STAGE_CONFIRM)),
              confirmation.getNormalizedValue());
        }
      });
    }
    if (Objects.equals(name, TBL_STAGE_CONDITIONS) && widget instanceof HasWidgets) {
      HtmlTable table = new HtmlTable();
      table.getCellFormatter().setWordWrap(0, 0, false);
      table.setHtml(0, 0, Localized.dictionary().status() + ":");

      Relation rel = Relation.create(TBL_STAGES, Collections.singletonList(COL_STAGE_NAME));
      rel.disableNewRow();
      conditions = MultiSelector.autonomous(rel, rel.getChoiceColumns());
      conditions.setAdditionalFilter(Filter.equals(COL_STAGE_VIEW, dataView));
      conditions.addSelectorHandler(event -> {
        if (conditions.isValueChanged()) {
          Queries.updateChildren(TBL_STAGES, stage.getId(),
              Collections.singleton(RowChildren.create(TBL_STAGE_CONDITIONS,
                  COL_STAGE, stage.getId(), COL_STAGE_FROM, conditions.getValue())), null);
        }
      });
      table.getColumnFormatter().setWidth(1, 100, CssUnit.PCT);
      table.setWidget(0, 1, conditions);
      conditions.setWidth("100%");

      emptyCondition = new CheckBox(Localized.dictionary().valueEmpty(""));
      emptyCondition.addValueChangeHandler(event -> {
        if (BeeUtils.unbox(event.getValue())) {
          Queries.insert(TBL_STAGE_CONDITIONS,
              Data.getColumns(TBL_STAGE_CONDITIONS, Collections.singletonList(COL_STAGE)),
              Collections.singletonList(BeeUtils.toString(stage.getId())));
        } else {
          Queries.delete(TBL_STAGE_CONDITIONS, Filter.and(Filter.equals(COL_STAGE, stage.getId()),
              Filter.isNull(COL_STAGE_FROM)), null);
        }
      });
      table.getCellFormatter().setWordWrap(0, 2, false);
      table.setWidget(0, 2, emptyCondition);
      ((HasWidgets) widget).add(table);
    }
    if (Objects.equals(name, TBL_STAGE_ACTIONS) && widget instanceof HasWidgets) {
      StageUtils.getActions(dataView).forEach((action, caption) -> {
        CheckBox check = new CheckBox(caption);

        check.addValueChangeHandler(event -> {
          if (BeeUtils.unbox(event.getValue())) {
            Queries.insert(TBL_STAGE_ACTIONS, Data.getColumns(TBL_STAGE_ACTIONS, COL_STAGE,
                COL_STAGE_ACTION), Arrays.asList(BeeUtils.toString(stage.getId()), action));
          } else {
            Queries.delete(TBL_STAGE_ACTIONS, Filter.and(Filter.equals(COL_STAGE, stage.getId()),
                Filter.equals(COL_STAGE_ACTION, action)), null);
          }
        });
        ((HasWidgets) widget).add(check);
        actions.put(action, check);
      });
      widget.asWidget().setVisible(!actions.isEmpty());
    }
    if (Objects.equals(name, TBL_STAGE_TRIGGERS) && widget instanceof HasWidgets) {
      StageUtils.getTriggers(dataView).forEach((trigger, caption) -> {
        CheckBox check = new CheckBox(caption);

        check.addValueChangeHandler(event -> {
          if (BeeUtils.unbox(event.getValue())) {
            Queries.insert(TBL_STAGE_TRIGGERS, Data.getColumns(TBL_STAGE_TRIGGERS, COL_STAGE,
                COL_STAGE_TRIGGER), Arrays.asList(BeeUtils.toString(stage.getId()), trigger));
          } else {
            Queries.delete(TBL_STAGE_TRIGGERS, Filter.and(Filter.equals(COL_STAGE, stage.getId()),
                Filter.equals(COL_STAGE_TRIGGER, trigger)), null);
          }
        });
        ((HasWidgets) widget).add(check);
        triggers.put(trigger, check);
      });
      widget.asWidget().setVisible(!actions.isEmpty());
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public FormInterceptor getInstance() {
    return new StageEditorForm(dataView);
  }

  @Override
  public String getCaption() {
    DataInfo info = Data.getDataInfo(dataView);
    return BeeUtils.joinWords(Localized.dictionary().statuses() + ":",
        Localized.maybeTranslate(info.getCaption()),
        BeeUtils.parenthesize(ModuleAndSub.parse(info.getModule()).getModule().getCaption()));
  }

  @Override
  public void onLoad(FormView form) {
    refresh();
    super.onLoad(form);
  }

  private void enableControls(boolean enable) {
    if (Objects.nonNull(confirmation)) {
      if (!enable) {
        confirmation.clearValue();
      }
      confirmation.setEnabled(enable);
    }
    if (Objects.nonNull(conditions)) {
      if (!enable) {
        conditions.clearValue();
        emptyCondition.setChecked(false);
      }
      conditions.setEnabled(enable);
      emptyCondition.setEnabled(enable);
    }
    Stream.of(actions, triggers).map(Map::values).forEach(widgets -> {
      if (!enable) {
        widgets.forEach(checkBox -> checkBox.setChecked(false));
      }
      widgets.forEach(checkBox -> checkBox.setEnabled(enable));
    });
  }

  private void refresh() {
    enableControls(false);

    if (Objects.nonNull(stage)) {
      if (Objects.nonNull(confirmation)) {
        confirmation.setValue(stage.getString(Data.getColumnIndex(TBL_STAGES, COL_STAGE_CONFIRM)));
      }
      Filter filter = Filter.equals(COL_STAGE, stage.getId());
      Map<String, Filter> map = new HashMap<>();

      if (Objects.nonNull(conditions)) {
        map.put(TBL_STAGE_CONDITIONS, filter);
      }
      if (!actions.isEmpty()) {
        map.put(TBL_STAGE_ACTIONS, filter);
      }
      if (!triggers.isEmpty()) {
        map.put(TBL_STAGE_TRIGGERS, filter);
      }
      Queries.getData(map.keySet(), map, null, new Queries.DataCallback() {
        @Override
        public void onSuccess(Collection<BeeRowSet> result) {
          result.forEach(rs -> {
            switch (rs.getViewName()) {
              case TBL_STAGE_CONDITIONS:
                int idx = rs.getColumnIndex(COL_STAGE_FROM);
                conditions.setIds(rs.getDistinctLongs(idx));
                emptyCondition.setChecked(rs.getRows().stream().anyMatch(row -> row.isEmpty(idx)));
                break;
              case TBL_STAGE_ACTIONS:
                idx = rs.getColumnIndex(COL_STAGE_ACTION);
                actions.forEach((action, check) ->
                    check.setChecked(Objects.nonNull(rs.findRow(idx, action))));
                break;
              case TBL_STAGE_TRIGGERS:
                idx = rs.getColumnIndex(COL_STAGE_TRIGGER);
                triggers.forEach((action, check) ->
                    check.setChecked(Objects.nonNull(rs.findRow(idx, action))));
                break;
            }
          });
          enableControls(true);
        }
      });
    }
  }
}
