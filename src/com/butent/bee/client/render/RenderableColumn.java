package com.butent.bee.client.render;

import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.grid.column.DataColumn;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.NameUtils;

public class RenderableColumn extends DataColumn<String> implements HasCellRenderer {

  private AbstractCellRenderer renderer;

  public RenderableColumn(AbstractCell<String> cell, CellSource cellSource,
      AbstractCellRenderer renderer) {
    super(cell, cellSource);
    this.renderer = renderer;
  }

  public RenderableColumn(CellSource cellSource, AbstractCellRenderer renderer) {
    this(new RenderableCell(), cellSource, renderer);
  }

  @Override
  public AbstractCellRenderer getRenderer() {
    return renderer;
  }

  @Override
  public String getStyleSuffix() {
    return (getRenderer() == null) ? "renderable" : NameUtils.getName(getRenderer());
  }

  @Override
  public String getValue(IsRow row) {
    return renderer.render(row);
  }

  @Override
  public void setRenderer(AbstractCellRenderer renderer) {
    this.renderer = renderer;
  }
}
