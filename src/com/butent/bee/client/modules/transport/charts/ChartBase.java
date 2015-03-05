package com.butent.bee.client.modules.transport.charts;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.transport.TransportHandler;
import com.butent.bee.client.modules.transport.charts.CargoEvent.Type;
import com.butent.bee.client.modules.transport.charts.Filterable.FilterType;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.timeboard.TimeBoard;
import com.butent.bee.client.timeboard.TimeBoardHelper;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewSupplier;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.Size;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.event.DataEvent;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class ChartBase extends TimeBoard {

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "tr-chart-";

  private static final String STYLE_SHIPMENT_DAY_PREFIX = STYLE_PREFIX + "shipment-day-";
  private static final String STYLE_SHIPMENT_DAY_PANEL = STYLE_SHIPMENT_DAY_PREFIX + "panel";
  private static final String STYLE_SHIPMENT_DAY_WIDGET = STYLE_SHIPMENT_DAY_PREFIX + "widget";
  private static final String STYLE_SHIPMENT_DAY_EMPTY = STYLE_SHIPMENT_DAY_PREFIX + "empty";
  private static final String STYLE_SHIPMENT_DAY_FLAG = STYLE_SHIPMENT_DAY_PREFIX + "flag";
  private static final String STYLE_SHIPMENT_DAY_LABEL = STYLE_SHIPMENT_DAY_PREFIX + "label";

  public static void registerBoards() {
    ensureStyleSheet();

    final ViewCallback showCallback = new ViewCallback() {
      @Override
      public void onSuccess(View result) {
        BeeKeeper.getScreen().show(result);
      }
    };

    MenuService.FREIGHT_EXCHANGE.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        FreightExchange.open(showCallback);
      }
    });

    ViewFactory.registerSupplier(FreightExchange.SUPPLIER_KEY, new ViewSupplier() {
      @Override
      public void create(ViewCallback callback) {
        FreightExchange.open(callback);
      }
    });

    MenuService.SHIPPING_SCHEDULE.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        ShippingSchedule.open(showCallback);
      }
    });

    ViewFactory.registerSupplier(ShippingSchedule.SUPPLIER_KEY, new ViewSupplier() {
      @Override
      public void create(ViewCallback callback) {
        ShippingSchedule.open(callback);
      }
    });

    MenuService.DRIVER_TIME_BOARD.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        DriverTimeBoard.open(showCallback);
      }
    });

    ViewFactory.registerSupplier(DriverTimeBoard.SUPPLIER_KEY, new ViewSupplier() {
      @Override
      public void create(ViewCallback callback) {
        DriverTimeBoard.open(callback);
      }
    });

    MenuService.TRUCK_TIME_BOARD.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        TruckTimeBoard.open(showCallback);
      }
    });

    ViewFactory.registerSupplier(TruckTimeBoard.SUPPLIER_KEY, new ViewSupplier() {
      @Override
      public void create(ViewCallback callback) {
        TruckTimeBoard.open(callback);
      }
    });

    MenuService.TRAILER_TIME_BOARD.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        TrailerTimeBoard.open(showCallback);
      }
    });

    ViewFactory.registerSupplier(TrailerTimeBoard.SUPPLIER_KEY, new ViewSupplier() {
      @Override
      public void create(ViewCallback callback) {
        TrailerTimeBoard.open(callback);
      }
    });
  }

  private final Multimap<Long, CargoHandling> cargoHandling = ArrayListMultimap.create();

  private boolean showCountryFlags;
  private boolean showPlaceInfo;

  private boolean showPlaceCities;
  private boolean showPlaceCodes;

  private final Set<String> relevantDataViews = Sets.newHashSet(VIEW_ORDER_CARGO,
      VIEW_CARGO_HANDLING, VIEW_CARGO_TRIPS, VIEW_TRIP_CARGO, ClassifierConstants.VIEW_COUNTRIES,
      AdministrationConstants.VIEW_COLORS, AdministrationConstants.VIEW_THEME_COLORS);

  private final List<ChartData> filterData = new ArrayList<>();

  protected ChartBase() {
    super();
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case FILTER:
        FilterHelper.openDialog(filterData, new FilterHelper.DialogCallback() {
          @Override
          public void onClear() {
            resetFilter(FilterType.TENTATIVE);
          }

          @Override
          public void onFilter() {
            setFiltered(persistFilter());
            refreshFilterInfo();
            render(false);
          }

          @Override
          public void onSelectionChange(HasWidgets dataContainer) {
            filter(FilterType.TENTATIVE);
            FilterHelper.enableData(getFilterData(), prepareFilterData(FilterType.TENTATIVE),
                dataContainer);
          }
        });
        break;

      case REMOVE_FILTER:
        clearFilter();
        render(false);
        break;

      default:
        super.handleAction(action);
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

  protected void clearFilter() {
    resetFilter(FilterType.TENTATIVE);
    resetFilter(FilterType.PERSISTENT);

    setFiltered(false);

    for (ChartData data : filterData) {
      if (data != null) {
        data.enableAll();
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

  @Override
  protected void editSettings() {
    if (BeeUtils.isEmpty(getSettingsFormName()) || DataUtils.isEmpty(getSettings())) {
      return;
    }

    BeeRow oldSettings = getSettings().getRow(0);
    final Long oldTheme = getColorTheme(oldSettings);

    RowEditor.openForm(getSettingsFormName(), getSettings().getViewName(), oldSettings,
        Opener.MODAL, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            if (result != null) {
              getSettings().clearRows();
              getSettings().addRow(DataUtils.cloneRow(result));

              if (BeeUtils.isEmpty(getThemeColumnName())) {
                render(false);

              } else {
                Long newTheme = getColorTheme(result);
                if (Objects.equals(oldTheme, newTheme)) {
                  render(false);
                } else {
                  updateColorTheme(newTheme);
                }
              }
            }
          }
        });
  }

  protected abstract boolean filter(FilterType filterType);

  protected Collection<CargoHandling> getCargoHandling(Long cargoId) {
    return cargoHandling.get(cargoId);
  }

  protected Pair<JustDate, JustDate> getCargoHandlingSpan(Long cargoId) {
    JustDate minLoad = null;
    JustDate maxUnload = null;

    if (hasCargoHandling(cargoId)) {
      for (CargoHandling ch : getCargoHandling(cargoId)) {
        minLoad = BeeUtils.min(minLoad, ch.getLoadingDate());
        maxUnload = BeeUtils.max(maxUnload, ch.getUnloadingDate());
      }
    }

    return Pair.of(minLoad, maxUnload);
  }

  protected abstract String getDataService();

  @Override
  protected Set<Action> getEnabledActions() {
    return EnumSet.of(Action.FILTER, Action.REFRESH, Action.ADD, Action.CONFIGURE, Action.PRINT);
  }

  protected List<ChartData> getFilterData() {
    return filterData;
  }

  protected abstract String getSettingsFormName();

  protected abstract String getShowCountryFlagsColumnName();

  protected abstract String getShowPlaceCitiesColumnName();

  protected abstract String getShowPlaceCodesColumnName();

  protected abstract String getShowPlaceInfoColumnName();

  protected abstract String getThemeColumnName();

  protected boolean hasCargoHandling(Long cargoId) {
    return cargoId != null && cargoHandling.containsKey(cargoId);
  }

  protected abstract void initData(Map<String, String> properties);

  @Override
  protected boolean isDataEventRelevant(DataEvent event) {
    return event != null && relevantDataViews.contains(event.getViewName());
  }

  protected boolean isItemVisible(Filterable item) {
    return item != null && (!isFiltered() || item.matched(FilterType.PERSISTENT));
  }

  protected abstract boolean persistFilter();

  @Override
  protected void prepareDefaults(Size canvasSize) {
    super.prepareDefaults(canvasSize);

    setShowCountryFlags(TimeBoardHelper.getBoolean(getSettings(), getShowCountryFlagsColumnName()));
    setShowPlaceInfo(TimeBoardHelper.getBoolean(getSettings(), getShowPlaceInfoColumnName()));

    setShowPlaceCities(TimeBoardHelper.getBoolean(getSettings(), getShowPlaceCitiesColumnName()));
    setShowPlaceCodes(TimeBoardHelper.getBoolean(getSettings(), getShowPlaceCodesColumnName()));
  }

  protected abstract List<ChartData> prepareFilterData(FilterType filterType);

  @Override
  protected void refresh() {
    BeeKeeper.getRpc().makePostRequest(TransportHandler.createArgs(getDataService()),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            if (setData(response)) {
              render(false);
            }
          }
        });
  }

  protected void renderCargoShipment(HasWidgets panel, OrderCargo cargo, String parentTitle) {
    if (panel == null || cargo == null) {
      return;
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

  protected void renderTrip(HasWidgets panel, String title,
      Collection<? extends OrderCargo> cargos, Range<JustDate> range, String styleVoid) {

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
  }

  protected abstract void resetFilter(FilterType filterType);

  @Override
  protected boolean setData(ResponseObject response) {
    if (!Queries.checkResponse(getCaption(), null, response, BeeRowSet.class)) {
      return false;
    }

    BeeRowSet rowSet = BeeRowSet.restore((String) response.getResponse());
    setSettings(rowSet);

    String serialized = rowSet.getTableProperty(PROP_COUNTRIES);
    if (!BeeUtils.isEmpty(serialized)) {
      Places.setCountries(BeeRowSet.restore(serialized));
    }

    serialized = rowSet.getTableProperty(PROP_CITIES);
    if (!BeeUtils.isEmpty(serialized)) {
      Places.setCities(BeeRowSet.restore(serialized));
    }

    serialized = rowSet.getTableProperty(PROP_COLORS);
    if (!BeeUtils.isEmpty(serialized)) {
      restoreColors(serialized);
    }

    cargoHandling.clear();
    serialized = rowSet.getTableProperty(PROP_CARGO_HANDLING);
    if (!BeeUtils.isEmpty(serialized)) {
      SimpleRowSet srs = SimpleRowSet.restore(serialized);
      for (SimpleRow row : srs) {
        cargoHandling.put(row.getLong(COL_CARGO), new CargoHandling(row));
      }
    }

    initData(rowSet.getTableProperties());
    updateMaxRange();

    updateFilterData();

    return true;
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

    if (hasCargoHandling(cargo.getCargoId())) {
      for (CargoHandling ch : getCargoHandling(cargo.getCargoId())) {
        if (ch.getLoadingDate() != null && range.contains(ch.getLoadingDate())) {
          result.put(ch.getLoadingDate(), new CargoEvent(cargo, ch, true));
        }

        if (ch.getUnloadingDate() != null && range.contains(ch.getUnloadingDate())) {
          result.put(ch.getUnloadingDate(), new CargoEvent(cargo, ch, false));
        }
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

  private Widget createShipmentDayWidget(Long countryId, Collection<CargoEvent> events,
      String parentTitle) {

    Flow widget = new Flow();
    widget.addStyleName(STYLE_SHIPMENT_DAY_WIDGET);

    String flag = showCountryFlags() ? Places.getCountryFlag(countryId) : null;

    if (!BeeUtils.isEmpty(flag)) {
      widget.addStyleName(STYLE_SHIPMENT_DAY_FLAG);
      StyleUtils.setBackgroundImage(widget, flag);
    }

    if (!BeeUtils.isEmpty(events)) {
      if (showPlaceInfo()) {
        List<String> info = new ArrayList<>();

        if (BeeUtils.isEmpty(flag) && DataUtils.isId(countryId)) {
          String countryLabel = Places.getCountryLabel(countryId);
          if (!BeeUtils.isEmpty(countryLabel)) {
            info.add(countryLabel);
          }
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
      }

      if (showPlaceCities()) {
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

            title.add(entry.getKey().getTitle(chLoading, chUnloading));
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

  private Long getColorTheme(BeeRow row) {
    if (row == null || BeeUtils.isEmpty(getThemeColumnName()) || DataUtils.isEmpty(getSettings())) {
      return null;
    } else {
      return row.getLong(getSettings().getColumnIndex(getThemeColumnName()));
    }
  }

  private void refreshFilterInfo() {
    if (isFiltered()) {
      List<String> selection = new ArrayList<>();
      for (ChartData data : filterData) {
        Collection<String> selectedNames = data.getSelectedNames();
        if (!selectedNames.isEmpty()) {
          selection.addAll(selectedNames);
        }
      }

      if (!selection.isEmpty()) {
        getFilterLabel().getElement().setInnerText(BeeUtils.join(BeeConst.STRING_COMMA, selection));
        getRemoveFilter().setVisible(true);
        return;
      }
    }

    getFilterLabel().getElement().setInnerText(BeeConst.STRING_EMPTY);
    getRemoveFilter().setVisible(false);
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

  private boolean showCountryFlags() {
    return showCountryFlags;
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

    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        restoreColors((String) response.getResponse());
        render(false);
      }
    });
  }

  private void updateFilterData() {
    List<ChartData> newData = FilterHelper.notEmptyData(prepareFilterData(null));
    if (newData != null) {
      for (ChartData cd : newData) {
        cd.prepare();
      }
    }

    boolean wasFiltered = isFiltered();

    if (BeeUtils.isEmpty(newData)) {
      filterData.clear();
      if (wasFiltered) {
        clearFilter();
      }

    } else if (filterData.isEmpty()) {
      filterData.addAll(newData);

    } else {
      if (wasFiltered) {
        for (ChartData ocd : filterData) {
          ChartData ncd = FilterHelper.getDataByType(newData, ocd.getType());

          if (ncd != null) {
            Collection<String> selectedNames = ocd.getSelectedNames();
            for (String name : selectedNames) {
              ncd.setSelected(name, true);
            }
          }
        }
      }

      filterData.clear();
      filterData.addAll(newData);

      if (wasFiltered) {
        setFiltered(filter(FilterType.TENTATIVE));

        if (isFiltered()) {
          FilterHelper.enableData(getFilterData(), prepareFilterData(FilterType.TENTATIVE), null);

          persistFilter();
          refreshFilterInfo();

        } else {
          clearFilter();
        }
      }
    }
  }
}
