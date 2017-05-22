package com.butent.bee.client.view.grid.interceptor;

import com.google.gwt.xml.client.Element;

import com.butent.bee.client.data.HasActiveRow;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.event.logical.ActiveRowChangeEvent;
import com.butent.bee.client.event.logical.DataReceivedEvent;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.event.logical.RowCountChangeEvent;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.ProvidesGridColumnRenderer;
import com.butent.bee.client.style.StyleProvider;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.ui.WidgetInterceptor;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditEndEvent;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.edit.EditorBuilder;
import com.butent.bee.client.view.edit.ReadyForUpdateEvent;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.grid.DynamicColumnEnumerator;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.ModificationPreviewer;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterComponent;
import com.butent.bee.shared.data.filter.FilterDescription;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GridInterceptor extends WidgetInterceptor, ActiveRowChangeEvent.Handler,
    ParentRowEvent.Handler, EditStartEvent.Handler, EditEndEvent.Handler,
    ProvidesGridColumnRenderer, DynamicColumnEnumerator, HasViewName, EditorBuilder,
    HasActiveRow, ModificationPreviewer, DataReceivedEvent.Handler {

  enum DeleteMode {
    CANCEL, DEFAULT, SILENT, CONFIRM, SINGLE, MULTI, DENY
  }

  void afterCreate(GridView gridView);

  boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn);

  void afterCreateColumns(GridView gridView);

  void afterCreatePresenter(GridPresenter presenter);

  void afterDeleteRow(long rowId);

  void afterInsertRow(IsRow result);

  void afterRender(GridView gridView, RenderingEvent event);

  void afterUpdateCell(IsColumn column, String oldValue, String newValue, IsRow result,
      boolean rowMode);

  void afterUpdateRow(IsRow result);

  boolean beforeAction(Action action, GridPresenter presenter);

  boolean beforeAddRow(GridPresenter presenter, boolean copy);

  void beforeCreate(List<? extends IsColumn> dataColumns, GridDescription gridDescription);

  ColumnDescription beforeCreateColumn(GridView gridView, ColumnDescription columnDescription);

  void beforeCreateColumns(List<? extends IsColumn> dataColumns,
      List<ColumnDescription> columnDescriptions);

  boolean beforeCreateExtWidget(Element root);

  DeleteMode beforeDeleteRow(GridPresenter presenter, IsRow row);

  DeleteMode beforeDeleteRows(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows);

  void beforeRefresh(GridPresenter presenter);

  void beforeRender(GridView gridView, RenderingEvent event);

  boolean ensureRelId(IdCallback callback);

  /**
   * Enables conditional styles for columns.
   */
  StyleProvider getColumnStyleProvider(String columnName);

  DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode);

  List<String> getDeleteRowMessage(IsRow row);

  Pair<String, String> getDeleteRowsMessage(int selectedRows);

  AbstractFilterSupplier getFilterSupplier(String columnName, ColumnDescription columnDescription);

  ColumnFooter getFooter(String columnName, ColumnDescription columnDescription);

  GridPresenter getGridPresenter();

  GridView getGridView();

  ColumnHeader getHeader(String columnName, String caption);

  Map<String, Filter> getInitialParentFilters(Collection<UiOption> uiOptions);

  BeeRowSet getInitialRowSet(GridDescription gridDescription);

  List<FilterComponent> getInitialUserFilters(List<FilterComponent> defaultFilters);

  GridInterceptor getInstance();

  List<String> getParentLabels();

  List<FilterDescription> getPredefinedFilters(List<FilterDescription> defaultFilters);

  String getRowCaption(IsRow row);

  /**
   * Enables conditional styles for rows.
   */
  StyleProvider getRowStyleProvider();

  boolean initDescription(GridDescription gridDescription);

  boolean isRowEditable(IsRow row);

  boolean onClose(GridPresenter presenter);

  void onLoad(GridView gridView);

  void onReadyForInsert(GridView gridView, ReadyForInsertEvent event);

  void onReadyForUpdate(GridView gridView, ReadyForUpdateEvent event);

  boolean onRowCountChange(GridView gridView, RowCountChangeEvent event);

  void onSaveChanges(GridView gridView, SaveChangesEvent event);

  boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy);

  void onUnload(GridView gridView);

  void setGridPresenter(GridPresenter gridPresenter);

  boolean validateRow(IsRow row, NotificationListener notificationListener);
}
