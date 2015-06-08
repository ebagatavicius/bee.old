package com.butent.bee.client.modules.transport;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_CURRENCY;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.event.logical.MutationEvent;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.modules.transport.TransportHandler.Profit;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditEndEvent;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.IntegerLabel;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Objects;

class OrderCargoForm extends AbstractFormInterceptor implements SelectorEvent.Handler {

  static void preload(final ScheduledCommand command) {
    Global.getParameter(PRM_CARGO_TYPE, new Consumer<String>() {
      @Override
      public void accept(String input) {
        if (DataUtils.isId(input)) {
          Queries.getRow(VIEW_CARGO_TYPES, BeeUtils.toLong(input), new RowCallback() {
            @Override
            public void onFailure(String... reason) {
              super.onFailure(reason);
              defaultCargoType = null;
              command.execute();
            }

            @Override
            public void onSuccess(BeeRow result) {
              defaultCargoType = result;
              command.execute();
            }
          });

        } else {
          defaultCargoType = null;
          command.execute();
        }
      }
    });
  }

  private static IsRow defaultCargoType;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, COL_CURRENCY) && widget instanceof DataSelector) {
      final DataSelector selector = (DataSelector) widget;

      selector.addEditStopHandler(new EditStopEvent.Handler() {
        @Override
        public void onEditStop(EditStopEvent event) {
          if (event.isChanged()) {
            refresh(BeeUtils.toLongOrNull(selector.getNormalizedValue()));
          }
        }
      });

    } else if (widget instanceof ChildGrid && BeeUtils.same(name, TBL_CARGO_INCOMES)) {
      final String viewName = getViewName();

      ((ChildGrid) widget).setGridInterceptor(new AbstractGridInterceptor() {
        @Override
        public void afterDeleteRow(long rowId) {
          refresh(Data.getLong(viewName, getActiveRow(), COL_CURRENCY));
        }

        @Override
        public void afterInsertRow(IsRow result) {
          refresh(Data.getLong(viewName, getActiveRow(), COL_CURRENCY));
        }

        @Override
        public void afterUpdateCell(IsColumn column, String oldValue, String newValue,
            IsRow result, boolean rowMode) {
          if (BeeUtils.inListSame(column.getId(), COL_DATE, COL_AMOUNT, COL_CURRENCY,
              COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)) {
            refresh(Data.getLong(viewName, getActiveRow(), COL_CURRENCY));
          }
        }

        @Override
        public GridInterceptor getInstance() {
          return null;
        }
      });

    } else if (widget instanceof ChildGrid && BeeUtils.same(name, TBL_CARGO_EXPENSES)) {
      ((ChildGrid) widget).setGridInterceptor(new AbstractGridInterceptor() {

        @Override
        public void afterCreateEditor(String source, Editor editor, boolean embedded) {
          if (BeeUtils.same(source, COL_CARGO_INCOME) && editor instanceof DataSelector) {
            ((DataSelector) editor).addSelectorHandler(OrderCargoForm.this);
          }
          super.afterCreateEditor(source, editor, embedded);
        }

        @Override
        public ColumnDescription beforeCreateColumn(GridView gridView, ColumnDescription descr) {
          if (!TransportHandler.bindExpensesToIncomes
              && Objects.equals(descr.getId(), COL_CARGO_INCOME)) {
            return null;
          }
          return super.beforeCreateColumn(gridView, descr);
        }

        @Override
        public GridInterceptor getInstance() {
          return null;
        }
      });

    } else if (widget instanceof ChildGrid && VIEW_CARGO_HANDLING.equals(name)) {
      ((ChildGrid) widget).addReadyHandler(new ReadyEvent.Handler() {
        @Override
        public void onReady(ReadyEvent re) {
          GridView gridView = ViewHelper.getChildGrid(getFormView(), VIEW_CARGO_HANDLING);

          if (gridView != null) {
            gridView.getGrid().addMutationHandler(new MutationEvent.Handler() {
              @Override
              public void onMutation(MutationEvent mu) {
                refreshKilometers(getActiveRow(), null, null);
              }
            });
          }
        }
      });

    } else if (widget instanceof InputBoolean
        && (BeeUtils.inListSame(name, "Partial", "Outsized"))) {
      ((InputBoolean) widget).addValueChangeHandler(new ValueChangeHandler<String>() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
          refreshMetrics(getCheckCount(getFormView()) > 0);
        }
      });
    }
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    refresh(row.getLong(form.getDataIndex(COL_CURRENCY)));
    refreshMetrics(BeeUtils.unbox(row.getBoolean(form.getDataIndex("Partial")))
        || BeeUtils.unbox(row.getBoolean(form.getDataIndex("Outsized"))));
    refreshKilometers(row, null, null);

    Widget cmrWidget = form.getWidgetBySource(COL_CARGO_CMR);
    if (cmrWidget instanceof DataSelector) {
      Filter filter;

      if (DataUtils.hasId(row)) {
        filter = Filter.in(Data.getIdColumn(DocumentConstants.VIEW_DOCUMENTS),
            DocumentConstants.VIEW_RELATED_DOCUMENTS, DocumentConstants.COL_DOCUMENT,
            Filter.equals(COL_CARGO, row.getId()));
      } else {
        filter = Filter.isFalse();
      }

      ((DataSelector) cmrWidget).setAdditionalFilter(filter);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new OrderCargoForm();
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (BeeUtils.same(event.getRelatedViewName(), TBL_CARGO_INCOMES) && event.isOpened()) {
      event.getSelector().setAdditionalFilter(Filter.equals(COL_CARGO, getActiveRowId()));
    }
  }

  @Override
  public void onEditEnd(EditEndEvent event, Object source) {
    String colId = event.getColumn().getId();
    if ((COL_EMPTY_KILOMETERS.equals(colId) || COL_LOADED_KILOMETERS.equals(colId))
        && event.valueChanged()) {
      refreshKilometers(getActiveRow(), colId, BeeUtils.toIntOrNull(event.getNewValue()));
    }
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    if (Data.isViewEditable(VIEW_CARGO_INVOICES)) {
      header.addCommandItem(new InvoiceCreator(Filter.equals(COL_CARGO, row.getId())));
    }
    header.addCommandItem(new Profit(COL_CARGO, row.getId()));

    return true;
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    form.getViewPresenter().getHeader().clearCommandPanel();

    if (defaultCargoType != null) {
      RelationUtils.updateRow(Data.getDataInfo(form.getViewName()), COL_CARGO_TYPE, newRow,
          Data.getDataInfo(VIEW_CARGO_TYPES), defaultCargoType, true);
    }
  }

  private static int getCheckCount(FormView form) {
    int checkBoxObserved = 0;

    InputBoolean ib1 = (InputBoolean) form.getWidgetByName("Partial");
    InputBoolean ib2 = (InputBoolean) form.getWidgetByName("Outsized");

    if (ib1 != null) {
      if (BeeUtils.unbox(BeeUtils.toBooleanOrNull(ib1.getValue()))) {
        checkBoxObserved = checkBoxObserved + 1;
      }
    }

    if (ib2 != null) {
      if (BeeUtils.unbox(BeeUtils.toBooleanOrNull(ib2.getValue()))) {
        checkBoxObserved = checkBoxObserved + 1;
      }
    }

    return checkBoxObserved;
  }

  private void refresh(Long currency) {
    final FormView form = getFormView();
    final Widget widget = form.getWidgetByName(COL_AMOUNT);

    if (widget != null) {
      widget.getElement().setInnerText(null);

      if (!DataUtils.isId(getActiveRow().getId())) {
        return;
      }
      ParameterList args = TransportHandler.createArgs(SVC_GET_CARGO_TOTAL);
      args.addDataItem(COL_CARGO, getActiveRow().getId());

      if (DataUtils.isId(currency)) {
        args.addDataItem(COL_CURRENCY, currency);
      }
      BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          response.notify(form);

          if (response.hasErrors()) {
            return;
          }
          widget.getElement().setInnerText(response.getResponseAsString());
        }
      });
    }
  }

  private void refreshKilometers(IsRow row, String colId, Integer km) {
    if (row == null) {
      return;
    }

    Integer emptyKm = COL_EMPTY_KILOMETERS.equals(colId)
        ? km : getIntegerValue(COL_EMPTY_KILOMETERS);
    Integer loadedKm = COL_LOADED_KILOMETERS.equals(colId)
        ? km : getIntegerValue(COL_LOADED_KILOMETERS);

    if (DataUtils.hasId(row)) {
      GridView grid = ViewHelper.getChildGrid(getFormView(), VIEW_CARGO_HANDLING);

      if (grid != null && !grid.isEmpty()) {
        List<? extends IsRow> childRows = grid.getRowData();
        int emptyKmIndex = grid.getDataIndex(COL_EMPTY_KILOMETERS);
        int loadedKmIndex = grid.getDataIndex(COL_LOADED_KILOMETERS);

        for (IsRow childRow : childRows) {
          Integer v = childRow.getInteger(emptyKmIndex);
          if (v != null) {
            if (emptyKm == null) {
              emptyKm = v;
            } else {
              emptyKm += v;
            }
          }

          v = childRow.getInteger(loadedKmIndex);
          if (v != null) {
            if (loadedKm == null) {
              loadedKm = v;
            } else {
              loadedKm += v;
            }
          }
        }
      }
    }

    Widget widget = getFormView().getWidgetByName("TotalEmpty");
    if (widget instanceof IntegerLabel) {
      ((IntegerLabel) widget).setValue(emptyKm);
    }

    widget = getFormView().getWidgetByName("TotalLoaded");
    if (widget instanceof IntegerLabel) {
      ((IntegerLabel) widget).setValue(loadedKm);
    }
  }

  private void refreshMetrics(boolean on) {
    Widget widget = getFormView().getWidgetByName("Metrics");

    if (widget != null) {
      widget.setVisible(on);
    }
  }
}