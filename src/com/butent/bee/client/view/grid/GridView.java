package com.butent.bee.client.view.grid;

import com.google.gwt.user.client.ui.HasWidgets;

import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.ParentRowCreator;
import com.butent.bee.client.event.DndWidget;
import com.butent.bee.client.ui.HandlesHistory;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.add.HasAddEndHandlers;
import com.butent.bee.client.view.add.HasAddStartHandlers;
import com.butent.bee.client.view.add.HasReadyForInsertHandlers;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.HasEditFormHandlers;
import com.butent.bee.client.view.edit.HasReadyForUpdateHandlers;
import com.butent.bee.client.view.edit.HasSaveChangesHandlers;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;

import java.util.Collection;
import java.util.List;

/**
 * Specifies necessary methods for grid view user interface component.
 */

public interface GridView extends DataView, HasAddStartHandlers, HasAddEndHandlers,
    HasReadyForInsertHandlers, HasReadyForUpdateHandlers, HasSaveChangesHandlers,
    HasEditFormHandlers, ParentRowCreator, HandlesHistory, DndWidget, HasWidgets,
    RowInsertEvent.Handler, RowUpdateEvent.Handler, EditStartEvent.Handler {

  public enum SelectedRows {
    ALL, EDITABLE, REMOVABLE
  }

  boolean addColumn(ColumnDescription columnDescription, String dynGroup, int beforeIndex);
  
  void create(Order order);

  void ensureRelId(IdCallback callback);
  
  int estimatePageSize(int containerWidth, int containerHeight);

  void formCancel();

  void formConfirm();
  
  List<BeeColumn> getDataColumns();

  int getDataIndex(String source);

  List<String> getDynamicColumnGroups();
  
  FormView getForm(boolean edit);

  CellGrid getGrid();

  GridDescription getGridDescription();
  
  GridInterceptor getGridInterceptor();
  
  String getGridKey();

  String getGridName();

  String getRelColumn();
  
  Collection<RowInfo> getSelectedRows(SelectedRows mode);

  void initData(int rowCount, BeeRowSet rowSet);
  
  boolean isAdding();

  boolean isChild();
  
  boolean isReadOnly();

  boolean isRowEditable(IsRow row, NotificationListener notificationListener);

  boolean isRowSelected(long rowId);

  boolean likeAMotherlessChild();

  int refreshCellContent(long rowId, String columnSource);

  void setRelId(Long relId);

  boolean validateFormData(FormView form, NotificationListener notificationListener,
      boolean focusOnError);
}
