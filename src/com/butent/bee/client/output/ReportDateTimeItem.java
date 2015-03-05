package com.butent.bee.client.output;

import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.EnumSet;

public class ReportDateTimeItem extends ReportDateItem {

  public ReportDateTimeItem(String name, String caption) {
    super(name, caption);
    setOptions(DateTimeFunction.DATE.name());
  }

  @Override
  public ReportItem create() {
    ReportDateTimeItem item = new ReportDateTimeItem(getName(), getCaption());
    item.setOptions(getOptions());
    return item;
  }

  @Override
  public String evaluate(SimpleRow row) {
    String value = null;
    DateTime dateTime = row.getDateTime(getName());

    if (dateTime != null) {
      DateTimeFunction fnc = EnumUtils.getEnumByName(DateTimeFunction.class, getOptions());

      if (fnc != null) {
        switch (fnc) {
          case DATE:
            value = dateTime.toDateString();
            break;
          case HOUR:
            value = TimeUtils.padTwo(dateTime.getHour());
            break;
          case MINUTE:
            value = TimeUtils.padTwo(dateTime.getMinute());
            break;
          case TIME:
            value = dateTime.toCompactTimeString();
            break;
          default:
            value = super.evaluate(row);
            break;
        }
      } else {
        value = dateTime.toCompactString();
      }
    }
    return value;
  }

  @Override
  public Editor getOptionsEditor() {
    ListBox editor = (ListBox) super.getOptionsEditor();

    for (DateTimeFunction fnc : EnumSet.of(DateTimeFunction.DATE, DateTimeFunction.TIME,
        DateTimeFunction.HOUR, DateTimeFunction.MINUTE)) {
      editor.addItem(fnc.getCaption(), fnc.name());
    }
    editor.setValue(getOptions());
    return editor;
  }
}
