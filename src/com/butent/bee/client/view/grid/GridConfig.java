package com.butent.bee.client.view.grid;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.Flexibility;
import com.butent.bee.shared.ui.GridComponentDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.StyleDeclaration;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GridConfig {

  private static final BeeLogger logger = LogUtils.getLogger(GridConfig.class);

  private static final List<BeeColumn> dataColumns = new ArrayList<>();

  private static int userIndex;
  private static int keyIndex;
  private static int captionIndex;
  private static int columnsIndex;
  private static int orderIndex;

  private static int headerHeightIndex;
  private static int headerStyleIndex;
  private static int headerFontIndex;
  private static int headerPaddingIndex;
  private static int headerBorderIndex;
  private static int headerMarginIndex;

  private static int rowHeightIndex;
  private static int bodyStyleIndex;
  private static int bodyFontIndex;
  private static int bodyPaddingIndex;
  private static int bodyBorderIndex;
  private static int bodyMarginIndex;

  private static int footerHeightIndex;
  private static int footerStyleIndex;
  private static int footerFontIndex;
  private static int footerPaddingIndex;
  private static int footerBorderIndex;
  private static int footerMarginIndex;

  private static int autoFitIndex;

  private static int minColumnWidthIndex;

  private static int maxColumnWidthIndex;

  private static int flexGrowIndex;

  private static int flexShrinkIndex;

  private static int flexBasisIndex;

  private static int flexUnitIndex;

  static void ensureFields(List<BeeColumn> columns) {
    if (dataColumns.isEmpty()) {
      dataColumns.addAll(columns);

      List<String> names = DataUtils.getColumnNames(columns);

      userIndex = GridUtils.getIndex(names, GridDescription.COL_GRID_SETTING_USER);
      keyIndex = GridUtils.getIndex(names, GridDescription.COL_GRID_SETTING_KEY);
      captionIndex = GridUtils.getIndex(names, "Caption");
      columnsIndex = GridUtils.getIndex(names, "Columns");
      orderIndex = GridUtils.getIndex(names, "Order");

      headerHeightIndex = GridUtils.getIndex(names, "HeaderHeight");
      headerStyleIndex = GridUtils.getIndex(names, "HeaderStyle");
      headerFontIndex = GridUtils.getIndex(names, "HeaderFont");
      headerPaddingIndex = GridUtils.getIndex(names, "HeaderPadding");
      headerBorderIndex = GridUtils.getIndex(names, "HeaderBorderWidth");
      headerMarginIndex = GridUtils.getIndex(names, "HeaderMargin");

      rowHeightIndex = GridUtils.getIndex(names, "RowHeight");
      bodyStyleIndex = GridUtils.getIndex(names, "BodyStyle");
      bodyFontIndex = GridUtils.getIndex(names, "BodyFont");
      bodyPaddingIndex = GridUtils.getIndex(names, "BodyPadding");
      bodyBorderIndex = GridUtils.getIndex(names, "BodyBorderWidth");
      bodyMarginIndex = GridUtils.getIndex(names, "BodyMargin");

      footerHeightIndex = GridUtils.getIndex(names, "FooterHeight");
      footerStyleIndex = GridUtils.getIndex(names, "FooterStyle");
      footerPaddingIndex = GridUtils.getIndex(names, "FooterPadding");
      footerFontIndex = GridUtils.getIndex(names, "FooterFont");
      footerBorderIndex = GridUtils.getIndex(names, "FooterBorderWidth");
      footerMarginIndex = GridUtils.getIndex(names, "FooterMargin");

      autoFitIndex = GridUtils.getIndex(names, "AutoFit");
      minColumnWidthIndex = GridUtils.getIndex(names, "MinColumnWidth");
      maxColumnWidthIndex = GridUtils.getIndex(names, "MaxColumnWidth");

      flexGrowIndex = GridUtils.getIndex(names, "FlexGrow");
      flexShrinkIndex = GridUtils.getIndex(names, "FlexShrink");
      flexBasisIndex = GridUtils.getIndex(names, "FlexBasis");
      flexUnitIndex = GridUtils.getIndex(names, "FlexBasisUnit");
    }
  }

  static int getColumnsIndex() {
    ensureFields();
    return columnsIndex;
  }

  static List<BeeColumn> getDataColumns() {
    ensureFields();
    return dataColumns;
  }

  static int getFooterHeightIndex() {
    ensureFields();
    return footerHeightIndex;
  }

  static int getHeaderHeightIndex() {
    ensureFields();
    return headerHeightIndex;
  }

  static int getKeyIndex() {
    ensureFields();
    return keyIndex;
  }

  static int getOrderIndex() {
    ensureFields();
    return orderIndex;
  }

  static int getRowHeightIndex() {
    ensureFields();
    return rowHeightIndex;
  }

  static int getUserIndex() {
    ensureFields();
    return userIndex;
  }

  private static void ensureFields() {
    if (dataColumns.isEmpty()) {
      ensureFields(Data.getColumns(GridDescription.VIEW_GRID_SETTINGS));
    }
  }

  private static GridComponentDescription fuseCompnent(GridComponentDescription component,
      Integer height, String style, String font, String padding, String border, String margin) {

    if (!BeeUtils.isPositive(height) && BeeUtils.allEmpty(style, font, padding, border, margin)) {
      return component;
    }

    GridComponentDescription result;
    if (component == null) {
      result = new GridComponentDescription(height);
    } else {
      result = component.copy();
      if (BeeUtils.isPositive(height)) {
        result.setHeight(height);
      }
    }

    if (!BeeUtils.allEmpty(style, font)) {
      result.setStyle(StyleDeclaration.fuse(result.getStyle(), null, style, font));
    }

    if (!BeeUtils.isEmpty(padding)) {
      result.setPadding(padding);
    }
    if (!BeeUtils.isEmpty(border)) {
      result.setBorderWidth(border);
    }
    if (!BeeUtils.isEmpty(margin)) {
      result.setMargin(margin);
    }

    return result;
  }

  private BeeRow row;

  final Map<String, ColumnConfig> columnSettings = new HashMap<>();

  GridConfig(BeeRow row) {
    this.row = row;
  }

  void applyTo(GridDescription gridDescription) {
    String caption = getCaption();
    if (!BeeUtils.isEmpty(caption)) {
      gridDescription.setCaption(caption);
    }

    String order = getOrder();
    if (!BeeUtils.isEmpty(order)) {
      gridDescription.setOrder(Order.restore(order));
    }

    gridDescription.setHeader(fuseCompnent(gridDescription.getHeader(), getHeaderHeight(),
        getHeaderStyle(), getHeaderFont(), getHeaderPadding(), getHeaderBorderWidth(),
        getHeaderMargin()));

    gridDescription.setBody(fuseCompnent(gridDescription.getBody(), getRowHeight(),
        getBodyStyle(), getBodyFont(), getBodyPadding(), getBodyBorderWidth(), getBodyMargin()));

    gridDescription.setFooter(fuseCompnent(gridDescription.getFooter(), getFooterHeight(),
        getFooterStyle(), getFooterFont(), getFooterPadding(), getFooterBorderWidth(),
        getFooterMargin()));

    Boolean autoFit = getAutoFit();
    if (BeeUtils.isTrue(autoFit)) {
      gridDescription.setAutoFit(BeeConst.STRING_TRUE);
    }

    Integer minColumnWidth = getMinColumnWidth();
    if (BeeUtils.isPositive(minColumnWidth)) {
      gridDescription.setMinColumnWidth(minColumnWidth);
    }

    Integer maxColumnWidth = getMaxColumnWidth();
    if (BeeUtils.isPositive(maxColumnWidth)) {
      gridDescription.setMaxColumnWidth(maxColumnWidth);
    }

    Flexibility flexibility = Flexibility.maybeCreate(getFlexGrow(), getFlexShrink(),
        getFlexBasis(), getFlexBasisUnit());
    if (flexibility != null) {
      if (gridDescription.getFlexibility() == null) {
        gridDescription.setFlexibility(flexibility);
      } else {
        gridDescription.getFlexibility().merge(flexibility);
      }
    }

    List<String> visibleColumnNames = getColumns();

    if (!visibleColumnNames.isEmpty() || !columnSettings.isEmpty()) {
      List<String> names = new ArrayList<>();
      for (ColumnDescription columnDescription : gridDescription.getColumns()) {
        names.add(columnDescription.getId());
      }

      for (Map.Entry<String, ColumnConfig> entry : columnSettings.entrySet()) {
        int index = names.indexOf(entry.getKey());
        if (index >= 0) {
          entry.getValue().applyTo(gridDescription.getColumns().get(index));
        } else {
          logger.warning("settings column not found:", entry.getKey());
        }
      }

      if (!visibleColumnNames.isEmpty()) {
        List<Integer> indexes = new ArrayList<>();

        for (String name : visibleColumnNames) {
          int index = names.indexOf(name);

          if (index >= 0) {
            indexes.add(index);
          } else {
            logger.warning("visible column not found:", name);
          }
        }

        if (!indexes.isEmpty()) {
          List<ColumnDescription> columnDescriptions = new ArrayList<>();

          for (int index : indexes) {
            ColumnDescription columnDescription = gridDescription.getColumns().get(index);
            if (BeeUtils.isFalse(columnDescription.getVisible())) {
              columnDescription.setVisible(null);
            }

            columnDescriptions.add(columnDescription);
          }

          int cc = names.size();
          if (columnDescriptions.size() < cc) {
            for (int i = 0; i < cc; i++) {
              if (!indexes.contains(i)) {
                ColumnDescription columnDescription = gridDescription.getColumns().get(i);
                if (columnDescription.getVisible() == null
                    && !BeeUtils.isTrue(columnDescription.getDynamic())) {
                  columnDescription.setVisible(false);
                }

                columnDescriptions.add(columnDescription);
              }
            }
          }

          gridDescription.getColumns().clear();
          gridDescription.getColumns().addAll(columnDescriptions);
        }
      }

      for (ColumnDescription columnDescription : gridDescription.getColumns()) {
        ColumnConfig columnConfig = columnSettings.get(columnDescription.getId());
        if (columnConfig != null) {
          columnConfig.applyTo(columnDescription);
        }
      }
    }
  }

  ColumnConfig findColumnConfig(long id) {
    for (ColumnConfig columnConfig : columnSettings.values()) {
      if (columnConfig.getRowId() == id) {
        return columnConfig;
      }
    }
    return null;
  }

  BeeRow getRow() {
    return row;
  }

  long getRowId() {
    return row.getId();
  }

  boolean hasVisibleColumns() {
    ensureFields();
    return !BeeUtils.isEmpty(row.getString(columnsIndex));
  }

  boolean isEmpty() {
    if (!columnSettings.isEmpty()) {
      for (ColumnConfig columnConfig : columnSettings.values()) {
        if (!columnConfig.isEmpty()) {
          return false;
        }
      }
    }

    return getCaption() == null
        && getColumns() == null && getOrder() == null
        && getHeaderHeight() == null && getHeaderStyle() == null
        && getHeaderFont() == null && getHeaderPadding() == null
        && getHeaderBorderWidth() == null && getHeaderMargin() == null
        && getRowHeight() == null && getBodyStyle() == null
        && getBodyFont() == null && getBodyPadding() == null
        && getBodyBorderWidth() == null && getBodyMargin() == null
        && getFooterHeight() == null && getFooterStyle() == null
        && getFooterFont() == null && getFooterPadding() == null
        && getFooterBorderWidth() == null && getFooterMargin() == null
        && getAutoFit() == null
        && getMinColumnWidth() == null && getMaxColumnWidth() == null
        && getFlexGrow() == null && getFlexShrink() == null
        && getFlexBasis() == null && getFlexBasisUnit() == null;
  }

  boolean maybeRemoveColumn(long id) {
    String key = findColumnKey(id);

    if (key == null) {
      return false;
    } else {
      columnSettings.remove(key);
      return true;
    }
  }

  void saveColumnSetting(String name, int index, String value) {
    Assert.notEmpty(name);
    Assert.isIndex(ColumnConfig.getDataColumns(), index);

    final String columnName = name.trim();
    final BeeColumn dataColumn = ColumnConfig.getDataColumns().get(index);
    final String newValue = GridUtils.normalizeValue(value);

    ColumnConfig columnConfig = columnSettings.get(columnName);

    if (columnConfig == null) {
      if (newValue != null) {
        List<BeeColumn> columns = new ArrayList<>();
        columns.add(ColumnConfig.getDataColumns().get(ColumnConfig.getGridIndex()));
        columns.add(ColumnConfig.getDataColumns().get(ColumnConfig.getNameIndex()));
        columns.add(dataColumn);

        List<String> values = Queries.asList(row.getId(), columnName, newValue);

        Queries.insert(ColumnDescription.VIEW_COLUMN_SETTINGS, columns, values, null,
            new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
                columnSettings.put(columnName, new ColumnConfig(result));
                logger.debug("created column settings:", columnName, dataColumn.getId(), newValue);
              }
            });
      }

    } else if (columnConfig.setValue(index, newValue)) {

      Queries.update(ColumnDescription.VIEW_COLUMN_SETTINGS,
          Filter.compareId(columnConfig.getRowId()), dataColumn.getId(),
          new TextValue(newValue), new Queries.IntCallback() {
            @Override
            public void onSuccess(Integer result) {
              if (BeeUtils.unbox(result) == 1) {
                logger.debug("updated column settings:", columnName, dataColumn.getId(), newValue);
              } else {
                logger.warning("could not update column settings:", result);
                logger.warning(columnName, dataColumn.getId(), newValue);
              }
            }
          });
    }
  }

  void setRow(BeeRow row) {
    this.row = row;
  }

  boolean setValue(int index, String value) {
    if (!BeeUtils.equalsTrim(row.getString(index), value)) {
      row.setValue(index, value);
      return true;
    } else {
      return false;
    }
  }

  private String findColumnKey(long id) {
    for (Map.Entry<String, ColumnConfig> entry : columnSettings.entrySet()) {
      if (entry.getValue().getRowId() == id) {
        return entry.getKey();
      }
    }
    return null;
  }

  private Boolean getAutoFit() {
    ensureFields();
    return row.getBoolean(autoFitIndex);
  }

  private String getBodyBorderWidth() {
    ensureFields();
    return row.getString(bodyBorderIndex);
  }

  private String getBodyFont() {
    ensureFields();
    return row.getString(bodyFontIndex);
  }

  private String getBodyMargin() {
    ensureFields();
    return row.getString(bodyMarginIndex);
  }

  private String getBodyPadding() {
    ensureFields();
    return row.getString(bodyPaddingIndex);
  }

  private String getBodyStyle() {
    ensureFields();
    return row.getString(bodyStyleIndex);
  }

  private String getCaption() {
    ensureFields();
    return row.getString(captionIndex);
  }

  private List<String> getColumns() {
    ensureFields();
    return NameUtils.toList(row.getString(columnsIndex));
  }

  private Integer getFlexBasis() {
    ensureFields();
    return row.getInteger(flexBasisIndex);
  }

  private String getFlexBasisUnit() {
    ensureFields();
    return row.getString(flexUnitIndex);
  }

  private Integer getFlexGrow() {
    ensureFields();
    return row.getInteger(flexGrowIndex);
  }

  private Integer getFlexShrink() {
    ensureFields();
    return row.getInteger(flexShrinkIndex);
  }

  private String getFooterBorderWidth() {
    ensureFields();
    return row.getString(footerBorderIndex);
  }

  private String getFooterFont() {
    ensureFields();
    return row.getString(footerFontIndex);
  }

  private Integer getFooterHeight() {
    ensureFields();
    return row.getInteger(footerHeightIndex);
  }

  private String getFooterMargin() {
    ensureFields();
    return row.getString(footerMarginIndex);
  }

  private String getFooterPadding() {
    ensureFields();
    return row.getString(footerPaddingIndex);
  }

  private String getFooterStyle() {
    ensureFields();
    return row.getString(footerStyleIndex);
  }

  private String getHeaderBorderWidth() {
    ensureFields();
    return row.getString(headerBorderIndex);
  }

  private String getHeaderFont() {
    ensureFields();
    return row.getString(headerFontIndex);
  }

  private Integer getHeaderHeight() {
    ensureFields();
    return row.getInteger(headerHeightIndex);
  }

  private String getHeaderMargin() {
    ensureFields();
    return row.getString(headerMarginIndex);
  }

  private String getHeaderPadding() {
    ensureFields();
    return row.getString(headerPaddingIndex);
  }

  private String getHeaderStyle() {
    ensureFields();
    return row.getString(headerStyleIndex);
  }

  private Integer getMaxColumnWidth() {
    ensureFields();
    return row.getInteger(maxColumnWidthIndex);
  }

  private Integer getMinColumnWidth() {
    ensureFields();
    return row.getInteger(minColumnWidthIndex);
  }

  private String getOrder() {
    ensureFields();
    return row.getString(orderIndex);
  }

  private Integer getRowHeight() {
    ensureFields();
    return row.getInteger(rowHeightIndex);
  }
}
