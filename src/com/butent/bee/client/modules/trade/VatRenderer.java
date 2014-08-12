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

public class VatRenderer extends AbstractCellRenderer implements HasRowValue {

  public static class Provider implements ProvidesGridColumnRenderer {
    @Override
    public AbstractCellRenderer getRenderer(String columnName,
        List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
        CellSource cellSource) {

      return new VatRenderer(dataColumns);
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

  public VatRenderer(List<? extends IsColumn> columns) {
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
    Double vat = getVat(row);
    return (vat == null) ? null : new XCell(cellIndex, vat, styleRef);
  }

  @Override
  public Value getRowValue(IsRow row) {
    Double vat = getVat(row);
    return (vat == null) ? null : DecimalValue.of(vat);
  }

  public Double getVat(IsRow row) {
    if (row == null) {
      return null;
    }

    Double result = null;

    Double vat;
    if (data.containsKey(COL_TRADE_VAT)) {
      vat = row.getDouble(data.get(COL_TRADE_VAT));
    } else {
      vat = null;
    }

    if (BeeUtils.isDouble(vat)) {
      if (data.containsKey(COL_TRADE_VAT_PERC)
          && BeeUtils.isTrue(row.getBoolean(data.get(COL_TRADE_VAT_PERC)))) {

        Double amount = null;

        if (data.containsKey(COL_TRADE_AMOUNT)) {
          amount = row.getDouble(data.get(COL_TRADE_AMOUNT));

        } else if (data.containsKey(COL_TRADE_ITEM_PRICE)) {
          Double price = row.getDouble(data.get(COL_TRADE_ITEM_PRICE));

          if (data.containsKey(COL_TRADE_ITEM_QUANTITY)) {
            Double qty = row.getDouble(data.get(COL_TRADE_ITEM_QUANTITY));
            if (BeeUtils.isDouble(price) && BeeUtils.isDouble(qty)) {
              amount = price * qty;
            }

          } else {
            amount = price;
          }
        }

        if (BeeUtils.isDouble(amount)) {
          result = amount / 100 * vat;
        }

      } else {
        result = vat;
      }
    }

    return result;
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
    Double vat = getVat(row);
    return (vat == null) ? null : BeeUtils.toString(vat, 2);
  }
}
