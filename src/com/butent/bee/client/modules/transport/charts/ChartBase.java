package com.butent.bee.client.modules.transport.charts;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BoundType;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.animation.RafCallback;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.transport.TransportHandler;
import com.butent.bee.client.modules.transport.charts.CargoEvent.Type;
import com.butent.bee.client.modules.transport.charts.ChartFilter.FilterValue;
import com.butent.bee.client.modules.transport.charts.Filterable.FilterType;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.timeboard.TimeBoard;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasState;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.State;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.transport.TransportUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public abstract class ChartBase extends TimeBoard implements HasState {

  private static final BeeLogger logger = LogUtils.getLogger(ChartBase.class);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "tr-chart-";

  private static final String STYLE_SHIPMENT_DAY_PREFIX = STYLE_PREFIX + "shipment-day-";
  private static final String STYLE_SHIPMENT_DAY_PANEL = STYLE_SHIPMENT_DAY_PREFIX + "panel";
  private static final String STYLE_SHIPMENT_DAY_WIDGET = STYLE_SHIPMENT_DAY_PREFIX + "widget";
  private static final String STYLE_SHIPMENT_DAY_EMPTY = STYLE_SHIPMENT_DAY_PREFIX + "empty";
  private static final String STYLE_SHIPMENT_DAY_FLAG = STYLE_SHIPMENT_DAY_PREFIX + "flag";
  private static final String STYLE_SHIPMENT_DAY_LABEL = STYLE_SHIPMENT_DAY_PREFIX + "label";

  private static final String STYLE_STATE_PREFIX = STYLE_PREFIX + "state-";

  private static final int DEFAULT_LOCAL_REFRESH_INTERVAL_SECONDS = 0;
  private static final int DEFAULT_REMOTE_REFRESH_INTERVAL_SECONDS = 600;

  public static void registerBoards() {
    ensureStyleSheet();

    final ViewCallback showCallback = result -> BeeKeeper.getScreen().show(result);

    MenuService.FREIGHT_EXCHANGE.setHandler(parameters -> FreightExchange.open(showCallback));
    ViewFactory.registerSupplier(FreightExchange.SUPPLIER_KEY, FreightExchange::open);

    MenuService.SHIPPING_SCHEDULE.setHandler(parameters -> ShippingSchedule.open(showCallback));
    ViewFactory.registerSupplier(ShippingSchedule.SUPPLIER_KEY, ShippingSchedule::open);

    MenuService.DRIVER_TIME_BOARD.setHandler(parameters -> DriverTimeBoard.open(showCallback));
    ViewFactory.registerSupplier(DriverTimeBoard.SUPPLIER_KEY, DriverTimeBoard::open);

    MenuService.TRUCK_TIME_BOARD.setHandler(parameters -> TruckTimeBoard.open(showCallback));
    ViewFactory.registerSupplier(TruckTimeBoard.SUPPLIER_KEY, TruckTimeBoard::open);

    MenuService.TRAILER_TIME_BOARD.setHandler(parameters -> TrailerTimeBoard.open(showCallback));
    ViewFactory.registerSupplier(TrailerTimeBoard.SUPPLIER_KEY, TrailerTimeBoard::open);
  }

  private final Map<Long, String> transportGroups = new HashMap<>();

  private final Map<Long, Color> cargoTypeColors = new HashMap<>();
  private final Map<Long, String> cargoTypeNames = new HashMap<>();

  private final Multimap<Long, CargoHandling> cargoHandling = ArrayListMultimap.create();

  private boolean showCountryFlags;
  private boolean showPlaceInfo;

  private boolean showPlaceCities;
  private boolean showPlaceCodes;

  private boolean showAdditionalInfo;

  private boolean showOrderCustomer;
  private boolean showOrderNo;

  private boolean filterDependsOnData;

  private final Set<String> relevantDataViews = Sets.newHashSet(VIEW_ORDER_CARGO,
      VIEW_CARGO_TYPES, TBL_CARGO_LOADING, TBL_CARGO_UNLOADING, VIEW_CARGO_TRIPS, VIEW_TRIP_CARGO,
      VIEW_TRANSPORT_GROUPS, ClassifierConstants.VIEW_COUNTRIES,
      AdministrationConstants.VIEW_COLORS, AdministrationConstants.VIEW_THEME_COLORS);

  private final Timer refreshTimer;
  private long refreshTimerScheduled;

  private State state;

  private boolean updateFilterResults = true;

  private final List<ChartData> filterData = new ArrayList<>();

  protected ChartBase(ResponseObject settingsResponse) {
    super();
    initSettings(settingsResponse);

    this.refreshTimer = new Timer() {
      @Override
      public void run() {
        if (isAttached()) {
          refresh();
        }
      }
    };
  }

  @Override
  public State getState() {
    return state;
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case FILTER:
        setState(State.LOADING);

        updateFilterSelection(isFilterUpdated -> {
          updateFilterResults = false;
          if (isFilterUpdated) {
            FilterHelper.enableDataTypes(getFilterData(), getEnabledFilterDataTypes());
            formatManagerFilterValue(getFilterData());
          }

          setState(State.LOADED);

          FilterHelper.openDialog(getFilterData(), getAllFilterTypes(), getEnabledFilterDataTypes(),
              getSavedFilters(), new FilterHelper.DialogCallback() {
                @Override
                public boolean applySavedFilter(int index) {
                  return onApplyFilter(index);
                }

                @Override
                public void onDataTypesChange(Set<ChartDataType> types) {
                  updateEnabledFilterDataTypes(types);
                  updateFilterResults = true;
                  handleAction(Action.FILTER);
                }

                @Override
                public boolean onFilter() {
                  if (filterDependsOnData) {
                    updateFilterResults = true;
                  }
                  return applyFilterData(true, true);
                }

                @Override
                public void onSave(Callback<List<ChartFilter>> callback) {
                  onSaveFilter(callback);
                }

                @Override
                public void removeSavedFilter(int index, Callback<List<ChartFilter>> callback) {
                  onRemoveFilter(index, callback);
                }

                @Override
                public void setInitial(int index, boolean initial, Runnable callback) {
                  onSetInitialFilter(index, initial, callback);
                }
              });
        });
        break;

      case REMOVE_FILTER:
        clearFilter();
        refresh();
        break;

      default:
        super.handleAction(action);
    }
  }

  @Override
  public void setState(State state) {
    if (state != getState()) {
      if (getState() != null) {
        removeStyleName(STYLE_STATE_PREFIX + getState().name().toLowerCase());
      }

      this.state = state;

      if (state != null) {
        addStyleName(STYLE_STATE_PREFIX + state.name().toLowerCase());
      }

      logger.debug(NameUtils.getName(this), getId(), state);
    }
  }

  protected abstract void addFilterSettingParams(ParameterList params);

  protected void addFilterValuesToParams(ParameterList params) {
    if (!getFilterData().isEmpty()) {
      params.addDataItem(PRM_CHART_FILTER,
          Codec.beeSerialize(FilterHelper.updateServerFilter(getFilterData())));

    } else {
      Map<String, Set<Long>> filterValuesMap = new HashMap<>();

      getSavedFilters().stream().filter(ChartFilter::isInitial).forEach(savedFilter ->
        savedFilter.getValues().stream().filter(filter -> filter.getType().isServerFilter())
            .forEach(filter -> {
              if (filter.getType().isServerFilter()) {
                String typeOrdinalValue = BeeUtils.toString(filter.getType().ordinal());

                if (filterValuesMap.containsKey(typeOrdinalValue)) {
                  filterValuesMap.get(typeOrdinalValue).add(filter.getId());
                } else {
                  filterValuesMap.put(typeOrdinalValue, Sets.newHashSet(filter.getId()));
                }
              }
            }));

      if (filterValuesMap.isEmpty()
          && getEnabledFilterDataTypes().contains(ChartDataType.MANAGER)) {
        filterValuesMap.put(BeeUtils.toString(ChartDataType.MANAGER.ordinal()),
            Sets.newHashSet(BeeKeeper.getUser().getUserId()));
      }

      params.addDataItem(PRM_CHART_FILTER, Codec.beeSerialize(filterValuesMap));
    }
  }

  protected void addRelevantDataViews(String... viewNames) {
    if (viewNames != null) {
      for (String viewName : viewNames) {
        if (!BeeUtils.isEmpty(viewName)) {
          relevantDataViews.add(viewName);
        }
      }
    }
  }

  protected void clampMaxRange(String minDateColumn, String maxDateColumn) {
    JustDate from = TimeBoardHelper.getDate(getSettings(), minDateColumn);
    JustDate to = TimeBoardHelper.getDate(getSettings(), maxDateColumn);

    Range<Value> period = TransportUtils.getChartPeriod(from, to);

    if (period != null) {
      JustDate min = period.hasLowerBound() ? period.lowerEndpoint().getDate() : null;
      if (min != null && period.lowerBoundType() == BoundType.OPEN) {
        min = TimeUtils.nextDay(min);
      }

      JustDate max = period.hasUpperBound() ? period.upperEndpoint().getDate() : null;
      if (max != null && period.upperBoundType() == BoundType.OPEN) {
        max = TimeUtils.previousDay(max);
      }

      clampMaxRange(min, max);
    }
  }

  protected void clearFilter() {
    resetFilter(FilterType.TENTATIVE);
    resetFilter(FilterType.PERSISTENT);

    setFiltered(false);

    for (ChartData data : getFilterData()) {
      if (data != null) {
        data.deselectAll();
      }
    }

    refreshFilterInfo();
  }

  protected Widget createShipmentDayPanel(Multimap<Long, CargoEvent> dayEvents,
      String parentTitle) {

    Flow panel = new Flow();
    panel.addStyleName(STYLE_SHIPMENT_DAY_PANEL);

    Set<Long> countryIds = dayEvents.keySet();
    Size size = TimeBoardHelper.splitRectangle(getDayColumnWidth(), getRowHeight(),
        countryIds.size());

    if (size != null) {
      for (Long countryId : countryIds) {
        Widget widget = createShipmentDayWidget(countryId, dayEvents.get(countryId), parentTitle);
        StyleUtils.setSize(widget, size.getWidth(), size.getHeight());

        panel.add(widget);
      }
    }

    return panel;
  }

  protected Multimap<Long, CargoHandling> deserializeCargoHandling(String serialized) {
    Multimap<Long, CargoHandling> cargoHandlingData = ArrayListMultimap.create();

    if (!BeeUtils.isEmpty(serialized)) {
      long millis = System.currentTimeMillis();

      SimpleRowSet srs = SimpleRowSet.restore(serialized);
      for (SimpleRow row : srs) {
        cargoHandlingData.put(row.getLong(COL_CARGO), new CargoHandling(row));
      }

      logger.debug(PROP_CARGO_HANDLING, cargoHandlingData.size(), TimeUtils.elapsedMillis(millis));
    }
    return cargoHandlingData;
  }

  protected static void deserializeCountriesAndCities(String countriesData, String citiesData) {
    long millis;
    if (!BeeUtils.isEmpty(countriesData)) {
      millis = System.currentTimeMillis();
      int size = Places.setCountries(SimpleRowSet.restore(countriesData));
      logger.debug(PROP_COUNTRIES, size, TimeUtils.elapsedMillis(millis));
    }

    if (!BeeUtils.isEmpty(citiesData)) {
      millis = System.currentTimeMillis();
      int size = Places.setCities(Codec.deserializeHashMap(citiesData));
      logger.debug(PROP_CITIES, size, TimeUtils.elapsedMillis(millis));
    }
  }

  @Override
  protected void editSettings() {
    if (BeeUtils.isEmpty(getSettingsFormName()) || DataUtils.isEmpty(getSettings())) {
      return;
    }

    BeeRow oldSettings = getSettings().getRow(0);

    final BeeRow oldRow = DataUtils.cloneRow(oldSettings);
    final Long oldTheme = getColorTheme(oldSettings);

    RowEditor.openForm(getSettingsFormName(), Data.getDataInfo(getSettings().getViewName()),
        oldSettings,
        Opener.MODAL, result -> {
          if (result != null) {
            getSettings().clearRows();
            getSettings().addRow(DataUtils.cloneRow(result));

            boolean refresh = oldRow == null;
            Collection<String> colNames = getSettingsColumnsTriggeringRefresh();

            if (!refresh && !BeeUtils.isEmpty(colNames)) {
              for (String colName : colNames) {
                int index = getSettings().getColumnIndex(colName);
                if (!BeeConst.isUndef(index)
                    && !BeeUtils.equalsTrimRight(oldRow.getString(index),
                    result.getString(index))) {

                  refresh = true;
                  break;
                }
              }
            }

            if (refresh) {
              setSettings(null);
              refresh();

            } else {
              Long newTheme = getColorTheme(result);
              if (Objects.equals(oldTheme, newTheme)) {
                render(false);
              } else {
                updateColorTheme(newTheme);
              }
            }
          }
        }, new AbstractFormInterceptor() {

          @Override
          public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
            if (BeeUtils.allNotEmpty(getSettingsMinDate(), getSettingsMaxDate())) {
              DateTime start = getDateTimeValue(getSettingsMinDate());
              DateTime end = getDateTimeValue(getSettingsMaxDate());

              if (!Objects.isNull(start) && !Objects.isNull(end)) {
                boolean ok = TimeUtils.isMore(end, start);

                if (!ok) {
                  getFormView().focus(getSettingsMaxDate());
                  getFormView()
                      .notifySevere(Localized.dictionary().crmFinishDateMustBeGreaterThanStart());
                  event.consume();
                }
              }
            }
          }

          @Override
          public FormInterceptor getInstance() {
            return null;
          }
        });
  }

  protected abstract boolean filter(FilterType filterType);

  protected void filterData(FilterType filterType, Boolean filterServerSide,
      Consumer<Boolean> consumer) {
    if (filterServerSide) {
      setState(State.UPDATING);
      requestData(response -> {
        BeeRowSet rowSet = BeeRowSet.restore((String) response.getResponse());
        initDataForVisualization(rowSet);
        setState(State.LOADED);
        consumer.accept(filter(filterType));
      });
    } else {
      consumer.accept(filter(filterType));
    }
  }

  protected abstract Set<ChartDataType> getAllFilterTypes();

  protected Multimap<Long, CargoHandling> getCargoHandling() {
    return this.cargoHandling;
  }

  protected Collection<CargoHandling> getCargoHandling(Long cargoId, Long cargoTripId) {
    return getCargoHandling(getCargoHandling(), cargoId, cargoTripId);
  }

  protected Collection<CargoHandling> getCargoHandling(Multimap<Long, CargoHandling> handlingData,
      Long cargoId, Long cargoTripId) {
    if (!(cargoId != null && handlingData.containsKey(cargoId))) {
      return Collections.emptyList();

    } else if (cargoTripId == null) {
      return handlingData.get(cargoId);

    } else {
      Collection<CargoHandling> input = handlingData.get(cargoId);
      List<CargoHandling> result = new ArrayList<>();

      boolean hasLoading = false;
      boolean hasUnloading = false;

      for (CargoHandling ch : input) {
        if (Objects.equals(cargoTripId, ch.getCargoTripId())) {
          if (ch.isLoading()) {
            hasLoading = true;
          } else {
            hasUnloading = true;
          }

          result.add(ch);
        }
      }

      if (!hasLoading || !hasUnloading) {
        for (CargoHandling ch : input) {
          if (ch.getCargoTripId() == null) {
            if (!hasLoading && ch.isLoading() || !hasUnloading && ch.isUnloading()) {
              result.add(ch);
            }
          }
        }
      }

      return result;
    }
  }

  protected Pair<JustDate, JustDate> getCargoHandlingSpan(
      Multimap<Long, CargoHandling> handlingData, Long cargoId, Long cargoTripId) {
    JustDate minLoad = null;
    JustDate maxUnload = null;

    for (CargoHandling ch : getCargoHandling(handlingData, cargoId, cargoTripId)) {
      minLoad = BeeUtils.min(minLoad, ch.getLoadingDate());
      maxUnload = BeeUtils.max(maxUnload, ch.getUnloadingDate());
    }

    return Pair.of(minLoad, maxUnload);
  }

  protected String getCargoTypeName(Long id) {
    return cargoTypeNames.get(id);
  }

  protected abstract String getDataService();

  protected static int getDefaultDayColumnWidth(int chartWidth) {
    return Math.max(chartWidth / 10, 1);
  }

  @Override
  protected Set<Action> getEnabledActions() {
    return EnumSet.of(Action.FILTER, Action.REFRESH, Action.ADD, Action.CONFIGURE, Action.PRINT);
  }

  protected List<ChartData> getFilterData() {
    return filterData;
  }

  protected abstract String getFilterDataTypesColumnName();

  protected abstract String getFiltersColumnName();

  protected abstract String getFilterService();

  protected abstract String getRefreshLocalChangesColumnName();

  protected abstract String getRefreshRemoteChangesColumnName();

  protected abstract Collection<String> getSettingsColumnsTriggeringRefresh();

  protected abstract String getSettingsFormName();

  protected abstract String getSettingsMaxDate();

  protected abstract String getSettingsMinDate();

  protected abstract String getShowAdditionalInfoColumnName();

  protected abstract String getShowCountryFlagsColumnName();

  protected abstract String getShowOrderCustomerColumnName();

  protected abstract String getShowOderNoColumnName();

  protected abstract String getShowPlaceCitiesColumnName();

  protected abstract String getShowPlaceCodesColumnName();

  protected abstract String getShowPlaceInfoColumnName();

  protected abstract String getThemeColumnName();

  protected String getTransportGroupName(Long id) {
    return transportGroups.get(id);
  }

  protected abstract void initData(Map<String, String> properties);

  @Override
  protected boolean isDataEventRelevant(ModificationEvent<?> event) {
    return event != null && event.containsAny(relevantDataViews);
  }

  protected boolean isFilterDependsOnData() {
    return filterDependsOnData;
  }

  protected boolean isItemVisible(Filterable item) {
    return item != null && (!isFiltered() || item.matched(FilterType.PERSISTENT));
  }

  @Override
  protected void onRelevantDataEvent(ModificationEvent<?> event) {
    if (event != null && getState() != null && getState() != State.LOADING) {
      String colName = event.isSpookyActionAtADistance()
          ? getRefreshRemoteChangesColumnName() : getRefreshLocalChangesColumnName();

      Integer seconds = TimeBoardHelper.getInteger(getSettings(), colName);
      if (seconds == null) {
        seconds = event.isSpookyActionAtADistance()
            ? DEFAULT_REMOTE_REFRESH_INTERVAL_SECONDS : DEFAULT_LOCAL_REFRESH_INTERVAL_SECONDS;
      }

      if (seconds == 0) {
        refresh();
      } else if (seconds > 0) {
        scheduleRefresh(seconds);
      } else {
        setState(State.CHANGED);
      }
    }
  }

  @Override
  protected void onUnload() {
    super.onUnload();
    cancelRefreshTimer();
  }

  protected abstract boolean persistFilter();

  @Override
  protected void prepareDefaults(Size canvasSize) {
    super.prepareDefaults(canvasSize);

    String colName = getShowAdditionalInfoColumnName();
    if (!BeeUtils.isEmpty(colName)) {
      setShowAdditionalInfo(TimeBoardHelper.getBoolean(getSettings(), colName));
    }

    String colOrderCustomerName = getShowOrderCustomerColumnName();
    if (!BeeUtils.isEmpty(colOrderCustomerName)) {
      setShowOrderCustomer(TimeBoardHelper.getBoolean(getSettings(), colOrderCustomerName));
    }

    String colOrderNoName = getShowOderNoColumnName();
    if (!BeeUtils.isEmpty(colOrderNoName)) {
      setShowOrderNo(TimeBoardHelper.getBoolean(getSettings(), colOrderNoName));
    }

    setShowCountryFlags(TimeBoardHelper.getBoolean(getSettings(), getShowCountryFlagsColumnName()));
    setShowPlaceInfo(TimeBoardHelper.getBoolean(getSettings(), getShowPlaceInfoColumnName()));

    setShowPlaceCities(TimeBoardHelper.getBoolean(getSettings(), getShowPlaceCitiesColumnName()));
    setShowPlaceCodes(TimeBoardHelper.getBoolean(getSettings(), getShowPlaceCodesColumnName()));

    setFilterDependsOnData(TimeBoardHelper.getBoolean(getSettings(), COL_FILTER_DEPENDS_ON_DATA));
  }

  protected abstract List<ChartData> prepareFilterData(ResponseObject response);

  @Override
  protected void refresh() {
    cancelRefreshTimer();
    setState(State.LOADING);

    requestData(response -> {
      setState(State.UPDATING);

      scheduleDeferred(() -> {
        if (setData(response, false)) {
          render(false, new Callback<Integer>() {
            @Override
            public void onFailure(String... reason) {
              onSuccess(null);
              logger.warning(ArrayUtils.joinWords(reason));
            }

            @Override
            public void onSuccess(Integer result) {
              setState(refreshTimer.isRunning() ? State.PENDING : State.LOADED);
            }
          });

        } else {
          setState(refreshTimer.isRunning() ? State.PENDING : State.ERROR);
        }
      });
    });
  }

  @Override
  protected void render(boolean updateRange) {
    final State previousState = getState();

    setState(State.UPDATING);

    scheduleDeferred(() ->
        super.render(updateRange, new Callback<Integer>() {
          @Override
          public void onFailure(String... reason) {
            logger.warning(ArrayUtils.joinWords(reason));
            setState(previousState);
          }

          @Override
          public void onSuccess(Integer result) {
            if (getState() == State.UPDATING) {
              State currentState;

              if (previousState == State.PENDING && !refreshTimer.isRunning()) {
                currentState = State.LOADED;
              } else {
                currentState = previousState;
              }

              setState(currentState);
            }
          }
        }));
  }

  protected void renderCargoShipment(HasWidgets panel, OrderCargo cargo, String parentTitle) {
    renderCargoShipment(panel, cargo, parentTitle, null);
  }

  protected void renderCargoShipment(HasWidgets panel, OrderCargo cargo, String parentTitle,
      String styleInfo) {

    if (panel == null || cargo == null) {
      return;
    }

    if (showOrderNo() || showOrderCustomer()) {
      String orderInfo = null;
      if (showOrderNo()) {
        orderInfo = BeeUtils.joinWords(orderInfo, cargo.getOrderNo());
      }

      if (showOrderCustomer()) {
        orderInfo = BeeUtils.joinWords(orderInfo, cargo.getCustomerName());
      }

      if (!BeeUtils.isEmpty(orderInfo)) {
        CustomDiv infoWidget = new CustomDiv(styleInfo);
        infoWidget.setText(orderInfo);
        panel.add(infoWidget);
      }
    }

    Range<JustDate> range = TimeBoardHelper.normalizedIntersection(cargo.getRange(),
        getVisibleRange());
    if (range == null) {
      return;
    }

    Multimap<JustDate, CargoEvent> cargoLayout = splitCargoByDate(cargo, range);
    if (cargoLayout.isEmpty()) {
      return;
    }

    for (JustDate date : cargoLayout.keySet()) {
      Multimap<Long, CargoEvent> dayLayout = CargoEvent.splitByCountry(cargoLayout.get(date));
      if (!dayLayout.isEmpty()) {
        Widget dayWidget = createShipmentDayPanel(dayLayout, parentTitle);

        StyleUtils.setLeft(dayWidget, getRelativeLeft(range, date));
        StyleUtils.setWidth(dayWidget, getDayColumnWidth());

        panel.add(dayWidget);
      }
    }
  }

  protected void renderTrip(HasWidgets panel, String title, String additionalInfo,
      Collection<? extends OrderCargo> cargos, Range<JustDate> range,
      String styleVoid, String styleInfo) {

    List<Range<JustDate>> voidRanges;

    if (BeeUtils.isEmpty(cargos)) {
      voidRanges = new ArrayList<>();
      voidRanges.add(range);

    } else {
      Multimap<JustDate, CargoEvent> tripLayout = splitTripByDate(cargos, range);
      Set<JustDate> eventDates = tripLayout.keySet();

      for (JustDate date : eventDates) {
        Multimap<Long, CargoEvent> dayLayout = CargoEvent.splitByCountry(tripLayout.get(date));
        if (!dayLayout.isEmpty()) {
          Widget dayWidget = createShipmentDayPanel(dayLayout, title);

          StyleUtils.setLeft(dayWidget, getRelativeLeft(range, date));
          StyleUtils.setWidth(dayWidget, getDayColumnWidth());

          panel.add(dayWidget);
        }
      }

      voidRanges = Trip.getVoidRanges(range, eventDates, cargos);
    }

    for (Range<JustDate> voidRange : voidRanges) {
      Widget voidWidget = new CustomDiv(styleVoid);

      StyleUtils.setLeft(voidWidget, getRelativeLeft(range, voidRange.lowerEndpoint()));
      StyleUtils.setWidth(voidWidget, TimeBoardHelper.getSize(voidRange) * getDayColumnWidth());

      panel.add(voidWidget);
    }

    if (showAdditionalInfo() && !BeeUtils.isEmpty(additionalInfo)) {
      CustomDiv infoWidget = new CustomDiv(styleInfo);
      infoWidget.setText(additionalInfo);

      panel.add(infoWidget);
    }
  }

  protected void requestData(ResponseCallback responseCallback) {
    ParameterList params = TransportHandler.createArgs(getDataService());
    addFilterValuesToParams(params);
    BeeKeeper.getRpc().makePostRequest(params, responseCallback);
  }

  protected abstract void resetFilter(FilterType filterType);

  @Override
  protected boolean setData(ResponseObject response, boolean init) {
    updateFilterResults = true;

    long startMillis = System.currentTimeMillis();
    long millis;

    if (!Queries.checkResponse(getCaption(), null, response, BeeRowSet.class)) {
      return false;
    }

    BeeRowSet rowSet = BeeRowSet.restore((String) response.getResponse());

    if (getSettings() == null) {
      setSettings(rowSet);
    }

    logger.debug(rowSet.getViewName(), TimeUtils.elapsedMillis(startMillis));

    initDataForVisualization(rowSet);

    millis = System.currentTimeMillis();
    updateMaxRange();

    logger.debug("update max range", TimeUtils.elapsedMillis(millis));

    millis = System.currentTimeMillis();
    if (init) {
      initFilterData();
      setState(State.LOADED);
    } else {
      updateFilterData(null);
      applyFilterData(false, false);

    }
    logger.debug(init ? "init" : "update", "filter data", TimeUtils.elapsedMillis(millis));

    logger.debug("total set data", TimeUtils.elapsedMillis(startMillis));
    return true;
  }

  @Override
  protected void setItemWidgetColor(HasDateRange item, Widget widget) {
    if (item instanceof HasCargoType) {
      Long cargoType = ((HasCargoType) item).getCargoType();

      if (cargoType != null && cargoTypeColors.containsKey(cargoType)) {
        UiHelper.setColor(widget, cargoTypeColors.get(cargoType));
        return;
      }
    }

    super.setItemWidgetColor(item, widget);
  }

  protected Multimap<JustDate, CargoEvent> splitCargoByDate(OrderCargo cargo,
      Range<JustDate> range) {

    Multimap<JustDate, CargoEvent> result = ArrayListMultimap.create();
    if (cargo == null || range == null || range.isEmpty()) {
      return result;
    }

    if (cargo.getLoadingDate() != null && range.contains(cargo.getLoadingDate())) {
      result.put(cargo.getLoadingDate(), new CargoEvent(cargo, true));
    }

    if (cargo.getUnloadingDate() != null && range.contains(cargo.getUnloadingDate())) {
      result.put(cargo.getUnloadingDate(), new CargoEvent(cargo, false));
    }

    for (CargoHandling ch : getCargoHandling(cargo.getCargoId(), cargo.getCargoTripId())) {
      if (ch.getLoadingDate() != null && range.contains(ch.getLoadingDate())) {
        result.put(ch.getLoadingDate(), new CargoEvent(cargo, ch, true));
      }

      if (ch.getUnloadingDate() != null && range.contains(ch.getUnloadingDate())) {
        result.put(ch.getUnloadingDate(), new CargoEvent(cargo, ch, false));
      }
    }

    return result;
  }

  protected Multimap<JustDate, CargoEvent> splitTripByDate(Collection<? extends OrderCargo> cargos,
      Range<JustDate> range) {

    Multimap<JustDate, CargoEvent> result = ArrayListMultimap.create();
    if (BeeUtils.isEmpty(cargos)) {
      return result;
    }

    for (OrderCargo cargo : cargos) {
      result.putAll(splitCargoByDate(cargo, range));
    }

    return result;
  }

  private boolean applyFilterData(Boolean filterServerSide, boolean checkIfSelected) {
    if (checkIfSelected) {
      final List<FilterValue> filterValues = FilterHelper.getSelectedValues(getFilterData());

      if (BeeUtils.isEmpty(filterValues)) {
        BeeKeeper.getScreen().notifyWarning(Localized.dictionary().noDataSelectedInFilter());
        return false;
      }
    }

    filterData(FilterType.TENTATIVE, filterServerSide, filtered -> {
      setFiltered(filtered);

      render(false);

      if (isFiltered()) {
        persistFilter();
      }
      refreshFilterInfo();
    });

    return true;
  }

  private void cancelRefreshTimer() {
    if (refreshTimer.isRunning()) {
      refreshTimer.cancel();
    }
  }

  private Widget createShipmentDayWidget(Long countryId, Collection<CargoEvent> events,
      String parentTitle) {

    Flow widget = new Flow();
    widget.addStyleName(STYLE_SHIPMENT_DAY_WIDGET);

    String flag = showCountryFlags() ? Places.getCountryFlag(countryId) : null;
    String countryLabel = Places.getCountryLabel(countryId);

    if (!BeeUtils.isEmpty(flag)) {
      widget.addStyleName(STYLE_SHIPMENT_DAY_FLAG);
      StyleUtils.setBackgroundImage(widget, flag);
    }

    if (!BeeUtils.isEmpty(events)) {
      if (showPlaceInfo()) {
        List<String> info = new ArrayList<>();

        if (BeeUtils.isEmpty(flag) && !BeeUtils.isEmpty(countryLabel)) {
          info.add(countryLabel);
        }

        for (CargoEvent event : events) {
          String place = event.getPlace();
          if (!BeeUtils.isEmpty(place) && !BeeUtils.containsSame(info, place)) {
            info.add(place);
          }

          String number = event.getNumber();
          if (!BeeUtils.isEmpty(number) && BeeUtils.containsSame(info, number)) {
            info.add(number);
          }

          if (showPlaceCities()) {
            String cityLabel = Places.getCityLabel(event.getCityId());
            if (!BeeUtils.isEmpty(cityLabel) && !BeeUtils.containsSame(info, cityLabel)) {
              info.add(cityLabel);
            }
          }
        }

        if (!info.isEmpty()) {
          CustomDiv label = new CustomDiv(STYLE_SHIPMENT_DAY_LABEL);
          label.setHtml(BeeUtils.join(BeeConst.STRING_SPACE, info));

          widget.add(label);
        }

      } else if (showPlaceCities()) {
        List<String> info = new ArrayList<>();

        for (CargoEvent event : events) {
          String cityLabel = Places.getCityLabel(event.getCityId());
          if (!BeeUtils.isEmpty(cityLabel) && !BeeUtils.containsSame(info, cityLabel)) {
            info.add(cityLabel);
          }
        }

        if (!info.isEmpty()) {
          CustomDiv label = new CustomDiv(STYLE_SHIPMENT_DAY_LABEL);
          label.setHtml(BeeUtils.join(BeeConst.STRING_SPACE, info));

          widget.add(label);
        }
      }

      if (showPlaceCodes()) {
        List<String> info = new ArrayList<>();

        for (CargoEvent event : events) {
          String codeLabel = event.getPostIndex();
          if (!BeeUtils.isEmpty(codeLabel) && !BeeUtils.containsSame(info, codeLabel)) {
            info.add(codeLabel);
          }
        }

        if (!info.isEmpty() && !BeeUtils.isEmpty(countryLabel)
            && BeeUtils.filterContext(info, countryLabel).isEmpty()) {
          info.add(0, countryLabel);
        }

        if (!info.isEmpty()) {
          CustomDiv label = new CustomDiv(STYLE_SHIPMENT_DAY_LABEL);
          label.setHtml(BeeUtils.join(BeeConst.STRING_SPACE, info));

          widget.add(label);
        }
      }

      List<String> title = new ArrayList<>();

      Multimap<OrderCargo, CargoEvent> eventsByCargo = LinkedListMultimap.create();
      for (CargoEvent event : events) {
        eventsByCargo.put(event.getCargo(), event);
      }

      for (OrderCargo cargo : eventsByCargo.keySet()) {
        Map<CargoHandling, EnumSet<CargoEvent.Type>> handlingEvents = new HashMap<>();

        for (CargoEvent event : eventsByCargo.get(cargo)) {
          if (event.isHandlingEvent()) {
            CargoEvent.Type eventType = event.isLoading()
                ? CargoEvent.Type.LOADING : CargoEvent.Type.UNLOADING;

            if (handlingEvents.containsKey(event.getHandling())) {
              handlingEvents.get(event.getHandling()).add(eventType);
            } else {
              handlingEvents.put(event.getHandling(), EnumSet.of(eventType));
            }
          }
        }

        if (!title.isEmpty()) {
          title.add(BeeConst.STRING_NBSP);
        }
        title.add(cargo.getTitle());

        if (!handlingEvents.isEmpty()) {
          title.add(BeeConst.STRING_NBSP);

          for (Map.Entry<CargoHandling, EnumSet<Type>> entry : handlingEvents.entrySet()) {
            String chLoading = entry.getValue().contains(CargoEvent.Type.LOADING)
                ? Places.getLoadingInfo(entry.getKey()) : null;
            String chUnloading = entry.getValue().contains(CargoEvent.Type.UNLOADING)
                ? Places.getUnloadingInfo(entry.getKey()) : null;

            title.add(CargoHandling.getTitle(chLoading, chUnloading));
          }
        }
      }

      if (!BeeUtils.isEmpty(parentTitle)) {
        title.add(BeeConst.STRING_NBSP);
        title.add(parentTitle);
      }

      if (!title.isEmpty()) {
        widget.setTitle(BeeUtils.join(BeeConst.STRING_EOL, title));
      }
    }

    if (widget.isEmpty() && BeeUtils.isEmpty(flag)) {
      widget.addStyleName(STYLE_SHIPMENT_DAY_EMPTY);
    }

    return widget;
  }

  private void formatManagerFilterValue(List<ChartData> filters) {
    ChartData managerChartData = FilterHelper.getDataByType(filters, ChartDataType.MANAGER);
    if (managerChartData != null && !managerChartData.contains(BeeKeeper.getUser().getUserId())) {
      String managerLabel = BeeUtils.joinWords(
          BeeKeeper.getUser().getFirstName(), BeeKeeper.getUser().getLastName());
      managerChartData.add(managerLabel, BeeKeeper.getUser().getUserId());
    }
  }

  private Long getColorTheme(BeeRow row) {
    if (row == null || BeeUtils.isEmpty(getThemeColumnName()) || DataUtils.isEmpty(getSettings())) {
      return null;
    } else {
      return row.getLong(getSettings().getColumnIndex(getThemeColumnName()));
    }
  }

  protected Set<ChartDataType> getEnabledFilterDataTypes() {
    Set<ChartDataType> types = EnumSet.noneOf(ChartDataType.class);

    String s = TimeBoardHelper.getString(getSettings(), getFilterDataTypesColumnName());
    if (!BeeUtils.isEmpty(s)) {
      types.addAll(EnumUtils.parseNameSet(ChartDataType.class, s));
    } else {
      types.addAll(getAllFilterTypes());
    }

    return types;
  }

  private long getRefreshTimerScheduled() {
    return refreshTimerScheduled;
  }

  private List<ChartFilter> getSavedFilters() {
    List<ChartFilter> filters = new ArrayList<>();

    String s = TimeBoardHelper.getString(getSettings(), getFiltersColumnName());
    if (!BeeUtils.isEmpty(s)) {
      filters.addAll(ChartFilter.restoreList(s));
    }

    return filters;
  }

  private void initDataForVisualization(BeeRowSet rowSet) {
    long millis;
    deserializeCountriesAndCities(rowSet.getTableProperty(PROP_COUNTRIES),
        rowSet.getTableProperty(PROP_CITIES));

    String serialized = rowSet.getTableProperty(PROP_COLORS);
    if (!BeeUtils.isEmpty(serialized)) {
      millis = System.currentTimeMillis();
      int size = restoreColors(serialized);
      logger.debug(PROP_COLORS, size, TimeUtils.elapsedMillis(millis));
    }

    transportGroups.clear();
    serialized = rowSet.getTableProperty(PROP_TRANSPORT_GROUPS);
    if (!BeeUtils.isEmpty(serialized)) {
      millis = System.currentTimeMillis();

      BeeRowSet groups = BeeRowSet.restore(serialized);
      int nameIndex = groups.getColumnIndex(COL_GROUP_NAME);

      for (BeeRow group : groups) {
        String name = group.getString(nameIndex);
        if (!BeeUtils.isEmpty(name)) {
          transportGroups.put(group.getId(), name);
        }
      }
      logger.debug(PROP_TRANSPORT_GROUPS, transportGroups.size(), TimeUtils.elapsedMillis(millis));
    }

    cargoTypeColors.clear();
    cargoTypeNames.clear();

    serialized = rowSet.getTableProperty(PROP_CARGO_TYPES);
    if (!BeeUtils.isEmpty(serialized)) {
      millis = System.currentTimeMillis();

      BeeRowSet cargoTypes = BeeRowSet.restore(serialized);

      int nameIndex = cargoTypes.getColumnIndex(COL_CARGO_TYPE_NAME);
      int colorIndex = cargoTypes.getColumnIndex(COL_CARGO_TYPE_COLOR);
      int bgIndex = cargoTypes.getColumnIndex(COL_BACKGROUND);
      int fgIndex = cargoTypes.getColumnIndex(COL_FOREGROUND);

      for (BeeRow cargoType : cargoTypes) {
        String name = cargoType.getString(nameIndex);
        if (!BeeUtils.isEmpty(name)) {
          cargoTypeNames.put(cargoType.getId(), name);
        }

        String bg = cargoType.getString(bgIndex);
        String fg = cargoType.getString(fgIndex);

        if (!BeeUtils.isEmpty(bg)) {
          cargoTypeColors.put(cargoType.getId(),
              new Color(cargoType.getLong(colorIndex), bg.trim(), BeeUtils.trim(fg)));
        }
      }

      logger.debug(PROP_CARGO_TYPES, cargoTypeNames.size(), cargoTypeColors.size(),
          TimeUtils.elapsedMillis(millis));
    }

    cargoHandling.clear();
    cargoHandling.putAll(deserializeCargoHandling(rowSet.getTableProperty(PROP_CARGO_HANDLING)));

    millis = System.currentTimeMillis();
    initData(rowSet.getTableProperties());
    logger.debug("init data", TimeUtils.elapsedMillis(millis));
  }

  private void initFilterData() {
    if (!getFilterData().isEmpty()) {
      getFilterData().clear();
    }
    if (isFiltered()) {
      clearFilter();
    }

    List<ChartData> data = FilterHelper.onlyEnabledAndSavedData(prepareFilterData(null),
        getEnabledFilterDataTypes(), getSavedFilters());

    if (!BeeUtils.isEmpty(data)) {
      List<ChartFilter> savedFilters = getSavedFilters();
      boolean filter = false;

      for (ChartFilter cf : savedFilters) {
        if (cf.isInitial()) {
          filter |= cf.applyTo(data);
        }
      }
      if (!filter) {
        ChartData managerFilter = FilterHelper.getDataByType(data, ChartDataType.MANAGER);

        if (managerFilter != null) {
          String managerLabel = BeeUtils.joinWords(
              BeeKeeper.getUser().getFirstName(), BeeKeeper.getUser().getLastName());
          managerFilter.setItemSelected(managerLabel, true);
        }
      }

      getFilterData().addAll(data);

      applyFilterData(false, false);
    }
  }

  private void initSettings(ResponseObject settingsResponse) {
    Long millis = System.currentTimeMillis();
    BeeRowSet rowSet = BeeRowSet.restore((String) settingsResponse.getResponse());
    setSettings(rowSet);

    logger.debug("init", "settings", TimeUtils.elapsedMillis(millis));
  }

  private boolean onApplyFilter(int index) {
    List<ChartFilter> filters = getSavedFilters();

    if (BeeUtils.isIndex(filters, index)) {
      ChartFilter cf = filters.get(index);

      if (cf.matches(getFilterData())) {
        clearFilter();

        if (cf.applyTo(getFilterData())) {
          applyFilterData(true, false);
        }

        return true;

      } else {
        BeeKeeper.getScreen().notifyWarning(cf.getLabel(), Localized.dictionary().nothingFound());
      }
    }

    return false;
  }

  private void onRemoveFilter(final int index, final Callback<List<ChartFilter>> callback) {
    final List<ChartFilter> filters = getSavedFilters();

    if (BeeUtils.isIndex(filters, index)) {
      Global.confirmDelete(Localized.dictionary().removeFilter(), Icon.QUESTION,
          Collections.singletonList(filters.get(index).getLabel()), () -> {
            filters.remove(index);
            String serialized = filters.isEmpty() ? null : Codec.beeSerialize(filters);

            TimeBoardHelper.updateSettings(getSettings(), getFiltersColumnName(), serialized,
                () -> callback.onSuccess(filters));
          });
    }
  }

  private void onSaveFilter(final Callback<List<ChartFilter>> callback) {
    final List<FilterValue> filterValues = FilterHelper.getSelectedValues(getFilterData());

    if (BeeUtils.isEmpty(filterValues)) {
      BeeKeeper.getScreen().notifyWarning(Localized.dictionary().noDataSelectedInFilter());

    } else {
      String label = BeeUtils.left(FilterHelper.getSelectionLabel(getFilterData()),
          ChartFilter.MAX_LABEL_LENGTH);

      Global.inputString(Localized.dictionary().saveFilter(), Localized.dictionary().name(),
          new StringCallback() {
            @Override
            public void onSuccess(String value) {
              ChartFilter filter = new ChartFilter(BeeUtils.trim(value), filterValues);
              List<ChartFilter> filters = getSavedFilters();
              FilterHelper.addFilter(filters, filter);

              TimeBoardHelper.updateSettings(getSettings(), getFiltersColumnName(),
                  Codec.beeSerialize(filters), () -> callback.onSuccess(filters));
            }
          }, null, label, ChartFilter.MAX_LABEL_LENGTH, null, 40, CssUnit.EM);
    }
  }

  private void onSetInitialFilter(int index, boolean initial, final Runnable callback) {
    List<ChartFilter> filters = getSavedFilters();

    if (BeeUtils.isIndex(filters, index) && filters.get(index).isInitial() != initial) {
      filters.get(index).setInitial(initial);
      TimeBoardHelper.updateSettings(getSettings(), getFiltersColumnName(),
          Codec.beeSerialize(filters), callback);
    }
  }

  private void refreshFilterInfo() {
    String label = FilterHelper.getSelectionLabel(getFilterData());

    if (BeeUtils.isEmpty(label)) {
      getFilterLabel().getElement().setInnerText(BeeConst.STRING_EMPTY);
      getRemoveFilter().setVisible(false);
    } else {
      getFilterLabel().getElement().setInnerText(label);
      getRemoveFilter().setVisible(true);
    }
  }

  private static void scheduleDeferred(Runnable command) {
    RafCallback raf = new RafCallback(10_000) {
      @Override
      protected boolean run(double elapsed) {
        return false;
      }

      @Override
      protected void onComplete() {
        command.run();
      }
    };

    raf.start();
  }

  private void scheduleRefresh(int seconds) {
    int delayMillis = seconds * TimeUtils.MILLIS_PER_SECOND;
    long now = System.currentTimeMillis();

    if (!refreshTimer.isRunning()
        || now + delayMillis + TimeUtils.MILLIS_PER_SECOND < getRefreshTimerScheduled()) {

      setState(State.PENDING);

      setRefreshTimerScheduled(now + delayMillis);

      if (refreshTimer.isRunning()) {
        refreshTimer.cancel();
      }
      refreshTimer.schedule(delayMillis);
    }
  }

  private void setFilterDependsOnData(boolean filterDependsOnData) {
    this.filterDependsOnData = filterDependsOnData;
  }

  private void setRefreshTimerScheduled(long refreshTimerScheduled) {
    this.refreshTimerScheduled = refreshTimerScheduled;
  }

  private void setShowOrderCustomer(boolean showOrderCustomer) {
    this.showOrderCustomer = showOrderCustomer;
  }

  private void setShowOrderNo(boolean showOrderNo) {
    this.showOrderNo = showOrderNo;
  }

  private void setShowAdditionalInfo(boolean showAdditionalInfo) {
    this.showAdditionalInfo = showAdditionalInfo;
  }

  private void setShowCountryFlags(boolean showCountryFlags) {
    this.showCountryFlags = showCountryFlags;
  }

  private void setShowPlaceCities(boolean showPlaceCities) {
    this.showPlaceCities = showPlaceCities;
  }

  private void setShowPlaceCodes(boolean showPlaceCodes) {
    this.showPlaceCodes = showPlaceCodes;
  }

  private void setShowPlaceInfo(boolean showPlaceInfo) {
    this.showPlaceInfo = showPlaceInfo;
  }

  private boolean showAdditionalInfo() {
    return showAdditionalInfo;
  }

  private boolean showCountryFlags() {
    return showCountryFlags;
  }

  private boolean showOrderCustomer() {
    return showOrderCustomer;
  }

  private boolean showOrderNo() {
    return showOrderNo;
  }

  private boolean showPlaceCities() {
    return showPlaceCities;
  }

  private boolean showPlaceCodes() {
    return showPlaceCodes;
  }

  private boolean showPlaceInfo() {
    return showPlaceInfo;
  }

  private void updateColorTheme(Long theme) {
    ParameterList args = TransportHandler.createArgs(SVC_GET_COLORS);
    if (theme != null) {
      args.addQueryItem(Service.VAR_ID, theme);
    }

    BeeKeeper.getRpc().makePostRequest(args, response -> {
      restoreColors(response.getResponseAsString());
      render(false);
    });
  }

  private void updateFilterSelection(Consumer<Boolean> filterUpdateConsumer) {
    if (updateFilterResults) {
      if (isFilterDependsOnData()) {
        updateFilterData(null);
        filterUpdateConsumer.accept(true);

      } else {
        ParameterList params = TransportHandler.createArgs(getFilterService());
        addFilterSettingParams(params);

        BeeKeeper.getRpc().makePostRequest(params, response -> {
          updateFilterData(response);
          filterUpdateConsumer.accept(true);
        });
      }
    } else {
      filterUpdateConsumer.accept(false);
    }
  }

  private boolean updateEnabledFilterDataTypes(Set<ChartDataType> types) {
    if (!DataUtils.isEmpty(getSettings())
        && getSettings().containsColumn(getFilterDataTypesColumnName())
        && !BeeUtils.sameElements(types, getEnabledFilterDataTypes())) {

      return TimeBoardHelper.updateSettings(getSettings(), getFilterDataTypesColumnName(),
          Strings.emptyToNull(EnumUtils.joinNames(types)), null);

    } else {
      return false;
    }
  }

  private void updateFilterData(ResponseObject response) {
    List<ChartData> newData = FilterHelper.onlyEnabledAndSavedData(prepareFilterData(response),
        getEnabledFilterDataTypes(), getSavedFilters());

    if (BeeUtils.isEmpty(newData)) {
      getFilterData().clear();

    } else if (getFilterData().isEmpty()) {
      getFilterData().addAll(newData);

    } else {
      for (ChartData ocd : getFilterData()) {
        ChartData ncd = FilterHelper.getDataByType(newData, ocd.getType());

        if (ncd != null) {
          Collection<String> selectedItems = ocd.getSelectedItems();
          for (String item : selectedItems) {
            ncd.setItemSelected(item, true);
          }
        }
      }

      getFilterData().clear();
      getFilterData().addAll(newData);
    }
  }
}
