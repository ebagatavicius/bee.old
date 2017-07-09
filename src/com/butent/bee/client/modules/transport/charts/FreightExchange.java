package com.butent.bee.client.modules.transport.charts;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.Rectangle;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.logical.MoveEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.transport.TransportHandler;
import com.butent.bee.client.modules.transport.charts.Filterable.FilterType;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class FreightExchange extends ChartBase {

  private static final BeeLogger logger = LogUtils.getLogger(FreightExchange.class);

  static final String SUPPLIER_KEY = "freight_exchange";
  private static final String DATA_SERVICE = SVC_GET_FX_DATA;

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "tr-fx-";

  private static final String STYLE_CUSTOMER_PREFIX = STYLE_PREFIX + "Customer-";
  private static final String STYLE_CUSTOMER_ROW_SEPARATOR = STYLE_CUSTOMER_PREFIX + "row-sep";
  private static final String STYLE_CUSTOMER_PANEL = STYLE_CUSTOMER_PREFIX + "panel";
  private static final String STYLE_CUSTOMER_LABEL = STYLE_CUSTOMER_PREFIX + "label";

  private static final String STYLE_ORDER_PREFIX = STYLE_PREFIX + "Order-";
  private static final String STYLE_ORDER_ROW_SEPARATOR = STYLE_ORDER_PREFIX + "row-sep";
  private static final String STYLE_ORDER_PANEL = STYLE_ORDER_PREFIX + "panel";
  private static final String STYLE_ORDER_NUMBER = STYLE_ORDER_PREFIX + "number";
  private static final String STYLE_ORDER_MANAGER = STYLE_ORDER_PREFIX + "manager";

  private static final String STYLE_ITEM_PREFIX = STYLE_PREFIX + "Item-";
  private static final String STYLE_ITEM_PANEL = STYLE_ITEM_PREFIX + "panel";

  private static final String STYLE_ITEM_DRAG = STYLE_ITEM_PREFIX + "drag";

  private static final String STYLE_DRAG_OVER = STYLE_PREFIX + "dragOver";

  private static final Set<String> acceptsDropTypes = Collections.singleton(DATA_TYPE_FREIGHT);

  private static final Set<ChartDataType> AVAILABLE_FILTERS = Sets.newHashSet(
      ChartDataType.CUSTOMER, ChartDataType.MANAGER, ChartDataType.ORDER,
      ChartDataType.ORDER_STATUS, ChartDataType.CARGO, ChartDataType.CARGO_TYPE,
      ChartDataType.LOADING, ChartDataType.UNLOADING, ChartDataType.PLACE);

  static void open(final ViewCallback callback) {
    BeeKeeper.getRpc().makePostRequest(TransportHandler.createArgs(SVC_GET_SETTINGS),
        settingsResponse -> {
          if (!settingsResponse.hasErrors()) {
            FreightExchange fx = new FreightExchange(settingsResponse);

            fx.requestData(response -> fx.onCreate(response, callback));
          }
        });
  }

  private final List<OrderCargo> items = new ArrayList<>();

  private int customerWidth = BeeConst.UNDEF;
  private int orderWidth = BeeConst.UNDEF;

  private final Set<String> customerPanels = new HashSet<>();
  private final Set<String> orderPanels = new HashSet<>();

  private final Map<Integer, Long> customersByRow = new HashMap<>();

  private FreightExchange(ResponseObject settingsResponse) {
    super(settingsResponse);
    addStyleName(STYLE_PREFIX + "View");

    addRelevantDataViews(VIEW_ORDERS);

    DndHelper.makeTarget(this, acceptsDropTypes, STYLE_DRAG_OVER, DndHelper.ALWAYS_TARGET,
        (t, u) -> {
          removeStyleName(STYLE_DRAG_OVER);

          if (DndHelper.isDataType(DATA_TYPE_FREIGHT) && u instanceof Freight) {
            ((Freight) u).maybeRemoveFromTrip(result -> {
              if (BeeUtils.isPositive(result)) {
                DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_CARGO_TRIPS);
              }
            });
          }
        });
  }

  @Override
  public String getCaption() {
    return Localized.dictionary().freightExchange();
  }

  @Override
  public String getIdPrefix() {
    return "tr-fx";
  }

  @Override
  public String getSupplierKey() {
    return SUPPLIER_KEY;
  }

  @Override
  public void handleAction(Action action) {
    if (Action.ADD.equals(action)) {
      Global.choiceWithCancel(Localized.dictionary().newTransportationOrder(), null,
          Lists.newArrayList(Localized.dictionary().inputFull(),
              Localized.dictionary().inputSimple()), value -> {
            switch (value) {
              case 0:
                RowFactory.createRow(VIEW_ORDERS, Modality.DISABLED);
                break;

              case 1:
                DataInfo dataInfo = Data.getDataInfo(VIEW_ORDER_CARGO);
                BeeRow row = RowFactory.createEmptyRow(dataInfo, true);
                RowFactory.createRow(FORM_NEW_SIMPLE_ORDER,
                    Localized.dictionary().newTransportationOrder(), dataInfo, row,
                    Modality.DISABLED, null);
                break;
            }
          });

    } else {
      super.handleAction(action);
    }
  }

  @Override
  protected void addFilterSettingParams(ParameterList params) {
  }

  @Override
  protected boolean filter(FilterType filterType) {
    List<ChartData> selectedData = FilterHelper.getSelectedData(getFilterData());
    if (selectedData.isEmpty()) {
      resetFilter(filterType);
      return false;
    }

    CargoMatcher cargoMatcher = CargoMatcher.maybeCreate(selectedData);
    PlaceMatcher placeMatcher = PlaceMatcher.maybeCreate(selectedData);

    for (OrderCargo item : items) {
      boolean match = cargoMatcher == null || cargoMatcher.matches(item);

      if (match && placeMatcher != null) {
        boolean ok = placeMatcher.matches(item);
        if (!ok) {
          ok = placeMatcher.matchesAnyOf(getCargoHandling(item.getCargoId(),
              item.getCargoTripId()));
        }

        if (!ok) {
          match = false;
        }
      }

      item.setMatch(filterType, match);
    }

    return true;
  }

  @Override
  protected Set<ChartDataType> getAllFilterTypes() {
    return AVAILABLE_FILTERS;
  }

  @Override
  protected Collection<? extends HasDateRange> getChartItems() {
    if (isFiltered()) {
      return FilterHelper.getPersistentItems(items);
    } else {
      return items;
    }
  }

  @Override
  protected String getDataService() {
    return DATA_SERVICE;
  }

  @Override
  protected String getFilterDataTypesColumnName() {
    return COL_FX_FILTER_DATA_TYPES;
  }

  @Override
  protected String getFiltersColumnName() {
    return COL_FX_FILTERS;
  }

  @Override
  protected String getFilterService() {
    return null;
  }

  @Override
  protected String getFooterHeightColumnName() {
    return COL_FX_FOOTER_HEIGHT;
  }

  @Override
  protected String getHeaderHeightColumnName() {
    return COL_FX_HEADER_HEIGHT;
  }

  @Override
  protected String getRefreshLocalChangesColumnName() {
    return COL_FX_REFRESH_LOCAL_CHANGES;
  }

  @Override
  protected String getRefreshRemoteChangesColumnName() {
    return COL_FX_REFRESH_REMOTE_CHANGES;
  }

  @Override
  protected String getRowHeightColumnName() {
    return COL_FX_PIXELS_PER_ROW;
  }

  @Override
  protected Collection<String> getSettingsColumnsTriggeringRefresh() {
    return BeeConst.EMPTY_IMMUTABLE_STRING_SET;
  }

  @Override
  protected String getSettingsFormName() {
    return FORM_FX_SETTINGS;
  }

  @Override
  protected String getSettingsMaxDate() {
    return null;
  }

  @Override
  protected String getSettingsMinDate() {
    return null;
  }

  @Override
  protected String getShowAdditionalInfoColumnName() {
    return null;
  }

  @Override
  protected String getShowCountryFlagsColumnName() {
    return COL_FX_COUNTRY_FLAGS;
  }

  @Override
  protected String getShowOrderCustomerColumnName() {
    return null;
  }

  @Override
  protected String getShowOderNoColumnName() {
    return null;
  }

  @Override
  protected String getShowPlaceInfoColumnName() {
    return COL_FX_PLACE_INFO;
  }

  @Override
  protected String getShowPlaceCitiesColumnName() {
    return COL_FX_PLACE_CITIES;
  }

  @Override
  protected String getShowPlaceCodesColumnName() {
    return COL_FX_PLACE_CODES;
  }

  @Override
  protected String getStripOpacityColumnName() {
    return COL_FX_STRIP_OPACITY;
  }

  @Override
  protected String getThemeColumnName() {
    return COL_FX_THEME;
  }

  @Override
  protected boolean isFilterDependsOnData() {
    return true;
  }


  @Override
  protected void initData(Map<String, String> properties) {
    items.clear();

    long millis = System.currentTimeMillis();
    SimpleRowSet srs = SimpleRowSet.getIfPresent(properties, PROP_ORDER_CARGO);

    if (!DataUtils.isEmpty(srs)) {
      for (SimpleRow row : srs) {
        Pair<JustDate, JustDate> handlingSpan = getCargoHandlingSpan(getCargoHandling(),
            row.getLong(COL_CARGO_ID), null);
        items.add(OrderCargo.create(row, handlingSpan.getA(), handlingSpan.getB()));
      }

      logger.debug(PROP_ORDER_CARGO, items.size(), TimeUtils.elapsedMillis(millis));
    }
  }

  @Override
  protected void onDoubleClickChart(int row, JustDate date) {
    Long customerId = customersByRow.get(row);

    if (customerId != null && TimeUtils.isMeq(date, TimeUtils.today())) {
      DataInfo dataInfo = Data.getDataInfo(VIEW_ORDERS);
      BeeRow newRow = RowFactory.createEmptyRow(dataInfo, true);

      if (TimeUtils.isMore(date, TimeUtils.today())) {
        newRow.setValue(dataInfo.getColumnIndex(COL_ORDER_DATE), date.getDateTime());
      }

      newRow.setValue(dataInfo.getColumnIndex(COL_CUSTOMER), customerId);
      newRow.setValue(dataInfo.getColumnIndex(COL_CUSTOMER_NAME), findCustomerName(customerId));

      RowFactory.createRow(dataInfo, newRow, Modality.DISABLED);
    }
  }

  @Override
  protected boolean persistFilter() {
    return FilterHelper.persistFilter(items);
  }

  @Override
  protected void prepareChart(Size canvasSize) {
    setCustomerWidth(TimeBoardHelper.getPixels(getSettings(), COL_FX_PIXELS_PER_CUSTOMER, 100,
        TimeBoardHelper.DEFAULT_MOVER_WIDTH + 1, canvasSize.getWidth() / 3));
    setOrderWidth(TimeBoardHelper.getPixels(getSettings(), COL_FX_PIXELS_PER_ORDER, 160,
        TimeBoardHelper.DEFAULT_MOVER_WIDTH + 1, canvasSize.getWidth() / 3));

    setChartLeft(getCustomerWidth() + getOrderWidth());
    setChartWidth(canvasSize.getWidth() - getChartLeft() - getChartRight());

    setDayColumnWidth(TimeBoardHelper.getPixels(getSettings(), COL_FX_PIXELS_PER_DAY,
        getDefaultDayColumnWidth(getChartWidth()), 1, getChartWidth()));
  }

  @Override
  protected List<ChartData> prepareFilterData(ResponseObject response) {
    List<ChartData> data = new ArrayList<>();
    if (items.isEmpty()) {
      return data;
    }

    ChartData customerData = new ChartData(ChartDataType.CUSTOMER);
    ChartData managerData = new ChartData(ChartDataType.MANAGER);

    ChartData orderData = new ChartData(ChartDataType.ORDER);
    ChartData statusData = new ChartData(ChartDataType.ORDER_STATUS);

    ChartData cargoData = new ChartData(ChartDataType.CARGO);
    ChartData cargoTypeData = new ChartData(ChartDataType.CARGO_TYPE);

    ChartData loadData = new ChartData(ChartDataType.LOADING);
    ChartData unloadData = new ChartData(ChartDataType.UNLOADING);
    ChartData placeData = new ChartData(ChartDataType.PLACE);

    for (OrderCargo item : items) {
      customerData.add(item.getCustomerName(), item.getCustomerId());
      managerData.addUser(item.getManager());

      orderData.add(item.getOrderName(), item.getOrderId());
      statusData.add(item.getOrderStatus());

      cargoData.add(item.getCargoDescription(), item.getCargoId());
      if (DataUtils.isId(item.getCargoType())) {
        cargoTypeData.add(getCargoTypeName(item.getCargoType()), item.getCargoType());
      }

      String loading = Places.getLoadingPlaceInfo(item);
      if (!BeeUtils.isEmpty(loading)) {
        loadData.add(loading);
        placeData.add(loading);
      }

      String unloading = Places.getUnloadingPlaceInfo(item);
      if (!BeeUtils.isEmpty(unloading)) {
        unloadData.add(unloading);
        placeData.add(unloading);
      }

      for (CargoHandling ch : getCargoHandling(item.getCargoId(), item.getCargoTripId())) {
        loading = Places.getLoadingPlaceInfo(ch);
        if (!BeeUtils.isEmpty(loading)) {
          loadData.add(loading);
          placeData.add(loading);
        }

        unloading = Places.getUnloadingPlaceInfo(ch);
        if (!BeeUtils.isEmpty(unloading)) {
          unloadData.add(unloading);
          placeData.add(unloading);
        }
      }
    }

    data.add(customerData);
    data.add(managerData);

    data.add(orderData);
    data.add(statusData);

    data.add(cargoData);
    data.add(cargoTypeData);

    data.add(loadData);
    data.add(unloadData);
    data.add(placeData);

    return data;
  }

  @Override
  protected void renderContent(ComplexPanel panel) {
    customerPanels.clear();
    orderPanels.clear();

    customersByRow.clear();

    List<List<OrderCargo>> layoutRows = doLayout();

    initContent(panel, layoutRows.size());
    if (layoutRows.isEmpty()) {
      return;
    }

    int calendarWidth = getCalendarWidth();

    Long lastCustomer = null;
    Long lastOrder = null;

    IdentifiableWidget customerWidget = null;
    IdentifiableWidget orderWidget = null;

    int customerStartRow = 0;
    int orderStartRow = 0;

    Double itemOpacity = TimeBoardHelper.getOpacity(getSettings(), COL_FX_ITEM_OPACITY);

    Edges margins = new Edges();
    margins.setBottom(TimeBoardHelper.ROW_SEPARATOR_HEIGHT);

    for (int row = 0; row < layoutRows.size(); row++) {
      List<OrderCargo> rowItems = layoutRows.get(row);
      int top = row * getRowHeight();

      OrderCargo rowItem = rowItems.get(0);

      if (row == 0) {
        customerWidget = createCustomerWidget(rowItem);
        customerStartRow = row;

        orderWidget = createOrderWidget(rowItem);
        orderStartRow = row;

        lastCustomer = rowItem.getCustomerId();
        lastOrder = rowItem.getOrderId();

      } else {
        boolean customerChanged = !Objects.equals(lastCustomer, rowItem.getCustomerId());
        boolean orderChanged = customerChanged || !Objects.equals(lastOrder, rowItem.getOrderId());

        if (customerChanged) {
          addCustomerWidget(panel, customerWidget, lastCustomer, customerStartRow, row - 1);

          customerWidget = createCustomerWidget(rowItem);
          customerStartRow = row;

          lastCustomer = rowItem.getCustomerId();
        }

        if (orderChanged) {
          addOrderWidget(panel, orderWidget, orderStartRow, row - 1);

          orderWidget = createOrderWidget(rowItem);
          orderStartRow = row;

          lastOrder = rowItem.getOrderId();
        }

        if (customerChanged) {
          TimeBoardHelper.addRowSeparator(panel, STYLE_CUSTOMER_ROW_SEPARATOR, top, 0,
              getCustomerWidth() + getOrderWidth() + calendarWidth);
        } else if (orderChanged) {
          TimeBoardHelper.addRowSeparator(panel, STYLE_ORDER_ROW_SEPARATOR, top,
              getCustomerWidth(),
              getOrderWidth() + calendarWidth);
        } else {
          TimeBoardHelper.addRowSeparator(panel, top, getChartLeft(), calendarWidth);
        }
      }

      for (OrderCargo item : rowItems) {
        Widget itemWidget = createItemWidget(item);

        Rectangle rectangle = getRectangle(item.getRange(), row);
        TimeBoardHelper.apply(itemWidget, rectangle, margins);

        styleItemWidget(item, itemWidget);
        if (itemOpacity != null) {
          StyleUtils.setOpacity(itemWidget, itemOpacity);
        }

        panel.add(itemWidget);
      }
    }

    int lastRow = layoutRows.size() - 1;

    if (customerWidget != null) {
      addCustomerWidget(panel, customerWidget, lastCustomer, customerStartRow, lastRow);
    }
    if (orderWidget != null) {
      addOrderWidget(panel, orderWidget, orderStartRow, lastRow);
    }
  }

  @Override
  protected void renderMovers(ComplexPanel panel, int height) {
    Mover customerMover = TimeBoardHelper.createHorizontalMover();
    StyleUtils.setLeft(customerMover, getCustomerWidth() - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
    StyleUtils.setHeight(customerMover, height);

    customerMover.addMoveHandler(this::onCustomerResize);

    panel.add(customerMover);

    Mover orderMover = TimeBoardHelper.createHorizontalMover();
    StyleUtils.setLeft(orderMover, getChartLeft() - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
    StyleUtils.setHeight(orderMover, height);

    orderMover.addMoveHandler(this::onOrderResize);

    panel.add(orderMover);
  }

  @Override
  protected void resetFilter(FilterType filterType) {
    FilterHelper.resetFilter(items, filterType);
  }

  private void addCustomerWidget(HasWidgets panel, IdentifiableWidget widget, Long customerId,
      int firstRow, int lastRow) {

    Rectangle rectangle = TimeBoardHelper.getRectangle(0, getCustomerWidth(), firstRow, lastRow,
        getRowHeight());

    Edges margins = new Edges();
    margins.setRight(TimeBoardHelper.DEFAULT_MOVER_WIDTH);
    margins.setBottom(TimeBoardHelper.ROW_SEPARATOR_HEIGHT);

    TimeBoardHelper.apply(widget.asWidget(), rectangle, margins);
    panel.add(widget.asWidget());

    customerPanels.add(widget.getId());
    for (int row = firstRow; row <= lastRow; row++) {
      customersByRow.put(row, customerId);
    }
  }

  private void addOrderWidget(HasWidgets panel, IdentifiableWidget widget,
      int firstRow, int lastRow) {

    Rectangle rectangle = TimeBoardHelper.getRectangle(getCustomerWidth(), getOrderWidth(),
        firstRow, lastRow, getRowHeight());

    Edges margins = new Edges();
    margins.setRight(TimeBoardHelper.DEFAULT_MOVER_WIDTH);
    margins.setBottom(TimeBoardHelper.ROW_SEPARATOR_HEIGHT);

    TimeBoardHelper.apply(widget.asWidget(), rectangle, margins);
    panel.add(widget.asWidget());

    orderPanels.add(widget.getId());
  }

  private static IdentifiableWidget createCustomerWidget(OrderCargo item) {
    Label widget = new Label(item.getCustomerName());
    widget.addStyleName(STYLE_CUSTOMER_LABEL);

    bindOpener(widget, ClassifierConstants.VIEW_COMPANIES, item.getCustomerId());

    Simple panel = new Simple(widget);
    panel.addStyleName(STYLE_CUSTOMER_PANEL);

    return panel;
  }

  private Widget createItemWidget(OrderCargo item) {
    Flow panel = new Flow();
    panel.addStyleName(STYLE_ITEM_PANEL);
    setItemWidgetColor(item, panel);

    panel.setTitle(item.getTitle());

    bindOpener(panel, VIEW_ORDER_CARGO, item.getCargoId());

    DndHelper.makeSource(panel, DATA_TYPE_ORDER_CARGO, item, STYLE_ITEM_DRAG);

    renderCargoShipment(panel, item, null);

    return panel;
  }

  private static IdentifiableWidget createOrderWidget(OrderCargo item) {
    Flow panel = new Flow(STYLE_ORDER_PANEL);
    panel.setTitle(item.getOrderTitle());

    Label numberWidget = new Label(item.getOrderNo());
    numberWidget.addStyleName(STYLE_ORDER_NUMBER);

    bindOpener(numberWidget, VIEW_ORDERS, item.getOrderId());
    panel.add(numberWidget);

    String managerName = item.getManagerName();
    if (!BeeUtils.isEmpty(managerName)) {
      Label managerWidget = new Label(managerName);
      managerWidget.addStyleName(STYLE_ORDER_MANAGER);

      panel.add(managerWidget);
    }

    return panel;
  }

  private List<List<OrderCargo>> doLayout() {
    List<List<OrderCargo>> rows = new ArrayList<>();

    Long lastOrder = null;
    List<OrderCargo> rowItems = new ArrayList<>();

    for (OrderCargo item : items) {
      if (isItemVisible(item) && BeeUtils.intersects(getVisibleRange(), item.getRange())) {

        if (!Objects.equals(item.getOrderId(), lastOrder)
            || BeeUtils.intersects(rowItems, item.getRange())) {

          if (!rowItems.isEmpty()) {
            rows.add(new ArrayList<>(rowItems));
            rowItems.clear();
          }

          lastOrder = item.getOrderId();
        }

        rowItems.add(item);
      }
    }

    if (!rowItems.isEmpty()) {
      rows.add(new ArrayList<>(rowItems));
    }
    return rows;
  }

  private String findCustomerName(Long customerId) {
    for (OrderCargo item : items) {
      if (Objects.equals(item.getCustomerId(), customerId)) {
        return item.getCustomerName();
      }
    }

    return null;
  }

  private int getCustomerWidth() {
    return customerWidth;
  }

  private int getOrderWidth() {
    return orderWidth;
  }

  private void onCustomerResize(MoveEvent event) {
    int delta = event.getDeltaX();

    Element resizer = ((Mover) event.getSource()).getElement();
    int oldLeft = StyleUtils.getLeft(resizer);

    int newLeft = BeeUtils.clamp(oldLeft + delta, 1,
        getChartLeft() - TimeBoardHelper.DEFAULT_MOVER_WIDTH * 2 - 1);

    if (newLeft != oldLeft || event.isFinished()) {
      int customerPx = newLeft + TimeBoardHelper.DEFAULT_MOVER_WIDTH;
      int orderPx = getChartLeft() - customerPx;

      if (newLeft != oldLeft) {
        StyleUtils.setLeft(resizer, newLeft);

        for (String id : customerPanels) {
          StyleUtils.setWidth(DomUtils.getElement(id),
              customerPx - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
        }

        for (String id : orderPanels) {
          Element element = Document.get().getElementById(id);
          if (element != null) {
            StyleUtils.setLeft(element, customerPx);
            StyleUtils.setWidth(element, orderPx - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
          }
        }
      }

      if (event.isFinished()
          && updateSettings(COL_FX_PIXELS_PER_CUSTOMER, customerPx, COL_FX_PIXELS_PER_ORDER,
          orderPx)) {
        setCustomerWidth(customerPx);
        setOrderWidth(orderPx);
      }
    }
  }

  private void onOrderResize(MoveEvent event) {
    int delta = event.getDeltaX();

    Element resizer = ((Mover) event.getSource()).getElement();
    int oldLeft = StyleUtils.getLeft(resizer);

    int maxLeft = getLastResizableColumnMaxLeft(getCustomerWidth());
    int newLeft = BeeUtils.clamp(oldLeft + delta, getCustomerWidth() + 1, maxLeft);

    if (newLeft != oldLeft || event.isFinished()) {
      int orderPx = newLeft - getCustomerWidth() + TimeBoardHelper.DEFAULT_MOVER_WIDTH;

      if (newLeft != oldLeft) {
        StyleUtils.setLeft(resizer, newLeft);

        for (String id : orderPanels) {
          StyleUtils.setWidth(DomUtils.getElement(id),
              orderPx - TimeBoardHelper.DEFAULT_MOVER_WIDTH);
        }
      }

      if (event.isFinished() && updateSetting(COL_FX_PIXELS_PER_ORDER, orderPx)) {
        setOrderWidth(orderPx);
        render(false);
      }
    }
  }

  private void setCustomerWidth(int customerWidth) {
    this.customerWidth = customerWidth;
  }

  private void setOrderWidth(int orderWidth) {
    this.orderWidth = orderWidth;
  }
}
