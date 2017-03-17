package com.butent.bee.client.modules.trade;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Storage;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeItemSearch;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

class TradeItemPicker extends Flow {

  private static BeeLogger logger = LogUtils.getLogger(TradeItemPicker.class);

  private static final String STYLE_NAME = TradeKeeper.STYLE_PREFIX + "item-picker";
  private static final String STYLE_PREFIX = STYLE_NAME + "-";

  private static final String STYLE_SEARCH_PREFIX = STYLE_PREFIX + "search-";
  private static final String STYLE_SEARCH_PANEL = STYLE_SEARCH_PREFIX + "panel";
  private static final String STYLE_SEARCH_BOX = STYLE_SEARCH_PREFIX + "box";
  private static final String STYLE_SEARCH_COMMAND = STYLE_SEARCH_PREFIX + "command";
  private static final String STYLE_SEARCH_SPINNER = STYLE_SEARCH_PREFIX + "spinner";
  private static final String STYLE_SEARCH_RUNNING = STYLE_SEARCH_PREFIX + "running";
  private static final String STYLE_SEARCH_BY_CONTAINER = STYLE_SEARCH_PREFIX + "by-container";
  private static final String STYLE_SEARCH_BY_SELECTOR = STYLE_SEARCH_PREFIX + "by-selector";

  private static final String STYLE_ITEM_PANEL = STYLE_PREFIX + "item-panel";
  private static final String STYLE_ITEM_TABLE = STYLE_PREFIX + "item-table";

  private static final String STYLE_HEADER_ROW = STYLE_PREFIX + "header";
  private static final String STYLE_ITEM_ROW = STYLE_PREFIX + "item";
  private static final String STYLE_SERVICE_ROW = STYLE_PREFIX + "service";
  private static final String STYLE_SELECTED_ROW = STYLE_PREFIX + "item-selected";

  private static final String STYLE_ID = STYLE_PREFIX + "id";
  private static final String STYLE_STOCK = STYLE_PREFIX + "stock";
  private static final String STYLE_RESERVED = STYLE_PREFIX + "reserved";
  private static final String STYLE_QTY = STYLE_PREFIX + "qty";
  private static final String STYLE_MAIN_WAREHOUSE = STYLE_PREFIX + "main-warehouse";

  private static final String STYLE_HEADER_CELL_SUFFIX = "-label";
  private static final String STYLE_CELL_SUFFIX = "-cell";

  private static final String STYLE_QTY_INPUT = STYLE_QTY + "-input";

  private static final int SEARCH_BY_SIZE = 3;

  private static final String SEARCH_BY_STORAGE_PREFIX =
      NameUtils.getClassName(TradeItemPicker.class) + "-by";

  private final Flow itemPanel = new Flow(STYLE_ITEM_PANEL);

  private TradeDocumentPhase documentPhase;
  private OperationType operationType;
  private Long warehouse;

  private ItemPrice itemPrice;
  private Long currency;

  private final Map<Long, Double> selection = new LinkedHashMap<>();

  private ChangeHandler quantityChangeHandler;

  TradeItemPicker(IsRow documentRow) {
    super(STYLE_NAME);

    add(createSearch());
    add(itemPanel);

    setDocumentRow(documentRow);
  }

  void setDocumentRow(IsRow row) {
    setDocumentPhase(TradeUtils.getDocumentPhase(row));
    setOperationType(TradeUtils.getDocumentOperationType(row));
    setWarehouse(TradeUtils.getDocumentRelation(row, COL_TRADE_WAREHOUSE_FROM));

    setItemPrice(TradeUtils.getDocumentItemPrice(row));
    setCurrency(TradeUtils.getDocumentRelation(row, COL_TRADE_CURRENCY));
  }

  private static String searchByStorageKey(int index) {
    return Storage.getUserKey(SEARCH_BY_STORAGE_PREFIX, BeeUtils.toString(index));
  }

  private static Widget createSearchBySelector(int index) {
    ListBox selector = new ListBox();
    selector.addStyleName(STYLE_SEARCH_BY_SELECTOR);

    selector.addItem(BeeConst.STRING_EMPTY, BeeConst.STRING_ASTERISK);
    for (TradeItemSearch tis : TradeItemSearch.values()) {
      selector.addItem(tis.getCaption(), tis.name());
    }

    String value = BeeKeeper.getStorage().get(searchByStorageKey(index));
    if (!BeeUtils.isEmpty(value)) {
      int selected = selector.getIndex(value);
      if (!BeeConst.isUndef(selected)) {
        selector.setSelectedIndex(selected);
      }
    }

    return selector;
  }

  private Widget createSearch() {
    Flow panel = new Flow(STYLE_SEARCH_PANEL);

    final List<Widget> searchBySelectors = new ArrayList<>();
    for (int i = 0; i < SEARCH_BY_SIZE; i++) {
      searchBySelectors.add(createSearchBySelector(i));
    }

    final InputText searchBox = new InputText();
    DomUtils.setSearch(searchBox);
    searchBox.setMaxLength(20);
    searchBox.addStyleName(STYLE_SEARCH_BOX);

    searchBox.addKeyDownHandler(event -> {
      if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
        String input = searchBox.getValue();
        if (!BeeUtils.isEmpty(input)) {
          doSearch(input, searchBySelectors);
        }
      }
    });
    panel.add(searchBox);

    FaLabel searchCommand = new FaLabel(FontAwesome.SEARCH, STYLE_SEARCH_COMMAND);
    searchCommand.addClickHandler(event -> doSearch(searchBox.getValue(), searchBySelectors));
    panel.add(searchCommand);

    FaLabel spinner = new FaLabel(FontAwesome.SPINNER, STYLE_SEARCH_SPINNER);
    panel.add(spinner);

    Flow searchByContainer = new Flow(STYLE_SEARCH_BY_CONTAINER);
    searchBySelectors.forEach(searchByContainer::add);

    panel.add(searchByContainer);

    return panel;
  }

  private static TradeItemSearch parseSearchBySelector(Widget selector) {
    if (selector instanceof ListBox) {
      String value = ((ListBox) selector).getValue();

      if (!BeeUtils.isEmpty(value) && !BeeUtils.equalsTrim(value, BeeConst.STRING_ASTERISK)) {
        return EnumUtils.getEnumByName(TradeItemSearch.class, value);
      }
    }
    return null;
  }

  private void doSearch(String input, List<Widget> selectors) {
    if (getDocumentPhase() == null) {
      BeeKeeper.getScreen().notifyWarning(
          Localized.dictionary().fieldRequired(Localized.dictionary().trdDocumentPhase()));
      return;
    }

    if (getOperationType() == null) {
      BeeKeeper.getScreen().notifyWarning(
          Localized.dictionary().fieldRequired(Localized.dictionary().trdOperationType()));
      return;
    }

    List<TradeItemSearch> by = new ArrayList<>();

    for (int i = 0; i < selectors.size(); i++) {
      TradeItemSearch tis = parseSearchBySelector(selectors.get(i));

      if (tis == null) {
        BeeKeeper.getStorage().remove(searchByStorageKey(i));
      } else {
        BeeKeeper.getStorage().set(searchByStorageKey(i), tis.name());
      }

      if (tis != null && !by.contains(tis)) {
        by.add(tis);
      }
    }

    String query;
    List<TradeItemSearch> searchBy = new ArrayList<>();

    if (BeeUtils.isEmpty(input) || Operator.CHAR_ANY.equals(BeeUtils.trim(input))) {
      query = null;

      if (!BeeUtils.isEmpty(by)) {
        searchBy.addAll(by);
      }

    } else {
      query = BeeUtils.trim(input);

      if (!BeeUtils.isEmpty(by)) {
        List<String> errorMessages = new ArrayList<>();

        for (TradeItemSearch tis : by) {
          String message = tis.validate(query, Localized.dictionary());

          if (BeeUtils.isEmpty(message)) {
            searchBy.add(tis);
          } else {
            errorMessages.add(message);
          }
        }

        if (searchBy.isEmpty()) {
          BeeKeeper.getScreen().notifySevere(ArrayUtils.toArray(errorMessages));
          return;
        }
      }
    }

    if (searchBy.isEmpty() && !BeeUtils.isEmpty(query)) {
      searchBy.addAll(getDefaultSearchBy(query));
    }

    Filter filter = Filter.and(buildParentFilter(), buildSearchFilter(query, searchBy));

    addStyleName(STYLE_SEARCH_RUNNING);

    Queries.getRowSet(VIEW_ITEM_SELECTION, null, filter, new Queries.RowSetCallback() {
      @Override
      public void onFailure(String... reason) {
        removeStyleName(STYLE_SEARCH_RUNNING);
      }

      @Override
      public void onSuccess(BeeRowSet result) {
        if (DataUtils.isEmpty(result)) {
          logger.warning(filter, "not found");

        } else {
          logger.debug(filter, result.getNumberOfRows());
          renderItems(result, searchBy);
        }

        removeStyleName(STYLE_SEARCH_RUNNING);
      }
    });
  }

  private static Filter buildSearchFilter(String query, List<TradeItemSearch> searchBy) {
    if (BeeUtils.isEmpty(query) || BeeUtils.isEmpty(searchBy)) {
      return null;

    } else if (searchBy.size() == 1) {
      return searchBy.get(0).getItemFilter(query);

    } else {
      CompoundFilter filter = Filter.or();

      for (TradeItemSearch by : searchBy) {
        filter.add(by.getItemFilter(query));
      }
      return filter;
    }
  }

  private Filter buildParentFilter() {
    if (getDocumentPhase().modifyStock() && getOperationType().consumesStock()) {
      return Filter.or(Filter.notNull(COL_ITEM_IS_SERVICE),
          Filter.custom(FILTER_ITEM_HAS_STOCK, getWarehouse()));
    } else {
      return null;
    }
  }

  private static List<TradeItemSearch> getDefaultSearchBy(String query) {
    List<TradeItemSearch> searchBy = new ArrayList<>();

    if (DataUtils.isId(query)) {
      searchBy.add(TradeItemSearch.ID);
      searchBy.add(TradeItemSearch.NAME);
      searchBy.add(TradeItemSearch.ARTICLE);
      searchBy.add(TradeItemSearch.BARCODE);

    } else if (!BeeUtils.isEmpty(query)) {
      searchBy.add(TradeItemSearch.NAME);
      searchBy.add(TradeItemSearch.ARTICLE);
    }

    return searchBy;
  }

  private TradeDocumentPhase getDocumentPhase() {
    return documentPhase;
  }

  private void setDocumentPhase(TradeDocumentPhase documentPhase) {
    this.documentPhase = documentPhase;
  }

  private OperationType getOperationType() {
    return operationType;
  }

  private void setOperationType(OperationType operationType) {
    this.operationType = operationType;
  }

  private Long getWarehouse() {
    return warehouse;
  }

  private void setWarehouse(Long warehouse) {
    this.warehouse = warehouse;
  }

  private ItemPrice getItemPrice() {
    return itemPrice;
  }

  private void setItemPrice(ItemPrice itemPrice) {
    this.itemPrice = itemPrice;
  }

  private Long getCurrency() {
    return currency;
  }

  private void setCurrency(Long currency) {
    this.currency = currency;
  }

  private ItemPrice getItemPriceForRender(BeeRowSet items) {
    ItemPrice ip = getItemPrice();
    if (ip == null && getOperationType() != null) {
      ip = getOperationType().getDefaultPrice();
    }

    if (ip != null && DataUtils.isId(getCurrency())
        && items.containsColumn(ip.getPriceColumn())
        && items.containsColumn(ip.getCurrencyColumn())) {

      return ip;
    } else {
      return null;
    }
  }

  private static List<String> getItemColumnsForRender(BeeRowSet items,
      Collection<TradeItemSearch> by, ItemPrice ip) {

    List<String> columns = new ArrayList<>();

    if (items.containsColumn(ALS_ITEM_TYPE_NAME) && by.contains(TradeItemSearch.TYPE)) {
      columns.add(ALS_ITEM_TYPE_NAME);
    }
    if (items.containsColumn(ALS_ITEM_GROUP_NAME) && by.contains(TradeItemSearch.GROUP)) {
      columns.add(ALS_ITEM_GROUP_NAME);
    }

    if (items.containsColumn(COL_ITEM_NAME)) {
      columns.add(COL_ITEM_NAME);
    }
    if (items.containsColumn(COL_ITEM_NAME_2) && by.contains(TradeItemSearch.NAME_2)) {
      columns.add(COL_ITEM_NAME_2);
    }
    if (items.containsColumn(COL_ITEM_NAME_3) && by.contains(TradeItemSearch.NAME_3)) {
      columns.add(COL_ITEM_NAME_3);
    }

    if (items.containsColumn(COL_ITEM_ARTICLE)) {
      columns.add(COL_ITEM_ARTICLE);
    }
    if (items.containsColumn(COL_ITEM_ARTICLE_2) && by.contains(TradeItemSearch.ARTICLE_2)) {
      columns.add(COL_ITEM_ARTICLE);
    }
    if (items.containsColumn(COL_ITEM_BARCODE) && by.contains(TradeItemSearch.BARCODE)) {
      columns.add(COL_ITEM_BARCODE);
    }

    if (items.containsColumn(COL_ITEM_DESCRIPTION) && by.contains(TradeItemSearch.DESCRIPTION)) {
      columns.add(COL_ITEM_DESCRIPTION);
    }

    if (ip != null) {
      columns.add(ip.getPriceColumn());
    }

    if (items.containsColumn(ALS_UNIT_NAME)) {
      columns.add(ALS_UNIT_NAME);
    }

    return columns;
  }

  private static String getColumnLabel(BeeRowSet items, String column) {
    switch (column) {
      case ALS_ITEM_TYPE_NAME:
        return Localized.dictionary().type();
      case ALS_ITEM_GROUP_NAME:
        return Localized.dictionary().group();
      case ALS_UNIT_NAME:
        return Localized.dictionary().unitShort();

      default:
        return Localized.getLabel(items.getColumn(column));
    }
  }

  private static String getColumnStylePrefix(String column) {
    String styleName;

    switch (column) {
      case ALS_ITEM_TYPE_NAME:
        styleName = "type";
        break;

      case ALS_ITEM_GROUP_NAME:
        styleName = "group";
        break;

      case ALS_UNIT_NAME:
        styleName = "unit";
        break;

      default:
        styleName = column.toLowerCase();
    }

    return STYLE_PREFIX + styleName;
  }

  private static String render(BeeRowSet items, BeeRow item, String column) {
    switch (column) {
      case ALS_ITEM_TYPE_NAME:
        if (items.containsColumn(ALS_PARENT_TYPE_NAME)) {
          return BeeUtils.buildLines(DataUtils.getString(items, item, ALS_PARENT_TYPE_NAME),
              DataUtils.getString(items, item, column));
        }
        break;

      case ALS_ITEM_GROUP_NAME:
        if (items.containsColumn(ALS_PARENT_GROUP_NAME)) {
          return BeeUtils.buildLines(DataUtils.getString(items, item, ALS_PARENT_GROUP_NAME),
              DataUtils.getString(items, item, column));
        }
    }

    return DataUtils.getString(items, item, column);
  }

  private Widget renderQty(BeeColumn column, Double qty) {
    InputNumber input = new InputNumber();

    input.setMinValue(BeeConst.STRING_ZERO);

    if (column != null) {
      input.setMaxValue(DataUtils.getMaxValue(column));
      input.setScale(column.getScale());
      input.setMaxLength(UiHelper.getMaxLength(column));
    }

    input.addStyleName(STYLE_QTY_INPUT);

    if (BeeUtils.isPositive(qty)) {
      input.setValue(qty);
    }

    input.addChangeHandler(ensureQuantityChangeHandler());

    return input;
  }

  private void renderItems(BeeRowSet items, Collection<TradeItemSearch> searchBy) {
    ItemPrice ip = getItemPriceForRender(items);
    List<String> itemColumns = getItemColumnsForRender(items, searchBy, ip);

    Map<Long, String> warehouses = extractWarehouses(items);

    Map<String, Long> warehouseCodes = new TreeMap<>();
    warehouses.forEach((id, code) -> warehouseCodes.put(code, id));

    itemPanel.clear();
    HtmlTable table = new HtmlTable(STYLE_ITEM_TABLE);

    int r = 0;
    int c = 0;

    table.setText(r, c++, Localized.dictionary().captionId(), STYLE_ID + STYLE_HEADER_CELL_SUFFIX);

    for (String itemColumn : itemColumns) {
      table.setText(r, c++, getColumnLabel(items, itemColumn),
          getColumnStylePrefix(itemColumn) + STYLE_HEADER_CELL_SUFFIX);
    }

    for (Map.Entry<String, Long> entry : warehouseCodes.entrySet()) {
      table.setText(r, c, entry.getKey(), STYLE_STOCK + STYLE_HEADER_CELL_SUFFIX);

      if (Objects.equals(entry.getValue(), getWarehouse())) {
        table.getCellFormatter().addStyleName(r, c,
            STYLE_MAIN_WAREHOUSE + STYLE_HEADER_CELL_SUFFIX);
      }

      c++;
    }

    table.setText(r, c, Localized.dictionary().quantity(), STYLE_QTY + STYLE_HEADER_CELL_SUFFIX);

    table.getRowFormatter().addStyleName(r, STYLE_HEADER_ROW);

    BeeColumn qtyColumn = Data.getColumn(VIEW_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY);

    int serviceTagIndex = items.getColumnIndex(COL_ITEM_IS_SERVICE);

    r++;
    for (BeeRow item : items) {
      c = 0;

      table.setValue(r, c++, item.getId(), STYLE_ID + STYLE_CELL_SUFFIX);

      for (String itemColumn : itemColumns) {
        table.setText(r, c++, render(items, item, itemColumn),
            getColumnStylePrefix(itemColumn) + STYLE_CELL_SUFFIX);
      }

      for (Map.Entry<String, Long> entry : warehouseCodes.entrySet()) {
        String stock = item.getProperty(keyStockWarehouse(entry.getKey()));

        if (!BeeUtils.isEmpty(stock)) {
          String reserved = item.getProperty(keyReservedWarehouse(entry.getKey()));
          table.setText(r, c, BeeUtils.joinWords(stock, reserved), STYLE_STOCK + STYLE_CELL_SUFFIX);

          if (Objects.equals(entry.getValue(), getWarehouse())) {
            table.getCellFormatter().addStyleName(r, c, STYLE_MAIN_WAREHOUSE + STYLE_CELL_SUFFIX);
          }
        }

        c++;
      }

      Double qty = selection.get(item.getId());
      table.setWidget(r, c, renderQty(qtyColumn, qty), STYLE_QTY + STYLE_CELL_SUFFIX);

      DomUtils.setDataIndex(table.getRow(r), item.getId());

      table.getRowFormatter().addStyleName(r, STYLE_ITEM_ROW);
      if (BeeUtils.isPositive(qty)) {
        table.getRowFormatter().addStyleName(r, STYLE_SELECTED_ROW);
      }

      if (item.isTrue(serviceTagIndex)) {
        table.getRowFormatter().addStyleName(r, STYLE_SERVICE_ROW);
      }

      r++;
    }

    itemPanel.add(table);
  }

  private ChangeHandler ensureQuantityChangeHandler() {
    if (quantityChangeHandler == null) {
      quantityChangeHandler = event -> {
        if (event.getSource() instanceof InputNumber) {
          InputNumber input = (InputNumber) event.getSource();
          onQuantityChange(input.getElement(), input.getNumber());
        }
      };
    }
    return quantityChangeHandler;
  }

  private void onQuantityChange(Element source, Double qty) {
    TableRowElement row = DomUtils.getParentRow(source, true);
    long id = DomUtils.getDataIndexLong(row);

    if (DataUtils.isId(id)) {
      if (BeeUtils.isPositive(qty)) {
        row.addClassName(STYLE_SELECTED_ROW);
        selection.put(id, qty);

      } else {
        row.removeClassName(STYLE_SELECTED_ROW);
        selection.remove(id);
      }
    }
  }

  private static Map<Long, String> extractWarehouses(BeeRowSet items) {
    Map<Long, String> warehouses = new HashMap<>();

    String serialized = (items == null) ? null : items.getTableProperty(PROP_WAREHOUSES);
    if (!BeeUtils.isEmpty(serialized)) {
      Map<String, String> map = Codec.deserializeHashMap(serialized);

      map.forEach((id, code) -> {
        if (DataUtils.isId(id) && !BeeUtils.isEmpty(code)) {
          warehouses.put(BeeUtils.toLong(id), code);
        }
      });
    }

    return warehouses;
  }
}
