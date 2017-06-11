package com.butent.bee.shared.modules.trade;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowPredicate;
import com.butent.bee.shared.data.RowToDouble;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Totalizer {

  private static final String COL_TRADE_VAT_INCLUSIVE = "VatInclusive";

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
    int idx = DataUtils.getColumnIndex(COL_TRADE_DISCOUNT_PERC, columns);

    setDiscountPercentPredicate(BeeConst.isUndef(idx)
        ? (RowPredicate) isRow -> true : RowPredicate.isTrue(idx));

    setVatInclusivePredicate(input -> !isVatPlus(input));
    int index = DataUtils.getColumnIndex(COL_TRADE_DOCUMENT_VAT_MODE, columns);

    if (!BeeConst.isUndef(index)) {
      idx = DataUtils.getColumnIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT, columns);

      if (!BeeConst.isUndef(idx)) {
        setDiscountPercentPredicate(RowPredicate.isTrue(idx));
      }
      idx = DataUtils.getColumnIndex(COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT, columns);

      if (!BeeConst.isUndef(idx)) {
        setVatPercentPredicate(RowPredicate.isTrue(idx));
      }
      setVatPlusPredicate(input -> input != null && Objects.equals(TradeVatMode.PLUS,
          EnumUtils.getEnumByIndex(TradeVatMode.class, input.getInteger(index))));
      setVatInclusivePredicate(input -> input != null && Objects.equals(TradeVatMode.INCLUSIVE,
          EnumUtils.getEnumByIndex(TradeVatMode.class, input.getInteger(index))));
      predicates.put(COL_TRADE_DOCUMENT_VAT_MODE, RowPredicate.notNull(index));
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

  public Double getDiscount(IsRow row, Double base) {
    Double discount = null;

    if (base != null) {
      discount = getNumber(COL_TRADE_DISCOUNT, row);

      if (discount != null && isTrue(COL_TRADE_DISCOUNT_PERC, row)) {
        discount = BeeUtils.percent(base, discount);
      }
    }
    return discount;
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
    if (isVatPlus(row)) {
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

  public Double getVat(IsRow row, Double base) {
    Double vat = null;

    if (base != null && (!dependsOnSource(COL_TRADE_DOCUMENT_VAT_MODE)
        || isTrue(COL_TRADE_DOCUMENT_VAT_MODE, row))) {
      vat = getNumber(COL_TRADE_VAT, row);

      if (vat != null && isTrue(COL_TRADE_VAT_PERC, row)) {
        if (isVatInclusive(row)) {
          vat = BeeUtils.percentInclusive(base, vat);

        } else if (isVatPlus(row)) {
          vat = BeeUtils.percent(base, vat);
        }
      }
    }
    return vat;
  }

  public boolean isVatInclusive(IsRow row) {
    return isTrue(COL_TRADE_VAT_INCLUSIVE, row);
  }

  public boolean isVatPlus(IsRow row) {
    return isTrue(COL_TRADE_VAT_PLUS, row);
  }

  public void setAmountFunction(RowToDouble function) {
    setFunction(COL_TRADE_AMOUNT, function);
  }

  public void setDiscountFunction(RowToDouble function) {
    setFunction(COL_TRADE_DISCOUNT, function);
  }

  public void setDiscountPercentPredicate(RowPredicate predicate) {
    setPredicate(COL_TRADE_DISCOUNT_PERC, predicate);
  }

  public void setPriceFunction(RowToDouble function) {
    setFunction(COL_TRADE_ITEM_PRICE, function);
  }

  public void setQuantityFunction(RowToDouble function) {
    setFunction(COL_TRADE_ITEM_QUANTITY, function);
  }

  public void setVatFunction(RowToDouble function) {
    setFunction(COL_TRADE_VAT, function);
  }

  public void setVatPercentPredicate(RowPredicate predicate) {
    setPredicate(COL_TRADE_VAT_PERC, predicate);
  }

  public void setVatInclusivePredicate(RowPredicate predicate) {
    setPredicate(COL_TRADE_VAT_INCLUSIVE, predicate);
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

  private Double getNumber(String key, IsRow row) {
    RowToDouble function = functions.get(key);
    return (function == null) ? null : function.apply(row);
  }

  private boolean isTrue(String key, IsRow row) {
    RowPredicate predicate = predicates.get(key);
    return predicate != null && predicate.test(row);
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
