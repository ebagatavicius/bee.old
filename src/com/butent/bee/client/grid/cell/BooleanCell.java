package com.butent.bee.client.grid.cell;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import com.butent.bee.client.grid.CellContext;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.ui.HandlesFormat;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Enables using a cell for representing boolean data.
 */

public class BooleanCell extends AbstractCell<Boolean> implements HandlesFormat {

  private String trueText;
  private String falseText;
  private String nullText;

  public BooleanCell() {
    this(BeeConst.STRING_CHECK_MARK, null, null);
  }

  public BooleanCell(String trueText, String falseText, String nullText) {
    this.trueText = trueText;
    this.falseText = falseText;
    this.nullText = nullText;
  }

  public String getFalseText() {
    return falseText;
  }

  public String getNullText() {
    return nullText;
  }

  public String getTrueText() {
    return trueText;
  }

  @Override
  public void render(CellContext context, Boolean value, SafeHtmlBuilder sb) {
    String text = (value == null) ? nullText : (value ? trueText : falseText);
    if (text != null) {
      sb.appendEscaped(text);
    }
  }

  public void setFalseText(String falseText) {
    this.falseText = falseText;
  }

  @Override
  public void setFormat(String format) {
    if (!BeeUtils.isEmpty(format)) {
      String[] arr = BeeUtils.split(format, BeeConst.CHAR_SPACE);
      int length = ArrayUtils.length(arr);

      if (length > 0) {
        setTrueText(arr[0]);
      }
      if (length > 1) {
        setFalseText(arr[1]);
      }
      if (length > 2) {
        setNullText(arr[2]);
      }
    }
  }

  public void setNullText(String nullText) {
    this.nullText = nullText;
  }

  public void setTrueText(String trueText) {
    this.trueText = trueText;
  }
}
