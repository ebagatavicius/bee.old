package com.butent.bee.client.view.grid;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.client.view.search.FilterConsumer;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterComponent;
import com.butent.bee.shared.data.filter.FilterDescription;
import com.butent.bee.shared.data.filter.FilterValue;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GridFilterManager {

  private static final BeeLogger logger = LogUtils.getLogger(GridFilterManager.class);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "GridFilter-";
  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog";
  private static final String STYLE_CHILD = STYLE_PREFIX + "child";

  private static final String STYLE_CONTENT = STYLE_PREFIX + "content";

  private static final String STYLE_SUPPLIER_PREFIX = STYLE_PREFIX + "supplier-";
  private static final String STYLE_SUPPLIER_PANEL = STYLE_SUPPLIER_PREFIX + "panel";
  private static final String STYLE_SUPPLIER_TABLE = STYLE_SUPPLIER_PREFIX + "table";
  private static final String STYLE_SUPPLIER_ROW = STYLE_SUPPLIER_PREFIX + "row";
  private static final String STYLE_SUPPLIER_LABEL = STYLE_SUPPLIER_PREFIX + "label";
  private static final String STYLE_SUPPLIER_ACTION_CONTAINER = STYLE_SUPPLIER_PREFIX
      + "action-container";
  private static final String STYLE_SUPPLIER_EMPTY = STYLE_SUPPLIER_PREFIX + "empty";
  private static final String STYLE_SUPPLIER_NOT_EMPTY = STYLE_SUPPLIER_PREFIX + "not-empty";
  private static final String STYLE_SUPPLIER_BUTTON = STYLE_SUPPLIER_PREFIX + "button";
  private static final String STYLE_SUPPLIER_CLEAR = STYLE_SUPPLIER_PREFIX + "clear";

  private static final String STYLE_SAVE_PREFIX = STYLE_PREFIX + "save-";
  private static final String STYLE_SAVE_PANEL = STYLE_SAVE_PREFIX + "panel";
  private static final String STYLE_SAVE_ICON = STYLE_SAVE_PREFIX + "icon";
  private static final String STYLE_SAVE_MESSAGE = STYLE_SAVE_PREFIX + "message";

  private static final String STYLE_FILTER_PANEL = STYLE_PREFIX + "saved-filters";

  public static Filter parseFilter(CellGrid grid, List<FilterComponent> components) {
    if (BeeUtils.isEmpty(components)) {
      return null;
    }

    List<Filter> filters = new ArrayList<>();

    for (FilterComponent component : components) {
      String columnId = component.getName();

      ColumnInfo columnInfo = GridUtils.getColumnInfo(grid.getPredefinedColumns(), columnId);
      if (columnInfo == null) {
        logger.warning("filter column not found:", component);
        continue;
      }

      AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();
      if (filterSupplier == null) {
        logger.warning("filter supplier not found:", component);
        continue;
      }

      Filter columnFilter = filterSupplier.parse(component.getFilterValue());
      if (columnFilter == null) {
        logger.warning(columnId, "cannot parse filter:", component);
      } else if (!filters.contains(columnFilter)) {
        filters.add(columnFilter);
      }
    }

    return Filter.and(filters);
  }

  private static Collection<FilterComponent> asComponents(Map<String, FilterValue> values) {
    List<FilterComponent> components = new ArrayList<>();

    if (!BeeUtils.isEmpty(values)) {
      for (Entry<String, FilterValue> entry : values.entrySet()) {
        components.add(new FilterComponent(entry.getKey(), entry.getValue()));
      }
    }

    return components;
  }

  private static Map<String, FilterValue> asValues(Collection<FilterComponent> components) {
    Map<String, FilterValue> values = new HashMap<>();

    if (!BeeUtils.isEmpty(components)) {
      for (FilterComponent component : components) {
        values.put(component.getName(), component.getFilterValue());
      }
    }

    return values;
  }

  private final String gridKey;
  private final CellGrid grid;
  private final boolean isChild;

  private final FilterConsumer filterConsumer;

  private final Map<String, FilterValue> valuesByColumn = new HashMap<>();

  private final Flow contentPanel = new Flow(STYLE_CONTENT);

  private Filter externalFilter;

  public GridFilterManager(GridView gridView, FilterConsumer filterConsumer) {
    super();

    this.gridKey = gridView.getGridKey();
    this.grid = gridView.getGrid();
    this.isChild = gridView.hasChildUi();

    this.filterConsumer = filterConsumer;
  }

  public void clearFilter() {
    List<ColumnInfo> columns = grid.getPredefinedColumns();
    for (ColumnInfo columnInfo : columns) {
      AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();
      if (filterSupplier != null && !filterSupplier.isEmpty()) {
        filterSupplier.setFilterValue(null);
      }
    }
  }

  public String getFilterLabel(boolean refresh) {
    if (refresh) {
      retainValues();
    }
    if (valuesByColumn.isEmpty()) {
      return null;
    }

    List<String> labels = new ArrayList<>();

    List<ColumnInfo> columns = grid.getPredefinedColumns();
    for (ColumnInfo columnInfo : columns) {
      if (columnInfo.getFilterSupplier() != null
          && valuesByColumn.containsKey(columnInfo.getColumnId())) {
        String label = columnInfo.getFilterSupplier().getComponentLabel(columnInfo.getLabel());
        if (!BeeUtils.isEmpty(label)) {
          labels.add(label);
        }
      }
    }

    if (labels.isEmpty()) {
      return null;
    } else {
      return BeeUtils.join(BeeConst.DEFAULT_LIST_SEPARATOR, labels);
    }
  }

  public void handleFilter(Filter queryFilter, Element target) {
    externalFilter = queryFilter;
    retainValues();

    DialogBox dialog = DialogBox.create(Localized.dictionary().filter(), STYLE_DIALOG);
    if (isChild) {
      dialog.addStyleName(STYLE_CHILD);
    }

    buildContentPanel();
    dialog.setWidget(contentPanel);

    dialog.setHideOnEscape(true);

    dialog.setAnimationEnabled(true);
    dialog.showRelativeTo(target);
  }

  public void setFilter(List<FilterComponent> components) {
    if (BeeUtils.isEmpty(components)) {
      clearFilter();
    } else {
      updateFilterValues(asValues(components), true);
    }
  }

  private void buildContentPanel() {
    if (!contentPanel.isEmpty()) {
      contentPanel.clear();
    }

    List<ColumnInfo> predefinedColumns = grid.getPredefinedColumns();
    List<Integer> visibleColumns = grid.getVisibleColumns();

    HtmlTable table = new HtmlTable(STYLE_SUPPLIER_TABLE);
    int row = 0;

    for (int index : visibleColumns) {
      ColumnInfo columnInfo = predefinedColumns.get(index);
      AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();
      if (filterSupplier != null) {
        createSupplierRow(table, row, columnInfo, filterSupplier);
        row++;
      }
    }

    if (predefinedColumns.size() > visibleColumns.size()) {
      for (int i = 0; i < predefinedColumns.size(); i++) {
        if (!visibleColumns.contains(i)) {
          ColumnInfo columnInfo = predefinedColumns.get(i);
          AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();
          if (filterSupplier != null && !filterSupplier.isEmpty()) {
            createSupplierRow(table, row, columnInfo, filterSupplier);
            row++;
          }
        }
      }
    }

    Simple supplierPanel = new Simple(table);
    supplierPanel.addStyleName(STYLE_SUPPLIER_PANEL);
    contentPanel.add(supplierPanel);

    if (!valuesByColumn.isEmpty()
        && !Global.getFilters().contains(gridKey, asComponents(valuesByColumn))) {
      Widget saveWidget = createSaveWidget();
      contentPanel.add(saveWidget);
    }

    if (Global.getFilters().containsKey(gridKey)) {
      BiConsumer<FilterDescription, Action> callback = new BiConsumer<FilterDescription, Action>() {
        @Override
        public void accept(FilterDescription t, Action u) {
          if (t != null) {
            updateFilterValues(asValues(t.getComponents()), true);
            onChange(null, true, null);
          } else if (Action.DELETE == u) {
            buildContentPanel();
          }
        }
      };

      Widget widget = Global.getFilters().createWidget(gridKey, asComponents(valuesByColumn),
          callback);
      widget.addStyleName(STYLE_FILTER_PANEL);
      contentPanel.add(widget);
    }
  }

  private Widget createSaveWidget() {
    Flow panel = new Flow();
    panel.addStyleName(STYLE_SAVE_PANEL);

    ClickHandler clickHandler = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String label = getFilterLabel(false);

        if (BeeUtils.isEmpty(label)) {
          logger.severe("filter has no label:", valuesByColumn);

        } else {
          Scheduler.ScheduledCommand onSave = new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
              buildContentPanel();

              Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                  for (Widget widget : contentPanel) {
                    if (StyleUtils.hasClassName(widget.getElement(), STYLE_FILTER_PANEL)) {
                      NodeList<Element> nodes =
                          widget.getElement().getElementsByTagName(Tags.TR);
                      if (nodes != null && nodes.getLength() > 1) {
                        DomUtils.scrollIntoView(nodes.getItem(nodes.getLength() - 1));
                      }
                      break;
                    }
                  }
                }
              });
            }
          };

          Global.getFilters().addCustomFilter(gridKey, label, asComponents(valuesByColumn), onSave);
        }
      }
    };

    FaLabel icon = new FaLabel(FontAwesome.PLUS);
    icon.addStyleName(STYLE_SAVE_ICON);
    icon.addClickHandler(clickHandler);
    panel.add(icon);

    CustomDiv message = new CustomDiv(STYLE_SAVE_MESSAGE);
    message.setHtml(Localized.dictionary().saveFilter());
    message.addClickHandler(clickHandler);
    panel.add(message);

    return panel;
  }

  private void createSupplierRow(HtmlTable table, int row, final ColumnInfo columnInfo,
      final AbstractFilterSupplier filterSupplier) {

    CustomDiv label = new CustomDiv();
    label.setHtml(columnInfo.getLabel());

    table.setWidgetAndStyle(row, 0, label, STYLE_SUPPLIER_LABEL);

    final Button button = new Button();
    button.addStyleName(STYLE_SUPPLIER_BUTTON);

    if (!filterSupplier.isEmpty()) {
      button.getElement().setInnerText(filterSupplier.getLabel());
      button.setTitle(filterSupplier.getTitle());
    }

    ClickHandler clickHandler = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        filterSupplier.setEffectiveFilter(getFilter(externalFilter, columnInfo.getColumnId()));
        filterSupplier.onRequest(button.getElement(), new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            onChange(columnInfo, true, new Scheduler.ScheduledCommand() {
              @Override
              public void execute() {
                filterSupplier.retainInput();
              }
            });
          }
        });
      }
    };

    label.addClickHandler(clickHandler);
    button.addClickHandler(clickHandler);

    CustomDiv clear = new CustomDiv(STYLE_SUPPLIER_CLEAR);
    clear.setText(String.valueOf(BeeConst.CHAR_TIMES));
    clear.setTitle(Action.REMOVE_FILTER.getCaption());

    clear.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        filterSupplier.setFilterValue(null);
        onChange(columnInfo, true, null);
      }
    });

    Flow actionContainer = new Flow();
    actionContainer.add(button);
    actionContainer.add(clear);

    table.setWidgetAndStyle(row, 1, actionContainer, STYLE_SUPPLIER_ACTION_CONTAINER);

    actionContainer.addStyleName(filterSupplier.isEmpty()
        ? STYLE_SUPPLIER_EMPTY : STYLE_SUPPLIER_NOT_EMPTY);
    table.getRowFormatter().addStyleName(row, STYLE_SUPPLIER_ROW);
  }

  private Filter getFilter(Filter queryFilter, String excludeColumn) {
    List<Filter> filters = new ArrayList<>();
    if (queryFilter != null) {
      filters.add(queryFilter);
    }

    List<ColumnInfo> columns = grid.getPredefinedColumns();
    for (ColumnInfo columnInfo : columns) {
      AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();

      if (filterSupplier != null) {
        if (!BeeUtils.isEmpty(excludeColumn) && columnInfo.is(excludeColumn)) {
          continue;
        }

        Filter columnFilter = filterSupplier.getFilter();
        if (columnFilter != null) {
          filters.add(columnFilter);
        }
      }
    }
    return Filter.and(filters);
  }

  private Map<String, FilterValue> getFilterValues() {
    Map<String, FilterValue> values = new HashMap<>();

    List<ColumnInfo> columns = grid.getPredefinedColumns();
    for (ColumnInfo columnInfo : columns) {
      AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();

      if (filterSupplier != null) {
        FilterValue filterValue = filterSupplier.getFilterValue();
        if (filterValue != null) {
          values.put(columnInfo.getColumnId(), filterValue);
        }
      }
    }
    return values;
  }

  private void onChange(final ColumnInfo columnInfo, final boolean closeDialog,
      final Scheduler.ScheduledCommand onSuccess) {

    final Filter filter = getFilter(null, null);

    filterConsumer.tryFilter(filter, new Consumer<Boolean>() {
      @Override
      public void accept(Boolean input) {
        if (BeeUtils.isTrue(input)) {
          if (onSuccess != null) {
            onSuccess.execute();
          }

          if (closeDialog) {
            UiHelper.closeDialog(contentPanel);
          } else {
            retainValues();
            buildContentPanel();
          }

        } else {
          BeeKeeper.getScreen().notifyWarning(Localized.dictionary().nothingFound());
          if (columnInfo == null) {
            updateFilterValues(valuesByColumn, false);
          } else {
            FilterValue filterValue = valuesByColumn.get(columnInfo.getColumnId());
            columnInfo.getFilterSupplier().setFilterValue(filterValue);
          }
        }
      }
    }, false);
  }

  private void retainValues() {
    valuesByColumn.clear();

    Map<String, FilterValue> values = getFilterValues();
    if (!values.isEmpty()) {
      valuesByColumn.putAll(values);
    }
  }

  private void updateFilterValues(Map<String, FilterValue> values, boolean ensureData) {
    if (BeeUtils.isEmpty(values)) {
      clearFilter();

    } else {
      List<ColumnInfo> columns = grid.getPredefinedColumns();
      for (ColumnInfo columnInfo : columns) {
        AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();

        if (filterSupplier != null) {
          String columnId = columnInfo.getColumnId();

          if (values.containsKey(columnId)) {
            filterSupplier.setFilterValue(values.get(columnId));
            if (ensureData) {
              filterSupplier.ensureData();
            }

          } else if (!filterSupplier.isEmpty()) {
            filterSupplier.setFilterValue(null);
          }
        }
      }
    }
  }
}
