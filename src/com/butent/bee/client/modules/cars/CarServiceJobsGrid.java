package com.butent.bee.client.modules.cars;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_CURRENCY;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_ITEM;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.COL_MODEL;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.ParentRowRefreshGrid;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CarServiceJobsGrid extends ParentRowRefreshGrid implements SelectorEvent.Handler {

  @Override
  public void afterCreateEditor(String source, Editor editor, boolean embedded) {
    if (editor instanceof DataSelector && ((DataSelector) editor).hasRelatedView(TBL_CAR_JOBS)) {
      ((DataSelector) editor).addSelectorHandler(this);
    }
    super.afterCreateEditor(source, editor, embedded);
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    HeaderView header = presenter.getHeader();

    if (Objects.nonNull(header) && BeeKeeper.getUser().canEditData(getViewName())) {
      header.addCommandItem(new PriceRecalculator(this));
    }
    super.afterCreatePresenter(presenter);
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    FormView parentForm = ViewHelper.getForm(getGridView());

    if (Objects.isNull(parentForm)) {
      return;
    }
    if (event.isOpened()) {
      Filter filter = Filter.isNull(COL_MODEL);

      if (DataUtils.isId(parentForm.getLongValue(COL_MODEL))) {
        filter = Filter.or(filter, Filter.equals(COL_MODEL, parentForm.getLongValue(COL_MODEL)));
      }
      event.getSelector().setAdditionalFilter(filter);

    } else if (event.isChangePending()) {
      String srcView = event.getRelatedViewName();
      IsRow srcRow = event.getRelatedRow();
      if (srcRow == null) {
        return;
      }
      DataView dataView = ViewHelper.getDataView(event.getSelector());
      if (dataView == null || BeeUtils.isEmpty(dataView.getViewName()) || !dataView.isFlushable()) {
        return;
      }
      IsRow target = dataView.getActiveRow();
      if (target == null) {
        return;
      }
      Map<String, String> values = new HashMap<>();

      values.put(COL_DURATION, BeeUtils.nvl(Data.getString(srcView, srcRow,
          COL_MODEL + COL_DURATION), Data.getString(srcView, srcRow, COL_DURATION)));

      PriceRecalculator.getPriceAndDiscount(getGridView(), 1, (idx, info) -> {
            switch (info) {
              case ITEM:
                return Data.getString(srcView, srcRow, COL_ITEM);
              case PRICE:
                return BeeUtils.nvl(Data.getString(srcView, srcRow, COL_MODEL + COL_PRICE),
                    Data.getString(srcView, srcRow, COL_PRICE));
              case CURRENCY:
                return BeeUtils.nvl(Data.getString(srcView, srcRow, COL_MODEL + COL_CURRENCY),
                    Data.getString(srcView, srcRow, COL_CURRENCY));
              default:
                return null;
            }
          },
          (idx, price, discount) -> {
            values.put(COL_PRICE, BeeUtils.toStringOrNull(price));
            values.put(COL_TRADE_DOCUMENT_ITEM_DISCOUNT, BeeUtils.toStringOrNull(discount));
            values.put(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT,
                BeeUtils.toString(BeeUtils.isPositive(discount)));
          },
          () -> values.forEach((col, val) -> {
            target.setValue(dataView.getDataIndex(col), val);
            dataView.refreshBySource(col);
          }));
    }
  }
}
