package com.butent.bee.client.presenter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Element;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.AsyncProvider;
import com.butent.bee.client.data.CachedProvider;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.HasActiveRow;
import com.butent.bee.client.data.HasDataProvider;
import com.butent.bee.client.data.LocalProvider;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.ModalGrid;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.modules.administration.HistoryHandler;
import com.butent.bee.client.output.Exporter;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.GridContainerImpl;
import com.butent.bee.client.view.GridContainerView;
import com.butent.bee.client.view.HasGridView;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.ReadyForUpdateEvent;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.GridFilterManager;
import com.butent.bee.client.view.grid.GridMenu;
import com.butent.bee.client.view.grid.GridSettings;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.search.FilterConsumer;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.css.values.FontSize;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.ProviderType;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterComponent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GridPresenter extends AbstractPresenter implements ReadyForInsertEvent.Handler,
    ReadyForUpdateEvent.Handler, SaveChangesEvent.Handler, HasDataProvider, HasActiveRow,
    HasGridView, HasViewName, FilterConsumer {

  private final class DeleteCallback extends ConfirmationCallback {
    private final IsRow activeRow;
    private final Collection<RowInfo> rows;

    private DeleteCallback(IsRow activeRow, Collection<RowInfo> rows) {
      this.activeRow = activeRow;
      this.rows = rows;
    }

    @Override
    public void onConfirm() {
      int count = (rows == null) ? 0 : rows.size();
      GridInterceptor interceptor = getGridInterceptor();

      if (interceptor != null) {
        GridInterceptor.DeleteMode deleteMode;
        if (count == 0) {
          deleteMode = interceptor.beforeDeleteRow(GridPresenter.this, activeRow);
        } else {
          deleteMode = interceptor.beforeDeleteRows(GridPresenter.this, activeRow, rows);
        }

        if (deleteMode == GridInterceptor.DeleteMode.CANCEL) {
          return;
        }
      }

      if (count == 0) {
        final long rowId = activeRow.getId();
        long version = activeRow.getVersion();

        if (BeeUtils.isEmpty(getViewName())) {
          RowDeleteEvent.forward(getDataProvider(), getViewName(), rowId);
          afterDelete(rowId);

        } else {
          Queries.deleteRow(getViewName(), rowId, version, new Queries.IntCallback() {
            @Override
            public void onFailure(String... reason) {
              showFailure("Error deleting row", reason);
            }

            @Override
            public void onSuccess(Integer result) {
              RowDeleteEvent.fire(BeeKeeper.getBus(), getViewName(), rowId);
              afterDelete(rowId);
            }
          });
        }

      } else {
        final long[] rowIds = new long[count];
        int i = 0;
        for (RowInfo rowInfo : rows) {
          rowIds[i] = rowInfo.getId();
          i++;
        }

        if (BeeUtils.isEmpty(getViewName())) {
          MultiDeleteEvent.forward(getDataProvider(), getViewName(), rows);
          afterMulti(rowIds);

        } else {
          Queries.deleteRows(getViewName(), rows, new Queries.IntCallback() {
            @Override
            public void onFailure(String... reason) {
              showFailure("Error deleting rows", reason);
            }

            @Override
            public void onSuccess(Integer result) {
              MultiDeleteEvent.fire(BeeKeeper.getBus(), getViewName(), rows);
              afterMulti(rowIds);
              showInfo(Localized.getMessages().deletedRows(result));
            }
          });
        }
      }
    }

    private void afterMulti(long[] rowIds) {
      for (long rowId : rowIds) {
        afterDelete(rowId);
      }
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(GridPresenter.class);

  private static GridContainerView createView(GridDescription gridDescription, GridView gridView,
      int rowCount, Filter userFilter, GridInterceptor gridInterceptor,
      Collection<UiOption> uiOptions, GridFactory.GridOptions gridOptions) {

    GridContainerView view = new GridContainerImpl(gridDescription.getName());
    view.create(gridDescription, gridView, rowCount, userFilter, gridInterceptor, uiOptions,
        gridOptions);

    return view;
  }

  private final GridContainerView gridContainer;
  private final Provider dataProvider;

  private final GridFilterManager filterManager;

  private final GridMenu menu;

  private final List<String> favorite = new ArrayList<>();

  private List<String> parentLabels;

  private Map<Long, String> roles;

  public GridPresenter(GridDescription gridDescription, GridView gridView, int rowCount,
      BeeRowSet rowSet, ProviderType providerType, CachingPolicy cachingPolicy,
      Collection<UiOption> uiOptions) {
    this(gridDescription, gridView, rowCount, rowSet, providerType, cachingPolicy, uiOptions,
        null, null, null, null, null, null, null);
  }

  public GridPresenter(GridDescription gridDescription, GridView gridView, int rowCount,
      BeeRowSet rowSet, ProviderType providerType, CachingPolicy cachingPolicy,
      Collection<UiOption> uiOptions, GridInterceptor gridInterceptor,
      Filter immutableFilter, Map<String, Filter> parentFilters,
      List<FilterComponent> userFilterValues, Filter userFilter,
      Order order, GridFactory.GridOptions gridOptions) {

    if (gridInterceptor != null) {
      gridInterceptor.setGridPresenter(this);
    }

    this.gridContainer = createView(gridDescription, gridView, rowCount, userFilter,
        gridInterceptor, uiOptions, gridOptions);

    this.dataProvider = createProvider(gridContainer, gridDescription.getViewName(),
        rowSet.getColumns(), gridDescription.getIdName(), gridDescription.getVersionName(),
        immutableFilter, parentFilters, userFilter, order, rowSet, providerType, cachingPolicy);

    if (gridContainer.hasSearch()) {
      this.filterManager = new GridFilterManager(gridContainer.getGridView(), this);
      if (userFilterValues != null && !userFilterValues.isEmpty()) {
        filterManager.setFilter(userFilterValues);
      }
    } else {
      this.filterManager = null;
    }

    this.menu = new GridMenu(gridDescription, uiOptions);

    if (!BeeUtils.isEmpty(gridDescription.getFavorite())) {
      favorite.addAll(NameUtils.toList(gridDescription.getFavorite()));
    }

    bind();
  }

  public void deleteRow(IsRow row, boolean confirm) {
    Assert.notNull(row);

    List<String> messages = (getGridInterceptor() != null)
        ? getGridInterceptor().getDeleteRowMessage(row)
        : AbstractGridInterceptor.DELETE_ROW_MESSAGE;

    GridInterceptor.DeleteMode mode = BeeUtils.isEmpty(messages)
        ? GridInterceptor.DeleteMode.SILENT : GridInterceptor.DeleteMode.DEFAULT;

    DeleteCallback deleteCallback = new DeleteCallback(row, null);

    if (mode == GridInterceptor.DeleteMode.SILENT
        || mode == GridInterceptor.DeleteMode.DEFAULT && !confirm) {
      deleteCallback.onConfirm();
    } else {
      Global.confirmDelete(getCaption(), Icon.WARNING, messages, deleteCallback);
    }
  }

  public void deleteRows(final IsRow activeRow, final Collection<RowInfo> selectedRows) {
    int size = selectedRows.size();
    List<String> options = Lists.newArrayList();

    Pair<String, String> defMsg = AbstractGridInterceptor.deleteRowsMessage(size);
    Pair<String, String> message =
        (getGridInterceptor() != null) ? getGridInterceptor().getDeleteRowsMessage(size) : defMsg;

    if (message != null) {
      options.add(BeeUtils.notEmpty(message.getA(), defMsg.getA()));
      options.add(BeeUtils.notEmpty(message.getB(), defMsg.getB()));
    }

    if (options.isEmpty()) {
      DeleteCallback deleteCallback = new DeleteCallback(activeRow, selectedRows);
      deleteCallback.onConfirm();

    } else {
      options.add(Localized.getConstants().cancel());

      Global.getMsgBoxen().display(getCaption(), Icon.ALARM,
          Lists.newArrayList(Localized.getConstants().deleteQuestion()), options, 2,
          new ChoiceCallback() {
            @Override
            public void onSuccess(int value) {
              if (value == 0) {
                deleteRow(activeRow, false);

              } else if (value == 1) {
                DeleteCallback deleteCallback = new DeleteCallback(activeRow, selectedRows);
                deleteCallback.onConfirm();
              }
            }
          }, BeeConst.UNDEF, null, StyleUtils.className(FontSize.XX_LARGE),
          StyleUtils.className(FontSize.MEDIUM), null, null);
    }
  }

  @Override
  public IsRow getActiveRow() {
    return getGridView().getActiveRow();
  }

  @Override
  public String getCaption() {
    return gridContainer.getCaption();
  }

  public List<BeeColumn> getDataColumns() {
    return getDataProvider().getColumns();
  }

  @Override
  public Provider getDataProvider() {
    return dataProvider;
  }

  public String getFilterLabel() {
    return (filterManager == null) ? null : filterManager.getFilterLabel(true);
  }

  @Override
  public GridView getGridView() {
    return gridContainer.getGridView();
  }

  @Override
  public HeaderView getHeader() {
    return gridContainer.getHeader();
  }

  @Override
  public View getMainView() {
    return gridContainer;
  }

  public List<String> getParentLabels() {
    if (getGridInterceptor() != null) {
      List<String> labels = getGridInterceptor().getParentLabels();
      if (labels != null) {
        return labels;
      }
    }

    if (parentLabels != null) {
      return parentLabels;
    }

    if (getGridView().isChild()) {
      FormView form = UiHelper.getForm(getWidget().asWidget());

      if (form != null && !BeeUtils.isEmpty(form.getViewName()) && form.getActiveRow() != null) {
        DataInfo dataInfo = Data.getDataInfo(form.getViewName());

        if (dataInfo != null) {
          String label = DataUtils.getRowCaption(dataInfo, form.getActiveRow());

          if (!BeeUtils.isEmpty(label)) {
            return Lists.newArrayList(label);
          }
        }
      }
    }

    return BeeConst.EMPTY_IMMUTABLE_STRING_LIST;
  }

  public Set<RightsState> getRightsStates() {
    return getDataProvider().getRightsStates();
  }

  public Map<Long, String> getRoles() {
    return roles;
  }

  @Override
  public String getViewName() {
    return getDataProvider().getViewName();
  }

  @Override
  public IdentifiableWidget getWidget() {
    return getMainView();
  }

  @Override
  public void handleAction(Action action) {
    Assert.notNull(action);

    if (getGridInterceptor() != null && !getGridInterceptor().beforeAction(action, this)) {
      return;
    }

    switch (action) {
      case ADD:
        if (getMainView().isEnabled()) {
          addRow(false);
        }
        break;

      case AUDIT:
        if (BeeUtils.isEmpty(getGridView().getViewName())) {
          return;
        }
        Set<Long> ids = Sets.newHashSet();

        for (RowInfo row : getGridView().getSelectedRows(SelectedRows.ALL)) {
          ids.add(row.getId());
        }
        if (ids.isEmpty() && getGridView().getActiveRow() != null) {
          ids.add(getGridView().getActiveRow().getId());
        }
        if (ids.isEmpty()) {
          if (BeeUtils.isPositive(getGridView().getGrid().getDataSize())) {
            getGridView().notifyWarning(Localized.getConstants().selectAtLeastOneRow());
          }
          return;
        }
        GridFactory.openGrid(AdministrationConstants.GRID_HISTORY,
            new HistoryHandler(getGridView().getViewName(), ids), null,
            ModalGrid.opener(500, 500));
        break;

      case BOOKMARK:
        Global.getFavorites().bookmark(getViewName(), getActiveRow(), getDataColumns(), favorite);
        break;

      case CANCEL:
      case CLOSE:
        close();
        break;

      case CONFIGURE:
        GridSettings.handle(getGridView().getGridKey(), getGridView().getGrid(),
            getHeaderElement());
        break;

      case COPY:
        if (getMainView().isEnabled() && getActiveRow() != null) {
          addRow(true);
        }
        break;

      case DELETE:
        if (getMainView().isEnabled() && getActiveRow() != null) {
          IsRow row = getActiveRow();

          if (!row.isRemovable()) {
            getGridView().notifyWarning(Localized.getConstants().rowIsNotRemovable());

          } else if (getGridView().isRowEditable(row, getGridView())) {
            Collection<RowInfo> selectedRows =
                getGridView().getSelectedRows(SelectedRows.REMOVABLE);

            GridInterceptor.DeleteMode mode = getDeleteMode(row, selectedRows);

            if (GridInterceptor.DeleteMode.SINGLE.equals(mode)) {
              deleteRow(row, true);
            } else if (GridInterceptor.DeleteMode.MULTI.equals(mode)) {
              deleteRows(row, selectedRows);
            }
          }
        }
        break;

      case EXPORT:
        if (getGridView().getGrid().getRowCount() > 0) {
          export();
        }
        break;

      case FILTER:
        if (filterManager != null) {
          filterManager.handleFilter(getDataProvider().getQueryFilter(null), getHeaderElement());
        }
        break;

      case MENU:
        menu.open(this);
        break;

      case PRINT:
        if (getGridView().getGrid().getRowCount() > 0) {
          Printer.print(gridContainer);
        }
        break;

      case REFRESH:
        refresh(true);
        break;

      case REMOVE_FILTER:
        if (filterManager != null) {
          filterManager.clearFilter();
          tryFilter(null, null, true);
        }
        break;

      case RIGHTS:
        if (!BeeUtils.isEmpty(getRoles())) {
          if (getDataProvider().getRightsStates().containsAll(GridMenu.ALL_STATES)) {
            getDataProvider().getRightsStates().clear();
          } else {
            getDataProvider().getRightsStates().addAll(GridMenu.ALL_STATES);
          }
          refresh(true);
        }
        break;

      default:
        logger.warning(NameUtils.getName(this), action, "not implemented");
    }

    if (getGridInterceptor() != null) {
      getGridInterceptor().afterAction(action, this);
    }
  }

  public void handleRights(RightsState rightsState) {
    Assert.notNull(rightsState);

    if (getGridInterceptor() != null && !getGridInterceptor().beforeAction(Action.RIGHTS, this)) {
      return;
    }

    if (!BeeUtils.isEmpty(getRoles())) {
      getDataProvider().toggleRightsState(rightsState);
      refresh(true);
    }

    if (getGridInterceptor() != null) {
      getGridInterceptor().afterAction(Action.RIGHTS, this);
    }
  }

  public boolean hasFilter() {
    return getDataProvider().hasFilter();
  }

  @Override
  public void onReadyForInsert(final ReadyForInsertEvent event) {
    Queries.insert(getViewName(), event.getColumns(), event.getValues(), event.getChildren(),
        new RowCallback() {
          @Override
          public void onFailure(String... reason) {
            if (event.getCallback() == null) {
              showFailure("Insert Row", reason);
            } else {
              event.getCallback().onFailure(reason);
            }
          }

          @Override
          public void onSuccess(BeeRow result) {
            RowInsertEvent.fire(BeeKeeper.getBus(), getViewName(), result, event.getSourceId());
            if (event.getCallback() != null) {
              event.getCallback().onSuccess(result);
            }
          }
        });
  }

  @Override
  public boolean onReadyForUpdate(final ReadyForUpdateEvent event) {
    final long rowId = event.getRowValue().getId();
    final long version = event.getRowValue().getVersion();
    final String columnId = event.getColumn().getId();
    final String newValue = event.getNewValue();

    final CellSource source = CellSource.forColumn(event.getColumn(),
        getDataProvider().getColumnIndex(columnId));

    if (BeeUtils.isEmpty(getViewName())) {
      CellUpdateEvent.forward(getDataProvider(), getViewName(), rowId, version, source, newValue);
      return true;
    }

    BeeRowSet rowSet = event.getRowSet(getViewName(), getDataColumns());
    event.getRowValue().reset();

    final boolean rowMode = event.isRowMode() || rowSet.getNumberOfColumns() > 1;

    RowCallback rowCallback = new RowCallback() {
      @Override
      public void onFailure(String... reason) {
        if (event.getCallback() != null) {
          event.getCallback().onFailure(reason);
        }
      }

      @Override
      public void onSuccess(BeeRow row) {
        if (event.getCallback() != null) {
          event.getCallback().onSuccess(row);
        }

        if (rowMode) {
          RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), row);
        } else {
          String value = row.getString(0);
          CellUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), rowId, row.getVersion(),
              source, value);
        }
      }
    };

    if (rowMode) {
      Queries.updateRow(rowSet, rowCallback);
    } else {
      Queries.updateCell(rowSet, rowCallback);
    }
    return true;
  }

  @Override
  public void onSaveChanges(final SaveChangesEvent event) {
    Queries.update(getViewName(), event.getRowId(), event.getVersion(), event.getColumns(),
        event.getOldValues(), event.getNewValues(), event.getChildren(), new RowCallback() {
          @Override
          public void onFailure(String... reason) {
            if (event.getCallback() == null) {
              showFailure("Save Changes", reason);
            } else {
              event.getCallback().onFailure(reason);
            }
          }

          @Override
          public void onSuccess(BeeRow row) {
            RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), row);
            if (event.getCallback() != null) {
              event.getCallback().onSuccess(row);
            }
          }
        });
  }

  @Override
  public void onViewUnload() {
    getMainView().setViewPresenter(null);

    getDataProvider().onUnload();

    super.onViewUnload();
  }

  public void refresh(boolean preserveActiveRow) {
    if (getGridInterceptor() != null) {
      getGridInterceptor().beforeRefresh(this);
    }

    if (getGridView().likeAMotherlessChild()) {
      if (getGridView().getGrid().getRowCount() > 0) {
        getDataProvider().clear();
      }

    } else {
      getDataProvider().refresh(preserveActiveRow);
    }
  }

  public void setParentLabels(List<String> parentLabels) {
    this.parentLabels = parentLabels;
  }

  public void setRoles(Map<Long, String> roles) {
    this.roles = roles;
  }

  @Override
  public void tryFilter(final Filter filter, final Consumer<Boolean> callback, boolean notify) {
    if (Objects.equals(getDataProvider().getUserFilter(), filter)) {
      if (callback != null) {
        callback.accept(true);
      }
      return;
    }

    getDataProvider().tryFilter(filter, new Consumer<Boolean>() {
      @Override
      public void accept(Boolean input) {
        if (BeeUtils.isTrue(input)) {
          HeaderView header = getHeader();
          if (header != null && header.hasAction(Action.REMOVE_FILTER)) {
            header.showAction(Action.REMOVE_FILTER, filter != null);
          }
        }

        if (callback != null) {
          callback.accept(input);
        }
      }
    }, notify);
  }

  public boolean validateParent() {
    FormView form = UiHelper.getForm(getWidget().asWidget());
    if (form == null) {
      return true;
    }

    if (!form.validate(form, true)) {
      return false;
    }

    if (form.getViewPresenter() instanceof HasGridView) {
      GridView rootGrid = ((HasGridView) form.getViewPresenter()).getGridView();
      if (rootGrid != null && !rootGrid.validateFormData(form, form, true)) {
        return false;
      }
    }
    return true;
  }

  private void addRow(boolean copy) {
    if (getGridView().likeAMotherlessChild() && !validateParent()) {
      return;
    }

    if (getGridInterceptor() != null && !getGridInterceptor().beforeAddRow(this, copy)) {
      return;
    }
    getGridView().startNewRow(copy);
  }

  private void afterDelete(long rowId) {
    if (getGridInterceptor() != null) {
      getGridInterceptor().afterDeleteRow(rowId);
    }
  }

  private void bind() {
    GridContainerView view = gridContainer;
    view.setViewPresenter(this);
    view.bind();

    view.getGridView().addReadyForUpdateHandler(this);
    view.getGridView().addReadyForInsertHandler(this);

    view.getGridView().addSaveChangesHandler(this);
  }

  private void close() {
    if (getGridInterceptor() != null && !getGridInterceptor().onClose(this)) {
      return;
    }
    BeeKeeper.getScreen().closeWidget(getMainView());
  }

  private Provider createProvider(GridContainerView view, String viewName,
      List<BeeColumn> columns, String idColumnName, String versionColumnName,
      Filter immutableFilter, Map<String, Filter> parentFilters, Filter userFilter, Order order,
      BeeRowSet rowSet, ProviderType providerType, CachingPolicy cachingPolicy) {

    if (providerType == null) {
      return null;
    }

    Provider provider;
    CellGrid display = view.getGridView().getGrid();
    NotificationListener notificationListener = view.getGridView();

    switch (providerType) {
      case ASYNC:
        provider = new AsyncProvider(display, this, notificationListener, viewName, columns,
            idColumnName, versionColumnName, immutableFilter, cachingPolicy, parentFilters,
            userFilter);
        break;

      case CACHED:
        provider = new CachedProvider(display, this, notificationListener, viewName, columns,
            idColumnName, versionColumnName, immutableFilter, rowSet, parentFilters, userFilter);
        break;

      case LOCAL:
        provider = new LocalProvider(display, this, notificationListener, viewName, columns,
            immutableFilter, rowSet, parentFilters, userFilter);
        break;

      default:
        Assert.untouchable();
        provider = null;
    }

    if (order != null) {
      provider.setOrder(order);
    }
    return provider;
  }

  private void export() {
    final String caption;

    if (!BeeUtils.isEmpty(getCaption())) {
      caption = getCaption();
    } else if (!BeeUtils.isEmpty(getViewName())) {
      caption = Data.getViewCaption(getViewName());
    } else {
      caption = null;
    }

    Exporter.confirm(caption, new Exporter.FileNameCallback() {
      @Override
      public void onSuccess(String value) {
        Exporter.export(GridPresenter.this, caption, value);
      }
    });
  }

  private GridInterceptor.DeleteMode getDeleteMode(IsRow row, Collection<RowInfo> selected) {
    GridInterceptor.DeleteMode mode =
        selected.isEmpty() || selected.size() == 1 && getGridView().isRowSelected(row.getId())
            ? GridInterceptor.DeleteMode.SINGLE : GridInterceptor.DeleteMode.MULTI;

    if (getGridInterceptor() == null) {
      return mode;
    } else {
      return getGridInterceptor().getDeleteMode(this, row, selected, mode);
    }
  }

  private GridInterceptor getGridInterceptor() {
    return getGridView().getGridInterceptor();
  }

  private Element getHeaderElement() {
    HeaderView header = getHeader();
    return (header == null) ? null : header.getElement();
  }

  private void showFailure(String activity, String... reasons) {
    List<String> messages = Lists.newArrayList(activity);
    if (reasons != null) {
      messages.addAll(Lists.newArrayList(reasons));
    }
    getGridView().notifySevere(ArrayUtils.toArray(messages));
  }

  private void showInfo(String... messages) {
    getGridView().notifyInfo(messages);
  }
}
