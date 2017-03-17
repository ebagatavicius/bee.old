package com.butent.bee.client.output;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.report.DateTimeFunction;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumSet;

public class ReportDateTimeItem extends ReportDateItem {

  public ReportDateTimeItem(String expression, String caption) {
    super(expression, caption);
    setFormat(DateTimeFunction.DATETIME);
  }

  @Override
  public ReportValue evaluate(SimpleRow row) {
    ReportValue value;
    DateTime date = row.getDateTime(getExpression());

    if (date != null) {
      String val = BeeUtils.toString(getValue(date));

      switch (getFormat()) {
        case DATETIME:
          value = ReportValue.of(BeeUtils.padLeft(val, 15, BeeConst.CHAR_ZERO))
              .setDisplay(Format.renderDateTime(date));
          break;
        case HOUR:
        case MINUTE:
          value = ReportValue.of(BeeUtils.padLeft(val, 2, BeeConst.CHAR_ZERO));
          break;
        default:
          value = evaluate(date.getDate());
          break;
      }
    } else {
      value = ReportValue.empty();
    }
    return value;
  }

  @Override
  public String getFormatedCaption() {
    String cap = getCaption();

    if (getFormat() != DateTimeFunction.DATETIME) {
      cap = BeeUtils.joinWords(cap, BeeUtils.parenthesize(getFormat().getCaption()));
    }
    return cap;
  }

  @Override
  public boolean validate(SimpleRow row) {
    if (!row.getRowSet().hasColumn(getExpression())) {
      return true;
    }
    return validate(row.getDateTime(getExpression()));
  }

  @Override
  protected Editor createFilterEditor() {
    int limit;

    switch (getFormat()) {
      case DATETIME:
        return new InputDateTime();
      case HOUR:
        limit = 24;
        break;
      case MINUTE:
        limit = 60;
        break;
      default:
        return super.createFilterEditor();
    }
    ListBox editor = new ListBox();
    editor.addItem("");

    for (int i = 0; i < limit; i++) {
      editor.addItem(TimeUtils.padTwo(i), BeeUtils.toString(i));
    }
    return editor;
  }

  @Override
  protected EnumSet<DateTimeFunction> getSupportedFunctions() {
    return EnumSet.allOf(DateTimeFunction.class);
  }

  @Override
  protected long getValue(HasDateValue date) {
    switch (getFormat()) {
      case DATETIME:
        return date.getTime();
      case HOUR:
        return date.getHour();
      case MINUTE:
        return date.getMinute();
      default:
        return super.getValue(date);
    }
  }
}
