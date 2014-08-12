package com.butent.bee.client.modules.ec.render;

import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.ProvidesGridColumnRenderer;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public final class CategoryFullNameRenderer extends AbstractCellRenderer {

  public static class Provider implements ProvidesGridColumnRenderer {
    public Provider() {
      super();
    }

    @Override
    public AbstractCellRenderer getRenderer(String columnName,
        List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
        CellSource cellSource) {

      int index = DataUtils.getColumnIndex(columnName, dataColumns);
      Assert.nonNegative(index);

      String options = (columnDescription == null) ? null : columnDescription.getOptions();
      return new CategoryFullNameRenderer(index, BeeUtils.nvl(options, " - "));
    }
  }

  private final int categoryIndex;
  private final String separator;

  private CategoryFullNameRenderer(int categoryIndex, String separator) {
    super(null);

    this.categoryIndex = categoryIndex;
    this.separator = separator;
  }

  @Override
  public String render(IsRow row) {
    if (row == null) {
      return null;
    }

    Long category = row.getLong(categoryIndex);
    if (category == null) {
      return null;
    } else {
      return EcKeeper.getCategoryFullName(category, separator);
    }
  }
}
