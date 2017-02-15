package com.butent.bee.client.render;

import com.google.gwt.text.shared.AbstractRenderer;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.HasValueType;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.i18n.DateOrdering;

public abstract class AbstractCellRenderer extends AbstractRenderer<IsRow> implements HasValueType {

  private final CellSource cellSource;

  public AbstractCellRenderer(CellSource cellSource) {
    this.cellSource = cellSource;
  }

  /**
   * @param row
   * @param cellIndex
   * @param styleRef
   * @param sheet
   */
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    return null;
  }

  public VerticalAlign getDefaultVerticalAlign() {
    return null;
  }

  public ValueType getExportType() {
    return getValueType();
  }

  @Override
  public ValueType getValueType() {
    return (cellSource == null) ? null : cellSource.getValueType();
  }

  /**
   * @param sheet used by subclasses
   */
  public Integer initExport(XSheet sheet) {
    return null;
  }

  protected CellSource getCellSource() {
    return cellSource;
  }

  protected Double getDouble(IsRow row) {
    if (row == null || cellSource == null) {
      return null;
    } else {
      return cellSource.getDouble(row);
    }
  }

  protected Integer getInteger(IsRow row) {
    if (row == null || cellSource == null) {
      return null;
    } else {
      return cellSource.getInteger(row);
    }
  }

  protected Long getLong(IsRow row) {
    if (row == null || cellSource == null) {
      return null;
    } else {
      return cellSource.getLong(row);
    }
  }

  protected int getScale() {
    if (cellSource == null) {
      return BeeConst.UNDEF;
    } else {
      return cellSource.getScale();
    }
  }

  protected String getString(IsRow row) {
    if (row == null || cellSource == null) {
      return null;
    } else {
      return cellSource.getString(row);
    }
  }

  protected Value getValue(IsRow row) {
    if (row == null || cellSource == null) {
      return null;
    } else {
      return cellSource.getValue(row);
    }
  }

  protected Value parse(String value, boolean parseDates, DateOrdering dateOrdering) {
    if (value == null || value.isEmpty() || cellSource == null) {
      return null;
    } else {
      return Value.parseValue(getValueType(), value, parseDates, dateOrdering);
    }
  }
}
