package com.butent.bee.client.modules.orders;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.orders.OrdersConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.validation.CellValidation;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.orders.OrdersConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OrderInvoiceBuilder extends AbstractGridInterceptor implements ClickHandler {

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {

    Button amount = new Button(Localized.dictionary().amount());
    amount.addClickHandler(this::getItemsAmount);
    presenter.getHeader().addCommandItem(amount);
    presenter.getHeader().addCommandItem(new Button(Localized.dictionary().createInvoice(),
        this));
  }

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if (BeeUtils.same(columnName, COL_COMPLETED_QTY)) {
      editableColumn.addCellValidationHandler(event -> {
        CellValidation cv = event.getCellValidation();
        IsRow row = cv.getRow();
        int qtyIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_TRADE_ITEM_QUANTITY);
        int resRemainderIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_RESERVED_REMAINDER);
        Double qty = row.getDouble(qtyIdx);
        Double free = Double.valueOf(row.getProperty(PRP_FREE_REMAINDER));
        Double compInvc = Double.valueOf(row.getProperty(PRP_COMPLETED_INVOICES));
        Double resRemainder =
            (row.getDouble(resRemainderIdx) == null) ? 0 : row.getDouble(resRemainderIdx);
        Double newValue = BeeUtils.toDouble(cv.getNewValue());
        Double complQty = null;

        if (BeeUtils.isPositive(newValue)) {
          if (qty - compInvc <= free + resRemainder) {
            complQty = qty - compInvc;
          } else if (qty - compInvc > free + resRemainder) {
            complQty = free + resRemainder;
          }

          if (complQty < newValue) {
            getGridPresenter().getGridView().notifySevere(
                Localized.dictionary().maxValue() + " " + complQty);
            return false;
          }
        } else {
          getGridPresenter().getGridView().notifySevere(
              Localized.dictionary().minValue() + " 1");
          return false;
        }
        return true;
      });
    }
    return super.afterCreateColumn(columnName, dataColumns, column, header, footer, editableColumn);
  }

  @Override
  public void beforeRender(GridView gridView, RenderingEvent event) {

    int qtyIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_TRADE_ITEM_QUANTITY);
    int resRemainderIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_RESERVED_REMAINDER);
    int comQtyIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_COMPLETED_QTY);
    int itemIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_ITEM);

    Map<Long, Double> freeMap = new HashMap<>();

    for (IsRow row : getGridView().getRowData()) {
      freeMap.put(row.getLong(itemIdx), Double.valueOf(row.getProperty(PRP_FREE_REMAINDER)));
    }

    for (IsRow row : getGridView().getRowData()) {
      Double qty = row.getDouble(qtyIdx);
      Double free = freeMap.get(row.getLong(itemIdx));
      Double compInvc = Double.valueOf(row.getProperty(PRP_COMPLETED_INVOICES));
      double resRemainder = BeeUtils.unbox(row.getDouble(resRemainderIdx));

      if (qty - compInvc <= free + resRemainder) {
        row.setValue(comQtyIdx, qty - compInvc);
        freeMap.put(row.getLong(itemIdx), free - qty + compInvc + resRemainder);
      } else if (qty - compInvc > free + resRemainder) {
        row.setValue(comQtyIdx, free + resRemainder);
        freeMap.put(row.getLong(itemIdx), BeeConst.DOUBLE_ZERO);
      }
    }
  }

  @Override
  public void onClick(ClickEvent clickEvent) {
    final Set<Long> ids = new HashSet<>();
    int comQtyIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_COMPLETED_QTY);
    BeeRowSet rowSet = new BeeRowSet(VIEW_ORDER_SALES, Data.getColumns(VIEW_ORDER_SALES));

    for (RowInfo row : getGridView().getSelectedRows(GridView.SelectedRows.ALL)) {
      ids.add(row.getId());

    }

    if (ids.isEmpty()) {
      getGridView().notifyWarning(Localized.dictionary().selectAtLeastOneRow());
      return;
    }

    for (IsRow row : getGridView().getRowData()) {
      if (ids.contains(row.getId())) {
        Double comQty = row.getDouble(comQtyIdx);

        if (BeeUtils.isPositive(comQty)) {
          rowSet.addRow((BeeRow) row);

        } else {
          getGridView().notifySevere(Localized.dictionary().ordEmptyFreeRemainder());
          return;
        }
      }
    }

    createInvoice(rowSet);
  }

  @Override
  public GridInterceptor getInstance() {
    return new OrderInvoiceBuilder();
  }

  private static ParameterList getRequestArgs() {
    return OrdersKeeper.createSvcArgs(OrdersConstants.SVC_CREATE_INVOICE_ITEMS);
  }

  private void createInvoice(final BeeRowSet data) {

    final DataInfo targetInfo = Data.getDataInfo(VIEW_ORDER_CHILD_INVOICES);
    final BeeRow newRow = RowFactory.createEmptyRow(targetInfo, true);

    if (data != null) {
      newRow.setValue(targetInfo.getColumnIndex(TradeConstants.COL_TRADE_CUSTOMER), data.getRow(0)
          .getLong(Data.getColumnIndex(VIEW_ORDER_SALES, COL_COMPANY)));
      newRow.setValue(targetInfo.getColumnIndex(ALS_CUSTOMER_NAME), data.getRow(0)
          .getString(Data.getColumnIndex(VIEW_ORDER_SALES, ALS_COMPANY_NAME)));

      newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_WAREHOUSE_FROM), data.getRow(0)
          .getLong(Data.getColumnIndex(VIEW_ORDER_SALES, COL_WAREHOUSE)));
      newRow.setValue(targetInfo.getColumnIndex("WarehouseFromCode"), data.getRow(0)
          .getString(Data.getColumnIndex(VIEW_ORDER_SALES, ALS_WAREHOUSE_CODE)));

      newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_OPERATION), data.getRow(0)
          .getLong(Data.getColumnIndex(VIEW_ORDER_SALES, COL_ORDER_TRADE_OPERATION)));
      newRow.setValue(targetInfo.getColumnIndex(COL_OPERATION_NAME), data.getRow(0)
          .getString(Data.getColumnIndex(VIEW_ORDER_SALES, COL_TRADE_OPERATION_NAME)));
      newRow.setValue(targetInfo.getColumnIndex(COL_OPERATION_CASH_REGISTER_NO), data.getRow(0)
          .getString(Data.getColumnIndex(VIEW_ORDER_SALES, COL_OPERATION_CASH_REGISTER_NO)));

      Integer creditDays =
          data.getRow(0).getInteger(Data.getColumnIndex(VIEW_ORDER_SALES, COL_COMPANY_CREDIT_DAYS));

      if (creditDays != null) {
        newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_TERM), new JustDate(TimeUtils.today()
            .getDays() + creditDays));
      }

      String notes =
          data.getRow(0).getString(Data.getColumnIndex(VIEW_ORDER_SALES, COL_TRADE_NOTES));

      if (!BeeUtils.isEmpty(notes)) {
        newRow.setValue(targetInfo.getColumnIndex(COL_TRADE_NOTES), notes);
      }
    }
    newRow.setValue(targetInfo.getColumnIndex(TradeConstants.COL_TRADE_SUPPLIER), BeeKeeper
        .getUser().getCompany());

    newRow.setValue(targetInfo.getColumnIndex(ALS_SUPPLIER_NAME), BeeKeeper.getUser()
            .getCompanyName());

    newRow.setValue(targetInfo.getColumnIndex(TradeConstants.COL_TRADE_MANAGER), BeeKeeper
        .getUser().getUserId());

    newRow.setValue(targetInfo.getColumnIndex(TradeConstants.COL_TRADE_MANAGER
        + ClassifierConstants.COL_FIRST_NAME), BeeKeeper.getUser().getFirstName());

    newRow.setValue(targetInfo.getColumnIndex(TradeConstants.COL_TRADE_MANAGER
        + ClassifierConstants.COL_LAST_NAME), BeeKeeper.getUser().getLastName());

    getInvoiceItems(data, newRow);
  }

  private void getInvoiceItems(final BeeRowSet data, BeeRow newRow) {
    final DataInfo dataInfo = Data.getDataInfo(VIEW_ORDER_CHILD_INVOICES);
    int item = DataUtils.getColumnIndex(ClassifierConstants.COL_ITEM, data.getColumns());
    boolean itemAbsent = BeeConst.isUndef(item);
    final Map<Long, Double> idsQty = new HashMap<>();

    if (!itemAbsent) {
      for (BeeRow row : data) {
        idsQty.put(row.getId(), row.getDouble(Data.getColumnIndex(VIEW_ORDER_SALES,
            COL_COMPLETED_QTY)));
        if (!DataUtils.isId(row.getLong(item))) {
          itemAbsent = true;
          break;
        }
      }
    }
    final Holder<Long> mainItem;

    if (itemAbsent) {
      mainItem = Holder.absent();
    } else {
      mainItem = null;
    }
    RowFactory.createRow(dataInfo.getNewRowForm(), dataInfo.getNewRowCaption(), dataInfo,
        newRow, null, null, null, null, new RowCallback() {
          @Override
          public void onSuccess(final BeeRow row) {
            ParameterList args = getRequestArgs();

            if (args != null) {
              Map<String, String> params = new HashMap<>();

              params.put(Service.VAR_TABLE, Data.getViewTable(getViewName()));
              params.put(COL_SALE, String.valueOf(row.getId()));
              params.put(Service.VAR_DATA, Codec.beeSerialize(idsQty));
              params.put(COL_CURRENCY, row.getString(dataInfo.getColumnIndex(COL_CURRENCY)));

              if (mainItem != null && DataUtils.isId(mainItem.get())) {
                params.put(ClassifierConstants.COL_ITEM, BeeUtils.toString(mainItem.get()));
              }
              for (String prm : params.keySet()) {
                if (!args.hasParameter(prm)) {
                  args.addDataItem(prm, params.get(prm));
                }
              }
              BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
                @Override
                public void onResponse(ResponseObject response) {
                  response.notify(getGridView());

                  if (!response.hasErrors()) {
                    Popup popup = UiHelper.getParentPopup(getGridView().getGrid());

                    if (popup != null) {
                      popup.close();
                    }
                    Data.onViewChange(getViewName(), DataChangeEvent.RESET_REFRESH);
                    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_ORDER_CHILD_INVOICES);
                    RowEditor.openForm(dataInfo.getEditForm(), dataInfo, Filter.compareId(row
                        .getId()), Opener.MODAL);
                  }
                }
              });
            }
          }
        });
  }

  private void getItemsAmount(ClickEvent event) {
    final Set<Long> ids = new HashSet<>();
    int comQtyIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_COMPLETED_QTY);
    int priceIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_ITEM_PRICE);
    int discountIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_TRADE_DISCOUNT);
    int vatPlusIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_TRADE_VAT_PLUS);
    int vatIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_TRADE_VAT);
    int vatPrcIdx = Data.getColumnIndex(VIEW_ORDER_SALES, COL_TRADE_VAT_PERC);

    double totalAmount = 0;

    for (RowInfo row : getGridView().getSelectedRows(GridView.SelectedRows.ALL)) {
      ids.add(row.getId());
    }

    if (ids.isEmpty()) {
      getGridView().notifyWarning(Localized.dictionary().selectAtLeastOneRow());
      return;
    }

    for (IsRow row : getGridView().getRowData()) {
      if (ids.contains(row.getId())) {
        double comQty = BeeUtils.unbox(row.getDouble(comQtyIdx));
        double discount = BeeUtils.unbox(row.getDouble(discountIdx));
        boolean vatPlus = BeeUtils.unbox(row.getBoolean(vatPlusIdx));
        double vat = 0;
        double price = BeeUtils.unbox(row.getDouble(priceIdx));

        double realPrice = price - price * discount / 100;

        if (vatPlus) {
          vat = BeeUtils.unbox(row.getDouble(vatIdx));

          if (BeeUtils.unbox(row.getBoolean(vatPrcIdx))) {
            vat = realPrice * vat / 100;
          } else {
            vat = vat / comQty;
          }
        }
        totalAmount += comQty * (vat + realPrice);
      }
    }

    NumberFormat formater = NumberFormat.getFormat("0.00");
    final Popup popup = new Popup(OutsideClick.CLOSE);
    popup.add(new Label(Localized.dictionary().amount() + ": " + formater.format(totalAmount)
        + " EUR"));
    popup.showRelativeTo(event.getRelativeElement());
  }
}
