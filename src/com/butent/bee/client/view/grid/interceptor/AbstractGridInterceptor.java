package com.butent.bee.client.view.grid.interceptor;

import com.google.common.collect.Lists;
import com.google.gwt.xml.client.Element;

import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.style.StyleProvider;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.ReadyForUpdateEvent;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.grid.DynamicColumnIdentity;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterDescription;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractGridInterceptor implements GridInterceptor {

  public static final List<String> DELETE_ROW_MESSAGE =
      Lists.newArrayList(Localized.getConstants().deleteRowQuestion());

  public static Pair<String, String> deleteRowsMessage(int selectedRows) {
    String m1 = Localized.getConstants().deleteActiveRow();

    String m2 = (selectedRows == 1)
        ? Localized.getConstants().deleteSelectedRow()
        : Localized.getMessages().deleteSelectedRows(selectedRows);

    return Pair.of(m1, m2);
  }

  private GridPresenter gridPresenter;

  @Override
  public void afterAction(Action action, GridPresenter presenter) {
  }

  @Override
  public void afterCreate(GridView gridView) {
  }

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {
    return true;
  }

  @Override
  public void afterCreateColumns(GridView gridView) {
  }

  @Override
  public void afterCreateEditor(String source, Editor editor, boolean embedded) {
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {
  }

  @Override
  public void afterDeleteRow(long rowId) {
  }

  @Override
  public void afterInsertRow(IsRow result) {
  }

  @Override
  public void afterRender(GridView gridView, RenderingEvent event) {
  }

  @Override
  public void afterUpdateCell(IsColumn column, IsRow result, boolean rowMode) {
  }

  @Override
  public void afterUpdateRow(IsRow result) {
  }

  @Override
  public boolean beforeAction(Action action, GridPresenter presenter) {
    return true;
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    return true;
  }

  @Override
  public void beforeCreate(List<? extends IsColumn> dataColumns, GridDescription gridDescription) {
  }

  @Override
  public ColumnDescription beforeCreateColumn(GridView gridView,
      ColumnDescription columnDescription) {
    return columnDescription;
  }

  @Override
  public void beforeCreateColumns(List<? extends IsColumn> dataColumns,
      List<ColumnDescription> columnDescriptions) {
  }

  @Override
  public boolean beforeCreateExtWidget(Element root) {
    return true;
  }

  @Override
  public boolean beforeCreateWidget(String name, Element description) {
    return true;
  }

  @Override
  public DeleteMode beforeDeleteRow(GridPresenter presenter, IsRow row) {
    return DeleteMode.DEFAULT;
  }

  @Override
  public DeleteMode beforeDeleteRows(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows) {
    return DeleteMode.DEFAULT;
  }

  @Override
  public void beforeRefresh(GridPresenter presenter) {
  }

  @Override
  public void beforeRender(GridView gridView, RenderingEvent event) {
  }

  @Override
  public IdentifiableWidget createCustomWidget(String name, Element description) {
    return null;
  }

  @Override
  public boolean ensureRelId(IdCallback callback) {
    return false;
  }

  @Override
  public String getCaption() {
    return null;
  }

  @Override
  public StyleProvider getColumnStyleProvider(String columnName) {
    return null;
  }

  @Override
  public List<BeeColumn> getDataColumns() {
    return (getGridView() == null) ? null : getGridView().getDataColumns();
  }

  @Override
  public int getDataIndex(String source) {
    return (getGridView() == null) ? BeeConst.UNDEF : getGridView().getDataIndex(source);
  }

  @Override
  public DeleteMode getDeleteMode(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows, DeleteMode defMode) {
    return defMode;
  }

  @Override
  public List<String> getDeleteRowMessage(IsRow row) {
    return DELETE_ROW_MESSAGE;
  }

  @Override
  public Pair<String, String> getDeleteRowsMessage(int selectedRows) {
    return deleteRowsMessage(selectedRows);
  }

  @Override
  public Collection<DynamicColumnIdentity> getDynamicColumns(GridView gridView, String dynGroup) {
    return null;
  }

  @Override
  public AbstractFilterSupplier getFilterSupplier(String columnName,
      ColumnDescription columnDescription) {
    return null;
  }

  @Override
  public ColumnFooter getFooter(String columnName, ColumnDescription columnDescription) {
    return null;
  }

  @Override
  public GridPresenter getGridPresenter() {
    return gridPresenter;
  }

  @Override
  public GridView getGridView() {
    return (getGridPresenter() == null) ? null : getGridPresenter().getGridView();
  }

  @Override
  public ColumnHeader getHeader(String columnName, String caption) {
    return null;
  }

  @Override
  public Map<String, Filter> getInitialParentFilters() {
    return null;
  }

  @Override
  public BeeRowSet getInitialRowSet(GridDescription gridDescription) {
    return null;
  }

  @Override
  public List<String> getParentLabels() {
    return null;
  }

  @Override
  public List<FilterDescription> getPredefinedFilters(List<FilterDescription> defaultFilters) {
    return defaultFilters;
  }

  @Override
  public AbstractCellRenderer getRenderer(String columnName, List<? extends IsColumn> dataColumns,
      ColumnDescription columnDescription, CellSource cellSource) {
    return null;
  }

  @Override
  public String getRowCaption(IsRow row, boolean edit) {
    return null;
  }

  @Override
  public StyleProvider getRowStyleProvider() {
    return null;
  }

  @Override
  public String getViewName() {
    return (getGridPresenter() == null) ? null : getGridPresenter().getViewName();
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    return true;
  }

  @Override
  public boolean isRowEditable(IsRow row) {
    return row != null && row.isEditable();
  }

  @Override
  public boolean onClose(GridPresenter presenter) {
    return true;
  }

  @Override
  public void onEditStart(EditStartEvent event) {
  }

  @Override
  public void onLoad(GridView gridView) {
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
  }

  @Override
  public void onReadyForInsert(GridView gridView, ReadyForInsertEvent event) {
  }

  @Override
  public void onReadyForUpdate(GridView gridView, ReadyForUpdateEvent event) {
  }

  @Override
  public boolean onRowInsert(RowInsertEvent event) {
    return true;
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
  }

  @Override
  public void onSaveChanges(GridView gridView, SaveChangesEvent event) {
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow) {
    return true;
  }

  @Override
  public void onUnload(GridView gridView) {
  }

  @Override
  public void setGridPresenter(GridPresenter gridPresenter) {
    this.gridPresenter = gridPresenter;
  }
}
