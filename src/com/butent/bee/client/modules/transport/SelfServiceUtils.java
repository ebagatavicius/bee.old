package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;

import java.util.Collection;

final class SelfServiceUtils {

  private static final class RelatedValuesCallback extends Queries.RowSetCallback {
    private final FormView formView;
    private final IsRow newRow;
    private final String targetColumn;

    private boolean refresh;

    private RelatedValuesCallback(FormView formView, IsRow newRow, String targetColumn) {
      this.formView = formView;
      this.newRow = newRow;
      this.targetColumn = targetColumn;
    }

    @Override
    public void onSuccess(BeeRowSet result) {
      IsRow row = refresh ? formView.getActiveRow() : newRow;

      if (!DataUtils.isEmpty(result) && DataUtils.isNewRow(row)) {
        RelationUtils.updateRow(Data.getDataInfo(formView.getViewName()), targetColumn, row,
            Data.getDataInfo(result.getViewName()), result.getRow(0), true);

        if (refresh) {
          formView.refreshBySource(targetColumn);
        }
      }
    }

    private void setRefresh(boolean refresh) {
      this.refresh = refresh;
    }
  }

  static void sendFiles(Long reqId, Collection<FileInfo> files) {
    FileUtils.commitFiles(files, VIEW_CARGO_REQUEST_FILES, COL_CRF_REQUEST, reqId,
        AdministrationConstants.COL_FILE, AdministrationConstants.COL_FILE_CAPTION);
  }

  static void setDefaultExpeditionType(FormView form, IsRow newRow, String targetColumn) {
    Filter filter = Filter.notNull(COL_EXPEDITION_TYPE_SELF_SERVICE);
    Order order = Order.ascending(COL_EXPEDITION_TYPE_SELF_SERVICE, COL_EXPEDITION_TYPE_NAME);

    RelatedValuesCallback callback = new RelatedValuesCallback(form, newRow, targetColumn);

    int rpcId = Queries.getRowSet(VIEW_EXPEDITION_TYPES, null, filter, order, 0, 1,
        CachingPolicy.FULL, callback);
    if (!Queries.isResponseFromCache(rpcId)) {
      callback.setRefresh(true);
    }
  }

  static void setDefaultShippingTerm(FormView form, IsRow newRow, String targetColumn) {
    Filter filter = Filter.notNull(COL_SHIPPING_TERM_SELF_SERVICE);
    Order order = Order.ascending(COL_SHIPPING_TERM_SELF_SERVICE, COL_SHIPPING_TERM_NAME);

    RelatedValuesCallback callback = new RelatedValuesCallback(form, newRow, targetColumn);

    int rpcId = Queries.getRowSet(VIEW_SHIPPING_TERMS, null, filter, order, 0, 1,
        CachingPolicy.FULL, callback);
    if (!Queries.isResponseFromCache(rpcId)) {
      callback.setRefresh(true);
    }
  }

  static void updateStatus(final FormView form, String column, final Enum<?> status) {
    BeeRow row = DataUtils.cloneRow(form.getActiveRow());
    row.setValue(form.getDataIndex(column), status.ordinal());

    Queries.update(form.getViewName(), form.getDataColumns(), form.getOldRow(), row,
        form.getChildrenForUpdate(), new RowCallback() {
          @Override
          public void onFailure(String... reason) {
            form.notifySevere(reason);
          }

          @Override
          public void onSuccess(BeeRow result) {
            if (DataUtils.sameId(result, form.getActiveRow()) && !form.observesData()) {
              form.updateRow(result, false);
            }
            RowUpdateEvent.fire(BeeKeeper.getBus(), form.getViewName(), result);
          }
        });
  }

  private SelfServiceUtils() {
  }
}
