package com.butent.bee.client.render;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;

import java.util.function.Function;

public class SimpleRenderer extends AbstractCellRenderer {

  private final Function<HasDateValue, String> dateRenderer;
  private final Function<DateTime, String> dateTimeRenderer;

  public SimpleRenderer(CellSource cellSource) {
    super(cellSource);

    this.dateRenderer = Format.getDateRenderer();
    this.dateTimeRenderer = Format.getDateTimeRenderer();
  }

  @Override
  public String render(IsRow row) {
    if (row == null || getCellSource() == null) {
      return null;
    } else {
      return getCellSource().render(row, dateRenderer, dateTimeRenderer);
    }
  }
}
