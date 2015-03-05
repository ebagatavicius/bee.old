package com.butent.bee.client.output;

import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;

public class ReportTextItem extends ReportItem {

  public ReportTextItem(String name, String caption) {
    super(name, caption);
  }

  @Override
  public ReportItem create() {
    return new ReportTextItem(getName(), getCaption());
  }

  @Override
  public String evaluate(SimpleRow row) {
    return row.getValue(getName());
  }
}
