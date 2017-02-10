package com.butent.bee.client.grid.column;

import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.i18n.HasDateTimeFormat;
import com.butent.bee.client.i18n.HasNumberFormat;
import com.butent.bee.client.output.Exporter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasPrecision;
import com.butent.bee.shared.HasScale;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Enables using columns which contain results of calculations with other columns.
 */

public class CalculatedColumn extends AbstractColumn<String> implements HasDateTimeFormat,
    HasNumberFormat, HasPrecision, HasScale, HasCellRenderer {

  private final ValueType valueType;
  private AbstractCellRenderer renderer;

  private DateTimeFormat dateTimeFormat;
  private NumberFormat numberFormat;

  private int precision = BeeConst.UNDEF;
  private int scale = BeeConst.UNDEF;

  public CalculatedColumn(AbstractCell<String> cell, ValueType valueType,
      AbstractCellRenderer renderer) {

    super(cell);

    this.valueType = valueType;
    this.renderer = renderer;

    UiHelper.setDefaultHorizontalAlignment(this, valueType);
    UiHelper.setDefaultWhiteSpace(this, valueType);
  }

  @Override
  public XCell export(CellContext context, Integer styleRef, XSheet sheet) {
    if (context == null || context.getRow() == null || getRenderer() == null) {
      return null;
    }

    XCell xc = getRenderer().export(context.getRow(), context.getColumnIndex(), styleRef, sheet);
    if (xc != null) {
      return xc;
    }

    String value = getRenderer().render(context.getRow());
    ValueType type = BeeUtils.nvl(getRenderer().getExportType(), getValueType());

    if (ValueType.isDateOrDateTime(type) && BeeUtils.isDigit(value)) {
      value = format(value, type);
    }

    return Exporter.createCell(value, type, context.getColumnIndex(), styleRef);
  }

  @Override
  public ColType getColType() {
    return ColType.CALCULATED;
  }

  @Override
  public DateTimeFormat getDateTimeFormat() {
    return dateTimeFormat;
  }

  @Override
  public NumberFormat getNumberFormat() {
    return numberFormat;
  }

  @Override
  public int getPrecision() {
    return precision;
  }

  @Override
  public AbstractCellRenderer getRenderer() {
    return renderer;
  }

  @Override
  public int getScale() {
    return scale;
  }

  @Override
  public String getString(CellContext context) {
    if (context.getRow() == null || getRenderer() == null) {
      return null;
    } else {
      return getRenderer().render(context.getRow());
    }
  }

  @Override
  public String getStyleSuffix() {
    return (getValueType() == null) ? "calc" : ("calc-" + getValueType().getTypeCode());
  }

  @Override
  public String getValue(IsRow object) {
    return null;
  }

  @Override
  public ValueType getValueType() {
    return valueType;
  }

  @Override
  public String render(CellContext context) {
    String value = getString(context);

    if (!BeeUtils.isEmpty(value)) {
      return getCell().render(context, format(value, getValueType()));
    } else {
      return null;
    }
  }

  @Override
  public void setDateTimeFormat(DateTimeFormat format) {
    this.dateTimeFormat = format;
  }

  @Override
  public void setNumberFormat(NumberFormat format) {
    this.numberFormat = format;
  }

  @Override
  public void setPrecision(int precision) {
    this.precision = precision;
  }

  @Override
  public void setRenderer(AbstractCellRenderer renderer) {
    this.renderer = renderer;
  }

  @Override
  public void setScale(int scale) {
    this.scale = scale;
  }

  private String format(String value, ValueType type) {
    return Format.render(value, type, getDateTimeFormat(), getNumberFormat(), getScale());
  }
}
