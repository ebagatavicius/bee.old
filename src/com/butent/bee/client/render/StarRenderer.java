package com.butent.bee.client.render;

import com.butent.bee.client.images.star.Stars;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XSheet;

public class StarRenderer extends AbstractCellRenderer {

  public StarRenderer(CellSource cellSource) {
    super(cellSource);
  }

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    Integer index = getInteger(row);
    if (index == null || sheet == null) {
      return null;
    }

    Integer ref = Stars.export(index, sheet);
    if (ref == null) {
      return null;
    }

    return XCell.forPicture(cellIndex, ref);
  }

  @Override
  public VerticalAlign getDefaultVerticalAlign() {
    return VerticalAlign.MIDDLE;
  }

  @Override
  public String render(IsRow row) {
    Integer index = getInteger(row);

    if (index == null) {
      return null;
    } else {
      return Stars.getHtml(index);
    }
  }
}
