package com.butent.bee.client.modules.trade.acts;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowToDouble;
import com.butent.bee.shared.modules.trade.acts.TradeActConstants;
import com.butent.bee.shared.utils.BeeUtils;

class QuantityReader extends RowToDouble {

  private final int index;

  QuantityReader(int index) {
    this.index = index;
  }

  @Override
  public Double apply(IsRow input) {
    if (input == null) {
      return null;
    }

    Double qty = input.getDouble(index);
    if (BeeUtils.isPositive(qty)) {
      qty -= BeeUtils.toDouble(input.getProperty(TradeActConstants.PRP_RETURNED_QTY));
    } else {
      return BeeConst.DOUBLE_ZERO;
    }

    return qty;
  }
}
