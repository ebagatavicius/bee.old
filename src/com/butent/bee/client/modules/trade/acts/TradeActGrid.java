package com.butent.bee.client.modules.trade.acts;

import com.google.common.collect.Lists;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.MessageBoxes;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.ActiveRowChangeEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.search.ListFilterSupplier;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.FilterComponent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActKind;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class TradeActGrid extends AbstractGridInterceptor {

  private static final BeeLogger logger = LogUtils.getLogger(TradeActGrid.class);

  private final TradeActKind kind;

  private Button supplementCommand;
  private Button returnCommand;
  private Button alterCommand;
  private Button copyCommand;
  private Button templateCommand;

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
      }

      presenter.getHeader().addCommandItem(ensureAlterCommand());

      if (canCreateActs) {
        presenter.getHeader().addCommandItem(ensureCopyCommand());
      }
      if (BeeKeeper.getUser().canCreateData(VIEW_TRADE_ACT_TEMPLATES)) {
        presenter.getHeader().addCommandItem(ensureTemplateCommand());
      }
    }

    super.afterCreatePresenter(presenter);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    newActKind = kind;

    if (kind == null) {
      final List<TradeActKind> kinds = Lists.newArrayList(TradeActKind.SALE, TradeActKind.TENDER,
          TradeActKind.PURCHASE, TradeActKind.WRITE_OFF, TradeActKind.RESERVE);

      List<String> options = new ArrayList<>();
      for (TradeActKind k : kinds) {
        options.add(k.getCaption());
      }

      Global.choice(Localized.dictionary().tradeActNew(), null, options, new ChoiceCallback() {
        @Override
        public void onSuccess(int value) {
          if (BeeUtils.isIndex(kinds, value)) {
            newActKind = kinds.get(value);
            getGridView().startNewRow(false);
          }
        }
      });

      return false;

    } else {
      return super.beforeAddRow(presenter, copy);
    }
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

    BeeRowSet statuses = TradeActKeeper.getStatuses();
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
    refreshCommands(event.getRowValue());
    super.onActiveRowChange(event);
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
    TradeActKeeper.prepareNewTradeAct(newRow, newActKind);
    return super.onStartNewRow(gridView, oldRow, newRow, copy);
  }

  private Button ensureCopyCommand() {
    if (copyCommand == null) {
      copyCommand = new Button(Localized.dictionary().actionCopy(),
          new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              final IsRow row = getGridView().getActiveRow();
              TradeActKind tak = TradeActKeeper.getKind(getViewName(), row);

              if (tak != null) {
                List<String> messages = new StringList();

                messages.add(row.getString(getDataIndex(COL_TRADE_ACT_NAME)));

                String number = row.getString(getDataIndex(COL_TA_NUMBER));
                if (!BeeUtils.isEmpty(number)) {
                  messages.add(BeeUtils.joinWords(
                      row.getString(getDataIndex(TradeConstants.COL_SERIES_NAME)), number));
                }

                messages.add(Localized.dictionary().tradeActCopyQuestion());

                Global.confirm(tak.getCaption(), Icon.QUESTION, messages,
                    Localized.dictionary().actionCopy(), Localized.dictionary().actionCancel(),
                    new ConfirmationCallback() {
                      @Override
                      public void onConfirm() {
                        if (DataUtils.sameId(row, getGridView().getActiveRow())) {
                          doCopy(row.getId());
                        }
                      }
                    });
              }
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

    BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (Queries.checkRowResponse(SVC_COPY_ACT, getViewName(), response)) {
          BeeRow row = BeeRow.restore(response.getResponseAsString());
          GridView gridView = getGridView();

          if (gridView != null && gridView.asWidget().isAttached()) {
            gridView.ensureRow(row, true);
            maybeOpenAct(gridView, row);
          }

          RowInsertEvent.fire(BeeKeeper.getBus(), getViewName(), row,
              (gridView == null) ? null : gridView.getId());
        }
      }
    });
  }

  private Button ensureReturnCommand() {
    if (returnCommand == null) {
      returnCommand = new Button(Localized.dictionary().taKindReturn(),
          new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              List<IsRow> rows = new ArrayList<>();

              IsRow activeRow = getGridView().getActiveRow();
              for (IsRow row : getGridView().getRowData()) {
                if (getGridView().isRowSelected(row.getId()) || DataUtils.sameId(row, activeRow)) {
                  TradeActKind tak = TradeActKeeper.getKind(getViewName(), row);
                  if (tak != null && tak.enableReturn()) {
                    rows.add(row);
                  }
                }
              }

              if (rows.size() == 1) {
                createReturn(rows.get(0));
              } else if (rows.size() > 1) {
                multiReturn(rows);
              }
            }
          });

      TradeActKeeper.addCommandStyle(returnCommand, "return");
      TradeActKeeper.setCommandEnabled(returnCommand, false);
    }
    return returnCommand;
  }

  private Button ensureAlterCommand() {
    if (alterCommand == null) {
      alterCommand = new Button(Localized.dictionary().taAlterKind(),
          new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              IsRow row = getGridView().getActiveRow();
              if (row != null) {
                alterKind(row);
              }
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

              BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
                @Override
                public void onResponse(ResponseObject response) {
                  if (response.hasResponse(BeeRow.class)) {
                    BeeRow updated = BeeRow.restore(response.getResponseAsString());
                    RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), updated);

                    GridView gridView = getGridView();

                    if (gridView != null && gridView.asWidget().isAttached()) {
                      if (DataUtils.idEquals(gridView.getActiveRow(), actId)) {
                        refreshCommands(updated);
                      }
                      maybeOpenAct(gridView, updated);
                    }
                  }
                }
              });
            }
          }, BeeConst.UNDEF, BeeConst.UNDEF, Localized.dictionary().cancel(), null);
    }
  }

  private void createReturn(IsRow parent) {
    DataInfo dataInfo = Data.getDataInfo(getViewName());
    BeeRow newRow = RowFactory.createEmptyRow(dataInfo, false);

    for (int i = 0; i < getDataColumns().size(); i++) {
      String colId = getDataColumns().get(i).getId();

      switch (colId) {
        case COL_TA_KIND:
          newRow.setValue(i, TradeActKind.RETURN.ordinal());
          break;

        case COL_TA_DATE:
          newRow.setValue(i, TimeUtils.nowMinutes());
          break;

        case COL_TA_PARENT:
          newRow.setValue(i, parent.getId());
          break;

        case COL_TA_UNTIL:
        case COL_TA_NOTES:
          break;

        default:
          if (!parent.isNull(i) && !colId.startsWith(COL_TA_STATUS)
              && !colId.startsWith(COL_TA_OPERATION)) {
            newRow.setValue(i, parent.getString(i));
          }
      }
    }

    TradeActKeeper.setDefaultOperation(newRow, TradeActKind.RETURN);

    RowFactory.createRow(dataInfo, newRow, Modality.ENABLED, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        getGridView().ensureRow(result, true);
      }
    });
  }

  private void multiReturn(final Collection<IsRow> parents) {
    ParameterList params = TradeActKeeper.createArgs(SVC_GET_ITEMS_FOR_MULTI_RETURN);
    params.addQueryItem(Service.VAR_LIST, DataUtils.buildIdList(DataUtils.getRowIds(parents)));

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(BeeRowSet.class)) {
          BeeRowSet parentItems = BeeRowSet.restore(response.getResponseAsString());

          BeeRowSet parentActs = Data.createRowSet(getViewName());
          for (IsRow parent : parents) {
            parentActs.addRow(DataUtils.cloneRow(parent));
          }

          TradeActItemReturn.show(Localized.dictionary().taKindReturn(), parentActs, parentItems,
              true, new Consumer<BeeRowSet>() {
                @Override
                public void accept(BeeRowSet selectedItems) {
                  if (!DataUtils.isEmpty(selectedItems)) {
                    ParameterList args = TradeActKeeper.createArgs(SVC_RETURN_ACT_ITEMS);
                    args.addDataItem(VIEW_TRADE_ACT_ITEMS, selectedItems.serialize());

                    BeeKeeper.getRpc().makeRequest(args, new ResponseCallback() {
                      @Override
                      public void onResponse(ResponseObject ro) {
                        DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TRADE_ACT_ITEMS);
                        DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TRADE_ACT_SERVICES);
                        DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TRADE_ACTS);
                      }
                    });
                  }
                }
              });

        } else {
          getGridView().notifyWarning(Localized.dictionary().noData());
        }
      }
    });
  }

  private Button ensureSupplementCommand() {
    if (supplementCommand == null) {
      supplementCommand = new Button(Localized.dictionary().taKindSupplement(),
          new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              IsRow row = getGridView().getActiveRow();
              if (row != null) {
                createSupplement(row);
              }
            }
          });

      TradeActKeeper.addCommandStyle(supplementCommand, "supplement");
      TradeActKeeper.setCommandEnabled(supplementCommand, false);
    }
    return supplementCommand;
  }

  private void createSupplement(IsRow parent) {
    DataInfo dataInfo = Data.getDataInfo(getViewName());
    BeeRow newRow = RowFactory.createEmptyRow(dataInfo, false);

    for (int i = 0; i < getDataColumns().size(); i++) {
      String colId = getDataColumns().get(i).getId();

      switch (colId) {
        case COL_TA_KIND:
          newRow.setValue(i, TradeActKind.SUPPLEMENT.ordinal());
          break;

        case COL_TA_DATE:
          newRow.setValue(i, TimeUtils.nowMinutes());
          break;

        case COL_TA_PARENT:
          newRow.setValue(i, parent.getId());
          break;

        case COL_TA_NUMBER:
          if (!parent.isNull(i)) {
            newRow.setValue(i, BeeUtils.trim(parent.getString(i)) + "-1");
          }
          break;

        case COL_TA_UNTIL:
        case COL_TA_NOTES:
          break;

        default:
          if (!parent.isNull(i) && !colId.startsWith(COL_TA_STATUS)) {
            newRow.setValue(i, parent.getString(i));
          }
      }
    }

    TradeActKeeper.setDefaultOperation(newRow, TradeActKind.SUPPLEMENT);

    RowFactory.createRow(dataInfo, newRow, Modality.ENABLED, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        getGridView().ensureRow(result, true);
      }
    });
  }

  private Button ensureTemplateCommand() {
    if (templateCommand == null) {
      templateCommand = new Button(Localized.dictionary().tradeActSaveAsTemplate(),
          new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
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
            }
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

  private void refreshCommands(IsRow row) {
    TradeActKind k = TradeActKeeper.getKind(row, getDataIndex(COL_TA_KIND));

    if (supplementCommand != null) {
      TradeActKeeper.setCommandEnabled(supplementCommand, k != null && k.enableSupplement());
    }
    if (returnCommand != null) {
      TradeActKeeper.setCommandEnabled(returnCommand, k != null && k.enableReturn());
    }

    if (alterCommand != null) {
      TradeActKeeper.setCommandEnabled(alterCommand, k != null && k.enableAlter());
    }

    if (copyCommand != null) {
      TradeActKeeper.setCommandEnabled(copyCommand, k != null && k.enableCopy());
    }
    if (templateCommand != null) {
      TradeActKeeper.setCommandEnabled(templateCommand, k != null && k.enableTemplate());
    }
  }

  private void saveAsTemplate(String name) {
    IsRow row = getGridView().getActiveRow();

    if (row == null) {
      logger.severe(SVC_SAVE_ACT_AS_TEMPLATE, "act row not available");

    } else {
      ParameterList params = TradeActKeeper.createArgs(SVC_SAVE_ACT_AS_TEMPLATE);
      params.addDataItem(COL_TRADE_ACT, row.getId());
      params.addDataItem(COL_TA_TEMPLATE_NAME, name);

      BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          response.notify(getGridView());

          if (response.hasResponse(BeeRow.class)) {
            BeeRow template = BeeRow.restore(response.getResponseAsString());
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TRADE_ACT_TEMPLATES);

            RowEditor.open(VIEW_TRADE_ACT_TEMPLATES, template, Opener.MODAL);
          }
        }
      });
    }
  }
}
