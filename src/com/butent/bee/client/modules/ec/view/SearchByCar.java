package com.butent.bee.client.modules.ec.view;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HasEnabled;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.CloseEvent;
import com.butent.bee.client.event.logical.OpenEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.modules.ec.widget.IndexSelector;
import com.butent.bee.client.modules.ec.widget.ItemPanel;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcCarModel;
import com.butent.bee.shared.modules.ec.EcCarType;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class SearchByCar extends EcView {

  private static final class CarAttributeWidget extends CustomDiv implements HasEnabled {

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

  private static final String STYLE_ATTRIBUTE = STYLE_PREFIX + "attribute-";
  private static final String STYLE_SELECTOR = STYLE_PREFIX + "selector-";
  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog-";
  private static final String STYLE_TYPE = STYLE_PREFIX + "type-";

  private static final String STYLE_MANUFACTURER = "manufacturer";
  private static final String STYLE_MODEL = "model";
  private static final String STYLE_YEAR = "year";
  private static final String STYLE_ENGINE = "engine";

  private static final String STYLE_ATTRIBUTE_DISABLED = STYLE_ATTRIBUTE + "disabled";
  private static final String STYLE_ATTRIBUTE_HAS_VALUE = STYLE_ATTRIBUTE + "hasValue";
  private static final String STYLE_ATTRIBUTE_ACTIVE = STYLE_ATTRIBUTE + "active";

  private static final String STYLE_TYPE_PANEL = STYLE_TYPE + "panel";
  private static final String STYLE_HAS_TYPES = STYLE_TYPE_PANEL + "-notEmpty";

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

  private final List<String> manufacturers = Lists.newArrayList();
  private String manufacturer;

  private final List<EcCarModel> models = Lists.newArrayList();
  private Integer modelIndex;

  private final List<EcCarType> types = Lists.newArrayList();
  private Long typeId;

  private final List<String> years = Lists.newArrayList();
  private Integer year;

  private final List<String> engines = Lists.newArrayList();
  private String engine;

  SearchByCar() {
    super();

    this.manufacturerWidget = new CarAttributeWidget(STYLE_ATTRIBUTE + STYLE_MANUFACTURER);
    this.manufacturerSelector = new IndexSelector(STYLE_SELECTOR + STYLE_MANUFACTURER);

    this.modelWidget = new CarAttributeWidget(STYLE_ATTRIBUTE + STYLE_MODEL);
    this.modelSelector = new IndexSelector(STYLE_SELECTOR + STYLE_MODEL);

    this.yearWidget = new CarAttributeWidget(STYLE_ATTRIBUTE + STYLE_YEAR);
    this.yearSelector = new IndexSelector(STYLE_SELECTOR + STYLE_YEAR);

    this.engineWidget = new CarAttributeWidget(STYLE_ATTRIBUTE + STYLE_ENGINE);
    this.engineSelector = new IndexSelector(STYLE_SELECTOR + STYLE_ENGINE);

    this.typePanel = new Flow(STYLE_TYPE_PANEL);
    this.itemPanel = new ItemPanel();
  }

  @Override
  protected void createUi() {
    Flow carPanel = new Flow(STYLE_PREFIX + "carPanel");

    carPanel.add(manufacturerWidget);
    carPanel.add(modelWidget);
    carPanel.add(yearWidget);
    carPanel.add(engineWidget);

    add(carPanel);
    add(typePanel);
    add(itemPanel);

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

    Binder.addClickHandler(typePanel, new ClickHandler() {
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

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        openManufacturers();
      }
    });
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

    if (id > 0 && !Objects.equal(getTypeId(), id)) {
      setTypeId(id);

      resetItems();
      renderTypes();

      EcCarType type = findType(id);
      String label = (type == null) ? BeeUtils.toString(id) : type.getInfo();

      ParameterList params = EcKeeper.createArgs(SVC_GET_ITEMS_BY_CAR_TYPE);
      params.addQueryItem(VAR_TYPE, id);

      EcKeeper.requestItems(SVC_GET_ITEMS_BY_CAR_TYPE, label, params, new Consumer<List<EcItem>>() {
        @Override
        public void accept(List<EcItem> items) {
          EcKeeper.renderItems(itemPanel, items);
        }
      });
    }
  }

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
    popup.showRelativeTo(attributeWidget.getElement());
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

    Set<Integer> produced = Sets.newHashSet();
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

  private static String renderModel(EcCarModel model) {
    return BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR, model.getModelName(),
        EcUtils.renderProduced(model.getProducedFrom(), model.getProducedTo()));
  }

  private List<String> renderModels() {
    List<String> items = Lists.newArrayList();
    for (EcCarModel model : models) {
      items.add(renderModel(model));
    }
    return items;
  }

  private void renderTypes() {
    HtmlTable table = new HtmlTable(STYLE_TYPE + "table");

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

    table.getRowFormatter().addStyleName(row, STYLE_TYPE + "headerRow");
    row++;

    String rowStyle = STYLE_TYPE + ((getTypeId() == null) ? "selectableRow" : "selectedRow");

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
          EcUtils.renderProduced(type.getProducedFrom(), type.getProducedTo()));
      table.setHtml(row, col++, type.getTypeName());
      table.setHtml(row, col++, type.getPower());

      table.setHtml(row, col++, EcUtils.string(type.getCcm()));
      table.setHtml(row, col++, EcUtils.string(type.getCylinders()));
      table.setHtml(row, col++, EcUtils.string(type.getMaxWeight()));

      table.setHtml(row, col++, type.getEngine());
      table.setHtml(row, col++, type.getFuel());
      table.setHtml(row, col++, type.getBody());
      table.setHtml(row, col++, type.getAxle());

      DomUtils.setDataIndex(table.getRowFormatter().getElement(row), type.getTypeId());

      table.getRowFormatter().addStyleName(row, rowStyle);
      row++;
    }

    typePanel.clear();
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
