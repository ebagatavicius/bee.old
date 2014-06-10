package com.butent.bee.client.grid;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.grid.cell.HtmlCell;
import com.butent.bee.client.grid.cell.TextCell;
import com.butent.bee.client.grid.column.AreaColumn;
import com.butent.bee.client.grid.column.BooleanColumn;
import com.butent.bee.client.grid.column.CurrencyColumn;
import com.butent.bee.client.grid.column.DataColumn;
import com.butent.bee.client.grid.column.DateColumn;
import com.butent.bee.client.grid.column.DateTimeColumn;
import com.butent.bee.client.grid.column.DecimalColumn;
import com.butent.bee.client.grid.column.DoubleColumn;
import com.butent.bee.client.grid.column.IntegerColumn;
import com.butent.bee.client.grid.column.LongColumn;
import com.butent.bee.client.grid.column.StringColumn;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.RenderableCell;
import com.butent.bee.client.render.RenderableColumn;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.ui.WidgetFactory;
import com.butent.bee.client.ui.WidgetSupplier;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.ColumnInfo;
import com.butent.bee.client.view.grid.GridFilterManager;
import com.butent.bee.client.view.grid.GridImpl;
import com.butent.bee.client.view.grid.GridSettings;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.ExtendedPropertiesData;
import com.butent.bee.shared.data.IsTable;
import com.butent.bee.shared.data.PropertiesData;
import com.butent.bee.shared.data.ProviderType;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterComponent;
import com.butent.bee.shared.data.filter.FilterDescription;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.CellType;
import com.butent.bee.shared.ui.Flexibility;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GridFactory {

  public static final class GridOptions implements HasCaption {

    public static GridOptions forCaptionAndFilter(String cap, Filter flt) {
      return (BeeUtils.isEmpty(cap) && flt == null) ? null : new GridOptions(cap, null, flt, null);
    }

    public static GridOptions forCurrentUserFilter(String column) {
      return BeeUtils.isEmpty(column) ? null : new GridOptions(null, null, null, column);
    }

    public static GridOptions forFilter(Filter flt) {
      return (flt == null) ? null : new GridOptions(null, null, flt, null);
    }

    private final String caption;

    private final String filterDescription;
    private final Filter filter;

    private final String currentUserFilter;

    private GridOptions(String caption, String filterDescription, Filter filter,
        String currentUserFilter) {
      this.caption = caption;
      this.filterDescription = filterDescription;
      this.filter = filter;
      this.currentUserFilter = currentUserFilter;
    }

    @Override
    public String getCaption() {
      return caption;
    }

    private Filter buildFilter(String viewName) {
      Filter f1;
      if (BeeUtils.isEmpty(filterDescription)) {
        f1 = null;
      } else {
        DataInfo dataInfo = Data.getDataInfo(viewName);
        f1 = (dataInfo == null) ? null : dataInfo.parseFilter(filterDescription,
            BeeKeeper.getUser().getUserId());
      }

      Filter f2 = BeeUtils.isEmpty(currentUserFilter) ? null
          : BeeKeeper.getUser().getFilter(currentUserFilter);

      return Filter.and(filter, f1, f2);
    }

    private boolean hasFilter() {
      return filter != null || !BeeUtils.isEmpty(filterDescription)
          || !BeeUtils.isEmpty(currentUserFilter);
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(GridFactory.class);

  private static final Map<String, GridDescription> descriptionCache = new HashMap<>();
  private static final Map<String, GridInterceptor> gridInterceptors = new HashMap<>();

  private static final Multimap<String, String> hiddenColumns = HashMultimap.create();

  public static void clearDescriptionCache() {
    descriptionCache.clear();
  }

  public static AbstractCell<String> createCell(CellType cellType) {
    Assert.notNull(cellType);

    switch (cellType) {
      case HTML:
        return new HtmlCell();
      default:
        return new TextCell();
    }
  }

  public static DataColumn<?> createColumn(CellSource cellSource) {
    return createColumn(cellSource, null);
  }

  public static DataColumn<?> createColumn(CellSource cellSource, CellType cellType) {
    if (cellType != null) {
      return new StringColumn(createCell(cellType), cellSource);
    }

    ValueType type = cellSource.getValueType();
    if (type == null) {
      return new StringColumn(cellSource);
    }

    switch (type) {
      case BOOLEAN:
        return new BooleanColumn(cellSource);

      case DATE:
        return new DateColumn(cellSource);

      case DATE_TIME:
        return new DateTimeColumn(cellSource);

      case DECIMAL:
        if (cellSource.getScale() == 2) {
          return new CurrencyColumn(cellSource);
        } else {
          return new DecimalColumn(cellSource);
        }

      case INTEGER:
        return new IntegerColumn(cellSource);

      case LONG:
        return new LongColumn(cellSource);

      case NUMBER:
        return new DoubleColumn(cellSource);

      case TEXT:
      case BLOB:
        if (cellSource.isText()) {
          return new AreaColumn(cellSource);
        } else {
          return new StringColumn(cellSource);
        }

      case TIME_OF_DAY:
        return new StringColumn(cellSource);
    }

    Assert.untouchable();
    return null;
  }

  public static DataColumn<?> createColumn(CellSource cellSource, CellType cellType,
      AbstractCellRenderer renderer) {
    if (renderer == null) {
      return createColumn(cellSource, cellType);
    } else {
      return createRenderableColumn(renderer, cellSource, cellType);
    }
  }

  public static void createGrid(String gridName, final String supplierKey,
      final GridInterceptor gridInterceptor, final Collection<UiOption> uiOptions,
      final GridOptions gridOptions, final PresenterCallback presenterCallback) {

    Assert.notEmpty(gridName);
    Assert.notNull(presenterCallback);

    getGridDescription(gridName, new Callback<GridDescription>() {
      @Override
      public void onSuccess(GridDescription result) {
        Assert.notNull(result);
        if (gridInterceptor != null && !gridInterceptor.initDescription(result)) {
          return;
        }

        consumeGridDescription(GridSettings.apply(supplierKey, result), supplierKey,
            gridInterceptor, presenterCallback, uiOptions, gridOptions);
      }
    });
  }

  public static GridView createGridView(GridDescription gridDescription, String supplierKey,
      List<BeeColumn> dataColumns) {
    return createGridView(gridDescription, supplierKey, dataColumns, null,
        getGridInterceptor(gridDescription.getName()), null);
  }

  public static GridView createGridView(GridDescription gridDescription, String supplierKey,
      List<BeeColumn> dataColumns, String relColumn, GridInterceptor gridInterceptor, Order order) {

    GridView gridView = new GridImpl(gridDescription, supplierKey, dataColumns, relColumn,
        gridInterceptor);
    gridView.create(order);

    return gridView;
  }

  public static DataColumn<?> createRenderableColumn(AbstractCellRenderer renderer,
      CellSource cellSource, CellType cellType) {
    AbstractCell<String> cell = (cellType == null) ? new RenderableCell() : createCell(cellType);
    return new RenderableColumn(cell, cellSource, renderer);
  }

  public static void getGridDescription(String name, Callback<GridDescription> callback) {
    getGridDescription(name, callback, false);
  }

  public static void getGridDescription(final String name,
      final Callback<GridDescription> callback, boolean reload) {

    Assert.notEmpty(name);
    Assert.notNull(callback);

    if (!reload && isGridDescriptionCached(name)) {
      callback.onSuccess(descriptionCache.get(gridDescriptionKey(name)));
      return;
    }

    BeeKeeper.getRpc().sendText(Service.GET_GRID, name, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        Assert.notNull(response);

        if (response.hasResponse(GridDescription.class)) {
          GridDescription gridDescription =
              GridDescription.restore((String) response.getResponse());
          callback.onSuccess(gridDescription);
          if (!BeeUtils.isFalse(gridDescription.getCacheDescription())) {
            descriptionCache.put(gridDescriptionKey(name), gridDescription);
          }
        } else {
          callback.onFailure(response.getErrors());
          descriptionCache.put(gridDescriptionKey(name), null);
        }
      }
    });
  }

  public static GridInterceptor getGridInterceptor(String gridName) {
    Assert.notEmpty(gridName);
    GridInterceptor interceptor = gridInterceptors.get(BeeUtils.normalize(gridName));
    return getInterceptorInstance(interceptor);
  }

  public static GridOptions getGridOptions(Map<String, String> attributes) {
    if (BeeUtils.isEmpty(attributes)) {
      return null;
    }

    String caption = attributes.get(UiConstants.ATTR_CAPTION);
    String filterDescription = attributes.get(UiConstants.ATTR_FILTER);
    String currentUserFilter = attributes.get(UiConstants.ATTR_CURRENT_USER_FILTER);

    if (BeeUtils.allEmpty(caption, filterDescription, currentUserFilter)) {
      return null;
    } else {
      return new GridOptions(Localized.maybeTranslate(caption), filterDescription, null,
          currentUserFilter);
    }
  }

  public static Filter getImmutableFilter(GridDescription gridDescription,
      GridOptions gridOptions) {
    Assert.notNull(gridDescription);

    Filter f1 = gridDescription.getFilter();
    Filter f2 = BeeUtils.isEmpty(gridDescription.getCurrentUserFilter()) ? null
        : BeeKeeper.getUser().getFilter(gridDescription.getCurrentUserFilter());

    if (gridOptions == null || !gridOptions.hasFilter()) {
      return Filter.and(f1, f2);
    } else {
      return Filter.and(f1, f2, gridOptions.buildFilter(gridDescription.getViewName()));
    }
  }

  public static Filter getInitialQueryFilter(Filter immutableFilter,
      Map<String, Filter> initialParentFilters, Filter initialUserFilter) {

    List<Filter> filters = new ArrayList<>();
    if (immutableFilter != null) {
      filters.add(immutableFilter);
    }

    if (initialParentFilters != null) {
      for (Filter filter : initialParentFilters.values()) {
        if (filter != null) {
          filters.add(filter);
        }
      }
    }

    if (initialUserFilter != null) {
      filters.add(initialUserFilter);
    }

    return Filter.and(filters);
  }

  public static List<FilterDescription> getPredefinedFilters(GridDescription gridDescription,
      GridInterceptor gridInterceptor) {
    Assert.notNull(gridDescription);

    if (gridInterceptor == null) {
      return gridDescription.getPredefinedFilters();
    } else {
      return gridInterceptor.getPredefinedFilters(gridDescription.getPredefinedFilters());
    }
  }

  public static String getSupplierKey(String gridName) {
    Assert.notEmpty(gridName);
    return WidgetFactory.SupplierKind.GRID.getKey(gridName);
  }

  public static void hideColumn(String gridName, String columnName) {
    Assert.notEmpty(gridName);
    Assert.notEmpty(columnName);

    hiddenColumns.put(gridName, columnName);
  }

  public static boolean isHidden(String gridName, String columnName) {
    return hiddenColumns.containsEntry(gridName, columnName);
  }

  public static void openGrid(String gridName) {
    openGrid(gridName, getGridInterceptor(gridName));
  }

  public static void openGrid(String gridName, GridInterceptor gridInterceptor) {
    openGrid(gridName, gridInterceptor, null);
  }

  public static void openGrid(String gridName, GridInterceptor gridInterceptor,
      GridOptions gridOptions) {
    openGrid(gridName, gridInterceptor, gridOptions, PresenterCallback.SHOW_IN_ACTIVE_PANEL);
  }

  public static void openGrid(String gridName, GridInterceptor gridInterceptor,
      GridOptions gridOptions, PresenterCallback presenterCallback) {

    String supplierKey = getSupplierKey(gridName);
    Collection<UiOption> uiOptions = EnumSet.of(UiOption.ROOT);

    if (!WidgetFactory.hasSupplier(supplierKey)) {
      registerGridSupplier(supplierKey, gridName, gridInterceptor, uiOptions, gridOptions);
    }

    createGrid(gridName, supplierKey, gridInterceptor, uiOptions, gridOptions, presenterCallback);
  }

  public static void openGrid(String gridName, GridOptions gridOptions) {
    openGrid(gridName, getGridInterceptor(gridName), gridOptions);
  }

  public static void registerGridInterceptor(String gridName, GridInterceptor interceptor) {
    Assert.notEmpty(gridName);
    gridInterceptors.put(BeeUtils.normalize(gridName), interceptor);
  }

  public static WidgetSupplier registerGridSupplier(String key, String gridName,
      GridInterceptor interceptor) {
    return registerGridSupplier(key, gridName, interceptor, EnumSet.of(UiOption.ROOT), null);
  }
  
  private static GridInterceptor getInterceptorInstance(GridInterceptor interceptor) {
    if (interceptor == null) {
      return null;
    } else {
      GridInterceptor instance = interceptor.getInstance();
      if (instance == null) {
        return interceptor;
      } else {
        return instance;
      }
    }
  }

  public static WidgetSupplier registerGridSupplier(final String key, final String gridName,
      final GridInterceptor interceptor, final Collection<UiOption> uiOptions,
      final GridOptions gridOptions) {

    Assert.notEmpty(key);
    Assert.notEmpty(gridName);

    WidgetSupplier supplier = new WidgetSupplier() {
      @Override
      public void create(final Callback<IdentifiableWidget> callback) {
        createGrid(gridName, key, getInterceptorInstance(interceptor), uiOptions, gridOptions,
            new PresenterCallback() {
          @Override
          public void onCreate(Presenter presenter) {
            callback.onSuccess(presenter.getWidget());
          }
        });
      }
    };

    WidgetFactory.registerSupplier(key, supplier);
    return supplier;
  }

  public static void showGridInfo(String name) {
    if (descriptionCache.isEmpty()) {
      logger.warning("grid description cache is empty");
      return;
    }

    if (!BeeUtils.isEmpty(name)) {
      if (isGridDescriptionCached(name)) {
        GridDescription gridDescription = descriptionCache.get(gridDescriptionKey(name));
        if (gridDescription != null) {
          Global.showTable(BeeUtils.joinWords("Grid", name),
              new ExtendedPropertiesData(gridDescription.getExtendedInfo(), true));
          return;
        } else {
          logger.warning("grid", name, "description was not found");
        }
      } else {
        logger.warning("grid", name, "description not in cache");
      }
    }

    List<Property> info = new ArrayList<>();
    for (Map.Entry<String, GridDescription> entry : descriptionCache.entrySet()) {
      GridDescription gridDescription = entry.getValue();
      String cc = (gridDescription == null) ? BeeConst.STRING_MINUS
          : BeeUtils.toString(gridDescription.getColumnCount());
      info.add(new Property(entry.getKey(), cc));
    }

    Global.showTable("Grids", new PropertiesData(info, "Grid Name", "Column Count"));
  }

  public static CellGrid simpleGrid(String caption, IsTable<?, ?> table, int containerWidth) {
    Assert.notNull(table);

    int c = table.getNumberOfColumns();
    Assert.isPositive(c);

    int r = table.getNumberOfRows();
    if (r <= 0) {
      logger.warning("data table empty");
      return null;
    }

    CellGrid grid = new CellGrid();
    grid.setCaption(caption);

    DataColumn<?> column;
    for (int i = 0; i < c; i++) {
      CellSource source = CellSource.forColumn(table.getColumn(i), i);
      column = createColumn(source);

      String id = table.getColumnId(i);
      String label = table.getColumnLabel(i);

      ColumnInfo columnInfo = new ColumnInfo(id, label, source, column,
          new ColumnHeader(id, label, label));
      grid.addColumn(columnInfo);
    }

    grid.setReadOnly(true);

    grid.estimateHeaderWidths();
    grid.estimateColumnWidths(table.getRows(), 0, Math.min(r, 50));

    grid.setDefaultFlexibility(new Flexibility(1, -1, true));
    int distrWidth = containerWidth;
    if (r > 10) {
      distrWidth -= DomUtils.getScrollBarWidth();
    }
    grid.doFlexLayout(distrWidth);

    grid.setRowCount(r, false);
    grid.setRowData(table.getRows(), true);

    return grid;
  }

  private static void consumeGridDescription(final GridDescription gridDescription,
      String supplierKey, final GridInterceptor gridInterceptor,
      final PresenterCallback presenterCallback, final Collection<UiOption> uiOptions,
      final GridOptions gridOptions) {

    final Filter immutableFilter = getImmutableFilter(gridDescription, gridOptions);
    final Map<String, Filter> initialParentFilters =
        (gridInterceptor == null) ? null : gridInterceptor.getInitialParentFilters();

    List<FilterDescription> predefinedFilters =
        getPredefinedFilters(gridDescription, gridInterceptor);
    Global.getFilters().ensurePredefinedFilters(supplierKey, predefinedFilters);

    final List<FilterComponent> initialUserFilterValues =
        Global.getFilters().getInitialValues(supplierKey);

    final Order order = gridDescription.getOrder();

    String viewName = gridDescription.getViewName();

    BeeRowSet brs = null;
    if (gridInterceptor != null) {
      brs = gridInterceptor.getInitialRowSet(gridDescription);
    }

    if (BeeUtils.isEmpty(viewName) && brs == null) {
      logger.severe("grid", gridDescription.getName(), "has no initial data");
      return;
    }

    final ProviderType providerType;
    final CachingPolicy cachingPolicy;

    if (BeeUtils.isEmpty(viewName)) {
      providerType = ProviderType.LOCAL;
      cachingPolicy = CachingPolicy.NONE;
    } else {
      providerType = BeeUtils.nvl(gridDescription.getDataProvider(), ProviderType.DEFAULT);
      cachingPolicy = BeeUtils.isFalse(gridDescription.getCacheData())
          ? CachingPolicy.NONE : CachingPolicy.FULL;
    }

    if (brs != null) {
      GridView gridView = createGridView(gridDescription, supplierKey, brs.getColumns(),
          gridInterceptor, order);
      gridView.initData(brs.getNumberOfRows(), brs);

      Filter filter = GridFilterManager.parseFilter(gridView.getGrid(), initialUserFilterValues);

      createPresenter(gridDescription, gridView, brs.getNumberOfRows(), brs, providerType,
          cachingPolicy, uiOptions, gridInterceptor, immutableFilter, initialParentFilters,
          initialUserFilterValues, filter, order, gridOptions, presenterCallback);
      return;
    }

    int limit;
    if (providerType == ProviderType.CACHED) {
      limit = BeeConst.UNDEF;
    } else if (gridDescription.getInitialRowSetSize() != null) {
      limit = gridDescription.getInitialRowSetSize();
    } else {
      limit = DataUtils.getMaxInitialRowSetSize();
    }

    final boolean requestSize = limit > 0;
    Collection<Property> queryOptions;
    if (requestSize) {
      queryOptions = PropertyUtils.createProperties(Service.VAR_VIEW_SIZE, requestSize);
    } else {
      queryOptions = null;
    }

    final GridView gridView = createGridView(gridDescription, supplierKey,
        Data.getColumns(viewName), gridInterceptor, order);

    final Filter initialUserFilter = GridFilterManager.parseFilter(gridView.getGrid(),
        initialUserFilterValues);
    Filter queryFilter = getInitialQueryFilter(immutableFilter, initialParentFilters,
        initialUserFilter);

    Queries.getRowSet(viewName, null, queryFilter, order, 0, limit, cachingPolicy, queryOptions,
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet rowSet) {
            Assert.notNull(rowSet);

            int rc = rowSet.getNumberOfRows();
            if (requestSize) {
              rc = Math.max(rc, BeeUtils.toInt(rowSet.getTableProperty(Service.VAR_VIEW_SIZE)));
            }

            gridView.initData(rc, rowSet);

            createPresenter(gridDescription, gridView, rc, rowSet, providerType, cachingPolicy,
                uiOptions, gridInterceptor, immutableFilter, initialParentFilters,
                initialUserFilterValues, initialUserFilter, order, gridOptions, presenterCallback);
          }
        });
  }

  private static GridView createGridView(GridDescription gridDescription, String supplierKey,
      List<BeeColumn> dataColumns, GridInterceptor gridInterceptor, Order order) {
    return createGridView(gridDescription, supplierKey, dataColumns, null, gridInterceptor, order);
  }

  private static void createPresenter(GridDescription gridDescription, GridView gridView,
      int rowCount, BeeRowSet rowSet, ProviderType providerType, CachingPolicy cachingPolicy,
      Collection<UiOption> uiOptions, GridInterceptor gridInterceptor,
      Filter immutableFilter, Map<String, Filter> parentFilters,
      List<FilterComponent> userFilterValues, Filter userFilter,
      Order order, GridOptions gridOptions, PresenterCallback presenterCallback) {

    GridPresenter presenter = new GridPresenter(gridDescription, gridView, rowCount, rowSet,
        providerType, cachingPolicy, uiOptions, gridInterceptor, immutableFilter,
        parentFilters, userFilterValues, userFilter, order, gridOptions);

    if (gridInterceptor != null) {
      gridInterceptor.afterCreatePresenter(presenter);
    }

    presenterCallback.onCreate(presenter);
  }

  private static String gridDescriptionKey(String name) {
    return name.trim().toLowerCase();
  }

  private static boolean isGridDescriptionCached(String name) {
    if (BeeUtils.isEmpty(name)) {
      return false;
    }
    return descriptionCache.containsKey(gridDescriptionKey(name));
  }

  private GridFactory() {
  }
}