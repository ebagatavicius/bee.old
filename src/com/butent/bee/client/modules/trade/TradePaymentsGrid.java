package com.butent.bee.client.modules.trade;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.event.logical.DataReceivedEvent;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.modules.trade.TradeDocumentSums;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TradePaymentsGrid extends AbstractGridInterceptor {

  private Supplier<TradeDocumentSums> tdsSupplier;
  private Consumer<Boolean> tdsListener;

  TradePaymentsGrid() {
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradePaymentsGrid();
  }

  void setTdsSupplier(Supplier<TradeDocumentSums> tdsSupplier) {
    this.tdsSupplier = tdsSupplier;
  }

  void setTdsListener(Consumer<Boolean> tdsListener) {
    this.tdsListener = tdsListener;
  }

  @Override
  public void onDataReceived(DataReceivedEvent event) {
    if (tdsSupplier != null && event != null) {
      if (getGridPresenter() != null && getGridPresenter().getUserFilter() == null) {
        tdsSupplier.get().clearPayments();
      }

      if (!BeeUtils.isEmpty(event.getRows())) {
        int index = getDataIndex(COL_TRADE_PAYMENT_AMOUNT);

        for (IsRow row : event.getRows()) {
          tdsSupplier.get().addPayment(row.getId(), row.getDouble(index));
        }
      }

      fireTdsChange(event.isInsert());
    }

    super.onDataReceived(event);
  }

  @Override
  public boolean previewCellUpdate(CellUpdateEvent event) {
    long id = event.getRowId();

    if (event.hasSource(COL_TRADE_PAYMENT_AMOUNT)
        && tdsSupplier != null && tdsSupplier.get().containsPayment(id)
        && tdsSupplier.get().updatePayment(id, BeeUtils.toDoubleOrNull(event.getValue()))) {

      fireTdsChange(true);
    }

    return super.previewCellUpdate(event);
  }

  @Override
  public boolean previewMultiDelete(MultiDeleteEvent event) {
    if (tdsSupplier != null) {
      boolean fire = false;

      for (long id : event.getRowIds()) {
        if (tdsSupplier.get().containsPayment(id)) {
          tdsSupplier.get().deletePayment(id);
          fire = true;
        }
      }

      if (fire) {
        fireTdsChange(true);
      }
    }

    return super.previewMultiDelete(event);
  }

  @Override
  public boolean previewRowDelete(RowDeleteEvent event) {
    if (tdsSupplier != null && tdsSupplier.get().containsPayment(event.getRowId())) {
      tdsSupplier.get().deletePayment(event.getRowId());
      fireTdsChange(true);
    }

    return super.previewRowDelete(event);
  }

  private void fireTdsChange(boolean update) {
    if (tdsListener != null) {
      tdsListener.accept(update);
    }
  }
}
