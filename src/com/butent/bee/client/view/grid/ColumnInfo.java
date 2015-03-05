package com.butent.bee.client.view.grid;

import com.google.gwt.safecss.shared.SafeStylesBuilder;

import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.cell.HeaderCell;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.render.RenderableColumn;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.style.Font;
import com.butent.bee.client.style.StyleDescriptor;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.value.HasValueType;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.ui.Flexibility;
import com.butent.bee.shared.ui.Flexible;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class ColumnInfo implements HasValueType, Flexible {

  private static final int DEFAULT_MIN_WIDTH = 40;
  private static final int DEFAULT_MAX_WIDTH = 400;

  private static final int AUTO_FIT_MIN_WIDTH = 20;

  private final String columnId;
  private String label;

  private final CellSource source;

  private final AbstractColumn<?> column;
  private final ColumnHeader header;
  private final ColumnFooter footer;

  private final AbstractFilterSupplier filterSupplier;

  private final String dynGroup;

  private int initialWidth = BeeConst.UNDEF;
  private int minWidth = BeeConst.UNDEF;
  private int maxWidth = BeeConst.UNDEF;

  private int headerWidth = BeeConst.UNDEF;
  private int bodyWidth = BeeConst.UNDEF;

  private int resizedWidth = BeeConst.UNDEF;
  private int flexWidth = BeeConst.UNDEF;

  private int autoFitRows = BeeConst.UNDEF;
  private Flexibility flexibility;

  private StyleDescriptor headerStyle;
  private StyleDescriptor bodyStyle;
  private StyleDescriptor footerStyle;

  private ConditionalStyle dynStyles;

  private boolean colReadOnly;

  private boolean cellResizable = true;

  private boolean hidable = true;

  private boolean exportable = true;
  private Double exportWidthFactor;

  public ColumnInfo(String columnId, String label, CellSource source, AbstractColumn<?> column,
      ColumnHeader header) {
    this(columnId, label, source, column, header, null, null, null);
  }

  public ColumnInfo(String columnId, String label, CellSource source, AbstractColumn<?> column,
      ColumnHeader header, ColumnFooter footer, AbstractFilterSupplier filterSupplier,
      String dynGroup) {

    this.columnId = columnId;
    this.label = label;
    this.source = source;

    this.column = column;
    this.header = header;
    this.footer = footer;

    this.filterSupplier = filterSupplier;

    this.dynGroup = dynGroup;
  }

  @Override
  public int clampSize(Orientation orientation, int size) {
    if (Orientation.HORIZONTAL.equals(orientation)) {
      return clampWidth(size);
    } else if (Orientation.VERTICAL.equals(orientation)) {
      Assert.unsupported();
      return size;
    } else {
      return size;
    }
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof ColumnInfo) && columnId.equals(((ColumnInfo) obj).columnId);
  }

  public AbstractColumn<?> getColumn() {
    return column;
  }

  public ConditionalStyle getDynStyles() {
    return dynStyles;
  }

  public Double getExportWidthFactor() {
    return exportWidthFactor;
  }

  @Override
  public Flexibility getFlexibility() {
    return flexibility;
  }

  public ColumnFooter getFooter() {
    return footer;
  }

  public ColumnHeader getHeader() {
    return header;
  }

  @Override
  public int getHypotheticalSize(Orientation orientation, boolean flexible) {
    if (Orientation.HORIZONTAL.equals(orientation)) {
      return flexible ? getWidth(getResizedWidth(), BeeConst.UNDEF) : getWidth();

    } else if (Orientation.VERTICAL.equals(orientation)) {
      Assert.unsupported();
      return BeeConst.UNDEF;

    } else {
      return BeeConst.UNDEF;
    }
  }

  @Override
  public ValueType getValueType() {
    return (getSource() == null) ? getColumn().getValueType() : getSource().getValueType();
  }

  @Override
  public int hashCode() {
    return columnId.hashCode();
  }

  public boolean is(String id) {
    return BeeUtils.same(getColumnId(), id);
  }

  public boolean isExportable() {
    return exportable;
  }

  @Override
  public boolean isFlexible() {
    return getFlexibility() != null || BeeConst.isUndef(getInitialWidth());
  }

  @Override
  public void setFlexibility(Flexibility flexibility) {
    this.flexibility = flexibility;
  }

  @Override
  public boolean updateSize(Orientation orientation, int size) {
    if (Orientation.HORIZONTAL.equals(orientation)) {
      return updateFlexWidth(size);
    } else {
      return false;
    }
  }

  void buildSafeStyles(SafeStylesBuilder stylesBuilder, ComponentType componentType) {
    StyleDescriptor sd = getStyleDescriptor(componentType);
    if (sd == null) {
      return;
    }
    sd.buildSafeStyles(stylesBuilder);
  }

  int clampWidth(int w) {
    return BeeUtils.clamp(w, getLowerWidthBound(), getUpperWidthBound());
  }

  void ensureBodyWidth(int w) {
    if (w > 0) {
      setBodyWidth(Math.max(getBodyWidth(), w));
    }
  }

  int getAutoFitRows() {
    return autoFitRows;
  }

  Font getBodyFont() {
    if (getBodyStyle() == null) {
      return null;
    }
    return getBodyStyle().getFont();
  }

  String getClassName(ComponentType componentType) {
    StyleDescriptor sd = getStyleDescriptor(componentType);
    if (sd == null) {
      return null;
    }
    return sd.getClassName();
  }

  String getColumnId() {
    return columnId;
  }

  String getDynGroup() {
    return dynGroup;
  }

  AbstractFilterSupplier getFilterSupplier() {
    return filterSupplier;
  }

  Font getHeaderFont() {
    if (getHeaderStyle() == null) {
      return null;
    }
    return getHeaderStyle().getFont();
  }

  int getHeaderWidth() {
    return headerWidth;
  }

  String getLabel() {
    return label;
  }

  int getLowerWidthBound() {
    if (getMinWidth() > 0) {
      return getMinWidth();
    } else if (getInitialWidth() <= 0) {
      return DEFAULT_MIN_WIDTH;
    } else {
      return Math.min(DEFAULT_MIN_WIDTH, getInitialWidth());
    }
  }

  int getMinAutoFitWidth() {
    if (getInitialWidth() <= 0) {
      return AUTO_FIT_MIN_WIDTH;
    } else {
      return Math.min(AUTO_FIT_MIN_WIDTH, getInitialWidth());
    }
  }

  List<String> getSortBy() {
    return getColumn().getSortBy();
  }

  CellSource getSource() {
    return source;
  }

  int getUpperWidthBound() {
    if (getMaxWidth() > 0) {
      return getMaxWidth();
    } else if (getInitialWidth() <= 0) {
      return DEFAULT_MAX_WIDTH;
    } else {
      return Math.max(DEFAULT_MAX_WIDTH, getInitialWidth());
    }
  }

  int getWidth() {
    return getWidth(getResizedWidth(), getFlexWidth());
  }

  boolean hasDynGroup(String group) {
    return !BeeUtils.isEmpty(group) && BeeUtils.same(dynGroup, group);
  }

  void initProperties(ColumnDescription columnDescription, GridDescription gridDescription) {
    Assert.notNull(columnDescription);

    if (columnDescription.getWidth() != null) {
      setInitialWidth(columnDescription.getWidth());
    }

    if (columnDescription.getMinWidth() != null) {
      setMinWidth(columnDescription.getMinWidth());
    } else if (gridDescription.getMinColumnWidth() != null) {
      setMinWidth(gridDescription.getMinColumnWidth());
    }

    if (columnDescription.getMaxWidth() != null) {
      setMaxWidth(columnDescription.getMaxWidth());
    } else if (gridDescription.getMaxColumnWidth() != null) {
      setMaxWidth(gridDescription.getMaxColumnWidth());
    }

    String af = BeeUtils.notEmpty(columnDescription.getAutoFit(), gridDescription.getAutoFit());
    if (BeeUtils.isInt(af)) {
      setAutoFitRows(BeeUtils.toInt(af));
    } else if (BeeConst.isTrue(af)) {
      setAutoFitRows(Integer.MAX_VALUE);
    }

    if (columnDescription.getFlexibility() != null) {
      setFlexibility(columnDescription.getFlexibility());
    }

    if (columnDescription.getHeaderStyle() != null) {
      setHeaderStyle(StyleDescriptor.copyOf(columnDescription.getHeaderStyle()));
    }
    if (columnDescription.getBodyStyle() != null) {
      setBodyStyle(StyleDescriptor.copyOf(columnDescription.getBodyStyle()));
    }
    if (columnDescription.getFooterStyle() != null) {
      setFooterStyle(StyleDescriptor.copyOf(columnDescription.getFooterStyle()));
    }

    Boolean cr = columnDescription.getCellResizable();
    if (cr == null) {
      setCellResizable(ValueType.TEXT.equals(getValueType()));
    } else {
      setCellResizable(cr);
    }

    if (BeeUtils.isFalse(columnDescription.getExportable())) {
      setExportable(false);
    }
    if (BeeUtils.isPositive(columnDescription.getExportWidthFactor())) {
      setExportWidthFactor(columnDescription.getExportWidthFactor());
    }
  }

  boolean isActionColumn() {
    return ColType.ACTION == getColumn().getColType();
  }

  boolean isCalculated() {
    return ColType.CALCULATED == getColumn().getColType();
  }

  boolean isCellResizable() {
    return cellResizable;
  }

  boolean isColReadOnly() {
    return colReadOnly;
  }

  boolean isDynamic() {
    return dynGroup != null || isRightsColumn();
  }

  boolean isHidable() {
    return hidable;
  }

  boolean isRenderable() {
    return getColumn() instanceof RenderableColumn;
  }

  boolean isRightsColumn() {
    return ColType.RIGHTS == getColumn().getColType();
  }

  boolean isSelection() {
    return ColType.SELECTION == getColumn().getColType();
  }

  void setBodyFont(String fontDeclaration) {
    if (getBodyStyle() == null) {
      if (!BeeUtils.isEmpty(fontDeclaration)) {
        setBodyStyle(new StyleDescriptor(null, null, fontDeclaration));
      }
    } else {
      getBodyStyle().setFontDeclaration(fontDeclaration);
    }
  }

  void setBodyWidth(int bodyWidth) {
    this.bodyWidth = bodyWidth;
  }

  void setCellResizable(boolean cellResizable) {
    this.cellResizable = cellResizable;
  }

  void setColReadOnly(boolean colReadOnly) {
    this.colReadOnly = colReadOnly;
  }

  void setDynStyles(ConditionalStyle dynStyles) {
    this.dynStyles = dynStyles;
  }

  void setExportable(boolean exportable) {
    this.exportable = exportable;
  }

  void setExportWidthFactor(Double exportWidthFactor) {
    this.exportWidthFactor = exportWidthFactor;
  }

  void setFooterFont(String fontDeclaration) {
    if (getFooterStyle() == null) {
      if (!BeeUtils.isEmpty(fontDeclaration)) {
        setFooterStyle(new StyleDescriptor(null, null, fontDeclaration));
      }
    } else {
      getFooterStyle().setFontDeclaration(fontDeclaration);
    }
  }

  void setHeaderFont(String fontDeclaration) {
    if (getHeaderStyle() == null) {
      if (!BeeUtils.isEmpty(fontDeclaration)) {
        setHeaderStyle(new StyleDescriptor(null, null, fontDeclaration));
      }
    } else {
      getHeaderStyle().setFontDeclaration(fontDeclaration);
    }
  }

  void setHeaderWidth(int headerWidth) {
    this.headerWidth = headerWidth;
  }

  void setHidable(boolean hidable) {
    this.hidable = hidable;
  }

  void setLabel(String label) {
    this.label = label;
    ((HeaderCell) getHeader().getCell()).setCaption(label);
  }

  void setResizedWidth(int resizedWidth) {
    this.resizedWidth = resizedWidth;
  }

  private StyleDescriptor getBodyStyle() {
    return bodyStyle;
  }

  private int getBodyWidth() {
    return bodyWidth;
  }

  private int getFlexWidth() {
    return flexWidth;
  }

  private StyleDescriptor getFooterStyle() {
    return footerStyle;
  }

  private StyleDescriptor getHeaderStyle() {
    return headerStyle;
  }

  private int getInitialWidth() {
    return initialWidth;
  }

  private int getMaxWidth() {
    return maxWidth;
  }

  private int getMinWidth() {
    return minWidth;
  }

  private int getResizedWidth() {
    return resizedWidth;
  }

  private StyleDescriptor getStyleDescriptor(ComponentType componentType) {
    switch (componentType) {
      case HEADER:
        return getHeaderStyle();
      case BODY:
        return getBodyStyle();
      case FOOTER:
        return getFooterStyle();
    }
    return null;
  }

  private int getWidth(int resized, int flex) {
    if (resized > 0) {
      return resized;
    }
    int w = BeeUtils.positive(flex, getInitialWidth());
    if (w <= 0) {
      w = Math.max(getBodyWidth(), getHeaderWidth());
    }
    return clampWidth(w);
  }

  private void setAutoFitRows(int autoFitRows) {
    this.autoFitRows = autoFitRows;
  }

  private void setBodyStyle(StyleDescriptor bodyStyle) {
    this.bodyStyle = bodyStyle;
  }

  private void setFlexWidth(int flexWidth) {
    this.flexWidth = flexWidth;
  }

  private void setFooterStyle(StyleDescriptor footerStyle) {
    this.footerStyle = footerStyle;
  }

  private void setHeaderStyle(StyleDescriptor headerStyle) {
    this.headerStyle = headerStyle;
  }

  private void setInitialWidth(int initialWidth) {
    this.initialWidth = initialWidth;
  }

  private void setMaxWidth(int maxWidth) {
    this.maxWidth = maxWidth;
  }

  private void setMinWidth(int minWidth) {
    this.minWidth = minWidth;
  }

  private boolean updateFlexWidth(int newWidth) {
    if (newWidth != getFlexWidth() || getResizedWidth() >= 0) {
      setFlexWidth(newWidth);
      setResizedWidth(BeeConst.UNDEF);
      return true;
    } else {
      return false;
    }
  }
}
