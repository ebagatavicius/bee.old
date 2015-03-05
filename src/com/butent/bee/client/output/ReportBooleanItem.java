package com.butent.bee.client.output;

import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

public class ReportBooleanItem extends ReportItem {

  public ReportBooleanItem(String name, String caption) {
    super(name, caption);
  }

  @Override
  public ReportItem create() {
    return new ReportBooleanItem(getName(), getCaption());
  }

  @Override
  public String evaluate(SimpleRow row) {
    return BeeUtils.unbox(row.getBoolean(getName()))
        ? Localized.getConstants().yes() : Localized.getConstants().no();
  }
}
