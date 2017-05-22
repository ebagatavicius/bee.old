package com.butent.bee.client.modules.administration;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.UniqueChildInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class DepartmentForm extends AbstractFormInterceptor implements SelectorEvent.Handler {

  private String parentSelectorId;
  private String headSelectorId;

  DepartmentForm() {
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof DataSelector) {
      DataSelector selector = (DataSelector) widget;

      if (BeeUtils.same(name, COL_DEPARTMENT_PARENT)) {
        parentSelectorId = selector.getId();
        selector.addSelectorHandler(this);
      } else if (BeeUtils.same(name, COL_DEPARTMENT_HEAD)) {
        headSelectorId = selector.getId();
        selector.addSelectorHandler(this);
      }

    } else if (widget instanceof ChildGrid && BeeUtils.same(name, VIEW_DEPARTMENT_EMPLOYEES)) {
      ((ChildGrid) widget).setGridInterceptor(
          new UniqueChildInterceptor(Localized.dictionary().newDepartmentEmployees(),
              COL_DEPARTMENT, COL_COMPANY_PERSON, VIEW_COMPANY_PERSONS,
              Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME),
              Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME, ALS_COMPANY_NAME,
                  ALS_POSITION_NAME)));

    } else if (widget instanceof ChildGrid && BeeUtils.same(name, VIEW_DEPARTMENT_POSITIONS)) {
      ((ChildGrid) widget).setGridInterceptor(
          new UniqueChildInterceptor(Localized.dictionary().newDepartmentPositions(),
              COL_DEPARTMENT, COL_POSITION, VIEW_POSITIONS,
              Lists.newArrayList(COL_POSITION_NAME), Lists.newArrayList(COL_POSITION_NAME)));
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new DepartmentForm();
  }

  @Override
  public boolean isWidgetEditable(EditableWidget editableWidget, IsRow row) {
    if (BeeUtils.same(editableWidget.getWidgetId(), headSelectorId)) {
      return DataUtils.hasId(row);
    } else {
      return super.isWidgetEditable(editableWidget, row);
    }
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isOpened()) {
      DataSelector selector = event.getSelector();

      long rowId = getActiveRowId();

      if (DomUtils.idEquals(selector, parentSelectorId)) {
        if (DataUtils.isId(rowId)) {
          selector.getOracle().setExclusions(Collections.singleton(rowId));
          Queries.getRowSet(VIEW_DEPARTMENTS, Collections.singletonList(COL_DEPARTMENT_PARENT),
              Filter.compareId(Operator.NE, BeeUtils.toString(rowId)),
              new Queries.RowSetCallback() {
                @Override
                public void onSuccess(BeeRowSet result) {
                    if (result.getNumberOfRows() > 0) {
                      Map<Long, Long> parents = new HashMap<>();

                      for (BeeRow row : result) {
                        Long id = row.getId();
                        Long parent = row.getLong(0);

                        if (DataUtils.isId(parent) && !Objects.equals(id, parent)) {
                          parents.put(id, parent);
                        }
                      }

                      Set<Long> exclusions = new HashSet<>();
                      exclusions.add(rowId);

                      for (BeeRow row : result) {
                        if (parents.containsKey(row.getId())) {
                          List<Long> branch = new ArrayList<>();
                          branch.add(row.getId());

                          Long parent = parents.get(row.getId());
                          while (parent != null && !branch.contains(parent)) {
                            branch.add(parent);
                            parent = parents.get(parent);
                          }

                          if (branch.contains(rowId)) {
                            exclusions.add(row.getId());
                          }
                        }
                      }

                      selector.getOracle().setExclusions(exclusions);
                    }
                }
              });

        } else {
          selector.getOracle().clearExclusions();
        }

      } else if (DomUtils.idEquals(selector, headSelectorId)) {
        if (DataUtils.isId(rowId)) {
          selector.setAdditionalFilter(Filter.equals(COL_DEPARTMENT, rowId));
        } else {
          selector.setAdditionalFilter(Filter.isFalse());
        }
      }
    }
  }
}
