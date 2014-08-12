package com.butent.bee.client.modules.ec.view;

import com.google.common.collect.Range;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.CloseEvent;
import com.butent.bee.client.event.logical.OpenEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.modules.ec.widget.IndexSelector;
import com.butent.bee.client.modules.ec.widget.ItemPanel;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcCarModel;
import com.butent.bee.shared.modules.ec.EcCarType;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SearchByCar extends EcView implements HasBeforeSelectionHandlers<EcCarType> {

  private static final class CarAttributeWidget extends CustomDiv implements EnablableWidget {

    private boolean enabled = true;

    private CarAttributeWidget(String styleName) {
      super(styleName);
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public void onBrowserEvent(Event event) {
      if (isEnabled() || !EventUtils.isClick(event)) {
        super.onBrowserEvent(event);
      }
    }

    @Override
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      setStyleName(STYLE_ATTRIBUTE_DISABLED, !enabled);
    }

    private void setActive(boolean active) {
      setStyleName(STYLE_ATTRIBUTE_ACTIVE, active);
    }

    private void setHasValue(boolean hasValue) {
      setStyleName(STYLE_ATTRIBUTE_HAS_VALUE, hasValue);
    }
  }

  private static final String STYLE_PREFIX = EcStyles.name("searchByCar-");

  private static final String STYLE_MAIN_PANEL = STYLE_PREFIX + "main-panel";
  private static final String STYLE_CAR_PANEL = STYLE_PREFIX + "car-panel";

  private static final String STYLE_ATTRIBUTE = STYLE_PREFIX + "attribute-";
  private static final String STYLE_SELECTOR = STYLE_PREFIX + "selector-";
  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog-";

  private static final String STYLE_MANUFACTURER = "manufacturer";
  private static final String STYLE_MODEL = "model";
  private static final String STYLE_YEAR = "year";
  private static final String STYLE_ENGINE = "engine";

  private static final String STYLE_ATTRIBUTE_DISABLED = STYLE_ATTRIBUTE + "disabled";
  private static final String STYLE_ATTRIBUTE_HAS_VALUE = STYLE_ATTRIBUTE + "hasValue";
  private static final String STYLE_ATTRIBUTE_ACTIVE = STYLE_ATTRIBUTE + "active";

  private static final String STYLE_TYPE_PREFIX = STYLE_PREFIX + "type-";
  private static final String STYLE_TYPE_PANEL = STYLE_TYPE_PREFIX + "panel";
  private static final String STYLE_HAS_TYPES = STYLE_TYPE_PANEL + "-notEmpty";

  private static final String STYLE_TYPE_TABLE = STYLE_TYPE_PREFIX + "table";
  private static final String STYLE_TYPE_HEADER = STYLE_TYPE_PREFIX + "header";
  private static final String STYLE_TYPE_SELECTABLE = STYLE_TYPE_PREFIX + "selectable";
  private static final String STYLE_TYPE_SELECTED = STYLE_TYPE_PREFIX + "selected";
  private static final String STYLE_TYPE_EXCLUDED = STYLE_TYPE_PREFIX + "excluded";

  private static final String STYLE_HAS_HISTORY = STYLE_PREFIX + "has-history";

  private static final String STYLE_HISTORY_PREFIX = STYLE_PREFIX + "history-";
  private static final String STYLE_HISTORY_PANEL = STYLE_HISTORY_PREFIX + "panel";
  private static final String STYLE_HISTORY_CAPTION = STYLE_HISTORY_PREFIX + "caption";
  private static final String STYLE_HISTORY_WRAPPER = STYLE_HISTORY_PREFIX + "wrapper";
  private static final String STYLE_HISTORY_ENTRY = STYLE_HISTORY_PREFIX + "entry";
  private static final String STYLE_HISTORY_MODEL = STYLE_HISTORY_PREFIX + "model";
  private static final String STYLE_HISTORY_DETAILS = STYLE_HISTORY_PREFIX + "details";
  private static final String STYLE_HISTORY_ENGINE = STYLE_HISTORY_PREFIX + "engine";
  private static final String STYLE_HISTORY_ACTIVE = STYLE_HISTORY_PREFIX + "active";

  private static final Edges selectorMargins = new Edges(0, 0, 2, 0);

  private static final List<EcCarType> history = new ArrayList<>();
  private static boolean historyInitialized;
  private static boolean historyEnabled;

  private static void openAttributeSelector(String styleSuffix, final IndexSelector selector,
      final CarAttributeWidget attributeWidget) {

    Popup popup = new Popup(OutsideClick.CLOSE, STYLE_DIALOG + styleSuffix);
    popup.setWidget(selector);

    popup.addOpenHandler(new OpenEvent.Handler() {
      @Override
      public void onOpen(OpenEvent event) {
        attributeWidget.setActive(true);
        selector.focus();
      }
    });

    popup.addCloseHandler(new CloseEvent.Handler() {
      @Override
      public void onClose(CloseEvent event) {
        attributeWidget.setActive(false);
      }
    });

    popup.setHideOnEscape(true);
    popup.showRelativeTo(attributeWidget.getElement(), selectorMargins);
  }

  private static String renderModel(EcCarModel model) {
    return BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR, model.getModelName(),
        EcUtils.formatProduced(model.getProducedFrom(), model.getProducedTo()));
  }

  private final Flow historyPanel;

  private final Flow mainPanel;

  private final CarAttributeWidget manufacturerWidget;
  private final IndexSelector manufacturerSelector;

  private final CarAttributeWidget modelWidget;

  private final IndexSelector modelSelector;
  private final CarAttributeWidget yearWidget;

  private final IndexSelector yearSelector;
  private final CarAttributeWidget engineWidget;

  private final IndexSelector engineSelector;
  private final Flow typePanel;

  private final ItemPanel itemPanel;
  private final List<String> manufacturers = new ArrayList<>();

  private String manufacturer;
  private final List<EcCarModel> models = new ArrayList<>();

  private Integer modelIndex;
  private final List<EcCarType> types = new ArrayList<>();

  private Long typeId;
  private final List<String> years = new ArrayList<>();

  private Integer year;
  private final List<String> engines = new ArrayList<>();

  private String engine;

  private final Set<Long> excludedTypes = new HashSet<>();

  public SearchByCar(Collection<Long> exclusions) {
    this();
    createUi();

    if (!BeeUtils.isEmpty(exclusions)) {
      this.excludedTypes.addAll(exclusions);
    }
  }

  SearchByCar() {
    super();

    this.historyPanel = new Flow(STYLE_HISTORY_PANEL);
    this.mainPanel = new Flow(STYLE_MAIN_PANEL);

    this.manufacturerWidget = new CarAttributeWidget(STYLE_ATTRIBUTE + STYLE_MANUFACTURER);
    this.manufacturerSelector = new IndexSelector(STYLE_SELECTOR + STYLE_MANUFACTURER);
    manufacturerSelector.enableAutocomplete(EcConstants.NAME_PREFIX + "car-manufacturer-selector");

    this.modelWidget = new CarAttributeWidget(STYLE_ATTRIBUTE + STYLE_MODEL);
    this.modelSelector = new IndexSelector(STYLE_SELECTOR + STYLE_MODEL);

    this.yearWidget = new CarAttributeWidget(STYLE_ATTRIBUTE + STYLE_YEAR);
    this.yearSelector = new IndexSelector(STYLE_SELECTOR + STYLE_YEAR);

    this.engineWidget = new CarAttributeWidget(STYLE_ATTRIBUTE + STYLE_ENGINE);
    this.engineSelector = new IndexSelector(STYLE_SELECTOR + STYLE_ENGINE);

    this.typePanel = new Flow(STYLE_TYPE_PANEL);
    this.itemPanel = new ItemPanel();

    Flow carPanel = new Flow(STYLE_CAR_PANEL);
    carPanel.add(manufacturerWidget);
    carPanel.add(modelWidget);
    carPanel.add(yearWidget);
    carPanel.add(engineWidget);

    mainPanel.add(carPanel);
    mainPanel.add(typePanel);
    mainPanel.add(itemPanel);

    add(historyPanel);
    add(mainPanel);
  }

  @Override
  public HandlerRegistration addBeforeSelectionHandler(BeforeSelectionHandler<EcCarType> handler) {
    return addHandler(handler, BeforeSelectionEvent.getType());
  }

  public Flow getMainPanel() {
    return mainPanel;
  }

  public void includeType(Long type) {
    if (DataUtils.isId(type) && excludedTypes.contains(type)) {
      excludedTypes.remove(type);

      if (!typePanel.isEmpty()) {
        Element typeRow = Selectors.getElementByDataIndex(typePanel, type);
        if (typeRow != null) {
          typeRow.removeClassName(STYLE_TYPE_EXCLUDED);
          typeRow.addClassName(STYLE_TYPE_SELECTABLE);
        }
      }
    }
  }

  @Override
  protected void createUi() {
    refreshAttributeWidgets();

    manufacturerWidget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (manufacturerWidget.isEnabled()) {
          openManufacturers();
        }
      }
    });

    modelWidget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (modelWidget.isEnabled()) {
          openModels();
        }
      }
    });

    yearWidget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (yearWidget.isEnabled()) {
          openYears();
        }
      }
    });

    engineWidget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (engineWidget.isEnabled()) {
          openEngines();
        }
      }
    });

    typePanel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onTypePanelClick(event);
      }
    });
  }

  @Override
  protected String getPrimaryStyle() {
    return "searchByCar";
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    if (historyInitialized) {
      if (history.isEmpty()) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            openManufacturers();
          }
        });

      } else {
        renderHistoryPanel();
      }

    } else {
      loadHistory();
    }
  }

  private void activateHistoryEntry(Widget entryWidget) {
    long id = DomUtils.getDataIndexLong(entryWidget.getElement());
    if (!DataUtils.isId(id)) {
      return;
    }

    if (Objects.equals(id, getTypeId())) {
      return;
    }

    EcCarType type = null;
    for (EcCarType ect : history) {
      if (ect.getTypeId() == id) {
        type = ect;
        break;
      }
    }

    if (type == null) {
      return;
    }

    UiHelper.removeChildStyleName(historyPanel, STYLE_HISTORY_ACTIVE);
    entryWidget.addStyleName(STYLE_HISTORY_ACTIVE);

    resetManufacturer();
    refreshAttributeWidgets();

    onSelectType(type, null, false);
  }

  private EcCarType findType(long id) {
    for (EcCarType type : types) {
      if (type.getTypeId() == id) {
        return type;
      }
    }
    return null;
  }

  private String getEngine() {
    return engine;
  }

  private Flow getHistoryWrapper() {
    for (Widget widget : historyPanel) {
      if (widget instanceof Flow && widget.getElement().hasClassName(STYLE_HISTORY_WRAPPER)) {
        return (Flow) widget;
      }
    }
    return null;
  }

  private String getManufacturer() {
    return manufacturer;
  }

  private EcCarModel getModel() {
    return (getModelIndex() == null) ? null : models.get(getModelIndex());
  }

  private Long getModelId() {
    return (getModelIndex() == null) ? null : models.get(getModelIndex()).getModelId();
  }

  private Integer getModelIndex() {
    return modelIndex;
  }

  private Long getTypeId() {
    return typeId;
  }

  private Integer getYear() {
    return year;
  }

  private void loadHistory() {
    ParameterList params = EcKeeper.createArgs(SVC_GET_CAR_TYPE_HISTORY);
    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        EcKeeper.dispatchMessages(response);

        if (!historyInitialized && !response.hasErrors()) {
          historyInitialized = true;

          if (!history.isEmpty()) {
            history.clear();
          }

          if (response.getSize() > 0) {
            historyEnabled = true;

            String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());
            if (arr != null) {
              for (String s : arr) {
                history.add(EcCarType.restore(s));
              }
            }

            if (!history.isEmpty()) {
              renderHistoryPanel();
            }

          } else if (response.hasResponse()
              && BeeUtils.isNegativeInt(response.getResponseAsString())) {
            historyEnabled = false;

          } else {
            historyEnabled = true;
          }
        }

        if (history.isEmpty()) {
          openManufacturers();
        }
      }
    });
  }

  private void maybeAddToHistory(long id) {
    if (!historyEnabled) {
      return;
    }

    EcCarType carType = findType(id);
    if (carType == null) {
      return;
    }

    if (history.isEmpty()) {
      history.add(carType);
      renderHistoryPanel();

      getHistoryWrapper().getWidget(0).addStyleName(STYLE_HISTORY_ACTIVE);
      return;
    }

    Flow wrapper = getHistoryWrapper();
    UiHelper.removeChildStyleName(wrapper, STYLE_HISTORY_ACTIVE);

    int index = history.indexOf(carType);
    if (index == 0) {
      wrapper.getWidget(0).addStyleName(STYLE_HISTORY_ACTIVE);
      DomUtils.scrollToTop(historyPanel);
      return;
    }

    Widget widget = renderHistoryEntry(carType);
    widget.addStyleName(STYLE_HISTORY_ACTIVE);

    if (index > 0) {
      history.remove(index);
      wrapper.remove(index);
    }

    history.add(0, carType);
    wrapper.insert(widget, 0);

    DomUtils.scrollToTop(historyPanel);
  }

  private void onSelectEngine(int index) {
    UiHelper.closeDialog(engineSelector);
    if (BeeUtils.isIndex(engines, index)) {
      setEngine(engines.get(index));
      setTypeId(null);

      resetItems();

      refreshAttributeWidgets();
      renderTypes();
    }
  }

  private void onSelectManufacturer(int index) {
    UiHelper.closeDialog(manufacturerSelector);
    if (!BeeUtils.isIndex(manufacturers, index)) {
      return;
    }

    setManufacturer(manufacturers.get(index));
    resetModel();

    refreshAttributeWidgets();
    openModels();
  }

  private void onSelectModel(int index) {
    UiHelper.closeDialog(modelSelector);
    if (!BeeUtils.isIndex(models, index)) {
      return;
    }

    setModelIndex(index);
    resetTypes();

    refreshAttributeWidgets();

    EcKeeper.getCarTypes(getModelId(), new Consumer<List<EcCarType>>() {
      @Override
      public void accept(List<EcCarType> input) {
        types.clear();
        types.addAll(input);

        refreshAttributeWidgets();
        renderTypes();
      }
    });
  }

  private void onSelectType(EcCarType type, Element sourceElement, final boolean addToHistory) {
    final long id = type.getTypeId();

    if (DataUtils.isId(id) && !excludedTypes.contains(id) && !Objects.equals(getTypeId(), id)) {
      BeforeSelectionEvent<EcCarType> bse = BeforeSelectionEvent.fire(this, type);
      if (bse != null && bse.isCanceled()) {
        excludedTypes.add(id);

        if (sourceElement != null) {
          sourceElement.removeClassName(STYLE_TYPE_SELECTABLE);
          sourceElement.addClassName(STYLE_TYPE_EXCLUDED);
        }

        if (addToHistory) {
          maybeAddToHistory(id);
        }
        return;
      }

      setTypeId(id);

      resetItems();
      renderTypes();

      String label = type.getInfo();

      ParameterList params = EcKeeper.createArgs(SVC_GET_ITEMS_BY_CAR_TYPE);
      params.addQueryItem(VAR_TYPE, id);

      EcKeeper.requestItems(SVC_GET_ITEMS_BY_CAR_TYPE, label, params, new Consumer<List<EcItem>>() {
        @Override
        public void accept(List<EcItem> items) {
          if (Objects.equals(id, getTypeId())) {
            EcKeeper.renderItems(itemPanel, items);

            if (addToHistory) {
              maybeAddToHistory(id);
            }
          }
        }
      });
    }
  }

  private void onSelectYear(int index) {
    UiHelper.closeDialog(yearSelector);
    if (BeeUtils.isIndex(years, index)) {
      setYear(BeeUtils.toIntOrNull(years.get(index)));
      setTypeId(null);

      resetEngine();
      resetItems();

      refreshAttributeWidgets();
      renderTypes();
    }
  }

  private void onTypePanelClick(ClickEvent event) {
    TableRowElement element = DomUtils.getParentRow(EventUtils.getEventTargetElement(event), true);
    long id = DomUtils.getDataIndexLong(element);

    EcCarType type = findType(id);
    if (type != null) {
      onSelectType(type, element, true);
    }
  }

  private void openEngines() {
    if (types.isEmpty()) {
      return;
    }
    engines.clear();

    for (EcCarType type : types) {
      if (getYear() != null && !type.isProduced(getYear())) {
        continue;
      }

      String eng = type.getTypeName();
      if (!BeeUtils.isEmpty(eng) && !engines.contains(eng)) {
        engines.add(eng);
      }
    }
    if (engines.isEmpty()) {
      return;
    }

    if (engines.size() > 1) {
      Collections.sort(engines);
    }

    if (!engineSelector.hasSelectionHandler()) {
      engineSelector.addSelectionHandler(new SelectionHandler<Integer>() {
        @Override
        public void onSelection(SelectionEvent<Integer> event) {
          onSelectEngine(event.getSelectedItem());
        }
      });
    }

    engineSelector.render(engines);
    openAttributeSelector(STYLE_ENGINE, engineSelector, engineWidget);
  }

  private void openManufacturers() {
    EcKeeper.getCarManufacturers(new Consumer<List<String>>() {
      @Override
      public void accept(List<String> input) {
        manufacturers.clear();
        manufacturers.addAll(input);

        if (!manufacturerSelector.hasSelectionHandler()) {
          manufacturerSelector.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
              onSelectManufacturer(event.getSelectedItem());
              manufacturerSelector.retainValue(getManufacturer());
            }
          });
        }

        manufacturerSelector.render(input);
        openAttributeSelector(STYLE_MANUFACTURER, manufacturerSelector, manufacturerWidget);
      }
    });
  }

  private void openModels() {
    if (BeeUtils.isEmpty(getManufacturer())) {
      return;
    }

    EcKeeper.getCarModels(getManufacturer(), new Consumer<List<EcCarModel>>() {
      @Override
      public void accept(List<EcCarModel> input) {
        models.clear();
        models.addAll(input);

        if (!modelSelector.hasSelectionHandler()) {
          modelSelector.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
              onSelectModel(event.getSelectedItem());
            }
          });
        }

        modelSelector.render(renderModels());
        openAttributeSelector(STYLE_MODEL, modelSelector, modelWidget);
      }
    });
  }

  private void openYears() {
    if (types.isEmpty()) {
      return;
    }
    years.clear();

    Set<Integer> produced = new HashSet<>();
    for (EcCarType type : types) {
      Range<Integer> range = EcUtils.yearsProduced(type.getProducedFrom(), type.getProducedTo());
      if (range != null) {
        for (int y = range.lowerEndpoint(); y <= range.upperEndpoint(); y++) {
          produced.add(y);
        }
      }
    }
    if (produced.isEmpty()) {
      return;
    }

    for (Integer y : produced) {
      years.add(y.toString());
    }
    if (years.size() > 1) {
      Collections.sort(years);
    }

    if (!yearSelector.hasSelectionHandler()) {
      yearSelector.addSelectionHandler(new SelectionHandler<Integer>() {
        @Override
        public void onSelection(SelectionEvent<Integer> event) {
          onSelectYear(event.getSelectedItem());
        }
      });
    }

    yearSelector.render(years);
    openAttributeSelector(STYLE_YEAR, yearSelector, yearWidget);
  }

  private void refreshAttributeWidgets() {
    boolean hasManufacturer = !BeeUtils.isEmpty(getManufacturer());

    manufacturerWidget.setHtml(hasManufacturer
        ? getManufacturer() : Localized.getConstants().ecCarManufacturer());
    manufacturerWidget.setHasValue(hasManufacturer);

    boolean modelEnabled = hasManufacturer;
    boolean hasModel = modelEnabled && getModelIndex() != null;

    modelWidget.setHtml(hasModel ? renderModel(getModel()) : Localized.getConstants().ecCarModel());
    modelWidget.setHasValue(hasModel);
    modelWidget.setEnabled(modelEnabled);

    boolean yearEnabled = hasModel && !types.isEmpty();
    boolean hasYear = yearEnabled && getYear() != null;

    yearWidget.setHtml(hasYear ? getYear().toString() : Localized.getConstants().ecCarYear());
    yearWidget.setHasValue(hasYear);
    yearWidget.setEnabled(yearEnabled);

    boolean engineEnabled = hasModel && !types.isEmpty();
    boolean hasEngine = engineEnabled && !BeeUtils.isEmpty(getEngine());

    engineWidget.setHtml(hasEngine ? getEngine() : Localized.getConstants().ecCarEngine());
    engineWidget.setHasValue(hasEngine);
    engineWidget.setEnabled(engineEnabled);
  }

  private Widget renderHistoryEntry(EcCarType carType) {
    Flow panel = new Flow(STYLE_HISTORY_ENTRY);

    CustomDiv modelInfo = new CustomDiv(STYLE_HISTORY_MODEL);
    modelInfo.setHtml(BeeUtils.joinWords(carType.getManufacturer(), carType.getModelName()));
    panel.add(modelInfo);

    CustomDiv typeDetails = new CustomDiv(STYLE_HISTORY_DETAILS);
    typeDetails.setHtml(BeeUtils.joinItems(
        EcUtils.formatProduced(carType.getProducedFrom(), carType.getProducedTo()),
        carType.getTypeName(), carType.getPower(),
        carType.getCcm(), carType.getCylinders(), carType.getMaxWeight()));
    panel.add(typeDetails);

    CustomDiv engineInfo = new CustomDiv(STYLE_HISTORY_ENGINE);
    engineInfo.setHtml(BeeUtils.joinItems(
        carType.getEngine(), carType.getFuel(), carType.getBody(), carType.getAxle()));
    panel.add(engineInfo);

    DomUtils.setDataIndex(panel.getElement(), carType.getTypeId());

    panel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (event.getSource() instanceof Widget) {
          activateHistoryEntry((Widget) event.getSource());
        }
      }
    });

    return panel;
  }

  private void renderHistoryPanel() {
    if (!historyPanel.isEmpty()) {
      historyPanel.clear();
    }

    Label caption = new Label(Localized.getConstants().ecCarTypeHistory());
    caption.addStyleName(STYLE_HISTORY_CAPTION);
    historyPanel.add(caption);

    Flow wrapper = new Flow(STYLE_HISTORY_WRAPPER);
    for (EcCarType carType : history) {
      wrapper.add(renderHistoryEntry(carType));
    }

    historyPanel.add(wrapper);

    addStyleName(STYLE_HAS_HISTORY);
  }

  private List<String> renderModels() {
    List<String> items = new ArrayList<>();
    for (EcCarModel model : models) {
      items.add(renderModel(model));
    }
    return items;
  }

  private void renderTypes() {
    typePanel.clear();
    if (types.isEmpty()) {
      return;
    }

    HtmlTable table = new HtmlTable(STYLE_TYPE_TABLE);

    int row = 0;
    int col = 0;

    table.setHtml(row, col++, Localized.getConstants().ecCarProduced());
    table.setHtml(row, col++, Localized.getConstants().ecCarEngine());

    table.setHtml(row, col++, Localized.getConstants().ecCarPower());

    table.setHtml(row, col++, COL_TCD_CCM);
    table.setHtml(row, col++, COL_TCD_CYLINDERS);
    table.setHtml(row, col++, COL_TCD_MAX_WEIGHT);

    table.setHtml(row, col++, COL_TCD_ENGINE);
    table.setHtml(row, col++, COL_TCD_FUEL);
    table.setHtml(row, col++, COL_TCD_BODY);
    table.setHtml(row, col++, COL_TCD_AXLE);

    table.getRowFormatter().addStyleName(row, STYLE_TYPE_HEADER);
    row++;

    for (EcCarType type : types) {
      if (getTypeId() != null) {
        if (!getTypeId().equals(type.getTypeId())) {
          continue;
        }

      } else {
        if (getYear() != null && !type.isProduced(getYear())) {
          continue;
        }
        if (!BeeUtils.isEmpty(getEngine()) && !getEngine().equals(type.getTypeName())) {
          continue;
        }
      }

      col = 0;

      table.setHtml(row, col++,
          EcUtils.formatProduced(type.getProducedFrom(), type.getProducedTo()));
      table.setHtml(row, col++, type.getTypeName());
      table.setHtml(row, col++, type.getPower());

      table.setHtml(row, col++, EcUtils.format(type.getCcm()));
      table.setHtml(row, col++, EcUtils.format(type.getCylinders()));
      table.setHtml(row, col++, EcUtils.format(type.getMaxWeight()));

      table.setHtml(row, col++, type.getEngine());
      table.setHtml(row, col++, type.getFuel());
      table.setHtml(row, col++, type.getBody());
      table.setHtml(row, col++, type.getAxle());

      DomUtils.setDataIndex(table.getRowFormatter().getElement(row), type.getTypeId());

      String rowStyle;
      if (excludedTypes.contains(type.getTypeId())) {
        rowStyle = STYLE_TYPE_EXCLUDED;
      } else if (getTypeId() == null) {
        rowStyle = STYLE_TYPE_SELECTABLE;
      } else {
        rowStyle = STYLE_TYPE_SELECTED;
      }

      table.getRowFormatter().addStyleName(row, rowStyle);

      row++;
    }

    typePanel.add(table);
    typePanel.addStyleName(STYLE_HAS_TYPES);
  }

  private void resetEngine() {
    setEngine(null);
  }

  private void resetItems() {
    if (!itemPanel.isEmpty()) {
      itemPanel.clear();
    }
  }

  private void resetManufacturer() {
    setManufacturer(null);
    resetModel();
  }

  private void resetModel() {
    models.clear();
    setModelIndex(null);

    resetTypes();
  }

  private void resetTypes() {
    types.clear();

    typePanel.clear();
    typePanel.removeStyleName(STYLE_HAS_TYPES);

    setTypeId(null);

    resetItems();

    resetYear();
  }

  private void resetYear() {
    years.clear();
    setYear(null);

    resetEngine();
  }

  private void setEngine(String engine) {
    engines.clear();
    this.engine = engine;
  }

  private void setManufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
  }

  private void setModelIndex(Integer modelIndex) {
    this.modelIndex = modelIndex;
  }

  private void setTypeId(Long typeId) {
    this.typeId = typeId;
  }

  private void setYear(Integer year) {
    this.year = year;
  }
}
