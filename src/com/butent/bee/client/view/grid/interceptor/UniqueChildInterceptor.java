package com.butent.bee.client.view.grid.interceptor;

import com.google.common.collect.Lists;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.administration.AdministrationConstants.UserGroupVisibility;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UniqueChildInterceptor extends AbstractGridInterceptor {

  public static UniqueChildInterceptor forUsers(String dialogCaption, String parentColumn,
      String childColumn) {
    return new UniqueChildInterceptor(dialogCaption, parentColumn, childColumn,
        AdministrationConstants.VIEW_USERS,
        Lists.newArrayList(ClassifierConstants.COL_FIRST_NAME, ClassifierConstants.COL_LAST_NAME),
        Lists.newArrayList(ClassifierConstants.COL_FIRST_NAME, ClassifierConstants.COL_LAST_NAME,
            ClassifierConstants.ALS_COMPANY_NAME, ClassifierConstants.ALS_POSITION_NAME));
  }

  public static UniqueChildInterceptor forUserGroups(String dialogCaption, String parentColumn,
      String childColumn) {

    List<String> columns =
        Collections.singletonList(AdministrationConstants.COL_USER_GROUP_SETTINGS_NAME);

    Filter filter = Filter.or(
        BeeKeeper.getUser().getFilter(AdministrationConstants.COL_USER_GROUP_SETTINGS_OWNER),
        Filter.isEqual(AdministrationConstants.COL_USER_GROUP_SETTINGS_VISIBILITY,
            IntegerValue.of(UserGroupVisibility.PUBLIC)));

    return new UniqueChildInterceptor(dialogCaption, parentColumn, childColumn,
        AdministrationConstants.VIEW_USER_GROUP_SETTINGS, columns, columns, columns, filter);
  }

  private final String dialogCaption;

  private final String parentColumn;
  private final String childColumn;

  private final String relationViewName;

  private final List<String> renderColumns;
  private final List<String> choiceColumns;
  private final List<String> searchableColumns;

  private final Filter filter;

  public UniqueChildInterceptor(String dialogCaption, String parentColumn, String childColumn,
      String relationViewName, String column) {
    this(dialogCaption, parentColumn, childColumn, relationViewName,
        Collections.singletonList(column));
  }

  public UniqueChildInterceptor(String dialogCaption, String parentColumn, String childColumn,
      String relationViewName, List<String> columns) {
    this(dialogCaption, parentColumn, childColumn, relationViewName, columns, columns);
  }

  public UniqueChildInterceptor(String dialogCaption, String parentColumn, String childColumn,
      String relationViewName, List<String> renderColumns, List<String> choiceColumns) {
    this(dialogCaption, parentColumn, childColumn, relationViewName, renderColumns, choiceColumns,
        choiceColumns, null);
  }

  public UniqueChildInterceptor(String dialogCaption, String parentColumn, String childColumn,
      String relationViewName, List<String> renderColumns, List<String> choiceColumns,
      List<String> searchableColumns, Filter filter) {
    this.dialogCaption = dialogCaption;
    this.parentColumn = parentColumn;
    this.childColumn = childColumn;
    this.relationViewName = relationViewName;
    this.renderColumns = renderColumns;
    this.choiceColumns = choiceColumns;
    this.searchableColumns = searchableColumns;
    this.filter = filter;
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    Relation relation = Relation.create();
    relation.setViewName(relationViewName);

    if (!BeeUtils.isEmpty(choiceColumns)) {
      relation.getChoiceColumns().addAll(choiceColumns);
    }
    if (!BeeUtils.isEmpty(searchableColumns)) {
      relation.getSearchableColumns().addAll(searchableColumns);
    }

    if (filter != null) {
      relation.setFilter(filter);
    }

    relation.disableNewRow();

    final MultiSelector selector = MultiSelector.autonomous(relation, renderColumns);

    int width = presenter.getGridView().asWidget().getOffsetWidth();
    StyleUtils.setWidth(selector, BeeUtils.clamp(width - 50, 300, 600));

    List<? extends IsRow> data = presenter.getGridView().getRowData();

    if (!BeeUtils.isEmpty(data)) {
      Set<Long> children = new HashSet<>();
      int childIndex = getDataIndex(childColumn);

      for (IsRow row : data) {
        Long child = row.getLong(childIndex);
        if (child != null) {
          children.add(child);
        }
      }

      if (!children.isEmpty()) {
        selector.setAdditionalFilter(Filter.idNotIn(children));
      }
    }

    Global.inputWidget(dialogCaption, selector, new InputCallback() {
      @Override
      public void onSuccess() {
        List<Long> input = DataUtils.parseIdList(selector.getValue());
        if (!input.isEmpty()) {
          addChildren(input);
        }
      }
    }, null, presenter.getHeader().getElement());

    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return new UniqueChildInterceptor(dialogCaption, parentColumn, childColumn, relationViewName,
        renderColumns, choiceColumns, searchableColumns, filter);
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    if (gridDescription != null
        && !gridDescription.getEnabledActions().contains(Action.FILTER)
        && !gridDescription.getDisabledActions().contains(Action.FILTER)) {

      gridDescription.getDisabledActions().add(Action.FILTER);
    }

    return super.initDescription(gridDescription);
  }

  private void addChildren(final List<Long> children) {
    getGridView().ensureRelId(new IdCallback() {
      @Override
      public void onSuccess(final Long parent) {
        if (DataUtils.isId(parent)) {
          List<BeeColumn> columns = DataUtils.getColumns(getDataColumns(),
              Lists.newArrayList(parentColumn, childColumn));
          BeeRowSet rowSet = new BeeRowSet(getViewName(), columns);

          for (Long child : children) {
            rowSet.addRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION,
                Queries.asList(parent, child));
          }
          Queries.insertRows(rowSet);
        }
      }
    });
  }
}
