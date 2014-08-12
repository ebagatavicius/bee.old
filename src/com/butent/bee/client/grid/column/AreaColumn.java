package com.butent.bee.client.grid.column;

import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.grid.cell.TextCell;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;

public class AreaColumn extends DataColumn<String> {

  private int maxDisplaySize;

  public AreaColumn(CellSource cellSource) {
    this(new TextCell(), cellSource, BeeConst.UNDEF);
  }

  public AreaColumn(AbstractCell<String> cell, CellSource cellSource) {
    this(cell, cellSource, BeeConst.UNDEF);
  }

  public AreaColumn(AbstractCell<String> cell, CellSource cellSource, int maxDisplaySize) {
    super(cell, cellSource);
    this.maxDisplaySize = maxDisplaySize;
  }

  public int getMaxDisplaySize() {
    return maxDisplaySize;
  }

  @Override
  public String getStyleSuffix() {
    return "area";
  }

  @Override
  public String getValue(IsRow row) {
    if (row == null) {
      return null;
    }

    String v = getCellSource().getString(row);
    if (v == null) {
      return null;
    }

    if (maxDisplaySize <= 0 || v.length() <= maxDisplaySize) {
      return v;
    }
    return BeeUtils.clip(v, maxDisplaySize);
  }

  public void setMaxDisplaySize(int maxDisplaySize) {
    this.maxDisplaySize = maxDisplaySize;
  }
}
