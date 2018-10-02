package com.butent.bee.client.modules.trade.acts;

import com.butent.bee.client.event.logical.SelectionCountChangeEvent;
import com.google.common.collect.Lists;
import com.google.gwt.event.shared.GwtEvent;

import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.MessageBoxes;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.ActiveRowChangeEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.search.ListFilterSupplier;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterComponent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActKind;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TradeActGrid extends AbstractGridInterceptor implements SelectionCountChangeEvent.Handler {

  private static final BeeLogger logger = LogUtils.getLogger(TradeActGrid.class);

  private static final List<TradeActKind> NEW_ROW_ACT_KINDS = Lists.newArrayList(
      TradeActKind.SALE, TradeActKind.TENDER, TradeActKind.PURCHASE, TradeActKind.WRITE_OFF,
      TradeActKind.RESERVE,
      TradeActKind.RENT_PROJECT
  );

  private final TradeActKind kind;

  private Button supplementCommand;
  private Button returnCommand;
  private Button alterCommand;
  private Button copyCommand;
  private Button templateCommand;
  private Button toRentProjectCommand;
  private Button removeFromRentCommand;

  private TradeActKind newActKind;

  TradeActGrid(TradeActKind kind) {
    this.kind = kind;
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    GridView gridView = presenter.getGridView();

    if (gridView != null && !gridView.isReadOnly()) {
      boolean canCreateActs = BeeKeeper.getUser().canCreateData(VIEW_TRADE_ACTS);

      if (canCreateActs) {
        presenter.getHeader().addCommandItem(ensureSupplementCommand());
        presenter.getHeader().addCommandItem(ensureReturnCommand());
        presenter.getHeader().addCommandItem(ensureToRentProject());
      }

      presenter.getHeader().addCommandItem(ensureAlterCommand());

      if (canCreateActs) {
        presenter.getHeader().addCommandItem(ensureCopyCommand());
      }
      if (BeeKeeper.getUser().canCreateData(VIEW_TRADE_ACT_TEMPLATES)) {
        presenter.getHeader().addCommandItem(ensureTemplateCommand());
      }

      if (BeeKeeper.getUser().canDeleteData(VIEW_TRADE_ACTS)) {
        presenter.getHeader().addCommandItem(ensureRemoveFromRent());
      }
      if (!TradeActKeeper.isClientArea()) {
        presenter.getHeader().addCommandItem(new Button(Localized.dictionary().taInvoiceCompose(),
            e -> buildInvoice()));
      }
    }

    super.afterCreatePresenter(presenter);
  }

  @Override
  public void onReadyForInsert(final GridView gridView, final ReadyForInsertEvent event) {
    IsRow row = gridView.getActiveForm().getActiveRow();
    checkContract(row, event, gridView);
  }

  @Override
  public void onSaveChanges(GridView gridView, SaveChangesEvent event) {
    if (event.isEmpty()) {
      return;
    }

    IsRow row = event.getNewRow();
    checkContract(row, event, gridView);
  }

  @Override
  public boolean beforeAction(Action action, GridPresenter presenter) {
    return (!TradeActKeeper.isClientArea() || Action.AUDIT != action) && super
        .beforeAction(action, presenter);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    newActKind = kind;
    IsRow parentRow =
        ViewHelper.getParentRow(getGridPresenter().getMainView().asWidget(), VIEW_TRADE_ACTS);
    if (kind == null) {
      List<String> options = new ArrayList<>();
      for (TradeActKind k : NEW_ROW_ACT_KINDS) {
        if (k == TradeActKind.RENT_PROJECT && k.equals(getKind(parentRow))) {
          continue;
        }
        options.add(k.getCaption());
      }

      Global.choice(Localized.dictionary().tradeActNew(), null, options, value -> {
        if (BeeUtils.isIndex(NEW_ROW_ACT_KINDS, value)) {
          newActKind = NEW_ROW_ACT_KINDS.get(value);
          getGridView().startNewRow(false);
        }
      });

      return false;
    } else if (kind == TradeActKind.SALE) {
      Global.choice(Localized.dictionary().tradeActNew(), null,
          Arrays.asList(TradeActKind.SALE.getCaption(), TradeActKind.RENT_PROJECT.getCaption()),
          value -> {
            newActKind = value == 0 ? TradeActKind.SALE : TradeActKind.RENT_PROJECT;
            getGridView().startNewRow(false);
          });
      return false;
    } else {
      return super.beforeAddRow(presenter, copy);
    }
  }

  @Override
  public DeleteMode beforeDeleteRow(GridPresenter presenter, IsRow row) {
    revertStatuses(row.getId());
    return DeleteMode.CANCEL;
  }

  @Override
  public DeleteMode beforeDeleteRows(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows) {
    List<Long> ids = new ArrayList<>(selectedRows.size());

    selectedRows.forEach(info -> ids.add(info.getId()));

    revertStatuses(ids.toArray(new Long[0]));
    return DeleteMode.CANCEL;
  }

  @Override
  public List<FilterComponent> getInitialUserFilters(List<FilterComponent> defaultFilters) {
    if (TradeActKeeper.isClientArea()) {
      return super.getInitialUserFilters(defaultFilters);
    }

    FilterComponent seriesFilter = getInitialSeriesFilter();
    FilterComponent statusFilter = getInitialStatusFilter();

    if (seriesFilter == null && statusFilter == null) {
      return super.getInitialUserFilters(defaultFilters);
    }

    List<FilterComponent> result = new ArrayList<>();
    if (!BeeUtils.isEmpty(defaultFilters)) {
      for (FilterComponent component : defaultFilters) {
        if (component != null) {
          result.add(component);

          if (BeeUtils.same(component.getName(), COL_TA_SERIES)) {
            seriesFilter = null;
          } else if (BeeUtils.same(component.getName(), COL_TA_STATUS)) {
            statusFilter = null;
          }
        }
      }
    }

    if (seriesFilter != null) {
      result.add(seriesFilter);
    }
    if (statusFilter != null) {
      result.add(statusFilter);
    }

    return result;
  }

  private void buildInvoice() {
    final Set<Long> ids = new HashSet<>();
    GridView gridView = getGridView();

    for (RowInfo row : gridView.getSelectedRows(GridView.SelectedRows.ALL)) {
      ids.add(row.getId());
    }
    if (ids.isEmpty()) {
      gridView.notifyWarning(Localized.dictionary().selectAtLeastOneRow());
      return;
    }
    Queries.getRowSet(getViewName(), null, Filter.and(
        Filter.or(Filter.idIn(ids), Filter.any(COL_TA_RENT_PROJECT, ids)),
        Filter.notEquals(COL_TA_KIND, TradeActKind.RETURN), Filter.isNull(COL_TA_CONTINUOUS),
        Filter.notNull(COL_TA_COMPANY)), result -> {
      if (DataUtils.isEmpty(result)) {
        gridView.notifyWarning(Localized.dictionary().noData());
        return;
      }
      List<Long> company = DataUtils.getDistinct(result, COL_TA_COMPANY);

      if (BeeUtils.size(company) > 1) {
        gridView.notifySevere("Pažymėti galima tik vieno kliento aktus");
        return;
      }
      gridView.getGrid().clearSelection();
      ViewHelper.refresh(gridView);

      FormFactory.openForm(FORM_INVOICE_BUILDER,
          new TradeActInvoiceBuilder(BeeUtils.peek(company), DataUtils.getRowIds(result), true));
    });
  }

  private static FilterComponent getInitialSeriesFilter() {
    BeeRowSet series = TradeActKeeper.getUserSeries(false);

    if (DataUtils.isEmpty(series)) {
      return null;

    } else {
      List<String> items = new ArrayList<>();
      for (BeeRow row : series) {
        items.add(BeeUtils.toString(row.getId()));
      }

      return new FilterComponent(COL_TA_SERIES, ListFilterSupplier.buildValue(items));
    }
  }

  private FilterComponent getInitialStatusFilter() {
    if (kind != null && !kind.enableReturn()) {
      return null;
    }

    Long returnedActStatus = TradeActKeeper.getReturnedActStatus();
    if (!DataUtils.isId(returnedActStatus)) {
      return null;
    }

    BeeRowSet statuses = kind == null ? null : TradeActKeeper.getStatuses();
    if (DataUtils.isEmpty(statuses)) {
      return null;
    }

    List<String> items = new ArrayList<>();
    for (BeeRow row : statuses) {
      if (!returnedActStatus.equals(row.getId())) {
        items.add(BeeUtils.toString(row.getId()));
      }
    }

    items.add(null);
    return new FilterComponent(COL_TA_STATUS, ListFilterSupplier.buildValue(items));
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeActGrid(kind);
  }

  @Override
  public void onActiveRowChange(ActiveRowChangeEvent event) {
    refreshCommands(event.getRowValue(), getGridView() != null
            && !getGridView().getSelectedRows(GridView.SelectedRows.ALL).isEmpty());
    super.onActiveRowChange(event);
  }

  @Override
  public void onLoad(GridView gridView) {
    super.onLoad(gridView);
    if (gridView.getGrid() != null) {
      gridView.getGrid().addSelectionCountChangeHandler(this);
    }
  }

  @Override
  public void onSelectionCountChange(SelectionCountChangeEvent event) {
    refreshCommands(getActiveRow(), event.getCount() > 0);
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
    IsRow parentRow =
        ViewHelper.getParentRow(getGridPresenter().getMainView().asWidget(), VIEW_TRADE_ACTS);
    TradeActKeeper.prepareNewTradeAct(newRow, parentRow, newActKind);
    return super.onStartNewRow(gridView, oldRow, newRow, copy);
  }

  private Button ensureCopyCommand() {
    if (copyCommand == null) {
      copyCommand = new Button(Localized.dictionary().actionCopy(),
          event -> {
            final IsRow row = getGridView().getActiveRow();

            if (getKind(row) != null) {
              List<String> messages = new StringList();

              messages.add(getActName(row));

              if (!BeeUtils.isEmpty(getNumber(row))) {
                messages.add(BeeUtils.joinWords(
                    row.getString(getDataIndex(TradeConstants.COL_SERIES_NAME)), getNumber(row)));
              }

              messages.add(Localized.dictionary().tradeActCopyQuestion());

              Global.confirm(getKind(row).getCaption(), Icon.QUESTION, messages,
                  Localized.dictionary().actionCopy(), Localized.dictionary().actionCancel(),
                  () -> {
                    if (DataUtils.sameId(row, getGridView().getActiveRow())) {
                      doCopy(row.getId());
                      Data.resetLocal(VIEW_TRADE_ACTS);
                    }
                  });
            }
          });

      TradeActKeeper.addCommandStyle(copyCommand, "copy");
      TradeActKeeper.setCommandEnabled(copyCommand, false);
    }
    return copyCommand;
  }

  private void doCopy(long id) {
    ParameterList params = TradeActKeeper.createArgs(SVC_COPY_ACT);
    params.addQueryItem(COL_TRADE_ACT, id);

    BeeKeeper.getRpc().makeGetRequest(params, response -> {
      if (Queries.checkRowResponse(SVC_COPY_ACT, getViewName(), response)) {
        BeeRow row = BeeRow.restore(response.getResponseAsString());
        BeeRow newRow = DataUtils.cloneRow(row);
        TradeActKeeper.setDefaultOperation(newRow, getKind(row));

        DataInfo tradeActsView = Data.getDataInfo(VIEW_TRADE_ACTS);

        Queries.update(VIEW_TRADE_ACTS, tradeActsView.getColumns(), row, newRow, null,
            updatedRow -> {
              RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TRADE_ACTS, updatedRow);

              GridView gridView = getGridView();

              if (gridView != null && gridView.asWidget().isAttached()) {
                gridView.ensureRow(updatedRow, true);
                maybeOpenAct(gridView, updatedRow);
              }
            });
      }
    });
  }

  private Button ensureReturnCommand() {
    if (returnCommand == null) {
      returnCommand = new Button(Localized.dictionary().taKindReturn(),
          event -> {
            final List<IsRow> rows = new ArrayList<>();

            IsRow activeRow = null;
            boolean mixedReturn = false;

            for (IsRow row : getGridView().getRowData()) {
              if (activeRow == null && getGridView().isRowSelected(row.getId())) {
                activeRow = row;

                if (getKind(activeRow) == null || !getKind(activeRow).enableReturn()) {
                  getGridView().notifyWarning(Localized.dictionary().taIsDifferent());
                  return;
                }
              } else if (activeRow == null) {
                continue;
              }

              if (getGridView().isRowSelected(row.getId()) || DataUtils.sameId(row, activeRow)) {
                if (!Objects.equals(getObject(activeRow), getObject(row))) {
                  getGridView().notifyWarning(Localized.dictionary().taObjectsIsDifferent());
                  return;
                }

                if (!Objects.equals(getSeries(activeRow), getSeries(row))
                    || !Objects.equals(getCompany(activeRow), getCompany(row))) {
                  getGridView().notifyWarning(Localized.dictionary().taIsDifferent());
                  return;
                }

                TradeActKind taKind = getKind(row);

                if (activeRow.getId() != row.getId() && !getKind(activeRow)
                    .enableMultiReturn(taKind)) {
                  getGridView().notifyWarning(Localized.dictionary().taIsDifferent());
                  return;
                }

                if (taKind != null && taKind.enableReturn()) {
                  mixedReturn |= taKind != getKind(activeRow);
                  rows.add(row);
                }
              }
            }

            if (isContinuousAct(activeRow) && !mixedReturn) {
              multiReturnContinuousAct(rows);
            } else if (rows.size() == 1 && isRentProjectAct(rows.get(0))) {
              multiReturnRentProject(rows.get(0));
            } else if (rows.size() == 1) {
              checkItemsBeforeReturn(rows.get(0));
            } else if (rows.size() > 1 && !mixedReturn) {
              multiReturn(rows);
            } else if (mixedReturn) {
              multiMixedReturn(rows);
            } else {
              getGridView().notifyWarning(Localized.dictionary().selectAtLeastOneRow());
            }

            Data.resetLocal(VIEW_TRADE_ACTS);
          });

      TradeActKeeper.addCommandStyle(returnCommand, "return");
      TradeActKeeper.setCommandEnabled(returnCommand, false);
    }
    return returnCommand;
  }

  private Button ensureRemoveFromRent() {
    if (removeFromRentCommand != null) {
      return removeFromRentCommand;
    }

    removeFromRentCommand = new Button("Pašalinti iš nuomos proj.", e -> {
      IsRow row = getGridView().getActiveRow();

      if (row == null) {
        getGridView().notifyWarning(Localized.dictionary().selectAtLeastOneRow());
        return;
      }
      Global.confirm("Ar norite aktą pašalinti iš nuomos projekto ? ", () -> {

        Queries.updateCellAndFire(VIEW_TRADE_ACTS, row.getId(), row.getVersion(),
            COL_TA_RENT_PROJECT, Data.getString(VIEW_TRADE_ACTS, row, COL_TA_RENT_PROJECT),
            null);

        Data.resetLocal(VIEW_TRADE_ACTS);
      });
    });

    TradeActKeeper.addCommandStyle(removeFromRentCommand, "projectCommand");
    TradeActKeeper.setCommandEnabled(removeFromRentCommand, false);

    return removeFromRentCommand;
  }

  private Button ensureToRentProject() {
    if (toRentProjectCommand != null) {
      return toRentProjectCommand;
    }

    toRentProjectCommand = new Button("Priskirti nuomos proj.", e -> {
      List<IsRow> rows = new ArrayList<>();
      IsRow firstRow = null;

      for (IsRow row : getGridView().getRowData()) {
        if (!getGridView().isRowSelected(row.getId())) {
          continue;
        }

        if (firstRow == null) {
          firstRow = row;
        }

        if (getKind(row) == null || !getKind(row).enableReturn()) {
          getGridView().notifyWarning(Localized.dictionary().taIsDifferent());
          return;
        }

        if (DataUtils.isId(getRentProject(row))) {
          getGridView().notifyWarning("Aktas " + row.getId() + "priskirtas nuomos projektui "
              + getRentProject(row));
          return;
        }

        if (!Objects.equals(getSeries(firstRow), getSeries(row))
            || !Objects.equals(getCompany(firstRow), getCompany(row))) {
          getGridView().notifyWarning(Localized.dictionary().taIsDifferent());
          return;
        }

        if (DataUtils.isId(getContinuousAct(row))) {
          getGridView().notifyWarning("Aktas " + row.getId() + " susietas su tęstiniu "
              + getContinuousAct(row));
          return;
        }

        if (getKind(row) == null || isContinuousAct(row) || isRentProjectAct(row)) {
          getGridView().notifyWarning("Aktas " + row.getId()
              + (getKind(row) != null ? " yra " + getKind(row).getCaption() : ""));
        }

        rows.add(row);
      }

      if (rows.isEmpty()) {
        getGridView().notifyWarning(Localized.dictionary().selectAtLeastOneRow());
        return;
      }

      assignToRentProject(rows);

      Data.resetLocal(VIEW_TRADE_ACTS);

    });

    TradeActKeeper.addCommandStyle(toRentProjectCommand, "projectCommand");
    TradeActKeeper.setCommandEnabled(toRentProjectCommand, false);

    return toRentProjectCommand;
  }

  private Button ensureAlterCommand() {
    if (alterCommand == null) {
      alterCommand = new Button(Localized.dictionary().taAlterKind(),
          event -> {
            IsRow row = getGridView().getActiveRow();
            if (row != null) {
              alterKind(row);
            }
          });

      TradeActKeeper.addCommandStyle(alterCommand, "alter");
      TradeActKeeper.setCommandEnabled(alterCommand, false);
    }
    return alterCommand;
  }

  private void alterKind(IsRow row) {
    TradeActKind tak = TradeActKeeper.getKind(getViewName(), row);

    if (tak != null && tak.enableAlter()) {
      final List<TradeActKind> targets = new ArrayList<>();
      List<String> choices = new ArrayList<>();

      for (TradeActKind target : TradeActKind.values()) {
        if (target.isAlterTarget() && target != tak) {
          targets.add(target);
          choices.add(target.getCaption());
        }
      }

      final long actId = row.getId();

      MessageBoxes.choice(Localized.dictionary().taAlterKind(),
          BeeUtils.joinWords(tak.getCaption(), row.getString(getDataIndex(COL_TRADE_ACT_NAME))),
          choices, value -> {
            if (DataUtils.idEquals(getGridView().getActiveRow(), actId)
                && BeeUtils.isIndex(targets, value)) {

              ParameterList params = TradeActKeeper.createArgs(SVC_ALTER_ACT_KIND);
              params.addQueryItem(COL_TRADE_ACT, actId);
              params.addQueryItem(COL_TA_KIND, targets.get(value).ordinal());

              BeeKeeper.getRpc().makeRequest(params, response -> {
                if (response.hasResponse(BeeRow.class)) {
                  BeeRow updated = BeeRow.restore(response.getResponseAsString());
                  RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), updated);

                  GridView gridView = getGridView();

                  if (gridView != null && gridView.asWidget().isAttached()) {
                    if (DataUtils.idEquals(gridView.getActiveRow(), actId)) {
                      refreshCommands(updated, !gridView.getSelectedRows(GridView.SelectedRows.ALL).isEmpty());
                    }
                    maybeOpenAct(gridView, updated);
                  }
                }
                Data.resetLocal(VIEW_TRADE_ACTS);
              });
            }
          }, BeeConst.UNDEF, BeeConst.UNDEF, Localized.dictionary().cancel(), null);
    }
  }

  private void assignToRentProject(List<IsRow> selectedRows) {
    Long combinedActStatus = Global.getParameterRelation(PRM_COMBINED_ACT_STATUS);
    Relation relation = Relation.create();
    relation.setViewName(VIEW_TRADE_ACTS);
    relation
        .setChoiceColumns(
            Arrays.asList("Id", "OperationName", "CompanyName", "TypeName", "SeriesName",
                "Number", "ActName", "Date", "ObjectName", "ObjectAddress"));
    relation.setFilter(Filter.and(Filter.equals(COL_TA_KIND, TradeActKind.RENT_PROJECT.ordinal()),
        Filter.or(Filter.notEquals(COL_TA_STATUS, combinedActStatus), Filter.isNull(COL_TA_STATUS)
        ), Filter.equals(COL_TA_COMPANY, getCompany(selectedRows.get(0)))));

    UnboundSelector selector = UnboundSelector.create(relation);
    selector.setNullable(false);

    Global.inputWidget("Pasirinkite nuomos projektą", selector, () -> {
      if (!DataUtils.isId(selector.getRelatedId())) {
        return;
      }

      Set<Long> ids = new HashSet<>();

      selectedRows.forEach(row -> ids.add(row.getId()));
      assignToRentProject(ids, selector.getRelatedId());
    });
  }

  private void assignToRentProject(Set<Long> selectedRows, Long rentProject) {
    ParameterList parameters = TradeActKeeper.createArgs(SVC_ASSIGN_RENT_PROJECT);
    parameters.addDataItem(COL_TRADE_ACT, rentProject);
    parameters.addDataItem(TradeConstants.VAR_ID_LIST, DataUtils.buildIdList(selectedRows));

    BeeKeeper.getRpc().makePostRequest(parameters, response -> {
      if (!response.hasResponse(BeeRowSet.class)) {
        return;
      }

      BeeRowSet rowSet = BeeRowSet.restore(response.getResponseAsString());
      rowSet.forEach(row -> RowUpdateEvent.fire(BeeKeeper.getBus(), rowSet.getViewName(), row));
      getGridView().notifyInfo("Aktai priskiti nuomos projektui " + rentProject);
    });
  }

  private void checkItemsBeforeReturn(final IsRow parent) {
    final TradeActKind parentKind = TradeActKeeper.getKind(VIEW_TRADE_ACTS, parent);

    if (parentKind == null || !parentKind.enableReturn()) {
      getGridView().notifyWarning(Localized.dictionary().taIsDifferent());
      return;
    }

    ParameterList params = TradeActKeeper.createArgs(SVC_GET_ITEMS_FOR_RETURN);
    params.addQueryItem(COL_TRADE_ACT, parent.getId());

    BeeKeeper.getRpc().makeRequest(params, response -> {
      if (response.hasResponse(BeeRowSet.class)) {
        createReturn(parent);
      } else {
        getGridView().notifyWarning(Localized.dictionary().noData());
      }
    });

  }

  private void createReturn(IsRow parent) {
    createReturn(parent, null, null);
  }

  private void createReturn(BeeRowSet parentActs, BeeRowSet parentItems) {
    createReturn(null, parentActs, parentItems);
  }

  private void createReturn(final IsRow parent, BeeRowSet parentActs, BeeRowSet parentItems) {

    if (!DataUtils.isEmpty(parentActs) && !DataUtils.isEmpty(parentItems) && !isRentProjectAct(
        parent)) {
      createReturnActForm(parentActs, parentItems);
      return;
    } else if (parent == null) {
      Assert.untouchable();
    }

    ParameterList prm = TradeActKeeper.createArgs(SVC_GET_NEXT_RETURN_ACT_NUMBER);

    if (DataUtils.isId(parent.getLong(getDataIndex(COL_TA_SERIES)))) {
      prm.addDataItem(COL_TA_SERIES, parent.getLong(getDataIndex(COL_TA_SERIES)));
    }

    prm.addDataItem(TradeConstants.VAR_VIEW_NAME, getViewName());
    prm.addDataItem(Service.VAR_COLUMN, COL_TA_NUMBER);
    prm.addDataItem(COL_TA_PARENT, parent.getId());

    BeeKeeper.getRpc().makePostRequest(prm, response -> {
      if (isRentProjectAct(parent)) {
        createReturnActForm(parent, response.getResponseAsString(), parentActs, parentItems);
      } else {
        createReturnActForm(parent, response.getResponseAsString());
      }
    });
  }

  private void createReturnActForm(BeeRowSet parentActs, BeeRowSet parentItems) {
    createReturnActForm(null, null, parentActs, parentItems);
  }

  private void createReturnActForm(IsRow parent, String number) {
    createReturnActForm(parent, number, null, null);
  }

  private void createReturnActForm(IsRow parent, String number, BeeRowSet parentActs,
      BeeRowSet parentItems) {
    DataInfo viewTradeActs = Data.getDataInfo(getViewName());
    BeeRow newRow = RowFactory.createEmptyRow(viewTradeActs, true);

    for (int i = 0; i < getDataColumns().size(); i++) {
      String colId = getDataColumns().get(i).getId();

      switch (colId) {
        case COL_TA_KIND:
          newRow.setValue(i, TradeActKind.RETURN.ordinal());
          break;

        case COL_TA_DATE:
          Long defTime = Global.getParameterTime(PRM_DEFAULT_RETURN_ACT_TIME);
          newRow.setValue(i, (DateTime) null);

          if (!BeeUtils.isPositive(defTime)) {
            break;
          }

          newRow.setValue(i, TimeUtils.combine(new DateTime(), defTime));
          break;

        case COL_TA_PARENT:
          if (parent == null) {
            break;
          }
          newRow.setValue(i, parent.getId());
          break;

        case COL_TA_NUMBER:
          if (parent == null || BeeUtils.isEmpty(number)) {
            break;
          }
          newRow.setValue(i, parent.getString(i) + "-"
              + number);
          break;

        case COL_TA_SERIES:
        case TradeConstants.COL_SERIES_NAME:
          if (parentActs != null && !isRentProjectAct(parent)) {
            newRow.setValue(i, parentActs.getString(0, i));
          } else if (parent != null) {
            newRow.setValue(i, parent.getString(i));
          }
          break;

        case COL_TA_COMPANY:
        case COL_TA_CONTRACT:
        case COL_TA_CONTACT:
        case COL_TA_OBJECT:
          if (parentActs != null) {
            if (viewTradeActs.hasRelation(colId)) {
              RelationUtils.updateRow(viewTradeActs, colId, newRow, viewTradeActs,
                  parentActs.getRow(0), false);
            }
            newRow.setValue(i, parentActs.getString(0, i));
          } else if (parent != null) {
            if (viewTradeActs.hasRelation(colId)) {
              RelationUtils.updateRow(viewTradeActs, colId, newRow, viewTradeActs,
                  parent, false);
            }
            newRow.setValue(i, parent.getString(i));
          }
          break;
        case COL_TA_RENT_PROJECT:
          if (parent != null && isRentProjectAct(parent)) {
            newRow.setValue(i, parent.getId());
          } else if (parent != null && DataUtils.isId(parent.getLong(i))) {
            RelationUtils.updateRow(viewTradeActs, colId, newRow, viewTradeActs,
                parent, false);
            newRow.setValue(i, parent.getLong(i));
          } else if (parent == null && parentActs.getDistinctLongs(i).size() ==1) {
            RelationUtils.updateRow(viewTradeActs, colId, newRow, viewTradeActs,
                    parentActs.getRow(0), false);
            newRow.setValue(i, parentActs.getLong(0, i));
          }
          break;
        case COL_TA_NAME:
            if(parent == null && parentActs != null
                    && parentActs.getDistinctLongs(i).size() == 1) {
              newRow.setValue(i, parentActs.getLong(0, i));
              RelationUtils.updateRow(viewTradeActs, colId, newRow, viewTradeActs,
                      parentActs.getRow(0), false);
            } else if (parent != null) {
              RelationUtils.updateRow(viewTradeActs, colId, newRow, viewTradeActs,
                      parent, false);
            }
          break;
        case COL_TA_UNTIL:
        case COL_TA_NOTES:
        case COL_TA_RETURN:
        case COL_TA_CONTINUOUS:
        case ALS_RETURNED_COUNT:
        case COL_TA_INPUT_DRIVER:
        case COL_TA_INPUT_VEHICLE:
        case COL_TA_MANAGER:
        case "ManagerPerson":
          break;

        default:
          if (BeeUtils.startsWith(colId, COL_TA_RENT_PROJECT)){
            break;
          }
          if (parent != null && !parent.isNull(i) && !colId.startsWith(COL_TA_STATUS)
              && !colId.startsWith(COL_TA_OPERATION)) {
            if (viewTradeActs.hasRelation(colId)) {
              RelationUtils.updateRow(viewTradeActs, colId, newRow, viewTradeActs,
                  parent, false);
            }

            if (viewTradeActs.getColumn(colId).getLevel() == 0) {
              newRow.setValue(i, parent.getString(i));
            }
          }
      }
    }

    if (!DataUtils.isEmpty(parentActs) && !DataUtils.isEmpty(parentItems)) {
      newRow.setProperty(PRP_MULTI_RETURN_DATA, Codec.beeSerialize(Pair.of(parentActs,
          parentItems)));
    }

    TradeActKeeper.setDefaultOperation(newRow, TradeActKind.RETURN);

    RowFactory.createRow(viewTradeActs, newRow, Opener.MODAL, result -> {
      getGridView().ensureRow(result, true);
      if (parent != null) {
        Queries.getRow(VIEW_TRADE_ACTS, parent.getId(), updatedParent -> {
          DataInfo info = Data.getDataInfo(VIEW_TRADE_ACTS);
          RowEditor.openForm(info.getEditForm(), info, Filter.compareId(updatedParent.getId()),
              Opener.modeless());
        });
      }
    });
  }

  private String getActName(IsRow row) {
    return row.getString(getDataIndex(COL_TRADE_ACT_NAME));
  }

  private Long getCompany(IsRow row) {
    return row.getLong(getDataIndex(COL_TA_COMPANY));
  }

  private Long getContinuousAct(IsRow row) {
    return row.getLong(getDataIndex(COL_TA_CONTINUOUS));
  }

  private TradeActKind getKind(IsRow row) {
    return TradeActKeeper.getKind(getViewName(), row);
  }

  private String getNumber(IsRow row) {
    return row.getString(getDataIndex(COL_TA_NUMBER));
  }

  private Long getObject(IsRow row) {
    return row.getLong(getDataIndex(COL_TA_OBJECT));
  }

  private Long getRentProject(IsRow row) {
    return row.getLong(getDataIndex(COL_TA_RENT_PROJECT));
  }

  private Long getSeries(IsRow row) {
    return row.getLong(getDataIndex(COL_TA_SERIES));
  }

  private boolean isContinuousAct(IsRow row) {
    return TradeActKind.CONTINUOUS == getKind(row);
  }

  private boolean isRentProjectAct(IsRow row) {
    return TradeActKind.RENT_PROJECT == getKind(row);
  }

  private void multiReturnContinuousAct(final Collection<IsRow> ctActs) {
    DataInfo taInfo = Data.getDataInfo(VIEW_TRADE_ACTS);

    Queries.getRowSet(VIEW_TRADE_ACTS, taInfo.getColumnNames(false),
        Filter.in(Data.getIdColumn(VIEW_TRADE_ACTS), VIEW_TRADE_ACT_ITEMS, COL_TA_PARENT,
            Filter.any(COL_TRADE_ACT, DataUtils.getRowIds(ctActs))),
        result -> {
          List<IsRow> rows = new ArrayList<>(result.getRows());
          multiReturn(rows);
        });
  }

  private void multiReturnRentProject(IsRow activeRow) {
    DataInfo taInfo = Data.getDataInfo(VIEW_TRADE_ACTS);

    Queries.getRowSet(VIEW_TRADE_ACTS, taInfo.getColumnNames(false),
        Filter.and(Filter.equals(COL_TA_RENT_PROJECT, activeRow.getId()),
            Filter.or(Filter.equals(COL_TA_KIND, TradeActKind.SALE),
                Filter.equals(COL_TA_KIND, TradeActKind.SUPPLEMENT))),
        result -> {
          List<IsRow> rows = new ArrayList<>(result.getRows());
          multiReturn(activeRow, rows);
        });
  }

  private void multiMixedReturn(final Collection<IsRow> mixedActs) {
    DataInfo taInfo = Data.getDataInfo(VIEW_TRADE_ACTS);

    Queries.getRowSet(VIEW_TRADE_ACTS, taInfo.getColumnNames(false),
        Filter.or(
            Filter.in(Data.getIdColumn(VIEW_TRADE_ACTS), VIEW_TRADE_ACT_ITEMS, COL_TA_PARENT,
                Filter.any(COL_TRADE_ACT, DataUtils.getRowIds(mixedActs))),
            Filter.in(Data.getIdColumn(VIEW_TRADE_ACTS), VIEW_TRADE_ACT_ITEMS, COL_TRADE_ACT,
                Filter.and(
                    Filter.any(COL_TRADE_ACT, DataUtils.getRowIds(mixedActs)),
                    Filter.isNull(COL_TA_PARENT)
                )
            )
        ), result -> {
          List<IsRow> rows = new ArrayList<>(result.getRows());
          multiReturn(rows);
        });

  }

  private void multiReturn(final Collection<IsRow> parents) {
    multiReturn(null, parents);
  }

  private void multiReturn(IsRow activeRow, final Collection<IsRow> parents) {
    ParameterList params = TradeActKeeper.createArgs(SVC_GET_ITEMS_FOR_MULTI_RETURN);
    params.addQueryItem(Service.VAR_LIST, DataUtils.buildIdList(DataUtils.getRowIds(parents)));
    params.addQueryItem("DEBUG", "[TradeActGrid][multiReturn]");

    BeeKeeper.getRpc().makeRequest(params, response -> {
      if (response.hasResponse(BeeRowSet.class)) {
        BeeRowSet parentItems = BeeRowSet.restore(response.getResponseAsString());

        BeeRowSet parentActs = Data.createRowSet(getViewName());
        for (IsRow parent : parents) {
          parentActs.addRow(DataUtils.cloneRow(parent));
        }
        createReturn(activeRow, parentActs, parentItems);
      } else {
        getGridView().notifyWarning(Localized.dictionary().noData());
      }
    });
  }

  private Button ensureSupplementCommand() {
    if (supplementCommand == null) {
      supplementCommand = new Button(Localized.dictionary().taKindSupplement(),
          event -> {
            IsRow row = getGridView().getActiveRow();
            if (row != null) {
              createSupplement(row);
              Data.resetLocal(VIEW_TRADE_ACTS);
            }
          });

      TradeActKeeper.addCommandStyle(supplementCommand, "supplement");
      TradeActKeeper.setCommandEnabled(supplementCommand, false);
    }
    return supplementCommand;
  }

  private void createSupplement(IsRow parent) {
    ParameterList prm = TradeActKeeper.createArgs(SVC_GET_NEXT_CHILD_ACT_NUMBER);

    if (DataUtils.isId(parent.getLong(getDataIndex(COL_TA_SERIES)))) {
      prm.addDataItem(COL_TA_SERIES, parent.getLong(getDataIndex(COL_TA_SERIES)));
    }

    prm.addDataItem(TradeConstants.VAR_VIEW_NAME, getViewName());
    prm.addDataItem(Service.VAR_COLUMN, COL_TA_NUMBER);
    prm.addDataItem(COL_TA_PARENT, parent.getId());
    prm.addDataItem(COL_TA_KIND, TradeActKind.SUPPLEMENT.ordinal());

    BeeKeeper.getRpc().makeRequest(prm, response -> {
      DataInfo dataInfo = Data.getDataInfo(getViewName());
      BeeRow newRow = RowFactory.createEmptyRow(dataInfo, false);

      for (int i = 0; i < getDataColumns().size(); i++) {
        String colId = getDataColumns().get(i).getId();

        switch (colId) {
          case COL_TA_KIND:
            if (TradeActKind.RENT_PROJECT.equals(getKind(parent))) {
              newRow.setValue(i, TradeActKind.SALE.ordinal());
              break;
            }
            newRow.setValue(i, TradeActKind.SUPPLEMENT.ordinal());
            break;

          case COL_TA_DATE:
            newRow.setValue(i, TimeUtils.nowMinutes());
            break;

          case COL_TA_PARENT:
            if (TradeActKind.RENT_PROJECT.equals(getKind(parent))) {
              break;
            }
            newRow.setValue(i, parent.getId());
            break;

          case COL_TA_NUMBER:
            if (!parent.isNull(i)) {
              newRow.setValue(i, BeeUtils.trim(parent.getString(i)) + " P-"
                  + response.getResponseAsString());
            }
            break;

          case COL_TA_RENT_PROJECT:
            if (TradeActKind.RENT_PROJECT.equals(getKind(parent))) {
              newRow.setValue(i, parent.getId());
              RelationUtils.updateRow(Data.getDataInfo(VIEW_TRADE_ACTS), colId, newRow,
                  Data.getDataInfo(VIEW_TRADE_ACTS), parent, true);
            } else {
              newRow.setValue(i, parent.getValue(i));
              RelationUtils.setRelatedValues(Data.getDataInfo(VIEW_TRADE_ACTS), colId, newRow,
                  parent);
            }

            break;

          case COL_TA_CONTINUOUS:
          case COL_TA_RETURN:
          case COL_TA_UNTIL:
          case COL_TA_NOTES:
          case ALS_RETURNED_COUNT:
          case "RentProjectName":
          case "RentProjectSeriesName":
          case "RentProjectNumber":
          case "RentProjectKind":
            break;

          default:
            if (!parent.isNull(i) && !colId.startsWith(COL_TA_STATUS)) {
              newRow.setValue(i, parent.getString(i));
            }
        }
      }

      TradeActKeeper.setDefaultOperation(newRow, TradeActKind.SUPPLEMENT);

      RowFactory.createRow(dataInfo, newRow, Opener.MODAL,
          result -> getGridView().ensureRow(result, true));
    });
  }

  private Button ensureTemplateCommand() {
    if (templateCommand == null) {
      templateCommand = new Button(Localized.dictionary().tradeActSaveAsTemplate(),
          event -> {
            int maxLen = Data.getColumnPrecision(VIEW_TRADE_ACT_TEMPLATES, COL_TA_TEMPLATE_NAME);

            Global.inputString(Localized.dictionary().tradeActNewTemplate(),
                Localized.dictionary().name(), new StringCallback() {
                  @Override
                  public void onSuccess(String value) {
                    if (!BeeUtils.isEmpty(value)) {
                      saveAsTemplate(value.trim());
                    }
                  }
                }, null, null, maxLen);
          });

      TradeActKeeper.addCommandStyle(templateCommand, "template");
      TradeActKeeper.setCommandEnabled(templateCommand, false);
    }
    return templateCommand;
  }

  private static void maybeOpenAct(GridView gridView, IsRow row) {
    if (DomUtils.isVisible(gridView.getGrid())) {
      gridView.onEditStart(new EditStartEvent(row, gridView.isReadOnly()));
    }
  }

  private void refreshCommands(IsRow row, boolean multipleSelection) {
    TradeActKind k = null;

    if (row != null) {
      k = TradeActKeeper.getKind(row, getDataIndex(COL_TA_KIND));
    }
    TradeActKeeper.setCommandEnabled(supplementCommand, row != null && k != null
            && k.enableSupplement() && !multipleSelection);
    TradeActKeeper.setCommandEnabled(returnCommand, row != null && k != null && k.enableReturn()
        && !DataUtils.isId(getContinuousAct(row)) || multipleSelection);
    TradeActKeeper.setCommandEnabled(toRentProjectCommand, row != null && k != null && k.enableReturn()
        && !(isRentProjectAct(row) || DataUtils.isId(getRentProject(row))));
    TradeActKeeper.setCommandEnabled(alterCommand, row != null && k != null && k.enableAlter() && !multipleSelection);
    TradeActKeeper.setCommandEnabled(copyCommand, row != null && k != null && k.enableCopy() && !multipleSelection);
    TradeActKeeper.setCommandEnabled(templateCommand, row != null && k != null && k.enableTemplate() && !multipleSelection);
    TradeActKeeper
        .setCommandEnabled(removeFromRentCommand, BeeKeeper.getUser().canCreateData(VIEW_TRADE_ACTS)
            && row != null && DataUtils.isId(getRentProject(row)));
  }

  private void saveAsTemplate(String name) {
    IsRow row = getGridView().getActiveRow();

    if (row == null) {
      logger.severe(SVC_SAVE_ACT_AS_TEMPLATE, "act row not available");

    } else {
      ParameterList params = TradeActKeeper.createArgs(SVC_SAVE_ACT_AS_TEMPLATE);
      params.addDataItem(COL_TRADE_ACT, row.getId());
      params.addDataItem(COL_TA_TEMPLATE_NAME, name);

      BeeKeeper.getRpc().makeRequest(params, response -> {
        response.notify(getGridView());

        if (response.hasResponse(BeeRow.class)) {
          BeeRow template = BeeRow.restore(response.getResponseAsString());
          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TRADE_ACT_TEMPLATES);

          Data.resetLocal(VIEW_TRADE_ACTS);
          RowEditor.open(VIEW_TRADE_ACT_TEMPLATES, template, Opener.MODAL);
        }
      });
    }
  }

  private static void checkContract(final IsRow row, final GwtEvent<?> event,
      final GridView gridView) {
    int idxKind = Data.getColumnIndex(VIEW_TRADE_ACTS, COL_TA_KIND);
    if (TradeActKind.SALE.ordinal() == BeeUtils.unbox(row.getInteger(idxKind))
        || TradeActKind.SUPPLEMENT.ordinal() == BeeUtils.unbox(row.getInteger(idxKind))) {

      int contractIdx = Data.getColumnIndex(VIEW_TRADE_ACTS, WIDGET_TA_CONTRACT);
      if (!BeeUtils.isPositive(row.getInteger(contractIdx))) {

        if (event instanceof ReadyForInsertEvent) {
          ((ReadyForInsertEvent) event).consume();
        }
        if (event instanceof SaveChangesEvent) {
          ((SaveChangesEvent) event).consume();
        }

        Global.confirm(Localized.dictionary().taEmptyContract()
            + Localized.dictionary().saveChanges(), () -> gridView.fireEvent(event));
      }
    }
  }

  private void revertStatuses(Long... acts) {
    ParameterList prm = TradeActKeeper.createArgs(SVC_REVERT_ACTS_STATUS_BEFORE_DELETE);
    prm.addDataItem(TradeConstants.VAR_ID_LIST, DataUtils.buildIdList(acts));

    BeeKeeper.getRpc().makePostRequest(prm, response -> {
      if (response.hasErrors()) {
        getGridView().notifySevere(response.getErrors());
        return;
      }

      if (getGridView() != null && getGridView().getGrid() != null) {
        getGridView().getGrid().clearSelection();
      }

      Queries.delete(VIEW_TRADE_ACTS, Filter.idIn(Lists.newArrayList(acts)),
          result -> DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TRADE_ACTS));
    });
  }
}
