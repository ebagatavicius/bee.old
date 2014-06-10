package com.butent.bee.client.view.grid;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.HasDimensions;
import com.butent.bee.client.widget.BooleanWidget;
import com.butent.bee.client.widget.DndDiv;
import com.butent.bee.client.widget.SimpleCheckBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GridSettings {

  private static final BeeLogger logger = LogUtils.getLogger(GridSettings.class);

  private static final String STYLE_PREFIX = "bee-GridSettings-";
  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog";
  private static final String STYLE_WRAPPER = STYLE_PREFIX + "wrapper";
  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_CHECK = STYLE_PREFIX + "check";
  private static final String STYLE_LABEL = STYLE_PREFIX + "label";
  private static final String STYLE_DRAG = STYLE_PREFIX + "drag";

  private static final String DND_CONTENT_TYPE = "GridColumn";

  private static final int CHECK_COL = 0;
  private static final int LABEL_COL = 1;

  private static final Map<String, GridConfig> grids = new HashMap<>();

  public static GridDescription apply(String key, GridDescription input) {
    GridConfig gridConfig = grids.get(key);

    if (gridConfig != null && !gridConfig.isEmpty()) {
      GridDescription gridDescription = input.copy();
      gridConfig.applyTo(gridDescription);
      return gridDescription;

    } else {
      return input;
    }
  }

  public static void handle(final String key, final CellGrid grid, Element target) {
    Assert.notNull(grid);
    if (grid.getRowData().isEmpty()) {
      return;
    }

    final List<ColumnInfo> predefinedColumns = grid.getStaticPredefinedColumns();
    List<Integer> visibleColumns = grid.getStaticVisibleColumns();

    final HtmlTable table = new HtmlTable();
    table.addStyleName(STYLE_TABLE);

    int row = 0;

    for (int index : visibleColumns) {
      ColumnInfo columnInfo = predefinedColumns.get(index);

      if (columnInfo.isHidable()) {
        table.setWidget(row, CHECK_COL, createCheckBox(true));
      }
      table.setWidget(row, LABEL_COL, createLabel(columnInfo, index));

      row++;
    }

    if (predefinedColumns.size() > visibleColumns.size()) {
      for (int i = 0; i < predefinedColumns.size(); i++) {
        if (!visibleColumns.contains(i)) {
          table.setWidget(row, CHECK_COL, createCheckBox(false));
          table.setWidget(row, LABEL_COL, createLabel(predefinedColumns.get(i), i));

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
              onDrop(t, table, BeeUtils.unbox((Integer) u), predefinedColumns);
            }
          }
        });

    Global.inputWidget(Localized.getConstants().settings(), wrapper, new InputCallback() {
      @Override
      public void onSuccess() {
        List<Integer> selectedColumns = getSelectedColumns(table, predefinedColumns);

        if (grid.updateStaticVisibleColumns(selectedColumns)) {
          List<String> names = new ArrayList<>();

          List<ColumnInfo> columns = grid.getColumns();
          for (ColumnInfo columnInfo : columns) {
            if (!columnInfo.isDynamic()) {
              names.add(columnInfo.getColumnId());
            }
          }

          saveGridSetting(key, GridConfig.getColumnsIndex(), NameUtils.join(names));
        }
      }
    }, STYLE_DIALOG, target);
  }
  
  public static boolean hasVisibleColumns(String key) {
    GridConfig gridConfig = grids.get(key);
    return gridConfig != null && gridConfig.hasVisibleColumns();
  }
  
  public static void load(String serializedGridSettings, String serializedColumnSettings) {
    grids.clear();

    if (!BeeUtils.isEmpty(serializedGridSettings)) {
      BeeRowSet gridRowSet = BeeRowSet.restore(serializedGridSettings);
      GridConfig.ensureFields(gridRowSet.getColumns());

      for (BeeRow gridRow : gridRowSet.getRows()) {
        grids.put(gridRow.getString(GridConfig.getKeyIndex()), new GridConfig(gridRow));
      }

      logger.info("grid settings", grids.size());

      if (!BeeUtils.isEmpty(serializedColumnSettings)) {
        BeeRowSet columnRowSet = BeeRowSet.restore(serializedColumnSettings);
        ColumnConfig.ensureFields(columnRowSet.getColumns());

        GridConfig gridConfig = null;
        int cc = 0;

        for (BeeRow columnRow : columnRowSet.getRows()) {
          Long gridId = columnRow.getLong(ColumnConfig.getGridIndex());
          if (gridConfig == null || !gridId.equals(gridConfig.row.getId())) {
            gridConfig = findGridByRowId(BeeUtils.unbox(gridId));
          }

          ColumnConfig columnConfig = new ColumnConfig(columnRow);
          gridConfig.columnSettings.put(columnConfig.getName(), columnConfig);

          cc++;
        }

        logger.info("column settings", cc);
      }
    }
  }
  
  public static void onSettingsChange(String key, SettingsChangeEvent event) {
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
        saveGridSetting(key, index, event.getValue());
      }

    } else if (HasDimensions.ATTR_WIDTH.equals(event.getAttribute())) {
      if (!BeeUtils.isEmpty(event.getColumnName())) {
        saveColumnSetting(key, event.getColumnName(), ColumnConfig.getWidthIndex(),
            event.getValue());
      }

    } else {
      logger.warning(key, event.getComponentType(), event.getColumnName(), event.getAttribute(),
          event.getValue(), "not persisted");
    }
  }

  public static void saveSortOrder(String key, Order order) {
    saveGridSetting(key, GridConfig.getOrderIndex(),
        (order == null || order.isEmpty()) ? null : order.serialize());
  }

  private static Widget createCheckBox(boolean value) {
    SimpleCheckBox widget = new SimpleCheckBox(value);
    widget.addStyleName(STYLE_CHECK);
    return widget;
  }

  private static Widget createLabel(ColumnInfo columnInfo, int index) {
    DndDiv widget = new DndDiv(STYLE_LABEL);

    widget.setHtml(columnInfo.getLabel());
    DomUtils.setDataIndex(widget.getElement(), index);

    DndHelper.makeSource(widget, DND_CONTENT_TYPE, index, STYLE_DRAG);

    return widget;
  }

  private static GridConfig findGridByRowId(long id) {
    for (GridConfig gridConfig : grids.values()) {
      if (gridConfig.row.getId() == id) {
        return gridConfig;
      }
    }
    return null;
  }

  private static int getDataIndex(HtmlTable table, int row) {
    return DomUtils.getDataIndexInt(table.getWidget(row, LABEL_COL).getElement());    
  }

  private static List<Integer> getSelectedColumns(HtmlTable table, 
      List<ColumnInfo> predefinedColumns) {

    List<Integer> selectedColumns = new ArrayList<>();

    for (int i = 0; i < table.getRowCount(); i++) {
      int index = getDataIndex(table, i);
      ColumnInfo columnInfo = predefinedColumns.get(index);

      boolean visible;

      if (columnInfo.isHidable()) {
        Widget checkBox = table.getWidget(i, CHECK_COL);
        visible = checkBox instanceof BooleanWidget
            && BeeUtils.isTrue(((BooleanWidget) checkBox).getValue());
      } else {
        visible = true;
      }

      if (visible) {
        selectedColumns.add(index);
      }
    }
    
    return selectedColumns;
  }

  private static void onDrop(DropEvent event, HtmlTable table, int sourceIndex,
      List<ColumnInfo> predefinedColumns) {

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
    
    table.clear();
    
    int row = 0;

    for (int index : indexes) {
      ColumnInfo columnInfo = predefinedColumns.get(index);

      if (columnInfo.isHidable()) {
        table.setWidget(row, CHECK_COL, createCheckBox(selectedColumns.contains(index)));
      }
      table.setWidget(row, LABEL_COL, createLabel(columnInfo, index));

      row++;
    }
  }

  private static void saveColumnSetting(final String key, final String name, final int index,
      final String value) {

    Assert.notEmpty(key);
    GridConfig gridConfig = grids.get(key);

    if (gridConfig != null) {
      gridConfig.saveColumnSetting(name, index, value);

    } else if (!BeeUtils.isEmpty(value)) {
      List<BeeColumn> columns = new ArrayList<>();
      columns.add(GridConfig.getDataColumns().get(GridConfig.getUserIndex()));
      columns.add(GridConfig.getDataColumns().get(GridConfig.getKeyIndex()));

      List<String> values = Queries.asList(BeeKeeper.getUser().getUserId(), key);

      Queries.insert(GridDescription.VIEW_GRID_SETTINGS, columns, values, null,
          new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              GridConfig gcfg = new GridConfig(result);
              grids.put(key, gcfg);

              gcfg.saveColumnSetting(name, index, value);
            }
          });
    }
  }

  private static void saveGridSetting(final String key, int index, String value) {
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
              }
            });
      }

    } else if (!BeeUtils.equalsTrim(gridConfig.row.getString(index), newValue)) {
      gridConfig.row.setValue(index, newValue);

      Queries.update(GridDescription.VIEW_GRID_SETTINGS,
          Filter.compareId(gridConfig.row.getId()), dataColumn.getId(),
          new TextValue(newValue), new Queries.IntCallback() {
            @Override
            public void onSuccess(Integer result) {
              if (BeeUtils.unbox(result) == 1) {
                logger.debug("updated grid settings:", key, dataColumn.getId(), newValue);
              } else {
                logger.warning("could not update grid settings:", result);
                logger.warning(key, dataColumn.getId(), newValue);
              }
            }
          });
    }
  }

  private GridSettings() {
  }
}
