package com.butent.bee.client.grid.column;

import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.cell.RightsCell;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.ui.ColumnDescription.ColType;

public class RightsColumn extends AbstractColumn<String> {

  public RightsColumn(String viewName, long roleId) {
    super(new RightsCell(viewName, roleId));
  }

  @Override
  public ValueType getValueType() {
    return null;
  }

  @Override
  public ColType getColType() {
    return ColType.RIGHTS;
  }

  @Override
  public String getString(CellContext context) {
    return null;
  }

  @Override
  public String getStyleSuffix() {
    return "rights";
  }

  @Override
  public String getValue(IsRow row) {
    return null;
  }
}
