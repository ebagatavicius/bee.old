package com.butent.bee.client.composite;

import com.google.common.collect.Lists;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.HasFosterParent;
import com.butent.bee.client.ui.HasRowChildren;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Map;

public final class ChildSelector extends MultiSelector implements HasFosterParent,
    ParentRowEvent.Handler, HasRowChildren {

  private static final String ATTR_CHILD_TABLE = "childTable";
  private static final String ATTR_TARGET_REL_COLUMN = "targetRelColumn";
  private static final String ATTR_SOURCE_REL_COLUMN = "sourceRelColumn";

  public static ChildSelector create(String targetView, Relation relation,
      Map<String, String> attributes) {

    if (relation == null || BeeUtils.isEmpty(attributes)) {
      return null;
    }

    String table = attributes.get(ATTR_CHILD_TABLE);
    if (BeeUtils.isEmpty(table)) {
      return null;
    }

    DataInfo childInfo = null;

    String targetColumn = attributes.get(ATTR_TARGET_REL_COLUMN);
    if (BeeUtils.isEmpty(targetColumn) && !BeeUtils.isEmpty(targetView)) {
      childInfo = Data.getDataInfo(table);
      if (childInfo == null) {
        return null;
      }

      DataInfo targetInfo = Data.getDataInfo(targetView);
      String targetTable = (targetInfo == null) ? targetView : targetInfo.getTableName();

      targetColumn = childInfo.getRelationField(targetTable);
      if (BeeUtils.isEmpty(targetColumn)) {
        return null;
      }
    }

    String sourceColumn = attributes.get(ATTR_SOURCE_REL_COLUMN);
    if (BeeUtils.isEmpty(sourceColumn) && !BeeUtils.isEmpty(relation.getViewName())) {
      if (childInfo == null) {
        childInfo = Data.getDataInfo(table);
        if (childInfo == null) {
          return null;
        }
      }

      DataInfo sourceInfo = Data.getDataInfo(relation.getViewName());
      String sourceTable =
          (sourceInfo == null) ? relation.getViewName() : sourceInfo.getTableName();

      sourceColumn = childInfo.getRelationField(sourceTable);
      if (BeeUtils.isEmpty(sourceColumn)) {
        return null;
      }
    }

    if (!BeeUtils.same(table, AdministrationConstants.TBL_RELATIONS)
        && BeeUtils.same(targetColumn, sourceColumn)) {
      return null;
    }

    CellSource cellSource;

    String rowProperty = attributes.get(UiConstants.ATTR_PROPERTY);
    if (BeeUtils.isEmpty(rowProperty)) {
      cellSource = null;
    } else {
      boolean userMode = BeeUtils.toBoolean(attributes.get(UiConstants.ATTR_USER_MODE));
      cellSource = CellSource.forProperty(rowProperty, BeeKeeper.getUser().idOrNull(userMode),
          ValueType.TEXT);
    }

    return new ChildSelector(relation, table, targetColumn, sourceColumn, cellSource);
  }

  private final String childTable;
  private final String targetRelColumn;
  private final String sourceRelColumn;

  private Long targetRowId;

  private String parentId;
  private HandlerRegistration parentRowReg;

  private ChildSelector(Relation relation, String childTable, String targetRelColumn,
      String sourceRelColumn, CellSource cellSource) {

    super(relation, true, cellSource);

    this.childTable = childTable;
    this.targetRelColumn = targetRelColumn;
    this.sourceRelColumn = sourceRelColumn;
  }

  @Override
  public Collection<RowChildren> getChildrenForInsert() {
    if (DataUtils.isId(getTargetRowId()) || BeeUtils.isEmpty(getValue())) {
      return null;
    } else {
      return getChildren();
    }
  }

  @Override
  public Collection<RowChildren> getChildrenForUpdate() {
    if (DataUtils.isId(getTargetRowId()) && isValueChanged()) {
      return getChildren();
    } else {
      return null;
    }
  }

  @Override
  public String getIdPrefix() {
    return "child-sel";
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.CHILD_SELECTOR;
  }

  @Override
  public void onParentRow(final ParentRowEvent event) {
    final Long rowId = event.getRowId();

    if (DataUtils.isId(rowId)) {
      setTargetRowId(null);

      Queries.getRelatedValues(childTable, targetRelColumn, rowId, sourceRelColumn,
          new Queries.IdListCallback() {
            @Override
            public void onSuccess(String result) {
              setTargetRowId(rowId);
              setIds(result);
            }
          });

    } else {
      setTargetRowId(rowId);

      String value;
      if (event.getRow() == null || getCellSource() == null) {
        value = BeeConst.STRING_EMPTY;
      } else {
        value = getCellSource().getString(event.getRow());
      }

      setIds(value);
    }
  }

  /**
   * hack.
   */
  @Override
  public void render(String value) {
  }

  @Override
  public void setParentId(String parentId) {
    this.parentId = parentId;
    if (isAttached()) {
      register();
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    register();
  }

  @Override
  protected void onUnload() {
    unregister();
    super.onUnload();
  }

  private Collection<RowChildren> getChildren() {
    return Lists.newArrayList(RowChildren.create(childTable, targetRelColumn, getTargetRowId(),
        sourceRelColumn, getValue()));
  }

  private HandlerRegistration getParentRowReg() {
    return parentRowReg;
  }

  private Long getTargetRowId() {
    return targetRowId;
  }

  private void register() {
    unregister();
    if (!BeeUtils.isEmpty(getParentId())) {
      setParentRowReg(BeeKeeper.getBus().registerParentRowHandler(getParentId(), this, false));
    }
  }

  private void setParentRowReg(HandlerRegistration parentRowReg) {
    this.parentRowReg = parentRowReg;
  }

  private void setTargetRowId(Long targetRowId) {
    this.targetRowId = targetRowId;
  }

  private void unregister() {
    if (getParentRowReg() != null) {
      getParentRowReg().removeHandler();
      setParentRowReg(null);
    }
  }
}
