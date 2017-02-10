package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;

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
      if (!DataUtils.isEmpty(result)) {
        Collection<String> items = RelationUtils.updateRow(Data.getDataInfo(formView.getViewName()),
            targetColumn, newRow, Data.getDataInfo(result.getViewName()), result.getRow(0), true);

        if (refresh) {
          for (String item : items) {
            formView.refreshBySource(item);
          }
        }
      }
    }

    private void setRefresh(boolean refresh) {
      this.refresh = refresh;
    }
  }

  static void setDefaultExpeditionType(FormView form, IsRow newRow, String targetColumn) {
    RelatedValuesCallback callback = new RelatedValuesCallback(form, newRow, targetColumn);

    int rpcId = Queries.getRowSet(VIEW_EXPEDITION_TYPES, null, Filter.notNull(COL_SELF_SERVICE),
        Order.ascending(COL_SELF_SERVICE, COL_EXPEDITION_TYPE_NAME), 0, 1, CachingPolicy.FULL,
        callback);

    if (!Queries.isResponseFromCache(rpcId)) {
      callback.setRefresh(true);
    }
  }

  static void setDefaultPerson(FormView form, IsRow newRow, String targetColumn) {
    RelatedValuesCallback callback = new RelatedValuesCallback(form, newRow, targetColumn);

    int rpcId = Queries.getRowSet(ClassifierConstants.VIEW_COMPANY_PERSONS, null,
        Filter.compareId(BeeKeeper.getUser().getUserData().getCompanyPerson()), null,
        CachingPolicy.FULL, callback);

    if (!Queries.isResponseFromCache(rpcId)) {
      callback.setRefresh(true);
    }
  }

  static void setDefaultShippingTerm(FormView form, IsRow newRow, String targetColumn) {
    RelatedValuesCallback callback = new RelatedValuesCallback(form, newRow, targetColumn);

    int rpcId = Queries.getRowSet(VIEW_SHIPPING_TERMS, null, Filter.notNull(COL_SELF_SERVICE),
        Order.ascending(COL_SELF_SERVICE, COL_SHIPPING_TERM_NAME), 0, 1, CachingPolicy.FULL,
        callback);

    if (!Queries.isResponseFromCache(rpcId)) {
      callback.setRefresh(true);
    }
  }

  static void updateStatus(FormView form, String column, Enum<?> status) {
    BeeRow row = DataUtils.cloneRow(form.getActiveRow());
    row.setValue(form.getDataIndex(column), status.ordinal());

    update(form, DataUtils.getUpdated(form.getViewName(), form.getDataColumns(), form.getOldRow(),
        row, form.getChildrenForUpdate()));
  }

  static void update(FormView form, BeeRowSet rowSet) {
    if (!DataUtils.isEmpty(rowSet)) {
      Queries.updateRow(rowSet, new RowUpdateCallback(form.getViewName()) {
        @Override
        public void onSuccess(BeeRow result) {
          if (DataUtils.sameId(result, form.getActiveRow()) && !form.observesData()) {
            form.updateRow(result, false);
          }
          super.onSuccess(result);
        }
      });
    }
  }

  private SelfServiceUtils() {
  }
}
