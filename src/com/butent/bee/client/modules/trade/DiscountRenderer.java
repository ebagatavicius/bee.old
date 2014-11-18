package com.butent.bee.client.modules.trade;

import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;

import java.util.List;

public class DiscountRenderer extends TotalRenderer {

  public DiscountRenderer(List<? extends IsColumn> columns) {
    super(columns);
  }

  @Override
  protected Double evaluate(IsRow row) {
    return getDiscount(row);
  }
}
