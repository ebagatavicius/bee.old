package com.butent.bee.client.modules.cars;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.DataChangeCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.modules.trade.TradeItemPicker;
import com.butent.bee.client.modules.trade.TradeUtils;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.ParentRowRefreshGrid;
import com.butent.bee.client.widget.CustomAction;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDiscountMode;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeVatMode;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CarServiceItemsGrid extends ParentRowRefreshGrid implements ClickHandler {

  private final CustomAction addBundle = new CustomAction(FontAwesome.DROPBOX, this);

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    HeaderView header = presenter.getHeader();

    if (Objects.nonNull(header) && BeeKeeper.getUser().canEditData(getViewName())) {
      addBundle.setTitle(Localized.dictionary().bundle());
      header.addCommandItem(addBundle);
      header.addCommandItem(new PriceRecalculator(this));
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
  public void onClick(ClickEvent clickEvent) {
    getGridView().ensureRelId(parentId -> {
      FormView parentForm = ViewHelper.getForm(getGridView());
      CompoundFilter filter = Filter.and();

      filter.add(Filter.or(Filter.isNull(COL_VALID_UNTIL),
          Filter.isMore(COL_VALID_UNTIL, Value.getValue(TimeUtils.today()))));

      Stream.of(COL_MODEL, COL_CUSTOMER).forEach(col -> {
        Filter subFilter = Filter.isNull(col);

        if (DataUtils.isId(parentForm.getLongValue(col))) {
          subFilter = Filter.or(subFilter, Filter.equals(col, parentForm.getLongValue(col)));
        }
        filter.add(subFilter);
      });
      Relation relation = Relation.create(TBL_CAR_BUNDLES,
          Arrays.asList(COL_CODE, COL_BUNDLE_NAME));
      relation.disableNewRow();
      relation.disableEdit();
      relation.setFilter(filter);
      UnboundSelector selector = UnboundSelector.create(relation);

      selector.addSelectorHandler(event -> {
        Long id = event.getValue();

        if (event.isChanged() && DataUtils.isId(id)) {
          UiHelper.getParentPopup(selector).close();
          addBundle.running();

          Queries.getRowSet(TBL_CAR_BUNDLE_ITEMS, null, Filter.equals(COL_BUNDLE, id),
              new Queries.RowSetCallback() {
                @Override
                public void onSuccess(BeeRowSet rs) {
                  if (rs.isEmpty()) {
                    getGridView().notifyWarning(Localized.dictionary().noData());
                    return;
                  }
                  DataInfo info = Data.getDataInfo(TBL_SERVICE_ORDER_ITEMS);

                  List<BeeColumn> cols = new ArrayList<>();
                  cols.add(info.getColumn(COL_SERVICE_ORDER));
                  cols.add(info.getColumn(COL_TRADE_DOCUMENT_ITEM_DISCOUNT));
                  cols.add(info.getColumn(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT));

                  rs.getColumns().stream()
                      .filter(BeeColumn::isEditable)
                      .map(BeeColumn::getId)
                      .filter(info::containsColumn)
                      .forEach(col -> cols.add(info.getColumn(col)));

                  BeeRowSet newRs = new BeeRowSet(info.getViewName(), cols);

                  PriceRecalculator.getPriceAndDiscount(getGridView(), rs.getNumberOfRows(),
                      (idx, data) -> {
                        switch (data) {
                          case ITEM:
                            return rs.getRow(idx).getString(rs.getColumnIndex(COL_ITEM));
                          case QUANTITY:
                            return rs.getRow(idx)
                                .getString(rs.getColumnIndex(COL_TRADE_ITEM_QUANTITY));
                          case PRICE:
                            return rs.getRow(idx).getString(rs.getColumnIndex(COL_PRICE));
                          case CURRENCY:
                            return rs.getRow(idx).getString(rs.getColumnIndex(COL_CURRENCY));
                        }
                        return null;
                      },
                      (idx, price, discount) -> {
                        BeeRow beeRow = rs.getRow(idx);
                        BeeRow newRow = newRs.addEmptyRow();

                        for (int i = 0; i < cols.size(); i++) {
                          String col = cols.get(i).getId();

                          switch (col) {
                            case COL_SERVICE_ORDER:
                              newRow.setValue(i, parentId);
                              break;

                            case COL_PRICE:
                              newRow.setValue(i, price);
                              break;

                            case COL_TRADE_DOCUMENT_ITEM_DISCOUNT:
                              newRow.setValue(i, discount);
                              break;

                            case COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT:
                              newRow.setValue(i, BeeUtils.isPositive(discount));
                              break;

                            default:
                              newRow.setValue(i, beeRow.getString(rs.getColumnIndex(col)));
                              break;
                          }
                        }
                      },
                      () -> Queries.insertRows(newRs, new RpcCallback<RowInfoList>() {
                        @Override
                        public void onSuccess(RowInfoList res) {
                          addBundle.idle();
                          Queries.getRow(parentForm.getViewName(), parentId,
                              RowCallback.refreshRow(parentForm.getViewName(), true));
                        }
                      }));
                }
              });
        }
      });
      Global.showModalWidget(Localized.dictionary().bundle(), selector);
    });
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
}
