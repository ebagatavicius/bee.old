package com.butent.bee.client.modules.trade;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.ProvidesGridColumnRenderer;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
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
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TotalRenderer extends AbstractCellRenderer implements HasRowValue {

  public static class Provider implements ProvidesGridColumnRenderer {
    @Override
    public AbstractCellRenderer getRenderer(String columnName,
        List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
        CellSource cellSource) {

      return new TotalRenderer(dataColumns);
    }
  }

  private static final String[] COL_NAMES = new String[] {
    COL_TRADE_AMOUNT,
    COL_TRADE_ITEM_QUANTITY,
    COL_TRADE_ITEM_PRICE,
    COL_TRADE_VAT_PLUS,
    COL_TRADE_VAT,
    COL_TRADE_VAT_PERC};

  private final Map<String, Integer> data = new HashMap<>();

  public TotalRenderer(List<? extends IsColumn> columns) {
    super(null);

    for (String colName : COL_NAMES) {
      int idx = DataUtils.getColumnIndex(colName, columns);

      if (!BeeConst.isUndef(idx)) {
        data.put(colName, idx);
      }
    }
  }

  @Override
  public boolean dependsOnSource(String source) {
    return !BeeUtils.isEmpty(source) && data.containsKey(source); 
  }

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    Double total = getTotal(row);
    return (total == null) ? null : new XCell(cellIndex, total, styleRef);
  }

  @Override
  public Value getRowValue(IsRow row) {
    if (row == null) {
      return null;
    } else {
      return DecimalValue.of(getTotal(row));
    }
  }

  public Double getTotal(IsRow row) {
    if (row == null) {
      return null;
    }

    double total = BeeConst.DOUBLE_ZERO;

    if (data.containsKey(COL_TRADE_AMOUNT)) {
      total = BeeUtils.unbox(row.getDouble(data.get(COL_TRADE_AMOUNT)));

    } else if (data.containsKey(COL_TRADE_ITEM_PRICE)) {
      total = BeeUtils.unbox(row.getDouble(data.get(COL_TRADE_ITEM_PRICE)));

      if (data.containsKey(COL_TRADE_ITEM_QUANTITY)) {
        total *= BeeUtils.unbox(row.getDouble(data.get(COL_TRADE_ITEM_QUANTITY)));
      }
    }
    if (data.containsKey(COL_TRADE_VAT_PLUS)
        && BeeUtils.isTrue(row.getBoolean(data.get(COL_TRADE_VAT_PLUS)))) {

      if (data.containsKey(COL_TRADE_VAT)) {
        double vat = BeeUtils.unbox(row.getDouble(data.get(COL_TRADE_VAT)));

        if (data.containsKey(COL_TRADE_VAT_PERC)
            && BeeUtils.isTrue(row.getBoolean(data.get(COL_TRADE_VAT_PERC)))) {
          vat = total / 100 * vat;
        }
        total += vat;
      }
    }

    return total;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.DECIMAL;
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
    Double total = getTotal(row);
    return (total == null) ? null : BeeUtils.toString(total, 2);
  }
}
