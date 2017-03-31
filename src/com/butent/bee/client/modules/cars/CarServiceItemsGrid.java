package com.butent.bee.client.modules.cars;

import com.google.gwt.core.client.Scheduler;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.DataChangeCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.modules.classifiers.ClassifierKeeper;
import com.butent.bee.client.modules.trade.TradeItemPicker;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.ParentRowRefreshGrid;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDiscountMode;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeVatMode;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class CarServiceItemsGrid extends ParentRowRefreshGrid {

  private Button recalc = new Button(Localized.dictionary().recalculateTradeItemPriceCaption(),
      (Scheduler.ScheduledCommand) this::recalculatePrice) {
    @Override
    public void setEnabled(boolean enabled) {
      setStyleName(StyleUtils.NAME_DISABLED, !enabled);
      super.setEnabled(enabled);
    }
  };

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    if (presenter != null && presenter.getHeader() != null
        && BeeKeeper.getUser().canEditData(getViewName())) {

      presenter.getHeader().addCommandItem(recalc);
    }
    super.afterCreatePresenter(presenter);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    presenter.getGridView().ensureRelId(docId -> {
      FormView parentForm = ViewHelper.getForm(getGridView());

      OperationType operationType = OperationType.SALE;
      TradeVatMode vatMode = EnumUtils.getEnumByIndex(TradeVatMode.class,
          parentForm.getIntegerValue(COL_TRADE_DOCUMENT_VAT_MODE));

      Map<String, String> options = new HashMap<>();
      options.put(COL_DISCOUNT_COMPANY, parentForm.getStringValue(COL_CUSTOMER));
      options.put(Service.VAR_TIME, parentForm.getStringValue(COL_DATE));
      options.put(COL_DISCOUNT_CURRENCY, parentForm.getStringValue(COL_CURRENCY));
      options.put(COL_MODEL, parentForm.getStringValue(COL_MODEL));
      options.put(COL_PRODUCTION_DATE, parentForm.getStringValue(COL_PRODUCTION_DATE));

      getVatPercent(vatMode, vatPercent -> {
        TradeItemPicker picker = new TradeItemPicker(TradeDocumentPhase.PENDING, operationType,
            parentForm.getLongValue(COL_WAREHOUSE), operationType.getDefaultPrice(),
            parentForm.getDateTimeValue(COL_DATE), parentForm.getLongValue(COL_CURRENCY),
            parentForm.getStringValue(ALS_CURRENCY_NAME), TradeDiscountMode.FROM_AMOUNT, null,
            vatMode, options, vatPercent);

        picker.open((selectedItems, tds) -> {
          List<BeeColumn> columns = DataUtils.getColumns(getDataColumns(),
              Arrays.asList(COL_SERVICE_ORDER, COL_ITEM, COL_TRADE_ITEM_QUANTITY,
                  COL_TRADE_ITEM_PRICE,
                  COL_TRADE_DOCUMENT_ITEM_DISCOUNT, COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT,
                  COL_TRADE_DOCUMENT_ITEM_VAT, COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT));

          BeeRowSet rowSet = new BeeRowSet(getViewName(), columns);

          for (BeeRow row : selectedItems) {
            long id = row.getId();

            double quantity = tds.getQuantity(id);

            Double price = tds.getPrice(id);
            if (BeeUtils.isZero(price)) {
              price = null;
            }
            Pair<Double, Boolean> discountInfo =
                TradeUtils.normalizeDiscountOrVatInfo(tds.getDiscountInfo(id));
            Pair<Double, Boolean> vatInfo =
                TradeUtils.normalizeDiscountOrVatInfo(tds.getVatInfo(id));

            rowSet.addRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION,
                Queries.asList(parentForm.getActiveRowId(), id, quantity, price,
                    discountInfo.getA(), discountInfo.getB(), vatInfo.getA(), vatInfo.getB()));
          }
          Queries.insertRows(rowSet, new DataChangeCallback(rowSet.getViewName()) {
            @Override
            public void onSuccess(RowInfoList result) {
              previewModify(null);
              super.onSuccess(result);
            }
          });
        });
      });
    });
    return false;
  }

  @Override
  public void onEditStart(EditStartEvent event) {
    if (Objects.equals(event.getColumnId(), COL_RESERVE)) {
      event.consume();
      IsRow row = event.getRowValue();
      String value = Data.getString(getViewName(), row, COL_RESERVE);

      Queries.updateCellAndFire(getViewName(), row.getId(), row.getVersion(), COL_RESERVE,
          value, BeeUtils.toString(!BeeUtils.toBoolean(value)));
    } else {
      super.onEditStart(event);
    }
  }

  private static void getVatPercent(TradeVatMode vatMode, Consumer<Double> vatConsumer) {
    Long operation = Global.getParameterRelation(PRM_SERVICE_TRADE_OPERATION);

    if (Objects.nonNull(vatMode) && DataUtils.isId(operation)) {
      Queries.getValue(VIEW_TRADE_OPERATIONS, operation, COL_OPERATION_VAT_PERCENT,
          new RpcCallback<String>() {
            @Override
            public void onSuccess(String result) {
              Double vatPercent = BeeUtils.toDoubleOrNull(result);

              if (vatPercent == null) {
                Number p = Global.getParameterNumber(PRM_VAT_PERCENT);

                if (p != null) {
                  vatPercent = p.doubleValue();
                }
              }
              vatConsumer.accept(vatPercent);
            }
          });
    } else {
      vatConsumer.accept(null);
    }
  }

  private void recalculatePrice() {
    Dictionary d = Localized.dictionary();
    List<? extends IsRow> rows = getGridView().getRowData();

    if (BeeUtils.isEmpty(rows)) {
      getGridView().notifyWarning(d.noData());
      return;
    }
    String caption = d.recalculateTradeItemPriceCaption();

    if (rows.size() == 1) {
      Global.confirm(caption, () -> recalculatePrice(rows));
    } else {
      IsRow activeRow = getGridView().getActiveRow();

      List<String> options = new ArrayList<>();
      options.add(d.recalculateTradeItemPriceForAllItems());

      if (activeRow == null) {
        Global.confirm(caption, Icon.QUESTION, options, () -> recalculatePrice(rows));
      } else {
        options.add(BeeUtils.joinWords(activeRow.getString(getDataIndex(ALS_ITEM_NAME)),
            activeRow.getString(getDataIndex(COL_TRADE_ITEM_ARTICLE))));

        Global.choiceWithCancel(caption, null, options, choice -> {
          switch (choice) {
            case 0:
              recalculatePrice(rows);
              break;
            case 1:
              recalculatePrice(Collections.singletonList(activeRow));
              break;
          }
        });
      }
    }
  }

  private void recalculatePrice(Collection<? extends IsRow> rows) {
    recalc.setEnabled(false);

    FormView parentForm = ViewHelper.getForm(getGridView());
    Map<String, String> options = new HashMap<>();

    options.put(COL_DISCOUNT_COMPANY, parentForm.getStringValue(COL_CUSTOMER));
    options.put(Service.VAR_TIME, parentForm.getStringValue(COL_DATE));
    options.put(COL_DISCOUNT_CURRENCY, parentForm.getStringValue(COL_CURRENCY));
    options.put(COL_DISCOUNT_WAREHOUSE, parentForm.getStringValue(COL_WAREHOUSE));
    options.put(COL_MODEL, parentForm.getStringValue(COL_MODEL));
    options.put(COL_PRODUCTION_DATE, parentForm.getStringValue(COL_PRODUCTION_DATE));

    BeeRowSet rowSet = new BeeRowSet(getViewName(), Data.getColumns(getViewName(),
        Arrays.asList(COL_PRICE, COL_TRADE_DOCUMENT_ITEM_DISCOUNT,
            COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT)));
    Latch latch = new Latch(rows.size());

    for (IsRow row : rows) {
      options.put(Service.VAR_QTY, row.getString(getDataIndex(COL_TRADE_ITEM_QUANTITY)));

      ClassifierKeeper.getPriceAndDiscount(row.getLong(getDataIndex(COL_ITEM)), options,
          (price, discount) -> {
            rowSet.addRow(row.getId(), row.getVersion(),
                Queries.asList(price, discount, BeeUtils.isPositive(discount)));

            latch.decrement();

            if (latch.isOpen()) {
              Queries.updateRows(rowSet, new DataChangeCallback(rowSet.getViewName()) {
                @Override
                public void onSuccess(RowInfoList result) {
                  recalc.setEnabled(true);
                  previewModify(null);
                  super.onSuccess(result);
                }
              });
            }
          });
    }
  }
}
