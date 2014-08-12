package com.butent.bee.client.render;

import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.NameUtils;

public class EvalRenderer extends AbstractCellRenderer {

  private final Evaluator evaluator;

  public EvalRenderer(CellSource cellSource, Evaluator evaluator) {
    super(cellSource);

    Assert.notNull(evaluator, NameUtils.getName(this) + ": evaluator is required");
    this.evaluator = evaluator;
  }

  @Override
  public String render(IsRow row) {
    if (row == null) {
      return null;
    }

    if (getCellSource() != null) {
      evaluator.update(row, getValueType(), getString(row));
    } else {
      evaluator.update(row);
    }

    return evaluator.evaluate();
  }
}
