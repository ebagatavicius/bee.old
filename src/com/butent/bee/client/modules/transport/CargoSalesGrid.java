package com.butent.bee.client.modules.transport;

import com.google.common.collect.ImmutableMap;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.EditEndEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class CargoSalesGrid extends AbstractGridInterceptor implements ClickHandler {

  private UnboundSelector mainItem;

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    presenter.getHeader().clearCommandPanel();
    presenter.getHeader()
        .addCommandItem(new Button(Localized.getConstants().createInvoice(), this));
  }

  @Override
  public Map<String, Filter> getInitialParentFilters() {
    return ImmutableMap.of("pyp", Filter.isNull(COL_SALE));
  }

  @Override
  public GridInterceptor getInstance() {
    return new CargoSalesGrid();
  }

  @Override
  public void onClick(ClickEvent event) {
    final GridPresenter presenter = getGridPresenter();
    final Set<Long> ids = new HashSet<>();

    for (RowInfo row : presenter.getGridView().getSelectedRows(SelectedRows.ALL)) {
      ids.add(row.getId());
    }
    if (ids.isEmpty()) {
      presenter.getGridView().notifyWarning(Localized.getConstants().selectAtLeastOneRow());
      return;
    }
    Queries.getRowSet(VIEW_CARGO_SALES, null, Filter.idIn(ids), new RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        Set<String> orders = new HashSet<>();
        Set<String> vehicles = new HashSet<>();
        Set<String> drivers = new HashSet<>();
        Set<String> cargoNotes = new HashSet<>();

        Map<Long, Pair<String, Integer>> payers = new HashMap<>();
        Map<Long, String> customers = new HashMap<>();
        Map<Long, String> currencies = new HashMap<>();

        boolean itemEmpty = false;
        DataInfo info = Data.getDataInfo(VIEW_CARGO_SALES);

        int item = info.getColumnIndex(COL_ITEM);
        int order = info.getColumnIndex(COL_ORDER_NO);
        int vehicle = info.getColumnIndex(COL_VEHICLE);
        int trailer = info.getColumnIndex(COL_TRAILER);
        int forwarderVehicle = info.getColumnIndex(COL_FORWARDER_VEHICLE);
        int driver = info.getColumnIndex(COL_DRIVER);
        int forwarderDriver = info.getColumnIndex(COL_FORWARDER_DRIVER);
        int custId = info.getColumnIndex(COL_CUSTOMER);
        int custName = info.getColumnIndex(COL_CUSTOMER_NAME);
        int currId = info.getColumnIndex(COL_CURRENCY);
        int currName = info.getColumnIndex(COL_CURRENCY + COL_CURRENCY_NAME);
        int notes = info.getColumnIndex(COL_CARGO_NOTES);

        for (BeeRow row : result.getRows()) {
          if (!itemEmpty) {
            itemEmpty = row.getLong(item) == null;
          }
          orders.add(row.getString(order));
          vehicles.add(BeeUtils.notEmpty(BeeUtils.join("/", row.getString(vehicle),
              row.getString(trailer)), row.getString(forwarderVehicle)));
          drivers.add(BeeUtils.notEmpty(row.getString(driver), row.getString(forwarderDriver)));

          String name = null;
          Long id = null;

          for (String fld : new String[] {"Company", "Payer", "Customer"}) {
            name = fld;
            id = row.getLong(info.getColumnIndex(name));

            if (DataUtils.isId(id)) {
              break;
            }
          }
          if (DataUtils.isId(id)) {
            payers.put(id, Pair.of(row.getString(info.getColumnIndex(name + "Name")),
                row.getInteger(info.getColumnIndex(name + "CreditDays"))));
          }
          customers.put(row.getLong(custId), row.getString(custName));

          id = row.getLong(currId);

          if (DataUtils.isId(id)) {
            currencies.put(id, row.getString(currName));
          }
          cargoNotes.add(row.getString(notes));
        }
        final boolean mainRequired = itemEmpty;
        final DataInfo saleInfo = Data.getDataInfo(VIEW_CARGO_INVOICES);

        final BeeRow newRow = RowFactory.createEmptyRow(saleInfo, true);

        newRow.setValue(saleInfo.getColumnIndex(COL_NUMBER), BeeUtils.joinItems(orders));
        newRow.setValue(saleInfo.getColumnIndex(COL_VEHICLE), BeeUtils.joinItems(vehicles));
        newRow.setValue(saleInfo.getColumnIndex(COL_DRIVER), BeeUtils.joinItems(drivers));
        newRow.setValue(saleInfo.getColumnIndex(COL_CARGO_NOTES), BeeUtils.joinItems(cargoNotes));

        newRow.setValue(saleInfo.getColumnIndex(COL_TRADE_MANAGER),
            BeeKeeper.getUser().getUserId());
        newRow.setValue(saleInfo.getColumnIndex(COL_TRADE_MANAGER + COL_FIRST_NAME),
            BeeKeeper.getUser().getFirstName());
        newRow.setValue(saleInfo.getColumnIndex(COL_TRADE_MANAGER + COL_LAST_NAME),
            BeeKeeper.getUser().getLastName());

        if (customers.size() == 1) {
          for (Entry<Long, String> entry : customers.entrySet()) {
            newRow.setValue(saleInfo.getColumnIndex(COL_CUSTOMER), entry.getKey());
            newRow.setValue(saleInfo.getColumnIndex(COL_CUSTOMER_NAME), entry.getValue());
          }
        }
        final Holder<Integer> days = Holder.absent();

        if (payers.size() == 1) {
          for (Entry<Long, Pair<String, Integer>> entry : payers.entrySet()) {
            if (!Objects.equals(entry.getKey(),
                newRow.getLong(saleInfo.getColumnIndex(COL_CUSTOMER)))) {

              newRow.setValue(saleInfo.getColumnIndex(COL_PAYER), entry.getKey());
              newRow.setValue(saleInfo.getColumnIndex(COL_PAYER_NAME),
                  entry.getValue().getA());
            }
            days.set(entry.getValue().getB());

            if (BeeUtils.isPositive(days.get())) {
              newRow.setValue(saleInfo.getColumnIndex(COL_TRADE_TERM),
                  TimeUtils.nextDay(newRow.getDateTime(saleInfo.getColumnIndex(COL_DATE)),
                      days.get()));
            }
          }
        }
        if (currencies.size() == 1) {
          for (Entry<Long, String> entry : currencies.entrySet()) {
            newRow.setValue(saleInfo.getColumnIndex(COL_CURRENCY), entry.getKey());
            newRow.setValue(saleInfo.getColumnIndex(COL_CURRENCY + COL_CURRENCY_NAME),
                entry.getValue());
          }
        }
        Global.getParameter(PRM_INVOICE_PREFIX, new Consumer<String>() {
          @Override
          public void accept(String prefix) {
            newRow.setValue(saleInfo.getColumnIndex(COL_TRADE_INVOICE_PREFIX), prefix);

            RowFactory.createRow(FORM_NEW_CARGO_INVOICE, null, saleInfo, newRow, null,
                new AbstractFormInterceptor() {
                  @Override
                  public FormInterceptor getInstance() {
                    return this;
                  }

                  @Override
                  public void onEditEnd(EditEndEvent ev, Object source) {
                    if (ev.getColumn() == null
                        || !BeeUtils.same(ev.getColumn().getId(), COL_DATE)) {
                      return;
                    }
                    if (BeeUtils.isPositive(days.get())) {
                      DateTime dt = new DateTime(BeeUtils.toLong(ev.getNewValue()));

                      getFormView().getActiveRow()
                          .setValue(saleInfo.getColumnIndex(COL_TRADE_TERM),
                              TimeUtils.nextDay(dt, days.get()));

                      getFormView().refreshBySource(COL_TRADE_TERM);
                    }
                  }

                  @Override
                  public void onStart(FormView form) {
                    Widget w = form.getWidgetByName("MainItem");

                    if (w != null && w instanceof UnboundSelector) {
                      mainItem = (UnboundSelector) w;

                      if (mainRequired) {
                        mainItem.setNullable(false);
                        w = form.getWidgetByName("MainItemCaption");

                        if (w != null) {
                          w.addStyleName(StyleUtils.NAME_REQUIRED);
                        }
                      }
                    }
                  }
                },
                new RowCallback() {
                  @Override
                  public void onCancel() {
                    mainItem = null;
                  }

                  @Override
                  public void onSuccess(final BeeRow row) {
                    ParameterList args = TransportHandler.createArgs(SVC_CREATE_INVOICE_ITEMS);
                    args.addDataItem(COL_SALE, row.getId());
                    args.addDataItem(COL_CURRENCY,
                        row.getLong(saleInfo.getColumnIndex(COL_CURRENCY)));
                    args.addDataItem(Service.VAR_ID, DataUtils.buildIdList(ids));

                    if (mainItem != null && DataUtils.isId(mainItem.getRelatedId())) {
                      args.addDataItem(COL_ITEM, mainItem.getRelatedId());
                    }
                    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
                      @Override
                      public void onResponse(ResponseObject response) {
                        response.notify(presenter.getGridView());

                        if (response.hasErrors()) {
                          return;
                        }
                        Popup popup = UiHelper.getParentPopup(presenter.getGridView().getGrid());

                        if (popup != null) {
                          popup.close();
                        }
                        Data.onViewChange(presenter.getViewName(),
                            DataChangeEvent.CANCEL_RESET_REFRESH);
                        RowEditor.openForm(FORM_CARGO_INVOICE, saleInfo, row.getId(), Opener.MODAL);
                      }
                    });
                    onCancel();
                  }
                });
          }
        });
      }
    });
  }
}
