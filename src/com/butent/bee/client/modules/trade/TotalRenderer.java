package com.butent.bee.client.modules.trade;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.ProvidesGridColumnRenderer;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.HasRowValue;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.DecimalValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.modules.trade.Totalizer;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class TotalRenderer extends AbstractCellRenderer implements HasRowValue {

  public static class Provider implements ProvidesGridColumnRenderer {
    @Override
    public AbstractCellRenderer getRenderer(String columnName,
        List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
        CellSource cellSource) {

      return new TotalRenderer(dataColumns);
    }
  }

  private final Totalizer totalizer;

  public TotalRenderer(List<? extends IsColumn> columns) {
    super(null);
    this.totalizer = new Totalizer(columns);
  }

  @Override
  public boolean dependsOnSource(String source) {
    return totalizer.dependsOnSource(source);
  }

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    Double v = evaluate(row);
    return (v == null) ? null : new XCell(cellIndex, v, styleRef);
  }

  public Double getDiscount(IsRow row) {
    return totalizer.getDiscount(row);
  }

  @Override
  public Value getRowValue(IsRow row) {
    Double v = evaluate(row);
    return (v == null) ? null : DecimalValue.of(v);
  }

  public Double getTotal(IsRow row) {
    return totalizer.getTotal(row);
  }

  public Totalizer getTotalizer() {
    return totalizer;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.DECIMAL;
  }

  public Double getVat(IsRow row) {
    return totalizer.getVat(row);
  }

  @Override
  public Integer initExport(XSheet sheet) {
    if (sheet == null) {
      return null;

    } else {
      XStyle style = XStyle.right();
      style.setFormat(Format.getDecimalPattern(2));
      style.setFontRef(sheet.registerFont(XFont.bold()));

      return sheet.registerStyle(style);
    }
  }

  @Override
  public String render(IsRow row) {
    Double v = evaluate(row);
    return (v == null) ? null : BeeUtils.toString(v, 2);
  }

  protected Double evaluate(IsRow row) {
    return getTotal(row);
  }
}
