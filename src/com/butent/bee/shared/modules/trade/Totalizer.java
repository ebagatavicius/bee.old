package com.butent.bee.shared.modules.trade;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowPredicate;
import com.butent.bee.shared.data.RowToDouble;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Totalizer {

  private static final String[] DOUBLE_COLUMNS = new String[] {
      COL_TRADE_AMOUNT, COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
      COL_TRADE_VAT, COL_TRADE_DISCOUNT};

  private static final String[] BOOLEAN_COLUMNS = new String[] {
      COL_TRADE_VAT_PLUS, COL_TRADE_VAT_PERC};

  private final Map<String, RowToDouble> functions = new HashMap<>();
  private final Map<String, RowPredicate> predicates = new HashMap<>();

  public Totalizer(List<? extends IsColumn> columns) {
    for (String colName : DOUBLE_COLUMNS) {
      int idx = DataUtils.getColumnIndex(colName, columns);

      if (!BeeConst.isUndef(idx)) {
        functions.put(colName, RowToDouble.at(idx));
      }
    }

    for (String colName : BOOLEAN_COLUMNS) {
      int idx = DataUtils.getColumnIndex(colName, columns);

      if (!BeeConst.isUndef(idx)) {
        predicates.put(colName, RowPredicate.isTrue(idx));
      }
    }
  }

  public boolean dependsOnSource(String source) {
    return functions.containsKey(source) || predicates.containsKey(source);
  }

  public Double getDiscount(IsRow row) {
    if (row == null) {
      return null;
    } else {
      return getDiscount(row, getAmount(row));
    }
  }

  public Double getTotal(IsRow row) {
    if (row == null) {
      return null;
    }

    Double amount = getAmount(row);
    if (!BeeUtils.isDouble(amount)) {
      return null;
    }

    Double discount = getDiscount(row, amount);
    if (BeeUtils.isDouble(discount)) {
      amount -= discount;
    }

    if (isTrue(COL_TRADE_VAT_PLUS, row)) {
      Double vat = getVat(row, amount);
      if (BeeUtils.isDouble(vat)) {
        amount += vat;
      }
    }

    return amount;
  }

  public Double getVat(IsRow row) {
    if (row == null) {
      return null;

    } else {
      Double base = getAmount(row);

      if (base == null) {
        return null;

      } else {
        Double discount = getDiscount(row, base);
        if (BeeUtils.isDouble(discount)) {
          base -= discount;
        }

        return getVat(row, base);
      }
    }
  }

  public void setAmountFuction(RowToDouble function) {
    setFunction(COL_TRADE_AMOUNT, function);
  }

  public void setDiscountFuction(RowToDouble function) {
    setFunction(COL_TRADE_DISCOUNT, function);
  }

  public void setPriceFuction(RowToDouble function) {
    setFunction(COL_TRADE_ITEM_PRICE, function);
  }

  public void setQuantityFuction(RowToDouble function) {
    setFunction(COL_TRADE_ITEM_QUANTITY, function);
  }

  public void setVatFuction(RowToDouble function) {
    setFunction(COL_TRADE_VAT, function);
  }

  public void setVatPercentPredicate(RowPredicate predicate) {
    setPredicate(COL_TRADE_VAT_PERC, predicate);
  }

  public void setVatPlusPredicate(RowPredicate predicate) {
    setPredicate(COL_TRADE_VAT_PLUS, predicate);
  }

  private Double getAmount(IsRow row) {
    if (row == null) {
      return null;

    } else if (functions.containsKey(COL_TRADE_AMOUNT)) {
      return getNumber(COL_TRADE_AMOUNT, row);

    } else if (functions.containsKey(COL_TRADE_ITEM_PRICE)) {
      Double price = getNumber(COL_TRADE_ITEM_PRICE, row);

      if (functions.containsKey(COL_TRADE_ITEM_QUANTITY) && price != null) {
        Double qty = getNumber(COL_TRADE_ITEM_QUANTITY, row);
        return (qty == null) ? null : (price * qty);
      } else {
        return price;
      }

    } else {
      return null;
    }
  }

  private Double getDiscount(IsRow row, Double base) {
    if (base != null && functions.containsKey(COL_TRADE_DISCOUNT)) {
      return BeeUtils.percent(base, getNumber(COL_TRADE_DISCOUNT, row));
    } else {
      return null;
    }
  }

  private Double getNumber(String key, IsRow row) {
    RowToDouble function = functions.get(key);
    return (function == null) ? null : function.apply(row);
  }

  private Double getVat(IsRow row, Double base) {
    if (base != null && functions.containsKey(COL_TRADE_VAT)) {
      Double vat = getNumber(COL_TRADE_VAT, row);

      if (vat != null && isTrue(COL_TRADE_VAT_PERC, row)) {
        if (isFalse(COL_TRADE_VAT_PLUS, row)) {
          return BeeUtils.percentInclusive(base, vat);
        } else {
          return BeeUtils.percent(base, vat);
        }

      } else {
        return vat;
      }

    } else {
      return null;
    }
  }

  private boolean isFalse(String key, IsRow row) {
    RowPredicate predicate = predicates.get(key);
    return predicate != null && !predicate.apply(row);
  }

  private boolean isTrue(String key, IsRow row) {
    RowPredicate predicate = predicates.get(key);
    return predicate != null && predicate.apply(row);
  }

  private void setFunction(String key, RowToDouble function) {
    if (function == null) {
      functions.remove(key);
    } else {
      functions.put(key, function);
    }
  }

  private void setPredicate(String key, RowPredicate predicate) {
    if (predicate == null) {
      predicates.remove(key);
    } else {
      predicates.put(key, predicate);
    }
  }
}
