package com.butent.bee.client.view.grid;

import com.google.gwt.user.client.ui.HasWidgets;

import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.ParentRowCreator;
import com.butent.bee.client.event.DndWidget;
import com.butent.bee.client.event.logical.DataReceivedEvent;
import com.butent.bee.client.event.logical.HasSummaryChangeHandlers;
import com.butent.bee.client.event.logical.RowCountChangeEvent;
import com.butent.bee.client.ui.HandlesHistory;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.add.HasReadyForInsertHandlers;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.HasReadyForUpdateHandlers;
import com.butent.bee.client.view.edit.HasSaveChangesHandlers;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.HasState;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.ModificationPreviewer;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Specifies necessary methods for grid view user interface component.
 */

public interface GridView extends DataView,
    HasReadyForInsertHandlers, HasReadyForUpdateHandlers, HasSaveChangesHandlers,
    HasGridFormHandlers, ParentRowCreator, HandlesHistory, DndWidget, HasWidgets,
    EditStartEvent.Handler, RowCountChangeEvent.Handler, DataReceivedEvent.Handler,
    HasSummaryChangeHandlers, HasState, ModificationPreviewer {

  enum SelectedRows {
    ALL, EDITABLE, REMOVABLE, MERGEABLE
  }

  boolean addColumn(ColumnDescription columnDescription, String dynGroup, int beforeIndex);

  void create(Order order);

  void ensureRelId(IdCallback callback);

  void ensureRow(IsRow row, boolean focus);

  int estimatePageSize(int containerWidth, int containerHeight);

  void formCancel();

  void formConfirm(Consumer<IsRow> consumer);

  FormView getActiveForm();

  List<String> getDynamicColumnGroups();

  Set<String> getEditInPlace();

  FormView getForm(GridFormKind kind);

  int getFormCount(GridFormKind kind);

  int getFormIndex(GridFormKind kind);

  List<String> getFormLabels(GridFormKind kind);

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

  boolean isEmpty();

  boolean isReadOnly();

  boolean isRowEditable(IsRow row, NotificationListener notificationListener);

  boolean isRowSelected(long rowId);

  boolean likeAMotherlessChild();

  int refreshCell(long rowId, String columnSource);

  void reset(GridDescription gridDescription);

  void selectForm(GridFormKind kind, int index);

  void setRelId(Long relId);

  boolean validateFormData(FormView form, NotificationListener notificationListener,
      boolean focusOnError);
}
