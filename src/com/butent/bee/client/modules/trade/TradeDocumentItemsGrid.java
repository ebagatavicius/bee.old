package com.butent.bee.client.modules.trade;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.PRM_COMPANY;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.client.modules.classifiers.ClassifierKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
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
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.DecimalValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDiscountMode;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeDocumentSums;
import com.butent.bee.shared.modules.trade.TradeVatMode;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

  private static BeeLogger logger = LogUtils.getLogger(TradeDocumentItemsGrid.class);

  private static final String STYLE_ITEM_SELECTOR =
      TradeKeeper.STYLE_PREFIX + "document-new-item-selector";
  private static final String STYLE_PRICE_CALCULATION_COMMAND =
      TradeKeeper.STYLE_PREFIX + "price-calculation";

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
  public void afterCreatePresenter(GridPresenter presenter) {
    if (presenter != null && presenter.getHeader() != null
        && BeeKeeper.getUser().canEditData(getViewName())) {

      Button button = new Button(Localized.dictionary().recalculateTradeItemPriceCaption(),
          event -> recalculatePrice());
      button.addStyleName(STYLE_PRICE_CALCULATION_COMMAND);

      presenter.getHeader().addCommandItem(button);

      FaLabel test = new FaLabel(FontAwesome.PLUS_SQUARE_O);
      test.addClickHandler(event -> testPicker());
      presenter.getHeader().addCommandItem(test);
    }

    super.afterCreatePresenter(presenter);
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
                      getStock(itemIds, warehouse, stock ->
                          TradeUtils.getDocumentVatPercent(documentRow, defVatPercent ->
                              addItems(documentRow, itemRows, true, stock, defVatPercent)));
                    }

                  } else {
                    TradeUtils.getDocumentVatPercent(documentRow, defVatPercent ->
                        addItems(documentRow, itemRows, false, null, defVatPercent));
                  }
                }
              }
            }));
      }
    }, null, presenter.getHeader().getElement());

    return false;
  }

  @Override
  public void beforeRender(GridView gridView, RenderingEvent event) {
    IsRow parentRow = ViewHelper.getFormRow(gridView);

    if (parentRow != null) {
      boolean changed = false;
      CellGrid grid = getGridView().getGrid();

      boolean visible = TradeUtils.getDocumentDiscountMode(parentRow) != null;

      changed |= grid.setColumnVisible(COL_TRADE_DOCUMENT_ITEM_DISCOUNT, visible);
      changed |= grid.setColumnVisible(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT, visible);

      visible = TradeUtils.getDocumentVatMode(parentRow) != null;

      changed |= grid.setColumnVisible(COL_TRADE_DOCUMENT_ITEM_VAT, visible);
      changed |= grid.setColumnVisible(COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT, visible);

      if (changed) {
        event.setDataChanged();
      }
    }

    super.beforeRender(gridView, event);
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
      boolean stockRequired, Map<Long, Pair<Long, Double>> stock, Double defVatPercent) {

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
        vat = BeeUtils.nvl(row.getDouble(vatPercentIndex), defVatPercent);
      } else {
        vat = null;
      }

      Boolean vatIsPercent = TradeUtils.vatIsPercent(vat);

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

  private void recalculatePrice() {
    final List<? extends IsRow> rows = getGridView().getRowData();
    if (BeeUtils.isEmpty(rows)) {
      getGridView().notifyWarning(Localized.dictionary().noData());
      return;
    }

    String caption = Localized.dictionary().recalculateTradeItemPriceCaption();

    if (rows.size() <= 1) {
      Global.confirm(caption, () -> recalculatePrice(rows));

    } else {
      final IsRow activeRow = getGridView().getActiveRow();

      if (activeRow == null) {
        Global.confirm(caption, Icon.QUESTION, Collections.singletonList(
            Localized.dictionary().recalculateTradeItemPriceForAllItems()),
            () -> recalculatePrice(rows));

      } else {
        List<String> options = new ArrayList<>();
        options.add(BeeUtils.joinWords(activeRow.getString(getDataIndex(ALS_ITEM_NAME)),
            activeRow.getString(getDataIndex(COL_TRADE_ITEM_ARTICLE))));
        options.add(Localized.dictionary().recalculateTradeItemPriceForAllItems());

        Global.choiceWithCancel(caption, null, options, choice -> {
          switch (choice) {
            case 0:
              recalculatePrice(Collections.singletonList(activeRow));
              break;
            case 1:
              recalculatePrice(rows);
              break;
          }
        });
      }
    }
  }

  private void recalculatePrice(List<? extends IsRow> rows) {
    if (BeeUtils.isEmpty(rows)) {
      return;
    }

    IsRow parentRow = getParentRow(getGridView());
    if (parentRow == null) {
      return;
    }

    DateTime date = TradeUtils.getDocumentDate(parentRow);
    Long currency = TradeUtils.getDocumentRelation(parentRow, COL_TRADE_CURRENCY);
    if (date == null || currency == null) {
      return;
    }

    Long operation = TradeUtils.getDocumentRelation(parentRow, COL_TRADE_OPERATION);
    OperationType operationType = TradeUtils.getDocumentOperationType(parentRow);
    if (operation == null || operationType == null) {
      return;
    }

    ItemPrice itemPrice = TradeUtils.getDocumentItemPrice(parentRow);

    final Latch latch = new Latch(rows.size());
    final Holder<Integer> counter = Holder.of(0);

    if (operationType.consumesStock() && ItemPrice.COST == itemPrice) {
      for (IsRow row : rows) {
        setPriceToParentCost(row, date, currency,
            changed -> afterRecalculatePrice(changed, latch, counter));
      }

      return;
    }

    Long company = TradeUtils.getDocumentRelation(parentRow, COL_TRADE_PAYER);
    if (company == null) {
      String colName = operationType.consumesStock() ? COL_TRADE_CUSTOMER : COL_TRADE_SUPPLIER;
      company = TradeUtils.getDocumentRelation(parentRow, colName);

      if (company == null) {
        colName = operationType.consumesStock() ? COL_TRADE_SUPPLIER : COL_TRADE_CUSTOMER;
        company = TradeUtils.getDocumentRelation(parentRow, colName);

        if (company == null) {
          company = Global.getParameterRelation(PRM_COMPANY);

          if (company == null) {
            getGridView().notifyWarning("company not specified");
            return;
          }
        }
      }
    }

    Long documentWarehouse = TradeUtils.getDocumentRelation(parentRow,
        operationType.consumesStock() ? COL_TRADE_WAREHOUSE_FROM : COL_TRADE_WAREHOUSE_TO);

    TradeDiscountMode discountMode = TradeUtils.getDocumentDiscountMode(parentRow);

    Map<String, String> options = new HashMap<>();

    options.put(COL_DISCOUNT_COMPANY, BeeUtils.toStringOrNull(company));

    options.put(COL_DISCOUNT_OPERATION, BeeUtils.toStringOrNull(operation));
    if (operationType.requireOperationForPriceCalculation()) {
      options.put(Service.VAR_REQUIRED, COL_DISCOUNT_OPERATION);
    }

    options.put(Service.VAR_TIME, BeeUtils.toString(date.getTime()));
    options.put(COL_DISCOUNT_CURRENCY, BeeUtils.toStringOrNull(currency));

    if (itemPrice != null) {
      options.put(COL_DISCOUNT_PRICE_NAME, BeeUtils.toString(itemPrice.ordinal()));
    }

    for (IsRow row : rows) {
      Double quantity = row.getDouble(getDataIndex(COL_TRADE_ITEM_QUANTITY));
      options.put(Service.VAR_QTY, BeeUtils.toStringOrNull(quantity));
      options.put(COL_DISCOUNT_UNIT, row.getString(getDataIndex(COL_UNIT)));

      Long warehouse = row.getLong(getDataIndex(operationType.consumesStock()
          ? COL_TRADE_ITEM_WAREHOUSE_FROM : COL_TRADE_ITEM_WAREHOUSE_TO));
      if (warehouse == null) {
        warehouse = documentWarehouse;
      }
      options.put(COL_DISCOUNT_WAREHOUSE, BeeUtils.toStringOrNull(warehouse));

      getPriceAndDiscount(row, options, discountMode,
          changed -> afterRecalculatePrice(changed, latch, counter));
    }
  }

  private void getPriceAndDiscount(final IsRow row, Map<String, String> options,
      final TradeDiscountMode discountMode, final Consumer<Boolean> callback) {

    Long item = row.getLong(getDataIndex(COL_ITEM));

    final int priceIndex = getDataIndex(COL_TRADE_ITEM_PRICE);
    final int discountIndex = getDataIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT);
    final int discountIsPercentIndex = getDataIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT);

    Double oldPrice = row.getDouble(priceIndex);
    Double oldDiscount = row.getDouble(discountIndex);
    boolean oldDiscountIsPercent = row.isTrue(discountIsPercentIndex);

    ClassifierKeeper.getPriceAndDiscount(item, options, (price, discount) -> {
      Double newPrice = oldPrice;
      Double newDiscount = oldDiscount;
      boolean newDiscountIsPercent = oldDiscountIsPercent;

      if (BeeUtils.isDouble(price)) {
        if (BeeUtils.nonZero(price) && BeeUtils.nonZero(discount) && discountMode == null) {
          newPrice = TradeUtils.roundPrice(BeeUtils.minusPercent(price, discount));
          newDiscount = null;
          newDiscountIsPercent = false;
        } else {
          newPrice = price;
        }
      }

      if (BeeUtils.isDouble(discount) && discountMode != null) {
        if (BeeUtils.isZero(discount)) {
          newDiscount = null;
          newDiscountIsPercent = false;
        } else {
          newDiscount = discount;
          newDiscountIsPercent = true;
        }
      }

      boolean priceChanged = !Objects.equals(oldPrice, newPrice);
      boolean discountChanged = !Objects.equals(oldDiscount, newDiscount);
      boolean discountIsPercentChanged = oldDiscountIsPercent ^ newDiscountIsPercent;

      if (priceChanged || discountChanged || discountIsPercentChanged) {
        final List<BeeColumn> columns = new ArrayList<>();
        List<String> oldValues = new ArrayList<>();
        List<String> newValues = new ArrayList<>();

        if (priceChanged) {
          columns.add(getDataColumns().get(priceIndex));
          oldValues.add(BeeUtils.toStringOrNull(oldPrice));
          newValues.add(BeeUtils.toStringOrNull(newPrice));
        }

        if (discountChanged) {
          columns.add(getDataColumns().get(discountIndex));
          oldValues.add(BeeUtils.toStringOrNull(oldDiscount));
          newValues.add(BeeUtils.toStringOrNull(newDiscount));
        }

        if (discountIsPercentChanged) {
          columns.add(getDataColumns().get(discountIsPercentIndex));
          oldValues.add(BooleanValue.pack(oldDiscountIsPercent));
          newValues.add(BooleanValue.pack(newDiscountIsPercent));
        }

        Queries.update(getViewName(), row.getId(), row.getVersion(), columns, oldValues, newValues,
            null, new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
                RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), result);
                getGridView().getGrid().addUpdatedSources(row.getId(),
                    DataUtils.getColumnNames(columns));

                callback.accept(true);
              }
            });

      } else {
        callback.accept(false);
      }
    });
  }

  private void setPriceToParentCost(IsRow row, DateTime date, Long currency,
      Consumer<Boolean> callback) {

    Double cost = row.getDouble(getDataIndex(ALS_PARENT_COST));
    Long costCurrency = row.getLong(getDataIndex(ALS_PARENT_COST_CURRENCY));

    if (BeeUtils.isDouble(cost) && costCurrency != null) {
      Double oldPrice = row.getDouble(getDataIndex(COL_TRADE_ITEM_PRICE));

      Double newPrice;
      if (Objects.equals(currency, costCurrency)) {
        newPrice = cost;
      } else {
        newPrice = Data.round(getViewName(), COL_TRADE_ITEM_PRICE,
            Money.exchange(costCurrency, currency, cost, date));
      }

      if (Objects.equals(oldPrice, newPrice)) {
        callback.accept(false);

      } else {
        Queries.updateCellAndFire(getViewName(), row.getId(), row.getVersion(),
            COL_TRADE_ITEM_PRICE, BeeUtils.toStringOrNull(oldPrice),
            BeeUtils.toStringOrNull(newPrice));

        getGridView().getGrid().addUpdatedSources(row.getId(),
            Collections.singleton(COL_TRADE_ITEM_PRICE));
        callback.accept(true);
      }

    } else {
      callback.accept(false);
    }
  }

  private void afterRecalculatePrice(boolean changed, Latch latch, Holder<Integer> counter) {
    if (changed) {
      counter.set(counter.get() + 1);
    }

    latch.decrement();
    if (latch.isOpen()) {
      getGridView().notifyInfo(
          Localized.dictionary().recalculateTradeItemPriceNotification(counter.get()));
    }
  }

  private void testPicker() {
    IsRow parentRow = getParentRow(getGridView());

    if (DataUtils.hasId(parentRow)) {
      TradeUtils.getDocumentVatPercent(parentRow, vatPercent -> openPicker(parentRow, vatPercent));
    }
  }

  private void addItems(TradeDocumentSums tds) {
    logger.debug(tds.getItemIds());
  }

  private void openPicker(IsRow parentRow, Double defaultVatPercent) {
    final TradeItemPicker picker = new TradeItemPicker(parentRow, defaultVatPercent);

    final DialogBox dialog = DialogBox.withoutCloseBox(Localized.dictionary().itemSelection(),
        TradeItemPicker.STYLE_DIALOG);

    final FaLabel save = new FaLabel(FontAwesome.SAVE);
    save.addStyleName(TradeItemPicker.STYLE_SAVE);

    save.addClickHandler(event -> {
      if (picker.hasSelection()) {
        addItems(picker.getTds());
      }
      dialog.close();
    });

    dialog.addAction(Action.SAVE, save);

    FaLabel close = new FaLabel(FontAwesome.CLOSE);
    close.addStyleName(TradeItemPicker.STYLE_CLOSE);

    close.addClickHandler(event -> {
      if (picker.hasSelection()) {
        Global.decide(Localized.dictionary().itemSelection(),
            Collections.singletonList(Localized.dictionary().saveSelectedItems()),
            new DecisionCallback() {
              @Override
              public void onConfirm() {
                addItems(picker.getTds());
                dialog.close();
              }

              @Override
              public void onDeny() {
                dialog.close();
              }
            }, DialogConstants.DECISION_YES);

      } else {
        dialog.close();
      }
    });

    dialog.addAction(Action.CLOSE, close);

    dialog.setWidget(picker);
    dialog.center();

    dialog.setOnSave(event -> save.click());
  }
}
