package com.butent.bee.client.view.grid;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.HasDimensions;
import com.butent.bee.client.widget.DndDiv;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.DataEvent;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GridSettings implements HandlesAllDataEvents {

  private static final BeeLogger logger = LogUtils.getLogger(GridSettings.class);

  private static final GridSettings INSTANCE = new GridSettings();

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "GridSettings-";
  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog";
  private static final String STYLE_WRAPPER = STYLE_PREFIX + "wrapper";
  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_LABEL = STYLE_PREFIX + "label";
  private static final String STYLE_VISIBILITY = STYLE_PREFIX + "visibility";
  private static final String STYLE_EDIT_IN_PLACE = STYLE_PREFIX + "editInPlace";
  private static final String STYLE_DRAG = STYLE_PREFIX + "drag";

  private static final String DND_CONTENT_TYPE = "GridColumn";

  private static final int LABEL_COL = 0;
  private static final int VISIBILITY_COL = 1;
  private static final int EDIT_IN_PLACE_COL = 2;

  private final Map<String, GridConfig> grids = new HashMap<>();

  public static GridDescription apply(String key, GridDescription input) {
    GridConfig gridConfig = INSTANCE.grids.get(key);

    if (gridConfig != null && !gridConfig.isEmpty()) {
      GridDescription gridDescription = input.copy();
      gridConfig.applyTo(gridDescription);
      return gridDescription;

    } else {
      return input;
    }
  }

  public static boolean contains(String key) {
    return INSTANCE.grids.containsKey(key);
  }

  public static void handle(final String key, final GridView gridView, Element target) {
    Assert.notNull(gridView);
    if (gridView.isEmpty()) {
      return;
    }

    final CellGrid grid = gridView.getGrid();

    final List<ColumnInfo> predefinedColumns = grid.getStaticPredefinedColumns();
    List<Integer> visibleColumns = grid.getStaticVisibleColumns();

    final boolean canEditInPlace = canEditInPlace(gridView);

    final HtmlTable table = new HtmlTable();
    table.addStyleName(STYLE_TABLE);

    int row = 0;

    for (int index : visibleColumns) {
      ColumnInfo columnInfo = predefinedColumns.get(index);

      table.setWidget(row, LABEL_COL, createLabel(columnInfo, index));
      if (columnInfo.isHidable()) {
        table.setWidget(row, VISIBILITY_COL, createVisibilityToggle(true));
      }

      if (canEditInPlace && !columnInfo.isColReadOnly()) {
        table.setWidget(row, EDIT_IN_PLACE_COL, createEditInPlaceToggle(gridView, columnInfo));
      }

      row++;
    }

    if (predefinedColumns.size() > visibleColumns.size()) {
      for (int i = 0; i < predefinedColumns.size(); i++) {
        if (!visibleColumns.contains(i)) {
          ColumnInfo columnInfo = predefinedColumns.get(i);

          table.setWidget(row, LABEL_COL, createLabel(columnInfo, i));
          table.setWidget(row, VISIBILITY_COL, createVisibilityToggle(false));

          if (canEditInPlace && !columnInfo.isColReadOnly()) {
            table.setWidget(row, EDIT_IN_PLACE_COL, createEditInPlaceToggle(gridView, columnInfo));
          }

          row++;
        }
      }
    }

    Flow wrapper = new Flow();
    wrapper.addStyleName(STYLE_WRAPPER);

    wrapper.add(table);

    DndHelper.makeTarget(wrapper, Lists.newArrayList(DND_CONTENT_TYPE), null,
        DndHelper.ALWAYS_TARGET, new BiConsumer<DropEvent, Object>() {
          @Override
          public void accept(DropEvent t, Object u) {
            if (u instanceof Integer) {
              onDrop(t, table, BeeUtils.unbox((Integer) u), gridView, predefinedColumns);
            }
          }
        });

    Global.inputWidget(Localized.getConstants().settings(), wrapper, new InputCallback() {
      @Override
      public void onSuccess() {
        List<Integer> selectedColumns = getSelectedColumns(table, predefinedColumns);

        final Set<String> editInPlaceColumns;
        if (canEditInPlace) {
          editInPlaceColumns = getEditInPlaceColumns(table, predefinedColumns);
        } else {
          editInPlaceColumns = BeeConst.EMPTY_IMMUTABLE_STRING_SET;
        }

        if (grid.updateStaticVisibleColumns(selectedColumns)) {
          List<String> names = new ArrayList<>();

          List<ColumnInfo> columns = grid.getColumns();
          for (ColumnInfo columnInfo : columns) {
            if (!columnInfo.isDynamic()) {
              names.add(columnInfo.getColumnId());
            }
          }

          INSTANCE.saveGridSetting(key, GridConfig.getColumnsIndex(), NameUtils.join(names),
              new IdCallback() {
                @Override
                public void onSuccess(Long result) {
                  if (canEditInPlace) {
                    INSTANCE.maybeUpdateEditInplace(key, gridView, editInPlaceColumns);
                  }
                }
              });

        } else if (canEditInPlace) {
          INSTANCE.maybeUpdateEditInplace(key, gridView, editInPlaceColumns);
        }
      }
    }, STYLE_DIALOG, target);
  }

  public static boolean hasVisibleColumns(String key) {
    GridConfig gridConfig = INSTANCE.grids.get(key);
    return gridConfig != null && gridConfig.hasVisibleColumns();
  }

  public static void load(String serializedGridSettings, String serializedColumnSettings) {
    INSTANCE.grids.clear();

    if (!BeeUtils.isEmpty(serializedGridSettings)) {
      BeeRowSet gridRowSet = BeeRowSet.restore(serializedGridSettings);
      GridConfig.ensureFields(gridRowSet.getColumns());

      for (BeeRow gridRow : gridRowSet.getRows()) {
        INSTANCE.grids.put(gridRow.getString(GridConfig.getKeyIndex()), new GridConfig(gridRow));
      }

      logger.info("grid settings", INSTANCE.grids.size());

      if (!BeeUtils.isEmpty(serializedColumnSettings)) {
        BeeRowSet columnRowSet = BeeRowSet.restore(serializedColumnSettings);
        ColumnConfig.ensureFields(columnRowSet.getColumns());

        GridConfig gridConfig = null;
        int cc = 0;

        for (BeeRow columnRow : columnRowSet.getRows()) {
          Long gridId = columnRow.getLong(ColumnConfig.getGridIndex());
          if (gridConfig == null || !gridId.equals(gridConfig.getRowId())) {
            gridConfig = INSTANCE.findGridConfig(BeeUtils.unbox(gridId));
          }

          ColumnConfig columnConfig = new ColumnConfig(columnRow);
          gridConfig.columnSettings.put(columnConfig.getName(), columnConfig);

          cc++;
        }

        logger.info("column settings", cc);
      }
    }
  }

  public static void onSettingsChange(String key, final SettingsChangeEvent event) {
    if (HasDimensions.ATTR_HEIGHT.equals(event.getAttribute())) {
      int index = BeeConst.UNDEF;

      if (event.getComponentType() != null) {
        switch (event.getComponentType()) {
          case HEADER:
            index = GridConfig.getHeaderHeightIndex();
            break;
          case BODY:
            index = GridConfig.getRowHeightIndex();
            break;
          case FOOTER:
            index = GridConfig.getFooterHeightIndex();
            break;
        }
      }

      if (!BeeConst.isUndef(index)) {
        INSTANCE.saveGridSetting(key, index, event.getValue(), null);
      }

    } else if (HasDimensions.ATTR_WIDTH.equals(event.getAttribute())) {
      if (!BeeUtils.isEmpty(event.getColumnName())) {
        INSTANCE.ensureGridConfig(key, new Callback<GridConfig>() {
          @Override
          public void onSuccess(GridConfig result) {
            result.saveColumnSetting(event.getColumnName(), ColumnConfig.getWidthIndex(),
                event.getValue());
          }
        });
      }

    } else {
      logger.warning(key, event.getComponentType(), event.getColumnName(), event.getAttribute(),
          event.getValue(), "not persisted");
    }
  }

  public static void refresh() {
    BeeKeeper.getRpc().makeGetRequest(Service.GET_GRID_SETTINGS, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        INSTANCE.grids.clear();

        if (response.hasResponse()) {
          Pair<String, String> data = Pair.restore(response.getResponseAsString());
          load(data.getA(), data.getB());
        }
      }
    });
  }

  public static boolean reset(String key) {
    final GridConfig gridConfig = INSTANCE.grids.get(key);

    if (gridConfig == null) {
      return false;

    } else {
      Queries.deleteRow(GridDescription.VIEW_GRID_SETTINGS, gridConfig.getRowId(),
          new Queries.IntCallback() {
            @Override
            public void onSuccess(Integer result) {
              RowDeleteEvent.fire(BeeKeeper.getBus(), GridDescription.VIEW_GRID_SETTINGS,
                  gridConfig.getRowId());
            }
          });

      return true;
    }
  }

  public static void saveSortOrder(String key, Order order) {
    INSTANCE.saveGridSetting(key, GridConfig.getOrderIndex(),
        (order == null || order.isEmpty()) ? null : order.serialize(), null);
  }

  private static boolean canEditInPlace(GridView gridView) {
    return gridView != null && !gridView.isReadOnly()
        && !BeeUtils.isEmpty(gridView.getEditFormName());
  }

  private static Widget createEditInPlaceToggle(GridView gridView, ColumnInfo columnInfo) {
    boolean value = gridView.getEditInPlace().contains(columnInfo.getColumnId());
    return createEditInPlaceToggle(value);
  }

  private static Widget createEditInPlaceToggle(boolean value) {
    Toggle toggle = new Toggle(FontAwesome.SQUARE_O, FontAwesome.EDIT, STYLE_EDIT_IN_PLACE, value);
    toggle.setTitle(Localized.getConstants().rightStateEdit());
    return toggle;
  }

  private static Widget createLabel(ColumnInfo columnInfo, int index) {
    DndDiv widget = new DndDiv(STYLE_LABEL);

    widget.setHtml(columnInfo.getLabel());
    DomUtils.setDataIndex(widget.getElement(), index);

    DndHelper.makeSource(widget, DND_CONTENT_TYPE, index, STYLE_DRAG);

    return widget;
  }

  private static Widget createVisibilityToggle(boolean value) {
    Toggle toggle = new Toggle(FontAwesome.CIRCLE_THIN, FontAwesome.EYE, STYLE_VISIBILITY, value);
    toggle.setTitle(Localized.getConstants().rightStateView());
    return toggle;
  }

  private static int getDataIndex(HtmlTable table, int row) {
    return DomUtils.getDataIndexInt(table.getWidget(row, LABEL_COL).getElement());
  }

  private static Set<String> getEditInPlaceColumns(HtmlTable table,
      List<ColumnInfo> predefinedColumns) {

    Set<String> result = new HashSet<>();

    for (int i = 0; i < table.getRowCount(); i++) {
      int index = getDataIndex(table, i);
      ColumnInfo columnInfo = predefinedColumns.get(index);

      if (!columnInfo.isColReadOnly() && isChecked(table, i, EDIT_IN_PLACE_COL)) {
        result.add(columnInfo.getColumnId());
      }
    }

    return result;
  }

  private static List<Integer> getSelectedColumns(HtmlTable table,
      List<ColumnInfo> predefinedColumns) {

    List<Integer> selectedColumns = new ArrayList<>();

    for (int i = 0; i < table.getRowCount(); i++) {
      int index = getDataIndex(table, i);
      ColumnInfo columnInfo = predefinedColumns.get(index);

      boolean visible;

      if (columnInfo.isHidable()) {
        visible = isChecked(table, i, VISIBILITY_COL);
      } else {
        visible = true;
      }

      if (visible) {
        selectedColumns.add(index);
      }
    }

    return selectedColumns;
  }

  private static boolean isChecked(HtmlTable table, int row, int col) {
    Widget checkBox = table.getWidget(row, col);
    return checkBox instanceof HasCheckedness && ((HasCheckedness) checkBox).isChecked();
  }

  private static void onDrop(DropEvent event, HtmlTable table, int sourceIndex,
      GridView gridView, List<ColumnInfo> predefinedColumns) {

    int y = event.getNativeEvent().getClientY();

    int sourceRow = BeeConst.UNDEF;
    int targetRow = BeeConst.UNDEF;

    int rowCount = table.getRowCount();

    for (int i = 0; i < rowCount; i++) {
      if (sourceRow == BeeConst.UNDEF && getDataIndex(table, i) == sourceIndex) {
        sourceRow = i;
      }

      if (targetRow == BeeConst.UNDEF) {
        Element rowElement = table.getRow(i);

        int top = rowElement.getAbsoluteTop();
        int height = rowElement.getOffsetHeight();

        if (y < top + height / 2) {
          targetRow = i;
        } else if (i >= rowCount - 1) {
          targetRow = rowCount;
        }
      }
    }

    if (sourceRow == BeeConst.UNDEF || targetRow == BeeConst.UNDEF
        || sourceRow == targetRow || targetRow == rowCount && sourceRow >= rowCount - 1) {
      return;
    }

    List<Integer> indexes = new ArrayList<>();
    for (int i = 0; i < rowCount; i++) {
      if (i == targetRow) {
        indexes.add(sourceIndex);
      }
      if (i != sourceRow) {
        indexes.add(getDataIndex(table, i));
      }
    }

    if (targetRow == rowCount) {
      indexes.add(sourceIndex);
    }

    List<Integer> selectedColumns = getSelectedColumns(table, predefinedColumns);

    boolean canEditInPlace = canEditInPlace(gridView);

    Set<String> editInPlaceColumns;
    if (canEditInPlace) {
      editInPlaceColumns = getEditInPlaceColumns(table, predefinedColumns);
    } else {
      editInPlaceColumns = BeeConst.EMPTY_IMMUTABLE_STRING_SET;
    }

    table.clear();

    int row = 0;

    for (int index : indexes) {
      ColumnInfo columnInfo = predefinedColumns.get(index);
      table.setWidget(row, LABEL_COL, createLabel(columnInfo, index));

      if (columnInfo.isHidable()) {
        table.setWidget(row, VISIBILITY_COL,
            createVisibilityToggle(selectedColumns.contains(index)));
      }

      if (canEditInPlace && !columnInfo.isColReadOnly()) {
        boolean value = editInPlaceColumns.contains(columnInfo.getColumnId());
        table.setWidget(row, EDIT_IN_PLACE_COL, createEditInPlaceToggle(value));
      }

      row++;
    }
  }

  private GridSettings() {
    BeeKeeper.getBus().registerDataHandler(this, false);
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (isGridEvent(event)) {
      GridConfig gridConfig = findGridConfig(event.getRowId());
      if (gridConfig != null) {
        event.applyTo(gridConfig.getRow());
      }

    } else if (isColumnEvent(event)) {
      for (GridConfig gridConfig : grids.values()) {
        ColumnConfig columnConfig = gridConfig.findColumnConfig(event.getRowId());
        if (columnConfig != null) {
          event.applyTo(columnConfig.getRow());
          break;
        }
      }
    }
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (isGridEvent(event) || isColumnEvent(event)) {
      refresh();
    }
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    if (isGridEvent(event)) {
      for (long id : event.getRowIds()) {
        maybeRemoveGrid(id);
      }
    } else if (isColumnEvent(event)) {
      for (long id : event.getRowIds()) {
        maybeRemoveColumn(id);
      }
    }
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (isGridEvent(event)) {
      maybeRemoveGrid(event.getRowId());
    } else if (isColumnEvent(event)) {
      maybeRemoveColumn(event.getRowId());
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (isGridEvent(event) || isColumnEvent(event)) {
      refresh();
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (isGridEvent(event)) {
      GridConfig gridConfig = findGridConfig(event.getRowId());
      if (gridConfig != null) {
        gridConfig.setRow(event.getRow());
      }

    } else if (isColumnEvent(event)) {
      for (GridConfig gridConfig : grids.values()) {
        ColumnConfig columnConfig = gridConfig.findColumnConfig(event.getRowId());
        if (columnConfig != null) {
          columnConfig.setRow(event.getRow());
          break;
        }
      }
    }
  }

  private void ensureGridConfig(final String key, final Callback<GridConfig> callback) {
    GridConfig gridConfig = grids.get(key);

    if (gridConfig != null) {
      callback.onSuccess(gridConfig);

    } else {
      ParameterList params = BeeKeeper.getRpc().createParameters(Service.ENSURE_GRID_SETTINGS);

      params.addQueryItem(GridDescription.COL_GRID_SETTING_KEY, key);
      params.addQueryItem(GridDescription.COL_GRID_SETTING_USER, BeeKeeper.getUser().getUserId());

      BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          if (response.hasResponse(BeeRow.class)) {
            BeeRow row = BeeRow.restore(response.getResponseAsString());

            GridConfig gcfg = new GridConfig(row);
            grids.put(key, gcfg);

            callback.onSuccess(gcfg);
          }
        }
      });
    }
  }

  private GridConfig findGridConfig(long id) {
    for (GridConfig gridConfig : grids.values()) {
      if (gridConfig.getRowId() == id) {
        return gridConfig;
      }
    }
    return null;
  }

  private String findGridKey(long id) {
    for (Map.Entry<String, GridConfig> entry : grids.entrySet()) {
      if (entry.getValue().getRowId() == id) {
        return entry.getKey();
      }
    }
    return null;
  }

  private static boolean isColumnEvent(DataEvent event) {
    return event != null && event.hasView(ColumnDescription.VIEW_COLUMN_SETTINGS);
  }

  private static boolean isGridEvent(DataEvent event) {
    return event != null && event.hasView(GridDescription.VIEW_GRID_SETTINGS);
  }

  private boolean maybeRemoveColumn(long id) {
    for (GridConfig gridConfig : grids.values()) {
      if (gridConfig.maybeRemoveColumn(id)) {
        return true;
      }
    }
    return false;
  }

  private boolean maybeRemoveGrid(long id) {
    String key = findGridKey(id);

    if (key == null) {
      return false;
    } else {
      grids.remove(key);
      return true;
    }
  }

  private void maybeUpdateEditInplace(String key, final GridView gridView,
      final Set<String> newColumns) {

    final Set<String> oldColumns = gridView.getEditInPlace();
    if (oldColumns.equals(newColumns)) {
      return;
    }

    ensureGridConfig(key, new Callback<GridConfig>() {
      @Override
      public void onSuccess(GridConfig gridConfig) {
        int index = ColumnConfig.getEditInPlaceIndex();

        for (String column : oldColumns) {
          if (!newColumns.contains(column)) {
            gridConfig.saveColumnSetting(column, index, null);
          }
        }

        for (String column : newColumns) {
          if (!oldColumns.contains(column)) {
            gridConfig.saveColumnSetting(column, index, BooleanValue.S_TRUE);
          }
        }

        BeeUtils.overwrite(gridView.getEditInPlace(), newColumns);
      }
    });
  }

  private void saveGridSetting(final String key, int index, String value,
      final IdCallback callback) {

    Assert.notEmpty(key);
    Assert.isIndex(GridConfig.getDataColumns(), index);

    final BeeColumn dataColumn = GridConfig.getDataColumns().get(index);
    final String newValue = GridUtils.normalizeValue(value);

    GridConfig gridConfig = grids.get(key);

    if (gridConfig == null) {
      if (newValue != null) {
        List<BeeColumn> columns = new ArrayList<>();
        columns.add(GridConfig.getDataColumns().get(GridConfig.getUserIndex()));
        columns.add(GridConfig.getDataColumns().get(GridConfig.getKeyIndex()));
        columns.add(dataColumn);

        List<String> values = Queries.asList(BeeKeeper.getUser().getUserId(), key, newValue);

        Queries.insert(GridDescription.VIEW_GRID_SETTINGS, columns, values, null,
            new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
                grids.put(key, new GridConfig(result));
                logger.debug("created grid settings:", key, dataColumn.getId(), newValue);

                if (callback != null) {
                  callback.onSuccess(result.getId());
                }
              }
            });

      } else if (callback != null) {
        callback.onSuccess(null);
      }

    } else if (gridConfig.setValue(index, newValue)) {
      final long id = gridConfig.getRowId();

      Queries.update(GridDescription.VIEW_GRID_SETTINGS, Filter.compareId(id), dataColumn.getId(),
          new TextValue(newValue), new Queries.IntCallback() {
            @Override
            public void onSuccess(Integer result) {
              if (BeeUtils.unbox(result) == 1) {
                logger.debug("updated grid settings:", key, dataColumn.getId(), newValue);
                if (callback != null) {
                  callback.onSuccess(id);
                }

              } else {
                logger.warning("could not update grid settings:", result);
                logger.warning(key, dataColumn.getId(), newValue);
              }
            }
          });

    } else if (callback != null) {
      callback.onSuccess(gridConfig.getRowId());
    }
  }
}
