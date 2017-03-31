package com.butent.bee.client.modules.trade;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Storage;
import com.butent.bee.client.data.ClientDefaults;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dialog.Notification;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.ScopeChangeEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.classifiers.ClassifierKeeper;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.navigation.HasPaging;
import com.butent.bee.client.view.navigation.SimplePager;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.DoubleLabel;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDiscountMode;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeDocumentSums;
import com.butent.bee.shared.modules.trade.TradeItemSearch;
import com.butent.bee.shared.modules.trade.TradeVatMode;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.NavigationOrigin;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TradeItemPicker extends Flow implements HasPaging {

  private static BeeLogger logger = LogUtils.getLogger(TradeItemPicker.class);

  private static final String STYLE_NAME = TradeKeeper.STYLE_PREFIX + "item-picker";
  private static final String STYLE_PREFIX = STYLE_NAME + "-";

  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog";
  private static final String STYLE_NEW_ITEM = STYLE_PREFIX + "new-item";
  private static final String STYLE_NEW_SERVICE = STYLE_PREFIX + "new-service";
  private static final String STYLE_SAVE = STYLE_PREFIX + "save";
  private static final String STYLE_CLOSE = STYLE_PREFIX + "close";

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
  private static final String STYLE_FOOTER_ROW = STYLE_PREFIX + "footer";

  private static final String STYLE_ID = STYLE_PREFIX + "id";
  private static final String STYLE_STOCK_CONTAINER = STYLE_PREFIX + "stock-container";
  private static final String STYLE_STOCK = STYLE_PREFIX + "stock";
  private static final String STYLE_RESERVED = STYLE_PREFIX + "reserved";
  private static final String STYLE_AVAILABLE = STYLE_PREFIX + "available";
  private static final String STYLE_MAIN_WAREHOUSE = STYLE_PREFIX + "main-warehouse";
  private static final String STYLE_QTY = STYLE_PREFIX + "qty";
  private static final String STYLE_PRICE = STYLE_PREFIX + "price";
  private static final String STYLE_DISCOUNT = STYLE_PREFIX + "discount";
  private static final String STYLE_VAT = STYLE_PREFIX + "vat";
  private static final String STYLE_TOTAL = STYLE_PREFIX + "total";

  private static final String STYLE_HEADER_CELL_SUFFIX = "-label";
  private static final String STYLE_CELL_SUFFIX = "-cell";
  private static final String STYLE_FOOTER_CELL_SUFFIX = "-footer";

  private static final String STYLE_QTY_INPUT = STYLE_QTY + "-input";

  private static final String STYLE_EMPTY = STYLE_PREFIX + "empty";
  private static final String STYLE_PAGER = STYLE_PREFIX + "pager";

  private static final int SEARCH_BY_SIZE = 3;

  private static final String SEARCH_BY_STORAGE_PREFIX =
      NameUtils.getClassName(TradeItemPicker.class) + "-by";

  private static final String KEY_AVAILABLE = "avail";
  private static final String KEY_PRICE = "price";

  private static final int itemPriceScale = Data.getColumnScale(VIEW_ITEMS, COL_ITEM_PRICE);
  private static final int costScale = Data.getColumnScale(VIEW_TRADE_ITEM_COST,
      COL_TRADE_ITEM_COST);
  private static final int priceScale = Data.getColumnScale(VIEW_TRADE_DOCUMENT_ITEMS,
      COL_TRADE_ITEM_PRICE);

  private static final NumberFormat itemPriceFormat = Format.getDecimalFormat(2, itemPriceScale);
  private static final NumberFormat costFormat = Format.getDecimalFormat(2, costScale);
  private static final NumberFormat priceFormat = Format.getDecimalFormat(2, priceScale);

  private static final NumberFormat amountFormat = Format.getDefaultMoneyFormat();
  private static final NumberFormat totalFormat = Format.getDefaultMoneyFormat();

  private final Flow itemPanel = new Flow(STYLE_ITEM_PANEL);
  private final Notification notification = new Notification();

  private TradeDocumentPhase documentPhase;
  private OperationType operationType;
  private Long warehouse;

  private ItemPrice itemPrice;

  private DateTime date;
  private Long currency;
  private String currencyName;

  private final TradeDocumentSums tds = new TradeDocumentSums();
  private final List<BeeRow> selectedItems = new ArrayList<>();

  private Double defaultVatPercent;

  private ChangeHandler quantityChangeHandler;
  private KeyDownHandler quantityKeyDownHandler;

  private BeeRowSet data;

  private Filter filter;
  private final List<TradeItemSearch> filterBy = new ArrayList<>();

  private final SimplePager pager = new SimplePager(999);

  private int pageSize = BeeConst.UNDEF;
  private int pageStart;
  private int rowCount = BeeConst.UNDEF;

  private final Map<String, String> priceCalculationOptions = new HashMap<>();

  private State state;

  private TradeItemPicker() {
    super(STYLE_NAME);
    addStyleName(STYLE_EMPTY);

    add(createSearch());
    add(itemPanel);

    pager.addStyleName(STYLE_PAGER);
    add(pager);

    add(notification);

    itemPanel.addClickHandler(event -> {
      Element target = EventUtils.getEventTargetElement(event);
      TableCellElement cell = DomUtils.getParentCell(target, true);

      if (cell != null) {
        onCellClick(cell);
      }
    });
  }

  TradeItemPicker(IsRow documentRow, Double defaultVatPercent) {
    this();

    setDocumentRow(documentRow);
    setDefaultVatPercent(defaultVatPercent);
  }

  public TradeItemPicker(TradeDocumentPhase documentPhase, OperationType operationType,
      Long warehouse, ItemPrice itemPrice, DateTime date, Long currency, String currencyName,
      TradeDiscountMode discountMode, Double documentDiscount, TradeVatMode vatMode,
      Map<String, String> priceCalculationOptions, Double defaultVatPercent) {
    this();

    setDocumentPhase(documentPhase);
    setOperationType(operationType);
    setWarehouse(warehouse);

    setItemPrice(itemPrice);

    setDate(date);
    setCurrency(currency);
    setCurrencyName(currencyName);

    setDiscountMode(discountMode);
    setDocumentDiscount(documentDiscount);
    setVatMode(vatMode);

    if (priceCalculationOptions != null) {
      this.priceCalculationOptions.putAll(priceCalculationOptions);
    }

    setDefaultVatPercent(defaultVatPercent);
  }

  public void open(BiConsumer<Collection<BeeRow>, TradeDocumentSums> selectionConsumer) {
    if (selectionConsumer == null) {
      logger.severe(NameUtils.getName(this), "selection consumer is null");
      return;
    }

    final DialogBox dialog = DialogBox.withoutCloseBox(Localized.dictionary().itemSelection(),
        STYLE_DIALOG);

    if (!needsStock()) {
      Button newItem = new Button(Localized.dictionary().newItem());
      newItem.addStyleName(STYLE_NEW_ITEM);

      newItem.addClickHandler(event -> createNew(false));

      dialog.addCommand(newItem);
    }

    Button newService = new Button(Localized.dictionary().newService());
    newService.addStyleName(STYLE_NEW_SERVICE);

    newService.addClickHandler(event -> createNew(true));

    dialog.addCommand(newService);

    FaLabel save = new FaLabel(FontAwesome.SAVE, STYLE_SAVE);

    save.addClickHandler(event -> {
      if (getState() == State.UPDATING) {
        setSavePending();

      } else if (getState() != State.CLOSING) {
        setState(State.CLOSING);

        if (hasSelection()) {
          selectionConsumer.accept(getSelectedItems(), getTds());
        }
        dialog.close();
      }
    });

    dialog.addAction(Action.SAVE, save);

    FaLabel close = new FaLabel(FontAwesome.CLOSE, STYLE_CLOSE);

    close.addClickHandler(event -> {
      if (hasSelection()) {
        Global.decide(Localized.dictionary().itemSelection(),
            Collections.singletonList(Localized.dictionary().saveSelectedItems()),
            new DecisionCallback() {
              @Override
              public void onCancel() {
                focusSearchBox();
              }

              @Override
              public void onConfirm() {
                selectionConsumer.accept(getSelectedItems(), getTds());
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

    dialog.setWidget(this);

    dialog.addOpenHandler(event -> {
      setState(State.OPEN);
      focusSearchBox();
    });

    dialog.addCloseHandler(event -> setState(State.CLOSED));

    dialog.center();
  }

  public List<BeeRow> getSelectedItems() {
    return selectedItems;
  }

  public TradeDocumentSums getTds() {
    return tds;
  }

  public boolean hasSelection() {
    return !selectedItems.isEmpty();
  }

  void setDocumentRow(IsRow row) {
    setDocumentPhase(TradeUtils.getDocumentPhase(row));
    setOperationType(TradeUtils.getDocumentOperationType(row));
    setWarehouse(TradeUtils.getDocumentRelation(row, COL_TRADE_WAREHOUSE_FROM));

    setItemPrice(TradeUtils.getDocumentItemPrice(row));

    setDate(TradeUtils.getDocumentDate(row));
    setCurrency(TradeUtils.getDocumentRelation(row, COL_TRADE_CURRENCY));
    setCurrencyName(TradeUtils.getDocumentString(row, AdministrationConstants.ALS_CURRENCY_NAME));

    setDiscountMode(TradeUtils.getDocumentDiscountMode(row));
    setDocumentDiscount(TradeUtils.getDocumentDiscount(row));
    setVatMode(TradeUtils.getDocumentVatMode(row));

    if (!priceCalculationOptions.isEmpty()) {
      priceCalculationOptions.clear();
    }

    Map<String, String> options = TradeUtils.getDocumentPriceCalculationOptions(row,
        getDate(), getCurrency(), getOperationType(),
        TradeUtils.getCompanyForPriceCalculation(row, getOperationType()),
        TradeUtils.getWarehouseForPriceCalculation(row, getOperationType()));

    if (!BeeUtils.isEmpty(options)) {
      priceCalculationOptions.putAll(options);
    }
  }

  public void setDefaultVatPercent(Double defaultVatPercent) {
    this.defaultVatPercent = defaultVatPercent;
  }

  private void createNew(boolean isService) {
    DataInfo dataInfo = Data.getDataInfo(VIEW_ITEMS);

    BeeRow row = RowFactory.createEmptyRow(dataInfo);
    if (isService) {
      Data.setValue(VIEW_ITEMS, row, COL_ITEM_IS_SERVICE, isService);
    }

    RowFactory.createRow(dataInfo, row, Modality.ENABLED, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        if (DataUtils.hasId(result)) {
          doQuery(Filter.compareId(result.getId()), Collections.emptySet());
        }
      }
    });
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
        if (UiHelper.isSave(event.getNativeEvent())) {
          save();

        } else {
          String input = searchBox.getValue();
          if (!BeeUtils.isEmpty(input)) {
            doSearch(input, searchBySelectors);
          }
        }

      } else if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
        close();

      } else if (event.getNativeKeyCode() == KeyCodes.KEY_F2) {
        showSelectedItems();
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
      notification.warning(
          Localized.dictionary().fieldRequired(Localized.dictionary().trdDocumentPhase()));
      return;
    }

    if (getOperationType() == null) {
      notification.warning(
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
          focusSearchBox();
          notification.severe(ArrayUtils.toArray(errorMessages));
          return;
        }
      }
    }

    if (searchBy.isEmpty() && !BeeUtils.isEmpty(query)) {
      searchBy.addAll(getDefaultSearchBy(query));
    }

    doQuery(Filter.and(buildParentFilter(), buildSearchFilter(query, searchBy)), searchBy);
  }

  private void doQuery(final Filter where, final Collection<TradeItemSearch> searchBy) {
    addStyleName(STYLE_SEARCH_RUNNING);

    Queries.getRowCount(VIEW_ITEM_SELECTION, where, new Queries.IntCallback() {
      @Override
      public void onFailure(String... reason) {
        removeStyleName(STYLE_SEARCH_RUNNING);
      }

      @Override
      public void onSuccess(Integer count) {
        if (BeeUtils.isPositive(count)) {
          int offset;
          int limit;

          int ps = estimatePageSize();

          if (ps < count) {
            offset = 0;
            limit = ps;
          } else {
            offset = BeeConst.UNDEF;
            limit = BeeConst.UNDEF;
          }

          Queries.getRowSet(VIEW_ITEM_SELECTION, null, where, null, offset, limit,
              new Queries.RowSetCallback() {
                @Override
                public void onFailure(String... reason) {
                  removeStyleName(STYLE_SEARCH_RUNNING);
                }

                @Override
                public void onSuccess(BeeRowSet result) {
                  if (DataUtils.isEmpty(result)) {
                    notification.warning(Localized.dictionary().nothingFound());

                  } else {
                    setFilter(where);
                    setFilterBy(searchBy);

                    renderItems(result);

                    setRowCount(count, false);
                    setPageStart(0, false, false, NavigationOrigin.SYSTEM);
                    setPageSize(ps, false);

                    pager.setDisplay(TradeItemPicker.this);
                    fireScopeChange(NavigationOrigin.SYSTEM);
                  }

                  removeStyleName(STYLE_SEARCH_RUNNING);
                }
              });

        } else {
          removeStyleName(STYLE_SEARCH_RUNNING);
          focusSearchBox();
          notification.warning(Localized.dictionary().nothingFound());
        }
      }
    });
  }

  private void refresh() {
    addStyleName(STYLE_SEARCH_RUNNING);

    Queries.getRowSet(VIEW_ITEM_SELECTION, null, getFilter(), null, getPageStart(), getPageSize(),
        new Queries.RowSetCallback() {
          @Override
          public void onFailure(String... reason) {
            removeStyleName(STYLE_SEARCH_RUNNING);
          }

          @Override
          public void onSuccess(BeeRowSet result) {
            if (DataUtils.isEmpty(result)) {
              notification.warning(Localized.dictionary().nothingFound());
            } else {
              renderItems(result);
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
      CompoundFilter sf = Filter.or();

      for (TradeItemSearch by : searchBy) {
        sf.add(by.getItemFilter(query));
      }
      return sf;
    }
  }

  private Filter buildParentFilter() {
    if (needsStock()) {
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

  private DateTime getDate() {
    return date;
  }

  private void setDate(DateTime date) {
    this.date = date;
  }

  private Long getCurrency() {
    return currency;
  }

  private void setCurrency(Long currency) {
    this.currency = currency;
  }

  private String getCurrencyName() {
    return currencyName;
  }

  private void setCurrencyName(String currencyName) {
    this.currencyName = currencyName;
  }

  private TradeDiscountMode getDiscountMode() {
    return tds.getDiscountMode();
  }

  private void setDiscountMode(TradeDiscountMode discountMode) {
    tds.updateDiscountMode(discountMode);
  }

  private void setDocumentDiscount(Double documentDiscount) {
    tds.updateDocumentDiscount(documentDiscount);
  }

  private TradeVatMode getVatMode() {
    return tds.getVatMode();
  }

  private void setVatMode(TradeVatMode vatMode) {
    tds.updateVatMode(vatMode);
  }

  private Double getDefaultVatPercent() {
    return defaultVatPercent;
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

  private static String getColumnStylePrefix(String column, ItemPrice ip) {
    String styleName;

    if (ip != null && column.equals(ip.getPriceColumn())) {
      styleName = "item-price";

    } else {
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

  private String renderItemPrice(BeeRowSet items, BeeRow item, ItemPrice ip,
      boolean isService, String mainWarehouseCode, boolean showStockCost) {

    if (!isService && showStockCost) {
      Double value = item.getPropertyDouble(keyCostWarehouse(mainWarehouseCode));

      if (BeeUtils.isDouble(value)) {
        Double cost = Money.maybeExchange(ClientDefaults.getCurrency(), getCurrency(),
            value, getDate());

        if (BeeUtils.isDouble(cost)) {
          return costFormat.format(BeeUtils.round(cost, costScale));
        }
      }
    }

    Double value = DataUtils.getDouble(items, item, ip.getPriceColumn());

    if (BeeUtils.isDouble(value)) {
      Double price = Money.maybeExchange(DataUtils.getLong(items, item, ip.getCurrencyColumn()),
          getCurrency(), value, getDate());

      if (BeeUtils.isDouble(price)) {
        return itemPriceFormat.format(BeeUtils.round(price, itemPriceScale));
      }
    }

    return null;
  }

  private static String renderPrice(Double price) {
    return BeeUtils.nonZero(price) ? priceFormat.format(price) : null;
  }

  private static String renderDiscountInfo(Double discount, Boolean isPercent) {
    if (BeeUtils.nonZero(discount)) {
      if (BeeUtils.isTrue(isPercent)) {
        return BeeUtils.joinWords(discount, BeeConst.STRING_PERCENT);
      } else {
        return BeeUtils.toString(discount);
      }

    } else {
      return null;
    }
  }

  private static String renderVatInfo(Double vat, Boolean isPercent) {
    if (BeeUtils.nonZero(vat)) {
      if (BeeUtils.isTrue(isPercent)) {
        return BeeUtils.joinWords(vat, BeeConst.STRING_PERCENT);
      } else {
        return BeeUtils.toString(vat);
      }

    } else {
      return null;
    }
  }

  private static String renderAmount(Double amount) {
    return BeeUtils.nonZero(amount) ? amountFormat.format(amount) : null;
  }

  private static String renderTotal(Double total) {
    return BeeUtils.nonZero(total) ? totalFormat.format(total) : null;
  }

  private String renderTotalQuantity() {
    return BeeUtils.toString(tds.sumQuantity());
  }

  private InputNumber renderQty(BeeColumn column, Double qty, Double available,
      boolean needsStock) {

    InputNumber input = new InputNumber();

    input.setMinValue(BeeConst.STRING_ZERO);

    if (column != null) {
      input.setMaxValue(DataUtils.getMaxValue(column));
      input.setScale(column.getScale());
      input.setMaxLength(UiHelper.getMaxLength(column));
    }

    if (BeeUtils.isPositive(available) && needsStock) {
      input.setMaxValue(BeeUtils.toString(available));
    }

    input.addStyleName(STYLE_QTY_INPUT);

    if (BeeUtils.isPositive(qty)) {
      input.setValue(qty);
    }

    input.addChangeHandler(ensureQuantityChangeHandler());
    input.addKeyDownHandler(ensureQuantityKeyDownHandler());

    return input;
  }

  private static Widget renderStock(Double stock, Double reserved) {
    Flow panel = new Flow(STYLE_STOCK_CONTAINER);

    if (BeeUtils.isPositive(stock)) {
      DoubleLabel stockWidget = new DoubleLabel(true);
      stockWidget.setTitle(Localized.dictionary().trdQuantityStock());
      stockWidget.addStyleName(STYLE_STOCK);

      stockWidget.setValue(stock);
      panel.add(stockWidget);
    }

    if (BeeUtils.isPositive(reserved)) {
      DoubleLabel reserveWidget = new DoubleLabel(true);
      reserveWidget.setTitle(Localized.dictionary().trdQuantityReserved());
      reserveWidget.addStyleName(STYLE_RESERVED);

      reserveWidget.setValue(reserved);
      panel.add(reserveWidget);

      double available = BeeUtils.unbox(stock) - BeeUtils.unbox(reserved);

      DoubleLabel availableWidget = new DoubleLabel(true);
      availableWidget.setTitle(Localized.dictionary().trdQuantityAvailable());
      availableWidget.addStyleName(STYLE_AVAILABLE);

      availableWidget.setValue(Math.max(available, BeeConst.DOUBLE_ZERO));
      panel.add(availableWidget);
    }

    return panel;
  }

  private void renderItems(BeeRowSet items) {
    setData(items);

    ItemPrice ip = getItemPriceForRender(items);
    List<String> itemColumns = getItemColumnsForRender(items, getFilterBy(), ip);

    Map<Long, String> warehouses = extractWarehouses(items);
    String mainWarehouseCode = warehouses.get(getWarehouse());

    Map<String, Long> warehouseCodes = new TreeMap<>();
    warehouses.forEach((id, code) -> warehouseCodes.put(code, id));

    boolean needsStock = needsStock();
    boolean showStockCost = TradeUtils.documentPriceIsParentCost(getOperationType(), ip)
        && !BeeUtils.isEmpty(mainWarehouseCode);

    int qtyCol;
    int priceCol;
    int discountCol = BeeConst.UNDEF;
    int vatCol = BeeConst.UNDEF;
    int totalCol;

    if (itemPanel.isEmpty()) {
      removeStyleName(STYLE_EMPTY);
    } else {
      itemPanel.clear();
    }

    final HtmlTable table = new HtmlTable(STYLE_ITEM_TABLE);

    int r = 0;
    int c = 0;

    table.setText(r, c++, Localized.dictionary().captionId(), STYLE_ID + STYLE_HEADER_CELL_SUFFIX);

    for (String itemColumn : itemColumns) {
      table.setText(r, c++, getColumnLabel(items, itemColumn),
          getColumnStylePrefix(itemColumn, ip) + STYLE_HEADER_CELL_SUFFIX);
    }

    for (Map.Entry<String, Long> entry : warehouseCodes.entrySet()) {
      table.setText(r, c, entry.getKey(), STYLE_STOCK + STYLE_HEADER_CELL_SUFFIX);

      if (Objects.equals(entry.getValue(), getWarehouse())) {
        table.getCellFormatter().addStyleName(r, c,
            STYLE_MAIN_WAREHOUSE + STYLE_HEADER_CELL_SUFFIX);
      }
      c++;
    }

    qtyCol = c;

    table.setText(r, c++, Localized.dictionary().quantity(), STYLE_QTY + STYLE_HEADER_CELL_SUFFIX);

    priceCol = c;
    table.setText(r, c++, Localized.dictionary().price(), STYLE_PRICE + STYLE_HEADER_CELL_SUFFIX);

    if (getDiscountMode() != null) {
      discountCol = c;
      table.setText(r, c++, Localized.dictionary().discount(),
          STYLE_DISCOUNT + STYLE_HEADER_CELL_SUFFIX);
    }

    if (getVatMode() != null) {
      vatCol = c;
      table.setText(r, c++, Localized.dictionary().vat(), STYLE_VAT + STYLE_HEADER_CELL_SUFFIX);
    }

    totalCol = c;
    table.setText(r, c, BeeUtils.joinWords(Localized.dictionary().total(), getCurrencyName()),
        STYLE_TOTAL + STYLE_HEADER_CELL_SUFFIX);

    table.getRowFormatter().addStyleName(r, STYLE_HEADER_ROW);

    BeeColumn qtyColumn = Data.getColumn(VIEW_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY);

    int serviceTagIndex = items.getColumnIndex(COL_ITEM_IS_SERVICE);
    String text;

    r++;
    for (BeeRow item : items) {
      long id = item.getId();
      boolean isService = item.isTrue(serviceTagIndex);

      c = 0;

      table.setValue(r, c++, id, STYLE_ID + STYLE_CELL_SUFFIX);

      for (String itemColumn : itemColumns) {
        if (ip != null && ip.getPriceColumn().equals(itemColumn)) {
          text = renderItemPrice(items, item, ip, isService, mainWarehouseCode, showStockCost);
          if (!BeeUtils.isEmpty(text)) {
            DomUtils.setDataProperty(table.getRow(r), KEY_PRICE, text);
          }

        } else {
          text = render(items, item, itemColumn);
        }

        table.setText(r, c++, text, getColumnStylePrefix(itemColumn, ip) + STYLE_CELL_SUFFIX);
      }

      double available = BeeConst.DOUBLE_ZERO;

      for (Map.Entry<String, Long> entry : warehouseCodes.entrySet()) {
        Double stock = item.getPropertyDouble(keyStockWarehouse(entry.getKey()));

        if (BeeUtils.isPositive(stock)) {
          Double reserved = item.getPropertyDouble(keyReservedWarehouse(entry.getKey()));
          table.setWidget(r, c, renderStock(stock, reserved), STYLE_STOCK + STYLE_CELL_SUFFIX);

          if (Objects.equals(entry.getValue(), getWarehouse())) {
            table.getCellFormatter().addStyleName(r, c, STYLE_MAIN_WAREHOUSE + STYLE_CELL_SUFFIX);
            available = stock - BeeUtils.unbox(reserved);

            if (BeeUtils.isPositive(available)) {
              DomUtils.setDataProperty(table.getCellFormatter().getElement(r, c),
                  KEY_AVAILABLE, available);
            }
          }
        }
        c++;
      }

      boolean selected = tds.containsItem(id);

      Double qty;
      if (selected) {
        qty = tds.getQuantity(id);
      } else {
        qty = null;
      }

      if (selected || isService || BeeUtils.isPositive(available) || !needsStock) {
        table.setWidget(r, qtyCol, renderQty(qtyColumn, qty, available, needsStock),
            STYLE_QTY + STYLE_CELL_SUFFIX);
      }

      text = selected ? renderPrice(tds.getPrice(id)) : BeeConst.STRING_EMPTY;
      table.setText(r, priceCol, text, STYLE_PRICE + STYLE_CELL_SUFFIX);

      if (!BeeConst.isUndef(discountCol)) {
        Pair<Double, Boolean> discountInfo = selected ? tds.getDiscountInfo(id) : null;
        text = (discountInfo == null)
            ? BeeConst.STRING_EMPTY : renderDiscountInfo(discountInfo.getA(), discountInfo.getB());

        table.setText(r, discountCol, text, STYLE_DISCOUNT + STYLE_CELL_SUFFIX);
      }

      if (!BeeConst.isUndef(vatCol)) {
        Pair<Double, Boolean> vatInfo = selected ? tds.getVatInfo(id) : null;
        text = (vatInfo == null)
            ? BeeConst.STRING_EMPTY : renderVatInfo(vatInfo.getA(), vatInfo.getB());

        table.setText(r, vatCol, text, STYLE_VAT + STYLE_CELL_SUFFIX);
      }

      text = selected ? renderTotal(tds.getItemTotal(id)) : BeeConst.STRING_EMPTY;
      table.setText(r, totalCol, text, STYLE_TOTAL + STYLE_CELL_SUFFIX);

      DomUtils.setDataIndex(table.getRow(r), id);

      table.getRowFormatter().addStyleName(r, STYLE_ITEM_ROW);
      if (isService) {
        table.getRowFormatter().addStyleName(r, STYLE_SERVICE_ROW);
      }

      if (selected) {
        table.getRowFormatter().addStyleName(r, STYLE_SELECTED_ROW);
      }

      r++;
    }

    table.setText(r, qtyCol, renderTotalQuantity(), STYLE_QTY + STYLE_FOOTER_CELL_SUFFIX);

    text = tds.hasItems() ? renderAmount(tds.getAmount()) : BeeConst.STRING_EMPTY;
    table.setText(r, priceCol, text, STYLE_PRICE + STYLE_FOOTER_CELL_SUFFIX);

    if (!BeeConst.isUndef(discountCol)) {
      text = tds.hasItems() ? renderAmount(tds.getDiscount()) : BeeConst.STRING_EMPTY;
      table.setText(r, discountCol, text, STYLE_DISCOUNT + STYLE_FOOTER_CELL_SUFFIX);
    }

    if (!BeeConst.isUndef(vatCol)) {
      text = tds.hasItems() ? renderAmount(tds.getVat()) : BeeConst.STRING_EMPTY;
      table.setText(r, vatCol, text, STYLE_VAT + STYLE_FOOTER_CELL_SUFFIX);
    }

    text = tds.hasItems() ? renderTotal(tds.getTotal()) : BeeConst.STRING_EMPTY;
    table.setText(r, totalCol, text, STYLE_TOTAL + STYLE_FOOTER_CELL_SUFFIX);

    table.getRowFormatter().addStyleName(r, STYLE_FOOTER_ROW);

    itemPanel.add(table);

    Scheduler.get().scheduleDeferred(() -> {
      Popup popup = UiHelper.getParentPopup(TradeItemPicker.this);
      if (popup != null) {
        popup.onResize();
      }

      UiHelper.focus(table);
    });
  }

  private KeyDownHandler ensureQuantityKeyDownHandler() {
    if (quantityKeyDownHandler == null) {
      quantityKeyDownHandler = event -> {
        if (event.getSource() instanceof InputNumber) {
          InputNumber input;

          if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            input = (InputNumber) event.getSource();

            if (validate(input)) {
              if (UiHelper.isSave(event.getNativeEvent())) {
                setSavePending();
                onQuantityChange(input);

              } else if (isLast(input)) {
                if (!focusSearchBox()) {
                  onQuantityChange(input);
                }

              } else if (!UiHelper.moveFocus(input.getParent(), true)) {
                onQuantityChange(input);
              }
            }

          } else if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
            input = (InputNumber) event.getSource();

            if (input.isEmpty()) {
              focusSearchBox();

            } else {
              long id = getRowId(input.getElement());

              Double oldValue;
              if (tds.containsItem(id)) {
                oldValue = tds.getQuantity(id);
              } else {
                oldValue = null;
              }

              if (Objects.equals(oldValue, input.getNumber())) {
                focusSearchBox();
              } else {
                input.setValue(oldValue);
              }
            }

          } else if (getRowCount() > 1) {

            switch (event.getNativeKeyCode()) {
              case KeyCodes.KEY_UP:
              case KeyCodes.KEY_DOWN:
                input = (InputNumber) event.getSource();

                if (validate(input)) {
                  UiHelper.moveFocus(input.getParent(),
                      event.getNativeKeyCode() == KeyCodes.KEY_DOWN);
                }
                break;

              case KeyCodes.KEY_PAGEUP:
              case KeyCodes.KEY_PAGEDOWN:
                input = (InputNumber) event.getSource();

                if (validate(input)) {
                  boolean forward = event.getNativeKeyCode() == KeyCodes.KEY_PAGEDOWN;
                  boolean hasModifiers = EventUtils.hasModifierKey(event);

                  onQuantityChange(input);

                  boolean ok = false;

                  if (getPageSize() > 0 && getPageSize() < getRowCount()) {
                    if (hasModifiers) {
                      if (forward) {
                        ok = pager.goLast();
                      } else {
                        ok = pager.goFirst();
                      }

                    } else {
                      if (forward) {
                        ok = pager.goNext();
                      } else {
                        ok = pager.goPrevious();
                      }
                    }
                  }

                  if (!ok) {
                    List<Element> inputElements = Selectors.getElementsByClassName(
                        itemPanel.getElement(), STYLE_QTY_INPUT);

                    if (BeeUtils.size(inputElements) > 1) {
                      int index = forward ? inputElements.size() - 1 : 0;
                      inputElements.get(index).focus();
                    }
                  }
                }
                break;
            }
          }
        }
      };
    }
    return quantityKeyDownHandler;
  }

  private boolean focusSearchBox() {
    Widget widget = UiHelper.getChildByStyleName(this, STYLE_SEARCH_BOX);

    if (widget instanceof InputText) {
      InputText input = (InputText) widget;

      input.setFocus(true);
      input.selectAll();

      return true;

    } else {
      return false;
    }
  }

  private void close() {
    FaLabel close = findAction(STYLE_CLOSE);
    if (close != null) {
      close.click();
    }
  }

  private void save() {
    FaLabel save = findAction(STYLE_SAVE);
    if (save != null) {
      save.click();
    }
  }

  private FaLabel findAction(String styleName) {
    Popup popup = UiHelper.getParentPopup(this);

    if (popup != null) {
      Widget widget = UiHelper.getChildByStyleName(popup, styleName);
      if (widget instanceof FaLabel) {
        return (FaLabel) widget;
      }
    }

    return null;
  }

  private boolean isLast(InputNumber input) {
    List<Element> inputElements = Selectors.getElementsByClassName(itemPanel.getElement(),
        STYLE_QTY_INPUT);

    return !BeeUtils.isEmpty(inputElements)
        && DomUtils.sameId(BeeUtils.getLast(inputElements), input.getElement());
  }

  private ChangeHandler ensureQuantityChangeHandler() {
    if (quantityChangeHandler == null) {
      quantityChangeHandler = event -> {
        if (event.getSource() instanceof InputNumber) {
          InputNumber input = (InputNumber) event.getSource();

          if (validate(input)) {
            onQuantityChange(input);
          }
        }
      };
    }
    return quantityChangeHandler;
  }

  private boolean needsStock() {
    return getDocumentPhase() != null && getDocumentPhase().modifyStock()
        && getOperationType() != null && getOperationType().consumesStock();
  }

  private boolean validate(InputNumber input) {
    List<String> errorMessages = input.validate(false);
    if (BeeUtils.isEmpty(errorMessages)) {
      return true;

    } else {
      long id = getRowId(input.getElement());

      if (tds.containsItem(id)) {
        input.setValue(tds.getQuantity(id));
      } else {
        input.clearValue();
      }

      notification.warning(ArrayUtils.toArray(errorMessages));
      input.setFocus(true);

      return false;
    }
  }

  private void onQuantityChange(InputNumber input) {
    final boolean pending = isSavePending();
    setState(State.UPDATING);

    onQuantityChange(input, input.getNumber(), b -> {
      boolean commit = pending || isSavePending();
      setState(State.CHANGED);

      if (commit) {
        save();
      }
    });
  }

  private void onQuantityChange(InputNumber input, Double quantity,
      final Consumer<Boolean> callback) {

    TableRowElement rowElement = DomUtils.getParentRow(input.getElement(), true);
    long id = DomUtils.getDataIndexLong(rowElement);

    if (DataUtils.isId(id)) {
      if (BeeUtils.isPositive(quantity)) {
        if (tds.containsItem(id)) {
          if (Objects.equals(quantity, tds.getQuantity(id))) {
            callback.accept(true);
            return;
          }

          tds.updateQuantity(id, quantity);
          maybeUpdatePriceAndDiscount(id, quantity, getLong(id, COL_UNIT), rowElement, callback);

        } else {
          rowElement.addClassName(STYLE_SELECTED_ROW);

          BeeRow item = getRow(id);
          if (item != null) {
            Double price = DomUtils.getDataPropertyDouble(rowElement, KEY_PRICE);

            Double vat = null;
            if (getVatMode() != null && item.isTrue(getDataIndex(COL_ITEM_VAT))) {
              vat = BeeUtils.nvl(item.getDouble(getDataIndex(COL_ITEM_VAT_PERCENT)),
                  getDefaultVatPercent());
            }
            Boolean vatIsPercent = TradeUtils.vatIsPercent(vat);

            tds.add(id, quantity, price, null, null, vat, vatIsPercent);
            selectedItems.add(DataUtils.cloneRow(item));

            maybeUpdatePriceAndDiscount(id, quantity, DataUtils.getLong(data, item, COL_UNIT),
                rowElement, callback);

          } else {
            callback.accept(false);
          }
        }

      } else {
        rowElement.removeClassName(STYLE_SELECTED_ROW);
        removeItem(id);

        callback.accept(true);
      }

      refreshSums(id, rowElement, rowElement.getParentElement());

    } else {
      callback.accept(false);
    }
  }

  private void maybeUpdatePriceAndDiscount(long id, Double quantity, Long unit,
      Element rowElement, final Consumer<Boolean> callback) {

    if (!TradeUtils.documentPriceIsParentCost(getOperationType(), getItemPrice())
        && !priceCalculationOptions.isEmpty()) {

      Map<String, String> options = new HashMap<>(priceCalculationOptions);
      options.put(Service.VAR_QTY, BeeUtils.toStringOrNull(quantity));
      options.put(COL_DISCOUNT_UNIT, BeeUtils.toStringOrNull(unit));

      getPriceAndDiscount(id, options, rowElement.getParentElement(), callback);

    } else {
      callback.accept(true);
    }
  }

  private void getPriceAndDiscount(final long id, Map<String, String> options,
      final Element tableElement, final Consumer<Boolean> callback) {

    ClassifierKeeper.getPriceAndDiscount(id, options, (price, discount) -> {
      if (tds.containsItem(id)) {
        Double oldPrice = tds.getPrice(id);
        if (BeeUtils.isZero(oldPrice)) {
          oldPrice = null;
        }

        Pair<Double, Boolean> discountInfo =
            TradeUtils.normalizeDiscountOrVatInfo(tds.getDiscountInfo(id));
        Double oldDiscount = discountInfo.getA();
        boolean oldDiscountIsPercent = BeeUtils.isTrue(discountInfo.getB());

        Double newPrice = oldPrice;
        Double newDiscount = oldDiscount;
        boolean newDiscountIsPercent = oldDiscountIsPercent;

        if (BeeUtils.isDouble(price)) {
          if (BeeUtils.nonZero(price) && BeeUtils.nonZero(discount) && getDiscountMode() == null) {
            newPrice = TradeUtils.roundPrice(BeeUtils.minusPercent(price, discount));
            newDiscount = null;
            newDiscountIsPercent = false;
          } else {
            newPrice = price;
          }
        }

        if (BeeUtils.isDouble(discount) && getDiscountMode() != null) {
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

        if (priceChanged) {
          tds.updatePrice(id, newPrice);
        }
        if (discountChanged) {
          tds.updateDiscount(id, newDiscount);
        }
        if (discountIsPercentChanged) {
          tds.updateDiscountIsPercent(id, newDiscountIsPercent);
        }

        if (priceChanged || discountChanged || discountIsPercentChanged) {
          Element rowElement = Selectors.getElementByDataIndex(tableElement, id);
          refreshSums(id, rowElement, tableElement);
        }

        callback.accept(true);

      } else {
        callback.accept(false);
      }
    });
  }

  private void removeItem(long id) {
    tds.deleteItem(id);

    int index = BeeConst.UNDEF;
    for (int i = 0; i < selectedItems.size(); i++) {
      if (DataUtils.idEquals(selectedItems.get(i), id)) {
        index = i;
        break;
      }
    }

    if (!BeeConst.isUndef(index)) {
      selectedItems.remove(index);
    }
  }

  private void refreshSums(long id, Element rowElement, Element footerContainer) {
    boolean contains = tds.containsItem(id);
    String text;

    Element el = findElement(rowElement, STYLE_PRICE + STYLE_CELL_SUFFIX);
    if (el != null) {
      text = contains ? renderPrice(tds.getPrice(id)) : BeeConst.STRING_EMPTY;
      el.setInnerText(text);
    }

    if (getDiscountMode() != null) {
      el = findElement(rowElement, STYLE_DISCOUNT + STYLE_CELL_SUFFIX);
      if (el != null) {
        Pair<Double, Boolean> discountInfo = contains ? tds.getDiscountInfo(id) : null;

        if (discountInfo == null) {
          DomUtils.clearText(el);
        } else {
          el.setInnerText(renderDiscountInfo(discountInfo.getA(), discountInfo.getB()));
        }
      }
    }

    if (getVatMode() != null) {
      el = findElement(rowElement, STYLE_VAT + STYLE_CELL_SUFFIX);
      if (el != null) {
        Pair<Double, Boolean> vatInfo = contains ? tds.getVatInfo(id) : null;

        if (vatInfo == null) {
          DomUtils.clearText(el);
        } else {
          el.setInnerText(renderVatInfo(vatInfo.getA(), vatInfo.getB()));
        }
      }
    }

    el = findElement(rowElement, STYLE_TOTAL + STYLE_CELL_SUFFIX);
    if (el != null) {
      text = contains ? renderTotal(tds.getItemTotal(id)) : BeeConst.STRING_EMPTY;
      el.setInnerText(text);
    }

    el = findElement(footerContainer, STYLE_QTY + STYLE_FOOTER_CELL_SUFFIX);
    if (el != null) {
      el.setInnerText(renderTotalQuantity());
    }

    el = findElement(footerContainer, STYLE_PRICE + STYLE_FOOTER_CELL_SUFFIX);
    if (el != null) {
      text = tds.hasItems() ? renderAmount(tds.getAmount()) : BeeConst.STRING_EMPTY;
      el.setInnerText(text);
    }

    if (getDiscountMode() != null) {
      el = findElement(footerContainer, STYLE_DISCOUNT + STYLE_FOOTER_CELL_SUFFIX);
      if (el != null) {
        text = tds.hasItems() ? renderAmount(tds.getDiscount()) : BeeConst.STRING_EMPTY;
        el.setInnerText(text);
      }
    }

    if (getVatMode() != null) {
      el = findElement(footerContainer, STYLE_VAT + STYLE_FOOTER_CELL_SUFFIX);
      if (el != null) {
        text = tds.hasItems() ? renderAmount(tds.getVat()) : BeeConst.STRING_EMPTY;
        el.setInnerText(text);
      }
    }

    el = findElement(footerContainer, STYLE_TOTAL + STYLE_FOOTER_CELL_SUFFIX);
    if (el != null) {
      text = tds.hasItems() ? renderTotal(tds.getTotal()) : BeeConst.STRING_EMPTY;
      el.setInnerText(text);
    }
  }

  private static Element findElement(Element root, String className) {
    if (root == null) {
      return null;
    }

    Element el = Selectors.getElementByClassName(root, className);
    if (el == null) {
      logger.warning(root.getTagName(), root.getClassName(), "child", className, "not found");
    }

    return el;
  }

  private Map<Long, String> extractWarehouses(BeeRowSet items) {
    Map<Long, String> warehouses = new HashMap<>();

    if (getOperationType() != null && getOperationType().consumesStock()) {
      String serialized = (items == null) ? null : items.getTableProperty(PROP_WAREHOUSES);

      if (!BeeUtils.isEmpty(serialized)) {
        Map<String, String> map = Codec.deserializeHashMap(serialized);

        map.forEach((id, code) -> {
          if (DataUtils.isId(id) && !BeeUtils.isEmpty(code)) {
            warehouses.put(BeeUtils.toLong(id), code);
          }
        });
      }
    }

    return warehouses;
  }

  private void setData(BeeRowSet data) {
    this.data = data;
  }

  private BeeRow getRow(Long id) {
    if (data != null && id != null) {
      return data.getRowById(id);
    } else {
      return null;
    }
  }

  private Long getLong(Long id, String column) {
    return DataUtils.getLongQuietly(getRow(id), getDataIndex(column));
  }

  private int getDataIndex(String column) {
    return (data == null) ? BeeConst.UNDEF : data.getColumnIndex(column);
  }

  private static long getRowId(Element child) {
    return DomUtils.getDataIndexLong(DomUtils.getParentRow(child, true));
  }

  private void onCellClick(TableCellElement cell) {
    if (cell.hasClassName(STYLE_ID + STYLE_CELL_SUFFIX)) {
      long id = getRowId(cell);
      if (DataUtils.isId(id)) {
        RowEditor.open(VIEW_ITEMS, id, Opener.MODAL);
      }

    } else if (cell.hasClassName(STYLE_MAIN_WAREHOUSE + STYLE_CELL_SUFFIX)) {
      maybeSetAvailableQuantity(cell);

    } else if (cell.hasClassName(STYLE_MAIN_WAREHOUSE + STYLE_HEADER_CELL_SUFFIX)) {
      List<Element> stockCells = Selectors.getElementsWithDataProperty(
          DomUtils.getParentTable(cell, false), KEY_AVAILABLE);

      if (!BeeUtils.isEmpty(stockCells)) {
        stockCells.forEach(this::maybeSetAvailableQuantity);
      }

    } else if (cell.hasClassName(STYLE_QTY + STYLE_HEADER_CELL_SUFFIX)
        || cell.hasClassName(STYLE_QTY + STYLE_FOOTER_CELL_SUFFIX)
        || cell.hasClassName(STYLE_TOTAL + STYLE_FOOTER_CELL_SUFFIX)) {

      showSelectedItems();
    }
  }

  private void showSelectedItems() {
    if (hasSelection()) {
      doQuery(Filter.idIn(tds.getItemIds()), Collections.emptySet());
    }
  }

  private void maybeSetAvailableQuantity(Element cell) {
    Double stock = DomUtils.getDataPropertyDouble(cell, KEY_AVAILABLE);
    long id = getRowId(cell);

    TableRowElement rowElement = DomUtils.getParentRow(cell, true);
    Element inputElement = Selectors.getElementByClassName(rowElement, STYLE_QTY_INPUT);
    HtmlTable table = UiHelper.getChild(itemPanel, HtmlTable.class);
    Widget widget = table.getWidgetByElement(inputElement);

    if (BeeUtils.isPositive(stock) && DataUtils.isId(id) && widget instanceof InputNumber) {
      InputNumber input = (InputNumber) widget;

      if (!Objects.equals(input.getNumber(), stock)) {
        input.setValue(stock);
        onQuantityChange(input);
      }
    }
  }

  @Override
  public HandlerRegistration addScopeChangeHandler(ScopeChangeEvent.Handler handler) {
    return addHandler(handler, ScopeChangeEvent.getType());
  }

  @Override
  public int getPageSize() {
    return pageSize;
  }

  @Override
  public int getPageStart() {
    return pageStart;
  }

  @Override
  public int getRowCount() {
    return rowCount;
  }

  @Override
  public void setPageSize(int size, boolean fireScopeChange) {
    if (size != getPageSize()) {
      this.pageSize = size;

      if (fireScopeChange) {
        fireScopeChange(NavigationOrigin.SYSTEM);
      }
    }
  }

  @Override
  public void setPageStart(int start, boolean fireScopeChange, boolean fireDataRequest,
      NavigationOrigin origin) {

    if (start >= 0 && start != getPageStart()) {
      this.pageStart = start;

      if (fireScopeChange) {
        fireScopeChange(origin);
      }

      if (fireDataRequest) {
        refresh();
      }
    }
  }

  @Override
  public void setRowCount(int count, boolean fireScopeChange) {
    if (count >= 0 && count != getRowCount()) {
      this.rowCount = count;

      if (fireScopeChange) {
        fireScopeChange(NavigationOrigin.SYSTEM);
      }
    }
  }

  private static int estimatePageSize() {
    return Math.max(BeeKeeper.getScreen().getHeight() / 60, 1);
  }

  private void fireScopeChange(NavigationOrigin origin) {
    fireEvent(new ScopeChangeEvent(getPageStart(), getPageSize(), getRowCount(), origin));
  }

  private Filter getFilter() {
    return filter;
  }

  private void setFilter(Filter filter) {
    this.filter = filter;
  }

  private List<TradeItemSearch> getFilterBy() {
    return filterBy;
  }

  private void setFilterBy(Collection<TradeItemSearch> by) {
    filterBy.clear();

    if (!BeeUtils.isEmpty(by)) {
      filterBy.addAll(by);
    }
  }

  private State getState() {
    return state;
  }

  private void setState(State state) {
    this.state = state;
  }

  private boolean isSavePending() {
    return getState() == State.PENDING;
  }

  private void setSavePending() {
    setState(State.PENDING);
  }
}
