package com.butent.bee.client.view.search;

import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterValue;
import com.butent.bee.shared.data.value.ValueType;

public class CustomDateTimeFilterSupplier extends DateTimeFilterSupplier {
  private final ValueType columnType;
  private final String columnId;

  public CustomDateTimeFilterSupplier(String viewName, BeeColumn filterColumn, String columnId,
      ValueType columnType, String label, String options) {

    super(viewName, filterColumn, label, options);

    this.columnId = columnId;
    this.columnType = columnType;
  }

  @Override
  public String getColumnId() {
    return columnId;
  }

  @Override
  public ValueType getColumnType() {
    return columnType;
  }

  @Override
  public Filter parse(FilterValue input) {
    if (input == null || !input.hasValue()) {
      return null;
    }
    return Filter.custom(getOptions(), getColumnId(), input.getValue());
  }
}
