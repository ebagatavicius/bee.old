package com.butent.bee.client.modules.transport;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.modules.transport.TransportHandler.Profit;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.utils.BeeUtils;

class OrderCargoForm extends AbstractFormInterceptor {

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
        public void afterUpdateCell(IsColumn column, IsRow result, boolean rowMode) {
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
  }

  @Override
  public FormInterceptor getInstance() {
    return new OrderCargoForm();
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

  private void refreshMetrics(boolean on) {
    Widget widget = getFormView().getWidgetByName("Metrics");

    if (widget != null) {
      widget.setVisible(on);
    }
  }
}