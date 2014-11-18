package com.butent.bee.server.data;

import com.butent.bee.server.sql.IsQuery;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class DataEvent {

  public static class TableModifyEvent extends DataEvent {
    private final IsQuery query;
    private int updateCount;

    TableModifyEvent(String targetName, IsQuery query) {
      super(targetName);
      this.query = query;
    }

    public IsQuery getQuery() {
      return query;
    }

    public int getUpdateCount() {
      return updateCount;
    }

    void setUpdateCount(int updateCount) {
      this.updateCount = updateCount;
      setAfter();
    }
  }
  public static class ViewDeleteEvent extends ViewModifyEvent {
    private final Set<Long> ids;

    ViewDeleteEvent(String viewName, Set<Long> ids) {
      super(viewName);
      Assert.notEmpty(ids);

      this.ids = ids;
    }

    public Set<Long> getIds() {
      return ids;
    }
  }
  public static class ViewInsertEvent extends ViewModifyEvent {
    private final List<BeeColumn> columns;
    private final BeeRow row;

    ViewInsertEvent(String viewName, List<BeeColumn> columns, BeeRow row) {
      super(viewName);
      Assert.notEmpty(columns);
      Assert.notNull(row);

      this.columns = columns;
      this.row = row;
    }

    public void addValue(BeeColumn column, Value value) {
      columns.add(column);
      row.addValue(value);
    }

    public List<BeeColumn> getColumns() {
      return columns;
    }

    public BeeRow getRow() {
      return row;
    }
  }

  public abstract static class ViewModifyEvent extends DataEvent {
    ViewModifyEvent(String viewName) {
      super(viewName);
    }
  }

  public static class ViewQueryEvent extends DataEvent {
    private final SqlSelect query;
    private BeeRowSet rowset;

    ViewQueryEvent(String viewName, SqlSelect query) {
      super(viewName);
      Assert.notNull(query);
      this.query = query;
    }

    public int getColumnCount() {
      return (rowset == null) ? 0 : rowset.getNumberOfColumns();
    }

    public SqlSelect getQuery() {
      return query;
    }

    public BeeRowSet getRowset() {
      return rowset;
    }

    public boolean hasData() {
      return !DataUtils.isEmpty(rowset);
    }

    void setRowset(BeeRowSet rowset) {
      Assert.notNull(rowset);
      this.rowset = rowset;
      setAfter();
    }
  }

  public static class ViewUpdateEvent extends ViewModifyEvent {
    private final List<BeeColumn> columns;
    private final BeeRow row;

    ViewUpdateEvent(String viewName, List<BeeColumn> columns, BeeRow row) {
      super(viewName);
      Assert.notEmpty(columns);
      Assert.notNull(row);

      this.columns = columns;
      this.row = row;
    }

    public List<BeeColumn> getColumns() {
      return columns;
    }

    public BeeRow getRow() {
      return row;
    }
  }

  private final String targetName;

  private List<String> errors;

  private boolean afterStage;

  private Object userObject;

  private DataEvent(String targetName) {
    Assert.notEmpty(targetName);
    this.targetName = targetName;
  }

  public void addErrorMessage(String message) {
    Assert.notEmpty(message);

    if (errors == null) {
      errors = new ArrayList<>();
    }
    errors.add(message);
  }

  public String getTargetName() {
    return targetName;
  }

  public Object getUserObject() {
    return userObject;
  }

  public boolean hasErrors() {
    return !BeeUtils.isEmpty(errors);
  }

  public boolean isAfter() {
    return afterStage;
  }

  public boolean isBefore() {
    return !isAfter();
  }

  public boolean isTarget(String target) {
    return BeeUtils.same(getTargetName(), target);
  }

  public void setUserObject(Object userObject) {
    this.userObject = userObject;
  }

  List<String> getErrorMessages() {
    return errors;
  }

  void setAfter() {
    this.afterStage = true;
  }
}
