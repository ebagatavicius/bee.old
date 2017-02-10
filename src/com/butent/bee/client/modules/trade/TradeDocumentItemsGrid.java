package com.butent.bee.client.modules.trade;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.PRM_VAT_PERCENT;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasRowValue;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.DecimalValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeDocumentSums;
import com.butent.bee.shared.modules.trade.TradeVatMode;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TradeDocumentItemsGrid extends AbstractGridInterceptor {

  private enum SumColumn {
    AMOUNT {
      @Override
      String getName() {
        return "Amount";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO : tds.getItemAmount(row.getId());
      }
    },

    DISCOUNT {
      @Override
      String getName() {
        return "DiscountAmount";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO : tds.getItemDiscount(row.getId());
      }
    },

    PRICE_WITHOUT_VAT {
      @Override
      String getName() {
        return "PriceWithoutVat";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO : tds.getPriceWithoutVat(row.getId());
      }
    },

    AMOUNT_WITHOUT_VAT {
      @Override
      String getName() {
        return "AmountWithoutVat";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO
            : (tds.getItemTotal(row.getId()) - tds.getItemVat(row.getId()));
      }
    },

    VAT {
      @Override
      String getName() {
        return "VatAmount";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO : tds.getItemVat(row.getId());
      }
    },

    PRICE_WITH_VAT {
      @Override
      String getName() {
        return "PriceWithVat";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO : tds.getPriceWithVat(row.getId());
      }
    },

    TOTAL {
      @Override
      String getName() {
        return "Total";
      }

      @Override
      double getValue(IsRow row, TradeDocumentSums tds) {
        return (tds == null) ? BeeConst.DOUBLE_ZERO : tds.getItemTotal(row.getId());
      }
    };

    abstract String getName();

    abstract double getValue(IsRow row, TradeDocumentSums tds);
  }

  private final class SumRenderer extends AbstractCellRenderer implements HasRowValue {

    private final SumColumn sumColumn;

    private SumRenderer(SumColumn sumColumn) {
      super(null);
      this.sumColumn = sumColumn;
    }

    @Override
    public boolean dependsOnSource(String source) {
      return BeeUtils.inList(source, COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
          COL_TRADE_DOCUMENT_ITEM_DISCOUNT, COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT,
          COL_TRADE_DOCUMENT_ITEM_VAT, COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT);
    }

    @Override
    public Value getRowValue(IsRow row) {
      return DecimalValue.of(evaluate(row));
    }

    @Override
    public String render(IsRow row) {
      double x = evaluate(row);
      return (x == BeeConst.DOUBLE_ZERO) ? null : BeeUtils.toString(x);
    }

    private double evaluate(IsRow row) {
      if (row == null || tdsSupplier == null) {
        return BeeConst.DOUBLE_ZERO;
      } else {
        return sumColumn.getValue(row, tdsSupplier.get());
      }
    }
  }

  private static final String STYLE_ITEM_SELECTOR =
      BeeConst.CSS_CLASS_PREFIX + "trade-document-new-item-selector";

  private Supplier<TradeDocumentSums> tdsSupplier;
  private Runnable tdsListener;

  TradeDocumentItemsGrid() {
  }

  void setTdsSupplier(Supplier<TradeDocumentSums> tdsSupplier) {
    this.tdsSupplier = tdsSupplier;
  }

  void setTdsListener(Runnable tdsListener) {
    this.tdsListener = tdsListener;
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeDocumentItemsGrid();
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    IsRow parentRow = getParentRow(presenter.getGridView());
    if (parentRow == null) {
      return false;
    }

    TradeDocumentPhase phase = TradeUtils.getDocumentPhase(parentRow);
    OperationType operationType = TradeUtils.getDocumentOperationType(parentRow);
    if (phase == null || operationType == null) {
      return false;
    }

    ItemPrice itemPrice = TradeUtils.getDocumentItemPrice(parentRow);

    Relation relation = Relation.create();
    relation.setViewName(VIEW_ITEMS);

    relation.disableNewRow();
    relation.setSelectorClass(STYLE_ITEM_SELECTOR);

    List<String> renderColumns = Arrays.asList(COL_ITEM_NAME, COL_ITEM_ARTICLE);

    List<String> searchableColumns = new ArrayList<>(renderColumns);
    relation.setSearchableColumns(searchableColumns);

    List<String> choiceColumns = new ArrayList<>(renderColumns);
    if (itemPrice != null) {
      choiceColumns.add(itemPrice.getPriceColumn());
      choiceColumns.add(itemPrice.getCurrencyNameAlias());
    }

    final String caption;
    final MultiSelector selector;

    if (operationType.consumesStock()) {
      Long warehouseFrom = Data.getLong(VIEW_TRADE_DOCUMENTS, parentRow, COL_TRADE_WAREHOUSE_FROM);
      if (!DataUtils.isId(warehouseFrom)) {
        presenter.getGridView().notifyWarning(Localized.dictionary()
            .fieldRequired(Localized.dictionary().trdWarehouseFrom()));
        return false;
      }

      caption = BeeUtils.joinItems(Localized.dictionary().trdStock(),
          Localized.dictionary().services());

      String wfCode = Data.getString(VIEW_TRADE_DOCUMENTS, parentRow, ALS_WAREHOUSE_FROM_CODE);
      if (!BeeUtils.isEmpty(wfCode)) {
        choiceColumns.add(keyStockWarehouse(wfCode));
      }

      relation.setChoiceColumns(choiceColumns);

      if (phase.modifyStock()) {
        Filter filter = Filter.or(Filter.notNull(COL_ITEM_IS_SERVICE),
            Filter.in(Data.getIdColumn(VIEW_ITEMS), VIEW_TRADE_DOCUMENT_ITEMS, COL_ITEM,
                Filter.in(Data.getIdColumn(VIEW_TRADE_DOCUMENT_ITEMS),
                    VIEW_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM,
                    Filter.and(Filter.equals(COL_STOCK_WAREHOUSE, warehouseFrom),
                        Filter.isPositive(COL_STOCK_QUANTITY)))));

        relation.setFilter(filter);
        relation.setCaching(Relation.Caching.NONE);
      }

      selector = MultiSelector.autonomous(relation, renderColumns);

    } else {
      caption = BeeUtils.joinItems(Localized.dictionary().goods(),
          Localized.dictionary().services());

      relation.setChoiceColumns(choiceColumns);
      selector = MultiSelector.autonomous(relation, renderColumns);
    }

    int width = presenter.getGridView().asWidget().getOffsetWidth();
    if (width > 300) {
      StyleUtils.setWidth(selector, width - 50);
    }

    Global.inputWidget(caption, selector, () -> {
      final List<Long> itemIds = DataUtils.parseIdList(selector.getValue());

      if (!itemIds.isEmpty()) {
        presenter.getGridView().ensureRelId(documentId ->
            Queries.getRowSequence(VIEW_ITEMS, itemIds, itemRows -> {
              if (BeeUtils.isEmpty(itemRows)) {
                getGridView().notifyWarning(Localized.dictionary().noData());
              } else {
                IsRow documentRow = getParentRow(getGridView());

                if (DataUtils.hasId(documentRow)) {
                  if (isStockRequired(documentRow)) {
                    Long warehouse = Data.getLong(VIEW_TRADE_DOCUMENTS, documentRow,
                        COL_TRADE_WAREHOUSE_FROM);

                    if (DataUtils.isId(warehouse)) {
                      getStock(itemIds, warehouse,
                          stock -> addItems(documentRow, itemRows, true, stock));
                    }
                  } else {
                    addItems(documentRow, itemRows, false, null);
                  }
                }
              }
            }));
      }
    }, null, presenter.getHeader().getElement());

    return false;
  }

  @Override
  public AbstractCellRenderer getRenderer(String columnName, List<? extends IsColumn> dataColumns,
      ColumnDescription columnDescription, CellSource cellSource) {

    for (SumColumn sc : SumColumn.values()) {
      if (BeeUtils.same(columnName, sc.getName())) {
        return new SumRenderer(sc);
      }
    }

    return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
  }

  @Override
  public void onDataReceived(List<? extends IsRow> rows) {
    if (tdsSupplier != null) {
      if (getGridPresenter() != null && getGridPresenter().getUserFilter() == null) {
        tdsSupplier.get().clearItems();
      }

      if (!BeeUtils.isEmpty(rows)) {
        int qtyIndex = getQuantityIndex();
        int priceIndex = getPriceIndex();

        int discountIndex = getDiscountIndex();
        int dipIndex = getDiscountIsPercentIndex();

        int vatIndex = getVatIndex();
        int vipIndex = getVatIsPercentIndex();

        for (IsRow row : rows) {
          tdsSupplier.get().add(row.getId(), row.getDouble(qtyIndex), row.getDouble(priceIndex),
              row.getDouble(discountIndex), row.getBoolean(dipIndex),
              row.getDouble(vatIndex), row.getBoolean(vipIndex));
        }
      }

      fireTdsChange();
    }

    TradeUtils.configureCostCalculation(getGridView());

    super.onDataReceived(rows);
  }

  @Override
  public boolean previewCellUpdate(CellUpdateEvent event) {
    if (event.hasColumn() && tdsSupplier != null
        && tdsSupplier.get().containsItem(event.getRowId())) {

      long id = event.getRowId();
      String value = event.getValue();

      boolean fire = false;

      switch (event.getSourceName()) {
        case COL_TRADE_ITEM_QUANTITY:
          fire = tdsSupplier.get().updateQuantity(id, BeeUtils.toDoubleOrNull(value));
          break;

        case COL_TRADE_ITEM_PRICE:
          fire = tdsSupplier.get().updatePrice(id, BeeUtils.toDoubleOrNull(value));
          break;

        case COL_TRADE_DOCUMENT_ITEM_DISCOUNT:
          fire = tdsSupplier.get().updateDiscount(id, BeeUtils.toDoubleOrNull(value));
          break;

        case COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT:
          fire = tdsSupplier.get().updateDiscountIsPercent(id, BeeUtils.toBooleanOrNull(value));
          break;

        case COL_TRADE_DOCUMENT_ITEM_VAT:
          fire = tdsSupplier.get().updateVat(id, BeeUtils.toDoubleOrNull(value));
          break;

        case COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT:
          fire = tdsSupplier.get().updateVatIsPercent(id, BeeUtils.toBooleanOrNull(value));
          break;
      }

      if (fire) {
        fireTdsChange();
      }
    }

    return super.previewCellUpdate(event);
  }

  @Override
  public boolean previewMultiDelete(MultiDeleteEvent event) {
    if (tdsSupplier != null) {
      boolean fire = false;

      for (long id : event.getRowIds()) {
        if (tdsSupplier.get().containsItem(id)) {
          tdsSupplier.get().deleteItem(id);
          fire = true;
        }
      }

      if (fire) {
        fireTdsChange();
      }
    }

    return super.previewMultiDelete(event);
  }

  @Override
  public boolean previewRowDelete(RowDeleteEvent event) {
    if (tdsSupplier != null && tdsSupplier.get().containsItem(event.getRowId())) {
      tdsSupplier.get().deleteItem(event.getRowId());
      fireTdsChange();
    }

    return super.previewRowDelete(event);
  }

  @Override
  public boolean previewRowUpdate(RowUpdateEvent event) {
    if (tdsSupplier != null && tdsSupplier.get().containsItem(event.getRowId())) {
      IsRow row = event.getRow();
      long id = row.getId();

      boolean fire = false;

      fire |= tdsSupplier.get().updateQuantity(id, row.getDouble(getQuantityIndex()));
      fire |= tdsSupplier.get().updatePrice(id, row.getDouble(getPriceIndex()));

      fire |= tdsSupplier.get().updateDiscount(id, row.getDouble(getDiscountIndex()));
      fire |= tdsSupplier.get().updateDiscountIsPercent(id,
          row.getBoolean(getDiscountIsPercentIndex()));

      fire |= tdsSupplier.get().updateVat(id, row.getDouble(getVatIndex()));
      fire |= tdsSupplier.get().updateVatIsPercent(id,
          row.getBoolean(getVatIsPercentIndex()));

      if (fire) {
        fireTdsChange();
      }
    }

    return super.previewRowUpdate(event);
  }

  private void fireTdsChange() {
    if (tdsListener != null) {
      tdsListener.run();
    }
  }

  private int getQuantityIndex() {
    return getDataIndex(COL_TRADE_ITEM_QUANTITY);
  }

  private int getPriceIndex() {
    return getDataIndex(COL_TRADE_ITEM_PRICE);
  }

  private int getDiscountIndex() {
    return getDataIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT);
  }

  private int getDiscountIsPercentIndex() {
    return getDataIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT);
  }

  private int getVatIndex() {
    return getDataIndex(COL_TRADE_DOCUMENT_ITEM_VAT);
  }

  private int getVatIsPercentIndex() {
    return getDataIndex(COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT);
  }

  private static IsRow getParentRow(GridView gridView) {
    if (gridView == null) {
      return null;
    } else {
      return ViewHelper.getParentRow(gridView.asWidget(), VIEW_TRADE_DOCUMENTS);
    }
  }

  private static void getStock(List<Long> itemIds, Long warehouse,
      final Consumer<Map<Long, Pair<Long, Double>>> consumer) {

    List<String> columns = Arrays.asList(COL_ITEM, COL_TRADE_DOCUMENT_ITEM, COL_STOCK_QUANTITY);

    Filter filter = Filter.and(
        Filter.equals(COL_STOCK_WAREHOUSE, warehouse),
        Filter.any(COL_ITEM, itemIds),
        Filter.isPositive(COL_STOCK_QUANTITY));

    Order order = Order.ascending(ALS_STOCK_PRIMARY_DATE, COL_TRADE_DATE);

    Queries.getRowSet(VIEW_TRADE_STOCK, columns, filter, order, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet rowSet) {
        Map<Long, Pair<Long, Double>> stock = new HashMap<>();

        if (!DataUtils.isEmpty(rowSet)) {
          int itemIndex = rowSet.getColumnIndex(COL_ITEM);
          int parentIndex = rowSet.getColumnIndex(COL_TRADE_DOCUMENT_ITEM);
          int qtyIndex = rowSet.getColumnIndex(COL_STOCK_QUANTITY);

          for (IsRow row : rowSet) {
            stock.putIfAbsent(row.getLong(itemIndex),
                Pair.of(row.getLong(parentIndex), row.getDouble(qtyIndex)));
          }
        }

        consumer.accept(stock);
      }
    });
  }

  private static boolean isStockRequired(IsRow documentRow) {
    if (documentRow == null) {
      return false;
    }

    TradeDocumentPhase phase = TradeUtils.getDocumentPhase(documentRow);
    OperationType operationType = TradeUtils.getDocumentOperationType(documentRow);

    if (phase == null || operationType == null) {
      return false;
    } else {
      return phase.modifyStock() && operationType.consumesStock();
    }
  }

  private void addItems(IsRow documentRow, List<? extends IsRow> itemRows,
      boolean stockRequired, Map<Long, Pair<Long, Double>> stock) {

    DateTime date = Data.getDateTime(VIEW_TRADE_DOCUMENTS, documentRow, COL_TRADE_DATE);
    Long currency = Data.getLong(VIEW_TRADE_DOCUMENTS, documentRow, COL_TRADE_CURRENCY);

    ItemPrice itemPrice = TradeUtils.getDocumentItemPrice(documentRow);
    TradeVatMode vatMode = TradeUtils.getDocumentVatMode(documentRow);

    int serviceIndex = Data.getColumnIndex(VIEW_ITEMS, COL_ITEM_IS_SERVICE);

    int articleIndex = Data.getColumnIndex(VIEW_ITEMS, COL_ITEM_ARTICLE);
    int quantityIndex = Data.getColumnIndex(VIEW_ITEMS, COL_ITEM_DEFAULT_QUANTITY);

    int vatIndex = Data.getColumnIndex(VIEW_ITEMS, COL_ITEM_VAT);
    int vatPercentIndex = Data.getColumnIndex(VIEW_ITEMS, COL_ITEM_VAT_PERCENT);

    int priceIndex;
    int currencyIndex;

    if (itemPrice == null) {
      priceIndex = BeeConst.UNDEF;
      currencyIndex = BeeConst.UNDEF;
    } else {
      priceIndex = Data.getColumnIndex(VIEW_ITEMS, itemPrice.getPriceColumn());
      currencyIndex = Data.getColumnIndex(VIEW_ITEMS, itemPrice.getCurrencyColumn());
    }

    List<BeeColumn> columns = DataUtils.getColumns(getDataColumns(),
        Arrays.asList(COL_TRADE_DOCUMENT, COL_ITEM, COL_TRADE_ITEM_ARTICLE,
            COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
            COL_TRADE_DOCUMENT_ITEM_VAT, COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT,
            COL_TRADE_ITEM_PARENT));

    BeeRowSet rowSet = new BeeRowSet(getViewName(), columns);

    for (IsRow row : itemRows) {
      Double quantity = BeeUtils.nvl(row.getDouble(quantityIndex), BeeConst.DOUBLE_ONE);

      Double price = BeeConst.isUndef(priceIndex) ? null : row.getDouble(priceIndex);
      if (BeeUtils.nonZero(price)) {
        Long itemCurrency = BeeConst.isUndef(currencyIndex) ? null : row.getLong(currencyIndex);

        if (Money.canExchange(itemCurrency, currency)) {
          price = Money.exchange(itemCurrency, currency, price, date);
          price = Localized.normalizeMoney(price);
        }
      }

      Double vat;
      if (vatMode != null && BeeUtils.isTrue(row.getBoolean(vatIndex))) {
        Number number = Global.getParameterNumber(PRM_VAT_PERCENT);
        vat = BeeUtils.nvl(row.getDouble(vatPercentIndex),
            Objects.isNull(number) ? null : number.doubleValue());
      } else {
        vat = null;
      }

      Boolean vatIsPercent = BeeUtils.isDouble(vat) ? true : null;

      Long parent = null;

      if (stockRequired && !BeeUtils.isTrue(row.getBoolean(serviceIndex))) {
        Pair<Long, Double> pair = (stock == null) ? null : stock.get(row.getId());
        if (pair == null) {
          continue;
        }

        parent = pair.getA();
        quantity = BeeUtils.min(quantity, pair.getB());
      }

      rowSet.addRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION,
          Queries.asList(documentRow.getId(), row.getId(), row.getString(articleIndex),
              quantity, price, vat, vatIsPercent, parent));
    }

    Queries.insertRows(rowSet);
  }
}
