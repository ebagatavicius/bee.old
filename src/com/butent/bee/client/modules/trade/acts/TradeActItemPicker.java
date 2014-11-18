package com.butent.bee.client.modules.trade.acts;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.OpenEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.Span;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActKind;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TradeActItemPicker extends Flow implements HasSelectionHandlers<BeeRowSet> {

  private static final String STYLE_PREFIX = TradeActKeeper.STYLE_PREFIX + "picker-";
  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog";
  private static final String STYLE_SAVE = STYLE_PREFIX + "save";
  private static final String STYLE_CLOSE = STYLE_PREFIX + "close";

  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";

  private static final String STYLE_SEARCH_PREFIX = STYLE_PREFIX + "search-";
  private static final String STYLE_SEARCH_PANEL = STYLE_SEARCH_PREFIX + "panel";
  private static final String STYLE_SEARCH_BY = STYLE_SEARCH_PREFIX + "by";
  private static final String STYLE_SEARCH_BOX = STYLE_SEARCH_PREFIX + "box";
  private static final String STYLE_SEARCH_COMMAND = STYLE_SEARCH_PREFIX + "command";

  private static final String STYLE_ITEM_PANEL = STYLE_PREFIX + "item-panel";
  private static final String STYLE_ITEM_TABLE = STYLE_PREFIX + "item-table";

  private static final String STYLE_HEADER_ROW = STYLE_PREFIX + "header";
  private static final String STYLE_ITEM_ROW = STYLE_PREFIX + "item";
  private static final String STYLE_SELECTED_ROW = STYLE_PREFIX + "item-selected";

  private static final String STYLE_HEADER_CELL_SUFFIX = "label";
  private static final String STYLE_CELL_SUFFIX = "cell";

  private static final String STYLE_ID_PREFIX = STYLE_PREFIX + "id-";

  private static final String STYLE_TYPE_PREFIX = STYLE_PREFIX + "type-";
  private static final String STYLE_GROUP_PREFIX = STYLE_PREFIX + "group-";
  private static final String STYLE_NAME_PREFIX = STYLE_PREFIX + "name-";
  private static final String STYLE_ARTICLE_PREFIX = STYLE_PREFIX + "article-";

  private static final String STYLE_PRICE_PREFIX = STYLE_PREFIX + "price-";
  private static final String STYLE_PRICE_WRAPPER = STYLE_PRICE_PREFIX + "wrapper";
  private static final String STYLE_PRICE_VALUE = STYLE_PRICE_PREFIX + "value";
  private static final String STYLE_PRICE_CURRENCY = STYLE_PRICE_PREFIX + "currency";

  private static final String STYLE_PRICE_HEADER_CELL = STYLE_PRICE_PREFIX
      + STYLE_HEADER_CELL_SUFFIX;
  private static final String STYLE_PRICE_CELL = STYLE_PRICE_PREFIX + STYLE_CELL_SUFFIX;

  private static final String STYLE_SELECTED_PRICE_PREFIX = STYLE_PREFIX + "sel-price-";
  private static final String STYLE_SELECTED_PRICE_HEADER_CELL = STYLE_SELECTED_PRICE_PREFIX
      + STYLE_HEADER_CELL_SUFFIX;
  private static final String STYLE_SELECTED_PRICE_CELL = STYLE_SELECTED_PRICE_PREFIX
      + STYLE_CELL_SUFFIX;

  private static final String STYLE_STOCK_PREFIX = STYLE_PREFIX + "stock-";
  private static final String STYLE_STOCK_POSITIVE = STYLE_STOCK_PREFIX + "positive";
  private static final String STYLE_STOCK_NEGATIVE = STYLE_STOCK_PREFIX + "negative";

  private static final String STYLE_FROM_PREFIX = STYLE_PREFIX + "from-";

  private static final String STYLE_QTY_PREFIX = STYLE_PREFIX + "qty-";
  private static final String STYLE_QTY_INPUT = STYLE_QTY_PREFIX + "input";

  private static final List<String> SEARCH_COLUMNS = Lists.newArrayList(COL_ITEM_NAME,
      COL_ITEM_ARTICLE, COL_ITEM_TYPE, COL_ITEM_GROUP);

  private static Filter buildFilter(String by, String query) {
    Filter filter = null;

    if (COL_ITEM.equals(by)) {
      Long id = BeeUtils.toLongOrNull(query);
      if (DataUtils.isId(id)) {
        filter = Filter.compareId(id);
      }

    } else if (SEARCH_COLUMNS.contains(by)) {
      switch (by) {
        case COL_ITEM_NAME:
        case COL_ITEM_ARTICLE:
          filter = condition(by, query);
          break;

        case COL_ITEM_TYPE:
          filter = Filter.or(condition(ALS_PARENT_TYPE_NAME, query),
              condition(ALS_ITEM_TYPE_NAME, query));
          break;
        case COL_ITEM_GROUP:
          filter = Filter.or(condition(ALS_PARENT_GROUP_NAME, query),
              condition(ALS_ITEM_GROUP_NAME, query));
          break;
      }

    } else {
      List<Filter> conditions = new ArrayList<>();

      for (String column : SEARCH_COLUMNS) {
        conditions.add(buildFilter(column, query));
      }
      if (DataUtils.isId(query)) {
        conditions.add(Filter.compareId(BeeUtils.toLong(query)));
      }

      filter = Filter.or(conditions);
    }

    return filter;
  }

  private static Filter condition(String column, String query) {
    Operator operator;
    String value;

    if (query.contains(Operator.CHAR_ANY) || query.contains(Operator.CHAR_ONE)) {
      operator = Operator.MATCHES;
      value = query;

    } else if (BeeUtils.isPrefixOrSuffix(query, BeeConst.CHAR_EQ)) {
      operator = Operator.EQ;
      value = BeeUtils.removePrefixAndSuffix(query, BeeConst.CHAR_EQ);

    } else {
      operator = Operator.CONTAINS;
      value = query;
    }

    return Filter.compareWithValue(column, operator, new TextValue(value));
  }

  private static void onQuantityChange(Element source, Double qty) {
    TableRowElement row = DomUtils.getParentRow(source, true);

    if (row != null) {
      if (BeeUtils.isPositive(qty)) {
        row.addClassName(STYLE_SELECTED_ROW);
      } else {
        row.removeClassName(STYLE_SELECTED_ROW);
      }
    }
  }

  private IsRow lastTaRow;

  private ItemPrice itemPrice;
  private Long warehouseFrom;

  private BeeRowSet items;

  private final Map<Long, ItemPrice> selectedPrices = new HashMap<>();

  private final Flow itemPanel = new Flow(STYLE_ITEM_PANEL);

  private NumberFormat priceFormat;

  private ChangeHandler quantityChangeHandler;

  TradeActItemPicker() {
    super(STYLE_CONTAINER);

    add(createSearch());
    add(itemPanel);

    itemPanel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Element target = EventUtils.getEventTargetElement(event);
        if (target != null) {
          onCellClick(target);
        }
      }
    });
  }

  @Override
  public HandlerRegistration addSelectionHandler(SelectionHandler<BeeRowSet> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  void show(IsRow taRow, Element target) {
    lastTaRow = DataUtils.cloneRow(taRow);

    warehouseFrom = TradeActKeeper.getWarehouseFrom(VIEW_TRADE_ACTS, taRow);
    itemPrice = TradeActKeeper.getItemPrice(VIEW_TRADE_ACTS, taRow);

    items = null;

    if (!selectedPrices.isEmpty()) {
      selectedPrices.clear();
    }

    if (!itemPanel.isEmpty()) {
      itemPanel.clear();
    }

    openDialog(target);
  }

  private Widget createSearch() {
    Flow panel = new Flow(STYLE_SEARCH_PANEL);

    final ListBox searchBy = new ListBox();
    searchBy.addStyleName(STYLE_SEARCH_BY);

    searchBy.addItem(BeeConst.STRING_EMPTY, BeeConst.STRING_ASTERISK);
    for (String column : SEARCH_COLUMNS) {
      searchBy.addItem(Data.getColumnLabel(VIEW_ITEMS, column), column);
    }
    searchBy.addItem(Localized.getConstants().captionId(), COL_ITEM);

    panel.add(searchBy);

    final InputText searchBox = new InputText();
    DomUtils.setSearch(searchBox);
    searchBox.setMaxLength(20);
    searchBox.addStyleName(STYLE_SEARCH_BOX);

    searchBox.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          String query = BeeUtils.trim(searchBox.getValue());
          if (!BeeUtils.isEmpty(query)) {
            doSearch(searchBy.getValue(), query);
          }
        }
      }
    });

    panel.add(searchBox);

    FaLabel searchCommand = new FaLabel(FontAwesome.SEARCH, STYLE_SEARCH_COMMAND);

    searchCommand.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        doSearch(searchBy.getValue(), BeeUtils.trim(searchBox.getValue()));
      }
    });

    panel.add(searchCommand);

    return panel;
  }

  private void doSearch(String by, String query) {
    Filter filter = null;
    boolean ok;

    if (BeeUtils.isEmpty(query) || Operator.CHAR_ANY.equals(query)) {
      ok = true;

    } else if (COL_ITEM.equals(by) && !DataUtils.isId(query)) {
      BeeKeeper.getScreen().notifyWarning(
          BeeUtils.joinWords(Localized.getConstants().invalidIdValue(), query));
      ok = false;

    } else {
      filter = buildFilter(by, query);
      ok = true;
    }

    if (ok) {
      getItems(filter, new RowSetCallback() {
        @Override
        public void onSuccess(BeeRowSet result) {
          Map<Long, Double> quantities = getQuantities();

          Collection<Long> whs = TradeActKeeper.extractWarehouses(result);

          if (quantities.isEmpty() || DataUtils.isEmpty(items)) {
            items = result;

          } else {
            whs.addAll(TradeActKeeper.extractWarehouses(items));

            List<BeeRow> rows = new ArrayList<>();

            for (BeeRow row : items) {
              if (quantities.containsKey(row.getId())) {
                rows.add(row);
              }
            }
            for (BeeRow row : result) {
              if (!quantities.containsKey(row.getId())) {
                rows.add(row);
              }
            }

            items.setRows(rows);
          }

          Map<Long, String> warehouses = TradeActKeeper.getWarehouses(whs);
          renderItems(quantities, warehouses);

          Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
              Popup popup = UiHelper.getParentPopup(TradeActItemPicker.this);
              if (popup != null) {
                popup.onResize();
              }
            }
          });
        }
      });
    }
  }

  private NumberFormat ensurePriceFormat() {
    if (priceFormat == null) {
      Integer scale = Data.getColumnScale(VIEW_ITEMS, COL_ITEM_PRICE);
      if (scale == null || scale <= 2) {
        priceFormat = Format.getDefaultMoneyFormat();
      } else {
        priceFormat = Format.getDecimalFormat(2, scale);
      }
    }
    return priceFormat;
  }

  private ChangeHandler ensureQuantityChangeHandler() {
    if (quantityChangeHandler == null) {
      quantityChangeHandler = new ChangeHandler() {
        @Override
        public void onChange(ChangeEvent event) {
          if (event.getSource() instanceof InputNumber) {
            InputNumber input = (InputNumber) event.getSource();
            onQuantityChange(input.getElement(), input.getNumber());
          }
        }
      };
    }
    return quantityChangeHandler;
  }

  private void getItems(Filter filter, final RowSetCallback callback) {
    ParameterList params = TradeActKeeper.createArgs(SVC_GET_ITEMS_FOR_SELECTION);

    if (DataUtils.hasId(lastTaRow)) {
      params.addDataItem(COL_TRADE_ACT, lastTaRow.getId());
    }

    TradeActKind kind = TradeActKeeper.getKind(VIEW_TRADE_ACTS, lastTaRow);
    if (kind != null) {
      params.addDataItem(COL_TA_KIND, kind.ordinal());
    }

    if (DataUtils.isId(warehouseFrom)) {
      params.addDataItem(COL_WAREHOUSE, warehouseFrom);
    }

    if (filter != null) {
      params.addDataItem(Service.VAR_VIEW_WHERE, filter.serialize());
    }

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(BeeRowSet.class)) {
          callback.onSuccess(BeeRowSet.restore(response.getResponseAsString()));
        } else {
          BeeKeeper.getScreen().notifyWarning(Localized.getConstants().nothingFound());
        }
      }
    });
  }

  private Map<Long, Double> getQuantities() {
    Map<Long, Double> result = new HashMap<>();
    if (itemPanel.isEmpty()) {
      return result;
    }

    Collection<InputNumber> inputs = UiHelper.getChildren(itemPanel, InputNumber.class);

    for (InputNumber input : inputs) {
      Double qty = input.getNumber();

      if (BeeUtils.isPositive(qty)) {
        long id = DomUtils.getDataIndexLong(DomUtils.getParentRow(input.getElement(), false));
        if (DataUtils.isId(id)) {
          result.put(id, qty);
        }
      }
    }

    return result;
  }

  private boolean isFrom(Long warehouse) {
    return warehouseFrom != null && warehouseFrom.equals(warehouse);
  }

  private void onCellClick(Element source) {
    TableCellElement cell = DomUtils.getParentCell(source, true);
    if (cell == null) {
      return;
    }

    if (cell.hasClassName(STYLE_FROM_PREFIX + STYLE_CELL_SUFFIX)) {
      String text = cell.getInnerText();
      Double stock = BeeUtils.toDoubleOrNull(text);

      if (BeeUtils.isPositive(stock)) {
        Element target = Selectors.getElementByClassName(cell.getParentElement(), STYLE_QTY_INPUT);

        if (InputElement.is(target)) {
          InputElement input = InputElement.as(target);
          if (!BeeUtils.isPositiveDouble(input.getValue())) {
            input.setValue(text);
            onQuantityChange(input, stock);
          }
        }
      }

    } else if (cell.hasClassName(STYLE_PRICE_HEADER_CELL)) {
      ItemPrice ip = EnumUtils.getEnumByIndex(ItemPrice.class, DomUtils.getDataColumnInt(cell));

      if (ip != null && itemPrice != ip) {
        if (itemPrice != null) {
          TableRowElement row = DomUtils.getParentRow(cell, false);
          Element el = Selectors.getElementByClassName(row, STYLE_SELECTED_PRICE_HEADER_CELL);

          if (el != null) {
            el.removeClassName(STYLE_SELECTED_PRICE_HEADER_CELL);
            el.addClassName(STYLE_PRICE_HEADER_CELL);
          }
        }

        cell.removeClassName(STYLE_PRICE_HEADER_CELL);
        cell.addClassName(STYLE_SELECTED_PRICE_HEADER_CELL);

        List<Element> rows = Selectors.getElementsByClassName(DomUtils.getParentTable(cell, false),
            STYLE_ITEM_ROW);

        String priceSelector = Selectors.conjunction(Selectors.classSelector(STYLE_PRICE_CELL),
            Selectors.attributeEquals(DomUtils.ATTRIBUTE_DATA_COLUMN, ip.ordinal()));

        for (Element row : rows) {
          if (!selectedPrices.containsKey(DomUtils.getDataIndexLong(row))) {
            if (itemPrice != null) {
              deselectPrice(row);
            }

            Element el = Selectors.getElement(row, priceSelector);
            if (el != null) {
              el.removeClassName(STYLE_PRICE_CELL);
              el.addClassName(STYLE_SELECTED_PRICE_CELL);
            }
          }
        }

        itemPrice = ip;
      }

    } else if (cell.hasClassName(STYLE_PRICE_CELL)) {
      ItemPrice ip = EnumUtils.getEnumByIndex(ItemPrice.class, DomUtils.getDataColumnInt(cell));

      TableRowElement row = DomUtils.getParentRow(cell, false);
      long item = DomUtils.getDataIndexLong(row);

      if (ip != null && DataUtils.isId(item)) {
        deselectPrice(row);

        cell.removeClassName(STYLE_PRICE_CELL);
        cell.addClassName(STYLE_SELECTED_PRICE_CELL);

        selectedPrices.put(item, ip);
      }
    }
  }

  private static void deselectPrice(Element row) {
    Element cell = Selectors.getElementByClassName(row, STYLE_SELECTED_PRICE_CELL);

    if (cell != null) {
      cell.removeClassName(STYLE_SELECTED_PRICE_CELL);
      cell.addClassName(STYLE_PRICE_CELL);
    }
  }

  private void openDialog(Element target) {
    final DialogBox dialog = DialogBox.withoutCloseBox(Localized.getConstants().goods(),
        STYLE_DIALOG);

    Image save = new Image(Global.getImages().silverSave());
    save.addStyleName(STYLE_SAVE);

    save.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Map<Long, Double> quantities = getQuantities();
        if (!quantities.isEmpty()) {
          selectItems(quantities);
        }

        dialog.close();
      }
    });

    dialog.addAction(Action.SAVE, save);

    Image close = new Image(Global.getImages().silverClose());
    close.addStyleName(STYLE_CLOSE);

    close.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        final Map<Long, Double> quantities = getQuantities();

        if (quantities.isEmpty()) {
          dialog.close();

        } else {
          Global.decide(Localized.getConstants().goods(),
              Lists.newArrayList(Localized.getConstants().taSaveSelectedItems()),
              new DecisionCallback() {
                @Override
                public void onConfirm() {
                  selectItems(quantities);
                  dialog.close();
                }

                @Override
                public void onDeny() {
                  dialog.close();
                }
              }, DialogConstants.DECISION_YES);
        }
      }
    });

    dialog.addAction(Action.CLOSE, close);

    dialog.addOpenHandler(new OpenEvent.Handler() {
      @Override
      public void onOpen(OpenEvent event) {
        Widget searchBox = UiHelper.getChildByStyleName(TradeActItemPicker.this, STYLE_SEARCH_BOX);
        if (searchBox instanceof Editor) {
          ((Editor) searchBox).clearValue();
          ((Editor) searchBox).setFocus(true);
        }
      }
    });

    dialog.setWidget(this);
    dialog.showOnTop(target);
  }

  private void renderItems(Map<Long, Double> quantities, Map<Long, String> warehouses) {
    List<Long> warehouseIds = new ArrayList<>();
    if (!BeeUtils.isEmpty(warehouses)) {
      warehouseIds.addAll(warehouses.keySet());
    }

    itemPanel.clear();

    HtmlTable table = new HtmlTable(STYLE_ITEM_TABLE);

    int r = 0;
    int c = 0;

    String pfx;

    table.setText(r, c++, Localized.getConstants().captionId(),
        STYLE_ID_PREFIX + STYLE_HEADER_CELL_SUFFIX);

    table.setText(r, c++, Localized.getConstants().type(),
        STYLE_TYPE_PREFIX + STYLE_HEADER_CELL_SUFFIX);
    table.setText(r, c++, Localized.getConstants().group(),
        STYLE_GROUP_PREFIX + STYLE_HEADER_CELL_SUFFIX);

    table.setText(r, c++, Localized.getConstants().name(),
        STYLE_NAME_PREFIX + STYLE_HEADER_CELL_SUFFIX);
    table.setText(r, c++, Localized.getConstants().article(),
        STYLE_ARTICLE_PREFIX + STYLE_HEADER_CELL_SUFFIX);

    for (ItemPrice ip : ItemPrice.values()) {
      table.setText(r, c, ip.getCaption(),
          (ip == itemPrice) ? STYLE_SELECTED_PRICE_HEADER_CELL : STYLE_PRICE_HEADER_CELL);

      DomUtils.setDataColumn(table.getCellFormatter().getElement(r, c), ip.ordinal());
      c++;
    }

    for (Long w : warehouseIds) {
      pfx = isFrom(w) ? STYLE_FROM_PREFIX : STYLE_STOCK_PREFIX;
      table.setText(r, c++, warehouses.get(w), pfx + STYLE_HEADER_CELL_SUFFIX);
    }

    table.setText(r, c++, Localized.getConstants().quantity(),
        STYLE_QTY_PREFIX + STYLE_HEADER_CELL_SUFFIX);

    table.getRowFormatter().addStyleName(r, STYLE_HEADER_ROW);

    List<Integer> typeIndexes = Lists.newArrayList(items.getColumnIndex(ALS_PARENT_TYPE_NAME),
        items.getColumnIndex(ALS_ITEM_TYPE_NAME));
    List<Integer> groupIndexes = Lists.newArrayList(items.getColumnIndex(ALS_PARENT_GROUP_NAME),
        items.getColumnIndex(ALS_ITEM_GROUP_NAME));

    int nameIndex = items.getColumnIndex(COL_ITEM_NAME);
    int articleIndex = items.getColumnIndex(COL_ITEM_ARTICLE);

    EnumMap<ItemPrice, Integer> priceIndexes = new EnumMap<>(ItemPrice.class);
    EnumMap<ItemPrice, Integer> currencyIndexes = new EnumMap<>(ItemPrice.class);

    for (ItemPrice ip : ItemPrice.values()) {
      priceIndexes.put(ip, items.getColumnIndex(ip.getPriceColumn()));
      currencyIndexes.put(ip, items.getColumnIndex(ip.getCurrencyNameAlias()));
    }

    BeeColumn qtyColumn = Data.getColumn(VIEW_TRADE_ACT_ITEMS,
        TradeConstants.COL_TRADE_ITEM_QUANTITY);

    r++;
    for (BeeRow item : items) {
      c = 0;

      table.setText(r, c++, BeeUtils.toString(item.getId()),
          STYLE_ID_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, DataUtils.join(items.getColumns(), item, typeIndexes,
          BeeConst.STRING_EOL), STYLE_TYPE_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, DataUtils.join(items.getColumns(), item, groupIndexes,
          BeeConst.STRING_EOL), STYLE_GROUP_PREFIX + STYLE_CELL_SUFFIX);

      table.setText(r, c++, item.getString(nameIndex),
          STYLE_NAME_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(r, c++, item.getString(articleIndex),
          STYLE_ARTICLE_PREFIX + STYLE_CELL_SUFFIX);

      ItemPrice defPrice = selectedPrices.containsKey(item.getId())
          ? selectedPrices.get(item.getId()) : itemPrice;

      for (ItemPrice ip : ItemPrice.values()) {
        String html = renderPrice(item, priceIndexes.get(ip), currencyIndexes.get(ip));

        if (html == null) {
          table.setText(r, c, BeeConst.STRING_EMPTY);
        } else {
          table.setHtml(r, c, html,
              (ip == defPrice) ? STYLE_SELECTED_PRICE_CELL : STYLE_PRICE_CELL);
          DomUtils.setDataColumn(table.getCellFormatter().getElement(r, c), ip.ordinal());
        }
        c++;
      }

      if (!warehouseIds.isEmpty() && !BeeUtils.isEmpty(item.getProperties())) {
        for (Map.Entry<String, String> entry : item.getProperties().entrySet()) {
          if (BeeUtils.isPrefix(entry.getKey(), PRP_WAREHOUSE_PREFIX)) {
            Long w = BeeUtils.toLongOrNull(BeeUtils.removePrefix(entry.getKey(),
                PRP_WAREHOUSE_PREFIX));

            if (DataUtils.isId(w) && warehouseIds.contains(w)) {
              Double stock = BeeUtils.toDouble(entry.getValue());
              pfx = (isFrom(w) && BeeUtils.isPositive(stock))
                  ? STYLE_FROM_PREFIX : STYLE_STOCK_PREFIX;

              table.setHtml(r, c + warehouseIds.indexOf(w),
                  renderStock(w, entry.getValue(), stock), pfx + STYLE_CELL_SUFFIX);
            }
          }
        }
      }

      c += warehouseIds.size();

      Double qty = quantities.get(item.getId());
      table.setWidget(r, c, renderQty(qtyColumn, qty), STYLE_QTY_PREFIX + STYLE_CELL_SUFFIX);

      DomUtils.setDataIndex(table.getRow(r), item.getId());

      table.getRowFormatter().addStyleName(r, STYLE_ITEM_ROW);
      if (BeeUtils.isPositive(qty)) {
        table.getRowFormatter().addStyleName(r, STYLE_SELECTED_ROW);
      }

      r++;
    }

    itemPanel.add(table);
  }

  private String renderPrice(BeeRow item, int priceIndex, int currencyIndex) {
    Double price = item.getDouble(priceIndex);

    if (BeeUtils.isDouble(price)) {
      Div div = new Div().addClass(STYLE_PRICE_WRAPPER);
      div.append(new Span().addClass(STYLE_PRICE_VALUE).text(ensurePriceFormat().format(price)));

      String currency = item.getString(currencyIndex);
      if (!BeeUtils.isEmpty(currency)) {
        div.append(new Span().addClass(STYLE_PRICE_CURRENCY).text(currency));
      }

      return div.build();

    } else {
      return null;
    }
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

  private String renderStock(Long warehouse, String text, Double stock) {
    Div div = new Div().text(text);

    if (BeeUtils.isPositive(stock)) {
      div.addClass(STYLE_STOCK_POSITIVE);
      if (isFrom(warehouse)) {
        div.title(Localized.getConstants().actionSelect());
      }

    } else if (BeeUtils.isNegative(stock)) {
      div.addClass(STYLE_STOCK_NEGATIVE);
    }

    return div.build();
  }

  private void selectItems(Map<Long, Double> quantities) {
    BeeRowSet selection = new BeeRowSet(items.getViewName(), items.getColumns());

    for (BeeRow item : items) {
      if (quantities.containsKey(item.getId())) {
        BeeRow row = DataUtils.cloneRow(item);
        row.setProperty(PRP_QUANTITY, BeeUtils.toString(quantities.get(item.getId())));

        ItemPrice ip = selectedPrices.get(item.getId());
        if (ip != null && ip != itemPrice) {
          row.setProperty(PRP_ITEM_PRICE, BeeUtils.toString(ip.ordinal()));
        }

        selection.addRow(row);
      }
    }

    if (itemPrice != null) {
      selection.setTableProperty(PRP_ITEM_PRICE, BeeUtils.toString(itemPrice.ordinal()));
    }

    SelectionEvent.fire(this, selection);
  }
}