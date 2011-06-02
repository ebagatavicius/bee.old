package com.butent.bee.client.grid;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.MultiSelectionModel;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.CachedProvider;
import com.butent.bee.client.data.KeyProvider;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.model.CachedTableModel;
import com.butent.bee.client.grid.model.TableModel;
import com.butent.bee.client.grid.model.TableModelHelper.Request;
import com.butent.bee.client.grid.model.TableModelHelper.Response;
import com.butent.bee.client.grid.render.FixedWidthGridBulkRenderer;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.IsTable;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.ui.BeeGrid;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Creates simple and scroll grid objects for usage in the user interface.
 */

public class GridFactory {

  public interface GridCallback extends Callback<BeeGrid, String[]> {
  }

  /**
   * Sets id and order for scroll grid columns.
   */

  private static class ScrollGridColumnDefinition extends ColumnDefinition {
    private int idx;
    private int maxDisplaySize;

    private ScrollGridColumnDefinition(int idx) {
      this(idx, -1);
    }

    private ScrollGridColumnDefinition(int idx, int max) {
      this.idx = idx;
      this.maxDisplaySize = max;

      setColumnId(idx);
      setColumnOrder(idx);
    }

    @Override
    public String getCellValue(IsRow rowValue) {
      String v = rowValue.getString(idx);
      if (v == null) {
        return BeeConst.STRING_EMPTY;
      }
      if (maxDisplaySize <= 0 || v.length() <= maxDisplaySize) {
        return v;
      }

      return BeeUtils.clip(v, maxDisplaySize);
    }

    @Override
    public void setCellValue(IsRow rowValue, Object cellValue) {
      rowValue.setValue(idx, BeeUtils.transform(cellValue));
    }
  }

  /**
   * Adds up row values into one object for response purposes.
   */

  private static class ScrollGridResponse extends Response {
    private Collection<IsRow> rowValues = new ArrayList<IsRow>();

    private ScrollGridResponse(IsTable<?, ?> data, int start, int cnt, int tot) {
      for (int i = start; i < start + cnt && i < tot; i++) {
        this.rowValues.add(data.getRow(i));
      }
    }

    @Override
    public Iterator<IsRow> getRowValues() {
      return rowValues.iterator();
    }
  }

  /**
   * Extends {@code TableModel} class, gets starting row of selection and number of rows to select.
   */

  private static class ScrollGridTableModel extends TableModel {
    private IsTable<?, ?> data;

    private ScrollGridTableModel(IsTable<?, ?> data) {
      super();
      this.data = data;
    }

    public void requestRows(Request request, Callback callback) {
      int start = request.getStartRow();
      int cnt = request.getNumRows();

      ScrollGridResponse resp = new ScrollGridResponse(data, start, cnt, data.getNumberOfRows());
      callback.onRowsReady(request, resp);
    }
  }

  private static final Map<String, BeeGrid> gridCache = Maps.newHashMap();

  public static Widget cellTable(Object data, CellType cellType, String... columnLabels) {
    Assert.notNull(data);

    IsTable<?, ?> table = DataUtils.createTable(data, columnLabels);
    Assert.notNull(table);

    int c = table.getNumberOfColumns();
    Assert.isPositive(c);

    int r = table.getNumberOfRows();
    if (r <= 0) {
      BeeKeeper.getLog().warning("data table empty");
      return null;
    }

    CellTable<IsRow> grid = new CellTable<IsRow>(r);

    TextColumn column;
    for (int i = 0; i < c; i++) {
      column = new TextColumn(createCell(cellType), i, table.getColumn(i));
      if (cellType != null && cellType.isEditable()) {
        column.setFieldUpdater(new CellUpdater(i));
      }
      grid.addColumn(column, table.getColumnLabel(i));
    }

    MultiSelectionModel<IsRow> selector = new MultiSelectionModel<IsRow>(new KeyProvider());
    grid.setSelectionModel(selector);

    grid.setRowCount(r, true);
    grid.setRowData(table.getRows().getList());

    return grid;
  }

  public static void clearCache() {
    gridCache.clear();
  }

  public static CellColumn<?> createColumn(IsColumn dataColumn, int index) {
    ValueType type = dataColumn.getType();
    if (type == null) {
      return new TextColumn(index, dataColumn);
    }

    switch (type) {
      case BOOLEAN:
        return new BooleanColumn(index, dataColumn);
      case DATE:
        return new DateColumn(index, dataColumn);
      case DATETIME:
        return new DateTimeColumn(index, dataColumn);
      case NUMBER:
        return new DoubleColumn(index, dataColumn);
      case INTEGER:
        return new IntegerColumn(index, dataColumn);
      case LONG:
        return new LongColumn(index, dataColumn);
      case DECIMAL:
        if (dataColumn.getScale() == 2) {
          return new CurrencyColumn(index, dataColumn);
        } else {
          return new DecimalColumn(index, dataColumn);
        }
      default:
        return new TextColumn(index, dataColumn);
    }
  }

  public static void getGrid(String name, GridCallback callback) {
    getGrid(name, callback, false);
  }

  public static void getGrid(final String name, final GridCallback callback, boolean reload) {
    Assert.notEmpty(name);
    Assert.notNull(callback);

    if (!reload && isGridCached(name)) {
      callback.onSuccess(gridCache.get(gridKey(name)));
      return;
    }

    BeeKeeper.getRpc().sendText(Service.GET_GRID, name, new ResponseCallback() {
      public void onResponse(JsArrayString respArr) {
        if (respArr.length() >= 2
            && BeeUtils.same(respArr.get(1), BeeUtils.getClassName(BeeGrid.class))) {
          BeeGrid grd = BeeGrid.restore(respArr.get(0));
          callback.onSuccess(grd);
          gridCache.put(gridKey(name), grd);
        } else {
          callback.onFailure(Codec.beeDeserialize(respArr.get(0)));
          gridCache.put(gridKey(name), null);
        }
      }
    });
  }

  public static Widget scrollGrid(int width, Object data, String... columnLabels) {
    Assert.notNull(data);

    IsTable<?, ?> table = DataUtils.createTable(data, columnLabels);
    Assert.notNull(table);

    int c = table.getNumberOfColumns();
    Assert.isPositive(c);

    int r = table.getNumberOfRows();
    if (r <= 0) {
      BeeKeeper.getLog().warning("data table empty");
      return null;
    }

    ScrollGridTableModel tableModel = new ScrollGridTableModel(table);
    CachedTableModel cachedModel = new CachedTableModel(tableModel);
    cachedModel.setRowCount(r);

    TableDefinition tableDef = new TableDefinition();

    for (int i = 0; i < c; i++) {
      ScrollGridColumnDefinition colDef = new ScrollGridColumnDefinition(i, 512);
      colDef.setHeader(table.getColumnLabel(i));
      colDef.setFooter("col " + i);

      tableDef.addColumnDefinition(colDef);
    }

    ScrollTable grid = new ScrollTable(cachedModel, tableDef);
    if (width > c) {
      int w = (width - DomUtils.getScrollbarWidth() - 2) / c;
      grid.setDefaultColumnWidth(BeeUtils.limit(w, 60, 300));
    }
    grid.createFooterTable();

    FixedWidthGridBulkRenderer renderer =
        new FixedWidthGridBulkRenderer(grid.getDataTable(), grid);
    grid.setBulkRenderer(renderer);

    return grid;
  }

  public static void showGridInfo(String name) {
    if (gridCache.isEmpty()) {
      BeeKeeper.getLog().warning("grid cache is empty");
      return;
    }

    if (!BeeUtils.isEmpty(name)) {
      if (isGridCached(name)) {
        BeeGrid grid = gridCache.get(gridKey(name));
        if (grid != null) {
          BeeKeeper.getUi().showGrid(grid.getInfo());
          return;
        } else {
          BeeKeeper.getLog().warning("grid", name, "was not found");
        }
      } else {
        BeeKeeper.getLog().warning("grid", name, "not in cache");
      }
    }

    List<Property> info = Lists.newArrayList();
    for (Map.Entry<String, BeeGrid> entry : gridCache.entrySet()) {
      BeeGrid grid = entry.getValue();
      String cc = (grid == null) ? BeeConst.STRING_MINUS : BeeUtils.toString(grid.getColumnCount());
      info.add(new Property(entry.getKey(), cc));
    }

    BeeKeeper.getUi().showGrid(info, "Grid Name", "Column Count");
  }

  public static Widget simpleGrid(Object data, String... columnLabels) {
    Assert.notNull(data);

    IsTable<?, ?> table = DataUtils.createTable(data, columnLabels);
    Assert.notNull(table);

    int c = table.getNumberOfColumns();
    Assert.isPositive(c);

    int r = table.getNumberOfRows();
    if (r <= 0) {
      BeeKeeper.getLog().warning("data table empty");
      return null;
    }

    CellGrid grid = new CellGrid();

    RowIdColumn idColumn = new RowIdColumn();
    String id = "row-id";
    grid.addColumn(id, -1, idColumn, new TextHeader("Id"));
    grid.setColumnWidth(id, 40);

    CellColumn<?> column;
    for (int i = 0; i < c; i++) {
      column = createColumn(table.getColumn(i), i);
      column.setSortable(true);
      grid.addColumn(table.getColumnLabel(i), i, column, new ColumnHeader(table.getColumn(i)));
    }

    @SuppressWarnings("unused")
    CachedProvider provider = new CachedProvider(grid, null, table);

    grid.setHeaderCellHeight(23);
    grid.setBodyCellHeight(20);
    grid.estimateColumnWidths(table.getRows().getList(), Math.min(r, 20));
    grid.estimateHeaderWidths();

    grid.setRowData(table.getRows().getList());
    grid.setReadOnly(true);

    return grid;
  }
  
  private static Cell<String> createCell(CellType type) {
    Cell<String> cell;

    switch (type) {
      case TEXT_EDIT:
        cell = new EditTextCell();
        break;
      case TEXT_INPUT:
        cell = new TextInputCell();
        break;
      default:
        cell = new TextCell();
    }
    return cell;
  }
  
  private static String gridKey(String name) {
    Assert.notEmpty(name);
    return name.trim().toLowerCase();
  }

  private static boolean isGridCached(String name) {
    if (BeeUtils.isEmpty(name)) {
      return false;
    }
    return gridCache.containsKey(gridKey(name));
  }

  private GridFactory() {
  }
}