package com.butent.bee.client.modules.tasks;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class TodoListInterceptor extends AbstractGridInterceptor {

  private static final String NAME_SLACK = "Slack";

  TodoListInterceptor() {
  }

  @Override
  public boolean afterCreateColumn(String columnId, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if (BeeUtils.same(columnId, NAME_SLACK) && column instanceof HasCellRenderer) {
      ((HasCellRenderer) column).setRenderer(new SlackRenderer(dataColumns));
    }

    return true;
  }

  @Override
  public GridInterceptor getInstance() {
    return new TodoListInterceptor();
  }

  @Override
  public boolean onRowInsert(RowInsertEvent event) {
    if (isRelevant(event.getRow())) {
      getGridPresenter().refresh(false);
    }
    return false;
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (isRelevant(event.getRow()) || getGridView().getGrid().containsRow(event.getRowId())) {
      getGridPresenter().refresh(false);
    }
  }

  private boolean isRelevant(BeeRow row) {
    return BeeKeeper.getUser().is(row.getLong(getDataIndex(TaskConstants.COL_EXECUTOR)));
  }
}
